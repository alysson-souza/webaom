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
 * Created on 03.09.05
 *
 * @version 	1.09
 * @author 		epoximator
 */
package epox.webaom.ui;

import epox.av.AVInfo;
import epox.swing.JTextInputDialog;
import epox.webaom.AppContext;
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
    /** Last directory selected in the folder chooser dialog. */
    private String lastSelectedDirectory = null;

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
        switch (commandId) {
            case APPLY_RULES:
                return "Apply Rules";
            case SHOW_INFO:
                return "Show Info";
            case EXPLORER:
                return "Explore Folder";
            case WATCH_NOW:
                return "Watch Now";
            case SET_FINISHED:
                return "Set Finished";
            case SET_FOLDER:
                return "Set Folder";
            case SET_PAR_FLD:
                return "Set Parent Folder";
            case REMOVE_DB:
                return "Remove from DB";
            case RESTORE_NAME:
                return "Restore Name";
            case PAUSE:
                return "Pause";
            case REHASH:
                return "Rehash";
            case REID:
                return "Identify";
            case READD:
                return "Add to mylist";
            case EDIT_PATH:
                return "Edit Folder Path";
            case EDIT_NAME:
                return "Edit File Name";
            case SET_FID:
                return "Set fid (force)";
            case REMOVE:
                return "Remove from mylist";
            case PARSE:
                return "Parse with avinfo";
            default:
                return "fook";
        }
    }

    public static boolean separator(int commandId) {
        switch (commandId) {
            case SEPARATOR_0:
            case SEPARATOR_1:
            case SEPARATOR_2:
            case SEPARATOR_3:
                return true;
            default:
                return false;
        }
    }

    /** Returns true if the command only applies to a single selected job. */
    public static boolean single(int commandId) {
        switch (commandId) {
            case SHOW_INFO:
            case WATCH_NOW:
            case EXPLORER:
            case SET_FID:
            case EDIT_NAME:
                return true;
            default:
                return false;
        }
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
            folderPath = new JTextInputDialog(
                            AppContext.frame, "Edit path", jobs[0].getFile().getParent())
                    .getStr();
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
                JobManager.updateStatus(job, Job.IDENTIFIED, true);
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
                JobManager.setName(job, new JTextInputDialog(AppContext.frame, "Edit name", currentName).getStr());
                break;
            case SET_FID:
                job.fileIdOverride = new JTextInputDialog(AppContext.frame, "Insert fid", "").getInt();
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
        if (lastSelectedDirectory != null) {
            fileChooser.setCurrentDirectory(new java.io.File(lastSelectedDirectory));
        }
        int option = fileChooser.showDialog(AppContext.component, "Select Directory");
        if (option == JFileChooser.APPROVE_OPTION) {
            lastSelectedDirectory = fileChooser.getSelectedFile().getAbsolutePath();
            return lastSelectedDirectory;
        }
        return null;
    }

    private class MenuWorker extends Thread {
        int commandId;
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
