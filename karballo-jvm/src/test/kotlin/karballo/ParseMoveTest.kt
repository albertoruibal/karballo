package karballo

import karballo.util.JvmPlatformUtils
import karballo.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseMoveTest {

    constructor() {
        Utils.instance = JvmPlatformUtils()
    }

    @Test
    fun testGetFromString1() {
        val b = Board()
        b.fen = "1r1qk2r/1bp1n1pp/p2b1p2/2B1p3/2Q1P3/2N2N2/PPP2PPP/R4RK1 w k - 1 26"
        assertEquals("Ra1-d1", Move.toStringExt(Move.getFromString(b, "Ra1-d1", true)))
    }

    @Test
    fun testGetFromString2() {
        val b = Board()
        b.fen = "1r1qk2r/1bp1n1pp/p2b1p2/2B1p3/2Q1P3/2N2N2/PPP2PPP/R4RK1 w k - 1 26"
        assertEquals("Ra1-d1", Move.toStringExt(Move.getFromString(b, "Rad1", true)))
    }

    @Test
    fun testGetFromString3() {
        val b = Board()
        b.fen = "1r1qk2r/1bp1n1pp/p2b1p2/2B1p3/2Q1P3/2N2N2/PPP2PPP/R4RK1 w k - 1 26"
        assertEquals("Ra1-d1", Move.toStringExt(Move.getFromString(b, "a1 d1", true)))
    }

    @Test
    fun testGetFromString4() {
        val b = Board()
        b.fen = "rnbqkb1r/pp3ppp/2p5/3pP3/8/2Q1BN2/PPP2PPP/R3KB1R b QKqk - 1 0"
        assertEquals("Nb8-d7", Move.toStringExt(Move.getFromString(b, "b8d7", true)))
    }

    @Test
    fun testGetFromStringCastlingInInitialPosition() {
        val b = Board()
        b.startPosition()
        assertEquals(Move.NONE_STRING, Move.toStringExt(Move.getFromString(b, "O-O", true)))
    }

    @Test
    fun testGetFromStringDisambiguateOneMoveNotLegal() {
        // Another knight can move to the same square but leaving the king in check
        val b = Board()
        b.fen = "r1bqk1nr/pp3ppp/2n5/1Bbp4/8/5N2/PPPN1PPP/R1BQ1RK1 b kq - 1 8"
        assertEquals("Ng8-e7", Move.toStringExt(Move.getFromString(b, "Ne7", true)))
    }

    @Test
    fun testBugSanGg6() {
        val b = Board()
        b.fen = "R7/3p3p/8/3P2P1/3k4/1p5p/1P1NKP1P/7q w - -"
        val move = Move.getFromString(b, "g6", true)
        b.doMove(move)
        assertEquals("Converts move g6 as gg6 if the move is done, and it must return none if the move is not legal", "none", Move.toSan(b, move))
    }
}