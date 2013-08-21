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
 * File: NSPDescriptor.java 
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
import java.io.IOException;
import java.lang.String;
import java.awt.event.MouseListener;
import java.util.Vector;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.xml.stream.*;
import org.xml.sax.*;
//import onlrouting.*;

public class NSPDescriptor extends Router //WUGSDescriptor
{
	public static final int NUMPORTS = 8;
	public static final String BITFILE = "bitfile";
	public static final String DQ = "DQ";
	public static final String PLUGIN_CLASSES = "pluginClasses";
	//private String IPBaseAddr = "";
	//private double internalBW = 0; //internal bandwidth in Mbps
	private PluginClasses pluginClasses = null;
	private DebugTable debugTable = null;

	///////////////////////////////////////////////////////// NSPDescriptor.DQAction ///////////////////////////////////////////////////////////////////////////

	private class DQAction extends AbstractAction //implements ListSelectionListener, KeyListener
	{
		private NSPDescriptor nsp = null;
		private TextFieldwLabel textField = null;
		private JCheckBox on_off = null;

		private class DQonListener implements ActionListener
		{
			public DQonListener() {}
			public void actionPerformed(ActionEvent e) 
			{
				textField.setEditable(on_off.isSelected());
			}
		}

		public DQAction(NSPDescriptor n)
		{
			super("Queueing");
			nsp = n;
			textField = new TextFieldwLabel(30, "Switch Bandwidth(Mbps):");
			on_off = new JCheckBox("Distributed Queueing On");
			on_off.addActionListener(new DQonListener());
		}
		public void actionPerformed(ActionEvent e) 
		{
			textField.setValue(String.valueOf(nsp.getP0SwitchBW()));
			JLabel lbl = new JLabel("Turning DQ on/off takes place after Commit.");
			on_off.setSelected(nsp.getPropertyBool(DQ));
			textField.setEditable(on_off.isSelected());

			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};

			int rtn = JOptionPane.showOptionDialog((nsp.getGraphic()), 
					new Object[]{textField, on_off, lbl}, 
					"Queueing", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				boolean dq_on = on_off.isSelected();
				if (dq_on && textField.getText().length() > 0)
					nsp.setInternalBW(Double.parseDouble(textField.getText())); 
				nsp.setDQ(dq_on);
			}
		}
	}

	///////////////////////////////////////////////////////// NSPDescriptor.BitfileAction /////////////////////////////////////////////////////////////////////////
	protected static class BitfileAction extends AbstractAction //implements ListSelectionListener, KeyListener
	{
		private NSPDescriptor nsp = null;
		public BitfileAction(NSPDescriptor n)
		{
			super("Set FPX Bitfile");
			nsp = n;
		}
		public void actionPerformed(ActionEvent e) 
		{
			TextFieldwLabel tfield = new TextFieldwLabel(30, "FPX rad bitfile");
			String tmp = nsp.getProperty(BITFILE);
			if (tmp != null) tfield.setText(tmp);
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};

			int rtn = JOptionPane.showOptionDialog((nsp.getGraphic()), 
					tfield, 
					"Add Parameter", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				nsp.setProperty(BITFILE, tfield.getText());
			}
		}
	}

	///////////////////////////////////////////////////////// NSPDescriptor.NSPContentHandler //////////////////////////////////////////////////////////////////
	private class NSPContentHandler extends Hardware.HWContentHandler
	{
		public NSPContentHandler(NSPDescriptor nsp, ExperimentXML exp_xml)
		{
			super(nsp, exp_xml);
			//initialize();
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			super.startElement(uri, localName, qName, attributes);
			if (localName.equals(ExperimentXML.PCLASSES))
				expXML.setContentHandler(getPluginClasses().getContentHandler(expXML));
		}

		public void characters(char[] ch, int start, int length)
		{
			super.characters(ch, start, length);
			if (getCurrentElement().equals(BITFILE)) getComponent().setProperty(BITFILE, new String(ch, start, length));
			if (getCurrentElement().equals(DQ)) getComponent().setProperty(DQ, true);
			if (getCurrentElement().equals(INTERNALBW)) ((NSPDescriptor)getComponent()).setInternalBW(Double.parseDouble(new String(ch, start, length)));
		}     

		public void endElement(String uri, String localName, String qName)
		{
			super.endElement(uri, localName, qName);
			if (localName.equals(ExperimentXML.LABEL)) initialize();
		}
	}


	///////////////////////////////////////////////////////// NSPDescriptor Methods ////////////////////////////////////////////////////////////////////////////
	public NSPDescriptor(String lbl, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{  
		super(lbl, ONLComponent.NSP_LBL, tr, ec, NSPMonClasses.NUMBEROFPORTS);
		setInternalBW(tr.readDouble());
		if (tr.getVersion() >= 2.5) 
		{
			String tmp_str = tr.readString();
			if (tmp_str.length() > 0)
				setProperty(BITFILE, tmp_str);
		}

		if (tr.getVersion() >= 1) setProperty(DQ, tr.readBoolean());
		initializePorts();
		if (tr.getVersion() >= 2.5) 
		{
			for (int i = 0; i < numPorts; ++i)
			{
				((RouterPort)getPort(i)).setSamplingTypes(tr);
			}
		}
		setComponentType(ExperimentXML.ROUTER);
	}
	public void initializeMonitorable() //override to create and initialize monitorable if there is one otw make empty
	{
		monitorable = new NSPMonitor(daemons, this, expCoordinator.getMonitorManager());
	}
	public NSPDescriptor(String lbl, ExpCoordinator ec)
	{
		super(lbl, ONLComponent.NSP_LBL, ec, NSPMonClasses.NUMBEROFPORTS);
		if (ec == null) ExpCoordinator.printer.print("NSPDescriptor::NSPDescriptor expCoor is null", 1);   
		if (pluginClasses == null) 
			ExpCoordinator.print("NSPDescriptor pluginClasses null", 4);
		setComponentType(ExperimentXML.ROUTER);
		initializePorts();
	}
	public NSPDescriptor(String lbl, String cp, String ipaddr, double ibw, ExpCoordinator ec)
	{
		super(lbl, ONLComponent.NSP_LBL, ec, NSPMonClasses.NUMBEROFPORTS);
		setComponentType(ExperimentXML.ROUTER);
		setCPHost(cp);
		if (ec == null) ExpCoordinator.printer.print("NSPDescriptor::NSPDescriptor expCoor is null", 1);
		setBaseIPAddr(ipaddr);
		setInternalBW(ibw);
		if (pluginClasses == null) 
			ExpCoordinator.print("NSPDescriptor pluginClasses still null", 4);
		initializePorts();
	}
	public NSPDescriptor(String uri, Attributes attributes)
	{
		super(uri, attributes);
		setProperty(DQ, false);
	}
	protected void initializeActions()
	{
		ExpCoordinator.print("NSPDescriptor.initializeActions", 5);
		if (getInternalBW() == 0) setInternalBW(QueueTable.DEFAULT_SWBANDWIDTH);
		super.initializeActions();
		addStateListener(INTERNALBW);
		actions.add(new DebugTable.DTAction("Debug", this));
		if (getExpCoordinator().isManagement())
		{
			if (getExpCoordinator().isManagement()) actions.add(new BitfileAction(this));
		}
		if (getProperty(DQ) == null) setProperty(DQ, false);
		actions.add(new DQAction(this));
		//if (pluginClasses == null) 
		//{
		//  ExpCoordinator.print("NSPDescriptor.initializeActions initializing pluginClasses", 4);
		//  pluginClasses = new PluginClasses(this);
		//}
		//if (pluginClasses == null) 
		//ExpCoordinator.print("NSPDescriptor.initializeActions pluginClasses still null", 4);
		addStateListener(DQ);
	}
	public void initializeDaemons()
	{
		//super.initializeDaemons();
		addDaemon(ONLDaemon.WUGSC, ONLDaemon.WUGSC_PORT);
		addDaemon(ONLDaemon.NSPC, ONLDaemon.NSPC_PORT);
	}
	protected Hardware.Port createPort(int ndx)
	{
		NSPPort p = new NSPPort(this, ndx, expCoordinator);
		if (ndx == 0) p.setLinkable(false);
		((NSPPortMonitor)p.getMonitorable()).initializeMenus();
		return p;
		//return (new NSPPort(this, ndx, expCoordinator));
	}
	public void initializePorts()
	{
		super.initializePorts();
		if (properties.getProperty(DQ) == null) setDQ(false);
		else 
		{
			setDQ(getPropertyBool(DQ));
			ExpCoordinator.print(new String("NSPDescriptor.initializePorts dq " + getPropertyBool(DQ)), 2);
		}
	}

	public void writeExpDescription(ONL.Writer tw)  throws IOException
	{
		super.writeExpDescription(tw);
		tw.writeDouble(getInternalBW());
		tw.writeString(getProperty(BITFILE));
		tw.writeBoolean(getPropertyBool(DQ));
		for (int i = 0; i < numPorts; ++i)
		{
			((RouterPort)getPort(i)).writeSamplingTypes(tw);
		}
	}

	public void writeXMLParams(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		//right now not doing this but in future should be reboot and config params
		super.writeXMLParams(xmlWrtr);
		xmlWrtr.writeStartElement(INTERNALBW);	
		xmlWrtr.writeCharacters(properties.getProperty(INTERNALBW));
		xmlWrtr.writeEndElement();
		xmlWrtr.writeStartElement(BITFILE);	
		xmlWrtr.writeCharacters(properties.getProperty(BITFILE));
		xmlWrtr.writeEndElement();
		if (getPropertyBool(DQ))
		{
			xmlWrtr.writeStartElement(DQ);	
			xmlWrtr.writeEndElement();
		}
		xmlWrtr.writeStartElement(ExperimentXML.PCLASSES);
		pluginClasses.writeXML(xmlWrtr);
		xmlWrtr.writeEndElement();
	}
	public void readParams(ONL.Reader reader) throws IOException
	{
		setCPHost(reader.readString());
	}
	public void writeParams(ONL.Writer writer) throws IOException
	{
		writer.writeString(getCPHost());
	}

	/*
      public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
      {
      xmlWrtr.writeStartElement("nsp");
      super.writeXML(xmlWrtr);
      xmlWrtr.writeEndElement();
      }
	 */


	protected void setBitfile(String bitfile)
	{
		ExpCoordinator.printer.print(new String("NSPDescriptor bitfile = " + bitfile), 2);
		setProperty(BITFILE, bitfile);
		addToDescription(BITFILE, new String("Bitfile: " + bitfile));
	}
	private double getP0SwitchBW()
	{
		return (((NSPPort)getPort(0)).getSWBandwidth());
	}
	public PluginClasses getPluginClasses() 
	{ 
		if (pluginClasses == null) 
		{
			ExpCoordinator.print("NSPDescriptor.getPluginClasses initializing pluginClasses", 4);
			pluginClasses = new PluginClasses(this);
		}
		return pluginClasses;
	}
	private void setDQ(boolean on)
	{
		if (on != getPropertyBool(DQ))
		{
			//should inform ports if dq is on
			NSPPort tmp_port;
			double bw = 0;
			double max_bw = 0;
			//find max switch bandwidth and set all ports to same
			for (int i = 0; i < numPorts; ++i)
			{
				tmp_port = (NSPPort)ports.elementAt(i);
				bw = tmp_port.getSWBandwidth();
				if (bw > max_bw) max_bw = bw;
			}
			setInternalBW(max_bw);
			properties.setProperty(DQ, on);
		}
		else//otherwise just force it
		{
			NSPPort tmp_port = (NSPPort)ports.elementAt(0);
			tmp_port.getQueueTable(true).setDQ(getPropertyBool(DQ));
		}
	}
	public DebugTable getDebugTable()
	{
		if (debugTable == null) 
		{
			debugTable = new DebugTable(this);
		}
		return debugTable;
	}
	public boolean isRouter() { return true;}
	//public ContentHandler getPortContentHandler(ExperimentXML expXML) { return (new NSPPort.NPContentHandler(this, expXML));}
	public ContentHandler getContentHandler(ExperimentXML expXML)
	{
		return (new NSPContentHandler(this, expXML));
	}  
	protected boolean mergeParams(ONLComponent c)
	{
		boolean rtn = true;
		if (c instanceof NSPDescriptor)
		{
			//rtn = super.mergeParams(c);
			if (getPropertyBool(DQ) != c.getPropertyBool(DQ))
			{
				String str1 = "on";
				String str2 = "off";
				if (!getPropertyBool(DQ))
				{
					str1 = "off";
					str2 = "on";
				}
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String(getLabel() + " DQ Inconsistency master has DQ:" + str1 + " other has DQ:" + str2 + "\n")),
						"DQ Inconsistency",
						"DQ Inconsistency",
						StatusDisplay.Error.LOG)); 
			}
		}
		return rtn;
	}
}
