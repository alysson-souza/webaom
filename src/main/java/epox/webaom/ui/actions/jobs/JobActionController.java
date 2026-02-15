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

package epox.webaom.ui.actions.jobs;

import epox.webaom.HyperlinkBuilder;
import epox.webaom.Job;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class JobActionController {
    private final JobActionGateway gateway;

    public JobActionController(JobActionGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway);
    }

    public static JobActionController createDefault() {
        return new JobActionController(new DefaultJobActionGateway());
    }

    public void executeCommand(JobActionCommand command, Job job, String folderPath) {
        if (command == null || job == null) {
            return;
        }
        switch (command) {
            case PAUSE:
                gateway.updateStatus(job, Job.H_PAUSED, true);
                break;
            case REHASH:
                gateway.updateStatus(job, Job.HASHWAIT, true);
                break;
            case REID:
                job.anidbFile = null;
                gateway.updateStatus(job, Job.HASHED, true);
                break;
            case READD:
                gateway.updateStatus(job, Job.ADDWAIT, true);
                break;
            case REMOVE:
                gateway.updateStatus(job, Job.REMWAIT, true);
                break;
            case APPLY_RULES:
                if (!gateway.applyRulesForced(job)) {
                    gateway.printLine(HyperlinkBuilder.formatAsError("Failed to apply rules for " + job));
                }
                break;
            case SET_FINISHED:
                gateway.updateStatus(job, Job.FINISHED, true);
                break;
            case SET_FOLDER:
            case EDIT_PATH:
                if (folderPath != null) {
                    gateway.setPath(job, folderPath, false);
                }
                break;
            case SET_PAR_FLD:
                if (folderPath != null) {
                    gateway.setPath(job, folderPath, true);
                }
                break;
            case REMOVE_DB:
                gateway.updateStatus(job, Job.H_DELETED, true);
                break;
            case RESTORE_NAME:
                gateway.restoreName(job);
                break;
            case PARSE:
                gateway.updateStatus(job, Job.PARSEWAIT, true);
                break;
            default:
                break;
        }
    }

    public void executeSingleCommand(JobActionCommand command, Job job, String editedName, String fidInput) {
        if (command == null || job == null) {
            return;
        }
        switch (command) {
            case SHOW_INFO:
                gateway.showInfo(job);
                break;
            case WATCH_NOW:
                gateway.openInDefaultPlayer(job);
                break;
            case EXPLORER:
                gateway.openInExplorer(job);
                break;
            case EDIT_NAME:
                gateway.setName(job, editedName);
                break;
            case SET_FID:
                job.fileIdOverride = parseFid(fidInput);
                if (job.fileIdOverride > 0) {
                    job.anidbFile = null;
                    gateway.updateStatus(job, Job.HASHED);
                }
                break;
            default:
                break;
        }
    }

    public DeletionResult deleteSelectedJobs(Collection<Job> selectedJobs) {
        if (selectedJobs == null || selectedJobs.isEmpty()) {
            return DeletionResult.notHandled();
        }
        Set<Job> deletableJobs = new LinkedHashSet<>();
        Set<Job> runningJobs = new LinkedHashSet<>();
        for (Job job : selectedJobs) {
            if (job == null) {
                continue;
            }
            if (job.check(Job.S_DOING)) {
                runningJobs.add(job);
            } else {
                deletableJobs.add(job);
            }
        }
        if (deletableJobs.isEmpty()) {
            if (!runningJobs.isEmpty()) {
                gateway.printLine("Cannot delete running job(s); stop them first.");
                return DeletionResult.handled(0);
            }
            return DeletionResult.notHandled();
        }

        String deletionMessage = createDeletionConfirmation(deletableJobs);
        if (!gateway.confirm("Remove jobs", deletionMessage, "Remove", "Cancel")) {
            return DeletionResult.handled(0);
        }

        int removedCount = gateway.deleteJobs(deletableJobs);
        if (removedCount > 0) {
            gateway.setStatusMessage(createRemovalStatus(removedCount, gateway.jobCount()));
            gateway.updateProgressBar();
        }
        if (!runningJobs.isEmpty()) {
            gateway.printLine("Skipped " + runningJobs.size() + " running job(s) during deletion.");
        }
        return DeletionResult.handled(removedCount);
    }

    private static String createDeletionConfirmation(Set<Job> deletableJobs) {
        if (deletableJobs.size() == 1) {
            Job job = deletableJobs.iterator().next();
            return "Remove \"" + job.getFile().getName() + "\" from the job list?";
        }
        return "Remove " + deletableJobs.size() + " selected jobs from the job list?";
    }

    private static String createRemovalStatus(int removedCount, int remainingCount) {
        if (removedCount == 1) {
            return "Removed 1 job. " + remainingCount + " remaining.";
        }
        return "Removed " + removedCount + " jobs. " + remainingCount + " remaining.";
    }

    private static int parseFid(String fidInput) {
        if (fidInput == null) {
            return -1;
        }
        try {
            return Integer.parseInt(fidInput.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public record DeletionResult(boolean handled, int removedCount) {
        static DeletionResult notHandled() {
            return new DeletionResult(false, 0);
        }

        static DeletionResult handled(int removedCount) {
            return new DeletionResult(true, removedCount);
        }
    }
}
