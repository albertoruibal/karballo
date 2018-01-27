package karballo

import karballo.hash.ZobristKey
import karballo.hash.ZobristKeyFen
import karballo.movegen.LegalMoveGenerator
import karballo.util.JvmPlatformUtils
import karballo.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * Test zobrist keys
 * Also test that after board.setfen(x), x==board.getFen();
 */
class ZobristKeyTest {

    constructor() {
        Utils.instance = JvmPlatformUtils()
    }

    /**
     * Test that the zobrist key of the board is equal than the obtained with fen
     * making random legal moves
     */
    @Test
    fun testBoardZobristKey() {
        val board = Board()
        val movegen = LegalMoveGenerator()
        board.startPosition()
        val random = Random()

        for (i in 1..99999) {
            val moves = IntArray(256)
            val moveCount = movegen.generateMoves(board, moves, 0)
            if (moveCount > 0 && i % 100 != 0) {
                val move = moves[(random.nextFloat() * moveCount).toInt()]
                board.doMove(move)

                val key1 = ZobristKeyFen.getKey(board.fen)
                val key2 = ZobristKey.getKey(board)
                assertEquals(board.getKey(), key1)
                assertEquals(board.getKey(), key2[0] xor key2[1])
            } else {
                board.startPosition()
            }
        }
    }

    @Test
    fun testZobristKey1() {
        val board = Board()
        board.startPosition()
        val result = ZobristKeyFen.getKey(board.fen)
        // Also test undoing
        board.doMove(Move.getFromString(board, "g1f3", true))
        board.undoMove()
        assertEquals(result, board.getKey())
        assertEquals(result, 5060803636482931868)
    }

    @Test
    fun testZobristKey2() {
        val board = Board()
        board.fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, -9062197578030825066)
    }

    @Test
    fun testZobristKey3() {
        val board = Board()
        board.fen = "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, 528813709611831216)
    }

    @Test
    fun testZobristKey4() {
        val board = Board()
        board.fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, 7363297126586722772)
    }

    @Test
    fun testZobristKey5() {
        val board = Board()
        board.fen = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, 2496273314520498040)
    }

    @Test
    fun testZobristKey6() {
        val board = Board()
        board.fen = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, 7289745035295343297)
    }

    @Test
    fun testZobristKey7() {
        val board = Board()
        board.fen = "rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, 71445182323015129)
    }

    @Test
    fun testZobristKey8() {
        val board = Board()
        board.fen = "rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, 4359805404264691255)
    }

    @Test
    fun testZobristKey9() {
        val board = Board()
        board.fen = "rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4"
        val result = ZobristKeyFen.getKey(board.fen)
        assertEquals(result, board.getKey())
        assertEquals(result, 6647202560273257824)
    }
}
