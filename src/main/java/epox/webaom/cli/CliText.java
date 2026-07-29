/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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

package epox.webaom.cli;

/** Small string helpers shared by the command-line entry point and its application layer. */
final class CliText {
    private CliText() {}

    /** Returns {@code value} unless it is null or blank, in which case {@code fallback}. */
    static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Returns the first non-blank argument, or null when both are blank. */
    static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }

    /** Returns {@code value} unless it is null or blank, in which case null. */
    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** Returns a throwable's message, falling back to its class name when there is none. */
    static String usefulMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
