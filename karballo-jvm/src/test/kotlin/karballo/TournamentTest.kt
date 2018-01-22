package karballo

import karballo.book.FileBook
import karballo.log.Logger
import karballo.pgn.PgnFile
import karballo.pgn.PgnImportExport
import karballo.search.SearchEngine
import karballo.search.SearchObserver
import karballo.search.SearchParameters
import karballo.search.SearchStatusInfo

import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Test tournament using the Noomen Test Suite
 */
class TournamentTest : SearchObserver {

    lateinit var engine1: SearchEngine
    lateinit var engine2: SearchEngine
    lateinit var b: Board
    var engine1Whites: Boolean = false
    var endGame: Int = 0
    lateinit var wins: DoubleArray
    var wtime: Int = 0
    var btime: Int = 0
    lateinit var params: SearchParameters
    var lastTime: Long = 0

    @Test
    @Category(SlowTest::class)
    fun testTournament() {
        val config1 = Config()
        config1.book = FileBook("/book_small.bin")
        val config2 = Config()
        config2.book = FileBook("/book_small.bin")

        // Change here the parameters in one of the chess engines to test the differences
        // Example: config1.setElo(2000);
        // ...
        config1.isLimitStrength = true
        config1.elo = 2100
        config2.isLimitStrength = true
        config2.elo = 2000

        engine1 = SearchEngine(config1)
        engine2 = SearchEngine(config2)

        var pgnGameNumber = 0

        // wins[0] = draws
        // wins[1] = engine 1 wins
        // wins[2] = engine 2 wins
        wins = DoubleArray(3)
        params = SearchParameters()

        Logger.noLog = true

        for (i in 0..GAMES - 1) {
            engine1.init()
            engine2.init()

            engine1Whites = i and 1 == 0

            b = Board()

            // Each position is played two times alternating color
            var positionPgn = PgnFile.getGameNumber(this.javaClass.getResourceAsStream("/NoomenTestsuite2012.pgn"), pgnGameNumber.ushr(1))
            if (positionPgn == null) {
                pgnGameNumber = 0
                positionPgn = PgnFile.getGameNumber(this.javaClass.getResourceAsStream("/NoomenTestsuite2012.pgn"), pgnGameNumber.ushr(1))
            } else {
                pgnGameNumber++
            }

            PgnImportExport.setBoard(b, positionPgn!!)
            PgnImportExport.setBoard(engine1.board, positionPgn)
            PgnImportExport.setBoard(engine2.board, positionPgn)

            engine1.setObserver(this)
            engine2.setObserver(this)

            wtime = GAME_TIME_PER_PLAYER
            btime = GAME_TIME_PER_PLAYER

            endGame = 0

            go()

            //System.out.println("move "+(j+1)+": " + Move.toStringExt(bestMove));

            while (endGame == 0) {
                try {
                    Thread.sleep(SLEEP.toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }

            val score = wins[1] + wins[0] / 2
            val total = wins[1] + wins[0] + wins[2]
            val percentage = score / total
            val eloDifference = -400 * Math.log(1 / percentage - 1) / Math.log(10.0)

            println((if (total % TEST_SIZE == 0.0) TestColors.ANSI_WHITE else if (eloDifference > 0) TestColors.ANSI_GREEN else TestColors.ANSI_RED) +
                    "At: " + (i + 1) + " draws: " + wins[0] + " engine1: " + wins[1] + " engine2: " + wins[2] + " elodif: " + String.format("%.2f", eloDifference) + " pointspercentage=" + String.format("%.2f", percentage * 100) + //

                    TestColors.ANSI_RESET)
        }
    }

    private fun go() {
        endGame = b.isEndGame

        // if time is up
        if (wtime < 0) {
            println("White losses by time")
            endGame = -1
        } else if (btime < 0) {
            println("Black losses by time")
            endGame = 1
        }

        if (endGame != 0) {
            var index = -1
            if (endGame == 99) {
                index = 0
            } else if (endGame == 1) {
                if (engine1Whites) {
                    index = 1
                } else {
                    index = 2
                }
            } else if (endGame == -1) {
                if (engine1Whites) {
                    index = 2
                } else {
                    index = 1
                }
            }
            wins[index]++
        } else {
            params.wtime = wtime
            params.winc = MOVE_TIME_INC
            params.btime = btime
            params.binc = MOVE_TIME_INC
            params.depth = THINK_TO_DEPTH
            params.nodes = THINK_TO_NODES

            lastTime = System.currentTimeMillis()

            if (engine1Whites == b.turn) {
                engine1.go(params)
            } else {
                engine2.go(params)
            }
        }
    }

    override fun bestMove(bestMove: Int, ponder: Int) {
        val timeThinked = System.currentTimeMillis() - lastTime
        if (b.turn) {
            wtime -= (timeThinked + MOVE_TIME_INC).toInt()
        } else {
            btime -= (timeThinked + MOVE_TIME_INC).toInt()
        }
        b.doMove(bestMove)
        engine1.board.doMove(bestMove)
        engine2.board.doMove(bestMove)
        go()
    }

    override fun info(info: SearchStatusInfo) {}

    companion object {
        internal val GAME_TIME_PER_PLAYER = 5000 // in milliseconds
        internal val MOVE_TIME_INC = 0 // in milliseconds
        internal val THINK_TO_DEPTH = Integer.MAX_VALUE // if > 0, it establishes a depth limit, used with 3 or 6 to make fast tournaments it is useful to fast test evaluator changes
        internal val THINK_TO_NODES = Integer.MAX_VALUE // When making changes in the karballo.search engine, is better to make tests limiting the karballo.search nodes
        internal val SLEEP = 100
        internal val TEST_SIZE = 60
        internal val GAMES = 20 * TEST_SIZE // Test suite is based on 30 games and they are played with whites and blacks, so we make x60 times
    }
}