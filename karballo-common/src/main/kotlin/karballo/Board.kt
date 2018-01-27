package karballo

import karballo.bitboard.AttacksInfo
import karballo.bitboard.BitboardAttacks
import karballo.bitboard.BitboardUtils
import karballo.hash.ZobristKey
import karballo.util.Utils
import kotlin.math.max

/**
 * Stores the position and the move history
 * TODO Other chess variants like Atomic, Suicide, etc.

 * @author Alberto Alonso Ruibal
 */
class Board {

    internal var legalMoveGenerator = karballo.movegen.LegalMoveGenerator()
    internal var legalMoves = IntArray(256)
    internal var legalMoveCount = -1 // if -1 then legal moves not generated
    internal var legalMovesKey = longArrayOf(0, 0)
    var movesSan = HashMap<Int, String>()

    // Bitboard arrays
    var whites: Long = 0L
    var blacks: Long = 0L
    var pawns: Long = 0L
    var rooks: Long = 0L
    var queens: Long = 0L
    var bishops: Long = 0L
    var knights: Long = 0L
    var kings: Long = 0L
    var flags: Long = 0L

    var fiftyMovesRule = 0
    var initialMoveNumber = 0
    var moveNumber = 0
    var outBookMove = Int.MAX_VALUE
    var key = longArrayOf(0, 0)

    lateinit var initialFen: String

    // History array indexed by moveNumber
    var keyHistory = Array(MAX_MOVES) { LongArray(2) } // to detect draw by treefold
    var moveHistory = IntArray(MAX_MOVES)
    var whitesHistory = LongArray(MAX_MOVES)
    var blacksHistory = LongArray(MAX_MOVES)
    var pawnsHistory = LongArray(MAX_MOVES)
    var rooksHistory = LongArray(MAX_MOVES)
    var queensHistory = LongArray(MAX_MOVES)
    var bishopsHistory = LongArray(MAX_MOVES)
    var knightsHistory = LongArray(MAX_MOVES)
    var kingsHistory = LongArray(MAX_MOVES)
    var flagsHistory = LongArray(MAX_MOVES)
    var fiftyMovesRuleHistory = IntArray(MAX_MOVES)
    var seeGain = IntArray(32)

    // Origin squares for the castling rook {White Kingside, White Queenside, Black Kingside, Black Queenside}
    var castlingRooks = longArrayOf(0, 0, 0, 0)

    var chess960: Boolean = false // basically decides the destiny square of the castlings

    internal var bbAttacks = BitboardAttacks.getInstance()

    /**
     * It also computes the zobrist key
     */
    fun startPosition() {
        fen = Companion.FEN_START_POSITION
    }

    /**
     * Set a Chess960 start position
     * http://en.wikipedia.org/wiki/Chess960_numbering_scheme
     */
    fun startPosition(chess960Position: Int) {
        val base = Companion.CHESS960_START_POSITIONS_BISHOPS[chess960Position and 0x0f]
        val otherPieces = Companion.CHESS960_START_POSITIONS[chess960Position.ushr(4)]
        val oSB = StringBuilder()
        var j = 0
        for (i in 0..7) {
            if (base[i] == '-') {
                oSB.append(otherPieces[j])
                j++
            } else {
                oSB.append('B')
            }
        }

        fen = oSB.toString().toLowerCase() + "/pppppppp/8/8/8/8/PPPPPPPP/" + oSB.toString() + " w KQkq - 0 1"
        chess960 = true
    }

    fun getKey(): Long {
        return key[0] xor key[1]
    }

    val exclusionKey: Long
        get() = key[0] xor key[1] xor ZobristKey.exclusionKey

    /**
     * An alternative key to avoid collisions in the TT
     */
    val key2: Long
        get() = key[0] xor key[1].inv()

    /**
     * @return true if white moves
     */
    val turn: Boolean
        get() = flags and FLAG_TURN == 0L

    /**
     * Returns the castling destiny square, 0 it it cannot castle
     * Supports Chess960 (the rook origin square is the castling destiny sq for chess 960)
     */
    fun canCastleKingSide(color: Int, ai: AttacksInfo): Long {
        if (if (color == Color.W) whiteKingsideCastling else blackKingsideCastling) {
            val rookOrigin = castlingRooks[if (color == Color.W) 0 else 2]
            val rookDestiny = Board.CASTLING_ROOK_DESTINY_SQUARE[if (color == Color.W) 0 else 2]
            val rookRoute = BitboardUtils.getHorizontalLine(rookDestiny, rookOrigin) and rookOrigin.inv()
            val kingOrigin = kings and if (color == Color.W) whites else blacks
            val kingDestiny = Board.CASTLING_KING_DESTINY_SQUARE[if (color == Color.W) 0 else 2]
            val kingRoute = BitboardUtils.getHorizontalLine(kingOrigin, kingDestiny) and kingOrigin.inv()

            if (whites or blacks and (kingRoute or rookRoute) and rookOrigin.inv() and kingOrigin.inv() == 0L && ai.attackedSquaresAlsoPinned[1 - color] and kingRoute == 0L) {
                return if (chess960) rookOrigin else kingDestiny
            }
        }
        return 0
    }

    /**
     * Returns the castling destiny square, 0 it it cannot castle
     * Supports Chess960 (the rook origin square is the castling destiny sq for chess 960)
     */
    fun canCastleQueenSide(color: Int, ai: AttacksInfo): Long {
        if (if (color == Color.W) whiteQueensideCastling else blackQueensideCastling) {
            val rookOrigin = castlingRooks[if (color == Color.W) 1 else 3]
            val rookDestiny = Board.CASTLING_ROOK_DESTINY_SQUARE[if (color == Color.W) 1 else 3]
            val rookRoute = BitboardUtils.getHorizontalLine(rookOrigin, rookDestiny) and rookOrigin.inv()
            val kingOrigin = kings and if (color == Color.W) whites else blacks
            val kingDestiny = Board.CASTLING_KING_DESTINY_SQUARE[if (color == Color.W) 1 else 3]
            val kingRoute = BitboardUtils.getHorizontalLine(kingDestiny, kingOrigin) and kingOrigin.inv()

            if (whites or blacks and (kingRoute or rookRoute) and rookOrigin.inv() and kingOrigin.inv() == 0L && ai.attackedSquaresAlsoPinned[1 - color] and kingRoute == 0L) {
                return if (chess960) rookOrigin else kingDestiny
            }
        }
        return 0
    }

    val whiteKingsideCastling: Boolean
        get() = flags and FLAG_WHITE_KINGSIDE_CASTLING != 0L

    val whiteQueensideCastling: Boolean
        get() = flags and FLAG_WHITE_QUEENSIDE_CASTLING != 0L

    val blackKingsideCastling: Boolean
        get() = flags and FLAG_BLACK_KINGSIDE_CASTLING != 0L

    val blackQueensideCastling: Boolean
        get() = flags and FLAG_BLACK_QUEENSIDE_CASTLING != 0L

    val passantSquare: Long
        get() = flags and FLAGS_PASSANT

    val check: Boolean
        get() = flags and FLAG_CHECK != 0L

    val all: Long
        get() = whites or blacks

    val mines: Long
        get() = if (flags and FLAG_TURN == 0L) whites else blacks

    val others: Long
        get() = if (flags and FLAG_TURN == 0L) blacks else whites

    fun getPieceIntAt(square: Long): Int {
        return (if (pawns and square != 0L)
            Piece.PAWN
        else if (knights and square != 0L)
            Piece.KNIGHT
        else if (bishops and square != 0L)
            Piece.BISHOP
        else if (rooks and square != 0L)
            Piece.ROOK
        else if (queens and square != 0L)
            Piece.QUEEN
        else if (kings and square != 0L)
            Piece.KING
        else 0).toInt()
    }

    fun getPieceAt(square: Long): Char {
        val p = if (pawns and square != 0L)
            'p'
        else if (knights and square != 0L)
            'n'
        else if (bishops and square != 0L)
            'b'
        else if (rooks and square != 0L)
            'r'
        else if (queens and square != 0L)
            'q'
        else if (kings and square != 0L) 'k' else '.'
        return if (whites and square != 0L) p.toUpperCase() else p
    }

    fun getPieceUnicodeAt(square: Long): Char {
        if (whites and square != 0L) {
            return if (pawns and square != 0L)
                '♙'
            else if (knights and square != 0L)
                '♘'
            else if (bishops and square != 0L)
                '♗'
            else if (rooks and square != 0L)
                '♖'
            else if (queens and square != 0L)
                '♕'
            else if (kings and square != 0L)
                '♔'
            else '.'

        } else if (blacks and square != 0L) {
            return if (pawns and square != 0L)
                '♟'
            else if (knights and square != 0L)
                '♞'
            else if (bishops and square != 0L)
                '♝'
            else if (rooks and square != 0L)
                '♜'
            else if (queens and square != 0L)
                '♛'
            else if (kings and square != 0L)
                '♚'
            else '.'
        } else {
            return '_'
        }
    }

    fun setPieceAt(square: Long, piece: Char) {
        pawns = pawns and square.inv()
        queens = queens and square.inv()
        rooks = rooks and square.inv()
        bishops = bishops and square.inv()
        knights = knights and square.inv()
        kings = kings and square.inv()

        if (piece == ' ' || piece == '.') {
            whites = whites and square.inv()
            blacks = blacks and square.inv()
            return
        } else if (piece == piece.toLowerCase()) {
            whites = whites and square.inv()
            blacks = blacks or square
        } else {
            whites = whites or square
            blacks = blacks and square.inv()
        }

        when (piece.toLowerCase()) {
            'p' -> pawns = pawns or square
            'q' -> queens = queens or square
            'r' -> rooks = rooks or square
            'b' -> bishops = bishops or square
            'n' -> knights = knights or square
            'k' -> kings = kings or square
        }

        key = ZobristKey.getKey(this)
        setCheckFlags()
    }

    /**
     * Converts board to its fen notation
     */
    /**
     * Loads board from a fen notation
     */
    // 0,1->1.. 2,3->2
    var fen: String
        get() {
            val sb = StringBuilder()
            var i = Square.A8
            var j = 0
            while (i != 0L) {
                val p = getPieceAt(i)
                if (p == '.') {
                    j++
                }
                if (j != 0 && (p != '.' || i and BitboardUtils.b_r != 0L)) {
                    sb.append(j)
                    j = 0
                }
                if (p != '.') {
                    sb.append(p)
                }
                if (i != 1L && i and BitboardUtils.b_r != 0L) {
                    sb.append("/")
                }
                i = i ushr 1
            }
            sb.append(" ")
            sb.append(if (turn) "w" else "b")
            sb.append(" ")
            if (whiteKingsideCastling) {
                sb.append("K")
            }
            if (whiteQueensideCastling) {
                sb.append("Q")
            }
            if (blackKingsideCastling) {
                sb.append("k")
            }
            if (blackQueensideCastling) {
                sb.append("q")
            }
            if (!whiteQueensideCastling && !whiteKingsideCastling && !blackQueensideCastling && !blackKingsideCastling) {
                sb.append("-")
            }
            sb.append(" ")
            sb.append(if (passantSquare != 0L) BitboardUtils.square2Algebraic(passantSquare) else "-")
            sb.append(" ")
            sb.append(fiftyMovesRule)
            sb.append(" ")
            sb.append((moveNumber shr 1) + 1)
            return sb.toString()
        }
        set(fen) = setFenMove(fen, null)

    /**
     * Sets fen without destroying move history. If lastMove = null destroy the move history
     */
    fun setFenMove(fen: String, lastMove: String?) {
        var tmpWhites: Long = 0
        var tmpBlacks: Long = 0
        var tmpPawns: Long = 0
        var tmpRooks: Long = 0
        var tmpQueens: Long = 0
        var tmpBishops: Long = 0
        var tmpKnights: Long = 0
        var tmpKings: Long = 0
        var tmpFlags: Long
        var tmpFiftyMovesRule = 0
        val tmpCastlingRooks = longArrayOf(0, 0, 0, 0)
        var fenMoveNumber = 0

        var i = 0
        var j = Square.A8
        val tokens = fen.split("[ \\t\\n\\x0B\\f\\r]+".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        val board = tokens[0]

        while (i < board.length && j != 0L) {
            val p = board[i++]
            if (p != '/') {
                var number = 0
                try {
                    number = p.toString().toInt()
                } catch (ignored: Exception) {
                }

                for (k in 0..(if (number == 0) 1 else number) - 1) {
                    tmpWhites = tmpWhites and j.inv() or if (number == 0 && p == p.toUpperCase()) j else 0
                    tmpBlacks = tmpBlacks and j.inv() or if (number == 0 && p == p.toLowerCase()) j else 0
                    tmpPawns = tmpPawns and j.inv() or if (p.toUpperCase() == 'P') j else 0
                    tmpRooks = tmpRooks and j.inv() or if (p.toUpperCase() == 'R') j else 0
                    tmpQueens = tmpQueens and j.inv() or if (p.toUpperCase() == 'Q') j else 0
                    tmpBishops = tmpBishops and j.inv() or if (p.toUpperCase() == 'B') j else 0
                    tmpKnights = tmpKnights and j.inv() or if (p.toUpperCase() == 'N') j else 0
                    tmpKings = tmpKings and j.inv() or if (p.toUpperCase() == 'K') j else 0
                    j = j ushr 1
                    if (j == 0L) {
                        break // security
                    }
                }
            }
        }

        // Now the rest ...
        val turn = tokens[1]
        tmpFlags = 0
        if ("b" == turn) {
            tmpFlags = tmpFlags or FLAG_TURN
        }
        if (tokens.size > 2) {
            // Set castling rights supporting XFEN to disambiguate positions in Chess960
            val castlings = tokens[2]

            chess960 = false
            // Squares to the sides of the kings {White Kingside, White Queenside, Black Kingside, Black Queenside}
            val whiteKingLateralSquares = longArrayOf(BitboardUtils.b_d and (tmpKings and tmpWhites) - 1, BitboardUtils.b_d and ((tmpKings and tmpWhites) - 1 or (tmpKings and tmpWhites)).inv(), BitboardUtils.b_u and (tmpKings and tmpBlacks) - 1, BitboardUtils.b_u and ((tmpKings and tmpBlacks) - 1 or (tmpKings and tmpBlacks)).inv())

            // Squares where we can find a castling rook
            val possibleCastlingRookSquares = longArrayOf(0, 0, 0, 0)

            for (k in 0..castlings.length - 1) {
                val c = castlings[k]
                when (c) {
                    'K' -> possibleCastlingRookSquares[0] = whiteKingLateralSquares[0]
                    'Q' -> possibleCastlingRookSquares[1] = whiteKingLateralSquares[1]
                    'k' -> possibleCastlingRookSquares[2] = whiteKingLateralSquares[2]
                    'q' -> possibleCastlingRookSquares[3] = whiteKingLateralSquares[3]
                    else -> {
                        // Shredder-FEN receives the name of the file where the castling rook is
                        val whiteFile = "ABCDEFGH".indexOf(c)
                        val blackFile = "abcdefgh".indexOf(c)
                        if (whiteFile >= 0) {
                            val rookSquare = BitboardUtils.b_d and BitboardUtils.FILE[whiteFile]
                            if (rookSquare and whiteKingLateralSquares[0] != 0L) {
                                possibleCastlingRookSquares[0] = rookSquare
                            } else if (rookSquare and whiteKingLateralSquares[1] != 0L) {
                                possibleCastlingRookSquares[1] = rookSquare
                            }
                        } else if (blackFile >= 0) {
                            val rookSquare = BitboardUtils.b_u and BitboardUtils.FILE[blackFile]
                            if (rookSquare and whiteKingLateralSquares[2] != 0L) {
                                possibleCastlingRookSquares[2] = rookSquare
                            } else if (rookSquare and whiteKingLateralSquares[3] != 0L) {
                                possibleCastlingRookSquares[3] = rookSquare
                            }
                        }
                    }
                }
            }

            // Now store the squares of the castling rooks
            tmpCastlingRooks[0] = BitboardUtils.lsb(tmpRooks and tmpWhites and possibleCastlingRookSquares[0])
            tmpCastlingRooks[1] = BitboardUtils.msb(tmpRooks and tmpWhites and possibleCastlingRookSquares[1])
            tmpCastlingRooks[2] = BitboardUtils.lsb(tmpRooks and tmpBlacks and possibleCastlingRookSquares[2])
            tmpCastlingRooks[3] = BitboardUtils.msb(tmpRooks and tmpBlacks and possibleCastlingRookSquares[3])

            // Set the castling flags and detect Chess960
            if (tmpCastlingRooks[0] != 0L) {
                tmpFlags = tmpFlags or FLAG_WHITE_KINGSIDE_CASTLING
                if (tmpWhites and tmpKings != 1L shl 3 || tmpCastlingRooks[0] != 1L) {
                    chess960 = true
                }
            }
            if (tmpCastlingRooks[1] != 0L) {
                tmpFlags = tmpFlags or FLAG_WHITE_QUEENSIDE_CASTLING
                if (tmpWhites and tmpKings != 1L shl 3 || tmpCastlingRooks[1] != 1L shl 7) {
                    chess960 = true
                }
            }
            if (tmpCastlingRooks[2] != 0L) {
                tmpFlags = tmpFlags or FLAG_BLACK_KINGSIDE_CASTLING
                if (tmpBlacks and tmpKings != 1L shl 59 || tmpCastlingRooks[2] != 1L shl 56) {
                    chess960 = true
                }
            }
            if (tmpCastlingRooks[3] != 0L) {
                tmpFlags = tmpFlags or FLAG_BLACK_QUEENSIDE_CASTLING
                if (tmpBlacks and tmpKings != 1L shl 59 || tmpCastlingRooks[3] != 1L shl 63) {
                    chess960 = true
                }
            }
            // END FEN castlings

            if (tokens.size > 3) {
                val passant = tokens[3]
                tmpFlags = tmpFlags or (FLAGS_PASSANT and BitboardUtils.algebraic2Square(passant))
                if (tokens.size > 4) {
                    try {
                        tmpFiftyMovesRule = tokens[4].toInt()
                    } catch (e: Exception) {
                        tmpFiftyMovesRule = 0
                    }

                    if (tokens.size > 5) {
                        val moveNumberString = tokens[5]
                        val aux = moveNumberString.toInt()
                        fenMoveNumber = ((if (aux > 0) aux - 1 else aux) shl 1) + if (tmpFlags and FLAG_TURN == 0L) 0 else 1
                        if (fenMoveNumber < 0) {
                            fenMoveNumber = 0
                        }
                    }
                }
            }
        }

        // try to apply the last move to see if we are advancing or undoing moves
        if (moveNumber + 1 == fenMoveNumber && lastMove != null) {
            doMove(Move.getFromString(this, lastMove, true))
        } else if (fenMoveNumber < moveNumber) {
            for (k in moveNumber downTo fenMoveNumber + 1) {
                undoMove()
            }
        }

        // Check if board changed or if we can keep the history
        if (whites != tmpWhites //

                || blacks != tmpBlacks //

                || pawns != tmpPawns //

                || rooks != tmpRooks //

                || queens != tmpQueens //

                || bishops != tmpBishops //

                || knights != tmpKnights //

                || kings != tmpKings //

                || flags and FLAG_TURN != tmpFlags and FLAG_TURN) {

            // board reset
            movesSan.clear()

            initialFen = fen
            initialMoveNumber = fenMoveNumber
            moveNumber = fenMoveNumber
            outBookMove = Int.MAX_VALUE

            whites = tmpWhites
            blacks = tmpBlacks
            pawns = tmpPawns
            rooks = tmpRooks
            queens = tmpQueens
            bishops = tmpBishops
            knights = tmpKnights
            kings = tmpKings
            fiftyMovesRule = tmpFiftyMovesRule

            // Flags are not completed till verify, so skip checking
            flags = tmpFlags

            castlingRooks[0] = tmpCastlingRooks[0]
            castlingRooks[1] = tmpCastlingRooks[1]
            castlingRooks[2] = tmpCastlingRooks[2]
            castlingRooks[3] = tmpCastlingRooks[3]

            // Set zobrist key and check flags
            key = ZobristKey.getKey(this)
            setCheckFlags()

            // and save history
            resetHistory()
            saveHistory(0, false)
        } else {
            if (moveNumber < outBookMove) {
                outBookMove = Int.MAX_VALUE
            }
        }
    }

    /**
     * Prints board in one string
     */
    override fun toString(): String {
        val sb = StringBuilder()
        var j = 8
        var i = Square.A8
        while (i != 0L) {
            sb.append(getPieceUnicodeAt(i))
            sb.append(" ")
            if (i and BitboardUtils.b_r != 0L) {
                sb.append(j--)
                if (i == Square.H1) {
                    sb.append(" ")
                    sb.append(fen)
                }
                sb.append("\n")
            }
            i = i ushr 1
        }
        sb.append("a b c d e f g h   ")
        sb.append(if (turn) "white moves " else "black moves ")
        sb.append((if (whiteKingsideCastling) " W:0-0" else "") + (if (whiteQueensideCastling) " W:0-0-0" else "") + (if (blackKingsideCastling) " B:0-0" else "") + if (blackQueensideCastling) " B:0-0-0" else "")

        return sb.toString()
    }

    /**
     * TODO is it necessary??
     */
    private fun resetHistory() {
        Utils.instance.arrayFill(whitesHistory, 0)
        Utils.instance.arrayFill(blacksHistory, 0)
        Utils.instance.arrayFill(pawnsHistory, 0)
        Utils.instance.arrayFill(knightsHistory, 0)
        Utils.instance.arrayFill(bishopsHistory, 0)
        Utils.instance.arrayFill(rooksHistory, 0)
        Utils.instance.arrayFill(queensHistory, 0)
        Utils.instance.arrayFill(kingsHistory, 0)
        Utils.instance.arrayFill(flagsHistory, 0)
        Utils.instance.arrayFill(fiftyMovesRuleHistory, 0)
        Utils.instance.arrayFill(moveHistory, 0)
        for (i in 0..MAX_MOVES - 1) {
            Utils.instance.arrayFill(keyHistory[i], 0)
        }
        movesSan.clear()
    }

    private fun saveHistory(move: Int, fillSanInfo: Boolean) {
        if (fillSanInfo) {
            movesSan.put(moveNumber, Move.toSan(this, move))
        }

        moveHistory[moveNumber] = move
        whitesHistory[moveNumber] = whites
        blacksHistory[moveNumber] = blacks
        pawnsHistory[moveNumber] = pawns
        knightsHistory[moveNumber] = knights
        bishopsHistory[moveNumber] = bishops
        rooksHistory[moveNumber] = rooks
        queensHistory[moveNumber] = queens
        kingsHistory[moveNumber] = kings
        flagsHistory[moveNumber] = flags
        keyHistory[moveNumber][0] = key[0]
        keyHistory[moveNumber][1] = key[1]
        fiftyMovesRuleHistory[moveNumber] = fiftyMovesRule
    }

    /**
     * Moves and also updates the board's zobrist key verify legality, if not
     * legal undo move and return false
     */
    fun doMove(move: Int, verifyCheck: Boolean = true, fillSanInfo: Boolean = true): Boolean {
        if (move == Move.NONE) {
            return false
        }
        // Save history
        saveHistory(move, fillSanInfo)

        // Count consecutive moves without capture or without pawn move
        fiftyMovesRule++
        moveNumber++ // Count Ply moves

        val turn = turn
        val color = if (turn) Color.W else Color.B

        if (flags and FLAGS_PASSANT != 0L) {
            // Remove passant flags: from the zobrist key
            key[1 - color] = key[1 - color] xor ZobristKey.passantFile[BitboardUtils.getFile(flags and FLAGS_PASSANT)]
            // and from the flags
            flags = flags and FLAGS_PASSANT.inv()
        }

        if (move == Move.NULL) {
            // Change turn
            flags = flags xor FLAG_TURN
            key[0] = key[0] xor ZobristKey.whiteMove
            return true
        }

        val fromIndex = Move.getFromIndex(move)
        val from = Move.getFromSquare(move)

        // Check if we are applying a move in the other turn
        if (from and mines == 0L) {
            undoMove()
            return false
        }

        var toIndex = Move.getToIndex(move)
        var to = Move.getToSquare(move)
        var moveMask = from or to // Move is as easy as xor with this mask (exceptions are promotions, captures and en-passant captures)
        val moveType = Move.getMoveType(move)
        val pieceMoved = Move.getPieceMoved(move)
        val capture = Move.isCapture(move)

        // Is it is a capture, remove pieces in destination square
        if (capture) {
            fiftyMovesRule = 0
            // En-passant pawn captures remove captured pawn, put the pawn in to
            var toIndexCapture = toIndex
            if (moveType == Move.TYPE_PASSANT) {
                to = if (turn) to.ushr(8) else to shl 8
                toIndexCapture += if (turn) -8 else 8
            }
            key[1 - color] = key[1 - color] xor ZobristKey.getKeyPieceIndex(toIndexCapture, getPieceAt(to))

            whites = whites and to.inv()
            blacks = blacks and to.inv()
            pawns = pawns and to.inv()
            queens = queens and to.inv()
            rooks = rooks and to.inv()
            bishops = bishops and to.inv()
            knights = knights and to.inv()
        }

        // Pawn movements
        when (pieceMoved) {
            Piece.PAWN -> {
                fiftyMovesRule = 0
                // Set new passant flags if pawn is advancing two squares (marks
                // the destination square where the pawn can be captured)
                // Set only passant flags when the other side can capture
                if (from shl 16 and to != 0L && bbAttacks.pawn[Color.W][toIndex - 8] and pawns and others != 0L) { // white
                    flags = flags or (from shl 8)
                }
                if (from.ushr(16) and to != 0L && bbAttacks.pawn[Color.B][toIndex + 8] and pawns and others != 0L) { // blask
                    flags = flags or from.ushr(8)
                }
                if (flags and FLAGS_PASSANT != 0L) {
                    key[color] = key[color] xor ZobristKey.passantFile[BitboardUtils.getFile(flags and FLAGS_PASSANT)]
                }

                if (moveType == Move.TYPE_PROMOTION_QUEEN || moveType == Move.TYPE_PROMOTION_KNIGHT || moveType == Move.TYPE_PROMOTION_BISHOP
                        || moveType == Move.TYPE_PROMOTION_ROOK) { // Promotions:
                    // change
                    // the piece
                    pawns = pawns and from.inv()
                    key[color] = key[color] xor ZobristKey.pawn[color][fromIndex]
                    when (moveType) {
                        Move.TYPE_PROMOTION_QUEEN -> {
                            queens = queens or to
                            key[color] = key[color] xor ZobristKey.queen[color][toIndex]
                        }
                        Move.TYPE_PROMOTION_KNIGHT -> {
                            knights = knights or to
                            key[color] = key[color] xor ZobristKey.knight[color][toIndex]
                        }
                        Move.TYPE_PROMOTION_BISHOP -> {
                            bishops = bishops or to
                            key[color] = key[color] xor ZobristKey.bishop[color][toIndex]
                        }
                        Move.TYPE_PROMOTION_ROOK -> {
                            rooks = rooks or to
                            key[color] = key[color] xor ZobristKey.rook[color][toIndex]
                        }
                        Move.TYPE_PROMOTION_KING -> {
                            kings = kings or to
                            key[color] = key[color] xor ZobristKey.king[color][toIndex]
                        }
                    }
                } else {
                    pawns = pawns xor moveMask
                    key[color] = key[color] xor (ZobristKey.pawn[color][fromIndex] xor ZobristKey.pawn[color][toIndex])
                }
            }
            Piece.ROOK -> {
                rooks = rooks xor moveMask
                key[color] = key[color] xor (ZobristKey.rook[color][fromIndex] xor ZobristKey.rook[color][toIndex])
            }
            Piece.BISHOP -> {
                bishops = bishops xor moveMask
                key[color] = key[color] xor (ZobristKey.bishop[color][fromIndex] xor ZobristKey.bishop[color][toIndex])
            }
            Piece.KNIGHT -> {
                knights = knights xor moveMask
                key[color] = key[color] xor (ZobristKey.knight[color][fromIndex] xor ZobristKey.knight[color][toIndex])
            }
            Piece.QUEEN -> {
                queens = queens xor moveMask
                key[color] = key[color] xor (ZobristKey.queen[color][fromIndex] xor ZobristKey.queen[color][toIndex])
            }
            Piece.KING // if castling, moves rooks too
            -> {
                if (moveType == Move.TYPE_KINGSIDE_CASTLING || moveType == Move.TYPE_QUEENSIDE_CASTLING) {
                    // {White Kingside, White Queenside, Black Kingside, Black Queenside}
                    val j = (color shl 1) + if (moveType == Move.TYPE_QUEENSIDE_CASTLING) 1 else 0

                    toIndex = CASTLING_KING_DESTINY_INDEX[j]
                    val originRookIndex = BitboardUtils.square2Index(castlingRooks[j])
                    val destinyRookIndex = CASTLING_ROOK_DESTINY_INDEX[j]
                    // Recalculate move mask for chess960 castlings
                    moveMask = from xor (1L shl toIndex)
                    val rookMoveMask = 1L shl originRookIndex xor (1L shl destinyRookIndex)
                    key[color] = key[color] xor (ZobristKey.rook[color][originRookIndex] xor ZobristKey.rook[color][destinyRookIndex])

                    if (turn) {
                        whites = whites xor rookMoveMask
                    } else {
                        blacks = blacks xor rookMoveMask
                    }
                    rooks = rooks xor rookMoveMask
                }
                kings = kings xor moveMask
                key[color] = key[color] xor (ZobristKey.king[color][fromIndex] xor ZobristKey.king[color][toIndex])
            }
        }
        // Move pieces in colour fields
        if (turn) {
            whites = whites xor moveMask
        } else {
            blacks = blacks xor moveMask
        }

        // Tests to disable castling
        if (flags and FLAG_WHITE_KINGSIDE_CASTLING != 0L && //
                (turn && pieceMoved == Piece.KING || from == castlingRooks[0] || to == castlingRooks[0])) {
            flags = flags and FLAG_WHITE_KINGSIDE_CASTLING.inv()
            key[0] = key[0] xor ZobristKey.whiteKingSideCastling
        }
        if (flags and FLAG_WHITE_QUEENSIDE_CASTLING != 0L && //
                (turn && pieceMoved == Piece.KING || from == castlingRooks[1] || to == castlingRooks[1])) {
            flags = flags and FLAG_WHITE_QUEENSIDE_CASTLING.inv()
            key[0] = key[0] xor ZobristKey.whiteQueenSideCastling
        }
        if (flags and FLAG_BLACK_KINGSIDE_CASTLING != 0L && //
                (!turn && pieceMoved == Piece.KING || from == castlingRooks[2] || to == castlingRooks[2])) {
            flags = flags and FLAG_BLACK_KINGSIDE_CASTLING.inv()
            key[1] = key[1] xor ZobristKey.blackKingSideCastling
        }
        if (flags and FLAG_BLACK_QUEENSIDE_CASTLING != 0L && //
                (!turn && pieceMoved == Piece.KING || from == castlingRooks[3] || to == castlingRooks[3])) {
            flags = flags and FLAG_BLACK_QUEENSIDE_CASTLING.inv()
            key[1] = key[1] xor ZobristKey.blackQueenSideCastling
        }
        // Change turn
        flags = flags xor FLAG_TURN
        key[0] = key[0] xor ZobristKey.whiteMove

        if (verifyCheck) {
            if (isValid) {
                setCheckFlags()

                if (fillSanInfo) {
                    if (isMate) { // Append # when mate
                        movesSan.put(moveNumber - 1, movesSan[moveNumber - 1]!!.replace("+", "#"))
                    }
                }
            } else {
                undoMove()
                return false
            }
        } else {
            // Trust move check flag
            if (Move.isCheck(move)) {
                flags = flags or FLAG_CHECK
            } else {
                flags = flags and FLAG_CHECK.inv()
            }
        }
        return true
    }

    /**
     * It checks if a state is valid basically, if the other king is not in check
     */
    private val isValid: Boolean
        get() = !bbAttacks.isSquareAttacked(this, kings and others, !turn)

    /**
     * Sets check flag if the own king is in check
     */
    private fun setCheckFlags() {
        if (bbAttacks.isSquareAttacked(this, kings and mines, turn)) {
            flags = flags or FLAG_CHECK
        } else {
            flags = flags and FLAG_CHECK.inv()
        }
    }

    fun undoMove() {
        undoMove(moveNumber - 1)
    }

    fun undoMove(moveNumber: Int) {
        if (moveNumber < 0 || moveNumber < initialMoveNumber) {
            return
        }
        this.moveNumber = moveNumber

        whites = whitesHistory[moveNumber]
        blacks = blacksHistory[moveNumber]
        pawns = pawnsHistory[moveNumber]
        knights = knightsHistory[moveNumber]
        bishops = bishopsHistory[moveNumber]
        rooks = rooksHistory[moveNumber]
        queens = queensHistory[moveNumber]
        kings = kingsHistory[moveNumber]
        flags = flagsHistory[moveNumber]
        key[0] = keyHistory[moveNumber][0]
        key[1] = keyHistory[moveNumber][1]
        fiftyMovesRule = fiftyMovesRuleHistory[moveNumber]
    }

    /**
     * 0 no, 1 whites won, -1 blacks won, 99 draw
     */
    val isEndGame: Int
        get() {
            var endGame = 0
            generateLegalMoves()
            if (legalMoveCount == 0) {
                if (check) {
                    endGame = if (turn) -1 else 1
                } else {
                    endGame = 99
                }
            } else if (isDraw) {
                endGame = 99
            }
            return endGame
        }

    val isMate: Boolean
        get() {
            val endgameState = isEndGame
            return endgameState == 1 || endgameState == -1
        }

    /**
     * checks draw by fifty move rule and threefold repetition
     */
    // with the last one they are 3
    // Draw by no material to mate by FIDE rules
    // https://en.wikipedia.org/wiki/Rules_of_chess#Draws
    // Kk, KNk, KNNk (KNnk IS NOT a draw), KBk, KBbk (with bishops in the same color)
    // KNNk, check same color
    val isDraw: Boolean
        get() {
            if (fiftyMovesRule >= 100) {
                return true
            }
            var repetitions = 0
            for (i in 0..moveNumber - 1 - 1) {
                if (keyHistory[i][0] == key[0] && keyHistory[i][1] == key[1]) {
                    repetitions++
                }
                if (repetitions >= 2) {
                    return true
                }
            }
            return pawns == 0L && rooks == 0L && queens == 0L && (bishops == 0L && knights == 0L
                    || knights == 0L && BitboardUtils.popCount(bishops) == 1
                    || bishops == 0L && (BitboardUtils.popCount(knights) == 1 || BitboardUtils.popCount(knights) == 2 && (BitboardUtils.popCount(knights and whites) == 2 || BitboardUtils.popCount(knights and whites.inv()) == 2))
                    || knights == 0L
                    && BitboardUtils.popCount(bishops and whites) == 1
                    && BitboardUtils.popCount(bishops and whites.inv()) == 1
                    && BitboardUtils.getSameColorSquares(bishops and whites) and bishops and whites.inv() != 0L)
        }

    fun see(move: Int): Int {
        return see(Move.getFromIndex(move), Move.getToIndex(move), Move.getPieceMoved(move), if (Move.isCapture(move)) Move.getPieceCaptured(this, move) else 0)
    }

    fun see(move: Int, ai: AttacksInfo): Int {
        val them = if (turn) 1 else 0
        if (ai.boardKey == getKey()
                && ai.attackedSquares[them] and Move.getToSquare(move) == 0L
                && ai.mayPin[them] and Move.getFromSquare(move) == 0L) {
            return if (Move.isCapture(move)) Board.SEE_PIECE_VALUES[Move.getPieceCaptured(this, move)] else 0
        } else {
            return see(move)
        }
    }

    /**
     * The SWAP algorithm https://chessprogramming.wikispaces.com/SEE+-+The+Swap+Algorithm
     */
    fun see(fromIndex: Int, toIndex: Int, pieceMovedIn: Int, targetPiece: Int): Int {
        var pieceMoved = pieceMovedIn
        var d = 0
        val mayXray = pawns or bishops or rooks or queens // not kings nor knights
        var fromSquare = 0x1L shl fromIndex
        var all = all
        var attacks = bbAttacks.getIndexAttacks(this, toIndex)
        var fromCandidates: Long

        seeGain[d] = SEE_PIECE_VALUES[targetPiece]
        do {
            val side = if (d and 1 == 0) others else mines
            d++ // next depth and side speculative store, if defended
            seeGain[d] = SEE_PIECE_VALUES[pieceMoved] - seeGain[d - 1]
            attacks = attacks xor fromSquare // reset bit in set to traverse
            all = all xor fromSquare // reset bit in temporary occupancy (for X-Rays)
            if (fromSquare and mayXray != 0L) {
                attacks = attacks or bbAttacks.getXrayAttacks(this, toIndex, all)
            }

            // Gets the next attacker
            fromCandidates = 0L
            if ((attacks and pawns and side) != 0L) {
                fromCandidates = attacks and pawns and side
                pieceMoved = Piece.PAWN
            } else if ((attacks and knights and side) != 0L) {
                fromCandidates = attacks and knights and side
                pieceMoved = Piece.KNIGHT
            } else if ((attacks and bishops and side) != 0L) {
                fromCandidates = attacks and bishops and side
                pieceMoved = Piece.BISHOP
            } else if ((attacks and rooks and side) != 0L) {
                fromCandidates = attacks and rooks and side
                pieceMoved = Piece.ROOK
            } else if ((attacks and queens and side) != 0L) {
                fromCandidates = attacks and queens and side
                pieceMoved = Piece.QUEEN
            } else if ((attacks and kings and side) != 0L) {
                fromCandidates = attacks and kings and side
                pieceMoved = Piece.KING
            }
            fromSquare = BitboardUtils.lsb(fromCandidates)
        } while (fromSquare != 0L)

        while (--d != 0) {
            seeGain[d - 1] = -max(-seeGain[d - 1], seeGain[d])
        }
        return seeGain[0]
    }

    val isUsingBook: Boolean
        get() = outBookMove > moveNumber

    /**
     * Check if a passed pawn is in the index, useful to trigger extensions
     */
    fun isPassedPawn(index: Int): Boolean {
        val rank = index shr 3
        val file = 7 - index and 7
        val square = 0x1L shl index

        if (whites and square != 0L) {
            return BitboardUtils.FILE[file] or BitboardUtils.FILES_ADJACENT[file] and BitboardUtils.RANKS_UPWARDS[rank] and pawns and blacks == 0L
        } else if (blacks and square != 0L) {
            return BitboardUtils.FILE[file] or BitboardUtils.FILES_ADJACENT[file] and BitboardUtils.RANKS_DOWNWARDS[rank] and pawns and whites == 0L
        }
        return false
    }

    /**
     * Generates legal moves for the position when not already generated
     */
    internal fun generateLegalMoves() {
        if (key[0] != legalMovesKey[0] || key[1] != legalMovesKey[1]) {
            legalMoveCount = legalMoveGenerator.generateMoves(this, legalMoves, 0)
            legalMovesKey[0] = key[0]
            legalMovesKey[1] = key[1]
        }
    }

    fun getLegalMoves(moves: IntArray): Int {
        generateLegalMoves()
        Utils.instance.arrayCopy(legalMoves, 0, moves, 0, if (legalMoveCount != -1) legalMoveCount else 0)
        return legalMoveCount
    }

    /**
     * Returns the move with the check flag set if the move is a legal move.
     * Ignores the check flag in the original move.
     * Returns Move.NONE if the move is not legal
     */
    fun getLegalMove(move: Int): Int {
        generateLegalMoves()
        for (i in 0..legalMoveCount - 1) {
            if (move and Move.CHECK_MASK.inv() == legalMoves[i] and Move.CHECK_MASK.inv()) {
                return legalMoves[i]
            }
        }
        return Move.NONE
    }

    fun getSanMove(moveNumber: Int): String {
        return movesSan[moveNumber]!!
    }

    fun getMoveTurn(moveNumber: Int): Boolean {
        return flagsHistory[moveNumber] and FLAG_TURN == 0L
    }

    val moves: String
        get() {
            val oSB = StringBuilder()
            for (i in initialMoveNumber..moveNumber - 1) {
                if (oSB.isNotEmpty()) {
                    oSB.append(" ")
                }
                oSB.append(Move.toString(moveHistory[i]))
            }
            return oSB.toString()
        }

    fun getMovesSan(): String {
        val oSB = StringBuilder()
        for (i in initialMoveNumber..moveNumber - 1) {
            if (oSB.isNotEmpty()) {
                oSB.append(" ")
            }
            oSB.append(movesSan[i])
        }
        return oSB.toString()
    }

    fun toSanNextMoves(moves: String?): String {
        if (moves == null || "" == moves.trim { it <= ' ' }) {
            return ""
        }

        val oSB = StringBuilder()
        val movesArray = moves.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        val savedMoveNumber = moveNumber

        for (moveString in movesArray) {
            val move = Move.getFromString(this, moveString, true)

            if (!doMove(move)) {
                undoMove(savedMoveNumber)
                return ""
            }

            if (oSB.isNotEmpty()) {
                oSB.append(" ")
            }
            oSB.append(lastMoveSan)
        }
        undoMove(savedMoveNumber)
        return oSB.toString()
    }

    val lastMove: Int
        get() {
            if (moveNumber == 0) {
                return Move.NONE
            }
            return moveHistory[moveNumber - 1]
        }

    val lastMoveSan: String?
        get() {
            if (moveNumber == 0) {
                return null
            }
            return movesSan[moveNumber - 1]
        }

    /**
     * Convenience method to apply all the moves in a string separated by spaces
     */
    fun doMoves(moves: String?) {
        if (moves == null || "" == moves.trim { it <= ' ' }) {
            return
        }
        val movesArray = moves.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        for (moveString in movesArray) {
            val move = Move.getFromString(this, moveString, true)
            doMove(move)
        }
    }

    companion object {
        val MAX_MOVES = 1024
        val FEN_START_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val CHESS960_START_POSITIONS = arrayOf("QNNRKR", "NQNRKR", "NNQRKR", "NNRQKR", "NNRKQR", "NNRKRQ", "QNRNKR", "NQRNKR", "NRQNKR", "NRNQKR", "NRNKQR", "NRNKRQ", "QNRKNR", "NQRKNR", "NRQKNR", "NRKQNR", "NRKNQR", "NRKNRQ", "QNRKRN", "NQRKRN", "NRQKRN", "NRKQRN", "NRKRQN", "NRKRNQ", "QRNNKR", "RQNNKR", "RNQNKR", "RNNQKR", "RNNKQR", "RNNKRQ", "QRNKNR", "RQNKNR", "RNQKNR", "RNKQNR", "RNKNQR", "RNKNRQ", "QRNKRN", "RQNKRN", "RNQKRN", "RNKQRN", "RNKRQN", "RNKRNQ", "QRKNNR", "RQKNNR", "RKQNNR", "RKNQNR", "RKNNQR", "RKNNRQ", "QRKNRN", "RQKNRN", "RKQNRN", "RKNQRN", "RKNRQN", "RKNRNQ", "QRKRNN", "RQKRNN", "RKQRNN", "RKRQNN", "RKRNQN", "RKRNNQ")
        val CHESS960_START_POSITIONS_BISHOPS = arrayOf("BB------", "B--B----", "B----B--", "B------B", "-BB-----", "--BB----", "--B--B--", "--B----B", "-B--B---", "---BB---", "----BB--", "----B--B", "-B----B-", "---B--B-", "-----BB-", "------BB")

        // Flags: must be changed only when moving
        private val FLAG_TURN = 0x0001L
        private val FLAG_WHITE_KINGSIDE_CASTLING = 0x0002L
        private val FLAG_WHITE_QUEENSIDE_CASTLING = 0x0004L
        private val FLAG_BLACK_KINGSIDE_CASTLING = 0x0008L
        private val FLAG_BLACK_QUEENSIDE_CASTLING = 0x0010L
        private val FLAG_CHECK = 0x0020L
        // Position on boarch in which is captured
        private val FLAGS_PASSANT = 0x0000ff0000ff0000L

        // For the castlings {White Kingside, White Queenside, Black Kingside, Black Queenside}
        val CASTLING_KING_DESTINY_INDEX = intArrayOf(1, 5, 57, 61)
        val CASTLING_KING_DESTINY_SQUARE = longArrayOf(1L shl 1, 1L shl 5, 1L shl 57, 1L shl 61)
        val CASTLING_ROOK_DESTINY_INDEX = intArrayOf(2, 4, 58, 60)
        val CASTLING_ROOK_DESTINY_SQUARE = longArrayOf(1L shl 2, 1L shl 4, 1L shl 58, 1L shl 60)

        // For the SEE SWAP algorithm
        val SEE_PIECE_VALUES = intArrayOf(0, 100, 325, 330, 500, 900, 9999)
    }

}