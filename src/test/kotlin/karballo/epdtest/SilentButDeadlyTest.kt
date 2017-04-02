package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class SilentButDeadlyTest : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testSilentButDeadly() {
        processEpdFile(this.javaClass.getResourceAsStream("/silentbutdeadly.epd"), 1000)
    }
}