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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.swing.Log;
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBConnectionResponse;
import epox.webaom.net.AniDBException;
import epox.webaom.net.AniDBFileClient;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChiiEmuTest {
    private AniDBFileClient previousConnection;
    private AniDBFileClient connection;

    @BeforeEach
    void setUp() {
        previousConnection = AppContext.conn;
        connection = mock(AniDBFileClient.class);
        AppContext.conn = connection;
    }

    @AfterEach
    void tearDown() {
        AppContext.conn = previousConnection;
    }

    @Test
    void reportsMissingLoginWithoutStartingAWorker() throws Exception {
        AppContext.conn = null;
        QueueLog log = new QueueLog();
        ChiiEmu emu = new ChiiEmu(connection);
        emu.setLog(log);

        emu.handleCommand("!uptime");

        assertEquals("Not logged in. Click the Login button first.", log.awaitMessage());
    }

    @Test
    void exposesAllLegacyUsageMessages() {
        assertTrue(ChiiEmu.getUsageMessage(ChiiEmu.USAGE_WATCHED).startsWith("WATCHED: usage:"));
        assertTrue(ChiiEmu.getUsageMessage(ChiiEmu.USAGE_STATE).startsWith("STATE: usage:"));
        assertTrue(ChiiEmu.getUsageMessage(ChiiEmu.USAGE_STATE2).startsWith("!state2"));
        assertTrue(ChiiEmu.getUsageMessage(ChiiEmu.USAGE_STORAGE).startsWith("!storage"));
        assertEquals("NOO!", ChiiEmu.getUsageMessage(-1));
    }

    @Test
    void printlnIgnoresMessagesWithoutALogAndLabelsNullMessages() throws Exception {
        ChiiEmu emu = new ChiiEmu(connection);
        emu.println("ignored");
        QueueLog log = new QueueLog();
        emu.setLog(log);

        emu.println(null);

        assertEquals("err..", log.awaitMessage());
    }

    @Test
    void workerFormatsUptimeAndPropagatesAniDbErrors() throws Exception {
        when(connection.send(eq("UPTIME"), isNull(), eq(true)))
                .thenReturn(response("208 UPTIME\n90061000"))
                .thenThrow(new AniDBException(AniDBException.CLIENT_SYSTEM, "offline"));

        assertEquals("UPTIME: 1d 1h 1m", runCommand("!uptime"));
        assertEquals("offline", runCommand("!uptime"));
    }

    @Test
    void workerRoutesIdAndNameQueriesAndFormatsTheirResponses() throws Exception {
        String anime = "123|12|12|0|850|100|0|0|900|5|2024|TV|Romaji|Kanji|English";
        String group = "7|800|20|4|50|Group Name|GN|channel|server|https://group.example";
        when(connection.send("ANIME", "aid=123", true)).thenReturn(response("230 ANIME\n" + anime));
        when(connection.send("ANIME", "aname=Cowboy Bebop", true)).thenReturn(response("230 ANIME\n" + anime));
        when(connection.send("GROUP", "gname=Group Name", true)).thenReturn(response("250 GROUP\n" + group));
        when(connection.send("MYLIST", "aid=123", true)).thenReturn(response("221 MYLIST\n1|2|3|4|5|6|1"));

        assertTrue(runCommand("!anime 123").startsWith("ANIME: Romaji (123), English, 12 eps"));
        assertTrue(runCommand("!anime Cowboy Bebop").contains("https://anidb.net/a123"));
        assertTrue(runCommand("!group Group Name").startsWith("GROUP: Group Name [GN] (7)"));
        assertEquals("MYLIST: lid=1 (On HDD)", runCommand("!mylist 123"));
        verify(connection).send("ANIME", "aid=123", true);
        verify(connection).send("ANIME", "aname=Cowboy Bebop", true);
        verify(connection).send("GROUP", "gname=Group Name", true);
    }

    @Test
    void workerPassesRawCommandsThroughAndRejectsUnknownOrIncompleteCommands() throws Exception {
        when(connection.send("PING", true)).thenReturn("raw result");

        assertEquals("raw result", runCommand("?PING"));
        assertEquals("Unknown command", runCommand("!does-not-exist"));
        assertEquals("No Command", runCommand("x"));
        assertEquals("Usage: !mylist <anime name or id>", runCommand("!mylist"));
        assertEquals("Usage: !anime <anime name or id>", runCommand("!anime"));
        assertEquals("Usage: !group <group name or id>", runCommand("!group"));
        assertEquals("Usage: !randomanime [watched|unwatched|all]", runCommand("!randomanime"));
    }

    @Test
    void workerFormatsMylistStatisticsIncludingOptionalViewedTime() throws Exception {
        String stats = "10|20|30|2|4|5|6|7|8|9|10|11|12|13|14|15|1501";
        when(connection.send(eq("MYLISTSTATS"), isNull(), eq(true))).thenReturn(response("222 MYLIST STATS\n" + stats));

        String result = runCommand("!mystats");

        assertTrue(result.startsWith("MYSTATS: 10 animes, 20 eps (13 / 12% watched)"));
        assertTrue(result.contains("4 animes, 5 eps, 6 files, 7 groups"));
        assertTrue(result.endsWith("Viewed: 1d 1h 1m."));
    }

    @Test
    void workerMapsRandomAnimeModesAndCurrentlyFallsBackToTypeZero() throws Exception {
        String anime = "123|12|12|0|850|100|0|0|900|5|2024|TV|Romaji|Kanji|English";
        when(connection.send("RANDOMANIME", "type=1", true)).thenReturn(response("232 RANDOM ANIME\n" + anime));
        when(connection.send("RANDOMANIME", "type=0", true)).thenReturn(response("232 RANDOM ANIME\n" + anime));

        assertTrue(runCommand("!randomanime watched").startsWith("RANDOM ANIME: Romaji"));
        // TODO: Review whether an unknown random-anime mode should be rejected instead of being sent as type zero.
        assertTrue(runCommand("!randomanime Watched").startsWith("RANDOM ANIME: Romaji"));
        verify(connection).send("RANDOMANIME", "type=0", true);
    }

    @Test
    void watchedUpdatesBuildLegacyFileAnimeAndEd2kRequests() throws Exception {
        AniDBConnection directConnection = mock(AniDBConnection.class);
        when(directConnection.send(eq("MYLISTADD"), anyString(), eq(true)))
                .thenReturn(response("311 MYLIST ENTRY EDITED\n1"));
        ChiiEmu emu = new ChiiEmu(directConnection);

        assertEquals("WATCHED: 1 entries updated.", emu.updateWatchedStatus("42 3"));
        verify(directConnection).send("MYLISTADD", "edit=1&viewed=1&epno=3&aid=42", true);

        assertEquals("WATCHED: 1 entries updated.", emu.updateWatchedStatus("123"));
        verify(directConnection).send("MYLISTADD", "edit=1&viewed=1&fid=123", true);

        assertEquals(
                "WATCHED: 1 entries updated.",
                emu.updateWatchedStatus("ed2k://|file|episode.mkv|100|0123456789abcdef|"));
        // TODO: Review why ED2K-link watched updates send an empty viewed value instead of viewed=1.
        verify(directConnection).send("MYLISTADD", "edit=1&viewed=&size=100&ed2k=0123456789abcdef", true);
    }

    @Test
    void watchedUpdatesHandleAllNoneNamesAndRejectedResponses() throws Exception {
        AniDBConnection directConnection = mock(AniDBConnection.class);
        when(directConnection.send(eq("MYLISTADD"), anyString(), eq(true)))
                .thenReturn(response("311 MYLIST ENTRY EDITED\n1234"))
                .thenReturn(response("311 MYLIST ENTRY EDITED\n2"))
                .thenReturn(response("411 NO SUCH MYLIST ENTRY\n"));
        ChiiEmu emu = new ChiiEmu(directConnection);

        assertEquals("WATCHED: entry updated.", emu.updateWatchedStatus("Anime Name all"));
        verify(directConnection).send("MYLISTADD", "edit=1&viewed=1&epno=0&aname=Anime Name", true);
        assertEquals("WATCHED: 2 entries updated.", emu.updateWatchedStatus("42 NONE"));
        verify(directConnection).send("MYLISTADD", "edit=1&viewed=0&epno=0&aid=42", true);
        assertEquals("WATCHED: no such entry", emu.updateWatchedStatus("42 upto 3"));
        verify(directConnection).send("MYLISTADD", "edit=1&viewed=1&epno=-3&aid=42", true);
        assertTrue(emu.updateWatchedStatus("not enough").startsWith("WATCHED: usage:"));
    }

    @Test
    void stateUpdatesMapLocationsForFileAnimeNameAndEd2kTargets() throws Exception {
        AniDBConnection directConnection = mock(AniDBConnection.class);
        when(directConnection.send(eq("MYLISTADD"), anyString(), eq(true)))
                .thenReturn(response("311 MYLIST ENTRY EDITED\n1"));
        ChiiEmu emu = new ChiiEmu(directConnection);

        assertEquals("STATE: 1 entries updated.", emu.updateState("42 3 hdd"));
        verify(directConnection).send("MYLISTADD", "edit=1&state=1&epno=3&aid=42", true);
        assertEquals("STATE: 1 entries updated.", emu.updateState("Anime Name all deleted"));
        verify(directConnection).send("MYLISTADD", "edit=1&state=3&epno=0&aname=Anime Name", true);
        assertEquals("STATE: 1 entries updated.", emu.updateState("123 remote"));
        verify(directConnection).send("MYLISTADD", "edit=1&state=4&fid=123", true);
        assertEquals("STATE: 1 entries updated.", emu.updateState("ed2k://|file|episode.mkv|100|0123456789abcdef| cd"));
        verify(directConnection).send("MYLISTADD", "edit=1&state=2&size=100&ed2k=0123456789abcdef", true);
        assertTrue(emu.updateState("42 3 elsewhere").startsWith("STATE: usage:"));
    }

    @Test
    void workerBuildsStorageAndState2Requests() throws Exception {
        when(connection.send(eq("MYLISTADD"), anyString(), eq(true)))
                .thenReturn(response("311 MYLIST ENTRY EDITED\n1"))
                .thenReturn(response("311 MYLIST ENTRY EDITED\n3"));

        assertEquals("STORAGE: one entry updated", runCommand("!storage 12,all,all,Shelf A"));
        verify(connection).send("MYLISTADD", "edit=1&aid=12&epno=0&storage=Shelf A", true);

        assertEquals("STATE2: updated 3 entries", runCommand("!state2 Anime,Group,upto 4,remote"));
        verify(connection).send("MYLISTADD", "edit=1&aname=Anime&gname=Group&epno=-4&state=4", true);
        assertEquals("STATE2: no such state", runCommand("!state2 Anime,all,1,elsewhere"));
        assertTrue(runCommand("!storage Anime,all,invalid,Shelf").startsWith("!storage"));
    }

    @Test
    void secondCommandIsRejectedWhileAWorkerIsBlocked() throws Exception {
        CountDownLatch enteredSend = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        when(connection.send(eq("UPTIME"), isNull(), eq(true))).thenAnswer(invocation -> {
            enteredSend.countDown();
            if (!releaseSend.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to release fake AniDB response");
            }
            return response("208 UPTIME\n60000");
        });
        QueueLog log = new QueueLog();
        ChiiEmu emu = new ChiiEmu(connection);
        emu.setLog(log);

        try {
            emu.handleCommand("!uptime");
            assertTrue(enteredSend.await(2, TimeUnit.SECONDS));
            emu.handleCommand("!anime 1");
            assertEquals("Please Wait!", log.awaitMessage());
        } finally {
            releaseSend.countDown();
        }
        assertEquals("UPTIME: 0d 0h 1m", log.awaitMessage());
    }

    private String runCommand(String command) throws Exception {
        QueueLog log = new QueueLog();
        ChiiEmu emu = new ChiiEmu(connection);
        emu.setLog(log);
        emu.handleCommand(command);
        return log.awaitMessage();
    }

    private static AniDBConnectionResponse response(String raw) throws Exception {
        return new AniDBConnectionResponse(null, 0, raw);
    }

    private static final class QueueLog implements Log {
        private final BlockingQueue<String> messages = new ArrayBlockingQueue<>(4);

        @Override
        public void println(Object message) {
            messages.add(String.valueOf(message));
        }

        @Override
        public void status0(String message) {}

        @Override
        public void status1(String message) {}

        String awaitMessage() throws InterruptedException {
            String message = messages.poll(2, TimeUnit.SECONDS);
            assertNotNull(message, "Timed out waiting for ChiiEmu output");
            return message;
        }
    }
}
