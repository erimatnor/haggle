/* haggle testbed
 * Uppsala University
 *
 * haggle internal release
 *
 * Copyright haggle
 */

package vendetta.monitored_network.haggle;

import java.net.InetAddress;
import java.net.UnknownHostException;
import vendetta.visualization.canvases.*;

import vendetta.Vendetta;
import vendetta.MonitorNode;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

import org.xml.sax.InputSource;
import java.io.*;
import java.net.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.util.*;
import vendetta.util.log.Log;
import java.awt.*;

class XMLGetter implements Runnable {
	private void xmlDebug(String str) {
		// Comment this out to hide parsing debug:
		try {
			// System.out.println(str);
		} catch (Exception e) {
		}
	}

	private void progressDebug(String str) {
		// Comment this out to hide progress debug:
		try {
			// System.out.println(str);
		} catch (Exception e) {
		}
	}

	private void xslDebug(String str) {
		// Comment this out to hide parsing debug:
		try {
			// System.out.println(str);
		} catch (Exception e) {
		}
	}

	protected Thread thread;

	public Node latestxml;
	private String fromIP;
	private Transformer transformer;
	private DocumentBuilder db;
	private int number_of_fails;

	public XMLGetter() {
		try {
			number_of_fails = 0;
			latestxml = null;
			fromIP = null;
			Source xsl = new StreamSource(new FileReader(Vendetta.CONFIG_PATH
					+ "dump2relations.xsl"));
			TransformerFactory factory = TransformerFactory.newInstance();
			Templates template = factory.newTemplates(xsl);
			transformer = template.newTransformer();
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			thread = new Thread(this);
			thread.start();
		} catch (Exception e) {
		}
	}

	public void getXML(String fromIP) {
		this.fromIP = fromIP;
		thread.interrupt();
	}

	public void run() {
		boolean should_wait;
		should_wait = false;
		while (true) {
			try {
				if (fromIP != null) {
					String ip = fromIP;
					fromIP = null;
					should_wait = false;
					progressDebug(new Date() + ": Getting XML from " + ip);
					String xml = getXMLFromHaggle(ip);
					if (xml != null) {
						progressDebug(new Date() + ": Got XML from " + ip);
						xslDebug("XML: " + xml);

						try {
							InputSource iSource = new InputSource(
									new StringReader(xml));
							Document doc = db.parse(iSource);
							doc.getDocumentElement().normalize();
							latestxml = (Node) doc.getDocumentElement();
							number_of_fails = 0;
						} catch (Exception e) {
						}
						progressDebug(new Date() + ": Parsed XML from " + ip);
					} else {
						number_of_fails++;
						if (number_of_fails < 3) {
							fromIP = ip;
							should_wait = true;
							progressDebug(new Date() + ": Didn't get XML from "
									+ ip);
						} else {
							fromIP = null;
							should_wait = true;
							number_of_fails = 0;
							progressDebug(new Date() + ": Didn't get XML from "
									+ ip);
						}
					}
				}
				if (should_wait || fromIP == null)
					Thread.sleep(1000);
			} catch (InterruptedException e) {
			} catch (Exception e) {
				System.out.println("run(): " + e);
				e.printStackTrace();
			}
		}
	}

	public String getXMLFromHaggle(String ip) {
		String retval = null;

		try {
			Socket MyClient;
			progressDebug("Retreiving database xml from " + ip);
			if (ip.startsWith("node-")) {
				MyClient = new Socket(Vendetta.getProxyIP(), 9797);
				MyClient.getOutputStream().write(
						("tcp " + ip + " 50901\n").getBytes());
			} else {
				MyClient = new Socket(ip, 50901);
			}
			new PrintWriter(MyClient.getOutputStream(),
					true);
			BufferedReader input = new BufferedReader(new InputStreamReader(
					MyClient.getInputStream()));

			String xmlString = "";
			String tmp;
			do {
				tmp = input.readLine();
				if (tmp != null) {
					xmlString = xmlString + tmp + "\n";
				}
			} while (tmp != null);

			xmlDebug("Original XML: " + xmlString);

			// System.out.println("*** Starting parse: " + new Date());
			Writer resWriter = new StringWriter();
			Result result = new StreamResult(resWriter);
			Source xml = new StreamSource(new StringReader(xmlString));
			transformer.transform(xml, result);

			retval = resWriter.toString();
			// System.out.println("*** Parsing complete: " + new Date());
		} catch (IOException e) {
			// System.out.println(e);
		} catch (TransformerConfigurationException tce) {
			// System.out.println(tce);
		} catch (TransformerException te) {
			// System.out.println(te);
		} catch (Exception pe) {
			System.out.println("getXMLFromHaggle(): " + pe);
			pe.printStackTrace();
		}

		return retval;
	}
}

public class SensorNode extends MonitorNode {
	private class Animation {
		public class EndPoint {
			public String DataObjectID;
			public boolean nodeTarget;

			EndPoint(String doID, boolean targ) {
				DataObjectID = doID;
				nodeTarget = targ;
			}
		}

		public EndPoint startPoint;
		public EndPoint middlePoint;
		public EndPoint endPoint;
		public long length, frame;
		public Color col;

		Animation(String fromDO, boolean fromNode, String throughDO,
				boolean throughNode, String toDO, boolean toNode, long length,
				Color col) {
			if (fromDO == null)
				throw new RuntimeException("no \"from\" DO.");
			if (toDO == null)
				throw new RuntimeException("no \"to\" DO.");

			this.startPoint = new EndPoint(fromDO, fromNode);
			this.endPoint = new EndPoint(toDO, toNode);
			if (throughDO != null)
				this.middlePoint = new EndPoint(throughDO, throughNode);
			this.col = col;

			this.length = length;
			this.frame = 0;
		}

		void advance() {
			frame++;
		}

		boolean isRunning() {
			return frame < length;
		}
	}

	private static Log LOG = Log.getInstance("SensorNode");

	private void parseDebug(String str) {
		// Comment this out to hide parsing debug:
		try {
			// LOG.debug(str);
		} catch (Exception e) {
		}
	}

	private String nodeName;

	private int nodeId;
	private XMLGetter xmlGetter;

	private DOTable DataObjectTable;
	private TreeMap<String, TreeMap<String, Float>> do_links;
	private TreeMap<String, TreeMap<String, Float>> metrics;
	private int selected_do;

	private boolean show_do_do_links;
	private static boolean do_xml_dump = true;
	private static boolean show_poor_metrics = true;
	private static boolean show_classic_metrics = true;
	private int ttl;
	private boolean centerThisNode;

	// The position as displayed
	private Coordinate displayed_pos;
	// The actual current position
	private Coordinate current_pos;
	// The next position
	private Coordinate calculated_pos;
	private Rect drawnPos;
	private boolean is_selected;
	private boolean is_being_dragged;
	private Color myColor;
	private String node_id;
	private ArrayList<Animation> animations;
	private String interface_list;
	private int send_failure_countdown;

	public synchronized void reset() {
		DataObjectTable = new DOTable();
		do_links = new TreeMap<String, TreeMap<String, Float>>();
		metrics = new TreeMap<String, TreeMap<String, Float>>();
		selected_do = -1;

		ttl = 0;
		setAlive(false);

		drawnPos = null;
		is_selected = false;
		node_id = null;
		animations = new ArrayList<Animation>();
		interface_list = "";
		send_failure_countdown = 0;
	}

	public SensorNode(int id, String nodeName) {
		super(id, "unknown");
		Random generator = new Random();

		myColor = null;
		reset();

		displayed_pos = new Coordinate((float) (generator.nextDouble()) - 0.5f,
				(float) (generator.nextDouble()) - 0.5f);
		current_pos = new Coordinate(displayed_pos.x, displayed_pos.y);
		calculated_pos = new Coordinate(displayed_pos.x, displayed_pos.y);

		centerThisNode = true;

		this.nodeId = id;
		this.nodeName = nodeName;
		this.hostname = null;
		show_do_do_links = false;
		xmlGetter = new XMLGetter();

		resolveHostname();
		initShape();
	}

	public Coordinate getDisplayedPosition() {
		return displayed_pos;
	}

	public Coordinate getCurrentPosition() {
		return current_pos;
	}

	public Coordinate getCalculatedPosition() {
		return calculated_pos;
	}

	public void setDisplayedPosition(Coordinate pos) {
		displayed_pos = pos;
	}

	public void setCurrentPosition(Coordinate pos) {
		current_pos = pos;
	}

	public void setCalculatedPosition(Coordinate pos) {
		calculated_pos = pos;
	}

	public void setRect(Rect r) {
		drawnPos = r;
	}

	public Rect getRect() {
		return drawnPos;
	}

	public boolean isAlive() {
		return ttl > 0;
	}

	public void select() {
		is_selected = true;
	}

	public void unselect() {
		is_selected = false;
	}

	public void select(boolean yes) {
		is_selected = yes;
	}

	public boolean isSelected() {
		return is_selected;
	}

	public void startDragging() {
		is_being_dragged = true;
	}

	public void stopDragging() {
		is_being_dragged = false;
	}

	public boolean isBeingDragged() {
		return is_being_dragged;
	}

	final static double base_size = 0.02f;

	public double getCurrentSize() {
		if (send_failure_countdown > 0)
			return base_size * 1.5;
		return base_size;
	}

	public Color getColor() {
		if (myColor == null)
			return new Color(1.0f, 1.0f, 1.0f);
		return myColor;
	}

	private Color getColorPriv() {
		if (myColor == null)
			return new Color(0, 0, 0);
		return myColor;
	}

	public Color getOutlineColor() {
		if (is_selected) {
			return new Color(1.0f, 1.0f, 0.0f);
		}
		if (send_failure_countdown > 0)
			return new Color(1.0f, 0.0f, 0.0f);
		return
		// Black is default:
		new Color(0.0f, 0.0f, 0.0f);
	}

	public String getNodeID() {
		return node_id;
	}

	public boolean isConnectedTo(String nodeID) {
		if (nodeID == null)
			return false;
		if (isAlive()) {
			DataObject dObj = DataObjectTable.getDO(DataObjectTable
					.indexByNodeID(nodeID));
			if (dObj != null)
				return dObj.isNodeVisible();
		}
		return false;
	}

	/**
	 * Resolve the hostname of the node client to an IP address.
	 */
	private void resolveHostname() {
		try {
			InetAddress addr = InetAddress.getByName(hostname);
			ip = addr.getHostAddress();
			guid = ip + ":" + "5001";
		} catch (UnknownHostException uhe) {
			throw new RuntimeException("Failed to resolve hostname: "
					+ uhe.getMessage());
		}
	}

	/**
	 * Initialize the shape to be drawn on the canvas.
	 */
	private void initShape() {
		/*
		 * Setup the shape. Once for each canvas.
		 */
	}

	public String getTableValue(int col) {
		switch (col) {
		case 0:
			return "" + alive;
		case 1:
			return nodeName;
		case 2:
			return (hostname == null ? "0.0.0.0" : hostname);
		case 3:
			return "";// rimeAddress == null ? "00:00" : rimeAddress;
		case 4:
			return "" + nodeId;
		case 5:
			return hostname;
		}

		return "UNKNOWN";
	}

	public void pickAction() {
		/*
		 * Ignore.
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

	public int getSortingValue() {
		if (nodeName.equals("Macken"))
			return -5;
		if (nodeName.equals("Tulip"))
			return -4;
		if (nodeName.equals("Adam"))
			return -3;
		if (nodeName.equals("Bertil"))
			return -2;
		if (nodeName.equals("Ceasar"))
			return -1;
		if (nodeName.equals("node-unknown"))
			return 200;
		if (nodeName.startsWith("node-")) {
			int value = 99;
			try {
				value = Integer.parseInt(nodeName.split("-")[1]);
			} catch (Exception e) {
			}
			if (nodeName.equals("node-" + value))
				return value;
		}
		return 201;
	}

	public synchronized void setCenterThisNode(boolean yes) {
		centerThisNode = yes;
	}

	public synchronized void setShowDODOLinks(boolean yes) {
		show_do_do_links = yes;
	}

	public static void setDoXMLDump(boolean yes) {
		do_xml_dump = yes;
	}

	public static void setShowPoorMetrics(boolean yes) {
		show_poor_metrics = yes;
	}

	public static void setShowClassicMetrics(boolean yes) {
		show_classic_metrics = yes;
	}

	public synchronized void setHideForwardingDOs(boolean yes) {
		DataObject.setHideForwardingDOs(yes);
	}

	private void updateTTL() {
		ttl =
		// Number of renderGraphics calls per second:
		10 *
		// Number of seconds between pings:
		10 *
		// Number of pings to miss before being dead:
		3;
	}

	private void getXML(String ip) {
		if (do_xml_dump)
			if (ip == null)
				xmlGetter.getXML(ip);
			else if (ip.startsWith("192.168.122."))
				xmlGetter.getXML(nodeName);
			else
				xmlGetter.getXML(ip);
	}

	public synchronized void pingReceived(String args) {
		if (ttl == 0) {
			String[] split = args.split("\\s");
			String ip;
			ip = split[0].split(":")[0];
			if (ip != null) {
				getXML(ip);

				if (hostname == null) {
					hostname = ip;
					resolveHostname();
					Vendetta.getGUI().updateTable(this);
				}
			}
			System.out.println("event received: reset drawing!");
		}
		updateTTL();
	}

	public synchronized void handleEvent(String evt) {
		// System.out.print(evt);
		try {
			String[] split = evt.split("\\s");

			pingReceived(split[1]);

			if (split.length < 3)
				LOG.debug("Bogus event: " + evt);
			else {
				if (hostname == null) {
					String[] my_ip = split[1].split(":");
					if (my_ip.length > 0) {
						hostname = my_ip[0];
						resolveHostname();
						Vendetta.getGUI().updateTable(this);
					}
				}

				if ("LE_EVENT".equals(split[2])) {
					String[] split2 = split[1].split(":");
					getXML(split2[0]);
					updateTTL();
				} else if ("EVENT_TYPE_SHUTDOWN".equals(split[2])) {
					// Set to 1 so that next time it's counted down to 0.
					ttl = 1;
				} else if ("EVENT_TYPE_DATAOBJECT_NEW".equals(split[2])) {
					if (split.length < 5)
						LOG.debug("Bogus event: " + evt);
					else {
						if (split.length > 6) {
							if (split[5].equals("-")) {
								DataObjectTable.ensure_do_is_in_table(split[4],
										null);
								DataObject node = DataObjectTable
										.getDO(DataObjectTable
												.indexByNodeID(split[6]));
								if (node != null) {
									animations.add(new Animation(node
											.getNodeID(), true, null, false,
											split[4], false, 30, new Color(
													0.0f, 1.0f, 0.0f)));
									// node.setImportantFor(30);
									DataObject dO = DataObjectTable
											.getDO(DataObjectTable
													.indexByID(split[4]));
									if (dO != null)
										dO.setImportantFor(90);
								}
							} else {
								DataObject node = DataObjectTable
										.getDO(DataObjectTable
												.indexByNodeID(split[6]));
								DataObject dO = DataObjectTable
										.getDO(DataObjectTable
												.indexByNodeID(split[5]));
								if (node != null && dO != null) {
									animations.add(new Animation(node
											.getNodeID(), true, null, false, dO
											.getID(), false, 30, new Color(
											0.0f, 1.0f, 0.0f)));
									// node.setImportantFor(30);
									dO.setImportantFor(90);
								}
							}
						} else if (split.length == 6) {
						} else {
							DataObjectTable.ensure_do_is_in_table(split[4],
									null);
						}
					}
				} else if ("EVENT_TYPE_DATAOBJECT_DELETED".equals(split[2])) {
					if (split.length < 5)
						LOG.debug("Bogus event: " + evt);
					else {
						DataObject dO = DataObjectTable.getDO(DataObjectTable
								.indexByID(split[4]));
						if (dO != null)
							if (!dO.isNodeDescription())
								dO.setAlive(false);
					}
				} else if ("EVENT_TYPE_NODE_DESCRIPTION_SEND".equals(split[2])) {
					DataObject thisNode;

					thisNode = DataObjectTable.getThisNodeDO();
					if (thisNode != null) {
						int i;
						for (i = 0; i < DataObjectTable.getLength(); i++) {
							DataObject dO;

							dO = DataObjectTable.getDO(i);
							if (dO != null && dO.isNodeVisible()) {
								animations.add(new Animation(thisNode.getID(),
										false, null, false, dO.getNodeID(),
										true, 30, new Color(0.0f, 1.0f, 0.0f)));
								// thisNode.setImportantFor(30);
								// dO.setImportantFor(30);
							}
						}
					}
				} else if ("EVENT_TYPE_NODE_CONTACT_NEW".equals(split[2])) {
					if (split.length < 5)
						LOG.debug("Bogus event: " + evt);
					else {
						DataObject dObj = DataObjectTable.getDO(DataObjectTable
								.indexByNodeID(split[4]));
						if (dObj != null)
							dObj.setNodeIsVisible(true);
						/*
						 * else System.out.println(
						 * "Got node contact new for node I " +
						 * "knew nothing about (" + split[4] + ")");
						 */
					}
				} else if ("EVENT_TYPE_NODE_CONTACT_END".equals(split[2])) {
					if (split.length < 5)
						LOG.debug("Bogus event: " + evt);
					else {
						DataObject dObj = DataObjectTable.getDO(DataObjectTable
								.indexByNodeID(split[4]));
						if (dObj != null)
							dObj.setNodeIsVisible(false);
						/*
						 * else System.out.println(
						 * "Got node contact end for node I " +
						 * "knew nothing about (" + split[4] + ")");
						 */
					}
				} else if ("EVENT_TYPE_DELEGATE_NODES".equals(split[2])) {
					if (split.length < 7)
						LOG.debug("Bogus event: " + evt);
					else if (split[4] != null && split[5] != null
							&& split[6] != null) {
						DataObject node1 = DataObjectTable
								.getDO(DataObjectTable.indexByNodeID(split[5]));
						DataObject node2 = DataObjectTable
								.getDO(DataObjectTable.indexByNodeID(split[6]));
						if (node1 != null && node2 != null &&
						// FIXME: hack to hide attempted forwarding of a
						// dataobject to itself.
								!split[4].equals(node2.getID())) {
							animations.add(new Animation(split[4], false, node1
									.getNodeID(), true, node2.getNodeID(),
									true, 60, new Color(1.0f, 0.0f, 1.0f)));
							// node1.setImportantFor(60);
							// node2.setImportantFor(60);
							/*
							 * DataObject dO = DataObjectTable.getDO(
							 * DataObjectTable.indexByID( split[4])); if(dO !=
							 * null) dO.setImportantFor(60);
							 */
						}
					}
				} else if ("EVENT_TYPE_TARGET_NODES".equals(split[2])) {
					if (split.length < 6)
						LOG.debug("Bogus event: " + evt);
					else if (split[4] != null && split[5] != null) {
						DataObject node = DataObjectTable.getDO(DataObjectTable
								.indexByNodeID(split[5]));
						if (node != null &&
						// FIXME: hack to hide attempted forwarding of a
						// dataobject to itself.
								!split[4].equals(node.getID())) {
							// Animate targetting
							animations.add(new Animation(split[4], false, null,
									false, node.getNodeID(), true, 30,
									new Color(0.0f, 0.0f, 1.0f)));
							/*
							 * node.setImportantFor(30); DataObject dO =
							 * DataObjectTable.getDO( DataObjectTable.indexByID(
							 * split[4])); if(dO != null)
							 * dO.setImportantFor(30);
							 */
						}
					}
				} else if ("EVENT_TYPE_DATAOBJECT_SEND_SUCCESSFUL"
						.equals(split[2])) {
					if (split.length < 6)
						LOG.debug("Bogus event: " + evt);
					else if (split[4] != null && split[5] != null) {
						DataObject node = DataObjectTable.getDO(DataObjectTable
								.indexByNodeID(split[5]));
						if (node != null) {
							// Animate forwarding
							animations.add(new Animation(split[4], false, null,
									false, node.getNodeID(), true, 30,
									new Color(0.0f, 1.0f, 0.0f)));
							/*
							 * node.setImportantFor(30); DataObject dO =
							 * DataObjectTable.getDO( DataObjectTable.indexByID(
							 * split[4])); if(dO != null)
							 * dO.setImportantFor(30);
							 */

							// Tell the canvas to animate DO sends, too...
							((HaggleCanvas) Vendetta.getGUI().getCanvas(0))
									.addPacketAnimation(node.getNodeID(),
											getNodeID());
						}
					}
				} else if ("EVENT_TYPE_DATAOBJECT_SEND_FAILURE"
						.equals(split[2])) {
					send_failure_countdown = 30;
				} else if ("LE_SEND_SECURITY_SUCCESS".equals(split[2])) {

				} else if ("LE_SEND_SECURITY_FAILURE".equals(split[2])) {
					if (split.length < 5)
						LOG.debug("Bogus event: " + evt);
					else {
						// DataObjectTable.ensure_do_is_in_table(
						// split[4],
						// null);
						DataObject dObj = DataObjectTable.getDO(DataObjectTable
								.indexByID(split[4]));
						if (dObj != null)
							dObj.startSecurityFailAnimation();
					}
				} else if ("LE_MACHINE_TYPE".equals(split[2])) {
					// FIXME: handle machine type info.
				} else if ("PING".equals(split[2])) {
				} else {
					LOG.debug("Bogus event: " + evt);
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		;
	}

	private final static double spacing = 0.02;
	private final static double line_width = 0.01;
	private final static double arrow_head_size = 0.03;

	private Coordinate asDOPos(Coordinate p) {
		return new Coordinate((p.x / 2.0 + 0.5), (p.y / 2.0 + 0.5)
				* (0.5 - spacing));
	}

	private Coordinate asNodePos(Coordinate p) {
		return new Coordinate((p.x / 2.0 + 0.5), 0.5 + spacing
				+ (p.y / 2.0 + 0.5) * (0.5 - spacing));
	}

	private Coordinate revAsDOPos(Coordinate p) {
		return new Coordinate((p.x - 0.5) * 2.0,
				((p.y / (0.5 - spacing)) - 0.5) * 2.0);
	}

	private Coordinate revAsNodePos(Coordinate p) {
		return new Coordinate((p.x - 0.5) * 2.0, ((p.y - (0.5 + spacing))
				/ (0.5 - spacing) - 0.5) * 2.0);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	public synchronized void doRender(DrawingSurface ds) {
		int i;

		//ds.pushBounds(0.0, 0.0, 0.0, 0.0);
		ds.fillRect(0.0, 0.0, 1.0, 1.0, Color.white);

		// Draw bottom circle:
		ds.fillOval(0.0, 0.5 + spacing, 1.0, 1.0, line_width, Color.black,
		// FIXME: perhaps a non-white color?
				Color.white);
		// Draw bottom circle interior again to mimic the look of the top
		// circle:
		ds.fillOval(0.0 + line_width / 2.0, 0.5 + spacing + line_width / 2.0,
				1.0 - line_width / 2.0, 1.0 - line_width / 2.0, new Color(1.0f,
						1.0f, 1.0f, 0.75f));

		// Draw top circle.
		ds.fillOval(0.0, 0.0, 1.0, 0.5 - spacing, line_width, getColorPriv(),
				Color.white);

		// Draw metrics (before nodes, so the lines appear behind the
		// nodes)

		Object[] entries = metrics.entrySet().toArray();
		Color better_metric_color = new Color(0.0f, 1.0f, 0.0f);
		Color worse_metric_color = new Color(1.0f, 0.0f, 0.0f);
		java.util.TreeMap<java.lang.String, java.lang.Float> internal_metric = null;

		DataObject thisNode = DataObjectTable.getThisNodeDO();
		if (thisNode != null) {
			for (i = 0; i < entries.length && internal_metric == null; i++) {
				Map.Entry<String, TreeMap<String, Float>> entry1 = (Map.Entry<String, TreeMap<String, Float>>) (entries[i]);
				if (entry1.getKey().equals(thisNode.getNodeID())) {
					internal_metric = entry1.getValue();
				}
			}
		}

		for (i = 0; i < entries.length; i++) {
			int j;
			Map.Entry<String, TreeMap<String, Float>> entry1 = (Map.Entry<String, TreeMap<String, Float>>) (entries[i]);

			int do1_index = DataObjectTable.indexByNodeID(entry1.getKey());
			Object[] Js = entry1.getValue().entrySet().toArray();
			for (j = 0; j < Js.length; j++) {
				Map.Entry<String, Float> entry2 = (Map.Entry<String, Float>) (Js[j]);
				int do2_index = DataObjectTable.indexByNodeID(entry2.getKey());

				if (do1_index != do2_index) {
					DataObject dObj1 = DataObjectTable.getDO(do1_index);
					DataObject dObj2 = DataObjectTable.getDO(do2_index);

					if (dObj1 != null && dObj2 != null) {
						if (dObj1.isVisible() && dObj2.isVisible()) {
							if (show_classic_metrics) {
								// Old style gray line thingy:

								ds.drawLine(
										asNodePos(dObj1.getDisplayedPosition()),
										asNodePos(dObj2.getDisplayedPosition()),
										line_width / 2.0,
										dObj1.getNodeNodeLinkColor(getMaxMetric(
												dObj1.getNodeID(),
												dObj2.getNodeID())));
							} else {
								// New style bent arrows thingy:

								Coordinate p1, p2, disp, top_of_arc;

								p1 = asNodePos(dObj1.getDisplayedPosition());
								p2 = asNodePos(dObj2.getDisplayedPosition());
								disp = p2.clone();
								disp.sub(p1);
								disp.normalize();
								disp = new Coordinate(-disp.y, disp.x);
								disp.scale(0.025f);
								p2.sub(p1);
								p2.scale(0.90f);
								p2.add(p1);
								top_of_arc = p2.clone();
								top_of_arc.sub(p1);
								top_of_arc.scale(0.5f);
								top_of_arc.add(disp);
								top_of_arc.add(p1);
								if (internal_metric != null) {
									Float im;
									float m;

									im = internal_metric.get(dObj2.getNodeID());
									if (im == null)
										m = 0.0f;
									else
										m = im.floatValue();

									if (m <= entry2.getValue().floatValue()) {
										ds.drawBentArrow(p1, 0.0, p2,
												arrow_head_size,
												line_width / 2.0, disp, 16,
												better_metric_color);
										ds.drawStringCentered(
												String.format("%.4f",
														entry2.getValue()),
												top_of_arc, 0.6, Color.black,
												Color.white);
									} else if (show_poor_metrics) {
										ds.drawBentArrow(p1, 0.0, p2,
												arrow_head_size,
												line_width / 2.0, disp, 16,
												worse_metric_color);
										ds.drawStringCentered(
												String.format("%.4f",
														entry2.getValue()),
												top_of_arc, 0.6, Color.black,
												Color.white);
									}
								} else {
									ds.drawBentArrow(p1, 0.0, p2,
											arrow_head_size, line_width / 2.0,
											disp, 16, better_metric_color);
									ds.drawStringCentered(
											String.format("%.4f",
													entry2.getValue()),
											top_of_arc, 0.6, Color.black,
											Color.white);
								}
							}
						}
					}
				}
			}
		}

		// draw nodes at the bottom in back-to-front order (by inverse y
		// coordinate).

		TreeMap<Double, DataObject> dObjMap = new TreeMap<Double, DataObject>();

		// Sort:
		for (i = 0; i < DataObjectTable.getLength(); i++) {
			DataObject dObj = DataObjectTable.getDO(i);

			if (dObj.isNodeDescription()) {
				dObjMap.put(new Double(dObj.getDisplayedPosition().y), dObj);
			}
		}

		// Get:
		DataObject[] dObj = (DataObject[]) (dObjMap.values()
				.toArray(new DataObject[0]));
		if (dObj != null)
			for (i = 0; i < dObj.length; i++) {
				// Draw line to the top:
				ds.drawLine(asNodePos(dObj[i].getDisplayedPosition()),
						asDOPos(dObj[i].getDisplayedPosition()),
						line_width / 2, dObj[i].getNodeDOLinkColor());

				// Draw:
				dObj[i].setNodeRect(ds.fillCube(
						asNodePos(dObj[i].getDisplayedPosition()),
						dObj[i].getCurrentNodeSize(), 0.05,
						dObj[i].getNodeOutlineColor(),
						dObj[i].getNodeColor(ttl > 0)));
			}

		// Draw top circle interior again.
		ds.fillOval(0.0 + line_width / 2.0, 0.0 + line_width / 2.0,
				1.0 - line_width / 2.0, 0.5 - spacing - line_width / 2.0,
				new Color(1.0f, 1.0f, 1.0f, 0.75f));

		// Draw DO-DO links:

		Object[] entry = do_links.entrySet().toArray();

		for (i = 0; i < entry.length; i++) {
			int do1_index = DataObjectTable
					.indexByRowID(((Map.Entry<String, TreeMap<String, Float>>) entry[i])
							.getKey());
			Object[] Js = ((Map.Entry<String, TreeMap<String, Float>>) entry[i])
					.getValue().entrySet().toArray();
			int j;
			for (j = 0; j < Js.length; j++) {
				Map.Entry<String, Float> entry2 = (Map.Entry<String, Float>) (Js[j]);
				int do2_index = DataObjectTable.indexByRowID(entry2.getKey());
				if (do1_index < do2_index) {
					DataObject dObj1 = DataObjectTable.getDO(do1_index);
					DataObject dObj2 = DataObjectTable.getDO(do2_index);

					if (dObj1 != null && dObj2 != null) {
						if (dObj1.isVisible() && dObj2.isVisible()) {
							if (show_do_do_links || dObj1.isNodeDescription()
									|| dObj2.isNodeDescription()) {
								ds.drawLine(
										asDOPos(dObj1.getCurrentPosition()),
										asDOPos(dObj2.getCurrentPosition()),
										DataObject.getDODOLinkWidth(
												line_width / 2,
												entry2.getValue(), dObj1, dObj2),
										DataObject.getDODOLinkColor(
												entry2.getValue(), dObj1, dObj2));
							}
						}
					}
				}
			}
		}

		// Draw data objects:
		dObj = null;

		dObjMap = new TreeMap<Double, DataObject>();

		// Sort:
		for (i = 0; i < DataObjectTable.getLength(); i++) {
			DataObject dO = DataObjectTable.getDO(i);

			dObjMap.put(new Double(dO.getDisplayedPosition().y), dO);
		}

		// Get:
		dObj = (DataObject[]) (dObjMap.values().toArray(new DataObject[0]));
		if (dObj != null) {
			for (i = 0; i < dObj.length; i++)
				if (dObj[i].isVisible()) {
					// Draw:
					dObj[i].setDORect(ds.fillCircle(
							asDOPos(dObj[i].getDisplayedPosition()),
							dObj[i].getCurrentSize(), 0.1,
							dObj[i].getDOOutlineColor(), dObj[i].getDOColor()));
				}
			// Show hit rects?
			if (false) {
				for (i = 0; i < dObj.length; i++)
					if (dObj[i].isVisible()) {
						ds.drawRect(dObj[i].getDORect(), 0.002, Color.red);
						ds.drawRect(dObj[i].getNodeRect(), 0.002, Color.red);
					}
			}
			// Show attributes of selected data objects:
			for (i = 0; i < dObj.length; i++)
				if (dObj[i].isVisible()
						&& (dObj[i].isSelected() || dObj[i].isAnimating())) {
					Attribute[] attr = dObj[i].getAttributes();
					Rect[] size = new Rect[attr.length];
					double textScale = 1.0;
					Coordinate p = asDOPos(dObj[i].getDisplayedPosition());
					double up = dObj[i].getDORect().height;
					double w = dObj[i].getDORect().width;
					double borderSize = 0.005;
					double width, height;
					int j;

					width = 0;
					height = 0;
					if (dObj[i].isSelected()) {
						for (j = 0; j < attr.length; j++) {
							String str;

							if (attr[j].getName().equals("Picture"))
								str = attr[j].getValue();
							else
								str = attr[j].getName() + "="
										+ attr[j].getValue();
							size[j] = ds.measureString(str, textScale);
							if (size[j].width > width)
								width = size[j].width;
							height += size[j].height;
						}

						Rect r = new Rect(p.x - width / 2 - 2 * borderSize, p.y
								- height - up - 2 * borderSize, p.x + width / 2
								+ 2 * borderSize, p.y - up + 2 * borderSize);
						ds.fillRect(r, Color.white);
						ds.drawRect(r, borderSize, Color.black);
						double x, y;
						x = r.left + 2 * borderSize;
						y = r.top + 2 * borderSize;
						for (j = 0; j < attr.length; j++) {
							String str;

							if (attr[j].getName().equals("Picture"))
								str = attr[j].getValue();
							else
								str = attr[j].getName() + "="
										+ attr[j].getValue();
							ds.drawStringTopLeft(str, x, y, textScale,
									Color.black);
							y += size[j].height;
						}
					}
					if (dObj[i].getThumbnail() != null) {
						Rect s = dObj[i].getThumbnailSize();
						ds.drawImageInRect(new Rect(p.x - 2 * w, p.y - height
								- up - 2 * borderSize - 4 * s.height * w
								/ s.width - up / 2, p.x + 2 * w, p.y - height
								- up - 2 * borderSize - up / 2),
								dObj[i].getThumbnail());
					}
				}
		}

		// Draw animations:
		for (i = 0; i < animations.size(); i++) {
			Animation a;
			Coordinate start, middle, end;
			long f, l;

			a = animations.get(i);
			start = getPos(a.startPoint);
			middle = getPos(a.middlePoint);
			end = getPos(a.endPoint);
			if (start != null && end != null) {
				if (middle != null) {
					middle.sub(start);
					middle.scale(0.95);
					middle.add(start);
					middle.sub(end);
					middle.scale(0.95);
					middle.add(end);
				}
				f = a.frame;
				l = a.length;
				if (f > l / 2) {
					f -= l / 2;
					l -= l / 2;
					if (middle != null) {
						if (f < l / 2) {
							ds.drawLine(start, ((double) f) / ((double) l / 2),
									middle, 1.0, line_width / 2, a.col);
							ds.drawArrow(middle, 0.0, 0.0, end, 1.0,
									arrow_head_size, line_width / 2, a.col);
						} else {
							f -= l / 2;
							ds.drawArrow(middle, ((double) f)
									/ ((double) l / 2), 0.0, end, 1.0,
									arrow_head_size, line_width / 2, a.col);
						}
					} else {
						ds.drawArrow(start, ((double) f) / ((double) l), 0.0,
								end, 1.0, arrow_head_size, line_width / 2,
								a.col);
					}
				} else {
					l -= l / 2;
					if (middle != null) {
						if (f < l / 2) {
							ds.drawArrow(start, 0.0, 0.0, middle, ((double) f)
									/ ((double) l / 2), arrow_head_size,
									line_width / 2, a.col);
						} else {
							f -= l / 2;
							ds.drawLine(start, 0.0, middle, 1.0,
									line_width / 2, a.col);
							ds.drawArrow(middle, 0.0, 0.0, end, ((double) f)
									/ ((double) l / 2), arrow_head_size,
									line_width / 2, a.col);
						}
					} else {
						ds.drawArrow(start, 0.0, 0.0, end, ((double) f)
								/ ((double) l), arrow_head_size,
								line_width / 2, a.col);
					}
				}
			}
		}

		// Draw thisNode's attributes below the window:
		if (dObj != null)
			for (i = 0; i < dObj.length; i++)
				if (dObj[i].isThisNode()) {
					Attribute[] attr = dObj[i].getAttributes();
					Rect[] size = new Rect[attr.length];
					double textScale = 1.0;
					Coordinate p = new Coordinate(0.5, 1);
					double width, height;
					int j;

					width = 0;
					height = 0;
					for (j = 0; j < attr.length; j++) {
						String str;

						if (attr[j].getName().equals("Picture"))
							str = attr[j].getValue();
						else
							str = attr[j].getName() + "=" + attr[j].getValue();
						size[j] = ds.measureString(str, textScale);
						if (size[j].width > width)
							width = size[j].width;
						height += size[j].height;
					}

					double borderSize = 0.005;
					Rect r = new Rect(p.x - width / 2 - 2 * borderSize, p.y,
							p.x + width / 2 + 2 * borderSize, p.y + height + 4
									* borderSize);
					ds.fillRect(r, Color.white);
					ds.drawRect(r, borderSize, Color.black);
					double x, y;
					x = p.x - width / 2;
					y = r.top + 2 * borderSize;
					for (j = 0; j < attr.length; j++) {
						String str;

						if (attr[j].getName().equals("Picture"))
							str = attr[j].getValue();
						else
							str = attr[j].getName() + "=" + attr[j].getValue();
						ds.drawStringTopLeft(str, x, y, textScale, Color.black);
						y += size[j].height;
					}
				}

		// Fade out non-active nodes.
		if (!isAlive()) {
			ds.fillRect(new Rect(0, 0, 1, 1),
					new Color(1.0f, 1.0f, 1.0f, 0.75f));
		}
		//ds.popBounds();
	}

	private Coordinate getPos(Animation.EndPoint ep) {
		if (ep != null) {
			DataObject o;
			if (ep.nodeTarget)
				o = DataObjectTable.getDO(DataObjectTable
						.indexByNodeID(ep.DataObjectID));
			else
				o = DataObjectTable.getDO(DataObjectTable
						.indexByID(ep.DataObjectID));
			if (o != null)
				if (ep.nodeTarget)
					return asNodePos(o.getDisplayedPosition());
				else
					return asDOPos(o.getDisplayedPosition());
		}
		return null;
	}

	public synchronized String getTagContent(Node node) {
		if (node != null) {
			NodeList fstNm = ((Element) node).getChildNodes();
			if (fstNm != null) {
				if (fstNm.getLength() > 0) {
					return ((Node) fstNm.item(0)).getNodeValue();
				}
			}
		}
		return null;
	}

	public synchronized String getAttributeContent(Node node, String attribute) {
		if (node != null) {
			NamedNodeMap fstNm = ((Element) node).getAttributes();
			if (fstNm != null) {
				if (fstNm.getLength() > 0) {
					return fstNm.getNamedItem(attribute).getNodeValue();
				}
			}
		}
		return null;
	}

	public synchronized String getTagContent(Node node, String element_name,
			int num) {
		if (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				NodeList nodes = getSubTags(node, element_name);
				if (nodes != null) {
					Element subElmnt = (Element) nodes.item(num);
					if (subElmnt != null) {
						NodeList fstNm = subElmnt.getChildNodes();
						if (fstNm != null) {
							if (fstNm.getLength() > 0) {
								return ((Node) fstNm.item(0)).getNodeValue();
							}
						}
					}
				}
			}
		}
		return null;
	}

	public synchronized String getTagContent(Node node, String element_name) {
		return getTagContent(node, element_name, 0);
	}

	public synchronized Node getSubTag(Node node, String element_name, int num) {
		if (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				// FIXME: figure out a faster way to create an empty node:
				Node tmp = node.cloneNode(false);
				while (tmp.hasChildNodes())
					tmp.removeChild(tmp.getFirstChild());

				NodeList children = node.getChildNodes();
				int i, j;

				j = 0;
				for (i = 0; i < children.getLength(); i++)
					if (children.item(i).getNodeName().equals(element_name)) {
						if (j == num)
							return children.item(i);
						j++;
					}
			}
		}
		return null;
	}

	public synchronized Node getSubTag(Node node, String element_name) {
		return getSubTag(node, element_name, 0);
	}

	public synchronized NodeList getSubTags(Node node, String element_name) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			// FIXME: figure out a faster way to create an empty node:
			Node tmp = node.cloneNode(false);
			while (tmp.hasChildNodes())
				tmp.removeChild(tmp.getFirstChild());

			NodeList children = node.getChildNodes();
			int i;

			for (i = 0; i < children.getLength(); i++)
				if (children.item(i).getNodeName().equals(element_name))
					tmp.appendChild(children.item(i));

			return tmp.getChildNodes();
		}
		return null;
	}

	public String getInterfaces() {
		return interface_list;
	}

	@SuppressWarnings("unused")
	public synchronized void parseXML(Node root) {
		try {
			int i;

			String currentSelectedDONodeID = null;

			DataObject dObj = DataObjectTable.getDO(selected_do);
			if (dObj != null)
				if (dObj.isNodeDescription())
					currentSelectedDONodeID = dObj.getNodeID();

			for (i = 0; i < DataObjectTable.getLength(); i++) {
				dObj = DataObjectTable.getDO(i);
				dObj.setAlive(false);
			}

			NodeList DOs = getSubTags(root, "dataobject");
			if (DOs != null) {
				parseDebug("Number of Data Objects: " + DOs.getLength());
				for (i = 0; i < DOs.getLength(); i++) {
					Node data = DOs.item(i);

					String id = getTagContent(data, "id");
					String rowid = getTagContent(data, "rowid");

					if (id != null && rowid != null) {
						String thisNode = getTagContent(data, "ThisNode");
						if (thisNode != null)
							parseDebug("Found thisNode: " + id + " rowid: "
									+ rowid);
						else
							parseDebug("Found DO: " + id + " rowid: " + rowid);
						DataObjectTable.ensure_do_is_in_table(id, rowid);
						dObj = DataObjectTable.getDO(DataObjectTable
								.indexByRowID(rowid));
						if (dObj != null) {
							if (dObj.isSpecialDO())
								dObj.setNew();
							if (thisNode != null)
								dObj.isThisNode(true);

							int j;

							dObj.setAlive(true);
							dObj.clearAttributes();

							if (dObj.isThisNode() && centerThisNode) {
								if (dObj.isNew())
									dObj.setCurrentPosition(new Coordinate(
											0.0f, 0.0f));
							}

							NodeList attributes = getSubTags(data, "attribute");
							for (j = 0; j < attributes.getLength(); j++) {
								Node attr = attributes.item(j);

								String name = getTagContent(attr, "name");
								String value = getTagContent(attr, "value");
								if (name != null && value != null) {
									dObj.addAttribute(name, value);
								}
							}
							String thumbnail = getTagContent(data, "Thumbnail");
							if (thumbnail != null) {
								dObj.setThumbnail(thumbnail);
							}

							if (dObj.isThisNode() && dObj.isNodeDescription()) {
								// Copy the color of this data object as the
								// color of this sensor node (and the top
								// ring)
								myColor = dObj.getDOColorRaw();
								node_id = dObj.getNodeID();
							}

							if (dObj.isThisNode()) {
								interface_list = "";
								NodeList interfaces = getSubTags(
										getSubTag(getSubTag(data, "xmlhdr"),
												"Node"), "Interface");
								if (interfaces != null) {
									for (j = 0; j < interfaces.getLength(); j++) {
										Node iface = interfaces.item(j);
										String type = getAttributeContent(
												iface, "type");
										if (type != null) {
											NodeList addresses = getSubTags(
													iface, "Address");
											int k;
											String addr = null;

											for (k = 0; k < addresses
													.getLength(); k++) {
												Node addy = addresses.item(k);

												String tmp = getTagContent(addy);

												if (tmp != null) {
													if (tmp.startsWith("eth://")
															&& type.equals("ethernet"))
														addr = tmp;
													if (tmp.startsWith("bt://")
															&& type.equals("bluetooth"))
														addr = tmp;
												}
											}
											if (addr != null) {
												String[] tmp = addr.split("/");
												addr = tmp[tmp.length - 1];
												interface_list = interface_list
														+ type + " " + addr
														+ " ";
											}
										}
									}
								}
							}

							if (dObj.isNodeDescription()) {
								DataObjectTable.associateNodeID(dObj);
								for (j = 0; j < DataObjectTable.getLength(); j++) {
									DataObject dObj2 = DataObjectTable.getDO(j);
									if (dObj2 != null)
										if (dObj2 != dObj
												&& dObj2.isNodeDescription())
											if (dObj2.getNodeID().equals(
													dObj.getNodeID())) {
												double dist;

												Coordinate old = dObj2
														.getCurrentPosition();
												Coordinate pos = (Coordinate) old
														.clone();
												// Move new NDs?
												if (false) {
													Random gen = new Random();

													do {
														pos.x += ((float) (gen
																.nextDouble()) - 0.5f) * 0.040f;
														pos.y += ((float) (gen
																.nextDouble()) - 0.5f) * 0.040f;
														dist = pos
																.distance(old);
													} while (dist > 0.020f
															|| dist < 0.010f);
												}
												dObj.setCurrentPosition(pos);
												dObj.setCalculatedPosition(pos);
												dObj.setDisplayedPosition(pos);
											}
								}
							}
						}
					} else {
						parseDebug("Ignored DO: " + id + " rowid: " + rowid);
					}
				}

			}

			NodeList maps = getSubTags(root, "map");
			if (maps != null) {
				parseDebug("Number of mappings: " + maps.getLength());
				for (i = 0; i < maps.getLength(); i++) {
					Node current_mapping = maps.item(i);

					String node_rowid;
					String dataobject_rowid;

					node_rowid = getTagContent(
							getSubTag(current_mapping, "node"), "rowid");
					dataobject_rowid = getTagContent(
							getSubTag(current_mapping, "dataobject"), "rowid");

					if (node_rowid != null && dataobject_rowid != null) {
						parseDebug("Found Node, node rowid: " + node_rowid
								+ " DO rowid: " + dataobject_rowid);
						dObj = DataObjectTable.getDO(DataObjectTable
								.indexByRowID(dataobject_rowid));
						if (dObj != null) {
							dObj.setNodeRowID(node_rowid);
						}
					} else {
						parseDebug("Ignored Node, node rowid: " + node_rowid
								+ " DO rowid: " + dataobject_rowid);
					}
				}
			}

			do_links = new TreeMap<String, TreeMap<String, Float>>();
			NodeList relations = getSubTags(root, "relation");
			if (relations != null) {
				parseDebug("Number of relations: " + relations.getLength());
				for (i = 0; i < relations.getLength(); i++) {
					String ratioStr = getTagContent(relations.item(i), "ratio");
					if (ratioStr != null)
						Float.parseFloat(ratioStr);

					NodeList matching_attrs = getSubTags(relations.item(i),
							"matching_attribute");

					int numMatches = matching_attrs.getLength();

					NodeList dObjs = getSubTags(relations.item(i), "dataobject");

					if (dObjs != null) {
						int j, k;
						String str_do1_num_attrs, str_do2_num_attrs;
						int do1_num_attrs = 0;
						String do1_rowid, do2_rowid;

						for (j = 0; j < dObjs.getLength(); j++) {
							do1_rowid = getTagContent(dObjs.item(j), "rowid");

							str_do1_num_attrs = getTagContent(dObjs.item(j),
									"num_attrs");

							if (str_do1_num_attrs != null)
								do1_num_attrs = Integer
										.parseInt(str_do1_num_attrs);

							if (do1_rowid != null)
								for (k = j + 1; k < dObjs.getLength(); k++) {
									do2_rowid = getTagContent(dObjs.item(k),
											"rowid");

									str_do2_num_attrs = getTagContent(
											dObjs.item(k), "num_attrs");

									if (str_do2_num_attrs != null)
										Integer
												.parseInt(str_do2_num_attrs);

									if (do2_rowid != null) {
										parseDebug("Found DO-DO relation:"
												+ " rowid 1: " + do1_rowid
												+ " rowid 2: " + do2_rowid);
										if (numMatches > 0) {
											addDOLink(do1_rowid, do2_rowid, 100
													* numMatches
													/ do1_num_attrs);
											/*
											 * if (do1_num_attrs >
											 * do2_num_attrs) { if
											 * (do1_num_attrs > 0) {
											 * addDOLink(do1_rowid, do2_rowid,
											 * 100*numMatches/do1_num_attrs ); }
											 * } else { if (do2_num_attrs > 0) {
											 * addDOLink(do2_rowid, do2_rowid,
											 * 100*numMatches/do2_num_attrs ); }
											 * }
											 */
										}
									}
								}
						}
					}
				}
			}

			for (i = 0; i < DataObjectTable.getLength(); i++) {
				DataObjectTable.getDO(i).setNodeIsVisible(false);
			}
			NodeList neighbors = getSubTags(root, "neighbor");
			if (neighbors != null) {
				for (i = 0; i < neighbors.getLength(); i++) {
					String node_id = getTagContent(neighbors.item(i));
					parseDebug("Found online node: " + node_id);
					if (node_id != null) {
						dObj = DataObjectTable.getDO(DataObjectTable
								.indexByNodeID(node_id));
						if (dObj != null) {
							parseDebug("Color: " + dObj.getNodeColor(ttl > 0));
							dObj.setNodeIsVisible(true);
							parseDebug("Color: " + dObj.getNodeColor(ttl > 0));
						}
					}
				}
			}

			metrics = new TreeMap<String, TreeMap<String, Float>>();

			NodeList vectors = getSubTags(getSubTag(root, "routingtable"),
					"vector");
			for (i = 0; i < vectors.getLength(); i++) {
				Node vector;
				int j;
				NodeList metrics_;
				String from_node;

				vector = vectors.item(i);
				metrics_ = getSubTags(vector, "metric");
				from_node = getTagContent(getSubTag(vector, "name"));
				for (j = 0; j < metrics_.getLength(); j++) {
					String to_node;
					Node metric = metrics_.item(j);
					float value;

					to_node = getTagContent(getSubTag(metric, "name"));
					value = Float.parseFloat(getTagContent(metric, "value"));

					addMetric(from_node, to_node, value);
				}
			}
			/*
			 * This is a hack to make the routing data between the local node
			 * and itself the maximum so that all arrows pointing to it are
			 * colored "worse".
			 * 
			 * This will only work as long as the maximum metric is 1.0.
			 */
			addMetric(DataObjectTable.getThisNodeDO().getNodeID(),
					DataObjectTable.getThisNodeDO().getNodeID(), 1.0f);

			if (currentSelectedDONodeID != null) {
				selected_do = DataObjectTable
						.indexByNodeID(currentSelectedDONodeID);
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	private void addDOLink(String I, String J, float ratio) {
		doAddDOLink(I, J, ratio);
		doAddDOLink(J, I, ratio);
	}

	private void doAddDOLink(String I, String J, float ratio) {
		TreeMap<String, Float> set;

		set = do_links.get(I);
		if (set == null) {
			set = new TreeMap<String, Float>();
			do_links.put(I, set);
		}
		set.put(J, new Float(ratio));
	}

	private void addMetric(String I, String J, float metric) {
		TreeMap<String, Float> map;

		map = metrics.get(I);
		if (map == null) {
			map = new TreeMap<String, Float>();
			metrics.put(I, map);
		}
		map.put(J, new Float(metric));
	}

	private float getMaxMetric(String I, String J) {
		float ij, ji;

		ij = getMetric(I, J);
		ji = getMetric(J, I);
		if (ij < ji)
			return ji;
		return ij;
	}

	@SuppressWarnings("unused")
	private float getMinMetric(String I, String J) {
		float ij, ji;

		ij = getMetric(I, J);
		ji = getMetric(J, I);
		if (ij > ji && ji != 0.0f)
			return ji;
		return ij;
	}

	private float getMetric(String I, String J) {
		TreeMap<String, Float> map;
		if (I == null)
			return 0.0f;

		map = metrics.get(I);
		if (map == null)
			return 0.0f;

		Float f = map.get(J);
		if (f == null)
			return 0.0f;
		return f.floatValue();
	}

	private static final double forceMinDistance = 0.001;
	private static final double forceMaxDistance = 1.0;

	@SuppressWarnings({ "unchecked" })
	public synchronized Coordinate getForceForPosition(Coordinate pos,
			DataObject dObj) {
		int i;
		Coordinate retval = new Coordinate();

		// Give the edge of the circle a repelling force:

		Coordinate posProject = pos.clone();
		// Calculate the distance to the origin:
		double D = posProject.length();
		if (!centerThisNode || !dObj.isThisNode()) {
			// Check that the position is outside the "dead zone" in the
			// center.
			if (D > forceMinDistance) {
				// Normalize (and reverse) the projected point:
				posProject.scale(-1.0 / D);
				// Calculate the distance from the original point to the
				// edge:
				D = 1.0 - D;
				// Scale the projected point to become a repelling force
				// from
				// the edge:
				posProject.scale(1.0 / (D * D) - 1.0);

				// Scale the force down a bit:
				posProject.scale(0.15);

				retval.add(posProject);
			}
		} else {
			// Check that the position is outside the center.
			if (D > 0.0) {
				// Normalize (and reverse) the projected point:
				posProject.scale(-1.0 / D);

				posProject.scale(D);

				retval.add(posProject);
			}
		}

		// Don't act with any other forces for the "this node" node description
		if (!centerThisNode || !dObj.isThisNode()) {

			// Give all the data objects repelling forces:

			for (i = 0; i < DataObjectTable.getLength(); i++) {
				DataObject otherObject = DataObjectTable.getDO(i);
				if (otherObject.isVisible()) {
					// No repelling forces between an object and itself:
					if (otherObject != dObj) {
						Coordinate repel = new Coordinate();
						double F;

						// repel = other - current
						repel.sub(otherObject.getCurrentPosition(), pos);

						// F = |repel|
						F = (new Coordinate()).distance(repel);

						if (F > forceMinDistance && F < forceMaxDistance) {
							// Normalize repel:
							repel.scale(1.0f / F);

							if (
							// No 1/F repelling forces two copies of the same ND
							!(otherObject.isNodeDescription()
									&& dObj.isNodeDescription() && otherObject
									.getNodeID().equals(dObj.getNodeID()))) {
								// Set to repel:
								repel.scale(1.0f / F - 1.0f / forceMaxDistance);
							}

							// Scaling constant:
							repel.scale(0.25f);

							retval.sub(repel);
						}
					}
				}
			}

			// Give those data objects that are linked to this one attracting
			// forces:
			if (dObj.getRowID() != null) {
				TreeMap<String, Float> set = do_links.get(dObj.getRowID());
				if (set != null) {
					Object[] arr = set.entrySet().toArray();

					for (i = 0; i < arr.length; i++) {
						Map.Entry<String, Float> entry = (Map.Entry<String, Float>) (arr[i]);
						DataObject otherObject = DataObjectTable
								.getDO(DataObjectTable.indexByRowID(entry
										.getKey()));
						if (otherObject != null) {
							if (otherObject.isVisible()) {
								if (
								// No attracting force between an object and
								// itself:
								otherObject != dObj &&
								// No attracting forces between non-NDs unless
								// those
								// links are shown:
										(show_do_do_links
												|| dObj.isNodeDescription() || otherObject
													.isNodeDescription())) {
									Coordinate attract = new Coordinate();
									double F;

									// attract = other - current
									attract.sub(
											otherObject.getCurrentPosition(),
											pos);

									// F = |attract|
									F = (new Coordinate()).distance(attract);

									if (F > forceMinDistance) {
										// Normalize attract:
										attract.scale(1.0 / F);

										// Set to attract:
										attract.scale(F);

										// Scaling constant:
										attract.scale(0.5);

										retval.add(attract);
									}
								}
							}
						}
					}
				}
			}

			// Give those node descriptions that are linked to this one
			// attracting
			// forces:
			if (dObj.isNodeDescription() && false) {
				TreeMap<String, Float> map = metrics.get(dObj.getNodeID());
				if (map != null) {
					Object[] arr = map.entrySet().toArray();

					for (i = 0; i < arr.length; i++) {
						Map.Entry<String, Float> entry = (Map.Entry<String, Float>) (arr[i]);

						DataObject otherObject = DataObjectTable
								.getDO(DataObjectTable.indexByRowID(entry
										.getKey()));
						if (otherObject != null) {
							if (otherObject.isVisible()) {
								if (otherObject != dObj) {
									Coordinate attract = new Coordinate();
									double F;

									// attract = other - current
									attract.sub(
											otherObject.getCurrentPosition(),
											pos);

									// F = |attract|
									F = (new Coordinate()).distance(attract);

									if (F > forceMinDistance) {
										// Normalize attract:
										attract.scale(1.0 / F);

										// Set to attract:
										attract.scale(F);

										// Scaling constant * metric:
										attract.scale(1.5 * getMaxMetric(
												dObj.getNodeID(),
												otherObject.getNodeID()));

										retval.add(attract);
									}
								}
							}
						}
					}
				}
			}
		}

		return retval;
	}

	public synchronized void advanceAnimation() {
		int i;
		Boolean do_redraw = false;
		if (send_failure_countdown > 0)
			send_failure_countdown--;
		if (ttl > 0) {
			ttl--;
			if (ttl == 0) {
				getXML(null);
				setAlive(false);
			}
		}

		if (xmlGetter.latestxml != null) {
			Node xml = xmlGetter.latestxml;
			xmlGetter.latestxml = null;
			if (xml != null) {
				try {
					parseXML(xml);
				} catch (Exception e) {
					System.out.println(e);
					e.printStackTrace();
				}
			}
		}

		try {
			for (i = 0; i < DataObjectTable.getLength(); i++) {
				DataObject dObj = DataObjectTable.getDO(i);

				if (dObj != null) {
					if (!dObj.isBeingDragged()) {
						Coordinate pos = dObj.getCurrentPosition();
						Coordinate F = getForceForPosition(pos, dObj);

						F.scale(0.01);
						Coordinate newPos = new Coordinate();
						newPos.add(pos);
						newPos.add(F);
						double dist = newPos.length();
						if (dist >= 0.9) {
							newPos.scale(0.9 / dist);
						}

						dObj.setCalculatedPosition(newPos);
						if (dObj.getDisplayedPosition().distance(
								dObj.getCalculatedPosition()) > 0.001) {
							do_redraw = true;
						}
					}
					if (dObj.isAnimating())
						do_redraw = true;
				}
			}

			for (i = 0; i < DataObjectTable.getLength(); i++) {
				DataObject dObj = DataObjectTable.getDO(i);
				dObj.setCurrentPosition(dObj.getCalculatedPosition());
				if (do_redraw) {
					dObj.setDisplayedPosition(dObj.getCalculatedPosition());
				}
			}

			if (do_redraw) {
				for (i = 0; i < DataObjectTable.getLength(); i++) {
					DataObject dObj = DataObjectTable.getDO(i);
					if (dObj != null)
						dObj.updateState();
				}
				DataObjectTable.updateState();
			}

			String currentSelectedDOID = null;
			{
				DataObject dObj = DataObjectTable.getDO(selected_do);
				if (dObj != null)
					currentSelectedDOID = dObj.getID();
			}

			DataObjectTable.compact();

			if (currentSelectedDOID != null) {
				selected_do = DataObjectTable.indexByID(currentSelectedDOID);
			}

			for (i = 0; i < animations.size(); i++) {
				Animation a;
				a = animations.get(i);
				a.advance();
				if (!a.isRunning()) {
					animations.remove(i);
					i--;
				}
			}

		} catch (Exception e) {
			LOG.debug("" + e);
			e.printStackTrace();
		}
		;
	}

	public synchronized void handleClick(Coordinate c, int mouseButton,
			boolean shiftClick) {
		int i;

		for (i = 0; i < DataObjectTable.getLength(); i++) {
			DataObject dObj = DataObjectTable.getDO(i);

			if (dObj != null) {
				if (mouseButton == 1) // Left mouse button
				{
					Rect r;
					r = dObj.getDORect();
					if (r != null)
						if (r.contains(c)) {
							if (!shiftClick) {
								handleClick(c, 3, false);
								dObj.select(true);
							} else {
								dObj.select(!dObj.isSelected());
							}
							return;
						}
					r = dObj.getNodeRect();
					if (r != null)
						if (r.contains(c)) {
							if (!shiftClick) {
								handleClick(c, 3, false);
								dObj.select(true);
							} else {
								dObj.select(!dObj.isSelected());
							}
							return;
						}
				} else if (mouseButton == 3) // Right mouse button
				{
					dObj.select(false);
				}
			}
		}
	}

	public synchronized void startDrag(Coordinate c, int mouseButton) {
		if (mouseButton != 1)
			return;
		int i;

		for (i = 0; i < DataObjectTable.getLength(); i++) {
			DataObject dObj = DataObjectTable.getDO(i);

			if (dObj != null) {
				Rect r;
				r = dObj.getDORect();
				if (r != null)
					if (r.contains(c)) {
						dObj.startDragging(false);
						dragTo(dObj, c, mouseButton);
						return;
					}
				r = dObj.getNodeRect();
				if (r != null)
					if (r.contains(c)) {
						dObj.startDragging(true);
						dragTo(dObj, c, mouseButton);
						return;
					}
			}
		}
	}

	public synchronized void dragTo(DataObject dObj, Coordinate c,
			int mouseButton) {
		if (mouseButton != 1)
			return;
		if (dObj.isDraggedByNode())
			c = revAsNodePos(c);
		else
			c = revAsDOPos(c);
		double dist = new Coordinate().distance(c);
		double dragBounds = 0.90;
		if (dist > dragBounds)
			c.scale(dragBounds / dist);
		dObj.setCurrentPosition(c);
		dObj.setDisplayedPosition(c);
		dObj.setCalculatedPosition(c);
	}

	public synchronized void dragTo(Coordinate c, int mouseButton) {
		if (mouseButton != 1)
			return;
		int i;

		for (i = 0; i < DataObjectTable.getLength(); i++) {
			DataObject dObj = DataObjectTable.getDO(i);

			if (dObj != null) {
				if (dObj.isBeingDragged()) {
					dragTo(dObj, c, mouseButton);
					return;
				}
			}
		}
	}

	public synchronized void releaseDrag(int mouseButton) {
		if (mouseButton != 1)
			return;
		int i;

		for (i = 0; i < DataObjectTable.getLength(); i++) {
			DataObject dObj = DataObjectTable.getDO(i);

			if (dObj != null) {
				if (dObj.isBeingDragged()) {
					dObj.stopDragging();
					return;
				}
			}
		}
	}
}
