/* Wisenet testbed
 * Uppsala University
 *
 * Wisenet internal release
 *
 * Copyright Wisenet
 */

package vendetta.monitored_network.wisenet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.vecmath.Point3f;

import vendetta.MonitorNode;
import vendetta.MonitoredNetwork;
import vendetta.Vendetta;
import vendetta.util.log.Log;
import vendetta.visualization.canvases.FloorCanvas;

/**
 * A Vendetta overlay implementation for managing a WISENET WSN testbed.
 *
 * @author Frederik Hermans (hermans@inf.fu-berlin.de)
 */
public class Wisenet extends MonitoredNetwork {
	public static final Log LOG = Log.getInstance("Wisenet");
	
	/**
	 * Maps node IDs to integers.
	 */
	private Map<Integer, SensorNode> nodes;
	
	/**
	 * Maps Contiki's Rime addresses to node IDs.
	 */
	private Map<String, Integer> rimeAddressToId;
	
	/**
	 * A log parser instance to handle log messages.
	 */
	private WisenetLogHandler logParser;
	
	public Wisenet() {
		nodes = new HashMap<Integer, SensorNode>();
		rimeAddressToId = new HashMap<String, Integer>();
		logParser = new WisenetLogHandler(this);
	}
	
	/**
	 * Clear and reset all loaded canvases.
	 */
	public void clearCanvases() {
		FloorCanvas canvas = (FloorCanvas) Vendetta.getGUI().getCanvas(0);
		canvas.clear();
		
		for (SensorNode node : nodes.values()) {
			node.getMonitorNodeShape(0).setSize(1.0f);
		}
	}

	/**
	 * Return the sensor node for a given ID. 
	 * 
	 * @param id The node ID.
	 * @return The node or null if not found.
	 */
	public SensorNode getNode(int id) {
		return nodes.get(id);
	}
	
	/**
	 * Return the sensor node for a given Rime address.
	 * 
	 * @param rimeAddress The node's Rime address.
	 * @return The node or null if not found.
	 */
	public SensorNode getNodeByRimeAddress(String rimeAddress) {
		Integer id = rimeAddressToId.get(rimeAddress);
		if (id == null)
			return null;
		
		return nodes.get(id);
	}
	
	/* Called when the vclient reports in for the fist time.
	 * @see vendetta.Overlay#overlayStarted(vendetta.MonitorNode, java.lang.String)
	 */
	public void overlayStarted(MonitorNode node, String args) {
		node.overlayStarted(args);
		Vendetta.getGUI().updateTable(node);
	}

	/**
	 * Called when the monitor receives a log event from a client.
	 */
	public boolean parseLogEvent(String msg) {
		if (msg.charAt(msg.length()-1) != '\n') {
			msg = msg + '\n';
		}
		
		return logParser.parseLogEvent(msg);
	}
 
	/* Load the nodes from a given config file.
	 * 
	 * @see vendetta.Overlay#loadNodes(java.lang.String)
	 */
	public void loadNodes(String args) {
		int lineNumber = 1;
		
		if (args == null) {
			LOG.error("Got no arguments.");
			return;
		}
		
		/* Prepare a scanner object to read from the node
		 * configuration file.
		 */
		Scanner sc = null;
		try {
			 sc = new Scanner(new File(args));
			 sc.useDelimiter(Pattern.compile("\\s+"));
		} catch (FileNotFoundException fnfe) {
			LOG.error("Node file " + args + " not found.");
			return;
		}
		
		/* Iterate over all lines ...
		 */
		while (sc.hasNextLine()) {
			String nodeName = "", hostname = "", rimeAddress = "";
			int nodeId = -1, port = -1;
			float x = 0.0f, y = 0.0f, z = 0.0f;
			
			try {
				nodeName = sc.next();
				if (nodeName.charAt(0) == '#' || nodeName.charAt(0) == '\n') {
					/* Eat comments and empty lines.
					 */
					sc.nextLine();
					lineNumber++;
					continue;
				}
				
				hostname = sc.next();
				port = sc.nextInt();
				nodeId = sc.nextInt();
				rimeAddress = sc.next();
				
				x = Float.parseFloat(sc.next());
				y = Float.parseFloat(sc.next());
				z = Float.parseFloat(sc.next());
				
				sc.nextLine();
				lineNumber++;
			} catch (NumberFormatException nfe) {
				LOG.warn("Malformed entry in node file in line " + lineNumber);
			} catch (InputMismatchException ime) {
				LOG.warn("Malformed entry in node file in line " + lineNumber);
			} catch (NoSuchElementException nsee) {
				LOG.warn("Malformed entry in node file in line " + lineNumber);
			}
			
			SensorNode current = new SensorNode(nodeId, rimeAddress, nodeName, hostname, port, new Point3f(x, y, z));
			nodes.put(nodeId, current);
			rimeAddressToId.put(rimeAddress, nodeId);
			
			Vendetta.addMonitorNode(current);
		}
	}
	
	/* (non-Javadoc)
	 * @see vendetta.MonitoredNetwork#countNodes(java.lang.String)
	 */
	public int countNodes(String filename) {
		int n = 0;
		
		try {
			Scanner s = new Scanner(new File(filename));
			while (s.hasNextLine()) {
				if (!s.nextLine().startsWith("#")) {
					n++;
				}
			}
		} catch (IOException ioe) {
			LOG.error("Failed to count nodes: ", ioe);
			return -1;
		}
		
		return n;
	}
	
//	/**
//	 * Create a new monitor node.
//	 * 
//	 * FIXME This seems to be never called.
//	 */
//	public MonitorNode createMonitorNode(int id, String hostname) {
//		return null;
//	}
}
