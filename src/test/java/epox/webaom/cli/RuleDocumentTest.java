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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuleDocumentTest {
    // RuleDocument joins rule lines with the platform separator, so expectations must too.
    private static String lines(String... rules) {
        return String.join(System.lineSeparator(), rules);
    }

    @Test
    void roundTripsRenameAndMoveSections() {
        RuleDocument document = RuleDocument.parse("""
            [rename]
            DO ADD '%eng - %enr'

            [move]
            IF T(TV) DO ADD '/Anime/%eng/'
            """);

        assertEquals("DO ADD '%eng - %enr'", document.renameRules());
        assertEquals("IF T(TV) DO ADD '/Anime/%eng/'", document.moveRules());
        assertTrue(document.format().contains("[rename]"));
        assertTrue(document.format().contains("[move]"));
    }

    @Test
    void rejectsConditionalRulesWithoutDo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF A(Test) ADD 'bad'\n"));

        assertTrue(exception.getMessage().contains("line 2"));
        assertTrue(exception.getMessage().contains("missing DO"));
    }

    @Test
    void rejectsUnknownSections() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> RuleDocument.parse("[organize]\nDO ADD 'bad'\n"));

        assertTrue(exception.getMessage().contains("unknown section"));
    }

    @Test
    void rejectsMalformedConditionalExpressions() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF A(foo DO ADD 'x'\n"));

        assertTrue(exception.getMessage().contains("line 2"));
        assertTrue(exception.getMessage().contains("missing closing parenthesis"));
    }

    @Test
    void acceptsNestedParenthesesInConditionalValues() {
        RuleDocument document = RuleDocument.parse("[rename]\nIF A(Naruto (TV));G(!unknown) DO ADD 'x'\n");

        assertEquals("IF A(Naruto (TV));G(!unknown) DO ADD 'x'", document.renameRules());
    }

    @Test
    void normalizesRuleIndentationAcceptedByValidation() {
        RuleDocument document = RuleDocument.parse("[rename]\n  DO ADD 'x'\n\tIF G(!unknown) DO ADD 'y'\n");

        assertEquals(lines("DO ADD 'x'", "IF G(!unknown) DO ADD 'y'"), document.renameRules());
    }

    @Test
    void rejectsUnsupportedConditionTypes() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF B(foo) DO ADD 'x'\n"));

        assertTrue(exception.getMessage().contains("unsupported condition type B"));
    }

    @Test
    void rejectsMalformedParameterizedOperations() {
        IllegalArgumentException assume =
                assertThrows(IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nDO ASSUME nope\n"));
        IllegalArgumentException truncate = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nDO TRUNCATE<garbage\n"));
        IllegalArgumentException embeddedTruncate = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nDO SET 'TRUNCATE<oops,...>'\n"));
        IllegalArgumentException nestedTruncate = assertThrows(
                IllegalArgumentException.class,
                () -> RuleDocument.parse("[rename]\nDO SET 'TRUNCATE<80,TRUNCATE<oops,...>>'\n"));

        assertTrue(assume.getMessage().contains("invalid ASSUME value"));
        assertTrue(truncate.getMessage().contains("invalid TRUNCATE value"));
        assertTrue(embeddedTruncate.getMessage().contains("invalid TRUNCATE value"));
        assertTrue(nestedTruncate.getMessage().contains("invalid TRUNCATE value"));
    }

    @Test
    void acceptsValidParameterizedOperations() {
        RuleDocument document = RuleDocument.parse("[rename]\n"
                + "DO ASSUME SPECIAL 12\n"
                + "DO TRUNCATE<200,...> // keep it short\n"
                + "DO SET 'TRUNCATE<80,...>'\n"
                + "DO ADD 'x' WEIGHT 1.5:200\n");

        assertEquals(
                lines(
                        "DO ASSUME SPECIAL 12",
                        "DO TRUNCATE<200,...> // keep it short",
                        "DO SET 'TRUNCATE<80,...>'",
                        "DO ADD 'x' WEIGHT 1.5:200"),
                document.renameRules());
    }

    @Test
    void rejectsMalformedWeightClauses() {
        IllegalArgumentException weight = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nDO ADD 'x' WEIGHT nope:200\n"));
        IllegalArgumentException maxLength = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nDO ADD 'x' WEIGHT 1.5:bad\n"));
        IllegalArgumentException separator = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nDO ADD 'x' WEIGHT 1.5\n"));

        assertTrue(weight.getMessage().contains("invalid WEIGHT value"));
        assertTrue(maxLength.getMessage().contains("invalid WEIGHT value"));
        assertTrue(separator.getMessage().contains("invalid WEIGHT value"));
    }

    @Test
    void acceptsWeightTextInsideQuotedContent() {
        RuleDocument document = RuleDocument.parse("[rename]\nDO ADD 'light weight class'\nDO ADD 'weight 1:200'\n");

        assertEquals(lines("DO ADD 'light weight class'", "DO ADD 'weight 1:200'"), document.renameRules());
    }

    @Test
    void rejectsConditionTypesWithoutTheirRequiredPairs() {
        IllegalArgumentException unequal = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF U(foo) DO ADD 'x'\n"));
        IllegalArgumentException regex = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF Z(foo) DO ADD 'x'\n"));

        assertTrue(unequal.getMessage().contains("U requires two values"));
        assertTrue(regex.getMessage().contains("Z requires two values"));
    }

    @Test
    void rejectsInvalidRegularExpressionsInConditions() {
        IllegalArgumentException anime = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF A([) DO ADD 'x'\n"));
        IllegalArgumentException tag = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF Z(eng:[) DO ADD 'x'\n"));

        assertTrue(anime.getMessage().contains("invalid regular expression"));
        assertTrue(tag.getMessage().contains("invalid regular expression"));
    }

    @Test
    void acceptsTypeSpecificConditionValues() {
        RuleDocument document =
                RuleDocument.parse("[rename]\nIF U(eng:ann);L(src:qua);Z(eng:^Anime);A(123) DO ADD 'x'\n");

        assertEquals("IF U(eng:ann);L(src:qua);Z(eng:^Anime);A(123) DO ADD 'x'", document.renameRules());
    }

    @Test
    void acceptsUnrecognizedLinesTheEngineSkips() {
        // Rules.buildSections silently skips lines that start with none of DO/IF/ELSE IF/ELSE DO,
        // so rejecting them here would fail documents that rename correctly.
        RuleDocument document = RuleDocument.parse("[rename]\nDO ADD 'x'\nSOMETHING ELSE ENTIRELY\n");

        assertTrue(document.renameRules().contains("DO ADD 'x'"));
        assertTrue(document.renameRules().contains("SOMETHING ELSE ENTIRELY"));
    }

    @Test
    void acceptsEmptyConditionGroupsAndValuesTheEngineDrops() {
        // The engine tokenizes conditions on ';' and values on ',', which drops empty entries.
        RuleDocument document = RuleDocument.parse("[rename]\nIF G(!unknown,);A(Test); DO ADD 'x'\n");

        assertEquals("IF G(!unknown,);A(Test); DO ADD 'x'", document.renameRules());
    }

    @Test
    void acceptsLowercaseDoInsideConditionValues() {
        // The separator search must ignore " do " inside parentheses; otherwise the condition is
        // truncated mid-value and a working rule fails validation.
        RuleDocument document = RuleDocument.parse("[rename]\nIF E(a do b) DO ADD 'x'\n");

        assertEquals("IF E(a do b) DO ADD 'x'", document.renameRules());
    }

    @Test
    void stillRejectsUnbalancedParenthesesRatherThanReportingMissingDo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> RuleDocument.parse("[rename]\nIF A(foo DO ADD 'x'\n"));

        assertTrue(exception.getMessage().contains("missing closing parenthesis"));
    }
}
