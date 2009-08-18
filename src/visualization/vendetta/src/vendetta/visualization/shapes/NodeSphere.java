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
import vendetta.MonitorNode;

public class NodeSphere extends com.sun.j3d.utils.geometry.Sphere {
	private vendetta.MonitorNode owner;

	public NodeSphere(float rad, Appearance app, MonitorNode owner) {
		super(rad, app);
		this.owner = owner;
	}

	public void pickAction() {
		owner.pickAction();
	}

	public MonitorNode getOwner() {
		return owner;
	}
}
