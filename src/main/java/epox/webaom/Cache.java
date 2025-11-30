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

import epox.util.U;
import epox.webaom.data.AFile;
import epox.webaom.data.AG;
import epox.webaom.data.Anime;
import epox.webaom.data.Base;
import epox.webaom.data.Ep;
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
		return "Anime: " + cacheMaps[DB.INDEX_ANIME].size() + ", Episode: " + cacheMaps[DB.INDEX_EPISODE].size()
				+ ", Group: " + cacheMaps[DB.INDEX_ANIME].size();
	}

	public void clear() {
		for (int index = 0; index < cacheMaps.length; index++) {
			cacheMaps[index].clear();
		}
	}

	public void add(Base baseObject, int updateMode, int cacheType) {
		Integer id = Integer.valueOf(baseObject.id);
		if (!cacheMaps[cacheType].containsKey(id)) {
			cacheMaps[cacheType].put(id, baseObject);
			if (updateMode == 1) {
				A.db.update(baseObject.id, baseObject, cacheType);
			}
		}
		if (updateMode == 2) {
			A.db.update(baseObject.id, baseObject, cacheType);
		}
	}

	public Base get(int id, int cacheType) {
		Base baseObject = cacheMaps[cacheType].get(Integer.valueOf(id));
		if (baseObject == null) {
			baseObject = A.db.getGeneric(id, cacheType);
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
			file.anime = (Anime) get(file.aid, DB.INDEX_ANIME);
			if (file.gid != 0) {
				file.group = (Group) get(file.gid, DB.INDEX_GROUP);
			} else {
				file.group = Group.none;
			}
			file.ep = (Ep) get(file.eid, DB.INDEX_EPISODE);
			if (addToTree) {
				treeAdd(job);
			}
			return null;
		} catch (Exception ex) {
			U.err(file);
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
		file.anime = (Anime) cacheMaps[DB.INDEX_ANIME].get(Integer.valueOf(file.aid));
		if (file.anime == null) {
			file.anime = new Anime(file.aid);
		} else {
			A.p.remove(file.anime);
		}

		file.ep = (Ep) cacheMaps[DB.INDEX_EPISODE].get(Integer.valueOf(file.eid));
		if (file.ep == null) {
			file.ep = new Ep(file.eid);
		}

		file.group = (Group) cacheMaps[DB.INDEX_GROUP].get(Integer.valueOf(file.gid));
		if (file.group == null) {
			file.group = new Group(file.gid);
		}

		// set group data
		file.group.name = fields[fieldIndex++];
		file.group.sname = fields[fieldIndex++];
		// set episode data
		file.ep.num = fields[fieldIndex++];
		file.ep.eng = fields[fieldIndex++];
		file.ep.rom = U.n(fields[fieldIndex++]);
		file.ep.kan = U.n(fields[fieldIndex++]);
		// set anime data
		file.anime.eps = Integer.parseInt(fields[fieldIndex++]);
		file.anime.lep = Integer.parseInt(fields[fieldIndex++]);

		try {
			file.anime.yea = Integer.parseInt(fields[fieldIndex++].substring(0, 4));
		} catch (Exception ex) {
			file.anime.yea = 0;
		}
		try {
			file.anime.yen = Integer.parseInt(fields[fieldIndex - 1].substring(5, 9));
		} catch (Exception ex) {
			file.anime.yen = file.anime.yea;
		}
		file.anime.typ = fields[fieldIndex++];
		file.anime.rom = fields[fieldIndex++];
		file.anime.kan = U.n(fields[fieldIndex++]);
		file.anime.eng = U.n(fields[fieldIndex++]);
		file.anime.cat = fields[fieldIndex++];
		file.anime.init();
		// wrap up
		file.def = file.anime.rom + " - " + file.ep.num + " - " + file.ep.eng + " - ["
				+ ((file.gid > 0) ? file.group.sname : "RAW") + "]";
		file.pack();

		// update cache/db
		add(file.anime, 2, DB.INDEX_ANIME);
		add(file.ep, 2, DB.INDEX_EPISODE);
		add(file.group, 2, DB.INDEX_GROUP);
		A.db.update(file.fid, file, DB.INDEX_FILE);

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
		A.p.clear();
		Job[] jobs = A.jobs.array();
		for (int index = 0; index < jobs.length; index++) {
			treeAdd(jobs[index]);
		}
		U.out("@ Rebuilt tree in " + (System.currentTimeMillis() - startTime) + " ms.");
	}

	public void treeAdd(Job job) {
		boolean isMissingOrDeleted = job.checkOr(Job.H_MISSING | Job.H_DELETED);
		if (job.incompl() || (hideExisting && !isMissingOrDeleted) || (hideNew && isMissingOrDeleted)
				|| (job.hide(A.preg))) {
			return;
		}
		AFile file = job.anidbFile;
		Anime anime = file.anime;
		if (A.p.has(anime)) {
			A.p.remove(anime);
		}
		switch (treeSortMode) {
			case MODE_ANIME_FILE : {
				anime.add(file);
			}
				break;
			case MODE_ANIME_EPISODE_FILE : {
				if (anime.has(file.ep)) {
					anime.remove(file.ep);
				}
				if (file.ep.has(file)) {
					file.ep.remove(file);
				}
				file.ep.add(file);
				anime.add(file.ep);
			}
				break;
			case MODE_ANIME_GROUP_FILE : {
				if (file.gid < 1) {
					file.group = Group.none;
				}
				Base groupNode = anime.get(file.group.getKey());
				if (groupNode != null) {
					anime.remove(groupNode);
				} else {
					groupNode = new AG(anime, file.group);
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
		anime.regEp(file.ep, true);
		anime.updatePct();
		A.p.add(anime);
	}

	public void treeRemove(Job job) {
		if (job.incompl()) {
			return;
		}
		AFile file = job.anidbFile;
		Anime anime = file.anime;
		if (A.p.has(anime)) {
			A.p.remove(anime);
		}
		switch (treeSortMode) {
			case MODE_ANIME_FILE : {
				anime.remove(file);
			}
				break;
			case MODE_ANIME_EPISODE_FILE : {
				anime.remove(file.ep);
				file.ep.remove(file);
				if (file.ep.size() > 0) {
					anime.add(file.ep);
				}
			}
				break;
			case MODE_ANIME_GROUP_FILE : {
				if (file.gid < 1) {
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
		anime.regEp(file.ep, file.ep.size() > 0);
		anime.updatePct();
		if (anime.size() > 0) {
			A.p.add(anime);
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
