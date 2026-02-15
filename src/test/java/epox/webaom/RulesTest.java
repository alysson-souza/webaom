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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RulesTest {

    @Test
    void rulesSmartTruncate_preservesExtension_andMaxBytes() {
        String filename = "a".repeat(260) + ".mkv";

        String truncated = Rules.smartTruncateFilename(filename, Rules.MAX_FILENAME_BYTES);

        assertNotEquals(filename, truncated);
        assertTrue(truncated.endsWith(".mkv"));
        assertTrue(truncated.contains("..."));
        assertTrue(Rules.getFilenameByteLength(truncated) <= Rules.MAX_FILENAME_BYTES);
    }

    @Test
    void rulesSmartTruncate_doesNotSplitUtf8() {
        String filename = "あ".repeat(120) + ".mkv";

        String truncated = Rules.smartTruncateFilename(filename, Rules.MAX_FILENAME_BYTES);

        byte[] bytes = truncated.getBytes(StandardCharsets.UTF_8);
        String roundTrip = new String(bytes, StandardCharsets.UTF_8);

        assertEquals(truncated, roundTrip);
        assertFalse(truncated.contains("\uFFFD"));
        assertTrue(Rules.getFilenameByteLength(truncated) <= Rules.MAX_FILENAME_BYTES);
    }

    @Test
    void rulesIsFilenameTooLong_usesUtf8BytesNotChars() {
        String filename = "あ".repeat(90) + ".mkv";

        assertTrue(Rules.getFilenameByteLength(filename) > Rules.MAX_FILENAME_BYTES);
        assertTrue(Rules.isFilenameTooLong(filename));
    }
}
