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

import epox.av.AVInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import jonelo.jacksum.algorithm.AbstractChecksum;
import jonelo.jacksum.algorithm.Edonkey;

/**
 * Manages disk I/O operations including parallel file hashing, file moving, and AV parsing.
 *
 * <p>Hashing operations run in parallel (up to 4 files simultaneously) using a thread pool,
 * while move and parse operations remain single-threaded for simplicity.
 */
public class DiskIOManager implements Runnable {
    private static final String DISK_SPACE_ERROR_MESSAGE = "There is not enough space on the disk";
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("0.00");

    /** Number of files to hash in parallel */
    private static final int PARALLEL_HASH_JOBS = 4;

    /** Buffer size per hash task (3MB) */
    private static final int BUFFER_SIZE = 1048576 * 3;

    /** Shared buffer for single-threaded operations (move, parse) */
    private static final byte[] SHARED_BUFFER = new byte[BUFFER_SIZE];

    private static final String FAILED_MOVE_CLEANUP_MESSAGE = "Cleanup after failed moving operation.";
    private static final String ABORTED_MOVE_CLEANUP_MESSAGE = "Cleanup after aborted moving operation.";
    private static final String SUCCESSFUL_MOVE_CLEANUP_MESSAGE = "Cleanup after successful moving operation.";

    /** Thread pool for parallel hashing */
    private ExecutorService hashExecutor;

    /** Jobs currently being hashed (to prevent double-submission) */
    private final Set<Job> activeHashJobs = ConcurrentHashMap.newKeySet();

    public static class ChecksumData {
        final String name;
        final AbstractChecksum algorithm;
        String hexValue;

        public ChecksumData(String name, AbstractChecksum algorithm) {
            this.name = name;
            this.algorithm = algorithm;
        }
    }

    @Override
    public void run() {
        AppContext.gui.setDiskIoOptionsEnabled(false);
        hashExecutor = Executors.newFixedThreadPool(PARALLEL_HASH_JOBS, r -> {
            Thread t = new Thread(r);
            t.setName("HashWorker-" + t.threadId());
            t.setDaemon(true);
            return t;
        });

        AppContext.gui.status0("DiskIO thread started.");

        try {
            mainLoop();
        } finally {
            shutdownExecutor();
            AppContext.gui.status0("DiskIO thread terminated.");
            AppContext.gui.statusProgressBar.setValue(0);
            AppContext.gui.diskIoThread = null;
            AppContext.gui.setDiskIoOptionsEnabled(true);
        }
    }

    /**
     * Main processing loop. Handles move/parse operations single-threaded,
     * and submits hash jobs to the thread pool for parallel processing.
     */
    private void mainLoop() {
        while (AppContext.gui.isDiskIoOk()) {
            boolean didWork = false;

            // Handle MOVE operations (single-threaded)
            Job moveJob = getNextJobByStatus(Job.MOVEWAIT);
            if (moveJob != null) {
                try {
                    fileMove(moveJob);
                } catch (IOException e) {
                    handleMoveError(moveJob, e);
                }
                didWork = true;
                continue; // Check for more work immediately
            }

            // Handle PARSE operations (single-threaded)
            Job parseJob = getNextJobByStatus(Job.PARSEWAIT);
            if (parseJob != null) {
                try {
                    fileParse(parseJob);
                } catch (IOException e) {
                    handleParseError(parseJob, e);
                }
                didWork = true;
                continue;
            }

            // Submit hash jobs to thread pool (parallel)
            int submitted = submitHashJobs();
            if (submitted > 0) {
                didWork = true;
            }

            // Exit if no work available and no jobs in progress
            if (!didWork && activeHashJobs.isEmpty() && !hasMoreWork()) {
                break;
            }

            // Brief pause to avoid tight loop when waiting for hash completion
            if (!didWork && !activeHashJobs.isEmpty()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Get the next job with the specified status from the disk I/O queue.
     */
    private Job getNextJobByStatus(int status) {
        List<Job> jobs = AppContext.jobs.getJobsDio(1, status, Set.of());
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    /**
     * Check if there's more work available in the disk I/O queue.
     */
    private boolean hasMoreWork() {
        return AppContext.jobs.workForDio();
    }

    /**
     * Submit hash jobs to the thread pool, up to PARALLEL_HASH_JOBS concurrent.
     *
     * @return number of jobs submitted
     */
    private int submitHashJobs() {
        int slotsAvailable = PARALLEL_HASH_JOBS - activeHashJobs.size();
        if (slotsAvailable <= 0) {
            return 0;
        }

        List<Job> jobs = AppContext.jobs.getJobsDio(slotsAvailable, Job.HASHWAIT, activeHashJobs);
        for (Job job : jobs) {
            activeHashJobs.add(job);
            JobManager.updateStatus(job, Job.HASHING);
            hashExecutor.submit(new HashTask(job));
        }

        return jobs.size();
    }

    /**
     * Gracefully shut down the hash executor, waiting for active tasks to complete.
     */
    private void shutdownExecutor() {
        if (hashExecutor == null) {
            return;
        }

        hashExecutor.shutdown();
        try {
            // Wait for active hash jobs to complete
            if (!hashExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                hashExecutor.shutdownNow();
                if (!hashExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Hash executor did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            hashExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        activeHashJobs.clear();
    }

    /**
     * Hash task that runs in the thread pool. Each task has its own buffer
     * and hash algorithm instances for thread safety.
     */
    private class HashTask implements Runnable {
        private final Job job;
        private final byte[] buffer = new byte[BUFFER_SIZE];
        private final LinkedHashMap<String, ChecksumData> checksums;
        private final long startTime;

        HashTask(Job job) {
            this.job = job;
            this.checksums = AppContext.gui.miscOptionsPanel.createChecksums();
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            try {
                hashFile();
            } catch (IOException e) {
                handleHashError(e);
            } finally {
                activeHashJobs.remove(job);
            }
        }

        private void hashFile() throws IOException {
            File file = job.getFile();
            job.fileSize = file.length();
            job.hashProgress = 0f;

            if (job.fileSize < 1) {
                JobManager.updateStatus(job, Job.FAILED);
                job.setError("File size less than 1.");
                return;
            }

            if (!file.exists()) {
                JobManager.updateStatus(job, Job.FAILED);
                job.setError("File does not exist.");
                return;
            }

            long totalBytesRead = 0;
            int bytesRead;

            try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                while (AppContext.gui.isDiskIoOk() && (bytesRead = inputStream.read(buffer)) != -1) {
                    // Update all hash algorithms with the chunk
                    for (ChecksumData data : checksums.values()) {
                        data.algorithm.update(buffer, 0, bytesRead);
                    }

                    totalBytesRead += bytesRead;
                    job.hashProgress = (float) totalBytesRead / job.fileSize;
                }
            }

            // Check if hashing completed or was interrupted
            if (job.hashProgress >= 0.9999f) {
                completeHashing(file);
            } else {
                // Interrupted - reset progress and return to wait state
                job.hashProgress = 0f;
                JobManager.updateStatus(job, Job.HASHWAIT);
            }
        }

        private void completeHashing(File file) {
            // Extract hash values from all algorithms
            for (ChecksumData data : checksums.values()) {
                data.hexValue = data.algorithm.getHexValue();
            }

            // Store hash values in job
            job.ed2kHash = checksums.get("ed2k").hexValue;
            job.md5Hash = checksums.containsKey("md5") ? checksums.get("md5").hexValue : null;
            job.sha1Hash = checksums.containsKey("sha1") ? checksums.get("sha1").hexValue : null;
            job.tthHash = checksums.containsKey("tth") ? checksums.get("tth").hexValue : null;
            job.crc32Hash = checksums.containsKey("crc32") ? checksums.get("crc32").hexValue : null;

            job.hashProgress = 1f;

            long endTime = System.currentTimeMillis();
            float elapsedSeconds = (endTime - startTime) / 1000f;

            // Build output strings
            String ed2kLink = "ed2k://|file|" + file.getName() + "|" + file.length() + "|" + job.ed2kHash + "|";
            String otherHashes = checksums.values().stream()
                    .skip(1)
                    .map(data -> data.name + ": " + data.hexValue)
                    .collect(Collectors.joining("\n"));
            String statsMessage = "Hashed " + HyperlinkBuilder.formatAsName(file) + " @ "
                    + formatStats(file.length(), elapsedSeconds);

            // Update UI on EDT for thread safety
            SwingUtilities.invokeLater(() -> {
                AppContext.gui.printHash(ed2kLink);
                if (!otherHashes.isEmpty()) {
                    AppContext.gui.printHash(otherHashes);
                }
                AppContext.gui.println(statsMessage);
            });

            // Transition to HASHED state (triggers next workflow step)
            JobManager.updateStatus(job, Job.HASHED);
        }

        private void handleHashError(IOException e) {
            e.printStackTrace();
            job.hashProgress = 0f;
            JobManager.updateStatus(job, Job.FAILED);
            job.setError(e.getMessage());

            String errorMessage = e.getMessage();
            SwingUtilities.invokeLater(() -> {
                AppContext.gui.println(HyperlinkBuilder.formatAsError(errorMessage));
            });
        }
    }

    // ========================= Single-threaded operations =========================

    private void fileParse(Job job) throws IOException {
        if (!AVInfo.ok()) {
            JobManager.updateStatus(job, Job.FAILED);
            return;
        }
        File file = job.getFile();
        JobManager.updateStatus(job, Job.PARSING);
        AppContext.gui.status0("Parsing " + file.getName());
        AppContext.gui.statusProgressBar.setValue(0);

        AVInfo avInfo = new AVInfo(file);
        long startTime = System.currentTimeMillis();
        float parseProgress = avInfo.parse();
        while (AppContext.gui.isDiskIoOk() && parseProgress >= 0) {
            AppContext.gui.statusProgressBar.setValue((int) (10 * parseProgress));
            parseProgress = avInfo.parse();
        }
        if (parseProgress < 0) {
            job.avFileInfo = avInfo.build();
        }
        avInfo.close();
        long endTime = System.currentTimeMillis();
        AppContext.gui.statusProgressBar.setValue(1000);
        AppContext.gui.println("Parsed " + HyperlinkBuilder.formatAsName(file) + " @ "
                + formatStats(file.length(), (endTime - startTime) / 1000f));
        JobManager.updateStatus(job, Job.FINISHED);
    }

    private void handleParseError(Job job, IOException e) {
        e.printStackTrace();
        JobManager.updateStatus(job, Job.FAILED);
        job.setError(e.getMessage());
        AppContext.gui.println(HyperlinkBuilder.formatAsError(e.getMessage()));
    }

    private void fileMove(Job job) throws IOException {
        if (job.currentFile.equals(job.targetFile)) {
            job.targetFile = null;
            JobManager.updateStatus(job, Job.MOVED);
            return;
        }
        if (!job.currentFile.exists()) {
            JobManager.updateStatus(job, Job.FAILED);
            job.setError("File does not exist.");
            AppContext.gui.println(HyperlinkBuilder.formatAsError("File " + job.currentFile + " does not exist!"));
            return;
        }
        if (!job.currentFile.canRead()) {
            AppContext.gui.println(HyperlinkBuilder.formatAsError("Cannot read file: " + job.currentFile));
            JobManager.updateStatus(job, Job.FAILED);
            job.setError("File can not be read.");
            return;
        }
        File parentDirectory = job.targetFile.getParentFile();
        try {
            if (!Files.exists(parentDirectory.toPath())) {
                Files.createDirectories(parentDirectory.toPath());
            }
        } catch (IOException e) {
            String folderCreationError = "Folder " + parentDirectory + " cannot be created!";
            job.setError(folderCreationError);
            AppContext.gui.println(HyperlinkBuilder.formatAsError(folderCreationError));
            return;
        }
        JobManager.updateStatus(job, Job.MOVING);
        AppContext.gui.status0("Moving " + job.currentFile.getName());

        long startTime = System.currentTimeMillis();
        boolean needsCopy = !Files.exists(job.targetFile.toPath());
        if (needsCopy && !fileCopy(job.currentFile, job.targetFile)) {
            AppContext.deleteFileAndFolder(job.targetFile, ABORTED_MOVE_CLEANUP_MESSAGE);
            JobManager.updateStatus(job, Job.MOVEWAIT);
            return;
        }

        if (needsCopy) {
            copySiblingFiles(job.currentFile, job.targetFile);
        }

        JobManager.updateStatus(job, Job.MOVECHECK);
        AppContext.gui.status0("Checking " + job.currentFile.getName());
        String checksumHex = computeFileChecksum(job.targetFile);
        if (checksumHex == null) {
            handleCanceledChecksum(job, needsCopy);
        } else if (job.ed2kHash.equalsIgnoreCase(checksumHex)) {
            handleSuccessfulMove(job, startTime);
        } else {
            handleFailedChecksumVerification(job, needsCopy);
        }
    }

    private void handleMoveError(Job job, IOException e) {
        e.printStackTrace();
        if (job.targetFile != null) {
            AppContext.deleteFileAndFolder(job.targetFile, FAILED_MOVE_CLEANUP_MESSAGE);
        }
        JobManager.updateStatus(job, Job.FAILED);
        job.setError(e.getMessage());

        String errorMessage = e.getMessage();
        AppContext.gui.println(HyperlinkBuilder.formatAsError(errorMessage));
        AppContext.gui.status0(errorMessage);
        if (AppContext.gui.isDiskIoOk()) {
            AppContext.gui.toggleDiskIo();
        }
    }

    private void handleCanceledChecksum(Job job, boolean needsCopy) {
        if (needsCopy) {
            AppContext.deleteFileAndFolder(job.targetFile, ABORTED_MOVE_CLEANUP_MESSAGE);
        }
        if (!AppContext.gui.isDiskIoOk()) {
            JobManager.updateStatus(job, Job.MOVEWAIT);
        }
    }

    private void handleSuccessfulMove(Job job, long startTime) {
        AppContext.gui.println("Moved " + HyperlinkBuilder.formatAsName(job.currentFile) + " to "
                + HyperlinkBuilder.formatAsName(job.targetFile) + " @ "
                + formatStats(job.currentFile.length(), (System.currentTimeMillis() - startTime) / 1000f));
        AppContext.deleteFileAndFolder(job.currentFile, SUCCESSFUL_MOVE_CLEANUP_MESSAGE);
        JobManager.setJobFile(job, job.targetFile);
        job.targetFile = null;
        job.directoryId = -1;
        JobManager.updateStatus(job, Job.MOVED);
    }

    private void handleFailedChecksumVerification(Job job, boolean needsCopy) {
        JobManager.updateStatus(job, Job.FAILED);
        if (needsCopy) {
            job.setError("CRC check failed on copy. HW problem?");
            AppContext.gui.println(
                    HyperlinkBuilder.formatAsError(job.currentFile + " was not moved! CRC check failed. HW problem?"));
        } else {
            job.setError("CRC check failed on copy. Destination file does already exist, but with"
                    + " wrong CRC. Handle this manually.");
            AppContext.gui.println(HyperlinkBuilder.formatAsError(job.currentFile + " was not moved! Destination file '"
                    + job.targetFile + "' does already exist, but with wrong CRC. Handle this" + " manually."));
        }
    }

    private String formatStats(long fileSizeBytes, float elapsedTimeSeconds) {
        if (elapsedTimeSeconds <= 0) {
            elapsedTimeSeconds = 0.001f; // Avoid division by zero
        }
        String speedText = DECIMAL_FORMATTER.format(fileSizeBytes / elapsedTimeSeconds / 1048576);
        String timeText = DECIMAL_FORMATTER.format(elapsedTimeSeconds);
        return HyperlinkBuilder.formatAsNumber(speedText) + " MB/s ("
                + HyperlinkBuilder.formatAsNumber(fileSizeBytes + "") + " bytes in "
                + HyperlinkBuilder.formatAsNumber(timeText) + " seconds)";
    }

    private boolean fileCopy(File sourceFile, File destinationFile) throws IOException {
        int bytesRead;
        long totalBytesRead = 0;
        long fileLength = sourceFile.length();
        float progress = 0;

        AppContext.gui.statusProgressBar.setValue(0);
        try (InputStream inputStream = Files.newInputStream(sourceFile.toPath());
                OutputStream outputStream = Files.newOutputStream(destinationFile.toPath())) {
            while (AppContext.gui.isDiskIoOk() && (bytesRead = inputStream.read(SHARED_BUFFER)) != -1) {
                outputStream.write(SHARED_BUFFER, 0, bytesRead);
                totalBytesRead += bytesRead;
                progress = (float) totalBytesRead / fileLength;
                AppContext.gui.statusProgressBar.setValue((int) (1000 * progress));
            }
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("space")) {
                AppContext.dialog("IOException", DISK_SPACE_ERROR_MESSAGE + ":\n" + destinationFile);
            } else {
                throw e;
            }
        }
        return progress == 1;
    }

    private String computeFileChecksum(File file) throws IOException {
        Edonkey edonkeyHash;
        try {
            edonkeyHash = new Edonkey();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        int bytesRead;
        long totalBytesRead = 0;
        long fileLength = file.length();
        float progress = 0;
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            AppContext.gui.statusProgressBar.setValue(0);
            while (AppContext.gui.isDiskIoOk() && (bytesRead = inputStream.read(SHARED_BUFFER)) != -1) {
                edonkeyHash.update(SHARED_BUFFER, 0, bytesRead);
                totalBytesRead += bytesRead;
                progress = (float) totalBytesRead / fileLength;
                AppContext.gui.statusProgressBar.setValue((int) (1000 * progress));
            }
        }
        if (progress < 1) {
            return null;
        }
        return edonkeyHash.getHexValue();
    }

    private void copySiblingFiles(File sourceFile, File destinationFile) {
        File sourceParent = sourceFile.getParentFile();
        File destParent = destinationFile.getParentFile();
        String sourceFileName = sourceFile.getName();
        int dotIndex = sourceFileName.lastIndexOf('.');
        String sourceFileBaseName = dotIndex > 0 ? sourceFileName.substring(0, dotIndex) : sourceFileName;

        File[] siblings = sourceParent.listFiles(f -> {
            String name = f.getName();
            return name.startsWith(sourceFileBaseName) && !name.equals(sourceFileName);
        });

        if (siblings == null || siblings.length == 0) {
            return;
        }

        for (File sibling : siblings) {
            String siblingName = sibling.getName();
            int extIndex = siblingName.lastIndexOf('.');
            String destName;
            if (extIndex > 0) {
                destName = destinationFile
                                .getName()
                                .substring(0, destinationFile.getName().lastIndexOf('.'))
                        + siblingName.substring(extIndex);
            } else {
                destName = siblingName;
            }
            File destSibling = new File(destParent, destName);
            if (destSibling.exists()) {
                continue;
            }
            try {
                Files.copy(sibling.toPath(), destSibling.toPath());
                AppContext.gui.println("Renamed sibling: " + sibling.getName());
            } catch (IOException e) {
                AppContext.gui.println("Failed to rename sibling file: " + sibling.getName());
            }
        }
    }
}
