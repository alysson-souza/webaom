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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class CliCommandValidationCharacterizationTest {
    @Test
    void commandGroupsPrintTheirOwnUsageWithoutStartingApplicationServices() {
        for (String group : new String[] {"mylist", "rules", "config", "db"}) {
            Invocation invocation = invoke(group);
            assertEquals(0, invocation.exitCode(), group);
            assertTrue(invocation.out().contains("Usage: webaom " + group), group);
            assertTrue(invocation.err().isEmpty(), group);
        }
    }

    @Test
    void versionProviderReportsTheApplicationVersion() {
        Invocation invocation = invoke("--version");

        assertEquals(0, invocation.exitCode());
        assertTrue(invocation.out().startsWith("WebAOM "));
        assertTrue(invocation.err().isEmpty());
    }

    @Test
    void invalidSubcommandAndMissingParametersReturnUsageErrors() {
        Invocation unknown = invoke("config", "unknown");
        Invocation missingPath = invoke("rename");
        Invocation missingOptionValue = invoke("rename", "--rules");

        assertEquals(2, unknown.exitCode());
        assertTrue(unknown.err().contains("Unmatched argument"));
        assertEquals(2, missingPath.exitCode());
        assertTrue(missingPath.err().contains("Missing required parameter"));
        assertEquals(2, missingOptionValue.exitCode());
        assertTrue(missingOptionValue.err().contains("Missing required parameter"));
    }

    @Test
    void invalidFileFormatAndMylistStateAreRejectedBeforeAnyIo() {
        Invocation format = invoke("hash", "--format", "yaml", "not-opened.mkv");
        Invocation state = invoke("mylist", "add", "--state", "floppy", "not-opened.mkv");

        assertEquals(2, format.exitCode());
        assertTrue(format.err().contains("Invalid value for option '--format'"));
        assertEquals(2, state.exitCode());
        assertTrue(state.err().contains("Invalid value for option '--state'"));
    }

    @Test
    void separatedStylingOptionsAreAppliedToHelpButNeverToErrorsAfterTheTerminator() {
        Invocation styled = invoke("--color", "always", "--hyperlinks", "never", "--help");
        Invocation terminated = invoke("config", "show", "--", "--color", "always");

        assertEquals(0, styled.exitCode());
        assertTrue(styled.out().contains("\u001B["));
        assertEquals(2, terminated.exitCode());
        assertFalse(terminated.err().contains("\u001B["));
    }

    private static Invocation invoke(String... arguments) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exitCode = Cli.execute(new PrintWriter(out, true), new PrintWriter(err, true), arguments);
        return new Invocation(exitCode, out.toString(), err.toString());
    }

    private record Invocation(int exitCode, String out, String err) {}
}
