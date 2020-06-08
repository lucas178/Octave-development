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

package gg.octave.bot.music.filters

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import gg.octave.bot.music.settings.BoostSetting

class DSPFilter(private val player: AudioPlayer) {
    // Equalizer properties
    var bassBoost = BoostSetting.OFF
        set(value) {
            field = value
            applyFilters()
        }

    // Karaoke properties
    val karaokeEnable: Boolean
        get() = kLevel > 0.0f

    var kLevel = 0.0f
        set(value) {
            field = value
            applyFilters()
        }
    var kFilterBand = 220f
        set(value) {
            field = value
            applyFilters()
        }
    var kFilterWidth = 100f
        set(value) {
            field = value
            applyFilters()
        }

    // Timescale properties
    val timescaleEnable: Boolean
        get() = tsSpeed != 1.0 || tsPitch != 1.0 || tsRate != 1.0

    var tsSpeed = 1.0
        set(value) {
            field = value
            applyFilters()
        }
    var tsPitch = 1.0
        set(value) {
            field = value
            applyFilters()
        }
    var tsRate = 1.0
        set(value) {
            field = value
            applyFilters()
        }

    // Tremolo properties
    val tremoloEnable: Boolean
        get() = tDepth > 0.0f

    var tDepth = 0.0f
        set(value) {
            field = value
            applyFilters()
        }
    var tFrequency = 2f
        set(value) {
            field = value
            applyFilters()
        }

    fun buildFilters(configs: List<FilterConfig<*>>, format: AudioDataFormat,
                     output: UniversalPcmAudioFilter): List<AudioFilter> {
        if (configs.isEmpty()) {
            return emptyList()
        }

        val filters = mutableListOf<FloatPcmAudioFilter>()

        for (filter in configs) { // Last filter writes to output.
            val built = if (filters.isEmpty()) { // First (read: last) filter
                filter.build(output, format)
            } else {
                filter.build(filters.last(), format)
            }
            filters.add(built)
        }

        return filters.reversed()
    }

    fun applyFilters() {
        player.setFilterFactory { _, format, output ->
            val filterConfigs = mutableListOf<FilterConfig<*>>()

            if (karaokeEnable) {
                val config = KaraokeFilter().configure {
                    level = kLevel
                    filterBand = kFilterBand
                    filterWidth = kFilterWidth
                }
                filterConfigs.add(config)
            }

            if (timescaleEnable) {
                val config = TimescaleFilter().configure {
                    pitch = tsPitch
                    speed = tsSpeed
                    rate = tsRate
                }
                filterConfigs.add(config)
            }

            if (tremoloEnable) {
                val config = TremoloFilter().configure {
                    depth = tDepth
                    frequency = tFrequency
                }
                filterConfigs.add(config)
            }

            if (bassBoost != BoostSetting.OFF) {
                val config = EqualizerFilter().configure {
                    setGain(0, bassBoost.band1)
                    setGain(1, bassBoost.band2)
                }
                filterConfigs.add(config)
            }

            return@setFilterFactory buildFilters(filterConfigs, format, output)
        }
    }

    fun clearFilters() {
        player.setFilterFactory(null)

        bassBoost = BoostSetting.OFF
        kLevel = 0.0f
        tDepth = 0.0f

        tsPitch = 1.0
        tsRate = 1.0
        tsSpeed = 1.0
    }
}
