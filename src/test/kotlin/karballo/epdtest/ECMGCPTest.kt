package karballo.epdtest

import karballo.SlowTest

import org.junit.Test
import org.junit.experimental.categories.Category

class ECMGCPTest : EpdTest() {

    @Test
    @Category(SlowTest::class)
    fun testECMGCP() {
        processEpdFile(this.javaClass.getResourceAsStream("/ecmgcp.epd"), 5000)
    }
}