package karballo.util

import java.text.SimpleDateFormat
import java.util.*

class JvmPlatformUtils : PlatformUtils {

    private val random = Random()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun randomFloat(): Float {
        return random.nextFloat()
    }

    override fun randomInt(bound: Int): Int {
        return random.nextInt(bound)
    }

    override fun getCurrentDateIso(): String {
        return dateFormat.format(Date())
    }

    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    override fun arrayFill(array: ShortArray, value: Short) {
        Arrays.fill(array, value)
    }

    override fun arrayFill(array: IntArray, value: Int) {
        Arrays.fill(array, value)
    }

    override fun arrayFill(array: LongArray, value: Long) {
        Arrays.fill(array, value)
    }

    override fun arrayCopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, length: Int) {
        System.arraycopy(src, srcPos, dest, destPos, length)
    }

    override fun exit(code: Int) {
        System.exit(code)
    }

    override fun gc() {
        System.gc()
    }
}
