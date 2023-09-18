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
 * File: LinkTool.java
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
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
//import java.awt.geom.*;
import javax.swing.*;
import java.util.Vector;

public class LinkTool extends MouseInputAdapter //implements Mode.ToolComponent
{
	public static final int TEST_10G = 4;
	public static final int TEST_GIGE = 4;
	public static final int TEST_ADD = 4;

	private ComponentListener buttonAction = null;
	protected ExpCoordinator expCoordinator = null;
	private double currentBW = 0;
	private int count = 0;
	private boolean override = false;

	private class ComponentListener extends ONLGraphic.ButtonListener
	{
		private LinkTool ltool = null;
		private ONLComponent startComponent = null;
		private ONLComponent endComponent = null;
		private LinkGraphic linkGraphic = null;

		public ComponentListener(LinkTool lt)
		{
			super(null);
			//setTool(lt);
			ltool = lt;
			this.setEnabled(false);
		}
		public void mousePressed(MouseEvent e) 
		{
			if (ltool.isEnabled())
			{
				startComponent = ((ONLComponentButton)e.getSource()).getONLComponent();
				if (startComponent instanceof GigEDescriptor) 
				{
					ExpCoordinator.print(new String("LinkTool.ComponentListener.mousePressed gige startComponent:" + startComponent.getLabel()), TEST_GIGE);
					startComponent = ((GigEDescriptor)startComponent).getUnlinkedPort();
					if (startComponent != null)
						ExpCoordinator.print(new String("  unlinked port: " + startComponent.getLabel()), TEST_GIGE);
					else
						ExpCoordinator.print("  unlinked port: null", TEST_GIGE);
				}
				if (startComponent instanceof HardwareHost) startComponent = ((HardwareHost)startComponent).getPort(0);
				endComponent = null;
				if (startComponent.isLinkable() && (!startComponent.isLinked()))
				{
					LinkDescriptor ld = ltool.createLinkDescriptor(startComponent);//new LinkDescriptor(ltool.getNextLabel(), startComponent, ltool.currentBW, ltool.expCoordinator);
					linkGraphic = (LinkGraphic)ld.getGraphic();
					if (linkGraphic.getONLComponent() == null) ExpCoordinator.print("LinkTool.ComponentListener::mousePressed onlcomp == null", 2);
					//linkGraphic = new LinkGraphic();
					//linkGraphic.setPoint1(startComponent.getGraphic());
					linkGraphic.setVisible(true);
					ltool.getTopologyPanel().addLink(linkGraphic);//expCoordinator.getMainWindow().getTopologyPanel()
					//System.out.println("LinkTool.ComponentListener::mousePressed component " + startComponent.getLabel());
					setIntermediate(e.getPoint(), ((ONLComponentButton)e.getSource()).getONLComponent().getGraphic());
				}
				else 
				{
					ExpCoordinator.print(new String("LinkTool::mousePressed startComponent " + startComponent.getLabel() + " is Linked"), 2);
					startComponent = null;
					ltool.setEnabled(false);
				}
			}
		}
		public void mouseDragged(MouseEvent e)
		{
			if (ltool.isEnabled())
			{
				//System.out.println("LinkTool.ComponentListener::mouseDragged");
				//translate point to real coordinates since point is in relation to the source component
				setIntermediate(e.getPoint(), ((ONLComponentButton)e.getSource()).getONLComponent().getGraphic());
			}
		}
		public void mouseReleased(MouseEvent e) 
		{
			if (ltool.isEnabled())
			{
				if (endComponent instanceof GigEDescriptor) endComponent = ((GigEDescriptor)endComponent).getUnlinkedPort();
				if (!ltool.addLink(linkGraphic, endComponent))
				{
					//ExpCoordinator.print(new String("LinkTool.ComponentListener::mouseReleased from " + startComponent.getLabel() + " cancelled"), TEST_ADD);
				    ExpCoordinator.getMainWindow().getTopologyPanel().removeLink(linkGraphic);
				}
				startComponent = null;
				endComponent = null;
				linkGraphic = null;
				ltool.setEnabled(false);
			}
		}
		public void mouseEntered(MouseEvent e) 
		{
			if (ltool.isEnabled())
			{
				endComponent = ((ONLComponentButton)e.getSource()).getONLComponent();
				if (endComponent instanceof HardwareHost) endComponent = ((HardwareHost)endComponent).getPort(0);
				//System.out.println("LinkTool.ComponentListener::mouseEntered component " + endComponent.getLabel());
			}
		}
		public void mouseExited(MouseEvent e) 
		{
			if (ltool.isEnabled() && startComponent != null && endComponent != null)
			{
				if (endComponent == ((ONLComponentButton)e.getSource()).getONLComponent()) endComponent = null;
				//if (endComponent != null) System.out.println("LinkTool.ComponentListener::mouseEntered component " + endComponent.getLabel());
			}
		}
		public void setIntermediate(Point p) { setIntermediate(p, null);}
		public void setIntermediate(Point p, ONLGraphic c) 
		{
			if (ltool.isEnabled())
			{
				if (linkGraphic != null)
				{
					Point pnt = new Point(p);
					//System.out.println("LinkTool::setIntermediate (" + pnt.getX() + ", " + pnt.getY() + ")"); 
					if (c != null)
					{
						//System.out.println("                         translated by (" + c.getScreenX()+ ", " + c.getScreenY() + ")"); 
						pnt.translate((c.getScreenX() + HardwareGraphic.OFFSET), (c.getScreenY() + HardwareGraphic.D_OFFSET));
						//System.out.println("                          (" + pnt.getX() + ", " + pnt.getY() + ")"); 
					}
					linkGraphic.setIntermediate(pnt);
					linkGraphic.revalidate();
					linkGraphic.repaint();
				}
			}
		}
		public Object getTool() { return ltool;}
	}

	public LinkTool(ExpCoordinator ec)
	{
		expCoordinator = ec;
		buttonAction = new ComponentListener(this);
		currentBW = LinkDescriptor.DEFAULT_BANDWIDTH;
	}
	protected  boolean addLink(LinkGraphic lg, ONLComponent end_comp) //returns true if successfully added
	{
		if (lg == null) return false;
		LinkDescriptor comp = (LinkDescriptor)lg.getONLComponent();
		ONLComponent start_comp = comp.getPoint1();
		//fail link if end_comp is not a component, it's a loop, the end_comp is unlinkable or is already linked
		if (end_comp == null || 
				end_comp == start_comp ||
				!end_comp.isLinkable() || 
				end_comp.isLinked())
		{
			if (end_comp == null) ExpCoordinator.print(new String("LinkTool.addLink " + comp.toString() + " fail end is null"), TEST_ADD);
			else
			{
				if (end_comp == start_comp) ExpCoordinator.print(new String("LinkTool.addLink " + comp.toString() + " fail end(" + end_comp.getLabel() + ") is same as start"), TEST_ADD);
				if(!end_comp.isLinkable() || end_comp.isLinked()) ExpCoordinator.print(new String("LinkTool.addLink " + comp.toString() + " fail diend(" + end_comp.getLabel() + ") unlinkable isLinkable:" + end_comp.isLinkable() + " isLinked:" + end_comp.isLinked()), TEST_ADD);
			}
			return false;
		}
		else
		{
			//10G change for gige to gige connections remove GigE to GigE restriction
		
			
			//SUBNET:add call SubnetManager addLink here
			try {
			    SubnetManager.addLink((ONLComponent.PortBase)start_comp, (ONLComponent.PortBase)end_comp);
			}
			catch(SubnetManager.SubnetException e)
			{
				ExpCoordinator.print(new String("LinkTool.addLink failed (" + start_comp.getLabel() + ", " + end_comp.getLabel() + ") -- " + e.getMessage()));
				return false;
			}
		}
		comp.setPoint2(end_comp);
		addLink(lg);
		return true;
	} 

	protected  void addLink(LinkGraphic lg)
	{
		LinkDescriptor comp = (LinkDescriptor)lg.getONLComponent();
		ExpCoordinator.print(new String("LinkTool::addLink " + comp.getPoint1().getLabel() + " to " + comp.getPoint2().getLabel()));

		lg.getONLComponent().setLabel(new String("explink" + count));
		expCoordinator.getCurrentExp().addLink(comp);
		++count;
	}
	public void setCount(int c) { count = c;}
	public void incrementCount(int c) { count = count + c;}
	public void setCurrentBW(double bw) { currentBW = bw;}
	public double getCurrentBW() { return currentBW;}
	public void mouseDragged(MouseEvent e)
	{
		if (isEnabled())
		{
			//System.out.println("LinkTool::mouseDragged");
			if (e.getSource() instanceof ONLGraphic) buttonAction.setIntermediate(e.getPoint(), (ONLGraphic)e.getSource());
		} 
	}
	public void mousePressed(MouseEvent e) 
	{
		//System.out.println("LinkTool::mousePressed");
		if (isEnabled() && (e.getSource() instanceof ONLGraphic))
			buttonAction.setIntermediate(e.getPoint(), (ONLGraphic)e.getSource());
	} 
	public void setEnabled(boolean b) 
	{
		if (!isOverride() || b)
			buttonAction.setEnabled(b);
	}
	public void setOverride(boolean b) { override = b; setEnabled(b);}
	public boolean isOverride() { return override;}
	public boolean isEnabled() { return  buttonAction.isEnabled();}
	public ONLGraphic.ButtonListener getButtonAction() { return buttonAction;}
	public Object getTool() { return this;}
	public String getNextLabel() 
	{
		int cnt = count;
		++count;
		return (new String("ulink" + cnt));
	}
	protected LinkDescriptor createLinkDescriptor(ONLComponent startc)
	{
		LinkDescriptor ld = new LinkDescriptor(getNextLabel(), startc, currentBW, expCoordinator);
		return ld;	
	}
    protected int getNextCount() { return count++;}
    protected TopologyPanel getTopologyPanel() { return (expCoordinator.getMainWindow().getTopologyPanel());}
}
