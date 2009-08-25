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

package vendetta.visualization.shapes;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import vendetta.MonitorNode;

/**
 * A visual representation of a node on a canvas.
 * 
 * This basic class represents a node on a canvas. Each instance of the class
 * instance can be displayed on one canvas at most.
 * 
 * @version $Id:MonitorNodeShape.java 1508 2008-05-28 13:28:59Z frhe4063 $
 */
public class MonitorNodeShape extends javax.media.j3d.BranchGroup {
	/**
	 * Color of the shape when selected.
	 */
	public static final ColoringAttributes SELECTED = new ColoringAttributes(
			1.0f, 0.3f, 0.3f, ColoringAttributes.FASTEST);

	/**
	 * Color of the shape when not selected.
	 */
	public static final ColoringAttributes UNSELECTED = new ColoringAttributes(
			0.0f, 0.3f, 0.8f, ColoringAttributes.FASTEST);

	/**
	 * The position of the node on the canvas the node was added to.
	 */
	protected Vector3f position;

	/**
	 * The parent node representing a vclient instace.
	 */
	protected MonitorNode owner;

	/**
	 * The transform of the node describing its translation.
	 */
	protected TransformGroup tg;

	private Transform3D trans;
	private NodeSphere sphere;
	private Appearance app;

	public MonitorNodeShape(MonitorNode owner, float size, Shape3D extra) {
		this(owner, size);
		tg.addChild(extra);
	}

	public MonitorNodeShape(MonitorNode owner, float size) {
		super();
		this.owner = owner;
		position = new Vector3f();
		tg = new TransformGroup();
		trans = new Transform3D();
		app = new Appearance();
		app.setColoringAttributes(UNSELECTED);
		app.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		sphere = new NodeSphere(size, app, owner);
		sphere.setPickable(true);
		tg.addChild(sphere);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		setCapability(ALLOW_DETACH);

		addChild(tg);
	}

	/** Free memory */
	public void deleteShape() {
		if (sphere != null) {
			sphere = null;
		}
		if (tg != null) {
			tg = null;
		}
		detach();
	}

	public String getPositionString() {
		return "[ x: " + position.x + " y: " + position.y + " z: " + position.z
				+ "] length: " + position.length();
	}

	public float[] getPosition() {
		if (position == null)
			return null;
		return new float[] { position.x, position.y, position.z };
	}

	/** Translate the node shape */
	public void translate(Vector3f newPos) {
		position = newPos;
		trans.setTranslation(position);
		tg.setTransform(trans);
	}

	/** Translate the node shape */
	public void translate(float x, float y, float z) {
		position = new Vector3f(x, y, z);
		trans.setTranslation(position);
		tg.setTransform(trans);
	}

	/** Called when the object is clicked */
	public void pickAction() {
		owner.pickAction();
	}

	public MonitorNode getOwner() {
		return owner;
	}

	public void select() {
		app.setColoringAttributes(SELECTED);
	}

	public void unSelect() {
		app.setColoringAttributes(UNSELECTED);
	}

	public void beep(ColoringAttributes color) {
		app.setColoringAttributes(color);
	}

	public void unbeep() {
		app.setColoringAttributes(UNSELECTED);
	}

	public void setSize(float f) {
		trans.setScale(f);
		tg.setTransform(trans);
	}

	/**
	 * This method will be called if the owner's global position changed.
	 * 
	 * Deriving classes may implement this method if they wish to be informed
	 * over owner's position changes.
	 * 
	 * @param newPos
	 *            The new global position of the owner.
	 */
	public void ownerPositionChanged(Point3f newPos) {
		translate(new Vector3f(newPos));
	}
}
