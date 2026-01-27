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

package epox.webaom.ui.shortcuts.handlers;

import epox.webaom.AppContext;
import epox.webaom.ui.shortcuts.ShortcutCategory;
import epox.webaom.ui.shortcuts.ShortcutHandler;
import epox.webaom.ui.shortcuts.ShortcutInfo;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

public class HelpHandler implements ShortcutHandler, ShortcutInfo {
    @Override
    public boolean handle(KeyEvent event, JComponent source) {
        String helpText = AppContext.shortcutRegistry.generateHelpText();
        Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(source);
        JOptionPane.showMessageDialog(parentWindow, helpText, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    @Override
    public char keyChar() {
        return '?';
    }

    @Override
    public int keyCode() {
        return 0;
    }

    @Override
    public ShortcutCategory category() {
        return ShortcutCategory.GLOBAL;
    }

    @Override
    public String description() {
        return "Show keyboard shortcuts help";
    }

    @Override
    public ShortcutHandler handler() {
        return this;
    }
}
