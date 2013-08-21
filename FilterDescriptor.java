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
 * File: FilterDescriptor.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/29/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.lang.String;
import java.io.*;
import javax.swing.undo.*;
import javax.swing.*;
import javax.swing.event.*;
import java.lang.reflect.Array;
import java.util.*;
import java.text.ParseException;
import java.lang.NumberFormatException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;


public class FilterDescriptor implements ONLCTable.Element, PropertyChangeListener //,ONLTableCellRenderer.Committable
{
	public static final int SPC_FLOW = 128;
	public static final int MAX_PRIORITY = 63;
	//property names
	public static final String FILTER_EDITABLE = "filter_editable";
	public static final String SRCADDR_SET = "srcaddr_set";
	public static final String SRCPORT_SET = "srcport_set";
	public static final String DESTADDR_SET = "destaddr_set";
	public static final String DESTPORT_SET = "destport_set";
	public static final String PROTOCOL_SET = "protocol_set";
	public static final String FKEY_SET = "fkey_set";
	public static final String PRIORITY_SET = "priority_set";
	public static final String TYPE = "type";
	public static final String COMMITTED_FD = "committed_fd";
	public static final String COMMITTED = "committed";
	public static final String ENABLED = "enabled";

	//field indices or table columns
	protected static final int SRCADDR = 0;
	protected static final int DESTADDR = 1;
	protected static final int SRCPORT = 2;
	protected static final int DESTPORT = 3;

	protected static final int PROTOCOL = 4;

	protected static final int FORWARDKEY = 5;  
	protected static final int PBINDING = 6;
	protected static final int QID = 7;
	protected static final int STATS = 8;
	protected static final int PRIORITY = 9;
	protected static final int MIRROR = 10;
	protected static final int ADDR_TYPE = 11;
	protected static final int NEGATE = 12;
	protected static final int DROP = 13;
	protected static final int ENABLE = 14;
	protected static final int SAMPLING_TYPE = 15;
	//protected static final int TCPFLAGS = 16;
	//protected static final int TCPFLAGSMASK = 17;
	protected static final int TCPFIN = 16;
	protected static final int TCPSYN = 17;
	protected static final int TCPRST = 18;
	protected static final int TCPPSH = 19;
	protected static final int TCPACK = 20;
	protected static final int TCPURG = 21;

	protected static final int TCPFIN_MASK = 1;
	protected static final int TCPSYN_MASK = 2;  protected static final int TCPRST_MASK = 4;
	protected static final int TCPPSH_MASK = 8;
	protected static final int TCPACK_MASK = 16;
	protected static final int TCPURG_MASK = 32;

	//filter types
	public static final int EXACT_MATCH = 52;//ONLComponent.IPPEMFTABLE;
	public static final int GEN_MATCH = 53;//ONLComponent.IPPGMFTABLE;
	public static final int EXCL_GEN_MATCH = 54;//ONLComponent.IPPEXGMFTABLE; //MSRC will figure this out from the type and the qid
	public static final int MCAST = EXCL_GEN_MATCH + 1;

	//wild card for GM Filters
	public static final String WILDCARD_STRING = "*";
	public static final int WILDCARD = 0;

	protected FilterDescriptor.PrefixMask srcAddr = null;
	protected PortRange srcPortRange = null;
	protected FilterDescriptor.PrefixMask destAddr = null;
	protected PortRange destPortRange = null;
	private Protocol protocol = null;
	private PluginBinding pluginBinding = null;
	private Queue.ID qid = null;
	private int priority = 60;
	private NSPPort nspport = null;
	private NSPDescriptor nsp = null;
	private ExpCoordinator expCoordinator = null;
	private boolean ingress = true;
	private boolean negation = false;
	private boolean drop = false;
	private boolean enabled = true;
	private RouterPort.SamplingType samplingType = null;
	private int tcpflags = 0;
	private int tcpflagsMask = 0;
	private FilterTable ftable = null;

	protected ForwardKey forwardKey = null;
	private Stats stats = null;
	private ONLPropertyList properties = null;

	private EventListenerList listeners = null;
	private UpdateEdit updateEdit = null;

	private int numCommits = 0;

	////////////////////////////////////////////////////// FilterDescriptor.XMLHandler /////////////////////////////////////////////////
	public static class XMLHandler extends DefaultHandler
	{
		protected ExperimentXML expXML = null;
		protected String currentElement = "";
		protected FilterDescriptor currentFilter = null;

		public XMLHandler(ExperimentXML exp_xml, FilterDescriptor fd)
		{
			super();
			currentFilter = fd;
			expXML = exp_xml;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			currentElement = new String(localName);
			ExpCoordinator.print(new String("FilterDescriptor.XMLHandler.startElement localName:" + localName), ExperimentXML.TEST_XML);
		}
		public void characters(char[] ch, int start, int length) throws SAXException
		{
			try
			{
				if (currentElement.equals(ExperimentXML.SRCADDR) && currentFilter != null)
					currentFilter.setSrcAddr(new FilterDescriptor.PrefixMask(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.DESTADDR) && currentFilter != null)
					currentFilter.setDestAddr(new FilterDescriptor.PrefixMask(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.SRCPORT) && currentFilter != null)
					currentFilter.setSrcPort(new FilterDescriptor.PortRange(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.DESTPORT) && currentFilter != null)
					currentFilter.setDestPort(new FilterDescriptor.PortRange(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.PROTOCOL) && currentFilter != null)
					currentFilter.setProtocol(new FilterDescriptor.Protocol(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.DROP) && currentFilter != null)
					currentFilter.setDrop(Boolean.parseBoolean(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.STATS_INDEX) && currentFilter != null)
					currentFilter.setStatsIndex(Integer.parseInt(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.SAMPLING_TYPE) && currentFilter != null)
					currentFilter.setSamplingType(currentFilter.ftable.getSamplingType(Integer.parseInt(new String(ch, start, length))));
				if (currentElement.equals(ExperimentXML.TCPFLAGS) && currentFilter != null)
					currentFilter.setTCPFlags(Integer.parseInt(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.TCPFLAGSMASK) && currentFilter != null)
					currentFilter.setTCPFlagsMask(Integer.parseInt(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.PRIORITY) && currentFilter != null)
					currentFilter.setPriority(Integer.parseInt(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.NEGATE) && currentFilter != null)
					currentFilter.setNegation(Boolean.parseBoolean(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.FORWARD_KEY) && currentFilter != null)
					currentFilter.setFKey(new FilterDescriptor.ForwardKey(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.PBINDING) && currentFilter != null)
					currentFilter.setPluginBinding(new FilterDescriptor.PluginBinding(new String(ch, start, length)));
				if (currentElement.equals(ExperimentXML.QID) && currentFilter != null)
					currentFilter.setQID(new FilterDescriptor.QueueID(new String(ch, start, length)));
			}
			catch (java.text.ParseException e)
			{
				ExpCoordinator.print(new String("FilterDescriptor.XMLHandler.characters ParseException " + e.getMessage()));
			}
		}

		public void endElement(String uri, String localName, String qName)
		{
			if (localName.endsWith(ExperimentXML.FILTER)) 
			{
				expXML.removeContentHandler(this);
			}
		}
	}//end FilterDescriptor.XMLHandler
	///////////////////////////////////////////////////////// FilterDescriptor.TCPFlag //////////////////////////////////////////////////
	public static class TCPFlag
	{
		private int flag = 0;
		private int mask = 0;
		private String string = "*";
		public TCPFlag(){}
		public TCPFlag(String s) throws java.text.ParseException
		{
			if (s.equals("*"))
			{
				flag = 0;
				mask = 0;
			}
			else 
			{
				if (s.equals("1"))
				{
					flag = 1;
					mask = 1;
				}
				else
				{
					if (s.equals("0"))
					{
						flag = 0;
						mask = 1;
					}
					else
						throw new java.text.ParseException("TCPFlag parse error: should be *,1, or 0", 0);
				}
			}
			string = new String(s);
		}
		public TCPFlag(int f, int m)
		{
			flag = f;
			mask = m;
			if (mask == 0) string = "*";
			if (flag > 0 && mask > 0) string = "1";
			if (flag == 0 && mask > 0) string = "0";
		}
		public String toString() { return string;}
		public int getFlag() { return flag;}
		public int getMask() { return mask;}
	}

	public static class Event extends EventObject
	{
		public static final int QID_CHANGED = 0;
		public static final int STATS_CHANGED = 1;
		public static final int PBINDING_CHANGED = 2;
		public static final int DATA_CHANGED = 3;

		private int event_type = QID_CHANGED;
		private double oldValue = 0;
		private double newValue = 0;


		public Event(FilterDescriptor fd) { this(fd, DATA_CHANGED, 0, 0);}
		public Event(FilterDescriptor fd, int tp, double ov, double nv)
		{
			super(fd);
			event_type = tp;
			oldValue = ov;
			newValue = ov;
		}
		public int getType() { return event_type;}
		public double getNewValue() { return newValue;}
		public double getOldValue() { return oldValue;}
	}

	public interface Listener extends EventListener
	{
		public void changedPBinding(FilterDescriptor.Event e);
		public void changedQID(FilterDescriptor.Event e);
		public void changedStats(FilterDescriptor.Event e);
		public void changedData(FilterDescriptor.Event e);
	}

	public static class Stats extends PortStats
	{
		private FilterDescriptor filter = null; 
		private MonitorDataType.Base mDataType = null;
		public Stats(String s) throws java.text.ParseException
		{
			super(s);
		}
		public Stats(NSPPort p, FilterDescriptor fd, boolean in)
		{
			super(p);
			filter = fd;
		}
		public MonitorDataType.Base getDataType()
		{
			if (mDataType == null)
			{
				mDataType = new NSPMonClasses.MDataType_EMFilter((NSPPort)port, filter, NSPMonClasses.MDataType_EMFilter.COUNTER, filter.ingress);
			}
			if (getState() == ONE_SHOT) mDataType.setPeriodic(false);
			if (getState() == PERIODIC)
			{
				mDataType.setPeriodic(true);
				mDataType.setPollingRate(pollingRate);
			}
			return mDataType;
		}
		public MonitorDataType.Base getMonitorDataType()
		{
			NSPMonClasses.MDataType_EMFilter mdt = null;
			//if ((getState() & PERIODIC) > 0)
				//  {
				mdt = new NSPMonClasses.MDataType_EMFilter((NSPPort)port, filter, NSPMonClasses.MDataType_EMFilter.RATE, filter.ingress);

				mdt.setPeriodic(true);
				mdt.setPollingRate(pollingRate);
				if (filter.ingress)
					mdt.setName(new String("IPP" + filter.nspport.getID() + "Filter" + filter.getPluginBinding().toInt() + "BW"));
				else
					mdt.setName(new String("IPP" + filter.nspport.getID() + "Filter" + filter.getPluginBinding().toInt() + "BW"));
				//  }
				return mdt;
		}
		public void setValue(double val)
		{
			double ov = getValue();
			super.setValue(val);
			filter.fireEvent(new FilterDescriptor.Event(filter, FilterDescriptor.Event.STATS_CHANGED, ov, getValue()));
		}
		public void setState(int s)
		{
			super.setState(s);
			String state_str = "";
			switch(s)
			{
			case PortStats.STOP:
				state_str = "Stop";
				break;
			case PortStats.ONE_SHOT:
				state_str = "Once";
				break;
			case PortStats.MONITOR:
				state_str = "Add to Graph";
				break;
			case PortStats.PERIODIC:
				state_str = "Periodic";
				break;
			}
			if (ExpCoordinator.isRecording())
				ExpCoordinator.recordEvent(SessionRecorder.STATS_FILTER, new String[]{filter.nspport.getLabel(), filter.getRecordedString(), state_str});
		}
	}//end class Stats


	public static class ForwardKey extends Route.NextHop
	{
		public static final int USE_ROUTE = -1;
		public static final String USE_ROUTE_STR = "use_route";
		public static final String DISABLED_STR = "x";
		private boolean disabled = false;
		private int bitarray;
		private boolean multicast;
		private boolean mcastable = false;
		private String mcastString;
		public ForwardKey(boolean in)
		{
			this();
			if (in) 
				port = USE_ROUTE;
		}
		public ForwardKey() 
		{ 
			super();
			bitarray = 0x00;
			multicast = false;
			mcastString = "";
		}
		public ForwardKey(String str) throws java.text.ParseException
		{
			super(str);
			//if (str.equals(USE_ROUTE_STR)) port = USE_ROUTE;
			//else parseString(str);
		}
		public ForwardKey(int dp) { super(dp);}
		public ForwardKey(ForwardKey fk) 
		{ 
			this(fk.port, fk.subport);
			bitarray = fk.bitarray;
			multicast = fk.multicast;
			mcastString = new String(fk.mcastString);
		}
		public ForwardKey(int dp, int sp) 
		{ 
			super(dp, sp);
			bitarray = 0x00;
			multicast = false;
			mcastString = "";
		}
		public int getAsInt()
		{
			if (port == USE_ROUTE) return USE_ROUTE;
			else return (super.getAsInt());
		}
		public String toString()
		{
			//ExpCoordinator.print(new String("ForwardKey.toString bitarray = " + bitarray + " multicast = " + multicast + " mcastString= " + mcastString), 4);
			if (multicast)return mcastString;
			if (disabled) return DISABLED_STR;
			else
			{
				if (port == USE_ROUTE) return USE_ROUTE_STR;
				else return (super.toString());
			}      
		}
		public boolean isMCast() { return multicast;}
		public void setDisabled(boolean b) {disabled = b;}
		public void parseString(String s) throws java.text.ParseException
		{
			bitarray = 0x00;
			multicast = false;
			mcastString = "";
			if (s.equals(USE_ROUTE_STR)) port = USE_ROUTE;
			else 
			{
				String tmp_str = s.trim();
				String strarray[] = tmp_str.split(",");
				String strarray2[];
				int len = Array.getLength(strarray);
				bitarray = 0x00;
				int numfound = 0;
				int val_a = -1;
				int val_b = -1;
				int numports = 7;
				for (int i = 0; i < len; ++i)
				{
					tmp_str = strarray[i].trim();
					ExpCoordinator.print(new String("FilterDescriptor.ForwardKey.parseString strarray[" + i + "] = " + tmp_str), 4);
					if (tmp_str.equals(WILDCARD_STRING)) 
					{
						bitarray = 0xff;
						numfound = 2;
						break;
					}
					else
					{
						{
							try
							{
								if (tmp_str.matches("(.+)-(.+)"))
								{
									strarray2 = tmp_str.split("(-|\\s)+");
									int len2 = Array.getLength(strarray2);
									if (len2 == 2)
									{
										val_a = Integer.parseInt(strarray2[0]);
										val_b = Integer.parseInt(strarray2[1]);
									}
									if (val_a < 0 || val_b < 0 || val_a > numports || val_b > numports)
									{
										bitarray = 0x00;
										ExpCoordinator.print("FilterDescriptor.ForwardKey parse error: not valid voq comma separated list, *, 0-7, or range x-y", 0);
										throw new java.text.ParseException("FilterDescriptor.ForwardKey parse error: not valid voq comma separated list, *, 0-7, or range x-y", 0);
									}
									if (val_b < val_a)
									{
										int tmp_val = val_b;
										val_b = val_a;
										val_a = tmp_val;
									}
									for (int j = val_a; j <= val_b; ++j)
									{
										bitarray = bitarray | (0x01 << j);
										++numfound;
									}
									ExpCoordinator.print(new String("FilterDescriptor.ForwardKey val_a = " + val_a + " val_b = " + val_b), 4);
								}
								else
								{
									port = Integer.parseInt(tmp_str);
									bitarray = bitarray | (0x01 << port);
									++numfound;
								}
							}
							catch (NumberFormatException e)
							{
								throw new java.text.ParseException("FilterDescriptor.ForwardKey parse error: not valid voq comma separated list, *, 0-7, or range x-y", 0);
							}
						}
					}
				}
				if (numfound > 1) 
				{
					multicast = true;
					mcastString = new String(s);
					ExpCoordinator.print(new String("FilterDescriptor.ForwardKey bitarray = " + bitarray), 4);
				}
				else super.parseString(s);
			}
		}
		public static int getAsInt(int p, int sp)
		{
			int rtn = (p << 2) | sp;
			return rtn;
		}
		public int getBitArray() { return bitarray;}
	}//ends class ForwardKey

	public static class VOQID extends Queue.VOQID
	{
		public VOQID() 
		{ 
			super();
			setValue(String.valueOf(0));
		}
		public VOQID(Queue.ID voq)
		{
			super(voq);
		}
		public VOQID(Queue.VOQID voq)
		{
			super(voq);
		}
		public VOQID(VOQID voq)
		{
			super((Queue.VOQID)voq);
		}
		public VOQID(String s) throws java.text.ParseException
		{
			super(s);
		}
		protected void parseString(String s, boolean is_voq) throws java.text.ParseException
		{
			if (s.equals(ForwardKey.USE_ROUTE_STR)) setValue(s);
			else super.parseString(s, true);
		}
		public int getIntValue() 
		{ 
			if (toString().equals(ForwardKey.USE_ROUTE_STR)) return ForwardKey.USE_ROUTE;
			return (super.getIntValue());
		}
	}//ends class VOQID

	public static class Protocol
	{
		public static final int UDP = 17;
		public static final int TCP = 6;
		public static final int ICMP = 1;
		public static final String TCP_STRING = "tcp";
		public static final String UDP_STRING = "udp";
		public static final String ICMP_STRING = "icmp";

		private int type = UDP;
		private boolean em_filter = false;

		public Protocol(Protocol p) 
		{
			this(p.type, p.em_filter);
		}
		public Protocol(String s) throws java.text.ParseException { this(s,false);}
		public Protocol(String s, boolean em) throws java.text.ParseException
		{
			if (s.equals(TCP_STRING)) type = TCP;
			else
			{
				if (s.equals(UDP_STRING)) type = UDP;
				else 
				{
					if (s.equals(ICMP_STRING)) type = ICMP;
					else
					{
						if (!em_filter && s.equals(WILDCARD_STRING)) type = WILDCARD;
						else
							type = Integer.parseInt(s);//throw new java.text.ParseException("FD.Protocol parse error: not 'tcp' or 'udp'", 0);
					}
				}
			}
		}
		public Protocol() { this(false);}
		public Protocol(boolean em)
		{
			this(TCP, em);
		}
		public Protocol(int t) { this(t, false);}
		public Protocol(int t, boolean em)
		{
			type = t;
			em_filter = em;
			//if (t == TCP || t == UDP) type = t;
			//else type = UDP;
		}
		public String toString()
		{
			switch (type)
			{
			case TCP:
				return TCP_STRING;
			case UDP:
				return UDP_STRING;
			case ICMP:
				return ICMP_STRING;
			case WILDCARD:
				if (!em_filter) return WILDCARD_STRING;
			default:
				return (String.valueOf(type));
			}
		}
		public int toInt() { return type;}
		public boolean isEqual(Protocol p) { return (p.type == type);}
	}    

	public static class EMProtocol extends Protocol
	{
		public EMProtocol(Protocol p) { this(p.toInt());}
		public EMProtocol(String s) throws java.text.ParseException { super(s, true);}
		public EMProtocol() { super(true);}
		public EMProtocol(int t) { super(t,true);}
	}

	public static class GMProtocol extends Protocol
	{
		public GMProtocol(Protocol p)  { this(p.toInt());}
		public GMProtocol(String s) throws java.text.ParseException { super(s, false);}
		public GMProtocol() { super(false);}
		public GMProtocol(int t) { super(t,false);}
	}

	public static class QueueID extends Queue.ID
	{
		public static final String ASSIGN = "assign";
		public QueueID()
		{
			super();
			setValue(ASSIGN);
		}
		public QueueID(String s) throws java.text.ParseException
		{
			super(s);
		}
		public QueueID(Queue.ID id) { super(id);}
		public QueueID(QueueID id)
		{
			super((Queue.ID)id);
		}
		protected void parseString(String s, boolean is_voq) throws java.text.ParseException
		{
			//ExpCoordinator.printer.print("FD.QueueID.parseString " + s);
			if (s.equals(ASSIGN)) setValue(s);
			else     
			{
				try
				{
					int q = Integer.parseInt(s);

					if (q == 0) setValue(ASSIGN);
					else
					{
						setValue(s);
						if (q < QueueTable.SPC_OUT_START || q >= QueueTable.DATAGRAM_START)
							throw new java.text.ParseException("Filter.QueueID parse error: not valid queue id must be 136-503 or 0 for auto-assignment", 0);
					}
				}
				catch (java.lang.NumberFormatException e)
				{
					throw new java.text.ParseException("Filter.QueueID parse error: not valid queue id must be 136-503 or 0 for auto-assignment", 0);
				}
			}
		}
		public int getIntValue() 
		{	
			if (equals(ASSIGN))
				return (Queue.DEFAULT);
			else
				return (getIntValue(false));
		}
	}

	public static class PluginBinding
	{
		public static final int PLUGIN = 1;
		public static final int NOPLUGIN = 0;
		public static final String NOPLUGIN_STRING = "no plugin";
		public static final String PLUGIN_STRING = "plugin";

		private int type = NOPLUGIN;

		public PluginBinding(PluginBinding p) 
		{
			this(p.type);
		}
		public PluginBinding(String s) throws java.text.ParseException
		{
			if (s.equals(PLUGIN_STRING)) type = PLUGIN;
			else
			{
				if (s.equals(NOPLUGIN_STRING)) type = NOPLUGIN;
				else 
				{
					type = Integer.parseInt(s);
					if (type < 8 || type > 127)
						throw new java.text.ParseException("FD.PluginBinding parse error: not a valid PBinding must be 8-127 or no plugin", 0);
				}
			}
		}
		public PluginBinding()
		{
			this(NOPLUGIN);
		}
		public PluginBinding(int t)
		{
			this.setPBinding(t);
		}
		public String toString()
		{
			switch (type)
			{
			case PLUGIN:
				return PLUGIN_STRING;
			case NOPLUGIN:
				return NOPLUGIN_STRING;
			default:
				return String.valueOf(type);
			}
		}
		public int toInt() { return type;}
		public boolean isEqual(int t) { return (t == type);}
		public boolean isEqual(PluginBinding p) { return (p.type == type);}
		public void setPBinding(PluginBinding p) { type = p.type;}
		public void setPBinding(int t) 
		{ 
			type = t;
		}
		public boolean isPlugin() { return (type != NOPLUGIN);}
	}    

	public static class PluginPBinding extends PluginBinding
	{
		public PluginPBinding()
		{
			super(PluginBinding.PLUGIN);
		}
	}


	public static class NoPluginPBinding extends PluginBinding
	{
		public NoPluginPBinding()
		{
			super(PluginBinding.NOPLUGIN);
		}
	}

	public static class PortRange
	{
		private int begin = 0;
		private int end = 0;
		private boolean em_filter = false;

		public PortRange(PortRange pr) { this(pr, pr.em_filter);}
		public PortRange(PortRange pr, boolean em) 
		{
			this(pr.begin, pr.end, em);
		}
		public PortRange(boolean em) {em_filter = em;}
		public PortRange(int b, int e, boolean em)
		{
			this(em);
			begin = b;
			end = e;
		}
		public PortRange(String st) throws java.text.ParseException
		{
			this(st, false);
		}
		protected PortRange(String st, boolean isem) throws java.text.ParseException
		{
			this(isem);
			if (em_filter)
			{
				try
				{
					begin = Integer.parseInt(st);
					if (begin < 0)
						throw new java.text.ParseException("PortRange parse error: port not integer", 0);
					else end = 0;
				}
				catch (NumberFormatException e)
				{
					throw new java.text.ParseException("PortRange parse error: port not integer", 0);
				}
			}
			else 
				parseString(st);
		}
		protected void parseString(String st) throws java.text.ParseException
		{
			String[] strarray = st.split("-"); 
			int len = Array.getLength(strarray);
			if (len == 1 || len == 2)
			{
				try
				{
					if (strarray[0].equals(WILDCARD_STRING)) 
					{
						begin = 0;
						end = 0;
					}
					else
					{
						begin = Integer.parseInt(strarray[0]);
						if (len == 2) end = Integer.parseInt(strarray[1]);
						else end = 0;
					}
				}
				catch (NumberFormatException e)
				{
					throw new java.text.ParseException("PortRange parse error: port not integer", 0);
				}
			}
			else throw new java.text.ParseException("PortRange parse error: not of form port-port or port", 0);
			if (begin < 0 || end < 0)  throw new java.text.ParseException("PortRange parse error: ports should be positive", 0);
		}

		public int getBegin() { return begin;}
		public int getEnd() { return end;}
		public String toString() 
		{
			if (!em_filter && end == 0 && begin == 0) return WILDCARD_STRING;
			if (end > 0 && !em_filter) return (new String(begin + "-" + end));
			else return (String.valueOf(begin));
		}

		public boolean isEqual(PortRange pr)
		{
			return (pr.begin == begin && pr.end == end);
		}
	}

	public static class EMPort extends PortRange
	{
		public EMPort() { super(true);}
		public EMPort(PortRange p) { super(p, true);}
		public EMPort(String st) throws java.text.ParseException { super(st, true);}
	}

	public static class GMPort extends PortRange
	{
		public GMPort() { super(false);}
		public GMPort(PortRange p) { super(p, false);}
		public GMPort(String st) throws java.text.ParseException { super(st, false);}
	}

	public static class PrefixMask extends Route.PrefixMask
	{
		private boolean em_filter = false;
		public PrefixMask() { this(false);}
		public PrefixMask( boolean em)
		{
			super();
			em_filter = em;
		}
		public PrefixMask(PrefixMask pf) { this(pf, false);}
		public PrefixMask(PrefixMask pf, boolean em)
		{
			super((Route.PrefixMask)pf);
			em_filter = em;
		}
		public PrefixMask(String str)  throws java.text.ParseException
		{
			this(str, false);
		}
		public PrefixMask(String str, boolean em)  throws java.text.ParseException
		{
			em_filter = em;
			String[] strarray = str.split("/"); //need new java to use this
			int len = Array.getLength(strarray);
			if (len != 1 && len != 2) 
			{
				System.err.println("FD.PrefixMask str = " + str + " len = " + len);
				throw new java.text.ParseException("FilterDescriptor.PrefixMask error: not of form x.x.x.x/mask or x.x.x.x", 0);
			}
			if (len == 1)
				parseString(str, 32);
			else parseString(str);
		}
		public String toString()
		{
			if (em_filter) return (getPrefix());
			else return (super.toString());
		}
	}

	public static class EMPrefix extends PrefixMask
	{
		public EMPrefix() { super(true);}
		public EMPrefix(FilterDescriptor.PrefixMask pf) { super((FilterDescriptor.PrefixMask)pf, true);}
		public EMPrefix(String st)  throws java.text.ParseException { super(st, true); }
	}

	public static class GMPrefix extends PrefixMask
	{
		public GMPrefix() { super(false);}
		public GMPrefix(FilterDescriptor.PrefixMask pf) { super((FilterDescriptor.PrefixMask)pf, false);}
		public GMPrefix(String st)  throws java.text.ParseException { super(st, false); }
	}

	public static class REND_Marker extends REND_Marker_class
	{
		public REND_Marker(int op, NSPPort p, FilterDescriptor fid, boolean in, int ndx)
		{
			super(p.getID(), in, op);
			field3 = fid.srcAddr.getPrefixBytes();
			field1 = fid.srcPortRange.getBegin();
			field4 = fid.destAddr.getPrefixBytes();
			field2 = fid.destPortRange.getBegin();
			index = ndx;
		}
		public REND_Marker(int op, NSPPort p, FilterDescriptor fid, boolean in)
		{
			super(p.getID(), in, op);
			field3 = fid.srcAddr.getPrefixBytes();
			field1 = fid.srcPortRange.getBegin();
			field4 = fid.destAddr.getPrefixBytes();
			field2 = fid.destPortRange.getBegin();
			index = fid.protocol.toInt();
		}
	}//class REND_Marker

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////// NCCP_FilterResponse /////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class NCCP_FilterResponse extends NCCP.ResponseBase
	{
		private double pluginBinding = 0;
		public NCCP_FilterResponse ()//FilterDescriptor fd)
		{
			super(2);
			//filter = fd;
		}
		public void retrieveFieldData(DataInput din) throws IOException
		{	
			pluginBinding = din.readUnsignedShort();
			//filter.setQID(new PluginBinding(din.readUnsignedShort()));
		}
		public double getData(MonitorDataType.Base mdt)
		{
			return pluginBinding;
		}
		public double getPBinding() { return pluginBinding;}
		public String getString() { return "Filter Op";}
	}// class NCCP_FilterResponse

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// NCCP_FilterRequester ///////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class NCCP_FilterRequester extends NCCP.RequesterBase
	{
		private FilterDescriptor filter;
		private NSPPort nspport;
		private boolean ingress = true;
		private String fdSent = null;

		public NCCP_FilterRequester(int op, NSPPort p, FilterDescriptor r, boolean in)
		{
			super(op, true);
			filter = r;
			nspport = p;
			setMarker(new REND_Marker_class());
			ingress = in;
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			FilterDescriptor tmp_filter = filter;
			if (getOp() == NCCP.Operation_DeleteEMFilter)
			{
				filter.getStats().reset();
				try
				{
					tmp_filter = new FilterDescriptor(nspport, ingress, filter.properties.getProperty(COMMITTED_FD));
				}
				catch (java.text.ParseException e){}
			}

			if (getOp() == NCCP.Operation_AddEMFilter)
			{
				filter.properties.setProperty(COMMITTED, true);
				filter.properties.setProperty(COMMITTED_FD, filter.toString());
				++filter.numCommits;
				ExpCoordinator.print(new String("NCCP_FilterRequester::storeData add filter for port:" + nspport.getID() + " ingress " + ingress + " " + tmp_filter.toString()), 5);
				filter.ftable.fireCommitEvent(filter);
			}
			else 
				ExpCoordinator.print(new String("NCCP_FilterRequester::storeData delete filter for port:" + nspport.getID() + " ingress " + ingress + " " + tmp_filter.toString()), 5);
			fdSent = filter.properties.getProperty(COMMITTED_FD);
			dout.writeShort(nspport.getID());
			if (ingress) dout.writeByte(1);
			else dout.writeByte(0);
			//write src address, range of ports, mask in 4 byte form e.g. 32 == 0xffffffff
			dout.write(tmp_filter.srcAddr.getPrefixBytes(), 0, 4);
			if (tmp_filter.forwardKey.isMCast()) 
				dout.writeInt(0);
			else dout.writeInt((int)tmp_filter.srcAddr.getMaskBytes());
			dout.writeShort(tmp_filter.srcPortRange.getBegin());
			dout.writeShort(tmp_filter.srcPortRange.getEnd());
			//write dest address, range of ports, mask in 4 byte form e.g. 32 == 0xffffffff
			dout.write(tmp_filter.destAddr.getPrefixBytes(), 0, 4);
			dout.writeInt((int)tmp_filter.destAddr.getMaskBytes());
			dout.writeShort(tmp_filter.destPortRange.getBegin());
			dout.writeShort(tmp_filter.destPortRange.getEnd());
			dout.writeShort(tmp_filter.protocol.toInt());
			if (tmp_filter.negation) dout.writeByte(1);
			else dout.writeByte(0);
			if (tmp_filter.drop) dout.writeByte(1);
			else dout.writeByte(0);
			dout.writeInt(tmp_filter.getStats().getIndex());
			dout.writeByte(tmp_filter.getSamplingType().getType());
			dout.writeByte(tmp_filter.getTCPFlags());
			dout.writeByte(tmp_filter.getTCPFlagsMask());
			int fk = tmp_filter.forwardKey.getAsInt();
			if (!ingress)
				fk = ForwardKey.getAsInt(nspport.getID(), nspport.getPropertyInt(NSPPort.SUBPORT));
			ExpCoordinator.print(new String("    writing forward key = " + fk), 5);
			dout.writeInt(fk);
			//if spc bound, ingress or want msrc to assign qid send the pluginBinding otw send the qid
			if (ingress || tmp_filter.pluginBinding.isPlugin() || tmp_filter.qid.isDefault()) 
			{
				if (tmp_filter.forwardKey.isMCast()) 
					dout.writeShort(tmp_filter.forwardKey.getBitArray());
				else
					dout.writeShort(tmp_filter.pluginBinding.toInt());
			} 
			else 
				dout.writeShort(tmp_filter.qid.getIntValue());
			dout.writeShort(tmp_filter.priority);
			if (tmp_filter.forwardKey.isMCast()) 
				dout.writeShort(MCAST);
			else
				dout.writeShort(tmp_filter.getType());
		}

		public String getFDSent() { return fdSent;}
		public FilterDescriptor getFilter() { return filter;}
	} //class NCCP_FilterRequester

	public static class NCCP_DisableRequester extends NCCP_FilterRequester
	{
		public NCCP_DisableRequester(NSPPort p, FilterDescriptor r, boolean in)
		{
			super(NCCP.Operation_DeleteEMFilter, p, r, in);
		}
	}//class NCCP_DisableRequester

	public static class AddEdit extends Experiment.AddEdit implements NCCPOpManager.Operation
	{
		protected Vector filters = null;
		protected FilterTable filterTable = null;
		protected static final String ClassName = "FilterDescriptor$AddEdit";
		private boolean ingress = true;
		private Vector requests = null;
		private Vector cancelled = null;
		private boolean update = false;
		private boolean deleteFailed = false;

		public AddEdit(NSPPort c, boolean in, FilterDescriptor r, FilterTable rt, Experiment exp)
		{
			this(c, in, new Vector(), rt, exp);
			filters.add(r);
		}
		public AddEdit(NSPPort c, boolean in, Vector r, FilterTable rt, Experiment exp)
		{
			super(c, exp);
			filterTable = rt;
			filters = r;
			ingress = in;
			requests = new Vector();
			cancelled = new Vector();
			//ExpCoordinator.printer.print("FilterDescriptor.AddEdit " + getClass().getName());
		}
		public void undo() throws CannotUndoException
		{
			//super.undo();
			if (!update) 
			{
				int max = filters.size();
				FilterDescriptor filter = null;
				for (int i = 0; i < max; ++i)
				{ 
					filter = (FilterDescriptor)filters.elementAt(i);
					filterTable.removeFilter(filter);
				}
			}
			if (filterTable.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(filterTable);
		}
		public void redo() throws CannotRedoException
		{
			//super.redo();
			if (!update) 
			{
				int max = filters.size();
				for (int i = 0; i < max; ++i)
				{
					filterTable.addFilter((FilterDescriptor)filters.elementAt(i));
				}
			}
			if ((getExperiment() != null) && !(getExperiment().containsParam(filterTable))) getExperiment().addParam(filterTable);	
		}
		protected void sendMessage() 
		{
			MonitorDaemon ec = (MonitorDaemon)((NSPPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);
			ExpCoordinator.printer.print(new String("AddFilter adding filters to daemon " + ec.toString() + ":"), 1);

			if (ec != null) 
			{
				int max = filters.size();
				boolean req_added = false;
				FilterDescriptor filter;
				boolean filter_change = false;
				boolean oldEnable;
				for (int i = 0; i < max; ++i)
				{
					filter = (FilterDescriptor)filters.elementAt(i);
					filter_change = filter.needsCommit();
					if (filter.isEnabled())
					{
						oldEnable = filter.properties.getPropertyBool(ENABLED);
						if (!oldEnable) filter.properties.setProperty(ENABLED, true);
						if (filter_change)
						{
							if (filter.properties.getProperty(COMMITTED_FD) != null && oldEnable) 
							{
								requests.add(new NCCP_FilterRequester(NCCP.Operation_DeleteEMFilter, (NSPPort)getONLComponent(), filter, ingress));
								if (filter.getStats().getState() == PortStats.PERIODIC) 
								{
									filter.getStats().stopMonitoring();
									filter.getStats().setState(PortStats.STOP); //stop stats if they're running
								}
							}
							ExpCoordinator.printer.print(new String("    " + filter.toString()), 1);
							requests.add(new NCCP_FilterRequester(NCCP.Operation_AddEMFilter, (NSPPort)getONLComponent(), filter, ingress));
							//ec.addMonitorEntry(NCCP_FilterRequester(opcode, (NSPPort)getONLComponent(), filter, ingress));
							req_added = true;
						}
					}
					else
					{
						if (filter.properties.getPropertyBool(ENABLED))//just disabled the filter
						{
							if (filter.properties.getProperty(COMMITTED_FD) != null)  //have committed
							{
								requests.add(new NCCP_DisableRequester((NSPPort)getONLComponent(), filter, ingress));
								ExpCoordinator.printer.print(new String("    " + filter.toString() + " disabled"), 1);
								req_added = true;
							}
							if (filter.getStats().getState() == PortStats.PERIODIC) 
							{
								filter.getStats().stopMonitoring();
								filter.getStats().setState(PortStats.STOP); //stop stats if they're running
							}
							filter.properties.setProperty(ENABLED, false);
						}
					}

					filter.resetUpdateEdit(filterTable);
				}
				if (req_added) filterTable.addOperation(this);
			}
			else System.err.println("AddFilter no daemon");
		}
		protected void sendUndoMessage() {}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (un instanceof FilterDescriptor.DeleteEdit)//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
			{
				FilterDescriptor.DeleteEdit edit = (FilterDescriptor.DeleteEdit) un;
				if (onlComponent == edit.getONLComponent())
				{
					int max = filters.size();
					Vector removals = new Vector();
					int i;
					for (i = 0; i < max; ++i)
					{
						FilterDescriptor elem = (FilterDescriptor)filters.elementAt(i);
						if (edit.contains(elem))
							removals.add(elem);
					}
					max = removals.size();
					for (i = 0; i < max; ++i)
					{
						filters.remove(removals.elementAt(i));
						cancelled.add(removals.elementAt(i));
					}
				}
				if (filters.isEmpty()) return true;
			}
			return false;
		}
		protected static boolean isFAddEdit(ONLComponent.Undoable un)
		{
			String nm = un.getClass().getName();
			return (nm.equals(ClassName));
		}
		public Vector getFilters() { return filters;}
		public boolean contains(FilterDescriptor r) 
		{ 
			return (filters.contains(r) || cancelled.contains(r));
		}
		//NCCPOpManager.Operation
		public void opSucceeded(NCCP.ResponseBase resp)
		{
			NCCP_FilterRequester req = getRequest(resp.getRendMarker());
			FilterDescriptor filter = req.getFilter();
			if (req != null) 
			{
				if (resp.getOp() == NCCP.Operation_AddEMFilter && deleteFailed)
				{
					try
					{
						filter = new FilterDescriptor((NSPPort)onlComponent, ingress, req.getFDSent());
						filter.properties.setProperty(COMMITTED_FD, req.getFDSent());
						filter.resetUpdateEdit(filterTable);
						filterTable.addFilter(filter);
					}
					catch(java.text.ParseException e) {}
				}
				int ftag = (int)((NCCP_FilterResponse)resp).getPBinding();
				if (ftag < SPC_FLOW) filter.setPluginBinding(new PluginBinding(ftag));
				if  (!filter.ingress)
				{
					if (ftag < SPC_FLOW) filter.setQID(ftag + SPC_FLOW);
					else filter.setQID(ftag);
				}
				else
				{
					if (filter.forwardKey.getPort() != ForwardKey.USE_ROUTE)
						filter.setQID(QueueTable.VOQ_START + filter.forwardKey.getPort());
				}
				if (filter.numCommits < 2)  filter.properties.setProperty(COMMITTED_FD, filter.toString());
				requests.remove(req);
				if (req instanceof NCCP_DisableRequester) 
					ExpCoordinator.print(new String("FilterDescriptor.AddEdit.opSucceeded disable for filter " + req.getFDSent() + " port " + ((NSPPort)onlComponent).getID()), 2);
				else
					ExpCoordinator.print(new String("FilterDescriptor.AddEdit.opSucceeded for filter " + req.getFDSent() + " port " + ((NSPPort)onlComponent).getID()), 2);
			}
			if (requests.isEmpty()) filterTable.removeOperation(this);
		}
		public void opFailed(NCCP.ResponseBase resp)
		{
			NCCP_FilterRequester req = getRequest(resp.getRendMarker());
			FilterDescriptor filter = req.getFilter();
			if (req != null) 
			{
				System.err.println("FilterDescriptor.AddEdit.opFailed for filter " + req.getFDSent() + " port " + ((NSPPort)onlComponent).getID());
				ExpCoordinator ec = filter.nspport.getExpCoordinator();
				if (resp.getOp() == NCCP.Operation_AddEMFilter)
				{
					int port = filter.nspport.getID();
					if (!deleteFailed)
					{
						ec.addError(new StatusDisplay.Error((new String("Filter Operation Failure on port " + port + " for filter " + filter.toString() + " failed to update")),
								(new String("Filter Operation Failure on port "+ port)),
								"Filter Operation Failure.",
								(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));          
						if (req.getFDSent().equals(filter.toString()))
						{
							filter.properties.remove(COMMITTED_FD);//setProperty(COMMITTED_FD, null);
							FilterDescriptor.UpdateEdit edit = (FilterDescriptor.UpdateEdit)filter.getUpdateEdit();
							if (!ec.isCurrentEdit(edit)) 
							{
								edit.resetUpdate();
								ec.addEdit(edit);
							}
							else edit.setLatest();
						}
						else //we've done subsequent changes so it's going to be recommitted anyway
						{
							if(req.getFDSent().equals(filter.properties.getProperty(COMMITTED_FD))) 
								filter.properties.remove(COMMITTED_FD);//setProperty(COMMITTED_FD, null);
						}
					}
					else //delete also failed so no change was made
					{
						ec.addError(new StatusDisplay.Error((new String("Filter Operation Failure on port " + port + " for filter " + filter.toString() + " failed to update")),
								(new String("Filter Operation Failure on port "+ port)),
								"Filter Operation Failure.",
								(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));          
					}
				}
				if (resp.getOp() == NCCP.Operation_DeleteEMFilter) 
				{
					if (filter.properties.getProperty(COMMITTED_FD).equals(filter.toString()))
					{
						try
						{
							filter.parseString(req.getFDSent());
						}
						catch (java.text.ParseException e) {}
						if ((req instanceof NCCP_DisableRequester) && !filter.isEnabled()) filter.setEnabled(true);
					}
					filter.properties.setProperty(COMMITTED_FD, req.getFDSent());
					deleteFailed = true;
					filterTable.changeFilter(filter);
				}
				requests.remove(req);
			}
			if (requests.isEmpty()) filterTable.removeOperation(this);
		}
		public Vector getRequests() { return requests;}
		public boolean containsRequest(REND_Marker_class rend)
		{
			if (getRequest(rend) != null) return true;
			return false;
		}
		//end NCCPOpManager.Operation
		private NCCP_FilterRequester getRequest(REND_Marker_class rend)
		{
			int max = requests.size();
			NCCP_FilterRequester req = null;
			//ExpCoordinator.printer.print("FilterDescriptor.AddEdit numReqs = " + max + " looking for");
			//rend.print();
			for (int i = 0; i < max; ++i)
			{
				req = (NCCP_FilterRequester)requests.elementAt(i);
				//ExpCoordinator.printer.print("  elem " + i);
				//req.getMarker().print();
				if (req.getMarker().isEqual(rend)) return req;
			}
			return null;
		}
		protected void setUpdate(boolean b) { update = b;}
	}//inner class AddEdit

	public static class UpdateEdit extends AddEdit
	{
		private String latest = null;
		private String old = null;
		private boolean latestEnable = false;
		private boolean oldEnable = false;

		public UpdateEdit(NSPPort c, boolean in, FilterDescriptor f, FilterTable ft, Experiment exp)
		{
			super(c, in, f, ft, exp);
			setUpdate(true);
			latest = f.toString();
			old = f.toString();
			latestEnable = f.isEnabled();
			oldEnable = latestEnable;
		}
		public void undo() throws CannotUndoException
		{
			int max = filters.size();
			FilterDescriptor filter = (FilterDescriptor)filters.elementAt(0);	
			try
			{
				//filter.parseString(filter.properties.getProperty(old));
				filter.parseString(old);
			}
			catch (java.text.ParseException e) {}
			filter.setEnabled(oldEnable);
			super.undo();
		}
		public void redo() throws CannotRedoException
		{
			super.redo();
			FilterDescriptor filter = (FilterDescriptor)filters.elementAt(0);
			try
			{
				filter.parseString(latest);
			}
			catch (java.text.ParseException e) {}
			filter.setEnabled(latestEnable);
		}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (super.isCancelled(un)) return true;
			else 
			{
				if (FilterDescriptor.AddEdit.isFAddEdit(un))
				{
					FilterDescriptor.AddEdit edit = (FilterDescriptor.AddEdit) un;
					if (edit.contains((FilterDescriptor)filters.elementAt(0))) return true;
				}
			}
			return false;
		}
		public void resetUpdate()
		{
			FilterDescriptor filter = (FilterDescriptor)filters.elementAt(0);
			latest = filter.toString();
			old = filter.toString();
			latestEnable = filter.isEnabled();
			oldEnable = latestEnable;
		}
		public void setLatest()
		{
			FilterDescriptor filter = (FilterDescriptor)filters.elementAt(0);
			latest = filter.toString();
			latestEnable = filter.isEnabled();
		}
	}//end inner class UpdateEdit

	//to make Delete Edit work for a multiple selection need to create a compound edit with several
	//Delete Edits and then do something about the cancellation. For now just limit to single selection
	//and single delete

	public static class DeleteEdit extends Experiment.DeleteEdit implements NCCPOpManager.Operation
	{
		private Vector filters = null;
		private Vector cancelled = null;
		private Vector requests = null;
		private FilterTable filterTable = null;
		protected static final String ClassName = "FilterDescriptor$DeleteEdit";
		private boolean ingress = true;

		public DeleteEdit(NSPPort c, boolean in, Vector r, FilterTable rt, Experiment exp)
		{
			super(c, exp);
			filterTable = rt;
			filters = r;
			ingress = in;
			cancelled = new Vector();
			requests = new Vector();
		}
		public void undo() throws CannotUndoException
		{
			//super.undo();
			int numFilters = filters.size();
			for (int i = 0; i < numFilters; ++i)
				filterTable.addFilter((FilterDescriptor)filters.elementAt(i));
			if ((getExperiment() != null) && !(getExperiment().containsParam(filterTable))) getExperiment().addParam(filterTable);
		}
		public void redo() throws CannotRedoException
		{
			//super.redo();
			int numFilters = filters.size();
			for (int i = 0; i < numFilters; ++i)
				filterTable.removeFilter((FilterDescriptor)filters.elementAt(i));
			if (filterTable.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(filterTable);
		}
		protected void sendMessage() 
		{
			ONLDaemon ec = ((NSPPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);
			FilterDescriptor filter;
			if (ec != null)
			{
				if (!ec.isConnected()) ec.connect();
				int numFilters = filters.size();
				for (int i = 0; i < numFilters; ++i)
				{
					filter = (FilterDescriptor)filters.elementAt(i);
					if (filter != null)
					{
						ExpCoordinator.printer.print(new String("DeleteFilter " + filter.toString() + " daemon " + ec.toString()), 1);
						if (filter.getStats().getState() == PortStats.PERIODIC) 
						{
							filter.getStats().stopMonitoring();
							filter.getStats().setState(PortStats.STOP); //stop stats if they're running
						}

						requests.add(new NCCP_FilterRequester(NCCP.Operation_DeleteEMFilter, (NSPPort)getONLComponent(), filter, ingress));
					}
					if (numFilters > 0) filterTable.addOperation(this);
				}
			}
			else System.err.println("DeleteFilter no daemon");
		}
		protected void sendUndoMessage() {}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (FilterDescriptor.AddEdit.isFAddEdit(un))//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
			{
				FilterDescriptor.AddEdit edit = (FilterDescriptor.AddEdit) un;
				if (onlComponent == edit.onlComponent) 
				{
					int max = filters.size();
					Vector removals = new Vector();
					int i;
					FilterDescriptor elem;
					for (i = 0; i < max; ++i)
					{
						elem = (FilterDescriptor)filters.elementAt(i);
						if (edit.contains(elem))
							removals.add(elem);
					}
					max = removals.size();
					for (i = 0; i < max; ++i)
					{
						elem = (FilterDescriptor)removals.elementAt(i);
						filters.remove(elem);
						cancelled.add(elem);
					}
				}
				if (filters.isEmpty()) return true;
			}
			return false;
		}
		protected static boolean isFDeleteEdit(ONLComponent.Undoable un)
		{
			String nm = un.getClass().getName();
			return (nm.equals(ClassName));
		}
		public boolean contains(FilterDescriptor r) 
		{ 
			return (filters.contains(r) || cancelled.contains(r));
		}
		//NCCPOpManager.Operation
		public void opSucceeded(NCCP.ResponseBase resp)
		{
			NCCP_FilterRequester req = getRequest(resp.getRendMarker());
			if (req != null)
			{
				System.err.println("Filter.DeleteEdit.opSucceeded for filter " + req.getFDSent() + " port " + filterTable.getPort().
						getID());
				requests.remove(req);
			}
			if (requests.isEmpty()) filterTable.removeOperation(this);
		}
		public void opFailed(NCCP.ResponseBase resp)
		{
			NCCP_FilterRequester req = getRequest(resp.getRendMarker());
			if (req != null)
			{
				System.err.println("Filter.DeleteEdit.opFailed for filter " + req.getFDSent() + " port " + filterTable.getPort().getID());
				filterTable.addFilter(req.getFilter());
				requests.remove(req);
			}
			if (requests.isEmpty()) filterTable.removeOperation(this);
		}
		public Vector getRequests() { return requests;}
		public boolean containsRequest(REND_Marker_class rend)
		{
			if (getRequest(rend) != null) return true;
			return false;
		}
		//end NCCPOpManager.Operation
		private NCCP_FilterRequester getRequest(REND_Marker_class rend)
		{
			int max = requests.size();
			NCCP_FilterRequester req = null;
			for (int i = 0; i < max; ++i)
			{
				req = (NCCP_FilterRequester)requests.elementAt(i);
				if (req.getMarker().isEqual(rend)) return req;
			}
			return null;
		}
	}

	public FilterDescriptor(NSPPort p, boolean in, String str) throws java.text.ParseException //form type srcx.x.x.x/mask srcportrange destx.x.x.x/mask destportrange protocl pluginBinding forwardkey (priority) -- priority is for GM filters
	{
		expCoordinator = p.getExpCoordinator();
		properties = new ONLPropertyList(this);
		listeners = new EventListenerList();
		stats = new Stats(p, this, in);
		nspport = p;
		ingress = in;

		properties.setProperty(FILTER_EDITABLE, true);
		properties.setProperty(COMMITTED, false);
		setFTable();
		parseString(str);
	}
	public void parseString(String str) throws java.text.ParseException //form type srcx.x.x.x/mask srcportrange destx.x.x.x/mask destportrange protocl pluginBinding forwardkey (priority) -- priority is for GM filters
	{
		if (str == null) 
		{
			initializeEmpty();
			return;
		}
		String strarray[] = str.split(" ");
		int len = Array.getLength(strarray);
		ExpCoordinator.printer.print(new String("FD.parseString " + str + " length = " + len), 3);
		int tp = Integer.parseInt(strarray[0]);
		setType(tp);
		int num_left = len - 1;
		if (len <= 0) 
		{
			initializeEmpty();
			return;
		}

		if (tp != EXACT_MATCH) //GM
		{
			if (len < 9)
			{
				throw new java.text.ParseException("FilterDescriptor error: not of form srcaddr/mask srcport destaddr/mask destport protocol pluginBinding forwardkey priority negation drop", 0);
			}
			srcAddr = new GMPrefix(strarray[1]);
			srcPortRange = new GMPort(strarray[2]);
			destAddr = new GMPrefix(strarray[3]);
			destPortRange = new GMPort(strarray[4]);
			protocol = new GMProtocol(strarray[5]);
			properties.setProperty(PRIORITY_SET, false); //priority is set per filter
			//ExpCoordinator.printer.print("FD GM filter priority " + strarray[8]);
			priority = Integer.parseInt(strarray[8]);
			//setPriority(Integer.parseInt(strarray[8]));
			num_left -= 6;
			if (len > 10) 
			{
				--num_left;
				negation = Boolean.valueOf(strarray[9]).booleanValue();
			}
		}
		else //EM
		{
			if (len < 8)
			{
				throw new java.text.ParseException("FilterDescriptor error: not of form srcaddr srcport destaddr destport protocol pluginBinding forwardkey drop", 0);
			}
			srcAddr = new EMPrefix(strarray[1]);
			srcPortRange = new EMPort(strarray[2]);
			destAddr = new EMPrefix(strarray[3]);
			destPortRange = new EMPort(strarray[4]);
			properties.setProperty(PRIORITY_SET, true); //priority is set per table
			protocol = new EMProtocol(strarray[5]);
			num_left -= 5;
		}
		forwardKey = new ForwardKey(strarray[7]);
		setType(tp);
		initializeQid(strarray[6]);
		num_left -= 2;
		if (num_left > 0) 
		{
			drop = Boolean.valueOf(strarray[len-num_left]).booleanValue();
			--num_left;
		}
		//STATS_MODULE
		if (num_left > 0) 
		{
			PortStats.Index tmp_ndx = new PortStats.Index(strarray[len-num_left]);
			stats.setIndex(tmp_ndx.getValue());
			--num_left;
		}
		if (num_left > 0) 
		{
			if (ftable != null) samplingType = ftable.getSamplingType(Integer.parseInt(strarray[len-num_left]));
			else samplingType = new RouterPort.SamplingType(0, Integer.parseInt(strarray[len-num_left]));
			--num_left;
		}
		else 
		{
			if (samplingType == null && ftable != null && tp != EXACT_MATCH) 
				samplingType = ftable.getSamplingType(RouterPort.SamplingType.ALL);
			else samplingType = new RouterPort.SamplingType(100, RouterPort.SamplingType.ALL);
		}

		if (num_left > 0) 
		{
			tcpflags = Integer.parseInt(strarray[len-num_left]);
			--num_left;
			tcpflagsMask = Integer.parseInt(strarray[len-num_left]);
			--num_left;
			ExpCoordinator.print(new String(" tcp flags found " + tcpflags + ":"  + tcpflagsMask), 3);
		}
		else ExpCoordinator.print(" no tcp flags found", 3);
	}

	private void initializeQid(String str) throws java.text.ParseException 
	{
		if (qid == null)
		{
			if (ingress) 
			{
				qid = new FilterDescriptor.VOQID();
			}
			else qid = new QueueID();
		}
		//ExpCoordinator.printer.print("FD.initializeQID " + str);
		int tmp = Integer.parseInt(str);
		if (ingress) //value is going to be equivalent to plugin binding
		{
			pluginBinding = new PluginBinding(tmp);
			qid.setValue(forwardKey.toString());
		}
		else //value is plugin binding if spc bound and qid if fpx bound
		{
			if (tmp == PluginBinding.NOPLUGIN || tmp == PluginBinding.PLUGIN)
			{
				qid.setValue(QueueID.ASSIGN);
				pluginBinding = new PluginBinding(tmp);
			}
			else
			{
				tmp = Integer.parseInt(str);
				if (tmp < QueueTable.FPX_START) 
				{
					pluginBinding = new PluginBinding(tmp - SPC_FLOW);
					qid.setValue(String.valueOf(tmp));
				}
				else
				{
					qid.setValue(String.valueOf(tmp));
					pluginBinding = new PluginBinding(PluginBinding.NOPLUGIN);
				}
			}
		}
	}
	public FilterDescriptor(NSPPort p, boolean in)
	{
		this(EXCL_GEN_MATCH, p, in);
	}

	public FilterDescriptor(String uri, Attributes attributes, NSPPort p)
	{
		this(Integer.parseInt(attributes.getValue(uri, ExperimentXML.FILTER_TYPE)), p, Boolean.valueOf(attributes.getValue(uri, ExperimentXML.INGRESS)).booleanValue());
	}
	public FilterDescriptor(int tp, NSPPort p, boolean in)
	{
		expCoordinator = p.getExpCoordinator();
		properties = new ONLPropertyList(this);
		listeners = new EventListenerList();
		stats = new Stats(p, this, in);
		nspport = p;
		nsp = (NSPDescriptor)p.getParent();
		ingress = in;

		properties.setProperty(FILTER_EDITABLE, true);
		properties.setProperty(COMMITTED, false);
		properties.setProperty(ENABLED, true);
		setType(tp);
		initializeEmpty();
	}
	public FilterDescriptor(FilterDescriptor.PrefixMask sa, PortRange sp, FilterDescriptor.PrefixMask da, PortRange dp, Protocol p, int tp, NSPPort prt, boolean in)
	{
		this(tp, prt, in);
		setFilter(sa, sp, da, dp, p);
	}

	public void initializeEmpty()
	{
		int tp = getType();
		if (tp != EXACT_MATCH) //GM
		{
			srcAddr = new GMPrefix();
			srcPortRange = new GMPort();
			destAddr = new GMPrefix();
			destPortRange = new GMPort();
			properties.setProperty(PRIORITY_SET, false); //priority is set per filter
			protocol = new GMProtocol();
		}
		else //EM
		{
			srcAddr = new EMPrefix();
			srcPortRange = new EMPort();
			destAddr = new EMPrefix();
			destPortRange = new EMPort();
			properties.setProperty(PRIORITY_SET, true); //priority is set per table
			protocol = new EMProtocol();
		}
		pluginBinding = new PluginBinding();
		forwardKey = new ForwardKey(ingress);
		if (ingress) qid = new FilterDescriptor.VOQID();
		else qid = new QueueID();
		setFTable();
		if (samplingType == null && ftable != null && tp != EXACT_MATCH) 
			samplingType = ftable.getSamplingType(RouterPort.SamplingType.ALL);
		else samplingType = new RouterPort.SamplingType(100, RouterPort.SamplingType.ALL);
	}

	private void setFilter(FilterDescriptor.PrefixMask sa, PortRange sp, FilterDescriptor.PrefixMask da, PortRange dp, Protocol p)
	{
		setSrcAddr(sa);
		setSrcPort(sp);
		setDestAddr(da);
		setDestPort(dp);
		setProtocol(p);
	}
	public FilterDescriptor(FilterDescriptor r)
	{
		this(r.getType(),r.nspport, r.ingress);
		setFilter(r.srcAddr, r.srcPortRange, r.destAddr, r.destPortRange, r.protocol);
		ftable = r.ftable;
		if (ingress) qid =  new FilterDescriptor.VOQID(r.qid);
		else qid = new QueueID(r.qid);
		setPluginBinding(r.pluginBinding);
		setFKey(new ForwardKey(r.forwardKey));
		setPriority(r.priority);
		setNegation(r.negation);
		setDrop(r.drop);
		setSamplingType(r.samplingType);
		setTCPFlags(r.tcpflags);
		setTCPFlagsMask(r.tcpflagsMask);
		if (r.getStatsIndex() != 0) setStatsIndex(r.getStatsIndex());
	}
	public FilterDescriptor.PrefixMask getSrcAddr() { return srcAddr;}
	public void setSrcAddr(FilterDescriptor.PrefixMask pf) 
	{ 
		if (properties.getPropertyBool(FILTER_EDITABLE))
		{
			if (getType() == EXACT_MATCH)
				srcAddr = new EMPrefix(pf);
			else
				srcAddr = new GMPrefix(pf);
		}
	}
	public PortRange getSrcPort() { return srcPortRange;}
	public void setSrcPort(PortRange pr) 
	{ 
		if (properties.getPropertyBool(FILTER_EDITABLE))
		{
			if (getType() == EXACT_MATCH)
				srcPortRange = new EMPort(pr);
			else
				srcPortRange = new GMPort(pr);
		}
	}
	public FilterDescriptor.PrefixMask getDestAddr() { return destAddr;}
	public void setDestAddr(FilterDescriptor.PrefixMask pf) 
	{ 
		if (properties.getPropertyBool(FILTER_EDITABLE))
		{
			if (getType() == EXACT_MATCH)
				if (getType() == EXACT_MATCH)
					destAddr = new EMPrefix(pf);	  
				else
					destAddr = new GMPrefix(pf);
		}
	}
	public PortRange getDestPort() { return destPortRange;}
	public void setDestPort(PortRange pr) 
	{ 
		if (properties.getPropertyBool(FILTER_EDITABLE))
		{
			if (getType() == EXACT_MATCH)
				destPortRange = new EMPort(pr);
			else
				destPortRange = new GMPort(pr);
		}
	}
	public void setEditable(boolean b)
	{
		properties.setProperty(FILTER_EDITABLE, b);
	}
	public boolean isEditable()
	{
		return (properties.getPropertyBool(FILTER_EDITABLE));
	}
	public ForwardKey getFKey() { return forwardKey;}
	public void setFKey(ForwardKey nh)  
	{ 
		boolean changed = false;
		if (nsp != null)
		{
			changed = (forwardKey != null && (nh == null || (nh.getPort() != forwardKey.getPort())));
			if (changed || nh == null)
			{
				int p = forwardKey.getPort();
				if (p != ForwardKey.USE_ROUTE)
					((NSPPort)nsp.getPort(p)).removePropertyListener(NSPPort.SUBPORT, this);
			}
			if (nh != null && (nh.getPort() != ForwardKey.USE_ROUTE) && !nh.isMCast())
			{
				nh.setSubport(nsp);
				if (changed) ((NSPPort)nsp.getPort(nh.getPort())).addPropertyListener(NSPPort.SUBPORT, this);
			}
		}
		ExpCoordinator.print(new String("FilterDescriptor.setFKey " + nh.toString()), 4);
		forwardKey = nh;
	}
	public Protocol getProtocol() { return protocol;}
	public void setProtocol(Protocol p)  
	{ 
		if (properties.getPropertyBool(FILTER_EDITABLE))
		{
			if (p.toInt() != Protocol.TCP)
			{
				setTCPFlags(0);
				setTCPFlagsMask(0);
			}
			if (getType() == EXACT_MATCH)
				protocol = new EMProtocol(p);
			else 
				protocol = new GMProtocol(p);
		}
	}
	public void setNegation(boolean b) 
	{
		if (getType() != EXACT_MATCH) negation = b;
		else negation = false;
	}
	public boolean isNegated() { return negation;}
	public void setDrop(boolean b)
	{
		if (getType() != GEN_MATCH) drop = b;
		else drop = false;
	}
	public boolean isDrop() { return drop;}
	public Stats getStats() { return stats;}
	public void setStats(Stats s) { stats.setState(s.getState());} //the value of stats gets set through direct access to the stats instance
	public void setStatsIndex(int i) { stats.setIndex(i);}
	public int getStatsIndex() { return (stats.getIndex());}
	public PluginBinding getPluginBinding() { return pluginBinding;}
	public void setPluginBinding(PluginBinding q) 
	{
		//for egress
		//if setting to no plugin and qid is spc bound set qid to assigned, if setting to plugin and qid set then set qid to assigned
		//else set qid to corresponding number
		double ov = pluginBinding.toInt();
		pluginBinding.setPBinding(q);
		if (!ingress)
		{
			if ((!pluginBinding.isPlugin() && qid.isSPCBound()) || (pluginBinding.toInt() == PluginBinding.PLUGIN))
				qid.setValue(QueueID.ASSIGN);
			else
			{
				if (pluginBinding.isPlugin()) qid.setValue(String.valueOf(pluginBinding.toInt() +  SPC_FLOW));
			}
		}
		fireEvent(new FilterDescriptor.Event(this, FilterDescriptor.Event.PBINDING_CHANGED, ov, (double)pluginBinding.toInt()));
	}
	public Queue.ID getQID() { return qid;}
	private void setQID(int q)
	{
		double ov = (double)qid.getIntValue();
		qid.setValue(String.valueOf(q));
		fireEvent(new FilterDescriptor.Event(this, FilterDescriptor.Event.QID_CHANGED, ov, (double)q)); 
	}
	public void setQID(Queue.ID q) 
	{
		if (!ingress)
		{
			double nv = (double)q.getIntValue();
			//if assigned then reset plugin binding to noplugin/plugin based on current value
			ExpCoordinator.printer.print(new String("FD.setQID " + q.toString() + " val " + nv), 2);
			if (q.isDefault()) 
			{
				if (pluginBinding.isPlugin()) pluginBinding.setPBinding(PluginBinding.PLUGIN);
			}
			//else reset plugin binding to noplugin/integer based on q
			else 
			{
				if (nv < QueueTable.FPX_START) //is spc bound flow
					pluginBinding.setPBinding((int)(nv - SPC_FLOW));
				else pluginBinding.setPBinding(PluginBinding.NOPLUGIN);
			}
			double ov = (double)qid.getIntValue();
			qid = new QueueID(q);
			fireEvent(new FilterDescriptor.Event(this, FilterDescriptor.Event.QID_CHANGED, ov, nv));
		}
	}
	public int getPriority() { return (priority);}
	public void setPriority(int p) 
	{ 
		if (properties.getPropertyBool(FILTER_EDITABLE) && !properties.getPropertyBool(PRIORITY_SET))
		{
			if (p >= 0 && p <= MAX_PRIORITY) priority = p;
			else
			{
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String("Filter priority should be set between 0 - " + MAX_PRIORITY),
						new String("Filter priority should be set between 0 - " + MAX_PRIORITY),
						"Filter Priority Error.",
						StatusDisplay.Error.POPUP));
			}
		}
	}
	public RouterPort.SamplingType getSamplingType() { return samplingType;}
	public void setSamplingType(RouterPort.SamplingType st) { samplingType = st;}
	public int getType() { return (properties.getPropertyInt(TYPE));}
	public void setType(int tp) 
	{ 
		properties.setProperty(TYPE, tp);
		//if (tp != GEN_MATCH)
		//{
		//  setTCPFlags(0);
		//  setTCPFlagsMask(0);
		//}
	}
	public NSPPort getNSPPort() { return nspport;}
	public boolean isIDEqual(FilterDescriptor r)
	{
		boolean flags_equal;
		if (protocol.toInt() == Protocol.TCP && tcpflags == r.tcpflags && tcpflagsMask == r.tcpflagsMask)
			flags_equal = true;
		else flags_equal = false;
		return ( r.srcAddr.isEqual(srcAddr) &&
				r.srcPortRange.isEqual(srcPortRange) &&
				r.destAddr.isEqual(destAddr) &&
				r.destPortRange.isEqual(destPortRange) &&
				r.protocol.isEqual(protocol) &&
				r.getType() == getType() &&
				flags_equal);
	}
	public boolean isEqual(FilterDescriptor r)
	{
		boolean flags_equal = true;
		if (protocol.toInt() == Protocol.TCP)
		{
			if( tcpflags == r.tcpflags && tcpflagsMask == r.tcpflagsMask)
				flags_equal = true;
			else flags_equal = false;
		}
		return ( r.forwardKey.isEqual(forwardKey) &&
				r.srcAddr.isEqual(srcAddr) &&
				r.srcPortRange.isEqual(srcPortRange) &&
				r.destAddr.isEqual(destAddr) &&
				r.destPortRange.isEqual(destPortRange) &&
				r.pluginBinding == pluginBinding &&
				r.protocol.isEqual(protocol) &&
				r.getType() == getType() &&
				flags_equal);
	}
	public void addListener(FilterDescriptor.Listener l){ listeners.add(FilterDescriptor.Listener.class, l);}
	public void removeListener(FilterDescriptor.Listener l){ listeners.remove(FilterDescriptor.Listener.class, l);}
	private void fireEvent(Event e)
	{
		int max = listeners.getListenerCount();
		int i = 0;
		Object[] list = listeners.getListenerList();
		//turns out this is a list of class, listener pairs so list looks like class1, listener1, class2, listener2 ... classn, listenern
		FilterDescriptor.Listener l;
		int change = e.getType();
		for (i = 1; i < list.length; i += 2)
		{
			l = (FilterDescriptor.Listener) (list[i]);
			switch (e.getType())
			{
			case Event.QID_CHANGED:
				l.changedQID(e);
				break;
			case Event.PBINDING_CHANGED:
				l.changedPBinding(e);
				break;
			case Event.STATS_CHANGED:
				l.changedStats(e);
				break;
			case Event.DATA_CHANGED:
				l.changedData(e);
				break;
			}
		}
	}
	public boolean isAux() { return (getType() == GEN_MATCH);}
	public void setAux(boolean b) { setMirrorTraffic(b);}
	public boolean isMirrorTraffic() { return (getType() == GEN_MATCH);}
	public void setMirrorTraffic(boolean b)
	{
		if (getType() != EXACT_MATCH)
		{
			if (b) 
			{
				if (drop)
				{
					fireEvent(new FilterDescriptor.Event(this));
					drop = false;
				}
				setType(GEN_MATCH);
			}
			else setType(EXCL_GEN_MATCH);
		}
	}
	public String toString()
	{
		String rtn = "";
		if (ingress)
			rtn = new String(getType() + " " + srcAddr.toString() + " " + srcPortRange.toString() + " " + destAddr.toString() + " " + destPortRange.toString() + " " + protocol.toString() + " " + pluginBinding.toInt() + " " + forwardKey.toString());
		else
		{
			String tmp_qid = qid.toString();
			if (tmp_qid.equals(QueueID.ASSIGN)) tmp_qid = String.valueOf(pluginBinding.toInt());
			rtn = new String(getType() + " " + srcAddr.toString() + " " + srcPortRange.toString() + " " + destAddr.toString() + " " + destPortRange.toString() + " " + protocol.toString() + " " + tmp_qid + " " + forwardKey.toString());
		}
		if (getType() != EXACT_MATCH) rtn = rtn.concat(" " + priority + " " + negation + " " + drop);
		else  rtn = rtn.concat(" " + drop);
		//STATS_MODULE
		if (stats == null) ExpCoordinator.print("FilterDescriptor.toString stats is null");
		rtn = rtn.concat(" " + stats.getIndex() + " " + samplingType.getType() + " " + tcpflags + " " + tcpflagsMask);
		return rtn;
	}
	public String getRecordedString()
	{
		String rtn = "";
		String tp_str = "exact_match";
		if (getType() != EXACT_MATCH) tp_str = "general_match";
		if (ingress)
			rtn = new String("ingress " + tp_str + " " + srcAddr.toString() + " " + srcPortRange.toString() + " " + destAddr.toString() + " " + destPortRange.toString() + " " + protocol.toString() + " " + pluginBinding.toInt() + " " + forwardKey.toString());
		else
		{
			String tmp_qid = qid.toString();
			if (tmp_qid.equals(QueueID.ASSIGN)) tmp_qid = String.valueOf(pluginBinding.toInt());
			rtn = new String("egress " + tp_str + " " + srcAddr.toString() + " " + srcPortRange.toString() + " " + destAddr.toString() + " " + destPortRange.toString() + " " + protocol.toString() + " " + tmp_qid + " " + forwardKey.toString());
		}
		if (getType() != EXACT_MATCH) rtn = rtn.concat(" " + priority + " " + negation + " " + drop);
		else  rtn = rtn.concat(" " + drop);
		//STATS_MODULE
		if (stats == null) ExpCoordinator.print("FilterDescriptor.toString stats is null");
		rtn = rtn.concat(" " + stats.getIndex() + " " + samplingType.getType() + " " + tcpflags + " " + tcpflagsMask);
		return rtn;
	}
	protected void resetUpdateEdit(FilterTable rt)
	{
		updateEdit = new UpdateEdit((NSPPort)rt.getPort(), ingress, this, rt, rt.getExpCoordinator().getCurrentExp());
	}
	public ONLComponent.Undoable getUpdateEdit() 
	{ 
		updateEdit.setIsUndoable(true);
		return updateEdit;
	}
	public boolean isCommitted() 
	{ 
		return (properties.getPropertyBool(COMMITTED));
	} 
	//begin interface ONLTableCellRenderer.Committable
	public boolean needsCommit()
	{
		//if (properties.getProperty(COMMITTED_FD) == null)
		//  ExpCoordinator.printer.print("FD.needsCommit committed = " + null + " actual " + toString());
		//else
		//  ExpCoordinator.printer.print("FD.needsCommit committed = " + properties.getProperty(COMMITTED_FD) + " actual " + toString());
		if ((properties.getProperty(COMMITTED_FD) == null) ||
				(isEnabled() != properties.getPropertyBool(ENABLED)) ||
				(!properties.getProperty(COMMITTED_FD).equals(toString())))
			return true;
		return false;
	}
	//end interface ONLTableCellRenderer.Committable
	public void setCommitted(boolean b)
	{
		if (b)
		{
			properties.setProperty(COMMITTED, true);
			properties.setProperty(COMMITTED_FD, toString());
		}
	}
	public boolean isEnabled() { return enabled;}
	public void setEnabled(boolean b) { enabled = b;}
	//PropertyChangeListener
	public void propertyChange(PropertyChangeEvent e) //listens on change in nsp port subport
	{
		//if the subport changes change next hop and add edit
		int p = forwardKey.getPort();
		if (p == ForwardKey.USE_ROUTE || forwardKey.isMCast()) return;
		int sub_port = ((NSPPort)nsp.getPort(forwardKey.getPort())).getPropertyInt(NSPPort.SUBPORT);
		if (sub_port != forwardKey.getSubport()) 
		{
			ExpCoordinator.print("FilterDescriptor.propertyChange subport changed" , 4);
			updateEdit.setIsUndoable(false);
			updateEdit.resetUpdate();
			expCoordinator.addEdit(updateEdit);
			setFKey(new ForwardKey(forwardKey.getPort(), sub_port));
		}
	}
	//end PropertyChangeListener
	//ONLCTable.Element
	public boolean isWritable() { return true;}
	public void write(ONL.Writer wrtr) throws java.io.IOException { wrtr.writeString(toString());}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.FILTER);//"filter");
		xmlWrtr.writeAttribute(ExperimentXML.FILTER_TYPE, String.valueOf(getType()));
		xmlWrtr.writeAttribute(ExperimentXML.INGRESS, String.valueOf(ingress));
		xmlWrtr.writeStartElement(ExperimentXML.SRCADDR);
		xmlWrtr.writeCharacters(getSrcAddr().toString());
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.SRCPORT);
		xmlWrtr.writeCharacters(getSrcPort().toString());
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.DESTADDR);
		xmlWrtr.writeCharacters(getDestAddr().toString());
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.DESTPORT);
		xmlWrtr.writeCharacters(getDestPort().toString());
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.PROTOCOL);
		xmlWrtr.writeCharacters(getProtocol().toString());
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.DROP);
		xmlWrtr.writeCharacters(String.valueOf(isDrop()));
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.STATS_INDEX);
		xmlWrtr.writeCharacters(String.valueOf((int)getStats().getValue()));
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.SAMPLING_TYPE);
		xmlWrtr.writeCharacters(String.valueOf(samplingType.getType()));
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.TCPFLAGS);
		xmlWrtr.writeCharacters(String.valueOf(getTCPFlags()));
		xmlWrtr.writeEndElement(); 
		xmlWrtr.writeStartElement(ExperimentXML.TCPFLAGSMASK);
		xmlWrtr.writeCharacters(String.valueOf(getTCPFlagsMask()));
		xmlWrtr.writeEndElement(); 

		if (getType() != EXACT_MATCH)
		{
			xmlWrtr.writeStartElement(ExperimentXML.PRIORITY);
			xmlWrtr.writeCharacters(String.valueOf(getPriority()));
			xmlWrtr.writeEndElement(); 
			xmlWrtr.writeStartElement(ExperimentXML.NEGATE);
			xmlWrtr.writeCharacters(String.valueOf(isNegated()));
			xmlWrtr.writeEndElement(); 
		}

		if (ingress)
		{
			xmlWrtr.writeStartElement(ExperimentXML.FORWARD_KEY);
			xmlWrtr.writeCharacters(String.valueOf(getFKey()));
			xmlWrtr.writeEndElement(); 
			xmlWrtr.writeStartElement(ExperimentXML.PBINDING);
			xmlWrtr.writeCharacters(String.valueOf(getPluginBinding()));
			xmlWrtr.writeEndElement(); 
		}
		else
		{
			xmlWrtr.writeStartElement(ExperimentXML.QID);
			xmlWrtr.writeCharacters(String.valueOf(getQID()));
			xmlWrtr.writeEndElement();
		} 
		xmlWrtr.writeEndElement();
	}
	public ONLComponent getTable() { return ftable;}
	public Object getDisplayField(int c)
	{
		switch(c)
		{
		case SRCADDR: 
			return (getSrcAddr());
		case DESTADDR:
			return (getDestAddr());
		case SRCPORT:
			return (getSrcPort());
		case DESTPORT:
			return (getDestPort());
		case PROTOCOL:
			return (getProtocol());
		case FORWARDKEY:
			return (getFKey());
		case PBINDING: 
			return (getPluginBinding());
		case QID:
			return (getQID());
		case STATS:
			return (getStats()); 
		case PRIORITY:
			return (new Integer(getPriority()));
		case MIRROR:
			return (new Boolean(isMirrorTraffic()));
		case DROP:
			return(new Boolean(isDrop()));
		case NEGATE:
			return(new Boolean(isNegated()));
		case ENABLE:
			return(new Boolean(isEnabled()));
		case SAMPLING_TYPE:
			return samplingType;
		case TCPFIN:
			return (new TCPFlag((tcpflags & TCPFIN_MASK), (tcpflagsMask & TCPFIN_MASK)));
		case TCPSYN:
			return (new TCPFlag((tcpflags & TCPSYN_MASK), (tcpflagsMask & TCPSYN_MASK)));
		case TCPRST:
			return (new TCPFlag((tcpflags & TCPRST_MASK), (tcpflagsMask & TCPRST_MASK)));
		case TCPPSH:
			return (new TCPFlag((tcpflags & TCPPSH_MASK), (tcpflagsMask & TCPPSH_MASK)));
		case TCPACK:
			return (new TCPFlag((tcpflags & TCPACK_MASK), (tcpflagsMask & TCPACK_MASK)));
		case TCPURG:
			return (new TCPFlag((tcpflags & TCPURG_MASK), (tcpflagsMask & TCPURG_MASK)));
			//case TCPFLAGS:
				//return (new Integer(getTCPFlags()));
				//case TCPFLAGSMASK:
					//return (new Integer(getTCPFlagsMask()));
		}
		return null;
	}
	public boolean isEditable(int c) 
	{
		//check if field set in filter if not let it be editable
		//if we're filling in a new filter can edit the prefix and the mask too
		switch(c)
		{
		/*
      case SAMPLING_TYPE:
        return (getType() == GEN_MATCH && isEditable());
      case TCPFIN:
      case TCPSYN:
      case TCPRST:
      case TCPPSH:
      case TCPACK:
      case TCPURG:
        //case TCPFLAGS:
        //case TCPFLAGSMASK:
        return (ExpCoordinator.isTestLevel(ExpCoordinator.RT_STATS) && getType() != EXACT_MATCH && isEditable());
      case DROP:
        return (getType() != GEN_MATCH && isEditable());
		 */
		case STATS: 
			return ((getStats().getState() == PortStats.STOP) && isEnabled());
			//case PROTOCOL:
			//return (isEditable() && (!forwardKey.isMCast() || !ingress || getType() != EXACT_MATCH));
		case ENABLE:
			return (isEditable());
		default:
			return false;//(isEditable());
		}
	}
	public void setField(int c, Object a)
	{
		String ostr = "";
		boolean change = false;
		if (ExpCoordinator.isRecording()) ostr = getRecordedString();
		try
		{
			switch(c)
			{
			case SRCADDR: 
				setSrcAddr(new PrefixMask((String)a));
				change = true;
				break; 
			case DESTADDR: 
				setDestAddr(new PrefixMask((String)a));
				change = true;
				break; 
			case SRCPORT:
				setSrcPort(new PortRange((String)a));
				change = true;
				break; 
			case DESTPORT:
				setDestPort(new PortRange((String)a));
				change = true;
				break;
			case PROTOCOL:
				setProtocol(new Protocol(a.toString()));
				change = true;
				break;
			case FORWARDKEY:
				setFKey(new ForwardKey(a.toString()));
				change = true;
				break;
			case PBINDING:
				setPluginBinding(new PluginBinding(a.toString()));
				change = true;
				break;
			case STATS:
				setStats((Stats)a);
				change = true;
				break;
			case PRIORITY:
				setPriority(((Integer)a).intValue());
				change = true;
				break;
			case TCPFIN:
				setTCPFlags(a.toString(), TCPFIN_MASK);
				change = true;
				break;
			case TCPSYN:
				setTCPFlags(a.toString(), TCPSYN_MASK);
				change = true;
				break;
			case TCPRST:
				setTCPFlags(a.toString(), TCPRST_MASK);
				change = true;
				break;
			case TCPPSH:
				setTCPFlags(a.toString(), TCPPSH_MASK);
				change = true;
				break;
			case TCPACK:
				setTCPFlags(a.toString(), TCPACK_MASK);
				change = true;
				break;
			case TCPURG:
				setTCPFlags(a.toString(), TCPURG_MASK);
				change = true;
				break;
				//case TCPFLAGS:
					//setTCPFlags(((Integer)a).intValue());
					//break;
					//case TCPFLAGSMASK:
						//setTCPFlagsMask(((Integer)a).intValue());
						//break;
					case MIRROR:
						boolean b = ((Boolean)a).booleanValue();
						setMirrorTraffic(b);
						if (!b)
						{
							//setTCPFlags(0);
							//setTCPFlagsMask(0);
							setSamplingType(ftable.getSamplingType(RouterPort.SamplingType.ALL));
						}
						change = true;
						break;
					case DROP:
						setDrop(((Boolean)a).booleanValue());
						change = true;
						break;
					case NEGATE:
						setNegation(((Boolean)a).booleanValue());
						change = true;
						break;
					case QID:
						setQID(new QueueID((String)a));
						change = true;
						break;
					case ENABLE:
						boolean b1 = ((Boolean)a).booleanValue();
						setEnabled(b1);
						if (ExpCoordinator.isRecording())
						{
							if (b1)
								ExpCoordinator.recordEvent(SessionRecorder.ENABLE_FILTER, new String[]{nspport.getLabel(), getRecordedString(), "on"});
							else 
								ExpCoordinator.recordEvent(SessionRecorder.ENABLE_FILTER, new String[]{nspport.getLabel(), getRecordedString(), "off"});
						}
						break; 
					case SAMPLING_TYPE:
						setSamplingType(ftable.getSamplingType((RouterPort.SamplingType)a));
						change = true;
						break; 
			}
			if (c != STATS && isCommitted()) 
			{
				ExpCoordinator ec = ExpCoordinator.theCoordinator;
				UpdateEdit edit = (UpdateEdit)getUpdateEdit();
				if (!ec.isCurrentEdit(edit)) 
				{
					edit.resetUpdate();
					ec.addEdit(edit);
				}
				else edit.setLatest();
			}
		}
		catch (java.text.ParseException e)
		{
			System.err.println("FilterTable::setValueAt (" + c + ") error: " + e.getMessage());
		}
		if (change && ExpCoordinator.isRecording())
			ExpCoordinator.recordEvent(SessionRecorder.EDIT_FILTER, new String[]{nspport.getLabel(), ostr, getRecordedString()});
	}

	public void setState(String s) { properties.setProperty(ONLComponent.STATE, s);}
	public String getState() { return (properties.getProperty(ONLComponent.STATE));}
	//end ONLCTable.Element
	private void setFTable()
	{
		ftable = nspport.getFilterTable(ingress, getType());
	}

	public int getTCPFlags() { return tcpflags;}
	public int getTCPFlagsMask() { return tcpflagsMask;}
	public TCPFlag getTCPFlag(int fl)
	{
		TCPFlag fb = null;
		switch(fl)
		{
		case TCPFIN_MASK:
			fb = new TCPFlag((tcpflags & TCPFIN_MASK), (tcpflagsMask & TCPFIN_MASK));
			break;
		case TCPSYN_MASK: 
			fb = new TCPFlag((tcpflags & TCPSYN_MASK), (tcpflagsMask & TCPSYN_MASK));
			break;
		case TCPRST_MASK:
			fb = new TCPFlag((tcpflags & TCPRST_MASK), (tcpflagsMask & TCPRST_MASK));
			break;
		case TCPPSH_MASK:
			fb = new TCPFlag((tcpflags & TCPPSH_MASK), (tcpflagsMask & TCPPSH_MASK));
			break;
		case TCPACK_MASK: 
			fb = new TCPFlag((tcpflags & TCPACK_MASK), (tcpflagsMask & TCPACK_MASK));
			break;
		case TCPURG_MASK:
			fb = new TCPFlag((tcpflags & TCPURG_MASK), (tcpflagsMask & TCPURG_MASK));
			break;
		}
		return fb;
	}
	public void setTCPFlags(int i) { tcpflags = i;}
	public void setTCPFlagsMask(int i) { tcpflagsMask = i;}
	public void setTCPFlags(String val, int mask)
	{
		if (val.equals("*"))
		{
			tcpflags = ~mask & tcpflags;
			tcpflagsMask = ~mask & tcpflagsMask;
		}
		else
		{
			setProtocol(new Protocol(Protocol.TCP, (getType() == EXACT_MATCH)));
			tcpflagsMask = mask | tcpflagsMask;
			if (val.equals("1")) tcpflags = mask | tcpflags;
			else //if (val.equals("0"))
				tcpflags = ~mask & tcpflags; //WRONG::want to zero out position
		}
		ExpCoordinator.print(new String("FilterDescriptor.setTCPFlags val=" + val + " mask = " + mask + " tcpflags = " + tcpflags + " tcpmask = " + tcpflagsMask), 2);
	}

	public boolean isIngress() { return ingress;}
}

