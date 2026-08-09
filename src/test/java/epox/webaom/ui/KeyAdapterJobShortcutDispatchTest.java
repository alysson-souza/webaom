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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.ui.actions.jobs.JobActionController;
import epox.webaom.ui.actions.jobs.JobActionGateway;
import epox.webaom.ui.actions.jobs.JobDeleteScope;
import epox.webaom.ui.shortcuts.ShortcutCategory;
import epox.webaom.ui.shortcuts.ShortcutHandler;
import epox.webaom.ui.shortcuts.ShortcutInfo;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyAdapterJobShortcutDispatchTest {
    private JobCounter originalCounter;
    private AppContext.AppMode originalMode;
    private Set<ShortcutInfo> originalShortcuts;

    @BeforeEach
    void isolateState() {
        originalCounter = AppContext.jobCounter;
        originalMode = AppContext.getAppMode();
        originalShortcuts = Collections.newSetFromMap(new IdentityHashMap<>());
        originalShortcuts.addAll(AppContext.shortcutRegistry.getAllShortcuts());
        AppContext.jobCounter = new JobCounter();
        AppContext.setAppMode(AppContext.AppMode.NORMAL);
    }

    @AfterEach
    void restoreState() {
        for (ShortcutInfo shortcut : AppContext.shortcutRegistry.getAllShortcuts()) {
            if (!originalShortcuts.contains(shortcut)) {
                AppContext.shortcutRegistry.unregister(shortcut);
            }
        }
        AppContext.jobCounter = originalCounter;
        AppContext.setAppMode(originalMode);
    }

    @Test
    void typedJobShortcutDispatchesOnlyWithASelectedJobAndWithoutModifiers() {
        Job job = job("selected.mkv");
        JTable table = table(1);
        AtomicInteger dispatches = new AtomicInteger();
        ShortcutInfo shortcut = shortcut('Z', 0, ShortcutCategory.JOB, dispatches);
        AppContext.shortcutRegistry.register(shortcut);
        KeyAdapterJob adapter = new KeyAdapterJob(table, row -> new Job[] {job}, new TrackingController());

        KeyEvent withoutSelection = typed(table, 'Z', 0);
        adapter.keyTyped(withoutSelection);
        assertEquals(0, dispatches.get());
        assertFalse(withoutSelection.isConsumed());

        table.setRowSelectionInterval(0, 0);
        KeyEvent modified = typed(table, 'Z', InputEvent.CTRL_DOWN_MASK);
        adapter.keyTyped(modified);
        assertEquals(0, dispatches.get());
        assertFalse(modified.isConsumed());

        KeyEvent matching = typed(table, 'Z', 0);
        adapter.keyTyped(matching);
        assertEquals(1, dispatches.get());
        assertTrue(matching.isConsumed());
    }

    @Test
    void pressedNavigationShortcutDispatchesWithoutASelectedJob() {
        JTable table = table(1);
        AtomicInteger dispatches = new AtomicInteger();
        AppContext.shortcutRegistry.register(shortcut('\0', KeyEvent.VK_F12, ShortcutCategory.NAVIGATION, dispatches));
        KeyAdapterJob adapter = new KeyAdapterJob(table, row -> null, new TrackingController());
        KeyEvent event = pressed(table, KeyEvent.VK_F12);

        adapter.keyPressed(event);

        assertEquals(1, dispatches.get());
        assertTrue(event.isConsumed());
    }

    @Test
    void deletionCollectsUniqueJobsAndRestoresTheFirstSelection() {
        Job first = job("first.mkv");
        Job second = job("second.mkv");
        JTable table = table(3);
        table.addRowSelectionInterval(0, 1);
        TrackingController controller = new TrackingController();
        RowModel rows = row -> row == 0 ? new Job[] {first, second} : new Job[] {first};
        KeyAdapterJob adapter = new KeyAdapterJob(table, rows, controller);
        KeyEvent event = pressed(table, KeyEvent.VK_DELETE);

        adapter.keyPressed(event);

        assertEquals(Set.of(first, second), controller.deletedJobs);
        assertEquals(1, controller.deleteCalls);
        assertEquals(0, table.getSelectedRow());
        assertEquals(1, table.getSelectedRowCount());
        assertTrue(event.isConsumed());
    }

    @Test
    void unhandledDeletionAndBlockedInteractionLeaveEventsUntouched() {
        JTable table = table(1);
        TrackingController controller = new TrackingController();
        KeyAdapterJob adapter = new KeyAdapterJob(table, row -> null, controller);
        KeyEvent noSelection = pressed(table, KeyEvent.VK_BACK_SPACE);

        adapter.keyPressed(noSelection);
        assertEquals(0, controller.deleteCalls);
        assertFalse(noSelection.isConsumed());

        table.setRowSelectionInterval(0, 0);
        AppContext.setAppMode(AppContext.AppMode.INCOMPATIBLE_DATABASE);
        KeyEvent blocked = pressed(table, KeyEvent.VK_DELETE);
        adapter.keyPressed(blocked);
        assertEquals(0, controller.deleteCalls);
        assertFalse(blocked.isConsumed());
    }

    private static ShortcutInfo shortcut(
            char keyChar, int keyCode, ShortcutCategory category, AtomicInteger dispatches) {
        return new ShortcutInfo() {
            @Override
            public char keyChar() {
                return keyChar;
            }

            @Override
            public int keyCode() {
                return keyCode;
            }

            @Override
            public ShortcutCategory category() {
                return category;
            }

            @Override
            public String description() {
                return "Test shortcut";
            }

            @Override
            public ShortcutHandler handler() {
                return (event, source) -> {
                    dispatches.incrementAndGet();
                    return true;
                };
            }
        };
    }

    private static JTable table(int rows) {
        return new JTable(new DefaultTableModel(rows, 1));
    }

    private static Job job(String name) {
        return new Job(new File(name), Job.HASHWAIT);
    }

    private static KeyEvent typed(JTable table, char character, int modifiers) {
        return new KeyEvent(table, KeyEvent.KEY_TYPED, 1L, modifiers, KeyEvent.VK_UNDEFINED, character);
    }

    private static KeyEvent pressed(JTable table, int keyCode) {
        return new KeyEvent(table, KeyEvent.KEY_PRESSED, 1L, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static final class TrackingController extends JobActionController {
        int deleteCalls;
        Set<Job> deletedJobs = Set.of();

        TrackingController() {
            super(new NoOpGateway());
        }

        @Override
        public DeletionResult deleteSelectedJobs(Collection<Job> selectedJobs) {
            deleteCalls++;
            deletedJobs = new LinkedHashSet<>(selectedJobs);
            return new DeletionResult(true, deletedJobs.size());
        }
    }

    private static final class NoOpGateway implements JobActionGateway {
        @Override
        public void updateStatus(Job job, int status) {}

        @Override
        public void updateStatus(Job job, int status, boolean checkIfBusy) {}

        @Override
        public boolean applyRulesForced(Job job) {
            return false;
        }

        @Override
        public void setPath(Job job, String path, boolean includeParent) {}

        @Override
        public void restoreName(Job job) {}

        @Override
        public void showInfo(Job job) {}

        @Override
        public void openInDefaultPlayer(Job job) {}

        @Override
        public void openInExplorer(Job job) {}

        @Override
        public void setName(Job job, String newName) {}

        @Override
        public int deleteJobs(Set<Job> jobs, JobDeleteScope scope) {
            return 0;
        }

        @Override
        public int jobCount() {
            return 0;
        }

        @Override
        public void printLine(String message) {}

        @Override
        public void setStatusMessage(String message) {}

        @Override
        public void updateProgressBar() {}

        @Override
        public boolean confirm(String title, String message, String positive, String negative) {
            return false;
        }
    }
}
