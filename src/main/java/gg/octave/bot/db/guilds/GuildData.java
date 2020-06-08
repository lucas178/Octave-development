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
import gg.octave.bot.db.guilds.suboptions.CommandData;
import gg.octave.bot.db.guilds.suboptions.IgnoredData;
import gg.octave.bot.db.guilds.suboptions.MusicData;
import gg.octave.bot.db.guilds.suboptions.RoleData;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

public class GuildData extends ManagedObject {
    @JsonIgnore
    public Object logData;
    @JsonSerialize
    @JsonDeserialize(as = CommandData.class)
    private CommandData commandData;
    @JsonSerialize
    @JsonDeserialize(as = RoleData.class)
    private RoleData roleData;
    @JsonSerialize
    @JsonDeserialize(as = IgnoredData.class)
    private IgnoredData ignoredData;
    @JsonSerialize
    @JsonDeserialize(as = MusicData.class)
    private MusicData musicData;
    @JsonSerialize(keyAs = String.class, contentAs = Long.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Long.class)
    private Map<String, Long> premiumKeys;

    @ConstructorProperties("id")
    public GuildData(String id) {
        super(id, "guilds_v2");
    }

    public GuildData addPremiumKey(String id, long duration) {
        if (premiumKeys == null) premiumKeys = new HashMap<>();

        if (!isPremium()) {
            premiumKeys.clear();
            premiumKeys.put("init", System.currentTimeMillis());
        }

        premiumKeys.put(id, duration);
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

    @JsonIgnore
    public CommandData getCommand() {
        if (commandData == null) commandData = new CommandData();
        return commandData;
    }

    @JsonIgnore
    public IgnoredData getIgnored() {
        if (ignoredData == null) ignoredData = new IgnoredData();
        return ignoredData;
    }

    @JsonIgnore
    public MusicData getMusic() {
        if (musicData == null) musicData = new MusicData();
        return musicData;
    }

    @JsonIgnore
    public RoleData getRoles() {
        if (roleData == null) roleData = new RoleData();
        return roleData;
    }

    public void reset() {
        commandData = null;
        ignoredData = null;
        musicData = null;
        roleData = null;
        logData = null;
    }
}
