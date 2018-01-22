package karballo

import karballo.bitboard.BitboardAttacks
import karballo.bitboard.BitboardUtils

/**
 * For efficiency moves are int, this is a static class to threat with this
 *
 *
 * Move format (18 bits):
 * MTMTCXPPPFFFFFFTTTTTT
 * ---------------^ To index (6 bits)
 * ---------^ From index (6 bits)
 * ------^ Piece moved (3 bits)
 * -----^ Is capture (1 bit)
 * ----^ Is check (1 bit)
 * ^ Move type (4 bits)

 * @author Alberto Alonso Ruibal
 */
object Move {
    // Predefined moves
    val NONE = 0
    val NULL = -1

    val NONE_STRING = "none"
    val NULL_STRING = "null"

    val PIECE_LETTERS_LOWERCASE = " pnbrqk"
    val PIECE_LETTERS_UPPERCASE = " PNBRQK"

    // Move Types
    val TYPE_KINGSIDE_CASTLING = 1
    val TYPE_QUEENSIDE_CASTLING = 2
    val TYPE_PASSANT = 3
    // Promotions must be always >= TYPE_PROMOTION_QUEEN
    val TYPE_PROMOTION_QUEEN = 4
    val TYPE_PROMOTION_KNIGHT = 5
    val TYPE_PROMOTION_BISHOP = 6
    val TYPE_PROMOTION_ROOK = 7
    val TYPE_PROMOTION_KING = 8 // For giveaway/suicide variants

    val CHECK_MASK = 0x1 shl 16
    val CAPTURE_MASK = 0x1 shl 15

    fun genMove(fromIndex: Int, toIndex: Int, pieceMoved: Int, capture: Boolean, check: Boolean, moveType: Int): Int {
        return toIndex or (fromIndex shl 6) or (pieceMoved shl 12) or (if (capture) CAPTURE_MASK else 0) or (if (check) CHECK_MASK else 0) or (moveType shl 17)
    }

    fun genMove(fromIndex: Int, toIndex: Int, pieceMoved: Int, capture: Boolean, moveType: Int): Int {
        return toIndex or (fromIndex shl 6) or (pieceMoved shl 12) or (if (capture) CAPTURE_MASK else 0) or (moveType shl 17)
    }

    fun getToIndex(move: Int): Int {
        return move and 0x3f
    }

    fun getToSquare(move: Int): Long {
        return 0x1L shl (move and 0x3f)
    }

    fun getFromIndex(move: Int): Int {
        return move.ushr(6) and 0x3f
    }

    fun getFromSquare(move: Int): Long {
        return 0x1L shl (move.ushr(6) and 0x3f)
    }

    fun getPieceMoved(move: Int): Int {
        return move.ushr(12) and 0x7
    }

    fun getPieceCaptured(board: Board, move: Int): Int {
        if (getMoveType(move) == TYPE_PASSANT) {
            return Piece.PAWN
        }
        val toSquare = getToSquare(move)
        if (toSquare and board.pawns != 0L) {
            return Piece.PAWN
        } else if (toSquare and board.knights != 0L) {
            return Piece.KNIGHT
        } else if (toSquare and board.bishops != 0L) {
            return Piece.BISHOP
        } else if (toSquare and board.rooks != 0L) {
            return Piece.ROOK
        } else if (toSquare and board.queens != 0L) {
            return Piece.QUEEN
        }
        return 0
    }

    fun isCapture(move: Int): Boolean {
        return move and CAPTURE_MASK != 0
    }

    fun isCheck(move: Int): Boolean {
        return move and CHECK_MASK != 0
    }

    fun isCaptureOrCheck(move: Int): Boolean {
        return move and (CHECK_MASK or CAPTURE_MASK) != 0
    }

    fun getMoveType(move: Int): Int {
        return move.ushr(17) and 0xf
    }

    // Pawn push to 7 or 8th rank
    fun isPawnPush(move: Int): Boolean {
        return Move.getPieceMoved(move) == Piece.PAWN && (Move.getToIndex(move) < 16 || Move.getToIndex(move) > 47)
    }

    // Pawn push to 6, 7 or 8th rank
    fun isPawnPush678(move: Int): Boolean {
        return Move.getPieceMoved(move) == Piece.PAWN && if (Move.getFromIndex(move) < Move.getToIndex(move)) Move.getToIndex(move) >= 40 else Move.getToIndex(move) < 24
    }

    // Pawn push to 5, 6, 7 or 8th rank
    fun isPawnPush5678(move: Int): Boolean {
        return Move.getPieceMoved(move) == Piece.PAWN && if (Move.getFromIndex(move) < Move.getToIndex(move)) Move.getToIndex(move) >= 32 else Move.getToIndex(move) < 32
    }

    /**
     * Checks if this move is a promotion
     */
    fun isPromotion(move: Int): Boolean {
        return Move.getMoveType(move) >= TYPE_PROMOTION_QUEEN
    }

    fun getPiecePromoted(move: Int): Int {
        when (getMoveType(move)) {
            TYPE_PROMOTION_QUEEN -> return Piece.QUEEN
            TYPE_PROMOTION_ROOK -> return Piece.ROOK
            TYPE_PROMOTION_KNIGHT -> return Piece.KNIGHT
            TYPE_PROMOTION_BISHOP -> return Piece.BISHOP
            TYPE_PROMOTION_KING -> return Piece.KING
        }
        return 0
    }

    /**
     * Is capture or promotion

     * @param move
     * *
     * @return
     */
    fun isTactical(move: Int): Boolean {
        return Move.isCapture(move) || Move.isPromotion(move)
    }

    fun isCastling(move: Int): Boolean {
        return Move.getMoveType(move) == TYPE_KINGSIDE_CASTLING || Move.getMoveType(move) == TYPE_QUEENSIDE_CASTLING
    }

    /**
     * Given a board creates a move from a String in uci format or short
     * algebraic form. verifyValidMove true is mandatory if using sort algebraic

     * @param board
     * *
     * @param move
     */
    fun getFromString(board: Board, move: String, verifyValidMove: Boolean): Int {
        var m = move
        if (NULL_STRING == m) {
            return Move.NULL
        } else if ("" == m || NONE_STRING == m) {
            return Move.NONE
        }

        var fromIndex: Int
        val toIndex: Int
        var moveType = 0
        var pieceMoved = 0
        val check = m.indexOf("+") > 0 || m.indexOf("#") > 0
        val mines = board.mines
        val turn = board.turn

        // Ignore checks, captures indicators...
        m = m.replace("+", "").replace("x", "").replace("-", "").replace("=", "").replace("#", "").replace("?", "").replace("!", "").replace(" ", "").replace("0", "o").replace("O", "o")

        if ("oo" == m) {
            m = BitboardUtils.SQUARE_NAMES[BitboardUtils.square2Index(board.kings and mines)] + //
                    BitboardUtils.SQUARE_NAMES[BitboardUtils.square2Index(if (board.chess960) board.castlingRooks[if (turn) 0 else 2] else Board.CASTLING_KING_DESTINY_SQUARE[if (turn) 0 else 2])]
        } else if ("ooo" == m) {
            m = BitboardUtils.SQUARE_NAMES[BitboardUtils.square2Index(board.kings and mines)] + //
                    BitboardUtils.SQUARE_NAMES[BitboardUtils.square2Index(if (board.chess960) board.castlingRooks[if (turn) 1 else 3] else Board.CASTLING_KING_DESTINY_SQUARE[if (turn) 1 else 3])]
        } else {
            val promo = m[m.length - 1]
            when (promo.toLowerCase()) {
                'q' -> moveType = TYPE_PROMOTION_QUEEN
                'n' -> moveType = TYPE_PROMOTION_KNIGHT
                'b' -> moveType = TYPE_PROMOTION_BISHOP
                'r' -> moveType = TYPE_PROMOTION_ROOK
                'k' -> moveType = TYPE_PROMOTION_KING
            }
            // If promotion, remove the last char
            if (moveType != 0) {
                m = m.substring(0, m.length - 1)
            }
        }

        // To is always the last 2 characters
        toIndex = BitboardUtils.algebraic2Index(m.substring(m.length - 2, m.length))
        val to = 0x1L shl toIndex
        var from: Long = 0

        val bbAttacks = BitboardAttacks.getInstance()

        // Fills from with a mask of possible from values
        when (m[0]) {
            'N' -> from = board.knights and mines and bbAttacks.knight[toIndex]
            'K' -> from = board.kings and mines and bbAttacks.king[toIndex]
            'R' -> from = board.rooks and mines and bbAttacks.getRookAttacks(toIndex, board.all)
            'B' -> from = board.bishops and mines and bbAttacks.getBishopAttacks(toIndex, board.all)
            'Q' -> from = board.queens and mines and (bbAttacks.getRookAttacks(toIndex, board.all) or bbAttacks.getBishopAttacks(toIndex, board.all))
        }
        if (from != 0L) { // remove the piece char
            m = m.substring(1)
        } else { // Pawn moves
            if (m.length == 2) {
                if (turn) {
                    from = board.pawns and mines and (to.ushr(8) or if (to.ushr(8) and board.all == 0L) to.ushr(16) else 0)
                } else {
                    from = board.pawns and mines and (to shl 8 or if (to shl 8 and board.all == 0L) to shl 16 else 0)
                }
            }
            if (m.length == 3) { // Pawn capture
                from = board.pawns and mines and bbAttacks.pawn[if (turn) Color.B else Color.W][toIndex]
            }
        }
        if (m.length == 3) { // now disambiaguate
            val disambiguate = m[0]
            val i = "abcdefgh".indexOf(disambiguate)
            if (i >= 0) {
                from = from and BitboardUtils.FILE[i]
            }
            val j = "12345678".indexOf(disambiguate)
            if (j >= 0) {
                from = from and BitboardUtils.RANK[j]
            }
        }
        if (m.length == 4) { // was algebraic complete e2e4 (=UCI!)
            from = BitboardUtils.algebraic2Square(m.substring(0, 2))
        }
        if (from == 0L || from and board.mines == 0L) {
            return NONE
        }

        // Treats multiple froms, choosing the first Legal Move
        while (from != 0L) {
            val myFrom = BitboardUtils.lsb(from)
            from = from xor myFrom
            fromIndex = BitboardUtils.square2Index(myFrom)

            var capture = false
            if (myFrom and board.pawns != 0L) {

                pieceMoved = Piece.PAWN
                // for passant captures
                if (toIndex != fromIndex - 8 && toIndex != fromIndex + 8 && toIndex != fromIndex - 16 && toIndex != fromIndex + 16) {
                    if (to and board.all == 0L) {
                        moveType = TYPE_PASSANT
                        capture = true // later is changed if it was not a pawn
                    }
                }
                // Default promotion to queen if not specified
                if (to and (BitboardUtils.b_u or BitboardUtils.b_d) != 0L && moveType < TYPE_PROMOTION_QUEEN) {
                    moveType = TYPE_PROMOTION_QUEEN
                }
            }
            if (myFrom and board.bishops != 0L) {
                pieceMoved = Piece.BISHOP
            } else if (myFrom and board.knights != 0L) {
                pieceMoved = Piece.KNIGHT
            } else if (myFrom and board.rooks != 0L) {
                pieceMoved = Piece.ROOK
            } else if (myFrom and board.queens != 0L) {
                pieceMoved = Piece.QUEEN
            } else if (myFrom and board.kings != 0L) {
                pieceMoved = Piece.KING
                if ((if (turn) board.whiteKingsideCastling else board.blackKingsideCastling) && //
                        (toIndex == fromIndex - 2 || to == board.castlingRooks[if (turn) 0 else 2])) {
                    moveType = TYPE_KINGSIDE_CASTLING
                }
                if ((if (turn) board.whiteQueensideCastling else board.blackQueensideCastling) && //
                        (toIndex == fromIndex + 2 || to == board.castlingRooks[if (turn) 1 else 3])) {
                    moveType = TYPE_QUEENSIDE_CASTLING
                }
            }

            // Now set captured piece flag
            if ((to and if (turn) board.blacks else board.whites) != 0L) {
                capture = true
            }
            var moveInt = Move.genMove(fromIndex, toIndex, pieceMoved, capture, check, moveType)
            if (verifyValidMove) {
                moveInt = board.getLegalMove(moveInt)
                if (moveInt != NONE) {
                    return moveInt
                }
            } else {
                return moveInt
            }
        }
        return NONE
    }

    /**
     * Gets an UCI-String representation of the move

     * @param move
     * *
     * @return
     */
    fun toString(move: Int): String {
        if (move == Move.NONE) {
            return NONE_STRING
        } else if (move == Move.NULL) {
            return NULL_STRING
        }
        val sb = StringBuilder()
        sb.append(BitboardUtils.index2Algebraic(Move.getFromIndex(move)))
        sb.append(BitboardUtils.index2Algebraic(Move.getToIndex(move)))
        if (isPromotion(move)) {
            sb.append(PIECE_LETTERS_LOWERCASE[getPiecePromoted(move)])
        }
        return sb.toString()
    }

    fun toStringExt(move: Int): String {
        if (move == Move.NONE) {
            return NONE_STRING
        } else if (move == Move.NULL) {
            return NULL_STRING
        } else if (Move.getMoveType(move) == TYPE_KINGSIDE_CASTLING) {
            return if (Move.isCheck(move)) "O-O+" else "O-O"
        } else if (Move.getMoveType(move) == TYPE_QUEENSIDE_CASTLING) {
            return if (Move.isCheck(move)) "O-O-O+" else "O-O-O"
        }

        val sb = StringBuilder()
        if (getPieceMoved(move) != Piece.PAWN) {
            sb.append(PIECE_LETTERS_UPPERCASE[getPieceMoved(move)])
        }
        sb.append(BitboardUtils.index2Algebraic(Move.getFromIndex(move)))
        sb.append(if (isCapture(move)) 'x' else '-')
        sb.append(BitboardUtils.index2Algebraic(Move.getToIndex(move)))
        if (isPromotion(move)) {
            sb.append(PIECE_LETTERS_LOWERCASE[getPiecePromoted(move)])
        }
        if (isCheck(move)) {
            sb.append("+")
        }
        return sb.toString()
    }

    /**
     * It does not append + or #

     * @param board
     * *
     * @param move
     * *
     * @return
     */
    fun toSan(board: Board, move: Int): String {
        if (move == Move.NONE) {
            return NONE_STRING
        } else if (move == Move.NULL) {
            return NULL_STRING
        }
        board.generateLegalMoves()

        var isLegal = false
        var disambiguate = false
        var fileEqual = false
        var rankEqual = false
        for (i in 0..board.legalMoveCount - 1) {
            val move2 = board.legalMoves[i]
            if (move == move2) {
                isLegal = true
            } else if (getToIndex(move) == getToIndex(move2)
                    && getPieceMoved(move) == getPieceMoved(move2)
                    && getMoveType(move) == getMoveType(move2)) {
                disambiguate = true
                if (getFromIndex(move) % 8 == getFromIndex(move2) % 8) {
                    fileEqual = true
                }
                if (getFromIndex(move) / 8 == getFromIndex(move2) / 8) {
                    rankEqual = true
                }
            }
        }
        if (!isLegal) {
            return Move.NONE_STRING
        } else if (Move.getMoveType(move) == TYPE_KINGSIDE_CASTLING) {
            return if (Move.isCheck(move)) "O-O+" else "O-O"
        } else if (Move.getMoveType(move) == TYPE_QUEENSIDE_CASTLING) {
            return if (Move.isCheck(move)) "O-O-O+" else "O-O-O"
        }

        val sb = StringBuilder()
        if (getPieceMoved(move) != Piece.PAWN) {
            sb.append(PIECE_LETTERS_UPPERCASE[getPieceMoved(move)])
        }
        val fromSq = BitboardUtils.index2Algebraic(Move.getFromIndex(move))

        if (isCapture(move) && getPieceMoved(move) == Piece.PAWN) {
            disambiguate = true
        }

        if (disambiguate) {
            if (fileEqual && rankEqual) {
                sb.append(fromSq)
            } else if (fileEqual) {
                sb.append(fromSq[1])
            } else {
                sb.append(fromSq[0])
            }
        }

        if (isCapture(move)) {
            sb.append("x")
        }
        sb.append(BitboardUtils.index2Algebraic(Move.getToIndex(move)))
        if (isPromotion(move)) {
            sb.append("=")
            sb.append(PIECE_LETTERS_UPPERCASE[getPiecePromoted(move)])
        }
        if (isCheck(move)) {
            sb.append("+")
        }
        return sb.toString()
    }

    fun printMoves(moves: IntArray, from: Int, to: Int) {
        for (i in from..to - 1) {
            print(Move.toStringExt(moves[i]))
            print(" ")
        }
        println()
    }

    fun sanToFigurines(`in`: String?): String? {
        return if (`in` == null) null else `in`.replace("N", "♘").replace("B", "♗").replace("R", "♖").replace("Q", "♕").replace("K", "♔")
    }
}