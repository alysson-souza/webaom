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

package epox.webaom.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteDatabaseManagerMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void initialize_existingV6Database_migratesToV7AndBackfillsVisibility() throws Exception {
        Path dbPath = tempDir.resolve("migrate-v6.sqlite");
        createLegacyV6Database(dbPath);

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.shutdown();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("select ver from vtb")) {
                assertTrue(rs.next());
                assertEquals(7, rs.getInt(1));
            }
            try (ResultSet rs =
                    statement.executeQuery("select jobs_visible, alt_visible from jtb where ed2k='hash-v6'")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                assertEquals(1, rs.getInt(2));
            }
        }
    }

    @Test
    void initialize_newDatabase_createsVisibilityColumnsWithDefaults() throws Exception {
        Path dbPath = tempDir.resolve("fresh.sqlite");

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.shutdown();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("PRAGMA table_info('jtb')")) {
            Map<String, String> defaultValues = new HashMap<>();
            while (rs.next()) {
                defaultValues.put(rs.getString("name"), rs.getString("dflt_value"));
            }
            assertEquals("1", defaultValues.get("jobs_visible"));
            assertEquals("1", defaultValues.get("alt_visible"));
        }
    }

    private void createLegacyV6Database(Path dbPath) throws Exception {
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
                            + "gid INTEGER NOT NULL, state INTEGER NOT NULL, size INTEGER NOT NULL, len INTEGER "
                            + "NOT NULL DEFAULT 0, ed2k TEXT DEFAULT NULL, md5 TEXT DEFAULT NULL, "
                            + "sha1 TEXT DEFAULT NULL, crc32 TEXT DEFAULT NULL, ripsource TEXT DEFAULT NULL, "
                            + "quality TEXT DEFAULT NULL, audio TEXT DEFAULT NULL, video TEXT DEFAULT NULL, "
                            + "resolution TEXT DEFAULT NULL, def_name TEXT NOT NULL, time TEXT NOT NULL DEFAULT "
                            + "(datetime('now')), sublang TEXT NOT NULL, dublang TEXT NOT NULL, ext TEXT DEFAULT '', "
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
                    + "dublang) VALUES (1, 1, 1, 0, 0, 123, 'hash-v6', 'legacy.mkv', '', '')");
            statement.executeUpdate("INSERT INTO jtb (orig, name, did, fid, status, ed2k, size, uid, lid) VALUES "
                    + "('legacy.mkv', 'legacy.mkv', 1, 1, 16777217, 'hash-v6', 123, 1, 0)");
            statement.executeUpdate("INSERT INTO vtb (ver) VALUES (6)");
        }
    }
}
