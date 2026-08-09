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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AttributeMapTest {
    @Test
    void putConvertsNumbersAndIgnoresNullValues() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("name", "value");
        attributes.put("int", 42);
        attributes.put("long", 4_000_000_000L);

        assertNull(attributes.put("ignored", (String) null));
        assertNull(attributes.put("name", (String) null));
        assertEquals("value", attributes.get("name"));
        assertEquals("42", attributes.get("int"));
        assertEquals("4000000000", attributes.get("long"));
        assertEquals(3, attributes.size());
    }

    @Test
    void toArrayContainsEveryEntryIndependentOfHashIterationOrder() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("first", "one");
        attributes.put("second", "two");

        Map<String, String> copied =
                Arrays.stream(attributes.toArray()).collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));

        assertEquals(Map.of("first", "one", "second", "two"), copied);
    }
}
