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

package gg.octave.bot.music.utils

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException

object LpErrorTranslator {
    fun ex(c: String.() -> Boolean) = c

    private val errors = mapOf(
        ex { contains("copyright") || contains("country") || contains("content") } to "This video is not playable in the bot's region.",
        ex { contains("403") } to "Access to the video was restricted.",
        ex { contains("supported formats") } to "This video cannot be streamed."
        //ex { contains("timed out") || contains("connection reset") || contains("connection refused") || contains("failed to respond") } to "<connection issues>"
    )

    fun rootCauseOf(exception: Throwable): Throwable {
        return exception.cause?.let { rootCauseOf(it) }
            ?: exception
    }

    fun translate(exception: FriendlyException): String {
        val rootCause = rootCauseOf(exception)
        val lowerCase = (rootCause.localizedMessage ?: rootCause.toString()).toLowerCase()

        return errors.entries
            .firstOrNull { it.key(lowerCase) }
            ?.value
            ?: rootCause.localizedMessage
            ?: "Unknown cause, try again?"
        // Or do we default to some generic message about how the error has been logged and we'll look into it?
    }
}
