package karballo.search

import karballo.Move
import karballo.bitboard.AttacksInfo
import karballo.evaluation.Evaluator

/**
 * Stores the elements to be kept in each node of the search tree
 *
 * Other nodes may access this elements
 */
class Node(searchEngine: SearchEngine, var distanceToInitialPly: Int) {

    // Current move
    var move: Int = 0
    // Transposition table move
    var ttMove: Int = 0

    // Two killer move slots
    var killerMove1: Int = 0
    var killerMove2: Int = 0

    // The static node eval
    var staticEval: Int = 0

    // The Move iterator
    var attacksInfo: AttacksInfo = AttacksInfo()
    var moveIterator: MoveIterator

    init {
        moveIterator = MoveIterator(searchEngine, attacksInfo, distanceToInitialPly)

        clear()
    }

    fun clear() {
        ttMove = Move.NONE

        killerMove1 = Move.NONE
        killerMove2 = Move.NONE

        staticEval = Evaluator.NO_VALUE
    }

    fun destroy() {
        moveIterator.destroy()
    }
}