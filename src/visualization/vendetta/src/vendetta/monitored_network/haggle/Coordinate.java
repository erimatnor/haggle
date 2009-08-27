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

/**
 */
public class Coordinate {
	private static Log LOG = Log.getInstance("Coordinate");
	
	public double x;
	public double y;
	
	public Coordinate()
	{
		this(0.0,0.0);
	}
	
	public Coordinate(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public String toString()
	{
		return x + ", " + y;
	}
	
	public Coordinate clone()
	{
		return new Coordinate(x,y);
	}
	
	public double distance(Coordinate other)
	{
		double dx, dy;
		
		dx = other.x - x;
		dy = other.y - y;
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	public double length()
	{
		return Math.sqrt(x*x + y*y);
	}
	
	public void normalize()
	{
		scale(1.0/Math.sqrt(x*x + y*y));
	}
	
	public double dot(Coordinate other)
	{
		return x*other.x + y*other.y;
	}
	
	public double cross(Coordinate other)
	{
		return x*other.y - y*other.x;
	}
	
	public void scale(double s)
	{
		x *= s;
		y *= s;
	}
	
	public void add(Coordinate other)
	{
		x += other.x;
		y += other.y;
	}
	
	public void sub(Coordinate other)
	{
		x -= other.x;
		y -= other.y;
	}
	
	public void add(Coordinate c1, Coordinate c2)
	{
		x = c1.x + c2.x;
		y = c1.y + c2.y;
	}
	
	public void sub(Coordinate c1, Coordinate c2)
	{
		x = c1.x - c2.x;
		y = c1.y - c2.y;
	}
}
