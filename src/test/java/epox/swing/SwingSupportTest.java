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

package epox.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import org.junit.jupiter.api.Test;

class SwingSupportTest {
    @Test
    void catppuccinLookAndFeelsExposeTheirNamesDescriptionsAndBrightness() {
        CatppuccinLatteLaf latte = new CatppuccinLatteLaf();
        CatppuccinMochaLaf mocha = new CatppuccinMochaLaf();

        assertEquals(CatppuccinLatteLaf.NAME, latte.getName());
        assertTrue(latte.getDescription().contains("pastel light theme"));
        assertFalse(latte.isDark());
        assertEquals(CatppuccinMochaLaf.NAME, mocha.getName());
        assertTrue(mocha.getDescription().contains("pastel dark theme"));
        assertTrue(mocha.isDark());
    }

    @Test
    void catppuccinLookAndFeelsCanRegisterWithSwing() {
        UIManager.LookAndFeelInfo[] original = UIManager.getInstalledLookAndFeels();
        try {
            CatppuccinLatteLaf.installLafInfo();
            CatppuccinMochaLaf.installLafInfo();

            assertTrue(java.util.Arrays.stream(UIManager.getInstalledLookAndFeels())
                    .anyMatch(info -> info.getName().equals(CatppuccinLatteLaf.NAME)));
            assertTrue(java.util.Arrays.stream(UIManager.getInstalledLookAndFeels())
                    .anyMatch(info -> info.getName().equals(CatppuccinMochaLaf.NAME)));
        } finally {
            UIManager.setInstalledLookAndFeels(original);
        }
    }

    @Test
    void uiTuningInstallsConsistentFieldAndComboInsets() {
        String[] keys = {
            "TextField.margin",
            "FormattedTextField.margin",
            "PasswordField.margin",
            "TextField.contentMargins",
            "FormattedTextField.contentMargins",
            "PasswordField.contentMargins",
            "ComboBox.padding"
        };
        Object[] original = java.util.Arrays.stream(keys).map(UIManager::get).toArray();
        try {
            UiTuning.applyForCurrentLookAndFeel();

            InsetsUIResource fieldMargin = new InsetsUIResource(3, 6, 3, 6);
            assertEquals(fieldMargin, UIManager.get("TextField.margin"));
            assertEquals(fieldMargin, UIManager.get("FormattedTextField.margin"));
            assertEquals(fieldMargin, UIManager.get("PasswordField.margin"));
            assertEquals(fieldMargin, UIManager.get("TextField.contentMargins"));
            assertEquals(fieldMargin, UIManager.get("FormattedTextField.contentMargins"));
            assertEquals(fieldMargin, UIManager.get("PasswordField.contentMargins"));
            assertEquals(new InsetsUIResource(2, 6, 2, 6), UIManager.get("ComboBox.padding"));
        } finally {
            for (int index = 0; index < keys.length; index++) {
                UIManager.put(keys[index], original[index]);
            }
        }
    }

    @Test
    void scrollTableReportsVisibleRowsAndRequestsFocusWhenClicked() {
        JTable table = new JTable(10, 1);
        table.setRowHeight(20);
        TestScrollTable scrollTable = new TestScrollTable(table);
        scrollTable.getViewport().setExtentSize(new Dimension(100, 60));
        scrollTable.getViewport().setViewPosition(new java.awt.Point(0, 40));

        assertEquals(2, scrollTable.getTopVisibleRow());
        assertEquals(5, scrollTable.getBottomVisibleRow());

        scrollTable.mouseClicked(
                new MouseEvent(scrollTable, MouseEvent.MOUSE_CLICKED, 1L, 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        assertTrue(scrollTable.focusRequested);
    }

    @Test
    void scrollTableUsesLastRowWhenViewportExtendsPastTheTable() {
        JTable table = new JTable(2, 1);
        table.setRowHeight(20);
        JScrollTable scrollTable = new JScrollTable(table);
        scrollTable.getViewport().setExtentSize(new Dimension(100, 100));

        assertEquals(1, scrollTable.getBottomVisibleRow());
    }

    private static final class TestScrollTable extends JScrollTable {
        private boolean focusRequested;

        private TestScrollTable(JTable table) {
            super(table);
        }

        @Override
        public boolean requestFocusInWindow() {
            focusRequested = true;
            return true;
        }
    }
}
