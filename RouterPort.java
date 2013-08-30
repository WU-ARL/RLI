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
 * File: RouterPort.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 6/6/2006
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.lang.String;
import java.io.*;
import javax.swing.*;
import java.util.Vector;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.undo.*;
import java.awt.Frame;
import java.awt.Component;
import javax.swing.event.*;
import javax.xml.stream.*;

public abstract class RouterPort extends Hardware.Port
{
	//private int portID = 0;
	private PortStats.Indices statIndices = null;
	protected static final int MAX_STAT_INDEX = 127;
	private SamplingSetAction fSamplingAction = null;
	public static final String SAMPLING_TYPE1 = "sampling_type1";
	public static final String SAMPLING_TYPE2 = "sampling_type2";
	public static final String SAMPLING_TYPE3 = "sampling_type3";
	public static final String SAMPLING_ALL = "sampling_all";
	public static final String INGRESS = "ingress";
	//private Vector tables = null;

	////////////////////////////////////////////////////////OpManager//////////////////////////////////////////////////////////////////////////////////////
	private class OpManager extends NCCPOpManager
	{
		private class NCCP_Response extends NCCP.ResponseBase
		{
			private double data = 0;
			private String msg = "";
			public NCCP_Response() {super(0);}
			public void retrieveFieldData(DataInput din) throws IOException
			{
				ONL.NCCPReader rdr = new ONL.NCCPReader(din);
				msg = rdr.readString();
				if (status != NCCP.Status_Fine)
					ExpCoordinator.print(new String(msg + " for port" + getID()), 8);
			}
			public double getData(MonitorDataType.Base mdt) { return 0;}
			public double getData() { return 0;}
		}//end RouteTable.OpManager.NCCP_Response

		public OpManager()
		{
			super((MonitorDaemon)getONLDaemon(ONLDaemon.HWC));
			response = new NCCP_Response();
		}
		protected void setRendMarker(NCCP.RequesterBase req, NCCPOpManager.Operation op, int i)
		{
			req.setMarker(new REND_Marker_class(getID(), true, req.getOp(), i));
		}
		//override method to process different types of responses
		/*
    public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException
      {
	if (response != null) 
	  {
	      //System.out.println("ME::retrieveData");
	      //hdr.getRendMarker().print();
	    response.setHeader(hdr); //set header info
	    response.retrieveData(din); //get op specific return data
	    processResponse(response);
	  }
      }
		 */
	}


	//////////////////////////////////////////////////////// SetSamplingAction //////////////////////////////////////////////////////////////////////

	public static class SamplingType
	{
		private double min = 0;
		private double max = 100;
		protected double percent = 100;
		private double committedPercent = -1;
		protected int type = TYPE1;
		public final static String ALL_STR = "100%";
		public final static int ALL = 0;
		public final static int TYPE1 = 1;
		public final static int TYPE2 = 2;
		public final static int TYPE3 = 3;
		public final static String TYPE1_STR = "sampling1";
		public final static String TYPE2_STR = "sampling2";
		public final static String TYPE3_STR = "sampling3";
		private RouterPort port = null;
		private Router router = null;

		public SamplingType() {}
		public SamplingType(String s) throws java.text.ParseException
		{
			parseString(s);
		}
		public SamplingType(SamplingType st)
		{
			this(st.percent, st.type);
		}
		public SamplingType(double rt, int tp)
		{
			setPercent(rt);
			setType(tp);
			if (tp >= 0 && tp < 4) type = tp;
		}
		public void parseString(String s) throws java.text.ParseException
		{
			try 
			{
				double tmp_int = Double.parseDouble(s);
				if (tmp_int < min || tmp_int > max) 
					throw new java.text.ParseException(new String("SamplingType not percent in " + min + "-" + max + " range"), 0);
				else percent = tmp_int;
			}
			catch (NumberFormatException e)
			{ 
				throw new java.text.ParseException(new String("SamplingType not percent in " + min + "-" + max + " range"), 0);
			}
		}
		public double getPercent() { return percent;}
		public int getType() { return type;}
		public String getTypeString() 
		{ 
			switch (type)
			{
			case TYPE1:
				return SAMPLING_TYPE1;
			case TYPE2:
				return SAMPLING_TYPE2;
			case TYPE3:
				return SAMPLING_TYPE3;
			default:
				return SAMPLING_ALL;
			}
		}
		public void setType(int tp) { type = tp;}
		public void setPercent(double rt) 
		{ 
			if (rt >= min && rt <= max) 
			{
				percent = rt;
				if (port != null) port.setProperty(getTypeString(), percent);
				if (router != null) router.setProperty(getTypeString(), percent);
			}
		}
		public void write(ONL.Writer wrtr) throws java.io.IOException
		{
			wrtr.writeInt(type);
			wrtr.writeDouble(percent);
		}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(ExperimentXML.SAMPLING_TYPE);//"samplingType");
			xmlWrtr.writeAttribute(ExperimentXML.INDEX, String.valueOf(type));
			xmlWrtr.writeCharacters(String.valueOf(percent));
			xmlWrtr.writeEndElement(); 
		}

		public void read(ONL.Reader rdr) throws java.io.IOException
		{
			type = rdr.readInt();
			setPercent(rdr.readDouble());
		}
		public String toString()
		{
			if (type == ALL) return ALL_STR;
			else return (new String(percent + "%"));
		}
		protected double getCommittedPercent() { return committedPercent;}
		protected void setCommittedPercent(double per) { committedPercent = per;}
		public boolean needsCommit() { return (committedPercent != percent);}
		public void setPort(RouterPort rp) { port = rp;}
		public void setRouter(Router rt) { router = rt;}
	}//end inner class SamplingType

	protected static class SamplingSetAction extends ONL.UserAction
	{
		private Edit edit = null;
		private TextFieldwLabel[] stypes = null;
		private RouterPort rtport = null;
		private Router router = null;
		private SamplingType[] samplingTypes = null;

		protected static interface Requester
		{
			public double[] getSentPercents(); //should be an array with 4 elements
		}

		private class Edit extends AbstractUndoableEdit implements ONLComponent.Undoable, NCCPOpManager.Operation
		{
			private Experiment experiment = null;
			private  Requester request = null;
			private double sentPercents[] = null;

			public Edit()
			{
				super();
				sentPercents = new double[4];
				sentPercents[SamplingType.ALL] = 0;
				for (int i = 1; i < 4; ++i) sentPercents[i] = samplingTypes[i].getPercent();
			}
			//ONLComponent.Undoable
			public boolean canUndo() { return false;}
			public boolean canRedo() { return false;}
			public void commit() 
			{
				ONLDaemon od = null;
				if (rtport != null) od = rtport.getONLDaemon(ONLDaemon.HWC);
				if (router != null) od = router.getONLDaemon(ONLDaemon.HWC);
				if (od != null) 
				{
					if (!od.isConnected()) od.connect();
					if (rtport != null)
					{
						request = rtport.getSamplingRequester();
						rtport.addOperation(this); //send it through one of the filter tables use gm ingress
					}
					if (router != null)
					{
						request = router.getSamplingRequester();
						router.addOperation(this); //send it through one of the filter tables use gm ingress
					}
				}
				else System.err.println("FilterTable.SamplingPanel.Edit.commit no daemon");
				//resetEdit();
			}
			public boolean isCancelled(ONLComponent.Undoable un)
			{
				if (!(un instanceof Edit)) return false;
				Edit e = (Edit)un;
				return ((rtport != null && e.getPort() == rtport) || (router != null && e.getRouter() == router));
			}
			//end ONLComponent.Undoable
			public void actionPerformed(ActionEvent e) {}
			//NCCPOpManager.Operation
			public void opSucceeded(NCCP.ResponseBase resp) 
			{
				ExpCoordinator.print(new String("RouterPort.SamplingSetAction.Edit.opSucceeded"), 2);
				for (int i = 1; i < 4; ++i)
				{
					samplingTypes[i].setCommittedPercent(sentPercents[i]);
				}
				if (rtport != null) rtport.removeOperation(this);
				if (router != null) router.removeOperation(this);
				//updateColor();
			}
			public void opFailed(NCCP.ResponseBase resp)
			{
				sentPercents = request.getSentPercents();
				for (int i = 1; i < 4; ++i)
				{
					if (sentPercents[i] == samplingTypes[i].getPercent())
						samplingTypes[i].setPercent(samplingTypes[i].getCommittedPercent());
				}
				if (rtport != null) rtport.removeOperation(this);
				if (router != null) router.removeOperation(this);
				//updateColor();
			}
			public Vector getRequests() 
			{ 
				Vector rtn = new Vector();
				rtn.add(request);
				return rtn;
			}
			public boolean containsRequest(REND_Marker_class rend) { return (request != null && ((NCCP.RequesterBase)request).getMarker().isEqual(rend));}
			//end NCCPOpManager.Operation
			public RouterPort getPort() { return rtport;}
			public Router getRouter() { return router;}
		}//inner class Edit

		public SamplingSetAction(RouterPort rp) { this("Change Sampling Filter Types", rp);}
		public SamplingSetAction(String lbl, RouterPort rp)
		{
			super(lbl, false, true);
			rtport = rp;
			samplingTypes = new SamplingType[4];
			samplingTypes[SamplingType.TYPE1] = new SamplingType(50, SamplingType.TYPE1);
			samplingTypes[SamplingType.TYPE2] = new SamplingType(25, SamplingType.TYPE2);
			samplingTypes[SamplingType.TYPE3] = new SamplingType(12.5, SamplingType.TYPE3);
			samplingTypes[SamplingType.ALL] = new SamplingType(100, SamplingType.ALL);
			stypes = new TextFieldwLabel[3];
			stypes[0] = new TextFieldwLabel(new JFormattedTextField(samplingTypes[SamplingType.TYPE1]), "type 1 (percentage):");
			stypes[1] = new TextFieldwLabel(new JFormattedTextField(samplingTypes[SamplingType.TYPE2]), "type 2 (percentage):"); 
			stypes[2] = new TextFieldwLabel(new JFormattedTextField(samplingTypes[SamplingType.TYPE3]), "type 3 (percentage):"); 
			for (int i = 0; i < 4; ++i)
			{
				samplingTypes[i].setPort(rtport);
				if (i > 0) stypes[i-1].setValue(samplingTypes[i].getPercent());
			}
		}
		public SamplingSetAction(Router rp) { this("Change Sampling Filter Types", rp);}
		public SamplingSetAction(String lbl, Router rp)
		{
			super(lbl, false, true);
			router = rp;
			samplingTypes = new SamplingType[4];
			samplingTypes[SamplingType.TYPE1] = new SamplingType(50, SamplingType.TYPE1);
			samplingTypes[SamplingType.TYPE2] = new SamplingType(25, SamplingType.TYPE2);
			samplingTypes[SamplingType.TYPE3] = new SamplingType(12.5, SamplingType.TYPE3);
			samplingTypes[SamplingType.ALL] = new SamplingType(100, SamplingType.ALL);
			stypes = new TextFieldwLabel[3];
			stypes[0] = new TextFieldwLabel(new JFormattedTextField(samplingTypes[SamplingType.TYPE1]), "type 1 (percentage):");
			stypes[1] = new TextFieldwLabel(new JFormattedTextField(samplingTypes[SamplingType.TYPE2]), "type 2 (percentage):"); 
			stypes[2] = new TextFieldwLabel(new JFormattedTextField(samplingTypes[SamplingType.TYPE3]), "type 3 (percentage):"); 
			for (int i = 0; i < 4; ++i)
			{
				samplingTypes[i].setRouter(router);
				if (i > 0) stypes[i-1].setValue(samplingTypes[i].getPercent());
			}
		}

		protected boolean merge(SamplingSetAction sa)
		{
			String nm = "";
			if (router != null) nm = router.getLabel();
			if (rtport != null) nm = rtport.getLabel();
			//compare sampling types
			double s1 = samplingTypes[SamplingType.TYPE1].getPercent();
			double s2 = sa.samplingTypes[SamplingType.TYPE1].getPercent();
			if (s1 != s2)
			{
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String(nm + ": Sampling Type Inconsistency: master sampling type 1 = " + s1 + " compared to " + s2 + "\n")),
						"Sampling Inconsistency",
						"Sampling Inconsistency",
						StatusDisplay.Error.LOG)); 
			}

			s1 = samplingTypes[SamplingType.TYPE2].getPercent();
			s2 = sa.samplingTypes[SamplingType.TYPE2].getPercent();
			if (s1 != s2)
			{
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String(nm + ": Sampling Type Inconsistency: master sampling type 2 = " + s1 + " compared to " + s2 + "\n")),
						"Sampling Inconsistency",
						"Sampling Inconsistency",
						StatusDisplay.Error.LOG)); 
			}
			s1 = samplingTypes[SamplingType.TYPE3].getPercent();
			s2 = sa.samplingTypes[SamplingType.TYPE3].getPercent();
			if (s1 != s2)
			{
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String(nm + ": Sampling Type Inconsistency: master sampling type 3 = " + s1 + " compared to " + s2 + "\n")),
						"Sampling Inconsistency",
						"Sampling Inconsistency",
						StatusDisplay.Error.LOG)); 
			}
			return true;
		}
		public void actionPerformed(ActionEvent e)
		{
			Object[] options = {"Enter","Cancel"}; 
			boolean add_edit = false;
			for (int i = 1; i < 4; ++i)
			{
				stypes[i-1].setValue(samplingTypes[i].getPercent());
			}

			JFrame window = null;
			if (rtport != null) rtport.getFrame();
			if (window == null) window = ExpCoordinator.getMainWindow();
			int rtn = JOptionPane.showOptionDialog( window, 
					stypes, 
					(String)getValue(Action.NAME), 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);
			if (rtn == JOptionPane.YES_OPTION)
			{
				for (int i = 1; i < 4; ++i)
				{
					ExpCoordinator.print(new String("RouterPort.samplingrate " + i + "= " + stypes[i-1].getValue()), 5);
					String str_array[] = stypes[i-1].getText().split("%");
					double perc = Double.parseDouble(str_array[0]);
					samplingTypes[i].setPercent(perc);
					if (samplingTypes[i].needsCommit()) add_edit = true;
				}
				if (ExpCoordinator.isRecording() && rtport != null)
					ExpCoordinator.recordEvent(SessionRecorder.CHANGE_SAMPLING_FILTER, new String[]{rtport.getLabel(), String.valueOf(samplingTypes[1].getPercent()), String.valueOf(samplingTypes[2].getPercent()), String.valueOf(samplingTypes[3].getPercent())});
			}

			if (add_edit) ExpCoordinator.theCoordinator.addEdit(new Edit());
			//save to file
		}
		//public void addEditForCommit()
		//{
		//edit.actionPerformed(null);
		//}
		public SamplingType[] getSamplingTypes() { return samplingTypes;}
		public void read(ONL.Reader rdr) throws java.io.IOException //the first type all for both reading and writing
		{
			for (int i = 1; i < 4; ++i) samplingTypes[i].read(rdr);
		}
		public void write(ONL.Writer wrtr) throws java.io.IOException
		{
			for (int i = 1; i < 4; ++i) samplingTypes[i].write(wrtr);
		}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			for (int i = 1; i < 4; ++i) samplingTypes[i].writeXML(xmlWrtr);
		}
	}//end SamplingSetAction
	public RouterPort(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{
		super(lbl,tp,tr,ec);
		//SUBNET:remove//setIPAddr(getID(), ((Router)getParent()).getBaseIPAddr());
		statIndices = new PortStats.Indices(MAX_STAT_INDEX);
		addStateListener(new String[]{SAMPLING_TYPE1, SAMPLING_TYPE2, SAMPLING_TYPE3});
		//tables = new Vector();
	}
	public RouterPort(Router wd, String tp, int p, ExpCoordinator ec)
	{
		super(wd, tp, p, ec);
		//SUBNET:remove//setIPAddr(p, wd.getBaseIPAddr());
		statIndices = new PortStats.Indices(MAX_STAT_INDEX);
		addStateListener(new String[]{SAMPLING_TYPE1, SAMPLING_TYPE2, SAMPLING_TYPE3});
		//tables = new Vector();
	}

	public void writeExpDescription(ONL.Writer tw)  throws IOException
	{
		super.writeExpDescription(tw);
		if (fSamplingAction != null) 
			fSamplingAction.write(tw);
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		super.writeXML(xmlWrtr);
		if (fSamplingAction != null) 
			fSamplingAction.writeXML(xmlWrtr);
	}
	/*
  protected boolean isParent(int tp) 
  {
    return ((getChild(tp) != null) || 
            ONLComponent.RTABLE == tp ||
            ONLComponent.IPPEMFTABLE == tp ||
            ONLComponent.IPPGMFTABLE == tp ||
            ONLComponent.IPPEXGMFTABLE == tp ||
            ONLComponent.IPPQTABLE == tp ||
            ONLComponent.OPPEMFTABLE == tp ||
            ONLComponent.OPPGMFTABLE == tp ||
            ONLComponent.OPPEXGMFTABLE == tp ||
            ONLComponent.OPPQTABLE == tp ||
            ONLComponent.PCLASSES == tp ||
            ONLComponent.PINSTANCES == tp);
  }
	 */
	public void addStat(PortStats ps) {statIndices.addStat(ps);}
	public void removeStat(PortStats ps) {statIndices.removeStat(ps);}
	public SamplingType[] getSamplingTypes() 
	{ 
		if (fSamplingAction == null) getSamplingAction();
		return (fSamplingAction.getSamplingTypes());
	}
	public AbstractAction getSamplingAction() 
	{ 
		if (fSamplingAction == null) 
			fSamplingAction = new SamplingSetAction(this);
		return fSamplingAction;
	}
	public void setSamplingTypes(ONL.Reader rdr) throws java.io.IOException 
	{
		if (fSamplingAction == null) getSamplingAction();
		fSamplingAction.read(rdr);
	}

	public void writeSamplingTypes(ONL.Writer wrtr)throws java.io.IOException 
	{
		if (fSamplingAction != null)
			fSamplingAction.write(wrtr);
	}

	public void addTable(ONLCTable table)
	{
		actions.add(table.getAction());
		addChild(table);
	}
	public void removeTable(ONLCTable table) 
	{
		actions.remove(table.getAction());
		removeChild(table);
	}

	public ONLCTable getTable(String tp)
	{
		ONLComponent rtn = getChild(tp);
		if (rtn != null && rtn instanceof ONLCTable) return ((ONLCTable)rtn);
		return null;
	}
	public ONLCTable getTable(String tp, boolean in)
	{
		ONLComponent rtn = null;
		ONLComponentList kids = getChildren();
		int max = kids.size();
		for (int i = 0; i < max; ++i)
		{
			rtn = kids.onlComponentAt(i);
			if (rtn.isType(tp) && rtn instanceof ONLCTable) 
			{
				if (rtn.getPropertyBool(INGRESS) == in)
					return ((ONLCTable)rtn);
			}
		}
		return null; 
	}

	public abstract RouterPort.SamplingSetAction.Requester getSamplingRequester();
	protected boolean merge(ONLComponent c)
	{
		if (c instanceof RouterPort)
		{
			RouterPort port2 = (RouterPort)c;
			if (getID() != port2.getID()) return false;
			ONLComponentList children = getChildren();
			//check any tables
			int max = children.getSize();
			ONLCTable c2;
			ONLComponent c1;
			for (int i = 0; i < max; ++i)
			{
				c1 = children.onlComponentAt(i);
				if (c1 instanceof ONLCTable)
				{
					c2 = port2.getTable(c1.getType());
					if (c2 == null || !c1.merge(c2)) return false;
				}
			}
			//compare sampling types
			if (fSamplingAction != null && port2.fSamplingAction != null)
			{
				if (!fSamplingAction.merge(port2.fSamplingAction)) return false;
			}
			return true;
		}
		else return false;
	}
}
