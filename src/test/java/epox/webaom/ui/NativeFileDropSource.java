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

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Separate JVM source so the desktop regression exercises native MIME conversion between processes. */
public final class NativeFileDropSource {
    private NativeFileDropSource() {}

    /** Opens a drag source and prints its screen position for the Robot-driven test. */
    public static void main(String[] args) throws Exception {
        Transferable transferable = uriList("smb://nas.example/Anime/native.mkv\r\n");
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("File source");
            JLabel label = new JLabel("Drag SMB file", SwingConstants.CENTER);
            frame.add(label);
            frame.setBounds(20, 20, 200, 150);
            frame.setVisible(true);
            new DragSource()
                    .createDefaultDragGestureRecognizer(
                            label, DnDConstants.ACTION_COPY, event -> event.startDrag(null, transferable));
            Point point = label.getLocationOnScreen();
            point.translate(label.getWidth() / 2, label.getHeight() / 2);
            System.out.println(point.x + "," + point.y);
            System.out.flush();
        });
    }

    /** A drop payload that offers only {@code text/uri-list}, as Dolphin does for remote files. */
    static Transferable uriList(String text) {
        DataFlavor flavor;
        try {
            flavor = new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {flavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor requested) {
                return flavor.equals(requested);
            }

            @Override
            public Object getTransferData(DataFlavor requested) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(requested)) throw new UnsupportedFlavorException(requested);
                return text;
            }
        };
    }
}
