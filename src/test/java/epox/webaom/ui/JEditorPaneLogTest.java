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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.swing.FlatLafSupport;
import epox.swing.FlatLafTheme;
import java.awt.Color;
import java.awt.Font;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class JEditorPaneLogTest {
    private String originalLookAndFeelClassName;
    private Font originalEditorPaneFont;

    @BeforeEach
    void setUp() {
        originalLookAndFeelClassName = UIManager.getLookAndFeel() == null
                ? null
                : UIManager.getLookAndFeel().getClass().getName();
        originalEditorPaneFont = UIManager.getFont("EditorPane.font");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (originalLookAndFeelClassName != null) {
            UIManager.setLookAndFeel(originalLookAndFeelClassName);
        }
        UIManager.put("EditorPane.font", originalEditorPaneFont);
    }

    @Test
    void effectiveHtml_usesThemeColorsAndRefreshesAcrossThemeChanges() throws Exception {
        FlatLafSupport.applyTheme(FlatLafTheme.DARK);
        JEditorPaneLog logPane = new JEditorPaneLog();
        logPane.println("Loaded db in " + epox.webaom.HyperlinkBuilder.formatAsNumber("21") + " ms.");

        String darkHtml = logPane.getRenderedHtml();
        String darkForeground = toHex(logPane.getForeground());
        String darkBackground = toHex(logPane.getBackground());
        String darkNumberColor = toHex(epox.webaom.HyperlinkBuilder.resolveNumberColor());

        assertTrue(darkHtml.contains("color:#" + darkForeground));
        assertTrue(darkHtml.contains("background-color:#" + darkBackground));
        assertTrue(darkHtml.contains("Loaded db in "));
        assertTrue(
                darkHtml.contains("." + epox.webaom.HyperlinkBuilder.CSS_CLASS_NUMBER + "{color:#" + darkNumberColor));

        FlatLafSupport.applyTheme(FlatLafTheme.LIGHT);
        logPane.updateUI();

        String lightHtml = logPane.getRenderedHtml();
        String lightForeground = toHex(logPane.getForeground());
        String lightBackground = toHex(logPane.getBackground());
        String lightNumberColor = toHex(epox.webaom.HyperlinkBuilder.resolveNumberColor());

        assertTrue(lightHtml.contains("color:#" + lightForeground));
        assertTrue(lightHtml.contains("background-color:#" + lightBackground));
        assertTrue(lightHtml.contains("Loaded db in "));
        assertTrue(lightHtml.contains(
                "." + epox.webaom.HyperlinkBuilder.CSS_CLASS_NUMBER + "{color:#" + lightNumberColor));
        assertNotEquals(darkHtml, lightHtml);
        assertNotEquals(darkBackground, lightBackground);
        assertNotEquals(darkNumberColor, lightNumberColor);
    }

    @Test
    void constructor_doesNotRegisterLegacyDeleteHeaderShortcut() {
        JEditorPaneLog logPane = new JEditorPaneLog();

        assertNotEquals("remove", logPane.getInputMap().get(KeyStroke.getKeyStroke("DELETE")));
        assertFalse(logPane.getActionMap().get("remove") instanceof JEditorPaneLog);
    }

    @Test
    void hyperlinkBuilderDefaultColors_areReadableInDarkTheme() throws Exception {
        FlatLafSupport.applyTheme(FlatLafTheme.DARK);
        JEditorPaneLog logPane = new JEditorPaneLog();

        Color background = logPane.getBackground();
        Color numberColor = epox.webaom.HyperlinkBuilder.resolveNumberColor();
        Color nameColor = epox.webaom.HyperlinkBuilder.resolveNameColor();
        Color warningColor = epox.webaom.HyperlinkBuilder.resolveWarningColor();

        assertTrue(contrastRatio(numberColor, background) >= 4.5);
        assertTrue(contrastRatio(nameColor, background) >= 4.5);
        assertTrue(contrastRatio(warningColor, background) >= 4.5);
    }

    @Test
    void updateUi_usesCurrentEditorPaneFontSizeForMonospaceRendering() {
        UIManager.put("EditorPane.font", new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 23));

        JEditorPaneLog logPane = new JEditorPaneLog();
        logPane.updateUI();

        assertEquals(23, logPane.getFont().getSize());
    }

    private String toHex(Color color) {
        return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private double contrastRatio(Color foreground, Color background) {
        double lighter = Math.max(relativeLuminance(foreground), relativeLuminance(background));
        double darker = Math.min(relativeLuminance(foreground), relativeLuminance(background));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double relativeLuminance(Color color) {
        double red = linearize(color.getRed() / 255.0);
        double green = linearize(color.getGreen() / 255.0);
        double blue = linearize(color.getBlue() / 255.0);
        return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
    }

    private double linearize(double channel) {
        if (channel <= 0.03928) {
            return channel / 12.92;
        }
        return Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
