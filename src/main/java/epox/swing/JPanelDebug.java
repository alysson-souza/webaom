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

import epox.util.U;
import epox.webaom.A;
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
	public static DecimalFormat nf = new DecimalFormat("000.00");
	private PrintStream originalErr;
	private PrintStream originalOut;
	private File logFile;
	protected JTextArea textArea;
	protected JScrollBar scrollBar;
	protected boolean logToFile = false;
	protected Updater updater;

	public JPanelDebug(String file, boolean captureOut, boolean captureErr, boolean echoOut, boolean echoErr) {
		super(new java.awt.BorderLayout());
		textArea = new JTextArea();
		textArea.setMargin(new java.awt.Insets(2, 2, 2, 2));
		textArea.append("Please report bugs at https://github.com/alysson-souza/webaom - Version:" + A.S_VER + "\r\n");
		JScrollPane scroll = new JScrollPane(textArea);
		scrollBar = scroll.getVerticalScrollBar();
		add(scroll);
		try {
			FileOutputStream outputStream = null;
			if (file != null) {
				logToFile = true;
				logFile = new File(file);
				outputStream = new FileOutputStream(logFile);
			}

			originalErr = System.err;
			originalOut = System.out;

			WinStream errStream = new WinStream(outputStream, echoErr, originalErr);
			WinStream outStream = new WinStream(outputStream, echoOut, originalOut);

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
		FileOutputStream fileOutputStream;
		PrintStream echoStream;
		boolean echoEnabled;
		boolean isNewLine = true;
		long startTime;
		long currentTime;

		public WinStream(FileOutputStream stream, boolean echoEnabled, PrintStream echoStream) {
			super(echoStream);
			this.fileOutputStream = stream;
			this.echoStream = echoStream;
			this.echoEnabled = echoEnabled;
			this.startTime = System.currentTimeMillis();
		}

		private synchronized void appendToTextArea(String text) {
			currentTime = System.currentTimeMillis();
			if (text == "\n") {
				if (!isNewLine) {
					textArea.append("\n");
				}
				isNewLine = true;
				startTime = currentTime;

			} else {
				String prefix = "[" + U.time() + "|" + nf.format((float) (currentTime - startTime) / 1000) + "] "
						+ Thread.currentThread().getName() + ": ";

				if (text.indexOf('\n') < 0) {
					if (isNewLine) {
						textArea.append(prefix);
					}
					textArea.append(text);
					isNewLine = false;
				} else {
					String[] lines = U.split(text, '\n');
					for (int index = 0; index < lines.length; index++) {
						if (isNewLine) {
							textArea.append(prefix);
						}
						textArea.append(lines[index] + "\n");
						isNewLine = true;
					}
					startTime = currentTime;
				}
			}
			if (textArea.isVisible()) {
				javax.swing.SwingUtilities.invokeLater(updater);
			}
		}

		@Override
		public void print(boolean value) {
			if (echoEnabled) {
				echoStream.print(value);
			}
			if (logToFile) {
				super.print(value);
			}
			appendToTextArea(value + "");
		}

		@Override
		public void print(char value) {
			if (echoEnabled) {
				echoStream.print(value);
			}
			if (logToFile) {
				super.print(value);
			}
			appendToTextArea(value + "");
		}

		@Override
		public void print(int value) {
			if (echoEnabled) {
				echoStream.print(value);
			}
			if (logToFile) {
				super.print(value);
			}
			appendToTextArea(value + "");
		}

		@Override
		public void print(long value) {
			if (echoEnabled) {
				echoStream.print(value);
			}
			if (logToFile) {
				super.print(value);
			}
			appendToTextArea(value + "");
		}

		@Override
		public void print(float value) {
			if (echoEnabled) {
				echoStream.print(value);
			}
			if (logToFile) {
				super.print(value);
			}
			appendToTextArea(value + "");
		}

		@Override
		public void print(double value) {
			if (echoEnabled) {
				echoStream.print(value);
			}
			if (logToFile) {
				super.print(value);
			}
			appendToTextArea(value + "");
		}

		@Override
		public void print(char[] value) {
			if (echoEnabled) {
				echoStream.print(value);
			}
			if (logToFile) {
				super.print(new String(value));
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
			if (logToFile) {
				super.print(text);
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
			if (logToFile) {
				super.print(value.toString());
			}
			appendToTextArea("" + value);
		}

		@Override
		public void println() {
			if (echoEnabled) {
				echoStream.println();
			}
			if (logToFile) {
				super.println();
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
