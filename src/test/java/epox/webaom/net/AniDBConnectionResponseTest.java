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

package epox.webaom.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class AniDBConnectionResponseTest {
    @Test
    void parsesTaggedResponseAndDecodesNumericHtmlData() throws Exception {
        AniDBConnectionResponse response = new AniDBConnectionResponse("t00001", 5, "t00001 220 FILE\nTitle &#65;");

        assertEquals("t00001", response.tag);
        assertEquals(220, response.code);
        assertEquals("FILE", response.message);
        assertEquals("Title A", response.data);
        assertEquals("220 FILE [Title A]", response.toString());
    }

    @Test
    void mismatchedTagIsRejectedButAnUntaggedReplySkipsValidation() throws Exception {
        assertThrows(TagMismatchException.class, () -> new AniDBConnectionResponse("t00001", 5, "t00002 220 FILE"));

        // TODO: Review whether an expected tag should be required when the server reply omits one.
        AniDBConnectionResponse untagged = new AniDBConnectionResponse("t00001", 5, "220 FILE");
        assertNull(untagged.tag);
        assertEquals(220, untagged.code);
    }

    @Test
    void loginAndEncryptionRepliesSplitTheirLeadingDataFromTheMessage() throws Exception {
        AniDBConnectionResponse login = new AniDBConnectionResponse(null, 0, "200 SESSION LOGIN ACCEPTED");
        assertEquals("SESSION", login.data);
        assertEquals("LOGIN ACCEPTED", login.message);

        AniDBConnectionResponse newerLogin = new AniDBConnectionResponse(null, 0, "201 NEWSID LOGIN ACCEPTED");
        assertEquals("NEWSID", newerLogin.data);
        assertEquals("LOGIN ACCEPTED", newerLogin.message);

        AniDBConnectionResponse encryption = new AniDBConnectionResponse(null, 0, "209 SALT ENCRYPTION ENABLED");
        assertEquals("SALT", encryption.data);
        assertEquals("ENCRYPTION ENABLED", encryption.message);
    }

    @Test
    void ordinarySingleLineAndMultilineRepliesKeepTheirLegacyShape() throws Exception {
        AniDBConnectionResponse singleLine = new AniDBConnectionResponse(null, 0, "203 LOGGED OUT");
        assertEquals("LOGGED OUT", singleLine.message);
        assertNull(singleLine.data);
        assertEquals("203 LOGGED OUT [null]", singleLine.toString());

        AniDBConnectionResponse multiline = new AniDBConnectionResponse(null, 0, "210 ADDED\n1234");
        assertEquals("ADDED", multiline.message);
        assertEquals("1234", multiline.data);
    }

    @Test
    void malformedAndServerFailureRepliesMapToCurrentExceptions() {
        AniDBException malformed =
                assertThrows(AniDBException.class, () -> new AniDBConnectionResponse(null, 0, "abc response"));
        assertTrue(malformed.is(AniDBException.ANIDB_SERVER_ERROR));
        assertEquals("Unexpected response", malformed.getMessage());

        // TODO: Review whether a truncated response should map to AniDBException like other malformed replies.
        assertThrows(StringIndexOutOfBoundsException.class, () -> new AniDBConnectionResponse(null, 0, "2"));

        AniDBException serverFailure =
                assertThrows(AniDBException.class, () -> new AniDBConnectionResponse(null, 0, "601 OUT OF SERVICE"));
        assertTrue(serverFailure.is(AniDBException.ANIDB_SERVER_ERROR));
        assertEquals("601 OUT OF SERVICE", serverFailure.getMessage());

        assertThrows(SocketTimeoutException.class, () -> new AniDBConnectionResponse(null, 0, "602 SERVER BUSY"));
    }

    @Test
    void protocolErrorCodesMapToTheirEstablishedClientCategories() {
        AniDBException banned =
                assertThrows(AniDBException.class, () -> new AniDBConnectionResponse(null, 0, "555 BANNED\nreason"));
        assertTrue(banned.is(AniDBException.CLIENT_USER));
        assertEquals("Banned: reason", banned.getMessage());

        AniDBException unknownBanReason =
                assertThrows(AniDBException.class, () -> new AniDBConnectionResponse(null, 0, "555 BANNED"));
        assertEquals("Banned: Unknown", unknownBanReason.getMessage());

        assertExceptionCode("502 ACCESS DENIED", AniDBException.CLIENT_USER);
        assertExceptionCode("503 CLIENT VERSION OUTDATED", AniDBException.CLIENT_OUTDATED);
        assertExceptionCode("504 CLIENT BANNED", AniDBException.CLIENT_BANNED);
        assertExceptionCode("505 ILLEGAL INPUT", AniDBException.CLIENT_BUG);
    }

    @Test
    void exceptionDefaultsAndConnectionSettingsExposeTheirConfiguredValues() {
        assertEquals(
                "This version of WebAOM is no longer supported! Check WIKI.",
                AniDBException.defaultMsg(AniDBException.CLIENT_BANNED));
        assertEquals(
                "A new version of WebAOM is out. Check WIKI!",
                AniDBException.defaultMsg(AniDBException.CLIENT_OUTDATED));
        assertEquals(
                "AniDB is out of service! Try again next year.",
                AniDBException.defaultMsg(AniDBException.ANIDB_OUT_OF_SERVICE));
        assertEquals("Internal Server Error @ AniDB!", AniDBException.defaultMsg(AniDBException.ANIDB_SERVER_ERROR));
        assertEquals(
                "Decryption failed. The connection timed out (most likely).",
                AniDBException.defaultMsg(AniDBException.ENCRYPTION));
        assertEquals("Unknown Error.", AniDBException.defaultMsg(AniDBException.CLIENT_SYSTEM));

        AniDBConnectionSettings settings = new AniDBConnectionSettings("host", 9000, 1234, 20, 4000, 3, true);
        assertEquals("host", settings.host);
        assertEquals(9000, settings.remotePort);
        assertEquals(1234, settings.localPort);
        assertEquals(20_000, settings.timeoutMillis);
        assertEquals(4000, settings.packetDelay);
        assertEquals(3, settings.maxTimeouts);
        assertTrue(settings.natEnabled);
        settings.packetDelay = 4500;
        assertEquals(4500, settings.packetDelay);
    }

    private static void assertExceptionCode(String rawResponse, int expectedCode) {
        AniDBException exception =
                assertThrows(AniDBException.class, () -> new AniDBConnectionResponse(null, 0, rawResponse));
        assertTrue(exception.is(expectedCode));
    }
}
