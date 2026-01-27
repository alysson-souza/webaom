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

package epox.webaom.ui;

import epox.webaom.ui.shortcuts.ShortcutHandler;
import epox.webaom.ui.shortcuts.ShortcutRegistry;
import epox.webaom.ui.shortcuts.handlers.HelpHandler;
import epox.webaom.ui.shortcuts.handlers.RefreshHandler;
import epox.webaom.ui.shortcuts.handlers.ResetHandler;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public class GlobalKeyboardShortcuts {
    private final JRootPane rootPane;

    public GlobalKeyboardShortcuts(JRootPane rootPane, ShortcutRegistry registry) {
        this.rootPane = rootPane;
        registerShortcuts(registry);
    }

    private void registerShortcuts(ShortcutRegistry registry) {
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");

        HelpHandler helpHandler = new HelpHandler();
        RefreshHandler refreshHandler = new RefreshHandler();
        ResetHandler resetHandler = new ResetHandler();

        registry.register(helpHandler);
        registry.register(refreshHandler);
        registry.register(resetHandler);

        inputMap.put(KeyStroke.getKeyStroke('?'), "help");
        actionMap.put("help", new ShortcutAction(helpHandler.handler(), '?'));

        String refreshKey = isMac ? "meta R" : "F5";
        inputMap.put(KeyStroke.getKeyStroke(refreshKey), "refresh");
        actionMap.put("refresh", new ShortcutAction(refreshHandler.handler(), KeyEvent.VK_F5));
        if (isMac) {
            inputMap.put(KeyStroke.getKeyStroke("F5"), "refresh");
        }

        inputMap.put(KeyStroke.getKeyStroke("F9"), "reset");
        actionMap.put("reset", new ShortcutAction(resetHandler.handler(), KeyEvent.VK_F9));
    }

    private static class ShortcutAction extends AbstractAction {
        private final ShortcutHandler handler;
        private final char keyChar;
        private final int keyCode;

        ShortcutAction(ShortcutHandler handler, char keyChar) {
            this.handler = handler;
            this.keyChar = keyChar;
            this.keyCode = 0;
        }

        ShortcutAction(ShortcutHandler handler, int keyCode) {
            this.handler = handler;
            this.keyChar = '\0';
            this.keyCode = keyCode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JComponent component) {
                KeyEvent ke;
                if (keyChar != '\0') {
                    ke = new KeyEvent(
                            component,
                            KeyEvent.KEY_TYPED,
                            System.currentTimeMillis(),
                            0,
                            KeyEvent.VK_UNDEFINED,
                            keyChar);
                } else {
                    ke = new KeyEvent(
                            component,
                            KeyEvent.KEY_PRESSED,
                            System.currentTimeMillis(),
                            0,
                            keyCode,
                            KeyEvent.CHAR_UNDEFINED);
                }
                handler.handle(ke, component);
            }
        }
    }
}
