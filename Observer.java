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
 * File: Observer.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 8/9/2007
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;

public class Observer implements MonitorManager.Listener, Experiment.Listener, ONLComponent.Listener, StatusDisplay.Listener
{
  public static final int TEST_OBS = 3;

  public static final int NCCP_Operation_Chat = 38;
  public static final int NCCP_Operation_StateRequest = 39;
  public static final int NCCP_Operation_StateResponse = 40;
  public static final int NCCP_Operation_EventResponse = 41;
  public static final int NCCP_Operation_TblEventResponse = 42;
  public static final int NCCP_Operation_ConnEventResponse = 43;
  public static final int NCCP_Operation_CommitResponse = 44;
  public static final int NCCP_Operation_MonitorEventResponse = 45;
  public static final int NCCP_Operation_MoveEventResponse = 46;
  public static final int NCCP_Operation_QuitMessage = 47;
  public static final int NCCP_Operation_ErrorResponse = 48;
  public JFrame frame = null;
  public JTextArea stateTArea = null;
  public JTextArea chatReceive = null;
  public JTextArea chatSend = null;
  public static Observer theObserver = null;
  private ObserveDaemon odaemon = null;
  private StateResponse stateResponse = null;
  private boolean stateSent = false;
  private JMenu observableExps = null;


  private class LineListener extends KeyAdapter
  {
    private String usernm = "user";
    public LineListener() 
    { 
      super();
      usernm = ExpCoordinator.theCoordinator.getProperty(ExpCoordinator.USERNAME);
      if (usernm == null) 
        {
          ExpCoordinator.theCoordinator.getUserInfo();
          usernm = ExpCoordinator.theCoordinator.getProperty(ExpCoordinator.USERNAME);
        }
    }
    public void keyPressed(KeyEvent e)
    {
      if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
          String msg = chatSend.getText();
          chatSend.selectAll();
          chatSend.replaceSelection("");
          chatSend.setCaretPosition(0);
          chatReceive.append(new String(usernm + ":" + msg + "\n"));
          //send message
          odaemon.sendMessage(new ObserveDaemon.ChatMessage(usernm, msg));
        }
    }
  }
 public static class QuitMessage extends NCCP.RequesterBase
  {
    public QuitMessage(DataInputStream din) throws IOException
    {
      this();
      ExpCoordinator.print("ObserveDaemon.QuitMessage", 3);
      retrieveHeader(din);
    }
    public QuitMessage()
    {
      super(NCCP_Operation_QuitMessage, true);
    }
    public void storeData(DataOutputStream dout) throws IOException 
    {
      ExpCoordinator.print("Observer.QuitMessage.storeData", 3);
    }
  }

  public static class StateRequest extends NCCP.RequesterBase
  {
    private short sid = 0;
    public StateRequest(DataInputStream din) throws IOException
    {
      super(NCCP_Operation_StateRequest, true);
      ExpCoordinator.print("ObserveDaemon.StateRequest", TEST_OBS);
      retrieveHeader(din);
      sid = din.readShort();
    }
    public StateRequest(short s)
    {
      super(NCCP_Operation_StateRequest, true);
      sid = s;
    }
    public void storeData(DataOutputStream dout) throws IOException 
    {
      ExpCoordinator.print("Observer.StateRequest.storeData", TEST_OBS);
      dout.writeShort(sid);
    }
    public int getSID() { return sid;}
  }

  public static class StateResponse extends NCCP.ResponseBase  implements NCCP.Message //also need to send this
  {
    public static final int ACCEPT = 0;
    public static final int ADD_CLUSTER = Experiment.Event.ADD|Experiment.Event.CLUSTER;
    public static final int ADD_NODE = Experiment.Event.ADD|Experiment.Event.NODE;
    public static final int ADD_LINK = Experiment.Event.ADD|Experiment.Event.LINK;
    public static final int ADD_PARAM = Experiment.Event.ADD|Experiment.Event.PARAM;
    public static final int REMOVE_CLUSTER = Experiment.Event.REMOVE|Experiment.Event.CLUSTER;
    public static final int REMOVE_NODE = Experiment.Event.REMOVE|Experiment.Event.NODE;
    public static final int REMOVE_LINK = Experiment.Event.REMOVE|Experiment.Event.LINK;
    public static final int REMOVE_PARAM = Experiment.Event.REMOVE|Experiment.Event.PARAM;
    private short sid = 0;
    private boolean accept = false;
    private int subop = ACCEPT;
    private NCCP.SendHeader sendHeader = null;
    private ONLComponent onlComponent = null;
    private Cluster.Instance cluster = null;
    private Experiment experiment = null;
    private ExpCoordinator expCoordinator = null;
    
    public StateResponse()
    {
      super(3, "Observe State Response");
      op = (NCCP_Operation_StateResponse  | ExpCoordinator.VERSION_BYTES);
      expCoordinator = ExpCoordinator.theCoordinator;
    }
    public StateResponse(int s, boolean a) { this((short)s, a);}
    public StateResponse(short s, boolean a)
    {
      this();
      sid = s;
      accept = a;
    }
    public StateResponse(int so, ONLComponent c)
    {
      this();
      subop = so;
      onlComponent = c;
    }
    public StateResponse(int so, Cluster.Instance c)
    {
      this();
      subop = so;
      cluster = c;
    }
    public void retrieveFieldData(DataInput din) throws IOException
    {
      ONL.NCCPReader rdr = new ONL.NCCPReader(din);
      retrieveFieldData(rdr);
    }
    public void retrieveFieldData(ONL.NCCPReader rdr) throws IOException
    {
      subop = rdr.readInt();
      String str;
      if (experiment == null) experiment = ExpCoordinator.theCoordinator.getCurrentExp();
      switch (subop)
        {
        case ACCEPT:
          sid = (short)rdr.readShort();
          ExpCoordinator.print(new String("Observer.StateResponse.retrieveFieldData  ACCEPT sid = " + sid), TEST_OBS);
          accept = rdr.readBoolean();
          ExpCoordinator.print(new String("Observer.StateResponse.retrieveFieldData accept = " + accept), TEST_OBS);
          break;
        case ADD_NODE:
          ExpCoordinator.print("Observer.StateResponse.retrieveFieldData ADD_NODE", TEST_OBS);
          onlComponent = experiment.readComponent(rdr.readString(), rdr);
          experiment.readComponentLocation(rdr.readString(), rdr);
          onlComponent.readWatchedProps(rdr);
          break;
        case ADD_CLUSTER:
          ExpCoordinator.print("Observer.StateResponse.retrieveFieldData ADD_CLUSTER", TEST_OBS);
	  cluster = new Cluster.Instance(rdr, experiment);
          //onlComponent = experiment.readComponent(rdr.readString(), rdr);
          //experiment.readComponentLocation(rdr.readString(), rdr);
          //onlComponent.readWatchedProps(rdr);
          break;
        case ADD_LINK:
          ExpCoordinator.print("Observer.StateResponse.retrieveFieldData ADD_LINK", TEST_OBS);
          onlComponent = experiment.readLink(rdr.readString(), rdr);
          onlComponent.readWatchedProps(rdr);
          break;
        case ADD_PARAM:
          ExpCoordinator.print("Observer.StateResponse.retrieveFieldData ADD_PARAM", TEST_OBS);
          onlComponent = experiment.readParam(rdr.readString(), rdr);
          onlComponent.readWatchedProps(rdr);
          break;
        case REMOVE_NODE:
          str = rdr.readString();
          onlComponent = expCoordinator.getComponentFromString(str);
          ExpCoordinator.print(new String("Observer.StateResponse.retrieveFieldData REMOVE_NODE " + str) , TEST_OBS);
          if (onlComponent != null)experiment.removeNode(onlComponent);
          break;
        case REMOVE_CLUSTER:
          ExpCoordinator.print("Observer.StateResponse.retrieveFieldData REMOVE_CLUSTER", TEST_OBS);
	  experiment.removeCluster(rdr.readInt());
          //onlComponent = experiment.readComponent(rdr.readString(), rdr);
          //experiment.readComponentLocation(rdr.readString(), rdr);
          //onlComponent.readWatchedProps(rdr);
          break;
        case REMOVE_LINK:
          str = rdr.readString();
          onlComponent = expCoordinator.getComponentFromString(str);
          ExpCoordinator.print(new String("Observer.StateResponse.retrieveFieldData REMOVE_LINK " + str) , TEST_OBS);
          if (onlComponent != null) experiment.removeLink(onlComponent);
          break;
        case REMOVE_PARAM:
          str = rdr.readString();
          onlComponent = expCoordinator.getComponentFromString(str);
          ExpCoordinator.print(new String("Observer.StateResponse.retrieveFieldData REMOVE_PARAM " + str) , TEST_OBS);
          if (onlComponent != null) experiment.removeParam(onlComponent);
          break;
        }
    }
    public double getData(MonitorDataType.Base mdt) { return sid;}
    
    public void store(DataOutputStream dout) throws IOException //interface Message
    {
      storeHeader(dout);
      ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
      wrtr.writeInt(subop);
      switch (subop)
        {
        case ACCEPT:
          wrtr.writeShort(sid);
          ExpCoordinator.print(new String("Observer.StateResponse.store sid = " + sid), TEST_OBS);
          wrtr.writeBoolean(accept);
          ExpCoordinator.print(new String("Observer.StateResponse.store accept = " + accept), TEST_OBS);
          break;
        case ADD_CLUSTER:
          ExpCoordinator.print("Observer.StateResponse.store ADD_CLUSTER", TEST_OBS);
          cluster.writeExpDescription(wrtr);
          break;
        case ADD_NODE:
	    ExpCoordinator.print(new String("Observer.StateResponse.store ADD_NODE " + onlComponent.getLabel()), TEST_OBS);
          onlComponent.writeExpDescription(wrtr);
          writeDisplay(wrtr);
          onlComponent.writeWatchedProps(wrtr);
          break;
        case ADD_LINK:
	  ExpCoordinator.print(new String("Observer.StateResponse.store ADD_LINK " + onlComponent.getLabel()), TEST_OBS);
        case ADD_PARAM:
	  if (subop == ADD_PARAM) ExpCoordinator.print(new String("Observer.StateResponse.store ADD_PARAM " + onlComponent.getLabel()), TEST_OBS);
          onlComponent.writeExpDescription(wrtr);
          onlComponent.writeWatchedProps(wrtr);
          break;
        case REMOVE_NODE:
        case REMOVE_LINK:
        case REMOVE_PARAM:
          wrtr.writeString(onlComponent.toString());
          break;
        case REMOVE_CLUSTER:
          wrtr.writeInt(cluster.getIndex());
          break;
        }
    }

    public void writeDisplay(ONL.Writer wrtr) throws IOException
    {
      ONLGraphic elem_graph = onlComponent.getGraphic();
      if (elem_graph != null)
        {
          wrtr.writeString(onlComponent.toString());
          if (onlComponent instanceof Hardware && !(onlComponent instanceof HardwareHost))
            {
              int spinner = ((HardwareGraphic)elem_graph).getSpinnerPosition();
              wrtr.writeString(new String(elem_graph.getX() + " " +  elem_graph.getY() + " " + spinner));
            }
          else
            wrtr.writeString(new String(elem_graph.getX() + " " +  elem_graph.getY()));
        }
    }
    public short getSID() { return sid;}
    public boolean getAccept() { return accept;}
    public int getSubop() { return subop;}
    public ONLComponent getONLComponent() { return onlComponent;}
    public Cluster.Instance getCluster() { return cluster;}
  }
  public static class EventResponse extends NCCP.ResponseBase  implements NCCP.Message //also need to send this
  {
    //private NCCP.SendHeader sendHeader = null;
    protected ONLComponent.Event event = null;
    private Experiment experiment = null;
    private ExpCoordinator expCoordinator = null;
    
    public EventResponse(int o)
    {
      super(3, "Observe Event Response");
      op = (o | ExpCoordinator.VERSION_BYTES);
      //op = (NCCP_Operation_EventResponse  | ExpCoordinator.VERSION_BYTES);
      expCoordinator = ExpCoordinator.theCoordinator;
    }
    public EventResponse() { this(NCCP_Operation_EventResponse);}
    public EventResponse(ONLComponent.Event e){ this(NCCP_Operation_EventResponse, e);}
    public EventResponse(int o, ONLComponent.Event e)
    {
      this(o);
      event = e;
    }
    public void retrieveFieldData(DataInput din) throws IOException
    {
      ONL.NCCPReader rdr = new ONL.NCCPReader(din);
      retrieveFieldData(rdr);
    }
    public void retrieveFieldData(ONL.NCCPReader rdr) throws IOException
    {
      String c_str = rdr.readString();
      ONLComponent c = ExpCoordinator.theCoordinator.getComponentFromString(c_str);
      if (c != null) 
        {
          //event = new ONLComponent.Event(c,rdr);
          c.readEvent(rdr);
        }
      else
        ExpCoordinator.print(new String("Observer.EventResponse.retrieveFieldData couldn't read component " + c_str), 6);
    }
    public double getData(MonitorDataType.Base mdt) { return 0;}
    
    public void store(DataOutputStream dout) throws IOException //interface Message
    {
      storeHeader(dout);
      ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
      if (event != null) event.write(wrtr);
    }
    public ONLComponent.Event getEvent() { return event;}
  }

  public static class MonitorEventResponse extends NCCP.ResponseBase  implements NCCP.Message //also need to send this
  {
    //private NCCP.SendHeader sendHeader = null;
    protected MonitorManager.Event event = null;
    private ExpCoordinator expCoordinator = null;
    
    public MonitorEventResponse(int o)
    {
      super(3, "Observe.MonitorEventResponse");
      op = (o | ExpCoordinator.VERSION_BYTES);
      expCoordinator = ExpCoordinator.theCoordinator;
    }
    public MonitorEventResponse() { this(NCCP_Operation_MonitorEventResponse);}
    public MonitorEventResponse(MonitorManager.Event e){ this(NCCP_Operation_MonitorEventResponse, e);}
    public MonitorEventResponse(int o, MonitorManager.Event e)
    {
      this(o);
      event = e;
    }
    public void retrieveFieldData(DataInput din) throws IOException
    {
      ONL.NCCPReader rdr = new ONL.NCCPReader(din);
      retrieveFieldData(rdr);
    }
    public void retrieveFieldData(ONL.NCCPReader rdr) throws IOException
    {
      event = MonitorManager.Event.read(rdr);
      MonitorManager.Display display = event.getDisplay();
      if (display == null)
        {
          ExpCoordinator.print(new String("Observer.MonitorEventResponse.retrieveFieldData type:" + event.getType() + " display null"), 3);
          return;
        }
      ExpCoordinator.print(new String("Observer.MonitorEventResponse.retrieveFieldData type:" + event.getType()), 5);
      switch(event.getType())
        {
        case MonitorManager.Event.REMOVE_DISPLAY:
          display.setVisible(false); //should not trigger stopMonitoring
          display.stopMonitoring();
          break;
        case MonitorManager.Event.REMOVE_PARAM:
          display.removeData(event.getParam().getMonitor());
          break;
        }
    }
    public double getData(MonitorDataType.Base mdt) { return 0;}
    
    public void store(DataOutputStream dout) throws IOException //interface Message
    {
      storeHeader(dout);
      ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
      event.write(wrtr);
    }
    public MonitorManager.Event getEvent() { return event;}
  }

  public static class ErrorResponse extends NCCP.ResponseBase  implements NCCP.Message //also need to send this
  {
    //private NCCP.SendHeader sendHeader = null;
    private ExpCoordinator expCoordinator = null;
    private StatusDisplay.Error error = null;
    
    public ErrorResponse() { this(null);}
    public ErrorResponse(StatusDisplay.Error e)
    {
      super(0, "Observe.ErrorResponse");
      op = (NCCP_Operation_ErrorResponse | ExpCoordinator.VERSION_BYTES);
      expCoordinator = ExpCoordinator.theCoordinator;
      error = e;
    }
    public void retrieveFieldData(DataInput din) throws IOException
    {
      ONL.NCCPReader rdr = new ONL.NCCPReader(din);
      retrieveFieldData(rdr);
    }
    public void retrieveFieldData(ONL.NCCPReader rdr) throws IOException
    {
      error = new StatusDisplay.Error(rdr);
      expCoordinator.addError(error);
      ExpCoordinator.print(new String("Observer.ErrorResponse.retrieveFieldData msg:" + error.getLogMsg()), 5);
    }
    public double getData(MonitorDataType.Base mdt) { return 0;}
    
    public void store(DataOutputStream dout) throws IOException //interface Message
    {
      storeHeader(dout);
      ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
      if (error != null) 
        {
          error.write(wrtr);
          ExpCoordinator.print(new String("Observer.ErrorResponse.store msg:" + error.getLogMsg()), 5);
        }
    }
    public StatusDisplay.Error getError() { return error;}
  }
  public static class CommitResponse extends NCCP.ResponseBase implements NCCP.Message //also need to send this
  {
    //private NCCP.SendHeader sendHeader = null;
    private ExpCoordinator expCoordinator = null;
    private boolean committed = false;
    
    public CommitResponse() { this(false);}
    public CommitResponse(boolean b)
    {
      super(0, "Observe.CommitResponse");
      op = (NCCP_Operation_CommitResponse | ExpCoordinator.VERSION_BYTES);
      expCoordinator = ExpCoordinator.theCoordinator;
      committed = b;
    }
    public void retrieveFieldData(DataInput din) throws IOException
    {
      ONL.NCCPReader rdr = new ONL.NCCPReader(din);
      retrieveFieldData(rdr);
    }
    public void retrieveFieldData(ONL.NCCPReader rdr) throws IOException
    {
      committed = rdr.readBoolean();
      expCoordinator.setCommit(committed);
      ExpCoordinator.print(new String("Observer.CommitResponse.retrieveFieldData " + committed), 5);
    }
    public double getData(MonitorDataType.Base mdt) { return 0;}
    
    public void store(DataOutputStream dout) throws IOException //interface Message
    {
      storeHeader(dout);
      ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
      wrtr.writeBoolean(committed);
      ExpCoordinator.print(new String("Observer.CommitResponse.store " + committed), 5);
    }
    public boolean getCommitted() { return committed;}
  }

  public static class ObserveData 
  {
    public String expID = "";
    public String oname = "";
    public String uname = "";
    public String password = "";
    public boolean accept = false;
    public int proxySID = 0;

    public ObserveData(){}
    public ObserveData(String eid, String onm, String unm, String pwd, boolean a, int psid)
    {
      expID = eid;
      oname = onm;
      uname = unm;
      password = pwd;
      accept = a;
      proxySID = psid;
    }
    public ObserveData(ONL.Reader rdr) throws IOException { read(rdr);}
    public void read(ONL.Reader rdr) throws IOException
    {
      expID = rdr.readString();
      oname = rdr.readString();
      uname = rdr.readString();
      //a password is never returned just sent
      accept = rdr.readBoolean();
      proxySID = (int)rdr.readInt();
      ExpCoordinator.print(new String("Observer.ObserveData.read " + toString()), 5);
    }
    public void write(ONL.Writer wrtr) throws IOException
    {
      wrtr.writeString(expID);
      wrtr.writeString(oname);
      wrtr.writeString(uname);
      wrtr.writeString(password);
      wrtr.writeBoolean(accept);
      wrtr.writeInt(proxySID);
    }
    public String toString()
    {
      return (new String("(expID, oname, uname, accept, proxySID) = (" + expID + ", " + oname + ", " + uname + ", " + accept + ", " + proxySID + ")"));
    }
  }//end class ObserveData

  public static class ExpInfo
  {
    public String id = "";
    public String uname = "";
    
    public ExpInfo(ONL.Reader rdr) throws IOException
    {
      id = rdr.readString();
      uname = rdr.readString();
    }
  }//end class ExpInfo
  
  public static class ObserveRequest extends ExpRequest //ObserveRequest from observer and response to request from student
  {
    private ObserveData observeData = null;

    private class NCCP_Request extends ExpRequest.RequesterBase
    {
      public NCCP_Request(int op)
      {
        super(op);
        setMarker(new REND_Marker_class(op, 0));
      }
      public void storeData(DataOutputStream dout) throws IOException  
      { 
        ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
        observeData.write(wrtr);
      }
    }//end class ObserveRequest.NCCP_Request

    public static class NCCP_Response extends NCCP.ResponseBase
    {
      private double errorCode = 0;
      private String errorMsg = "";
      private ObserveData odata = null;
      
      public NCCP_Response()
      {
        super(0);
        odata = new ObserveData();
      }
      
      public void retrieveFieldData(DataInput din) throws IOException
      {
        ONL.NCCPReader rdr = new ONL.NCCPReader(din);
        errorMsg = rdr.readString();
        errorCode = rdr.readInt();
        odata.read(rdr);
      }
      
      public ObserveData getObserveData() { return odata;}
      public double getErrorCode() { return errorCode;}
      public String getErrorMsg() { return errorMsg;}
      public double getData(MonitorDataType.Base mdt) { return 0;}
    }//end class ObserveRequest.NCCP_Response
    
    public ObserveRequest(ExpInfo einfo) { this(ExpDaemon.NCCP_Operation_ObserveExpReq, einfo.id, einfo.uname, false);}
    public ObserveRequest(String eid, String nm, boolean a) { this(ExpDaemon.NCCP_Operation_ObserveExpResp, eid, nm, a);}
    public ObserveRequest(int op, String eid, String nm, boolean a)
    {
      super();
      setMarker(new REND_Marker_class(op, 0));
      observeData = new ObserveData();
      ExpCoordinator ec = ExpCoordinator.theCoordinator;
      ExpCoordinator.theCoordinator.getUserInfo();
      
      if (op == ExpDaemon.NCCP_Operation_ObserveExpReq)
        {
          observeData.oname = ec.getProperty(ExpCoordinator.USERNAME);
          observeData.uname = nm;
        }
      else
        {
          observeData.uname = ec.getProperty(ExpCoordinator.USERNAME);
          observeData.proxySID = ec.getProxy().getSID();
          observeData.accept = a;
          observeData.oname = nm;
        }
      observeData.password = ec.getProperty(ExpCoordinator.PASSWORD);
      observeData.expID = new String(eid);

      ExpCoordinator.print(new String("Observer.ObserveRequest op = " + op + " observeData  " + observeData.toString()), 6);
      
      request = new NCCP_Request(op);
      response = new NCCP_Response();
    } 
    public void processResponse(NCCP.ResponseBase r)
    {
      ExpCoordinator ec = ExpCoordinator.theCoordinator;
      int op = response.getOp();
      int tp = JOptionPane.PLAIN_MESSAGE;
      ObserveData odata = ((NCCP_Response)r).getObserveData();
      //check for errors 
      String msg = null;
      String ttl = null;
      switch ((int)r.getStatus())
        {
        case NCCP.Status_Fine: 
          if (op == ExpDaemon.NCCP_Operation_ObserveExpReq) //response to observer asking to join
            {
              if (odata.accept)
                {
                  //inform user the invite is accepted and 
                  msg = new String("Observe request was accepted by user " + odata.uname);
                  short sid = (short)odata.proxySID;
                  ObserveDaemon od = theObserver.startDaemon(sid);
                  od.connect();
                  //add connection listener to daemon, queue request for state
                  od.sendMessage(new StateRequest(sid));
                  ExpCoordinator.print(new String("Observer sending state request to " + sid), 2);
                  ExpCoordinator.setObserve(true);
                }
              else //inform user invite was rejected
                msg = new String("Observe request was rejected by user " + odata.uname);
	      ttl = "RSVP - Experiment Observation";
            }
          ec.removeRequest(this);
          break;
        case NCCP.Status_StillRemaining: 
          if (op == ExpDaemon.NCCP_Operation_ObserveExpReq) 
            {
              msg = new String(((NCCP_Response)r).getErrorMsg());
              ttl = "Processing Observation Request";
            }
          break;
        default:
          msg = new String(((NCCP_Response)r).getErrorMsg());
          ttl = "Error Experiment Observation";
          tp = JOptionPane.ERROR_MESSAGE;
          if (((NCCP_Response)r).getErrorCode() == Reservation.AUTH_ERR)
            ec.clearPassword();
          ec.removeRequest(this);
        }
      if (msg != null)
        {
          JOptionPane.showMessageDialog(ec.getMainWindow(), 
                                        msg,
                                        ttl,
                                        tp);
        }
    }
    public ObserveData getObserveData() { return observeData;}
  }


  public static class NCCP_ErrorResponse extends NCCP.ResponseBase
  {
    private double errorCode = 0;
    private String errorMsg = "";
    
    public NCCP_ErrorResponse()
    {
      super(0);
    }
    
    public void retrieveFieldData(DataInput din) throws IOException
    {
      retrieveFieldData(new ONL.NCCPReader(din));
    }
    public void retrieveFieldData(ONL.Reader rdr) throws IOException
    {
      //super.retrieveFieldData(rdr);
      ExpCoordinator.print("Observer.NCCP_ErrorResponse.retrieveFieldData", 5);
      errorMsg = rdr.readString();
      errorCode = rdr.readInt();
    }

    public double getErrorCode() { return errorCode;}
    public double getData(MonitorDataType.Base mdt) { return 0;}
    public String getErrorMsg() { return errorMsg;} 
  }//end class GetObservable.Request.NCCP_Response


  public static class GetObservable extends ONL.UserAction
  {
    public static class Request extends ExpRequest
    {
      private class NCCP_Request extends ExpRequest.RequesterBase
      {
        public NCCP_Request()
        {
          super(ExpDaemon.NCCP_Operation_ObservableReq);
          setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_ObservableReq, 0));
          ExpCoordinator.theCoordinator.getUserInfo();
        }
      
        public void storeData(DataOutputStream dout) throws IOException  
        { 
          ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
          ExpCoordinator ec = ExpCoordinator.theCoordinator;
          wrtr.writeString(ec.getProperty(ExpCoordinator.USERNAME));
          wrtr.writeString(ec.getProperty(ExpCoordinator.PASSWORD));
        }
      }//end class GetObservable.Request.NCCP_Request


      public static class ExpAction extends ONL.UserAction
      {
        private ExpInfo expInfo = null;
        public ExpAction(ExpInfo ei)
        {
          super(new String("experiment" + ei.id + ": user(" + ei.uname + ")"));
          expInfo = ei;
        }
        public void actionPerformed(ActionEvent e)
        {
          //send request to observe
            int rtn = JOptionPane.showConfirmDialog(theObserver.frame, 
                                                    new String("Do you really want to observe " + expInfo.uname + "'s experiment" + expInfo.id + "\n This will close any active experiments"),
                                                    new String("Observation Request"),
                                                    JOptionPane.YES_NO_OPTION,
                                                    JOptionPane.QUESTION_MESSAGE);
            if (rtn == JOptionPane.YES_OPTION)
              {
                ExpCoordinator.theCoordinator.closeCurrentExp();
                ExpCoordinator.theCoordinator.sendRequest(new ObserveRequest(expInfo));
              }
        }
      }

      private class NCCP_Response extends NCCP_ErrorResponse
      {
        private Vector expList = null;
        private double errorCode = 0;

        public NCCP_Response()
        {
          super();
          expList = new Vector();
        }

        public void retrieveFieldData(ONL.Reader rdr) throws IOException
        {
          super.retrieveFieldData(rdr);
          int n_exp = (int)rdr.readInt();
          ExpCoordinator.print(new String("Observer.GetObservable.Request.NCCP_Response.retrieveFieldData error: " + getErrorMsg() + " " + errorCode + " numExp:" + n_exp), 5);
          for (int i = 0; i < n_exp; ++i)
            {
              expList.add(new ExpInfo(rdr));
            }
        }

        public Vector getExpList() { return expList;}
      }//end class GetObservable.Request.NCCP_Response

      public Request()
      {
        super();
        setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_ObservableReq, 0));
        request = new NCCP_Request();
        response = new NCCP_Response();
      }

      public void processResponse(NCCP.ResponseBase r)
      {
        ExpCoordinator ec = ExpCoordinator.theCoordinator;
        //check for errors 
	if (r.getStatus() == NCCP.Status_Fine) 
	  {
            Vector exp_list = ((NCCP_Response)r).getExpList();
            if (exp_list.size() > 0)
              {
                //if successful start observer window and add submenu to "observe experiment" with available experiment options
                if (theObserver == null) theObserver = new Observer();
                theObserver.addObservableExps(exp_list);
                theObserver.showWindow();
                Experiment curr_exp = ExpCoordinator.theCoordinator.getCurrentExp();
                if (curr_exp != null) curr_exp.setObserver(theObserver);
              }
            else
              {
                //if no available experiments pop up window with "no experiments available for observation by <uname>"
                JOptionPane.showMessageDialog(ec.getMainWindow(), 
                                              "No Experiments returned",
                                              "Error Getting Available Experiments for Observation",
                                              JOptionPane.ERROR_MESSAGE);
              }
          }
        else
          {
            String msg = ((NCCP_Response)r).getErrorMsg();
            if (((NCCP_Response)r).getErrorCode() == Reservation.AUTH_ERR)
              ec.clearPassword();
            JOptionPane.showMessageDialog(ec.getMainWindow(), 
                                          msg,
                                          "Error Getting Available Experiments for Observation",
                                          JOptionPane.ERROR_MESSAGE);
          }
        ec.removeRequest(this);
      }
      
    }//end class GetObservable.Request

    public GetObservable() { super("Get Observable Experiments", false, false);}
    public void actionPerformed(ActionEvent e)
    {
      ExpCoordinator ec = ExpCoordinator.theCoordinator;
      if (!ec.isConnected()) 
        {
          ec.addConnectionListener( new NCCPConnection.ConnectionListener() {
              public void connectionFailed(NCCPConnection.ConnectionEvent e)
              {
                ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error("Connection to Experiment Manager Failed. Can't get observable experiments.", 
                                                                               "Connection to Experiment Manager Failed",
                                                                               "Can't get observable experiments.", 
                                                                               (StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR)));
              }
              public void connectionClosed(NCCPConnection.ConnectionEvent e) {}
              public void connectionOpened(NCCPConnection.ConnectionEvent e)
              {
                ExpCoordinator ec = ExpCoordinator.theCoordinator;
                ec.removeConnectionListener(this);
                ec.sendRequest(new Request());
              }
            });
          ec.connect();
        }
      else 
        ec.sendRequest(new Request());
    }
  }//end class GetObservable


  public static class AddObserver extends ONL.UserAction
  {
    public static class Request extends ExpRequest
    {
      private class NCCP_Request extends ExpDaemon.NCCP_Requester
      {
        private String obsUname = "";
        public NCCP_Request(Experiment exp, String unm)
        {
          super(ExpDaemon.NCCP_Operation_AddObserver, exp);
          setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_AddObserver, 0));
          obsUname = new String(unm);
        }
        public void storeData(DataOutputStream dout) throws IOException 
        {
          super.storeData(dout);
          NCCP.writeString(obsUname, dout);
        }
      }
      public Request(Experiment exp, String unm)
      {
        super();
        setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_AddObserver, 0));
        request = new NCCP_Request(exp,unm);
        response = new ExpDaemon.NCCP_Response();
      }
      public void processResponse(NCCP.ResponseBase r) //not sure what to do here guess should check for authentication error
      {
        ExpCoordinator ec = ExpCoordinator.theCoordinator;
        if (r.getStatus() == NCCP.Status_Failed)
          {
            //String msg = ((ExpDaemon.NCCP_Response)r).getErrorMsg();
            //if (((ExpDaemon.NCCP_Response)r).getErrorCode() == Reservation.AUTH_ERR)
            //  ec.clearPassword();
            String ttl = new String("Error Adding Observer (" + ((NCCP_Request)request).obsUname + ") to experiment");
            JOptionPane.showMessageDialog(ec.getMainWindow(), 
                                          ttl,
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
          }
        ec.removeRequest(this);
      }
    }//end class AddObserver.Request

    public AddObserver()
    {
      super("Add Observer to experiment", false, false);
    }
    public void actionPerformed(ActionEvent e)
    {
        final String opt0 = "Enter";
        final String opt1 = "Cancel";
        Object[] options = {opt0,opt1};
        TextFieldwLabel tf = new TextFieldwLabel(30, "user name:");
        Object[] objs = {tf};
        ExpCoordinator ec = ExpCoordinator.theCoordinator;
        int rtn = JOptionPane.showOptionDialog(ec.getMainWindow(), 
                                               objs, 
                                               "Allow Observe By", 
                                               JOptionPane.YES_NO_OPTION,
                                               JOptionPane.QUESTION_MESSAGE, 
                                               null,
                                               options,
                                               options[0]);
        
        if (rtn == JOptionPane.YES_OPTION)
          {
            String uname = tf.getText();
            if (uname.length() > 0)
              {
                //for the current experiment, send addObserver request to ONLCD-ExpDaemon
                ec.sendRequest(new AddObserver.Request(ec.getCurrentExp(), uname));
              }
          }
    }
  }

  public static class StartObserver extends AbstractAction
  {
    public StartObserver()
    {
      super("Start Observation Window");
    }
    public void actionPerformed(ActionEvent e)
    {
      if (theObserver == null) theObserver = new Observer();
      theObserver.showWindow();
      Experiment curr_exp = ExpCoordinator.theCoordinator.getCurrentExp();
      if (curr_exp != null) curr_exp.setObserver(theObserver);
    }
  }

  private Observer()
  {
    frame = new JFrame("Observation Window");
    JMenuBar mbar = new JMenuBar();
    frame.setJMenuBar(mbar);
    JMenu menu = new JMenu("Options");
    observableExps = new JMenu("Exps Available for Observation");
    menu.add(observableExps);
    mbar.add(menu);
    //frame.setVisible(false);
    stateTArea = new JTextArea();
    stateTArea.setLineWrap(true);
    stateTArea.append("\n");
    stateTArea.setEditable(false);
    Container contentPane = frame.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    if (ExpCoordinator.getDebugLevel() > 0)
    {
      contentPane.add((new JLabel("Experiment State")));
      contentPane.add(new JScrollPane(stateTArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }
    //add chat receive window
    chatReceive = new JTextArea();
    chatReceive.setLineWrap(true);
    //chatReceive.append("\n");
    chatReceive.setEditable(false);
    contentPane.add((new JLabel("Chat Received")));
    contentPane.add(new JScrollPane(chatReceive, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    //add chat send window
    chatSend = new JTextArea();
    chatSend.setLineWrap(true);
    //chatSend.append("\n");
    chatSend.setEditable(true);
    chatSend.addKeyListener(new LineListener());
    contentPane.add((new JLabel("Chat Send")));
    contentPane.add(new JScrollPane(chatSend, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

    frame.setSize(425,450);
    stateTArea.setSize(400,120);
    chatReceive.setSize(400,120);
    chatSend.setSize(400,120);
    chatReceive.setMinimumSize(new Dimension(400,100));
    chatSend.setMinimumSize(new Dimension(400,100));
    frame.setVisible(true);
    startDaemon(0);
  }
  public void monitoringChanged(MonitorManager.Event e)//MonitorManager.Listener
  {
    int tp = e.getType();
    boolean observer = ExpCoordinator.isObserver();
    switch(tp)
      {
      case MonitorManager.Event.ADD_DISPLAY:
        stateTArea.append(new String("MONITOR CHANGE: display added " + e.getDisplay().getTitle() + "\n"));
        break;
      case MonitorManager.Event.REMOVE_DISPLAY:
        stateTArea.append(new String("MONITOR CHANGE: display removed " + e.getDisplay().getTitle() + "\n"));
        break;
      case MonitorManager.Event.ADD_PARAM:
        stateTArea.append(new String("MONITOR CHANGE: param added " + e.getParam().getMonitor().getDataType().getName() + " to display " + e.getDisplay().getTitle() + "\n"));
        break;
      case MonitorManager.Event.REMOVE_PARAM:
        stateTArea.append(new String("MONITOR CHANGE: param removed " + e.getParam().getMonitor().getDataType().getName() + " from display " + e.getDisplay().getTitle() + "\n"));
        break;
      case MonitorManager.Event.CHANGE_PARAM:
        stateTArea.append(new String("MONITOR CHANGE: param[" + e.getIndex() + "] changed " + e.getParam().getMonitor().getDataType().getName() + " from display " + e.getDisplay().getTitle() + "\n"));
        break;
      }
    if (stateSent && !observer)
      odaemon.sendMessage(new MonitorEventResponse(e));
  }

  //start StatusDisplay.Listener
  public void errorAdded(StatusDisplay.Error e)
  {
    if (stateSent && !ExpCoordinator.isObserver())
      odaemon.sendMessage(new ErrorResponse(e));
    ExpCoordinator.print(new String("Observer.errorAdded msg " + e.getLogMsg()), 8);
  }
  public void setCommit(boolean b)
  {
    if (stateSent && !ExpCoordinator.isObserver())
      odaemon.sendMessage(new CommitResponse(b));
    ExpCoordinator.print(new String("Observer.setCommit " + b), 8);
  }
  //end StatusDisplay.Listener
  public void experimentChanged(Experiment.Event e) //Experiment.Listener
  {
    int tp = e.getType();
    boolean observer = ExpCoordinator.isObserver();
    ExpCoordinator.print(new String("Observer.experimentChanged observer = " + observer), 8);
    switch(tp)
      {
      case (Experiment.Event.ADD|Experiment.Event.CLUSTER):
        stateTArea.append(new String("EXPERIMENT CHANGE: cluster added " + e.getCluster().getType() + ":" + e.getCluster().getIndex() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.ADD_CLUSTER, e.getCluster()));
        break;
      case (Experiment.Event.ADD|Experiment.Event.NODE):
        stateTArea.append(new String("EXPERIMENT CHANGE: node added " + e.getONLComponent().getLabel() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.ADD_NODE, e.getONLComponent()));
        break;
      case (Experiment.Event.ADD|Experiment.Event.LINK):
        stateTArea.append(new String("EXPERIMENT CHANGE: link added " + e.getONLComponent().getLabel() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.ADD_LINK, e.getONLComponent()));
        break;
      case (Experiment.Event.ADD|Experiment.Event.PARAM):
        stateTArea.append(new String("EXPERIMENT CHANGE: param added " + e.getONLComponent().getLabel() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.ADD_PARAM, e.getONLComponent()));
        break;
      case (Experiment.Event.REMOVE|Experiment.Event.CLUSTER):
        stateTArea.append(new String("EXPERIMENT CHANGE: cluster removed " + e.getCluster().getType() + ":" + e.getCluster().getIndex() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.REMOVE_CLUSTER, e.getCluster()));
        break;
      case (Experiment.Event.REMOVE|Experiment.Event.NODE):
        stateTArea.append(new String("EXPERIMENT CHANGE: node removed " + e.getONLComponent().getLabel() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.REMOVE_NODE, e.getONLComponent()));
        break;
      case (Experiment.Event.REMOVE|Experiment.Event.LINK):
        stateTArea.append(new String("EXPERIMENT CHANGE: link removed " + e.getONLComponent().getLabel() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.REMOVE_LINK, e.getONLComponent()));
        break;
      case (Experiment.Event.REMOVE|Experiment.Event.PARAM):
        stateTArea.append(new String("EXPERIMENT CHANGE: param removed " + e.getONLComponent().getLabel() + "\n"));
        if (stateSent && !observer)
          odaemon.sendMessage(new StateResponse(StateResponse.REMOVE_PARAM, e.getONLComponent()));
        break;
      }
  }
  
  //ONLComponent.Listener
  public void onlcStateChanged(ONLComponent.PropertyEvent e)
  {
    String n_val = e.getNewValue();
    String msg = new String("\nCOMPONENT STATE CHANGE:" + e.toString() + ": component " + e.getONLComponent().getLabel() + " changed from " + e.getOldValue() + " to " + n_val + "\n");
    ExpCoordinator.print(new String("Observer.onlcStateChanged " + msg), 5);
    stateTArea.append(msg);
    //if (n_val != null && stateSent && !ExpCoordinator.isObserver() && (n_val.equals(ONLComponent.ACTIVE) || n_val.equals(ONLComponent.COMMITTED)))
    if (stateSent && !ExpCoordinator.isObserver())
      {
        odaemon.sendMessage(new EventResponse(e)); 
        //encode any connected daemons
      }
  }
  public void onlcValueChanged(ONLComponent.Event e)
  {
    String msg = new String("COMPONENT VALUE CHANGE:" + e.toString() + ": component " + e.getONLComponent().getLabel() + "\n");
    stateTArea.append(msg);
    ExpCoordinator.print(new String("Observer.onlcStateChanged " + msg), 5);
    if (stateSent && !ExpCoordinator.isObserver())
      {
        odaemon.sendMessage(new EventResponse(e)); 
        //encode any connected daemons
      }
  }
  public void onlcTableChanged(ONLComponent.TableEvent e) 
  { 
    int tp = e.getType();
    ExpCoordinator.print("Observer:onlcTableChanged", 2);
    switch(tp)
      {
      case ONLComponent.Event.VALUE_CHANGE:
        stateTArea.append(new String("COMPONENT TABLE VALUE CHANGE:" + e.toString() + ": table " + e.getONLComponent().getLabel() + " entry: " + e.getTableEntry().toString() + " index:" + e.getIndex() + "\n"));
        break;
      case ONLComponent.Event.STATE_CHANGE:
        stateTArea.append(new String("COMPONENT TABLE STATE CHANGE:" + e.toString() + ": table " + e.getONLComponent().getLabel() + " entry: " + e.getTableEntry().toString() + " index:" + e.getIndex() + "\n"));
        break;
      case ONLComponent.Event.TABLE_ADD:
        stateTArea.append(new String("COMPONENT TABLE ADD:" + e.toString() + ": table " + e.getONLComponent().getLabel() + " entry: " + e.getTableEntry().toString() + " index:" + e.getIndex() + "\n"));
        break;
      case ONLComponent.Event.TABLE_DELETE:
        stateTArea.append(new String("COMPONENT TABLE DELETE:" + e.toString() + ": table " + e.getONLComponent().getLabel() + " entry: " + e.getTableEntry().toString() + " index:" + e.getIndex() + "\n"));
        break;
      case ONLComponent.Event.TABLE_COMMIT:
        stateTArea.append(new String("COMPONENT TABLE COMMIT:" + e.toString() + ": table " + e.getONLComponent().getLabel() + " entry: " + e.getTableEntry().toString() + " index:" + e.getIndex() + "\n"));
        break;
      }
    if (stateSent && !ExpCoordinator.isObserver())
      odaemon.sendMessage(new EventResponse(e));
  }
  public void onlcConnectionChanged(ONLComponent.ConnectionEvent e)
  {
    stateTArea.append(new String("CONNECTION Opened:" + e.getONLComponent().getLabel() + " daemon: " + e.getDaemonType() + " cid:" + e.getCID() + "\n"));
    if (stateSent && !ExpCoordinator.isObserver())
      odaemon.sendMessage(new EventResponse(e));
  }
  public void onlcGraphicMoved(ONLComponent.MoveEvent e)
  {
    stateTArea.append(new String("GRAPHICS MOVED:" + e.getONLComponent().getLabel() + "\n"));
    if (stateSent && !ExpCoordinator.isObserver())
      odaemon.sendMessage(new EventResponse(e));
  }
  //end ONLComponent.Listener


  private ObserveDaemon startDaemon(int sid) { return (startDaemon((short)sid));}
  private ObserveDaemon startDaemon(short sid)
  {
    if (odaemon == null)
      odaemon = new ObserveDaemon(sid, this);
    else
      {
        if (odaemon.getSID() <= 0) odaemon.setSID(sid);
      }
    return odaemon;
  }

  protected void clear() 
  {
    if (odaemon != null) 
      odaemon.close();
    if (theObserver == this) theObserver = null;
  }

  public void addChatMessage(String s) { addChatMessage("observer", s);}

  public void addChatMessage(String nm, String s)
  {
    chatReceive.append(new String(nm + ":" + s + "\n"));
  }
  public void processPeerMessage(NCCP.ResponseHeader responseHdr, ONL.NCCPReader rdr) throws IOException
  {
    EventResponse theResponse = null;
    //NCCP.ResponseBase theResponse = null;
    switch(responseHdr.getOp())
      {
      case (NCCP_Operation_StateResponse):
        ExpCoordinator.print("Observer.processPeerMessage stateResponse", TEST_OBS);
        stateResponse = new StateResponse();
        stateResponse.setHeader(responseHdr);
        stateResponse.retrieveFieldData(rdr);
        stateTArea.append(new String("processPeerMessage from " + stateResponse.getSID() + " accept is " + stateResponse.getAccept()));
        break;
      case (NCCP_Operation_MonitorEventResponse):
        ExpCoordinator.print("Observer.processPeerMessage monitorEventResponse", TEST_OBS);
        MonitorEventResponse meResponse = new MonitorEventResponse();
        meResponse.setHeader(responseHdr);
        meResponse.retrieveFieldData(rdr);
        break;
      case ( NCCP_Operation_EventResponse):
        ExpCoordinator.print("Observer.processPeerMessage eventResponse", TEST_OBS);
        theResponse = new EventResponse();
        break;
      case ( NCCP_Operation_ErrorResponse):
        ExpCoordinator.print("Observer.processPeerMessage errorResponse", TEST_OBS);
        ErrorResponse errResponse = new ErrorResponse();
        errResponse.setHeader(responseHdr);
        errResponse.retrieveFieldData(rdr);
        break;
      case ( NCCP_Operation_CommitResponse):
        ExpCoordinator.print("Observer.processPeerMessage commitResponse", TEST_OBS);
        CommitResponse commitResponse = new CommitResponse();
        commitResponse.setHeader(responseHdr);
        commitResponse.retrieveFieldData(rdr);
        break;
      }

    if (theResponse != null)
      {
        theResponse.setHeader(responseHdr);
        theResponse.retrieveFieldData(rdr);
      }
  }
  public void processQuitMessage(QuitMessage qm)
  {
    if (ExpCoordinator.isObserver()) ExpCoordinator.theCoordinator.closeCurrentExp();
    ExpCoordinator.print("Observer.processQuitMessage", 3);
    close();
  }
  public void processStateRequest(StateRequest req)
  {
    ExpCoordinator.print(new String("processStateRequest from " + req.getSID() + " accepting"), TEST_OBS);
    stateResponse = new StateResponse(req.getSID(), true);
    odaemon.sendMessage(stateResponse);
    stateTArea.append(new String("processStateRequest from " + req.getSID() + " accepting"));
    Experiment.Snapshot snap = new Experiment.Snapshot(ExpCoordinator.theCoordinator.getCurrentExp());
    stateSent = true;
    int max = snap.clusters.size();
    int i = 0;
    ONLComponent onlc_elem;
    Cluster.Instance cl_elem;
    Vector daemons;
    for (i = 0; i < max; ++i)
      {
        cl_elem = (Cluster.Instance)snap.clusters.elementAt(i);
        //cl_elem.addONLCListener(this);
        odaemon.sendMessage(new StateResponse(StateResponse.ADD_CLUSTER, cl_elem));
      }
    max = snap.nodes.size();
    for (i = 0; i < max; ++i)
      {
        onlc_elem = snap.nodes.onlComponentAt(i);
        onlc_elem.addONLCListener(this);
        odaemon.sendMessage(new StateResponse(StateResponse.ADD_NODE, onlc_elem));
        daemons = onlc_elem.getDaemons();
        if (daemons != null)
          {
            int num_daemons = daemons.size();
            ExpCoordinator.print(new String("Observer.processStateRequest sending daemons number=" + num_daemons), TEST_OBS);
            for (int j = 0; j < num_daemons; ++j)
              {
                ONLDaemon onl_d = (ONLDaemon)daemons.elementAt(j);
                ExpCoordinator.print(new String("   daemon:" + onl_d.toString()), TEST_OBS);
                if (onl_d.isConnected())
                  {
                    ExpCoordinator.print("     sending: connection info", TEST_OBS);
                  odaemon.sendMessage(new EventResponse(new ONLComponent.ConnectionEvent(onlc_elem, onl_d.getDaemonType(), onl_d.getConnectionID())));
                  }
              }
          }
      }
    max = snap.links.size();
    for (i = 0; i < max; ++i)
      {
        onlc_elem = snap.links.onlComponentAt(i);
        onlc_elem.addONLCListener(this);
        odaemon.sendMessage(new StateResponse(StateResponse.ADD_LINK, onlc_elem));
      }
    max = snap.params.size();
    ExpCoordinator.print(new String("Observer.processStateRequest params:" + max), TEST_OBS);
    for (i = 0; i < max; ++i)
      {
        onlc_elem = snap.params.onlComponentAt(i);
        onlc_elem.addONLCListener(this);
        odaemon.sendMessage(new StateResponse(StateResponse.ADD_PARAM, onlc_elem));
      }
    max = snap.monitorDisplays.size();
    for (i = 0; i < max; ++i)
      {
        MonitorManager.Display d = (MonitorManager.Display)snap.monitorDisplays.elementAt(i);
        odaemon.sendMessage(new MonitorEventResponse(new MonitorManager.Event(d, MonitorManager.Event.ADD_DISPLAY)));
      }
  }

  public void showWindow()
  {
    if (frame == null) return;
    if ((frame.getExtendedState() & Frame.ICONIFIED) != 0)
      frame.setExtendedState(frame.getExtendedState() - Frame.ICONIFIED);
    //frame.show(); //show deprecated 1.5
    frame.setVisible(true);  //replacement 1.5
  }

  private void addObservableExps(Vector elst)
  {
    int max = elst.size();
    observableExps.removeAll();
    for (int i = 0; i < max; ++i)
      {
        observableExps.add(new GetObservable.Request.ExpAction((ExpInfo)elst.elementAt(i)));
      }
  }
  
  public static void start()
  {
    if (theObserver == null) theObserver = new Observer();
    theObserver.showWindow();
    Experiment curr_exp = ExpCoordinator.theCoordinator.getCurrentExp();
    if (curr_exp != null) curr_exp.setObserver(theObserver);
  }

  public void close()
  {
    if (odaemon.isConnected()) 
      {
        ExpCoordinator.print("Observer.close closing observer", TEST_OBS);
        //send exit request to student
        odaemon.sendMessage(new QuitMessage());
        //close proxy connection
        odaemon.close();
      }
    ExpCoordinator.theCoordinator.getCurrentExp().removeObserver();
    if (frame != null) frame.setVisible(false);
    ExpCoordinator.setObserve(false);
  }
}
