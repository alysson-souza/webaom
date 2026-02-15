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

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobManager;
import java.util.Set;

public final class DefaultJobActionGateway implements JobActionGateway {
    @Override
    public void updateStatus(Job job, int status) {
        JobManager.updateStatus(job, status);
    }

    @Override
    public void updateStatus(Job job, int status, boolean checkIfBusy) {
        JobManager.updateStatus(job, status, checkIfBusy);
    }

    @Override
    public boolean applyRulesForced(Job job) {
        return JobManager.applyRulesForced(job);
    }

    @Override
    public void setPath(Job job, String path, boolean includeParent) {
        JobManager.setPath(job, path, includeParent);
    }

    @Override
    public void restoreName(Job job) {
        JobManager.restoreName(job);
    }

    @Override
    public void showInfo(Job job) {
        JobManager.showInfo(job);
    }

    @Override
    public void openInDefaultPlayer(Job job) {
        JobManager.openInDefaultPlayer(job);
    }

    @Override
    public void openInExplorer(Job job) {
        JobManager.openInExplorer(job);
    }

    @Override
    public void setName(Job job, String newName) {
        JobManager.setName(job, newName);
    }

    @Override
    public int deleteJobs(Set<Job> jobs) {
        return JobManager.deleteJobs(jobs);
    }

    @Override
    public int jobCount() {
        if (AppContext.jobs == null) {
            return 0;
        }
        return AppContext.jobs.size();
    }

    @Override
    public void printLine(String message) {
        if (AppContext.gui != null) {
            AppContext.gui.println(message);
        }
    }

    @Override
    public void setStatusMessage(String message) {
        if (AppContext.gui != null) {
            AppContext.gui.setStatusMessage(message);
        }
    }

    @Override
    public void updateProgressBar() {
        if (AppContext.gui != null) {
            AppContext.gui.updateProgressBar();
        }
    }

    @Override
    public boolean confirm(String title, String message, String positive, String negative) {
        return AppContext.confirm(title, message, positive, negative);
    }
}
