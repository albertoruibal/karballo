package karballo.pgn

import karballo.Board
import karballo.Move
import java.util.*

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
        var whiteName = whiteName
        var blackName = blackName
        var event = event
        var site = site
        var result = result

        val sb = StringBuilder()

        if (whiteName == null || "" == whiteName) {
            whiteName = "?"
        }
        if (blackName == null || "" == blackName) {
            blackName = "?"
        }

        if (event == null) {
            event = "Chess Game"
        }
        if (site == null) {
            site = "-"
        }

        sb.append("[Event \"").append(event).append("\"]\n")
        sb.append("[Site \"").append(site).append("\"]\n")

        val d = Date()
        // For GWT we use deprecated methods
        sb.append("[Date \"").append(d.year + 1900).append(".").append(d.month + 1).append(".").append(d.date).append("\"]\n")
        sb.append("[Round \"?\"]\n")
        sb.append("[White \"").append(whiteName).append("\"]\n")
        sb.append("[Black \"").append(blackName).append("\"]\n")
        if (result == null) {
            result = "*"
            when (b.isEndGame) {
                1 -> result = "1-0"
                -1 -> result = "0-1"
                99 -> result = "1/2-1/2"
            }
        }
        sb.append("[Result \"").append(result).append("\"]\n")
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
        line.append(result)
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