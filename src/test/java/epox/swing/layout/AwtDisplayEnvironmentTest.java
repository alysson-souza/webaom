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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Window;
import org.junit.jupiter.api.Test;

class AwtDisplayEnvironmentTest {
    @Test
    void resolveGraphicsConfiguration_prefersWindowConfiguration() {
        Window window = mock(Window.class);
        GraphicsConfiguration windowConfiguration = mock(GraphicsConfiguration.class);
        GraphicsConfiguration defaultConfiguration = mock(GraphicsConfiguration.class);
        when(window.getGraphicsConfiguration()).thenReturn(windowConfiguration);

        GraphicsConfiguration resolved =
                AwtDisplayEnvironment.resolveGraphicsConfiguration(window, defaultConfiguration);

        assertSame(windowConfiguration, resolved);
    }

    @Test
    void resolveGraphicsConfiguration_usesOwnerConfigurationWhenWindowIsNotDisplayable() {
        Window window = mock(Window.class);
        Window owner = mock(Window.class);
        GraphicsConfiguration ownerConfiguration = mock(GraphicsConfiguration.class);
        GraphicsConfiguration defaultConfiguration = mock(GraphicsConfiguration.class);
        when(window.getGraphicsConfiguration()).thenReturn(null);
        when(window.getOwner()).thenReturn(owner);
        when(owner.getGraphicsConfiguration()).thenReturn(ownerConfiguration);

        GraphicsConfiguration resolved =
                AwtDisplayEnvironment.resolveGraphicsConfiguration(window, defaultConfiguration);

        assertSame(ownerConfiguration, resolved);
    }

    @Test
    void resolveGraphicsConfiguration_fallsBackToDefaultConfiguration() {
        Window window = mock(Window.class);
        GraphicsConfiguration defaultConfiguration = mock(GraphicsConfiguration.class);
        when(window.getGraphicsConfiguration()).thenReturn(null);
        when(window.getOwner()).thenReturn(null);

        GraphicsConfiguration resolved =
                AwtDisplayEnvironment.resolveGraphicsConfiguration(window, defaultConfiguration);

        assertSame(defaultConfiguration, resolved);
    }

    @Test
    void resolveGraphicsConfiguration_throwsWhenNoConfigurationExists() {
        Window window = mock(Window.class);
        when(window.getGraphicsConfiguration()).thenReturn(null);
        when(window.getOwner()).thenReturn(null);

        assertThrows(HeadlessException.class, () -> AwtDisplayEnvironment.resolveGraphicsConfiguration(window, null));
    }
}
