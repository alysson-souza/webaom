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
 * Created on 29.01.05
 *
 * @version 	1.06, 1.05, 1.00
 * @author 		epoximator
 */
package epox.swing;

/**
 * Interface for logging output. Provides methods for general output and status messages.
 */
public interface Log {
	/**
	 * Prints a message followed by a newline.
	 *
	 * @param message
	 *            the message to print
	 */
	void println(Object message);

	/**
	 * Displays a primary status message.
	 *
	 * @param message
	 *            the status message
	 */
	void status0(String message);

	/**
	 * Displays a secondary status message.
	 *
	 * @param message
	 *            the status message
	 */
	void status1(String message);
}
