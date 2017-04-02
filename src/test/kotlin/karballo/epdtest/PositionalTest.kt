package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class PositionalTest : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testPositional() {
        val time = processEpdFile(this.javaClass.getResourceAsStream("/positional.epd"), 15 * 60 * 1000)
        val timeSeconds = (time / 1000).toDouble()
        println("time in seconds = " + timeSeconds)
    }
}