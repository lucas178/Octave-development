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

package gg.octave.bot.db.guilds.suboptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties("djRole")
public class MusicData {
    private Set<String> musicChannels;
    private int volume = 100;
    private boolean announce = true;
    private int maxQueueSize;
    private long maxSongLength;
    private long voteSkipCooldown;
    private long voteSkipDuration;
    private boolean isVotePlay;
    private long votePlayCooldown;
    private long votePlayDuration;
    private String announcementChannel;
    private boolean disableDj;
    private Set<String> djRoles = new HashSet<>();
    private boolean allDayMusic;

    @NotNull
    public final Set<String> getChannels() {
        if (musicChannels == null) musicChannels = new HashSet<>();
        return musicChannels;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public void setMaxSongLength(long maxSongLength) {
        this.maxSongLength = maxSongLength;
    }

    public long getMaxSongLength() {
        return maxSongLength;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int musicVolume) {
        this.volume = musicVolume;
    }

    public boolean getAnnounce() {
        return announce;
    }

    public void setAnnounce(boolean announce) {
        this.announce = announce;
    }

    public long getVoteSkipCooldown() {
        return voteSkipCooldown;
    }

    public void setVoteSkipCooldown(long voteSkipCooldown) {
        this.voteSkipCooldown = voteSkipCooldown;
    }

    public long getVoteSkipDuration() {
        return voteSkipDuration;
    }

    public void setVoteSkipDuration(long voteSkipDuration) {
        this.voteSkipDuration = voteSkipDuration;
    }

    public boolean isVotePlay() {
        return isVotePlay;
    }

    public void setVotePlay(boolean votePlay) {
        isVotePlay = votePlay;
    }

    public long getVotePlayCooldown() {
        return votePlayCooldown;
    }

    public void setVotePlayCooldown(long votePlayCooldown) {
        this.votePlayCooldown = votePlayCooldown;
    }

    public long getVotePlayDuration() {
        return votePlayDuration;
    }

    public void setVotePlayDuration(long votePlayDuration) {
        this.votePlayDuration = votePlayDuration;
    }

    public String getAnnouncementChannel() {
        return announcementChannel;
    }

    public void setAnnouncementChannel(String announcementChannel) {
        this.announcementChannel = announcementChannel;
    }

    public boolean isDisableDj() {
        return disableDj;
    }

    public void setDisableDj(boolean disableDj) {
        this.disableDj = disableDj;
    }

    public Set<String> getDjRoles() {
        return djRoles;
    }

    public void setDjRoles(Set<String> djRoles) {
        this.djRoles = djRoles;
    }

    public boolean isAllDayMusic() {
        return allDayMusic;
    }

    public void setAllDayMusic(boolean allDayMusic) {
        this.allDayMusic = allDayMusic;
    }
}
