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
