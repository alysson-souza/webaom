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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.db.DatabaseManager;
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBConnectionSettings;
import epox.webaom.net.AniDBFileClient;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

class CliApplicationNetworkTest {
    @TempDir
    Path temporaryDirectory;

    private String previousUsername;
    private String previousPassword;
    private String previousApiKey;

    @BeforeEach
    void setCredentials() {
        previousUsername = AppContext.userPass.username;
        previousPassword = AppContext.userPass.password;
        previousApiKey = AppContext.userPass.apiKey;
        AppContext.userPass.username = "user";
        AppContext.userPass.password = "password";
        AppContext.userPass.apiKey = null;
    }

    @AfterEach
    void restoreCredentials() {
        AppContext.userPass.username = previousUsername;
        AppContext.userPass.password = previousPassword;
        AppContext.userPass.apiKey = previousApiKey;
    }

    @Test
    void connectsBeforeLoginAndDisconnectsAfterLogout() throws Exception {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(true);
        when(client.encrypt()).thenReturn(25);
        when(client.login()).thenReturn(true);
        CliApplication application = application(client);

        assertSame(client, application.connection());
        application.close();

        InOrder order = inOrder(client);
        order.verify(client).connect();
        order.verify(client).encrypt();
        order.verify(client).login();
        order.verify(client).logout();
        order.verify(client).disconnect();
    }

    @Test
    void disconnectsWhenTheSocketCannotBeOpened() throws Exception {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(false);
        when(client.getLastError()).thenReturn("Cannot bind local port");
        CliApplication application = application(client);

        CliException exception = assertThrows(CliException.class, application::connection);

        assertEquals(2, exception.exitCode());
        verify(client, never()).login();
        verify(client).disconnect();
    }

    @Test
    void disconnectsWhenEncryptionSetupFails() throws Exception {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(true);
        when(client.encrypt()).thenThrow(new IOException("encryption handshake failed"));
        CliApplication application = application(client);

        CliException exception = assertThrows(CliException.class, application::connection);

        assertEquals(2, exception.exitCode());
        verify(client, never()).login();
        verify(client).disconnect();
    }

    @Test
    void refusesPlaintextLoginWhenEncryptionIsRejected() throws Exception {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(true);
        when(client.encrypt()).thenReturn(-1);
        CliApplication application = application(client);

        CliException exception = assertThrows(CliException.class, application::connection);

        assertEquals(2, exception.exitCode());
        verify(client, never()).login();
        verify(client).disconnect();
    }

    @Test
    void preservesZeroAsAnOsAssignedLocalPort() {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(false);
        Options options = new Options(temporaryDirectory.resolve("config"));
        options.setInteger(Options.INT_LOCAL_PORT, 0);
        AtomicReference<AniDBConnectionSettings> capturedSettings = new AtomicReference<>();
        CliApplication application = new CliApplication(options, new Rules(), Set.of("mkv"), settings -> {
            capturedSettings.set(settings);
            return client;
        });

        assertThrows(CliException.class, application::connection);

        assertEquals(0, capturedSettings.get().localPort);
    }

    @Test
    void disconnectsWhenAuthenticationIsRejected() throws Exception {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(true);
        when(client.encrypt()).thenReturn(20);
        when(client.login()).thenReturn(false);

        CliException exception = assertThrows(CliException.class, application(client)::connection);

        assertEquals("AniDB authentication failed.", exception.getMessage());
        verify(client).disconnect();
    }

    @Test
    void invalidConnectionOptionsFallBackToProtocolDefaults() {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(false);
        Options options = new Options(temporaryDirectory.resolve("invalid-options"));
        options.setInteger(Options.INT_REMOTE_PORT, -1);
        options.setInteger(Options.INT_LOCAL_PORT, 70_000);
        options.setInteger(Options.INT_TIMEOUT, 0);
        options.setInteger(Options.INT_DATAGRAM_DELAY, -1);
        AtomicReference<AniDBConnectionSettings> captured = new AtomicReference<>();
        CliApplication application = new CliApplication(options, new Rules(), Set.of("mkv"), settings -> {
            captured.set(settings);
            return client;
        });

        assertThrows(CliException.class, application::connection);

        AniDBConnectionSettings settings = captured.get();
        assertEquals(9_000, settings.remotePort);
        assertEquals(AniDBConnection.DEFAULT_LOCAL_PORT, settings.localPort);
        assertEquals(20_000, settings.timeoutMillis);
        assertEquals(4_000, settings.packetDelay);
    }

    @Test
    void doctorAuthenticatesWhenDatabaseAndCredentialsAreAvailable() throws Exception {
        DatabaseManager previousDatabase = AppContext.databaseManager;
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.isConnected()).thenReturn(true);
        AppContext.databaseManager = database;
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(true);
        when(client.encrypt()).thenReturn(20);
        when(client.login()).thenReturn(true);
        try {
            assertTrue(application(client).doctor(silentOutput()));
        } finally {
            AppContext.databaseManager = previousDatabase;
        }
    }

    @Test
    void doctorReportsAniDbAuthenticationFailureAsUnhealthy() throws Exception {
        DatabaseManager previousDatabase = AppContext.databaseManager;
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.isConnected()).thenReturn(true);
        AppContext.databaseManager = database;
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(false);
        when(client.getLastError()).thenReturn("offline");
        try {
            assertFalse(application(client).doctor(silentOutput()));
        } finally {
            AppContext.databaseManager = previousDatabase;
        }
    }

    private CliApplication application(AniDBFileClient client) {
        Options options = new Options(temporaryDirectory.resolve("config"));
        return new CliApplication(options, new Rules(), Set.of("mkv"), settings -> client);
    }

    private static CliOutput silentOutput() {
        PrintWriter discard = new PrintWriter(OutputStream.nullOutputStream());
        return new CliOutput(discard, discard, CliOutput.Format.TABLE, true, false);
    }
}
