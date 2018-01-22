package karballo.search

import karballo.Board
import karballo.Config
import karballo.Move
import karballo.bitboard.BitboardUtils
import karballo.evaluation.CompleteEvaluator
import karballo.evaluation.Evaluator
import karballo.evaluation.SimplifiedEvaluator
import karballo.log.Logger
import karballo.tt.TranspositionTable
import karballo.util.Utils
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Search engine

 * @author Alberto Alonso Ruibal
 */
open class SearchEngine(var config: Config) {
    var debug = false

    internal var searchLock = Any()
    var startStopSearchLock = Any()

    private lateinit var searchParameters: SearchParameters

    protected var initialized = false
    var isSearching = false
        protected set

    // Think limits
    private var stop = false
    private var thinkToTime: Long = 0
    private var thinkToNodes = 0
    private var thinkToDepth = 0

    val board: Board = Board()
    private var observer: SearchObserver? = null
    lateinit private var evaluator: Evaluator
    lateinit var tt: TranspositionTable
        private set
    var nodes: Array<Node>
    var history: Array<ShortArray> = Array(6) { ShortArray(64) } // By piece type and destiny square

    var bestMoveScore: Int = 0
        private set
    var bestMove: Int = 0
        private set
    private var globalPonderMove: Int = 0

    private var initialPly: Int = 0 // Initial Ply for search
    private var depth: Int = 0
    private var selDepth: Int = 0
    private var rootScore: Int = 0
    private var aspWindows: IntArray? = null
    private var panicTime: Boolean = false
    private var engineIsWhite: Boolean = false

    var startTime: Long = 0

    // For performance benching
    var nodeCount: Long = 0
        private set
    private var pvCutNodes: Long = 0
    private var pvAllNodes: Long = 0
    private var nullCutNodes: Long = 0
    private var nullAllNodes: Long = 0

    private val logMatrix: Array<FloatArray>

    init {
        nodes = Array(MAX_DEPTH, { i -> Node(this, i) })

        // The logMatrix is normalized [0..1]
        logMatrix = Array(64) { FloatArray(64) }
        for (i in 1..63) {
            for (j in 1..63) {
                logMatrix[i][j] = (ln(i.toDouble()) * ln(j.toDouble()) / (ln(63.0) * ln(63.0))).toFloat()
            }
        }

        init()
    }

    fun init() {
        initialized = false

        if (config.isUciChess960) {
            board.chess960 = true
        }

        board.startPosition()
        for (i in 0..MAX_DEPTH - 1) {
            nodes[i].clear()
        }
        clearHistory()

        val evaluatorName = config.evaluator
        if ("simplified" == evaluatorName) {
            evaluator = SimplifiedEvaluator()
        } else if ("complete" == evaluatorName) {
            evaluator = CompleteEvaluator()
        }

        tt = TranspositionTable(config.transpositionTableSize)

        initialized = true
        if (debug) {
            logger.debug(config.toString())
        }
    }

    fun clear() {
        clearHistory()
        // And transposition table
        tt.clear()
        // And the killer moves stored in the nodes
        for (i in 0..MAX_DEPTH - 1) {
            nodes[i].clear()
        }
    }

    fun clearHistory() {
        for (i in 0..5) {
            Utils.instance.arrayFill(history[i], 0.toShort())
        }
    }

    fun setObserver(observer: SearchObserver) {
        this.observer = observer
    }

    /**
     * Decides when we are going to allow null move. Don't do null move in king and pawn endings
     */
    private fun boardAllowsNullMove(): Boolean {
        return board.mines and (board.knights or board.bishops or board.rooks or board.queens) != 0L
    }

    /**
     * Returns true if we can use the value stored on the TT to return from search
     */
    private fun canUseTT(depthRemaining: Int, alpha: Int, beta: Int): Boolean {
        if (tt.depthAnalyzed >= depthRemaining) {
            when (tt.nodeType) {
                TranspositionTable.TYPE_EXACT_SCORE -> {
                    ttPvHit++
                    return true
                }
                TranspositionTable.TYPE_FAIL_LOW -> {
                    ttLBHit++
                    if (tt.score <= alpha) {
                        return true
                    }
                }
                TranspositionTable.TYPE_FAIL_HIGH -> {
                    ttUBHit++
                    if (tt.score >= beta) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * It also changes the sign to the score depending of the turn
     */
    fun evaluate(node: Node, foundTT: Boolean) {
        ttEvalProbe++

        if (foundTT) {
            ttEvalHit++
            node.staticEval = tt.getEval()
            return
        }
        node.staticEval = if (board.turn)
            evaluator.evaluate(board, node.attacksInfo)
        else
            -evaluator.evaluate(board, node.attacksInfo)

        // Store always the eval value in the TT
        tt[board, TranspositionTable.TYPE_EVAL, node.distanceToInitialPly, 0, Move.NONE, 0, node.staticEval] = false
    }

    fun refineEval(node: Node, foundTT: Boolean): Int {
        return if (foundTT && (tt.nodeType == TranspositionTable.TYPE_EXACT_SCORE
                || tt.nodeType == TranspositionTable.TYPE_FAIL_LOW && tt.score < node.staticEval
                || tt.nodeType == TranspositionTable.TYPE_FAIL_HIGH && tt.score > node.staticEval))
            tt.score
        else
            node.staticEval
    }

    fun quiescentSearch(qsdepth: Int, alphaIn: Int, betaIn: Int): Int {
        var alpha = alphaIn
        var beta = betaIn
        val distanceToInitialPly = board.moveNumber - initialPly

        // It checks draw by three fold repetition, fifty moves rule and no material to mate
        if (board.isDraw) {
            return evaluateDraw(distanceToInitialPly)
        }

        // Mate distance pruning
        alpha = max(valueMatedIn(distanceToInitialPly), alpha)
        beta = min(valueMateIn(distanceToInitialPly + 1), beta)
        if (alpha >= beta) {
            return alpha
        }

        val node = nodes[distanceToInitialPly]

        val isPv = beta - alpha > 1
        val checkEvasion = board.check
        // Generate checks on PLY 0
        val generateChecks = qsdepth == 0
        // If we generate check, the entry in the TT has depthAnalyzed=1, because is better than without checks (depthAnalyzed=0)
        val ttDepth = if (generateChecks || checkEvasion) TranspositionTable.DEPTH_QS_CHECKS else TranspositionTable.DEPTH_QS_NO_CHECKS

        ttProbe++
        val foundTT = tt.search(board, distanceToInitialPly, false)
        if (foundTT) {
            if (!isPv && canUseTT(ttDepth, alpha, beta)) {
                return tt.score
            }
            node.ttMove = tt.bestMove
        } else {
            node.ttMove = Move.NONE
        }

        var bestMove = Move.NONE
        var bestScore = alpha
        node.staticEval = Evaluator.NO_VALUE
        var eval = -Evaluator.MATE
        var futilityBase = -Evaluator.MATE

        // Do not allow stand pat when in check
        if (!checkEvasion) {
            evaluate(node, foundTT)
            eval = refineEval(node, foundTT)

            // Evaluation functions increase alphaIn and can originate betaIn cutoffs
            bestScore = max(bestScore, eval)
            if (bestScore >= beta) {
                if (!foundTT) {
                    tt[board, TranspositionTable.TYPE_FAIL_HIGH, distanceToInitialPly, TranspositionTable.DEPTH_QS_CHECKS, Move.NONE, bestScore, node.staticEval] = false
                }
                return bestScore
            }

            futilityBase = eval + FUTILITY_MARGIN_QS
        }

        // If we have more depths than possible...
        if (distanceToInitialPly >= MAX_DEPTH - 1) {
            return if (checkEvasion) evaluateDraw(distanceToInitialPly) else eval // Return a drawish score if we are in check
        }

        node.moveIterator.genMoves(node.ttMove, if (generateChecks) MoveIterator.GENERATE_CAPTURES_PROMOS_CHECKS else MoveIterator.GENERATE_CAPTURES_PROMOS)

        var moveCount = 0
        while (true) {
            node.move = node.moveIterator.next()
            if (node.move == Move.NONE) {
                break
            }

            nodeCount++
            moveCount++

            // Futility pruning
            if (!node.moveIterator.checkEvasion
                    && !Move.isCheck(node.move)
                    && !Move.isPawnPush678(node.move)
                    && futilityBase > -Evaluator.KNOWN_WIN) {
                val futilityValue = futilityBase + Evaluator.PIECE_VALUES[Move.getPieceCaptured(board, node.move)]
                if (futilityValue <= alpha) {
                    bestScore = max(bestScore, futilityValue)
                    continue
                }
                if (futilityBase <= alpha && node.moveIterator.lastMoveSee <= 0L) {
                    bestScore = max(bestScore, futilityBase)
                    continue
                }
            }

            board.doMove(node.move, false, false)
            //assert(board.check == Move.isCheck(node.move)) { "Check flag not generated properly" }

            val score = -quiescentSearch(qsdepth + 1, -beta, -bestScore)
            board.undoMove()
            if (score > bestScore) {
                bestMove = node.move
                bestScore = score
                if (score >= beta) {
                    break
                }
            }
        }

        if (checkEvasion && moveCount == 0) {
            return valueMatedIn(distanceToInitialPly)
        }
        tt[board, if (bestScore <= alpha)
            TranspositionTable.TYPE_FAIL_LOW
        else if (bestScore >= beta)
            TranspositionTable.TYPE_FAIL_HIGH
        else
            TranspositionTable.TYPE_EXACT_SCORE, distanceToInitialPly, ttDepth, bestMove, bestScore, node.staticEval] = false

        return bestScore
    }

    /**
     * Search Root, PV and null window
     */
    fun search(nodeType: Int, depthRemaining: Int, alphaIn: Int, betaIn: Int, allowPrePruning: Boolean, excludedMove: Int): Int {
        var alpha = alphaIn
        var beta = betaIn
        //assert(depthRemaining > 0) { "Wrong depthRemaining" }

        if (nodeType != NODE_ROOT && bestMove != Move.NONE && (Utils.instance.currentTimeMillis() > thinkToTime || nodeCount > thinkToNodes)) {
            throw SearchFinishedException()
        }

        val distanceToInitialPly = board.moveNumber - initialPly

        if (nodeType == NODE_PV || nodeType == NODE_ROOT) {
            if (distanceToInitialPly > selDepth) {
                selDepth = distanceToInitialPly
            }
        }

        // It checks draw by three fold repetition, fifty moves rule and no material to mate
        if (board.isDraw) {
            return evaluateDraw(distanceToInitialPly)
        }

        // Mate distance pruning
        alpha = max(valueMatedIn(distanceToInitialPly), alpha)
        beta = min(valueMateIn(distanceToInitialPly + 1), beta)
        if (alpha >= beta) {
            return alpha
        }

        val node = nodes[distanceToInitialPly]

        var ttScore = 0
        var ttNodeType = 0
        var ttDepthAnalyzed = 0
        var score = 0

        ttProbe++
        val foundTT = tt.search(board, distanceToInitialPly, excludedMove != Move.NONE)
        if (foundTT) {
            if (nodeType != NODE_ROOT && canUseTT(depthRemaining, alpha, beta)) {
                if (distanceToInitialPly + tt.depthAnalyzed > selDepth) {
                    selDepth = distanceToInitialPly + tt.depthAnalyzed
                }

                historyGood(node, tt.bestMove, depthRemaining)
                return tt.score
            }
            node.ttMove = tt.bestMove
            ttScore = tt.score
            ttNodeType = tt.nodeType
            ttDepthAnalyzed = tt.depthAnalyzed
        } else {
            node.ttMove = Move.NONE
        }

        val checkEvasion = board.check
        var mateThreat = false
        var eval = -Evaluator.MATE
        node.staticEval = -Evaluator.MATE

        if (!checkEvasion) {
            // Do a static eval, in case of exclusion and not found in the TT, search again with the normal key
            val evalTT = if (excludedMove == Move.NONE || foundTT) foundTT else tt.search(board, distanceToInitialPly, false)
            evaluate(node, evalTT)
            eval = refineEval(node, foundTT)
        }

        // If we have more depths than possible...
        if (distanceToInitialPly >= MAX_DEPTH - 1) {
            return if (checkEvasion) evaluateDraw(distanceToInitialPly) else eval // Return a drawish score if we are in check
        }

        if (!checkEvasion && allowPrePruning) {
            // Hyatt's Razoring http://chessprogramming.wikispaces.com/Razoring
            if (nodeType == NODE_NULL
                    && node.ttMove == Move.NONE
                    && depthRemaining < RAZORING_MARGIN.size
                    && abs(beta) < VALUE_IS_MATE
                    && abs(eval) < Evaluator.KNOWN_WIN
                    && eval + RAZORING_MARGIN[depthRemaining] < beta
                    && board.pawns and (board.whites and BitboardUtils.R7 or (board.blacks and BitboardUtils.R2)) == 0L) { // No pawns on 7TH
                razoringProbe++

                if (depthRemaining <= PLY && eval + RAZORING_MARGIN[RAZORING_MARGIN.size - 1] < beta) {
                    razoringHit++
                    return quiescentSearch(0, alpha, beta)
                }

                val rbeta = beta - RAZORING_MARGIN[depthRemaining]
                val v = quiescentSearch(0, rbeta - 1, rbeta)
                if (v < rbeta) {
                    razoringHit++
                    return v
                }
            }

            // Static null move pruning or futility pruning in parent node
            if (nodeType != NODE_ROOT
                    && depthRemaining < FUTILITY_MARGIN_CHILD.size
                    && abs(beta) < VALUE_IS_MATE
                    && abs(eval) < Evaluator.KNOWN_WIN
                    && eval - FUTILITY_MARGIN_CHILD[depthRemaining] >= beta
                    && boardAllowsNullMove()) {
                return eval - FUTILITY_MARGIN_CHILD[depthRemaining]
            }

            // Null move pruning and mate threat detection
            if (nodeType == NODE_NULL
                    && depthRemaining >= 2 * PLY
                    && abs(beta) < VALUE_IS_MATE
                    && eval >= beta
                    && boardAllowsNullMove()) {

                nullMoveProbe++

                val R = 3 * PLY + (depthRemaining shr 2)

                board.doMove(Move.NULL, false, false)
                score = if (depthRemaining - R < PLY)
                    -quiescentSearch(0, -beta, -beta + 1)
                else
                    -search(NODE_NULL, depthRemaining - R, -beta, -beta + 1, false, Move.NONE)
                board.undoMove()

                if (score >= beta) {
                    if (score >= VALUE_IS_MATE) {
                        score = beta
                    }

                    // Verification search on initial depths
                    if (depthRemaining < 12 * PLY || (if (depthRemaining - R < PLY)
                        quiescentSearch(0, beta - 1, beta)
                    else
                        search(NODE_NULL, depthRemaining - R, beta - 1, beta, false, Move.NONE)) >= beta) {
                        nullMoveHit++
                        return score
                    }
                } else {
                    // Detect mate threat
                    if (score <= -VALUE_IS_MATE) {
                        mateThreat = true
                    }
                }
            }

            // Internal Iterative Deepening (IID)
            // Do a reduced move to search for a ttMove that will improve sorting
            if (node.ttMove == Move.NONE
                    && depthRemaining >= IID_DEPTH[nodeType]
                    && (nodeType != NODE_NULL || node.staticEval + IID_MARGIN > beta)) {
                val d = if (nodeType != NODE_NULL)
                    depthRemaining - 2 * PLY
                else
                // Root and PV nodes are less reduced
                    depthRemaining shr 1
                search(nodeType, d, alpha, beta, false, Move.NONE)
                if (tt.search(board, distanceToInitialPly, false)) {
                    node.ttMove = tt.bestMove
                }
            }
        }

        node.moveIterator.genMoves(node.ttMove)

        var bestMove = Move.NONE
        var bestScore = -Evaluator.MATE
        var moveCount = 0

        // Eval diff between our node and two nodes before
        var nodeEvalDiff = if (distanceToInitialPly > 2
                && node.staticEval != Evaluator.NO_VALUE
                && nodes[distanceToInitialPly - 2].staticEval != Evaluator.NO_VALUE)
            node.staticEval - nodes[distanceToInitialPly - 2].staticEval
        else
            0
        nodeEvalDiff = if (nodeEvalDiff < NODE_EVAL_DIFF_MIN)
            NODE_EVAL_DIFF_MIN
        else if (nodeEvalDiff > NODE_EVAL_DIFF_MAX)
            NODE_EVAL_DIFF_MAX
        else
            nodeEvalDiff

        while (true) {
            node.move = node.moveIterator.next()
            if (node.move == Move.NONE) {
                break
            }

            if (node.move == excludedMove) {
                continue
            }
            if (nodeType == NODE_ROOT
                    && searchParameters.searchMoves != null
                    && searchParameters.searchMoves!!.size > 0
                    && !searchParameters.searchMoves!!.contains(node.move)) {
                continue
            }
            nodeCount++
            moveCount++

            //
            // Calculates the extension of a move in the actual position
            //
            var extension = if (mateThreat)
                PLY
            else if (Move.isCheck(node.move) && node.moveIterator.lastMoveSee >= 0)
                PLY
            else
                0

            // Check singular move extension
            // It also detects singular replies
            if (nodeType != NODE_ROOT
                    && node.move == node.ttMove
                    && extension < PLY
                    && excludedMove == Move.NONE
                    && depthRemaining >= SINGULAR_MOVE_DEPTH[nodeType]
                    && ttNodeType == TranspositionTable.TYPE_FAIL_HIGH
                    && ttDepthAnalyzed >= depthRemaining - 3 * PLY
                    && abs(ttScore) < Evaluator.KNOWN_WIN) {

                val savedMove = node.move

                singularExtensionProbe++
                val seBeta = ttScore - SINGULAR_EXTENSION_MARGIN_PER_PLY * depthRemaining / PLY
                val excScore = search(nodeType, depthRemaining shr 1, seBeta - 1, seBeta, false, node.move)
                if (excScore < seBeta) {
                    singularExtensionHit++
                    extension = PLY
                }

                // ****** FIX NODE AND MOVE ITERATOR ******
                // The same move iterator is used in the excluded search, so reset it to the point
                // where it was previously (the TT move)
                node.move = savedMove
                node.ttMove = savedMove
                node.moveIterator.genMoves(node.ttMove)
                node.moveIterator.next()
            }

            val newDepth = depthRemaining + extension - PLY
            var reduction = 0

            // If the move is not important
            if (nodeType != NODE_ROOT
                    && node.move != node.ttMove
                    && !checkEvasion
                    && !Move.isCaptureOrCheck(node.move) // Include ALL captures

                    && !Move.isPawnPush678(node.move) // Includes promotions

                    && !node.moveIterator.lastMoveIsKiller) {

                // History pruning
                if (bestMove != Move.NONE
                        && nodeType == NODE_NULL
                        && newDepth < HISTORY_PRUNING_TRESHOLD.size
                        && node.moveIterator.lastMoveScore < HISTORY_MIN + HISTORY_PRUNING_TRESHOLD[newDepth]) {
                    continue
                }

                // Late move reductions (LMR)
                if (depthRemaining >= LMR_DEPTHS_NOT_REDUCED) {
                    reduction = (0.5f + 4.4f * (if (nodeType == NODE_NULL) 1f else 0.85f) * (1f - 0.8f * nodeEvalDiff / (NODE_EVAL_DIFF_MAX - NODE_EVAL_DIFF_MIN)) // [-0.5..0.5]
                            * (1f - 1.1f * node.moveIterator.lastMoveScore / (HISTORY_MAX - HISTORY_MIN)) * logMatrix[min(depthRemaining / PLY, 63)][min(moveCount, 63)]).toInt()

                    if (reduction > newDepth) {
                        reduction = newDepth
                    }
                }

                if (bestMove != Move.NONE) { // There is a best move
                    // Futility Pruning
                    if (newDepth - reduction < FUTILITY_MARGIN_PARENT.size) {
                        val futilityValue = node.staticEval + FUTILITY_MARGIN_PARENT[newDepth - reduction]
                        if (futilityValue <= alpha) {
                            futilityHit++
                            if (futilityValue > bestScore) {
                                bestScore = futilityValue
                            }
                            continue
                        }
                    }

                    // Prune moves with negative SSEs
                    if (newDepth - reduction < 4 * PLY && node.moveIterator.lastMoveSee < 0) {
                        continue
                    }
                }
            }

            board.doMove(node.move, false, false)
            //assert(board.check == Move.isCheck(node.move)) { "Check flag not generated properly" }

            val lowBound = if (alpha > bestScore) alpha else bestScore
            if ((nodeType == NODE_PV || nodeType == NODE_ROOT) && moveCount == 1) {
                // PV move not null searched nor reduced
                score = if (newDepth < PLY)
                    -quiescentSearch(0, -beta, -lowBound)
                else
                    -search(NODE_PV, newDepth, -beta, -lowBound, true, Move.NONE)
            } else {
                // Try searching null window
                var doFullSearch = true
                if (reduction > 0) {
                    score = if (newDepth - reduction < PLY)
                        -quiescentSearch(0, -lowBound - 1, -lowBound)
                    else
                        -search(NODE_NULL, newDepth - reduction, -lowBound - 1, -lowBound, true, Move.NONE)
                    doFullSearch = score > lowBound
                }
                if (doFullSearch) {
                    score = if (newDepth < PLY)
                        -quiescentSearch(0, -lowBound - 1, -lowBound)
                    else
                        -search(NODE_NULL, newDepth, -lowBound - 1, -lowBound, true, Move.NONE)

                    // Finally search as PV if score on window
                    if ((nodeType == NODE_PV || nodeType == NODE_ROOT) //

                            && score > lowBound //

                            && (nodeType == NODE_ROOT || score < beta)) {
                        score = if (newDepth < PLY)
                            -quiescentSearch(0, -beta, -lowBound)
                        else
                            -search(NODE_PV, newDepth, -beta, -lowBound, true, Move.NONE)
                    }
                }
            }

            board.undoMove()

            // It tracks the best move and...
            if (score > bestScore && (config.rand == 0 //... insert errors to lower the ELO
                    || bestMove == Move.NONE // it makes sure that has at least one move
                    || Utils.instance.randomInt(1000) > config.rand)) {
                bestMove = node.move
                bestScore = score

                if (nodeType == NODE_ROOT) {
                    this.bestMove = node.move
                    bestMoveScore = score

                    if (depthRemaining > 6 * PLY) {
                        notifyMoveFound(node.move, score, alpha, beta)
                    }
                }
            }

            // alphaIn/betaIn cut (fail high)
            if (score >= beta) {
                break
            } else if (score <= alpha) {
                historyBad(node.move, depthRemaining)
            }
        }

        // Checkmate or stalemate
        if (moveCount == 0) {
            bestScore = if (excludedMove != Move.NONE)
                alpha
            else if (checkEvasion)
                valueMatedIn(distanceToInitialPly)
            else
                evaluateDraw(distanceToInitialPly)
        }

        // Tells history the good move
        if (bestScore >= beta) {
            if (moveCount > 0) {
                historyGood(node, bestMove, depthRemaining)
            }
            if (nodeType == NODE_NULL) {
                nullCutNodes++
            } else {
                pvCutNodes++
            }
        } else {
            if (nodeType == NODE_NULL) {
                nullAllNodes++
            } else {
                pvAllNodes++
            }
        }

        // Save in the transposition table
        tt[board, if (bestScore <= alpha)
            TranspositionTable.TYPE_FAIL_LOW
        else if (bestScore >= beta)
            TranspositionTable.TYPE_FAIL_HIGH
        else
            TranspositionTable.TYPE_EXACT_SCORE, distanceToInitialPly, depthRemaining, bestMove, bestScore, node.staticEval] = excludedMove != Move.NONE

        return bestScore
    }

    /**
     * Notifies the best move to the SearchObserver filling a SearchStatusInfo object
     */
    private fun notifyMoveFound(move: Int, score: Int, alpha: Int, beta: Int) {
        val time = Utils.instance.currentTimeMillis()

        val info = SearchStatusInfo()
        info.depth = depth
        info.selDepth = selDepth
        info.time = time - startTime
        info.pv = getPv(move)
        info.setScore(score, alpha, beta)
        info.nodes = nodeCount
        info.hashFull = tt.hashFull
        info.nps = (1000 * nodeCount / (time - startTime + 1))

        if (observer != null) {
            observer?.info(info)
        } else {
            logger.debug(info.toString())
        }
    }

    /**
     * It searches for the best movement
     */
    open fun go(searchParameters: SearchParameters) {
        var shouldStart = false
        synchronized(startStopSearchLock) {
            if (initialized && !isSearching) {
                isSearching = true
                shouldStart = true
                setInitialSearchParameters(searchParameters)
            }
        }
        if (shouldStart) {
            run()
        }
    }

    private fun searchStats() {
        logger.debug("Positions         = " + nodeCount)
        logger.debug("PV Cut            = " + pvCutNodes + " " + 100 * pvCutNodes / (pvCutNodes + pvAllNodes + 1) + "%")
        logger.debug("PV All            = " + pvAllNodes)
        logger.debug("Null Cut          = " + nullCutNodes + " " + 100 * nullCutNodes / (nullCutNodes + nullAllNodes + 1) + "%")
        logger.debug("Null All          = " + nullAllNodes)
        if (aspirationWindowProbe > 0) {
            logger.debug("Asp Win      Hits = " + 100 * aspirationWindowHit / aspirationWindowProbe + "%")
        }
        if (ttEvalProbe > 0) {
            logger.debug("TT Eval      Hits = " + ttEvalHit + " " + 100 * ttEvalHit / ttEvalProbe + "%")
        }
        if (ttProbe > 0) {
            logger.debug("TT PV        Hits = " + ttPvHit + " " + 1000000 * ttPvHit / ttProbe + " per 10^6")
            logger.debug("TT LB        Hits = " + ttProbe + " " + 100 * ttLBHit / ttProbe + "%")
            logger.debug("TT UB        Hits = " + ttUBHit + " " + 100 * ttUBHit / ttProbe + "%")
        }
        logger.debug("Futility     Hits = " + futilityHit)
        if (nullMoveProbe > 0) {
            logger.debug("Null Move    Hits = " + nullMoveHit + " " + 100 * nullMoveHit / nullMoveProbe + "%")
        }
        if (razoringProbe > 0) {
            logger.debug("Razoring     Hits = " + razoringHit + " " + 100 * razoringHit / razoringProbe + "%")
        }
        if (singularExtensionProbe > 0) {
            logger.debug("S.Extensions Hits = " + singularExtensionHit + " " + 100 * singularExtensionHit / singularExtensionProbe + "%")
        }
    }

    private fun prepareRun() {
        logger.debug("Board\n" + board)

        panicTime = false
        bestMove = Move.NONE
        globalPonderMove = Move.NONE

        initialPly = board.moveNumber

        if (config.useBook && board.isUsingBook
                && (config.bookKnowledge == 100 || Utils.instance.randomFloat() * 100 < config.bookKnowledge)) {
            logger.debug("Searching move in book")
            val bookMove = config.book.getMove(board)
            if (bookMove != Move.NONE) {
                bestMove = bookMove
                logger.debug("Move found in book")
                throw SearchFinishedException()
            } else {
                logger.debug("Move NOT found in book")
                board.outBookMove = board.moveNumber
            }
        }

        depth = 1
        val foundTT = tt.search(board, 0, false)
        if (canUseTT(0, -Evaluator.MATE, Evaluator.MATE)) {
            rootScore = tt.score
        } else {
            evaluate(nodes[0], foundTT)
            rootScore = nodes[0].staticEval
        }
        tt.newGeneration()
        aspWindows = ASPIRATION_WINDOW_SIZES
    }

    private fun runStepped() {
        selDepth = 0
        var failHighCount = 0
        var failLowCount = 0
        val initialScore = rootScore
        var alpha = if (initialScore - aspWindows!![failLowCount] > -Evaluator.MATE) initialScore - aspWindows!![failLowCount] else -Evaluator.MATE
        var beta = if (initialScore + aspWindows!![failHighCount] < Evaluator.MATE) initialScore + aspWindows!![failHighCount] else Evaluator.MATE
        val previousRootScore = rootScore
        val time1 = Utils.instance.currentTimeMillis()

        // Iterate aspiration windows
        while (true) {
            aspirationWindowProbe++
            rootScore = search(NODE_ROOT, depth * PLY, alpha, beta, false, Move.NONE)

            if (rootScore <= alpha) {
                failLowCount++
                alpha = if (failLowCount < aspWindows!!.size && initialScore - aspWindows!![failLowCount] > -Evaluator.MATE)
                    initialScore - aspWindows!![failLowCount]
                else
                    -Evaluator.MATE - 1
            } else if (rootScore >= beta) {
                failHighCount++
                beta = if (failHighCount < aspWindows!!.size && initialScore + aspWindows!![failHighCount] < Evaluator.MATE)
                    initialScore + aspWindows!![failHighCount]
                else
                    Evaluator.MATE + 1
            } else {
                aspirationWindowHit++
                break
            }
        }

        val time2 = Utils.instance.currentTimeMillis()

        if (depth <= 6) {
            notifyMoveFound(bestMove, bestMoveScore, alpha, beta)
        } else if (!panicTime && rootScore < previousRootScore - 100) {
            panicTime = true
            updateSearchParameters(searchParameters)
        }

        if (searchParameters.manageTime() && (// Under time restrictions and...
                abs(rootScore) > VALUE_IS_MATE // Mate found or
                        || time2 + (time2 - time1 shl 1) > thinkToTime) // It will not likely finish the next iteration

                || depth == MAX_DEPTH
                || depth >= thinkToDepth
                || abs(rootScore) == Evaluator.MATE) { // Search limit reached
            throw SearchFinishedException()
        }
        depth++
    }

    fun run() {
        var bestMove = Move.NONE
        var ponderMove = Move.NONE

        synchronized(searchLock) {
            try {
                prepareRun()
                while (true) {
                    runStepped()
                }
            } catch (ignored: SearchFinishedException) {
            }

            // Return the board to the initial position
            board.undoMove(initialPly)

            bestMove = this.bestMove
            ponderMove = globalPonderMove

            while (searchParameters.isPonder && !stop) {
                sleep(10)
            }

            isSearching = false
        }

        observer?.bestMove(bestMove, ponderMove)
        if (debug) {
            searchStats()
        }
    }

    open fun sleep(time: Long) {
    }

    /**
     * Cannot be called during search (!)
     */
    fun setInitialSearchParameters(searchParameters: SearchParameters) {
        engineIsWhite = board.turn
        startTime = Utils.instance.currentTimeMillis()
        nodeCount = 0
        stop = false
        updateSearchParameters(searchParameters)
    }

    /**
     * This is used to update the search parameters while searching
     */
    fun updateSearchParameters(searchParameters: SearchParameters) {
        this.searchParameters = searchParameters

        thinkToNodes = searchParameters.nodes
        thinkToDepth = searchParameters.depth
        thinkToTime = searchParameters.calculateMoveTime(engineIsWhite, startTime, panicTime)
    }

    /**
     * Gets the principal variation from the transposition table
     */
    private fun getPv(firstMove: Int): String {
        if (firstMove == Move.NONE) {
            return ""
        }

        val sb = StringBuilder()
        val keys = ArrayList<Long>() // To not repeat keys
        sb.append(Move.toString(firstMove))
        val savedMoveNumber = board.moveNumber
        board.doMove(firstMove, true, false)

        var i = 1
        while (i < 256) {
            if (tt.search(board, i, false)) {
                if (tt.bestMove == Move.NONE || keys.contains(board.getKey())) {
                    break
                }
                keys.add(board.getKey())
                if (i == 1) {
                    globalPonderMove = tt.bestMove
                }
                sb.append(" ")
                sb.append(Move.toString(tt.bestMove))
                board.doMove(tt.bestMove, true, false)
                i++
            } else {
                break
            }
        }

        // Now undo moves
        board.undoMove(savedMoveNumber)
        return sb.toString()
    }

    open fun stop() {
        stop = true
        thinkToTime = 0
        thinkToNodes = 0
        thinkToDepth = 0
    }

    fun evaluateDraw(distanceToInitialPly: Int): Int {
        val nonPawnMat = BitboardUtils.popCount(board.knights) * Evaluator.KNIGHT +
                BitboardUtils.popCount(board.bishops) * Evaluator.BISHOP +
                BitboardUtils.popCount(board.rooks) * Evaluator.ROOK +
                BitboardUtils.popCount(board.queens) * Evaluator.QUEEN
        val gamePhase = if (nonPawnMat >= Evaluator.NON_PAWN_MATERIAL_MIDGAME_MAX)
            Evaluator.GAME_PHASE_MIDGAME
        else if (nonPawnMat <= Evaluator.NON_PAWN_MATERIAL_ENDGAME_MIN)
            Evaluator.GAME_PHASE_ENDGAME
        else
            (nonPawnMat - Evaluator.NON_PAWN_MATERIAL_ENDGAME_MIN) * Evaluator.GAME_PHASE_MIDGAME / (Evaluator.NON_PAWN_MATERIAL_MIDGAME_MAX - Evaluator.NON_PAWN_MATERIAL_ENDGAME_MIN)

        return (if (distanceToInitialPly and 1 == 0) -config.contemptFactor else config.contemptFactor) * gamePhase / Evaluator.GAME_PHASE_MIDGAME
    }

    private fun valueMatedIn(distanceToInitialPly: Int): Int {
        return -Evaluator.MATE + distanceToInitialPly
    }

    private fun valueMateIn(distanceToInitialPly: Int): Int {
        return Evaluator.MATE - distanceToInitialPly
    }

    /**
     * We are informed of the score produced by the move at any level
     */
    fun historyGood(node: Node, move: Int, depth: Int) {
        // removes captures and promotions from killers
        if (move == Move.NONE || Move.isTactical(move)) {
            return
        }

        if (move != node.killerMove1) {
            node.killerMove2 = node.killerMove1
            node.killerMove1 = move
        }

        val pieceMoved = Move.getPieceMoved(move) - 1
        val toIndex = Move.getToIndex(move)

        val v = history[pieceMoved][toIndex].toInt()
        history[pieceMoved][toIndex] = (v + ((HISTORY_MAX - v) * depth shr 8)).toShort()
    }

    fun historyBad(move: Int, depth: Int) {
        if (move == Move.NONE || Move.isTactical(move)) {
            return
        }

        val pieceMoved = Move.getPieceMoved(move) - 1
        val toIndex = Move.getToIndex(move)

        val v = history[pieceMoved][toIndex].toInt()
        history[pieceMoved][toIndex] = (v + ((HISTORY_MIN - v) * depth shr 8)).toShort()
    }

    private fun printNodeTree(distanceToInitialPly: Int) {
        for (i in 0..distanceToInitialPly) {
            val node = nodes[i]
            print(Move.toString(node.move) + " (" + Move.toString(node.ttMove) + " " + node.staticEval + ") ")
        }
        println()
    }

    companion object {
        private val logger = Logger.getLogger("SearchEngine")

        val MAX_DEPTH = 64
        val VALUE_IS_MATE = Evaluator.MATE - MAX_DEPTH

        private val NODE_ROOT = 0
        private val NODE_PV = 1
        private val NODE_NULL = 2
        private val PLY = 1

        val HISTORY_MAX = Short.MAX_VALUE - 1
        val HISTORY_MIN = Short.MIN_VALUE + 1
        // The bigger the treshold, the more moves are pruned
        private val HISTORY_PRUNING_TRESHOLD = intArrayOf(7000, 5994, 5087, 5724)

        private val LMR_DEPTHS_NOT_REDUCED = 3 * PLY
        private val SINGULAR_MOVE_DEPTH = intArrayOf(0, 6 * PLY, 8 * PLY) // By node type
        private val IID_DEPTH = intArrayOf(5 * PLY, 5 * PLY, 8 * PLY)

        private val IID_MARGIN = 150
        private val SINGULAR_EXTENSION_MARGIN_PER_PLY = 1
        private val ASPIRATION_WINDOW_SIZES = intArrayOf(10, 25, 150, 400, 550, 1025)
        private val FUTILITY_MARGIN_QS = 50

        // Margins by depthRemaining in PLYs
        private val RAZORING_MARGIN = intArrayOf(0, 225, 230, 235) // [0] is not used
        private val FUTILITY_MARGIN_CHILD = intArrayOf(0, 80, 160, 240) // [0] is not used
        private val FUTILITY_MARGIN_PARENT = intArrayOf(100, 180, 260, 340, 420, 500)

        private val NODE_EVAL_DIFF_MIN = -250
        private val NODE_EVAL_DIFF_MAX = 250

        // Aspiration window
        private var aspirationWindowProbe: Long = 0
        private var aspirationWindowHit: Long = 0

        // Futility pruning
        private var futilityHit: Long = 0

        // Razoring
        private var razoringProbe: Long = 0
        private var razoringHit: Long = 0

        // Singular Extension
        private var singularExtensionProbe: Long = 0
        private var singularExtensionHit: Long = 0

        // Null Move
        private var nullMoveProbe: Long = 0
        private var nullMoveHit: Long = 0

        // Transposition Table
        private var ttProbe: Long = 0
        private var ttPvHit: Long = 0
        private var ttLBHit: Long = 0
        private var ttUBHit: Long = 0
        private var ttEvalHit: Long = 0
        private var ttEvalProbe: Long = 0
    }
}