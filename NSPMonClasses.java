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
 * File: NSPMonClasses.java
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
import java.io.*;
import java.util.Vector;
import javax.swing.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class NSPMonClasses
{
	public static final int NSP_QLENGTH = MonitorDataType.NSPOP + 2;
	public static final int NSP_PKTCOUNTER = MonitorDataType.NSPOP + 3;
	public static final int NSP_EMFILTER = MonitorDataType.NSPOP + 4;
	public static final int NSP_SUBPORTPC = MonitorDataType.NSPOP + 5;
	public static final int NSP_FPXCNTR = MonitorDataType.NSPOP + 6;
	public static final int NSP_VOQLENGTH = MonitorDataType.NSPOP + 7;
	public static final int NSP_RTSTATS = MonitorDataType.NSPOP + 8;
	public static final int NUMBEROFPORTS = 8;

	public static final String NSP_QLENGTH_LBL = "nspQLength";
	public static final String NSP_PKTCOUNTER_LBL = "nspPktCtr";
	public static final String NSP_EMFILTER_LBL = "nspEMFilter";
	public static final String NSP_SUBPORTPC_LBL = "nspSubPortPktCtr";
	public static final String NSP_FPXCNTR_LBL = "nspFPXCtr";
	public static final String NSP_VOQLENGTH_LBL = "nspVOQLength";
	public static final String NSP_RTSTATS_LBL = "nspRTStats";

	//FPX Counters
	public static final int CTR_SP0_INPUT_PKT = 0x00;
	public static final int CTR_P0_INPUT_PKT = 0x04;
	public static final int CTR_P1_INPUT_PKT = 0x05;
	public static final int CTR_P2_INPUT_PKT = 0x06;
	public static final int CTR_P3_INPUT_PKT = 0x07;
	public static final int CTR_P4_INPUT_PKT = 0x08;
	public static final int CTR_P5_INPUT_PKT = 0x09;
	public static final int CTR_P6_INPUT_PKT = 0x0a;
	public static final int CTR_P7_INPUT_PKT = 0x0b;
	public static final int CTR_INPUT_PKT = 0x0100;
	public static final int CTR_SP0_OUTPUT_PKT = 0x10;
	public static final int CTR_P0_OUTPUT_PKT = 0x14;
	public static final int CTR_P1_OUTPUT_PKT = 0x15;
	public static final int CTR_P2_OUTPUT_PKT = 0x16;
	public static final int CTR_P3_OUTPUT_PKT = 0x17;
	public static final int CTR_P4_OUTPUT_PKT = 0x18;
	public static final int CTR_P5_OUTPUT_PKT = 0x19;
	public static final int CTR_P6_OUTPUT_PKT = 0x1a;
	public static final int CTR_P7_OUTPUT_PKT = 0x1b;
	public static final int CTR_OUTPUT_PKT = 0x0200;


	public static class NCCP_LTDataResponse extends NCCP.LTResponseBase //LT response w/clkRate and one 4 byte field 
	{
		protected double data = 0;
		protected double odata = 0;
		protected double clkRateScaler = 1000000;
		protected NCCP_LTDataResponse() { super(12);}
		protected NCCP_LTDataResponse(int dl) { super(dl + 12);}
		public NCCP_LTDataResponse(DataInput din) throws IOException
		{
			super(din, 12);
		}
		public NCCP_LTDataResponse(DataInput din, int dl) throws IOException
		{
			super(din, (dl + 12));
		}

		public void retrieveFieldData(DataInput din) throws IOException
		{
			odata = data;
			double tmp = NCCP.readUnsignedInt(din);
			clkRate = NCCP.readUnsignedInt(din) * clkRateScaler;
			data = NCCP.readUnsignedInt(din);
			setLT(tmp);//since this computes the timeInterval we need the clock rate set first
			print(3);
		}
		public void setLT(double nsp_lt) //change from switch lt because lt is returned as a number of clock ticks    
		{
			oLT = lt;
			lt = nsp_lt;
			if (oLT > 0) 
			{
				if (lt > oLT)
					timeInterval = (lt - oLT)/clkRate;
				else //lt wrapped
				{
					System.out.println("NCCP.LTResponseBase::setLT lt wrapped");
					timeInterval = ((maxUnsignedInt() + lt) - oLT)/clkRate;
				}

				realTimeInterval = timeInterval;

				//if we dropped the last response make the reported timeInterval what is expected
				if (oStatus != NCCP.Status_Fine) timeInterval = period.getSecsOnly();

				//this assumes that the switch has reset and we've gotten a ridiculous number
				if (period != null && (timeInterval > (period.getSecsOnly() + 2))) 
				{
					System.out.println("NCCP.LTResponseBase::reset happened");
					print();
					reset = true;
					timeInterval = period.getSecsOnly();
					realTimeInterval = timeInterval;
				}
				else reset = false;
			}
			else reset = true; //not a switch reset but the first response we've gotten
		}
		public double getData(MonitorDataType.Base mdt) 
		{
			if (mdt == null || mdt.isAbsolute())
				return data;
			else 
			{
				if (odata > data) return (getRate(maxUnsignedInt() + (data - odata)));
				else return (getRate(data - odata));
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////// Qlength //////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////// Request ////////////////////////////////////////////////////////////////////////////////////////////


	public static class NCCP_QLengthRequest extends NCCP.RequesterBase
	{
		protected int pp;
		protected int qid;

		public NCCP_QLengthRequest(int port, int q)
		{
			super(NCCP.Operation_PortQLength);
			pp = port;
			qid = q;
		}
		public void setPP(int port) { pp = port;}
		public int getPP() { return pp;}
		public void setQID(int q) { qid = q;}
		public int getQID() { return qid;}
		public void storeData(DataOutputStream dout) throws IOException
		{
			dout.writeShort(pp);
			dout.writeShort(qid);
		}
	}

	//////////////////////////////////////////////////// Response ////////////////////////////////////////////////////////////////////////////////////////////

	public static class NCCP_QLengthResponse extends NCCP_LTDataResponse
	{
		public NCCP_QLengthResponse() { super();}
		public NCCP_QLengthResponse(DataInput din)  throws IOException 
		{ 
			super(din);
		}
	}

	//////////////////////////////////////////////////// MonitorDataType  ////////////////////////////////////////////////////////////////////////////////////

	public static class MDataType_QLength extends MonitorDataType.Base
	{
		//monitor id is qid
		public MDataType_QLength(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.NSPC, NSPMonClasses.NSP_QLENGTH);
			monitorID = Integer.parseInt(attributes.getValue(ExperimentXML.QID));
		}
		public MDataType_QLength(SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_QLENGTH, Units.BYTES);
			setIsRate(false);
			initFromFile(infile, lv, nlists);
		}
		public MDataType_QLength(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_QLENGTH, Units.BYTES);
			setIsRate(false);
			initFromReader(rdr,dt);
		}

		public MDataType_QLength(int p, int q)
		{
			this(p, q, new String("P" + p + "Q" + q));
		}

		public MDataType_QLength(int p, int q, String nm)
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_QLENGTH, nm, Units.BYTES);
			setIsRate(false);
			monitorID = q;
			port = p;
		}

		public boolean isIPP() { return false;}

		public String getSpec()
		{
			return (String.valueOf(monitorID));
		}

		public void loadFromSpec(String ln)//fills in params based on ln
		{
			monitorID = Integer.parseInt(ln);
		}
		public void initFromReader(ONL.Reader rdr, int dt) throws IOException
		{
			super.initFromReader(rdr, dt);
			monitorID = rdr.readInt();
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeInt(monitorID);
		}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.QID, String.valueOf(monitorID));
		}
		public void setDescription()
		{
			setDescription(new String("Qlength on port " + port + " for qid " + monitorID));
		}
		public String getParamTypeName() { return NSP_QLENGTH_LBL;}
	}


	////////////////////////////////////////////// MonitorEntry //////////////////////////////////////////////////////////////////////////////////////////

	public static class MEntry_QLength extends MonitorEntry.Base
	{
		public MEntry_QLength(MonitorEntry.Listener l, MonitorDataType.Base dt)
		{
			super(l);
			request = new NCCP_QLengthRequest(dt.getPort(), dt.getMonitorID());
			response = new NCCP_QLengthResponse();
			msr = true;
			setMarker(getNewMarker(dt));
			marker.print(2);
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("ME::processResponse");
			Monitor monitor;
			int numParams = monitors.size();
			for (int i = 0; i < numParams; ++i)
			{
				monitor = (Monitor)monitors.elementAt(i);
				if (monitor != null)
				{
					//System.out.println("  calling monitor");
					if (!monitor.isStarted()) monitor.start();
					monitor.setData(r.getData(monitor.getDataType()), ((NCCP_LTDataResponse)r).getTimeInterval());
				}
			}
		}

		public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
		{
			return (new REND_Marker_class(mdt.getPort(), mdt.isIPP(), NCCP.Operation_PortQLength, mdt.getMonitorID()));
		}
	}

	//////////////////////////////////////// MonitorMenuItem /////////////////////////////////////////////////////////////////////////////////////////////
	public static class MMenuItem_Qlength extends MonitorMenuItem.PortItem
	{
		protected Vector monitors;
		private boolean isIPP = true;

		//////////////////////////////////////////////// MMI.MMLParamOptions /////////////////////////////////////////////////////////////////////////////////

		//inner MenuItemParamOptions class 
		protected class QMParamOptions extends MonitorMenuItem.INTParamOptions
		{
			public QMParamOptions()
			{
				super("QID:");
				setIsRate(false);
			}
			public int getQID() { return (getIntParam());}
		}



		/////////////////////////////////////////////////// MMI methods //////////////////////////////////////////////////////////////////////////////////////
		public MMenuItem_Qlength(String nm, NSPPort p)
		{
			super(nm, p);
			monitors = new Vector();
		}
		protected MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			dt = new  NSPMonClasses.MDataType_QLength(port.getID(), ((QMParamOptions)mpo).getQID()); //, ("IPP " + String.valueOf(port.getID())));
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}
		public MonitorMenuItem.MMLParamOptions getParamOptions() 
		{
			return (new QMParamOptions());
		}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			if (mdt.getParamType() == NSPMonClasses.NSP_QLENGTH) return true;
			else return false;
		}
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////// VOQlengths //////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////// Request ////////////////////////////////////////////////////////////////////////////////////////////


	public static class NCCP_VOQLengthRequest extends NCCP.RequesterBase
	{
		protected int pp;

		public NCCP_VOQLengthRequest(int port)
		{
			super(NCCP.Operation_ReadVOQLength);
			pp = port;
		}
		public void setPP(int port) { pp = port;}
		public int getPP() { return pp;}
		public void storeData(DataOutputStream dout) throws IOException
		{
			dout.writeShort(pp);
		}
	}

	//////////////////////////////////////////////////// Response ////////////////////////////////////////////////////////////////////////////////////////////

	public static class NCCP_VOQLengthResponse extends NCCP.LTResponseBase
	{
		protected double data[] = null;
		protected double odata[] = null;
		public NCCP_VOQLengthResponse() 
		{ 
			super(40);
			data = new double[8];
			odata = new double[8];
			for (int i = 0; i < 8; ++i) 
			{
				data[i] = 0;
				odata[i] = 0;
			}
		}
		public NCCP_VOQLengthResponse(DataInput din)  throws IOException 
		{ 
			super(din, 40);
			if (data == null) 
			{
				data = new double[8];
				for (int i = 0; i < 8; ++i) data[i] = 0;
			}
			if (odata == null)
			{
				for (int i = 0; i < 8; ++i) odata[i] = 0;
			}
		}

		public void retrieveFieldData(DataInput din) throws IOException
		{
			int i = 0;
			if (data == null) 
			{
				data = new double[8];
				for (i = 0; i < 8; ++i) data[i] = 0;
			}
			if (odata == null) odata = new double[8];
			for (i = 0; i < 8; ++i) odata[i] = data[i];
			double tmp = NCCP.readUnsignedInt(din);
			clkRate = NCCP.readUnsignedInt(din);
			for (i = 0; i < 8; ++i) data[i] = NCCP.readUnsignedInt(din);
			setLT(tmp);//since this computes the timeInterval we need the clock rate set first
		}
		public void setLT(double nsp_lt) //change from switch lt because lt is returned as a number of clock ticks    
		{
			oLT = lt;
			lt = nsp_lt;
			if (oLT > 0) 
			{
				if (lt > oLT)
					timeInterval = (lt - oLT)/clkRate;
				else //lt wrapped
				{
					System.out.println("NCCP.LTResponseBase::setLT lt wrapped");
					timeInterval = ((maxUnsignedInt() + lt) - oLT)/clkRate;
				}

				realTimeInterval = timeInterval;

				//if we dropped the last response make the reported timeInterval what is expected
				if (oStatus != NCCP.Status_Fine) timeInterval = period.getSecsOnly();

				//this assumes that the switch has reset and we've gotten a ridiculous number
				if (period != null && (timeInterval > (period.getSecsOnly() + 2))) 
				{
					System.out.println("NCCP.LTResponseBase::reset happened");
					print();
					reset = true;
					timeInterval = period.getSecsOnly();
					realTimeInterval = timeInterval;
				}
				else reset = false;
			}
			else reset = true; //not a switch reset but the first response we've gotten
		}
		public double getData(MonitorDataType.Base mdt) 
		{ 
			int ndx = mdt.getMonitorID();
			if (mdt.isAbsolute())
				return data[ndx];
			else 
			{
				double diff = data[ndx] - odata[ndx];
				if (diff < 0) return (getRate(maxUnsignedInt() + diff));
				else return (getRate(diff));
			}
		}
	}

	//////////////////////////////////////////////////// MonitorDataType  ////////////////////////////////////////////////////////////////////////////////////

	public static class MDataType_VOQLength extends MonitorDataType.Base
	{
		//monitor id is qid
		public MDataType_VOQLength(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.NSPC, NSPMonClasses.NSP_VOQLENGTH);
			monitorID = Integer.parseInt(attributes.getValue(ExperimentXML.QID));
		}
		public MDataType_VOQLength(SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_VOQLENGTH, Units.BYTES);
			setIsRate(false);
			initFromFile(infile, lv, nlists);
		}

		public MDataType_VOQLength(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_VOQLENGTH, Units.BYTES);
			setIsRate(false);
			initFromReader(rdr, dt);
		}

		public MDataType_VOQLength(int p, int q)
		{
			this(p, q, new String("P" + p + "VOQ" + q));
		}

		public MDataType_VOQLength(int p, int q, String nm)
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_VOQLENGTH, nm, Units.BYTES);
			setIsRate(false);
			monitorID = q;
			port = p;
		}

		public boolean isIPP() { return true;}

		public String getSpec()
		{
			return (String.valueOf(monitorID));
		}

		public void loadFromSpec(String ln)//fills in params based on ln
		{
			monitorID = Integer.parseInt(ln);
		}
		public void initFromReader(ONL.Reader rdr, int dt) throws IOException
		{
			super.initFromReader(rdr, dt);
			monitorID = rdr.readInt();
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeInt(monitorID);
		}
		public String getParamTypeName() { return NSP_VOQLENGTH_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.QID, String.valueOf(monitorID));
		}
		protected void setDescription()
		{
			setDescription(new String("VOQlength on port " + port + " for qid " + monitorID));
		}
	}


	////////////////////////////////////////////// MonitorEntry //////////////////////////////////////////////////////////////////////////////////////////

	public static class MEntry_VOQLength extends MonitorEntry.Base
	{
		public MEntry_VOQLength(MonitorEntry.Listener l, MonitorDataType.Base dt)
		{
			super(l);
			request = new NCCP_VOQLengthRequest(dt.getPort());
			response = new NCCP_VOQLengthResponse();
			msr = true;
			setMarker(getNewMarker(dt));
			marker.print(2);
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("ME::processResponse");
			Monitor monitor;
			int numParams = monitors.size();
			for (int i = 0; i < numParams; ++i)
			{
				monitor = (Monitor)monitors.elementAt(i);
				if (monitor != null)
				{
					//System.out.println("  calling monitor");
					if (!monitor.isStarted()) monitor.start();
					monitor.setData(r.getData(monitor.getDataType()), ((NCCP.LTResponseBase)r).getTimeInterval());
				}
			}
		}

		public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
		{
			return (new REND_Marker_class(mdt.getPort(), mdt.isIPP(), NCCP.Operation_ReadVOQLength));
		}
	}

	//////////////////////////////////////// MonitorMenuItem /////////////////////////////////////////////////////////////////////////////////////////////
	public static class MMenuItem_VOQlength extends MonitorMenuItem.PortItem
	{
		protected Monitor monitors[] = null;
		private boolean isIPP = true;

		//////////////////////////////////////////////// MMI.MMLParamOptions /////////////////////////////////////////////////////////////////////////////////

		//inner MenuItemParamOptions class 
		protected class VOQMParamOptions extends MonitorMenuItem.INTParamOptions
		{
			public VOQMParamOptions()
			{
				super("VOQ:");
				setIsRate(false);
			}
			public int getQID() { return (getIntParam());}
		}



		/////////////////////////////////////////////////// MMI methods //////////////////////////////////////////////////////////////////////////////////////
		public MMenuItem_VOQlength(String nm, NSPPort p)
		{
			super(nm, p);
			monitors = new Monitor[8];
		}
		protected MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo)
		{
			System.out.println("NSPMonClasses.MMenuItem_VOQlength.getDataType " + port.getID() + ", " + ((VOQMParamOptions)mpo).getQID());
			MonitorDataType.Base dt = null;
			dt = new  NSPMonClasses.MDataType_VOQLength(port.getID(), ((VOQMParamOptions)mpo).getQID()); //, ("IPP " + String.valueOf(port.getID())));
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}
		public MonitorMenuItem.MMLParamOptions getParamOptions() 
		{
			return (new VOQMParamOptions());
		}
		public Monitor getMonitor(MonitorDataType.Base dt)
		{
			if (!dt.isPeriodic()) return null;
			int i = dt.getMonitorID();
			if (i < 0 || i > 7) i = 0;
			return (monitors[i]);
		}
		public Monitor createMonitor(MonitorDataType.Base dt)
		{
			Monitor m = super.createMonitor(dt);
			if (m != null && dt.isPeriodic()) monitors[dt.getMonitorID()] = m;
			return (m);
		}

		public void removeMonitor(Monitor m)
		{
			for (int i = 0; i < 8; ++i)
				if (m == monitors[i]) monitors[i] = null;
		}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			if (mdt.getParamType() == NSPMonClasses.NSP_VOQLENGTH) return true;
			else return false;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////// FPX General Counter ////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////// NCCP_FPXCounterRequest ///////////////////////////////////////////////////////////////////////////

	public static class NCCP_FPXCounterRequest extends NCCP.RequesterBase
	{
		protected int pp;
		protected int counter;

		public NCCP_FPXCounterRequest(int port, int ctype)
		{
			super(NCCP.Operation_FPXCounter);
			pp = port;
			counter = ctype;
		}
		public void setPP(int port) { pp = port;}
		public int getPP() { return pp;}
		public void setCounterType(int q) { counter = q;}
		public int getCounterType() { return counter;}
		public void storeData(DataOutputStream dout) throws IOException
		{
			dout.writeShort(pp);
			dout.writeShort(counter);
		}
	}

	//////////////////////////////////////////////////// NCCP_PktCounterResponse ////////////////////////////////////////////////////////////////////////////

	public static class NCCP_PktCounterResponse extends NCCP_LTDataResponse
	{
		public NCCP_PktCounterResponse() { super();}
		public NCCP_PktCounterResponse(DataInput din)  throws IOException 
		{ 
			super(din);
		}
	}  


	//////////////////////////////////////////////////// NCCP_PktCounter16Response for 16 bit counter  ////////////////////////////////////////////////////

	public static class NCCP_Pkt16CounterResponse extends NCCP_LTDataResponse
	{
		//protected double packetRate = 0;
		//protected double odata = 0;
		public NCCP_Pkt16CounterResponse() { super();}
		public NCCP_Pkt16CounterResponse(DataInput din)  throws IOException 
		{ 
			super(din);
		}
		/*
    public void retrieveFieldData(DataInput din) throws IOException
      {
	odata = data;
	super.retrieveFieldData(din);
	double tmp;
	if (data >= odata) tmp = data - odata;
	else tmp = maxUnsignedShort() + data - odata;
	packetRate = tmp/(getRTimeInterval());
	double rt = getRTimeInterval();
	//System.out.println("Packet16Count NCCP.Response (clkRate, lt, rt, data, odata, tmp) = (" + clkRate + ", " + lt + ", " + rt + ", " + data + ", " + odata + ", " + tmp + ")");
        }*/
		public double getCounter() { return data;}
		public double getData(MonitorDataType.Base mdt) 
		{
			if (mdt.isAbsolute())
				return data;
			else 
			{
				if (odata > data) return (getRate(maxUnsignedShort() + (data - odata)));
				else return (getRate(data - odata));
			}
		}
		/*public double getData(MonitorDataType.Base mdt) 
      {
        if (mdt.isPeriodic()) return packetRate;
        else return (getCounter());
        }*/
	}


	//////////////////////////////////////////////////// MDataType_FPXCounter  ///////////////////////////////////////////////////////////////////////////

	public static class MDataType_FPXCounter extends MonitorDataType.Base
	{
		private int otherPort = -1;
		//monitor id is counter
		public MDataType_FPXCounter(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.NSPC, NSPMonClasses.NSP_FPXCNTR);
			monitorID = Integer.parseInt(attributes.getValue(ExperimentXML.COUNTER));
		}
		public MDataType_FPXCounter()
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_FPXCNTR, Units.PKTS);
		}
		public MDataType_FPXCounter(SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_FPXCNTR, Units.PKTS);
			setIsRate(false);
			initFromFile(infile, lv, nlists);
			boolean old = (infile.getVersion() <= 1.9);
			if (old)
			{
				if (monitorID == CTR_SP0_INPUT_PKT) monitorID = CTR_INPUT_PKT;
				if (monitorID == CTR_SP0_OUTPUT_PKT) monitorID = CTR_OUTPUT_PKT;
			}
			setMonitorID(monitorID);
		}
		public MDataType_FPXCounter(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_FPXCNTR, Units.PKTS);
			setIsRate(false);
			initFromReader(rdr, dt);
			setMonitorID(monitorID);
		}
		public MDataType_FPXCounter(int p, int o)
		{
			this(p, o, new String("P" + p + "Counter"+ o));
			if (monitorID == CTR_INPUT_PKT)
				setName(new String("P"+p+"InPktCtr"));
			if (monitorID >= CTR_P0_OUTPUT_PKT && monitorID <= CTR_P7_OUTPUT_PKT)
				setName(new String("P"+p+"toP"+otherPort+"PktCtr"));
			if (monitorID == CTR_OUTPUT_PKT)
				setName(new String("P"+p+"OutPktCtr"));
			if (monitorID >= CTR_P0_INPUT_PKT && monitorID <= CTR_P7_INPUT_PKT)
				setName(new String("P"+p+"fromP"+otherPort+"PktCtr"));
		}
		public MDataType_FPXCounter(int p, int o, String nm)
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_FPXCNTR, nm, Units.PKTS);
			setIsRate(false);
			port = p;
			setMonitorID(o);
		}
		public void setMonitorID(int o)
		{
			monitorID = o;
			if ((monitorID >= CTR_P0_OUTPUT_PKT && monitorID <= CTR_P7_OUTPUT_PKT) ||
					(monitorID >= CTR_P0_INPUT_PKT && monitorID <= CTR_P7_INPUT_PKT))
				setOtherPort();
			switch(monitorID)
			{
			case 32:
			case 33:
			case 34:
			case 48:
			case 49:
			case 50:
			case 64:
				setDataUnits(Units.CELLS);
				break;
			default:
				setDataUnits(Units.PKTS);
			}
		}
		public boolean isIPP() { return true;} //doesn't matter
		private void setOtherPort()
		{
			if (monitorID >= CTR_P0_OUTPUT_PKT && monitorID <= CTR_P7_OUTPUT_PKT)
				otherPort = monitorID - CTR_P0_OUTPUT_PKT;
			if (monitorID >= CTR_P0_INPUT_PKT && monitorID <= CTR_P7_INPUT_PKT)
				otherPort = monitorID - CTR_P0_INPUT_PKT;
		}

		public String getSpec()
		{
			return (String.valueOf(monitorID));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			monitorID = Integer.parseInt(ln);
		}
		public void initFromReader(ONL.Reader rdr, int dt) throws IOException
		{
			super.initFromReader(rdr, dt);
			monitorID = rdr.readInt();
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeInt(monitorID);
		}
		public String getParamTypeName() { return NSP_FPXCNTR_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.COUNTER, String.valueOf(monitorID));
		}
		protected void setDescription()
		{
			String st;
			switch(monitorID)
			{
			case CTR_INPUT_PKT:
				st = new String("FPX:number of packets arriving at ingress port " + port);
				break;
			case CTR_OUTPUT_PKT:
				st = new String("FPX:number of packets sent out egress port " + port);
				break;
			case CTR_P0_INPUT_PKT:
			case CTR_P1_INPUT_PKT:
			case CTR_P2_INPUT_PKT:
			case CTR_P3_INPUT_PKT:
			case CTR_P4_INPUT_PKT:
			case CTR_P5_INPUT_PKT:
			case CTR_P6_INPUT_PKT:
			case CTR_P7_INPUT_PKT:
				st = new String("FPX:number of packets arriving at egress port " + port + " from port " + otherPort);
				break;
			case CTR_P0_OUTPUT_PKT:
			case CTR_P1_OUTPUT_PKT:
			case CTR_P2_OUTPUT_PKT:
			case CTR_P3_OUTPUT_PKT:
			case CTR_P4_OUTPUT_PKT:
			case CTR_P5_OUTPUT_PKT:
			case CTR_P6_OUTPUT_PKT:
			case CTR_P7_OUTPUT_PKT:
				st = new String("FPX:number of packets destined for port " + otherPort + " at ingress port " + port); 
				break;
			case 32:
			case 33:
			case 34:
			case 48:
			case 49:
			case 50:
			case 64:
				st = new String("FPX Counter " + monitorID + " on port " + port + " in cells");
				break;
			default:
				st = new String("FPX Counter " + monitorID + " on port " + port + " in packets");
			}
			setDescription(st);
		}
	}

	/////////////////////////////////////////////////////////////  MDataType_PktCounter ///////////////////////////////////////////////////////////////

	//NOTE: this here for backwards compatibility
	public static class MDataType_PktCounter extends MDataType_FPXCounter
	{
		//monitor id is outport
		public MDataType_PktCounter(String uri, Attributes attributes)
		{
			super(uri, attributes);
			setMonitorID(monitorID + CTR_P0_OUTPUT_PKT);
		}
		public MDataType_PktCounter(SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
		{
			super();
			initFromFile(infile, lv, nlists);
			setMonitorID(monitorID + CTR_P0_OUTPUT_PKT);
		}
		public MDataType_PktCounter(ONL.Reader rdr, int dt) throws IOException
		{
			super();
			initFromReader(rdr, dt);
			setMonitorID(monitorID + CTR_P0_OUTPUT_PKT);
		}
	}


	////////////////////////////////////////////// MonitorEntry //////////////////////////////////////////////////////////////////////////////////////////

	public static class MEntry_FPXCounter extends MonitorEntry.Base
	{
		public MEntry_FPXCounter(MonitorEntry.Listener l, MonitorDataType.Base dt)
		{
			super(l);
			request = new NCCP_FPXCounterRequest(dt.getPort(), dt.getMonitorID());
			response = new NCCP_PktCounterResponse();
			msr = true;
			setMarker(getNewMarker(dt));
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("ME::processResponse");
			Monitor monitor;
			int numParams = monitors.size();
			for (int i = 0; i < numParams; ++i)
			{
				monitor = (Monitor)monitors.elementAt(i);
				if (monitor != null)
				{
					//System.out.println("  calling monitor");
					if (!monitor.isStarted()) monitor.start();
					monitor.setData(r.getData(monitor.getDataType()), ((NCCP_LTDataResponse)r).getTimeInterval());
				}
			}
		}

		public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
		{
			return (new REND_Marker_class(mdt.getPort(), mdt.isIPP(), NCCP.Operation_FPXCounter, mdt.getMonitorID()));
		}
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////// FPXCounterMenu ///////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class FPXCounterMenu extends MonitorMenuItem.PortMenu
	{
		///////////////////////////////////////// FPXCounterMenu Constructor ////////////////////////////////////////////////////////////////////
		public FPXCounterMenu(NSPPort p, boolean ipp)
		{
			super("FPX Counters", p, 3, ipp);
			if (ipp)
			{
				mmItems[0] = new MMenuItem_FPXCounter("Ingress Packets", p, CTR_INPUT_PKT) ;
				mmItems[1] = new MMenuItem_PktCounter("Ingress Packets destined for Port", p, ipp);
				mmItems[2] = new MMenuItem_GenFPXCounter("FPX General Counter", p);
			}
			else 
			{
				mmItems[0] = new MMenuItem_FPXCounter("Egress Packets", p, CTR_OUTPUT_PKT) ;
				mmItems[1] = new MMenuItem_PktCounter("Egress Packets arriving from Port", p, ipp);
				mmItems[2] = new MMenuItem_GenFPXCounter("FPX General Counter", p);
			}
			add(mmItems[0].getMenuItem());
			add(mmItems[1].getMenuItem());
			add(mmItems[2].getMenuItem());
		}
	}

	//////////////////////////////////////// MonitorMenuItem /////////////////////////////////////////////////////////////////////////////////////////////
	public static class MMenuItem_FPXCounter extends MonitorMenuItem.PortItem
	{
		private int counter = 0;

		/////////////////////////////////////////////////// MMI methods //////////////////////////////////////////////////////////////////////////////////////
		public MMenuItem_FPXCounter(String nm, NSPPort p, int ctr)
		{
			super(nm, p);
			counter = ctr;
		}
		protected MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			dt = new  NSPMonClasses.MDataType_FPXCounter(port.getID(), counter); //, ("IPP " + String.valueOf(port.getID())));
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}
		public MonitorMenuItem.MMLParamOptions getParamOptions()
		{
			MonitorMenuItem.MMLParamOptions mpo = super.getParamOptions();
			mpo.setIsRate(false);
			return mpo;
		}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			if (mdt.getParamType() == NSPMonClasses.NSP_FPXCNTR && mdt.getMonitorID() == counter) return true;
			else return false;
		}
	}

	//////////////////////////////////////// MonitorMenuItem /////////////////////////////////////////////////////////////////////////////////////////////
	public static class MMenuItem_GenFPXCounter extends MonitorMenuItem.PortItem
	{
		protected Vector monitors;

		//////////////////////////////////////////////// MMI.MMLParamOptions /////////////////////////////////////////////////////////////////////////////////

		//inner MenuItemParamOptions class 
		protected class FPXCParamOptions extends MonitorMenuItem.INTParamOptions
		{
			public FPXCParamOptions()
			{
				super("Counter:");
				setIsRate(false);
			}
			public int getCounter() { return (getIntParam());}
		}



		/////////////////////////////////////////////////// MMI methods //////////////////////////////////////////////////////////////////////////////////////
		public MMenuItem_GenFPXCounter(String nm, NSPPort p)
		{
			super(nm, p);
			monitors = new Vector();
		}
		protected MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			dt = new  NSPMonClasses.MDataType_FPXCounter(port.getID(), ((FPXCParamOptions)mpo).getCounter()); //, ("IPP " + String.valueOf(port.getID())));
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}
		public MonitorMenuItem.MMLParamOptions getParamOptions() { return (new FPXCParamOptions());}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			int monitor_id = mdt.getMonitorID();
			if (mdt.getParamType() != NSPMonClasses.NSP_FPXCNTR)// ||
				//monitorID == CTR_SP0_INPUT_PKT ||
				//monitorID == CTR_SP0_OUTPUT_PKT ||
				//(monitorID >= CTR_P0_INPUT_PKT && monitorID <= CTR_P7_INPUT_PKT) ||
				//(monitorID >= CTR_P0_INPUT_PKT && monitorID <= CTR_P7_INPUT_PKT))
				return false;
			else return true;
		}
	}
	//////////////////////////////////////// MonitorMenuItem /////////////////////////////////////////////////////////////////////////////////////////////
	public static class MMenuItem_PktCounter extends  MMenuItem_GenFPXCounter
	{
		private int baseCounter = CTR_P0_OUTPUT_PKT;
		//////////////////////////////////////////////// MMI.MMLParamOptions /////////////////////////////////////////////////////////////////////////////////

		//inner MMLParamOptions class
		protected class PCParamOptions extends MonitorMenuItem.INTParamOptions
		{
			public PCParamOptions(String nm)
			{
				super(nm);
				setIsRate(false);
			}
			public int getOutPort() { return (getIntParam());}
		}


		/////////////////////////////////////////////////// MMI methods //////////////////////////////////////////////////////////////////////////////////////
		public MMenuItem_PktCounter(String nm, NSPPort p, boolean in)
		{
			super(nm, p);
			if (in) baseCounter = CTR_P0_OUTPUT_PKT;
			else baseCounter = CTR_P0_INPUT_PKT;
		}

		protected MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo)
		{
			MonitorDataType.Base dt = null;
			int ctr = baseCounter + ((PCParamOptions)mpo).getOutPort();
			dt = new  NSPMonClasses.MDataType_FPXCounter(port.getID(), ctr); //, ("IPP " + String.valueOf(port.getID())));
			dt.setPollingRate(mpo.getPollingRate());
			dt.setONLComponent(port);
			return dt;
		}
		public MonitorMenuItem.MMLParamOptions getParamOptions() 
		{
			if (baseCounter == CTR_P0_OUTPUT_PKT )
				return (new PCParamOptions("OUTPORT:"));
			else 
				return (new PCParamOptions("INPORT:"));
		}
		public boolean isDataType(MonitorDataType.Base mdt)
		{
			boolean is_ctr = super.isDataType(mdt);
			int mid = mdt.getMonitorID();
			if (is_ctr && mid >= baseCounter && mid < (baseCounter + 8)) return true;
			return false;
		}
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////// Exact Match Filter ///////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////// Request ////////////////////////////////////////////////////////////////////////////////////////////


	public static class NCCP_EMFilterRequest extends FilterDescriptor.NCCP_FilterRequester
	{
		public NCCP_EMFilterRequest(int port, FilterDescriptor fid, boolean in)
		{
			super(NCCP.Operation_EMFilterPC, fid.getNSPPort(), fid, in);
		}
		/*
    public void setPP(int port) { pp = port;}
    public int getPP() { return pp;}
    public void setFilterID(FilterID fid) { filterID = fid;}
    public FilterID getFilterID() { return filterID;}
    public boolean isIPP() { return is_input;}
    public void storeData(DataOutputStream dout) throws IOException
      {
	dout.writeShort(pp);
	filterID.write(dout);
	if (is_input) dout.writeByte(1);
	else dout.writeByte(0);
      }
		 */
	}

	//////////////////////////////////////////////////// Response ////////////////////////////////////////////////////////////////////////////////////////////

	public static class NCCP_EMFilterResponse extends NCCP_Pkt16CounterResponse
	{
		public NCCP_EMFilterResponse() { super();}
		public NCCP_EMFilterResponse(DataInput din)  throws IOException 
		{ 
			super(din);
		}
		/*
    public double getData(MonitorDataType.Base mdt) 
      {
	if (mdt.isAbsolute() || mdt.getMonitorID() == MDataType_EMFilter.COUNTER) return (getCounter());
	else return packetRate;
        }*/
	}

	//////////////////////////////////////////////////// MonitorDataType  ////////////////////////////////////////////////////////////////////////////////////

	public static class MDataType_EMFilter extends MonitorDataType.Base implements StatsMonitorable
	{
		public static final int RATE = 0; 
		public static final int COUNTER = 1; 
		protected boolean is_input = true;
		protected FilterDescriptor filter = null;


		////////////////////////// NSPMonClasses.MDataType_EMFilter.XMLHandler /////////////////////////////////////////
		protected static class XMLHandler extends MonitorDataType.Base.XMLHandler
		{
			public XMLHandler(ExperimentXML exp_xml, MDataType_EMFilter mdt)
			{
				super(exp_xml, mdt);
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	  
				super.startElement(uri, localName, qName, attributes);
				if (localName.equals(ExperimentXML.FILTER))
				{
					ONLComponent onlc = mdataType.getONLComponent();
					if (onlc != null && onlc instanceof FilterTable)
					{
						FilterDescriptor fd = new FilterDescriptor(uri, attributes, (NSPPort)onlc.getParent());
						expXML.setContentHandler(new FilterDescriptor.XMLHandler(expXML, fd)
						{
							public void endElement(String uri, String localName, String qName)
							{
								super.endElement(uri, localName, qName);
								if (localName.endsWith(ExperimentXML.FILTER)) 
								{
									((NSPMonClasses.MDataType_EMFilter)mdataType).filter = ((FilterTable)mdataType.getONLComponent()).getFilter(currentFilter);
								}
							}
						});
					}
				}
			}
		}//end NSPMonClasses.MDataType_EMFilter.XMLHandler

		public MDataType_EMFilter(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.NSPC, NSPMonClasses.NSP_EMFILTER);
			is_input = Boolean.valueOf(attributes.getValue(ExperimentXML.INGRESS)).booleanValue();
		}
		public MDataType_EMFilter(SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_EMFILTER, Units.PKTS);
			initFromFile(infile, lv, nlists);
			if (monitorID == COUNTER) setIsRate(false);
			monitorID = 0;
		}
		public MDataType_EMFilter(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_EMFILTER, Units.PKTS);
			initFromReader(rdr, dt);
			if (monitorID == COUNTER) setIsRate(false);
			monitorID = 0;
		}
		public MDataType_EMFilter(NSPPort p, FilterDescriptor fd, int monID, boolean in) { this(p, fd, monID, in, Units.PKTS);}
		public MDataType_EMFilter(NSPPort p, FilterDescriptor fd, int monID, boolean in, int u)
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_EMFILTER, u);
			setONLComponent(p.getFilterTable(in, fd.getType()));
			filter = fd;
			if (monID == COUNTER) setIsRate(false);
			port = p.getID();
			is_input = in;
		}

		public boolean isEqual(MonitorDataType.Base mdt)
		{
			if (mdt instanceof MDataType_EMFilter)
				return (super.isEqual(mdt) && filter == ((MDataType_EMFilter)mdt).filter);
			else
				return false;
		}
		public boolean isIPP() { return is_input;}

		public String getParamTypeName() { return NSP_EMFILTER_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.INGRESS, String.valueOf(is_input));
		}
		public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			filter.writeXML(xmlWrtr);
		}
		public String getSpec()
		{
			return (new String(is_input + " " + filter.toString()));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			String[] strarray = ln.split(" ");
			is_input = Boolean.valueOf(strarray[0]).booleanValue();
			try 
			{
				filter = ((FilterTable)getONLComponent()).getFilter(new FilterDescriptor((NSPPort)getONLComponent().getParent(), 
						is_input, 
						ln.substring(strarray[0].length())));
			}
			catch (java.text.ParseException e) { filter = null; }
		}
		public void initFromReader(ONL.Reader rdr, int dt) throws IOException
		{
			super.initFromReader(rdr, dt);
			is_input = rdr.readBoolean();
			try 
			{
				filter = ((FilterTable)getONLComponent()).getFilter(new FilterDescriptor((NSPPort)getONLComponent().getParent(), 
						is_input,
						rdr.readString()));
			}
			catch (java.text.ParseException e) { filter = null; }
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeBoolean(is_input);
			filter.write(wrtr);
		}
		protected void setDescription()
		{
			if (is_input)
				setDescription(new String("Filter Packet Count(" + filter.toString() + ") on input port " + port));
			else
				setDescription(new String("Filter Packet Count(" + filter.toString() + ") on output port " + port));
		}
		public FilterDescriptor getFilter() { return filter;}
		//StatsMonitorable
		public int getStatsIndex() { return (filter.getStatsIndex());}

		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new NSPMonClasses.MDataType_EMFilter.XMLHandler(exp_xml, this));}
	}

	////////////////////////////////////////////// MonitorEntry //////////////////////////////////////////////////////////////////////////////////////////

	public static class MEntry_EMFilter extends MonitorEntry.Base
	{
		public static class EMFREND_Marker extends FilterDescriptor.REND_Marker
		{
			public EMFREND_Marker (MDataType_EMFilter mdt)
			{
				super(NCCP.Operation_EMFilterPC, mdt.getFilter().getNSPPort(), mdt.getFilter(), mdt.isIPP());
			}
		}

		public MEntry_EMFilter(MonitorEntry.Listener l, MonitorDataType.Base dt)
		{
			super(l);
			request = new NCCP_EMFilterRequest(dt.getPort(), ((MDataType_EMFilter)dt).getFilter(), dt.isIPP());
			response = new NCCP_EMFilterResponse();
			msr = true;
			setMarker(getNewMarker(dt));
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("ME::processResponse");
			Monitor monitor;
			int numParams = monitors.size();
			for (int i = 0; i < numParams; ++i)
			{
				monitor = (Monitor)monitors.elementAt(i);
				if (monitor != null)
				{
					//System.out.println("  calling monitor");
					if (!monitor.isStarted()) monitor.start();
					monitor.setData(r.getData(monitor.getDataType()), ((NCCP_LTDataResponse)r).getTimeInterval());
				}
			}
		}

		public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
		{
			return (new EMFREND_Marker((MDataType_EMFilter)mdt));
		}
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////// Route Stats ///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////// Request /////////////////////////////////////////////////////////////////////////////////////


	public static class NCCP_RTStatsRequest extends NCCP.RequesterBase
	{  
		private Route route;
		private int port;
		public NCCP_RTStatsRequest(int p, Route rt)
		{
			super(NCCP.Operation_RTPktCounter);
			route = rt;
			port = p;
			setMarker(new REND_Marker_class());
		}
		public void storeData(DataOutputStream dout) throws IOException
		{
			dout.writeShort(port);
			NCCP.writeString(route.getProperty(Route.COMMITTED_PM), dout);
		}  
	}

	//////////////////////////////////////////////////// Response ////////////////////////////////////////////////////////////////////////////////////////

	public static class NCCP_RTStatsResponse  extends  NCCP_PktCounterResponse
	{
		public NCCP_RTStatsResponse() { super();}
		public NCCP_RTStatsResponse(DataInput din)  throws IOException 
		{ 
			super(din);
		}
		/*
    public double getData(MonitorDataType.Base mdt) 
    { 
      if (!mdt.isPeriodic() || mdt.getMonitorID() == MDataType_RTStats.COUNTER) return (getCounter());
      else return packetRate;
      }*/
		public double getCounter() { return data;}
	}

	//////////////////////////////////////////////////// MonitorDataType  ////////////////////////////////////////////////////////////////////////////////////

	public static class MDataType_RTStats extends MonitorDataType.Base implements StatsMonitorable
	{
		public static final int RATE = 0; 
		public static final int COUNTER = 1; 
		protected Route route = null;

		////////////////////////// NSPMonClasses.MDataType_RTStats.XMLHandler /////////////////////////////////////////
		protected static class XMLHandler extends MonitorDataType.Base.XMLHandler
		{
			public XMLHandler(ExperimentXML exp_xml, MDataType_RTStats mdt)
			{
				super(exp_xml, mdt);
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	  
				super.startElement(uri, localName, qName, attributes);
				if (localName.equals(ExperimentXML.ROUTE))
				{
					ONLComponent onlc = mdataType.getONLComponent();
					if (onlc != null && onlc instanceof RouteTable)
					{
						Route rt = new NSPRoute((RouteTable)onlc);
						expXML.setContentHandler(new Route.XMLHandler(expXML, rt)
						{
							public void endElement(String uri, String localName, String qName)
							{
								super.endElement(uri, localName, qName);
								if (localName.endsWith(ExperimentXML.ROUTE)) 
								{
									((NSPMonClasses.MDataType_RTStats)mdataType).route = ((RouteTable)mdataType.getONLComponent()).getRoute(currentRoute.getPrefixMask());
								}
							}
						});
					}
				}
			}
		}//end NSPMonClasses.MDataType_RTStats.XMLHandler

		public MDataType_RTStats(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.NSPC, NSPMonClasses.NSP_RTSTATS);
		}
		public MDataType_RTStats(SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_RTSTATS, Units.PKTS);
			initFromFile(infile, lv, nlists);
			if (monitorID == COUNTER) setIsRate(false);
			monitorID = 0;
		}
		public MDataType_RTStats(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_RTSTATS, Units.PKTS);
			initFromReader(rdr, dt);
			if (monitorID == COUNTER) setIsRate(false);
			monitorID = 0;
		}
		public MDataType_RTStats(NSPPort p, Route rt, int tp) { this(p,rt,tp,Units.PKTS);}
		public MDataType_RTStats(NSPPort p, Route rt, int tp, int u)
		{
			super(ONLDaemon.NSPC, NSPMonClasses.NSP_RTSTATS, u);
			route = rt;
			if (tp == COUNTER) setIsRate(false);
			setONLComponent(p.getRouteTable());
			port = p.getID();
		}

		public boolean isEqual(MonitorDataType.Base mdt)
		{
			if (mdt instanceof MDataType_RTStats)
				return (super.isEqual(mdt) && route == ((MDataType_RTStats)mdt).route);
			else
				return false;
		}
		public String getParamTypeName() { return NSP_RTSTATS_LBL;}
		public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			route.writeXML(xmlWrtr);
		}

		public String getSpec()
		{
			return (new String(route.getPrefixMask().toString()));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			try 
			{
				Route.PrefixMask pm = new Route.PrefixMask(ln);
				if (onlComponent != null)
					route = ((RouteTable)onlComponent).getRoute(pm);
				else ExpCoordinator.print("MDataType_RTStats.loadFromSpec no component found", 6);
			}
			catch (java.text.ParseException e) { route = null; }
		}
		public void initFromReader(ONL.Reader rdr, int dt) throws IOException
		{
			super.initFromReader(rdr, dt);
			try 
			{
				Route.PrefixMask pm = new Route.PrefixMask(rdr.readString());
				if (onlComponent != null)
					route = ((RouteTable)onlComponent).getRoute(pm);
				else ExpCoordinator.print("MDataType_RTStats.loadFromSpec no component found", 6);
			}
			catch (java.text.ParseException e) { route = null; }
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeString(route.getPrefixMask().toString());
		}
		protected void setDescription()
		{
			setDescription(new String("Route Packet Count (" + route.toString() + ") on port " + port));
		}
		public Route getRoute() { return route;}
		//StatsMonitorable
		public int getStatsIndex() { return (route.getStatsIndex());}

		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new NSPMonClasses.MDataType_RTStats.XMLHandler(exp_xml, this));}
	}

	////////////////////////////////////////////// MonitorEntry //////////////////////////////////////////////////////////////////////////////////////////

	public static class MEntry_RTStats extends MonitorEntry.Base
	{
		public static class RTREND_Marker extends REND_Marker_class
		{
			public RTREND_Marker (MDataType_RTStats mdt)
			{
				super();
				port = mdt.getPort();
				op = NCCP.Operation_RTPktCounter;
				field3 = mdt.getRoute().getPrefixMask().getPrefixBytes();
			}
		}

		public MEntry_RTStats(MonitorEntry.Listener l, MonitorDataType.Base dt)
		{
			super(l);
			request = new NCCP_RTStatsRequest(dt.getPort(), ((MDataType_RTStats)dt).getRoute());
			response = new NCCP_RTStatsResponse();
			msr = true;
			setMarker(getNewMarker(dt));
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("ME::processResponse");
			Monitor monitor;
			int numParams = monitors.size();
			for (int i = 0; i < numParams; ++i)
			{
				monitor = (Monitor)monitors.elementAt(i);
				if (monitor != null)
				{
					//System.out.println("  calling monitor");
					if (!monitor.isStarted()) monitor.start();
					monitor.setData(r.getData(monitor.getDataType()), ((NCCP_LTDataResponse)r).getTimeInterval());
				}
			}
		}

		public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
		{
			return (new RTREND_Marker((MDataType_RTStats)mdt));
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////// Stats Index ///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////// Request /////////////////////////////////////////////////////////////////////////////////////


	public static class NCCP_StatsRequest extends NCCP.RequesterBase
	{  
		private int statsIndex;
		private int port;
		public NCCP_StatsRequest(int p, int ndx)
		{
			super(NCCP.Operation_ReadStats, true);
			statsIndex = ndx;
			port = p;
			setMarker(new REND_Marker_class());
			ExpCoordinator.print(new String("NCCP_StatsRequest port: " + port + " statsIndex:" + statsIndex), 5);
		}
		public void storeData(DataOutputStream dout) throws IOException
		{
			ExpCoordinator.print(new String("NCCP_StatsRequest::storeData port: " + port + " statsIndex:" + statsIndex), 5);
			dout.writeShort(port);
			dout.writeInt(statsIndex);
		}  
	}

	/////////////////////////////////////////////////// MDataType Interface /////////////////////////////////////////////////////////////////////////////
	public static interface StatsMonitorable
	{
		public int getStatsIndex();
		public int getPort();
	}

	//////////////////////////////////////////////////// Response ////////////////////////////////////////////////////////////////////////////////////////

	public static class NCCP_StatsResponse  extends  NCCP_PktCounterResponse
	{
		private double byteData = 0;
		private double obyteData = 0;
		public NCCP_StatsResponse() { super();}
		public NCCP_StatsResponse(DataInput din)  throws IOException 
		{ 
			super(din);
		}
		public void retrieveFieldData(DataInput din) throws IOException
		{
			super.retrieveFieldData(din);
			obyteData = byteData;
			byteData = NCCP.readUnsignedInt(din);
		}
		public double getData(MonitorDataType.Base mdt) 
		{ 
			if ((mdt.getDataUnits() & Units.PKTS) > 0) return (super.getData(mdt));
			else
			{
				if (mdt.isAbsolute())
					return byteData;
				else
				{
					if (obyteData > byteData) return (getRate(maxUnsignedInt() + (byteData - obyteData)));
					else return (getRate(byteData - obyteData));
				}
			}
		}
		public double getPackets() { return data;}
		public double getBytes() { return byteData;}
	}
	////////////////////////////////////////////// MonitorEntry //////////////////////////////////////////////////////////////////////////////////////////

	public static class MEntry_Stats extends MonitorEntry.Base
	{
		public MEntry_Stats(MonitorEntry.Listener l, MonitorDataType.Base dt)
		{
			super(l);
			request = new NCCP_StatsRequest(dt.getPort(), ((StatsMonitorable)dt).getStatsIndex());
			response = new NCCP_RTStatsResponse();
			msr = true;
			setMarker(getNewMarker(dt));
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("ME::processResponse");
			Monitor monitor;
			int numParams = monitors.size();
			for (int i = 0; i < numParams; ++i)
			{
				monitor = (Monitor)monitors.elementAt(i);
				if (monitor != null)
				{
					//System.out.println("  calling monitor");
					if (!monitor.isStarted()) monitor.start();
					monitor.setData(r.getData(monitor.getDataType()), ((NCCP_LTDataResponse)r).getTimeInterval());
				}
			}
		}

		public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
		{
			return (new REND_Marker_class(mdt.getPort(), false, NCCP.Operation_ReadStats, ((StatsMonitorable)mdt).getStatsIndex()));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public NSPMonClasses() {}

	public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
	{
		REND_Marker_class mrkr = null;
		switch(mdt.getParamType())
		{
		case NSPMonClasses.NSP_QLENGTH:
			mrkr = MEntry_QLength.getNewMarker(mdt);
			break;
		case NSPMonClasses.NSP_FPXCNTR:
			mrkr = MEntry_FPXCounter.getNewMarker(mdt);
			break;
		case NSPMonClasses.NSP_EMFILTER:
			mrkr = MEntry_Stats.getNewMarker(mdt);
			break;
		case NSPMonClasses.NSP_RTSTATS:
			mrkr = MEntry_Stats.getNewMarker(mdt);
			break;
		case NSPMonClasses.NSP_VOQLENGTH:
			mrkr = MEntry_VOQLength.getNewMarker(mdt);
			break;
		default:
			System.out.println("NSPMonClasses::getNewMarker unknown type");
		}

		return mrkr;
	}

	public static MonitorEntry.Base getNewMEntry(MonitorEntry.Listener l, MonitorDataType.Base mdt)
	{
		MonitorEntry.Base me = null;
		switch(mdt.getParamType())
		{
		case NSPMonClasses.NSP_QLENGTH:
			me = new MEntry_QLength(l, mdt);
			break;
		case NSPMonClasses.NSP_FPXCNTR:
			me = new MEntry_FPXCounter(l, mdt);
			break;
		case NSPMonClasses.NSP_EMFILTER:
			me = new MEntry_Stats(l, mdt);
			break;
		case NSPMonClasses.NSP_RTSTATS:
			me = new MEntry_Stats(l, mdt);
			break;
		case NSPMonClasses.NSP_VOQLENGTH:
			me = new MEntry_VOQLength(l, mdt);
			break;
		default:
			System.out.println("NSPMonClasses::getNewMEntry unknown type");
		}

		return me;
	}

	public static MonitorDataType.Base getMonitorDataType(int tp, SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
	{
		MonitorDataType.Base mdt = null;

		switch(tp)
		{
		case NSPMonClasses.NSP_QLENGTH:
			mdt = new MDataType_QLength(infile, lv, nlists);
			break;
		case NSPMonClasses.NSP_PKTCOUNTER:
			mdt = new MDataType_PktCounter(infile, lv, nlists);
			break;
		case NSPMonClasses.NSP_FPXCNTR:
			mdt = new MDataType_FPXCounter(infile, lv, nlists);
			break;
		case NSPMonClasses.NSP_EMFILTER:
			mdt = new MDataType_EMFilter(infile, lv, nlists);
			break;
		case NSPMonClasses.NSP_RTSTATS:
			mdt = new MDataType_RTStats(infile, lv, nlists);
			break;
		case NSPMonClasses.NSP_VOQLENGTH:
			mdt = new MDataType_VOQLength(infile, lv, nlists);
			break;
		default:
			System.out.println("NSPMonClasses::getMonitorDataType unknown type");
		}

		return mdt;
	}


	public static MonitorDataType.Base getMonitorDataType(int tp, ONL.Reader rdr, int dt) throws IOException
	{
		MonitorDataType.Base mdt = null;

		switch(tp)
		{
		case NSPMonClasses.NSP_QLENGTH:
			mdt = new MDataType_QLength(rdr, dt);
			break;
		case NSPMonClasses.NSP_PKTCOUNTER:
			mdt = new MDataType_PktCounter(rdr, dt);
			break;
		case NSPMonClasses.NSP_FPXCNTR:
			mdt = new MDataType_FPXCounter(rdr, dt);
			break;
		case NSPMonClasses.NSP_EMFILTER:
			mdt = new MDataType_EMFilter(rdr, dt);
			break;
		case NSPMonClasses.NSP_RTSTATS:
			mdt = new MDataType_RTStats(rdr, dt);
			break;
		case NSPMonClasses.NSP_VOQLENGTH:
			mdt = new MDataType_VOQLength(rdr, dt);
			break;
		default:
			System.out.println("NSPMonClasses::getMonitorDataType unknown type");
		}

		return mdt;
	}
}
