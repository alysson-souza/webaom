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

package epox.swing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.EnumSet;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ThemeComboBoxTest {

    @Test
    void model_containsEachThemeInBothSections() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThemeComboBox comboBox = new ThemeComboBox(null, () -> false);
            EnumSet<FlatLafTheme> expected = EnumSet.copyOf(Arrays.asList(FlatLafTheme.availableThemes()));
            EnumSet<FlatLafTheme> lightSectionThemes = EnumSet.noneOf(FlatLafTheme.class);
            EnumSet<FlatLafTheme> darkSectionThemes = EnumSet.noneOf(FlatLafTheme.class);

            for (int i = 0; i < comboBox.getModel().getSize(); i++) {
                Object item = comboBox.getModel().getElementAt(i);
                if (item instanceof ThemeComboBox.ThemeEntry entry) {
                    if (entry.darkSection()) {
                        darkSectionThemes.add(entry.theme());
                    } else {
                        lightSectionThemes.add(entry.theme());
                    }
                }
            }

            assertEquals(expected, lightSectionThemes);
            assertEquals(expected, darkSectionThemes);
        });
    }

    @Test
    void syncSelection_usesActiveAppearanceSection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThemeComboBox comboBox = new ThemeComboBox(null, () -> true);
            comboBox.setLightTheme(FlatLafTheme.DARCULA);
            comboBox.setDarkTheme(FlatLafTheme.INTELLIJ);

            Object selectedItem = comboBox.getSelectedItem();
            assertInstanceOf(ThemeComboBox.ThemeEntry.class, selectedItem);
            ThemeComboBox.ThemeEntry entry = (ThemeComboBox.ThemeEntry) selectedItem;
            assertEquals(FlatLafTheme.INTELLIJ, entry.theme());
            assertTrue(entry.darkSection());
        });
    }

    @Test
    void renderer_unselectedChosenEntry_showsManualCheckmark() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThemeComboBox comboBox = new ThemeComboBox(null, () -> true);
            comboBox.setLightTheme(FlatLafTheme.CATPPUCCIN_MOCHA);

            EntryLocation entry = findEntry(comboBox, FlatLafTheme.CATPPUCCIN_MOCHA, false);
            JLabel label = renderEntry(comboBox, entry, false);

            assertEquals("\u2713 " + FlatLafTheme.CATPPUCCIN_MOCHA, label.getText());
        });
    }

    @Test
    void renderer_comboSelectedEntry_omitsManualCheckmarkEvenWhenNotHovered() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThemeComboBox comboBox = new ThemeComboBox(null, () -> true);
            comboBox.setDarkTheme(FlatLafTheme.INTELLIJ);

            EntryLocation entry = findEntry(comboBox, FlatLafTheme.INTELLIJ, true);
            JLabel label = renderEntry(comboBox, entry, false);

            assertFalse(label.getText().startsWith("\u2713 "));
            assertEquals("   " + FlatLafTheme.INTELLIJ, label.getText());
        });
    }

    @Test
    void renderer_hoveringDifferentChosenEntry_keepsManualCheckmark() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThemeComboBox comboBox = new ThemeComboBox(null, () -> true);
            comboBox.setLightTheme(FlatLafTheme.CATPPUCCIN_MOCHA);
            comboBox.setDarkTheme(FlatLafTheme.INTELLIJ);

            EntryLocation entry = findEntry(comboBox, FlatLafTheme.CATPPUCCIN_MOCHA, false);
            JLabel label = renderEntry(comboBox, entry, true);

            assertEquals("\u2713 " + FlatLafTheme.CATPPUCCIN_MOCHA, label.getText());
        });
    }

    private static JLabel renderEntry(ThemeComboBox comboBox, EntryLocation entry, boolean selected) {
        return (JLabel) comboBox.getRenderer()
                .getListCellRendererComponent(new JList<>(), entry.entry(), entry.index(), selected, false);
    }

    private static EntryLocation findEntry(ThemeComboBox comboBox, FlatLafTheme theme, boolean darkSection) {
        for (int i = 0; i < comboBox.getModel().getSize(); i++) {
            Object item = comboBox.getModel().getElementAt(i);
            if (item instanceof ThemeComboBox.ThemeEntry entry
                    && entry.theme() == theme
                    && entry.darkSection() == darkSection) {
                return new EntryLocation(i, entry);
            }
        }
        fail("Theme entry not found: " + theme + " darkSection=" + darkSection);
        return null;
    }

    private record EntryLocation(int index, ThemeComboBox.ThemeEntry entry) {}
}
