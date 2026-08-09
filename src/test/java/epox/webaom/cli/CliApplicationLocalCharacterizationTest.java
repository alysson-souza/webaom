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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.db.DatabaseManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliApplicationLocalCharacterizationTest {
    @TempDir
    Path tempDir;

    @Test
    void discoverReportsAnEmptySupportedFileSetAsACommandFailure() {
        CliApplication application = localApplication();

        CliException failure = assertThrows(CliException.class, () -> application.discover(List.of(tempDir)));

        assertEquals(1, failure.exitCode());
        assertEquals("No supported files found.", failure.getMessage());
    }

    @Test
    void doctorUsesAnExistingLocalDatabaseAndStopsBeforeNetworkWhenCredentialsAreMissing() {
        DatabaseManager previousDatabase = AppContext.databaseManager;
        String previousUsername = AppContext.userPass.username;
        String previousPassword = AppContext.userPass.password;
        String previousApiKey = AppContext.userPass.apiKey;
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.isConnected()).thenReturn(true);
        AppContext.databaseManager = database;
        AppContext.userPass.username = null;
        AppContext.userPass.password = null;
        AppContext.userPass.apiKey = null;
        StringWriter out = new StringWriter();
        CliOutput output = new CliOutput(
                new PrintWriter(out, true),
                new PrintWriter(new StringWriter(), true),
                CliOutput.Format.TABLE,
                false,
                false);
        try {
            assertFalse(localApplication().doctor(output));
        } finally {
            AppContext.databaseManager = previousDatabase;
            AppContext.userPass.username = previousUsername;
            AppContext.userPass.password = previousPassword;
            AppContext.userPass.apiKey = previousApiKey;
        }

        assertTrue(out.toString().contains("Database: ok"));
        assertTrue(out.toString().contains("Credentials: missing"));
        assertFalse(out.toString().contains("AniDB:"));
    }

    @Test
    void persistenceResultRecordingIsIdempotentAndPreservesExistingDetail() {
        CliFileResult successful = new CliFileResult(tempDir.resolve("successful.mkv"));
        successful.status = CliFileResult.Status.IDENTIFIED;
        CliApplication.recordPersistenceResult(successful, true);
        assertFalse(successful.persistenceFailed);
        assertNull(successful.message);

        CliFileResult failed = new CliFileResult(tempDir.resolve("failed.mkv"));
        failed.status = CliFileResult.Status.IDENTIFIED;
        failed.message = "Identified from cache.";
        CliApplication.recordPersistenceResult(failed, false);
        String firstMessage = failed.message;
        CliApplication.recordPersistenceResult(failed, false);

        assertTrue(failed.persistenceFailed);
        assertTrue(failed.failed());
        assertTrue(firstMessage.startsWith("Identified from cache."));
        assertEquals(firstMessage, failed.message);
        assertFalse(CliApplication.canContinueAfterIdentify(failed, false));
        assertTrue(CliApplication.canContinueAfterIdentify(failed, true));
    }

    @Test
    void moveEligibilityRequiresAHealthyPlannedResult() {
        CliFileResult result = new CliFileResult(tempDir.resolve("episode.mkv"));
        result.status = CliFileResult.Status.IDENTIFIED;
        assertFalse(CliApplication.canApplyMove(result));

        result.status = CliFileResult.Status.RENAME_PLANNED;
        assertTrue(CliApplication.canApplyMove(result));
        result.status = CliFileResult.Status.MOVE_PLANNED;
        assertTrue(CliApplication.canApplyMove(result));

        result.status = CliFileResult.Status.UNKNOWN;
        assertFalse(CliApplication.canApplyMove(result));
    }

    @Test
    void defaultConfigPathUsesThePlatformResolver() {
        assertTrue(CliApplication.defaultConfigPath().isAbsolute());
    }

    private CliApplication localApplication() {
        Options options = new Options(tempDir.resolve("config"));
        return new CliApplication(options, new Rules(), Set.of("mkv"), settings -> {
            throw new AssertionError("Network access is not expected");
        });
    }
}
