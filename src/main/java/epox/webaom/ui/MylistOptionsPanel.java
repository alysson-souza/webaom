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

import epox.webaom.Options;
import epox.webaom.data.Mylist;
import epox.webaom.data.MylistStates;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MylistOptionsPanel extends JPanel {
    private static final String[] FILE_STATES = {
        MylistStates.toLocationDisplayName(MylistStates.LOCATION_UNKNOWN),
        MylistStates.toLocationDisplayName(MylistStates.LOCATION_HDD),
        MylistStates.toLocationDisplayName(MylistStates.LOCATION_CD),
        MylistStates.toLocationDisplayName(MylistStates.LOCATION_DELETED),
        MylistStates.toLocationDisplayName(MylistStates.LOCATION_REMOTE)
    };
    private final JTextField storageField;
    private final JTextField sourceField;
    private final JComboBox<String> stateComboBox;
    private final JTextArea otherInfoArea;
    private final JCheckBox watchedCheckBox;

    public MylistOptionsPanel() {
        storageField = new JTextField();
        sourceField = new JTextField();
        otherInfoArea = new JTextArea();

        stateComboBox = new JComboBox<>(FILE_STATES);
        stateComboBox.setSelectedIndex(1);

        watchedCheckBox = new JCheckBox("Watched", false);

        GridBagConstraints gridConstraints = new GridBagConstraints();
        gridConstraints.insets = new Insets(4, 4, 4, 4);
        setLayout(new GridBagLayout());
        gridConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridConstraints.anchor = GridBagConstraints.WEST;

        gridConstraints.weightx = 0.1;
        gridConstraints.gridwidth = 1;
        add(new JLabel("State"), gridConstraints);

        gridConstraints.weightx = 1.0;
        gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        add(stateComboBox, gridConstraints);

        gridConstraints.weightx = 0.1;
        gridConstraints.gridwidth = 1;
        add(new JLabel("Source"), gridConstraints);

        gridConstraints.weightx = 1.0;
        gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        add(sourceField, gridConstraints);

        gridConstraints.weightx = 0.1;
        gridConstraints.gridwidth = 1;
        add(new JLabel("Storage"), gridConstraints);

        gridConstraints.weightx = 1.0;
        gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        add(storageField, gridConstraints);

        gridConstraints.weightx = 0.1;
        gridConstraints.gridwidth = 1;
        add(new JLabel("Other"), gridConstraints);

        gridConstraints.gridheight = 2;
        gridConstraints.weightx = 1.0;
        gridConstraints.weighty = 1.0;
        gridConstraints.fill = GridBagConstraints.BOTH;
        gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        add(new JScrollPane(otherInfoArea), gridConstraints);

        gridConstraints.gridheight = 1;
        gridConstraints.weightx = 0.1;
        gridConstraints.weighty = 0.0;
        gridConstraints.fill = GridBagConstraints.HORIZONTAL;

        gridConstraints.gridx = 0;
        gridConstraints.gridy = 4;
        gridConstraints.gridheight = 1;
        gridConstraints.gridwidth = 1;
        add(watchedCheckBox, gridConstraints);
    }

    public Mylist getMylistData() {
        Mylist mylist = new Mylist();
        int selectedState = stateComboBox.getSelectedIndex();
        if (MylistStates.isValidLocation(selectedState)) {
            mylist.state = selectedState;
        }
        mylist.viewed = watchedCheckBox.isSelected() ? 1 : 0;
        mylist.storage = storageField.getText();
        mylist.source = sourceField.getText();
        mylist.other = otherInfoArea.getText();
        return mylist;
    }

    @Override
    public void setEnabled(boolean enabled) {
        watchedCheckBox.setEnabled(enabled);
        stateComboBox.setEnabled(enabled);
        otherInfoArea.setEnabled(enabled);
        sourceField.setEnabled(enabled);
        storageField.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    /** Saves the current UI values to the options model. */
    public void saveToOptions(Options options) {
        options.setBoolean(Options.BOOL_WATCHED, watchedCheckBox.isSelected());
        options.setInteger(Options.INT_FILE_STATE, stateComboBox.getSelectedIndex());
        options.setString(Options.STR_STORAGE, storageField.getText());
        options.setString(Options.STR_SOURCE_FOLDER, sourceField.getText());
        options.setString(Options.STR_OTHER_INFO, otherInfoArea.getText());
    }

    /** Loads values from the options model into the UI components. */
    public void loadFromOptions(Options options) {
        watchedCheckBox.setSelected(options.getBoolean(Options.BOOL_WATCHED));
        storageField.setText(options.getString(Options.STR_STORAGE));
        sourceField.setText(options.getString(Options.STR_SOURCE_FOLDER));
        otherInfoArea.setText(options.getString(Options.STR_OTHER_INFO));
        stateComboBox.setSelectedIndex(options.getInteger(Options.INT_FILE_STATE));
    }
}
