/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.webaom.ui.shortcuts;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShortcutRegistry {
    private static final String GLOBAL_CATEGORY_DISPLAY = "Global Shortcuts";
    private static final String JOB_CATEGORY_DISPLAY = "Job Actions";
    private static final String NAVIGATION_CATEGORY_DISPLAY = "Navigation";
    private static final String OTHER_DISPLAY = "Other";

    private final Map<ShortcutCategory, List<ShortcutInfo>> shortcutsByCategory = new LinkedHashMap<>();
    private final List<ShortcutInfo> otherShortcuts = new ArrayList<>();

    public void register(ShortcutInfo shortcut) {
        if (shortcut.category() == null) {
            otherShortcuts.add(shortcut);
        } else {
            shortcutsByCategory
                    .computeIfAbsent(shortcut.category(), k -> new ArrayList<>())
                    .add(shortcut);
        }
    }

    public void unregister(ShortcutInfo shortcut) {
        if (shortcut.category() == null) {
            otherShortcuts.remove(shortcut);
        } else {
            List<ShortcutInfo> list = shortcutsByCategory.get(shortcut.category());
            if (list != null) {
                list.remove(shortcut);
            }
        }
    }

    public List<ShortcutInfo> getShortcuts(ShortcutCategory category) {
        return shortcutsByCategory.getOrDefault(category, List.of());
    }

    public List<ShortcutInfo> getAllShortcuts() {
        List<ShortcutInfo> all = new ArrayList<>();
        shortcutsByCategory.values().forEach(all::addAll);
        all.addAll(otherShortcuts);
        return all;
    }

    public String generateHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("WebAOM Keyboard Shortcuts\n\n");

        sb.append(GLOBAL_CATEGORY_DISPLAY).append(":\n");
        for (ShortcutInfo s : shortcutsByCategory.getOrDefault(ShortcutCategory.GLOBAL, List.of())) {
            sb.append("  ")
                    .append(formatKey(s))
                    .append("  - ")
                    .append(s.description())
                    .append('\n');
        }
        sb.append('\n');

        sb.append(JOB_CATEGORY_DISPLAY).append(":\n");
        for (ShortcutInfo s : shortcutsByCategory.getOrDefault(ShortcutCategory.JOB, List.of())) {
            sb.append("  ")
                    .append(formatKey(s))
                    .append("  - ")
                    .append(s.description())
                    .append('\n');
        }
        sb.append('\n');

        sb.append(NAVIGATION_CATEGORY_DISPLAY).append(":\n");
        for (ShortcutInfo s : shortcutsByCategory.getOrDefault(ShortcutCategory.NAVIGATION, List.of())) {
            sb.append("  ")
                    .append(formatKey(s))
                    .append("  - ")
                    .append(s.description())
                    .append('\n');
        }
        sb.append('\n');

        sb.append(OTHER_DISPLAY).append(":\n");
        sb.append("  Enter/Space - Show job info\n");
        sb.append("  Delete/BackSpace - Delete selected jobs");

        return sb.toString();
    }

    private static String formatKey(ShortcutInfo info) {
        if (info.keyChar() != '\0') {
            return String.valueOf(info.keyChar());
        } else if (info.keyCode() != 0) {
            return switch (info.keyCode()) {
                case java.awt.event.KeyEvent.VK_F5 -> "F5";
                case java.awt.event.KeyEvent.VK_F9 -> "F9";
                case java.awt.event.KeyEvent.VK_LEFT -> "Left";
                case java.awt.event.KeyEvent.VK_RIGHT -> "Right";
                case java.awt.event.KeyEvent.VK_ESCAPE -> "ESC";
                case java.awt.event.KeyEvent.VK_DELETE -> "Delete";
                case java.awt.event.KeyEvent.VK_BACK_SPACE -> "BackSpace";
                default -> KeyEvent.getKeyText(info.keyCode());
            };
        }
        return "?";
    }
}
