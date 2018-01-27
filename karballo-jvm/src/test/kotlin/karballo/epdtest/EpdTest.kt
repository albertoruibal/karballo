package karballo.epdtest

import karballo.Config
import karballo.Move
import karballo.TestColors
import karballo.log.Logger
import karballo.search.SearchEngine
import karballo.search.SearchObserver
import karballo.search.SearchParameters
import karballo.search.SearchStatusInfo
import karballo.util.StringUtils
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Estimate ELO with BS2850, BT2450, BT2630 test suites:
 *
 *
 * Think 15 minutes in each position, if best move is found, time is the first time best move is seen
 * If best move is changed during thinking time, and then newly found, the time to figure is the last
 * one.
 *
 *
 * If best move is not found, time for the test is 15 minutes.
 *
 *
 * ELO is calculated with Total Time (TT) in minutes
 *
 *
 * ELO = 2830 - (TT / 1.5) - (TT * TT) / (22 * 22)
 *
 *
 * 2450/2630
 * Each test suite contain 30 positions. Select each position think for 15 minutes (900 seconds).
 * If a position is solved, write down its solution time in seconds. It doesn't count as a solution if finds
 * the move and then changes its mind. If after finding a move, then changing its mind, then finding it again,
 * you should use the last time found. Any solution that is not found, score as 900 seconds.
 * add up all the times, divide by 30 and subtract the result from either 2630 or 2450.

 * @author rui
 */
open class EpdTest : SearchObserver {

    lateinit internal var search: SearchEngine

    var solved: Int = 0
        internal set
    internal var fails: Int = 0
    internal var total: Int = 0
    internal var totalTime: Int = 0
    internal var totalNodes: Long = 0
    var lctPoints: Int = 0
        internal set

    lateinit internal var avoidMoves: IntArray
    lateinit internal var bestMoves: IntArray
    internal var solutionFound: Boolean = false

    internal var bestMove: Int = 0
    internal var solutionTime: Int = 0
    internal var solutionNodes: Long = 0

    lateinit internal var allSolutionTimes: ArrayList<Int>
    lateinit internal var allSolutionNodes: ArrayList<Long>

    internal fun processEpdFile(`is`: InputStream, timeLimit: Int): Long {
        val config = Config()
        return processEpdFile(config, `is`, timeLimit)
    }

    internal fun processEpdFile(config: Config, `is`: InputStream, timeLimit: Int): Long {
        logger.debug(config)
        search = SearchEngine(config)
        search.debug = true
        search.setObserver(this)

        allSolutionTimes = ArrayList<Int>()
        allSolutionNodes = ArrayList<Long>()

        totalTime = 0
        totalNodes = 0
        lctPoints = 0
        solved = 0
        total = 0
        val notSolved = StringBuilder()
        // goes through all positions
        val br = BufferedReader(InputStreamReader(`is`))
        try {
            var line: String
            while (true) {
                line = br.readLine()
                if (line == null) {
                    break
                }
                logger.debug("Test = " + line)
                // TODO use strtok
                var avoidMovesString: String? = null
                val i0 = line.indexOf(" am ")
                if (i0 >= 0) {
                    val i2 = line.indexOf(";", i0 + 4)
                    avoidMovesString = line.substring(i0 + 4, i2)
                }
                var bestMovesString: String? = null
                val i1 = line.indexOf(" bm ")
                if (i1 >= 0) {
                    val i2 = line.indexOf(";", i1 + 4)
                    bestMovesString = line.substring(i1 + 4, i2)
                }

                val timeSolved = testPosition(line.substring(0, if (i0 != -1) i0 else i1), avoidMovesString, bestMovesString, timeLimit)
                totalTime += timeSolved

                /*
				 *    * 30 points, if solution is found between 0 and 9 seconds
				 *    * 25 points, if solution is found between 10 and 29 seconds
				 *    * 20 points, if solution is found between 30 and 89 seconds
				 *    * 15 points, if solution is found between 90 and 209 seconds
				 *    * 10 points, if solution is found between 210 and 389 seconds
				 *    * 5 points, if solution is found between 390 and 600 seconds
				 *    * 0 points, if not found with in 10 minutes
				 */
                if (timeSolved < timeLimit) {
                    if (timeSolved in 0..9999) {
                        lctPoints += 30
                    } else if (timeSolved in 10000..29999) {
                        lctPoints += 25
                    } else if (timeSolved in 30000..89999) {
                        lctPoints += 20
                    } else if (timeSolved in 90000..209999) {
                        lctPoints += 15
                    } else if (timeSolved in 210000..389999) {
                        lctPoints += 10
                    } else if (timeSolved in 390000..599999) {
                        lctPoints += 5
                    }
                } else {
                    notSolved.append(line)
                    notSolved.append("\n")
                }

                total++
                if (timeSolved < timeLimit) {
                    solved++
                }
                logger.debug("Status: " + solved + " positions solved of " + total + " in " + totalTime + "Ms and " + totalNodes + " nodes (lctPoints=" + lctPoints + ")")
                logger.debug("")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fails = total - solved

        logger.debug("TEST    TIME       NODES")
        for (i in allSolutionTimes.indices) {
            logger.debug(StringUtils.padRight((i + 1).toString(), 4) + StringUtils.padLeft(allSolutionTimes[i].toString(), 8) + StringUtils.padLeft(allSolutionNodes[i].toString(), 12))
        }
        logger.debug("***** Positions not Solved:")
        logger.debug(notSolved.toString())
        logger.debug("***** Result:" + solved + " positions solved of " + total + " in " + totalTime + "Ms and " + totalNodes + " nodes (" + fails + " fails)")

        return totalTime.toLong()
    }

    private fun parseMoves(movesString: String?): IntArray {
        if (movesString == null) {
            return IntArray(0)
        }
        val movesStringArray = movesString.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        val moves = IntArray(movesStringArray.size)
        for (i in moves.indices) {
            moves[i] = Move.getFromString(search.board, movesStringArray[i], true)
        }
        return moves
    }

    private fun testPosition(fen: String, avoidMovesString: String?, bestMovesString: String?, timeLimit: Int): Int {
        bestMove = 0
        solutionFound = false

        search.clear()
        search.board.fen = fen
        avoidMoves = parseMoves(avoidMovesString)
        if (avoidMovesString != null) {
            logger.debug("Lets see if " + avoidMovesString + (if (avoidMoves.size > 1) " are " else " is ") + "avoided")
        }
        bestMoves = parseMoves(bestMovesString)
        if (bestMovesString != null) {
            logger.debug("Lets see if " + bestMovesString + (if (bestMoves.size > 1) " are " else " is ") + "found")
        }

        search.go(SearchParameters[timeLimit])

        if (solutionFound) {
            logger.debug("Solution found in " + solutionTime + "Ms and " + solutionNodes + " nodes :D " + Move.toStringExt(bestMove))
            totalNodes += solutionNodes
            allSolutionNodes.add(solutionNodes)
            allSolutionTimes.add(solutionTime)
            return solutionTime
        } else {
            logger.debug("Solution not found, instead played: " + Move.toStringExt(search.bestMove))
            allSolutionNodes.add(search.nodeCount)
            allSolutionTimes.add(timeLimit)
            return timeLimit
        }
    }

    override fun info(info: SearchStatusInfo) {
        if (bestMove != search.bestMove) {
            bestMove = search.bestMove
            solutionTime = info.time.toInt()
            solutionNodes = info.nodes
        }

        var found = bestMoves.isEmpty()
        for (move in bestMoves) {
            if (move == search.bestMove) {
                found = true
                break
            }
        }
        for (move in avoidMoves) {
            if (move == search.bestMove) {
                found = false
                break
            }
        }
        solutionFound = found

        if (found) {
            logger.debug(TestColors.ANSI_GREEN + info.toString() + TestColors.ANSI_RESET)
        } else {
            logger.debug(TestColors.ANSI_RED + info.toString() + TestColors.ANSI_RESET)
        }
    }

    override fun bestMove(bestMove: Int, ponder: Int) {

    }

    companion object {
        private val logger = Logger.getLogger("EpdTest")
    }
}