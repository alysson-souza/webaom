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

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * JComboBox for selecting the application's light and dark FlatLaf themes.
 * Displays a grouped dropdown with section headers for light and dark themes.
 * Tracks two independent selections (one for each mode) and applies the theme
 * matching the currently active OS appearance.
 *
 * <p>Each theme appears twice (once per section) wrapped in a {@link ThemeEntry}
 * so that JComboBox can distinguish items by identity rather than by value.
 */
public class ThemeComboBox extends JComboBox<Object> {
    private static final String LIGHT_HEADER = "\u2500\u2500 Light Mode Theme \u2500\u2500";
    private static final String DARK_HEADER = "\u2500\u2500 Dark Mode Theme \u2500\u2500";

    /** Pairs a theme with its section so each combo item is unique. */
    record ThemeEntry(FlatLafTheme theme, boolean darkSection) {
        @Override
        public String toString() {
            return theme.toString();
        }
    }

    private final Component rootComponent;
    private final BooleanSupplier osDarkModeSupplier;
    private FlatLafTheme lightTheme;
    private FlatLafTheme darkTheme;
    private boolean suppressApply;

    public ThemeComboBox(Component rootComponent) {
        this(rootComponent, OsAppearanceMonitor::isOsDarkMode);
    }

    ThemeComboBox(Component rootComponent, BooleanSupplier osDarkModeSupplier) {
        this.rootComponent = rootComponent;
        this.osDarkModeSupplier = osDarkModeSupplier;
        this.lightTheme = FlatLafTheme.getDefaultLightTheme();
        this.darkTheme = FlatLafTheme.getDefaultDarkTheme();

        setModel(buildModel());
        setMaximumRowCount(getModel().getSize());
        setRenderer(new ThemeCellRenderer());

        addActionListener(e -> {
            if (suppressApply) {
                return;
            }
            Object selected = getSelectedItem();
            if (selected instanceof ThemeEntry entry) {
                if (entry.darkSection()) {
                    darkTheme = entry.theme();
                } else {
                    lightTheme = entry.theme();
                }
                applyActiveTheme();
            }
        });

        syncSelection();
        setToolTipText("Select themes for light and dark modes.");
    }

    public FlatLafTheme getLightTheme() {
        return lightTheme;
    }

    public FlatLafTheme getDarkTheme() {
        return darkTheme;
    }

    public void setLightTheme(FlatLafTheme theme) {
        lightTheme = (theme == null) ? FlatLafTheme.getDefaultLightTheme() : theme;
        syncSelection();
    }

    public void setDarkTheme(FlatLafTheme theme) {
        darkTheme = (theme == null) ? FlatLafTheme.getDefaultDarkTheme() : theme;
        syncSelection();
    }

    /** Applies the theme matching the current OS appearance mode. */
    public void applyActiveTheme() {
        boolean osDark = osDarkModeSupplier.getAsBoolean();
        FlatLafTheme target = osDark ? darkTheme : lightTheme;
        applyTheme(target);
    }

    /** Called by OsAppearanceMonitor when the OS appearance changes. */
    public void onOsAppearanceChanged(boolean osDark) {
        FlatLafTheme target = osDark ? darkTheme : lightTheme;
        applyTheme(target);
        syncSelection();
    }

    private void applyTheme(FlatLafTheme theme) {
        try {
            FlatLafSupport.applyTheme(theme);
            UiTuning.applyForCurrentLookAndFeel();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (rootComponent != null) {
            SwingUtilities.updateComponentTreeUI(rootComponent);
            rootComponent.invalidate();
            rootComponent.validate();
            rootComponent.repaint();
        }
    }

    /** Syncs the combo box selection to the active section's theme without firing apply. */
    private void syncSelection() {
        suppressApply = true;
        try {
            boolean osDark = osDarkModeSupplier.getAsBoolean();
            FlatLafTheme active = osDark ? darkTheme : lightTheme;
            // Find the matching ThemeEntry in the correct section
            for (int i = 0; i < getModel().getSize(); i++) {
                Object item = getModel().getElementAt(i);
                if (item instanceof ThemeEntry entry && entry.theme() == active && entry.darkSection() == osDark) {
                    super.setSelectedIndex(i);
                    return;
                }
            }
        } finally {
            suppressApply = false;
        }
    }

    private DefaultComboBoxModel<Object> buildModel() {
        FlatLafTheme[] all = FlatLafTheme.availableThemes();
        List<Object> items = new ArrayList<>();
        items.add(LIGHT_HEADER);
        for (FlatLafTheme theme : all) {
            items.add(new ThemeEntry(theme, false));
        }
        items.add(DARK_HEADER);
        for (FlatLafTheme theme : all) {
            items.add(new ThemeEntry(theme, true));
        }
        return new DefaultComboBoxModel<>(items.toArray());
    }

    private class ThemeCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            // Collapsed display: show "LightName / DarkName"
            if (index == -1) {
                JLabel label =
                        (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(lightTheme + " / " + darkTheme);
                return label;
            }

            if (value instanceof String header) {
                JLabel label = new JLabel(header);
                label.setFont(list.getFont().deriveFont(Font.BOLD));
                label.setOpaque(true);
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
                label.setBorder(new EmptyBorder(4, 6, 4, 6));
                label.setEnabled(false);
                return label;
            }

            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setBorder(new EmptyBorder(2, 16, 2, 6));

            if (value instanceof ThemeEntry entry) {
                FlatLafTheme chosen = entry.darkSection() ? darkTheme : lightTheme;
                boolean isChosen = entry.theme() == chosen;
                boolean usesNativePopupCheckmark = index == ThemeComboBox.this.getSelectedIndex();
                label.setText((isChosen && !usesNativePopupCheckmark ? "\u2713 " : "   ") + entry.theme());
            }

            return label;
        }
    }

    @Override
    public void setSelectedIndex(int index) {
        Object item = getModel().getElementAt(index);
        if (item instanceof String) {
            // Skip section headers — move to next valid item
            if (index + 1 < getModel().getSize()) {
                super.setSelectedIndex(index + 1);
            }
            return;
        }
        super.setSelectedIndex(index);
    }
}
