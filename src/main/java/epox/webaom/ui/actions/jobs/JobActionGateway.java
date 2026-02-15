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

import epox.webaom.Job;
import java.util.Set;

public interface JobActionGateway {
    void updateStatus(Job job, int status);

    void updateStatus(Job job, int status, boolean checkIfBusy);

    boolean applyRulesForced(Job job);

    void setPath(Job job, String path, boolean includeParent);

    void restoreName(Job job);

    void showInfo(Job job);

    void openInDefaultPlayer(Job job);

    void openInExplorer(Job job);

    void setName(Job job, String newName);

    int deleteJobs(Set<Job> jobs);

    int jobCount();

    void printLine(String message);

    void setStatusMessage(String message);

    void updateProgressBar();

    boolean confirm(String title, String message, String positive, String negative);
}
