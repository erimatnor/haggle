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

package vendetta.monitored_network.tutorial;

import vendetta.MonitorNode;
import vendetta.visualization.shapes.MonitorNodeShape;

/**
 * This class is part of the PING tutorial. Please see the Vendetta
 * documentation for more information.
 */
public class PingNode extends MonitorNode {
	public PingNode(int id, String ip, int port) {
		super(id, ip);
		
		guid = ip + ":" + port;
		this.ip = ip;
		this.port = port;
		
		nodeShapes = new MonitorNodeShape[1];
		nodeShapes[0] = new MonitorNodeShape(this, 0.01f);
	}
	
	public String getTableValue(int num) {
		switch (num) {
		case 0:
			return new Boolean(alive).toString();
		case 1:
			return ip;
		case 2:
			return new Integer(port).toString();
		}
		
		return "Unknown.";
	}
}
