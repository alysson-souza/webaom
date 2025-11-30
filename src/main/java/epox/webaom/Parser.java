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
 * @version 	0.01
 * @author 		epoximator
 */
package epox.webaom;

import epox.util.StringUtilities;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Base;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javax.swing.JFileChooser;

public final class Parser {
    private Parser() {}

    public static Group parseGroup(String[] fields) {
        if (fields == null) {
            return null;
        }
        Group group = new Group(Integer.parseInt(fields[0]));
        group.name = fields[5];
        group.shortName = fields[6];
        return group;
    }

    public static Episode parseEpisode(String[] fields) {
        if (fields == null) {
            return null;
        }
        Episode episode = new Episode(Integer.parseInt(fields[0]));
        episode.num = fields[5];
        episode.eng = fields[6];
        episode.rom = fields[7];
        episode.kan = fields[8];
        return episode;
    }

    public static Anime parseAnime(String[] fields) {
        if (fields == null) {
            return null;
        }
        Anime anime = new Anime(Integer.parseInt(fields[0]));
        anime.episodeCount = Integer.parseInt(fields[1]);
        anime.latestEpisode = Integer.parseInt(fields[2]);
        anime.year = Integer.parseInt(fields[10].substring(0, 4));
        anime.endYear = fields[10].length() != 9 ? anime.year : Integer.parseInt(fields[10].substring(5, 9));
        anime.type = fields[11].intern();
        anime.romajiTitle = fields[12];
        anime.kanjiTitle = fields[13];
        anime.englishTitle = fields[14];
        anime.categories = fields[18];
        return anime;
    }

    /** Zero padding prefixes for episode number formatting (0-4 zeros). */
    private static final String[] ZERO_PADDING_PREFIXES = {"", "0", "00", "000", "0000"};

    /**
     * Pads an episode number string with leading zeros based on total episode count.
     *
     * @param input
     *            the episode number string (may contain '-' or ',' for ranges)
     * @param totalEpisodes
     *            the total number of episodes for zero-padding calculation
     * @return the padded episode number string
     */
    public static String pad(String input, int totalEpisodes) {
        int dashIndex = input.indexOf('-');
        int commaIndex = input.indexOf(',');
        char separator = '-';
        if (commaIndex >= 0) {
            if (dashIndex < 0 || (commaIndex < dashIndex && dashIndex >= 0)) {
                separator = ',';
                dashIndex = commaIndex;
            }
        }
        String numberPart = input;
        if (dashIndex >= 0) {
            numberPart = input.substring(0, dashIndex);
        }
        if (totalEpisodes == 0) {
            totalEpisodes = AppContext.assumedEpisodeCount; // presume this
        }
        int episodeNumber;
        String prefix = "";
        char firstChar = numberPart.charAt(0);
        if (Character.isDigit(firstChar)) {
            episodeNumber = StringUtilities.i(numberPart);
        } else {
            episodeNumber = StringUtilities.i(numberPart.substring(1));
            prefix += firstChar;
            // !totalEpisodes = episodeNumber; //no total specials
            totalEpisodes = AppContext.assumedSpecialCount;
        }
        if (totalEpisodes < episodeNumber) {
            totalEpisodes = episodeNumber; // just in case...
        }
        String paddedNumber = prefix
                + ZERO_PADDING_PREFIXES[log10(totalEpisodes) - log10(episodeNumber > 0 ? episodeNumber : 1)]
                + episodeNumber;
        if (dashIndex >= 0) {
            // return paddedNumber+separator+pad(input.substring(dashIndex+1), totalEpisodes);
            return paddedNumber + separator + input.substring(dashIndex + 1);
        }
        return paddedNumber;
    }

    private static int log10(int i) {
        return (int) (Math.log(i) / Math.log(10));
    }

    public static void exportDB() {
        if (AppContext.p != null) {
            try {
                synchronized (AppContext.p) {
                    JFileChooser fileChooser = new JFileChooser();
                    if (AppContext.dir != null) {
                        fileChooser.setCurrentDirectory(new File(AppContext.dir));
                    }
                    if (fileChooser.showDialog(AppContext.component, "Select File") == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        AppContext.dir = file.getParentFile().getAbsolutePath();
                        FileOutputStream outputStream = new FileOutputStream(file);
                        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                        writer.write("a0\r\n");
                        AppContext.p.buildSortedChildArray();
                        for (int animeIndex = 0; animeIndex < AppContext.p.size(); animeIndex++) {
                            Base animeEntry = AppContext.p.get(animeIndex);
                            animeEntry.buildSortedChildArray();
                            writer.write("a" + animeEntry.serialize() + AppContext.S_N);
                            for (int episodeIndex = 0; episodeIndex < animeEntry.size(); episodeIndex++) {
                                Base episodeEntry = animeEntry.get(episodeIndex);
                                episodeEntry.buildSortedChildArray();
                                writer.write("e" + episodeEntry.serialize() + AppContext.S_N);
                                for (int fileIndex = 0; fileIndex < episodeEntry.size(); fileIndex++) {
                                    AniDBFile anidbFile = (AniDBFile) episodeEntry.get(fileIndex);
                                    writer.write("f" + anidbFile.serialize() + AppContext.S_N);
                                    if (anidbFile.getJob() != null) {
                                        writer.write("j" + anidbFile.getJob().serialize() + AppContext.S_N);
                                    }
                                }
                            }
                        }
                        writer.flush();
                        writer.close();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void importDB() throws Exception {
        AppContext.databaseManager.debug = false;
        if (AppContext.p != null) {
            try {
                synchronized (AppContext.p) {
                    JFileChooser fileChooser = new JFileChooser();
                    if (AppContext.dir != null) {
                        fileChooser.setCurrentDirectory(new File(AppContext.dir));
                    }
                    if (fileChooser.showDialog(AppContext.component, "Select File") == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        AppContext.dir = file.getParentFile().getAbsolutePath();
                        FileInputStream inputStream = new FileInputStream(file);
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        String formatVersion = reader.readLine();
                        boolean isLegacyFormat = false;
                        if (formatVersion.equals("s0")) {
                            isLegacyFormat = true;
                        } else if (!formatVersion.equals("a0")) {
                            throw new Exception("format not supported");
                        }
                        // A.p.buildSortedChildArray();
                        String line;
                        Anime currentAnime = null;
                        Episode currentEpisode = null;
                        AniDBFile currentFile = null;
                        Job currentJob = null;
                        while (reader.ready()) {
                            line = StringUtilities.htmldesc(reader.readLine());
                            if (line.isEmpty()) {
                                continue;
                            }
                            switch (line.charAt(0)) {
                                case 'a':
                                    currentAnime = new Anime(StringUtilities.split(line.substring(1), '|'));
                                    AppContext.cache.add(currentAnime, 2, DatabaseManager.INDEX_ANIME);
                                    AppContext.p.add(currentAnime);
                                    break;
                                case 'e':
                                    currentEpisode = new Episode(StringUtilities.split(line.substring(1), '|'));
                                    AppContext.cache.add(currentEpisode, 2, DatabaseManager.INDEX_EPISODE);
                                    break;
                                case 'f':
                                    String[] fields = StringUtilities.split(line.substring(1), '|');
                                    currentFile = new AniDBFile(fields);
                                    Group group = new Group(currentFile.groupId);
                                    group.name = fields[20];
                                    group.shortName = fields[21];
                                    AppContext.cache.add(group, 1, DatabaseManager.INDEX_GROUP);
                                    currentFile.anime = currentAnime;
                                    currentFile.episode = currentEpisode;
                                    currentFile.group = group;
                                    currentFile.defaultName = currentAnime.romajiTitle + " - " + currentEpisode.num
                                            + " - " + currentEpisode.eng + " - ["
                                            + ((currentFile.groupId > 0) ? group.shortName : "RAW") + "]";
                                    AppContext.databaseManager.update(
                                            currentFile.fileId, currentFile, DatabaseManager.INDEX_FILE);
                                    break;
                                case 'j':
                                    line = line.substring(1);
                                    if (isLegacyFormat) {
                                        currentJob = new Job(
                                                new File(File.separatorChar + StringUtilities.replace(line, "/", "")),
                                                Job.FINISHED);
                                        currentJob.originalName = line;
                                        currentJob.ed2kHash = currentFile.ed2kHash;
                                        currentJob.fileSize = currentFile.totalSize;
                                    } else {
                                        currentJob = new Job(StringUtilities.split(line, '|'));
                                    }
                                    currentJob.anidbFile = currentFile;
                                    currentFile.setJob(currentJob);
                                    AppContext.jobs.add(currentJob);
                                    AppContext.databaseManager.update(0, currentJob, DatabaseManager.INDEX_JOB);
                                    break;
                                default:
                                    break;
                            }
                        }
                        reader.close();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        AppContext.databaseManager.debug = true;
    }
}
