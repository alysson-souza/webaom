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

import org.junit.jupiter.api.Test;

class JobCounterCharacterizationTest {
    @Test
    void statusTextReportsEveryCategoryAndTotal() {
        JobCounter counter = new JobCounter();
        counter.register(-1, -1, Job.FINISHED, Job.H_NORMAL);
        counter.register(-1, -1, Job.HASHWAIT, Job.H_NORMAL);
        counter.register(-1, -1, Job.IDENTWAIT, Job.H_NORMAL);
        counter.register(-1, -1, Job.FAILED, Job.H_NORMAL);
        counter.register(-1, -1, Job.HASHWAIT, Job.H_PAUSED);

        assertEquals("fin=1 dio=1 nio=1 err=1 hlt=1  tot=5", counter.getStatus());
        assertEquals(333, counter.getProgress());
    }

    @Test
    void transitionsMoveCountsWithoutChangingTotal() {
        JobCounter counter = new JobCounter();
        counter.register(-1, -1, Job.HASHWAIT, Job.H_NORMAL);

        counter.register(Job.HASHWAIT, Job.H_NORMAL, Job.IDENTWAIT, Job.H_NORMAL);
        assertEquals("fin=0 dio=0 nio=1 err=0 hlt=0  tot=1", counter.getStatus());

        counter.register(Job.IDENTWAIT, Job.H_NORMAL, Job.FINISHED, Job.H_NORMAL);
        assertEquals(1000, counter.getProgress());
        assertEquals("fin=1 dio=0 nio=0 err=0 hlt=0  tot=1", counter.getStatus());
    }

    @Test
    void unknownStatusIsCurrentlyClassifiedAsFinished() {
        JobCounter counter = new JobCounter();

        counter.register(-1, -1, Job.UNKNOWN, Job.H_NORMAL);

        // TODO: Review category ordering. UNKNOWN contains the FINISHED bits and reaches that branch first.
        assertEquals("fin=1 dio=0 nio=0 err=0 hlt=0  tot=1", counter.getStatus());
        assertEquals(1000, counter.getProgress());
    }

    @Test
    void unregisterAndResetRemoveAllObservableState() {
        JobCounter counter = new JobCounter();
        counter.register(-1, -1, Job.UNKNOWN, Job.H_NORMAL);
        counter.unregister(Job.UNKNOWN, Job.H_NORMAL);

        assertEquals("fin=0 dio=0 nio=0 err=0 hlt=0  tot=0", counter.getStatus());
        assertEquals(0, counter.getProgress());

        counter.register(-1, -1, Job.FINISHED, Job.H_NORMAL);
        counter.reset();
        assertEquals("fin=0 dio=0 nio=0 err=0 hlt=0  tot=0", counter.getStatus());
    }
}
