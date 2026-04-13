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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        appendShortcutLines(sb, shortcutsByCategory.getOrDefault(ShortcutCategory.GLOBAL, List.of()));
        sb.append('\n');

        sb.append(JOB_CATEGORY_DISPLAY).append(":\n");
        appendShortcutLines(sb, shortcutsByCategory.getOrDefault(ShortcutCategory.JOB, List.of()));
        sb.append('\n');

        sb.append(NAVIGATION_CATEGORY_DISPLAY).append(":\n");
        appendShortcutLines(sb, shortcutsByCategory.getOrDefault(ShortcutCategory.NAVIGATION, List.of()));
        sb.append('\n');

        sb.append(OTHER_DISPLAY).append(":\n");
        sb.append("  Enter/Space - Show job info\n");
        sb.append("  Delete/BackSpace - Remove selected entries from the current tab\n");
        sb.append("  ESC - Stop worker thread");

        return sb.toString();
    }

    private static void appendShortcutLines(StringBuilder sb, List<ShortcutInfo> shortcuts) {
        Set<String> seenEntries = new LinkedHashSet<>();
        for (ShortcutInfo shortcut : shortcuts) {
            String entry = formatKey(shortcut) + "\u0000" + shortcut.description();
            if (!seenEntries.add(entry)) {
                continue;
            }
            sb.append("  ")
                    .append(formatKey(shortcut))
                    .append("  - ")
                    .append(shortcut.description())
                    .append('\n');
        }
    }

    private static String formatKey(ShortcutInfo info) {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        String baseKey = formatBaseKey(info, isMac);
        if (baseKey.equals("F5/Cmd+R")) {
            return baseKey;
        }
        String modifiers = formatModifiers(info.displayModifiers(), isMac);
        return modifiers + baseKey;
    }

    private static String formatBaseKey(ShortcutInfo info, boolean isMac) {
        if (info.keyChar() != '\0') {
            return String.valueOf(info.keyChar());
        } else if (info.keyCode() != 0) {
            return switch (info.keyCode()) {
                case java.awt.event.KeyEvent.VK_F5 -> isMac ? "F5/Cmd+R" : "F5";
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

    private static String formatModifiers(int modifiers, boolean isMac) {
        if (modifiers == 0) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            parts.add("Ctrl");
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            parts.add(isMac ? "Cmd" : "Meta");
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            parts.add(isMac ? "Option" : "Alt");
        }
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            parts.add("Shift");
        }
        return String.join("+", parts) + "+";
    }
}
