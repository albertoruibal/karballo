package karballo.pgn

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object PgnFile {

    val UTF8_BOM = "\uFEFF"

    fun getGameNumber(`is`: InputStream, gameNumber: Int): String? {
        val br = BufferedReader(InputStreamReader(`is`))
        var line: String?
        var counter = 0
        try {
            while (true) {
                line = br.readLine()
                if (line == null) {
                    break
                }

                if (line.startsWith(UTF8_BOM)) {
                    line = line.substring(1)
                }

                if (line.startsWith("[Event ")) {
                    if (counter == gameNumber) {
                        val pgnSb = StringBuilder()
                        try {
                            while (true) {
                                pgnSb.append(line)
                                pgnSb.append("\n")
                                line = br.readLine()
                                if (line == null || line.startsWith("[Event ")) {
                                    break
                                }
                            }
                        } catch (ignored: IOException) {
                        }

                        return pgnSb.toString()
                    }
                    counter++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

}