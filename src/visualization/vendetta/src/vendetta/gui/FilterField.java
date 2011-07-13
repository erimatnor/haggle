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

import java.awt.Choice;

import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * Panel for changing filters on a vclient.
 *
 * @todo Comment
 * @version $Id: FilterField.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class FilterField extends JPanel {
    private String LE;
    private JLabel label_type;
    private JLabel label_net = new JLabel("Net:");
    private Choice choice_net = new Choice();

    public FilterField(String filter) {
        String label = filter.split(":", 2)[0];
        LE = filter.split(":", 2)[1];
        setLayout(null);

        label_type = new JLabel(label);
        label_type.setBounds(0, 0, 150, 15);
        add(label_type);

        choice_net.add("unchanged");
        choice_net.add("tcp");
        choice_net.add("udp");
        choice_net.add("none");

        label_net.setBounds(150, 0, 40, 15);
        add(label_net);

        choice_net.setBounds(190, 0, 100, 15);
        add(choice_net);
    }

    public String getFilter() {
        if (choice_net.getSelectedItem().equals("unchanged")) {
            return null;
        } else {
            String ret = "net=" + choice_net.getSelectedItem();

            return LE + " " + ret;
        }
    }

    public void setEnabled(boolean state) {
        choice_net.setEnabled(state);
    }
}
