/*
 * Created on 09.mar.2006 16:09:43
 * Filename: KeyAdapterJob.java
 */
package epox.webaom.ui;

import epox.webaom.A;
import epox.webaom.Job;
import epox.webaom.JobMan;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTable;

public class KeyAdapterJob extends KeyAdapter {
	private final JTable table;
	private final JTreeTableR treeTable;
	private final RowModel rowModel;

	public KeyAdapterJob(JTable table, RowModel rowModel) {
		this.table = table;
		this.rowModel = rowModel;
		if (table instanceof JTreeTableR) {
			this.treeTable = (JTreeTableR) table;
		} else {
			this.treeTable = null;
		}
	}

	public void keyPressed(KeyEvent event) {
		try {
			int keyCode = event.getKeyCode();
			switch (keyCode) {
				case 'R' :
					A.gui.altViewPanel.updateAlternativeView(true);
					return;
				case 'D' :
					A.p.dump("@ ");
					return; // A.jobs.dumpHashSet();
				case 'B' :
					A.dumpStats();
					return;
				case 'L' :
					A.p.clear();
					A.gui.altViewPanel.updateAlternativeView(false);
					return;
			}
			int selectedRow = table.getSelectedRow();
			if (selectedRow < 0) {
				return;
			}
			Job[] jobs = rowModel.getJobs(selectedRow);
			if (jobs == null || jobs.length < 1) {
				return;
			}
			Job selectedJob = jobs[0];
			boolean eventConsumed = true;
			switch (keyCode) {
				case 'A' :
					A.gui.openHyperlink(selectedJob.anidbFile.urlAnime());
					break;
				case 'M' :
					A.gui.openHyperlink(selectedJob.anidbFile.urlMylist());
					break;
				case 'N' :
					A.gui.openHyperlink(selectedJob.anidbFile.urlMylistE(selectedJob.mylistId));
					break;
				case 'E' :
					A.gui.openHyperlink(selectedJob.anidbFile.urlEp());
					break;
				case 'G' :
					A.gui.openHyperlink(selectedJob.anidbFile.urlGroup());
					break;
				case 'F' :
					A.gui.openHyperlink(selectedJob.anidbFile.urlFile());
					break;
				case 'K' :
					A.gui.openHyperlink(selectedJob.anidbFile.urlExport());
					break;
				case 'W' :
					JobMan.openInDefaultPlayer(selectedJob);
					break;
				case 'X' :
					JobMan.openInExplorer(selectedJob);
					break;
				case 'C' :
					JobMan.runAvdump(selectedJob);
					A.gui.openHyperlink(selectedJob.anidbFile.urlFile());
					break;
				case 'P' :
					JobMan.updateStatus(selectedJob, Job.H_PAUSED, true);
					break;
				case 'S' :
					JobMan.updateStatus(selectedJob, Job.IDENTIFIED, true);
					break;
				case 'I' :
					selectedJob.anidbFile = null;
					JobMan.updateStatus(selectedJob, Job.HASHED, true);
					break;
				case ' ' :
				case 10 : // Enter key
					JobMan.showInfo(selectedJob);
					break;
				case 39 : // Right arrow key
					if (treeTable != null) {
						treeTable.expandRow();
					}
					break;
				case 37 : // Left arrow key
					if (treeTable != null) {
						treeTable.collapseRow();
					}
					break;
				default :
					eventConsumed = false;
			}
			if (eventConsumed) {
				event.consume();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
