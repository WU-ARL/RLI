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
 * File: Route.java
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

import java.lang.String;
import java.io.*;
import javax.swing.undo.*;
import javax.swing.*;
import java.lang.reflect.Array;
import java.util.*;
import java.text.ParseException;
import java.lang.NumberFormatException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;


public abstract class Route implements ONLTableCellRenderer.Committable, ONLCTable.Element//, PropertyChangeListener
{
	public static final int TEST_DELETE = 8;
	public static final int TEST_UPDATE = 8;
	//property names
	public static final String ROUTE_EDITABLE = "route_editable";
	public static final String PREFIX_SET = "prefix_set";
	public static final String MASK_SET = "mask_set";
	public static final String NEXTHOP_SET = "nexthop_set";
	public static final String COMMITTED_PM = "committed_pm";
	public static final String COMMITTED_NH = "committed_nh";
	public static final String COMMITTED_STATS = "committed_stats";
	public static final String COMMITTED = "committed";
	public static final String GENERATED = HWTableElement.GENERATED;
	//field indices or table columns
	public static final int PREFIX_FIELD = 0;
	public static final int NEXTHOP_FIELD = 1;
	public static final int STATS_FIELD = 2;
	protected PrefixMask prefixMask = null;
	//protected String prefix = "0.0.0.0";
	//protected int mask = 0;
	protected NextHop nextHop = null;
	protected ONLPropertyList properties = null;
	protected UpdateEdit updateEdit = null;
	protected Router router = null;
	protected ExpCoordinator expCoordinator = null;
	protected RouteTable routeTable = null;
	protected Stats stats = null;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////// RouteTable.RTContentHandler ////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class XMLHandler extends DefaultHandler
	{
		protected ExperimentXML expXML = null;
		protected String currentElement = "";
		protected Route currentRoute = null;

		public XMLHandler(ExperimentXML exp_xml, Route rt)
		{
			super();
			expXML = exp_xml;
			currentRoute = rt;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			currentElement = new String(localName);
			ExpCoordinator.print(new String("Route.XMLHandler.startElement currentElement " + currentElement), ExperimentXML.TEST_XML); 
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("RouteTable.RTContentHandler.characters " + currentElement + ":" + new String(ch, start, length)), ExperimentXML.TEST_XML); 
			try
			{
				if (currentElement.equals(ExperimentXML.PREFIX_MASK) && currentRoute != null)
					currentRoute.setPrefixMask(new Route.PrefixMask(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.NEXTHOP) && currentRoute != null)
					currentRoute.setNextHop(new Route.NextHop(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.STATS_INDEX) && currentRoute != null)
					currentRoute.setStatsIndex(Integer.parseInt(new String(ch, start, length)));
			}
			catch (java.text.ParseException e)
			{
				ExpCoordinator.print(new String("RouteTable.RTContentHandler.characters IOException " + e.getMessage()));
			}
		}
		public void setCurrentElement(String s) { currentElement = new String(s);}
		public String getCurrentElement() { return currentElement;}
		public void endElement(String uri, String localName, String qName)
		{
			if (localName.equals(ExperimentXML.ROUTE)) 
			{
				expXML.removeContentHandler(this);
			}
		}
	}//end Route.XMLHandler

	//////////////////////////////////////////////////////// Stats ///////////////////////////////////////////////////////////////
	public abstract static class Stats extends PortStats
	{
		protected Route route = null; 
		public Stats(String s) throws java.text.ParseException { super(s);}
		public Stats(RouterPort p, Route r)
		{
			super(p);
			route = r;
		}
		public Route getRoute() { return route;}
		public void setRoute(Route r) { route = r;}
		public void setState(int s)
		{
			super.setState(s);
			String state_str = "";
			switch(s)
			{
			case PortStats.STOP:
				state_str = "Stop";
				break;
			case PortStats.ONE_SHOT:
				state_str = "Once";
				break;
			case PortStats.MONITOR:
				state_str = "Add to Graph";
				break;
			case PortStats.PERIODIC:
				state_str = "Periodic";
				break;
			}
			if (ExpCoordinator.isRecording())
				ExpCoordinator.recordEvent(SessionRecorder.STATS_ROUTE_TABLE, new String[]{route.routeTable.getPort().getLabel(), route.toString(), String.valueOf(getIndex()), state_str});
		}
	}

	//////////////////////////////////////////////////////// PrefixMask //////////////////////////////////////////////////////////
	public static class PrefixMask
	{
		protected String prefix_str = "0.0.0.0";
		protected byte[] prefix_bytes;
		protected double mask_bytes = 0;
		protected int mask = 0;

		public PrefixMask(PrefixMask pm)
		{
			set(pm);
		}
		public PrefixMask(String p, int m) throws java.text.ParseException
		{
			parseString(p, m);
		}
		public PrefixMask() 
		{
			prefix_str = "0.0.0.0";
			prefix_bytes = new byte[4];
		}
		public PrefixMask(String str) throws java.text.ParseException
		{	
			parseString(str);
		}
		protected void parseString(String str) throws java.text.ParseException
		{
			String[] strarray = str.split("/"); //need new java to use this
			int len = Array.getLength(strarray);
			ExpCoordinator.print(new String("Route.PrefixMask string " + str + " len:" + len), 5);
			if (len != 2) throw new java.text.ParseException("Route.PrefixMask error: not of form x.x.x.x/mask", 0);
			else
			{
				int tmp_int;
				try
				{
					tmp_int = Integer.parseInt(strarray[1]);
					parseString(strarray[0], tmp_int);
				}
				catch(NumberFormatException e)
				{ 
					throw new java.text.ParseException(new String("Route.PrefixMask error: " + e.getMessage()), 0);
				}
			}
		}

		public void set(PrefixMask pm)
		{
			prefix_str = new String(pm.prefix_str);
			prefix_bytes = new byte[4];
			for (int i = 0; i < 4; ++i)
			{
				prefix_bytes[i] = pm.prefix_bytes[i];
			}
			mask = pm.mask;
			mask_bytes = pm.mask_bytes;
		}

		public int getPrefixAsInt()
		{
			int rtn = prefix_bytes[0] & 0x000000FF;
			for (int i = 1; i < 4; ++i)
			{
				rtn = (rtn << 8) | (prefix_bytes[i] & 0x000000FF);
			}
			return rtn;
		}
		protected void parseString(String pref_str, int m) throws java.text.ParseException
		{
			String str = ONL.getAddrFromLabel(pref_str);
			ExpCoordinator.print(new String("Route.PrefixMask.parseString prefix_str:" + pref_str + " m:" + m + " addr:" + str), 5);
			String[] prefarray = str.split("\\."); //need new java to use this
			int len = Array.getLength(prefarray);
			if (len != 4 ) throw new java.text.ParseException("Route.PrefixMask.parseString error: prefix not of form x.x.x.x ", 0);
			else
			{
				try
				{
					int tmp_int;
					prefix_bytes = new byte[4];
					for (int i = 0; i < 4; ++i)
					{
						tmp_int = Integer.parseInt(prefarray[i]);
						if (tmp_int < 0 || tmp_int > 255) throw new java.text.ParseException("Route.PrefixMask.parseString error: prefix address component out of range 0-255", 0);
						else 
						{
							//System.out.println("                      token[" + i +"] = " + prefarray[i]); 
							prefix_bytes[i] = (Integer.decode(prefarray[i])).byteValue();
						}
					}
					prefix_str = new String(prefarray[0] + "." + prefarray[1] + "." + prefarray[2]+ "." + prefarray[3]);
					if (m < 0 || m > 32) throw new java.text.ParseException("Route.PrefixMask error: mask out of range 0-32", 0);
					else 
					{
						mask = m;
						mask_bytes = 0;
						int min = 32 - m;
						for (int i = 31; i >= min; --i)
						{
							mask_bytes = (((int)mask_bytes) | (1 << i));
						}
					}
				}
				catch (NumberFormatException e)
				{ 
					throw new java.text.ParseException(new String("Route.PrefixMask error: " + e.getMessage()), 0);
				}
			}
		}
		public String getPrefix() { return prefix_str;}
		public byte[] getPrefixBytes() { return prefix_bytes;}
		public int getMask() { return mask;}
		public double getMaskBytes() { return mask_bytes;}
		public String getIPwMask() {  return (new String(prefix_str + "/" + mask));}
		public String toString()
		{
			return (getIPwMask());
		}
		public boolean isEqual(PrefixMask pm)
		{
			return ((prefix_str.equals(pm.prefix_str)) && (mask == pm.mask));
		}
	}

	////////////////////////////////////////////// interface NCCP_RouteRequester ////////////////////////////////////////////////////
	public static interface NCCP_RouteRequester
	{ 
		public void storeData(DataOutputStream dout) throws IOException;
		public Route getRoute();
		public int getNHSent();
		public String getPMSent();
		public String getOpString();
		public int getOp();
		public boolean isAdd();
		public boolean isDelete();
		public boolean isUpdate();
		public REND_Marker_class getMarker();
	}


	////////////////////////////////////////////// Edit ///////////////////////////////////////////////////////////////////

	public static abstract class Edit extends Experiment.Edit implements NCCPOpManager.Operation
	{
		protected Vector routes = null;
		protected Vector cancelled = null;
		protected RouteTable routeTable = null;
		protected Vector requests = null;
		protected int requestsLeft = 0;

		public Edit(RouterPort c, Route r, RouteTable rt, Experiment exp, int tp)
		{
			this(c, new Vector(), rt, exp, tp);
			routes.add(r);
		}

		public Edit(RouterPort c, Vector r, RouteTable rt, Experiment exp, int tp)
		{
			super(c, exp, tp);
			routeTable = rt;
			routes = r;
			cancelled = new Vector();
			//System.out.println("Route.AddEdit " + getClass().getName());
			requests = new Vector();
		}
		//protected void sendMessage() {} //define in extension to send NCCP requests
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (un instanceof Route.Edit)//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
			{
				Route.Edit edit = (Route.Edit) un;
				if (onlComponent == edit.getONLComponent() && 
						((isAdd() && edit.isDelete()) || (isDelete() && edit.isAdd()))) 
				{
					int max = routes.size();
					Vector removals = new Vector();
					int i;
					Route elem;
					for (i = 0; i < max; ++i)
					{
						elem = (Route)routes.elementAt(i);
						if (edit.contains(elem))
							removals.add(elem);
					}
					max = removals.size();
					for (i = 0; i < max; ++i)
					{
						elem = (Route)removals.elementAt(i);
						routes.remove(elem);
						cancelled.add(elem);
					}
				}
				if (routes.isEmpty()) return true;
			}
			return false;
		}
		public Vector getRoutes() { return routes;}
		public boolean contains(Route r)
		{
			if (routes.contains(r) || cancelled.contains(r)) return true;
			else
			{
				int max = routes.size();
				for (int i = 0; i < max; ++i)
					if (r.isEqual((Route)routes.elementAt(i))) return true;
				max = cancelled.size();
				for (int i = 0; i < max; ++i)
					if (r.isEqual((Route)cancelled.elementAt(i))) return true;
				return false;
			}
		}
		//NCCPOpManager.Operation
		public void opSucceeded(NCCP.ResponseBase resp)
		{
			NCCP_RouteRequester req = getRequest(resp.getRendMarker());
			if (req != null) 
			{
				//requests.remove(req);
				--requestsLeft;
				ExpCoordinator.print(new String("Route.Edit." + req.getOpString() + ".opSucceeded for route " + req.getPMSent() + " port " + routeTable.getPort().getID()), 6);
			}
			if (requestsLeft <= 0) routeTable.removeOperation(this);
		}
		public void opFailed(NCCP.ResponseBase resp)
		{
			NCCP_RouteRequester req = getRequest(resp.getRendMarker());
			Route route = req.getRoute();
			if (req != null) 
			{
				onlComponent.getExpCoordinator().addError(new StatusDisplay.Error((new String("Route Operation Failure on port " + routeTable.getPort().getID() + " for route " + req.getPMSent())),
						"Route Operation Failure.",
						"Route Operation Failure.",
						(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));                        
				ExpCoordinator.print(new String("Route.AddEdit.opFailed for route " + req.getPMSent() + " port " + routeTable.getPort().getID()), 2);
				if (req.getPMSent().equals(route.prefixMask.getIPwMask()))
				{
					if (isAdd())
						routeTable.removeRoute(route); //may change this to just indicate a state change and show the difference in the table somehow
					else 
						routeTable.addRoute(route);
				}
				else
				{ 
					if (isAdd() && req.getPMSent().equals(route.properties.getProperty(COMMITTED_PM))) 
						route.properties.remove(COMMITTED_PM); //setProperty(COMMITTED_PM, null);
				}
				--requestsLeft; //requests.remove(req);
			}
			if (requestsLeft <= 0) routeTable.removeOperation(this);
			//if (requests.isEmpty()) routeTable.removeOperation(this);
		}
		public Vector getRequests() { return requests;}
		public boolean containsRequest(REND_Marker_class rend)
		{
			if (getRequest(rend) != null) return true;
			return false;
		}
		//end NCCPOpManager.Operation
		protected NCCP_RouteRequester getRequest(REND_Marker_class rend)
		{
			int max = requests.size();
			NCCP_RouteRequester req = null;
			//System.out.println("Route.AddEdit numReqs = " + max + " looking for");
			//rend.print();
			for (int i = 0; i < max; ++i)
			{
				req = (NCCP_RouteRequester)requests.elementAt(i);
				//System.out.println("  elem " + i);
				//req.getMarker().print();
				if (req.getMarker().isEqual(rend)) return req;
			}
			return null;
		}
		protected abstract NCCP_RouteRequester createRequest(Route r);
	}//inner class Edit

	////////////////////////////////////////////// AddEdit ////////////////////////////////////////////////////////////////
	public static abstract class AddEdit extends Edit
	{
		public AddEdit(RouterPort c, Route r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp, Experiment.Edit.ADD);
		}

		public AddEdit(RouterPort c, Vector r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp, Experiment.Edit.ADD);
		}
		public void undo() throws CannotUndoException
		{
			//super.undo();
			int max = routes.size();
			for (int i = 0; i < max; ++i)
			{
				routeTable.removeRoute((Route)routes.elementAt(i));
			}
			if (routeTable.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(routeTable);
		}
		public void redo() throws CannotRedoException
		{
			//super.redo();
			int max = routes.size();
			for (int i = 0; i < max; ++i)
			{
				routeTable.addRoute((Route)routes.elementAt(i));
			}
			if ((getExperiment() != null) && !(getExperiment().containsParam(routeTable))) getExperiment().addParam(routeTable);	
		}
		protected void sendMessage() 
		{
			ONLDaemon ec = ((RouterPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);

			if (ec != null) 
			{
				if (!ec.isConnected()) ec.connect();
				int max = routes.size();
				Route route;
				for (int i = 0; i < max; ++i)
				{
					route = (Route)routes.elementAt(i);
					ExpCoordinator.print(new String("Route.AddEdit adding route " + route.getPrefixMask().toString() + "(" + route.getPrefixMask().getIPwMask() + ") daemon " + ec.toString()), 6);
					requests.add(createRequest(route));
					//ec.sendMessage(new NCCP_RouteRequester(NCCP.Operation_AddFIPLRoute, (RouterPort)getONLComponent(), route));
					++requestsLeft;
				}
				if (max > 0) routeTable.addOperation(this);
			}
			else System.err.println("AddRoute no daemon");
		}
	}//inner class AddEdit

	/////////////////////////////////////////// DeleteEdit /////////////////////////////////////////////////////////////////////////
	//to make Delete Edit work for a multiple selection need to create a compound edit with several
	//Delete Edits and then do something about the cancellation. For now just limit to single selection
	//and single delete

	public static abstract class DeleteEdit extends Route.Edit
	{
		public DeleteEdit(RouterPort c, Vector r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp, Experiment.Edit.REMOVE);
		}
		public void undo() throws CannotUndoException
		{
			//super.undo();
			int numRoutes = routes.size();
			for (int i = 0; i < numRoutes; ++i)
				routeTable.addRoute((Route)routes.elementAt(i));
			if ((getExperiment() != null) && !(getExperiment().containsParam(routeTable))) getExperiment().addParam(routeTable);	
		}
		public void redo() throws CannotRedoException
		{
			//super.redo();
			int numRoutes = routes.size();
			for (int i = 0; i < numRoutes; ++i)
				routeTable.removeRoute((Route)routes.elementAt(i));
			if (routeTable.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(routeTable);
		}
		protected void sendMessage() 
		{
			ONLDaemon ec = ((RouterPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);
			Route route;
			if (ec != null && getONLComponent().getParent().isActive())
			{
				if (!ec.isConnected()) ec.connect();
				int numRoutes = routes.size(); 
				for (int i = 0; i < numRoutes; ++i)
				{
					route = (Route)routes.elementAt(i);
					System.out.println("DeleteRoute " + route.properties.getProperty(COMMITTED_PM) + " daemon " + ec.toString());
					if (route.getStats().getState() == PortStats.PERIODIC) 
					{
						route.getStats().stopMonitoring();
						route.getStats().setState(PortStats.STOP); //stop stats if they're running
					}
					requests.add(createRequest(route));
					//ec.sendMessage(new NCCP_RouteRequester(NCCP.Operation_DeleteFIPLRoute, (NSPPort)getONLComponent(), route));
					++requestsLeft;
				}
				if (numRoutes > 0) routeTable.addOperation(this);
			}
			else System.err.println("DeleteRoute no daemon");
		}
	}// end inner class DeleteEdit
	/////////////////////////////////////////// UpdateEdit ///////////////////////////////////////////////////////////////////


	public static abstract class UpdateEdit extends Route.AddEdit implements NCCPOpManager.Operation
	{
		private final int INIT = 0;
		private final int NEXTHOP = 1;
		private final int PREFIX = 2;
		private final int DELETE_FAILED = 3;
		protected Route route = null;
		private NextHop newNH = null;
		private NextHop oldNH = null;
		private PrefixMask oldPM = null;
		private PrefixMask newPM = null;
		private int state = INIT;

		public UpdateEdit(RouterPort c, Route r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp);
			route = r;
			newNH = new NextHop();
			oldNH = new NextHop();
			newPM = new PrefixMask();
			oldPM = new PrefixMask();
		}
		public void undo() throws CannotUndoException
		{
			//super.undo();
			if (state == NEXTHOP) route.nextHop.set(oldNH);
			if (state == PREFIX) route.prefixMask.set(oldPM);
			routeTable.changeRoute(route);
		}
		public void redo() throws CannotRedoException
		{
			//super.redo();
			if (state == NEXTHOP) route.nextHop.set(newNH);
			if (state == PREFIX) route.prefixMask.set(newPM);
			routeTable.changeRoute(route);
		}
		protected void sendMessage() 
		{
			ONLDaemon ec = ((RouterPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);
			System.out.println("UpdateRoute " + route.getPrefixMask().toString() + "(" + route.getPrefixMask().getIPwMask() + ") daemon " + ec.toString());
			if (ec != null) 
			{
				if (!ec.isConnected()) ec.connect();
				String nh_str = route.properties.getProperty(COMMITTED_NH);
				String pm_str = route.properties.getProperty(COMMITTED_PM);

				try
				{
					if (nh_str != null) oldNH.parseString(nh_str);
					else oldNH = new NextHop();
					if (pm_str != null) oldPM.parseString(pm_str);
					else oldPM = new PrefixMask();
				} 
				catch (java.text.ParseException e)
				{
					System.err.println("Route.UpdateEdit error: " + e.getMessage());
				}
				newNH.set(route.getNextHop());
				newPM.set(route.getPrefixMask());
				createRequests(pm_str, nh_str);
				if (!requests.isEmpty()) routeTable.addOperation(this);
				route.resetUpdateEdit(routeTable);
			}
			else System.err.println("UpdateRoute no daemon");
		}
		protected abstract void createRequests(String pm_str, String nh_str);
		protected NCCP_RouteRequester createRequest(Route r) { return null;}
		//problem for each add or delete update there may be several update edits that need to be removed
		//problem for each add or delete update there may be several update edits that need to be removed
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (un instanceof Route.AddEdit && (!(un instanceof UpdateEdit)))//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
			{
				Route.AddEdit edit = (Route.AddEdit) un;
				if ((onlComponent == edit.onlComponent) && edit.contains(route)) return true;
			}
			if (un instanceof Route.DeleteEdit)//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
			{
				Route.DeleteEdit edit = (Route.DeleteEdit) un;
				if ((onlComponent == edit.onlComponent) && edit.contains(route)) return true;
			}
			return false;
		}
		public Route getRoute() { return route;}

		//NCCPOpManager.Operation
		public void opSucceeded(NCCP.ResponseBase resp)
		{
			NCCP_RouteRequester req = getRequest(resp.getRendMarker());
			if (req != null) 
			{
				requests.remove(req);
				System.err.println("Route.UpdateEdit.opSucceeded " + req.getOpString() + " for route " + req.getPMSent() + " port " + routeTable.getPort().getID());
				if (req.isAdd() && state == DELETE_FAILED) //this means it failed to delete the old entry but added the new one
				{
					//for this also need to change old route back
					Route tmp_rt = routeTable.addNewRoute();//new Route(newPM, newNH, routeTable);
					tmp_rt.setPrefixMask(newPM);
					tmp_rt.setNextHop(newNH);
					tmp_rt.properties.setProperty(COMMITTED_PM, newPM.getIPwMask());
					tmp_rt.properties.setProperty(COMMITTED_NH, newNH.toString());
					//routeTable.addRoute(tmp_rt);
				}
			}
			if (requests.isEmpty()) routeTable.removeOperation(this);
		}
		public void opFailed(NCCP.ResponseBase resp)
		{
			NCCP_RouteRequester req = getRequest(resp.getRendMarker());
			if (req != null) 
			{
				requests.remove(req);
				System.err.println("Route.UpdateEdit.opFailed " + req.getOpString() + " for route " + req.getPMSent() + " port " + routeTable.getPort().getID());

				if (req.isUpdate())
				{
					route.properties.setProperty(COMMITTED_NH, oldNH.toString());
					if (newNH.isEqual(route.nextHop)) route.nextHop = oldNH;
					routeTable.changeRoute(route);
					routeTable.removeOperation(this);
					onlComponent.getExpCoordinator().addError(new StatusDisplay.Error((new String("Route Operation Update Failure on port " + routeTable.getPort().getID() + " for route " + req.getPMSent() + " failed update operation")),
							(new String("Route Operation Failure on port " + routeTable.getPort().getID())),
							"Route Operation Failure.",
							(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));                        
				}
				if (req.getOp() == NCCP.Operation_DeleteFIPLRoute)
				{
					route.properties.setProperty(COMMITTED_PM, oldPM.getIPwMask());
					route.properties.setProperty(COMMITTED_NH, oldNH.toString());
					if (newPM.isEqual(route.prefixMask)) route.prefixMask = oldPM;
					if (newNH.isEqual(route.nextHop)) route.nextHop = oldNH;
					routeTable.changeRoute(route);
					state = DELETE_FAILED;
					onlComponent.getExpCoordinator().addError(new StatusDisplay.Error((new String("Route Operation Update Failure on port " + routeTable.getPort().getID() + " for route " + req.getPMSent() + " failed to delete old route")),
							(new String("Route Operation Failure on port " + routeTable.getPort().getID())),
							"Route Operation Failure.",
							(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));                        
				}
				if (req.getOp() == NCCP.Operation_AddFIPLRoute && state != DELETE_FAILED)	    
				{
					onlComponent.getExpCoordinator().addError(new StatusDisplay.Error((new String("Route Operation Update Failure on port " + routeTable.getPort().getID() + " for route " + req.getPMSent() + " failed to add new route")),
							(new String("Route Operation Failure on port " + routeTable.getPort().getID())),
							"Route Operation Failure.",
							(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));                        
					if (req.getPMSent().equals(route.prefixMask.getIPwMask()))
						routeTable.removeRoute(req.getRoute()); //may change this to just indicate a state change and show the difference in the table somehow
					else
					{ 
						if (req.getPMSent().equals(route.properties.getProperty(COMMITTED_PM))) route.properties.remove(COMMITTED_PM);//setProperty(COMMITTED_PM, null);
					}
				}
			}
			if (requests.isEmpty()) routeTable.removeOperation(this);
		}

		public void setNextHop(NextHop nnh, NextHop onh)
		{
			state = NEXTHOP;
			newNH.set(nnh);
			oldNH.set(onh);
		}

		public void setPrefixMask(PrefixMask npm, PrefixMask opm)
		{
			state = PREFIX;
			newPM.set(npm);
			oldPM.set(opm);
		}
	}
	///////////////////////////////////////////////// NextHop ////////////////////////////////////////////////////////////////

	public static class NextHop
	{
		protected int port = 0;
		protected int subport = 0;
		public NextHop() {}
		public NextHop(String s) throws java.text.ParseException
		{
			parseString(s);
		}
		public NextHop(NextHop nh)
		{
			this(nh.port, nh.subport);
		}
		public NextHop(int dp)
		{
			this(dp, 0);
		}
		public NextHop(int dp, int sp)
		{
			port = dp;
			subport = sp;
		}
		public void parseString(String s) throws java.text.ParseException
		{
			try 
			{
				if (ExpCoordinator.isTestLevel(ExpCoordinator.GIGETEST))
				{
					String strarray[] = s.split(":");

					int tmp_int = Integer.parseInt(strarray[0]);               
					if (tmp_int < 0 || tmp_int > 7) throw new java.text.ParseException("Route.NextHop not integer in 0-7 range", 0);
					else
					{
						port = tmp_int;
						if (Array.getLength(strarray) > 1) 
						{
							tmp_int = Integer.parseInt(strarray[1]);  
							if (tmp_int < 0 || tmp_int > 3) subport = 0;
							else subport = tmp_int;
						}
					}
				}
				else
				{
					int tmp_int = Integer.parseInt(s);
					if (tmp_int < 0 || tmp_int > 7) throw new java.text.ParseException("Route.NextHop not integer in 0-7 range", 0);
					else 
					{
						port = tmp_int;
						subport = 0;
					}
				}
			}
			catch (NumberFormatException e)
			{
				throw new java.text.ParseException("Route.NextHop not integer in 0-7 range", 0);
			}
		}
		public int getSubport() { return subport;}
		public void setSubport(int p) { subport = p;}
		public void setSubport(Router rtr) 
		{ 
			RouterPort prt = (RouterPort)rtr.getPort(port);
			if (prt != null)
				subport = prt.getPropertyInt(NSPPort.SUBPORT);
		}
		public int getPort() { return port;}
		public void setPort(int p) { port = p;}
		public boolean isEqual(NextHop nh)
		{
			return ((nh.port == port) && (nh.subport == subport));
		}
		public int getAsInt()
		{
			int rtn = (port << 2) | subport;
			return rtn;
		}
		public String toString()
		{
			if (ExpCoordinator.isTestLevel(ExpCoordinator.GIGETEST))
			{
				return (new String(port + ":" + subport));
			}
			else
				return (String.valueOf(port));
		}
		public void set(NextHop nh)
		{
			port = nh.port;
			subport = nh.subport;
		}
	}

	public Route(RouteTable rt)
	{
		routeTable = rt;
		router = (Router)rt.getPort().getParent();
		properties = new ONLPropertyList(this);
		setNextHop(new NextHop());
		prefixMask = new PrefixMask();
		properties.setProperty(PREFIX_SET, false);
		//properties.setProperty(MASK_SET, false);
		properties.setProperty(NEXTHOP_SET, false);
		properties.setProperty(ROUTE_EDITABLE, true);
		properties.setProperty(COMMITTED, false);
		properties.setProperty(GENERATED, false);
		resetUpdateEdit(rt);
		expCoordinator = ExpCoordinator.theCoordinator;
	}
	public Route(String str, RouteTable rt)  throws java.text.ParseException //form x.x.x.x/mask nexthop
	{
		this(rt);
		parseString(str);
		//String[] strarray = str.split(" ");
		//prefixMask = new PrefixMask(strarray[0]);
		//nextHop = new NextHop(strarray[1]);
	}

	public void parseString(String str) throws java.text.ParseException
	{
		String[] strarray = str.split(" ");
		setPrefixMask(new PrefixMask(strarray[0]));
		setNextHop(new NextHop(strarray[1]));
		if (strarray.length > 2) setGenerated(strarray[2]);
	}
	public Route(String pf, int m, NextHop nh, RouteTable rt)
	{
		this(rt);
		setNextHop(nh);
		try 
		{
			setPrefixMask(new PrefixMask(pf, m));
		}
		catch (java.text.ParseException e)
		{
			System.err.println("Route: error in setting prefix " + e.getMessage());
			prefixMask = new PrefixMask();
		}
		resetUpdateEdit(rt);
	}
	public Route(PrefixMask pfm, NextHop nh, RouteTable rt)
	{
		this(rt);
		setNextHop(nh);
		setPrefixMask(pfm);
		resetUpdateEdit(rt);
	}
	public Route(String pf, int m, int nh_op, int nh_sp, RouteTable rt)
	{
		this(pf, m, (new NextHop(nh_op, nh_sp)), rt);
	}
	public Route(Route r, RouteTable rt)
	{
		this(r.prefixMask, (new NextHop(r.nextHop)), rt);
		setGenerated(r.isGenerated());
		if (r.getStatsIndex() != 0) setStatsIndex(r.getStatsIndex());
	}
	public void setGenerated(String g) { properties.setProperty(GENERATED, g);}
	public void setGenerated(boolean g) { properties.setProperty(GENERATED, g);}
	public boolean isGenerated() { return (properties.getPropertyBool(GENERATED));}
	public PrefixMask getPrefixMask() { return prefixMask;}
	public void setPrefixMask(PrefixMask pf) 
	{ 
		properties.setProperty(PREFIX_SET, true);
		updateEdit.setPrefixMask(pf, prefixMask);
		String ostr = toString();
		prefixMask = pf;
		if (ExpCoordinator.isRecording())
			ExpCoordinator.recordEvent(SessionRecorder.PREFIX_MASK_RT, new String[]{routeTable.getPort().getLabel(), ostr, toString()});
	}
	public boolean isPrefixSet() { return ((Boolean.valueOf((String)properties.getProperty(PREFIX_SET))).booleanValue());}
	private void setEditable(boolean b)
	{
		properties.setProperty(ROUTE_EDITABLE, b);
	}
	public boolean isEditable()
	{
		return (properties.getPropertyBool(ROUTE_EDITABLE));
	}
	public Route.NextHop getNextHop() { return nextHop;}
	public void setNextHop(NextHop nh)  
	{ 
		String ostr = toString();
		if (nh != null)
		{
			properties.setProperty(NEXTHOP_SET, true);
			nh.setSubport(router);
			if (updateEdit != null) updateEdit.setNextHop(nh, nextHop);
		}
		nextHop = nh;
		if (ExpCoordinator.isRecording())
			ExpCoordinator.recordEvent(SessionRecorder.NEXTHOP_RT, new String[]{routeTable.getPort().getLabel(), ostr, toString()});
	}
	public void setNextHop(int p, int sp) { setNextHop(new NextHop(p, sp));}
	public void setStats(Stats s) 
	{ 
		if (stats != null) stats.setState(s.getState());
	} //the value of stats gets set through direct access to the stats instance
	public Route.Stats getStats(){ return stats;}
	public int getStatsIndex() 
	{ 
		if (stats != null) return (stats.getIndex());
		else return 0;
	}
	public void setStatsIndex(int i) 
	{
		String tmp_str = "";
		if (stats != null) stats.setIndex(i);
		else tmp_str = " stats null";
		ExpCoordinator.print(new String("Route.(" + toString() +").setStatsIndex " + i + tmp_str), PortStats.TEST_STATS);
	}
	public boolean isNextHopSet() 
	{ 
		boolean b = (Boolean.valueOf((String)properties.getProperty(NEXTHOP_SET))).booleanValue();
		//System.out.println("Route::isNextHopSet boolean = " + b + " property = " + (String)properties.getProperty(NEXTHOP_SET));
		return (b);
	}
	public boolean isAddressEqual(Route r)
	{
		return ( r.prefixMask.isEqual(prefixMask));
	}
	public boolean isEqual(Route r)
	{
		return ( r.nextHop.isEqual(nextHop) &&
				r.prefixMask.isEqual(prefixMask));
	}
	private byte[] getPrefixRAddr()
	{
		return ( prefixMask.getPrefixBytes());
	}
	public String toString()
	{
		if (prefixMask != null && nextHop != null)
			return (new String(prefixMask.toString() + " " + nextHop.toString()));
		else return "";
	}
	protected abstract void resetUpdateEdit(RouteTable rt);
	public ONLComponent.Undoable getUpdateEdit() 
	{
		updateEdit.setIsUndoable(true);
		return updateEdit;
	}
	public boolean isCommitted() 
	{ 
		return (properties.getPropertyBool(COMMITTED));
	}
	//begin interface ONLTableCellRenderer.Committable
	public boolean needsCommit()
	{
		if (properties.getProperty(COMMITTED) == null) return true;
		if (properties.getProperty(COMMITTED_PM) == null || !properties.getProperty(COMMITTED_PM).equals(getPrefixMask().getIPwMask()))
			return true;
		if (properties.getProperty(COMMITTED_NH) == null || !properties.getProperty(COMMITTED_NH).equals(getNextHop().toString()))
			return true;
		return false;
	}
	//end interface ONLTableCellRenderer.Committable
	public void setCommitted(boolean b) //used by observer
	{
		if (b)
		{
			properties.setProperty(COMMITTED, true);
			properties.setProperty(COMMITTED_PM, getPrefixMask().getIPwMask());
			properties.setProperty(COMMITTED_NH, getNextHop().toString());
		}
	}
	public String getProperty(String nm) { return (properties.getProperty(nm));}
	public RouterPort getPort() { return (routeTable.getPort());}
	public boolean isWritable() { return true;} //interface ONLCTable.Element
	public void write(ONL.Writer wrtr) throws java.io.IOException
	{
		wrtr.writeString(toString());
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.ROUTE);
		xmlWrtr.writeAttribute(GENERATED, properties.getProperty(GENERATED));
		xmlWrtr.writeStartElement(ExperimentXML.PREFIX_MASK);//"prefixMask");
		xmlWrtr.writeCharacters(getPrefixMask().toString());
		xmlWrtr.writeEndElement();
		xmlWrtr.writeStartElement(ExperimentXML.NEXTHOP);//"nextHop");
		xmlWrtr.writeCharacters(getNextHop().toString());
		xmlWrtr.writeEndElement();
		xmlWrtr.writeStartElement(ExperimentXML.STATS_INDEX);//"statsIndex");
		xmlWrtr.writeCharacters(String.valueOf(getStats().getIndex()));
		xmlWrtr.writeEndElement();
		xmlWrtr.writeEndElement();
	}
	public ONLComponent getTable() { return routeTable;}
	public Object getDisplayField(int c)
	{
		Object rtn = null;
		switch(c)
		{
		case PREFIX_FIELD:
			rtn = getPrefixMask();
			break;
		case NEXTHOP_FIELD:
			rtn = getNextHop();
			break;
		case STATS_FIELD:
			rtn = getStats(); //should be stats but don't know what exactly that will be may be more than just one column
			break;
		}
		return rtn;
	}
	public boolean isEditable(int c)
	{
		switch(c)
		{
		case PREFIX_FIELD:
		case NEXTHOP_FIELD:
			return true;
		case STATS_FIELD:
			return (getStats().getState() == PortStats.STOP);
		default:
			return false;
		}
	}
	public void setField(int c, Object a)
	{
		try
		{
			ExpCoordinator ec = ExpCoordinator.theCoordinator;
			switch(c)
			{
			case PREFIX_FIELD:
				setPrefixMask(new Route.PrefixMask((String)a));
				break;
			case NEXTHOP_FIELD:
				//ExpCoordinator.printer.print("RouteTable::setValue change next hop");
				setNextHop(new Route.NextHop((String)a));
				break;
			case STATS_FIELD:
				setStats((Route.Stats)a);
			}
			if (isCommitted() && c != STATS_FIELD)
			{
				ExpCoordinator.printer.print(new String("Route.setField inserting UpdateEdit for " + toString()), 7);
				ec.addEdit(getUpdateEdit());
			} 
		}
		catch (java.text.ParseException e)
		{
			System.err.println("Route::setField (" + c + ") error: " + e.getMessage());
		}
	} 
	public void setState(String s) { properties.setProperty(ONLComponent.STATE, s);}
	public String getState() { return (properties.getProperty(ONLComponent.STATE));}
}

