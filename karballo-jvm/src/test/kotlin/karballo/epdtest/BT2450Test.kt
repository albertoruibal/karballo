package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class BT2450Test : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testBT2450() {
        val time = processEpdFile(this.javaClass.getResourceAsStream("/bt2450.epd"), 15 * 60000)
        val timeSeconds = (time / 1000).toDouble()
        val elo = 2450 - timeSeconds / 30
        println("BT2450 Elo = " + elo)
    }
}