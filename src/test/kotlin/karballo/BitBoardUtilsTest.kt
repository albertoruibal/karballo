package karballo

import karballo.bitboard.BitboardUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class BitBoardUtilsTest {

    @Test
    fun testHorizontalLine() {
        assertEquals(1L shl 7 or (1L shl 6) or (1L shl 5), BitboardUtils.getHorizontalLine(1L shl 7, 1L shl 5))
        assertEquals(1L shl 63 or (1L shl 62) or (1L shl 61) or (1L shl 60), BitboardUtils.getHorizontalLine(1L shl 63, 1L shl 60))
    }

    @Test
    fun testLsb() {
        assertEquals(1L, BitboardUtils.lsb(1L))
        assertEquals(1L shl 63, BitboardUtils.lsb(1L shl 63))
        assertEquals(1L shl 32, BitboardUtils.lsb(1L shl 63 or (1L shl 32)))
        assertEquals(1L, BitboardUtils.lsb(1L shl 32 or 1L))
        assertEquals(0, BitboardUtils.lsb(0))
    }

    @Test
    fun testMsb() {
        assertEquals(1L, BitboardUtils.msb(1L))
        assertEquals(1L shl 63, BitboardUtils.msb(1L shl 63))
        assertEquals(1L shl 63, BitboardUtils.msb(1L shl 63 or (1L shl 32)))
        assertEquals(1L shl 32, BitboardUtils.msb(1L shl 32 or 1L))
        assertEquals(0, BitboardUtils.msb(0))
    }
}
