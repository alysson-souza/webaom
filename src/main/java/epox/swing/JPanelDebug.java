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
 * @version 	1.07
 * @author 		epoximator
 */

package epox.swing;

import epox.util.StringUtilities;
import epox.webaom.AppContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Debug panel that captures System.out and System.err, displaying output in a scrollable text area. Optionally logs
 * to a file.
 */
public class JPanelDebug extends JPanel {
    private static final DecimalFormat nf = new DecimalFormat("000.00");
    protected JTextArea textArea;
    protected JScrollBar scrollBar;
    protected boolean logToFile = false;
    protected transient Updater updater;
    private transient PrintStream originalErr;
    private transient PrintStream originalOut;
    private transient PrintStream logFileStream;

    public JPanelDebug(String file, boolean captureOut, boolean captureErr, boolean echoOut, boolean echoErr) {
        super(new java.awt.BorderLayout());
        textArea = new JTextArea();
        textArea.setMargin(new java.awt.Insets(2, 2, 2, 2));
        textArea.append("Please report bugs at https://github.com/alysson-souza/webaom - Version:" + AppContext.VERSION
                + "\r\n");
        JScrollPane scroll = new JScrollPane(textArea);
        scrollBar = scroll.getVerticalScrollBar();
        add(scroll);
        try {
            if (file != null) {
                logToFile = true;
                logFileStream = new PrintStream(new FileOutputStream(new File(file)), true);
            }

            // Intentionally capturing System.out/err - this debug panel's purpose is to intercept
            // standard streams and display them in a GUI text area while optionally echoing to original
            originalErr = System.err; // NOSONAR - intentional capture of stderr
            originalOut = System.out; // NOSONAR - intentional capture of stdout

            WinStream errStream = new WinStream(echoErr, originalErr);
            WinStream outStream = new WinStream(echoOut, originalOut);

            if (captureErr) {
                System.setErr(errStream);
            }
            if (captureOut) {
                System.setOut(outStream);
            }
        } catch (FileNotFoundException ex) {
            textArea.append("Could not openfile" + file);
        }
        updater = new Updater();
    }

    public JPanelDebug(String file) {
        this(file, true, true, false, true);
    }

    protected class Updater implements Runnable {
        @Override
        public void run() {
            if (!scrollBar.getValueIsAdjusting()) {
                scrollBar.setValue(scrollBar.getMaximum());
            }
        }
    }

    private class WinStream extends PrintStream {
        PrintStream echoStream;
        boolean echoEnabled;
        boolean isNewLine = true;
        long startTime;
        long currentTime;

        public WinStream(boolean echoEnabled, PrintStream echoStream) {
            super(echoStream);
            this.echoStream = echoStream;
            this.echoEnabled = echoEnabled;
            this.startTime = System.currentTimeMillis();
        }

        private synchronized void appendToTextArea(String text) {
            currentTime = System.currentTimeMillis();
            if ("\n".equals(text)) {
                handleNewline();
            } else {
                handleTextWithPrefix(text);
            }
            scheduleScrollUpdate();
        }

        private void handleNewline() {
            if (!isNewLine) {
                textArea.append("\n");
            }
            isNewLine = true;
            startTime = currentTime;
        }

        private void handleTextWithPrefix(String text) {
            String prefix = "[" + StringUtilities.time() + "|" + nf.format((float) (currentTime - startTime) / 1000)
                    + "] " + Thread.currentThread().getName() + ": ";

            if (text.indexOf('\n') < 0) {
                appendSingleLine(prefix, text);
            } else {
                appendMultipleLines(prefix, text);
            }
        }

        private void appendSingleLine(String prefix, String text) {
            if (isNewLine) {
                textArea.append(prefix);
            }
            textArea.append(text);
            isNewLine = false;
        }

        private void appendMultipleLines(String prefix, String text) {
            String[] lines = StringUtilities.split(text, '\n');
            for (String line : lines) {
                if (isNewLine) {
                    textArea.append(prefix);
                }
                textArea.append(line + "\n");
                isNewLine = true;
            }
            startTime = currentTime;
        }

        private void scheduleScrollUpdate() {
            if (textArea.isVisible()) {
                javax.swing.SwingUtilities.invokeLater(updater);
            }
        }

        @Override
        public void print(boolean value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(value);
            }
            appendToTextArea(value + "");
        }

        @Override
        public void print(char value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(value);
            }
            appendToTextArea(value + "");
        }

        @Override
        public void print(int value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(value);
            }
            appendToTextArea(value + "");
        }

        @Override
        public void print(long value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(value);
            }
            appendToTextArea(value + "");
        }

        @Override
        public void print(float value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(value);
            }
            appendToTextArea(value + "");
        }

        @Override
        public void print(double value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(value);
            }
            appendToTextArea(value + "");
        }

        @Override
        public void print(char[] value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(new String(value));
            }
            appendToTextArea(new String(value));
        }

        @Override
        public void print(String text) {
            if (text == null) {
                text = "null";
            }
            if (echoEnabled) {
                echoStream.print(text);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(text);
            }
            if (text.endsWith("\n")) {
                isNewLine = true;
            }
            appendToTextArea(text);
        }

        @Override
        public void print(Object value) {
            if (echoEnabled) {
                echoStream.print(value);
            }
            if (logToFile && logFileStream != null) {
                logFileStream.print(value.toString());
            }
            appendToTextArea("" + value);
        }

        @Override
        public void println() {
            if (echoEnabled) {
                echoStream.println();
            }
            if (logToFile && logFileStream != null) {
                logFileStream.println();
            }
            appendToTextArea("\n");
        }

        @Override
        public void println(boolean value) {
            print(value);
            println();
        }

        @Override
        public void println(char value) {
            print(value);
            println();
        }

        @Override
        public void println(int value) {
            print(value);
            println();
        }

        @Override
        public void println(long value) {
            print(value);
            println();
        }

        @Override
        public void println(float value) {
            print(value);
            println();
        }

        @Override
        public void println(double value) {
            print(value);
            println();
        }

        @Override
        public void println(char[] value) {
            print(value);
            println();
        }

        @Override
        public void println(String text) {
            print(text);
            println();
        }

        @Override
        public void println(Object value) {
            print(value);
            println();
        }
    }
}
