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
 * @version 	1.00, 23.01.05
 * @author 		epoximator
 */
package epox.webaom.net;

public class AniDBException extends Exception {
    public static final int CLIENT_BANNED = 0;
    public static final int CLIENT_OUTDATED = 1;
    public static final int CLIENT_BUG = 2;
    public static final int CLIENT_SYSTEM = 3;
    public static final int CLIENT_USER = 4;
    public static final int ANIDB_UNREACHABLE = 5;
    public static final int ANIDB_OUT_OF_SERVICE = 6;
    public static final int ANIDB_SERVER_ERROR = 7;
    public static final int ENCRYPTION = 8;

    private final int code;

    public AniDBException(int code) {
        this(code, defaultMsg(code));
    }

    public AniDBException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public boolean is(int i) {
        return code == i;
    }

    public static String defaultMsg(int code) {
        switch (code) {
            case CLIENT_BANNED:
                return "This version of WebAOM is no longer supported! Check WIKI.";
            case CLIENT_OUTDATED:
                return "A new version of WebAOM is out. Check WIKI!";
            case ANIDB_OUT_OF_SERVICE:
                return "AniDB is out of service! Try again next year.";
            case ANIDB_SERVER_ERROR:
                return "Internal Server Error @ AniDB!";
            case ENCRYPTION:
                return "Decryption failed. The connection timed out (most likely).";
            default:
                return "Unknown Error.";
        }
    }
}
