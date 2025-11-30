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
 * Created on 08.08.05
 *
 * @version 	01 (1.09) [from MainPanel]
 * @author 		epoximator
 */
package epox.webaom.net;

import epox.webaom.HyperlinkBuilder;
import epox.webaom.ui.JPanelMain;

/**
 * Thread that performs a ping test to the AniDB server and reports the result.
 */
public class Pinger extends Thread {
	private final JPanelMain mainPanel;

	public Pinger(JPanelMain mainPanel) {
		super("Pinger");
		this.mainPanel = mainPanel;
		this.mainPanel.handleFatalError(true);
		start();
	}

	@Override
	public void run() {
		AniDBConnection connection = null;
		try {
			connection = mainPanel.createConnection();
			if (connection.connect()) {
				mainPanel.println("PING...");
				String pingResultMessage = "PONG (in " + (connection.ping() / 1000f) + " seconds).";
				mainPanel.println(pingResultMessage);
				mainPanel.showMessage(pingResultMessage);
			} else {
				mainPanel.showMessage(connection.getLastError() + ".");
			}
		} catch (java.net.SocketTimeoutException e) {
			String errorMessage = "AniDB is not reachable";
			mainPanel.println(HyperlinkBuilder.formatAsError(errorMessage + "."));
			mainPanel.showMessage(errorMessage);
		} catch (NumberFormatException e) {
			mainPanel.showMessage("Invalid number." + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			mainPanel.println(HyperlinkBuilder.formatAsError(e.getMessage() + "."));
			mainPanel.showMessage(e.getMessage() + ".");
		}
		if (connection != null) {
			connection.disconnect();
		}
		mainPanel.handleFatalError(false);
	}
}
