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
import epox.swing.OsAppearanceMonitor;
import epox.swing.UiTuning;
import epox.swing.layout.DisplayEnvironment;
import epox.swing.layout.UsableScreenBounds;
import epox.swing.layout.WindowLayoutPolicy;
import epox.swing.layout.WindowLayoutSupport;
import epox.swing.layout.WindowPlacement;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.FontUIResource;

public class WebAOM {
    private static final Dimension MAIN_WINDOW_MINIMUM_SIZE = new Dimension(800, 648);
    private static final DisplayEnvironment DISPLAY_ENVIRONMENT = DisplayEnvironment.current();
    private static final WindowLayoutPolicy WINDOW_LAYOUT_POLICY = new WindowLayoutPolicy();

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
        boolean isMacOS = osName.contains("mac");

        if (isMacOS) {
            configureMacOsAppearance();
        }

        if (isLinux) {
            configureLinuxScaling();
        }

        if (System.getProperty("awt.useSystemAAFontSettings") == null) {
            System.setProperty("awt.useSystemAAFontSettings", isLinux ? "lcd" : "on");
        }
        if (System.getProperty("swing.aatext") == null) {
            System.setProperty("swing.aatext", "true");
        }
        if (isLinux && System.getProperty("sun.java2d.xrender") == null) {
            System.setProperty("sun.java2d.xrender", "true");
        }

        if (isLinux) {
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }
    }

    // Window title bars follow the macOS system appearance (light/dark).
    // Must be called on the main thread before AWT/Swing initialization.
    private static void configureMacOsAppearance() {
        if (System.getProperty("apple.awt.application.appearance") == null) {
            System.setProperty("apple.awt.application.appearance", "system");
        }
    }

    // On Linux Wayland, Java Swing runs under XWayland which reports 96 DPI
    // regardless of display scaling. GDK_SCALE is not set on KDE (5.27+ dropped
    // it) and sun.java2d.uiScale is ignored on XWayland. Read Xft.dpi from X
    // resources and set flatlaf.uiScale so FlatLaf can scale the UI.
    // See: https://wiki.archlinux.org/title/HiDPI#AWT/Swing
    private static void configureLinuxScaling() {
        if (System.getProperty("flatlaf.uiScale") != null || System.getProperty("sun.java2d.uiScale") != null) {
            return;
        }
        String gdkScale = System.getenv("GDK_SCALE");
        if (gdkScale != null && !gdkScale.isEmpty()) {
            return;
        }
        try {
            Process process = new ProcessBuilder("xrdb", "-query").start();
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    double scale = parseXftDpiScale(reader);
                    if (scale > 1.0) {
                        System.setProperty("flatlaf.uiScale", scale + "x");
                    }
                }
            } finally {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            // xrdb not available or parsing failed — proceed without scaling
        }
    }

    /** Parses xrdb output for Xft.dpi and returns the scale factor (dpi/96), or -1 if not found. */
    static double parseXftDpiScale(BufferedReader reader) throws java.io.IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Xft.dpi:")) {
                double dpi =
                        Double.parseDouble(line.substring(line.indexOf(':') + 1).trim());
                return dpi / 96.0;
            }
        }
        return -1;
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

        AppContext.init();

        Dimension scaledMinimumSize = DISPLAY_ENVIRONMENT.scaleDimension(MAIN_WINDOW_MINIMUM_SIZE, frame);
        frame.getContentPane().add(AppContext.gui, java.awt.BorderLayout.CENTER);
        WindowLayoutSupport.placeCenteredAt(
                frame, scaledMinimumSize, DISPLAY_ENVIRONMENT, WINDOW_LAYOUT_POLICY, scaledMinimumSize);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                OsAppearanceMonitor.stop();
                if (AppContext.shutdown(true)) {
                    System.exit(0);
                }
            }
        });
        startOsAppearanceMonitor();
        AppContext.gui.startup();
    }

    private static void startOsAppearanceMonitor() {
        OsAppearanceMonitor.start(dark -> {
            if (AppContext.gui != null && AppContext.gui.miscOptionsPanel != null) {
                AppContext.gui.miscOptionsPanel.getThemeComboBox().onOsAppearanceChanged(dark);
            }
        });
    }

    private static FlatLafTheme loadStartupTheme() {
        Options startupOptions = new Options();
        if (!startupOptions.loadFromFile()) {
            return OsAppearanceMonitor.isOsDarkMode()
                    ? FlatLafTheme.getDefaultDarkTheme()
                    : FlatLafTheme.getDefaultLightTheme();
        }

        boolean osDark = OsAppearanceMonitor.isOsDarkMode();
        String themeValue = osDark
                ? startupOptions.getString(Options.STR_THEME_DARK)
                : startupOptions.getString(Options.STR_THEME_LIGHT);
        if (themeValue == null || themeValue.isBlank()) {
            return osDark ? FlatLafTheme.getDefaultDarkTheme() : FlatLafTheme.getDefaultLightTheme();
        }
        return FlatLafTheme.fromOptionValue(themeValue);
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

    static void refreshMainWindowLayout() {
        if (AppContext.frame == null || AppContext.gui == null) {
            return;
        }
        Rectangle currentBounds = AppContext.frame.getBounds();
        AppContext.frame.invalidate();
        AppContext.frame.validate();

        Dimension scaledMinimumSize = DISPLAY_ENVIRONMENT.scaleDimension(MAIN_WINDOW_MINIMUM_SIZE, AppContext.frame);
        UsableScreenBounds screenBounds = DISPLAY_ENVIRONMENT.getUsableScreenBounds(AppContext.frame);
        WindowPlacement placement = WINDOW_LAYOUT_POLICY.layoutWindow(
                AppContext.frame.getPreferredSize(), scaledMinimumSize, screenBounds.usableBounds());
        Rectangle adjustedBounds = WINDOW_LAYOUT_POLICY.expandCurrentBounds(
                currentBounds, placement.bounds().getSize(), screenBounds.usableBounds());

        AppContext.frame.setMinimumSize(placement.minimumSize());
        AppContext.frame.setBounds(adjustedBounds);
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
