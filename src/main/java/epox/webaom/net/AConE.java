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
 * Created on 10.des.2005 18:58:48
 * Filename: aConEx.java
 */
package epox.webaom.net;

import epox.swing.Log;
import epox.util.U;
import epox.webaom.Job;
import epox.webaom.data.Mylist;

public class AConE extends ACon {
	/** Query parameters for file and anime data fields bitmasks */
	private static final String FILE_AND_ANIME_CODES = "&fcode=123682590&acode=75435779";

	public AConE(Log log, AConS settings) {
		super(log, settings);
	}

	public String[] retrieveFileData(long fileSize, String ed2kHash, String fileName) throws AConEx {
		String query = "size=" + fileSize + "&ed2k=" + ed2kHash + FILE_AND_ANIME_CODES;
		String ed2kLink = "ed2k://|file|" + fileName + "|" + fileSize + "|" + ed2kHash + "|";
		return retrieveFileData(send("FILE", query, true), ed2kLink);
	}

	public String[] retrieveFileData(int fileId, String fileName) throws AConEx {
		return retrieveFileData(send("FILE", "fid=" + fileId + FILE_AND_ANIME_CODES, true), fileName);
	}

	private String[] retrieveFileData(AConR response, String fileName) {
		switch (response.code) {
			case AConR.FILE :
				return U.split(response.data, '|');
			case AConR.NO_SUCH_FILE :
				error("No such file in AniDB: " + fileName);
				break;
			case AConR.INTERNAL_SERVER_ERROR :
				error("Internal server error on " + fileName + ". (Illegal char in epname?)");
				break;
			default :
				error("Unexpected response (" + response.code + "): " + response.message);
		}
		return null;
	}

	/** Escapes special characters for XML/HTML encoding and converts newlines to BR tags. */
	public static String escapeForXml(String input) {
		String escaped = U.replace(input, "&", "&amp;");
		escaped = U.replace(escaped, "\r", "");
		escaped = U.replace(escaped, "\n", "<br />");
		return escaped;
	}

	public int addFileToMylist(Job job, Mylist mylistEntry) throws AConEx {
		String escapedSource = escapeForXml(mylistEntry.sou);
		String escapedStorage = escapeForXml(mylistEntry.sto);
		String escapedOther = escapeForXml(mylistEntry.oth);
		StringBuilder params = new StringBuilder();
		params.append("fid=").append(job.anidbFile.fid);
		params.append("&state=").append(mylistEntry.stt);
		params.append("&viewed=").append(mylistEntry.vie);
		params.append("&source=").append(escapedSource);
		params.append("&storage=").append(escapedStorage);
		params.append("&other=").append(escapedOther);
		params.append("&edit=0");

		AConR response = send("MYLISTADD", params.toString(), true);
		if (response == null) {
			return 0;
		}
		switch (response.code) {
			case AConR.MYLIST_ENTRY_ADDED :
				return Integer.parseInt(response.data);
			case AConR.FILE_ALREADY_IN_MYLIST :
				error(job.anidbFile.def + " is already in mylist.");
				return Integer.parseInt(response.data);
			case AConR.NO_SUCH_MYLIST_FILE :
				error(job.anidbFile.def + " was not found in AniDB.");
			case AConR.MYLIST_ENTRY_EDITED :
			case AConR.NO_SUCH_MYLIST_ENTRY :
			default :
				error("Unexpected response (" + response.code + "): " + response.message);
		}
		return 0;
	}

	public boolean removeFromMylist(int listEntryId, String fileName) throws AConEx {
		AConR response = send("MYLISTDEL", "lid=" + listEntryId, true);
		switch (response.code) {
			case AConR.MYLIST_ENTRY_DELETED :
				return true;
			case AConR.NO_SUCH_MYLIST_ENTRY :
				return false;
			default :
				error(response.message + " (" + fileName + ")");
		}
		return false;
	}
}
