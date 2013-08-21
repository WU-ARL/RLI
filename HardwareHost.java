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
 * File: HostDescriptor.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: HostDescriptor.java
 *
 * Date Created: 04/26/2010
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.Vector;
import java.lang.String;
import java.io.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.lang.reflect.Array;
import javax.swing.*;
import java.text.ParseException;
import javax.xml.stream.*;
import org.xml.sax.*;


public class HardwareHost extends Hardware 
{
	public static final String ORIG_LABEL = ExperimentXML.ORIG_LABEL;
	public static final String ORIG_IPADDR = ExperimentXML.ORIG_IPADDR;


	protected static class ButtonAction extends ONLGraphic.ButtonListener
	{
		public ButtonAction(ONLComponent c)
		{
			super(c);
		}
		//MouseListener
		public void mouseClicked(MouseEvent e)
		{
			HostGraphic hgraphic = ((HostGraphic.HButton)e.getSource()).getHostGraphic();
			((HardwareHost)getONLComponent()).showMenu(hgraphic, hgraphic.getCenter());
		}
		//end MouseListener
	}
	public HardwareHost(String lbl, ONL.Reader tr, ExpCoordinator onlcd) throws IOException
	{
		super(lbl, tr, onlcd);
		setOriginalLabel(lbl);
		setComponentType(ExperimentXML.ENDPOINT);
	}
	public HardwareHost(int ndx, ExpCoordinator ec, HardwareSpec hw_type) 
	{ 
		this(ndx, ec, hw_type.getDefaultSubtype());
	}
	public HardwareHost(int ndx, ExpCoordinator ec, HardwareSpec.Subtype hw_type) 
	{ 
		super(ndx, ec, hw_type);
		setOriginalLabel(getLabel());
		setComponentType(ExperimentXML.ENDPOINT);
	}
	public HardwareHost(String uri, Attributes attributes)
	{
		super(uri,attributes);
	}


	public ContentHandler getContentHandler(ExperimentXML expXML)
	{
		ContentHandler ch = new Hardware.HWContentHandler(this, expXML){
			public void characters(char[] ch, int start, int length)
			{
				super.characters(ch, start, length);
				if (getCurrentElement().equals(ExperimentXML.ORIG_LABEL)) setOriginalLabel(new String(ch, start, length));
				if (getCurrentElement().equals(ExperimentXML.ORIG_IPADDR)) setProperty(ORIG_IPADDR, new String(ch, start, length));
				//if (getCurrentElement().equals(ExperimentXML.CPADDR)) setRealHost(new String(ch, start, length));
			}
			/*
			public void endElement(String uri, String localName, String qName)
			{
				super.endElement(uri, localName, qName);
				if (localName.equals(ExperimentXML.NODE))
				{
					//setDisplayLabel();
					//addStateListener(REALHOST);
				}
			}*/
		};
		return ch;
	}

	public void writeXMLParams(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.ORIG_LABEL);
		xmlWrtr.writeCharacters(getProperty(ExperimentXML.ORIG_LABEL));
		xmlWrtr.writeEndElement();
		xmlWrtr.writeStartElement(ExperimentXML.ORIG_IPADDR);
		xmlWrtr.writeCharacters(getProperty(ExperimentXML.ORIG_IPADDR));
		xmlWrtr.writeEndElement();
		super.writeXMLParams(xmlWrtr);

	}

	private void setOriginalLabel(String olbl)
	{
		String tmp_tpnm = getHWSubtype().getLabel();
		//if (!olbl.startsWith(tmp_tpnm)) return;
		ExpCoordinator.print(new String("HardwareHost.setOriginalLabel olbl:" + olbl), 3);
		properties.setProperty(ORIG_LABEL, olbl);
		String ndx_str = olbl.substring(tmp_tpnm.length() + 1);
		properties.setProperty(INDEX, ndx_str);
		return;
	}
	public ONLGraphic getGraphic()
	{
		if (graphic == null)
		{
			graphic = new HostGraphic(this, getIconColor());
			((HostGraphic)graphic).addNodeListener(new ButtonAction(this));
			//graphic.addNodeListener(((HostMonitor)monitorable).getButtonAction());
			//addPropertyListener(LABEL, graphic);
			addPropertyListener(ONLComponent.LABEL, graphic);
		}
		return (graphic);
	}
	public void setIPAddr(String ip)
	{
		super.setIPAddr(ip);
		//if (getProperty(ORIG_IPADDR) == null) setProperty(ORIG_IPADDR, ip);
		String strarray[] = ip.split("\\.");
		if (ExpCoordinator.isOldSubnet())
		{		
			String orig_ip = getPort(0).getProperty(ORIG_IPADDR);
			if (orig_ip != null && orig_ip.equals(ip)) return;
			OldSubnetManager.Subnet osn = (OldSubnetManager.Subnet)getSubnet();
			if (osn == null) ExpCoordinator.print(new String("HardwareHost(" + getLabel() + ").setIPAddr " + ip + " osn null"), TEST_SUBNET);
			else
			{
				String orig_sn = "null";
				if (getOriginalSubnet() != null) orig_sn = getOriginalSubnet().getBaseIP().toString();
				ExpCoordinator.print(new String("HardwareHost(" + getLabel() + ").setIPAddr " + ip + " osn:" + osn.getBaseIP() + " orig:" + orig_sn), TEST_SUBNET);
			}
			if (osn != null && osn != getOriginalSubnet())
			{
				int ndx = (Integer.parseInt(strarray[3])) - osn.getSubIndex(); 
				char port_ch = ONL.portLabels.charAt(ndx);
				if (ndx > 1 || (getPort(0).getLinkedTo() != null && !getPort(0).getLinkedTo().isRouter()))
					setLabel(new String("n"+ osn.getIndex() + "p" + osn.getSubnetPort() + port_ch));
				else
					setLabel(new String("n"+ osn.getIndex() + "p" + osn.getSubnetPort()));
			}
		}
		else
		{
			setLabel(new String("h" + strarray[2] + "x" + strarray[3]));
		}
	}

	public void setSubnet(SubnetManager.Subnet sn)
	{
		SubnetManager.Subnet tmp_sn = getSubnet();
		super.setSubnet(sn);
		if (sn != tmp_sn && Topology.DefaultHostRtAction.isOn())
		{
		  if (tmp_sn != null) removeHostGeneratedRoute();
		  if (sn != null) 
		  {
			  if (ExpCoordinator.isOldSubnet())
				  addHostGeneratedRoute(sn.getBaseIP().toString(), 28, 0, new ONL.IPAddress());
			  else
				  addHostGeneratedRoute(sn.getBaseIP().toString(), 24, 0, new ONL.IPAddress());
		  }
		}
		if (sn == null) setLabel(properties.getProperty(ORIG_LABEL));
	}

	private void removeHostGeneratedRoute()
	{
		ExpCoordinator.print(new String("HardwareHost(" + getLabel() + ").removeHostGeneratedRoute " ), Topology.TEST_DEFRTS);
		HWRouteTable rt = (HWRouteTable)getPort(0).getTableByType(ONLComponent.RTABLE);
		if (rt != null) rt.removeHostGeneratedRoute();
	}
	private void addHostGeneratedRoute(String ip, int mask, int nhp, ONL.IPAddress nhip)
	{
		ExpCoordinator.print(new String("HardwareHost(" + getLabel() + ").addHostGeneratedRoute " + ip + "/" + mask + " " + nhp + ":" + nhip.toString()), Topology.TEST_DEFRTS);
		HWRouteTable rt = (HWRouteTable)getPort(0).getTableByType(ONLComponent.RTABLE);
		if (rt != null) rt.addHostGeneratedRoute(ip, mask, nhp, nhip);
	}
	public String getOriginalLabel() { return (properties.getProperty(ORIG_LABEL));}
	protected void initializeMenu()
	{
		if (menu == null)
		{
			super.initializeMenu();
			int max = ports.size();
			for (int i = 0; i < max; ++i)
			{
				menu.addCfg(((Hardware.Port)ports.elementAt(i)).getCfgMenuCopy());
			}
		}
	}
	public void removeLink(LinkDescriptor lnk) 
	{
		if ((lnk.getPoint1() == this && linkedTo == lnk.getPoint2()) ||
				(lnk.getPoint2() == this && linkedTo == lnk.getPoint1())) 
		{
			//SUBNET:remove//if (linkedTo == getSubNetRouter() || linkedTo.getSubNetRouter() == getSubNetRouter()) setSubNetRouter(null);
			linkedTo = null; 
		}
	}
}
