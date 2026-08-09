/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import epox.webaom.AppContext;
import epox.webaom.ui.shortcuts.ShortcutCategory;
import epox.webaom.ui.shortcuts.ShortcutRegistry;
import epox.webaom.ui.shortcuts.handlers.HelpHandler;
import epox.webaom.ui.shortcuts.handlers.RefreshHandler;
import epox.webaom.ui.shortcuts.handlers.ResetHandler;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalKeyboardShortcutsTest {
    private MainPanel originalGui;

    @BeforeEach
    void saveApplicationContext() {
        originalGui = AppContext.gui;
    }

    @AfterEach
    void restoreApplicationContext() {
        AppContext.gui = originalGui;
    }

    @Test
    void constructorRegistersBindingsAndHelpMetadata() {
        JRootPane rootPane = new JRootPane();
        ShortcutRegistry registry = new ShortcutRegistry();

        new GlobalKeyboardShortcuts(rootPane, registry);

        Object helpKey = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke('?'));
        String refreshStroke = System.getProperty("os.name").toLowerCase().contains("mac") ? "meta R" : "F5";
        Object refreshKey =
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(refreshStroke));
        Object resetKey =
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("F9"));

        assertEquals("help", helpKey);
        assertEquals("refresh", refreshKey);
        assertEquals("reset", resetKey);
        assertNotNull(rootPane.getActionMap().get("help"));
        assertNotNull(rootPane.getActionMap().get("refresh"));
        assertNotNull(rootPane.getActionMap().get("reset"));
        assertEquals(3, registry.getShortcuts(ShortcutCategory.GLOBAL).size());
        assertTrue(registry.generateHelpText().contains("Show keyboard shortcuts help"));
    }

    @Test
    void refreshAndResetActionsDelegateToTheApplication() {
        MainPanel mainPanel = mock(MainPanel.class);
        AlternateViewPanel alternateViewPanel = mock(AlternateViewPanel.class);
        mainPanel.altViewPanel = alternateViewPanel;
        AppContext.gui = mainPanel;
        JRootPane rootPane = new JRootPane();
        new GlobalKeyboardShortcuts(rootPane, new ShortcutRegistry());

        rootPane.getActionMap()
                .get("refresh")
                .actionPerformed(new ActionEvent(rootPane, ActionEvent.ACTION_PERFORMED, "refresh"));
        rootPane.getActionMap()
                .get("reset")
                .actionPerformed(new ActionEvent(rootPane, ActionEvent.ACTION_PERFORMED, "reset"));

        verify(alternateViewPanel).updateAlternativeView(true);
        verify(mainPanel).reset();
    }

    @Test
    void shortcutActionIgnoresEventsWithoutAComponentSource() {
        JRootPane rootPane = new JRootPane();
        new GlobalKeyboardShortcuts(rootPane, new ShortcutRegistry());
        Action helpAction = rootPane.getActionMap().get("help");

        helpAction.actionPerformed(new ActionEvent(new Object(), ActionEvent.ACTION_PERFORMED, "help"));
    }

    @Test
    void builtInHandlersDescribeTheirPublicShortcuts() {
        HelpHandler help = new HelpHandler();
        RefreshHandler refresh = new RefreshHandler();
        ResetHandler reset = new ResetHandler();

        assertEquals('?', help.keyChar());
        assertEquals(0, help.keyCode());
        assertEquals(ShortcutCategory.GLOBAL, help.category());
        assertEquals("Show keyboard shortcuts help", help.description());
        assertSame(help, help.handler());

        assertEquals('R', refresh.keyChar());
        assertEquals(KeyEvent.VK_F5, refresh.keyCode());
        assertEquals(ShortcutCategory.GLOBAL, refresh.category());
        assertEquals("Refresh alternate view", refresh.description());
        assertSame(refresh, refresh.handler());

        assertEquals('\0', reset.keyChar());
        assertEquals(KeyEvent.VK_F9, reset.keyCode());
        assertEquals(ShortcutCategory.GLOBAL, reset.category());
        assertEquals("Reset application", reset.description());
        assertSame(reset, reset.handler());
    }
}
