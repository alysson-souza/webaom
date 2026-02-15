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

import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.net.URI;
import javax.swing.Timer;

public class DefaultMainPanelRuntime implements MainPanelRuntime {
    @Override
    public Timer createTimer(int delayMs, ActionListener listener) {
        return new Timer(delayMs, listener);
    }

    @Override
    public Thread startBackgroundTask(String threadName, Runnable runnable) {
        Thread thread = new Thread(runnable, threadName);
        thread.start();
        return thread;
    }

    @Override
    public boolean requestCredentials() {
        return new JDialogLogin().getPass() != null;
    }

    @Override
    public void openHyperlink(String browserPath, String url) throws Exception {
        if (browserPath != null && !browserPath.isEmpty()) {
            Runtime.getRuntime().exec(new String[] {browserPath, url});
            return;
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
            return;
        }
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            Runtime.getRuntime().exec(new String[] {"open", url});
        } else if (os.contains("win")) {
            Runtime.getRuntime().exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", url});
        } else {
            Runtime.getRuntime().exec(new String[] {"xdg-open", url});
        }
    }
}
