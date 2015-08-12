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
 * File: NCCP.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/23/2003
 *
 * Description: NCCP messaging class
 *
 * Modification History:
 *
 */
import java.io.*;


public class NCCP
{
	//enums

	////////////////////////////////////
	//NCCP_MessageType
	////////////////////////////////////
	public static final int MessageRequest = 1;
	public static final int MessageResponse = 2;
	public static final int MessagePeriodic = 3;
	public static final int MessageChat = 4;


	////////////////////////////////////
	//NCCP_Status 
	////////////////////////////////////
	public static final int Status_OperationIncomplete = 0;
	public static final int Status_Fine = 1;	//this means read operation is ok
	public static final int Status_TimeoutLocal = 2;
	public static final int Status_TimeoutRemote = 3;
	public static final int Status_StillRemaining = 4;
	public static final int Status_Failed = 5;


	////////////////////////////////////
	//NCCP_OperationType
	////////////////////////////////////
	public static final int Operation_NOP = 0;
	public static final int Operation_Ping = 1;
	public static final int Operation_ReadVCXT = 2;
	public static final int Operation_ReadVPXT = 3;
	public static final int Operation_WriteVCXT = 4;
	public static final int Operation_WriteVPXT = 5;
	public static final int Operation_ReadMR = 6;
	public static final int Operation_WriteMR = 7;
	public static final int Operation_ShutdownSC = 8;
	public static final int Operation_Reset = 9;
	public static final int Operation_TestCell0 = 10;
	public static final int Operation_TestCell1 = 11;
	public static final int Operation_TestCell2 = 12;
	public static final int Operation_ClearMR = 13;
	public static final int Operation_ClearMRAll = 14;
	public static final int Operation_ClearMRAllPPs = 15;
	public static final int Operation_ClearVCXT = 16;
	public static final int Operation_ClearVPXT = 17;
	public static final int Operation_ClearVXTAll = 18;
	public static final int Operation_ClearVXTAllPPs = 19;
	public static final int Operation_TestMR = 20;
	public static final int Operation_TestVPXT = 21;
	public static final int Operation_TestVCXT = 22;
	public static final int Operation_GetCells = 23;
	public static final int Operation_PutCells = 24;
	public static final int Operation_ReadVCXTCC = 25;
	public static final int Operation_ReadVPXTCC = 26;
	public static final int Operation_WriteVCXTCC = 27;
	public static final int Operation_WriteVPXTCC = 28;
	public static final int Operation_WriteVCXTTR = 29;
	public static final int Operation_WriteVPXTTR = 30;
	public static final int Operation_ReadErrors = 31;
	public static final int Operation_ClearErrors = 32;
	public static final int Operation_StopPeriodic = 33;
	public static final int Operation_AckPeriodic = 34;
	public static final int Operation_ReadStatus = 36;


	///////////////////////////////////////////////////////////////////////////////////////////////////////
	// MSR operations
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final int Operation_ReadDQ = 35;
	//public static final int Operation_ReadDRRWV = 37;
	//public static final int Operation_ReadLFSStatus = 38;
	//public static final int Operation_ReadDRR = 39;
	public static final int Operation_ReadPlugin = 41;


	//NSP operations (it's too late for the above ops but from now on use this as a base for NSP ops)
	public static final int Operation_NSP = 64;
	public static final int Operation_ReadGFCounter = 65; //General match filter packet/byte counter
	public static final int Operation_ReadEFCounter = 66; //Exact match filter packet/byte counter
	public static final int Operation_PortPacketCounter = 67; //Port packet/byte counter
	public static final int Operation_PortQLength = 68; //Qlength byte
	public static final int Operation_EMFilterPC = 69; //exact match filter packet counter
	public static final int Operation_SubPortPC = 70; //subport packet counter
	public static final int Operation_FPXCounter = 71; //general fpx counter
	public static final int Operation_WritePlugin = 72; 

	//route table ops
	public static final int Operation_AddFIPLRoute = 73; //general fpx counter
	public static final int Operation_AddRoute = 73; //general fpx counter
	public static final int Operation_UpdateFIPLNextHop = 74; //general fpx counter
	public static final int Operation_UpdateNextHop = 74; //general fpx counter
	public static final int Operation_DeleteFIPLRoute = 75; //general fpx counter
	public static final int Operation_DeleteRoute = 75; //general fpx counter

	public static final int Operation_AddFilter = 76; //general fpx counter
	public static final int Operation_DeleteFilter = 77; //general fpx counter
	public static final int Operation_AddEMFilter = Operation_AddFilter;//general fpx counter
	public static final int Operation_DeleteEMFilter = Operation_DeleteFilter; //general fpx counter
	public static final int Operation_ChangeQueue = 78;
	public static final int Operation_ChangeVOQ = 79;
	public static final int Operation_SwitchBW = 80;
	public static final int Operation_LinkBW = 81;
	public static final int Operation_ReadVOQLength = 82;
	public static final int Operation_AddPClasses = 83;
	public static final int Operation_UnloadPClass = 84;
	public static final int Operation_CreatePInstance = 85;
	public static final int Operation_DeletePInstance = 86;
	public static final int Operation_BindPInstance = 87;
	public static final int Operation_NPRPluginCounter = 87;
	public static final int Operation_UnbindPInstance = 88;
	public static final int Operation_PluginData = 89;
	public static final int Operation_EMFPriority = 90;
	public static final int Operation_RTPriority = 91;
	public static final int Operation_Debug = 92;
	public static final int Operation_PluginDebug = 92;
	public static final int Operation_RTPktCounter = 93;
	public static final int Operation_DQONOFF = 94;
	public static final int Operation_ReadStats = 95;
	public static final int Operation_SetFIPLStatsIndex = 96;
	public static final int Operation_SetStatsIndex = 96;
	public static final int Operation_SetSPCIPAddr = 97; //not sent from RLI
	public static final int Operation_FilterSampling = 98;



	///////////////////////////////////////////////////////////////////////////////////////////////////////
	// End node operations
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final int Operation_ENDNODE = 128;
	public static final int Operation_ReadUserData = 130;
	public static final int Operation_ReadStampedData = 131;



	///////////////////////////////////////////////////////////////////////////////////////////////////////
	// IPP & OPP Hardware Status/Error Bits
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final int IPP_HARDWARE_LNKEN = 0x40;
	public static final int IPP_HARDWARE_RESET =  0x20;
	public static final int IPP_HARDWARE_LNKDIS = 0x10;
	public static final int IPP_HARDWARE_PARITY = 0x08;
	public static final int IPP_HARDWARE_VXIOR =  0x04;
	public static final int IPP_HARDWARE_BADCC =  0x02;
	public static final int IPP_HARDWARE_BADSC =  0x01;
	public static final int OPP_HARDWARE_RESET =  0x10;
	public static final int OPP_HARDWARE_PARITY3 = 0x08;
	public static final int OPP_HARDWARE_PARITY2 = 0x04;
	public static final int OPP_HARDWARE_PARITY1 = 0x02;
	public static final int OPP_HARDWARE_PARITY0 = 0x01;
	public static final int OPP_HARDWARE_PARITY = 0x0f;



	///////////////////////////////////////////////////////////////////////////////////////////////////////
	// The link type field.  For now, don't have actual values, will get them from Mike Richards //un2byte
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final int GBNSC_ChipType_IPPversion1_0 = 0x01;	// no idea what these actually are
	public static final int GBNSC_ChipType_OPPversion1_0 = 0x02;	// get them from Saied
	public static final int GBNSC_ChipType_IPPversion1_1 = 0x03;	// just for testing -- remove later


	///////////////////////////////////////////////////////////////////////////////////////////////////////
	// The link type field.  For now, don't have actual values, will get them from Mike Richards //un1byte
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final int GBNSC_HWLinkType_155_SONET = 0;
	public static final int GBNSC_HWLinkType_620_SONET = 1;
	public static final int GBNSC_HWLinkType_2400_SONET = 2;
	public static final int GBNSC_HWLinkType_155_GLINK = 8;
	public static final int GBNSC_HWLinkType_620_GLINK = 9;
	public static final int GBNSC_HWLinkType_2400_GLINK = 10;


	/////////////////////////////////////////////
	// The maintenance register fields //un1byte
	/////////////////////////////////////////////
	public static final int GBNSC_MRegId_NONE =                0;

	public static final int GBNSC_MRegId_IPP_CHIPINFO = 	     1;
	public static final int GBNSC_MRegId_IPP_CONFIG = 	     2;
	public static final int GBNSC_MRegId_IPP_HARDWARE =	     3;
	public static final int GBNSC_MRegId_IPP_STATS_A = 	     4;
	public static final int GBNSC_MRegId_IPP_STATS_B = 	     5;
	public static final int GBNSC_MRegId_IPP_HARDWARE_RESET =  6;
	public static final int GBNSC_MRegId_IPP_HARDWARE_LNKDIS = 7;
	public static final int GBNSC_MRegId_IPP_HARDWARE_PARITY = 8;
	public static final int GBNSC_MRegId_IPP_HARDWARE_VXIOR =  9;
	public static final int GBNSC_MRegId_IPP_HARDWARE_BADCC =  10;
	public static final int GBNSC_MRegId_IPP_HARDWARE_BADSC =  11;

	public static final int GBNSC_MRegId_OPP_CHIPINFO = 	     12;
	public static final int GBNSC_MRegId_OPP_CONFIG = 	     13;
	public static final int GBNSC_MRegId_OPP_HARDWARE =	     14;
	public static final int GBNSC_MRegId_OPP_STATS_A = 	     15;
	public static final int GBNSC_MRegId_OPP_STATS_B = 	     16;
	public static final int GBNSC_MRegId_OPP_HARDWARE_RESET =  17;
	public static final int GBNSC_MRegId_OPP_HARDWARE_PARITY3 =	18;
	public static final int GBNSC_MRegId_OPP_HARDWARE_PARITY2 = 	19;
	public static final int GBNSC_MRegId_OPP_HARDWARE_PARITY1 = 	20;
	public static final int GBNSC_MRegId_OPP_HARDWARE_PARITY0 =	21;
	public static final int GBNSC_MRegId_OPP_BDC_CONTROL =        22;



	//end of enums





	/************************************************************************
	 **Base classes
	 ************************************************************************/


	/////////////////////
	//message interface
	/////////////////////
	public static interface Message
	{
		public void store(DataOutputStream dout) throws IOException ;
	}



	/////////////////////
	//NCCP.SendHeader
	/////////////////////
	public static class SendHeader implements Message
	{
		protected int op;
		protected double period_secs;
		protected double period_usecs;
		protected REND_Marker_class returnMarker;
		protected int periodic;
		protected PollingRate pollingRate;
		private boolean versionAdded = false;
		private int messageType = MessageRequest;

		protected SendHeader(int o, double p_secs, double p_usecs, REND_Marker_class mrkr, boolean addVersion)
		{
			op = o;
			if (addVersion) op = (o | ExpCoordinator.VERSION_BYTES);
			versionAdded = addVersion;
			period_secs = p_secs;
			period_usecs = p_usecs;
			returnMarker = mrkr;
			periodic = 1;
		}

		protected SendHeader(int o, boolean addVersion)
		{
			op = o;
			if (addVersion) op = (o | ExpCoordinator.VERSION_BYTES);
			ExpCoordinator.print(new String("RequesterBase op given " + o + " op set " + op), 2);
			versionAdded = addVersion;
			periodic = 0;
		}

		public void setPeriodic(boolean b)
		{
			if (b) periodic = 1;
			else periodic = 0;
		}

		public void setPeriod(PollingRate pr)    
		{
			if (pr != null)
			{
				period_secs = pr.getCSecs();
				period_usecs = pr.getCUSecs();
				if (period_secs > 0 || period_usecs > 0)
					periodic = 1;
				else periodic = 0;
			}
			else periodic = 0;
			//ExpCoordinator.printer.print("NCCP.RequesterBase::setPeriod " + period_secs + " " + period_usecs);
		}

		public void setMarker(REND_Marker_class mrkr)
		{
			returnMarker = mrkr;
		}

		//public void storeMessage(DataOutputStream dout) throws IOException { store(dout);}
		public void store(DataOutputStream dout) throws IOException  { storeHeader(dout);}
		public void storeHeader(DataOutputStream dout) throws IOException 
		{
			ExpCoordinator.print("NCCP.RequesterBase.storeHeader", 8);
			if (dout == null) ExpCoordinator.printer.print("NCCP.RequesterBase.storeHeader dout is null");
			dout.writeShort(messageType); //message type (2 bytes)
			dout.writeShort(op); //operation (2 bytes)
			if (returnMarker == null)
			{
				ExpCoordinator.print("NCCP.RequesterBase.storeHeader return marker is null", 8);
				returnMarker = new REND_Marker_class(op);
			}
			returnMarker.store(dout); //return marker (28 bytes)
			dout.writeShort(periodic); //is periodic (2 bytes this should really be 1 byte)
			if (isPeriodic())
			{ 
				dout.writeInt((int)period_secs); //period's secs (4 bytes)
				dout.writeInt((int)period_usecs); //period's usecs (4 bytes) 
			}
		}
		public void retrieveHeader(DataInputStream din) throws IOException 
		{
			ONL.NCCPReader rdr = new ONL.NCCPReader(din);
			ExpCoordinator.print("NCCP.RequesterBase.retrieveHeader", 8);
			if (returnMarker == null)
				returnMarker = new REND_Marker_class();
			returnMarker.retrieve(din); //return marker (28 bytes)
			periodic = rdr.readShort();
			if (isPeriodic())
			{ 
				period_secs = rdr.readInt();
				period_usecs = rdr.readInt(); //period's usecs (4 bytes) 
			}
		}
		public boolean isPeriodic() { return (periodic != 0);}
		public REND_Marker_class getMarker() { return returnMarker;}
		public double getPeriodSecs() { return period_secs;}
		public double getPeriodUSecs() { return period_usecs;}
		public void setMessageType(int s) { messageType = s;}
		public int getOp() 
		{ 
			if (versionAdded) return (op & 0x00ff); //remove version if used
			else return op;
		}
		protected void setOp(int o)
		{
			if (versionAdded)
			{
				int version = (op & 0xff00);
				op = version + o;
			}
			else op = o;
		}
	}

	/////////////////////
	//NCCP_RequesterBase
	/////////////////////
	public static abstract class RequesterBase extends SendHeader
	{ 
		protected RequesterBase(int o, double p_secs, double p_usecs, REND_Marker_class mrkr) { super(o, p_secs, p_usecs, mrkr, false);}
		protected RequesterBase(int o, double p_secs, double p_usecs, REND_Marker_class mrkr, boolean addVersion)
		{
			super(o, p_secs, p_usecs, mrkr, addVersion);
		}

		protected RequesterBase(int o) { super(o, false);}
		protected RequesterBase(int o, boolean addVersion) { super(o, addVersion);}


		public void store(DataOutputStream dout) throws IOException 
		{
			ExpCoordinator.print("NCCP.RequesterBase.store", 8);
			storeHeader(dout);
			storeData(dout);
		}
		public abstract void storeData(DataOutputStream dout) throws IOException ;
	}


	///////////////////////////////////////
	//NCCP_ResponseBase
	//////////////////////////////////////
	public static abstract class ResponseBase
	{
		protected int op;
		protected REND_Marker_class stopMarker;
		REND_Marker_class rend; //the marker of the monitor entry object
		protected int endOfMessage = 1;
		protected int periodic = 0;
		protected double status = Status_Fine;
		protected double oStatus = Status_Fine;
		protected PollingRate period = null;
		protected int dataLength = 0;
		private String opStr = "unknown op";
		private int version = 0;

		public ResponseBase(int dl, String os)
		{
			this(dl);
			opStr = new String(os);
		}
		public ResponseBase(int dl)
		{
			stopMarker = new REND_Marker_class();
			rend = new REND_Marker_class();
			dataLength = dl;
		}

		public ResponseBase(DataInput din, int dl) throws IOException
		{
			stopMarker = new REND_Marker_class();
			rend = new REND_Marker_class();
			retrieve(din);
			dataLength = dl;
		}

		protected void retrieveHeader(DataInput din) throws IOException
		{
			int tmp;
			REND_Marker_class tmp_mrker = new REND_Marker_class();
			//tmp = din.readUnsignedShort(); //message type (2 bytes)
			ExpCoordinator.print("NCCP.ResponseBase::retrieveHeader", 8);
			tmp = din.readUnsignedShort(); //operation (2 bytes)
			ExpCoordinator.print(new String("   tmp = " + tmp), 8);
			version = ((tmp & 0xff00) >> 2);
			op = tmp & 0x00ff;
			ExpCoordinator.print(new String("   op = " + op + " version = " + version), 8);
			rend.retrieve(din); //return marker (28 bytes)
			ExpCoordinator.print("  rend", 8);
			periodic = din.readUnsignedShort(); //is periodic (2 bytes this should really be 1 byte)
			ExpCoordinator.print(new String("   periodic = " + periodic), 8);
			if (periodic != 0)
			{ 
				//ExpCoordinator.printer.print("NCCP.ResponseBase::retrieveHeader -- getting stop marker");
				stopMarker.retrieve(din); //return marker (28 bytes)
				//stopMarker.print();
			}
			endOfMessage = din.readUnsignedShort(); //end of message (2 bytes)
			ExpCoordinator.print(new String("   endOfMessage = " + endOfMessage), 8);
			status = readUnsignedInt(din);
			ExpCoordinator.print(new String("   status = " + status), 8);
		}
		public void storeHeader(DataOutputStream dout) throws IOException 
		{
			ExpCoordinator.print("NCCP.ResponseBase.storeHeader", 8);
			if (dout == null) ExpCoordinator.printer.print("NCCP.ResponseBase.storeHeader dout is null");
			dout.writeShort(MessageResponse); //message type (2 bytes)
			dout.writeShort(op); //operation (2 bytes)
			rend.store(dout); //return marker (28 bytes)
			dout.writeShort(periodic); //is periodic (2 bytes this should really be 1 byte)
			if (isPeriodic())
			{ 
				stopMarker.store(dout);
			}
			dout.writeShort(endOfMessage);
			dout.writeInt((int)status);
		}

		public static double maxUnsignedInt() { return (2*((double)Integer.MAX_VALUE)); }
		public static double maxUnsignedShort() { return 65535; }

		public int getOp() { return op;}
		public int getVersion() { return version;}
		public REND_Marker_class getStopMarker() { return stopMarker;}
		public REND_Marker_class getRendMarker() { return rend;}
		public boolean isPeriodic() { return (periodic != 0);}
		public void setPeriodic(boolean b) 
		{
			if (b) periodic = 1;
			else periodic = 0;
		}
		public void setPeriod(PollingRate pr) 
		{
			period = pr;
			if (period != null) setPeriodic(true);
		}
		public PollingRate getPeriod() { return period;}
		public boolean isEndOfMessage() { return (endOfMessage != 0);}
		public void setEndOfMessage(int eom) { endOfMessage = eom;}
		public double getStatus() { return status;}
		public void setStatus(double s) { status = s;}
		public void setHeader(ResponseBase r)
		{
			//ExpCoordinator.printer.print("NCCP.Response::setHeader");
			stopMarker.assign(r.stopMarker);
			op = r.op;
			periodic = r.periodic;
			rend.assign(r.rend);
			endOfMessage = r.endOfMessage;
			status = r.status;
		}

		public void retrieve(DataInput din) throws IOException
		{
			retrieveHeader(din);
			retrieveData(din);
		}
		public void print() { print(0);}
		public void print(int level)
		{ 
			ExpCoordinator.printer.print(new String("NCCP.Response op:" + getString() + " status:" + status + " periodic:" + periodic + " end:" + endOfMessage), level);
			ExpCoordinator.printer.print("Rend Marker:", level);
			rend.print(level);
			ExpCoordinator.printer.print("Stop Marker:", level);
			stopMarker.print(level);
		}

		public void retrieveData(DataInput din) throws IOException
		{
			if (status == Status_Fine || status == Status_StillRemaining) retrieveFieldData(din);
			else 
			{
				System.err.println("NCCP.ResponseBase status bad " + status + " skipping " + dataLength);
				print();
				din.skipBytes(dataLength);
			}
			oStatus = status;
		}
		public String getString()
		{	
			switch (op)
			{
			case Operation_ReadMR:
				opStr = "Read MR";
				break;
			case Operation_ReadVCXTCC:
				opStr = "Read VC cc";
				break;
			case Operation_ReadVPXTCC:
				opStr = "Read VC cc";
				break;
			case Operation_ReadDQ:
				opStr = "Read DQ";
				break;
			case Operation_ReadStatus:
				opStr = "Read Status";
				break;
			}
			return opStr;
		}
		public abstract void retrieveFieldData(DataInput din) throws IOException;
		public abstract double getData(MonitorDataType.Base mdt);
	}

	public static class ErrorMsgResponse extends ResponseBase
	{
		private String errorMsg = "";
		public ErrorMsgResponse(String os)
		{
			super(0, os);
		}
		public ErrorMsgResponse()
		{
			super(0);
		}
		public ErrorMsgResponse(DataInput din) throws IOException
		{
			super(0);
			retrieveHeader(din);
		}
		public void retrieveFieldData(DataInput din) throws IOException {}
		public double getData(MonitorDataType.Base mdt) {return 0;}
		public String getErrorMsg() { return errorMsg;} 
		protected void retrieveHeader(DataInput din) throws IOException
		{
			super.retrieveHeader(din);
			errorMsg = readString(din);
		}
	}

	public static class ResponseHeader extends ResponseBase
	{
		public ResponseHeader()
		{
			super(0);
		}
		public ResponseHeader(DataInput din) throws IOException
		{
			super(0);
			retrieveHeader(din);
		}
		public void retrieveFieldData(DataInput din) throws IOException {}
		public double getData(MonitorDataType.Base mdt) {return 0;}
	}

	/////////////////////////////////////////////////////////////////////////
	// NCCP_LTResponseBase - supports local time & timeInterval calculations
	/////////////////////////////////////////////////////////////////////////
	public static abstract class LTResponseBase extends ResponseBase 
	{
		protected double    lt = 0;     // INFO fields: Local Time
		protected double    clkRate;     // ClockRate
		protected boolean reset = false;
		protected double timeInterval = 0;
		protected double realTimeInterval = 0; //use for all internal calculations will compensate when a response is dropped

		//local time of last response
		protected double   oLT = -1;

		protected LTResponseBase(int dl) 
		{
			super(dl);
		}

		protected LTResponseBase(DataInput din, int dl) throws IOException
		{
			super(din, dl);
		}

		public double getClkRate() { return clkRate;}
		public double getLT() { return lt;}
		public double getTimeInterval() { return timeInterval;}
		protected double getRTimeInterval() { return realTimeInterval;}
		public void setLT(double sw_lt)
		{
			oLT = lt;
			lt = sw_lt;
			if (oLT > 0) 
			{
				if (lt > oLT)
					timeInterval = ((lt - oLT) * 16)/clkRate;
				else //lt wrapped
				{
					//ExpCoordinator.printer.print("NCCP.LTResponseBase::setLT lt wrapped");
					timeInterval = (((maxUnsignedInt() + lt) - oLT) * 16)/clkRate;
				}

				realTimeInterval = timeInterval;

				//if we dropped the last response make the reported timeInterval what is expected
				if (oStatus != Status_Fine) timeInterval = period.getSecsOnly();

				//this assumes that the switch has reset and we've gotten a ridiculous number
				if (period != null && (timeInterval > (period.getSecsOnly() + 2))) 
				{
					ExpCoordinator.printer.print("NCCP.LTResponseBase::reset happened", 10);
					print(10);
					reset = true;
					timeInterval = period.getSecsOnly();
					realTimeInterval = timeInterval;
				}
				else reset = false;
			}
			else reset = true; //not a switch reset but the first response we've gotten
		}
		public void printTimeInterval()
		{
			ExpCoordinator.printer.print("NCCP.ReadMRResponseBase::getTimeInterval clkRate = " + clkRate + " time interval = " + getTimeInterval());
		}
		public void print() { print(0);}
		public void print(int level)
		{
			super.print(level);
			ExpCoordinator.printer.print(new String(" NCCP.ReadLTResponseBase  (clkRate, timeInterval, rtimeInterval, lt, olt) = ( " + clkRate + ", " + getTimeInterval() + ", " + realTimeInterval + ", " + lt + ", " + oLT + ")"), level);
		}
		public boolean isReset() { return reset;} 
		protected double convertCCtoMbs(double val)
		{
			return ((val * 424)/realTimeInterval);
		}
		protected double getRate(double val) { return (val/realTimeInterval);}
	}

    ////////////////////////////////////////////////////////////////////////////
    ///////////////// LTDataResponse ///////////////////////////////////////////
    ///////////////////LT response w/clkRate and one 4 byte field///////////////
    ////////////////////////////////////////////////////////////////////////////

	public static class LTDataResponse extends NCCP.LTResponseBase //LT response w/clkRate and one 4 byte field 
	{
		protected double data = 0;
		protected double odata = 0;
		protected double clkRateScaler = 1000000;
		protected LTDataResponse() { super(12);}
		protected LTDataResponse(int dl) { super(dl + 12);}
		public LTDataResponse(DataInput din) throws IOException
		{
			super(din, 12);
		}
		public LTDataResponse(DataInput din, int dl) throws IOException
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
					System.out.println("NCCP.LTDataResponseBase::reset happened");
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


	///////////////////////////////////////
	//NCCP_PeriodicMessage
	//////////////////////////////////////
	public static class PeriodicMessage implements Message
	{
		protected REND_Marker_class rend;
		protected REND_Marker_class returnRend;
		protected int op;
		public PeriodicMessage(DataInput din) throws IOException
		{
			rend = new REND_Marker_class();
			returnRend = new REND_Marker_class();
			retrieveMessage(din);
		}
		public PeriodicMessage(int o, REND_Marker_class rnd, REND_Marker_class rtn_rnd)
		{
			rend = rnd;
			returnRend = rtn_rnd;
			op = o;
		}
		public void retrieveMessage(DataInput din) throws IOException
		{
			//tmp = din.readUnsignedShort(); //message type (2 bytes)
			op = din.readUnsignedShort(); //operation (2 bytes)
			rend.retrieve(din); //my marker (28 bytes)
			returnRend.retrieve(din);//return marker (28 bytes)
		}
		public void store(DataOutputStream dout) throws IOException
		{
			dout.writeShort(MessagePeriodic); //message type (2 bytes)
			dout.writeShort(op); //operation (2 bytes)
			rend.store(dout); //receiver marker (28 bytes)
			returnRend.store(dout); //sender marker (28 bytes)
		}
		public boolean isAck() { return (op == NCCP.Operation_AckPeriodic);}
		public boolean isStop() { return (op == NCCP.Operation_StopPeriodic);}
		public REND_Marker_class getRendMarker() { return rend;}
		public REND_Marker_class getReturnRendMarker() { return returnRend;}
		public void print() { print(0);}
		public void print(int level) 
		{
			ExpCoordinator.printer.print(new String("NCCP.MessagePeriodic " + MessagePeriodic + " " + op ), level);
			ExpCoordinator.printer.print("Rend", level);
			rend.print(level);
			ExpCoordinator.printer.print("Return Rend", level);
			returnRend.print(level);
		}
		public int getOp() { return op;}
	}

	/////////////////////////////////////////////////////////
	//NCCP_StopRequester
	////////////////////////////////////////////////////////
	public static class StopRequester extends RequesterBase
	{
		protected REND_Marker_class stopMarker;

		public StopRequester(REND_Marker_class mrkr, REND_Marker_class stpmrkr)
		{
			super(Operation_StopPeriodic);
			returnMarker = mrkr;
			stopMarker = stpmrkr;
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			stopMarker.store(dout);
		}
	}

	//end Base classes



	public static String readString(DataInput dIn) throws IOException
	{
		double length = readUnsignedInt(dIn);
		byte[] data = new byte[(int)length];
		dIn.readFully(data);
		String rtn = new String(data);
		return rtn;
	}
	public static void writeString(String str, DataOutput dout) throws IOException
	{
		if (str != null)
		{
			double length = str.length();
			byte[] data = str.getBytes();
			dout.writeInt((int)length);
			dout.write(data);
		}
		else dout.writeInt(0);
	}
	public static void writeComponentID(ONLComponent c, DataOutput dout) throws IOException
	{ 
		ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
		if (c != null) 
		{ 
			wrtr.writeString(c.getBaseType());
			ExpCoordinator.print(new String("NCCP.writeComponentID baseType:" + c.getBaseType()), 5);
			wrtr.writeInt(c.getReference());
			wrtr.writeInt(c.getClusterReference());
			if (c.getCommittedLabel() != null)
			{
				ExpCoordinator.print(new String("NCCP.writeComponentID " + c.getCommittedLabel()), 5);
				wrtr.writeString(c.getCommittedLabel());
			}
			else  
			{
				ExpCoordinator.print(new String("NCCP.writeComponentID " + c.getLabel()), 5);
				wrtr.writeString(c.getLabel());
			}
			wrtr.writeString(c.getCPAddr());
			wrtr.writeBoolean(c.isRouter());
		}
		else 
		{
			wrtr.writeString("");
			wrtr.writeInt(0);
			wrtr.writeInt(0);
			wrtr.writeString("");
			wrtr.writeString("");
			wrtr.writeBoolean(false);
		}
	}

	public static double readUnsignedInt(DataInput din) throws IOException
	{
		double rtn = (double)din.readInt();
		if (rtn < 0)
		{
			rtn += 2*((double)Integer.MAX_VALUE);
		}
		return rtn;
	}
}
