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
 * Created on 25.feb.2006 18:58:58
 * Filename: U.java
 */
package epox.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Utility class providing common helper methods for string manipulation,
 * formatting, and file operations.
 *
 * @author JV
 * @version X
 */
public class U {
	/** Date formatter for time display (German locale, medium format) */
	private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.GERMANY);
	/** Decimal formatter for two decimal places */
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

	/**
	 * Returns the current time formatted as a string.
	 *
	 * @return formatted time string
	 */
	public static String time() {
		return TIME_FORMAT.format(new Date(System.currentTimeMillis()));
	}

	/**
	 * Prints an error message to stderr with "! " prefix.
	 *
	 * @param message
	 *            the message to print
	 */
	public static void err(Object message) {
		System.err.println("! " + message);
	}

	/**
	 * Prints a message to stdout.
	 *
	 * @param message
	 *            the message to print
	 */
	public static void out(Object message) {
		System.out.println(message);
	}

	/**
	 * Checks if a string contains only alphanumeric characters, hyphens, and underscores.
	 * Must be 3-16 characters long.
	 *
	 * @param text
	 *            the string to validate
	 * @return true if the string matches the pattern
	 */
	public static boolean alfanum(String text) {
		return text.matches("^[a-zA-Z0-9\\-\\_]{3,16}$");
	}

	/**
	 * Replaces all occurrences of a source string with a destination string.
	 *
	 * @param text
	 *            the original text
	 * @param source
	 *            the substring to find
	 * @param destination
	 *            the replacement string
	 * @return the text with all replacements made
	 */
	public static String replace(String text, String source, String destination) {
		int sourceLength = source.length();
		int destLength = destination.length();
		int index = text.indexOf(source);
		while (index >= 0) {
			text = text.substring(0, index) + destination + text.substring(index + sourceLength);
			index = text.indexOf(source, index + destLength);
		}
		return text;
	}

	/**
	 * Extracts the content between XML/HTML opening and closing tags.
	 *
	 * @param text
	 *            the text containing the tag
	 * @param tag
	 *            the tag name (without angle brackets)
	 * @return the content between the tags, or null if not found
	 */
	public static String getInTag(String text, String tag) {
		int startIndex = text.indexOf("<" + tag + ">");
		if (startIndex < 0) {
			return null;
		}
		startIndex += tag.length() + 2;
		int endIndex = text.indexOf("</" + tag + ">", startIndex);
		if (endIndex < 0) {
			return null;
		}
		return text.substring(startIndex, endIndex);
	}

	/**
	 * Parses a string to an integer. Shorthand for Integer.parseInt().
	 *
	 * @param text
	 *            the string to parse
	 * @return the integer value
	 * @throws NumberFormatException
	 *             if the string is not a valid integer
	 */
	public static int i(String text) throws NumberFormatException {
		return Integer.parseInt(text);
	}

	/**
	 * Normalizes a string by returning null for empty or "null" strings.
	 *
	 * @param text
	 *            the string to normalize
	 * @return the original string, or null if empty or "null"
	 */
	public static String n(String text) {
		if (text == null || text.isEmpty() || text.equals("null")) {
			return null;
		}
		return text;
	}

	/**
	 * Splits a string by a delimiter, including empty strings (unlike String.split()).
	 *
	 * @param text
	 *            the string to split
	 * @param delimiter
	 *            the character to split on
	 * @return array of substrings
	 */
	public static String[] split(String text, char delimiter) {
		int segmentCount = 1;
		for (int index = 0; index < text.length(); index++) {
			if (text.charAt(index) == delimiter) {
				segmentCount++;
			}
		}
		if (segmentCount < 2) {
			return new String[]{text};
		}
		int[] delimiterPositions = new int[segmentCount];
		int posIndex = 0;
		for (int index = 0; index < text.length(); index++) {
			if (text.charAt(index) == delimiter) {
				delimiterPositions[posIndex++] = index;
			}
		}
		delimiterPositions[segmentCount - 1] = text.length();
		String[] result = new String[segmentCount];
		int startPos = 0;
		for (int index = 0; index < segmentCount; index++) {
			result[index] = text.substring(startPos, delimiterPositions[index]);
			startPos = delimiterPositions[index] + 1;
		}
		return result;
	}

	/**
	 * Strips all HTML tags from a string.
	 *
	 * @param html
	 *            the HTML string
	 * @return the text content without tags
	 */
	public static String dehtmlize(String html) {
		if (html == null) {
			return null;
		}
		StringBuilder result = new StringBuilder(html.length());
		StringBuffer source = new StringBuffer(html);
		boolean insideTag = false;
		char currentChar;
		for (int index = 0; index < source.length(); index++) {
			currentChar = source.charAt(index);
			if (currentChar == '<') {
				insideTag = true;
				continue;
			}
			if (currentChar == '>') {
				insideTag = false;
				continue;
			}
			if (!insideTag) {
				result.append(currentChar);
			}
		}
		return result.toString();
	}

	/**
	 * Decodes HTML numeric character references (&#nnnn;) to their character equivalents.
	 *
	 * @param text
	 *            the text containing HTML character references
	 * @return the text with character references decoded
	 */
	public static String htmldesc(String text) {
		if (text == null) {
			return null;
		}
		StringBuffer source = new StringBuffer(text);
		StringBuilder result = new StringBuilder(text.length());
		StringBuffer numericCode = new StringBuffer(5);
		char currentChar;
		boolean inNumericRef = false;
		int length = source.length();
		for (int index = 0; index < length; index++) {
			currentChar = source.charAt(index);
			if (currentChar == '&' && index < (length - 2)) {
				if (source.charAt(index + 1) == '#') {
					inNumericRef = true;
					index++;
					numericCode.delete(0, numericCode.length());
				} else {
					result.append(currentChar);
				}
			} else if (currentChar == ';') {
				if (inNumericRef) {
					try {
						result.append((char) Integer.parseInt(numericCode.toString()));
					} catch (NumberFormatException ex) {
						result.append('&');
						result.append('#');
						result.append(numericCode);
						result.append(currentChar);
					}
					inNumericRef = false;
				} else {
					result.append(';');
				}
			} else if (inNumericRef) {
				if (Character.isDigit(currentChar) && numericCode.length() < 11) {
					numericCode.append(currentChar);
				} else {
					inNumericRef = false;
					result.append('&');
					result.append('#');
					result.append(numericCode);
					result.append(currentChar);
				}
			} else {
				result.append(currentChar);
			}
		}
		return result.toString();
	}

	/** Size unit prefixes for byte formatting (B, KB, MB, GB, TB, PB, EB) */
	private static final char[] SIZE_UNIT_PREFIXES = {' ', 'K', 'M', 'G', 'T', 'P', 'E'};

	private static String sbyte(double bytes, int unitIndex) {
		if (bytes < 1000) {
			return DECIMAL_FORMAT.format(bytes) + " " + SIZE_UNIT_PREFIXES[unitIndex] + "B";
		}
		return sbyte(bytes / 1024, unitIndex + 1);
	}

	/**
	 * Formats a byte size into a human-readable string with appropriate unit.
	 *
	 * @param bytes
	 *            the size in bytes
	 * @return formatted string (e.g., "1.50 MB")
	 */
	public static String sbyte(long bytes) {
		return sbyte(bytes, 0);
		/*
		 * if(l<1000) return def.format(l)+" B";//1024
		 * if(l<1024000) return def.format(l/1024)+" KB";//1048576
		 * if(l<1048576000) return def.format(l/1048576)+" MB";//1073741824
		 * if(l<1073741824000l) return def.format(l/1073741824)+" GB";
		 * return def.format(l/1099511627776l)+" TB";
		 */
	}

	/**
	 * Reads a file's content as a string (limited to 1MB files).
	 *
	 * @param path
	 *            the file path
	 * @return the file content, or null if file is too large or on error
	 */
	public static String fileToString(String path) {
		try {
			File file = new File(path);
			if (file.length() > 1024 * 1024) {
				return null;
			}
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file), (int) file.length());
			byte[] buffer = new byte[(int) file.length()];
			inputStream.read(buffer);
			inputStream.close();
			return new String(buffer);
		} catch (IOException ex) {
			// Silently fail and return null
		}
		return null;
	}

	/**
	 * Replaces 3-character codes prefixed with '%' using a lookup map.
	 *
	 * @param text
	 *            the text containing codes to replace
	 * @param codeMap
	 *            map of 3-char codes to replacement values
	 * @return the text with codes replaced
	 */
	@SuppressWarnings("rawtypes")
	public static String replaceCCCode(String text, HashMap codeMap) {
		StringBuilder result = new StringBuilder(text.length() * 4);
		char currentChar;
		Object replacement;
		int index;
		for (index = 0; index < text.length() - 3; index++) {
			currentChar = text.charAt(index);
			if (currentChar == '%') {
				replacement = codeMap.get(text.substring(index + 1, index + 4));
				if (replacement != null) {
					result.append(replacement);
					index += 3;
					continue;
				}
			}
			result.append(currentChar);
		}
		for (; index < text.length(); index++) {
			result.append(text.charAt(index));
		}
		return result.toString();
	}
}
