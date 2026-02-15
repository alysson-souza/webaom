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

import javax.swing.event.HyperlinkEvent;

public final class MainPanelController {
    private MainPanelController() {}

    public static DiskToggleAction toggleDiskIo(boolean currentlyRunning, String enableLabel, String disableLabel) {
        boolean running = !currentlyRunning;
        String label = running ? disableLabel : enableLabel;
        return new DiskToggleAction(running, label);
    }

    public static HyperlinkAction handleHyperlink(
            HyperlinkEvent.EventType eventType, String description, String currentStatus, String previousStatus) {
        if (eventType == HyperlinkEvent.EventType.ACTIVATED) {
            return new HyperlinkAction(currentStatus, previousStatus, description);
        }
        if (eventType == HyperlinkEvent.EventType.ENTERED) {
            return new HyperlinkAction(description, currentStatus, null);
        }
        if (eventType == HyperlinkEvent.EventType.EXITED) {
            return new HyperlinkAction(previousStatus, previousStatus, null);
        }
        return new HyperlinkAction(currentStatus, previousStatus, null);
    }

    public record DiskToggleAction(boolean running, String buttonLabel) {}

    public record HyperlinkAction(String statusText, String lastStatusMessage, String urlToOpen) {}
}
