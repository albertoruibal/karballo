package karballo.bitboard

import karballo.Square
import kotlin.math.abs
import kotlin.math.max

object BitboardUtils {

    // Board borders
    val b_d: Long = 255 // down
    val b_u: Long = -72057594037927936 // up
    val b_r: Long = 72340172838076673 // right
    val b_l: Long = -9187201950435737472 // left

    // Board borders (2 squares),for the knight
    val b2_d: Long = 65535 // down
    val b2_u: Long = -281474976710656 // up
    val b2_r: Long = 217020518514230019 // right
    val b2_l: Long = -4557430888798830400 // left

    val A = b_l
    val B = b_r shl 6
    val C = b_r shl 5
    val D = b_r shl 4
    val E = b_r shl 3
    val F = b_r shl 2
    val G = b_r shl 1
    val H = b_r

    // 0 is a, 7 is g
    val FILE = longArrayOf(A, B, C, D, E, F, G, H)
    val FILES_ADJACENT = longArrayOf(
            FILE[1],
            FILE[0] or FILE[2],
            FILE[1] or FILE[3],
            FILE[2] or FILE[4],
            FILE[3] or FILE[5],
            FILE[4] or FILE[6],
            FILE[5] or FILE[7],
            FILE[6]
    )

    val FILES_LEFT = longArrayOf(//
            0,
            FILE[0],
            FILE[0] or FILE[1],
            FILE[0] or FILE[1] or FILE[2],
            FILE[0] or FILE[1] or FILE[2] or FILE[3],
            FILE[0] or FILE[1] or FILE[2] or FILE[3] or FILE[4],
            FILE[0] or FILE[1] or FILE[2] or FILE[3] or FILE[4] or FILE[5],
            FILE[0] or FILE[1] or FILE[2] or FILE[3] or FILE[4] or FILE[5] or FILE[6]
    )

    val FILES_RIGHT = longArrayOf(//
            FILE[1] or FILE[2] or FILE[3] or FILE[4] or FILE[5] or FILE[6] or FILE[7],
            FILE[2] or FILE[3] or FILE[4] or FILE[5] or FILE[6] or FILE[7],
            FILE[3] or FILE[4] or FILE[5] or FILE[6] or FILE[7],
            FILE[4] or FILE[5] or FILE[6] or FILE[7],
            FILE[5] or FILE[6] or FILE[7],
            FILE[6] or FILE[7],
            FILE[7],
            0
    )

    val R1 = b_d
    val R2 = b_d shl 8
    val R3 = b_d shl 16
    val R4 = b_d shl 24
    val R5 = b_d shl 32
    val R6 = b_d shl 40
    val R7 = b_d shl 48
    val R8 = b_d shl 56

    val RANK = longArrayOf(R1, R2, R3, R4, R5, R6, R7, R8) // 0 is 1, 7 is 8
    val RANKS_UPWARDS = longArrayOf(
            RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[5] or RANK[6] or RANK[7],
            RANK[6] or RANK[7],
            RANK[7],
            0
    )
    val RANK_AND_UPWARDS = longArrayOf(
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[4] or RANK[5] or RANK[6] or RANK[7],
            RANK[5] or RANK[6] or RANK[7],
            RANK[6] or RANK[7],
            RANK[7]
    )
    val RANKS_DOWNWARDS = longArrayOf(
            0,
            RANK[0],
            RANK[0] or RANK[1],
            RANK[0] or RANK[1] or RANK[2],
            RANK[0] or RANK[1] or RANK[2] or RANK[3],
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4],
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5],
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6]
    )
    val RANK_AND_DOWNWARDS = longArrayOf(
            RANK[0],
            RANK[0] or RANK[1],
            RANK[0] or RANK[1] or RANK[2],
            RANK[0] or RANK[1] or RANK[2] or RANK[3],
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4],
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5],
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6],
            RANK[0] or RANK[1] or RANK[2] or RANK[3] or RANK[4] or RANK[5] or RANK[6] or RANK[7]
    )

    // Ranks forward in pawn direction W, B
    val RANKS_FORWARD = arrayOf(RANKS_UPWARDS, RANKS_DOWNWARDS)
    val RANKS_BACKWARD = arrayOf(RANKS_DOWNWARDS, RANKS_UPWARDS)
    val RANK_AND_FORWARD = arrayOf(RANK_AND_UPWARDS, RANK_AND_DOWNWARDS)
    val RANK_AND_BACKWARD = arrayOf(RANK_AND_DOWNWARDS, RANK_AND_UPWARDS)

    val SQUARE_NAMES = changeEndianArray64(arrayOf(
            "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
            "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
            "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
            "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
            "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
            "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
            "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
            "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1")
    )

    // To use with square2Index
    val BIT_TABLE = intArrayOf(63, 30, 3, 32, 25, 41, 22, 33, 15, 50, 42, 13, 11, 53, 19, 34, 61, 29, 2, 51, 21, 43, 45, 10, 18, 47, 1, 54, 9, 57, 0, 35, 62, 31, 40, 4, 49, 5, 52, 26, 60, 6, 23, 44, 46, 27, 56, 16, 7, 39, 48, 24, 59, 14, 12, 55, 38, 28, 58, 20, 37, 17, 36, 8)

    /**
     * Converts a square to its index 0=H1, 63=A8
     */
    fun square2Index(square: Long): Int {
        val b = square xor square - 1
        val fold = (b xor b.ushr(32)).toInt()
        return BIT_TABLE[(fold * 0x783a9b23).ushr(26)]
    }

    /**
     * And viceversa
     */
    fun index2Square(index: Int): Long {
        return Square.H1 shl index
    }

    /**
     * Changes element 0 with 63 and consecuvely: this way array constants are more legible
     */
    fun changeEndianArray64(sarray: Array<String>): Array<String> {
        sarray.reverse()
        return sarray
    }

    fun changeEndianArray64(sarray: IntArray): IntArray {
        sarray.reverse()
        return sarray
    }

    /**
     * Prints a BitBoard to standard output
     */
    fun toString(board: Long): String {
        val sb = StringBuilder()
        var i = Square.A8
        while (i != 0L) {
            sb.append(if (board and i != 0L) "1 " else "0 ")
            if (i and b_r != 0L) {
                sb.append("\n")
            }
            i = i ushr 1
        }
        return sb.toString()
    }

    /**
     * Flips board vertically
     * https://chessprogramming.wikispaces.com/Flipping+Mirroring+and+Rotating
     */
    fun flipVertical(board: Long): Long {
        var b = board
        val k1 = 0x00FF00FF00FF00FFL
        val k2 = 0x0000FFFF0000FFFFL
        b = b.ushr(8) and k1 or (b and k1 shl 8)
        b = b.ushr(16) and k2 or (b and k2 shl 16)
        b = b.ushr(32) or (b shl 32)
        return b
    }

    fun flipHorizontalIndex(index: Int): Int {
        return index and 0xF8 or 7 - (index and 7)
    }

    /**
     * Counts the number of bits of one long
     * http://chessprogramming.wikispaces.com/Population+Count
     */
    fun popCount(board: Long): Int {
        var b = board
        if (b == 0L) {
            return 0
        }
        val k1 = 0x5555555555555555L
        val k2 = 0x3333333333333333L
        val k4 = 0x0f0f0f0f0f0f0f0fL
        val kf = 0x0101010101010101L
        b = b - (b shr 1 and k1) // put count of each 2 bits into those 2 bits
        b = (b and k2) + (b shr 2 and k2) // put count of each 4 bits into those 4
        // bits
        b = b + (b shr 4) and k4 // put count of each 8 bits into those 8 bits
        b = b * kf shr 56 // returns 8 most significant bits of board + (board<<8) +
        // (board<<16) + (board<<24) + ...
        return b.toInt()
    }

    /**
     * Convert a bitboard square to algebraic notation number depends of rotated board
     */
    fun square2Algebraic(square: Long): String {
        return SQUARE_NAMES[square2Index(square)]
    }

    fun index2Algebraic(index: Int): String {
        return SQUARE_NAMES[index]
    }

    fun algebraic2Index(name: String): Int {
        for (i in 0..63) {
            if (name == SQUARE_NAMES[i]) {
                return i
            }
        }
        return -1
    }

    fun algebraic2Square(name: String): Long {
        var aux = Square.H1
        for (i in 0..63) {
            if (name == SQUARE_NAMES[i]) {
                return aux
            }
            aux = aux shl 1
        }
        return 0
    }

    /**
     * Gets the file (0..7) for (a..h) of the square
     */
    fun getFile(square: Long): Int {
        for (file in 0..7) {
            if (FILE[file] and square != 0L) {
                return file
            }
        }
        return 0
    }

    fun getRankLsb(square: Long): Int {
        for (rank in 0..7) {
            if (RANK[rank] and square != 0L) {
                return rank
            }
        }
        return 0
    }

    fun getRankMsb(square: Long): Int {
        for (rank in 7 downTo 0) {
            if (RANK[rank] and square != 0L) {
                return rank
            }
        }
        return 0
    }

    fun getFileOfIndex(index: Int): Int {
        return 7 - index and 7
    }

    fun getRankOfIndex(index: Int): Int {
        return index shr 3
    }

    /**
     * Gets a long with the less significant bit of the board
     */
    fun lsb(board: Long): Long {
        return board and -board
    }

    fun msb(board: Long): Long {
        var b = board
        b = b or b.ushr(32)
        b = b or b.ushr(16)
        b = b or b.ushr(8)
        b = b or b.ushr(4)
        b = b or b.ushr(2)
        b = b or b.ushr(1)
        return if (b == 0L) 0 else b.ushr(1) + 1
    }

    /**
     * Distance between two indexes
     */
    fun distance(index1: Int, index2: Int): Int {
        return max(abs((index1 and 7) - (index2 and 7)), abs((index1 shr 3) - (index2 shr 3)))
    }

    /**
     * Gets the horizontal line between two squares (including the origin and destiny squares)
     * square1 must be to the left of square2 (square1 must be a higher bit)
     */
    fun getHorizontalLine(square1: Long, square2: Long): Long {
        return square1 or square1 - 1 and (square2 - 1).inv()
    }

    fun isWhiteSquare(square: Long): Boolean {
        return square and Square.WHITES != 0L
    }

    fun isBlackSquare(square: Long): Boolean {
        return square and Square.BLACKS != 0L
    }

    fun getSameColorSquares(square: Long): Long {
        return if (square and Square.WHITES != 0L) Square.WHITES else Square.BLACKS
    }

    fun frontPawnSpan(pawn: Long, color: Int): Long {
        val index = square2Index(pawn)
        val rank = index shr 3
        val file = 7 - index and 7

        return RANKS_FORWARD[color][rank] and (FILE[file] or FILES_ADJACENT[file])
    }

    fun frontFile(square: Long, color: Int): Long {
        val index = square2Index(square)
        val rank = index shr 3
        val file = 7 - index and 7

        return RANKS_FORWARD[color][rank] and FILE[file]
    }

    fun sameRankOrFile(index1: Int, index2: Int): Boolean {
        return index1 shr 3 == index2 shr 3 || index1 and 7 == index2 and 7
    }
}