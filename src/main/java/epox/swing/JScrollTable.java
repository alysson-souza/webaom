/*
 * Created on 28.mai.2006 11:14:54
 * Filename: JScrollTable.java
 */
package epox.swing;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * A JScrollPane wrapper for a JTable that provides convenient methods to get visible row range.
 */
public class JScrollTable extends JScrollPane implements MouseListener {
    private final JTable table;

    public JScrollTable(JTable table) {
        super(table);
        this.table = table;
        getViewport().setBackground(java.awt.Color.white);

        setFocusable(true);
        addMouseListener(this);
    }

    public int getTopVisibleRow() {
        return table.rowAtPoint(getViewport().getViewPosition());
    }

    public int getBottomVisibleRow() {
        int yPosition = getViewport().getViewPosition().y;
        yPosition += getViewport().getExtentSize().getHeight();
        int row = table.rowAtPoint(new Point(0, yPosition));
        return (row > -1) ? row : (table.getRowCount() - 1);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        requestFocusInWindow();
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
}
