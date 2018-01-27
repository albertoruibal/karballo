package karballo.evaluation

import karballo.Board
import karballo.bitboard.AttacksInfo

/**
 * Piece square values from Tomasz Michniewski, got from:
 * http://chessprogramming.wikispaces.com/Simplified+evaluation+function

 * @author rui
 */
class SimplifiedEvaluator : Evaluator() {

    override fun evaluate(b: Board, ai: AttacksInfo): Int {
        val all = b.all

        val materialValue = intArrayOf(0, 0)
        val pawnMaterialValue = intArrayOf(0, 0)
        val pcsqValue = intArrayOf(0, 0)
        val pcsqOpeningValue = intArrayOf(0, 0)
        val pcsqEndgameValue = intArrayOf(0, 0)

        val noQueen = booleanArrayOf(true, true)

        var square: Long = 1
        var index = 0
        while (square != 0L) {
            val isWhite = b.whites and square != 0L
            val color = if (isWhite) 0 else 1
            val pcsqIndex: Int = if (isWhite) 63 - index else index

            if (square and all != 0L) {

                if (square and b.pawns != 0L) {
                    pawnMaterialValue[color] += PAWN
                    pcsqValue[color] += pawnSquare[pcsqIndex]

                } else if (square and b.knights != 0L) {
                    materialValue[color] += KNIGHT
                    pcsqValue[color] += knightSquare[pcsqIndex]

                } else if (square and b.bishops != 0L) {
                    materialValue[color] += BISHOP
                    pcsqValue[color] += bishopSquare[pcsqIndex]

                } else if (square and b.rooks != 0L) {
                    materialValue[color] += ROOK
                    pcsqValue[color] += rookSquare[pcsqIndex]

                } else if (square and b.queens != 0L) {
                    pcsqValue[color] += queenSquare[pcsqIndex]
                    materialValue[color] += QUEEN
                    noQueen[color] = false

                } else if (square and b.kings != 0L) {
                    pcsqOpeningValue[color] += kingSquareOpening[pcsqIndex]
                    pcsqEndgameValue[color] += kingSquareEndGame[pcsqIndex]
                }
            }
            square = square shl 1
            index++
        }

        var value = 0
        value += pawnMaterialValue[0] - pawnMaterialValue[1]
        value += materialValue[0] - materialValue[1]
        value += pcsqValue[0] - pcsqValue[1]

        // Endgame
        // 1. Both sides have no queens or
        // 2. Every side which has a queen has additionally no other pieces or one minorpiece maximum.
        if ((noQueen[0] || materialValue[0] <= QUEEN + BISHOP) && (noQueen[1] || materialValue[1] <= QUEEN + BISHOP)) {
            value += pcsqEndgameValue[0] - pcsqEndgameValue[1]
        } else {
            value += pcsqOpeningValue[0] - pcsqOpeningValue[1]
        }

        return value
    }

    companion object {

        internal val PAWN = 100
        internal val KNIGHT = 320
        internal val BISHOP = 330
        internal val ROOK = 500
        internal val QUEEN = 900

        // Values are rotated for whites, so when white is playing is like shown in the code TODO at the moment must be symmetric
        val pawnSquare = intArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0,
                50, 50, 50, 50, 50, 50, 50, 50,
                10, 10, 20, 30, 30, 20, 10, 10,
                5, 5, 10, 25, 25, 10, 5, 5,
                0, 0, 0, 20, 20, 0, 0, 0,
                5, -5, -10, 0, 0, -10, -5, 5,
                5, 10, 10, -20, -20, 10, 10, 5,
                0, 0, 0, 0, 0, 0, 0, 0
        )

        val knightSquare = intArrayOf(
                -50, -40, -30, -30, -30, -30, -40, -50,
                -40, -20, 0, 0, 0, 0, -20, -40,
                -30, 0, 10, 15, 15, 10, 0, -30,
                -30, 5, 15, 20, 20, 15, 5, -30,
                -30, 0, 15, 20, 20, 15, 0, -30,
                -30, 5, 10, 15, 15, 10, 5, -30,
                -40, -20, 0, 5, 5, 0, -20, -40,
                -50, -40, -30, -30, -30, -30, -40, -50)

        val bishopSquare = intArrayOf(
                -20, -10, -10, -10, -10, -10, -10, -20,
                -10, 0, 0, 0, 0, 0, 0, -10,
                -10, 0, 5, 10, 10, 5, 0, -10,
                -10, 5, 5, 10, 10, 5, 5, -10,
                -10, 0, 10, 10, 10, 10, 0, -10,
                -10, 10, 10, 10, 10, 10, 10, -10,
                -10, 5, 0, 0, 0, 0, 5, -10,
                -20, -10, -10, -10, -10, -10, -10, -20)

        val rookSquare = intArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0,
                5, 10, 10, 10, 10, 10, 10, 5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                0, 0, 0, 5, 5, 0, 0, 0
        )

        val queenSquare = intArrayOf(
                -20, -10, -10, -5, -5, -10, -10, -20,
                -10, 0, 0, 0, 0, 0, 0, -10,
                -10, 0, 5, 5, 5, 5, 0, -10,
                -5, 0, 5, 5, 5, 5, 0, -5,
                0, 0, 5, 5, 5, 5, 0, -5,
                -10, 5, 5, 5, 5, 5, 0, -10,
                -10, 0, 5, 0, 0, 0, 0, -10,
                -20, -10, -10, -5, -5, -10, -10, -20
        )

        val kingSquareOpening = intArrayOf(
                -30, -40, -40, -50, -50, -40, -40, -30,
                -30, -40, -40, -50, -50, -40, -40, -30,
                -30, -40, -40, -50, -50, -40, -40, -30,
                -30, -40, -40, -50, -50, -40, -40, -30,
                -20, -30, -30, -40, -40, -30, -30, -20,
                -10, -20, -20, -20, -20, -20, -20, -10,
                20, 20, 0, 0, 0, 0, 20, 20,
                20, 30, 10, 0, 0, 10, 30, 20
        )

        val kingSquareEndGame = intArrayOf(
                -50, -40, -30, -20, -20, -30, -40, -50,
                -30, -20, -10, 0, 0, -10, -20, -30,
                -30, -10, 20, 30, 30, 20, -10, -30,
                -30, -10, 30, 40, 40, 30, -10, -30,
                -30, -10, 30, 40, 40, 30, -10, -30,
                -30, -10, 20, 30, 30, 20, -10, -30,
                -30, -30, 0, 0, 0, 0, -30, -30,
                -50, -30, -30, -30, -30, -30, -30, -50
        )
    }
}