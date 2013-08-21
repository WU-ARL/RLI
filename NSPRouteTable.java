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
 * File: NSPRouteTable.java
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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import java.util.*;
//import onlrouting.*;

//note: when first add routes make sure they get the right subport value
public class NSPRouteTable extends RouteTable 
{

  private class OpManager extends NCCPOpManager
  {
    private FilterDescriptor.NCCP_FilterResponse prResponse = null;
    private class NCCP_Response extends NCCP.LTResponseBase
    {
      public NCCP_Response()
	{
	  super(8);
	}
      public void retrieveFieldData(DataInput din) throws IOException
	{
	  double tmp = NCCP.readUnsignedInt(din);
	  clkRate = NCCP.readUnsignedInt(din);
	  setLT(tmp);//since this computes the timeInterval we need the clock rate set first
	}
      public double getData(MonitorDataType.Base mdt) { return 0;}
      public String getString() { return (new String("Route Table Op for port " + port.getID()));}
    }//end RouteTable.OpManager.NCCP_Response
    public OpManager()
      {
	super((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC));
	response = new NCCP_Response();
        prResponse = new FilterDescriptor.NCCP_FilterResponse();
      }
    protected void setRendMarker(NCCP.RequesterBase req, NCCPOpManager.Operation op, int i)
      {
	req.setMarker(new REND_Marker_class(port.getID(), true, req.getOp(), i));
      }

    public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException
      {
        if (hdr.getOp() == NCCP.Operation_RTPriority)
          {
            prResponse.setHeader(hdr); //set header info
	    prResponse.retrieveData(din); //get op specific return data
	    processResponse(prResponse);
          }
        else super.retrieveData(hdr, din);
      }
  }

  //class TableModel
  private class TableModel extends RouteTable.TableModel //AbstractTableModel implements ONLTableCellRenderer.CommittableTable
  {
    public TableModel(RouteTable rtable)
      {
	super(rtable);
      }
    public TableCellEditor getEditor(int col) 
    {
      if (col == findColumn(STATS))
        {  
          try
            {
              Route.Stats stats[] = { new NSPRoute.Stats(PortStats.ONE_SHOT_STRING), new NSPRoute.Stats(PortStats.PERIODIC_STRING), new NSPRoute.Stats(PortStats.MONITOR_STRING)};
              JComboBox cb = new JComboBox(stats);
              cb.setEditable(true);
              return (new DefaultCellEditor(cb));
            }
          catch (java.text.ParseException e2)
            {
            }
        }
      return (super.getEditor(col));
    }
  } //class TableModel


  
  public NSPRouteTable(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
    {
      super(lbl,tp,tr, ec);
    }
  public NSPRouteTable(RouterPort p)
    {
      super(p);
    }
  public void createTableModel()
  {
    if (getTableModel() == null) setTableModel(new TableModel(this));
  }

  protected Route createRoute(ONL.Reader rdr) //adds an element read from some stream
    {
      NSPRoute route = null;
      try
        {
          String tmp_str = rdr.readString();
          String strarray[] = tmp_str.split(" ");
          if (Array.getLength(strarray) > 1)
            route = new NSPRoute(tmp_str, this);
          else
            {
              route = new NSPRoute(this);
              route.setPrefixMask(new Route.PrefixMask(tmp_str));
              route.setNextHop(new Route.NextHop(rdr.readString()));
            }
        }
      catch (java.io.IOException e) {}
      catch (java.text.ParseException e2) {}
      return route;
    }
  protected Route createRoute(Route r) { return (new NSPRoute(r, this));}
  protected Route createRoute() { return (new NSPRoute(this));}
  protected Route createRoute(String pf, int m, int nh_op, int nh_sp) { return (new NSPRoute(pf, m, nh_op, nh_sp, this));}

  protected Object addElement(ONLCTable.Element e, int s, boolean can_repeat)
    {
      Route r = (Route)super.addElement(e, s, can_repeat);
      if (r != null)
        {
          router.getPort(r.getNextHop().getPort()).addPropertyListener(NSPPort.SUBPORT, (NSPRoute)r);
          port.addStat(r.getStats());
        }
      return r;
    }
 
  protected ONLComponent.Undoable getAddEdit(Route rt)  { return (new NSPRoute.AddEdit((NSPPort)port, rt, this, expCoordinator.getCurrentExp()));}
  protected ONLComponent.Undoable getAddEdit(Vector rts)  { return (new NSPRoute.AddEdit((NSPPort)port, rts, this, expCoordinator.getCurrentExp()));}
  protected ONLComponent.Undoable getDeleteEdit(Vector rts) { return (new NSPRoute.DeleteEdit(port, rts, this, expCoordinator.getCurrentExp()));}
  protected ONLComponent.Undoable getDeleteEdit(Route rt) 
  { 
    Vector v = new Vector();
    v.add(rt);
    return (new NSPRoute.DeleteEdit(port, v, this, expCoordinator.getCurrentExp()));
  }
  protected void removeFromList(ONLCTable.Element e)
    {
      ExpCoordinator.print("RouteTable.removeFromList", 5);
      if (e != null && containsElement(e))
	{
          Route r = (Route)getElement(e);
          ExpCoordinator.print(new String("    found route " + r.toString()), 5);
          super.removeFromList(r);
	  if (r != null) //should always be true
	    {
              router.getPort(r.getNextHop().getPort()).removePropertyListener(NSPPort.SUBPORT, (NSPRoute)r);
              port.removeStat(r.getStats());
	    }
	}
    }
  
  //Priority.PRTable interface
  protected NCCPOpManager createOpManager()
    {
      OpManager op_man = new OpManager();
      ((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC)).addOpManager(op_man);
      return op_man;
    }
  public boolean isDataType(MonitorDataType.Base mdt)
  {
    return (mdt.getParamType() == NSPMonClasses.NSP_RTSTATS && mdt.getONLComponent() == this);
  }
  public Monitor addMonitor(MonitorDataType.Base mdt)
  {
    if (isDataType(mdt))
      {
        Route rt = ((NSPMonClasses.MDataType_RTStats)mdt).getRoute();
        if (rt != null)
          {
            return (rt.getStats().addMonitor(mdt));
          }
      }
    return null;
  }
  //end interface MonitorAction

  public Class[] getColumnTypes() 
  { 
    Route tmp_route = new NSPRoute(this); 
    Class[] types = new Class[]{
      tmp_route.getPrefixMask().getClass(), tmp_route.getNextHop().getClass(), tmp_route.getStats().getClass()
    };
    return types;
  }
}
