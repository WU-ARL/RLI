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
 * File: RouteTable.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/29/2004 
 *
 * Description:
 *
 * Modification History:
 *
 */
//package onlrouting;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import java.util.*;

import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

//note: when first add routes make sure they get the right subport value
public abstract class RouteTable extends ONLCTable implements Priority.PRTable, PortStats.Listener, MonitorAction
{
	//private Vector routes = null;
	protected RouterPort port = null;
	protected Router router = null;
	protected static final String PREFIX_NOMASK = "prefix";
	protected static final String PREFIX = "prefix/mask";
	protected static final String MASK = "mask";
	protected static final String NH_PORT = "next hop";
	protected static final String NEXTHOP = NH_PORT;
	protected static final String NH_SUBPORT = "next hop subport";
	protected static final String STATS = "stats";
	protected static final String PRIORITY = Priority.PRIORITY;
	private Priority priority = null;
	private JInternalFrame frame = null;
	public static final int PREFIX_NDX = 0;
	public static final int NH_NDX = 1;
	public static final int STATS_NDX = 2;



	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////// RouteTable.RTContentHandler ////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private class RTContentHandler extends Route.XMLHandler
	{
		private RouteTable routeTable = null;

		public RTContentHandler(ExperimentXML exp_xml, RouteTable rt)
		{
			super(exp_xml, null);
			routeTable = rt;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			super.startElement(uri, localName, qName, attributes);
			if (localName.equals(ExperimentXML.ROUTE) && currentRoute == null) 
				{
					currentRoute = routeTable.createRoute();
					String a = attributes.getValue(Route.GENERATED);
					if (a != null) currentRoute.setGenerated(a);
				}
		}
		public void endElement(String uri, String localName, String qName)
		{
			if (localName.equals(ExperimentXML.ROUTE_TABLE)) 
			{
				expXML.removeContentHandler(this);
			}
			if (localName.equals(ExperimentXML.ROUTE) && currentRoute != null) 
			{
				addNewElement(currentRoute);
				currentRoute = null;
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////// RouteTable.TableModel ////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//class TableModel
	protected static class TableModel extends ONLCTable.TableModel //AbstractTableModel implements ONLTableCellRenderer.CommittableTable
	{
		public TableModel(RouteTable rtable)
		{
			super(rtable);
			//Route tmp_route = new Route(rtable); 
			/*types = new Class[]{
	  tmp_route.getPrefixMask().getClass(), tmp_route.getNextHop().getClass(), tmp_route.getStats().getClass()
          };*/
			preferredW[0] = 120;
			preferredW[1] = 50;
			preferredW[2] = 100;
		}
		public TableCellEditor getEditor(int col) 
		{
			if (col == 0)
				return (new DefaultCellEditor(new JFormattedTextField(new Route.PrefixMask())));
			if (col == 1)
				return (new DefaultCellEditor(new JFormattedTextField(new Route.NextHop())));
			return null;
		}

		public int findColumn(String nm)
		{
			if (nm.equals(PREFIX)) return 0;
			//if (nm.equals(MASK)) return 2;
			if (nm.equals(NH_PORT)) return 1;
			if (nm.equals(STATS)) return 2;
			System.err.println("RouteTable::findColumn not a recognized string");
			return 0;
		}
	} //class TableModel

	protected static class DefaultRoutesAction extends ONL.UserAction //implements ListSelectionListener, KeyListener
	{
		private RouteTable routeTable;
		private ExpCoordinator expCoordinator;
		//private int nRoute = -1;
		public DefaultRoutesAction(RouteTable rt)
		{
			super("Generate Local Default Routes", false, true);
			routeTable = rt;
			expCoordinator = routeTable.getExpCoordinator();
		}

		public void actionPerformed(ActionEvent e)
		{
			routeTable.generateDefaultRts();
			if (ExpCoordinator.isRecording())
				ExpCoordinator.recordEvent(SessionRecorder.GENERATE_LOCAL_DEFRTS, new String[]{routeTable.port.getLabel()}); 
		}
	}
	protected static class StopStatsAction extends ONL.UserAction 
	{
		private RouteTable routeTable;
		private JTable jtable;

		public StopStatsAction(RouteTable rt, JTable jt) { this(rt, jt, "Stop Stats");}
		public StopStatsAction(RouteTable rt, JTable jt, String ttl)
		{
			super(ttl, false, true);
			//super("Stop Stats");
			routeTable = rt;
			jtable = jt;
		}

		public void actionPerformed(ActionEvent e)
		{
			Vector routes = routeTable.getSelectedRoutes();
			int max = routes.size();
			Route rt;
			int i = 0;
			boolean confirmed = false;
			for (i = 0; i < max; ++i)
			{
				rt = (Route)routes.elementAt(i);
				if (rt.getStats() != null && ((rt.getStats().getState() & PortStats.PERIODIC) > 0))
				{
					if (!confirmed)
					{
						//ask user for confirmation that they really want to stop
						int rtn = JOptionPane.showConfirmDialog(routeTable.jtable, "Do you really want to stop statistics polling?");
						//if no return else continue
						if (rtn == JOptionPane.OK_OPTION)
							confirmed = true;
						else return;
					}
					rt.getStats().setState(PortStats.STOP);
				}
			}
		}
	}

	private class RTAction extends AbstractAction
	{
		public RTAction(String ttl) 
		{
			super(ttl);
		}
		public RTAction() 
		{
			this("Route Table");
		}
		public void actionPerformed(ActionEvent e) 
		{
			show();
		}
	}


	public RouteTable(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{
		super(lbl,tp,ec, null);
		setParentFromString(tr.readString());
		port = (RouterPort)getParent();
		router = (Router)port.getParent();
		createTableModel();
		priority = new Priority("priority:", this, NCCP.Operation_RTPriority, "route table priority");
		priority.setObserveAction(false, true);
		read(tr);
		addToDescription(1, "Port " + port.getLabel() + " Route Table");
		setProperty(PRIORITY, priority.getPriority());
		addStateListener(PRIORITY);
	}
	public RouteTable(RouterPort p)
	{
		super("RT", ONLComponent.RTABLE, p.getExpCoordinator(), null, p);
		port = p;
		router = (Router)port.getParent();
		//routes = new Vector();
		createTableModel();
		priority = new Priority("priority:", this, NCCP.Operation_RTPriority, "route table priority");
		priority.setObserveAction(false, true);
		addToDescription(1, "Port " + port.getLabel() + " Route Table");
		setProperty(PRIORITY, priority.getPriority());
		addStateListener(PRIORITY);
	}
	public void show()
	{
		ExpCoordinator.print(new String("RouteTable." + getLabel() + ".show"), 6);
		if (frame == null)
		{
			getFrame();
			port.addToDesktop(frame);
		}
		frame.setSize(250,250);
		port.showDesktop(frame);
	}
	public abstract void createTableModel();
	protected static void skip(ONL.Reader tr) throws IOException
	{
		tr.readInt();
		int numRoutes = tr.readInt();
		for (int i = 0; i < numRoutes; ++i)
			tr.readString();
	}
	protected void read(ONL.Reader tr) throws IOException
	{
		setPriority(tr.readInt());
		super.read(tr);
	}
	public void writeTable(ONL.Writer wr) throws IOException
	{
		wr.writeInt(priority.getPriority());
		super.writeTable(wr);
	}
	public boolean areEqual(Object o1, Object o2)
	{
		boolean rtn = false;
		if (o1 instanceof Route)
		{
			if (o2 instanceof Route) 
				rtn = ((Route)o1).isAddressEqual((Route)o2);
			if (o2 instanceof Route.PrefixMask)
				rtn = ((Route)o1).getPrefixMask().isEqual((Route.PrefixMask)o2);
		}
		else
		{ 
			if (o1 instanceof Route.PrefixMask && o2 instanceof Route)
				rtn = ((Route)o2).getPrefixMask().isEqual((Route.PrefixMask)o1);
		}
		return rtn;
	}

	public Route getRoute(Object o) { return ((Route)getElement(o));}
	public boolean containsRoute(Route r) { return (containsElement(r));}
	public void changeRoute(Route r) { changeElement(r);}
	public void addRouteReplace(Route r)
	{
		if (r != null)
		{
			ExpCoordinator ec = port.getExpCoordinator();
			Experiment exp = ec.getCurrentExp();
			Route oldRoute = getRoute(r);
			if (oldRoute != null)
			{
				String ort = oldRoute.toString();
				oldRoute.setNextHop(new Route.NextHop(r.getNextHop()));
				if (oldRoute.isCommitted())
				{
					ExpCoordinator.printer.print("                    inserting UpdateEdit", 10);
					ec.addEdit(oldRoute.getUpdateEdit());
				}  
				changeElement(r);
			}
			else 
			{
				addNewElement(r);
			}
		}
	}
	protected Object addNewElement(ONL.Reader rdr, int i) //adds an element read from some stream
	{
		return ((Route)addNewElement(createRoute(rdr), i));//new NSPRoute(rdr.readString(), this), i);
	}
	protected Object addNewElement(ONLCTable.Element elem, int i) //adds new element making a copy of elem
	{ 
		Route rt = (Route)addElement(createRoute((Route)elem)); //new NSPRoute((NSPRoute)elem, this), i, true);
		if (rt != null) 
		{
			ExpCoordinator.print(new String("RouteTable(" + getLabel() + ").addNewElement adding edit: " + rt.toString()), ExperimentXML.TEST_XML);
			expCoordinator.addEdit(getAddEdit(rt));//new Route.AddEdit(port, rt, this, expCoordinator.getCurrentExp()));
		}
		else rt = getRoute((Route) elem);
		if (ExpCoordinator.isRecording())
			ExpCoordinator.recordEvent(SessionRecorder.ADD_ROUTE, new String[]{port.getLabel(), elem.toString()}); 
		return rt;
	}
	protected abstract Route createRoute(Route r); 
	protected abstract Route createRoute(ONL.Reader rdr);
	protected abstract Route createRoute();
	protected Route createRoute(String pf, int m, Route.NextHop nh) { return (createRoute(pf, m, nh.getPort(), nh.getSubport()));}
	protected Route createGeneratedRoute(String pf, int m, Route.NextHop nh) 
		{ 
			Route rtn = createRoute(pf, m, nh.getPort(), nh.getSubport());
			rtn.setGenerated(true);
			return rtn;
		}
	protected abstract Route createRoute(String pf, int m, int nh_op, int nh_sp);

	protected Object addNewElement(int i) { return(addNewElement(createRoute(), i));} //creates new element and adds 

	public void addRoute(Route r) { addElement(r);}
	public void addRoute(Route r, int s) { addElement(r, s);}
	protected Route addNewRoute() { return ((Route)addNewElement());}
	public void removeRoute(Route r) { removeFromList(r);}
	public void removeElements(Vector v)
	{
		super.removeElements(v);
		expCoordinator.addEdit(getDeleteEdit(v));
	}
	protected void removeFromList(ONLCTable.Element r)
	{
		super.removeFromList(r);
		if (ExpCoordinator.isRecording())
			ExpCoordinator.recordEvent(SessionRecorder.DELETE_ROUTE, new String[]{port.getLabel(), r.toString()});
	}

	protected abstract ONLComponent.Undoable getAddEdit(Route rt) ;
	protected abstract ONLComponent.Undoable getAddEdit(Vector rts);
	protected abstract ONLComponent.Undoable getDeleteEdit(Route rt); 
	protected abstract ONLComponent.Undoable getDeleteEdit(Vector rts); 
	public Route getRoute(int i) { return ((Route)getElement(i));}
	public Vector getSelectedRoutes()
	{
		int ndxs[] = jtable.getSelectedRows();
		int max = Array.getLength(ndxs);
		Vector rts = new Vector(max);
		int i = 0;
		Route rt;
		for (i = 0; i < max; ++i)
		{
			rt = getRoute(ndxs[i]);
			if (!rts.contains(rt))
				rts.add(rt);
		}
		return rts;
	}
	public void generateDefaultRts()
	{
		Router router = (Router)port.getParent();
		int numPorts = router.getNumPorts();

		Vector tmp_routes = new Vector();
		for (int i = 0; i < numPorts; ++i)
		{
			ONLComponent.PortBase p = router.getPort(i);
			//add a route of form 192.168.nsp.(16*(port+1))/28
			Route tmp_route = createRoute(port.getIPAddr().toString(), port.getNetmask(), i, 0);
			if (!containsRoute(tmp_route))
			{
				tmp_routes.add(tmp_route);
				addRoute(tmp_route);
			}
		}
		/*//SUBNET:remove
      String ip_base = router.getBaseIPAddr();
      //ExpCoordinator.printer.print("RouteTable::generateDefaultRts " + ip_base);
      int numPorts = router.getNumPorts();
      String[] strarray = ip_base.split("\\.");
      String tmp_str = new String(strarray[0] + "." + strarray[1] + "." + strarray[2] + ".");

      Vector tmp_routes = new Vector();
      int tmp_int = 0;
      for (int i = 0; i < numPorts; ++i)
	{

	  tmp_int = (i+1)*16;
	  //add a route of form 192.168.nsp.(16*(port+1))/28
	  Route tmp_route = createRoute(new String(tmp_str + tmp_int), 28, i, 0);
	  if (!containsRoute(tmp_route))
	    {
	      tmp_routes.add(tmp_route);
	      addRoute(tmp_route);
	    }
	}
		 */
		if (!tmp_routes.isEmpty()) 
		{
			Experiment exp = expCoordinator.getCurrentExp();
			if (!exp.containsParam(this)) exp.addParam(this);
			expCoordinator.addEdit(getAddEdit(tmp_routes));
		}
	}

	public int getPriority() { return (priority.getPriority());}
	public void setPriority(int p) 
	{
		priority.setValue(p);
		priority.addEditForCommit();
		setProperty(PRIORITY, p);
		if (ExpCoordinator.isRecording())
		{
			ExpCoordinator.recordEvent(SessionRecorder.PRIORITY_RT_TABLE, new String[]{port.getLabel(), String.valueOf(p)});   
		}
	}

	public ONLCTable.TablePanel getPanel()
	{
		ONLCTable.TablePanel panel = getPanel(tableModel);
		panel.add(priority, 0);

		jtable = panel.getJTable();
		//frame.getContentPane().add(panel);
		return panel;
	}

	private JInternalFrame getFrame() 
	{
		if (frame == null)
		{
			if (port == null) ExpCoordinator.printer.print("RTAction.actionPerformed port is null");
			frame = new JInternalFrame(new String(port.getLabel() + " Route Table")); // (" + getPriority()  + ")")); 

			JPanel panel = getPanel();
			frame.getContentPane().add(panel);
			JMenuBar mb = new JMenuBar();
			frame.setJMenuBar(mb);
			JMenu menu = new JMenu("Edit");
			mb.add(menu);
			ONLCTable.ElementAction add_action = new ONLCTable.ElementAction(this, "Add Route");
			menu.add(add_action);
			menu.add(new DefaultRoutesAction(this));
			if (router instanceof NSPDescriptor) menu.add(new StopStatsAction(this, jtable));
			menu.add(new ONLCTable.ElementAction(this, "Delete Route", false));
			frame.addInternalFrameListener(new ONLComponent.ONLInternalFrameListener(this));
		}
		return frame;
	}
	public void clear()
	{
		Vector del_rts = new Vector(getList());
		super.clear();
		if (!del_rts.isEmpty())
		{
			ONLComponent.Undoable delEdit = getDeleteEdit(del_rts);
			delEdit.commit();
		}
		if (frame != null && frame.isShowing()) frame.dispose();
	}
	public void removeGeneratedElements()
	{
		Vector del_rts = new Vector(getList());
		ExpCoordinator.print(new String("RouteTable(" + getLabel() + ").removeGeneratedElements numRts = " + del_rts), Topology.TEST_DEFRTS);
		if (!del_rts.isEmpty())
		{
			int max = del_rts.size();
			for (int i = 0; i < max; ++i)
			{
				Route elem = (Route)del_rts.elementAt(i);
				if (elem.isGenerated()) 
				{
					ExpCoordinator.print(new String("RouteTable(" + getLabel() + ").removeGeneratedElements removing(" + elem.toString() + ")"), Topology.TEST_DEFRTS);
					removeRoute(elem);
				}
			}
		}
	}
	public void clearRoutes()
	{
		Vector del_rts = new Vector(getList());
		ExpCoordinator.print(new String("RouteTable(" + getLabel() + ").clearElements numRts = " + del_rts), Topology.TEST_DEFRTS);
		if (!del_rts.isEmpty())
		{
			int max = del_rts.size();
			for (int i = 0; i < max; ++i)
			{
				Route elem = (Route)del_rts.elementAt(i);
				ExpCoordinator.print(new String("RouteTable(" + getLabel() + ").removeGeneratedElements removing(" + elem.toString() + ")"), Topology.TEST_DEFRTS);
				removeRoute(elem);
			}
		}
	}

	//Priority.PRTable interface
	protected abstract NCCPOpManager createOpManager();
	public RouterPort getPort() { return port;}
	public ONLComponent getONLComponent() { return this;}
	//end Priority.PRTable interface
	//interface PortStats.Listener
	public void stateChanged(PortStats.Event pe){}
	public void valueChanged(PortStats.Event pe)
	{
		//Route rt = ((Route.Stats)pe).getRoute();
		int r = getIndex(((Route.Stats)pe.getSource()).getRoute()); 
		if (r >= 0)
		{
			tableModel.fireTableCellUpdated(r,2);
			tableModel.fireTableRowsUpdated(2,2);
		}
	} 
	public void indexChanged(PortStats.Event pe)
	{
		//Route rt = ((Route.Stats)pe).getRoute();
		int r = getIndex(((Route.Stats)pe.getSource()).getRoute()); 
		if (r >= 0)
		{
			tableModel.fireTableCellUpdated(r,2);
			tableModel.fireTableRowsUpdated(2,2);
		}
	} 
	//end interface PortStats.Listener
	//interface MonitorAction
	public Monitor addMonitor(Monitor m){return null;}
	public void removeMonitor(Monitor m){}
	public abstract boolean isDataType(MonitorDataType.Base mdt);
	public abstract Monitor addMonitor(MonitorDataType.Base mdt);
	//end interface MonitorAction

	public abstract Class[] getColumnTypes();
	public String[] getColumnNames()
	{
		String[] rtn = new String[]{PREFIX,NH_PORT,STATS};
		return rtn;
	}
	public void processEvent(ONLComponent.Event e)
	{
		super.processEvent(e);
		if (e instanceof ONLComponent.PropertyEvent)
		{
			PropertyEvent pe = (PropertyEvent)e;
			if (pe.getLabel().equals(PRIORITY)) 
			{
				String val = pe.getNewValue();
				ExpCoordinator.print(new String("RouteTable.processEvent set priority " + val), 5);
				if (val != null && val.length() > 0)
					setPriority(Integer.parseInt(val));
				return;
			}
		}
	}

	public Action getAction()
	{
		return (new RTAction(NSPPort.RTACTION));
	}


	public ONLComponent.ONLMonitorable getMonitorable() { return (port.getMonitorable());}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.ROUTE_TABLE);//"routeTable");
		xmlWrtr.writeAttribute(ExperimentXML.PRIORITY, String.valueOf(getPriority()));
		int num_elems = size();
		for (int i = 0; i < num_elems; ++i)
		{
			Element rt = getElement(i);
			if (rt.isWritable())
				rt.writeXML(xmlWrtr);
		}
		xmlWrtr.writeEndElement();
	}

	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new RTContentHandler(exp_xml, this));}
}
