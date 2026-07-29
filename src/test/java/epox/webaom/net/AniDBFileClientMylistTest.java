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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Mylist;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AniDBFileClientMylistTest {

    @TempDir
    Path temporaryDirectory;

    private JobCounter originalJobCounter;

    @BeforeEach
    void setUp() {
        originalJobCounter = AppContext.jobCounter;
        AppContext.jobCounter = new JobCounter();
    }

    @AfterEach
    void tearDown() {
        AppContext.jobCounter = originalJobCounter;
    }

    private static AniDBConnectionSettings settings() {
        return new AniDBConnectionSettings("localhost", 9000, 0, 20, 4000, 3, false);
    }

    private Job jobWithFile() throws Exception {
        File file = Files.createFile(temporaryDirectory.resolve("episode.mkv")).toFile();
        Job job = new Job(file, Job.HASHWAIT);
        AniDBFile anidbFile = new AniDBFile(4242);
        anidbFile.setFileId(4242);
        job.anidbFile = anidbFile;
        return job;
    }

    private static Mylist watchedEntry() {
        return entry(1);
    }

    /** Mirrors the desktop panel's defaults, where the "Watched" checkbox starts unchecked. */
    private static Mylist unwatchedEntry() {
        return entry(0);
    }

    private static Mylist entry(int viewed) {
        Mylist entry = new Mylist();
        entry.state = 1;
        entry.filestate = 0;
        entry.viewed = viewed;
        entry.source = "";
        entry.storage = "";
        entry.other = "";
        return entry;
    }

    private static AniDBConnectionResponse response(String raw) throws Exception {
        return new AniDBConnectionResponse(null, 0, raw);
    }

    @Test
    void reportsCreationWhenTheEntryIsNew() throws Exception {
        AniDBFileClient client = spy(new AniDBFileClient(null, settings()));
        doReturn(response("210 MYLIST ENTRY ADDED\n98765"))
                .when(client)
                .send(eq("MYLISTADD"), contains("edit=0"), eq(true));

        AniDBFileClient.MylistAddOutcome outcome = client.addOrEditMylistEntry(jobWithFile(), watchedEntry());

        assertEquals(98765, outcome.listEntryId());
        assertTrue(outcome.created());
        assertFalse(outcome.edited());
    }

    @Test
    void editsTheExistingEntryWhenAniDBReportsItAlreadyExists() throws Exception {
        // AniDB answers an edit=0 add for a known file with 310 and ignores the supplied values,
        // so the requested state has to be re-sent as an edit or it is silently dropped.
        AniDBFileClient client = spy(new AniDBFileClient(null, settings()));
        doReturn(response("310 FILE ALREADY IN MYLIST\n55555|4242|20|10|30|1700000000|1|0|0|||"))
                .when(client)
                .send(eq("MYLISTADD"), contains("edit=0"), eq(true));
        doReturn(response("311 MYLIST ENTRY EDITED\n"))
                .when(client)
                .send(eq("MYLISTADD"), contains("edit=1"), eq(true));

        AniDBFileClient.MylistAddOutcome outcome = client.addOrEditMylistEntry(jobWithFile(), watchedEntry());

        assertEquals(55555, outcome.listEntryId());
        assertFalse(outcome.created());
        assertTrue(outcome.edited());
        // Both the initial add and the follow-up edit must carry the requested watched flag.
        verify(client, times(2)).send(eq("MYLISTADD"), contains("viewed=1"), eq(true));
        verify(client).send(eq("MYLISTADD"), contains("edit=1"), eq(true));
    }

    @Test
    void reportsFailureWhenTheFollowUpEditIsRejected() throws Exception {
        AniDBFileClient client = spy(new AniDBFileClient(null, settings()));
        doReturn(response("310 FILE ALREADY IN MYLIST\n55555|4242|20|10|30|1700000000|1|0|0|||"))
                .when(client)
                .send(eq("MYLISTADD"), contains("edit=0"), eq(true));
        doReturn(response("411 NO SUCH MYLIST ENTRY\n"))
                .when(client)
                .send(eq("MYLISTADD"), contains("edit=1"), eq(true));

        AniDBFileClient.MylistAddOutcome outcome = client.addOrEditMylistEntry(jobWithFile(), watchedEntry());

        // The entry id is still known and worth keeping, but the requested state was not applied.
        assertEquals(55555, outcome.listEntryId());
        assertFalse(outcome.created());
        assertFalse(outcome.edited());
    }

    @Test
    void keepsTheLegacyIntegerApiWorkingForTheDesktopPipeline() throws Exception {
        AniDBFileClient client = spy(new AniDBFileClient(null, settings()));
        doReturn(response("210 MYLIST ENTRY ADDED\n4321"))
                .when(client)
                .send(eq("MYLISTADD"), contains("edit=0"), eq(true));

        assertEquals(4321, client.addFileToMylist(jobWithFile(), watchedEntry()));
    }

    @Test
    void desktopAddNeverRewritesAnEntryThatAlreadyExists() throws Exception {
        // A desktop bulk run supplies the MyList Options panel's current values, where "Watched" is
        // unchecked by default. Editing on 310 would mark already-watched episodes as unwatched and
        // blank their storage, so the desktop path must leave existing entries alone.
        AniDBFileClient client = spy(new AniDBFileClient(null, settings()));
        doReturn(response("310 FILE ALREADY IN MYLIST\n55555|4242|20|10|30|1700000000|1|0|0|||"))
                .when(client)
                .send(eq("MYLISTADD"), contains("edit=0"), eq(true));

        assertEquals(55555, client.addFileToMylist(jobWithFile(), unwatchedEntry()));

        verify(client, never()).send(eq("MYLISTADD"), contains("edit=1"), eq(true));
    }
}
