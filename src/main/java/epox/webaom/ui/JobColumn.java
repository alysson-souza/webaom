// Copyright (C) 2025 Alysson Souza
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

package epox.webaom.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum defining all columns in the Jobs table. Centralizes column metadata (index, tag,
 * description, type) for maintainability.
 */
public enum JobColumn {
    // Row metadata
    NUMB(0, "#", "Row", Integer.class),

    // IDs
    LIDN(1, "lid", "Mylist ID", Integer.class),
    FIDN(2, "fid", "File ID", Integer.class),
    AIDN(3, "aid", "Anime ID", Integer.class),
    EIDN(4, "eid", "Episode ID", Integer.class),
    GIDN(5, "gid", "Group ID", Integer.class),

    // Anime metadata
    AYEA(6, "yea", "Year Start", Integer.class),
    AEPS(7, "eps", "Episodes", Integer.class),
    ALEP(8, "lep", "Latest Episode", Integer.class),

    // File metadata
    FSIZ(9, "size", "File Size", Long.class),
    FLEN(10, "len", "Length", Integer.class),
    FILE(11, "file", "File Path", String.class),
    PATH(12, "path", "Directory", String.class),
    NAME(13, "name", "Filename", String.class),
    STAT(14, "status", "Status", String.class),

    // Anime titles
    AROM(15, "ann", "Romaji Title", String.class),
    AKAN(16, "kan", "Kanji Title", String.class),
    AENG(17, "eng", "English Title", String.class),
    ATYP(18, "typ", "Anime Type", String.class),

    // Episode info
    ENUM(19, "enr", "Episode Number", String.class),
    EENG(20, "epn", "Episode Title (EN)", String.class),
    EKAN(21, "epk", "Episode Title (JP)", String.class),
    EROM(22, "epr", "Episode Title (Romaji)", String.class),

    // Group info
    GNAM(23, "grn", "Group Name", String.class),
    GSHO(24, "grp", "Group Short Name", String.class),

    // File attributes
    FDUB(25, "dub", "Audio Language", String.class),
    FSUB(26, "sub", "Subtitle Language", String.class),
    FSRC(27, "src", "Source", String.class),
    FQUA(28, "qua", "Quality", String.class),
    FRES(29, "res", "Resolution", String.class),
    FVID(30, "vid", "Video Codec", String.class),
    FAUD(31, "aud", "Audio Codec", String.class),

    // Hash metadata
    FMDS(32, "mds", "MD5 / SHA1", String.class),
    FMDA(33, "mda", "MD5 / Audio", String.class),

    // Extended anime metadata
    AYEN(34, "yen", "Year End", Integer.class);

    // Pseudo-column for row object access
    public static final int JOB = -1;

    private final int index;
    private final String tag;
    private final String description;
    private final Class<?> type;

    JobColumn(int index, String tag, String description, Class<?> type) {
        this.index = index;
        this.tag = tag;
        this.description = description;
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public String getTag() {
        return tag;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getType() {
        return type;
    }

    // Fast lookups
    private static final JobColumn[] BY_INDEX;
    private static final Map<String, JobColumn> BY_TAG = new HashMap<>();

    static {
        JobColumn[] cols = values();
        BY_INDEX = new JobColumn[cols.length];
        for (JobColumn col : cols) {
            BY_INDEX[col.index] = col;
            BY_TAG.put(col.tag, col);
        }
    }

    public static JobColumn fromIndex(int index) {
        return (index >= 0 && index < BY_INDEX.length) ? BY_INDEX[index] : null;
    }

    public static JobColumn fromTag(String tag) {
        return BY_TAG.get(tag);
    }

    public static int getColumnCount() {
        return BY_INDEX.length;
    }
}
