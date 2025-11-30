// Copyright (C) 2005-2006 epoximator
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

/*
 * Created on 31.08.05
 *
 * @version 	1.09
 * @author 		epoximator
 */

package epox.webaom.ui;

import epox.swing.JTableSortable;
import epox.util.ReplacementRule;
import epox.webaom.AppContext;
import epox.webaom.RuleMenu;
import epox.webaom.Rules;
import epox.webaom.WebAOM;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RulesOptionsPanel extends JPanel implements Action, ItemListener {
    protected final JTextArea rulesTextArea;
    private final JRadioButton renameRadioButton;
    private final JRadioButton moveRadioButton;
    private final JButton applyButton;

    private final transient Rules rules;

    protected JTable replacementsTable;
    protected ReplacementTableModel replacementsTableModel;

    public RulesOptionsPanel(Rules rules) {
        super(new BorderLayout());
        this.rules = rules;
        // TOP
        renameRadioButton = new JRadioButton("Renaming (name)", true);
        moveRadioButton = new JRadioButton("Moving (path)", false);
        renameRadioButton.addItemListener(this);
        moveRadioButton.addItemListener(this);

        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(renameRadioButton);
        radioButtonGroup.add(moveRadioButton);

        applyButton = new JButton("Apply!");
        applyButton.addActionListener(this);

        JPanel radioButtonPanel = new JPanel();
        radioButtonPanel.add(renameRadioButton);
        radioButtonPanel.add(moveRadioButton);
        radioButtonPanel.add(applyButton);

        rulesTextArea = new JTextArea(rules.getRenameRules());
        rulesTextArea.setMargin(new java.awt.Insets(2, 4, 2, 4));

        final RuleMenu ruleMenuHandler = new RuleMenu(rulesTextArea);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        try {
            parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            SAXParser saxParser = parserFactory.newSAXParser();
            saxParser.parse(WebAOM.class.getClassLoader().getResourceAsStream("rule-helper.xml"), ruleMenuHandler);

            rulesTextArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    super.mouseClicked(event);
                    if (event.getButton() == MouseEvent.BUTTON3) {
                        ruleMenuHandler.getMenu().show(rulesTextArea, event.getX(), event.getY());
                    }
                }
            });
            rulesTextArea.setToolTipText("Right click for menu");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(radioButtonPanel, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(rulesTextArea));
        // BOTTOM
        replacementsTableModel = new ReplacementTableModel(rules.illegalCharReplacements, "From", "To");
        replacementsTable = new JTableSortable(replacementsTableModel);
        replacementsTable.setGridColor(Color.lightGray);
        replacementsTable.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "remove");
        replacementsTable.getActionMap().put("remove", this);
        replacementsTable.getInputMap().put(KeyStroke.getKeyStroke("control UP"), "moveup");
        replacementsTable.getInputMap().put(KeyStroke.getKeyStroke("control DOWN"), "movedown");
        replacementsTable.getActionMap().put("moveup", new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                moveElement(replacementsTable, replacementsTableModel.getData(), -1);
            }
        });
        replacementsTable.getActionMap().put("movedown", new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                moveElement(replacementsTable, replacementsTableModel.getData(), 1);
            }
        });

        ReplacementTableModel.formatTable(replacementsTable);

        JScrollPane tableScrollPane = new JScrollPane(replacementsTable);
        tableScrollPane.getViewport().setBackground(Color.white);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(tableScrollPane, BorderLayout.CENTER);

        // MAIN
        setBorder(new EtchedBorder());
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setOneTouchExpandable(true);
        splitPane.add(topPanel);
        splitPane.add(bottomPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == replacementsTable) {
            removeElements(replacementsTableModel.getData(), replacementsTable.getSelectedRows());
        } else if (source == applyButton) {
            testAndApplyRules();
        }
        replacementsTable.updateUI();
    }

    public void itemStateChanged(ItemEvent event) {
        Object source = event.getSource();
        if (event.getStateChange() == ItemEvent.DESELECTED) {
            if (source == moveRadioButton) {
                rules.setMoveRules(rulesTextArea.getText());
            } else {
                rules.setRenameRules(rulesTextArea.getText());
            }
        } else if (source == moveRadioButton) {
            rulesTextArea.setText(rules.getMoveRules());
        } else {
            rulesTextArea.setText(rules.getRenameRules());
        }
    }

    public void testAndApplyRules() {
        String rulesText = rulesTextArea.getText();
        String currentLine;
        StringTokenizer lineTokenizer = new StringTokenizer(rulesText, "\r\n");
        int lineNumber = 0;
        while (lineTokenizer.hasMoreTokens()) {
            lineNumber++;
            currentLine = lineTokenizer.nextToken().toUpperCase();
            if (currentLine.charAt(0) == '#') {
                continue;
            }
            if (!currentLine.contains("DO ")) {
                AppContext.dialog("Error in script @ line" + lineNumber, "All lines must include ' DO '.");
                return;
            }
        }
        if (renameRadioButton.isSelected()) {
            rules.setRenameRules(rulesText);
        } else {
            rules.setMoveRules(rulesText);
        }
    }

    public void updateRules() {
        if (renameRadioButton.isSelected()) {
            rulesTextArea.setText(rules.getRenameRules());
        } else {
            rulesTextArea.setText(rules.getMoveRules());
        }
    }

    private void removeElements(List<ReplacementRule> dataList, int[] selectedRows) {
        Arrays.sort(selectedRows);
        for (int index = selectedRows.length - 1; index >= 0; index--) {
            if (selectedRows[index] >= dataList.size()) {
                break;
            }
            dataList.remove(selectedRows[index]);
        }
        if (dataList.isEmpty()) {
            dataList.add(new ReplacementRule("", "", false));
        }
    }

    protected void moveElement(JTable table, List<ReplacementRule> dataList, int direction) {
        int selectedIndex = table.getSelectedRow();
        int targetIndex = direction + selectedIndex;
        if (targetIndex >= dataList.size() || targetIndex < 0) {
            return;
        }
        try {
            ReplacementRule removedElement = dataList.remove(selectedIndex);
            dataList.add(targetIndex, removedElement);
            table.setRowSelectionInterval(targetIndex, targetIndex);
            table.updateUI();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Object getValue(String key) {
        return null;
    }

    public void putValue(String key, Object value) {
        // don't care
    }
}
