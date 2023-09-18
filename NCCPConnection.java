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
 * File: NCCPConnection.java
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
import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.*;

import java.nio.*;
import java.lang.reflect.Array;

public abstract class NCCPConnection extends NodeLabel
{
	public static final String PEER = "peer";
	private int state = ConnectionEvent.CONNECTION_CLOSED;
	//private boolean connected = false;
	protected int count = 0;
	private boolean portSet = false;

	private boolean testing = false;//temporary for testing

	private Vector listeners;
	protected Vector pendingMessages = null; //changed 10/1/08 from private
	private Proxy proxy = null;
	private NonProxyConn nonProxy = null;
	private boolean isRecording = false;


	private class MessageRunnable implements Runnable
	{
		private int msgsize = 0;
		private int msgtype = 0;
		private DataInputStream din = null;
		private byte msgbytes[] = null;


		public MessageRunnable(int msz, int mtp, DataInputStream di) throws IOException, SocketException
		{
			msgsize = msz;
			msgtype = mtp;
			msgbytes = new byte[msgsize-2];
			di.readFully(msgbytes);
			// di.read(msgbytes);
			din = new DataInputStream(new ByteArrayInputStream(msgbytes));
		}
		public void run()
		{
			try
			{
				if (msgtype == NCCP.MessageResponse)
					processResponse(msgsize, msgtype, din);
				else
					processOtherMsg(msgsize, msgtype, din);
			}
			catch(java.io.IOException e) {}
		}
		public void print(int dlevel)
		{
			if (msgtype == NCCP.MessageResponse)
				ExpCoordinator.print(new String("NCCPConnection.Message Response size = " + msgsize) , dlevel);
			else
				ExpCoordinator.print(new String("NCCPConnection.Message type = " + msgtype + " size = " + msgsize) , dlevel);
			String tmp_str = "";
			int max = msgsize -2;
			for (int i = 0 ; i < max; ++i)
			{
				for (int j = 0; j < 4; ++j)
				{
					if (i < max)
					{
						tmp_str = tmp_str.concat(new String(msgbytes[i] + " "));
						++i;
					}
				}

				ExpCoordinator.print(new String("     " + tmp_str), dlevel);
				tmp_str = "";
			}
		}
	}


	public static class ConnectionEvent extends EventObject
	{
		public static final int CONNECTION_FAILED = 1;
		public static final int CONNECTION_CLOSED = 2;
		public static final int CONNECTION_OPENED = 3;
		public static final int CONNECTION_PENDING = 4;
		public static final int ADDRESS_CHANGED = 5;
		private int eType = 0;

		public ConnectionEvent(NodeLabel id, int tp)
		{
			super(id);
			eType = tp;
		}
		public int getType() { return eType;}
		public int getPort() { return (((NodeLabel)getSource()).getPort());}
		public String getHost() { return (((NodeLabel)getSource()).getHost());}
	}

	public static interface ConnectionListener extends EventListener
	{
		public void connectionFailed(NCCPConnection.ConnectionEvent e);
		public void connectionClosed(NCCPConnection.ConnectionEvent e);
		public void connectionOpened(NCCPConnection.ConnectionEvent e);
	}

	public NCCPConnection(String h, int p, String  lbl) 
	{
		super(h,p,lbl);
		listeners = new Vector();
		pendingMessages = new Vector();
	}

	public void setProxy(Proxy p) 
	{ 
		proxy = p;
		proxy.addConnection(this);
	}
	protected void setNonProxy()
	{
		nonProxy = new NonProxyConn(this);
	}
	public boolean connect()
	{
		ExpCoordinator.printer.print("NCCPConnection.connect", 3);
		if (host.length() == 0) 
		{
			ExpCoordinator.printer.print("NCCPConnection.connect host is not set", 6);
			return false;
		}
		if (proxy == null && nonProxy == null) 
		{
			ExpCoordinator.printer.print("NCCPConnection.connect proxy null");
			return false;
		}
		if (state == ConnectionEvent.CONNECTION_CLOSED || state == ConnectionEvent.CONNECTION_FAILED)
		{
			state = ConnectionEvent.CONNECTION_PENDING;
			if (proxy != null)
			{	
				proxy.openConnection(this);
			}
		}
		/*
		if (nonProxy != null && proxy == null)
		{
			return (nonProxy.connect());
		}
		*/
		return true;
	}
	
	public void connectionFailed()
	{
		ExpCoordinator.printer.print(new String("ConnectionFailed Closed control socket for " + host + "." + port));
		if (state != ConnectionEvent.CONNECTION_CLOSED)
		{
			state = ConnectionEvent.CONNECTION_FAILED;
			fireEvent(new ConnectionEvent(this,ConnectionEvent.CONNECTION_FAILED));
			close();
		}
	}

	public void setConnected(boolean b) { setConnected(b, true);}
	public void setConnected(boolean b, boolean process_pending)
	{
		if (b && state != ConnectionEvent.CONNECTION_OPENED)
		{
			if (host.length() > 0) ExpCoordinator.print("NCCPConnection open connection to ipaddress " + host + " port " + port);
			state = ConnectionEvent.CONNECTION_OPENED;
			//connected = true;
			fireEvent(new ConnectionEvent(this, state));
			ExpCoordinator.printer.print("NCCPConnection.setConnected " + b + " event fired", 8);
			if (process_pending) 
			{
				int max = pendingMessages.size();
				ExpCoordinator.printer.print(new String("   pendingMessages " + max + " elements"), 8);
				for (int i = 0; i < max; ++i)
				{
					sendMessage((NCCP.Message)pendingMessages.elementAt(i));
				}
				pendingMessages.removeAllElements();
			}
		}
		else 
		{
			if (!b && (state != ConnectionEvent.CONNECTION_CLOSED || state != ConnectionEvent.CONNECTION_FAILED))
			{
				if (host.length() > 0) ExpCoordinator.print(new String("NCCPConnection.setConnected -- Closed control socket for " + host + "." + port));
				state = ConnectionEvent.CONNECTION_CLOSED;
				fireEvent(new ConnectionEvent(this, state));
			}
		}
	}


	public void sendAck(NCCP.PeriodicMessage msg) 
	{
		//form ack from message switching the rendezvous markers for the return trip back to the sender
		NCCP.PeriodicMessage ack = new NCCP.PeriodicMessage(NCCP.Operation_AckPeriodic, msg.getReturnRendMarker(), msg.getRendMarker());
		ExpCoordinator.printer.print("NCCPConnection::sendAck", 9);
		//ack.print();
		sendMessage(ack);
	}

	public void sendMessage(NCCP.Message msg)
	{
		if (isClosed() || isFailed()) connect();
		if (isConnected())
		{
			ExpCoordinator.printer.print("NCCPConnection::sendMessage", 5);
			if (proxy != null) proxy.sendMessage(msg, this);
			else
			{
				if (nonProxy != null) nonProxy.sendMessage(msg);
			}
		}
		else //still can't connect
		{
			pendingMessages.add(msg);
		}
	}

	public void close() 
	{
		if (proxy != null && isConnected())
		{
			proxy.closeConnection(this);
			setConnected(false);
		} 
		else
		{
			if (nonProxy != null && isConnected())
			{
				try{
					Thread.sleep(1000);
				}
				catch (java.lang.InterruptedException e2) 
				{}
				nonProxy.close();
				setConnected(false);
			}
		}
	}
	public void flush() 
	{
		if (nonProxy != null) nonProxy.flush();
	}
	public void processMessage(DataInputStream dataIn) throws IOException
	{
		count = 0;
		int msgsize = dataIn.readInt();
		count = 1;
		int msgtype = 0;
		if (msgsize >= 2) msgtype = dataIn.readUnsignedShort();
		else
		{
			if (msgsize == 1) dataIn.readByte();
			ExpCoordinator.printer.print(new String("ERROR:NCCPConnection.run msgsize(" + msgsize + ") < 2 msgtype = " + msgtype));
			ExpCoordinator.printer.printHistory();
			return;
		}
		count = 2;
		switch (msgtype)
		{
		case NCCP.MessageResponse:
			ExpCoordinator.print(new String("NCCPConnection::run message is  NCCP.MessageResponse " + msgsize + " " + msgtype), 3);
			MessageRunnable tmp_msg = new MessageRunnable(msgsize, msgtype, dataIn);
			tmp_msg.print(5);
			SwingUtilities.invokeLater(tmp_msg);
			break;
		case NCCP.MessagePeriodic:
			ExpCoordinator.printer.print(new String("NCCPConnection::run message is  NCCP.MessagePeriodic " + msgsize + " " + msgtype), 3);
			processPeriodicMsg(msgsize, msgtype, dataIn);
			break;
		default:
			ExpCoordinator.printer.print(new String("NCCPConnection::run message is  Other " + msgsize + " " + msgtype));
			SwingUtilities.invokeLater(new MessageRunnable(msgsize, msgtype, dataIn));
		}
	}
	public boolean isTesting() { return testing;} //temporary for testing
	public boolean isConnected() { return (state == ConnectionEvent.CONNECTION_OPENED);}
	public boolean isClosed() { return (state == ConnectionEvent.CONNECTION_CLOSED);}
	public boolean isFailed() { return (state == ConnectionEvent.CONNECTION_FAILED);}
	public void addConnectionListener(NCCPConnection.ConnectionListener l) 
	{ 
		if (!listeners.contains(l)) 
		{
			ExpCoordinator.printer.print(new String("NCCPConnection.addConnectionListener to " + toString()), 6);
			listeners.add(l);
		}
	}
	public void removeConnectionListener(NCCPConnection.ConnectionListener l) 
	{ 
		if (listeners.contains(l)) 
		{
			//ExpCoordinator.printer.print(new String("NCCPConnection.removeConnectionListener to " + toString()), 2);
			listeners.remove(l);
		}
	}

	public void fireEvent(int tp)
	{
		NCCPConnection.ConnectionEvent e = new NCCPConnection.ConnectionEvent(this, tp);
		fireEvent(e);
	}
	public void fireEvent(NCCPConnection.ConnectionEvent e)
	{
		ConnectionListener l;
		int max = listeners.size();
		for (int i = 0; i < max; ++i)
		{
			ExpCoordinator.print(new String("NCCPConnection.fireEvent (" + toString() + ") event: " + e.getType() + " max:" + max + " i:" + i), 2);
			l = (ConnectionListener) (listeners.elementAt(i));
			switch(e.getType())
			{
			case ConnectionEvent.CONNECTION_FAILED:
				l.connectionFailed(e);
				break;
			case ConnectionEvent.CONNECTION_CLOSED:
				l.connectionClosed(e);
				break;
			case ConnectionEvent.CONNECTION_OPENED:
				ExpCoordinator.printer.print(new String("Opened control connection (" + toString() + ")"));
				l.connectionOpened(e);
			}
		}
	} 

	//descendants define to handle a NCCP response
	protected abstract void processResponse(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException ;
	//descendants define to handle a NCCP periodic response 
	protected abstract void processPeriodicMsg(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException ;

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

	protected void processOtherMsg(int msgsize, int msgtype, DataInputStream dIn) throws SocketException, IOException, java.lang.NullPointerException 
	{
		dIn.skipBytes(msgsize - 2); //need to just skip this message because we don't recognize it 
		ExpCoordinator.printer.print(new String("NCCPConnection.processOtherMsg skipping message " + msgsize + " " + msgtype));
		ExpCoordinator.printer.printHistory();
	}
	public byte[] getNBOPort() { return (getNBOPort((short)port));}
	public static byte[] getNBOPort(short p)
	{
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.putShort((short)p);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.rewind();
		byte[] rtn = new byte[2];
		buf.get(rtn);
		return rtn;
	}
	public byte[] getNBOHost() { return (getNBOHost(host));}
	public static byte[] getNBOHost(String h)
	{
		if (h.equals(PEER)) return null;
		byte hostBytes[] = h.getBytes();
		int len = Array.getLength(hostBytes);
		ByteBuffer buf = ByteBuffer.allocate(len);
		buf.put(hostBytes);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.rewind();
		return (buf.array());
	}
	public String toString() { return (new String( host + " " + port));}
	protected Proxy getProxy() { return proxy;}

	public void write(ONL.Writer wrtr) throws java.io.IOException
	{
		wrtr.writeString(host);
		wrtr.writeInt(port);
		short cid = 0;
		if (proxy != null) cid = proxy.getConnectionID(this);
		wrtr.writeShort(cid);
	}
	public void read(ONL.Reader rdr) throws java.io.IOException
	{
		setHost(new String(rdr.readString()));
		setPort(rdr.readInt());
		short cid = (short)rdr.readShort();
		ExpCoordinator.print(new String("NCCPConnection.read host:" + getHost() + " port:" + getPort() + " cid:" + cid), 5);
		if (proxy != null) proxy.setConnectionID(this, cid);
	}
	public void setConnectionID(short cid) 
	{ 
		ExpCoordinator.print(new String("NCCPConnection.setConnectionID host:" + getHost() + " port:" + getPort() + " cid:" + cid), 5);
		if (proxy != null) proxy.setConnectionID(this, cid);
	}
	public short getConnectionID() 
	{
		if (proxy != null) return ((short)proxy.getConnectionID(this));
		else return ((short)0);
	}

	protected int getState() { return state;}
}

