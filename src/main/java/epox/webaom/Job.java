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
 * Created on 02.08.05
 *
 * @version 	04 (1.14,1.09)
 * @author 		epoximator
 */
package epox.webaom;

import epox.av.FileInfo;
import epox.util.StringUtilities;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.AttributeMap;
import java.io.File;

public class Job {
    /// STATIC
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
        AppContext.jobc.register(-1, -1, status & M_R, getHealth());
    }

    private static String statusStr(int i) {
        switch (i) {
            case HASHWAIT:
                return "Wait/Hash";
            case HASHING:
                return "Hashing";
            case HASHED:
                return "Hashed";
            case IDENTWAIT:
                return "Wait/ID";
            case IDENTIFYING:
                return "Identifying";
            case IDENTIFIED:
                return "Identified";
            case ADDWAIT:
                return "Wait/Add";
            case ADDING:
                return "Adding";
            case ADDED:
                return "Added";
            case MOVEWAIT:
                return "Wait/Move";
            case MOVING:
                return "Moving";
            case MOVECHECK:
                return "Checking";
            case MOVED:
                return "Moved";
            case FINISHED:
                return "Finished";
            case UNKNOWN:
                return "Unknown";
            case FAILED:
                return "Failed";
            //			case INPUTWAIT:		return "Wait/Input";
            case H_PAUSED:
                return "P";
            case H_DELETED:
                return "D";
            case H_MISSING:
                return "M";
            case REMWAIT:
                return "Wait/Rem";
            case REMING:
                return "Removing";
            case PARSEWAIT:
                return "Wait/Parse";
            case PARSING:
                return "Parsing";
            default:
                return "" + i;
        }
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
        if (anidbFile == null || anidbFile.extension == null) {
            String fileName = currentFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex < 1) {
                System.out.println("No ext: " + fileName);
                return "unk";
            }
            return fileName.substring(dotIndex + 1);
        }
        return anidbFile.extension;
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

        // A.jobc.register(getRegVal(), currentStatus|health);
        AppContext.jobc.register(status & M_R, getHealth(), status & M_R, health);
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
        return matches && (fileFlags < 1 || (anidbFile != null && (anidbFile.state & fileFlags) > 0));
    }

    public boolean isLocked(int targetStatus) {
        int health = getHealth();
        if (health < H_MISSING) {
            return false;
        }
        if (health == H_MISSING) {
            switch (targetStatus) {
                case FINISHED:
                case ADDWAIT:
                case REMWAIT:
                    return false;
            }
        }
        return true;
    }

    public boolean isCorrupt() {
        return anidbFile != null && ((anidbFile.state & AniDBFile.F_CRCERR) == AniDBFile.F_CRCERR);
    }

    public boolean incompl() {
        return anidbFile == null || anidbFile.anime == null || anidbFile.episode == null;
    }

    public String getStatusText() {
        if (check(H_NORMAL)) {
            return statusStr(getStatus());
        }
        return statusStr(getStatus()) + " [" + statusStr(getHealth()) + "]";
    }

    //	INPUTWAIT	= M_USR|U_1|S_DO|F_DB,	//0xb0,

    public void setStatus(int newStatus, boolean test) {
        int health = getHealth();
        if (test) {
            // if(health>H_PAUSED&&!(newStatus==FINISHED||newStatus==REMWAIT||newStatus==ADDWAIT))
            // &&(newStatus&F_PD)==0)//extra check, could be removed maybe
            //	return;
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
            // A.jobc.register(getRegVal(), (newStatus|H_NORMAL)&M_R);
            health = H_NORMAL;
            if (newStatus == FINISHED && !currentFile.exists()) {
                health = H_MISSING;
            }
            AppContext.jobc.register(status & M_R, getHealth(), newStatus & M_R, health);
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
            case H_PAUSED:
                if (health == H_NORMAL) {
                    health = H_PAUSED; // turn pause on
                } else if (health == H_PAUSED) {
                    health = H_NORMAL; // turn pause off
                }
                break;
            case H_DELETED:
                if (health == H_DELETED) {
                    health = (currentFile.exists() ? H_NORMAL : H_MISSING);
                    AppContext.databaseManager.update(0, this, DatabaseManager.INDEX_JOB);
                } else {
                    health = H_DELETED; // delete
                    AppContext.databaseManager.removeJob(this);
                }
                break;
            case H_MISSING:
                health = H_MISSING;
                break;
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
                    processedAvInfo = StringUtilities.replace(processedAvInfo, tagContent, converted);
                }
            } else {
                processedAvInfo = "";
            }
            template = StringUtilities.replace(template, avInfoBlock, processedAvInfo);
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
            am.put("aid", anidbFile.animeId);
            am.put("eid", anidbFile.episodeId);
            am.put("fid", anidbFile.fileId);
            am.put("gid", anidbFile.groupId);
            am.put("lid", mylistId);
            am.put("ver", anidbFile.getVersion());
            am.put("ula", anidbFile.urlAnime());
            am.put("ule", anidbFile.urlEp());
            am.put("ulf", anidbFile.urlFile());
            am.put("ulg", anidbFile.urlGroup());
            am.put("ulx", anidbFile.urlMylistE(mylistId));
            am.put("ulm", anidbFile.urlMylist());
            am.put("uly", anidbFile.urlYear());
            am.put("ed2", anidbFile.ed2kHash.toLowerCase());
            am.put("ED2", anidbFile.ed2kHash.toUpperCase());
            am.put("cen", anidbFile.getCensored());
            am.put("inv", anidbFile.getInvalid());
            am.put("dub", anidbFile.dubLanguage);
            am.put("sub", anidbFile.subLanguage);
            am.put("src", anidbFile.ripSource);
            am.put("res", anidbFile.resolution);
            am.put("vid", anidbFile.videoCodec);
            am.put("aud", anidbFile.audioCodec);
            am.put("qua", anidbFile.quality);

            if (anidbFile.shaHash != null) {
                am.put("sha", anidbFile.shaHash.toLowerCase());
                am.put("SHA", anidbFile.shaHash.toUpperCase());
            }
            if (anidbFile.md5Hash != null) {
                am.put("md5", anidbFile.md5Hash.toLowerCase());
                am.put("MD5", anidbFile.md5Hash.toUpperCase());
            }
            if (anidbFile.crcHash != null) {
                am.put("CRC", anidbFile.crcHash.toUpperCase());
                am.put("crc", anidbFile.crcHash.toLowerCase());
            }
            if (anidbFile.anime != null) {
                am.put("ann", anidbFile.anime.romajiTitle);
                am.put("kan", anidbFile.anime.kanjiTitle);
                am.put("eng", anidbFile.anime.englishTitle);
                am.put("eps", anidbFile.anime.episodeCount);
                am.put("typ", anidbFile.anime.type);
                am.put("yea", anidbFile.anime.year);
                am.put("gen", anidbFile.anime.categories.replaceAll(",", ", "));
                am.put("lep", anidbFile.anime.latestEpisode);
                am.put("yen", anidbFile.anime.endYear);
            }
            if (anidbFile.episode != null) {
                am.put("epn", anidbFile.episode.eng);
                am.put("epk", anidbFile.episode.kan);
                am.put("epr", anidbFile.episode.rom);
            }
            if (anidbFile.anime != null && anidbFile.episode != null) {
                am.put("enr", Parser.pad(anidbFile.episode.num, anidbFile.anime.getTotal()));
            }
            if (anidbFile.group != null && anidbFile.groupId > 0) {
                am.put("grp", anidbFile.group.shortName);
                am.put("grn", anidbFile.group.name);
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
