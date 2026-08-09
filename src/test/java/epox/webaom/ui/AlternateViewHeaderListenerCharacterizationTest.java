/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.data.Anime;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AlternateViewHeaderListenerCharacterizationTest {
    @Test
    void repeatedHeaderClickSortsAscendingThenDescendingAndRefreshesTree() {
        JobTreeTable treeTable = mock(JobTreeTable.class);
        JTableHeader header = mock(JTableHeader.class);
        JTable table = mock(JTable.class);
        Point point = new Point(20, 5);
        when(treeTable.getTableHeader()).thenReturn(header);
        when(header.columnAtPoint(point)).thenReturn(2);
        when(header.getTable()).thenReturn(table);
        when(table.convertColumnIndexToModel(2)).thenReturn(4);
        AlternateViewHeaderListener listener = new AlternateViewHeaderListener(treeTable);
        MouseEvent event = new MouseEvent(header, MouseEvent.MOUSE_CLICKED, 1L, 0, 20, 5, 20, 5, 1, false, 1);

        try (MockedStatic<Anime> anime = mockStatic(Anime.class)) {
            listener.mouseClicked(event);
            listener.mouseClicked(event);

            anime.verify(() -> Anime.setSortColumn(5));
            anime.verify(() -> Anime.setSortColumn(-5));
            verify(treeTable, times(2)).updateUI();
        }
        verify(header).addMouseListener(listener);
    }
}
