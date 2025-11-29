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
 * Created on 29.05.05
 *
 * @version 	02 (1.09,1.06)
 * @author 		epoximator
 */
package epox.webaom;

import java.util.StringTokenizer;

public final class Hyper {

	public static String sWarn = "F00000";
	public static String sName = "006699";
	public static String sNumb = "000080";

	private Hyper() {
		// static only
	}

	public static String href(String url, String name) {
		return "<a href=\"" + url + "\">" + name + "</a>";
	}

	public static String color(String col, String str) {
		return "<font color=#" + col + ">" + str + "</font>";
	}

	public static String error(String str) {
		return color(sWarn, str);
	}

	public static String name(String str) {
		return color(sName, str);
	}

	public static String name(Object o) {
		if (o != null)
			return color(sName, o.toString());
		return "null";
	}

	public static String number(int i) {
		return color(sNumb, "" + i);
	}

	public static String number(String str) {
		return color(sNumb, str);
	}

	public static String enc() {
		return sWarn + Options.S_SEP + sName + Options.S_SEP + sNumb;
	}

	public static void dec(String str) {
		if (str == null)
			return;
		StringTokenizer st = new StringTokenizer(str, Options.S_SEP);
		if (st.countTokens() != 3)
			return;
		String s = st.nextToken();
		if (s.length() == 6)
			sWarn = s;
		s = st.nextToken();
		if (s.length() == 6)
			sName = s;
		s = st.nextToken();
		if (s.length() == 6)
			sNumb = s;
	}
}
