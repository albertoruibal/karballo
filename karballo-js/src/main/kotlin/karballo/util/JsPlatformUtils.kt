package karballo.util

import kotlin.js.Date
import kotlin.js.Math

class JsPlatformUtils : PlatformUtils {

    override fun currentTimeMillis(): Long {
        val d = Date()
        return d.getTime().toLong()
    }

    override fun getCurrentDateIso(): String {
        val d = Date()
        return "" +  d.getFullYear() + "." + (d.getMonth() + 1) + "." + d.getDate()
    }

    override fun randomFloat(): Float {
        return Math.random().toFloat()
    }

    override fun randomInt(bound: Int): Int {
        return (Math.random() * bound).toInt()
    }

    override fun arrayFill(array: ShortArray, value: Short) {
        for (i in 0..array.size - 1) {
            array[i] = value
        }
    }

    override fun arrayFill(array: IntArray, value: Int) {
        for (i in 0..array.size - 1) {
            array[i] = value
        }
    }

    override fun arrayFill(array: LongArray, value: Long) {
        for (i in 0..array.size - 1) {
            array[i] = value
        }
    }

    override fun arrayCopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, length: Int) {
        for (i in 0..length - 1) {
            dest[destPos + i] = src[srcPos + i]
        }
    }

    override fun exit(code: Int) {
        js("process.exit(code)") // For NodeJS
    }

    override fun gc() {
    }
}
