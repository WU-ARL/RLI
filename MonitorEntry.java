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
	  case MonitorDataType.IPPBANDWIDTH:
	    //ExpCoordinator.print("NodeDescriptor adding IPPBandwidth");
	    me = new MonitorEntry.IPPBandwidth(l, dt);
	    break;
	  case MonitorDataType.VCIBANDWIDTH:
	    //ExpCoordinator.print("NodeDescriptor adding VCIBandwidth");
	    me = new MonitorEntry.VCBandwidth(l, dt);
	    break;
	  case MonitorDataType.VPIBANDWIDTH:
	    //ExpCoordinator.print("NodeDescriptor adding VPIBandwidth");
	    me = new MonitorEntry.VPBandwidth(l, dt);
	  break;
	  case MonitorDataType.OPPBANDWIDTH:
	    //ExpCoordinator.print("NodeDescriptor adding OPPBandwidth");
	    me = new MonitorEntry.OPPBandwidth(l, dt);
	    break;
	  case MonitorDataType.IPPDISCARD:
	    //ExpCoordinator.print("NodeDescriptor adding IPPDiscard");
	    me = new MonitorEntry.IPPDiscard(l, dt);
	    break;
	  case MonitorDataType.OPPDISCARD:
	    //ExpCoordinator.print("NodeDescriptor adding OPPDiscard");
	    me = new MonitorEntry.OPPDiscard(l, dt);
	    break;
	  case MonitorDataType.WUGSCLOCKRATE:
	    me = new MonitorEntry.SwitchClockRate(l, dt);
	    break;
	  case MonitorDataType.PORTSTATUS:
	    //ExpCoordinator.print("NodeDescriptor adding OPPDiscard");
	    me = new MonitorEntry.ReadStatus(l, dt);
	    break;
	  case MonitorDataType.PLUGIN:
	    me = new PluginData.MEntry(l, dt);
	    break;
	  case MonitorDataType.HWMONITOR:
	  case MonitorDataType.HWCOMMAND:
            me = new Hardware.MEntry(l, (Hardware.MDataType)dt);
            break;
	default:
	    if ((dt.getParamType() & MonitorDataType.NSPOP) > 0)
	      me = NSPMonClasses.getNewMEntry(l, dt);
	    else
		  ExpCoordinator.print("MonitorEntry::getMEntry unknown type");
	  }
	
	return me;
      }

    public static REND_Marker_class getMarker(MonitorDataType.Base dt)
      {
	REND_Marker_class mrkr = null;
	switch(dt.getParamType())
	  {
	  case MonitorDataType.IPPBANDWIDTH:
	    //ExpCoordinator.print("MonitorEntry::getMarker IPPBandwidth");
	    mrkr = MonitorEntry.IPPBandwidth.getNewMarker(dt);//dt.getPort(), dt.getMonitorID());
	    break;
	  case MonitorDataType.VCIBANDWIDTH:
	    //ExpCoordinator.print("MonitorEntry::getMarker VCIBandwidth");
	    mrkr = MonitorEntry.VCBandwidth.getNewMarker(dt);//dt.getPort(), dt.getMonitorID());
	    break;
	  case MonitorDataType.VPIBANDWIDTH:
	    //ExpCoordinator.print("MonitorEntry::getMarker VPIBandwidth");
	    mrkr = MonitorEntry.VPBandwidth.getNewMarker(dt);//dt.getPort(), dt.getMonitorID());
	    break;
	  case MonitorDataType.OPPBANDWIDTH:
	    mrkr = MonitorEntry.OPPBandwidth.getNewMarker(dt);//dt.getPort(), dt.getMonitorID());
	    break;
	  case MonitorDataType.IPPDISCARD:
	    mrkr = MonitorEntry.IPPDiscard.getNewMarker(dt);//dt.getPort(), dt.getMonitorID());
	    break;
	  case MonitorDataType.OPPDISCARD:
	    mrkr = MonitorEntry.OPPDiscard.getNewMarker(dt);//dt.getPort(), dt.getMonitorID());
	    break;
	  case MonitorDataType.WUGSCLOCKRATE:
	    mrkr = MonitorEntry.SwitchClockRate.getNewMarker(dt);
	    break;
	  case MonitorDataType.PORTSTATUS:
	    mrkr = MonitorEntry.ReadStatus.getNewMarker(dt);//dt.getPort(), dt.getMonitorID());
	    break;
	  case MonitorDataType.PLUGIN:
	    mrkr = PluginData.MEntry.getNewMarker(dt);
	    break;
	  case MonitorDataType.HWMONITOR:
	  case MonitorDataType.HWCOMMAND:
            mrkr = Hardware.MEntry.getNewMarker(dt);
            break;
	  default:
	    if ((dt.getParamType() & MonitorDataType.NSPOP) > 0)
	      mrkr = NSPMonClasses.getNewMarker(dt);
	    else
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


  public static class IPPBandwidth extends Base
  {
    public static final int BANDWIDTH = 0;
    public static final int VXTCS0 = 5; //gets lumped in with IPPDiscard in some places so needs to be different than those enums
    public IPPBandwidth(MonitorEntry.Listener l, MonitorDataType.Base dt)
      { 
	super(l);
	int port = dt.getPort();
	request = new NCCP.RequesterReadIPPMR4(port);
	response = new NCCP.ResponseReadIPPMR4();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	//ExpCoordinator.print("ME::processResponse");
	Monitor monitor;
	int numParams = monitors.size();
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    if (monitor != null)
	      {
		MonitorDataType.IPPBandwidth mdt = (MonitorDataType.IPPBandwidth)monitor.getDataType();
		//ExpCoordinator.print("  calling monitor");
		if (!monitor.isStarted()) monitor.start();
		monitor.setData(r.getData(mdt), ((NCCP.ResponseReadIPPMR4)r).getTimeInterval());
	      }
	  }
      }

    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	int p = dt.getPort();
	return (new REND_Marker_class(p, true, NCCP.Operation_ReadMR, NCCP.GBNSC_MRegId_IPP_STATS_A));
      }
  }


  public static class IPPDiscard extends Base
  {
    public static final int RCBCLP0 = 1;
    public static final int RCBCLP1 = 2;
    public static final int CYCB = 3;
    public static final int BADHEC = 4;
    public static final int TOTAL = 0;
    public IPPDiscard(MonitorEntry.Listener sd, MonitorDataType.Base dt) 
      { 
	super(sd);
	int port = dt.getPort();
	request = new NCCP.RequesterReadIPPMR5(port);
	response = new NCCP.ResponseReadIPPMR5();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	Monitor monitor;
	//ExpCoordinator.print("ME.IPPDiscard::processResponse");
	int numParams = monitors.size();
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    MonitorDataType.IPPDiscard mdt = (MonitorDataType.IPPDiscard)monitor.getDataType();
	    //ExpCoordinator.print("  calling monitor");
	    if (!monitor.isStarted()) monitor.start();
	    monitor.setData(r.getData(mdt), ((NCCP.ResponseReadIPPMR5)r).getTimeInterval());
	  }
      }

    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	int p = dt.getPort();
	return (new REND_Marker_class(p, true, NCCP.Operation_ReadMR, NCCP.GBNSC_MRegId_IPP_STATS_B));
      }
  }


  public static class OPPBandwidth extends Base
  {
    public OPPBandwidth(MonitorEntry.Listener sd, MonitorDataType.Base dt) 
      { 
	super(sd);
	int port = dt.getPort();
	request = new NCCP.RequesterReadOPPMR15(port);
	response = new NCCP.ResponseReadOPPMR15();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	Monitor monitor;
	int numParams = monitors.size();
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    MonitorDataType.OPPBandwidth mdt = (MonitorDataType.OPPBandwidth)monitor.getDataType();
	    if (!monitor.isStarted()) monitor.start();
	    monitor.setData(r.getData(mdt), ((NCCP.ResponseReadOPPMR15)r).getTimeInterval());
	  }
      }

    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	int p = dt.getPort();
	return (new REND_Marker_class(p, false, NCCP.Operation_ReadMR, NCCP.GBNSC_MRegId_OPP_STATS_A));
      }
  }


  public static class OPPDiscard extends Base
  {
    public static final int XMBCS0 = 1;
    public static final int XMBCS1 = 2;
    public static final int TOOLATE = 3;
    public static final int RESEQUENCER = 4;
    public static final int TOTAL = 0;

    public OPPDiscard(MonitorEntry.Listener sd, MonitorDataType.Base dt)
      { 
	super(sd);
	int port = dt.getPort();
	request = new NCCP.RequesterReadOPPMR16(port);
	response = new NCCP.ResponseReadOPPMR16();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	Monitor monitor;
	int numParams = monitors.size();
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    MonitorDataType.OPPDiscard mdt = (MonitorDataType.OPPDiscard)monitor.getDataType();
	    if (!monitor.isStarted()) monitor.start();
	    monitor.setData(r.getData(mdt), ((NCCP.ResponseReadOPPMR16)r).getTimeInterval());
	  }
      }

    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	int p = dt.getPort();
	return (new REND_Marker_class(p, false, NCCP.Operation_ReadMR, NCCP.GBNSC_MRegId_OPP_STATS_B));
      }
  }


  public static class VPBandwidth extends Base
  {
    public VPBandwidth(MonitorEntry.Listener sd, MonitorDataType.Base dt) 
      { 
	super(sd);
	int port = dt.getPort(); 
	int vp = dt.getMonitorID();
	request = new NCCP.RequesterReadVPXTCC(port, vp);
	response = new NCCP.ResponseReadVPXTCC();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	Monitor monitor;
	int numParams = monitors.size();
        ExpCoordinator.print(new String("VPBandwidth.processResponse monitors.length = " + numParams), 4);
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    if (!monitor.isStarted()) monitor.start();
	    monitor.setData(r.getData(monitor.getDataType()), ((NCCP.ResponseReadVPXTCC)r).getTimeInterval());
	  }
      }

    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	int p = dt.getPort(); 
	int i = dt.getMonitorID();
	return (new REND_Marker_class(p, true, NCCP.Operation_ReadVPXTCC, i));
      }
  }

  public static class VCBandwidth extends Base
  {
    public VCBandwidth(MonitorEntry.Listener sd, MonitorDataType.Base dt) 
      { 
	super(sd);
	int port = dt.getPort();
	int vc = dt.getMonitorID();
	request = new NCCP.RequesterReadVCXTCC(port, vc);
	response = new NCCP.ResponseReadVCXTCC();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	Monitor monitor;
	int numParams = monitors.size();
        ExpCoordinator.print(new String("VCBandwidth.processResponse monitors.length = " + numParams), 4);
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    if (!monitor.isStarted()) monitor.start();
	    monitor.setData(r.getData(monitor.getDataType()), ((NCCP.ResponseReadVCXTCC)r).getTimeInterval());
	  }
      }

    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	int p = dt.getPort();
	int i = dt.getMonitorID();
	return (new REND_Marker_class(p, true, NCCP.Operation_ReadVCXTCC, i));
      }
  }

  public static class SwitchClockRate extends Base
  {

    public SwitchClockRate(MonitorEntry.Listener sd, MonitorDataType.Base dt)
      {
	super(sd);
	request = new NCCP.RequesterReadIPPMR1(1);
	request.setPeriodic(false);
	response = new NCCP.ResponseReadIPPMR1();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	//ExpCoordinator.print("ME::processResponse");
	Monitor monitor;
	int numParams = monitors.size();
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    if (monitor != null)
	      {
		//ExpCoordinator.print("  calling monitor");
		if (!monitor.isStarted()) monitor.start();
		monitor.setData(r.getData(monitor.getDataType()), 0); //no real time since this is a one shot response
	      }
	  }
      }

    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	return (new REND_Marker_class(-1, true, NCCP.Operation_ReadMR, NCCP.GBNSC_MRegId_IPP_CHIPINFO));
      }
  }


  public static class ReadStatus extends Base
  {
    public ReadStatus(MonitorEntry.Listener sd, MonitorDataType.Base dt)
      {
	super(sd);
	request = new NCCP.RequesterReadStatus();
	response = new NCCP.ResponseReadStatus();
	setMarker(getNewMarker(dt));
      }

    public void processResponse(NCCP.ResponseBase r)
      {
	Monitor monitor;
	MonitorDataType.Base mdt;
	int numParams = monitors.size();
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    mdt = monitor.getDataType();
	    if (monitor != null)  
	      {
		mdt = monitor.getDataType();
		if (!monitor.isStarted()) monitor.start();
		monitor.setData(r.getData(mdt), ((NCCP.ResponseReadStatus)r).getTimeInterval());
	      }
	  }
      }

  
    public static REND_Marker_class getNewMarker(MonitorDataType.Base dt)
      {
	return (new REND_Marker_class(-1, true, NCCP.Operation_ReadStatus));
      }
  }
}
