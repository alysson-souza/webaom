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
 * PostgreSQL-specific database manager implementation.
 */
public class PostgreSQLDatabaseManager extends DatabaseManager {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }

    /**
     * SQL files for PostgreSQL.
     * Index 0 = schema, Index 1-6 = migrations from version (index-1) to version index.
     */
    private static final String[] SQL_FILES = {
        "db00.sql", // schema (creates at v6)
        "db03.sql", // v0 -> v1
        "db04.sql", // v1 -> v2
        "db05.sql", // v2 -> v3
        "db06.sql", // v3 -> v4
        "db07a.sql", // v4 -> v5
        "db08a.sql", // v5 -> v6
    };

    public PostgreSQLDatabaseManager() {
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
        // PostgreSQL uses currval with sequence name
        try (ResultSet rs = statement.executeQuery("SELECT currval('dtb_did_seq')")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public int getDirectoryId(String path) {
        if (!isInitialized) {
            return -1;
        }
        if (path == null) {
            path = "";
        }
        try {
            String escapedPath = escapeString(path);
            Integer cachedId = directoryIdCache.get(path);
            if (cachedId != null) {
                return cachedId;
            }

            ResultSet rs = query("select did from dtb where name='" + escapedPath + "'", false);
            if (rs != null && rs.next()) {
                int directoryId = rs.getInt(1);
                rs.close();
                directoryIdCache.put(path, directoryId);
                return directoryId;
            }
            if (rs != null) rs.close();

            // PostgreSQL: use INSERT with SELECT currval in same statement
            log("} insert into dtb (name) values ('" + escapedPath + "')");
            statement.execute("insert into dtb (name) values ('" + escapedPath + "');SELECT currval('dtb_did_seq')");
            if (statement.getMoreResults()) {
                rs = statement.getResultSet();
                if (rs.next()) {
                    int directoryId = rs.getInt(1);
                    rs.close();
                    directoryIdCache.put(path, directoryId);
                    return directoryId;
                }
                if (rs != null) rs.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    @Override
    protected String escapeString(String value) {
        if (value == null) {
            return "";
        }
        // PostgreSQL uses doubled single quotes for escaping
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
