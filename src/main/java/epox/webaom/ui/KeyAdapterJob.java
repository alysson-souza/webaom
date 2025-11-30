/*
 * Created on 09.mar.2006 16:09:43
 * Filename: KeyAdapterJob.java
 */
package epox.webaom.ui;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTable;

public class KeyAdapterJob extends KeyAdapter {
    private final JTable table;
    private final JobTreeTable treeTable;
    private final RowModel rowModel;

    public KeyAdapterJob(JTable table, RowModel rowModel) {
        this.table = table;
        this.rowModel = rowModel;
        if (table instanceof JobTreeTable) {
            this.treeTable = (JobTreeTable) table;
        } else {
            this.treeTable = null;
        }
    }

    public void keyPressed(KeyEvent event) {
        try {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case 'R':
                    AppContext.gui.altViewPanel.updateAlternativeView(true);
                    return;
                case 'D':
                    AppContext.p.dump("@ ");
                    return; // A.jobs.dumpHashSet();
                case 'B':
                    AppContext.dumpStats();
                    return;
                case 'L':
                    AppContext.p.clear();
                    AppContext.gui.altViewPanel.updateAlternativeView(false);
                    return;
                default:
                    break;
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
                case 'A':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlAnime());
                    break;
                case 'M':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlMylist());
                    break;
                case 'N':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlMylistE(selectedJob.mylistId));
                    break;
                case 'E':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlEp());
                    break;
                case 'G':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlGroup());
                    break;
                case 'F':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlFile());
                    break;
                case 'K':
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlExport());
                    break;
                case 'W':
                    JobManager.openInDefaultPlayer(selectedJob);
                    break;
                case 'X':
                    JobManager.openInExplorer(selectedJob);
                    break;
                case 'C':
                    JobManager.runAvdump(selectedJob);
                    AppContext.gui.openHyperlink(selectedJob.anidbFile.urlFile());
                    break;
                case 'P':
                    JobManager.updateStatus(selectedJob, Job.H_PAUSED, true);
                    break;
                case 'S':
                    JobManager.updateStatus(selectedJob, Job.IDENTIFIED, true);
                    break;
                case 'I':
                    selectedJob.anidbFile = null;
                    JobManager.updateStatus(selectedJob, Job.HASHED, true);
                    break;
                case ' ':
                case 10: // Enter key
                    JobManager.showInfo(selectedJob);
                    break;
                case 39: // Right arrow key
                    if (treeTable != null) {
                        treeTable.expandRow();
                    }
                    break;
                case 37: // Left arrow key
                    if (treeTable != null) {
                        treeTable.collapseRow();
                    }
                    break;
                default:
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
