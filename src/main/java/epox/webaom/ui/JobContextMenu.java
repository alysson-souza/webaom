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
import epox.swing.FileChooserBuilder;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.ui.actions.jobs.JobActionCommand;
import epox.webaom.ui.actions.jobs.JobActionController;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class JobContextMenu extends JPopupMenu implements MouseListener, ActionListener {
    protected final JTable table;
    protected final RowModel rowModel;
    private final JobActionController controller;
    private final JMenuItem[] menuItems;
    protected MenuWorker worker = null;

    public JobContextMenu(final JTable table, final RowModel rowModel) {
        this(table, rowModel, JobActionController.createDefault());
    }

    JobContextMenu(final JTable table, final RowModel rowModel, JobActionController controller) {
        this.table = table;
        this.rowModel = rowModel;
        this.controller = controller;

        menuItems = new JMenuItem[JobActionCommand.values().length];
        for (int index = 0; index < menuItems.length; index++) {
            JobActionCommand command = JobActionCommand.fromId(index);
            if (command == null || command.separator()) {
                this.addSeparator();
            } else {
                menuItems[index] = new JMenuItem(command.label());
                menuItems[index].addActionListener(this);
                menuItems[index].setActionCommand("" + index);
                this.add(menuItems[index]);
            }
        }
    }

    public static String commandText(int commandId) {
        JobActionCommand command = JobActionCommand.fromId(commandId);
        if (command == null || command.label() == null) {
            return "fook";
        }
        return command.label();
    }

    public static boolean separator(int commandId) {
        JobActionCommand command = JobActionCommand.fromId(commandId);
        return command != null && command.separator();
    }

    /** Returns true if the command only applies to a single selected job. */
    public static boolean single(int commandId) {
        JobActionCommand command = JobActionCommand.fromId(commandId);
        return command != null && command.single();
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
            menuItems[JobActionCommand.REMOVE_DB.id()].setEnabled(AppContext.databaseManager.isConnected());
            menuItems[JobActionCommand.PARSE.id()].setEnabled(AVInfo.ok());
            this.updateUI();
            show(table, event.getX(), event.getY());
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (table.getSelectedRowCount() > 0) {
            JobActionCommand command = JobActionCommand.fromId(Integer.parseInt(event.getActionCommand()));
            if (command != null) {
                worker = new MenuWorker(command);
            }
        }
    }

    void command(JobActionCommand command, Job[] jobs, String folderPath) {
        if (jobs.length < 1) {
            return;
        }
        if (command == JobActionCommand.EDIT_PATH) {
            folderPath = javax.swing.JOptionPane.showInputDialog(
                    AppContext.frame, "Edit path", jobs[0].getFile().getParent());
            if (folderPath == null || folderPath.length() < 2) {
                return;
            }
        }
        for (Job job : jobs) {
            if (job != null) {
                controller.executeCommand(command, job, folderPath);
            }
        }
    }

    void commandSingle(JobActionCommand command, Job job) {
        switch (command) {
            case EDIT_NAME:
                String currentName = job.getFile().getName();
                String editedName = javax.swing.JOptionPane.showInputDialog(AppContext.frame, "Edit name", currentName);
                controller.executeSingleCommand(command, job, editedName, null);
                break;
            case SET_FID:
                String fidInput = javax.swing.JOptionPane.showInputDialog(AppContext.frame, "Insert fid", "");
                controller.executeSingleCommand(command, job, null, fidInput);
                break;
            default:
                controller.executeSingleCommand(command, job, null, null);
                break;
        }
    }

    String getFolder() {
        FileChooserBuilder.FileChooserResult chooserResult = FileChooserBuilder.createWithLastDirectory(
                        AppContext.lastDirectory)
                .forDirectories()
                .multiSelection(false)
                .withStateStoreId("job-folder")
                .showDialog(AppContext.component, "Select Directory");

        String currentDirectory = chooserResult.getCurrentDirectory();
        if (currentDirectory != null && !currentDirectory.isEmpty()) {
            AppContext.lastDirectory = currentDirectory;
        }

        if (chooserResult.isApproved() && chooserResult.getSelectedFile() != null) {
            AppContext.lastDirectory = chooserResult.getSelectedFile().getAbsolutePath();
            return chooserResult.getSelectedFile().getAbsolutePath();
        }

        return null;
    }

    private class MenuWorker extends Thread {
        final JobActionCommand command;
        boolean run = true;

        MenuWorker(JobActionCommand command) {
            super("pMenu");
            this.command = command;
            start();
        }

        @Override
        @SuppressWarnings("checkstyle:NoWhitespaceBefore")
        public void run() {
            ie:
            if (command.single()) {
                try {
                    commandSingle(command, rowModel.getJobs(table.getSelectedRow())[0]);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    // Selection may be empty
                }
            } else {
                String folderPath = null;
                if (command.requiresFolderSelection()) {
                    folderPath = getFolder();
                    if (folderPath == null) {
                        break ie;
                    }
                }
                int[] selectedRows = table.getSelectedRows();

                table.clearSelection();
                for (int index = 0; index < selectedRows.length && run; index++) {
                    command(command, rowModel.getJobs(selectedRows[index]), folderPath);
                }
            }
            worker = null;
        }
    }
}
