package karballo.log

class Logger private constructor(internal var prefix: String) {

    fun info(`in`: Any) {
        if (noLog) return
        print("INFO ")
        print(prefix)
        print(" - ")
        println(`in`.toString())
    }

    fun debug(`in`: Any) {
        if (noLog) return
        print("DEBUG ")
        print(prefix)
        print(" - ")
        println(`in`.toString())
    }

    fun error(`in`: Any) {
        if (noLog) return
        print("ERROR ")
        print(prefix)
        print(" - ")
        println(`in`.toString())
    }

    companion object {
        var noLog = false

        fun getLogger(prefix: String): Logger {
            return Logger(prefix)
        }
    }
}
