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

import vendetta.MonitoredNetwork;
import vendetta.Vendetta;

import vendetta.vconfig.VSettings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;


/**
 * A tabbed panel for functionality related to network mode.
 *
 * @todo comment
 * @version $Id: NetModePanel.java 1523 2008-06-08 14:15:26Z frhe4063 $
 */
public class NetModePanel extends JTabbedPane implements ActionListener {
    private int w;
    private GUI gui;
    private MonitoredNetwork overlay;
    private boolean sleep_mode = false;
    private JPanel panel_Filter;
    private JPanel panel_Node;
    private JPanel panel_Commands;
    private JPanel panel_Testbed;
    private FilterField[] filters;
    private String[] filter_Settings;

    // additional arguments
    private JLabel nodeArgsLabel = new JLabel("Additional arguments:");
    private JLabel overlayArgsLabel = new JLabel("Additional arguments:");
    private JTextField nodeArgsText = new JTextField("");
    private JTextField overlayArgsText = new JTextField("");

    // Static stuff
    private JButton button_UpdateFilter;

    // Buttons
    private JButton[] buttons_Node;
    private JButton[] buttons_Testbed;
    private JButton[] buttons_Commands;
    private String[] cmd_Node;
    private String[] cmd_Overlay;
    private boolean[] buttons_Node_activeWithNodeSelected;
    private boolean[] buttons_Testbed_activeWithNodeSelected;
    private boolean[] buttons_Node_onlyOneExec;
    private boolean[] buttons_Testbed_onlyOneExec;

    public NetModePanel(int w, int h, VSettings vsettings, GUI gui) {
        this.w = w;
        this.gui = gui;
        overlay = Vendetta.getOverlay();
        initGraphics(vsettings);
    }

    private void initGraphics(VSettings vsettings) {
        // Settings from file:
        filter_Settings = vsettings.getSetting("LOGFILTER");

        String[][] NODECMDS = vsettings.getSettings("NODECMD");
        String[][] OVERLAYCMDS = vsettings.getSettings("TESTBEDCMD");

        int width_Panels = w - 10;

        width_Panels = w - 50;

        //setLayout( null );
        panel_Filter = new JPanel();
        panel_Filter.setLayout(null);

        panel_Node = new JPanel();
        panel_Node.setLayout(null);

        panel_Testbed = new JPanel();
        panel_Testbed.setLayout(null);

        // "Static" commands (gui commands)
        panel_Commands = new JPanel();
        panel_Commands.setLayout(null);

        addTab("Node Commands", panel_Node);
        addTab("Testbed Commands", panel_Testbed);
        addTab("V Commands", panel_Commands);
        addTab("Log Filter", panel_Filter);

        buttons_Commands = new JButton[8];
        buttons_Commands[0] = new JButton("SSH to Node");
        buttons_Commands[1] = new JButton("Show vclient log");
        buttons_Commands[2] = new JButton("Download Logfiles");
        buttons_Commands[3] = new JButton("Clear Nodes");
        buttons_Commands[4] = new JButton("Unselect Nodes");
        buttons_Commands[5] = new JButton("Toggle Sleep Mode");
        buttons_Commands[6] = new JButton("Clear Canvases");
        buttons_Commands[7] = new JButton("Remove from list");

        for (int i = 0; i < buttons_Commands.length; i++) {
            buttons_Commands[i].addActionListener(this);
            buttons_Commands[i].setBounds(10, (i * 25) + 10, width_Panels - 20,
                20);
            panel_Commands.add(buttons_Commands[i]);
        }

        // Choice boxes
        // filter = new JCheckBox[ filter_Settings.length ];
        int lastY = 20;
        filters = new FilterField[filter_Settings.length];

        for (int i = 0; i < filter_Settings.length; i++) {
            filters[i] = new FilterField(filter_Settings[i]);
            filters[i].setBounds(5, lastY, 380, 20);
            lastY += 20;
            panel_Filter.add(filters[i]);
        }

        button_UpdateFilter = new JButton("Update Log Filter");
        button_UpdateFilter.addActionListener(this);
        button_UpdateFilter.setBounds(10, 5 + lastY, width_Panels - 20, 20);
        panel_Filter.add(button_UpdateFilter);

        buttons_Node = new JButton[NODECMDS.length];
        buttons_Testbed = new JButton[OVERLAYCMDS.length];
        buttons_Node_activeWithNodeSelected = new boolean[NODECMDS.length];
        buttons_Testbed_activeWithNodeSelected = new boolean[OVERLAYCMDS.length];
        buttons_Node_onlyOneExec = new boolean[NODECMDS.length];
        buttons_Testbed_onlyOneExec = new boolean[OVERLAYCMDS.length];
        cmd_Node = new String[NODECMDS.length];
        cmd_Overlay = new String[OVERLAYCMDS.length];

        lastY = 10;

        for (int i = 0; i < OVERLAYCMDS.length; i++) {
            String label = vsettings.getSubSetting(OVERLAYCMDS[i], "label");
            String type = vsettings.getSubSetting(OVERLAYCMDS[i], "type");
            String msg = vsettings.getSubSetting(OVERLAYCMDS[i], "msg");
            String sel = vsettings.getSubSetting(OVERLAYCMDS[i], "select");

            if ((sel == null) || sel.equals("yes")) {
                buttons_Testbed_activeWithNodeSelected[i] = true;
            } else {
                buttons_Testbed_activeWithNodeSelected[i] = false;
            }

            String exe = vsettings.getSubSetting(OVERLAYCMDS[i], "onlyone");

            if ((exe == null) || exe.equals("no")) {
                buttons_Testbed_onlyOneExec[i] = false;
            } else {
                buttons_Testbed_onlyOneExec[i] = true;
            }

            cmd_Overlay[i] = "__" + type + ":" + msg;
            buttons_Testbed[i] = new JButton(label);
            buttons_Testbed[i].setBounds(10, lastY, width_Panels - 20, 20);
            lastY += 20;
            buttons_Testbed[i].addActionListener(this);
            panel_Testbed.add(buttons_Testbed[i]);
        }

        overlayArgsLabel.setBounds(10, lastY + 10, 160, 20);
        overlayArgsText.setBounds(160, lastY + 10, width_Panels - 170, 20);
        panel_Testbed.add(overlayArgsLabel);
        panel_Testbed.add(overlayArgsText);

        lastY = 10;

        for (int i = 0; i < NODECMDS.length; i++) {
            String label = vsettings.getSubSetting(NODECMDS[i], "label");
            String type = vsettings.getSubSetting(NODECMDS[i], "type");
            String msg = vsettings.getSubSetting(NODECMDS[i], "msg");
            String sel = vsettings.getSubSetting(NODECMDS[i], "select");

            if ((sel == null) || sel.equals("yes")) {
                buttons_Node_activeWithNodeSelected[i] = true;
            } else {
                buttons_Node_activeWithNodeSelected[i] = false;
            }

            String exe = vsettings.getSubSetting(NODECMDS[i], "onlyone");

            if ((exe == null) || exe.equals("no")) {
                buttons_Node_onlyOneExec[i] = false;
            } else {
                buttons_Node_onlyOneExec[i] = true;
            }

            cmd_Node[i] = "__" + type + ":" + msg;
            buttons_Node[i] = new JButton(label);
            buttons_Node[i].setBounds(10, lastY, width_Panels - 20, 20);
            lastY += 20;
            buttons_Node[i].addActionListener(this);
            panel_Node.add(buttons_Node[i]);
        }

        nodeArgsLabel.setBounds(10, lastY + 10, 160, 20);
        nodeArgsText.setBounds(160, lastY + 10, width_Panels - 170, 20);
        panel_Node.add(nodeArgsLabel);
        panel_Node.add(nodeArgsText);

        setActivated(false);
    }

    public void setActivated(boolean nodesSelected) {
        button_UpdateFilter.setEnabled(nodesSelected);

        //	button_removeNodes.setEnabled( nodesSelected );
        for (int i = 0; i < filters.length; i++)
            filters[i].setEnabled(nodesSelected);

        for (int i = 0; i < buttons_Node.length; i++)
            if (buttons_Node_activeWithNodeSelected[i]) {
                buttons_Node[i].setEnabled(nodesSelected);
            }

        for (int i = 0; i < buttons_Testbed.length; i++)
            if (buttons_Testbed_activeWithNodeSelected[i]) {
                buttons_Testbed[i].setEnabled(nodesSelected);
            }
    }

    /** Button clicked */
    public void actionPerformed(ActionEvent e) {
        // Update logfilter button
        if (e.getSource() == button_UpdateFilter) {
            updateLogFilter();
        } else if (e.getSource() == buttons_Commands[0]) {
            Vendetta.sshToNodes();
        } else if (e.getSource() == buttons_Commands[1]) {
            //			Vendetta.sshShowLog();
        } else if (e.getSource() == buttons_Commands[2]) {
            Vendetta.exec(Vendetta.sshCommand, "");
        } else if (e.getSource() == buttons_Commands[3]) { // clear
            Vendetta.clearSelectedNodes();
        } else if (e.getSource() == buttons_Commands[4]) { // unselect
            Vendetta.unselectNodes();
        } else if (e.getSource() == buttons_Commands[5]) { // toggle sleep

            if (sleep_mode) {
                sleep_mode = false;
            } else {
                sleep_mode = true;
            }

            // notify the gui
            gui.toggleSleepMode(sleep_mode);
        } else if (e.getSource() == buttons_Commands[6]) { // clear
            overlay.clearCanvases();
        } else if (e.getSource() == buttons_Commands[7]) { // remove

            int i;
            vendetta.MonitorNode[] node = Vendetta.getSelectedMonitorNodes()
                                                  .clone();

            for (i = 0; i < node.length; i++)
                Vendetta.removeMonitorNode(node[i]);
        } else {
            // One of the node CTRL-messages buttons
            for (int i = 0; i < buttons_Node.length; i++) {
                if (e.getSource() == buttons_Node[i]) {
                    String[] command = cmd_Node[i].split(":", 2);
                    String args = nodeArgsText.getText();

                    if (args.equals("")) {
                        args = null;
                    }

                    performCommand(command, args,
                        !buttons_Node_activeWithNodeSelected[i],
                        buttons_Node_onlyOneExec[i]);
                    nodeArgsText.setText("");

                    break;
                }
            }

            // One of the overlay CTRL-messages buttons
            for (int i = 0; i < buttons_Testbed.length; i++) {
                if (e.getSource() == buttons_Testbed[i]) {
                    String[] command = cmd_Overlay[i].split(":", 2);
                    String args = overlayArgsText.getText();

                    if (args.equals("")) {
                        args = null;
                    }

                    performCommand(command, args,
                        !buttons_Testbed_activeWithNodeSelected[i],
                        buttons_Testbed_onlyOneExec[i]);
                    overlayArgsText.setText("");

                    break;
                }
            }
        }
    }

    private void performCommand(String[] command, String args) {
        performCommand(command, args, false, false);
    }

    private void performCommand(String[] command, String args,
        boolean withoutnodes, boolean onlyOneExec) {
        // check the type of the button
        if (command[0].startsWith("__EXEC")) {
            // check if we had additional arguments. 
            if (args != null) {
                Vendetta.exec(command[1], args, withoutnodes, onlyOneExec);
            } else {
                Vendetta.exec(command[1], "", withoutnodes, onlyOneExec);
            }
        } else if (command[0].startsWith("__TCP")) {
            // check if we had additional arguments
            if (args != null) {
                Vendetta.tcpNodes(command[1] + " " + args);
            } else {
                Vendetta.tcpNodes(command[1]);
            }
        }
    }

    private void updateLogFilter() {
        String msg = "CTRL_LOGFILTER ";
        boolean send = false;

        for (int i = 0; i < filters.length; i++) {
            String filter = filters[i].getFilter();

            if (filter != null) {
                msg += (filter + "\n");
                send = true;
            }
        }

        if (send) {
            Vendetta.tcpNodes(msg);
        }
    }
}
