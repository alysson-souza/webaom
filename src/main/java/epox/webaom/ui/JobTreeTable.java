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
 * Created on 25.des.2005 20:18:12
 * Filename: JTreeTableR.java
 */
package epox.webaom.ui;

import com.sun.swing.JTreeTable;
import com.sun.swing.TreeTableModel;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobMan;
import epox.webaom.data.AFile;
import epox.webaom.data.Base;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class JobTreeTable extends JTreeTable implements RowModel, MouseListener {
	public JobTreeTable(TreeTableModel treeTableModel) {
		super(treeTableModel);
		addMouseListener(this);
	}

	public void updateUI() {
		long elapsedTime = System.currentTimeMillis();
		super.updateUI();
		elapsedTime = System.currentTimeMillis() - elapsedTime;
		System.out.println("@ Alt.updateUI() in " + elapsedTime + " ms. (" + AppContext.cache.stats() + ")");
	}

	public Job[] getJobs(int row) {
		Object treeNode = tree.getPathForRow(row).getLastPathComponent();
		if (treeNode instanceof AFile file) {
			if (file.getJob() != null) {
				return new Job[]{file.getJob()};
			}
		} else {
			ArrayList<Job> jobsList = new ArrayList<>();
			collectJobsRecursively(jobsList, (Base) treeNode);
			return jobsList.toArray(new Job[0]);
		}
		return null;
	}

	private void collectJobsRecursively(ArrayList<Job> jobsList, Base parent) {
		if (parent.size() < 1) {
			if (parent instanceof AFile) {
				jobsList.add(((AFile) parent).getJob());
			}
			return;
		}
		parent.buildSortedChildArray();
		for (int index = 0; index < parent.size(); index++) {
			collectJobsRecursively(jobsList, parent.get(index));
		}
	}

	public void mouseClicked(MouseEvent event) {
		if (event.getClickCount() == 2) {
			Object treeNode = tree.getPathForRow(getSelectedRow()).getLastPathComponent();
			if (treeNode instanceof AFile file) {
				if (file.getJob() != null) {
					if (event.isAltDown()) {
						JobMan.openInDefaultPlayer(file.getJob());
					} else {
						JobMan.showInfo(file.getJob());
					}
				}
			}
		}
	}

	public void mousePressed(MouseEvent event) {
		// No action required
	}

	public void mouseReleased(MouseEvent event) {
		// No action required
	}

	public void mouseEntered(MouseEvent event) {
		// No action required
	}

	public void mouseExited(MouseEvent event) {
		// No action required
	}

	private void calculateRowHeight(Graphics graphics) {
		Font font = getFont();
		FontMetrics fontMetrics = graphics.getFontMetrics(font);
		setRowHeight(fontMetrics.getHeight() + 3);
	}

	private boolean needsRowHeightCalculation = true;

	public void paint(Graphics graphics) {
		if (needsRowHeightCalculation) {
			calculateRowHeight(graphics);
			needsRowHeightCalculation = false;
		}
		super.paint(graphics);
	}

	public void setFont(Font font) {
		needsRowHeightCalculation = true;
		super.setFont(font);
	}
}
