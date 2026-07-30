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

import epox.webaom.Job;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** Immutable, null-safe view of the AniDB metadata attached to a CLI result. */
record CliIdentification(FileMetadata file, AnimeMetadata anime, EpisodeMetadata episode, GroupMetadata group) {

    static CliIdentification from(Job job) {
        if (job == null || job.anidbFile == null) {
            return null;
        }

        AniDBFile file = job.anidbFile;
        Anime anime = file.getAnime();
        Episode episode = file.getEpisode();
        Group group = file.getGroup();
        boolean raw = file.getGroupId() == 0;

        FileMetadata fileMetadata = new FileMetadata(
                file.getFileId(),
                job.mylistId,
                file.getTotalSize(),
                file.getLengthInSeconds(),
                present(file.getExtension()),
                file.getState(),
                present(file.getVersion()),
                present(file.getCensored()),
                present(file.getInvalid()),
                present(file.getDubLanguage()),
                present(file.getSubLanguage()),
                present(file.getRipSource()),
                present(file.getQuality()),
                present(file.getResolution()),
                present(file.getVideoCodec()),
                present(file.getAudioCodec()),
                missingData(file),
                new AniDbHashes(
                        present(file.getEd2kHash()),
                        present(file.getCrcHash()),
                        present(file.getMd5Hash()),
                        present(file.getShaHash())),
                safe(file::getFileUrl));

        AnimeMetadata animeMetadata = new AnimeMetadata(
                file.getAnimeId(),
                anime == null
                        ? Titles.empty()
                        : new Titles(
                                present(anime.romajiTitle), present(anime.kanjiTitle), present(anime.englishTitle)),
                anime == null ? null : present(anime.type),
                anime == null ? 0 : anime.year,
                anime == null ? 0 : anime.endYear,
                anime == null ? 0 : anime.episodeCount,
                anime == null ? 0 : anime.latestEpisode,
                anime == null ? List.of() : categories(anime.categories),
                safe(file::getAnimeUrl));

        EpisodeMetadata episodeMetadata = new EpisodeMetadata(
                file.getEpisodeId(),
                episode == null ? null : present(episode.num),
                episode == null
                        ? Titles.empty()
                        : new Titles(present(episode.rom), present(episode.kan), present(episode.eng)),
                safe(file::getEpisodeUrl));

        GroupMetadata groupMetadata = new GroupMetadata(
                file.getGroupId(),
                raw,
                raw || group == null ? null : present(group.name),
                raw || group == null ? null : present(group.shortName),
                raw ? null : safe(file::getGroupUrl));

        return new CliIdentification(fileMetadata, animeMetadata, episodeMetadata, groupMetadata);
    }

    private static MissingData missingData(AniDBFile file) {
        String strict = safeValue(file::getMissingDataStrict);
        String additional = safeValue(file::getMissingDataAdditional);
        return new MissingData(codes(strict), codes(additional));
    }

    private static List<String> categories(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(category -> !category.isEmpty())
                .toList();
    }

    private static List<String> codes(String value) {
        if (value == null || "N/A".equals(value)) {
            return null;
        }
        return value.codePoints()
                .mapToObj(code -> Character.toString((char) code))
                .toList();
    }

    private static String present(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String safe(Supplier<String> supplier) {
        return present(safeValue(supplier));
    }

    private static String safeValue(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    record FileMetadata(
            int id,
            int mylistId,
            long sizeBytes,
            int lengthSeconds,
            String extension,
            int state,
            String version,
            String censorship,
            String integrity,
            String audioLanguages,
            String subtitleLanguages,
            String source,
            String quality,
            String resolution,
            String videoCodec,
            String audioCodec,
            MissingData missingData,
            AniDbHashes hashes,
            String url) {}

    record AnimeMetadata(
            int id,
            Titles titles,
            String type,
            int yearStart,
            int yearEnd,
            int episodeCount,
            int latestEpisode,
            List<String> categories,
            String url) {}

    record EpisodeMetadata(int id, String number, Titles titles, String url) {}

    record GroupMetadata(int id, boolean raw, String name, String shortName, String url) {}

    record Titles(String romaji, String kanji, String english) {
        static Titles empty() {
            return new Titles(null, null, null);
        }

        boolean isEmpty() {
            return romaji == null && kanji == null && english == null;
        }
    }

    record MissingData(List<String> strict, List<String> additional) {}

    record AniDbHashes(String ed2k, String crc32, String md5, String sha1) {
        boolean isEmpty() {
            return ed2k == null && crc32 == null && md5 == null && sha1 == null;
        }
    }
}
