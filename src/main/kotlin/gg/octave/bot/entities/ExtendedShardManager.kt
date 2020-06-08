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

package gg.octave.bot.entities

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import gg.octave.bot.Launcher
import gg.octave.bot.utils.IntentHelper
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.ConcurrentSessionController
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.*

class ExtendedShardManager(private val shardManager: ShardManager) : ShardManager by shardManager {
    fun openPrivateChannel(userId: Long): RestAction<PrivateChannel> {
        return shards.first { it != null }.openPrivateChannelById(userId)
    }

    companion object {
        private val sessionController = ConcurrentSessionController()

        fun create(token: String, apply: DefaultShardManagerBuilder.() -> Unit = {}): ExtendedShardManager {
            RestAction.setDefaultFailure(ErrorResponseException.ignore(
                RestAction.getDefaultFailure(),
                ErrorResponse.UNKNOWN_MESSAGE
            ))

            sessionController.setConcurrency(Launcher.configuration.bucketFactor)

            return DefaultShardManagerBuilder.create(token, IntentHelper.enabledIntents)
                .apply {
                    val configuration = Launcher.configuration
                    val credentials = Launcher.credentials

                    // General
                    setActivityProvider { Activity.playing(configuration.game.format(it)) }

                    // Gateway
                    setSessionController(sessionController)
                    setShardsTotal(credentials.totalShards)
                    setShards(credentials.shardStart, Launcher.credentials.shardEnd - 1)
                    setMaxReconnectDelay(32)

                    // Audio
                    setAudioSendFactory(NativeAudioSendFactory(configuration.bufferDuration))

                    // Performance
                    setBulkDeleteSplittingEnabled(false)
                    disableCache(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS))
                    setMemberCachePolicy(MemberCachePolicy.VOICE)
                    setChunkingFilter(ChunkingFilter.NONE)
                }
                .apply(apply)
                .build()
                .let(::ExtendedShardManager)
        }
    }
}
