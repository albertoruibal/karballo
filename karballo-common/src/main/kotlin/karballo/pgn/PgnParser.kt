package karballo.pgn

/**
 * Pgn parser wit variations support

 * @author rui
 */

object PgnParser {
    /**
     * Parses a 1-game pgn
     */
    fun parsePgn(pgn: String?, parseBody: Boolean = true): Game? {
        if (pgn == null) {
            return null
        }
        val game = Game()
        val variations = ArrayList<GameNodeVariation>()
        val principalVariation = GameNodeVariation()
        var currentVariation = principalVariation
        variations.add(principalVariation)

        var lastMoveNumber: String? = null
        var lastMove: GameNodeMove? = null
        var sb = StringBuilder()
        var parsingHeaders = true
        var parsingComment = false

        try {
            val lines = pgn.split("\\r?\\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

            for (line in lines) {
                if ("" != line.trim({ it <= ' ' })) {
                    if (parsingHeaders && line.indexOf("[") == 0) {
                        // It is a header
                        val headerName = line.substring(1, line.indexOf("\"")).trim({ it <= ' ' }).toLowerCase()
                        val headerValue = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""))

                        if ("" != headerValue && "?" != headerValue && "-" != headerValue) {
                            if ("event" == headerName) {
                                game.setEvent(headerValue)
                            } else if ("round" == headerName) {
                                game.setRound(headerValue)
                            } else if ("site" == headerName) {
                                game.setSite(headerValue)
                            } else if ("eventdate" == headerName) {
                                game.setEventDate(headerValue)
                            } else if ("date" == headerName) {
                                game.setDate(headerValue)
                            } else if ("white" == headerName) {
                                game.white = headerValue
                            } else if ("black" == headerName) {
                                game.black = headerValue
                            } else if ("whiteelo" == headerName) {
                                game.whiteElo = headerValue.toInt()
                            } else if ("blackelo" == headerName) {
                                game.blackElo = headerValue.toInt()
                            } else if ("whitefideid" == headerName) {
                                game.whiteFideId = headerValue.toInt()
                            } else if ("blackfideid" == headerName) {
                                game.blackFideId = headerValue.toInt()
                            } else if ("result" == headerName) {
                                game.setResult(headerValue)
                            } else if ("fen" == headerName) {
                                game.fenStartPosition = headerValue
                            }
                        }
                    } else {
                        parsingHeaders = false
                        if (!parseBody) {
                            break
                        }

                        for (i in 0..line.length - 1) {
                            val c = line[i]

                            if (parsingComment) {
                                if (c == '}') {
                                    currentVariation.variation.add(GameNodeComment(sb.toString()))
                                    sb = StringBuilder() //sb.setLength(0)
                                    parsingComment = false
                                } else {
                                    sb.append(c)
                                }
                            } else {
                                var finishLastEntity = false

                                if (c == '{') {
                                    parsingComment = true
                                    finishLastEntity = true
                                } else if (c == ' ') {
                                    if (sb.isNotEmpty()) {
                                        finishLastEntity = true
                                    }
                                } else if (c == '(' || c == ')') {
                                    finishLastEntity = true
                                } else {
                                    sb.append(c)
                                    if (i == line.length - 1) {
                                        finishLastEntity = true
                                    }
                                }

                                if (finishLastEntity && sb.isNotEmpty()) {
                                    var s = sb.toString()
                                    sb = StringBuilder() //sb.setLength(0)

                                    if ("1-0" == s
                                            || "0-1" == s
                                            || "0-1" == s
                                            || "1/2-1/2" == s
                                            || "½-½" == s
                                            || "*" == s) {
                                        currentVariation.variation.add(GameNodeResult(s))

                                    } else if (s.length > 1 && isAlphaNumeric(s[0])) {

                                        // Strip the move number (with one or more dots) from the beginning of the string
                                        if (s[0] in '1'..'9') {
                                            val lastDotIndex = s.lastIndexOf(".")
                                            lastMoveNumber = s.substring(0, lastDotIndex + 1)
                                            s = s.substring(lastDotIndex + 1)
                                        }

                                        // Search annotations at the end of the move
                                        var annotation: String? = null
                                        var lastIndex: Int = s.length - 1
                                        while (lastIndex >= 0) {
                                            if (isAlphaNumeric(s[lastIndex])) {
                                                break
                                            }
                                            lastIndex--
                                        }
                                        if (lastIndex < s.length - 1) {
                                            annotation = s.substring(lastIndex + 1)
                                            s = s.substring(0, lastIndex + 1)
                                        }

                                        if (s.isNotEmpty()) {
                                            lastMove = GameNodeMove(lastMoveNumber, s, annotation)
                                            currentVariation.variation.add(lastMove)
                                            lastMoveNumber = null
                                        }

                                    } else {
                                        // glyph
                                        if (lastMove != null) {
                                            if (lastMove.annotation == null) {
                                                lastMove.annotation = NumericAnnotationGlyphs.translate(s)
                                            } else {
                                                lastMove.annotation += " " + NumericAnnotationGlyphs.translate(s)
                                            }
                                        }
                                    }
                                }

                                // Variations management
                                if (c == '(') {
                                    currentVariation = GameNodeVariation()
                                    variations.add(currentVariation)
                                } else if (c == ')') {
                                    val lastVariation = variations[variations.size - 1]
                                    variations.removeAt(variations.size - 1)
                                    currentVariation = variations[variations.size - 1]
                                    currentVariation.variation.add(lastVariation)
                                }
                            }
                        }
                        if (parsingComment) {
                            sb.append(" ")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("ERROR parsing pgn: " + pgn)
        }

        game.pv = principalVariation
        return game
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return c in 'A'..'Z'
                || c in 'a'..'z'
                || c in '1'..'9'
    }
}