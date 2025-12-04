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

package epox.swing;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * Fluent builder for creating and configuring JFileChooser dialogs.
 *
 * <p>This builder simplifies the common pattern of setting up file choosers with consistent
 * behavior across an application. It handles:
 *
 * <ul>
 *   <li>Last directory tracking and persistence
 *   <li>File vs directory selection modes
 *   <li>Single vs multi-selection
 *   <li>File filters
 *   <li>Optional-based result handling for null-safety
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FileChooserResult result = FileChooserBuilder.create()
 *     .forFiles()
 *     .multiSelection(true)
 *     .withLastDirectory(lastDirPath)
 *     .showDialog(parentComponent, "Select Files");
 *
 * if (result.isApproved()) {
 *     File[] files = result.getSelectedFiles();
 *     String newLastDir = result.getCurrentDirectory();
 *     // Process files...
 * }
 * }</pre>
 */
public class FileChooserBuilder {
    private final JFileChooser fileChooser;
    private String lastDirectory;
    private boolean trackLastDirectory = true;

    private FileChooserBuilder() {
        this.fileChooser = new JFileChooser();
    }

    /**
     * Creates a new FileChooserBuilder with default settings.
     *
     * @return a new builder instance
     */
    public static FileChooserBuilder create() {
        return new FileChooserBuilder();
    }

    /**
     * Creates a new FileChooserBuilder initialized with a last directory.
     *
     * @param lastDirectory the initial directory to start in, or null
     * @return a new builder instance
     */
    public static FileChooserBuilder createWithLastDirectory(String lastDirectory) {
        FileChooserBuilder builder = new FileChooserBuilder();
        return builder.withLastDirectory(lastDirectory);
    }

    /**
     * Configures the chooser for file selection (default mode).
     *
     * @return this builder for method chaining
     */
    public FileChooserBuilder forFiles() {
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return this;
    }

    /**
     * Configures the chooser for directory selection.
     *
     * @return this builder for method chaining
     */
    public FileChooserBuilder forDirectories() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return this;
    }

    /**
     * Configures the chooser for both files and directories.
     *
     * @return this builder for method chaining
     */
    public FileChooserBuilder forFilesAndDirectories() {
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return this;
    }

    /**
     * Enables or disables multi-selection mode.
     *
     * @param enabled true to allow multiple files/directories to be selected
     * @return this builder for method chaining
     */
    public FileChooserBuilder multiSelection(boolean enabled) {
        fileChooser.setMultiSelectionEnabled(enabled);
        return this;
    }

    /**
     * Sets a file filter for the chooser.
     *
     * @param filter the filter to apply, or null for no filtering
     * @return this builder for method chaining
     */
    public FileChooserBuilder withFilter(FileFilter filter) {
        if (filter != null) {
            fileChooser.setFileFilter(filter);
        }
        return this;
    }

    /**
     * Sets the last directory for the chooser to start in.
     *
     * @param lastDirectory the directory path, or null
     * @return this builder for method chaining
     */
    public FileChooserBuilder withLastDirectory(String lastDirectory) {
        this.lastDirectory = lastDirectory;
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(new File(lastDirectory));
        }
        return this;
    }

    /**
     * Sets the last directory using a Path object.
     *
     * @param lastDirectory the directory path, or null
     * @return this builder for method chaining
     */
    public FileChooserBuilder withLastDirectory(Path lastDirectory) {
        if (lastDirectory != null) {
            return withLastDirectory(lastDirectory.toString());
        }
        return this;
    }

    /**
     * Controls whether the builder should track and update the last directory on approval.
     *
     * @param track true to track last directory (default), false otherwise
     * @return this builder for method chaining
     */
    public FileChooserBuilder trackLastDirectory(boolean track) {
        this.trackLastDirectory = track;
        return this;
    }

    /**
     * Shows a custom dialog with the specified button text.
     *
     * @param parent the parent component for the dialog
     * @param approveButtonText the text for the approval button
     * @return a FileChooserResult containing the selection result
     */
    public FileChooserResult showDialog(Component parent, String approveButtonText) {
        int option = fileChooser.showDialog(parent, approveButtonText);
        return buildResult(option);
    }

    /**
     * Shows a standard "Open" dialog.
     *
     * @param parent the parent component for the dialog
     * @return a FileChooserResult containing the selection result
     */
    public FileChooserResult showOpenDialog(Component parent) {
        int option = fileChooser.showOpenDialog(parent);
        return buildResult(option);
    }

    /**
     * Shows a standard "Save" dialog.
     *
     * @param parent the parent component for the dialog
     * @return a FileChooserResult containing the selection result
     */
    public FileChooserResult showSaveDialog(Component parent) {
        int option = fileChooser.showSaveDialog(parent);
        return buildResult(option);
    }

    private FileChooserResult buildResult(int option) {
        boolean approved = (option == JFileChooser.APPROVE_OPTION);
        File selectedFile = approved ? fileChooser.getSelectedFile() : null;
        File[] selectedFiles = approved ? fileChooser.getSelectedFiles() : new File[0];

        // Update last directory only on approval
        String currentDir = null;
        if (trackLastDirectory && fileChooser.getCurrentDirectory() != null) {
            currentDir = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (approved) {
                this.lastDirectory = currentDir;
            }
        }

        return new FileChooserResult(approved, selectedFile, selectedFiles, currentDir);
    }

    /**
     * Result of a file chooser dialog operation.
     *
     * <p>Provides both traditional null-check APIs and Optional-based APIs for null-safety.
     */
    public static class FileChooserResult {
        private final boolean approved;
        private final File selectedFile;
        private final File[] selectedFiles;
        private final String currentDirectory;

        private FileChooserResult(boolean approved, File selectedFile, File[] selectedFiles, String currentDirectory) {
            this.approved = approved;
            this.selectedFile = selectedFile;
            this.selectedFiles = selectedFiles;
            this.currentDirectory = currentDirectory;
        }

        /**
         * Returns whether the user approved the selection (clicked OK/Open/Save).
         *
         * @return true if approved, false if cancelled
         */
        public boolean isApproved() {
            return approved;
        }

        /**
         * Returns whether the user cancelled the dialog.
         *
         * @return true if cancelled, false if approved
         */
        public boolean isCancelled() {
            return !approved;
        }

        /**
         * Returns the selected file (for single-selection mode).
         *
         * @return the selected file, or null if none selected
         */
        public File getSelectedFile() {
            return selectedFile;
        }

        /**
         * Returns the selected file as an Optional (for single-selection mode).
         *
         * @return Optional containing the selected file, or empty if none
         */
        public Optional<File> getSelectedFileOptional() {
            return Optional.ofNullable(selectedFile);
        }

        /**
         * Returns the selected file as a Path (for single-selection mode).
         *
         * @return the selected file path, or null if none selected
         */
        public Path getSelectedPath() {
            return selectedFile != null ? selectedFile.toPath() : null;
        }

        /**
         * Returns the selected file as an Optional Path (for single-selection mode).
         *
         * @return Optional containing the selected file path, or empty if none
         */
        public Optional<Path> getSelectedPathOptional() {
            return Optional.ofNullable(selectedFile).map(File::toPath);
        }

        /**
         * Returns all selected files (for multi-selection mode).
         *
         * @return array of selected files, empty array if none selected
         */
        public File[] getSelectedFiles() {
            return selectedFiles;
        }

        /**
         * Returns all selected files as Paths.
         *
         * @return array of selected file paths
         */
        public Path[] getSelectedPaths() {
            return Arrays.stream(selectedFiles).map(File::toPath).toArray(Path[]::new);
        }

        /**
         * Returns the current directory of the file chooser.
         *
         * @return the current directory path, or null if not tracked
         */
        public String getCurrentDirectory() {
            return currentDirectory;
        }

        /**
         * Returns the current directory as an Optional.
         *
         * @return Optional containing the current directory, or empty if not tracked
         */
        public Optional<String> getCurrentDirectoryOptional() {
            return Optional.ofNullable(currentDirectory);
        }

        /**
         * Returns the current directory as a Path.
         *
         * @return the current directory path, or null if not tracked
         */
        public Path getCurrentDirectoryPath() {
            return currentDirectory != null ? Paths.get(currentDirectory) : null;
        }

        /**
         * Returns the current directory as an Optional Path.
         *
         * @return Optional containing the current directory path, or empty if not tracked
         */
        public Optional<Path> getCurrentDirectoryPathOptional() {
            return Optional.ofNullable(currentDirectory).map(Paths::get);
        }
    }
}
