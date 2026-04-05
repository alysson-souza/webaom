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

import epox.swing.layout.DisplayEnvironment;
import epox.swing.layout.WindowLayoutPolicy;
import epox.swing.layout.WindowLayoutSupport;
import epox.webaom.AppContext;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class JFrameHtml extends JFrame implements HyperlinkListener {
    private static final DisplayEnvironment DISPLAY_ENVIRONMENT = DisplayEnvironment.current();
    private static final WindowLayoutPolicy WINDOW_LAYOUT_POLICY = new WindowLayoutPolicy();

    public JFrameHtml(String title, String text) {
        super(title);

        JEditorPane htmlEditorPane = new JEditorPane("text/html", text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics;
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                super.paintComponent(graphics2D);
            }
        };
        htmlEditorPane.setEditable(false);
        htmlEditorPane.addHyperlinkListener(this);

        getContentPane().add(new JScrollPane(htmlEditorPane));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        DialogHelper.bindEscapeToClose(getRootPane(), this::dispose);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('q'), "closeHtmlFrame");
        getRootPane().getActionMap().put("closeHtmlFrame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        pack();
        WindowLayoutSupport.placeCentered(
                this,
                AppContext.frame,
                DISPLAY_ENVIRONMENT,
                WINDOW_LAYOUT_POLICY,
                new Dimension(getPreferredSize()),
                false);
        setVisible(true);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            AppContext.gui.openHyperlink(event.getDescription());
        }
    }
}
