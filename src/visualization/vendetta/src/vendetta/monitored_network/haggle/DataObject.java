/* Haggle testbed
 * Uppsala University
 *
 * Haggle internal release
 *
 * Copyright Haggle
 */

package vendetta.monitored_network.haggle;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.imageio.*;
import java.io.*;

import java.lang.Math;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import vendetta.util.log.Log;
import vendetta.visualization.canvases.HaggleCanvas;

import java.util.*;
import sun.misc.BASE64Decoder;
/**
 */
public class DataObject {
	private static boolean hideForwardingDOs = true;
	private static Log LOG = Log.getInstance("DataObject");
	
	private String ID;
	private String do_rowid;
	private String node_rowid;
	private String node_id;
	// The position as displayed
	private Coordinate displayed_pos;
	// The actual current position
	private Coordinate current_pos;
	// The next position
	private Coordinate calculated_pos;
	
	private Rect drawnNodePos;
	private Rect drawnDOPos;
	
	private boolean is_forwarding_do;
	private boolean node_is_visible;
	private boolean is_this_node;
	private boolean is_security_fail_tagged;
	private boolean is_being_dragged;
	private boolean dragged_by_node;
	private boolean is_selected;
	private int is_important_counter;
	private DOTable owner;
	private BufferedImage thumbnail;
	
	private ArrayList<Attribute> attributes;
	
	enum dataobject_state {
		spawned_this_time,
		alive,
		dying,
		dead
	}
	
	private int anim_state;
	private int anim_counter;
	
	private dataobject_state state;
	
	DataObject(String id, String rowid)
	{
		Random generator = new Random();
		int i;
		
		thumbnail = null;
		ID = id;
		do_rowid = rowid;
		node_rowid = null;
		node_id = null;
		is_forwarding_do = false;
		node_is_visible = false;
		is_this_node = false;
		is_security_fail_tagged = false;
		is_being_dragged = false;
		dragged_by_node = false;
		is_selected = false;
		anim_state = 0;
		anim_counter = 0;
		is_important_counter = 90;
		attributes = new ArrayList<Attribute>();
		
		displayed_pos = 
			new Coordinate(
				(float) (generator.nextDouble()) - 0.5f,
				(float) (generator.nextDouble()) - 0.5f);
		current_pos = 
			new Coordinate(
				displayed_pos.x,
				displayed_pos.y);
		calculated_pos = 
			new Coordinate(
				displayed_pos.x,
				displayed_pos.y);
		
		drawnNodePos = null;
		drawnDOPos = null;
		
		state = dataobject_state.spawned_this_time;
	}
	
	public void setThumbnail(String str)
	{
		try{
		thumbnail = 
			ImageIO.read(
				new ByteArrayInputStream(
					new BASE64Decoder().decodeBuffer(
						str)));
		}catch(Exception e){
		}
	}
	
	public BufferedImage getThumbnail()
	{
		return thumbnail;
	}
	
	public Rect getThumbnailSize()
	{
		return new Rect(0,0,thumbnail.getWidth(),thumbnail.getHeight());
	}
	
	public void setOwner(DOTable _owner)
	{
		owner = _owner;
	}
	
	public void setImportantFor(int time)
	{
		if(is_important_counter < time)
		{
			is_important_counter = time;
			owner.setImportantFor(time);
		}
	}
	
	public boolean isImportant()
	{
		return is_important_counter > 0;
	}
	
	public static void setHideForwardingDOs(boolean yes)
	{
		hideForwardingDOs = yes;
	}
	
	boolean isAlive()
	{
		return 
			state == dataobject_state.alive || 
			state == dataobject_state.spawned_this_time;
	}
	
	boolean isNew()
	{
		return state == dataobject_state.spawned_this_time;
	}
	
	boolean isThisNode()
	{
		return is_this_node;
	}
	
	void clearAttributes()
	{
		attributes = new ArrayList<Attribute>();
	}
	
	void isThisNode(boolean yes)
	{
		is_this_node = yes;
	}
	
	void startDragging(boolean byNode)
	{
		is_being_dragged = true;
		dragged_by_node = byNode;
	}
	
	void stopDragging()
	{
		is_being_dragged = false;
		dragged_by_node = false;
	}
	
	boolean isBeingDragged()
	{
		return is_being_dragged;
	}
	
	boolean isDraggedByNode()
	{
		return dragged_by_node;
	}
	
	void select(boolean yes)
	{
		is_selected = yes;
	}
	
	boolean isSelected()
	{
		return is_selected;
	}
	
	boolean isSpecialDO()
	{
		try {
			return Long.parseLong(do_rowid) < 0;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}
	
	void setNodeIsVisible(boolean yes)
	{
		node_is_visible = yes;
	}
	
	private static final boolean should_dim = true;
	private static final boolean should_dim_colors = true && should_dim;
	private static final boolean should_dim_lines = false && should_dim;
	
	private boolean shouldDim()
	{
		return false;//should_dim && !isImportant() && owner.hasImportant();
	}
	
	private Color getDimmed(Color c)
	{
		if(should_dim_colors && shouldDim())
		{
			float	red,green,blue;
			float	r,g,b,a;
			r = 1.0f;
			g = 1.0f;
			b = 1.0f;
			a = 0.50f;
			
			red = ((float)c.getRed())/255.0f;
			green = ((float)c.getGreen())/255.0f;
			blue = ((float)c.getBlue())/255.0f;
			
			red = red * (1.0f - a) + r * (a);
			green = green * (1.0f - a) + g * (a);
			blue = blue * (1.0f - a) + b * (a);
			return new Color(red,green,blue);
		}else
			return c;
	}
	
	Color getDOColorRaw()
	{
		Color	retval;
		
		if(is_security_fail_tagged)
		{
			retval =
				new Color(1.0f, 0.0f, 0.0f);
		}else if(isNodeDescription())
		{
			float h;
			long i;
			
			int which = 3;
			i = Integer.parseInt(node_id.substring(which,which+6),16);
			h =  ((float) i) / ((float) 0xFFFFFF);
			java.awt.Color c = java.awt.Color.getHSBColor(h,1.0f,0.75f);
			
			retval = c;
		}else if(isForwardingInfo())
			retval = 
				new Color(0.5f, 0.5f, 0.5f);
		else
			retval =
				new Color(1.0f, 1.0f, 1.0f);
		return retval;
	}
	
	Color getDOColor()
	{
		return getDimmed(getDOColorRaw());
	}
	
	Color getNodeColor(boolean thisNodeOn)
	{
		Color	retval;
		
		if(node_is_visible || (is_this_node && thisNodeOn))
			retval = 
				// visible nodes are green:
				new Color(0.0f,1.0f,0.0f);
		else
			retval =
				// non-visible nodes are red:
				new Color(1.0f,0.0f,0.0f);
		return getDimmed(retval);
	}

	Color getDOOutlineColor()
	{
		Color	retval;
		
		retval =
			// Black is default:
			new Color(0.0f,0.0f,0.0f);
		return getDimmed(retval);
	}

	Color getNodeOutlineColor()
	{
		Color	retval;

		retval =
			// Black is default:
			new Color(0.0f,0.0f,0.0f);
		return getDimmed(retval);
	}
	
	Color getNodeDOLinkColor()
	{
		return getDimmed(Color.black);
	}
	
	static Color getDODOLinkColor(float weight, DataObject dO1, DataObject dO2)
	{
		Color	retval;
		DataObject dOtoAsk;
		
		if(dO1.isImportant())
			dOtoAsk = dO1;
		else 
			dOtoAsk = dO2;
		
		if(dOtoAsk.isImportant())
			retval = new Color(1.0f,0.0f,0.0f);
		else{
		if(weight < 0.0f)
			retval = new
				Color(0.5f,0.5f,0.5f);
		if(weight > 100.0f)
			retval = new
				Color(0.0f,0.0f,0.0f);
		weight = (100.0f - weight)/100.0f;
		weight = weight * 0.2f;
		retval = new
			Color(weight,weight,weight);
		}
		return dOtoAsk.getDimmed(retval);
	}
	
	Color getNodeNodeLinkColor(float weight)
	{
		Color	retval;
		
		if(weight < 0.0f)
			retval = new
				Color(0.5f,0.5f,0.5f);
		if(weight > 1.0f)
			retval = new
				Color(0.0f,0.0f,0.0f);
		weight = (1.0f - weight)/1.0f;
		weight = weight * 0.75f;
		retval = new
			Color(weight,weight,weight);
		return getDimmed(retval);
	}
	
	static double getDODOLinkWidth(double width, double weight, DataObject dO1, DataObject dO2)
	{
		width = width * (1 + weight/166.0f);
		
		if(should_dim_lines)
		{
			DataObject dOtoAsk;
			
			if(dO1.isImportant())
				dOtoAsk = dO1;
			else 
				dOtoAsk = dO2;
			if(dOtoAsk.shouldDim())
			{
				return width;
			}else{
				return width*2;
			}
		}else{
			return width;
		}
	}
	
	void setNew()
	{
		state = dataobject_state.spawned_this_time;
	}
	
	void setAlive(boolean yes)
	{
		switch(state)
		{
			case spawned_this_time:
				if(!yes)
					state = dataobject_state.dead;
			break;
			
			case alive:
				if(!yes)
					state = dataobject_state.dying;
			break;
			
			case dying:
				if(yes)
					state = dataobject_state.alive;
			break;
			
			case dead:
				if(yes)
					state = dataobject_state.spawned_this_time;
			break;
		}
	}
	
	void updateState()
	{
		if(is_important_counter > 0)
		{
			is_important_counter--;
		}
		switch(state)
		{
			case spawned_this_time:
				state = dataobject_state.alive;
			break;
			
			case dying:
				if(isNodeDescription())
				{
					anim_state = 3;
				}else if(anim_state < 2)
				{
					anim_state = 2;
					anim_counter = -1;
				}
				
				if(anim_state == 3)
				{ 
					state = dataobject_state.dead;
				}
			break;
			
			default:
			break;
		}
		
		switch(anim_state)
		{
			case 0:
				anim_counter++;
				if(anim_counter > 30)
				{
					anim_state = 1;
					anim_counter = 0;
					is_security_fail_tagged = false;
				}
			break;
			
			default:
				anim_state = 1;
				anim_counter = 0;
			case 1:
			break;
			
			case 2:
				anim_counter++;
				if(anim_counter > 30)
				{
					anim_state = 3;
					anim_counter = 0;
				}
			break;
			
			case 3:
			break;
		}
	}
	
	boolean isNodeVisible()
	{
		return node_is_visible && isNodeDescription();
	}
	
	boolean isVisible()
	{
		if(hideForwardingDOs && isForwardingInfo())
			return false;
		return (state != dataobject_state.dead) || anim_counter == 2;
	}
	
	boolean isAnimating()
	{
		return anim_state == 0 || anim_state == 2 || isImportant();
	}
	
	void startSecurityFailAnimation()
	{
		is_security_fail_tagged = true;
		anim_state = 0;
		anim_counter = 0;
	}
	
	final static float do_object_size = 0.04f;
	
	float getCurrentSize()
	{
		if(isAnimating())
		{
			if(is_security_fail_tagged)
				return 
					do_object_size +
					do_object_size*2.0f +  
					(do_object_size/2.0f)*
					((float)sin(2*PI*((float)anim_counter)/30.0f));
			return 
				do_object_size + 
				(do_object_size/2.0f)*
				((float)sin(2*PI*((float)anim_counter)/30.0f));
		}else
			return do_object_size;
	}
	
	float getCurrentNodeSize()
	{
		return do_object_size;
	}
	
	void addAttribute(String name, String value)
	{
		if(name.equals("NodeDescription"))
		{
			if(!value.equals("*"))
			{
				node_id = value;
			}else{
				attributes.add(new Attribute(name, value));
			}
		}else if(name.equals("Hide") && value.equals("this"))
		{
			is_forwarding_do = true;
		}else if(name.equals("Forward"))
		{
			if(!value.equals("*"))
			{
				is_forwarding_do = true;
				node_id = value;
			}
		}else{
			attributes.add(new Attribute(name, value));
		}
	}
	
	void showAttributes()
	{
		int i;
		
		Object[] attrs = attributes.toArray();
		if(attrs != null)
			for(i = 0; i < attrs.length; i++)
			{
				LOG.debug(
					"Attribute: " + 
					((Attribute)(attrs[i])).getName() + "=" + 
					((Attribute)(attrs[i])).getValue());
			}
	}
	
	Attribute[] getAttributes()
	{
		return attributes.toArray(new Attribute[0]);
	}
	
	boolean idEquals(String id)
	{
		return id.equals(ID);
	}
	
	boolean isNodeDescription()
	{
		return node_id != null && !is_forwarding_do;
	}
	
	boolean isForwardingInfo()
	{
		return is_forwarding_do;
	}
	
	void setNodeRowID(String id)
	{
		node_rowid = id;
	}
	
	String getID()
	{
		return ID;
	}
	
	String getRowID()
	{
		return do_rowid;
	}
	
	void setRowID(String rowid)
	{
		if(do_rowid == null)
			do_rowid = rowid;
	}
	
	String getNodeID()
	{
		return node_id;
	}
	
	String getNodeRowID()
	{
		return node_rowid;
	}
	
	Coordinate getDisplayedPosition()
	{
		return displayed_pos;
	}
	
	Coordinate getCurrentPosition()
	{
		return current_pos;
	}
	
	Coordinate getCalculatedPosition()
	{
		return calculated_pos;
	}
	
	void setDisplayedPosition(Coordinate pos)
	{
		displayed_pos = pos;
	}
	
	void setCurrentPosition(Coordinate pos)
	{
		current_pos = pos;
	}
	
	void setCalculatedPosition(Coordinate pos)
	{
		calculated_pos = pos;
	}
	
	void setNodeRect(Rect r)
	{
		drawnNodePos = r;
	}
	
	void setDORect(Rect r)
	{
		drawnDOPos = r;
	}
	
	Rect getNodeRect()
	{
		return drawnNodePos;
	}
	
	Rect getDORect()
	{
		return drawnDOPos;
	}
}
