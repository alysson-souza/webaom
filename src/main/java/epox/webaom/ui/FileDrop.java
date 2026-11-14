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

package epox.webaom.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Reads file drops while the native drag-and-drop transfer is still available. */
final class FileDrop {
    record Location(File file, URI uri) {}

    private FileDrop() {}

    static boolean supports(Transferable transfer) {
        return uriFlavor(transfer) != null || transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    static List<Location> read(Transferable transfer) throws IOException, UnsupportedFlavorException {
        boolean hasFileList = transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        DataFlavor flavor = uriFlavor(transfer);
        // Java's native file-list conversion can discard remote URLs. Read the original URI list first.
        List<Location> locations = flavor == null ? List.of() : readUris(transfer, flavor);
        if (locations == null) {
            if (!hasFileList) throw new IOException("The drop contains an invalid file URL.");
            locations = List.of();
        }
        // macOS fills the URI list of a Finder drop with plain paths, so only its file list is usable.
        if (locations.isEmpty() && hasFileList) locations = readFileList(transfer);
        if (locations.isEmpty()) throw new IOException("The drop contains no files.");
        return List.copyOf(locations);
    }

    private static List<Location> readFileList(Transferable transfer) throws IOException, UnsupportedFlavorException {
        Object data = transfer.getTransferData(DataFlavor.javaFileListFlavor);
        if (!(data instanceof List<?> files)) throw new IOException("The drop is not a file list.");
        List<Location> locations = new ArrayList<>();
        for (Object file : files) {
            if (!(file instanceof File local)) throw new IOException("The drop is not a file list.");
            locations.add(new Location(local, null));
        }
        return locations;
    }

    /** Returns the listed URLs, or null when the text is not a list of absolute URLs. */
    private static List<Location> readUris(Transferable transfer, DataFlavor flavor)
            throws IOException, UnsupportedFlavorException {
        List<Location> locations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(flavor.getReaderForText(transfer))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                URI uri = URI.create(line);
                if (!uri.isAbsolute()) return null;
                locations.add(new Location(null, uri));
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return locations;
    }

    private static DataFlavor uriFlavor(Transferable transfer) {
        DataFlavor[] flavors = transfer.getTransferDataFlavors();
        if (flavors != null) {
            for (DataFlavor flavor : flavors) {
                if (flavor.isMimeTypeEqual("text/uri-list") && flavor.isFlavorTextType()) return flavor;
            }
        }
        return null;
    }
}
