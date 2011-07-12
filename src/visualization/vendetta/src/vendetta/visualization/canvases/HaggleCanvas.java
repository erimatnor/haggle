/* Copyright (c) 2009 Uppsala Universitet.
 * All rights reserved.
 * 
 */

package vendetta.visualization.canvases;

import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.net.*;

import vendetta.MonitorNode;
import vendetta.Vendetta;
import vendetta.vconfig.VConfigReader;
import vendetta.vconfig.VSettings;

import vendetta.monitored_network.haggle.*;

public class HaggleCanvas extends VendettaCanvas implements MouseWheelListener {
	private class Animation {
		public class EndPoint {
			public SensorNode node;

			EndPoint(SensorNode _node) {
				node = _node;
			}
		}

		public EndPoint startPoint;
		public EndPoint endPoint;
		public long length, frame;
		public Color col;

		Animation(SensorNode fromNode, SensorNode toNode, long length, Color col) {
			if (fromNode == null)
				throw new RuntimeException("no \"from\" node.");
			if (toNode == null)
				throw new RuntimeException("no \"to\" node.");

			this.startPoint = new EndPoint(fromNode);
			this.endPoint = new EndPoint(toNode);
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

	public static final double default_line_width = 0.005;
	public static final boolean showOutline = true;

	private class SensorNodeComparator implements Comparator<SensorNode> {
		public int compare(SensorNode o1, SensorNode o2) {
			return o1.getID() - o2.getID();
		}
	}

	private class SensorWindow {
		public SensorNode node;
		public Rect contentBounds;
		public Rect closeBounds;
		public Rect dragBounds;
		public Rect allBounds;
		public int type;
		public static final int type_internal_state = 1;
		public static final int type_connectivity = 2;
		public static final int type_window_array = 3;
		public TreeMap<SensorNode, SensorWindow> subwindows;
		public SensorWindow dragSubWindow;
		public double window_height;
		public double scale;

		private SensorWindow(SensorNode node, int type) {
			this.type = type;
			this.node = node;
			clear();
		}

		public void clear() {
			window_height = 0.0;
			subwindows = new TreeMap<SensorNode, SensorWindow>(
					new SensorNodeComparator());
			dragSubWindow = null;
		}

		SensorWindow(SensorNode node, Coordinate center, double width,
				double height, int type) {
			this(node, type);
			Rect content = new Rect(center.x - width / 2,
					center.y - height / 2, center.x + width / 2, center.y
							+ height / 2);
			Rect drag = new Rect(content.left - 0.002, content.top - 0.02,
					content.right + 0.002, content.bottom + 0.002);
			Rect close = new Rect(drag.left + 0.001, drag.top + 0.001,
					drag.left + 0.019, drag.top + 0.019);
			setBounds(content, close, drag);
		}

		SensorWindow(SensorNode node, Rect contentBounds, Rect closeBounds,
				Rect dragBounds, int type) {
			this(node, type);
			setBounds(contentBounds, closeBounds, dragBounds);
		}

		void setBounds(Rect contentBounds, Rect closeBounds, Rect dragBounds) {
			this.contentBounds = contentBounds;
			this.closeBounds = closeBounds;
			this.dragBounds = dragBounds;
			if (contentBounds != null) {
				allBounds = new Rect(contentBounds);

				if (dragBounds != null) {
					if (allBounds.left > dragBounds.left)
						allBounds.left = dragBounds.left;
					if (allBounds.top > dragBounds.top)
						allBounds.top = dragBounds.top;
					if (allBounds.right < dragBounds.right)
						allBounds.right = dragBounds.right;
					if (allBounds.bottom < dragBounds.bottom)
						allBounds.bottom = dragBounds.bottom;
				}
				if (closeBounds != null) {
					if (allBounds.left > closeBounds.left)
						allBounds.left = closeBounds.left;
					if (allBounds.top > closeBounds.top)
						allBounds.top = closeBounds.top;
					if (allBounds.right < closeBounds.right)
						allBounds.right = closeBounds.right;
					if (allBounds.bottom < closeBounds.bottom)
						allBounds.bottom = closeBounds.bottom;
				}

				allBounds = new Rect(allBounds);
			} else {
				allBounds = null;
			}
		}

		boolean isInWindow(Coordinate c) {
			if (allBounds != null)
				return allBounds.contains(c);
			return false;
		}

		private Coordinate toPos(Coordinate p) {
			return new Coordinate(((p.x / 2.0) * window_height + 0.5),
					((p.y / 2.0)) * window_height + 0.5);
		}

		private Coordinate revPos(Coordinate p) {
			return new Coordinate((p.x - 0.5) * 2.0 / window_height,
					((p.y - 0.5) / window_height) * 2.0);
		}
	}

	private class NodeLink {
		public int i;
		public int j;

		public NodeLink(int i, int j) {
			if (i < j) {
				this.i = i;
				this.j = j;
			} else {
				this.j = i;
				this.i = j;
			}
		}

		public String iName() {
			return "node-" + i;
		}

		public String jName() {
			return "node-" + j;
		}
	}

	private class NodeLinkComparator implements Comparator<NodeLink> {
		public int compare(NodeLink o1, NodeLink o2) {
			if (o1.i == o2.i)
				if (o1.j == o2.j)
					return 0;
				else
					return o1.j - o2.j;
			else
				return o1.i - o2.i;
		}
	}

	private SensorWindow rootWindow;
	private TreeSet<NodeLink> links;

	private HaggleLogHandler lh;
	private Coordinate mousePoint;
	// 1 = clicking
	// 2 = dragging
	// 3 = invalid drag, ignore further dragging
	// 4 = dragging window
	private int mouseAction;
	private int mouseButton;
	private SensorNode mouseNode;
	private double connectivityScale;
	private boolean try_report_nodes;
	private ArrayList<Animation> animations;

	@SuppressWarnings({ "unchecked" })
	public HaggleCanvas() {
		super();

		rootWindow = null;

		clear();
		// setIgnoreRepaint(true);
		// createBufferStrategy(2);
		mouseNode = null;
		mousePoint = null;
		mouseAction = 0;
		connectivityScale = 0.9;
		animations = new ArrayList<Animation>();

		initFromConfigFile(Vendetta.CONFIG_PATH + "HaggleCanvas");
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				HaggleCanvas.this.mouseDragged(e);
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				HaggleCanvas.this.mouseReleased(e);
			}
		});
		addMouseWheelListener(this);
	}

	public void addPacketAnimation(String fromNode, String toNode) {
		if (fromNode.equals(toNode))
			return;

		int i;
		SensorNode[] entries = getNodes();
		SensorNode _fromNode, _toNode;

		_fromNode = null;
		_toNode = null;
		for (i = 0; i < entries.length
				&& (_fromNode == null || _toNode == null); i++) {
			SensorNode Node = entries[i];
			if (Node.getNodeID().equals(fromNode))
				_fromNode = Node;
			if (Node.getNodeID().equals(toNode))
				_toNode = Node;
		}
		if (_fromNode != null && _toNode != null) {
			animations.add(new Animation(_fromNode, _toNode, 15, new Color(
					1.0f, 1.0f, 1.0f)));
		}
	}

	/**
	 * Parses the configuration file and constructs the floors.
	 * 
	 * @param configFile
	 * @throws RuntimeException
	 *             If a parse error occurrs.
	 */
	private void initFromConfigFile(String configFile) {
		VConfigReader configReader = new VConfigReader();
		VSettings config = configReader.parseFile(configFile);
		if (config == null) {
			throw new RuntimeException(
					"Failed to read canvas configuration file.");
		}

		name = "Circle canvas (" + config.getSettingLine("canvas_name") + ")";
		String mode = config.getSettingLine("mode");
		if (mode == null) {
			throw new RuntimeException(
					"Failed to read canvas configuration file. No <mode> tag.");
		}

		if (mode.equals("window array"))
			rootWindow = new SensorWindow(null, null, null, null,
					SensorWindow.type_window_array);
		else if (mode.equals("connectivity"))
			rootWindow = new SensorWindow(null, null, null, null,
					SensorWindow.type_connectivity);
		else
			throw new RuntimeException(
					"Failed to read canvas configuration file, mode = " + mode
							+ ".");
	}

	/**
	 * Prepare the Java 3D-related stuff.
	 */
	protected void createSceneGraph(int id) {
		createBufferStrategy(2);
	}

	private void addLink(int i, int j) {
		int l;
		if (i > j) {
			l = i;
			i = j;
			j = l;
		}

		NodeLink[] link = links.toArray(new NodeLink[0]);
		for (l = 0; l < link.length; l++)
			if (link[l].i == i && link[l].j == j)
				return;

		links.add(new NodeLink(i, j));
	}

	private void removeLink(int i, int j) {
		int l;
		if (i > j) {
			l = i;
			i = j;
			j = l;
		}

		NodeLink[] link = links.toArray(new NodeLink[0]);
		for (l = 0; l < link.length; l++)
			if (link[l].i == i && link[l].j == j) {
				links.remove(link[l]);
				return;
			}
	}

	public synchronized void handleEvent(String evt) {
		try {
			String[] split = evt.split("\\s");

			if (split.length < 3)
				LOG.debug("Bogus event: " + evt);
			else {
				if ("CONTROLLER_EVENT".equals(split[2])) {
					// handle controller events:
					if (split.length < 5)
						LOG.debug("Bogus event: " + evt);
					else {
						if ("NODE_UP".equals(split[4])) {
							lh.getParent().addNode(split[5]);
						} else if ("NODE_DOWN".equals(split[4])) {
							lh.getParent().deleteNode(
									lh.getSensorNode(split[5]));
						} else if ("LINK_UP".equals(split[4])) {
							addLink(Integer.parseInt(split[5]),
									Integer.parseInt(split[6]));
						} else if ("LINK_DOWN".equals(split[4])) {
							removeLink(Integer.parseInt(split[5]),
									Integer.parseInt(split[6]));
						} else
							LOG.debug("Bogus event: " + evt);
					}
				}
				// Ignore non-controller events (for now).
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		;
	}

	/*
	 * void drawLines(DrawingSurface ds) { Color gray = new
	 * Color(0.75f,0.75f,0.75f); ds.drawLine( allBounds.topLeft(),
	 * node.getDisplayedPosition(), HaggleCanvas.default_line_width/2, gray);
	 * ds.drawLine( allBounds.topRight(), node.getDisplayedPosition(),
	 * HaggleCanvas.default_line_width/2, gray); ds.drawLine(
	 * allBounds.bottomLeft(), node.getDisplayedPosition(),
	 * HaggleCanvas.default_line_width/2, gray); ds.drawLine(
	 * allBounds.bottomRight(), node.getDisplayedPosition(),
	 * HaggleCanvas.default_line_width/2, gray); }
	 * 
	 * void draw(DrawingSurface ds) { ds.fillRect(dragBounds, new Color(0,0,0));
	 * ds.fillRect(closeBounds, new Color(1.0f,0.75f,0.75f));
	 * ds.pushBounds(contentBounds); node.doRender(ds); clickRect =
	 * ds.popBounds(); }
	 */
	private static final double forceMinDistance = 0.001;
	private static final double forceMaxDistance = 1.0;

	public synchronized Coordinate getForceForPosition(Coordinate pos,
			SensorNode thisNode, SensorNode[] node, NodeLink[] link) {
		int i;
		Coordinate retval = new Coordinate();

		// Give the center an attracting force:
		{
			Coordinate posProject = pos.clone();
			double F = posProject.length();
			// Check that the position is outside the "dead zone" in the center.
			if (F > forceMinDistance) {
				posProject.scale(-0.15);
				retval.add(posProject);
			}
		}

		// Give all other nodes repelling forces:
		for (i = 0; i < node.length; i++) {
			// No repelling forces between an object and itself:
			if (node[i] != thisNode) {
				Coordinate repel = new Coordinate();
				double F;

				// repel = other - current
				repel.sub(node[i].getCurrentPosition(), pos);

				// F = |repel|
				F = (new Coordinate()).distance(repel);

				if (F > forceMinDistance && F < forceMaxDistance) {
					// Normalize repel:
					repel.scale(1.0f / F);

					// Set to repel:
					repel.scale(1.0f / F - 1.0f / forceMaxDistance);

					// Scaling constant:
					repel.scale(0.25f);

					retval.sub(repel);
				}
			}
		}

		{
			String id = thisNode.getNodeName();
			for (i = 0; i < link.length; i++) {
				// Go through all links and give the objects linked to this one
				// attractive forces:
				if (link[i].iName().equals(id) || link[i].jName().equals(id)) {
					// Figure out which object is the other object:
					String otherid = null;
					int j;
					SensorNode otherObject = null;
					if (link[i].iName().equals(id))
						otherid = link[i].jName();
					else
						otherid = link[i].iName();
					for (j = 0; j < node.length; j++)
						if (node[j].getNodeName().equals(otherid)) {
							otherObject = node[j];
							j = node.length;
						}

					if (otherObject != null) {
						Coordinate attract = new Coordinate();
						double F;

						// attract = other - current
						attract.sub(otherObject.getCurrentPosition(), pos);

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
		return retval;
	}

	private SensorNode[] getNodes() {
		if (lh != null) {
			int i, j = 0;
			// Figure out how many nodes there actually are:
			for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
				if (Vendetta.getMonitorNode(i) != null)
					j++;
			}
			// Fill an array with the nodes:
			SensorNode[] node = new SensorNode[j];
			j = 0;
			for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
				if (Vendetta.getMonitorNode(i) != null) {
					node[j] = lh.getSensorNode(i);
					j++;
				}
			}

			return node;
		}
		return new SensorNode[0];
	}

	private void animateSensorNodes(SensorNode[] node) {
		int i;
		NodeLink[] link = links.toArray(new NodeLink[0]);

		// Calculate new positions for all nodes:
		for (i = 0; i < node.length; i++) {
			// Don't give dragged nodes a new position:
			if (!node[i].isBeingDragged()) {
				Coordinate pos = node[i].getCurrentPosition();
				Coordinate F = getForceForPosition(pos, node[i], node, link);

				F.scale(0.02);
				Coordinate newPos = new Coordinate();
				newPos.add(pos);
				newPos.add(F);
				double dist = newPos.length();
				if (dist > 0.9) {
					newPos.scale(0.9 / dist);
				}

				node[i].setCalculatedPosition(newPos);
			}
		}

		// Move nodes:
		for (i = 0; i < node.length; i++) {
			node[i].setCurrentPosition(node[i].getCalculatedPosition());
			if (node[i].getDisplayedPosition().distance(
					node[i].getCalculatedPosition()) > 0.001) {
				node[i].setDisplayedPosition(node[i].getCalculatedPosition());
			}
		}

		// Advance animations:
		for (i = 0; i < animations.size(); i++)
			animations.get(i).advance();
		// Delete dead animations:
		for (i = animations.size() - 1; i >= 0; i--)
			if (!animations.get(i).isRunning())
				animations.remove(i);
	}

	Coordinate squareToPos(Coordinate p) {
		return new Coordinate(p.x / 2.0 + 0.5, p.y / 2.0 + 0.5);
	}

	Coordinate squareRevPos(Coordinate p) {
		return new Coordinate((p.x - 0.5) * 2.0, (p.y - 0.5) * 2.0);
	}

	@SuppressWarnings({ "unchecked" })
	private void renderConnectivityWindow(DrawingSurface ds, SensorWindow win,
			SensorNode[] node) {
		ds.fillRect(0, 0, 1, 1, Color.white);

		win.window_height = ds.getHeight();
		// Get ourselves a square-looking drawing area:
		ds.pushBounds((1.0 - connectivityScale) / 2.0,
				(win.window_height - connectivityScale)
						/ (2.0 * win.window_height),
				(1.0 + connectivityScale) / 2.0,
				(win.window_height + connectivityScale)
						/ (2.0 * win.window_height));

		{
			Coordinate c = win.toPos(new Coordinate());
			ds.pushBounds(c.x - win.window_height / 2.0, c.y
					- win.window_height / 2.0, c.x + win.window_height / 2.0,
					c.y + win.window_height / 2.0);
		}

		boolean should_move_objects;
		int i, j;

		// Draw boundrary circle:
		ds.fillCircle(new Coordinate(0.5, 0.5), 0.9, 0.001 / connectivityScale,
				new Color(0.75f, 0.75f, 0.75f), Color.white);
		// Draw origin:
		ds.fillCircle(new Coordinate(0.5, 0.5), 0.002 / connectivityScale, 0.0,
				null, new Color(0.75f, 0.75f, 0.75f));

		Object[] entries = win.subwindows.entrySet().toArray();

		// draw lines to subwindows:
		for (i = 0; i < entries.length; i++) {
			SensorWindow Win = ((Map.Entry<SensorNode, SensorWindow>) (entries[i]))
					.getValue();
			SensorNode Node = ((Map.Entry<SensorNode, SensorWindow>) (entries[i]))
					.getKey();
			Color grey = new Color(0.75f, 0.75f, 0.75f);
			Coordinate nodePos = Node.getDisplayedPosition();
			nodePos = squareToPos(nodePos);

			if (!Win.allBounds.contains(nodePos)) {
				Coordinate leftMost, lM_n, rightMost, rM_n, tmp, tmp_n;
				leftMost = null;
				rightMost = null;
				lM_n = null;
				rM_n = null;
				for (j = 0; j < 4; j++) {
					switch (j) {
					default:
					case 0:
						tmp = Win.allBounds.topLeft().clone();
						break;
					case 1:
						tmp = Win.allBounds.topRight().clone();
						break;
					case 2:
						tmp = Win.allBounds.bottomLeft().clone();
						break;
					case 3:
						tmp = Win.allBounds.bottomRight().clone();
						break;
					}
					tmp.sub(nodePos);
					tmp_n = tmp.clone();
					tmp_n.normalize();
					if (lM_n == null || tmp_n.cross(lM_n) < 0) {
						lM_n = tmp_n;
						leftMost = tmp;
					}
					if (rM_n == null || tmp_n.cross(rM_n) > 0) {
						rM_n = tmp_n;
						rightMost = tmp;
					}
				}

				if (leftMost != null) {
					leftMost.add(nodePos);
					ds.drawLine(leftMost, nodePos, default_line_width / 3.0,
							grey);
				}
				if (leftMost != null) {
					rightMost.add(nodePos);
					ds.drawLine(rightMost, nodePos, default_line_width / 3.0,
							grey);
				}
			}
		}

		// Draw real connectivity:
		boolean[][] link_drawn = new boolean[node.length][node.length];
		for (i = 0; i < node.length; i++)
			for (j = 0; j < node.length; j++)
				link_drawn[i][j] = false;

		NodeLink[] link = links.toArray(new NodeLink[0]);
		for (i = 0; i < link.length; i++) {
			SensorNode n1, n2;

			n1 = null;
			n2 = null;
			for (j = 0; j < node.length && (n1 == null || n2 == null); j++) {
				if (node[j].getNodeName().equals("node-" + link[i].i))
					n1 = node[j];
				if (node[j].getNodeName().equals("node-" + link[i].j))
					n2 = node[j];
			}

			if (n1 != null && n2 != null) {
				ds.drawLine(squareToPos(n1.getDisplayedPosition()),
						squareToPos(n2.getDisplayedPosition()),
						default_line_width * 2, Color.black);
			}
		}

		for (i = 0; i < link.length; i++) {
			SensorNode n1, n2;
			int n1i, n2i;

			n1 = null;
			n1i = -1;
			n2 = null;
			n2i = -1;
			for (j = 0; j < node.length && (n1 == null || n2 == null); j++) {
				if (node[j].getNodeName().equals("node-" + link[i].i)) {
					n1 = node[j];
					n1i = j;
				}
				if (node[j].getNodeName().equals("node-" + link[i].j)) {
					n2 = node[j];
					n2i = j;
				}
			}

			if (n1 != null && n2 != null) {
				Coordinate a, b;
				a = n1.getDisplayedPosition().clone();
				b = n2.getDisplayedPosition().clone();
				b.sub(a);
				b.scale(0.51);
				b.add(a);
				a = squareToPos(a);
				b = squareToPos(b);
				if (n1.isConnectedTo(n2.getNodeID())) {
					ds.drawLine(a, b, default_line_width, Color.green);
				} else {
					ds.drawLine(a, b, default_line_width, Color.red);
				}
				link_drawn[n1i][n2i] = true;
				a = n2.getDisplayedPosition().clone();
				b = n1.getDisplayedPosition().clone();
				b.sub(a);
				b.scale(0.51);
				b.add(a);
				a = squareToPos(a);
				b = squareToPos(b);
				if (n2.isConnectedTo(n1.getNodeID())) {
					ds.drawLine(a, b, default_line_width, Color.green);
				} else {
					ds.drawLine(a, b, default_line_width, Color.red);
					link_drawn[n2i][n1i] = true;
				}
				link_drawn[n2i][n1i] = true;
			}
		}

		// Draw haggle connectivity:
		for (i = 0; i < node.length; i++) {
			for (j = 0; j < node.length; j++) {
				if (j != i && !link_drawn[i][j]) {
					Coordinate a, b;
					a = node[i].getDisplayedPosition().clone();
					b = node[j].getDisplayedPosition().clone();
					b.sub(a);
					b.scale(0.51);
					b.add(a);
					a = squareToPos(a);
					b = squareToPos(b);
					if (node[i].isConnectedTo(node[j].getNodeID())) {
						ds.drawLine(a, b, default_line_width, Color.green);
					} else if (node[j].isConnectedTo(node[i].getNodeID())) {
						ds.drawLine(a, b, default_line_width, Color.red);
					}
				}
			}
		}

		// Draw data objects:
		for (i = 0; i < animations.size(); i++) {
			Animation a = animations.get(i);
			Coordinate p, q, r, c;
			double s, tmp;

			p = a.startPoint.node.getDisplayedPosition();
			q = a.endPoint.node.getDisplayedPosition();
			c = p.clone();
			c.sub(q);

			r = new Coordinate(-c.y, c.x);

			s = ((double) a.frame) / ((double) a.length);
			c.scale(s);
			c.add(q);
			tmp = ((double) a.length) / 2;
			tmp = (((double) a.frame) - tmp) / tmp;
			tmp = 1.0 - tmp * tmp;
			r.scale(0.1 * tmp);
			c.add(r);

			p = squareToPos(c);
			q = p.clone();
			s = a.startPoint.node.getCurrentSize() * 0.5;
			p.add(new Coordinate(-s, -s));
			q.add(new Coordinate(s, s));

			ds.fillRect(p, q, 0.1, Color.black, a.col);
		}

		// Draw nodes:
		for (i = 0; i < node.length; i++) {
			node[i].setRect(ds.fillCircle(
					squareToPos(node[i].getDisplayedPosition()),
					node[i].getCurrentSize(), 0.1, node[i].getOutlineColor(),
					node[i].getColor()));
			if (!node[i].isAlive())
				ds.fillCircle(squareToPos(node[i].getDisplayedPosition()),
						node[i].getCurrentSize() * 0.4, 0.0, null, Color.white);
		}

		// draw subwindows:
		for (i = 0; i < entries.length; i++) {
			SensorWindow Win = ((Map.Entry<SensorNode, SensorWindow>) (entries[i]))
					.getValue();
			render(ds, Win, node);
		}

		// done with the enclosed area:
		ds.popBounds();
		// done with the square-looking drawing area:
		// ds.popBounds();
	}
	double x_bound1 = 0.0; //0.0;
	double y_bound1 = 0.0; //0.05;
	double x_bound2 = 0.0; //-0.1
	double y_bound2 = 0.0; //-0.2
	
	@SuppressWarnings({ "unchecked" })
	private void renderWindowArrayWindow(DrawingSurface ds, SensorWindow win,
			SensorNode[] node) {
		boolean need_to_recalculate_bounds = false;
		boolean need_to_redo_entries = false;
		int i;

		double old_window_height = win.window_height;
		win.window_height = ds.getHeight();
		if (old_window_height != win.window_height)
			need_to_recalculate_bounds = true;

		ds.fillRect(0, 0, 1, 1, Color.white);
		
		// Get ourselves a square-looking drawing area:
		//ds.pushBounds(0.0, 0.0, 1.0, (1.0  / win.window_height));
		ds.pushBounds(0.0 + x_bound1, 0.0 + y_bound1, 1 + x_bound2, 
				(1.0  / win.window_height) + y_bound2);

		// Find any new nodes:
		for (i = 0; i < node.length; i++) {
			SensorWindow sub = win.subwindows.get(node[i]);
			if (sub == null) {
				sub = new SensorWindow(node[i], null, null, null,
						SensorWindow.type_internal_state);
				win.subwindows.put(node[i], sub);
				need_to_recalculate_bounds = true;
			}
		}

		Object[] entries = win.subwindows.entrySet().toArray();

		// Find any removed nodes:
		for (i = 0; i < entries.length; i++) {
			int j;
			boolean exists;
			SensorNode entry = ((Map.Entry<SensorNode, SensorWindow>) (entries[i]))
					.getKey();

			exists = false;
			for (j = 0; !exists && j < node.length; j++)
				if (node[j] == entry)
					exists = true;

			if (!exists) {
				win.subwindows.remove(entry);
				need_to_recalculate_bounds = true;
				need_to_redo_entries = true;
			}
		}

		// Is this array obsolete?
		if (need_to_redo_entries)
			// Yep. Redo:
			entries = win.subwindows.entrySet().toArray();

		// Do we need to recalculate the window bounds?
		if (need_to_recalculate_bounds) {
			// Calculate best layout:
			int max_col;
			int max_row;
			boolean max_squeeze_horizontally;
			double max_size;

			max_col = entries.length;
			max_row = 1;
			max_size = 0;
			max_squeeze_horizontally = true;
			// Go through all possibilities:
			for (i = entries.length/* 1 */; i <= entries.length; i++) {
				int col;
				int row;
				boolean squeeze_horizontally;
				double size;
				int t;

				// Number of columns: i
				col = i;
				// Count number of needed rows:
				t = entries.length;
				row = 0;
				while (t > 0) {
					row++;
					t -= col;
				}

				// Figure out the maximum size of a square so that all will fit
				size = 1.0 / ((double) col);
				squeeze_horizontally = true;
				if (size * row > win.window_height) {
					size = win.window_height / ((double) row);
					squeeze_horizontally = false;
				}

				// Is this better?
				if (size > max_size) {
					max_col = col;
					max_row = row;
					max_size = size;
					max_squeeze_horizontally = squeeze_horizontally;
				}
			}

			int x, y;
			double offx, offy;

			x = 0;
			y = 0;
			if (max_squeeze_horizontally) {
				offx = 0;
				offy = (win.window_height - max_size * max_row) / 2.0;
			} else {
				offx = (1.0 - max_size * max_col) / 2.0;
				offy = 0;
			}
			for (i = 0; i < entries.length; i++) {
				SensorWindow sub = ((Map.Entry<SensorNode, SensorWindow>) (entries[i]))
						.getValue();
				Rect r = new Rect(offx + max_size * x + 0.01, offy + max_size
						* y + 0.01, offx + max_size * (x + 1) - 0.01, offy
						+ max_size * (y + 1) - 0.01);
				sub.setBounds(r, null, null);
				x++;
				if (x >= max_col) {
					x = 0;
					y++;
				}
			}
		}

		// Draw white background:
		ds.fillRect(0, 0, 1, win.window_height, Color.white);

		Coordinate[] p = new Coordinate[15];

		// Draw connectivity:
		for (i = 0; i < entries.length; i++) {
			int j;
			SensorWindow subi = ((Map.Entry<SensorNode, SensorWindow>) (entries[i]))
					.getValue();
			for (j = i + 1; j < entries.length; j++) {
				SensorWindow subj = ((Map.Entry<SensorNode, SensorWindow>) (entries[j]))
						.getValue();
				if (subi.node.isConnectedTo(subj.node.getNodeID())
						|| subj.node.isConnectedTo(subi.node.getNodeID())) {
					Coordinate iconn;
					Coordinate jconn, up;
					int k;

					double lw = default_line_width / 3;
					double a1s, a2s;
					double as = 0.02;
					double offset;
					Color col = Color.blue;

					offset = subi.allBounds.width / 2;
					offset = offset
							* 0.99
							* (((double) (j - i)) / ((double) (entries.length - 1 - i)));

					iconn = new Coordinate(subi.allBounds.right - offset,
							subi.allBounds.top - lw);

					offset = subj.allBounds.width / 2;
					if (j == 1) {
					} else
						offset = offset
								* (((double) (j - i - 1)) / ((double) (j - 1)));
					jconn = new Coordinate(subj.allBounds.left + offset,
							subj.allBounds.top - lw);
					up = new Coordinate(0, -0.03 * (j - i));

					if (subi.node.isConnectedTo(subj.node.getNodeID()))
						a1s = as;
					else
						a1s = 0.0;
					if (subj.node.isConnectedTo(subi.node.getNodeID()))
						a2s = as;
					else
						a2s = 0.0;

					double dlen;
					dlen = p.length - 1;
					for (k = 0; k < p.length; k++) {
						double dk, tmp;
						Coordinate a, b, c;

						dk = k;

						a = iconn.clone();
						b = up.clone();
						c = jconn.clone();

						tmp = dlen / 2;
						tmp = (dk - tmp) / tmp;
						tmp = 1.0 - tmp * tmp;

						a.scale(dk / dlen);
						b.scale(tmp);
						c.scale((dlen - dk) / dlen);

						p[k] = a;
						p[k].add(b);
						p[k].add(c);
					}

					ds.drawArrow(p, a1s, a2s, lw, col);
				}
			}
		}

		// Draw subwindows:
		for (i = 0; i < entries.length; i++) {
			// FIXME: arrange these in order.
			SensorWindow sub = ((Map.Entry<SensorNode, SensorWindow>) (entries[i]))
					.getValue();
			render(ds, sub, node);
		}

		// done with the square-looking drawing area:
		ds.popBounds();
	}

	private void renderInternalStateWindow(DrawingSurface ds, SensorWindow win,
			SensorNode[] node) {
		win.node.doRender(ds);
	}

	private void render(DrawingSurface ds, SensorWindow win, SensorNode[] node) {
		if (win == null) {
			System.out.println("Tried to render() null window.");
			return;
		}
		if (win.dragBounds != null)
			ds.fillRect(win.dragBounds, new Color(0, 0, 0));
		if (win.closeBounds != null)
			ds.fillOval(win.closeBounds, new Color(1.0f, 0.0f, 0.0f));
		if (win.contentBounds != null) {
			if (win.dragBounds != null)
				if (win.closeBounds != null)
					ds.drawStringTopLeft(win.node.getNodeName(),
							win.closeBounds.topRight(),
							1.5 * win.contentBounds.width, Color.white);
				else
					ds.drawStringTopLeft(win.node.getNodeName(),
							win.dragBounds.topLeft(),
							1.5 * win.contentBounds.width, Color.white);
			else
				ds.drawString(win.node.getNodeName(),
						win.contentBounds.topLeft(),
						1.5 * win.contentBounds.width, Color.white);
			ds.pushBounds(win.contentBounds);
		}

		switch (win.type) {
		case SensorWindow.type_internal_state:
			renderInternalStateWindow(ds, win, node);
			break;

		case SensorWindow.type_connectivity:
			renderConnectivityWindow(ds, win, node);
			break;

		case SensorWindow.type_window_array:
			renderWindowArrayWindow(ds, win, node);
			break;

		default:
			System.out.println("Unknown window type in render(): " + win.type);
			break;
		}
		ds.popBounds();
	}

	public synchronized void paint(Graphics g) {
		if (try_report_nodes) {
			try {
				Socket sock;
				sock = new Socket(Vendetta.getProxyIP(), 9797);
				sock.getOutputStream().write(("report_nodes.sh\n").getBytes());
			} catch (Exception e) {
			}
			try_report_nodes = false;
		}

		// Create drawing surface:
		DrawingSurface ds = new DrawingSurface((Graphics2D) g, getBounds());

		// Get an array with all the nodes:
		SensorNode[] node = getNodes();
		// Animate nodes:
		animateSensorNodes(node);
		// Render graphics:
		render(ds, rootWindow, node);

		// Flush drawing:
		ds.flush();
	}

	public void monitorNodeAdded(MonitorNode node) {
	}

	public void clear() {
		int i;

		links = new TreeSet<NodeLink>(new NodeLinkComparator());
		try_report_nodes = true;
		connectivityScale = 1.0;
		if (rootWindow != null)
			rootWindow.clear();
		// Get an array with all the nodes:
		SensorNode[] node = getNodes();
		for (i = 0; i < node.length; i++)
			node[i].reset();
	}

	public void monitorNodeRemoved(MonitorNode node) {
	}

	public void monitorNodeStarted(MonitorNode node) {
		/*
		 * MonitorNodeShape shape = node.getMonitorNodeShape(id);
		 * objTrans.addChild(shape);
		 */
	}

	public void monitorNodeStopped(MonitorNode node) {
	}

	public void overlayStarted(MonitorNode node) {
	}

	public void overlayStopped(MonitorNode node) {
	}

	public void toggleSleepMode(boolean state) {
	}

	public void setLogHandler(HaggleLogHandler lh) {
		this.lh = lh;
	}

	public synchronized void mousePressed(MouseEvent e) {
		mousePoint = new Coordinate(e.getX(), e.getY());
		mouseAction = 1;
		if (e.getButton() == MouseEvent.BUTTON1)
			mouseButton = 1;
		if (e.getButton() == MouseEvent.BUTTON2)
			mouseButton = 2;
		if (e.getButton() == MouseEvent.BUTTON3)
			mouseButton = 3;
	}

	private void reportSelectedNodes() {
		SensorNode s;
		int i;
		int j = 0;
		for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
			s = lh.getSensorNode(i);
			if (s != null) {
				if (s.isSelected())
					j++;
			}
		}
		SensorNode[] node = new SensorNode[j];
		j = 0;
		for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
			s = lh.getSensorNode(i);
			if (s != null) {
				if (s.isSelected()) {
					node[j] = s;
					j++;
				}
			}
		}
		Vendetta.selectNodes(node, true);
	}

	private SensorWindow findSubWindow(SensorWindow win, Coordinate c) {
		SensorWindow[] subwindow = win.subwindows.values().toArray(
				new SensorWindow[0]);
		int i;
		for (i = 0; i < subwindow.length; i++) {
			if (subwindow[i].allBounds.contains(c)) {
				return subwindow[i];
			}
		}
		return null;
	}

	private void mouseReleasedConnectivity(MouseEvent e, Coordinate c,
			SensorWindow win) {
		Coordinate p = win.toPos(new Coordinate());
		c = new Rect(p.x - win.window_height / 2.0, p.y - win.window_height
				/ 2.0, p.x + win.window_height / 2.0, p.y + win.window_height
				/ 2.0).revConvert(new Rect((1.0 - connectivityScale) / 2.0,
				(win.window_height - connectivityScale)
						/ (2.0 * win.window_height),
				(1.0 + connectivityScale) / 2.0,
				(win.window_height + connectivityScale)
						/ (2.0 * win.window_height)).revConvert(c));
		if (mouseAction == 1) {
			SensorNode s;
			int i;
			if (mouseButton == 1) {
				SensorWindow w = findSubWindow(win, c);
				if (w != null) {
					if (w.closeBounds != null && mouseAction == 1) {
						if (w.closeBounds.contains(c)) {
							win.subwindows.remove(w.node);
							return;
						}
					}
					mouseReleased(e, c, w);
				} else
					switch (e.getClickCount()) {
					case 1:
						if (!e.isShiftDown()) {
							for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
								s = lh.getSensorNode(i);
								if (s != null) {
									s.unselect();
								}
							}
						}
						for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
							s = lh.getSensorNode(i);
							if (s != null) {
								if (s.getRect() != null)
									if (s.getRect().contains(c)) {
										if (e.isShiftDown() && s.isSelected())
											s.unselect();
										else
											s.select();
									}
							}
						}
						reportSelectedNodes();
						break;

					case 2:
						for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
							s = lh.getSensorNode(i);
							if (s != null) {
								if (s.getRect().contains(c)) {
									if (win.subwindows.get(s) == null) {
										s.unselect();
										reportSelectedNodes();
										win.subwindows
												.put(s,
														new SensorWindow(
																s,
																win.toPos(s
																		.getDisplayedPosition()),
																0.25,
																0.25,
																SensorWindow.type_internal_state));
										return;
									}
								}
							}
						}

						break;

					default:
						break;
					}
			} else if (mouseButton == 3) {
				SensorWindow w = findSubWindow(win, c);
				if (w != null) {
					mouseReleased(e, c, w);
				} else {
					for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
						s = lh.getSensorNode(i);
						if (s != null) {
							s.unselect();
						}
					}
					reportSelectedNodes();
				}
			}
		} else if (mouseAction == 2) {
			if (win.dragSubWindow == null) {
				releaseDrag(mouseButton, win);
				if (mouseNode != null)
					mouseNode.releaseDrag(mouseButton);
			} else {
				win.dragSubWindow.node.releaseDrag(mouseButton);
				win.dragSubWindow = null;
			}
		} else if (mouseAction == 4) {
			win.dragSubWindow = null;
		}
	}

	private void mouseReleasedWindowArray(MouseEvent e, Coordinate c,
			SensorWindow win) {
		c = new Rect(0, 0, 1, 1.0 / win.window_height).revConvert(c);
		if (mouseAction == 1) {
			win.dragSubWindow = findSubWindow(win, c);
		}
		if (win.dragSubWindow != null) {
			mouseReleased(e, c, win.dragSubWindow);
			win.dragSubWindow = null;
		}
	}

	private void mouseReleasedInternalState(MouseEvent e, Coordinate c,
			SensorWindow win) {
		if (mouseAction == 1) {
			win.node.handleClick(c, mouseButton, e.isShiftDown());
		} else if (mouseAction == 2)
			win.node.releaseDrag(mouseButton);
	}

	private void mouseReleased(MouseEvent e, Coordinate c, SensorWindow win) {
		if (win == null) {
			System.out.println("Tried to render() null window.");
			return;
		}
		if (win.contentBounds != null)
			c = win.contentBounds.revConvert(c);
		switch (win.type) {
		case SensorWindow.type_internal_state:
			mouseReleasedInternalState(e, c, win);
			break;

		case SensorWindow.type_connectivity:
			mouseReleasedConnectivity(e, c, win);
			break;

		case SensorWindow.type_window_array:
			mouseReleasedWindowArray(e, c, win);
			break;

		default:
			System.out.println("Unknown window type in mouseReleased(): "
					+ win.type);
			break;
		}
	}

	public synchronized void mouseReleased(MouseEvent e) {
		if (mouseAction == 0)
			return;

		Coordinate c = new Coordinate(e.getX(), e.getY());
		{
			Rect r = new Rect(getBounds());
			c = r.revConvert(c);
		}
		try {
			mouseReleased(e, c, rootWindow);
		} catch (Exception ex) {
			System.out.println("" + ex);
			ex.printStackTrace();
		}

		mouseAction = 0;
		mousePoint = null;
		mouseNode = null;
	}

	public synchronized boolean startDrag(Coordinate c, int mouseButton,
			SensorWindow win) {
		if (mouseButton != 1)
			return false;
		int i;
		SensorNode s;

		for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
			s = lh.getSensorNode(i);
			if (s != null) {
				Rect r;
				r = s.getRect();
				if (r != null)
					if (r.contains(c)) {
						s.startDragging();
						mouseNode = s;
						dragTo(s, c, mouseButton, win);
						return true;
					}
			}
		}
		return false;
	}

	public synchronized void dragTo(SensorNode s, Coordinate c,
			int mouseButton, SensorWindow win) {
		if (mouseButton != 1)
			return;
		c = squareRevPos(c);
		double dist = new Coordinate().distance(c);
		if (dist > 1.0)
			c.scale(1.0 / dist);
		s.setCurrentPosition(c);
		s.setDisplayedPosition(c);
		s.setCalculatedPosition(c);
	}

	public synchronized void dragTo(Coordinate c, int mouseButton,
			SensorWindow win) {
		if (mouseButton != 1)
			return;
		int i;
		SensorNode s;

		for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
			s = lh.getSensorNode(i);
			if (s != null) {
				if (s.isBeingDragged()) {
					dragTo(s, c, mouseButton, win);
					return;
				}
			}
		}
	}

	public synchronized void releaseDrag(int mouseButton, SensorWindow win) {
		if (mouseButton != 1)
			return;
		int i;
		SensorNode s;

		for (i = 0; i < Vendetta.getMonitorNodeCount(); i++) {
			s = lh.getSensorNode(i);
			if (s != null) {
				if (s.isBeingDragged()) {
					s.stopDragging();
					return;
				}
			}
		}
	}

	private void mouseDraggedConnectivity(MouseEvent e, Coordinate c,
			Coordinate mp, SensorWindow win) {
		{
			Coordinate p = win.toPos(new Coordinate());
			Rect r2 = new Rect(p.x - win.window_height / 2.0, p.y
					- win.window_height / 2.0, p.x + win.window_height / 2.0,
					p.y + win.window_height / 2.0);
			Rect r = new Rect((1.0 - connectivityScale) / 2.0,
					(win.window_height - connectivityScale)
							/ (2.0 * win.window_height),
					(1.0 + connectivityScale) / 2.0,
					(win.window_height + connectivityScale)
							/ (2.0 * win.window_height));
			c = r2.revConvert(r.revConvert(c));
			mp = r2.revConvert(r.revConvert(mp));
		}
		if (mouseAction == 1) {
			win.dragSubWindow = findSubWindow(win, mp);
			if (win.dragSubWindow == null) {
				if (startDrag(c, mouseButton, win)) {
					mouseAction = 2;
				} else {
					mouseAction = 3;
				}
			}
		}
		if (win.dragSubWindow == null) {
			dragTo(c, mouseButton, win);
		} else {
			mouseDragged(e, c, mp, win.dragSubWindow);
		}
	}

	private void mouseDraggedWindowArray(MouseEvent e, Coordinate c,
			Coordinate mp, SensorWindow win) {
		{
			Rect r = new Rect(0, 0, 1, 1.0 / win.window_height);
			c = r.revConvert(c);
			mp = r.revConvert(mp);
		}
		if (mouseAction == 1 && win.dragSubWindow == null) {
			win.dragSubWindow = findSubWindow(win, mp);
			if (win.dragSubWindow == null) {
				mouseAction = 3;
			}
		}
		if (win.dragSubWindow != null) {
			mouseDragged(e, c, mp, win.dragSubWindow);
		}
	}

	private void mouseDraggedInternalState(MouseEvent e, Coordinate c,
			Coordinate mp, SensorWindow win) {
		if (mouseAction == 1) {
			if (mp.x < 0.0 || mp.x > 1.0 || mp.y < 0.0 || mp.y > 1.0) {
				mouseAction = 3;
			} else {
				win.node.startDrag(mp, mouseButton);
				mouseAction = 2;
			}
		}
		if (mouseAction == 2) {
			if (c.x < 0.0 || c.x > 1.0 || c.y < 0.0 || c.y > 1.0) {
				mouseAction = 3;
				win.node.releaseDrag(mouseButton);
				return;
			}
			win.node.dragTo(c, mouseButton);
		}
	}

	private void mouseDragged(MouseEvent e, Coordinate c, Coordinate mp,
			SensorWindow win) {
		if (win == null) {
			System.out.println("Tried to render() null window.");
			return;
		}
		if (win.closeBounds != null) {
			if (mouseAction == 1) {
				if (win.closeBounds.contains(mp)) {
					System.out.println("Dragged in close bounds!");
					mouseAction = 3;
					return;
				}
			}
		}
		if (win.dragBounds != null) {
			if (mouseAction == 1) {
				if (win.dragBounds.contains(mp)
						&& !win.contentBounds.contains(mp)) {
					mouseAction = 4;
				}
			}
			if (mouseAction == 4 && win.dragBounds.contains(mp)) {
				Coordinate delta = new Coordinate();
				delta.sub(c, mp);
				if (win.dragBounds != null)
					win.dragBounds.move(delta);
				if (win.closeBounds != null)
					win.closeBounds.move(delta);
				if (win.contentBounds != null)
					win.contentBounds.move(delta);
				win.setBounds(win.contentBounds, win.closeBounds,
						win.dragBounds);
				return;
			}
		}
		if (win.contentBounds != null) {
			c = win.contentBounds.revConvert(c);
			mp = win.contentBounds.revConvert(mp);
		}
		switch (win.type) {
		case SensorWindow.type_internal_state:
			mouseDraggedInternalState(e, c, mp, win);
			break;

		case SensorWindow.type_connectivity:
			mouseDraggedConnectivity(e, c, mp, win);
			break;

		case SensorWindow.type_window_array:
			mouseDraggedWindowArray(e, c, mp, win);
			break;

		default:
			System.out.println("Unknown window type in mouseDragged(): "
					+ win.type);
			break;
		}
	}

	public synchronized void mouseDragged(MouseEvent e) {
		if (mouseAction == 0 || mouseAction == 3)
			return;

		Coordinate c = new Coordinate(e.getX(), e.getY());
		Coordinate mp = mousePoint;
		mousePoint = c;

		{
			Rect r = new Rect(getBounds());
			c = r.revConvert(c);
			mp = r.revConvert(mp);
		}
		try {
			mouseDragged(e, c, mp, rootWindow);
		} catch (Exception ex) {
			System.out.println("" + ex);
			ex.printStackTrace();
		}
	}

	public synchronized void mouseWheelMoved(MouseWheelEvent e) {
		// System.out.println("Scroll: " + e.getUnitsToScroll());
		connectivityScale = connectivityScale + ((double) e.getUnitsToScroll())
				* 0.01;
		if (connectivityScale < 0.01)
			connectivityScale = 0.01;
		if (connectivityScale > 5.0)
			connectivityScale = 5.0;
	}
}
