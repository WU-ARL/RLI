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
