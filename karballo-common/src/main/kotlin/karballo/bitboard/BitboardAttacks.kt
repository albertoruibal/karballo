package karballo.bitboard

import karballo.Board
import karballo.Color
import karballo.log.Logger
import karballo.util.Utils

/**
 * Discover attacks to squares
 */
open class BitboardAttacks internal constructor() {

    var rook: LongArray
    var bishop: LongArray
    var knight: LongArray
    var king: LongArray
    var pawn: Array<LongArray>

    internal fun squareAttackedAux(square: Long, shift: Int, border: Long): Long {
        var s = square
        if (s and border == 0L) {
            if (shift > 0) {
                s = s shl shift
            } else {
                s = s ushr (-shift)
            }
            return s
        }
        return 0
    }

    internal fun squareAttackedAuxSlider(square: Long, shift: Int, border: Long): Long {
        var s = square
        var ret: Long = 0
        while (s and border == 0L) {
            if (shift > 0) {
                s = s shl shift
            } else {
                s = s ushr (-shift)
            }
            ret = ret or s
        }
        return ret
    }

    init {
        logger.debug("Generating attack tables...")
        val time1 = Utils.instance.currentTimeMillis()
        rook = LongArray(64)
        bishop = LongArray(64)
        knight = LongArray(64)
        king = LongArray(64)
        pawn = Array(2) { LongArray(64) }

        var square: Long = 1
        var i = 0
        while (square != 0L) {
            rook[i] = squareAttackedAuxSlider(square, +8, BitboardUtils.b_u) or
                    squareAttackedAuxSlider(square, -8, BitboardUtils.b_d) or
                    squareAttackedAuxSlider(square, -1, BitboardUtils.b_r) or
                    squareAttackedAuxSlider(square, +1, BitboardUtils.b_l)

            bishop[i] = squareAttackedAuxSlider(square, +9, BitboardUtils.b_u or BitboardUtils.b_l) or
                    squareAttackedAuxSlider(square, +7, BitboardUtils.b_u or BitboardUtils.b_r) or
                    squareAttackedAuxSlider(square, -7, BitboardUtils.b_d or BitboardUtils.b_l) or
                    squareAttackedAuxSlider(square, -9, BitboardUtils.b_d or BitboardUtils.b_r)

            knight[i] = squareAttackedAux(square, +17, BitboardUtils.b2_u or BitboardUtils.b_l) or
                    squareAttackedAux(square, +15, BitboardUtils.b2_u or BitboardUtils.b_r) or
                    squareAttackedAux(square, -15, BitboardUtils.b2_d or BitboardUtils.b_l) or
                    squareAttackedAux(square, -17, BitboardUtils.b2_d or BitboardUtils.b_r) or
                    squareAttackedAux(square, +10, BitboardUtils.b_u or BitboardUtils.b2_l) or
                    squareAttackedAux(square, +6, BitboardUtils.b_u or BitboardUtils.b2_r) or
                    squareAttackedAux(square, -6, BitboardUtils.b_d or BitboardUtils.b2_l) or
                    squareAttackedAux(square, -10, BitboardUtils.b_d or BitboardUtils.b2_r)

            pawn[Color.W][i] = squareAttackedAux(square, 7, BitboardUtils.b_u or BitboardUtils.b_r) or
                    squareAttackedAux(square, 9, BitboardUtils.b_u or BitboardUtils.b_l)

            pawn[Color.B][i] = squareAttackedAux(square, -7, BitboardUtils.b_d or BitboardUtils.b_l) or
                    squareAttackedAux(square, -9, BitboardUtils.b_d or BitboardUtils.b_r)

            king[i] = squareAttackedAux(square, +8, BitboardUtils.b_u) or
                    squareAttackedAux(square, -8, BitboardUtils.b_d) or
                    squareAttackedAux(square, -1, BitboardUtils.b_r) or
                    squareAttackedAux(square, +1, BitboardUtils.b_l) or
                    squareAttackedAux(square, +9, BitboardUtils.b_u or BitboardUtils.b_l) or
                    squareAttackedAux(square, +7, BitboardUtils.b_u or BitboardUtils.b_r) or
                    squareAttackedAux(square, -7, BitboardUtils.b_d or BitboardUtils.b_l) or
                    squareAttackedAux(square, -9, BitboardUtils.b_d or BitboardUtils.b_r)

            square = square shl 1
            i++
        }
        val time2 = Utils.instance.currentTimeMillis()
        logger.debug("Generated attack tables in " + (time2 - time1) + "ms")
    }

    /**
     * Discover attacks to squares using magics: expensive version
     */
    fun isSquareAttacked(board: Board, square: Long, white: Boolean): Boolean {
        return isIndexAttacked(board, BitboardUtils.square2Index(square), white)
    }

    fun areSquaresAttacked(board: Board, squares: Long, white: Boolean): Boolean {
        var s = squares
        while (s != 0L) {
            val square = BitboardUtils.lsb(s)
            val attacked = isIndexAttacked(board, BitboardUtils.square2Index(square), white)
            if (attacked) {
                return true
            }
            s = s xor square
        }
        return false
    }

    /**
     * Discover attacks to squares using magics: cheap version
     */
    fun isIndexAttacked(board: Board, index: Int, white: Boolean): Boolean {
        if (index < 0 || index > 63) {
            return false
        }
        val others = if (white) board.blacks else board.whites
        val all = board.all

        if (pawn[if (white) Color.W else Color.B][index] and board.pawns and others != 0L) {
            return true
        } else if (king[index] and board.kings and others != 0L) {
            return true
        } else if (knight[index] and board.knights and others != 0L) {
            return true
        } else if (getRookAttacks(index, all) and (board.rooks or board.queens) and others != 0L) {
            return true
        } else if (getBishopAttacks(index, all) and (board.bishops or board.queens) and others != 0L) {
            return true
        }
        return false
    }

    /**
     * Discover attacks to squares using magics: cheap version
     */
    fun getIndexAttacks(board: Board, index: Int): Long {
        if (index < 0 || index > 63) {
            return 0
        }
        val all = board.all

        return board.blacks and pawn[Color.W][index] or (board.whites and pawn[Color.B][index]) and board.pawns or (king[index] and board.kings) or (knight[index] and board.knights) or (getRookAttacks(index, all) and (board.rooks or board.queens)) or (getBishopAttacks(index, all) and (board.bishops or board.queens))
    }

    fun getXrayAttacks(board: Board, index: Int, all: Long): Long {
        if (index < 0 || index > 63) {
            return 0
        }
        return getRookAttacks(index, all) and (board.rooks or board.queens) or (getBishopAttacks(index, all) and (board.bishops or board.queens)) and all
    }

    /**
     * without magic bitboards, too expensive, but uses less memory
     */
    open fun getRookAttacks(index: Int, all: Long): Long {
        return getRookShiftAttacks(BitboardUtils.index2Square(index), all)
    }

    open fun getBishopAttacks(index: Int, all: Long): Long {
        return getBishopShiftAttacks(BitboardUtils.index2Square(index), all)
    }

    fun getRookShiftAttacks(square: Long, all: Long): Long {
        return checkSquareAttackedAux(square, all, +8, BitboardUtils.b_u) or checkSquareAttackedAux(square, all, -8, BitboardUtils.b_d) or checkSquareAttackedAux(square, all, -1, BitboardUtils.b_r) or checkSquareAttackedAux(square, all, +1, BitboardUtils.b_l)
    }

    fun getBishopShiftAttacks(square: Long, all: Long): Long {
        return checkSquareAttackedAux(square, all, +9, BitboardUtils.b_u or BitboardUtils.b_l) or checkSquareAttackedAux(square, all, +7, BitboardUtils.b_u or BitboardUtils.b_r) or checkSquareAttackedAux(square, all, -7, BitboardUtils.b_d or BitboardUtils.b_l) or checkSquareAttackedAux(square, all, -9, BitboardUtils.b_d or BitboardUtils.b_r)
    }

    /**
     * Attacks for sliding pieces
     */
    private fun checkSquareAttackedAux(square: Long, all: Long, shift: Int, border: Long): Long {
        var s = square
        var ret: Long = 0
        while (s and border == 0L) {
            if (shift > 0) {
                s = s shl shift
            } else {
                s = s ushr (-shift)
            }
            ret = ret or s
            // If we collide with other piece
            if (s and all != 0L) {
                break
            }
        }
        return ret
    }

    companion object {
        private val logger = Logger.getLogger("BitboardAttacks")

        /**
         * If disabled, does not use Magic Bitboards, improves loading speed in GWT
         * and avoids memory crashes in mobile browsers
         */
        var USE_MAGIC = true
        internal var instance: BitboardAttacks? = null

        fun getInstance(): BitboardAttacks {
            if (instance == null) {
                if (USE_MAGIC) {
                    instance = BitboardAttacksMagic()
                } else {
                    instance = BitboardAttacks()
                }
            }
            return instance!!
        }
    }
}