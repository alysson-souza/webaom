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

package epox.webaom.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.Options;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StartupValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void startupValidatorValidateDatabase_warnsOnNonJdbcLikeUrl() {
        List<StartupIssue> issues = StartupValidator.validateDatabase(true, "localhost/webaom");

        assertEquals(1, issues.size());
        StartupIssue issue = issues.getFirst();
        assertEquals(StartupIssue.Severity.WARN, issue.severity());
        assertTrue(issue.message().contains("localhost/webaom"));
        assertTrue(issue.suggestion().contains("jdbc:"));
    }

    @Test
    void validateDatabase_acceptsDisabledBlankAndRecognizedUrls() {
        assertTrue(StartupValidator.validateDatabase(false, "not-jdbc").isEmpty());
        assertTrue(StartupValidator.validateDatabase(true, null).isEmpty());
        assertTrue(StartupValidator.validateDatabase(true, "  ").isEmpty());
        assertTrue(StartupValidator.validateDatabase(true, "jdbc:custom:value").isEmpty());
        assertTrue(
                StartupValidator.validateDatabase(true, "postgresql://host/db").isEmpty());
        assertTrue(StartupValidator.validateDatabase(true, "MYSQL://host/db").isEmpty());
        assertTrue(StartupValidator.validateDatabase(true, "sqlite:file.db").isEmpty());
    }

    @Test
    void validateLogging_createsParentAndRejectsParentThatIsAFile() throws Exception {
        Path log = tempDir.resolve("logs/webaom.log");

        assertNull(StartupValidator.validateLogging(false, log.toString()));
        assertEquals(log.toString(), StartupValidator.validateLogging(true, log.toString()));
        assertTrue(Files.isDirectory(log.getParent()));

        Path blockingFile = Files.writeString(tempDir.resolve("blocking"), "not a directory");
        assertNull(StartupValidator.validateLogging(
                true, blockingFile.resolve("webaom.log").toString()));
    }

    @Test
    void validateStartup_handlesNullAndValidOptions() {
        assertTrue(StartupValidator.validateStartup(null).isEmpty());

        Options options = new Options();
        options.setBoolean(Options.BOOL_AUTO_LOG, false);
        options.setBoolean(Options.BOOL_AUTO_LOAD_DATABASE, false);

        assertTrue(StartupValidator.validateStartup(options).isEmpty());
    }

    @Test
    void issueFormattingAndSeverityQueriesCoverNullEmptyAndMixedLists() {
        StartupIssue info = new StartupIssue(StartupIssue.Severity.INFO, "Info title", "Info message");
        StartupIssue warning =
                new StartupIssue(StartupIssue.Severity.WARN, "Warning title", "Warning message", "Fix it");
        Exception cause = new Exception("cause");
        StartupIssue error =
                new StartupIssue(StartupIssue.Severity.ERROR, "Error title", "Error message", "Recover", cause);

        assertEquals("", StartupValidator.formatIssuesForDisplay(null));
        assertEquals("", StartupValidator.formatIssuesForDisplay(List.of()));
        String formatted = StartupValidator.formatIssuesForDisplay(List.of(info, warning, error));
        assertTrue(formatted.contains("[Info] Info title\nInfo message"));
        assertTrue(formatted.contains("[Warning] Warning title"));
        assertTrue(formatted.contains("Suggestion: Fix it"));
        assertTrue(formatted.contains("\n\n---\n\n"));
        assertEquals(warning.getFormattedMessage(), warning.toString());
        assertEquals(cause, error.cause());
        assertEquals("Error", StartupIssue.Severity.ERROR.getDisplayName());

        assertFalse(StartupValidator.hasErrors(null));
        assertFalse(StartupValidator.hasErrors(List.of(info, warning)));
        assertTrue(StartupValidator.hasErrors(List.of(info, error)));
        assertFalse(StartupValidator.hasWarningsOrInfo(null));
        assertFalse(StartupValidator.hasWarningsOrInfo(List.of(error)));
        assertTrue(StartupValidator.hasWarningsOrInfo(List.of(error, warning)));
    }
}
