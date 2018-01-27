package karballo

import karballo.bitboard.AttacksInfo
import karballo.evaluation.CompleteEvaluator
import karballo.evaluation.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class EvaluatorTest : BaseTest() {

    @Test
    fun testEvaluatorSimmetry1() {
        assertEquals(Evaluator.o(CompleteEvaluator.TEMPO), getEval("r2q1rk1/ppp2ppp/2n2n2/1B1pp1B1/1b1PP1b1/2N2N2/PPP2PPP/R2Q1RK1 w QKqk - 0 0"))
    }

    @Test
    fun testEvaluatorSimmetry2() {
        assertEquals(Evaluator.e(CompleteEvaluator.TEMPO), getEval("7k/7p/6p1/3Np3/3Pn3/1P6/P7/K7 w - - 0 0"))
    }

    @Test
    fun testPawnClassification() {
        val board = Board()
        val attacksInfo = AttacksInfo()
        val evaluator = CompleteEvaluator()
        evaluator.debug = true
        evaluator.debugPawns = true

        board.fen = "8/8/7p/1P2Pp1P/2Pp1PP1/8/8/7K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("Four passers", 4, countSubstring("passed ", evaluator.debugSB.toString()).toLong())
        assertEquals("One outside passed", 1, countSubstring("outside ", evaluator.debugSB.toString()).toLong())
        assertEquals("Three supported", 3, countSubstring("supported ", evaluator.debugSB.toString()).toLong())
        assertEquals("Six connected", 6, countSubstring("connected ", evaluator.debugSB.toString()).toLong())
        assertEquals("Two isolated", 2, countSubstring("isolated ", evaluator.debugSB.toString()).toLong())
        assertEquals("Four opposed", 4, countSubstring("opposed ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/p6p/PP6/6P1/8/7P/8/7K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("Two candidates", 2, countSubstring("candidate ", evaluator.debugSB.toString()).toLong())
        assertEquals("No backward", 0, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/p7/8/PP3ppp/8/5P1P/8/7K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("Two candidates", 2, countSubstring("candidate ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/3p4/1p6/2PP4/8/8/7K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("Two candidates", 2, countSubstring("candidate ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/3r4/8/3p4/8/8/8/R6K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("Runner", 1, countSubstring("runner ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/8/3p4/8/8/1r6/R6K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("No Runner", 0, countSubstring("runner ", evaluator.debugSB.toString()).toLong())
        assertEquals("Mobile", 1, countSubstring("mobile ", evaluator.debugSB.toString()).toLong())
        assertEquals("No Outside", 0, countSubstring("outside ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/8/3p4/R7/8/1r6/7K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("No Runner", 0, countSubstring("runner ", evaluator.debugSB.toString()).toLong())
        assertEquals("No Mobile", 0, countSubstring("mobile ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/5ppp/8/2p5/P7/8/5PPP/7K w - - 0 0"
        assertTrue("Outside passer superior to inside passer", evaluator.evaluate(board, attacksInfo) > 10)

        board.fen = "7k/8/8/5P2/5P2/8/8/7K w - - 0 0"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("Doubled", 1, countSubstring("doubled ", evaluator.debugSB.toString()).toLong())
        assertEquals("Not connected", 0, countSubstring("connected ", evaluator.debugSB.toString()).toLong())
        assertEquals("Only one passed", 1, countSubstring("passed ", evaluator.debugSB.toString()).toLong())

        board.fen = "R7/3p3p/8/3P2P1/3k4/1p5p/1P1NKP1P/7q w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("One backward", 1, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/8/3p1p2/1p1P1Pp1/1P2P1P1/P1P4P/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("Five backward", 5, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/7P/8/5p2/8/8/6P1/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("One backward", 1, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/Pp6/8/8/1P6/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("One backward", 1, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/P1p5/8/8/1P6/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("One backward", 1, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/2p5/P7/8/8/1P6/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("One backward", 1, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/2p5/8/P7/8/1P6/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("No backward", 0, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/8/P7/2p5/1P6/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("No backward", 0, countSubstring("backward ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/8/4p3/8/4pp2/8/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("One backward", 1, countSubstring("backward ", evaluator.debugSB.toString()).toLong())
        assertEquals("One Doubled", 1, countSubstring("doubled ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/4p3/8/5p2/4p3/8/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("No backward", 0, countSubstring("backward ", evaluator.debugSB.toString()).toLong())
        assertEquals("One Doubled", 1, countSubstring("doubled ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/4p3/8/5p2/3Pp3/8/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("One backward", 1, countSubstring("backward ", evaluator.debugSB.toString()).toLong())
        assertEquals("One Doubled", 1, countSubstring("doubled ", evaluator.debugSB.toString()).toLong())

        board.fen = "7k/2P5/pp6/1P6/8/8/8/7K w - -"
        evaluator.evaluate(board, attacksInfo)
        assertEquals("No backward because it can capture", 0, countSubstring("backward ", evaluator.debugSB.toString()).toLong())
    }

    @Test
    fun testPassedPawn1() {
        assertTrue(getEval("7k/7p/P7/8/8/6p1/7P/7K w QKqk - 0 0") > 0)
    }

    @Test
    fun testKnightTrapped() {
        assertTrue(getEval("NPP5/PPP5/PPP5/8/8/8/8/k6K w - - 0 0") > 0)
    }

    @Test
    fun testKingSafety() {
        assertTrue(getEval("r6k/1R6/8/7p/7P/8/8/7K w QKqk - 0 0") > 0)
    }

    @Test
    fun testBishopBonus() {
        compareEval("3BB2k/8/8/8/8/8/p7/7K w QKqk - 0 0", "2B1B2k/8/8/8/8/8/p7/7K w QKqk - 0 0", 40)
    }

    @Test
    fun testSBDCastling() {
        compareEval("r4r2/pppbkp2/2n3p1/3Bp2p/4P2N/2P5/PP3PPP/2KR3R b q - 0 1",
                "2kr1r2/pppb1p2/2n3p1/3Bp2p/4P2N/2P5/PP3PPP/2KR3R b - - 0 1", 0)
    }

    @Test
    fun testConnectedPassersVsCandidate() {
        assertTrue(getEval("8/p1p5/6pp/PPP2k2/8/4PK2/8/8 w - - 0 43") > 0)
    }

    companion object {

        fun countSubstring(subStr: String, str: String): Int {
            return (str.length - str.replace(subStr, "").length) / subStr.length
        }
    }
}