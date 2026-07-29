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

    /**
     * Outcome of a MYLISTADD request.
     *
     * @param listEntryId the MyList entry id, or 0 when the request failed
     * @param created whether a new entry was created
     * @param edited whether an existing entry was updated with the supplied values
     */
    public record MylistAddOutcome(int listEntryId, boolean created, boolean edited) {
        static final MylistAddOutcome FAILED = new MylistAddOutcome(0, false, false);

        public boolean succeeded() {
            return listEntryId > 0;
        }
    }

    /**
     * Adds the file to MyList, leaving any existing entry untouched.
     *
     * <p>This is the behaviour the desktop job pipeline relies on: a bulk run must never rewrite the
     * state of entries the user already curated. Use {@link #addOrEditMylistEntry} when the caller
     * has explicitly asked for the supplied values to be applied.
     */
    public int addFileToMylist(Job job, Mylist mylistEntry) throws AniDBException {
        return addMylistEntry(job, mylistEntry, false).listEntryId();
    }

    /**
     * Adds the file to MyList, updating the existing entry when AniDB reports one already exists.
     *
     * <p>AniDB answers a plain {@code edit=0} add for a known file with 310 and ignores the supplied
     * values, so the requested state is re-sent as an edit rather than being silently dropped. Only
     * call this when the values came from an explicit request; it overwrites whatever is on record.
     */
    public MylistAddOutcome addOrEditMylistEntry(Job job, Mylist mylistEntry) throws AniDBException {
        return addMylistEntry(job, mylistEntry, true);
    }

    private MylistAddOutcome addMylistEntry(Job job, Mylist mylistEntry, boolean editExisting) throws AniDBException {
        AniDBConnectionResponse response = send("MYLISTADD", mylistAddParameters(job, mylistEntry, false), true);
        if (response == null) {
            return MylistAddOutcome.FAILED;
        }
        switch (response.code) {
            case AniDBConnectionResponse.MYLIST_ENTRY_ADDED:
                return new MylistAddOutcome(Integer.parseInt(response.data), true, false);
            case AniDBConnectionResponse.FILE_ALREADY_IN_MYLIST: {
                // Response format: {lid}|{fid}|{eid}|{aid}|{gid}|{date}|{state}|...
                String[] fields = StringUtilities.split(response.data, '|');
                int listEntryId = Integer.parseInt(fields[0]);
                if (!editExisting) {
                    error(job.anidbFile.getDefaultName() + " is already in mylist.");
                    return new MylistAddOutcome(listEntryId, false, false);
                }
                AniDBConnectionResponse editResponse =
                        send("MYLISTADD", mylistAddParameters(job, mylistEntry, true), true);
                if (editResponse != null && editResponse.code == AniDBConnectionResponse.MYLIST_ENTRY_EDITED) {
                    return new MylistAddOutcome(listEntryId, false, true);
                }
                error(job.anidbFile.getDefaultName() + " is already in mylist and could not be updated"
                        + (editResponse == null ? "." : ": " + editResponse.message));
                return new MylistAddOutcome(listEntryId, false, false);
            }
            case AniDBConnectionResponse.NO_SUCH_MYLIST_FILE:
                error(job.anidbFile.getDefaultName() + " was not found in AniDB.");
                break;
            case AniDBConnectionResponse.MYLIST_ENTRY_EDITED:
            case AniDBConnectionResponse.NO_SUCH_MYLIST_ENTRY:
            default:
                error("Unexpected response (" + response.code + "): " + response.message);
        }
        return MylistAddOutcome.FAILED;
    }

    private String mylistAddParameters(Job job, Mylist mylistEntry, boolean edit) {
        return "fid=" + job.anidbFile.getFileId() + "&state="
                + mylistEntry.state + "&filestate="
                + mylistEntry.filestate + "&viewed="
                + mylistEntry.viewed + "&source="
                + escapeForXml(mylistEntry.source) + "&storage="
                + escapeForXml(mylistEntry.storage) + "&other="
                + escapeForXml(mylistEntry.other) + "&edit="
                + (edit ? 1 : 0);
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
