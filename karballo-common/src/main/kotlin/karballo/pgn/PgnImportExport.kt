package karballo.pgn

import karballo.Board
import karballo.Move
import karballo.util.Utils

object PgnImportExport {

    /**
     * Parses a PGN and does all the moves in a board
     */
    fun setBoard(b: Board, pgnString: String) {
        val game = PgnParser.parsePgn(pgnString)
        if (game!!.fenStartPosition != null) {
            b.fen = game.fenStartPosition!!
        } else {
            b.startPosition()
        }

        for (gameNode in game.pv!!.variation) {
            if (gameNode is GameNodeMove) {
                val move = Move.getFromString(b, gameNode.move, true)
                b.doMove(move)
            }
        }
    }

    fun getPgn(b: Board, whiteName: String?, blackName: String?, event: String? = null, site: String? = null, result: String? = null): String {
        var whiteNameVar = whiteName
        var blackNameVar = blackName
        var eventVar = event
        var siteVar = site
        var resultVar = result

        val sb = StringBuilder()

        if (whiteNameVar == null || "" == whiteNameVar) {
            whiteNameVar = "?"
        }
        if (blackNameVar == null || "" == blackNameVar) {
            blackNameVar = "?"
        }

        if (eventVar == null) {
            eventVar = "Chess Game"
        }
        if (siteVar == null) {
            siteVar = "-"
        }

        sb.append("[Event \"").append(eventVar).append("\"]\n")
        sb.append("[Site \"").append(siteVar).append("\"]\n")

        sb.append("[Date \"").append(Utils.instance.getCurrentDateIso().replace('-', '.')).append("\"]\n")
        sb.append("[Round \"?\"]\n")
        sb.append("[White \"").append(whiteNameVar).append("\"]\n")
        sb.append("[Black \"").append(blackNameVar).append("\"]\n")
        if (resultVar == null) {
            resultVar = "*"
            when (b.isEndGame) {
                1 -> resultVar = "1-0"
                -1 -> resultVar = "0-1"
                99 -> resultVar = "1/2-1/2"
            }
        }
        sb.append("[Result \"").append(resultVar).append("\"]\n")
        if (Board.FEN_START_POSITION != b.initialFen) {
            sb.append("[FEN \"").append(b.initialFen).append("\"]\n")
        }
        sb.append("[PlyCount \"").append(b.moveNumber - b.initialMoveNumber).append("\"]\n")
        sb.append("\n")

        val line = StringBuilder()

        for (i in b.initialMoveNumber..b.moveNumber - 1) {
            line.append(" ")
            if (i and 1 == 0) {
                line.append(i.ushr(1) + 1)
                line.append(". ")
            }
            line.append(b.getSanMove(i))
        }

        line.append(" ")
        line.append(resultVar)
        // Cut line in a limit of 80 characters
        val tokens = line.toString().split("[ \\t\\n\\x0B\\f\\r]+".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

        var length = 0
        for (token in tokens) {
            if (length + token.length + 1 > 80) {
                sb.append("\n")
                length = 0
            } else if (length > 0) {
                sb.append(" ")
                length++
            }
            length += token.length
            sb.append(token)
        }

        return sb.toString()
    }
}