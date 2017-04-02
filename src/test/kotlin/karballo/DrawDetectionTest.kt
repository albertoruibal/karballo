package karballo

import karballo.pgn.PgnFile
import karballo.pgn.PgnImportExport
import karballo.search.SearchEngine
import org.junit.Assert.*
import org.junit.Test


class DrawDetectionTest {

    @Test
    fun test3FoldDraw() {
        val se = SearchEngine(Config())

        val `is` = this.javaClass.getResourceAsStream("/draw.pgn")
        val pgnGame = PgnFile.getGameNumber(`is`, 0)
        PgnImportExport.setBoard(se.board, pgnGame!!)

        println(se.board.toString())
        println("draw = " + se.board.isDraw)

        assertTrue(se.board.isDraw)
    }

    @Test
    fun test3FoldDrawNo() {
        val se = SearchEngine(Config())

        val `is` = this.javaClass.getResourceAsStream("/draw.pgn")
        val pgnGame = PgnFile.getGameNumber(`is`, 0)
        PgnImportExport.setBoard(se.board, pgnGame!!)

        se.board.undoMove()

        println(se.board.toString())
        println("draw = " + se.board.isDraw)

        assertFalse(se.board.isDraw)
    }

    @Test
    fun testDrawDetection() {
        val b = Board()
        b.fen = "7k/8/8/8/8/8/8/7K w - - 0 0"
        assertEquals(b.isDraw, true)
        b.fen = "7k/8/8/8/8/8/8/6BK b - - 0 0"
        assertEquals(b.isDraw, true)
        b.fen = "7k/8/8/8/8/8/8/6NK b - - 0 0"
        assertEquals(b.isDraw, true)
        b.fen = "7k/8/nn6/8/8/8/8/8K b - - 0 0"
        assertEquals(b.isDraw, true)
        b.fen = "7k/8/Nn6/8/8/8/8/8K b - - 0 0"
        assertEquals(b.isDraw, false)
        b.fen = "7k/7p/8/8/8/8/8/6NK b - - 0 0"
        assertEquals(b.isDraw, false)
    }

    @Test
    fun testKBbkDraw() {
        val b = Board()
        // Different bishop color is NOT draw
        b.fen = "6bk/8/8/8/8/8/8/6BK b - - 0 0"
        assertEquals(b.isDraw, false)
        // Both bishops in the same color is draw
        b.fen = "6bk/8/8/8/8/8/8/5B1K b - - 0 0"
        assertEquals(b.isDraw, true)
    }
}