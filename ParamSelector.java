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
 * File: ParamSelector.java
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
import java.awt.*;


public class ParamSelector
{
  private PListener paramListener = null;
  private boolean selectionEnabled = true;
  private boolean listeningEnabled = true;

  //static inner class ParamListener listens for change in ParamSelector
  public static class PListener
  {
    protected MonitorFunction mfunction;
    public PListener(MonitorFunction mf)
      {
	mfunction = mf;
      }
    public void changeInParam(ParamSelector.PEvent e)
      {
	((Monitor)e.getSource()).addMonitorFunction(mfunction);
      }
    public MonitorFunction getMFunction() { return mfunction;}
  }
  //end inner class ParamListener

  //inner class PEvent
  public static class PEvent extends AWTEvent
  {
    public PEvent(Monitor src)
      {
	super(src, -1);
      }
  }
  //end class PEvent


  public ParamSelector(){}
  public void addParamListener(MonitorFunction mf)
    {
      addParamListener(new PListener(mf));
      ExpCoordinator.print("ParamSelector.addParamListener",5);
    }
  public void removeParamListener(MonitorFunction mf)
    {
      if (paramListener != null && paramListener.getMFunction() == mf) paramListener = null;
    }
  public void addParamListener(ParamSelector.PListener plistener)
    {
      if (listeningEnabled) paramListener = plistener;
    }
  public void removeParamListener(ParamSelector.PListener plistener)
    {
      if (paramListener == plistener) paramListener = null;
    }
  public void setCurrentParam(Monitor param)
    {
      String tmp_str;
      if (paramListener == null) tmp_str = "is null";
      else tmp_str = "is not null";
      ExpCoordinator.print(new String("ParamSelector.setCurrentParam selectEnabled = " + selectionEnabled + " plistener " + tmp_str), 5);
      if (selectionEnabled && paramListener != null)
	{
          ExpCoordinator.print(new String("ParamSelector.setCurrentParam " + param.getDataType().getName()), 5);
	  paramListener.changeInParam(new PEvent(param));
	}
    }
  public void clear() { paramListener = null;}
  public void setListenEnable(boolean b) { listeningEnabled = b;}
  public boolean isListenEnabled() { return listeningEnabled;}
  public void setSelectionEnable(boolean b) { selectionEnabled = b;}
  public boolean isSelectionEnabled() { return selectionEnabled;}
  public boolean isListenersEmpty() { return (paramListener == null);}
  public PListener getCurrentListener() { return paramListener;}
  public boolean isOneShotMon()
   {
     if (paramListener == null) return false;
     else
       {
         return (paramListener.getMFunction() instanceof MonitorTable);
       }
   }
}
