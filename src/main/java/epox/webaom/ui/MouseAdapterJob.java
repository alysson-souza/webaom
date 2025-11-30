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

import epox.webaom.Job;
import epox.webaom.JobList;
import epox.webaom.JobManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;

public class MouseAdapterJob extends MouseAdapter {
    private final JTable table;
    private final JobList jobList;
    private final TableModelJobs tableModel;

    public MouseAdapterJob(JTable table, TableModelJobs tableModel, JobList jobList) {
        this.table = table;
        this.tableModel = tableModel;
        this.jobList = jobList;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                return;
            }
            int rowIndex = tableModel.convertRow(table.convertRowIndexToModel(selectedRow))[0];
            if (rowIndex >= 0 && rowIndex < jobList.size()) {
                Job job = jobList.get(rowIndex);
                if (event.isAltDown()) {
                    JobManager.openInDefaultPlayer(job);
                } else {
                    JobManager.showInfo(job);
                }
            }
        }
    }
}
