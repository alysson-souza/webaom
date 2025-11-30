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
 * Created on 02.08.05
 *
 * @version 	03 (1.14,1.10,1.09)
 * @author 		epoximator
 */
package epox.webaom;

import epox.util.LinkedHash;
import epox.webaom.ui.TableModelJobs;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class JobList {
	private final ArrayList<Job> jobsList;
	private Job[] filteredJobs = null;
	private final HashSet<File> filePathSet;
	private final LinkedHash[] jobQueues;

	public TableModelJobs tableModel = null;

	public JobList() {
		jobsList = new ArrayList<>();
		filePathSet = new HashSet<>();

		jobQueues = new LinkedHash[3];
		for (int i = 0; i < jobQueues.length; i++) {
			jobQueues[i] = new LinkedHash();
		}
	}

	/*
	 * public void dumpHashSet(){
	 * try {
	 * Iterator it = m_al.iterator();
	 * FileWriter fw;
	 *
	 * fw = new FileWriter("C:\\fdump.txt");
	 *
	 * while (it.hasNext()) {
	 * fw.write(it.next()+"\n");
	 * }
	 * fw.close();
	 * } catch (IOException e) {
	 * e.printStackTrace();
	 * }
	 * }
	 */
	public String toString() {
		return "HashSet: " + filePathSet.size() + ", ArrayList: " + jobsList.size() + ", Array: "
				+ (filteredJobs == null ? -1 : filteredJobs.length);
	}

	public synchronized void clear() {
		filteredJobs = null;
		jobsList.clear();
		filePathSet.clear();
		for (int i = 0; i < jobQueues.length; i++) {
			jobQueues[i].clear();
		}
	}

	public synchronized boolean has(File file) {
		return filePathSet.contains(file);
	}

	public synchronized boolean addPath(File file) {
		return filePathSet.add(file);
	}

	public synchronized void filter(int status, int state, boolean includeUnknown) {
		if (status == 0) {
			filteredJobs = null;
			return;
		}
		long startTime = System.currentTimeMillis();
		ArrayList<Job> matchingJobs = new ArrayList<>(jobsList.size());
		for (int i = 0; i < jobsList.size(); i++) {
			Job job = jobsList.get(i);
			if (job.checkSep(status, state, includeUnknown)) {
				matchingJobs.add(job);
			}
		}
		filteredJobs = matchingJobs.toArray(new Job[0]);
		System.out.println("! Filter: " + (System.currentTimeMillis() - startTime));
	}

	private void addJobInternal(Job job) {
		jobsList.add(job);
		tableModel.insertJob(jobsList.size() - 1);
	}

	public synchronized Job add(File file) {
		if (filePathSet.add(file)) { // TODO if update then check against existing files
			Job job = new Job(file, Job.HASHWAIT);
			int status = A.db.getJob(job, false);
			if (status >= 0 && job.anidbFile != null) {
				A.cache.gatherInfo(job, true);
				job.setStatus(status, false);
			}
			addJobInternal(job);
			return job;
		}
		return null;
	}

	public synchronized boolean add(Job job) {
		if (filePathSet.add(job.currentFile)) {
			addJobInternal(job);
			return true;
		}
		return false;
	}

	public synchronized Job get(int index) {
		try {
			if (filteredJobs != null) {
				return filteredJobs[index];
			}
			return jobsList.get(index);
		} catch (ArrayIndexOutOfBoundsException ex) {
			System.err.println("[ ArrayIndexOutOfBoundsException " + index);
		} catch (IndexOutOfBoundsException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public synchronized Job rem(int index) {
		Job job = jobsList.remove(index);
		filePathSet.remove(job.getFile());
		return job;
	}

	public synchronized Job[] array() {
		Job[] jobArray = new Job[jobsList.size()];
		jobsList.toArray(jobArray);
		return jobArray;
	}

	public synchronized int size() {
		if (filteredJobs != null) {
			return filteredJobs.length;
		}
		return jobsList.size();
	}

	public Job getJobDio() {
		return (Job) jobQueues[QUEUE_DISK_IO].getFirst();
	}

	public Job getJobNio() {
		return (Job) jobQueues[QUEUE_NETWORK_IO].getFirst();
	}

	public boolean workForDio() {
		return !jobQueues[QUEUE_DISK_IO].isEmpty();
	}

	public boolean workForNio() {
		return !jobQueues[QUEUE_NETWORK_IO].isEmpty();
	}

	public void updateQueues(Job job, int oldStatus, int newStatus) {
		synchronized (jobQueues) {
			updateJobQueue(job, oldStatus, false); // remove from set
			updateJobQueue(job, newStatus, true); // add to set
		}
	}

	private void updateJobQueue(Job job, int status, boolean shouldAdd) {
		if (status < 0) {
			return;
		}
		int queueType = -1;
		if (A.bitcmp(status, Job.S_DO) || A.bitcmp(status, Job.S_DOING)) {
			if (A.bitcmp(status, Job.D_DIO)) {
				queueType = QUEUE_DISK_IO;
			} else if (A.bitcmp(status, Job.D_NIO)) {
				queueType = QUEUE_NETWORK_IO;
			} else {
				return;
			}
		} else if (A.bitcmp(status, Job.FAILED) || A.bitcmp(status, Job.UNKNOWN)) {
			queueType = QUEUE_ERROR;
		} else {
			return;
		}
		if (shouldAdd) {
			jobQueues[queueType].addLast(job);
		} else {
			jobQueues[queueType].remove(job);
		}
	}

	public static final int QUEUE_ERROR = 0;
	public static final int QUEUE_DISK_IO = 1;
	public static final int QUEUE_NETWORK_IO = 2;
}
