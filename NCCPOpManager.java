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
 * File: NCCPOpManager.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/25/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.*;

public abstract class NCCPOpManager extends MonitorEntry.Base
{
	//private MonitorDaemon mdaemon = null;
	private Vector<Operation> operations = null;
	private int currentIndex = 0;
	private ExpCoordinator expCoordinator = null;
	private boolean added = false;

	//////////////////////////////////////////////////////// interface Manager ///////////////////////////////////////////////////////
	public static interface Manager
	{
		public void removeOperation(Operation op);
		public void addOperation(Operation op);
	}
	/////////////////////////////////////////////////////interface Operation/////////////////////////////////////////////////////////

	public static interface Operation
	{
		public void opSucceeded(NCCP.ResponseBase resp);
		public void opFailed(NCCP.ResponseBase resp);
		public Vector getRequests(); //return list of requests to send
		public boolean containsRequest(REND_Marker_class rend);
	} //interface Operation

	///////////////////////////////////////////NCCPOpManager constructor and methods/////////////////////////////////////////////////
	public NCCPOpManager(MonitorDaemon md)
	{
		super(md);
		requestSent = true;
		operations = new Vector<Operation>();
		if (mdaemon == null) 
			ExpCoordinator.print("NCCPOpManager mdaemon null");
		expCoordinator = mdaemon.getExpCoordinator();
	}

	public boolean hasMarker(REND_Marker_class mrkr) 
	{
		if (getOperation(mrkr) != null) return true;
		return false;
	}

	public void addOperation(Operation op) { addOperation(op, false);}
	protected void addOperation(Operation op, boolean dont_send)
	{
		if (op == null) return;
		if (!added) 
		{
			mdaemon.addOpManager(this);
			added = true;
		}
		operations.add(op);
		Vector reqs = op.getRequests();
		int max = reqs.size();
		NCCP.RequesterBase elem;
		//System.out.println("NCCPOpManager.addOperation reqs " + max);
		for (int i = 0; i < max; ++i)
		{
			elem = (NCCP.RequesterBase)reqs.elementAt(i);
			setRendMarker(elem, op, getCurrentIndex());
			if (!dont_send) mdaemon.sendMessage(elem); //don't send if the daemon is calling this
		}
		if (expCoordinator == null)  expCoordinator = mdaemon.getExpCoordinator();
		//if (expCoordinator != null)  expCoordinator.addToProgressBar(op);
	}

	public void removeOperation(Operation op)
	{
		if (operations.contains(op)) operations.remove(op);
		if (expCoordinator == null)  expCoordinator = mdaemon.getExpCoordinator();
		//if (expCoordinator != null)  expCoordinator.removeFromProgressBar(op);
	}

	protected abstract void setRendMarker(NCCP.RequesterBase req, Operation op, int i);

	public void processResponse(NCCP.ResponseBase r) 
	{
		int max = operations.size();
		//System.out.println("NCCPOpManager.processResponse num elems " + max);  
		//r.getRendMarker().print();
		Operation elem = getOperation(r.getRendMarker());
		if (elem != null) 
		{
			if (r.getStatus() == NCCP.Status_Fine) elem.opSucceeded(r);
			else elem.opFailed(r);
		}
		else System.out.println(" NCCPOpManager.processResponse num elems " + max +  " no operation found");
	}
	private int getCurrentIndex()
	{
		if (currentIndex >= Integer.MAX_VALUE) currentIndex = 0;
		return (++currentIndex);
	}
	public Vector<Operation> getOperations() { return operations;}
	public boolean contains(Operation o){ return (operations.contains(o));}
	protected Operation getOperation(REND_Marker_class mrkr) 
	{
		int max = operations.size();
		Operation elem;
		for (int i = 0; i < max; ++i)
		{
			elem = operations.elementAt(i);
			if (elem.containsRequest(mrkr)) return elem;
		}
		return null;
	}
	protected void sendStopRequest(REND_Marker_class r1, REND_Marker_class r2) { mdaemon.sendStopRequest(r1, r2);}

	//public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException //should override
}
