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

package gg.octave.bot.db.music;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import gg.octave.bot.Launcher;
import gg.octave.bot.db.ManagedObject;
import gg.octave.bot.utils.Utils;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomPlaylist extends ManagedObject {
    @JsonSerialize
    @JsonDeserialize
    private String author;

    @JsonSerialize
    @JsonDeserialize
    private String name;

    @JsonSerialize
    @JsonDeserialize
    private List<String> encodedTracks = new ArrayList<>();

    @JsonDeserialize
    private boolean imported = false;

    @ConstructorProperties("id")
    public CustomPlaylist(String id) {
        super(id, "customplaylists");
    }

    @JsonIgnore
    public String getName() {
        return name;
    }

    @JsonIgnore
    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getAuthor() {
        return author;
    }

    @JsonIgnore
    public void setAuthor(String author) {
        this.author = author;
    }

    @JsonIgnore
    public void setImported(boolean imported) {
        this.imported = imported;
    }

    @JsonIgnore
    public boolean isImported() {
        return imported;
    }

    @JsonIgnore
    public List<String> getEncodedTracks() {
        return encodedTracks;
    }

    @JsonIgnore
    public List<AudioTrack> getDecodedTracks() {
        return encodedTracks.stream()
                .map(Launcher.INSTANCE.getPlayers().getPlayerManager()::decodeAudioTrack)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public void addTrack(AudioTrack track) {
        encodedTracks.add(Launcher.INSTANCE.getPlayers().getPlayerManager().encodeAudioTrack(track));
    }

    @JsonIgnore
    public void addTracks(List<AudioTrack> tracks) {
        for (AudioTrack track : tracks) {
            encodedTracks.add(Launcher.INSTANCE.getPlayers().getPlayerManager().encodeAudioTrack(track));
        }
    }

    @JsonIgnore
    public void removeTrackAt(int index) {
        encodedTracks.remove(index);
    }

    @JsonIgnore
    public void setTracks(List<AudioTrack> tracks) {
        encodedTracks = tracks.stream()
                .map(Launcher.INSTANCE.getPlayers().getPlayerManager()::encodeAudioTrack)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public BasicAudioPlaylist toBasicAudioPlaylist() {
        return new BasicAudioPlaylist(getName(), getDecodedTracks(), null, false);
    }

    public static CustomPlaylist createWith(String authorId, String name) {
        String id = Utils.INSTANCE.generateId();

        if (Launcher.INSTANCE.getDatabase().getCustomPlaylistById(id) != null) {
            return createWith(authorId, name);
        }

        CustomPlaylist playlist = new CustomPlaylist(id);
        playlist.setAuthor(authorId);
        playlist.setName(name);
        return playlist;
    }
}
