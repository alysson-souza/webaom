/*
 * Created on 09.mar.2006 15:55:57
 * Filename: JPanelAlt.java
 */
package epox.webaom.ui;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class AlternateViewPanel extends JPanel {
    public JobTreeTable altViewTreeTable;
    public JComboBox<String> sortModeComboBox;
    public JComboBox<String> fileVisibilityComboBox;
    public JComboBox<String> animeTitleComboBox;
    public JComboBox<String> episodeTitleComboBox;
    public JTextField pathRegexField;

    public AlternateViewPanel(ActionListener actionListener) {
        AlternateViewTableModel tableModel = new AlternateViewTableModel();
        altViewTreeTable = new JobTreeTable(tableModel);
        tableModel.formatTable(altViewTreeTable.getColumnModel());
        new AlternateViewHeaderListener(altViewTreeTable);

        JScrollPane scrollPane = new JScrollPane(altViewTreeTable);
        scrollPane.getViewport().setBackground(java.awt.Color.white);

        sortModeComboBox = new JComboBox<>(Cache.SORT_MODE_LABELS);
        sortModeComboBox.setSelectedIndex(Cache.treeSortMode);
        sortModeComboBox.setEditable(false);
        sortModeComboBox.addActionListener(actionListener);

        animeTitleComboBox = new JComboBox<>(new String[] {"Romaji", "Kanji", "English"});
        animeTitleComboBox.setEditable(false);
        animeTitleComboBox.addActionListener(actionListener);

        episodeTitleComboBox = new JComboBox<>(new String[] {"English", "Romaji", "Kanji"});
        episodeTitleComboBox.setEditable(false);
        episodeTitleComboBox.addActionListener(actionListener);

        fileVisibilityComboBox =
                new JComboBox<>(new String[] {"Show all files", "Show only existing", "Show only non existing"});
        fileVisibilityComboBox.setEditable(false);
        fileVisibilityComboBox.addActionListener(actionListener);

        pathRegexField = new JTextField(20);
        pathRegexField.setText(AppContext.preg);
        pathRegexField.setToolTipText("Path Regexp");
        pathRegexField.addActionListener(actionListener);

        JPanel southPanel = new JPanel();
        southPanel.add(animeTitleComboBox);
        southPanel.add(episodeTitleComboBox);
        southPanel.add(sortModeComboBox);
        southPanel.add(fileVisibilityComboBox);
        southPanel.add(pathRegexField);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        JobContextMenu popupMenu = new JobContextMenu(altViewTreeTable, altViewTreeTable);
        AppContext.com1 = popupMenu;
        altViewTreeTable.addMouseListener(popupMenu);

        altViewTreeTable.getInputMap().put(KeyStroke.getKeyStroke("F5"), "refresh");
        altViewTreeTable.getActionMap().put("refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                updateAlternativeView(false);
            }
        });
        altViewTreeTable.addKeyListener(new KeyAdapterJob(altViewTreeTable, altViewTreeTable));
    }

    protected void updateAlternativeView(boolean rebuildTree) {
        synchronized (AppContext.p) {
            if (rebuildTree) {
                AppContext.cache.rebuildTree();
            }
            altViewTreeTable.updateUI();
        }
    }
}
