package karballo.bitboard

import karballo.Board
import karballo.Square

/**
 * Holds all the possible attacks for a board.
 * It is used by the evaluators, the move generators and the move iterator,
 * and also speeds the SEE calculations detecting not attacked squares.
 * Calculates the checking pieces and the interpose squares to avoid checks.
 */
class AttacksInfo {

    internal var bbAttacks: BitboardAttacks = BitboardAttacks.getInstance()

    var boardKey: Long = 0
    // Includes attacks by pinned pieces that cannot move to the square, but limit king mobility
    var attackedSquaresAlsoPinned = longArrayOf(0, 0)
    // The other attacks do not include those from pinned pieces
    var attackedSquares = longArrayOf(0, 0)
    var attacksFromSquare = LongArray(64)
    var pawnAttacks = longArrayOf(0, 0)
    var knightAttacks = longArrayOf(0, 0)
    var bishopAttacks = longArrayOf(0, 0)
    var rookAttacks = longArrayOf(0, 0)
    var queenAttacks = longArrayOf(0, 0)
    var kingAttacks = longArrayOf(0, 0)
    var kingIndex = intArrayOf(0, 0)
    var pinnedMobility = LongArray(64)
    //
    // Squares with possible ray attacks to the kings: used to detect check and move legality
    //
    var bishopAttacksKing = longArrayOf(0, 0)
    var rookAttacksKing = longArrayOf(0, 0)

    var mayPin = longArrayOf(0, 0) // both my pieces than can discover an attack and the opponent pieces pinned, that is any piece attacked by a slider
    var piecesGivingCheck: Long = 0
    var interposeCheckSquares: Long = 0
    var pinnedPieces: Long = 0

    /**
     * Checks for a pinned piece in each ray
     */
    private fun checkPinnerRay(ray: Long, mines: Long, attackerSlider: Long) {
        val pinner = ray and attackerSlider
        if (pinner != 0L) {
            val pinned = ray and mines
            pinnedPieces = pinnedPieces or pinned
            pinnedMobility[BitboardUtils.square2Index(pinned)] = ray
        }
    }

    private fun checkPinnerBishop(kingIndex: Int, bishopSliderAttacks: Long, all: Long, mines: Long, otherBishopsOrQueens: Long) {
        if (bishopSliderAttacks and mines == 0L || bbAttacks.bishop[kingIndex] and otherBishopsOrQueens == 0L) {
            return
        }
        val xray = bbAttacks.getBishopAttacks(kingIndex, all and (mines and bishopSliderAttacks).inv())
        if (xray and bishopSliderAttacks.inv() and otherBishopsOrQueens != 0L) {
            val rank = kingIndex shr 3
            val file = 7 - kingIndex and 7

            checkPinnerRay(xray and BitboardUtils.RANKS_UPWARDS[rank] and BitboardUtils.FILES_LEFT[file], mines, otherBishopsOrQueens)
            checkPinnerRay(xray and BitboardUtils.RANKS_UPWARDS[rank] and BitboardUtils.FILES_RIGHT[file], mines, otherBishopsOrQueens)
            checkPinnerRay(xray and BitboardUtils.RANKS_DOWNWARDS[rank] and BitboardUtils.FILES_LEFT[file], mines, otherBishopsOrQueens)
            checkPinnerRay(xray and BitboardUtils.RANKS_DOWNWARDS[rank] and BitboardUtils.FILES_RIGHT[file], mines, otherBishopsOrQueens)
        }
    }

    private fun checkPinnerRook(kingIndex: Int, rookSliderAttacks: Long, all: Long, mines: Long, otherRooksOrQueens: Long) {
        if (rookSliderAttacks and mines == 0L || bbAttacks.rook[kingIndex] and otherRooksOrQueens == 0L) {
            return
        }
        val xray = bbAttacks.getRookAttacks(kingIndex, all and (mines and rookSliderAttacks).inv())
        if (xray and rookSliderAttacks.inv() and otherRooksOrQueens != 0L) {
            val rank = kingIndex shr 3
            val file = 7 - kingIndex and 7

            checkPinnerRay(xray and BitboardUtils.RANKS_UPWARDS[rank], mines, otherRooksOrQueens)
            checkPinnerRay(xray and BitboardUtils.FILES_LEFT[file], mines, otherRooksOrQueens)
            checkPinnerRay(xray and BitboardUtils.RANKS_DOWNWARDS[rank], mines, otherRooksOrQueens)
            checkPinnerRay(xray and BitboardUtils.FILES_RIGHT[file], mines, otherRooksOrQueens)
        }
    }

    /**
     * If we already hold the attacks for this board, do nothing
     */
    fun build(board: Board) {
        if (boardKey == board.getKey()) {
            return
        }
        boardKey = board.getKey()
        val all = board.all
        val mines = board.mines
        val myKing = board.kings and mines
        val us = if (board.turn) 0 else 1

        attackedSquaresAlsoPinned[W] = 0
        attackedSquaresAlsoPinned[B] = 0
        pawnAttacks[W] = 0
        pawnAttacks[B] = 0
        knightAttacks[W] = 0
        knightAttacks[B] = 0
        bishopAttacks[W] = 0
        bishopAttacks[B] = 0
        rookAttacks[W] = 0
        rookAttacks[B] = 0
        queenAttacks[W] = 0
        queenAttacks[B] = 0
        kingAttacks[W] = 0
        kingAttacks[B] = 0
        mayPin[W] = 0
        mayPin[B] = 0
        pinnedPieces = 0
        piecesGivingCheck = 0
        interposeCheckSquares = 0

        kingIndex[W] = BitboardUtils.square2Index(board.kings and board.whites)
        kingIndex[B] = BitboardUtils.square2Index(board.kings and board.blacks)

        bishopAttacksKing[W] = bbAttacks.getBishopAttacks(kingIndex[W], all)
        checkPinnerBishop(kingIndex[W], bishopAttacksKing[W], all, board.whites, board.bishops or board.queens and board.blacks)
        bishopAttacksKing[B] = bbAttacks.getBishopAttacks(kingIndex[B], all)
        checkPinnerBishop(kingIndex[B], bishopAttacksKing[B], all, board.blacks, board.bishops or board.queens and board.whites)

        rookAttacksKing[W] = bbAttacks.getRookAttacks(kingIndex[W], all)
        checkPinnerRook(kingIndex[W], rookAttacksKing[W], all, board.whites, board.rooks or board.queens and board.blacks)
        rookAttacksKing[B] = bbAttacks.getRookAttacks(kingIndex[B], all)
        checkPinnerRook(kingIndex[B], rookAttacksKing[B], all, board.blacks, board.rooks or board.queens and board.whites)

        var pieceAttacks: Long
        var index = 0
        var square: Long = 1
        while (index < 64) {
            if (square and all != 0L) {
                val color = if (board.whites and square != 0L) W else B
                val pinnedSquares = if (square and pinnedPieces != 0L) pinnedMobility[index] else Square.ALL

                pieceAttacks = 0
                if (square and board.pawns != 0L) {
                    pieceAttacks = bbAttacks.pawn[color][index]
                    if (square and mines == 0L && pieceAttacks and myKing != 0L) {
                        piecesGivingCheck = piecesGivingCheck or square
                    }
                    pawnAttacks[color] = pawnAttacks[color] or (pieceAttacks and pinnedSquares)

                } else if (square and board.knights != 0L) {
                    pieceAttacks = bbAttacks.knight[index]
                    if (square and mines == 0L && pieceAttacks and myKing != 0L) {
                        piecesGivingCheck = piecesGivingCheck or square
                    }
                    knightAttacks[color] = knightAttacks[color] or (pieceAttacks and pinnedSquares)

                } else if (square and board.bishops != 0L) {
                    pieceAttacks = bbAttacks.getBishopAttacks(index, all)
                    if (square and mines == 0L && pieceAttacks and myKing != 0L) {
                        piecesGivingCheck = piecesGivingCheck or square
                        interposeCheckSquares = interposeCheckSquares or (pieceAttacks and bishopAttacksKing[us]) // And with only the diagonal attacks to the king
                    }
                    bishopAttacks[color] = bishopAttacks[color] or (pieceAttacks and pinnedSquares)
                    mayPin[color] = mayPin[color] or (all and pieceAttacks)

                } else if (square and board.rooks != 0L) {
                    pieceAttacks = bbAttacks.getRookAttacks(index, all)
                    if (square and mines == 0L && pieceAttacks and myKing != 0L) {
                        piecesGivingCheck = piecesGivingCheck or square
                        interposeCheckSquares = interposeCheckSquares or (pieceAttacks and rookAttacksKing[us]) // And with only the rook attacks to the king
                    }
                    rookAttacks[color] = rookAttacks[color] or (pieceAttacks and pinnedSquares)
                    mayPin[color] = mayPin[color] or (all and pieceAttacks)

                } else if (square and board.queens != 0L) {
                    val bishopSliderAttacks = bbAttacks.getBishopAttacks(index, all)
                    if (square and mines == 0L && bishopSliderAttacks and myKing != 0L) {
                        piecesGivingCheck = piecesGivingCheck or square
                        interposeCheckSquares = interposeCheckSquares or (bishopSliderAttacks and bishopAttacksKing[us]) // And with only the diagonal attacks to the king
                    }
                    val rookSliderAttacks = bbAttacks.getRookAttacks(index, all)
                    if (square and mines == 0L && rookSliderAttacks and myKing != 0L) {
                        piecesGivingCheck = piecesGivingCheck or square
                        interposeCheckSquares = interposeCheckSquares or (rookSliderAttacks and rookAttacksKing[us]) // And with only the rook attacks to the king
                    }
                    pieceAttacks = rookSliderAttacks or bishopSliderAttacks
                    queenAttacks[color] = queenAttacks[color] or (pieceAttacks and pinnedSquares)
                    mayPin[color] = mayPin[color] or (all and pieceAttacks)

                } else if (square and board.kings != 0L) {
                    pieceAttacks = bbAttacks.king[index]
                    kingAttacks[color] = kingAttacks[color] or pieceAttacks
                }

                attackedSquaresAlsoPinned[color] = attackedSquaresAlsoPinned[color] or pieceAttacks
                attacksFromSquare[index] = pieceAttacks and pinnedSquares
            } else {
                attacksFromSquare[index] = 0
            }
            square = square shl 1
            index++
        }
        attackedSquares[W] = pawnAttacks[W] or knightAttacks[W] or bishopAttacks[W] or rookAttacks[W] or queenAttacks[W] or kingAttacks[W]
        attackedSquares[B] = pawnAttacks[B] or knightAttacks[B] or bishopAttacks[B] or rookAttacks[B] or queenAttacks[B] or kingAttacks[B]
    }

    companion object {
        val W = 0
        val B = 1
    }
}