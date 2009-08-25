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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vendetta.gui.GUI;
import vendetta.gui.splash.Splash;
import vendetta.monitored_network.haggle.SensorNode;
import vendetta.network.TCPDown;
import vendetta.network.TCPDownDirect;
import vendetta.network.TCPDownStatic;
import vendetta.network.TCPUp;
import vendetta.network.UDPDown;
import vendetta.util.QuotedStringTokenizer;
import vendetta.util.fork.BulkForkWindow;
import vendetta.util.log.GUISink;
import vendetta.util.log.Log;
import vendetta.vbuffer.FileBuffer;
import vendetta.vbuffer.NetBuffer;
import vendetta.vbuffer.VBuffer;
import vendetta.vconfig.VConfigReader;
import vendetta.vconfig.VSettings;

/**
 * Vendetta Monitor Software
 * 
 * @version $Id: Vendetta.java 1534 2008-07-11 09:12:25Z frhe4063 $
 */
public class Vendetta {
	public static final Log LOG = Log.getInstance("main");
	public static final String CHARSET = "US-ASCII";

	private static final String USER = "testbed001";
	private static final String REMOTE_PATH ="/home/"+USER+"/config/";
	private static final String LOCAL_PATH = "configs/";
	
	/**
	 * A helper class containing information about a proxy.
	 */
	private static class Proxy {
		final String address;
		final int portUp;
		final int portDown;
		
		Proxy(String a, int pu, int pd) {
			address = a;
			portUp = pu;
			portDown = pd;
		}
	}

	/**
	 * The two different modes of Vendetta.
	 */
	public enum PlaybackMode {
		NETWORK,
		FILE
	}
	
	/**
	 * Three different types of network log events.
	 */
	public enum NetworkType {
		TCP,	/* Received over TCP. */
		UDP,	/* Received over UDP. */
		NA		/* Not received over the network. */
	}
	
	/**
	 * Parser for log events from nodes. The application logic is defined here.
	 */
	private static MonitoredNetwork monitoredNetwork;
	
	/**
	 * The GUI
	 */
	private static GUI gui;
	
	/** 
	 * UDP downstream channel for log events and PING messages from nodes.
	 */
	private static UDPDown udpDown;
	
	/** 
	 * TCP downstream channel for log events from nodes.
	 */
	private static TCPDown tcpDown;
	
	/**
	 * TCP upstream channel for control commands to sensor nodes.
	 */
	private static TCPUp tcpUp;
	
	/**
	 * Source of log events if in network mode.
	 */
	private static NetBuffer netBuffer;
	
	/**
	 * Source of log events if in file playback mode.
	 */
	private static FileBuffer fileBuffer;
	
	/**
	 * A pointer to one of the two above structures.
	 */
	private static VBuffer srcBuffer;
	
	/**
	 * A log for all events that are received from nodes.
	 * 
	 * This log can later be replayed in file mode.
	 */
	private static PrintWriter eventLog;
	
	/** The Monitor nodes
	 */
	private static MonitorNode[] nodes;
	
	/**
	 * The number of nodes that have been loaded.
	 */
	private static int nrNodes = 0;
	
	/**
	 * Monitor nodes that are currently selected in the GUI.
	 */
	private static MonitorNode[] selected;
	
	/**
	 * The number of nodes that are currently selected in the GUI.
	 */
	private static int nrSelected = 0;

	/** 
	 * Determines which playback buffer to use.
	 */
	private static PlaybackMode mode = PlaybackMode.NETWORK;
	
	/**
	 * The command that is executed when the user opens an SSH connection.
	 */
	public static String sshCommand;
	
	/**
	 * The command that is executed when the user does an secure copy.
	 */
	public static String scpCommand;
	
	/**
	 * The default background color for the GUI and Canvases.
	 * 
	 * Can be set in the configuration file.
	 */
	public static float[] bgColor = { 0/255.0f, 0/255.0f, 0/255.0f };
	
	private static boolean useRemoteConfigPath = false;

	private static boolean useProxy = true;

	public static String CONFIG_PATH = "./configs/";

	public static String proxyIP;
	
	/**
	 * Main method of the Vendetta monitor.
	 * 
	 * This method will load the configuration file, initialize the GUI,
	 * setup the playback buffer (either network or file) and start the
	 * buffer.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		proxyIP = null;
		Proxy proxy = null;
		String[] tableColumnNames = null;
		int buffer_size = 1000, port = 4444;

		gui = new GUI();
		Log.addSink(new GUISink(gui));
		Splash splash = new Splash(gui);
		splash.setStep("Loading config file");

		parseCommandLineArgs(args);
		
		if (useRemoteConfigPath) {
			mountConfigFilesystem();
		}
		
		/* Read the configuration file.
		 */
		VSettings settings = new VConfigReader().parseFile(CONFIG_PATH + "monitor");
		if (settings == null) {
			LOG.error("Unable to load configuration file.");
			cleanUpAndExit(1);
		}
		
		/* Load various configuration settings.
		 */
		try {
			int col = Integer.parseInt(settings.getSettingLine("BGCOLOR"));
			bgColor = new float[] { col / 255.0f, col / 255.0f, col / 255.0f };
			port = Integer.parseInt(settings.getSettingLine("PORT"));
			tableColumnNames = settings.getSetting("TABLECOLUMN");
			buffer_size = Integer.parseInt(settings.getSettingLine("BUFFERSIZE"));
			proxy = loadProxySettings(settings);
		} catch (Exception e) {	
			LOG.error("Failed to read configuration file: ", e);
			cleanUpAndExit(1);
		}

		loadMonitoredNetworkImpl(settings);
		openEventLogFile(settings);
		
		/* Load the user-defined ssh-related commands.
		 */
		sshCommand = settings.getSettingLine("SSHCOMMAND");
		scpCommand = settings.getSettingLine("SCPCOMMAND");
		
		
		splash.setStep("Loading nodes");
		allocateNodes(settings);

		splash.setStep("Initializing GUI");
		gui.initGUI(splash, nodes, tableColumnNames, settings, mode);
		
		splash.setStep( "Loading Nodes" );
		loadNodes(settings);
		
		/* Force de-allocation of the settings.
		 */
		settings = null;
		
		splash.setStep("Starting Network");
		initNetwork(buffer_size, port, proxy);
		initPlayback();
		
		try {
			splash.dispose();
		} catch (Exception e) { }

		srcBuffer.start();
	}
	
	/**
	 * Parse configuration options set at the command line.
	 * 
	 * @param args The arguments given as to main()
	 */
	private static void parseCommandLineArgs(String[] args) {
		for (int i=0;i<args.length;i++) {
			String s = args[i];
			
			if ("--file".equals(s)) {
				mode = PlaybackMode.FILE;
			} else if ("--with-remote-config".equals(s)) {
				useRemoteConfigPath = true;
			} else if ("--no-proxy".equals(s)) {
				LOG.info("Not using a proxy.");
				useProxy = false;
			} else if ("--config-path".equals(s)) {
				if (i+1 >= args.length) {
					LOG.error("Option --config-path requires argument.");
					cleanUpAndExit(1);
				} else {
					CONFIG_PATH = args[i+1];
					i++;
				}
			}
		}
	}
	
	/**
	 * Load the user's implementation of the network to be examined. 
	 */
	private static void loadMonitoredNetworkImpl(VSettings settings) {
		String implName = settings.getSettingLine("MONITORED_NETWORK");
		try {
			monitoredNetwork = (MonitoredNetwork) Class.forName(implName).newInstance();
		} catch (Exception e) {
			LOG.error("Unable load instance of MonitoredNetwork" + implName +
					  ": ", e);
			cleanUpAndExit(1);
		}		
	}
	
	/**
	 * Open a log file for log events received from nodes.
	 * 
	 * Does not have an effect if run in file playback mode.
	 * 
	 * @param settings
	 */
	private static void openEventLogFile(VSettings settings) {
		if (mode != PlaybackMode.NETWORK) {
			return;
		}
		
		String filename = settings.getSettingLine("EVENT_LOG");
		if (filename == null) {
			filename= "./log/logfile.txt";
		}

		try {
			eventLog = new PrintWriter(filename);
		} catch (IOException ioe) {
			LOG.error("Cannot open event log file: ", ioe);
			cleanUpAndExit(1);
		}
	}
	
	/**
	 * Allocate memory for node objects.
	 * 
	 * @param settings
	 */
	private static void allocateNodes(VSettings settings) {
		String filename = settings.getSettingLine("NODEFILE");
		if (filename == null) {
			LOG.error("Cannot parse node file name.");
			cleanUpAndExit(1);
		}
		
		int num = monitoredNetwork.countNodes(CONFIG_PATH + filename);
		if (num == -1) {
			LOG.error("Failed to count nodes.");
			cleanUpAndExit(1);
		}

		nodes = new MonitorNode[num];
	}
	
	/**
	 * Load nodes from configuration file.
	 * 
	 * @param settings
	 */
	private static void loadNodes(VSettings settings) {
		String filename = settings.getSettingLine("NODEFILE");
		if (filename == null) {
			LOG.error("Cannot parse node file name.");
			cleanUpAndExit(1);
		}

		monitoredNetwork.loadNodes(CONFIG_PATH + filename);
	}
	
	/**
	 * Load proxy configuration.
	 * 
	 * @param settings
	 * @return The proxy to use or null.
	 */
	private static Proxy loadProxySettings(VSettings settings) {
		Proxy proxy = null;
		
		if (!useProxy) {
			return null;
		}
		
		String[] tmp = settings.getSetting("PROXY");
		if (tmp != null) {
			try {
				proxyIP = settings.getSubSetting(tmp, "address");
				int portUp = Integer.parseInt(settings.getSubSetting(tmp, "port_up"));
				int portDown = Integer.parseInt(settings.getSubSetting(tmp, "port_down"));
				
				proxy = new Proxy(proxyIP, portUp, portDown);
				
				LOG.info("Using proxy " + proxyIP + ", upstream port: " + portUp + ", downstream port: " + portDown);
			} catch (Exception e) {
				LOG.info("Could not read proxy settings. Not using a proxy.");
				useProxy = false;
			}
		} else {
			LOG.info("Could not read proxy settings. Not using a proxy.");
			useProxy = false;
		}
		
		return proxy;
	}
	
	/**
	 * Initialize the network subsystem.
	 * 
	 * Does not have an effect when in file playback mode.
	 * 
	 * @param buffer_size
	 * @param proxy
	 */
	private static void initNetwork(int buffer_size, int port, Proxy proxy) {
		if (mode != PlaybackMode.NETWORK) {
			return;
		}
		
		netBuffer = new NetBuffer(buffer_size);

		try {
			if (useProxy) {
				tcpDown = new TCPDownStatic(proxy.address, proxy.portDown);
				tcpUp = new TCPUp(proxy.address, proxy.portUp);
				new Thread(tcpUp).start();
			} else {
				tcpDown = new TCPDownDirect(port);
				udpDown = new UDPDown(port+1);
				udpDown.start();
				
				tcpUp = new TCPUp("", -1);
				new Thread(tcpUp).start();
			}
			
			Thread t = new Thread(tcpDown);
			t.setName("TCPDown");
			t.start();
		} catch (IOException ioe) {
			LOG.error("Failed to initialize network: ", ioe);
			cleanUpAndExit(1);
		}
		
		srcBuffer = netBuffer;
	}
	
	/**
	 * Initialize the file playback.
	 * 
	 * Does not have an effect when in playback mode.
	 */
	private static void initPlayback() {
		if (mode != PlaybackMode.FILE) {
			return;
		}
		
		fileBuffer = new FileBuffer();
		srcBuffer = fileBuffer;
	}
	
	/**
	 * Mount the configuration directory from the central server
	 */
	private static void mountConfigFilesystem() {
		Process fs = null; 

		//mount the users File system 
		try {
			  //make sure the target path is unmounted
			  Runtime.getRuntime().exec("fusermount -u "+LOCAL_PATH);
			  //mount the remote file system 
			  fs = Runtime.getRuntime().exec("sshfs "+USER+"@carbon:"+REMOTE_PATH+" "+LOCAL_PATH);
			  
			  
			  
		} catch (IOException e1) {
			e1.printStackTrace();
			LOG.error("Unable to mount remote file system");
			cleanUpAndExit(1);
			
		}
		//Wait for the file system to be mounted before loading the configuration files.
	    try {
			fs.waitFor();
			if(fs.exitValue() != 0)
			{
				LOG.error("Unable to mount remote file system: login failed");
				cleanUpAndExit(1);
				
			}
			
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			cleanUpAndExit(1); 
			LOG.error("an Error occurred while waiting for the remote file system to mount");
		}
	}
	
	/**
	 * Shut down Vendetta.
	 */
	public static void shutdown() {
		if (eventLog != null) {
			eventLog.close();
		}
		
		if (tcpDown != null) {
			tcpDown.close();
		}
		
		if (tcpUp != null) {
			tcpUp.stopServer();
		}
		
		if (udpDown != null) {
			udpDown.close();
		}
		
		if (fileBuffer != null) {
			fileBuffer.stop();
		}
		
		if (netBuffer != null) {
			netBuffer.stop();
		}
		
		cleanUpAndExit(0);
	}

	
	/* ===================================================================== */
	/* ============== END OF INITIALIZATION-RELATED METHODS ================ */
	/* ===================================================================== */

	public static void sortMonitorNodes()
	{
		boolean not_done = true;
		int		i;
		
		do{
			not_done = false;
			
			for(i = 0; i < nrNodes-1; i++)
			{
				if(nodes[i].getSortingValue() > nodes[i+1].getSortingValue())
				{
					MonitorNode n;
					
					n = nodes[i];
					nodes[i] = nodes[i+1];
					nodes[i+1] = n;
					not_done = true;
				}
			}
		
		}while(not_done);
		for(i = 0; i < nrNodes; i++)
			gui.updateTable(nodes[i]);
	}


	/**
	 * Add a monitor node the list of nodes.
	 * 
	 * @param node
	 */ 
	public static void addMonitorNode(MonitorNode node) {
		if (nrNodes == nodes.length) {
			LOG.error("Maximum number of nodes.");
		}
		
		nodes[nrNodes] = node;
		nrNodes++;
		gui.monitorNodeAdded(node);
		sortMonitorNodes();
	}
	
	public static void removeMonitorNode(MonitorNode node) {
		int i;
		boolean has_found;
		
		if (nrNodes == 0) {
			LOG.error("Minimum number of nodes.");
		}
		has_found = false;
		for(i = 0; i < nodes.length; i++)
		{
			if(!has_found)
				if(nodes[i] == node)
					has_found = true;
			
			if(has_found)
				if(i + 1 < nodes.length)
					nodes[i] = nodes[i+1];
				else
					nodes[i] = null;
		}
		if(has_found)
			nrNodes--;
		gui.monitorNodeRemoved(node);
		gui.tableUnselectNodes();
	}
	
	public static void resetMonitorNodes()
	{
		int i;
		for(i = 0; i < nodes.length; i++)
			if(nodes[i] != null)
				nodes[i].reset();
	}

	/**
	 * The monitored application on a node was stopped.
	 * 
	 * @param node The node on which the application was stopped.
	 */
	private static void overlayStopped(MonitorNode node) {
		node.overlayStopped();
		gui.overlayStopped(node);
	}

	/**
	 * Called when we received the first PING from a node.
	 * 
	 * @param GUID
	 * @param host
	 * @return
	 */
	private static MonitorNode monitorNodeStarted(String GUID, String host) {
		MonitorNode node = null;

		/* Find the node in the node list.
		 */
		for (int i=0; i < nodes.length; i++) {
			if (nodes[i] == null) {
				break;
			}

			/* Did we have this hostname in the list?
			 */
			if (host.equals(nodes[i].getHostName())) {
				/* Did we think it was running?
				 */
				if (nodes[i].getGUID().equals(GUID)) {
					LOG.info("Node restarted: " + host);
					return nodes[i];
				}

				/* ?
				 */
				if (nodes[i].getGUID().equals("N/A")) {
					node = nodes[i];
				}
			}
		}

		/* Did we find it?
		 */
		if (node == null) {
//			LOG.debug("Received message from unknown node: " + host);
			return null;
		}

		node.monitorNodeStarted(GUID);
		gui.monitorNodeStarted(node);
		
		return node;
	}

	/**
	 * Called when a node was shut down.
	 * 
	 * @param node
	 */
	public static void monitorNodeStopped(MonitorNode node) {
		if (node.isActive()) {
			overlayStopped(node);
		}
		
		node.monitorNodeStopped();
		gui.monitorNodeStopped(node);
	}

	/**
	 * Clear all nodes.
	 * 
	 * XXX ???
	 */
	public static void clearAllNodes() {
		monitoredNetwork.clearCanvases();
		for (MonitorNode node : nodes) {
			if (node != null) {
				monitorNodeStopped(node);
			}
		}
	}

	/**
	 * Clear selected nodes.
	 * 
	 * XXX ???
	 */
	public static void clearSelectedNodes() {
		if (selected == null) {
			return;
		}
		
		for (MonitorNode node : selected) {
			monitorNodeStopped(node);
		}
	}

	/**
	 * Handle a ping message.
	 * 
	 * Meta-information about the monitored node can piggy-back on
	 * the PING message, so the message is passed on the node object
	 * using the pingReceived() message on the object.
	 *  
	 *  msg[ 0 ] == time
	 *  msg[ 1 ] == GUID
	 *  msg[ 2 ] == "PING"
	 *  msg[ 3 ] == args          0     1    2       3
	 *  PINGs have static looks: time GUID "PING" hostname [overlay args]
	 *  
	 *  @param msg The PING message.
	 */
	public static void handlePing(String[] msg, String original) {
		/* Check if we know of this node
		 */
		MonitorNode node = getMonitorNode(msg[1]);
		String[] args = msg[3].split(" ", 3);
		if (node == null) {
//			LOG.debug("Should not happen");
//			return;
			monitoredNetwork.parseLogEvent(original);
			node = monitorNodeStarted(msg[1], args[0]);
		} else if (!node.isAlive()) {
			gui.monitorNodeStarted(node);
			node.setAlive(true);
		}
		
		/* Does this node have an overlay, and did we know of it?
		 */
		if (node != null && args.length > 2 && !node.isActive()) {
			monitoredNetwork.overlayStarted(node, args[0]);
			node.pingReceived(msg[1] + " " + args[0] + " " + args[1]);
		} else if (node != null && args.length <= 2 && node.isActive()) {
			overlayStopped(node);
		} else if (node != null && args.length > 1) {
			node.pingReceived(msg[1] + " " + args[0] + " " + args[1]);
		}
	}

	/**
	 * Handle a log event received over the network.
	 * 
	 * This method passes the log event over to the netBuffer
	 * which will later pass it on to handleLogEvent. 
	 * 
	 * @param msg The log event.
	 * @param type TCP, if received over TCP, or UDP if recvd over UDP.
	 */
	public static void netReceiveLogEvent(String msg, NetworkType type) {
		String[] split = msg.split(" ", 4);
		try {
			long time = Long.parseLong(split[0]);

			// Check if it was a ping
			if (split[2].equals("PING")) {
				handlePing(split, msg);
			} else {
				// Add it to the playback buffer
				netBuffer.addEvent(msg, time, type);
			}

			if (eventLog != null) {
				eventLog.println(msg);
			}
		} catch (NumberFormatException e) {
			LOG.error("Unable to get the time on: " + msg);
		}
	}

	/**
	 * Pass a log event to the monitored network implementation.
	 * 
	 * This method may only be called by the playback buffers.
	 * 
	 * @param msg
	 */
	public static void handleLogEvent(String msg) {
		String[] split = msg.split(" ", 4);
		if (split.length >= 3 && "PING".equals(split[2])) {
			handlePing(split, msg);
		} else {		
			monitoredNetwork.parseLogEvent(msg);
		}
	}
	
	public static void redraw() {
		monitoredNetwork.redraw();
	}
	
	/**
	 * Flush the event log file.
	 */
	public static void flushEventLog() {
		eventLog.flush();
	}


	/**
	 * Unselect all nodes
	 */
	public static void unselectNodes() {
		/* Unselect the old selection.
		 */
		for (int i = 0; i < nrSelected; i++) {
			/* Unselect the actual node.
			 * This will remove the selection on the canvases
			 */
			selected[i].unselect();
		}
		
		selected = null;
		nrSelected = 0;
		gui.tableUnselectNodes();
		gui.setPanelsEnabled(false);
		monitoredNetwork.handleNodeSelectionChange();
	}

	/**
	 * Set selected nodes.
	 * 
	 * Called by canvases or table when nodes are selected.
	 * 
	 * @param newSelection The newly selected nodes.
	 * @param canvas If nodes where selected in the canvas.
	 */
	public static void selectNodes(MonitorNode[] newSelection, boolean canvas) {
		if (canvas) {
			unselectNodes();
		} else {
			for (int i = 0; i < nrSelected; i++) {
				selected[i].unselect();
			}
		}
		
		/* Select the new nodes
		 */
		selected = newSelection;
		nrSelected = selected.length;

		for (int i = 0; i < nrSelected; i++) {
			/* Notify the nodes, this will show up on the canvases
			 */
			selected[i].select();
			selected[i].pickAction();
		}

		/* Notify the GUI, mainly for table
		 */
		if (canvas) {
			gui.tableSelectNodes(selected);
		}
		
		gui.setPanelsEnabled(true);
		monitoredNetwork.handleNodeSelectionChange();
	}

	/**
	 * Send a TCP control command to the selected nodes.
	 *
	 * @param payload The control command to send.
	 */
	public static void tcpNodes(String payload) {
		try {
			byte[] data = payload.getBytes(CHARSET);
			tcpUp.add(selected, data);
			tcpUp.send();
		} catch (UnsupportedEncodingException e) { }
	}

	/**
	 * Send a control command to a specific monitor node.
	 * 
	 * @param dest The monitor node the command will be sent to.
	 * @param payload The command to send.
	 */
	public static void tcpNode(MonitorNode dest, String payload) {
		try {
			byte[] data = payload.getBytes(CHARSET);
			tcpUp.add(new MonitorNode[] { dest }, data);
			tcpUp.send();
		} catch (UnsupportedEncodingException e) {
		}
	}

	/**
	 * Execute a command.
	 * 
	 * @param command The command to execute.
	 * @param args The commands arguments.
	 */
	public static void exec(String command, String args) {
	    exec(command, args, false, false);
	}
	public static void exec(String command, String args, boolean withoutnodes, boolean onlyOneExec)
	{
		if (!withoutnodes && selected == null) {
			LOG.info("You need to select nodes first.");
			return;
		}
		
		if (command == null) {
			LOG.info("No command specified.");
			return;
		}

		/* Prepare replacements.
		 */
		Map<String, List<String>> replacements = new HashMap<String, List<String>>();
		List<String> hostnameList = new ArrayList<String>();
		replacements.put("<NODE_HOSTNAME>", hostnameList);
		List<String> portList = new ArrayList<String>();
		replacements.put("<NODE_PORT>", portList);
		List<String> nodeNameList = new ArrayList<String>();
		replacements.put("<NODE_NAME>", nodeNameList);
		List<String> nodeIdList = new ArrayList<String>();
		replacements.put("<NODE_ID>", nodeIdList);
		List<String> nodeNameListNext = new ArrayList<String>();
		replacements.put("<NODE_NAME_NEXT>", nodeNameListNext);
		List<String> argsList = new ArrayList<String>();
		replacements.put("<ARGS>", argsList);
		List<String> proxyList = new ArrayList<String>();
		replacements.put("<PROXY_IP>", proxyList);
		List<String> nodeInterfacesList = new ArrayList<String>();
		replacements.put("<NODE_INTERFACES>", nodeInterfacesList);
		List<String> nodeInterfacesListNext = new ArrayList<String>();
		replacements.put("<NODE_INTERFACES_NEXT>", nodeInterfacesListNext);

		if(!withoutnodes && selected != null)
		{
		    for (int i = 0; i < selected.length; i++) {
			hostnameList.add(i, selected[i].getHostName());
			portList.add(i, new Integer(selected[i].getPort()).toString());
			nodeIdList.add(i, new Integer(selected[i].getID()).toString());

			if (selected[i] instanceof SensorNode) {
				if(i + 1 < selected.length)
				{
					nodeNameListNext.add(i, ((SensorNode) selected[i+1]).getNodeName());
					nodeInterfacesListNext.add(i, selected[i+1].getInterfaces());
				}else{
					nodeNameListNext.add(i, ((SensorNode) selected[0]).getNodeName());
					nodeInterfacesListNext.add(i, selected[0].getInterfaces());
				}
			} else {
			    nodeNameListNext.add(i, "");
				nodeInterfacesListNext.add(i, "");
			}
			
			if (selected[i] instanceof SensorNode) {
				nodeNameList.add(i, ((SensorNode) selected[i]).getNodeName());
			} else {
			    nodeNameList.add(i, "");
			}
			nodeInterfacesList.add(i, selected[i].getInterfaces());
			
			argsList.add(i, args);
			proxyList.add(i, proxyIP);
			if(onlyOneExec)
			    i = selected.length;
		    }
		}else{
		    argsList.add(0,args);
		    hostnameList.add(0, "");
		    portList.add(0, "");
		    nodeIdList.add(0, "");
		    nodeNameListNext.add(0, "");
		    nodeNameList.add(0, "");
			proxyList.add(0, proxyIP);
			nodeInterfacesList.add(0, "");
			nodeInterfacesListNext.add(0, "");
		}

		/* Fork the processes ...
		 */
		new BulkForkWindow(gui, command, replacements);
	}

	/**
	 * Execute the SSHCOMMAND
	 */
	public static void sshToNodes() {
		if (selected == null) {
			LOG.info("You need to select a node first.");
			return;
		}
		
		if (sshCommand == null) {
			LOG.info("No SSH command specified.");
			return;
		}

		/* Get the address to the node
		 */
		String IP = selected[0].getIP();
		if (IP.equals("N/A")) {
			IP = selected[0].getHostName();
		}
		
		LOG.info("SSH -> " + IP);

		String command = sshCommand.replaceAll("<NODE_HOSTNAME>", IP);
		String[] args = new QuotedStringTokenizer(command).toArray();

		try {
			Runtime.getRuntime().exec(args);
		} catch (Exception er) {
			LOG.error("Unable to start SSH session.");
			er.printStackTrace();
		}
	}
	
	/* =========== GETTER/SETTER ================ */

	public static String getProxyIP() {
		return proxyIP;
	}
	
	/**
	 * Return the file playback buffer.
	 */
	public static FileBuffer getFileBuffer() {
		return fileBuffer;
	}
	
	/**
	 * Return the GUI.
	 * 
	 * @return
	 */
	public static GUI getGUI() {
		return gui;
	}

	/**
	 * Return the object representing the examined network.
	 *  
	 * @return
	 */
	public static MonitoredNetwork getOverlay() {
		return monitoredNetwork;
	}

	/**
	 * Return the node with the specified guid.
	 * 
	 * @param guid
	 * @return
	 */
	public static MonitorNode getMonitorNode(String guid) {
		for (int i = 0; i < nrNodes; i++) {
			if (nodes[i].guid.equals(guid)) {
				return nodes[i];
			}
		}
		
		return null;
	}
	
	/**
	 * Return the i-th monitor node.
	 * 
	 * @param i
	 * @return
	 */
	public static MonitorNode getMonitorNode(int i) {
		if(nodes == null)
			return null;
		if (i >= nodes.length)
			return null;
		return nodes[i];
	}
	public static int getMonitorNodeCount() {
		return nrNodes;
	}

	/**
	 * Return all selected monitor nodes.
	 * 
	 * @return
	 */
	public static MonitorNode[] getSelectedMonitorNodes() {
		return selected;
	}
	

	public static void cleanUpAndExit(int num) {
		if (useRemoteConfigPath) {
			try {
				Runtime.getRuntime().exec("fusermount -u " + LOCAL_PATH);
			} catch (IOException e) {
				LOG.error("Failed to unmount remote configuration: "
						+ e.getMessage());
				LOG.error(e);
			}
		}

		System.exit(num);
	}

//	public static void msg(String s) {
//		if (gui != null) {
//			gui.println(Color.yellow, s);
//		}
//		System.out.println("Msg: " + s);
//
//	}
//	public static void debug(String src, String s) {
//		System.out.println(src + " " + s);
//		if (gui != null) {
//			gui.print(Color.yellow, src + " ");
//			gui.println(s);
//		}
//	}
//
//	// Error
//	private static void error(String s) {
//		error("(Vendetta)", s);
//	}
//
//	public static void error(String src, String s) {
//		System.out.println(src + " ERROR: " + s);
//		if (gui != null) {
//			gui.print(Color.yellow, src + " ERROR ");
//			gui.println(Color.red, s);
//		}
//	}
//	// Warning
//	public static void warning(String src, String s) {
//		System.out.println(src + " WARNING: " + s);
//		if (gui != null) {
//			gui.print(Color.red, src + " WARNING ");
//			gui.println(s);
//		}
//	}
//
//	public static void log(String msg) {
//		if (gui != null) {
//			gui.print(msg);
//		}
//	}
// Debug
//	private static void debug( String s ) {
//		debug( "(Vendetta)", s );
//	}
//	public static FileBuffer getNetBuffer() {
//		return fileBuffer;
//	}
//
//	public static void sshShowLog() {
//		if (selected == null) {
//			msg("You need to select a node first.");
//			return;
//		}
//		if (SSH_COMMAND_SHOWLOG == null) {
//			msg("No SSH command specified.");
//			return;
//		}
//
//		// get the address to the node
//		String IP = selected[0].getIP();
//		if (IP.equals("N/A"))
//			IP = selected[0].getHostName();
//		Vendetta.msg("SSH -> " + IP);
//
//		String command = Vendetta.SSH_COMMAND_SHOWLOG.replaceAll("<NODE_HOSTNAME>", IP);
//		String[] args = new QuotedStringTokenizer(command).toArray();
//		
//
//		try {
//			Runtime.getRuntime().exec(args);
//		} catch (Exception er) {
//			error("(SSH)", "Unable to start SSH session.");
//			er.printStackTrace();
//		}
//	}
//	/**
//	 * Associate an Overlay Network node with a MonitorNode.
//	 */
//	public static void overlayStarted(MonitorNode node) {
//		gui.overlayStarted(node);
//	}
//
//	/** An Overlay Network has stopped on a node. */
//	public static void overlayStopped(String GUID) {
//		MonitorNode node = getMonitorNode(GUID);
//		node.overlayStopped();
//		// notify the gui an overlay has stopped on 'node'
//		gui.overlayStopped(node);
//	}

}
