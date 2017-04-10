package karballo.search

import karballo.Config
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

class SearchEngineThreaded(config: Config) : SearchEngine(config) {

    lateinit var thread: Job

    /**
     * Threaded version
     */
    override fun go(searchParameters: SearchParameters) {
        thread = launch(CommonPool) {
            super.go(searchParameters)
        }
    }

    /**
     * Stops thinking
     */
    suspend fun stopAwait() {
        synchronized(startStopSearchLock) {
            super.stop()
            thread.join()
        }
    }

    override fun sleep(time: Long) {
        try {
            Thread.sleep(time)
        } catch (e: InterruptedException) {
        }
    }
}