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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL-specific database manager implementation.
 */
public class MySQLDatabaseManager extends DatabaseManager {

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }
    }

    /**
     * SQL files for MySQL.
     * Index 0 = schema, Index 1-7 = migrations from version (index-1) to version index.
     */
    private static final String[] SQL_FILES = {
        "db00.sql", // schema (creates at v7) - needs serial->auto_increment conversion
        "db03.sql", // v0 -> v1
        "db04.sql", // v1 -> v2
        "db05.sql", // v2 -> v3
        "db06.sql", // v3 -> v4
        "db07b.sql", // v4 -> v5
        "db08b.sql", // v5 -> v6
        "db09b.sql", // v6 -> v7
    };

    public MySQLDatabaseManager() {
        this.username = "root";
        this.password = null;
    }

    @Override
    protected String[] getSqlFiles() {
        return SQL_FILES;
    }

    @Override
    protected Statement createStatement() throws SQLException {
        return connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }

    @Override
    protected Connection openConnection() throws SQLException {
        if (password != null) {
            return DriverManager.getConnection(connectionUrl, username, password);
        }
        return DriverManager.getConnection(connectionUrl + "?user=" + username);
    }

    @Override
    protected int getLastInsertId() throws SQLException {
        try (ResultSet rs = statement.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    /** MySQL has no `serial` type; the shipped schema uses it for auto-incrementing keys. */
    @Override
    protected String prepareSchemaSql(String sqlBatch) {
        return sqlBatch.replace("serial", "integer NOT NULL auto_increment");
    }

    /**
     * MySQL commits implicitly on DDL, so wrapping a migration in a transaction would give a false
     * sense of safety: a failure part-way through a migration file cannot be rolled back. The batch
     * still stops at the first failing statement, which keeps the recorded version untouched.
     */
    @Override
    protected boolean supportsTransactionalSchemaChanges() {
        return false;
    }

    @Override
    protected String escapeString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    @Override
    public String quoteString(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + escapeString(value) + "'";
    }
}
