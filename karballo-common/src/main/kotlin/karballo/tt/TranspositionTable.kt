package karballo.tt

import karballo.Board
import karballo.Move
import karballo.bitboard.BitboardUtils
import karballo.log.Logger
import karballo.search.SearchEngine
import karballo.util.Utils

/**
 * Transposition table using two keys and multiprobe
 *
 * It uses part of the board's zobrist key (shifted) as the index

 * @author rui
 */
class TranspositionTable(sizeMb: Int) {

    var keys: LongArray
    var infos: LongArray
    var evals: ShortArray

    private val size: Int
    private var info: Long = 0
    private var eval: Short = 0
    private var generation: Int = 0
    private var entriesOccupied: Int = 0

    var score: Int = 0
        private set
    private val sizeBits: Int = BitboardUtils.square2Index(sizeMb.toLong()) + 16

    init {
        size = 1 shl sizeBits
        keys = LongArray(size)
        infos = LongArray(size)
        evals = ShortArray(size)
        entriesOccupied = 0

        generation = 0
        logger.debug("Created transposition table, size = " + size + " slots " + size * 18.0 / (1024 * 1024) + " MBytes")
    }

    fun clear() {
        entriesOccupied = 0
        Utils.instance.arrayFill(keys, 0)
    }

    fun search(board: Board, distanceToInitialPly: Int, exclusion: Boolean): Boolean {
        info = 0
        score = 0
        val startIndex = (if (exclusion) board.exclusionKey else board.getKey()).ushr(64 - sizeBits).toInt()
        // Verifies that it is really this board
        var i = startIndex
        while (i < startIndex + MAX_PROBES && i < size) {
            if (keys[i] == board.key2) {
                info = infos[i]
                eval = evals[i]
                score = (info.ushr(48) and 0xffff).toShort().toInt()

                // Fix mate score with the real distance to the initial PLY
                if (score >= SearchEngine.VALUE_IS_MATE) {
                    score -= distanceToInitialPly
                } else if (score <= -SearchEngine.VALUE_IS_MATE) {
                    score += distanceToInitialPly
                }
                return true
            }
            i++
        }
        return false
    }

    val bestMove: Int
        get() = (info and 0x1fffff).toInt()

    val nodeType: Int
        get() = (info.ushr(21) and 0xf).toInt()

    fun getGeneration(): Int {
        return (info.ushr(32) and 0xff).toInt()
    }

    val depthAnalyzed: Int
        get() {
            val depthAnalyzed = info.ushr(40).toInt() and 0xff
            return if (depthAnalyzed == 0xff) -1 else depthAnalyzed
        }

    fun getEval(): Int {
        return eval.toInt()
    }

    fun newGeneration() {
        generation = generation + 1 and 0xff
    }

    val isMyGeneration: Boolean
        get() = getGeneration() == generation

    operator fun set(board: Board, nodeType: Int, distanceToInitialPly: Int, depthAnalyzed: Int, bestMoveInVal: Int, scoreInVal: Int, eval: Int, exclusion: Boolean) {
        var scoreVar = scoreInVal
        var bestMoveVar = bestMoveInVal
        val key2 = board.key2
        val startIndex = (if (exclusion) board.exclusionKey else board.getKey()).ushr(64 - sizeBits).toInt()
        var replaceIndex = startIndex
        var replaceImportance = Int.MAX_VALUE // A higher value, so the first entry will be the default

        // Fix mate score with the real distance to mate from the current PLY, not from the initial PLY
        if (scoreVar >= SearchEngine.VALUE_IS_MATE) {
            scoreVar += distanceToInitialPly
        } else if (scoreVar <= -SearchEngine.VALUE_IS_MATE) {
            scoreVar -= distanceToInitialPly
        }

//        assert(score >= -Evaluator.MATE && score <= Evaluator.MATE) { "Fixed TT score is outside limits" }
//        assert(Math.abs(eval) < SearchEngine.VALUE_IS_MATE || Math.abs(eval) == Evaluator.MATE || eval == Evaluator.NO_VALUE) { "Storing a eval value in the TT outside limits" }

        var i = startIndex
        while (i < startIndex + MAX_PROBES && i < size) {
            info = infos[i]

            if (keys[i] == 0L) { // Replace an empty TT position
                entriesOccupied++
                replaceIndex = i
                break
            } else if (keys[i] == key2) { // Replace the same position
                replaceIndex = i
                if (bestMoveVar == Move.NONE) { // Keep previous best move
                    bestMoveVar = bestMove
                }
                break
            }

            // Calculates a value with this TT entry importance
            val entryImportance = if (nodeType == TYPE_EXACT_SCORE) 10 else 0 // Bonus for the PV entries
            -generationDelta // The older the generation, the less importance
            +depthAnalyzed // The more depth, the more importance

            // We will replace the less important entry
            if (entryImportance < replaceImportance) {
                replaceImportance = entryImportance
                replaceIndex = i
            }
            i++
        }

        keys[replaceIndex] = key2
        info = (bestMoveVar and 0x1fffff).toLong() or (nodeType and 0xf shl 21).toLong() or ((generation and 0xff).toLong() shl 32) or ((depthAnalyzed and 0xff).toLong() shl 40) or ((scoreVar and 0xffff).toLong() shl 48)

        infos[replaceIndex] = info
        evals[replaceIndex] = eval.toShort()
    }

    /**
     * Returns the difference between the current generation and the entry generation (max 255)
     */
    private val generationDelta: Int
        get() {
            val entryGeneration = (info.ushr(32) and 0xff).toByte()
            return if (generation >= entryGeneration) generation - entryGeneration else 256 + generation - entryGeneration
        }

    val hashFull: Int
        get() = (1000L * entriesOccupied / size).toInt()

    companion object {
        private val logger = Logger.getLogger("TranspositionTable")

        val DEPTH_QS_CHECKS = 0
        val DEPTH_QS_NO_CHECKS = -1

        val TYPE_EVAL = 0
        val TYPE_EXACT_SCORE = 1
        val TYPE_FAIL_LOW = 2
        val TYPE_FAIL_HIGH = 3

        private val MAX_PROBES = 4
    }
}