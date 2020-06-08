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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CommandData {
    private String prefix;
    private String djRole;
    private boolean autoDelete = false;
    private boolean invokeDelete = false;
    private long autoDeleteDelay = 0L;
    private boolean adminBypass = false;
    private boolean djOnlyMode;

    @JsonSerialize(keyAs = Integer.class, contentAs = CommandOptions.class)
    private Map<Integer, CommandOptions> options;

    @JsonSerialize(keyAs = Integer.class, contentAs = CommandOptions.class)
    private Map<Integer, CommandOptions> categoryOptions;

    @Nullable
    public String getPrefix() {
        return prefix;
    }

    @Nullable
    public String getDJRole() {
        return djRole;
    }

    public String setDJRole(String djRole) {
        return this.djRole = djRole;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public boolean isInvokeDelete() {
        return invokeDelete;
    }

    public void setInvokeDelete(boolean invokeDelete) {
        this.invokeDelete = invokeDelete;
    }

    public boolean isAdminBypass() {
        return adminBypass;
    }

    public void setAdminBypass(boolean adminBypass) {
        this.adminBypass = adminBypass;
    }

    public Map<Integer, CommandOptions> getOptions() {
        if (options == null) {
            options = new HashMap<>();
        }
        return options;
    }

    public Map<Integer, CommandOptions> getCategoryOptions() {
        if (categoryOptions == null) {
            categoryOptions = new HashMap<>();
        }
        return categoryOptions;
    }

    public boolean isDjOnlyMode() {
        return djOnlyMode;
    }

    public void setDjOnlyMode(boolean djOnlyMode) {
        this.djOnlyMode = djOnlyMode;
    }

    public long getAutoDeleteDelay() {
        return autoDeleteDelay;
    }

    public void setAutoDeleteDelay(long autoDeleteDelay) {
        this.autoDeleteDelay = autoDeleteDelay;
    }
}
