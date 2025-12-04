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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import javax.swing.filechooser.FileFilter;

/**
 * File filter that accepts files based on their extensions.
 *
 * <p>This filter always accepts directories (to allow navigation in file choosers) and accepts
 * files if either:
 *
 * <ul>
 *   <li>No extensions are configured (accepts all files)
 *   <li>The file's extension matches one of the configured extensions
 * </ul>
 *
 * <p>Extension matching is case-insensitive.
 *
 * <p>Implements both javax.swing.filechooser.FileFilter (for JFileChooser) and
 * java.io.FileFilter (for File.listFiles()).
 */
public class ExtensionFileFilter extends FileFilter implements java.io.FileFilter {

    private final Set<String> extensions;
    private final String description;
    private final Predicate<String> extensionTest;

    /**
     * Creates a filter with specific allowed extensions.
     *
     * @param description human-readable description of this filter
     * @param extensions allowed file extensions (without dots, e.g., "txt", "avi")
     */
    public ExtensionFileFilter(String description, String... extensions) {
        this.description = description;
        this.extensions = new HashSet<>();
        for (String ext : extensions) {
            this.extensions.add(ext.toLowerCase());
        }

        // Create predicate for extension testing
        if (this.extensions.isEmpty()) {
            // Accept all files if no extensions specified
            this.extensionTest = ext -> true;
        } else {
            // Accept only if extension matches
            this.extensionTest = ext -> ext != null && this.extensions.contains(ext.toLowerCase());
        }
    }

    /**
     * Creates a filter with extensions from a collection.
     *
     * @param description human-readable description of this filter
     * @param extensions collection of allowed file extensions
     */
    public ExtensionFileFilter(String description, Iterable<String> extensions) {
        this.description = description;
        this.extensions = new HashSet<>();
        for (String ext : extensions) {
            this.extensions.add(ext.toLowerCase());
        }

        // Create predicate for extension testing
        if (this.extensions.isEmpty()) {
            this.extensionTest = ext -> true;
        } else {
            this.extensionTest = ext -> ext != null && this.extensions.contains(ext.toLowerCase());
        }
    }

    /**
     * Creates a filter that accepts all files (no extension filtering).
     *
     * @param description human-readable description of this filter
     */
    public ExtensionFileFilter(String description) {
        this.description = description;
        this.extensions = new HashSet<>();
        this.extensionTest = ext -> true;
    }

    @Override
    public boolean accept(File file) {
        // Always accept directories (needed for navigation)
        if (file.isDirectory()) {
            return true;
        }

        // Test file extension
        String ext = getExtension(file);
        return extensionTest.test(ext);
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Extracts the file extension from a file.
     *
     * @param file the file to extract extension from
     * @return the extension (without dot, lowercase), or null if no extension
     */
    private String getExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == name.length() - 1) {
            return null;
        }
        return name.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Returns the set of allowed extensions.
     *
     * @return unmodifiable view of allowed extensions
     */
    public Set<String> getExtensions() {
        return new HashSet<>(extensions);
    }

    /**
     * Checks if this filter has any configured extensions.
     *
     * @return true if no extensions configured (accepts all files), false otherwise
     */
    public boolean acceptsAllExtensions() {
        return extensions.isEmpty();
    }

    /**
     * Returns a formatted string of all allowed extensions.
     *
     * @return comma-separated list of extensions, or "all files" if none configured
     */
    public String getExtensionsString() {
        if (extensions.isEmpty()) {
            return "all files";
        }
        return String.join(", ", extensions);
    }

    @Override
    public String toString() {
        return description + " (" + getExtensionsString() + ")";
    }
}
