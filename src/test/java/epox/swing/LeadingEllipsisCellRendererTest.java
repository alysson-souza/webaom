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

package epox.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Font;
import java.awt.FontMetrics;
import org.junit.jupiter.api.Test;

class LeadingEllipsisCellRendererTest {
    /** Every char is 10 pixels wide, so widths are easy to reason about. */
    private static final FontMetrics FIXED_WIDTH = new FontMetrics(new Font(Font.MONOSPACED, Font.PLAIN, 12)) {
        @Override
        public int stringWidth(String text) {
            return text.length() * 10;
        }
    };

    @Test
    void keepTail_neverStartsTheTailInsideASurrogatePair() {
        String text = "xxxxx😀end";

        // Room for the ellipsis and four chars, which would begin with the emoji's low surrogate
        assertEquals("…end", LeadingEllipsisCellRenderer.keepTail(text, FIXED_WIDTH, 50));
    }

    @Test
    void keepTail_leavesEmptyTextEmptyInAColumnNarrowerThanItsInsets() {
        assertEquals("", LeadingEllipsisCellRenderer.keepTail("", FIXED_WIDTH, -2));
    }
}
