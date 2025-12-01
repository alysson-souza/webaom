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

import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.db.DatabaseManager;
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBException;
import epox.webaom.net.AniDBFileClient;

public class NetworkIOManager implements Runnable {
    private static final String THREAD_TERMINATED_MESSAGE = "NetIO thread terminated.";

    private Job currentJob;

    public void run() {
        AppContext.gui.status1("Checking connection...");
        boolean timedOut = false;
        AniDBFileClient ac = AppContext.gui.createConnection();
        if (ac.connect()) {
            if (ping(ac)) {
                AppContext.conn = ac;
                try {
                    doWork();
                    AppContext.gui.println(THREAD_TERMINATED_MESSAGE);
                    AppContext.gui.status1(THREAD_TERMINATED_MESSAGE);
                } catch (AniDBException e) {
                    e.printStackTrace();
                    String message = e.getMessage();
                    timedOut = message != null && message.contains("TIME OUT");
                    AppContext.gui.status1(message);
                    AppContext.gui.showMessage(" " + ((message == null) ? "Null pointer exception." : message));
                    if (!e.is(AniDBException.ENCRYPTION)) {
                        AppContext.gui.kill();
                        AppContext.gui.handleFatalError(true);
                    } // else A.userPass.apiKey = null;
                    cleanCurrentJob(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    AppContext.gui.println(" " + HyperlinkBuilder.formatAsError(e.getMessage()));
                    AppContext.gui.showMessage(" " + e.getMessage());
                    AppContext.gui.kill();
                    AppContext.gui.handleFatalError(true);
                    cleanCurrentJob(e.getMessage());
                }
                AppContext.conn.disconnect();
                AppContext.conn = null;
            } else {
                ac.disconnect();
                ac = null;
                // web.setEnabled_conn(true);
                AppContext.gui.status1("Sleeping...");
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    /* don't care */
                }
                AppContext.gui.status1(THREAD_TERMINATED_MESSAGE);
            }
            try {
                if (ac != null && ac.isLoggedIn() && !timedOut && ac.logout()) {
                    AppContext.gui.println("Logged out after extra check!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            String errorMsg = ac.getLastError();
            if (errorMsg.endsWith("Cannot bind")) {
                errorMsg = "The local port is already in use. Try another port.";
            }
            AppContext.gui.println(HyperlinkBuilder.formatAsError(errorMsg));
            AppContext.gui.showMessage(errorMsg);
        }
        // !A.nr_nio = -1;
        AppContext.gui.setNetworkIoOptionsEnabled(true);
        AppContext.gui.setNetworkIoEnabled(false);
        AppContext.gui.networkIoThread = null;
    }

    private void cleanCurrentJob(String err) {
        if (currentJob != null) {
            currentJob.errorMessage = err;
            JobManager.updateStatus(currentJob, Job.FAILED);
            currentJob = null;
        }
        // !A.nr_nio = -1;
    }

    private void doWork() throws AniDBException, InterruptedException {
        AppContext.gui.status1("Authenticating...");
        if (AppContext.conn.login()) {
            AppContext.gui.setNetworkIoEnabled(true);
            do {
                currentJob = AppContext.jobs.getJobNio();
                if (currentJob != null) {
                    // !A.nr_nio = currentJob.mIid;
                    if (currentJob.getStatus() == Job.REMWAIT) {
                        remove(currentJob);
                    } else {
                        if (currentJob.getStatus() == Job.IDENTWAIT) {
                            identify(currentJob);
                        }
                        if (AppContext.gui.isNetworkIoOk() && currentJob.getStatus() == Job.ADDWAIT) {
                            mylistAdd(currentJob);
                        }
                    }
                } else {
                    AppContext.gui.status1("Idle");
                    Thread.sleep(500);
                }
            } while (AppContext.gui.isNetworkIoOk());
            AppContext.gui.status1("Disconnecting...");
            AppContext.conn.logout();
        }
    }

    private void remove(Job job) throws AniDBException {
        JobManager.updateStatus(job, Job.REMING);
        // A.gui.updateJobTable(job);
        AppContext.gui.status1("Removing from mylist: " + job.getFile());
        if (job.mylistId > 0) {
            if (AppContext.conn.removeFromMylist(job.mylistId, job.getFile().getName())) {
                job.mylistId = 0;
                AppContext.gui.println("Removed " + HyperlinkBuilder.formatAsName(job.getFile()));
                JobManager.updateStatus(job, Job.FINISHED);
                return;
            }
            AppContext.gui.println(HyperlinkBuilder.formatAsError("Could not remove: " + job.getFile()));
        } else {
            AppContext.gui.println(HyperlinkBuilder.formatAsError("Not in mylist: " + job.getFile()));
        }
        job.setError("Was not in mylist");
        JobManager.updateStatus(job, Job.FAILED);
    }

    private void identify(Job job) throws AniDBException {
        JobManager.updateStatus(job, Job.IDENTIFYING);
        // A.gui.updateJobTable(job);
        AppContext.gui.status1("Retrieving file data for " + job.getFile().getName());
        if (job.anidbFile == null) {
            String[] fileData;
            if (job.fileIdOverride > 0) {
                fileData = AppContext.conn.retrieveFileData(
                        job.fileIdOverride, job.getFile().getName());
            } else {
                fileData = AppContext.conn.retrieveFileData(
                        job.fileSize, job.ed2kHash, job.getFile().getName());
            }
            if (fileData != null && AppContext.cache.parseFile(fileData, job) != null) {
                job.mylistId = job.anidbFile.mylistEntryId;
                job.anidbFile.setJob(job);
                String fileName = HyperlinkBuilder.formatAsName(job.anidbFile.defaultName);
                String animeLink = HyperlinkBuilder.createHyperlink(job.anidbFile.urlAnime(), "a");
                String epLink = HyperlinkBuilder.createHyperlink(job.anidbFile.urlEp(), "e");
                String fileLink = HyperlinkBuilder.createHyperlink(job.anidbFile.urlFile(), "f");
                AppContext.gui.println("Found " + fileName + " " + animeLink + " " + epLink + " " + fileLink);
                JobManager.updateStatus(job, Job.IDENTIFIED);
            } else {
                JobManager.updateStatus(job, Job.UNKNOWN);
            }
        } else {
            if (job.anidbFile.group == null) {
                job.anidbFile.group = (Group) AppContext.cache.get(job.anidbFile.groupId, DatabaseManager.INDEX_GROUP);
            }
            if (job.anidbFile.episode == null) {
                job.anidbFile.episode =
                        (Episode) AppContext.cache.get(job.anidbFile.episodeId, DatabaseManager.INDEX_EPISODE);
            }
            if (job.anidbFile.group == null) {
                job.anidbFile.anime = (Anime) AppContext.cache.get(job.anidbFile.animeId, DatabaseManager.INDEX_ANIME);
            }
            JobManager.updateStatus(job, Job.IDENTIFIED);
        }
    }

    private void mylistAdd(Job job) throws AniDBException {
        JobManager.updateStatus(job, Job.ADDING);
        // A.gui.updateJobTable(job);
        AppContext.gui.status1("Adding " + job.getFile() + " to your list...");
        int listId = AppContext.conn.addFileToMylist(job, AppContext.gui.mylistOptionsPanel.getMylistData());
        if (listId > 0) {
            job.mylistId = listId;
            AppContext.gui.println("Added " + HyperlinkBuilder.formatAsName(job.getFile()) + " to mylist");
        }
        JobManager.updateStatus(job, Job.ADDED);
    }

    public boolean ping(AniDBConnection ac) {
        try {
            long encryptTime = ac.encrypt();
            String replyTime = HyperlinkBuilder.formatAsNumber(String.valueOf(encryptTime));
            AppContext.gui.println("AniDB is reachable. Received reply in " + replyTime + " ms.");
            return true;
        } catch (java.net.SocketTimeoutException e) {
            String errorMessage = "AniDB is not reachable.";
            AppContext.gui.println(HyperlinkBuilder.formatAsError(errorMessage));
            AppContext.gui.status1(errorMessage);
            AppContext.gui.showMessage(errorMessage);
        } catch (NumberFormatException e) {
            AppContext.gui.showMessage("Invalid number. " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            AppContext.gui.println(HyperlinkBuilder.formatAsError(e.getMessage()));
            AppContext.gui.showMessage(e.getMessage());
        }
        AppContext.gui.println("Check out the connection options or try again later.");
        return false;
    }
}
