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
 * File: ONLComponent.java
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
import java.io.*;
import java.util.*;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.undo.*;
import java.beans.*;
import java.lang.reflect.Array;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public abstract class ONLComponent implements ListDataListener, PropertyChangeListener
{
	public static final int TEST_CTYPE = 7;
	public static final int TEST_LABEL_CHANGE = 6;
	public static final int TEST_10G = 6;
	public static final int TEST_IPADDR = Topology.TEST_DEFRTS;
	public static final int TEST_SUBNET = 2;
	public static final int TEST_LINK = Topology.TEST_DEFRTS;
	//type names
	public static final String HOST_LBL = "PC";
	public static final String GIGE_LBL = "GIGE";
	public static final String VGIGE_LBL = "VGIGE";
	public static final String NSP_LBL = "NSP";
	public static final String NPR_LBL = "NPR";
	public static final String IXP_LBL = "IXP";
	public static final String UNKNOWN_LBL = "unknown";
	public static final String LINK_LBL = "link";
	public static final String PORTS_INIT = "Ports_initialized";
	public static final String PORT_LBL = "Port";
	//Properties
	public static final String SELECTED = "selected";
	public static final String COMMITTED = "committed";
	public static final String ACTIVE = "active";
	public static final String FAILED = "failed";
	public static final String WAITING = "waiting";
	public static final String NOT_INIT = "uninitialized";
	public static final String INIT = "initialized";
	public static final String CLEARED = "cleared";
	//comparison state
	public static final String INBOTH = "in_both";
	public static final String IN1 = "in_1";
	public static final String IN2 = "in_2";
	public static final String INCONSISTENT = "inconsistent";

	public static final String STATE = "state";
	public static final String LABEL = "label";
	public static final String LINKED_TO = "linkedto";
	public static final String TYPE = "type";
	public static final String COMPONENT_TYPE = ExperimentXML.CTYPE;
	public static final String IPADDR = "ipaddr";
	public static final String SUBNET = "subnet";
	//SUBNET:remove//public static final String NHIPADDR = "nhipaddr";
	public static final String COMMITTED_LABEL = "committed_label";
	public static final String REMAP_SENT = "remap_sent";
	public static final String MAPPEDTO = "mapped_to"; //the component label of the actual hardware
	public static final String REF_NUM = "hwRef";
	public static final String CLUSTER_REF = "clusterRef";
	public static final String INDEX = "index";
	public static final String INTERFACE_TYPE = ExperimentXML.INTERFACE_TYPE;//"interfaceType";
	public static final String NETMASK = ExperimentXML.MASK;//"interfaceType";
	public static final String ORIG_LABEL = "orig_label";
	public static final String ORIG_IPADDR = "orig_ipaddr";
	public static final String D_LABEL = "display_label";
	public static final int GIGE_IP = 160;
	public static final int HOST_IP = 30;
	public static final String RTABLE = ExperimentXML.ROUTE_TABLE;//51; //route table
	public static final String IPPEMFTABLE = "IPPEMFTable"; //EM filter table
	public static final String IPPGMFTABLE = "IPPGMFTable"; //GM filter table
	public static final String IPPEXGMFTABLE = "IPPEXGMFTable"; //54; //EXGM filter table
	public static final String IPPQTABLE = "IPPQTable";//QTABLE; //queue table
	public static final String PCLASSES = ExperimentXML.PCLASSES;//56;//pluginClasses
	public static final String PINSTANCES  = ExperimentXML.PINSTANCES;//57;
	public static final String OPPEMFTABLE = "OPPEMFTable";//58; //EM filter table
	public static final String OPPGMFTABLE = "OPPGMFTable";//59; //GM filter table
	public static final String OPPEXGMFTABLE = "OPPEXGMFTable"; //60; //EXGM filter table
	public static final String OPPQTABLE = "OPPQTable";//61; //queue table
	public static final String HWTABLE = ExperimentXML.TABLE;


	protected ONLMonitorable monitorable = null; //controls the monitorable params of this component null if nothing to monitor
	protected ExpCoordinator expCoordinator = null;
	protected Action removeAction = null;
	private ONLPropertyList description = null;
	private ONLComponent parent = null;
	private ONLComponentList children = null;
	private boolean linkable = true;
	//private boolean linked = false;
	//private boolean is_selected = false;
	//private boolean active = false;
	protected ONLPropertyList properties = null;
	protected ONLComponent linkedTo = null;
	protected Vector<ONLDaemon> daemons = null;
	private ConnectionListener connListener = null;
	private GraphicsListener graphicsListener = null;
	private StateListener stateListener = null;
	private Vector propertiesToWatch = null;

	private Vector eventListeners = null;

	private Cluster.Instance cluster = null;
	protected SubnetManager.Subnet subnet = null;
	private SubnetManager.Subnet originalSubnet = null;

	private Color iconColor = null;
	private Dimension iconSize = null;


	public static interface ONLMonitorable
	{
		public Monitor addMonitor(MonitorDataType.Base mdt);
		public Monitor addMonitor(Monitor m);
		public void removeMonitor(Monitor m);
		public ONLComponent getONLComponent();
		public MonitorManager getMonitorManager();
		public void clear();
		public void addActions(ONLComponent.CfgMonMenu menu);
		//public void initializeMenus();
	}

	/////////////////////////////////////////////////////ONLContentHandler/////////////////////////////////////////////////////////////
	public static class ONLContentHandler extends DefaultHandler
	{
		protected ONLComponent component = null;
		protected ExperimentXML expXML = null;
		private String currentElement = "";

		public ONLContentHandler(ONLComponent c, ExperimentXML exp_xml)
		{
			super();
			component = c;
			expXML = exp_xml;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			currentElement = new String(localName);
			ExpCoordinator.print(new String("ONLComponent.ONLContentHandler.startElement " + localName), 3);
			if (localName.equals(ExperimentXML.USER_LABEL)) component.setProperty(ExperimentXML.USER_LABEL, attributes.getValue(uri, ExperimentXML.VALUE));
			if (localName.equals(ExperimentXML.COLOR)) component.setIconColor(Integer.parseInt(attributes.getValue(uri, "r")),
					Integer.parseInt(attributes.getValue(uri, "g")),
					Integer.parseInt(attributes.getValue(uri, "b")));
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("ONLComponent.ONLContentHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
			if (currentElement.equals(ExperimentXML.LABEL)) component.setLabel(new String(ch, start, length));
			if (currentElement.equals(ExperimentXML.HWREF)) component.setProperty(REF_NUM, new String(ch, start, length));
			if (currentElement.equals(ExperimentXML.CLUSTER_REF)) component.setProperty(CLUSTER_REF,  new String(ch, start, length));
		}
		public void setCurrentElement(String s) { currentElement = new String(s);}
		public String getCurrentElement() { return currentElement;}
		public void endElement(String uri, String localName, String qName)
		{
			ExpCoordinator.print(new String("ONLComponent.ONLContentHandler.endElement " + localName + "  component " + component.getLabel()), 3);
			if (localName.equals(ExperimentXML.NODE)) 
			{
				expXML.addNode(component);
				expXML.removeContentHandler(this);
			}
			if (localName.equals(ExperimentXML.LINK) && (component instanceof LinkDescriptor))
			{
				expXML.addLink(component);
				expXML.removeContentHandler(this);
			}
		}
		public ONLComponent getComponent() { return component;}
	}

	/////////////////////////////////////////////////////XMLComponentHandler//////////////////////////////////////////////////////////
	public static class XMLComponentHandler extends DefaultHandler
	{
		private String typeName = "";
		//private int typeCode = UNKNOWN;
		protected ExperimentXML expXML = null;
		private String currentElement = "";
		private ONLComponent component = null;
		private Topology topology = null;

		public XMLComponentHandler(String uri, Attributes attributes, ExperimentXML exp_xml) { this(uri, attributes, exp_xml, null);}
		public XMLComponentHandler(String uri, Attributes attributes, ExperimentXML exp_xml, Topology topo)
		{
			super();
			typeName = new String(attributes.getValue(uri, ExperimentXML.TYPENAME));
			//typeCode = Integer.parseInt(attributes.getValue(uri, ExperimentXML.TYPECODE));
			ExpCoordinator.print(new String("ONLComponent.XMLComponentHandler constructor type " + typeName), 3);
			expXML = exp_xml;
			topology = topo;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			currentElement = new String(localName);
			ExpCoordinator.print(new String("ONLComponent.XMLComponentHandler.startElement " + localName), 3);
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("ONLComponent.XMLComponentHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), 3);
			if (currentElement.equals(ExperimentXML.LABEL)) 
			{
				if (topology == null)
					component = expXML.getExperiment().getTopology().getONLComponent(new String(ch, start, length), typeName);
				else
					component = topology.getONLComponent(new String(ch, start, length), typeName);

			}
		}
		public void setCurrentElement(String s) { currentElement = new String(s);}
		public String getCurrentElement() { return currentElement;}
		public ONLComponent getComponent() { return component;}
	}

	/////////////////////////////////////////////////////FocusListener/////////////////////////////////////////////////////////////////

	public static class ONLInternalFrameListener extends InternalFrameAdapter
	{
		private ONLComponent component = null;
		public ONLInternalFrameListener(ONLComponent c)
		{
			super();
			component = c;
		}

		private void fire()
		{
			component.fireEvent(new ONLComponent.Event(ONLComponent.Event.GET_FOCUS, component, "Get Focus"));
		}
		public void internalFrameActivated(InternalFrameEvent e) {fire();}
		public void internalFrameDeiconified(InternalFrameEvent e) {fire();}
		public void internalFrameOpened(InternalFrameEvent e) {fire();}

	}
	public static class ONLFrameListener extends WindowAdapter
	{
		private ONLComponent component = null;
		public ONLFrameListener(ONLComponent c)
		{
			super();
			component = c;
		}
		private void fire()
		{
			component.fireEvent(new ONLComponent.Event(ONLComponent.Event.GET_FOCUS, component, "Get Focus"));
		}
		public void windowActivated(WindowEvent e) {fire();}
		public void windowDeiconified(WindowEvent e) {fire();}
		public void windowOpened(WindowEvent e) {fire();}

	}
	/////////////////////////////////////////////////////Event/////////////////////////////////////////////////////////////////
	public static class Event extends EventObject
	{
		public static final int STATE_CHANGE = 0;
		public static final int VALUE_CHANGE = 1;
		public static final int TABLE_ADD = 2;
		public static final int TABLE_DELETE = 3;
		public static final int TABLE_CHANGE = 4;
		public static final int PROPERTY_CHANGE = 5;
		public static final int GRAPHIC_MOVE = 6;
		public static final int GET_FOCUS = 7;
		public static final int TABLE_COMMIT = 8;
		public static final int CONNECTION = 16;
		private String label = null;

		private int type = STATE_CHANGE;

		public Event(ONLComponent c, int tp, ONL.Reader rdr) throws IOException
		{ 
			super(c);
			type = tp;
			read(rdr);
		}
		public Event(int tp, ONLComponent c) { this(tp, c, null);}
		public Event(int tp, ONLComponent c, String lbl)
		{
			super(c);
			type = tp;
			if (lbl != null) label = new String(lbl);
		}

		public int getType() { return type;}
		public void setType(int t) { type = t;}
		public int getIndex() { return 0;}
		public ONLComponent getONLComponent() { return ((ONLComponent)getSource());}
		public String toString()
		{
			if (label != null) return label;
			else return (super.toString());
		}
		public String getLabel() { return label;}
		public void setLabel(String lbl) 
		{ 
			label = new String(lbl); 
			getONLComponent().setProperty(LABEL, lbl);
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			ExpCoordinator.print(new String(getONLComponent().toString()+".Event.write"), 8);
			wrtr.writeString(getONLComponent().toString());
			wrtr.writeInt(type);
			wrtr.writeString(label);
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			//type = rdr.readInt();
			label = rdr.readString();
		}
	}


	/////////////////////////////////////////////////////PropertyEvent/////////////////////////////////////////////////////////////////
	public static class PropertyEvent extends Event
	{ 
		private String oldValue ; //not valid for add and delete
		private String newValue; //not valid for add and delete

		public PropertyEvent(ONLComponent c, int tp, ONL.Reader rdr) throws IOException
		{ 
			super(c, tp, rdr);
		}
		public PropertyEvent(int tp, ONLComponent c, String oval, String nval, String lbl)
		{
			super(tp, c, lbl);
			oldValue = oval;
			newValue = nval; 
		}

		public PropertyEvent(ONLComponent c, PropertyChangeEvent pce) { this(c, pce, Event.PROPERTY_CHANGE);}
		public PropertyEvent(ONLComponent c, PropertyChangeEvent pce, int tp) { this(tp, c, (String)pce.getOldValue(), (String)pce.getNewValue(), pce.getPropertyName());}
		public String getOldValue() { return oldValue;}
		public String getNewValue() { return newValue;}
		public void write(ONL.Writer wrtr) throws IOException
		{
			ExpCoordinator.print(new String(getONLComponent().toString()+".PropertyEvent.write oval = " + oldValue + ", nval = " + newValue), 6);
			super.write(wrtr);
			wrtr.writeString(oldValue);
			wrtr.writeString(newValue);
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			super.read(rdr);
			oldValue = rdr.readString();
			newValue = rdr.readString();
			ExpCoordinator.print(new String(getONLComponent().toString()+".PropertyEvent.read oval = " + oldValue + ", nval = " + newValue), 6);
		}
	}

	//////////////////////////////////////////////////// StateEvent ///////////////////////////////////////////////////////////////////
	public static class StateEvent extends ONLComponent.PropertyEvent
	{
		private int subtype ;   
		public StateEvent(ONLComponent c, int tp, ONL.Reader rdr) throws IOException
		{ 
			super(c, tp, rdr);
		}
		public StateEvent(int stp, ONLComponent c) { this(stp, c, null, null, "StateEvent");}
		public StateEvent(int stp, ONLComponent c, String oval, String nval, String lbl) 
		{ 
			super(Event.STATE_CHANGE, c, oval, nval, lbl);
			subtype = stp;
		}
		public StateEvent(ONLComponent c, PropertyChangeEvent pce) { super(c, pce, Event.STATE_CHANGE);}
		public int getSubtype() { return subtype;}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeInt(subtype);
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			super.read(rdr);
			subtype = rdr.readInt();
		}
	}// end StateEvent

	/////////////////////////////////////////////////////// TableEvent ////////////////////////////////////////////////////////////////////
	public static class TableEvent extends ONLComponent.PropertyEvent
	{
		private ONLCTable.Element tableEntry = null;
		private int index;
		private boolean committed = false;

		public TableEvent(ONLComponent c, int tp, ONL.Reader rdr) throws IOException { super(c, tp, rdr);}
		public TableEvent(int tp, ONLComponent c, ONLCTable.Element te, int i, String oval, String lbl)
		{
			super(tp, c, oval, te.toString(), lbl);
			tableEntry = te;
			index = i;
		}

		public ONLCTable.Element getTableEntry() { return tableEntry;}
		public void setTableEntry(ONLCTable.Element elem) { tableEntry = elem;}
		public int getIndex() { return index;}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeInt(index);
			wrtr.writeBoolean(!tableEntry.needsCommit());
			ExpCoordinator.print(new String("ONLComponent.TableEvent.write index " + index), 8);
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			super.read(rdr);
			index = rdr.readInt();
			committed = rdr.readBoolean();
			ExpCoordinator.print(new String("ONLComponent.TableEvent.read index " + index), 8);
		}
		public boolean isCommitted() { return committed;}
	}//end TableEvent

	////////////////////////////////////////////////////////// ConnectionEvent /////////////////////////////////////////////////////////////
	public static class ConnectionEvent extends ONLComponent.Event
	{
		public static final int CONN_OPEN = ONLComponent.Event.CONNECTION + NCCPConnection.ConnectionEvent.CONNECTION_OPENED;
		public static final int CONN_CLOSE = ONLComponent.Event.CONNECTION + NCCPConnection.ConnectionEvent.CONNECTION_CLOSED;
		public static final int CONN_FAIL = ONLComponent.Event.CONNECTION + NCCPConnection.ConnectionEvent.CONNECTION_FAILED;
		public short connectionID;
		public int daemonType;

		public ConnectionEvent(ONLComponent c, int tp, ONL.Reader rdr) throws IOException { super(c, tp, rdr);}
		public ConnectionEvent(ONLComponent c, int dt, short cid) { this(c, dt, cid, CONN_OPEN);}
		public ConnectionEvent(ONLComponent c, int dt, short cid, int tp)
		{
			super(tp, c);
			daemonType = dt;
			connectionID = cid;
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			ExpCoordinator.print(new String(getONLComponent().toString()+".ConnectionEvent.write"), 8);
			wrtr.writeString(getONLComponent().toString());
			wrtr.writeInt(getType());
			wrtr.writeInt(daemonType);
			wrtr.writeShort(connectionID);
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			ExpCoordinator.print(new String(getONLComponent().toString()+".ConnectionEvent.read"), 6);
			//setType(rdr.readInt());
			daemonType = rdr.readInt();
			connectionID = (short)rdr.readShort();
			ExpCoordinator.print(new String("         daemonType:" + daemonType + " connectionID:" + connectionID), 6);
		}
		public short getCID() { return connectionID;}
		public ONLDaemon getDaemon() { return (getONLComponent().getDaemon(daemonType));}
		public int getDaemonType() { return daemonType;}
	}


	/////////////////////////////////////////////////////////////// MoveEvent ///////////////////////////////////////////////////////////////////////////////////////
	public static class MoveEvent extends ONLComponent.Event
	{
		public MoveEvent(ONLComponent c, int tp, ONL.Reader rdr) throws IOException { super(c, tp, rdr);}
		public MoveEvent(ONLComponent c)
		{
			super(Event.GRAPHIC_MOVE, c);
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			ONLComponent onl_component = getONLComponent();
			ExpCoordinator.print(new String(onl_component.toString()+".MoveEvent.write"), 8);
			wrtr.writeString(onl_component.toString());
			wrtr.writeInt(getType());
			ONLGraphic elem_graph = onl_component.getGraphic();
			if (elem_graph != null)
			{
				wrtr.writeInt(elem_graph.getX());
				wrtr.writeInt(elem_graph.getY());
				ExpCoordinator.print(new String("ONLComponent.MoveEvent.write " + onl_component.getLabel() + " x:" + elem_graph.getX() + " y:" +  elem_graph.getY()), 7);
				if (onl_component instanceof Hardware && !(onl_component instanceof HardwareHost))
				{
					int spinner = ((HardwareGraphic)elem_graph).getSpinnerPosition();
					wrtr.writeInt(spinner);
				}
			}
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			ONLComponent onl_component = getONLComponent();
			ONLGraphic elem_graph = onl_component.getGraphic();
			//setType(rdr.readInt());
			if (elem_graph != null)
			{
				int x = rdr.readInt();
				int y = rdr.readInt();
				ExpCoordinator.print(new String("ONLComponent.MoveEvent.read " + onl_component.getLabel() + " x:" + x + " y:" +  y), 7);
				elem_graph.setLocation(x, y);
				if ((onl_component instanceof Hardware))
				{
					((HardwareGraphic)elem_graph).setSpinnerPosition(rdr.readInt());
				}
			}
		}
	}

	//////////////////////////////////////////// Listener ////////////////////////////////////////////////////////////////////

	public static interface Listener
	{
		public void onlcStateChanged(ONLComponent.PropertyEvent e);
		public void onlcValueChanged(ONLComponent.Event e);
		public void onlcTableChanged(ONLComponent.TableEvent e);
		public void onlcConnectionChanged(ONLComponent.ConnectionEvent e);
		public void onlcGraphicMoved(ONLComponent.MoveEvent e);
	}

	//////////////////////////////////////////// State Listener //////////////////////////////////////////////////////////////

	private class StateListener implements PropertyChangeListener
	{
		private ONLComponent onlComponent = null;
		public StateListener(ONLComponent c)
		{
			onlComponent = c;
		}
		public void propertyChange(PropertyChangeEvent evt)
		{
			if (evt.getPropertyName().equals(STATE))
			{
				onlComponent.fireEvent(new ONLComponent.StateEvent(onlComponent, evt));//onlComponent, ((String)evt.getOldValue()), ((String)evt.getNewValue()), "state change"));
			}
			else 
			{
				onlComponent.fireEvent(new ONLComponent.PropertyEvent(onlComponent, evt));//onlComponent, 
			}
		}
	}


	/////////////////////////////////////////////GraphicsListener////////////////////////////////////////////////////////////

	private class GraphicsListener extends ComponentAdapter
	{
		private ONLComponent onlComponent = null;
		public GraphicsListener(ONLComponent c)
		{
			super();
			onlComponent = c;
		}

		public void componentMoved(ComponentEvent e) 
		{
			onlComponent.fireEvent(new ONLComponent.MoveEvent(onlComponent));
		}
	}

	/////////////////////////////////////////////ConnectionListener///////////////////////////////////////////////////////////
	private class ConnectionListener implements NCCPConnection.ConnectionListener
	{
		private ONLComponent onlComponent = null;
		public ConnectionListener(ONLComponent c)
		{
			onlComponent = c;
		}
	        public void connectionFailed(NCCPConnection.ConnectionEvent e){}
		public void connectionClosed(NCCPConnection.ConnectionEvent e) {}
		public void connectionOpened(NCCPConnection.ConnectionEvent e)
		{
			ONLDaemon od = (ONLDaemon)e.getSource();
			onlComponent.fireEvent(new ConnectionEvent(onlComponent, od.getDaemonType(), od.getConnectionID()));
			ExpCoordinator.print(new String(onlComponent.getLabel() + ".ConnectionListener.connectionOpened"), 1);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface PortInterface
	{
		public static interface Index
		{
			public String getIPAddr();
			public int getIndex();
			public String getLabel();
			public PortInterface getPortInterface();
			public String toString();
		}
		public ONLComponent getParent();
		public int getID();
		/*//SUBNET:remove
    public PortInterface.Index getNewIndex();
    public void freeIndex(PortInterface.Index in);
    public boolean isUsedUp();//are there any subnet addresses available
    public int getNumFreeIndices();
		 */
		public String getProperty(String pnm);
		public String getInterfaceType();
		public void setSubnet(SubnetManager.Subnet s);
		public SubnetManager.Subnet getSubnet();

	}//end PortInterface

	public static abstract class PortBase extends ONLComponent implements PortInterface
	{
		public static final String PORTID = "portid";
		public static final String NEXTHOP = "nexthop";
		//private SubnetManager.Subnet subnet = null;
		//private PortInterface subNetRouter = null;
		public PortBase(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException { super(lbl,tp,tr,ec);}
		public PortBase(ONLComponent p, String lbl, String tp, ExpCoordinator ec) { super(p,lbl,tp,ec);}
		public PortBase(String lbl, String tp, ExpCoordinator ec) { super(lbl,tp,ec);}
		public void setID(int p) { setProperty(PORTID, p);}
		public int getID() { return (getPropertyInt(PORTID));}
		//SUBNET:remove //public abstract PortInterface.Index getNewIndex();
		public boolean isRouter() { return (getParent().isRouter());}
		public boolean isSwitch() { return (getParent().isSwitch());}
		public boolean isEndpoint() { return (getParent().isEndpoint());}
		/*//SUBNET:remove
    public abstract void freeIndex(PortInterface.Index in);
    public abstract int getNumFreeIndices();
		 */
		public void setState(String st) { setProperty(STATE, st);}

		public String getInterfaceType()
		{
			String rtn = properties.getProperty(INTERFACE_TYPE);
			if (rtn != null) return rtn;
			else return ExperimentXML.ITYPE_1G;
		}
		public void setInterfaceType(String s) 
		{ 
			ExpCoordinator.print(new String("ONLComponent.PortBase(" + getLabel() + ").setInterfaceType " + s), TEST_10G);
			properties.setProperty(INTERFACE_TYPE, s);
			addToDescription(INTERFACE_TYPE, s);
		}
		public int getBandwidth() //return port bandwidth in Mbps based on interface type
		{
			int m = 1;
			String interfaceType = getInterfaceType();

			if (interfaceType.endsWith("G")) m = 1000;
			int end = interfaceType.length() - 1;
			int rtn = Integer.parseInt(interfaceType.substring(0,end));
			rtn = rtn * m;
			return rtn;
		}
		/*
    public void setNetmask(int m) 
    {
      ExpCoordinator.print(new String("ONLComponent.PortBase(" + getLabel() + ").setNetmask " + m), TEST_SUBNET);
      properties.setProperty(NETMASK, m);
      addToDescription(ONLComponent.IPADDR, new String(getIPAddr() + "/" + m));
    }*/
		public int getNetmask() 
		{
			int m = 0;
			if (getSubnet() != null)  m = getSubnet().getNetmask().getBitlen();
			ExpCoordinator.print(new String("ONLComponent.PortBase(" + getLabel() + ").getNetmask " + m), TEST_SUBNET);
			return m;
		}
		public String getNetmaskString() 
		{ 
			if (getSubnet() != null) return (getSubnet().getNetmask().toString());
			else return null;
		}
		public void setSubnet(SubnetManager.Subnet s) 
		{
			this.subnet = s;
			if (s != null)
			{
				if (s.getNetmask() == null) ExpCoordinator.print(new String("ONLComponent.PortBase(" + getLabel() + ").setSubnet " + s.getBaseIP() + " netmask null"));
				addToDescription(ONLComponent.IPADDR, new String(getIPAddr().toString() + "/" + s.getNetmask().getBitlen()));
				addToDescription(ONLComponent.SUBNET, new String("subnet:" + s.getBaseIP().toString()));// s.getIndex()));
			}
			else 
			{
			    addToDescription(ONLComponent.SUBNET, "subnet:none");
			}
			if (isSwitch()) getParent().setSubnet(s);// && originalSubnet == null) originalSubnet = s;
		}	

		public SubnetManager.Subnet getSubnet() 
		{ 
			if (isSwitch()) return (getParent().getSubnet());
			return (this.subnet);
		}
		public SubnetManager.Subnet getOriginalSubnet() 
		{ 
			if (isSwitch()) return (getParent().getOriginalSubnet());//originalSubnet;}
			return (super.getOriginalSubnet());
		}
		public ONL.IPAddress getNHIPAddr()
		{
			ONLComponent c = getLinkedTo();
			//if (c.isRouter() && isRouter()) return (c.getProperty(ONLComponent.NHIPADDR));
			//String rtn = "0.0.0.0";
			ONL.IPAddress rtn = new ONL.IPAddress();
			if (c != null && c instanceof ONLComponent.PortInterface)
			{
				//CHANGED 9/7/10 //if (isRouter() && c.isRouter()) rtn = c.getIPAddr();
				if (c.isRouter()) rtn = c.getIPAddr();
				//else
				//{
				//	rtn = (((PortInterface)c).getSubnet().getBase());//.toString());
				//}
			}
			ExpCoordinator.print(new String("ONLComponent(" + getLabel() + ").PortBase.getNHIPAddr rtn:" + rtn), TEST_SUBNET);
			return rtn;
		}
		public void setIPAddr(String ip) 
		{
			super.setIPAddr(ip);
			addToDescription(ONLComponent.IPADDR, new String(ip));// + "/" + getNetmask()));
			if (properties.getProperty(ORIG_IPADDR) == null) properties.setProperty(ORIG_IPADDR, ip);
		}
		public void clear()
		{	
			if (subnet != null)
			{
				subnet.clearPort(this);
				subnet = null;
			}
		}

		protected Vector<ONLComponent.PortBase> getConnectedPorts()  //used by OldSubnetManager to get all of the ports connected through a switch
		{ 
			if (isSwitch()) return (getParent().getConnectedPorts());
			else return null;
		}
	}

	public static interface Undoable extends UndoableEdit, ActionListener
	{
		public void commit();
		public boolean isCancelled(ONLComponent.Undoable un);
	}//end Undoable


	/////////////////////////////////////////////////////// ONLComponent.CfgMonMenu interface ///////////////////////////////////////////
	protected static interface CfgMonMenu 
	{
		public void addCfg(Action a);
		public void addCfg(Action a, Font f);
		public void addCfg(JMenuItem mi);
		public void addMon(Action a);
		public void addMon(Action a, Font f);
		public void addMon(JMenuItem mi);
		public void addDescription(ONLPropertyList desc);
		public void removeDescription();
		public void showMenu(JComponent c, Point2D pnt) ;
	}//end CfgMonMenu

	/////////////////////////////////////////////////////// ONLComponent.CfgMonPopupMenu ///////////////////////////////////////////

	protected static class CfgMonPopupMenu extends JPopupMenu implements ONLComponent.CfgMonMenu
	{
		private ONLComponent onlComponent = null;
		private int titleLen = 2;
		private int descLen = 0;
		private int cfgLen = 0;
		private int monLen = 0;
		private Color hdrBG = null;
		private Font defaultFnt = null;

		public static class Header extends JPanel
		{
			private JLabel lbl = null;
			public Header(String s, Color bg)
			{
				this(s, null, bg, null);
			}
			public Header(String s, Color fg, Color bg, Font f)
			{
				super();
				setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				if (bg != null) setBackground(bg);
				lbl = new JLabel(s);
				//lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
				if (fg != null) lbl.setForeground(fg);
				if (f != null) lbl.setFont(f);
				add(lbl);
				add(Box.createHorizontalGlue());
			}
		}

		public CfgMonPopupMenu(ONLComponent c) { this(c, new String(c.getLabel()));}
		public CfgMonPopupMenu(ONLComponent c, String ttl)
		{
			super(ttl);
			onlComponent = c;  
			hdrBG = new Color(130, 163, 167);
			defaultFnt = new Font("Dialog", Font.BOLD, 10);
			add(new Header(ttl, null, hdrBG, new Font("Dialog", Font.BOLD, 14)));
			addSeparator();  
		}
		public void addCfg(Action a)
		{
			addCfg(a, defaultFnt);
		}
		public void addCfg(Action a, Font f)
		{
			JMenuItem mi = new JMenuItem(a);
			if (f != null) mi.setFont(f);
			addCfg(mi);
		}
		public void addCfg(JMenuItem mi)
		{
			if (cfgLen <= 0)
			{
				insert(new JPopupMenu.Separator(), (cfgLen + descLen + titleLen));
				++cfgLen;
				insert(new Header("Configuration", hdrBG), (cfgLen + descLen + titleLen));
				++cfgLen;
				insert(new JPopupMenu.Separator(), (cfgLen + descLen + titleLen));
				++cfgLen;
			}
			insert(mi, (cfgLen + descLen + titleLen));
			++cfgLen;
		}
		public void addMon(Action a)
		{
			addMon(a, defaultFnt);
		}
		public void addMon(Action a, Font f)
		{
			JMenuItem mi = new JMenuItem(a);
			if (f != null) mi.setFont(f);
			addMon(mi);
		}
		public void addMon(JMenuItem mi)
		{
			if (monLen <= 0)
			{
				insert(new JPopupMenu.Separator(), (monLen + cfgLen + descLen + titleLen));
				++monLen;
				insert(new Header("Monitoring", hdrBG), (monLen + cfgLen + descLen + titleLen));
				++monLen;
				insert(new JPopupMenu.Separator(), (monLen + cfgLen + descLen + titleLen));
				++monLen;
			}
			insert(mi, (monLen + descLen + cfgLen + titleLen));
			++monLen;
		}
		public void addDescription() { addDescription(onlComponent.description);}
		public void addDescription(ONLPropertyList desc)
		{
			if (descLen > 0) removeDescription();
			Iterator desc_it = desc.values().iterator();
			int add_at = titleLen;
			Font fnt = new Font("Dialog", Font.PLAIN, 9);
			while(desc_it.hasNext())
			{
				add(new Header((String)desc_it.next(), null, null, fnt), add_at);
				++add_at;
				++descLen;
			}
		}
		public void removeDescription()
		{
			while(descLen > 0)
			{
				remove(titleLen);
				--descLen;
			}
		}
		public void showMenu(JComponent c, Point2D pnt) 
		{
			show(c, (int)pnt.getX(), (int)pnt.getY());
			setVisible(true);
		}
	}//end CfgMonPopupMenu

	/////////////////////////////////////////////////////// ONLComponent.CfgMonJMenu ///////////////////////////////////////////

	protected static class CfgMonJMenu extends JMenu implements ONLComponent.CfgMonMenu
	{
		private ONLComponent onlComponent = null;
		private int descLen = 0;
		private int cfgLen = 0;
		private int monLen = 0;
		private Font defaultFnt = null;


		public CfgMonJMenu(ONLComponent c) { this(c, new String(c.getLabel()));}
		public CfgMonJMenu(ONLComponent c, String ttl)
		{
			super(ttl);
			onlComponent = c;  
		}
		public void addCfg(Action a)
		{
			addCfg(a, defaultFnt);
		}
		public void addCfg(Action a, Font f)
		{
			JMenuItem mi = new JMenuItem(a);
			if (f != null) mi.setFont(f);
			addCfg(mi);
		}
		public void addCfg(JMenuItem mi)
		{
			if (cfgLen <= 0)
			{
				addSeparator();
				++cfgLen;
				add(new JLabel("Configuration"));
				++cfgLen;
				addSeparator();
				++cfgLen;
			}
			add(mi);
			++cfgLen;
		}
		public void addMon(Action a)
		{
			addMon(a, defaultFnt);
		}
		public void addMon(Action a, Font f)
		{
			JMenuItem mi = new JMenuItem(a);
			if (f != null) mi.setFont(f);
			addMon(mi);
		}
		public void addMon(JMenuItem mi)
		{
			if (monLen <= 0)
			{
				addSeparator();
				++monLen;
				add(new JLabel("Monitoring"));
				++monLen;
				addSeparator();
				++monLen;
			}
			add(mi);
			++monLen;
		}
		public void addDescription() { addDescription(onlComponent.description);}
		public void addDescription(ONLPropertyList desc)
		{
			if (descLen > 0) removeDescription();
			Iterator desc_it = desc.values().iterator();
			int add_at = 0;
			Font fnt = new Font("Dialog", Font.PLAIN, 9);
			while(desc_it.hasNext())
			{
				JLabel lbl = new JLabel((String)desc_it.next());
				lbl.setFont(fnt);
				add(lbl, add_at);
				++add_at;
				++descLen;
			}
		}
		public void removeDescription()
		{
			while(descLen > 0)
			{
				remove(0);
				--descLen;
			}
		}
		public void showMenu(JComponent c, Point2D pnt) {}
	}//end CfgMonJMenu


	///////////////////////////////////////////// ONLComponent methods ///////////////////////////////////////////////////////////////////////
	public ONLComponent(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{
		this(lbl,tp,ec);
		if (tr.getVersion() > 3.0)
		{
			properties.setProperty(REF_NUM, tr.readInt());
			int tmp_int = tr.readInt();
			if (tmp_int > 0)
				properties.setProperty(CLUSTER_REF, tmp_int);
		}
	}

	public ONLComponent(ONLComponent p, String lbl, String tp, ExpCoordinator ec)
	{
		this((new String(p.getLabel() + ":" + lbl)), tp, ec);
		setParent(p);//parent = p;
	}
	public ONLComponent(String lbl, String tp, ExpCoordinator ec)
	{
		//label = lbl;
		//type = tp;
		expCoordinator = ec;
		description = new ONLPropertyList(this);
		children = new ONLComponentList();
		//addToDescription(LABEL, label);
		//addToDescription(TYPE, getTypeString(tp));
		properties = new ONLPropertyList(this);
		properties.setProperty(SELECTED, false);
		properties.setProperty(STATE, NOT_INIT);
		properties.setProperty(LABEL, lbl);
		properties.setProperty(TYPE, tp);
		properties.setProperty(REMAP_SENT, true);
		properties.setProperty(PORTS_INIT, false);
		eventListeners = new Vector();
		//add property listener to send on state change;
		stateListener = new StateListener(this);
		propertiesToWatch = new Vector();
		addStateListener(STATE);
	}

	public ONLComponent(String uri, Attributes attributes)
	{
		expCoordinator = ExpCoordinator.theCoordinator;
		description = new ONLPropertyList(this);
		children = new ONLComponentList();
		properties = new ONLPropertyList(this);
		properties.setProperty(SELECTED, false);
		properties.setProperty(STATE, NOT_INIT);
		properties.setProperty(TYPE, attributes.getValue(uri, ExperimentXML.TYPENAME));
		properties.setProperty(PORTS_INIT, false);
		String str = attributes.getValue(uri, ExperimentXML.CTYPE);
		if (str != null) setComponentType(str);
		//properties.setProperty(TYPENAME, attributes.getValue(uri, ExperimentXML.TYPENAME));
		properties.setProperty(REMAP_SENT, true);
		eventListeners = new Vector();
		//add property listener to send on state change;
		stateListener = new StateListener(this);
		propertiesToWatch = new Vector();
		addStateListener(STATE);
	}

	public void setParent(ONLComponent p) { parent = p;}
	public ONLComponent getParent() { return parent;}
	public void addChild(ONLComponent c) 
	{ 
		children.addComponent(c);
		ONLComponent root_c = getRootComponent();
		root_c.addPropertyListener(LABEL, c);
		ExpCoordinator.print(new String("ONLComponent(" + getLabel() + ").addChild " + c.getLabel() + " rootComponent:" + root_c.getLabel()), TEST_LABEL_CHANGE);
	}
	public ONLComponent getChild(String lbl, String tp)
	{
		return (getChild(lbl, tp, 0));
	}
	public ONLComponent getChild(String lbl, String tp, int level)
	{
		//System.out.println(getLabel() + "::getChild " + lbl + " " + level);
		return (children.getONLComponent(lbl,tp,level));
	}
	public String getLabel() { return ((String)properties.getProperty(LABEL));}
	public String getType() 
	{  
		if (properties.getProperty(TYPE) != null) return (properties.getProperty(TYPE));
		return (UNKNOWN_LBL);
	}
	//return (properties.getProperty(TYPE));}
	public void setType(String s) { properties.setProperty(TYPE, s);}
	public String getBaseType() { return (getType());}
	public boolean isType(String tp) { return (getType().equals(tp));}
	public boolean isRouter() 
	{ 
		String str = getProperty(COMPONENT_TYPE);
		return (str != null && str.equals(ExperimentXML.ROUTER));
	}
	public boolean isSwitch() 
	{ 
		String str = getProperty(COMPONENT_TYPE);
		return (str != null && str.equals(ExperimentXML.SWITCH));
	}
	public boolean isEndpoint() 
	{ 
		String str = getProperty(COMPONENT_TYPE);
		return (str != null && str.equals(ExperimentXML.ENDPOINT));
	}
	public String getComponentType() { return (getProperty(COMPONENT_TYPE));}
	public void setComponentType(String ctype) 
	{ 
		setProperty(COMPONENT_TYPE, ctype);
		if (ctype != null) addToDescription(COMPONENT_TYPE,ctype);
		ExpCoordinator.print(new String("ONLComponent(" + getLabel() + ").setComponentType " + ctype), TEST_CTYPE);
	}
	public String toString() { return (new String(getLabel() + " " + getType()));}
	public void writeExpDescription(ONL.Writer tw) throws IOException     
	{ 
		tw.writeString(toString());
		tw.writeInt(getReference());
		tw.writeInt(getClusterReference());
		if (parent != null) 
		{
			tw.writeString(parent.toString());
		}
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException { writeXMLID(xmlWrtr);}
	public void writeXMLID(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeAttribute(ExperimentXML.TYPENAME, getType());//"numPorts", String.valueOf(numPorts));
		String tmp_str = getComponentType();
		if (tmp_str != null) xmlWrtr.writeAttribute(ExperimentXML.CTYPE, tmp_str);
		xmlWrtr.writeStartElement(ExperimentXML.LABEL);
		xmlWrtr.writeCharacters(getLabel());
		xmlWrtr.writeEndElement(); //end label
		if (getProperty(ExperimentXML.USER_LABEL) != null)
		{
			xmlWrtr.writeStartElement(ExperimentXML.USER_LABEL);
			xmlWrtr.writeAttribute(ExperimentXML.VALUE, getUserLabel());
			xmlWrtr.writeEndElement(); //end label
		}
		if (iconColor != null)
		{
			xmlWrtr.writeStartElement(ExperimentXML.COLOR);
			xmlWrtr.writeAttribute("r", String.valueOf(iconColor.getRed()));
			xmlWrtr.writeAttribute("g", String.valueOf(iconColor.getGreen()));
			xmlWrtr.writeAttribute("b", String.valueOf(iconColor.getBlue()));
			xmlWrtr.writeEndElement(); //end label
		}
		if (properties.getProperty(REF_NUM) != null)
		{
			xmlWrtr.writeStartElement(REF_NUM);
			xmlWrtr.writeCharacters(properties.getProperty(REF_NUM));
			xmlWrtr.writeEndElement();
		}
		if (getCluster() != null)
		{
			xmlWrtr.writeStartElement(CLUSTER_REF);
			xmlWrtr.writeCharacters(String.valueOf(getCluster().getIndex()));
			xmlWrtr.writeEndElement();
		}
		ExpCoordinator.print(new String("ONLComponent.writeXMLID " + getComponentID()));
	}
	public ONLMonitorable getMonitorable() { return monitorable;}
	public ExpCoordinator getExpCoordinator() 
	{ 
		if (expCoordinator == null) expCoordinator = ExpCoordinator.theCoordinator;
		return expCoordinator;
	}
	protected void setExpCoordinator(ExpCoordinator ec) { expCoordinator = ec;}

	//interface ListDataListener 
	//not sure if should do something if this is removed from the component list. for now don't do anything
	public void contentsChanged(ListDataEvent e) {}
	public void intervalAdded(ListDataEvent e) {}
	public void intervalRemoved(ListDataEvent e) {} 
	//end interface ListDataListener

	public ONLMenuItem getMenuItem() { return (new ONLMenuItem(this));}
	public ONLGraphic getGraphic() { return null;} //components that have graphical representation should return that object here
	public boolean isMonitoring() { return (expCoordinator.isMonitoring());}
	public Action getRemoveAction() {return removeAction;}
	public void setRemoveAction(Action a) { removeAction = a;}
	public boolean isLabel(String lbl)
	{
		return (getLabel().equals(lbl));
	}
	public boolean isEqual(ONLComponent c) { return (isEqual(c.getLabel(), c.getType()));}
	public boolean isEqual(String lbl, String tp) 
	{
		if (isLabel(lbl))
		{ 
			if ((isType(tp)) || isAncestor(tp)) return true;
		}
		return false;
	}
	protected ONLPropertyList getDescription() { return description;}
	//public Object[] getDescArray() { return (description.toArray());}
	//public Vector getDescVector() { return (description);}
	protected void addToDescription(String key, String d) { description.setProperty(key, d);}
	protected void addToDescription(int index, String d) { addToDescription(String.valueOf(index), d);}
	protected void showDescription(JComponent c, Point2D pnt)
	{
		//System.out.println("showDescription " + description.size() + " values");
		PopupList pl = new PopupList(properties.getProperty(LABEL));
		Iterator desc_it = description.values().iterator();
		while(desc_it.hasNext())
		{
			pl.addItem((String)desc_it.next());
		}
		pl.show(c, (int)pnt.getX(), (int)pnt.getY());
		pl.setVisible(true);
	}
	public void setParentFromString(String str)
	{
		setParent(expCoordinator.getComponentFromString(str));
	}
	public boolean isLinkable() { return linkable;}
	protected void setLinkable(boolean b) { linkable = b;}
	public boolean isLinked()
	{
		return (linkedTo != null);
		//return (expCoordinator.getTopology().isLinked(this));
	}
	//edit actions
	protected ONLComponent.Undoable getDeleteEdit() { return (new Experiment.DeleteNodeEdit(this, expCoordinator.getCurrentExp()));}

	public boolean isSelected() 
	{ 
		if (parent != null && parent.isSelected()) return true;
		else return (Boolean.valueOf(properties.getProperty(SELECTED)).booleanValue());
	}
	public void setSelected(boolean b) { properties.setProperty(SELECTED, b);}
	public void setLabel(String s) { properties.setProperty(LABEL, s);}
	public String getState() { return ((String)properties.getProperty(STATE));}
	public boolean isActive() { return (((String)(properties.getProperty(STATE))).equals(ACTIVE));}
	public boolean isFailed() { return (((String)(properties.getProperty(STATE))).equals(FAILED));}
	public boolean isWaiting() { return (((String)(properties.getProperty(STATE))).equals(WAITING));}
	public boolean isUnused() { return (((String)(properties.getProperty(STATE))).equals(NOT_INIT));}
	public boolean isPropertyEqual(String nm, String o) { return (((String)(properties.getProperty(nm))).equals(o));}

	public void setProperty(String nm, String o) { properties.setProperty(nm, o);}
	public void setProperty(String nm, int o) { properties.setProperty(nm, o);}
	public void setProperty(String nm, boolean o) { properties.setProperty(nm, o);}
	public void setProperty(String nm, double o) { properties.setProperty(nm, o);}
	public String getProperty(String nm) { return (properties.getProperty(nm));}
	public int getPropertyInt(String nm) { return (properties.getPropertyInt(nm));}
	public double getPropertyDouble(String nm) { return (properties.getPropertyDouble(nm));}
	public boolean getPropertyBool(String nm) { return (properties.getPropertyBool(nm));}
	public void addPropertyListener(PropertyChangeListener l) { properties.addListener(l);}
	public void addPropertyListener(String pname, PropertyChangeListener l) { properties.addListener(pname, l);}
	public void addPropertyListener(String pnames[], PropertyChangeListener l) 
	{
		int max = Array.getLength(pnames);
		for (int i = 0; i < max; ++i)
			properties.addListener(pnames[i], l);
	}
	public void removePropertyListener(PropertyChangeListener l) { properties.removeListener(l);}
	public void removePropertyListener(String pname, PropertyChangeListener l) { properties.removeListener(pname, l);}
	public void removePropertyListener(String pnames[], PropertyChangeListener l) 
	{
		int max = Array.getLength(pnames);
		for (int i = 0; i < max; ++i)
			properties.removeListener(pnames[i], l);
	}
	public void addStateListener(String pnames[]) 
	{ 
		int max = Array.getLength(pnames);
		for (int i = 0; i < max; ++i)
			addStateListener(pnames[i]);
	}
	public void addStateListener(String pname) 
	{ 
		ExpCoordinator.print(new String(getLabel() + ".ONLComponent.addStateListener to " + pname), 4);
		properties.addListener(pname, stateListener);
		if (!propertiesToWatch.contains(pname)) 
			propertiesToWatch.add(pname);
	}
	public void addStateListener() { addPropertyListener(stateListener);}
	public void removeStateListener(String pnames[]) 
	{ 
		int max = Array.getLength(pnames);
		for (int i = 0; i < max; ++i)
			removeStateListener(pnames);
	}
	public void removeStateListener(String pname) 
	{ 
		removePropertyListener(pname, stateListener);
		propertiesToWatch.remove(pname);
	}
	public void removeStateListener() { removePropertyListener(stateListener);}

	//Linkable
	//called when added to link
	public void addLink(LinkDescriptor lnk) 
	{
		ONLComponent olnk = linkedTo;
		boolean second_lnk = false;
		if (lnk.getState().equals(IN2) && linkedTo != null) second_lnk = true;
		if (lnk.getPoint1() == this && !second_lnk)
		{
			linkedTo = lnk.getPoint2();
		}
		if (lnk.getPoint2() == this && !second_lnk)
		{
			linkedTo = lnk.getPoint1();
		}
		if (linkedTo == null || !linkedTo.isRouter()) setProperty(PortBase.NEXTHOP, "0.0.0.0");
		else setProperty(PortBase.NEXTHOP, linkedTo.getIPAddr().toString());

		if (linkedTo != null && !second_lnk)
		{
			/*//SUBNET:remove
	  if (!isRouter() && (linkedTo instanceof ONLComponent.PortInterface) &&
	      (linkedTo.getParent() != null && linkedTo.getParent().isRouter()))
	      setSubNetRouter((ONLComponent.PortInterface)linkedTo);
	  else
	    {
	       if (!isRouter() && linkedTo.getSubNetRouter() != null)
		   setSubNetRouter(linkedTo.getSubNetRouter());
		   }
			 */
			ExpCoordinator.print(new String("ONLComponent.addLink for " + getLabel() + " linkedTo " + linkedTo.getLabel() + " nexthop: " + getProperty(PortBase.NEXTHOP)), TEST_LINK);
		}
		else
			ExpCoordinator.print(new String("ONLComponent.addLink for " + getLabel() + " linkedTo null nexthop: " + getProperty(PortBase.NEXTHOP)), TEST_LINK);
		//if (olnk != linkedTo)
		//{
		//}

	}  
	public void removeLink(LinkDescriptor lnk) 
	{
		if ((lnk.getPoint1() == this && linkedTo == lnk.getPoint2()) ||
				(lnk.getPoint2() == this && linkedTo == lnk.getPoint1())) 
		{
			//SUBNET:remove//if (linkedTo == getSubNetRouter()) setSubNetRouter(null);
			linkedTo = null; 
			setProperty(PortBase.NEXTHOP, "0.0.0.0");
		}
	}
	//end Linkable
	public ONLComponent getLinkedTo() { return linkedTo;}
	public void readParams(ONL.Reader reader) throws IOException {} //reads in any extra params this does the mapping to real hardware
	public void writeParams(ONL.Writer writer) throws IOException {} //writes any extra params this does the mapping to real hardware
	public ONLDaemon getDaemon(int tp)
	{
		if (daemons != null)
		{
			int max = daemons.size();
			for (int i = 0; i < max; ++i)
			{
				ONLDaemon d = (ONLDaemon)daemons.elementAt(i);
				if (d.getDaemonType() == tp) return d;
			}
		}
		return null;
	}
	public void clear() 
	{
		if (monitorable != null) monitorable.clear();
		//clear listeners
		eventListeners.removeAllElements();
	}
	protected void clearDaemons()
	{
		if (daemons != null)
		{
			int max = daemons.size();
			for (int i = 0; i < max; ++i)
			{
				NCCPConnection conn = (NCCPConnection)daemons.elementAt(i);
				conn.close();
			}
			daemons = null;
		}
	} //define this if there's stuff you need to do if the component is deleted from the topology
	public boolean needToRemap()
	{
		String clabel = properties.getProperty(COMMITTED_LABEL);
		ExpCoordinator.print(new String("ONLComponent.needToRemap committed = " + clabel + " label = " + getLabel() + " remapSent = " + properties.getPropertyBool(REMAP_SENT)), 2);
		if (clabel != null && getLabel().equals(clabel)) return false;
		else return (!properties.getPropertyBool(REMAP_SENT));
	}
	public void setCommittedLabel(String l)
	{
		ExpCoordinator.print(new String("ONLComponent.setCommittedLabel committed = " + properties.getProperty(COMMITTED_LABEL) + " to label = " + l), 2);
		properties.setProperty(COMMITTED_LABEL, l);
		properties.setProperty(REMAP_SENT, false);
	}
	public String getCommittedLabel() { return (properties.getProperty(COMMITTED_LABEL));}
	public boolean isDescendant(ONLComponent c)
	{
		ONLComponent p = getParent();
		while (p != null)
		{
			if (p == c) return true;
			p = p.getParent();
		}
		return false;
	}

	private void fireEvent(ONLComponent.MoveEvent e)
	{
		ONLComponent.Listener elem;
		int max = eventListeners.size();
		ExpCoordinator.print(new String(getLabel() + " ONLComponent:fireEvent move event  #listeners = " + max), 8);
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLComponent.Listener)eventListeners.elementAt(i);
			elem.onlcGraphicMoved(e);
		}
	}

	private void fireEvent(ONLComponent.ConnectionEvent e)
	{
		ONLComponent.Listener elem;
		int max = eventListeners.size();
		ExpCoordinator.print(new String(getLabel() + " ONLComponent:fireEvent connection event  #listeners = " + max), 2);
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLComponent.Listener)eventListeners.elementAt(i);
			elem.onlcConnectionChanged(e);
		}
	}
	public void fireEvent(ONLComponent.Event e)
	{
		ONLComponent.Listener elem;
		int max = eventListeners.size();
		boolean istable = (e instanceof ONLComponent.TableEvent);
		ExpCoordinator.print(new String(getLabel() + " ONLComponent:fireEvent " + e.getType() + " #listeners = " + max), 6);
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLComponent.Listener)eventListeners.elementAt(i);
			if (istable) 
			{
				ONL.StringWriter wrtr = new ONL.StringWriter();
				try
				{
					e.write(wrtr);
					ExpCoordinator.print(new String(getLabel() + "ONLComponent.fireEvent tableEvent: " + wrtr.getString()), 6);
				}
				catch (java.io.IOException ie){}
				elem.onlcTableChanged((TableEvent)e);
			}
			else
			{
				if (e.getType() == ONLComponent.Event.STATE_CHANGE || (e instanceof PropertyEvent))
				{
					ExpCoordinator.print("   Property Event", 4);
					elem.onlcStateChanged((PropertyEvent)e);
				}
				else
					elem.onlcValueChanged(e);
			}
		}
	}

	public void show() {}

	public void processEvent(ONLComponent.Event e)
	{
		ExpCoordinator.print(new String(getLabel() + ".ONLComponent.processEvent type " + e.getType()), 6);
		if (e.getType() == ConnectionEvent.CONN_OPEN)
		{
			ExpCoordinator.print("   open connection", 5);
			ONLDaemon d = ((ConnectionEvent)e).getDaemon();
			if (d != null) d.setConnectionID(((ConnectionEvent)e).getCID());
		}
		if (e.getType() == Event.GET_FOCUS) show();
		if (e instanceof PropertyEvent)
		{
			PropertyEvent pe = (PropertyEvent)e;
			setProperty(pe.getLabel(), pe.getNewValue());
		}
	}


	public void readEvent(ONL.Reader rdr) throws IOException
	{
		int tp = rdr.readInt();
		Event event = null;

		switch(tp)
		{
		case Event.TABLE_ADD:
		case Event.TABLE_DELETE:
		case Event.TABLE_CHANGE:
		case Event.TABLE_COMMIT:
			event = new TableEvent(this, tp, rdr);
			break;
		case Event.STATE_CHANGE:
			event = new StateEvent(this, tp, rdr);
			break;
		case Event.PROPERTY_CHANGE:
			event = new PropertyEvent(this, tp, rdr);
			break;
		case Event.GRAPHIC_MOVE:
			event = new MoveEvent(this, tp, rdr);
			break;
		case ConnectionEvent.CONN_OPEN:
		case ConnectionEvent.CONN_CLOSE:
		case ConnectionEvent.CONN_FAIL:
			ExpCoordinator.print(new String(getLabel() + ".ONLComponent.readEvent connection event type " + tp), 6);
			event = new ConnectionEvent(this, tp, rdr);
			break;
		default:
			event = new Event(this, tp, rdr);
		}
		if (event != null)
		{
			ExpCoordinator.print(new String(getLabel() + ".ONLComponent.readEvent about to call processEvent " + tp), 6);
			processEvent(event);
		}
	}

	public void addONLCListener(ONLComponent.Listener l)
	{
		ExpCoordinator.print(new String(getLabel() + " ONLComponent:addListener"), 2);
		if (!eventListeners.contains(l)) eventListeners.add(l);
	}
	public void removeONLCListener(ONLComponent.Listener l)
	{
		if (eventListeners.contains(l)) eventListeners.remove(l);
	}

	public NCCPConnection.ConnectionListener getConnectionListener()
	{
		if (connListener == null) connListener = new ConnectionListener(this);
		return connListener;
	}

	public GraphicsListener getGraphicsListener()
	{
		if (graphicsListener == null) graphicsListener = new GraphicsListener(this);
		return graphicsListener;
	}

	public void readWatchedProps(ONL.Reader rdr) throws IOException
	{
		if (propertiesToWatch == null) propertiesToWatch = new Vector();
		int max = rdr.readInt();
		String lbl;
		String val;
		for (int i = 0; i < max; ++i)
		{
			lbl = rdr.readString();
			val = rdr.readString();
			setProperty(lbl, val);
			ExpCoordinator.print(new String(getLabel() + ".ONLComponent.readWatchedProps prop:" + lbl + " val:" + val), 5);
		}
	}

	public void writeWatchedProps(ONL.Writer wrtr) throws IOException
	{
		int max = propertiesToWatch.size();
		wrtr.writeInt(max);
		String elem;
		String elem_val;
		for (int i = 0; i < max; ++i)
		{
			elem = (String)propertiesToWatch.elementAt(i);
			wrtr.writeString(elem);
			elem_val = getProperty(elem);
			wrtr.writeString(elem_val);
			ExpCoordinator.print(new String(getLabel() + ".ONLComponent.writeWatchedProps prop:" + elem + " val:" + elem_val), 5);
		}
	}

	public ONLComponent getChild(String tp) { return (children.getONLComponentByType(tp));}
	public ONLComponent getChildAt(int ndx) { return (children.onlComponentAt(ndx));}
	public int getNumChildren() { return (children.size());}
	public int getNumPorts() { return 0;}
	protected Vector<ONLComponent.PortBase> getConnectedPorts() { return null;} //used by OldSubnetManager to get all of the ports connected through a switch
	protected ONLComponentList getChildren() { return children;}
	public void removeChild(ONLComponent c) { children.removeComponent(c);}

	protected Vector getDaemons() 
	{ 
		if (daemons != null) return (new Vector(daemons));
		else return null;
	}


	protected Vector getHWTables()
	{
		Vector rtn = new Vector();
		int max = getNumChildren();
		ONLComponent c;
		for (int i = 0; i < max; ++i)
		{
			c = getChildAt(i);
			if (c instanceof HWTable) rtn.add(c);
		}
		return rtn;
	}

	public void setCluster(Cluster.Instance cli) { cluster = cli;}
	public Cluster.Instance getCluster() { return cluster;} 
	public int getClusterReference()
	{
		if (cluster != null)
			return (cluster.getIndex());
		else
			return (getPropertyInt(CLUSTER_REF));
	}
	public void writeComponentID(ONL.Writer wrtr) throws java.io.IOException 
	{
		if (getCommittedLabel() != null)
		{
			ExpCoordinator.print(new String("ONLComponent.writeComponentID " + getCommittedLabel()), 5);
			wrtr.writeString(getCommittedLabel());
		}
		else  
		{
			ExpCoordinator.print(new String("ONLComponent.writeComponentID " + getLabel()), 5);
			wrtr.writeString(getLabel());
		}
		wrtr.writeString(getType());
		wrtr.writeInt(getReference());
		wrtr.writeInt(getClusterReference());
		wrtr.writeString(getCPAddr());
	} 

	public String getComponentID()
	{
		String lbl = new String(getLabel());
		if (getCommittedLabel() != null)
		{
			lbl = new String(getCommittedLabel());
		}
		return (new String(lbl + ":" + getType() + ":" + getReference() + ":" + getClusterReference()));
	}
	public String getCPAddr() { return ("");}
	public int getReference() { return (getPropertyInt(REF_NUM));}
	/*
  public String getTypeLabel()
   {
       if (properties.getProperty(TYPE) != null) return (properties.getProperty(TYPE));
       return (UNKNOWN_LBL);
   }
  public String getBaseTypeLabel() { return getBaseType();}
	 */
	public ONL.IPAddress getIPAddr() 
	{
		String ip = getProperty(ONLComponent.IPADDR);
		if (ip != null)
		{
			try 
			{
				return (new ONL.IPAddress(ip));
			}
			catch(java.text.ParseException e) 
			{
				ExpCoordinator.print(new String("ONLComponent("+ getLabel() + ").getIPAddr error:" + e.getMessage()));
				return (new ONL.IPAddress());
			}
		}
		else return (new ONL.IPAddress());
	}
	public void setIPAddr(String ip) 
	{ 
		ExpCoordinator.print(new String(getLabel() + ".ONLComponent.setIPAddr " + ip));
		setProperty(ONLComponent.IPADDR, ip);
		addToDescription(ONLComponent.IPADDR, ip);
	}
	/*//SUBNET:remove
  public void setSubNetRouter(ONLComponent.PortInterface c) 
  { 
    if (!isRouter()) 
      {
	subNetRouter = c;
	if (c != null)
	  ExpCoordinator.print(new String(getLabel() + ".setSubNetRouter " + (((ONLComponent)c).getLabel())), 5);
	else
	  ExpCoordinator.print(new String(getLabel() + ".setSubNetRouter " + null), 5);
      }
  }
  public ONLComponent.PortInterface getSubNetRouter() 
    { 
	if (!isRouter()) return subNetRouter;
	else return null;
    }
	 */
	/*//SUBNET:remove
  public String getNHIPAddr()
    {
	if (subNetRouter != null) return (subNetRouter.getProperty(NHIPADDR));
	else return "0.0.0.0";
    }
  public int getNumInterfaces(Vector v) 
    {
	if (!v.contains(this)) v.add(this);
	return 0;
    }
	 */
	public ContentHandler getContentHandler(ExperimentXML expXML) { return null;}
	public void setState(String st) { setProperty(STATE, st);}
	abstract protected boolean merge(ONLComponent c);
	public ONLComponent getChild(ONLComponent c)
	{
		ONLComponent rtn = children.getONLComponent(c.getLabel(), c.getType());
		if (rtn == null && getLabel().equals(c.getLabel()) && getType() == c.getType()) return this;
		return rtn;
	}
	public Hardware getHardware() //returns the Hardware the component belongs to
	{
		ONLComponent c = this;
		while (c != null)
		{
			if (c instanceof Hardware) return ((Hardware)c);
			c = c.getParent();
		}
		return null;
	}
	public Command getCommand(int monitor_id, boolean is_mon)
	{
		Hardware hwi = getHardware();
		if (hwi != null) return (hwi.getCommand(monitor_id, is_mon));
		return null;
	}
	protected void setMonitorable(ONLMonitorable mon) { monitorable = mon;}

	public IDAssigner.ID getNewID(FieldSpec.AssignerSpec as)
	{
		if (as.isHWAssigner()) return (getHardware().getNewID(as));
		if (as.isPortAssigner())
		{
			ONLComponent c = this;
			while(c != null && !(c instanceof Hardware.Port)) c = c.getParent();
			if (c instanceof Hardware.Port) return (c.getNewID(as));
		}
		if (as.isTableAssigner())
		{
			ONLComponent c = this;
			while(c != null && !(c instanceof HWTable)) c = c.getParent();
			if (c instanceof HWTable) return (c.getNewID(as));
		}
		return null;
	}

	//PropertyChangeListener
	public void propertyChange(PropertyChangeEvent e)
	{
		ExpCoordinator.print(new String("ONLComponent.propertyChange " + e.getPropertyName() + " to " + ((String)e.getNewValue()) + " from " + ((String)e.getOldValue())), TEST_LABEL_CHANGE);
		if (e.getPropertyName().equals(LABEL))
		{
			ExpCoordinator.print(new String("    label changed"), TEST_LABEL_CHANGE);
			String olbl = getLabel();
			String oval = (String)e.getOldValue();
			if (oval != null && olbl.startsWith(oval))
				setLabel(new String((String)e.getNewValue() + olbl.substring(oval.length())));
		}
	}
	public boolean isRootComponent(ONLComponent c)
	{
		ONLComponent tmp_c = getRootComponent();
		return (c != null && tmp_c == c);
	}
	public ONLComponent getRootComponent()
	{
		ONLComponent tmp_c = getParent();
		ONLComponent tmp_last = this;
		while (tmp_c != null)
		{
			tmp_last = tmp_c;
			tmp_c = tmp_c.getParent();
		}
		return (tmp_last);
	}
	public boolean isAncestor(ONLComponent c)
	{
		ONLComponent tmp_c = getParent();
		while (tmp_c != null)
		{
			if (tmp_c == c) return true;
			tmp_c = tmp_c.getParent();
		}
		return false;
	}
	//protected boolean isParent(String tp) { return (getParent() != null && getParent().isType(tp));}
	protected boolean isAncestor(String tp) 
	{ 
		if (isType(tp)) return true;
		ONLComponent tmp_c = getParent();
		while (tmp_c != null)
		{
			if (tmp_c.isType(tp)) return true;
			tmp_c = tmp_c.getParent();
		}
		return false;
	}
	public String getInterfaceType()
	{
		String rtn = properties.getProperty(INTERFACE_TYPE);
		if (rtn != null) return rtn;
		else return ExperimentXML.ITYPE_1G;
	}
	public String getNetmaskString() { return null;}
	public void setSubnet(SubnetManager.Subnet sn)
	{
		//if (isSwitch())
		//{
		if (sn != null) addToDescription(ONLComponent.SUBNET, new String("subnet:" + sn.getBaseIP() + "/" + sn.getNetmask()));//+ sn.getIndex()));
		if (originalSubnet == null) setOriginalSubnet(sn);
		subnet = sn;
		//}
	}
	protected SubnetManager.Subnet getSubnet() { return subnet;}
	protected SubnetManager.Subnet getOriginalSubnet() { return originalSubnet;}
	protected void setOriginalSubnet(SubnetManager.Subnet osn) { originalSubnet = osn;}
	protected Color getIconColor() { return iconColor;}
	protected void setIconColor(Color c) { iconColor = c;}
	protected void setIconColor(int r, int g, int b) { setIconColor(new Color(r,g,b));}
	protected void setIconSize(int w, int h) { setIconSize(new Dimension(w, h));}
	protected void setIconSize(Dimension d) { iconSize = d;}
	protected Dimension getIconSize() { return iconSize;}
	protected String getUserLabel()
	{
		String rtn = getProperty(ExperimentXML.USER_LABEL);
		if (rtn != null) return rtn;
		else return "";
	}
	protected void setUserLabel(String s) { setProperty(ExperimentXML.USER_LABEL, s);}

}
