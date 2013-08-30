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
 * File: NSPRoute.java
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
//import onlrouting.*;


public class NSPRoute extends Route implements PropertyChangeListener
{

	public static class Stats extends Route.Stats
	{
		private MonitorDataType.Base mDataType = null;
		public Stats(String s) throws java.text.ParseException
		{
			super(s);
		}
		public Stats(RouterPort p, Route r)
		{
			super(p, r);
		}
		public MonitorDataType.Base getDataType()
		{
			//if (!filter.isEditable())
			//{
			if (mDataType == null)
				return (new NSPMonClasses.MDataType_RTStats((NSPPort)port, route, NSPMonClasses.MDataType_RTStats.COUNTER));
			if (getState() == ONE_SHOT) mDataType.setPeriodic(false);
			if (getState() == PERIODIC)
			{
				mDataType.setPeriodic(true);
				mDataType.setPollingRate(pollingRate);
			}
			return mDataType;
			//}
			//else return (new NSPMonClasses.MDataType_RTStats(port, route));
		}
		public MonitorDataType.Base getMonitorDataType()
		{
			NSPMonClasses.MDataType_RTStats mdt = null;
			//if ((getState() & PERIODIC) > 0)
			//{
			mdt = new NSPMonClasses.MDataType_RTStats((NSPPort)port, route, NSPMonClasses.MDataType_RTStats.RATE);  
			mdt.setPeriodic(true);
			mdt.setPollingRate(pollingRate);
			mdt.setName(route.prefixMask.getPrefix());
			//  }
			return mdt;
		}
		/*
    public void setValue(double val)
      {
	double ov = getValue();
	super.setValue(val);
	//route.fireEvent(new PortStats.Event(filter, PortStats.Event.STATS_CHANGED, ov, getValue()));
        }*/
	}//end class Stats

	/////////////////////////////////////////////////////// NCCP_RouteRequester ///////////////////////////////////////////
	public static class NCCP_RouteRequester extends NCCP.RequesterBase implements Route.NCCP_RouteRequester
	{
		private Route route;
		private NSPPort nspport;
		private String pmSent = null;
		private int nhSent = 0;
		private int statsSent = 0;
		private String op_str = "";

		public NCCP_RouteRequester(int op, NSPPort p, Route r)
		{
			super(op, true);
			route = r;
			nspport = p;

			switch(getOp())
			{
			case NCCP.Operation_AddFIPLRoute:
				op_str = "add route";
				break;
			case NCCP.Operation_UpdateFIPLNextHop:
				op_str = "update route";
				break;
			case NCCP.Operation_DeleteFIPLRoute:
				op_str = "delete route";
				break;
			}
			setMarker(new REND_Marker_class());
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			//String op_str = "";
			switch(getOp())
			{
			case NCCP.Operation_AddFIPLRoute:
				pmSent = new String(route.getPrefixMask().getIPwMask());
				nhSent = route.getNextHop().getAsInt();
				statsSent = route.getStats().getIndex();
				route.properties.setProperty(COMMITTED_PM, pmSent);
				route.properties.setProperty(COMMITTED_NH, route.getNextHop().toString());
				route.properties.setProperty(COMMITTED_STATS, statsSent);
				route.properties.setProperty(COMMITTED, true);
				route.routeTable.fireCommitEvent(route);
				break;
			case NCCP.Operation_UpdateFIPLNextHop:
				pmSent = new String(route.properties.getProperty(COMMITTED_PM));
				nhSent = route.getNextHop().getAsInt();
				statsSent = route.getStats().getIndex();
				route.properties.setProperty(COMMITTED_NH, route.getNextHop().toString());
				route.routeTable.fireCommitEvent(route);
				break;
			case NCCP.Operation_SetFIPLStatsIndex:
				pmSent = new String(route.getPrefixMask().getIPwMask());
				nhSent = route.getNextHop().getAsInt();
				statsSent = route.getStats().getIndex();
				route.properties.setProperty(COMMITTED_STATS, statsSent);
				route.routeTable.fireCommitEvent(route);
				break;
			case NCCP.Operation_DeleteFIPLRoute:
				route.getStats().reset();
				pmSent = new String(route.properties.getProperty(COMMITTED_PM));
				nhSent = route.getNextHop().getAsInt(); //doesn't really matter
				break;
			}
			dout.writeShort(nspport.getID());
			//write prefix in its byte form
			NCCP.writeString(pmSent, dout);
			NCCP.writeString(String.valueOf(nhSent), dout);
			dout.writeInt(statsSent);
			//ExpCoordinator.print(new String("NCCP_RouteRequester.storeData " + op_str + " for port:" + nspport.getID() + " prefix/mask:" + pmSent + " nextHop:" + nhSent), 6);
		}

		public Route getRoute() { return route;}
		public int getNHSent() { return nhSent;}
		public String getPMSent() { return pmSent;}
		public String getOpString() { return op_str;}
		public boolean isAdd() { return (getOp() == NCCP.Operation_AddFIPLRoute);}
		public boolean isDelete() { return (getOp() == NCCP.Operation_DeleteFIPLRoute);}
		public boolean isUpdate() { return (getOp() == NCCP.Operation_UpdateFIPLNextHop);}
	}

	////////////////////////////////////////////////////////// AddEdit /////////////////////////////////////////////////////////

	public static class AddEdit extends Route.AddEdit
	{
		public AddEdit(RouterPort c, Route r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp);
		}

		public AddEdit(RouterPort c, Vector r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp);
		}
		protected Route.NCCP_RouteRequester createRequest(Route route) { return (new NCCP_RouteRequester(NCCP.Operation_AddFIPLRoute, (NSPPort)getONLComponent(), route));}
	}//inner class AddEdit

	////////////////////////////////////////////////////////// DeleteEdit /////////////////////////////////////////////////////////
	//to make Delete Edit work for a multiple selection need to create a compound edit with several
	//Delete Edits and then do something about the cancellation. For now just limit to single selection
	//and single delete

	public static class DeleteEdit extends Route.DeleteEdit
	{
		public DeleteEdit(RouterPort c, Vector r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp);
		}
		protected Route.NCCP_RouteRequester createRequest(Route route) { return (new NCCP_RouteRequester(NCCP.Operation_DeleteFIPLRoute, (NSPPort)getONLComponent(), route));}
	}

	///////////////////////////////////////////// UpdateEdit ////////////////////////////////////////////////////////////
	public static class UpdateEdit extends Route.UpdateEdit
	{

		public UpdateEdit(RouterPort c, Route r, RouteTable rt, Experiment exp)
		{
			super(c, r, rt, exp);
		}

		protected void createRequests(String pm_str, String nh_str)
		{
			if (pm_str != null && pm_str.equals(route.getPrefixMask().getIPwMask()))
			{
				requests.add(new NCCP_RouteRequester(NCCP.Operation_UpdateFIPLNextHop, (NSPPort)getONLComponent(), route));
				route.properties.setProperty(COMMITTED_NH, route.getNextHop().toString());
				//ec.sendMessage(new NCCP_RouteRequester(NCCP.Operation_UpdateFIPLRoute, (NSPPort)getONLComponent(), route));
			}
			else //need to delete then add
			{
				if (route.getStats().getState() == PortStats.PERIODIC) 
				{
					route.getStats().stopMonitoring();
					route.getStats().setState(PortStats.STOP); //stop stats if they're running
				}
				requests.add(new NCCP_RouteRequester(NCCP.Operation_DeleteFIPLRoute, (NSPPort)getONLComponent(), route));
				requests.add(new NCCP_RouteRequester(NCCP.Operation_AddFIPLRoute, (NSPPort)getONLComponent(), route));
			}
		}
	}//end inner class UpdateEdit

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////Route Constructor

	public NSPRoute(RouteTable rt)
	{
		super(rt);
		stats = new Stats(rt.getPort(), this);
		stats.addListener(rt);
	}
	public NSPRoute(String str, RouteTable rt)  throws java.text.ParseException //form x.x.x.x/mask nexthop
	{
		this(rt);
		parseString(str);
	}

	public NSPRoute(String pf, int m, NextHop nh, RouteTable rt)
	{
		super(pf, m, nh, rt);
		stats = new Stats(rt.getPort(), this);
		stats.addListener(rt);
	}
	public NSPRoute(PrefixMask pfm, NextHop nh, RouteTable rt)
	{
		super(pfm, nh, rt);
		stats = new Stats(rt.getPort(), this);
		stats.addListener(rt);
	}
	public NSPRoute(String pf, int m, int nh_op, int nh_sp, RouteTable rt)
	{
		this(pf, m, (new NextHop(nh_op, nh_sp)), rt);
	}
	public NSPRoute(Route r, RouteTable rt)
	{
		this(r.prefixMask, (new NextHop(r.nextHop)), rt);
		setGenerated(r.isGenerated());
		if (r.getStatsIndex() != 0) setStatsIndex(r.getStatsIndex());
	}
	public void setNextHop(NextHop nh)  
	{ 
		boolean changed = (nh == null || nextHop == null || (nh.getPort() != nextHop.getPort()));
		if (changed)
		{
			NSPPort prt;
			if (nh == null)
			{
				if (nextHop != null) 
				{
					prt = (NSPPort)router.getPort(nextHop.getPort());
					if (prt != null) prt.removePropertyListener(NSPPort.SUBPORT, this);
				}
			}
			else
			{
				prt = (NSPPort)router.getPort(nh.getPort());
				if (prt != null) prt.addPropertyListener(NSPPort.SUBPORT, this);
			}
		}
		super.setNextHop(nh);
	}
	protected void resetUpdateEdit(RouteTable rt)
	{
		updateEdit = new UpdateEdit((NSPPort)rt.getPort(), this, rt, rt.getExpCoordinator().getCurrentExp());
	}
	//PropertyChangeListener
	public void propertyChange(PropertyChangeEvent e)
	{
		//if the subport changes change next hop and add edit
		int sub_port = ((NSPPort)router.getPort(nextHop.getPort())).getPropertyInt(NSPPort.SUBPORT);
		if (sub_port != nextHop.getSubport()) 
		{
			updateEdit.setIsUndoable(false);
			expCoordinator.addEdit(updateEdit);
			setNextHop(nextHop);//.getPort(), sub_port);
		}
	}
	//end PropertyChangeListener
}

