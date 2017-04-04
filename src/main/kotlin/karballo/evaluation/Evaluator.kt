package karballo.evaluation

import karballo.Board
import karballo.Color
import karballo.bitboard.AttacksInfo
import karballo.bitboard.BitboardAttacks
import karballo.util.StringUtils

abstract class Evaluator {

    var bbAttacks: BitboardAttacks = BitboardAttacks.getInstance()

    /**
     * Board evaluator
     */
    abstract fun evaluate(b: Board, ai: AttacksInfo): Int

    fun formatOE(value: Int): String {
        return StringUtils.padLeft(o(value).toString(), 8) + " " + StringUtils.padLeft(e(value).toString(), 8)
    }

    companion object {
        val W = Color.W
        val B = Color.B

        val NO_VALUE = Short.MAX_VALUE.toInt()
        val MATE = 30000
        val KNOWN_WIN = 20000
        val DRAW = 0

        val PAWN_OPENING = 80
        val PAWN = 100
        val KNIGHT = 325
        val BISHOP = 325
        val ROOK = 500
        val QUEEN = 975
        val PIECE_VALUES = intArrayOf(0, PAWN, KNIGHT, BISHOP, ROOK, QUEEN)
        val PIECE_VALUES_OE = intArrayOf(0, oe(PAWN_OPENING, PAWN), oe(KNIGHT, KNIGHT), oe(BISHOP, BISHOP), oe(ROOK, ROOK), oe(QUEEN, QUEEN))
        val BISHOP_PAIR = oe(50, 50) // Bonus by having two bishops in different colors

        val GAME_PHASE_MIDGAME = 1000
        val GAME_PHASE_ENDGAME = 0
        val NON_PAWN_MATERIAL_ENDGAME_MIN = QUEEN + ROOK
        val NON_PAWN_MATERIAL_MIDGAME_MAX = 3 * KNIGHT + 3 * BISHOP + 4 * ROOK + 2 * QUEEN

        /**
         * Merges two short Opening - Ending values in one int
         */
        fun oe(opening: Int, endgame: Int): Int {
            return (opening shl 16) + endgame
        }

        /**
         * Get the "Opening" part
         */
        fun o(oe: Int): Int {
            return oe + 0x8000 shr 16
        }

        /**
         * Get the "Endgame" part
         */
        fun e(oe: Int): Int {
            return (oe and 0xffff).toShort().toInt()
        }

        /**
         * Shift right each part by factor positions
         */
        fun oeShr(factor: Int, oeValue: Int): Int {
            return oe(o(oeValue) shr factor, e(oeValue) shr factor)
        }
    }
}