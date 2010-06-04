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

package vendetta.gui.splash;

import javax.swing.*;
import java.awt.*;

/**
 * Splash screen for startup progress information.
 * 
 * @version $Id: Splash.java 1520 2008-06-02 14:53:23Z frhe4063 $
 *
 */

public class Splash extends JDialog {
	private static final String TAGLINE = "Starting V for Visualization";
	private static final String TEXTURE = "./textures/splash.jpg";
	
	private JLabel bg = new JLabel();
	private final int W = 400, H = 200;
	private JLabel msg = new JLabel(TAGLINE,
			JLabel.CENTER);

	public Splash(JFrame gui) {
		super(gui, false);
		setUndecorated(true);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		// Sizes
		int w = d.width;
		int h = d.height;

		bg.setIcon(new ImageIcon(TEXTURE));
		bg.setLayout(null);
		getContentPane().add(bg);
		msg.setBounds(0, H - 30, W, 25);
		bg.add(msg);

		setSize(W, H);
		setLocation(w / 2 - W / 2, h / 2 - H / 2);
		setVisible(true);
	}

	public void setStep(String action) {
		msg.setText(action);
	}
}
