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
import epox.util.ReplacementRule;
import epox.webaom.AppContext;
import epox.webaom.RuleMenu;
import epox.webaom.Rules;
import epox.webaom.WebAOM;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
    private static final String DEFAULT_RENAME_RULES = new Rules().getRenameRules();
    private static final String DEFAULT_MOVE_RULES = new Rules().getMoveRules();

    protected final JTextArea rulesTextArea;
    private final JRadioButton renameRadioButton;
    private final JRadioButton moveRadioButton;
    private final JButton applyButton;

    private final transient Rules rules;

    private JRadioButton lastSelectedRadioButton;

    protected final JTable replacementsTable;
    protected final ReplacementTableModel replacementsTableModel;

    public RulesOptionsPanel(Rules rules) {
        super(new BorderLayout());
        // TOP
        renameRadioButton = new JRadioButton("Renaming (name)", true);
        moveRadioButton = new JRadioButton("Moving (path)", false);
        renameRadioButton.addItemListener(this);
        moveRadioButton.addItemListener(this);

        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(renameRadioButton);
        radioButtonGroup.add(moveRadioButton);

        applyButton = new JButton("Save");
        applyButton.addActionListener(this);

        this.rules = rules;
        this.lastSelectedRadioButton = renameRadioButton;

        JPanel radioButtonPanel = new JPanel();
        radioButtonPanel.add(renameRadioButton);
        radioButtonPanel.add(moveRadioButton);
        radioButtonPanel.add(applyButton);

        rulesTextArea = new JTextArea(rules.getRenameRules());
        rulesTextArea.setMargin(new java.awt.Insets(2, 4, 2, 4));
        Font rulesFont = rulesTextArea.getFont();
        rulesTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, rulesFont.getSize()));

        final RuleMenu ruleMenuHandler = new RuleMenu(rulesTextArea);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        try {
            parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            SAXParser saxParser = parserFactory.newSAXParser();
            saxParser.parse(WebAOM.class.getClassLoader().getResourceAsStream("rule-helper.xml"), ruleMenuHandler);

            final JPopupMenu rulesPopupMenu = ruleMenuHandler.getMenu();
            JMenuItem resetToDefaultItem = new JMenuItem("Reset to default");
            resetToDefaultItem.addActionListener(event -> resetCurrentRulesToDefault());
            rulesPopupMenu.addSeparator();
            rulesPopupMenu.add(resetToDefaultItem);

            rulesTextArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    super.mouseClicked(event);
                    if (event.getButton() == MouseEvent.BUTTON3) {
                        rulesPopupMenu.show(rulesTextArea, event.getX(), event.getY());
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
        replacementsTableModel = new ReplacementTableModel(rules.getIllegalCharReplacements(), "From", "To");
        replacementsTable = new JTableSortable(replacementsTableModel);
        replacementsTable.setGridColor(Color.lightGray);
        replacementsTable.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "remove");
        replacementsTable.getActionMap().put("remove", this);
        replacementsTable.getInputMap().put(KeyStroke.getKeyStroke("control UP"), "moveup");
        replacementsTable.getInputMap().put(KeyStroke.getKeyStroke("control DOWN"), "movedown");
        replacementsTable.getActionMap().put("moveup", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                moveElement(replacementsTable, replacementsTableModel.getData(), -1);
            }
        });
        replacementsTable.getActionMap().put("movedown", new AbstractAction() {
            @Override
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

    @Override
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == replacementsTable) {
            removeElements(replacementsTableModel.getData(), replacementsTable.getSelectedRows());
        } else if (source == applyButton) {
            testAndApplyRules();
        }
        replacementsTable.updateUI();
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getSource();
        if (event.getStateChange() == ItemEvent.DESELECTED) {
            if (lastSelectedRadioButton == moveRadioButton) {
                rules.setMoveRules(rulesTextArea.getText());
            } else if (lastSelectedRadioButton == renameRadioButton) {
                rules.setRenameRules(rulesTextArea.getText());
            }
            lastSelectedRadioButton = null;
        } else if (event.getStateChange() == ItemEvent.SELECTED) {
            if (source == moveRadioButton) {
                rulesTextArea.setText(rules.getMoveRules());
            } else {
                rulesTextArea.setText(rules.getRenameRules());
            }
            lastSelectedRadioButton = (JRadioButton) source;
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
            if (!currentLine.isEmpty() && currentLine.charAt(0) == '#') {
                continue;
            }
            if (!currentLine.isEmpty() && !currentLine.contains("DO ")) {
                AppContext.dialog("Error in script @ line" + lineNumber, "All lines must include ' DO '.");
                return;
            }
        }
        if (renameRadioButton.isSelected()) {
            rules.setRenameRules(rulesText);
        } else {
            rules.setMoveRules(rulesText);
        }
        AppContext.rules.saveToOptions(AppContext.opt);
        AppContext.opt.saveToFile();
    }

    public void updateRules() {
        if (renameRadioButton.isSelected()) {
            rulesTextArea.setText(rules.getRenameRules());
        } else {
            rulesTextArea.setText(rules.getMoveRules());
        }
    }

    private void resetCurrentRulesToDefault() {
        if (moveRadioButton.isSelected()) {
            rulesTextArea.setText(DEFAULT_MOVE_RULES);
            rules.setMoveRules(DEFAULT_MOVE_RULES);
        } else {
            rulesTextArea.setText(DEFAULT_RENAME_RULES);
            rules.setRenameRules(DEFAULT_RENAME_RULES);
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

    @Override
    public Object getValue(String key) {
        return null;
    }

    @Override
    public void putValue(String key, Object value) {
        // don't care
    }
}
