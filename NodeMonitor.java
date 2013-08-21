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
 * File: NodeMonitor.java
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
import java.net.*;

public abstract class NodeMonitor implements NCCPConnection.ConnectionListener, ONLComponent.ONLMonitorable
{
  protected Vector daemons = null;
  protected MonitorManager monitorManager = null;
  protected Vector monitorEntries = null;
  protected ONLComponent onlComponent = null;

  public NodeMonitor(Vector d, ONLComponent c, MonitorManager w) 
    {
      this(c,w);
      //ExpCoordinator.printer.print("NodeMonitor::NodeMonitor (int, NodeMonitor.ID, MonitorManager)");
      initializeDaemons(d);
    }
  public NodeMonitor(ONLComponent c, MonitorManager w)
    {
      daemons = new Vector();
      onlComponent = c;
      monitorManager = w;
    }

  protected void initializeDaemons(Vector mds)
    {
      int max = mds.size();
      for (int i = 0; i < max; ++i)
	{
	  addDaemon((MonitorDaemon)mds.elementAt(i));
	}
    }
  //CHANGE to get specific daemon
  public MonitorDaemon getDaemon(int tp)
    {
      int max = daemons.size();
      MonitorDaemon elem;
      ExpCoordinator.printer.print(new String("NodeMonitor.getDaemon looking for " + tp + " daemons:" + max), ExperimentXML.TEST_XML);
      for (int i = 0; i < max; ++i)
	{
	  elem = (MonitorDaemon)daemons.elementAt(i);
	  if (elem.isType(tp)) return elem;
	}
      ExpCoordinator.printer.print(new String("NodeMonitor.getDaemon looking for " + tp + " didn't find"), 1);
      return null;
    }
  
  public void addDaemon(MonitorDaemon md) 
    { 
      if (!daemons.contains(md)) 
	{
	  ExpCoordinator.print(new String("NodeMonitor(" + onlComponent.getLabel() + ").addDaemon " + md.toString()), 5);
	  daemons.add(md);
	  md.addConnectionListener(this);
	}
    }

  public void removeDaemon(MonitorDaemon md) { daemons.remove(md);}


  public MonitorManager getMonitorManager() { return monitorManager;}
  public void setMonitorManager(MonitorManager w) { monitorManager = w;}

  //NCCPConnection.ConnectionListener 
  public void connectionFailed(NCCPConnection.ConnectionEvent e)
    {
      //removeDaemon((MonitorDaemon)(e.getSource()));
    }
  public void connectionClosed(NCCPConnection.ConnectionEvent e){}
  public void connectionOpened(NCCPConnection.ConnectionEvent e){}
  //end interface NCCPConnection.ConnectionListener 


  public abstract Monitor addMonitor(MonitorDataType.Base dt);
  public Monitor addMonitor(Monitor m)
    {
      ExpCoordinator.printer.print(new String("NodeMonitor.addMonitor(Monitor)"), 1);
      MonitorDaemon md = getDaemon(m.getDataType().getDaemonType());
      if (md != null) return (md.addMonitor(m));
      else
        ExpCoordinator.printer.print("NodeMonitor.addMonitor(Monitor) daemon is null");
      return null;
    }
  public void removeMonitor(Monitor m)
    {
      MonitorDaemon md = getDaemon(m.getDataType().getDaemonType());
      if (md != null) md.removeMonitor(m);
    }

  //public boolean isEqual(NodeMonitor nd) { return (nodeID.isEqual(nd.nodeID));}
  //public boolean isEqual(NodeMonitor.ID nid) { return (nodeID.isEqual(nid));}
  // public boolean isActive() { return (numMEs > 0);}
  //public boolean isWUGS() { return (onlComponent.getType() == ONLComponent.NSP || onlComponent.getType() == ONLComponent.WUGS);}
  /*
    public boolean isNSP() { return (onlComponent.getType() == ONLComponent.NSP);}
    public boolean isEndNode() { return (onlComponent.getType() == ONLComponent.HOST);}
  */
  //public void sendMessage(NCCP.Message msg) { nodeID.getConnection().sendMessage(msg);}

  public ONLComponent getONLComponent() { return onlComponent;}
  public void clear()
    {
      int max = daemons.size();
      MonitorDaemon elem;
      for (int i = 0; i < max; ++i)
	{
	  elem = (MonitorDaemon)daemons.elementAt(i);
	  elem.clear();
	}
    }
  public void addActions(ONLComponent.CfgMonMenu menu) {}
}

