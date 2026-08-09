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

package epox.av;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileInfoTest {
    private static final String TRACKS_XML = """
        <file>
          <vid default="1">
            <fmt>H264</fmt>
            <res>1920x1080</res>
          </vid>
          <vid>
            <fmt>AV1</fmt>
          </vid>
          <aud default="0">
            <fmt>AAC</fmt>
            <lan>jpn</lan>
          </aud>
          <sub default="1">
            <fmt>ASS</fmt>
          </sub>
        </file>
        """;

    @Test
    void parsesTrackAttributesAndNumbersTracksInDocumentOrder() throws Exception {
        FileInfo info = new FileInfo(TRACKS_XML);

        assertEquals(2, info.vid.size());
        assertEquals(1, info.aud.size());
        assertEquals(1, info.sub.size());
        assertEquals("default", info.vid.get(0).get("def"));
        assertEquals("", info.vid.get(1).get("def"));
        assertEquals("1", info.vid.get(0).get("num"));
        assertEquals("2", info.vid.get(1).get("num"));
        assertEquals("3", info.aud.get(0).get("num"));
        assertEquals("4", info.sub.get(0).get("num"));
        assertEquals("H264", info.vid.get(0).get("fmt"));
        assertEquals("jpn", info.aud.get(0).get("lan"));
        assertFalse(info.m_xml.contains("\n"));
        assertFalse(info.m_xml.contains("\t"));
    }

    @Test
    void convertsEachSelectedTrackWithLegacyThreeCharacterPlaceholders() throws Exception {
        FileInfo info = new FileInfo(TRACKS_XML);

        assertEquals("1:default:H264\n2::AV1", info.convert("%num:%def:%fmt", 0));
        assertEquals("3::AAC:jpn", info.convert("%num:%def:%fmt:%lan", 1));
        assertEquals("4:default:ASS", info.convert("%num:%def:%fmt", 2));
        assertNull(info.convert(null, 0));
        assertNull(info.convert("%num", -1));
        assertNull(info.convert("%num", 3));
    }

    @Test
    void dumpPrintsEveryParsedAttribute() throws Exception {
        FileInfo info = new FileInfo("<file><vid><fmt>AV1</fmt></vid></file>");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previousOut = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            info.dump();
        } finally {
            System.setOut(previousOut);
        }

        String dumped = output.toString(StandardCharsets.UTF_8);
        assertTrue(dumped.contains("fmt: AV1"));
        assertTrue(dumped.contains("num: 1"));
        assertTrue(dumped.contains("def: "));
    }

    @Test
    void malformedXmlIsSilentlyRetainedAsAnEmptyUninitializedResult() throws Exception {
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        FileInfo info;
        try {
            System.setErr(new PrintStream(errors, true, StandardCharsets.UTF_8));
            info = new FileInfo("<file><vid>");
        } finally {
            System.setErr(previousErr);
        }

        // TODO: Review whether FileInfo should report malformed XML instead of swallowing the parser exception.
        assertNull(info.m_xml);
        assertTrue(info.vid.isEmpty());
        assertTrue(info.aud.isEmpty());
        assertTrue(info.sub.isEmpty());
        assertFalse(errors.toString(StandardCharsets.UTF_8).isEmpty());
    }
}
