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

import vendetta.MonitorNode;
import vendetta.Vendetta;

import vendetta.gui.splash.Splash;

import vendetta.gui.vtable.VTable;
import vendetta.gui.vtable.VTableModel;

import vendetta.monitored_network.haggle.SensorNode;

import vendetta.util.log.Log;

import vendetta.vconfig.VSettings;

import vendetta.visualization.canvases.VendettaCanvas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ColorUIResource;


/**
 * The main GUI window.
 *
 * @todo comment
 * @version $Id: GUI.java 1523 2008-06-08 14:15:26Z frhe4063 $
 */
public class GUI extends JFrame implements ActionListener,
    ListSelectionListener {
    private static final Log LOG = Log.getInstance("GUI");
    private static final int SETTINGS_WIDTH = 400;
    private static final int SETTINGS_HEIGHT = 250;
    private static final int TABLE_HEIGHT = 175;
    private static final int TEXTAREA_HEIGHT = 75;
    private VTextArea textArea = new VTextArea();
    private VendettaCanvas[] canvases;
    private VTable table;
    private VTableModel tableModel;
    private JButton button_close = new JButton("Exit");
    private JButton button_NetMode = new JButton("Net Mode");
    private JButton button_FileMode = new JButton("File Mode");

    // The Net Mode panel
    private NetModePanel panel_NetMode;

    // The File Mode panel
    private FileModePanel panel_FileMode;
    private JScrollPane scrollTable;
    public int width;
    public int height;
    private int height_Canvas;
    private boolean ignoreSelect = false;
    private Vendetta.PlaybackMode mode;
    private JPanel controlPanel;
    private JMenuBar menuBar;
    private JMenuItem miExit;
    private JMenuItem miResetViews;
    private JCheckBoxMenuItem miShowControl;
    private JCheckBoxMenuItem miShowLinks;
    private JCheckBoxMenuItem miShowForwardingDOs;
    private JCheckBoxMenuItem miCenterThisNode;
    private JCheckBoxMenuItem miDoScreenshots;
    private JCheckBoxMenuItem miDoXMLDump;
    private JCheckBoxMenuItem miShowPoorMetrics;
    private JCheckBoxMenuItem miShowClassicMetrics;

    public GUI() {
        super("Vendetta monitor");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    public void initGUI(Splash splash, MonitorNode[] nodes,
        String[] tableColNames, VSettings vsettings, Vendetta.PlaybackMode mode) {
        this.mode = mode;

        /* This window listener handles the close request
         * and shuts down Vendetta gracefully.
         */
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    Vendetta.shutdown();
                }
            });

        // Initiate window
        initMenuBar();
        setJMenuBar(menuBar);
        initWindow(nodes, tableColNames, vsettings, mode);
        splash.setStep("Loading Canvases");

        if (!initGraphics(vsettings.getSetting("CANVAS"))) {
            System.err.println("Unable to init canvases.");
            System.exit(1);
        }

        calculateBounds();

        addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    calculateBounds();
                    scrollTable.invalidate();
                    textArea.repaint();
                    repaint();
                }
            });

        setVisible(true);
    }

    public void setPanelsEnabled(boolean state) {
        if (panel_NetMode != null) {
            panel_NetMode.setActivated(state);
        }
    }

    public VendettaCanvas getCanvas(int i) {
        return canvases[i];
    }

    public int getNumCanvases() {
        return canvases.length;
    }

    public void monitorNodeStarted(MonitorNode node) {
        for (int i = 0; i < canvases.length; i++)
            canvases[i].monitorNodeStarted(node);

        tableModel.repaintRow(node.getID());
    }

    public void monitorNodeStopped(MonitorNode node) {
        for (int i = 0; i < canvases.length; i++)
            canvases[i].monitorNodeStopped(node);

        tableModel.repaintRow(node.getID());
    }

    /** Notifices the GUI that a Monitor Node has been added.
     * @param newNode the new node */
    public void monitorNodeAdded(MonitorNode newNode) {
        for (int i = 0; i < canvases.length; i++)
            canvases[i].monitorNodeAdded(newNode);

        tableModel.addMonitorNode(newNode);
    }

    /** Notifices the GUI that a Monitor Node has been removed.
     * @param newNode the node */
    public void monitorNodeRemoved(MonitorNode node) {
        for (int i = 0; i < canvases.length; i++)
            canvases[i].monitorNodeRemoved(node);

        tableModel.removeMonitorNode(node);
    }

    /** Notifices the GUI that an overlay has started on a MonitorNode.
     * @param newNode the node with the new overlay */
    public void overlayStarted(MonitorNode newNode) {
        for (int i = 0; i < canvases.length; i++)
            canvases[i].overlayStarted(newNode);

        tableModel.repaintRow(newNode.getID());
    }

    /** Notifices the GUI that an overlay has stopped on a MonitorNode.
     * @param newNode the node with the overlay */
    public void overlayStopped(MonitorNode node) {
        for (int i = 0; i < canvases.length; i++)
            canvases[i].overlayStopped(node);

        tableModel.repaintRow(node.getID());
    }

    public void toggleSleepMode(boolean state) {
        for (int i = 0; i < canvases.length; i++)
            canvases[i].toggleSleepMode(state);
    }

    /**
     * Construct and setup the menu bar.
     */
    private void initMenuBar() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menuFile);

        miExit = new JMenuItem("Exit");
        miExit.setMnemonic(KeyEvent.VK_Q);
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                ActionEvent.CTRL_MASK));
        miExit.addActionListener(this);
        menuFile.add(miExit);

        JMenu menuView = new JMenu("View");
        menuView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(menuView);

        miCenterThisNode = new JCheckBoxMenuItem("Center this node", true);
        miCenterThisNode.setMnemonic(KeyEvent.VK_C);
        miCenterThisNode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        miCenterThisNode.addActionListener(this);
        menuView.add(miCenterThisNode);

        miResetViews = new JMenuItem("Reset views");
        miResetViews.setMnemonic(KeyEvent.VK_C);
        miResetViews.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        miResetViews.addActionListener(this);
        menuView.add(miResetViews);

        miShowControl = new JCheckBoxMenuItem("Show control panel", true);
        miShowControl.setMnemonic(KeyEvent.VK_C);
        miShowControl.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        miShowControl.addActionListener(this);
        menuView.add(miShowControl);

        miShowLinks = new JCheckBoxMenuItem("Show all links", false);
        miShowLinks.setMnemonic(KeyEvent.VK_C);
        miShowLinks.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        miShowLinks.addActionListener(this);
        menuView.add(miShowLinks);

        miShowForwardingDOs = new JCheckBoxMenuItem("Hide forwarding DOs", true);
        miShowForwardingDOs.setMnemonic(KeyEvent.VK_C);
        miShowForwardingDOs.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_F9, 0));
        miShowForwardingDOs.addActionListener(this);
        menuView.add(miShowForwardingDOs);

        miDoXMLDump = new JCheckBoxMenuItem("Get XML dumps", true);
        miDoXMLDump.setMnemonic(KeyEvent.VK_C);
        miDoXMLDump.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        miDoXMLDump.addActionListener(this);
        menuView.add(miDoXMLDump);

        miShowPoorMetrics = new JCheckBoxMenuItem("Show poor metrics", true);
        miShowPoorMetrics.setMnemonic(KeyEvent.VK_C);
        miShowPoorMetrics.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_F8, 0));
        miShowPoorMetrics.addActionListener(this);
        menuView.add(miShowPoorMetrics);

        miShowClassicMetrics = new JCheckBoxMenuItem("Show classic metrics",
                true);
        miShowClassicMetrics.setMnemonic(KeyEvent.VK_C);
        miShowClassicMetrics.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_F11, 0));
        miShowClassicMetrics.addActionListener(this);
        menuView.add(miShowClassicMetrics);

        miDoScreenshots = new JCheckBoxMenuItem("Do periodic screenshots", false);
        miDoScreenshots.setMnemonic(KeyEvent.VK_S);
        miDoScreenshots.addActionListener(this);
        menuView.add(miDoScreenshots);
    }

    private void initWindow(MonitorNode[] nodes, String[] colNames,
        VSettings vsettings, Vendetta.PlaybackMode mode) {
        // Get screen resolution information
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        // Sizes
        width = (int) (d.width * 0.7);
        height = (int) (d.height * 0.6);
        setSize(width + 20, height + 40 + 20);

        height_Canvas = height - SETTINGS_HEIGHT;

        ToolTipManager.sharedInstance().setInitialDelay(1000);
        ToolTipManager.sharedInstance().setReshowDelay(1000);
        ToolTipManager.sharedInstance().setDismissDelay(60000);

        getContentPane().setLayout(null);

        UIManager.put("ScrollBar.background", new ColorUIResource(Color.black));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Viewport.background", new ColorUIResource(Color.black));

        controlPanel = new JPanel();
        controlPanel.setLayout(null);

        // Table, create the tablemodel with the settings read from file
        tableModel = new VTableModel(colNames, nodes);
        table = new VTable(tableModel);

        table.setBorder(null);
        table.setBackground(Color.black);
        table.setForeground(Color.white);
        table.setGridColor(Color.black);

        //Create the scroll pane and add the table to it.
        scrollTable = new JScrollPane(table);
        scrollTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollTable.setBorder(null);
        scrollTable.setViewportBorder(null);

        javax.swing.table.JTableHeader anHeader = table.getTableHeader();
        anHeader.setForeground(Color.black);
        anHeader.setBackground(new Color(180, 180, 180));

        // Add a row listener to the table
        table.getSelectionModel().addListSelectionListener(this);

        // Add the scroll pane to this panel.
        controlPanel.add(scrollTable);

        // Debug text area
        controlPanel.add(textArea);
        SwingUtilities.updateComponentTreeUI(textArea);

        // Buttons
        button_close.addActionListener(this);
        controlPanel.add(button_close);
        button_NetMode.addActionListener(this);
        controlPanel.add(button_NetMode);
        button_FileMode.addActionListener(this);
        controlPanel.add(button_FileMode);

        // Appearance
        float[] bg = Vendetta.bgColor;
        getContentPane().setBackground(new Color(bg[0], bg[1], bg[2]));

        // Input mode
        if (mode == Vendetta.PlaybackMode.NETWORK) {
            // Control panel for netmode
            panel_NetMode = new NetModePanel(SETTINGS_WIDTH, TABLE_HEIGHT,
                    vsettings, this);
            button_NetMode.setBackground(Color.green);
            controlPanel.add(panel_NetMode);
        } else if (mode == Vendetta.PlaybackMode.FILE) {
            // control panel for filemode
            panel_FileMode = new FileModePanel(new Color(bg[0], bg[1], bg[2]),
                    SETTINGS_WIDTH);
            button_FileMode.setBackground(Color.green);
            controlPanel.add(panel_FileMode);
            button_NetMode.setEnabled(false);
            button_FileMode.setEnabled(false);
        }

        getContentPane().add(controlPanel);
    }

    /** Initiate the graphic Canvases */
    public boolean initGraphics(String[] canvasNames) {
        //		int globeWidth, globeHeight;
        int NR_CANVASES = canvasNames.length;

        /** ------------------ */
        canvases = new VendettaCanvas[NR_CANVASES];

        for (int i = 0; i < NR_CANVASES; i++) {
            try {
                canvases[i] = (VendettaCanvas) Class.forName(canvasNames[i])
                                                    .newInstance();
                getContentPane().add(canvases[i]);
            } catch (Exception e) {
                canvases = null;
                LOG.error("Error loading Canvas: " + canvasNames[i], e);
                e.printStackTrace();

                return false;
            }
        }

        for (int i = 0; i < NR_CANVASES; i++)
            canvases[i].repaint();

        repaint();

        return true;
    }

    /** Appends a line to the debug textarea. */
    public void print(String s) {
        textArea.print(" " + s, Color.white);
    }

    public void print(Color col, String s) {
        textArea.print(" " + s, col);
    }

    public void println(String s) {
        textArea.print(" " + s + "\n", Color.white);
    }

    public void println(Color col, String s) {
        textArea.print(" " + s + "\n", col);
    }

    /** Row selected on table */
    public void valueChanged(ListSelectionEvent e) {
        //Ignore extra messages.
        if (e.getValueIsAdjusting() || ignoreSelect) {
            return;
        }

        ListSelectionModel lsm = (ListSelectionModel) e.getSource();

        if (!lsm.isSelectionEmpty()) {
            int[] selectedIndexes = table.getSelectedRows();
            int nrSelected = selectedIndexes.length;
            MonitorNode[] selected = new MonitorNode[nrSelected];

            for (int i = 0; i < nrSelected; i++) {
                //				selected[i] = tableModel.getNodeAt(table
                //				.getSortedRow(selectedIndexes[i]));
                //				selected[i] = tableModel.getNodeAt(table
                //				.
                selected[i] = tableModel.getNodeAt(selectedIndexes[i]);
            }

            Vendetta.selectNodes(selected, false);
        } else {
            // We unselected all
            Vendetta.unselectNodes();
        }
    }

    /** Button clicked */
    public void actionPerformed(ActionEvent e) {
        if ((e.getSource() == button_close) || (e.getSource() == miExit)) {
            Vendetta.shutdown();
        } else if (e.getSource() == button_FileMode) {
            if (mode == Vendetta.PlaybackMode.NETWORK) {
                //	Vendetta.flushLogFile();
                // Start a file mode V
                String[] command = { "./scripts/vendetta-file-run" };

                try {
                    // flush the logfile
                    Vendetta.flushEventLog();
                    Runtime.getRuntime().exec(command);
                } catch (Exception er) {
                    er.printStackTrace();
                }
            }
        } else if (e.getSource() == button_NetMode) {
            if (mode == Vendetta.PlaybackMode.FILE) {
                // We close this process
                System.exit(0);
            }
        } else if (e.getSource() == miShowControl) {
            /* Show or hide control panel.
             */
            if (miShowControl.getState()) {
                controlPanel.setVisible(true);

                for (int i = 0; i < canvases.length; i++) {
                    Rectangle bounds = canvases[i].getBounds();
                    bounds.height = getHeight() - 60 - SETTINGS_HEIGHT;
                    canvases[i].setBounds(bounds);
                }
            } else {
                controlPanel.setVisible(false);

                for (int i = 0; i < canvases.length; i++) {
                    Rectangle bounds = canvases[i].getBounds();
                    bounds.height = height;
                    canvases[i].setBounds(bounds);
                }
            }
        } else if (e.getSource() == miResetViews) {
            for (int i = 0; i < canvases.length; i++) {
                canvases[i].clear();
            }
        } else if (e.getSource() == miCenterThisNode) {
            for (int i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
                if (Vendetta.getMonitorNode(i) != null) {
                    Vendetta.getMonitorNode(i).setCenterThisNode(miCenterThisNode.getState());
                }
            }
        } else if (e.getSource() == miShowLinks) {
            for (int i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
                if (Vendetta.getMonitorNode(i) != null) {
                    Vendetta.getMonitorNode(i).setShowDODOLinks(miShowLinks.getState());
                }
            }
        } else if (e.getSource() == miShowForwardingDOs) {
            for (int i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
                if (Vendetta.getMonitorNode(i) != null) {
                    Vendetta.getMonitorNode(i).setHideForwardingDOs(miShowForwardingDOs.getState());
                }
            }
        } else if (e.getSource() == miDoXMLDump) {
            SensorNode.setDoXMLDump(miDoXMLDump.getState());
        } else if (e.getSource() == miShowPoorMetrics) {
            SensorNode.setShowPoorMetrics(miShowPoorMetrics.getState());
        } else if (e.getSource() == miShowClassicMetrics) {
            SensorNode.setShowClassicMetrics(miShowClassicMetrics.getState());
        } else if (e.getSource() == miDoScreenshots) {
            /* Start or stop periodic screenshots.
             */
            for (int i = 0; i < canvases.length; i++) {
                canvases[i].setPeriodicScreenshots(miDoScreenshots.getState());
            }
        }
    }

    /** A number of nodes is selected, called by Vendetta, when
     * eg someone clicks a node on the canvases */
    public void tableSelectNodes(MonitorNode[] selection) {
        ignoreSelect = true;

        ListSelectionModel sm = table.getSelectionModel();

        // We need to find the indexes of the nodes in the table
        for (int i = 0; i < selection.length; i++) {
            int index = tableModel.getNodeRow(selection[i]);
            sm.addSelectionInterval(index, index);
        }

        ignoreSelect = false;
    }

    /** Clears the selection of nodes, mainly in the table */
    public void tableUnselectNodes() {
        table.getSelectionModel().clearSelection();
    }

    public void updateTable(MonitorNode node) {
        table.repaint();
        tableModel.repaintRow(node.getID());
    }

    private void calculateBounds() {
        width = getWidth() - 20;
        height = getHeight() - 60;
        height_Canvas = height - SETTINGS_HEIGHT;

        int pos_TableY = height_Canvas;
        int width_Table = 1024 - SETTINGS_WIDTH - 30;

        controlPanel.setBounds(0, pos_TableY, width, SETTINGS_HEIGHT);
        scrollTable.setBounds(0, 0, width_Table, TABLE_HEIGHT);
        textArea.setBounds(120, TABLE_HEIGHT, width_Table - 120, TEXTAREA_HEIGHT);
        button_close.setBounds(0, SETTINGS_HEIGHT - 25, 120, 25);
        button_NetMode.setBounds(0, SETTINGS_HEIGHT - 50, 120, 25);
        button_FileMode.setBounds(0, SETTINGS_HEIGHT - 75, 120, 25);

        if (mode == Vendetta.PlaybackMode.NETWORK) {
            panel_NetMode.setBounds(width_Table + 20, 0, SETTINGS_WIDTH,
                SETTINGS_HEIGHT);
        } else {
            panel_FileMode.setBounds(width_Table + 20, 25, SETTINGS_WIDTH,
                SETTINGS_HEIGHT - 25);
        }

        if (!controlPanel.isVisible()) {
            height_Canvas = height;
        }

        int cw = width / canvases.length;

        for (int i = 0; i < canvases.length; i++)
            canvases[i].setBounds(cw * i, 0, cw, height_Canvas);
    }
}
