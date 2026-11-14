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

package epox.swing;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Table cell renderer that shortens text from the start instead of the end, so the most specific part of a path (the
 * filename) stays visible in a narrow column.
 */
public class LeadingEllipsisCellRenderer extends DefaultTableCellRenderer {
    private static final String ELLIPSIS = "…";

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String text = value == null ? "" : value.toString();
        Insets insets = getInsets();
        // The cell excludes the margin between columns, so measure it rather than the column
        int availableWidth = table.getCellRect(row, column, false).width - insets.left - insets.right;
        setText(keepTail(text, getFontMetrics(getFont()), availableWidth));
        return this;
    }

    /** Returns {@code text} unchanged when it fits, otherwise an ellipsis followed by the longest fitting tail. */
    static String keepTail(String text, FontMetrics metrics, int availableWidth) {
        if (text.isEmpty() || metrics.stringWidth(text) <= availableWidth) {
            return text;
        }
        int ellipsisWidth = metrics.stringWidth(ELLIPSIS);
        int low = 1;
        int high = text.length();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (ellipsisWidth + metrics.stringWidth(text.substring(middle)) <= availableWidth) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        // Starting at a low surrogate would cut a supplementary character in half
        if (low < text.length() && Character.isLowSurrogate(text.charAt(low))) {
            low++;
        }
        return ELLIPSIS + text.substring(low);
    }
}
