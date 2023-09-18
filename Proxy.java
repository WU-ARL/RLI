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
 * File: Proxy.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 1/31/2005
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
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;

public class Proxy extends NodeLabel
{
	private ExpCoordinator expCoordinator = null;
	private Socket dataSocket = null;
	private DataOutputStream socketDataOut = null;
	private DataInputStream dataIn = null;
	private boolean connected = false;
	protected int count = 0;

	private boolean testing = false;//temporary for testing
	private TestMenu testMenu = null;

	//private Vector listeners;
	private Vector awaitingReply = null;
	private Vector connections = null;
	private short sessionID = 0;

	//command codes
	private static final short ECHO = 0;
	private static final short FORWARD = 1;
	private static final short OPEN = 2;
	private static final short CLOSE = 3;
	private static final short GET_STATUS = 4;
	private static final short REPLY = 5;
	private static final short STATUS = 6;

	//command codes needed for observation connections
	private static final short SHARE = 8; //requests copy connection
	private static final short PEER_FORWARD = 9;
	private static final short COPY_FORWARD = 10;
	private static final short GET_SID = 11;
	private static final short RETURN_SID = 12;
	private static final short SHARE_REPLY = 13;

	//command status codes
	private final int CMD_SUCCESS = 0x00000000;
	private final int CMD_FAILURE = 0x00000001;
	private final int CMD_PENDING = 0x00000002;
	//connection status codes
	private final int CONN_OPEN = 0x00000010;
	private final int CONN_PENDING = 0x00000020;
	private final int CONN_ERROR = 0x00000040;
	//error codes
	private final int NO_ERROR = 0x00000000;
	private final int PARAM_ERROR = 0x00001000;
	private final int AUTH_ERROR = 0x00002000;
	private final int TIMEOUT = 0x00003000;
	private final int DEST_ERROR = 0x00004000;
	private final int NET_ERROR = 0x00005000;
	private final int BAD_CONNID = 0x00006000;
	private final int CONN_NOT_OPEN = 0x00007000;
	private final int CONN_FAILURE = 0x00008000;
	private final int SYS_ERROR = 0x00010000;

	//types
	private static final int TCP = 6;

	private short currentMID = 0;

	private class Message
	{
		protected short len = 8;
		protected short command = ECHO;
		protected short messageID = 0;

		protected Connection connection = null;
		protected short connectionID = 0;

		public Message() {}
		public Message(short c, short cmd)
		{
			command = cmd;
			connectionID = c;
		}
		public Message(Connection c, short cmd)
		{
			command = cmd;
			connection = c;
		}
		public boolean isMessage(short m) { return (m == messageID);}
		public void setMessageID(short m) { messageID = m;}
		public void setConnectionID(short c) { connectionID = c;}
		public short getConnectionID() 
		{ 
			if (connection != null) 
				return (connection.getCID());
			else 
				return connectionID;
		}
		public short getCommand() { return command;}
		public void processReply(Reply r) 
		{
			if (awaitingReply.contains(this)) awaitingReply.remove(this);
		}
		public void storeMessage(DataOutputStream dout) throws IOException 
		{
			dout.writeShort(len);
			dout.writeShort(command);
			dout.writeShort(messageID);
			dout.writeShort(getConnectionID());
			//if (connection != null) dout.writeShort(connection.getCID());
			//else dout.writeShort(connectionID);
		}
		public String toString()
		{
			short cid = connectionID;
			if (connection != null) cid = connection.getCID();
			return (new String("Command " + command + ":(length, messageID, connectionID)= (" + len + ", " + messageID + ", " + cid + ")"));
		}
	}

	private class ForwardMessage extends Message
	{
		private NCCP.Message nccpMsg  = null;
		private int forwardLen = 0;

		public ForwardMessage(NCCP.Message msg, Connection c) { this(msg, c, FORWARD);}
		public ForwardMessage(NCCP.Message msg, Connection c, short cmd)
		{
			super(c, cmd);
			nccpMsg = msg;
		}

		public void storeMessage(DataOutputStream dout) throws IOException 
		{
			ExpCoordinator.print(new String("Proxy.ForwardMessage.storeMessage for " + connection.toString()), 8);
			ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(byteBuffer);
			byteBuffer.reset();
			ExpCoordinator.print("Proxy.ForwardMessage.storeMessage 1", 8);
			if (nccpMsg == null)
				ExpCoordinator.print("Proxy.ForwardMessage.storeMessage nccpMsg is null", 8);
			nccpMsg.store(dataOut);
			ExpCoordinator.print("Proxy.ForwardMessage.storeMessage 2", 8);
			forwardLen = byteBuffer.size();
			ExpCoordinator.print("Proxy.ForwardMessage.storeMessage 3", 8);
			len += forwardLen + 4;
			super.storeMessage(dout);
			ExpCoordinator.print(new String("msg stored " + byteBuffer.size()), 8);
			dout.writeInt(forwardLen);
			byteBuffer.writeTo(dout);
		}
		public int getForwardLen() { return forwardLen;}
	}

	private class ControlMessage extends Message
	{
		public ControlMessage(Connection c, short cmd) { super(c, cmd);}
		public ControlMessage(short c, short cmd) { super(c, cmd);}
		public ControlMessage(int c, short cmd) { this((short)c, cmd);}
		public ControlMessage(int c, int cmd) { this((short)c, (short)cmd);}
		public ControlMessage(short c, int cmd) { this(c, (short)cmd);}
	}

	private class ShareMessage extends ControlMessage
	{
		public ShareMessage(Connection c)
		{
			super(c, SHARE);
			len += 2;
			c.setOpenMessage(this);
		}

		public void processReply(Reply r) 
		{
			ExpCoordinator.print(new String("Proxy.ShareMessage processReply " + r.toString()), 3);
			if (r.succeeded()) 
			{
				ExpCoordinator.print("     succeeded", 3);
				connection.setCID(r.getConnectionID());
				connection.setConnected(true);
			}
			else 
			{
				ExpCoordinator.print("     failed", 3);
				connection.setConnected(false);
			}
			super.processReply(r);
		}

		public void storeMessage(DataOutputStream dout) throws IOException 
		{
			byte[] session_bytes = connection.getNBOPort();
			super.storeMessage(dout);
			dout.write(session_bytes, 0, 2);
		}
	}


	private class GetSIDMessage extends ControlMessage
	{
		public GetSIDMessage() { super(0, GET_SID);}
	}

	private class OpenMessage extends ControlMessage
	{
		//protected final int TCP = 6;
		public OpenMessage (Connection c)
		{
			super(c, OPEN);
			c.setOpenMessage(this);
			len += 3;
		}

		public void processReply(Reply r) 
		{
			ExpCoordinator.print(new String("Proxy.OpenMessage processReply " + r.toString()), 3);
			if (r.succeeded()) 
			{
				ExpCoordinator.print("     succeeded", 3);
				connection.setCID(r.getConnectionID());
				connection.setConnected(true);
			}
			else 
			{
				ExpCoordinator.print("     failed", 3);
				connection.setConnected(false);
			}
			super.processReply(r);
		}

		public void storeMessage(DataOutputStream dout) throws IOException 
		{
			byte[] hostBytes = connection.getNBOHost();
			int hst_len = Array.getLength(hostBytes);;
			len += hst_len;
			super.storeMessage(dout);
			//byte[] addr = connection.getNBOAddr();
			//dout.write(addr, 0, 4);
			byte[] port = connection.getNBOPort();
			dout.write(port, 0, 2);
			dout.writeByte(TCP);
			dout.write(hostBytes, 0, hst_len);
		}
	}

	private class CloseMessage extends ControlMessage
	{
		public CloseMessage (Connection c)
		{
			super(c, CLOSE);
		}

		public void processReply(Reply r) 
		{
			ExpCoordinator.print(new String("Proxy.CloseMessage.processReply " + connection.toString()), 3);
			if (r.succeeded()) 
			{
				connection.setConnected(false); 
			}   
			else connection.setConnected(true);
			super.processReply(r);
		}

		public short getConnectionID()
		{
			if (connection.isPeer()) return sessionID;
			else return (super.getConnectionID());
		}
	}

	private class ReplyHeader extends Message
	{
		protected short connectionID = 0;

		public ReplyHeader(DataInputStream din) throws IOException 
		{
			super();
			ExpCoordinator.print("Proxy.ReplyHeader", 9);
			len = din.readShort();
			ExpCoordinator.print(new String("Proxy.ReplyHeader len = "+ len), 9);
			command = din.readShort();
			ExpCoordinator.print(new String("Proxy.ReplyHeader command = "+ command), 9);
			messageID = din.readShort();
			ExpCoordinator.print(new String("Proxy.ReplyHeader messageID = "+ messageID), 9);
			connectionID = din.readShort();
			ExpCoordinator.print(new String("Proxy.ReplyHeader connectionID = "+ connectionID), 9);
		}
		public ReplyHeader(ReplyHeader r)
		{
			super();
			len = r.len;
			command = r.command;
			messageID = r.messageID;
			connectionID = r.connectionID;
		}
		public short getConnectionID() { return connectionID;}
		public short getMessageID() { return messageID;}
		public String toString()
		{
			String rtn = new String("length:" + len + " command:" + command + " messageID:" + messageID + " connectionID:" + connectionID);
			return rtn;
		}
	}

	private class Reply extends ReplyHeader
	{
		private int statusWord = 0;
		protected final int statusMask = 0x0000000F;
		protected final int connMask = 0x00000FF0;
		protected final int errMask = 0x000FF000;

		public Reply(ReplyHeader r, DataInputStream din) throws IOException 
		{
			super(r);
			statusWord = din.readInt();
			if (command == RETURN_SID)
				sessionID = din.readShort();
		}

		public boolean succeeded() 
		{
			return ((statusWord & statusMask) == CMD_SUCCESS);
		}

		public int getStatus()
		{
			return (statusWord & statusMask);
		}

		public int getError()
		{
			return (statusWord & errMask);
		}

		public int getConnectionState()
		{
			return (statusWord & connMask);
		}

		public String toString()
		{
			String rtn = new String(super.toString() + " status:" + statusWord + " sessionID:" + sessionID);
			return rtn;
		}
	}

	private class Connection
	{
		private NCCPConnection nccpconn = null;
		private short connectionID = 0;
		private ControlMessage openMessage = null;

		public Connection(NCCPConnection nc) 
		{
			nccpconn = nc;
		}

		public void setOpenMessage(ControlMessage omsg) { openMessage = omsg;}
		public void setCID(short cid) 
		{
			ExpCoordinator.print(new String("Proxy.Connection.setCID " + cid), 5);
			connectionID = cid;
		}
		public boolean isCID(short cid) { return (connectionID == cid);}
		public short getCID() { return connectionID;}
		public boolean isNCCP(NCCPConnection nc) { return (nc == nccpconn);}
		public boolean isHost(String hst) { return (nccpconn.getHost().equals(hst));}
		public void processMessage(DataInputStream din) throws IOException 
		{
			//ExpCoordinator.printer.print("Connection:processMessage");
			if (nccpconn != null) nccpconn.processMessage(din);
			else ExpCoordinator.print("Proxy.Connection.processMessage nccpconn is null");
		}
		public void setConnected(boolean b)
		{
			//ExpCoordinator.printer.print(new String(toString() + " setConnected " + b));
			if (!b) 
			{
				openMessage = null;
				connectionID = 0;
				if (connections.contains(this)) connections.remove(this);
			}
			if (nccpconn != null) nccpconn.setConnected(b);
		}
		public void setStatus(int stat, int err)
		{
			ExpCoordinator.print(new String(toString() + " setStatus " + stat + " error: " + err), 6);
			if (stat == CONN_ERROR || err == SYS_ERROR) 
			{
				nccpconn.connectionFailed();
				//report the error to user
				expCoordinator.addError(new StatusDisplay.Error((new String("Connection Failure for " + nccpconn.toString())),
						(new String("Connection to " + nccpconn.getHost())),
						"Connection Failure",
						(StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR)));
				//nccpconn.close();
			}
		}
		public boolean isClosed()
		{
			if (nccpconn.isClosed()) return true;
			if (openMessage == null) return true;
			return false;
		}
		public boolean isConnected() { return (nccpconn.isConnected());}
		public byte[] getNBOAddr() { return (nccpconn.getRAddr());}
		public byte[] getNBOPort() { return (nccpconn.getNBOPort());}
		public byte[] getNBOHost() { return (nccpconn.getNBOHost());}
		public String toString()
		{
			return (new String(nccpconn.toString() + " connectionID:" + connectionID));
		}
		public boolean isPeer() { return (nccpconn.getHost().equals(NCCPConnection.PEER));}
	}

	public Proxy(ExpCoordinator ec)
	{
		super("127.0.0.1", 7070);
		expCoordinator = ec;
		//listeners = new Vector();
		awaitingReply = new Vector();
		connections = new Vector();
	}

	public boolean connect()
	{
		try 
		{
			System.err.println("Proxy.connect to local port " + port);
			if (dataSocket != null) return true;
			dataSocket = new Socket(InetAddress.getByName(host), port);
			socketDataOut =  new DataOutputStream(new BufferedOutputStream(dataSocket.getOutputStream()));
			dataIn = new DataInputStream(new BufferedInputStream(dataSocket.getInputStream()));
			connected = true;
		}
		catch (UnknownHostException e) 
		{
			System.err.println("Don't know about host: " + host);
			connectionFailed(new String("Unable to Connect: unknown host " + host));
			return false;
		}
		catch (IOException e) 
		{
			System.err.println("Couldnt get I/O for " + host);
			if (host.equals("127.0.0.1"))
				connectionFailed(new String("Unable to Connect: please make sure you've created the ssh tunnel to ONL.\nTry the following command:\n ssh <your onl user name>@onl.wustl.edu -L 7070:onlsrv:7070 \n or Look at the Getting Started section at https://onl.wustl.edu"));
			else
				connectionFailed(new String("Unable to Connect: couldn't get I/O for " + host));
			return false;
		}

		Runnable second = new Runnable() 
		{
			public void run() 
			{
				ONL.ExpPropertyAction expPAction = (new ONL.ExpPropertyAction(false)
				{            
					public void propertyChange(PropertyChangeEvent e)
					{
						if (e.getPropertyName().equals(ExpCoordinator.RECORDING))
						{
							String val = (String)e.getNewValue();
							if (Boolean.valueOf(val).booleanValue())
								Thread.yield();
						}
					}
				});
				if (connected)
					sendMessage(new GetSIDMessage());
				while(connected) 
				{
					while (ExpCoordinator.isRecording()) Thread.yield();
					if (dataIn == null)
					{
						ExpCoordinator.print("Proxy dataIn is null");
						connected = false;
					}
					else
					{
						try  
						{
							ExpCoordinator.print("Proxy.run preread", 5);
							ReplyHeader rhdr = new ReplyHeader(dataIn);
							short cmd = rhdr.getCommand();
							ExpCoordinator.print(new String("Proxy.run " + rhdr.toString()), 5);
							if (cmd == FORWARD || cmd == PEER_FORWARD || cmd == COPY_FORWARD)
							{
								ExpCoordinator.print(new String("Proxy forward reply " + rhdr.toString()), 5);
								Connection c = null;
								if (cmd == PEER_FORWARD)
									c = getConnection(NCCPConnection.PEER);
								else 
									c = getConnection(rhdr.getConnectionID());
								if (c != null) 
								{
									c.processMessage(dataIn);
								}
								else
								{
									int msgsize = dataIn.readInt();
									ExpCoordinator.print(new String("       proxy skipping bytes " + msgsize));
									dataIn.skipBytes(msgsize); //need to just skip this message because we don't recognize it 
								}
							}
							else //it's a control message
							{
								Reply r = new Reply(rhdr, dataIn);
								ExpCoordinator.print(new String("Proxy control message " + r.toString()), 4);
								if (cmd == STATUS)
								{
									ExpCoordinator.print("   setting status", 4);
									Connection c = getConnection(r.getConnectionID());
									if (c != null) c.setStatus(r.getStatus(), r.getError());
								}
								else
								{
									Message m = getMessage(rhdr.getMessageID());
									if (m != null) m.processReply(r);
									else
									{
										if (cmd == SHARE_REPLY)
										{
											//create connection object
											Connection c = getConnection(NCCPConnection.PEER);
											if (c != null) 
											{
												ExpCoordinator.print(new String("Proxy.run ShareReply " + r.toString()), 4);
												if (r.succeeded()) 
												{
													ExpCoordinator.print("     succeeded", 4);
													c.setCID(r.getConnectionID());
													c.setConnected(true);
													
													ExpCoordinator.print("Proxy share message set connection", 8);
												}
												else ExpCoordinator.print("     failed", 4);
											}
										}
										if (cmd == CLOSE)
										{
											Connection c = getConnection(r.getConnectionID());
											if (c != null) 
											{
												ExpCoordinator.print(new String("Proxy.run CloseConnection " + r.toString()), 4);
												c.setConnected(false); 
											}
										}
									}
								}
							}
						}
						catch (SocketException e) 
						{
							ExpCoordinator.print(new String("Connection reset by peer -- closing connection for " + host + "." + port + "  Error:" + e.getMessage()));
							connectionFailed(new String("Socket Exception: " + e.getMessage()));//Connection reset by peer");
						}
						catch(IOException e) 
						{
							ExpCoordinator.print(new String("IOException in second for " + host + "." + port));
							connectionFailed("IOException");
							//e.printStackTrace();
						}
						catch(java.lang.NullPointerException e)
						{
							ExpCoordinator.print(new String("NullPointerException in second for " + host + "." + port));
							ExpCoordinator.print(new String("   count = " + count), 1);
							e.printStackTrace();
							//switchID.connectionFailed();
							//connected = false;
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

	private void connectionFailed(String msg)
	{
		JOptionPane.showMessageDialog(expCoordinator.getMainWindow(), 
				msg,
				"Main ONL Connection Failed", 
				JOptionPane.PLAIN_MESSAGE);
		close();
	}
	public void sendMessage(NCCP.Message msg, NCCPConnection nc)
	{
		Connection c = getConnection(nc);
		if (c != null)
		{
			ForwardMessage fmsg = null;
			if (c.isPeer()) fmsg = new ForwardMessage(msg, c, PEER_FORWARD);
			else 
			{
				fmsg = new ForwardMessage(msg, c);
			}
			if (fmsg != null) send(fmsg);
		}
	}

	public void sendMessage(ControlMessage cm)
	{
		if (!awaitingReply.contains(cm))
			awaitingReply.add(cm);
		cm.setMessageID(getNextMessageID());
		send(cm);
	}

	public void send(Message msg)
	{
		if (!connected) connect();
		if (connected)
		{
			try 
			{              
				//form ack from message switching the rendezvous markers for the return trip back to the sender
				ExpCoordinator.print("Proxy::sendMessage", 8);
				msg.storeMessage(socketDataOut);
				ExpCoordinator.print("message stored", 8);
				socketDataOut.flush();
				ExpCoordinator.print("flushed", 8);
			}
			catch (IOException e) 
			{
				//NEED something here
				ExpCoordinator.print("Send Message failed");
			}
		}
	}

	private Message getMessage(short mid)
	{
		int max = awaitingReply.size();
		Message elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (Message)awaitingReply.elementAt(i);
			if (elem.isMessage(mid)) return elem;
		}
		return null;
	}


	private Connection getConnection(String hst)
	{
		int max = connections.size();
		Connection elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (Connection)connections.elementAt(i);
			if (elem.isHost(hst)) return elem;
		}
		return null;
	}
	private Connection getConnection(short cid)
	{
		int max = connections.size();
		Connection elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (Connection)connections.elementAt(i);
			if (elem.isCID(cid)) return elem;
		}
		return null;
	}

	public void setConnectionID(NCCPConnection nc, short cid)
	{
		ExpCoordinator.print(new String("Proxy.setConnectionID for " + nc.toString() + " cid:" + cid), 1);
		Connection conn = getConnection(nc);
		if (conn != null) conn.setCID(cid);
	}

	public short getConnectionID(NCCPConnection nc)
	{
		Connection conn = getConnection(nc);
		if (conn != null) return (conn.getCID());
		return ((short)0);
	}
	private Connection getConnection(NCCPConnection nc)
	{
		int max = connections.size();
		Connection elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (Connection)connections.elementAt(i);
			if (elem.isNCCP(nc)) return elem;
		}
		return null;
	}

	public void addConnection(NCCPConnection c)
	{
		Connection conn = getConnection(c);
		if (conn == null) 
		{
			conn = new Connection(c);
			connections.add(conn);
		}
	}

	public void openConnection(NCCPConnection c)
	{
		Connection conn = getConnection(c);
		if (conn == null) 
		{
			conn = new Connection(c);
			connections.add(conn);
		}
		if (conn.isClosed())
		{
			if (conn.isPeer())
			{
			    ShareMessage smsg = new ShareMessage(conn);
			    sendMessage(smsg);
			}
			else
			{
			    OpenMessage omsg = new OpenMessage(conn);
			    sendMessage(omsg);
			}
		}
	}

	public void closeConnection(NCCPConnection c)
	{
		Connection conn = getConnection(c);
		ExpCoordinator.print(new String("Proxy.closeConnection " + c.toString()), 3);
		if (conn == null || !connected) return;
		if (conn.isClosed())
		{
			conn.setConnected(false);
			return;
		}
		if (conn.isConnected())
		{
			CloseMessage cmsg = new CloseMessage(conn);
			sendMessage(cmsg);
		}
	}

	public void close() 
	{
		try 
		{
			if (dataIn != null) dataIn.close();
			if (dataSocket != null) dataSocket.close();
			dataSocket = null;
			dataIn = null;
			connected = false;
			ExpCoordinator.print(new String("Proxy.close -- Closed control socket for " + host + "." + port));
			//fireEvent(Proxy.ConnectionEvent.CONNECTION_CLOSED);
			int max = connections.size();
			for (int i = 0; i < max; ++i)
			{
				Connection c = (Connection)connections.elementAt(i);
				c.nccpconn.close();
			}
			connections.clear();
		}
		catch(IOException e) 
		{
			ExpCoordinator.print("IOException on close", 1);
			//fireEvent(Proxy.ConnectionEvent.CONNECTION_CLOSED);
		}
	}

	public short getNextMessageID() 
	{
		if (currentMID < Short.MAX_VALUE) ++currentMID;
		else currentMID = 1;
		return currentMID;
	}
	public boolean isTesting() { return testing;} //temporary for testing
	public boolean isConnected() { return connected;}
	public String toString() { return (new String( host + " " + port));}

	public short getSID() { return sessionID;}

	protected JMenu getTestMenu() 
	{
		if (testMenu == null) testMenu = new TestMenu(this);
		return testMenu;
	}


	//////////////////////////////////////////////////////// Test Menu Class //////////////////////////////////////////////////////////////////////////////////
	private class TestMenu extends JMenu
	{
		private JTextArea tarea = null;
		private JFrame frame = null;
		private Proxy proxy = null;

		private class MessageAction extends AbstractAction
		{
			private int msgType = Proxy.GET_SID;

			public MessageAction(String ttl, int tp)
			{
				super(ttl);
				msgType = tp;
			}
			public void actionPerformed(ActionEvent e)
			{
				TestMessage message = null;
				switch(msgType)
				{
				case Proxy.OPEN:
					message = new OpenMessage();
					break;
				case Proxy.CLOSE:
					message = new CloseMessage();
					break;
				case Proxy.SHARE:
					message = new ShareMessage();
					break;
				case Proxy.PEER_FORWARD:
					message = new PFwdMessage();
					break;
				default:
					message = new GetSIDMessage();
				}
				if (message.fillMessage()) proxy.sendMessage(message);  
			}
		}

		private abstract class TestMessage extends Proxy.ControlMessage
		{
			protected Object paramObjects[] = null;
			public TestMessage(short cmd, int xtra_len)
			{
				super(0, cmd);
				len += xtra_len;
			}
			public void processReply(Proxy.Reply r) 
			{
				appendText(new String("Reply:" + toString() + " = " + r.toString() + "\n"));
				super.processReply(r);
			}
			public boolean fillMessage()
			{
				final String opt0 = "Send";
				final String opt1 = "Cancel";
				Object[] options = {opt0,opt1};

				int rtn = JOptionPane.showOptionDialog(ExpCoordinator.getMainWindow(), 
						paramObjects, 
						getMessageTitle(), 
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, 
						null,
						options,
						options[0]);
				if (rtn == JOptionPane.YES_OPTION)
					return true;
				else return false;
			}
			protected String getMessageTitle() { return ("Send Proxy Message");}
		}
		private class OpenMessage extends TestMessage
		{
			public final int HOST = 0;
			public final int PORT = 1;
			private String host = null;
			private short port = 0;

			public OpenMessage()
			{
				super(Proxy.OPEN, 3);
				paramObjects = new Object[2];
				paramObjects[HOST] = new TextFieldwLabel(25, "host (a.b.c.d):");
				paramObjects[PORT] = new TextFieldwLabel(10, "port:");
			}
			protected String getMessageTitle() { return ("Send Proxy Open Message");}
			public boolean fillMessage()
			{
				boolean rtn = super.fillMessage();
				if (rtn)
				{
					host = ((TextFieldwLabel)paramObjects[HOST]).getText();
					port = Short.parseShort(((TextFieldwLabel)paramObjects[HOST]).getText());
				}
				return rtn;
			}
			public void storeMessage(DataOutputStream dout) throws IOException 
			{
				byte[] hostBytes = NCCPConnection.getNBOHost(host);
				int hst_len = Array.getLength(hostBytes);
				len += hst_len;
				super.storeMessage(dout);
				//byte[] addr = connection.getNBOAddr();
				//dout.write(addr, 0, 4);
				byte[] port_bytes = NCCPConnection.getNBOPort(port);
				dout.write(port_bytes, 0, 2);
				dout.writeByte(Proxy.TCP);
				dout.write(hostBytes, 0, hst_len);
			}
		}

		private class CloseMessage extends TestMessage
		{
			public final int CID = 0;
			public CloseMessage() {this(Proxy.CLOSE);}
			public CloseMessage(short cmd)
			{
				super(cmd, 0);
				paramObjects = new Object[1];
				paramObjects[CID] = new TextFieldwLabel(10, "connection id:");
			}
			protected String getMessageTitle() { return ("Send Proxy Close Message");}
			public boolean fillMessage()
			{
				boolean rtn = super.fillMessage();
				if (rtn)
				{
					setConnectionID(Short.parseShort(((TextFieldwLabel)paramObjects[CID]).getText()));
				}
				return rtn;
			}
		}

		private class PFwdMessage extends CloseMessage
		{
			public PFwdMessage() { super(Proxy.PEER_FORWARD);}
			protected String getMessageTitle() { return ("Send Proxy Peer Forward Message");}
		}

		private class GetSIDMessage extends TestMessage
		{
			public GetSIDMessage()
			{
				super(Proxy.GET_SID, 0);
			}
			protected String getMessageTitle() { return ("Send Proxy Get Session ID Message");}
		}

		private class ShareMessage extends TestMessage
		{
			//public final int CID = 0;
			public final int SID = 0;
			private short sessionID = 0;
			public ShareMessage()
			{
				super(Proxy.SHARE, 1);
				paramObjects = new Object[1];
				//paramObjects[CID] = new TextFieldwLabel(10, "connection id:");
				paramObjects[SID] = new TextFieldwLabel(10, "session id:");
			}
			protected String getMessageTitle() { return ("Send Proxy Share Message");}

			public boolean fillMessage()
			{
				boolean rtn = super.fillMessage();
				if (rtn)
				{
					//setConnectionID(Short.parseShort(((TextFieldwLabel)paramObjects[CID]).getText()));
					sessionID = Short.parseShort(((TextFieldwLabel)paramObjects[SID]).getText());
				}
				return rtn;
			}

			public void storeMessage(DataOutputStream dout) throws IOException 
			{
				byte[] session_bytes = NCCPConnection.getNBOPort(sessionID);
				super.storeMessage(dout);
				dout.write(session_bytes, 0, 2);
				ExpCoordinator.print(new String("ShareMessage.storeMessage len=" + len));
			}
		}

		////////////////////////////////////////////////Constructor///////////////////////////////////////////////////////////////////////////////////

		public TestMenu(Proxy p)
		{
			super("Proxy Test");
			proxy = p;

			frame = new JFrame("Proxy Test Reply");
			frame.setVisible(false);
			tarea = new JTextArea();
			tarea.setLineWrap(true);
			tarea.append("\n");
			frame.getContentPane().add((new JScrollPane(tarea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)), BorderLayout.CENTER);
			frame.setSize(225,120);
			tarea.setSize(225,120);
			add(new MessageAction("Open Message", Proxy.OPEN));
			add(new MessageAction("Close Message", Proxy.CLOSE));
			add(new MessageAction("Share Message", Proxy.SHARE));
			add(new MessageAction("Get SID Message", Proxy.GET_SID));
			add(new MessageAction("Peer Forward Message", Proxy.PEER_FORWARD));
		}

		private void initiateFrame()
		{
			if ((frame.getExtendedState() & Frame.ICONIFIED) != 0)
				frame.setExtendedState(frame.getExtendedState() - Frame.ICONIFIED);
			frame.setVisible(true);
		}

		public void appendText(String s)
		{
			initiateFrame();
			tarea.append(s);
		}
	}//end class TestMenu
}

