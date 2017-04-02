package karballo.pgn

object NumericAnnotationGlyphs {

    fun translate(`in`: String): String {
        when (`in`) {
            "$0" -> return ""
            "$1" -> return "!"
            "$2" -> return "?"
            "$3" -> return "‼"
            "$4" -> return "⁇"
            "$5" -> return "⁉"
            "$6" -> return "⁈"
            "$7" -> return "□"
            "$8" -> return ""

            "$10" -> return "="
            "$13" -> return "∞"
            "$14" -> return "⩲⩲+="
            "$15" -> return "=+"
            "$16" -> return "±"
            "$17" -> return "∓"
            "$18" -> return "+-"
            "$19" -> return "-+"

            "$22" -> return "⨀"
            "$23" -> return "⨀"

            "$32" -> return "⟳"
            "$33" -> return "⟳"

            "$36" -> return "→"
            "$37" -> return "→"
            "$40" -> return "↑"
            "$41" -> return "↑"

            "$132" -> return "⇆"
            "$133" -> return "⇆"
            "$140" -> return "∆"
            "$142" -> return "⌓"
            "$145" -> return "RR"
            "$146" -> return "N"

            "$239" -> return "⇔"
            "$240" -> return "⇗"
            "$242" -> return "⟫"
            "$243" -> return "⟪"
            "$244" -> return "✕"
            "$245" -> return "⊥"
        }
        return `in`
    }

}
