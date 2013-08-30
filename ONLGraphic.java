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
 * File: ONLGraphic.java
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
import javax.swing.JComponent;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.*;
import java.awt.color.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;

public class ONLGraphic extends JComponent implements ONLComponentButton, PropertyChangeListener
{
	public static final int TEST_BUTTON = 9;
	public static final int TEST_GRAPHIC = 9;
	public static final int TEST_PROPERTY_CHANGE = 5;
	protected ONLComponent onlComponent;
	private Color convertedBG = null;

	public static class ButtonListener extends ONL.UserAction implements MouseInputListener
	{
		private ONLComponent onlComponent = null;
		public ButtonListener() { this(null,"");}
		public ButtonListener(ONLComponent c) { this(c, "");}
		public ButtonListener(ONLComponent c, String lbl)
		{
			super(lbl, true, true);
			onlComponent = c;
		}
		/*
    public ButtonListener(ONLComponent c, int m)
      {
	super(m);
	onlComponent = c;
      }
    public ButtonListener(ONLComponent c, int m, String lbl)
      {
	super(m, lbl);
	onlComponent = c;
      }
		 */
		public ONLComponent getONLComponent() { return onlComponent;}
		public void setONLComponent(ONLComponent c) { onlComponent = c;}
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseDragged(MouseEvent e) {}
		public void mouseMoved(MouseEvent e) {}
		public void actionPerformed(ActionEvent e) {} //called on mousePressed 
	}

	public static class ButtonEvent extends ActionEvent
	{
		private MouseEvent mevent = null;
		public ButtonEvent(MouseEvent me)
		{
			this(me, 0, "");
		}
		public ButtonEvent(MouseEvent me, int i)
		{
			this(me, i, "");
		}
		public ButtonEvent(MouseEvent me, int i, String lbl)
		{
			super(me.getSource(), i, lbl);
			mevent = me;
		}
		public MouseEvent getMouseEvent() { return mevent;}
	}

	protected static class TopButtonListener extends ONL.CompositeAction implements MouseInputListener
	{
		public TopButtonListener()
		{
			super("", true, true); 
		}
		public void mouseClicked(MouseEvent e) { fireEvent(e);}
		public void mouseEntered(MouseEvent e)  { fireEvent(e);}
		public void mouseExited(MouseEvent e) { fireEvent(e);} 
		public void mousePressed(MouseEvent e) { fireEvent(e);} 
		public void mouseReleased(MouseEvent e)  { fireEvent(e);}
		public void mouseDragged(MouseEvent e)  { fireEvent(e);}    
		public void mouseMoved(MouseEvent e)  { fireEvent(e);}


		public void fireEvent(MouseEvent e)
		{
			if (!isEnabled()) return;
			int max = actions.size();
			ONLComponent onlcomp = ((ONLComponentButton)e.getSource()).getONLComponent();
			ButtonListener elem;
			ONLComponent tmp_c;
			String nm = "";
			switch(e.getID())
			{
			case MouseEvent.MOUSE_CLICKED:
				nm = "mouse_clicked";
				break;
			case MouseEvent.MOUSE_PRESSED:
				nm = "mouse_pressed";
				break;
			case MouseEvent.MOUSE_RELEASED:
				nm = "mouse_released";
				break;
			case MouseEvent.MOUSE_ENTERED:
				nm = "mouse_entered";
				break;
			case MouseEvent.MOUSE_EXITED:
				nm = "mouse_exited";
				break;
			case MouseEvent.MOUSE_DRAGGED:
				nm = "mouse_dragged";
				break;
			case MouseEvent.MOUSE_MOVED:
				nm = "mouse_moved";
				break;
			default:
				nm = new String("unknown:" + e.getID());
			}
			for (int i = 0; i < max; ++i)
			{
				elem = (ButtonListener)actions.elementAt(i);
				tmp_c = elem.getONLComponent();
				if (((tmp_c == onlcomp) || tmp_c == null) && elem.isEnabled()) 
				{
					switch(e.getID())
					{
					case MouseEvent.MOUSE_CLICKED:
						elem.mouseClicked(e);
						elem.actionPerformed(new ButtonEvent(e));
						break;
					case MouseEvent.MOUSE_PRESSED:
						elem.mousePressed(e);
						elem.actionPerformed(new ButtonEvent(e));
						break;
					case MouseEvent.MOUSE_RELEASED:
						elem.mouseReleased(e);
						break;
					case MouseEvent.MOUSE_ENTERED:
						elem.mouseEntered(e);
						break;
					case MouseEvent.MOUSE_EXITED:
						elem.mouseExited(e);
						break;
					case MouseEvent.MOUSE_DRAGGED:
						elem.mouseDragged(e);
						break;
					case MouseEvent.MOUSE_MOVED:
						elem.mouseMoved(e);
						break;
					default:
						elem.actionPerformed(new ButtonEvent(e));
					}
				}
			}
			if (onlcomp != null) ExpCoordinator.print(new String("ONLGraphic.TopButtonListener.fireEvent event:"  + nm + " component:" + onlcomp.getLabel()), TEST_BUTTON);
			else
				ExpCoordinator.print(new String("ONLGraphic.TopButtonListener.fireEvent event:"  + nm + " component:null"), TEST_BUTTON);
			//System.out.println("PortButtonListener::fireEvent #of listeners " + max);
		}  
	}

	public ONLGraphic()
	{ 
		super();
	}
	public ONLGraphic(ONLComponent c) 
	{ 
		super();
		setONLComponent(c);
	}
	public ONLComponent getONLComponent() { return onlComponent;}
	public void setONLComponent(ONLComponent c) 
	{ 
		onlComponent = c;
		c.addPropertyListener(ONLComponent.STATE, this);
		c.addPropertyListener(ONLComponent.SELECTED, this);
		c.addPropertyListener(ExperimentXML.USER_LABEL, this);
		/*if (activateListener == null)
	{
	  activateListener = new ActivateListener(this);
	  addMouseListener(activateListener);
	}*/
	}
	public void addDragListener(ONLGraphic.ButtonListener dListener)
	{
		addMouseListener(dListener);
		addMouseMotionListener(dListener);
	}
	public void removeDragListener(ONLGraphic.ButtonListener dListener)
	{
		removeMouseListener(dListener);
		removeMouseMotionListener(dListener);
	}
	public void repaint()
	{
		super.repaint();
		repaint(getX(), getY(), getWidth(), getHeight());
	}
	public Point2D getLinkPoint() 
	{
		Point p = new Point(getX(), getY());
		return p;
	}
	public int getScreenX() { return (getX());}
	public int getScreenY() { return (getY());}
	public void setBackground(Color c)
	{
		super.setBackground(c);
		convertedBG = convertColor(c);
	}
	public Color getConvertedBG() { return convertedBG;}
	protected Color convertColor(Color color)
	{
		//System.out.println("ONLGraphic::convertColor onlComponent isActive " + onlComponent.isActive());
		if (onlComponent == null || color == null || onlComponent.isActive() || onlComponent.getState().equals(ONLComponent.INBOTH) || onlComponent.getState().equals(ONLComponent.INCONSISTENT))
		{
			//System.out.println("                         returning opaque");
			if (onlComponent != null && onlComponent.isSelected()) return (color.darker());
			return color;
		}
		else
		{
			Color tmp_color;
			if (onlComponent.isFailed() || onlComponent.getState().equals(ONLComponent.IN2))
			{
				//System.out.println("                         returning gray scale");
				if (!color.equals(Color.white))
				{
					ColorSpace tmp_colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
					tmp_color = new Color(tmp_colorSpace, tmp_colorSpace.fromRGB(color.getRGBColorComponents(new float[3])), 1);
				}
				else tmp_color = Color.lightGray;
			}
			else //WAITING try it transparent
			{      
				if (onlComponent.getState().equals(ONLComponent.IN1))
				{                 
					if (!color.equals(Color.white))
					{
						ColorSpace tmp_colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
						tmp_color = new Color(tmp_colorSpace, tmp_colorSpace.fromRGB(color.getRGBColorComponents(new float[3])), 1);
					}
					else tmp_color = Color.lightGray;
					int r = tmp_color.getRed() + 50;
					if (r > 255) r = 255;
					if (r <0)   r = 0  ;
					int b = tmp_color.getBlue();
					int g = tmp_color.getGreen();
					tmp_color = new Color(r,g,b);
				}
				else
				{
					if (color.equals(Color.blue)) tmp_color = new Color(200,200,255);
					else
					{
						if (!color.equals(Color.white))
						{
							//System.out.println("                         returning transparent");
							//tmp_color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 75);
							int r = color.getRed() + 20;
							if (r > 255) r = 255;
							if (r <0)   r = 0  ;
							int b = color.getBlue() + 20;
							if (b > 255)  b = 255;
							if (b <0)   b = 0  ;
							int g = color.getGreen() + 20;
							if (g > 255)  g = 255;
							if (g <0) g = 0  ;
							tmp_color = new Color(r,g,b);
							//tmp_color = color.darker();

						}
						else tmp_color = new Color(230,230,230);
					}
				}

			}
			if (onlComponent.isSelected()) tmp_color = tmp_color.darker();
			return tmp_color;
		}
	}
	protected void setStateChange(String st) {} //override to disable clicks and change color
	//PropertyChangeListener
	public void propertyChange(PropertyChangeEvent e)
	{
		if (e.getSource() == onlComponent)
		{
			ExpCoordinator.print(new String("ONLGraphic(" + onlComponent.getLabel() + ").propertyChange " + e.getPropertyName() + " to " + ((String)e.getNewValue()) + " from " + ((String)e.getOldValue())), TEST_PROPERTY_CHANGE);		
			if (e.getPropertyName().equals(ONLComponent.STATE))
			{
				setStateChange((String)e.getNewValue());
			}
			setBackground(getBackground());
			revalidate();
			repaint();
		}
	}
	//end PropertyChangeListener
	//protected ActivateListener getActivateListener() { return activateListener;}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.POSITION);
		xmlWrtr.writeAttribute(ExperimentXML.XCOORD, String.valueOf(getX()));
		xmlWrtr.writeAttribute(ExperimentXML.YCOORD, String.valueOf(getY()));
		xmlWrtr.writeEndElement();//end position
		ExpCoordinator.print("ONLGraphic.writeXML");
	}
	public void setSpinnerPosition(int spinner) {}
	public void setUserLabel(String s){}
	public void setIconColor(Color c){}
}
