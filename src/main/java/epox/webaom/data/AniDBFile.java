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

package epox.webaom.data;

import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.Job;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AniDBFile extends AniDBEntity {
    public static final int F_CRCOK = 1;
    public static final int F_CRCERR = 2;
    public static final int F_ISV2 = 4;
    public static final int F_ISV3 = 8;
    public static final int F_ISV4 = 16;
    public static final int F_ISV5 = 32;
    public static final int F_UNC = 64;
    public static final int F_CEN = 128;

    private static final String PATH_ANIME = "anime";
    private static final String PARAM_NONAV = "nonav";
    private static final String PARAM_MYLIST = "mylist";
    private static final String VALUE_UNKNOWN = "unknown";

    /** AniDB file ID. */
    private int fileId;
    /** AniDB anime ID. */
    private int animeId;
    /** AniDB episode ID. */
    private int episodeId;
    /** AniDB group ID (0 if no group/raw). */
    private int groupId;
    /** AniDB mylist entry ID. */
    private int mylistEntryId;
    /** File state bitmask (CRC status, version, censored). */
    private int state;
    /** Video length in seconds. */
    private int lengthInSeconds;
    /** ED2K hash. */
    private String ed2kHash;
    /** MD5 hash. */
    private String md5Hash;
    /** SHA-1 hash. */
    private String shaHash;
    /** CRC32 hash. */
    private String crcHash;
    /** Dub language(s). */
    private String dubLanguage;
    /** Subtitle language(s). */
    private String subLanguage;
    /** Quality descriptor. */
    private String quality;
    /** Rip source (TV, DVD, BluRay, etc). */
    private String ripSource;
    /** Video resolution (e.g. "1920x1080"). */
    private String resolution;
    /** Video codec (e.g. "H264"). */
    private String videoCodec;
    /** Audio codec (e.g. "AAC"). */
    private String audioCodec;
    /** Default/computed file name. */
    private String defaultName;
    /** File extension. */
    private String extension;

    private Episode episode;
    private AnimeGroup animeGroup;
    private Group group;
    private Anime anime;
    private Job job;

    public AniDBFile(int id) {
        fileId = id;
        extension = null;
    }

    public AniDBFile(String[] fields) {
        int index = 0;
        fileId = StringUtilities.i(fields[index++]);
        animeId = StringUtilities.i(fields[index++]);
        episodeId = StringUtilities.i(fields[index++]);
        if (fields[index].isEmpty()) {
            groupId = 0;
        } else {
            groupId = StringUtilities.i(fields[index]);
        }
        index++;
        mylistEntryId = StringUtilities.i(fields[index++]);
        state = StringUtilities.i(fields[index++]);
        if (fields[index].isEmpty()) {
            totalSize = 0;
        } else {
            totalSize = Long.parseLong(fields[index]);
        }
        index++;
        ed2kHash = fields[index++];
        md5Hash = StringUtilities.n(fields[index++]);
        shaHash = StringUtilities.n(fields[index++]);
        crcHash = StringUtilities.n(fields[index++]);

        dubLanguage = fields[index++].intern();
        subLanguage = fields[index++].intern();
        quality = fields[index++].intern();
        ripSource = fields[index++].intern();
        audioCodec = fields[index++].intern();
        videoCodec = fields[index++].intern();
        resolution = fields[index++].intern();
        extension = fields[index++].intern();
        lengthInSeconds = StringUtilities.i(fields[index++]);
    }

    public static AniDBEntity createFromFields(String[] fields) {
        return new AniDBFile(fields);
    }

    // Getters
    public int getFileId() {
        return fileId;
    }

    public int getAnimeId() {
        return animeId;
    }

    public int getEpisodeId() {
        return episodeId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getMylistEntryId() {
        return mylistEntryId;
    }

    public int getState() {
        return state;
    }

    public int getLengthInSeconds() {
        return lengthInSeconds;
    }

    public String getEd2kHash() {
        return ed2kHash;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public String getShaHash() {
        return shaHash;
    }

    public String getCrcHash() {
        return crcHash;
    }

    public String getDubLanguage() {
        return dubLanguage;
    }

    public String getSubLanguage() {
        return subLanguage;
    }

    public String getQuality() {
        return quality;
    }

    public String getRipSource() {
        return ripSource;
    }

    public String getResolution() {
        return resolution;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public String getExtension() {
        return extension;
    }

    public Episode getEpisode() {
        return episode;
    }

    public AnimeGroup getAnimeGroup() {
        return animeGroup;
    }

    public Group getGroup() {
        return group;
    }

    public Anime getAnime() {
        return anime;
    }

    public Job getJob() {
        return job;
    }

    // Setters
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public void setAnimeId(int animeId) {
        this.animeId = animeId;
    }

    public void setEpisodeId(int episodeId) {
        this.episodeId = episodeId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setMylistEntryId(int mylistEntryId) {
        this.mylistEntryId = mylistEntryId;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setLengthInSeconds(int lengthInSeconds) {
        this.lengthInSeconds = lengthInSeconds;
    }

    public void setEd2kHash(String ed2kHash) {
        this.ed2kHash = ed2kHash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public void setShaHash(String shaHash) {
        this.shaHash = shaHash;
    }

    public void setCrcHash(String crcHash) {
        this.crcHash = crcHash;
    }

    public void setDubLanguage(String dubLanguage) {
        this.dubLanguage = dubLanguage;
    }

    public void setSubLanguage(String subLanguage) {
        this.subLanguage = subLanguage;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setRipSource(String ripSource) {
        this.ripSource = ripSource;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setEpisode(Episode episode) {
        this.episode = episode;
    }

    public void setAnimeGroup(AnimeGroup animeGroup) {
        this.animeGroup = animeGroup;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setAnime(Anime anime) {
        this.anime = anime;
    }

    public void setJob(Job newJob) {
        job = newJob;
    }

    @Override
    public String toString() {
        if (job != null) {
            return job.getFile().getName();
        }
        return defaultName;
    }

    @Override
    public String serialize() {
        return "" + fileId + S + animeId + S + episodeId + S + groupId + S + mylistEntryId + S + state + S + totalSize
                + S + ed2kHash + S + md5Hash + S + shaHash + S + crcHash + S + dubLanguage + S + subLanguage + S
                + quality + S + ripSource + S + audioCodec + S + videoCodec + S + resolution + S + extension + S
                + lengthInSeconds + S + (group != null ? group.serialize() : "");
    }

    @Override
    public Object getKey() {
        return fileId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AniDBFile other = (AniDBFile) obj;
        return fileId == other.fileId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId);
    }

    @Override
    public void clear() {
        //
    }

    public void pack() {
        dubLanguage = dubLanguage.replace('/', '&'); // for jap/eng
        videoCodec = videoCodec.replace('/', ' '); // for 'H264/AVC'
        int spaceIndex = videoCodec.indexOf(" ");
        if (spaceIndex > 0) {
            videoCodec = videoCodec.substring(0, spaceIndex);
        }
        spaceIndex = audioCodec.indexOf(" ("); // for Vorbis (Ogg Vorbis)
        if (spaceIndex > 0) {
            audioCodec = audioCodec.substring(0, spaceIndex);
        }
        if (dubLanguage.startsWith("dual (")) // remove dual()
        {
            dubLanguage = dubLanguage.substring(6, dubLanguage.lastIndexOf(')'));
        }

        videoCodec = videoCodec.intern();
        audioCodec = audioCodec.intern();
        dubLanguage = dubLanguage.intern();
    }

    private static String encodeQuery(Map<String, Object> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String encodePath(String... segments) {
        return Arrays.stream(segments)
                .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/", "/", ""));
    }

    private URI buildUri(String path, Map<String, Object> queryParams, String fragment) {
        try {
            String query = queryParams != null ? encodeQuery(queryParams) : null;
            return new URI("https", AppContext.ANIDB_HOST, path, query, fragment);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI", e);
        }
    }

    public String getAnimeUrl() {
        return buildUri(encodePath("a" + animeId), null, null).toString();
    }

    public String getExportUrl() {
        return buildUri(
                        encodePath(PATH_ANIME, String.valueOf(animeId), "export"),
                        Map.of("show", "ed2kexport", "h", 1, PARAM_NONAV, 1),
                        null)
                .toString();
    }

    public String getEpisodeUrl() {
        return buildUri(encodePath("e" + episodeId), null, null).toString();
    }

    public String getFileUrl() {
        return buildUri(encodePath("f" + fileId), null, null).toString();
    }

    public String getGroupUrl() {
        return buildUri(encodePath("g" + groupId), null, null).toString();
    }

    public String getMylistEditUrl(int listId) {
        if (listId < 2) {
            return getMylistUrl();
        }
        Map<String, Object> params = Map.of(
                "show",
                PARAM_MYLIST,
                "do",
                "add",
                "aid",
                animeId,
                "eid",
                episodeId,
                "fid",
                fileId,
                "lid",
                listId,
                PARAM_NONAV,
                1);
        return buildUri(encodePath("perl-bin", "animedb.pl"), params, null).toString();
    }

    public String getMylistUrl() {
        try {
            Map<String, Object> params = Map.of(
                    "show", PARAM_MYLIST, "expand", animeId, "char", anime.romajiTitle.charAt(0), PARAM_NONAV, 1);
            return buildUri(encodePath("perl-bin", "animedb.pl"), params, "a" + animeId)
                    .toString();
        } catch (Exception ex) {
            return buildUri(encodePath(PARAM_MYLIST), null, null).toString();
        }
    }

    public String getYearUrl() {
        try {
            return buildUri(encodePath(PATH_ANIME, ""), Map.of("season.year", anime.year), null)
                    .toString();
        } catch (Exception ex) {
            return buildUri(encodePath(PATH_ANIME), null, null).toString();
        }
    }

    public boolean inYear(String yearRange) {
        yearRange = yearRange.replace(" ", "");
        int dashIndex = yearRange.indexOf('-');
        if (dashIndex > 0) { // Range
            int startYear = Integer.parseInt(yearRange.substring(0, dashIndex));
            int endYear = Integer.parseInt(yearRange.substring(dashIndex + 1));
            if (startYear > endYear) {
                int temp = endYear;
                endYear = startYear;
                startYear = temp;
            }
            return anime.year >= startYear && anime.year <= endYear;
        }
        return anime.year == Integer.parseInt(yearRange);
    }

    /*
     * f_state
     * 0 - crc ok
     * 1 - corrupt
     * 2 - v2
     * 3
     * 4
     * 5 - v5
     * 6 - uncensored
     * 7 - censored
     */
    public String getVersion() {
        return switch (state & 0x3C) {
            case F_ISV2 -> "v2";
            case F_ISV3 -> "v3";
            case F_ISV4 -> "v4";
            case F_ISV5 -> "v5";
            default -> "";
        };
    }

    public String getCensored() {
        return switch (state & 0xC0) {
            case F_CEN -> "cen";
            case F_UNC -> "unc";
            default -> "";
        };
    }

    public String getInvalid() {
        if ((state & F_CRCERR) == F_CRCERR) {
            return "invalid crc";
        }
        return "";
    }

    /**
     * Gets missing data indicators (strict).
     * Returns a string of characters indicating what data is missing:
     * c=CRC, h=hashes, l=length, d=dub, s=sub, a=audio, v=video, x=resolution
     */
    public String getMissingDataStrict() {
        if (anime == null || episode == null) {
            return "N/A";
        }
        String missingFlags = "";

        if (crcHash == null || crcHash.isEmpty()) {
            missingFlags += 'c';
        }
        if (md5Hash == null || md5Hash.isEmpty() || shaHash == null || shaHash.isEmpty()) {
            missingFlags += 'h';
        }
        if (lengthInSeconds < 1) {
            missingFlags += 'l';
        }
        if (dubLanguage.contains(VALUE_UNKNOWN)) {
            missingFlags += 'd';
        }
        if (subLanguage.contains(VALUE_UNKNOWN)) {
            missingFlags += 's';
        }
        if (audioCodec.contains(VALUE_UNKNOWN)) {
            missingFlags += 'a';
        }
        if (videoCodec.contains(VALUE_UNKNOWN)) {
            missingFlags += 'v';
        }
        if (resolution.equals("0x0") || resolution.equals(VALUE_UNKNOWN)) {
            missingFlags += 'x';
        }

        return missingFlags;
    }

    /**
     * Gets missing data indicators (additional/optional).
     * Returns a string of characters indicating what data is missing:
     * q=quality, o=rip source, e=english title, k=kanji title, r=romaji title
     */
    public String getMissingDataAdditional() {
        if (anime == null || episode == null) {
            return "N/A";
        }
        String missingFlags = "";

        if (quality.contains(VALUE_UNKNOWN)) {
            missingFlags += 'q';
        }
        if (ripSource.contains(VALUE_UNKNOWN)) {
            missingFlags += 'o';
        }

        if (episode.eng == null || episode.eng.isEmpty()) {
            missingFlags += 'e';
        }
        if (episode.kan == null || episode.kan.isEmpty()) {
            missingFlags += 'k';
        }
        if (episode.rom == null || episode.rom.isEmpty()) {
            missingFlags += 'r';
        }

        return missingFlags;
    }
}
