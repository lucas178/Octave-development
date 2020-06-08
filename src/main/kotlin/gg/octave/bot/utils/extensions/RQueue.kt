/*
 * MIT License
 *
 * Copyright (c) 2020 Melms Media LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package gg.octave.bot.utils.extensions

import org.redisson.api.RQueue

fun RQueue<String>.removeAt(index: Int): String {
    var iterIndex = 0
    val iterator = this.iterator()
    var value = ""
    while (iterator.hasNext() && iterIndex <= index) {
        val currentValue = iterator.next()
        if (iterIndex == index) {
            value = currentValue
            iterator.remove()
        }

        iterIndex++
    }

    return value
}

// I don't like duplicate code but this should theoretically be more efficient
// for removing at multiple indexes, than using .removeAt(index).
fun RQueue<String>.removeAll(indexes: Collection<Int>): Int {
    val biggestIndex = indexes.max()!!
    var iterIndex = 0
    val iterator = this.iterator()
    var removedCount = 0

    while (iterator.hasNext() && iterIndex <= biggestIndex) {
        iterator.next()
        if (iterIndex in indexes) {
            removedCount++
            iterator.remove()
        }

        iterIndex++
    }

    return removedCount
}

fun <T> RQueue<T>.insertAt(index: Int, element: T) {
    val elements = this.readAll()
    elements.add(index, element)
    this.clear()
    this.addAll(elements)
}

/**
 * @return The element that was moved.
 */
fun <T> RQueue<T>.move(index: Int, to: Int): T {
    val elements = this.readAll()
    val temp = elements.removeAt(index)
    elements.add(to, temp)
    this.clear()
    this.addAll(elements)

    return temp
}

fun <T> RQueue<T>.shuffle() {
    (this as MutableList<*>).shuffle()
}