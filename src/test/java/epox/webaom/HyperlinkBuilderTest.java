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
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.swing.ThemeColorSupport;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class HyperlinkBuilderTest {
    @Test
    void buildsTheLegacyHtmlFragmentsWithoutEscapingInput() {
        assertEquals(
                "<a href=\"https://example.test/a?x=1&y=2\">A&B</a>",
                HyperlinkBuilder.createHyperlink("https://example.test/a?x=1&y=2", "A&B"));
        assertEquals("<span class=\"custom\"><name></span>", HyperlinkBuilder.wrapInCssClass("custom", "<name>"));
        assertEquals("<span class=\"log-warning\">failure</span>", HyperlinkBuilder.formatAsError("failure"));
        assertEquals("<span class=\"log-name\">episode.mkv</span>", HyperlinkBuilder.formatAsName("episode.mkv"));
        assertEquals("<span class=\"log-name\">42</span>", HyperlinkBuilder.formatAsName((Object) 42));
        assertEquals("null", HyperlinkBuilder.formatAsName((Object) null));
        assertEquals("<span class=\"log-number\">7</span>", HyperlinkBuilder.formatAsNumber(7));
        assertEquals("<span class=\"log-number\">7.5</span>", HyperlinkBuilder.formatAsNumber("7.5"));
        // TODO: Review whether user and server supplied hyperlink text should be HTML escaped.
    }

    @Test
    void semanticColorsMeetTheRequiredContrastAgainstTheConfiguredBackground() {
        Map<String, Object> previousValues = replaceUiColors(Color.WHITE, Color.BLACK);
        try {
            assertTrue(ThemeColorSupport.contrastRatio(HyperlinkBuilder.resolveWarningColor(), Color.WHITE) >= 4.5);
            assertTrue(ThemeColorSupport.contrastRatio(HyperlinkBuilder.resolveNameColor(), Color.WHITE) >= 4.5);
            assertTrue(ThemeColorSupport.contrastRatio(HyperlinkBuilder.resolveNumberColor(), Color.WHITE) >= 4.5);
        } finally {
            previousValues.forEach((key, value) -> {
                if (value == null) {
                    UIManager.getDefaults().remove(key);
                } else {
                    UIManager.put(key, value);
                }
            });
        }
    }

    private static Map<String, Object> replaceUiColors(Color background, Color foreground) {
        Map<String, Object> previous = new LinkedHashMap<>();
        for (String key : new String[] {
            "EditorPane.background",
            "TextPane.background",
            "TextArea.background",
            "Panel.background",
            "EditorPane.foreground",
            "TextPane.foreground",
            "TextArea.foreground",
            "Label.foreground"
        }) {
            previous.put(key, UIManager.get(key));
            UIManager.put(key, key.endsWith("background") ? background : foreground);
        }
        return previous;
    }
}
