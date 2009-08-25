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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vendetta.MonitorNode;
import vendetta.MonitoredNetwork;
import vendetta.Vendetta;
import vendetta.util.log.Log;
import vendetta.visualization.canvases.FloorCanvas;
import vendetta.visualization.other.PacketVisualizer;

/**
 * This class is part of the PING tutorial. Please see the Vendetta
 * documentation for more information.
 */
public class PingNetwork extends MonitoredNetwork {
	private Log LOG = Log.getInstance("PingNetwork");
	
	private Map<String, MonitorNode> ipToNode = new HashMap<String, MonitorNode>();
	
	public int countNodes(String filename) {
		int n = 0;
		try {
			Scanner s = new Scanner(new File(filename));
			while (s.hasNextLine()) {
				n++;
				s.nextLine();
			}
			
			return n;
		} catch (IOException ioe) {
			LOG.error("Failed counting nodes: ", ioe);
			return -1;
		}
	}
	
	public void loadNodes(String filename) {
		Scanner s = null;
		int nodeId = 0;
		try {
			s = new Scanner(new File(filename));
		} catch (IOException ioe) {
			LOG.error("Failed to load nodes: ", ioe);
			return;
		}
		
		while (s.hasNextLine()) {
			String ip;
			int port;
			PingNode node;
			
			ip = s.next();
			port = Integer.parseInt(s.next());
			
			nodeId++;
			
			s.nextLine();
			
			node = new PingNode(nodeId, ip, port);
			Vendetta.addMonitorNode(node);
			ipToNode.put(ip, node);
		}
	}
	
	public void overlayStarted(MonitorNode n, String args) {
		
	}
	
	public boolean parseLogEvent(String message) {
		String[] split = message.split(" ", 4);
		
		System.out.println(message);
		if ("LE_RECVD".equals(split[2])) {
			Matcher m = Pattern.compile("^\\d* bytes from ([^:]+).*").matcher(split[3]);
			if (m.matches()) {
				String fromIp = m.group(1);
				String toIp = split[1].split(":")[0];
				
				MonitorNode fromNode = ipToNode.get(fromIp);
				MonitorNode toNode = ipToNode.get(toIp);
				
				if (fromNode == null || toNode == null) {
					LOG.warn("PING from unknown node!");
				} else {
					getPacketVisualizer().showPacket(fromNode.getPosition(), toNode.getPosition());
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public void clearCanvases() { }
	
	private PacketVisualizer getPacketVisualizer() {
		return PacketVisualizer.getInstance((FloorCanvas) Vendetta.getGUI().getCanvas(0));
	}
}
