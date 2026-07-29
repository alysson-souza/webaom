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

import epox.webaom.data.AniDBFile;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Formats CLI results without mixing machine-readable output with progress. */
public final class CliOutput {
    public enum Format {
        TABLE,
        JSON,
        JSONL
    }

    private final PrintWriter out;
    private final PrintWriter err;
    private final Format format;
    private final boolean quiet;
    private final boolean verbose;

    public CliOutput(PrintWriter out, PrintWriter err, Format format, boolean quiet, boolean verbose) {
        this.out = out;
        this.err = err;
        this.format = format;
        this.quiet = quiet;
        this.verbose = verbose;
    }

    public void progress(String message) {
        if (!quiet && format == Format.TABLE) {
            err.println(message);
            err.flush();
        }
    }

    public void error(String message) {
        err.println("Error: " + message);
        err.flush();
    }

    public void printResults(List<CliFileResult> results, boolean renamePreview) {
        switch (format) {
            case TABLE -> printTable(results, renamePreview);
            case JSON -> out.println(results.stream().map(this::toJson).collect(Collectors.joining(",", "[", "]")));
            case JSONL -> results.forEach(result -> out.println(toJson(result)));
        }
        out.flush();
    }

    public void printText(String text) {
        out.print(text);
        if (!text.endsWith(System.lineSeparator())) {
            out.println();
        }
        out.flush();
    }

    private void printTable(List<CliFileResult> results, boolean renamePreview) {
        for (CliFileResult result : results) {
            if (renamePreview && result.target != null) {
                out.println(result.source);
                out.println("→ " + result.target);
                for (CliFileResult.CompanionMove companionMove : result.companionMoves) {
                    out.println("  Companion: " + companionMove.source);
                    out.println("           → " + companionMove.target);
                }
                if (result.failed()) {
                    out.println("  Status: " + display(result.status));
                }
                if (result.message != null && (verbose || result.failed())) {
                    out.println("  Detail: " + result.message);
                }
                out.println();
                continue;
            }

            out.println(result.source);
            out.println("  Status:  " + display(result.status));
            if (result.job != null && result.job.anidbFile != null) {
                AniDBFile file = result.job.anidbFile;
                out.println("  Anime:   " + title(file));
                out.println("  Episode: " + episode(file));
                out.println("  Group:   " + group(file));
                out.println("  AniDB:   file " + file.getFileId());
            }
            if (!result.hashes.isEmpty()) {
                result.hashes.forEach((algorithm, value) ->
                        out.printf("  %-8s %s%n", algorithm.name().toLowerCase(Locale.ROOT) + ":", value));
            }
            if (result.message != null) {
                out.println("  Detail:  " + result.message);
            }
            out.println();
        }
        long failed = results.stream().filter(CliFileResult::failed).count();
        out.printf("%d file%s, %d failed or unknown%n", results.size(), results.size() == 1 ? "" : "s", failed);
    }

    private String toJson(CliFileResult result) {
        StringBuilder json = new StringBuilder("{");
        field(json, "path", result.source.toString());
        comma(json);
        field(
                json,
                "status",
                result.status == null ? "failed" : result.status.name().toLowerCase(Locale.ROOT));
        if (result.target != null) {
            comma(json);
            field(json, "target", result.target.toString());
            comma(json);
            json.append("\"applied\":").append(result.applied);
        }
        if (result.message != null) {
            comma(json);
            field(json, "message", result.message);
        }
        if (result.persistenceFailed) {
            comma(json);
            json.append("\"databasePersisted\":false");
        }
        if (!result.companionMoves.isEmpty()) {
            comma(json);
            json.append("\"companions\":[");
            for (int index = 0; index < result.companionMoves.size(); index++) {
                if (index > 0) {
                    json.append(',');
                }
                CliFileResult.CompanionMove companionMove = result.companionMoves.get(index);
                json.append('{');
                field(json, "path", companionMove.source.toString());
                comma(json);
                field(json, "target", companionMove.target.toString());
                comma(json);
                json.append("\"applied\":").append(companionMove.applied);
                json.append('}');
            }
            json.append(']');
        }
        if (result.job != null && result.job.anidbFile != null) {
            AniDBFile file = result.job.anidbFile;
            comma(json);
            json.append("\"anidb\":{");
            numberField(json, "fileId", file.getFileId());
            comma(json);
            field(json, "anime", title(file));
            comma(json);
            field(json, "episode", episode(file));
            comma(json);
            field(json, "group", group(file));
            json.append('}');
        }
        if (!result.hashes.isEmpty()) {
            comma(json);
            json.append("\"hashes\":{");
            boolean first = true;
            for (Map.Entry<FileHasher.Algorithm, String> entry : result.hashes.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                field(json, entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue());
                first = false;
            }
            json.append('}');
        }
        return json.append('}').toString();
    }

    private static String title(AniDBFile file) {
        if (file.getAnime() == null) {
            return "";
        }
        String english = file.getAnime().englishTitle;
        return english == null || english.isBlank() ? file.getAnime().romajiTitle : english;
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

    private static String display(CliFileResult.Status status) {
        return status == null
                ? "Failed"
                : status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static void field(StringBuilder json, String name, String value) {
        json.append('"')
                .append(escape(name))
                .append("\":\"")
                .append(escape(value))
                .append('"');
    }

    private static void numberField(StringBuilder json, String name, long value) {
        json.append('"').append(escape(name)).append("\":").append(value);
    }

    private static void comma(StringBuilder json) {
        json.append(',');
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
}
