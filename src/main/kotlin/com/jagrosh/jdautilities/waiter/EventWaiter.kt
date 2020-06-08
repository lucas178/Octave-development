/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jagrosh.jdautilities.waiter

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * A simple object used primarily for entities found in [com.jagrosh.jdautilities.menu].
 *
 * <p>The EventWaiter is capable of handling specialized forms of [GenericEvent]
 * that must meet criteria not normally specifiable without implementation of an [EventListener].
 *
 * <p>If you intend to use the EventWaiter, it is highly recommended you <b>DO NOT create multiple EventWaiters</b>!
 * Doing this will cause unnecessary increases in memory usage.
 *
 * @author John Grosh (jagrosh)
 * @author Avarel
 */
class EventWaiter : EventListener {
    private val waiters = mutableMapOf<Class<*>, MutableList<Waiter<GenericEvent>>>()
    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    @Suppress("UNCHECKED_CAST")
    fun <T : GenericEvent> waitForEvent(cls: Class<in T>,
                                        predicate: (T) -> Boolean,
                                        action: (T) -> Unit,
                                        timeout: Long,
                                        unit: TimeUnit?,
                                        timeoutAction: (() -> Unit)?): Waiter<T> {
        val list = waiters.getOrPut(cls, ::mutableListOf)

        val waiter = Waiter(cls, predicate, action)
        list.add(waiter as Waiter<GenericEvent>)

        if (timeout > 0) {
            requireNotNull(unit)

            scheduledExecutor.schedule({
                if (list.remove(waiter)) {
                    timeoutAction?.invoke()
                }
            }, timeout, unit)
        }

        return waiter
    }

    @Suppress("UNCHECKED_CAST")
    override fun onEvent(event: GenericEvent) {
        var cls: Class<in GenericEvent> = event.javaClass

        while (cls.superclass != null) {
            waiters[cls]?.removeIf {
                @Suppress("SENSELESS_COMPARISON")
                it == null || it.attempt(event)
            }
            cls = cls.superclass
        }
    }

    fun <T : GenericEvent> waitFor(cls: Class<T>, action: (T) -> Unit): WaiterBuilder<T> = WaiterBuilder(cls, action)

    fun <T : GenericEvent> waitFor(cls: Class<T>, action: Consumer<T>): WaiterBuilder<T> = WaiterBuilder(cls) { action.accept(it) }

    // builder
    inner class WaiterBuilder<T : GenericEvent>(private var cls: Class<T>, private var action: (T) -> Unit) {
        private var predicate: ((T) -> Boolean) = { true }

        fun predicate(predicate: (event: T) -> Boolean): WaiterBuilder<T> {
            this.predicate = predicate
            return this
        }

        fun noTimeout(): Waiter<T> {
            return waitForEvent(cls, predicate, action, 0, null, null)
        }

        fun timeout(timeout: Long, unit: TimeUnit, timeoutAction: () -> Unit): Waiter<T> {
            return waitForEvent(cls, predicate, action, timeout, unit, timeoutAction)
        }

        fun timeout(timeout: Long, unit: TimeUnit, timeoutAction: Runnable): Waiter<T> {
            return waitForEvent(cls, predicate, action, timeout, unit, { timeoutAction.run() })
        }

        fun timeout(timeout: Long, unit: TimeUnit): Waiter<T> {
            return waitForEvent(cls, predicate, action, timeout, unit, null)
        }
    }

    inner class Waiter<in T : GenericEvent>(private val cls: Class<in T>,
                                            private val predicate: (T) -> Boolean,
                                            private val action: (T) -> Unit) {
        fun isValid(): Boolean {
            return waiters[cls]?.contains(this) == true
        }

        fun attempt(event: T): Boolean { // predicate(event) && action(event).let { true }
            return if (predicate(event)) {
                action(event)
                true
            } else {
                false
            }
        }

        fun cancel(): Boolean {
            return waiters[cls]?.remove(this) == true
        }
    }
}