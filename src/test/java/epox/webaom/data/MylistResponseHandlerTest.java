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

package epox.webaom.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MylistResponseHandlerTest {
    @Test
    void parseReadsStateAndFileStateWhenPresent() {
        Mylist parsed = MylistResponseHandler.parse("1|2|3|4|5|date|1|viewed|storage|source|other|10");
        assertEquals(1, parsed.state);
        assertEquals(10, parsed.filestate);

        Mylist shortResponse = MylistResponseHandler.parse("1|2");
        assertEquals(0, shortResponse.state);
        assertEquals(0, shortResponse.filestate);

        Mylist malformed = MylistResponseHandler.parse("1|2|3|4|5|date|invalid|||||invalid");
        assertEquals(0, malformed.state);
        assertEquals(0, malformed.filestate);
    }

    @Test
    void formatLeavesResponsesWithoutTheMinimumFieldsUntouched() {
        assertEquals("too|short", MylistResponseHandler.format("too|short"));
    }

    @Test
    void formatSingleEntryShowsKnownAndUnknownLocationStates() {
        assertEquals("lid=123 (On HDD)", MylistResponseHandler.format("123|2|3|4|5|date|1"));
        assertEquals("lid=123 (Unknown)", MylistResponseHandler.format("123|2|3|4|5|date|"));
        assertEquals("lid=123 (Unknown)", MylistResponseHandler.format("123|2|3|4|5|date|invalid"));
        assertEquals("lid=123 (Unknown)", MylistResponseHandler.format("123|2|3|4|5|date"));
    }

    @Test
    void formatMultipleEntriesSuppliesDefaultsAndOptionalWatchedCount() {
        assertEquals("Anime - 0/? eps", MylistResponseHandler.format("Anime||x|x|x|"));
        assertEquals("Anime - 8/12 eps, watched: 5", MylistResponseHandler.format("Anime|12|x|x|x|8|5"));
        assertEquals(" - 8/12 eps", MylistResponseHandler.format("|12|x|x|x|8"));
    }

    @Test
    void locationStateHelpersRecognizeTheApiRangeAndLabels() {
        assertFalse(MylistStates.isValidLocation(-1));
        assertTrue(MylistStates.isValidLocation(MylistStates.LOCATION_UNKNOWN));
        assertTrue(MylistStates.isValidLocation(MylistStates.LOCATION_REMOTE));
        assertFalse(MylistStates.isValidLocation(5));
        assertEquals("Unknown", MylistStates.toLocationDisplayName(MylistStates.LOCATION_UNKNOWN));
        assertEquals("On HDD", MylistStates.toLocationDisplayName(MylistStates.LOCATION_HDD));
        assertEquals("On CD", MylistStates.toLocationDisplayName(MylistStates.LOCATION_CD));
        assertEquals("Deleted", MylistStates.toLocationDisplayName(MylistStates.LOCATION_DELETED));
        assertEquals("Remote", MylistStates.toLocationDisplayName(MylistStates.LOCATION_REMOTE));
    }
}
