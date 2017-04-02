package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class WCSACTest : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testWinningChessSacrificesAndCombinations() {
        processEpdFile(this.javaClass.getResourceAsStream("/wcsac.epd"), 5000)
    }
}