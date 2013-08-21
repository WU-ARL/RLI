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
 * File: MonitorDaemon.java
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
import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.SocketException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


public class MonitorDaemon extends ONLDaemon implements MonitorEntry.Listener, PropertyChangeListener
{
	public static final int TEST_RESPONSE = 5;
	public static final int TEST_SUBTYPEINIT = 6;
	private double ackcount = 0;
	protected Vector monitorEntries = null;
	protected Vector pendingMonEntries = null;
	//protected Vector pendingMessages = null;
	protected int numMEs = 0;
	protected boolean isClearing = false;
	private ONLComponent onlComponent = null;
	private ExpCoordinator expCoordinator = null;


	public MonitorDaemon(String h, int p, int tp, ONLComponent c)
	{
		this(h, p, tp, c, c.getExpCoordinator().isDirectConn());
	}
	public MonitorDaemon(String h, int p, int tp, ONLComponent c, boolean dir_conn) 
	{
		this(h, p, tp);
		if (!dir_conn && !ExpCoordinator.isSPPMon()) setProxy(c.getExpCoordinator().getProxy());
		else setNonProxy();
		onlComponent = c;
		c.addPropertyListener(ONLComponent.STATE, this);
		addConnectionListener(c.getConnectionListener());
	}

	public MonitorDaemon(String h, int p, int tp) 
	{
		super(h, p, tp);
		expCoordinator = ExpCoordinator.theCoordinator;
		ExpCoordinator.printer.print(new String("MonD " + h + " " + p + ": " +  toString()),3);
		monitorEntries = new Vector();
		pendingMonEntries = new Vector();
		pendingMessages = new Vector();
		addConnListener();
	}

	private void addConnListener()
	{
		addConnectionListener( new NCCPConnection.ConnectionListener() {
			public void connectionFailed(NCCPConnection.ConnectionEvent e)
			{
				clear();
			}
			public void connectionClosed(NCCPConnection.ConnectionEvent e) { clearMEs();}
			public void connectionOpened(NCCPConnection.ConnectionEvent e){}
		});
	}

	protected void processResponse(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException
	{
		NCCP.ResponseHeader responseHdr = new NCCP.ResponseHeader();
		count = 2;
		//ExpCoordinator.printer.print("msg size is " + msgsize);
		responseHdr.retrieveHeader(dIn);
		count = 3;
		//responseHdr.print();
		MonitorEntry.Base me = getMonitorEntry(responseHdr.getRendMarker());
		count = 4;
		if (me != null) 
		{
			ExpCoordinator.print(new String("MonitorDaemon(" + toString() + ") msgsize:" + msgsize + " msgtype:" + msgtype + " found monitor entry"), TEST_RESPONSE);
			count = 5;
			me.retrieveData(responseHdr, dIn);
			count = 6;
		}
		else //if me is null we still need to read out the response somehow so we should look at the op and read
		{
			int tmp = msgsize - 40; //size of non-periodic header
			if (responseHdr.isPeriodic())
			{
				count = 7;
				tmp -= 28;//extra added to header for periodic op
			}
			//responseHdr.getRendMarker().print();
			ExpCoordinator.printer.print(new String("MonitorDaemon.processResponse for " + host + " skipping " + tmp + " bytes"));
			responseHdr.print();
			if (tmp > 0) 
			{
				dIn.skipBytes(tmp);
				count = 8;
			}
		}
	}

	protected void processPeriodicMsg(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException
	{
		//ExpCoordinator.printer.print("MonitorDaemon::run message is  NCCP.MessagePeriodic");
		count = 9;
		NCCP.PeriodicMessage msg = new NCCP.PeriodicMessage(dIn);
		if (msg.isAck()) 
		{ 
			++ackcount;
			MonitorEntry.Base me = getMonitorEntry(msg.getRendMarker());
			count = 10;
			if (me != null) sendAck(msg);
			//ExpCoordinator.printer.print(host + ":- MonitorDaemon::run message is  NCCP.MessagePeriodic ack " + ackcount);
			count = 11;
		}
		else 
		{
			ExpCoordinator.printer.print(new String("MonitorDaemon::run message is  NCCP.MessagePeriodic stop ackcount = " + ackcount));
			//msg.getRendMarker().print();
			count = 12;
			MonitorEntry.Base me = getMonitorEntry(msg.getRendMarker());
			count = 13;
			if (me != null) 
			{
				me.setRequestSent(false);
				me.clear();
			}
		}
	}


	protected void sendRequest(MonitorEntry.Base me) 
	{
		//ExpCoordinator.printer.print("MonitorDaemon::sendRequest");
		if (!me.isRequestSent()) sendMessage(me);
		me.setRequestSent(true);
	}


	protected void sendStopRequest(MonitorEntry.Base me) 
	{
		if (me.getReturnMarker() != null) //we may have never received a response
		{
			//me.getReturnMarker().print();
			sendStopRequest(me.getReturnMarker(), me.getMarker());
		}
		me.setRequestSent(false);
	}

	protected void sendStopRequest(REND_Marker_class rmrkr,REND_Marker_class mrkr) 
	{
		if (rmrkr != null)
		{
			//form ack from message switching the rendezvous markers for the return trip back to the sender
			NCCP.PeriodicMessage stopmsg = new NCCP.PeriodicMessage(NCCP.Operation_StopPeriodic, rmrkr, mrkr);
			ExpCoordinator.printer.print(new String("MonitorDaemon::sendStopRequest"), 2);
			sendMessage(stopmsg);
		}
	}

	protected void addOpManager(NCCPOpManager opm) 
	{
		//opm.setRequestSent(true);
		//ExpCoordinator.printer.print("MonD.addOpManager");
		addMonitorEntry(opm);
	}

	protected void addMonitorEntry(MonitorEntry.Base me)
	{
		me.setDaemon(this);
		if (onlComponent == null || onlComponent.isActive())
		{
			if (!monitorEntries.contains(me)) monitorEntries.add(me);
			if (!isConnected())
			{
				connect();
			}
			sendRequest(me);
		}
		else 
		{
			if (!pendingMonEntries.contains(me)) pendingMonEntries.add(me);
		}
	}

	protected MonitorEntry.Base getMonitorEntry(REND_Marker_class mrkr)
	{
		MonitorEntry.Base elem = getMonitorEntry(mrkr, false);
		if (elem == null && pendingMonEntries.size() > 0)
			elem = getMonitorEntry(mrkr, true);
		return elem;
	}

	protected MonitorEntry.Base getMonitorEntry(REND_Marker_class mrkr, boolean pending)
	{
		//ExpCoordinator.printer.print("MonitorDaemon::getMonitorEntry");
		//mrkr.print();
		MonitorEntry.Base elem;
		Vector mes;
		if (pending) mes = pendingMonEntries;
		else mes = monitorEntries;
		int max = mes.size();
		ExpCoordinator.print(new String("MonitorDaemon.getMonitorEntry " + toString() + " max:" + max + " pending:" + pending), 6);
		for(int i = 0; i < max; ++i)
		{
			elem = (MonitorEntry.Base)mes.elementAt(i);
			if (elem.hasMarker(mrkr)) return elem;
			else
			{
				if (elem.getMarker() != null) elem.getMarker().print(7);
			}
			/*
	  REND_Marker_class r_mrkr = elem.getMarker();
	  if (r_mrkr == null) ExpCoordinator.printer.print("MonitorDaemon::getMEntry mrkr is null");
	  if (r_mrkr.isEqual(mrkr))
	    {
	      //ExpCoordinator.printer.print("    found marker ");
	      //r_mrkr.print();
	      return elem;
	    }
			 */
		}
		return null;
	}
	protected void removeMonitorEntry(MonitorEntry.Base me)
	{
		if (monitorEntries.contains(me))
		{
			if (me.isRequestSent()) 
			{
				ExpCoordinator.printer.print("    sendStop", 3);
				sendStopRequest(me);
			}
			monitorEntries.remove(me);
			me.setDaemon(null);
		}
		else
		{
			if (pendingMonEntries.contains(me))  
			{
				pendingMonEntries.remove(me);
				me.setDaemon(null);
			} 
		}
	}
	protected void removeMonitorEntry(REND_Marker_class mrkr)
	{
		ExpCoordinator.printer.print("MonitorDaemon::removeME", 3);
		//look in monitorEntries
		MonitorEntry.Base elem = getMonitorEntry(mrkr, false);
		if (elem != null)
		{
			removeMonitorEntry(elem);
		}
		else
		{//look in pendingMonEntries
			elem = getMonitorEntry(mrkr, true);
			if (elem != null) 
			{
				pendingMonEntries.remove(elem);
				elem.setDaemon(null);
			} 
		}
	}
	protected void clear()
	{
		clearMEs();
		close();
	}
	protected void clearMEs() 
	{
		isClearing = true;
		int max = monitorEntries.size();
		for(int i = 0; i < max; ++i)
		{
			MonitorEntry.Base elem = (MonitorEntry.Base)monitorEntries.elementAt(i);
			if (elem.isRequestSent()) 
			{
				sendStopRequest(elem);
				elem.clear();
			}
		}
		monitorEntries.removeAllElements();
		pendingMonEntries.removeAllElements();
		isClearing = false;
	}

	public Monitor addMonitor(Monitor m) //returns true if a new me is created and the polling rate is the same
	{
		Monitor rtn = null;
		if (!isConnected() && 
				!expCoordinator.isTestLevel(ExpCoordinator.NCCP_OFF) && 
				(onlComponent == null || onlComponent.isActive())) 
			connect();
		MonitorDataType.Base dt = m.getDataType();
		MonitorEntry.Base me = getMonitorEntry(MonitorEntry.Factory.getMarker(dt));
		if (me == null) 
		{
			ExpCoordinator.printer.print(new String("MonitorDaemon::addMonitor  " + dt.getName() + " " + dt.getMonitorID() + " creating new me"), 3);
			me = MonitorEntry.Factory.getNewMEntry(this, dt);

			if (m.getDataType().getParamType() != MonitorDataType.WUGSCLOCKRATE && m.getDataType().isPeriodic()) me.setPeriod(m.getDataType().getPollingRate());
			addMonitorEntry(me);
		}
		else
		{
			ExpCoordinator.printer.print(new String("MonitorDaemon::addMonitor  " + dt.getName() + " " + dt.getMonitorID() + " me exists already"), 3);
			if (!me.isPeriodic())
			{
				if (m.getDataType().isPeriodic())
					me.setPeriod(m.getDataType().getPollingRate());
				//and send another request
				sendRequest(me);
			}
			else
			{
				if (m.getDataType().isPeriodic() && !me.getPollingRate().isEqual(m.getDataType().getPollingRate()))
					m.getDataType().setPollingRate(me.getPollingRate());
			}
		}
		me.addMonitor(m);
		rtn = m;
		return rtn;
	}

	public Monitor removeMonitor(Monitor m)
	{
		MonitorDataType.Base dt = m.getDataType();
		MonitorEntry.Base me = getMonitorEntry(MonitorEntry.Factory.getMarker(dt));
		if (me != null) 
		{
			ExpCoordinator.print(new String("MonitorDaemon::removeMonitor  " + dt.getName() + " " + dt.getMonitorID()), 2);
			me.removeMonitor(m);
		}
		return m;
	}


	public void setConnected(boolean b)
	{
		String comp_label = "null";
		boolean newly_connected = b;
		if (b && getState() != ConnectionEvent.CONNECTION_OPENED) newly_connected = b;
		else newly_connected = false;
		if (onlComponent != null)
		{
			comp_label = new String(onlComponent.getLabel() + " isActive:" + onlComponent.isActive());
		}
		ExpCoordinator.print(new String("MonitorDaemon.setConnected " + toString() + " to " + b + " monEntries " + pendingMonEntries.size() + " messages " + pendingMessages.size() + " connection state:" + getState() + " " + comp_label), 6);
		
		super.setConnected(b, false);
		if (b) 
		{
			if (newly_connected && onlComponent != null && onlComponent instanceof Hardware)
			{
				Hardware hw = (Hardware)onlComponent;
				NCCPOpManager.Operation op = hw.getSubtypeInitOp();
				if (op != null) 
				{
					ExpCoordinator.print(new String("MonitorDaemon(" + onlComponent.getLabel() + ").setConnected sending subtypeInitOp"), TEST_SUBTYPEINIT);
					
					hw.getOpManager().addOperation(op, true);
					Vector reqs = op.getRequests();
					int max = reqs.size();
					NCCP.RequesterBase elem;
					//System.out.println("NCCPOpManager.addOperation reqs " + max);
					for (int i = 0; i < max; ++i)
					{
						elem = (NCCP.RequesterBase)reqs.elementAt(i);
						sendMessage(elem); //don't send if the daemon is calling this
					}
				}
			}
			processPendingMEs();
		}
		else
		{
			if (onlComponent != null && onlComponent.getState().equals(ONLComponent.ACTIVE))
			{
				onlComponent.setState(ONLComponent.FAILED);
				onlComponent.clear();
				String msg = new String(onlComponent.getLabel() + ": connection failed");
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(msg, msg, msg, (StatusDisplay.Error.STATUSBAR | StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP)));
			
			}
		}
	}

	private void processPendingMEs()
	{
		String comp_label = "null";
		if (onlComponent != null)
		{
			comp_label = new String(onlComponent.getLabel() + " isActive:" + onlComponent.isActive());
		}
		ExpCoordinator.print(new String("MonitorDaemon::processingPendingMEs " + toString() + " monEntries " + pendingMonEntries.size() + " messages " + pendingMessages.size() + " connection state:" + getState() + " " + comp_label), 6);
		if ((getState() == ConnectionEvent.CONNECTION_OPENED) && (onlComponent == null || onlComponent.isActive()))
		{
			if (pendingMonEntries.size() > 0)
			{
				int max = pendingMonEntries.size();
				for (int i = 0; i < max; ++i)
				{
					addMonitorEntry((MonitorEntry.Base)pendingMonEntries.elementAt(i));
				}
				pendingMonEntries.removeAllElements();
			}
			if (pendingMessages.size() > 0)
			{
				int max = pendingMessages.size();
				for (int i = 0; i < max; ++i)
				{
					sendMessage((NCCP.Message)pendingMessages.elementAt(i));
				}
				pendingMessages.removeAllElements();
			}
		}
	}

	//MonitorEntry.Listener
	public void monitorEntryEmpty(REND_Marker_class rend)
	{
		//ExpCoordinator.printer.print("MonitorDaemon::monitorEntryEmpty");
		if (!isClearing) removeMonitorEntry(rend);
	}  

	//PropertyChangeListener
	public void propertyChange(PropertyChangeEvent e)
	{
		if (onlComponent != null && e.getSource() == onlComponent)
		{
			if ((getState() == ConnectionEvent.CONNECTION_OPENED) && e.getPropertyName().equals(ONLComponent.STATE) && ((String)e.getNewValue()).equals(ONLComponent.ACTIVE))
			{
				ExpCoordinator.print(new String("MonitorDaemon::propertyChange " + e.getPropertyName() + " to " + ((String)e.getNewValue()) + " from " + ((String)e.getOldValue())), 6);
				processPendingMEs();
			}
		}
	}
	//end PropertyChangeListener
	public ExpCoordinator getExpCoordinator() 
	{ 
		if (onlComponent != null) return (onlComponent.getExpCoordinator());
		else return null;
	}
	public void sendMessage(NCCP.Message msg)
	{
		ExpCoordinator.print(new String("MonitorDaemon(" + onlComponent.getLabel() + ").sendMessage"), TEST_SUBTYPEINIT);
		if (onlComponent == null || onlComponent.isActive())
		{
			if (!ExpCoordinator.isTesting())
				super.sendMessage(msg);
		}
		else pendingMessages.add(msg);
	}
	/*
  public void setHost(String h) 
  {
    ExpCoordinator.printer.print("MonitorDaemon.setHost " + h);
    super.setHost(h);
  }
	 */
}

