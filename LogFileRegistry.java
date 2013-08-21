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
 * File: LogFileRegistry.java
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
import java.io.File;


public class LogFileRegistry implements MenuFileAction.Saveable
{
  private Vector files;
  private MonitorManager monitorManager;
  
  public LogFileRegistry(MonitorManager w)
    {
      monitorManager = w;
      files = new Vector();
    }

  public Monitor addMonitorToFile(MonitorDataType.LogFile mdt)
    {
      System.out.println("LogFileRegistry::addMonitorToFile " + mdt.getFileName());
      LogFileEntry elem = getEntry(mdt.getFileName());
      if (elem == null)
	{
	  System.out.println("   file entry doesn't exist");
	  elem = LogFileEntry.loadFromFile(new File(mdt.getFileName()), this);
	  if (elem != null) 
	    {
	      System.out.println("   creation successful");
	      elem.setDisplayRate(mdt.getPollingRate().getSecsOnly(), 0);
	      files.add(elem);
	    }
	  else 
	    {
	      System.out.println("   creation unsuccessful");
	      return null;
	    }
	}
      return elem.addMonitor(mdt);
      //return elem;
    }

  public LogFileEntry getEntry(String fname)
    {
      LogFileEntry elem;
      int mx = files.size();
      System.out.println("LogFileEntry::getEntry");
      for (int i = 0; i < mx; ++i)
	{
	  elem = (LogFileEntry)files.elementAt(i);
	  
	  System.out.println("fname = " + fname + " elemnm = " + elem.getFileName());
	  if (elem.isEqual(fname))
	    {
	      return elem;
	    }
	}
      return null;
    }
  //MenuFileAction.Saveable interface 
  public void saveToFile(java.io.File f){}
  public void loadFromFile(java.io.File f)
    {
      LogFileEntry new_elem = getEntry(f.getName());

      if (new_elem == null) 
	{
	  new_elem = LogFileEntry.loadFromFile(f, this);
	  files.add(new_elem);
	}
      new_elem.showAvailableParams();
    }
  //end MenuFileAction.Saveable 

  public void removeFile(String fnm)
    {
      int mx = files.size();
      LogFileEntry elem;
      for (int i = 0; i < mx; ++i)
	{
	  elem = (LogFileEntry)files.elementAt(i);
	  if (elem.isEqual(fnm))
	    {
	      files.remove(i);
	      return;
	    }
	}
    }

  public void addEntry(LogFileEntry lfe)
    {
      files.add(lfe);
    }

  public MonitorManager getMonitorManager() { return monitorManager;}
}
