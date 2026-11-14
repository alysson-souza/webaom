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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.AppContext;
import epox.webaom.Options;
import epox.webaom.Rules;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the interactive consent prompt for {@code db upgrade}. The command-level tests can only
 * reach the no-console branch, because {@link System#console()} is null under Gradle.
 */
class CliApplicationUpgradePromptTest {
    @TempDir
    Path temporaryDirectory;

    @AfterEach
    void releaseDatabase() {
        if (AppContext.databaseManager != null) {
            AppContext.databaseManager.shutdown();
            AppContext.databaseManager = null;
        }
    }

    private record Run(boolean succeeded, String out, String err, List<String> prompts) {}

    private Run upgradeAnsweringWith(Path dbPath, String answer) throws Exception {
        Options options = new Options(temporaryDirectory.resolve("prompt-config"));
        options.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:" + dbPath);
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CliOutput output = new CliOutput(
                new PrintWriter(out, true), new PrintWriter(err, true), CliOutput.Format.TABLE, false, false);

        CliApplication application = new CliApplication(options, new Rules(), Set.of(), settings -> null);
        List<String> prompts = new java.util.ArrayList<>();
        application.setConsoleReader(prompt -> {
            prompts.add(prompt);
            return answer;
        });

        boolean succeeded = application.upgradeDatabase(output, false);
        return new Run(succeeded, out.toString(), err.toString(), prompts);
    }

    @Test
    void answeringYesAtThePromptMigratesTheDatabase() throws Exception {
        Path dbPath = temporaryDirectory.resolve("prompt-yes.sqlite");
        createPreV4Database(dbPath);

        Run run = upgradeAnsweringWith(dbPath, "y");

        assertTrue(run.succeeded());
        assertEquals(1, run.prompts().size());
        assertTrue(run.prompts().get(0).contains("[y/N]"));
        assertTrue(run.err().contains("Back up the database"));
        assertEquals(8, schemaVersion(dbPath));
    }

    @Test
    void answeringYesInFullAndWithSurroundingSpaceIsAccepted() throws Exception {
        Path dbPath = temporaryDirectory.resolve("prompt-yes-word.sqlite");
        createPreV4Database(dbPath);

        assertTrue(upgradeAnsweringWith(dbPath, "  YES  ").succeeded());
        assertEquals(8, schemaVersion(dbPath));
    }

    @Test
    void answeringNoLeavesTheSchemaUntouched() throws Exception {
        Path dbPath = temporaryDirectory.resolve("prompt-no.sqlite");
        createPreV4Database(dbPath);

        Run run = upgradeAnsweringWith(dbPath, "n");

        assertFalse(run.succeeded());
        assertTrue(run.err().contains("nothing was changed"));
        assertEquals(3, schemaVersion(dbPath));
    }

    @Test
    void pressingEnterDefaultsToNo() throws Exception {
        Path dbPath = temporaryDirectory.resolve("prompt-empty.sqlite");
        createPreV4Database(dbPath);

        assertFalse(upgradeAnsweringWith(dbPath, "").succeeded());
        assertEquals(3, schemaVersion(dbPath));
    }

    @Test
    void anythingOtherThanYesDeclines() throws Exception {
        Path dbPath = temporaryDirectory.resolve("prompt-junk.sqlite");
        createPreV4Database(dbPath);

        assertFalse(upgradeAnsweringWith(dbPath, "sure why not").succeeded());
        assertEquals(3, schemaVersion(dbPath));
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
            statement.executeUpdate("INSERT INTO vtb (ver) VALUES (3)");
        }
    }
}
