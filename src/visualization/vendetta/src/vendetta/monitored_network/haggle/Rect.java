/* Haggle testbed
 * Uppsala University
 *
 * Haggle internal release
 *
 * Copyright Haggle
 */

package vendetta.monitored_network.haggle;

import vendetta.util.log.Log;
import java.lang.Math;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 */
public class Rect {
	private static Log LOG = Log.getInstance("Rect");
	
	public double left;
	public double top;
	public double right;
	public double bottom;
	
	public double width;
	public double height;
	
	public Rect()
	{
		this(0.0,0.0,0.0,0.0);
	}
	
	public Rect(Rect r)
	{
		this(r.left, r.top, r.right, r.bottom);
	}
	
	public Rect(Rectangle r)
	{
		this(r.x, r.y, r.x + r.width, r.y + r.height);
	}
	
	public Rect(Rectangle2D r)
	{
		this(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
	}
	
	public Rect(double left, double top, double right, double bottom)
	{
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		
		width = right - left;
		height = bottom - top;
	}
	
	public Rect(Coordinate topLeft, Coordinate bottomRight)
	{
		left = topLeft.x;
		top = topLeft.y;
		right = bottomRight.x;
		bottom = bottomRight.y;
		
		width = right - left;
		height = bottom - top;
	}
	
	public String toString()
	{
		return left + ", " + top + ", " + right + ", " + bottom;
	}
	
	public Rect clone()
	{
		return new Rect(left, top, right, bottom);
	}
	
	public Coordinate topLeft()
	{
		return new Coordinate(left, top);
	}
	
	public Coordinate topRight()
	{
		return new Coordinate(right, top);
	}
	
	public Coordinate bottomLeft()
	{
		return new Coordinate(left, bottom);
	}
	
	public Coordinate bottomRight()
	{
		return new Coordinate(right, bottom);
	}
	
	public void scale(double s)
	{
		left *= s;
		top *= s;
		right *= s;
		bottom *= s;
		
		width = right - left;
		height = bottom - top;
	}
	
	public void move(Coordinate delta)
	{
		left += delta.x;
		right += delta.x;
		bottom += delta.y;
		top += delta.y;
		
		width = right - left;
		height = bottom - top;
	}
	
	public boolean contains(Coordinate p)
	{
		return
			(
			 (width > 0 &&
			  (left <= p.x && right >= p.x)) ||
			 (width < 0 &&
			  (left >= p.x && right <= p.x))) &&
			(
			 (height > 0 &&
			  (top <= p.y && bottom >= p.y)) ||
			 (height < 0 &&
			  (top >= p.y && bottom <= p.y)));
	}
	
	private boolean crosses(
		double l1x, double l1y1, double l1y2,
		double l2y, double l2x1, double l2x2)
	{
		if(l1x < l2x1 || l1x > l2x2)
			return false;
		if(l2y < l1y1 || l2y > l1y2)
			return false;
		return true;
	}
	
	public boolean overlaps(Rect r)
	{
		// For two axis-aligned rectangles to overlap, either one of the 
		// rectangles' corner points has to be within the other rectangle...
		if(contains(new Coordinate(r.top, r.left)))
			return true;
		if(contains(new Coordinate(r.top, r.right)))
			return true;
		if(contains(new Coordinate(r.bottom, r.left)))
			return true;
		if(contains(new Coordinate(r.bottom, r.right)))
			return true;
		if(r.contains(new Coordinate(top, left)))
			return true;
		if(r.contains(new Coordinate(top, right)))
			return true;
		if(r.contains(new Coordinate(bottom, left)))
			return true;
		if(r.contains(new Coordinate(bottom, right)))
			return true;
		
		// ... or one of the rectangles' border lines has to intersect one of
		// the other rectangle's border lines.
		if(crosses(
			r.left, r.top, r.bottom, 
			top, left, right))
			return true;
		if(crosses(
			r.left, r.top, r.bottom, 
			bottom, left, right))
			return true;
		
		if(crosses(
			r.right, r.top, r.bottom, 
			top, left, right))
			return true;
		if(crosses(
			r.right, r.top, r.bottom, 
			bottom, left, right))
			return true;
		
		if(crosses(
			r.top, r.left, r.right, 
			left, top, bottom))
			return true;
		if(crosses(
			r.top, r.left, r.right, 
			left, top, bottom))
			return true;
		
		if(crosses(
			r.bottom, r.left, r.right, 
			top, left, right))
			return true;
		if(crosses(
			r.bottom, r.left, r.right, 
			right, top, bottom))
			return true;
		
		return false;
	}
	
	public double convertX(double x)
	{
		return left + (x*width);
	}
	
	public double convertY(double y)
	{
		return top + (y*height);
	}
	
	public Coordinate convert(Coordinate p)
	{
		return new Coordinate(convertX(p.x), convertY(p.y));
	}
	
	public Rect convert(Rect r)
	{
		return
			new Rect(
				convertX(r.left), convertY(r.top),
				convertX(r.right), convertY(r.bottom));
	}
	
	public double revConvertX(double x)
	{
		return (x - left)/width;
	}
	
	public double revConvertY(double y)
	{
		return (y - top)/height;
	}
	
	public Coordinate revConvert(Coordinate p)
	{
		return new Coordinate(revConvertX(p.x), revConvertY(p.y));
	}
	
	public Rect revConvert(Rect r)
	{
		return 
			new Rect(
				revConvertX(r.left), revConvertY(r.top),
				revConvertX(r.right), revConvertY(r.bottom));
	}
}
