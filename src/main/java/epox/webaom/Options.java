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

import epox.webaom.util.PlatformPaths;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

public class Options {
    /** Primary field separator character (ASCII SOH) for serialization. */
    public static final String FIELD_SEPARATOR = "\1";
    // Boolean option indices
    public static final int BOOL_UNUSED_0 = 0;
    public static final int BOOL_HASH_CRC = 1;
    public static final int BOOL_HASH_MD5 = 2;
    public static final int BOOL_HASH_SHA = 3;
    public static final int BOOL_HASH_TTH = 4;
    public static final int BOOL_ADD_FILE = 5;
    public static final int BOOL_WATCHED = 6;
    public static final int BOOL_UNUSED_1 = 7;
    public static final int BOOL_NAT_KEEP_ALIVE = 8;
    public static final int BOOL_STORE_PASSWORD = 9;
    public static final int BOOL_AUTO_LOAD_DATABASE = 10;
    public static final int BOOL_AUTO_LOGIN = 11;
    public static final int BOOL_AUTO_SAVE = 12;
    /** When disabled, files won't be automatically renamed/moved after identification. */
    public static final int BOOL_AUTO_RENAME = 13;
    /** Load all jobs including finished ones. */
    public static final int BOOL_LOAD_ALL_JOBS = 14;

    public static final int BOOLEAN_OPTIONS_COUNT = 15;
    // Integer option indices
    public static final int INT_REMOTE_PORT = 0;
    public static final int INT_LOCAL_PORT = 1;
    /** File state on mylist add. */
    public static final int INT_FILE_STATE = 2;
    /** Network timeout in milliseconds. */
    public static final int INT_TIMEOUT = 3;
    /** Datagram delay between packets. */
    public static final int INT_DATAGRAM_DELAY = 4;
    /** Rename mode setting. */
    public static final int INT_RENAME_MODE = 5;

    public static final int INTEGER_OPTIONS_COUNT = 6;
    // String option indices
    public static final int STR_USERNAME = 0;
    public static final int STR_HOST_URL = 1;
    public static final int STR_DATABASE_URL = 2;
    public static final int STR_HASH_DIRECTORY = 3;
    public static final int STR_BROWSER = 4;
    public static final int STR_EXTENSIONS = 5;
    public static final int STR_SOURCE_FOLDER = 6;
    public static final int STR_STORAGE = 7;
    public static final int STR_OTHER_INFO = 8;
    public static final int STR_RENAME_RULES = 9;
    public static final int STR_MOVE_RULES = 10;
    public static final int STR_REPLACE_RULES = 11;
    public static final int STR_HTML_COLORS = 12;
    public static final int STR_LOG_FILE = 13;
    public static final int STR_PATH_REGEX = 14;
    public static final int STR_FONT = 15;
    public static final int STR_LOG_HEADER = 16;
    public static final int STR_JOB_COLUMNS = 17;
    public static final int STR_LAST_DIRECTORY = 18;
    public static final int STR_THEME = 19;
    public static final int STRING_OPTIONS_COUNT = 20;
    private static final String OPTIONS_VERSION = "001";
    /** Secondary separator character (ASCII STX) for section boundaries. */
    private static final String SECTION_SEPARATOR = "\2";

    private final File optionsFile;
    public final int[] integerOptions = new int[INTEGER_OPTIONS_COUNT];
    public final String[] stringOptions = new String[STRING_OPTIONS_COUNT];
    public final boolean[] booleanOptions = new boolean[BOOLEAN_OPTIONS_COUNT];

    public Options() {
        // Migrate legacy config to XDG-compliant path if needed
        PlatformPaths.migrateIfNeeded(PlatformPaths.getLegacyConfigFilePath(), PlatformPaths.getConfigFilePath());

        optionsFile = new File(PlatformPaths.getConfigFilePath());
        // Default auto-rename to true to preserve existing behavior
        booleanOptions[BOOL_AUTO_RENAME] = true;
    }

    public boolean existsOnDisk() {
        return Files.exists(optionsFile.toPath());
    }

    public String getFilePath() {
        return optionsFile.getAbsolutePath();
    }

    public boolean equals(Options other) {
        for (int index = 0; index < BOOLEAN_OPTIONS_COUNT; index++) {
            if (booleanOptions[index] != other.booleanOptions[index]) {
                return false;
            }
        }
        for (int index = 0; index < INTEGER_OPTIONS_COUNT; index++) {
            if (integerOptions[index] != other.integerOptions[index]) {
                return false;
            }
        }
        for (int index = 0; index < STRING_OPTIONS_COUNT; index++) {
            if (!areStringsEqual(stringOptions[index], other.stringOptions[index])) {
                return false;
            }
        }
        return true;
    }

    private boolean areStringsEqual(String first, String second) {
        boolean firstBlank = first == null || first.isEmpty();
        boolean secondBlank = second == null || second.isEmpty();
        if (firstBlank || secondBlank) {
            return firstBlank == secondBlank;
        }
        return first.equals(second);
    }

    public void saveToFile() {
        if (!existsOnDisk()
                && !AppContext.confirm(
                        "Warning", "Options will be stored here:\n" + getFilePath(), "Continue", "Cancel")) {
            return;
        }
        try {
            Path parentDir = optionsFile.toPath().getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.writeString(optionsFile.toPath(), encodeAllOptions(), StandardCharsets.UTF_8);
            System.out.println("$ File written:" + optionsFile);
        } catch (IOException e) {
            System.err.println("! Failed to save options: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean loadFromFile() {
        if (!existsOnDisk()) {
            return false;
        }
        try {
            String content = Files.readString(optionsFile.toPath(), StandardCharsets.UTF_8);
            System.out.println("$ File read:" + optionsFile);
            return decodeAllOptions(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String encodeAllOptions() {
        return OPTIONS_VERSION
                + SECTION_SEPARATOR
                + encodeBooleans()
                + SECTION_SEPARATOR
                + encodeIntegers()
                + SECTION_SEPARATOR
                + encodeStrings();
    }

    public boolean decodeAllOptions(String encodedData) {
        StringTokenizer tokenizer = new StringTokenizer(encodedData, SECTION_SEPARATOR);
        if (tokenizer.countTokens() > 0 && tokenizer.nextToken().equals(OPTIONS_VERSION)) {
            decodeBooleans(tokenizer.nextToken());
            decodeIntegers(tokenizer.nextToken());
            decodeStrings(tokenizer);
            return true;
        }
        System.out.println("! Options file is outdated. Could not load.");
        return false;
    }

    public boolean getBoolean(int optionId) {
        return booleanOptions[optionId];
    }

    public void setBoolean(int optionId, boolean value) {
        booleanOptions[optionId] = value;
    }

    public int getInteger(int optionId) {
        return integerOptions[optionId];
    }

    public void setInteger(int optionId, int value) {
        integerOptions[optionId] = value;
    }

    public String getString(int optionId) {
        return stringOptions[optionId];
    }

    public void setString(int optionId, String value) {
        stringOptions[optionId] = value;
    }

    private String encodeBooleans() {
        String encodedBooleans = "";
        for (int index = 0; index < BOOLEAN_OPTIONS_COUNT; index++) {
            encodedBooleans += booleanOptions[index] ? '1' : '0';
        }
        return encodedBooleans;
    }

    private void decodeBooleans(String encodedBooleans) {
        char[] characters = encodedBooleans.toCharArray();
        int count = Math.min(characters.length, BOOLEAN_OPTIONS_COUNT);
        for (int index = 0; index < count; index++) {
            booleanOptions[index] = (characters[index] == '1');
        }
    }

    private String encodeIntegers() {
        String encodedIntegers = "";
        for (int index = 0; index < INTEGER_OPTIONS_COUNT; index++) {
            encodedIntegers += integerOptions[index] + Options.FIELD_SEPARATOR;
        }
        return encodedIntegers;
    }

    private void decodeIntegers(String encodedData) {
        StringTokenizer tokenizer = new StringTokenizer(encodedData, Options.FIELD_SEPARATOR);
        for (int index = 0; index < INTEGER_OPTIONS_COUNT; index++) {
            integerOptions[index] = Integer.parseInt(tokenizer.nextToken());
        }
    }

    private String encodeStrings() {
        String encodedStrings = "";
        for (int index = 0; index < STRING_OPTIONS_COUNT; index++) {
            String value = stringOptions[index];
            encodedStrings += ((value == null || value.isEmpty()) ? "null" : value) + SECTION_SEPARATOR;
        }
        return encodedStrings;
    }

    private void decodeStrings(StringTokenizer tokenizer) {
        String tokenValue;
        for (int index = 0; index < STRING_OPTIONS_COUNT; index++) {
            if (tokenizer.hasMoreTokens()) {
                tokenValue = tokenizer.nextToken();
            } else {
                tokenValue = null;
            }
            stringOptions[index] = (tokenValue == null || tokenValue.equals("null")) ? "" : tokenValue;
        }
    }
}
