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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrackTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void specializedTracksCopyAllGenericMetadata() {
        GenericTrack source = populatedTrack();

        VideoTrack video = new VideoTrack(source);
        AudioTrack audio = new AudioTrack(source);

        assertGenericMetadataEquals(source, video);
        assertGenericMetadataEquals(source, audio);
        assertNotSame(source, video);
        assertNotSame(source, audio);
    }

    @Test
    void genericCopyKeepsCurrentScalarAndReferenceValues() {
        GenericTrack source = populatedTrack();

        GenericTrack copy = new GenericTrack(source);

        assertGenericMetadataEquals(source, copy);
        assertNotSame(source, copy);
    }

    @Test
    void avInfoRejectsMissingInputBeforeOpeningNativeMedia() {
        Path missing = temporaryDirectory.resolve("missing.mkv");

        IOException exception = assertThrows(IOException.class, () -> new AVInfo(missing.toFile()));

        assertEquals(AVInfo.ok() ? "Error: file not found." : "Error: dll not found.", exception.getMessage());
    }

    @Test
    void unavailableNativeLibraryRejectsExistingInput() throws Exception {
        if (AVInfo.ok()) {
            return;
        }

        Path media = java.nio.file.Files.writeString(temporaryDirectory.resolve("episode.mkv"), "not media");
        IOException exception = assertThrows(IOException.class, () -> new AVInfo(media.toFile()));

        assertEquals("Error: dll not found.", exception.getMessage());
        assertFalse(AVInfo.ok());
    }

    private static GenericTrack populatedTrack() {
        GenericTrack track = new GenericTrack();
        track.num = 7;
        track.type_id = 2;
        track.codec_id = 42;
        track.codec = "codec";
        track.codecExtra = "profile";
        track.lang = "jpn";
        track.name = "Main";
        track.size = 1234.5;
        track.dur = 98.25f;
        track.def = true;
        return track;
    }

    private static void assertGenericMetadataEquals(GenericTrack expected, GenericTrack actual) {
        assertEquals(expected.num, actual.num);
        assertEquals(expected.type_id, actual.type_id);
        assertEquals(expected.codec_id, actual.codec_id);
        assertEquals(expected.codec, actual.codec);
        assertEquals(expected.codecExtra, actual.codecExtra);
        assertEquals(expected.lang, actual.lang);
        assertEquals(expected.name, actual.name);
        assertEquals(expected.size, actual.size);
        assertEquals(expected.dur, actual.dur);
        assertEquals(expected.def, actual.def);
    }
}
