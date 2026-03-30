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

import epox.swing.JTableSortable;
import epox.swing.ThemeColorSupport;
import epox.webaom.AppContext;
import epox.webaom.Job;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellRenderer;

public class JTableJobs extends JTableSortable {
    /** Color for jobs doing disk I/O (blue) */
    private static final Color COLOR_DISK_IO = new Color(0, 102, 153);
    /** Color for jobs doing network I/O (red) */
    private static final Color COLOR_NETWORK_IO = new Color(182, 0, 20);
    /** Color for missing files (gray) */
    private static final Color COLOR_MISSING = new Color(100, 100, 100);
    /** Background color for invalid/corrupt jobs (light red) */
    private static final Color COLOR_INVALID = new Color(255, 180, 180);

    private static final double MINIMUM_NORMAL_CONTRAST = 4.5;
    private static final double MINIMUM_STATE_CONTRAST = 3.0;
    private static final double INVALID_BACKGROUND_BLEND_RATIO = 0.35;
    private static final double MISSING_FOREGROUND_BLEND_RATIO = 0.30;

    private final TableModelJobs jobsTableModel;
    /** Controls whether UI updates should be processed */
    public boolean updateEnabled = true;

    public JTableJobs(TableModelJobs tableModel) {
        super(tableModel);
        jobsTableModel = tableModel;
        final JobContextMenu popupMenu = new JobContextMenu(this, tableModel);
        AppContext.primaryPopupMenu = popupMenu;
        addMouseListener(popupMenu);

        addMouseListener(new MouseAdapterJob(this, tableModel, AppContext.jobs));
        getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "stop");
        getActionMap().put("stop", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                popupMenu.stop();
            }
        });
        addKeyListener(new KeyAdapterJob(this, tableModel));
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);

        if (isRowSelected(row)) {
            return component;
        }

        applyUnselectedRowColors(component, getJobAtViewRow(row));
        return component;
    }

    private void applyUnselectedRowColors(Component component, Job job) {
        Color tableBackground = getThemeTableBackground();
        Color background = resolveBackground(job, tableBackground);
        Color foreground = resolveForeground(job, getThemeTableForeground(), background);

        component.setBackground(background);
        component.setForeground(foreground);
    }

    private Job getJobAtViewRow(int row) {
        int modelRow = convertRowIndexToModel(row);
        return (Job) jobsTableModel.getValueAt(modelRow, TableModelJobs.JOB);
    }

    private Color resolveBackground(Job job, Color tableBackground) {
        if (job.isCorrupt()) {
            return ThemeColorSupport.blend(tableBackground, COLOR_INVALID, INVALID_BACKGROUND_BLEND_RATIO);
        }
        return tableBackground;
    }

    private Color resolveForeground(Job job, Color tableForeground, Color background) {
        if (job.isCorrupt()) {
            return pickReadableForeground(background, tableForeground);
        }
        if (job.check(Job.D_DIO | Job.S_DOING)) {
            return ThemeColorSupport.ensureContrast(COLOR_DISK_IO, background, tableForeground, MINIMUM_STATE_CONTRAST);
        }
        if (job.check(Job.D_NIO | Job.S_DOING)) {
            return ThemeColorSupport.ensureContrast(
                    COLOR_NETWORK_IO, background, tableForeground, MINIMUM_STATE_CONTRAST);
        }
        if (job.check(Job.H_MISSING)) {
            Color mutedForeground =
                    ThemeColorSupport.blend(tableForeground, background, MISSING_FOREGROUND_BLEND_RATIO);
            return ThemeColorSupport.ensureContrast(
                    mutedForeground, background, tableForeground, MINIMUM_STATE_CONTRAST);
        }
        return ThemeColorSupport.ensureContrast(tableForeground, background, tableForeground, MINIMUM_NORMAL_CONTRAST);
    }

    private Color pickReadableForeground(Color background, Color preferred) {
        Color best = ThemeColorSupport.pickHighestContrast(
                background, preferred, getThemeTableForeground(), Color.black, Color.white);
        return ThemeColorSupport.ensureContrast(best, background, best, MINIMUM_NORMAL_CONTRAST);
    }

    private Color getThemeTableForeground() {
        return ThemeColorSupport.colorOrDefault(getForeground(), Color.black, "Table.foreground", "Label.foreground");
    }

    private Color getThemeTableBackground() {
        return ThemeColorSupport.colorOrDefault(getBackground(), Color.white, "Table.background", "Panel.background");
    }
}
