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

import java.util.Collections;
import java.util.Set;

public class CommandOptionsOverride extends CommandOptions {
    private final CommandOptions parent;
    private final CommandOptions child;

    public CommandOptionsOverride(CommandOptions child, CommandOptions parent) {
        this.parent = parent;
        this.child = child;
    }

    public CommandOptions getParent() {
        return parent;
    }

    public CommandOptions getChild() {
        return child;
    }

    @Override
    public boolean isEnabled() {
        return child != null ? child.isEnabled() : parent == null || parent.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }

    public boolean inheritToggle() {
        return child == null;
    }

    @Override
    public Set<String> getDisabledChannels() {
        if (child != null && child.rawDisabledChannels() != null)
            return Collections.unmodifiableSet(child.getDisabledChannels());
        else if (parent != null) return Collections.unmodifiableSet(parent.getDisabledChannels());
        else return Collections.emptySet();
    }

    public boolean inheritChannels() {
        return child == null || child.rawDisabledChannels() == null;
    }

    @Override
    public Set<String> getDisabledRoles() {
        if (child != null && child.rawDisabledRoles() != null)
            return Collections.unmodifiableSet(child.getDisabledRoles());
        else if (parent != null) return Collections.unmodifiableSet(parent.getDisabledRoles());
        else return Collections.emptySet();
    }

    public boolean inheritRoles() {
        return child == null || child.rawDisabledRoles() == null;
    }

    @Override
    public Set<String> getDisabledUsers() {
        if (child != null && child.rawDisabledUsers() != null)
            return Collections.unmodifiableSet(child.getDisabledUsers());
        else if (parent != null) return Collections.unmodifiableSet(parent.getDisabledUsers());
        else return Collections.emptySet();
    }

    public boolean inheritUsers() {
        return child == null || child.rawDisabledUsers() == null;
    }
}
