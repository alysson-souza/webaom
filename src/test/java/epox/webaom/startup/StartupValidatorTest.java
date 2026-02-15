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

package epox.webaom.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StartupValidatorTest {

    @Test
    void startupValidatorValidateDatabase_warnsOnNonJdbcLikeUrl() {
        List<StartupIssue> issues = StartupValidator.validateDatabase(true, "localhost/webaom");

        assertEquals(1, issues.size());
        StartupIssue issue = issues.getFirst();
        assertEquals(StartupIssue.Severity.WARN, issue.severity());
        assertTrue(issue.message().contains("localhost/webaom"));
        assertTrue(issue.suggestion().contains("jdbc:"));
    }
}
