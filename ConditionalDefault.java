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
