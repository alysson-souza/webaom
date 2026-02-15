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

import org.junit.jupiter.api.Test;

class JobCounterTest {

    @Test
    void jobCounterProgress_excludesHaltedAndErrorFromDenominator() {
        JobCounter counter = new JobCounter();

        counter.register(-1, -1, Job.FINISHED, Job.H_NORMAL);
        counter.register(-1, -1, Job.FAILED, Job.H_NORMAL);
        counter.register(-1, -1, Job.HASHWAIT, Job.H_PAUSED);
        counter.register(-1, -1, Job.HASHWAIT, Job.H_NORMAL);

        assertEquals(500, counter.getProgress());
    }
}
