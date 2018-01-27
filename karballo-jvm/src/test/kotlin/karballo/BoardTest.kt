package karballo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    @Test
    fun testNoChess960StartPosition() {
        val b = Board()
        b.startPosition()
        assertEquals(false, b.chess960)
    }

    @Test
    fun testChess960StartPosition() {
        val b = Board()
        b.startPosition(545)
        println(b)
        assertEquals("brnbknqr/pppppppp/8/8/8/8/PPPPPPPP/BRNBKNQR w KQkq - 0 1", b.fen)
    }

    @Test
    fun testChess960Castling() {
        val b = Board()
        b.fen = "nqrkbbnr/pppppppp/8/8/8/8/PPPPPPPP/NQRKBBNR w KQkq - 0 1"
        println(b)
        val move = Move.getFromString(b, "O-O-O", false)
        b.doMove(move, false, false)
        println(b)
        assertEquals("nqrkbbnr/pppppppp/8/8/8/8/PPPPPPPP/NQKRBBNR b kq - 1 1", b.fen)
    }

    @Test
    fun testChess960CastlingKingSameSquare() {
        val b = Board()
        b.fen = "r1krbnqb/1pp1pppp/1p1p4/8/3P4/8/PPP1PPPP/NRK1RNQB w Qk - 0 1"
        println(b)
        val move = Move.getFromString(b, "O-O-O", false)
        b.doMove(move, false, false)
        println(b)
        assertEquals("r1krbnqb/1pp1pppp/1p1p4/8/3P4/8/PPP1PPPP/N1KRRNQB b k - 1 1", b.fen)
    }

    @Test
    fun testChess960CastlingRookSameSquare() {
        val b = Board()
        b.fen = "7k/pppppppp/8/8/8/8/PPPPPPPP/3RK3 w Q - 0 1"
        println(b)
        val move = Move.getFromString(b, "O-O-O", false)
        b.doMove(move, false, false)
        println(b)
        assertEquals("7k/pppppppp/8/8/8/8/PPPPPPPP/2KR4 b - - 1 1", b.fen)
    }

    @Test
    fun testXFen() {
        // http://en.wikipedia.org/wiki/X-FEN
        val b = Board()
        b.fen = "rn2k1r1/ppp1pp1p/3p2p1/5bn1/P7/2N2B2/1PPPPP2/2BNK1RR w Gkq - 4 11"
        println(b)
        assertEquals(1L shl 1, b.castlingRooks[0])
        b.doMove(Move.getFromString(b, "O-O", true))
        println(b)
        assertEquals("rn2k1r1/ppp1pp1p/3p2p1/5bn1/P7/2N2B2/1PPPPP2/2BN1RKR b kq - 5 11", b.fen)
    }

    @Test
    fun testMoveNumber1() {
        val b = Board()
        b.fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w QKqk - 0 1"
        assertEquals(b.moveNumber.toLong(), 0)
    }

    @Test
    fun testMoveNumber2() {
        val b = Board()
        b.fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b QKqk - 0 1"
        assertEquals(b.moveNumber.toLong(), 1)
    }

    @Test
    fun testMoveNumber3() {
        val b = Board()
        b.fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w QKqk - 0 1"
        b.setFenMove("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b QKqk - 0 1", "e2e4")
        assertEquals(b.initialMoveNumber.toLong(), 0)
        assertEquals(b.moveNumber.toLong(), 1)
    }

    @Test
    fun testUndo() {
        val b = Board()
        b.fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        b.doMove(Move.getFromString(b, "e2e4", true))
        b.doMove(Move.getFromString(b, "e7e5", true))
        b.undoMove()
        b.undoMove()
        assertEquals(b.fen, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    @Test
    fun testCastling() {
        // Must keep history after castling
        val b = Board()
        b.fen = "rnbqk2r/ppp1bppp/4pn2/3p4/2PP4/3QP3/PP1B1PPP/RN2KBNR b QKqk - 2 5"
        b.setFenMove("rnbq1rk1/ppp1bppp/4pn2/3p4/2PP4/3QP3/PP1B1PPP/RN2KBNR w QK - 0 6", "O-O")
        assertEquals(b.initialMoveNumber.toLong(), 9)
    }

    @Test
    fun testPassedPawn() {
        val b = Board()
        // Position from http://en.wikipedia.org/wiki/Passed_pawn
        b.fen = "7k/8/7p/1P2Pp1P/2Pp1PP1/8/8/7K w - - 0 0"
        print(b)
        assertEquals(b.isPassedPawn(25), false)
        assertEquals(b.isPassedPawn(26), false)
        assertEquals(b.isPassedPawn(28), true)
        assertEquals(b.isPassedPawn(29), true)
        assertEquals(b.isPassedPawn(32), false)
        assertEquals(b.isPassedPawn(34), false)
        assertEquals(b.isPassedPawn(35), true)
        assertEquals(b.isPassedPawn(38), true)
        assertEquals(b.isPassedPawn(40), false)
    }

    @Test
    fun testAdjacentColumnBug() {
        val b = Board()
        b.fen = "7k/8/2p5/1P6/8/8/8/7K w - - 0 0"
        print(b)
        assertEquals(b.isPassedPawn(38), false)
    }

    @Test
    fun testCheckDetection() {
        val b = Board()
        b.fen = "4k3/8/8/8/8/2q5/1P6/4K3 w - - 0 1"
        println(b.toString())
        assertTrue("Position must be check", b.check)
    }
}