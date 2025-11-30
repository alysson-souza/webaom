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
import epox.util.HashContainer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import jonelo.jacksum.algorithm.Edonkey;

public class DiskIO implements Runnable {
	private static final String DISK_SPACE_ERROR_MESSAGE = "There is not enough space on the disk";
	private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("0.00");
	private static final int BUFFER_SIZE = 1048576 * 3;
	private static final byte[] READ_BUFFER = new byte[BUFFER_SIZE];
	private static String failedMoveCleanupMessage;
	private static String abortedMoveCleanupMessage;
	private static String successfulMoveCleanupMessage;

	private HashContainer hashContainer;

	public DiskIO() {
		String cleanupPrefix = "Cleanup after ";
		String cleanupSuffix = " moving operation.";
		failedMoveCleanupMessage = cleanupPrefix + "failed" + cleanupSuffix;
		abortedMoveCleanupMessage = cleanupPrefix + "aborted" + cleanupSuffix;
		successfulMoveCleanupMessage = cleanupPrefix + "successful" + cleanupSuffix;
	}

	public void run() {
		A.gui.setDiskIoOptionsEnabled(false);
		Job currentJob = null;
		try {
			hashContainer = A.gui.miscOptionsPanel.getHashContainer();
			A.gui.status0("DiskIO thread started.");
			while (A.gui.isDiskIoOk() && (currentJob = A.jobs.getJobDio()) != null) {
				// !A.nr_dio = currentJob.mIid;
				switch (currentJob.getStatus()) {
					case Job.HASHWAIT :
						fileHash(currentJob);
						break;
					case Job.MOVEWAIT :
						fileMove(currentJob);
						break;
					case Job.PARSEWAIT :
						fileParse(currentJob);
						break;
					default :
						A.dialog("INF LOOP", "Illegal status: " + currentJob.getStatusText());
						A.gui.kill();
				}
				// !A.nr_dio = -1;
				A.gui.statusProgressBar.setValue(0);
			}
			A.gui.status0("DiskIO thread terminated.");
		} catch (IOException e) {
			e.printStackTrace();
			if (currentJob != null) {
				if (currentJob.targetFile != null) {
					A.deleteFileAndFolder(currentJob.targetFile, failedMoveCleanupMessage);
				}
				JobMan.updateStatus(currentJob, Job.FAILED);
				currentJob.setError(e.getMessage());
				// A.gui.updateJobTable(currentJob);
			}

			String errorMessage = e.getMessage();
			A.gui.println(Hyper.formatAsError(errorMessage));
			A.gui.status0(errorMessage);
			if (A.gui.isDiskIoOk()) {
				A.gui.toggleDiskIo();
			}
		}
		// !A.nr_dio = -1;
		A.gui.diskIoThread = null;
		A.gui.setDiskIoOptionsEnabled(true);
	}

	private void fileParse(Job job) throws IOException {
		if (!AVInfo.ok()) {
			JobMan.updateStatus(job, Job.FAILED);
			return;
		}
		File file = job.getFile();
		JobMan.updateStatus(job, Job.PARSING);
		// A.gui.updateJobTable(job);
		A.gui.status0("Parsing " + file.getName()); // +" (#"+(job.mIid+1)+")");
		A.gui.statusProgressBar.setValue(0);

		AVInfo avInfo = new AVInfo(file);
		long startTime = System.currentTimeMillis();
		float parseProgress = avInfo.parse();
		while (A.gui.isDiskIoOk() && parseProgress >= 0) {
			A.gui.statusProgressBar.setValue((int) (10 * parseProgress));
			parseProgress = avInfo.parse();
		}
		if (parseProgress < 0) {
			job.avFileInfo = avInfo.build();
		}
		avInfo.close();
		long endTime = System.currentTimeMillis();
		A.gui.statusProgressBar.setValue(1000);
		A.gui.println("Parsed " + Hyper.formatAsName(file) + " @ "
				+ formatStats(file.length(), (endTime - startTime) / 1000f));
		JobMan.updateStatus(job, Job.FINISHED);
		// A.gui.updateJobTable(job);
	}

	private void fileHash(Job job) throws IOException {
		File file = job.getFile();
		job.fileSize = file.length();
		if (job.fileSize < 1) {
			JobMan.updateStatus(job, Job.FAILED);
			job.setError("File size less than 1.");
			// A.gui.updateJobTable(job);
			return;
		}
		int bytesRead;
		long totalBytesRead = 0;
		long fileLength = file.length();
		float progress = 0;

		JobMan.updateStatus(job, Job.HASHING);
		// A.gui.updateJobTable(job);
		A.gui.status0("Hashing " + file.getName()); // +" (#"+(job.mIid+1)+")");
		A.gui.statusProgressBar.setValue(0);

		long startTime = System.currentTimeMillis();
		InputStream inputStream = new FileInputStream(file);
		while (A.gui.isDiskIoOk() && (bytesRead = inputStream.read(READ_BUFFER)) != -1) {
			hashContainer.update(READ_BUFFER, 0, bytesRead);
			totalBytesRead += bytesRead;
			progress = (float) totalBytesRead / fileLength;
			A.gui.statusProgressBar.setValue((int) (1000 * progress));
		}
		inputStream.close();
		hashContainer.finalizeHashes();
		long endTime = System.currentTimeMillis();
		A.gui.statusProgressBar.setValue(0);

		if (progress == 1) {
			job.ed2kHash = hashContainer.getHex("ed2k");
			job.md5Hash = hashContainer.getHex("md5");
			job.sha1Hash = hashContainer.getHex("sha1");
			job.tthHash = hashContainer.getHex("tth");
			job.crc32Hash = hashContainer.getHex("crc32");

			String ed2kLink = "ed2k://|file|" + file.getName() + "|" + file.length() + "|" + job.ed2kHash + "|";

			A.gui.printHash(ed2kLink);
			A.gui.printHash(hashContainer.toString());

			A.gui.println("Hashed " + Hyper.formatAsName(file) + " @ "
					+ formatStats(file.length(), (endTime - startTime) / 1000f));
			JobMan.updateStatus(job, Job.HASHED);
		} else {
			JobMan.updateStatus(job, Job.HASHWAIT);
		}
		// A.gui.updateJobTable(job);
	}

	private void fileMove(Job job) throws IOException {
		if (job.currentFile.equals(job.targetFile)) {
			job.targetFile = null;
			JobMan.updateStatus(job, Job.MOVED);
			// A.gui.updateJobTable(job);
		}
		if (!job.currentFile.exists()) {
			JobMan.updateStatus(job, Job.FAILED);
			job.setError("File does not exist.");
			// A.gui.updateJobTable(job);
			A.gui.println(Hyper.formatAsError("File " + job.currentFile + " does not exist!"));
			return;
		}
		if (!job.currentFile.canRead()) {
			System.out.println("! Cannot read file: " + job.currentFile);
			JobMan.updateStatus(job, Job.FAILED);
			job.setError("File can not be read.");
			return;
		}
		File parentDirectory = job.targetFile.getParentFile();
		if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
			String folderCreationError = "Folder " + parentDirectory + " cannot be created!";
			job.setError(folderCreationError);
			A.gui.println(Hyper.formatAsError(folderCreationError));
			return;
		}
		JobMan.updateStatus(job, Job.MOVING);
		// A.gui.updateJobTable(job);
		A.gui.status0("Moving " + job.currentFile.getName());

		long startTime = System.currentTimeMillis();
		boolean needsCopy = !job.targetFile.exists();
		if (needsCopy && !fileCopy(job.currentFile, job.targetFile)) {
			A.deleteFileAndFolder(job.targetFile, abortedMoveCleanupMessage);
			JobMan.updateStatus(job, Job.MOVEWAIT);
			// A.gui.updateJobTable(job);
			return;
		}

		JobMan.updateStatus(job, Job.MOVECHECK);
		// A.gui.updateJobTable(job);
		A.gui.status0("Checking " + job.currentFile.getName());
		String checksumHex = computeFileChecksum(job.targetFile);
		if (checksumHex == null) { // canceled
			if (needsCopy) {
				A.deleteFileAndFolder(job.targetFile, abortedMoveCleanupMessage);
			}
			if (!A.gui.isDiskIoOk()) {
				JobMan.updateStatus(job, Job.MOVEWAIT);
			}
		} else if (job.ed2kHash.equalsIgnoreCase(checksumHex)) {
			A.gui.println("Moved " + Hyper.formatAsName(job.currentFile) + " to " + Hyper.formatAsName(job.targetFile)
					+ " @ " + formatStats(job.currentFile.length(), (System.currentTimeMillis() - startTime) / 1000f));
			A.deleteFileAndFolder(job.currentFile, successfulMoveCleanupMessage);
			JobMan.setJobFile(job, job.targetFile);
			job.targetFile = null;
			job.directoryId = -1;
			JobMan.updateStatus(job, Job.MOVED);
		} else {
			JobMan.updateStatus(job, Job.FAILED);
			if (needsCopy) {
				job.setError("CRC check failed on copy. HW problem?");
				A.gui.println(Hyper.formatAsError(job.currentFile + " was not moved! CRC check failed. HW problem?"));
			} else {
				job.setError("CRC check failed on copy. Destination file does already exist, but with"
						+ " wrong CRC. Handle this manually.");
				A.gui.println(Hyper.formatAsError(job.currentFile + " was not moved! Destination file '"
						+ job.targetFile + "' does already exist, but with wrong CRC. Handle this" + " manually."));
			}
		}
		// A.gui.updateJobTable(job);
	}

	private String formatStats(long fileSizeBytes, float elapsedTimeSeconds) {
		String speedText = DECIMAL_FORMATTER.format(fileSizeBytes / elapsedTimeSeconds / 1048576);
		String timeText = DECIMAL_FORMATTER.format(elapsedTimeSeconds);
		return Hyper.formatAsNumber(speedText) + " MB/s (" + Hyper.formatAsNumber(fileSizeBytes + "") + " bytes in "
				+ Hyper.formatAsNumber(timeText) + " seconds)";
	}

	private boolean fileCopy(File sourceFile, File destinationFile) throws IOException {
		int bytesRead;
		long totalBytesRead = 0;
		long fileLength = sourceFile.length();
		float progress = 0;

		InputStream inputStream = new FileInputStream(sourceFile);
		OutputStream outputStream = new FileOutputStream(destinationFile);

		A.gui.statusProgressBar.setValue(0);
		try {
			while (A.gui.isDiskIoOk() && (bytesRead = inputStream.read(READ_BUFFER)) != -1) {
				outputStream.write(READ_BUFFER, 0, bytesRead);
				totalBytesRead += bytesRead;
				progress = (float) totalBytesRead / fileLength;
				A.gui.statusProgressBar.setValue((int) (1000 * progress));
			}
			outputStream.close();
			inputStream.close();
		} catch (IOException e) {
			if (e.getMessage().equals(DISK_SPACE_ERROR_MESSAGE)) {
				A.dialog("IOException", DISK_SPACE_ERROR_MESSAGE + ":\n" + destinationFile);
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
		InputStream inputStream = new FileInputStream(file);
		A.gui.statusProgressBar.setValue(0);
		while (A.gui.isDiskIoOk() && (bytesRead = inputStream.read(READ_BUFFER)) != -1) {
			edonkeyHash.update(READ_BUFFER, 0, bytesRead);
			totalBytesRead += bytesRead;
			progress = (float) totalBytesRead / fileLength;
			A.gui.statusProgressBar.setValue((int) (1000 * progress));
		}
		inputStream.close();
		if (progress < 1) {
			return null;
		}
		// return crc.getHexValue();
		return edonkeyHash.getHexValue();
	}
}
