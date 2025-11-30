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
 * Created on 03.08.05
 *
 * @version 	02 (1.09,1.05)
 * @author 		epoximator
 */
package epox.util;

import epox.webaom.Options;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Represents a source-to-destination mapping rule with an enabled/disabled state.
 * Used for character replacement rules in file renaming.
 */
public class DSData {
	/** The source pattern to match */
	public String source;
	/** The destination string to replace the source with */
	public String destination;
	/** Whether this mapping rule is enabled */
	public Boolean enabled;

	public DSData(String sourcePattern, String destinationPattern, boolean isEnabled) {
		source = sourcePattern;
		destination = destinationPattern;
		enabled = Boolean.valueOf(isEnabled);
	}

	public String toString() {
		String destValue = destination;
		if (destValue.equals("")) {
			destValue = "\\0";
		}
		if (enabled) {
			return source + Options.FIELD_SEPARATOR + destValue;
		}
		return "#" + source + Options.FIELD_SEPARATOR + destValue;
	}

	/**
	 * Parses a source-destination pair from encoded strings.
	 * Lines starting with '#' indicate a disabled rule.
	 */
	public static DSData parse(String sourceString, String destinationString) {
		boolean isDisabled = sourceString.startsWith("#");
		if (isDisabled) {
			sourceString = sourceString.substring(1);
		}
		if (destinationString.equals("\\0")) {
			destinationString = "";
		}
		return new DSData(sourceString, destinationString, !isDisabled);
	}

	/**
	 * Encodes a vector of DSData rules into a single delimited string.
	 */
	public static String encode(Vector<DSData> rulesList) {
		String encodedResult = "";
		DSData currentRule;
		for (int i = 0; i < rulesList.size(); i++) {
			currentRule = rulesList.elementAt(i);
			if (!currentRule.source.equals("")) {
				encodedResult += currentRule + Options.FIELD_SEPARATOR;
			}
		}
		return encodedResult;
	}

	/**
	 * Decodes an encoded string into a vector of DSData rules.
	 */
	public static String decode(Vector<DSData> rulesList, String encodedString) {
		rulesList.clear();
		StringTokenizer tokenizer = new StringTokenizer(encodedString, Options.FIELD_SEPARATOR);
		while (tokenizer.hasMoreTokens()) {
			rulesList.add(parse(tokenizer.nextToken(), tokenizer.nextToken()));
		}
		return encodedString;
	}
}
