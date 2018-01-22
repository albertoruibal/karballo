package karballo.book

import karballo.Board
import karballo.Move
import karballo.log.Logger
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.*

/**
 * Polyglot opening book support

 * @author rui
 */
class FileBook(private val bookName: String) : Book {
    internal var moves: MutableList<Int> = ArrayList()
    internal var weights: MutableList<Int> = ArrayList()
    internal var totalWeight: Long = 0

    private val random = Random()

    init {
        logger.debug("Using opening book " + bookName)
    }

    /**
     * "move" is a bit field with the following meaning (bit 0 is the least significant bit)
     *
     *
     * bits                meaning
     * ===================================
     * 0,1,2               to file
     * 3,4,5               to row
     * 6,7,8               from file
     * 9,10,11             from row
     * 12,13,14            promotion piece
     * "promotion piece" is encoded as follows
     * none       0
     * knight     1
     * bishop     2
     * rook       3
     * queen      4

     * @param move
     * *
     * @return
     */
    private fun int2MoveString(move: Int): String {
        val sb = StringBuilder()
        sb.append(('a' + (move shr 6 and 0x7)))
        sb.append((move shr 9 and 0x7) + 1)
        sb.append(('a' + (move and 0x7)))
        sb.append((move shr 3 and 0x7) + 1)
        if (move shr 12 and 0x7 != 0) sb.append("nbrq"[(move shr 12 and 0x7) - 1])
        return sb.toString()
    }

    fun generateMoves(board: Board) {
        totalWeight = 0
        moves.clear()
        weights.clear()

        val key2Find = board.getKey()

        try {
            val bookIs = javaClass.getResourceAsStream(bookName)
            val dataInputStream = DataInputStream(BufferedInputStream(bookIs))

            var key: Long
            var moveInt: Int
            var weight: Int

            while (true) {
                key = dataInputStream.readLong()
                if (key == key2Find) {
                    moveInt = dataInputStream.readShort().toInt()
                    weight = dataInputStream.readShort().toInt()
                    dataInputStream.readInt() // Unused learn field

                    val move = Move.getFromString(board, int2MoveString(moveInt), true)
                    // Add only if it is legal
                    if (board.getLegalMove(move) != Move.NONE) {
                        moves.add(move)
                        weights.add(weight)
                        totalWeight += weight.toLong()
                    }
                } else {
                    dataInputStream.skipBytes(8)
                }
            }
        } catch (ignored: Exception) {
        }

    }

    /**
     * Gets a random move from the book taking care of weights
     */
    override fun getMove(board: Board): Int {
        generateMoves(board)
        var randomWeight = (random.nextFloat() * totalWeight).toLong()
        for (i in moves.indices) {
            randomWeight -= weights[i].toLong()
            if (randomWeight <= 0) {
                return moves[i]
            }
        }
        return Move.NONE
    }

    companion object {
        private val logger = Logger.getLogger("FileBook")
    }
}