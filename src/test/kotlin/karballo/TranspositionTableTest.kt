package karballo

import karballo.evaluation.Evaluator
import karballo.tt.TranspositionTable
import org.junit.Assert.assertEquals
import org.junit.Test

class TranspositionTableTest {

    @Test
    fun testTraspositionTable() {
        val b = Board()
        b.startPosition()
        val tt = TranspositionTable(20)

        val nodeType = TranspositionTable.TYPE_EXACT_SCORE
        val bestMove = Move.getFromString(b, "e2e4", true)
        val score = -100
        val depthAnalyzed = -1
        val eval: Short = 456
        tt[b, nodeType, 0, depthAnalyzed, bestMove, score, eval.toInt()] = false
        tt.search(b, 0, false)
        assertEquals(nodeType.toLong(), tt.nodeType.toLong())
        assertEquals(bestMove.toLong(), tt.bestMove.toLong())
        assertEquals(score.toLong(), tt.score.toLong())
        assertEquals(depthAnalyzed.toLong(), tt.depthAnalyzed.toLong())
        assertEquals(eval.toLong(), tt.getEval().toLong())
    }

    @Test
    fun testDistanceToInitialPly() {
        val tt = TranspositionTable(20)
        val b = Board()
        b.fen = "8/7K/8/8/8/8/R7/7k w - - 0 1"

        val bestMove = Move.getFromString(b, "a2f2", true)
        val score = Evaluator.MATE - 8
        val depthAnalyzed = 4
        // Must store SearchEngine.VALUE_IS_MATE - 4
        tt[b, TranspositionTable.TYPE_EXACT_SCORE, 4, depthAnalyzed, bestMove, score, 0] = false
        tt.search(b, 1, false)
        assertEquals("It does not fix the mate score in the transposition table", tt.score.toLong(), (Evaluator.MATE - 5).toLong())
    }
}