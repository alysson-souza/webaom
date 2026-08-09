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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.util.PlatformPaths;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class WebAOMRenderingTest {
    private static final String[] RENDERING_PROPERTIES = {
        "os.name",
        "awt.useSystemAAFontSettings",
        "swing.aatext",
        "sun.java2d.xrender",
        "flatlaf.uiScale",
        "sun.java2d.uiScale",
        "apple.awt.application.appearance"
    };

    private static final String[] FONT_KEYS = {
        "Button.font",
        "CheckBox.font",
        "CheckBoxMenuItem.acceleratorFont",
        "CheckBoxMenuItem.font",
        "ComboBox.font",
        "DesktopIcon.font",
        "EditorPane.font",
        "FormattedTextField.font",
        "Label.font",
        "List.font",
        "Menu.font",
        "MenuItem.acceleratorFont",
        "MenuItem.font",
        "OptionPane.font",
        "Panel.font",
        "PasswordField.font",
        "PopupMenu.font",
        "ProgressBar.font",
        "RadioButton.font",
        "RadioButtonMenuItem.acceleratorFont",
        "RadioButtonMenuItem.font",
        "ScrollPane.font",
        "Slider.font",
        "TabbedPane.font",
        "Table.font",
        "TableHeader.font",
        "TextArea.font",
        "TextField.font",
        "TextPane.font",
        "TitledBorder.font",
        "ToggleButton.font",
        "ToolTip.font",
        "Tree.font",
        "Viewport.font"
    };

    private Map<String, String> previousProperties;
    private Map<String, Object> previousFonts;
    private boolean previousFrameDecoration;
    private boolean previousDialogDecoration;

    @BeforeAll
    static void initializePlatformPathsBeforeChangingSystemProperties() {
        PlatformPaths.getOSName();
    }

    @BeforeEach
    void setUp() {
        previousProperties = new LinkedHashMap<>();
        for (String property : RENDERING_PROPERTIES) {
            previousProperties.put(property, System.getProperty(property));
        }
        previousFonts = new LinkedHashMap<>();
        for (String key : FONT_KEYS) {
            previousFonts.put(key, UIManager.get(key));
        }
        previousFrameDecoration = JFrame.isDefaultLookAndFeelDecorated();
        previousDialogDecoration = JDialog.isDefaultLookAndFeelDecorated();
    }

    @AfterEach
    void tearDown() {
        previousProperties.forEach(WebAOMRenderingTest::restoreProperty);
        previousFonts.forEach(WebAOMRenderingTest::restoreUiDefault);
        JFrame.setDefaultLookAndFeelDecorated(previousFrameDecoration);
        JDialog.setDefaultLookAndFeelDecorated(previousDialogDecoration);
    }

    @Test
    void linuxRenderingSetsDefaultsWithoutRunningScaleDiscoveryWhenScaleIsConfigured() {
        System.setProperty("os.name", "Linux");
        System.setProperty("flatlaf.uiScale", "1.25x");
        System.clearProperty("awt.useSystemAAFontSettings");
        System.clearProperty("swing.aatext");
        System.clearProperty("sun.java2d.xrender");

        WebAOM.configureRendering();

        assertEquals("lcd", System.getProperty("awt.useSystemAAFontSettings"));
        assertEquals("true", System.getProperty("swing.aatext"));
        assertEquals("true", System.getProperty("sun.java2d.xrender"));
        assertEquals("1.25x", System.getProperty("flatlaf.uiScale"));
        assertTrue(JFrame.isDefaultLookAndFeelDecorated());
        assertTrue(JDialog.isDefaultLookAndFeelDecorated());
    }

    @Test
    void configuredRenderingPropertiesAreNotOverwritten() {
        System.setProperty("os.name", "Linux");
        System.setProperty("flatlaf.uiScale", "2x");
        System.setProperty("awt.useSystemAAFontSettings", "off");
        System.setProperty("swing.aatext", "false");
        System.setProperty("sun.java2d.xrender", "false");

        WebAOM.configureRendering();

        assertEquals("off", System.getProperty("awt.useSystemAAFontSettings"));
        assertEquals("false", System.getProperty("swing.aatext"));
        assertEquals("false", System.getProperty("sun.java2d.xrender"));
    }

    @Test
    void standardJavaScaleConfigurationSkipsLinuxXftDiscovery() {
        System.setProperty("os.name", "Linux");
        System.clearProperty("flatlaf.uiScale");
        System.setProperty("sun.java2d.uiScale", "2");

        WebAOM.configureRendering();

        assertNull(System.getProperty("flatlaf.uiScale"));
        assertEquals("2", System.getProperty("sun.java2d.uiScale"));
    }

    @Test
    void macRenderingFollowsSystemAppearanceAndUsesNonLinuxAntialiasingDefault() {
        System.setProperty("os.name", "Mac OS X");
        System.clearProperty("apple.awt.application.appearance");
        System.clearProperty("awt.useSystemAAFontSettings");
        System.clearProperty("swing.aatext");
        System.clearProperty("sun.java2d.xrender");

        WebAOM.configureRendering();

        assertEquals("system", System.getProperty("apple.awt.application.appearance"));
        assertEquals("on", System.getProperty("awt.useSystemAAFontSettings"));
        assertEquals("true", System.getProperty("swing.aatext"));
        assertNull(System.getProperty("sun.java2d.xrender"));
        assertEquals(previousFrameDecoration, JFrame.isDefaultLookAndFeelDecorated());
        assertEquals(previousDialogDecoration, JDialog.isDefaultLookAndFeelDecorated());
    }

    @Test
    void macRenderingPreservesAnExplicitAppearance() {
        System.setProperty("os.name", "Mac OS X");
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameAqua");

        WebAOM.configureRendering();

        assertEquals("NSAppearanceNameAqua", System.getProperty("apple.awt.application.appearance"));
    }

    @Test
    void nonLinuxRenderingUsesGenericTextDefaultsWithoutChangingWindowDecoration() {
        System.setProperty("os.name", "Windows 11");
        System.clearProperty("awt.useSystemAAFontSettings");
        System.clearProperty("swing.aatext");
        System.clearProperty("sun.java2d.xrender");

        WebAOM.configureRendering();

        assertEquals("on", System.getProperty("awt.useSystemAAFontSettings"));
        assertEquals("true", System.getProperty("swing.aatext"));
        assertNull(System.getProperty("sun.java2d.xrender"));
        assertEquals(previousFrameDecoration, JFrame.isDefaultLookAndFeelDecorated());
        assertEquals(previousDialogDecoration, JDialog.isDefaultLookAndFeelDecorated());
    }

    @Test
    void globalFontUsesThePrimaryFontEverywhereExceptTables() {
        Font primary = new Font(Font.DIALOG, Font.BOLD, 17);
        Font table = new Font(Font.MONOSPACED, Font.PLAIN, 15);

        WebAOM.setGlobalFont(primary, table);

        assertTrue(UIManager.get("Label.font") instanceof FontUIResource);
        assertTrue(UIManager.get("Table.font") instanceof FontUIResource);
        assertEquals(primary, UIManager.getFont("Label.font"));
        assertEquals(primary, UIManager.getFont("Button.font"));
        assertEquals(primary, UIManager.getFont("Tree.font"));
        assertEquals(table, UIManager.getFont("Table.font"));
        for (String key : FONT_KEYS) {
            Object expected = key.equals("Table.font") ? UIManager.get("Table.font") : UIManager.get("Label.font");
            assertSame(expected, UIManager.get(key), key);
        }
    }

    @Test
    void layoutRefreshReturnsWhenNoWindowHasBeenCreated() {
        java.awt.Frame previousFrame = AppContext.frame;
        epox.webaom.ui.MainPanel previousGui = AppContext.gui;
        try {
            AppContext.frame = null;
            AppContext.gui = null;
            WebAOM.refreshMainWindowLayout();
            assertNull(AppContext.frame);
            assertNull(AppContext.gui);
        } finally {
            AppContext.frame = previousFrame;
            AppContext.gui = previousGui;
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static void restoreUiDefault(String key, Object value) {
        if (value == null) {
            UIManager.getDefaults().remove(key);
        } else {
            UIManager.put(key, value);
        }
    }
}
