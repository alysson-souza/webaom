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

    public static String defaultMsg(int code) {
        return switch (code) {
            case CLIENT_BANNED -> "This version of WebAOM is no longer supported! Check WIKI.";
            case CLIENT_OUTDATED -> "A new version of WebAOM is out. Check WIKI!";
            case ANIDB_OUT_OF_SERVICE -> "AniDB is out of service! Try again next year.";
            case ANIDB_SERVER_ERROR -> "Internal Server Error @ AniDB!";
            case ENCRYPTION -> "Decryption failed. The connection timed out (most likely).";
            default -> "Unknown Error.";
        };
    }

    public boolean is(int i) {
        return code == i;
    }
}
