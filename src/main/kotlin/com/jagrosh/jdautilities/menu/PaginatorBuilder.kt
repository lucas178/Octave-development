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

import com.google.common.collect.Lists
import com.jagrosh.jdautilities.waiter.EventWaiter

class PaginatorBuilder(waiter: EventWaiter) : MenuBuilder<PaginatorBuilder>(waiter) {
    private val items: MutableList<String> = mutableListOf()
    private var emptyMessage: String? = null

    private var itemsPerPage = 10

    inline fun entry(lazy: () -> String): PaginatorBuilder {
        return addEntry(lazy())
    }

    fun addEntry(item: String): PaginatorBuilder {
        this.items.add(item)
        return this
    }

    inline fun empty(lazy: () -> String): PaginatorBuilder {
        return setEmptyMessage(lazy())
    }

    fun setEmptyMessage(emptyMessage: String?): PaginatorBuilder {
        this.emptyMessage = emptyMessage
        return this
    }

    fun addAll(items: Collection<String>): PaginatorBuilder {
        this.items.addAll(items)
        return this
    }

    fun setItemsPerPage(itemsPerPage: Int): PaginatorBuilder {
        this.itemsPerPage = itemsPerPage
        return this
    }

    override fun build(): Paginator {
        return Paginator(waiter, user, title, description, color, fields, emptyMessage,
            if (items.isEmpty()) emptyList() else Lists.partition(items, itemsPerPage), timeout, unit, finally)
    }
}