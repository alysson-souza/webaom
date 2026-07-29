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

import java.nio.file.Path;

/** Expected command failure with a stable process exit code. */
public final class CliException extends Exception {
    private final int exitCode;
    private final boolean error;
    private final Path linkedPath;

    public CliException(String message, int exitCode) {
        this(message, exitCode, null, true, null);
    }

    public CliException(String message, int exitCode, Throwable cause) {
        this(message, exitCode, cause, true, null);
    }

    static CliException cancelled(String message, int exitCode) {
        return new CliException(message, exitCode, null, false, null);
    }

    static CliException withPath(String message, int exitCode, Throwable cause, Path linkedPath) {
        return new CliException(message, exitCode, cause, true, linkedPath);
    }

    private CliException(String message, int exitCode, Throwable cause, boolean error, Path linkedPath) {
        super(message, cause);
        this.exitCode = exitCode;
        this.error = error;
        this.linkedPath = linkedPath;
    }

    public int exitCode() {
        return exitCode;
    }

    boolean isError() {
        return error;
    }

    Path linkedPath() {
        return linkedPath;
    }
}
