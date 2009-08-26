/* Haggle testbed
 * Uppsala University
 *
 * Haggle internal release
 *
 * Copyright Haggle
 */

package vendetta.monitored_network.haggle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.vecmath.Point3f;

import vendetta.MonitorNode;
import vendetta.MonitoredNetwork;
import vendetta.Vendetta;
import vendetta.util.log.Log;
import vendetta.visualization.canvases.HaggleCanvas;
import java.net.*;

/**
 * A Vendetta overlay implementation for managing a WISENET WSN testbed.
 *
 * @author Frederik Hermans (hermans@inf.fu-berlin.de)
 */
public class Haggle extends MonitoredNetwork {
	public static final Log LOG = Log.getInstance("Haggle");
	
	/**
	 * Maps node IDs to integers.
	 */
	private Map<Integer, SensorNode> nodes;
	
	/**
	 * A log parser instance to handle log messages.
	 */
	private HaggleLogHandler logParser;
	
	private int nextNodeID;
	
	public Haggle() {
		nodes = new HashMap<Integer, SensorNode>();
		logParser = new HaggleLogHandler(this);
		nextNodeID = 0;
	}
	
	/**
	 * Clear and reset all loaded canvases.
	 */
	public void clearCanvases() {
		int i;
		
		for(i = 0; i < Vendetta.getGUI().getNumCanvases(); i++)
		{
			Vendetta.getGUI().getCanvas(i).clear();
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
	
	public SensorNode getNodeByName(String id) {
		Collection<SensorNode> coll;
		
		if(nodes != null)
		{
			coll = nodes.values();
			if(coll != null)
			{
				Iterator<SensorNode> i;
				i = coll.iterator();
				if(i != null)
				{
					while(i.hasNext())
					{
						SensorNode node = i.next();
						
						if(node.getNodeName().equals(id))
							return node;
					}
				}
			}
		}
		
		return null;
	}
	
	public synchronized SensorNode addNode(String name)
	{
		SensorNode retval;
		
		retval = getNodeByName(name);
		if(retval == null)
		{
			retval = 
				new SensorNode(
					nextNodeID, 
					name, 
					new Point3f(0.0f, 0.0f, 0.0f));
			nodes.put(nextNodeID, retval);
			
			Vendetta.addMonitorNode(retval);
			nextNodeID++;
		}
		return retval;
	}
	
	public synchronized void deleteNode(SensorNode s)
	{
		if(s != null)
		{
			nodes.remove(s.getID());
			if(nodes.containsKey(s.getID()))
				System.out.println("remove failed!\n");
			Vendetta.removeMonitorNode(s);
		}
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
	public synchronized boolean parseLogEvent(String msg) {
		String[] split = msg.split("\\s");
		if (msg.charAt(msg.length()-1) != '\n') {
			msg = msg + '\n';
		}
		
		SensorNode node = getNodeByName(split[3]);
		if(node == null)
		{
			if(!split[3].equals("controller"))
			{
				String[] split2 = split[1].split(":");
				if(split2.length >= 2)
				{
					addNode(split[3]);
				}
			}
		}
		
		return logParser.parseLogEvent(msg);
	}
	
	public void redraw()
	{
		logParser.redraw();
	}

 
	public void handleNodeSelectionChange()
	{
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
		
		Scanner sc = null;
		try {
			 sc = new Scanner(new File(args));
			 sc.useDelimiter(Pattern.compile("\\s+"));
		} catch (FileNotFoundException fnfe) {
			LOG.error("Node file " + args + " not found.");
			return;
		}
		
		while (sc.hasNextLine()) {
			String nodeName = "", hostname = "";
			int nodeId = -1, port = -1;
			float x = 0.0f, y = 0.0f, z = 0.0f;
			
			try {
				nodeName = sc.next();
				if (nodeName.charAt(0) == '#' || nodeName.charAt(0) == '\n') {
					sc.nextLine();
					lineNumber++;
					continue;
				}
				
				hostname = sc.next();
				port = sc.nextInt();
				nodeId = sc.nextInt();
				
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
			
			addNode(nodeName);
		}
	}
	
	/* (non-Javadoc)
	 * @see vendetta.MonitoredNetwork#countNodes(java.lang.String)
	 */
	public int countNodes(String filename) {
		return 100;
		/*
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
		
		return n;*/
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
