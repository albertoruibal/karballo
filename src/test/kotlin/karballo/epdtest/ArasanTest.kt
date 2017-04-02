package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class ArasanTest : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testArasan() {
        processEpdFile(this.javaClass.getResourceAsStream("/arasan.epd"), 60000)
    }
}