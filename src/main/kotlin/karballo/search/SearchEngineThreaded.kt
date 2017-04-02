package karballo.search

import karballo.Config

class SearchEngineThreaded(config: Config) : SearchEngine(config) {

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
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }

    override fun sleep(time: Int) {
        try {
            Thread.sleep(10)
        } catch (e: InterruptedException) {
        }

    }
}