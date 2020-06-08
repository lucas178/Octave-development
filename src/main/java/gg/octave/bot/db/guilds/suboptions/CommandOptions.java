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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties({"allowedChannels", "allowedUsers", "allowedRoles"})
public class CommandOptions {
    private boolean enabled = true;

    @JsonSerialize
    @JsonDeserialize(contentAs = String.class)
    private Set<String> disabledChannels;

    @JsonSerialize
    @JsonDeserialize(contentAs = String.class)
    private Set<String> disabledUsers;

    @JsonSerialize
    @JsonDeserialize(contentAs = String.class)
    private Set<String> disabledRoles;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonIgnore
    public Set<String> getDisabledChannels() {
        if (disabledChannels == null) {
            disabledChannels = new HashSet<>();
        }
        return disabledChannels;
    }

    @JsonIgnore
    public Set<String> getDisabledUsers() {
        if (disabledUsers == null) {
            disabledUsers = new HashSet<>();
        }
        return disabledUsers;
    }

    @JsonIgnore
    public Set<String> getDisabledRoles() {
        if (disabledRoles == null) {
            disabledRoles = new HashSet<>();
        }
        return disabledRoles;
    }

    public CommandOptions copy() {
        CommandOptions copy = new CommandOptions();
        copy.enabled = this.enabled;
        copy.disabledChannels = new HashSet<>(this.disabledChannels);
        copy.disabledRoles = new HashSet<>(this.disabledRoles);
        copy.disabledUsers = new HashSet<>(this.disabledUsers);
        return copy;
    }

    @JsonIgnore
    public Set<String> rawDisabledChannels() {
        return disabledChannels;
    }

    @JsonIgnore
    public Set<String> rawDisabledUsers() {
        return disabledUsers;
    }

    @JsonIgnore
    public Set<String> rawDisabledRoles() {
        return disabledRoles;
    }
}
