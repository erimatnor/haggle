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
import javax.media.j3d.GraphicsConfigTemplate3D;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsContext3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.PickRay;
import javax.media.j3d.Raster;
import javax.media.j3d.SceneGraphPath;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.Timer;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import vendetta.MonitorNode;
import vendetta.Vendetta;
import vendetta.util.log.Log;
import vendetta.visualization.shapes.NodeSphere;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * The base class for visualization canvases.
 * 
 * All canvases must be derived from this class. If you extend this class,
 * you will have to implement several methods for handling events such as
 * a node being added/removed, etc.
 * 
 * @version $Id$
 */
public abstract class Vendetta3DCanvas extends Canvas3D {
	protected static final Log LOG = Log.getInstance("Canvas");
	protected final static BoundingSphere INF =
		new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 1000.0f);
	
	public static String SCREENSHOT_FILE = "/tmp/Capture.jpg";
	public static String SCREENSHOT_CMD = "scripts/wisenet/copy_screenshot";
	public static int SCREENSHOT_TIME = 60 * 1000;

	protected SimpleUniverse su;
	protected BranchGroup branchRoot = null;
	protected TransformGroup objTrans;
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
	 * Create a new Vendetta3DCanvas.
	 * 
	 * @param config Configuration for the Canvas3D
	 * @param w Width of the canvas
	 * @param h Height of the canvas
	 */
	public Vendetta3DCanvas() {
		super(GraphicsEnvironment.getLocalGraphicsEnvironment().
		      getDefaultScreenDevice().getBestConfiguration(new GraphicsConfigTemplate3D()));
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Vendetta3DCanvas.this.mousePressed(e);
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
	 * Create the scene graph and root for this canvas.
	 *  
	 * @param id
	 */
	protected void createSceneGraph(int id) {
		if (branchRoot != null) {
			LOG.error("BranchRoot already exists");
			return;
		}
		this.id = id;

		branchRoot = new BranchGroup();

		Background vBackground = new Background(new Color3f(Vendetta.bgColor));
		vBackground.setApplicationBounds(INF);
		branchRoot.addChild(vBackground);
	}

	/**
	 * Create and initiate the universe and scenegraph.
	 */
	public void createUniverse(int id) {
		su = new SimpleUniverse(this);
		LOG.info("Creating SceneGraph: " + name);
		createSceneGraph(id);
	}

	/**
	 * Get the name of this canvas.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Adds a BranchGroup to the scenegraph, eg a line representing a lookup.
	 */
	public void addBranchGroup(BranchGroup bg) {
		objTrans.addChild(bg);
	}

	/**
	 * Remove a BranchGroup from the scenegraph.
	 * @param bg
	 */
	public void removeBranchGroup(BranchGroup bg) {
		objTrans.removeChild(bg);
	}

	/**
	 * Handle a mouse click.
	 * 
	 * Called by the anonymous adapter created in the constructor.
	 * 
	 * @param e
	 */
	protected void mousePressed(MouseEvent e) {
		Point3d eyePos = new Point3d();
		Point3d mousePos = new Point3d();

		getCenterEyeInImagePlate(eyePos);
		getPixelLocationInImagePlate(e.getX(), e.getY(), mousePos);

		Transform3D motion = new Transform3D();
		getImagePlateToVworld(motion);
		motion.transform(eyePos);
		motion.transform(mousePos);

		Vector3d direction = new Vector3d(mousePos);
		direction.sub(eyePos);

		SceneGraphPath[] paths = branchRoot.pickAll(new PickRay(eyePos,
				direction));

		if (paths != null) {
			/*
			 * Only select one to avoid problems with dragging spheres that lie
			 * ontop of each other.
			 */
			MonitorNode[] selected = new MonitorNode[1];
			NodeSphere picked = (NodeSphere) paths[0].getNode(0);
			selected[0] = picked.getOwner();
			Vendetta.selectNodes(selected, true);
		}
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
					Vendetta3DCanvas.this.repaint();
				}
			});
			screenshotTimer.start();

			/*
			 * Trigger the first screenshot now ...
			 */
			doScreenshotNow = true;
			Vendetta3DCanvas.this.repaint();
		} else {
			if (screenshotTimer != null)
				screenshotTimer.stop();
		}
	}

	/* (non-Javadoc)
	 * @see javax.media.j3d.Canvas3D#postSwap()
	 */
	public void postSwap() {
		if (doScreenshotNow && SCREENSHOT_FILE != null) {
			int width = this.getWidth();
			int height = this.getHeight();

			GraphicsContext3D ctx = getGraphicsContext3D();
			// The raster components need all be set!
			Raster ras = new Raster(new Point3f(-1.0f, -1.0f, -1.0f),
					Raster.RASTER_COLOR, 0, 0, width, height,
					new ImageComponent2D(ImageComponent.FORMAT_RGB,
							new BufferedImage(width, height,
									BufferedImage.TYPE_INT_RGB)), null);

			ctx.readRaster(ras);

			// Now strip out the image info
			BufferedImage img = ras.getImage().getImage();

			// write that to disk....
			try {
				FileOutputStream out = new FileOutputStream(SCREENSHOT_FILE);
				JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
				JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
				param.setQuality(0.5f, false); // 70% quality JPEG
				encoder.setJPEGEncodeParam(param);
				encoder.encode(img);
				doScreenshotNow = false;
				out.close();

				Runtime.getRuntime().exec(SCREENSHOT_CMD);
			} catch (IOException e) {
				LOG.error("I/O exception: ", e);
			}
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
