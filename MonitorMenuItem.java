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
 * File: MonitorMenuItem.java
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
import java.util.Vector;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;
import java.text.DecimalFormat;
import java.lang.reflect.Array;

public class MonitorMenuItem
{
	private static final int TEST_VXI = 6;
	//add mouse listener to make menu visible when selected
	public static abstract class PortMenu extends JMenu implements MonitorAction
	{
		//protected PollingRate pollingRate;
		protected int numParams = 0;
		protected PortItem mmItems[];
		protected NSPPort port;
		protected boolean isIPP = true;
		protected MonitorManager.TimeListener timeListener = null;

		private class ClickListener extends MouseAdapter
		{
			private PortMenu menu;
			public ClickListener(PortMenu m)
			{
				menu = m;
			}
			public void mouseReleased(MouseEvent e)
			{
				menu.setPopupMenuVisible(true);
			}
		}
		public PortMenu(String nm, NSPPort p, boolean ipp)
		{
			super(nm);
			port = p;
			timeListener = p.getMonitorable().getMonitorManager().getTimeListener();
			isIPP = ipp;
			setFont(new Font("Dialog", Font.PLAIN, 9));
		}

		public PortMenu(String nm, NSPPort p, int np, boolean ipp)
		{
			super(nm);
			port = p;
			numParams = np;
			mmItems = new PortItem[numParams];
			timeListener = p.getMonitorable().getMonitorManager().getTimeListener();
			isIPP = ipp;
			setFont(new Font("Dialog", Font.PLAIN, 9));
		}

		public Monitor addMonitor(Monitor m)
		{
			MonitorAction mmi = getMenuItem(m.getDataType());
			if (mmi != null) return (mmi.addMonitor(m));
			return null;
		}
		public Monitor addMonitor(MonitorDataType.Base mdt)
		{
			MonitorAction mmi = getMenuItem(mdt);
			if (mmi != null) return (mmi.addMonitor(mdt));
			else return null;
		}
		public void removeMonitor(Monitor m)
		{
			MonitorAction mmi = getMenuItem(m.getDataType());
			if (mmi != null) mmi.removeMonitor(m);
		}

		protected MonitorAction getMenuItem(MonitorDataType.Base mdt)
		{
			for (int i = 0; i < numParams; ++i)
			{
				if (mmItems[i].isDataType(mdt)) return mmItems[i];
			}
			return null;
		}

		public boolean isDataType(MonitorDataType.Base mdt)
		{
			for (int i = 0; i < numParams; ++i)
			{
				if (mmItems[i].isDataType(mdt)) return true;
			}
			return false;
		}
	}

	public static class PortBandwidth extends PortMenu
	{
		public PortBandwidth(NSPPort p, boolean ipp)
		{
			super("Port Bandwidth", p, 2, ipp);
			mmItems[0] = new PortBWWRecycle(p, ipp);
			mmItems[1] = new PortBWWORecycle(p, ipp);
			add(mmItems[0].getMenuItem());
			add(mmItems[1].getMenuItem());
		}
	}

	public static class IPPDiscard extends PortMenu
	{
		public IPPDiscard(NSPPort p)
		{
			super("Port Discard", p, 6, true);
			mmItems[0] = new PortVXTCS0Discard(p);
			mmItems[1] = new PortDiscardItem("RCB CLP0 Discard", p, true, MonitorEntry.IPPDiscard.RCBCLP0);
			mmItems[2] = new PortDiscardItem("RCB CLP1 Discard", p, true, MonitorEntry.IPPDiscard.RCBCLP1);
			mmItems[3] = new PortDiscardItem("CYCB Discard", p, true, MonitorEntry.IPPDiscard.CYCB);
			mmItems[4] = new PortDiscardItem("Bad HEC Discard", p, true, MonitorEntry.IPPDiscard.BADHEC);
			mmItems[5] = new IPPDiscardTotal("TOTAL Discard", p, (PortVXTCS0Discard)mmItems[0]);
			add(mmItems[0].getMenuItem());
			add(mmItems[1].getMenuItem());
			add(mmItems[2].getMenuItem());
			add(mmItems[3].getMenuItem());
			add(mmItems[4].getMenuItem());
			add(mmItems[5].getMenuItem());
		}
	}

	public static class OPPDiscard extends PortMenu
	{
		public OPPDiscard(NSPPort p)
		{
			super("Port Discard", p, 5, false);
			mmItems[0] = new PortDiscardItem("XMBCS0 Discard", p, false, MonitorEntry.OPPDiscard.XMBCS0);
			mmItems[1] = new PortDiscardItem("XMBCS1 Discard", p, false, MonitorEntry.OPPDiscard.XMBCS1);
			mmItems[2] = new PortDiscardItem("TOOLATE Discard", p, false, MonitorEntry.OPPDiscard.TOOLATE);
			mmItems[3] = new PortDiscardItem("RESEQUENCER Discard", p, false, MonitorEntry.OPPDiscard.RESEQUENCER);
			mmItems[4] = new PortDiscardItem("Total Discard", p, false, MonitorEntry.OPPDiscard.TOTAL);
			add(mmItems[0].getMenuItem());
			add(mmItems[1].getMenuItem());
			add(mmItems[2].getMenuItem());
			add(mmItems[3].getMenuItem());
			add(mmItems[4].getMenuItem());
		}
	}

	//class used by menu item to get user preferences
	public static class MMLParamOptions
	{
		protected PollingRate pollingRate = null;
		protected File logfile = null;
		protected Vector xtraParams = null;
		private boolean oneShot = false;
		private boolean is_rate = true;
		private boolean can_be_rate = true;

		public MMLParamOptions() { this(new Vector());}
		public MMLParamOptions(Vector x) 
		{
			xtraParams = x;
		}
		public MMLParamOptions(Object[] x)
		{
			xtraParams = new Vector();
			int max = Array.getLength(x);
			for (int i = 0; i < max; ++i)
			{
				xtraParams.add(x[i]);
			}
		}
		public PollingRate getPollingRate() { return pollingRate;}
		public void setPollingRate(PollingRate pr) { pollingRate = pr;}
		public java.io.File getLogFile() { return logfile;}
		public void setLogFile(java.io.File f) { logfile = f;}
		public void setXtraParams(Vector x)
		{
			xtraParams = x;
		}
		public Vector getXtraParams() { return xtraParams;}
		public void setOneShot(boolean b) 
		{ 
			//if (b) pollingRate = null;
			oneShot = b;
		}
		public boolean isOneShot() { return oneShot;}
		public void setIsRate(boolean r) { is_rate = r;}
		public boolean isRate() { return (is_rate && canBeRate());}
		public void setCanBeRate(boolean r) { can_be_rate = r;}
		public boolean canBeRate() { return can_be_rate;}
	}

	public static class INTParamOptions  extends MonitorMenuItem.MMLParamOptions
	{
		protected JTextField intParam = null;
		public INTParamOptions(String lbl)
		{
			this(lbl,"");
		} 
		public INTParamOptions(String lbl, String initVal)
		{
			super();
			intParam = new JTextField(5);
			intParam.setText(initVal);
			xtraParams.add(lbl);
			xtraParams.add(intParam);
		} 
		public int getIntParam() 
		{
			if (intParam.getText().length() > 0) return (Integer.parseInt(intParam.getText()));
			else return 0;
		}
	}

	public static abstract class BaseItem extends AbstractMonitorAction //JMenuItem implements MonitorAction
	{
		//protected PollingRate pollingRate;
		protected MonitorManager.TimeListener timeListener = null;
		protected ParamSelector selector = null;
		protected MonitorManager monitorManager = null;
		//protected ONLMonitorable onlMonitorable = null; //for most this will be NSPMonitor
		private JMenuItem menuItem = null;

		//inner class MonitorMenuListener
		public static class MonitorMenuListener extends MenuDragMouseAdapter
		{
			public static final int MML_CANCELLED = 0;
			public static final int MML_RATESET = 1;
			public static final int MML_NORATESET = 2;
			protected AbstractMonitorAction monAction;
			private ExpCoordinator expCoordinator = null;

			public MonitorMenuListener(AbstractMonitorAction d)
			{
				monAction = d;
				expCoordinator = d.onlMonitorable.getONLComponent().getExpCoordinator();
			}
			public void menuDragMouseReleased(MenuDragMouseEvent e)
			{
				String nm = "";
				JMenuItem mi = null;
				if (e.getSource() instanceof JMenuItem)
				{
					mi = (JMenuItem)e.getSource();
					nm = mi.getText();
				}
				ExpCoordinator.print(new String("MonitorMenuItem.BaseItem.MonitorMenuListener.menuDragMouseReleased " + nm), 4);
				//if (mi != null && mi.isArmed()) 
				monAction.select();
			}
			public void mouseReleased(MouseEvent e)
			{
				//System.out.println("MMI.BaseItem::mouseClicked");
				String nm = "";
				JMenuItem mi = null;
				if (e.getSource() instanceof JMenuItem)
				{
					mi = (JMenuItem)e.getSource();
					nm = mi.getText();
				}
				ExpCoordinator.print(new String("MonitorMenuItem.BaseItem.MonitorMenuListener.mouseReleased " + nm), 4);
				//if (mi != null && mi.isArmed()) 
				monAction.select();
			}
		}//inner class MonitorMenuListener

		protected BaseItem(String nm, MonitorManager w, ONLComponent.ONLMonitorable om)
		{
			this(nm, false, w, om);
		}
		protected BaseItem(String nm, boolean noListener, MonitorManager w, ONLComponent.ONLMonitorable om)
		{
			this(nm, noListener, w, om, new Font("Dialog", Font.PLAIN, 9));
		}
		protected BaseItem(String nm, boolean noListener, MonitorManager w, ONLComponent.ONLMonitorable om, Font f)
		{ 
			super(om);
			menuItem = new JMenuItem(nm);
			if (!noListener) 
			{
				MonitorMenuListener mml = new MonitorMenuListener(this);
				menuItem.addMenuDragMouseListener(mml);
				menuItem.addMouseListener(mml);
			}
			if (f != null) menuItem.setFont(f);
		}
		public JMenuItem getMenuItem() { return menuItem;}
		public String getText() { return (menuItem.getText());}
		public void setFont(Font f)
		{
			if (f != null) menuItem.setFont(f);
		}
	}

	public static abstract class PortItem extends BaseItem
	{
		protected RouterPort port;
		protected PortItem(String nm, RouterPort p)
		{ 
			this(nm, p, false);
		}

		protected PortItem(String nm, RouterPort p, boolean noListener)
		{ 
			super(nm, noListener, p.getMonitorable().getMonitorManager(), p.getMonitorable());
			port = p;
		}
		public RouterPort getPort() { return port;}
	}

	public static class PortDiscardItem extends PortItem
	{
		protected int type = MonitorEntry.IPPDiscard.TOTAL;
		protected boolean isIPP = true;
		public PortDiscardItem(String nm, NSPPort p, boolean ipp, int tp)
		{
			super(nm, p);
			isIPP = ipp;
			type = tp;
		}
		protected MonitorDataType.Base getDataType(MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			if (isIPP) 
			{
				dt = new  MonitorDataType.IPPDiscard(port.getID(), type);//, ("IPP " + String.valueOf(port.getID())));
			} 
			else 
			{
				dt = new  MonitorDataType.OPPDiscard(port.getID(), type);
			} 
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}
		public boolean isDataType(MonitorDataType.Base dt)
		{
			int ptype = dt.getParamType();
			if (isIPP && ptype == MonitorDataType.IPPDISCARD && type == dt.getMonitorID()) return true;
			if (!isIPP && ptype == MonitorDataType.OPPDISCARD && type == dt.getMonitorID()) return true;
			return false;
		}
	}

	public static class IPPDiscardTotal extends PortDiscardItem
	{
		//private Monitor vxtcs0Discard = null;
		//private Monitor totalDiscard = null;
		private PortVXTCS0Discard vcsDiscardItem = null;

		public IPPDiscardTotal(String nm, NSPPort p, PortVXTCS0Discard vdis)
		{
			super(nm, p, true, MonitorEntry.IPPDiscard.TOTAL);
			vcsDiscardItem = vdis;
		}

		public void selected(MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			java.io.File f = mpo.getLogFile();
			//Monitor totalDiscard = null;
			//if (totalDiscard == null)
			//{
			//Monitor m = null;
			Monitor totalDiscard = new MonitorAlgFunction(new MonitorDataType.MFormula(new String("IPPDtot " + port.getID()), 
					2, 
					Units.MBITS + Units.PERSEC,
					new String("Total Input Port Discards")), 
					monitorManager);
			if (f != null) totalDiscard.setLogFile(f);
			//if (monitor == null)
			//{
			dt = getDataType(mpo);
			Monitor monitor = addMonitor(dt);
			if (monitor.getDataType().isPeriodic() && !monitor.getDataType().getPollingRate().isEqual(mpo.getPollingRate()))
			{
				PollingRate pRate = monitor.getDataType().getPollingRate();
				JOptionPane.showMessageDialog(monitorManager.getMainWindow(), 
						(new String("Polling Rate is already set at " + pRate.getSecs() + "secs " + pRate.getUSecs() + "usecs")), 
						"Current Polling Rate",
						JOptionPane.PLAIN_MESSAGE);
				//System.out.println("MMI::setPollingRate polling rate already set");
			}
			//}	    
			//if (vxtcs0Discard == null) 
			//{
			MonitorDataType.IPPBandwidth mdt = new MonitorDataType.IPPBandwidth(port.getID(), MonitorEntry.IPPBandwidth.VXTCS0);
			mdt.setPollingRate(mpo.getPollingRate());
			mdt.setONLComponent(port);
			Monitor vxtcs0Discard = vcsDiscardItem.addMonitor(mdt);
			//}
		((MonitorAlgFunction)totalDiscard).setNextOp(MonitorAlgFunction.ADD);
		vxtcs0Discard.addMonitorFunction((MonitorAlgFunction)totalDiscard);
		monitor.addMonitorFunction((MonitorAlgFunction)totalDiscard);
		//}
		setSelected(totalDiscard);
		} 

		protected MonitorDataType.Base getDataType(MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = new MonitorDataType.IPPDiscard(port.getID(), MonitorEntry.IPPDiscard.TOTAL);
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}
	}

	public static class PortBW extends PortItem
	{
		private boolean isIPP = true;
		private boolean wRecyc = true; 
		protected int type = MonitorEntry.IPPBandwidth.BANDWIDTH;

		public PortBW(String nm, NSPPort p, boolean ipp, boolean wr)
		{
			super(nm, p);
			isIPP = ipp;
			wRecyc = wr;
		}

		protected MonitorDataType.Base getDataType(MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			if (isIPP) 
			{
				dt = new  MonitorDataType.IPPBandwidth(port.getID(), type);//, ("IPP " + String.valueOf(port.getID())));
				//System.out.println("IPPBandwidth addMonitor " + dt.getName());
				if (type != MonitorEntry.IPPBandwidth.VXTCS0)((MonitorDataType.IPPBandwidth)dt).setWithRecycled(wRecyc);
			} 
			else 
			{
				dt = new  MonitorDataType.OPPBandwidth(port.getID());
				((MonitorDataType.OPPBandwidth)dt).setWithRecycled(wRecyc);
			} 
			if (mpo.isOneShot() && mpo.getPollingRate() != null) ExpCoordinator.print(new String("MonitorMenuItem.PortBW.getDataType " + getText() + " mpo is oneshot but polling rate is set after addMonitor"), 1);
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}

		public boolean isDataType(MonitorDataType.Base mdt)
		{
			if ((mdt.getMonitorID() == type) && isIPP && (mdt.getParamType() == MonitorDataType.IPPBANDWIDTH))
			{
				if (((MonitorDataType.IPPBandwidth)mdt).isWithRecycled() == wRecyc)  
					return true;
				else 
					return false;
			}
			if ((mdt.getMonitorID() == type) && !isIPP && (mdt.getParamType() == MonitorDataType.OPPBANDWIDTH))
			{
				if (((MonitorDataType.OPPBandwidth)mdt).isWithRecycled() == wRecyc)  
					return true;
				else 
					return false;
			}
			else return false;
		}
	}

	public static class PortBWWRecycle extends PortBW
	{
		public PortBWWRecycle(String ttl, NSPPort p, boolean ipp)
		{
			super(ttl, p, ipp, true);
		}
		public PortBWWRecycle(NSPPort p, boolean ipp)
		{
			this("BW with Recycled", p, ipp);
		}
	}

	public static class PortBWWORecycle extends PortBW
	{
		public PortBWWORecycle(String ttl, NSPPort p, boolean ipp)
		{
			super(ttl, p, ipp, false);
		}
		public PortBWWORecycle(NSPPort p, boolean ipp)
		{
			this("BW w/o Recycled", p, ipp);
		}
	}

	public static class PortVXTCS0Discard extends PortBW
	{
		public PortVXTCS0Discard(NSPPort p)
		{
			super("VXTCS0 Discard", p, true, false);
			type = MonitorEntry.IPPBandwidth.VXTCS0;
		}
	}

	public static abstract class VXIBandwidth extends PortItem
	{
		private boolean isVCI = true;

		//inner ParamOptions class
		protected class VXIParamOptions extends INTParamOptions
		{
			public VXIParamOptions(String str)
			{
				super(str);
			}
			public int getVXI() { return (getIntParam());}
		}

		public VXIBandwidth(String title, NSPPort p, boolean vc)
		{
			super(title, p);
			isVCI = vc;
		}
		public MMLParamOptions getParamOptions() 
		{
			if (isVCI) return (new VXIParamOptions("to port:"));
			else return (new VXIParamOptions("vpi:"));
		}

		protected MonitorDataType.Base getDataType(MMLParamOptions mpo)
		{
			VXIParamOptions vpo = (VXIParamOptions)mpo;
			MonitorDataType.Base dt = null;
			if (isVCI)
				dt = new  MonitorDataType.VCIBandwidth(port.getID(), vpo.getVXI());
			else
				dt = new  MonitorDataType.VPIBandwidth(port.getID(), vpo.getVXI());
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}

		private Monitor getVXI(int vxi) 
		{
			int i;
			int max = monitors.size();
			for (i = 0; i < max; ++i)
			{
				Monitor rtn = (Monitor)monitors.elementAt(i);
				if (rtn.getDataType().getMonitorID() == vxi)
					return rtn;
			}
			return null;
		}
/*
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			ExpCoordinator.print(new String("MonitorMenuItem.VXIBandwidth.isDataType isVCI: " + isVCI +" mdt:" + mdt.getName()), TEST_VXI);
			if ((isVCI && (mdt.getParamType() == MonitorDataType.VCIBANDWIDTH)) ||
					(!isVCI && (mdt.getParamType() == MonitorDataType.VPIBANDWIDTH)))
				return true;
			else return false;
		}
		*/
	}

	public static class VCIBandwidth extends VXIBandwidth
	{
		public VCIBandwidth(NSPPort p)
		{
			super("Bandwidth to OPP", p, true);
		}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			//ExpCoordinator.print(new String("MonitorMenuItem.VCIBandwidth.isDataType mdt:" + mdt.getName()), TEST_VXI);
			return (mdt.getParamType() == MonitorDataType.VCIBANDWIDTH );
			//we no longer use the VPI Bandwidth so this fixes a problem before when w
		}
	}

	public static class VPIBandwidth extends VXIBandwidth
	{
		public VPIBandwidth(NSPPort p)
		{
			super("VPI Bandwidth", p, false);
		}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			//ExpCoordinator.print(new String("MonitorMenuItem.VPIBandwidth.isDataType mdt:" + mdt.getName()), TEST_VXI);
			return (mdt.getParamType() == MonitorDataType.VPIBANDWIDTH);
		}
	}
}
