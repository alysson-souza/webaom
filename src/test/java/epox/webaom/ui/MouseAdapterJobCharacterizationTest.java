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

package epox.webaom.ui;

import static org.mockito.Mockito.mockStatic;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.JobManager;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class MouseAdapterJobCharacterizationTest {
    private JobCounter originalCounter;

    @BeforeEach
    void installCounter() {
        originalCounter = AppContext.jobCounter;
        AppContext.jobCounter = new JobCounter();
    }

    @AfterEach
    void restoreCounter() {
        AppContext.jobCounter = originalCounter;
    }

    @Test
    void doubleClickDispatchesInfoOrPlayerWhileOtherClicksDoNothing() {
        Job job = new Job(new File("missing.mkv"), Job.HASHWAIT);
        JobList jobs = new JobList();
        jobs.add(job);
        TableModelJobs model = new TableModelJobs(jobs);
        JTable table = new JTable(model);
        MouseAdapterJob adapter = new MouseAdapterJob(table, model, jobs);
        try (MockedStatic<JobManager> manager = mockStatic(JobManager.class)) {
            adapter.mouseClicked(click(table, 1, 0));
            adapter.mouseClicked(click(table, 2, 0));
            manager.verifyNoInteractions();

            table.setRowSelectionInterval(0, 0);
            adapter.mouseClicked(click(table, 2, 0));
            manager.verify(() -> JobManager.showInfo(job));

            adapter.mouseClicked(click(table, 2, InputEvent.ALT_DOWN_MASK));
            manager.verify(() -> JobManager.openInDefaultPlayer(job));
        }
    }

    private static MouseEvent click(JTable table, int count, int modifiers) {
        return new MouseEvent(table, MouseEvent.MOUSE_CLICKED, 1L, modifiers, 1, 1, count, false);
    }
}
