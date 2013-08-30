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
 * File: ObserveDaemon.java
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
import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.SocketException;

public class ObserveDaemon extends ONLDaemon //implements ExpRequest.Listener
{
  private Observer observer = null;
  private int reqCount = 0;
  private int ackcount = 0;
  //protected boolean isClearing = false;

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////ChatMessage//////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class ChatMessage implements NCCP.Message
    {
      private String msg = null;
      private String sender = null;
      private short version = 0;
      public ChatMessage(ONL.Reader rdr) throws IOException
      {
        retrieve(rdr);
      }
      public ChatMessage(String nm, String m)
      {
        msg = new String(m);
        version = (short)(ExpCoordinator.VERSION_BYTES >> 16);
        sender = new String(nm);
      }
      public void store(DataOutputStream dout) throws IOException 
      {
	if (dout == null) ExpCoordinator.printer.print("NCCP.RequesterBase.storeHeader dout is null");
	dout.writeShort(NCCP.MessageChat); //message type (2 bytes)
	dout.writeShort(version); //version (2 bytes)
        NCCP.writeString(sender, dout);
        NCCP.writeString(msg, dout);
      }
      public void retrieve(ONL.Reader rdr) throws  IOException
      {
        version = (short)rdr.readShort();
        sender = rdr.readString();
        msg = rdr.readString();
      }
      public short getVersion() { return version;}
      public String getMsg() { return msg;}
      public String getSender() { return sender;}
  }

  ////////////////////////////////////////////////Constructor/////////////////////////////////////////////////////////////////////////////
  public ObserveDaemon(int sid, Observer o) 
    {
      super(NCCPConnection.PEER, sid, ONLDaemon.OBSERVE);
      ExpCoordinator.print(new String("ObserverDaemon Observed sessionID:" + sid), 2);
      ExpCoordinator.print(toString());
      setProxy(ExpCoordinator.theCoordinator.getProxy());
      getProxy().addConnection(this);
      observer = o;
    }
  protected void processOtherMsg(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException
    { 
      ONL.NCCPReader rdr = new ONL.NCCPReader(dIn);
      ExpCoordinator.printer.print(new String("ObserveDaemon.processOtherMsg msg size is " + msgsize), 9);
      if (msgtype == NCCP.MessageChat)
        {
          ExpCoordinator.printer.print(" Chat Message", 9);
          ChatMessage cmsg = new ChatMessage(rdr);
          observer.addChatMessage(cmsg.getSender(), cmsg.getMsg());
        }
      else
        {
          int op = (rdr.readShort() & 0x00ff);
          ExpCoordinator.printer.print(new String(" Other Message " + op), 9);
          if (op == Observer.NCCP_Operation_StateRequest)
            observer.processStateRequest(new Observer.StateRequest(dIn));
          if (op == Observer.NCCP_Operation_QuitMessage)
            observer.processQuitMessage(new Observer.QuitMessage(dIn));
        }      
    }

  protected void processResponse(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException
    { 
      ONL.NCCPReader rdr = new ONL.NCCPReader(dIn);
      NCCP.ResponseHeader responseHdr = new NCCP.ResponseHeader();
      ExpCoordinator.printer.print(new String("ObserveDaemon.processResponse msg size is " + msgsize), 9);
      responseHdr.retrieveHeader(dIn);
      responseHdr.print(10);
      observer.processPeerMessage(responseHdr, rdr);
      ExpCoordinator.printer.print(new String("ObserveDaemon.processResponse returned from  observer.processPeerMessage " + msgsize), 9);
    }
  protected void processPeriodicMsg(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException 
    {
      ExpCoordinator.printer.print("ObserveDaemon::run message is  NCCP.MessagePeriodic", 10);
      NCCP.PeriodicMessage msg = new NCCP.PeriodicMessage(dIn);
      if (msg.isAck()) 
        { 
          ++ackcount;
          sendAck(msg);
          ExpCoordinator.printer.print(new String("ObserveDaemon::run message is  NCCP.MessagePeriodic ack " + ackcount), 9);
        }
      else 
        {
          ExpCoordinator.printer.print(new String("ObserveDaemon::run message is  NCCP.MessagePeriodic stop ackcount = " + ackcount), 1);
          //msg.getRendMarker().print();
        }
    }
	
/*
  protected void addExpRequest(ExpRequest me)
    {
      me.setMarker(getNextMarker()); 
      requests.add(me);
      //ExpCoordinator.printer.print("ObserveDaemon::addExpRequest ");
      if (!isConnected())
	{
	  ExpCoordinator.printer.print(" connecting ");
	  connect();
	}
      sendRequest(me);
      }*/

  private REND_Marker_class getNextMarker()
    {
      return (new REND_Marker_class(++reqCount));
    }
/*
  protected ExpRequest getExpRequest(REND_Marker_class mrkr)
    {
      ExpCoordinator.printer.print(new String("ObserveDaemon::getExpRequest numRequests = " + requests.size()), 10);
      //mrkr.print(4);
      ExpRequest elem;
      int max = requests.size();
      for(int i = 0; i < max; ++i)
	{
	  elem = (ExpRequest)requests.elementAt(i);
	  REND_Marker_class r_mrkr = elem.getMarker();
	  if (r_mrkr == null) ExpCoordinator.printer.print("ObserveDaemon::getMEntry mrkr is null");
	  if (r_mrkr.isEqual(mrkr))
	    {
	      ExpCoordinator.print(new String("    found marker " + i), 10);
	      r_mrkr.print(4);
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
      ExpCoordinator.printer.print("ObserveDaemon::removeExpRequest", 1);
      ExpRequest elem;
      int max = requests.size();
      for (int i = 0; i < max; ++i)
	{
	  elem = (ExpRequest)requests.elementAt(i);
	  REND_Marker_class r_mrkr = elem.getMarker();
	  if (r_mrkr == null) ExpCoordinator.printer.print("ObserveDaemon::removeMEntry mrkr is null");
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
*/
  public short getSID() { return ((short)getPort());}
  public void setSID(short sid) 
  { 
    ExpCoordinator.print(new String("ObserveDaemon.setSID " + sid), 2);
    setPort((int)sid);
  }

  public void close() 
  {
    if (ExpCoordinator.isObserver())
      super.close();
    else 
      setConnected(false);
  }
}

