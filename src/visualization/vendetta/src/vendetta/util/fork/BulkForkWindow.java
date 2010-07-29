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

package vendetta.util.fork;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import vendetta.gui.GUI;

/**
 * A GUI dialog for reporting progress on bulk execution of processes.
 * 
 * <p>Creating this dialog will also implicitly start the execution of
 * the processes. Before execution, `replacements' will be applied to
 * the command. A replacement is a mapping from a placeholder, e.g.
 * &lt;NODE_HOSTNAME&gt; to a list of values. Each placeholder maps
 * to the same number of values.<br /> 
 * Given a command, and an arbitrary number of replacements, where each
 * placeholder has N values, an object of this class will fork N commands,
 * where in the i-th command every placeholder is replace by it's
 * i-th value. (It's less complicated than it sounds: The command
 * "ssh &lt;NODE_HOSTNAME&gt;" will be executed once for every value of
 * &ltNODE_HOSTNAME&gt :))"
 * </p>
 * 
 * @todo Comment
 * @see BulkForkThread
 * @version $Id$
 */
public class BulkForkWindow extends JDialog {
	private final int w = 400, h = 110;
	private JProgressBar bar;
	private JPanel bg = new JPanel();
	private JLabel error_label;
	private int failed = 0;
	private int max;

	/**
	 * Create a new GUI window and start the bulk execution.
	 * 
	 * @param gui The parent GUI
	 * @param command The command to execute.
	 * @param replacements The replacements. (See class description)
	 */
	public BulkForkWindow(GUI gui, String command,
			Map<String, List<String>> replacements) {
		this(gui,command,replacements,true);
	}
	public BulkForkWindow(GUI gui, String command,
			Map<String, List<String>> replacements, boolean simultaneous) {
		super(gui, false);
		int sw = gui.width;
		int sh = gui.height;

		//		this.hostnames = hostnames;
		//		max = hostnames.length;

		// Number of hostnames provided.
		max = replacements.get("<NODE_HOSTNAME>").size();

		String[] commands = new String[max];
		int timeout = 0;
		// Do we have a timeout?
		if (command.startsWith("TIMEOUT=")) {
			String[] split = command.split(" ", 2);
			split[0] = split[0].substring(8);
			timeout = Integer.parseInt(split[0]);
			System.out.println("timeout = " + timeout);
			command = split[1];
		}

		// For each command ...
		for (int i = 0; i < max; i++) {
			commands[i] = new String(command);

			// ... iterate over all replacements ...
			for (String template : replacements.keySet()) {
				// ... and fill in the right value.
				List<String> values = replacements.get(template);
				commands[i] = commands[i].replaceAll(template, values.get(i));
			}
		}

		add(bg);
		bg.setLayout(null);
		bg.setBorder(BorderFactory.createLineBorder(Color.white, 3));
		bg.setBackground(new Color(30, 30, 30));
		setUndecorated(true);
		setLocation(sw / 2 - w / 2, sh / 2 - h / 2);
		setSize(w, h);

		JLabel label = new JLabel(command.split(" ", 2)[0]);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setBounds(0, 10, w, 20);
		label.setForeground(java.awt.Color.white);
		bg.add(label);

		bar = new JProgressBar(0, (max==1?max+1:max) - 1);
		bar.setIndeterminate(true);
		//		bar.setString( "Connecting..." );
		bar.setString("Executing ...");
		bar.setStringPainted(true);
		bar.setBounds(10, 40, w - 20, 30);
		bg.add(bar);

		error_label = new JLabel("Failed: 0");
		error_label.setHorizontalAlignment(JLabel.CENTER);
		error_label.setBounds(0, 80, w, 20);
		error_label.setForeground(java.awt.Color.white);
		bg.add(error_label);

		setVisible(true);

		// <ugly_hack>
		String[] hostnames = new String[max];
		List<String> hostnameList = replacements.get("<NODE_HOSTNAME>");
		for (int i = 0; i < max; i++) {
			hostnames[i] = hostnameList.get(i);
		}
		// </ugly_hack>
		
		if(simultaneous)
			new BulkForkThread(this, commands, hostnames, timeout);
		else
			new BulkForkOneThread(this, commands, hostnames, timeout);
	}

	/**
	 * Called when execution on a node has failed.
	 *
	 * @param id
	 */
	public void add_failed() {
		failed++;
		error_label.setForeground(java.awt.Color.red);
		error_label.setText("Failed: " + failed);
	}

	/**
	 * Update the progressbar.
	 * 
	 * @param val
	 * @param msg
	 */
	public void set_bar(int val, String msg) {
		bar.setIndeterminate(false);

		if (val == -1) {
			bar.setString(msg);
			setVisible(false);
			this.dispose();
		} else
			bar.setString(msg);
		bar.setValue(val);
		repaint();
	}
}
