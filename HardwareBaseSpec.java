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
 * File: HardwareSpec.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created:1/22/2007
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.Color;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.awt.Point;
import java.awt.Frame;
import java.awt.Component;

//NOTE: fix Instance writeParams and readParams so the right thing happens for NSP
public class HardwareBaseSpec implements ComponentXMLHandler.Spec
{
	public static final int TEST_SUBTYPE = 3;
	public static final int TEST_TYPENAME = 3;
	public static final String DAEMON_STR = "daemon";
	public static final String ROUTER = ExperimentXML.ROUTER;
	public static final String SWITCH = ExperimentXML.SWITCH;
	public static final String ENDPOINT = ExperimentXML.ENDPOINT;
	//public static final String NOT_ADDRESSED = "not_addressed";
	//public static final String ADDRESSED = "addressed";
	public static final String HOST = ExperimentXML.HOST;//"host";
	public static final String ANY = "any";
	public static final int HARDWARE_OPCODE = 128;
	protected java.io.File file;
	protected Vector fieldSpecs = null;
	protected Vector assignerSpecs = null;
	protected Vector monitorSpecs = null;
	protected Vector<CommandSpec> commandSpecs = null;
	protected Vector tableSpecs = null;
	protected Vector<CommandSpec> cfgCommands = null;
	protected CommandSpec rebootSpec = null;
	protected CommandSpec initSpec = null;
	protected CommandSpec stypeInitSpec = null;
	protected PortSpec portSpec = null;
	protected PortSpec vinterfaceSpec = null;
	protected String typeName = null;
	protected String resourceName = null;
	protected boolean has_daemon = false;
	protected int cpPort = -1;
	//private int numPorts = 0;
	protected double version = 0;
	protected boolean clusterOnly = false;
	//private boolean is_router = false;
	//private int routerType = NOT_ROUTER;
	protected String componentType = ANY;//NOT_ADDRESSED;
	private String description = "";
	private Color iconColor = null;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////// HardwareBaseSpec.XMLHandler ////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class XMLHandler extends ComponentXMLHandler //DefaultHandler
	{
		protected static final String R = "r";
		protected static final String G = "g";
		protected static final String B = "b";
		
		protected HardwareBaseSpec hardware = null;
		protected java.io.File file = null;
		protected String resourceName = null;

		public XMLHandler(XMLReader xmlr, java.io.File f)
		{
			this(xmlr);
			file = f;
		}

		public XMLHandler(XMLReader xmlr, String rnm)
		{
			this(xmlr);
			resourceName = rnm;
		}

		public XMLHandler(XMLReader xmlr)
		{
			super(xmlr);
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			super.startElement(uri, localName, qName, attributes);
			if (localName.equals(ExperimentXML.PORTS) || localName.equals(ExperimentXML.INTERFACES))
			{
				if (hardware.portSpec == null) hardware.setPortSpec(hardware.createPortSpec(uri, attributes));
				xmlReader.setContentHandler(hardware.portSpec.getXMLHandler(xmlReader, this));
			}
			if (localName.equals(ExperimentXML.COLOR))
			{
				hardware.setIconColor(Integer.parseInt(attributes.getValue(uri, R)),
						Integer.parseInt(attributes.getValue(uri, G)),
						Integer.parseInt(attributes.getValue(uri, B)));
			}
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("HardwareBaseSpec.XMLHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
			if (currentElement.equals(ExperimentXML.CLUSTERONLY)) hardware.clusterOnly = Boolean.valueOf(new String(ch, start, length)).booleanValue();
			if (currentElement.equals(ExperimentXML.DAEMON)) 
			{
				hardware.has_daemon = true;
				hardware.cpPort = Integer.parseInt(new String(ch, start, length));
			}
			if (currentElement.equals(ExperimentXML.CTYPE)) 
			{
				hardware.componentType = new String(ch, start, length);
			}
			if (currentElement.equals(ExperimentXML.DESCRIPTION))  hardware.description = new String(ch, start,length);
		}
	}// end inner class HardwareBaseSpec.XMLHandler

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////// HardwareBaseSpec.PortSpec ////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected static class PortSpec implements ComponentXMLHandler.Spec
	{
		private HardwareBaseSpec hardware = null;
		Vector fieldSpecs = null;
		Vector assignerSpecs = null;
		Vector monitorSpecs = null;
		Vector commandSpecs = null;
		Vector cfgCommands = null;
		Vector tableSpecs = null;
		int numPorts = 0;
		String interfaceType = ExperimentXML.ITYPE_1G;

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////// HardwareBaseSpec.PortSpec.XMLHandler /////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		public PortSpec(String uri, Attributes attributes, HardwareBaseSpec hw)
		{
			this(hw);
			if (attributes.getValue(ExperimentXML.NUMPORTS) != null) numPorts = Integer.parseInt(attributes.getValue(ExperimentXML.NUMPORTS));
			if (attributes.getValue(ExperimentXML.INTERFACE_TYPE) != null) interfaceType = attributes.getValue(ExperimentXML.INTERFACE_TYPE);
			if (interfaceType == null) interfaceType = ExperimentXML.ITYPE_1G;
		}

		public PortSpec(HardwareBaseSpec hw)
		{
			hardware = hw;
			fieldSpecs = new Vector();
			monitorSpecs = new Vector();
			commandSpecs = new Vector();
			tableSpecs = new Vector();
			cfgCommands = new Vector();
			assignerSpecs = new Vector();
		}

		public PortSpec(PortSpec ps)
		{
			this(ps.hardware);
			numPorts = ps.numPorts;
			interfaceType = new String(ps.getInterfaceType());
			int max = ps.fieldSpecs.size();
			int i = 0;
			for (i = 0; i < max; ++i)
			{
				fieldSpecs.add(new FieldSpec((FieldSpec)ps.fieldSpecs.elementAt(i)));
			}
			max = ps.monitorSpecs.size();
			for (i = 0; i < max; ++i)
			{
				monitorSpecs.add(new CommandSpec((CommandSpec)ps.monitorSpecs.elementAt(i)));
			}
			max = ps.commandSpecs.size();
			for (i = 0; i < max; ++i)
			{
				commandSpecs.add(new CommandSpec((CommandSpec)ps.commandSpecs.elementAt(i)));
			}
			max = ps.tableSpecs.size();
			for (i = 0; i < max; ++i)
			{
				HWTable.Spec sp = (HWTable.Spec)ps.tableSpecs.elementAt(i);
				if (sp instanceof HWRouteTable.Spec) tableSpecs.add(new HWRouteTable.Spec((HWRouteTable.Spec)sp));
				else
					tableSpecs.add(new HWTable.Spec(sp));
			}
			max = ps.assignerSpecs.size();
			for (i = 0; i < max; ++i)
			{
				assignerSpecs.add(new ComponentXMLHandler.AssignerSpec((ComponentXMLHandler.AssignerSpec)ps.assignerSpecs.elementAt(i)));
			}
			max = ps.cfgCommands.size();
			for (i = 0; i < max; ++i)
			{
				cfgCommands.add(new CommandSpec((CommandSpec)ps.cfgCommands.elementAt(i)));
			}
		}

		public int getNumPorts() { return numPorts;}
		public void setNumPorts(int n) { numPorts = n;}
		public String getInterfaceType() { return interfaceType;}
		public void setInterfaceType(String it) {interfaceType = it;}
		//interface ComponentXMLHandler.Spec
		public CommandSpec addCommand(CommandSpec cspec)
		{
			if (cspec.isMonitor()) monitorSpecs.add(cspec);
			if (cspec.isCommand()) commandSpecs.add(cspec);
			if (cspec.isCFGCommand()) cfgCommands.add(cspec);
			return cspec;
		}
		public void addAssigner(ComponentXMLHandler.AssignerSpec aspec) { assignerSpecs.add(aspec);}
		public void addTable(HWTable.Spec htspec) { tableSpecs.add(htspec);}
		public void addField(FieldSpec fspec) { fieldSpecs.add(fspec);}
		public String getEndToken() { return (ExperimentXML.PORTS);}
		public boolean isPort() { return true;}
		public Vector getMonitorSpecs() { return monitorSpecs;}
		public Vector getCommandSpecs() { return commandSpecs;}
		public Vector getFieldSpecs() { return fieldSpecs;}
		public Vector getAssignerSpecs() { return assignerSpecs;}
		public Vector getTableSpecs() { return tableSpecs;}
		//end interface ComponentXMLHandler.Spec
		public Vector getCfgCommands() { return cfgCommands;}
		public ContentHandler getXMLHandler(XMLReader xmlrdr, ContentHandler last_h) { return (new ComponentXMLHandler(xmlrdr, this, last_h));}
		public CommandSpec getSpec(int op_code, boolean is_mon)
		{
			int max = commandSpecs.size();
			int i = 0;
			CommandSpec elem;
			if (!is_mon)
			{
				for (i = 0; i < max; ++i)
				{
					elem = (CommandSpec)commandSpecs.elementAt(i);
					if (elem.getOpcode() == op_code) 
					{
						ExpCoordinator.print(new String("HardwareBaseSpec.PortSpec.getSpec op:" + op_code + " is_mon:" + is_mon), 6);
						return elem;
					}
				}
			}
			max = monitorSpecs.size();
			for (i = 0; i < max; ++i)
			{
				elem = (CommandSpec)monitorSpecs.elementAt(i);
				if (elem.getOpcode() == op_code) 
				{
					ExpCoordinator.print(new String("HardwareBaseSpec.PortSpec.getSpec op:" + op_code + " is_mon:" + is_mon), 6);
					return elem;
				}
			}
			return null;
		}
		protected void setHardware(HardwareBaseSpec hw) { hardware = hw;}
	}

	////////////////////////////////////////////////////// HardwareBaseSpec Constructor ////////////////////////////////////////////////////////

	public HardwareBaseSpec(Attributes attributes)
	{
		this();  
		if (attributes.getValue(ExperimentXML.TYPENAME) != null) typeName = new String(attributes.getValue(ExperimentXML.TYPENAME));
		ExpCoordinator.print(new String("HardwareBaseSpec(" + typeName + ") constructor"), TEST_TYPENAME);
		if (attributes.getValue(ExperimentXML.VERSION) != null ) version = Double.parseDouble(attributes.getValue(ExperimentXML.VERSION));
		//numPorts = Integer.parseInt(attributes.getValue(ExperimentXML.NUMPORTS));
	}
	public HardwareBaseSpec()
	{
		has_daemon = false;
		monitorSpecs = new Vector();
		commandSpecs = new Vector<CommandSpec>();
		tableSpecs = new Vector();
		fieldSpecs = new Vector();
		cfgCommands = new Vector<CommandSpec>();
		assignerSpecs = new Vector();
	}

	public HardwareBaseSpec(HardwareBaseSpec hs)
	{
		this();
		initFromHardware(hs);
	}

	protected PortSpec createPortSpec(String uri, Attributes attributes) { return (new PortSpec(uri, attributes, this));}
	protected PortSpec createPortSpec(PortSpec ps) { return (new PortSpec(ps));}
	protected void initFromHardware(HardwareBaseSpec hs)
	{
		has_daemon = hs.has_daemon;
		if (hs.rebootSpec != null) rebootSpec = new CommandSpec(hs.rebootSpec);
		if (hs.initSpec != null) initSpec = new CommandSpec(hs.initSpec);
		portSpec = createPortSpec(hs.portSpec);//new PortSpec(hs.portSpec);
		portSpec.setHardware(this);
		typeName = new String(hs.typeName);
		if (hs.resourceName != null) resourceName = new String(hs.resourceName);
		file = hs.file;
		cpPort = hs.cpPort;
		version = hs.version;
		clusterOnly = hs.clusterOnly;
		componentType = new String(hs.componentType);
		int max = hs.fieldSpecs.size();
		int i = 0;
		for (i = 0; i < max; ++i)
		{
			fieldSpecs.add(new FieldSpec((FieldSpec)hs.fieldSpecs.elementAt(i)));
		}
		max = hs.assignerSpecs.size();
		for (i = 0; i < max; ++i)
		{
			assignerSpecs.add(new ComponentXMLHandler.AssignerSpec((ComponentXMLHandler.AssignerSpec)hs.assignerSpecs.elementAt(i)));
		}
		max = hs.monitorSpecs.size();
		for (i = 0; i < max; ++i)
		{
			monitorSpecs.add(new CommandSpec((CommandSpec)hs.monitorSpecs.elementAt(i)));
		}
		max = hs.commandSpecs.size();
		for (i = 0; i < max; ++i)
		{
			commandSpecs.add(new CommandSpec((CommandSpec)hs.commandSpecs.elementAt(i)));
		}
		max = hs.tableSpecs.size();
		for (i = 0; i < max; ++i)
		{
			tableSpecs.add(new HWTable.Spec((HWTable.Spec)hs.tableSpecs.elementAt(i)));
		}
		max = hs.cfgCommands.size();
		for (i = 0; i < max; ++i)
		{
			cfgCommands.add(new CommandSpec((CommandSpec)hs.cfgCommands.elementAt(i)));
		}
	}


	public void print() { print(0);}
	public void print(int d)
	{
		ExpCoordinator.print(new String("HardwareBaseSpec:" + typeName + " version:" + version + " cluster only:" + clusterOnly + " daemon:" + has_daemon + " #ports:" + getNumPorts() + " componentType:" + componentType), d);
		if (rebootSpec != null)
		{
			ExpCoordinator.print("  reboot spec:", d);
			rebootSpec.print(d);
		}
		if (initSpec != null)
		{
			ExpCoordinator.print("  init spec:", d);
			initSpec.print(d);
		}
		ExpCoordinator.print("  commands:", d);
		int max = commandSpecs.size();
		int i = 0;
		for (i = 0; i < max; ++i)
			((CommandSpec)commandSpecs.elementAt(i)).print(d);
		ExpCoordinator.print("  monitors:", d);
		max = monitorSpecs.size();
		for (i = 0; i < max; ++i)
			((CommandSpec)monitorSpecs.elementAt(i)).print(d);    
	}

	public CommandSpec  getRebootSpec() { return rebootSpec;}
	public CommandSpec getInitSpec() { return initSpec;}
	public CommandSpec getSubtypeInitSpec() { return stypeInitSpec;}

	public CommandSpec getSpec(int op_code, boolean is_mon)
	{
		int max = commandSpecs.size();
		int i = 0;
		CommandSpec elem;
		if (!is_mon)
		{
			for (i = 0; i < max; ++i)
			{
				elem = (CommandSpec)commandSpecs.elementAt(i);
				if (elem.getOpcode() == op_code) 
				{
					ExpCoordinator.print(new String(typeName + ".HardwareBaseSpec.getSpec op:" + op_code + " is_mon:" + is_mon), 6);
					return elem;
				}
			}
		}
		max = monitorSpecs.size();
		for (i = 0; i < max; ++i)
		{
			elem = (CommandSpec)monitorSpecs.elementAt(i);
			if (elem.getOpcode() == op_code) 
			{
				ExpCoordinator.print(new String(typeName + ".HardwareBaseSpec.getSpec op:" + op_code + " is_mon:" + is_mon), 6);
				return elem;
			}
		}
		return null;
	}

	private Vector getSpecsVector(int tp)
	{
		Vector specs = null;
		if ((tp & CommandSpec.MONITOR) > 0)
			specs = monitorSpecs;
		else 
		{
			if ((tp & CommandSpec.COMMAND) > 0) specs = commandSpecs;
		}
		return specs;
	}

	public boolean equals(HardwareBaseSpec hw)
	{
		//ExpCoordinator.print(new String("HardwareBaseSpec(" + getLabel() + ").equals " + hw.getLabel()), HardwareSpec.TEST_SUBTYPEMENU);
		return (hw.getLabel().equals(getLabel()) && hw.version == version && hw.componentType.equals(componentType));
	}

	public int getNumPorts() { return (portSpec.getNumPorts());}
	public boolean hasDaemon() { return has_daemon;}
	public String getLabel() { return typeName;}
	public String getTypeLabel() { return typeName;}
	public String getDisplayLabel() { return typeName;}
	public double getVersion() { return version;}
	public int getCPPort() { return cpPort;}
	public boolean isClusterOnly() { return clusterOnly;}
	public void setClusterOnly(boolean b) { clusterOnly = b;}
	public void setComponentType(String ctype) { componentType = new String(ctype);}
	public String getComponentType() { return componentType;}
	public boolean isAny() { return (componentType.equals(ANY));}
	public boolean isRouter() { return (componentType.equals(ROUTER));}
	public boolean isSwitch() { return (componentType.equals(SWITCH));}
	//public boolean isAddressed() { return (componentType.equals(ADDRESSED) || isRouter());}
	public boolean isHost() { return (componentType.equals(HOST));}
	public boolean isEndpoint() { return (componentType.equals(HOST) || componentType.equals(ENDPOINT));}
	protected void setPortSpec(PortSpec ps) { portSpec = ps;}

	//interface ComponentXMLHandler.Spec
	public CommandSpec addCommand(CommandSpec cspec)
	{
		CommandSpec rtn = cspec;
		ExpCoordinator.print(new String("HardwareBaseSpec(" + getLabel() + ").addCommand"), ComponentXMLHandler.TEST_XML);
		cspec.print(ComponentXMLHandler.TEST_XML);
		if (cspec.isMonitor()) monitorSpecs.add(cspec);
		if (cspec.isCommand()) commandSpecs.add(cspec);
		if (cspec.isReboot()) rebootSpec = cspec;
		if (cspec.isInit()) 
		{
			initSpec = cspec;//new CommandSpec(hwcspec, cspec.getParams());
			ExpCoordinator.print(new String("HardwareBaseSpec(" + getLabel() + ".addCommand init command"), ComponentXMLHandler.TEST_XML);
			initSpec.print(ComponentXMLHandler.TEST_XML);
		}
		if (cspec.isSubtypeInit())
		{
			stypeInitSpec = cspec;//new CommandSpec(hwcspec, cspec.getParams());
			ExpCoordinator.print(new String("HardwareBaseSpec(" + getLabel() + ".addCommand subtype init command"), ComponentXMLHandler.TEST_XML);
			stypeInitSpec.print(ComponentXMLHandler.TEST_XML);
		}
		if (cspec.isCFGCommand()) 
		{
			CommandSpec hwcspec = getSpec(cspec.getOpcode(), false);
			if (hwcspec != null)
			{
				rtn = new CommandSpec(hwcspec, cspec.getParams());
				cfgCommands.add(rtn);
			}
		}
		return rtn;  
	}
	public void addAssigner(ComponentXMLHandler.AssignerSpec aspec) 
	{
		ExpCoordinator.print(new String("HardwareBaseSpec(" + typeName + ").addAssigner " + aspec.getName()), ComponentXMLHandler.TEST_XML);
		assignerSpecs.add(aspec);
	}
	public void addTable(HWTable.Spec htspec) { tableSpecs.add(htspec);}
	public void addField(FieldSpec fspec) { fieldSpecs.add(fspec);}
	public String getEndToken() { return (ExperimentXML.HARDWARE);}
	public boolean isPort() { return false;}
	public PortSpec getPortSpec() { return portSpec;}
	public PortSpec getVInterfaceSpec() { return vinterfaceSpec;}
	public Vector<CommandSpec> getCommandSpecs() { return commandSpecs;}
	public Vector getMonitorSpecs() { return monitorSpecs;}
	public Vector getTableSpecs() { return tableSpecs;}
	public Vector getFieldSpecs() { return fieldSpecs;}
	public Vector getAssignerSpecs() { return assignerSpecs;}
	//end interface ComponentXMLHandler.Spec
	public Vector<CommandSpec> getCfgCommands() { return cfgCommands;}
	public void setFile(java.io.File f) { file = f;}
	public java.io.File getFile() { return file;}
	public void setResourceName(String nm) { resourceName = new String(nm);}
	public String getResourceName() { return resourceName;}
	public String getDescription() { return (new String(getLabel() + ": " + description));}
	public void setIconColor(int r, int g, int b) { iconColor = new Color(r,g,b);}
	public Color getIconColor() { return iconColor;}
}
