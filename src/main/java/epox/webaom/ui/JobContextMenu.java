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

package epox.webaom.ui;

import epox.av.AVInfo;
import epox.webaom.AppContext;
import epox.webaom.HyperlinkBuilder;
import epox.webaom.Job;
import epox.webaom.JobManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class JobContextMenu extends JPopupMenu implements MouseListener, ActionListener {
    private static final int PAUSE = 0;
    private static final int SEPARATOR_0 = 1;
    private static final int SHOW_INFO = 2;
    private static final int WATCH_NOW = 3;
    private static final int EXPLORER = 4;
    private static final int SEPARATOR_1 = 5;
    private static final int REHASH = 6;
    private static final int REID = 7;
    private static final int READD = 8;
    private static final int REMOVE = 9;
    private static final int APPLY_RULES = 10;
    private static final int SEPARATOR_2 = 11;
    private static final int SET_FINISHED = 12;
    private static final int RESTORE_NAME = 13;
    private static final int SET_FOLDER = 14;
    private static final int SET_PAR_FLD = 15;
    private static final int EDIT_PATH = 16;
    private static final int EDIT_NAME = 17;
    private static final int SEPARATOR_3 = 18;
    private static final int PARSE = 19;
    private static final int SET_FID = 20;
    private static final int REMOVE_DB = 21;
    private static final int MENU_ITEM_COUNT = 22;
    protected final JTable table;
    protected final RowModel rowModel;
    private final JMenuItem[] menuItems;
    protected MenuWorker worker = null;

    public JobContextMenu(final JTable table, final RowModel rowModel) {
        this.table = table;
        this.rowModel = rowModel;

        menuItems = new JMenuItem[MENU_ITEM_COUNT];
        for (int index = 0; index < MENU_ITEM_COUNT; index++) {
            if (separator(index)) {
                this.addSeparator();
            } else {
                menuItems[index] = new JMenuItem(commandText(index));
                menuItems[index].addActionListener(this);
                menuItems[index].setActionCommand("" + index);
                this.add(menuItems[index]);
            }
        }
    }

    public static String commandText(int commandId) {
        return switch (commandId) {
            case APPLY_RULES -> "Apply Rules";
            case SHOW_INFO -> "Show Info";
            case EXPLORER -> "Explore Folder";
            case WATCH_NOW -> "Watch Now";
            case SET_FINISHED -> "Set Finished";
            case SET_FOLDER -> "Set Folder";
            case SET_PAR_FLD -> "Set Parent Folder";
            case REMOVE_DB -> "Remove from DB";
            case RESTORE_NAME -> "Restore Name";
            case PAUSE -> "Pause";
            case REHASH -> "Rehash";
            case REID -> "Identify";
            case READD -> "Add to mylist";
            case EDIT_PATH -> "Edit Folder Path";
            case EDIT_NAME -> "Edit File Name";
            case SET_FID -> "Set fid (force)";
            case REMOVE -> "Remove from mylist";
            case PARSE -> "Parse with avinfo";
            default -> "fook";
        };
    }

    public static boolean separator(int commandId) {
        return switch (commandId) {
            case SEPARATOR_0, SEPARATOR_1, SEPARATOR_2, SEPARATOR_3 -> true;
            default -> false;
        };
    }

    /** Returns true if the command only applies to a single selected job. */
    public static boolean single(int commandId) {
        return switch (commandId) {
            case SHOW_INFO, WATCH_NOW, EXPLORER, SET_FID, EDIT_NAME -> true;
            default -> false;
        };
    }

    public void stop() {
        try {
            worker.run = false;
        } catch (Exception ignored) {
            // don't care
        }
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        /* don't care */
    }

    @Override
    public void mouseExited(MouseEvent event) {
        /* don't care */
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (table instanceof JTableJobs) {
            ((JTableJobs) table).updateEnabled = false;
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (table instanceof JTableJobs) {
            ((JTableJobs) table).updateEnabled = true;
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (worker == null && SwingUtilities.isRightMouseButton(event)) {
            menuItems[REMOVE_DB].setEnabled(AppContext.databaseManager.isConnected());
            menuItems[PARSE].setEnabled(AVInfo.ok());
            this.updateUI();
            show(table, event.getX(), event.getY());
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (table.getSelectedRowCount() > 0) {
            worker = new MenuWorker(Integer.parseInt(event.getActionCommand()));
        }
    }

    void command(int commandId, Job[] jobs, String folderPath) {
        if (jobs.length < 1) {
            return;
        }
        if (commandId == EDIT_PATH) {
            folderPath = javax.swing.JOptionPane.showInputDialog(
                    AppContext.frame, "Edit path", jobs[0].getFile().getParent());
            if (folderPath == null || folderPath.length() < 2) {
                return;
            }
        }
        for (Job job : jobs) {
            if (job != null) {
                executeCommand(commandId, job, folderPath);
            }
        }
    }

    void executeCommand(int commandId, Job job, String folderPath) {
        switch (commandId) {
            case PAUSE:
                JobManager.updateStatus(job, Job.H_PAUSED, true);
                break;
            case REHASH:
                JobManager.updateStatus(job, Job.HASHWAIT, true);
                break;
            case REID:
                job.anidbFile = null;
                JobManager.updateStatus(job, Job.HASHED, true);
                break;
            case READD:
                JobManager.updateStatus(job, Job.ADDWAIT, true);
                break;
            case REMOVE:
                JobManager.updateStatus(job, Job.REMWAIT, true);
                break;
            case APPLY_RULES:
                if (!JobManager.applyRulesForced(job)) {
                    AppContext.gui.println(HyperlinkBuilder.formatAsError("Failed to apply rules for " + job));
                }
                break;
            case SET_FINISHED:
                JobManager.updateStatus(job, Job.FINISHED, true);
                break;
            case SET_FOLDER, EDIT_PATH:
                if (folderPath != null) {
                    JobManager.setPath(job, folderPath, false);
                }
                break;
            case SET_PAR_FLD:
                if (folderPath != null) {
                    JobManager.setPath(job, folderPath, true);
                }
                break;
            case REMOVE_DB:
                JobManager.updateStatus(job, Job.H_DELETED, true);
                break;
            case RESTORE_NAME:
                JobManager.restoreName(job);
                break;
            case PARSE:
                JobManager.updateStatus(job, Job.PARSEWAIT, true);
                break;
            default:
                // Unknown command - no action needed
                break;
        }
    }

    void commandSingle(int commandId, Job job) {
        switch (commandId) {
            case SHOW_INFO:
                JobManager.showInfo(job);
                break;
            case WATCH_NOW:
                JobManager.openInDefaultPlayer(job);
                break;
            case EXPLORER:
                JobManager.openInExplorer(job);
                break;
            case EDIT_NAME:
                String currentName = job.getFile().getName();
                JobManager.setName(
                        job, javax.swing.JOptionPane.showInputDialog(AppContext.frame, "Edit name", currentName));
                break;
            case SET_FID:
                String fidInput = javax.swing.JOptionPane.showInputDialog(AppContext.frame, "Insert fid", "");
                try {
                    job.fileIdOverride = fidInput != null ? Integer.parseInt(fidInput.trim()) : -1;
                } catch (NumberFormatException ignored) {
                    job.fileIdOverride = -1;
                }
                if (job.fileIdOverride > 0) {
                    job.anidbFile = null;
                    JobManager.updateStatus(job, Job.HASHED);
                }
                break;
            default:
                // Unknown command - no action needed
                break;
        }
    }

    String getFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        if (AppContext.lastDirectory != null) {
            fileChooser.setCurrentDirectory(new java.io.File(AppContext.lastDirectory));
        }
        int option = fileChooser.showDialog(AppContext.component, "Select Directory");
        if (option == JFileChooser.APPROVE_OPTION) {
            AppContext.lastDirectory = fileChooser.getSelectedFile().getAbsolutePath();
            return AppContext.lastDirectory;
        }
        return null;
    }

    private class MenuWorker extends Thread {
        final int commandId;
        boolean run = true;

        MenuWorker(int command) {
            super("pMenu");
            commandId = command;
            start();
        }

        @Override
        @SuppressWarnings("checkstyle:NoWhitespaceBefore")
        public void run() {
            ie:
            if (single(commandId)) {
                try {
                    commandSingle(commandId, rowModel.getJobs(table.getSelectedRow())[0]);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    // Selection may be empty
                }
            } else {
                String folderPath = null;
                if (commandId == SET_FOLDER || commandId == SET_PAR_FLD) {
                    folderPath = getFolder();
                    if (folderPath == null) {
                        break ie;
                    }
                }
                int[] selectedRows = table.getSelectedRows();

                table.clearSelection();
                for (int index = 0; index < selectedRows.length && run; index++) {
                    command(commandId, rowModel.getJobs(selectedRows[index]), folderPath);
                }
            }
            worker = null;
        }
    }
}
