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

/*
 * Created on 10.des.2005 18:58:48
 * Filename: aConEx.java
 */
package epox.webaom.net;

import epox.swing.Log;
import epox.util.StringUtilities;
import epox.webaom.Job;
import epox.webaom.data.Mylist;

public class AniDBFileClient extends AniDBConnection {
    /** Query parameters for file and anime data fields bitmasks */
    private static final String FILE_AND_ANIME_CODES = "&fcode=123682590&acode=75435779";

    public AniDBFileClient(Log log, AniDBConnectionSettings settings) {
        super(log, settings);
    }

    /** Escapes special characters for XML/HTML encoding and converts newlines to BR tags. */
    public static String escapeForXml(String input) {
        String escaped = input.replace("&", "&amp;");
        escaped = escaped.replace("\r", "");
        escaped = escaped.replace("\n", "<br />");
        return escaped;
    }

    public String[] retrieveFileData(long fileSize, String ed2kHash, String fileName) throws AniDBException {
        String query = "size=" + fileSize + "&ed2k=" + ed2kHash + FILE_AND_ANIME_CODES;
        String ed2kLink = "ed2k://|file|" + fileName + "|" + fileSize + "|" + ed2kHash + "|";
        return retrieveFileData(send("FILE", query, true), ed2kLink);
    }

    public String[] retrieveFileData(int fileId, String fileName) throws AniDBException {
        return retrieveFileData(send("FILE", "fid=" + fileId + FILE_AND_ANIME_CODES, true), fileName);
    }

    private String[] retrieveFileData(AniDBConnectionResponse response, String fileName) {
        switch (response.code) {
            case AniDBConnectionResponse.FILE:
                return StringUtilities.split(response.data, '|');
            case AniDBConnectionResponse.NO_SUCH_FILE:
                error("No such file in AniDB: " + fileName);
                break;
            case AniDBConnectionResponse.INTERNAL_SERVER_ERROR:
                error("Internal server error on " + fileName + ". (Illegal char in epname?)");
                break;
            default:
                error("Unexpected response (" + response.code + "): " + response.message);
        }
        return null;
    }

    public int addFileToMylist(Job job, Mylist mylistEntry) throws AniDBException {
        String escapedSource = escapeForXml(mylistEntry.source);
        String escapedStorage = escapeForXml(mylistEntry.storage);
        String escapedOther = escapeForXml(mylistEntry.other);
        String params = "fid=" + job.anidbFile.getFileId() + "&state=" + mylistEntry.state + "&viewed="
                + mylistEntry.viewed + "&source=" + escapedSource + "&storage=" + escapedStorage + "&other="
                + escapedOther + "&edit=0";

        AniDBConnectionResponse response = send("MYLISTADD", params, true);
        if (response == null) {
            return 0;
        }
        switch (response.code) {
            case AniDBConnectionResponse.MYLIST_ENTRY_ADDED:
                return Integer.parseInt(response.data);
            case AniDBConnectionResponse.FILE_ALREADY_IN_MYLIST:
                error(job.anidbFile.getDefaultName() + " is already in mylist.");
                // Response format: {lid}|{fid}|{eid}|{aid}|{gid}|{date}|{state}|...
                String[] fields = StringUtilities.split(response.data, '|');
                return Integer.parseInt(fields[0]);
            case AniDBConnectionResponse.NO_SUCH_MYLIST_FILE:
                error(job.anidbFile.getDefaultName() + " was not found in AniDB.");
            case AniDBConnectionResponse.MYLIST_ENTRY_EDITED:
            case AniDBConnectionResponse.NO_SUCH_MYLIST_ENTRY:
            default:
                error("Unexpected response (" + response.code + "): " + response.message);
        }
        return 0;
    }

    public boolean removeFromMylist(int listEntryId, String fileName) throws AniDBException {
        AniDBConnectionResponse response = send("MYLISTDEL", "lid=" + listEntryId, true);
        switch (response.code) {
            case AniDBConnectionResponse.MYLIST_ENTRY_DELETED:
                return true;
            case AniDBConnectionResponse.NO_SUCH_MYLIST_ENTRY:
                return false;
            default:
                error(response.message + " (" + fileName + ")");
        }
        return false;
    }
}
