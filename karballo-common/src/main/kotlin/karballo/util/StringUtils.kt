package karballo.util

object StringUtils {
    private val SPACES = "                     "

    fun padRight(str: String, totalChars: Int): String {
        return str + SPACES.substring(0, totalChars - str.length)
    }

    fun padLeft(str: String, totalChars: Int): String {
        return SPACES.substring(0, totalChars - str.length) + str
    }
}
