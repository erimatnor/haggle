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

import javax.media.j3d.Alpha;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Interpolator;
import javax.media.j3d.PositionPathInterpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;

/**
 * Show a particle moving from one position to another.
 *
 * @see vendetta.visualization.other.ConnectionVisualizer
 * @version $Id:MovingParticle.java 1509 2008-05-28 14:16:47Z frhe4063 $
 */
public class MovingParticle extends BranchGroup {
	/**
	 * This is the "timer" for our animation.
	 */
	private Alpha alpha;
	
	/**
	 * Create a particle that moves from `start' to `end' within
	 * `duration' milliseconds.
	 * 
	 * Add this object to your BranchGroup to show it on the
	 * screen. Then start() it to animate.
	 * 
	 * @param start
	 * @param end
	 * @param duration
	 */
	public MovingParticle(Point3f start, Point3f end, long duration) {
		/* Allow detaching the particle later on. We don't
		 * want to be a memory hog.
		 */
		setCapability(ALLOW_DETACH);

		/* We need to translate the origin of our animation
		 * to the start position.
		 */
		TransformGroup translGroup = new TransformGroup();
		Transform3D trans = new Transform3D();
		trans.setTranslation(new Vector3f(start));
		translGroup.setTransform(trans);
		
		/* Now we're relative to the new origin, so translate
		 * the end position as well.
		 */
		Point3f pEnd = new Point3f(end);
		pEnd.sub(start);
		
		TransformGroup tg = new TransformGroup();
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		
		Appearance app = new Appearance();
		app.setColoringAttributes(new ColoringAttributes(new Color3f(1.0f, 0.0f, 1.0f), ColoringAttributes.NICEST));
		Sphere sphere = new Sphere(0.005f, app);
		sphere.setPickable(false);
		tg.addChild(sphere);

		/* Set up timing constraints.
		 */
		alpha = new Alpha(1, duration);
		alpha.setStartTime(Long.MAX_VALUE);

		/* Simple path interpolator.
		 */
		Interpolator interp = new PositionPathInterpolator(alpha, tg, new Transform3D(),
														   new float[] { 0.0f, 1.0f },
														   new Point3f[] { new Point3f(0.0f, 0.0f, 0.0f), pEnd });		
		interp.setSchedulingBounds(new BoundingSphere());

		/* Wire things ...
		 */
		tg.addChild(interp);
		translGroup.addChild(tg);
		addChild(translGroup);
	}
	
	/**
	 * Start the animation. Can be called exactly once.
	 */
	public void start(long delay) {
		alpha.setStartTime(System.currentTimeMillis() + delay);
	}
	
	/**
	 * Returns true if the animation has been completed.
	 * 
	 * @return
	 */
	public boolean isComplete() {
		return alpha.finished();
	}
}
