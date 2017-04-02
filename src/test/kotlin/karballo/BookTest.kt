package karballo

import karballo.book.FileBook
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTest {

    @Test
    fun testBook() {
        var count = 0
        val board = Board()
        board.startPosition()
        val book = FileBook("/book_small.bin")
        var move = book.getMove(board)
        while (move != 0) {
            println(Move.toString(move))
            board.doMove(move)
            println(board)
            move = book.getMove(board)
            count++
        }
        assertTrue(count > 3)
    }
}