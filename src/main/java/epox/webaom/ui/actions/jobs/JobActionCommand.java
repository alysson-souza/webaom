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

package epox.webaom.ui.actions.jobs;

public enum JobActionCommand {
    PAUSE(0, "Pause", false, false),
    SEPARATOR_0(1, null, false, true),
    SHOW_INFO(2, "Show Info", true, false),
    WATCH_NOW(3, "Watch Now", true, false),
    EXPLORER(4, "Explore Folder", true, false),
    SEPARATOR_1(5, null, false, true),
    REHASH(6, "Rehash", false, false),
    REID(7, "Identify", false, false),
    READD(8, "Add to mylist", false, false),
    REMOVE(9, "Remove from mylist", false, false),
    APPLY_RULES(10, "Apply Rules", false, false),
    SEPARATOR_2(11, null, false, true),
    SET_FINISHED(12, "Set Finished", false, false),
    RESTORE_NAME(13, "Restore Name", false, false),
    SET_FOLDER(14, "Set Folder", false, false),
    SET_PAR_FLD(15, "Set Parent Folder", false, false),
    EDIT_PATH(16, "Edit Folder Path", false, false),
    EDIT_NAME(17, "Edit File Name", true, false),
    SEPARATOR_3(18, null, false, true),
    PARSE(19, "Parse with avinfo", false, false),
    SET_FID(20, "Set fid (force)", true, false),
    REMOVE_DB(21, "Remove from DB", false, false);

    private final int id;
    private final String label;
    private final boolean single;
    private final boolean separator;

    JobActionCommand(int id, String label, boolean single, boolean separator) {
        this.id = id;
        this.label = label;
        this.single = single;
        this.separator = separator;
    }

    public int id() {
        return id;
    }

    public String label() {
        return label;
    }

    public boolean single() {
        return single;
    }

    public boolean separator() {
        return separator;
    }

    public boolean requiresFolderSelection() {
        return this == SET_FOLDER || this == SET_PAR_FLD;
    }

    public static JobActionCommand fromId(int commandId) {
        for (JobActionCommand command : values()) {
            if (command.id == commandId) {
                return command;
            }
        }
        return null;
    }
}
