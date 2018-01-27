package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class LTCIITest : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testLCTII() {
        processEpdFile(this.javaClass.getResourceAsStream("/lctii.epd"), 10 * 60000)
        println("LCTII ELO = " + (1900 + lctPoints))
    }
}