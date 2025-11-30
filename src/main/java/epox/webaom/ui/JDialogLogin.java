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
 * Created on 22.01.05
 *
 * @version 	1.05, 1.03
 * @author 		epoximator
 */
package epox.webaom.ui;

import epox.util.U;
import epox.util.UserPass;
import epox.webaom.A;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class JDialogLogin extends JDialog implements ActionListener {
	private boolean success = false;
	private JTextField usernameField;
	private JTextField apiPassField;
	private JPasswordField passwordField;
	private JButton okButton;
	private String usernameText;
	private String passwordText;
	private String apiPassText;

	public JDialogLogin() {
		super(A.frame, "Enter your AniDB username and password.", true);
		init();
	}

	private void init() {
		usernameField = new JTextField(A.up.usr, 20);
		passwordField = new JPasswordField(A.up.psw, 20);
		apiPassField = new JTextField(A.up.key, 20);
		apiPassField.setToolTipText("Use blank if you don't care about encryption");
		okButton = new JButton("OK");
		okButton.setToolTipText("<html>Login is required to access the <i>AniDB UDP Service</i> which"
				+ " enables<br>WebAOM to identify files and add them to your MyList.</html>");

		usernameField.addActionListener(this);
		passwordField.addActionListener(this);
		apiPassField.addActionListener(this);
		okButton.addActionListener(this);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new GridBagLayout());
		contentPanel.setBorder(new EmptyBorder(2, 0, 2, 0));

		GridBagConstraints gridConstraints = new GridBagConstraints();
		gridConstraints.insets = new Insets(2, 4, 2, 4);
		gridConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridConstraints.anchor = GridBagConstraints.CENTER;
		gridConstraints.weighty = 1.0;

		gridConstraints.gridwidth = GridBagConstraints.RELATIVE;
		gridConstraints.weightx = 0.2;
		contentPanel.add(new JLabel("Username:"), gridConstraints);

		gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridConstraints.weightx = 0.8;
		contentPanel.add(usernameField, gridConstraints);

		gridConstraints.gridwidth = GridBagConstraints.RELATIVE;
		gridConstraints.weightx = 0.2;
		contentPanel.add(new JLabel("Password:"), gridConstraints);

		gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridConstraints.weightx = 0.8;
		contentPanel.add(passwordField, gridConstraints);

		gridConstraints.gridwidth = GridBagConstraints.RELATIVE;
		gridConstraints.weightx = 0.2;
		contentPanel.add(new JLabel("ApiPass (optional):"), gridConstraints);

		gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridConstraints.weightx = 0.8;
		contentPanel.add(apiPassField, gridConstraints);

		gridConstraints.weightx = 1.0;
		contentPanel.add(okButton, gridConstraints);

		setContentPane(contentPanel);
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle dialogBounds = this.getBounds();
		int centeredX = screenSize.width / 2 - dialogBounds.width / 2;
		int centeredY = screenSize.height / 2 - dialogBounds.height / 2;
		setBounds(centeredX, centeredY, dialogBounds.width, dialogBounds.height);

		if (!usernameField.getText().isEmpty()) {
			passwordField.requestFocus();
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		passwordText = new String(passwordField.getPassword());
		usernameText = usernameField.getText().toLowerCase();
		apiPassText = apiPassField.getText();
		if (usernameText.length() < 3) {
			okButton.setText("Username too short - OK");
		} else if (usernameText.length() > 16) {
			okButton.setText("Username too long - OK");
		} else if (!U.alfanum(usernameText)) {
			okButton.setText("Only letters and digits - OK");
		} else if (passwordText.length() < 4) {
			okButton.setText("Password too short - OK");
		} else {
			A.up.usr = usernameText;
			A.up.psw = passwordText;
			A.up.key = apiPassText;
			success = true;
			dispose();
		}
	}

	public UserPass getPass() {
		setVisible(true);
		if (success) {
			return new UserPass(usernameText, passwordText, apiPassText);
		}
		return null;
	}
}
