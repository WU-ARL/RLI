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
 * File: Reservation.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 5/31/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;

import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.GridLayout;
import java.text.*;
//import javax.swing.text.*;
import java.awt.Font;
import javax.swing.event.*;
//import java.beans.*;
//import javax.accessibility.AccessibleContext;

class Reservation implements NCCPConnection.ConnectionListener
{
	//error codes - add as defined
	public static final int AUTH_ERR = 1;
	public static final int NO_RES_ERR = 2;
	public static final int ALLOC_ERR = 3;
	public static final int EXPIRED = 4;
	public static final int ALERT = 5;
	public static final int ACK = 8;

	private ExpCoordinator expCoordinator = null;
	private GregorianCalendar earlyStartTime = null;
	private GregorianCalendar lateStartTime = null;
	private double duration = 0; //duration in minutes
	private SimpleDateFormat dateFormat = null;
	protected static TimeZone cstTimeZone = null;

	protected static int index = 0;

	private NCCPRequest pendingRequest = null;


	////////////////////////////////////////////// Reservation.Reservable interface /////////////////////////////////////////////////////////////////////
	public interface Reservable
	{
		public boolean isReserved();
	}

	/////////////////////////////////////////////////////// NCCPRequest ///////////////////////////////////////////////////////////////////////
	private class NCCPRequest extends ExpRequest
	{
		private Vector topologyRequests;
		private String ref_id = "";
		///////////////////////////////////////////////  NCCP_Requester /////////////////////////////////////////////////////////////
		private class NCCP_Requester extends ExpRequest.RequesterBase
		{
			public NCCP_Requester()
			{
				super(ExpDaemon.NCCP_Operation_ReservationRequest, true);
				setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_ReservationRequest, getNextIndex()));
			}

			public void storeData(DataOutputStream dout) throws IOException
			{
				//first change time to UTC or GMT
				//long early = earlyStartTime.getTimeInMillis();
				//long late = lateStartTime.getTimeInMillis();
				//ExpCoordinator.print(new String("Reservation.NCCPRequest.NCCP_Requester.storeData earlyStartTime: " + early + "ms lateStartTime: " + late + "ms duration: " + duration + "min"), 2);
				GregorianCalendar tmp_cal = new GregorianCalendar(cstTimeZone);
				tmp_cal.setTime(earlyStartTime.getTime());
				String early = dateFormat.format(tmp_cal.getTime());
				tmp_cal.setTime(lateStartTime.getTime());
				String late = dateFormat.format(tmp_cal.getTime());
				ExpCoordinator.print(new String("Reservation.NCCPRequest.NCCP_Requester.storeData earlyStartTime: " + early + " lateStartTime: " + late + " duration: " + duration + "min"), 2);

				ONL.NCCPWriter nccp_writer = new ONL.NCCPWriter(dout);
				//nccp_writer.writeString("experiment");
				nccp_writer.writeString(expCoordinator.getProperty(ExpCoordinator.USERNAME));
				nccp_writer.writeString(expCoordinator.getProperty(ExpCoordinator.PASSWORD));
				//expCoordinator.getCurrentExp().writeTopologySummary(nccp_writer);
				nccp_writer.writeString(early);
				nccp_writer.writeString(late);
				nccp_writer.writeInt((int)duration);
				nccp_writer.writeInt(expCoordinator.getCurrentExp().getNumCfgObjs());
			}
		}//end inner class Reservation.NCCPRequest.NCCP_Requester

		///////////////////////////////////////////////  NCCP_Response //////////////////////////////////////////////////////////////
		private class NCCP_Response extends NCCP.ResponseBase
		{
			private String errorMsg;
			private int errorCode;
			private String startTime = null;

			public NCCP_Response() { super(4);}
			public void retrieveData(DataInput din) throws IOException
			{     
				errorMsg = NCCP.readString(din);
				errorCode = din.readInt();
				startTime = NCCP.readString(din);
				if (status != NCCP.Status_Fine)
					ExpCoordinator.printer.print(new String("Reservation.NCCPRequest.NCCP_Response.retrieveData msg = " + errorMsg), 2);
			}
			public void retrieveFieldData(DataInput din) throws IOException {}
			public double getData(MonitorDataType.Base mdt) { return 0;}
			public String getErrorMessage() { return errorMsg;}
			public int getErrorCode() { return errorCode;}
			public Date getStartTime() 
			{ 
				try
				{
					return (dateFormat.parse(startTime));
				}
				catch (ParseException e)
				{
					return null;
				}
			}
		}//end inner class Reservation.NCCPRequest.NCCP_Response

		///////////////////////////////////////////////  NCCP_EndReservationReq /////////////////////////////////////////////////////////////
		private class NCCP_EndReservationReq extends  NCCP.RequesterBase
		{
			private String refid = "";
			public NCCP_EndReservationReq(String ref)
			{
				super(ExpDaemon.NCCP_Operation_EndReservationReq);
				setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_EndReservationReq, getNextIndex()));
				refid = new String(ref);
			}
			public void storeData(DataOutputStream dout) throws IOException
			{
				NCCP.writeString(refid, dout);
				//NCCP.writeComponentID(null, dout);
			}
		}//end inner class Reservation.NCCPRequest.NCCP_Requester

		/////////////////////////////////////////////////////////////////////////////////

		public NCCPRequest(NCCPCancelReq req)
		{
			request = req;
			response = new NCCP_Response();
			topologyRequests = new Vector();
		}

		public NCCPRequest(NCCPExtensionReq req)
		{
			request = req;
			response = new NCCP_Response();
			topologyRequests = new Vector();
		}

		public NCCPRequest()
		{
			request = new NCCP_Requester();
			response = new NCCP_Response();
			topologyRequests = new Vector();
		}

		public void processResponse(NCCP.ResponseBase r)
		{
			boolean remove = true;
			int i =0;
			int max = 0;
			if (r.getStatus() == NCCP.Status_Fine) 
			{
				Date d = ((NCCP_Response)r).getStartTime();

				//check requests were all answered
				//remove all requests from ExpCoordinator
				Vector failedRequests = new Vector();
				max = topologyRequests.size();
				Reservable req;
				for (i = 0; i < max; ++i)
				{
					req = (Reservable)topologyRequests.elementAt(i);
					if (!req.isReserved()) failedRequests.add(req);
					expCoordinator.removeRequest((ExpRequest)req);
				}
				if (failedRequests.size() > 0)
				{
					JOptionPane.showMessageDialog(expCoordinator.getMainWindow(), 
							new Object[]{"Incomplete Topology Reserved."},
							"Reservation Error",
							JOptionPane.ERROR_MESSAGE);
					failedRequests.clear();
				}
				topologyRequests.clear();

				if (request instanceof NCCPExtensionReq)
				{
					JOptionPane.showMessageDialog(expCoordinator.getMainWindow(), 
							new Object[]{((NCCP_Response)r).getErrorMessage()},
							"Reservation",
							JOptionPane.PLAIN_MESSAGE);
				}
				else if (request instanceof NCCPCancelReq)
				{
					JOptionPane.showMessageDialog(expCoordinator.getMainWindow(), 
							new Object[]{((NCCP_Response)r).getErrorMessage()},//"Cancel Reservation Succeeded"},
							"Reservation",
							JOptionPane.PLAIN_MESSAGE);
				}
				else
				{
					String date_time = "";
					if (d != null) date_time = DateFormat.getDateTimeInstance().format(d);
					JOptionPane.showMessageDialog(expCoordinator.getMainWindow(), 
							new Object[]{((NCCP_Response)r).getErrorMessage(), (new String("Reservation starts " + date_time))},
							"Reservation",
							JOptionPane.PLAIN_MESSAGE);
				}
			}
			else
			{
				if ((((NCCP_Response)r).getErrorCode() & AUTH_ERR) > 0)
				{
					expCoordinator.clearPassword();
					expCoordinator.getUserInfo("Authentication Failure");
					remove = false;
					expCoordinator.sendRequest(this);
				}
				else
				{
					if ((((NCCP_Response)r).getErrorCode() & ACK) > 0)
					{
						String oref_id = ref_id;
						ref_id = new String(((NCCP_Response)r).getErrorMessage());
						ExpCoordinator.print(new String("Reservation.NCCPRequest.processResponse got ACK for reservation id:" + ref_id + " set from " + oref_id));
						remove = false;
						//form and send requests
						Topology topo = expCoordinator.getTopology();
						Vector clusters = topo.getClusters();
						ONLComponentList cl;
						max = clusters.size();
						Cluster.Instance cluster;
						Experiment ex = expCoordinator.getCurrentExp();
						Cluster.Instance.NCCP_AddCluster addcluster;
						for (i = 0; i < max; ++i)
						{
							cluster = (Cluster.Instance)clusters.elementAt(i);
							ExpCoordinator.print(new String("Reservation.NCCPRequest.processResponse send cluster " + cluster.getTypeLabel() + " ref " + cluster.getIndex() + " i = " + i), 6);
							addcluster = new Cluster.Instance.NCCP_AddCluster(cluster, ex);
							addcluster.setReservation(ref_id, ExpDaemon.NCCP_Operation_ReservationCluster);
							topologyRequests.add(addcluster);
							expCoordinator.sendRequest(addcluster);
						}
						cl = topo.getNodes();
						max = cl.getSize();
						ONLComponent c;
						Experiment.NCCP_AddComponent addc;
						for (i = 0; i < max; ++i)
						{
							c = cl.onlComponentAt(i);
							addc = new Experiment.NCCP_AddComponent(c,ex);
							addc.setReservation(ref_id, ExpDaemon.NCCP_Operation_ReservationComponent);
							topologyRequests.add(addc);
							expCoordinator.sendRequest(addc);
						}
						cl = topo.getLinks();
						max = cl.getSize();
						LinkDescriptor.NCCP_AddLink addl;
						for (i = 0; i < max; ++i)
						{
							c = cl.onlComponentAt(i);
							addl = new LinkDescriptor.NCCP_AddLink(((LinkDescriptor)c),ex);
							addl.setReservation(ref_id, ExpDaemon.NCCP_Operation_ReservationLink);
							topologyRequests.add(addl);
							expCoordinator.sendRequest(addl);
						}
						//end with end reservation operation
						expCoordinator.sendMessage(new NCCP_EndReservationReq(ref_id));
					}
					else
						JOptionPane.showMessageDialog(expCoordinator.getMainWindow(), 
								new Object[]{((NCCP_Response)r).getErrorMessage()},
								"Reservation Error",
								JOptionPane.ERROR_MESSAGE);
				}
			}
			if (remove || (request instanceof NCCPCancelReq))
				expCoordinator.removeRequest(this);
		}
	}//end inner class Reservation.NCCPRequest

	///////////////////////////////////////////////// Action ///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class Action extends AbstractAction implements ExpCoordinator.MenuAction
	{
		private Reservation reservation;
		private ExpCoordinator expCoordinator = null;
		public Action(ExpCoordinator ec)
		{
			super("Make Reservation");
			reservation = new Reservation(ec);
			expCoordinator = ec;
		}
		public void actionPerformed(ActionEvent e)
		{
			if (!expCoordinator.getCurrentExp().isEmpty()) reservation.getReservation();
			else JOptionPane.showMessageDialog(expCoordinator.getMainWindow(),
					"You need to specify a topology.",
					"Reservation Error",
					JOptionPane.ERROR_MESSAGE);

		}
		//ExpCoordinator.MenuAction interface
		public int getType() { return ExpCoordinator.RESERVATION;}
		public boolean isType(int t) { return (t == ExpCoordinator.RESERVATION);}
		//end ExpCoordinator.MenuAction interface
	}//end inner class Reservation.Action

	///////////////////////////////////////////////// ExtendAction ///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class ExtendAction extends AbstractAction implements ExpCoordinator.MenuAction
	{
		private Reservation reservation;
		private ExpCoordinator expCoordinator = null;
		public ExtendAction(ExpCoordinator ec)
		{
			super("Extend Current Reservation");
			//setEnabled(false);
			reservation = new Reservation(ec);
			expCoordinator = ec;
		}
		public void actionPerformed(ActionEvent e)
		{
			reservation.getExtension();
		}
		//ExpCoordinator.MenuAction interface
		public int getType() { return ExpCoordinator.EXT_RESERVATION;}
		public boolean isType(int t) { return (t == ExpCoordinator.EXT_RESERVATION);}
		//end ExpCoordinator.MenuAction interface
	}//end inner class Reservation.ExtendAction

	///////////////////////////////////////////////// CancelAction ///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class CancelAction extends AbstractAction implements ExpCoordinator.MenuAction
	{
		private Reservation reservation;
		private ExpCoordinator expCoordinator = null;
		public CancelAction(ExpCoordinator ec)
		{
			super("Cancel Current Reservation");
			//setEnabled(false);
			reservation = new Reservation(ec);
			expCoordinator = ec;
		}
		public void actionPerformed(ActionEvent e)
		{
			int rtn = JOptionPane.showConfirmDialog(ExpCoordinator.getMainWindow(), 
					new String("Do you wish to cancel your current reservation?"),
					new String("Cancel Reservation"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (rtn == JOptionPane.YES_OPTION)
			{
				reservation.cancelReservation();
			}
		}
		//ExpCoordinator.MenuAction interface
		public int getType() { return ExpCoordinator.CANCEL_RESERVATION;}
		public boolean isType(int t) { return (t == ExpCoordinator.CANCEL_RESERVATION);}
		//end ExpCoordinator.MenuAction interface
	}//end inner class Reservation.CancelAction

	//////////////////////////////////////////////// DateField/////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class DateField extends JPanel
	{
		private GregorianCalendar date = null;
		private JComboBox month = null;
		private JComboBox day = null;
		private JComboBox hour = null;
		private JComboBox min = null;
		private JComboBox year = null;
		private JComboBox am_pm = null;

		public DateField(GregorianCalendar d) { this(d, false);}
		public DateField(GregorianCalendar d, boolean later)
		{
			super();
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			date = d;
			month = new JComboBox(new Object[]{"1","2","3","4","5","6","7","8","9","10","11","12"});
			month.setSelectedIndex(date.get(Calendar.MONTH));//-1);
			day = new JComboBox(new Object[]{"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31"});
			day.setSelectedIndex(date.get(Calendar.DAY_OF_MONTH)-1);
			int curr_year = date.get(Calendar.YEAR);
			int tmp_min = date.get(Calendar.MINUTE);
			int curr_min = 0;
			if (tmp_min >= 15) ++curr_min;
			if (tmp_min >= 30) ++curr_min;
			if (tmp_min >= 45) ++curr_min;
			year = new JComboBox(new Object[]{String.valueOf(curr_year), String.valueOf(curr_year + 1)});
			hour = new JComboBox(new Object[]{"12","1","2","3","4","5","6","7","8","9","10","11"});
			int curr_hr = date.get(Calendar.HOUR);
			boolean am = (date.get(Calendar.AM_PM) == Calendar.AM);
			if (later)
			{
				++curr_hr;
				if (curr_hr > 11) 
				{
					curr_hr = 0;
					am = !am;
				}
			}

			hour.setSelectedIndex(curr_hr);
			min = new JComboBox(new Object[]{"0","15","30","45"});
			min.setSelectedIndex(curr_min);
			am_pm = new JComboBox(new Object[]{"AM", "PM"});
			if (!am) am_pm.setSelectedIndex(1);

			JPanel tmp_panel = new JPanel();
			JLabel tmp_lbl = null;
			tmp_panel.setLayout(new GridLayout(2, 1));
			tmp_lbl = new JLabel("month");
			Font f = tmp_lbl.getFont().deriveFont(Font.PLAIN);
			tmp_lbl.setFont(f);
			tmp_panel.add(tmp_lbl);
			tmp_panel.add(month);
			add(tmp_panel);

			tmp_panel = new JPanel();
			tmp_panel.setLayout(new GridLayout(2, 1));
			tmp_lbl = new JLabel("day");
			tmp_lbl.setFont(f);
			tmp_panel.add(tmp_lbl);
			tmp_panel.add(day);
			add(tmp_panel);

			tmp_panel = new JPanel();
			tmp_panel.setLayout(new GridLayout(2, 1));
			tmp_lbl = new JLabel("year");
			tmp_lbl.setFont(f);
			tmp_panel.add(tmp_lbl);
			tmp_panel.add(year);
			add(tmp_panel);

			tmp_panel = new JPanel();
			tmp_panel.setLayout(new GridLayout(2, 1));
			tmp_lbl = new JLabel("hour");
			tmp_lbl.setFont(f);
			tmp_panel.add(tmp_lbl);
			tmp_panel.add(hour);
			add(tmp_panel);

			tmp_panel = new JPanel();
			tmp_panel.setLayout(new GridLayout(2, 1));
			tmp_lbl = new JLabel("min");
			tmp_lbl.setFont(f);
			tmp_panel.add(tmp_lbl);
			tmp_panel.add(min);
			add(tmp_panel);

			tmp_panel = new JPanel();
			tmp_panel.setLayout(new GridLayout(2, 1));
			tmp_panel.add(new JLabel(""));
			tmp_panel.add(am_pm);
			add(tmp_panel);
		}

		public void computeTime()
		{
			ExpCoordinator.print(new String("computeTime: " + date.get(Calendar.MONTH) + " " + date.get(Calendar.DAY_OF_MONTH) + " " + date.get(Calendar.YEAR) + " " + date.get(Calendar.HOUR) + " " + date.get(Calendar.MINUTE) + " " + date.get(Calendar.AM_PM) + " ms:" + date.getTimeInMillis()), 2);
			int hr = hour.getSelectedIndex();
			if (am_pm.getSelectedIndex() == 1)// && hr != 12))
			{
				ExpCoordinator.print(new String("     is pm hr:" + hr), 2);
				hr += 12;
			}

			if (am_pm.getSelectedIndex() == 0)// && hr == 12))
			{
				ExpCoordinator.print(new String("     is am hr:" + hr), 2);
				//hr = 0;
			}

			date.set(Integer.parseInt((String)year.getSelectedItem()),
					(month.getSelectedIndex()), // + 1),
					(day.getSelectedIndex() + 1),
					hr,
					(min.getSelectedIndex() * 15));
			ExpCoordinator.print(new String("     after setting: " + date.get(Calendar.MONTH) + " " + date.get(Calendar.DAY_OF_MONTH) + " " + date.get(Calendar.YEAR) + " " + date.get(Calendar.HOUR_OF_DAY) + " " + date.get(Calendar.MINUTE) + " " + date.get(Calendar.AM_PM) + " ms:" + date.getTimeInMillis()), 2);
		} 
	}//end inner class DateField



	public static class DurationField extends JPanel
	{
		private JFormattedTextField hours = null;
		private JComboBox min = null;

		public DurationField(int h)
		{
			this();
			hours.setValue(h);
		}
		
		public DurationField()
		{
			super();
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			JLabel tmp_lbl = new JLabel("hours:");
			Font f = tmp_lbl.getFont().deriveFont(Font.PLAIN);
			tmp_lbl.setFont(f);
			add(tmp_lbl);
			NumberFormat nf = NumberFormat.getIntegerInstance();
			//nf.setParseIntegerOnly(true);
			//((DecimalFormat)nf).setNegativePrefix("");
			hours = new TextFieldPlus.NumberTextField(nf);
			hours.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
			add(hours);
			min = new JComboBox(new Object[]{"0", "15", "30", "45"}); 
			tmp_lbl = new JLabel("min:");
			tmp_lbl.setFont(f);
			add(tmp_lbl);
			add(min);
		}
		public int getDuration()
		{
			int tmp_min = min.getSelectedIndex() * 15;
			if (hours.getText().length() > 0)
				tmp_min += (Integer.parseInt(hours.getText()) * 60);
			return tmp_min;
		}
	}//end inner class DurationField

	///////////////////////////////////////////////// NCCPCancelReq ///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private class NCCPCancelReq extends ExpRequest.RequesterBase//ONLUndoManager.NCCP_Requester
	{
		public NCCPCancelReq()
		{
			super(ExpDaemon.NCCP_Operation_CancelRes, true);
			setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_CancelRes, getNextIndex()));
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			ExpCoordinator.print("Reservation.NCCPCancelReq.storeData", 4);
			ONL.NCCPWriter nccp_writer = new ONL.NCCPWriter(dout);
			nccp_writer.writeString(expCoordinator.getProperty(ExpCoordinator.USERNAME));
			nccp_writer.writeString(expCoordinator.getProperty(ExpCoordinator.PASSWORD));
		}
	}//end inner class NCCPCancelReq

	///////////////////////////////////////////////// NCCPExtensionReq ///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private class NCCPExtensionReq extends ExpRequest.RequesterBase
	{
		private int minutes = 0;

		public NCCPExtensionReq(Experiment e, int min)
		{
			super(ExpDaemon.NCCP_Operation_ResExtension, true);
			minutes = min;
			setMarker(new REND_Marker_class(ExpDaemon.NCCP_Operation_ResExtension, getNextIndex()));
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			//super.storeData(dout);
			NCCP.writeString(ExpCoordinator.theCoordinator.getProperty(ExpCoordinator.USERNAME), dout);
			NCCP.writeString(ExpCoordinator.theCoordinator.getProperty(ExpCoordinator.PASSWORD), dout); 
			dout.writeInt(minutes);
		}
	}//end inner class NCCPExtensionReq

	public Reservation(ExpCoordinator ec)
	{
		expCoordinator = ec;
		earlyStartTime = new GregorianCalendar();
		lateStartTime = new GregorianCalendar();
		ExpCoordinator.print(new String("Reservation earlyStartTime timezone " + earlyStartTime.getTimeZone().getID()));
		if (cstTimeZone == null) 
		{
			cstTimeZone = TimeZone.getTimeZone("America/Chicago");
			//TimeZone tz = earlyStartTime.getTimeZone();
			ExpCoordinator.print(new String("timezone offset: " + cstTimeZone.getRawOffset() + " id:" + cstTimeZone.getID()), 2);
		}
		dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		dateFormat.setTimeZone(cstTimeZone);
	}

	public void setStartTime(Date d) //used to set initial time for reservation
	{
		earlyStartTime.setTime(d);
		lateStartTime.setTime(d);
	}
	public SimpleDateFormat getDateFormat() { return dateFormat;}
	public void getReservation() { getReservation(expCoordinator.getMainWindow(), "Make Reservation");}
	public void getReservation(JFrame window, String ttl)
	{
		earlyStartTime.setTime(new Date());
		//lateStartTime.setTime(new Date());
		duration = 60;
		DateField early = new DateField(earlyStartTime);
		//DateField late = new DateField(lateStartTime, true);
		DurationField late = new DurationField(1);
		DurationField dur = new DurationField();
		JLabel lbl1 = new JLabel("Earliest Start:");
		JLabel lbl2 = new JLabel("Range of start from earliest:");
		JLabel lbl3 = new JLabel("Duration of experiment:");
		Object[] objectArray = {lbl1, early, lbl2, late, lbl3, dur};
		final String opt0 = "Enter";
		final String opt1 = "Cancel";
		Object[] options = {opt0,opt1};

		int rtn = JOptionPane.showOptionDialog(window, 
				objectArray, 
				ttl, 
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, 
				null,
				options,
				options[0]);

		if (rtn == JOptionPane.YES_OPTION)
		{
			duration = dur.getDuration();
			early.computeTime(); //sets earlyStartTime from user input
			int tmp = late.getDuration();
			lateStartTime.setTime(earlyStartTime.getTime());
			lateStartTime.add(Calendar.MINUTE, tmp); //set lateStartTime from user input
			//late.computeTime(); //set lateStartTime from user input
			expCoordinator.getUserInfo(); //make sure we have username and password
			print(2);
			sendRequest(new NCCPRequest());
		}
	}
	public void cancelReservation() 
	{
		expCoordinator.getUserInfo(); //make sure we have username and password
		print(2);
		sendRequest(new NCCPRequest(new NCCPCancelReq()));
	}  
	public void getExtension() { getExtension("Extend Current Reservation");}
	public void getExtension(String ttl)
	{
		JFrame window = expCoordinator.getMainWindow();
		JLabel lbl = new JLabel("Extend by time:");
		DurationField dur = new DurationField();
		Object[] objectArray = {lbl, dur};
		final String opt0 = "Enter";
		final String opt1 = "Cancel";
		Object[] options = {opt0,opt1};
        dur.setSize(200, dur.getHeight());
		int rtn = JOptionPane.showOptionDialog(window, 
				objectArray, 
				ttl, 
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, 
				null,
				options,
				options[0]);

		expCoordinator.print(new String("Reservation.getExtension Durations w=" + dur.getWidth()));
		if (rtn == JOptionPane.YES_OPTION)
		{
			duration = dur.getDuration();
			expCoordinator.getUserInfo(); //make sure we have username and password
			print(2);
			sendRequest(new NCCPRequest(new NCCPExtensionReq(expCoordinator.getCurrentExp(), (int)duration)));
		}
	}

	private void sendRequest(NCCPRequest nccp_req)
	{
		ExpCoordinator.print("Reservation.sendRequest",2);

		if (!expCoordinator.isConnected())
		{
			if (pendingRequest == null)
			{
				ExpCoordinator.print("   adding self as connection listener",2);
				expCoordinator.addConnectionListener(this);
			}
			pendingRequest = nccp_req;
			// if (!expCoordinator.connect())
			expCoordinator.connect();
		}
		else
		{
			ExpCoordinator.print("   adding request to expDaemon",2);
			expCoordinator.sendRequest(nccp_req);
		}

	}

	public GregorianCalendar getEarlyStartTime() { return earlyStartTime;}
	public GregorianCalendar getLateStartTime() { return lateStartTime;}
	public double getDuration() { return duration;} //in minutes
	public void print(int l)
	{
		ExpCoordinator.print(new String("Reservation earlyStartTime: " + earlyStartTime.getTime().getTime() + " ms lateStartTime: " + lateStartTime.getTime().getTime() + " ms duration: " + duration + " min"), l);
	}
	//NCCPConnection.ConnectionListener
	public void connectionFailed(NCCPConnection.ConnectionEvent e){}
	public void connectionClosed(NCCPConnection.ConnectionEvent e){}
	public void connectionOpened(NCCPConnection.ConnectionEvent e)
	{
		ExpCoordinator.print("Reservation.connectionOpened", 2);
		if (pendingRequest != null)
		{
			ExpCoordinator.print("  calling sendRequest", 2);
			sendRequest(pendingRequest);
			pendingRequest = null;
			expCoordinator.removeConnectionListener(this);
		}
	}
	//end NCCPConnection.ConnectionListener
	protected static int getNextIndex() { return(++index);}
}
