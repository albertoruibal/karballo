package karballo

import karballo.evaluation.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test Opening/Ending two short values in the same integer arithmetic
 */
class EvaluatorOpeningEndgameTest {

    @Test
    fun conversionA() {
        val value = Evaluator.oe(-89, 54)
        assertEquals("Conversion O", -89, Evaluator.o(value))
        assertEquals("Conversion E", 54, Evaluator.e(value))
    }

    @Test
    fun conversionB() {
        val value = Evaluator.oe(54, -89)
        assertEquals("Conversion O", 54, Evaluator.o(value))
        assertEquals("Conversion E", -89, Evaluator.e(value))
    }

    @Test
    fun add() {
        val value = Evaluator.oe(12, 38) + Evaluator.oe(9, 67)
        assertEquals("Add O", 21, Evaluator.o(value))
        assertEquals("Add E", 105, Evaluator.e(value))
    }

    @Test
    fun subFromNegative() {
        val value = Evaluator.oe(-8, -8) - Evaluator.oe(8, 8)
        assertEquals("Sub O", -16, Evaluator.o(value))
        assertEquals("Sub E", -16, Evaluator.e(value))
    }

    @Test
    fun subThreeNegatives() {
        val value = Evaluator.oe(-8, -8) - Evaluator.oe(8, 8) - Evaluator.oe(8, 8)
        assertEquals("Sub O", -24, Evaluator.o(value))
        assertEquals("Sub E", -24, Evaluator.e(value))
    }

    @Test
    fun subSwitchToNegativeA() {
        val value = Evaluator.oe(10, 10) - Evaluator.oe(12, 12)
        assertEquals("Sub O", -2, Evaluator.o(value))
        assertEquals("Sub E", -2, Evaluator.e(value))
    }

    @Test
    fun mutiplyPositive() {
        val value = 5 * Evaluator.oe(4, 50)
        assertEquals("Multiply O", 20, Evaluator.o(value))
        assertEquals("Multiply E", 250, Evaluator.e(value))
    }

    @Test
    fun openingValueMustBeZero() {
        val value = Evaluator.oe(0, -5)
        assertEquals("O must be zero", 0, Evaluator.o(value))
    }

    @Test
    fun endgameValueMustBeZero() {
        val value = Evaluator.oe(-5, 0)
        assertEquals("E must be zero", 0, Evaluator.e(value))
    }
}