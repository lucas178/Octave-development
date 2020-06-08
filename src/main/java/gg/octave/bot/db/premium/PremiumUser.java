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
import com.rethinkdb.net.Cursor;
import gg.octave.bot.Launcher;
import gg.octave.bot.db.ManagedObject;
import gg.octave.bot.db.music.CustomPlaylist;

import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.List;

public class PremiumUser extends ManagedObject {
    // The ID of the user that enabled premium for the guild.
    @JsonSerialize
    @JsonDeserialize
    private double pledgeAmount;

    @ConstructorProperties("id")
    public PremiumUser(String id) {
        super(id, "premiumusers");
    }

    @JsonIgnore
    public PremiumUser setPledgeAmount(double pledgeAmount) {
        this.pledgeAmount = pledgeAmount;
        return this;
    }

    @JsonIgnore
    public long getIdLong() {
        return Long.parseLong(getId());
    }

    @JsonIgnore
    public double getPledgeAmount() {
        return pledgeAmount;
    }

    @JsonIgnore
    @Nullable
    public Cursor<PremiumGuild> getPremiumGuilds() {
        return Launcher.INSTANCE.getDatabase().getPremiumGuilds(getId());
    }

    @JsonIgnore
    public List<PremiumGuild> getPremiumGuildsList() {
        Cursor<PremiumGuild> cursor = getPremiumGuilds();
        return cursor == null ? List.of() : cursor.toList();
    }

    @JsonIgnore
    @Nullable
    public Cursor<CustomPlaylist> getCustomPlaylists() {
        return Launcher.INSTANCE.getDatabase().getCustomPlaylists(getId());
    }

    @JsonIgnore
    public int getTotalPremiumGuildQuota() {
        if (Launcher.INSTANCE.getConfiguration().getAdmins().contains(Long.parseLong(getId()))) {
            return 99999;
        } else if (pledgeAmount >= 20.0) {  // base: 5 servers + 1 for every extra $3
            int extra = (int) ((pledgeAmount - 20) / 3);
            return 5 + extra;
        } else if (pledgeAmount >= 10.0) {
            return 2;
        } else if (pledgeAmount >= 5) {
            return 1;
        } else {
            return 0;
        }
    }

    @JsonIgnore
    public int getRemainingPremiumGuildQuota() {
        Cursor<PremiumGuild> cursor = getPremiumGuilds();

        if (cursor == null) {
            return 0;
            // In case of database error, return 0 to ensure users can't exploit malfunctions.
        }

        List<PremiumGuild> redeemedGuilds = cursor.toList();
        return getTotalPremiumGuildQuota() - redeemedGuilds.size();
    }

    @JsonIgnore
    public int getTotalCustomPlaylistQuota() {
        if (Launcher.INSTANCE.getConfiguration().getAdmins().contains(Long.parseLong(getId()))) {
            return 99999;
        } else if (isPremium()) {
            return 99999;
        } else {
            return 5;
        }
    }

    @JsonIgnore
    public int getRemainingCustomPlaylistQuota() {
        Cursor<CustomPlaylist> cursor = getCustomPlaylists();

        if (cursor == null) {
            return 0;
            // In case of database error, return 0 to ensure users can't exploit malfunctions.
        }

        List<CustomPlaylist> customPlaylists = cursor.toList();
        return getTotalCustomPlaylistQuota() - customPlaylists.size();
    }

    @JsonIgnore
    public boolean isPremium() {
        return pledgeAmount >= 5;
    }
}
