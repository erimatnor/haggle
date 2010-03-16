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

package vendetta.visualization.canvases;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfigTemplate;

import java.awt.Canvas;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.Timer;

import vendetta.MonitorNode;
import vendetta.Vendetta;
import vendetta.util.log.Log;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * The base class for visualization canvases.
 * 
 * All canvases must be derived from this class. If you extend this class,
 * you will have to implement several methods for handling events such as
 * a node being added/removed, etc.
 * 
 * @version $Id$
 */
public abstract class VendettaCanvas extends Canvas {
	protected static final Log LOG = Log.getInstance("Canvas");
	
	public static String SCREENSHOT_FILE = "/tmp/Capture.jpg";
	public static String SCREENSHOT_CMD = "scripts/wisenet/copy_screenshot";
	public static int SCREENSHOT_TIME = 60 * 1000;

	protected String name = "noname";
	protected int id;

	/**
	 * Is fired when the next screenshot should be taken.
	 */
	private Timer screenshotTimer;
	
	/**
	 * True when a screenshot should be taken on next postSwap()
	 */
	private boolean doScreenshotNow = false;

	/**
	 * Create a new VendettaCanvas.
	 * 
	 * @param config Configuration for the Canvas3D
	 * @param w Width of the canvas
	 * @param h Height of the canvas
	 */
	public VendettaCanvas() {
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				VendettaCanvas.this.mousePressed(e);
			}
		});
	}
	
	/**
	 * Reset the canvas to its original state, e.g. reset transforms.
	 */
	abstract public void clear();
	
	/**
	 * Called when a monitor node is added.
	 * 
	 * This is usually triggered by loading a node from a configuration
	 * file.
	 * 
	 * @param node The added node.
	 */
	abstract public void monitorNodeAdded(MonitorNode node);
	
	/**
	 * Called when a monitor node is removed.
	 * 
	 * @param node The removed node.
	 */
	abstract public void monitorNodeRemoved(MonitorNode node);
	
	/**
	 * Called when we haved received the first PING from this node.
	 * 
	 * @param node
	 */
	abstract public void monitorNodeStarted(MonitorNode node);
	
	/**
	 * Called when the monitor node went down.
	 * 
	 * @param node
	 */
	abstract public void monitorNodeStopped(MonitorNode node);
	
	/**
	 * Called when the examined application was started on a node.
	 * 
	 * @param node
	 */
	abstract public void overlayStarted(MonitorNode node);
	
	/**
	 * Called when the examined appliation was stopped on a node.
	 * 
	 * @param node
	 */
	abstract public void overlayStopped(MonitorNode node);
	
	/**
	 * 
	 * @param state
	 */
	public void toggleSleepMode(boolean state) { }

	/**
	 * Get the name of this canvas.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Handle a mouse click.
	 * 
	 * Called by the anonymous adapter created in the constructor.
	 * 
	 * @param e
	 */
	protected void mousePressed(MouseEvent e) {
	}
	
	/**
	 * Enable or disable periodic screenshots on this canvas.
	 * 
	 * @param enable
	 */
	public void setPeriodicScreenshots(boolean enable) {
		if (screenshotTimer != null && screenshotTimer.isRunning()) {
			screenshotTimer.stop();
		}

		if (enable) {
			screenshotTimer = new Timer(SCREENSHOT_TIME, new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					doScreenshotNow = true;
					VendettaCanvas.this.repaint();
				}
			});
			screenshotTimer.start();

			/*
			 * Trigger the first screenshot now ...
			 */
			doScreenshotNow = true;
			VendettaCanvas.this.repaint();
		} else {
			if (screenshotTimer != null)
				screenshotTimer.stop();
		}
	}

	public void setShowDODOLinks(boolean yes)
	{
	
	}
	public void allowUpDownRotate(boolean yes)
	{
	
	}
	public void allowLeftRightRotate(boolean yes)
	{
	
	}
}
