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

package epox.webaom.db;

import epox.av.FileInfo;
import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.data.AniDBEntity;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for database operations. Subclasses implement database-specific behavior.
 */
public abstract class DatabaseManager {

    /** Cache index for Anime objects. */
    public static final int INDEX_ANIME = 0;
    /** Cache index for Episode objects. */
    public static final int INDEX_EPISODE = 1;
    /** Cache index for Group objects. */
    public static final int INDEX_GROUP = 2;
    /** Cache index for AFile objects. */
    public static final int INDEX_FILE = 3;
    /** Cache index for Job objects. */
    public static final int INDEX_JOB = 4;
    /** Total count of cache indices (array size). */
    public static final int INDEX_COUNT = 5;

    /** SQL query for loading jobs with file, episode, and directory data. */
    protected static final String SQL_JOB_QUERY = "select d.name,j.name,j.status,j.orig,j.ed2k,j.md5,j.sha1,j.tth,"
            + "j.crc32,j.size,j.did,j.uid,j.lid,j.avxml,f.fid,f.aid,f.eid,f.gid,f.def_name,f.state,f.size,"
            + "f.ed2k,f.md5,f.sha1,f.crc32,f.dublang,f.sublang,f.quality,f.ripsource,f.audio,f.video,"
            + "f.resolution,f.ext,f.len,e.eid,e.number,e.english,e.romaji,e.kanji "
            + "from dtb d,jtb j,ftb f,etb e where d.did=j.did and j.fid=f.fid and f.eid=e.eid";

    protected final Map<String, Integer> directoryIdCache = new HashMap<>();

    protected Connection connection;
    protected Statement statement;
    protected String connectionUrl;
    protected String username;
    protected String password;

    protected PreparedStatement[] updateStatements;
    protected PreparedStatement[] insertStatements;

    protected boolean isConnectionReady = false;
    protected boolean isInitialized = false;
    protected boolean loadAllJobs = false;
    protected final boolean shouldCleanDatabase = false;
    protected boolean debug = false;

    // ========== Debug control ==========

    public void setDebug(boolean enabled) {
        this.debug = enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setLoadAllJobs(boolean loadAll) {
        this.loadAllJobs = loadAll;
    }

    public boolean isLoadAllJobs() {
        return loadAllJobs;
    }

    // ========== Abstract methods for database-specific behavior ==========

    /**
     * Returns all SQL files for this database type.
     * Index 0 = schema file (db00*.sql) for new database creation.
     * Index 1-6 = migration files to upgrade from version (index-1) to version index.
     * Example: index 1 = v0→v1, index 5 = v4→v5, index 6 = v5→v6.
     */
    protected abstract String[] getSqlFiles();

    /** Creates a Statement appropriate for this database type. */
    protected abstract Statement createStatement() throws SQLException;

    /** Gets the last inserted auto-increment ID. */
    protected abstract int getLastInsertId() throws SQLException;

    /** Escapes a string value for use in SQL queries. */
    protected abstract String escapeString(String value);

    /** Quotes a string value for use in SQL, handling null. */
    public abstract String quoteString(String value);

    /** Opens a connection to the database. */
    protected abstract Connection openConnection() throws SQLException;

    // ========== Common initialization ==========

    public boolean initialize(String databaseString) {
        if (isInitialized) {
            return false;
        }
        try {
            parseConnectionString(databaseString);

            if (!connect()) {
                return false;
            }
            if (!updateSchema()) {
                shutdown();
                return false;
            }
            if (!prepareStatements()) {
                shutdown();
                return false;
            }
            if (shouldCleanDatabase) {
                cleanDatabase();
            }
            return true;
        } catch (Exception e) {
            log("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            AppContext.dialog("Database:", e.toString());
        }
        return false;
    }

    protected void parseConnectionString(String databaseString) {
        String lowercaseDbString = databaseString.toLowerCase();
        int passwordIndex = lowercaseDbString.indexOf("&password=");
        if (passwordIndex < 0) {
            passwordIndex = databaseString.length();
        } else {
            password = databaseString.substring(passwordIndex + 10);
        }
        int userIndex = lowercaseDbString.indexOf("?user=");
        if (userIndex < 0) {
            userIndex = passwordIndex;
        } else {
            username = databaseString.substring(userIndex + 6, passwordIndex);
        }
        connectionUrl = databaseString.substring(0, userIndex);
    }

    protected boolean connect() {
        isConnectionReady = false;
        closeConnection();

        try {
            log("Connecting to database: " + connectionUrl);
            connection = openConnection();
            statement = createStatement();
            log("Database connection established successfully");
            isConnectionReady = true;
            return true;
        } catch (SQLException ex) {
            log("Database connection failed: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    protected void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                // ignore
            }
        }
    }

    protected boolean prepareStatements() {
        try {
            log("Preparing SQL statements...");
            updateStatements = new PreparedStatement[INDEX_COUNT];
            updateStatements[INDEX_ANIME] = connection.prepareStatement(
                    "update atb set romaji=?,kanji=?,english=?,year=?,episodes=?,last_ep=?,type=?,genre=?,img=0 where"
                            + " aid=?");
            updateStatements[INDEX_EPISODE] =
                    connection.prepareStatement("update etb set english=?,kanji=?,romaji=?,number=? where eid=?");
            updateStatements[INDEX_FILE] = connection.prepareStatement("update ftb set"
                + " aid=?,eid=?,gid=?,def_name=?,state=?,size=?,len=?,ed2k=?,md5=?,sha1=?,crc32=?,dublang=?,sublang=?,quality=?,ripsource=?,audio=?,video=?,resolution=?,ext=?"
                + " where fid=?");
            updateStatements[INDEX_GROUP] = connection.prepareStatement("update gtb set name=?,short=? where gid=?");
            updateStatements[INDEX_JOB] = connection.prepareStatement(
                    "update jtb set name=?,did=?,status=?,md5=?,sha1=?,tth=?,crc32=?,fid=?,lid=?,avxml=? where size=?"
                            + " and ed2k=?");

            insertStatements = new PreparedStatement[INDEX_COUNT];
            insertStatements[INDEX_ANIME] = connection.prepareStatement(
                    "insert into atb (romaji,kanji,english,year,episodes,last_ep,type,genre,img,aid) values"
                            + " (?,?,?,?,?,?,?,?,0,?)");
            insertStatements[INDEX_EPISODE] =
                    connection.prepareStatement("insert into etb (english,kanji,romaji,number,eid) values (?,?,?,?,?)");
            insertStatements[INDEX_FILE] = connection.prepareStatement("insert into ftb"
                + " (aid,eid,gid,def_name,state,size,len,ed2k,md5,sha1,crc32,dublang,sublang,quality,ripsource,audio,video,resolution,ext,fid)"
                + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            insertStatements[INDEX_GROUP] =
                    connection.prepareStatement("insert into gtb (name,short,gid) values (?,?,?)");
            insertStatements[INDEX_JOB] = connection.prepareStatement(
                    "insert into jtb (name,did,status,md5,sha1,tth,crc32,fid,lid,avxml,size,ed2k,orig,uid) values"
                            + " (?,?,?,?,?,?,?,?,?,?,?,?,?,1)");

            isInitialized = true;
            log("SQL statements prepared successfully");
            return true;
        } catch (SQLException ex) {
            log("Failed to prepare SQL statements: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    // ========== Schema management ==========

    protected boolean updateSchema() {
        log("Checking database schema...");
        try {
            // Check for ancient legacy schema (mylist table from ~2005)
            ResultSet rs = query("select state from mylist where lid=0", true);
            if (rs != null && rs.next()) {
                log("Detected legacy database schema (mylist table) - too old to migrate");
                rs.close();
                return false;
            }
            if (rs != null) rs.close();

            // Check current schema version
            rs = query("select ver from vtb", true);
            String[] sqlFiles = getSqlFiles();

            if (rs != null && rs.next()) {
                int version = rs.getInt(1);
                rs.close();
                log("Detected existing database with schema version " + version);

                if (version < 4
                        && !AppContext.confirm(
                                "Warning",
                        """
                                The database definition has to be upgraded.
                                This will make it incompatible with previous versions of WebAOM.
                                Do you want to continue? (Backup now, if needed.)""",
                                "Yes",
                                "No")) {
                    return false;
                }

                // Apply migrations from current version to latest
                for (int v = version + 1; v < sqlFiles.length; v++) {
                    log("Applying migration: " + sqlFiles[v]);
                    executeStatements(AppContext.getFileString(sqlFiles[v]));
                }
            } else {
                if (rs != null) rs.close();
                // New database - use schema file at index 0
                log("No existing database found, creating new schema...");
                log("Using schema file: " + sqlFiles[0]);
                executeStatements(AppContext.getFileString(sqlFiles[0]));
                log("Database schema created successfully");
            }
        } catch (Exception ex) {
            log("Schema update failed: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        log("Database schema is up to date");
        return true;
    }

    protected void executeStatements(String sqlBatch) {
        String[] statements = sqlBatch.split(";");
        for (String sql : statements) {
            sql = sql.trim();
            if (!sql.isEmpty()) {
                exec(sql, true);
            }
        }
    }

    // ========== Core SQL operations ==========

    protected boolean exec(String command, boolean silent) {
        if (!isConnectionReady) {
            return false;
        }
        try {
            if (!silent) {
                log("} " + command);
            }
            statement.execute(command);
            return true;
        } catch (SQLException ex) {
            if (!silent) {
                log("! DB Error: " + ex.getMessage());
                ex.printStackTrace();
            }
            return false;
        }
    }

    protected ResultSet query(String command, boolean silent) {
        if (!isConnectionReady) {
            return null;
        }
        try {
            if (!silent) {
                log("} " + command);
            }
            return statement.executeQuery(command);
        } catch (SQLException ex) {
            if (!silent) {
                log("! DB Error: " + ex.getMessage());
            }
            return null;
        }
    }

    // ========== Directory ID management ==========

    public int getDirectoryId(String path) {
        if (!isInitialized) {
            return -1;
        }
        if (path == null) {
            path = "";
        }
        try {
            String escapedPath = escapeString(path);
            Integer cachedId = directoryIdCache.get(path);
            if (cachedId != null) {
                return cachedId;
            }

            ResultSet rs = query("select did from dtb where name='" + escapedPath + "'", false);
            if (rs != null && rs.next()) {
                int directoryId = rs.getInt(1);
                rs.close();
                directoryIdCache.put(path, directoryId);
                return directoryId;
            }
            if (rs != null) rs.close();

            // Insert new directory
            log("} insert into dtb (name) values ('" + escapedPath + "')");
            statement.executeUpdate("insert into dtb (name) values ('" + escapedPath + "')");
            int directoryId = getLastInsertId();
            if (directoryId > 0) {
                directoryIdCache.put(path, directoryId);
            }
            return directoryId;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    // ========== Entity operations ==========

    public synchronized AniDBEntity getGeneric(int entityId, int entityType) {
        if (!isInitialized) {
            return null;
        }
        try {
            if (entityType == INDEX_EPISODE) {
                ResultSet rs = query("select english,kanji,romaji,number from etb where eid=" + entityId, false);
                if (rs != null && rs.next()) {
                    Episode episode = new Episode(entityId);
                    episode.eng = rs.getString(1);
                    episode.kan = rs.getString(2);
                    episode.rom = rs.getString(3);
                    episode.num = rs.getString(4).intern();
                    rs.close();
                    log("{ " + episode);
                    return episode;
                }
                if (rs != null) rs.close();
            } else if (entityType == INDEX_GROUP) {
                ResultSet rs = query("select name,short from gtb where gid=" + entityId, false);
                if (rs != null && rs.next()) {
                    Group group = new Group(entityId);
                    group.name = rs.getString(1);
                    group.shortName = rs.getString(2);
                    rs.close();
                    log("{ " + group);
                    return group;
                }
                if (rs != null) rs.close();
            } else if (entityType == INDEX_ANIME) {
                ResultSet rs = query(
                        "select episodes,last_ep,year,type,romaji,kanji,english,genre,img from atb where aid="
                                + entityId,
                        false);
                if (rs != null && rs.next()) {
                    String[] fields = new String[9];
                    fields[0] = String.valueOf(entityId);
                    for (int i = 1; i < 9; i++) {
                        fields[i] = rs.getString(i);
                    }
                    rs.close();
                    Anime anime = new Anime(fields);
                    log("{ " + anime);
                    return anime;
                }
                if (rs != null) rs.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public synchronized int getJob(Job job, boolean useEd2k) {
        if (!isInitialized) {
            return -1;
        }
        try {
            ResultSet rs;
            if (useEd2k) {
                rs = query(
                        SQL_JOB_QUERY + " and j.size=" + job.currentFile.length() + " and j.ed2k="
                                + quoteString(job.ed2kHash),
                        false);
            } else {
                int directoryId = getDirectoryId(job.currentFile.getParent());
                rs = query(
                        SQL_JOB_QUERY + " and j.size=" + job.currentFile.length() + " and j.name="
                                + quoteString(job.currentFile.getName()) + " and j.did=" + directoryId,
                        false);
            }
            if (rs != null && rs.next()) {
                int colIndex = 1;
                job.targetFile = new File(rs.getString(colIndex++) + File.separatorChar + rs.getString(colIndex++));
                if (job.currentFile.equals(job.targetFile)) {
                    job.targetFile = null;
                }
                int status = rs.getInt(colIndex++);
                populateJobFromResultSet(rs, colIndex, job);
                rs.close();
                job.isFresh = false;
                log("{ Job found: " + job + ":" + status);
                return status;
            }
            if (rs != null) rs.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public synchronized void getJobs() {
        if (!isInitialized) {
            log("getJobs() called but database not initialized");
            return;
        }
        try {
            log("Loading jobs from database (loadAllJobs=" + loadAllJobs + ")...");
            ResultSet rs;
            if (loadAllJobs) {
                rs = query(SQL_JOB_QUERY + " ORDER BY j.time", false);
            } else {
                rs = query(SQL_JOB_QUERY + " and j.status!=" + Job.FINISHED + " ORDER BY j.time", false);
            }

            if (rs == null) {
                log("Job query returned null ResultSet");
                return;
            }

            int jobCount = 0;
            while (rs.next()) {
                int colIndex = 1;
                File file = new File(rs.getString(colIndex++) + File.separatorChar + rs.getString(colIndex++));
                Job job = new Job(file, rs.getInt(colIndex++));
                populateJobFromResultSet(rs, colIndex, job);
                if (!AppContext.jobs.add(job)) {
                    StringUtilities.err("DB: Dupe: " + job);
                }
                jobCount++;
            }
            rs.close();
            log("Loaded " + jobCount + " jobs from database");
        } catch (SQLException ex) {
            log("Error loading jobs: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void populateJobFromResultSet(ResultSet rs, int colIndex, Job job) throws SQLException {
        job.originalName = rs.getString(colIndex++);
        job.ed2kHash = rs.getString(colIndex++);
        job.md5Hash = rs.getString(colIndex++);
        job.sha1Hash = rs.getString(colIndex++);
        job.tthHash = rs.getString(colIndex++);
        job.crc32Hash = rs.getString(colIndex++);
        job.fileSize = rs.getLong(colIndex++);
        job.directoryId = rs.getInt(colIndex++);
        colIndex++; // skip user id field
        job.mylistId = rs.getInt(colIndex++);
        String xml = rs.getString(colIndex++);
        if (xml != null && !xml.isEmpty()) {
            try {
                job.avFileInfo = new FileInfo(xml);
            } catch (Exception ex) {
                job.avFileInfo = null;
            }
        }
        int fid = rs.getInt(colIndex);
        if (fid > 0) {
            String[] fields = new String[20];
            for (int i = 0; i < 20; i++) {
                fields[i] = rs.getString(colIndex++);
            }
            if (fields[18] == null || fields[18].isEmpty()) {
                fields[18] = job.getExtension();
            }
            String defaultName = fields[4];
            fields[4] = String.valueOf(job.mylistId);
            job.anidbFile = new AniDBFile(fields);
            job.anidbFile.pack();
            job.anidbFile.setJob(job);
            job.anidbFile.setDefaultName(defaultName);
            job.anidbFile.pack();

            fields = new String[5];
            for (int i = 0; i < 5; i++) {
                fields[i] = rs.getString(colIndex++);
            }
            AppContext.cache.add(new Episode(fields), 0, INDEX_EPISODE);
        }
    }

    public synchronized void removeJob(Job job) {
        exec(
                "delete from jtb where ed2k=" + quoteString(job.ed2kHash) + " and name="
                        + quoteString(job.currentFile.getName()),
                false);
    }

    public synchronized boolean update(int entityId, Object dataObject, int entityType) {
        if (!isInitialized) {
            return false;
        }
        try {
            fillPreparedStatement(1, updateStatements[entityType], entityId, dataObject, false);
            if (updateStatements[entityType].executeUpdate() > 0) {
                return true;
            }
        } catch (SQLException ex) {
            // Update failed, try insert
        }
        try {
            fillPreparedStatement(1, insertStatements[entityType], entityId, dataObject, true);
            return insertStatements[entityType].executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void fillPreparedStatement(
            int paramIndex, PreparedStatement ps, int entityId, Object dataObject, boolean isInsert)
            throws SQLException {
        if (dataObject instanceof Anime anime) {
            ps.setString(paramIndex++, anime.romajiTitle);
            ps.setString(paramIndex++, anime.kanjiTitle);
            ps.setString(paramIndex++, anime.englishTitle);
            ps.setInt(paramIndex++, anime.year);
            ps.setInt(paramIndex++, anime.episodeCount);
            ps.setInt(paramIndex++, anime.latestEpisode);
            ps.setString(paramIndex++, anime.type);
            ps.setString(paramIndex++, anime.categories);
            ps.setInt(paramIndex, entityId);
        } else if (dataObject instanceof Episode episode) {
            ps.setString(paramIndex++, episode.eng);
            ps.setString(paramIndex++, episode.kan);
            ps.setString(paramIndex++, episode.rom);
            ps.setString(paramIndex++, episode.num);
            ps.setInt(paramIndex, entityId);
        } else if (dataObject instanceof AniDBFile file) {
            ps.setInt(paramIndex++, file.getAnimeId());
            ps.setInt(paramIndex++, file.getEpisodeId());
            ps.setInt(paramIndex++, file.getGroupId());
            ps.setString(paramIndex++, file.getDefaultName());
            ps.setInt(paramIndex++, file.getState());
            ps.setLong(paramIndex++, file.getTotalSize());
            ps.setInt(paramIndex++, file.getLengthInSeconds());
            ps.setString(paramIndex++, file.getEd2kHash());
            ps.setString(paramIndex++, file.getMd5Hash());
            ps.setString(paramIndex++, file.getShaHash());
            ps.setString(paramIndex++, file.getCrcHash());
            ps.setString(paramIndex++, file.getDubLanguage());
            ps.setString(paramIndex++, file.getSubLanguage());
            ps.setString(paramIndex++, file.getQuality());
            ps.setString(paramIndex++, file.getRipSource());
            ps.setString(paramIndex++, file.getAudioCodec());
            ps.setString(paramIndex++, file.getVideoCodec());
            ps.setString(paramIndex++, file.getResolution());
            ps.setString(paramIndex++, file.getExtension());
            ps.setInt(paramIndex, file.getFileId());
        } else if (dataObject instanceof Group group) {
            ps.setString(paramIndex++, group.name);
            ps.setString(paramIndex++, group.shortName);
            ps.setInt(paramIndex, entityId);
        } else if (dataObject instanceof Job job) {
            if (job.directoryId < 1) {
                job.directoryId = getDirectoryId(job.currentFile.getParent());
            }
            ps.setString(paramIndex++, job.currentFile.getName());
            ps.setInt(paramIndex++, job.directoryId);
            ps.setInt(paramIndex++, job.getStatus());
            ps.setString(paramIndex++, job.md5Hash);
            ps.setString(paramIndex++, job.sha1Hash);
            ps.setString(paramIndex++, job.tthHash);
            ps.setString(paramIndex++, job.crc32Hash);
            ps.setInt(paramIndex++, job.anidbFile != null ? job.anidbFile.getFileId() : 0);
            ps.setInt(paramIndex++, job.mylistId);
            ps.setString(paramIndex++, job.avFileInfo == null ? null : job.avFileInfo.m_xml);
            ps.setLong(paramIndex++, job.currentFile.exists() ? job.currentFile.length() : job.fileSize);
            ps.setString(paramIndex++, job.ed2kHash);
            if (isInsert) {
                ps.setString(paramIndex, job.originalName);
            }
        }
    }

    // ========== Utility methods ==========

    public boolean isConnected() {
        return isInitialized;
    }

    public void shutdown() {
        if (!isInitialized && !isConnectionReady) {
            return;
        }
        try {
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        isInitialized = false;
        isConnectionReady = false;
    }

    protected void cleanDatabase() {
        exec("delete from jtb", false);
        exec("delete from dtb", false);
        exec("delete from ftb where fid>0", false);
        exec("delete from atb", false);
        exec("delete from gtb", false);
        exec("delete from etb", false);
    }

    protected void log(Object message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
