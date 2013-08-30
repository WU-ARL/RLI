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
 * File: LinkGraphic.java
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

import java.awt.geom.*;
import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.PropertyChangeEvent;

public class LinkGraphic extends ONLGraphic implements ComponentListener
{
	private ONLGraphic point1 = null;
	private ONLGraphic point2 = null;
	private LinkDescriptor ldescriptor = null;
	private Point intermediatePnt = null;
	private Line2D.Double line = null;
	private Ellipse2D.Double loopback = null;
	private boolean updateLoopTransform = false;
	private AffineTransform loopTransform = null;
	private Shape shape = null;
	private final int LOOPBACK_RAD = 10;
	private final float STROKE_WIDTH_1G = 2;
	private final float STROKE_WIDTH_10G = 4;
	private boolean is_selected = false;
	private BasicStroke stroke = null;
	private float strokeWidth = STROKE_WIDTH_1G;
	private Point oldP1 = null;
	private Point oldP2 = null;
	private int count = 0;

	public LinkGraphic()
	{
		super();
		setOpaque(false);
		setVisible(true);
		line = new Line2D.Double();
		loopback = new Ellipse2D.Double();
		shape = line;
		stroke = new BasicStroke((float)strokeWidth);
		setForeground(Color.black);
	}
	public LinkGraphic(LinkDescriptor ld)
	{
		this();
		setLinkDescriptor(ld);
	}
	public boolean isSelfLoop()
	{
		return (ldescriptor != null && ldescriptor.getPoint1() == ldescriptor.getPoint2());
	}
	public void setLinkDescriptor(LinkDescriptor ld) 
	{ 
		ldescriptor = ld;
		setONLComponent(ld);
		/*ExpCoordinator.print("LinkGraphic::setLinkDescriptor " + ldescriptor.toString());      
      ExpCoordinator.print("           " + ldescriptor.getPoint1().getLabel() + " to " +  ldescriptor.getPoint2().getLabel());        
      if (ldescriptor.getPoint1() != null && ldescriptor.getPoint1().getGraphic() == null)    
         ExpCoordinator.print("           " + ldescriptor.getPoint1().getLabel() + "'s graphic is null");              
      if (ldescriptor.getPoint2() != null && ldescriptor.getPoint2().getGraphic() == null)    
      ExpCoordinator.print("           " + ldescriptor.getPoint2().getLabel() + "'s graphic is null");        */
		if (ldescriptor.getPoint1() != null)
		{
			if (ldescriptor.getInterfaceType().equals(ExperimentXML.ITYPE_10G)) strokeWidth = STROKE_WIDTH_10G; 
			if (ldescriptor.getPoint2() != null)
			{
				setPoints(ldescriptor.getPoint1().getGraphic(), ldescriptor.getPoint2().getGraphic());
			}
			else setPoint1(ldescriptor.getPoint1().getGraphic());
		}
		setStateChange(ldescriptor.getProperty(ONLComponent.STATE));
		revalidate();
		repaint();
	}
	public void setONLComponent(ONLComponent c) 
	{ 
		super.setONLComponent( c);
		c.addPropertyListener(ONLComponent.SELECTED, this);
		c.addPropertyListener(ONLComponent.INTERFACE_TYPE, this);
	}
	private Rectangle setBounds()
	{
		//if (onlComponent != null) ExpCoordinator.print("LinkGraphic::setBounds for " + onlComponent.getLabel());
		//else ExpCoordinator.print("LinkGraphic::setBounds component is null");
		Rectangle r = new Rectangle();
		if (point1 == null)  return r;

		if ((point2 == null) && (((LinkDescriptor)onlComponent).getPoint2() != null)) 
			point2 = ((LinkDescriptor)onlComponent).getPoint2().getGraphic();

		if ((point2 == null) &&
				(intermediatePnt == null))
			return r;

		Point2D tmp_p = point1.getLinkPoint();
		Point p1 = new Point((int)tmp_p.getX(), (int)tmp_p.getY());
		tmp_p = intermediatePnt;
		if (point2 != null)
		{
			//ExpCoordinator.print("                      setting from point2");
			tmp_p = point2.getLinkPoint();
		}

		Point p2 = new Point((int)tmp_p.getX(), (int)tmp_p.getY());

		//if (point1 == point2)
		//	{
		//    ExpCoordinator.print(new String(onlComponent.getLabel() + "::LinkGraphic.setBounds p1 = (" + p1.getX() + "," + p1.getY() + ") p2 = (" + p2.getX() + "," + p2.getY() + ")"), 6);
		//    if (oldP1 != null && oldP2 != null) ExpCoordinator.print(new String("oldP1 = (" + oldP1.getX() + "," + oldP1.getY() + ") oldP2 = (" + oldP2.getX() + "," + oldP2.getY() + ")"), 6);
		//  }
		//if nothing has moved leave the computations alone
		//if (count > 1 && oldP1 != null && oldP2 != null && p1.equals(oldP1) && p2.equals(oldP2)) return (getBounds());
		//if ( p1.equals(oldP1) && p2.equals(oldP2)) ++count;
		oldP1 = new Point(p1);
		oldP2 = new Point(p2);

		int x = -1 * getX();
		int y = -1 * getY();
		r.setFrameFromDiagonal(p1, p2);

		if (point1 == point2)
		{
			//  ExpCoordinator.print("                      point1 == point2 loopback");
			//ExpCoordinator.print("                       (" + r.getX() + ", " + r.getY() + ", " + r.getWidth() + ", " + r.getHeight() +")");
			ONLComponent onlcomp = point1.getONLComponent();
			Point center = null;
			if (onlcomp instanceof NSPPort)
			{
				center = new Point((int)p1.getX(), (int)(p1.getY() - LOOPBACK_RAD));
				double rad = 2*LOOPBACK_RAD;
				Point corner = new Point((int)(p1.getX() - rad), (int)(p1.getY() - rad));
				r.setFrameFromCenter(p1, corner);
				p1.translate(x,y);
				double theta = (Math.PI/2) - ((HardwareGraphic.PortButton)point1).getConnectorTheta();
				AffineTransform tmp_trans = AffineTransform.getRotateInstance(theta, p1.getX(), p1.getY());
				if (loopTransform == null || !loopTransform.equals(tmp_trans)) 
				{
					corner = new Point((int)(center.getX() - LOOPBACK_RAD), (int)(center.getY() - LOOPBACK_RAD));
					center.translate(x,y);
					corner.translate(x,y);
					updateLoopTransform = true;
					loopTransform = tmp_trans;
					loopback.setFrameFromCenter(center, corner);
					shape = loopTransform.createTransformedShape(loopback);
				}
				else 
					ExpCoordinator.print(new String(onlComponent.getLabel() + "::LinkGraphic.setBounds transform same"), 6);
				ExpCoordinator.print(new String(onlComponent.getLabel() + "::LinkGraphic.setBounds loopback(" + loopback.getX() + ", " + loopback.getY() + ", " + loopback.getWidth() + ", " + loopback.getHeight() +") p1(" + p1.getX() + ", " + p1.getY() + ") theta = " + theta), 6);
			}
		}
		else
		{
			loopTransform = null;
			if ((point2 != null) &&
					(point1.getONLComponent().getParent() != null) &&
					(point1.getONLComponent().getParent() == point2.getONLComponent().getParent()))
			{
				ONLComponent onlcomp = point1.getONLComponent();
				//ExpCoordinator.print("i don't want to be here parents = " + point1.getParent() + " " + point2.getParent());
				if (onlcomp instanceof Hardware.Port) //onlcomp.isType(ONLComponent.WUGSPORT) || (onlcomp.isType(ONLComponent.NSPPORT)))
				{
					//ExpCoordinator.print("                      point1.parent == point2.parent loopback");
					//ExpCoordinator.print("                       (" + r.getX() + ", " + r.getY() + ", " + r.getWidth() + ", " + r.getHeight() +")");
					shape = loopback;
					Point center = new Point((int)r.getCenterX(), (int)r.getCenterY());
					//ExpCoordinator.print("                       center (" + center.getX() + ", " + center.getY() + ")");
					double rad = center.distance(p1);
					Point corner = new Point((int)(center.getX() - (rad + 3)), (int)(center.getY() - (rad + 3)));
					Point corner2 = new Point((int)(center.getX() - rad), (int)(center.getY() - rad));
					r.setFrameFromCenter(center, corner);
					center.translate(x,y);
					corner2.translate(x,y);
					loopback.setFrameFromCenter(center, corner2);
				}
			}
			else
			{
				//ExpCoordinator.print("                      line");
				shape = line;
				p1.translate(x,y);
				p2.translate(x,y);
				line.setLine(p1, p2);
			}

			if (r.getHeight() < 1) r.setSize((int)r.getWidth(), 10);
			if (r.getWidth() < 1) r.setSize(10, (int)r.getHeight());

		}
		setBounds(r);
		//ExpCoordinator.print("                       (" + r.getX() + ", " + r.getY() + ", " + r.getWidth() + ", " + r.getHeight() +")");

		return r;
	}
	public void setIntermediate(Point p) 
	{ 
		intermediatePnt = new Point(p);
		//intermediatePnt.translate((-1*getX()), (-1*getY()));
		//ExpCoordinator.print("LinkGraphic::setIntermediate (" + intermediatePnt.getX() + ", " + intermediatePnt.getY() + ")"); 
		setBounds();
		revalidate();
		repaint();
	}
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		setBounds();
		Graphics2D g2 = (Graphics2D)g;
		Color oldColor = g2.getColor();

		g2.setStroke(stroke);
		if (ldescriptor.isFailed() || ldescriptor.getState().equals(ONLComponent.IN2)) g2.setColor(Color.lightGray);
		else 
			g2.setColor(getForeground());
		g2.draw(shape);
		g2.setColor(oldColor);
	}
	public void setPoints(ONLGraphic p1, ONLGraphic p2)
	{
		if (point1 != p1)
		{
			if (point1 != null) point1.removeComponentListener(this);
			point1 = p1;
			point1.addComponentListener(this);
		}
		if (point2 != p2)
		{
			if (point2 != null) point2.removeComponentListener(this);
			point2 = p2;
			point2.addComponentListener(this);
		}
		setBounds();
		revalidate();
		repaint();
	}
	public void setPoint1(ONLGraphic p1)
	{
		if (point1 != p1)
		{
			if (point1 != null) point1.removeComponentListener(this);
			point1 = p1;
			point1.addComponentListener(this);
		}
		setBounds();
		revalidate();
		repaint();
	}
	public void setPoint2(ONLGraphic p2)
	{
		if (point2 != p2)
		{
			if (point2 != null) point2.removeComponentListener(this);
			point2 = p2;
			point2.addComponentListener(this);
		}
		setBounds();
		revalidate();
		repaint();
	}

	protected ONLGraphic getPoint1() { return point1;}
	protected ONLGraphic getPoint2() { return point2;}

	//ComponentListener
	public void componentMoved(ComponentEvent e) 
	{
		//ExpCoordinator.print("LinkGraphic.componentMoved", 2);
		setBounds();
		//revalidate();
		repaint();
	}
	public void componentHidden(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {} 
	{
		//ExpCoordinator.print("LinkGraphic.componentResized", 2);
		setBounds();
		//revalidate();
		repaint();
	}
	public void componentShown(ComponentEvent e)
	{
		//ExpCoordinator.print("LinkGraphic.componentShown", 2);
		setBounds();
		//revalidate();
		repaint();
	}
	//end ComponentListener
	protected void setSelectionChange(boolean b)
	{
		if (onlComponent == null) return;
		//ExpCoordinator.print("LinkGraphic.setSelectionChange " + b);
		if (b && (onlComponent.isActive() || onlComponent.isWaiting()))
		{
			setForeground(Color.yellow);
		}
		else setForeground(Color.black);
	}
	protected void setStateChange(String st) //override to disable clicks and change color
	{
		if (onlComponent == null) return;
		if (st.equals(ONLComponent.ACTIVE) || st.equals(ONLComponent.INBOTH))
		{
			stroke = new BasicStroke(strokeWidth);
			if (onlComponent.isSelected() && !st.equals(ONLComponent.INBOTH)) setForeground(Color.yellow);
			else setForeground(Color.black);
		}
		else
		{
			if(!(st.equals(ONLComponent.IN2) || st.equals(ONLComponent.IN1)))
			{
				setForeground(Color.black);
				float[] dashArray = new float[2];
				dashArray[0] = 3;
				dashArray[1] = 7;
				stroke = new MultiLineGraph.DashStroke(strokeWidth, dashArray);
				if (onlComponent.isSelected()) setForeground(Color.yellow);
			}
			else 
			{
				if (st.equals(ONLComponent.IN2))
					setForeground(Color.gray);
				else
					setForeground(Color.pink);
				//if (onlComponent.isSelected()) setForeground(Color.yellow);
			}
		}
	} 
	public void propertyChange(PropertyChangeEvent e) //overridden from ONLGraphic
	{
		//ExpCoordinator.print("LinkGraphic.propertyChange " + e.getPropertyName());
		if ((e.getSource() == onlComponent) && (e.getPropertyName().equals(ONLComponent.INTERFACE_TYPE)))
		{
		    if (e.getNewValue().equals(ExperimentXML.ITYPE_10G)) strokeWidth = STROKE_WIDTH_10G; 
		    else strokeWidth = STROKE_WIDTH_1G;
			setStateChange(ldescriptor.getProperty(ONLComponent.STATE));
		}
		if ((e.getSource() == onlComponent) && (e.getPropertyName().equals(ONLComponent.SELECTED)))
		{
			setSelectionChange(Boolean.valueOf((String)e.getNewValue()).booleanValue());
			//if (onlComponent != null) setStateChange(onlComponent.getProperty(ONLComponent.STATE));
			revalidate();
			repaint();
		}
		else super.propertyChange(e);
	}
}
