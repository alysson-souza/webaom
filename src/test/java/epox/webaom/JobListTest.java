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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobListTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AppContext.jobCounter = new JobCounter();
    }

    @Test
    void jobListUpdateQueues_movesBetweenDiskAndNetworkOnlyOnStatusChange() throws IOException {
        JobList jobList = new JobList();
        Job job = new Job(Files.createFile(tempDir.resolve("episode.mkv")).toFile(), Job.HASHWAIT);

        jobList.updateQueues(job, -1, Job.HASHWAIT);
        assertTrue(jobList.getJobsDio(10, Job.HASHWAIT, Set.of()).contains(job));

        jobList.updateQueues(job, Job.HASHWAIT, Job.IDENTWAIT);
        assertFalse(jobList.getJobsDio(10, Job.HASHWAIT, Set.of()).contains(job));
        assertEquals(job, jobList.getJobNio());
    }
}
