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

import vendetta.Vendetta;

import vendetta.util.log.Log;

import vendetta.vbuffer.FileBuffer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;


/**
 * A tabbed control panel for use with playing back a log file.
 *
 * @todo Comment
 * @version $Id: FileModePanel.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class FileModePanel extends JPanel implements ActionListener {
    private static final Log LOG = Log.getInstance("FileModePanel");
    private int width;
    private final JFileChooser fc = new JFileChooser();
    private JLabel label_logfile = new JLabel("Loaded File:");
    private JTextField tf_logfile = new JTextField("none");
    private JButton button_browse = new JButton("...");
    private JButton button_play = new JButton();
    private JButton button_step = new JButton();
    private JButton button_pause = new JButton();
    private JSlider slider;
    private FileBuffer buffer;
    private long startTime;
    private long endTime;
    private long duration = 1000;
    private boolean PAUSED = false;

    public FileModePanel(Color bg, int w) {
        width = w;

        setLayout(null);
        setBackground(Color.black);
        button_play.setBounds(5, 60, 50, 50);
        button_step.setBounds(60, 60, 50, 50);
        button_pause.setBounds(115, 60, 50, 50);
        button_play.setIcon(new ImageIcon("./textures/button_play.gif"));
        button_step.setIcon(new ImageIcon("./textures/button_step.gif"));
        button_pause.setIcon(new ImageIcon("./textures/button_stop.gif"));
        button_play.setToolTipText("Play");
        button_step.setToolTipText("Play until next event");
        button_pause.setToolTipText("Pause/Resume");
        button_play.setEnabled(false);
        button_step.setEnabled(false);
        button_pause.setEnabled(false);
        button_play.addActionListener(this);
        button_step.addActionListener(this);
        button_pause.addActionListener(this);
        add(button_play);
        add(button_step);
        add(button_pause);
        label_logfile.setForeground(Color.white);
        label_logfile.setBounds(5, 5, 100, 20);
        tf_logfile.setBackground(Color.black);
        tf_logfile.setForeground(Color.white);
        tf_logfile.setBounds(5, 25, 200, 20);
        button_browse.setBounds(205, 25, 30, 20);
        button_browse.addActionListener(this);
        tf_logfile.setEditable(false);
        add(label_logfile);
        add(tf_logfile);
        add(button_browse);
    }

    private void openLogFile(String file) throws Exception {
        // Try and open the file
        BufferedReader infile = new BufferedReader(new FileReader(file));

        // Read the first line and use it as a timeoffset
        String first = infile.readLine();
        String lastLine = "";
        String line = "";
        int lines = 1;

        if (first == null) {
            LOG.error("File empty");

            return;
        }

        startTime = Long.parseLong(first.split(" ")[0]);

        // Read the last line
        while ((line = infile.readLine()) != null) {
            lastLine = line;
            lines++;
        }

        endTime = Long.parseLong(lastLine.split(" ")[0]);
        duration = endTime - startTime;
        LOG.debug("#Events: " + lines + " Starttime: " + startTime +
            " Endtime: " + endTime + " duration: " + duration);

        updateSlider(duration);

        infile.close();

        if (buffer == null) {
            buffer = Vendetta.getFileBuffer();
            buffer.setFileModePanel(this, startTime, endTime);
        }

        buffer.loadFile(file);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button_browse) {
            int returnVal = fc.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String file = "none";

                try {
                    file = fc.getSelectedFile().getCanonicalPath();
                    tf_logfile.setText(file);
                    openLogFile(file);
                    button_play.setEnabled(true);
                    button_step.setEnabled(true);
                    button_pause.setEnabled(false);
                } catch (Exception exc) {
                    LOG.error("Cannot open: " + file, exc);
                    exc.printStackTrace();
                }
            }
        } else if (e.getSource() == button_pause) {
            if (!PAUSED) {
                PAUSED = true;
                buffer.pause();
            }
        } else if (e.getSource() == button_step) {
            PAUSED = false;
            button_play.setEnabled(false);
            button_step.setEnabled(false);
            button_pause.setEnabled(true);
            buffer.step(slider.getValue());
        } else if (e.getSource() == button_play) {
            PAUSED = false;
            button_play.setEnabled(false);
            button_step.setEnabled(false);
            button_pause.setEnabled(true);
            buffer.resume(slider.getValue()); // get value???
        }
    }

    public void pauseButtons() {
        button_play.setEnabled(true);
        button_step.setEnabled(true);
        button_pause.setEnabled(false);
    }

    private void updateSlider(long maxval) {
        int dur = (int) maxval;

        if (slider != null) {
            remove(slider);
        }

        slider = null;
        slider = new JSlider(JSlider.HORIZONTAL, 0, dur, 0);
        slider.setBounds(5, 120, width - 10, 50);

        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBorder(BorderFactory.createLineBorder(java.awt.Color.black));
        slider.setPaintTrack(true);
        slider.setMajorTickSpacing(dur / 4);
        slider.setMinorTickSpacing(dur / 20);

        add(slider);
    }

    public void setSliderValue(long value) {
        slider.setValue((int) value);
    }
}
