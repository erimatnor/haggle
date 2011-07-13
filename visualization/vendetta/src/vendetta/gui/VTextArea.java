/* Copyright (c) 2008 Uppsala Universitet.
 * All rights reserved.
 *
 * This file is part of Vendetta.
 *
 * Vendetta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Vendetta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vendetta.  If not, see <http://www.gnu.org/licenses/>.
 */
package vendetta.gui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


/**
 * A scrolling text area for log information.
 *
 * @todo Comment
 * @version $Id: VTextArea.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
class VTextArea extends JScrollPane {
    private JTextPane pane;
    private StyledDocument doc;

    public VTextArea() {
        super();

        // Make document
        doc = new DefaultStyledDocument();

        // Make pane
        pane = new JTextPane(doc);
        pane.setBorder(null);
        setBorder(null);
        pane.setBackground(Color.black);
        pane.setEditable(false);
        pane.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Set Viewport
        setViewportView(pane);
    }

    public void print(String text, Color color) {
        MutableAttributeSet mas = new SimpleAttributeSet();
        StyleConstants.setForeground(mas, color);

        StyledDocument sd = pane.getStyledDocument();

        try {
            sd.insertString(sd.getLength(), text, mas);
            pane.setCaretPosition(sd.getLength());
            pane.repaint();
        } catch (Error e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            e.printStackTrace();
        }
    }

    public void clear() {
        pane.setText("");
        print("", Color.white);
    }

    public void scrollUp() {
        pane.setCaretPosition(0);
    }
}
