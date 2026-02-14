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

import com.sun.swing.JTreeTable;
import com.sun.swing.TreeTableModel;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobManager;
import epox.webaom.data.AniDBEntity;
import epox.webaom.data.AniDBFile;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.UIManager;

public class JobTreeTable extends JTreeTable implements RowModel, MouseListener {
    private boolean needsRowHeightCalculation = true;

    public JobTreeTable(TreeTableModel treeTableModel) {
        super(treeTableModel);
        addMouseListener(this);
    }

    @Override
    public void updateUI() {
        long elapsedTime = System.currentTimeMillis();
        super.updateUI();
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        System.out.println("@ Alt.updateUI() in " + elapsedTime + " ms. (" + AppContext.cache.stats() + ")");
    }

    @Override
    public Job[] getJobs(int row) {
        Object treeNode = tree.getPathForRow(row).getLastPathComponent();
        if (treeNode instanceof AniDBFile file) {
            if (file.getJob() != null) {
                return new Job[] {file.getJob()};
            }
        } else {
            ArrayList<Job> jobsList = new ArrayList<>();
            collectJobsRecursively(jobsList, (AniDBEntity) treeNode);
            return jobsList.toArray(new Job[0]);
        }
        return null;
    }

    private void collectJobsRecursively(ArrayList<Job> jobsList, AniDBEntity parent) {
        if (parent.size() < 1) {
            if (parent instanceof AniDBFile) {
                jobsList.add(((AniDBFile) parent).getJob());
            }
            return;
        }
        parent.buildSortedChildArray();
        for (int index = 0; index < parent.size(); index++) {
            collectJobsRecursively(jobsList, parent.get(index));
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Object treeNode = tree.getPathForRow(getSelectedRow()).getLastPathComponent();
            if (treeNode instanceof AniDBFile file) {
                if (file.getJob() != null) {
                    if (event.isAltDown()) {
                        JobManager.openInDefaultPlayer(file.getJob());
                    } else {
                        JobManager.showInfo(file.getJob());
                    }
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        // No action required
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        // No action required
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        // No action required
    }

    @Override
    public void mouseExited(MouseEvent event) {
        // No action required
    }

    private void calculateRowHeight(Graphics graphics) {
        Font font = getFont();
        FontMetrics fontMetrics = graphics.getFontMetrics(font);
        int preferredRowHeight = Math.max(fontMetrics.getHeight() + 3, UIManager.getInt("Table.rowHeight"));
        if (preferredRowHeight < 1) {
            preferredRowHeight = fontMetrics.getHeight() + 3;
        }
        setRowHeight(preferredRowHeight);
    }

    @Override
    public void paint(Graphics graphics) {
        if (needsRowHeightCalculation) {
            calculateRowHeight(graphics);
            needsRowHeightCalculation = false;
        }
        super.paint(graphics);
    }

    @Override
    public void setFont(Font font) {
        needsRowHeightCalculation = true;
        super.setFont(font);
    }
}
