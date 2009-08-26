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

package vendetta;

import javax.vecmath.Point3f;

import vendetta.util.log.Log;

/**
 * A node to be monitored within the examined network.
 * 
 * This abstract class is the base for application-specific implementations.
 * An object of this class represents <b>two</b> fundamental concepts that
 * are very closely related: First, it represents an instance of a vclient,
 * the can be reached over an IP network. Second, it represents a node
 * of the examined system, which is being monitored by the vclient instance.
 * It is important to mind the difference between these two when
 * extending this class.
 * 
 * @version $Id: MonitorNode.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public abstract class MonitorNode {
	private static final Log LOG = Log.getInstance("Vendetta");
	
	/**
	 * Minimum distance between to positions to consider them different.
	 */
	private static final float EPSILON = 0.0001f;
	
	/**
	 * A (locally) unique ID for identifying this node.
	 */
	protected int id;

	/**
	 * A flag indicating whether the examined node is active.
	 * 
	 * Note that the vclient instance can be running without currently
	 * monitoring an application.
	 */
	protected boolean active = false;

	/**
	 * The IP address of the vclient instance.
	 */
	protected String ip = "N/A"; // ex: 127.0.0.1

	/**
	 * The hostname of the vclient instance.
	 */
	protected String hostname = "unknown"; // ex: pollacks.it.uu.se

	/**
	 * The port on which this node's vclient listens for control commands.
	 */
	protected int port;

	/**
	 * Application-specific information about this node.
	 * 
	 * The text in this string will be shown in the node's tooltip.
	 */
	protected String info;

	/**
	 * The globally unique identifier (GUID) of the vclient instance.
	 */
	public String guid = "N/A"; // ex: 127.0.0.1:1234
	
	/**
	 * The globally unique identifier (GUID) of the node under examination.
	 * 
	 * The format of this datum is application dependent; it could be, e.g.,
	 * a MAC address of a wireless interface.
	 */
	protected String oGUID = "N/A";

	/**
	 * The global position information for this monitor node.
	 * 
	 * This is supposed to reflect the actual physical position
	 * information of the node. A node shape can use this positioning
	 * information to create an appropriate placement on an arbitrary
	 * canvas.
	 */
	protected Point3f position;
	
	/**
	 * A flag indicating whether the examined node is alive.
	 * 
	 * Note that the vclient instance can be running (=alive) without currently
	 * monitoring an application, thus `active' and `alive' have two different
	 * meanings.
	 */
	protected boolean alive = false;

	/**
	 * Create a new monitor node.
	 * 
	 * @param id
	 * @param hostName
	 */
	public MonitorNode(int id, String hostName) {
		this.id = id;
		this.hostname = hostName;
	}
	
	/**
	 * Return the string cell value to put in the table.
	 * 
	 * @param col The number of the column, counting from the left.
	 * @return A string describing a property of this node.
	 */
	abstract public String getTableValue(int col);

	public void reset()
	{
	
	}
	/**
	 * Called by Vendetta when a PING from the vclient was received.
	 * 
	 * @param GUID The globally unique identifier, an IP:Port tuple.
	 */
	public void monitorNodeStarted(String GUID) {
		String[] temp = GUID.split(":");
		
		if (temp.length != 2) {
			LOG.error("Invalid GUID for node.");
			return;
		}
		
		this.guid = GUID;
		this.ip = temp[0];
		try {
			this.port = Integer.parseInt(temp[1]);
		} catch (NumberFormatException e) {
			LOG.error("Invalid GUID for node, failed to parse port.");
			this.guid = "N/A";
		}
	}

	/**
	 * Called by Vendetta when the vclient instance of this node was stopped.
	 */
	public void monitorNodeStopped() {
//		guid = "N/A";
	}

	/**
	 * Called by Vendetta when the examined system is started.
	 * 
	 * @param oGUID A global identifier for the examined node.
	 */
	public void overlayStarted(String oGUID) {
		this.oGUID = oGUID;
		active = true;
	}

	/**
	 * Called by Vendetta when the examined system was stopped.
	 */
	public void overlayStopped() {
		oGUID = "N/A";
		active = false;
	}

	public void setCenterThisNode(boolean yes) {}
	public void setHideForwardingDOs(boolean yes) {}
	public void setShowDODOLinks(boolean yes) {}
	public void setDoXMLDump(boolean yes) {}
	
	public void redraw() { }
	/**
	 * Called by Vendetta when a shape represting this monitor was picked.
	 */
	public void pickAction() { }

	/**
	 * Called by Vendetta when this node has been selected in the GUI.
	 */
	public void select() {
	}

	/**
	 * Called by Vendetta when this node was unselected in the GUI.
	 */
	public void unselect() {
	}
	
	/**
	 * Called by Vendetta when a PING message was received.
	 * 
	 * @param args The PING message.
	 */
	public void pingReceived(String args) {
		String[] guids = args.split(" ");
		String[] posString;
		float x, y, z;
		
		if (guids.length < 2) {
			LOG.warn("PING message from " + getHostName() + " misses position.");
			return;
		}
		
		guids[1] = guids[1].replaceAll("\\(|\\)", "");
		posString = guids[1].split(",");
		if (posString.length != 3) {
			LOG.warn("PING message from " + getHostName() + " has malformed position.");
			return;
		}
		
		try {
			x = Float.parseFloat(posString[0]);
			y = Float.parseFloat(posString[1]);
			z = Float.parseFloat(posString[2]);
		} catch (NumberFormatException nfe) {
			LOG.warn("PING message from " + getHostName() + " has malformed position.");
			return;
		}
		
		setPosition(new Point3f(x, y, z), false);		
	}
	
	/**
	 * Get the unique ID of this node.
	 *  
	 * @return
	 */
	public int getID() {
		return id;
	}

	/**
	 * Returns the IP address of the vclient instance.
	 * 
	 * @return An IP address.
	 */
	public String getIP() {
		return ip;
	}

	/**
	 * Returns the hostname of the vclient instance.
	 * 
	 * @return A hostname.
	 */
	public String getHostName() {
		return hostname;
	}

	/**
	 * Returns the port of the vclient instance.
	 * 
	 * @return A port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns the GUID of the vclient instance.
	 * 
	 * @return
	 */
	public String getGUID() {
		return guid;
	}

	/**
	 * Returns the GUID of the observed system.
	 * 
	 * @return The GUID of the observed system.
	 */
	public String getOGUID() {
		return oGUID;
	}
	
	public String getNodeName() {
		return "";
	}
	
	public String getInterfaces() {
		return "";
	}
	
	public int getSortingValue(){
		return 0;
	}
	
	/**
	 * Returns whether the observed system is running.
	 * 
	 * @return
	 */
	public boolean isActive() {
		return active;
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void setAlive(boolean a) {
		Vendetta.getGUI().updateTable(this);
		alive = a;
	}

	/**
	 * Returns application-specific information about this node.
	 * 
	 * @return
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Set the application-specific information about this node.
	 * 
	 * @param info
	 */
	public void setInfo(String info) {
		this.info = info;
	}
	
	/**
	 * Return a human-readable representation of this node.
	 */
	public String toString() {
		return hostname + " [" + guid + "]";
	}

	/**
	 * Get the node's global position information.
	 * 
	 * @return A vector describing the node's position.
	 */
	public Point3f getPosition() {
		return position;
	}

	/**
	 * Set the node's global position information.
	 * 
	 * @param newPosition The new position.
	 * @param report If true, the new position will be sent to the
	 * 					vclient instance.
	 */
	public void setPosition(Point3f newPosition, boolean report) {
		if (position != null && newPosition.distance(position) < EPSILON) {
			return;
		}

		position = newPosition;

		if (report) {
			Vendetta.tcpNode(this, "CTRL_POSITION_UPDATE " +
									position.toString());
		}
	}

///////////// Currently unused.
//	public void unbeep() {
//		if (nodeShapes != null)
//			for (int i = 0; i < nodeShapes.length; i++) {
//				if (nodeShapes[i] != null)
//					nodeShapes[i].unbeep();
//			}
//	}
//
//	public void beep(ColoringAttributes color) {
//		if (nodeShapes != null)
//			for (int i = 0; i < nodeShapes.length; i++) {
//				if (nodeShapes[i] != null)
//					nodeShapes[i].beep(color);
//			}
//	}
//
//	public void deleteNode() {
//		if (nodeShapes != null) {
//			for (int i = 0; i < nodeShapes.length; i++) {
//				if (nodeShapes[i] != null) {
//					nodeShapes[i].deleteShape();
//					nodeShapes[i] = null;
//				}
//			}
//		}
//	}
}
