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
import java.util.List;
import java.util.StringTokenizer;

/**
 * Represents a source-to-destination mapping rule with an enabled/disabled state.
 * Used for character replacement rules in file renaming.
 */
public class ReplacementRule {
    /** The source pattern to match */
    public String source;
    /** The destination string to replace the source with */
    public String destination;
    /** Whether this mapping rule is enabled */
    public boolean enabled;

    public ReplacementRule(String sourcePattern, String destinationPattern, boolean isEnabled) {
        source = sourcePattern;
        destination = destinationPattern;
        enabled = isEnabled;
    }

    /**
     * Parses a source-destination pair from encoded strings.
     * Lines starting with '#' indicate a disabled rule.
     */
    public static ReplacementRule parse(String sourceString, String destinationString) {
        boolean isDisabled = sourceString.startsWith("#");
        if (isDisabled) {
            sourceString = sourceString.substring(1);
        }
        if (destinationString.equals("\\0")) {
            destinationString = "";
        }
        return new ReplacementRule(sourceString, destinationString, !isDisabled);
    }

    /**
     * Encodes a list of DSData rules into a single delimited string.
     */
    public static String encode(List<ReplacementRule> rulesList) {
        StringBuilder encodedResult = new StringBuilder();
        for (ReplacementRule currentRule : rulesList) {
            if (!currentRule.source.isEmpty()) {
                encodedResult.append(currentRule).append(Options.FIELD_SEPARATOR);
            }
        }
        return encodedResult.toString();
    }

    /**
     * Decodes an encoded string into a list of DSData rules.
     */
    public static String decode(List<ReplacementRule> rulesList, String encodedString) {
        rulesList.clear();
        StringTokenizer tokenizer = new StringTokenizer(encodedString, Options.FIELD_SEPARATOR);
        while (tokenizer.hasMoreTokens()) {
            rulesList.add(parse(tokenizer.nextToken(), tokenizer.nextToken()));
        }
        return encodedString;
    }

    @Override
    public String toString() {
        String destValue = destination;
        if (destValue.isEmpty()) {
            destValue = "\\0";
        }
        if (enabled) {
            return source + Options.FIELD_SEPARATOR + destValue;
        }
        return "#" + source + Options.FIELD_SEPARATOR + destValue;
    }
}
