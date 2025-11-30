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
 * Created on 21.nov.2005 15:22:46
 * Filename: ChiiEmu.java
 */
package epox.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Panel with a command input combo box and output text area. Used for command-line style interfaces.
 */
public class JPanelCommand extends JPanel implements ActionListener, Log {
    private final JTextArea outputArea;
    private final JComboBox<String> commandComboBox;

    private final transient CommandModel commandModel;

    public JPanelCommand(CommandModel commandModel, String initialText) {
        this.commandModel = commandModel;

        outputArea = new JTextArea(initialText);
        outputArea.setMargin(new java.awt.Insets(2, 2, 2, 2));
        commandComboBox = new JComboBox<>();
        commandComboBox.setBackground(Color.white);

        JScrollPane scrollPane = new JScrollPane(outputArea);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(commandComboBox, BorderLayout.SOUTH);

        commandComboBox.addActionListener(this);
        commandComboBox.setEditable(true);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("comboBoxEdited")) {
            handleCommand(commandComboBox.getSelectedItem().toString().trim());
        }
    }

    private void handleCommand(String command) {
        commandComboBox.removeItem(command);
        if (command.isEmpty()) {
            return;
        }
        commandComboBox.insertItemAt(command, 0);
        commandComboBox.setSelectedItem("");
        println(command);
        commandModel.handleCommand(command);
    }

    @Override
    public void println(Object message) {
        outputArea.append(message + "\r\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    @Override
    public void status0(String message) {
        println(message);
    }

    @Override
    public void status1(String message) {
        println(message);
    }
}
