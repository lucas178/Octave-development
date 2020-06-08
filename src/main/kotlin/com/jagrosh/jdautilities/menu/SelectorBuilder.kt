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

package com.jagrosh.jdautilities.menu

import com.jagrosh.jdautilities.waiter.EventWaiter
import net.dv8tion.jda.api.entities.Message

class SelectorBuilder(waiter: EventWaiter) : MenuBuilder<SelectorBuilder>(waiter) {
    private val options: MutableList<Selector.Entry> = mutableListOf()
    private var type: Selector.Type = Selector.Type.MESSAGE

    fun addOption(option: String, action: (Message) -> Unit): SelectorBuilder {
        options.add(Selector.Entry(option, action))
        return this
    }

    fun setType(type: Selector.Type): SelectorBuilder {
        this.type = type
        return this
    }

    override fun build(): Selector {
        return Selector(waiter, user, title, description, color, fields, type, options, timeout, unit, finally)
    }
}