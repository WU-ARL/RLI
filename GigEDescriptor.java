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
 * File: GigEDescriptor.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/25/2004
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
import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;
import javax.swing.event.*;
import javax.xml.stream.*;
import org.xml.sax.*;

public class GigEDescriptor extends ONLComponent
{
	//public static final String NSP = "nsplabel";
	//public static final String NSPPORT = "nspport";
	public static final int TEST_GRAPHIC = 5;
	//public static final int TEST_10G = 4;
	public static final String NSPINDEX = "nspindex";
	protected Graphic graphic = null;
	//private Port[] ports = null;
	private Vector ports = null;
	//private int numPorts = 0;
	protected static final String portLabels = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	protected static class ButtonAction extends ONLGraphic.ButtonListener
	{
		public ButtonAction(ONLComponent c)
		{
			super(c);
		}
		//MouseListener
		public void mouseClicked(MouseEvent e)
		{
			if (getONLComponent().isMonitoring())
			{
				Graphic button = (Graphic)e.getSource();
				Point2D pnt = button.getCenter();
				if (e.getButton() != MouseEvent.BUTTON1)
				{ 
					getONLComponent().showDescription(button, pnt);
				}
			}
		}
		//end MouseListener
	}
	public static class Port extends ONLComponent.PortBase
	{
		private final String PORTID = "portid";
		public static final int MAX_SUBNET_INDEX = 14;
		public Port(String lbl, ONL.Reader tr, ExpCoordinator onlcd) throws IOException
		{
			super(lbl, new String(ONLComponent.VGIGE_LBL + ONLComponent.PORT_LBL), tr, onlcd);
			setParentFromString(tr.readString());
			setID(tr.readInt());
			setInterfaceType(((GigEDescriptor)getParent()).getInterfaceType());
			//setIPAddr();
		}
		protected Port(GigEDescriptor gige, int p, ExpCoordinator ec)
		{
			super(gige, new String("port" + p), new String(ONLComponent.VGIGE_LBL + ONLComponent.PORT_LBL), ec);
			setID(p);
			setInterfaceType(gige.getInterfaceType());
			//setIPAddr();
		}
		public ONLGraphic getGraphic() { return (getParent().getGraphic());}
		/*
    private void setIPAddr()
      {
	if (getSubNetRouter() != null)
	  {
	      ONLComponent.PortInterface sn = getSubNetRouter();
	      if (sn instanceof ONLComponent.PortInterface)
		{
		  if (subnetIndex != null && subnetIndex.getPortInterface() != sn)
		    {
		      subnetIndex.getPortInterface().freeIndex(subnetIndex());
		      subnetIndex = null;
		    }
		  if (subnetIndex == null) 
		    {
		      subnetIndex = sn.getNewIndex();
		      setIPAddr(subnetIndex.getIPAddr());
		    }
		}
	    }
	else
	    {
		String ip_base = ((GigEDescriptor)getParent()).getIPAddr();
		if (ip_base != null) setBaseIPAddr(ip_base);
		else setBaseIPAddr("192.168.50.50");
	    }
      }
    protected void setBaseIPAddr(String ip_base)
      {
	if (ip_base != null)
	  {
	    ExpCoordinator.printer.print(new String("GigE.Port.getIPAddr ip_base = " + ip_base), 2);
	    String[] strarray = ip_base.split("\\.");
	    int tmp = Integer.parseInt(strarray[3]);
	    tmp += (getID() - 1);
	    setIPAddr(new String(strarray[0] + "." + strarray[1] + "." + strarray[2] + "." + tmp));
	  }
      else 
	{
	  ExpCoordinator.printer.print(new String("GigE.Port.getIPAddr ip_base = null"), 1);
	  setIPAddr("");
	}
      }
		 */
		/*//SUBNET:remove
    public void setSubNetRouter(ONLComponent.PortInterface c)
    {
	if (getSubNetRouter() == c) return;
	super.setSubNetRouter(c);
	//setIPAddr();
	getParent().setSubNetRouter(c);
	ONLComponent lt = getLinkedTo();
	if (lt != null) lt.setSubNetRouter(c);
	}*/
		public Cluster.Instance getCluster() { return (getParent().getCluster());}
		/*//SUBNET:remove
    public ONLComponent.PortInterface.Index getNewIndex()
      {
	  if (getSubNetRouter() != null) return (getSubNetRouter().getNewIndex());
	  else return null;
      }
    public void freeIndex(PortInterface.Index in)
      {
	  if (in.getPortInterface() != null) in.getPortInterface().freeIndex(in);
      }
    public boolean isUsedUp()
      {
	  if (getSubNetRouter() != null) return (getSubNetRouter().isUsedUp());
	  else return false;
      }
    public int getNumFreeIndices() 
      {
	  if (getSubNetRouter() != null) return (getSubNetRouter().getNumFreeIndices());
	  else return 0;
      }
    public int getNumInterfaces(Vector v) 
      {
	  if (!v.contains(this))
	      {
		  v.add(this);
		  return (getParent().getNumInterfaces(v));
	      }
	  return 0;
	  }*/
		protected boolean merge(ONLComponent c)
		{
			if (c instanceof Port && ((Port)c).getID() == getID()) return true;
			else return false;
		}
	}//end inner class Port

	/////////////////////////////////////////////////////// GigEDescriptor.Graphic /////////////////////////////////////////////////////
	public static class Graphic extends ONLGraphic implements ONLComponentButton
	{
		private Ellipse2D.Double ellipse = null;
		private TopButtonListener buttonListener = null;
		public Graphic(GigEDescriptor gd)
		{
			super(gd);
			ExpCoordinator.print(new String("GigEDescriptor(" + gd.getLabel() +").Graphic itype:" + gd.getInterfaceType()), TEST_GRAPHIC);
			ellipse = new Ellipse2D.Double();
			setDoubleBuffered(true);
			setOpaque(false);
			setVisible(true);
			setForeground(Color.black);
			if (gd.getInterfaceType().equals(ExperimentXML.ITYPE_10G)) setBackground(Color.pink);
			else setBackground(new Color(249,211,162));
			buttonListener = new TopButtonListener();
			buttonListener.setEnabled(true);
			addMouseListener(buttonListener);
			addMouseMotionListener(buttonListener);
		}
		public boolean contains(int dx, int dy)
		{
			return (ellipse.contains(dx,dy));
		}
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			//ExpCoordinator.printer.print("GigEDescriptor.Graphic.paintComponent");
			Graphics2D g2 = (Graphics2D)g;
			Color oldColor = g2.getColor();
			g2.setColor(convertColor(getBackground()));
			g2.fill(ellipse);
			g2.setColor(getForeground());
			g2.draw(ellipse);
			drawLabel(g2);
			g2.setColor(oldColor);
		}
		public void setSize(int w, int h)
		{
			super.setSize(w,h);
			//ExpCoordinator.printer.print("GigEDescriptor.Graphic.setSize " + w + "x" + h);
			//Point p = getLocation();
			//ellipse.setFrame(p.getX(), p.getY(), w, h);
			ellipse.setFrame(0, 0, w, h);
		}
		public void setSize(double d)
		{
			this.setSize((int)d,(int)d);
			//Point p = getLocation();
			//ellipse.setFrame(p.getX(), p.getY(), (d-2), (d-2));
		}
		public void drawLabel(Graphics2D g2)
		{
			//place label of switch id in middle of button
			int labelOffsetX = 0;
			int labelOffsetY = 0;
			float fsize = 8;
			String label = onlComponent.getLabel();
			//label = label.concat(onlComponent.getInterfaceType());
			g2.setFont(getFont().deriveFont(fsize));
			FontMetrics fntmet = g2.getFontMetrics();
			if (fntmet != null)
			{
				labelOffsetX = fntmet.stringWidth(label)/2;
				labelOffsetY = fntmet.getHeight()/4;
			}
			int x = (int)(ellipse.getCenterX() - labelOffsetX);
			int y = (int)(ellipse.getCenterY() + labelOffsetY);
			//int y = (int)(ellipse.getCenterY());

			g2.setColor(getForeground());
			g2.drawString(label, x, y);
		}
		public Point2D getCenter()
		{
			return (new Point2D.Double(ellipse.getCenterX(), ellipse.getCenterY()));
		}
		public void addNodeListener(ONLGraphic.ButtonListener l) 
		{
			buttonListener.addAction(l);
		}
		public void removeNodeListener(ONLGraphic.ButtonListener l) 
		{
			buttonListener.removeAction(l);
		}
		public Point2D getLinkPoint() 
		{ 
			Point rtn = new Point((int)ellipse.getCenterX(), (int)ellipse.getCenterY());
			Point loc = getLocation();
			rtn.translate((int)loc.getX(), (int)loc.getY());
			return (rtn);
		}
	}

	public GigEDescriptor(String lbl, String tp, ONL.Reader tr, ExpCoordinator onlcd) throws IOException
	{
		super(lbl, tp, tr, onlcd);
		//numPorts = tr.readInt();
		initializePorts();
		setComponentType(ExperimentXML.SWITCH);
	}
	protected GigEDescriptor(String lbl, ExpCoordinator ec)
	{
		super(lbl, ONLComponent.VGIGE_LBL, ec);
		//numPorts = n_ports;
		initializePorts();
		setComponentType(ExperimentXML.SWITCH);
	}
	protected GigEDescriptor(String uri, Attributes attributes)
	{
		super(uri, attributes);
		//numPorts = Integer.parseInt(attributes.getValue(uri, "numPorts"));
		setInterfaceType(attributes.getValue(uri, ExperimentXML.INTERFACE_TYPE));
		if (attributes.getValue(ExperimentXML.ORIG_SUBNET) != null)
		{
			try
			{
				SubnetManager.Subnet sn = SubnetManager.getSubnet(Integer.parseInt(attributes.getValue(ExperimentXML.ORIG_SUBNET)));
				if (sn != null) setOriginalSubnet(sn);
			} catch (SubnetManager.SubnetException e){ ExpCoordinator.print(new String("Hardware.HWContentHandler.startElement error:" + e.getMessage()));}
		}
	}
	public void setSubnet(SubnetManager.Subnet sn)
	{
		super.setSubnet(sn);
		if (sn == null) subnet = getOriginalSubnet();
	}
	protected void setOriginalSubnet(SubnetManager.Subnet osn) 
	{ 
		SubnetManager.Subnet old_osn = getOriginalSubnet();
		if (old_osn == null && osn != null)
		{
			super.setOriginalSubnet(osn);
			osn.setSwitch(this);
		}
	}

	public ContentHandler getContentHandler(ExperimentXML expXML)
	{
		ContentHandler ch = new ONLComponent.ONLContentHandler(this, expXML){
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{         
				super.startElement(uri, localName, qName, attributes);
				if (getCurrentElement().equals(ExperimentXML.POSITION))
				{
					component.getGraphic().setLocation(Integer.parseInt(attributes.getValue(uri, ExperimentXML.XCOORD)), Integer.parseInt(attributes.getValue(uri, ExperimentXML.YCOORD)));
				}
			}
			public void characters(char[] ch, int start, int length)
			{
				super.characters(ch, start, length);
				if (getCurrentElement().equals(ExperimentXML.EXPADDR)) setIPAddr(new String(ch, start, length));
			}
			public void endElement(String uri, String localName, String qName)
			{
				super.endElement(uri, localName, qName);
				if (localName.equals(ExperimentXML.NODE))
				{
					initializePorts();
				}
			}
		};
		return ch;
	}


	public void writeExpDescription(ONL.Writer tw) throws IOException  
	{
		super.writeExpDescription(tw);
		//tw.writeInt(numPorts);
	}

	private void initializePorts()
	{
		ports = new Vector();//Port[numPorts];
		//ExpCoordinator.printer.print("GigEDescriptor::initializePorts numPorts = " + numPorts);
		/*
      for (int i = 0; i < numPorts; ++i)
	{
	  ports[i] = new Port(this, i, expCoordinator);
	  addChild(ports[i]);
	}
      if (getIPAddr() == null) setIPAddr();
		 */
	}

	public ONLGraphic getGraphic()
	{
		if (graphic == null)
		{
			ExpCoordinator.print(new String("GigEDescriptor(" + getLabel() + ").getGraphic itype:" + getInterfaceType()), TEST_GRAPHIC);
			graphic = new Graphic(this);
			//graphic.addNodeListener(new ButtonAction(this));
		}
		return (graphic);
	}


	/*//SUBNET:remove
  public void setSubNetRouter(ONLComponent.PortInterface c)
    {
      if (c != getSubNetRouter())
	{
	  ONLComponent lt = null;
	  super.setSubNetRouter(c);
	  int max = ports.size();
	  for (int i = 0; i < max; ++i)
	    {
	      ((Port)ports.elementAt(i)).setSubNetRouter(c);
		//lt = ports[i].getLinkedTo();
		//if (lt != null) lt.setSubNetRouter(c);
	    }
	}
	}
	 */
/*
	public void setSubnet(SubnetManager.Subnet sn)
	{
		super.setSubnet(sn);
		if (ExpCoordinator.isOldSubnet())
		{
			int max = ports.size();
			for (int i = 0; i < max; ++i)
			{
				Port port = (Port)ports.elementAt(i);
				ONLComponent c = port.getLinkedTo();
				if (c != null) sn.addPort((ONLComponent.PortBase)c);
			}
		}
	}
	private void setRouterIndex(int ind) { setRouterIndex(ind, 1);}
	private void setRouterIndex(int ind, int pind)
	{
		properties.setProperty(NSPINDEX, ind);
		int nsp_ndx = ind;
		int max = ports.size();
		for (int i = 0; i < max; ++i)
		{
			Port port = (Port)ports.elementAt(i);
			ONLComponent c = port.getLinkedTo();
			if (c != null)
				ExpCoordinator.print(new String(getLabel() + ".setRouterIndex " + ind + " port" + i + " linked to " + c.getLabel()), 5);
			if (c != null && (c.getParent() instanceof HardwareHost))
			{
				char port_ch = portLabels.charAt((port.getID()));
				c.setLabel(new String("n" + nsp_ndx + "p" + pind + port_ch));
			}
		}
	}
*/
	public void setIPAddr(String ip)
	{
		if (ip != null)
		{
			setProperty(ONLComponent.IPADDR, ip);
			addToDescription(ONLComponent.IPADDR, ip);
			//Note:: should make ports property listeners but right now this is easiest
			/*
	 for (int i = 0; i < numPorts; ++i)
	   {
	       //ports[i].setBaseIPAddr(ip);
	   }
			 */
		}
	}
	private void setIPAddr()
	{
		String tmp_str = getLabel().substring(4);//end index
		setProperty(INDEX, tmp_str);
		int ndx = getPropertyInt(INDEX);
		setIPAddr(new String("192.168." + GIGE_IP + "." + ndx));
	}
	//public String getIPAddr() { return (getProperty(ONLComponent.IPADDR));}
	public Port getUnlinkedPort()
	{
		int max = ports.size();
		Port port = null;
		for (int i = 0; i < max; ++i)
		{
			port = (Port)ports.elementAt(i);
			if (!port.isLinked()) return (port);
		}

		return (addNewPort(ports.size()));
	}

	private GigEDescriptor.Port addNewPort(int i)
	{
		Port p = null;
		if (i < ports.size()) p = (Port)ports.elementAt(i);
		if (p == null)
		{
			int min = ports.size();
			for (int j = min; j <= i; ++j)
			{
				p = new Port(this, j, expCoordinator);
				//if (getSubNetRouter() != null) p.setSubNetRouter(getSubNetRouter()); //SUBNET:need subnet equivalent
				ports.add(p);
				addChild(p);
			}
		}
		return p;
	}
	protected Port getPort(int i) 
	{ 
		if (i >= ports.size()) return (addNewPort(i));
		else return ((Port)ports.elementAt(i));
	}
	protected Vector<ONLComponent.PortBase> getConnectedPorts()  //used by OldSubnetManager to get all of the ports connected through a switch
	{ 
		if (!isSwitch()) return null;
		Vector<ONLComponent.PortBase> rtn = new Vector<ONLComponent.PortBase>();
		int max = getNumPorts();
		GigEDescriptor.Port p;
		ONLComponent.PortBase lnkto;
		for (int i = 0; i < max; ++i)
		{
			p = getPort(i);
			rtn.add(p);
			lnkto = (ONLComponent.PortBase)p.getLinkedTo();
			if (lnkto != null) 
			{
				rtn.add(lnkto);
				/*
				Vector<ONLComponent.PortBase> lnkto_conn = lnkto.getConnectedPorts();
				if (lnkto_conn != null && !lnkto_conn.isEmpty()) rtn.addAll(lnkto_conn);
				*/
			}
		}
		return rtn;
	}
	/*//SUBNET:remove
  public int getNumInterfaces(Vector v) 
    {
	if (v.contains(this)) return 0;
	int rtn = 0;
	v.add(this);
	int max = ports.size();
	Port port;
	for (int i = 0; i < max; ++i)
	  {
	    port = (Port)ports.elementAt(i);
	    if (port.isLinked() && !(v.contains(port.getLinkedTo()))) 
	      {
		rtn += port.getLinkedTo().getNumInterfaces(v);
	      }
	  }
	return rtn;
    }
	 */
	public boolean isLinkable() 
	{
		return true;
		/*
	for (int i = 0; i < numPorts; ++i)
	  {
	    if (!ports[i].isLinked()) return (super.isLinkable());
	  }
	return false;
		 */
	}

	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		//xmlWrtr.writeAttribute(ExperimentXML.NUMPORTS, String.valueOf(numPorts));
		xmlWrtr.writeAttribute(ExperimentXML.INTERFACE_TYPE, getInterfaceType());
		if (getSubnet() != null) 
		{
			xmlWrtr.writeAttribute(ExperimentXML.SUBNET, String.valueOf(getSubnet().getIndex()));
			xmlWrtr.writeAttribute(ExperimentXML.ORIG_SUBNET, String.valueOf(getOriginalSubnet().getIndex()));
		}
		
		super.writeXML(xmlWrtr);
		graphic.writeXML(xmlWrtr);
		xmlWrtr.writeStartElement(ExperimentXML.EXPADDR);
		xmlWrtr.writeCharacters(getIPAddr().toString());
		xmlWrtr.writeEndElement(); //end expaddr
	}
	protected boolean merge(ONLComponent c)
	{
		if (c instanceof GigEDescriptor)
		{
			GigEDescriptor gige2 = (GigEDescriptor)c;
			int numPorts = ports.size();
			int numPorts2 = gige2.ports.size();
			if (numPorts2 != numPorts)
			{
				int error_type = StatusDisplay.Error.LOG;
				expCoordinator.addError(new StatusDisplay.Error((new String("GigE Error: Number of ports not equal master " + getLabel() + " has " + numPorts + " ports other has " + numPorts2 + " ports.\n")),
						"GigE Error",
						"GigE Error",
						error_type));  
				Port tmp_p;
				int i;
				if (numPorts < numPorts2)
				{
					Vector tmp_ports = new Vector(ports);//new Port[numPorts2];
					//for each port do a merge
					for (i = 0; i < numPorts; ++i)
					{
						tmp_p = (Port)tmp_ports.elementAt(i);
						if (!tmp_p.merge(gige2.getPort(i))) return false;
						tmp_p.setState(ONLComponent.INBOTH);
					}
					for (i = numPorts; i < numPorts2; ++i)
					{
						tmp_p = gige2.getPort(i);
						tmp_ports.add(tmp_p);
						tmp_p.setState(ONLComponent.IN2);
					}
					ports = tmp_ports;
				}	    
				else
				{
					//for each port do a merge
					for (i = 0; i < numPorts2; ++i)
					{
						tmp_p = getPort(i);
						if (!tmp_p.merge(gige2.getPort(i))) return false;
						tmp_p.setState(ONLComponent.INBOTH);
					}
					for (i = numPorts2; i < numPorts; ++i)
					{
						getPort(i).setState(ONLComponent.IN1);
					}
				}
				return true;
			}
			else 
			{
				//for each port do a merge
				for (int i = 0; i < numPorts; ++i)
				{
					if (!getPort(i).merge(gige2.getPort(i))) return false;
				}
				return true;
			}
		}
		else return false;
	}
	public String getInterfaceType()
	{
		String rtn = properties.getProperty(INTERFACE_TYPE);
		if (rtn != null) return rtn;
		else return ExperimentXML.ITYPE_1G;
	}
	public void setInterfaceType(String s) 
	{ 
		ExpCoordinator.print(new String("GigEDescriptor(" + getLabel() + ").setInterfaceType " + s), TEST_10G);
		String itype = s;
		if (itype == null) itype = ExperimentXML.ITYPE_1G;
		properties.setProperty(INTERFACE_TYPE, itype);
		if (ports != null)
		{
			int numPorts = ports.size();
			for (int i = 0; i < numPorts; ++i)
			{
				getPort(i).setInterfaceType(itype);
			}
		}
	}
	public int getNumPorts() { return (ports.size());}
	public ONLComponent getChild(String lbl, String tp, int level)
	{
		String portStart = new String(getLabel() + ":port");
		if (lbl.startsWith(portStart))
		{
			int p_index = Integer.parseInt(lbl.substring(portStart.length()));
			return (getPort(p_index));
		}
		return (super.getChild(lbl, tp, level));
	}
}
