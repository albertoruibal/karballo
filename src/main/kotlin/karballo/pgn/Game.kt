package karballo.pgn

class Game {
    var id: Long = 0
    internal var event: String? = null
    internal var eventDate: String? = null
    internal var site: String? = null
    internal var date: String? = null
    internal var round: String? = null
    var white: String? = null
    var black: String? = null
    var whiteElo: Int? = null
    var whiteFideId: Int? = null
    var blackElo: Int? = null
    var blackFideId: Int? = null
    var fenStartPosition: String? = null
    internal var result: String? = null
    var eco: String? = null
    var pv: GameNodeVariation? = null

    fun getEvent(): String? {
        return event
    }

    fun setEvent(event: String) {
        this.event = event.replace("?", "")
    }

    fun getSite(): String? {
        return site
    }

    fun setSite(site: String) {
        this.site = site.replace("?", "")
    }

    fun getRound(): String? {
        return round
    }

    fun setRound(round: String) {
        this.round = round.replace("?", "")
    }

    fun getResult(): String? {
        return result
    }

    fun setResult(result: String) {
        if ("1" == result) {
            this.result = "1-0"
        } else if ("0" == result) {
            this.result = "0-1"
        } else if ("=" == result || "1/2-1/2" == result) {
            this.result = "½-½"
        } else {
            this.result = result
        }
    }

    fun getDate(): String? {
        return date
    }

    fun setDate(date: String?) {
        this.date = if (date == null) null else date.replace(".??.??", "").replace("????", "")
    }

    fun getEventDate(): String? {
        return eventDate
    }

    fun setEventDate(eventDate: String?) {
        this.eventDate = if (eventDate == null) null else eventDate.replace(".??.??", "").replace("????", "")
    }
}
