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
 * File: NSPPort.java
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
import java.util.*;
import javax.swing.*;
//import java.awt.Frame;
//import java.awt.Component;
import java.awt.event.*;
import javax.swing.event.*;
//import java.io.IOException;
import java.io.*;
import javax.xml.stream.*;
import org.xml.sax.*;




public class NSPPort extends RouterPort //WUGSPort //implements ONLComponent.PortInterface
{
	//private RouteTable routeTable = null;
	private PluginInstanceTable pInstanceTable = null;
	private Ingress ingress = null;
	private Egress egress = null;


	protected static final String QTACTION = "Queue Tables";
	protected static final String INFTACTION = "Ingress Filters";
	protected static final String OUTFTACTION = "Egress Filters";
	protected static final String PINTACTION = "Plugin Table";
	protected static final String RTACTION = "Route Table";

	//properties
	public static final String SUBPORT = "subport";

	////////////////////////////////////////////////// NSPPort.NPContentHandler ///////////////////////////////////////////////////
	protected static class NPContentHandler extends Hardware.Port.HWContentHandler
	{
		private Object currentObj = null;
		public NPContentHandler(ExperimentXML expxml,NSPPort p)
		{
			super(expxml, p);
			currentPort = p;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			//super.startElement(uri, localName, qName, attributes);
			//currentElement = new String(localName);
			if (getCurrentElement().equals(ExperimentXML.SAMPLING_TYPE) && currentPort != null) 
			{
				currentObj = ((NSPPort)currentPort).getSamplingTypes()[Integer.parseInt(attributes.getValue(ExperimentXML.INDEX))];
			}
			if (localName.equals(ExperimentXML.ROUTE_TABLE))
				expXML.setContentHandler(((NSPPort)currentPort).getRouteTable().getContentHandler(expXML));
			if (localName.endsWith(ExperimentXML.FILTER_TABLE))
			{
				String ftp = attributes.getValue(ExperimentXML.FILTER_TYPE);
				FilterTable ftbl = ((NSPPort)currentPort).getFilterTable(ftp);
				if (ftp.equals(ONLComponent.IPPEMFTABLE) || ftp.equals(ONLComponent.OPPEMFTABLE))
					ftbl.setPriority(Integer.parseInt(attributes.getValue(ExperimentXML.PRIORITY)));
				expXML.setContentHandler(ftbl.getContentHandler(expXML));
			}
			if (localName.endsWith(ExperimentXML.NSPQUEUE_TABLE))
			{
				QueueTable qtbl = null;
				if (localName.startsWith("in")) qtbl = ((NSPPort)currentPort).getQueueTable(true);
				else  qtbl = ((NSPPort)currentPort).getQueueTable(false);
				expXML.setContentHandler(qtbl.getContentHandler(expXML));
			}
			if (localName.equals(ExperimentXML.PINSTANCES))
				expXML.setContentHandler(((NSPPort)currentPort).getPInstanceTable().getContentHandler(expXML));
		}
		public void characters(char[] ch, int start, int length) throws SAXException
		{
			super.characters(ch, start, length);
			if (getCurrentElement().equals(ExperimentXML.SAMPLING_TYPE) && currentObj != null) 
			{
				if (currentObj instanceof RouterPort.SamplingType) 
				{
					((RouterPort.SamplingType)currentObj).setPercent(Double.parseDouble(new String(ch,start,length)));
					currentObj = null;
				}
			}
		}
		public void endElement(String uri, String localName, String qName)
		{
			super.endElement(uri, localName, qName);
			if (localName.equals(ExperimentXML.SAMPLING_TYPE)) currentObj = null;
			//else
			//{
			//  if (localName.equals(ExperimentXML.PORTS)) expXML.removeContentHandler(this);
			//}
		}
	}

	private class DirectionalPort
	{
		private boolean ingress = true;
		private FilterTable emFilterTable = null;
		private FilterTable gmFilterTable = null;
		protected QueueTable queueTable = null;

		public DirectionalPort(NSPPort p, boolean in)
		{
			ingress = in;
			if (in)
			{
				emFilterTable = new FilterTable(p, in, ONLComponent.IPPEMFTABLE);
				gmFilterTable = new FilterTable(p, in, ONLComponent.IPPEXGMFTABLE);
			}
			else
			{
				emFilterTable = new FilterTable(p, in, ONLComponent.OPPEMFTABLE);
				gmFilterTable = new FilterTable(p, in, ONLComponent.OPPEXGMFTABLE);
			}
			p.addChild(emFilterTable);
			p.addChild(gmFilterTable);
			if (in) 
			{
				queueTable = new VOQueue.QTable(p);
				//queueTable.resetVOQs();
			}
			else queueTable = new QueueTable(p, ONLComponent.OPPQTABLE);
			p.addChild(queueTable);
		}
		public FilterTable getFilterTable(String tp)
		{
			if (tp.equals(ONLComponent.IPPEMFTABLE) || tp.equals(ONLComponent.OPPEMFTABLE)) return emFilterTable;
			else return gmFilterTable;
		}
		public FilterTable getFilterTable(int tp)
		{
			switch (tp)
			{
			case FilterDescriptor.EXACT_MATCH:
				return emFilterTable;
			default:
				return gmFilterTable;
			}
		}
		public QueueTable getQueueTable() { return queueTable;}
		public void clear()
		{
			emFilterTable.clear();
			gmFilterTable.clear();
		}
		protected boolean merge(DirectionalPort p)
		{
			boolean rtn = false;
			if (p instanceof DirectionalPort)
			{
				rtn = emFilterTable.merge(p.emFilterTable);
				if (rtn) rtn = gmFilterTable.merge(p.gmFilterTable);
				if (rtn) rtn = queueTable.merge(p.queueTable);
			}
			return rtn;
		}
	}

	private class Ingress extends DirectionalPort
	{
		public Ingress(NSPPort p)
		{
			super(p, true);
		}
		public void resetVOQs() { queueTable.resetVOQs();}
	}

	private class Egress extends DirectionalPort
	{
		public Egress(NSPPort p)
		{
			super(p, false);
		}
	}

	////////////////////////////////////// class NSPPort.NCCP_SamplingRequester ////////////////////////////////////////////////////////
	private class NCCP_SamplingRequester extends NCCP.RequesterBase implements RouterPort.SamplingSetAction.Requester
	{
		private String op_str = "";
		private double sentPercents[] = null;

		public NCCP_SamplingRequester()
		{
			super(NCCP.Operation_FilterSampling);
			op_str = "set sampling rate";
			setMarker(new REND_Marker_class());
			sentPercents = new double[4];
			sentPercents[SamplingType.ALL] = 0;
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
			wrtr.writeShort(getID());
			SamplingType[] samplingTypes = getSamplingTypes();
			for (int i = 1; i < 4; ++i) 
			{
				samplingTypes[i].write(wrtr);
				sentPercents[i] = samplingTypes[i].getCommittedPercent();
			}
			ExpCoordinator.printer.print(new String("RouterPort.SamplingSetAction.Edit.NCCP_SamplingRequester.storeData for port:" + getID()), 4);
		}

		public String getOpString() { return op_str;}
		public double[] getSentPercents() { return sentPercents;}
	}//end class NSPPort.NCCP_SamplingRequester

	//should add in L, DQ, FIPL etc.
	public NSPPort(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{
		super(lbl,tp,tr,ec);
		initializeActions();
		ExpCoordinator.print(new String("NSPPort ipAddr:" + getIPAddr().toString()), 2);
	}
	public NSPPort(NSPDescriptor wd, int p, ExpCoordinator ec)
	{
		super(wd, new String(ONLComponent.NSP_LBL + ONLComponent.PORT_LBL), p, ec);
		initializeActions();
		ExpCoordinator.print(new String("NSPPort ipAddr:" + getIPAddr().toString()), 2);
	}

	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		super.writeXML(xmlWrtr);
		getRouteTable().writeXML(xmlWrtr);
		getFilterTable(ONLComponent.IPPEMFTABLE).writeXML(xmlWrtr);
		getFilterTable(ONLComponent.IPPEXGMFTABLE).writeXML(xmlWrtr);
		getFilterTable(ONLComponent.OPPEMFTABLE).writeXML(xmlWrtr);
		getFilterTable(ONLComponent.OPPEXGMFTABLE).writeXML(xmlWrtr);
		getQueueTable(true).writeXML(xmlWrtr);
		getQueueTable(false).writeXML(xmlWrtr);
		pInstanceTable.writeXML(xmlWrtr);
	}
	protected void initializeActions()
	{
		ingress = new Ingress(this);
		egress = new Egress(this);
		ingress.resetVOQs();
		setProperty(SUBPORT, 0);
		NSPRouteTable rtable = new NSPRouteTable(this);
		addTable(rtable);
		//actions.add(routeTable.getAction());

		actions.add(new FilterTable.FTAction(INFTACTION, this, true));
		actions.add(new FilterTable.FTAction(OUTFTACTION, this, false));
		actions.add(new QueueTable.QTAction(QTACTION, this));
		actions.add(new PluginInstanceTable.PTAction(PINTACTION, this));
		//addChild(routeTable);
		pInstanceTable = new PluginInstanceTable(this, ((NSPDescriptor)getParent()).getPluginClasses());
		addChild(pInstanceTable);

		//now add monitor actions for filter tables and route tables. this allows us to save and load packet rate monitors from files
		NSPPortMonitor nspm = (NSPPortMonitor)monitorable;
		nspm.addMonitorAction(rtable);
		nspm.addMonitorAction(ingress.getFilterTable(ONLComponent.IPPEMFTABLE));
		nspm.addMonitorAction(ingress.getFilterTable(ONLComponent.IPPEXGMFTABLE));
		nspm.addMonitorAction(egress.getFilterTable(ONLComponent.OPPEMFTABLE));
		nspm.addMonitorAction(egress.getFilterTable(ONLComponent.OPPEXGMFTABLE));
	}
	protected boolean merge(ONLComponent c)
	{
		boolean rtn = false;

		//ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("NSP Port(" + getLabel() + ") comparing\n")),
		//							   "NSP Port Compare",
		//							   "NSP Port Compare",
		//							   StatusDisplay.Error.LOG)); 
		if (c instanceof NSPPort)
		{
			rtn = super.merge(c);
			if (getID() != ((NSPPort)c).getID()) rtn = false;
			if (rtn)
			{
				NSPPort p2 = (NSPPort)c;
				rtn = pInstanceTable.merge(p2.pInstanceTable);
				if (rtn) rtn = ingress.merge(p2.ingress);
				if (rtn) rtn = egress.merge(p2.egress);
			}
		}
		if (!rtn)
		{
			ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("NSP Router Port(" + getLabel() + ") inconsistent\n")),
					"NSP Port Inconsistency",
					"NSP Port Inconsistency",
					StatusDisplay.Error.LOG)); 
		}
		return rtn;
	}
	public NSPRouteTable getRouteTable() { return ((NSPRouteTable)getTableByType(ONLComponent.RTABLE));}

	public QueueTable getQueueTable(String tp)
	{
		if (tp.equals(ONLComponent.IPPQTABLE)) return (getQueueTable(true));
		else return (getQueueTable(false));
	}

	public QueueTable getQueueTable(boolean in)
	{
		if (in) return (ingress.getQueueTable());
		else return (egress.getQueueTable());
	}
	public FilterTable getFilterTable(String tp) 
	{
		if (tp.equals(ONLComponent.IPPEMFTABLE) || tp.equals(ONLComponent.IPPGMFTABLE) || tp.equals(ONLComponent.IPPEXGMFTABLE) )
			return (getFilterTable(true, tp));
		else return (getFilterTable(false, tp));
	}
	public FilterTable getFilterTable(boolean in, String tp) 
	{
		if (in && ingress != null) 
			return (ingress.getFilterTable(tp));
		if (!in && egress != null) return (egress.getFilterTable(tp));
		return null;
	}
	public FilterTable getFilterTable(boolean in, int tp) 
	{
		if (in && ingress != null) 
			return (ingress.getFilterTable(tp));
		if (!in && egress != null) return (egress.getFilterTable(tp));
		return null;
	}
	public PluginInstanceTable getPInstanceTable() { return pInstanceTable;}
	protected void initializeMonitorable()
	{
		monitorable = new NSPPortMonitor((NSPMonitor)getParent().getMonitorable(), this);
	}  
	public void clear()
	{
		super.clear();
		//routeTable.clear();
		ingress.clear();
		egress.clear();
		QueueTable.QTAction qta = (QueueTable.QTAction)getAction(QTACTION);
		if (qta != null) qta.clear();
		FilterTable.FTAction fta = (FilterTable.FTAction)getAction(INFTACTION);
		if (fta != null) fta.clear();
		fta = (FilterTable.FTAction)getAction(OUTFTACTION);
		if (fta != null) fta.clear();
	}
	public boolean isDQon() { return (expCoordinator.isTestLevel(ExpCoordinator.DQ) && 
			(((NSPDescriptor)getParent()).getPropertyBool(NSPDescriptor.DQ)));}
	public double getSWBandwidth()
	{
		return (ingress.getQueueTable().getBandwidthVal());
	}
	public void addLink(LinkDescriptor lnk) 
	{
		super.addLink(lnk);
		if (lnk.getState().equals(IN2)) return;
		if (linkedTo != null && ((linkedTo instanceof RouterPort) || (linkedTo.isRouter())))
		{
			setProperty(SUBPORT, 1);
			ExpCoordinator.print(new String("NSPPort.addLink linkedTo:" + linkedTo.getLabel() + " setting subport to 1"), 4);
		}
		else 
		{
			setProperty(SUBPORT, 0);
			ExpCoordinator.print(new String("NSPPort.addLink linkedTo:" + linkedTo.getLabel() + " setting subport to 1"), 4);
		}
	}
	public void removeLink(LinkDescriptor lnk) 
	{
		super.removeLink(lnk);
		setProperty(SUBPORT, 0);
	}

	public RouterPort.SamplingSetAction.Requester getSamplingRequester() { return (new NCCP_SamplingRequester());}
	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new NSPPort.NPContentHandler(exp_xml, this));}
	public String getInterfaceType() { return ExperimentXML.ITYPE_1G;}
	public void removeGeneratedRoutes()
	{
		ExpCoordinator.print(new String("NSPPort(" + getLabel() + ").removeGeneratedRoutes"), Topology.TEST_DEFRTS);
		RouteTable rt = getRouteTable();
		if (rt != null) rt.removeGeneratedElements();
		else ExpCoordinator.print("    routeTable is null", Topology.TEST_DEFRTS);
	}
	public void clearRoutes()
	{
		ExpCoordinator.print(new String("NSPPort(" + getLabel() + ").clearRoutes"), Topology.TEST_DEFRTS);
		RouteTable rt = getRouteTable();
		if (rt != null) rt.clearRoutes();
		else ExpCoordinator.print("    routeTable is null", Topology.TEST_DEFRTS);
	}
}
