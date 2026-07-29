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

import java.io.IOException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/** JLine-backed interactive prompts with deterministic cancellation and terminal cleanup. */
final class CliPrompt implements CliInput {
    @FunctionalInterface
    interface Factory {
        CliInput open(CliStyle style) throws CliException;
    }

    static final class Cancelled extends RuntimeException {
        private final int exitCode;

        Cancelled(int exitCode) {
            this.exitCode = exitCode;
        }

        int exitCode() {
            return exitCode;
        }
    }

    private final CliStyle style;
    private final Terminal terminal;
    private final LineReader reader;

    private CliPrompt(CliStyle style, Terminal terminal, LineReader reader) {
        this.style = style;
        this.terminal = terminal;
        this.reader = reader;
    }

    static CliPrompt open(CliStyle style) throws CliException {
        Terminal terminal = null;
        try {
            terminal = TerminalBuilder.builder().system(true).dumb(true).build();
            if (Terminal.TYPE_DUMB.equalsIgnoreCase(terminal.getType())
                    || Terminal.TYPE_DUMB_COLOR.equalsIgnoreCase(terminal.getType())) {
                terminal.close();
                throw new CliException("This command requires an interactive terminal.", 2);
            }
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .variable(LineReader.DISABLE_HISTORY, true)
                    .build();
            return new CliPrompt(style, terminal, reader);
        } catch (IOException exception) {
            closeAfterFailedInitialization(terminal);
            throw new CliException("Could not initialize the interactive terminal.", 2, exception);
        } catch (RuntimeException exception) {
            closeAfterFailedInitialization(terminal);
            throw new CliException("Could not initialize the interactive terminal.", 2, exception);
        }
    }

    private static void closeAfterFailedInitialization(Terminal terminal) {
        if (terminal == null) {
            return;
        }
        try {
            terminal.close();
        } catch (IOException ignored) {
            // The initialization failure is the useful error.
        }
    }

    @Override
    public String readLine(String prompt) {
        return read(prompt, null);
    }

    @Override
    public String readSecret(String prompt) {
        return read(prompt, Character.MIN_VALUE);
    }

    private String read(String prompt, Character mask) {
        try {
            return reader.readLine(style.prompt(prompt), mask);
        } catch (UserInterruptException exception) {
            throw new Cancelled(130);
        } catch (EndOfFileException exception) {
            // Some terminal providers surface SIGINT as EOF after interrupting the reader thread.
            // JLine restores the interrupt flag during cleanup, which distinguishes that path from
            // a real Ctrl+D/end-of-input event.
            throw new Cancelled(Thread.interrupted() ? 130 : 1);
        }
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }
}
