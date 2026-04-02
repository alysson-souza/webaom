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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class DatabaseManagerMigrationConfigTest {
    @Test
    void sqliteMigrationList_includesV7Migration() {
        assertArrayEquals(
                new String[] {
                    "db00c.sql", "db03.sql", "db04.sql", "db05.sql", "db06.sql", "db07c.sql", "db08c.sql", "db09c.sql"
                },
                new SQLiteDatabaseManager().getSqlFiles());
    }

    @Test
    void mysqlMigrationList_includesV7Migration() {
        assertArrayEquals(
                new String[] {
                    "db00.sql", "db03.sql", "db04.sql", "db05.sql", "db06.sql", "db07b.sql", "db08b.sql", "db09b.sql"
                },
                new MySQLDatabaseManager().getSqlFiles());
    }

    @Test
    void postgresqlMigrationList_includesV7Migration() {
        assertArrayEquals(
                new String[] {
                    "db00.sql", "db03.sql", "db04.sql", "db05.sql", "db06.sql", "db07a.sql", "db08a.sql", "db09a.sql"
                },
                new PostgreSQLDatabaseManager().getSqlFiles());
    }
}
