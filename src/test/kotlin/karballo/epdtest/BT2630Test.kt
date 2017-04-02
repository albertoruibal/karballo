package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class BT2630Test : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testBT2630() {
        val time = processEpdFile(this.javaClass.getResourceAsStream("/bt2630.epd"), 15 * 60000)
        val timeSeconds = (time / 1000).toDouble()
        val elo = 2630 - timeSeconds / 30
        println("BT2630 Elo = " + elo)
    }
}