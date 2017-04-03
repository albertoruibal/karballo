package karballo.uci

import karballo.Config
import karballo.Move
import karballo.book.FileBook
import karballo.log.Logger
import karballo.search.SearchEngineThreaded
import karballo.search.SearchObserver
import karballo.search.SearchParameters
import karballo.search.SearchStatusInfo

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * UCI Interface
 */
class Uci : SearchObserver {
    internal var config: Config
    lateinit internal var engine: SearchEngineThreaded
    lateinit internal var searchParameters: SearchParameters

    internal var needsReload = true

    init {
        Logger.noLog = true // Disable logging
        config = Config()
        config.book = FileBook("/book_small.bin")
    }

    internal fun loop() {
        println(NAME + " by " + AUTHOR)
        val reader = BufferedReader(InputStreamReader(System.`in`))
        try {
            while (true) {
                val `in` = reader.readLine()
                val tokens = `in`.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                var index = 0
                val command = tokens[index++].toLowerCase()

                if ("uci" == command) {
                    println("id name " + NAME)
                    println("id author " + AUTHOR)
                    println("option name Hash type spin default " + Config.DEFAULT_TRANSPOSITION_TABLE_SIZE + " min 16 max 1024")
                    println("option name Ponder type check default " + Config.DEFAULT_PONDER)
                    println("option name OwnBook type check default " + Config.DEFAULT_USE_BOOK)
                    println("option name UCI_Chess960 type check default " + Config.DEFAULT_UCI_CHESS960)
                    println("option name UCI_LimitStrength type check default " + Config.DEFAULT_LIMIT_STRENGTH)
                    println("option name UCI_Elo type spin default " + Config.DEFAULT_ELO + " min 500 max " + Config.DEFAULT_ELO)
                    println("option name Evaluator type combo default " + Config.DEFAULT_EVALUATOR + " var simplified var complete var experimental")
                    println("option name Contempt Factor type spin default " + Config.DEFAULT_CONTEMPT_FACTOR + " min -200 max 200")
                    println("uciok")

                } else if ("setoption" == command) {
                    index++ // Skip name
                    // get the option name without spaces
                    val nameSB = StringBuilder()
                    var tok: String
                    while (true) {
                        tok = tokens[index++]
                        if ("value" == tok) {
                            break
                        }
                        nameSB.append(tok)
                    }
                    val name = nameSB.toString()
                    val value = tokens[index]

                    if ("Hash" == name) {
                        config.transpositionTableSize = value.toInt()
                    } else if ("Ponder" == name) {
                        config.ponder = value.toBoolean()
                    } else if ("OwnBook" == name) {
                        config.useBook = value.toBoolean()
                    } else if ("UCI_Chess960" == name) {
                        config.isUciChess960 = value.toBoolean()
                    } else if ("UCI_LimitStrength" == name) {
                        config.isLimitStrength = value.toBoolean()
                    } else if ("UCI_Elo" == name) {
                        config.elo = value.toInt()
                    } else if ("Evaluator" == name) {
                        config.evaluator = value
                    } else if ("ContemptFactor" == name) {
                        config.contemptFactor = value.toInt()
                    }
                    needsReload = true

                } else if ("isready" == command) {
                    if (needsReload) {
                        engine = SearchEngineThreaded(config)
                        engine.setObserver(this)
                        needsReload = false
                        System.gc()
                    } else {
                        // Wait for the engine to finish searching
                        while (engine.isSearching) {
                            try {
                                Thread.sleep(10)
                            } catch (e: Exception) {
                            }

                        }
                    }
                    println("readyok")

                } else if ("quit" == command) {
                    System.exit(0)

                } else if ("go" == command) {
                    if (engine == null) {
                        println("info string The engine is not initialized: the isready command must be sent before any search")
                        continue
                    }

                    searchParameters = SearchParameters()
                    while (index < tokens.size) {
                        val arg1 = tokens[index++]
                        if ("searchmoves" == arg1) {
                            // While valid moves are found, add to the searchMoves
                            while (index < tokens.size) {
                                val move = Move.getFromString(engine.board, tokens[index++], true)
                                if (move != Move.NONE) {
                                    searchParameters.addSearchMove(move)
                                } else {
                                    index--
                                    break
                                }
                            }
                        } else if ("ponder" == arg1) {
                            searchParameters.isPonder = true
                        } else if ("wtime" == arg1) {
                            searchParameters.wtime = tokens[index++].toInt()
                        } else if ("btime" == arg1) {
                            searchParameters.btime = tokens[index++].toInt()
                        } else if ("winc" == arg1) {
                            searchParameters.winc = tokens[index++].toInt()
                        } else if ("binc" == arg1) {
                            searchParameters.binc = tokens[index++].toInt()
                        } else if ("movestogo" == arg1) {
                            searchParameters.movesToGo = tokens[index++].toInt()
                        } else if ("depth" == arg1) {
                            searchParameters.depth = tokens[index++].toInt()
                        } else if ("nodes" == arg1) {
                            searchParameters.nodes = tokens[index++].toInt()
                        } else if ("mate" == arg1) {
                            searchParameters.mate = tokens[index++].toInt()
                        } else if ("movetime" == arg1) {
                            searchParameters.moveTime = tokens[index++].toInt()
                        } else if ("infinite" == arg1) {
                            searchParameters.isInfinite = true
                        }
                    }
                    engine.go(searchParameters)

                } else if ("stop" == command) {
                    engine.stop()

                } else if ("ucinewgame" == command) {
                    engine.board.startPosition()
                    engine.clear()

                } else if ("position" == command) {
                    if (index < tokens.size) {
                        val arg1 = tokens[index++]
                        if ("startpos" == arg1) {
                            engine.board.startPosition()
                        } else if ("fen" == arg1) {
                            // FEN string may have spaces
                            val fenSb = StringBuilder()
                            while (index < tokens.size) {
                                if ("moves" == tokens[index]) {
                                    break
                                }
                                fenSb.append(tokens[index++])
                                if (index < tokens.size) {
                                    fenSb.append(" ")
                                }
                            }
                            engine.board.fen = fenSb.toString()
                        }

                    }
                    if (index < tokens.size) {
                        val arg1 = tokens[index++]
                        if ("moves" == arg1) {
                            while (index < tokens.size) {
                                val move = Move.getFromString(engine.board, tokens[index++], true)
                                engine.board.doMove(move)
                            }
                        }
                    }

                } else if ("debug" == command) {
                } else if ("ponderhit" == command) {
                    if (searchParameters != null) {
                        searchParameters.isPonder = false
                        engine.updateSearchParameters(searchParameters)
                    }

                } else if ("register" == command) {
                    // not used
                } else {
                    println("info string Wrong UCI command")
                }
            }

        } catch (e: IOException) {
            println("info string Wrong UCI syntax")
        }

    }

    override fun bestMove(bestMove: Int, ponder: Int) {
        val sb = StringBuilder()
        sb.append("bestmove ")
        sb.append(Move.toString(bestMove))
        if (config.ponder && ponder != Move.NONE) {
            sb.append(" ponder ")
            sb.append(Move.toString(ponder))
        }
        println(sb.toString())
    }

    override fun info(info: SearchStatusInfo) {
        print("info ")
        println(info.toString())
    }

    companion object {
        internal val NAME = "Carballo Chess Engine v1.8"
        internal val AUTHOR = "Alberto Alonso Ruibal"

        @JvmStatic fun main(args: Array<String>) {
            val uci = Uci()
            uci.loop()
        }
    }
}