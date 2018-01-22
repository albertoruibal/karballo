package karballo.movegen

import karballo.Board
import karballo.Color
import karballo.Move
import karballo.Piece
import karballo.bitboard.AttacksInfo
import karballo.bitboard.BitboardUtils

/**
 * Magic move generator
 * Generate pseudo-legal moves because can leave the king in check.
 * It does not set the check flag.

 * @author Alberto Alonso Ruibal
 */
open class MagicMoveGenerator : MoveGenerator {

    private lateinit var moves: IntArray
    private var moveIndex: Int = 0
    private var all: Long = 0
    private var mines: Long = 0
    private var others: Long = 0

    internal var ai = AttacksInfo()

    override fun generateMoves(board: Board, moves: IntArray, startIndex: Int): Int {
        this.moves = moves
        ai.build(board)

        moveIndex = startIndex
        all = board.all
        mines = board.mines
        others = board.others

        var index = 0
        var square = 0x1L
        while (square != 0L) {
            if (board.turn == (square and board.whites != 0L)) {
                if (square and board.rooks != 0L) { // Rook
                    generateMovesFromAttacks(Piece.ROOK, index, ai.attacksFromSquare[index] and mines.inv())
                } else if (square and board.bishops != 0L) { // Bishop
                    generateMovesFromAttacks(Piece.BISHOP, index, ai.attacksFromSquare[index] and mines.inv())
                } else if (square and board.queens != 0L) { // Queen
                    generateMovesFromAttacks(Piece.QUEEN, index, ai.attacksFromSquare[index] and mines.inv())
                } else if (square and board.kings != 0L) { // King
                    generateMovesFromAttacks(Piece.KING, index, ai.attacksFromSquare[index] and mines.inv())
                } else if (square and board.knights != 0L) { // Knight
                    generateMovesFromAttacks(Piece.KNIGHT, index, ai.attacksFromSquare[index] and mines.inv())
                } else if (square and board.pawns != 0L) { // Pawns
                    if (square and board.whites != 0L) {
                        if (square shl 8 and all == 0L) {
                            addMoves(Piece.PAWN, index, index + 8, false, 0)
                            // Two squares if it is in he first row
                            if (square and BitboardUtils.b2_d != 0L && square shl 16 and all == 0L) {
                                addMoves(Piece.PAWN, index, index + 16, false, 0)
                            }
                        }
                        generatePawnCapturesFromAttacks(index, ai.attacksFromSquare[index], board.passantSquare)
                    } else {
                        if (square.ushr(8) and all == 0L) {
                            addMoves(Piece.PAWN, index, index - 8, false, 0)
                            // Two squares if it is in he first row
                            if (square and BitboardUtils.b2_u != 0L && square.ushr(16) and all == 0L) {
                                addMoves(Piece.PAWN, index, index - 16, false, 0)
                            }
                        }
                        generatePawnCapturesFromAttacks(index, ai.attacksFromSquare[index], board.passantSquare)
                    }
                }
            }
            square = square shl 1
            index++
        }

        // Castling: disabled when in check or king route attacked
        if (!board.check) {
            val us = if (board.turn) Color.W else Color.B

            val kingCastlingDestination = board.canCastleKingSide(us, ai)
            if (kingCastlingDestination != 0L) {
                addMoves(Piece.KING, ai.kingIndex[us], BitboardUtils.square2Index(kingCastlingDestination), false, Move.TYPE_KINGSIDE_CASTLING)
            }
            val queenCastlingDestination = board.canCastleQueenSide(us, ai)
            if (queenCastlingDestination != 0L) {
                addMoves(Piece.KING, ai.kingIndex[us], BitboardUtils.square2Index(queenCastlingDestination), false, Move.TYPE_QUEENSIDE_CASTLING)
            }
        }

        return moveIndex
    }

    /**
     * Generates moves from an attack mask
     */
    private fun generateMovesFromAttacks(pieceMoved: Int, fromIndex: Int, attacks: Long) {
        var a = attacks
        while (a != 0L) {
            val to = BitboardUtils.lsb(a)
            addMoves(pieceMoved, fromIndex, BitboardUtils.square2Index(to), to and others != 0L, 0)
            a = a xor to
        }
    }

    private fun generatePawnCapturesFromAttacks(fromIndex: Int, attacks: Long, passant: Long) {
        var a = attacks
        while (a != 0L) {
            val to = BitboardUtils.lsb(a)
            if (to and others != 0L) {
                addMoves(Piece.PAWN, fromIndex, BitboardUtils.square2Index(to), true, 0)
            } else if (to and passant != 0L) {
                addMoves(Piece.PAWN, fromIndex, BitboardUtils.square2Index(to), true, Move.TYPE_PASSANT)
            }
            a = a xor to
        }
    }

    /**
     * Adds a move (it can add a non legal move)
     */
    private fun addMoves(pieceMoved: Int, fromIndex: Int, toIndex: Int, capture: Boolean, moveType: Int) {
        if (pieceMoved == Piece.PAWN && (toIndex < 8 || toIndex >= 56)) {
            moves[moveIndex++] = Move.genMove(fromIndex, toIndex, pieceMoved, capture, Move.TYPE_PROMOTION_QUEEN)
            moves[moveIndex++] = Move.genMove(fromIndex, toIndex, pieceMoved, capture, Move.TYPE_PROMOTION_KNIGHT)
            moves[moveIndex++] = Move.genMove(fromIndex, toIndex, pieceMoved, capture, Move.TYPE_PROMOTION_ROOK)
            moves[moveIndex++] = Move.genMove(fromIndex, toIndex, pieceMoved, capture, Move.TYPE_PROMOTION_BISHOP)
        } else {
            moves[moveIndex++] = Move.genMove(fromIndex, toIndex, pieceMoved, capture, moveType)
        }
    }
}