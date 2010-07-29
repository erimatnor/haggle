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

package vendetta.vconfig;

/**
 * Instances of this class hold all the settings read from a config-file
 * 
 * @todo Comment
 * @version $Id: VSettings.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class VSettings {
	private String[] marks;
	private String[][][] settings;

	public VSettings(String[] marks, int[] nrMarks) {
		this.marks = marks;
		settings = new String[marks.length][][];
		for (int i = 0; i < settings.length; i++)
			if (nrMarks[i] > 0)
				settings[i] = new String[nrMarks[i]][];
	}

	public String getSettingLine(String m) {
		for (int i = 0; i < marks.length; i++) {
			if (marks[i] == null)
				return null;
			if (settings[i][0] == null)
				return null;
			if (marks[i].equals("<" + m + ">"))
				return settings[i][0][0];
		}
		return null;
	}

	// eg: get the first "<LOGEVENT>"
	public String[] getSetting(String m) {
		for (int i = 0; i < marks.length; i++) {
			if (marks[i] == null)
				return null;
			if (marks[i].equals("<" + m + ">"))
				return settings[i][0];
		}
		return null;
	}

	// eg: get all "LOGEVENT"
	public String[][] getSettings(String m) {
		for (int i = 0; i < marks.length; i++) {
			if (marks[i] == null)
				return null;
			if (marks[i].equals("<" + m + ">"))
				return settings[i];
		}
		return null;
	}

	public String getSubSetting(String[] ss, String pattern) {
		for (int i = 0; i < ss.length; i++) {
			if (ss[i] == null)
				return null;
			String[] split = ss[i].split("=", 2);
			if (pattern.equals(split[0]))
				return split[1];
		}
		return null;
	}

	public boolean addSetting(String m, String[] s) {
		int i = 0;
		for (i = 0; i < marks.length; i++) {
			if (settings[i] == null || marks[i] == null)
				break;
			if (m.equals(marks[i])) {
				int pos = 0;
				while (settings[i][pos] != null)
					pos++;
				settings[i][pos] = s;
				return true;
			}
		}
		// New setting:
		marks[i] = m;

		return false;
	}

	public String toString() {
		String s = "";
		// Loop through all marks
		for (int i = 0; i < marks.length; i++) {
			if (settings[i] == null)
				break;
			s += marks[i] + ":\n";
			for (int j = 0; j < settings[i].length; j++) {
				s += "\t" + j + ":\n";
				for (int k = 0; k < settings[i][j].length; k++)
					s += "\t\t" + settings[i][j][k] + "\n";
			}
		}
		return s;
	}
}
