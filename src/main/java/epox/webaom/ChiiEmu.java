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
 * Created on 21.nov.2005 15:56:58
 * Filename: ChiiEmulator.java
 */
package epox.webaom;

import epox.swing.CommandModel;
import epox.swing.Log;
import epox.util.U;
import epox.webaom.net.ACon;
import epox.webaom.net.AConEx;
import epox.webaom.net.AConR;

public class ChiiEmu implements CommandModel {
	private Thread workerThread;
	protected ACon aniDbConnection;
	private Log log;

	public ChiiEmu(ACon connection) {
		log = null;
		aniDbConnection = connection;
	}

	public void handleCommand(String command) {
		aniDbConnection = A.conn;
		if (command.startsWith("!font")) {
			if (command.length() > 6) {
				A.font = command.substring(6).trim();
				A.setFont(A.font);
			} else {
				println("!font name,size");
			}
		} else if (aniDbConnection == null) {
			println("Not logged in. Click the Login button first.");
		} else if (workerThread == null) {
			workerThread = new EmuWorker(command);
			workerThread.start();
		} else {
			println("Please Wait!");
		}
	}

	public void setLog(Log log) {
		this.log = log;
	}

	public void println(Object message) {
		if (log == null) {
			return;
		}
		if (message == null) {
			log.println("err..");
		} else {
			log.println(message.toString());
		}
	}

	private class EmuWorker extends Thread {
		private final String commandText;

		EmuWorker(String commandText) {
			super("ChiiEmu");
			this.commandText = commandText;
		}

		public void run() {
			String result = "Query failed.";
			try {
				result = executeCommand();
			} catch (AConEx ex) {
				result = ex.getMessage();
			} catch (NullPointerException ex) {
				/* don't care */
			}
			println(result);
			workerThread = null;
		}

		public String executeCommand() throws AConEx {
			if (commandText == null | commandText.length() < 3) {
				return "No Command";
			}
			if (commandText.startsWith("!uptime")) {
				return formatUptime(aniDbConnection.send("UPTIME", null, true).data);
			}
			if (commandText.startsWith("!mystats")) {
				return formatMyStats(aniDbConnection.send("MYLISTSTATS", null, true).data);
			}
			if (commandText.startsWith("!mylist")) {
				String param = commandText.substring(7).trim();
				if (param.isEmpty()) {
					return "Usage: !mylist <anime name or id>";
				}
				return formatMyList(queryByIdOrName("MYLIST", "aid=", "aname=", param));
			}
			if (commandText.startsWith("!anime")) {
				return formatAnime(queryByIdOrName("ANIME", "aid=", "aname=", commandText.substring(6).trim()));
			}
			if (commandText.startsWith("!group")) {
				return formatGroup(queryByIdOrName("GROUP", "gid=", "gname=", commandText.substring(6).trim()));
			}
			if (commandText.startsWith("!randomanime")) {
				return randomAnime(commandText.substring(12).trim());
			}
			if (commandText.startsWith("!state2")) {
				return updateStorageOrState(commandText.substring(7).trim(), USAGE_STATE2);
			}
			if (commandText.startsWith("!state")) {
				return updateState(commandText.substring(6).trim());
			}
			if (commandText.startsWith("!watched")) {
				return updateWatchedStatus(commandText.substring(8).trim());
			}
			if (commandText.startsWith("!storage")) {
				return updateStorageOrState(commandText.substring(8).trim(), USAGE_STORAGE);
			}
			if (commandText.charAt(0) == '?') {
				return aniDbConnection.send(commandText.substring(1), true);
			}
			return "Unknown command";
		}

		public String formatMinutes(int totalMinutes) {
			int days = totalMinutes / 1440;
			int hours = (totalMinutes % 1440) / 60;
			int minutes = totalMinutes % 60;
			if (days > 0) {
				return days + "d " + hours + "h " + minutes + "m";
			} else if (hours > 0) {
				return hours + "h " + minutes + "m";
			}
			return minutes + "m";
		}

		public String formatUptime(String uptime) {
			long uptimeMinutes = Long.parseLong(uptime) / 60000;
			int days = (int) (uptimeMinutes / 1440);
			int hours = (int) ((uptimeMinutes % 1440) / 60);
			int minutes = (int) (uptimeMinutes % 1440 % 60);
			return "UPTIME: " + days + "d " + hours + "h " + minutes + "m";
		}

		public String queryByIdOrName(String command, String idParameter, String nameParameter, String value)
				throws AConEx {
			try {
				return aniDbConnection.send(command, idParameter + Integer.parseInt(value), true).data;
			} catch (NumberFormatException ex) {
				/* part of plan */
			}
			if (nameParameter == null) {
				return "Not possible";
			}
			return aniDbConnection.send(command, nameParameter + value, true).data;
		}

		public String updateStorageOrState(String parameters, int usageType) throws AConEx {
			String[] arguments = U.split(parameters, ',');
			if (arguments.length != 4) {
				return getUsageMessage(usageType);
			}
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = arguments[i].trim();
				if (i < (arguments.length - 1) && arguments[i].isEmpty()) {
					return getUsageMessage(usageType);
				}
			}
			StringBuffer requestParameters = new StringBuffer(50);
			requestParameters.append("edit=1");
			int id;
			try {
				id = Integer.parseInt(arguments[0]);
				requestParameters.append("&aid=");
				requestParameters.append(id);
			} catch (NumberFormatException ex) {
				requestParameters.append("&aname=");
				requestParameters.append(arguments[0]);
			}
			try {
				id = Integer.parseInt(arguments[1]);
				requestParameters.append("&gid=");
				requestParameters.append(id);
			} catch (NumberFormatException ex) {
				if (!arguments[1].equalsIgnoreCase("all")) {
					requestParameters.append("&gname=");
					requestParameters.append(arguments[1]);
				}
			}
			try {
				requestParameters.append("&epno=");
				if (arguments[2].equalsIgnoreCase("all")) {
					requestParameters.append("0");
				} else if (arguments[2].length() > 5 && arguments[2].toLowerCase().startsWith("upto ")) {
					requestParameters.append(-Integer.parseInt(arguments[2].substring(5)));
				} else {
					requestParameters.append(Integer.parseInt(arguments[2]));
				}
			} catch (NumberFormatException ex) {
				return getUsageMessage(usageType);
			}
			String command;
			if (USAGE_STORAGE == usageType) {
				command = "STORAGE";
				requestParameters.append("&storage=");
				requestParameters.append(arguments[3]);
			} else {
				command = "STATE2";
				requestParameters.append("&state=");
				if (arguments[3].equalsIgnoreCase("unknown")) {
					requestParameters.append(0);
				} else if (arguments[3].equalsIgnoreCase("hdd")) {
					requestParameters.append(1);
				} else if (arguments[3].equalsIgnoreCase("cd")) {
					requestParameters.append(2);
				} else if (arguments[3].equalsIgnoreCase("deleted")) {
					requestParameters.append(3);
				} else {
					return command + ": no such state";
				}
			}

			AConR response = aniDbConnection.send("MYLISTADD", requestParameters.toString(), true);
			if (response.code != AConR.MYLIST_ENTRY_EDITED || response.data == null || response.data.isEmpty()) {
				return command + ": no such entry";
			}
			if (response.data.equals("1")) {
				return command + ": one entry updated";
			}
			return command + ": updated " + response.data + " entries";
		}

		public String randomAnime(String param) throws AConEx {
			int type = 0;
			if (param.equals("watched")) {
				type = 1;
			} else if (param.equals("unwatched")) {
				type = 2;
			} else if (param.equals("all")) {
				type = 3;
			}
			AConR response = aniDbConnection.send("RANDOMANIME", "type=" + type, true);
			return "RANDOM " + formatAnime(response.data);
		}

		public String formatAnime(String response) {
			String[] fields = U.split(response, '|');
			if (fields.length < 15) {
				return response;
			}
			// Fields: aid|eps|highest ep|specials|rating|votes|temp rating|temp votes|
			//         review rating|review count|year|type|romaji|kanji|english|other|short|synonyms
			String ratingText = ((float) Integer.parseInt(fields[4]) / 100) + " (" + fields[5] + " votes)";
			String reviewsText = fields[9] + " reviews (avg: " + ((float) Integer.parseInt(fields[8]) / 100) + ")";
			String englishName = fields.length > 14 && !fields[14].isEmpty() ? fields[14] : fields[12];
			String animeDescription = "ANIME: " + fields[12] + " (" + fields[0] + "), " + englishName + ", " + fields[1]
					+ " eps, Year: " + fields[10] + ", Type: " + fields[11] + ", Rating: " + ratingText + ", "
					+ reviewsText + " https://anidb.net/a" + fields[0];
			return animeDescription;
		}

		public String formatGroup(String response) {
			String[] fields = U.split(response, '|');
			if (fields.length < 9) {
				return response;
			}
			// Fields: gid|rating|votes|acount|fcount|name|short|irc channel|irc server|url|...
			String ratingText = ((float) Integer.parseInt(fields[1]) / 100) + " (" + fields[2] + " votes)";
			String databaseText = fields[3] + " animes/" + fields[4] + " files";
			String ircInfo = !fields[7].isEmpty() ? fields[7] + "@" + fields[8] : "";
			String urlInfo = fields.length > 9 && !fields[9].isEmpty() ? fields[9] : "";
			String groupDescription = "GROUP: " + fields[5] + " [" + fields[6] + "] (" + fields[0] + "), " + "rating: "
					+ ratingText + ", db: " + databaseText + (!ircInfo.isEmpty() ? ", irc: " + ircInfo : "")
					+ (!urlInfo.isEmpty() ? ", url: " + urlInfo : "") + " https://anidb.net/g" + fields[0];
			return groupDescription;
		}

		public String formatMyList(String response) {
			String[] fields = U.split(response, '|');
			if (fields.length < 6) {
				return response;
			}
			// Fields: name|eps|special eps|???|???|eps in mylist|watched eps|state|...
			String animeName = fields[0];
			String totalEps = fields[1].isEmpty() ? "?" : fields[1];
			String epsInList = fields[5].isEmpty() ? "0" : fields[5];
			String watchedEps = fields.length > 6 && !fields[6].isEmpty() ? fields[6] : "";
			String state = fields.length > 7 && !fields[7].isEmpty() ? fields[7] : "";
			String result = "MYLIST: " + animeName + " - " + epsInList + "/" + totalEps + " eps";
			if (!watchedEps.isEmpty()) {
				result += ", watched: " + watchedEps;
			}
			if (!state.isEmpty()) {
				result += " (" + state + ")";
			}
			return result;
		}

		public String formatMyStats(String response) {
			String[] fields = U.split(response, '|');
			if (fields.length < 16) {
				return response;
			}
			String watchedEpsText = fields[13] + " / " + fields[12] + "% watched";
			String mylistSizeText = U.sbyte(1048576L * U.i(fields[3])) + ", " + fields[11] + "% of AniDB, " + fields[10]
					+ "% watched";
			String contributionText = fields[4] + " animes, " + fields[5] + " eps, " + fields[6] + " files, "
					+ fields[7] + " groups";
			String votesReviewsText = fields[14] + " votes, " + fields[15] + " reviews added to DB";
			String leechLameText = "Leech factor: " + fields[8] + "%, Lameness: " + fields[9] + "%";
			String viewedTime = fields.length > 16 ? " Viewed: " + formatMinutes(U.i(fields[16])) + "." : "";
			String description = "MYSTATS: " + fields[0] + " animes, " + fields[1] + " eps (" + watchedEpsText
					+ ") and " + fields[2] + " files in mylist (" + mylistSizeText + "). " + contributionText + ". "
					+ votesReviewsText + ". " + leechLameText + "." + viewedTime;
			return description;
		}
	}

	/**
	 * !state <anime> <ep#> <state> !state <anime> all <state> !state <anime> upto <ep#> <state> !state
	 * <anime> last <state> !state <fid> <state> !state <ed2k link> <state> !state last <state>
	 *
	 * <p>
	 * !watched <anime> <epnumber> !watched <fid> !watched <ed2k link> !watched <anime> upto
	 * <epnumber> !watched <anime> all !watched <anime> none
	 */
	public String updateWatchedStatus(String arguments) {
		try {
			String apiParameters = "edit=1&viewed=";
			int ed2kIndex = arguments.indexOf("ed2k://|file|");
			if (ed2kIndex >= 0) {
				String[] fields = U.split(arguments.substring(13).trim(), '|');
				apiParameters += "&size=" + fields[1];
				apiParameters += "&ed2k=" + fields[2];
			} else {
				int episodeNumber = 0;
				ed2kIndex = arguments.indexOf(" none");
				if (ed2kIndex < 0) {
					ed2kIndex = arguments.indexOf(" all");
					apiParameters += "1";
					if (ed2kIndex < 0) {
						ed2kIndex = arguments.indexOf(" upto ");
						if (ed2kIndex > 0) {
							episodeNumber = -Integer.parseInt(arguments.substring(ed2kIndex + 6).trim());
						} else {
							ed2kIndex = arguments.lastIndexOf(' ');
							if (ed2kIndex < 0) {
								ed2kIndex = 0;
							}
							try {
								episodeNumber = Integer
										.parseInt(arguments.substring(ed2kIndex < 0 ? 0 : ed2kIndex).trim());
							} catch (NumberFormatException ex) {
								return getUsageMessage(USAGE_WATCHED);
							}
						}
					}
				} else {
					apiParameters += "0";
				}
				String animeIdentifier = arguments.substring(0, ed2kIndex).trim();
				if (animeIdentifier.isEmpty()) {
					if (episodeNumber < 1) {
						return getUsageMessage(USAGE_WATCHED);
					}
					apiParameters += "&fid=" + episodeNumber;
				} else {
					apiParameters += "&epno=" + episodeNumber;
					try {
						apiParameters += "&aid=" + Integer.parseInt(animeIdentifier);
					} catch (NumberFormatException ex) {
						apiParameters += "&aname=" + animeIdentifier;
					}
				}
			}
			AConR response = aniDbConnection.send("MYLISTADD", apiParameters, true);
			if (response.code == AConR.MYLIST_ENTRY_EDITED) {
				if (response.data.length() > 3) {
					return "WATCHED: entry updated.";
				}
				return "WATCHED: " + response.data + " entries updated.";
			}
			return "WATCHED: no such entry";
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return getUsageMessage(USAGE_WATCHED);
	}

	public String updateState(String arguments) {
		try {
			int lastSpaceIndex = arguments.lastIndexOf(' ');
			int stateCode = 0;
			String stateString = arguments.substring(lastSpaceIndex).trim();
			if (stateString.equalsIgnoreCase("unknown")) {
				stateCode = 0;
			} else if (stateString.equalsIgnoreCase("hdd")) {
				stateCode = 1;
			} else if (stateString.equalsIgnoreCase("cd")) {
				stateCode = 2;
			} else if (stateString.equalsIgnoreCase("deleted")) {
				stateCode = 3;
			} else {
				return getUsageMessage(USAGE_STATE);
			}
			String apiParameters = "edit=1&state=" + stateCode;
			int ed2kIndex = arguments.indexOf("ed2k://|file|");
			if (ed2kIndex >= 0) {
				String[] fields = U.split(arguments.substring(13, lastSpaceIndex).trim(), '|');
				apiParameters += "&size=" + fields[1];
				apiParameters += "&ed2k=" + fields[2];
			} else {
				int episodeNumber = 0;
				ed2kIndex = arguments.indexOf(" all ");
				if (ed2kIndex < 0) {
					ed2kIndex = arguments.indexOf(" upto ");
					if (ed2kIndex > 0) {
						episodeNumber = -Integer.parseInt(arguments.substring(ed2kIndex + 6, lastSpaceIndex).trim());
					} else {
						ed2kIndex = arguments.lastIndexOf(' ', lastSpaceIndex - 1);
						if (ed2kIndex < 0) {
							ed2kIndex = 0;
						}
						episodeNumber = Integer
								.parseInt(arguments.substring(ed2kIndex < 0 ? 0 : ed2kIndex, lastSpaceIndex).trim());
					}
				}
				String animeIdentifier = arguments.substring(0, ed2kIndex).trim();
				if (animeIdentifier.isEmpty()) {
					if (episodeNumber < 1) {
						return getUsageMessage(USAGE_STATE);
					}
					apiParameters += "&fid=" + episodeNumber;
				} else {
					apiParameters += "&epno=" + episodeNumber;
					try {
						apiParameters += "&aid=" + Integer.parseInt(animeIdentifier);
					} catch (NumberFormatException ex) {
						apiParameters += "&aname=" + animeIdentifier;
					}
				}
			}
			AConR response = aniDbConnection.send("MYLISTADD", apiParameters, true);
			if (response.code == AConR.MYLIST_ENTRY_EDITED) {
				if (response.data.length() > 3) {
					return "STATE: entry updated.";
				}
				return "STATE: " + response.data + " entries updated.";
			}
			return "STATE: no such entry";
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return getUsageMessage(USAGE_STATE);
	}

	protected static final int USAGE_WATCHED = 0;
	protected static final int USAGE_STATE = 1;
	protected static final int USAGE_STATE2 = 2;
	protected static final int USAGE_STORAGE = 3;

	protected static String getUsageMessage(int usageType) {
		switch (usageType) {
			case USAGE_WATCHED :
				return "WATCHED: usage: !watched <anime> <epnumber>, !state <fid>, !state <ed2k"
						+ " link>, epnumber may be 'all', 'upto <epno>' or 'none'.";
			case USAGE_STATE :
				return "STATE: usage: !state <anime> <epnumber> <state>, !state <fid> <state>,"
						+ " !state <ed2k link> <state>, !state last <state>, epnumber may be"
						+ " 'all' or 'upto <epno>'. State is: unknown/hdd/cd/deleted.";
			case USAGE_STATE2 :
				return "!state2 anime/aid, group/gid/all, all/upto x/x, unknown/hdd/cd/deleted";
			case USAGE_STORAGE :
				return "!storage anime/aid, group/gid/all, all/upto x/x, text";
			default :
				return "NOO!";
		}
	}
}
