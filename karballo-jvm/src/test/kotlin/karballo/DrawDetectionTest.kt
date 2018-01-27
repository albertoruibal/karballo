package karballo

import karballo.pgn.PgnFile
import karballo.pgn.PgnImportExport
import org.junit.Assert.*
import org.junit.Test


class DrawDetectionTest {

    @Test
    fun test3FoldDraw() {
        val b = Board()

        val `is` = this.javaClass.getResourceAsStream("/draw.pgn")
        val pgnGame = PgnFile.getGameNumber(`is`, 0)
        PgnImportExport.setBoard(b, pgnGame!!)

        System.out.println(b.toString())
        System.out.println("draw = " + b.isDraw)

        assertTrue(b.isDraw)
    }

    @Test
    fun test3FoldDrawNo() {
        val b = Board()

        val `is` = this.javaClass.getResourceAsStream("/draw.pgn")
        val pgnGame = PgnFile.getGameNumber(`is`, 0)
        PgnImportExport.setBoard(b, pgnGame!!)

        b.undoMove()

        System.out.println(b.toString())
        System.out.println("draw = " + b.isDraw)

        assertFalse(b.isDraw)
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