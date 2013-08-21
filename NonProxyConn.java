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
 * File: NonProxyConn.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 2/24/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.event.*;

public class NonProxyConn //extends NodeLabel
{
  private Socket dataSocket = null;
  private DataOutputStream dataOut = null;
  private DataOutputStream socketDataOut = null;
  private DataInputStream dataIn = null;
  private ByteArrayOutputStream byteBuffer = null;
  private boolean connected = false;
  protected int count = 0;
  private Vector pendingMessages = null;
  private NCCPConnection nccpconn = null;
  private String host = null;
  private int port = 0;


  public NonProxyConn(NCCPConnection conn)
    {
      nccpconn = conn;
      host = nccpconn.getHost();
      port = nccpconn.getPort();
      pendingMessages = new Vector();
    }

  public boolean connect() throws UnknownHostException, IOException, SocketTimeoutException
    {
      host = nccpconn.getHost();
      port = nccpconn.getPort();
      if (host.length() == 0) return false;
      //try 
      //{
	  System.err.println("NonProxyConn.connect to host " + host + " port " + port);
	  if (dataSocket != null && nccpconn.isConnected()) return true;
	  InetSocketAddress tmp_socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
	  dataSocket = new Socket();
	  dataSocket.connect(tmp_socketAddress, 10000);
	  ExpCoordinator.print("      opened socket", 1);
	  byteBuffer = new ByteArrayOutputStream();
	  dataOut = new DataOutputStream(byteBuffer);
	  socketDataOut =  new DataOutputStream(new BufferedOutputStream(dataSocket.getOutputStream()));//dataSocket.getOutputStream();
	  dataIn = new DataInputStream(new BufferedInputStream(dataSocket.getInputStream()));
	  connected = true;
          nccpconn.setConnected(true);
	  int max = pendingMessages.size();
	  for (int i = 0; i < max; ++i)
	    {
	      sendMessage((NCCP.Message)pendingMessages.elementAt(i));
	    }
	  //}
      /*
      catch (UnknownHostException e) 
	{
	  System.err.println("Don't know about host: " + host);
          nccpconn.connectionFailed();//setConnected(false);
	  //fireEvent(new ConnectionEvent(this, ConnectionEvent.CONNECTION_FAILED));
	  connected = false;
	  return false;
	  // testing = true;//temporary for testing
	}
      catch (IOException e) 
	{
	  System.err.println("Couldnt get I/O for " + host);
          nccpconn.connectionFailed();//setConnected(false);
	  //fireEvent(new ConnectionEvent(this, ConnectionEvent.CONNECTION_FAILED));
	  return false;
	  //testing = true;//temporary for testing
	}
      catch (SocketTimeoutException e)
	{
	  System.err.println("Socket time out for " + host + ":" + e.getMessage());
	  nccpconn.connectionTimedOut();
	  return false;
	}
      */
      Runnable second = new Runnable() 
	{
	  public void run() 
	    {
	      System.out.println("NonProxyConn run");
	      while(connected) // && !testing) 
		{
		  if (dataIn == null)
		    {
		      System.out.println("NonProxyConn dataIn is null");
		      connected = false;
                      nccpconn.setConnected(false);
		    }
		  else
		    {
		      try  
			{
                          nccpconn.processMessage(dataIn);
			}
		      catch (SocketException e) 
			{
			  System.out.println("Connection reset by peer -- closing connection for " + host + "." + port);
			  connected = false;
                          nccpconn.connectionFailed();//setConnected(false);
			  //fireEvent(NonProxyConn.ConnectionEvent.CONNECTION_FAILED);
			}
		      catch(IOException e) 
			{
			  System.out.println("IOException in second for " + host + "." + port);
			  connected = false;
                          nccpconn.connectionFailed();
			  //fireEvent(NonProxyConn.ConnectionEvent.CONNECTION_FAILED);
			  //e.printStackTrace();
			}
		      catch(java.lang.NullPointerException e)
			{
			  System.out.println("NullPointerException in second for " + host + "." + port);
			  System.out.println("   count = " + count);
			}
		  
		      Thread.yield();
		    }
		}
	    }
	};
	Thread t2 = new Thread(second);
	t2.start();
	return true;
    }
	
  public void sendMessage(NCCP.Message msg)
    {
      if (!connected) 
	{
	  try { connect();}
	catch(UnknownHostException e) 
	  {
	    nccpconn.informUserError("Don't know about host: " + host + ":" + e.getMessage());
	  }
	catch (SocketTimeoutException e)
	  {
	    nccpconn.informUserError("Socket time out for " + host + ":" + e.getMessage());
	  }
	catch (IOException e) 
	  {
	    nccpconn.informUserError("Couldnt get I/O for " + host+ ":" + e.getMessage());
	  }
      }
      if (connected)
	{
	  try 
	    { 
	      //form ack from message switching the rendezvous markers for the return trip back to the sender
	      ExpCoordinator.print("NonProxyConn.sendMessage", 7);
	      //ack.print();
	      byteBuffer.reset();
	      msg.store(dataOut);
	      //System.out.println
	      ExpCoordinator.print(new String("msg stored " + byteBuffer.size()), 7);
	      socketDataOut.writeInt(byteBuffer.size());
	      byteBuffer.writeTo(socketDataOut);
	      //System.out.println
	      ExpCoordinator.print(new String("buffer is " + byteBuffer.size() + " bytes long "), 7);
	      socketDataOut.flush();
	      //System.out.println
	      ExpCoordinator.print("flushed", 7);
	    }
	  catch (IOException e) 
	    {
	      //NEED something here
	      System.out.println("Send Message failed");
	    }
	}
      else //still can't connect
	{
	  pendingMessages.add(msg);
	}
    }
  
  
  public void close() 
    {
      try 
	{
	  if (dataOut != null) dataOut.close();
	  if (dataIn != null) dataIn.close();
	  if (dataSocket != null) dataSocket.close();
	  dataSocket = null;
	  dataOut = null;
	  dataIn = null;
	  connected = false;
	  System.out.println("Closed control socket for " + host + "." + port); 
          nccpconn.setConnected(false);
	  //fireEvent(NCCPConnection.ConnectionEvent.CONNECTION_CLOSED);
	}
      catch(IOException e) 
	{
	  System.out.println("IOException on close");
          nccpconn.setConnected(false);
	  //fireEvent(NCCPConnection.ConnectionEvent.CONNECTION_CLOSED);
	}
    } 
  public void flush() 
    {
      try {
	dataOut.flush();
      }
      catch (IOException e) { }
    }
  public boolean isConnected() { return connected;}
  public byte[] getRAddr()
    {
      try
	{
	  InetAddress addr = InetAddress.getByName(host);
	  return addr.getAddress();
	}
      catch (UnknownHostException e) 
	{
	  System.err.println("NCCPConnection::getRAddr Don't know about host: ");
	  return null;
	}
    }
  public String toString() { return (new String( host + " " + port));}
}

