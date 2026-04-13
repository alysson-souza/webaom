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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import org.junit.jupiter.api.Test;

class ShortcutRegistryTest {
    @Test
    void generateHelpText_describesScopedRemoveShortcut() {
        ShortcutRegistry registry = new ShortcutRegistry();

        String helpText = registry.generateHelpText();

        assertTrue(helpText.contains("Delete/BackSpace - Remove selected entries from the current tab"));
    }

    @Test
    void generateHelpText_explicitlyShowsShiftModifiedCharacterShortcuts() {
        ShortcutRegistry registry = new ShortcutRegistry();
        registry.register(new TestShortcut('A', 0, ShortcutCategory.GLOBAL, "Open anime URL", 0));
        registry.register(new TestShortcut('?', 0, ShortcutCategory.GLOBAL, "Show keyboard shortcuts help", 0));

        String helpText = registry.generateHelpText();

        assertAll(
                () -> assertTrue(helpText.contains("Shift+A  - Open anime URL")),
                () -> assertTrue(helpText.contains("Shift+?  - Show keyboard shortcuts help")));
    }

    @Test
    void generateHelpText_explicitlyShowsDeclaredCtrlModifiers() {
        ShortcutRegistry registry = new ShortcutRegistry();
        registry.register(new TestShortcut(
                '\0', KeyEvent.VK_R, ShortcutCategory.GLOBAL, "Refresh alternate view", InputEvent.CTRL_DOWN_MASK));

        String helpText = registry.generateHelpText();

        assertTrue(helpText.contains("Ctrl+R  - Refresh alternate view"));
    }

    private record TestShortcut(
            char keyChar, int keyCode, ShortcutCategory category, String description, int displayModifiers)
            implements ShortcutInfo {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> true;
        }

        @Override
        public int displayModifiers() {
            return displayModifiers != 0 ? displayModifiers : ShortcutInfo.super.displayModifiers();
        }
    }
}
