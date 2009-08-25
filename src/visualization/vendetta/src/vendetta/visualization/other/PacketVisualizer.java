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
import java.util.Collections;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Point3f;

import vendetta.visualization.canvases.Vendetta3DCanvas;
import vendetta.visualization.shapes.MovingParticle;

/**
 * A singleton class to visualize a packet being sent from one node to another.
 * 
 * @todo Are the method addChild(), etc. in BranchGroup thread-safe?
 * @version $Id:PacketVisualizer.java 1509 2008-05-28 14:16:47Z frhe4063 $
 */
public class PacketVisualizer {
	/**
	 * The singleton instance of this class. Use getInstance() to obtain it.
	 */
	private static PacketVisualizer INSTANCE;
	
	
	/**
	 * A list of packets we're currently showing. Periodically cleaned.
	 */
	private List<MovingParticle> packets;
	
	/**
	 * The canvas we're drawing on.
	 */
	private Vendetta3DCanvas canvas;
	
	/**
	 * A branch group for all our packets.
	 */
	private BranchGroup myBranches;
	
	/**
	 * This is our garbage collector.
	 * 
	 * The class is run as its own thread and periodically scans the
	 * parent's `packets'-object for completed animations. It removes
	 * all completed packets from the screen and from the internal
	 * `packet'-list.
	 */
	private class Cleaner implements Runnable {
		boolean running = true;
		
		public void run() {
			List<MovingParticle> completed = new ArrayList<MovingParticle>();
			
			while (running) {
				completed.clear();
				
				synchronized (packets) {
					for (MovingParticle p : packets) {
						if (p.isComplete()) {
							myBranches.removeChild(p);
							completed.add(p);
						}
					}

					/* packets.remove(completed) doesn't work for some reason. -- Why?
					 */
					for (MovingParticle c : completed) {
						packets.remove(c);
					}
				}
				
				try {
					Thread.sleep(1000);
				}
				
				catch (InterruptedException ie) {
					/* Ignore.
					 */
				}
			}
		}
	}
	
	/**
	 * Private constructor. Use getInstance() to get an instance of this class.
	 * @param canvas
	 */
	private PacketVisualizer(Vendetta3DCanvas canvas) {
		packets = Collections.synchronizedList(new ArrayList<MovingParticle>());
		this.canvas = canvas;
		
		myBranches = new BranchGroup();
		myBranches.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		myBranches.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		
		canvas.addBranchGroup(myBranches);
		
		new Thread(new Cleaner()).start();
	}
	
	/**
	 * Returns one single instance of this class.
	 * 
	 * Note that on subsequent calls to getInstance(), the same canvas
	 * has to be supplied as an argument. 
	 * 
	 * @param canvas The canvas to draw on.
	 * @return
	 */
	public static PacketVisualizer getInstance(Vendetta3DCanvas canvas) {
		if (INSTANCE != null) {
			if (!INSTANCE.canvas.equals(canvas)) {
				throw new RuntimeException("Cannot re-instantiate singleton object with new parameters!");
			}
		} else {
			INSTANCE = new PacketVisualizer(canvas);
		}
		
		return INSTANCE;
	}
	
	/**
	 * Draw a nice little animation showing a packet moving from
	 * `from' to `to'.
	 * 
	 * @param from
	 * @param to
	 */
	public void showPacket(Point3f from, Point3f to) {
		MovingParticle particle = new MovingParticle(from, to, 2000);
		myBranches.addChild(particle);
		particle.start(0);
		packets.add(particle);
	}
	
	public void showPacket(Point3f from, Point3f to, long delay) {
		MovingParticle particle = new MovingParticle(from, to, 750);
		myBranches.addChild(particle);
		particle.start(delay * 750);
		packets.add(particle);
	}

}
