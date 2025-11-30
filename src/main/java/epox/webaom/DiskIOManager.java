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
 * Created on 22.01.05
 *
 * @version 	05
 * @author 		epoximator
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
import java.util.stream.Collectors;
import jonelo.jacksum.algorithm.AbstractChecksum;
import jonelo.jacksum.algorithm.Edonkey;

public class DiskIOManager implements Runnable {
    private static final String DISK_SPACE_ERROR_MESSAGE = "There is not enough space on the disk";
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("0.00");
    private static final int BUFFER_SIZE = 1048576 * 3;
    private static final byte[] READ_BUFFER = new byte[BUFFER_SIZE];
    private static final String FAILED_MOVE_CLEANUP_MESSAGE = "Cleanup after failed moving operation.";
    private static final String ABORTED_MOVE_CLEANUP_MESSAGE = "Cleanup after aborted moving operation.";
    private static final String SUCCESSFUL_MOVE_CLEANUP_MESSAGE = "Cleanup after successful moving operation.";

    static class ChecksumData {
        final String name;
        final AbstractChecksum algorithm;
        String hexValue;

        ChecksumData(String name, AbstractChecksum algorithm) {
            this.name = name;
            this.algorithm = algorithm;
        }
    }

    private LinkedHashMap<String, ChecksumData> checksums;

    public void run() {
        AppContext.gui.setDiskIoOptionsEnabled(false);
        Job currentJob = null;
        try {
            checksums = AppContext.gui.miscOptionsPanel.getChecksums();
            AppContext.gui.status0("DiskIO thread started.");
            while (AppContext.gui.isDiskIoOk() && (currentJob = AppContext.jobs.getJobDio()) != null) {
                switch (currentJob.getStatus()) {
                    case Job.HASHWAIT:
                        fileHash(currentJob);
                        break;
                    case Job.MOVEWAIT:
                        fileMove(currentJob);
                        break;
                    case Job.PARSEWAIT:
                        fileParse(currentJob);
                        break;
                    default:
                        AppContext.dialog("INF LOOP", "Illegal status: " + currentJob.getStatusText());
                        AppContext.gui.kill();
                }
                AppContext.gui.statusProgressBar.setValue(0);
            }
            AppContext.gui.status0("DiskIO thread terminated.");
        } catch (IOException e) {
            e.printStackTrace();
            if (currentJob.targetFile != null) {
                AppContext.deleteFileAndFolder(currentJob.targetFile, FAILED_MOVE_CLEANUP_MESSAGE);
            }
            JobManager.updateStatus(currentJob, Job.FAILED);
            currentJob.setError(e.getMessage());

            String errorMessage = e.getMessage();
            AppContext.gui.println(HyperlinkBuilder.formatAsError(errorMessage));
            AppContext.gui.status0(errorMessage);
            if (AppContext.gui.isDiskIoOk()) {
                AppContext.gui.toggleDiskIo();
            }
        }
        AppContext.gui.diskIoThread = null;
        AppContext.gui.setDiskIoOptionsEnabled(true);
    }

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

    private void fileHash(Job job) throws IOException {
        File file = job.getFile();
        job.fileSize = file.length();
        if (job.fileSize < 1) {
            JobManager.updateStatus(job, Job.FAILED);
            job.setError("File size less than 1.");
            return;
        }
        int bytesRead;
        long totalBytesRead = 0;
        long fileLength = file.length();
        float progress = 0;

        JobManager.updateStatus(job, Job.HASHING);
        AppContext.gui.status0("Hashing " + file.getName());
        AppContext.gui.statusProgressBar.setValue(0);

        long startTime = System.currentTimeMillis();
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            while (AppContext.gui.isDiskIoOk() && (bytesRead = inputStream.read(READ_BUFFER)) != -1) {
                checksums.values().forEach(data -> data.algorithm.update(READ_BUFFER, 0, bytesRead));
                totalBytesRead += bytesRead;
                progress = (float) totalBytesRead / fileLength;
                AppContext.gui.statusProgressBar.setValue((int) (1000 * progress));
            }
        }
        checksums.values().forEach(data -> {
            data.hexValue = data.algorithm.getHexValue();
            data.algorithm.reset();
        });
        long endTime = System.currentTimeMillis();
        AppContext.gui.statusProgressBar.setValue(0);

        if (progress == 1) {
            job.ed2kHash = checksums.get("ed2k").hexValue;
            job.md5Hash = checksums.containsKey("md5") ? checksums.get("md5").hexValue : null;
            job.sha1Hash = checksums.containsKey("sha1") ? checksums.get("sha1").hexValue : null;
            job.tthHash = checksums.containsKey("tth") ? checksums.get("tth").hexValue : null;
            job.crc32Hash = checksums.containsKey("crc32") ? checksums.get("crc32").hexValue : null;

            String ed2kLink = "ed2k://|file|" + file.getName() + "|" + file.length() + "|" + job.ed2kHash + "|";

            AppContext.gui.printHash(ed2kLink);
            AppContext.gui.printHash(checksums.values().stream()
                    .skip(1)
                    .map(data -> data.name + ": " + data.hexValue)
                    .collect(Collectors.joining("\n")));

            AppContext.gui.println("Hashed " + HyperlinkBuilder.formatAsName(file) + " @ "
                    + formatStats(file.length(), (endTime - startTime) / 1000f));
            JobManager.updateStatus(job, Job.HASHED);
        } else {
            JobManager.updateStatus(job, Job.HASHWAIT);
        }
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
            while (AppContext.gui.isDiskIoOk() && (bytesRead = inputStream.read(READ_BUFFER)) != -1) {
                outputStream.write(READ_BUFFER, 0, bytesRead);
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
            while (AppContext.gui.isDiskIoOk() && (bytesRead = inputStream.read(READ_BUFFER)) != -1) {
                edonkeyHash.update(READ_BUFFER, 0, bytesRead);
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
}
