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
 * Created on 10.06.05
 *
 * @version 	1.07
 * @author 		epoximator
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

    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            AppContext.gui.openHyperlink(event.getDescription());
        }
    }

    public void keyTyped(KeyEvent event) {
        if (event.getKeyChar() == 'q') {
            dispose();
        }
    }

    public void keyPressed(KeyEvent event) {
        // No action needed on key press
    }

    public void keyReleased(KeyEvent event) {
        // No action needed on key release
    }
}
