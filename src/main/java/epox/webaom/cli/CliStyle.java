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

package epox.webaom.cli;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/** Applies terminal-safe semantic styling to human-readable CLI output. */
final class CliStyle {
    enum Mode {
        AUTO,
        ALWAYS,
        NEVER;

        static Mode parse(String value) {
            if (value == null) {
                return AUTO;
            }
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return AUTO;
            }
        }
    }

    private static final String ESC = "\u001B";
    private static final String RESET = ESC + "[0m";
    private static final String OSC_LINK_OPEN = ESC + "]8;;";
    private static final String OSC_LINK_CLOSE = ESC + "]8;;" + ESC + "\\";
    private static final String STRING_TERMINATOR = ESC + "\\";

    private static final String BOLD = "1";
    private static final String DIM = "2";
    private static final String RED = "31";
    private static final String GREEN = "32";
    private static final String YELLOW = "33";
    private static final String CYAN = "36";

    private final boolean colors;
    private final boolean hyperlinks;

    private CliStyle(boolean colors, boolean hyperlinks) {
        this.colors = colors;
        this.hyperlinks = hyperlinks;
    }

    static CliStyle plain() {
        return new CliStyle(false, false);
    }

    static CliStyle resolve(
            Mode colorMode, Mode hyperlinkMode, boolean interactive, boolean noColorEnvironmentPresent) {
        boolean colors =
                switch (colorMode) {
                    case ALWAYS -> true;
                    case NEVER -> false;
                    case AUTO -> interactive && !noColorEnvironmentPresent;
                };
        boolean hyperlinks =
                switch (hyperlinkMode) {
                    case ALWAYS -> true;
                    case NEVER -> false;
                    case AUTO -> interactive;
                };
        return new CliStyle(colors, hyperlinks);
    }

    boolean colorsEnabled() {
        return colors;
    }

    boolean hyperlinksEnabled() {
        return hyperlinks;
    }

    String text(String value) {
        return sanitize(value, true);
    }

    String inline(String value) {
        return sanitize(value, false);
    }

    String label(String value) {
        return ansi(BOLD, value, true);
    }

    String detail(String value) {
        return ansi(DIM, value, true);
    }

    String progress(String value) {
        return ansi(CYAN, value, true);
    }

    String error(String value) {
        return ansi(RED, value, true);
    }

    String success(String value) {
        return ansi(GREEN, value, true);
    }

    String warning(String value) {
        return ansi(YELLOW, value, true);
    }

    String prompt(String value) {
        return ansi(BOLD + ";" + CYAN, value, true);
    }

    String status(CliFileResult.Status status) {
        String display = status == null
                ? "failed"
                : status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (status == null || status == CliFileResult.Status.FAILED || status == CliFileResult.Status.UNKNOWN) {
            return error(display);
        }
        if (status == CliFileResult.Status.RENAME_PLANNED
                || status == CliFileResult.Status.MOVE_PLANNED
                || status == CliFileResult.Status.UNCHANGED
                || status == CliFileResult.Status.MYLIST_ABSENT) {
            return warning(display);
        }
        return success(display);
    }

    String path(Path path) {
        if (path == null) {
            return "";
        }
        String label = ansi(CYAN, path.toString(), false);
        return hyperlink(path.toAbsolutePath().normalize().toUri().toASCIIString(), label);
    }

    String webLink(String url, String label) {
        return hyperlink(url, ansi(CYAN, label, false));
    }

    String messageWithPath(String message, Path path) {
        if (message == null || path == null) {
            return text(message);
        }
        String needle = path.toString();
        if (needle.isEmpty() || !message.contains(needle)) {
            return text(message);
        }
        StringBuilder rendered = new StringBuilder();
        int offset = 0;
        int match;
        while ((match = message.indexOf(needle, offset)) >= 0) {
            rendered.append(text(message.substring(offset, match)));
            rendered.append(path(path));
            offset = match + needle.length();
        }
        return rendered.append(text(message.substring(offset))).toString();
    }

    private String hyperlink(String destination, String label) {
        if (!hyperlinks) {
            return label;
        }
        String safeDestination = safeDestination(destination);
        if (safeDestination == null) {
            return label;
        }
        return OSC_LINK_OPEN + safeDestination + STRING_TERMINATOR + label + OSC_LINK_CLOSE;
    }

    private static String safeDestination(String destination) {
        try {
            URI uri = URI.create(destination);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("file")
                            || scheme.equalsIgnoreCase("http")
                            || scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            String ascii = uri.toASCIIString();
            for (int index = 0; index < ascii.length(); index++) {
                if (Character.isISOControl(ascii.charAt(index))) {
                    return null;
                }
            }
            return ascii;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String ansi(String code, String value, boolean multiline) {
        String safe = sanitize(value, multiline);
        return colors ? ESC + "[" + code + "m" + safe + RESET : safe;
    }

    private static String sanitize(String value, boolean multiline) {
        if (value == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (multiline && character == '\r' && index + 1 < value.length() && value.charAt(index + 1) == '\n') {
                continue;
            }
            if (multiline && (character == '\n' || character == '\t')) {
                safe.append(character);
            } else if (Character.isISOControl(character)) {
                safe.append(String.format("\\u%04X", (int) character));
            } else {
                safe.append(character);
            }
        }
        return safe.toString();
    }
}
