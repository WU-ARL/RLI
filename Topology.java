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
 * File: Topology.java
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
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.lang.String;
import java.lang.reflect.Array;
import java.util.Vector;
import java.io.*;
import java.awt.*;

import javax.xml.stream.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

//handles reading in initial topology from file or NCCP message
//holds lists of nodes and links
public class Topology
{
	public final static int TEST_DEFRTS = 1;
	private final int MAX_HOSTS_PER_CLUSTER = 9;
	private ExpCoordinator expCoordinator = null;
	private ONLComponentList nodes = null; //includes all hardware
	private ONLComponentList links = null; //to be added later
	private Vector clusters = null;
	private LinkTool linktool = null;
	private LinkToolAction linktoolAction = null;
	private int hardware_count = 0;
	private int host_count = 0;

	private boolean baseTopology = false;

	private class VisitedSubnet
	{
		SubnetManager.Subnet subnet = null;
		int numHops = 0;
		ONL.IPAddress nhip = null;
		ONLComponent.PortBase rport = null;
		public VisitedSubnet(SubnetManager.Subnet sn, ONL.IPAddress nhi, int nh, ONLComponent.PortBase rp) 
		{ 
			subnet = sn; 
			if (nhi == null) nhip = new ONL.IPAddress();
			else nhip = nhi; 
			numHops = nh;
			rport = rp;
		}
		public String toString()
		{
			return (new String("VisitedSubnet  " + subnet.getBaseIP() + "/" + subnet.getNetmask() + " nhip:" + nhip.toString() + " nh port:" + rport.getID() + " numHops:" + numHops));
		}
	}
	private class VisitedSubnets extends Vector<VisitedSubnet>
	{
		public VisitedSubnets() { super();}
		public boolean contains(SubnetManager.Subnet s)
		{
			return (getVisitedSubnet(s) != null);
		}
		public VisitedSubnet getVisitedSubnet(SubnetManager.Subnet s)
		{
			int max = size();
			VisitedSubnet elem;
			for (int i = 0; i < max; ++i)
			{
				elem = elementAt(i);
				if (elem.subnet == s) return elem;
			}
			return null;
		}
	}
	private class ReachableSubnets
	{
		private ONLComponent.PortBase rootPort = null;
		private Visited componentsVisited = null;
		private VisitedSubnets subnetsReached = null;

		private class VisitedPoint 
		{
			ONL.IPAddress nhip = null;
			ONLComponent.PortBase port = null;
			int numHops = 0;

			public VisitedPoint(ONL.IPAddress ni, ONLComponent.PortBase p) { nhip = ni; port = p;}
			public VisitedPoint(ONL.IPAddress ni, ONLComponent.PortBase p, int nh) { nhip = ni; port = p; numHops = nh;}
		}

		private class Visited extends Vector<VisitedPoint>
		{
			public Visited() { super();}
			public boolean contains(ONLComponent.PortBase p)
			{
				return (getPoint(p) != null);
			}
			public VisitedPoint getPoint(ONLComponent.PortBase p)
			{
				int max = size();
				VisitedPoint elem;
				for (int i = 0; i < max; ++i)
				{
					elem = elementAt(i);
					if (elem.port == p) return elem;
				}
				return null;
			}
		}

		public ReachableSubnets(ONLComponent.PortBase rtp)
		{
			rootPort = rtp;
			componentsVisited = new Visited();
			subnetsReached = new VisitedSubnets();
		}

		//public void addRoutes()
		public void findSubnets()
		{    
			ExpCoordinator.print(new String("Topology.ReachableSubnets.addRoutes root:" + rootPort.getLabel()), TEST_DEFRTS);
			//clear any old state

			componentsVisited.clear();
			subnetsReached.clear();

			Visited lastReachable = new Visited();
			VisitedPoint vp = new VisitedPoint(null, rootPort);
			componentsVisited.add(vp);
			lastReachable.add(vp);

			while (lastReachable.size() > 0) 
			{
				lastReachable = findReachableSubnets(lastReachable);
			}
			ExpCoordinator.print("Topology.ReachableSubnets.addRoutes finished", TEST_DEFRTS);
		}
		private Visited findReachableSubnets(Visited lastVisited)
		{
			int max = links.size();
			LinkDescriptor elem;
			Visited newSeen = new Visited();
			VisitedPoint connectedTo = null;  
			int portid = rootPort.getID();
			ExpCoordinator.print(new String("Topology.ReachableSubnets.findReachableSubnets for port " + rootPort.getLabel() + " lastVisited.size() = " + lastVisited.size()), TEST_DEFRTS);
			for (int i = 0; i < max; ++i) //iterate through all links
			{
				connectedTo = null;
				elem = (LinkDescriptor)links.elementAt(i);
				ExpCoordinator.print(new String("   link[" + i + "]: (" + elem.getPoint1().getLabel() + ", " + elem.getPoint2().getLabel() + ")"), TEST_DEFRTS);
				//if the list of lastVisited ports contains one of the endpoints look at the other end(connectedTo)
				VisitedPoint tmp_pnt = lastVisited.getPoint((ONLComponent.PortBase)elem.getPoint1());
				if (tmp_pnt != null)
					connectedTo = new VisitedPoint(tmp_pnt.nhip, (ONLComponent.PortBase)elem.getPoint2(), (tmp_pnt.numHops + 1));
				else 
				{
					tmp_pnt = lastVisited.getPoint((ONLComponent.PortBase)elem.getPoint2());
					if (tmp_pnt != null)
						connectedTo = new VisitedPoint(tmp_pnt.nhip, (ONLComponent.PortBase)elem.getPoint1(), (tmp_pnt.numHops + 1));
				}

				//process connectedTo if we haven't seen it before
				if (connectedTo != null && !componentsVisited.contains(connectedTo.port))
				{
					//if the next hop isn't set and it's a router port set the next hop
					if ((connectedTo.nhip == null || connectedTo.nhip.isZero()) && connectedTo.port.isRouter()) 
					{
						connectedTo.nhip = connectedTo.port.getIPAddr();
						ExpCoordinator.print(new String("Topology.ReachableSubnets.findReachableSubnets setting nhip connectedTo:" + connectedTo.port.getLabel() + " nhip:" + connectedTo.nhip), TEST_DEFRTS);
					}
					else
						ExpCoordinator.print(new String("Topology.ReachableSubnets.findReachableSubnets nhip stays the same connectedTo:" + connectedTo.port.getLabel() + " nhip:" + connectedTo.nhip), TEST_DEFRTS);

					componentsVisited.add(connectedTo);
					newSeen.add(connectedTo);
					SubnetManager.Subnet sn = connectedTo.port.getSubnet();
					//if we haven't seen this subnet before, add it to the reached list
					// if connectedTo is a Router port we need to supply a valid nhip and add look at the other router ports	
					if (connectedTo.port.isRouter())
					{
						if (sn != null && !subnetsReached.contains(sn)) addSubnet(sn, connectedTo);
						Hardware tmp_rtr = (Hardware)connectedTo.port.getParent();
						Hardware.Port tmp_port;
						SubnetManager.Subnet tmp_sn;
						int num_ports = tmp_rtr.getNumPorts();
						//look at the rest of the router ports
						for (int p = 0; p < num_ports; ++p)
						{
							tmp_port = tmp_rtr.getPort(p);
							//if we haven't seen this port before add it to the list and add a route to the subnet
							if (!componentsVisited.contains(tmp_port))
							{
								VisitedPoint vp = new VisitedPoint(connectedTo.nhip, tmp_port, (connectedTo.numHops + 1));
								newSeen.add(vp);
								componentsVisited.add(vp);
								tmp_sn = tmp_port.getSubnet();
								addSubnet(tmp_sn, vp);
							}
						}
					}
					else if (connectedTo.port.isSwitch()) 
						//if it's a switch add a route for the subnet and add the rest of the ports to the seen list. need to handle special class GigE separately
					{
						if (sn != null && !subnetsReached.contains(sn)) addSubnet(sn, connectedTo);
						if (connectedTo.port instanceof Hardware.Port)
						{
							Hardware tmp_sw = (Hardware)connectedTo.port.getParent();
							Hardware.Port tmp_port;
							int num_ports = tmp_sw.getNumPorts();
							//look at the rest of the router ports
							for (int p = 0; p < num_ports; ++p)
							{
								tmp_port = tmp_sw.getPort(p);
								//if we haven't seen this port before add it to the list and add a route to the subnet
								if (!componentsVisited.contains(tmp_port))
								{
									VisitedPoint vp = new VisitedPoint(connectedTo.nhip, tmp_port, (connectedTo.numHops + 1));
									newSeen.add(vp);
									componentsVisited.add(vp);
								}
							}
						}
						else if (connectedTo.port instanceof GigEDescriptor.Port)
						{
							GigEDescriptor tmp_sw = (GigEDescriptor)connectedTo.port.getParent();
							GigEDescriptor.Port tmp_port;
							int num_ports = tmp_sw.getNumPorts();
							//look at the rest of the router ports
							for (int p = 0; p < num_ports; ++p)
							{
								tmp_port = tmp_sw.getPort(p);
								//if we haven't seen this port before add it to the list and add a route to the subnet
								if (!componentsVisited.contains(tmp_port))
								{
									VisitedPoint vp = new VisitedPoint(connectedTo.nhip, tmp_port, (connectedTo.numHops + 1));
									newSeen.add(vp);
									componentsVisited.add(vp);
								}
							}
						}
					}
					else if (sn != null && !subnetsReached.contains(sn)) //it's an endpoint so just add the subnet route
						addSubnet(sn, connectedTo);
				}
			}
			return newSeen;
		}
		private void addSubnet(SubnetManager.Subnet subnet, VisitedPoint vp)//ONL.IPAddress nh)
		{
			SubnetManager.Subnet sn = subnet;
			if (ExpCoordinator.isOldSubnet() && subnet != null)
			{
				sn = ((OldSubnetManager.Subnet)subnet).getRootSubnet();
				if (sn.isEqual(((OldSubnetManager.Subnet)rootPort.getSubnet()).getRootSubnet()))  sn = subnet;
			}
			if	(sn != null && !subnetsReached.contains(sn))
			{
				VisitedSubnet vsn = new VisitedSubnet(sn, vp.nhip, vp.numHops, rootPort);
				subnetsReached.add(vsn);
				ExpCoordinator.print(new String("Topology.ReachableSubnets.addSubnet  " + vsn.toString() + " nh port:" + rootPort.getID()), TEST_DEFRTS);	
			}
		}
		public VisitedSubnets getSubnetsReached() { return subnetsReached;}
	}//end class ReachableSubnets


	public static abstract class TopologyAction extends ONL.UserAction implements PropertyChangeListener
	{
		public TopologyAction(String ttl)
		{
			super(ttl, false, false);
			ExpCoordinator.theCoordinator.addPropertyListener(ExpCoordinator.TOPO_SET, this);
		}

		public void propertyChange(PropertyChangeEvent e)
		{
			if (e.getPropertyName().equals(ExpCoordinator.TOPO_SET))
				setEnabled(!ExpCoordinator.isTopoSet());
		}
	}

	public static class DefaultRtsAction extends ONL.UserAction
	{
		protected Topology topology = null;
		public DefaultRtsAction(Topology ec)
		{
			super("Generate Default Routes", false, false);
			topology = ec;
			//setEnabled(false);
		}
		public void actionPerformed(ActionEvent e)
		{		
			if (!ExpCoordinator.isOldSubnet())
			{
				//super.actionPerformed(e);
				final String opt0 = "OK";
				final String opt1 = "Cancel";
				Object[] options = {opt0,opt1};
				int rtn = JOptionPane.showOptionDialog(ExpCoordinator.theCoordinator.getMainWindow(), 
						"Previously generated routes will be removed first.", 
						"Generate Default Routes", 
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, 
						null,
						options,
						options[0]);

				if (rtn == JOptionPane.YES_OPTION)
				{
					//super.actionPerformed(e);
					topology.generateDefaultRts();
					if (ExpCoordinator.isRecording())
						ExpCoordinator.recordEvent(SessionRecorder.GENERATE_DEFRTS, null);
				}
			}
			else
			{
				//topology.oldGenerateDefaultRts();
				topology.generateDefaultRts();
			}
		}
	}
	public static class DefaultHostRtAction extends TopologyAction
	{
		protected Topology topology = null;
		private static boolean on = true;
		public DefaultHostRtAction(Topology ec)
		{
			super("Deactivate Default Host Route");
			topology = ec;
			//setEnabled(false);
		}
		public void actionPerformed(ActionEvent e)
		{
			if (on) 
			{
				on = false;
				putValue(Action.NAME, "Activate Default Host Route");
			}
			else
			{
				on = true;
				putValue(Action.NAME, "Deactivate Default Host Route");
			}
		}
		public static boolean isOn() { return on;}
		public static void setOn(boolean b) { on = b;}
	}

	public static class ClearRtsAction extends ONL.UserAction
	{
		protected Topology topology = null;
		public ClearRtsAction(Topology ec)
		{
			super("Clear Route Tables", false, false);
			topology = ec;
			//setEnabled(false);
		}
		public void actionPerformed(ActionEvent e)
		{		
			final String opt0 = "OK";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			int rtn = JOptionPane.showOptionDialog(ExpCoordinator.theCoordinator.getMainWindow(), 
					"All routes will be removed. Do you want to do this?", 
					"Clear Route Tables", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				//super.actionPerformed(e);
				topology.clearRoutes();
				//if (ExpCoordinator.isRecording())
				//ExpCoordinator.recordEvent(SessionRecorder.GENERATE_DEFRTS, null);
			}
		}
	}
	public static class AddLinkAction extends TopologyAction
	{
		protected Topology topology = null;
		public AddLinkAction(Topology ec)
		{
			super("Add Link");
			topology = ec;
			//setEnabled(false);
		}
		public void actionPerformed(ActionEvent e)
		{
			//super.actionPerformed(e);
			topology.linktool.setEnabled(true);
		}
	}

	public static class LinkToolAction extends TopologyAction
	{
		private Topology topology = null;
		private boolean on = false;
		public LinkToolAction(Topology ec)
		{
			super("Links Off");
			topology = ec;
			//setEnabled(false);
		}
		public void actionPerformed(ActionEvent e)
		{
			//super.actionPerformed(e);
			setOn(!on);
		}
		public void setOn(boolean b)
		{
			if (b != on)
			{
				if (b) putValue(Action.NAME, "Links On");
				else putValue(Action.NAME, "Links Off");
				on = b;
				topology.linktool.setOverride(on);
			}
		}
		public void propertyChange(PropertyChangeEvent e)
		{
			if (e.getPropertyName().equals(ExpCoordinator.TOPO_SET) && on)
			{
				setOn(false);
			}
			super.propertyChange(e);
		}
	}
	public static class AddHWTypeAction extends MenuFileAction implements MenuFileAction.Saveable, PropertyChangeListener
	{
		private HardwareSpec.Manager hwManager = null;
		public AddHWTypeAction()
		{
			super(null, true, null, "Add New Hardware Type", false, false);
			hwManager = HardwareSpec.getTheManager();
			setSaveable(this);
			ExpCoordinator.theCoordinator.addPropertyListener(ExpCoordinator.TOPO_SET, this);
		}
		public void actionPerformed(ActionEvent e)
		{
			ExpCoordinator.print("AddHWTypeAction::actionPerformed", 2);
			super.actionPerformed(e);
		}  

		public void propertyChange(PropertyChangeEvent e)
		{
			if (e.getPropertyName().equals(ExpCoordinator.TOPO_SET))
				setEnabled(!ExpCoordinator.isTopoSet());
		}

		public void loadFromFile(java.io.File f)
		{
			if (f.getName().endsWith(HardwareSpec.Subtype.FILE_SUFFIX))
				hwManager.addSubtype(f);
			else
				hwManager.addFromFile("", f);
		}
		public void saveToFile(java.io.File f){}
	}

	public static class RemoveComponentAction extends AbstractAction
	{
		protected Topology topology = null;
		protected ONLComponent onlComponent = null;
		public RemoveComponentAction(Topology ec, ONLComponent c, String lbl)
		{
			super(lbl);
			topology = ec;
			onlComponent = c;
		}
		public RemoveComponentAction(Topology ec, ONLComponent c)
		{
			this(ec, c, c.getLabel());
		}
		public void actionPerformed(ActionEvent e)
		{
			topology.removeComponent(onlComponent);
		}
	}

	public Topology(ExpCoordinator ec)
	{
		this(ec, new LinkTool(ec));
	}
	public Topology(ExpCoordinator ec, LinkTool lt)
	{
		nodes = new ONLComponentList();//ONLComponent.NODE);
		links = new ONLComponentList();//ONLComponent.LINK);
		links.setLinks(true);
		expCoordinator = ec;
		linktool = lt;//new LinkTool(ec);
		clusters = new Vector();
	}
	public void addTopologyListener(ListDataListener dl)
	{
		nodes.addListDataListener(dl);
		links.addListDataListener(dl);
	}
	public void removeTopologyListener(ListDataListener dl)
	{
		nodes.removeListDataListener(dl);
		links.removeListDataListener(dl);
	}
	public void removeDefaultRts()
	{
		int max = nodes.size();
		//System.out.println("Topology.generateDefaultRts");
		ONLComponent c;
		for (int i = 0; i < max; ++i)
		{
			c = nodes.onlComponentAt(i);
			//System.out.println(c.getLabel());
			if (c instanceof Hardware) 
				((Hardware)c).removeGeneratedRoutes();
		}
	}
	public void generateDefaultRts()
	{
		int max = nodes.size();
		//System.out.println("Topology.generateDefaultRts");
		ONLComponent c;
		int i;

		for (i = 0; i < max; ++i)
		{
			c = nodes.onlComponentAt(i);
			//System.out.println(c.getLabel());
			if (c instanceof Hardware) 
			{
				((Hardware)c).removeGeneratedRoutes();
				//if (c.isRouter()) ((Hardware)c).generateDefaultRts();
				int num_ports = ((Hardware)c).getNumPorts();
				VisitedSubnets subnetsUnreached = null;//this is the list of Subnets that and NSP can't reach that others can
				ExpCoordinator.print(new String("Topology.generateDefaultRts component(" + c.getLabel() + ") nports=" + num_ports), TEST_DEFRTS);
				VisitedSubnets subnets = new VisitedSubnets();
				for (int j = 0; j < num_ports; ++j)
				{
					Hardware.Port p = ((Hardware)c).getPort(j);
					if (p.getSubnet() != null)
					{
						ReachableSubnets rs = new ReachableSubnets(p);
						rs.findSubnets();
						VisitedSubnets sreached = rs.getSubnetsReached();
						for (VisitedSubnet elem : sreached)
						{
							if (!subnets.contains(elem.subnet)) 
							{
								subnets.add(elem);
							}
							else
							{
								VisitedSubnet e2 = subnets.getVisitedSubnet(elem.subnet);
								if (e2.numHops > elem.numHops) 
								{
									subnets.remove(e2);
									subnets.add(elem);
								}
							}
						}
					}
				}
				for (VisitedSubnet elem : subnets)
				{
					addSubnetRoute(elem);
				}
			}
		}
	}
	private void addSubnetRoute(VisitedSubnet subnet)
	{
		SubnetManager.Subnet sn = subnet.subnet;
		ONLComponent.PortBase port = subnet.rport;
		if	(sn != null && sn.hasEndpoint())
		{
			int portid = port.getID();
			ONL.IPAddress nhip = subnet.nhip;
			ExpCoordinator.print(new String("Topology.addSubnetRoute for  " + subnet.toString()), TEST_DEFRTS);

			if (port.isRouter() && (port.getParent() instanceof Hardware))
			    ((Hardware)port.getParent()).addGeneratedRoute(sn.getBaseIP().toString(), sn.getNetmask().getBitlen(), portid, nhip);
			else 
			    {
				if (port instanceof Hardware.Port)
				    ((Hardware.Port)port).addGeneratedRoute(sn.getBaseIP().toString(), sn.getNetmask().getBitlen(), portid, nhip);
			    }
		}
	}
	public void clearRoutes()
	{
		int max = nodes.size();
		//System.out.println("Topology.generateDefaultRts");
		ONLComponent c;
		int i;

		for (i = 0; i < max; ++i)
		{
			c = nodes.onlComponentAt(i);
			//System.out.println(c.getLabel());
			if (c instanceof Hardware) 
				((Hardware)c).clearRoutes();
		}
	}

	public boolean containsONLComponent(String lbl, String tp)
	{
		ONLComponent c = getONLComponent(lbl, tp);
		return (c != null);
	}
	public ONLComponent getONLComponent(String lbl, String tp)
	{
		ONLComponentList cl = getONLComponentList(tp);
		if (cl != null)
		{
			ExpCoordinator.print(new String("Topology.getONLComponent got component list for " + lbl), 6);
			return (cl.getONLComponent(lbl, tp));
		}
		else return null;
	}
	public ONLComponent getONLComponent(ONLComponent c)
	{
		ONLComponentList cl = getONLComponentList(c.getType());
		if (cl != null) return (cl.getONLComponent(c));
		else return null;
	}
	protected ONLComponent addComponent(ONLComponent sd) //called if new component is added through gui
	{
		// System.out.println("Topology::addNode " + sd.toString());
		//if (sd.isLink()) System.out.println("  adding link");
		//else System.out.println("  adding node");
		boolean added = false;
		ONLComponentList cl = getONLComponentList(sd.getType());
		//System.out.println("Topology::addComponentToList " + sd.toString());
		ONLComponent c = cl.getONLComponent(sd);
		if (c != null) return c;
		if (sd instanceof LinkDescriptor) 
		{
			LinkDescriptor lnk = (LinkDescriptor)sd;
			if (lnk.getPoint1() != null && lnk.getPoint2() != null)
			{
				lnk.getPoint1().addLink(lnk);
				lnk.getPoint2().addLink(lnk);
			}
			//System.out.println("  adding link");
		}
		else 
		{
			//System.out.println("  adding node");
			if (sd instanceof Hardware) ++hardware_count;
			//if (sd.getType() == ONLComponent.HOST) ++host_count;
		}

		if (cl != null) 
		{
			sd.setRemoveAction(new RemoveComponentAction(this, sd));
			int old_ref = sd.getReference();
			sd.setProperty(ONLComponent.REF_NUM, getNextRefNum(sd.getReference()));
			ExpCoordinator.print(new String("Topology.addComponent " + sd.getLabel() + " setting ref from " + old_ref + " to " + sd.getReference()), 6);
			added = cl.addComponent(sd);
			if (added) return sd;
		}
		return null;
	}

	public boolean removeComponent(ONLComponent c)
	{
		System.out.println("Topology::removeComponent " + c.toString());
		ONLComponentList cl = getONLComponentList(c.getType()); 
		if (cl != null && cl.contains(c))
		{
			boolean rtn = cl.removeComponent(c); //CHANGE 11_3_2011
			if (c instanceof LinkDescriptor) 
			{
				LinkDescriptor lnk = (LinkDescriptor)c;
				if (lnk.getPoint1() != null && lnk.getPoint2() != null)
				{
					lnk.getPoint1().removeLink(lnk);
					lnk.getPoint2().removeLink(lnk);

					if (ExpCoordinator.isOldSubnet()) OldSubnetManager.removeLink((ONLComponent.PortBase)lnk.getPoint1(), (ONLComponent.PortBase)lnk.getPoint2());
					else SubnetManager.removeLink((ONLComponent.PortBase)lnk.getPoint1(), (ONLComponent.PortBase)lnk.getPoint2());
				}
			}
			//if (cl != null) 
			//{
			return rtn; //(cl.removeComponent(c));  //CHANGE 11_3_2011
			//}
		}
		return false;
	}

	private ONLComponentList getONLComponentList(String tp)
	{
		ONLComponentList rtn_cl = null;
		//System.out.println("Topology::getONLComponentList type:" + tp + " " + ONLComponent.getTypeString(tp));
		if (tp.equals(ONLComponent.LINK_LBL)) return links;
		else return nodes;
	}
	public LinkTool getLinkTool() { return linktool;}
	public void addLink(LinkDescriptor ld) { addComponent(ld);}
	public void removeLink(LinkDescriptor ld) { removeComponent(ld);}

	public boolean isLinked(ONLComponent onlc)
	{
		int max = links.size();
		LinkDescriptor elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (LinkDescriptor)links.elementAt(i);
			if (elem.getPoint1() == onlc || elem.getPoint2() == onlc)
				return true;
		}
		return false;
	}

	public boolean isWaiting()
	{
		int max_nodes = nodes.size();
		int i = 0;
		ONLComponent elem;
		for (i =0; i < max_nodes; ++i)
		{
			elem = nodes.onlComponentAt(i);
			if (elem.isWaiting()) return true;
		}
		int max_lnks = links.size();
		for (i =0; i < max_lnks; ++i)
		{
			elem = links.onlComponentAt(i);
			if (elem.isWaiting()) return true;
		}
		return false;
	}
	public Vector getAttachedComponents(ONLComponent c)
	{
		Vector rtn = new Vector();
		Cluster.Instance cluster = c.getCluster();
		int max_nodes = nodes.size();
		int i = 0;
		int j = 0;
		int max_lnks = links.size();
		LinkDescriptor lnk;
		if (cluster != null)
		{
			rtn.add(cluster);
			for (j = 0; j < max_lnks; ++j)
			{
				lnk = (LinkDescriptor)links.onlComponentAt(j);
				//link is attached if it's part of the cluster of if the component or one of it's ports is a link endpoint
				if (lnk.getCluster() == cluster || 
						lnk.getPoint1() == c || 
						lnk.getPoint2() == c || 
						lnk.getPoint1().getParent() == c || 
						lnk.getPoint2().getParent() == c )
					rtn.add(lnk);
			}
			ONLComponent elem;
			for (i = 0; i < max_nodes; ++i)
			{
				elem = nodes.onlComponentAt(i);
				if (elem.getCluster() == cluster) 
				{
					rtn.add(elem);
					for (j = 0; j < max_lnks; ++j)
					{
						lnk = (LinkDescriptor)links.onlComponentAt(j);
						//link is attached if it's part of the cluster of if the component or one of it's ports is a link endpoint
						if ((lnk.getPoint1() == elem || 
								lnk.getPoint2() == elem || 
								lnk.getPoint1().getParent() == elem || 
								lnk.getPoint2().getParent() == elem ) &&
								!rtn.contains(lnk))
							rtn.add(lnk);
					}
				}
			}
		}
		else 
		{
			rtn.add(c);
			for (j = 0; j < max_lnks; ++j)
			{
				lnk = (LinkDescriptor)links.onlComponentAt(j);
				//link is attached if it's part of the cluster of if the component or one of it's ports is a link endpoint
				if (lnk.getPoint1() == c || 
						lnk.getPoint2() == c || 
						lnk.getPoint1().getParent() == c || 
						lnk.getPoint2().getParent() == c )
					rtn.add(lnk);
			}
		}
		return rtn;
	}
	public void clear()
	{
		links.clear();
		nodes.clear();
		clusters.removeAllElements();
		host_count = 0;
		hardware_count = 0;
	}

	protected ONLComponentList getNodes() { return nodes;}
	protected Vector<Hardware> getHWNodes()
	{
		Vector<Hardware> rtn = new Vector<Hardware>();
		int max = nodes.size();
		for (int i = 0; i < max; ++i)
		{
			ONLComponent c = nodes.onlComponentAt(i);
			if (c instanceof Hardware) rtn.add((Hardware)c);
		}
		return rtn;
	}
	protected ONLComponentList getLinks() { return links;}
	protected Vector getClusters() { return clusters;}
	protected Cluster.Instance getCluster(int ref)
	{
		int max = clusters.size();
		Cluster.Instance ci;
		for (int i = 0; i < max; ++i)
		{
			ci = (Cluster.Instance)clusters.elementAt(i);
			if (ci.getIndex() == ref) return ci;
		}
		return null;
	} 

	protected int getNewRouterIndex()
	{
		int x = 1;
		while (getRouter(x) != null)
		{
			++x;
		}
		return x;
	}

	private Hardware getRouter(int ndx)
	{
		int max = nodes.size();
		ONLComponent elem;
		for (int i = 0; i < max; ++i)
		{
			elem = nodes.onlComponentAt(i);
			if ((elem instanceof Hardware) && (elem.getPropertyInt(Hardware.INDEX) == ndx)) return ((Hardware)elem);
		}
		return null;
	}
	public Hardware getNewHW(HardwareBaseSpec tp, String ctype)
	{
		String tmp_ctype = ctype;
		if (tmp_ctype == null) tmp_ctype = tp.getComponentType().replaceAll("\\s", "");
		String ctypes[] = tmp_ctype.split("\\|");
		if (tmp_ctype.equals(ExperimentXML.ANY))
		{
			ctypes = new String[3];
			ctypes[0] = ExperimentXML.ENDPOINT;
			ctypes[1] = ExperimentXML.SWITCH;
			ctypes[2] = ExperimentXML.ROUTER;
		}

		if (Array.getLength(ctypes) > 1)
		{
			JComboBox component_choice = new JComboBox(ctypes);
			Object[] options = {"Enter", "Cancel"};
			int rtn = JOptionPane.showOptionDialog(null,
					new ONL.ComponentwLabel(component_choice, "component type:"),
					new String("Add " + tp.getLabel()),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);
			if (rtn == JOptionPane.YES_OPTION)
			{
				tmp_ctype = (String)component_choice.getSelectedItem();
			}
			else return null;
		}
		if (tmp_ctype.equals(ExperimentXML.HOST)) tmp_ctype = ExperimentXML.ENDPOINT;
		int x = getNewRouterIndex();
		Hardware rtn = null;
		if (tp instanceof HardwareSpec) 
		{
			if (tp.isHost())
				rtn = new HardwareHost(x, expCoordinator, (HardwareSpec)tp);
			else
				rtn = new Hardware(x, expCoordinator, (HardwareSpec)tp);
		}
		else
		{
			if (tp instanceof HardwareSpec.Subtype) 
			{
				if (tp.isHost())
					rtn = new HardwareHost(x, expCoordinator, (HardwareSpec.Subtype)tp);
				else
					rtn = new Hardware(x, expCoordinator, (HardwareSpec.Subtype)tp);
			}
		}
		rtn.setComponentType(tmp_ctype);
		rtn.setBaseIPAddr(getValidIP(x));
		rtn.setProperty(Hardware.INDEX, x);
		return rtn;
	}

	public ONLComponent getNewHW(String hwlbl, String ctype)
	{
		if (hwlbl.equals(ONLComponent.VGIGE_LBL)) return (getNewVGigE());
		//get hardware type
		HardwareSpec.Subtype hwtp = HardwareSpec.getTheManager().getSubtype(hwlbl);
		if (hwtp != null) 
		{
			return (getNewHW(hwtp, ctype));
		}
		return null; 
	}

	public GigEDescriptor getNewVGigE()
	{
		//might just want to start from 0 each time and refuse if host_count == hardware_count * 7, when GigE added number should be hardware_count * 10
		int x = 0;
		String lbl = new String("gige" + x);
		while (containsONLComponent(lbl, ONLComponent.VGIGE_LBL))
		{
			++x;
			lbl = new String("gige" + x);
		}
		return (new GigEDescriptor(lbl, expCoordinator));
	}


	public String getNewLinklbl()
	{
		return (linktool.getNextLabel());
	}
	private String getValidIP(int x)
	{
		return (new String("192.168." + x + ".0"));
		//return (new String("192.168." + (x*32) + ".0"));
	}

	public int getNextRefNum() { return (getNextRefNum(0));}
	public int getNextRefNum(int r)
	{
		//change this to increase per hw and never reuse in experiment maybe this number is housed in experiment
		int max = nodes.size();
		Experiment exp = ExpCoordinator.theCoordinator.getCurrentExp();
		int rtn = r;
		if (rtn <= 0) rtn = exp.getNextRefNum();
		ONLComponent celem;
		int i = 0;
		for (i = 0; i < max; ++i)
		{
			celem = nodes.onlComponentAt(i);
			if (celem.getPropertyInt(ONLComponent.REF_NUM) == rtn) 
			{
				++rtn;
				i = 0;
			}
		}
		max = links.size();
		for (i = 0; i < max; ++i)
		{
			celem = links.onlComponentAt(i);
			if (celem.getPropertyInt(ONLComponent.REF_NUM) == rtn) 
			{
				++rtn;
				i = 0;
			}
		}
		max = clusters.size();
		Cluster.Instance ci;
		for (i = 0; i < max; ++i)
		{
			ci = (Cluster.Instance)clusters.elementAt(i);
			if (ci.getIndex() == rtn)
			{
				++rtn;
				i = 0;
			}
		}
		exp.setNextRefNum(rtn+1);
		return rtn;
	}

	public int getNumNodes() { return (nodes.getSize());}
	public int getNumLinks() { return (links.getSize());} 
	public int getNumComponents() { return (nodes.size() + links.size() + clusters.size());}
	public void addCluster(Cluster.Instance ci)
	{
		if (!clusters.contains(ci))
		{
			clusters.add(ci);
			ExpCoordinator.print(new String("Topology.addCluster ref=" + ci.getIndex()), 3);
		}
	}
	public void removeCluster(Cluster.Instance ci)
	{
		if (clusters.contains(ci)) clusters.remove(ci);
	}
	public void addNewCluster(Cluster.Instance ci)
	{
		if (!clusters.contains(ci))
		{
			ci.setIndex(getNextRefNum(ci.getIndex()));
			clusters.add(ci);
			ExpCoordinator.print(new String("Topology.addNewCluster ref=" + ci.getIndex() + " size = " + clusters.size()), 3);
		}
	}
	public void writeXMLHWTypes(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.HWTYPES);
		int max = nodes.getSize();
		for (int i = 0; i < max; ++i)
		{
			ONLComponent c = nodes.onlComponentAt(i);
			if (c instanceof Hardware)
			{
				((Hardware)c).getHWType().writeXML(xmlWrtr);
				((Hardware)c).getHWSubtype().writeXML(xmlWrtr);
			}
		}
		xmlWrtr.writeEndElement();//hwtypes
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		int max = clusters.size();
		ONLComponent c;
		int i;
		if (max > 0)
		{
			xmlWrtr.writeStartElement(ExperimentXML.CLUSTERS);
			Cluster.Instance ci;
			for (i = 0; i < max; ++i)
			{
				ci = (Cluster.Instance)clusters.elementAt(i);
				ci.writeXML(xmlWrtr);
			}
			xmlWrtr.writeEndElement();//clusters
		} 
		max = nodes.getSize();
		xmlWrtr.writeStartElement(ExperimentXML.NODES);
		for (i = 0; i < max; ++i)
		{
			xmlWrtr.writeStartElement(ExperimentXML.NODE);
			c = nodes.onlComponentAt(i);
			c.writeXML(xmlWrtr);
			xmlWrtr.writeEndElement();//node
		}
		xmlWrtr.writeEndElement();//nodes
		max = links.getSize();
		if (max > 0)
		{
			xmlWrtr.writeStartElement(ExperimentXML.LINKS);
			for (i = 0; i < max; ++i)
			{
				c = links.onlComponentAt(i);
				c.writeXML(xmlWrtr);
			}
			xmlWrtr.writeEndElement();//links
		}
	}

	public void print(int d) 
	{

		//write all clusters
		int max = clusters.size();
		int i;
		String cpaddr;
		ExpCoordinator ec = ExpCoordinator.theCoordinator;
		ec.print("\n\n\nTopology - ", d);
		ec.print(new String("   clusters:" + max), d);
		Cluster.Instance ci;
		int max2;
		int j;
		for (i = 0; i < max; ++i)
		{
			ci = (Cluster.Instance)clusters.elementAt(i);
			ec.print(new String("     cluster[" + i + "]:" + ci.getTypeLabel() + " " + ci.getIndex()), d);
		}                 

		max = nodes.getSize();
		ec.print(new String("   nodes:" + max), d);
		ONLComponent c;
		for (i = 0; i < max; ++i)
		{
			c = nodes.onlComponentAt(i);
			ec.print(new String("     node[" + i + "]:" + c.getComponentID()), d);
		}
		max = links.getSize();
		ec.print(new String("   links:" + max), d);
		for (i = 0; i < max; ++i)
		{
			c = links.onlComponentAt(i);
			ec.print(new String("     link[" + i + "]:" + c.getComponentID()), d);
		}
		ec.print("\n\n\n", d);
	}

	public boolean isCycle(ONLComponent c1, ONLComponent c2) //returns true if a link between c1 and c2 would cause a cycle
	{
		Vector seen = new Vector();
		//if (c1.getParent() != null && c1.getParent().isSwitch())
		if (c1.getParent() != null && c1.getParent() instanceof GigEDescriptor)
			seen.add(c1.getParent());
		else
			seen.add(c1);
		//if (c2.getParent() != null && c2.getParent().isSwitch())
		if (c2.getParent() != null && c2.getParent() instanceof GigEDescriptor)
			seen.add(c2.getParent());
		else
			seen.add(c2);

		ONLComponentList tmp_links = new ONLComponentList(links);
		Vector tmp_rm = new Vector();
		while(!tmp_links.isEmpty())
		{
			int max = tmp_links.size();
			LinkDescriptor lnk;
			ONLComponent c;
			for (int i = 0; i < max; ++i)
			{
				lnk = (LinkDescriptor)tmp_links.getElementAt(i);
				if ((seen.contains(lnk.getPoint1()) || (lnk.getPoint1().getParent() != null && seen.contains(lnk.getPoint1().getParent()))) && 
						(seen.contains(lnk.getPoint2()) || (lnk.getPoint2().getParent() != null && seen.contains(lnk.getPoint2().getParent())))) return true;
				c = lnk.getPoint1();
				if (seen.contains(c) || seen.contains(c.getParent())) 
				{
					if ((c == c1) || 
							(c == c2) || 
							(c.getParent() != null && c.getParent() instanceof GigEDescriptor))
						//(c.getParent() != null && c.getParent().isSwitch()))
					{
						c = lnk.getPoint2();
						if (c.getParent() instanceof GigEDescriptor)
							seen.add(c.getParent());
						else seen.add(c);
						tmp_rm.add(lnk);
					}
				}
				c = lnk.getPoint2();
				if (seen.contains(c) || seen.contains(c.getParent())) 
				{
					if ((c == c1) || 
							(c == c2) || 
							(c.getParent() != null && c.getParent() instanceof GigEDescriptor))
						//(c.getParent() != null && c.getParent().isSwitch()))
					{
						c = lnk.getPoint1();
						if (c.getParent() instanceof GigEDescriptor)
							seen.add(c.getParent());
						else seen.add(c);
						tmp_rm.add(lnk);
					}
				}
			}
			if (tmp_rm.isEmpty()) return false;
			tmp_links.removeAll(tmp_rm);
			tmp_rm.clear();
		}
		return false;
	}
	public LinkToolAction getLinkToolAction()
	{
		if (linktoolAction == null) linktoolAction = new LinkToolAction(this);
		return linktoolAction;
	}
}
