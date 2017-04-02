package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class BS2830Test : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testBS2830() {
        val time = processEpdFile(this.javaClass.getResourceAsStream("/bs2830.epd"), 15 * 60000)
        val timeMinutes = (time / 60000).toDouble()
        val elo = 2830.0 - timeMinutes / 1.5 - timeMinutes * timeMinutes / (22 * 22)
        println("BS2830 Elo = " + elo)
    }
}