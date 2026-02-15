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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.event.HyperlinkEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class MainPanelControllerTest {
    @Test
    void toggleDiskIo_fromStopped_setsRunningAndStopLabel() {
        MainPanelController.DiskToggleAction action = MainPanelController.toggleDiskIo(false, "Start", "Stop");

        assertEquals(true, action.running());
        assertEquals("Stop", action.buttonLabel());
    }

    @Test
    void toggleDiskIo_fromRunning_setsStoppedAndStartLabel() {
        MainPanelController.DiskToggleAction action = MainPanelController.toggleDiskIo(true, "Start", "Stop");

        assertEquals(false, action.running());
        assertEquals("Start", action.buttonLabel());
    }

    @Test
    void handleHyperlink_entered_setsHoverTextAndStoresPreviousStatus() {
        MainPanelController.HyperlinkAction action =
                MainPanelController.handleHyperlink(HyperlinkEvent.EventType.ENTERED, "https://anidb.net", "Idle", "");

        assertEquals("https://anidb.net", action.statusText());
        assertEquals("Idle", action.lastStatusMessage());
        assertNull(action.urlToOpen());
    }

    @Test
    void handleHyperlink_exited_restoresLastStatus() {
        MainPanelController.HyperlinkAction action = MainPanelController.handleHyperlink(
                HyperlinkEvent.EventType.EXITED, "https://anidb.net", "hover", "Idle");

        assertEquals("Idle", action.statusText());
        assertEquals("Idle", action.lastStatusMessage());
        assertNull(action.urlToOpen());
    }

    @Test
    void handleHyperlink_activated_requestsUrlOpenWithoutChangingStatus() {
        MainPanelController.HyperlinkAction action = MainPanelController.handleHyperlink(
                HyperlinkEvent.EventType.ACTIVATED, "https://anidb.net", "Idle", "BeforeHover");

        assertEquals("Idle", action.statusText());
        assertEquals("BeforeHover", action.lastStatusMessage());
        assertEquals("https://anidb.net", action.urlToOpen());
    }
}
