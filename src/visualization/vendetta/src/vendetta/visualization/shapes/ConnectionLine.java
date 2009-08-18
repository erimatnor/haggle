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
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * A transparent line. The transparency can be set.
 * 
 * This class is used by the connection visualizer class.
 * The line is drawn as a narrow plane that lies within the XY plane.
 * 
 * @see vendetta.visualization.other.ConnectionVisualizer
 * @version $Id:ConnectionLine.java 1509 2008-05-28 14:16:47Z frhe4063 $
 */
public class ConnectionLine extends BranchGroup {
	/**
	 * The color of the line.
	 */
	private static Color3f DARK_GREEN = new Color3f(0.0f / 255.0f, 0.0f / 255.0f, 0.0f / 255.0f);
	
	/**
	 * The transparency attributes.
	 */
	private TransparencyAttributes transpAttrib;
	
	/**
	 * The current transparency.
	 */
	private float transpValue;
	
	public ConnectionLine(Point3f from, Point3f to)
	{
		this(
			from, 
			to, 
			new ColoringAttributes(
				DARK_GREEN, 
				ColoringAttributes.NICEST));
	}
	
	public ConnectionLine(Point3f from, Point3f to, float size)
	{
		this(
			from, 
			to, 
			new ColoringAttributes(
				DARK_GREEN, 
				ColoringAttributes.NICEST),
			size);
	}
	
	private static final float default_width = 0.003f;
	
	public ConnectionLine(Point3f from, Point3f to, ColoringAttributes color) {
		this(from, to, color, default_width);
	}
	
	static private Appearance getAppearanceWithColor(ColoringAttributes col)
	{
		Appearance app = new Appearance();
		app.setColoringAttributes(col);
		return app;
	}
	
	public ConnectionLine(
			Point3f from, 
			Point3f to, 
			ColoringAttributes color, 
			float size) {
		this(from, to, getAppearanceWithColor(color), size);
	}
	
	public ConnectionLine(
			Point3f from, 
			Point3f to, 
			Appearance app) {
		this(from, to, app, default_width);
	}
	
	public ConnectionLine(
			Point3f from, 
			Point3f to, 
			Appearance app, 
			float size) {
		Point3f dir = (Point3f) from.clone();
		dir.sub(to);
		dir.negate();
		
		Vector3f dirv = new Vector3f(dir.x,dir.y,dir.z);
		Vector3f in_plane_vector = new Vector3f(0.0f,0.0f,1.0f);
		
		/* Calculate the normal vector of dir in the XY-plane.
		 */
		Vector3f normal = new Vector3f();
		normal.cross(dirv, in_plane_vector);
		if(normal.length() == 0.0)
		{
			in_plane_vector = new Vector3f(1.0f,0.0f,0.0f);
			normal.cross(dirv, in_plane_vector);
		}
		normal.normalize();
		normal.scale(size);
		
		in_plane_vector.cross(dirv, normal);
		in_plane_vector.normalize();
		in_plane_vector.scale(size);
		
		/* Construct the plane (a thin bar) that represents
		 * the connection.
		 */
		QuadArray plane1 = new QuadArray(4, GeometryArray.COORDINATES);
		
		Point3f p = (Point3f) from.clone();
		p.sub(normal);
		p.sub(in_plane_vector);
		plane1.setCoordinate(3, p);
		
		p = (Point3f) from.clone();
		p.sub(normal);
		p.add(in_plane_vector);
		plane1.setCoordinate(2, p);
		
		p = (Point3f) to.clone();
		p.sub(normal);
		p.add(in_plane_vector);
		plane1.setCoordinate(1, p);
		
		p = (Point3f) to.clone();
		p.sub(normal);
		p.sub(in_plane_vector);
		plane1.setCoordinate(0, p);
		
		QuadArray plane2 = new QuadArray(4, GeometryArray.COORDINATES);
		
		p = (Point3f) from.clone();
		p.add(normal);
		p.sub(in_plane_vector);
		plane2.setCoordinate(0, p);
		
		p = (Point3f) from.clone();
		p.add(normal);
		p.add(in_plane_vector);
		plane2.setCoordinate(1, p);
		
		p = (Point3f) to.clone();
		p.add(normal);
		p.add(in_plane_vector);
		plane2.setCoordinate(2, p);
		
		p = (Point3f) to.clone();
		p.add(normal);
		p.sub(in_plane_vector);
		plane2.setCoordinate(3, p);
		
		QuadArray plane3 = new QuadArray(4, GeometryArray.COORDINATES);
		
		p = (Point3f) from.clone();
		p.sub(normal);
		p.sub(in_plane_vector);
		plane3.setCoordinate(0, p);
		
		p = (Point3f) from.clone();
		p.add(normal);
		p.sub(in_plane_vector);
		plane3.setCoordinate(1, p);
		
		p = (Point3f) to.clone();
		p.add(normal);
		p.sub(in_plane_vector);
		plane3.setCoordinate(2, p);
		
		p = (Point3f) to.clone();
		p.sub(normal);
		p.sub(in_plane_vector);
		plane3.setCoordinate(3, p);
		
		QuadArray plane4 = new QuadArray(4, GeometryArray.COORDINATES);
		
		p = (Point3f) from.clone();
		p.sub(normal);
		p.add(in_plane_vector);
		plane4.setCoordinate(3, p);
		
		p = (Point3f) from.clone();
		p.add(normal);
		p.add(in_plane_vector);
		plane4.setCoordinate(2, p);
		
		p = (Point3f) to.clone();
		p.add(normal);
		p.add(in_plane_vector);
		plane4.setCoordinate(1, p);
		
		p = (Point3f) to.clone();
		p.sub(normal);
		p.add(in_plane_vector);
		plane4.setCoordinate(0, p);

		/* Construct the shape and its appearance.
		 */
		Shape3D shape1 = new Shape3D(plane1);
		Shape3D shape2 = new Shape3D(plane2);
		Shape3D shape3 = new Shape3D(plane3);
		Shape3D shape4 = new Shape3D(plane4);
		//transpAttrib = new TransparencyAttributes(TransparencyAttributes.NICEST, 1.0f);
		//transpAttrib.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		//app.setTransparencyAttributes(transpAttrib);
		app.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK, 0.0f));
		LineAttributes lineAttributes = new LineAttributes();
		lineAttributes.setLineAntialiasingEnable(true);
		app.setLineAttributes(lineAttributes);
		app.setPointAttributes(new PointAttributes(1.0f, true));
		shape1.setAppearance(app);
		shape1.setPickable(false);
		shape2.setAppearance(app);
		shape2.setPickable(false);
		shape3.setAppearance(app);
		shape3.setPickable(false);
		shape4.setAppearance(app);
		shape4.setPickable(false);
		
		setCapability(BranchGroup.ALLOW_DETACH);
		
		//setTransparency(1.0f);
		
		addChild(shape1);
		addChild(shape2);
		addChild(shape3);
		addChild(shape4);
	}
	
	/**
	 * Set the strength of the connection associated with this line.
	 * 
	 * Setting the strength will affect the line's transparency.
	 * 
	 * @param strength A value in the range of [0.0, 1.0]
	 */
	public void setTransparency(float strength) {
		this.transpValue = strength;
		
		/* Using the squared strength will give a nicer fading effect.
		 */
		float opacity = 1 - (strength * strength);
		transpAttrib.setTransparency(opacity);
	}
	
	/**
	 * Get the current transparency value.
	 * 
	 * @return The current transparency value.
	 */
	public float getTransparency() {
		return transpValue;
	}
}
