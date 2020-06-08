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

package gg.octave.bot.db.guilds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gg.octave.bot.db.ManagedObject;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

public class UserData extends ManagedObject {
    @JsonSerialize(keyAs = String.class, contentAs = Long.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Long.class)
    private Map<String, Long> premiumKeys;

    @ConstructorProperties({"id"})
    public UserData(String id) {
        super(id, "user_data");
    }

    public UserData addPremiumKey(String id, long duration) {
        if (premiumKeys == null) premiumKeys = new HashMap<>();

        if (!isPremium()) {
            premiumKeys.clear();
            premiumKeys.put("init", System.currentTimeMillis());
        }

        premiumKeys.put(id, duration);

        return this;
    }

    public UserData removePremiumKey(String id) {
        if (premiumKeys.containsKey(id)) {
            premiumKeys.remove(id);
            return this;
        }
        return this;
    }

    @JsonIgnore
    public Map<String, Long> getPremiumKeys() {
        if (premiumKeys == null) premiumKeys = new HashMap<>();
        return premiumKeys;
    }

    @JsonIgnore
    public long getPremiumUntil() {
        if (premiumKeys == null || premiumKeys.isEmpty()) {
            return 0;
        }

        long premiumUntil = 0;
        for (long duration : premiumKeys.values()) {
            premiumUntil += duration;
        }
        return premiumUntil;
    }

    @JsonIgnore
    public final long remainingPremium() {
        return this.isPremium() ? getPremiumUntil() - System.currentTimeMillis() : 0L;
    }

    @JsonIgnore
    public boolean isPremium() {
        return System.currentTimeMillis() < getPremiumUntil();
    }

}
