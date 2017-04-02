package karballo

import karballo.evaluation.Evaluator
import org.junit.Assert.assertTrue
import org.junit.Test

class ContemptFactorTest : BaseTest() {

    @Test
    fun testContemp1() {
        assertTrue(getSearchScore("7k/7p/5P1K/8/8/8/8/8 w", 18) == Evaluator.DRAW.toLong())
    }
}
