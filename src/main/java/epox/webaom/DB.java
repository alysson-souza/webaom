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
 * Created on 29.09.05
 *
 * @version 	01
 * @author 		epoximator
 */
package epox.webaom;

import epox.av.FileInfo;
import epox.util.U;
import epox.webaom.data.AFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Base;
import epox.webaom.data.Ep;
import epox.webaom.data.Group;
import epox.webaom.util.PlatformPaths;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class DB {
	/** Maximum retry attempts for database operations after connection failures. */
	public static final int RETRY_LIMIT = 1;
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
	private static final String SQL_JOB_QUERY = "select"
			+ " d.name,j.name,j.status,j.orig,j.ed2k,j.md5,j.sha1,j.tth,j.crc32,j.size,j.did,j.uid,j.lid,j.avxml,f.fid,f.aid,f.eid,f.gid,f.def_name,f.state,f.size,f.ed2k,f.md5,f.sha1,f.crc32,f.dublang,f.sublang,f.quality,f.ripsource,f.audio,f.video,f.resolution,f.ext,f.len,e.eid,e.number,e.english,e.romaji,e.kanji"
			+ " from dtb d,jtb j,ftb f,etb e where d.did=j.did and j.fid=f.fid and f.eid=e.eid";
	private Connection con = null;
	/** Cache mapping directory paths to their database IDs. */
	private final HashMap<String, Integer> directoryIdCache = new HashMap<String, Integer>();
	/** JDBC connection URL. */
	private String connectionUrl = null;
	/** Database username. */
	private String username = null;
	/** Database password. */
	private String password = null;
	/** Whether the database connection has been initialized. */
	private boolean isInitialized = false;
	/** Whether to load all jobs (including finished) from database. */
	private boolean loadAllJobs = false;
	/** Whether the database is PostgreSQL (vs MySQL/H2). */
	private boolean isPostgreSQL = true;
	/** Whether to clean the database on startup. */
	private boolean shouldCleanDatabase = false;

	// private PreparedStatement psau, pseu, psgu, psfu, psai, psei, psgi, psfi, psju, psji;
	/** Prepared statements for UPDATE operations, indexed by entity type. */
	private PreparedStatement[] updateStatements;
	/** Prepared statements for INSERT operations, indexed by entity type. */
	private PreparedStatement[] insertStatements;
	private Statement statement = null;

	/** Clears all job-related data from the database. */
	private synchronized void cleanDatabase() {
		exec("delete from jtb", false);
		exec("delete from dtb", false);
		exec("delete from ftb where fid>0", false);
		exec("delete from atb", false);
		exec("delete from gtb", false);
		exec("delete from etb", false);
	}

	/** Logs an exception's stack trace. */
	private void logException(Exception ex) {
		ex.printStackTrace();
	}

	/** Executes a semicolon-delimited batch of SQL statements. */
	private void executeStatements(String sqlBatch, boolean silent) {
		String[] statements = sqlBatch.split("\\;");
		for (int i = 0; i < statements.length; i++) {
			exec(statements[i], silent);
		}
	}

	public boolean initialize(String databaseString) {
		if (isInitialized) {
			return false;
		}
		try {
			if (databaseString.startsWith("!")) {
				loadAllJobs = true;
				databaseString = databaseString.substring(1);
			} else if (databaseString.startsWith("?")) {
				shouldCleanDatabase = A.confirm("Warning", "Do you really want to clean the db?", "Yes", "No");
				databaseString = databaseString.substring(1);
			} else {
				loadAllJobs = false;
				shouldCleanDatabase = false;
			}
			username = "root";
			password = null;

			// Try to initialize with provided database string, or fallback to embedded H2
			boolean success = tryInitializeDatabase(databaseString);
			if (!success) {
				// If external database fails, attempt fallback to embedded H2
				log("External database connection failed, attempting embedded H2 database...");
				String embeddedDbPath = PlatformPaths.getDefaultEmbeddedDatabasePath();
				String h2ConnectionString = "jdbc:h2:" + embeddedDbPath;
				success = tryInitializeDatabase(h2ConnectionString);

				if (success) {
					log("Successfully initialized embedded H2 database at: " + embeddedDbPath);
				} else {
					return false;
				}
			}

			if (!connect()) {
				return false;
			}
			if (!updateSchema()) {
				shutdown();
				return false;
			}
			if (shouldCleanDatabase) {
				cleanDatabase();
			}
			return true;
		} catch (Exception e) {
			A.dialog("Database:", e.toString());
		}
		return false;
	}

	/** Attempts to initialize a database connection with the given connection string. */
	private boolean tryInitializeDatabase(String databaseString) {
		try {
			if (databaseString.indexOf("postgresql") > 0) {
				Class.forName("org.postgresql.Driver").getDeclaredConstructor().newInstance();
				isPostgreSQL = true;
			} else if (databaseString.indexOf("mysql") > 0) {
				Class.forName("com.mysql.jdbc.Driver").getDeclaredConstructor().newInstance();
				isPostgreSQL = false;
			} else if (databaseString.indexOf("h2") > 0) {
				Class.forName("org.h2.Driver").getDeclaredConstructor().newInstance();
				isPostgreSQL = false;
			} else {
				return false;
			}

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

			return true;
		} catch (Exception ex) {
			log("Database initialization attempt failed: " + ex.getMessage());
			return false;
		}
	}

	/** Establishes a database connection and prepares SQL statements. */
	private boolean connect() {
		isInitialized = false;
		if (con != null) {
			try {
				con.close();
			} catch (SQLException ex) {
				// don't care
			}
		}
		try {
			if (password != null) {
				con = DriverManager.getConnection(connectionUrl, username, password);
			} else {
				con = DriverManager.getConnection(connectionUrl + "?user=" + username);
			}
			statement = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

			updateStatements = new PreparedStatement[INDEX_COUNT];
			updateStatements[INDEX_ANIME] = con.prepareStatement("update atb set"
					+ " romaji=?,kanji=?,english=?,year=?,episodes=?,last_ep=?,type=?,genre=?,img=0" + " where aid=?");
			updateStatements[INDEX_EPISODE] = con
					.prepareStatement("update etb set english=?,kanji=?,romaji=?,number=? where eid=?");
			updateStatements[INDEX_FILE] = con.prepareStatement("update ftb set"
					+ " aid=?,eid=?,gid=?,def_name=?,state=?,size=?,len=?,ed2k=?,md5=?,sha1=?,crc32=?,dublang=?,sublang=?,quality=?,ripsource=?,audio=?,video=?,resolution=?,ext=?"
					+ " where fid=?");
			updateStatements[INDEX_GROUP] = con.prepareStatement("update gtb set name=?,short=? where gid=?");
			updateStatements[INDEX_JOB] = con.prepareStatement(
					"update jtb set" + " name=?,did=?,status=?,md5=?,sha1=?,tth=?,crc32=?,fid=?,lid=?,avxml=?"
							+ " where size=? and ed2k=?");
			insertStatements = new PreparedStatement[INDEX_COUNT];
			insertStatements[INDEX_ANIME] = con.prepareStatement(
					"insert into atb" + " (romaji,kanji,english,year,episodes,last_ep,type,genre,img,aid)"
							+ " values (?,?,?,?,?,?,?,?,0,?)");
			insertStatements[INDEX_EPISODE] = con
					.prepareStatement("insert into etb (english,kanji,romaji,number,eid) values (?,?,?,?,?)");
			insertStatements[INDEX_FILE] = con.prepareStatement("insert into ftb"
					+ " (aid,eid,gid,def_name,state,size,len,ed2k,md5,sha1,crc32,dublang,sublang,quality,ripsource,audio,video,resolution,ext,fid)"
					+ " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			insertStatements[INDEX_GROUP] = con.prepareStatement("insert into gtb (name,short,gid) values (?,?,?)");
			insertStatements[INDEX_JOB] = con.prepareStatement(
					"insert into jtb" + " (name,did,status,md5,sha1,tth,crc32,fid,lid,avxml,size,ed2k,orig,uid)"
							+ " values (?,?,?,?,?,?,?,?,?,?,?,?,?,1)");

			isInitialized = true;
			return true;
		} catch (SQLException ex) {
			return false;
		}
	}

	/** Checks whether the database is connected and initialized. */
	public boolean isConnected() {
		return isInitialized;
	}

	public volatile boolean debug = true;

	/** Logs a debug message to stdout if debug mode is enabled. */
	private void log(Object message) {
		if (debug) {
			System.out.println(message);
		}
	}

	/** Shuts down the database connection and releases resources. */
	public void shutdown() {
		if (!isInitialized) {
			return;
		}
		try {
			if (statement != null) {
				statement.close();
			}
			if (con != null) {
				con.close();
			}
		} catch (SQLException ex) {
			logException(ex);
		}
		isInitialized = false;
	}

	/** Updates the database schema if needed. */
	private boolean updateSchema() {
		boolean silent = true;
		try {
			ResultSet rs = query("select state from mylist where lid=0", silent);
			if (rs != null && rs.next()) { // old system
				int version = rs.getInt(1);
				if (version < 1) {
					executeStatements(A.getFileString("db01.sql"), silent);
				}
				if (version < 2) {
					executeStatements(A.getFileString("db02.sql"), silent);
				}
			} else { // new system or none db defined
				rs = query("select ver from vtb;", silent);
				if (rs != null && rs.next()) {
					int version = rs.getInt(1);
					if (version < 4) {
						if (!A.confirm("Warning",
								"The database definition has to be upgraded.\n"
										+ "This will make it uncompatible with previous versions of" + " WebAOM.\n"
										+ "Do you want to continue? (Backup now, if needed.)",
								"Yes", "No")) {
							return false;
						}
					}
					if (version < 1) {
						executeStatements(A.getFileString("db03.sql"), silent);
					}
					if (version < 2) {
						executeStatements(A.getFileString("db04.sql"), silent);
					}
					if (version < 3) {
						executeStatements(A.getFileString("db05.sql"), silent);
					}
					if (version < 4) {
						executeStatements(A.getFileString("db06.sql"), silent);
					}
					if (version < 5) {
						if (isPostgreSQL) {
							executeStatements(A.getFileString("db07a.sql"), silent);
						} else {
							executeStatements(A.getFileString("db07b.sql"), silent);
						}
					}
					if (version < 6) {
						if (isPostgreSQL) {
							executeStatements(A.getFileString("db08a.sql"), silent);
						} else {
							executeStatements(A.getFileString("db08b.sql"), silent);
						}
					}
				} else {
					// Initialize new database with appropriate schema
					String schemaSql;
					if (connectionUrl.indexOf("h2") > 0) {
						// H2 database - use H2-specific schema
						schemaSql = A.getFileString("db00-h2.sql");
					} else if (isPostgreSQL) {
						// PostgreSQL - use default schema (has serial)
						schemaSql = A.getFileString("db00.sql");
					} else {
						// MySQL - convert serial to auto_increment
						schemaSql = A.getFileString("db00.sql");
						schemaSql = U.replace(schemaSql, "serial", "integer NOT NULL auto_increment");
					}
					executeStatements(schemaSql, silent);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	/** Checks if an error message indicates a communication exception. */
	private static boolean isCommunicationException(String message) {
		return message.toLowerCase().indexOf("communication") >= 0;
	}

	private boolean exec(String command, boolean silent) {
		return exec(command, silent, RETRY_LIMIT);
	}

	private ResultSet query(String command, boolean silent) {
		return query(command, silent, RETRY_LIMIT);
	}

	private boolean exec(String command, boolean silent, int retryCount) {
		if (!isInitialized) {
			return false;
		}
		try {
			if (!silent) {
				log("} " + command);
			}
			statement.execute(command);
			return true;
		} catch (SQLException ex) {
			if (isCommunicationException(ex.getMessage())) {
				log("! CommunicationsException: " + ex.getMessage());
				if (retryCount > 0 && connect()) {
					return exec(command, silent, retryCount - 1);
				}
				return false;
			}
			if (!silent) {
				if (ex.getErrorCode() != 1062) {
					log("! DB Error Code: " + ex.getErrorCode());
					logException(ex);
				} else {
					log("{ DUPE!");
				}
			}
			return false;
		}
	}

	private ResultSet query(String command, boolean silent, int retryCount) {
		if (!isInitialized) {
			return null;
		}
		try {
			if (!silent) {
				log("} " + command);
			}
			return statement.executeQuery(command);
		} catch (SQLException ex) {
			if (isCommunicationException(ex.getMessage())) {
				log("! CommunicationsException: " + ex.getMessage());
				if (retryCount > 0 && connect()) {
					return query(command, silent, retryCount - 1);
				}
				return null;
			}
			if (!silent) {
				if (ex.getErrorCode() != 1062) {
					log("! DB Error Code: " + ex.getErrorCode());
					logException(ex);
				} else {
					log("{ DUPE!");
				}
			}
			return null;
		}
	}

	/**
	 * Fills a prepared statement with values from a data object.
	 *
	 * @param paramIndex
	 *            starting parameter index
	 * @param preparedStatement
	 *            the statement to fill
	 * @param entityId
	 *            the entity ID
	 * @param dataObject
	 *            the data object (Anime, Ep, AFile, Group, or Job)
	 * @param isInsert
	 *            whether this is an insert operation (includes extra fields)
	 * @return the next parameter index after filling
	 */
	private int fillPreparedStatement(int paramIndex, PreparedStatement preparedStatement, int entityId,
			Object dataObject, boolean isInsert) throws SQLException {
		if (dataObject instanceof Anime anime) {
			preparedStatement.setString(paramIndex++, anime.romajiTitle);
			preparedStatement.setString(paramIndex++, anime.kanjiTitle);
			preparedStatement.setString(paramIndex++, anime.englishTitle);
			preparedStatement.setInt(paramIndex++, anime.year);
			preparedStatement.setInt(paramIndex++, anime.episodeCount);
			preparedStatement.setInt(paramIndex++, anime.latestEpisode);
			preparedStatement.setString(paramIndex++, anime.type);
			preparedStatement.setString(paramIndex++, anime.categories);
			preparedStatement.setInt(paramIndex++, entityId);
		} else if (dataObject instanceof Ep episode) {
			preparedStatement.setString(paramIndex++, episode.eng);
			preparedStatement.setString(paramIndex++, episode.kan);
			preparedStatement.setString(paramIndex++, episode.rom);
			preparedStatement.setString(paramIndex++, episode.num);
			preparedStatement.setInt(paramIndex++, entityId);
		} else if (dataObject instanceof AFile file) {
			preparedStatement.setInt(paramIndex++, file.animeId);
			preparedStatement.setInt(paramIndex++, file.episodeId);
			preparedStatement.setInt(paramIndex++, file.groupId);
			preparedStatement.setString(paramIndex++, file.defaultName);
			preparedStatement.setInt(paramIndex++, file.state);
			preparedStatement.setLong(paramIndex++, file.totalSize);
			preparedStatement.setInt(paramIndex++, file.lengthInSeconds);
			preparedStatement.setString(paramIndex++, file.ed2kHash);
			preparedStatement.setString(paramIndex++, file.md5Hash);
			preparedStatement.setString(paramIndex++, file.shaHash);
			preparedStatement.setString(paramIndex++, file.crcHash);
			preparedStatement.setString(paramIndex++, file.dubLanguage);
			preparedStatement.setString(paramIndex++, file.subLanguage);
			preparedStatement.setString(paramIndex++, file.quality);
			preparedStatement.setString(paramIndex++, file.ripSource);
			preparedStatement.setString(paramIndex++, file.audioCodec);
			preparedStatement.setString(paramIndex++, file.videoCodec);
			preparedStatement.setString(paramIndex++, file.resolution);
			preparedStatement.setString(paramIndex++, file.extension);
			preparedStatement.setInt(paramIndex++, file.fileId);
		} else if (dataObject instanceof Group group) {
			preparedStatement.setString(paramIndex++, group.name);
			preparedStatement.setString(paramIndex++, group.shortName);
			preparedStatement.setInt(paramIndex++, entityId);
		} else if (dataObject instanceof Group group) {
			preparedStatement.setString(paramIndex++, group.name);
			preparedStatement.setString(paramIndex++, group.shortName);
			preparedStatement.setInt(paramIndex++, entityId);
		} else if (dataObject instanceof Job job) {
			if (job.directoryId < 1) {
				job.directoryId = getDirectoryId(job.currentFile.getParent());
			}
			preparedStatement.setString(paramIndex++, job.currentFile.getName());
			preparedStatement.setInt(paramIndex++, job.directoryId);
			preparedStatement.setInt(paramIndex++, job.getStatus());
			preparedStatement.setString(paramIndex++, job.md5Hash);
			preparedStatement.setString(paramIndex++, job.sha1Hash);
			preparedStatement.setString(paramIndex++, job.tthHash);
			preparedStatement.setString(paramIndex++, job.crc32Hash);
			preparedStatement.setInt(paramIndex++, job.anidbFile != null ? job.anidbFile.fileId : 0);
			preparedStatement.setInt(paramIndex++, job.mylistId);
			preparedStatement.setString(paramIndex++, job.avFileInfo == null ? null : job.avFileInfo.m_xml);
			if (job.currentFile.exists()) {
				preparedStatement.setLong(paramIndex++, job.currentFile.length());
			} else {
				preparedStatement.setLong(paramIndex++, job.fileSize);
			}
			preparedStatement.setString(paramIndex++, job.ed2kHash);
			if (isInsert) {
				preparedStatement.setString(paramIndex, job.originalName);
			}
		}
		return paramIndex;
	}

	/**
	 * Gets or creates a directory ID from the database cache.
	 *
	 * @param path
	 *            the directory path
	 * @return the directory ID, or -1 if not found/created
	 */
	private int getDirectoryId(String path) {
		if (!isInitialized) {
			return -1;
		}
		if (path == null) {
			path = "";
		}
		try {
			path = U.replace(path, "\\", "\\\\");
			path = U.replace(path, "'", "\\'");
			Object cachedId = directoryIdCache.get(path);
			if (cachedId != null) {
				return ((Integer) cachedId).intValue();
			}
			ResultSet rs = query("select did from dtb where name='" + path + "'", false);
			if (rs.first()) {
				int directoryId = rs.getInt(1);
				directoryIdCache.put(path, Integer.valueOf(directoryId));
				return directoryId;
			}
			log("} insert into dtb (name) values ('" + path + "')");
			if (isPostgreSQL) {
				statement.execute("insert into dtb (name) values ('" + path + "');SELECT currval('dtb_did_seq')");
				if (!statement.getMoreResults()) {
					return -1;
				}
				rs = statement.getResultSet();
				if (rs.first()) {
					int directoryId = rs.getInt(1);
					directoryIdCache.put(path, Integer.valueOf(directoryId));
					return directoryId;
				}
			} else {
				statement.executeUpdate("insert into dtb (name) values ('" + path + "')");
				rs = statement.getGeneratedKeys();
				if (rs.first()) {
					int directoryId = rs.getInt(1);
					directoryIdCache.put(path, Integer.valueOf(directoryId));
					return directoryId;
				}
			}
		} catch (SQLException ex) {
			logException(ex);
		}
		return -1;
	}

	/**
	 * Retrieves a generic database entity by ID and type.
	 *
	 * @param entityId
	 *            the entity ID
	 * @param entityType
	 *            the entity type (INDEX_ANIME, INDEX_EPISODE, or INDEX_GROUP)
	 * @return the retrieved entity, or null if not found
	 */
	public synchronized Base getGeneric(int entityId, int entityType) {
		if (!isInitialized) {
			return null;
		}
		try {
			if (entityType == INDEX_EPISODE) {
				Ep episode = new Ep(entityId);
				ResultSet rs = query("select english,kanji,romaji,number from etb where eid=" + entityId + ";", false);
				if (rs.first()) {
					episode.eng = rs.getString(1);
					episode.kan = rs.getString(2);
					episode.rom = rs.getString(3);
					episode.num = rs.getString(4).intern();
					log("{ " + episode);
					return episode;
				}
			} else if (entityType == INDEX_GROUP) {
				Group group = new Group(entityId);
				ResultSet rs = query("select name,short from gtb where gid=" + entityId + ";", false);
				if (rs.first()) {
					group.name = rs.getString(1);
					group.shortName = rs.getString(2);
					log("{ " + group);
					return group;
				}
			} else if (entityType == INDEX_ANIME) {
				ResultSet rs = query("select episodes,last_ep,year,type,romaji,kanji,english,genre,img"
						+ " from atb where aid=" + entityId + ";", false);
				if (rs.first()) {
					int colIndex = 1;
					String[] fields = new String[9];
					fields[0] = "" + entityId;
					for (int fieldIndex = 1; fieldIndex < fields.length; fieldIndex++) {
						fields[colIndex] = rs.getString(colIndex++);
					}
					Anime anime = new Anime(fields);
					log("{ " + anime);
					return anime;
				}
			}
			return null;
		} catch (SQLException ex) {
			logException(ex);
			return null;
		}
	}

	/**
	 * Retrieves job data from the database.
	 *
	 * @param job
	 *            the job to populate
	 * @param useEd2k
	 *            whether to look up by ED2K hash (true) or by name/directory (false)
	 * @return the job status, or -1 if not found
	 */
	public synchronized int getJob(Job job, boolean useEd2k) {
		if (!isInitialized) {
			return -1;
		}
		try {
			ResultSet rs;
			if (useEd2k) {
				rs = query(SQL_JOB_QUERY + " and j.size=" + job.currentFile.length() + " and j.ed2k="
						+ quoteString(job.ed2kHash) + ";", false);
			} else {
				int directoryId = getDirectoryId(job.currentFile.getParent());
				rs = query(SQL_JOB_QUERY + " and j.size=" + job.currentFile.length() + " and j.name="
						+ quoteString(job.currentFile.getName()) + " and j.did=" + directoryId + ";", false);
			}
			if (rs.first()) {
				int colIndex = 1;
				job.targetFile = new File(rs.getString(colIndex++) + File.separatorChar + rs.getString(colIndex++));
				if (job.currentFile.equals(job.targetFile)) {
					job.targetFile = null;
				}
				int status = rs.getInt(colIndex++);
				populateJobFromResultSet(rs, colIndex, job);
				job.isFresh = false;
				log("{ Job found: " + job + ":" + status);
				return status;
			}
		} catch (SQLException ex) {
			logException(ex);
		}
		return -1;
	}

	/** Loads all jobs from the database into the job list. */
	public synchronized void getJobs() {
		if (!isInitialized) {
			return;
		}
		try {
			Job job;
			ResultSet resultSet;
			if (loadAllJobs) {
				resultSet = query(SQL_JOB_QUERY + " ORDER BY j.time", false);
			} else {
				resultSet = query(SQL_JOB_QUERY + " and j.status!=" + Job.FINISHED + " ORDER BY j.time", false);
			}

			while (resultSet.next()) {
				int columnIndex = 1;
				File file = new File(
						resultSet.getString(columnIndex++) + File.separatorChar + resultSet.getString(columnIndex++));
				job = new Job(file, resultSet.getInt(columnIndex++));
				populateJobFromResultSet(resultSet, columnIndex, job);
				if (!A.jobs.add(job)) {
					U.err("DB: Dupe: " + job);
				}
			}
		} catch (SQLException ex) {
			logException(ex);
		}
	}

	/**
	 * Populates a Job object with data from a ResultSet.
	 *
	 * @param rs
	 *            the result set positioned at the job row
	 * @param colIndex
	 *            the starting column index
	 * @param job
	 *            the job to populate
	 */
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
			for (int fieldIndex = 0; fieldIndex < 20; fieldIndex++) {
				fields[fieldIndex] = rs.getString(colIndex++);
			}

			if (fields[18] == null || fields[18].isEmpty()) {
				fields[18] = job.getExtension();
			}

			String defaultName = fields[4];
			fields[4] = "" + job.mylistId;
			job.anidbFile = new AFile(fields);
			job.anidbFile.pack();
			job.anidbFile.setJob(job);
			job.anidbFile.defaultName = defaultName;
			job.anidbFile.pack();
			fields = new String[5];
			for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
				fields[fieldIndex] = rs.getString(colIndex++);
			}
			A.cache.add(new Ep(fields), 0, DB.INDEX_EPISODE);
		}
	}

	/** Removes a job from the database. */
	public synchronized boolean removeJob(Job job) {
		return exec("delete from jtb where ed2k=" + quoteString(job.ed2kHash) + " and name="
				+ quoteString(job.currentFile.getName()) + ";", false);
	}

	/**
	 * Quotes a string value for use in SQL, handling null and escaping quotes.
	 *
	 * @param value
	 *            the string to quote
	 * @return the quoted string, or "NULL" if value is null
	 */
	private String quoteString(String value) {
		if (value == null) {
			return "NULL";
		}
		return "'" + U.replace(value, "'", "\\'") + "'";
	}

	/**
	 * Updates or inserts an entity in the database.
	 *
	 * @param entityId
	 *            the entity ID
	 * @param dataObject
	 *            the data object to persist
	 * @param entityType
	 *            the entity type index
	 * @return true if successful
	 */
	public synchronized boolean update(int entityId, Object dataObject, int entityType) {
		return updateWithRetry(entityId, dataObject, entityType, RETRY_LIMIT);
	}

	private boolean updateWithRetry(int entityId, Object dataObject, int entityType, int retryCount) {
		try {
			return performUpdate(entityId, dataObject, entityType);
		} catch (SQLException ex) {
			log("! CommunicationsException: " + ex.getMessage());
			if (retryCount > 0 && connect()) {
				return updateWithRetry(entityId, dataObject, entityType, retryCount - 1);
			}
		}
		return false;
	}

	private boolean performUpdate(int entityId, Object dataObject, int entityType) throws SQLException {
		if (!isInitialized) {
			return false;
		}
		try {
			fillPreparedStatement(1, updateStatements[entityType], entityId, dataObject, false);
			if (executeUpdate(updateStatements[entityType]) > 0) {
				return true;
			}
		} catch (SQLException ex) {
			if (isCommunicationException(ex.getMessage())) {
				throw ex;
			}
			logException(ex);
		}
		try {
			fillPreparedStatement(1, insertStatements[entityType], entityId, dataObject, true);
			if (executeUpdate(insertStatements[entityType]) > 0) {
				return true;
			}
		} catch (SQLException ex) {
			if (isCommunicationException(ex.getMessage())) {
				throw ex;
			}
			logException(ex);
		}
		return false;
	}

	/**
	 * Executes a prepared statement and logs its SQL.
	 *
	 * @param preparedStatement
	 *            the statement to execute
	 * @return the number of rows affected
	 */
	private int executeUpdate(PreparedStatement preparedStatement) throws SQLException {
		String sqlString = preparedStatement.toString();
		if (!isPostgreSQL) {
			int colonIndex = sqlString.indexOf(": ");
			if (colonIndex > 0) {
				sqlString = sqlString.substring(colonIndex + 2);
			} else {
				int dashIndex = sqlString.indexOf(" - ");
				if (dashIndex > 0) {
					sqlString = sqlString.substring(dashIndex + 3);
				}
			}
		}
		log("} " + sqlString);
		return preparedStatement.executeUpdate();
	}
}
