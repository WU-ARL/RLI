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
 * File: HardwareGraphic.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 1/14/2008
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.awt.*;
import java.awt.geom.*;
import java.lang.Math;
import java.lang.String;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;

import javax.swing.event.*;
import javax.swing.*;//JComponent;
//import javax.swing.JLabel;
import java.util.Vector;
import javax.xml.stream.*;

public class HardwareGraphic extends ONLGraphic
{   
	protected static final int TEST_HWGRAPHIC = 1;
	protected static final int MAIN_BUTTON = -1;
	protected static final int OFFSET = 5; //space between the inner and outer border of the component
	protected static final int D_OFFSET = 12;//12; //2*OFFSET - the offset for the diameter
	private MainButton mainButton = null; //circular button 1/3 diameter of switch representation
	private Area mButtonArea = null; //area that defines mainButton space used for computing the area of the portButton
	private PortButton portButtons[] = null; 
	private PortButton.PArea portAreas[] = null; //pie segments
	private int numPorts = 8;
	private boolean dimensionChange = true;
	private boolean selected = false;
	private Point2D.Double startOfDrag;
	private TopButtonListener pButtonListener = null;  
	private TopButtonListener mButtonListener = null;
	protected static final double MBUTTON_FRACTION = 0.4;
	private ComponentLabel componentLabel = null;
	private ComponentLabel userLabel = null;
	private int originalW = 0;
	private int originalH = 0;
	//private double diameter = 0;

	private Ellipse2D.Double borderEllipse = null;


	private double radius = 0;
	private double labelRadius = 0; //radius used to calculate where to draw the label on the port button
	private double CPRadius = 0; //radius used to calculate where to put the port point on the cp port button

	private SpinnerButton spinner = null;
	private boolean graphicSelected = false;

	public static interface HWButton 
	{
		public boolean isPortButton();
		public boolean isMainButton();
		public void setPressed(boolean b) ;
		public boolean isPressed();
	}

	protected static class ComponentLabel extends JLabel implements ONLComponentButton
	{
		private ONLComponent component = null;
		private int numChars = 0;

		public ComponentLabel(String lbl, ONLComponent c)
		{
			super(lbl);
			component = c;
			setFont(new Font("Dialog", Font.PLAIN, 9));
			setHorizontalAlignment(SwingConstants.CENTER);
			setHorizontalTextPosition(SwingConstants.CENTER);
			setForeground(Color.black);
			numChars = lbl.length();
		}
		public ONLComponent getONLComponent() { return component;}
		public void setText(String s) { numChars = s.length(); super.setText(s);}
		public int getNumChars() { return numChars;}
	}//end class HardwareGraphic.CLabel

	protected static class SpinnerButton extends JComponent 
	{
		private int curIndex = 0;
		private boolean selected = false;
		private HardwareGraphic routerGraphic;
		private Ellipse2D.Double ellipse = null;
		private static final double SRADIUS = 4; //radius of spinner
		public SpinnerButton(HardwareGraphic nspg) 
		{
			super();
			setDoubleBuffered(true);
			routerGraphic = nspg;
			ellipse = new Ellipse2D.Double();
			setCenter();
			setOpaque(false);
			setVisible(true);
		}
		public boolean contains(int dx, int dy)
		{
			return (ellipse.contains(dx,dy));
		}
		public int getIndex() { return curIndex;}
		public void setIndex(int ndx) 
		{ 
			int diff = HardwareGraphic.mod((ndx - curIndex), routerGraphic.numPorts);
			if (diff > 0)
			{
				//ExpCoordinator.printer.print(new String("SpinnerButton.setIndex from " + curIndex + " to " + ndx), 10);
				if ((HardwareGraphic.mod((curIndex + diff), routerGraphic.numPorts)) == ndx) routerGraphic.spinClockwise(diff);
				else
				{
					routerGraphic.spinCClockwise(diff);
				}

				if (curIndex != ndx) 
				{
					curIndex = ndx;
					setCenter();
				}
				routerGraphic.revalidate();
				routerGraphic.repaint();
			}
		}
		public void setCenter()
		{
			if ((curIndex < routerGraphic.numPorts) && (curIndex >= 0))
			{
				//ExpCoordinator.printer.print("SpinnerButton::setCenter " + curIndex);
				Point2D.Double cen_pnt = routerGraphic.getPortArea(curIndex).getConnectorPoint();
				//Point2D.Double cen_pnt = (Point2D.Double)routerGraphic.getPortButton(curIndex).getCenter();
				Point2D tmp_pnt = new Point2D.Double();
				tmp_pnt.setLocation((cen_pnt.getX() - SRADIUS), (cen_pnt.getY() - SRADIUS));
				ellipse.setFrameFromCenter(cen_pnt,tmp_pnt);
			}
		}
		public boolean isSelected() { return selected;}
		public void setSelected(boolean b) 
		{ 
			if (selected != b)
			{
				selected = b;
				resetColor();
			}
		}
		public void resetColor()
		{
			if (selected) setBackground(routerGraphic.convertColor(Color.red));
			else setBackground(routerGraphic.convertColor(Color.pink));
			setCenter();
		}
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			//ExpCoordinator.printer.print("SpinnerButton::paintComponent");
			//setCenter();
			Graphics2D g2 = (Graphics2D)g;
			Color oldColor = g2.getColor();
			g2.setColor(getBackground());
			g2.fill(ellipse);
			g2.setColor(routerGraphic.getForeground());
			g2.draw(ellipse);
			g2.setColor(oldColor);
		}

	}// inner class Spinner


	private class SpinnerListener extends TopButtonListener //MouseInputAdapter
	{
		private HardwareGraphic routerGraphic = null;
		private boolean selected = false;
		private Point2D.Double startOfDrag = null;
		private SpinnerButton spinner = null;
		public SpinnerListener(HardwareGraphic nsp_g)
		{
			super();
			routerGraphic = nsp_g;
			spinner = nsp_g.spinner;
		}
		public void mousePressed(java.awt.event.MouseEvent e)
		{
			//ExpCoordinator.print(new String("HardwareGraphic.SpinnerListener.mousePressed point=" + e.getPoint() + " spinner=" + spinner.getLocation()), TEST_HWGRAPHIC);
			if (spinner.contains(e.getPoint()))
			{
				selected = true;
				startOfDrag = new Point2D.Double(e.getX(), e.getY());
				spinner.setSelected(true);
				routerGraphic.revalidate();
				routerGraphic.repaint();
				//spinner.repaint();
			}
			else super.mousePressed(e);
		}
		public void mouseReleased(java.awt.event.MouseEvent e)
		{
			if (selected)
			{
				selected = false;
				startOfDrag = null;
				//ExpCoordinator.printer.print("SpinnerReleased");
				spinner.setSelected(false);
				routerGraphic.revalidate();
				routerGraphic.repaint();
				//routerGraphic.printPorts();
			}
			else super.mouseReleased(e);
		}
		public void mouseDragged(java.awt.event.MouseEvent e)
		{
			//if not in this component do nothing
			if (selected)
			{
				//convert x,y coords into double
				double dx = (double)e.getX();
				double dy = (double)e.getY();

				//use for spinner
				//if spinner moves to a new section clockwise or counter clockwise update picture
				int spin_ndx = spinner.getIndex();
				int tmp_ndx = spin_ndx;

				for (int i = 0; i < numPorts; ++i)
				{
					if (routerGraphic.getPortArea(i).containsSpinner(dx,dy))
					{
						tmp_ndx = i;
						if (tmp_ndx != spin_ndx) ExpCoordinator.print(new String("HardwareGraphic.SpinnerListener.mouseDragged index change from " + spin_ndx + " to " + tmp_ndx));
						//break;
					}
				}
				spinner.setIndex(tmp_ndx); //should set spinners new location
				startOfDrag.setLocation(dx, dy);
				routerGraphic.revalidate();
				routerGraphic.repaint();
			}
			else super.mouseDragged(e);
		} 
	} //inner class SpinnerListener

	public static class MainButton extends JComponent implements HWButton, ONLComponentButton //Ellipse2D.Double
	{
		private HardwareGraphic routerGraphic = null;
		private String mlabel;
		private int labelOffsetX = -1;
		private int labelOffsetY = -1;
		private double labelX = 0;
		private double labelY = 0;
		private Ellipse2D.Double ellipse = null;
		private boolean pressed = false;
		private Point2D.Double center = null;

		public MainButton(HardwareGraphic nspg, String lbl)
		{
			super();
			setDoubleBuffered(true);
			//ExpCoordinator.printer.print("MainButton::MainButton for " + lbl);
			routerGraphic = nspg;
			mlabel = lbl;
			ellipse = new Ellipse2D.Double();
			center = new Point2D.Double();
			setOpaque(false);
			setVisible(true);
			setFont(new Font("Dialog", Font.PLAIN, 8));
			addMouseListener(new MouseAdapter()
			{ 
				public void mousePressed(MouseEvent e)
				{
					setPressed(true);
				}
				public void mouseReleased(MouseEvent e)
				{
					setPressed(false);
				}
			});
		}
		public void setSize(double d)
		{
			this.setSize((int)d,(int)d);
			Point p = getLocation();
			ellipse.setFrame(p.getX(), p.getY(), d, d);
			center.setLocation(d/2, d/2);
			labelX = ellipse.getCenterX() - 9;
			labelY = ellipse.getCenterY() + 2;
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			//ExpCoordinator.printer.print("MainButton::paintComponent");
			Graphics2D g2 = (Graphics2D)g;
			Color oldColor = g2.getColor();
			Font oldFont = g2.getFont();
			g2.setFont(routerGraphic.getFont()); 
			//if (pressed)
			//{
			//  g2.setColor(routerGraphic.getBackground().darker());
			//  g2.fill(ellipse);
			//}
			g2.setColor(routerGraphic.getForeground());
			g2.draw(ellipse);
			//drawLabel(g2);
			g2.setColor(oldColor);
			g2.setFont(oldFont);
		}
		public void drawLabel(Graphics2D g2)
		{
			//place label of switch id in middle of button
			float fsize = 8;
			g2.setFont(getFont());
			g2.setColor(routerGraphic.getForeground());
			g2.drawString(mlabel, (int)labelX, (int)labelY);
		}
		public Point2D getCenter() 
		{
			//Point2D.Double rtn = new Point2D.Double( ellipse.getCenterX(),  ellipse.getCenterY());
			return center;
		}

		//interface HWButton
		public boolean isPortButton() { return false;}
		public boolean isMainButton() { return true;}
		public void setPressed(boolean b) 
		{ 
			if (pressed != b)
			{
				pressed = b;
				routerGraphic.repaint();
			}
		}
		public boolean isPressed() { return pressed;}
		//end interface HWButton

		public ONLComponent getONLComponent() { return routerGraphic.getONLComponent();}

	}//inner class MainButton

	public static class PortButton extends ONLGraphic implements HWButton  //Arc2D.Double
	{
		private static final double CONNRADIUS = 3; //radius of port connector graphic
		private String plabel;
		private Ellipse2D.Double pconnector;
		private boolean pressed = false;
		private HardwareGraphic routerGraphic = null;
		private PArea parea = null;
		private int index;
		private int numPorts;
		private int portID = 0;
		private int labelX = 0;
		private int labelY = 0;

		protected static class PArea extends Area
		{
			private int index;
			private int numPorts;
			private Arc2D.Double arc = null;
			private Area area = null;
			private Point2D.Double ppPoint = null;
			private Point2D.Double cpPoint = null;
			private Point2D.Double lblPoint = null;
			private double labelTheta = 0; //angle needed to draw points
			private HardwareGraphic routerGraphic = null;

			public PArea(HardwareGraphic routerg, int ndx)
			{
				super();
				index = ndx;
				numPorts = routerg.numPorts;
				arc = new Arc2D.Double(Arc2D.PIE);
				numPorts = routerg.numPorts;
				routerGraphic = routerg;

				cpPoint = new Point2D.Double();
				ppPoint = new Point2D.Double();	
				lblPoint = new Point2D.Double();

				double ext = 360/numPorts;
				arc.setAngleExtent(ext);
				double ang_strt = 90 - (index * ext);// + 90;
				arc.setAngleStart(ang_strt);
				double labelDegrees = ang_strt + (ext/2);
				labelTheta = Math.toRadians(labelDegrees);
			}

			public void setPoints(double x, double y, double rad)
			{
				//ExpCoordinator.printer.print("PortButton " + index + "::SetPoints center = (" + x + ", " + y + ") r = " + rad);
				double tmp_x =(Math.cos(labelTheta)*rad) + x;
				double tmp_y = y - (Math.sin(labelTheta)*rad);
				ppPoint.setLocation(tmp_x, tmp_y);
				//ExpCoordinator.printer.print("   ppPoint = (" + tmp_x + ", " + tmp_y);

				double tmp_r = rad * MBUTTON_FRACTION;
				tmp_x = (Math.cos(labelTheta)*tmp_r) + x;
				tmp_y = y - (Math.sin(labelTheta)*tmp_r);
				cpPoint.setLocation(tmp_x, tmp_y);
				//ExpCoordinator.printer.print("   cpPoint = (" + tmp_x + ", " + tmp_y);

				tmp_r = (rad * (MBUTTON_FRACTION + 1))/2;
				tmp_x = (Math.cos(labelTheta)*tmp_r) + x;
				tmp_y = y - (Math.sin(labelTheta)*tmp_r);
				lblPoint.setLocation(tmp_x, tmp_y);
				//ExpCoordinator.printer.print("   lblPoint = (" + tmp_x + ", " + tmp_y);
			}
			public void setSize(double d, Point loc)
			{
				Point p = loc;
				ExpCoordinator.print(new String("PortButton.PArea setSize " + d + " location:(" + p.getX() + ", " + p.getY() + ") index = " + index), TEST_HWGRAPHIC);
				//arc.setFrame((p.getX() + HardwareGraphic.OFFSET), (p.getY() + HardwareGraphic.OFFSET), d, d);
				arc.setFrame(p.getX(), p.getY(), d, d);
				reset();
				add(new Area(arc));
				subtract(routerGraphic.getMainButtonArea());
				setPoints(arc.getCenterX(), arc.getCenterY(), (d/2));
			}

			public Point2D getLinkPoint()
			{
				Point loc = routerGraphic.getLocation();
				Point tmp_p = new Point((int)ppPoint.getX(), (int)ppPoint.getY());
				tmp_p.translate((int)loc.getX(), ((int)loc.getY() + D_OFFSET));
				//ExpCoordinator.printer.print("                          link point = ( " + tmp_p.getX() + ", " + tmp_p.getY() + ")");
				return tmp_p;
			}   
			protected boolean containsSpinner(double x, double y) 
			{ 
				//boolean rtn = outerArc.contains(x,y);
				Rectangle2D.Double rect = new Rectangle2D.Double();
				Point2D endPoint = arc.getEndPoint();
				Point2D strPoint = arc.getStartPoint();
				rect.setFrameFromDiagonal(strPoint, endPoint);
				if (rect.getHeight() < 1) rect.setRect(rect.getX(), (rect.getY()-SpinnerButton.SRADIUS), rect.getWidth(), (rect.getHeight() + (2*SpinnerButton.SRADIUS)));
				if (rect.getWidth() < 1) rect.setRect((rect.getX()-SpinnerButton.SRADIUS), rect.getY(), (rect.getWidth() + (2*SpinnerButton.SRADIUS)), rect.getHeight());
				boolean rtn = rect.contains(x,y);
				if (index == 3)
					ExpCoordinator.print(new String("HardwareGraphic.PortButton.PArea(" + index + ").containsSpinner (" + x + ", " + y + ")" + " start=" + strPoint + " end=" + endPoint + " rtn=" + rtn), TEST_HWGRAPHIC);
				/*if (rtn)
	    {
	    ExpCoordinator.printer.print("PortButton (index,port) (" + index + ", " + port + ")::containsSpinner (" + x + ", " + y + ") " + rtn);	
	    //ExpCoordinator.printer.print("   intersecting rect (" + tmp_x + ", " + tmp_y + ", " + tmp_w + ", " + tmp_h +")");	
	    ExpCoordinator.printer.print("   with rect (" + rect.getX() + ", " + rect.getY() + ", " + rect.getWidth() + ", " + rect.getHeight() +")");
	    ExpCoordinator.printer.print("   ppPoint = (" + ppPoint.getX() + ", " + ppPoint.getY());
	    ExpCoordinator.printer.print("   cpPoint = (" + cpPoint.getX() + ", " + cpPoint.getY());
	    ExpCoordinator.printer.print("   lblPoint = (" + lblPoint.getX() + ", " + lblPoint.getY());
	    ExpCoordinator.printer.print("   startPoint = (" + strPoint.getX() + ", " + strPoint.getY());
	    ExpCoordinator.printer.print("   endPoint = (" + endPoint.getX() + ", " + endPoint.getY());
	    }*/
				return rtn;
			}
			public Point2D.Double getConnectorPoint() { return (ppPoint);}
		}
		//end PortButton.PArea inner class
		public PortButton(HardwareGraphic nspg, Hardware.Port p, int ndx)
		{
			super(p);
			setDoubleBuffered(true);
			portID = p.getID();
			plabel = String.valueOf(portID);
			numPorts = nspg.numPorts;
			routerGraphic = nspg;
			index = ndx;
			//parea = routerGraphic.getPortArea(index);
			pconnector = new  Ellipse2D.Double();

			//ExpCoordinator.printer.print("PortButton::PortButton " + ndx  + " angleStart:" + ang_strt + " ext:" + ext + " labelDegrees:" + labelDegrees);

			setOpaque(false);
			setVisible(true);
			addMouseListener(new MouseAdapter()
			{ 
				public void mousePressed(MouseEvent e)
				{
					if (!routerGraphic.spinner.contains(e.getPoint())) setPressed(true);
				}
				public void mouseReleased(MouseEvent e)
				{
					setPressed(false);
				}
			});
		}
		public Point2D.Double getConnectorPoint() { return (parea.ppPoint);}
		public double getConnectorTheta() { return (parea.labelTheta);}
		public Point2D getLinkPoint() { return (parea.getLinkPoint());}
		public boolean contains(int x, int y) { return (parea.contains(x,y));}

		public void incrementIndex(int i)
		{
			setIndex(HardwareGraphic.mod((index + i), numPorts));
		}
		public void decrementIndex(int i)
		{
			setIndex(HardwareGraphic.mod((index - i), numPorts));
		}
		public Hardware.Port getPort() { return ((Hardware.Port)getONLComponent());}
		protected int getIndex() { return index;}
		protected void setIndex(int ndx) 
		{ 
			//if (index != ndx || parea == null)
			// {
			index = ndx;
			setPArea();
		}

		public void setPArea()
		{
			parea = routerGraphic.getPortArea(index);
			labelX = (int)(parea.lblPoint.getX() - 3);
			labelY = (int)(parea.lblPoint.getY() + 7);

			Point2D cen_pnt = parea.ppPoint;
			Point2D tmp_pnt = new Point2D.Double();
			if (portID == 0) cen_pnt = parea.cpPoint;
			tmp_pnt.setLocation((cen_pnt.getX() - CONNRADIUS), (cen_pnt.getY() - CONNRADIUS));
			pconnector.setFrameFromCenter(cen_pnt,tmp_pnt);
		}
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (parea == null) setPArea();
			//ExpCoordinator.printer.print("PortButton::paintComponent index " + index);
			Graphics2D g2 = (Graphics2D)g;
			Color oldColor = g2.getColor();
			Font oldFont = g2.getFont();
			g2.setFont(routerGraphic.getFont()); 
			if (pressed)
			{
				g2.setColor(routerGraphic.getConvertedBG().darker());	  
				g2.fill(parea);
			}
			g2.setColor(routerGraphic.getForeground());
			g2.draw(parea);
			drawLabel(g2);
			drawPortConnector(g2);
			if (portID == 0) routerGraphic.spinner.paintComponent(g);
			g2.setColor(oldColor);
			g2.setFont(oldFont);
		}

		public void drawLabel(Graphics2D g2)
		{
			g2.setColor(routerGraphic.getForeground());
			g2.drawString(plabel, labelX, labelY);
		}
		public void drawPortConnector(Graphics2D g2)
		{
			g2.setColor(Color.black);//routerGraphic.getForeground());
			g2.fill(pconnector);
		}
		public Point2D getCenter() { return (parea.lblPoint);}
		//interface HWButton
		public boolean isPortButton() { return true;}
		public boolean isMainButton() { return false;}
		public void setPressed(boolean b) 
		{ 
			if (pressed != b)
			{
				//ExpCoordinator.printer.print("PortButton (index,port) (" + index + ", " + port + ")::setPressed");
				pressed = b;
				this.repaint();
			}
		}
		public boolean isPressed() { return pressed;}
		//end interface HWButton
		public void addDragListener(MouseInputListener dListener){}
		public void addComponentListener(ComponentListener cl)
		{
			super.addComponentListener(cl);
			routerGraphic.addComponentListener(cl);
		}
		public void removeComponentListener(ComponentListener cl)
		{
			super.removeComponentListener(cl);
			routerGraphic.removeComponentListener(cl);
		}
		private PArea getPArea() 
		{
			return (parea);
		}
		protected void setPArea(PArea p) { parea = p;}
		public int getScreenX() { 
			//ExpCoordinator.printer.print("PortButton::getScreenX " + routerGraphic.getLocation().getX());
			return ((int)routerGraphic.getLocation().getX());}
		public int getScreenY() { return ((int)routerGraphic.getLocation().getY());}
	}
	//inner class PortButton


	public HardwareGraphic(Hardware sd, Color bcolor, double d) 
	{
		this(sd, bcolor);
		setSize(d);
	}
	public HardwareGraphic(Hardware sd, Color bcolor)
	{
		super(sd);
		setDoubleBuffered(true);
		numPorts = sd.getNumPorts();
		ExpCoordinator.print(new String("HardwareGraphic::HardwareGraphic numPorts = " + numPorts), 2);
		mButtonArea = new Area();
		setForeground(Color.black);
		setBackground(bcolor);
		portButtons = new PortButton[numPorts];
		portAreas = new PortButton.PArea[numPorts];

		mainButton = new MainButton(this, onlComponent.getLabel());
		int i = 0;

		mainButton.addMouseListener(new MouseAdapter()
		{ 
			public void mousePressed(MouseEvent e)
			{
				setSelected();
			}
		});
		mButtonListener = new TopButtonListener();
		mButtonListener.setEnabled(true);
		//mainButton.addMouseListener(getActivateListener());
		mainButton.addMouseListener(mButtonListener);
		mainButton.addMouseMotionListener(mButtonListener);
		Hardware router = (Hardware)getONLComponent();
		for (i = 0; i < numPorts; ++i)
		{
			portAreas[i] = new PortButton.PArea(this, i);
		}
		PortButton tmp_pb = null;
		for (i = 0; i < numPorts; ++i)
		{
			ExpCoordinator.print(("HardwareGraphic.port " + i), 5);
			tmp_pb = new PortButton(this, router.getPort(i), i);
			//tmp_pb.setLocation(OFFSET, OFFSET);
			//tmp_pb.addMouseListener(pButtonListener);
			//tmp_pb.addMouseMotionListener(pButtonListener);
			//add(tmp_pb);
			portButtons[i] = tmp_pb;
		}

		//printPorts();

		spinner = new SpinnerButton(this);
		add(spinner,0);//add at the second position so it will be a top level component. i.e. the user can get to it

		pButtonListener = new SpinnerListener(this);//TopButtonListener();
		pButtonListener.setEnabled(true);
		//SpinnerListener sl = new SpinnerListener(this);
		//spinner.addMouseListener(sl);
		//spinner.addMouseMotionListener(sl);
		//addMouseListener(sl);
		//addMouseMotionListener(sl);
		for (i = 0; i < numPorts; ++i)
		{
			tmp_pb = portButtons[i];
			//tmp_pb.addMouseListener(sl);
			//tmp_pb.addMouseMotionListener(sl);
			tmp_pb.addMouseListener(pButtonListener);
			tmp_pb.addMouseMotionListener(pButtonListener);
			add(tmp_pb);
		}


		add(mainButton,1);
		
		borderEllipse = new Ellipse2D.Double();
		setOpaque(false);
		setFont(new Font("Dialog", Font.PLAIN, 11));
		componentLabel = new ComponentLabel(sd.getLabel(),router);
		componentLabel.addMouseListener(mButtonListener);
		componentLabel.addMouseMotionListener(mButtonListener);

		componentLabel.addMouseListener(new MouseAdapter()
		{ 
			public void mousePressed(MouseEvent e)
			{
				setSelected();
			}
		});
		add(componentLabel);
		userLabel = new ComponentLabel(sd.getUserLabel(),router);
		userLabel.addMouseListener(mButtonListener);
		userLabel.addMouseMotionListener(mButtonListener);
		userLabel.setFont(new Font("Dialog", Font.BOLD, 10));

		userLabel.addMouseListener(new MouseAdapter()
		{ 
			public void mousePressed(MouseEvent e)
			{
				setSelected();
			}
		});
		add(userLabel);
		repaint();
	}

	public void setSize(double d)
	{
		setSize((int)d, (int)d);
	}

	public void setSize(int w, int h)
	{
		originalW = w;
		originalH = h;
		int h2 = h - (2*D_OFFSET);
		int d = w - D_OFFSET;
		if (h2 < w) d = h2;//h2;//h - (2*D_OFFSET);
		borderEllipse.setFrame(0, D_OFFSET, d, d);
		int d3 = (int)(d/2 *(1 - MBUTTON_FRACTION));
		userLabel.setLocation(0, 0);
                //userLabel.setHorizontalAlignment(SwingConstants.LEFT);
		int ulbl_w = userLabel.getNumChars() * 9;
		int lbl_w = componentLabel.getNumChars() * 9;
		if (ulbl_w > lbl_w) lbl_w = ulbl_w;

		if (lbl_w > w)
		{
			super.setSize(lbl_w,h);
			//float g_loc = (lbl_w - w)/8;
			//ExpCoordinator.print("HostGraphic.setSize(" + w + "," + h + ") lbl_w:" + lbl_w + " g_loc:" + g_loc);
			userLabel.setSize(lbl_w,D_OFFSET);
			componentLabel.setSize(lbl_w,D_OFFSET);
		}
                else
		    {
			super.setSize(w,h);
			userLabel.setSize(d,D_OFFSET);
			componentLabel.setSize(d,D_OFFSET);
		    }
		ExpCoordinator.print(new String("HardwareGraphic::setSize " + w + " " + h +" d=" + d + " d3=" + d3), TEST_HWGRAPHIC);
		//mainButton.setLocation((d3 + OFFSET), (d3 + OFFSET));
		mainButton.setLocation(d3, (d3 + D_OFFSET));
		mainButton.setSize(MBUTTON_FRACTION*d);
		mButtonArea.reset();
		Ellipse2D.Double mb_ellipse = new Ellipse2D.Double(mainButton.ellipse.getX(), (mainButton.ellipse.getY()-D_OFFSET), mainButton.ellipse.getWidth(), mainButton.ellipse.getHeight());
		mButtonArea.add(new Area(mb_ellipse));//mainButton.ellipse));
		PortButton elem = null;
		for (int i = 0; i < numPorts; ++i)
		{
			elem = portButtons[i];
			elem.setLocation(0, D_OFFSET);
			Point elem_loc = new Point(0,0);//elem.getX(), (elem.getY() + D_OFFSET));
		    //Point elem_loc = elem.getLocation();
		    //elem.setLocation(elem_loc);
			portAreas[i].setSize(d, elem_loc);
			elem.setSize(d, d);
			elem.setIndex(i);
		}
		spinner.setCenter();
		spinner.revalidate();
		componentLabel.setLocation(0, (d + D_OFFSET));
                //componentLabel.setHorizontalAlignment(SwingConstants.LEFT);
		revalidate();
		repaint();
	}
	public void setUserLabel(String s)
	{
		userLabel.setText(s);
		setSize(originalW, originalH);
	}
    /*original setSize without adjustment for label size*/
    /*
	public void setSize(int w, int h)
	{
		super.setSize(w,h);
		int h2 = h - (2*D_OFFSET);
		int d = w - D_OFFSET;
		if (h2 < w) d = h2;//h2;//h - (2*D_OFFSET);
		borderEllipse.setFrame(0, D_OFFSET, d, d);
		int d3 = (int)(d/2 *(1 - MBUTTON_FRACTION));
		userLabel.setLocation(0, 0);
		userLabel.setSize(d,D_OFFSET);
		ExpCoordinator.print(new String("HardwareGraphic::setSize " + w + " " + h +" d=" + d + " d3=" + d3), TEST_HWGRAPHIC);
		//mainButton.setLocation((d3 + OFFSET), (d3 + OFFSET));
		mainButton.setLocation(d3, (d3 + D_OFFSET));
		mainButton.setSize(MBUTTON_FRACTION*d);
		mButtonArea.reset();
		Ellipse2D.Double mb_ellipse = new Ellipse2D.Double(mainButton.ellipse.getX(), (mainButton.ellipse.getY()-D_OFFSET), mainButton.ellipse.getWidth(), mainButton.ellipse.getHeight());
		mButtonArea.add(new Area(mb_ellipse));//mainButton.ellipse));
		PortButton elem = null;
		for (int i = 0; i < numPorts; ++i)
		{
			elem = portButtons[i];
			elem.setLocation(0, D_OFFSET);
			Point elem_loc = new Point(0,0);//elem.getX(), (elem.getY() + D_OFFSET));
		    //Point elem_loc = elem.getLocation();
		    //elem.setLocation(elem_loc);
			portAreas[i].setSize(d, elem_loc);
			elem.setSize(d, d);
			elem.setIndex(i);
		}
		spinner.setCenter();
		spinner.revalidate();
		componentLabel.setLocation(0, (d + D_OFFSET));
		componentLabel.setSize(d,D_OFFSET);
		revalidate();
		repaint();
		}*/

	private HardwareGraphic.PortButton.PArea getPortArea(int i)
	{
		return (portAreas[i]);
	}

	public HardwareGraphic.PortButton getPortButton(int i)
	{
		return (portButtons[i]);
	}

	protected PortButton getPortButton(Hardware.Port p) 
	{ 
		//ExpCoordinator.print(new String("HardwareGraphic.getPortButton " + p.getID()), 2);
		return (portButtons[p.getID()]);}

	public void setSize(Dimension dim)
	{
		setSize((int)dim.getWidth(), (int)dim.getHeight());
	}

	public boolean contains(int x, int y)
	{
		return (borderEllipse.contains(x, y));
	}

	public void spinCClockwise(int c)
	{
		PortButton elem = null;
		//ExpCoordinator.printer.print(" spinCCLock " + c);
		for (int i = 0; i < numPorts; ++i)
		{
			elem = portButtons[i];
			//elem.incrementIndex(c);
			elem.decrementIndex(c);
		}
		revalidate();
	}
	public void spinClockwise(int c)
	{
		PortButton elem = null;
		//ExpCoordinator.printer.print(" spinCLock " + c);
		for (int i = 0; i < numPorts; ++i)
		{
			elem = portButtons[i];
			//elem.decrementIndex(c);
			elem.incrementIndex(c);
		}
		revalidate();
	}
	public void paintComponent(Graphics g)
	{
		//ExpCoordinator.printer.print("HardwareGraphic::paintComponent", 2);
		Graphics2D g2 = (Graphics2D)g;
		Color oldColor = g2.getColor();
		Font oldFont = g2.getFont();
		g2.setFont(getFont()); 
		//if (graphicSelected) g2.setColor(getBackground().darker());
		//else  g2.setColor(getBackgound());
		g2.setColor(getConvertedBG());
		g2.fill(borderEllipse);
		g2.setColor(getForeground());
		g2.draw(borderEllipse);
		g2.setColor(oldColor);
		g2.setFont(oldFont);
		mainButton.paintComponent(g);
		super.paintComponent(g);
	}
	public void addPortListener(ONLGraphic.ButtonListener l) 
	{
		pButtonListener.addAction(l);
	}
	public void removePortListener(ONLGraphic.ButtonListener l) 
	{
		pButtonListener.removeAction(l);
	}
	public void addNodeListener(ONLGraphic.ButtonListener l) 
	{
		//ExpCoordinator.print(new String("HardwareGraphic(" + onlComponent.getLabel() + ").addNodeListener " + l.toString()), ExpCompare.TEST_CMP);
		mButtonListener.addAction(l);
	}
	public void removeNodeListener(ONLGraphic.ButtonListener l)  { mButtonListener.removeAction(l);}

	protected Area getMainButtonArea() { return (mButtonArea);}

	public boolean isSpinning() { return spinner.isSelected();}

	public int getSpinnerPosition() { return (spinner.getIndex());}
	public void setSpinnerPosition(int i) { spinner.setIndex(i);}

	public static int mod(int x, int m)
	{
		if (x < m) 
		{
			if (x >= 0) return x;
			else
			{
				return (mod((x+m), m));
			}
		}
		else
		{
			if (x == 0 && m == 0) return 0;
			else return (mod((x-m),m));
		}
	}
	public void printPorts()
	{
		for (int i = 0; i < numPorts; ++i)
		{
			ExpCoordinator.printer.print("PortButton (index,port) (" +  portButtons[i].getIndex() + ", " + portButtons[i].getPort() + ")");	
		}
	}

	public void revalidate()
	{
		super.revalidate();
		if ((mainButton != null) && 
				(portButtons != null) && 
				(spinner != null))
		{
			for (int i = 0; i < numPorts; ++i)
			{
				portButtons[i].revalidate();
			}
			mainButton.revalidate();
			spinner.revalidate();
		}
		if (componentLabel != null) componentLabel.revalidate();
	}
	public void setSelected()
	{
		if (graphicSelected) graphicSelected = false;
		else graphicSelected = true;
		//if (onlComponent != null && onlComponent instanceof VirtualTopology.VNode)
		//{
			//ONLComponent real_component = ((VirtualTopology.VNode)onlComponent).getPhysicalComponent();
			//if (real_component != null) real_component.setSelected(graphicSelected);
		//}
	}
	public void addComponentListener(ComponentListener cl)
	{
		super.addComponentListener(cl);
		spinner.addComponentListener(cl);
	}
	public void addDragListener(ONLGraphic.ButtonListener dListener)
	{
		mainButton.addMouseListener(dListener);
		mainButton.addMouseMotionListener(dListener);
		componentLabel.addMouseListener(dListener);
		componentLabel.addMouseMotionListener(dListener);
		//addNodeListener(dListener);
	}
	public void removeDragListener(ONLGraphic.ButtonListener dListener)
	{
		mainButton.removeMouseListener(dListener);
		mainButton.removeMouseMotionListener(dListener);
		componentLabel.removeMouseListener(dListener);
		componentLabel.removeMouseMotionListener(dListener);
		//removeNodeListener(dListener);
	}
	protected void setStateChange(String st) //override from ONLGraphic to disable clicks and change colors
	{
		boolean b = (st.equals(ONLComponent.ACTIVE) || 
				st.equals(ONLComponent.WAITING) || 
				st.equals(ONLComponent.IN1) || 
				st.equals(ONLComponent.INBOTH) || 
				st.equals(ONLComponent.IN2));
		//ExpCoordinator.printer.print("HardwareGraphic::setStateChange " + st + " " + b);
		mButtonListener.setEnabled(b);
		pButtonListener.setEnabled(b);
		spinner.resetColor();
	} 
	//public Color getBackground() { return (getConvertedBG());}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		super.writeXML(xmlWrtr);
		xmlWrtr.writeStartElement(ExperimentXML.SPINNER);
		xmlWrtr.writeCharacters(String.valueOf(getSpinnerPosition()));
		xmlWrtr.writeEndElement();
	}
	public void propertyChange(PropertyChangeEvent e)
	{
		if (e.getSource() == onlComponent)
		{
			if (e.getPropertyName().equals(ExperimentXML.USER_LABEL)) 
			{
				userLabel.setText((String)e.getNewValue());
				//setSize(getWidth(), getHeight());
			}
			super.propertyChange(e);
			revalidate();
		}
	}
}
