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

import java.util.Arrays;

public class JobCounter {
    /** Index for finished jobs count */
    private static final int INDEX_FINISHED = 0;
    /** Index for disk I/O jobs count */
    private static final int INDEX_DISK_IO = 1;
    /** Index for network I/O jobs count */
    private static final int INDEX_NETWORK_IO = 2;
    /** Index for error jobs count */
    private static final int INDEX_ERROR = 3;
    /** Index for halted jobs count */
    private static final int INDEX_HALTED = 4;
    /** Total number of status categories */
    private static final int STATUS_COUNT = 5;
    /** Display names for status categories */
    private static final String[] STATUS_NAMES = {"fin", "dio", "nio", "err", "hlt"};
    /** Count of jobs by status category */
    private final int[] statusCounts = new int[STATUS_COUNT];
    /** Total number of jobs registered */
    private int totalJobCount = 0;

    public synchronized int getProgress() {
        int activeJobCount = totalJobCount - statusCounts[INDEX_HALTED] - statusCounts[INDEX_ERROR];
        if (activeJobCount == 0) {
            return 0;
        }
        return 1000 * (statusCounts[INDEX_FINISHED]) / activeJobCount;
    }

    public synchronized String getStatus() {
        StringBuilder statusText = new StringBuilder();
        for (int index = 0; index < STATUS_COUNT; index++) {
            statusText
                    .append(STATUS_NAMES[index])
                    .append("=")
                    .append(statusCounts[index])
                    .append(" ");
        }
        return statusText + " tot=" + totalJobCount;
    }

    public synchronized void register(int oldStatus, int oldHealth, int newStatus, int newHealth) {
        updateCount(oldStatus, oldHealth, false);
        updateCount(newStatus, newHealth, true);
    }

    private void updateCount(int status, int health, boolean increment) {
        if (status < 0) {
            totalJobCount++;
            return;
        }
        int categoryIndex = INDEX_HALTED;
        if (AppContext.bitcmp(health, Job.H_NORMAL)
                && (AppContext.bitcmp(status, Job.S_DO) || AppContext.bitcmp(status, Job.S_DOING))) {
            if (AppContext.bitcmp(status, Job.D_DIO)) {
                categoryIndex = INDEX_DISK_IO;
            } else if (AppContext.bitcmp(status, Job.D_NIO)) {
                categoryIndex = INDEX_NETWORK_IO;
            }
        } else if (AppContext.bitcmp(health, Job.H_NORMAL) && AppContext.bitcmp(status, Job.FINISHED)) {
            categoryIndex = INDEX_FINISHED;
        } else if (AppContext.bitcmp(status, Job.FAILED) || AppContext.bitcmp(status, Job.UNKNOWN)) {
            categoryIndex = INDEX_ERROR;
        }
        if (increment) {
            statusCounts[categoryIndex]++;
        } else {
            statusCounts[categoryIndex]--;
        }
    }

    public synchronized void reset() {
        Arrays.fill(statusCounts, 0);
        totalJobCount = 0;
    }

    /**
     * Remove a job from the counters when it is deleted from the list.
     *
     * @param status the job status flags
     * @param health the job health flags
     */
    public synchronized void unregister(int status, int health) {
        updateCount(status, health, false);
        if (totalJobCount > 0) {
            totalJobCount--;
        }
    }
}
