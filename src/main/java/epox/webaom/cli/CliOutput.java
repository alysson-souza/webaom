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

import epox.util.StringUtilities;
import epox.webaom.data.AniDBFile;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Formats CLI results without mixing machine-readable output with progress. */
public final class CliOutput {
    public enum Format {
        TABLE,
        JSON,
        JSONL
    }

    public enum ResultView {
        STANDARD,
        IDENTIFY,
        RENAME_PREVIEW
    }

    private final PrintWriter out;
    private final PrintWriter err;
    private final Format format;
    private final boolean quiet;
    private final boolean verbose;
    private final CliStyle style;

    public CliOutput(PrintWriter out, PrintWriter err, Format format, boolean quiet, boolean verbose) {
        this(out, err, format, quiet, verbose, CliStyle.plain());
    }

    CliOutput(PrintWriter out, PrintWriter err, Format format, boolean quiet, boolean verbose, CliStyle style) {
        this.out = out;
        this.err = err;
        this.format = format;
        this.quiet = quiet;
        this.verbose = verbose;
        this.style = format == Format.TABLE ? style : CliStyle.plain();
    }

    public void progress(String message) {
        if (!quiet && format == Format.TABLE) {
            err.println(style.progress(message));
            err.flush();
        }
    }

    public void progress(String action, java.nio.file.Path path) {
        if (!quiet && format == Format.TABLE) {
            err.println(style.progress(action + " ") + style.path(path));
            err.flush();
        }
    }

    public void error(String message) {
        err.println(style.error("Error:") + " " + style.text(message));
        err.flush();
    }

    /**
     * Writes an unprefixed message to stderr, for narration that is neither an error nor part of the
     * command's output. Unlike {@link #progress(String)} this is never suppressed, so it can carry
     * text the user must read before answering a prompt.
     */
    public void notice(String message) {
        err.println(style.warning(message));
        err.flush();
    }

    public void printResults(List<CliFileResult> results, ResultView view) {
        switch (format) {
            case TABLE -> printTable(results, view);
            case JSON -> out.println(results.stream().map(this::toJson).collect(Collectors.joining(",", "[", "]")));
            case JSONL -> results.forEach(result -> out.println(toJson(result)));
        }
        out.flush();
    }

    public void printText(String text) {
        String safeText = style.text(text);
        out.print(safeText);
        if (!text.endsWith(System.lineSeparator())) {
            out.println();
        }
        out.flush();
    }

    public void printSuccess(String text) {
        out.println(style.success(text));
        out.flush();
    }

    public void printWarning(String text) {
        out.println(style.warning(text));
        out.flush();
    }

    public void printValue(String label, String value) {
        out.println(style.label(label + ":") + " " + style.text(value));
        out.flush();
    }

    public void printPath(String label, java.nio.file.Path path) {
        out.println(style.label(label + ":") + " " + style.path(path));
        out.flush();
    }

    public void printCheck(String label, String value, boolean healthy) {
        out.println(style.label(label + ":") + " " + (healthy ? style.success(value) : style.error(value)));
        out.flush();
    }

    private void printTable(List<CliFileResult> results, ResultView view) {
        for (CliFileResult result : results) {
            if (view == ResultView.RENAME_PREVIEW && result.target != null) {
                printRenamePreview(result);
                continue;
            }

            CliIdentification identification = CliIdentification.from(result.job);
            if (view == ResultView.IDENTIFY && identification != null) {
                printIdentification(result, identification);
            } else {
                printCompactResult(result);
            }
        }
        long failed = results.stream().filter(CliFileResult::failed).count();
        String summary =
                results.size() + " file" + (results.size() == 1 ? "" : "s") + ", " + failed + " failed or unknown";
        out.println(failed == 0 ? style.success(summary) : style.error(summary));
    }

    private void printRenamePreview(CliFileResult result) {
        out.println(style.path(result.source));
        out.println(style.warning("→") + " " + style.path(result.target));
        for (CliFileResult.CompanionMove companionMove : result.companionMoves) {
            out.println(style.label("  Companion:") + " " + style.path(companionMove.source));
            out.println("             " + style.warning("→") + " " + style.path(companionMove.target));
        }
        if (result.failed()) {
            out.println(style.label("  Status:") + " " + style.status(result.status));
        }
        if (result.message != null && (verbose || result.failed())) {
            out.println(style.label("  Detail:") + " " + style.detail(result.message));
        }
        out.println();
    }

    private void printCompactResult(CliFileResult result) {
        out.println(style.path(result.source));
        out.println(style.label("  Status:") + "  " + style.status(result.status));
        if (result.job != null && result.job.anidbFile != null) {
            AniDBFile file = result.job.anidbFile;
            out.println(style.label("  Anime:") + "   " + style.webLink(file.getAnimeUrl(), title(file)));
            out.println(style.label("  Episode:") + " " + style.webLink(file.getEpisodeUrl(), episode(file)));
            String group = group(file);
            out.println(style.label("  Group:") + "   "
                    + (file.getGroupId() == 0 ? style.text(group) : style.webLink(file.getGroupUrl(), group)));
            out.println(style.label("  AniDB:") + "   " + style.webLink(file.getFileUrl(), "file " + file.getFileId()));
        }
        printCompactHashes(result.hashes);
        if (result.message != null) {
            out.println(style.label("  Detail:") + "  " + style.detail(result.message));
        }
        out.println();
    }

    private void printCompactHashes(Map<FileHasher.Algorithm, String> hashes) {
        for (FileHasher.Algorithm algorithm : FileHasher.Algorithm.values()) {
            String value = hashes.get(algorithm);
            if (value == null) {
                continue;
            }
            String label = String.format("%-8s", algorithm.name().toLowerCase(Locale.ROOT) + ":");
            out.println("  " + style.label(label) + " " + style.text(value));
        }
    }

    private void printIdentification(CliFileResult result, CliIdentification identification) {
        out.println(style.path(result.source));
        out.println(style.label("  Status:") + "  " + style.status(result.status));

        CliIdentification.FileMetadata file = identification.file();
        printSection("File");
        printLinkedValue("File ID", file.id(), file.url());
        printPositiveValue("MyList ID", file.mylistId());
        if (file.sizeBytes() > 0) {
            printMetadataValue("Size", StringUtilities.sbyte(file.sizeBytes()) + " (" + file.sizeBytes() + " bytes)");
        }
        if (file.lengthSeconds() > 0) {
            printMetadataValue(
                    "Length", formatDuration(file.lengthSeconds()) + " (" + file.lengthSeconds() + " seconds)");
        }
        printMetadataValue("Extension", file.extension());
        printMetadataValue(
                "State",
                file.state() + " (0x" + Integer.toHexString(file.state()).toUpperCase(Locale.ROOT) + ")");
        printMetadataValue("Version", file.version());
        printMetadataValue("Censorship", file.censorship());
        printMetadataValue("Integrity", file.integrity());
        printMetadataValue("Audio language", file.audioLanguages());
        printMetadataValue("Subtitle language", file.subtitleLanguages());
        printMetadataValue("Source", file.source());
        printMetadataValue("Quality", file.quality());
        printMetadataValue("Resolution", file.resolution());
        printMetadataValue("Video codec", file.videoCodec());
        printMetadataValue("Audio codec", file.audioCodec());
        printMetadataValue("Missing required", missingData(file.missingData().strict()));
        printMetadataValue("Missing optional", missingData(file.missingData().additional()));

        CliIdentification.AnimeMetadata anime = identification.anime();
        printSection("Anime");
        printLinkedValue("Anime ID", anime.id(), anime.url());
        printMetadataValue("Romaji title", anime.titles().romaji());
        printMetadataValue("Kanji title", anime.titles().kanji());
        printMetadataValue("English title", anime.titles().english());
        printMetadataValue("Type", anime.type());
        printPositiveValue("Year start", anime.yearStart());
        printPositiveValue("Year end", anime.yearEnd());
        printPositiveValue("Episodes", anime.episodeCount());
        printPositiveValue("Latest episode", anime.latestEpisode());
        if (!anime.categories().isEmpty()) {
            printMetadataValue("Categories", String.join(", ", anime.categories()));
        }

        CliIdentification.EpisodeMetadata episode = identification.episode();
        printSection("Episode");
        printLinkedValue("Episode ID", episode.id(), episode.url());
        printMetadataValue("Number", episode.number());
        printMetadataValue("English title", episode.titles().english());
        printMetadataValue("Romaji title", episode.titles().romaji());
        printMetadataValue("Kanji title", episode.titles().kanji());

        CliIdentification.GroupMetadata group = identification.group();
        printSection("Group");
        printLinkedValue("Group ID", group.id(), group.url());
        if (group.raw()) {
            printMetadataValue("Release", "RAW");
        } else {
            printMetadataValue("Name", group.name());
            printMetadataValue("Short name", group.shortName());
        }

        if (!result.hashes.isEmpty()) {
            printSection("Local Hashes");
            printHashes(result.hashes);
        }
        if (!file.hashes().isEmpty()) {
            printSection("AniDB Hashes");
            printMetadataValue("ED2K", file.hashes().ed2k());
            printMetadataValue("CRC32", file.hashes().crc32());
            printMetadataValue("MD5", file.hashes().md5());
            printMetadataValue("SHA-1", file.hashes().sha1());
        }
        if (result.message != null) {
            printRenderedValue("Detail", style.detail(result.message));
        }
        out.println();
    }

    private void printSection(String title) {
        out.println(style.label("  " + title));
    }

    private void printPositiveValue(String label, int value) {
        if (value > 0) {
            printMetadataValue(label, Integer.toString(value));
        }
    }

    private void printLinkedValue(String label, int value, String url) {
        String text = Integer.toString(value);
        printRenderedValue(label, url == null ? style.text(text) : style.webLink(url, text));
    }

    private void printMetadataValue(String label, String value) {
        if (value != null) {
            printRenderedValue(label, style.text(value));
        }
    }

    private void printRenderedValue(String label, String renderedValue) {
        out.println(style.label(String.format(Locale.ROOT, "    %-20s", label + ":")) + " " + renderedValue);
    }

    private void printHashes(Map<FileHasher.Algorithm, String> hashes) {
        for (FileHasher.Algorithm algorithm : FileHasher.Algorithm.values()) {
            printMetadataValue(hashLabel(algorithm), hashes.get(algorithm));
        }
    }

    private static String hashLabel(FileHasher.Algorithm algorithm) {
        return switch (algorithm) {
            case ED2K -> "ED2K";
            case CRC32 -> "CRC32";
            case MD5 -> "MD5";
            case SHA1 -> "SHA-1";
            case TTH -> "TTH";
        };
    }

    private static String missingData(List<String> codes) {
        if (codes == null) {
            return null;
        }
        return codes.isEmpty() ? "none" : String.join(", ", codes);
    }

    private static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;
        return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private String toJson(CliFileResult result) {
        JsonObject json = new JsonObject();
        json.string("path", result.source.toString());
        json.string(
                "status",
                result.status == null ? "failed" : result.status.name().toLowerCase(Locale.ROOT));
        if (result.target != null) {
            json.string("target", result.target.toString());
            json.bool("applied", result.applied);
        }
        if (result.message != null) {
            json.string("message", result.message);
        }
        if (result.persistenceFailed) {
            json.bool("databasePersisted", false);
        }
        if (!result.companionMoves.isEmpty()) {
            json.objectArray("companions", result.companionMoves, (companion, move) -> {
                companion.string("path", move.source.toString());
                companion.string("target", move.target.toString());
                companion.bool("applied", move.applied);
            });
        }
        CliIdentification identification = CliIdentification.from(result.job);
        if (identification != null) {
            json.object("anidb", anidb -> writeIdentification(anidb, identification));
        }
        if (!result.hashes.isEmpty()) {
            json.object("hashes", hashes -> {
                for (FileHasher.Algorithm algorithm : FileHasher.Algorithm.values()) {
                    hashes.optionalString(algorithm.name().toLowerCase(Locale.ROOT), result.hashes.get(algorithm));
                }
            });
        }
        return json.finish();
    }

    private static void writeIdentification(JsonObject json, CliIdentification identification) {
        json.object("file", object -> writeFile(object, identification.file()));
        json.object("anime", object -> writeAnime(object, identification.anime()));
        json.object("episode", object -> writeEpisode(object, identification.episode()));
        json.object("group", object -> writeGroup(object, identification.group()));
    }

    private static void writeFile(JsonObject json, CliIdentification.FileMetadata file) {
        json.number("id", file.id());
        if (file.mylistId() > 0) {
            json.number("mylistId", file.mylistId());
        }
        if (file.sizeBytes() > 0) {
            json.number("sizeBytes", file.sizeBytes());
        }
        if (file.lengthSeconds() > 0) {
            json.number("lengthSeconds", file.lengthSeconds());
        }
        json.optionalString("extension", file.extension());
        json.number("state", file.state());
        json.optionalString("version", file.version());
        json.optionalString("censorship", file.censorship());
        json.optionalString("integrity", file.integrity());
        json.optionalString("audioLanguages", file.audioLanguages());
        json.optionalString("subtitleLanguages", file.subtitleLanguages());
        json.optionalString("source", file.source());
        json.optionalString("quality", file.quality());
        json.optionalString("resolution", file.resolution());
        json.optionalString("videoCodec", file.videoCodec());
        json.optionalString("audioCodec", file.audioCodec());
        if (file.missingData().strict() != null || file.missingData().additional() != null) {
            json.object("missingData", missing -> {
                missing.stringArray("strict", file.missingData().strict());
                missing.stringArray("additional", file.missingData().additional());
            });
        }
        if (!file.hashes().isEmpty()) {
            json.object("hashes", hashes -> {
                hashes.optionalString("ed2k", file.hashes().ed2k());
                hashes.optionalString("crc32", file.hashes().crc32());
                hashes.optionalString("md5", file.hashes().md5());
                hashes.optionalString("sha1", file.hashes().sha1());
            });
        }
        json.optionalString("url", file.url());
    }

    private static void writeAnime(JsonObject json, CliIdentification.AnimeMetadata anime) {
        json.number("id", anime.id());
        writeTitles(json, anime.titles());
        json.optionalString("type", anime.type());
        if (anime.yearStart() > 0) {
            json.number("yearStart", anime.yearStart());
        }
        if (anime.yearEnd() > 0) {
            json.number("yearEnd", anime.yearEnd());
        }
        if (anime.episodeCount() > 0) {
            json.number("episodeCount", anime.episodeCount());
        }
        if (anime.latestEpisode() > 0) {
            json.number("latestEpisode", anime.latestEpisode());
        }
        if (!anime.categories().isEmpty()) {
            json.stringArray("categories", anime.categories());
        }
        json.optionalString("url", anime.url());
    }

    private static void writeEpisode(JsonObject json, CliIdentification.EpisodeMetadata episode) {
        json.number("id", episode.id());
        json.optionalString("number", episode.number());
        writeTitles(json, episode.titles());
        json.optionalString("url", episode.url());
    }

    private static void writeGroup(JsonObject json, CliIdentification.GroupMetadata group) {
        json.number("id", group.id());
        json.bool("raw", group.raw());
        if (!group.raw()) {
            json.optionalString("name", group.name());
            json.optionalString("shortName", group.shortName());
            json.optionalString("url", group.url());
        }
    }

    private static void writeTitles(JsonObject json, CliIdentification.Titles titles) {
        if (!titles.isEmpty()) {
            json.object("titles", object -> {
                object.optionalString("romaji", titles.romaji());
                object.optionalString("kanji", titles.kanji());
                object.optionalString("english", titles.english());
            });
        }
    }

    private static String title(AniDBFile file) {
        if (file.getAnime() == null) {
            return "";
        }
        String english = file.getAnime().englishTitle;
        if (english != null && !english.isBlank()) {
            return english;
        }
        String romaji = file.getAnime().romajiTitle;
        return romaji == null ? "" : romaji;
    }

    private static String episode(AniDBFile file) {
        if (file.getEpisode() == null) {
            return "";
        }
        return file.getEpisode().num + " — " + file.getEpisode().eng;
    }

    private static String group(AniDBFile file) {
        if (file.getGroup() == null || file.getGroupId() == 0) {
            return "RAW";
        }
        return file.getGroup().shortName;
    }

    static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static final class JsonObject {
        private final StringBuilder json = new StringBuilder("{");
        private boolean first = true;

        void string(String name, String value) {
            add(name, '"' + escape(value) + '"');
        }

        void optionalString(String name, String value) {
            if (value != null && !value.isBlank()) {
                string(name, value);
            }
        }

        void number(String name, long value) {
            add(name, Long.toString(value));
        }

        void bool(String name, boolean value) {
            add(name, Boolean.toString(value));
        }

        void object(String name, Consumer<JsonObject> writer) {
            JsonObject object = new JsonObject();
            writer.accept(object);
            add(name, object.finish());
        }

        <T> void objectArray(String name, List<T> values, BiConsumer<JsonObject, T> writer) {
            StringBuilder array = new StringBuilder("[");
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    array.append(',');
                }
                JsonObject object = new JsonObject();
                writer.accept(object, values.get(index));
                array.append(object.finish());
            }
            add(name, array.append(']').toString());
        }

        void stringArray(String name, List<String> values) {
            if (values == null) {
                return;
            }
            String array =
                    values.stream().map(value -> '"' + escape(value) + '"').collect(Collectors.joining(",", "[", "]"));
            add(name, array);
        }

        String finish() {
            return json.toString() + '}';
        }

        private void add(String name, String value) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(name)).append("\":").append(value);
        }
    }
}
