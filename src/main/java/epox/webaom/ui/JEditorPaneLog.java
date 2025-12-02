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

import epox.swing.Log;
import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.HyperlinkBuilder;
import epox.webaom.util.PlatformPaths;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

public class JEditorPaneLog extends JEditorPane implements Log, Action {
    public static String htmlHeader = "<style type=\"text/css\">" + "body{background-color:#FFFFFF;"
            + "font-family:Verdana,Arial,Helvetica,sans-serif;font-size:11pt;}" + "</style>";
    private PrintStream logOutputStream;

    public JEditorPaneLog() {
        super("text/html", htmlHeader);
        setEditable(false);
        getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "remove");
        getActionMap().put("remove", this);
    }

    public synchronized void append(String htmlText) {
        try {
            Document document = getDocument();
            int documentLength = document.getLength();
            if (htmlText == null || htmlText.equals("")) {
                return;
            }
            Reader htmlReader = new StringReader(htmlText);
            EditorKit editorKit = getEditorKit();
            editorKit.read(htmlReader, document, documentLength);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void println(Object message) {
        String formattedTime = HyperlinkBuilder.formatAsNumber(StringUtilities.time());
        StringBuffer logLine = new StringBuffer(256);
        logLine.append('[');
        logLine.append(formattedTime);
        logLine.append("] ");
        logLine.append(message);
        logLine.append("<br>\n");
        append(logLine.toString());

        try {
            logOutputStream.print(logLine);
        } catch (NullPointerException ignored) {
            // Log file not yet opened, ignore
        }
    }

    public boolean openLogFile(String filePath) {
        try {
            // Use default log path if the provided path is empty or null
            if (filePath == null || filePath.trim().isEmpty()) {
                filePath = PlatformPaths.getDefaultLogFilePath();
            }

            // Ensure the parent directory exists
            if (!PlatformPaths.ensureParentDirectoryExists(filePath)) {
                String errorMessage = "Failed to create log directory for: " + filePath;
                AppContext.dialog("Log Error", errorMessage);
                return false;
            }

            logOutputStream = new PrintStream(new AppendFileStream(filePath), true, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ioException) {
            AppContext.dialog("Log Error", ioException.getMessage());
            return false;
        }
    }

    public void closeLogFile() {
        try {
            logOutputStream.close();
        } catch (Exception ignored) {
            // Stream may already be closed or null
        }
    }

    @Override
    public void status0(String msg) {
        // don't care
    }

    @Override
    public void status1(String msg) {
        // don't care
    }

    @Override
    public Object getValue(String key) {
        return null;
    }

    @Override
    public void putValue(String key, Object value) {
        // don't care
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        setHeader(javax.swing.JOptionPane.showInputDialog(AppContext.frame, "Edit header", htmlHeader));
    }

    public void setHeader(String newHeader) {
        if (newHeader == null || newHeader.isEmpty()) {
            return;
        }
        synchronized (this) {
            htmlHeader = newHeader;
            setText(htmlHeader);
            Runtime.getRuntime().gc();
        }
    }

    private class AppendFileStream extends OutputStream {
        RandomAccessFile randomAccessFile;

        AppendFileStream(String filePath) throws IOException {
            randomAccessFile = new RandomAccessFile(filePath, "rw");
            randomAccessFile.seek(randomAccessFile.length());
        }

        @Override
        public void close() throws IOException {
            randomAccessFile.close();
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            randomAccessFile.write(bytes);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            randomAccessFile.write(bytes, offset, length);
        }

        @Override
        public void write(int byteValue) throws IOException {
            randomAccessFile.write(byteValue);
        }
    }
}
