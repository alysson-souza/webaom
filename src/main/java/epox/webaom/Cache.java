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
 * Created on 30.07.05
 *
 * @version 	1.09
 * @author 		epoximator
 */

package epox.webaom;

import epox.util.StringUtilities;
import epox.webaom.data.AFile;
import epox.webaom.data.AnimeGroup;
import epox.webaom.data.Anime;
import epox.webaom.data.Base;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.data.Path;
import java.util.HashMap;

public class Cache {
	/** Internal HashMap for caching Base objects by ID. */
	protected class CacheMap extends HashMap<Integer, Base> {
		//
	}

	private final CacheMap[] cacheMaps;

	public Cache() {
		cacheMaps = new CacheMap[3];
		for (int index = 0; index < cacheMaps.length; index++) {
			cacheMaps[index] = new CacheMap();
		}
	}

	public String toString() {
		return "Anime: " + cacheMaps[DatabaseManager.INDEX_ANIME].size() + ", Episode: "
				+ cacheMaps[DatabaseManager.INDEX_EPISODE].size() + ", Group: "
				+ cacheMaps[DatabaseManager.INDEX_ANIME].size();
	}

	public void clear() {
		for (CacheMap cacheMap : cacheMaps) {
			cacheMap.clear();
		}
	}

	public void add(Base baseObject, int updateMode, int cacheType) {
		Integer id = Integer.valueOf(baseObject.id);
		if (!cacheMaps[cacheType].containsKey(id)) {
			cacheMaps[cacheType].put(id, baseObject);
			if (updateMode == 1) {
				AppContext.databaseManager.update(baseObject.id, baseObject, cacheType);
			}
		}
		if (updateMode == 2) {
			AppContext.databaseManager.update(baseObject.id, baseObject, cacheType);
		}
	}

	public Base get(int id, int cacheType) {
		Base baseObject = cacheMaps[cacheType].get(Integer.valueOf(id));
		if (baseObject == null) {
			baseObject = AppContext.databaseManager.getGeneric(id, cacheType);
			if (baseObject != null) {
				cacheMaps[cacheType].put(Integer.valueOf(id), baseObject);
			}
		}
		return baseObject;
	}

	/////////////// OTHER///////////////
	/** Gathers related info (anime, group, episode) for a job's file. */
	public synchronized String gatherInfo(Job job, boolean addToTree) {
		if (job == null || job.anidbFile == null) {
			return null;
		}
		AFile file = job.anidbFile;
		try {
			file.anime = (Anime) get(file.animeId, DatabaseManager.INDEX_ANIME);
			if (file.groupId != 0) {
				file.group = (Group) get(file.groupId, DatabaseManager.INDEX_GROUP);
			} else {
				file.group = Group.none;
			}
			file.episode = (Episode) get(file.episodeId, DatabaseManager.INDEX_EPISODE);
			if (addToTree) {
				treeAdd(job);
			}
			return null;
		} catch (Exception ex) {
			StringUtilities.err(file);
			ex.printStackTrace();
			return ex.getMessage();
		}
	}

	public AFile parseFile(String[] fields, Job job) {
		if (fields.length != 34) {
			System.out.println("Unexpected response! len=" + fields.length);
			job.setError("Unexpected response from server.");
			return null;
		}
		AFile file = new AFile(fields);
		int fieldIndex = 20;
		// create/retrieve data objects
		file.anime = (Anime) cacheMaps[DatabaseManager.INDEX_ANIME].get(Integer.valueOf(file.animeId));
		if (file.anime == null) {
			file.anime = new Anime(file.animeId);
		} else {
			AppContext.p.remove(file.anime);
		}

		file.episode = (Episode) cacheMaps[DatabaseManager.INDEX_EPISODE].get(Integer.valueOf(file.episodeId));
		if (file.episode == null) {
			file.episode = new Episode(file.episodeId);
		}

		file.group = (Group) cacheMaps[DatabaseManager.INDEX_GROUP].get(Integer.valueOf(file.groupId));
		if (file.group == null) {
			file.group = new Group(file.groupId);
		}

		// set group data
		file.group.name = fields[fieldIndex++];
		file.group.shortName = fields[fieldIndex++];
		// set episode data
		file.episode.num = fields[fieldIndex++];
		file.episode.eng = fields[fieldIndex++];
		file.episode.rom = StringUtilities.n(fields[fieldIndex++]);
		file.episode.kan = StringUtilities.n(fields[fieldIndex++]);
		// set anime data
		file.anime.episodeCount = Integer.parseInt(fields[fieldIndex++]);
		file.anime.latestEpisode = Integer.parseInt(fields[fieldIndex++]);

		try {
			file.anime.year = Integer.parseInt(fields[fieldIndex++].substring(0, 4));
		} catch (Exception ex) {
			file.anime.year = 0;
		}
		try {
			file.anime.endYear = Integer.parseInt(fields[fieldIndex - 1].substring(5, 9));
		} catch (Exception ex) {
			file.anime.endYear = file.anime.year;
		}
		file.anime.type = fields[fieldIndex++];
		file.anime.romajiTitle = fields[fieldIndex++];
		file.anime.kanjiTitle = StringUtilities.n(fields[fieldIndex++]);
		file.anime.englishTitle = StringUtilities.n(fields[fieldIndex++]);
		file.anime.categories = fields[fieldIndex++];
		file.anime.init();
		// wrap up
		file.defaultName = file.anime.romajiTitle + " - " + file.episode.num + " - " + file.episode.eng + " - ["
				+ ((file.groupId > 0) ? file.group.shortName : "RAW") + "]";
		file.pack();

		// update cache/db
		add(file.anime, 2, DatabaseManager.INDEX_ANIME);
		add(file.episode, 2, DatabaseManager.INDEX_EPISODE);
		add(file.group, 2, DatabaseManager.INDEX_GROUP);
		AppContext.databaseManager.update(file.fileId, file, DatabaseManager.INDEX_FILE);

		// update data tree
		job.anidbFile = file;
		treeAdd(job);
		return file;
	}

	public String stats() {
		return cacheMaps[0].size() + "," + cacheMaps[1].size() + "," + cacheMaps[2].size();
	}

	public void rebuildTree() {
		long startTime = System.currentTimeMillis();
		AppContext.p.clear();
		Job[] jobs = AppContext.jobs.array();
		for (Job job : jobs) {
			treeAdd(job);
		}
		StringUtilities.out("@ Rebuilt tree in " + (System.currentTimeMillis() - startTime) + " ms.");
	}

	public void treeAdd(Job job) {
		boolean isMissingOrDeleted = job.checkOr(Job.H_MISSING | Job.H_DELETED);
		if (job.incompl() || (hideExisting && !isMissingOrDeleted) || (hideNew && isMissingOrDeleted)
				|| (job.hide(AppContext.preg))) {
			return;
		}
		AFile file = job.anidbFile;
		Anime anime = file.anime;
		if (AppContext.p.has(anime)) {
			AppContext.p.remove(anime);
		}
		switch (treeSortMode) {
			case MODE_ANIME_FILE : {
				anime.add(file);
			}
				break;
			case MODE_ANIME_EPISODE_FILE : {
				if (anime.has(file.episode)) {
					anime.remove(file.episode);
				}
				if (file.episode.has(file)) {
					file.episode.remove(file);
				}
				file.episode.add(file);
				anime.add(file.episode);
			}
				break;
			case MODE_ANIME_GROUP_FILE : {
				if (file.groupId < 1) {
					file.group = Group.none;
				}
				Base groupNode = anime.get(file.group.getKey());
				if (groupNode != null) {
					anime.remove(groupNode);
				} else {
					groupNode = new AnimeGroup(anime, file.group);
				}
				if (groupNode.has(file)) {
					groupNode.remove(file);
				}
				groupNode.add(file);
				anime.add(groupNode);
			}
				break;
			case MODE_ANIME_FOLDER_FILE : {
				String parentPath = job.getFile().getParent();
				Base folderNode = anime.get(parentPath);
				if (folderNode != null) {
					anime.remove(folderNode);
				} else {
					folderNode = new Path(parentPath);
				}
				if (folderNode.has(file)) {
					folderNode.remove(file);
				}
				folderNode.add(file);
				anime.add(folderNode);
			}
				break;
		}
		anime.registerEpisode(file.episode, true);
		anime.updateCompletionPercent();
		AppContext.p.add(anime);
	}

	public void treeRemove(Job job) {
		if (job.incompl()) {
			return;
		}
		AFile file = job.anidbFile;
		Anime anime = file.anime;
		if (AppContext.p.has(anime)) {
			AppContext.p.remove(anime);
		}
		switch (treeSortMode) {
			case MODE_ANIME_FILE : {
				anime.remove(file);
			}
				break;
			case MODE_ANIME_EPISODE_FILE : {
				anime.remove(file.episode);
				file.episode.remove(file);
				if (file.episode.size() > 0) {
					anime.add(file.episode);
				}
			}
				break;
			case MODE_ANIME_GROUP_FILE : {
				if (file.groupId < 1) {
					file.group = Group.none;
				}
				Base groupNode = anime.get(file.group.getKey());
				if (groupNode != null) {
					anime.remove(groupNode);
					groupNode.remove(file);
					if (groupNode.size() > 0) {
						anime.add(groupNode);
					}
				}
			}
				break;
			case MODE_ANIME_FOLDER_FILE : {
				String parentPath = job.getFile().getParent();
				Base folderNode = anime.get(parentPath);
				if (folderNode != null) {
					anime.remove(folderNode);
					folderNode.remove(file);
					if (folderNode.size() > 0) {
						anime.add(folderNode);
					}
				}
			}
				break;
		}
		anime.registerEpisode(file.episode, file.episode.size() > 0);
		anime.updateCompletionPercent();
		if (anime.size() > 0) {
			AppContext.p.add(anime);
		}
	}

	/** Tree sort mode: Anime > File */
	public static final int MODE_ANIME_FILE = 0;
	/** Tree sort mode: Anime > Episode > File */
	public static final int MODE_ANIME_EPISODE_FILE = 1;
	/** Tree sort mode: Anime > Group > File */
	public static final int MODE_ANIME_GROUP_FILE = 2;
	/** Tree sort mode: Anime > Folder > File */
	public static final int MODE_ANIME_FOLDER_FILE = 3;
	/** Total number of tree sort modes */
	public static final int MODE_COUNT = 4;
	/** Display labels for each sort mode */
	public static final String[] SORT_MODE_LABELS = {"Anime, File", "Anime, Episode, File", "Anime, Group, File",
			"Anime, Folder, File"};
	/** Current tree sort mode */
	public static int treeSortMode = MODE_ANIME_EPISODE_FILE;
	/** Whether to hide existing (non-missing) files from tree */
	public static boolean hideExisting = false;
	/** Whether to hide new/missing files from tree */
	public static boolean hideNew = false;
}
