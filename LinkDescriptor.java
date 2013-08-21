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
 * File: LinkDescriptor.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 11/07/2003
 *
 * Description:
 *
 * Modification History:
 *
 */

import java.lang.String;
import java.io.*;
import java.awt.event.*;
import javax.swing.undo.*;
import javax.xml.stream.*;
import org.xml.sax.*;

public class LinkDescriptor extends ONLComponent
{
	public static final int TEST_ADD = 3;
	public static final String DIRECTION = "direction";
	public static final String POINT1 = "point1";
	public static final String POINT2 = "point2";
	public static final String BW = "bandwidth";
	public static final int BIDIRECTIONAL = 0;
	public static final int UNIDIRECTIONAL = 1;
	public static final int DEFAULT_BANDWIDTH = 200; //Mbps

	private ONLComponent point1 = null;
	private ONLComponent point2 = null;
	private double bandwidth = 0;
	private int direction = BIDIRECTIONAL;
	private int delay = 0; // not used now
	private LinkGraphic graphic = null;
	private boolean fixed = false;

	/////////////////////////////////////////////////////LinkDescriptor.LContentHandler/////////////////////////////////////////////////////////////
	public static class LContentHandler extends ONLComponent.ONLContentHandler
	{
		private class PointHandler extends ONLComponent.XMLComponentHandler
		{
			private String pointType = "";
			private LContentHandler lchandler = null;
			public PointHandler(String uri, Attributes attributes, String p, LContentHandler lch)
			{
				super(uri,attributes,lch.expXML);
				pointType = new String(p);
				lchandler = lch;
			}
			public String getPointType() { return pointType;}
			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("LinkDescriptor.LContentHandler.PointHandler.endElement " + localName + "  pointType " + pointType), 3);
				if (localName.equals(pointType)) 
				{
					if (pointType.equals(ExperimentXML.POINT1))
						((LinkDescriptor)lchandler.getComponent()).setPoint1(getComponent());
					else 
						((LinkDescriptor)lchandler.getComponent()).setPoint2(getComponent());
					expXML.removeContentHandler(this);
				}
			}
		}
		public LContentHandler(LinkDescriptor c, ExperimentXML exp_xml)
		{
			super(c, exp_xml);
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			super.startElement(uri,localName,qName,attributes);
			if (getCurrentElement().startsWith(ExperimentXML.POINT))
				expXML.setContentHandler(new PointHandler(uri, attributes, getCurrentElement(), this));
		}
		/*
      public void characters(char[] ch, int start, int length)
      {
	super.characters(ch,start,length);
      }
      public void endElement(String uri, String localName, String qName)
      {
	super.endElement(uri,localName,qName);
      }
		 */
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////LinkDescriptor.NCCP_LinkRequester/////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected static class NCCP_LinkRequester extends ExpDaemon.NCCP_Requester
	{
		public NCCP_LinkRequester(int op, ONLComponent ld, Experiment exp)
		{
			super(op, ld, exp);
		}
		public void storeData(DataOutputStream dout) throws IOException 
		{
			super.storeData(dout);
			LinkDescriptor link = (LinkDescriptor)getONLComponent();
			ONLComponent.PortBase p = (ONLComponent.PortBase)link.getPoint1(); 
			if (p instanceof PortInterface)
			{
				NCCP.writeComponentID(p.getParent(), dout);
				dout.writeShort(p.getID());
			}
			else
			{
				NCCP.writeComponentID(p, dout);
				dout.writeShort(0);
			}
			if ((getOp() == ExpDaemon.NCCP_Operation_AddLink) || (getOp() == ExpDaemon.NCCP_Operation_ReservationLink))
			{
				if (p.getIPAddr() != null)
					NCCP.writeString(p.getIPAddr().toString(), dout);
				else NCCP.writeString("0.0.0.0", dout);
				if (p.getNetmaskString() != null)
					NCCP.writeString(p.getNetmaskString(), dout);
				else NCCP.writeString("255.255.255.240", dout);
				if (p.getNHIPAddr() != null)
					NCCP.writeString(p.getNHIPAddr().toString(), dout);
				else NCCP.writeString("0.0.0.0", dout);
			}
			//NCCP.writeString(p.getIPAddrString(), dout);
			p = (ONLComponent.PortBase)link.getPoint2(); 
			NCCP.writeComponentID(p.getParent(), dout);
			dout.writeShort(((PortInterface)p).getID());
			if ((getOp() == ExpDaemon.NCCP_Operation_AddLink) || (getOp() == ExpDaemon.NCCP_Operation_ReservationLink))
			{
				if (p.getIPAddr() != null)
					NCCP.writeString(p.getIPAddr().toString(), dout);
				else NCCP.writeString("0.0.0.0", dout);
				if (p.getNetmaskString() != null)
					NCCP.writeString(p.getNetmaskString(), dout);
				else NCCP.writeString("255.255.255.240", dout);
				if (p.getNHIPAddr() != null)
					NCCP.writeString(p.getNHIPAddr().toString(), dout);
				else NCCP.writeString("0.0.0.0", dout);
			}
			//NCCP.writeString(p.getIPAddrString(), dout);
			dout.writeInt((int)link.getBandwidth());
			if (getOp() !=  ExpDaemon.NCCP_Operation_ReservationLink) link.getExpCoordinator().addToProgressBar(link);
		}
	}

	protected static class NCCP_AddLink extends ExpRequest implements Reservation.Reservable
	{
		private LinkDescriptor link = null;
		private boolean reserved = false;
		public NCCP_AddLink(LinkDescriptor ld, Experiment e)
		{
			super();
			link = ld;
			ExpCoordinator.print(new String("LinkDescriptor.NCCP_AddLink link:" + link.getFullString()), TEST_ADD);
			request = new NCCP_LinkRequester(ExpDaemon.NCCP_Operation_AddLink, ld, e);
			response = new ExpDaemon.NCCP_Response();
		}
		public void processResponse(NCCP.ResponseBase r)
		{
			ExpCoordinator expc = link.getExpCoordinator();
			ExpCoordinator.printer.print(new String("LinkDescriptor.NCCP_AddLink.processResponse for " + link.getLabel() + ":" + link.printString() + " status=" + r.getStatus()), 1);
			if (r.getStatus() == NCCP.Status_Fine) 
			{
				if (((ExpDaemon.NCCP_Requester)request).getReservationID() == null)
				{
					link.setProperty(ONLComponent.STATE, ONLComponent.ACTIVE);
					if (((ExpDaemon.NCCP_Requester)request).getExperiment() == expc.getCurrentExp())
					{
						ONLComponent c = link.getPoint1();
						if (c != null && c.needToRemap()) expc.remap(c);
						c = link.getPoint2();
						if (c != null && c.needToRemap()) expc.remap(c);
					}
				}
				else reserved = true;
			}
			else 
			{
				if (((ExpDaemon.NCCP_Requester)request).getReservationID() == null)
				{
					link.setProperty(ONLComponent.STATE, ONLComponent.FAILED);
					Experiment exp = ((ExpDaemon.NCCP_Requester)request).getExperiment();
					if (exp == expc.getCurrentExp())
					{
						exp.setProperty(Experiment.FAILURE, true);
						//generate Status Bar Error
						int error = StatusDisplay.Error.LOG;
						if ((link.point1 != null && link.point2 != null && link.point1.isActive() && link.point2.isActive()) &&
								(r.isEndOfMessage()))
							error = StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR;
						expc.addError(new StatusDisplay.Error((new String(link.getFullString() + " Link Failure.Failed to make virtual link. Hardware Error.\n")),
								"Link Failure.",
								"Link Failure",
								error));         
					}
				}
				//exp.removeComponent(link);
				//if (((ExpDaemon.NCCP_Requester)request).getExperiment() == expc.getCurrentExp())
				//expc.addEdit(new AddEdit(link, expc.getTopology(), ((ExpDaemon.NCCP_Requester)request).getExperiment()));
			}
			if (((ExpDaemon.NCCP_Requester)request).getReservationID() == null)
			{
				expc.removeRequest(this);
				expc.removeFromProgressBar(link);
			}
		}

		protected void setReservation(String res_id, int opc)
		{
			if (request != null)
			{
				request.setOp(opc);
				((NCCP_LinkRequester)request).setReservationID(res_id);
				ExpCoordinator.theCoordinator.removeFromProgressBar(link);
			}
		}
		public boolean isReserved() { return reserved;}
	}

	protected static class NCCP_DeleteLink extends ExpRequest
	{
		private LinkDescriptor link = null;
		public NCCP_DeleteLink(LinkDescriptor ld, Experiment e)
		{
			super();
			link = ld;
			request = new NCCP_LinkRequester(ExpDaemon.NCCP_Operation_DeleteLink, ld, e);
			response = new ExpDaemon.NCCP_Response();
		}
		public void processResponse(NCCP.ResponseBase r)
		{
			ExpCoordinator expc = link.getExpCoordinator();
			if (r.getStatus() == NCCP.Status_Fine) 
			{
				link.setProperty(ONLComponent.STATE, ONLComponent.NOT_INIT);
				ExpCoordinator.print(new String("LinkDescriptor.DeleteLink.processResponse " + link.getLabel()), 2);
				if (((ExpDaemon.NCCP_Requester)request).getExperiment() == expc.getCurrentExp())
				{
					ONLComponent c = link.getPoint1();
					if (c != null && c.needToRemap()) expc.remap(c);
					c = link.getPoint2();
					if (c != null && c.needToRemap()) expc.remap(c);
				}
			}
			else
			{
				if (((ExpDaemon.NCCP_Requester)request).getExperiment() == expc.getCurrentExp())
				{
					expc.addError(new StatusDisplay.Error((new String(link.getFullString() + " Failed to delete link. Hardware Error.\n")),
							"Delete Link Failure.",
							"Delete Link Failure",
							(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));         
				}
			}
			expc.removeRequest(this);
			expc.removeFromProgressBar(link);
		}
		public void setRequestSent(boolean r) 
		{
			super.setRequestSent(r);
			if (r) link.setProperty(ONLComponent.STATE, ONLComponent.WAITING);
		}
	}//end NCCP_DeleteLink

	public static class AddEdit extends Experiment.AddEdit
	{
		private Topology topology = null;
		public AddEdit(LinkDescriptor ld, Topology top, Experiment exp)
		{
			super(ld, exp);
			setIsUndoable(false);
			topology = top;
		}
		public void undo() throws CannotUndoException
		{
			super.undo();
			getExperiment().removeComponent(getONLComponent());
		}
		public void redo() throws CannotRedoException
		{
			super.redo();
			getExperiment().addComponent(getONLComponent());
		}
		protected void sendMessage() 
		{
			ExpCoordinator ec = getONLComponent().getExpCoordinator();
			ec.sendRequest(new NCCP_AddLink((LinkDescriptor)getONLComponent(), getExperiment()));
		}
		protected void sendUndoMessage() {}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			boolean rtn = super.isCancelled(un);
			//if (rtn) return true;
			//else
			//{
			if (isEditInstance(un))//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
			{
				Experiment.Edit edit = (Experiment.Edit) un;
				LinkDescriptor ld = (LinkDescriptor) onlComponent;
				/*  if (edit.isRemove()) 
              ExpCoordinator.printer.print(new String("LD.AddEdit.isCancelled() delEdit.onlComponent:" + onlComponent.getLabel()), 9);
            else
            ExpCoordinator.printer.print(new String("LD.AddEdit.isCancelled() " + edit.getClass().getName() + " addEdit.onlComponent:" + onlComponent.getLabel()), 9);*/
				ld.print(4);
				if (edit.isRemove() && (ld == edit.onlComponent ||
						ld.point1 == edit.onlComponent || 
						ld.point1.getParent() == edit.onlComponent ||
						ld.point2 == edit.onlComponent || 
						ld.point2.getParent() == edit.onlComponent))
				{

					//ExpCoordinator.printer.print("   returning true", 4);
					return true;
				}
				//}
			}

			return (super.isCancelled(un));
		}
	}


	public static class DeleteEdit extends Experiment.DeleteEdit
	{
		private Topology topology = null;
		public DeleteEdit(LinkDescriptor ld, Topology top, Experiment exp)
		{
			super(ld, exp);
			setIsUndoable(false);

			//ExpCoordinator.printer.print("LinkDescriptor.DeleteEdit", 4);
			ld.print(4);
			topology = top;
		}
		public void actionPerformed(ActionEvent e)
		{
			super.actionPerformed(e);
			topology.removeComponent(onlComponent);
		}
		public void undo() throws CannotUndoException
		{
			super.undo();
			topology.addComponent(onlComponent);
		}
		public void redo() throws CannotRedoException
		{
			super.redo();
			topology.removeComponent(onlComponent);
		}
		protected void sendMessage()
		{
			ExpCoordinator ec = getONLComponent().getExpCoordinator();
			ec.sendRequest(new NCCP_DeleteLink((LinkDescriptor)getONLComponent(), getExperiment()));
		}
		protected void sendUndoMessage() {}
	}

	public LinkDescriptor (String lbl, ONL.Reader tr, ExpCoordinator onlcd) throws IOException
	{
		super(lbl, ONLComponent.LINK_LBL, tr, onlcd);
		String tmp_str = tr.readString();
		ExpCoordinator.printer.print("LinkDescriptor", 2);
		ExpCoordinator.printer.print(new String(" direction:" + tmp_str), 2);
		setDirection(tmp_str);
		bandwidth = tr.readDouble();
		ExpCoordinator.printer.print(new String(" bandwidth:" + bandwidth), 2);
		tmp_str = tr.readString();
		ExpCoordinator.printer.print(new String(" point1:" + tmp_str), 2);
		point1 = onlcd.getComponentFromString(tmp_str);  
		tmp_str = tr.readString();
		ExpCoordinator.printer.print(new String(" point2:" + tmp_str), 2);    
		point2 = onlcd.getComponentFromString(tmp_str);
		//setDirection(tr.readString());
		//bandwidth = tr.readDouble();
		//point1 = onlcd.getComponentFromString(tr.readString());      
		//point2 = onlcd.getComponentFromString(tr.readString());
		addToDescription(DIRECTION, getDirectionString());
		addToDescription(BW, new String("Bandwidth: " + bandwidth + " Mbps"));
		addToDescription(POINT1, new String("Point 1: " + point1.toString()));
		addToDescription(POINT2, new String("Point 2: " + point2.toString()));
		setLinkable(false);
		if (point1 != null) point1.addLink(this);
		if (point2 != null) point2.addLink(this);
	}
	protected LinkDescriptor(String uri, Attributes attributes)
	{
		super(uri,attributes);
	}
	protected LinkDescriptor(String lbl, ONLComponent p1, ONLComponent p2, double bw, ExpCoordinator ec)
	{
		this(lbl, p1, bw, ec);	  
		if (p2 instanceof GigEDescriptor) point2 = ((GigEDescriptor)p2).getUnlinkedPort();
		else point2 = p2;
		addToDescription(POINT2, new String("Point 2: " + point2.toString()));

		if (point1 != null) point1.addLink(this);
		if (point2 != null) 
		{
			point2.addLink(this);
			if (point2.getInterfaceType().equals(ExperimentXML.ITYPE_10G) ||
					(point1 != null && point1 instanceof GigEDescriptor.Port && point2 instanceof GigEDescriptor.Port)) 
				setCapacity(ExperimentXML.ITYPE_10G);
		}

	}
	protected LinkDescriptor(String lbl, ONLComponent p1, double bw, ExpCoordinator ec)
	{
		super(lbl, ONLComponent.LINK_LBL, ec);
		if (p1 instanceof GigEDescriptor) point1 = ((GigEDescriptor)p1).getUnlinkedPort();
		else point1 = p1;
		bandwidth = bw;
		addToDescription(DIRECTION, getDirectionString());
		addToDescription(BW, new String("Bandwidth: " + bandwidth + " Mbps"));
		addToDescription(POINT1, new String("Point 1: " + point1.toString()));
		if (point1 != null && (point1.getInterfaceType().equals(ExperimentXML.ITYPE_10G))) setCapacity(ExperimentXML.ITYPE_10G);
		else
			setCapacity(ExperimentXML.ITYPE_1G);
		setLinkable(false);
	}
	public String getInterfaceType() { return (getCapacity());}
	public String getCapacity()
	{
		String rtn = properties.getProperty(INTERFACE_TYPE);
		if (rtn != null) return rtn;
		else return ExperimentXML.ITYPE_1G;
	}
	public void setCapacity(String s) 
	{ 
		ExpCoordinator.print(new String("LinkDescriptor(" + getLabel() + ").setCapacity " + s), TEST_10G);
		properties.setProperty(INTERFACE_TYPE, s);
	}
	public String getDirectionString()
	{
		if (direction == BIDIRECTIONAL) return "bidirectional";
		else return "unidirectional";
	}
	public void setDirection(String s)
	{
		if (s.equals("bidirectional")) direction = BIDIRECTIONAL;
		else direction = UNIDIRECTIONAL;
	}
	public ONLGraphic getGraphic()
	{
		if (graphic == null)
		{
			graphic = new LinkGraphic(this);
		}
		return graphic;
	}
	public void setGraphic(LinkGraphic lg) 
	{
		graphic = lg;
		graphic.setLinkDescriptor(this);
	}
	public void writeExpDescription(ONL.Writer tw)  throws IOException
	{
		if (point1 == null || point2 == null) return;
		super.writeExpDescription(tw);
		tw.writeString(getDirectionString());
		tw.writeDouble(bandwidth);
		tw.writeString(point1.toString());      
		tw.writeString(point2.toString());
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		if (point1 == null || point2 == null) return;
		xmlWrtr.writeStartElement(getXMLElementName());
		super.writeXML(xmlWrtr);
		xmlWrtr.writeStartElement(ExperimentXML.POINT1);
		point1.writeXMLID(xmlWrtr);   
		xmlWrtr.writeEndElement();    
		xmlWrtr.writeStartElement(ExperimentXML.POINT2);  
		point2.writeXMLID(xmlWrtr);
		xmlWrtr.writeEndElement();    
		xmlWrtr.writeEndElement();  //link  
	}
	protected String getXMLElementName() { return ExperimentXML.LINK;}
	public ONLComponent getPoint1() { return point1;}
	public ONLComponent getPoint2() { return point2;}
	protected void setPoint1(ONLComponent p1)
	{
		point1 = p1;
		if (point1 != null)
		{
			if ((point1.getInterfaceType().equals(ExperimentXML.ITYPE_10G))) setCapacity(ExperimentXML.ITYPE_10G);
			else
				setCapacity(ExperimentXML.ITYPE_1G);
			ExpCoordinator.print(new String("LinkDescriptor.setPoint1(" + point1.getLabel() + ")"), ExperimentXML.TEST_XML);
		}
		else
			ExpCoordinator.print(new String("LinkDescriptor.setPoint1(null)"), ExperimentXML.TEST_XML);
	}

	public void setPoint2(ONLComponent p2)
	{
		if (point2 == null)
		{
			if (p2 instanceof GigEDescriptor)
			{
				point2 = ((GigEDescriptor)p2).getUnlinkedPort();
			}
			else
				point2 = p2;

			if (point2 != null)
				ExpCoordinator.print(new String("LinkDescriptor.setPoint2(" + point2.getLabel() + ")"), ExperimentXML.TEST_XML);
			else
				ExpCoordinator.print(new String("LinkDescriptor.setPoint2(null)"), ExperimentXML.TEST_XML);
			addToDescription(POINT2, new String("Point 2: " + point2.toString()));
			if (point1 != null) point1.addLink(this);
			if (point2 != null) 
			{
				point2.addLink(this);
				if (point2.getInterfaceType().equals(ExperimentXML.ITYPE_10G) ||
						(point1 != null && point1 instanceof GigEDescriptor.Port && point2 instanceof GigEDescriptor.Port)) 
					setCapacity(ExperimentXML.ITYPE_10G);
			}
		}
	}

	public double getBandwidth() { return bandwidth;}
	public void setBandwidth(double b) { bandwidth = b;}

	public ONLComponent.Undoable getDeleteEdit() 
	{ 
		return (new DeleteEdit(this, expCoordinator.getTopology(), expCoordinator.getCurrentExp()));
		//else return null;
	}
	public void setSelected(boolean b) 
	{
		if (!fixed)
		{
			super.setSelected(b);
			if (graphic != null) graphic.repaint();
		}
	}
	public boolean isFixed() { return fixed;}
	protected void setFixed(boolean f) { fixed = f;}
	public boolean isEqual(ONLComponent c)
	{
		if (c instanceof LinkDescriptor)
		{
			LinkDescriptor ld = (LinkDescriptor)c;
			return ((point1 == ld.point1) && (point2 == ld.point2));
		}
		return false;
	}
	public void setLabel(String l) 
	{
		super.setLabel(l);
		setCommittedLabel(l);
	}
	public void print(int level)
	{
		String p1 = "";
		String p2 = "";
		if (point1 != null) p1 = point1.getLabel();
		if (point2 != null) p2 = point2.getLabel();
		ExpCoordinator.printer.print(new String("LinkDescriptor:" + getLabel() + " point1:" + p1 + " point2:" + p2), level);
	}
	public String getFullString()
	{
		String rtn = new String ("Link:" + getLabel());
		if (point1 != null) rtn = rtn.concat(" from:" + point1.getLabel() + " " + point1.getIPAddr().toString() + "/" + point1.getNetmaskString());
		if (point2 != null) rtn = rtn.concat(" to:" + point2.getLabel()+ " " + point2.getIPAddr().toString() + "/" + point2.getNetmaskString());

		return rtn;
	}
	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new LContentHandler(this, exp_xml));}
	protected boolean merge(ONLComponent c)
	{
		if (c instanceof LinkDescriptor) 
		{
			return true;
		}
		else return false;
	}

	public String printString()
	{
		String rtn = new String(getLabel() + ": point1:");
		if (point1 != null) rtn = rtn.concat(point1.getLabel());
		else rtn = rtn.concat("null");
		rtn = rtn.concat(" point2:");
		if (point2 != null) rtn = rtn.concat(point2.getLabel());
		else rtn = rtn.concat("null");
		return rtn;
	}
}
