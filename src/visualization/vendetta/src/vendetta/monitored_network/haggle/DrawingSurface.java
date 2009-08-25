/* Haggle testbed
 * Uppsala University
 *
 * Haggle internal release
 *
 * Copyright Haggle
 */

package vendetta.monitored_network.haggle;

import vendetta.util.log.Log;

import javax.media.j3d.J3DGraphics2D;
import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
 */
public class DrawingSurface {
	private static Log LOG = Log.getInstance("Attribute");
	
	private J3DGraphics2D g;
	private Font baseFont;
	private FontMetrics baseFontMetrics;
	private AffineTransform baseTransform;
	
	private Rect[] boundsStack;
	private int boundsSP;
	private int boundsStackSize;
	
	private Rect currentBounds()
	{
		return boundsStack[boundsSP-1];
	}
	
	public double convertX(double x)
	{
		return currentBounds().convertX(x);
	}
	
	public double convertY(double y)
	{
		return currentBounds().convertY(y);
	}
	
	public Coordinate convert(Coordinate p)
	{
		return currentBounds().convert(p);
	}
	
	public Rect convert(Rect r)
	{
		return currentBounds().convert(r);
	}
	
	public double revConvertX(double x)
	{
		return currentBounds().revConvertX(x);
	}
	
	public double revConvertY(double y)
	{
		return currentBounds().revConvertY(y);
	}
	
	public Coordinate revConvert(Coordinate p)
	{
		return currentBounds().revConvert(p);
	}
	
	public Rect revConvert(Rect r)
	{
		return currentBounds().revConvert(r);
	}
	
	private void pushBoundsPriv(Rect b)
	{
		if(boundsSP == boundsStackSize)
		{
			Rect[] bS;
			int i;
			
			bS = new Rect[boundsStackSize + 5];
			for(i = 0; i < boundsStackSize; i++)
				bS[i] = boundsStack[i];
			boundsStackSize += 5;
			for(i = boundsSP; i < boundsStackSize; i++)
				bS[i] = null;
			
			boundsStack = bS;
		}
		
		boundsStack[boundsSP] = b;
		boundsSP++;
		// FIXME:
		// g.setClip(null);
		// g.setClip(...)
	}
	
	public void pushBounds(Rect b)
	{
		b = convert(b);
		pushBoundsPriv(b);
	}
	
	public void pushBounds(double x1, double y1, double x2, double y2)
	{
		pushBounds(
			new Rect(
				x1,y1,
				x2,y2));
	}
	
	public void pushBounds(
					double x1, double y1, 
					double x2, double y2, 
					double xScale, double yScale)
	{
		pushBounds(
			new Rect(
				x1*xScale,y1*yScale,
				x2*xScale,y2*yScale));
	}
	
	public Rect popBounds()
	{
		if(boundsSP > 1)
		{
			boundsSP--;
			Rect r = boundsStack[boundsSP];
			boundsStack[boundsSP] = null;
			return revConvert(r);
		}
		if(boundsSP == 1)
			return revConvert(boundsStack[0]);
		return null;
	}
	
	public DrawingSurface(J3DGraphics2D g, Rectangle r)
	{
		this.g = g;
		baseFont = g.getFont();
		baseTransform = g.getTransform();
		baseFontMetrics = g.getFontMetrics();
		
		g.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING, 
			RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		boundsStack = null;
		boundsStackSize = 0;
		boundsSP = 0;
		pushBoundsPriv(new Rect(r));
	}
	
	public void flush()
	{
		g.flush(true);
		/*
			On Mac OS X, the graphics context _should_ be disposed, or drawing
			will become slower and slower...
			
			On (one or more) Linuxes, the graphics context _should_not_ be 
			disposed, or the Java VM will CRASH!
		*/
		if(System.getProperty("os.name").equals("Mac OS X"))
		{
			g.dispose();
			g = null;
		}
	}
	
	public double getHeight()
	{
		return currentBounds().height/currentBounds().width;
	}
	
	public void printRect()
	{
		Rect b = currentBounds();
		System.out.println("Rect: " + b);
	}
	
	private double internalScaleForScale(double s)
	{
		double stdPixelWidth = 300.0;
		return s*(currentBounds().width/stdPixelWidth);
	}
	
	public Rect measureString(String str, double scale)
	{
		return measureString(str, new Coordinate(0, 0), scale);
	}
	
	public Rect measureString(String str, double x, double y, double scale)
	{
		return measureString(str, new Coordinate(x, y), scale);
	}
	
	public Rect measureString(String str, Coordinate p, double scale)
	{
		Rect r;
		
		r = new Rect(baseFontMetrics.getStringBounds(str,g));
		r.move(convert(p));
		r = revConvert(r);
		r.scale(internalScaleForScale(scale));
		
		return r;
	}
	
	public void drawString(String str, Coordinate p, double scale, Color c)
	{
		drawString(str, p.x, p.y, scale, c);
	}
	
	public void drawString(
					String str, 
					double x, 
					double y, 
					double scale, 
					Color c)
	{
		g.setColor(c);
		// scale/translate!
		scale = internalScaleForScale(scale);
		x = convertX(x);
		y = convertY(y);
		g.translate(x,y);
		g.scale(scale,scale);
		
		// draw!
		g.drawString(str, 0.0f, 0.0f);
		
		// remove scaling/translation!
		g.setTransform(baseTransform);
	}
	
	public void drawString(
					String str, 
					Coordinate p, 
					double scale, 
					Color textCol, 
					Color bgCol)
	{
		fillRect(measureString(str, p, scale), bgCol);
		drawString(str, p, scale, textCol);
	}
	
	public void drawString(
					String str, 
					double x, 
					double y, 
					double scale, 
					Color textCol, 
					Color bgCol)
	{
		drawString(str, new Coordinate(x,y), scale, textCol, bgCol);
	}
	
	public void drawStringTopLeft(
					String str, 
					Coordinate p, 
					double scale, 
					Color c)
	{
		Rect r;
		r = measureString(str, 0, 0, scale);
		p = new Coordinate(p.x - r.left, p.y - r.top);
		drawString(str, p, scale, c);
	}
	
	public void drawStringTopLeft(
					String str, 
					double x, 
					double y, 
					double scale, 
					Color c)
	{
		drawStringTopLeft(str, new Coordinate(x,y), scale, c);
	}
	
	public void drawStringTopLeft(
					String str, 
					Coordinate p, 
					double scale, 
					Color textCol, 
					Color bgCol)
	{
		Rect r;
		r = measureString(str, 0, 0, scale);
		p = new Coordinate(p.x - r.left, p.y - r.top);
		r = new Rect(0,0,r.width,r.height);
		fillRect(r, bgCol);
		drawString(str, p, scale, textCol);
	}
	
	public void drawStringTopLeft(
					String str, 
					double x, 
					double y, 
					double scale, 
					Color textCol, 
					Color bgCol)
	{
		drawStringTopLeft(str, new Coordinate(x,y), scale, textCol, bgCol);
	}
	
	public void drawLine(
					double x1,
					double y1,
					double x2,
					double y2,
					double width,
					Color c)
	{
		drawLine(new Coordinate(x1,y1), new Coordinate(x2,y2), width, c);
	}
	
	public void drawLine(
					Coordinate p1,
					Coordinate p2,
					double width,
					Color c)
	{
		Rect b = currentBounds();
		g.setColor(c);
		
		Coordinate p1a, p1b, p2a, p2b, a;
		
		a = new Coordinate(-(p2.y - p1.y), p2.x - p1.x);
		a.scale(width*0.5/a.distance(new Coordinate()));
		
		p1a = new Coordinate();
		p1a.add(p1,a);
		p1b = new Coordinate();
		p1b.sub(p1,a);
		p2a = new Coordinate();
		p2a.add(p2,a);
		p2b = new Coordinate();
		p2b.sub(p2,a);
		
		g.fill(
			createQuad(
				p1a.x,p1a.y,
				p1b.x,p1b.y,
				p2b.x,p2b.y,
				p2a.x,p2a.y));
	}
	
	public void drawLine(Coordinate[] p, double width, Color c)
	{
		Coordinate p1a, p1b, p2a, p2b, a, d;
		int	i;
		Rect b = currentBounds();
		g.setColor(c);
		
		a = new Coordinate(-(p[1].y - p[0].y), p[1].x - p[0].x);
		a.scale(width*0.5/a.distance(new Coordinate()));
		d = new Coordinate();
		
		p1a = new Coordinate();
		p1a.add(p[0],a);
		p1b = new Coordinate();
		p1b.sub(p[0],a);
		for(i = 1; i < p.length; i++)
		{
			p2a = p1a;
			p2b = p1b;
			p2a.add(d);
			p2b.add(d);
			
			a = new Coordinate(-(p[i].y - p[i-1].y), p[i].x - p[i-1].x);
			a.scale(width*0.5/a.distance(new Coordinate()));
			d = new Coordinate(p[i].x - p[i-1].x, p[i].y - p[i-1].y);
			d.scale(0.0);
			
			p2a.add(d);
			p2b.add(d);
			
			p1a = new Coordinate();
			p1a.add(p[i],a);
			p1a.sub(d);
			p1b = new Coordinate();
			p1b.sub(p[i],a);
			p1b.sub(d);
			
			g.fill(
				createQuad(
					p1a.x,p1a.y,
					p1b.x,p1b.y,
					p2b.x,p2b.y,
					p2a.x,p2a.y));
		}
	}
	
	public void drawLine(
					Coordinate p1,
					double startDistance,
					Coordinate p2,
					double stopDistance,
					double width,
					Color c)
	{
		if(p1 == null || p2 == null)
			return;
		
		Coordinate a = new Coordinate();
		Coordinate b = new Coordinate();
		
		a.sub(p2,p1);
		b.sub(p2,p1);
		
		a.scale(startDistance);
		b.scale(stopDistance);
		
		a.add(p1);
		b.add(p1);
		
		drawLine(
			a, 
			b,
			width,
			c);
	}
	
	public void drawArrow(
					double x1,
					double y1,
					double head_size_1,
					double x2,
					double y2,
					double head_size_2,
					double width,
					Color c)
	{
		drawArrow(
			new Coordinate(x1,y1), 
			head_size_1, 
			new Coordinate(x2,y2), 
			head_size_2, 
			width, 
			c);
	}
	
	public void drawArrow(
					Coordinate[] p, 
					double head_size_1,
					double head_size_2,
					double width, 
					Color col)
	{
		if(p.length > 1)
		{
			drawLine(p,width,col);
			if(head_size_1 != 0.0)
			{
				Coordinate a,b,c,d,e,f,p1,p2;
				
				p1 = p[0];
				p2 = p[1];
				
				c = new Coordinate(-(p2.y - p1.y), p2.x - p1.x);
				d = new Coordinate(p2.x - p1.x, p2.y - p1.y);
				c.scale(0.5/c.distance(new Coordinate()));
				d.scale(1.0/d.distance(new Coordinate()));
				c.scale(head_size_1);
				d.scale(head_size_1);
				a = p1.clone();
				b = p1.clone();
				
				a.sub(c);
				a.add(d);
				b.add(c);
				b.add(d);
				drawLine(p1,a,width,col);
				drawLine(p1,b,width,col);
			}
			if(head_size_2 != 0.0)
			{
				Coordinate a,b,c,d,p1,p2;
				
				p1 = p[p.length-2];
				p2 = p[p.length-1];
				
				c = new Coordinate(-(p2.y - p1.y), p2.x - p1.x);
				d = new Coordinate(p2.x - p1.x, p2.y - p1.y);
				c.scale(0.5/c.distance(new Coordinate()));
				d.scale(1.0/d.distance(new Coordinate()));
				c.scale(head_size_2);
				d.scale(head_size_2);
				a = p2.clone();
				b = p2.clone();
				
				a.sub(c);
				a.sub(d);
				b.add(c);
				b.sub(d);
				drawLine(p2,a,width,col);
				drawLine(p2,b,width,col);
			}
		}
	}
	
	public void drawArrow(
					Coordinate p1,
					double head_size_1,
					Coordinate p2,
					double head_size_2,
					double width,
					Color col)
	{
		Coordinate a,b,c,d,e,f;
		
		c = new Coordinate(-(p2.y - p1.y), p2.x - p1.x);
		d = new Coordinate(p2.x - p1.x, p2.y - p1.y);
		c.scale(0.5/c.distance(new Coordinate()));
		d.scale(1.0/d.distance(new Coordinate()));
		
		drawLine(p1,p2,width,col);
		if(head_size_1 != 0.0)
		{
			e = c.clone();
			f = d.clone();
			e.scale(head_size_1);
			f.scale(head_size_1);
			a = p1.clone();
			b = p1.clone();
			
			a.sub(e);
			a.add(f);
			b.add(e);
			b.add(f);
			drawLine(p1,a,width,col);
			drawLine(p1,b,width,col);
		}
		if(head_size_2 != 0.0)
		{
			e = c.clone();
			f = d.clone();
			e.scale(head_size_2);
			f.scale(head_size_2);
			a = p2.clone();
			b = p2.clone();
			
			a.sub(e);
			a.sub(f);
			b.add(e);
			b.sub(f);
			drawLine(p2,a,width,col);
			drawLine(p2,b,width,col);
		}
	}
	
	public void drawArrow(
					Coordinate p1,
					double startDistance,
					double head_size_1,
					Coordinate p2,
					double stopDistance,
					double head_size_2,
					double width,
					Color c)
	{
		if(p1 == null || p2 == null)
			return;
		
		Coordinate a = new Coordinate();
		Coordinate b = new Coordinate();
		
		a.sub(p2,p1);
		b.sub(p2,p1);
		
		a.scale(startDistance);
		b.scale(stopDistance);
		
		a.add(p1);
		b.add(p1);
		
		drawArrow(
			a, 
			head_size_1,
			b,
			head_size_2,
			width,
			c);
	}
	
	public Rect drawRect(
					Coordinate p1,
					Coordinate p2,
					double width,
					Color c)
	{
		if(p1 == null || p2 == null)
			return null;
		return 
			drawRect(
				p1.x, p1.y,
				p2.x, p2.y,
				width,
				c);
	}
	
	public Rect drawRect(
					Rect r,
					double width,
					Color c)
	{
		if(r == null)
			return null;
		return 
			drawRect(
				r.left, r.top,
				r.right, r.bottom,
				width,
				c);
	}
	
	public Rect drawRect(
					double x1, 
					double y1, 
					double x2, 
					double y2, 
					double width,
					Color c)
	{
		g.setColor(c);
		drawLine(x1-width/2,y1,x2+width/2,y1,width,c);
		drawLine(x2,y1-width/2,x2,y2+width/2,width,c);
		drawLine(x2+width/2,y2,x1-width/2,y2,width,c);
		drawLine(x1,y2+width/2,x1,y1-width/2,width,c);
		return new Rect(x1, y1, x2, y2);
	}
	
	public Rect fillRect(
					Coordinate p1,
					Coordinate p2,
					Color c)
	{
		return 
			fillRect(
				p1.x, p1.y,
				p2.x, p2.y,
				c);
	}
	
	public Rect fillRect(
					Rect r,
					Color c)
	{
		return 
			fillRect(
				r.left, r.top,
				r.right, r.bottom,
				c);
	}
	
	public Rect fillRect(
					double x1, 
					double y1, 
					double x2, 
					double y2, 
					Color c)
	{
		g.setColor(c);
		Rect b = currentBounds();
		double _x1 = convertX(x1);
		double _y1 = convertY(y1);
		double _x2 = convertX(x2);
		double _y2 = convertY(y2);
		g.fill(
			new Rectangle2D.Double(
				_x1,
				_y1,
				_x2 - _x1,
				_y2 - _y1));
		return new Rect(x1, y1, x2, y2);
	}
	
	public Rect drawImageInRect(
					Rect r,
					BufferedImage im)
	{
		return
			drawImageInRect(
				r.left, r.top,
				r.right, r.bottom,
				im);
	}
	
	public Rect drawImageInRect(
					double x1, 
					double y1,
					double x2, 
					double y2,
					BufferedImage im)
	{
		Rect b = currentBounds();
		double _x1 = convertX(x1);
		double _y1 = convertY(y1);
		double _x2 = convertX(x2);
		double _y2 = convertY(y2);
		double w = im.getWidth();
		double h = im.getHeight();
		AffineTransform at = new AffineTransform();
		at.translate(_x1,_y1);
		at.scale((_x2-_x1)/w,(_y2-_y1)/h);
		g.drawImage(
			im,
			at,
			null);
		return new Rect(x1, y1, x2, y2);
	}
	
	public Rect fillOval(
					Rect r,
					Color c)
	{
		return 
			fillOval(
				r.topLeft(),
				r.bottomRight(),
				0.0,
				null,
				c);
	}
	
	public Rect fillOval(
					Coordinate p1,
					Coordinate p2,
					Color c)
	{
		return 
			fillOval(
				p1,
				p2,
				0.0,
				null,
				c);
	}
	
	public Rect fillOval(
					Coordinate p1,
					Coordinate p2,
					double edgeWidth,
					Color edgeColor,
					Color c)
	{
		return
			fillOval(
				p1.x, p1.y,
				p2.x, p2.y,
				edgeWidth,
				edgeColor,
				c);
	}
	
	public Rect fillOval(
					double x1, 
					double y1, 
					double x2, 
					double y2, 
					Color c)
	{
		return
			fillOval(
				x1, y1,
				x2, y2,
				0.0,
				null,
				c);
	}
	
	public Rect fillOval(
					double x1, 
					double y1, 
					double x2, 
					double y2, 
					double edgeWidth,
					Color edgeColor,
					Color c)
	{
		pushBounds(x1,y1,x2,y2);
		fillOval(edgeWidth, edgeColor, c);
		return popBounds();
	}
	
	public Rect fillCircle(
					Coordinate p, 
					double diameter,
					double edgeWidth,
					Color edgeColor,
					Color c)
	{
		return fillCircle(p.x,p.y,diameter,edgeWidth,edgeColor,c);
	}
	
	public Rect fillCircle(
					double x, 
					double y, 
					double diameter,
					Color c)
	{
		return fillCircle(x,y,diameter,0.0,null,c);
	}
	
	public Rect fillCircle(
					double x, 
					double y, 
					double diameter,
					double edgeWidth,
					Color edgeColor,
					Color c)
	{
		double radius = diameter/2.0;
		pushBounds(
			x - radius,y - radius,
			x + radius,y + radius);
		fillOval(
			edgeWidth,
			edgeColor,
			c);
		return popBounds();
	}
	
	public void fillOval(
					Color col)
	{
		fillOval(0.0,null,col);
	}
	
	public void fillOval(
					double edgeWidth,
					Color edgeColor,
					Color col)
	{
		Rect b = currentBounds();
		boolean drawEdge = (edgeWidth > 0.0);
		
		if(drawEdge)
			g.setColor(edgeColor);
		else
			g.setColor(col);
		{
		double _x1 = convertX(0.0);
		double _y1 = convertY(0.0);
		double _x2 = convertX(1.0);
		double _y2 = convertY(1.0);
		g.fill(
			new Ellipse2D.Double(
				_x1,
				_y1,
				_x2 - _x1,
				_y2 - _y1));
		}
		if(drawEdge)
		{
			g.setColor(col);
			double _x1 = convertX(0.0 + edgeWidth);
			double _y1 = convertY(0.0 + edgeWidth);
			double _x2 = convertX(1.0 - edgeWidth);
			double _y2 = convertY(1.0 - edgeWidth);
			g.fill(
				new Ellipse2D.Double(
					_x1,
					_y1,
					_x2 - _x1,
					_y2 - _y1));
		}
	}
	
	public Rect fillCube(
					Coordinate p,
					double size,
					Color surfaceColor)
	{
		return
			fillCube(
				p,
				size,
				0.0,
				null,
				surfaceColor);
	}
	public Rect fillCube(
					Coordinate p,
					double size,
					double edgeWidth,
					Color edgeColor,
					Color surfaceColor)
	{
		return 
			fillCube(
				p.x - size/2, p.y - size/2,
				p.x + size/2, p.y + size/2,
				edgeWidth,
				edgeColor,
				surfaceColor);
	}
	
	public Rect fillCube(
					Coordinate p1,
					Coordinate p2,
					Color surfaceColor)
	{
		return fillCube(p1,p2,0.0,null,surfaceColor);
	}
	
	public Rect fillCube(
					Coordinate p1,
					Coordinate p2,
					double edgeWidth,
					Color edgeColor,
					Color surfaceColor)
	{
		return 
			fillCube(
				p1.x, p1.y,
				p2.x, p2.y,
				edgeWidth,
				edgeColor,
				surfaceColor);
	}
	
	public Rect fillCube(
					double x1, 
					double y1, 
					double x2, 
					double y2, 
					Color surfaceColor)
	{
		return fillCube(x1,y1,x2,y2,0.0,null,surfaceColor);
	}
	
	public Rect fillCube(
					double x1, 
					double y1, 
					double x2, 
					double y2, 
					double edgeWidth,
					Color edgeColor,
					Color surfaceColor)
	{
		pushBounds(x1,y1,x2,y2);
		fillCube(
			edgeWidth,
			edgeColor,
			surfaceColor);
		return popBounds();
	}
	
	private GeneralPath createQuad(
						double x1,
						double y1,
						double x2,
						double y2,
						double x3,
						double y3,
						double x4,
						double y4)
	{
		Rect b = currentBounds();
		GeneralPath p = new GeneralPath();
		float x,y;
		
		x = (float) convertX(x1);
		y = (float) convertY(y1);
		p.moveTo(x,y);
		x = (float) convertX(x2);
		y = (float) convertY(y2);
		p.lineTo(x,y);
		x = (float) convertX(x3);
		y = (float) convertY(y3);
		p.lineTo(x,y);
		x = (float) convertX(x4);
		y = (float) convertY(y4);
		p.lineTo(x,y);
		
		p.closePath();
		
		return p;
	}
	
	private GeneralPath createHex(
						double x1,
						double y1,
						double x2,
						double y2,
						double x3,
						double y3,
						double x4,
						double y4,
						double x5,
						double y5,
						double x6,
						double y6)
	{
		Rect b = currentBounds();
		GeneralPath p = new GeneralPath();
		float x,y;
		
		x = (float) convertX(x1);
		y = (float) convertY(y1);
		p.moveTo(x,y);
		x = (float) convertX(x2);
		y = (float) convertY(y2);
		p.lineTo(x,y);
		x = (float) convertX(x3);
		y = (float) convertY(y3);
		p.lineTo(x,y);
		x = (float) convertX(x4);
		y = (float) convertY(y4);
		p.lineTo(x,y);
		x = (float) convertX(x5);
		y = (float) convertY(y5);
		p.lineTo(x,y);
		x = (float) convertX(x6);
		y = (float) convertY(y6);
		p.lineTo(x,y);
		
		p.closePath();
		
		return p;
	}
	
	public void fillCube(
					double edgeWidth,
					Color edgeColor,
					Color surfaceColor)
	{
		int x,y;
		Polygon p;
		boolean drawEdge = (edgeWidth > 0.0);
		
		if(drawEdge)
			g.setColor(edgeColor);
		else
			g.setColor(surfaceColor);
		
		g.fill(
			createHex(
				0.5, 0.0,
				1.0, 0.25,
				1.0, 0.75,
				0.5, 1.0,
				0.0, 0.75,
				0.0, 0.25));
		if(drawEdge)
		{
			g.setColor(surfaceColor);
			
			// FIXME: these coordinates aren't quite right...
			double topEdgeWidth = Math.sqrt(2*edgeWidth*edgeWidth);
			g.fill(
				createQuad(
					0.5, 0.0 + edgeWidth,
					1.0 - topEdgeWidth, 0.25,
					0.5, 0.5 - edgeWidth,
					0.0 + edgeWidth, 0.25));
			g.fill(
				createQuad(
					0.0 + edgeWidth, 0.25 + edgeWidth,
					0.5 - edgeWidth, 0.5,
					0.5 - edgeWidth, 1.0 - edgeWidth,
					0.0 + edgeWidth, 0.75 - edgeWidth));
			g.fill(
				createQuad(
					1.0 - edgeWidth, 0.25 + edgeWidth,
					0.5 + edgeWidth, 0.5,
					0.5 + edgeWidth, 1.0 - edgeWidth,
					1.0 - edgeWidth, 0.75 - edgeWidth));
		}
	}
}
