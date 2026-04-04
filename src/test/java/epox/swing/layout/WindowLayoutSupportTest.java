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

package epox.swing.layout;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import org.junit.jupiter.api.Test;

class WindowLayoutSupportTest {
    private final WindowLayoutPolicy windowLayoutPolicy = new WindowLayoutPolicy();

    @Test
    void placeCentered_usesReferenceWindowForScreenSelection() {
        Window window = mock(Window.class);
        Window referenceWindow = mock(Window.class);
        RecordingDisplayEnvironment displayEnvironment = new RecordingDisplayEnvironment();
        when(window.getPreferredSize()).thenReturn(new Dimension(400, 300));

        WindowLayoutSupport.placeCentered(
                window, referenceWindow, displayEnvironment, windowLayoutPolicy, new Dimension(200, 150));

        assertSame(referenceWindow, displayEnvironment.requestedWindow);
        verify(window).setMinimumSize(new Dimension(200, 150));
        verify(window).setBounds(new Rectangle(400, 250, 400, 300));
    }

    @Test
    void placeCentered_defaultsToTargetWindowWhenNoReferenceIsProvided() {
        Window window = mock(Window.class);
        RecordingDisplayEnvironment displayEnvironment = new RecordingDisplayEnvironment();
        when(window.getPreferredSize()).thenReturn(new Dimension(320, 240));

        WindowLayoutSupport.placeCentered(
                window, null, displayEnvironment, windowLayoutPolicy, new Dimension(200, 150));

        assertSame(window, displayEnvironment.requestedWindow);
    }

    @Test
    void placeCentered_canSkipApplyingMinimumSize() {
        Window window = mock(Window.class);
        RecordingDisplayEnvironment displayEnvironment = new RecordingDisplayEnvironment();
        when(window.getPreferredSize()).thenReturn(new Dimension(320, 240));

        WindowLayoutSupport.placeCentered(
                window, null, displayEnvironment, windowLayoutPolicy, new Dimension(200, 150), false);

        verify(window, never()).setMinimumSize(any(Dimension.class));
        verify(window).setBounds(new Rectangle(440, 280, 320, 240));
    }

    private static final class RecordingDisplayEnvironment implements DisplayEnvironment {
        private Window requestedWindow;

        @Override
        public UsableScreenBounds getUsableScreenBounds(Window window) {
            requestedWindow = window;
            Rectangle screenBounds = new Rectangle(0, 0, 1200, 800);
            return new UsableScreenBounds(screenBounds, screenBounds);
        }
    }
}
