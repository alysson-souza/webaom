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

package epox.webaom;

import epox.util.StringUtilities;
import epox.webaom.data.AniDBEntity;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.AnimeGroup;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.data.Path;
import epox.webaom.db.DatabaseManager;
import java.util.HashMap;
import java.util.logging.Logger;

public class Cache {
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

    private static final Logger LOGGER = Logger.getLogger(Cache.class.getName());
    /** Display labels for each sort mode */
    private static final String[] SORT_MODE_LABELS = {
        "Anime, File", "Anime, Episode, File", "Anime, Group, File", "Anime, Folder, File"
    };
    /** Current tree sort mode */
    private static int treeSortMode = MODE_ANIME_EPISODE_FILE;

    // Configuration flags for tree display
    /** Whether to hide existing (non-missing) files from tree */
    private static boolean hideExisting = false;
    /** Whether to hide new/missing files from tree */
    private static boolean hideNew = false;

    private final CacheMap[] cacheMaps;

    public Cache() {
        cacheMaps = new CacheMap[3];
        for (int index = 0; index < cacheMaps.length; index++) {
            cacheMaps[index] = new CacheMap();
        }
    }

    public static String[] getSortModeLabels() {
        return SORT_MODE_LABELS.clone();
    }

    public static int getTreeSortMode() {
        return treeSortMode;
    }

    public static void setTreeSortMode(int mode) {
        treeSortMode = mode;
    }

    public static boolean isHideExisting() {
        return hideExisting;
    }

    public static void setHideExisting(boolean hide) {
        hideExisting = hide;
    }

    public static boolean isHideNew() {
        return hideNew;
    }

    public static void setHideNew(boolean hide) {
        hideNew = hide;
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

    public void add(AniDBEntity baseObject, int updateMode, int cacheType) {
        Integer id = baseObject.getId();
        if (!cacheMaps[cacheType].containsKey(id)) {
            cacheMaps[cacheType].put(id, baseObject);
            if (updateMode == 1) {
                AppContext.databaseManager.update(baseObject.getId(), baseObject, cacheType);
            }
        }
        if (updateMode == 2) {
            AppContext.databaseManager.update(baseObject.getId(), baseObject, cacheType);
        }
    }

    public AniDBEntity get(int id, int cacheType) {
        AniDBEntity baseObject = cacheMaps[cacheType].get(id);
        if (baseObject == null) {
            baseObject = AppContext.databaseManager.getGeneric(id, cacheType);
            if (baseObject != null) {
                cacheMaps[cacheType].put(id, baseObject);
            }
        }
        return baseObject;
    }

    /** Gathers related info (anime, group, episode) for a job's file. */
    public synchronized void gatherInfo(Job job, boolean addToTree) {
        if (job == null || job.anidbFile == null) {
            return;
        }
        AniDBFile file = job.anidbFile;
        try {
            file.setAnime((Anime) get(file.getAnimeId(), DatabaseManager.INDEX_ANIME));
            if (file.getGroupId() != 0) {
                file.setGroup((Group) get(file.getGroupId(), DatabaseManager.INDEX_GROUP));
            } else {
                file.setGroup(Group.NONE);
            }
            file.setEpisode((Episode) get(file.getEpisodeId(), DatabaseManager.INDEX_EPISODE));
            if (addToTree) {
                treeAdd(job);
            }
        } catch (Exception ex) {
            StringUtilities.err(file);
            ex.printStackTrace();
        }
    }

    public AniDBFile parseFile(String[] fields, Job job) {
        if (fields.length != 34) {
            LOGGER.warning(() -> "Unexpected response! len=" + fields.length);
            job.setError("Unexpected response from server.");
            return null;
        }
        AniDBFile file = new AniDBFile(fields);
        int fieldIndex = 20;
        // create/retrieve data objects
        Anime anime = (Anime) cacheMaps[DatabaseManager.INDEX_ANIME].get(file.getAnimeId());
        if (anime == null) {
            anime = new Anime(file.getAnimeId());
        } else {
            AppContext.animeTreeRoot.remove(anime);
        }
        file.setAnime(anime);

        Episode episode = (Episode) cacheMaps[DatabaseManager.INDEX_EPISODE].get(file.getEpisodeId());
        if (episode == null) {
            episode = new Episode(file.getEpisodeId());
        }
        file.setEpisode(episode);

        Group group = (Group) cacheMaps[DatabaseManager.INDEX_GROUP].get(file.getGroupId());
        if (group == null) {
            group = new Group(file.getGroupId());
        }
        file.setGroup(group);

        // set group data
        file.getGroup().name = fields[fieldIndex++];
        file.getGroup().shortName = fields[fieldIndex++];
        // set episode data
        file.getEpisode().num = fields[fieldIndex++];
        file.getEpisode().eng = fields[fieldIndex++];
        file.getEpisode().rom = StringUtilities.n(fields[fieldIndex++]);
        file.getEpisode().kan = StringUtilities.n(fields[fieldIndex++]);
        // set anime data
        file.getAnime().episodeCount = Integer.parseInt(fields[fieldIndex++]);
        file.getAnime().latestEpisode = Integer.parseInt(fields[fieldIndex++]);

        try {
            file.getAnime().year = Integer.parseInt(fields[fieldIndex++].substring(0, 4));
        } catch (Exception ex) {
            file.getAnime().year = 0;
        }
        try {
            file.getAnime().endYear = Integer.parseInt(fields[fieldIndex - 1].substring(5, 9));
        } catch (Exception ex) {
            file.getAnime().endYear = file.getAnime().year;
        }
        file.getAnime().type = fields[fieldIndex++];
        file.getAnime().romajiTitle = fields[fieldIndex++];
        file.getAnime().kanjiTitle = StringUtilities.n(fields[fieldIndex++]);
        file.getAnime().englishTitle = StringUtilities.n(fields[fieldIndex++]);
        file.getAnime().categories = fields[fieldIndex];
        file.getAnime().init();
        // wrap up
        file.setDefaultName(file.getAnime().romajiTitle + " - " + file.getEpisode().num + " - " + file.getEpisode().eng
                + " - [" + ((file.getGroupId() > 0) ? file.getGroup().shortName : "RAW") + "]");
        file.pack();

        // update cache/db
        add(file.getAnime(), 2, DatabaseManager.INDEX_ANIME);
        add(file.getEpisode(), 2, DatabaseManager.INDEX_EPISODE);
        add(file.getGroup(), 2, DatabaseManager.INDEX_GROUP);
        AppContext.databaseManager.update(file.getFileId(), file, DatabaseManager.INDEX_FILE);

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
        AppContext.animeTreeRoot.clear();
        Job[] jobs = AppContext.jobs.array();
        for (Job job : jobs) {
            treeAdd(job);
        }
        StringUtilities.out("@ Rebuilt tree in " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    public void treeAdd(Job job) {
        boolean isMissingOrDeleted = job.checkOr(Job.H_MISSING | Job.H_DELETED);
        if (job.incompl()
                || (hideExisting && !isMissingOrDeleted)
                || (hideNew && isMissingOrDeleted)
                || (job.hide(AppContext.pathRegex))) {
            return;
        }
        AniDBFile file = job.anidbFile;
        Anime anime = file.getAnime();
        if (AppContext.animeTreeRoot.has(anime)) {
            AppContext.animeTreeRoot.remove(anime);
        }
        switch (treeSortMode) {
            case MODE_ANIME_FILE:
                {
                    anime.add(file);
                }
                break;
            case MODE_ANIME_EPISODE_FILE:
                {
                    if (anime.has(file.getEpisode())) {
                        anime.remove(file.getEpisode());
                    }
                    if (file.getEpisode().has(file)) {
                        file.getEpisode().remove(file);
                    }
                    file.getEpisode().add(file);
                    anime.add(file.getEpisode());
                }
                break;
            case MODE_ANIME_GROUP_FILE:
                {
                    if (file.getGroupId() < 1) {
                        file.setGroup(Group.NONE);
                    }
                    AniDBEntity groupNode = anime.get(file.getGroup().getKey());
                    if (groupNode != null) {
                        anime.remove(groupNode);
                    } else {
                        groupNode = new AnimeGroup(anime, file.getGroup());
                    }
                    if (groupNode.has(file)) {
                        groupNode.remove(file);
                    }
                    groupNode.add(file);
                    anime.add(groupNode);
                }
                break;
            case MODE_ANIME_FOLDER_FILE:
                {
                    String parentPath = job.getFile().getParent();
                    AniDBEntity folderNode = anime.get(parentPath);
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
            default:
                break;
        }
        anime.registerEpisode(file.getEpisode(), true);
        anime.updateCompletionPercent();
        AppContext.animeTreeRoot.add(anime);
    }

    public void treeRemove(Job job) {
        if (job.incompl()) {
            return;
        }
        AniDBFile file = job.anidbFile;
        Anime anime = file.getAnime();
        if (AppContext.animeTreeRoot.has(anime)) {
            AppContext.animeTreeRoot.remove(anime);
        }
        switch (treeSortMode) {
            case MODE_ANIME_FILE:
                {
                    anime.remove(file);
                }
                break;
            case MODE_ANIME_EPISODE_FILE:
                {
                    anime.remove(file.getEpisode());
                    file.getEpisode().remove(file);
                    if (file.getEpisode().size() > 0) {
                        anime.add(file.getEpisode());
                    }
                }
                break;
            case MODE_ANIME_GROUP_FILE:
                {
                    if (file.getGroupId() < 1) {
                        file.setGroup(Group.NONE);
                    }
                    AniDBEntity groupNode = anime.get(file.getGroup().getKey());
                    if (groupNode != null) {
                        anime.remove(groupNode);
                        groupNode.remove(file);
                        if (groupNode.size() > 0) {
                            anime.add(groupNode);
                        }
                    }
                }
                break;
            case MODE_ANIME_FOLDER_FILE:
                {
                    String parentPath = job.getFile().getParent();
                    AniDBEntity folderNode = anime.get(parentPath);
                    if (folderNode != null) {
                        anime.remove(folderNode);
                        folderNode.remove(file);
                        if (folderNode.size() > 0) {
                            anime.add(folderNode);
                        }
                    }
                }
                break;
            default:
                break;
        }
        anime.registerEpisode(file.getEpisode(), file.getEpisode().size() > 0);
        anime.updateCompletionPercent();
        if (anime.size() > 0) {
            AppContext.animeTreeRoot.add(anime);
        }
    }

    /** Internal HashMap for caching AniDBEntity objects by ID. */
    protected class CacheMap extends HashMap<Integer, AniDBEntity> {
        //
    }
}
