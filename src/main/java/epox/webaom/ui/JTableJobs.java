/*
 * Created on 09.mar.2006 20:51:26
 * Filename: JTableJobs.java
 */
package epox.webaom.ui;

import epox.swing.JTableSortable;
import epox.webaom.A;
import epox.webaom.Job;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellRenderer;

public class JTableJobs extends JTableSortable {
	private final TableModelJobs jobsTableModel;

	public JTableJobs(TableModelJobs tableModel) {
		super(tableModel);
		jobsTableModel = tableModel;
		final JPopupMenuM popupMenu = new JPopupMenuM(this, tableModel);
		A.com0 = popupMenu;
		addMouseListener(popupMenu);

		addMouseListener(new MouseAdapterJob(this, tableModel, A.jobs));
		getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "stop");
		getActionMap().put("stop", new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				popupMenu.stop();
			}
		});
		addKeyListener(new KeyAdapterJob(this, tableModel));
	}

	/** Color for jobs doing disk I/O (blue) */
	private static final Color COLOR_DISK_IO = new Color(0, 102, 153);
	/** Color for jobs doing network I/O (red) */
	private static final Color COLOR_NETWORK_IO = new Color(182, 0, 20);
	/** Color for missing files (gray) */
	private static final Color COLOR_MISSING = new Color(100, 100, 100);
	/** Background color for invalid/corrupt jobs (light red) */
	private static final Color COLOR_INVALID = new Color(255, 180, 180);

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component component = super.prepareRenderer(renderer, row, column);

		if (isSelected(row)) {
			return component;
		}

		Job job = (Job) jobsTableModel.getValueAt(row, TableModelJobs.JOB);

		if (job.isCorrupt()) {
			component.setBackground(COLOR_INVALID);
			component.setForeground(Color.black);
		} else {
			component.setBackground(this.getBackground());

			if (job.check(Job.D_DIO | Job.S_DOING)) {
				component.setForeground(COLOR_DISK_IO);
			} else if (job.check(Job.D_NIO | Job.S_DOING)) {
				component.setForeground(COLOR_NETWORK_IO);
			} else if (job.check(Job.H_MISSING)) {
				component.setForeground(COLOR_MISSING);
			} else {
				component.setForeground(Color.black);
			}
		}

		return component;
	}

	private boolean isSelected(int row) {
		int[] selectedRows = getSelectedRows();
		Arrays.sort(selectedRows);
		return Arrays.binarySearch(selectedRows, row) >= 0;
	}

	/** Controls whether UI updates should be processed */
	public boolean updateEnabled = true;

}
