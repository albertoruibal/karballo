package karballo.evaluation

import karballo.Board
import karballo.Color
import karballo.bitboard.BitboardAttacks
import karballo.bitboard.BitboardUtils

object Endgame {
    val SCALE_FACTOR_DRAW = 0
    val SCALE_FACTOR_DRAWISH = 100
    val SCALE_FACTOR_DEFAULT = 1000

    val closerSquares = intArrayOf(0, 0, 100, 80, 60, 40, 20, 10)

    private val toCorners = intArrayOf(
            100, 90, 80, 70, 70, 80, 90, 100,
            90, 70, 60, 50, 50, 60, 70, 90,
            80, 60, 40, 30, 30, 40, 60, 80,
            70, 50, 30, 20, 20, 30, 50, 70,
            70, 50, 30, 20, 20, 30, 50, 70,
            80, 60, 40, 30, 30, 40, 60, 80,
            90, 70, 60, 50, 50, 60, 70, 90,
            100, 90, 80, 70, 70, 80, 90, 100)

    private val toColorCorners = intArrayOf(200, 190, 180, 170, 160, 150, 140, 130, 190, 180, 170, 160, 150, 140, 130, 140, 180, 170, 155, 140, 140, 125, 140, 150, 170, 160, 140, 120, 110, 140, 150, 160, 160, 150, 140, 110, 120, 140, 160, 170, 150, 140, 125, 140, 140, 155, 170, 180, 140, 130, 140, 150, 160, 170, 180, 190, 130, 140, 150, 160, 170, 180, 190, 200)

    internal var kpkBitbase: KPKBitbase = KPKBitbase()

    /**
     * It may return a perfect knowledge value, a scaleFactor or nothing
     */
    fun evaluateEndgame(board: Board, scaleFactor: IntArray, whitePawns: Int, blackPawns: Int, whiteKnights: Int, blackKnights: Int, whiteBishops: Int, blackBishops: Int, whiteRooks: Int, blackRooks: Int, whiteQueens: Int, blackQueens: Int): Int {
        scaleFactor[0] = SCALE_FACTOR_DEFAULT

        // Endgame detection
        val whiteNoPawnMaterial = whiteKnights + whiteBishops + whiteRooks + whiteQueens
        val blackNoPawnMaterial = blackKnights + blackBishops + blackRooks + blackQueens
        val whiteMaterial = whiteNoPawnMaterial + whitePawns
        val blackMaterial = blackNoPawnMaterial + blackPawns

        // Do not put here draws already detected by the FIDE rules in the Board class

        if (whitePawns == 0 && blackPawns == 0) {
            //
            // Endgames without Pawns
            //

            if (blackMaterial == 0 && whiteMaterial == 2 && whiteKnights == 1 && whiteBishops == 1 || //
                    whiteMaterial == 0 && blackMaterial == 2 && blackKnights == 1 && blackBishops == 1) {
                return Endgame.endgameKBNK(board, whiteMaterial > blackMaterial)
            }
            if (whiteMaterial == 1 && blackMaterial == 1) {
                if (whiteRooks == 1 && blackRooks == 1) {
                    return Evaluator.DRAW
                }
                if (whiteQueens == 1 && blackQueens == 1) {
                    return Evaluator.DRAW
                }
            }

        } else if (whitePawns == 1 && blackPawns == 0 || whitePawns == 0 && blackPawns == 1) {
            //
            // Single pawn endings
            //

            if (whiteNoPawnMaterial == 0 && blackNoPawnMaterial == 0) {
                return Endgame.endgameKPK(board, whiteMaterial > blackMaterial)
            }

            // Only with a non-pawn piece
            if (whiteNoPawnMaterial == 1 && blackNoPawnMaterial == 0 || whiteNoPawnMaterial == 0 && blackNoPawnMaterial == 1) {
                if (whiteQueens == 1 && blackPawns == 1 || blackQueens == 1 && whitePawns == 1) {
                    return endgameKQKP(board, whiteQueens > blackQueens)
                }
            }

            // With a non-pawn piece by each side
            if (whiteNoPawnMaterial == 1 && blackNoPawnMaterial == 1) {
                if (whiteRooks == 1 && blackRooks == 1) {
                    scaleFactor[0] = scaleKRPKR(board, whitePawns > blackPawns)
                }
                if (whiteBishops == 1 && blackBishops == 1) {
                    return endgameKBPKB(board, whitePawns > blackPawns)
                }
                if (whiteBishops == 1 && whitePawns == 1 && blackKnights == 1 || blackBishops == 1 && blackPawns == 1 && whiteKnights == 1) {
                    return endgameKBPKN(board, whitePawns > blackPawns)
                }
            }
        }

        //
        // Other endgames
        //
        if (blackMaterial == 0 && (whiteBishops >= 2 || whiteRooks > 0 || whiteQueens > 0) || //
                whiteMaterial == 0 && (whiteBishops >= 2 || blackRooks > 0 || blackQueens > 0)) {
            return Endgame.endgameKXK(board, whiteMaterial > blackMaterial, whiteKnights + blackKnights, whiteBishops + blackBishops, whiteRooks + blackRooks, whiteQueens + blackQueens)
        }

        if (whiteRooks == 1 && blackRooks == 1 &&
                (whitePawns == 2 && blackPawns == 1 || whitePawns == 1 && blackPawns == 2)) {
            scaleFactor[0] = scaleKRPPKRP(board, whitePawns > blackPawns)
        }

        //
        // Interior node recognizer for draws
        //
        if (scaleFactor[0] == SCALE_FACTOR_DRAW) {
            return Evaluator.DRAW
        }
        return Evaluator.NO_VALUE
    }

    // One side does not have pieces, drives the king to the corners and try to approximate the kings
    private fun endgameKXK(board: Board, whiteDominant: Boolean, knights: Int, bishops: Int, rooks: Int, queens: Int): Int {
        val whiteKingIndex = BitboardUtils.square2Index(board.kings and board.whites)
        val blackKingIndex = BitboardUtils.square2Index(board.kings and board.blacks)
        val value = Evaluator.KNOWN_WIN +
                knights * Evaluator.KNIGHT +
                bishops * Evaluator.BISHOP +
                rooks * Evaluator.ROOK +
                queens * Evaluator.QUEEN +
                closerSquares[BitboardUtils.distance(whiteKingIndex, blackKingIndex)] + //

                if (whiteDominant) toCorners[blackKingIndex] else toCorners[whiteKingIndex]

        return if (whiteDominant) value else -value
    }

    // NB vs K must drive the king to the corner of the color of the bishop
    private fun endgameKBNK(board: Board, whiteDominant: Boolean): Int {
        var whiteKingIndex = BitboardUtils.square2Index(board.kings and board.whites)
        var blackKingIndex = BitboardUtils.square2Index(board.kings and board.blacks)

        if (BitboardUtils.isBlackSquare(board.bishops)) {
            whiteKingIndex = BitboardUtils.flipHorizontalIndex(whiteKingIndex)
            blackKingIndex = BitboardUtils.flipHorizontalIndex(blackKingIndex)
        }

        val value = Evaluator.KNOWN_WIN + closerSquares[BitboardUtils.distance(whiteKingIndex, blackKingIndex)] + //

                if (whiteDominant) toColorCorners[blackKingIndex] else toColorCorners[whiteKingIndex]

        return if (whiteDominant) value else -value
    }

    private fun endgameKPK(board: Board, whiteDominant: Boolean): Int {
        if (!kpkBitbase.probe(board)) {
            return Evaluator.DRAW
        }

        return if (whiteDominant)
            Evaluator.KNOWN_WIN + Evaluator.PAWN + BitboardUtils.getRankOfIndex(BitboardUtils.square2Index(board.pawns))
        else
            -Evaluator.KNOWN_WIN - Evaluator.PAWN - (7 - BitboardUtils.getRankOfIndex(BitboardUtils.square2Index(board.pawns)))
    }

    private fun scaleKRPKR(board: Board, whiteDominant: Boolean): Int {
        val dominantColor = if (whiteDominant) Color.W else Color.B
        //val dominantRook = board.rooks and if (whiteDominant) board.whites else board.blacks
        val otherRook = board.rooks and if (whiteDominant) board.blacks else board.whites

        val dominantKing = board.kings and if (whiteDominant) board.whites else board.blacks
        val otherKing = board.kings and if (whiteDominant) board.blacks else board.whites
        val dominantKingIndex = BitboardUtils.square2Index(dominantKing)

        val rank8 = if (whiteDominant) 7 else 0
        val rank7 = if (whiteDominant) 6 else 1
        val rank6 = if (whiteDominant) 5 else 2
        val rank2 = if (whiteDominant) 1 else 6

        val pawn = board.pawns
        val pawnIndex = BitboardUtils.square2Index(pawn)
        val pawnFileIndex = 7 - (pawnIndex and 7)
        val pawnFile = BitboardUtils.FILE[pawnFileIndex]
        val pawnFileAndAdjacents = BitboardUtils.FILE[pawnFileIndex] or BitboardUtils.FILES_ADJACENT[pawnFileIndex]

        // Philidor position
        if (BitboardUtils.RANKS_BACKWARD[dominantColor][rank6] and pawn != 0L // Pawn behind rank 6

                && BitboardUtils.RANKS_BACKWARD[dominantColor][rank6] and dominantKing != 0L // Dominant king behind rank 6

                && BitboardUtils.RANKS_FORWARD[dominantColor][rank6] and pawnFileAndAdjacents and otherKing != 0L // King defending promotion squares

                && BitboardUtils.RANK[rank6] and otherRook != 0L) { // Other rook in rank 6
            return SCALE_FACTOR_DRAW
        }
        // When the pawn is advanced to 6th, check the king from behind
        if (BitboardUtils.RANK[rank6] and pawn != 0L // Pawn in rank 6

                && BitboardUtils.RANKS_FORWARD[dominantColor][rank6] and pawnFileAndAdjacents and otherKing != 0L // King defending promotion squares

                && (BitboardUtils.RANK_AND_BACKWARD[dominantColor][rank2] and otherRook != 0L || board.turn != whiteDominant && BitboardUtils.distance(pawnIndex, dominantKingIndex) >= 3)) { // Rook ready to check from behind
            return SCALE_FACTOR_DRAW
        }
        // If the pawn is in advanced to 7th...
        if (BitboardUtils.RANK[rank7] and pawn != 0L
                && BitboardUtils.RANKS_FORWARD[dominantColor][rank6] and pawnFile and otherKing != 0L // King in the promotion squares

                && BitboardUtils.RANK_AND_BACKWARD[dominantColor][rank2] and otherRook != 0L // Rook must be already behind

                && (board.turn != whiteDominant || BitboardUtils.distance(pawnIndex, dominantKingIndex) >= 2)) {
            return SCALE_FACTOR_DRAW
        }
        // Back rank defense
        if (BitboardUtils.A or BitboardUtils.B or BitboardUtils.G or BitboardUtils.H and pawn != 0L
                && BitboardUtils.RANK[rank8] and pawnFileAndAdjacents and otherKing != 0L // King in rank 8 in front of the pawn

                && BitboardUtils.RANK[rank8] and otherRook != 0L) { // Defending rook in rank 8
            return SCALE_FACTOR_DRAW
        }

        return SCALE_FACTOR_DEFAULT
    }

    /**
     * This position may be a draw with a the pawn in a, c, f, h and in 7th with the defending king near
     */
    private fun endgameKQKP(board: Board, whiteDominant: Boolean): Int {
        val ranks12 = if (whiteDominant) BitboardUtils.R1 or BitboardUtils.R2 else BitboardUtils.R7 or BitboardUtils.R8
        val pawn = board.pawns
        val pawnZone: Long

        if (BitboardUtils.A and pawn != 0L) {
            pawnZone = BitboardUtils.FILES_LEFT[3] and ranks12
        } else if (BitboardUtils.C and pawn != 0L) {
            pawnZone = BitboardUtils.FILES_LEFT[4] and ranks12
        } else if (BitboardUtils.F and pawn != 0L) {
            pawnZone = BitboardUtils.FILES_RIGHT[3] and ranks12
        } else if (BitboardUtils.H and pawn != 0L) {
            pawnZone = BitboardUtils.FILES_RIGHT[4] and ranks12
        } else {
            return Evaluator.NO_VALUE
        }

        val dominantKing = board.kings and if (whiteDominant) board.whites else board.blacks
        val otherKing = board.kings and if (whiteDominant) board.blacks else board.whites

        val dominantKingIndex = BitboardUtils.square2Index(dominantKing)
        val pawnIndex = BitboardUtils.square2Index(pawn)

        if (pawnZone and otherKing != 0L && BitboardUtils.distance(dominantKingIndex, pawnIndex) >= 1) {
            return Evaluator.DRAW
        }

        return Evaluator.NO_VALUE
    }

    private fun endgameKBPKN(board: Board, whiteDominant: Boolean): Int {
        val dominantColor = if (whiteDominant) Color.W else Color.B
        val dominantBishop = board.bishops and if (whiteDominant) board.whites else board.blacks
        val dominantBishopSquares = BitboardUtils.getSameColorSquares(dominantBishop)

        val pawn = board.pawns
        val pawnRoute = BitboardUtils.frontFile(pawn, dominantColor)

        val otherKing = board.kings and if (whiteDominant) board.blacks else board.whites

        // Other king in front of the pawn in a square different than the bishop color: DRAW
        if (pawnRoute and otherKing != 0L && dominantBishopSquares and otherKing == 0L) {
            return Evaluator.DRAW
        }
        return Evaluator.NO_VALUE
    }

    private fun endgameKBPKB(board: Board, whiteDominant: Boolean): Int {
        val dominantColor = if (whiteDominant) Color.W else Color.B
        val dominantBishop = board.bishops and if (whiteDominant) board.whites else board.blacks
        val dominantBishopSquares = BitboardUtils.getSameColorSquares(dominantBishop)
        val otherBishop = board.bishops and if (whiteDominant) board.blacks else board.whites

        val pawn = board.pawns
        val pawnRoute = BitboardUtils.frontFile(pawn, dominantColor)

        val otherKing = board.kings and if (whiteDominant) board.blacks else board.whites

        // Other king in front of the pawn in a square different than the bishop color: DRAW
        if (pawnRoute and otherKing != 0L && dominantBishopSquares and otherKing == 0L) {
            return Evaluator.DRAW
        }

        // Different bishop colors
        val otherBishopSquares = BitboardUtils.getSameColorSquares(otherBishop)
        if (dominantBishopSquares != otherBishopSquares) {
            val otherBishopIndex = BitboardUtils.square2Index(otherBishop)
            if (otherBishop and pawnRoute != 0L ||
                    BitboardAttacks.getInstance().bishop[otherBishopIndex] and pawnRoute != 0L) {
                return Evaluator.DRAW
            }
        }

        return Evaluator.NO_VALUE
    }

    private fun scaleKRPPKRP(board: Board, whiteDominant: Boolean): Int {
        val dominantColor = if (whiteDominant) Color.W else Color.B
        val dominantPawns = board.pawns and if (whiteDominant) board.whites else board.blacks
        val p1Front = BitboardUtils.frontPawnSpan(BitboardUtils.lsb(dominantPawns), dominantColor)
        val p2Front = BitboardUtils.frontPawnSpan(BitboardUtils.msb(dominantPawns), dominantColor)
        val otherPawn = board.pawns and if (whiteDominant) board.blacks else board.whites

        // Check for a Passed Pawn
        if (p1Front and otherPawn == 0L || p2Front and otherPawn == 0L) {
            return SCALE_FACTOR_DEFAULT
        }
        // If the other king is in front of the pawns, it is drawish
        val otherKing = board.kings and if (whiteDominant) board.blacks else board.whites
        if (p1Front and otherKing != 0L && p2Front and otherKing != 0L) {
            return SCALE_FACTOR_DRAWISH
        }

        return SCALE_FACTOR_DEFAULT
    }
}