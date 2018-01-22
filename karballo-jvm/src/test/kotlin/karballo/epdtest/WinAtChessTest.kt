package karballo.epdtest

import karballo.SlowTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category

class WinAtChessTest : EpdTest() {
    @Test
    @Category(SlowTest::class)
    fun testWinAtChess() {
        processEpdFile(this.javaClass.getResourceAsStream("/wacnew.epd"), 1000)
        assertTrue(fails <= 16)
    }
}