package karballo

import karballo.book.FileBook
import karballo.log.Logger
import karballo.search.SearchEngine
import karballo.search.SearchEngineThreaded
import karballo.uci.Uci
import karballo.util.JvmPlatformUtils
import karballo.util.Utils

fun searchEngineBuilder(config: Config): SearchEngine {
    return SearchEngineThreaded(config)
}

fun main(args: Array<String>) {
    Logger.noLog = true // Disable logging
    Utils.instance = JvmPlatformUtils()

    val config = Config()
    config.book = FileBook("/book_small.bin")

    val uci = Uci(config, ::searchEngineBuilder)

    while (true) {
        val line = readLine()
        line ?: return
        uci.processLine(line)
    }
}
