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

/**
 * A base class for a network to be examined.
 * 
 * This serves as the base for application-specific implementations. You can
 * use it to store global information about the network you are examining.
 * 
 * Give the name of your implementation of this class as the MONITORED_NETWORK
 * option in the main configuration file.
 * 
 * @version $Id: MonitoredNetwork.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
abstract public class MonitoredNetwork {
	/**
	 * Load the network's nodes from a conifguration file.
	 * 
	 * This method is supposed to call Vendetta.addNode() for each node that
	 * it loads.
	 * 
	 * @param nodeFile The name of the file containing the nodes.
	 */
	abstract public void loadNodes(String nodeFile);

	/**
	 * Return the number of nodes defined in a configuration file. 
	 * 
	 * @param file The configuration file.
	 * @return
	 */
	abstract public int countNodes(String file);
	
	/**
	 * Handle a log event that was received from a node.
	 * 
	 * @param msg IP:PORT TYPE args1=value1 args2=value2 ...
	 */
	abstract public boolean parseLogEvent(String msg);

	public void handleNodeSelectionChange() {}
	
	public void redraw() {}
	
	/**
	 * Called by Vendetta when a node is started.
	 */
	abstract public void overlayStarted(MonitorNode node, String args);

	/**
	 * Clear drawn stuff on the canvases.
	 */
	abstract public void clearCanvases();

/////////// Currently unused.
//	abstract public MonitorNode createMonitorNode(int id, String hostname);
//	/**
//	 * Get an argument value of the message
//	 */
//	protected String getArg(String arg, String[] args) {
//		for (int i = 2; i < args.length; i++)
//			if (args[i].startsWith(arg))
//				return args[i].split("=")[1];
//		return null;
//	}
}
