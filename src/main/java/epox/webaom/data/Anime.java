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
 * Created on 31.07.05
 * Copyright 2005 epoximator
 *
 * @version 	02
 * @author 		epoximator
 */
package epox.webaom.data;

import epox.util.Bits;
import epox.util.U;
import epox.webaom.ui.TableModelAlt;

/**
 * Represents an anime entry from AniDB with metadata and episode tracking.
 */
public class Anime extends Base {
	/** Primary sort column index (positive = ascending, negative = descending). */
	private static int primarySortColumn = 1;
	/** Secondary sort column index for tie-breaking. */
	private static int secondarySortColumn = 1;

	/**
	 * Sets the sort column, updating primary/secondary sort order.
	 *
	 * @param column
	 *            column index (positive = ascending, negative = descending)
	 */
	public static void setSortColumn(int column) {
		if (Math.abs(column) != Math.abs(primarySortColumn)) {
			secondarySortColumn = primarySortColumn;
		}
		primarySortColumn = column;
	}

	/** Start year of the anime. */
	public int year;
	/** End year of the anime (same as year if still ongoing or single year). */
	public int endYear;
	/** Total episode count (0 if unknown/ongoing). */
	public int episodeCount;
	/** Latest known episode number. */
	public int latestEpisode;
	/** Completion percentage (0-100). */
	public int completionPercent;
	/** Anime type (TV, OVA, Movie, etc). */
	public String type;
	/** Romaji title. */
	public String romajiTitle;
	/** Kanji/Japanese title. */
	public String kanjiTitle;
	/** English title. */
	public String englishTitle;
	/** Categories/genres (comma-separated). */
	public String categories;
	/** Episode progress tracker (bit per episode). */
	public Bits episodeProgress = null;

	public Anime(int id) {
		this.id = id;
	}

	public Anime(String[] fields) {
		int index = 0;
		id = U.i(fields[index++]);
		episodeCount = U.i(fields[index++]);
		latestEpisode = U.i(fields[index++]);
		year = U.i(fields[index++].substring(0, 4));
		if (fields[index - 1].length() == 9) { // Data is from AniDB in XXXX-YYYY format
			endYear = U.i(fields[index - 1].substring(5, 9));
		} else if (fields.length == 10) { // Data is from serialize file
			endYear = U.i(fields[9].substring(0, 4));
		} else { // Data is from AniDB in XXXX format
			endYear = year;
		}
		type = fields[index++];
		romajiTitle = fields[index++];
		kanjiTitle = U.n(fields[index++]);
		englishTitle = U.n(fields[index++]);
		categories = fields[index++];

		init();
	}

	public void init() {
		if (episodeProgress == null) {
			episodeProgress = new Bits(episodeCount > 0 ? episodeCount : latestEpisode);
		}
	}

	public int getTotal() {
		return (episodeCount < 1 ? (latestEpisode < 10 ? 99 : latestEpisode) : episodeCount);
	}

	/** Title display priority: 0=romaji, 1=kanji, 2=english. */
	public static int TITLE_PRIORITY = 0;

	public String toString() {
		switch (TITLE_PRIORITY) {
			case 1 :
				return (kanjiTitle == null ? romajiTitle : kanjiTitle) + " (" + episodeCount + ":" + latestEpisode
						+ ")";
			case 2 :
				return (englishTitle == null ? romajiTitle : englishTitle) + " (" + episodeCount + ":" + latestEpisode
						+ ")";
			default :
				return romajiTitle + " (" + episodeCount + ":" + latestEpisode + ")";
		}
	}

	public String serialize() {
		return "" + id + S + episodeCount + S + latestEpisode + S + year + S + type + S + romajiTitle + S + kanjiTitle
				+ S + englishTitle + S + categories + S + endYear;
	}

	private void setOrFill(int episodeNumber, boolean value) {
		if (!episodeProgress.set(episodeNumber - 1, value)) {
			if (episodeNumber > (episodeCount > 0 ? episodeCount : latestEpisode)) {
				if (!episodeProgress.fill(value)) {
					System.out.println("@ Completion " + (value ? "over" : "under") + "flow: " + this + " [" + type
							+ "] epno=" + episodeNumber);
				}
			}
		}
	}

	/**
	 * Registers an episode as watched/unwatched.
	 *
	 * @param episode
	 *            the episode to register
	 * @param watched
	 *            true if watched, false if unwatched
	 */
	public void registerEpisode(Ep episode, boolean watched) {
		if (Character.isDigit(episode.num.charAt(0))) {
			try {
				setOrFill(Integer.parseInt(episode.num), watched);
			} catch (NumberFormatException ex) {
				String[] commaParts = episode.num.split(",");
				for (int i = 0; i < commaParts.length; i++) {
					String[] rangeParts = commaParts[0].split("-");
					switch (rangeParts.length) {
						case 2 :
							for (int j = Integer.parseInt(rangeParts[0]); j <= Integer.parseInt(rangeParts[1]); j++) {
								setOrFill(j, watched);
							}
							break;
						case 1 :
							setOrFill(Integer.parseInt(rangeParts[0]), watched);
							break;
						default :
							System.out.println("@ Anime.registerEpisode: Unexpected epno format!");
					}
				}
			}
		} else if (episode.num.charAt(0) == 'O') {
			try {
				if (episode.eng.startsWith("Episodes ")) {
					int dashIndex = episode.eng.indexOf('-');

					if (dashIndex > 0) {
						int startEp = U.i(episode.eng.substring(9, dashIndex));
						int spaceIndex = episode.eng.indexOf(' ', dashIndex);
						if (spaceIndex < dashIndex) {
							spaceIndex = episode.eng.length();
						}
						int endEp = U.i(episode.eng.substring(dashIndex + 1, spaceIndex));
						for (int epIndex = startEp - 1; epIndex < endEp; epIndex++) {
							episodeProgress.set(epIndex, watched);
						}
					}
				}
			} catch (Exception ignored) {
				// Ignore parsing errors for special episode formats
			}
		}
	}

	/** Updates the completion percentage based on episode progress. */
	public void updateCompletionPercent() {
		int maxEpisodes = episodeCount;
		if (maxEpisodes == 0) {
			maxEpisodes = -latestEpisode;
		}
		if (maxEpisodes == 0) {
			completionPercent = 0;
		} else {
			completionPercent = (episodeProgress.countSetBits() * 100) / maxEpisodes;
		}
	}

	public int getCompletionPercent() {
		return completionPercent;
	}

	/**
	 * Returns a character indicating the missing episode pattern.
	 * ' ' = complete or no gaps
	 * 'f' = missing first episodes
	 * 'l' = has gaps but has last
	 * 'e' = ends missing
	 * Other chars = multiple holes
	 */
	public char getMissingPattern() {
		int transitions = episodeProgress.switchCount();
		if (transitions < 1) {
			return ' ';
		}
		if (transitions < 2) {
			return episodeProgress.last() ? 'f' : ' ';
		}
		if (transitions < 3) {
			return episodeProgress.first() ? 'l' : 'e';
		}
		return (char) (62 + transitions);
	}

	public int compareTo(Object o) {
		int result = compareBy(o, primarySortColumn);
		if (result == 0 && primarySortColumn != secondarySortColumn) {
			return compareBy(o, secondarySortColumn);
		}
		return result;
	}

	/**
	 * Compares this anime with another by the specified column.
	 *
	 * @param obj
	 *            the object to compare
	 * @param column
	 *            the column index (negative for descending)
	 * @return comparison result
	 */
	public int compareBy(Object obj, int column) {
		if (obj instanceof Anime a) {
			Anime b = this;
			if (column < 0) {
				b = a;
				a = this;
			}
			switch (Math.abs(column) - 1) {
				case TableModelAlt.NAME :
					return b.romajiTitle.compareTo(a.romajiTitle);
				case TableModelAlt.TYPE :
					return b.type.compareTo(a.type);
				case TableModelAlt.YEAR :
					return b.year - a.year;
				case TableModelAlt.NUMB :
					return b.size() - a.size();
				case TableModelAlt.SIZE :
					return (int) ((b.totalSize - a.totalSize) / 100000);
				case TableModelAlt.PRCT :
					return a.getCompletionPercent() - b.getCompletionPercent();
				case TableModelAlt.LAST :
					return a.getMissingPattern() - b.getMissingPattern();
			}
		}
		return super.compareTo(obj);
	}

	public void clear() {
		super.clear();
		episodeProgress.reset();
	}
}
