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
 */

package epox.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StringUtilitiesTest {
    @TempDir
    Path tempDir;

    @Test
    void basicStringHelpersPreserveLegacyBoundaries() {
        assertTrue(StringUtilities.alfanum("abc-_123"));
        assertTrue(StringUtilities.alfanum("1234567890123456"));
        assertFalse(StringUtilities.alfanum("ab"));
        assertFalse(StringUtilities.alfanum("12345678901234567"));
        assertFalse(StringUtilities.alfanum("spaces fail"));

        assertEquals("inside", StringUtilities.getInTag("before<x>inside</x>after", "x"));
        assertNull(StringUtilities.getInTag("<x>missing close", "x"));
        assertNull(StringUtilities.getInTag("no tag", "x"));
        assertEquals(42, StringUtilities.i("42"));
        assertThrows(NumberFormatException.class, () -> StringUtilities.i("4.2"));

        assertNull(StringUtilities.n(null));
        assertNull(StringUtilities.n(""));
        assertNull(StringUtilities.n("null"));
        assertEquals("NULL", StringUtilities.n("NULL"));
    }

    @Test
    void splitRetainsLeadingMiddleAndTrailingEmptySegments() {
        assertArrayEquals(new String[] {"one"}, StringUtilities.split("one", '|'));
        assertArrayEquals(new String[] {"", "one", "", "two", ""}, StringUtilities.split("|one||two|", '|'));
        assertArrayEquals(new String[] {""}, StringUtilities.split("", '|'));
    }

    @Test
    void htmlHelpersRecordLooseLegacyParsing() {
        assertNull(StringUtilities.dehtmlize(null));
        assertEquals("Hello world", StringUtilities.dehtmlize("<b>Hello</b> <i>world</i>"));
        assertEquals("prefix", StringUtilities.dehtmlize("prefix<unclosed"));

        assertNull(StringUtilities.htmldesc(null));
        assertEquals("A A &amp;", StringUtilities.htmldesc("A &#65; &amp;"));
        assertEquals("&#no;", StringUtilities.htmldesc("&#no;"));
        assertEquals("&#12x;", StringUtilities.htmldesc("&#12x;"));
        assertEquals("plain;", StringUtilities.htmldesc("plain;"));

        // TODO: Review whether an unterminated numeric reference should be discarded instead of preserved.
        assertEquals("prefix", StringUtilities.htmldesc("prefix&#65"));
    }

    @Test
    void byteFormattingSelectsUnitsWithoutLosingTheValue() {
        assertTrue(StringUtilities.sbyte(999).startsWith("999"));
        assertTrue(StringUtilities.sbyte(1024).startsWith("1"));
        assertTrue(StringUtilities.sbyte(1024).endsWith(" KB"));
        assertTrue(StringUtilities.sbyte(1024L * 1024).endsWith(" MB"));
        assertTrue(StringUtilities.sbyte(-1).startsWith("-1"));
        assertFalse(StringUtilities.time().isBlank());
    }

    @Test
    void fileToStringReadsSmallFilesAndRejectsMissingOrOversizedFiles() throws Exception {
        Path text = tempDir.resolve("text.txt");
        Files.writeString(text, "ASCII content", StandardCharsets.US_ASCII);
        assertEquals("ASCII content", StringUtilities.fileToString(text.toString()));

        assertNull(StringUtilities.fileToString(tempDir.resolve("missing.txt").toString()));

        Path oversized = tempDir.resolve("oversized.bin");
        Files.write(oversized, new byte[1024 * 1024 + 1]);
        assertNull(StringUtilities.fileToString(oversized.toString()));

        Path empty = tempDir.resolve("empty.txt");
        Files.createFile(empty);
        // TODO: Review whether reading an empty file should return an empty string rather than throw.
        assertThrows(IllegalArgumentException.class, () -> StringUtilities.fileToString(empty.toString()));
    }

    @Test
    void replaceCcCodeReplacesKnownCodesAndLeavesOtherTextUntouched() {
        Map<String, String> replacements = Map.of("ABC", "replacement", "END", "done");

        assertEquals("replacement/value", StringUtilities.replaceCCCode("%ABC/value", replacements));
        assertEquals("x%MISSy", StringUtilities.replaceCCCode("x%MISSy", replacements));
        assertEquals("short%", StringUtilities.replaceCCCode("short%", replacements));
        assertEquals("done", StringUtilities.replaceCCCode("%END", replacements));
    }

    @Test
    void outputHelpersWriteToTheirDocumentedStreams() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));

            StringUtilities.out("message");
            StringUtilities.err("problem");
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        assertEquals("message" + System.lineSeparator(), stdout.toString(StandardCharsets.UTF_8));
        assertEquals("! problem" + System.lineSeparator(), stderr.toString(StandardCharsets.UTF_8));
    }
}
