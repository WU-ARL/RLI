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
 * File: ExpDaemon.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/11/2004
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

public class ExpDaemon extends ONLDaemon //implements ExpRequest.Listener
{
  public static final int NCCP_Operation_AddLink = 38;
  public static final int NCCP_Operation_DeleteLink = 39;
  public static final int NCCP_Operation_AddComponent = 40;
  public static final int NCCP_Operation_DeleteComponent = 41;
  public static final int NCCP_Operation_AddCluster = 42;
  public static final int NCCP_Operation_ClearExperiment = 43;
  public static final int NCCP_Operation_EndCommit = 44;
  public static final int NCCP_Operation_RemapComponent = 46;
  public static final int NCCP_Operation_ReservationRequest = 47;
  public static final int NCCP_Operation_ExperimentRequest = 48; 
  public static final int NCCP_Operation_ResExtension = 49;
  public static final int NCCP_Operation_CancelRes = 50;
  public static final int NCCP_Operation_AddObserver = 51;
  public static final int NCCP_Operation_ObservableReq = 52;
  public static final int NCCP_Operation_ObserveExpReq = 53;
  public static final int NCCP_Operation_ObserveExpResp = 54;
  public static final int NCCP_Operation_ReservationLink = 58;
  public static final int NCCP_Operation_ReservationComponent = 59;
  public static final int NCCP_Operation_ReservationCluster = 60;
  public static final int NCCP_Operation_EndReservationReq = 61;


  private int reqCount = 0;
  private int ackcount = 0;
  protected Vector requests = null;
  protected boolean isClearing = false;

  public static class NCCP_Requester extends ExpRequest.RequesterBase
    {
      private ONLComponent onlComponent = null;
      private Experiment experiment = null;
      private String reservationID = null;

      public NCCP_Requester(int op, Experiment e)
       {
         super(op);
         experiment = e;
       }
      public NCCP_Requester(int op, ONLComponent c, Experiment e)
       {
         this(op,e);
         onlComponent = c;
       }

      public void store(DataOutputStream dout) throws IOException
      {
    	  if (returnMarker == null) setMarker(new REND_Marker_class());
    	  storeHeader(dout);
    	  if (reservationID == null)
    	  {
    		  if (experiment != ExpCoordinator.theCoordinator.getCurrentExp())
    			  ExpCoordinator.print(new String("ExpDaemon.NCCP_Requester.store experiment id = " + experiment.getProperty(Experiment.ONLID) + " experiment is not current"), 7);
    		  else
    			  ExpCoordinator.print(new String("ExpDaemon.NCCP_Requester.store experiment id = " + experiment.getProperty(Experiment.ONLID)), 7);
    		  NCCP.writeString(experiment.getProperty(Experiment.ONLID), dout);
    	  }
    	  else 
    	  {
    		  ExpCoordinator.print(new String("ExpDaemon.NCCP_Requester.store reservation id = " + reservationID), 7);
    		  NCCP.writeString(reservationID, dout);
    	  }
    	  storeData(dout);
      }
      public Experiment getExperiment() { return experiment;}
      public void setExperiment(Experiment e ) { experiment = e;}
      public void setReservationID(String s) { reservationID = new String(s);}
      public String getReservationID() { return reservationID;}
      public ONLComponent getONLComponent() { return onlComponent;}
      public void setONLComponent(ONLComponent c ) { onlComponent = c;}
      public void storeData(DataOutputStream dout) throws IOException 
      {
    	  NCCP.writeComponentID(onlComponent, dout);
      }
    }

  public static class NCCP_Response extends NCCP.ResponseBase
    {
      private String expID = "";
      private String compID = "";
	//private int compType = ONLComponent.UNKNOWN;

      public NCCP_Response(int dl)
       {
         super(dl);
       }
      public NCCP_Response()
       {
         super(0);
       }
      public void retrieveData(DataInput din) throws IOException
       {
	 ONL.NCCPReader rdr = new ONL.NCCPReader(din);
	 String tmp_string = rdr.readString(); //ipaddr of central node handling this exp not needed by GUI
	 double tmp_int = rdr.readUnsignedInt();//tcpport of central node handling this exp not needed by GUI
         expID = rdr.readString();
         rdr.readString();//read experiment name
         rdr.readString();//read user name
         
	 tmp_string = rdr.readString();//hardware type
         tmp_int = rdr.readUnsignedInt();//hardware reference
         double cluster_int = rdr.readUnsignedInt();//cluster reference
	 compID = rdr.readString();
         //compType = ONLComponent.getTypeInt(tmp_string);
	 tmp_string = rdr.readString();
	 rdr.readBoolean();
	 
	 ExpCoordinator.print(new String("ExpDaemon.NCCP_Response.retrieveData compID=" + compID), 5);
	 oStatus = status;
       }
      public String getExperimentID() { return expID;}
      public String getComponentID() { 
	  ExpCoordinator.printer.print(new String("ExpDaemon.NCCP_Response.getComponentID compID=" + compID), 10);
	 return compID;}
      //public int getComponentType() { return compType;}

      public void retrieveFieldData(DataInput din) throws IOException {}
      public double getData(MonitorDataType.Base mdt) { return 0;}
    }

  protected static class NCCP_RemapComponent extends ExpRequest
  {
    private ONLComponent onlComponent = null;
    private String label = null;
    private ExpDaemon expDaemon = null;
    
    private class NCCP_Requester extends ExpDaemon.NCCP_Requester
    {
      public NCCP_Requester(Experiment e)
	{
	  super(ExpDaemon.NCCP_Operation_RemapComponent, onlComponent, e);
	}

      public void store(DataOutputStream dout) throws IOException
	{
	  super.store(dout);
	  label = new String(onlComponent.getLabel());
	  ExpCoordinator.printer.print(new String("Experiment.NCCP_AddComponent.NCCP_Request remapping " + onlComponent.getCommittedLabel() + " to " + label+ " type=" + onlComponent.getType()), 5);
	  NCCP.writeString(onlComponent.getLabel(), dout);
          //dout.writeShort(onlComponent.getType());
	}
    }

    public NCCP_RemapComponent(ONLComponent ld, Experiment e, ExpDaemon ed)
     {
       super();
       onlComponent = ld;
       request = new NCCP_RemapComponent.NCCP_Requester(e);
       response = new NCCP_Response();
       expDaemon = ed;
     }
    public void processResponse(NCCP.ResponseBase r)
      {
	ExpCoordinator.printer.print(new String("NCCP_RemapComponent.processResponse status = " + r.getStatus()), 2);
	if (r.getStatus() == NCCP.Status_Fine) onlComponent.setCommittedLabel(label);
	else 
	  {
            if (label.equals(onlComponent.getLabel()))
              onlComponent.setProperty(ONLComponent.REMAP_SENT, false);
            //else something has changed that already reset things
	  }
	expDaemon.removeExpRequest(this);
      }
  }//end NCCP_RemapComponent

  public ExpDaemon(String h, int p, int tp) 
    {
      super(h, p, tp);
      ExpCoordinator.print("ExpD " + h + " " + p);
      ExpCoordinator.print(toString());
      requests = new Vector();
      addConnListener();
    }

  private void addConnListener()
    {
      addConnectionListener( new NCCPConnection.ConnectionListener() {
	public void connectionFailed(NCCPConnection.ConnectionEvent e)
	  {
	    clearReqs();
	    close();
            ExpCoordinator expc = ExpCoordinator.theCoordinator;
            expc.closeCurrentExp();
            expc.addError(new StatusDisplay.Error("Connection to Experiment Manager Failed. Closing Experiment now.", 
                                                  "Connection to Experiment Manager Failed",
                                                  "Experiment Closing", 
                                                  (StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR)));
	  }
	public void connectionClosed(NCCPConnection.ConnectionEvent e) { 
          ExpCoordinator.print("Connection to Experiment Manager Closed", 2);
          clearReqs();
          close();
          ExpCoordinator expc = ExpCoordinator.theCoordinator;
          expc.closeCurrentExp();
          expc.addError(new StatusDisplay.Error("Connection to Experiment Manager Closed. Closing Experiment now.", 
                                                "Connection to Experiment Manager Closed",
                                                "Experiment Closing", 
                                                (StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR)));
        }
	public void connectionOpened(NCCPConnection.ConnectionEvent e){}
      });
    }

  protected void processResponse(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException
    {
      NCCP.ResponseHeader responseHdr = new NCCP.ResponseHeader();
      int count = 2;
      ExpCoordinator.printer.print(new String("ExpDaemon.processResponse numRequests = " + requests.size()), 4);
      ExpCoordinator.printer.print(new String("msg size is " + msgsize), 10);
      responseHdr.retrieveHeader(dIn);
      count = 3;
      responseHdr.print(7);
      ExpRequest me = getExpRequest(responseHdr.getRendMarker());
      count = 4;
      if (me != null) 
	{
	  ExpCoordinator.print(new String("ExpDaemon.processResponse found experiment request " + me.getMarker().toString() + " " + me.getClass()), 4);
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
	  ExpCoordinator.printer.print(new String("ExpDaemon.processResponse skipping " + tmp + " bytes"));
	  if (tmp > 0) 
	    {
	      dIn.skipBytes(tmp);
	      count = 8;
	    }
	}
    }
  protected void processPeriodicMsg(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException 
    {
      ExpCoordinator.printer.print("ExpDaemon::run message is  NCCP.MessagePeriodic", 10);
      count = 9;
      NCCP.PeriodicMessage msg = new NCCP.PeriodicMessage(dIn);
      ExpRequest me = getExpRequest(msg.getRendMarker());
      if (me != null)
	{ 
	  if (msg.isAck()) 
	    { 
	      ++ackcount;
	      sendAck(msg);
	      ExpCoordinator.printer.print(new String("ExpDaemon::run message is  NCCP.MessagePeriodic ack " + ackcount), 9);
	      count = 10;
	    }
	  else 
	    {
	      ExpCoordinator.printer.print(new String("ExpDaemon::run message is  NCCP.MessagePeriodic stop ackcount = " + ackcount), 1);
	      //msg.getRendMarker().print();
	      count = 11;
	    }
	  me.processPeriodicMessage(msg);
	  count = 12;
	}
    }
  
 
  protected void sendRequest(ExpRequest me) 
    {
      ExpCoordinator.printer.print("ExpDaemon::sendRequest", 7);
      //ONLComponent c = me.getONLComponent();
      //if (c != null && c.needToRemap())
      //{
      // c.setProperty(ONLComponent.REMAP_SENT, true);
      //  NCCP_RemapComponent remap = new NCCP_RemapComponent(c, c.getExpCoordinator().getCurrentExp());
      //  remap.setMarker(getNextMarker()); 
      //  requests.add(remap);
      //  sendMessage(remap);
      //  remap.setRequestSent(true);
      //  }
      sendMessage(me);
      me.setRequestSent(true);
    }
	
  protected void addExpRequest(ExpRequest me)
    {
      me.setMarker(getNextMarker()); 
      requests.add(me);
      ExpCoordinator.print(new String("ExpDaemon::addExpRequest " + me.getMarker().toString()), 7);
      if (!isConnected())
	{
	  ExpCoordinator.printer.print(" connecting ");
	  connect();
	}
      sendRequest(me);
    }

  private REND_Marker_class getNextMarker()
   {
     return (new REND_Marker_class(++reqCount));
   }
  protected ExpRequest getExpRequest(REND_Marker_class mrkr)
    {
      ExpCoordinator.printer.print(new String("ExpDaemon::getExpRequest numRequests = " + requests.size()), 10);
      //mrkr.print(4);
      ExpRequest elem;
      int max = requests.size();
      for(int i = 0; i < max; ++i)
	{
	  elem = (ExpRequest)requests.elementAt(i);
	  REND_Marker_class r_mrkr = elem.getMarker();
	  if (r_mrkr == null) ExpCoordinator.print("ExpDaemon.getMEntry mrkr is null");
	  if (r_mrkr.isEqual(mrkr))
	    {
	      ExpCoordinator.print(new String("ExpDaemon.getMEntry found marker " + i + " = " + r_mrkr.toString()), 6);
	      return elem;
	    }
	}
      return null;
    }


  protected void removeExpRequest(ExpRequest r)
    {
	requests.remove(r);
    }

  protected void removeExpRequest(REND_Marker_class mrkr)
    {
      ExpCoordinator.print(new String("ExpDaemon::removeExpRequest " + mrkr.toString()), 1);
      ExpRequest elem;
      int max = requests.size();
      for (int i = 0; i < max; ++i)
	{
	  elem = (ExpRequest)requests.elementAt(i);
	  REND_Marker_class r_mrkr = elem.getMarker();
	  if (r_mrkr == null) ExpCoordinator.printer.print("ExpDaemon::removeMEntry mrkr is null");
	  if (r_mrkr.isEqual(mrkr))
	    {
	      //ExpCoordinator.printer.print("   found marker to remove");
	      //mrkr.print();
	      requests.remove(elem);
	      break;
	    }
	}
    }
  protected void clearReqs() 
    {
      isClearing = true;
      int max = requests.size();
      for(int i = 0; i < max; ++i)
	{
	  ExpRequest elem = (ExpRequest)requests.elementAt(i);
	  if (elem.isRequestSent()) 
	    {
	      elem.clear(); //not sure this makes sense
	    }
	}
      requests.removeAllElements();
      isClearing = false;
    }
}

