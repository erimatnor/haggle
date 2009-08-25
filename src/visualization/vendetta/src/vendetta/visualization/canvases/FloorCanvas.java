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

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickRay;
import javax.media.j3d.QuadArray;
import javax.media.j3d.SceneGraphPath;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3d;

import vendetta.MonitorNode;
import vendetta.Vendetta;
import vendetta.vconfig.VConfigReader;
import vendetta.vconfig.VSettings;
import vendetta.visualization.shapes.MonitorNodeShape;
import vendetta.visualization.shapes.NodeSphere;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.image.TextureLoader;

/**
 * A 3D Vendetta canvas showing floor plan
 * 
 * @version $Id:FloorCanvas.java 1509 2008-05-28 14:16:47Z frhe4063 $
 */
public class FloorCanvas extends Vendetta3DCanvas {
	/**
	 * A plane within the XY plane a floor texture mapped to it.
	 */
	private class FloorPlane extends Shape3D {
		public FloorPlane(String textureFile, Point3f[] planeCoords, TexCoord2f[] texCoords) {
			/* Construct the plane.
			 */
			QuadArray plane = new QuadArray(4, GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
			plane.setCoordinate(0, planeCoords[0]);
			plane.setCoordinate(1, planeCoords[1]);
			plane.setCoordinate(2, planeCoords[2]);
			plane.setCoordinate(3, planeCoords[3]);
			
			plane.setTextureCoordinate(0, 0, texCoords[0]);
			plane.setTextureCoordinate(0, 1, texCoords[1]);
			plane.setTextureCoordinate(0, 2, texCoords[2]);
			plane.setTextureCoordinate(0, 3, texCoords[3]);
			
			setGeometry(plane);
			
			/* Load the texture.
			 */
			TextureLoader loader = new TextureLoader(textureFile, FloorCanvas.this);
			ImageComponent2D img = loader.getImage();
			Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA, img.getWidth(), img.getHeight());
			texture.setImage(0, img);
	        texture.setMagFilter(Texture.NICEST);
	        texture.setMinFilter(Texture.NICEST);
	        
//	        TextureAttributes ta = new TextureAttributes();
//	        ta.setTextureMode(TextureAttributes.DECAL);
	        
			/* Set its appearance.
			 */
			Appearance app = new Appearance();
			app.setColoringAttributes(new ColoringAttributes(new Color3f(1.0f, 1.0f, 1.0f), ColoringAttributes.NICEST));
//			app.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, 0.1f));
			app.setTexture(texture);
//			app.setTextureAttributes(ta);
			
			setAppearance(app);
			setPickable(true);
		}
	}

	/**
	 * The floors that are displayed on the canvas.
	 */
	private FloorPlane[] floors;
	
	/**
	 * The node that has to be moved on the next click (if any).
	 * 
	 * This canvas supports moving nodes: Click on a node with the SHIFT key
	 * down and then click again on a floor plane to move the node.
	 */
	private NodeSphere nodeToMove;
	
	public FloorCanvas() {
		super();
		initFromConfigFile(Vendetta.CONFIG_PATH + "floorcanvas");
	}
	
	/**
	 * Parses the configuration file and constructs the floors.
	 * 
	 * @param configFile
	 * @throws RuntimeException If a parse error occurrs.
	 */
	private void initFromConfigFile(String configFile) {
		VConfigReader configReader = new VConfigReader();
		VSettings config = configReader.parseFile(configFile);
		if (config == null) {
			throw new RuntimeException("Failed to read configuration for floor canvas.");
		}
		
		name = "Floor canvas (" + config.getSettingLine("canvas_name") + ")";
		
		/* Iterate over all <floor>-tags in the config file.
		 */
		String[][] floorTags = config.getSettings("floor");
		floors = new FloorPlane[floorTags.length];
		for (int i=0;i<floorTags.length;i++) {
			/* First, read the coordinates of the plane.
			 */
			String planeCoordsConfig = config.getSubSetting(floorTags[i], "plane_coordinates");
			String[] planeCoordsStrings;
			Point3f[] planeCoords = new Point3f[4];
			int j = 0;
			
			if (planeCoordsConfig == null) {
				throw new RuntimeException("A <floor>-tag is missing plane_coordinates value.");
			}
			
			planeCoordsStrings = planeCoordsConfig.split(";");
			if (planeCoordsStrings.length != 4) {
				throw new RuntimeException("A plane_coordinates value does not specify exactly four coordinates.");
			}
			
			/* Now parse each coordinate.
			 */
			for (String c : planeCoordsStrings) {
				String[] components = c.split(",");
				float x, y, z;
				
				if (components.length != 3) {
					throw new RuntimeException("plane_coordinates coordinate does not have three components.");
				}
				
				try {
					x = Float.parseFloat(components[0]);
					y = Float.parseFloat(components[1]);
					z = Float.parseFloat(components[2]);
				} catch (NumberFormatException nfe) {
					throw new RuntimeException("Failed to parse float in plane_coordinates.");
				}

				/* Construct the j-th coordinate.
				 */
				planeCoords[j] = new Point3f(x, y, z);
				j++;
			}
			
			/* Next, read the texture file name and the texture coordinates.
			 */
			String textureFile, texCoordsConfig;
			String[] texCoordsStrings;
			TexCoord2f[] texCoords = new TexCoord2f[4];
			
			textureFile = config.getSubSetting(floorTags[i], "texture");
			if (textureFile == null) {
				throw new RuntimeException("floor tag is missing texture value.");
			}
			
			texCoordsConfig = config.getSubSetting(floorTags[i], "texture_coordinates");
			if (texCoordsConfig == null) {
				throw new RuntimeException("floor tag is missing texture_coordinates value.");
			}
			texCoordsStrings = texCoordsConfig.split(";");
			if (texCoordsStrings.length != 4) {
				throw new RuntimeException("texture_coordinates value does not specify four coordinates.");
			}
			
			/* Parse each texture coordinate.
			 */
			j=0;
			for (String c : texCoordsStrings) {
				String[] components = c.split(",");
				float x, y;
				
				if (components.length != 2) {
					throw new RuntimeException("texture_coordinates coordinate does not have two components.");
				}
				
				try {
					x = Float.parseFloat(components[0]);
					y = Float.parseFloat(components[1]);
				} catch (NumberFormatException nfe) {
					throw new RuntimeException("Failed to parse float in texture_coordinates.");
				}
				
				texCoords[j] = new TexCoord2f(x, y);
				j++;
			}
			
			/* Finally, create the floor plane!
			 */
			floors[i] = new FloorPlane(textureFile, planeCoords, texCoords);
		}
	}
	
	/**
	 * Prepare the Java 3D-related stuff.
	 */
	protected void createSceneGraph(int id) {
		branchRoot = new BranchGroup();

		/* Set the background.
		 */
		Background bg = new Background(new Color3f(1.0f, 1.0f, 1.0f));
		bg.setApplicationBounds(INF);
		branchRoot.addChild(bg);
		
		branchRoot.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		branchRoot.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		branchRoot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		branchRoot.setCapability(BranchGroup.ALLOW_DETACH);
		
		/* Setup the transform group
		 */
		objTrans = new TransformGroup();
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		objTrans.setCapability(BranchGroup.ALLOW_DETACH);
		branchRoot.addChild(objTrans);

		/* Setup zooming.
		 */
		MouseWheelZoom zoom = new MouseWheelZoom(objTrans);
		zoom.setSchedulingBounds(new BoundingSphere());
		branchRoot.addChild(zoom);

		MouseTranslate transl = new MouseTranslate(objTrans);
		transl.setCapability(MouseTranslate.ALLOW_BOUNDS_WRITE);
		transl.setSchedulingBounds(new BoundingSphere());
		transl.setFactor(0.005f);
		branchRoot.addChild(transl);
		
		MouseRotate rotate = new MouseRotate();
		rotate.setTransformGroup(objTrans);
		rotate.setSchedulingBounds(new BoundingSphere());
		branchRoot.addChild(rotate);
		
		/* Setup the planes.
		 */
		for (int i=0;i<floors.length;i++) {
			objTrans.addChild(floors[i]);
		}
		
		su.getViewingPlatform().setNominalViewingTransform();
		su.addBranchGraph(branchRoot);
	}
	
	public void monitorNodeAdded(MonitorNode node) {
	}
	
	public void clear() {
		objTrans.setTransform(new Transform3D());
	}
	
	public void monitorNodeRemoved(MonitorNode node) {
	}
	
	public void monitorNodeStarted(MonitorNode node) {
		MonitorNodeShape shape = node.getMonitorNodeShape(id);
		objTrans.addChild(shape);
	}
	
	public void monitorNodeStopped(MonitorNode node) {
	}
	
	public void overlayStarted(MonitorNode node) {
	}

	public void overlayStopped(MonitorNode node) {
	}

	public void toggleSleepMode(boolean state) {
	}
	
	/* (non-Javadoc)
	 * @see vendetta.visualization.canvases.Vendetta3DCanvas#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent evt) {
		Point3d eyePos = new Point3d();
		Point3d mousePos = new Point3d();

		getCenterEyeInImagePlate(eyePos);
		getPixelLocationInImagePlate(evt.getX(), evt.getY(), mousePos);

		Transform3D motion = new Transform3D();
		getImagePlateToVworld(motion);
		motion.transform(eyePos);
		motion.transform(mousePos);

		Vector3d direction = new Vector3d(mousePos);
		
		direction.sub(eyePos);

		PickInfo info = branchRoot.pickClosest(PickInfo.PICK_GEOMETRY,
							PickInfo.CLOSEST_INTERSECTION_POINT |
							PickInfo.SCENEGRAPHPATH | PickInfo.NODE,
							new PickRay(eyePos, direction));
		
		if (info != null) {
			Object picked = (Object) info.getNode();
			if (picked instanceof FloorPlane) {
				if (nodeToMove != null) {
					nodeToMove.getOwner().setPosition(new Point3f(info.getClosestIntersectionPoint()), true);
					nodeToMove = null;
				}
			} else {
				SceneGraphPath path = info.getSceneGraphPath();
				
				if (path != null) {
					MonitorNode[] selected = new MonitorNode[1];
					NodeSphere node = (NodeSphere) path.getNode(0);
					selected[0] = node.getOwner();
					Vendetta.selectNodes(selected, true);
					
					if ((evt.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) {
						LOG.info("Click again to reposition node.");
						nodeToMove = node;
					}
				}
			}
		}
	}
}
