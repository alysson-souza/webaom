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

package epox.webaom.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.swing.Log;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AniDBConnectionTest {
    private boolean previousShutdown;

    @BeforeEach
    void setUp() {
        previousShutdown = AniDBConnection.isShutdown();
        AniDBConnection.setShutdown(false);
    }

    @AfterEach
    void tearDown() {
        AniDBConnection.setShutdown(previousShutdown);
    }

    @Test
    void initializesTagStateAndExposesShutdownFlag() {
        TestConnection connection = connection(settings(false));

        assertEquals("t00001", connection.currentTag);
        assertFalse(connection.isLoggedIn());
        assertEquals("Not Initialized.", connection.getLastError());

        AniDBConnection.setShutdown(true);
        assertTrue(AniDBConnection.isShutdown());
    }

    @Test
    void rejectsNullCommandsAsClientBugs() {
        TestConnection connection = connection(settings(false));

        AniDBException operationError = assertThrows(AniDBException.class, () -> connection.send(null, null, true));
        AniDBException rawCommandError = assertThrows(AniDBException.class, () -> connection.send(null, true));

        assertTrue(operationError.is(AniDBException.CLIENT_BUG));
        assertTrue(rawCommandError.is(AniDBException.CLIENT_BUG));
    }

    @Test
    void appendsTagsAndFormatsStringCommandResponses() throws Exception {
        TestConnection connection = connection(settings(false));
        connection.respond(response("220 FILE\npayload"));

        String result = connection.send("FILE size=100", true);

        assertEquals("220 FILE\npayload", result);
        assertEquals("FILE size=100&tag=t00002", connection.commands.getFirst());
        assertTrue(connection.waitFlags.getFirst());
    }

    @Test
    void retriesTimeoutsWithANewTagAndIncreasedPacketDelay() throws Exception {
        AniDBConnectionSettings settings = settings(false);
        TestConnection connection = connection(settings);
        connection.fail(new SocketTimeoutException("first timeout"));
        connection.respond(response("220 FILE\ndata"));

        AniDBConnectionResponse result = connection.send("FILE", "size=100", true);

        assertEquals(220, result.code);
        assertEquals(4100, settings.packetDelay);
        assertEquals(2, connection.commands.size());
        assertNotEquals(connection.tagFromCommand(0), connection.tagFromCommand(1));
        assertTrue(connection.log.lines.getFirst().contains("Operation Failed: TIMEOUT"));
    }

    @Test
    void retriesIoFailuresWithoutChangingPacketDelay() throws Exception {
        AniDBConnectionSettings settings = settings(false);
        TestConnection connection = connection(settings);
        connection.fail(new IOException("socket closed"));
        connection.respond(response("220 FILE\ndata"));

        assertEquals(220, connection.send("FILE", null, true).code);
        assertEquals(4000, settings.packetDelay);
        assertTrue(connection.getLastError().contains("IOEXCEPT: socket closed"));
    }

    @Test
    void exhaustedRetriesAndShutdownReportDistinctFailures() {
        AniDBConnectionSettings oneAttempt = new AniDBConnectionSettings("localhost", 9000, 0, 1, 0, 1, false);
        TestConnection unreachable = connection(oneAttempt);
        unreachable.fail(new SocketTimeoutException("timeout"));

        AniDBException unreachableError =
                assertThrows(AniDBException.class, () -> unreachable.send("FILE", null, true));
        assertTrue(unreachableError.is(AniDBException.ANIDB_UNREACHABLE));

        AniDBConnection.setShutdown(true);
        AniDBException shutdownError = assertThrows(AniDBException.class, () -> unreachable.send("FILE", null, true));
        assertTrue(shutdownError.is(AniDBException.CLIENT_SYSTEM));
        assertEquals("Connection shutdown", shutdownError.getMessage());
    }

    @Test
    void invalidSessionTriggersLoginAndRetriesWithTheNewSession() throws Exception {
        TestConnection connection = connection(settings(false));
        connection.set("user", "password", null);
        connection.respond(response("501 LOGIN FIRST"));
        connection.respond(response("200 abcde LOGIN ACCEPTED"));
        connection.respond(response("220 FILE\ndata"));

        AniDBConnectionResponse result = connection.send("FILE", "size=100", true);

        assertEquals(220, result.code);
        assertEquals("abcde", connection.session);
        assertTrue(connection.isLoggedIn());
        assertTrue(connection.commands.get(1).startsWith("AUTH user=user&pass=password&protover=3"));
        assertTrue(connection.commands.get(2).contains("size=100&s=abcde&tag="));
    }

    @Test
    void acceptedLoginStoresOnlyTheFiveCharacterSessionPrefix() throws Exception {
        TestConnection connection = connection(settings(true));
        connection.set("user", "password", null);
        connection.respond(response("200 abcde:4567 LOGIN ACCEPTED"));

        assertTrue(connection.login());

        assertEquals("abcde", connection.session);
        assertTrue(connection.isLoggedIn());
        assertTrue(connection.commands.getFirst().contains("protover=3&client=webaom&clientver=119"));
    }

    @Test
    void rejectedLoginHonorsTheDisabledPromptBoundary() throws Exception {
        TestConnection connection = connection(settings(false));
        connection.set("user", "wrong", null);
        connection.setLoginPromptEnabled(false);
        connection.respond(response("500 LOGIN FAILED"));

        AniDBException exception = assertThrows(AniDBException.class, connection::login);

        assertTrue(exception.is(AniDBException.CLIENT_USER));
        assertEquals("AniDB authentication failed.", exception.getMessage());
        assertEquals("Login Failed", connection.getLastError());
    }

    @Test
    void encryptionSetupHandlesMissingKeysSaltAndAniDbRejections() throws Exception {
        TestConnection noKey = connection(settings(false));
        noKey.set("user", "password", null);
        noKey.respond(response("300 PONG"));
        assertEquals(0, noKey.encrypt());
        assertTrue(noKey.commands.getFirst().startsWith("PING"));

        TestConnection encrypted = connection(settings(false));
        encrypted.set("user", "password", "api-key");
        encrypted.respond(response("209 salt ENCRYPTION ENABLED"));
        assertEquals(0, encrypted.encrypt());
        assertTrue(encrypted.commands.getFirst().startsWith("ENCRYPT user=user&type=1&tag="));

        TestConnection undefined = connection(settings(false));
        undefined.set("user", "password", "api-key");
        undefined.respond(response("309 API PASSWORD NOT DEFINED"));
        AniDBException undefinedError = assertThrows(AniDBException.class, undefined::encrypt);
        assertTrue(undefinedError.is(AniDBException.ENCRYPTION));

        TestConnection unknownUser = connection(settings(false));
        unknownUser.set("user", "password", "api-key");
        unknownUser.respond(response("394 NO SUCH USER"));
        assertEquals(
                "No such user. Check username.",
                assertThrows(AniDBException.class, unknownUser::encrypt).getMessage());

        TestConnection unset = connection(settings(false));
        // TODO: Review whether encrypt() should report a checked authentication error before credentials are set.
        assertThrows(NullPointerException.class, unset::encrypt);
    }

    @Test
    void normalAndShutdownLogoutPathsClearAuthentication() throws Exception {
        TestConnection normal = loggedInConnection();
        normal.respond(response("203 LOGGED OUT"));

        assertTrue(normal.logout());
        assertFalse(normal.isLoggedIn());
        assertTrue(normal.commands.getLast().startsWith("LOGOUT s=abcde&tag="));

        TestConnection shutdown = loggedInConnection();
        AniDBConnection.setShutdown(true);
        assertTrue(shutdown.logout());
        assertFalse(shutdown.isLoggedIn());
        assertEquals("LOGOUT s=abcde", shutdown.commands.getLast());
        assertFalse(shutdown.waitFlags.getLast());
    }

    @Test
    void keepAliveActionOnlyPingsWhenNatIsEnabled() throws Exception {
        TestConnection disabled = connection(settings(false));
        disabled.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "timer"));
        assertTrue(disabled.commands.isEmpty());

        TestConnection enabled = connection(settings(true));
        enabled.respond(response("300 PONG"));
        enabled.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "timer"));
        assertTrue(enabled.commands.getFirst().startsWith("PING"));

        TestConnection timeout = connection(settings(true));
        timeout.fail(new SocketTimeoutException("timeout"));
        timeout.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "timer"));
        timeout.disconnect();
        assertEquals(1, timeout.commands.size());
    }

    @Test
    void connectAndDisconnectUseOnlyTheConfiguredLocalSocket() {
        AniDBConnection connection = new AniDBConnection(null, socketSettings(0, 9));

        assertTrue(connection.connect());
        connection.disconnect();
        assertFalse(connection.isLoggedIn());
    }

    @Test
    void connectReportsAnOccupiedLocalPort() throws Exception {
        try (DatagramSocket occupied = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            AniDBConnection connection = new AniDBConnection(null, socketSettings(occupied.getLocalPort(), 9));

            assertFalse(connection.connect());
            assertTrue(connection.getLastError().startsWith("SocketException:"));
            connection.disconnect();
        }
    }

    @Test
    void loopbackTransportDecodesCompressedUtf8AndSkipsWrongTags() throws Exception {
        try (DatagramSocket server = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout((int) Duration.ofSeconds(2).toMillis());
            DebugConnection connection = new DebugConnection(socketSettings(0, server.getLocalPort()));
            assertTrue(connection.connect());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> serverTask = executor.submit(() -> serveWrongTagThenCompressedUtf8(server));
            try {
                AniDBConnectionResponse result = connection.send("FILE", "user=u&pass=secret&enc=utf8", true);

                assertEquals(220, result.code);
                assertEquals("café", result.data);
                assertTrue(connection.debugMessages.stream().anyMatch(message -> message.contains("pass=xxxxx")));
                assertTrue(connection.debugMessages.stream().noneMatch(message -> message.contains("pass=secret")));
                serverTask.get(3, TimeUnit.SECONDS);
            } finally {
                connection.disconnect();
                server.close();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
            }
        }
    }

    private TestConnection loggedInConnection() throws Exception {
        TestConnection connection = connection(settings(false));
        connection.set("user", "password", null);
        connection.respond(response("200 abcde LOGIN ACCEPTED"));
        assertTrue(connection.login());
        return connection;
    }

    private static TestConnection connection(AniDBConnectionSettings settings) {
        return new TestConnection(new RecordingLog(), settings);
    }

    private static AniDBConnectionSettings settings(boolean natEnabled) {
        return new AniDBConnectionSettings("localhost", 9000, 0, 1, 4000, 3, natEnabled);
    }

    private static AniDBConnectionSettings socketSettings(int localPort, int remotePort) {
        return new AniDBConnectionSettings("127.0.0.1", remotePort, localPort, 2, 0, 1, false);
    }

    private static AniDBConnectionResponse response(String raw) throws Exception {
        return new AniDBConnectionResponse(null, 0, raw);
    }

    private static void serveWrongTagThenCompressedUtf8(DatagramSocket server) {
        try {
            byte[] requestBuffer = new byte[4096];
            DatagramPacket request = new DatagramPacket(requestBuffer, requestBuffer.length);
            server.receive(request);
            String command = new String(request.getData(), 0, request.getLength(), StandardCharsets.US_ASCII);
            int tagIndex = command.indexOf("tag=");
            String tag = command.substring(tagIndex + 4, tagIndex + 10);

            send(server, request, "t99999 220 FILE\nignored\n".getBytes(StandardCharsets.UTF_8));
            send(server, request, compress(tag + " 220 FILE\ncafé\n"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static byte[] compress(String response) {
        byte[] input = response.getBytes(StandardCharsets.UTF_8);
        Deflater compressor = new Deflater();
        try {
            compressor.setInput(input);
            compressor.finish();
            byte[] compressed = new byte[1024];
            int compressedLength = compressor.deflate(compressed);
            byte[] packet = new byte[compressedLength + 2];
            System.arraycopy(compressed, 0, packet, 2, compressedLength);
            return packet;
        } finally {
            compressor.end();
        }
    }

    private static void send(DatagramSocket server, DatagramPacket request, byte[] data) throws IOException {
        server.send(new DatagramPacket(data, data.length, request.getAddress(), request.getPort()));
    }

    private record Failure(Exception exception) {}

    private static final class TestConnection extends AniDBConnection {
        private final RecordingLog log;
        private final Deque<Object> results = new ArrayDeque<>();
        private final List<String> commands = new ArrayList<>();
        private final List<Boolean> waitFlags = new ArrayList<>();

        TestConnection(RecordingLog log, AniDBConnectionSettings settings) {
            super(log, settings);
            this.log = log;
        }

        void respond(AniDBConnectionResponse response) {
            results.addLast(response);
        }

        void fail(Exception exception) {
            results.addLast(new Failure(exception));
        }

        String tagFromCommand(int index) {
            String command = commands.get(index);
            return command.substring(command.indexOf("tag=") + 4);
        }

        @Override
        AniDBConnectionResponse sendRaw(String command, boolean wait) throws IOException, AniDBException {
            commands.add(command);
            waitFlags.add(wait);
            if (results.isEmpty()) {
                return null;
            }
            Object result = results.removeFirst();
            if (result instanceof Failure failure) {
                if (failure.exception() instanceof IOException ioException) {
                    throw ioException;
                }
                if (failure.exception() instanceof AniDBException aniDBException) {
                    throw aniDBException;
                }
                throw new AssertionError(failure.exception());
            }
            return (AniDBConnectionResponse) result;
        }
    }

    private static final class DebugConnection extends AniDBConnection {
        private final List<String> debugMessages = new ArrayList<>();

        DebugConnection(AniDBConnectionSettings settings) {
            super(null, settings);
        }

        @Override
        protected void debug(String message) {
            debugMessages.add(message);
        }
    }

    private static final class RecordingLog implements Log {
        private final List<String> lines = new ArrayList<>();

        @Override
        public void println(Object message) {
            lines.add(String.valueOf(message));
        }

        @Override
        public void status0(String message) {}

        @Override
        public void status1(String message) {
            lines.add(message);
        }
    }
}
