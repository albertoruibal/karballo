package karballo

import karballo.evaluation.Evaluator
import karballo.search.*
import karballo.tt.TranspositionTable
import org.junit.Assert.*
import org.junit.Test

class SearchEngineTest : BaseTest() {

    @Test
    fun testRefine() {
        val search = SearchEngine(Config())
        search.board.fen = "3r1rk1/p3qp1p/2bb2p1/2p5/3P4/1P6/PBQN1PPP/2R2RK1 b - -"
        search.nodes[0].staticEval = -11
        var refine: Int
        var foundTT: Boolean

        search.tt[search.board, TranspositionTable.TYPE_FAIL_LOW, 0, 0, Move.NONE, 23, search.nodes[0].staticEval] = false
        foundTT = search.tt.search(search.board, 0, false)
        refine = search.refineEval(search.nodes[0], foundTT)
        assertEquals("Must find it in the TT", true, foundTT)
        assertEquals("Must be fail low in the TT", TranspositionTable.TYPE_FAIL_LOW, search.tt.nodeType)
        assertEquals("Must not refine", search.nodes[0].staticEval.toLong(), refine.toLong())

        search.tt[search.board, TranspositionTable.TYPE_FAIL_HIGH, 0, 0, Move.NONE, 45, search.nodes[0].staticEval] = false
        foundTT = search.tt.search(search.board, 0, false)
        refine = search.refineEval(search.nodes[0], foundTT)
        assertEquals("Must find it in the TT", true, foundTT)
        assertEquals("Must be fail high in the TT", TranspositionTable.TYPE_FAIL_HIGH, search.tt.nodeType)
        assertEquals("Must refine", 45, refine.toLong())

        search.nodes[0].staticEval = 40
        search.tt[search.board, TranspositionTable.TYPE_FAIL_LOW, 0, 0, Move.NONE, 23, search.nodes[0].staticEval] = false
        foundTT = search.tt.search(search.board, 0, false)
        refine = search.refineEval(search.nodes[0], foundTT)
        assertEquals("Must be fail low in the TT", TranspositionTable.TYPE_FAIL_LOW, search.tt.nodeType)
        assertEquals("Must refine", 23, refine.toLong())

        search.nodes[0].staticEval = 40
        search.tt[search.board, TranspositionTable.TYPE_FAIL_HIGH, 0, 0, Move.NONE, 45, search.nodes[0].staticEval] = false
        foundTT = search.tt.search(search.board, 0, false)
        refine = search.refineEval(search.nodes[0], foundTT)
        assertEquals("Must be fail high in the TT", TranspositionTable.TYPE_FAIL_HIGH, search.tt.nodeType)
        assertEquals("Must refine", 45, refine.toLong())

        search.nodes[0].staticEval = 40
        search.tt[search.board, TranspositionTable.TYPE_EXACT_SCORE, 0, 0, Move.NONE, 43, search.nodes[0].staticEval] = false
        foundTT = search.tt.search(search.board, 0, false)
        refine = search.refineEval(search.nodes[0], foundTT)
        assertEquals("Must be exact score", TranspositionTable.TYPE_EXACT_SCORE, search.tt.nodeType)
        assertEquals("Must refine", 43, refine.toLong())
    }

    @Test
    fun testSearchAlreadyMate() {
        assertTrue("none" == getSearchBestMoveSan("r7/4K1q1/r7/1p5p/4k3/8/8/8 w - - 8 75", 1))
    }

    @Test
    fun testBishopTrapped() {
        val san = getSearchBestMoveSan("4k3/5ppp/8/8/8/8/2B5/K7 w - - 8 75", 10)
        assertNotEquals("Bxh7", san)
    }

    @Test
    fun testOpeningWithE4OrD4() {
        val san = getSearchBestMoveSan(Board.FEN_START_POSITION, 14)
        assertTrue("e4" == san || "d4" == san)
    }

    @Test
    fun testAnalysisMateCrash() {
        getSearchScore("8/8/8/8/8/k7/8/K6r w - - 1 1", 15)
    }

    @Test
    fun testRetiEndgameStudy() {
        assertTrue(getSearchScore("7K/8/k1P5/7p/8/8/8/8 w - -", 15) == Evaluator.DRAW.toLong())
    }

    @Test
    fun testSameResultsAfterClear() {
        val fen = "rnq1nrk1/pp3pbp/6p1/3p4/3P4/5N2/PP2BPPP/R1BQK2R w KQ - 0 1"

        val search = SearchEngine(Config())
        search.debug = true
        search.board.fen = fen

        val searchParams = SearchParameters()
        searchParams.depth = 10
        search.go(searchParams)
        val nodes1 = search.nodeCount

        search.clear()

        search.go(searchParams)
        val nodes2 = search.nodeCount
        assertEquals(nodes1, nodes2)
    }

    inner class DetectBestMoveSearchObserver : SearchObserver {
        internal var notifiedBestMove = false

        override fun info(info: SearchStatusInfo) {}

        override fun bestMove(bestMove: Int, ponder: Int) {
            notifiedBestMove = true
        }
    }

    @Test
    fun testDoNotSendBestBoveWithPonder() {
        // A mate in 2 puzzle should end ponder
        val fen = "2bqkbn1/2pppp2/np2N3/r3P1p1/p2N2B1/5Q2/PPPPKPP1/RNB2r2 w KQkq - 0 1"

        val search = SearchEngineThreaded(Config())
        search.debug = true
        search.board.fen = fen

        val searchObserver = DetectBestMoveSearchObserver()
        search.setObserver(searchObserver)

        val searchParams = SearchParameters()
        searchParams.isPonder = true

        search.go(searchParams)
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
        }

        assertEquals(false, searchObserver.notifiedBestMove)

        search.stop()
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
        }

        assertEquals(true, searchObserver.notifiedBestMove)
    }
}