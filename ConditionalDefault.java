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
 * File: ConditionalDefault.java
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

import java.util.*;
import javax.swing.event.*;
public abstract class ConditionalDefault implements Conditional
{
  private EventListenerList listeners;
  protected MonitorDataType.Base dataType = null;
  protected boolean stopped = true;
  private boolean inStop = false;
  protected Vector monitorables;
  protected Vector mfunctions;
  protected boolean inUserStop = false;
  public ConditionalDefault(MonitorDataType.Base m)
    { 
      dataType = m;
      listeners = new EventListenerList();
      monitorables = new Vector();
      mfunctions = new Vector();
    }

  //Monitor interface 
  public abstract void setData(double val, double timeInt); //not sure if future monitors will take this form of params so this is why it is a separate descendant of Monitor
  public void setDataExact(double val, double timev){setData(val, timev);}//exact data value and exact time value 
  public void stop()
    {
      inStop = true;
      for (int i = 0; i < mfunctions.size(); ++i) 
	{
	  MonitorFunction g = (MonitorFunction)mfunctions.elementAt(i);
	  //System.out.println("calling MonitorFunction.removeData");
	  g.removeConditional(this);
	}
      mfunctions.clear();
      userStop();
      stopped = true;
      inStop = false;
    }
  public void userStop() 
    {
      inUserStop = true;
      System.out.println("NumberData::userStop for " + dataType.getName() + ", " + dataType.getMonitorID());
      for (int i = 0; i < monitorables.size(); ++i) 
	{
	  Monitorable m = (Monitorable)monitorables.elementAt(i);
	  //System.out.println("calling ME.removeMonitor");
	  m.removeMonitor(this);
	}
      monitorables.clear();
      inUserStop = false;
    }
  public void start() 
    { 
      for (int i = 0; i < mfunctions.size(); ++i) 
	{
	  MonitorFunction g = (MonitorFunction)mfunctions.elementAt(i);
	  //System.out.println("calling MonitorFunction.addData");
	  g.addConditional(this);
	}
      System.out.println("ConditionalDefault::start"); 
      stopped = false;
    }
  public boolean isStarted() { return (!stopped);}
  public void addMonitorable(Monitorable m)
    { 
      if (!monitorables.contains(m)) 
	{
	  //System.out.println("NumberData::addMonitorable");
	  monitorables.addElement(m);
	}
    }
  public void removeMonitorable(Monitorable m)
    { 
      if (!inUserStop) 
	monitorables.removeElement(m);
    }
  public MonitorDataType.Base getDataType() { return dataType;}
  public void addMonitorFunction(MonitorFunction mf)
    { 
      if (!mfunctions.contains(mf))
	{
	  System.out.println(dataType.getName() + "::addMonitorFunction");
	  if (!stopped) mf.addConditional(this);
	  mfunctions.addElement(mf);
	}
    }
  public void removeMonitorFunction(MonitorFunction mf)
    { 
      if (!inStop)
	{
	  //System.out.println(dataType.getName() + "::removeMonitorFunction");
	  mfunctions.removeElement(mf);
	  if (mfunctions.isEmpty()) userStop();
	}
    }
  //end Monitor interface 

  public abstract boolean isTrue(Monitor mp); //does test
  public void addConditionListener(ConditionListener l)
    { listeners.add(ConditionListener.class, l);}
  public void removeConditionListener(ConditionListener l)
    { listeners.remove(ConditionListener.class, l);}
  public void fireEvent(ConditionalEvent e)
    {
      int max = listeners.getListenerCount();
      int i = 0;
      Object[] list = listeners.getListenerList();
      ConditionListener l;
      //turns out this is a list of class, listener pairs so list looks like class1, listener1, class2, listener2 ... classn, listenern
      for (i = 1; i < list.length; i += 2)
	{
	  l = (ConditionListener) (list[i]);
	  l.changeCondition(e);
	}
    }
  public boolean isPassive() { return false;}
  public void setLogFile(java.io.File f){}
}
