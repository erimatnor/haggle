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

package vendetta.visualization.other;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Point3f;

import vendetta.visualization.canvases.Vendetta3DCanvas;
import vendetta.visualization.shapes.ConnectionLine;

public class ConnectionVisualizer {
	private class FaderThread extends Thread {
		boolean running = true;
		
		public void run() {
			while (running) {
				synchronized (lines) {
					for (ConnectionLine l : lines) {
						float strength = l.getTransparency();
						strength = strength>0.05f ? strength-0.05f : 0.0f;
						l.setTransparency(strength);
					}
				}
				
				try {
					Thread.sleep(300);
				} catch (InterruptedException ie) {
					/* Check if we are still running.
					 */
					continue;
				}
			}
		}
	}
	
	private class Pair {
		Point3f from, to;
		
		Pair(Point3f f, Point3f t) {
			from = f;
			to = t;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof Pair))
				return false;
			
			Pair p = (Pair) o;
			
			return (from.equals(p.from) && to.equals(p.to))
			       || (from.equals(p.to) && to.equals(p.from));
		}
	}	

	private BranchGroup myBranches;
	private List<Pair> pairs;
	private List<ConnectionLine> lines;

	public ConnectionVisualizer(Vendetta3DCanvas canvas) {
		pairs = new ArrayList<Pair>();
		lines = new ArrayList<ConnectionLine>();

		myBranches = new BranchGroup();
		myBranches.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		myBranches.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

		canvas.addBranchGroup(myBranches);
		
		new Thread(new FaderThread()).start();
	}
	
	public void packetReceived(Point3f from, Point3f to) {
		Pair p = new Pair(from, to);
		
		int idx = pairs.indexOf(p);
		if (idx != -1) {
			ConnectionLine l = lines.get(idx);
			l.setTransparency(1.0f);
		} else {
			ConnectionLine l = new ConnectionLine(from, to);
			l.setTransparency(1.0f);
			myBranches.addChild(l);
			
			synchronized (lines) {
				pairs.add(p);
				lines.add(l);
			}
		}
	}
}
