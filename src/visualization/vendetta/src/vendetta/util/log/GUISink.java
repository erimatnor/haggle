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

package vendetta.util.log;

import java.awt.Color;

import vendetta.gui.GUI;

/**
 * A sink for displaying log information in the GUI.
 * 
 * @version $Id$
 */
public class GUISink implements LogSink {
	private GUI gui;
	
	public GUISink(GUI g) {
		gui = g;
	}
	
	
	/* (non-Javadoc)
	 * @see vendetta.log.LogSink#println(java.lang.String, vendetta.log.Log.Level, java.lang.String)
	 */
	public void println(String name, Log.Level level, String msg) {
		Color c;
		
		switch (level) {
		case ERROR:
			c = Color.RED;
			gui.print(c, "ERROR ");
			break;
		case WARN:
			c = Color.YELLOW;
			gui.print(c, "WARN ");
			break;
		default:
			c = Color.WHITE;
		}
		
		gui.print(Color.GREEN, name + ":");
		gui.println(c, msg);
	}
}
