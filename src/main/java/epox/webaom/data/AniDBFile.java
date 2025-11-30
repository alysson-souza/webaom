// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 22.01.05
 *
 * @version 	03
 * @author 		epoximator
 */
package epox.webaom.data;

import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.Job;

public class AniDBFile extends AniDBEntity {
    public static final int F_CRCOK = 1;
    public static final int F_CRCERR = 2;
    public static final int F_ISV2 = 4;
    public static final int F_ISV3 = 8;
    public static final int F_ISV4 = 16;
    public static final int F_ISV5 = 32;
    public static final int F_UNC = 64;
    public static final int F_CEN = 128;
    /** AniDB file ID. */
    public int fileId;
    /** AniDB anime ID. */
    public int animeId;
    /** AniDB episode ID. */
    public int episodeId;
    /** AniDB group ID (0 if no group/raw). */
    public int groupId;
    /** AniDB mylist entry ID. */
    public int mylistEntryId;
    /** File state bitmask (CRC status, version, censored). */
    public int state;
    /** Video length in seconds. */
    public int lengthInSeconds;
    /** ED2K hash. */
    public String ed2kHash;
    /** MD5 hash. */
    public String md5Hash;
    /** SHA-1 hash. */
    public String shaHash;
    /** CRC32 hash. */
    public String crcHash;
    /** Dub language(s). */
    public String dubLanguage;
    /** Subtitle language(s). */
    public String subLanguage;
    /** Quality descriptor. */
    public String quality;
    /** Rip source (TV, DVD, BluRay, etc). */
    public String ripSource;
    /** Video resolution (e.g. "1920x1080"). */
    public String resolution;
    /** Video codec (e.g. "H264"). */
    public String videoCodec;
    /** Audio codec (e.g. "AAC"). */
    public String audioCodec;
    /** Default/computed file name. */
    public String defaultName;
    /** File extension. */
    public String extension;

    public Episode episode;
    public AnimeGroup animeGroup;
    public Group group;
    public Anime anime;
    private Job job = null;

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

    public static AniDBEntity getInst(String[] s) {
        return new AniDBFile(s);
    }

    public Job getJob() {
        return job;
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
        return Integer.valueOf(fileId);
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

    private String url0(String str, boolean non) {
        if (non) {
            return "https://" + AppContext.ANIDB_HOST + "/perl-bin/animedb.pl?" + str + "&nonav=1";
        }
        return "https://" + AppContext.ANIDB_HOST + "/perl-bin/animedb.pl?" + str;
    }

    private String url1(String str) {
        return url0("show=" + str, true);
    }

    public String urlAnime() {
        return url1("anime&aid=" + animeId);
    }

    public String urlExport() {
        return url1("ed2kexport&h=1&aid=" + animeId);
    }

    public String urlEp() {
        return url1("ep&aid=" + animeId + "&eid=" + episodeId);
    }

    public String urlFile() {
        return url1("file&aid=" + animeId + "&eid=" + episodeId + "&fid=" + fileId);
    }

    public String urlGroup() {
        return url1("group&gid=" + groupId);
    }

    public String urlMylistE(int listId) {
        if (listId < 2) {
            return urlMylist();
        }
        return url1("mylist&do=add&aid=" + animeId + "&eid=" + episodeId + "&fid=" + fileId + "&lid=" + listId);
    }

    public String urlMylist() {
        try {
            return url1("mylist&expand=" + animeId + "&char=" + anime.romajiTitle.charAt(0) + "#a" + animeId);
        } catch (Exception ex) {
            return url1("mylist");
        }
    }

    public String urlYear() {
        try {
            return url0("do.search=%20Start%20Search%20&show=search&noalias=1&search.anime.year=" + anime.year, false);
        } catch (Exception ex) {
            return url1("search");
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
        switch (state & 0x3C) {
            case F_ISV2:
                return "v2";
            case F_ISV3:
                return "v3";
            case F_ISV4:
                return "v4";
            case F_ISV5:
                return "v5";
            default:
                return "";
        }
    }

    public String getCensored() {
        switch (state & 0xC0) {
            case F_CEN:
                return "cen";
            case F_UNC:
                return "unc";
            default:
                return "";
        }
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
        if (dubLanguage.contains("unknown")) {
            missingFlags += 'd';
        }
        if (subLanguage.contains("unknown")) {
            missingFlags += 's';
        }
        if (audioCodec.contains("unknown")) {
            missingFlags += 'a';
        }
        if (videoCodec.contains("unknown")) {
            missingFlags += 'v';
        }
        if (resolution.equals("0x0") || resolution.equals("unknown")) {
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

        if (quality.contains("unknown")) {
            missingFlags += 'q';
        }
        if (ripSource.contains("unknown")) {
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
