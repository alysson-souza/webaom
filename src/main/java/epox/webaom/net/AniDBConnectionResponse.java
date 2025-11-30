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
 * Created on 19.jan.2006 12:50:06
 * Filename: Reply.java
 */
package epox.webaom.net;

import epox.util.StringUtilities;
import java.net.SocketTimeoutException;

/**
 * AniDB Connection Response - represents a parsed response from the AniDB UDP API.
 *
 * <p>
 * Parses raw server responses into structured data containing the response code, message, and
 * optional data payload. Handles tag validation, error code detection, and throws appropriate
 * exceptions for error conditions (bans, auth failures, etc.).
 */
public class AniDBConnectionResponse {
	public int code = -1;
	public String message = null;
	public String data = null;
	public String tag;

	public AniDBConnectionResponse(String expectedTag, int tagLength, String rawResponse)
			throws AniDBException, TagMismatchException, SocketTimeoutException {
		if (expectedTag != null && !rawResponse.isEmpty() && rawResponse.charAt(0) == 't') {
			tag = rawResponse.substring(0, tagLength + 1);
			if (!tag.equals(expectedTag)) {
				throw new TagMismatchException();
			}
			rawResponse = rawResponse.substring(tagLength + 2);
		}
		try {
			code = Integer.parseInt(rawResponse.substring(0, 3));
		} catch (NumberFormatException e) {
			throw new AniDBException(AniDBException.ANIDB_SERVER_ERROR, "Unexpected response");
		}

		if ((code > 600 && code < 700) && code != 602) {
			throw new AniDBException(AniDBException.ANIDB_SERVER_ERROR, rawResponse);
		}

		int separatorIndex;
		switch (code) {
			case BANNED :
				separatorIndex = rawResponse.indexOf('\n');
				String banReason = "Unknown";
				if (separatorIndex > 0) {
					banReason = rawResponse.substring(separatorIndex + 1);
				}
				throw new AniDBException(AniDBException.CLIENT_USER, "Banned: " + banReason);
			case LOGIN_ACCEPTED :
			case LOGIN_ACCEPTED_NEW_VER :
				separatorIndex = rawResponse.indexOf("LOGIN ACCEPTED");
				data = rawResponse.substring(4, separatorIndex - 1);
				message = rawResponse.substring(separatorIndex);
				break;
			case ENCRYPTION_ENABLED :
				separatorIndex = rawResponse.indexOf("ENCRYPTION ENABLED");
				data = rawResponse.substring(4, separatorIndex - 1);
				message = rawResponse.substring(separatorIndex);
				break;
			case ACCESS_DENIED :
				throw new AniDBException(AniDBException.CLIENT_USER);
			case SERVER_BUSY :
				throw new SocketTimeoutException();
			case CLIENT_BANNED :
				message = rawResponse.substring(4, 17);
				// data = rawResponse.substring(18);
				throw new AniDBException(AniDBException.CLIENT_BANNED);
			case CLIENT_VERSION_OUTDATED :
				throw new AniDBException(AniDBException.CLIENT_OUTDATED);
			case ILLEGAL_INPUT_OR_ACCESS_DENIED :
				throw new AniDBException(AniDBException.CLIENT_BUG, "Illegal Input or Access Denied");
			default :
				separatorIndex = rawResponse.indexOf('\n');
				if (separatorIndex > 0) {
					message = rawResponse.substring(4, separatorIndex);
					data = rawResponse.substring(separatorIndex + 1);
				} else {
					message = rawResponse.substring(4);
				}
		}
		data = StringUtilities.htmldesc(data);
	}

	@Override
	public String toString() {
		return code + " " + message + " [" + data + "]";
	}

	///////////////////////////////// REPLY CODES////////////////////////////////
	public static final int LOGIN_ACCEPTED = 200; // C
	public static final int LOGIN_ACCEPTED_NEW_VER = 201; // C
	public static final int LOGGED_OUT = 203; // C
	public static final int//	STATS							=206, //N
	//	TOP								=207, //N
	//	UPTIME							=208, //N
	ENCRYPTION_ENABLED = 209; // N
	public static final int MYLIST_ENTRY_ADDED = 210; // C
	public static final int MYLIST_ENTRY_DELETED = 211; // C
	public static final int FILE = 220; // C
	//	MYLIST							=221, //C
	//	MYLIST_STATS					=222, //N

	public static final int ANIME = 230; // N
	//	ANIME_BEST_MATCH				=231, //N
	//	RANDOMANIME						=232, //N

	public static final int EPISODE = 240; // N
	public static final int GROUP = 250; // N

	//	VOTED							=260, //N
	//	VOTE_FOUND						=261, //N
	//	VOTE_UPDATED					=262, //N
	//	VOTE_REVOKED					=263, //N

	public static final int/*
							 * NOTIFICATION_ENABLED =270, //C
							 * PUSHACK_CONFIRMED =280, //C
							 * NOTIFYACK_SUCCESSFUL_M =281, //C
							 * NOTIFYACK_SUCCESSFUL_N =282, //C
							 * NOTIFICATION =290, //C
							 * NOTIFYLIST =291, //C
							 * NOTIFYGET_MESSAGE =292, //C
							 * NOTIFYGET_NOTIFY =293, //C
							 * SENDMSG_SUCCESSFUL =294, //C
							 */
	/*
	 * AFFIRMATIVE/NEGATIVE 3XX
	 */
	//	PONG							=300, //C
	API_PASSWORD_NOT_DEFINED = 309;
	public static final int FILE_ALREADY_IN_MYLIST = 310; // C
	public static final int MYLIST_ENTRY_EDITED = 311; // C
	public static final int MULTIPLE_MYLIST_ENTRIES = 312; // C
	public static final int NO_SUCH_FILE = 320; // C
	public static final int/*
							 * NO_SUCH_ENTRY =321, //C
							 * MULTIPLE_FILES_FOUND =322, //N
							 *
							 * NO_SUCH_ANIME =330, //N
							 * NO_SUCH_EPISODE =340, //N
							 * NO_SUCH_GROUP =350, //N
							 *
							 * NO_SUCH_VOTE =360, //N
							 * INVALID_VOTE_TYPE =361, //N
							 * INVALID_VOTE_VALUE =362, //N
							 * PERMVOTE_NOT_ALLOWED =363, //N
							 * ALREADY_PERMVOTED =364, //N
							 *
							 * NOTIFICATION_DISABLED =370, //C
							 * NO_SUCH_PACKET_PENDING =380, //C
							 * NO_SUCH_ENTRY_M =381, //C
							 * NO_SUCH_ENTRY_N =382, //C
							 *
							 * NO_SUCH_MESSAGE =392, //C
							 * NO_SUCH_NOTIFY =393, //C
							 */
	NO_SUCH_USER = 394; // C

	//	NO_SUCH_DATA_ENTRY				=396, //N

	/*
	 * NEGATIVE 4XX
	 */

	public static final int NOT_LOGGED_IN = 403; // C
	public static final int NO_SUCH_MYLIST_FILE = 410; // C
	public static final int NO_SUCH_MYLIST_ENTRY = 411; // C

	/*
	 * CLIENT SIDE FAILURE 5XX
	 */

	public static final int LOGIN_FAILED = 500; // C
	public static final int LOGIN_FIRST = 501; // C
	public static final int ACCESS_DENIED = 502; // C
	public static final int CLIENT_VERSION_OUTDATED = 503; // C
	public static final int CLIENT_BANNED = 504; // C
	public static final int ILLEGAL_INPUT_OR_ACCESS_DENIED = 505; // C
	public static final int INVALID_SESSION = 506; // C
	public static final int ENCODING_NOT_SUPPORTED = 510; // M
	public static final int BANNED = 555; // C
	//	UNKNOWN_COMMAND					=598, //C

	/*
	 * SERVER SIDE FAILURE 6XX
	 */

	public static final int INTERNAL_SERVER_ERROR = 600; // C
	public static final int SERVER_BUSY = 602;
	//	ANIDB_OUT_OF_SERVICE			=601, //C
	//	API_VIOLATION					=666; //C
}
