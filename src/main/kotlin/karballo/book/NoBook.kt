package karballo.book

import karballo.Board
import karballo.Move

class NoBook : Book {

    override fun getMove(board: Board): Int {
        return Move.NONE
    }
}