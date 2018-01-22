package karballo.search

interface SearchObserver {

    fun info(info: SearchStatusInfo)

    fun bestMove(bestMove: Int, ponder: Int)

}