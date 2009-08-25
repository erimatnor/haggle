/* Wisenet testbed
 * Uppsala University
 *
 * Wisenet internal release
 *
 * Copyright Wisenet
 */

package vendetta.monitored_network.wisenet;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import vendetta.MonitorNode;
import vendetta.visualization.shapes.MonitorNodeShape;

/**
 * A sensor node that is part of the testbed.
 * 
 * @version $Id: WisenetNode.java 1496 2008-04-22 17:08:34Z frhe4063 $
 */
public class SensorNode extends MonitorNode {
	/**
	 * The unique name of the node.
	 */
	private String nodeName;
	
	/**
	 * The unique id of the node.
	 */
	private int nodeId;
	
	/**
	 * The Contiki rime address, if any.
	 */
	private String rimeAddress;
	
	/**
	 * Create a new testbed node.
	 * 
	 * @param id
	 * @param hostname
	 * @param port
	 * @param nodeId
	 * @param pos
	 */
	public SensorNode(int id, String rimeAddress, String nodeName, String hostname, int port, Point3f pos) {
		super(id, hostname);
		
		this.nodeId = id;
		this.nodeName = nodeName;
		this.hostname = hostname;
		this.port = port;
		this.rimeAddress = rimeAddress;
		this.position = pos;
		
		resolveHostname();
		initShape();
	}
	
	/**
	 * Resolve the hostname of the node client to an IP address.
	 */
	private void resolveHostname() {
		try {
			InetAddress addr = InetAddress.getByName(hostname);
			ip = addr.getHostAddress();
			guid = ip + ":" + port;
		} catch (UnknownHostException uhe) {
			throw new RuntimeException("Failed to resolve hostname: " + uhe.getMessage());
		}
	}
	
	/**
	 * Initialize the shape to be drawn on the canvas.
	 */
	private void initShape() {
		/* Setup the shape. Once for each canvas.
		 */
		nodeShapes = new MonitorNodeShape[1];
		nodeShapes[0] = new MonitorNodeShape(this, 0.01f);
		nodeShapes[0].translate(new Vector3f(position));
	}
	
	public String getTableValue(int col) {
		switch (col) {
		case 0:
			return ""+alive;
		case 1:
			return ""+active;
		case 2:
			return nodeName;
		case 3:
			return rimeAddress == null ? "00:00" : rimeAddress;
		case 4:
			return "" + nodeId;
		case 5:
			return hostname;
		}
		
		return "UNKNOWN";
	}

	public void pickAction() {
		/* Ignore.
		 */
	}
	
	/**
	 * Return the unique name of the node.
	 * 
	 * @return The unique name of the node.
	 */
	public String getNodeName() {
		return nodeName;
	}
	
	/**
	 * Return the unique Rime address of the node, if any.
	 * 
	 * @return The Rime address of the node or null if none.
	 */
	public String getRimeAddress() {
		return rimeAddress;
	}
}
