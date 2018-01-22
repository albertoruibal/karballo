package karballo

import karballo.search.*

import org.junit.Before
import org.junit.Test

class SearchEngineMinimumTimeTest {

    lateinit var config: Config
    lateinit var searchEngine: SearchEngine
    var endTime: Long = 0

    @Before
    @Throws(Exception::class)
    fun setUp() {
        config = Config()
        searchEngine = SearchEngineThreaded(config)
    }

    @Test
    fun testMinimumTime() {
        searchEngine.board.fen = "rq2r1k1/5pp1/p7/4bNP1/1p2P2P/5Q2/PP4K1/5R1R w - -"
        searchEngine.go(SearchParameters[500])
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        searchEngine.board.doMove(searchEngine.bestMove)

        searchEngine.setObserver(object : SearchObserver {
            override fun info(info: SearchStatusInfo) {
                println("info " + info.toString())
            }

            override fun bestMove(bestMove: Int, ponder: Int) {
                println("bestMove " + Move.toString(bestMove))
                endTime = System.nanoTime()
            }
        })
        val time1m = System.currentTimeMillis()
        val time1 = System.nanoTime()
        searchEngine.go(SearchParameters[1])
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        println("Time startTime = " + (searchEngine.startTime - time1m) + " mS")
        println("Time elapsed = " + (endTime - time1) + " nS")
    }
}
