package karballo.book

import karballo.Board

/**
 * Opening book support

 * @author rui
 */
interface Book {
    /**
     * Gets a random move from the book taking care of weights
     */
    fun getMove(board: Board): Int
}
