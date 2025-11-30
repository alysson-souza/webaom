/*
 * Created on 09.mar.2006 15:55:57
 * Filename: JPanelAlt.java
 */
package epox.webaom.ui;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class AlternateViewPanel extends JPanel {

    private static final String ACTION_REFRESH = "refresh";

    private final JobTreeTable altViewTreeTable;
    private final JComboBox<String> sortModeComboBox;
    private final JComboBox<String> fileVisibilityComboBox;
    private final JComboBox<String> animeTitleComboBox;
    private final JComboBox<String> episodeTitleComboBox;
    private final JTextField pathRegexField;

    public AlternateViewPanel(ActionListener actionListener) {
        altViewTreeTable = createTreeTable();
        sortModeComboBox = createSortModeComboBox(actionListener);
        animeTitleComboBox = createAnimeTitleComboBox(actionListener);
        episodeTitleComboBox = createEpisodeTitleComboBox(actionListener);
        fileVisibilityComboBox = createFileVisibilityComboBox(actionListener);
        pathRegexField = createPathRegexField(actionListener);

        initializeLayout();
        initializeKeyBindings();
    }

    public JobTreeTable getAltViewTreeTable() {
        return altViewTreeTable;
    }

    public JComboBox<String> getSortModeComboBox() {
        return sortModeComboBox;
    }

    public JComboBox<String> getFileVisibilityComboBox() {
        return fileVisibilityComboBox;
    }

    public JComboBox<String> getAnimeTitleComboBox() {
        return animeTitleComboBox;
    }

    public JComboBox<String> getEpisodeTitleComboBox() {
        return episodeTitleComboBox;
    }

    public JTextField getPathRegexField() {
        return pathRegexField;
    }

    protected void updateAlternativeView(boolean rebuildTree) {
        synchronized (AppContext.animeTreeRoot) {
            if (rebuildTree) {
                AppContext.cache.rebuildTree();
            }
            altViewTreeTable.updateUI();
        }
    }

    private JobTreeTable createTreeTable() {
        AlternateViewTableModel tableModel = new AlternateViewTableModel();
        JobTreeTable treeTable = new JobTreeTable(tableModel);
        tableModel.formatTable(treeTable.getColumnModel());
        new AlternateViewHeaderListener(treeTable);
        return treeTable;
    }

    private JComboBox<String> createSortModeComboBox(ActionListener actionListener) {
        JComboBox<String> comboBox = new JComboBox<>(Cache.getSortModeLabels());
        comboBox.setSelectedIndex(Cache.getTreeSortMode());
        comboBox.setEditable(false);
        comboBox.addActionListener(actionListener);
        return comboBox;
    }

    private JComboBox<String> createAnimeTitleComboBox(ActionListener actionListener) {
        JComboBox<String> comboBox = new JComboBox<>(new String[] {"Romaji", "Kanji", "English"});
        comboBox.setEditable(false);
        comboBox.addActionListener(actionListener);
        return comboBox;
    }

    private JComboBox<String> createEpisodeTitleComboBox(ActionListener actionListener) {
        JComboBox<String> comboBox = new JComboBox<>(new String[] {"English", "Romaji", "Kanji"});
        comboBox.setEditable(false);
        comboBox.addActionListener(actionListener);
        return comboBox;
    }

    private JComboBox<String> createFileVisibilityComboBox(ActionListener actionListener) {
        JComboBox<String> comboBox =
                new JComboBox<>(new String[] {"Show all files", "Show only existing", "Show only non existing"});
        comboBox.setEditable(false);
        comboBox.addActionListener(actionListener);
        return comboBox;
    }

    private JTextField createPathRegexField(ActionListener actionListener) {
        JTextField textField = new JTextField(20);
        textField.setText(AppContext.pathRegex);
        textField.setToolTipText("Path Regexp");
        textField.addActionListener(actionListener);
        return textField;
    }

    private void initializeLayout() {
        JScrollPane scrollPane = new JScrollPane(altViewTreeTable);
        scrollPane.getViewport().setBackground(Color.white);

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
        AppContext.secondaryPopupMenu = popupMenu;
        altViewTreeTable.addMouseListener(popupMenu);
    }

    private void initializeKeyBindings() {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        String refreshKey = isMac ? "meta R" : "F5";

        altViewTreeTable.getInputMap().put(KeyStroke.getKeyStroke(refreshKey), ACTION_REFRESH);
        if (isMac) {
            altViewTreeTable.getInputMap().put(KeyStroke.getKeyStroke("F5"), ACTION_REFRESH);
        }

        altViewTreeTable.getActionMap().put(ACTION_REFRESH, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAlternativeView(false);
            }
        });

        altViewTreeTable.addKeyListener(new KeyAdapterJob(altViewTreeTable, altViewTreeTable));
    }
}
