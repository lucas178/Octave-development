package gg.octave.bot.utils

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class Task(private val delay: Long, private val unit: TimeUnit, private val runnable: () -> Unit) {
    @Volatile
    private var task: Future<*>? = null
    val isRunning: Boolean get() = task?.let { !it.isCancelled } ?: false // null

    /**
     * Starts the task, cancelling the previous one if it exists.
     */
    fun start() {
        stop()
        task = executor.schedule(runnable, delay, unit)
    }

    fun stop(interrupt: Boolean = false) {
        task?.cancel(interrupt)
        task = null
    }

    companion object {
        private val executor = Executors.newSingleThreadScheduledExecutor()
    }
}
