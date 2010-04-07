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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * A class for parsing a configuration file into a VSettings object.
 * 
 * @todo Rewrite using XML
 * @version $Id: VConfigReader.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class VConfigReader {
	private String[] marks;
	private VSettings settings;

	public VConfigReader() {
		// Hmmm
		this.marks = new String[20];
	}

	/**
	 * Parse a  configurationfile into a VSettings object.
	 * 
	 * @param path
	 * @return
	 */
	public VSettings parseFile(String path) {
		int[] nrMarks = new int[marks.length];

		// Count the number of marks in the file
		if (!countMarks(path, nrMarks))
			return null;
		settings = new VSettings(marks, nrMarks);
		try {
			FileInputStream fis = new FileInputStream(path);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis));
			String line = "";
			while ((line = in.readLine()) != null) {
				line = line.trim();
				// It should be a mark, find which one
				for (int i = 0; i < marks.length; i++) {
					if (line.equals("") || line.charAt(0) == '#')
						continue;
					// Was it this mark? (It should be some of them)
					if (line.equals(marks[i])) {
						// Subsettings for this setting
						String[] subSettings;
						// How the endmark should look like
						String endString = "</" + marks[i].substring(1);
						// We found a match on <xxx> loop until </xxx>
						// and count the number of "subsettings"
						int nrSettings = 0;
						boolean ok = false;
						// remember where we are
						in.mark(512);
						// start the counting
						while (!ok) {
							line = in.readLine();
							// Did we reach end of file or new mark?
							if (line == null) {
								System.out.println("Found EOF, expected: "
										+ endString);
								return null;
							}
							line = line.trim();
							// Ignore comments and empty lines
							if (line.equals("") || line.charAt(0) == '#')
								continue;
							// Did we reach a new mark?
							if (line.charAt(0) == '<'
									&& !line.equals(endString)) {
								System.out.println("Found: " + line
										+ ", Expected: " + endString);
								return null;
							}
							if (line.equals(endString))
								ok = true;
							else
								nrSettings++;
						}
						if (nrSettings == 0) {
							System.out.println("No subsettings for " + marks[i]
									+ " found.");
							return null;
						}
						subSettings = new String[nrSettings];
						// jump to our mark
						in.reset();
						// read each subsettings
						for (int j = 0; j < nrSettings; j++) {
							subSettings[j] = in.readLine();
							subSettings[j] = subSettings[j].trim();
						}
						// read the end mark
						in.readLine();
						settings.addSetting(marks[i], subSettings);
					}
				}
			}
			in.close();
		} catch (Exception e) {
			return null;
		}
		return settings;
	}

	/**
	 * Counts the number of marks in the file, should be used as a syntax checker.
	 */
	private boolean countMarks(String path, int[] nrMarks) {
		try {
			FileInputStream fis = new FileInputStream(path);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis));
			String line = "";
			int nr = 0;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.equals("") || line.charAt(0) != '<'
						|| line.charAt(0) == '#' || line.startsWith("</"))
					continue;
				for (int i = 0; i < marks.length; i++) {
					// Didn't we find the mark?
					if (marks[i] == null) {
						marks[i] = line;
						nr++;
					}
					if (line.equals(marks[i])) {
						nrMarks[i]++;
						break;
					}
				}
			}
			in.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
