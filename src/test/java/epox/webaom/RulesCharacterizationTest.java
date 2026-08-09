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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.util.ReplacementRule;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RulesCharacterizationTest {
    @TempDir
    Path tempDir;

    private Rules rules;
    private Job job;
    private JobList originalJobs;
    private JobCounter originalJobCounter;
    private epox.webaom.ui.MainPanel originalGui;
    private int originalAssumedEpisodeCount;
    private int originalAssumedSpecialCount;

    @BeforeEach
    void setUp() throws IOException {
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        originalGui = AppContext.gui;
        originalAssumedEpisodeCount = AppContext.assumedEpisodeCount;
        originalAssumedSpecialCount = AppContext.assumedSpecialCount;
        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.gui = null;
        AppContext.assumedEpisodeCount = 0;
        AppContext.assumedSpecialCount = 0;

        Path sourceDirectory = Files.createDirectories(tempDir.resolve("Anime Show"));
        File source = Files.writeString(sourceDirectory.resolve("episode 01.mkv"), "video")
                .toFile();
        job = new Job(source, Job.FINISHED);
        job.mylistId = 3;
        job.anidbFile = createAniDbFile(job);

        rules = new Rules();
        rules.setMoveRules("# no move rule");
    }

    @AfterEach
    void tearDown() {
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.gui = originalGui;
        AppContext.assumedEpisodeCount = originalAssumedEpisodeCount;
        AppContext.assumedSpecialCount = originalAssumedSpecialCount;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matchingConditions")
    void apply_acceptsEveryDocumentedConditionType(String description, String condition) {
        rules.setRenameRules("IF " + condition + " DO SET 'matched'\nELSE DO SET 'missed'");

        File result = rules.apply(job);

        assertEquals("matched.mkv", result.getName(), description);
    }

    private static Stream<Arguments> matchingConditions() {
        return Stream.of(
                Arguments.of("anime id", "A(42)"),
                Arguments.of("anime romaji regex", "A(Cowboy Bebop)"),
                Arguments.of("episode number", "E(1)"),
                Arguments.of("episode title", "E(Asteroid Blues)"),
                Arguments.of("video codec", "C(H264)"),
                Arguments.of("audio codec fragment", "C(AAC)"),
                Arguments.of("quality", "Q(HD)"),
                Arguments.of("rip source", "R(BluRay)"),
                Arguments.of("anime type", "T(TV)"),
                Arguments.of("group id", "G(7)"),
                Arguments.of("group name", "G(Example Fansubs)"),
                Arguments.of("year range", "Y(1999-2001)"),
                Arguments.of("dub language", "D(jap)"),
                Arguments.of("subtitle language", "S(eng)"),
                Arguments.of("episode count", "X(26)"),
                Arguments.of("path regex", "P(.*episode 01[.]mkv)"),
                Arguments.of("category fragment", "N(action)"),
                Arguments.of("defined tag", "I(eng)"),
                Arguments.of("unequal tags", "U(ann:eng)"),
                Arguments.of("equal tags", "L(ann:kan)"),
                Arguments.of("tag regex", "Z(ann:Cowboy.*)"),
                Arguments.of("negated alternative", "A(!999,42)"),
                Arguments.of("multiple groups", "A(42);Q(HD);S(eng)"));
    }

    @Test
    void apply_handlesUnknownGroupElseIfAndElseBranches() {
        job.anidbFile.setGroupId(0);
        job.anidbFile.setGroup(null);
        rules.setRenameRules("""
            IF G(7) DO SET 'wrong'
            ELSE IF G(unknown) DO SET 'unknown-group'
            ELSE DO SET 'fallback'
            """);

        assertEquals("unknown-group.mkv", rules.apply(job).getName());

        rules.setRenameRules("IF Q(SD) DO SET 'wrong'\nELSE IF T(Movie) DO SET 'wrong-again'\nELSE DO SET 'fallback'");

        assertEquals("fallback.mkv", rules.apply(job).getName());
    }

    @Test
    void apply_characterizesSetAddFinishReturnAndFailOperations() {
        rules.setRenameRules("""
            DO SET 'first'
            DO ADD '-second'
            DO FINISH '-done'
            DO ADD '-ignored'
            """);

        assertEquals("first-second-done.mkv", rules.apply(job).getName());

        rules.setRenameRules("DO SET 'discarded'\nDO RETURN 'returned'\nDO ADD '-ignored'");
        assertEquals("returned.mkv", rules.apply(job).getName());

        rules.setRenameRules("DO SET 'discarded'\nDO FAIL");
        assertEquals(job.currentFile.getAbsolutePath(), rules.apply(job).getAbsolutePath());
    }

    @Test
    void apply_characterizesAssumeTruncationCommentsAndLegacyTags() {
        rules.setRenameRules("""
            ignored legacy line
            DO ASSUME 26
            DO ASSUME SPECIAL 4
            DO TRUNCATE<8,abcdefghijk>
            DO ADD '-%year-%type' // trailing comment
            """);

        File result = rules.apply(job);

        // TODO: Review whether direct rule setters should translate legacy tags and preserve truncation case.
        assertEquals("ABCDEFGH-2001r-TVe.mkv", result.getName());
        assertEquals(26, AppContext.assumedEpisodeCount);
        assertEquals(4, AppContext.assumedSpecialCount);
    }

    @Test
    void apply_sanitizesIllegalCharactersAndPreservesDirectorySeparators() {
        rules.setRenameRules("DO SET 'bad`name:?*'");
        rules.setMoveRules("DO SET './Season: 1'");

        File result = rules.apply(job);

        // TODO: Review why the default illegal-character rules preserve colons in final filenames.
        assertEquals("bad'name:.mkv", result.getName());
        assertEquals(tempDir.resolve("Anime Show/Season: 1").toFile(), result.getParentFile());
    }

    @Test
    void apply_convertsTagsAndUnderscoreSuffixUsingCurrentLegacyRules() {
        rules.setRenameRules("DO SET '%ann - %enr - %epn [%grp]_'");

        File result = rules.apply(job);

        assertEquals("Cowboy_Bebop_-_01_-_Asteroid_Blues_[EX].mkv", result.getName());
    }

    @Test
    void apply_returnsNullForIncompleteJobs() {
        assertNull(rules.apply(null));

        Job unidentified = new Job(job.currentFile, Job.FINISHED);
        assertNull(rules.apply(unidentified));

        unidentified.anidbFile = new AniDBFile(1);
        assertNull(rules.apply(unidentified));
    }

    @Test
    void optionsRoundTrip_preservesScriptsAndReplacementRules() {
        Options options = new Options();
        rules.setRenameRules("DO SET '%year-%type-%qual-%ed2k'");
        rules.setMoveRules("DO SET '%year'");
        rules.getIllegalCharReplacements().add(new ReplacementRule("source", "destination", false));

        rules.saveToOptions(options);
        Rules restored = new Rules();
        restored.loadFromOptions(options);

        assertEquals("DO SET '%yea-%typ-%qua-%ed2'", restored.getRenameRules());
        assertEquals("DO SET '%yea'", restored.getMoveRules());
        assertTrue(restored.getIllegalCharReplacements().stream()
                .anyMatch(rule -> rule.getSource().equals("source") && !rule.isEnabled()));
    }

    @Test
    void conditionSeparator_usesLegacyFallbackForUnbalancedParenthesis() {
        String condition = "IF A((Cowboy DO SET 'matched'".toUpperCase();

        // TODO: Review whether malformed nested conditions should keep splitting at the first DO.
        assertEquals(12, Rules.indexOfConditionSeparator(condition, 3));
    }

    private AniDBFile createAniDbFile(Job owner) {
        Anime anime = new Anime(42);
        anime.year = 2001;
        anime.endYear = 2001;
        anime.episodeCount = 26;
        anime.latestEpisode = 26;
        anime.type = "TV";
        anime.romajiTitle = "Cowboy Bebop";
        anime.kanjiTitle = "Cowboy Bebop";
        anime.englishTitle = "Cowboy Bebop English";
        anime.categories = "action,space";
        anime.init();

        Episode episode = new Episode(101);
        episode.num = "1";
        episode.eng = "Asteroid Blues";
        episode.rom = "Asteroid Blues Romaji";
        episode.kan = "Asteroid Blues Kanji";

        Group group = new Group(7);
        group.name = "Example Fansubs";
        group.shortName = "EX";

        AniDBFile file = new AniDBFile(1001);
        file.setAnimeId(42);
        file.setEpisodeId(101);
        file.setGroupId(7);
        file.setMylistEntryId(77);
        file.setState(AniDBFile.F_CRCOK | AniDBFile.F_ISV2 | AniDBFile.F_UNC);
        file.setLengthInSeconds(1440);
        file.setEd2kHash("ABCDEF");
        file.setMd5Hash("1234ABCD");
        file.setShaHash("5678ABCD");
        file.setCrcHash("90ABCDEF");
        file.setDubLanguage("jap&eng");
        file.setSubLanguage("eng");
        file.setQuality("HD");
        file.setRipSource("BluRay");
        file.setResolution("1920x1080");
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC 2.0");
        file.setDefaultName("Cowboy Bebop 01");
        file.setExtension("mkv");
        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        file.setJob(owner);
        return file;
    }
}
