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

