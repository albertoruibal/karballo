package karballo.search

import karballo.evaluation.Evaluator

/**
 * * info
 * the engine wants to send infos to the GUI. This should be done whenever one of the info has changed.
 * The engine can send only selected infos and multiple infos can be send with one info command,
 * e.g. "info currmove e2e4 currmovenumber 1" or
 * "info depth 12 nodes 123456 nps 100000".
 * Also all infos belonging to the pv should be sent together
 * e.g. "info depth 2 score cp 214 time 1242 nodes 2124 nps 34928 pv e2e4 e7e5 g1f3"
 * I suggest to start sending "currmove", "currmovenumber", "currline" and "refutation" only after one second
 * to avoid too much traffic.
 * Additional info:
 * * depth
 * search depth in plies
 * * seldepth
 * selective search depth in plies,
 * if the engine sends seldepth there must also a "depth" be present in the same string.
 * * time
 * the time searched in ms, this should be sent together with the pv.
 * * nodes
 * x nodes searched, the engine should send this info regularly
 * * pv  ...
 * the best line found
 * * multipv
 * this for the multi pv mode.
 * for the best move/pv add "multipv 1" in the string when you send the pv.
 * in k-best mode always send all k variants in k strings together.
 * * score
 * * cp
 * the score from the engine's point of view in centipawns.
 * * mate
 * mate in y moves, not plies.
 * If the engine is getting mated use negativ values for y.
 * * lowerbound
 * the score is just a lower bound.
 * * upperbound
 * the score is just an upper bound.
 * * currmove
 * currently searching this move
 * * currmovenumber
 * currently searching move number x, for the first move x should be 1 not 0.
 * * hashfull
 * the hash is x permill full, the engine should send this info regularly
 * * nps
 * x nodes per second searched, the engine should send this info regularly
 * * tbhits
 * x positions where found in the endgame table bases
 * * cpuload
 * the cpu usage of the engine is x permill.
 * * string
 * any string str which will be displayed be the engine,
 * if there is a string command the rest of the line will be interpreted as .
 * * refutation   ...
 * move  is refuted by the line  ... , i can be any number >= 1.
 * Example: after move d1h5 is searched, the engine can send
 * "info refutation d1h5 g6h5"
 * if g6h5 is the best answer after d1h5 or if g6h5 refutes the move d1h5.
 * if there is norefutation for d1h5 found, the engine should just send
 * "info refutation d1h5"
 * The engine should only send this if the option "UCI_ShowRefutations" is set to true.
 * * currline   ...
 * this is the current line the engine is calculating.  is the number of the cpu if
 * the engine is running on more than one cpu.  = 1,2,3....
 * if the engine is just using one cpu,  can be omitted.
 * If  is greater than 1, always send all k lines in k strings together.
 * The engine should only send this if the option "UCI_ShowCurrLine" is set to true.
 */

class SearchStatusInfo {

    var depth: Int = 0
    var selDepth: Int = 0
    var time = Long.MIN_VALUE
    var nodes: Long = 0
    var pv: String? = null
    var multiPv: Int = 0
    var score: Int = 0
    internal var lowerBound: Boolean = false
    internal var upperBound: Boolean = false
    var currMove: String? = null
    var currMoveNumber: Int = 0
    var hashFull: Int = 0
    var nps: Long = 0
    var tbHits: Int = 0
    var cpuLoad: Int = 0
    var string: String? = null
    var refutation: String? = null
    var currLine: String? = null

    fun setScore(score: Int, alpha: Int, beta: Int) {
        this.score = score
        upperBound = score <= alpha
        lowerBound = score >= beta
    }

    val isMate: Boolean
        get() = score < -SearchEngine.VALUE_IS_MATE || score > SearchEngine.VALUE_IS_MATE

    val mateIn: Int
        get() {
            val x = (if (score < 0) -Evaluator.MATE else Evaluator.MATE) - score
            if (x and 1 != 0) {
                return (x shr 1) + 1
            } else {
                return x shr 1
            }
        }

    /**
     * in UCI format
     * TODO complete
     */
    override fun toString(): String {
        val sb = StringBuilder()
        if (depth != 0) {
            sb.append("depth ")
            sb.append(depth)
        }
        if (selDepth != 0) {
            sb.append(" seldepth ")
            sb.append(selDepth)
        }
        if (isMate) {
            sb.append(" score mate ")
            sb.append(mateIn)
        } else {
            sb.append(" score cp ")
            sb.append(score)
        }
        if (lowerBound) {
            sb.append(" lowerbound")
        } else if (upperBound) {
            sb.append(" upperbound")
        }
        if (nodes != 0L) {
            sb.append(" nodes ")
            sb.append(nodes)
        }
        if (time != Long.MIN_VALUE) {
            sb.append(" time ")
            sb.append(time)
        }
        if (hashFull != 0) {
            sb.append(" hashfull ")
            sb.append(hashFull)
        }
        if (nps != 0L) {
            sb.append(" nps ")
            sb.append(nps)
        }
        if (pv != null) {
            sb.append(" pv ")
            sb.append(pv)
        }
        return sb.toString()
    }
}
