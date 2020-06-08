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

package gg.octave.bot.db.premium;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gg.octave.bot.Launcher;
import gg.octave.bot.db.ManagedObject;

import java.beans.ConstructorProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class PremiumGuild extends ManagedObject {
    // The ID of the user that enabled premium for the guild.
    @JsonSerialize
    @JsonDeserialize
    private String redeemer;

    // The epoch timestamp of when premium status expires.
    @JsonSerialize
    @JsonDeserialize
    private long added;

    @ConstructorProperties("id")
    public PremiumGuild(String id) {
        super(id, "premiumguilds");
    }

    @JsonIgnore
    public PremiumGuild setRedeemer(String redeemer) {
        this.redeemer = redeemer;
        return this;
    }

    @JsonIgnore
    public PremiumGuild setAdded(long added) {
        this.added = added;
        return this;
    }

    @JsonIgnore
    public String getRedeemerId() {
        return redeemer;
    }

    @JsonIgnore
    public PremiumUser getRedeemer() {
        return Launcher.INSTANCE.getDatabase().getPremiumUser(getRedeemerId());
    }

    @JsonIgnore
    public int getQueueSizeQuota() {
        double pledgeAmount = getRedeemer().getPledgeAmount();
        if (Launcher.INSTANCE.getConfiguration().getAdmins().contains(Long.parseLong(getRedeemerId())) || pledgeAmount >= 10) {
            return Integer.MAX_VALUE;
        } else if (pledgeAmount >= 5) {
            return 500;
        } else {
            return Launcher.INSTANCE.getConfiguration().getQueueLimit();
        }
    }

    @JsonIgnore
    public long getSongLengthQuota() {
        double pledgeAmount = getRedeemer().getPledgeAmount();
        if (Launcher.INSTANCE.getConfiguration().getAdmins().contains(Long.parseLong(getRedeemerId()))) {
            return Integer.MAX_VALUE;
        } else if (pledgeAmount >= 10) {
            return TimeUnit.MINUTES.toMillis(720);
        } else if (pledgeAmount >= 5) {
            return TimeUnit.MINUTES.toMillis(360);
        } else {
            return Launcher.INSTANCE.getConfiguration().getDurationLimit().toMillis();
        }
    }


    @JsonIgnore
    public long getDaysSinceAdded() {
        Instant current = Instant.now();
        Instant then = Instant.ofEpochMilli(added);

        return Duration.between(then, current).toDays();
    }
}
