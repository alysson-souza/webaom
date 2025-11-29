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
    private final TableModelJobs m_jlm;

    public JTableJobs(TableModelJobs jlm) {
        super(jlm);
        m_jlm = jlm;
        final JPopupMenuM pop = new JPopupMenuM(this, jlm);
        A.com0 = pop;
        addMouseListener(pop);

        addMouseListener(new MouseAdapterJob(this, jlm, A.jobs));
        getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "stop");
        getActionMap()
                .put(
                        "stop",
                        new AbstractAction() {
                            public void actionPerformed(ActionEvent e) {
                                pop.stop();
                            }
                        });
        addKeyListener(new KeyAdapterJob(this, jlm));
    }

    private final Color c_dio = new Color(0, 102, 153);
    private final Color c_nio = new Color(182, 0, 20);
    private final Color c_mis = new Color(100, 100, 100);
    private final Color c_inv = new Color(255, 180, 180);

    public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
        Component c = super.prepareRenderer(renderer, row, col);

        if (isSelected(row)) return c;

        Job j = (Job) m_jlm.getValueAt(row, TableModelJobs.JOB);

        if (j.isCorrupt()) {
            c.setBackground(c_inv);
            c.setForeground(Color.black);
        } else {
            c.setBackground(this.getBackground());

            if (j.check(Job.D_DIO | Job.S_DOING)) c.setForeground(c_dio);
            else if (j.check(Job.D_NIO | Job.S_DOING)) c.setForeground(c_nio);
            else if (j.check(Job.H_MISSING)) c.setForeground(c_mis);
            else c.setForeground(Color.black);
        }

        return c;
    }

    private boolean isSelected(int row) {
        int[] a = getSelectedRows();
        Arrays.sort(a);
        return Arrays.binarySearch(a, row) >= 0;
    }

    public boolean upd = true;

    /*
     * public void updateUI(){
     * if(!isEditing()&&upd)
     * SwingUtilities.invokeLater(new Runnable(){
     *
     * @SuppressWarnings("synthetic-access")
     * public void run(){
     * JTableJobs.super.updateUI();
     * }
     * });
     * }
     */
    /*
     * protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
     * boolean retValue = super.processKeyBinding(ks, e, condition, pressed);
     * A.dialog("HI","HO");
     * return retValue;
     * }
     */

}
