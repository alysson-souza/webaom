// Copyright (C) 2005-2006 epoximator

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 03.08.05
 *
 * @version 	05 (1.14,1.13,1.12,1.11,1.09)
 * @author 		epoximator
 */
package epox.webaom;

import epox.util.ReplacementRule;
import epox.util.StringUtilities;
import epox.webaom.data.AttributeMap;
import java.io.File;
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

    /** Characters to replace in filenames (illegal filesystem characters) */
    public List<ReplacementRule> illegalCharReplacements;

    private String renameRulesScript;
    private String moveRulesScript;
    private AttributeMap tagValueMap = null;

    // private int mItruncate = 0;

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

        renameRulesScript = "#RENAME\n" + "#DO SET '%ann - %enr - %epn '\n" + "#IF G(!unknown) DO ADD '[%grp]'\n"
                + "#ELSE DO ADD '[RAW]'";
        moveRulesScript = "#MOVE";
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
                text = StringUtilities.replace(text, replacement.getSource(), replacement.getDestination());
            }
        }
        return text;
    }

    private static String applyTruncation(String text) {
        try {
            String suffix;
            int truncateStart = text.indexOf(TRUNC);
            int closeIndex;
            int commaIndex;
            int maxLength;
            while (truncateStart > 0) {
                closeIndex = text.indexOf('>', truncateStart);
                if (closeIndex < truncateStart) {
                    break;
                }
                commaIndex = text.indexOf(',', truncateStart);
                if (commaIndex < truncateStart || commaIndex > closeIndex) {
                    break;
                }
                maxLength = StringUtilities.i(text.substring(truncateStart + TRUNC.length(), commaIndex));
                suffix = text.substring(commaIndex + 1, closeIndex);
                if (truncateStart > maxLength + suffix.length()) {
                    text = text.substring(0, maxLength - suffix.length()) + suffix + text.substring(closeIndex + 1);
                } else {
                    text = text.substring(0, truncateStart) + text.substring(closeIndex + 1);
                }
                truncateStart = text.indexOf(TRUNC);
            }
        } catch (NumberFormatException e) {
            System.err.println(e);
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
        script = StringUtilities.replace(script, "%year", "%yea");
        script = StringUtilities.replace(script, "%type", "%typ");
        script = StringUtilities.replace(script, "%qual", "%qua");
        script = StringUtilities.replace(script, "%ed2k", "%ed2");
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

        if (job.anidbFile.anime != null) {
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
        abs = StringUtilities.replace(abs, File.separator + File.separator, File.separator);

        if (path.startsWith("\\\\")) {
            abs = "\\" + abs;
        }

        File f = new File(abs);

        System.out.println("% New file: " + f);
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
                result.append(sections.remove(0));
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
                    && (previousConditionPassed =
                            evaluateConditions(token.substring(3, (doIndex = upperToken.indexOf(" DO "))), job))
                    && handleOperation(sections, token.substring(doIndex + 4))) {
                break;
            } else if (upperToken.startsWith("ELSE IF ")
                    && !previousConditionPassed
                    && (previousConditionPassed =
                            evaluateConditions(token.substring(8, (doIndex = upperToken.indexOf(" DO "))), job))
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
                AppContext.gui.println(HyperlinkBuilder.formatAsError("Invalid rule element: " + operation));
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
                    return job.anidbFile.animeId == Integer.parseInt(testValue.trim());
                } catch (NumberFormatException e) {
                    return matchesPattern(job.anidbFile.anime.romajiTitle, testValue)
                            || matchesPattern(job.anidbFile.anime.kanjiTitle, testValue)
                            || matchesPattern(job.anidbFile.anime.englishTitle, testValue);
                }
            }
            case 'E': // Episode
                return matchesPattern(job.anidbFile.episode.num, testValue)
                        || matchesPattern(job.anidbFile.episode.eng, testValue);
            case 'C': // Codec
                return job.anidbFile.videoCodec.equalsIgnoreCase(testValue)
                        || containsIgnoreCase(job.anidbFile.audioCodec, testValue);
            case 'Q': // Quality
                return job.anidbFile.quality.equalsIgnoreCase(testValue);
            case 'R': // Rip source
                return job.anidbFile.ripSource.equalsIgnoreCase(testValue);
            case 'T': // Type
                return job.anidbFile.anime.type.equalsIgnoreCase(testValue);
            case 'G': { // Group name or ID
                if (job.anidbFile.groupId == 0) {
                    return testValue.equalsIgnoreCase("unknown");
                }
                try {
                    return job.anidbFile.groupId == Integer.parseInt(testValue.trim());
                } catch (NumberFormatException e) {
                    /* Expected when testValue is a group name */
                }
                return job.anidbFile.group.name.equalsIgnoreCase(testValue)
                        || job.anidbFile.group.shortName.equalsIgnoreCase(testValue);
            }
            case 'Y': // Year
                return job.anidbFile.inYear(testValue);
            case 'D': // Dub language
                return containsIgnoreCase(job.anidbFile.dubLanguage, testValue);
            case 'S': // Sub language
                return containsIgnoreCase(job.anidbFile.subLanguage, testValue);
            case 'X': // Episode count
                return testValue.equals("" + job.anidbFile.anime.episodeCount);
            case 'P': // Path
                return matchesPattern(job.currentFile.getAbsolutePath(), testValue);
            case 'N': // Genre/category
                return containsIgnoreCase(job.anidbFile.anime.categories, testValue);
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
                System.out.println("ERROR: Invalid data in test: U(" + testValue + ")");
                return false;
            }
            case 'L': { // Tags are equal (Like)
                String[] comparison = testValue.split(":", 2);
                if (comparison.length == 2) {
                    return tagValueMap.containsKey(comparison[0])
                            && tagValueMap.containsKey(comparison[1])
                            && tagValueMap.get(comparison[0]).equals(tagValueMap.get(comparison[1]));
                }
                System.out.println("ERROR: Invalid data in test: L(" + testValue + ")");
                return false;
            }
            case 'Z': { // Tag matches regex
                String[] comparison = testValue.split(":", 2);
                if (comparison.length == 2) {
                    return tagValueMap.containsKey(comparison[0])
                            && matchesPattern(tagValueMap.get(comparison[0]), comparison[1]);
                }
                System.out.println("ERROR: Invalid data in test: Z(" + testValue + ")");
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
        int weightIndex = input.indexOf(" WEIGHT ");
        if (weightIndex >= 0) {
            content = input.substring(0, weightIndex);
            input = input.substring(weightIndex + 8).trim();
            int colonIndex = input.indexOf(':');
            if (colonIndex >= 0) {
                weight = Float.parseFloat(input.substring(0, colonIndex));
                maxLength = Integer.parseInt(input.substring(colonIndex + 1));
            }
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
