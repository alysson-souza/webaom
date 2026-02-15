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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParserTest {
    private int originalAssumedEpisodeCount;
    private int originalAssumedSpecialCount;

    @BeforeEach
    void setUp() {
        originalAssumedEpisodeCount = AppContext.assumedEpisodeCount;
        originalAssumedSpecialCount = AppContext.assumedSpecialCount;
        AppContext.assumedEpisodeCount = 99;
        AppContext.assumedSpecialCount = 99;
    }

    @AfterEach
    void tearDown() {
        AppContext.assumedEpisodeCount = originalAssumedEpisodeCount;
        AppContext.assumedSpecialCount = originalAssumedSpecialCount;
    }

    @Test
    void parserPad_numericEpisode_padsBasedOnTotal() {
        assertEquals("01", Parser.pad("1", 12));
    }

    @Test
    void parserPad_specialEpisode_usesSpecialAssumptionAndKeepsPrefix() {
        String result = Parser.pad("S1", 0);

        assertEquals("S01", result);
        assertTrue(result.startsWith("S"));
    }

    @Test
    void parserPad_rangeInput_padsBothSides() {
        assertEquals("01-02", Parser.pad("1-2", 12));
    }
}
