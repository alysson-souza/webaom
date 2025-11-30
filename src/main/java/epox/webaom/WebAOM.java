// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

package epox.webaom;

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.FontUIResource;

public class WebAOM {

    public static void main(String[] args) {
        try {
            launch();
        } catch (Exception e) {
            AppContext.dialog("Exception", e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void launch() {
        setMyFont(new Font("Tahoma", Font.PLAIN, 11), new Font("Times", Font.PLAIN, 11));

        JFrame jf = new JFrame("WebAOM " + AppContext.S_VER + " Loading...");
        AppContext.frame = jf;
        AppContext.component = jf;
        jf.setSize(800, 600);
        jf.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        jf.setLocationRelativeTo(null);
        jf.setVisible(true);

        AppContext.init();

        jf.getContentPane().add(AppContext.gui, java.awt.BorderLayout.CENTER);
        jf.setVisible(true);
        jf.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (AppContext.shutdown(true)) {
                    System.exit(0);
                }
            }
        });
        AppContext.gui.startup();
    }

    static void setMyFont(Font f0, Font f1) {
        FontUIResource f = new FontUIResource(f0);
        FontUIResource f2 = new FontUIResource(f1);
        UIManager.put("Button.font", f);
        UIManager.put("CheckBox.font", f);
        UIManager.put("CheckBoxMenuItem.acceleratorFont", f);
        UIManager.put("CheckBoxMenuItem.font", f);
        UIManager.put("ComboBox.font", f);
        UIManager.put("DesktopIcon.font", f);
        UIManager.put("EditorPane.font", f);
        UIManager.put("FormattedTextField.font", f);
        UIManager.put("Label.font", f);
        UIManager.put("List.font", f);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.acceleratorFont", f);
        UIManager.put("MenuItem.font", f);
        UIManager.put("OptionPane.font", f);
        UIManager.put("Panel.font", f);
        UIManager.put("PasswordField.font", f);
        UIManager.put("PopupMenu.font", f);
        UIManager.put("ProgressBar.font", f);
        UIManager.put("RadioButton.font", f);
        UIManager.put("RadioButtonMenuItem.acceleratorFont", f);
        UIManager.put("RadioButtonMenuItem.font", f);
        UIManager.put("ScrollPane.font", f);
        UIManager.put("Slider.font", f);
        UIManager.put("TabbedPane.font", f);
        UIManager.put("Table.font", f2);
        UIManager.put("TableHeader.font", f);
        UIManager.put("TextArea.font", f);
        UIManager.put("TextField.font", f);
        UIManager.put("TextPane.font", f);
        UIManager.put("TitledBorder.font", f);
        UIManager.put("ToggleButton.font", f);
        UIManager.put("ToolTip.font", f);
        UIManager.put("Tree.font", f);
        UIManager.put("Viewport.font", f);
    }
}
