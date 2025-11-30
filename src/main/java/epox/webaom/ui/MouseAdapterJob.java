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
 * Created on 02.09.05
 *
 * @version 	1.09
 * @author 		epoximator
 */

package epox.webaom.ui;

import epox.webaom.Job;
import epox.webaom.JobList;
import epox.webaom.JobMan;
import java.awt.event.InputEvent;
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

	public void mouseClicked(MouseEvent event) {
		if (event.getClickCount() == 2) {
			int rowIndex = tableModel.convertRow(table.getSelectedRow())[0];
			if (rowIndex >= 0 && rowIndex < jobList.size()) {
				Job job = jobList.get(rowIndex);
				if ((event.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
					JobMan.openInDefaultPlayer(job);
				} else {
					JobMan.showInfo(job);
				}
			}
		}
	}
}
