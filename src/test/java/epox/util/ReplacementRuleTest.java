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

package epox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.Options;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ReplacementRuleTest {
    @Test
    void parseAndMutatorsPreserveEnabledAndEmptyDestinationEncoding() {
        ReplacementRule enabled = ReplacementRule.parse("source", "\\0");
        assertEquals("source", enabled.getSource());
        assertEquals("", enabled.getDestination());
        assertTrue(enabled.isEnabled());
        assertEquals("source" + Options.FIELD_SEPARATOR + "\\0", enabled.toString());

        ReplacementRule disabled = ReplacementRule.parse("#old", "new");
        assertFalse(disabled.isEnabled());
        assertEquals("#old" + Options.FIELD_SEPARATOR + "new", disabled.toString());

        disabled.setSource("");
        disabled.setDestination("destination");
        disabled.setEnabled(true);
        assertTrue(disabled.hasEmptySource());
        assertEquals("destination", disabled.getDestination());
    }

    @Test
    void encodeAndDecodeRoundTripNonEmptyRulesAndClearTheTargetList() {
        List<ReplacementRule> rules = List.of(
                new ReplacementRule("a", "b", true),
                new ReplacementRule("", "ignored", true),
                new ReplacementRule("c", "", false));

        String encoded = ReplacementRule.encode(rules);
        assertEquals(
                "a" + Options.FIELD_SEPARATOR + "b" + Options.FIELD_SEPARATOR + "#c" + Options.FIELD_SEPARATOR + "\\0"
                        + Options.FIELD_SEPARATOR,
                encoded);

        List<ReplacementRule> decoded = new ArrayList<>();
        decoded.add(new ReplacementRule("stale", "value", true));
        ReplacementRule.decode(decoded, encoded);
        assertEquals(2, decoded.size());
        assertEquals("a", decoded.get(0).getSource());
        assertFalse(decoded.get(1).isEnabled());
        assertEquals("", decoded.get(1).getDestination());
    }

    @Test
    void decodeRejectsAnUnpairedTokenAfterClearingExistingRules() {
        List<ReplacementRule> rules = new ArrayList<>();
        rules.add(new ReplacementRule("stale", "value", true));

        // TODO: Review whether a truncated persisted rule should be ignored instead of aborting the entire decode.
        assertThrows(NoSuchElementException.class, () -> ReplacementRule.decode(rules, "source"));
        assertTrue(rules.isEmpty());
    }
}
