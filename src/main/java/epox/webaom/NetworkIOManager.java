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

    @Override
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
                    if (isExpectedShutdownException(e)) {
                        cleanCurrentJob("Interrupted during shutdown");
                    } else {
                        e.printStackTrace();
                        String message = e.getMessage();
                        timedOut = message != null && message.contains("TIME OUT");
                        AppContext.gui.status1(message);
                        AppContext.gui.showMessage(" " + ((message == null) ? "Null pointer exception." : message));
                        if (!e.is(AniDBException.ENCRYPTION)) {
                            AppContext.gui.kill();
                            AppContext.gui.handleFatalError(true);
                        }
                        cleanCurrentJob(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (AniDBConnection.isShutdown()) {
                        cleanCurrentJob("Interrupted during shutdown");
                    } else {
                        AppContext.gui.println(" " + HyperlinkBuilder.formatAsError("Thread interrupted"));
                        cleanCurrentJob("Thread interrupted");
                    }
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
                AppContext.gui.status1("Sleeping...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                AppContext.gui.status1(THREAD_TERMINATED_MESSAGE);
            }
            try {
                if (!AniDBConnection.isShutdown() && ac != null && ac.isLoggedIn() && !timedOut && ac.logout()) {
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
        AppContext.gui.setNetworkIoOptionsEnabled(true);
        AppContext.gui.setNetworkIoEnabled(false);
        AppContext.gui.networkIoThread = null;
    }

    private boolean isExpectedShutdownException(AniDBException exception) {
        if (!AniDBConnection.isShutdown()) {
            return false;
        }

        if (exception.is(AniDBException.CLIENT_SYSTEM)) {
            return true;
        }

        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String lowercaseMessage = message.toLowerCase();
        return lowercaseMessage.contains("interrupted")
                || lowercaseMessage.contains("socket closed")
                || lowercaseMessage.contains("socket")
                || lowercaseMessage.contains("timeout");
    }

    private void cleanCurrentJob(String err) {
        if (currentJob != null) {
            currentJob.errorMessage = err;
            JobManager.updateStatus(currentJob, Job.FAILED);
            currentJob = null;
        }
    }

    private void doWork() throws AniDBException, InterruptedException {
        AppContext.gui.status1("Authenticating...");
        if (AppContext.conn.login()) {
            AppContext.gui.setNetworkIoEnabled(true);
            do {
                currentJob = AppContext.jobs.getJobNio();
                if (currentJob != null) {
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
                job.mylistId = job.anidbFile.getMylistEntryId();
                job.anidbFile.setJob(job);
                String fileName = HyperlinkBuilder.formatAsName(job.anidbFile.getDefaultName());
                String animeLink = HyperlinkBuilder.createHyperlink(job.anidbFile.getAnimeUrl(), "a");
                String epLink = HyperlinkBuilder.createHyperlink(job.anidbFile.getEpisodeUrl(), "e");
                String fileLink = HyperlinkBuilder.createHyperlink(job.anidbFile.getFileUrl(), "f");
                AppContext.gui.println("Found " + fileName + " " + animeLink + " " + epLink + " " + fileLink);
                JobManager.updateStatus(job, Job.IDENTIFIED);
            } else {
                JobManager.updateStatus(job, Job.UNKNOWN);
            }
        } else {
            if (job.anidbFile.getGroup() == null) {
                job.anidbFile.setGroup(
                        (Group) AppContext.cache.get(job.anidbFile.getGroupId(), DatabaseManager.INDEX_GROUP));
            }
            if (job.anidbFile.getEpisode() == null) {
                job.anidbFile.setEpisode(
                        (Episode) AppContext.cache.get(job.anidbFile.getEpisodeId(), DatabaseManager.INDEX_EPISODE));
            }
            if (job.anidbFile.getGroup() == null) {
                job.anidbFile.setAnime(
                        (Anime) AppContext.cache.get(job.anidbFile.getAnimeId(), DatabaseManager.INDEX_ANIME));
            }
            JobManager.updateStatus(job, Job.IDENTIFIED);
        }
    }

    private void mylistAdd(Job job) throws AniDBException {
        JobManager.updateStatus(job, Job.ADDING);
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
