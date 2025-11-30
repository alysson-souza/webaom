/*
 * Created on 09.mar.2006 15:55:57
 * Filename: JPanelAlt.java
 */
package epox.webaom.ui;

import epox.webaom.A;
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

public class JPanelAlt extends JPanel {
	public JTreeTableR altViewTreeTable;
	public JComboBox sortModeComboBox;
	public JComboBox fileVisibilityComboBox;
	public JComboBox animeTitleComboBox;
	public JComboBox episodeTitleComboBox;
	public JTextField pathRegexField;

	public JPanelAlt(ActionListener actionListener) {
		TableModelAlt tableModel = new TableModelAlt();
		altViewTreeTable = new JTreeTableR(tableModel);
		tableModel.formatTable(altViewTreeTable.getColumnModel());
		new HeaderListenerAlt(altViewTreeTable);

		JScrollPane scrollPane = new JScrollPane(altViewTreeTable);
		scrollPane.getViewport().setBackground(java.awt.Color.white);

		sortModeComboBox = new JComboBox(Cache.SORT_MODE_LABELS);
		sortModeComboBox.setSelectedIndex(Cache.treeSortMode);
		sortModeComboBox.setEditable(false);
		sortModeComboBox.addActionListener(actionListener);

		animeTitleComboBox = new JComboBox(new String[]{"Romaji", "Kanji", "English"});
		animeTitleComboBox.setEditable(false);
		animeTitleComboBox.addActionListener(actionListener);

		episodeTitleComboBox = new JComboBox(new String[]{"English", "Romaji", "Kanji"});
		episodeTitleComboBox.setEditable(false);
		episodeTitleComboBox.addActionListener(actionListener);

		fileVisibilityComboBox = new JComboBox(
				new String[]{"Show all files", "Show only existing", "Show only non existing"});
		fileVisibilityComboBox.setEditable(false);
		fileVisibilityComboBox.addActionListener(actionListener);

		pathRegexField = new JTextField(20);
		pathRegexField.setText(A.preg);
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

		JPopupMenuM popupMenu = new JPopupMenuM(altViewTreeTable, altViewTreeTable);
		A.com1 = popupMenu;
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
		synchronized (A.p) {
			if (rebuildTree) {
				A.cache.rebuildTree();
			}
			altViewTreeTable.updateUI();
		}
	}
}
