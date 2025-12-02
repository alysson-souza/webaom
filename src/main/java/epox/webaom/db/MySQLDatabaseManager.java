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

import epox.webaom.AppContext;
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
     * Index 0 = schema, Index 1-6 = migrations from version (index-1) to version index.
     */
    private static final String[] SQL_FILES = {
        "db00.sql", // schema (creates at v6) - needs serial->auto_increment conversion
        "db03.sql", // v0 -> v1
        "db04.sql", // v1 -> v2
        "db05.sql", // v2 -> v3
        "db06.sql", // v3 -> v4
        "db07b.sql", // v4 -> v5
        "db08b.sql", // v5 -> v6
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

    @Override
    protected boolean updateSchema() {
        log("Checking database schema...");
        try {
            // Check for ancient legacy schema
            ResultSet rs = query("select state from mylist where lid=0", true);
            if (rs != null && rs.next()) {
                log("Detected legacy database schema (mylist table) - too old to migrate");
                rs.close();
                return false;
            }
            if (rs != null) rs.close();

            // Check current schema version
            rs = query("select ver from vtb", true);
            String[] sqlFiles = getSqlFiles();

            if (rs != null && rs.next()) {
                int version = rs.getInt(1);
                rs.close();
                log("Detected existing database with schema version " + version);

                if (version < 4 && !AppContext.confirm("Warning", """
                    The database definition has to be upgraded.
                    This will make it incompatible with previous versions of WebAOM.
                    Do you want to continue? (Backup now, if needed.)\
                    """, "Yes", "No")) {
                    return false;
                }

                // Apply migrations
                for (int v = version + 1; v < sqlFiles.length; v++) {
                    log("Applying migration: " + sqlFiles[v]);
                    executeStatements(AppContext.getFileString(sqlFiles[v]));
                }
            } else {
                if (rs != null) rs.close();
                // New database - MySQL needs serial -> auto_increment conversion
                log("No existing database found, creating new schema...");
                String schemaSql = AppContext.getFileString(sqlFiles[0]);
                schemaSql = schemaSql.replace("serial", "integer NOT NULL auto_increment");
                log("Using MySQL schema with auto_increment conversion");
                executeStatements(schemaSql);
                log("Database schema created successfully");
            }
        } catch (Exception ex) {
            log("Schema update failed: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        log("Database schema is up to date");
        return true;
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
