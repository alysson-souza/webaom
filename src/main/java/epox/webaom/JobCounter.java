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
 * Created on 24.10.05
 *
 * @version 	01 (1.14)
 * @author 		epoximator
 */

package epox.webaom;

public class JobCounter {
    /** Total number of jobs registered */
    private int totalJobCount = 0;
    /** Count of jobs by status category */
    private final int[] statusCounts = new int[STATUS_COUNT];

    public synchronized int getProgress() {
        int activeJobCount = totalJobCount - statusCounts[INDEX_HALTED] - statusCounts[INDEX_ERROR];
        if (activeJobCount == 0) {
            return 0;
        }
        return 1000 * (statusCounts[INDEX_FINISHED]) / activeJobCount;
    }

    public synchronized String getStatus() {
        String statusText = "";
        for (int index = 0; index < STATUS_COUNT; index++) {
            statusText += STATUS_NAMES[index] + "=" + statusCounts[index] + " ";
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
        for (int index = 0; index < STATUS_COUNT; index++) {
            statusCounts[index] = 0;
        }
        totalJobCount = 0;
    }

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
}
