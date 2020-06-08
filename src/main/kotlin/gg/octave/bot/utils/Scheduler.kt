package gg.octave.bot.utils

import io.sentry.Sentry
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object Scheduler {
    fun fixedRateScheduleWithSuppression(executor: ScheduledExecutorService, initalDelay: Long, delay: Long, unit: TimeUnit, block: () -> Unit) {
        executor.scheduleAtFixedRate({
            try {
                block()
            } catch (e: Exception) {
                Sentry.capture(e)
            }
        }, initalDelay, delay, unit)
    }
}
