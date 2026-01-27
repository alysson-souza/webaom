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

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobManager;
import epox.webaom.ui.shortcuts.ShortcutCategory;
import epox.webaom.ui.shortcuts.ShortcutHandler;
import epox.webaom.ui.shortcuts.ShortcutInfo;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JTable;

public class KeyAdapterJob extends KeyAdapter {
    private final JTable table;
    private final RowModel rowModel;
    private final JobTreeTable treeTable;

    public KeyAdapterJob(JTable table, RowModel rowModel) {
        this.table = table;
        this.rowModel = rowModel;
        this.treeTable = table instanceof JobTreeTable jobTreeTable ? jobTreeTable : null;
        registerShortcuts();
    }

    private void registerShortcuts() {
        AppContext.shortcutRegistry.register(new JobInfoShortcut());
        AppContext.shortcutRegistry.register(new OpenAnimeShortcut());
        AppContext.shortcutRegistry.register(new OpenMylistShortcut());
        AppContext.shortcutRegistry.register(new OpenPlayerShortcut());
        AppContext.shortcutRegistry.register(new OpenExplorerShortcut());
        AppContext.shortcutRegistry.register(new PauseJobShortcut());
        AppContext.shortcutRegistry.register(new UpdateStatusShortcut());
        AppContext.shortcutRegistry.register(new ResetJobShortcut());
        if (treeTable != null) {
            AppContext.shortcutRegistry.register(new ExpandRowShortcut());
            AppContext.shortcutRegistry.register(new CollapseRowShortcut());
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        try {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
                Job job = getSelectedJob();
                if (job != null) {
                    JobManager.showInfo(job);
                    event.consume();
                }
                return;
            }

            if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
                handleDeletionKey(event);
                return;
            }

            for (ShortcutInfo shortcut : getJobShortcuts()) {
                if (shortcut.keyCode() != 0 && shortcut.keyCode() == keyCode) {
                    if (handleJobShortcut(shortcut, event)) {
                        event.consume();
                    }
                    return;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
        try {
            char keyChar = event.getKeyChar();
            if (!Character.isISOControl(keyChar) && !hasShortcutModifiers(event)) {
                for (ShortcutInfo shortcut : getJobShortcuts()) {
                    if (shortcut.keyChar() == keyChar) {
                        if (handleJobShortcut(shortcut, event)) {
                            event.consume();
                        }
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Set<ShortcutInfo> getJobShortcuts() {
        Set<ShortcutInfo> result = new LinkedHashSet<>();
        for (ShortcutInfo s : AppContext.shortcutRegistry.getAllShortcuts()) {
            if (s.category() == ShortcutCategory.JOB || s.category() == ShortcutCategory.NAVIGATION) {
                result.add(s);
            }
        }
        return result;
    }

    private boolean handleJobShortcut(ShortcutInfo shortcut, KeyEvent event) {
        if (shortcut.category() == ShortcutCategory.JOB && getSelectedJob() == null) {
            return false;
        }
        return shortcut.handler().handle(event, table);
    }

    private void handleDeletionKey(KeyEvent event) {
        if (deleteSelectedJobs()) {
            event.consume();
        }
    }

    private Job getSelectedJob() {
        int selectedRow = toModelRow(table.getSelectedRow());
        if (selectedRow < 0) {
            return null;
        }
        Job[] jobs = rowModel.getJobs(selectedRow);
        if (jobs == null || jobs.length < 1) {
            return null;
        }
        return jobs[0];
    }

    private boolean deleteSelectedJobs() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            return false;
        }

        DeletionSelection selection = collectSelection(selectedRows);
        if (selection.isEmpty()) {
            if (selection.hasRunningJobs()) {
                AppContext.gui.println("Cannot delete running job(s); stop them first.");
            }
            return selection.hasRunningJobs();
        }

        if (!confirmDeletion(selection.deletableJobs)) {
            return true;
        }

        int firstSelectedViewRow = selectedRows[0];
        int removedCount = JobManager.deleteJobs(selection.deletableJobs);
        if (removedCount > 0) {
            String statusMessage = removedCount == 1
                    ? "Removed 1 job. " + AppContext.jobs.size() + " remaining."
                    : "Removed " + removedCount + " jobs. " + AppContext.jobs.size() + " remaining.";
            AppContext.gui.setStatusMessage(statusMessage);
            AppContext.gui.updateProgressBar();
            restoreSelectionAfterDeletion(firstSelectedViewRow);
        }

        if (selection.hasRunningJobs()) {
            AppContext.gui.println("Skipped " + selection.runningCount + " running job(s) during deletion.");
        }

        table.requestFocusInWindow();
        return true;
    }

    private void restoreSelectionAfterDeletion(int previousViewRow) {
        int rowCount = table.getRowCount();
        if (rowCount == 0) {
            table.clearSelection();
            return;
        }
        int newSelection = Math.min(previousViewRow, rowCount - 1);
        table.setRowSelectionInterval(newSelection, newSelection);
    }

    private boolean confirmDeletion(Set<Job> jobsToDelete) {
        String message;
        if (jobsToDelete.size() == 1) {
            Job job = jobsToDelete.iterator().next();
            message = "Remove \"" + job.getFile().getName() + "\" from the job list?";
        } else {
            message = "Remove " + jobsToDelete.size() + " selected jobs from the job list?";
        }
        return AppContext.confirm("Remove jobs", message, "Remove", "Cancel");
    }

    private int toModelRow(int viewRow) {
        if (viewRow < 0) {
            return viewRow;
        }
        try {
            return table.convertRowIndexToModel(viewRow);
        } catch (Exception ex) {
            return viewRow;
        }
    }

    private DeletionSelection collectSelection(int[] selectedRows) {
        Set<Job> deletableJobs = new LinkedHashSet<>();
        int runningCount = 0;
        for (int viewRow : selectedRows) {
            Job[] jobs = rowModel.getJobs(toModelRow(viewRow));
            if (jobs != null) {
                for (Job job : jobs) {
                    if (job == null) {
                        continue;
                    }
                    if (job.check(Job.S_DOING)) {
                        runningCount++;
                    } else {
                        deletableJobs.add(job);
                    }
                }
            }
        }
        return new DeletionSelection(deletableJobs, runningCount);
    }

    private record DeletionSelection(Set<Job> deletableJobs, int runningCount) {
        boolean isEmpty() {
            return deletableJobs.isEmpty();
        }

        boolean hasRunningJobs() {
            return runningCount > 0;
        }
    }

    private static boolean hasShortcutModifiers(KeyEvent event) {
        return event.isControlDown() || event.isMetaDown() || event.isAltDown();
    }

    private Job getSelectedJobForHandler() {
        return getSelectedJob();
    }

    private abstract class JobBasedShortcut implements ShortcutInfo {
        @Override
        public ShortcutCategory category() {
            return ShortcutCategory.JOB;
        }

        protected Job getJob() {
            return getSelectedJobForHandler();
        }

        @Override
        public char keyChar() {
            return '\0';
        }

        @Override
        public int keyCode() {
            return 0;
        }
    }

    private class JobInfoShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null) {
                    JobManager.showInfo(job);
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return '\0';
        }

        @Override
        public int keyCode() {
            return KeyEvent.VK_ENTER;
        }

        @Override
        public String description() {
            return "Show job info";
        }
    }

    private class OpenAnimeShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null && job.anidbFile != null) {
                    AppContext.gui.openHyperlink(job.anidbFile.getAnimeUrl());
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return 'A';
        }

        @Override
        public String description() {
            return "Open anime URL in browser";
        }
    }

    private class OpenMylistShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null && job.anidbFile != null) {
                    AppContext.gui.openHyperlink(job.anidbFile.getMylistUrl());
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return 'M';
        }

        @Override
        public String description() {
            return "Open MyList URL in browser";
        }
    }

    private class OpenPlayerShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null) {
                    JobManager.openInDefaultPlayer(job);
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return 'W';
        }

        @Override
        public String description() {
            return "Open file in default player";
        }
    }

    private class OpenExplorerShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null) {
                    JobManager.openInExplorer(job);
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return 'X';
        }

        @Override
        public String description() {
            return "Open containing folder";
        }
    }

    private class PauseJobShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null) {
                    JobManager.updateStatus(job, Job.H_PAUSED, true);
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return 'P';
        }

        @Override
        public String description() {
            return "Pause/unpause job";
        }
    }

    private class UpdateStatusShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null) {
                    JobManager.updateStatus(job, Job.IDENTIFIED, true);
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return 'S';
        }

        @Override
        public String description() {
            return "Apply rules / update status";
        }
    }

    private class ResetJobShortcut extends JobBasedShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                Job job = getJob();
                if (job != null) {
                    job.anidbFile = null;
                    JobManager.updateStatus(job, Job.HASHED, true);
                    return true;
                }
                return false;
            };
        }

        @Override
        public char keyChar() {
            return 'I';
        }

        @Override
        public String description() {
            return "Re-identify file";
        }
    }

    private abstract class NavigationShortcut implements ShortcutInfo {
        @Override
        public ShortcutCategory category() {
            return ShortcutCategory.NAVIGATION;
        }

        @Override
        public char keyChar() {
            return '\0';
        }
    }

    private class ExpandRowShortcut extends NavigationShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                if (treeTable != null) {
                    treeTable.expandRow();
                    return true;
                }
                return false;
            };
        }

        @Override
        public int keyCode() {
            return KeyEvent.VK_RIGHT;
        }

        @Override
        public String description() {
            return "Expand row (tree view)";
        }
    }

    private class CollapseRowShortcut extends NavigationShortcut {
        @Override
        public ShortcutHandler handler() {
            return (event, source) -> {
                if (treeTable != null) {
                    treeTable.collapseRow();
                    return true;
                }
                return false;
            };
        }

        @Override
        public int keyCode() {
            return KeyEvent.VK_LEFT;
        }

        @Override
        public String description() {
            return "Collapse row (tree view)";
        }
    }
}
