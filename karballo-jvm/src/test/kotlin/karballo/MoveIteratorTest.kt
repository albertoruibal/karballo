package karballo

import karballo.search.MoveIterator
import karballo.search.SearchEngine
import karballo.util.JvmPlatformUtils
import karballo.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveIteratorTest {

    constructor() {
        Utils.instance = JvmPlatformUtils()
    }

    private fun testPositionCountingMoves(fen: String, generateMoves: Int, ttMoveString: String?, totalMoves: Int, totalCaptures: Int, totalEnPassant: Int, totalChecks: Int) {
        val b = Board()
        b.fen = fen
        println(b.toString())
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = fen
        val moveIterator = searchEngine.nodes[0].moveIterator
        val ttMove = if (ttMoveString == null) Move.NONE else Move.getFromString(b, ttMoveString, true)
        moveIterator.genMoves(ttMove, generateMoves)
        var move: Int
        var moves = 0
        var captures = 0
        var enPassant = 0
        var checks = 0
        while (true) {
            move = moveIterator.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move) + " SEE=" + moveIterator.lastMoveSee)
            moves++
            if (Move.isCapture(move)) {
                captures++
            }
            if (Move.getMoveType(move) == Move.TYPE_PASSANT) {
                enPassant++
            }
            if (Move.isCheck(move)) {
                checks++
            }
        }
        assertTrue(totalMoves.toString() + " moves", moves == totalMoves)
        assertTrue(totalCaptures.toString() + " captures", captures == totalCaptures)
        assertTrue(totalEnPassant.toString() + " en-passant", enPassant == totalEnPassant)
        assertTrue(totalChecks.toString() + " checks", checks == totalChecks)
    }

    @Test
    fun testCheckAfterPromotion() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "7k/P7/8/8/8/8/8/7K w - - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        assertTrue("First move must be check", Move.isCheck(lmi.next()))
    }

    @Test
    fun testCheckAfterPromotionKingBehind() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "8/P7/8/k7/8/8/8/7K w - - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        assertTrue("First move must be check", Move.isCheck(lmi.next()))
    }

    @Test
    fun castlingGivesCheck() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "5k2/8/8/8/8/8/8/4K2R w K - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        var move: Int
        var castling = Move.NONE
        while (true) {
            move = lmi.next()
            if (move == Move.NONE) {
                break
            }

            println(Move.toStringExt(move))

            if (Move.getMoveType(move) == Move.TYPE_KINGSIDE_CASTLING) {
                castling = move
            }
        }
        assertTrue("Castling must give check", Move.isCheck(castling))
    }

    @Test
    fun castlingGivesCheck2() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "K3k2r/8/8/8/8/8/8/8 b k - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        var move: Int
        var castling = Move.NONE
        while (true) {
            move = lmi.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move))

            if (Move.getMoveType(move) == Move.TYPE_KINGSIDE_CASTLING) {
                castling = move
            }
        }
        assertTrue("Castling must give check", Move.isCheck(castling))
    }

    @Test
    fun cannotCastleAttackedSquare1() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "4k2r/8/8/8/8/8/K7/5R2 b k - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        var move: Int
        var castling = Move.NONE
        while (true) {
            move = lmi.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move))

            if (Move.getMoveType(move) == Move.TYPE_KINGSIDE_CASTLING) {
                castling = move
            }
        }
        assertTrue("Must not allow castling", castling == Move.NONE)
    }

    @Test
    fun cannotCastleAttackedSquare2() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "4k2r/7B/8/8/8/8/K7/8 b k - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        var move: Int
        var castling = Move.NONE
        while (true) {
            move = lmi.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move))

            if (Move.getMoveType(move) == Move.TYPE_KINGSIDE_CASTLING) {
                castling = move
            }
        }
        assertTrue("Must not allow castling", castling == Move.NONE)
    }

    @Test
    fun canCastleLongAlthoughThereIsAnAttackedSquareNear() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "7k/8/8/8/8/8/p5p1/R3K3 w Q - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        var move: Int
        var castling = Move.NONE
        while (true) {
            move = lmi.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move))

            if (Move.getMoveType(move) == Move.TYPE_QUEENSIDE_CASTLING) {
                castling = move
            }
        }
        assertTrue("Must allow castling because the king does not crosses an attacked square", castling != Move.NONE)
    }

    @Test
    fun longCastlingGivesCheck2() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "8/8/8/8/8/8/8/R3K2k w Q - 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        var move: Int
        var castling = Move.NONE
        while (true) {
            move = lmi.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move))

            if (Move.getMoveType(move) == Move.TYPE_QUEENSIDE_CASTLING) {
                castling = move
            }
        }
        assertTrue("Long castling must give check", Move.isCheck(castling))
    }

    @Test
    fun enPassantGivesCheck() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "7k/8/8/1b6/1pP5/8/8/5K2 b - c3 0 1"
        println(searchEngine.board.toString())
        val lmi = searchEngine.nodes[0].moveIterator
        lmi.genMoves(Move.NONE)
        var move: Int
        var enPassant = Move.NONE
        while (true) {
            move = lmi.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move))

            if (Move.getMoveType(move) == Move.TYPE_PASSANT) {
                enPassant = move
            }
        }
        assertTrue("En passant must give check", Move.isCheck(enPassant))
    }

    @Test
    fun enPassantGivesCheck2PiecesXray() {
        testPositionCountingMoves("8/8/8/R2Pp2k/8/8/8/7K w - e6 0 1", MoveIterator.GENERATE_ALL, null, 14, 1, 1, 1)
    }

    @Test
    fun cannotCaptureEnPassantCheck2PiecesXray() {
        testPositionCountingMoves("8/8/8/r2Pp2K/8/8/8/7k w - e6 0 1", MoveIterator.GENERATE_ALL, null, 6, 0, 0, 0)
    }

    @Test
    fun promoteCapturing() {
        testPositionCountingMoves("1n5k/2P5/8/8/8/8/8/7K w - - 0 1", MoveIterator.GENERATE_ALL, null, 11, 4, 0, 4)
    }

    @Test
    fun checkEvasions() {
        testPositionCountingMoves("4k3/8/8/8/1b1Q4/2b5/1P6/4K3 w - - 0 1", MoveIterator.GENERATE_ALL, null, 7, 2, 0, 0)
    }

    @Test
    fun avoidCheckPromoting() {
        testPositionCountingMoves("K6r/1P6/8/8/8/8/8/7k w - - 0 1", MoveIterator.GENERATE_ALL, null, 5, 0, 0, 0)
    }

    @Test
    fun checkEvasionCapturingEnPassant() {
        testPositionCountingMoves("8/8/8/3k4/1pP5/8/8/5K2 b - c3 0 1", MoveIterator.GENERATE_ALL, null, 9, 2, 1, 0)
    }

    @Test
    fun checkEvasionInterposeCapturingEnPassant() {
        testPositionCountingMoves("8/8/8/8/1pPk4/8/8/B4K2 b - c3 0 1", MoveIterator.GENERATE_ALL, null, 6, 2, 1, 0)
    }

    @Test
    fun captureCheckingPieceWithKing() {
        testPositionCountingMoves("rq2r1k1/5Qp1/p4p2/4bNP1/1p2P2P/8/PP4K1/5R1R b - - 1 2", MoveIterator.GENERATE_ALL, null, 3, 1, 0, 0)
    }

    @Test
    fun captureCheckingPieceWithKingAndTwoPiecesGivingCheck() {
        testPositionCountingMoves("k4r2/R5pb/1pQp1n1p/3P4/5p1P/3P2P1/r1q1R2K/8 b - - 1 1", MoveIterator.GENERATE_ALL, null, 2, 1, 0, 0)
    }

    @Test
    fun evadeCheckMoveKingCapturing() {
        testPositionCountingMoves("r5r1/p1q2pBk/1p1R2p1/3pP3/6bQ/2p5/P1P1NPPP/6K1 b - - 1 1", MoveIterator.GENERATE_ALL, null, 2, 1, 0, 0)
    }

    @Test
    fun generatingDuplicatedTTMove() {
        testPositionCountingMoves("rq2r1k1/5p2/p6p/4b1P1/1p2P2P/5Q2/PP4K1/5R1R w - - 0 2", MoveIterator.GENERATE_ALL, "Qf3xf7+", 35, 2, 0, 1)
    }

    @Test
    fun testGeneratePromotionsInQuiescence() {
        // It must only generate the promotion to queen
        testPositionCountingMoves("7k/P7/8/8/8/8/8/7K w - - 0 1", MoveIterator.GENERATE_CAPTURES_PROMOS, null, 1, 0, 0, 1)

        // Generates the underpromotion to rook
        testPositionCountingMoves("7k/P7/8/8/8/8/8/7K w - - 0 1", MoveIterator.GENERATE_CAPTURES_PROMOS_CHECKS, null, 2, 0, 0, 2)
    }

    @Test
    fun testGenerateCapturesInQuiescence() {
        testPositionCountingMoves("8/1kb2p2/4b1p1/8/2Q2NB1/8/8/K7 w - - 0 1", MoveIterator.GENERATE_CAPTURES_PROMOS, null, 2, 2, 0, 0)
    }

    @Test
    fun testGenerateOnlyGoodAnEqualCapturesInQuiescence() {
        testPositionCountingMoves("2q4k/3n4/1p6/2b5/8/1N2B3/8/6QK w - - 0 1", MoveIterator.GENERATE_CAPTURES_PROMOS, null, 2, 2, 0, 0)
    }

    @Test
    fun testChess960Castling() {
        testPositionCountingMoves("nqrkbbnr/pppppppp/8/8/8/8/PPPPPPPP/NQRKBBNR w KQkq - 0 1", MoveIterator.GENERATE_ALL, null, 20, 0, 0, 0)
    }

    @Test
    fun testChess960CastlingRookSameSquareGivesCheck() {
        testPositionCountingMoves("8/8/8/8/8/8/8/3RK2k w Q - 0 1", MoveIterator.GENERATE_ALL, null, 15, 0, 0, 4)
    }

    @Test
    fun testPawnPushDoesNotLeaveTheKingInCheck() {
        testPositionCountingMoves("5rk1/6P1/2Q5/b5Pp/p6P/8/1rPK4/q3R1R1 w - - 3 6", MoveIterator.GENERATE_ALL, null, 4, 0, 0, 0)
    }

    @Test
    fun testDoNotGenerateLongCastling() {
        val b = Board()

        // position startpos moves e2e4 c7c6 g1f3 d7d5 b1c3 c8g4 f1e2 e7e6 d2d4 g8f6 e4e5 f6e4 e1g1 h7h6 c1e3 e4c3 b2c3 d8a5 e3d2
        b.startPosition()
        b.doMove(Move.getFromString(b, "e2e4", false))
        b.doMove(Move.getFromString(b, "c7c6", false))
        b.doMove(Move.getFromString(b, "g1f3", false))
        b.doMove(Move.getFromString(b, "d7d5", false))
        b.doMove(Move.getFromString(b, "b1c3", false))
        b.doMove(Move.getFromString(b, "c8g4", false))
        b.doMove(Move.getFromString(b, "f1e2", false))
        b.doMove(Move.getFromString(b, "e7e6", false))
        b.doMove(Move.getFromString(b, "d2d4", false))
        b.doMove(Move.getFromString(b, "g8f6", false))
        b.doMove(Move.getFromString(b, "e4e5", false))
        b.doMove(Move.getFromString(b, "f6e4", false))
        b.doMove(Move.getFromString(b, "e1g1", false))
        b.doMove(Move.getFromString(b, "h7h6", false))
        b.doMove(Move.getFromString(b, "c1e3", false))
        b.doMove(Move.getFromString(b, "e4c3", false))
        b.doMove(Move.getFromString(b, "b2c3", false))
        b.doMove(Move.getFromString(b, "d8a5", false))
        b.doMove(Move.getFromString(b, "e3d2", false))

        println(b.toString())
        val searchEngine = SearchEngine(Config())
        val moveIterator = searchEngine.nodes[0].moveIterator
        moveIterator.genMoves(0, MoveIterator.GENERATE_ALL)
        var move: Int
        var longCastling = false
        while (true) {
            move = moveIterator.next()
            if (move == Move.NONE) {
                break
            }
            println(Move.toStringExt(move))
            if ("O-O-O" == Move.toStringExt(move)) {
                longCastling = true
            }
        }
        assertEquals("Must not allow black long castling", false, longCastling)
    }

    @Test
    fun testTtLastMoveSee() {
        val searchEngine = SearchEngine(Config())
        searchEngine.board.fen = "rq2r1k1/5p2/p6p/4b1P1/1p2P2P/5Q2/PP4K1/5R1R w - - 0 2"
        println(searchEngine.board.toString())
        val moveIterator = searchEngine.nodes[0].moveIterator
        val ttMove = Move.getFromString(searchEngine.board, "Qc3", true)
        moveIterator.genMoves(ttMove, MoveIterator.GENERATE_ALL)
        val move = moveIterator.next()
        assertEquals("Qc3", Move.toSan(searchEngine.board, move))
        assertEquals(-900, moveIterator.lastMoveSee.toLong())
    }
}