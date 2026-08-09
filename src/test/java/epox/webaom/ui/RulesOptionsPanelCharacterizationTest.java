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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.util.ReplacementRule;
import epox.webaom.AppContext;
import epox.webaom.Options;
import epox.webaom.Rules;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JRadioButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RulesOptionsPanelCharacterizationTest {
    @TempDir
    Path temporaryDirectory;

    private Rules originalRules;
    private Options originalOptions;

    @BeforeEach
    void saveGlobals() {
        originalRules = AppContext.rules;
        originalOptions = AppContext.opt;
    }

    @AfterEach
    void restoreGlobals() {
        AppContext.rules = originalRules;
        AppContext.opt = originalOptions;
    }

    @Test
    void switchingRuleKindsPersistsTheDepartingEditorText() {
        Rules rules = new Rules();
        rules.setRenameRules("rename original");
        rules.setMoveRules("move original");
        RulesOptionsPanel panel = new RulesOptionsPanel(rules);
        JRadioButton rename = radio(panel, "Renaming (name)");
        JRadioButton move = radio(panel, "Moving (path)");

        panel.rulesTextArea.setText("rename edited");
        move.doClick();

        assertFalse(rename.isSelected());
        assertTrue(move.isSelected());
        assertEquals("rename edited", rules.getRenameRules());
        assertEquals("move original", panel.rulesTextArea.getText());

        panel.rulesTextArea.setText("move edited");
        rename.doClick();
        assertEquals("move edited", rules.getMoveRules());
        assertEquals("rename edited", panel.rulesTextArea.getText());
    }

    @Test
    void validRulesAreAppliedToTheModelAndSavedOptions() throws Exception {
        Rules rules = new Rules();
        Options options = new Options(temporaryDirectory.resolve("rules-config"));
        assertTrue(options.saveToFileUnchecked());
        AppContext.rules = rules;
        AppContext.opt = options;
        RulesOptionsPanel panel = new RulesOptionsPanel(rules);
        panel.rulesTextArea.setText("IF %ann = 'Series' DO SET 'Renamed'");

        panel.testAndApplyRules();

        assertEquals("IF %ann = 'Series' DO SET 'Renamed'", rules.getRenameRules());
        assertEquals(rules.getRenameRules(), options.getString(Options.STR_RENAME_RULES));
        assertEquals(rules.getMoveRules(), options.getString(Options.STR_MOVE_RULES));
        assertTrue(options.existsOnDisk());
    }

    @Test
    void removeActionKeepsOneEmptyReplacementRowWhenLastRuleIsDeleted() {
        Rules rules = new Rules();
        RulesOptionsPanel panel = new RulesOptionsPanel(rules);
        List<ReplacementRule> replacements = panel.replacementsTableModel.getData();
        replacements.clear();
        replacements.add(new ReplacementRule("a", "b", true));
        panel.replacementsTableModel.fireTableDataChanged();
        panel.replacementsTable.setRowSelectionInterval(0, 0);

        panel.actionPerformed(new ActionEvent(panel.replacementsTable, ActionEvent.ACTION_PERFORMED, "remove"));

        assertEquals(1, replacements.size());
        assertTrue(replacements.getFirst().hasEmptySource());
        assertFalse(replacements.getFirst().isEnabled());
    }

    @Test
    void moveActionsReorderRulesAndRetainTheMovedSelection() {
        Rules rules = new Rules();
        RulesOptionsPanel panel = new RulesOptionsPanel(rules);
        List<ReplacementRule> replacements = panel.replacementsTableModel.getData();
        replacements.clear();
        replacements.add(new ReplacementRule("first", "1", true));
        replacements.add(new ReplacementRule("second", "2", true));
        panel.replacementsTableModel.fireTableDataChanged();
        panel.replacementsTable.setRowSelectionInterval(1, 1);

        Action moveUp = panel.replacementsTable.getActionMap().get("moveup");
        moveUp.actionPerformed(new ActionEvent(panel.replacementsTable, ActionEvent.ACTION_PERFORMED, "moveup"));

        assertEquals("second", replacements.getFirst().getSource());
        assertEquals(0, panel.replacementsTable.getSelectedRow());

        moveUp.actionPerformed(new ActionEvent(panel.replacementsTable, ActionEvent.ACTION_PERFORMED, "moveup"));
        assertEquals("second", replacements.getFirst().getSource());
        assertEquals(0, panel.replacementsTable.getSelectedRow());
    }

    @Test
    void updateAndSizingMethodsReflectCurrentRulesWithoutChangingActionMetadata() {
        Rules rules = new Rules();
        RulesOptionsPanel panel = new RulesOptionsPanel(rules);
        rules.setRenameRules("updated rename");

        panel.updateRules();
        int originalWidth =
                panel.replacementsTable.getColumnModel().getColumn(1).getPreferredWidth();
        panel.scaleCurrentColumnWidths(1.5);
        panel.applyScaleAwareSizing();

        assertEquals("updated rename", panel.rulesTextArea.getText());
        assertTrue(panel.replacementsTable.getColumnModel().getColumn(1).getPreferredWidth() > 0);
        assertTrue(originalWidth > 0);
        assertNull(panel.getValue("unused"));
        panel.putValue("unused", "ignored");
    }

    private static JRadioButton radio(Container root, String text) {
        return components(root, JRadioButton.class).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow();
    }

    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container container) {
                matches.addAll(components(container, type));
            }
        }
        return matches;
    }
}
