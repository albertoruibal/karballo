package karballo

import karballo.book.Book
import karballo.book.NoBook

/**
 * Holds configuration parameters

 * @author rui
 */
class Config {
    var transpositionTableSize = DEFAULT_TRANSPOSITION_TABLE_SIZE
    var ponder = DEFAULT_PONDER
    var useBook = DEFAULT_USE_BOOK
    var book: Book = NoBook()
    var evaluator = DEFAULT_EVALUATOR
    var contemptFactor = DEFAULT_CONTEMPT_FACTOR

    var isUciChess960 = DEFAULT_UCI_CHESS960

    var rand = DEFAULT_RAND
    var bookKnowledge = DEFAULT_BOOK_KNOWGLEDGE
    var isLimitStrength = DEFAULT_LIMIT_STRENGTH
        set(limitStrength) {
            field = limitStrength
            calculateErrorsFromElo()
        }
    var elo = DEFAULT_ELO
        set(elo) {
            field = elo
            calculateErrorsFromElo()
        }

    /**
     * Calculates the errors and the book knowledge using the limitStrength and elo params
     * 2100 is the max elo, 500 the min
     */
    private fun calculateErrorsFromElo() {
        if (isLimitStrength) {
            rand = 900 - (this.elo - 500) * 900 / 1600 // Errors per 1000
            bookKnowledge = (this.elo - 500) * 100 / 1600 // In percentage
        } else {
            rand = 0
            bookKnowledge = 100
        }
    }

    companion object {
        // Default values are static fields used also from UCIEngine
        val DEFAULT_TRANSPOSITION_TABLE_SIZE = 64
        val DEFAULT_PONDER = true
        val DEFAULT_USE_BOOK = true
        val DEFAULT_BOOK_KNOWGLEDGE = 100
        val DEFAULT_EVALUATOR = "complete"

        // >0 refuses draw <0 looks for draw
        val DEFAULT_CONTEMPT_FACTOR = 90
        val DEFAULT_UCI_CHESS960 = false

        val DEFAULT_RAND = 0
        val DEFAULT_LIMIT_STRENGTH = false
        val DEFAULT_ELO = 2100
    }
}