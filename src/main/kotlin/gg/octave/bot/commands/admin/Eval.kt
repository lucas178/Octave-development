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

package gg.octave.bot.commands.admin

import gg.octave.bot.Launcher
import gg.octave.bot.utils.extensions.existingManager
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.entities.Cog
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import java.util.concurrent.CompletableFuture

class Eval : Cog {
    private val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine

    @Command(description = "Evaluate Kotlin code.", developerOnly = true)
    fun eval(ctx: Context, @Greedy code: String) {
        val stripped = code.replace("^```\\w+".toRegex(), "").removeSuffix("```")

        val bindings = mutableMapOf(
            "bot" to Launcher,
            "launcher" to Launcher,
            "db" to Launcher.database,
            "ctx" to ctx,
            "jda" to ctx.jda,
            "sm" to ctx.jda.shardManager!!
        )

        ctx.existingManager?.let { bindings["mm"] = it }

        val bindString = bindings.map { "val ${it.key} = bindings[\"${it.key}\"] as ${it.value.javaClass.kotlin.qualifiedName}" }
            .joinToString("\n")
        val bind = engine.createBindings()
        bind.putAll(bindings)

        try {
            val result = engine.eval("$bindString\n$stripped", bind)
                ?: return ctx.message.addReaction("👌").queue()

            if (result is CompletableFuture<*>) {
                ctx.messageChannel.sendMessage("```\nCompletableFuture<Pending>```").queue { m ->
                    result.whenComplete { r, ex ->
                        val post = ex ?: r
                        m.editMessage("```\n$post```").queue()
                    }
                }
            } else {
                ctx.send("```\n${result.toString().take(1950)}```")
            }
        } catch (e: Exception) {
            ctx.send("An exception occurred.\n```\n${e.localizedMessage}```")
        }
    }
}