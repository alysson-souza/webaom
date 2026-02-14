/*
 * Created on 30. juni. 2007 00.05.11
 * Filename: JComboBoxLF.java
 */
package epox.swing;

import java.awt.Component;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

/**
 * JComboBox for selecting the application's FlatLaf theme. Displays supported theme options and applies selection
 * immediately.
 */
public class JComboBoxLF extends JComboBox<String> {
    private static final FlatLafTheme[] THEMES = FlatLafTheme.availableThemes();

    public JComboBoxLF(final Component rootComponent) {
        super(new DefaultComboBoxModel<>() {
            @Override
            public String getElementAt(int index) {
                return THEMES[index].toString();
            }

            @Override
            public int getSize() {
                return THEMES.length;
            }
        });
        addActionListener(event -> {
            try {
                FlatLafSupport.applyTheme(THEMES[getSelectedIndex()]);
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
        });

        FlatLafTheme currentTheme = FlatLafSupport.getCurrentTheme();
        int selectedIndex;
        for (selectedIndex = 0; selectedIndex < THEMES.length; selectedIndex++) {
            if (currentTheme == THEMES[selectedIndex]) {
                break;
            }
        }
        setSelectedIndex(selectedIndex);
        setToolTipText("Select application theme here.");
    }

    public FlatLafTheme getSelectedTheme() {
        int index = getSelectedIndex();
        if (index < 0 || index >= THEMES.length) {
            return FlatLafTheme.getDefaultTheme();
        }

        return THEMES[index];
    }

    public void setSelectedTheme(FlatLafTheme theme) {
        FlatLafTheme selectedTheme = (theme == null) ? FlatLafTheme.getDefaultTheme() : theme;
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i] == selectedTheme) {
                setSelectedIndex(i);
                return;
            }
        }

        setSelectedIndex(0);
    }
}
