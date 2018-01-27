package karballo

import karballo.bitboard.AttacksInfo
import karballo.bitboard.BitboardUtils
import karballo.util.JvmPlatformUtils
import karballo.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Test

class AttacksInfoTest {

    constructor() {
        Utils.instance = JvmPlatformUtils()
    }

    @Test
    fun testPinnedBishop() {
        val b = Board()
        b.fen = "3k4/3r4/8/3B4/2P5/8/8/3K4 b - - 1 1"
        println(b)
        val ai = AttacksInfo()
        ai.build(b)
        println(BitboardUtils.toString(ai.bishopAttacks[0]))
        assertEquals(0, ai.bishopAttacks[0])
    }

    @Test
    fun testPinnedRook() {
        val b = Board()
        b.fen = "3k4/3r4/8/3R4/2P5/8/8/3K4 b - - 1 1"
        println(b)
        val ai = AttacksInfo()
        ai.build(b)
        println(BitboardUtils.toString(ai.rookAttacks[0]))
        assertEquals(5, BitboardUtils.popCount(ai.rookAttacks[0]).toLong())
    }

    @Test
    fun testPinnedPawn() {
        val b = Board()
        b.fen = "3k4/8/2b5/3P4/8/8/8/7K b - - 1 1"
        println(b)
        val ai = AttacksInfo()
        ai.build(b)
        println(BitboardUtils.toString(ai.pawnAttacks[0]))
        assertEquals(1, BitboardUtils.popCount(ai.pawnAttacks[0]).toLong())
    }
}