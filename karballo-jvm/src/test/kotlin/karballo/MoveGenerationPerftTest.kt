package karballo

import karballo.movegen.LegalMoveGenerator
import karballo.movegen.MoveGenerator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Do some move generation tests from http://chessprogramming.wikispaces.com/Perft+Results
 * Takes a looooong time
 */
class MoveGenerationPerftTest {

    var legalMovegen: MoveGenerator = LegalMoveGenerator()
    var board = Board()
    lateinit var moveCount: IntArray
    lateinit var captures: IntArray
    lateinit var passantCaptures: IntArray
    lateinit var castles: IntArray
    lateinit var promotions: IntArray
    lateinit var checks: IntArray
    lateinit var checkMates: IntArray

    private fun reset() {
        moveCount = IntArray(DEPTH)
        captures = IntArray(DEPTH)
        passantCaptures = IntArray(DEPTH)
        castles = IntArray(DEPTH)
        promotions = IntArray(DEPTH)
        checks = IntArray(DEPTH)
        checkMates = IntArray(DEPTH)
    }

    private fun print(depth: Int) {
        for (i in 0..depth - 1) {
            println("Moves: " + moveCount[i] + " Captures="
                    + captures[i] + " E.P.=" + passantCaptures[i] + " Castles="
                    + castles[i] + " Promotions=" + promotions[i] + " Checks="
                    + checks[i] + " CheckMates=" + checkMates[i])
        }
    }

    @Test
    @Category(SlowTest::class)
    fun testInitialPosition() {
        reset()
        println("TEST INITIAL POSITION")
        board.startPosition()
        recursive(0, 6)
        print(6)
        assertEquals(moveCount[5].toLong(), 119060324)
        assertEquals(captures[5].toLong(), 2812008)
        assertEquals(passantCaptures[5].toLong(), 5248)
        assertEquals(castles[5].toLong(), 0)
        assertEquals(promotions[5].toLong(), 0)
        assertEquals(checks[5].toLong(), 809099)
        assertEquals(checkMates[5].toLong(), 10828)
    }

    @Test
    @Category(SlowTest::class)
    fun testPosition2() {
        reset()
        println("TEST POSITION 2")
        board.fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -"
        recursive(0, 5)
        print(5)
        assertEquals(moveCount[4].toLong(), 193690690)
        assertEquals(captures[4].toLong(), 35043416)
        assertEquals(passantCaptures[4].toLong(), 73365)
        assertEquals(castles[4].toLong(), 4993637)
        assertEquals(promotions[4].toLong(), 8392)
        assertEquals(checks[4].toLong(), 3309887)
        assertEquals(checkMates[4].toLong(), 30171)
    }

    @Test
    @Category(SlowTest::class)
    fun testPosition3() {
        reset()
        println("TEST POSITION 3")
        board.fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -"
        recursive(0, 7)
        print(7)

        assertEquals(moveCount[6].toLong(), 178633661)
        assertEquals(captures[6].toLong(), 14519036)
        assertEquals(passantCaptures[6].toLong(), 294874)
        assertEquals(castles[6].toLong(), 0)
        assertEquals(promotions[6].toLong(), 140024)
        assertEquals(checks[6].toLong(), 12797406)
        assertEquals(checkMates[6].toLong(), 87)
    }

    @Test
    @Category(SlowTest::class)
    fun testPosition4() {
        reset()
        println("TEST POSITION 4")
        board.fen = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
        recursive(0, 6)
        print(6)

        assertEquals(moveCount[5].toLong(), 706045033)
        assertEquals(captures[5].toLong(), 210369132)
        assertEquals(passantCaptures[5].toLong(), 212)
        assertEquals(castles[5].toLong(), 10882006)
        assertEquals(promotions[5].toLong(), 81102984)
        assertEquals(checks[5].toLong(), 26973664)
        assertEquals(checkMates[5].toLong(), 81076)
    }

    @Test
    @Category(SlowTest::class)
    fun testPosition5() {
        reset()
        println("TEST POSITION 5")
        board.fen = "rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6"
        recursive(0, 3)
        print(3)

        assertEquals(moveCount[0].toLong(), 42)
        assertEquals(moveCount[1].toLong(), 1352)
        assertEquals(moveCount[2].toLong(), 53392)
    }

    private fun recursive(depth: Int, depthRemaining: Int) {
        val moves = IntArray(256)
        val index = legalMovegen.generateMoves(board, moves, 0)
        for (i in 0..index - 1) {
            val move = moves[i]
            board.doMove(move)
            if (depthRemaining > 0) {
                moveCount[depth]++
                if (moveCount[depth] % 100000 == 0) {
                    println("movecount[" + depth + "]=" + moveCount[depth])
                }
                if (Move.isCapture(move)) {
                    captures[depth]++
                }
                if (Move.getMoveType(move) == Move.TYPE_PASSANT) {
                    passantCaptures[depth]++
                }
                if (Move.getMoveType(move) == Move.TYPE_KINGSIDE_CASTLING || Move.getMoveType(move) == Move.TYPE_QUEENSIDE_CASTLING) {
                    castles[depth]++
                }
                if (Move.isPromotion(move)) {
                    promotions[depth]++
                }
                if (board.check) {
                    checks[depth]++
                }
                if (board.isMate) {
                    checkMates[depth]++
                }

                recursive(depth + 1, depthRemaining - 1)
            }
            board.undoMove()
        }
    }

    @Test
    fun testWeirdError() {
        val legalMoves = IntArray(256)
        val board = Board()
        val legalMoveCount: Int

        // board.setFen("rnbqk1nr/pppp1ppp/8/4p3/1b1P4/7N/PPP1PPPP/RNBQKB1R w QKqk - 2 0");
        // legalMoveCount = board.getLegalMoves(legalMoves);
        board.fen = "r1bqkbnr/pppp1ppp/n7/4p3/3P4/7N/PPP1PPPP/RNBQKB1R w QKqk - 2 0"
        legalMoveCount = board.getLegalMoves(legalMoves)
        assertEquals(legalMoveCount.toLong(), 29)
    }

    companion object {
        private val DEPTH = 7
    }
}