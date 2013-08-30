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
 * File: NSPPortMonitor.java
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
import java.util.Vector;
import javax.swing.*;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

public class NSPPortMonitor implements ONLComponent.ONLMonitorable //extends WUGSPortMonitor
{
	private static final int TEST_ADD = 6;
	protected NSPPort nspPort = null;
	protected NSPMonitor nspMonitor = null;
	protected ButtonAction pbuttonAction = null;
	protected BaseNSPMonitor IPPMenu = null;
	protected BaseNSPMonitor OPPMenu = null;
	private Vector monitorActions = null;

	//////////////////////////////////////////// NSPPortMonitor.ButtonAction ///////////////////////////////////////////////
	protected class ButtonAction extends ONLGraphic.ButtonListener
	{
		private NSPPortMonitor portMonitor = null;
		public ButtonAction(NSPPortMonitor c)
		{
			super(c.getONLComponent());
			portMonitor = c;
		}
		public void mousePressed(MouseEvent e)
		{
			//NOTE: should eventually remove active check here and just allow user to queue monitored params that get sent once component is active
			if (((HardwareGraphic.HWButton)e.getSource()).isPortButton()) // && ((WUGSDescriptor)getONLComponent().getParent()).isActive())
			{
				//gonna have to check if IPP or OPP
				//get port button and call showMenu with button coordinates and the main jpanel
				HardwareGraphic.PortButton pbutton = (HardwareGraphic.PortButton)e.getSource();
				String ttl = new String("Port " + pbutton.getPort() + " parameters");
				JPopupMenu menu = new JPopupMenu(ttl);
				menu.setLabel(ttl);
				menu.add(portMonitor.IPPMenu);
				menu.add(portMonitor.OPPMenu);
				Point2D pnt = pbutton.getCenter();
				menu.show(pbutton, (int)pnt.getX(), (int)pnt.getY());
				menu.setVisible(true);
			}
		}
	}//end NSPPortMonitor.ButtonAction

	/////////////////////////////////////////////// NSPPortMonitor.BaseNSPMonitor ////////////////////////////////////////////////////
	protected static class BaseNSPMonitor  extends JMenu implements Monitor //extends WUGSPortMonitor.BaseMonitor
	{
		protected int id = 0;
		protected boolean ipp = true;
		protected Vector monitorableParams;
		private int status = 0;
		private MonitorDataType.PortStatus dataType = null;
		protected NSPPortMonitor portMonitor = null;
		private Vector pluginParams;

		public BaseNSPMonitor(NSPPortMonitor sd, boolean ip)
		{
			super();
			id = sd.nspPort.getID();

			//if (sd.getParent() != null) wugsMonitor = (WUGSMonitor)sd.getParent().getMonitorable();
			portMonitor = sd;

			ipp = ip;
			monitorableParams = new Vector();
			JMenuItem mitem;
			String lbl;
			if (ipp)
			{ 
				setText("Ingress");
			}
			else
			{ 
				setText("Egress");
			}
			dataType = new MonitorDataType.PortStatus(portMonitor.nspPort.getID(), ipp);
			dataType.setONLComponent(portMonitor.nspPort);
			if (ipp)
			{
				//addPortItem(new MonitorMenuItem.PortBandwidth(portMonitor.wugsPort, ipp));
				addPortItem(new MonitorMenuItem.PortBWWORecycle("Port Bandwidth", portMonitor.nspPort, ipp));
				addPortItem(new MonitorMenuItem.VCIBandwidth(portMonitor.nspPort));
				//addPortItem(new MonitorMenuItem.VPIBandwidth(portMonitor.wugsPort));
				addPortItem(new MonitorMenuItem.IPPDiscard(portMonitor.nspPort));
			}
			else 
			{
				addPortItem(new MonitorMenuItem.PortBWWORecycle("Port Bandwidth", portMonitor.nspPort, ipp));
				//addPortItem(new MonitorMenuItem.PortBandwidth(portMonitor.wugsPort, ipp));
				addPortItem(new MonitorMenuItem.OPPDiscard(portMonitor.nspPort));
			}
			setFont(new Font("Dialog", Font.BOLD, 10));
			JMenuItem tmp_item;
			//System.out.println("NSPPortMonitor.BaseNSPMonitor");
			if (ip)
			{
				addPortItem(new NSPMonClasses.MMenuItem_VOQlength("VOQLength", (NSPPort)portMonitor.nspPort));
				addPortItem(new NSPMonClasses.FPXCounterMenu((NSPPort)portMonitor.nspPort, ip));
				tmp_item = new JMenuItem(((NSPPort)portMonitor.nspPort).getAction(NSPPort.INFTACTION));
				tmp_item.setText("Filters");
				tmp_item.setFont(new Font("Dialog", Font.PLAIN, 9));
				add(tmp_item);
			}
			else
			{
				addPortItem(new NSPMonClasses.MMenuItem_Qlength("QLength", (NSPPort)portMonitor.nspPort));
				addPortItem(new NSPMonClasses.FPXCounterMenu((NSPPort)portMonitor.nspPort, ip));
				//addPortItem(new NSPMonClasses.MMenuItem_SubPortPC("FPX Port Packet Counter", (NSPPort)portMonitor.wugsPort));
				//addPortItem(new NSPMonClasses.MMenuItem_FPXCounter("FPX General Counter", (NSPPort)portMonitor.wugsPort));
				tmp_item = new JMenuItem(((NSPPort)portMonitor.nspPort).getAction(NSPPort.OUTFTACTION));
				tmp_item.setText("Filters");
				tmp_item.setFont(new Font("Dialog", Font.PLAIN, 9));
				add(tmp_item);
			}

			tmp_item = new JMenuItem(((NSPPort)portMonitor.nspPort).getAction(NSPPort.PINTACTION));
			tmp_item.setText("Plugin Instances");
			tmp_item.setFont(new Font("Dialog", Font.PLAIN, 9));
			add(tmp_item);
		}

		public void addPortItem(MonitorMenuItem.PortMenu p_mmi)
		{
			monitorableParams.add(p_mmi);
			add(p_mmi);
		}
		public void addPortItem(MonitorMenuItem.BaseItem p_mmi)
		{
			monitorableParams.add(p_mmi);
			add(p_mmi.getMenuItem());
		}
		public MonitorManager getMonitorManager() { return (portMonitor.nspMonitor.getMonitorManager());}
		public int getID() { return id;}
		public boolean isIPP() { return ipp;}
		public String getString()
		{
			if (ipp) return ("IPP" + String.valueOf(id));
			else return ("OPP" + String.valueOf(id));
		}


		protected MonitorAction getMMI(MonitorDataType.Base mdt)
		{
			int max = monitorableParams.size();
			MonitorAction elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (MonitorAction)monitorableParams.elementAt(i);
				if (elem.isDataType(mdt)) return ((MonitorAction)elem);
			}
			return null;
		}

		public boolean isError()
		{
			if (ipp) return ((status & NCCP.IPP_HARDWARE_PARITY) > 0);
			else return ((status & NCCP.OPP_HARDWARE_PARITY) > 0);
		}
		//Monitor Interface
		public void setData(double val, double timeInt)
		{
			status = (int)val;
		}
		public void setDataExact(double val, double timeInt)
		{
			status = (int)val;
		}
		public void stop(){}
		public void start(){}
		public boolean isStarted() { return true;}
		public void addMonitorable(Monitorable m){}
		public void removeMonitorable(Monitorable m){}
		public MonitorDataType.Base getDataType()
		{
			return dataType;
		}
		public void addMonitorFunction(MonitorFunction mf){}
		public void removeMonitorFunction(MonitorFunction mf){}
		public void setLogFile(java.io.File f){}
		//end Monitor Interface

		public int getStatus() { return status;}
		public ONLComponent getONLComponent() { return (portMonitor.nspPort);}
	}//end inner class BasePortMonitor


	////////////////////////////////////////////////// NSPPortMonitor methods ///////////////////////////////////////////////////
	public NSPPortMonitor(NSPMonitor m, NSPPort p)
	{
		nspMonitor = m;
		nspPort = p;
		pbuttonAction = new ButtonAction(this);
		monitorActions = new Vector();
	}
	public void initializeMenus()
	{
		IPPMenu = new BaseNSPMonitor(this, true);
		OPPMenu = new BaseNSPMonitor(this, false);
	}

	public Monitor addMonitor(Monitor m)
	{
		return (nspMonitor.addMonitor(m));
	}

	public void removeMonitor(Monitor m)
	{
		nspMonitor.removeMonitor(m);
	}

	public Monitor addMonitor(MonitorDataType.Base dt)
	{
		MonitorAction mmi = getMMI(dt);
		ExpCoordinator.print(new String("NSPPortMonitor.addMonitor  " + dt.getName() + " " + dt.getMonitorID()), TEST_ADD);
		if (mmi != null)
		{
			ExpCoordinator.print("     found mmi", TEST_ADD);
			return (mmi.addMonitor(dt));
		}
		else 
		{
			MonitorAction ma = getMonitorAction(dt);
			if (ma != null) return (ma.addMonitor(dt));
			else return null;
		}
	}
	public void addMonitorAction(MonitorAction ma)
	{
		if (!monitorActions.contains(ma)) monitorActions.add(ma);
	}
	public void removeMonitorAction(MonitorAction ma)
	{
		if (monitorActions.contains(ma)) monitorActions.remove(ma);
	}
	public MonitorAction getMonitorAction(MonitorDataType.Base dt)
	{
		int max = monitorActions.size();
		MonitorAction rtn;
		ExpCoordinator.print(new String("NSPPortMonitor.getMonitorAction mdt:" + dt.getName() + " numMonitorActions:" + max), TEST_ADD);
		for (int i = 0; i < max; ++i)
		{
			rtn = (MonitorAction)monitorActions.elementAt(i);
			if (rtn.isDataType(dt)) return rtn;
		}
		return null;
	}
	public MonitorAction getMMI(MonitorDataType.Base dt)
	{
		if (dt.isIPP()) return (IPPMenu.getMMI(dt));
		else return (OPPMenu.getMMI(dt));
	}
	public ONLGraphic.ButtonListener getPButtonAction() { return pbuttonAction;}
	public MonitorManager getMonitorManager() { return (nspMonitor.getMonitorManager());}
	public ONLComponent getONLComponent() { return nspPort;}
	public void clear() {}
	public void addPortMenus(ONLComponent.CfgMonMenu mn) 
	{
		mn.addMon(IPPMenu);
		mn.addMon(OPPMenu);
	}
	public void addActions(ONLComponent.CfgMonMenu mn) { addPortMenus(mn);}
}
