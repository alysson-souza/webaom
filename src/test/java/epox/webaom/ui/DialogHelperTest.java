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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;

class DialogHelperTest {
    @Test
    void bindEscapeToClose_registersEscapeShortcutAndInvokesCloseAction() {
        JRootPane rootPane = new JRootPane();
        AtomicBoolean closed = new AtomicBoolean(false);

        DialogHelper.bindEscapeToClose(rootPane, () -> closed.set(true));

        Object actionKey =
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("ESCAPE"));
        assertEquals(DialogHelper.CLOSE_ON_ESCAPE_ACTION_KEY, actionKey);

        Action action = rootPane.getActionMap().get(DialogHelper.CLOSE_ON_ESCAPE_ACTION_KEY);
        assertNotNull(action);
        action.actionPerformed(null);

        assertTrue(closed.get());
    }

    @Test
    void resolveOptionResult_closedDialogReturnsClosedOption() {
        assertEquals(JOptionPane.CLOSED_OPTION, DialogHelper.resolveOptionResult(null, new Object[] {"Yes", "No"}));
        assertEquals(
                JOptionPane.CLOSED_OPTION,
                DialogHelper.resolveOptionResult(JOptionPane.UNINITIALIZED_VALUE, new Object[] {"Yes", "No"}));
    }

    @Test
    void resolveOptionResult_customOptionsReturnsMatchingIndex() {
        Object[] options = {"Truncate", "Skip", "Skip All"};

        assertEquals(1, DialogHelper.resolveOptionResult("Skip", options));
        assertEquals(JOptionPane.CLOSED_OPTION, DialogHelper.resolveOptionResult("Missing", options));
    }

    @Test
    void resolveOptionResult_standardOptionsReturnsSelectedInteger() {
        assertEquals(JOptionPane.YES_OPTION, DialogHelper.resolveOptionResult(JOptionPane.YES_OPTION, null));
        assertEquals(JOptionPane.NO_OPTION, DialogHelper.resolveOptionResult(JOptionPane.NO_OPTION, null));
        assertEquals(JOptionPane.CLOSED_OPTION, DialogHelper.resolveOptionResult("unexpected", null));
    }
}
