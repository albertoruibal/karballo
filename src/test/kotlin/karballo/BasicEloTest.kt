package karballo

import karballo.book.FileBook
import karballo.search.SearchEngine
import karballo.search.SearchParameters
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Estimate program ELO, from:
 * http://www.chessmaniac.com/ELORating/ELO_Chess_Rating.shtml
 */
class BasicEloTest {

    lateinit internal var config: Config
    lateinit internal var search: SearchEngine

    @Before
            //@Throws(Exception::class)
    fun setUp() {
        config = Config()
        config.book = FileBook("/book_small.bin")
        search = SearchEngine(config)
    }

    @Test
    @Category(SlowTest::class)
    fun testElo() {
        var elo1 = 1000
        val move1 = processPosition("r1b3k1/6p1/P1n1pr1p/q1p5/1b1P4/2N2N2/PP1QBPPP/R3K2R b")
        if ("f6f3" == move1) {
            elo1 = 2600
        }
        if ("c5d4" == move1) {
            elo1 = 1900
        }
        if ("c6d4" == move1) {
            elo1 = 1900
        }
        if ("b4c3" == move1) {
            elo1 = 1400
        }
        if ("c8a6" == move1) {
            elo1 = 1500
        }
        if ("f6g6" == move1) {
            elo1 = 1400
        }
        if ("e6e5" == move1) {
            elo1 = 1200
        }
        if ("c8d7" == move1) {
            elo1 = 1600
        }
        println(move1 + " Elo1 = " + elo1)

        var elo2 = 1000
        val move2 = processPosition("2nq1nk1/5p1p/4p1pQ/pb1pP1NP/1p1P2P1/1P4N1/P4PB1/6K1 w")
        if ("g2e4" == move2) {
            elo2 = 2600
        }
        if ("g5h7" == move2) {
            elo2 = 1950
        }
        if ("h5g6" == move2) {
            elo2 = 1900
        }
        if ("g2f1" == move2) {
            elo2 = 1400
        }
        if ("g2d5" == move2) {
            elo2 = 1200
        }
        if ("f2f4" == move2) {
            elo2 = 1400
        }
        println(move2 + " Elo2 = " + elo2)

        var elo3 = 1000
        val move3 = processPosition("8/3r2p1/pp1Bp1p1/1kP5/1n2K3/6R1/1P3P2/8 w")
        if ("c5c6" == move3) {
            elo3 = 2500
        }
        if ("g3g6" == move3) {
            elo3 = 2000
        }
        if ("e4e5" == move3) {
            elo3 = 1900
        }
        if ("g3g5" == move3) {
            elo3 = 1700
        }
        if ("e4d4" == move3) {
            elo3 = 1200
        }
        if ("d6e5" == move3) {
            elo3 = 1200
        }
        println(move3 + " Elo3 = " + elo3)

        var elo4 = 1000
        val move4 = processPosition("8/4kb1p/2p3pP/1pP1P1P1/1P3K2/1B6/8/8 w")
        if ("e5e6" == move4) {
            elo4 = 2500
        }
        if ("b3f7" == move4) {
            elo4 = 1600
        }
        if ("b3c2" == move4) {
            elo4 = 1700
        }
        if ("b3d1" == move4) {
            elo4 = 1800
        }
        println(move4 + " Elo4 = " + elo4)

        var elo5 = 1000
        val move5 = processPosition("b1R2nk1/5ppp/1p3n2/5N2/1b2p3/1P2BP2/q3BQPP/6K1 w")
        if ("e3c5" == move5) {
            elo5 = 2500
        }
        if ("f5h6" == move5) {
            elo5 = 2100
        }
        if ("e3h6" == move5) {
            elo5 = 1900
        }
        if ("f5g7" == move5) {
            elo5 = 1500
        }
        if ("f2g3" == move5) {
            elo5 = 1750
        }
        if ("c8f8" == move5) {
            elo5 = 1200
        }
        if ("f2h4" == move5) {
            elo5 = 1200
        }
        if ("e3b6" == move5) {
            elo5 = 1750
        }
        if ("e2c4" == move5) {
            elo5 = 1400
        }
        println(move5 + " Elo5 = " + elo5)

        var elo6 = 1000
        val move6 = processPosition("3rr1k1/pp3pbp/2bp1np1/q3p1B1/2B1P3/2N4P/PPPQ1PP1/3RR1K1 w")
        if ("g5f6" == move6) {
            elo6 = 2500
        }
        if ("c3d5" == move6) {
            elo6 = 1700
        }
        if ("c4b5" == move6) {
            elo6 = 1900
        }
        if ("f2f4" == move6) {
            elo6 = 1700
        }
        if ("a2a3" == move6) {
            elo6 = 1200
        }
        if ("e1e3" == move6) {
            elo6 = 1200
        }
        println(move6 + " Elo6 = " + elo6)

        var elo7 = 1000
        val move7 = processPosition("r1b1qrk1/1ppn1pb1/p2p1npp/3Pp3/2P1P2B/2N5/PP1NBPPP/R2Q1RK1 b")
        if ("f6h7" == move7) {
            elo7 = 2500
        }
        if ("f6e4" == move7) {
            elo7 = 1800
        }
        if ("g6g5" == move7) {
            elo7 = 1700
        }
        if ("a6a5" == move7) {
            elo7 = 1700
        }
        if ("g8h7" == move7) {
            elo7 = 1500
        }
        println(move7 + " Elo7 = " + elo7)

        var elo8 = 1000
        val move8 = processPosition("2R1r3/5k2/pBP1n2p/6p1/8/5P1P/2P3P1/7K w")
        if ("b6d8" == move8) {
            elo8 = 2500
        }
        if ("c8e8" == move8) {
            elo8 = 1600
        }
        println(move8 + " Elo8 = " + elo8)

        var elo9 = 1000
        val move9 = processPosition("2r2rk1/1p1R1pp1/p3p2p/8/4B3/3QB1P1/q1P3KP/8 w")
        if ("e3d4" == move9) {
            elo9 = 2500
        }
        if ("e4g6" == move9) {
            elo9 = 1800
        }
        if ("e4h7" == move9) {
            elo9 = 1800
        }
        if ("e3h6" == move9) {
            elo9 = 1700
        }
        if ("d7b7" == move9) {
            elo9 = 1400
        }
        println(move9 + " Elo9 = " + elo9)

        var elo10 = 1000
        val move10 = processPosition("r1bq1rk1/p4ppp/1pnp1n2/2p5/2PPpP2/1NP1P3/P3B1PP/R1BQ1RK1 b")
        if ("d8d7" == move10) {
            elo10 = 2000
        }
        if ("f6e8" == move10) {
            elo10 = 2000
        }
        if ("h7h5" == move10) {
            elo10 = 1800
        }
        if ("c5d4" == move10) {
            elo10 = 1600
        }
        if ("c8a6" == move10) {
            elo10 = 1800
        }
        if ("a7a5" == move10) {
            elo10 = 1800
        }
        if ("f8e8" == move10) {
            elo10 = 1400
        }
        if ("d6d5" == move10) {
            elo10 = 1500
        }
        println(move10 + " Elo10 = " + elo10)

        val elo = (elo1 + elo2 + elo3 + elo4 + elo5 + elo6 + elo7 + elo8 + elo9 + elo10) / 10
        println("Calculated Elo = " + elo)

        assertTrue(elo > 2000)
    }

    private fun processPosition(fen: String): String {
        search.board.fen = fen
        search.go(SearchParameters[5 * 60000]) // five minutes
        val move = Move.toString(search.bestMove)
        println("result = " + move)
        return move
    }
}