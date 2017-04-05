package karballo.search

import karballo.Board
import karballo.Color
import karballo.Move
import karballo.Piece
import karballo.bitboard.AttacksInfo
import karballo.bitboard.BitboardAttacks
import karballo.bitboard.BitboardUtils

/**
 * The Move Iterator generates moves as needed. It is separated into phases.
 * It sets the check flag on moves. It also checks if the move is legal before generating it.
 */
class MoveIterator(private var searchEngine: SearchEngine?, private val ai: AttacksInfo, private val depth: Int) {

    private var board: Board

    private var ttMove: Int = 0
    private var movesToGenerate: Int = 0

    private var move: Int = 0
    var lastMoveSee: Int = SEE_NOT_CALCULATED
        get() {
            if (field == SEE_NOT_CALCULATED) {
                field = board.see(move, ai)
            }
            return field
        }

    var lastMoveScore: Int = 0
        private set
    var lastMoveIsKiller: Boolean = false
        private set
    private var killer1: Int = 0
    private var killer2: Int = 0
    private var killer3: Int = 0
    private var killer4: Int = 0
    private var foundKiller1: Boolean = false
    private var foundKiller2: Boolean = false
    private var foundKiller3: Boolean = false
    private var foundKiller4: Boolean = false

    var checkEvasion: Boolean = false

    private var us: Int = 0
    private var them: Int = 0
    private var turn: Boolean = false
    private var all: Long = 0
    private var mines: Long = 0
    private var others: Long = 0

    private var goodCaptureIndex: Int = 0
    private var equalCaptureIndex: Int = 0
    private var badCaptureIndex: Int = 0
    private var nonCaptureIndex: Int = 0

    private val goodCaptures = IntArray(256) // Stores captures and queen promotions
    private val goodCapturesSee = IntArray(256)
    private val goodCapturesScores = IntArray(256)
    private val badCaptures = IntArray(256) // Stores captures and queen promotions
    private val badCapturesScores = IntArray(256)
    private val equalCaptures = IntArray(256) // Stores captures and queen promotions
    private val equalCapturesSee = IntArray(256)
    private val equalCapturesScores = IntArray(256)
    private val nonCaptures = IntArray(256) // Stores non captures and underpromotions
    private val nonCapturesSee = IntArray(256)
    private val nonCapturesScores = IntArray(256)
    private var phase: Int = 0

    private var bbAttacks: BitboardAttacks? = null

    init {
        this.board = searchEngine!!.board
        bbAttacks = BitboardAttacks.getInstance()
    }

    fun destroy() {
        searchEngine = null
        bbAttacks = null
    }

    fun genMoves(ttMove: Int, movesToGenerate: Int = GENERATE_ALL) {
        this.ttMove = ttMove
        this.movesToGenerate = movesToGenerate

        phase = PHASE_TT
        checkEvasion = board.check
        lastMoveSee = SEE_NOT_CALCULATED
        lastMoveIsKiller = false
    }

    private fun initMoveGen() {
        ai.build(board)

        killer1 = searchEngine!!.nodes[depth].killerMove1
        killer2 = searchEngine!!.nodes[depth].killerMove2
        killer3 = if (depth < 2) Move.NONE else searchEngine!!.nodes[depth - 2].killerMove1
        killer4 = if (depth < 2) Move.NONE else searchEngine!!.nodes[depth - 2].killerMove2

        foundKiller1 = false
        foundKiller2 = false
        foundKiller3 = false
        foundKiller4 = false

        goodCaptureIndex = 0
        badCaptureIndex = 0
        equalCaptureIndex = 0
        nonCaptureIndex = 0

        // Only for clarity
        turn = board.turn
        us = if (turn) Color.W else Color.B
        them = if (turn) Color.B else Color.W
        all = board.all
        mines = board.mines
        others = board.others
    }

    operator fun next(): Int {
        while (true) {
            when (phase) {
                PHASE_TT -> {
                    phase++
                    if (ttMove != Move.NONE) {
                        move = ttMove
                        if (checkEvasion
                                || movesToGenerate == GENERATE_ALL
                                || Move.getMoveType(move) == Move.TYPE_PROMOTION_QUEEN
                                || movesToGenerate == GENERATE_CAPTURES_PROMOS && Move.isCapture(move) && lastMoveSee >= 0 // TODO is it calling lastMoveSee getter ??

                                || movesToGenerate == GENERATE_CAPTURES_PROMOS_CHECKS && Move.isCaptureOrCheck(move) && lastMoveSee >= 0) {
                            return move
                        }
                    }
                }

                PHASE_GEN_CAPTURES -> {
                    initMoveGen()
                    if (checkEvasion) {
                        generateCheckEvasionCaptures()
                    } else {
                        generateCaptures()
                    }
                    phase++
                }

                PHASE_GOOD_CAPTURES_AND_PROMOS -> {
                    move = pickMoveFromArray(goodCaptureIndex, goodCaptures, goodCapturesScores, goodCapturesSee)
                    if (move != Move.NONE) {
                        return move
                    }
                    phase++
                }

                PHASE_EQUAL_CAPTURES -> {
                    move = pickMoveFromArray(equalCaptureIndex, equalCaptures, equalCapturesScores, equalCapturesSee)
                    if (move != Move.NONE) {
                        return move
                    }
                    phase++
                }

                PHASE_GEN_NON_CAPTURES -> {
                    if (checkEvasion) {
                        generateCheckEvasionsNonCaptures()
                    } else {
                        if (movesToGenerate == GENERATE_CAPTURES_PROMOS) {
                            phase = PHASE_END
                            return Move.NONE
                        }
                        generateNonCaptures()
                    }
                    phase++
                }

                PHASE_KILLER1 -> {
                    phase++
                    lastMoveIsKiller = true
                    if (foundKiller1) {
                        move = killer1
                        lastMoveSee = SEE_NOT_CALCULATED
                        return move
                    }
                }

                PHASE_KILLER2 -> {
                    phase++
                    if (foundKiller2) {
                        move = killer2
                        lastMoveSee = SEE_NOT_CALCULATED
                        return move
                    }
                }

                PHASE_KILLER3 -> {
                    phase++
                    if (foundKiller3) {
                        move = killer3
                        lastMoveSee = SEE_NOT_CALCULATED
                        return move
                    }
                }

                PHASE_KILLER4 -> {
                    phase++
                    if (foundKiller4) {
                        move = killer4
                        lastMoveSee = SEE_NOT_CALCULATED
                        return move
                    }
                }

                PHASE_NON_CAPTURES -> {
                    lastMoveIsKiller = false
                    move = pickMoveFromArray(nonCaptureIndex, nonCaptures, nonCapturesScores, nonCapturesSee)
                    if (move != Move.NONE) {
                        return move
                    }
                    phase++
                }

                PHASE_BAD_CAPTURES -> {
                    move = pickMoveFromArray(badCaptureIndex, badCaptures, badCapturesScores, badCapturesScores)
                    if (move != Move.NONE) {
                        return move
                    }
                    phase = PHASE_END
                    return Move.NONE
                }
            }
        }
    }

    private fun pickMoveFromArray(arrayLength: Int, arrayMoves: IntArray, arrayScores: IntArray, arraySee: IntArray): Int {
        if (arrayLength == 0) {
            return Move.NONE
        }
        var maxScore = SCORE_LOWEST
        var bestIndex = -1
        for (i in 0..arrayLength - 1) {
            if (arrayScores[i] > maxScore) {
                maxScore = arrayScores[i]
                bestIndex = i
            }
        }
        if (bestIndex != -1) {
            val move = arrayMoves[bestIndex]
            lastMoveSee = arraySee[bestIndex]
            lastMoveScore = maxScore
            arrayScores[bestIndex] = SCORE_LOWEST
            return move
        } else {
            return Move.NONE
        }
    }

    fun setBoard(board: Board) {
        this.board = board
    }

    /**
     * Generates captures and good promos
     */
    fun generateCaptures() {
        var square = 0x1L
        for (index in 0..63) {
            if (square and mines != 0L) {
                if (square and board.rooks != 0L) { // Rook
                    generateMovesFromAttacks(Piece.ROOK, index, square, ai.attacksFromSquare[index] and others, true)
                } else if (square and board.bishops != 0L) { // Bishop
                    generateMovesFromAttacks(Piece.BISHOP, index, square, ai.attacksFromSquare[index] and others, true)
                } else if (square and board.queens != 0L) { // Queen
                    generateMovesFromAttacks(Piece.QUEEN, index, square, ai.attacksFromSquare[index] and others, true)
                } else if (square and board.kings != 0L) { // King
                    generateMovesFromAttacks(Piece.KING, index, square, ai.attacksFromSquare[index] and others and ai.attackedSquaresAlsoPinned[them].inv(), true)
                } else if (square and board.knights != 0L) { // Knight
                    generateMovesFromAttacks(Piece.KNIGHT, index, square, ai.attacksFromSquare[index] and others, true)
                } else if (square and board.pawns != 0L) { // Pawns
                    if (turn) {
                        generatePawnCapturesOrGoodPromos(index, square, //
                                ai.attacksFromSquare[index] and (others or board.passantSquare) //
                                        or if (square and BitboardUtils.b2_u != 0L && square shl 8 and all == 0L) square shl 8 else 0, // Pushes only if promotion
                                board.passantSquare)
                    } else {
                        generatePawnCapturesOrGoodPromos(index, square, //
                                ai.attacksFromSquare[index] and (others or board.passantSquare) //
                                        or if (square and BitboardUtils.b2_d != 0L && square.ushr(8) and all == 0L) square.ushr(8) else 0, // Pushes only if promotion
                                board.passantSquare)
                    }
                }
            }
            square = square shl 1
        }
    }

    /**
     * Generates non tactical moves
     */
    fun generateNonCaptures() {
        // Castling: disabled when in check or king route attacked
        if (!board.check) {
            val kingCastlingDestination = board.canCastleKingSide(us, ai)
            if (kingCastlingDestination != 0L) {
                addMove(Piece.KING, ai.kingIndex[us], board.kings and mines, kingCastlingDestination, false, Move.TYPE_KINGSIDE_CASTLING)
            }
            val queenCastlingDestination = board.canCastleQueenSide(us, ai)
            if (queenCastlingDestination != 0L) {
                addMove(Piece.KING, ai.kingIndex[us], board.kings and mines, queenCastlingDestination, false, Move.TYPE_QUEENSIDE_CASTLING)
            }
        }

        var square = 0x1L
        for (index in 0..63) {
            if (square and mines != 0L) {
                if (square and board.rooks != 0L) { // Rook
                    generateMovesFromAttacks(Piece.ROOK, index, square, ai.attacksFromSquare[index] and all.inv(), false)
                } else if (square and board.bishops != 0L) { // Bishop
                    generateMovesFromAttacks(Piece.BISHOP, index, square, ai.attacksFromSquare[index] and all.inv(), false)
                } else if (square and board.queens != 0L) { // Queen
                    generateMovesFromAttacks(Piece.QUEEN, index, square, ai.attacksFromSquare[index] and all.inv(), false)
                } else if (square and board.kings != 0L) { // King
                    generateMovesFromAttacks(Piece.KING, index, square, ai.attacksFromSquare[index] and all.inv() and ai.attackedSquaresAlsoPinned[them].inv(), false)
                } else if (square and board.knights != 0L) { // Knight
                    generateMovesFromAttacks(Piece.KNIGHT, index, square, ai.attacksFromSquare[index] and all.inv(), false)
                }
                if (square and board.pawns != 0L) { // Pawns excluding the already generated promos
                    if (turn) {
                        generatePawnNonCapturesAndBadPromos(index, square, (if (square shl 8 and all == 0L) square shl 8 else 0) or if (square and BitboardUtils.b2_d != 0L && square shl 8 or (square shl 16) and all == 0L) square shl 16 else 0)
                    } else {
                        generatePawnNonCapturesAndBadPromos(index, square, (if (square.ushr(8) and all == 0L) square.ushr(8) else 0) or if (square and BitboardUtils.b2_u != 0L && square.ushr(8) or square.ushr(16) and all == 0L) square.ushr(16) else 0)
                    }
                }
            }
            square = square shl 1
        }
    }

    fun generateCheckEvasionCaptures() {
        // King can capture one of the checking pieces if two pieces giving check
        generateMovesFromAttacks(Piece.KING, ai.kingIndex[us], board.kings and mines, others and ai.attacksFromSquare[ai.kingIndex[us]] and ai.attackedSquaresAlsoPinned[them].inv(), true)

        if (BitboardUtils.popCount(ai.piecesGivingCheck) == 1) {
            var square: Long = 1
            for (index in 0..63) {
                if (square and mines != 0L && square and board.kings == 0L) {
                    if (square and board.pawns != 0L) { // Pawns
                        var destinySquares: Long = 0
                        // Good promotion interposes to the check
                        if ((square and if (turn) BitboardUtils.b2_u else BitboardUtils.b2_d) != 0L) { // Pawn about to promote
                            destinySquares = ai.interposeCheckSquares and if (turn) if (square shl 8 and all == 0L) square shl 8 else 0 else if (square.ushr(8) and all == 0L) square.ushr(8) else 0
                        }
                        // Pawn captures the checking piece
                        destinySquares = destinySquares or (ai.attacksFromSquare[index] and ai.piecesGivingCheck)
                        if (destinySquares != 0L) {
                            generatePawnCapturesOrGoodPromos(index, square, destinySquares, board.passantSquare)
                        } else if (board.passantSquare != 0L && ai.attacksFromSquare[index] and board.passantSquare != 0L) { // This pawn can capture to the passant square
                            val testPassantSquare = if (turn) ai.piecesGivingCheck shl 8 else ai.piecesGivingCheck.ushr(8)
                            if (testPassantSquare == board.passantSquare || // En-passant capture target giving check
                                    board.passantSquare and ai.interposeCheckSquares != 0L) { // En passant capture to interpose
                                addMove(Piece.PAWN, index, square, board.passantSquare, true, Move.TYPE_PASSANT)
                            }
                        }
                    } else {
                        if (ai.attacksFromSquare[index] and ai.piecesGivingCheck != 0L) {
                            if (square and board.rooks != 0L) { // Rook
                                generateMovesFromAttacks(Piece.ROOK, index, square, ai.piecesGivingCheck, true)
                            } else if (square and board.bishops != 0L) { // Bishop
                                generateMovesFromAttacks(Piece.BISHOP, index, square, ai.piecesGivingCheck, true)
                            } else if (square and board.queens != 0L) { // Queen
                                generateMovesFromAttacks(Piece.QUEEN, index, square, ai.piecesGivingCheck, true)
                            } else if (square and board.knights != 0L) { // Knight
                                generateMovesFromAttacks(Piece.KNIGHT, index, square, ai.piecesGivingCheck, true)
                            }
                        }
                    }
                }
                square = square shl 1
            }
        }
    }

    fun generateCheckEvasionsNonCaptures() {
        // Moving king (without captures)
        generateMovesFromAttacks(Piece.KING, ai.kingIndex[us], board.kings and mines, ai.attacksFromSquare[ai.kingIndex[us]] and all.inv() and ai.attackedSquaresAlsoPinned[them].inv(), false)

        // Interpose: Cannot interpose with more than one piece giving check
        if (BitboardUtils.popCount(ai.piecesGivingCheck) == 1) {
            var square: Long = 1
            for (index in 0..63) {
                if (square and mines != 0L && square and board.kings == 0L) {
                    if (square and board.pawns != 0L) {
                        val destinySquares: Long
                        if (turn) {
                            destinySquares = ai.interposeCheckSquares and ((if (square shl 8 and all == 0L) square shl 8 else 0) or if (square and BitboardUtils.b2_d != 0L && square shl 8 or (square shl 16) and all == 0L) square shl 16 else 0)
                        } else {
                            destinySquares = ai.interposeCheckSquares and ((if (square.ushr(8) and all == 0L) square.ushr(8) else 0) or if (square and BitboardUtils.b2_u != 0L && square.ushr(8) or square.ushr(16) and all == 0L) square.ushr(16) else 0)
                        }
                        if (destinySquares != 0L) {
                            generatePawnNonCapturesAndBadPromos(index, square, destinySquares)
                        }
                    } else {
                        val destinySquares = ai.attacksFromSquare[index] and ai.interposeCheckSquares and all.inv()
                        if (destinySquares != 0L) {
                            if (square and board.rooks != 0L) { // Rook
                                generateMovesFromAttacks(Piece.ROOK, index, square, destinySquares, false)
                            } else if (square and board.bishops != 0L) { // Bishop
                                generateMovesFromAttacks(Piece.BISHOP, index, square, destinySquares, false)
                            } else if (square and board.queens != 0L) { // Queen
                                generateMovesFromAttacks(Piece.QUEEN, index, square, destinySquares, false)
                            } else if (square and board.knights != 0L) { // Knight
                                generateMovesFromAttacks(Piece.KNIGHT, index, square, destinySquares, false)
                            }
                        }
                    }
                }
                square = square shl 1
            }
        }
    }

    /**
     * Generates moves from an attack mask
     */
    private fun generateMovesFromAttacks(pieceMoved: Int, fromIndex: Int, from: Long, attacks: Long, capture: Boolean) {
        var a = attacks
        while (a != 0L) {
            val to = if (turn) BitboardUtils.msb(a) else BitboardUtils.lsb(a)
            addMove(pieceMoved, fromIndex, from, to, capture, 0)
            a = a xor to
        }
    }

    private fun generatePawnCapturesOrGoodPromos(fromIndex: Int, from: Long, attacks: Long, passant: Long) {
        var a = attacks
        if (ai.pinnedPieces and from != 0L) {
            a = a and ai.pinnedMobility[fromIndex] // Be careful with pawn advance moves, the pawn may be pinned
        }

        while (a != 0L) {
            val to = if (turn) BitboardUtils.msb(a) else BitboardUtils.lsb(a)
            if (to and passant != 0L) {
                addMove(Piece.PAWN, fromIndex, from, to, true, Move.TYPE_PASSANT)
            } else {
                val capture = to and others != 0L
                if (to and (BitboardUtils.b_u or BitboardUtils.b_d) != 0L) {
                    addMove(Piece.PAWN, fromIndex, from, to, capture, Move.TYPE_PROMOTION_QUEEN)
                    // If it is a capture, we must add the underpromotions
                    if (capture) {
                        addMove(Piece.PAWN, fromIndex, from, to, true, Move.TYPE_PROMOTION_KNIGHT)
                        addMove(Piece.PAWN, fromIndex, from, to, true, Move.TYPE_PROMOTION_ROOK)
                        addMove(Piece.PAWN, fromIndex, from, to, true, Move.TYPE_PROMOTION_BISHOP)
                    }
                } else if (capture) {
                    addMove(Piece.PAWN, fromIndex, from, to, true, 0)
                }
            }
            a = a xor to
        }
    }

    private fun generatePawnNonCapturesAndBadPromos(fromIndex: Int, from: Long, attacks: Long) {
        var a = attacks
        if (ai.pinnedPieces and from != 0L) {
            a = a and ai.pinnedMobility[fromIndex] // Be careful with pawn advance moves, the pawn may be pinned
        }

        while (a != 0L) {
            val to = if (turn) BitboardUtils.msb(a) else BitboardUtils.lsb(a)
            if (to and (BitboardUtils.b_u or BitboardUtils.b_d) != 0L) {
                addMove(Piece.PAWN, fromIndex, from, to, false, Move.TYPE_PROMOTION_KNIGHT)
                addMove(Piece.PAWN, fromIndex, from, to, false, Move.TYPE_PROMOTION_ROOK)
                addMove(Piece.PAWN, fromIndex, from, to, false, Move.TYPE_PROMOTION_BISHOP)
            } else {
                addMove(Piece.PAWN, fromIndex, from, to, false, 0)
            }
            a = a xor to
        }
    }

    private fun addMove(pieceMoved: Int, fromIndex: Int, from: Long, to: Long, capture: Boolean, moveType: Int) {
        val toIndex = BitboardUtils.square2Index(to)

        //
        // Verify check and legality
        //
        var check = false
        val newMyKingIndex: Int
        var rookSlidersAfterMove: Long
        var allAfterMove: Long
        val minesAfterMove: Long
        var bishopSlidersAfterMove = board.bishops or board.queens and from.inv() and to.inv()
        var squaresForDiscovery = from

        if (moveType == Move.TYPE_KINGSIDE_CASTLING || moveType == Move.TYPE_QUEENSIDE_CASTLING) {
            // {White Kingside, White Queenside, Black Kingside, Black Queenside}
            val j = (if (turn) 0 else 2) + if (moveType == Move.TYPE_QUEENSIDE_CASTLING) 1 else 0

            newMyKingIndex = Board.CASTLING_KING_DESTINY_INDEX[j]
            // Castling has a special "to" in Chess960 where the destiny square is the rook
            val kingTo = Board.CASTLING_KING_DESTINY_SQUARE[j]
            val rookTo = Board.CASTLING_ROOK_DESTINY_SQUARE[j]
            val rookMoveMask = board.castlingRooks[j] xor rookTo

            rookSlidersAfterMove = board.rooks xor rookMoveMask or board.queens
            allAfterMove = all xor rookMoveMask or kingTo and from.inv()
            minesAfterMove = mines xor rookMoveMask or kingTo and from.inv()

            // Direct check by rook
            check = check or (rookTo and ai.rookAttacksKing[them] != 0L)
        } else {
            if (pieceMoved == Piece.KING) {
                newMyKingIndex = toIndex
            } else {
                newMyKingIndex = ai.kingIndex[us]
            }

            rookSlidersAfterMove = board.rooks or board.queens and from.inv() and to.inv()
            allAfterMove = all or to and from.inv()
            minesAfterMove = mines or to and from.inv()
            squaresForDiscovery = from

            if (moveType == Move.TYPE_PASSANT) {
                squaresForDiscovery = squaresForDiscovery or if (turn) to.ushr(8) else to shl 8
                allAfterMove = allAfterMove and squaresForDiscovery.inv()
            }

            // Direct checks
            if (pieceMoved == Piece.KNIGHT || moveType == Move.TYPE_PROMOTION_KNIGHT) {
                check = to and bbAttacks!!.knight[ai.kingIndex[them]] != 0L
            } else if (pieceMoved == Piece.BISHOP || moveType == Move.TYPE_PROMOTION_BISHOP) {
                check = to and ai.bishopAttacksKing[them] != 0L
                bishopSlidersAfterMove = bishopSlidersAfterMove or to
            } else if (pieceMoved == Piece.ROOK || moveType == Move.TYPE_PROMOTION_ROOK) {
                check = to and ai.rookAttacksKing[them] != 0L
                rookSlidersAfterMove = rookSlidersAfterMove or to
            } else if (pieceMoved == Piece.QUEEN || moveType == Move.TYPE_PROMOTION_QUEEN) {
                check = to and (ai.bishopAttacksKing[them] or ai.rookAttacksKing[them]) != 0L
                bishopSlidersAfterMove = bishopSlidersAfterMove or to
                rookSlidersAfterMove = rookSlidersAfterMove or to
            } else if (pieceMoved == Piece.PAWN) {
                check = to and bbAttacks!!.pawn[them][ai.kingIndex[them]] != 0L
            }
        }

        /**
         * As AttacksInfo already excludes pinned pieces, we only must take care from en passant captures
         * (they can remove two pieces from 4th rank discovering a check) and king moves when the king is
         * in check by a slider
         */
        if (squaresForDiscovery and ai.mayPin[them] != 0L && (moveType == Move.TYPE_PASSANT || ai.piecesGivingCheck and (board.rooks or board.bishops or board.queens) != 0L && pieceMoved == Piece.KING)) {
            // Candidates to leave the king in check after moving
            if (squaresForDiscovery and ai.bishopAttacksKing[us] != 0L || ai.piecesGivingCheck and (board.bishops or board.queens) != 0L && pieceMoved == Piece.KING) { // Moving the king when the king is in check by a slider
                // Regenerate bishop attacks to my king
                val newBishopAttacks = bbAttacks!!.getBishopAttacks(newMyKingIndex, allAfterMove)
                if (newBishopAttacks and bishopSlidersAfterMove and minesAfterMove.inv() != 0L) {
                    return  // Illegal move
                }
            }
            if (squaresForDiscovery and ai.rookAttacksKing[us] != 0L || ai.piecesGivingCheck and (board.rooks or board.queens) != 0L && pieceMoved == Piece.KING) {
                // Regenerate rook attacks to my king
                val newRookAttacks = bbAttacks!!.getRookAttacks(newMyKingIndex, allAfterMove)
                if (newRookAttacks and rookSlidersAfterMove and minesAfterMove.inv() != 0L) {
                    return  // Illegal move
                }
            }
        }

        // After a promotion to queen or rook there are new sliders transversing the origin square, so mayPin is not valid
        if (!check && (squaresForDiscovery and ai.mayPin[us] != 0L || moveType == Move.TYPE_PROMOTION_QUEEN || moveType == Move.TYPE_PROMOTION_ROOK || moveType == Move.TYPE_PROMOTION_BISHOP)) {
            // Discovered checks
            if (squaresForDiscovery and ai.bishopAttacksKing[them] != 0L) {
                // Regenerate bishop attacks to the other king
                val newBishopAttacks = bbAttacks!!.getBishopAttacks(ai.kingIndex[them], allAfterMove)
                if (newBishopAttacks and bishopSlidersAfterMove and minesAfterMove != 0L) {
                    check = true
                }
            }
            if (squaresForDiscovery and ai.rookAttacksKing[them] != 0L) {
                // Regenerate rook attacks to the other king
                val newRookAttacks = bbAttacks!!.getRookAttacks(ai.kingIndex[them], allAfterMove)
                if (newRookAttacks and rookSlidersAfterMove and minesAfterMove != 0L) {
                    check = true
                }
            }
        }

        // Generating checks, if the move is not a check, skip it
        if (movesToGenerate == GENERATE_CAPTURES_PROMOS_CHECKS && !checkEvasion && !check && !capture && moveType != Move.TYPE_PROMOTION_QUEEN) {
            return
        }

        // Now, with legality verified and the check flag, generate the move
        val move = Move.genMove(fromIndex, toIndex, pieceMoved, capture, check, moveType)
        if (move == ttMove) {
            return
        }
        if (!capture) {
            if (move == killer1) {
                foundKiller1 = true
                return
            } else if (move == killer2) {
                foundKiller2 = true
                return
            } else if (move == killer3) {
                foundKiller3 = true
                return
            } else if (move == killer4) {
                foundKiller4 = true
                return
            }
        }

        val pieceCaptured = if (capture) Move.getPieceCaptured(board, move) else 0
        var see = SEE_NOT_CALCULATED

        if (capture || movesToGenerate == GENERATE_CAPTURES_PROMOS_CHECKS && check) {
            // If there aren't pieces attacking the destiny square
            // and the piece cannot pin an attack to the see square,
            // the see will be the captured piece value
            if (ai.attackedSquares[them] and to == 0L && ai.mayPin[them] and from == 0L) {
                see = if (capture) Board.SEE_PIECE_VALUES[pieceCaptured] else 0
            } else {
                see = board.see(fromIndex, toIndex, pieceMoved, pieceCaptured)
            }
        }

        if (movesToGenerate != GENERATE_ALL && !checkEvasion && see < 0) {
            return
        }

        if (capture && see < 0) {
            badCaptures[badCaptureIndex] = move
            badCapturesScores[badCaptureIndex] = see
            badCaptureIndex++
            return
        }

        val underPromotion = moveType == Move.TYPE_PROMOTION_KNIGHT || moveType == Move.TYPE_PROMOTION_ROOK || moveType == Move.TYPE_PROMOTION_BISHOP

        if ((capture || moveType == Move.TYPE_PROMOTION_QUEEN) && !underPromotion) {
            // Order GOOD captures by MVV/LVA (Hyatt dixit)
            var score = 0
            if (capture) {
                score = VICTIM_PIECE_VALUES[pieceCaptured] - AGGRESSOR_PIECE_VALUES[pieceMoved]
            }
            if (moveType == Move.TYPE_PROMOTION_QUEEN) {
                score += SCORE_PROMOTION_QUEEN
            }
            if (see > 0 || moveType == Move.TYPE_PROMOTION_QUEEN) {
                goodCaptures[goodCaptureIndex] = move
                goodCapturesSee[goodCaptureIndex] = see
                goodCapturesScores[goodCaptureIndex] = score
                goodCaptureIndex++
            } else {
                equalCaptures[equalCaptureIndex] = move
                equalCapturesSee[equalCaptureIndex] = see
                equalCapturesScores[equalCaptureIndex] = score
                equalCaptureIndex++
            }
        } else {
            nonCaptures[nonCaptureIndex] = move
            nonCapturesSee[nonCaptureIndex] = see
            nonCapturesScores[nonCaptureIndex] = if (underPromotion) SCORE_UNDERPROMOTION else searchEngine!!.history[pieceMoved - 1][toIndex].toInt()
            nonCaptureIndex++
        }
    }

    companion object {
        //
        // Kind of moves to generate
        // In check evasions all moves are always generated
        val GENERATE_ALL = 0
        val GENERATE_CAPTURES_PROMOS = 1 // Generates only good/equal captures and queen promotions
        val GENERATE_CAPTURES_PROMOS_CHECKS = 2 // Generates only good/equal captures, queen promotions and checks
        //
        // Move generation phases
        //
        val PHASE_TT = 0
        val PHASE_GEN_CAPTURES = 1
        val PHASE_GOOD_CAPTURES_AND_PROMOS = 2
        val PHASE_EQUAL_CAPTURES = 3
        val PHASE_GEN_NON_CAPTURES = 4
        val PHASE_KILLER1 = 5
        val PHASE_KILLER2 = 6
        val PHASE_KILLER3 = 7
        val PHASE_KILLER4 = 8
        val PHASE_NON_CAPTURES = 9
        val PHASE_BAD_CAPTURES = 10
        val PHASE_END = 11

        private val VICTIM_PIECE_VALUES = intArrayOf(0, 100, 325, 330, 500, 975, 10000)
        private val AGGRESSOR_PIECE_VALUES = intArrayOf(0, 10, 32, 33, 50, 97, 99)
        private val SCORE_PROMOTION_QUEEN = 975
        private val SCORE_UNDERPROMOTION = Int.MIN_VALUE + 1
        private val SCORE_LOWEST = Int.MIN_VALUE

        val SEE_NOT_CALCULATED = Short.MAX_VALUE.toInt()
    }
}