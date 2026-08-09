/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintStream;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JPanelDebugTest {
    @TempDir
    Path temporaryDirectory;

    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void saveStandardStreams() {
        originalOut = System.out;
        originalErr = System.err;
    }

    @AfterEach
    void restoreStandardStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void capturedStreamsRenderEveryPrintShapeInThePanel() throws Exception {
        JPanelDebug panel = new JPanelDebug(null, true, true, false, false);

        System.out.print(true);
        System.out.print(' ');
        System.out.print(42);
        System.out.print(43L);
        System.out.print(1.5f);
        System.out.print(2.5d);
        System.out.print(new char[] {'x', 'y'});
        System.out.print(" single");
        System.out.print((Object) " object");
        System.out.println();
        System.out.println(false);
        System.out.println('z');
        System.out.println(7);
        System.out.println(8L);
        System.out.println(3.5f);
        System.out.println(4.5d);
        System.out.println(new char[] {'a', 'b'});
        System.out.println("two\nlines");
        System.out.println((Object) "last");
        System.err.println("failure");
        SwingUtilities.invokeAndWait(() -> {});

        String output = panel.textArea.getText();
        assertTrue(output.contains("true 42431.52.5xy single object"));
        assertTrue(output.contains("false"));
        assertTrue(output.contains("two"));
        assertTrue(output.contains("lines"));
        assertTrue(output.contains("last"));
        assertTrue(output.contains("failure"));
    }

    @Test
    void updaterScrollsToTheLatestCapturedOutput() {
        JPanelDebug panel = new JPanelDebug(null, false, false, false, false);
        panel.scrollBar.setValues(0, 10, 0, 100);

        panel.updater.run();

        assertEquals(panel.scrollBar.getMaximum() - panel.scrollBar.getVisibleAmount(), panel.scrollBar.getValue());
    }

    @Test
    void unavailableLogFileIsReportedInsideThePanel() {
        JPanelDebug panel = new JPanelDebug(temporaryDirectory.toString(), false, false, false, false);

        assertTrue(panel.textArea.getText().contains("Could not openfile" + temporaryDirectory));
    }
}
