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
 */

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

class CliPromptCharacterizationTest {
    @Test
    void lineAndSecretReadsUseStyledPromptsAndTheExpectedMask() throws Exception {
        Terminal terminal = mock(Terminal.class);
        LineReader reader = mock(LineReader.class);
        when(reader.readLine("Username: ", (Character) null)).thenReturn("alice");
        when(reader.readLine("Password: ", Character.MIN_VALUE)).thenReturn("secret");
        CliPrompt prompt = new CliPrompt(CliStyle.plain(), terminal, reader);

        assertEquals("alice", prompt.readLine("Username: "));
        assertEquals("secret", prompt.readSecret("Password: "));
        prompt.close();

        verify(terminal).close();
    }

    @Test
    void userInterruptMapsToShellInterruptExitCode() {
        LineReader reader = mock(LineReader.class);
        when(reader.readLine("Prompt: ", (Character) null)).thenThrow(new UserInterruptException("partial"));
        CliPrompt prompt = new CliPrompt(CliStyle.plain(), mock(Terminal.class), reader);

        CliPrompt.Cancelled cancelled = assertThrows(CliPrompt.Cancelled.class, () -> prompt.readLine("Prompt: "));

        assertEquals(130, cancelled.exitCode());
    }

    @Test
    void ordinaryEndOfFileMapsToStatusOne() {
        LineReader reader = mock(LineReader.class);
        when(reader.readLine("Prompt: ", (Character) null)).thenThrow(new EndOfFileException());
        CliPrompt prompt = new CliPrompt(CliStyle.plain(), mock(Terminal.class), reader);

        CliPrompt.Cancelled cancelled = assertThrows(CliPrompt.Cancelled.class, () -> prompt.readLine("Prompt: "));

        assertEquals(1, cancelled.exitCode());
    }

    @Test
    void interruptedEndOfFileMapsToShellInterruptAndClearsTheFlag() {
        LineReader reader = mock(LineReader.class);
        when(reader.readLine("Prompt: ", (Character) null)).thenThrow(new EndOfFileException());
        CliPrompt prompt = new CliPrompt(CliStyle.plain(), mock(Terminal.class), reader);
        Thread.currentThread().interrupt();
        try {
            CliPrompt.Cancelled cancelled = assertThrows(CliPrompt.Cancelled.class, () -> prompt.readLine("Prompt: "));
            assertEquals(130, cancelled.exitCode());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void closePropagatesTerminalIoFailure() throws Exception {
        Terminal terminal = mock(Terminal.class);
        org.mockito.Mockito.doThrow(new IOException("close failed"))
                .when(terminal)
                .close();
        CliPrompt prompt = new CliPrompt(CliStyle.plain(), terminal, mock(LineReader.class));

        IOException failure = assertThrows(IOException.class, prompt::close);

        assertEquals("close failed", failure.getMessage());
    }
}
