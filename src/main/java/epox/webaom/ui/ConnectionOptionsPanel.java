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
import epox.webaom.net.AniDBConnection;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ConnectionOptionsPanel extends JPanel implements ChangeListener {
    private static final int MIN_INTER_PACKET_DELAY = 3;
    private static final int MIN_PACKET_TIMEOUT = 10;
    private final JSlider timeoutSlider;
    private final JSlider delaySlider;
    private final JCheckBox natKeepAliveCheckbox;
    public final JButton pingButton;
    public final JTextField hostTextField;
    public final JTextField remotePortTextField;
    public final JTextField localPortTextField;

    public ConnectionOptionsPanel() {
        super(new GridBagLayout());

        timeoutSlider = new JSlider(MIN_PACKET_TIMEOUT, 60, 20);
        timeoutSlider.setMajorTickSpacing(10);
        timeoutSlider.setPaintLabels(true);
        timeoutSlider.addChangeListener(this);

        delaySlider = new JSlider(MIN_INTER_PACKET_DELAY, 10, 4);
        delaySlider.setMajorTickSpacing(1);
        delaySlider.setPaintLabels(true);
        delaySlider.setSnapToTicks(true);
        delaySlider.setToolTipText("Delay between each datagram sent to server.");

        pingButton = new JButton("Ping AniDB");

        hostTextField = new JTextField(AniDBConnection.DEFAULT_HOST);
        remotePortTextField = new JTextField("" + AniDBConnection.DEFAULT_REMOTE_PORT);
        localPortTextField = new JTextField("" + AniDBConnection.DEFAULT_LOCAL_PORT);

        KeyAdapter numericOnlyAdapter = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) {
                    e.consume();
                }
            }
        };
        remotePortTextField.addKeyListener(numericOnlyAdapter);
        localPortTextField.addKeyListener(numericOnlyAdapter);

        natKeepAliveCheckbox = new JCheckBox("Keep-Alive");

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 4, 2, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 0.0;

        add("AniDB Host", hostTextField, constraints);
        add("Remote Port", remotePortTextField, constraints);

        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        add(new JLabel("Local Port"), constraints);
        constraints.weightx = 1.0;
        add(localPortTextField, constraints);
        constraints.weightx = 0.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        add(natKeepAliveCheckbox, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;

        add("Delay (sec)", delaySlider, constraints);
        add("Timeout (sec)", timeoutSlider, constraints);

        constraints.insets = new Insets(8, 4, 2, 4);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        add(pingButton, constraints);

        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = GridBagConstraints.RELATIVE;
        fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fillerConstraints.weightx = 1.0;
        fillerConstraints.weighty = 1.0;
        fillerConstraints.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), fillerConstraints);
    }

    private void add(String label, Component component, GridBagConstraints constraints) {
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        add(new JLabel(label), constraints);
        constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(component, constraints);
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        timeoutSlider.setToolTipText(timeoutSlider.getValue() + " sec");
    }

    public int getTimeout() {
        return timeoutSlider.getValue();
    }

    public int getDelayMillis() {
        return delaySlider.getValue() * 1000;
    }

    public boolean isNatKeepAliveEnabled() {
        return natKeepAliveCheckbox.isSelected();
    }

    @Override
    public void setEnabled(boolean enabled) {
        hostTextField.setEnabled(enabled);
        localPortTextField.setEnabled(enabled);
        remotePortTextField.setEnabled(enabled);
        pingButton.setEnabled(enabled);
        timeoutSlider.setEnabled(enabled);
        delaySlider.setEnabled(enabled);
    }

    public void saveOptions(Options options) {
        options.setString(Options.STR_HOST_URL, hostTextField.getText());
        options.setInteger(Options.INT_REMOTE_PORT, Integer.parseInt(remotePortTextField.getText()));
        options.setInteger(Options.INT_LOCAL_PORT, Integer.parseInt(localPortTextField.getText()));
        options.setInteger(Options.INT_TIMEOUT, timeoutSlider.getValue());
        options.setInteger(Options.INT_DATAGRAM_DELAY, delaySlider.getValue());
        options.setBoolean(Options.BOOL_NAT_KEEP_ALIVE, natKeepAliveCheckbox.isSelected());
    }

    public void loadOptions(Options options) {
        remotePortTextField.setText("" + options.getInteger(Options.INT_REMOTE_PORT));
        localPortTextField.setText("" + options.getInteger(Options.INT_LOCAL_PORT));
        hostTextField.setText(options.getString(Options.STR_HOST_URL));

        int timeout = options.getInteger(Options.INT_TIMEOUT);
        if (timeout < MIN_PACKET_TIMEOUT) {
            timeout = MIN_PACKET_TIMEOUT;
        } else if (timeout > 60) {
            timeout = 60;
        }
        timeoutSlider.setValue(timeout);

        int delay = options.getInteger(Options.INT_DATAGRAM_DELAY);
        if (delay < MIN_INTER_PACKET_DELAY) {
            delay = MIN_INTER_PACKET_DELAY;
        } else if (delay > 10) {
            delay = 10;
        }
        delaySlider.setValue(delay);

        natKeepAliveCheckbox.setSelected(options.getBoolean(Options.BOOL_NAT_KEEP_ALIVE));
    }
}
