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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public final class JobManager {
    private JobManager() {}

    public static void setPath(Job job, String path, boolean includeParent) {
        synchronized (job) {
            if (job.check(Job.S_DOING)) {
                return;
            }
            int newStatus = Job.FAILED;
            File destinationFolder = new File(path);
            File originalFile = job.targetFile != null ? job.targetFile : job.currentFile;
            String fileName = "";
            if (includeParent) {
                fileName += originalFile.getParentFile().getName() + File.separatorChar;
            }
            fileName += originalFile.getName();
            File newFile = new File(destinationFolder + File.separator + fileName);
            if (job.getHealth() == Job.H_MISSING) {
                job.find(newFile);
                AppContext.databaseManager.update(0, job, DatabaseManager.INDEX_JOB);
            } else if (updatePath(job, newFile)) {
                if (job.targetFile != null) {
                    newStatus = Job.MOVEWAIT;
                } else {
                    newStatus = Job.FINISHED;
                }
                job.setStatus(newStatus, true);
                AppContext.databaseManager.update(0, job, DatabaseManager.INDEX_JOB);
            }
        }
    }

    public static void setName(Job job, String newName) {
        synchronized (job) {
            if (job.check(Job.S_DOING)) {
                return;
            }
            if (job.getHealth() < Job.H_DELETED
                    && job.getStatus() == Job.FINISHED
                    && newName != null
                    && !newName.isEmpty()) {
                File renamedFile = new File(job.currentFile.getParent() + File.separatorChar + newName);
                if (job.currentFile.renameTo(renamedFile)) {
                    JobManager.setJobFile(job, renamedFile);
                }
                updateStatus(job, Job.FINISHED);
            }
        }
    }

    public static void restoreName(Job job) {
        setName(job, job.originalName);
    }

    public static void openInDefaultPlayer(Job job) {
        if (!job.getFile().exists()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(job.getFile());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void openInExplorer(Job job) {
        if (!job.getFile().exists()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(job.getFile().getParentFile());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void runAvdump(Job job) {
        if (!job.getFile().exists()) {
            return;
        }
        try {
            new ProcessBuilder(
                            "cmd", "/C", "start", "avdump", "-ps", job.getFile().getAbsolutePath())
                    .start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateStatus(Job job, int status) {
        updateStatus(job, status, false);
    }

    public static void updateStatus(Job job, int status, boolean checkIfBusy) {
        synchronized (job) {
            if (checkIfBusy && job.check(Job.S_DOING)) {
                return;
            }
            if ((status & Job.M_H) > 0) {
                job.updateHealth(status);
                return;
            }
            if (job.isLocked(status)) { // we don't wanna mess with all types
                return;
            }
            int newStatus = -1;
            switch (status) {
                case Job.HASHED:
                    if (job.ed2kHash == null) {
                        return;
                    }
                    if (job.anidbFile == null) {
                        newStatus = Job.IDENTWAIT;
                    } else {
                        newStatus = Job.FINISHED;
                    }
                    if (job.isFresh) {
                        if (AppContext.databaseManager.isConnected()
                                && !AppContext.databaseManager.update(
                                        0, job, DatabaseManager.INDEX_JOB)) { // add job to db, if it exist...
                            newStatus = AppContext.databaseManager.getJob(job, true); // then retrieve
                            if (newStatus >= 0) { // if job exist in db... (should be true, always)
                                AppContext.cache.gatherInfo(job, true); // retrieve info (like anime, group, etc.)
                                // File old = job.nfile; //copy the original location (little abuse
                                // of nfile)
                                job.targetFile = null;
                                // if(newStatus==Job.FINISHED&&old!=null&&old.exists()){ //presume old
                                // location is right
                                //	newStatus = Job.MOVEWAIT; //move (or rather check and delete)
                                //	job.nfile = old; //move function use nfile for this
                                // }else{ //the original file was moved, or the job never finished
                                if (updatePath(job)) { // apply rules
                                    if (job.targetFile != null) {
                                        newStatus = Job.MOVEWAIT; // we want to move to a new
                                    }
                                    // location, is it the same as
                                    // the orig?
                                    else {
                                        newStatus = Job.FINISHED;
                                    }
                                } else {
                                    newStatus = Job.FAILED;
                                }
                                // }
                                // old = null;
                            } else {
                                newStatus = Job.FAILED;
                                job.setError("Database error. See log/debug.");
                            }
                        }
                        job.isFresh = false;
                    }
                    break;
                case Job.IDENTIFIED:
                    if (job.anidbFile == null || job.anidbFile.fileId == 0 || !updatePath(job)) {
                        newStatus = Job.FAILED;
                    } else if (AppContext.autoadd && job.mylistId == 0) {
                        newStatus = Job.ADDWAIT;
                    } else if (job.targetFile != null) {
                        newStatus = Job.MOVEWAIT;
                    } else {
                        newStatus = Job.FINISHED;
                    }
                    break;
                case Job.ADDED:
                    if (job.anidbFile == null || job.anidbFile.fileId == 0) {
                        return;
                    }
                    if (job.targetFile != null) {
                        newStatus = Job.MOVEWAIT;
                    } else {
                        newStatus = Job.FINISHED;
                    }
                    // A.db.addMylistEntry(this);
                    break;
                case Job.MOVED:
                    newStatus = Job.FINISHED;
                    break;
                case Job.FAILED:
                    newStatus = Job.FAILED;
                    break;
                case Job.ADDWAIT:
                    if (job.anidbFile == null) {
                        newStatus = Job.FAILED;
                        job.setError("Can't add unknown file to mylist.");
                        break;
                    }
                case Job.FINISHED:
                    // if(job.afl==null||job.afl.fid==0) return; //is it ok to disable this?
                    job.targetFile = null;
                    newStatus = status;
                    break;
                default:
                    newStatus = status;
            }
            job.setStatus(newStatus, true);
            if (!job.isFresh && job.check(Job.F_DB)) {
                AppContext.databaseManager.update(0, job, DatabaseManager.INDEX_JOB);
            }
        }
    }

    public static boolean updatePath(Job job) {
        if (!AppContext.opt.getBoolean(Options.BOOL_AUTO_RENAME)) {
            return true; // Skip automatic renaming
        }
        if (job.incompl()) {
            job.setError("Extensive fileinfo not available.");
            AppContext.gui.println(job.currentFile + " cannot be renamed: Extensive fileinfo not available.");
            return false;
        }
        return updatePath(job, AppContext.rules.apply(job));
    }

    public static boolean updatePath(Job job, File destinationFile) { // only want to use renameTo if same partition
        job.targetFile = null;
        if (destinationFile == null) {
            return true;
        }
        // EQUAL
        boolean isNormalMove = true;
        if (job.currentFile.equals(destinationFile)) {
            if (job.currentFile.getName().equals(destinationFile.getName())) // win case sens
            {
                return true;
            }
            isNormalMove = false;
        }
        String sourceDisplayName = HyperlinkBuilder.formatAsName(job.currentFile);
        String destDisplayName = HyperlinkBuilder.formatAsName(destinationFile);
        // SOURCE FILE HEALTHY?
        if (!job.currentFile.exists()) {
            job.setError("File does not exist.");
            AppContext.gui.println(sourceDisplayName + " cannot be moved. File not found.");
            return false;
        }
        // DESTINATION FILE HEALTHY?
        if (isNormalMove && destinationFile.exists()) {
            if (job.currentFile.length() == destinationFile.length()) { // could be the same
                job.targetFile = destinationFile;
                AppContext.gui.println(sourceDisplayName + " will be moved to " + destDisplayName + " later.");
                return true;
            }
            job.setError("File cannot be moved. Destination file already exists!");
            AppContext.gui.println(sourceDisplayName + " cannot be moved to " + destDisplayName
                    + ": Destination file already exists!");
            return false;
        }
        // DESTINATION FOLDER OK?
        File parentFolder = destinationFile.getParentFile();
        if (!parentFolder.exists() && !parentFolder.mkdirs()) {
            job.setError("Folder " + parentFolder + " cannot be created!");
            AppContext.gui.println("Folder " + parentFolder + " cannot be created!");
            return false;
        }
        AppContext.jobs.addPath(destinationFile);
        job.directoryId = -1;
        // TRY TO MOVE: WINDOWS
        String sourcePath = job.currentFile.getAbsolutePath().toLowerCase();
        String destPath = destinationFile.getAbsolutePath().toLowerCase();
        String cleanupMessage = "Cleanup after successful rename operation.";
        if (sourcePath.charAt(1) == ':' || destPath.charAt(1) == ':') { // windows (not network)
            if (sourcePath.charAt(0) == destPath.charAt(0)) {
                if (job.currentFile.renameTo(destinationFile)) {
                    moveSubtitleFiles(job.currentFile, destinationFile);
                    AppContext.deleteFile(job.currentFile.getParentFile(), cleanupMessage);
                    JobManager.setJobFile(job, destinationFile);
                    AppContext.gui.println("Renamed " + sourceDisplayName + " to " + destDisplayName);
                    return true;
                }
                AppContext.gui.println(HyperlinkBuilder.formatAsError("Renaming failed!") + " (" + sourceDisplayName
                        + " to " + destDisplayName + ")");
                return false;
            }
            job.targetFile = destinationFile;
            AppContext.gui.println(sourceDisplayName + " will be moved to " + destDisplayName + " later.");
            return true;
        }
        // TRY TO MOVE: *NIX
        if (job.currentFile.renameTo(destinationFile)) { // linux can't rename over partitions
            AppContext.deleteFile(job.currentFile.getParentFile(), cleanupMessage);
            JobManager.setJobFile(job, destinationFile);
            AppContext.gui.println("Renamed" + sourceDisplayName + " to " + destDisplayName);
            return true;
        }
        job.targetFile = destinationFile;
        AppContext.gui.println(sourceDisplayName + " will be moved to " + destDisplayName + " later.");
        return true;
        // THE END
    }

    public static void setJobFile(Job job, File file) {
        if (Cache.treeSortMode == Cache.MODE_ANIME_FOLDER_FILE) {
            AppContext.cache.treeRemove(job);
            job.currentFile = file;
            AppContext.cache.treeAdd(job);
        } else {
            job.currentFile = file;
        }
    }

    public static void showInfo(Job job) {
        AppContext.dialog2(job.currentFile.getName(), job.convert(AppContext.fschema));
    }

    private static void moveSubtitleFiles(File sourceFile, File destinationFile) {
        moveSubtitleFile(sourceFile, destinationFile, "ass");
        moveSubtitleFile(sourceFile, destinationFile, "idx");
        moveSubtitleFile(sourceFile, destinationFile, "pdf");
        moveSubtitleFile(sourceFile, destinationFile, "sbv");
        moveSubtitleFile(sourceFile, destinationFile, "smi");
        moveSubtitleFile(sourceFile, destinationFile, "srt");
        moveSubtitleFile(sourceFile, destinationFile, "ssa");
        moveSubtitleFile(sourceFile, destinationFile, "sub");
        moveSubtitleFile(sourceFile, destinationFile, "vtt");
    }

    private static void moveSubtitleFile(File sourceFile, File destinationFile, String extension) {
        File subtitleFile = new File(changeExtension(sourceFile, extension));
        if (subtitleFile.exists()) {
            subtitleFile.renameTo(new File(changeExtension(destinationFile, extension)));
        }
    }

    private static String changeExtension(File file, String extension) {
        String path = file.getAbsolutePath();
        return path.substring(0, 1 + path.lastIndexOf('.')) + extension;
    }
}
