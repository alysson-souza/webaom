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

import epox.webaom.Rules;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Reads and validates the editable two-section rules document. */
public record RuleDocument(String renameRules, String moveRules) {
    private static final String TRUNCATE_PREFIX = "TRUNCATE<";
    private static final Set<Character> CONDITION_TYPES =
            Set.of('A', 'C', 'D', 'E', 'G', 'I', 'L', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'X', 'Y', 'Z');

    public static RuleDocument fromRules(Rules rules) {
        return new RuleDocument(rules.getRenameRules(), rules.getMoveRules());
    }

    public static RuleDocument parse(String text) {
        StringBuilder rename = new StringBuilder();
        StringBuilder move = new StringBuilder();
        StringBuilder current = null;
        List<String> errors = new ArrayList<>();

        String[] lines = text.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("[rename]")) {
                current = rename;
                continue;
            }
            if (trimmed.equalsIgnoreCase("[move]")) {
                current = move;
                continue;
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                errors.add("line " + (index + 1) + ", column 1: unknown section " + trimmed);
                continue;
            }
            if (current == null) {
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    errors.add("line " + (index + 1) + ", column 1: rule appears before [rename] or [move]");
                }
                continue;
            }
            current.append(trimmed).append(System.lineSeparator());
            validateRuleLine(trimmed, index + 1, errors);
        }
        if (errors.isEmpty() && rename.isEmpty() && move.isEmpty()) {
            errors.add("document must contain a [rename] or [move] section");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(System.lineSeparator(), errors));
        }
        return new RuleDocument(
                rename.toString().stripTrailing(), move.toString().stripTrailing());
    }

    private static void validateRuleLine(String line, int number, List<String> errors) {
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        String upper = line.toUpperCase(Locale.ROOT);
        String operation;
        if (upper.startsWith("DO ")) {
            operation = upper.substring(3).trim();
        } else if (upper.startsWith("IF ") || upper.startsWith("ELSE IF ")) {
            int conditionStart = upper.startsWith("ELSE IF ") ? 8 : 3;
            int doIndex = Rules.indexOfConditionSeparator(upper, conditionStart);
            if (doIndex < 0) {
                errors.add("line " + number + ", column " + (line.length() + 1) + ": conditional rule is missing DO");
                return;
            }
            validateCondition(line.substring(conditionStart, doIndex), number, errors);
            operation = upper.substring(doIndex + 4).trim();
        } else if (upper.startsWith("ELSE DO ")) {
            operation = upper.substring(8).trim();
        } else {
            // Rules.buildSections skips lines it does not recognise, so rejecting them here would
            // fail documents that rename correctly.
            return;
        }

        if (operation.startsWith("ASSUME ")) {
            validateAssume(operation, number, errors);
        } else if (operation.startsWith(TRUNCATE_PREFIX)) {
            validateTruncate(operation, number, errors);
        } else if (operation.startsWith("ADD ")
                || operation.startsWith("SET ")
                || operation.equals("FAIL")
                || operation.equals("FINISH")
                || operation.startsWith("FINISH ")
                || operation.startsWith("RETURN ")) {
            validateEmbeddedTruncates(operation, number, errors);
            validateWeight(operation, number, errors);
        } else {
            errors.add("line " + number + ", column 1: unsupported operation");
        }
    }

    private static void validateAssume(String operation, int number, List<String> errors) {
        String value = operation.startsWith("ASSUME SPECIAL ")
                ? operation.substring("ASSUME SPECIAL ".length())
                : operation.substring("ASSUME ".length());
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            errors.add("line " + number + ", column 1: invalid ASSUME value");
        }
    }

    private static void validateTruncate(String operation, int number, List<String> errors) {
        int closeIndex = validateTruncateAt(operation, 0, number, errors);
        String trailing =
                closeIndex < 0 ? "" : operation.substring(closeIndex + 1).trim();
        if (closeIndex >= 0 && !trailing.isEmpty() && !trailing.startsWith("//")) {
            errors.add("line " + number + ", column 1: invalid TRUNCATE value");
        }
    }

    private static void validateEmbeddedTruncates(String operation, int number, List<String> errors) {
        int markerIndex = operation.indexOf(TRUNCATE_PREFIX);
        while (markerIndex >= 0) {
            int closeIndex = validateTruncateAt(operation, markerIndex, number, errors);
            if (closeIndex < 0) {
                return;
            }
            markerIndex = operation.indexOf(TRUNCATE_PREFIX, markerIndex + TRUNCATE_PREFIX.length());
        }
    }

    private static void validateWeight(String operation, int number, List<String> errors) {
        try {
            Rules.parseWeightClause(operation);
        } catch (IllegalArgumentException exception) {
            errors.add("line " + number + ", column 1: invalid WEIGHT value; expected weight:maxLength");
        }
    }

    private static int validateTruncateAt(String operation, int markerIndex, int number, List<String> errors) {
        int valueStart = markerIndex + TRUNCATE_PREFIX.length();
        int commaIndex = operation.indexOf(',', valueStart);
        int closeIndex = operation.indexOf('>', valueStart);
        if (commaIndex <= valueStart || closeIndex <= commaIndex) {
            errors.add("line " + number + ", column 1: invalid TRUNCATE value");
            return -1;
        }
        try {
            int maxBytes = Integer.parseInt(operation.substring(valueStart, commaIndex));
            if (maxBytes <= 0 || maxBytes > 10_000) {
                errors.add("line " + number + ", column 1: invalid TRUNCATE value");
                return -1;
            }
        } catch (NumberFormatException exception) {
            errors.add("line " + number + ", column 1: invalid TRUNCATE value");
            return -1;
        }
        return closeIndex;
    }

    private static void validateCondition(String condition, int number, List<String> errors) {
        for (String group : condition.split(";", -1)) {
            String expression = group.trim();
            if (expression.isEmpty()) {
                // The engine tokenizes on ';' and drops empty groups, so a trailing or doubled
                // separator is harmless.
                continue;
            }

            int openIndex = expression.indexOf('(');
            char conditionType = Character.toUpperCase(expression.charAt(0));
            if (openIndex < 1
                    || !Character.isLetter(conditionType)
                    || !expression.substring(1, openIndex).isBlank()) {
                addConditionError(number, errors, "expected a condition such as A(title)");
                return;
            }
            if (!CONDITION_TYPES.contains(conditionType)) {
                addConditionError(number, errors, "unsupported condition type " + conditionType);
                return;
            }

            int closeIndex = findClosingParenthesis(expression, openIndex);
            if (closeIndex < 0) {
                addConditionError(number, errors, "missing closing parenthesis");
                return;
            }
            if (!expression.substring(closeIndex + 1).isBlank()) {
                addConditionError(number, errors, "unexpected text after condition");
                return;
            }

            String values = expression.substring(openIndex + 1, closeIndex);
            if (values.isBlank()) {
                addConditionError(number, errors, "condition value is empty");
                return;
            }
            for (String value : values.split(",", -1)) {
                String normalizedValue = value.trim();
                if (normalizedValue.isEmpty()) {
                    // The engine tokenizes on ',' and drops empty values.
                    continue;
                }
                if (normalizedValue.startsWith("!")) {
                    normalizedValue = normalizedValue.substring(1);
                }
                if (normalizedValue.isBlank()) {
                    addConditionError(number, errors, "condition value is empty");
                    return;
                }
                if (!validateConditionValue(conditionType, normalizedValue, number, errors)) {
                    return;
                }
            }
        }
    }

    private static boolean validateConditionValue(char conditionType, String value, int number, List<String> errors) {
        if (conditionType == 'U' || conditionType == 'L' || conditionType == 'Z') {
            String[] pair = value.split(":", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                addConditionError(number, errors, conditionType + " requires two values separated by ':'");
                return false;
            }
            if (conditionType == 'Z') {
                return validateRegex(pair[1], number, errors);
            }
            return true;
        }
        if (conditionType == 'E' || conditionType == 'P') {
            return validateRegex(value, number, errors);
        }
        if (conditionType == 'A') {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException exception) {
                return validateRegex(value, number, errors);
            }
        }
        return true;
    }

    private static boolean validateRegex(String regex, int number, List<String> errors) {
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException exception) {
            addConditionError(number, errors, "invalid regular expression: " + exception.getDescription());
            return false;
        }
    }

    private static int findClosingParenthesis(String expression, int openIndex) {
        int depth = 0;
        for (int index = openIndex; index < expression.length(); index++) {
            char character = expression.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static void addConditionError(int number, List<String> errors, String detail) {
        errors.add("line " + number + ", column 1: invalid condition: " + detail);
    }

    public String format() {
        return "[rename]" + System.lineSeparator()
                + renameRules.stripTrailing()
                + System.lineSeparator()
                + System.lineSeparator()
                + "[move]"
                + System.lineSeparator()
                + moveRules.stripTrailing()
                + System.lineSeparator();
    }
}
