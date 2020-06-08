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

@file:Suppress("NOTHING_TO_INLINE")

package gg.octave.bot.utils

import net.dv8tion.jda.api.entities.Member
import ninja.leaping.configurate.ConfigurationNode
import java.time.Duration
import java.util.concurrent.TimeUnit

inline operator fun ConfigurationNode.get(vararg nodes: Any): ConfigurationNode = this.getNode(*nodes)

fun Member.hasAnyRoleNamed(name: String) = roles.any { it.name == name }
fun Member.hasAnyRoleId(id: String) = id == guild.publicRole.id || roles.any { it.id == id }

fun String.toDuration(): Duration = Duration.ofNanos(parseDuration(this))

fun parseDuration(input: String): Long {
    val s = input.trim()
    val originalUnitString = getUnits(s)
    var unitString = originalUnitString
    val numberString = s.substring(0, s.length - unitString.length).trim()

    // this would be caught later anyway, but the error message
    // is more helpful if we check it here.
    if (numberString.isEmpty()) {
        throw RuntimeException("No number in duration value '$input'")
    }

    if (unitString.length > 2 && !unitString.endsWith("s"))
        unitString += "s"

    // note that this is deliberately case-sensitive
    val units: TimeUnit = when (unitString) {
        "", "ms", "millis", "milliseconds" -> TimeUnit.MILLISECONDS
        "us", "micros", "microseconds" -> TimeUnit.MICROSECONDS
        "ns", "nanos", "nanoseconds" -> TimeUnit.NANOSECONDS
        "d", "days" -> TimeUnit.DAYS
        "h", "hours" -> TimeUnit.HOURS
        "s", "seconds" -> TimeUnit.SECONDS
        "m", "minutes" -> TimeUnit.MINUTES
        else -> throw RuntimeException("Could not parse time unit '$originalUnitString' (try ns, us, ms, s, m, h, d)")
    }

    return try {
        if (numberString.matches("[+-]?[0-9]+".toRegex())) {
            units.toNanos(numberString.toLong())
        } else {
            val nanosInUnit = units.toNanos(1)
            (numberString.toDouble() * nanosInUnit).toLong()
        }
    } catch (e: NumberFormatException) {
        throw RuntimeException("Could not parse duration number '$numberString'")
    }
}

fun getDisplayValue(ms: Long, shorthand: Boolean = false): String {
    val seconds = ms / 1000 % 60
    val minutes = ms / (1000 * 60) % 60
    val hours = ms / (1000 * 60 * 60) % 24
    val days = ms / (1000 * 60 * 60 * 24)

    return when {
        shorthand && days > 0 -> String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds)
        shorthand && hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        shorthand -> String.format("%02d:%02d", minutes, seconds)
        days > 0 -> String.format("%d days, %d hours, %d minutes and %d seconds", days, hours, minutes, seconds)
        hours > 0 -> String.format("%d hours, %d minutes and %d seconds", hours, minutes, seconds)
        minutes > 0 -> String.format("%d minutes and %d seconds", minutes, seconds)
        else -> String.format("%d seconds", seconds)
    }
}

private fun getUnits(s: String): String {
    var i = s.length - 1
    while (i >= 0) {
        val c = s[i]
        if (!Character.isLetter(c))
            break
        i -= 1
    }
    return s.substring(i + 1)
}
