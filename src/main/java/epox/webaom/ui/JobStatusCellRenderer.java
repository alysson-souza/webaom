// Copyright (C) 2025 Alysson Souza
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

package epox.webaom.ui;

import epox.webaom.Job;
import java.awt.Component;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renders the job status as text, or as a progress bar while the job is being hashed. {@link JTableJobs} colors the
 * bar's track to match the row.
 */
public class JobStatusCellRenderer extends DefaultTableCellRenderer {
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    public JobStatusCellRenderer() {
        progressBar.setStringPainted(true);
        progressBar.setBorderPainted(false);
    }

    /** The bar is not in the component tree, so a look-and-feel change only reaches it through this renderer. */
    @Override
    public void updateUI() {
        super.updateUI();
        // The superclass constructor calls this before the field is assigned
        if (progressBar != null) {
            progressBar.updateUI();
        }
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Object rowValue = table.getModel().getValueAt(table.convertRowIndexToModel(row), JobColumn.JOB);
        if (rowValue instanceof Job job && job.check(Job.HASHING)) {
            progressBar.setValue(job.getHashPercent());
            progressBar.setString(String.valueOf(value));
            progressBar.setFont(table.getFont());
            return progressBar;
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
