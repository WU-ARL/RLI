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
 * File: Hardware.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created:1/22/2007
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.Color;
import javax.xml.stream.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

//import MonitorDataType.Base.XMLHandler.ComponentHandler;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Frame;
import java.awt.Component;


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////// Hardware /////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

public class Hardware extends ONLComponent implements NCCPOpManager.Manager,Field.Owner
{ 
	public static final int MAX_PORTS = 25;
	public static final int TEST_BATCHING = 1;
	public static final int TEST_LOADING = 3;
	public static final int TEST_PORT = 3;
	public static final int TEST_MONITOR = 5;
	public static final int TEST_SUBNET = 5;
	public static final int TEST_NCCPVALS = 5;
	public static final int TEST_INITCMD= HardwareBaseSpec.TEST_SUBTYPE;
	public static final int TEST_NCCP = 8;
	public static final String CPREQ = "cprequested";
	public static final String CTL_PORT_STR =  ExperimentXML.CPPORT;
	public static final String RETURN_NDX_STR = "RETURN_NDX";
	public static final String VMNAME = "vmname";
	public static final String INDEX = "index";
	public static final String CPHOST = "cphost";
	public static final String IPBASEADDR = "ipbaseaddr";
	private HardwareSpec.Subtype hwType = null;
	private int returnIndex = 0;
	protected int numPorts = 8;
	protected Vector<ONLComponent.PortBase> ports = null;
	protected Vector<Field> fields = null;
	protected Vector assigners = null;
	protected ONLGraphic graphic = null;
	protected ButtonAction buttonAction = null;
	protected Vector actions = null;
	protected CfgMonMenu menu = null;
	protected NCCPOpManager opManager = null;
	//private FieldList.OpManager listOpManager = null;
	private Command rebootCommand = null;
	private Command initCommand = null;
	private Command subtypeInitCommand = null;
	private JDesktopPane desktop = null;
	private DesktopListener desktopListener = null;
	private JFrame frame = null;


	//////////////////////////////////////////////// Hardware.LabelAction ///////////////////////////////////////////////////////////////////

	protected static class LabelAction extends AbstractAction //implements ListSelectionListener, KeyListener
	{
		private Hardware hwInstance = null;
		public LabelAction(Hardware r)
		{
			super("Change Label");
			hwInstance = r;
		}
		public void actionPerformed(ActionEvent e) 
		{
			TextFieldwLabel tfield = new TextFieldwLabel(30, "label:");
			tfield.setText(hwInstance.getLabel());
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			Object[] fields = {tfield};

			int rtn = JOptionPane.showOptionDialog((hwInstance.getGraphic()), 
					fields, 
					"Change Label", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				ExpCoordinator.print(new String("Hardware(" + hwInstance.getLabel() + ").LabelAction.actionPerformed text:" + tfield.getValue()), 1);
				hwInstance.setLabel((String)tfield.getValue());
				//hwInstance.getGraphic().revalidate();
				//hwInstance.getGraphic().repaint();
			}
		}
	}

	//////////////////////////////////////////////// Hardware.UserLabelAction ///////////////////////////////////////////////////////////////////

	protected static class UserLabelAction extends AbstractAction //implements ListSelectionListener, KeyListener
	{
		private Hardware hwInstance = null;
		public UserLabelAction(Hardware r)
		{
			super("Change User Label");
			hwInstance = r;
		}
		public void actionPerformed(ActionEvent e) 
		{
			TextFieldwLabel tfield = new TextFieldwLabel(30, "label:");
			tfield.setText(hwInstance.getUserLabel());
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			Object[] fields = {tfield};

			int rtn = JOptionPane.showOptionDialog((hwInstance.getGraphic()), 
					fields, 
					"Change UserLabel", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				ExpCoordinator.print(new String("Hardware(" + hwInstance.getLabel() + ").UserLabelAction.actionPerformed text:" + tfield.getValue()), 1);
				hwInstance.setUserLabel((String)tfield.getValue());
				//hwInstance.getGraphic().revalidate();
				//hwInstance.getGraphic().repaint();
			}
		}
	}

	///////////////////////////////////////////// Hardware.DesktopListener /////////////////////////////////////////////////
	protected static class DesktopListener extends InternalFrameAdapter implements MouseListener
	{
		public DesktopListener() { super();}
		public void mousePressed(MouseEvent e) { action(e);}
		public void mouseClicked(MouseEvent e){}
		public void mouseEntered(MouseEvent e){}
		public void mouseExited(MouseEvent e){}
		public void mouseReleased(MouseEvent e){}
		public void internalFrameActivated(InternalFrameEvent e)  { action(e);}
		public void internalFrameDeiconified(InternalFrameEvent e) { action(e);}
		public void internalFrameOpened(InternalFrameEvent e) { action(e);}
		public void action(MouseEvent e) 
		{
			ExpCoordinator.print("Hardware.DesktopListener.action me move to front", 2);
			((JInternalFrame)e.getSource()).moveToFront(); 
		}
		public void action(InternalFrameEvent e) 
		{
			ExpCoordinator.print("Hardware.DesktopListener.action ife move to front", 2);
			e.getInternalFrame().moveToFront(); 
		}
	}

	//////////////////////////////////////////////////// Hardware.OpManager/////////////////////////////////////////////////////////////////////

	protected static class OpManager extends NCCPOpManager
	{
		protected Port port = null;
		private class NCCP_Response extends NCCP_MonitorResponse
		{
			public NCCP_Response()
			{
				super();
			}
			public void setLT(double nsp_lt) {} //don't care since it's not a periodic op 
			public void retrieveFieldData(DataInput din) throws IOException
			{
				super.retrieveFieldData(din);
				ExpCoordinator.print(new String("Hardware.OpManager.NCCP_Response.retrieveFieldData statusMsg: " + getStatusMsg()), 6);
			}
		}//end Harware.OpManager.NCCP_Response
		public OpManager(MonitorDaemon md) { this(md, null);}
		public OpManager(MonitorDaemon md, Port p)
		{
			super(md);//(MonitorDaemon)getONLDaemon(ONLDaemon.HWC));
			port = p;
			response = new NCCP_Response();
		}
		public void processResponse(NCCP.ResponseBase r) 
		{
			super.processResponse(r);
			ExpCoordinator.print("Hardware.OpManager.processResponse", 7);
		}
		protected void setRendMarker(NCCP.RequesterBase req, NCCPOpManager.Operation op, int i)
		{
			REND_Marker_class mrkr = req.getMarker();//new REND_Marker_class(mdt.getPort(), mdt.isIPP(), mdt.getMonitorID(), ((MDataType)mdt).getIndex());
			mrkr.setField3(i);
			req.setMarker(mrkr);
		}
		public void addOperation(Operation op)
		{
			super.addOperation(op);
			if ((op instanceof Command.NCCPOp) && !(((Command.NCCPOp)op).getCommand().isImmediate()))
				ExpCoordinator.theCoordinator.addToProgressBar(op);
		}

		public void removeOperation(Operation op)
		{
			if (contains(op))
			{
				super.removeOperation(op);
				if ((op instanceof Command.NCCPOp) && !(((Command.NCCPOp)op).getCommand().isImmediate()))
					ExpCoordinator.theCoordinator.removeFromProgressBar(op);
			}
		}
	}


	////////////////////////////////////////////////// ButtonAction //////////////////////////////////////////////////////////////////////////
	protected static class ButtonAction extends ONLGraphic.ButtonListener
	{
		public ButtonAction(ONLComponent c)
		{
			super(c);
		}
		public void mouseClicked(MouseEvent me)
		{
			//display description
			HardwareGraphic.MainButton mbutton = (HardwareGraphic.MainButton)me.getSource();
			Point2D pnt = mbutton.getCenter();
			((Hardware)getONLComponent()).showMenu(mbutton, pnt);
		}
	}


	//////////////////////////////////////////////// CPAction ///////////////////////////////////////////////////////////////////

	protected static class CPAction extends AbstractAction //implements ListSelectionListener, KeyListener
	{
		private Hardware hardware = null;
		public CPAction(Hardware r)
		{
			super("Set CP");
			hardware = r;
		}
		public void actionPerformed(ActionEvent e) 
		{
			TextFieldwLabel tfield = new TextFieldwLabel(30, "CP addr:");
			String tmp = hardware.getProperty(CPREQ);
			if (tmp != null) tfield.setText(tmp);
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};

			int rtn = JOptionPane.showOptionDialog((hardware.getGraphic()), 
					tfield, 
					"Add Parameter", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				hardware.setProperty(CPREQ, tfield.getText());
			}
		}
	}

	//////////////////////////////////////////////// SPPMonCPAction ///////////////////////////////////////////////////////////////////

	protected static class SPPMonCPAction extends AbstractAction //implements ListSelectionListener, KeyListener
	{
		private Hardware hardware = null;
		public SPPMonCPAction(Hardware r)
		{
			super("Set Daemon");
			hardware = r;
		}
		public void actionPerformed(ActionEvent e) 
		{
			TextFieldwLabel tfield = new TextFieldwLabel(30, "daemon addr:");
			String tmp = hardware.getProperty(CPHOST);
			if (tmp != null) tfield.setText(tmp);
			TextFieldwLabel tfield2 = new TextFieldwLabel(30, "daemon port:");
			String tmp2 = hardware.getProperty(CTL_PORT_STR);
			if (tmp2 != null) tfield2.setText(tmp2);
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			Object[] fields = {tfield, tfield2};

			int rtn = JOptionPane.showOptionDialog((hardware.getGraphic()), 
					fields, 
					"Specify Daemon", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				hardware.setProperty(CPREQ, tfield.getText());
				hardware.setCPAddr(tfield.getText(), tfield2.getInt());
			}
		}
	}

	/////////////////////////////////////////////////////// Hardware.Port ///////////////////////////////////////////////////////////////////////////
	public static class Port extends ONLComponent.PortBase implements NCCPOpManager.Manager,Field.Owner
	{
		protected ButtonAction buttonAction = null;
		protected HardwareSpec.PortSpec portSpec = null;
		protected Vector actions = null;
		protected Vector<Field> fields = null;
		protected Vector assigners = null;
		protected ONLComponent.CfgMonMenu menu = null;
		protected NCCPOpManager opManager = null;
		protected static final String portLabels = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		protected static final int MAX_SUBNET_INDEX = 14;
		private JDesktopPane desktop = null;
		private DesktopListener desktopListener = null;
		private JFrame frame = null;

		///////////////////////////////////////////////////////// ButtonAction ////////////////////////////////////////////////
		protected static class ButtonAction extends ONLGraphic.ButtonListener
		{
			public ButtonAction(Port c)
			{
				super(c);
			}
			public void mousePressed(MouseEvent e)
			{
				if (((HardwareGraphic.HWButton)e.getSource()).isPortButton())
				{
					//gonna have to check if IPP or OPP
					//get port button and call showMenu with button coordinates and the main jpanel
					HardwareGraphic.PortButton pbutton = (HardwareGraphic.PortButton)e.getSource();
					String ttl = new String("Port " + pbutton.getPort());
					((Port)getONLComponent()).showMenu(pbutton, pbutton.getCenter());
				}
			}
		}
		///////////////////////// Hardware.Port.HWContentHandler ///////////////////////////////////////////////////////////
		protected static class HWContentHandler extends DefaultHandler
		{
			protected Hardware hardware = null;
			protected Hardware.Port currentPort = null;
			protected ExperimentXML expXML = null;
			protected String currentElement = "";

			public HWContentHandler(ExperimentXML expxml, Hardware.Port hwip)
			{
				super();
				currentPort = hwip;
				expXML = expxml;
			}

			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	  
				currentElement = new String(localName);
				ExpCoordinator.print(new String("Hardware.Port.HWContentHandler.startElement " + currentElement), TEST_LOADING);
				if (currentElement.equals(ExperimentXML.FIELD))
				{
					Field field = currentPort.getField(attributes.getValue(ExperimentXML.FIELDNAME));
					expXML.setContentHandler(field.getContentHandler(expXML));
				}
				if (currentElement.equals(ExperimentXML.HWTABLE))
				{
					ExpCoordinator.print(new String("     table title:" + attributes.getValue(ExperimentXML.TITLE)), TEST_LOADING);
					HWTable table = (HWTable)currentPort.getTable(attributes.getValue(ExperimentXML.TITLE));
					if (table != null) expXML.setContentHandler(table.getContentHandler(expXML));
				}
				if (currentElement.equals(ExperimentXML.ROUTE_TABLE))
				{
					ExpCoordinator.print(new String("     table title:" + attributes.getValue(ExperimentXML.TITLE)), TEST_LOADING);
					HWTable table = (HWTable)currentPort.getTable(attributes.getValue(ExperimentXML.TITLE), ONLComponent.RTABLE);
					if (table != null) expXML.setContentHandler(table.getContentHandler(expXML));
				}
			}
			public void setCurrentElement(String s) { currentElement = new String(s);}
			public String getCurrentElement() { return currentElement;}
			protected Hardware.Port getCurrentPort() { return currentPort;}
			public void endElement(String uri, String localName, String qName)
			{
				if (localName.equals(ExperimentXML.PORT)) expXML.removeContentHandler(this);
			}
		}//end class Hardware.Port.HWContentHandler


		////////////////////////////////// start of Hardware.Port methods ////////////////////////////////////
		public Port(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
		{
			super(lbl,tp,tr,ec);
			setParentFromString(tr.readString());
			setID(tr.readInt());
			initialize();
		}

		public Port(Hardware wd, String tp, int p, ExpCoordinator ec)
		{
			super(wd, new String("port" + p), tp, ec);
			setParent(wd);
			setID(p);
			initialize();
		}

		public boolean isPort() { return true;}
		protected void initialize()
		{
			initializeMonitorable();
			buttonAction = new ButtonAction(this);
			if (fields == null) fields = new Vector<Field>();
			if (assigners == null) assigners = new Vector();
			if (actions == null) actions = new Vector();
			int max;
			int i;
			if (portSpec == null) 
			{
				//if (isRouter()) initSubnetIndices();
				return;
			}

			Vector aspecs = portSpec.getAssignerSpecs();
			max = aspecs.size();
			for (i = 0; i < max; ++i)
			{
				ComponentXMLHandler.AssignerSpec as = (ComponentXMLHandler.AssignerSpec)aspecs.elementAt(i);
				assigners.add(as.createAssigner());
			}
			Vector hw_flds = portSpec.getFieldSpecs();
			max = hw_flds.size();
			for (i = 0; i < max; ++i)
			{
				FieldSpec fs = (FieldSpec)hw_flds.elementAt(i);
				Field f = new Field(fs, this);
				f.addNextHopListener();
				fields.add(f);
			}
			Vector tbl_specs = portSpec.getTableSpecs();
			max = tbl_specs.size();
			for (i = 0; i < max; ++i)
			{
				HWTable.Spec tcspec = (HWTable.Spec)tbl_specs.elementAt(i);
				addTable(tcspec.createTable(this));
			}
			initializeActions();
			Vector cfgParams = portSpec.getCfgCommands();
			//need to send cfg commands
			max = cfgParams.size();
			CommandSpec elem;
			Command cspec;
			for (i = 0; i < max; ++i)
			{
				elem = (CommandSpec)cfgParams.elementAt(i);
				cspec = getCommand(elem.getOpcode(), false);
				addOperation(new Command.NCCPOp(cspec, elem.getDefaultValues(), this));
			}
			//if (isRouter()) initSubnetIndices();
		}
		public Port(Hardware hwi, int ndx, ExpCoordinator ec)
		{
			this(hwi, new String(hwi.getType() + ONLComponent.PORT_LBL), ndx, ec);
		}
		public void addGraphic(HardwareGraphic wg)
		{
			//ExpCoordinator.print("Hardware.Port::addGraphic", 6);
			wg.addPortListener(buttonAction);
		}

		public void setParent(ONLComponent p) 
		{
			super.setParent(p);
			setPortSpec();
		}
		protected void setPortSpec()
		{
			ONLComponent p = getParent();
			if (p != null && p instanceof Hardware) 
			{
				portSpec = ((Hardware)p).getPortSpec();
				if (portSpec != null) setInterfaceType(portSpec.getInterfaceType());
			}
		}
	
		public void addRoute(String ip, int mask, int nhp, ONL.IPAddress nhip)
		{
			ExpCoordinator.print(new String("Hardware.Port(" + getLabel() + ").addRoute " + ip + "/" + mask + " " + nhp + ":" + nhip.toString()), Topology.TEST_DEFRTS);
			HWRouteTable rt = (HWRouteTable)getTableByType(ONLComponent.RTABLE);
			if (rt != null) rt.addRoute(ip, mask, nhp, nhip);
		}
		public void addGeneratedRoute(String ip, int mask, int nhp, ONL.IPAddress nhip)
		{
			ExpCoordinator.print(new String("Hardware.Port(" + getLabel() + ").addRoute " + ip + "/" + mask + " " + nhp + ":" + nhip.toString()), Topology.TEST_DEFRTS);
			HWRouteTable rt = (HWRouteTable)getTableByType(ONLComponent.RTABLE);
			if (rt != null) rt.addGeneratedRoute(ip, mask, nhp, nhip);
		}
		public void removeGeneratedRoutes()
		{
			ExpCoordinator.print(new String("Hardware.Port(" + getLabel() + ").removeGeneratedRoutes"), Topology.TEST_DEFRTS);
			HWRouteTable rt = (HWRouteTable)getTableByType(ONLComponent.RTABLE);
			if (rt != null) rt.removeGeneratedElements();
		}
		public void clearRoutes()
		{
			ExpCoordinator.print(new String("Hardware.Port(" + getLabel() + ").clearRoutes"), Topology.TEST_DEFRTS);
			HWRouteTable rt = (HWRouteTable)getTableByType(ONLComponent.RTABLE);
			if (rt != null) rt.clearRoutes();
		}
		
		/*
		public void addRoute(String ip, int mask, NextHop nh)
		{
			ExpCoordinator.print(new String("Hardware.Port(" + getLabel() + ").addRoute " + ip + "/" + mask + " " + nh.getPort()), Topology.TEST_DEFRTS);
			HWRouteTable rt = (HWRouteTable)getTableByType(ONLComponent.RTABLE);
			if (rt != null) rt.addRoute(ip, mask, nh);
		}*/
		
		public Field getField(String str)
		{
			int max = fields.size();
			Field elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (Field)fields.elementAt(i);
				if (elem.getLabel().equals(str)) return elem;
			}
			return null;
		}
		public IDAssigner.ID getNewID(FieldSpec.AssignerSpec as)
		{
			if (as.isPortAssigner())
			{
				IDAssigner assigner = getIDAssigner(as.getName());
				if (assigner != null) 
					return (assigner.getNewID());
				else return null;
			}
			else return(super.getNewID(as));
		}   
		public IDAssigner getIDAssigner(String nm)
		{
			int max = assigners.size();
			for (int i = 0; i < max; ++i)
			{
				IDAssigner ias = (IDAssigner)assigners.elementAt(i);
				if (ias.getName().equals(nm)) return ias;
			}
			return null;
		}
		public ONLCTable getTable(String lbl) { return (getTable(lbl, ONLComponent.HWTABLE));}
		public ONLCTable getTable(String lbl, String tp)
		{
			ONLComponent c = getChild(lbl, tp);
			if (c != null && c instanceof ONLCTable) return ((ONLCTable)c);
			else
			{
				if (c == null)
					ExpCoordinator.print(new String("Hardware.Port(" + getLabel() + ").getTable " + lbl + ":" + tp + " not found"), TEST_PORT);
				else
					ExpCoordinator.print(new String("Hardware.Port(" + getLabel() + ").getTable " + lbl + ":" + tp + " not table"), TEST_PORT);
			}
			return null;
		}
		public ONLCTable getTableByType(String tp) //this supports old stuff should go away will have to use the label to distinguish tables
		{
			ONLComponent rtn = getChild(tp);
			if (rtn != null && rtn instanceof ONLCTable) return ((ONLCTable)rtn);
			return null;
		}

		public void addTable(ONLCTable table)
		{
			if (table.getID() < 0)
			{
				table.setID(getNewTableID());
			}
			ExpCoordinator.print(new String("Hardware.Port(" + table.getLabel() + ").addTable " + table.getLabel()), TEST_PORT);
			actions.add(table.getAction());
			addChild(table);
		}
		public void removeTable(ONLCTable table) 
		{
			actions.remove(table.getAction());
			removeChild(table);
		}
		private int getNewTableID()
		{
			ONLComponentList clist = getChildren();
			ONLComponent celem;
			int max = clist.size();
			int min_id = 1;
			for (int i = 0; i < max; ++i)
			{
				celem = clist.onlComponentAt(i);
				if (celem instanceof ONLCTable)
					if (((ONLCTable)celem).getID() >= min_id) min_id = ((ONLCTable)celem).getID() + 1;
			}
			return min_id;
		}
		protected void initializeMonitorable() 
		{ 
			monitorable = new HWMonitor.ChildMonitorable(this, ((HWMonitor)getParent().getMonitorable()), portSpec);
			((HWMonitor.ChildMonitorable)monitorable).initializeMMItems();
		}
		protected void showMenu(JComponent c, Point2D pnt)
		{
			if (expCoordinator.isLinkToolEnabled()) return;
			if (menu == null)
			{
				menu = new ONLComponent.CfgMonPopupMenu(this, new String("Port " + getID()));
				menu.addDescription(getDescription());
				//add Configuration actions
				Vector acts = getActions();
				int max = acts.size();
				for (int i = 0; i < max; ++i)
					menu.addCfg((Action)acts.elementAt(i));
				//add Monitoring actions
				//if (monitorable instanceof Hardware.HWMonitor.ChildMonitorable) 
				//((Hardware.HWMonitor.ChildMonitorable)monitorable).addActions(menu);
				if (monitorable != null) monitorable.addActions(menu);
			}
			menu.showMenu(c, pnt);
		}
		protected ONLComponent.CfgMonJMenu getCfgMenuCopy()
		{
			CfgMonJMenu rtn = new CfgMonJMenu(this, new String("Port " + getID()));
			rtn.addDescription(getDescription());
			//add Configuration actions
			Vector acts = getActions();
			int max = acts.size();
			for (int i = 0; i < max; ++i)
				rtn.addCfg((Action)acts.elementAt(i));
			//add Monitoring actions
			if (monitorable instanceof Hardware.HWMonitor.ChildMonitorable) 
				((Hardware.HWMonitor.ChildMonitorable)monitorable).addActions(rtn);
			return rtn;
		}
		public ONLGraphic getGraphic() 
		{ 
			if (getParent() != null) 
			{
				if (getParent().getGraphic() instanceof HardwareGraphic)
					return (((HardwareGraphic)getParent().getGraphic()).getPortButton(this));
				else return (getParent().getGraphic());
			}
			else return null;
		}
		public Vector getActions() { return actions;}
		protected Action getAction(String ttl)
		{
			int max = actions.size();
			for (int i = 0; i < max; ++i)
			{
				Action elem = (Action)actions.elementAt(i);
				if (((String)elem.getValue(Action.NAME)).equals(ttl)) return elem;
			}
			return null;
		}
		protected ONLDaemon getONLDaemon(int tp) 
		{
			Hardware par = (Hardware)getParent();
			if (par != null) return (par.getONLDaemon(tp)); 
			return null;
		}
		public void setIPAddr(String ip)
		{
			super.setIPAddr(ip);
			if (getParent() instanceof HardwareHost) getParent().setIPAddr(ip);
		}

		public void setSubnet(SubnetManager.Subnet sn)
		{
			super.setSubnet(sn);
			if (getParent() instanceof HardwareHost) getParent().setSubnet(sn);
		}
		protected void addToDescription(String key, String d) 
		{
			super.addToDescription(key, d);
			if (menu != null) menu.addDescription(getDescription());
		}
		protected void initializeActions()
		{
			Vector cmds = getHWSpec().getPortSpec().getCommandSpecs();
			if (cmds.isEmpty()) return;
			int max = fields.size();
			int i;
			for (i = 0; i < max; ++i)
			{
				Field fld = (Field)fields.elementAt(i);
				Action ua = fld.getUpdateAction();
				if (ua != null) actions.add(ua);
			}
			max = cmds.size();
			for (i = 0; i < max; ++i)
			{
				Command cmd = new Command((CommandSpec)cmds.elementAt(i), this);
				actions.add(new Command.CAction(this, cmd));
			}
		}
		public Command getCommand(int monitor_id, boolean is_mon)//PROBLEM:need to add table ops to this
		{
			if (is_mon) return (((Hardware.HWMonitor.ChildMonitorable)monitorable).getCommand(monitor_id));
			int max = actions.size();
			Command.CAction ca;
			for (int i = 0; i < max; ++i)
			{
				Object elem = actions.elementAt(i);
				if (elem instanceof Command.CAction)
				{
					ca = (Command.CAction)actions.elementAt(i);
					if (ca.getCommand().getOpcode() == monitor_id) return (ca.getCommand());
				}
			}
			return null;
		}
		public HardwareSpec.Subtype getHWSpec()
		{
			return (((Hardware)getParent()).getHWSpec());
		}
		public double getVersion() { return (((Hardware)getParent()).getVersion());}
		//NCCPOpManager.Manage
		public void addOperation(NCCPOpManager.Operation op) 
		{
			if (opManager == null) getOpManager();
			opManager.addOperation(op);
		}
		public void removeOperation(NCCPOpManager.Operation op) { if (opManager != null) opManager.removeOperation(op);}
		//end NCCPOpManager.Manager
		public NCCPOpManager getOpManager()
		{ 
			if (opManager == null) opManager = new OpManager((MonitorDaemon)getONLDaemon(ONLDaemon.HWC), this);
			return opManager;
		}
		/*
	public NCCPOpManager getListOpManager()
	{ 
	if (listOpManager == null) listOpManager = new FieldListOpManager((MonitorDaemon)getONLDaemon(ONLDaemon.HWC));
	return listOpManager;
	}
		 */
		public void setOpManager(NCCPOpManager opman) { opManager = opman;}
		public Cluster.Instance getCluster() { return (getParent().getCluster());}

		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.INDEX, String.valueOf(getID()));
			if (getSubnet() != null) 
			{
				xmlWrtr.writeAttribute(ExperimentXML.SUBNET, String.valueOf(getSubnet().getIndex()));
				if (ExpCoordinator.isOldSubnet()) xmlWrtr.writeAttribute(ExperimentXML.SUBNET_ID, String.valueOf(((OldSubnetManager.Subnet)getSubnet()).getSubIndex()));
				ONL.IPAddress tmp_ip = getIPAddr();
				byte sub_ndx = tmp_ip.getBytes()[3];

				if (ExpCoordinator.isOldSubnet())
				{
					sub_ndx = (byte)(sub_ndx - ((OldSubnetManager.Subnet)getSubnet()).getSubIndex());
				}
				xmlWrtr.writeAttribute(ExperimentXML.SUBNET_INDEX, String.valueOf(sub_ndx));
			}
			xmlWrtr.writeStartElement(ExperimentXML.FIELDS);
			int max = fields.size();
			int i;
			for (i = 0; i < max; ++i)
			{
				((Field)fields.elementAt(i)).writeXML(xmlWrtr);
			}
			xmlWrtr.writeEndElement();//end fields
			xmlWrtr.writeStartElement(ExperimentXML.TABLES);
			Vector tables = getHWTables();
			max = tables.size();
			for (i = 0; i < max; ++i)
			{
				((HWTable)tables.elementAt(i)).writeXML(xmlWrtr);
			}
			xmlWrtr.writeEndElement();//end tables
		}
		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new Port.HWContentHandler(exp_xml, this));}
		protected boolean merge(ONLComponent c)
		{
			boolean rtn = true;
			if (c instanceof Port)
			{
				Port p = (Port)c;
				if (getID() != ((Port)c).getID()) rtn = false;
				if (rtn)
				{
					//merge fields
					int max = fields.size();
					for (int i = 0; i < max; ++i)
					{
						Field f1 = fields.elementAt(i);
						Field f2 = p.getField(f1.getLabel());
						if (f2 != null && !f1.isEqual(f2))
							ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String(getLabel() + " " + f1.getLabel() + " Inconsistency master has " + f1.getLabel() + ":" + f1.getCurrentValue().toString() + " other has " + f1.getLabel() + ":" + f2.getCurrentValue().toString() + "\n"),
									"Hardware Field Error",
									"Hardware Field Error",
									StatusDisplay.Error.LOG)); 
					}
					//merge tables
					max = getNumChildren();
					for (int i = 0 ; i < max; ++i)
					{
						ONLComponent child = getChildAt(i);
						if (child instanceof HWTable)
						{
							HWTable hwt = (HWTable)p.getTable(child.getLabel());
							if (hwt == null || !child.merge(hwt)) rtn = false;
						}
					}
				}
			}
			else rtn = false;
			if (!rtn)
			{
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("Hardware Port(" + getLabel() + ") inconsistent\n")),
						"Hardware Port Inconsistency",
						"Hardware Port Inconsistency",
						StatusDisplay.Error.LOG)); 
			}
			return rtn;
		}

		protected JFrame getFrame() { return frame;}
		public void showDesktop(JInternalFrame f)
		{
			showDesktop();
			try
			{
				Component[] comps = desktop.getComponents();
				boolean found = false;
				int max = comps.length;
				for (int i = 0; i < max; ++i)
				{
					if (comps[i] == f) 
					{
						found = true;
						break;
					}
				}
				if (!found) addToDesktop(f);
				f.setClosed(false);
				f.setSelected(true);
				f.setVisible(true);
				f.toFront();
			}
			catch(java.beans.PropertyVetoException e){}
			desktop.setSelectedFrame(f);
		}
		public void showDesktop()
		{
			if (desktop == null)
			{
				frame = new JFrame(getLabel());
				desktop = new JDesktopPane();
				desktopListener = new DesktopListener();
				frame.setContentPane(desktop);
				frame.setSize(700,300);
				JMenuBar mb = new JMenuBar();
				frame.setJMenuBar(mb);
				JMenu menu = new JMenu("Operations");
				int max = actions.size();
				Action elem;
				for (int i = 0; i < max; ++i)
				{
					elem = (Action)actions.elementAt(i);
					menu.add(new JMenuItem(elem));
				}
				mb.add(menu);
			}
			if ((frame.getExtendedState() & Frame.ICONIFIED) != 0)
				frame.setExtendedState(frame.getExtendedState() - Frame.ICONIFIED);
			//frame.show(); //show deprecated 1.5
			frame.setVisible(true);  //replacement 1.5
		}
		public void addToDesktop(JInternalFrame f)
		{
			showDesktop();
			f.setIconifiable(true); 
			f.setResizable(true);
			f.addMouseListener(desktopListener);
			f.addInternalFrameListener(desktopListener);
			if (expCoordinator.getDebugLevel() > 0) f.setClosable(true);
			desktop.add(f);
			desktop.setSelectedFrame(f);
		}
		public void clear()
		{ 
			super.clear();
			//clear all tables //should probably be calling clear on all children
			ONLComponentList childlist = getChildren();
			int max = childlist.size();
			ONLComponent elem;
			for (int i = 0; i < max; ++i)
			{
				elem = childlist.onlComponentAt(i);
				if (elem instanceof ONLCTable) ((ONLCTable)elem).clear();
			}
			if (frame != null ) 
			{
				desktop.removeAll();
				desktop = null;
				frame.dispose();
				frame = null;
			}
		}
		public HardwareSpec.PortSpec getPortSpec() 
		{
			if (portSpec == null) portSpec = ((Hardware)getParent()).getPortSpec();
			return portSpec;
		}
	}//end class Hardware.Port

	////////////////////////////////////////////////// Hardware.HWMonitor /////////////////////////////////////////////////////////////////////////////////
	protected static class HWMonitor extends NodeMonitor
	{
		private int numPorts = 0;
		private MMenuItem mmenuItems[] = null;
		private int numMMItems = -1;

		///////////////////////////////////////////////// Hardware.HWMonitor.ChildMonitorable ////////////////////////////////////////////////
		public static class ChildMonitorable implements ONLComponent.ONLMonitorable
		{
			private ONLComponent child = null;
			private ONLComponent.ONLMonitorable hwmonitor = null;
			private Vector mmenuItems = null;
			private Vector monitorSpecs = null;
			//private int numMMItems = -1;

			public ChildMonitorable(ONLComponent c, ONLComponent.ONLMonitorable hwm, ComponentXMLHandler.Spec spec)
			{
				child = c;
				ExpCoordinator.print(new String("Hardware.HWMonitor.ChildMonitorable child:" + child.getLabel()), TEST_MONITOR);
				hwmonitor = hwm;
				monitorSpecs = spec.getMonitorSpecs();
			}
			public void initializeMMItems()
			{
				int numMMItems = monitorSpecs.size();//Array.getLength(mons);
				mmenuItems = new Vector();//MMenuItem[numMMItems];
				for (int i = 0; i < numMMItems; ++i)
				{
					mmenuItems.add(createMMItem((CommandSpec)monitorSpecs.elementAt(i)));
				}
			}
			public MMenuItem createMMItem(CommandSpec cs)
			{
				ExpCoordinator.print(new String("Hardware.HWMonitor.ChildMonitorable.createMMItem(" + cs.getLabel() + " child:" + child.getLabel()), TEST_MONITOR);
				return (new MMenuItem(child, cs.createCommand(child)));
			}
			public Monitor addMonitor(MonitorDataType.Base mdt)
			{
				MonitorAction ma = getMonitorAction(mdt);
				if (ma != null) return (ma.addMonitor(mdt));
				else return null;
			}
			public Monitor addMonitor(Monitor m) { return(hwmonitor.addMonitor(m));}
			public void removeMonitor(Monitor m) { hwmonitor.removeMonitor(m);}
			public ONLComponent getONLComponent() { return child;}
			public MonitorManager getMonitorManager() { return (hwmonitor.getMonitorManager());}
			public void clear(){}
			public void addActions(ONLComponent.CfgMonMenu menu)
			{
				if (mmenuItems == null) initializeMMItems();
				int numMMItems = mmenuItems.size();
				for (int i = 0; i < numMMItems; ++i)
					menu.addMon(((MMenuItem)mmenuItems.elementAt(i)).getMenuItem());
			}
			// public void initializeMenus(){}
			public MonitorAction getMonitorAction(MonitorDataType.Base dt)
			{
				if (mmenuItems == null) initializeMMItems();
				int numMMItems = mmenuItems.size();
				for (int i = 0; i < numMMItems; ++i)
				{
					if (((MMenuItem)mmenuItems.elementAt(i)).isDataType(dt)) return ((MMenuItem)mmenuItems.elementAt(i));
				}
				return null;
			}
			public Command getCommand(int opcode)
			{
				if (mmenuItems == null) initializeMMItems();
				int numMMItems = mmenuItems.size();
				for (int i = 0; i < numMMItems; ++i)
				{
					if (((MMenuItem)mmenuItems.elementAt(i)).getCommand().getOpcode() == opcode) return (((MMenuItem)mmenuItems.elementAt(i)).getCommand());
				}
				return null;
			}
			protected void addMMenuItem(MMenuItem mmi) { mmenuItems.add(mmi);}
			protected Vector getMMenuItems() { return mmenuItems;}
		}//end inner class Hardware.HWMonitor.ChildMonitorable

		//////////////////////////////////////////// Hardware.HWMonitor methods ///////////////////////////////////////////////////////////////////

		public HWMonitor(Vector<ONLDaemon> mds, Hardware comp, MonitorManager w) 
		{
			super(mds, comp, w);
			numPorts = ((Hardware)comp).getNumPorts();
		}

		public Monitor addMonitor(MonitorDataType.Base dt)
		{
			ExpCoordinator.print(new String("Hardware(" + getONLComponent().getLabel() + ").HWMonitor.addMonitor(" + dt.getName() + ")"), TEST_MONITOR);
			//need tp add a switchwide version
			if (dt.isNodeWide())
			{
				MMenuItem mmi = getMMItem(dt);
				if (mmi != null) return (mmi.addMonitor(dt));
				ExpCoordinator.print("    no menu item found", TEST_MONITOR);
			}
			return null;
		}

		private MMenuItem getMMItem(MonitorDataType.Base mdt) 
		{
			if (numMMItems < 0) initializeActions();
			for (int i = 0; i < numMMItems; ++i)
			{
				if (mmenuItems[i].isDataType(mdt)) return (mmenuItems[i]);
			}
			return null;
		}
		public int getNumPorts() { return numPorts;}
		private void initializeActions()
		{
			if (numMMItems < 0)
			{            
				Vector mons = ((Hardware)getONLComponent()).getHWSpec().getMonitorSpecs();
				numMMItems = mons.size();//Array.getLength(mons);
				if (numMMItems > 0) mmenuItems = new MMenuItem[numMMItems];
				for (int i = 0; i < numMMItems; ++i)
				{
					mmenuItems[i] = new MMenuItem((Hardware)getONLComponent(), new Command((CommandSpec)mons.elementAt(i), (Hardware)getONLComponent()));
				}
			}
			ExpCoordinator.print(new String("Hardware(" + getONLComponent().getLabel() + ").HWMonitor.initializeActions numMMItems:" + numMMItems), TEST_MONITOR);
		}
		public void addActions(ONLComponent.CfgMonMenu menu) 
		{
			if (numMMItems < 0) initializeActions();
			for (int i = 0; i < numMMItems; ++i)
			{
				menu.addMon(mmenuItems[i].getMenuItem());
			}
		}
		public Command getCommand(int opcode)
		{
			if (numMMItems < 0) initializeActions();
			ExpCoordinator.print(new String("Hardware(" + getONLComponent().getLabel() + ").HWMonitor.getCommand " + opcode + " numMMItems:" + numMMItems), TEST_MONITOR);
			for (int i = 0; i < numMMItems; ++i)
			{
				int op = mmenuItems[i].getCommand().getOpcode();
				ExpCoordinator.print(new String("    item["+ i + "] op:" + op), TEST_MONITOR);
				if ( op == opcode) return (mmenuItems[i].getCommand());
			}
			return null;
		}
	}//end inner class Hardware.HWMonitor


	///////////////////////// Hardware.HWContentHandler ///////////////////////////////////////////////////////////
	protected static class HWContentHandler extends ONLComponent.ONLContentHandler
	{
		protected String tpname = null;
		protected String hwtpname = null;
		protected HardwareSpec.Subtype hwtype = null;
		private boolean is_rtr = false;
		protected Point position = null;
		protected int spinner = 0;

		public HWContentHandler(Hardware hwi, ExperimentXML expxml)
		{
			super(hwi, expxml);
			hwtype = hwi.hwType;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			super.startElement(uri, localName, qName, attributes);
			if (getCurrentElement().equals(ExperimentXML.POSITION))
			{
				position = new Point(Integer.parseInt(attributes.getValue(uri, ExperimentXML.XCOORD)), Integer.parseInt(attributes.getValue(uri, ExperimentXML.YCOORD)));
			}
			/*
      if (getCurrentElement().equals(ExperimentXML.HWTYPE))
	{
	  hwtpname = new String(attributes.getValue(uri, ExperimentXML.TYPENAME));
	  //is_rtr = Boolean.valueOf(attributes.getValue(uri, ExperimentXML.ROUTER)).booleanValue();
	}
			 */
			//Parse Reboot and Init commands
			if (getCurrentElement().equals(ExperimentXML.REBOOT))
			{
				Command cmd = ((Hardware)getComponent()).rebootCommand;
				expXML.setContentHandler(cmd.getContentHandler(expXML));
			}
			if (getCurrentElement().equals(ExperimentXML.INIT))
			{
				Command cmd = ((Hardware)getComponent()).initCommand;
				if (cmd == null) ExpCoordinator.print("HWContentHandler(" + getComponent().getLabel() + ").startElement cmd is null");
				expXML.setContentHandler(cmd.getContentHandler(expXML));
			}
			if (getCurrentElement().equals(ExperimentXML.SUBTYPE_INIT))
			{
				Command cmd = ((Hardware)getComponent()).subtypeInitCommand;
				expXML.setContentHandler(cmd.getContentHandler(expXML));
			}
			if (getCurrentElement().equals(ExperimentXML.FIELD))
			{
				ExpCoordinator.print(new String("Hardware(" + getComponent().getLabel() + ").HWContentHandler.startElement field fieldname:" + attributes.getValue(ExperimentXML.FIELDNAME)), ExperimentXML.TEST_XML);
				Field field = ((Hardware)getComponent()).getField(attributes.getValue(ExperimentXML.FIELDNAME));
				expXML.setContentHandler(field.getContentHandler(expXML));
			}
			if (getCurrentElement().equals(ExperimentXML.HWTABLE))
			{
				ExpCoordinator.print(new String("Hardware.HWContentHandler.startElement " + getCurrentElement() + " for " + getComponent().getLabel() + " adding table handler"), 3);
				HWTable table = (HWTable)((Hardware)getComponent()).getTable(attributes.getValue(ExperimentXML.TITLE));
				expXML.setContentHandler(table.getContentHandler(expXML));
			}
			if (getCurrentElement().equals(ExperimentXML.ROUTE_TABLE))
			{
				ExpCoordinator.print(new String("Hardware.HWContentHandler.startElement " + getCurrentElement() + " for " + getComponent().getLabel() + " adding route table handler"), 3);			
				//HWTable table = (HWTable)((Hardware)getComponent()).getTable(attributes.getValue(ExperimentXML.TITLE));
				HWTable table = (HWTable)((Hardware)getComponent()).getTable(attributes.getValue(ExperimentXML.TITLE), ONLComponent.RTABLE);
				if (table != null) expXML.setContentHandler(table.getContentHandler(expXML));
				else ExpCoordinator.print("table null", 3);
			}
			if (getCurrentElement().equals(ExperimentXML.PORT))
			{
				//initialized = true;
				((Hardware)getComponent()).initialize(null);
				((Hardware)getComponent()).initializePorts();
				ExpCoordinator.print(new String("Hardware.HWContentHandler.startElement " + getCurrentElement() + " for " + getComponent().getLabel() + " adding port handler"), 3);
				Hardware.Port port = ((Hardware)getComponent()).getPort(Integer.parseInt(attributes.getValue(ExperimentXML.INDEX)));
				if (attributes.getValue(ExperimentXML.SUBNET) != null)
				{		
					if (!ExpCoordinator.isOldSubnet())
					{
						try
						{
							SubnetManager.Subnet sn = SubnetManager.getSubnet(Integer.parseInt(attributes.getValue(ExperimentXML.SUBNET)));
							int sni = Integer.parseInt(attributes.getValue(ExperimentXML.SUBNET_INDEX));
							if (sn != null) 
							{
								sn.addPort(port, sni);
								ExpCoordinator.print(new String("    adding port(" + port.getLabel() + ") to subnet:" + sn.getIndex() + " index:" + sni), ExperimentXML.TEST_XML); 
							}
						} catch (SubnetManager.SubnetException e){ ExpCoordinator.print(new String("Hardware.HWContentHandler.startElement error:" + e.getMessage()));}
					}
					else //Old Subnet: 
					{
						if (attributes.getValue(ExperimentXML.SUBNET) != null)
						{
							try
							{
								OldSubnetManager.Subnet sn = OldSubnetManager.getSubnetFromSubIndex(Integer.parseInt(attributes.getValue(ExperimentXML.SUBNET)), Integer.parseInt(attributes.getValue(ExperimentXML.SUBNET_ID)));
								int sni = Integer.parseInt(attributes.getValue(ExperimentXML.SUBNET_INDEX));
								if (sn != null) 
								{
									sn.addPort(port, sni);
									ExpCoordinator.print(new String("    adding port to subnet:" + sn.getIndex() + "/" + sn.subIndex + " index:" + sni), ExperimentXML.TEST_XML); 
								}
							} catch (SubnetManager.SubnetException e){ ExpCoordinator.print(new String("Hardware.HWContentHandler.startElement error:" + e.getMessage()));}
						}
					}
				}
				expXML.setContentHandler(port.getContentHandler(expXML));
			}
		}
		public void characters(char[] ch, int start, int length)
		{
			super.characters(ch, start, length);
			if (getCurrentElement().equals(ExperimentXML.EXPADDR)) ((Hardware)component).setBaseIPAddr(new String(ch, start, length));
			if (getCurrentElement().equals(ExperimentXML.SPINNER)) spinner = Integer.parseInt(new String(ch, start, length));
			if (getCurrentElement().equals(ExperimentXML.CPADDR)) 
			{
				getComponent().setProperty(CPREQ, new String(ch, start, length));
				if (ExpCoordinator.isSPPMon()) ((Hardware)getComponent()).setCPHost(new String(ch, start, length));
			}
			if (getCurrentElement().equals(ExperimentXML.CPPORT)) 
				getComponent().setProperty(ExperimentXML.CPPORT, new String(ch, start, length));
			/*
      if (getCurrentElement().equals(ExperimentXML.FILE) || getCurrentElement().equals(ExperimentXML.RESOURCE)) 
	{
	  Hardware hw_comp = (Hardware)getComponent();
	  if (hw_comp.hwtype == null) 
	    {
	      hw_comp.setHWSubtype(HardwareSpec.readID(hwtpname, new String(ch, start, length)));
	      if (hw_comp.hwtype != null) 
		ExpCoordinator.print(new String("Hardware.HWContentHandler hwtype:" + hwtype.getTypeLabel()), HardwareBaseSpec.TEST_SUBTYPE);
	      else 
		ExpCoordinator.print("Hardware.HWContentHandler hwtype:null", HardwareBaseSpec.TEST_SUBTYPE);
	    }
	   else
	    {
	      HardwareSpec.Subtype hw_type = hwtype.readSubtypeID(tpname, is_rtr, new String(ch, start, length));
	      if (hw_type != null)
	      {
	      hw_comp.setProperty(ONL.VERSION_TOK, hw_type.getVersion());
	      hw_comp.setHWSubtype(hw_type);
	      }
	      }
	}
			 */
		}
		protected void initialize() 
		{ 
			((Hardware)getComponent()).initialize(null);
		}
		public void endElement(String uri, String localName, String qName)
		{
			super.endElement(uri, localName, qName);
			if (localName.equals(ExperimentXML.NODE))
			{
				//initialize();
				if (position != null) getComponent().getGraphic().setLocation(position);
				component.getGraphic().setSpinnerPosition(spinner);
				//update main route table if there is one
				//HWTable hwtable = (HWTable)((Hardware)getComponent()).getTableByType(ExperimentXML.ROUTE_TABLE);
				//if (hwtable != null && hwtable instanceof HWRouteTable)
				//{
					//((HWRouteTable)hwtable).setNextHops();
				//}
			}
			if (localName.equals(ExperimentXML.SUBHWTYPE))
			{
				if (hwtype != null)
				{ 
					((Hardware)getComponent()).setHWSubtype(hwtype);
					ExpCoordinator.print(new String("Hardware.HWContentHandler.endElement " + localName + " " + hwtype.getLabel()), HardwareBaseSpec.TEST_SUBTYPE);
				}
				else
					ExpCoordinator.print(new String("Hardware.HWContentHandler.endElement " + localName + " null"), HardwareBaseSpec.TEST_SUBTYPE);
				/*
	  if ((((Hardware)getComponent()).getHWSubtype() == null) && hwtype != null)
	    {
	      HardwareSpec.Subtype hw_type = hwtype.getDefaultSubtype();
	      getComponent().setProperty(ONL.VERSION_TOK, hw_type.getVersion());
	      ((Hardware)getComponent()).setHWSubtype(hw_type);
	    }
				 */
			}
		}
	}//end class Hardware.hwContentHandler

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////// Hardware.RebootAction ///////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class RebootAction extends Command.CAction
	{
		public RebootAction(Hardware hwi, Command cs) { super(hwi,cs);}
		protected void addOperation(Command cmd, Object[] vals) { ((Hardware)getONLComponent()).setRebootParams(vals);}
	}//end Hardware.RebootAction

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////// Hardware.InitAction //////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class InitAction extends Command.CAction
	{
		public InitAction(Hardware hwi, Command cs) 
		{ 
			super(hwi,cs);
			ExpCoordinator.print(new String("InitAction " + hwi.getLabel() + " spec:"), HardwareSpec.TEST_SUBTYPE);
			cs.print(HardwareSpec.TEST_SUBTYPE);
		}
		protected void addOperation(Command cmd, Object[] vals) { ((Hardware)getONLComponent()).setInitParams(vals);}
	}//end Hardware.InitAction


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////// Hardware.SubtypeInitAction //////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class SubtypeInitAction extends Command.CAction
	{
		public SubtypeInitAction(Hardware hwi, Command cs) 
		{ 
			super(hwi,cs);
			ExpCoordinator.print(new String("SubtypeInitAction " + hwi.getLabel() + " spec:"), HardwareSpec.TEST_SUBTYPE);
			cs.print(HardwareSpec.TEST_SUBTYPE);
		}
		protected void addOperation(Command cmd, Object[] vals) { ((Hardware)getONLComponent()).setSubtypeInitParams(vals);}
	}//end Hardware.SubtypeInitAction



	///////////////////////////////////////////////// Hardware methods //////////////////////////////////////////////////////////////////
	protected Hardware(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec, int n_ports) throws IOException
	{
		super(lbl, tp, tr, ec);
		//read in description of Hardware
		numPorts = n_ports;
		setBaseIPAddr(tr.readString()); 
		initialize(null);
		if (tr.getVersion() >= 2.5) 
		{
			String tmp_str = tr.readString();
			if (tmp_str.length() > 0)
				properties.setProperty(CPREQ, tmp_str);
		}
	}
	protected Hardware(String lbl, String tp, ExpCoordinator ec, int n_ports) { this(lbl, tp, (String)null, ec, n_ports);}
	protected Hardware(String lbl, String tp, String cp, ExpCoordinator ec, int n_ports)
	{
		super(lbl, tp, ec);
		numPorts = n_ports;
		initialize(cp);
		//ExpCoordinator.print(new String("Hardware numPorts=" + numPorts), 6);
	}

	public Hardware(String lbl, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{ this(lbl, "", tr, ec);}
	public Hardware(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{
		super(lbl, tp, tr, ec);
		//read in description of Hardware
		hwType = HardwareSpec.readSubtypeID(tr);
		String tmp_str = "null";
		if (hwType != null) 
		{
			tmp_str = hwType.getLabel();
			setType(tmp_str);
		}

		ExpCoordinator.print(new String("Hardware read hardware type " + tmp_str), 6);
		properties.setProperty(ONL.VERSION_TOK, hwType.getVersion());
		numPorts = hwType.getNumPorts();
		setBaseIPAddr(tr.readString()); 
		initialize(null);
		if (tr.getVersion() >= 2.5) 
		{
			tmp_str = tr.readString();
			if (tmp_str.length() > 0)
				properties.setProperty(CPREQ, tmp_str);
		}
	}

	public Hardware(int ndx, ExpCoordinator ec, HardwareSpec hw_type) { this(ndx, ec, hw_type.getDefaultSubtype());}
	public Hardware(int ndx, ExpCoordinator ec, HardwareSpec.Subtype hw_type) { this(new String(hw_type.getLabel() + "." + ndx), ec, hw_type);}
	public Hardware(String lbl, ExpCoordinator ec, HardwareSpec hw_type) { this(lbl, ec, hw_type.getDefaultSubtype());}
	public Hardware(String lbl, ExpCoordinator ec, HardwareSpec.Subtype hw_type){ this(lbl, ec, hw_type, hw_type.getNumPorts());}
	public Hardware(String lbl, ExpCoordinator ec, HardwareSpec.Subtype hw_type, int np)
	{ 
		super(lbl, hw_type.getTypeLabel(), ec);
		if (!hw_type.isHost())
			setComponentType(hw_type.getComponentType());
		else setComponentType(ExperimentXML.ENDPOINT);
		setProperty(ONL.VERSION_TOK, hw_type.getVersion());
		hwType = hw_type;
		numPorts = np;
		initialize(null);
	}
	public Hardware(String uri, Attributes attributes)
	{
		super(uri,attributes);
		hwType = HardwareSpec.manager.getSubtype(attributes.getValue(uri, ExperimentXML.TYPENAME));
		numPorts = Integer.parseInt(attributes.getValue(uri, ExperimentXML.NUMPORTS));
		ports = new Vector();
		//if (hwType != null) setHWSubtype(hwType);
	}
	protected void initialize(String cp)
	{ 
		boolean initialized = getPropertyBool(ONLComponent.INIT);
		if (initialized) return;
		else setProperty(ONLComponent.INIT, true);
		if (daemons == null) daemons = new Vector<ONLDaemon>();
		if (fields == null) fields = new Vector<Field>();
		if (assigners == null) assigners = new Vector();
		if (ports == null) ports = new Vector();
		if (actions == null) actions = new Vector();
		initializeMonitorable();
		if (cp != null && cp.length() > 0)
			setCPHost(cp); //this will also call initializeDaemons
		else
			initializeDaemons();
		setIndex();
		int max = 0;
		int i;
		if (hwType != null) 
		{
			addToDescription(ONLComponent.TYPE, hwType.getTypeLabel());
			addToDescription(ExperimentXML.INTERFACE_TYPE, hwType.getPortSpec().getInterfaceType());
			if (getIconColor() == null) setIconColor(hwType.getIconColor());
			if (getIconSize() == null)  setIconSize(hwType.getIconSize());
			Vector aspecs = hwType.getAssignerSpecs();
			max = aspecs.size();
			for (i = 0; i < max; ++i)
			{
				ComponentXMLHandler.AssignerSpec as = (ComponentXMLHandler.AssignerSpec)aspecs.elementAt(i);
				assigners.add(as.createAssigner());
			}
			Vector hw_flds = hwType.getFieldSpecs();
			max = hw_flds.size();
			for (i = 0; i < max; ++i)
			{
				FieldSpec fs = (FieldSpec)hw_flds.elementAt(i);
				fields.add(new Field(fs, this));
			}
		}
		initializeActions();
		addStateListener(new String[]{ CTL_PORT_STR, INDEX, CPHOST, IPBASEADDR});
		if (hwType != null) //not NSP
		{
			if (rebootCommand == null)
				{
					if (hwType.getRebootSpec() != null)
				       rebootCommand = new Command(hwType.getRebootSpec(), this);
			        else rebootCommand = null;
				}
			if (initCommand == null)
			{
				if (hwType.getInitSpec() != null)
					{
						ExpCoordinator.print(new String("Hardware(" + getLabel() + ").initialize init command:"), TEST_INITCMD);
						initCommand = new Command(hwType.getInitSpec(), this);
						initCommand.print(TEST_INITCMD);
					}
				else initCommand = null;
			}
			Vector<CommandSpec> cfgParams = hwType.getCfgCommands();
			//need to send cfg commands
			max = cfgParams.size();
			CommandSpec elem;
			Command cspec;
			for (i = 0; i < max; ++i)
			{
				elem = cfgParams.elementAt(i);
				cspec = getCommand(elem.getOpcode(), false);
				addOperation(new Command.NCCPOp(cspec, elem.getDefaultValues(), this));
			}
			ExpCoordinator.print(new String("Hardware.initialize hwType:" + hwType.getLabel()), HardwareBaseSpec.TEST_SUBTYPE);
			hwType.print(HardwareBaseSpec.TEST_SUBTYPE);
			if (rebootCommand != null)
			{
				ExpCoordinator.print("     reboot params:", HardwareBaseSpec.TEST_SUBTYPE);
				rebootCommand.print(HardwareBaseSpec.TEST_SUBTYPE);
			}
			if (initCommand != null)
			{
				ExpCoordinator.print("     init params:", HardwareBaseSpec.TEST_SUBTYPE);
				initCommand.print(HardwareBaseSpec.TEST_SUBTYPE);
			}
			Vector tbl_specs = hwType.getTableSpecs();
			max = tbl_specs.size();
			for (i = 0; i < max; ++i)
			{
				HWTable.Spec tcspec = (HWTable.Spec)tbl_specs.elementAt(i);
				addTable(tcspec.createTable(this));
			}
		}
	} 
	//public Hardware(String lbl, String cp, ExpCoordinator ec, HardwareSpec hw_type)
	//{
	// this(lbl, ec, hw_type);
	// setCPHost(cp);
	// ((NodeMonitor)monitorable).initializeDaemons(daemons);
	//initializePorts();
	//}  
	/*
  public boolean isRouter()
  {
    if (hwType != null) return (hwType.isRouter());
    else return false;
  }
	 */
	public void addDaemon(int tp, int port)
	{
		addDaemon(tp, port, expCoordinator.isDirectConn());
	}
	public void addDaemon(int tp, int port, boolean is_dir)
	{
		if (daemons != null)
		{
			String cp = getCPHost();
			if (cp == null) cp = "";
			MonitorDaemon md = (MonitorDaemon)getONLDaemon(tp);
			if (md != null) 
			{
				md.setHost(cp);
				md.setPort(port);
				ExpCoordinator.print(new String("Hardware.addDaemon for " + getLabel() + " setting host to " + cp), 4);
			}
			else
			{
				md = new MonitorDaemon(cp, port, tp, this, is_dir);
				daemons.add(md); 
				if (monitorable != null) ((NodeMonitor)monitorable).initializeDaemons(daemons);
				ExpCoordinator.print(new String("Hardware.addDaemon for " + getLabel() + " type:" + tp + " port:" + port), 4);
			}
			if (!md.isConnected()) md.connect();
		}
		else System.out.println("Hardware.addDaemon daemons is null");
	}
	protected ONLDaemon getONLDaemonByPort(int port)
	{
		if (daemons != null)
		{
			for(ONLDaemon dmn : daemons)
			{
				if (dmn.getPort() == port) return dmn;
			}
		}
		return null;
	}
	public String getType() 
	{
		if (hwType != null) return (hwType.getTypeLabel());
		else return (super.getType());
	}
	public String getBaseType()
	{
		if (hwType != null) return (hwType.getBaseTypeLabel());
		else return (super.getBaseType());
	}
	public IDAssigner.ID getNewID(FieldSpec.AssignerSpec as)
	{
		if (as.isHWAssigner())
		{
			IDAssigner assigner = getIDAssigner(as.getName());
			if (assigner != null) 
				return (assigner.getNewID());
			else return null;
		}
		else return(super.getNewID(as));
	}

	public IDAssigner getIDAssigner(String nm)
	{
		int max = assigners.size();
		for (int i = 0; i < max; ++i)
		{
			IDAssigner ias = (IDAssigner)assigners.elementAt(i);
			if (ias.getName().equals(nm)) return ias;
		}
		return null;
	}
	public ONLCTable getTable(String lbl) { return (getTable(lbl, ONLComponent.HWTABLE));}
	public ONLCTable getTable(String lbl, String tp)
	{
		ONLComponent c = getChild(lbl, tp);
		if (c != null && c instanceof ONLCTable) return ((ONLCTable)c);
		return null;
	}

	public ONLCTable getTableByType(String tp) //this supports old stuff should go away will have to use the label to distinguish tables
	{
		ONLComponent rtn = getChild(tp);
		if (rtn != null && rtn instanceof ONLCTable) return ((ONLCTable)rtn);
		return null;
	}

	public void addTable(ONLCTable table)
	{
		if (table.getID() < 0)
		{
			table.setID(getNewTableID());
		}
		actions.add(table.getAction());
		addChild(table);
	}
	public void removeTable(ONLCTable table) 
	{
		actions.remove(table.getAction());
		removeChild(table);
	}
	private int getNewTableID()
	{
		ONLComponentList clist = getChildren();
		ONLComponent celem;
		int max = clist.size();
		int min_id = 1;
		for (int i = 0; i < max; ++i)
		{
			celem = clist.onlComponentAt(i);
			if (celem instanceof ONLCTable)
				if (((ONLCTable)celem).getID() >= min_id) min_id = ((ONLCTable)celem).getID() + 1;
		}
		return min_id;
	}
	public void initializePorts()
	{
		//ExpCoordinator.print("Hardware.initializePorts", 5);
		boolean there = (ports.size() >= numPorts);
		Port tmp_port;
		//Vector tmp_specs = hwType.getTableSpecs();
		//Vector tbl_specs = new Vector();
		int max = 0;//tmp_specs.size();
		int i;
		//for (i = 0; i < max; ++i)
		//{
		//HWTable.Spec tcspec = (HWTable.Spec)tmp_specs.elementAt(i);
		//if (tcspec.isPerPort()) tbl_specs.add(tcspec);
		//}
		//int num_tbls = tbl_specs.size();

		for (i = 0; i < numPorts; ++i)
		{
			if (there)
			{
				tmp_port = getPort(i);
				/*//SUBNET:remove
	      if (tmp_port != null) 
	         tmp_port.setIPAddr(i, getBaseIPAddr());
				 */
			}
			else
			{
				tmp_port = createPort(i);
				//tmp_port.initializeMenus();
				/*for (int j = 0; j < num_tbls; ++j)
	      {
		tmp_port.addTable(((HWTable.Spec)tbl_specs.elementAt(j)).createTable(tmp_port));
		}*/
				//SUBNET:remove//if (getBaseIPAddr() != null)  tmp_port.setIPAddr(i, getBaseIPAddr());
				ports.add(tmp_port);
				addChild(tmp_port);
			}
		}
		if (!there) setProperty(ONLComponent.PORTS_INIT, true);
		if (ExpCoordinator.isOldSubnet() && getBaseIPAddr() != null) setBaseIPAddr(getBaseIPAddr());
	}
	public void writeExpDescription(ONL.Writer tw)  throws IOException
	{
		super.writeExpDescription(tw);
		if (hwType != null) hwType.getHardwareType().writeID(tw);
		tw.writeString(getBaseIPAddr());
		tw.writeString(properties.getProperty(CPREQ));
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeAttribute(ExperimentXML.NUMPORTS, String.valueOf(numPorts));
		super.writeXML(xmlWrtr);
		if (hwType != null)  hwType.writeXML(xmlWrtr); //CHANGE in SCHEMA
		graphic.writeXML(xmlWrtr);
		xmlWrtr.writeStartElement(ExperimentXML.EXPADDR);
		xmlWrtr.writeCharacters(getIPAddrString());
		xmlWrtr.writeEndElement(); //end expaddr
		if (getCPAddr() != null)
		{
			if (ExpCoordinator.isSPPMon())
			{
				xmlWrtr.writeStartElement(ExperimentXML.CPPORT);
				xmlWrtr.writeCharacters(getProperty(ExperimentXML.CPPORT));
				xmlWrtr.writeEndElement(); //end cpport
			}
			xmlWrtr.writeStartElement(ExperimentXML.CPADDR);
			xmlWrtr.writeCharacters(getCPAddr());
			xmlWrtr.writeEndElement(); //end cpaddr
		}
		writeXMLParams(xmlWrtr);
		writeXMLPorts(xmlWrtr);
	}

	public void writeXMLParams(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		if (rebootCommand != null) rebootCommand.writeXML(xmlWrtr);
		if (initCommand != null) initCommand.writeXML(xmlWrtr);
		xmlWrtr.writeStartElement(ExperimentXML.FIELDS);
		int max = fields.size();
		int i;
		for (i = 0; i < max; ++i)
		{
			((Field)fields.elementAt(i)).writeXML(xmlWrtr);
		}
		xmlWrtr.writeEndElement();//end fields
		xmlWrtr.writeStartElement(ExperimentXML.TABLES);
		Vector tables = getHWTables();
		max = tables.size();
		for (i = 0; i < max; ++i)
		{
			((HWTable)tables.elementAt(i)).writeXML(xmlWrtr);
		}
		xmlWrtr.writeEndElement();//end tables

	}

	public void writeXMLPorts(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.PORTS);
		for (int i = 0; i < numPorts; ++i)
		{
			xmlWrtr.writeStartElement(ExperimentXML.PORT);
			((Port)ports.elementAt(i)).writeXML(xmlWrtr);
			xmlWrtr.writeEndElement();//end ports
		}
		xmlWrtr.writeEndElement();//end ports
	}

	public String getCPAddr() { return (properties.getProperty(CPREQ));}
	public void initializeDaemons()
	{
		ExpCoordinator.print(new String("Hardware.initializeDaemons hwType:" + hwType), 5);
		if (hwType != null && hwType.hasDaemon())
		{
			int ctl_port = getPropertyInt(CTL_PORT_STR);
			ExpCoordinator.print(new String("   adding daemon " + ctl_port), 5);
			//if (ctl_port > 0)
			addDaemon(ONLDaemon.HWC, ctl_port);
		}
	}
	public void initializeMonitorable() //override to create and initialize monitorable if there is one otw make empty
	{
		if (monitorable == null) 
		{
			ExpCoordinator.print(new String("Hardware.initializeMonitorable " + daemons.size() + " daemons" ), 5);
			monitorable = new HWMonitor(daemons, this, expCoordinator.getMonitorManager());
		}
	}
	protected Hardware.Port createPort(int ndx)
	{
		Port rtn = new Port(this, ndx, expCoordinator);
		ExpCoordinator.print(new String("Hardware(" + getLabel() + ").createPort(" + ndx + ")"), TEST_PORT);
		if (hwType != null && ndx == hwType.getCPPort()) rtn.setLinkable(false);
		return rtn;
	}
	public CommandSpec getSpec(int op_code, boolean is_mon)
	{
		return ((CommandSpec)getCommand(op_code, is_mon));
		/*
      if (hwType == null) 
      {
      ExpCoordinator.print(new String("Hardware.getSpec op:" + op_code + " is_mon:" + is_mon + " hwType null"), 6);
      return null;
      }
      else return (hwType.getSpec(op_code, is_mon));
		 */
	}
	public void setRebootParams(Object[] vals) { rebootCommand.setCurrentValues(vals);}
	public void writeRebootParams(ONL.Writer wrtr) throws java.io.IOException
	{
		Object[] rebootParams = null;
		//if (hwType != null) rebootParams = hwType.getRebootDefaults();
		if (rebootCommand != null) rebootParams = rebootCommand.getCurrentValues();
		if (rebootParams != null)
		{
			int max = Array.getLength(rebootParams);
			ExpCoordinator.print(new String("Hardware." + getLabel() + ".writeRebootParams numParams = " + max), 3);
			wrtr.writeInt(max);
			Object o;
			for (int i = 0; i < max; ++i)
			{
				o = rebootParams[i];
				if (o instanceof Integer || o instanceof ParamSpec.WCInteger) wrtr.writeShort(CommandSpec.INT);
				if (o instanceof Double || o instanceof ParamSpec.WCDouble) wrtr.writeShort(CommandSpec.DOUBLE);
				if (o instanceof Boolean) wrtr.writeShort(CommandSpec.BOOL);
				if (o instanceof String) wrtr.writeShort(CommandSpec.STRING);
				ExpCoordinator.print(new String("     rebootParams[" + i + "] = " + o.toString()), 3);
				wrtr.writeString(o.toString());
			}
		}
		else wrtr.writeInt(0);
	}
	public void setSubtypeInitParams(Object[] vals) { subtypeInitCommand.setCurrentValues(vals);}

	protected void setOriginalSubnet(SubnetManager.Subnet osn) 
	{ 
		SubnetManager.Subnet old_osn = getOriginalSubnet();
		if (old_osn == null && osn != null)
		{
			super.setOriginalSubnet(osn);
			if (isSwitch()) osn.setSwitch(this);
		}
	}
	public NCCPOpManager.Operation getSubtypeInitOp() 
	{
		Object[] subtypeInitParams = null;
		//if (hwType != null) rebootParams = hwType.getRebootDefaults();
		if (subtypeInitCommand != null) subtypeInitParams = subtypeInitCommand.getCurrentValues();
		if (subtypeInitParams != null)
		{
			return (subtypeInitCommand.createNCCPOp(subtypeInitParams));
		}
		else return null;
	}
	public void setInitParams(Object[] vals) { initCommand.setDefaults(vals);}    
	public void writeInitParams(ONL.Writer wrtr) throws java.io.IOException
	{
		ExpCoordinator.print(new String("Hardware." + getLabel() + ".writeInitParams"), 3);
		Object[] initParams = null;
		if (initCommand != null) initParams = initCommand.getCurrentValues();
		if (initParams != null)
		{
			int max = Array.getLength(initParams);
			wrtr.writeInt(max);
			ExpCoordinator.print(new String("     numParams = " + max), 3);
			Object o;
			for (int i = 0; i < max; ++i)
			{
				o = initParams[i];
				if (o instanceof Integer || o instanceof ParamSpec.WCInteger) wrtr.writeShort(CommandSpec.INT);
				if (o instanceof Double || o instanceof ParamSpec.WCDouble) wrtr.writeShort(CommandSpec.DOUBLE);
				if (o instanceof Boolean) wrtr.writeShort(CommandSpec.BOOL);
				if (o instanceof String) wrtr.writeShort(CommandSpec.STRING);
				ExpCoordinator.print(new String("     initParams[" + i + "] = " + o.toString()), 3);
				wrtr.writeString(o.toString());
			}
		}
		else wrtr.writeInt(0);
	}
	public void readParams(ONL.Reader reader) throws IOException
	{ 
		String cp_str = reader.readString();
		int ctl_port = reader.readInt();
		setCPAddr(cp_str, ctl_port);
		ExpCoordinator.print(new String("Hardware.readParams cp is " + cp_str), 3);
		returnIndex = 0; //reader.readInt();
		ExpCoordinator.print(new String("Hardware returnIndex = " + returnIndex), 5);
		setProperty(RETURN_NDX_STR, returnIndex);
		String vm_str = reader.readString().trim();
		setProperty(VMNAME, vm_str);
		if (vm_str.length() > 0)
			addToDescription(VMNAME, new String("VM host: " + vm_str));
	}

	public void writeParams(ONL.Writer writer) throws IOException
	{ 
		writer.writeInt(getPropertyInt(CTL_PORT_STR));
		writer.writeString(getCPHost());
		writer.writeInt(returnIndex);
	}

	protected void initializeActions()
	{
		if (hwType != null)
			ExpCoordinator.print(new String("Hardware.initializeActions hwType = " + hwType.getLabel()), 5);
		if (hwType != null)
		{
			initializeDaemons();
			initializeMonitorable();
		}
		actions.add(new UserLabelAction(this));
		if (ExpCoordinator.isSPPMon()) 
		{
			actions.add(new LabelAction(this));
			actions.add(new SPPMonCPAction(this));
		}
		if (getExpCoordinator().isManagement())
		{
			if (buttonAction == null) buttonAction = new ButtonAction(this);
			if (!ExpCoordinator.isSPPMon()) actions.add(new CPAction(this));
		} 

		if (hwType != null && rebootCommand == null && hwType.getRebootSpec() != null) rebootCommand = new Command(hwType.getRebootSpec(), this);
		if (rebootCommand != null) 
		{
			actions.add(new RebootAction(this, rebootCommand));
		}
		if (hwType != null && initCommand == null && hwType.getInitSpec() != null) 
		{
			ExpCoordinator.print(new String("Hardware(" + getLabel() + ").initializeActions init command:"), TEST_INITCMD);
			initCommand = new Command(hwType.getInitSpec(), this);
			initCommand.print(TEST_INITCMD);
		}
		if (initCommand != null) 
		{
			actions.add(new InitAction(this, initCommand));
		}
		if (hwType != null && subtypeInitCommand == null && hwType.getSubtypeInitSpec() != null) subtypeInitCommand = new Command(hwType.getSubtypeInitSpec(), this);
		if (subtypeInitCommand != null) 
		{
			actions.add(new SubtypeInitAction(this, subtypeInitCommand));
		}
		if (fields != null)
		{
			int max = fields.size();
			Field elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (Field)fields.elementAt(i);
				Action ua = elem.getUpdateAction();
				if (ua != null) actions.add(ua);
			}
		}
		if (hwType != null)
		{
			ExpCoordinator.print(new String("Hardware.initializeActions " + getLabel() + " subtype "), HardwareBaseSpec.TEST_SUBTYPE);
			hwType.print(HardwareBaseSpec.TEST_SUBTYPE);
			Vector cmds = hwType.getCommandSpecs();
			if (cmds.isEmpty()) 
			{
				ExpCoordinator.print("     no commands to add", HardwareBaseSpec.TEST_SUBTYPE);
				return;
			}
			int max = cmds.size();
			ExpCoordinator.print(new String("     " + max + " commands to add"), HardwareBaseSpec.TEST_SUBTYPE);
			for (int i = 0; i < max; ++i)
			{
				Command cmd = new Command((CommandSpec)cmds.elementAt(i), this);
				actions.add(new Command.CAction(this, cmd));
			}
		}
	}
	public double getVersion() { return (hwType.getVersion());}
	public HardwareBaseSpec.PortSpec getPortSpec()
	{
		if (hwType != null) return (hwType.getPortSpec());
		return null;
	}
	public HardwareSpec getHWType() 
	{
		if (hwType != null)
			return (hwType.getHardwareType());
		else return null;
	}
	public HardwareSpec.Subtype getHWSubtype() { return hwType;}
	public HardwareSpec.Subtype getHWSpec() { return hwType;}
	protected void setHWSubtype(HardwareSpec.Subtype stp) 
	{ 
		hwType = stp;
		ExpCoordinator.print(new String("Hardware.setHWSubtype hwType:" + hwType.getLabel()), HardwareBaseSpec.TEST_SUBTYPE);
		initialize(null);
	}
	protected VirtualTopology.VNodeSubtype getVirtualType() 
	{ 
		if (hwType != null) return (hwType.getVirtualType());
		else return null;
	}
	public int getReturnIndex() { return returnIndex;}
	public Vector getActions() { return actions;}
	public Field getField(String str)
	{
		int max = fields.size();
		Field elem;
		ExpCoordinator.print(new String("Hardware(" + getLabel() + ").getField(" + str + ")  num_fields:" + max), TEST_LOADING);
		for (int i = 0; i < max; ++i)
		{
			elem = (Field)fields.elementAt(i);
			ExpCoordinator.print(new String("    " + elem.getLabel()), TEST_LOADING);
			if (elem.getLabel().equals(str)) return elem;
		}
		return null;
	}
	protected void setIndex() //router name should be some label followed by an index
	{
		String tmp = getLabel();
		int i = tmp.indexOf(".");
		if (i >= 0) tmp = tmp.substring(i);
		String ndx = tmp.replaceAll("[^0-9]", "");
		ExpCoordinator.print(new String("Hardware(" +  tmp + ").setIndex " + ndx), 2);
		if (ndx.length() > 0) setProperty(INDEX, ndx);
		// }
	}
	public ONLGraphic getGraphic()
	{
		if (graphic == null)
		{
			//graphic = new UserLabelGraphic(new HardwareGraphic(this, new Color(127,211,239)));
			//initializeGraphic((HardwareGraphic)((UserLabelGraphic)graphic).getMainGraphic());
			graphic = new HardwareGraphic(this, new Color(127,211,239));
			initializeGraphic((HardwareGraphic)graphic);
		}
		return (graphic);
	}
	protected void initializeGraphic(HardwareGraphic rtn)
	{
		for (int i = 0; i < numPorts; ++i)
		{
			((Hardware.Port)ports.elementAt(i)).addGraphic(rtn);
		}
		//add any node listeners here
		rtn.addNodeListener(new ButtonAction(this));
		
		Color bg_color = getIconColor();
		//set background color
		if (bg_color == null)
		{
		    if (isType(ONLComponent.NPR_LBL))
			bg_color = new Color(127,239,167);//rtn.setBackground(new Color(127,239,167));
		    else
			bg_color = new Color(213,119,239);//rtn.setBackground(new Color(213,119,239));
		}
		if (bg_color != null) rtn.setBackground(bg_color);
		Dimension g_sz = getIconSize();
		if (g_sz == null)
		{
			g_sz = new Dimension(82,82);
		}
		if (g_sz != null) rtn.setSize(g_sz);
	}

	protected JFrame getFrame() { return frame;}
	public void showDesktop(JInternalFrame f)
	{
		showDesktop();
		try
		{
			Component[] comps = desktop.getComponents();
			boolean found = false;
			int max = comps.length;
			for (int i = 0; i < max; ++i)
			{
				if (comps[i] == f) 
				{
					found = true;
					break;
				}
			}
			if (!found) addToDesktop(f);
			f.setClosed(false);
			f.setSelected(true);
			f.setVisible(true);
			f.toFront();
		}
		catch(java.beans.PropertyVetoException e){}
		desktop.setSelectedFrame(f);
	}
	public void showDesktop()
	{
		if (desktop == null)
		{
			frame = new JFrame(getLabel());
			desktop = new JDesktopPane();
			this.desktopListener = new DesktopListener();
			frame.setContentPane(desktop);
			frame.setSize(700,300);
			JMenuBar mb = new JMenuBar();
			frame.setJMenuBar(mb);
			JMenu menu = new JMenu("Operations");
			int max = actions.size();
			Action elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (Action)actions.elementAt(i);
				menu.add(new JMenuItem(elem));
			}
			mb.add(menu);
		}
		if ((frame.getExtendedState() & Frame.ICONIFIED) != 0)
			frame.setExtendedState(frame.getExtendedState() - Frame.ICONIFIED);
		//frame.show(); //show deprecated 1.5
		frame.setVisible(true);  //replacement 1.5
	}
	public void addToDesktop(JInternalFrame f)
	{
		showDesktop();
		f.setIconifiable(true); 
		f.setResizable(true);
		f.addMouseListener(desktopListener);
		f.addInternalFrameListener(desktopListener);
		if (expCoordinator.getDebugLevel() > 0) f.setClosable(true);
		desktop.add(f);
		desktop.setSelectedFrame(f);
	}
	public int getNumPorts() { return numPorts;}
	public int getCores() 
	{
		if (hwType != null) return (hwType.getCores());
		else return 1;
	}
	public int getMemory() 
	{
		if (hwType != null) return (hwType.getMemory());
		else return 1;
	}

	public ContentHandler getContentHandler(ExperimentXML expXML)
	{
		return (new HWContentHandler(this, expXML));
	}

	//public ContentHandler getPortContentHandler(ExperimentXML expXML) { return (new Port.HWContentHandler(expXML, this));}

	public Hardware.Port getPort(int p)
	{
		int num_ports = ports.size();
		//ExpCoordinator.print(new String("Hardware.getPort numPorts = " + num_ports + " p = " + p), 5);
		if ((p >= 0) && (p < num_ports))
			return ((Hardware.Port)ports.elementAt(p));
		else return null;
	}
	protected Vector<ONLComponent.PortBase> getPorts() { return ports;}
	protected Vector<ONLComponent.PortBase> getConnectedPorts()  //used by OldSubnetManager to get all of the ports connected through a switch
	{ 
		if (!isSwitch()) return null;
		Vector<ONLComponent.PortBase> rtn = new Vector<ONLComponent.PortBase>();
		int max = getNumPorts();
		Hardware.Port p;
		ONLComponent.PortBase lnkto;
		for (int i = 0; i < max; ++i)
		{
			p = getPort(i);
			rtn.add(p);
			lnkto = (ONLComponent.PortBase)p.getLinkedTo();
			if (lnkto != null) 
			{
				rtn.add(lnkto);
				/*
				Vector<ONLComponent.PortBase> lnkto_conn = lnkto.getConnectedPorts();
				if (lnkto_conn != null && !lnkto_conn.isEmpty()) rtn.addAll(lnkto_conn);
				*/
			}
		}
		return rtn;
	}
	//protected boolean isParent(int tp) { return (tp == (getType() + ONLComponent.PORT));}
	protected ONLDaemon getONLDaemon(int tp)
	{
		int max = daemons.size();
		ONLDaemon elem;
		ExpCoordinator.print(new String("Hardware.getONLDaemon " + tp + " daemons.size=" + max), 4);
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLDaemon)daemons.elementAt(i);

			ExpCoordinator.print(new String("  element" + i + " is type " + elem.getDaemonType()), 4);
			if (elem.isType(tp)) return elem;
		}
		elem = null;
		return elem;
	}

	public void setProperty(String nm, String val) 
	{ 
		if (val != null && !(val.equals(properties.getProperty(nm))))
		{
			if (nm.equals(CPHOST)) setCPHost(val);
			else
			{
				if (nm.equals(IPBASEADDR)) setBaseIPAddr(val);
				else
					super.setProperty(nm, val);
			}
		}
	}
	protected void setCPAddr(String cp, int p) 
	{ 
		setProperty(CTL_PORT_STR, p);
		setCPHost(cp);
	}
	protected void setCPHost(String cp)
	{
		if (getCPHost() == null && cp != null && cp.length() > 0)
		{
			ExpCoordinator.print(new String("Hardware.setCPHost CP host: " + cp), 0);
			properties.setProperty(CPHOST, cp);
			initializeDaemons();
			addToDescription(CPHOST, new String("CP host: " + cp));
		}
		else
		{
			if ((getCPHost() != null && cp == null) || (cp != null && cp.length() == 0))
			{
				ExpCoordinator.print("Hardware.setCPHost clearing CP host", 0);
				properties.remove(CPHOST);//setProperty(CPHOST, null);
			}
		}
	}
	protected void addToDescription(String key, String d) 
	{
		super.addToDescription(key, d);
		if (menu != null) menu.addDescription(getDescription());
	}
	protected void setBaseIPAddr(String ipaddr)
	{
		ExpCoordinator.print(new String("Hardware.setBaseIPAddr = " + ipaddr), 2);
		properties.setProperty(IPBASEADDR, ipaddr);
		//addToDescription(IPBASEADDR, new String("IP Base Address: " + ipaddr));
		int max = 0;
		if (ports != null) 
		{
			max = ports.size();
			if (ExpCoordinator.isOldSubnet())
			{
				Port tmp_port;
				OldSubnetManager.Subnet sn;
				String[] strarray = ipaddr.split("\\.");
				int ndx = Integer.parseInt(strarray[2]);
				for (int i = 0; i < max; ++i)
				{
					tmp_port = getPort(i);
					sn = OldSubnetManager.getSubnet(ndx, i);
					try{
						sn.addPort(tmp_port);
					}catch (SubnetManager.SubnetException e)
					{
						ExpCoordinator.print(new String("Hardware(" + getLabel() + ").setBaseIPAddr " + ipaddr + " error:" + e.getMessage()));
					}
					sn.setOwner(tmp_port);
				}
			}
		}
	}
	public String getCPHost() { return (properties.getProperty(CPHOST));}
	public String getBaseIPAddr() { return (properties.getPropertyString(IPBASEADDR));}
	public int getPortBandwidth()
	{
		int m = 1;
		String interfaceType = getInterfaceType();
		
		if (interfaceType.endsWith("G")) m = 1000;
	    int end = interfaceType.length() - 1;
		int rtn = Integer.parseInt(interfaceType.substring(0,end));
		rtn = rtn * m;
		return rtn;
	}
	public String getIPAddrString() { return (getBaseIPAddr());}
	protected void initializeMenu()
	{
		if (menu == null)
		{
			menu = new ONLComponent.CfgMonPopupMenu(this);
			menu.addDescription(getDescription());
			Vector acts = getActions();
			if (acts != null)
			{
				int max = acts.size();
				for (int i = 0; i < max; ++i)
				{
					//add Configuration actions
					menu.addCfg((Action)acts.elementAt(i));
					//add Monitoring actions
				}
			}
			if (monitorable != null) monitorable.addActions(menu);
		}
	}
	protected void showMenu(JComponent c, Point2D pnt)
	{
		if (menu == null)
		{
			initializeMenu();
		}
		menu.showMenu(c, pnt);
	}
	public void clear()
	{
		setState(ONLComponent.CLEARED); //JP added 3_21_2012
		super.clear();
		//clear all tables //should probably be calling clear on all children
		ONLComponentList childlist = getChildren();
		int max = childlist.size();
		ONLComponent elem;
		for (int i = 0; i < max; ++i)
		{
			elem = childlist.onlComponentAt(i);
			if (elem instanceof ONLCTable) ((ONLCTable)elem).clear();
		}
		if (frame != null ) 
		{
			desktop.removeAll();
			desktop = null;
			frame.dispose();
			frame = null;
		}
		Hardware.Port tmp_port;
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (Hardware.Port)ports.elementAt(i);
			tmp_port.clear();
		}
		setCPHost(null);
		//setState(ONLComponent.CLEARED); //JP added 3_21_2012
		clearDaemons();
	}

	public void processEvent(ONLComponent.Event e)
	{
		ExpCoordinator.print(new String(getLabel() + ".Hardware.processEvent type " + e.getType()), 6);
		if (e instanceof ONLComponent.PropertyEvent)
		{
			PropertyEvent pe = (PropertyEvent)e;
			if (pe.getLabel().equals(CPHOST))
			{
				setCPHost(pe.getNewValue());
				return;
			}
			if (pe.getLabel().equals(IPBASEADDR))
			{
				setBaseIPAddr(pe.getNewValue());
				return;
			}
		}
		super.processEvent(e); //all other non STATE properties will be set if they weren't handled above
	} 

	//NCCPOpManager.Manager
	public void addOperation(NCCPOpManager.Operation op) 
	{
		if (opManager == null) getOpManager();
		opManager.addOperation(op);
	}
	public void removeOperation(NCCPOpManager.Operation op) { if (opManager != null) opManager.removeOperation(op);}
	//end NCCPOpManager.Manager
	public NCCPOpManager getOpManager()
	{ 
		if (opManager == null) opManager = new OpManager((MonitorDaemon)getONLDaemon(ONLDaemon.HWC));
		return opManager;
	}
	/*
    public NCCPOpManager getListOpManager()
    { 
      if (listOpManager == null) listOpManager = new FieldListOpManager((MonitorDaemon)getONLDaemon(ONLDaemon.HWC));
      return listOpManager;
    }
	 */
	public void setOpManager(NCCPOpManager opman) { opManager = opman;}
	/*//SUBNET:remove
    public int getNumInterfaces(Vector v) 
    {
      if (v.contains(this)) return 0;
      int rtn = 0;
      Port pelem;
      v.add(this);
      for (int i = 0; i < numPorts; ++i)
	{
	  pelem = (Port)ports.elementAt(i);
	  if (pelem.isLinked() && !(v.contains(pelem.getLinkedTo()))) 
	    {
	      rtn += pelem.getLinkedTo().getNumInterfaces(v);
	    }
		    else rtn += 1;
	}
      return rtn;
    }
	 */
	/*//SUBNET:remove
  public void setSubNetRouter(ONLComponent.PortInterface c)
    {
      if (!isRouter() && c != getSubNetRouter())
	{
	  super.setSubNetRouter(c);
	  for (int i = 0; i < numPorts; ++i)
	    {
	      ((Port)ports.elementAt(i)).setSubNetRouter(c);
	    }
	}
	}*/

	public void addRoute(String ip, int mask, int nhp, ONL.IPAddress nhip)
	{
		Hardware.Port tmp_port;
		HWRouteTable rt = null;
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (Hardware.Port)ports.elementAt(i);
			tmp_port.addRoute(ip, mask, nhp, nhip);
		}
	}
	public void addGeneratedRoute(String ip, int mask, int nhp, ONL.IPAddress nhip)
	{
		HWRouteTable rt = (HWRouteTable)getTableByType(ONLComponent.RTABLE);
		if (rt != null) 
		{
			ExpCoordinator.print(new String("Hardware(" + getLabel() + ").addRoute " + ip + "/" + mask + " " + nhp + ":" + nhip.toString()), Topology.TEST_DEFRTS);
			rt.addGeneratedRoute(ip, mask, nhp, nhip);
		}
		else
		{
			Hardware.Port tmp_port;
			for (int i = 0; i < numPorts; ++i)
			{
				tmp_port = (Hardware.Port)ports.elementAt(i);
				tmp_port.addGeneratedRoute(ip, mask, nhp, nhip);
			}
		}
	}
	/*
	public void addRoute(String ip, int mask, NextHop nh)
	{
		Hardware.Port tmp_port;
		HWRouteTable rt = null;
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (Hardware.Port)ports.elementAt(i);
			tmp_port.addRoute(ip, mask, nh);
		}
	}
	*/
	public void generateDefaultRts()
	{
		Hardware.Port tmp_port;
		HWRouteTable rt = null;
		ExpCoordinator.print(new String("Hardware(" + getLabel() + ").generateDefaultRts"), TEST_SUBNET);
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (Hardware.Port)ports.elementAt(i);
			SubnetManager.Subnet sn = tmp_port.getSubnet();
			if (sn != null)
			{
				ONL.IPAddress nhip;
				ONLComponent nh = tmp_port.getLinkedTo();
				if (nh != null && nh.isRouter()) nhip = new ONL.IPAddress(nh.getIPAddr());
				else nhip = new ONL.IPAddress();
				addGeneratedRoute(sn.getBaseIP().toString(), sn.getNetmask().getBitlen(), i, nhip);
			}
		}
	}
	public void removeGeneratedRoutes()
	{
		Hardware.Port tmp_port;
		ExpCoordinator.print(new String("Hardware(" + getLabel() + ").removeGeneratedRoutes"), TEST_SUBNET);
		HWRouteTable rt = (HWRouteTable)getTableByType(ONLComponent.RTABLE);
		if (rt != null) 
		{
			ExpCoordinator.print(new String("Hardware(" + getLabel() + ").removeGeneratedRoutes removing from main table"), TEST_SUBNET);
			rt.removeGeneratedElements();
		}
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (Hardware.Port)ports.elementAt(i);
			tmp_port.removeGeneratedRoutes();
		}
	}
	public void clearRoutes()
	{
		Hardware.Port tmp_port;
		ExpCoordinator.print(new String("Hardware(" + getLabel() + ").removeGeneratedRoutes"), TEST_SUBNET);
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (Hardware.Port)ports.elementAt(i);
			tmp_port.clearRoutes();
		}
	}
	
	protected boolean merge(ONLComponent c)
	{
		if (c instanceof Hardware)
		{
			Hardware hwi2 = (Hardware)c;
			if (hwi2.numPorts != numPorts)
			{
				expCoordinator.addError(new StatusDisplay.Error((new String("Hardware Error: Number of ports not equal master " + getLabel() + " has " + numPorts + " ports other has " + hwi2.numPorts + " ports.\n")),
						"Hardware Error",
						"Hardware Error",
						StatusDisplay.Error.LOG)); 
				return false;
			}
			else 
			{
				//should check reboot and init Specs
				if (!mergeParams(c)) return false;
				//for each port do a merge
				Port pelem1;
				Port pelem2;
				for (int i = 0; i < numPorts; ++i)
				{
					pelem1 = (Port)ports.elementAt(i);
					pelem2 = (Port)hwi2.ports.elementAt(i);
					if (!pelem1.merge(pelem2)) return false;
				}
				return true;
			}
		}
		else return false;
	}
	protected boolean mergeParams(ONLComponent c) //should check reboot and init Specs
	{ 
		Hardware hw = (Hardware)c;
		//merge fields
		int max = fields.size();
		for (int i = 0; i < max; ++i)
		{
			Field f1 = fields.elementAt(i);
			Field f2 = hw.getField(f1.getLabel());
			if (f2 != null && !f1.isEqual(f2))
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String(getLabel() + " " + f1.getLabel() + " Inconsistency master has " + f1.getLabel() + ":" + f1.getCurrentValue().toString() + " other has " + f1.getLabel() + ":" + f2.getCurrentValue().toString() + "\n"),
						"Hardware Field Error",
						"Hardware Field Error",
						StatusDisplay.Error.LOG)); 
		}
		//merge reboot and init specs
		if (rebootCommand != null && !rebootCommand.merge(hw.rebootCommand)) return false;
		else if (rebootCommand == null && hw.rebootCommand != null)
		{
			ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String(getLabel() + " rebootParams Inconsistency master rebootParams:none other has rebootParams:" + hw.rebootCommand.toString() + "\n"),
					"Hardware Reboot Error",
					"Hardware Reboot Error",
					StatusDisplay.Error.LOG)); 		
		}
		if (initCommand != null && !initCommand.merge(hw.initCommand)) return false;
		else if (initCommand == null && hw.initCommand != null)
		{
			ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String(getLabel() + " initParams Inconsistency master rebootParams:none other has initParams:" + hw.initCommand.toString() + "\n"),
					"Hardware Init Error",
					"Hardware Init Error",
					StatusDisplay.Error.LOG)); 		
		}
		//merge tables
		max = getNumChildren();
		for (int i = 0 ; i < max; ++i)
		{
			ONLComponent child = getChildAt(i);
			if (child instanceof HWTable)
			{
				HWTable hwt = (HWTable)hw.getTable(child.getLabel());
				if (hwt == null || !child.merge(hwt)) return false;
			}
		}
		return true;
	}

	public Command getCommand(int monitor_id, boolean is_mon)
	{
		if (is_mon) return (((HWMonitor)monitorable).getCommand(monitor_id));
		int max = actions.size();
		Command.CAction ca;
		for (int i = 0; i < max; ++i)
		{
			ca = (Command.CAction)actions.elementAt(i);
			if (ca.getCommand().getOpcode() == monitor_id) return (ca.getCommand());
		}
		return null;
	}
	public String getInterfaceType() 
	{ 
		if (hwType != null) return (hwType.getPortSpec().getInterfaceType());
		else return (super.getInterfaceType());
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////// Monitoring Classes ////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////// Hardware.NCCP_Request /////////////////////////////////////////////////////////////////


	public static class NCCP_Request extends NCCP.RequesterBase
	{
		protected int port = 0;
		private Object[] values = null;
		protected int numParams = 0;
		private double version = 0;
		private int returnIndex = -1;
		private Hardware hardware = null;

		public NCCP_Request(int op, Object[] vals, double v)
		{
			this(op, vals, 0, v);
		}

		public NCCP_Request(int op, Object[] vals, int p, double v)
		{
			super(op);
			port = p;
			values = vals;
			if (values != null) numParams = Array.getLength(values);
			version = v;
		}
		public void setValues(Object[] vals)
		{
			values = vals;
			if (values != null) numParams = Array.getLength(values);
		}
		public void setPort(int p) { port = p;}
		public int getPort() { return port;}
		public void setHardware(Hardware v) { hardware = v;}
		public Hardware getHardware() { return hardware;}
		public void storeData(DataOutputStream dout) throws IOException
		{
			if (returnIndex < 0)
			{
				if (hardware != null)
				{
					returnIndex = hardware.getReference();//ReturnIndex();
				}
				else returnIndex = 0;
				//returnMarker.setIndex(returnIndex);
			}
			ExpCoordinator.print(new String("Hardware.NCCP_Requester.storeData returnIndex:" + returnIndex + " port:" + port + " version:" + version + " numParams:" + numParams), 5);
			dout.writeInt(returnIndex);
			dout.writeShort(port);
			if (hardware != null)
			    NCCP.writeString(hardware.expCoordinator.getCurrentExp().getProperty(Experiment.ONLID), dout); //1_30_23 added to be able identify component and experiment
			else
			    NCCP.writeString(String.valueOf(version), dout);
			Object param;
			int tmp_num_params = numParams;
			int i;
			for (i = 0; i < numParams; ++i)
			{
				param = values[i];
				if (param instanceof NextHop && !(param instanceof NextHopIP)) ++tmp_num_params;
			}
			dout.writeInt(tmp_num_params);
			for (i = 0; i < numParams; ++i)
			{
				param = values[i];
				if (param instanceof Integer) 
				{
					dout.writeShort(CommandSpec.INT);
					dout.writeInt(((Integer)param).intValue());
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending int " + ((Integer)param).intValue()), TEST_NCCPVALS);
				}
				if (param instanceof ParamSpec.WCInteger) 
				{
					dout.writeShort(CommandSpec.INT);
					dout.writeInt(((ParamSpec.WCInteger)param).intValue());
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending int " + ((ParamSpec.WCInteger)param).intValue()), TEST_NCCPVALS);
				}
				if (param instanceof Double) 
				{
					dout.writeShort(CommandSpec.DOUBLE);
					NCCP.writeString(String.valueOf(((Double)param).doubleValue()), dout);
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending double " + ((Double)param).doubleValue()), TEST_NCCPVALS);
				}
				if (param instanceof ParamSpec.WCDouble) 
				{
					dout.writeShort(CommandSpec.DOUBLE);
					NCCP.writeString(String.valueOf(((ParamSpec.WCDouble)param).doubleValue()), dout);
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending double " + ((ParamSpec.WCDouble)param).doubleValue()), TEST_NCCPVALS);
				}
				if (param instanceof Boolean) 
				{
					dout.writeShort(CommandSpec.BOOL);
					if (((Boolean)param).booleanValue())
						dout.writeByte(1);
					else
						dout.writeByte(0);
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending bool " + ((Boolean)param).booleanValue()), TEST_NCCPVALS);
				}
				if (param instanceof String) 
				{
					dout.writeShort(CommandSpec.STRING);
					NCCP.writeString((String)param, dout);
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending string " + ((String)param)), TEST_NCCPVALS);
				}
				if (param instanceof ONL.IPAddress)
				{
					//dout.writeShort(CommandSpec.IPADDR);
					//NCCP.writeString(param.toString(), dout);
					dout.writeShort(CommandSpec.INT);
					dout.writeInt(((ONL.IPAddress)param).getInt());
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending ipaddress " + param.toString()), TEST_NCCPVALS);
				}
				if (param instanceof NextHop)
				{
					//dout.writeShort(CommandSpec.NEXTHOP);
					//NCCP.writeString(((NextHop)param).toCommandString(), dout);
					if (!(param instanceof NextHopIP))
					{
						dout.writeShort(CommandSpec.INT);
						dout.writeInt(((NextHop)param).getPort());
					}
					dout.writeShort(CommandSpec.INT);
					dout.writeInt(((NextHop)param).getNextHopIP().getInt());
					ExpCoordinator.print(new String("Hardware.NCCP_Request.storeData sending nexthop " + ((NextHop)param).toCommandString()), TEST_NCCPVALS);
				}
			}
		}
		public void setPeriod(PollingRate pr)
		{
			if (pr != null)
			{
				period_secs = pr.getSecs();
				period_usecs = pr.getUSecs();
				if (period_secs > 0 || period_usecs > 0) periodic = 1;
				else periodic = 0;	
			} 
			else periodic = 0;
		}
		public Object[] getValues() { return values;}
		public String getValuesString()
		{
			String rtn = "";
			if (numParams > 0) rtn = rtn.concat(values[0].toString());
			for (int i = 1; i < numParams; ++i)
			{
				rtn = rtn.concat(new String("," + values[0].toString()));
			}
			return rtn;
		}
	}


	public static class NCCP_MonitorResponse extends NCCP.LTDataResponse
	{
		private String statusMsg = null;
		private double otimeInterval = 1;
		private boolean is_positive = true; //true if the rate can never be negative
		public NCCP_MonitorResponse() 
		{ 
			super();
			clkRateScaler = 1;
			lt = -1;
		}
		public NCCP_MonitorResponse(DataInput din) throws IOException
		{
			super(din);
			clkRateScaler = 1;
			lt = -1;
		}
		public NCCP_MonitorResponse(DataInput din, int dl) throws IOException
		{
			super(din, dl);
			clkRateScaler = 1;
			lt = -1;
		}
		public void setIsPositive(boolean b) { is_positive = b;}
		public void retrieveFieldData(DataInput din) throws IOException
		{
			super.retrieveFieldData(din);
			statusMsg = NCCP.readString(din);
		}
		public void retrieveData(DataInput din) throws IOException
		{ 
			retrieveFieldData(din);
			oStatus = status;
		}
		public String getStatusMsg() { return statusMsg;}
		public void setLT(double nsp_lt) //change from switch lt because lt is returned as a number of clock ticks    
		{
			oLT = lt;
			lt = nsp_lt;
			if (timeInterval > 0) otimeInterval = timeInterval;
			if (oLT >= 0) 
			{
				reset = false;
				if (lt == 0 || (status != NCCP.Status_Fine))
				    {
					ExpCoordinator.print("Hardware.NCCP_MonitorResponse.setLT error: - clock time is 0 or status is not fine setting lt to last known positive timestamp");
					lt = oLT;
					data = odata;
					timeInterval = 0;
					if  (status == NCCP.Status_Fine)
					    {
						status = NCCP.Status_TimeoutRemote;//this mimics what I've been seeing but I've had a vodka gimlet. I currently believe I'm the only one reading my comments.
						ExpCoordinator.print("Hardware.NCCP_MonitorResponse.setLT error: - clock time is 0 and it's saying it's fine. I think that's not really true.");
					    }
					this.print();
					return;
				    }
				else if (lt >= oLT)
				    {
					timeInterval = (lt - oLT)/clkRate;///1000;
					ExpCoordinator.print(new String("Hardware.NCCP_MonitorResponse.setLT timeInterval:" + timeInterval), 5);
				    }
				else //lt wrapped
				{
					ExpCoordinator.print("Hardware.NCCP_MonitorResponse.setLT lt wrapped or reset oLt");
					timeInterval = 0;
					if (period != null) timeInterval = period.getSecsOnly();
					reset = true;
					this.print();
					//if (lt == 0) //added 9/29/2020
					//{
					//ExpCoordinator.print("Hardware.NCCP_MonitorResponse.setLT error: - clock time is 0 setting lt to last known positive timestamp");
					//	lt = oLT;
					//  }
				}


				//if we dropped the last response make the reported timeInterval what is expected
				//if (oStatus != NCCP.Status_Fine) //is this the problem the last one failed if we had zero last time and we get a good one we want to rely on the system clock
				//{
				//	if (period != null) timeInterval = period.getSecsOnly();
				//	else timeInterval = 0;
				//}
				realTimeInterval = timeInterval;
			}
			else 
			{
				timeInterval = 0;
				if (period != null) otimeInterval = period.getSecsOnly();
				else otimeInterval = 1;
				realTimeInterval = timeInterval;
				reset = true; //not a switch reset but the first response we've gotten
			}
		}
		public double getData(MonitorDataType.Base mdt) 
		{
			if (mdt == null || mdt.isAbsolute())
				return data;
			else 
			{
				if (reset) return 0;
				double rtn = 0;
				double data_change = data - odata;
				if (data_change < 0) 
				{
					if (is_positive) data_change = data;
					ExpCoordinator.print(new String("Hardware.NCCP_MonitorResponse.getData negative data:" + data + " odata:" + odata + " data_change:" + data_change));
					print();
				}
				if (timeInterval <= 0)
				    {
					if (otimeInterval > 0)
					    rtn = (data_change)/otimeInterval;
					else
					    rtn = 0;
				    }
				else
				    rtn = getRate(data_change);
				return rtn;
			}
		}
	}		
	
	//////////////////////////////////// Hardware.BatchCommandHandler ////////////////////////////
	protected static class BatchCommandHandler extends DefaultHandler
	{
		protected ExperimentXML expXML = null;
		protected String currentElement = "";
		private ONLComponent bComponent = null;
		private Command bCommand = null;
		private HWTableElement tableElement = null;
		private HWTableElement.TContentHandler telemHandler = null;

		//////////////////////////////////////////// Hardware.BatchCommandHandler.ComponentHandler ////////////////////////////////////////////////////////
		private class ComponentHandler extends ONLComponent.XMLComponentHandler
		{
			public ComponentHandler(String uri, Attributes attributes, ExperimentXML exp_xml)
			{
				super(uri,attributes,exp_xml);
			}

			public void endElement(String uri, String localName, String qName)
			{
				if (localName.equals(ExperimentXML.COMPONENT))
				{
					ExpCoordinator.print(new String("Hardward.BatchCommandHandler.ComponentHandler.endElement " + localName + "  component " + getComponent().getLabel()), ExperimentXML.TEST_XML);
					expXML.removeContentHandler(this);
					bComponent = getComponent();
				}
			}
		}// end class Hardware.BatchCommandHandler.ComponentHandler

		public BatchCommandHandler(ExperimentXML exp_xml)
		{
			super();
			expXML = exp_xml;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			currentElement = new String(localName);
			ExpCoordinator.print(new String("Hardware.BatchCommandHandler.startElement " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(ExperimentXML.COMPONENT)) expXML.setContentHandler(new ComponentHandler(uri, attributes, expXML));
			if (localName.equals(MDataType.TABLE_ELEM))
			{
				ONLComponent onlc = bComponent;
				if (onlc != null && onlc instanceof HWTable)
				{
					tableElement = ((HWTable)onlc).createNewElement();//want to get existing element if it is there
					if (attributes.getValue(HWTableElement.GENERATED) != null) tableElement.setGenerated(attributes.getValue(HWTableElement.GENERATED));
					if (attributes.getValue(HWTableElement.HOST_GENERATED) != null) tableElement.setHostGenerated(attributes.getValue(HWTableElement.HOST_GENERATED));
					telemHandler = (HWTableElement.TContentHandler)tableElement.getContentHandler(expXML);
					telemHandler.setAdd(true);//false);
					expXML.setContentHandler(telemHandler);
				}
			}
			if (localName.equals(ExperimentXML.COMMAND))
			{
				
				ONLComponent onlc = bComponent;
				if (onlc != null)
				{
					int opcode = Integer.parseInt(attributes.getValue(ExperimentXML.OPCODE));
					Command cmd = null;
					if (onlc instanceof Hardware) cmd = ((Hardware)onlc).getCommand(opcode, false);
					if (onlc instanceof Hardware.Port) cmd = ((Hardware.Port)onlc).getCommand(opcode, false);
					if (onlc instanceof HWTable)
					{
						if (tableElement == null)  cmd = ((HWTable)onlc).getCommand(opcode);
						else
						{
							if (!telemHandler.isAdded()) tableElement = (HWTableElement)((HWTable)onlc).getElement(tableElement);
							cmd = ((HWTableElement)((HWTable)onlc).getElement(tableElement)).getCommand(opcode);
						}
					}
					if (cmd != null)
					{
						cmd.print(TEST_BATCHING);
						cmd = cmd.getCopy(onlc);
						bCommand = cmd;
						expXML.setContentHandler(bCommand.getContentHandler(expXML));
					}
					else
					{
						ExpCoordinator.print("Hardware.BatchCommandHandler.startElement command(" + opcode + ") is null");
					}
				}
			}
		}
		public void setCurrentElement(String s) { currentElement = new String(s);}
		public String getCurrentElement() { return currentElement;}
		public void endElement(String uri, String localName, String qName)
		{
			ExpCoordinator.print(new String("Hardware.BatchCommandHandler.endElement " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(ExperimentXML.BATCH_COMMAND)) 
			{
				bCommand.print(ExperimentXML.TEST_XML);
				expXML.removeContentHandler(this);
				Command.CAction caction = new Command.CAction(bComponent, bCommand);
				caction.addOperation();
			}
		}
	}//end class Hardware.BatchCommandHandler

	//////////////////////////////////////////////////// Hardware.MDataType  /////////////////////////////////////////////////////////////////////////

	public static class MDataType extends MonitorDataType.Base
	{
		//private Hardware.CommandSpec commandSpec = null;
		public static final String TABLE_ELEM = "tableElement";
		private Command command = null;
		protected int numParams = 0;
		private int index = 0;
		private boolean is_positive = true; //is true if rate is never negative

		////////////////////////// Hardware.MDataType.XMLHandler /////////////////////////////////////////
		protected static class XMLHandler extends MonitorDataType.Base.XMLHandler
		{
			private HWTableElement tableElement = null;
			public XMLHandler(ExperimentXML exp_xml, MDataType mdt)
			{
				super(exp_xml, mdt);
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	  
				super.startElement(uri, localName, qName, attributes);
				if (localName.equals(TABLE_ELEM))
				{
					ONLComponent onlc = mdataType.getONLComponent();
					if (onlc != null && onlc instanceof HWTable)
					{
						tableElement = ((HWTable)onlc).createNewElement();
						if (attributes.getValue(HWTableElement.GENERATED) != null) tableElement.setGenerated(attributes.getValue(HWTableElement.GENERATED));
						if (attributes.getValue(HWTableElement.HOST_GENERATED) != null) tableElement.setHostGenerated(attributes.getValue(HWTableElement.HOST_GENERATED));
						HWTableElement.TContentHandler tc = (HWTableElement.TContentHandler)tableElement.getContentHandler(expXML);
						tc.setAdd(false);
						expXML.setContentHandler(tc);
					}
				}
				if (localName.equals(ExperimentXML.COMMAND))
				{
					ONLComponent onlc = mdataType.getONLComponent();
					if (onlc != null)
					{
						Command cmd = null;
						if (onlc instanceof Hardware) cmd = ((Hardware)onlc).getCommand(Integer.parseInt(attributes.getValue(ExperimentXML.OPCODE)), true);
						if (onlc instanceof Hardware.Port) cmd = ((Hardware.Port)onlc).getCommand(Integer.parseInt(attributes.getValue(ExperimentXML.OPCODE)), true);
						if (onlc instanceof HWTable)
						{
							if (tableElement == null)  cmd = ((HWTable)onlc).getMonitorCommand(Integer.parseInt(attributes.getValue(ExperimentXML.OPCODE)));
							else
							{
								cmd = ((HWTableElement)((HWTable)onlc).getElement(tableElement)).getMonitorCommand(Integer.parseInt(attributes.getValue(ExperimentXML.OPCODE)));
							}
						}
						if (cmd != null)
						{
							cmd.print(TEST_MONITOR);
							cmd = cmd.getCopy(onlc);
							((Hardware.MDataType)mdataType).setCommand(cmd);
							expXML.setContentHandler(cmd.getContentHandler(expXML));
						}
					}
				}
			}
		}//end Hardware.MDataType.XMLHandler

		//monitor id is opcode
		public MDataType(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.HWC, MonitorDataType.HWMONITOR);
			if (attributes.getValue(ExperimentXML.TYPE).equals(MonitorDataType.HWCOMMAND_LBL)) paramType = MonitorDataType.HWCOMMAND;
		}
		public MDataType(int p_type, SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
		{
			super(ONLDaemon.HWC, p_type, Units.UNKNOWN);
			initFromFile(infile, lv, nlists);
			//setDescription();
		}
		public MDataType(int p_type, ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.HWC, p_type, Units.UNKNOWN);
			initFromReader(rdr, dt);
			//setDescription();
		}

		//protected MDataType(ONLComponent c,  Hardware.CommandSpec cspec, Object[] vals)
		protected MDataType(Command cspec) { this(cspec.getParentComponent(), cspec, null);}//(CommandSpec)cspec, cspec.getCurrentValues());}
		protected MDataType(ONLComponent oc, Command cspec) { this(oc, cspec, null);}

		protected MDataType(ONLComponent c,  Command cspec, Object[] vals)  
		{
			super(ONLDaemon.HWC, MonitorDataType.HWCOMMAND, cspec.getLabel(), cspec.getUnits());
			ExpCoordinator.print(new String("Hardware.MDataType component:" + c.getLabel() + " command:" + cspec.getLabel() + " dataUnits:" + dataUnits), TEST_MONITOR);
			command = cspec.getCopy(c, vals);//new Command(cspec, c);
			if (command.isMonitor())
				paramType = MonitorDataType.HWMONITOR;
			//setIsRate(false);
			monitorID = command.getOpcode();
			setONLComponent(c);
			//paramValues = vals;
			numParams = command.getNumParams();
			setIsRate(command.isRate());
			if (c instanceof Hardware && (c.getProperty(Hardware.INDEX) != null))
				setName(getName().concat((String)c.getProperty(Hardware.INDEX)));
			if (c instanceof Hardware.Port && (c.getParent().getProperty(Hardware.INDEX) != null))
			{
				port = ((Hardware.Port)c).getID();
				setName(getName().concat(new String(c.getParent().getProperty(Hardware.INDEX) + "." + port)));
			}
			if (c instanceof HWTable && ((HWTable)c).getPort() != null)
				port = ((HWTable)c).getPort().getID();
			is_positive = cspec.isPositive();
		}
		public String getParamTypeName() 
		{ 
			if (paramType == MonitorDataType.HWMONITOR) return MonitorDataType.HWMONITOR_LBL;
			else return MonitorDataType.HWCOMMAND_LBL;
		}
		public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			if (command instanceof HWTableElement.TableCommand) 
			{
				xmlWrtr.writeStartElement(TABLE_ELEM);
				((HWTableElement.TableCommand)command).getEntry().writeXML(xmlWrtr);
				xmlWrtr.writeEndElement();
			}
			command.writeXML(xmlWrtr, ExperimentXML.COMMAND);
		}
		public boolean isPositive() { return is_positive;} //true if rate is never negative

		/*    protected MDataType(ONLComponent c,  CommandSpec cspec, Object[] vals)
      {
	super(ONLDaemon.HWC, MonitorDataType.HWCOMMAND, cspec.getLabel(), cspec.getUnits());
        if (cspec.isMonitor())
          paramType = MonitorDataType.HWMONITOR;
        //setIsRate(false);
	monitorID = cspec.getOpcode();
        command = cspec.createCommand(c);//new Command(cspec, c);
	command.setCurrentValues(vals);
        setONLComponent(c);
        //paramValues = vals;
        numParams = cspec.getNumParams();
        setIsRate(cspec.isRate());
	if (c instanceof Hardware && (c.getProperty(Hardware.INDEX) != null))
	  setName(getName().concat((String)c.getProperty(Hardware.INDEX)));
	if (c instanceof Hardware.Port && (c.getParent().getProperty(Hardware.INDEX) != null))
	  {
	    port = ((Hardware.Port)c).getID();
	    setName(getName().concat(new String(c.getParent().getProperty(Hardware.INDEX) + "." + port)));
	  }
      }
		 */

		protected void setDescription()
		{
			if (numParams > 0)
			{
				Param pelem = (Param)command.getParam(0);
				String val_str = "(" ;
				String plbl_str = "(";
				if (pelem != null)
				{
					plbl_str = plbl_str.concat(new String("," + pelem.getLabel()));
					val_str = val_str.concat(new String("," + pelem.getCurrentValue()));
				}
				for (int i = 1; i < numParams; ++i)
				{
					pelem = (Param)command.getParam(i);
					plbl_str = plbl_str.concat(new String("," + pelem.getLabel()));
					val_str = val_str.concat(new String("," + pelem.getCurrentValue()));
				}
				String rtn = new String(command.getDescription() + ": " + plbl_str + ") = " + val_str + ")");
				ExpCoordinator.print(new String("Hardware.MDataType.setDescription " + rtn), 5);
				setDescription(rtn);
			}
			else
				setDescription(command.getDescription());
		}
		public double getVersion() 
		{
			if (onlComponent.getHardware() != null)
				return (onlComponent.getHardware().getVersion());
			return 0;
		}
		public Object[] getValues() { return (command.getCurrentValues());}
		public String getSpec()
		{
			String param_str = "";
			//CommandSpec.Param[] params = commandSpec.getParams();
			Param pelem;
			for (int i = 0; i < numParams; ++i)
			{
				pelem = (Param)command.getParam(i);
				param_str = param_str.concat(new String(pelem.getType() + " " + pelem.getCurrentValue() + " "));
			}
			return (new String(monitorID + " " + command.getNumParams() + " " + param_str));
		}

		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeInt(monitorID);
			wrtr.writeInt(command.getNumParams());
			//CommandSpec.Param[] params = commandSpec.getParams();
			Param pelem;
			for (int i = 0; i < numParams; ++i)
			{
				pelem = (Param)command.getParam(i);
				wrtr.writeString(pelem.getType());
				wrtr.writeString(pelem.getCurrentValue().toString());
			}
		}


		public void initFromReader(ONL.Reader rdr, int dt) throws IOException
		{
			super.initFromReader(rdr, dt);
			monitorID = rdr.readInt();
			boolean is_mon =  (paramType == MonitorDataType.HWMONITOR);
			if (onlComponent != null)
			{
				ExpCoordinator.print(new String("Hardware.MDataType.initFromReader onlComponent is " + onlComponent.getLabel() + " monitorID:" + monitorID + " is_mon:" + is_mon), 5);
				command = new Command(onlComponent.getCommand(monitorID, is_mon));
			}
			else
				ExpCoordinator.print(new String("Hardware.MDataType.initFromReader onlComponent is null"), 5);
			if (command == null) //the spec may have changed from when this was saved
			{
				ExpCoordinator.print(new String("         commandSpec is null"), 5);
				monitorID = 0;
				return;
			}
			//setDataUnits(commandSpec.getUnits());
			int n_params = rdr.readInt();
			ExpCoordinator.print(new String("         commandSpec is num params = " + command.getNumParams() + " file num params = " + n_params), 5);
			if (n_params != command.getNumParams())
			{
				monitorID = 0;
				return;
			}
			numParams = n_params;
			String tp;
			Param pelem; //arams[] = commandSpec.getParams();
			//paramValues = new Object[n_params];
			for (int i = 0; i < n_params; ++i)
			{
				pelem = (Param)command.getParam(i);
				tp = rdr.readString();
				if (!tp.equals(pelem.getType()))            
				{
					monitorID = 0;
					return;
				}
				ExpCoordinator.print(new String("    param["+i+"] command type = " + pelem.getType() + " file type = " + tp), 5);
				pelem.setDefaultFromString(rdr.readString());
			}
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			try
			{
				ONL.StringReader srdr = new ONL.StringReader(ln);
				monitorID = srdr.readInt();
				boolean is_mon =  (paramType == MonitorDataType.HWMONITOR);
				if (onlComponent != null)
				{
					ExpCoordinator.print(new String("Hardware.MDataType.loadFromSpec onlComponent is " + onlComponent.getLabel()), 5);
					command = new Command(onlComponent.getCommand(monitorID, is_mon));
				}
				else
					ExpCoordinator.print(new String("Hardware.MDataType.loadFromSpec onlComponent is null"), 5);
				if (command == null) //the spec may have changed from when this was saved
				{
					ExpCoordinator.print(new String("         command is null"), 5);
					monitorID = 0;
					return;
				}
				setDataUnits(command.getUnits());
				int n_params = srdr.readInt();
				ExpCoordinator.print(new String("         command is num params = " + command.getNumParams() + " file num params = " + n_params), 5);
				if (n_params != command.getNumParams())
				{
					monitorID = 0;
					return;
				}
				numParams = n_params;
				String tp;
				ParamSpec pelem;//arams[] = commandSpec.getParams();
				//paramValues = new Object[n_params];
				for (int i = 0; i < n_params; ++i)
				{
					pelem = command.getParam(i);
					tp = srdr.readString();
					if (!tp.equals(pelem.getType()))            
					{
						monitorID = 0;
						return;
					}
					ExpCoordinator.print(new String("    param["+i+"] command type = " + pelem.getType() + " file type = " + tp), 5);
					pelem.setDefaultFromString(srdr.readString());
				}
			}
			catch (java.io.IOException e) 
			{
				ExpCoordinator.print("Hardware.MDataType failed to load spec");
			}
		}
		public void setIndex(int i) { index = i;}
		public int getIndex() { return index;}
		public Hardware getHardware() 
		{
			ONLComponent c = getONLComponent();
			return (c.getHardware());
			//return null;
		}
		public boolean isEqual(MonitorDataType.Base mdt)
		{
			if (!(mdt instanceof Hardware.MDataType)) return false;
			Hardware.MDataType hmdt =  (Hardware.MDataType) mdt;
			return (super.isEqual(mdt) && numParams == hmdt.numParams && command.isEqual(hmdt.command));
			/*
          {

            Hardware.MDataType hmdt =  (Hardware.MDataType) mdt;
            if (numParams != hmdt.numParams) return false;
            for (int i = 0 ; i < numParams; ++i)
              {
                if (!(paramValues[i].toString().equals(hmdt.paramValues[i].toString())))
                  return false;
              }
            return true;
          }
        else return false;
			 */
		}
		protected Command getCommand() { return command;}
		protected void setCommand(Command cmd)
		{
			command = cmd;
			monitorID = cmd.getOpcode();
			numParams = cmd.getNumParams();
		}
		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new Hardware.MDataType.XMLHandler(exp_xml, this));}
	}


	////////////////////////////////////////////// MEntry //////////////////////////////////////////////////////////////////////////////////////////

	public static class MEntry extends MonitorEntry.Base
	{
		public MEntry(MonitorEntry.Listener l, MDataType dt)
		{
			super(l);
			request = new NCCP_Request(dt.getMonitorID(), dt.getValues(), dt.getPort(), dt.getVersion());
			((NCCP_Request)request).setHardware(dt.getHardware());
			response = new NCCP_MonitorResponse();
			response.setPeriod(dt.getPollingRate());
			((NCCP_MonitorResponse)response).setIsPositive(dt.isPositive());
			setMarker(getNewMarker(dt));
			marker.print(2);
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("ME::processResponse");
			if (r.getStatus() == NCCP.Status_Failed)
			{
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("Hardware Monitoring Failure  msg:" + ((NCCP_MonitorResponse)r).getStatusMsg())),
						"Hardware Monitoring Operation Failure.",
						"Hardware Monitoring Operation Failure.",
						(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR | StatusDisplay.Error.POPUP)));
				setRequestSent(false);
				clear();
			}
			else
			{
				Monitor monitor;
				int numParams = monitors.size();
				for (int i = 0; i < numParams; ++i)
				{
					monitor = (Monitor)monitors.elementAt(i);
					if (monitor != null)
					{
						//System.out.println("  calling monitor");
						if (!monitor.isStarted()) monitor.start();
						monitor.setData(r.getData(monitor.getDataType()), ((NCCP.LTDataResponse)r).getTimeInterval(), r.getStatus() != NCCP.Status_Fine);
					}
				}
			}
		}

		public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
		{
			return (new REND_Marker_class(mdt.getPort(), mdt.isIPP(), mdt.getMonitorID(), ((MDataType)mdt).getIndex()));
		}
	}

	//////////////////////////////////////////////// MParamOptions /////////////////////////////////////////////////////////////////////////////// 
	//inner MenuItemParamOptions class 
	protected static class MParamOptions extends MonitorMenuItem.MMLParamOptions
	{
		private Command command = null;
		public MParamOptions(Command cs)
		{
			super(cs.getParamUIReps());
			setIsRate(cs.isRate());
			command = cs;
		}
		public Command getCommand() { return command;}
	}
	//////////////////////////////////////// Hardware.MMenuItem ////////////////////////////////////////////////////////////////////////////
	public static class MMenuItem extends MonitorMenuItem.BaseItem
	{
		private Command command = null;
		private ONLComponent onlComponent = null;
		private int index = 0;

		/////////////////////////////////////////////////// MMI methods ////////////////////////////////////////////////////////////////////////////
		/* 
       public MMenuItem(Hardware.Port hwip, Hardware.Port.Monitorable mon, Command cs) { this(hwip, mon, cs, false);}
       public MMenuItem(Hardware.Port hwip, Hardware.Port.Monitorable mon, Command cs, boolean noListener)
       {
       this(cs.getLabel(), hwip, mon, cs, noListener);
       }
       public MMenuItem(String lbl, Hardware.Port hwip, Hardware.Port.Monitorable mon, Command cs, boolean noListener)
       {
       super(cs.getLabel(), noListener, mon.getMonitorManager(), mon, null);
       monitors = new Vector();
       command = cs;
       port = hwip;
       }
       public MMenuItem(Hardware hwi, Command cs) { this(hwi, cs, false);}
       public MMenuItem(Hardware hwi, Command cs, boolean noListener)
       {
       this (cs.getLabel(), hwi, cs, noListener);
       }
       public MMenuItem(String lbl, Hardware hwi, Command cs, boolean noListener)
       {
       super(lbl, noListener, hwi.getMonitorable().getMonitorManager(), hwi.getMonitorable(), null);
       monitors = new Vector();
       command = cs;
       hardware = hwi;
       }
		 */
		public MMenuItem(ONLComponent oc, Command cmd) { this(oc, cmd, false);}
		public MMenuItem(ONLComponent oc, Command cmd, boolean noListener) { this (cmd.getLabel(), oc, cmd, noListener);}
		public MMenuItem(String lbl, ONLComponent oc, Command cmd, boolean noListener)
		{
			super(lbl, noListener, oc.getMonitorable().getMonitorManager(), oc.getMonitorable(), null);
			onlComponent = oc;
			command = cmd;
		}
		public Monitor createMonitor(MonitorDataType.Base dt)
		{
			((MDataType)dt).setIndex(++index);
			return (super.createMonitor(dt));
		}
		protected MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			Object params[] = ((MParamOptions)mpo).getXtraParams().toArray();
			Command cmd = ((MParamOptions)mpo).getCommand();
			dt = new  MDataType(onlComponent, cmd, cmd.getCurrentValues()); 
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(onlComponent);
			/*
	  if (port != null)
	  {
	  dt = new  MDataType(port, command, command.getCurrentValues()); 
	  dt.setPollingRate(mpo.getPollingRate());
	  dt.setONLComponent(port);
	  }
	  else 
	  {
	  if (hardware != null)
	  {
	  dt = new MDataType(hardware, command, command.getCurrentValues());
	  dt.setPollingRate(mpo.getPollingRate());
	  dt.setONLComponent(hardware);
	  }
	  }
			 */
			return dt;
		}
		public MonitorMenuItem.MMLParamOptions getParamOptions() 
		{
			return (new MParamOptions(command));
		}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			ExpCoordinator.print(new String("Hardware.MMenuItem.isDataType " + mdt.getMonitorID() + " command:" + command.getOpcode()), TEST_MONITOR);
			if (mdt.getMonitorID() == command.getOpcode())
			{
				if ((command.isMonitor() &&  mdt.getParamType() == MonitorDataType.HWMONITOR) ||
						(command.isCommand() &&  mdt.getParamType() == MonitorDataType.HWCOMMAND))
					return true;
			}
			return false;
		}
		protected Command getCommand() { return command;}
	}
}//end class Hardware
