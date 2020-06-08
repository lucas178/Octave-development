package gg.octave.bot.utils.extensions

import gg.octave.bot.utils.getDisplayValue
import java.time.Duration

fun Duration.toHuman(shorthand: Boolean = false) = getDisplayValue(this.toMillis(), shorthand)
