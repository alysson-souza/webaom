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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliApplicationSafetyTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preflightRejectsExistingDestinationBeforeMutation() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("source.mkv"), "source");
        Path target = Files.writeString(temporaryDirectory.resolve("target.mkv"), "target");
        CliFileResult result = planned(source, target);

        CliException exception =
                assertThrows(CliException.class, () -> CliApplication.validateDestinations(List.of(result)));

        assertTrue(exception.getMessage().contains("Destination already exists"));
        assertTrue(Files.exists(source));
        assertTrue(Files.exists(target));
    }

    @Test
    void preflightRejectsDestinationSymlinkToTheSource() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("source.mkv"), "source");
        Path target = temporaryDirectory.resolve("target.mkv");
        createSymbolicLinkOrSkip(target, source.getFileName());

        CliException exception = assertThrows(
                CliException.class, () -> CliApplication.validateDestinations(List.of(planned(source, target))));

        assertTrue(exception.getMessage().contains("Destination already exists"));
        assertTrue(Files.isSymbolicLink(target));
        assertTrue(Files.exists(source));
    }

    @Test
    void preflightRejectsBrokenDestinationSymlink() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("source.mkv"), "source");
        Path target = temporaryDirectory.resolve("target.mkv");
        createSymbolicLinkOrSkip(target, Path.of("missing.mkv"));

        CliException exception = assertThrows(
                CliException.class, () -> CliApplication.validateDestinations(List.of(planned(source, target))));

        assertTrue(exception.getMessage().contains("Destination already exists"));
        assertTrue(Files.isSymbolicLink(target));
        assertTrue(Files.exists(source));
    }

    @Test
    void preflightRejectsDistinctHardLinkDestination() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("source.mkv"), "source");
        Path target = temporaryDirectory.resolve("target.mkv");
        createHardLinkOrSkip(target, source);

        CliException exception = assertThrows(
                CliException.class, () -> CliApplication.validateDestinations(List.of(planned(source, target))));

        assertTrue(exception.getMessage().contains("Destination already exists"));
        assertTrue(Files.exists(source));
        assertTrue(Files.exists(target));
    }

    @Test
    void preflightRejectsTwoSourcesWithSameTarget() throws Exception {
        Path first = Files.writeString(temporaryDirectory.resolve("first.mkv"), "first");
        Path second = Files.writeString(temporaryDirectory.resolve("second.mkv"), "second");
        Path target = temporaryDirectory.resolve("same.mkv");

        CliException exception = assertThrows(
                CliException.class,
                () -> CliApplication.validateDestinations(List.of(planned(first, target), planned(second, target))));

        assertTrue(exception.getMessage().contains("Multiple files would become"));
        assertTrue(Files.notExists(target));
    }

    @Test
    void preflightRejectsCaseOnlyCollisionsOnCaseInsensitiveTargets() throws Exception {
        Path first = Files.writeString(temporaryDirectory.resolve("first.mkv"), "first");
        Path second = Files.writeString(temporaryDirectory.resolve("second.mkv"), "second");
        Path upperTarget = temporaryDirectory.resolve("Episode.mkv");
        Path lowerTarget = temporaryDirectory.resolve("episode.mkv");

        CliException exception = assertThrows(
                CliException.class,
                () -> CliApplication.validateDestinations(
                        List.of(planned(first, upperTarget), planned(second, lowerTarget)), ignored -> true));

        assertTrue(exception.getMessage().contains("Multiple files would become"));
        assertTrue(Files.notExists(upperTarget));
        assertTrue(Files.notExists(lowerTarget));
    }

    @Test
    void preflightUsesTheActualTargetFilesystemCaseBehavior() throws Exception {
        Path first = Files.writeString(temporaryDirectory.resolve("first.mkv"), "first");
        Path second = Files.writeString(temporaryDirectory.resolve("second.mkv"), "second");
        List<CliFileResult> results = List.of(
                planned(first, temporaryDirectory.resolve("Episode.mkv")),
                planned(second, temporaryDirectory.resolve("episode.mkv")));
        boolean caseInsensitive = Files.exists(temporaryDirectory.resolve("First.mkv"));

        if (caseInsensitive) {
            assertThrows(CliException.class, () -> CliApplication.validateDestinations(results));
        } else {
            assertDoesNotThrow(() -> CliApplication.validateDestinations(results));
        }
    }

    @Test
    void crossVolumeCopyPreservesSupportedAttributes() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("source.mkv"), "content");
        FileTime expectedModifiedTime = FileTime.fromMillis(System.currentTimeMillis() - 86_400_000);
        Files.setLastModifiedTime(source, expectedModifiedTime);
        expectedModifiedTime = Files.getLastModifiedTime(source);
        Path target = temporaryDirectory.resolve("copied.mkv");

        CliApplication.copyPreservingAttributes(source, target);

        assertTrue(Files.exists(source));
        org.junit.jupiter.api.Assertions.assertEquals(expectedModifiedTime, Files.getLastModifiedTime(target));
    }

    @Test
    void publishingACrossVolumeCopyNeverReplacesAnExistingTarget() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("copy.part"), "new");
        Path target = Files.writeString(temporaryDirectory.resolve("target.mkv"), "existing");

        assertThrows(java.io.IOException.class, () -> CliApplication.publishWithoutReplace(source, target));

        assertEquals("new", Files.readString(source));
        assertEquals("existing", Files.readString(target));
    }

    @Test
    void failedDatabasePersistencePreservesTheCompletedActionAndFailsTheResult() {
        CliFileResult result = new CliFileResult(temporaryDirectory.resolve("source.mkv"));
        result.status = CliFileResult.Status.MOVED;
        result.applied = true;

        CliApplication.recordPersistenceResult(result, false);

        assertEquals(CliFileResult.Status.MOVED, result.status);
        assertTrue(result.applied);
        assertTrue(result.persistenceFailed);
        assertTrue(result.failed());
        assertTrue(result.message.contains("not saved locally"));
    }

    @Test
    void persistenceFailurePreventsMutationsButStillAllowsReadOnlyStatus() {
        CliFileResult result = new CliFileResult(temporaryDirectory.resolve("source.mkv"));
        result.status = CliFileResult.Status.IDENTIFIED;
        CliApplication.recordPersistenceResult(result, false);

        assertFalse(CliApplication.canContinueAfterIdentify(result, false));
        assertTrue(CliApplication.canContinueAfterIdentify(result, true));

        result.status = CliFileResult.Status.MOVE_PLANNED;
        assertFalse(CliApplication.canApplyMove(result));
    }

    @Test
    void movedJobRecalculatesItsDirectoryBeforePersistence() {
        Path source = temporaryDirectory.resolve("source.mkv");
        Path target = temporaryDirectory.resolve("library").resolve("target.mkv");
        JobCounter previousCounter = AppContext.jobCounter;
        AppContext.jobCounter = new JobCounter();
        try {
            Job job = new Job(source.toFile(), Job.FINISHED);
            job.targetFile = target.toFile();
            job.directoryId = 42;

            CliApplication.updateJobPathAfterMove(job, target);

            assertEquals(target.toFile(), job.currentFile);
            assertNull(job.targetFile);
            assertEquals(-1, job.directoryId);
            assertEquals(Job.FINISHED, job.getStatus());
        } finally {
            AppContext.jobCounter = previousCounter;
        }
    }

    @Test
    void failedCrossVolumeCleanupReportsThatBothPathsMayRemain() {
        Path source = temporaryDirectory.resolve("source.mkv");
        Path target = temporaryDirectory.resolve("target.mkv");
        java.io.IOException deleteFailure = new java.io.IOException("source is locked");
        java.io.IOException cleanupFailure = new java.io.IOException("target cannot be removed");

        java.io.IOException combined =
                CliApplication.publishedCopyCleanupFailure(source, target, deleteFailure, cleanupFailure);

        assertTrue(combined.getMessage().contains(source.toString()));
        assertTrue(combined.getMessage().contains(target.toString()));
        assertTrue(combined.getMessage().contains("both files may remain"));
        assertEquals(deleteFailure, combined.getCause());
        assertEquals(cleanupFailure, combined.getSuppressed()[0]);
    }

    @Test
    void companionFilesArePlannedPreflightedAndMovedWithThePrimaryFile() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("episode.mkv"), "video");
        Path subtitle = Files.writeString(temporaryDirectory.resolve("episode.en.srt"), "subtitle");
        Path destinationDirectory = temporaryDirectory.resolve("renamed");
        Path target = destinationDirectory.resolve("Anime - 01.mkv");
        CliFileResult result = planned(source, target);
        result.hashes = new FileHasher().hash(source, EnumSet.of(FileHasher.Algorithm.ED2K));

        CliApplication.planCompanionMoves(List.of(result), java.util.Set.of("mkv"));
        CliApplication.validateDestinations(List.of(result));
        CliApplication application = new CliApplication(
                new epox.webaom.Options(temporaryDirectory.resolve("config")),
                new epox.webaom.Rules(),
                java.util.Set.of("mkv"),
                settings -> {
                    throw new AssertionError("Network access is not expected");
                });
        application.moveWithCompanions(result);

        assertTrue(Files.exists(target));
        assertTrue(Files.exists(destinationDirectory.resolve("Anime - 01.en.srt")));
        assertTrue(Files.notExists(source));
        assertTrue(Files.notExists(subtitle));
        assertTrue(result.companionMoves.getFirst().applied);
    }

    @Test
    void preflightRejectsExistingCompanionDestination() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("episode.mkv"), "video");
        Files.writeString(temporaryDirectory.resolve("episode.srt"), "subtitle");
        Path destinationDirectory = Files.createDirectories(temporaryDirectory.resolve("renamed"));
        Path target = destinationDirectory.resolve("Anime - 01.mkv");
        Path companionTarget = Files.writeString(destinationDirectory.resolve("Anime - 01.srt"), "existing");
        CliFileResult result = planned(source, target);

        CliApplication.planCompanionMoves(List.of(result), java.util.Set.of("mkv"));
        CliException exception =
                assertThrows(CliException.class, () -> CliApplication.validateDestinations(List.of(result)));

        assertTrue(exception.getMessage().contains("Destination already exists: " + companionTarget));
        assertTrue(Files.exists(source));
    }

    @Test
    void companionTargetsPreserveTheirSuffixWithinTheFilenameByteLimit() throws Exception {
        String sourceBase = "s".repeat(240);
        String targetBase = "t".repeat(251);
        Path source = Files.writeString(temporaryDirectory.resolve(sourceBase + ".mkv"), "video");
        Files.writeString(temporaryDirectory.resolve(sourceBase + ".en.forced.srt"), "subtitle");
        CliFileResult result = planned(source, temporaryDirectory.resolve(targetBase + ".mkv"));

        CliApplication.planCompanionMoves(List.of(result), java.util.Set.of("mkv"));

        String companionName =
                result.companionMoves.getFirst().target.getFileName().toString();
        assertTrue(companionName.endsWith(".en.forced.srt"));
        assertTrue(companionName.getBytes(StandardCharsets.UTF_8).length <= epox.webaom.Rules.MAX_FILENAME_BYTES);
    }

    @Test
    void primaryMoveFailureRollsCompanionFilesBackToTheirOriginalPaths() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("episode.mkv"), "video");
        Path subtitle = Files.writeString(temporaryDirectory.resolve("episode.srt"), "subtitle");
        Path destinationDirectory = Files.createDirectories(temporaryDirectory.resolve("renamed"));
        Path target = destinationDirectory.resolve("Anime - 01.mkv");
        CliFileResult result = planned(source, target);
        result.hashes = new FileHasher().hash(source, EnumSet.of(FileHasher.Algorithm.ED2K));
        CliApplication.planCompanionMoves(List.of(result), java.util.Set.of("mkv"));
        CliApplication.validateDestinations(List.of(result));
        Files.writeString(target, "created after preflight");
        CliApplication application = new CliApplication(
                new epox.webaom.Options(temporaryDirectory.resolve("config")),
                new epox.webaom.Rules(),
                java.util.Set.of("mkv"),
                settings -> {
                    throw new AssertionError("Network access is not expected");
                });

        assertThrows(java.io.IOException.class, () -> application.moveWithCompanions(result));

        assertTrue(Files.exists(source));
        assertTrue(Files.exists(subtitle));
        assertTrue(Files.notExists(destinationDirectory.resolve("Anime - 01.srt")));
        assertTrue(Files.exists(target));
        org.junit.jupiter.api.Assertions.assertFalse(result.companionMoves.getFirst().applied);
    }

    @Test
    void companionPlanningDoesNotIncludeOtherConfiguredMediaFiles() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("episode.mkv"), "video");
        Path otherMedia = Files.writeString(temporaryDirectory.resolve("episode.part2.mkv"), "other video");
        Path subtitle = Files.writeString(temporaryDirectory.resolve("episode.en.srt"), "subtitle");
        CliFileResult result = planned(source, temporaryDirectory.resolve("Anime - 01.mkv"));

        CliApplication.planCompanionMoves(List.of(result), java.util.Set.of("mkv", "mp4"));

        assertEquals(1, result.companionMoves.size());
        assertEquals(subtitle.toAbsolutePath(), result.companionMoves.getFirst().source);
        assertFalse(result.companionMoves.stream().anyMatch(move -> move.source.equals(otherMedia.toAbsolutePath())));
    }

    private static CliFileResult planned(Path source, Path target) {
        CliFileResult result = new CliFileResult(source.toAbsolutePath());
        result.target = target.toAbsolutePath();
        result.status = CliFileResult.Status.RENAME_PLANNED;
        return result;
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (Exception exception) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + exception.getMessage());
        }
    }

    private static void createHardLinkOrSkip(Path link, Path target) {
        try {
            Files.createLink(link, target);
        } catch (Exception exception) {
            Assumptions.assumeTrue(false, "Hard links are unavailable: " + exception.getMessage());
        }
    }
}
