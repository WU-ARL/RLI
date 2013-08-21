/*
 * Copyright (c) 2008 Jyoti Parwatikar and Washington University in St. Louis.
 * All rights reserved
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author or Washington University may not be used
 *       to endorse or promote products derived from this source code
 *       without specific prior written permission.
 *    4. Conditions of any other entities that contributed to this are also
 *       met. If a copyright notice is present from another entity, it must
 *       be maintained in redistributions of the source code.
 *
 * THIS INTELLECTUAL PROPERTY (WHICH MAY INCLUDE BUT IS NOT LIMITED TO SOFTWARE,
 * FIRMWARE, VHDL, etc) IS PROVIDED BY THE AUTHOR AND WASHINGTON UNIVERSITY
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR WASHINGTON UNIVERSITY
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS INTELLECTUAL PROPERTY, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * */

/*
 * File: HostGraphic.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 11/07/2003
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.*;

import javax.swing.event.*;
import java.beans.PropertyChangeEvent;

public class HostGraphic  extends ONLGraphic
{
	private HLabel label = null;
	private HLabel userLabel = null;
	private WSGraphic wsgraphic = null;
	private TopButtonListener buttonListener = null;
	private Point2D.Double center = null;
	private int originalW = 0;
	private int originalH = 0;

	public interface HButton extends ONLComponentButton
	{
		public HostGraphic getHostGraphic();
	}

	private class HLabel extends JLabel implements HButton //, ONLComponentButton
	{
		private HostGraphic hgraphic = null;
		private int numChars = 0;
		public HLabel(HostGraphic hg, String lbl)
		{
			super(lbl);
			numChars = lbl.length();
			setDoubleBuffered(true);
			setHorizontalTextPosition(SwingConstants.CENTER);
			setHorizontalAlignment(SwingConstants.CENTER);
			hgraphic = hg;
		}
		public HostGraphic getHostGraphic() { return hgraphic;}
		public ONLComponent getONLComponent() { return (hgraphic.getONLComponent());}
		public void setText(String s) { numChars = s.length(); super.setText(s);}
		public int getNumChars() { return numChars;}
	}//end HLabel 
	
	private class WSGraphic extends JComponent implements HButton //, ONLComponentButton
	{
		private Rectangle2D.Double outerRect = null;
		private Rectangle2D.Double innerRect = null;
		private Polygon keyboard = null;
		private HostGraphic hgraphic = null;
		private Color convertedWhite = null;
		private Color convertedBlue = null;
		private Color convertedBlack = null;
		private Color convertedScreenColor = null;
		private Color screenColor = Color.blue;

		public WSGraphic(HostGraphic hg) { this(hg, null);}
		public WSGraphic(HostGraphic hg, Color c)
		{
			super();
			setDoubleBuffered(true);
			//setAlignmentX(Component.CENTER_ALIGNMENT);
			outerRect = new Rectangle2D.Double();
			innerRect = new Rectangle2D.Double();
			keyboard = new Polygon();
			setVisible(true);
			setOpaque(false);
			hgraphic = hg;
			if (c == null && hgraphic.getInterfaceType().equals(ExperimentXML.ITYPE_10G)) screenColor =  Color.pink;
			else if (c != null) screenColor = c;
			resetColors();
		}
		public HostGraphic getHostGraphic() { return hgraphic;}
		public void setSize(int w, int h)
		{
			super.setSize(w,h);
			double h2 = h - 3; //10 for the label and 3 for space between monitor and keyboard
			double tmp_h = .75 * h2;
			double tmp_w = .75 * w;
			int tmp_x = (int)(w/8);
			outerRect.setRect(tmp_x, 0, tmp_w, tmp_h);
			innerRect.setRect((tmp_x + (tmp_w/8)), (tmp_h/8), (tmp_w * .75), (tmp_h * .75)); 
			keyboard = new Polygon();
			int tmp_y = (int)(tmp_h + 3);
			keyboard.addPoint(tmp_x,tmp_y);
			keyboard.addPoint((int)(tmp_x + tmp_w),tmp_y);
			keyboard.addPoint(w, h);
			keyboard.addPoint(0, h);
			//image.setSize(w,h);
			this.revalidate();
			repaint();
		}
		public void resetColors()
		{
			convertedWhite = hgraphic.convertColor(Color.white);
			convertedBlue = hgraphic.convertColor(Color.blue);
			convertedBlack = hgraphic.convertColor(Color.black);
			convertedScreenColor = hgraphic.convertColor(screenColor);
			//if (hgraphic.getInterfaceType().equals(ExperimentXML.ITYPE_10G)) convertedScreenColor =  hgraphic.convertColor(Color.pink);
			//else convertedScreenColor = convertedBlue;
		}
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			//System.out.println("HostGraphic.WSGraphic::paintComponent");
			Graphics2D g2 = (Graphics2D)g;
			Color oldColor = g2.getColor();
			Font oldFont = g2.getFont();
			int w = getWidth();
			int h = getHeight();
			g2.setFont(getFont()); 
			g2.setColor(convertedWhite);
			g2.fill(outerRect);
			g2.fill(keyboard); 
			g2.setColor(convertedScreenColor);
			//if (hgraphic.getInterfaceType().equals(ExperimentXML.ITYPE_10G)) g2.setColor(convertedPink);
			//else g2.setColor(convertedBlue);//(Color.lightGray);
			g2.fill(innerRect);
			g2.setColor(convertedBlack);
			g2.draw(outerRect);
			g2.draw(innerRect);
			g2.draw(keyboard);
			g2.setFont(oldFont); 
			g2.setColor(oldColor);
		}
		public ONLComponent getONLComponent() { return (hgraphic.getONLComponent());}
		public void setScreenColor(Color c) 
		{ 
			screenColor = c;
			convertedScreenColor = hgraphic.convertColor(c);
		}
	}//end WSGraphic


	public HostGraphic(ONLComponent h) { this(h, h.getIconColor());}
	public HostGraphic(ONLComponent h, Color c)
	{
		super(h);
		setDoubleBuffered(true);
		//image = new JLabel(new ImageIcon("images/host.gif"));
		//image.setVisible(true);
		//add(image);
		label = new HLabel(this, h.getLabel());
		label.setFont(new Font("Dialog", Font.PLAIN, 9));
		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setForeground(Color.black);
		add(label);
		wsgraphic = new WSGraphic(this, c);
		wsgraphic.setLocation(0,0);
		add(wsgraphic);
		userLabel = new HLabel(this, h.getUserLabel());
		userLabel.setFont(new Font("Dialog", Font.PLAIN, 9));
		userLabel.setHorizontalAlignment(SwingConstants.LEFT);
		userLabel.setForeground(Color.black);
		userLabel.setFont(new Font("Dialog", Font.BOLD, 10));
		add(userLabel);
		setOpaque(false);
		buttonListener = new TopButtonListener();
		buttonListener.setEnabled(true);
		addMouseListener(buttonListener);
		addMouseMotionListener(buttonListener);
		label.addMouseListener(buttonListener);
		label.addMouseMotionListener(buttonListener);
		userLabel.addMouseListener(buttonListener);
		userLabel.addMouseMotionListener(buttonListener);
		//label.addMouseListener(getActivateListener());
		wsgraphic.addMouseListener(buttonListener);
		wsgraphic.addMouseMotionListener(buttonListener);
		//wsgraphic.addMouseListener(getActivateListener());
	}  
	public String getInterfaceType() { return (onlComponent.getInterfaceType());}
	/*public void setSize(int w, int h)//pre userLabel
	{
		originalW = w;
		originalH = h;
		int lbl_w = label.getNumChars() * 9;
		if (lbl_w > w)
		{
			super.setSize(lbl_w,h);
			float g_loc = (lbl_w - w)/8;
			//ExpCoordinator.print("HostGraphic.setSize(" + w + "," + h + ") lbl_w:" + lbl_w + " g_loc:" + g_loc);
			wsgraphic.setLocation((int)g_loc, 0);
			label.setSize(lbl_w,11);
		}
		else
		{
			super.setSize(w,h);
			label.setSize(w,11);
		}
		wsgraphic.setSize(w,(h-11));
		if (lbl_w < w)
		{
			float l_loc = (w - lbl_w)/2;
			label.setLocation((int)l_loc,(h-11));
		}
		else
			label.setLocation(0,(h-11));
		//setLabelSize();
		center = new Point2D.Double((getWidth()/2), (getHeight()/2));
		revalidate();
		repaint();
	}*/
	public void setSize(int w, int h)
	{
		originalW = w;
		originalH = h;
		int ulbl_w = userLabel.getNumChars() * 9;
		int lbl_w = label.getNumChars() * 9;
		if (ulbl_w > lbl_w) lbl_w = ulbl_w;
		if (lbl_w > w)
		{
			super.setSize(lbl_w,h);
			float g_loc = (lbl_w - w)/8;
			//ExpCoordinator.print("HostGraphic.setSize(" + w + "," + h + ") lbl_w:" + lbl_w + " g_loc:" + g_loc);
			wsgraphic.setLocation((int)g_loc, 11);
			label.setSize(lbl_w,11);
			userLabel.setSize(lbl_w,11);
		}
		else
		{
			super.setSize(w,h);
			label.setSize(w,11);
			userLabel.setSize(lbl_w,11);
		}
		wsgraphic.setSize(w,(h-22));
		if (lbl_w < w)
		{
			float l_loc = (w - lbl_w)/2;
			label.setLocation((int)l_loc,(h-11));
		}
		else
			label.setLocation(0,(h-11));
		userLabel.setLocation(0,0);
		//setLabelSize();
		center = new Point2D.Double((getWidth()/2), (getHeight()/2));
		revalidate();
		repaint();
	}
	public void setLabelSize() 
	{ 
		int uw = userLabel.getNumChars() * 9;
		int w = label.getNumChars() * 9;
		if (uw > w) w = uw;
		label.setSize(w,11);
	}
	public boolean contains(int x, int y)
	{
		if (wsgraphic.contains(x,y) || label.contains(x,y)) return true;
		else return false;
	}
	public void addNodeListener(ONLGraphic.ButtonListener l) 
	{
		buttonListener.addAction(l);
	}
	public void removeNodeListener(ONLGraphic.ButtonListener l) 
	{
		buttonListener.removeAction(l);
	}
	public void revalidate()
	{
		super.revalidate();      
		label.revalidate();
		wsgraphic.revalidate();
		//image.revalidate();
	}
	public Point2D getCenter()
	{
		return center;
		//Point2D.Double rtn = new Point2D.Double((getWidth()/2), (getHeight()/2));
		//return rtn;
	}
	public Point2D getLinkPoint()
	{
		Point loc = getLocation();
		//ExpCoordinator.print("HostGraphic::getLinkPoint location = ( " + loc.getX() + ", " + loc.getY() + ")");
		Point rtn = new Point((getWidth()/8), (getHeight()/2));//((getWidth()/2), (getHeight()/2));
		rtn.translate((int)loc.getX(), (int)loc.getY());
		//ExpCoordinator.print("                          link point = ( " + rtn.getX() + ", " + rtn.getY() + ")");
		return rtn;
	}
	/*
  public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      wsgraphic.paintComponent(g);
      //label.paintComponent(g);
    }*/
	public void addDragListener(ONLGraphic.ButtonListener dListener)
	{
		wsgraphic.addMouseListener(dListener);
		label.addMouseListener(dListener);
		wsgraphic.addMouseMotionListener(dListener);
		label.addMouseMotionListener(dListener);
		//addNodeListener(dListener);
	}
	public void removeDragListener(ONLGraphic.ButtonListener dListener)
	{
		wsgraphic.removeMouseListener(dListener);
		label.removeMouseListener(dListener);
		wsgraphic.removeMouseMotionListener(dListener);
		label.removeMouseMotionListener(dListener);
		//removeNodeListener(dListener);
	}
	protected void setStateChange(String st) //override from ONLGraphic to disable clicks and change colors
	{
		boolean b = (st.equals(ONLComponent.ACTIVE) || 
				st.equals(ONLComponent.WAITING) || 
				st.equals(ONLComponent.IN1) || 
				st.equals(ONLComponent.INBOTH) || 
				st.equals(ONLComponent.IN2));
		//System.out.println("HostGraphic::setStateChange " + st + " " + b);
		//buttonListener.setEnabled(getONLComponent().isActive());
		wsgraphic.resetColors();
		buttonListener.setEnabled(b);
	}
	public void propertyChange(PropertyChangeEvent e)
	{
		if (e.getSource() == onlComponent)
		{
			// if ((ExpCoordinator.getDebugLevel() > 12) && e.getPropertyName().equals(ONLComponent.D_LABEL))
			//label.setText((String)e.getNewValue());
			//else
			//{
			if (e.getPropertyName().equals(ONLComponent.LABEL)) 
			{
				label.setText((String)e.getNewValue());
				setSize(originalW, originalH);
			}
			if (e.getPropertyName().equals(ExperimentXML.USER_LABEL)) 
			{
				userLabel.setText((String)e.getNewValue());
				setSize(originalW, originalH);
			}
			// }
			wsgraphic.resetColors();
			super.propertyChange(e);
		}
	}
	public void setUserLabel(String s)
	{
		userLabel.setText(s);
		setSize(originalW, originalH);
		wsgraphic.resetColors();
	}
	public void setIconColor(Color c)
	{
		wsgraphic.setScreenColor(c);
		setSize(originalW, originalH);
		wsgraphic.resetColors();
	}
/*
	public void setONLComponent(ONLComponent c) 
	{ 
		super.setONLComponent(c);
		c.addPropertyListener(ExperimentXML.USER_LABEL, this);
	}
	*/
}
