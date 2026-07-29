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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.AppContext;
import epox.webaom.FileHandler;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.data.MylistStates;
import epox.webaom.net.AniDBConnection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rootHelpPresentsUserGoalCommands() {
        Invocation invocation = invoke("--help");

        assertEquals(0, invocation.exitCode());
        assertTrue(invocation.out().contains("identify"));
        assertTrue(invocation.out().contains("rename"));
        assertTrue(invocation.out().contains("mylist"));
        assertTrue(invocation.out().contains("rules"));
    }

    @Test
    void noArgumentsShowCliUsageWithoutLaunchingSwing() {
        Invocation invocation = invoke();

        assertEquals(0, invocation.exitCode());
        assertTrue(invocation.out().contains("Usage: webaom"));
        assertNull(AppContext.gui);
    }

    @Test
    void missingFileArgumentsReturnUsageExitCode() {
        Invocation invocation = invoke("identify");

        assertEquals(2, invocation.exitCode());
        assertTrue(invocation.err().contains("Missing required parameter"));
    }

    @Test
    void wrappedCommandFailuresPreserveTheirExitCode() {
        Path missing = temporaryDirectory.resolve("missing.mkv");

        Invocation invocation = invoke("hash", missing.toString());

        assertEquals(2, invocation.exitCode());
        assertTrue(invocation.err().contains("Path does not exist"));
    }

    @Test
    void hashRunsHeadlesslyAndSupportsJson() throws Exception {
        Path file = temporaryDirectory.resolve("sample.mkv");
        Files.writeString(file, "abc");
        Path config = temporaryDirectory.resolve("missing-config");

        Invocation invocation = invoke("hash", file.toString(), "--config", config.toString(), "--format", "json");

        assertEquals(0, invocation.exitCode());
        assertTrue(invocation.out().contains("\"status\":\"hashed\""));
        assertTrue(invocation.out().contains("\"ed2k\":\"a448017aaf21d8525fc10ae87aa6729d\""));
        assertTrue(Files.notExists(config));
        assertNull(AppContext.gui);
    }

    @Test
    void existingMalformedConfigFailsClosedWithoutBeingOverwritten() throws Exception {
        Path file = Files.writeString(temporaryDirectory.resolve("sample.mkv"), "abc");
        Path config = Files.writeString(temporaryDirectory.resolve("broken-config"), "not a WebAOM config");

        Invocation invocation = invoke("hash", file.toString(), "--config", config.toString(), "--format", "json");

        assertEquals(2, invocation.exitCode());
        assertTrue(invocation.out().isEmpty());
        assertTrue(invocation.err().contains("Could not load WebAOM configuration"));
        assertEquals("not a WebAOM config", Files.readString(config));
    }

    @Test
    void nestedHelpShowsMylistOverrides() {
        Invocation invocation = invoke("mylist", "add", "--help");

        assertEquals(0, invocation.exitCode());
        assertTrue(invocation.out().contains("--[no-]watched"));
        assertTrue(invocation.out().contains("--state"));
        assertTrue(invocation.out().contains("--storage"));
    }

    @Test
    void formatIsOnlyAcceptedByCommandsThatProduceFileResults() {
        Invocation fileHelp = invoke("identify", "--help");
        Invocation configHelp = invoke("config", "show", "--help");
        Invocation unsupported = invoke("config", "show", "--format", "json");

        assertTrue(fileHelp.out().contains("--format"));
        assertFalse(configHelp.out().contains("--format"));
        assertEquals(2, unsupported.exitCode());
        assertTrue(unsupported.err().contains("Unknown options"));
    }

    @Test
    void rulesCheckReportsLineNumberAndPreservesUsageBoundary() throws Exception {
        Path rules = temporaryDirectory.resolve("invalid-rules.conf");
        Files.writeString(rules, "[rename]\nIF A(Test) ADD 'bad'\n");

        Invocation invocation = invoke("rules", "check", rules.toString());

        assertEquals(1, invocation.exitCode());
        assertTrue(invocation.err().contains("line 2"));
    }

    @Test
    void configCommandsUseConfiguredDatabaseAndRedactCredentials() {
        Path config = temporaryDirectory.resolve("config");
        Options options = new Options(config);
        options.setString(
                Options.STR_DATABASE_URL,
                "jdbc:postgresql://db.example/webaom?user=alice&password=correct-horse"
                        + "&sslpassword=tls-secret&clientCertificateKeyStorePassword=key-secret&ssl=true");
        assertTrue(options.saveToFileUnchecked());

        Invocation show = invoke("config", "show", "--config", config.toString());
        Invocation path = invoke("config", "path", "--config", config.toString());

        assertEquals(0, show.exitCode());
        assertEquals(0, path.exitCode());
        assertTrue(show.out().contains("user=<redacted>&password=<redacted>"));
        assertTrue(show.out().contains("sslpassword=<redacted>"));
        assertTrue(show.out().contains("clientCertificateKeyStorePassword=<redacted>"));
        assertTrue(path.out().contains("db.example/webaom"));
        assertFalse(show.out().contains("alice"));
        assertFalse(show.out().contains("correct-horse"));
        assertFalse(show.out().contains("tls-secret"));
        assertFalse(show.out().contains("key-secret"));
        assertFalse(path.out().contains("alice"));
        assertFalse(path.out().contains("correct-horse"));
        assertFalse(path.out().contains("tls-secret"));
        assertFalse(path.out().contains("key-secret"));
    }

    @Test
    void firstSavedCliConfigRetainsFreshApplicationDefaults() throws Exception {
        Path config = temporaryDirectory.resolve("first-cli-config");
        try (CliApplication application = CliApplication.open(config, false)) {
            assertTrue(application.options().saveToFileUnchecked());
        }

        Options loaded = new Options(config);
        assertTrue(loaded.loadFromFileQuietly());
        FileHandler fileHandler = new FileHandler();
        fileHandler.loadOptions(loaded);
        Rules rules = new Rules();
        rules.loadFromOptions(loaded);

        assertEquals(AniDBConnection.DEFAULT_HOST, loaded.getString(Options.STR_HOST_URL));
        assertEquals(AniDBConnection.DEFAULT_REMOTE_PORT, loaded.getInteger(Options.INT_REMOTE_PORT));
        assertEquals(AniDBConnection.DEFAULT_LOCAL_PORT, loaded.getInteger(Options.INT_LOCAL_PORT));
        assertEquals(20, loaded.getInteger(Options.INT_TIMEOUT));
        assertEquals(4, loaded.getInteger(Options.INT_DATAGRAM_DELAY));
        assertEquals(MylistStates.LOCATION_HDD, loaded.getInteger(Options.INT_FILE_STATE));
        assertTrue(loaded.getBoolean(Options.BOOL_ADD_FILE));
        assertTrue(fileHandler.allowedExtensions.getSize() > 0);
        assertFalse(rules.getRenameRules().isBlank());
    }

    @Test
    void renameFailuresAlwaysShowTheirDetailInTableOutput() {
        Path source = temporaryDirectory.resolve("source.mkv").toAbsolutePath();
        CliFileResult result = new CliFileResult(source);
        result.target = temporaryDirectory.resolve("target.mkv").toAbsolutePath();
        result.status = CliFileResult.Status.FAILED;
        result.message = "Permission denied";
        StringWriter out = new StringWriter();

        new CliOutput(
                        new PrintWriter(out, true),
                        new PrintWriter(new StringWriter(), true),
                        CliOutput.Format.TABLE,
                        false,
                        false)
                .printResults(List.of(result), true);

        assertTrue(out.toString().contains("Status: failed"));
        assertTrue(out.toString().contains("Detail: Permission denied"));
    }

    @Test
    void renameOutputIncludesCompanionMovesInTableAndJson() {
        Path source = temporaryDirectory.resolve("episode.mkv").toAbsolutePath();
        Path target = temporaryDirectory.resolve("Anime - 01.mkv").toAbsolutePath();
        CliFileResult result = new CliFileResult(source);
        result.target = target;
        result.status = CliFileResult.Status.RENAME_PLANNED;
        result.companionMoves.add(new CliFileResult.CompanionMove(
                temporaryDirectory.resolve("episode.en.srt").toAbsolutePath(),
                temporaryDirectory.resolve("Anime - 01.en.srt").toAbsolutePath()));
        StringWriter table = new StringWriter();
        StringWriter json = new StringWriter();

        new CliOutput(
                        new PrintWriter(table, true),
                        new PrintWriter(new StringWriter(), true),
                        CliOutput.Format.TABLE,
                        false,
                        false)
                .printResults(List.of(result), true);
        new CliOutput(
                        new PrintWriter(json, true),
                        new PrintWriter(new StringWriter(), true),
                        CliOutput.Format.JSON,
                        false,
                        false)
                .printResults(List.of(result), true);

        assertTrue(table.toString().contains("Companion:"));
        assertTrue(table.toString().contains("episode.en.srt"));
        assertTrue(json.toString().contains("\"companions\":["));
        assertTrue(json.toString().contains("Anime - 01.en.srt"));
    }

    @Test
    void structuredOutputDistinguishesACompletedActionFromFailedDatabasePersistence() {
        Path source = temporaryDirectory.resolve("episode.mkv").toAbsolutePath();
        Path target = temporaryDirectory.resolve("Anime - 01.mkv").toAbsolutePath();
        CliFileResult result = new CliFileResult(source);
        result.target = target;
        result.status = CliFileResult.Status.RENAMED;
        result.applied = true;
        CliApplication.recordPersistenceResult(result, false);
        StringWriter json = new StringWriter();

        new CliOutput(
                        new PrintWriter(json, true),
                        new PrintWriter(new StringWriter(), true),
                        CliOutput.Format.JSON,
                        false,
                        false)
                .printResults(List.of(result), true);

        assertTrue(result.failed());
        assertTrue(json.toString().contains("\"status\":\"renamed\""));
        assertTrue(json.toString().contains("\"applied\":true"));
        assertTrue(json.toString().contains("\"databasePersisted\":false"));
    }

    @Test
    void identifyPreservesMissingCredentialExitCodeForExplicitUnknownFile() throws Exception {
        Path file = Files.writeString(temporaryDirectory.resolve("sample.unknown"), "abc");
        Path config = temporaryDirectory.resolve("identify-config");
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + temporaryDirectory.resolve("identify.sqlite"));
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("identify", file.toString(), "--config", config.toString());

        assertEquals(2, invocation.exitCode());
        assertTrue(invocation.err().contains("AniDB credentials are missing"));
    }

    @Test
    void unsupportedDatabaseUrlFailsWithoutDisclosingIt() {
        Path config = temporaryDirectory.resolve("unsupported-database-config");
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:oracle:thin:alice/secret@db.example:1521/webaom");
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("identify", "--config", config.toString(), temporaryDirectory.toString());

        assertEquals(2, invocation.exitCode());
        assertTrue(invocation.err().contains("Unsupported database type"));
        assertFalse(invocation.out().contains("alice"));
        assertFalse(invocation.out().contains("secret"));
        assertFalse(invocation.err().contains("alice"));
        assertFalse(invocation.err().contains("secret"));
    }

    @Test
    void doctorReportsDatabaseFailureInsteadOfAborting() {
        Path config = temporaryDirectory.resolve("doctor-unsupported-database-config");
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:oracle:thin:alice/secret@db.example:1521/webaom");
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("doctor", "--config", config.toString());

        // The database check must be reported alongside the rest of the checklist rather than
        // aborting it; diagnosing that failure is what doctor exists for.
        assertEquals(1, invocation.exitCode());
        assertTrue(invocation.out().contains("Configuration:"));
        assertTrue(invocation.out().contains("Database: Unsupported database type"));
        assertTrue(invocation.out().contains("Credentials:"));
        assertFalse(invocation.out().contains("alice"));
        assertFalse(invocation.out().contains("secret"));
        assertFalse(invocation.err().contains("alice"));
        assertFalse(invocation.err().contains("secret"));
    }

    @Test
    void identifyRefusesUndeclinedLegacyDatabaseAndPointsToDbUpgrade() throws Exception {
        Path config = temporaryDirectory.resolve("legacy-database-config");
        Path dbPath = temporaryDirectory.resolve("legacy.sqlite");
        createPreV4Database(dbPath);
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + dbPath);
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("identify", "--config", config.toString(), temporaryDirectory.toString());

        assertEquals(2, invocation.exitCode());
        assertTrue(invocation.err().contains("must be upgraded"));
        assertTrue(invocation.err().contains("webaom db upgrade"));
    }

    @Test
    void dbUpgradeWithoutYesAndWithoutConsoleRefusesAndLeavesSchemaUntouched() throws Exception {
        Path config = temporaryDirectory.resolve("db-upgrade-no-console-config");
        Path dbPath = temporaryDirectory.resolve("no-console.sqlite");
        createPreV4Database(dbPath);
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + dbPath);
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("db", "upgrade", "--config", config.toString());

        assertEquals(1, invocation.exitCode());
        assertTrue(invocation.out().contains("must be upgraded"));
        assertTrue(invocation.out().contains("--yes"));
        assertEquals(3, schemaVersion(dbPath));
    }

    @Test
    void dbUpgradeWithYesMigratesPreV4DatabaseAndUnblocksOtherCommands() throws Exception {
        Path config = temporaryDirectory.resolve("db-upgrade-yes-config");
        Path dbPath = temporaryDirectory.resolve("upgrade-me.sqlite");
        createPreV4Database(dbPath);
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + dbPath);
        assertTrue(options.saveToFileUnchecked());

        Invocation upgrade = invoke("db", "upgrade", "--yes", "--config", config.toString());

        assertEquals(0, upgrade.exitCode());
        assertTrue(upgrade.out().contains("--yes"));
        assertTrue(upgrade.out().contains("up to date"));
        assertEquals(7, schemaVersion(dbPath));

        Invocation doctor = invoke("doctor", "--config", config.toString());
        assertTrue(doctor.out().contains("Database: ok"));
    }

    @Test
    void dbUpgradeOnFreshDatabaseSucceedsWithoutRequestingConsent() throws Exception {
        Path config = temporaryDirectory.resolve("db-upgrade-fresh-config");
        Path dbPath = temporaryDirectory.resolve("fresh.sqlite");
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + dbPath);
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("db", "upgrade", "--config", config.toString());

        assertEquals(0, invocation.exitCode());
        assertFalse(invocation.out().contains("must be upgraded"));
        assertTrue(invocation.out().contains("up to date"));
        assertEquals(7, schemaVersion(dbPath));
    }

    @Test
    void dbUpgradeRefusesAncientSchemaEvenWithYes() throws Exception {
        Path config = temporaryDirectory.resolve("db-upgrade-ancient-config");
        Path dbPath = temporaryDirectory.resolve("ancient.sqlite");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE mylist (lid INTEGER NOT NULL, state INTEGER NOT NULL)");
            statement.executeUpdate("INSERT INTO mylist (lid, state) VALUES (0, 1)");
        }
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + dbPath);
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("db", "upgrade", "--yes", "--config", config.toString());

        assertEquals(1, invocation.exitCode());
        assertTrue(invocation.out().contains("predates WebAOM's migrations and cannot be upgraded"));
    }

    @Test
    void dbUpgradeRefusesFutureSchemaEvenWithYes() throws Exception {
        Path config = temporaryDirectory.resolve("db-upgrade-future-config");
        Path dbPath = temporaryDirectory.resolve("future.sqlite");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE vtb (ver INTEGER NOT NULL)");
            statement.executeUpdate("INSERT INTO vtb (ver) VALUES (99)");
        }
        Options options = new Options(config);
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + dbPath);
        assertTrue(options.saveToFileUnchecked());

        Invocation invocation = invoke("db", "upgrade", "--yes", "--config", config.toString());

        assertEquals(1, invocation.exitCode());
        assertTrue(invocation.out().contains("newer WebAOM version"));
    }

    private static int schemaVersion(Path dbPath) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select ver from vtb")) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    private static void createPreV4Database(Path dbPath) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE vtb (ver INTEGER NOT NULL)");
            statement.executeUpdate(
                    "CREATE TABLE atb (aid INTEGER NOT NULL, romaji TEXT NOT NULL, kanji TEXT DEFAULT NULL, "
                            + "english TEXT DEFAULT NULL, year INTEGER NOT NULL, episodes INTEGER NOT NULL, "
                            + "last_ep INTEGER NOT NULL, type TEXT NOT NULL, genre TEXT NOT NULL, img INTEGER "
                            + "DEFAULT 0, PRIMARY KEY (aid))");
            statement.executeUpdate(
                    "CREATE TABLE etb (eid INTEGER NOT NULL, english TEXT NOT NULL, kanji TEXT DEFAULT NULL, "
                            + "romaji TEXT DEFAULT NULL, number TEXT NOT NULL, PRIMARY KEY (eid))");
            statement.executeUpdate("CREATE TABLE gtb (gid INTEGER NOT NULL, name TEXT NOT NULL, short TEXT NOT NULL, "
                    + "PRIMARY KEY (gid))");
            statement.executeUpdate(
                    "CREATE TABLE ftb (fid INTEGER NOT NULL, aid INTEGER NOT NULL, eid INTEGER NOT NULL, "
                            + "gid INTEGER NOT NULL, state INTEGER NOT NULL, size INTEGER NOT NULL, "
                            + "ed2k TEXT DEFAULT NULL, md5 TEXT DEFAULT NULL, "
                            + "sha1 TEXT DEFAULT NULL, crc32 TEXT DEFAULT NULL, ripsource TEXT DEFAULT NULL, "
                            + "quality TEXT DEFAULT NULL, audio TEXT DEFAULT NULL, video TEXT DEFAULT NULL, "
                            + "resolution TEXT DEFAULT NULL, def_name TEXT NOT NULL, time TEXT NOT NULL DEFAULT "
                            + "(datetime('now')), sublang TEXT NOT NULL, dublang TEXT NOT NULL, "
                            + "PRIMARY KEY (fid))");
            statement.executeUpdate("CREATE TABLE utb (uid INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE dtb (did INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE jtb (orig TEXT NOT NULL, name TEXT NOT NULL, did INTEGER NOT NULL, "
                    + "fid INTEGER NOT NULL, status INTEGER NOT NULL, ed2k TEXT NOT NULL, md5 TEXT "
                    + "DEFAULT NULL, sha1 TEXT DEFAULT NULL, tth TEXT DEFAULT NULL, crc32 TEXT DEFAULT NULL, "
                    + "size INTEGER NOT NULL, uid INTEGER NOT NULL, lid INTEGER NOT NULL, time TEXT "
                    + "NOT NULL DEFAULT (datetime('now')), avxml TEXT DEFAULT NULL, PRIMARY KEY (size, ed2k))");
            statement.executeUpdate("INSERT INTO utb (uid, name) VALUES (1, 'default')");
            statement.executeUpdate("INSERT INTO dtb (did, name) VALUES (1, '/tmp')");
            statement.executeUpdate("INSERT INTO ftb (fid, aid, eid, gid, state, size, ed2k, def_name, sublang, "
                    + "dublang) VALUES (1, 1, 1, 0, 0, 123, 'hash-prev4', 'legacy.mkv', '', '')");
            statement.executeUpdate("INSERT INTO jtb (orig, name, did, fid, status, ed2k, size, uid, lid) VALUES "
                    + "('legacy.mkv', 'legacy.mkv', 1, 1, 16777217, 'hash-prev4', 123, 1, 0)");
            statement.executeUpdate("INSERT INTO vtb (ver) VALUES (3)");
        }
    }

    @Test
    void windowsEditorCommandPreservesArgumentsAndQuotedExecutablePaths() throws Exception {
        Path rules = Path.of("C:\\Temp\\rules.conf");

        assertEquals(
                List.of("code", "--wait", rules.toString()),
                Cli.RulesEditCommand.windowsEditorCommand("code --wait", rules));
        assertEquals(
                List.of("C:\\Program Files\\Editor\\editor.exe", "--wait", rules.toString()),
                Cli.RulesEditCommand.windowsEditorCommand("\"C:\\Program Files\\Editor\\editor.exe\" --wait", rules));
    }

    @Test
    void rulesEditPreservesTheTemporaryFileWhenTheEditorFails() throws Exception {
        Path temporaryRules = temporaryDirectory.resolve("edited-rules.conf");
        Rules rules = new Rules();
        rules.setRenameRules("DO ADD 'original'");
        rules.setMoveRules("# none");
        CliApplication application = new CliApplication(
                new Options(temporaryDirectory.resolve("config")), rules, Set.of("mkv"), settings -> {
                    throw new AssertionError("Network access is not expected");
                });
        StringWriter out = new StringWriter();
        CliOutput output = new CliOutput(
                new PrintWriter(out, true),
                new PrintWriter(new StringWriter(), true),
                CliOutput.Format.TABLE,
                false,
                false);

        CliException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CliException.class,
                () -> Cli.RulesEditCommand.editRules(application, output, temporaryRules, file -> {
                    Files.writeString(file, "[rename]\nDO ADD 'edited'\n");
                    throw new CliException("Editor exited with status 1.", 1);
                }));

        assertTrue(exception.getMessage().contains("was kept at " + temporaryRules));
        assertTrue(Files.exists(temporaryRules));
        assertTrue(Files.readString(temporaryRules).contains("edited"));
    }

    private static Invocation invoke(String... arguments) {
        StringWriter outBuffer = new StringWriter();
        StringWriter errBuffer = new StringWriter();
        int exitCode = Cli.execute(new PrintWriter(outBuffer, true), new PrintWriter(errBuffer, true), arguments);
        return new Invocation(exitCode, outBuffer.toString(), errBuffer.toString());
    }

    private record Invocation(int exitCode, String out, String err) {}
}
