package karballo.movegen

import karballo.Board

interface MoveGenerator {

    fun generateMoves(board: Board, moves: IntArray, startIndex: Int): Int

}