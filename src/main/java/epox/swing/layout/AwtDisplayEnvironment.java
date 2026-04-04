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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

final class AwtDisplayEnvironment implements DisplayEnvironment {
    static final AwtDisplayEnvironment INSTANCE = new AwtDisplayEnvironment();
    private static final Rectangle HEADLESS_BOUNDS = new Rectangle(0, 0, 1280, 800);

    private AwtDisplayEnvironment() {
        // singleton
    }

    @Override
    public UsableScreenBounds getUsableScreenBounds(Window window) {
        if (GraphicsEnvironment.isHeadless()) {
            return new UsableScreenBounds(HEADLESS_BOUNDS, HEADLESS_BOUNDS);
        }

        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultDevice = environment.getDefaultScreenDevice();
        GraphicsConfiguration graphicsConfiguration =
                resolveGraphicsConfiguration(window, defaultDevice.getDefaultConfiguration());
        Rectangle screenBounds = graphicsConfiguration.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
        return UsableScreenBounds.fromScreenBounds(screenBounds, screenInsets);
    }

    static GraphicsConfiguration resolveGraphicsConfiguration(
            Window window, GraphicsConfiguration defaultConfiguration) {
        Window currentWindow = window;
        while (currentWindow != null) {
            GraphicsConfiguration graphicsConfiguration = currentWindow.getGraphicsConfiguration();
            if (graphicsConfiguration != null) {
                return graphicsConfiguration;
            }
            currentWindow = currentWindow.getOwner();
        }

        GraphicsConfiguration configuration = defaultConfiguration;
        if (configuration != null) {
            return configuration;
        }

        throw new HeadlessException("No graphics configuration is available.");
    }
}
