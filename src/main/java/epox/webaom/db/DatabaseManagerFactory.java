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

import epox.webaom.util.PlatformPaths;

/**
 * Factory for creating the appropriate DatabaseManager based on connection string.
 */
public final class DatabaseManagerFactory {

    private DatabaseManagerFactory() {}

    /**
     * Creates a DatabaseManager for the given connection string.
     *
     * @param connectionString JDBC connection string (e.g., "jdbc:sqlite:...", "jdbc:postgresql:...", "jdbc:mysql:...")
     * @return the appropriate DatabaseManager implementation, or null if unsupported
     */
    public static DatabaseManager create(String connectionString) {
        if (connectionString == null || connectionString.isEmpty()) {
            return createEmbedded();
        }

        String lowerCase = connectionString.toLowerCase();

        if (lowerCase.contains("sqlite")) {
            return new SQLiteDatabaseManager();
        } else if (lowerCase.contains("postgresql")) {
            return new PostgreSQLDatabaseManager();
        } else if (lowerCase.contains("mysql")) {
            return new MySQLDatabaseManager();
        }

        // Unknown database type - try fallback to embedded SQLite
        System.out.println("Unknown database type in connection string: " + connectionString);
        System.out.println("Falling back to embedded SQLite database");
        return createEmbedded();
    }

    /**
     * Creates a DatabaseManager for the embedded SQLite database.
     *
     * @return SQLiteDatabaseManager configured for the default embedded path
     */
    public static DatabaseManager createEmbedded() {
        return new SQLiteDatabaseManager();
    }

    /**
     * Gets the default embedded database connection string.
     *
     * @return JDBC connection string for the embedded SQLite database
     */
    public static String getEmbeddedConnectionString() {
        String dbPath = PlatformPaths.getDefaultEmbeddedDatabasePath();
        return "jdbc:sqlite:" + dbPath;
    }

    /**
     * Attempts to create and initialize a database from the given connection string,
     * falling back to embedded SQLite if the connection fails.
     *
     * @param connectionString the primary connection string to try
     * @return an initialized DatabaseManager, or null if all attempts fail
     */
    public static DatabaseManager createWithFallback(String connectionString) {
        DatabaseManager manager = create(connectionString);

        if (manager != null && manager.initialize(connectionString)) {
            return manager;
        }

        // Primary connection failed, try embedded SQLite
        if (connectionString != null && !connectionString.toLowerCase().contains("sqlite")) {
            System.out.println("Primary database connection failed, attempting embedded SQLite...");
            String embeddedConnection = getEmbeddedConnectionString();
            manager = createEmbedded();

            if (manager.initialize(embeddedConnection)) {
                System.out.println("Successfully initialized embedded SQLite database");
                return manager;
            }
        }

        System.out.println("All database connection attempts failed");
        return null;
    }
}
