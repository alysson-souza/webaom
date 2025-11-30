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
 * Created on 12.jan.2006 10:17:46
 * Filename: TextIputDialog.java
 */
package epox.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Simple dialog for text or integer input.
 */
public class JTextInputDialog extends JDialog implements ActionListener {
	private final JTextField inputField;
	private final JButton okButton;
	private final JButton cancelButton;
	private String inputValue = null;
	private int numericValue = -1;

	public JTextInputDialog(Frame parent, String title, String defaultValue) {
		super(parent, title, true);
		inputField = new JTextField(defaultValue);
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");
		inputField.addActionListener(this);
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		JPanel inputPanel = new JPanel();
		inputPanel.add(inputField);
		add(inputPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		pack();
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle bounds = getBounds();
		int centeredX = screenSize.width / 2 - bounds.width / 2;
		int centeredY = screenSize.height / 2 - bounds.height / 2;
		setBounds(centeredX, centeredY, bounds.width, bounds.height);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == okButton || event.getSource() == inputField) {
			inputValue = inputField.getText();
			if (numericValue == 0) {
				try {
					numericValue = Integer.parseInt(inputValue.trim());
				} catch (NumberFormatException ignored) {
					numericValue = 0;
				}
				if (numericValue > 0) {
					dispose();
				} else {
					setTitle("Only positive integers allowed.");
				}
			} else {
				dispose();
			}
		} else {
			dispose();
		}
	}

	public String getStr() {
		setVisible(true);
		return numericValue == 0 ? null : inputValue;
	}

	public int getInt() {
		numericValue = 0;
		setVisible(true);
		return numericValue;
	}
}
