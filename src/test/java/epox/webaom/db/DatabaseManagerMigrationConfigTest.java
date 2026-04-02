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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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

    @Test
    void mysqlFutureSchemaValidation_rejectsNewerDatabaseVersion() {
        GuardAwareMySQLDatabaseManager manager = new GuardAwareMySQLDatabaseManager();

        assertFalse(manager.validateSchemaVersion(8));
        assertEquals(8, manager.warnedDetectedVersion);
        assertEquals(7, manager.warnedSupportedVersion);
    }

    @Test
    void mysqlUpdateSchema_rejectsFutureVersionBeforeRunningMigrations() {
        SchemaAwareMySQLDatabaseManager manager = new SchemaAwareMySQLDatabaseManager();
        manager.queueQueryResult(null);
        manager.queueQueryResult(resultSetWithSingleIntRow(8));

        assertFalse(manager.runUpdateSchema());
        assertEquals(8, manager.warnedDetectedVersion);
        assertEquals(7, manager.warnedSupportedVersion);
        assertTrue(manager.executedBatches.isEmpty());
    }

    @Test
    void sharedSchemaValidation_acceptsSupportedVersionWithoutWarning() {
        GuardAwareMySQLDatabaseManager manager = new GuardAwareMySQLDatabaseManager();

        assertTrue(manager.validateSchemaVersion(7));
        assertEquals(-1, manager.warnedDetectedVersion);
        assertEquals(-1, manager.warnedSupportedVersion);
    }

    @Test
    void mysqlUpdateSchema_migratesOlderVersionWithoutWarning() {
        SchemaAwareMySQLDatabaseManager manager = new SchemaAwareMySQLDatabaseManager();
        manager.queueQueryResult(null);
        manager.queueQueryResult(resultSetWithSingleIntRow(6));

        assertTrue(manager.runUpdateSchema());
        assertEquals(-1, manager.warnedDetectedVersion);
        assertEquals(-1, manager.warnedSupportedVersion);
        assertEquals(1, manager.executedBatches.size());
        assertTrue(manager.executedBatches.get(0).contains("ALTER TABLE jtb ADD COLUMN jobs_visible"));
    }

    @Test
    void futureSchemaWarningMessage_matchesSimpleUserFacingCopy() {
        String message = DatabaseManager.buildFutureSchemaWarningMessage(8, 7);

        assertEquals("""
            This data is from a newer version of WebAOM.

            Please update WebAOM.\
            """, message);
    }

    private static final class GuardAwareMySQLDatabaseManager extends MySQLDatabaseManager {
        private int warnedDetectedVersion = -1;
        private int warnedSupportedVersion = -1;

        private boolean validateSchemaVersion(int version) {
            return validateExistingSchemaVersion(version);
        }

        @Override
        protected void showFutureSchemaWarning(int detectedVersion, int supportedVersion) {
            warnedDetectedVersion = detectedVersion;
            warnedSupportedVersion = supportedVersion;
        }
    }

    private static final class SchemaAwareMySQLDatabaseManager extends MySQLDatabaseManager {
        private static final Object NULL_RESULT = new Object();

        private final Deque<Object> queryResults = new ArrayDeque<>();
        private final List<String> executedBatches = new ArrayList<>();
        private int warnedDetectedVersion = -1;
        private int warnedSupportedVersion = -1;

        private void queueQueryResult(ResultSet resultSet) {
            queryResults.addLast(resultSet == null ? NULL_RESULT : resultSet);
        }

        private boolean runUpdateSchema() {
            isConnectionReady = true;
            return updateSchema();
        }

        @Override
        protected ResultSet query(String command, boolean silent) {
            if (queryResults.isEmpty()) {
                return null;
            }
            Object nextResult = queryResults.removeFirst();
            return nextResult == NULL_RESULT ? null : (ResultSet) nextResult;
        }

        @Override
        protected void executeStatements(String sqlBatch) {
            executedBatches.add(sqlBatch);
        }

        @Override
        protected void showFutureSchemaWarning(int detectedVersion, int supportedVersion) {
            warnedDetectedVersion = detectedVersion;
            warnedSupportedVersion = supportedVersion;
        }
    }

    private static ResultSet resultSetWithSingleIntRow(int value) {
        return (ResultSet) Proxy.newProxyInstance(
                DatabaseManagerMigrationConfigTest.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                new java.lang.reflect.InvocationHandler() {
                    private boolean beforeFirst = true;

                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args)
                            throws Throwable {
                        return switch (method.getName()) {
                            case "next" -> {
                                if (beforeFirst) {
                                    beforeFirst = false;
                                    yield true;
                                }
                                yield false;
                            }
                            case "getInt" -> value;
                            case "close" -> null;
                            case "wasNull" -> false;
                            case "unwrap" -> null;
                            case "isWrapperFor" -> false;
                            default -> {
                                Class<?> returnType = method.getReturnType();
                                if (returnType == boolean.class) {
                                    yield false;
                                }
                                if (returnType == int.class) {
                                    yield 0;
                                }
                                if (returnType == long.class) {
                                    yield 0L;
                                }
                                if (returnType == double.class) {
                                    yield 0d;
                                }
                                if (returnType == float.class) {
                                    yield 0f;
                                }
                                yield null;
                            }
                        };
                    }
                });
    }
}
