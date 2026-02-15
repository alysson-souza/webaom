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

package epox.webaom.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class DefaultMainPanelRuntimeTest {
    @Test
    void defaultMainPanelRuntime_createTimer_setsDelay() {
        DefaultMainPanelRuntime runtime = new DefaultMainPanelRuntime();

        javax.swing.Timer timer = runtime.createTimer(1234, event -> {});

        assertEquals(1234, timer.getDelay());
    }

    @Test
    void defaultMainPanelRuntime_startBackgroundTask_runsRunnable() throws InterruptedException {
        DefaultMainPanelRuntime runtime = new DefaultMainPanelRuntime();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        Thread thread = runtime.startBackgroundTask("worker", () -> {
            executed.set(true);
            latch.countDown();
        });

        assertNotNull(thread);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(executed.get());
    }
}
