package karballo.util

/**
 * Utils that have optimized implementations on each platform (jvm/js)
 */
interface PlatformUtils {
    fun randomFloat() : Float

    fun randomInt(bound: Int) : Int

    fun currentTimeMillis() : Long

    fun getCurrentDateIso() : String

    fun arrayFill(array: ShortArray, value: Short)

    fun arrayFill(array: IntArray, value: Int)

    fun arrayFill(array: LongArray, value: Long)

    fun arrayCopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, length: Int)

    fun exit(code: Int)

    fun gc()
}