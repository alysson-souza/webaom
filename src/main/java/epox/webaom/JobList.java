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

import epox.webaom.ui.TableModelJobs;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class JobList {
    public static final int QUEUE_ERROR = 0;
    public static final int QUEUE_DISK_IO = 1;
    public static final int QUEUE_NETWORK_IO = 2;
    private final ArrayList<Job> jobsList;
    private final HashSet<File> filePathSet;
    private final List<LinkedHashMap<Job, Job>> jobQueues;
    public TableModelJobs tableModel = null;
    private Job[] filteredJobs = null;

    public JobList() {
        jobsList = new ArrayList<>();
        filePathSet = new HashSet<>();

        jobQueues = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            jobQueues.add(new LinkedHashMap<>());
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
        for (LinkedHashMap<Job, Job> jobQueue : jobQueues) {
            jobQueue.clear();
        }
    }

    public synchronized boolean has(File file) {
        return filePathSet.contains(file);
    }

    public synchronized void addPath(File file) {
        filePathSet.add(file);
    }

    public synchronized void filter(int status, int state, boolean includeUnknown) {
        if (status == 0) {
            filteredJobs = null;
            return;
        }
        long startTime = System.currentTimeMillis();
        ArrayList<Job> matchingJobs = new ArrayList<>(jobsList.size());
        for (Job job : jobsList) {
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
            int status = AppContext.databaseManager.getJob(job, false);
            if (status >= 0 && job.anidbFile != null) {
                AppContext.cache.gatherInfo(job, true);
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

    public synchronized int removeJobs(Collection<Job> jobsToRemove) {
        if (jobsToRemove == null || jobsToRemove.isEmpty()) {
            return 0;
        }
        HashSet<Job> removalSet = new HashSet<>();
        for (Job job : jobsToRemove) {
            if (job != null) {
                removalSet.add(job);
            }
        }
        if (removalSet.isEmpty()) {
            return 0;
        }
        ArrayList<Job> removedJobs = new ArrayList<>();
        Iterator<Job> iterator = jobsList.iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            if (removalSet.contains(job)) {
                iterator.remove();
                filePathSet.remove(job.getFile());
                removedJobs.add(job);
            }
        }
        if (removedJobs.isEmpty()) {
            return 0;
        }
        HashSet<Job> removedSet = new HashSet<>(removedJobs);
        if (filteredJobs != null) {
            ArrayList<Job> filtered = new ArrayList<>(filteredJobs.length);
            for (Job job : filteredJobs) {
                if (!removedSet.contains(job)) {
                    filtered.add(job);
                }
            }
            filteredJobs = filtered.toArray(new Job[0]);
        }
        for (LinkedHashMap<Job, Job> queue : jobQueues) {
            queue.keySet().removeAll(removedSet);
        }
        for (Job job : removedJobs) {
            AppContext.jobCounter.unregister(job.getStatus(), job.getHealth());
        }
        if (tableModel != null) {
            tableModel.fireTableDataChanged();
        }
        return removedJobs.size();
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
        LinkedHashMap<Job, Job> queue = jobQueues.get(QUEUE_DISK_IO);
        return queue.isEmpty() ? null : queue.values().iterator().next();
    }

    /**
     * Get up to maxCount jobs waiting for a specific disk I/O operation.
     * Filters by the specified status (e.g., HASHWAIT, MOVEWAIT, PARSEWAIT).
     *
     * @param maxCount maximum number of jobs to return
     * @param status the job status to filter by
     * @param exclude jobs to exclude (already being processed)
     * @return list of jobs ready for processing
     */
    public synchronized List<Job> getJobsDio(int maxCount, int status, Set<Job> exclude) {
        List<Job> jobs = new ArrayList<>();
        LinkedHashMap<Job, Job> queue = jobQueues.get(QUEUE_DISK_IO);
        for (Job job : queue.values()) {
            if (jobs.size() >= maxCount) {
                break;
            }
            if (job.getStatus() == status && !exclude.contains(job)) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    public Job getJobNio() {
        LinkedHashMap<Job, Job> queue = jobQueues.get(QUEUE_NETWORK_IO);
        return queue.isEmpty() ? null : queue.values().iterator().next();
    }

    public boolean workForDio() {
        return !jobQueues.get(QUEUE_DISK_IO).isEmpty();
    }

    public boolean workForNio() {
        return !jobQueues.get(QUEUE_NETWORK_IO).isEmpty();
    }

    public void updateQueues(Job job, int oldStatus, int newStatus) {
        synchronized (jobQueues) {
            int oldQueue = getQueueType(oldStatus);
            int newQueue = getQueueType(newStatus);

            // Only update if changing queues (preserve position within same queue)
            if (oldQueue != newQueue) {
                if (oldQueue >= 0) {
                    jobQueues.get(oldQueue).remove(job);
                }
                if (newQueue >= 0) {
                    jobQueues.get(newQueue).putIfAbsent(job, job);
                }
            }
        }
    }

    /**
     * Determine which queue a job status belongs to.
     * @return queue index, or -1 if status doesn't belong to any queue
     */
    private int getQueueType(int status) {
        if (status < 0) {
            return -1;
        }
        if (AppContext.bitcmp(status, Job.S_DO) || AppContext.bitcmp(status, Job.S_DOING)) {
            if (AppContext.bitcmp(status, Job.D_DIO)) {
                return QUEUE_DISK_IO;
            } else if (AppContext.bitcmp(status, Job.D_NIO)) {
                return QUEUE_NETWORK_IO;
            }
        } else if (AppContext.bitcmp(status, Job.FAILED) || AppContext.bitcmp(status, Job.UNKNOWN)) {
            return QUEUE_ERROR;
        }
        return -1;
    }

    private void updateJobQueue(Job job, int status, boolean shouldAdd) {
        if (status < 0) {
            return;
        }
        int queueType = getQueueType(status);
        if (queueType < 0) {
            return;
        }
        if (shouldAdd) {
            jobQueues.get(queueType).putIfAbsent(job, job);
        } else {
            jobQueues.get(queueType).remove(job);
        }
    }
}
