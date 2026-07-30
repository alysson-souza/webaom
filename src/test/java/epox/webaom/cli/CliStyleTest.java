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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.Job;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliStyleTest {
    private static final String ESC = "\u001B";

    @TempDir
    Path temporaryDirectory;

    @Test
    void autoModesRespectInteractivityAndNoColorIndependently() {
        CliStyle nonInteractive = CliStyle.resolve(CliStyle.Mode.AUTO, CliStyle.Mode.AUTO, false, false);
        CliStyle noColor = CliStyle.resolve(CliStyle.Mode.AUTO, CliStyle.Mode.AUTO, true, true);

        assertFalse(nonInteractive.colorsEnabled());
        assertFalse(nonInteractive.hyperlinksEnabled());
        assertFalse(noColor.colorsEnabled());
        assertTrue(noColor.hyperlinksEnabled());
    }

    @Test
    void explicitModesOverrideAutomaticDetection() {
        CliStyle forced = CliStyle.resolve(CliStyle.Mode.ALWAYS, CliStyle.Mode.ALWAYS, false, true);
        CliStyle disabled = CliStyle.resolve(CliStyle.Mode.NEVER, CliStyle.Mode.NEVER, true, false);

        assertTrue(forced.colorsEnabled());
        assertTrue(forced.hyperlinksEnabled());
        assertFalse(disabled.colorsEnabled());
        assertFalse(disabled.hyperlinksEnabled());
    }

    @Test
    void pathLinksUseAbsoluteEncodedUrisAndEscapeControlCharacters() {
        Path path = temporaryDirectory.resolve("episode \u001B[31m.mkv");
        CliStyle style = CliStyle.resolve(CliStyle.Mode.ALWAYS, CliStyle.Mode.ALWAYS, true, false);

        String rendered = style.path(path);

        assertTrue(rendered.startsWith(
                ESC + "]8;;" + path.toAbsolutePath().normalize().toUri().toASCIIString()));
        assertTrue(rendered.contains("episode \\u001B[31m.mkv"));
        assertFalse(rendered.contains("episode " + ESC + "[31m.mkv"));
        assertTrue(rendered.endsWith(ESC + "]8;;" + ESC + "\\"));
    }

    @Test
    void multilineTextNormalizesCrLfAndEscapesLoneCarriageReturns() {
        CliStyle style = CliStyle.plain();

        String rendered = style.text("first\r\nsecond\roverwritten");

        assertEquals("first\nsecond\\u000Doverwritten", rendered);
    }

    @Test
    void tableOutputLinksEveryAniDbEntity() {
        Path source = temporaryDirectory.resolve("episode.mkv");
        CliFileResult result = new CliFileResult(source);
        result.status = CliFileResult.Status.IDENTIFIED;
        AniDBFile file = new AniDBFile(444);
        file.setAnimeId(111);
        file.setEpisodeId(222);
        file.setGroupId(333);
        Anime anime = new Anime(111);
        anime.englishTitle = "Example Anime";
        Episode episode = new Episode(222);
        episode.num = "1";
        episode.eng = "Pilot";
        Group group = new Group(333);
        group.shortName = "Example";
        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        Job job = org.mockito.Mockito.mock(Job.class);
        job.anidbFile = file;
        result.job = job;
        StringWriter out = new StringWriter();

        new CliOutput(
                        new PrintWriter(out, true),
                        new PrintWriter(new StringWriter(), true),
                        CliOutput.Format.TABLE,
                        false,
                        false,
                        CliStyle.resolve(CliStyle.Mode.NEVER, CliStyle.Mode.ALWAYS, true, false))
                .printResults(List.of(result), CliOutput.ResultView.STANDARD);

        assertTrue(out.toString().contains(ESC + "]8;;" + file.getAnimeUrl() + ESC + "\\Example Anime"));
        assertTrue(out.toString().contains(ESC + "]8;;" + file.getEpisodeUrl() + ESC + "\\1 — Pilot"));
        assertTrue(out.toString().contains(ESC + "]8;;" + file.getGroupUrl() + ESC + "\\Example"));
        assertTrue(out.toString().contains(ESC + "]8;;" + file.getFileUrl() + ESC + "\\file 444"));
    }

    @Test
    void structuredOutputNeverContainsTerminalEscapesWhenStylingIsForced() {
        CliFileResult result = new CliFileResult(temporaryDirectory.resolve("episode.mkv"));
        result.status = CliFileResult.Status.HASHED;
        StringWriter json = new StringWriter();

        new CliOutput(
                        new PrintWriter(json, true),
                        new PrintWriter(new StringWriter(), true),
                        CliOutput.Format.JSON,
                        false,
                        false,
                        CliStyle.resolve(CliStyle.Mode.ALWAYS, CliStyle.Mode.ALWAYS, true, false))
                .printResults(List.of(result), CliOutput.ResultView.STANDARD);

        assertTrue(json.toString().contains("\"status\":\"hashed\""));
        assertFalse(json.toString().contains(ESC));
    }
}
