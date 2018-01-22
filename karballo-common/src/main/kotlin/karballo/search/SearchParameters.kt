package karballo.search

import karballo.log.Logger
import kotlin.math.min

class SearchParameters {

    // UCI parameters
    // List of moves to search, if null search all moves
    internal var searchMoves: ArrayList<Int>? = null

    // Remaining time
    var wtime: Int = 0
    var btime: Int = 0
    // Time increment per move
    var winc: Int = 0
    var binc: Int = 0
    // Moves to the next time control
    var movesToGo: Int = 0
    // Analize x plyes only
    var depth = Int.MAX_VALUE
    // Search only this number of nodes
    var nodes = Int.MAX_VALUE
    // Search for mate in mate moves
    var mate: Int = 0
    // Search movetime milliseconds
    var moveTime = Int.MAX_VALUE
    // Think infinite
    var isInfinite: Boolean = false
    var isPonder: Boolean = false

    internal var manageTime: Boolean = false

    fun clearSearchMoves() {
        if (searchMoves == null) {
            searchMoves = ArrayList<Int>()
        }
        searchMoves!!.clear()
    }

    fun addSearchMove(move: Int) {
        if (searchMoves == null) {
            searchMoves = ArrayList<Int>()
        }
        searchMoves!!.add(move)
    }

    /**
     * Used to detect if it can add more time in case of panic or apply other heuristics to reduce time

     * @return true if the engine is responsible of managing the remaining time
     */
    fun manageTime(): Boolean {
        return manageTime
    }

    /**
     * Time management routine
     * @param panicTime is set to true when the score fails low in the root node by 100
     * *
     * *
     * @return the time to think, or Long.MAX_VALUE if it can think an infinite time
     */
    fun calculateMoveTime(engineIsWhite: Boolean, startTime: Long, panicTime: Boolean): Long {
        manageTime = false
        if (isPonder || isInfinite || depth < Int.MAX_VALUE || nodes < Int.MAX_VALUE) {
            return Long.MAX_VALUE
        }
        if (moveTime != Int.MAX_VALUE) {
            return startTime + moveTime
        }
        manageTime = true

        var calcTime = 0
        val timeAvailable = if (engineIsWhite) wtime else btime
        val timeInc = if (engineIsWhite) winc else binc
        if (timeAvailable > 0) {
            calcTime = timeAvailable / if (movesToGo != 0) movesToGo else 25
        }
        if (panicTime) { // x 4
            calcTime = calcTime shl 2
        }
        calcTime = min(calcTime, timeAvailable.ushr(3)) // Never consume more than time / 8
        calcTime += timeInc

        logger.debug("Thinking for " + calcTime + "Ms")
        return startTime + calcTime
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = Logger.getLogger("SearchParameters")

        operator fun get(moveTime: Int): SearchParameters {
            val searchParameters = SearchParameters()
            searchParameters.moveTime = moveTime
            return searchParameters
        }
    }
}
