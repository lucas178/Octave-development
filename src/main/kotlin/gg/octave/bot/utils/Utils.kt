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

package gg.octave.bot.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object Utils {
    private val TIME_PATTERN = Pattern.compile("(-?\\d+)\\s*((?:d(?:ays?)?)|(?:h(?:ours?)?)|(?:m(?:in(?:utes?)?)?)|(?:s(?:ec(?:onds?)?)?))?")
    private val alphaNumeric = ('a'..'z').union('A'..'Z').union('0'..'9')

    fun generateId(): String {
        return (0 until 5).map { alphaNumeric.random() }.joinToString("").toUpperCase()
    }

    fun parseTime(time: String): Long {
        val s = time.toLowerCase()
        val matcher = TIME_PATTERN.matcher(s)
        var ms = 0L

        while (matcher.find()) {
            val numStr = matcher.group(1)
            val unit = when (matcher.group(2)) {
                "d", "day", "days" -> TimeUnit.DAYS
                "h", "hour", "hours" -> TimeUnit.HOURS
                "m", "min", "minute", "minutes" -> TimeUnit.MINUTES
                "s", "sec", "second", "seconds" -> TimeUnit.SECONDS
                else -> TimeUnit.SECONDS
            }
            ms += unit.toMillis(numStr.toLong())
        }

        return ms
    }

    fun getTimestamp(ms: Long): String {
        val s = ms / 1000
        val m = s / 60
        val h = m / 60

        return when {
            h > 0 -> String.format("%02d:%02d:%02d", h, m % 60, s % 60)
            else -> String.format("%02d:%02d", m, s % 60)
        }
    }

    fun hasteBin(content: String): String? {
        return RequestUtil.jsonObject {
            url("https://hastebin.com/documents")
            header("User-Agent", "Octave")
            post(RequestBody.create(MediaType.get("text/plain"), content))
        }.thenApply { "https://hastebin.com/${it["key"]}" }.get()
    }
}
