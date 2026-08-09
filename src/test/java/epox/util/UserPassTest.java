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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UserPassTest {
    @Test
    void encryptedRepresentationRoundTripsAllCredentialFields() {
        UserPass credentials = new UserPass("alice", "secret", "api-key");

        String encoded = credentials.get(true);

        assertFalse(encoded.contains("secret"));
        assertFalse(encoded.contains("api-key"));
        UserPass decoded = new UserPass(null, null, null);
        decoded.set(encoded);
        assertEquals("alice", decoded.username);
        assertEquals("secret", decoded.password);
        assertEquals("api-key", decoded.apiKey);
        assertEquals("alice", decoded.get(false));
    }

    @Test
    void absentEncryptedFieldsDecodeAsNull() {
        UserPass credentials = new UserPass("alice", "", null);
        assertEquals("alice::", credentials.get(true));

        credentials.set("bob");
        assertEquals("bob", credentials.username);
        assertNull(credentials.password);
        assertNull(credentials.apiKey);
    }

    @Test
    void malformedInputPreservesFieldsAssignedBeforeTheFailure() {
        UserPass credentials = new UserPass("before", "password", "key");

        credentials.set("after:not-valid-base32");

        assertEquals("after", credentials.username);
        assertNull(credentials.password);
        assertNull(credentials.apiKey);

        credentials.set(null);
        assertEquals("after", credentials.username);
    }
}
