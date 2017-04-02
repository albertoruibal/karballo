package karballo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveTest {

    @Test
    fun toSanTest() {
        val b = Board()
        b.fen = "1r4k1/4Bppp/2p5/3p4/3Pq3/4P1Pb/4NP2/3RR1K1 b - - 0 0"
        val m = Move.getFromString(b, "Qg2#", true)
        assertTrue("Move must have the check flag", m and Move.CHECK_MASK != 0)
        b.doMove(m)
        assertEquals("Move must not be none", "Qg2#", b.getSanMove(b.moveNumber - 1))
    }

    @Test
    fun testMoveNotInTurn() {
        val b = Board()
        b.fen = "8/8/5B2/8/4b3/1k6/p7/K7 w - - 72 148"
        val move = Move.getFromString(b, "d4b2", true)
        assertEquals(Move.NONE.toLong(), move.toLong())
    }

    @Test
    fun testPromotionSan() {
        val b = Board()
        b.startPosition()
        b.doMoves("d4 Nf6 Nf3 d5 e3 e6 Bd3 c5 c3 b6 O-O Bb7 Nbd2 Be7 b3 O-O Bb2 Nbd7 Qe2 Bd6 c4 cxd4 exd4 Qe7 Ne5 Ba3 Bxa3 Qxa3 f4 Qb2 Nef3 dxc4 bxc4 Bxf3 Nxf3 Qxe2 Bxe2 Rac8 a4 Rc7 a5 Rd8 axb6 Nxb6 Rfc1 Nc8 g3 Ne7 Ra4 Nc6 c5 Nd5 Ne5 Nde7 Rcc4 Nxe5 fxe5 f6 exf6 gxf6 Bf3 e5 dxe5 fxe5 c6 Nf5 Ra5 Re8 Rcc5 Nd4 Be4 Kf7 Kg2 Kf6 Ra2 Ke6 Rca5 Ree7 Rf2 Rg7 h4 h5 Kh2 a6 Bd5+ Ke7 Rxa6 Kd6 Bg2 Rge7 Ra5 Nxc6 Rf6+ Re6 Rd5+ Ke7 Rf5 e4 Rxh5 e3 Rh7+ Ke8 Rxc7 e2 Bf3 e1=Q")
        assertEquals("e1=Q", b.lastMoveSan)
    }
}