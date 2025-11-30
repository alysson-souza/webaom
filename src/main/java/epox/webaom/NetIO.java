// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 29.05.05
 *
 * @version 	06
 * @author 		epoximator
 */
package epox.webaom;

import epox.webaom.data.Anime;
import epox.webaom.data.Ep;
import epox.webaom.data.Group;
import epox.webaom.net.ACon;
import epox.webaom.net.AConE;
import epox.webaom.net.AConEx;

public class NetIO implements Runnable {
	private static final String THREAD_TERMINATED_MESSAGE = "NetIO thread terminated.";

	private Job currentJob;

	public void run() {
		A.gui.status1("Checking connection...");
		boolean timedOut = false;
		AConE ac = A.gui.createConnection();
		if (ac.connect()) {
			if (ping(ac)) {
				A.conn = ac;
				try {
					doWork();
					A.gui.println(THREAD_TERMINATED_MESSAGE);
					A.gui.status1(THREAD_TERMINATED_MESSAGE);
				} catch (AConEx e) {
					e.printStackTrace();
					String message = e.getMessage();
					timedOut = message != null && message.indexOf("TIME OUT") >= 0;
					A.gui.status1(message);
					A.gui.showMessage(" " + ((message == null) ? "Null pointer exception." : message));
					if (!e.is(AConEx.ENCRYPTION)) {
						A.gui.kill();
						A.gui.handleFatalError(true);
					} // else A.up.key = null;
					cleanCurrentJob(e.getMessage());
				} catch (Exception e) {
					e.printStackTrace();
					A.gui.println(" " + Hyper.formatAsError(e.getMessage()));
					A.gui.showMessage(" " + e.getMessage());
					A.gui.kill();
					A.gui.handleFatalError(true);
					cleanCurrentJob(e.getMessage());
				}
				A.conn.disconnect();
				A.conn = null;
			} else {
				ac.disconnect();
				ac = null;
				// web.setEnabled_conn(true);
				A.gui.status1("Sleeping...");
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					/* don't care */
				}
				A.gui.status1(THREAD_TERMINATED_MESSAGE);
			}
			try {
				if (ac != null && ac.isLoggedIn() && !timedOut && ac.logout()) {
					A.gui.println("Logged out after extra check!");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			String errorMsg = ac.getLastError();
			if (errorMsg.endsWith("Cannot bind")) {
				errorMsg = "The local port is already in use. Try another port.";
			}
			A.gui.println(Hyper.formatAsError(errorMsg));
			A.gui.showMessage(errorMsg);
		}
		// !A.nr_nio = -1;
		A.gui.setNetworkIoOptionsEnabled(true);
		A.gui.setNetworkIoEnabled(false);
		A.gui.networkIoThread = null;
	}

	private void cleanCurrentJob(String err) {
		if (currentJob != null) {
			currentJob.errorMessage = err;
			JobMan.updateStatus(currentJob, Job.FAILED);
			currentJob = null;
		}
		// !A.nr_nio = -1;
	}

	private void doWork() throws AConEx, InterruptedException {
		A.gui.status1("Authenticating...");
		if (A.conn.login()) {
			A.gui.setNetworkIoEnabled(true);
			do {
				currentJob = A.jobs.getJobNio();
				if (currentJob != null) {
					// !A.nr_nio = currentJob.mIid;
					if (currentJob.getStatus() == Job.REMWAIT) {
						remove(currentJob);
					} else {
						if (currentJob.getStatus() == Job.IDENTWAIT) {
							identify(currentJob);
						}
						if (A.gui.isNetworkIoOk() && currentJob.getStatus() == Job.ADDWAIT) {
							mylistAdd(currentJob);
						}
					}
				} else {
					A.gui.status1("Idle");
					Thread.sleep(500);
				}
			} while (A.gui.isNetworkIoOk());
			A.gui.status1("Disconnecting...");
			A.conn.logout();
		}
	}

	private void remove(Job job) throws AConEx {
		JobMan.updateStatus(job, Job.REMING);
		// A.gui.updateJobTable(job);
		A.gui.status1("Removing from mylist: " + job.getFile());
		if (job.mylistId > 0) {
			if (A.conn.removeFromMylist(job.mylistId, job.getFile().getName())) {
				job.mylistId = 0;
				A.gui.println("Removed " + Hyper.formatAsName(job.getFile()));
				JobMan.updateStatus(job, Job.FINISHED);
				return;
			}
			A.gui.println(Hyper.formatAsError("Could not remove: " + job.getFile()));
		} else {
			A.gui.println(Hyper.formatAsError("Not in mylist: " + job.getFile()));
		}
		job.setError("Was not in mylist");
		JobMan.updateStatus(job, Job.FAILED);
	}

	private void identify(Job job) throws AConEx {
		JobMan.updateStatus(job, Job.IDENTIFYING);
		// A.gui.updateJobTable(job);
		A.gui.status1("Retrieving file data for " + job.getFile().getName());
		if (job.anidbFile == null) {
			String[] fileData = null;
			if (job.fileIdOverride > 0) {
				fileData = A.conn.retrieveFileData(job.fileIdOverride, job.getFile().getName());
			} else {
				fileData = A.conn.retrieveFileData(job.fileSize, job.ed2kHash, job.getFile().getName());
			}
			if (fileData != null && A.cache.parseFile(fileData, job) != null) {
				job.mylistId = job.anidbFile.mylistEntryId;
				job.anidbFile.setJob(job);
				String fileName = Hyper.formatAsName(job.anidbFile.defaultName);
				String animeLink = Hyper.createHyperlink(job.anidbFile.urlAnime(), "a");
				String epLink = Hyper.createHyperlink(job.anidbFile.urlEp(), "e");
				String fileLink = Hyper.createHyperlink(job.anidbFile.urlFile(), "f");
				A.gui.println("Found " + fileName + " " + animeLink + " " + epLink + " " + fileLink);
				JobMan.updateStatus(job, Job.IDENTIFIED);
			} else {
				JobMan.updateStatus(job, Job.UNKNOWN);
			}
		} else {
			if (job.anidbFile.group == null) {
				job.anidbFile.group = (Group) A.cache.get(job.anidbFile.groupId, DB.INDEX_GROUP);
			}
			if (job.anidbFile.ep == null) {
				job.anidbFile.ep = (Ep) A.cache.get(job.anidbFile.episodeId, DB.INDEX_EPISODE);
			}
			if (job.anidbFile.group == null) {
				job.anidbFile.anime = (Anime) A.cache.get(job.anidbFile.animeId, DB.INDEX_ANIME);
			}
			JobMan.updateStatus(job, Job.IDENTIFIED);
		}
	}

	private void mylistAdd(Job job) throws AConEx {
		JobMan.updateStatus(job, Job.ADDING);
		// A.gui.updateJobTable(job);
		A.gui.status1("Adding " + job.getFile() + " to your list...");
		int listId = A.conn.addFileToMylist(job, A.gui.mylistOptionsPanel.getMylistData());
		if (listId > 0) {
			job.mylistId = listId;
			A.gui.println("Added " + Hyper.formatAsName(job.getFile()) + " to mylist");
		}
		JobMan.updateStatus(job, Job.ADDED);
	}

	public boolean ping(ACon ac) {
		try {
			long encryptTime = ac.encrypt();
			String replyTime = Hyper.formatAsNumber(String.valueOf(encryptTime));
			A.gui.println("AniDB is reachable. Received reply in " + replyTime + " ms.");
			return true;
		} catch (java.net.SocketTimeoutException e) {
			String errorMessage = "AniDB is not reachable.";
			A.gui.println(Hyper.formatAsError(errorMessage));
			A.gui.status1(errorMessage);
			A.gui.showMessage(errorMessage);
		} catch (NumberFormatException e) {
			A.gui.showMessage("Invalid number. " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			A.gui.println(Hyper.formatAsError(e.getMessage()));
			A.gui.showMessage(e.getMessage());
		}
		A.gui.println("Check out the connection options or try again later.");
		return false;
	}
}
