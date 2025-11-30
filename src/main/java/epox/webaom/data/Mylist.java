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
 * Created on 19.des.2005 12:27:35
 * Filename: aMylist.java
 */
package epox.webaom.data;

/**
 * Represents a mylist entry for AniDB.
 * Contains metadata about how a file is stored in the user's collection.
 */
public class Mylist {
	/** File state: 0=unknown, 1=on HDD, 2=on CD, 3=deleted. */
	public int state;
	/** Viewed status: 0=not viewed, 1=viewed. */
	public int viewed;
	/** Storage location (e.g., "External HDD", "DVD-R"). */
	public String storage;
	/** Source of the file (e.g., "Nyaa", "IRC"). */
	public String source;
	/** Other/additional notes. */
	public String other;

	/*
	 * public static final int S_UNKNOWN = 0,
	 * S_ON_HDD = 1,
	 * S_ON_CD = 2,
	 * S_DELETED = 3;
	 */
}
