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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JTable;

public class KeyAdapterJob extends KeyAdapter {
    private final JTable table;
    private final JobTreeTable treeTable;
    private final RowModel rowModel;

    public KeyAdapterJob(JTable table, RowModel rowModel) {
        this.table = table;
        this.rowModel = rowModel;
        if (table instanceof JobTreeTable jobTreeTable) {
            this.treeTable = jobTreeTable;
        } else {
            this.treeTable = null;
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        try {
            int keyCode = event.getKeyCode();
            if (handleGlobalShortcut(keyCode)) {
                return;
            }
            if (handleDeletionKey(keyCode, event)) {
                return;
            }
            Job selectedJob = getSelectedJob();
            if (selectedJob == null) {
                return;
            }
            if (handleJobAction(keyCode, selectedJob)) {
                event.consume();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean handleGlobalShortcut(int keyCode) {
        switch (keyCode) {
            case 'R':
                AppContext.gui.altViewPanel.updateAlternativeView(true);
                return true;
            case 'D':
                AppContext.animeTreeRoot.dump("@ ");
                return true;
            case 'B':
                AppContext.dumpStats();
                return true;
            case 'L':
                AppContext.animeTreeRoot.clear();
                AppContext.gui.altViewPanel.updateAlternativeView(false);
                return true;
            default:
                return false;
        }
    }

    private boolean handleDeletionKey(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
            if (deleteSelectedJobs()) {
                event.consume();
            }
            return true;
        }
        return false;
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

    private boolean handleJobAction(int keyCode, Job selectedJob) {
        // Handle actions that require anidbFile to be present
        if (selectedJob.anidbFile != null) {
            switch (keyCode) {
                case 'A':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlAnime());
                    return true;
                case 'M':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlMylist());
                    return true;
                case 'N':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlMylistE(selectedJob.mylistId));
                    return true;
                case 'E':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlEp());
                    return true;
                case 'G':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlGroup());
                    return true;
                case 'F':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlFile());
                    return true;
                case 'K':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlExport());
                    return true;
                case 'C':
                    JobManager.runAvdump(selectedJob);
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlFile());
                    return true;
                default:
                    break;
            }
        }

        // Handle actions that don't require anidbFile
        switch (keyCode) {
            case 'W':
                JobManager.openInDefaultPlayer(selectedJob);
                return true;
            case 'X':
                JobManager.openInExplorer(selectedJob);
                return true;
            case 'P':
                JobManager.updateStatus(selectedJob, Job.H_PAUSED, true);
                return true;
            case 'S':
                JobManager.updateStatus(selectedJob, Job.IDENTIFIED, true);
                return true;
            case 'I':
                selectedJob.anidbFile = null;
                JobManager.updateStatus(selectedJob, Job.HASHED, true);
                return true;
            case ' ', 10: // Enter or Space
                JobManager.showInfo(selectedJob);
                return true;
            case 39: // Right arrow key
                if (treeTable != null) {
                    treeTable.expandRow();
                    return true;
                }
                return false;
            case 37: // Left arrow key
                if (treeTable != null) {
                    treeTable.collapseRow();
                    return true;
                }
                return false;
            default:
                return false;
        }
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

        // Remember position for selection restoration
        int firstSelectedViewRow = selectedRows[0];

        int removedCount = JobManager.deleteJobs(selection.deletableJobs);
        if (removedCount > 0) {
            // Update status bar with removal message
            String statusMessage = removedCount == 1
                    ? "Removed 1 job. " + AppContext.jobs.size() + " remaining."
                    : "Removed " + removedCount + " jobs. " + AppContext.jobs.size() + " remaining.";
            AppContext.gui.setStatusMessage(statusMessage);

            // Update progress bar and title bar immediately
            AppContext.gui.updateProgressBar();

            // Restore selection to a sensible position
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
        // Select the row at the same position, or the last row if we deleted at the end
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
}
