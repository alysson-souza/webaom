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

import epox.av.FileInfo;
import epox.util.StringUtilities;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.AttributeMap;
import epox.webaom.db.DatabaseManager;
import java.io.File;
import java.util.logging.Logger;

public class Job {
    private static final Logger LOGGER = Logger.getLogger(Job.class.getName());

    // STATIC
    public static final int S_DONE = 0x00000001; // status
    public static final int S_DO = 0x00000002;
    public static final int S_DOING = 0x00000014;
    public static final int S_FAILED = 0x00000008;
    public static final int U_2 = 0x00000020; // sub status
    public static final int H_NORMAL = 0x00000100; // health
    public static final int H_PAUSED = 0x00000200;
    public static final int H_MISSING = 0x00000400;
    public static final int H_DELETED = 0x00000800;
    public static final int D_DIO = 0x00010000; // mode
    public static final int D_NIO = 0x00020000;
    public static final int // 	M_USR		= 0x00040000,
            T_1 = 0x00100000; // type
    public static final int T_2 = 0x00200000;
    public static final int T_4 = 0x00400000;
    public static final int // 	T_8			= 0x00800000,
            F_DB = 0x01000000; // flag
    public static final int F_UNK = 0x02000000;
    public static final int F_PD = 0x00000000; // pardoned
    public static final int M_H = 0x0000ff00; // mask
    public static final int M_S = 0x0fff00ff;
    public static final int M_SS = 0x0000001f;
    public static final int M_D = 0x000f0000;
    public static final int M_R = 0x0f0f001f;
    public static final int FAILED = S_FAILED | F_DB; // 0x00,0   -> 16777224
    public static final int UNKNOWN = S_DONE | F_UNK | F_DB; // 0x10,16  -> 50331649
    public static final int FINISHED = S_DONE | F_DB | F_PD; // 0xf0,240 -> 16777217
    public static final int HASHWAIT = D_DIO | T_1 | S_DO | F_DB; // 0x30,48  -> 17891330
    public static final int HASHING = D_DIO | T_1 | S_DOING; // 0x32,
    public static final int HASHED = D_DIO | T_1 | S_DONE; // 0x3f,
    public static final int IDENTWAIT = D_NIO | T_1 | S_DO | F_DB; // 0x50,80  -> 17956866
    public static final int IDENTIFYING = D_NIO | T_1 | S_DOING; // 0x52,
    public static final int IDENTIFIED = D_NIO | T_1 | S_DONE; // 0x5f,
    public static final int ADDWAIT = D_NIO | T_2 | S_DO | F_DB | F_PD; // 0x70,112 -> 19005442
    public static final int ADDING = D_NIO | T_2 | S_DOING; // 0x72,
    public static final int ADDED = D_NIO | T_2 | S_DONE; // 0x7f,
    public static final int MOVEWAIT = D_DIO | T_2 | S_DO | F_DB; // 0x90,144 -> 18939906
    public static final int MOVING = D_DIO | T_2 | S_DOING; // 0x92,
    public static final int MOVECHECK = D_DIO | T_2 | U_2 | S_DOING; // 0x94,
    public static final int MOVED = D_DIO | T_2 | S_DONE; // 0x9f,
    public static final int REMWAIT = D_NIO | T_4 | S_DO | F_DB | F_PD; // 0xc0,
    public static final int REMING = D_NIO | T_4 | S_DOING; // 0xc2,
    public static final int PARSEWAIT = D_DIO | T_4 | S_DO | F_DB | F_PD; // 0xd0,
    public static final int PARSING = D_DIO | T_4 | S_DOING; // 0xd2,
    // Size, int=4, long=1, pointer=4, string=7 -> 4*4+1*8+4*8+7*8= 112 bytes / calc = 108 bytes
    private static final char FIELD_SEPARATOR = '|';
    public int directoryId;
    public int mylistId = 0;
    public int fileIdOverride = 0;
    public boolean isFresh = true;
    public long fileSize;
    public File currentFile;
    public File targetFile;
    public AniDBFile anidbFile;
    public FileInfo avFileInfo;
    public String originalName;
    public String errorMessage;
    public String ed2kHash;
    public String md5Hash;
    public String sha1Hash;
    public String tthHash;
    public String crc32Hash;
    /** Hashing progress from 0.0 to 1.0, updated by HashTask workers */
    public volatile float hashProgress = 0f;

    private int status;

    public Job(String[] serializedData) {
        this(new File(serializedData[1]), StringUtilities.i(serializedData[0]));
        int index = 2;
        fileSize = Long.parseLong(serializedData[index++]);
        ed2kHash = serializedData[index++];
        md5Hash = StringUtilities.n(serializedData[index++]);
        sha1Hash = StringUtilities.n(serializedData[index++]);
        tthHash = StringUtilities.n(serializedData[index++]);
        crc32Hash = StringUtilities.n(serializedData[index++]);
        originalName = serializedData[index++];
    }

    public Job(File file, int initialStatus) {
        status = initialStatus;
        currentFile = file;
        targetFile = null;
        anidbFile = null;
        ed2kHash = md5Hash = sha1Hash = tthHash = crc32Hash = null;

        fileSize = file.length();
        originalName = file.getName();

        if (!currentFile.exists()) {
            status |= H_MISSING;
        } else {
            status |= H_PAUSED;
        }
        AppContext.jobCounter.register(-1, -1, status & M_R, getHealth());
    }

    private static String statusStr(int i) {
        return switch (i) {
            case HASHWAIT -> "Wait/Hash";
            case HASHING -> "Hashing";
            case HASHED -> "Hashed";
            case IDENTWAIT -> "Wait/ID";
            case IDENTIFYING -> "Identifying";
            case IDENTIFIED -> "Identified";
            case ADDWAIT -> "Wait/Add";
            case ADDING -> "Adding";
            case ADDED -> "Added";
            case MOVEWAIT -> "Wait/Move";
            case MOVING -> "Moving";
            case MOVECHECK -> "Checking";
            case MOVED -> "Moved";
            case FINISHED -> "Finished";
            case UNKNOWN -> "Unknown";
            case FAILED -> "Failed";
            case H_PAUSED -> "P";
            case H_DELETED -> "D";
            case H_MISSING -> "M";
            case REMWAIT -> "Wait/Rem";
            case REMING -> "Removing";
            case PARSEWAIT -> "Wait/Parse";
            case PARSING -> "Parsing";
            default -> "" + i;
        };
    }

    public String serialize() {
        return "" + getStatus() + FIELD_SEPARATOR + currentFile + FIELD_SEPARATOR + fileSize + FIELD_SEPARATOR
                + ed2kHash + FIELD_SEPARATOR + md5Hash + FIELD_SEPARATOR + sha1Hash + FIELD_SEPARATOR + tthHash
                + FIELD_SEPARATOR + crc32Hash + FIELD_SEPARATOR + originalName;
    }

    public String toString() {
        return currentFile.getName() + ": " + getStatusText();
    }

    public boolean hide(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        if (pattern.charAt(0) == '!') {
            return currentFile.getAbsolutePath().matches(pattern.substring(1));
        }
        return !currentFile.getAbsolutePath().matches(pattern);
    }

    public File getFile() {
        return currentFile;
    }

    public String getExtension() {
        if (anidbFile == null || anidbFile.getExtension() == null) {
            String fileName = currentFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex < 1) {
                LOGGER.warning(() -> "No extension found in file: " + fileName);
                return "unk";
            }
            return fileName.substring(dotIndex + 1);
        }
        return anidbFile.getExtension();
    }

    public int getStatus() {
        return status & M_S;
    }

    public int getHealth() {
        return status & M_H;
    }

    private void setHealth(int health) {
        int currentStatus = getStatus();
        if (!check(H_NORMAL) && health == H_NORMAL) {
            AppContext.jobs.updateQueues(this, 0, currentStatus);
        } else if (check(H_NORMAL) && health != H_NORMAL) {
            AppContext.jobs.updateQueues(this, currentStatus, -1);
        }

        AppContext.jobCounter.register(status & M_R, getHealth(), status & M_R, health);
        setHealth0(health);
    }

    public boolean check(int statusFlags) {
        return (status & statusFlags) == statusFlags;
    }

    public boolean checkOr(int statusFlags) {
        return (status & statusFlags) > 0;
    }

    public boolean checkSep(int statusFlags, int fileFlags, boolean unknownOnly) {
        boolean matches = (status & M_SS & statusFlags) > 0
                && (status & M_H & statusFlags) > 0
                && ((statusFlags & M_D) < 1 || (status & M_D & statusFlags) > 0);
        if (unknownOnly) {
            return anidbFile == null && matches;
        }
        return matches && (fileFlags < 1 || (anidbFile != null && (anidbFile.getState() & fileFlags) > 0));
    }

    public boolean isLocked(int targetStatus) {
        int health = getHealth();
        if (health < H_MISSING) {
            return false;
        }
        if (health == H_MISSING) {
            switch (targetStatus) {
                case FINISHED, ADDWAIT, REMWAIT -> {
                    return false;
                }
                default -> {
                    // All other statuses remain locked when health is MISSING
                }
            }
        }
        return true;
    }

    public boolean isCorrupt() {
        return anidbFile != null && ((anidbFile.getState() & AniDBFile.F_CRCERR) == AniDBFile.F_CRCERR);
    }

    public boolean incompl() {
        return anidbFile == null || anidbFile.getAnime() == null || anidbFile.getEpisode() == null;
    }

    public String getStatusText() {
        if (check(HASHING)) {
            return "Hashing " + (int) (hashProgress * 100) + "%";
        }
        if (check(H_NORMAL)) {
            return statusStr(getStatus());
        }
        return statusStr(getStatus()) + " [" + statusStr(getHealth()) + "]";
    }

    public void setStatus(int newStatus, boolean test) {
        int health = getHealth();
        if (test) {
            if (isLocked(newStatus)) {
                return;
            }
            if (health == H_PAUSED || health == H_MISSING) {
                setHealth(H_NORMAL);
            }
            health = getHealth();
        }
        if ((newStatus & F_DB) == F_DB) { // only for main status
            if (test && health == H_NORMAL) {
                AppContext.jobs.updateQueues(this, getStatus(), newStatus & M_S); // TODO pause off fix
            }
            health = H_NORMAL;
            if (newStatus == FINISHED && !currentFile.exists()) {
                health = H_MISSING;
            }
            AppContext.jobCounter.register(status & M_R, getHealth(), newStatus & M_R, health);
            status = newStatus | health;

        } else {
            status = newStatus | H_NORMAL;
        }
    }

    public void setHealth0(int health) {
        status = (status & M_S) | health;
    }

    public void setError(String errorText) {
        errorMessage = errorText;
    }

    public void updateHealth(int healthUpdate) {
        if (healthUpdate != H_DELETED && checkOr(H_MISSING | H_DELETED)) {
            return;
        }
        int health = getHealth();
        switch (healthUpdate) {
            case H_PAUSED -> {
                if (health == H_NORMAL) {
                    health = H_PAUSED; // turn pause on
                } else if (health == H_PAUSED) {
                    health = H_NORMAL; // turn pause off
                }
            }
            case H_DELETED -> {
                if (health == H_DELETED) {
                    health = (currentFile.exists() ? H_NORMAL : H_MISSING);
                    AppContext.databaseManager.update(0, this, DatabaseManager.INDEX_JOB);
                } else {
                    health = H_DELETED; // delete
                    AppContext.databaseManager.removeJob(this);
                }
            }
            case H_MISSING -> health = H_MISSING;
            default -> {
                // Unknown health update value - no action needed
            }
        }
        setHealth(health);
    }

    public void find(File file) {
        if (!check(H_MISSING)) {
            return;
        }
        JobManager.setJobFile(this, file);
        setHealth0(file.exists() ? H_PAUSED : H_MISSING);
        directoryId = -1;
    }

    public String convert(String template) {
        String avInfoBlock = StringUtilities.getInTag(template, "avinfo");
        if (avInfoBlock != null) {
            String processedAvInfo = avInfoBlock;
            if (avFileInfo != null) {
                String[] tags = new String[] {"vid", "aud", "sub"};
                for (int i = 0; i < 3; i++) {
                    String tagContent = StringUtilities.getInTag(processedAvInfo, tags[i]);
                    if (tagContent == null) {
                        continue;
                    }
                    String converted = avFileInfo.convert(tagContent, i);
                    processedAvInfo = processedAvInfo.replace(tagContent, converted);
                }
            } else {
                processedAvInfo = "";
            }
            template = template.replace(avInfoBlock, processedAvInfo);
        }
        return StringUtilities.replaceCCCode(template, genMap());
    }

    public AttributeMap genMap() {
        AttributeMap am = new AttributeMap();

        am.put("fil", currentFile.getName());
        am.put("pat", currentFile.getParent());
        am.put("new", ((targetFile != null) ? targetFile.toString() : ""));
        am.put("ori", originalName);
        am.put("siz", fileSize);
        String stat = getStatusText();
        if (check(Job.FAILED) && errorMessage != null) {
            stat += ". " + errorMessage;
        }
        am.put("sta", stat);

        if (anidbFile != null) {
            am.put("aid", anidbFile.getAnimeId());
            am.put("eid", anidbFile.getEpisodeId());
            am.put("fid", anidbFile.getFileId());
            am.put("gid", anidbFile.getGroupId());
            am.put("lid", mylistId);
            am.put("ver", anidbFile.getVersion());
            am.put("ula", anidbFile.getAnimeUrl());
            am.put("ule", anidbFile.getEpisodeUrl());
            am.put("ulf", anidbFile.getFileUrl());
            am.put("ulg", anidbFile.getGroupUrl());
            am.put("ulx", anidbFile.getMylistEditUrl(mylistId));
            am.put("ulm", anidbFile.getMylistUrl());
            am.put("uly", anidbFile.getYearUrl());
            am.put("ed2", anidbFile.getEd2kHash().toLowerCase());
            am.put("ED2", anidbFile.getEd2kHash().toUpperCase());
            am.put("cen", anidbFile.getCensored());
            am.put("inv", anidbFile.getInvalid());
            am.put("dub", anidbFile.getDubLanguage());
            am.put("sub", anidbFile.getSubLanguage());
            am.put("src", anidbFile.getRipSource());
            am.put("res", anidbFile.getResolution());
            am.put("vid", anidbFile.getVideoCodec());
            am.put("aud", anidbFile.getAudioCodec());
            am.put("qua", anidbFile.getQuality());

            if (anidbFile.getShaHash() != null) {
                am.put("sha", anidbFile.getShaHash().toLowerCase());
                am.put("SHA", anidbFile.getShaHash().toUpperCase());
            }
            if (anidbFile.getMd5Hash() != null) {
                am.put("md5", anidbFile.getMd5Hash().toLowerCase());
                am.put("MD5", anidbFile.getMd5Hash().toUpperCase());
            }
            if (anidbFile.getCrcHash() != null) {
                am.put("CRC", anidbFile.getCrcHash().toUpperCase());
                am.put("crc", anidbFile.getCrcHash().toLowerCase());
            }
            if (anidbFile.getAnime() != null) {
                am.put("ann", anidbFile.getAnime().romajiTitle);
                am.put("kan", anidbFile.getAnime().kanjiTitle);
                am.put("eng", anidbFile.getAnime().englishTitle);
                am.put("eps", anidbFile.getAnime().episodeCount);
                am.put("typ", anidbFile.getAnime().type);
                am.put("yea", anidbFile.getAnime().year);
                am.put("gen", anidbFile.getAnime().categories.replace(",", ", "));
                am.put("lep", anidbFile.getAnime().latestEpisode);
                am.put("yen", anidbFile.getAnime().endYear);
            }
            if (anidbFile.getEpisode() != null) {
                am.put("epn", anidbFile.getEpisode().eng);
                am.put("epk", anidbFile.getEpisode().kan);
                am.put("epr", anidbFile.getEpisode().rom);
            }
            if (anidbFile.getAnime() != null && anidbFile.getEpisode() != null) {
                am.put(
                        "enr",
                        Parser.pad(
                                anidbFile.getEpisode().num, anidbFile.getAnime().getTotal()));
            }
            if (anidbFile.getGroup() != null && anidbFile.getGroupId() > 0) {
                am.put("grp", anidbFile.getGroup().shortName);
                am.put("grn", anidbFile.getGroup().name);
            } else {
                am.put("grp", "unknown");
                am.put("grn", "unknown");
            }
        } else if (ed2kHash != null) {
            am.put("ed2", ed2kHash.toLowerCase());
            am.put("ED2", ed2kHash.toUpperCase());
        }
        return am;
    }
}
