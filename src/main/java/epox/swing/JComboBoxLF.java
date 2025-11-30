/*
 * Created on 30. juni. 2007 00.05.11
 * Filename: JComboBoxLF.java
 */
package epox.swing;

import java.awt.Component;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * JComboBox for selecting the application's Look and Feel. Displays installed L&F options and applies selection
 * immediately.
 */
public class JComboBoxLF extends JComboBox<String> {
    /** Array of installed look and feel options */
    protected static final LookAndFeelInfo[] LOOK_AND_FEELS = UIManager.getInstalledLookAndFeels();

    public JComboBoxLF(final Component rootComponent) {
        super(new DefaultComboBoxModel<>() {
            @Override
            public String getElementAt(int index) {
                return LOOK_AND_FEELS[index].getName();
            }

            @Override
            public int getSize() {
                return LOOK_AND_FEELS.length;
            }
        });
        addActionListener(event -> {
            try {
                UIManager.setLookAndFeel(LOOK_AND_FEELS[getSelectedIndex()].getClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            SwingUtilities.updateComponentTreeUI(rootComponent);
        });
        String currentLookAndFeel = UIManager.getLookAndFeel().getClass().getCanonicalName();
        int selectedIndex;
        for (selectedIndex = 0; selectedIndex < LOOK_AND_FEELS.length; selectedIndex++) {
            if (currentLookAndFeel.equals(LOOK_AND_FEELS[selectedIndex].getClassName())) {
                break;
            }
        }
        setSelectedIndex(selectedIndex);
        setToolTipText("Select wanted look and feel here.");
    }
}
