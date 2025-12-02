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

import epox.webaom.AppContext;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class JFrameHtml extends JFrame implements HyperlinkListener, KeyListener {
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
        pack();
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle frameBounds = getBounds();
        int maxWidth = (int) (0.95 * screenSize.width);
        int maxHeight = (int) (0.95 * screenSize.height);
        if (frameBounds.width > maxWidth) {
            frameBounds.width = maxWidth;
        }
        if (frameBounds.height > maxHeight) {
            frameBounds.height = maxHeight;
        }
        setBounds(
                screenSize.width / 2 - frameBounds.width / 2,
                screenSize.height / 2 - frameBounds.height / 2,
                frameBounds.width,
                frameBounds.height);
        setVisible(true);
        htmlEditorPane.addKeyListener(this);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            AppContext.gui.openHyperlink(event.getDescription());
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
        if (event.getKeyChar() == 'q') {
            dispose();
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        // No action needed on key press
    }

    @Override
    public void keyReleased(KeyEvent event) {
        // No action needed on key release
    }
}
