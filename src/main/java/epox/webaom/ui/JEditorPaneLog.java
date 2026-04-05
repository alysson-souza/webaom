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
import epox.swing.ThemeColorSupport;
import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.HyperlinkBuilder;
import epox.webaom.util.PlatformPaths;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import javax.swing.JEditorPane;
import javax.swing.UIManager;

public class JEditorPaneLog extends JEditorPane implements Log {
    private static final String MONOSPACE_FONT_FAMILY = "Monospaced,Consolas,Menlo,Monaco,'Liberation Mono',monospace";
    private static final String BODY_MARGIN = "2px";
    private final StringBuilder logBodyHtml = new StringBuilder();
    private PrintStream logOutputStream;
    private String renderedHtml = "";

    public JEditorPaneLog() {
        setContentType("text/html");
        setEditable(false);
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        applyMonospaceFont();
        refreshDocument();
    }

    public synchronized void append(String htmlText) {
        if (htmlText == null || htmlText.isEmpty()) {
            return;
        }
        logBodyHtml.append(htmlText);
        refreshDocument();
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
    public void updateUI() {
        super.updateUI();
        applyMonospaceFont();
        refreshDocument();
    }

    synchronized String getRenderedHtml() {
        return renderedHtml;
    }

    private void applyMonospaceFont() {
        Font defaultEditorFont = UIManager.getFont("EditorPane.font");
        Font currentFont = getFont();
        int fallbackSize = currentFont == null ? 12 : currentFont.getSize();
        int fontSize = defaultEditorFont == null ? fallbackSize : defaultEditorFont.getSize();
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
    }

    private synchronized void refreshDocument() {
        if (!"text/html".equalsIgnoreCase(getContentType())) {
            return;
        }

        renderedHtml = buildHtmlDocument(logBodyHtml.toString());
        setText(renderedHtml);
        setCaretPosition(getDocument().getLength());
    }

    private String buildHtmlDocument(String bodyContent) {
        Color foreground = ThemeColorSupport.colorOrDefault(
                getForeground(),
                Color.black,
                "EditorPane.foreground",
                "TextPane.foreground",
                "TextArea.foreground",
                "Label.foreground");
        Color background = ThemeColorSupport.colorOrDefault(
                getBackground(),
                Color.white,
                "EditorPane.background",
                "TextPane.background",
                "TextArea.background",
                "Panel.background");
        Color linkColor = HyperlinkBuilder.resolveNameColor();

        StringBuilder html = new StringBuilder(bodyContent.length() + 256);
        html.append("<html><head><style type=\"text/css\">");
        html.append("body{margin:");
        html.append(BODY_MARGIN);
        html.append(";font-family:");
        html.append(MONOSPACE_FONT_FAMILY);
        html.append(";font-size:");
        html.append(getFont().getSize());
        html.append("pt;color:#");
        html.append(ThemeColorSupport.toHex(foreground));
        html.append(";background-color:#");
        html.append(ThemeColorSupport.toHex(background));
        html.append(";}");
        html.append(".");
        html.append(HyperlinkBuilder.CSS_CLASS_WARNING);
        html.append("{color:#");
        html.append(ThemeColorSupport.toHex(HyperlinkBuilder.resolveWarningColor()));
        html.append(";}");
        html.append(".");
        html.append(HyperlinkBuilder.CSS_CLASS_NAME);
        html.append("{color:#");
        html.append(ThemeColorSupport.toHex(HyperlinkBuilder.resolveNameColor()));
        html.append(";}");
        html.append(".");
        html.append(HyperlinkBuilder.CSS_CLASS_NUMBER);
        html.append("{color:#");
        html.append(ThemeColorSupport.toHex(HyperlinkBuilder.resolveNumberColor()));
        html.append(";}");
        html.append("a{color:#");
        html.append(ThemeColorSupport.toHex(linkColor));
        html.append(";}");
        html.append("</style></head><body>");
        html.append(bodyContent);
        html.append("</body></html>");
        return html.toString();
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
