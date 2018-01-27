package karballo

import karballo.book.NoBook
import karballo.log.Logger
import karballo.search.SearchEngine
import karballo.search.SearchEngineThreaded
import karballo.uci.Uci
import karballo.util.JsPlatformUtils
import karballo.util.Utils

external fun require(module:String):dynamic

fun searchEngineBuilder(config: Config): SearchEngine {
    return SearchEngineThreaded(config)
}

fun main(args: Array<String>) {
    val readline = require("readline")
    Logger.noLog = true // Disable logging
    Utils.instance = JsPlatformUtils()

    val config = Config()
    config.book = NoBook()
    val uci = Uci(config, ::searchEngineBuilder)

    val rl = readline.createInterface(js("{ input: process.stdin, output: process.stdout }"))
    rl.on("line", uci::processLine)
}