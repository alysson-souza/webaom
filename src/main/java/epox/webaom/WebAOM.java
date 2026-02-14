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

import epox.swing.FlatLafSupport;
import epox.swing.FlatLafTheme;
import epox.swing.UiTuning;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.FontUIResource;

public class WebAOM {

    public static void main(String[] args) {
        try {
            configureRendering();
            launch();
        } catch (Exception e) {
            AppContext.dialog("Exception", e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void configureRendering() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isLinux = osName.contains("linux");

        if (System.getProperty("awt.useSystemAAFontSettings") == null) {
            System.setProperty("awt.useSystemAAFontSettings", isLinux ? "lcd" : "on");
        }
        if (System.getProperty("swing.aatext") == null) {
            System.setProperty("swing.aatext", "true");
        }
        if (isLinux && System.getProperty("sun.java2d.xrender") == null) {
            System.setProperty("sun.java2d.xrender", "true");
        }
    }

    private static void launch() {
        try {
            FlatLafSupport.applyTheme(loadStartupTheme());
        } catch (Exception ex) {
            System.err.println("! Failed to initialize FlatLaf theme: " + ex.getMessage());
            ex.printStackTrace();
        }

        UiTuning.applyForCurrentLookAndFeel();

        JFrame frame = new JFrame("WebAOM " + AppContext.VERSION + " Loading...");
        setWindowIcon(frame);
        AppContext.frame = frame;
        AppContext.component = frame;
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.setSize(800, 648);
        frame.setMinimumSize(new Dimension(800, 648));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        AppContext.init();

        frame.getContentPane().add(AppContext.gui, java.awt.BorderLayout.CENTER);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                if (AppContext.shutdown(true)) {
                    System.exit(0);
                }
            }
        });
        AppContext.gui.startup();
    }

    private static FlatLafTheme loadStartupTheme() {
        Options startupOptions = new Options();
        if (!startupOptions.loadFromFile()) {
            return FlatLafTheme.LIGHT;
        }

        return FlatLafTheme.fromOptionValue(startupOptions.getString(Options.STR_THEME));
    }

    static void setGlobalFont(Font primaryFont, Font tableFont) {
        FontUIResource primaryFontResource = new FontUIResource(primaryFont);
        FontUIResource tableFontResource = new FontUIResource(tableFont);
        UIManager.put("Button.font", primaryFontResource);
        UIManager.put("CheckBox.font", primaryFontResource);
        UIManager.put("CheckBoxMenuItem.acceleratorFont", primaryFontResource);
        UIManager.put("CheckBoxMenuItem.font", primaryFontResource);
        UIManager.put("ComboBox.font", primaryFontResource);
        UIManager.put("DesktopIcon.font", primaryFontResource);
        UIManager.put("EditorPane.font", primaryFontResource);
        UIManager.put("FormattedTextField.font", primaryFontResource);
        UIManager.put("Label.font", primaryFontResource);
        UIManager.put("List.font", primaryFontResource);
        UIManager.put("Menu.font", primaryFontResource);
        UIManager.put("MenuItem.acceleratorFont", primaryFontResource);
        UIManager.put("MenuItem.font", primaryFontResource);
        UIManager.put("OptionPane.font", primaryFontResource);
        UIManager.put("Panel.font", primaryFontResource);
        UIManager.put("PasswordField.font", primaryFontResource);
        UIManager.put("PopupMenu.font", primaryFontResource);
        UIManager.put("ProgressBar.font", primaryFontResource);
        UIManager.put("RadioButton.font", primaryFontResource);
        UIManager.put("RadioButtonMenuItem.acceleratorFont", primaryFontResource);
        UIManager.put("RadioButtonMenuItem.font", primaryFontResource);
        UIManager.put("ScrollPane.font", primaryFontResource);
        UIManager.put("Slider.font", primaryFontResource);
        UIManager.put("TabbedPane.font", primaryFontResource);
        UIManager.put("Table.font", tableFontResource);
        UIManager.put("TableHeader.font", primaryFontResource);
        UIManager.put("TextArea.font", primaryFontResource);
        UIManager.put("TextField.font", primaryFontResource);
        UIManager.put("TextPane.font", primaryFontResource);
        UIManager.put("TitledBorder.font", primaryFontResource);
        UIManager.put("ToggleButton.font", primaryFontResource);
        UIManager.put("ToolTip.font", primaryFontResource);
        UIManager.put("Tree.font", primaryFontResource);
        UIManager.put("Viewport.font", primaryFontResource);
    }

    private static void setWindowIcon(JFrame frame) {
        try (InputStream iconStream = WebAOM.class.getResourceAsStream("/webaom.png")) {
            if (iconStream != null) {
                Image icon = ImageIO.read(iconStream);
                frame.setIconImage(icon);

                if (java.awt.Taskbar.isTaskbarSupported()) {
                    java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                    if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                        taskbar.setIconImage(icon);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore - icon is not critical
        }
    }
}
