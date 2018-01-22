package karballo.search

import karballo.Config

class SearchEngineThreaded(config: Config) : SearchEngine(config), Runnable {

    lateinit var thread: Thread

    /**
     * Threaded version
     */
    override fun go(searchParameters: SearchParameters) {
        synchronized(startStopSearchLock) {
            if (!initialized || isSearching) {
                return
            }
            isSearching = true
            setInitialSearchParameters(searchParameters)
        }

        thread = Thread(this)
        thread.start()
    }

    /**
     * Stops thinking
     */
    override fun stop() {
        synchronized(startStopSearchLock) {
            while (isSearching) {
                super.stop()
                sleep(10)
            }
        }
    }

    override fun sleep(time: Long) {
        try {
            Thread.sleep(time)
        } catch (e: InterruptedException) {
        }
    }
}