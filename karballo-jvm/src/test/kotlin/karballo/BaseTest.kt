package karballo

import karballo.bitboard.AttacksInfo
import karballo.evaluation.CompleteEvaluator
import karballo.evaluation.Evaluator
import karballo.search.SearchEngine
import karballo.search.SearchParameters
import karballo.util.JvmPlatformUtils
import karballo.util.Utils

import org.junit.Assert.assertTrue

abstract class BaseTest {

    constructor() {
        Utils.instance = JvmPlatformUtils()
    }

    fun getEval(fen: String): Int {
        val attacksInfo = AttacksInfo()
        val evaluator = CompleteEvaluator()
        evaluator.debug = true
        val board = Board()
        board.fen = fen
        return evaluator.evaluate(board, attacksInfo)
    }

    /**
     * Compares the eval of two fens
     */
    fun compareEval(fenBetter: String, fenWorse: String, requiredDifference: Int) {
        println("*\n* Comparing two board evaluations (first must be better for white):\n*")
        val valueBetter = getEval(fenBetter)
        val valueWorse = getEval(fenWorse)
        println("valueBetter = " + valueBetter)
        println("valueWorse = " + valueWorse)
        assertTrue(valueBetter > valueWorse + requiredDifference)
    }

    fun getSearchQS(fen: String): Int {
        val search = SearchEngine(Config())
        search.board.fen = fen
        try {
            return search.quiescentSearch(0, -Evaluator.MATE, Evaluator.MATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0
    }

    fun getSearchScore(fen: String, depth: Int): Long {
        val search = SearchEngine(Config())
        search.debug = true
        val searchParams = SearchParameters()
        search.board.fen = fen
        searchParams.depth = depth
        search.go(searchParams)
        return search.bestMoveScore.toLong()
    }

    fun getSearchBestMoveSan(fen: String, depth: Int): String {
        val search = SearchEngine(Config())
        search.debug = true
        val searchParams = SearchParameters()
        search.board.fen = fen
        searchParams.depth = depth
        search.go(searchParams)
        return Move.toSan(search.board, search.bestMove)
    }
}
