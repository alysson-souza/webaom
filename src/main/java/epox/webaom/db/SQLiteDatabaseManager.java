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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite-specific database manager implementation.
 */
public class SQLiteDatabaseManager extends DatabaseManager {
    private static final String SQLITE_JDBC_PREFIX = "jdbc:sqlite:";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    /**
     * SQL files for SQLite.
     * Index 0 = schema, Index 1-6 = migrations from version (index-1) to version index.
     */
    private static final String[] SQL_FILES = {
        "db00c.sql", // schema (creates at v6)
        "db03.sql", // v0 -> v1
        "db04.sql", // v1 -> v2
        "db05.sql", // v2 -> v3
        "db06.sql", // v3 -> v4
        "db07c.sql", // v4 -> v5
        "db08c.sql", // v5 -> v6
    };

    public SQLiteDatabaseManager() {
        this.username = null;
        this.password = null;
    }

    @Override
    protected String[] getSqlFiles() {
        return SQL_FILES;
    }

    @Override
    protected Statement createStatement() throws SQLException {
        // SQLite doesn't support scroll-sensitive or updatable result sets
        return connection.createStatement();
    }

    @Override
    protected Connection openConnection() throws SQLException {
        ensureParentDirectoryExists(connectionUrl);
        // SQLite doesn't need username/password
        return DriverManager.getConnection(connectionUrl);
    }

    private void ensureParentDirectoryExists(String jdbcUrl) throws SQLException {
        if (jdbcUrl == null || !jdbcUrl.startsWith(SQLITE_JDBC_PREFIX)) {
            return;
        }

        String databasePath = jdbcUrl.substring(SQLITE_JDBC_PREFIX.length());
        if (databasePath.isEmpty() || databasePath.startsWith(":memory:") || databasePath.startsWith(":resource:")) {
            return;
        }

        int queryIndex = databasePath.indexOf('?');
        if (queryIndex >= 0) {
            databasePath = databasePath.substring(0, queryIndex);
        }

        if (databasePath.startsWith("file:")) {
            if (databasePath.startsWith("file::memory:")) {
                return;
            }
            databasePath = databasePath.substring("file:".length());
        }

        if (databasePath.startsWith("~/")) {
            databasePath = System.getProperty("user.home") + databasePath.substring(1);
        }

        try {
            Path parentPath = Paths.get(databasePath).toAbsolutePath().getParent();
            if (parentPath != null && !Files.exists(parentPath)) {
                Files.createDirectories(parentPath);
            }
        } catch (Exception ex) {
            throw new SQLException("Failed to create SQLite database directory for: " + databasePath, ex);
        }
    }

    @Override
    protected int getLastInsertId() throws SQLException {
        try (ResultSet rs = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    protected String escapeString(String value) {
        if (value == null) {
            return "";
        }
        // SQLite uses doubled single quotes for escaping
        return value.replace("'", "''");
    }

    @Override
    public String quoteString(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + escapeString(value) + "'";
    }
}
