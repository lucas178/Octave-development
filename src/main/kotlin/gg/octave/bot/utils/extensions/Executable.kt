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

import gg.octave.bot.entities.framework.Usages
import me.devoxin.flight.internal.entities.Executable
import net.dv8tion.jda.api.entities.*
import java.net.URL
import java.time.Duration
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

fun Executable.generateDefaultUsage(): String {
    return buildString {
        for (arg in this@generateDefaultUsage.arguments) {
            val value = when (arg.type) {
                String::class.java -> "\"some text\""
                Int::class, java.lang.Integer::class.java, Long::class.java, java.lang.Long::class.java -> "0"
                Double::class.java, java.lang.Double::class.java -> "0.0"
                Member::class.java, User::class.java -> "@User"
                Role::class.java -> "@DJ"
                TextChannel::class.java -> "#general"
                VoiceChannel::class.java -> "Music"
                Boolean::class.java, java.lang.Boolean::class.java -> "yes"
                Duration::class.java -> "20m"
                URL::class.java -> "https://www.youtube.com/watch?v=2JBNbhIYR18"
                else -> {
                    if (arg.type.isEnum) {
                        arg.type.enumConstants.first().toString().toLowerCase()
                    } else {
                        "[Unknown Type, report to devs]"
                    }
                }
            }
            append(value)
            append(" ")
        }
    }.trim()
}

@ExperimentalStdlibApi
fun Executable.generateExampleUsage(commandLayout: String): String {
    return when {
        this.method.hasAnnotation<Usages>() -> this.method.findAnnotation<Usages>()!!.usages.joinToString("\n") { "`$commandLayout $it`" }
        else -> "`" + "$commandLayout ${generateDefaultUsage()}".trim() + "`"
    }
}
