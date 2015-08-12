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
 * File: MonitorEntry.java
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
import java.io.*;

public class MonitorEntry
{
  public static interface Listener
  {
    public void monitorEntryEmpty(REND_Marker_class rend);//removeMonitorEntry(REND_Marker_class); //called when no monitors left in entry
  }

  public static class Factory
  {
    public Factory() {}

    public static MonitorEntry.Base getNewMEntry(MonitorEntry.Listener l, MonitorDataType.Base dt)
      {   
	MonitorEntry.Base me = null;
	if (!dt.isLiveData()) return null;
	switch(dt.getParamType())
	  {
	  case MonitorDataType.HWMONITOR:
	  case MonitorDataType.HWCOMMAND:
            me = new Hardware.MEntry(l, (Hardware.MDataType)dt);
            break;
	default:
	    ExpCoordinator.print("MonitorEntry::getMEntry unknown type");
	  }
	
	return me;
      }

    public static REND_Marker_class getMarker(MonitorDataType.Base dt)
      {
	REND_Marker_class mrkr = null;
	switch(dt.getParamType())
	  {

	  case MonitorDataType.HWMONITOR:
	  case MonitorDataType.HWCOMMAND:
            mrkr = Hardware.MEntry.getNewMarker(dt);
            break;
	  default:
	      ExpCoordinator.print("MonitorEntry::getMarker unknown type");
	  }

	return mrkr;
      }
  }
  public static abstract class Base implements Monitorable, NCCP.Message
  {
    protected REND_Marker_class marker;
    protected Vector monitors; //as framework expands this will probably become an aggregate
    protected NCCP.RequesterBase request = null; //forms the nccp request for this op
    protected NCCP.ResponseBase response = null; //parses the nccp response for this op
    protected boolean requestSent = false;
    protected MonitorEntry.Listener meListener = null;
    protected boolean inClear = false; //used to let us know if we're in the process of stopping monitors
    protected boolean msr = false;
    protected MonitorDaemon mdaemon = null;

    public Base(MonitorEntry.Listener sd)
      {
	this();
	meListener = sd;
      }
    
    public Base(MonitorDaemon md)
      { 
        this();
        mdaemon = md;
      }

    public Base()
      { 
	monitors = new Vector();
      }
    
    public Monitor addMonitor(Monitor m)
      {
	if (!monitors.contains(m)) 
	  {
	    monitors.add(m);
	    m.addMonitorable(this);
	  } 
	return m;
      }

    /*
    protected void addMonitor(Monitor m, int ndx)
      {
	monitors.add(ndx, m);
	m.addMonitorable(this);
      }
      */
    public void removeMonitor(Monitor m)
      { 
	  //ExpCoordinator.print("MonitorEntry.removeMonitor");
	if (!inClear)
	  {
	    monitors.remove(m);
	    if (monitors.isEmpty() && (meListener != null)) 
	      {
		ExpCoordinator.print(new String("MonitorEntry.removeMonitor " + m.getDataType().getName() + " entry is empty removing mentry"), 2);
		meListener.monitorEntryEmpty(marker);
		//need to send stop request through node descriptor
	      }
	    if (meListener == null) ExpCoordinator.print("    nodeD is null");
	    //ExpCoordinator.print("   monitors left " + monitors.size());
	  }
      }
    
    public REND_Marker_class getMarker() 
      { return marker;}
    
    public REND_Marker_class getReturnMarker() 
      { 
	if (response != null && response.isPeriodic()) 
	  {
	    return (response.getStopMarker());
	  }
	else return null;
      }
    
    public void setMarker(REND_Marker_class m)
      { 
	marker = m;
	if (request != null) request.setMarker(marker);
      }

    public boolean hasMarker(REND_Marker_class mrkr) { return (mrkr.isEqual(marker));} 

    public void clear()
      {
	//ExpCoordinator.print("ME::clear");
	inClear = true;
        int max = monitors.size();
	Monitor m;
	for (int i = 0; i < max; ++i)
	  {
	    m = (Monitor)monitors.elementAt(i);
	    m.stop();
	  }
	monitors.clear();
	//if (monitors.isEmpty() && (meListener != null)) 
	//{
	//   ExpCoordinator.print("   removing mentry");
	//   meListener.monitorEntryEmpty(marker);
	//need to send stop request through node descriptor
	//  }
	inClear = false;
      }
    
    public void store(DataOutputStream dout) throws IOException { storeRequest(dout);}
    public void storeRequest(DataOutputStream dout) throws IOException
      {
	//ExpCoordinator.print("ME::storeRequest");
	if (request != null) request.store(dout);
	else ExpCoordinator.print("ME::storeRequest   request is null");
      }
    
    public void storeStopRequest(DataOutputStream dout) throws IOException
      {
	if (response != null && response.isPeriodic()) 
	  {
	    NCCP.StopRequester req = new NCCP.StopRequester(marker, response.getStopMarker());
	    req.store(dout);
	  }
      }

    public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException
      {
	if (response != null) 
	  {
	      //ExpCoordinator.print("ME::retrieveData");
	      //hdr.getRendMarker().print();
	    response.setHeader(hdr); //set header info
	    response.retrieveData(din); //get op specific return data
	    processResponse(response);
	  }
      }
    public boolean isPeriodic() 
      {
	if (request != null) return (request.isPeriodic());
	else return false;
      }
    public void setPeriod(PollingRate pr)
      {
	if (request != null) 
	  {
	    //ExpCoordinator.print("     setting period to " + pr.getCSecs() +", " + pr.getCUSecs());
	    request.setPeriod(pr);
	    if (response != null) response.setPeriod(pr);
	  }
	//else ExpCoordinator.print("ME::setPeriod request is null");
      }
    public PollingRate getPeriod()
      {
	PollingRate pr = new PollingRate();
	if (response != null) 
	  {
	    pr.setRate(response.getPeriod().getSecs(), response.getPeriod().getUSecs());
	  }
	return pr;
      }
    public PollingRate getPollingRate()
      {
	PollingRate pr = new PollingRate();;
	if (response != null) 
	  {
	    pr.setRate(response.getPeriod().getSecs(), response.getPeriod().getUSecs());
	  }
	return pr;
      }
    public boolean isRequestSent(){ return requestSent;}
    public boolean isActive() { return requestSent;}
    public int getType()
      { 
	if (request != null) return (request.getOp());
	else return 0;
      }
    public void setRequestSent(boolean r) { 
      //ExpCoordinator.print("ME::SetRequestSent to " + r + " marker is ");
      //marker.print();
      requestSent = r;}
    public boolean isMSR() { return msr;}
    public abstract void processResponse(NCCP.ResponseBase r); //should pass parsed data to monitor
    protected void setDaemon(MonitorDaemon md) { mdaemon = md;}
  }

}
