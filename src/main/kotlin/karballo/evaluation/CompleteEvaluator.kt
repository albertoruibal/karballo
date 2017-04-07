package karballo.evaluation

import karballo.Board
import karballo.Piece
import karballo.Square
import karballo.bitboard.AttacksInfo
import karballo.bitboard.BitboardUtils
import karballo.log.Logger

class CompleteEvaluator : Evaluator() {
    private val logger = Logger.getLogger("CompleteEvaluator")

    // Mobility units: this value is added for the number of destination square not occupied by one of our pieces or attacked by opposite pawns
    private val MOBILITY = arrayOf(intArrayOf(), intArrayOf(), intArrayOf(Evaluator.Companion.oe(-12, -16), Evaluator.Companion.oe(2, 2), Evaluator.Companion.oe(5, 7), Evaluator.Companion.oe(7, 9), Evaluator.Companion.oe(8, 11), Evaluator.Companion.oe(10, 13), Evaluator.Companion.oe(11, 14), Evaluator.Companion.oe(11, 15), Evaluator.Companion.oe(12, 16)), intArrayOf(Evaluator.Companion.oe(-16, -16), Evaluator.Companion.oe(-1, -1), Evaluator.Companion.oe(3, 3), Evaluator.Companion.oe(6, 6), Evaluator.Companion.oe(8, 8), Evaluator.Companion.oe(9, 9), Evaluator.Companion.oe(11, 11), Evaluator.Companion.oe(12, 12), Evaluator.Companion.oe(13, 13), Evaluator.Companion.oe(13, 13), Evaluator.Companion.oe(14, 14), Evaluator.Companion.oe(15, 15), Evaluator.Companion.oe(15, 15), Evaluator.Companion.oe(16, 16)), intArrayOf(Evaluator.Companion.oe(-14, -21), Evaluator.Companion.oe(-1, -2), Evaluator.Companion.oe(3, 4), Evaluator.Companion.oe(5, 7), Evaluator.Companion.oe(7, 10), Evaluator.Companion.oe(8, 12), Evaluator.Companion.oe(9, 13), Evaluator.Companion.oe(10, 15), Evaluator.Companion.oe(11, 16), Evaluator.Companion.oe(11, 17), Evaluator.Companion.oe(12, 18), Evaluator.Companion.oe(13, 19), Evaluator.Companion.oe(13, 20), Evaluator.Companion.oe(14, 20), Evaluator.Companion.oe(14, 21)), intArrayOf(Evaluator.Companion.oe(-27, -27), Evaluator.Companion.oe(-9, -9), Evaluator.Companion.oe(-2, -2), Evaluator.Companion.oe(2, 2), Evaluator.Companion.oe(5, 5), Evaluator.Companion.oe(8, 8), Evaluator.Companion.oe(10, 10), Evaluator.Companion.oe(12, 12), Evaluator.Companion.oe(13, 13), Evaluator.Companion.oe(14, 14), Evaluator.Companion.oe(16, 16), Evaluator.Companion.oe(17, 17), Evaluator.Companion.oe(18, 18), Evaluator.Companion.oe(19, 19), Evaluator.Companion.oe(19, 19), Evaluator.Companion.oe(20, 20), Evaluator.Companion.oe(21, 21), Evaluator.Companion.oe(22, 22), Evaluator.Companion.oe(22, 22), Evaluator.Companion.oe(23, 23), Evaluator.Companion.oe(24, 24), Evaluator.Companion.oe(24, 24), Evaluator.Companion.oe(25, 25), Evaluator.Companion.oe(25, 25), Evaluator.Companion.oe(26, 26), Evaluator.Companion.oe(26, 26), Evaluator.Companion.oe(27, 27), Evaluator.Companion.oe(27, 27)))

    // Space
    private val WHITE_SPACE_ZONE = BitboardUtils.C or BitboardUtils.D or BitboardUtils.E or BitboardUtils.F and (BitboardUtils.R2 or BitboardUtils.R3 or BitboardUtils.R4)
    private val BLACK_SPACE_ZONE = BitboardUtils.C or BitboardUtils.D or BitboardUtils.E or BitboardUtils.F and (BitboardUtils.R5 or BitboardUtils.R6 or BitboardUtils.R7)
    private val SPACE = Evaluator.Companion.oe(2, 0)

    // Attacks
    private val PAWN_ATTACKS = intArrayOf(0, 0, Evaluator.Companion.oe(11, 15), Evaluator.Companion.oe(12, 16), Evaluator.Companion.oe(17, 23), Evaluator.Companion.oe(19, 25), 0)
    private val MINOR_ATTACKS = intArrayOf(0, Evaluator.Companion.oe(3, 5), Evaluator.Companion.oe(7, 9), Evaluator.Companion.oe(7, 9), Evaluator.Companion.oe(10, 14), Evaluator.Companion.oe(11, 15), 0) // Minor piece attacks to pawn undefended pieces
    private val MAJOR_ATTACKS = intArrayOf(0, Evaluator.Companion.oe(2, 2), Evaluator.Companion.oe(3, 4), Evaluator.Companion.oe(3, 4), Evaluator.Companion.oe(5, 6), Evaluator.Companion.oe(5, 7), 0) // Major piece attacks to pawn undefended pieces

    private val HUNG_PIECES = Evaluator.Companion.oe(16, 25) // Two or more pieces of the other side attacked by inferior pieces
    private val PINNED_PIECE = Evaluator.Companion.oe(7, 15)

    // Pawns
    // Those are all penalties. Array is {not opposed, opposed}: If not opposed, backwards and isolated pawns can be easily attacked
    private val PAWN_BACKWARDS = intArrayOf(Evaluator.Companion.oe(20, 15), Evaluator.Companion.oe(10, 15)) // Not opposed is worse in the opening
    private val PAWN_ISOLATED = intArrayOf(Evaluator.Companion.oe(20, 20), Evaluator.Companion.oe(10, 20)) // Not opposed is worse in the opening
    private val PAWN_DOUBLED = intArrayOf(Evaluator.Companion.oe(8, 16), Evaluator.Companion.oe(10, 20)) // Not opposed is better, opening is better
    private val PAWN_UNSUPPORTED = Evaluator.Companion.oe(2, 4) // Not backwards or isolated

    // And now the bonuses. Array by relative rank
    private val PAWN_CANDIDATE = intArrayOf(0, Evaluator.Companion.oe(10, 13), Evaluator.Companion.oe(10, 13), Evaluator.Companion.oe(14, 18), Evaluator.Companion.oe(22, 28), Evaluator.Companion.oe(34, 43), Evaluator.Companion.oe(50, 63), 0)
    private val PAWN_PASSER = intArrayOf(0, Evaluator.Companion.oe(20, 25), Evaluator.Companion.oe(20, 25), Evaluator.Companion.oe(28, 35), Evaluator.Companion.oe(44, 55), Evaluator.Companion.oe(68, 85), Evaluator.Companion.oe(100, 125), 0)
    private val PAWN_PASSER_OUTSIDE = intArrayOf(0, 0, 0, Evaluator.Companion.oe(2, 3), Evaluator.Companion.oe(7, 9), Evaluator.Companion.oe(14, 18), Evaluator.Companion.oe(24, 30), 0)
    private val PAWN_PASSER_CONNECTED = intArrayOf(0, 0, 0, Evaluator.Companion.oe(3, 3), Evaluator.Companion.oe(8, 8), Evaluator.Companion.oe(15, 15), Evaluator.Companion.oe(25, 25), 0)
    private val PAWN_PASSER_SUPPORTED = intArrayOf(0, 0, 0, Evaluator.Companion.oe(6, 6), Evaluator.Companion.oe(17, 17), Evaluator.Companion.oe(33, 33), Evaluator.Companion.oe(55, 55), 0)
    private val PAWN_PASSER_MOBILE = intArrayOf(0, 0, 0, Evaluator.Companion.oe(2, 2), Evaluator.Companion.oe(6, 6), Evaluator.Companion.oe(12, 12), Evaluator.Companion.oe(20, 20), 0)
    private val PAWN_PASSER_RUNNER = intArrayOf(0, 0, 0, Evaluator.Companion.oe(6, 6), Evaluator.Companion.oe(18, 18), Evaluator.Companion.oe(36, 36), Evaluator.Companion.oe(60, 60), 0)

    private val PAWN_PASSER_OTHER_KING_DISTANCE = intArrayOf(0, 0, 0, Evaluator.Companion.oe(0, 2), Evaluator.Companion.oe(0, 6), Evaluator.Companion.oe(0, 12), Evaluator.Companion.oe(0, 20), 0)
    private val PAWN_PASSER_MY_KING_DISTANCE = intArrayOf(0, 0, 0, Evaluator.Companion.oe(0, 1), Evaluator.Companion.oe(0, 3), Evaluator.Companion.oe(0, 6), Evaluator.Companion.oe(0, 10), 0)

    private val PAWN_SHIELD_CENTER = intArrayOf(0, Evaluator.Companion.oe(55, 0), Evaluator.Companion.oe(41, 0), Evaluator.Companion.oe(28, 0), Evaluator.Companion.oe(14, 0), 0, 0, 0)
    private val PAWN_SHIELD = intArrayOf(0, Evaluator.Companion.oe(35, 0), Evaluator.Companion.oe(26, 0), Evaluator.Companion.oe(18, 0), Evaluator.Companion.oe(9, 0), 0, 0, 0)
    private val PAWN_STORM_CENTER = intArrayOf(0, 0, 0, Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(15, 0), Evaluator.Companion.oe(30, 0), 0, 0)
    private val PAWN_STORM = intArrayOf(0, 0, 0, Evaluator.Companion.oe(5, 0), Evaluator.Companion.oe(10, 0), Evaluator.Companion.oe(20, 0), 0, 0)

    private val PAWN_BLOCKADE = Evaluator.Companion.oe(5, 0) // Penalty for pawns in [D,E] in the initial square blocked by our own pieces

    // Knights
    private val KNIGHT_OUTPOST = intArrayOf(Evaluator.Companion.oe(15, 10), Evaluator.Companion.oe(22, 15)) // Array is Not defended by pawn, defended by pawn

    // Bishops
    private val BISHOP_OUTPOST = intArrayOf(Evaluator.Companion.oe(7, 4), Evaluator.Companion.oe(10, 7))
    private val BISHOP_MY_PAWNS_IN_COLOR_PENALTY = Evaluator.Companion.oe(2, 4) // Penalty for each of my pawns in the bishop color (Capablanca rule)
    private val BISHOP_TRAPPED_PENALTY = intArrayOf(Evaluator.Companion.oe(40, 40), Evaluator.Companion.oe(80, 80)) // By pawn not guarded / guarded
    private val BISHOP_TRAPPING = longArrayOf(// Indexed by bishop position, contains the square where a pawn can trap the bishop
            0, Square.F2, 0, 0, 0, 0, Square.C2, 0, Square.G3, 0, 0, 0, 0, 0, 0, Square.B3, Square.G4, 0, 0, 0, 0, 0, 0, Square.B4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Square.G5, 0, 0, 0, 0, 0, 0, Square.B5, Square.G6, 0, 0, 0, 0, 0, 0, Square.B6, 0, Square.F7, 0, 0, 0, 0, Square.C7, 0)
    private val BISHOP_TRAPPING_GUARD = longArrayOf(// Indexed by bishop position, contains the square where other pawn defends the trapping pawn
            0, 0, 0, 0, 0, 0, 0, 0, Square.F2, 0, 0, 0, 0, 0, 0, Square.C2, Square.F3, 0, 0, 0, 0, 0, 0, Square.C3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Square.F6, 0, 0, 0, 0, 0, 0, Square.C6, Square.F7, 0, 0, 0, 0, 0, 0, Square.C7, 0, 0, 0, 0, 0, 0, 0, 0)

    // Rooks
    private val ROOK_OUTPOST = intArrayOf(Evaluator.Companion.oe(2, 1), Evaluator.Companion.oe(3, 2)) // Array is Not defended by pawn, defended by pawn
    private val ROOK_FILE = intArrayOf(Evaluator.Companion.oe(15, 10), Evaluator.Companion.oe(7, 5)) // Open / Semi open
    private val ROOK_7 = Evaluator.Companion.oe(7, 10) // Rook 5, 6 or 7th rank attacking a pawn in the same rank not defended by pawn
    private val ROOK_TRAPPED_PENALTY = intArrayOf(Evaluator.Companion.oe(40, 0), Evaluator.Companion.oe(30, 0), Evaluator.Companion.oe(20, 0), Evaluator.Companion.oe(10, 0)) // Penalty by number of mobility squares
    private val ROOK_TRAPPING = longArrayOf(// Indexed by own king position, contains the squares where a rook may be traped by the king
            0, Square.H1 or Square.H2, Square.H1 or Square.H2 or Square.G1 or Square.G2, 0, 0, Square.A1 or Square.A2 or Square.B1 or Square.B2, Square.A1 or Square.A2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Square.H7 or Square.H8, Square.H7 or Square.H8 or Square.G7 or Square.G8, 0, 0, Square.A7 or Square.A8 or Square.B7 or Square.B8, Square.A7 or Square.A8, 0)

    // King
    // Sums for each piece attacking an square near the king
    private val PIECE_ATTACKS_KING = intArrayOf(0, 0, Evaluator.Companion.oe(30, 0), Evaluator.Companion.oe(20, 0), Evaluator.Companion.oe(40, 0), Evaluator.Companion.oe(80, 0))
    // Ponder kings attacks by the number of attackers (not pawns)
    private val KING_SAFETY_PONDER = intArrayOf(0, 0, 32, 48, 56, 60, 62, 63, 64, 64, 64, 64, 64, 64, 64, 64)


    private val OUTPOST_MASK = longArrayOf(0x00007e7e7e000000L, 0x0000007e7e7e0000L)

    private val pawnPcsq = intArrayOf(Evaluator.Companion.oe(-18, 4), Evaluator.Companion.oe(-6, 2), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(6, -2), Evaluator.Companion.oe(6, -2), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-6, 2), Evaluator.Companion.oe(-18, 4), Evaluator.Companion.oe(-21, 1), Evaluator.Companion.oe(-9, -1), Evaluator.Companion.oe(-3, -3), Evaluator.Companion.oe(3, -5), Evaluator.Companion.oe(3, -5), Evaluator.Companion.oe(-3, -3), Evaluator.Companion.oe(-9, -1), Evaluator.Companion.oe(-21, 1), Evaluator.Companion.oe(-20, 1), Evaluator.Companion.oe(-8, -1), Evaluator.Companion.oe(-2, -3), Evaluator.Companion.oe(4, -5), Evaluator.Companion.oe(4, -5), Evaluator.Companion.oe(-2, -3), Evaluator.Companion.oe(-8, -1), Evaluator.Companion.oe(-20, 1), Evaluator.Companion.oe(-19, 2), Evaluator.Companion.oe(-7, 0), Evaluator.Companion.oe(-1, -2), Evaluator.Companion.oe(12, -4), Evaluator.Companion.oe(12, -4), Evaluator.Companion.oe(-1, -2), Evaluator.Companion.oe(-7, 0), Evaluator.Companion.oe(-19, 2), Evaluator.Companion.oe(-17, 3), Evaluator.Companion.oe(-5, 1), Evaluator.Companion.oe(1, -1), Evaluator.Companion.oe(10, -3), Evaluator.Companion.oe(10, -3), Evaluator.Companion.oe(1, -1), Evaluator.Companion.oe(-5, 1), Evaluator.Companion.oe(-17, 3), Evaluator.Companion.oe(-16, 4), Evaluator.Companion.oe(-4, 2), Evaluator.Companion.oe(2, 0), Evaluator.Companion.oe(8, -2), Evaluator.Companion.oe(8, -2), Evaluator.Companion.oe(2, 0), Evaluator.Companion.oe(-4, 2), Evaluator.Companion.oe(-16, 4), Evaluator.Companion.oe(-15, 6), Evaluator.Companion.oe(-3, 4), Evaluator.Companion.oe(3, 2), Evaluator.Companion.oe(9, 0), Evaluator.Companion.oe(9, 0), Evaluator.Companion.oe(3, 2), Evaluator.Companion.oe(-3, 4), Evaluator.Companion.oe(-15, 6), Evaluator.Companion.oe(-18, 4), Evaluator.Companion.oe(-6, 2), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(6, -2), Evaluator.Companion.oe(6, -2), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-6, 2), Evaluator.Companion.oe(-18, 4))
    private val knightPcsq = intArrayOf(Evaluator.Companion.oe(-27, -22), Evaluator.Companion.oe(-17, -17), Evaluator.Companion.oe(-9, -12), Evaluator.Companion.oe(-4, -9), Evaluator.Companion.oe(-4, -9), Evaluator.Companion.oe(-9, -12), Evaluator.Companion.oe(-17, -17), Evaluator.Companion.oe(-27, -22), Evaluator.Companion.oe(-21, -15), Evaluator.Companion.oe(-11, -8), Evaluator.Companion.oe(-3, -4), Evaluator.Companion.oe(2, -2), Evaluator.Companion.oe(2, -2), Evaluator.Companion.oe(-3, -4), Evaluator.Companion.oe(-11, -8), Evaluator.Companion.oe(-21, -15), Evaluator.Companion.oe(-15, -10), Evaluator.Companion.oe(-5, -4), Evaluator.Companion.oe(3, 1), Evaluator.Companion.oe(8, 3), Evaluator.Companion.oe(8, 3), Evaluator.Companion.oe(3, 1), Evaluator.Companion.oe(-5, -4), Evaluator.Companion.oe(-15, -10), Evaluator.Companion.oe(-9, -6), Evaluator.Companion.oe(1, -1), Evaluator.Companion.oe(9, 4), Evaluator.Companion.oe(14, 8), Evaluator.Companion.oe(14, 8), Evaluator.Companion.oe(9, 4), Evaluator.Companion.oe(1, -1), Evaluator.Companion.oe(-9, -6), Evaluator.Companion.oe(-5, -4), Evaluator.Companion.oe(5, 1), Evaluator.Companion.oe(13, 6), Evaluator.Companion.oe(18, 10), Evaluator.Companion.oe(18, 10), Evaluator.Companion.oe(13, 6), Evaluator.Companion.oe(5, 1), Evaluator.Companion.oe(-5, -4), Evaluator.Companion.oe(-6, -4), Evaluator.Companion.oe(4, 2), Evaluator.Companion.oe(12, 7), Evaluator.Companion.oe(17, 9), Evaluator.Companion.oe(17, 9), Evaluator.Companion.oe(12, 7), Evaluator.Companion.oe(4, 2), Evaluator.Companion.oe(-6, -4), Evaluator.Companion.oe(-10, -8), Evaluator.Companion.oe(0, -1), Evaluator.Companion.oe(8, 3), Evaluator.Companion.oe(13, 5), Evaluator.Companion.oe(13, 5), Evaluator.Companion.oe(8, 3), Evaluator.Companion.oe(0, -1), Evaluator.Companion.oe(-10, -8), Evaluator.Companion.oe(-20, -15), Evaluator.Companion.oe(-10, -10), Evaluator.Companion.oe(-2, -5), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(-2, -5), Evaluator.Companion.oe(-10, -10), Evaluator.Companion.oe(-20, -15))
    private val bishopPcsq = intArrayOf(Evaluator.Companion.oe(-7, 0), Evaluator.Companion.oe(-8, -1), Evaluator.Companion.oe(-11, -2), Evaluator.Companion.oe(-13, -2), Evaluator.Companion.oe(-13, -2), Evaluator.Companion.oe(-11, -2), Evaluator.Companion.oe(-8, -1), Evaluator.Companion.oe(-7, 0), Evaluator.Companion.oe(-3, -1), Evaluator.Companion.oe(3, 1), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(3, 1), Evaluator.Companion.oe(-3, -1), Evaluator.Companion.oe(-6, -2), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(7, 3), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(7, 3), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-6, -2), Evaluator.Companion.oe(-8, -2), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(15, 5), Evaluator.Companion.oe(15, 5), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(-8, -2), Evaluator.Companion.oe(-8, -2), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(15, 5), Evaluator.Companion.oe(15, 5), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(-8, -2), Evaluator.Companion.oe(-6, -2), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(7, 3), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(7, 3), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-6, -2), Evaluator.Companion.oe(-3, -1), Evaluator.Companion.oe(3, 1), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(3, 1), Evaluator.Companion.oe(-3, -1), Evaluator.Companion.oe(-2, 0), Evaluator.Companion.oe(-3, -1), Evaluator.Companion.oe(-6, -2), Evaluator.Companion.oe(-8, -2), Evaluator.Companion.oe(-8, -2), Evaluator.Companion.oe(-6, -2), Evaluator.Companion.oe(-3, -1), Evaluator.Companion.oe(-2, 0))
    private val rookPcsq = intArrayOf(Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(8, 0), Evaluator.Companion.oe(4, 0), Evaluator.Companion.oe(0, 0), Evaluator.Companion.oe(-4, 0), Evaluator.Companion.oe(-4, 1), Evaluator.Companion.oe(0, 1), Evaluator.Companion.oe(4, 1), Evaluator.Companion.oe(8, 1), Evaluator.Companion.oe(8, 1), Evaluator.Companion.oe(4, 1), Evaluator.Companion.oe(0, 1), Evaluator.Companion.oe(-4, 1), Evaluator.Companion.oe(-4, 3), Evaluator.Companion.oe(0, 3), Evaluator.Companion.oe(4, 3), Evaluator.Companion.oe(8, 3), Evaluator.Companion.oe(8, 3), Evaluator.Companion.oe(4, 3), Evaluator.Companion.oe(0, 3), Evaluator.Companion.oe(-4, 3), Evaluator.Companion.oe(-4, 5), Evaluator.Companion.oe(0, 5), Evaluator.Companion.oe(4, 5), Evaluator.Companion.oe(8, 5), Evaluator.Companion.oe(8, 5), Evaluator.Companion.oe(4, 5), Evaluator.Companion.oe(0, 5), Evaluator.Companion.oe(-4, 5), Evaluator.Companion.oe(-4, -2), Evaluator.Companion.oe(0, -2), Evaluator.Companion.oe(4, -2), Evaluator.Companion.oe(8, -2), Evaluator.Companion.oe(8, -2), Evaluator.Companion.oe(4, -2), Evaluator.Companion.oe(0, -2), Evaluator.Companion.oe(-4, -2))
    private val queenPcsq = intArrayOf(Evaluator.Companion.oe(-9, -15), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-9, -15), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-1, -5), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(-1, -5), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(5, 0), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(5, 0), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(9, 5), Evaluator.Companion.oe(9, 5), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(9, 5), Evaluator.Companion.oe(9, 5), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(5, 0), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(6, 2), Evaluator.Companion.oe(5, 0), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-1, -5), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(3, -2), Evaluator.Companion.oe(1, -3), Evaluator.Companion.oe(-1, -5), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-9, -15), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(-2, -7), Evaluator.Companion.oe(-4, -8), Evaluator.Companion.oe(-6, -10), Evaluator.Companion.oe(-9, -15))
    private val kingPcsq = intArrayOf(Evaluator.Companion.oe(34, -58), Evaluator.Companion.oe(39, -35), Evaluator.Companion.oe(14, -19), Evaluator.Companion.oe(-6, -13), Evaluator.Companion.oe(-6, -13), Evaluator.Companion.oe(14, -19), Evaluator.Companion.oe(39, -35), Evaluator.Companion.oe(34, -58), Evaluator.Companion.oe(31, -35), Evaluator.Companion.oe(36, -10), Evaluator.Companion.oe(11, 2), Evaluator.Companion.oe(-9, 8), Evaluator.Companion.oe(-9, 8), Evaluator.Companion.oe(11, 2), Evaluator.Companion.oe(36, -10), Evaluator.Companion.oe(31, -35), Evaluator.Companion.oe(28, -19), Evaluator.Companion.oe(33, 2), Evaluator.Companion.oe(8, 17), Evaluator.Companion.oe(-12, 23), Evaluator.Companion.oe(-12, 23), Evaluator.Companion.oe(8, 17), Evaluator.Companion.oe(33, 2), Evaluator.Companion.oe(28, -19), Evaluator.Companion.oe(25, -13), Evaluator.Companion.oe(30, 8), Evaluator.Companion.oe(5, 23), Evaluator.Companion.oe(-15, 32), Evaluator.Companion.oe(-15, 32), Evaluator.Companion.oe(5, 23), Evaluator.Companion.oe(30, 8), Evaluator.Companion.oe(25, -13), Evaluator.Companion.oe(20, -13), Evaluator.Companion.oe(25, 8), Evaluator.Companion.oe(0, 23), Evaluator.Companion.oe(-20, 32), Evaluator.Companion.oe(-20, 32), Evaluator.Companion.oe(0, 23), Evaluator.Companion.oe(25, 8), Evaluator.Companion.oe(20, -13), Evaluator.Companion.oe(15, -19), Evaluator.Companion.oe(20, 2), Evaluator.Companion.oe(-5, 17), Evaluator.Companion.oe(-25, 23), Evaluator.Companion.oe(-25, 23), Evaluator.Companion.oe(-5, 17), Evaluator.Companion.oe(20, 2), Evaluator.Companion.oe(15, -19), Evaluator.Companion.oe(5, -35), Evaluator.Companion.oe(10, -10), Evaluator.Companion.oe(-15, 2), Evaluator.Companion.oe(-35, 8), Evaluator.Companion.oe(-35, 8), Evaluator.Companion.oe(-15, 2), Evaluator.Companion.oe(10, -10), Evaluator.Companion.oe(5, -35), Evaluator.Companion.oe(-5, -58), Evaluator.Companion.oe(0, -35), Evaluator.Companion.oe(-25, -19), Evaluator.Companion.oe(-45, -13), Evaluator.Companion.oe(-45, -13), Evaluator.Companion.oe(-25, -19), Evaluator.Companion.oe(0, -35), Evaluator.Companion.oe(-5, -58))

    var debug = false
    var debugPawns = false
    lateinit var debugSB: StringBuilder

    private val scaleFactor = intArrayOf(0)

    private val pawnMaterial = intArrayOf(0, 0)
    private val nonPawnMaterial = intArrayOf(0, 0)
    private val pcsq = intArrayOf(0, 0)
    private val space = intArrayOf(0, 0)
    private val positional = intArrayOf(0, 0)
    private val mobility = intArrayOf(0, 0)
    private val attacks = intArrayOf(0, 0)
    private val kingAttackersCount = intArrayOf(0, 0)
    private val kingSafety = intArrayOf(0, 0)
    private val pawnStructure = intArrayOf(0, 0)
    private val passedPawns = intArrayOf(0, 0)
    private val pawnCanAttack = longArrayOf(0, 0)
    private val mobilitySquares = longArrayOf(0, 0)
    private val kingZone = longArrayOf(0, 0) // Squares surrounding King

    override fun evaluate(b: Board, ai: AttacksInfo): Int {
        if (debug) {
            debugSB = StringBuilder()
            debugSB.append("\n")
            debugSB.append(b.toString())
            debugSB.append("\n")
        }

        val whitePawns = BitboardUtils.popCount(b.pawns and b.whites)
        val blackPawns = BitboardUtils.popCount(b.pawns and b.blacks)
        val whiteKnights = BitboardUtils.popCount(b.knights and b.whites)
        val blackKnights = BitboardUtils.popCount(b.knights and b.blacks)
        val whiteBishops = BitboardUtils.popCount(b.bishops and b.whites)
        val blackBishops = BitboardUtils.popCount(b.bishops and b.blacks)
        val whiteRooks = BitboardUtils.popCount(b.rooks and b.whites)
        val blackRooks = BitboardUtils.popCount(b.rooks and b.blacks)
        val whiteQueens = BitboardUtils.popCount(b.queens and b.whites)
        val blackQueens = BitboardUtils.popCount(b.queens and b.blacks)

        val endgameValue = Endgame.evaluateEndgame(b, scaleFactor, whitePawns, blackPawns, whiteKnights, blackKnights, whiteBishops, blackBishops, whiteRooks, blackRooks, whiteQueens, blackQueens)
        if (endgameValue != Evaluator.Companion.NO_VALUE) {
            return endgameValue
        }

        pawnMaterial[Evaluator.Companion.W] = whitePawns * Evaluator.Companion.PIECE_VALUES_OE[Piece.PAWN]
        nonPawnMaterial[Evaluator.Companion.W] = whiteKnights * Evaluator.Companion.PIECE_VALUES_OE[Piece.KNIGHT] +
                whiteBishops * Evaluator.Companion.PIECE_VALUES_OE[Piece.BISHOP] +
                whiteRooks * Evaluator.Companion.PIECE_VALUES_OE[Piece.ROOK] +
                whiteQueens * Evaluator.Companion.PIECE_VALUES_OE[Piece.QUEEN] +
                if (b.whites and b.bishops and Square.WHITES != 0L && b.whites and b.bishops and Square.BLACKS != 0L)
                    Evaluator.Companion.BISHOP_PAIR
                else
                    0
        pawnMaterial[Evaluator.Companion.B] = blackPawns * Evaluator.Companion.PIECE_VALUES_OE[Piece.PAWN]
        nonPawnMaterial[Evaluator.Companion.B] = blackKnights * Evaluator.Companion.PIECE_VALUES_OE[Piece.KNIGHT] +
                blackBishops * Evaluator.Companion.PIECE_VALUES_OE[Piece.BISHOP] +
                blackRooks * Evaluator.Companion.PIECE_VALUES_OE[Piece.ROOK] +
                blackQueens * Evaluator.Companion.PIECE_VALUES_OE[Piece.QUEEN] +
                if (b.blacks and b.bishops and Square.WHITES != 0L && b.blacks and b.bishops and Square.BLACKS != 0L)
                    Evaluator.Companion.BISHOP_PAIR
                else
                    0

        val nonPawnMat = Evaluator.Companion.e(nonPawnMaterial[Evaluator.Companion.W] + nonPawnMaterial[Evaluator.Companion.B])
        val gamePhase = if (nonPawnMat >= Evaluator.Companion.NON_PAWN_MATERIAL_MIDGAME_MAX)
            Evaluator.Companion.GAME_PHASE_MIDGAME
        else if (nonPawnMat <= Evaluator.Companion.NON_PAWN_MATERIAL_ENDGAME_MIN)
            Evaluator.Companion.GAME_PHASE_ENDGAME
        else
            (nonPawnMat - Evaluator.Companion.NON_PAWN_MATERIAL_ENDGAME_MIN) * Evaluator.Companion.GAME_PHASE_MIDGAME / (Evaluator.Companion.NON_PAWN_MATERIAL_MIDGAME_MAX - Evaluator.Companion.NON_PAWN_MATERIAL_ENDGAME_MIN)

        pcsq[Evaluator.Companion.W] = 0
        pcsq[Evaluator.Companion.B] = 0
        positional[Evaluator.Companion.W] = 0
        positional[Evaluator.Companion.B] = 0
        mobility[Evaluator.Companion.W] = 0
        mobility[Evaluator.Companion.B] = 0
        kingAttackersCount[Evaluator.Companion.W] = 0
        kingAttackersCount[Evaluator.Companion.B] = 0
        kingSafety[Evaluator.Companion.W] = 0
        kingSafety[Evaluator.Companion.B] = 0
        pawnStructure[Evaluator.Companion.W] = 0
        pawnStructure[Evaluator.Companion.B] = 0
        passedPawns[Evaluator.Companion.W] = 0
        passedPawns[Evaluator.Companion.B] = 0

        mobilitySquares[Evaluator.Companion.W] = b.whites.inv()
        mobilitySquares[Evaluator.Companion.B] = b.blacks.inv()

        ai.build(b)

        var whitePawnsAux = b.pawns and b.whites
        var blackPawnsAux = b.pawns and b.blacks

        // Space evaluation
        if (gamePhase > 0) {
            val whiteSafe = WHITE_SPACE_ZONE and ai.pawnAttacks[Evaluator.Companion.B].inv() and (ai.attackedSquares[Evaluator.Companion.B].inv() or ai.attackedSquares[Evaluator.Companion.W])
            val blackSafe = BLACK_SPACE_ZONE and ai.pawnAttacks[Evaluator.Companion.W].inv() and (ai.attackedSquares[Evaluator.Companion.W].inv() or ai.attackedSquares[Evaluator.Companion.B])

            val whiteBehindPawn = whitePawnsAux.ushr(8) or whitePawnsAux.ushr(16) or whitePawnsAux.ushr(24)
            val blackBehindPawn = blackPawnsAux shl 8 or (blackPawnsAux shl 16) or (blackPawnsAux shl 24)

            space[Evaluator.Companion.W] = SPACE * ((BitboardUtils.popCount(whiteSafe) + BitboardUtils.popCount(whiteSafe and whiteBehindPawn)) * (whiteKnights + whiteBishops) / 4)
            space[Evaluator.Companion.B] = SPACE * ((BitboardUtils.popCount(blackSafe) + BitboardUtils.popCount(blackSafe and blackBehindPawn)) * (blackKnights + blackBishops) / 4)
        } else {
            space[Evaluator.Companion.W] = 0
            space[Evaluator.Companion.B] = 0
        }

        // Squares that pawns attack or can attack by advancing
        pawnCanAttack[Evaluator.Companion.W] = ai.pawnAttacks[Evaluator.Companion.W]
        pawnCanAttack[Evaluator.Companion.B] = ai.pawnAttacks[Evaluator.Companion.B]
        for (i in 0..4) {
            whitePawnsAux = whitePawnsAux shl 8
            whitePawnsAux = whitePawnsAux and (b.pawns and b.blacks or ai.pawnAttacks[Evaluator.Companion.B]).inv() // Cannot advance because of a blocking pawn or a opposite pawn attack
            blackPawnsAux = blackPawnsAux.ushr(8)
            blackPawnsAux = blackPawnsAux and (b.pawns and b.whites or ai.pawnAttacks[Evaluator.Companion.W]).inv() // Cannot advance because of a blocking pawn or a opposite pawn attack

            if (whitePawnsAux == 0L && blackPawnsAux == 0L) {
                break
            }
            pawnCanAttack[Evaluator.Companion.W] = pawnCanAttack[Evaluator.Companion.W] or (whitePawnsAux and BitboardUtils.b_l.inv() shl 9 or (whitePawnsAux and BitboardUtils.b_r.inv() shl 7))
            pawnCanAttack[Evaluator.Companion.B] = pawnCanAttack[Evaluator.Companion.B] or ((blackPawnsAux and BitboardUtils.b_r.inv()).ushr(9) or (blackPawnsAux and BitboardUtils.b_l.inv()).ushr(7))
        }

        // Calculate attacks
        attacks[Evaluator.Companion.W] = evalAttacks(b, ai, Evaluator.Companion.W, b.blacks)
        attacks[Evaluator.Companion.B] = evalAttacks(b, ai, Evaluator.Companion.B, b.whites)

        // Squares surrounding King and three squares towards thew other side
        kingZone[Evaluator.Companion.W] = bbAttacks.king[ai.kingIndex[Evaluator.Companion.W]]
        kingZone[Evaluator.Companion.W] = kingZone[Evaluator.Companion.W] or (kingZone[Evaluator.Companion.W] shl 8)
        kingZone[Evaluator.Companion.B] = bbAttacks.king[ai.kingIndex[Evaluator.Companion.B]]
        kingZone[Evaluator.Companion.B] = kingZone[Evaluator.Companion.B] or kingZone[Evaluator.Companion.B].ushr(8)

        val all = b.all
        var pieceAttacks: Long
        var safeAttacks: Long
        var kingAttacks: Long

        var square: Long = 1

        for (index in 0..63) {
            if (square and all != 0L) {
                val isWhite = b.whites and square != 0L
                val us = if (isWhite) Evaluator.Companion.W else Evaluator.Companion.B
                val them = if (isWhite) Evaluator.Companion.B else Evaluator.Companion.W
                val mines = if (isWhite) b.whites else b.blacks
                val others = if (isWhite) b.blacks else b.whites
                val pcsqIndex = if (isWhite) index else 63 - index
                val rank = index shr 3
                val relativeRank = if (isWhite) rank else 7 - rank
                val file = 7 - index and 7

                pieceAttacks = ai.attacksFromSquare[index]

                if (square and b.pawns != 0L) {
                    pcsq[us] += pawnPcsq[pcsqIndex]

                    val myPawns = b.pawns and mines
                    val otherPawns = b.pawns and others
                    val adjacentFiles = BitboardUtils.FILES_ADJACENT[file]
                    val ranksForward = BitboardUtils.RANKS_FORWARD[us][rank]
                    val pawnFile = BitboardUtils.FILE[file]
                    val routeToPromotion = pawnFile and ranksForward
                    val otherPawnsAheadAdjacent = ranksForward and adjacentFiles and otherPawns
                    val pushSquare = if (isWhite) square shl 8 else square.ushr(8)

                    val supported = square and ai.pawnAttacks[us] != 0L
                    val doubled = myPawns and routeToPromotion != 0L
                    val opposed = otherPawns and routeToPromotion != 0L
                    val passed = !doubled
                            && !opposed
                            && otherPawnsAheadAdjacent == 0L

                    if (!passed) {
                        val myPawnsAheadAdjacent = ranksForward and adjacentFiles and myPawns
                        val myPawnsBesideAndBehindAdjacent = BitboardUtils.RANK_AND_BACKWARD[us][rank] and adjacentFiles and myPawns
                        val isolated = myPawns and adjacentFiles == 0L
                        val candidate = !doubled
                                && !opposed
                                && (otherPawnsAheadAdjacent and pieceAttacks.inv() == 0L || // Can become passer advancing
                                BitboardUtils.popCount(myPawnsBesideAndBehindAdjacent) >= BitboardUtils.popCount(otherPawnsAheadAdjacent and pieceAttacks.inv())) // Has more friend pawns beside and behind than opposed pawns controlling his route to promotion
                        val backward = !isolated
                                && !candidate
                                && myPawnsBesideAndBehindAdjacent == 0L
                                && pieceAttacks and otherPawns == 0L // No backwards if it can capture
                                && BitboardUtils.RANK_AND_BACKWARD[us][if (isWhite) BitboardUtils.getRankLsb(myPawnsAheadAdjacent) else BitboardUtils.getRankMsb(myPawnsAheadAdjacent)] and
                                routeToPromotion and (b.pawns or ai.pawnAttacks[them]) != 0L // Other pawns stopping it from advance, opposing or capturing it before reaching my pawns

                        if (debugPawns) {
                            val connected = bbAttacks.king[index] and adjacentFiles and myPawns != 0L
                            debugSB.append("PAWN " + BitboardUtils.SQUARE_NAMES[index] +
                                    (if (isWhite) " WHITE " else " BLACK ") +
                                    (if (isolated) "isolated " else "") +
                                    (if (supported) "supported " else "") +
                                    (if (connected) "connected " else "") +
                                    (if (doubled) "doubled " else "") +
                                    (if (opposed) "opposed " else "") +
                                    (if (candidate) "candidate " else "") +
                                    (if (backward) "backward " else "") +
                                    "\n"
                            )
                        }

                        if (backward) {
                            pawnStructure[us] -= PAWN_BACKWARDS[if (opposed) 1 else 0]
                        }
                        if (isolated) {
                            pawnStructure[us] -= PAWN_ISOLATED[if (opposed) 1 else 0]
                        }
                        if (doubled) {
                            pawnStructure[us] -= PAWN_DOUBLED[if (opposed) 1 else 0]
                        }
                        if (!supported
                                && !isolated
                                && !backward) {
                            pawnStructure[us] -= PAWN_UNSUPPORTED
                        }
                        if (candidate) {
                            passedPawns[us] += PAWN_CANDIDATE[relativeRank]
                        }

                        if (square and (BitboardUtils.D or BitboardUtils.E) != 0L
                                && relativeRank == 1
                                && pushSquare and mines and b.pawns.inv() != 0L) {
                            pawnStructure[us] -= PAWN_BLOCKADE
                        }

                        // Pawn Storm: It can open a file near the other king
                        if (gamePhase > 0 && relativeRank > 2) {
                            // Only if in kingside or queenside
                            val stormedPawns = otherPawnsAheadAdjacent and BitboardUtils.D.inv() and BitboardUtils.E.inv()
                            if (stormedPawns != 0L) {
                                // The stormed pawn must be in the other king's adjacent files
                                val otherKingFile = 7 - ai.kingIndex[them] and 7
                                if (stormedPawns and BitboardUtils.FILE[otherKingFile] != 0L) {
                                    pawnStructure[us] += PAWN_STORM_CENTER[relativeRank]
                                } else if (stormedPawns and BitboardUtils.FILES_ADJACENT[otherKingFile] != 0L) {
                                    pawnStructure[us] += PAWN_STORM[relativeRank]
                                }
                            }
                        }
                    } else {
                        //
                        // Passed Pawn
                        //
                        // Backfile only to the first piece found
                        val backFile = bbAttacks.getRookAttacks(index, all) and pawnFile and BitboardUtils.RANKS_BACKWARD[us][rank]
                        // If it has a rook or queen behind consider all the route to promotion attacked or defended
                        val attackedAndNotDefendedRoute =
                                ((routeToPromotion and ai.attackedSquares[them]) or if (backFile and (b.rooks or b.queens) and others != 0L) routeToPromotion else 0L) and
                                        ((routeToPromotion and ai.attackedSquares[us]) or if (backFile and (b.rooks or b.queens) and mines != 0L) routeToPromotion else 0L).inv()
                        val connected = bbAttacks.king[index] and adjacentFiles and myPawns != 0L
                        val outside = otherPawns != 0L && (square and BitboardUtils.FILES_LEFT[3] != 0L && b.pawns and BitboardUtils.FILES_LEFT[file] == 0L || square and BitboardUtils.FILES_RIGHT[4] != 0L && b.pawns and BitboardUtils.FILES_RIGHT[file] == 0L)
                        val mobile = pushSquare and (all or attackedAndNotDefendedRoute) == 0L
                        val runner = mobile
                                && routeToPromotion and all == 0L
                                && attackedAndNotDefendedRoute == 0L

                        if (debug) {
                            debugSB.append("PAWN " + BitboardUtils.SQUARE_NAMES[index] +
                                    (if (isWhite) " WHITE " else " BLACK ") +
                                    "passed " +
                                    (if (outside) "outside " else "") +
                                    (if (connected) "connected " else "") +
                                    (if (supported) "supported " else "") +
                                    (if (mobile) "mobile " else "") +
                                    (if (runner) "runner " else "") +
                                    "\n"
                            )
                        }

                        passedPawns[us] += PAWN_PASSER[relativeRank]

                        if (relativeRank >= 2) {
                            val pushIndex = if (isWhite) index + 8 else index - 8
                            passedPawns[us] += BitboardUtils.distance(pushIndex, ai.kingIndex[them]) * PAWN_PASSER_OTHER_KING_DISTANCE[relativeRank] - BitboardUtils.distance(pushIndex, ai.kingIndex[us]) * PAWN_PASSER_MY_KING_DISTANCE[relativeRank]
                        }
                        if (outside) {
                            passedPawns[us] += PAWN_PASSER_OUTSIDE[relativeRank]
                        }
                        if (supported) {
                            passedPawns[us] += PAWN_PASSER_SUPPORTED[relativeRank]
                        } else if (connected) {
                            passedPawns[us] += PAWN_PASSER_CONNECTED[relativeRank]
                        }
                        if (runner) {
                            passedPawns[us] += PAWN_PASSER_RUNNER[relativeRank]
                        } else if (mobile) {
                            passedPawns[us] += PAWN_PASSER_MOBILE[relativeRank]
                        }
                    }
                    // Pawn is part of the king shield
                    if (gamePhase > 0 && pawnFile and ranksForward.inv() and kingZone[us] and BitboardUtils.D.inv() and BitboardUtils.E.inv() != 0L) { // Pawn in the kingzone
                        pawnStructure[us] += if (pawnFile and b.kings and mines != 0L)
                            PAWN_SHIELD_CENTER[relativeRank]
                        else
                            PAWN_SHIELD[relativeRank]
                    }

                } else if (square and b.knights != 0L) {
                    pcsq[us] += knightPcsq[pcsqIndex]

                    safeAttacks = pieceAttacks and ai.pawnAttacks[them].inv()

                    mobility[us] += MOBILITY[Piece.KNIGHT][BitboardUtils.popCount(safeAttacks and mobilitySquares[us])]

                    kingAttacks = safeAttacks and kingZone[them]
                    if (kingAttacks != 0L) {
                        kingSafety[us] += PIECE_ATTACKS_KING[Piece.KNIGHT] * BitboardUtils.popCount(kingAttacks)
                        kingAttackersCount[us]++
                    }

                    // Knight outpost: no opposite pawns can attack the square
                    if (square and OUTPOST_MASK[us] and pawnCanAttack[them].inv() != 0L) {
                        positional[us] += KNIGHT_OUTPOST[if (square and ai.pawnAttacks[us] != 0L) 1 else 0]
                    }

                } else if (square and b.bishops != 0L) {
                    pcsq[us] += bishopPcsq[pcsqIndex]

                    safeAttacks = pieceAttacks and ai.pawnAttacks[them].inv()

                    mobility[us] += MOBILITY[Piece.BISHOP][BitboardUtils.popCount(safeAttacks and mobilitySquares[us])]

                    kingAttacks = safeAttacks and kingZone[them]
                    if (kingAttacks != 0L) {
                        kingSafety[us] += PIECE_ATTACKS_KING[Piece.BISHOP] * BitboardUtils.popCount(kingAttacks)
                        kingAttackersCount[us]++
                    }

                    // Bishop Outpost
                    if (square and OUTPOST_MASK[us] and pawnCanAttack[them].inv() != 0L) {
                        positional[us] += BISHOP_OUTPOST[if (square and ai.pawnAttacks[us] != 0L) 1 else 0]
                    }

                    positional[us] -= BISHOP_MY_PAWNS_IN_COLOR_PENALTY * BitboardUtils.popCount(b.pawns and mines and BitboardUtils.getSameColorSquares(square))

                    if (BISHOP_TRAPPING[index] and b.pawns and others != 0L) {
                        mobility[us] -= BISHOP_TRAPPED_PENALTY[if (BISHOP_TRAPPING_GUARD[index] and b.pawns and others != 0L) 1 else 0]
                    }

                } else if (square and b.rooks != 0L) {
                    pcsq[us] += rookPcsq[pcsqIndex]

                    safeAttacks = pieceAttacks and ai.pawnAttacks[them].inv() and ai.knightAttacks[them].inv() and ai.bishopAttacks[them].inv()

                    val mobilityCount = BitboardUtils.popCount(safeAttacks and mobilitySquares[us])
                    mobility[us] += MOBILITY[Piece.ROOK][mobilityCount]

                    kingAttacks = safeAttacks and kingZone[them]
                    if (kingAttacks != 0L) {
                        kingSafety[us] += PIECE_ATTACKS_KING[Piece.ROOK] * BitboardUtils.popCount(kingAttacks)
                        kingAttackersCount[us]++
                    }

                    if (square and OUTPOST_MASK[us] and pawnCanAttack[them].inv() != 0L) {
                        positional[us] += ROOK_OUTPOST[if (square and ai.pawnAttacks[us] != 0L) 1 else 0]
                    }

                    val rookFile = BitboardUtils.FILE[file]
                    if (rookFile and b.pawns and mines == 0L) {
                        positional[us] += ROOK_FILE[if (rookFile and b.pawns == 0L) 0 else 1]
                    }

                    if (relativeRank >= 4) {
                        val pawnsAligned = BitboardUtils.RANK[rank] and b.pawns and others
                        if (pawnsAligned != 0L) {
                            positional[us] += ROOK_7 * BitboardUtils.popCount(pawnsAligned)
                        }
                    }

                    // Rook trapped by king
                    if (square and ROOK_TRAPPING[ai.kingIndex[us]] != 0L && mobilityCount < ROOK_TRAPPED_PENALTY.size) {
                        positional[us] -= ROOK_TRAPPED_PENALTY[mobilityCount]
                    }

                } else if (square and b.queens != 0L) {
                    pcsq[us] += queenPcsq[pcsqIndex]

                    safeAttacks = pieceAttacks and ai.pawnAttacks[them].inv() and ai.knightAttacks[them].inv() and ai.bishopAttacks[them].inv() and ai.rookAttacks[them].inv()

                    mobility[us] += MOBILITY[Piece.QUEEN][BitboardUtils.popCount(safeAttacks and mobilitySquares[us])]

                    kingAttacks = safeAttacks and kingZone[them]
                    if (kingAttacks != 0L) {
                        kingSafety[us] += PIECE_ATTACKS_KING[Piece.QUEEN] * BitboardUtils.popCount(kingAttacks)
                        kingAttackersCount[us]++
                    }

                } else if (square and b.kings != 0L) {
                    pcsq[us] += kingPcsq[pcsqIndex]
                }
            }
            square = square shl 1
        }

        val oe = (if (b.turn) TEMPO else -TEMPO) + pawnMaterial[Evaluator.Companion.W] - pawnMaterial[Evaluator.Companion.B] + nonPawnMaterial[Evaluator.Companion.W] - nonPawnMaterial[Evaluator.Companion.B] + pcsq[Evaluator.Companion.W] - pcsq[Evaluator.Companion.B] + space[Evaluator.Companion.W] - space[Evaluator.Companion.B] + positional[Evaluator.Companion.W] - positional[Evaluator.Companion.B] + attacks[Evaluator.Companion.W] - attacks[Evaluator.Companion.B] + mobility[Evaluator.Companion.W] - mobility[Evaluator.Companion.B] + pawnStructure[Evaluator.Companion.W] - pawnStructure[Evaluator.Companion.B] + passedPawns[Evaluator.Companion.W] - passedPawns[Evaluator.Companion.B] + Evaluator.Companion.oeShr(6, KING_SAFETY_PONDER[kingAttackersCount[Evaluator.Companion.W]] * kingSafety[Evaluator.Companion.W] - KING_SAFETY_PONDER[kingAttackersCount[Evaluator.Companion.B]] * kingSafety[Evaluator.Companion.B])

        // Ponder opening and Endgame value depending of the game phase and the scale factor
        val value = (gamePhase * Evaluator.Companion.o(oe) + (Evaluator.Companion.GAME_PHASE_MIDGAME - gamePhase) * Evaluator.Companion.e(oe) * scaleFactor[0] / Endgame.SCALE_FACTOR_DEFAULT) / Evaluator.Companion.GAME_PHASE_MIDGAME

        if (debug) {
            logger.debug(debugSB)
            logger.debug("                    WOpening WEndgame BOpening BEndgame")
            logger.debug("pawnMaterial      = " + formatOE(pawnMaterial[Evaluator.Companion.W]) + " " + formatOE(pawnMaterial[Evaluator.Companion.B]))
            logger.debug("nonPawnMaterial   = " + formatOE(nonPawnMaterial[Evaluator.Companion.W]) + " " + formatOE(nonPawnMaterial[Evaluator.Companion.B]))
            logger.debug("pcsq              = " + formatOE(pcsq[Evaluator.Companion.W]) + " " + formatOE(pcsq[Evaluator.Companion.B]))
            logger.debug("space             = " + formatOE(space[Evaluator.Companion.W]) + " " + formatOE(space[Evaluator.Companion.B]))
            logger.debug("mobility          = " + formatOE(mobility[Evaluator.Companion.W]) + " " + formatOE(mobility[Evaluator.Companion.B]))
            logger.debug("positional        = " + formatOE(positional[Evaluator.Companion.W]) + " " + formatOE(positional[Evaluator.Companion.B]))
            logger.debug("pawnStructure     = " + formatOE(pawnStructure[Evaluator.Companion.W]) + " " + formatOE(pawnStructure[Evaluator.Companion.B]))
            logger.debug("passedPawns       = " + formatOE(passedPawns[Evaluator.Companion.W]) + " " + formatOE(passedPawns[Evaluator.Companion.B]))
            logger.debug("attacks           = " + formatOE(attacks[Evaluator.Companion.W]) + " " + formatOE(attacks[Evaluator.Companion.B]))
            logger.debug("kingSafety        = " + formatOE(Evaluator.Companion.oeShr(6, KING_SAFETY_PONDER[kingAttackersCount[Evaluator.Companion.W]] * kingSafety[Evaluator.Companion.W])) + " " + formatOE(Evaluator.Companion.oeShr(6, KING_SAFETY_PONDER[kingAttackersCount[Evaluator.Companion.B]] * kingSafety[Evaluator.Companion.B])))
            logger.debug("tempo             = " + formatOE(if (b.turn) TEMPO else -TEMPO))
            logger.debug("                    -----------------")
            logger.debug("TOTAL:              " + formatOE(oe))
            logger.debug("gamePhase = $gamePhase => value = $value")
        }
        //assert(Math.abs(value) < Evaluator.Companion.KNOWN_WIN) { "Eval is outside limits" }
        return value
    }

    private fun evalAttacks(board: Board, ai: AttacksInfo, us: Int, others: Long): Int {
        var attacks = 0

        var attackedByPawn = ai.pawnAttacks[us] and others and board.pawns.inv()
        while (attackedByPawn != 0L) {
            val lsb = BitboardUtils.lsb(attackedByPawn)
            attacks += PAWN_ATTACKS[board.getPieceIntAt(lsb)]
            attackedByPawn = attackedByPawn and lsb.inv()
        }

        val otherWeak = ai.attackedSquares[us] and others and ai.pawnAttacks[1 - us].inv()
        if (otherWeak != 0L) {
            var attackedByMinor = ai.knightAttacks[us] or ai.bishopAttacks[us] and otherWeak
            while (attackedByMinor != 0L) {
                val lsb = BitboardUtils.lsb(attackedByMinor)
                attacks += MINOR_ATTACKS[board.getPieceIntAt(lsb)]
                attackedByMinor = attackedByMinor and lsb.inv()
            }
            var attackedByMajor = ai.rookAttacks[us] or ai.queenAttacks[us] and otherWeak
            while (attackedByMajor != 0L) {
                val lsb = BitboardUtils.lsb(attackedByMajor)
                attacks += MAJOR_ATTACKS[board.getPieceIntAt(lsb)]
                attackedByMajor = attackedByMajor and lsb.inv()
            }
        }

        val superiorAttacks = ai.pawnAttacks[us] and others and board.pawns.inv() or (ai.knightAttacks[us] or ai.bishopAttacks[us] and others and (board.rooks or board.queens)) or (ai.rookAttacks[us] and others and board.queens)
        val superiorAttacksCount = BitboardUtils.popCount(superiorAttacks)
        if (superiorAttacksCount >= 2) {
            attacks += superiorAttacksCount * HUNG_PIECES
        }

        val pinnedNotPawn = ai.pinnedPieces and board.pawns.inv() and others
        if (pinnedNotPawn != 0L) {
            attacks += PINNED_PIECE * BitboardUtils.popCount(pinnedNotPawn)
        }
        return attacks
    }

    companion object {
        // Tempo
        val TEMPO = Evaluator.Companion.oe(15, 5) // Add to moving side score

    }
}