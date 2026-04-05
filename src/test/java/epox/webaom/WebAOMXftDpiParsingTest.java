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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WebAOMXftDpiParsingTest {

    static Stream<Arguments> dpiToScaleProvider() {
        return Stream.of(
                Arguments.of(153, 153.0 / 96.0, "typical KDE 160% scaling"),
                Arguments.of(96, 1.0, "standard DPI"),
                Arguments.of(192, 2.0, "2x scaling"),
                Arguments.of(144, 1.5, "150% scaling"),
                Arguments.of(72, 72.0 / 96.0, "low DPI"));
    }

    @ParameterizedTest(name = "Xft.dpi={0} → scale={1} ({2})")
    @MethodSource("dpiToScaleProvider")
    void parseXftDpiScale_computesCorrectScale(int dpi, double expectedScale, String description) throws Exception {
        double scale = WebAOM.parseXftDpiScale(readerOf("Xft.dpi:\t" + dpi + "\n"));

        assertEquals(expectedScale, scale, 0.0001);
    }

    @Test
    void parseXftDpiScale_handlesSpacesAroundValue() throws Exception {
        double scale = WebAOM.parseXftDpiScale(readerOf("Xft.dpi:  144  \n"));

        assertEquals(1.5, scale, 0.0001);
    }

    @Test
    void parseXftDpiScale_extractsFromMultiLineXrdbOutput() throws Exception {
        String xrdbOutput = "Xft.antialias:\ttrue\nXft.dpi:\t153\nXft.hinting:\ttrue\n";

        double scale = WebAOM.parseXftDpiScale(readerOf(xrdbOutput));

        assertEquals(153.0 / 96.0, scale, 0.0001);
    }

    static Stream<Arguments> noMatchProvider() {
        return Stream.of(
                Arguments.of("Xft.antialias:\ttrue\nXft.hinting:\ttrue\n", "Xft.dpi absent"),
                Arguments.of("", "empty input"),
                Arguments.of("SomeApp.Xft.dpi:\t192\nXft.dpiSuffix:\t192\n", "similar but wrong keys"));
    }

    @ParameterizedTest(name = "returns -1 when {1}")
    @MethodSource("noMatchProvider")
    void parseXftDpiScale_returnsNegativeOneWhenNoMatch(String xrdbOutput, String description) throws Exception {
        double scale = WebAOM.parseXftDpiScale(readerOf(xrdbOutput));

        assertEquals(-1.0, scale, 0.0001);
    }

    @Test
    void parseXftDpiScale_usesFirstXftDpiLine() throws Exception {
        double scale = WebAOM.parseXftDpiScale(readerOf("Xft.dpi:\t120\nXft.dpi:\t240\n"));

        assertEquals(120.0 / 96.0, scale, 0.0001);
    }

    private static BufferedReader readerOf(String text) {
        return new BufferedReader(new StringReader(text));
    }
}
