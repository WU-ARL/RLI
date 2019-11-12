/*
 * Copyright (c) 2005-2013 Jyoti Parwatikar
 * and Washington University in St. Louis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

/*
 * File: Axis.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/23/2003
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.awt.*;
import javax.swing.*;


//inner class for Axis
public abstract class Axis extends JPanel
{
	protected float spaceBetweenDivs;
	protected boolean isXAxis;
	protected Graph graph;
	protected int numberOfDivs;
	protected int left;
	protected int right;
	protected int top;
	protected int bottom;
	protected int units = Units.UNKNOWN;
	protected Arrow arrow = null;
	protected boolean hasArrow = false;
	protected FontMetrics fontMetrics = null;
	private boolean needsUpdate = false;
	//inner Division class draws the division and the label
	public class Division
	{
		public final int LEFT = 0;
		public final int CENTER = 1;
		public final int RIGHT = 2;
		private String label = null;
		private int justification = RIGHT;
		private int markerWidth = 5;
		private int position = 0;
		private int length = 8;
		private int half_length = 4;

		public Division(String lbl, int p)
		{
			setDimensions(lbl, p);
		}

		public void setLabel(String lbl)
		{
			label = lbl;
			if (fontMetrics != null)
			{
				length = fontMetrics.stringWidth(label);
				half_length = (int)(length/2);
			}
		}

		public void setDimensions(int p)
		{
			position = p;
		}

		public void setDimensions(String lbl, int p)
		{
			setLabel(lbl);
			position = p;
		}

		public void drawDivision(Graphics g)
		{	  
			if (isXAxis)
			{
				if ((position <= getMaxCoordinate()) || (position >= getMinCoordinate()))
				{
					g.drawLine(position, top, position, top + markerWidth);
					//add text: number += divunit
					g.drawString(label, (position - half_length), top + 25);
				}
			}
			else
			{
				if ((position >= getMaxCoordinate()) || (position <= getMinCoordinate())) //this is because origin is in top left
				{
					g.drawLine(right, position, right - markerWidth, position);
					//add text: number += divunit
					g.drawString(label, (right - length - 5), position + 3);
				}
			}
		}

		public void setMarkerWidth(int w) { markerWidth = w;}
		public int getLength() { return length;}
		public int compareTo(String id) { return (label.compareTo(id));}
	}

	//inner Arrow class
	public static class Arrow extends Polygon
	{
		public Arrow(int h, int w, Point c, boolean isVert) // height, width, center, points up or right
		{
			super();
			double w_2 = w/2;
			if (isVert)
			{
				addPoint((int)(c.getX() - w_2), (int)c.getY());
				addPoint((int)(c.getX() + w_2), (int)c.getY());
				addPoint((int)c.getX(), (int)(c.getY() - h));
			}
			else
			{
				addPoint((int)c.getX(), (int)(c.getY() - w_2));
				addPoint((int)c.getX(), (int)(c.getY() + w_2));
				addPoint((int)(c.getX() + h), (int)c.getY());
			}
		}
	}
	//end inner Arrow class

	//start inner Label class  
	public static class Label extends JPanel
	{
		private String lbl_text;
		private boolean isHorizontal = true;
		private FontMetrics fm = null;

		public Label(String txt, boolean isX) { this(txt,isX,12);}
		public Label(String txt, boolean isX, int fsize)
		{
			super();	
			setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
			this.setFont(new Font("Dialog", Font.BOLD, fsize));
			setForeground(new Color(102, 102, 153));
			lbl_text = txt;
			isHorizontal = isX;
		}
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			//if (fm == null)
			//  {
			//   fm = g.getFontMetrics();
			//   if (!isHorizontal) setPreferredSize(new Dimension((fm.getAscent() + fm.getDescent() + 5), (fm.getStringWidth(lbl_text) + 5)));
			// }
			//g2.setFont(new Font("Dialog", Font.PLAIN, 14));
			if (!isHorizontal) g2.rotate(Math.toRadians(90));
			g2.drawString(lbl_text, 0,0);
		}
		public String getText() { return lbl_text;}
		public void setText(String txt) { lbl_text = txt;}

	}
	//end inner Label class


	//start Axis class
	public Axis(Graph g, boolean xory)
	{
		graph = g;
		isXAxis = xory;
		setFont(new Font("Dialog", Font.BOLD, g.monitorManager.getAxisNFontSize()));//9));
		if (isXAxis)
		{
			setBorder(BorderFactory.createEmptyBorder(2,5,2,11));
		}
		else 
		{
			setPreferredSize(new Dimension(40,50));
			setBorder(BorderFactory.createEmptyBorder(11,2,5,2));
		}
	}

	public int length()
	{
		if (isXAxis) return (right - left);
		else return (bottom - top);
	}

	protected void updateBounds()
	{
		Insets insets = getInsets();
		left = insets.left;
		right = getWidth() - insets.right;
		top = insets.top;
		bottom = getHeight() - insets.bottom;
	}

	protected void drawMainLine(Graphics g)
	{
		if (isXAxis)
		{
			g.drawLine(left, top, right, top);
			if (hasArrow) 
			{
				arrow = new Arrow(8, 8, new Point(right,top), false);
				g.drawPolygon(arrow);
			}
		}
		else
		{
			g.drawLine(right, top, right, bottom);
			if (hasArrow) 
			{
				arrow = new Arrow(8, 8, new Point(right,top), true);
				g.drawPolygon(arrow);
			}
		}
	}

	public int getNumberOfDivs()
	{ 
		return numberOfDivs;
	}

	public float getSpaceBetweenDivs()
	{
		return spaceBetweenDivs;
	}



	public int getMinCoordinate()//returns the smallest coordinate for this axis
	{
		if (isXAxis)
			return left;
		else 
			return bottom;
	}


	public int getMaxCoordinate()//returns the largest coordinate for this axis
	{
		if (isXAxis)
			return right;
		else 
			return top;
	}

	public abstract int getCoordinate(double value); //translates a value into a coordinate on the axis
	public abstract void updateDimensions(); //should set the numberOfDivs and spaceBetweenDivs atleast
	//and call updateBounds to set left,right,top,bottom
	public abstract double getValue(int coord);

	public int getUnits() { return units;}
	public void setUnits(int u) { units = u;}
	public void setArrow(boolean b) 
	{ 
		hasArrow = b;
		if (b)
		{
			if (isXAxis)
				setBorder(BorderFactory.createEmptyBorder(5,5,5,11));
			else 
				setBorder(BorderFactory.createEmptyBorder(11,5,5,5));
		}
		else
		{
			if (isXAxis)
				setBorder(BorderFactory.createEmptyBorder(2,5,2,11));
			else 
				setBorder(BorderFactory.createEmptyBorder(11,2,5,2));
		}
	}
	public void setFont(Font f)
	{
		super.setFont(f);
		fontMetrics = null;
	}
	public void paintComponent(Graphics g)
	{
		if (needsUpdate)
		{
			updateDimensions();
			needsUpdate = false;
		}
		super.paintComponent(g);
		drawMainLine(g);
		if (fontMetrics == null) fontMetrics = g.getFontMetrics();
	}
	public void setUpdateDimensions(boolean b) { needsUpdate = b;} //should set to true instead of calling updateDimensions
	public boolean getUpdateDimensions() { return needsUpdate;}
}
