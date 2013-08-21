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
 * File: NSPMonitor.java
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
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.Font;
import java.awt.event.*;
import java.awt.Color;
import java.awt.geom.Point2D;

//old switch descriptr
public class NSPMonitor extends NodeMonitor //WUGSMonitor
{
  protected int numPorts = 8;

  private NSPDescriptor nspDescriptor;
  protected MonitorDaemon wugsc = null; 
  protected MonitorDaemon nspc = null; 

  private double clockRate = 0;
  private int state = STOPPED;

  private static final int STOPPED = 0;
  private static final int WAITING_FOR_PING = 1;
  private static final int PINGED = 2;
  

  //protected WUGSPort ports[];
  private Ping ping = null;
  protected Vector pendingMonitors;


  /////////////////////////////////////////////////// NSPMonitor.Ping //////////////////////////////////////////////////////
  private class Ping implements Monitor
  {
    private NSPMonitor nspMonitor;
    private MonitorDataType.ClockRate dataType = null;

    public Ping(NSPMonitor sd)
      {
	nspMonitor = sd;
        dataType = new MonitorDataType.ClockRate(sd.getONLComponent());
      }
    public void setData(double val, double timeInt)
      {
	nspMonitor.pingSucceeded(val); 
      }
    public void setDataExact(double val, double timev){}//exact data value and exact time value 
    public void stop(){}
    public void start(){}
    public boolean isStarted() { return true;}
    public void addMonitorable(Monitorable m){}
    public void removeMonitorable(Monitorable m){}
    public MonitorDataType.Base getDataType(){ return dataType;}
    public void addMonitorFunction(MonitorFunction mf){}
    public void removeMonitorFunction(MonitorFunction mf){}
    public void setLogFile(java.io.File f){}
  }//end NSPMonitor.Ping



  /////////////////////////////////////////////////////// NSPMonitor methods /////////////////////////////////////////////////
  public NSPMonitor(Vector mds, ONLComponent comp, MonitorManager w) 
    {
      super(mds, comp, w);
      wugsc = getDaemon(ONLDaemon.WUGSC);
      numPorts = ((NSPDescriptor)comp).getNumPorts();
      if (monitorManager == null) ExpCoordinator.printer.print(new String("NSPMonitor::initializeDesc monitorManager is null"), 4);
      ping = new Ping(this);
      pendingMonitors = new Vector();
      if (ExpCoordinator.isObserver()) 
        {
          ExpCoordinator.print("NSPMonitor setting ping succeeded", 7);
          pingSucceeded(0);
        }
      nspc = getDaemon(ONLDaemon.NSPC);
      if (nspc != null) nspc.addConnectionListener(this);
      //System.out.println("NSPMonitor::NSPMonitor (NodeDescriptor.ID, NodeDescriptor.ID, MonitorManager)");
      //if (nspc != null) System.out.println( " "+ nspc.toString());
    }
  protected void initializeDaemons(Vector mds)
    {
      super.initializeDaemons(mds);
      ExpCoordinator.print("NSPMonitor.initializeDaemons", 4);
      wugsc =  getDaemon(ONLDaemon.WUGSC);

      nspc = getDaemon(ONLDaemon.NSPC);
    }

  public String toString()
    { 
      if (nspc != null && wugsc != null) return (new String(wugsc.toString() + " " + nspc.toString()));
      return (new String(""));
    }
  
  public void setClockRate(double cRate)
    { 
      clockRate = cRate;
    }
  
  public double getClockRate()
    { return clockRate;}
  

  public String getLabel() { return onlComponent.getLabel();}



  public int getNumPorts() { return numPorts;}


  public void connectToSwitch()
    {
      if (isStopped() && wugsc != null)
	{
	  ExpCoordinator.printer.print(new String("NSPMonitor::connectToSwitch"), 2);
	  wugsc.addMonitor(ping);
	  //ExpCoordinator.printer.print("NSPMonitor::connectToSwitch returned from sendRequest");
	  state = WAITING_FOR_PING;
	}
    }

  public void pingSucceeded(double cRate) // called after a successful ping of the switch
    { 
      ExpCoordinator.printer.print("NSPMonitor::pingSucceeded clockRate is " + cRate + " pending monitors " + pendingMonitors.size());
      setClockRate(cRate);
      state = PINGED;
      for ( int i = 0; i < pendingMonitors.size(); ++i)
	{
	  Monitor elem = (Monitor) pendingMonitors.elementAt(i);
	  //ExpCoordinator.printer.print("have element");
	  addMonitor(elem);
	  //had problems here with not adding elem correctly this should fix
	}
      pendingMonitors.removeAllElements();
    }
  public void startStatusMonitor(PollingRate pr)
    {/*
      for (int i = 0; i < numPorts; ++i)
	{
	  //CHANGE not sure what this should really become
	  //NEW input and output no longer seen as separate
	  //ports[i].getDataType().setPollingRate(pr);
	  //wugsc.addMonitor(ports[i]);
	  IPPs[i].getDataType().setPollingRate(pr);
	  wugsc.addMonitor(IPPs[i]);
	  OPPs[i].getDataType().setPollingRate(pr);
	  wugsc.addMonitor(OPPs[i]);
	}*/
    }

  public void endStatusMonitor()
    {/*
      for (int i = 0; i < numPorts; ++i)
	{
	  //NEW input and output no longer seen as separate
	  //wugsc.removeMonitor(ports[i]);
	  wugsc.removeMonitor(IPPs[i]);
	  wugsc.removeMonitor(OPPs[i]);
	}*/
    }


  public Monitor addMonitor(Monitor m)
    {
      if (isPinged())
	{
	  //ExpCoordinator.printer.print("NSPMonitor::addMonitor calling super.addMonitor");
	  return super.addMonitor(m);
	}
      else 
	{
	  //ExpCoordinator.printer.print("NSPMonitor::addMonitor calling adding to pending");
	  pendingMonitors.addElement(m);
	  connectToSwitch();
	  return m;
	}
    }  

  public void removeMonitor(Monitor m)
    {
      if (isPinged())
	{
	  super.removeMonitor(m);
	}
      else 
	{
	  pendingMonitors.remove(m);
	}
    }

  public Monitor addMonitor(MonitorDataType.Base dt)
    {
      //ExpCoordinator.printer.print("SwitchD::addMonitor  " + dt.getName() + " " + dt.getMonitorID());
      //need tp add a switchwide version
      if (dt.isNodeWide())
	{
	  Monitor rtn = new NumberData(dt, NumberData.DROP_OLD);
	  return (addMonitor(rtn));
	}
      return null;
      /*
      else
	{
	  //NEW input and output no longer seen as separate
	  //return (ports[dt.getPort()].addMonitor(dt));
	  if (dt.isIPP()) return (IPPs[dt.getPort()].addMonitor(dt));
	  else return (OPPs[dt.getPort()].addMonitor(dt));
	}*/
    }

  private boolean isPinged() { return (state == PINGED);}
  private boolean isWaitingForPing() { return (state == WAITING_FOR_PING);}
  private boolean isStopped() { return (state == STOPPED);}
}

