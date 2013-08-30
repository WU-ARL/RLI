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
 * File: Router.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 6/6/2006 6/6/2006
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.Vector;
import java.lang.String;
import java.io.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;
import java.lang.reflect.Array;
import org.xml.sax.*;

public abstract class Router extends Hardware
{
	public static final String INTERNALBW = "internalbw";


	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Router(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec, int n_ports) throws IOException
	{
		super(lbl, tp, tr, ec, n_ports);
	}
	protected Router(String lbl, String tp, ExpCoordinator ec, int n_ports)
	{
		super(lbl, tp, ec, n_ports);
	}
	protected Router(String lbl, String tp, String cp, ExpCoordinator ec, int n_ports)
	{
		super(lbl, tp, cp, ec, n_ports);
	}

	//constructors to support router types that use Hardware interface
	protected Router(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{
		super(lbl, tp, tr, ec);
	}
	//protected Router(int ndx, ExpCoordinator ec, int tp, Hardware hw_type)
	//{
	//super(ndx, ec, tp, hw_type);
	//}
	// protected Router(String lbl, ExpCoordinator ec, int tp, Hardware hw_type)
	//{
	//super(lbl, ec, tp, hw_type);
	//}

	protected Router(String uri, Attributes attributes) { super(uri, attributes);}

	/*
  protected void initializeActions()
    {
      ExpCoordinator.print("Router.initializeActions", 5);
     if (getInternalBW() == 0) setInternalBW(QueueTable.DEFAULT_SWBANDWIDTH);
     super.initializeActions();
     addStateListener(INTERNALBW);
    }//define and initialize menu options that are accessed through center button of graphic
	 */
	/*
  protected boolean isAncestor(int tp) 
  {
    if (tp == getType()) return true;
    switch(tp)
      {
      case (ONLComponent.RTABLE):
      case (ONLComponent.FTABLE):
      case (ONLComponent.IPPGMFTABLE):
      case (ONLComponent.IPPEXGMFTABLE):
      case (ONLComponent.OPPEMFTABLE):
      case (ONLComponent.OPPGMFTABLE):
      case (ONLComponent.OPPEXGMFTABLE):
      case (ONLComponent.QTABLE):
      case (ONLComponent.OPPQTABLE):
      case (ONLComponent.PCLASSES):
      case (ONLComponent.PINSTANCES):
        return true;
      default:
        return (isParent(tp));
      }
  }
	 */
	protected void setInternalBW(double ibw)
	{
		if (getPropertyDouble(INTERNALBW) != ibw && ibw > 0)
		{
			properties.setProperty(INTERNALBW, ibw);
			//addToDescription(new String("Max Internal Bandwidth: " + ibw + " Mbps"));
			if (ExpCoordinator.isRecording())
				ExpCoordinator.recordEvent(SessionRecorder.SWBANDWIDTH, new String[]{getLabel(), String.valueOf(ibw)});
		}
	}
	public double getInternalBW() { return (properties.getPropertyDouble(INTERNALBW));}

	public void processEvent(ONLComponent.Event e)
	{
		ExpCoordinator.print(new String(getLabel() + ".Router.processEvent type " + e.getType()), 6);
		if (e instanceof ONLComponent.PropertyEvent)
		{
			PropertyEvent pe = (PropertyEvent)e;
			if (pe.getLabel().equals(INTERNALBW)) 
			{
				String val = pe.getNewValue();
				if (val != null && val.length() > 0)
					setInternalBW(Double.parseDouble(val));
				return;
			}
		}
		super.processEvent(e);
	}
	//public void readParams(ONL.Reader reader) throws IOException
	//{ 
	//  if (reader.getVersion() < 2.6)
	//    {
	//      String str = reader.readString();
	//      setCPHost(str);
	//    }
	//  else super.readParams(reader);
	//}

	public void addRoute(String subnet, int m, Route.NextHop nh)
	{    
		RouterPort tmp_port;
		RouteTable rt = null;
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (RouterPort)ports.elementAt(i);
			rt = (RouteTable)tmp_port.getTableByType(ONLComponent.RTABLE);
			//CHANGED 9/17/10 //if (rt != null) rt.addRouteReplace(rt.createRoute(subnet, m, nh));
			if (rt != null) rt.addRoute(rt.createRoute(subnet, m, nh));
		}
	}
	public void addGeneratedRoute(String subnet, int m, Route.NextHop nh)
	{    
		RouterPort tmp_port;
		RouteTable rt = null;
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (RouterPort)ports.elementAt(i);
			rt = (RouteTable)tmp_port.getTableByType(ONLComponent.RTABLE);
			//CHANGED 9/17/10 //if (rt != null) rt.addRouteReplace(rt.createRoute(subnet, m, nh));
			if (rt != null) rt.addNewElement(rt.createGeneratedRoute(subnet, m, nh));
		}
	}
	
	public void generateDefaultRts()
	{
		RouterPort tmp_port;
		ExpCoordinator.print(new String("Router(" + getLabel() + ").generateDefaultRts"), TEST_SUBNET);
		for (int i = 0; i < numPorts; ++i)
		{
			tmp_port = (RouterPort)ports.elementAt(i);
			SubnetManager.Subnet sn = tmp_port.getSubnet();
			if (sn != null)
				addGeneratedRoute(sn.getBaseIP().toString(), sn.getNetmask().getBitlen(), new Route.NextHop(i, 0));
		}
	}
	
	public RouterPort.SamplingSetAction.Requester getSamplingRequester() { return null;}
	//public boolean isRouter() 
	//{
	//	return (super.isRouter());
	//}
	protected boolean mergeParams(ONLComponent c)
	{
		if (c instanceof Router)
		{
			Router rtr2 = (Router)c;
			ONLComponentList children = getChildren();
			//check any tables
			int max = children.getSize();
			ONLCTable c2;
			ONLComponent c1;
			for (int i = 0; i < max; ++i)
			{
				c1 = children.onlComponentAt(i);
				if (c1 instanceof ONLCTable)
				{
					c2 = rtr2.getTableByType(c1.getType());
					if (c2 == null || !c1.merge(c2)) return false;
				}
			}
			return true;
		}
		else return false;
	}
}
