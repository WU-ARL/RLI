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
 * File: LogFileEntry.java
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
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Vector;
import java.awt.event.*;
import java.lang.*;

public class LogFileEntry implements Monitorable, Runnable //log file entry stored in aggregate LFileRegistry
{
  protected long updateRate; // in milliseconds
  protected double timeInterval; // saves the polling rate of the file data as seconds
  protected int numUpdates = 1; //number of reads to do per update
  protected MonitorDataType.LogFile dataType;
  protected LFDataReader reader;
  protected ONL.Reader dfile = null;//RandomAccessFile dfile = null;
  protected boolean started = false;
  protected boolean paused = false;
  protected MonitorPlus monitor = null;
  protected Thread thread = null;
  protected MonitorManager monitorManager = null;
  protected LogFileRegistry logFileRegistry;
  protected int dataBuffer = 400;
  private boolean inClear = false;
  private String filename = "";
  protected double currentTime = -1;
  protected double minTime = 0;
  protected final static int MAXUPDATES = 5;
  protected boolean hold = false;
  private ONL.StringReader stringReader = null;
  private boolean showAll = false;

  public static class NData extends NumberData
  {
    private LogFileEntry lfentry = null;
    
    public NData(MonitorDataType.Base mdt, LogFileEntry lfe)
      {
        super(mdt, 0, 0, 0, 0, NumberData.EXPAND);
        lfentry = lfe;
      }
    public void setShowAll(boolean b) { lfentry.setShowAll(b);}
    public void setDisplayRate(double s, double us) { lfentry.setDisplayRate(s, us);}
  }
  protected static interface LFDataReader
  {
    public void retrieveData(ONL.Reader din) throws IOException;//java.io.DataInput din) throws IOException;
    public double getData(MonitorDataType.Base mdt); //return the data specified by the data type
    public double getTime();
  }
  
  //inner class DefaultReader reads data in from file
  private class DefaultReader implements LogFileEntry.LFDataReader
  {
    private double currentX = 0;
    private double currentY = 0;
    public DefaultReader(){}
    public double getData() { return currentY;}
    public double getData(MonitorDataType.Base mdt) { return currentY;}
    public double getTime() { return currentX;}
    public void retrieveData(ONL.Reader din) throws IOException
      {
	ExpCoordinator.print("LogFileEntry.DefaultReader.retrieveData", 2);
        currentX = din.readDouble();
        currentY = din.readDouble();
	ExpCoordinator.print(new String("    data =( " + currentX + ", " + currentY + ")"), 2);
      }
  }// end inner class DefaultReader

  //protected LogFileEntry(File f, MonitorDataType.Base mdt, LogFileRegistry lfr) throws IOException;
  protected LogFileEntry(ONL.Reader in_str, MonitorDataType.Base mdt, LogFileRegistry lfr) throws IOException
    {
      dfile = in_str;
      //skip over data spec and get to the data
      //int max_lines = mdt.getSpecLength(); //length of data spec in number of lines
      //for (int i = 0; i < max_lines; ++i)
      //dfile.readLine();
      filename = ((ONL.BaseFileReader)in_str).getFileName();//f.getAbsolutePath(); //getName();
      ExpCoordinator.print(new String("LFE::filename = " + filename));
      dataType = new MonitorDataType.LogFile(filename, mdt);
      timeInterval = dataType.getPollingRate().getSecsOnly();
      ExpCoordinator.print(new String("LogFileEntry timeInterval " + timeInterval));
      monitorManager = lfr.getMonitorManager();
      logFileRegistry = lfr;
      //thread = new Thread(this);
      reader = new DefaultReader();
      stringReader = new ONL.StringReader("");
    }
   
  public static LogFileEntry loadFromFile(String fname, LogFileRegistry lfr)
    {
      return (LogFileEntry.loadFromFile(new java.io.File(fname) , lfr));
    }

  public static LogFileEntry loadFromFile(java.io.File f, LogFileRegistry lfr)
    {
      try
	{
	  LogFileEntry rtn = null;
	  //open file
	  ONL.BaseFileReader rdr = new ONL.BaseFileReader(f);
	  //ONL.InStream rdr = new ONL.InStream(f);
	 
	  //get data type
	  MonitorDataType.Base mdt = MonitorDataType.Base.read(rdr);//loadFromFile((new SpecFile.SpecReader(rdr)), null);
	  //rdr.finish();
	  int type = MonitorDataType.UNKNOWN;
	  if (mdt != null) type = mdt.getParamType();
	  //ExpCoordinator.print("type = ( " + type + ", " + s + ", " + us + ")");
	  //read in data type
	  //create entry
	  ExpCoordinator.print(new String("LogFileEntry::loadFromFile MonitorDataType type is " + type ), 1);
	  mdt.print(1);
          rtn = new LogFileEntry(rdr, mdt, lfr);
	  //return new entry
	  return rtn;
	}
      catch (IOException e)
	{
	  System.err.println("Error: LogFileEntry::loadFromFile - problem opening/reading file " + f.getName());
	}
      return null;
    }
  
  
  
  
  public void setHold(boolean b)
    {
      hold = b;
      if (monitor != null) ((BoxedRangeDefault)monitor).setHold(b);
    }
  public boolean getHold() { return hold;}

  private void readAll() 
    { 
      try
        {
          if (showAll) readNextUntil(0);
        }
      catch (IOException e)
        {
          stopReading();
          logFileRegistry.removeFile(dataType.getFileName());
          dfile = null;
          //clear();
        }
    }

  protected void readNextUntil(double tm) throws IOException //reads until it adds an element with a time value equal to or just under tm
    {
      if (reader != null && dfile.ready() && ((minTime <= tm) || showAll))
	{
          stringReader.reset(dfile.readLine());
          reader.retrieveData(stringReader);
	  processData();
	  while (((tm >= (currentTime + timeInterval)) || showAll) &&
                 dfile.ready())
	    {
              stringReader.reset(dfile.readLine());
	      reader.retrieveData(stringReader);
	      processData();
	    }
	}
    }
  
  protected void readNext() throws IOException //called from the runnable stuff
    {
      if ((reader != null) && dfile.ready()) 
	{
          ExpCoordinator.print("LogFileEntry.readNext", 2);
          stringReader.reset(dfile.readLine());
          reader.retrieveData(stringReader);
	  processData();
	}
    }
  
  public void finalize() throws Throwable
    {
      super.finalize();
      stopReading();
      if (dfile != null) dfile.finish();//close();
    }
  public void processData()
    {
      if (monitor == null ) 
        {
          ExpCoordinator.print("LogFileEntry::processData no monitors");
          return;
        }
      double timev = reader.getTime();
      double reltimev = timev - currentTime;
      if (currentTime < 0) reltimev = 0;
      currentTime = timev;
      double datav = ((DefaultReader)reader).getData();
      MonitorDataType.LogFile mdt;
      mdt = (MonitorDataType.LogFile)monitor.getDataType();
      if (!monitor.isStarted()) monitor.start();
      //ExpCoordinator.print("LogFileEntry::processData for monitor " + monitor.getDataType().getName());
      if (mdt.isExactReplay()) 
        {
          monitor.setDataExact(datav, timev);
          //ExpCoordinator.print("  exact replay (time, data) = (" + timev + ", " + datav + ")");
        }
      else 
        {
          monitor.setData(datav, reltimev);
          //ExpCoordinator.print("  relative replay (time, reltime, data) = (" + timev + ", " + reltimev + ", " + datav + ")");
        }
    }
  
  
  public Monitor addMonitor(MonitorDataType.Base mdt) { return (addMonitor(mdt, true, false));}
  public Monitor addMonitor(MonitorDataType.Base mdt, boolean startRead) { return (addMonitor(mdt, startRead, true));}
  public Monitor addMonitor(MonitorDataType.Base mdt, boolean startRead, boolean addToDisplay)
    {
      if (monitor == null)
	{
          monitor = new LogFileEntry.NData(mdt, this);
	}
      ExpCoordinator.print(new String("LogFileEntry.addMonitor addToDisplay = " + addToDisplay), 2);
      if (addToDisplay)
	{
	  ExpCoordinator.print("            adding to display", 2);
	  setSelected(monitor);
	}
      monitor.addMonitorable(this);
      if (!started && startRead) startReading();
      ExpCoordinator.print(new String("LogFileEntry::addMonitor " + monitor.getDataType().getName() + ", " +  monitor.getDataType().getMonitorID()), 2);
      return monitor;
    }
  
  public Monitor addMonitor(Monitor m) { return m;}
  public void setSelected(MonitorPlus mp)
    {
      ExpCoordinator.print("LogFileEntry.setSelected",2);
      if (monitorManager.getParamSelector().isListenersEmpty()) monitorManager.addDisplay(Graph.MULTILINE);
      monitorManager.getParamSelector().setCurrentParam(mp);
    }
  
  public void removeMonitor(Monitor m)
    {
      if (!inClear)
	{
          monitor = null;
          stopReading();
	}
    }


  public void showAvailableParams()//called from graph menu
    {
      //give the parameter info and allow to cancel	  
      final String opt0 = "OK";
      final String opt1 = "Cancel";
      Object[] options = {opt0,opt1};
      Object[] array = {new String(dataType.getName())};
      
      int rtn = JOptionPane.showOptionDialog((monitorManager.getExpCoordinator().getMainWindow()), 
					     array, 
					     "Data Information", 
					     JOptionPane.YES_NO_OPTION,
					     JOptionPane.QUESTION_MESSAGE, 
					     null,
					     options,
					     options[0]);
      if (rtn == JOptionPane.YES_OPTION)
	{
	  PollingRate pr = dataType.getPollingRate();
	  setDisplayRate(pr.getSecsOnly(), 0);
	  addMonitor(dataType,true, true);
	}
      else if (monitor == null) 
	{
	  try
	    {
	      logFileRegistry.removeFile(getFileName());
	      dfile.finish();//close();
	      dfile = null;
	    }
	  catch (IOException e)
	    {
	      System.err.println("Error closing file -- LogFileEntry::showAvailableParams");
	    }
	}
    }
  
  public void stopReading()
    { 
      if (started)
        {
          started = false;
          try
            {
              dfile.finish();
            }
          catch (IOException e)
            {
              System.err.println("Error closing file -- LogFileEntry::stopReading");
            }
        }
    }
  public void startReading()
    {
      if (monitor != null)
        {
          MonitorManager.TimeListener timeListener = monitorManager.getTimeListener();
          NumberData nd = (NumberData)monitor;
	  nd.setTimeListener(timeListener);
	  nd.addBoxedRangeListener(timeListener);
          if (showAll)
            readAll();
          else
            {
              if (thread == null)
                thread = new Thread(this);
              ExpCoordinator.print("LogFileEntry::startReading", 1);
              started = true;
              thread.start();
            }
        }
    }
  
  public void run()
    {
      while(started)
	{
	  try
	    {
	      if (!paused)
		{
		  //sleep
		  if (updateRate > 0) thread.sleep(updateRate);
		  //read in data until we've read enough to fill the update period or we reach end of file
		  setHold(true);
                  ExpCoordinator.print("LogFileEntry.run", 2);
		  for (int i = 0; i < numUpdates; ++i)
		    if (dfile.ready()) readNext();
		  setHold(false);
                  if (!dfile.ready()) stopReading();
		}
	    }
	  catch (IOException e)
	    {
              stopReading();
              logFileRegistry.removeFile(dataType.getFileName());
              dfile = null;
	      //clear();
	    }
	  catch (InterruptedException e) 
	    {
	      //do nothing
	    } 
	  Thread.yield();
	}
      thread = null;
      try 
	{
	 if (dfile != null)
	   {
	     logFileRegistry.removeFile(dataType.getFileName());
	     dfile.finish();//close();
	     dfile = null;
	   }
	}
      catch (IOException e3)
	{
	  System.err.println("Error: LogFileEntry::run couldn't close file");
	}
    }
  
  public void clear()
    {
      inClear = true;
      stopReading();
      if (monitor != null) monitor.stop();
      monitor = null;
      inClear = false;
    }
  
  public boolean isEqual(LogFileEntry e)
    {
      return (e.filename.compareTo(filename) == 0);
    }
  
  public boolean isEqual(String fname)
    {
      return (fname.compareTo(filename) == 0);
    }
  public void setDisplayRate(double s, double us) //arg supplies the number of seconds worth of data to show per display second
    {
      double tmp = 1;
      PollingRate pr = new PollingRate(s, us);
      dataType.setPollingRate(pr);
      PollingRate pr2 = dataType.getLogDataType().getPollingRate();
      tmp = (pr.getSecsOnly())/(pr2.getSecsOnly());

      if (tmp > MAXUPDATES)
	{
	  updateRate = (long)(Math.rint(1000/MAXUPDATES));
	  numUpdates = (int)(Math.rint(tmp/MAXUPDATES));
	}
      else
	{
	  updateRate = (long)(Math.rint(1000/tmp));
	  numUpdates = 1;
	}
      ExpCoordinator.print(new String("LFE::setDisplayRate rate is " + updateRate + " ms, numUpdates = " + numUpdates), 2);
    }

  public void setNumUpdates(int v) { numUpdates = v;}

  public void setShowAll(boolean b)
    {
      showAll = b;
      if (showAll)
        {
          if (monitor != null)
            {
              ((NumberData)monitor).setStyle(NumberData.EXPAND);
              readAll();
            }
          stopReading();
        }
      
    }

  public void pause() 
    { 
      ExpCoordinator.print("paused");
      paused = true;
    }
  public void resume() { paused = false;}
  public MonitorManager getMonitorManager() { return monitorManager;}
  public String getFileName() { return (dataType.getFileName());}
  public int getType() { return (dataType.getParamType());}
  protected MonitorDataType.Base getLogDataType() { return dataType.getLogDataType();}
      
}
