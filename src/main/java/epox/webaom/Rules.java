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

import epox.util.ReplacementRule;
import epox.util.StringUtilities;
import epox.webaom.data.AttributeMap;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;

public class Rules {
    public static final int M_NOREN = 0;
    public static final int M_RULES = 1;
    public static final int M_ANIDB = 2;
    public static final int M_FALLB = 3;
    public static final String TRUNC = "TRUNCATE<";

    /** Maximum filename length in bytes (filesystem limit). */
    public static final int MAX_FILENAME_BYTES = 255;
    /** Safe filename length with margin for UTF-8 multi-byte chars. */
    public static final int SAFE_FILENAME_LENGTH = 200;

    /** Parsed trailing weight metadata for a rule section. */
    public record WeightClause(int startIndex, float weight, int maxLength) {}

    /** Characters to replace in filenames (illegal filesystem characters) */
    private final List<ReplacementRule> illegalCharReplacements;

    private String renameRulesScript;
    private String moveRulesScript;
    private AttributeMap tagValueMap = null;

    public Rules() {
        illegalCharReplacements = new ArrayList<>();
        illegalCharReplacements.add(new ReplacementRule("`", "'", true));
        illegalCharReplacements.add(new ReplacementRule("\"", "", true));
        illegalCharReplacements.add(new ReplacementRule("<", "", true));
        illegalCharReplacements.add(new ReplacementRule(">", "", true));
        illegalCharReplacements.add(new ReplacementRule("|", "", true));
        illegalCharReplacements.add(new ReplacementRule("/", "", true));
        illegalCharReplacements.add(new ReplacementRule(":", "", true));
        illegalCharReplacements.add(new ReplacementRule("\\", "", true));
        illegalCharReplacements.add(new ReplacementRule("?", "", true));
        illegalCharReplacements.add(new ReplacementRule("*", "", true));

        renameRulesScript = """
            #Uncomment to enable:
            #DO SET 'TRUNCATE<200,%ann (%yea) - %enr - %epn [%src-%res][%aud][%dub][%vid]-%grp>'
            """;
        moveRulesScript = "#MOVE";
    }

    private static void printError(String message) {
        if (AppContext.gui != null) {
            AppContext.gui.println(HyperlinkBuilder.formatAsError(message));
        } else {
            System.err.println(message);
        }
    }

    public List<ReplacementRule> getIllegalCharReplacements() {
        return illegalCharReplacements;
    }

    private static boolean matchesPattern(String text, String pattern) {
        if (text == null) {
            return false;
        }
        try {
            return text.matches(pattern);
        } catch (PatternSyntaxException e) {
            AppContext.dialog("Error", e.getMessage());
            return false;
        }
    }

    private static boolean containsIgnoreCase(String text, String searchTerm) {
        return text.toLowerCase().contains(searchTerm.toLowerCase());
    }

    private static String applyReplacements(String text, List<ReplacementRule> replacements) {
        for (ReplacementRule replacement : replacements) {
            if (replacement.isEnabled()) {
                text = text.replace(replacement.getSource(), replacement.getDestination());
            }
        }
        return text;
    }

    /**
     * Checks if a filename exceeds the filesystem byte limit.
     *
     * @param filename the filename to check (without path)
     * @return true if the filename is too long for the filesystem
     */
    public static boolean isFilenameTooLong(String filename) {
        return filename.getBytes(StandardCharsets.UTF_8).length > MAX_FILENAME_BYTES;
    }

    /**
     * Gets the byte length of a filename in UTF-8 encoding.
     *
     * @param filename the filename to measure
     * @return the byte length
     */
    public static int getFilenameByteLength(String filename) {
        return filename.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Smart-truncates a filename to fit within the specified byte limit,
     * preserving the file extension and adding an ellipsis indicator.
     *
     * @param filename the filename to truncate
     * @param maxBytes the maximum byte length (typically 255)
     * @return the truncated filename
     */
    public static String smartTruncateFilename(String filename, int maxBytes) {
        if (!isFilenameTooLong(filename)) {
            return filename;
        }

        int dotIndex = filename.lastIndexOf('.');
        String baseName;
        String extension;
        // Handle dotfiles (.hidden) and ensure there's actual content after the dot
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            baseName = filename.substring(0, dotIndex);
            extension = filename.substring(dotIndex);
        } else {
            baseName = filename;
            extension = "";
        }

        String ellipsis = "...";
        int ellipsisBytes = ellipsis.getBytes(StandardCharsets.UTF_8).length;
        int extensionBytes = extension.getBytes(StandardCharsets.UTF_8).length;
        int availableBytes = maxBytes - extensionBytes - ellipsisBytes;

        // If extension is too long, truncate it to a reasonable size
        if (availableBytes < 20) {
            int maxExtBytes = Math.min(extensionBytes, 10);
            extension = truncateToBytes(extension, maxExtBytes);
            extensionBytes = extension.getBytes(StandardCharsets.UTF_8).length;
            availableBytes = maxBytes - extensionBytes - ellipsisBytes;
        }

        String truncatedBase = truncateToBytes(baseName, availableBytes);
        return truncatedBase + ellipsis + extension;
    }

    /**
     * Smart-truncates a filename while preserving a multi-part suffix such as {@code .en.forced.srt}.
     *
     * @param baseName the filename portion before the suffix
     * @param suffix the complete suffix to preserve
     * @param maxBytes the maximum byte length
     * @return the filename fitted to the byte limit
     */
    public static String smartTruncateFilenamePreservingSuffix(String baseName, String suffix, int maxBytes) {
        String filename = baseName + suffix;
        if (filename.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return filename;
        }

        String ellipsis = "...";
        int suffixBytes = suffix.getBytes(StandardCharsets.UTF_8).length;
        int availableBytes = maxBytes - suffixBytes - ellipsis.getBytes(StandardCharsets.UTF_8).length;
        if (availableBytes <= 0) {
            return smartTruncateFilename(filename, maxBytes);
        }
        return truncateToBytes(baseName, availableBytes) + ellipsis + suffix;
    }

    /**
     * Finds the {@code " DO "} separator that ends a conditional rule's condition list.
     *
     * <p>Occurrences inside parentheses are ignored so that condition values and regular
     * expressions may themselves contain the word "do".
     *
     * @param upperInput the rule line, already upper-cased
     * @param fromIndex the offset at which the condition list starts
     * @return the index of the separator, or -1 when the line has no {@code DO}
     */
    public static int indexOfConditionSeparator(String upperInput, int fromIndex) {
        int depth = 0;
        for (int index = fromIndex; index <= upperInput.length() - 4; index++) {
            char character = upperInput.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                if (depth > 0) {
                    depth--;
                }
            } else if (depth == 0 && upperInput.startsWith(" DO ", index)) {
                return index;
            }
        }
        // Unbalanced parentheses leave the scan stuck inside a group; fall back to the first
        // separator so malformed conditions are still split, and reported, as they always were.
        return upperInput.indexOf(" DO ", fromIndex);
    }

    /**
     * Parses an optional trailing {@code WEIGHT number:number} clause outside quoted rule text.
     *
     * @param input a rule operation or section
     * @return the parsed clause, or {@code null} when no clause is present
     * @throws IllegalArgumentException when a clause is present but malformed
     */
    public static WeightClause parseWeightClause(String input) {
        int weightIndex = findWeightClause(input);
        if (weightIndex < 0) {
            return null;
        }
        String value = input.substring(weightIndex + " WEIGHT ".length()).trim();
        int colonIndex = value.indexOf(':');
        if (colonIndex <= 0 || colonIndex == value.length() - 1) {
            throw new IllegalArgumentException("expected weight:maxLength");
        }
        try {
            float weight = Float.parseFloat(value.substring(0, colonIndex));
            int maxLength = Integer.parseInt(value.substring(colonIndex + 1));
            return new WeightClause(weightIndex, weight, maxLength);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("expected weight:maxLength", exception);
        }
    }

    static int findWeightClause(String input) {
        boolean quoted = false;
        int markerLength = " WEIGHT ".length();
        for (int index = 0; index <= input.length() - markerLength; index++) {
            if (input.charAt(index) == '\'') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && input.regionMatches(true, index, " WEIGHT ", 0, markerLength)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Truncates a string to fit within the specified byte limit in UTF-8,
     * ensuring we don't cut in the middle of a multi-byte character.
     */
    private static String truncateToBytes(String str, int maxBytes) {
        if (str.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return str;
        }

        // Binary search for the right length
        int low = 0;
        int high = str.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (str.substring(0, mid).getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return str.substring(0, low);
    }

    private static String applyTruncation(String text) {
        try {
            String suffix;
            int truncateStart = text.indexOf(TRUNC);
            int closeIndex;
            int commaIndex;
            int maxBytes;
            while (truncateStart >= 0) {
                closeIndex = text.indexOf('>', truncateStart);
                if (closeIndex < truncateStart) {
                    break;
                }
                commaIndex = text.indexOf(',', truncateStart);
                if (commaIndex < truncateStart || commaIndex > closeIndex) {
                    break;
                }
                maxBytes = StringUtilities.i(text.substring(truncateStart + TRUNC.length(), commaIndex));
                if (maxBytes <= 0 || maxBytes > 10000) {
                    break;
                }
                suffix = text.substring(commaIndex + 1, closeIndex);
                String prefix = text.substring(0, truncateStart);
                String tail = text.substring(closeIndex + 1);
                int prefixBytes = prefix.getBytes(StandardCharsets.UTF_8).length;
                int availableForSuffix = maxBytes - prefixBytes;
                if (availableForSuffix > 0) {
                    suffix = truncateToBytes(suffix, availableForSuffix);
                } else {
                    suffix = "";
                }
                text = prefix + suffix + tail;
                truncateStart = text.indexOf(TRUNC);
            }
        } catch (NumberFormatException e) {
            printError("Truncation error: " + e.getMessage());
        }
        return text;
    }

    public String getMoveRules() {
        return moveRulesScript;
    }

    public void setMoveRules(String rules) {
        moveRulesScript = rules;
    }

    public String getRenameRules() {
        return renameRulesScript;
    }

    public void setRenameRules(String rules) {
        renameRulesScript = rules;
    }

    private String replaceOldTags(String script) {
        script = script.replace("%year", "%yea");
        script = script.replace("%type", "%typ");
        script = script.replace("%qual", "%qua");
        script = script.replace("%ed2k", "%ed2");
        return script;
    }

    public void loadFromOptions(Options options) {
        renameRulesScript = replaceOldTags(options.getString(Options.STR_RENAME_RULES));
        moveRulesScript = replaceOldTags(options.getString(Options.STR_MOVE_RULES));
        ReplacementRule.decode(illegalCharReplacements, options.getString(Options.STR_REPLACE_RULES));
    }

    public void saveToOptions(Options options) {
        options.setString(Options.STR_RENAME_RULES, renameRulesScript);
        options.setString(Options.STR_MOVE_RULES, moveRulesScript);
        options.setString(Options.STR_REPLACE_RULES, ReplacementRule.encode(illegalCharReplacements));
    }

    public File apply(Job job) {
        if (job == null || job.anidbFile == null) {
            return null;
        }

        String path = job.currentFile.getParent();
        String name = job.currentFile.getName();

        if (job.anidbFile.getAnime() != null) {
            tagValueMap = job.genMap();
            String renameResult = processRulesScript(job, renameRulesScript);
            String moveResult = processRulesScript(job, moveRulesScript);
            tagValueMap = null;

            if (moveResult != null) {
                String movePath = finalizeFilename(job, moveResult);
                if (movePath.startsWith("." + File.separator)) {
                    path += movePath.substring(1);
                } else {
                    path = movePath;
                }
            }
            if (renameResult != null) {
                name = finalizeFilename(job, renameResult) + "." + job.getExtension();
            }
        } else {
            return null;
        }

        String abs = path + File.separator + name;
        abs = abs.replace(File.separator + File.separator, File.separator);

        if (path.startsWith("\\\\")) {
            abs = "\\" + abs;
        }

        File f = new File(abs);

        if (AppContext.gui != null) {
            AppContext.gui.println("% New file: " + f);
        }
        return f;
    }

    private String processRulesScript(Job job, String script) {
        try {
            List<Section> sections = buildSections(script, job);
            if (sections.isEmpty()) {
                return null;
            }
            StringBuilder result = new StringBuilder();
            while (!sections.isEmpty()) {
                result.append(sections.removeFirst());
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Section> buildSections(String script, Job job) throws Exception {
        StringTokenizer tokenizer = new StringTokenizer(script, "\r\n");
        String token;
        String upperToken;
        List<Section> sections = new ArrayList<>();
        int doIndex;
        boolean previousConditionPassed = true;
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            upperToken = token.toUpperCase();
            if (upperToken.startsWith("#")) {
                continue;
            }
            if (upperToken.startsWith("DO ") && handleOperation(sections, token.substring(3))) {
                break;
            } else if (upperToken.startsWith("IF ")
                    && (previousConditionPassed = evaluateConditions(
                            token.substring(3, (doIndex = indexOfConditionSeparator(upperToken, 3))), job))
                    && handleOperation(sections, token.substring(doIndex + 4))) {
                break;
            } else if (upperToken.startsWith("ELSE IF ")
                    && !previousConditionPassed
                    && (previousConditionPassed = evaluateConditions(
                            token.substring(8, (doIndex = indexOfConditionSeparator(upperToken, 8))), job))
                    && handleOperation(sections, token.substring(doIndex + 4))) {
                break;
            } else if (upperToken.startsWith("ELSE DO ")
                    && !previousConditionPassed
                    && handleOperation(sections, token.substring(8))) {
                break;
            }
        }
        return sections;
    }

    private boolean handleOperation(List<Section> sections, String operation) throws Exception {
        String upperOp = operation.toUpperCase();
        if (upperOp.startsWith("ADD ")) {
            sections.add(new Section(operation.substring(4)));
            return false;
        }
        if (upperOp.startsWith("SET ")) {
            sections.clear();
            sections.add(new Section(operation.substring(4)));
            return false;
        }
        if (upperOp.equals("FAIL")) {
            sections.clear();
            return true;
        }
        if (upperOp.equals("FINISH")) {
            return true;
        }
        if (upperOp.startsWith("FINISH ")) {
            sections.add(new Section(operation.substring(7)));
            return true;
        }
        if (upperOp.startsWith("RETURN ")) {
            sections.clear();
            sections.add(new Section(operation.substring(7)));
            return true;
        }
        if (upperOp.startsWith("ASSUME ")) {
            try {
                if (upperOp.startsWith("ASSUME SPECIAL ")) {
                    AppContext.assumedSpecialCount =
                            StringUtilities.i(upperOp.substring(15).trim());
                } else {
                    AppContext.assumedEpisodeCount =
                            StringUtilities.i(upperOp.substring(7).trim());
                }
            } catch (NumberFormatException e) {
                AppContext.dialog("NumberFormatException", "Parsing '" + operation + "' failed.");
            }
            return false;
        }
        if (upperOp.startsWith(TRUNC)) {
            int closeIndex = upperOp.indexOf('>');
            int commaIndex = upperOp.indexOf(',');
            if (closeIndex > 0 && commaIndex > 0 && commaIndex < closeIndex) {
                sections.add(new Section(upperOp.substring(0, closeIndex + 1)));
            } else {
                printError("Invalid rule element: " + operation);
            }
            return false;
        }
        sections.clear();
        throw new Exception("Error after DO: Expected SET/ADD/FAIL/FINISH/TRUNCATE<int,str>: " + operation);
    }

    private boolean evaluateConditions(String conditionString, Job job) {
        StringTokenizer tokenizer = new StringTokenizer(conditionString, ";");
        if (tokenizer.countTokens() < 1) {
            return false;
        }
        boolean allConditionsPass = true;
        while (tokenizer.hasMoreTokens() && allConditionsPass) {
            try {
                allConditionsPass &=
                        evaluateConditionGroup(tokenizer.nextToken().trim(), job);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return allConditionsPass;
    }

    private int findClosingParenthesis(String text, int startPosition) {
        int nestingLevel = 0;
        char currentChar;
        for (int index = startPosition; index < text.length(); index++) {
            currentChar = text.charAt(index);
            if (currentChar == '(') {
                nestingLevel++;
            } else if (currentChar == ')') {
                if (nestingLevel == 0) {
                    return index;
                }
                nestingLevel--;
            }
        }
        return -1;
    }

    private boolean evaluateConditionGroup(String conditionExpr, Job job) throws Exception {
        char conditionType = Character.toUpperCase(conditionExpr.charAt(0));
        int openParenIndex = conditionExpr.indexOf('(') + 1;
        int closeParenIndex = findClosingParenthesis(conditionExpr, openParenIndex);
        if (openParenIndex < 1 || closeParenIndex < 0) {
            throw new Exception("Missing ( or ): " + conditionExpr);
        }

        StringTokenizer tokenizer = new StringTokenizer(conditionExpr.substring(openParenIndex, closeParenIndex), ",");
        while (tokenizer.hasMoreTokens()) {
            try {
                String testValue = tokenizer.nextToken().trim();
                boolean negated = testValue.startsWith("!");
                if (negated) {
                    testValue = testValue.substring(1);
                }
                if (negated ^ evaluateSingleCondition(conditionType, testValue, job)) {
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    private boolean evaluateSingleCondition(char conditionType, String testValue, Job job) {
        switch (Character.toUpperCase(conditionType)) {
            case 'A': { // Anime name or ID
                try {
                    return job.anidbFile.getAnimeId() == Integer.parseInt(testValue.trim());
                } catch (NumberFormatException e) {
                    return matchesPattern(job.anidbFile.getAnime().romajiTitle, testValue)
                            || matchesPattern(job.anidbFile.getAnime().kanjiTitle, testValue)
                            || matchesPattern(job.anidbFile.getAnime().englishTitle, testValue);
                }
            }
            case 'E': // Episode
                return matchesPattern(job.anidbFile.getEpisode().num, testValue)
                        || matchesPattern(job.anidbFile.getEpisode().eng, testValue);
            case 'C': // Codec
                return job.anidbFile.getVideoCodec().equalsIgnoreCase(testValue)
                        || containsIgnoreCase(job.anidbFile.getAudioCodec(), testValue);
            case 'Q': // Quality
                return job.anidbFile.getQuality().equalsIgnoreCase(testValue);
            case 'R': // Rip source
                return job.anidbFile.getRipSource().equalsIgnoreCase(testValue);
            case 'T': // Type
                return job.anidbFile.getAnime().type.equalsIgnoreCase(testValue);
            case 'G': { // Group name or ID
                if (job.anidbFile.getGroupId() == 0) {
                    return testValue.equalsIgnoreCase("unknown");
                }
                try {
                    return job.anidbFile.getGroupId() == Integer.parseInt(testValue.trim());
                } catch (NumberFormatException e) {
                    /* Expected when testValue is a group name */
                }
                return job.anidbFile.getGroup().name.equalsIgnoreCase(testValue)
                        || job.anidbFile.getGroup().shortName.equalsIgnoreCase(testValue);
            }
            case 'Y': // Year
                return job.anidbFile.inYear(testValue);
            case 'D': // Dub language
                return containsIgnoreCase(job.anidbFile.getDubLanguage(), testValue);
            case 'S': // Sub language
                return containsIgnoreCase(job.anidbFile.getSubLanguage(), testValue);
            case 'X': // Episode count
                return testValue.equals("" + job.anidbFile.getAnime().episodeCount);
            case 'P': // Path
                return matchesPattern(job.currentFile.getAbsolutePath(), testValue);
            case 'N': // Genre/category
                return containsIgnoreCase(job.anidbFile.getAnime().categories, testValue);
            case 'I': { // Is tag defined
                String tagValue = tagValueMap.get(testValue);
                return tagValue != null && !tagValue.isEmpty();
            }
            case 'U': { // Tags are unequal
                String[] comparison = testValue.split(":", 2);
                if (comparison.length == 2) {
                    return tagValueMap.containsKey(comparison[0])
                            && tagValueMap.containsKey(comparison[1])
                            && !tagValueMap.get(comparison[0]).equals(tagValueMap.get(comparison[1]));
                }
                printError("Invalid data in test: U(" + testValue + ")");
                return false;
            }
            case 'L': { // Tags are equal (Like)
                String[] comparison = testValue.split(":", 2);
                if (comparison.length == 2) {
                    return tagValueMap.containsKey(comparison[0])
                            && tagValueMap.containsKey(comparison[1])
                            && tagValueMap.get(comparison[0]).equals(tagValueMap.get(comparison[1]));
                }
                printError("Invalid data in test: L(" + testValue + ")");
                return false;
            }
            case 'Z': { // Tag matches regex
                String[] comparison = testValue.split(":", 2);
                if (comparison.length == 2) {
                    return tagValueMap.containsKey(comparison[0])
                            && matchesPattern(tagValueMap.get(comparison[0]), comparison[1]);
                }
                printError("Invalid data in test: Z(" + testValue + ")");
                return false;
            }

            default:
                return false;
        }
    }

    private String finalizeFilename(Job job, String filename) {
        filename = filename.replace(File.separatorChar, '\1').replace(':', '\2');
        filename = applyTruncation(job.convert(filename));
        filename = applyReplacements(filename, illegalCharReplacements);
        filename = filename.replace('\1', File.separatorChar).replace('\2', ':');
        if (filename.endsWith("_")) {
            filename = filename.substring(0, filename.length() - 1);
            filename = filename.replace(' ', '_');
        }
        return filename;
    }
}

/**
 * Represents a section of a parsed rename/move rule with optional weight and max length.
 */
class Section {
    String content;
    float weight = 1.0f;
    int maxLength = 255;

    Section(String input) {
        Rules.WeightClause weightClause;
        try {
            weightClause = Rules.parseWeightClause(input);
        } catch (IllegalArgumentException exception) {
            // Older versions ignored malformed trailing WEIGHT clauses; keep legacy scripts renaming.
            weightClause = null;
            input = input.substring(0, Rules.findWeightClause(input));
        }
        if (weightClause != null) {
            content = input.substring(0, weightClause.startIndex());
            weight = weightClause.weight();
            maxLength = weightClause.maxLength();
        } else {
            content = input;
        }
        int quoteStart = content.indexOf('\'');
        int quoteEnd = content.lastIndexOf('\'');
        if (quoteStart >= 0 && quoteEnd > quoteStart) {
            content = content.substring(quoteStart + 1, quoteEnd);
        }
        int commentIndex = content.indexOf("//");
        if (commentIndex > 0) {
            content = content.substring(0, commentIndex);
        }
    }

    public String toString() {
        return content;
    }
}
