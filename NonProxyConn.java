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
	    System.out.println("Don't know about host: " + host + ":" + e.getMessage());
	  }
	catch (SocketTimeoutException e)
	  {
	    System.out.println("Socket time out for " + host + ":" + e.getMessage());
	  }
	catch (IOException e) 
	  {
	    System.out.println("Couldnt get I/O for " + host+ ":" + e.getMessage());
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

