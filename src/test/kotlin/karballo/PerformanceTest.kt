package karballo

import karballo.bitboard.AttacksInfo
import karballo.evaluation.CompleteEvaluator
import karballo.evaluation.SimplifiedEvaluator
import karballo.hash.ZobristKey
import karballo.movegen.LegalMoveGenerator
import karballo.movegen.MagicMoveGenerator
import karballo.movegen.MoveGenerator
import karballo.search.SearchEngine
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Helps searching for bottlenecks
 */
class PerformanceTest {
    lateinit var movegen: MoveGenerator
    lateinit var legalMovegen: MoveGenerator
    lateinit var testBoards: Array<Board>

    var tests = arrayOf("4r1k1/p1pb1ppp/Qbp1r3/8/1P6/2Pq1B2/R2P1PPP/2B2RK1 b - - ",
            "7r/2qpkp2/p3p3/6P1/1p2b2r/7P/PPP2QP1/R2N1RK1 b - - ",
            "r1bq1rk1/pp4bp/2np4/2p1p1p1/P1N1P3/1P1P1NP1/1BP1QPKP/1R3R2 b - - ",
            "8/2kPR3/5q2/5N2/8/1p1P4/1p6/1K6 w - - ",
            "2r1r3/p3bk1p/1pnqpppB/3n4/3P2Q1/PB3N2/1P3PPP/3RR1K1 w - - ",
            "8/2p5/7p/pP2k1pP/5pP1/8/1P2PPK1/8 w - - ",
            "8/5p1p/1p2pPk1/p1p1P3/P1P1K2b/4B3/1P5P/8 w - - ",
            "rn2r1k1/pp3ppp/8/1qNp4/3BnQb1/5N2/PPP2PPP/2KR3R b - - ",
            "r3kb1r/1p1b1p2/p1nppp2/7p/4PP2/qNN5/P1PQB1PP/R4R1K w kq - ",
            "r3r1k1/pp1bp2p/1n2q1P1/6b1/1B2B3/5Q2/5PPP/1R3RK1 w - - ",
            "r3k2r/pb3pp1/2p1qnnp/1pp1P3/Q1N4B/2PB1P2/P5PP/R4RK1 w kq - ",
            "r1b1r1k1/ppp2ppp/2nb1q2/8/2B5/1P1Q1N2/P1PP1PPP/R1B2RK1 w - - ",
            "rnb1kb1r/1p3ppp/p5q1/4p3/3N4/4BB2/PPPQ1P1P/R3K2R w KQkq - ",
            "r1bqr1k1/pp1n1ppp/5b2/4N1B1/3p3P/8/PPPQ1PP1/2K1RB1R w - - ",
            "2r2rk1/1bpR1p2/1pq1pQp1/p3P2p/P1PR3P/5N2/2P2PPK/8 w - - ",
            "8/pR4pk/1b6/2p5/N1p5/8/PP1r2PP/6K1 b - - ",
            "r1b1qrk1/ppBnppb1/2n4p/1NN1P1p1/3p4/8/PPP1BPPP/R2Q1R1K w - - ",
            "8/8/4b1p1/2Bp3p/5P1P/1pK1Pk2/8/8 b - - ",
            "r3k2r/pp1n1ppp/1qpnp3/3bN1PP/3P2Q1/2B1R3/PPP2P2/2KR1B2 w kq - ",
            "r1bqk2r/pppp1Npp/8/2bnP3/8/6K1/PB4PP/RN1Q3R b kq - ",
            "r4r1k/pbnq1ppp/np3b2/3p1N2/5B2/2N3PB/PP3P1P/R2QR1K1 w - - ",
            "r2qr2k/pbp3pp/1p2Bb2/2p5/2P2P2/3R2P1/PP2Q1NP/5RK1 b - - ",
            "5r2/1p4r1/3kp1b1/1Pp1p2p/2PpP3/q2B1PP1/3Q2K1/1R5R b - - ",
            "8/7p/8/7P/1p6/1p5P/1P2Q1pk/1K6 w - - ",
            "r5k1/p4n1p/6p1/2qPp3/2p1P1Q1/8/1rB3PP/R4R1K b - - ",
            "1r4k1/1q2pN1p/3pPnp1/8/2pQ4/P5PP/5P2/3R2K1 b - - ",
            "2rq1rk1/pb3ppp/1p2pn2/4N3/1b1PPB2/4R1P1/P4PBP/R2Q2K1 w - - ")

    @Before
    @Throws(Exception::class)
    fun setUp() {
        movegen = MagicMoveGenerator()
        legalMovegen = LegalMoveGenerator()
        testBoards = Array(tests.size, { Board() })
        for (i in tests.indices) {
            testBoards[i] = Board()
            testBoards[i].fen = tests[i]
        }
    }

    @Test
    @Category(SlowTest::class)
    fun testCompleteEvaluatorPerf() {
        val attacksInfo = AttacksInfo()
        val completeEvaluator = CompleteEvaluator()

        val t1 = System.currentTimeMillis()
        var positions: Long = 0
        for (i in 0..9999) {
            for (testBoard in testBoards) {
                completeEvaluator.evaluate(testBoard, attacksInfo)
                positions++
            }
        }
        val t2 = System.currentTimeMillis()
        val pps = 1000 * positions / (t2 - t1 + 1)
        println("Positions evaluated per second (complete) = " + pps)
        assertTrue(pps > 100000)
    }

    @Test
    @Category(SlowTest::class)
    fun testSimplifiedEvaluatorPerf() {
        val attacksInfo = AttacksInfo()

        val simplifiedEvaluator = SimplifiedEvaluator()
        val t1 = System.currentTimeMillis()
        var positions: Long = 0
        for (i in 0..9999) {
            for (testBoard in testBoards) {
                simplifiedEvaluator.evaluate(testBoard, attacksInfo)
                positions++
            }
        }
        val t2 = System.currentTimeMillis()
        val pps = 1000 * positions / (t2 - t1 + 1)
        println("Positions evaluated per second (simplified) = " + pps)
        assertTrue(pps > 100000)
    }

    @Test
    @Category(SlowTest::class)
    fun testPseudoLegalMoveGenPerf() {
        val t1 = System.currentTimeMillis()
        var positions: Long = 0
        val moves = IntArray(256)

        for (i in 0..9999) {
            for (j in tests.indices) {
                movegen.generateMoves(testBoards[j], moves, 0)
                positions++
            }
        }
        val t2 = System.currentTimeMillis()
        val pps = 1000 * positions / (t2 - t1 + 1)
        println("Positions with Pseudo legal moves generated per second = " + pps)
    }

    @Test
    @Category(SlowTest::class)
    fun testMoveIteratorNewNewPerf() {
        val searchEngine = SearchEngine(Config())
        val moveIterator = searchEngine.nodes[0].moveIterator

        val t1 = System.currentTimeMillis()
        var positions: Long = 0

        for (i in 0..49999) {
            for (j in tests.indices) {
                moveIterator.setBoard(testBoards[j])
                moveIterator.genMoves(0)
                while (moveIterator.next() != 0) {
                }
                positions++
            }
        }
        val t2 = System.currentTimeMillis()
        val pps = 1000 * positions / (t2 - t1 + 1)
        println("Positions with all moves generated, sorted and transversed per second = " + pps)
    }

    @Test
    @Category(SlowTest::class)
    fun testDoMovePerf() {
        val t1 = System.currentTimeMillis()
        var moveCount: Long = 0
        val moves = IntArray(256)

        for (j in tests.indices) {
            val moveIndex = movegen.generateMoves(testBoards[j], moves, 0)
            for (k in 0..moveIndex - 1) {
                for (i in 0..9999) {
                    if (testBoards[j].doMove(moves[k])) {
                        testBoards[j].undoMove()
                    }
                    moveCount++
                }
            }
        }
        val t2 = System.currentTimeMillis()
        println("Moves done/undone per second = " + 1000 * moveCount / (t2 - t1 + 1))
    }

    @Test
    @Category(SlowTest::class)
    fun testLegalMoveGenPerf() {
        val t1 = System.currentTimeMillis()
        var positions: Long = 0
        val moves = IntArray(256)

        for (i in 0..9999) {
            for (j in tests.indices) {
                legalMovegen.generateMoves(testBoards[j], moves, 0)
                positions++
            }
        }
        val t2 = System.currentTimeMillis()
        val pps = 1000 * positions / (t2 - t1 + 1)
        println("Positions with legal moves generated per second = " + pps)
    }

    @Test
    @Category(SlowTest::class)
    fun testZobrishKeyPerf() {
        val t1 = System.currentTimeMillis()
        var keys: Long = 0
        for (i in 0..9999) {
            for (j in tests.indices) {
                ZobristKey.getKey(testBoards[j])
                keys++
            }
        }
        val t2 = System.currentTimeMillis()
        val pps = 1000 * keys / (t2 - t1 + 1)
        println("keys generated per second = " + pps)
    }
}