package karballo

import karballo.pgn.GameNodeMove
import karballo.pgn.PgnFile
import karballo.pgn.PgnParser
import karballo.util.JvmPlatformUtils
import karballo.util.Utils
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class PgnTest {

    constructor() {
        Utils.instance = JvmPlatformUtils()
    }

    @Test
    fun testGameNotFullyParsed() {
        val file = File("crashgame.pgn")
        try {
            val `is` = FileInputStream(file)

            val pgnString = PgnFile.getGameNumber(`is`, 0)

            println(pgnString)

            val game = PgnParser.parsePgn(pgnString)!!
            println(game.pv)

            var counter = 0
            for (gameNode in game.pv!!.variation) {
                if (gameNode is GameNodeMove) {
                    counter++
                }
            }
            print(counter.toString() + " moves in the root")

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
