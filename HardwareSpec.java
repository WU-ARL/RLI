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
import java.beans.*;

//NOTE: fix Instance writeParams and readParams so the right thing happens for NSP
public class HardwareSpec extends HardwareBaseSpec implements ComponentXMLHandler.Spec
{
	public static final int TEST_ADD = 5;
	public static final int TEST_SUBTYPEMENU = 5;
	private ExpCoordinator expCoordinator = null;
	private Subtype defaultSubtype = null;
	private Subtypes subtypes = null;
	private boolean verified = false;



	protected static Manager manager = null; 

	//////////////////////////////////////////////  HardwareSpec.HWFileFilter ///////////////////////////////////////////
	public static class HWFileFilter implements FilenameFilter
	{
		public static String FILE_SUFFIX = ".hw";
		public HWFileFilter() {}
		public boolean accept(File dir, String name) 
		{ 
			return (name.endsWith(FILE_SUFFIX) || name.equals(new String(FILE_SUFFIX + ".xml")));
		}
	}// end class HardwareSpec.Subtype.SHWFileFilter

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////// HardwareSpec.Manager ////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class Manager extends DefaultListModel
	{
		///////////////////////////////////////////////// HardwareSpec.Manager.XMLHandler /////////////////////////////////////////
		private class XMLHandler extends DefaultHandler
		{
			private ExperimentXML expXML = null;
			private String currentElement = "";
			private String section = "";
			private String tp_nm = null;
			public XMLHandler(ExperimentXML exp_xml)
			{
				super();
				expXML = exp_xml;
			}

			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{
				currentElement = new String(localName);
				ExpCoordinator.print(new String("HardwareSpec.Manager.XMLHandler.startElement " + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.HWTYPE) || localName.equals(ExperimentXML.SUBHWTYPE) )
				{
					section = new String(localName);
					tp_nm = new String(attributes.getValue(ExperimentXML.TYPENAME));
				}
			}

			public void characters(char[] ch, int start, int length)
			{
				if (section.equals(ExperimentXML.HWTYPE))
				{
					if (currentElement.equals(ExperimentXML.FILE) && tp_nm != null) addFromFile(tp_nm, new String(ch, start, length));
					if (currentElement.equals(ExperimentXML.RESOURCE) && tp_nm != null) addFromResource(tp_nm, new String(ch, start, length));
				}
				if (section.equals(ExperimentXML.SUBHWTYPE))
				{
					if (getSubtype(tp_nm) == null)
					{
						if (currentElement.equals(ExperimentXML.FILE) && tp_nm != null) addSubtype(new java.io.File(new String(ch, start, length)));
						if (currentElement.equals(ExperimentXML.RESOURCE) && tp_nm != null) addSubtypeFromResource(new String(ch, start, length));
					}
				}
			}
			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("HardwareSpec.Manager.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.HWTYPE) || localName.equals(ExperimentXML.SUBHWTYPE) )
				{
					tp_nm = null;
					section = "";
				}
				if (localName.equals(ExperimentXML.HARDWARE)) 
					expXML.removeContentHandler(this);
			}
		}

		private Manager()
		{
			super();
		}

		public HardwareSpec getHardware(String tp_nm)
		{
			int max = getSize();
			HardwareSpec elem;
			ExpCoordinator.print(new String("HardwareSpec.Manager.getHardware " + tp_nm + " max = " + max), 6);
			for (int i = 0; i < max; ++i)
			{
				elem = (HardwareSpec)getElementAt(i);
				ExpCoordinator.print(new String("     hardware[" + i + "] = " + elem.getTypeLabel()), 6);
				if (elem.getTypeLabel().equals(tp_nm)) return elem;
			}
			return null;
		}
		public HardwareSpec.Subtype getSubtype(String tp_nm)
		{
			int max = getSize();
			HardwareSpec elem;
			HardwareSpec.Subtype subtype;
			ExpCoordinator.print(new String("HardwareSpec.Manager.getSubtype " + tp_nm + " max = " + max), 6);
			for (int i = 0; i < max; ++i)
			{
				elem = (HardwareSpec)getElementAt(i);
				ExpCoordinator.print(new String("     hardware[" + i + "] = " + elem.getTypeLabel()), 6);
				if (elem.getTypeLabel().equals(tp_nm)) return (elem.getDefaultSubtype());
				else 
				{
					subtype = elem.getSubtype(tp_nm);
					if (subtype != null) return subtype;
				}
			}
			return null;
		}
		/*
    public HardwareSpec getHardware(String tp_nm, boolean is_rtr)
    {
      return (getHardware(tp_nm));
      }*/

		public void add(Object elem)
		{
			ExpCoordinator.print("HardwareSpec.Manager.add", 3);
			if ((elem instanceof HardwareSpec) && 
					(getHardware(((HardwareSpec)elem).getLabel()) == null))
				//(!contains(elem))) 
			{
				addElement(elem);
				ExpCoordinator.print(new String("   adding hardware " + ((HardwareSpec)elem).getLabel()), 3);
				((HardwareSpec)elem).print(3);
			}
			if (elem instanceof HardwareSpec.Subtype)
			{
				((HardwareSpec.Subtype)elem).getHardwareType().addSubtype((HardwareSpec.Subtype)elem);
				ExpCoordinator.print(new String("   adding hardware " + ((HardwareSpec.Subtype)elem).getLabel()), 3);
				((HardwareSpec.Subtype)elem).print(3);
			}
		}
		public HardwareSpec addFromFile(File f) { return (addFromFile(null, f));}
		public HardwareSpec addFromFile(String tp_nm, File f)
		{
			HardwareSpec rtn = null;
			if (tp_nm != null) getHardware(tp_nm);
			if (rtn == null)
			{
				try
				{
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					HardwareSpec.XMLHandler xmlh = new HardwareSpec.XMLHandler(xmlReader, f);
					xmlReader.setContentHandler(xmlh);
					xmlReader.parse(new InputSource(new FileInputStream(f)));
					if (tp_nm != null) rtn = getHardware(tp_nm);
				}
				catch(java.lang.Exception e1)
				{
					ExpCoordinator.print(new String("HardwareSpec.manager.addFromFile Error reading from file:" + e1.getMessage()));
				}
			}
			return rtn;
		}
		public HardwareSpec addFromFile(String tp_nm, String fnm) { return (addFromFile(tp_nm, new File(fnm)));}
		public HardwareSpec addFromResource(String tp_nm, String rnm)
		{
			HardwareSpec rtn = getHardware(tp_nm);
			if (rtn == null)
			{
				try
				{
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					HardwareSpec.XMLHandler xmlh = new HardwareSpec.XMLHandler(xmlReader, rnm);
					xmlReader.setContentHandler(xmlh);
					InputStream in_str = this.getClass().getResourceAsStream(rnm);
					xmlReader.parse(new InputSource(in_str));
					rtn = getHardware(tp_nm);
				}
				catch(java.lang.Exception e1)
				{
					ExpCoordinator.print(new String("HardwareSpec.manager.addFromFile Error reading from file:" + e1.getMessage()));
				}
			}
			return rtn;
		}
		public void addSubtype(java.io.File f)
		{   
			try
			{
				ExpCoordinator.print(new String("HardwareSpec.manager.addSubtype from file(" + f.getPath() + ")"), TEST_ADD);
				XMLReader xmlReader = XMLReaderFactory.createXMLReader();
				Subtype.SHWXMLHandler xmlh = new Subtype.SHWXMLHandler(xmlReader, f);
				xmlReader.setContentHandler(xmlh);
				xmlReader.parse(new InputSource(new FileInputStream(f)));
			}
			catch(java.lang.Exception e1)
			{
				ExpCoordinator.print(new String("HardwareSpec.manager.addSubtype Error reading from file(" + f.getPath() + ":" + e1.getMessage()));
				e1.printStackTrace();
			}
		}
		public void addSubtypeFromResource(String rnm)
		{   
			try
			{
				ExpCoordinator.print(new String("HardwareSpec.manager.addSubtype from resource(" + rnm + ")"), TEST_ADD);
				XMLReader xmlReader = XMLReaderFactory.createXMLReader();
				Subtype.SHWXMLHandler xmlh = new Subtype.SHWXMLHandler(xmlReader, rnm);
				xmlReader.setContentHandler(xmlh);
				InputStream in_str = this.getClass().getResourceAsStream(rnm);
				xmlReader.parse(new InputSource(in_str));
			}
			catch(java.lang.Exception e1)
			{
				ExpCoordinator.print(new String("HardwareSpec.manager.addSubtype Error reading from resource(" + rnm + "):" + e1.getMessage()));
				e1.printStackTrace();
			}
		}
		public void remove(Object elem)
		{
			if (contains(elem)) removeElement(elem);
		}
		public int size() { return (getSize());}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(ExperimentXML.HARDWARE);
			int max = getSize();
			int i;
			int max2;
			int j;
			HardwareSpec hw = null;
			for (i = 0; i < max; ++i)
			{
				hw = (HardwareSpec)getElementAt(i);
				hw.writeXML(xmlWrtr);
				max2 = hw.subtypes.size();
				HardwareSpec.Subtype sub;
				for (j = 0; j < max2; ++j)
				{
					sub = (HardwareSpec.Subtype)hw.subtypes.getElementAt(j);
					sub.writeXML(xmlWrtr);
				}
			}
			xmlWrtr.writeEndElement();
		}

		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new XMLHandler(exp_xml));}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////// HardwareSpec.XMLHandler ////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class XMLHandler extends HardwareBaseSpec.XMLHandler //ComponentXMLHandler //DefaultHandler
	{
		//private HardwareSpec hardware = null;
		private java.io.File file = null;
		private String resourceName = null;

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
			currentElement = new String(localName);
			if (localName.equals(ExperimentXML.HARDWARE)) 
			{
				section = ExperimentXML.HARDWARE;
				ExpCoordinator.print(new String("HardwareSpec.XMLHandler.startElement name: " + attributes.getValue(ExperimentXML.TYPENAME) + " version:" + attributes.getValue(ExperimentXML.VERSION)), ExperimentXML.TEST_XML);
				//if (resourceName != null && resourceName.equals("NPR.hw")) hardware = new NPRouter(attributes);
				//else 
				hardware = new HardwareSpec(attributes);
				if (file != null) hardware.setFile(file);
				if (resourceName != null) hardware.setResourceName(resourceName);
				setSpec(hardware);
			}
		}
		public void endElement(String uri, String localName, String qName)
		{
			if (localName.equals(ExperimentXML.HARDWARE)) 
			{
				((HardwareSpec)hardware).initializeSubtypes();
				manager.add(hardware);
			}
			super.endElement(uri, localName, qName);
		}
	}// end inner class HardwareSpec.XMLHandler

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////// HardwareSpec.Subtype ///////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected static class Subtype extends HardwareBaseSpec
	{
		public static final String FILE_SUFFIX = ".shw";
		//private String label = "";
		//private boolean is_router = false;
		//private String componentType = NOT_ADDRESSED;
		private HardwareSpec hardwareType = null;
		private VirtualTopology.VNodeSubtype virtualType = null;
		//private Vector cfgParams = null;
		private double stVersion = ExpCoordinator.VERSION;

		/////////////////////////////////////////////// HardwareSpec.Subtype.SHWXMLHandler /////////////////////////////////////

		public static class SHWXMLHandler extends HardwareBaseSpec.XMLHandler //ComponentXMLHandler //DefaultHandler  
		{
			//private Subtype subtype = null;
			private HardwareSpec hwType = null;
			private String hwTypeName = null;
			//private XMLReader xmlReader = null;
			//private String currentElement = "";
			private Vector commands = null;
			//private double version = ExpCoordinator.VERSION;
			//private File file = null;
			//private String resourceName = null;

			public SHWXMLHandler(XMLReader xmlr, java.io.File f)
			{
				super(xmlr, f);
				commands = new Vector();
				//this(xmlr);
				//file = f;
			}

			public SHWXMLHandler(XMLReader xmlr, String rnm)
			{
				super(xmlr, rnm);
				commands = new Vector();
				//this(xmlr);
				//resourceName = rnm;
			}

			public SHWXMLHandler(XMLReader xmlr)
			{
				super(xmlr);
				//super();
				//xmlReader = xmlr;
				commands = new Vector();
				//xmlReader.setContentHandler(this);
			}

			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{
				if (localName.equals(ExperimentXML.PORTS) && hardware != null)
				{
					if (attributes.getValue(ExperimentXML.VPORTS) != null) 
					{
						hardware.getPortSpec().setNumPorts(Integer.parseInt(attributes.getValue(ExperimentXML.VPORTS)));	
						if (attributes.getValue(ExperimentXML.INTERFACE_TYPE) != null) hardware.getPortSpec().setInterfaceType(attributes.getValue(ExperimentXML.INTERFACE_TYPE));
					}
				}
				if(!localName.equals(ExperimentXML.COMMAND) ||
						!section.equals(ExperimentXML.CONFIGURE))
					super.startElement(uri, localName, qName, attributes);
				ExpCoordinator.print(new String("HardwareSpec.Subtype.SHWXMLHandler.startElement " + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.SUBTYPE))
				{
					section = ExperimentXML.SUBTYPE;
					hardware = new Subtype(attributes);
					if (resourceName != null) hardware.setResourceName(resourceName);
					if (file != null) hardware.setFile(file);
					setSpec(hardware);
				}
				if (localName.equals(ExperimentXML.VIRTUAL))
				{
					((Subtype)hardware).virtualType = new VirtualTopology.VNodeSubtype(attributes);
					xmlReader.setContentHandler(((Subtype)hardware).virtualType.getXMLHandler(xmlReader, this));
				}
				if (localName.equals(ExperimentXML.HWTYPE)) 
				{
					ExpCoordinator.print(new String("HardwareSpec.Subtype.SHWXMLHandler.startElement name: " + attributes.getValue(ExperimentXML.TYPENAME)), ExperimentXML.TEST_XML);
					hwTypeName = new String(attributes.getValue(ExperimentXML.TYPENAME));
					hwType = manager.getHardware(hwTypeName);
					if (hwType != null) hardware.initFromHardware(hwType);
				}
				if (localName.equals(ExperimentXML.COMMAND) && section.equals(ExperimentXML.CONFIGURE))
				{
					commandType = CommandSpec.CFG_COMMAND;
					if (spec.isPort()) commandType = commandType | CommandSpec.PERPORT;
					CommandSpec cs = createCommandSpec(uri, localName, attributes, commandType);//new CommandSpec(uri, attributes);
					//cs.setSubtype(true);
					commands.add(cs);
					xmlReader.setContentHandler(cs.getXMLHandler(xmlReader, this));
				}
			}
			public void characters(char[] ch, int start, int length)
			{
				super.characters(ch, start, length);
				ExpCoordinator.print(new String("HardwareSpec.Subtype.SHWXMLHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
				if (currentElement.equals(ExperimentXML.FILE) && hwTypeName != null && hwType == null)
				{
					hwType = manager.addFromFile(hwTypeName, new String(ch, start, length));
					if (hwType != null) hardware.initFromHardware(hwType);
				}
				if (currentElement.equals(ExperimentXML.RESOURCE) && hwTypeName != null && hwType == null)
				{
					String tmp_rnm = new String(ch, start, length);
					ExpCoordinator.print(new String("HardwareSpec.Subtype.SHWXMLHandler.characters resource hardware:" + tmp_rnm), ExperimentXML.TEST_XML);
					hwType = manager.addFromResource(hwTypeName, tmp_rnm);
					if (hwType != null) hardware.initFromHardware(hwType);
					else
						ExpCoordinator.print(new String("     hardware " + tmp_rnm + " not found"), ExperimentXML.TEST_XML);
				}
			}
			public void endElement(String uri, String localName, String qName)
			{
				super.endElement(uri, localName, qName);
				if (localName.equals(ExperimentXML.HARDWARE)) 
				{
					if (hwType != null) hardware.initFromHardware(hwType);
				}
				if (localName.equals(ExperimentXML.CONFIGURE)) 
				{
					CommandSpec cmd;
					int max = commands.size();
					int i = 0;
					ExpCoordinator.print(new String(hardware.getLabel() + ":Subtype.SHWXMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
					for (i = 0; i < max; ++i)
					{
						cmd = (CommandSpec)commands.elementAt(i);
						ExpCoordinator.print(new String("    command " + i + ":"), ExperimentXML.TEST_XML);
						cmd.print(ExperimentXML.TEST_XML);
						hardware.addCommand(cmd);
					}
				}

				if (localName.equals(ExperimentXML.SUBTYPE)) 
				{
					if (hwType != null) 
					{
						ExpCoordinator.print(new String(hardware.getLabel() + ":Subtype.SHWXMLHandler.endElement " + localName + " adding subtype to hwType:" + hwType.getLabel()), ExperimentXML.TEST_XML);
						((HardwareSpec)hwType).addSubtype((HardwareSpec.Subtype)hardware);
					}
				}
			}
			public boolean isSection(String nm)
			{
				if (nm.equals(ExperimentXML.CONFIGURE)) return true;
				else return (super.isSection(nm));
			}
		}// end inner class HardwareSpec.Subtype.SHWXMLHandler

		//////////////////////////////////////////////  HardwareSpec.Subtype.SHWFileFilter ///////////////////////////////////////////
		public static class SHWFileFilter implements FilenameFilter
		{
			public SHWFileFilter() {}
			public boolean accept(File dir, String name) 
			{ 
				return (name.endsWith(FILE_SUFFIX) || name.equals(new String(FILE_SUFFIX + ".xml")));
			}
		}// end class HardwareSpec.Subtype.SHWFileFilter

		//////////////////////////////////////////////  HardwareSpec.Subtype.MenuItem ////////////////////////////////////////////////
		public static class MenuItem extends JMenuItem implements Typeable
		{
			private HardwareSpec.Subtype type;
			private class HWIAction extends Topology.TopologyAction
			{
				private ExpCoordinator expCoordinator = null;
				public HWIAction(HardwareSpec.Subtype tp, String nm)
				{
					super(new String(nm));
					expCoordinator = ExpCoordinator.theCoordinator;
				}
				public void actionPerformed(ActionEvent e)
				{
					final String opt0 = "OK";
					final String opt1 = "Cancel";
					Object[] options = {opt0,opt1};
					TextFieldwLabel num = new TextFieldwLabel(new JTextField("1"), "How many instances?");
					Object[] objarray = {num};
					int rtn = JOptionPane.showOptionDialog(ExpCoordinator.getMainWindow(), 
							objarray, 
							(String)getValue(Action.NAME), 
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, 
							null,
							options,
							options[0]);

					if (rtn == JOptionPane.YES_OPTION)
					{
						int num_inst = Integer.parseInt(num.getText());
						for (int i = 0; i < num_inst; ++i)
						{
							Hardware hwi = expCoordinator.getTopology().getNewHW(type, null);
							if (hwi != null)
							{
								hwi.initializePorts();
								expCoordinator.getCurrentExp().addNode(hwi);
								//hwi.getGraphic().setLocation(10,95);
							}
						}
					}
				}
			}
			public MenuItem(HardwareSpec.Subtype tp)
			{
				this(tp, new String("Add " + tp.getTypeLabel()));
			}
			public MenuItem(HardwareSpec.Subtype tp, String nm)
			{
				super(new String(nm));
				type = tp;
				setAction(new HWIAction(tp, nm));
			}
			public boolean isType(HardwareSpec tp)
			{
				return (type.hardwareType.equals(tp));
			}
			public HardwareSpec.Subtype getType() { return type;}
			public boolean isSubtype(HardwareSpec.Subtype shw) 
			{
				return (type.equals(shw));
			}
		}//end HardwareSpec.Subtype.MenuItem

		/////////////////////////////////////////////// HardwareSpec.Subtype.SPortSpec /////////////////////////////////////
		public static class SPortSpec extends HardwareBaseSpec.PortSpec
		{
			public SPortSpec(String uri, Attributes attributes, HardwareBaseSpec hw) { super(uri, attributes, hw);}
			public SPortSpec(HardwareBaseSpec hw) { super(hw);}
			public SPortSpec(PortSpec ps) { super(ps);}
			public CommandSpec addCommand(CommandSpec cmd)
			{
				CommandSpec hwcmd = getSpec(cmd.getOpcode(), false);
				if (hwcmd == null  || cmd.isCFGCommand())
					return (super.addCommand(cmd));
				return hwcmd;
			}
		}//end HardwareSpec.Subtype.SPortSpec

		//////////////////// Subtype methods //////////////
		/* public Subtype(java.io.File f) throws IOException
    {
      file = f;
      cfgParams = new Vector();
      commandSpecs = new Vector();
      monitorSpecs = new Vector();
      ONL.BaseFileReader rdr = new ONL.BaseFileReader(f);
      rdr.setVersion(3.7);
      read(rdr);
      ExpCoordinator.print(new String("HardwareSpec.Subtype constructor hw:" + getLabel()), HardwareBaseSpec.TEST_SUBTYPE);
      print(HardwareBaseSpec.TEST_SUBTYPE);
      }*/

		public Subtype(Attributes attributes)
		{
			super(attributes);
			//cfgParams = new Vector();
			//label = new String(attributes.getValue(ExperimentXML.TYPENAME));
			if (attributes.getValue(ExperimentXML.VERSION) != null) stVersion = Double.parseDouble(new String(attributes.getValue(ExperimentXML.VERSION)));
		}

		public Subtype(HardwareSpec hw)
		{
			super((HardwareBaseSpec)hw);
			hardwareType = hw;
			//label = getTypeLabel();
			//ExpCoordinator.print(new String("HardwareSpec.Subtype constructor hw:" + hw.getTypeLabel() + " label:" + label), HardwareBaseSpec.TEST_SUBTYPE);
			//cfgParams = new Vector();
		}
		public Subtype(HardwareSpec hw,  String ctype)
		{
			this(hw);
			setComponentType(ctype);
			if (isRouter() && hw.isAny()) 
			{
				typeName = typeName.concat("Router");
				ExpCoordinator.print(new String("HardwareSpec.Subtype constructor type EITHER hw:" + hw.getTypeLabel() + " ctyper:"+ ctype + " label:" + typeName), HardwareBaseSpec.TEST_SUBTYPE);
			}
			else
				ExpCoordinator.print(new String("HardwareSpec.Subtype constructor hw:" + hw.getTypeLabel() + " componentType:" + hw.getComponentType() + " ctype:" + ctype + " label:" + typeName), HardwareBaseSpec.TEST_SUBTYPE);
		}
		//interface ComponentXMLHandler.Spec
		public CommandSpec addCommand(CommandSpec cmd)
		{
			CommandSpec rtn = null;
			if (hardwareType == null) 
				ExpCoordinator.print(new String("HardwareSpec.Subtype(" + getTypeLabel() + ").addCommandSpec hardwareType is null"), HardwareBaseSpec.TEST_SUBTYPE);
			CommandSpec hwcmd = getSpec(cmd.getOpcode(), false);
			if (cmd.isReboot() && getRebootSpec() != null) 
			{
				getRebootSpec().setDefaults(cmd.getParams().toArray());
				return(getRebootSpec());

			}
			if (cmd.isInit() && getInitSpec() != null)
			{
				getInitSpec().setDefaults(cmd.getParams().toArray());
				ExpCoordinator.print(new String("HardwareBaseSpec(" + getTypeLabel() + ".addCommand init command"), ComponentXMLHandler.TEST_XML);
				getInitSpec().print(ComponentXMLHandler.TEST_XML);
				return(getInitSpec());
			}
			if (hwcmd == null || cmd.isCFGCommand())
				return(super.addCommand(cmd));
			else return hwcmd;
			/*
	{
	if (hwcmd == null && cmd.getOpcode() >= HARDWARE_OPCODE)
	{
	ExpCoordinator.print(new String(label + ":Subtype.addCommandSpec adding new command not supported by hardware type"), HardwareBaseSpec.TEST_SUBTYPE);
	cmd.print(HardwareBaseSpec.TEST_SUBTYPE);
	if (cmd.isCommand())
	commandSpecs.add(cmd);
	if (cmd.isMonitor())
	monitorSpecs.add(cmd);
	}
	else  return false;
	}
	else
	{
	if (hwcmd != null)
	{
	new_cmd = new CommandSpec(hwcmd, cmd.getParams());
	if (cmd.isCFGCommand()) cfgParams.add(new_cmd);
	if (cmd.isReboot()) rebootParams = new_cmd;
	if (cmd.isInit()) initParams = new_cmd;
	}
	else
	{
	return false;
	}
	}
	return true;
			 */
		}
		public String getEndToken() { return (ExperimentXML.SUBTYPE);}
		public boolean isPort() { return false;}
		//end interface ComponentXMLHandler.Spec
		//public String getTypeLabel() { return label;}
		public String getBaseTypeLabel() { return (hardwareType.getTypeLabel());}
		//public String getLabel() { return label;}
		public HardwareSpec getHardwareType() { return hardwareType;}
		public void setHardwareType(HardwareSpec hs) { hardwareType = hs;}

		public boolean equals(HardwareSpec.Subtype hw)
		{
			return (super.equals((HardwareBaseSpec)hw) && hw.hardwareType == hardwareType);
		}

		protected PortSpec createPortSpec(String uri, Attributes attributes) { return (new SPortSpec(uri, attributes, this));}
		protected PortSpec createPortSpec(PortSpec ps) { return (new SPortSpec(ps));}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(ExperimentXML.SUBHWTYPE);
			xmlWrtr.writeAttribute(ExperimentXML.TYPENAME, getTypeLabel());//"typeName", typeName);
			xmlWrtr.writeAttribute(ExperimentXML.ROUTER, componentType);//"router", String.valueOf(is_router));
			hardwareType.writeXML(xmlWrtr);
			if (file != null)
			{
				xmlWrtr.writeStartElement(ExperimentXML.FILE);//"file");
				xmlWrtr.writeCharacters(file.getAbsolutePath());
				xmlWrtr.writeEndElement(); //file
			}
			else
			{
				if (resourceName != null)
				{
					xmlWrtr.writeStartElement(ExperimentXML.RESOURCE);//"resource");
					xmlWrtr.writeCharacters(resourceName);
					xmlWrtr.writeEndElement(); //resource
				}
			}
			xmlWrtr.writeEndElement(); //subtype
		}
		public JMenuItem getMenuItem() { return (new Subtype.MenuItem(this));}
		public Object[] getRebootDefaults()
		{
			if (rebootSpec != null) return (rebootSpec.getDefaultValues());
			else return null;
		}
		public CommandSpec getInitSpec() { return initSpec;}
		public Object[] getInitDefaults()
		{
			if (initSpec != null) return (initSpec.getDefaultValues());
			else return null;
		}
		//public boolean isAddressed() { return (!componentType.equals(NOT_ADDRESSED));}
		//public String getDisplayLabel() { return ;}
		protected void initFromHardware(HardwareBaseSpec hs)
		{
			String otp_nm = typeName;
			super.initFromHardware(hs);
			hardwareType = (HardwareSpec)hs;
			//if (!hardwareType.componentType.equals(ANY)) 
			componentType = new String(hardwareType.componentType);
			//else componentType = NOT_ADDRESSED;
			if (otp_nm != null) typeName = otp_nm;
		}
		
		protected VirtualTopology.VNodeSubtype getVirtualType() { return virtualType;}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////// HardwareSpec.Subtypes ////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class Subtypes extends DefaultListModel
	{
		private Subtypes()
		{
			super();
		}

		public HardwareSpec.Subtype getSubtype(String tp_nm)
		{
			int max = getSize();
			HardwareSpec.Subtype elem;
			ExpCoordinator.print(new String("HardwareSpec.Subtypes.getHardware " + tp_nm), 6);
			for (int i = 0; i < max; ++i)
			{
				elem = (HardwareSpec.Subtype)getElementAt(i);
				if (elem.getTypeLabel().equals(tp_nm)) return elem;
			}
			return null;
		}
		/*
    public HardwareSpec.Subtype getSubtype(String tp_nm, boolean is_rtr)
    {
      int max = getSize();
      HardwareSpec.Subtype elem;
      ExpCoordinator.print(new String("HardwareSpec.Subtypes.getHardware " + tp_nm + " is router " + is_rtr), 6);
      for (int i = 0; i < max; ++i)
        {
          elem = (HardwareSpec.Subtype)getElementAt(i);
          if (elem.label.equals(tp_nm) && elem.isRouter() == is_rtr) return elem;
        }
      return null;
    }
		 */

		public void add(Object elem)
		{
			if ((elem instanceof HardwareSpec.Subtype) && 
					(!contains(elem))) 
				addElement(elem);
		}
		public void remove(Object elem)
		{
			if (contains(elem)) removeElement(elem);
		}
		public int size() { return (getSize());}
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////// ManagerListener ///////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public static class ManagerListener implements ListDataListener
	{
		private JMenu menu = null;
		public ManagerListener(JMenu m) 
		{
			menu = m;
			Manager mngr = getTheManager();
			mngr.addListDataListener(this);
		}

		//interface ListDataListener
		public void contentsChanged(ListDataEvent e) {}

		public void intervalAdded(ListDataEvent e) 
		{
			//System.out.println("TopologyPanel::intervalAdded");
			HardwareSpec.Manager l = (HardwareSpec.Manager)e.getSource();
			int bot = e.getIndex0();
			int top = e.getIndex1();
			JMenuItem elem;
			HardwareSpec hw;
			for (int i = bot; i <= top; ++i)
			{
				hw = (HardwareSpec)l.get(i);
				if (getItem(hw) == null && !hw.isClusterOnly()) 
				{
					int pos = menu.getItemCount() - 2;
					if (ExpCoordinator.isSPPMon()) pos += 1;
					menu.insert(hw.getMenuItem(), pos);//new HardwareSpec.MenuItem(hw), pos);
				}
			}
		}

		public void intervalRemoved(ListDataEvent e) {}

		private JMenuItem getItem(HardwareSpec hw)
		{
			JMenuItem elem;
			int max = menu.getItemCount();
			for (int i = 0; i < max; ++i)
			{
				elem = menu.getItem(i);
				if ((elem instanceof HardwareSpec.MenuItem) && ((HardwareSpec.Typeable)elem).isType(hw)) 
				{
					return elem;
				}
			}
			return null;
		}
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////// HardwareSpec.MenuItem /////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface Typeable
	{
		public boolean isType(HardwareSpec hw);
		public boolean isSubtype(HardwareSpec.Subtype shw);
	}

	public static class MenuItem extends JMenuItem implements Typeable
	{
		private HardwareSpec type;
		private class HWIAction extends Topology.TopologyAction
		{
			private ExpCoordinator expCoordinator = null;
			public HWIAction(HardwareSpec tp, String nm)
			{
				super(new String(nm));
				expCoordinator = tp.expCoordinator;
			}
			public void actionPerformed(ActionEvent e)
			{
				//super.actionPerformed(e);
				Hardware hwi = expCoordinator.getTopology().getNewHW(type, null);
				if (hwi != null)
				{
					hwi.initializePorts();
					expCoordinator.getCurrentExp().addNode(hwi);
					//hwi.getGraphic().setLocation(10,95);
				}
			}
		}
		public MenuItem(HardwareSpec tp)
		{
			this(tp, new String("Add " + tp.getDisplayLabel()));
		}
		public MenuItem(HardwareSpec tp, String nm)
		{
			super(new String(nm));
			type = tp;
			setAction(new HWIAction(tp, nm));
		}
		public boolean isType(HardwareSpec tp)
		{
			return (type.equals(tp));
		}
		public HardwareSpec getType() { return type;}
		public boolean isSubtype(HardwareSpec.Subtype shw) 
		{
			return (type.defaultSubtype.equals(shw));
		}
	}

	public static class SubtypeMenu extends JMenu implements Typeable, ListDataListener, PropertyChangeListener
	{
		private HardwareSpec type;
		private ONL.Log log = null;
		public SubtypeMenu(HardwareSpec tp)
		{
			this(tp, new String("Add " + tp.getDisplayLabel()));
		}
		public SubtypeMenu(HardwareSpec tp, String nm)
		{
			super(new String(nm));
			type = tp;
			type.addSubtypeListener(this);
			log = new ONL.Log(new String(type.getDisplayLabel() + " Subtype Descriptions "), false);
			log.setEditable(false);
			log.addLine(tp.getDescription());
			log.setSize(350, 120);
			Subtypes stypes = type.subtypes;
			int max = stypes.size();
			Subtype elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (Subtype)stypes.getElementAt(i);
				if (getItem(elem) == null) 
				{
					insert(elem.getMenuItem(), i);
					log.addLine(elem.getDescription());
				}
			}
			ExpCoordinator ec = ExpCoordinator.theCoordinator;
			ec.addPropertyListener(ExpCoordinator.ADVANCED, this);
			if (ec.isAdvanced() || max <= 0) insert(type.defaultSubtype.getMenuItem(), max);
			add(new JMenuItem(new AbstractAction("Get Subtype Info"){
				public void actionPerformed(ActionEvent e)
				{
					log.showLog();
				}
			}));
		}
		public boolean isType(HardwareSpec tp)
		{
			return (type.equals(tp));
		}
		public HardwareSpec getType() { return type;}
		public boolean isSubtype(HardwareSpec.Subtype shw) 
		{
			return (type.defaultSubtype.equals(shw));
		}
		//interface ListDataListener
		public void contentsChanged(ListDataEvent e) {}

		public void intervalAdded(ListDataEvent e) 
		{
			//System.out.println("TopologyPanel::intervalAdded");
			HardwareSpec.Subtypes l = (HardwareSpec.Subtypes)e.getSource();
			int bot = e.getIndex0();
			int top = e.getIndex1();
			//ExpCoordinator.print(new String("HardwareSpec.SubtypeMenu intervalAdded (" + bot + "," + top + ")"), TEST_SUBTYPEMENU);
			JMenuItem elem;
			HardwareSpec.Subtype hw;
			for (int i = bot; i <= top; ++i)
			{
				hw = (HardwareSpec.Subtype)l.get(i);
				//ExpCoordinator.print(new String("   elem(" + i + "):" + hw.getLabel()), TEST_SUBTYPEMENU);
				if (getItem(hw) == null) 
				{
					//ExpCoordinator.print(new String("   added " + hw.getLabel()), TEST_SUBTYPEMENU);
					//int pos = getItemCount();// - 1;
					insert(hw.getMenuItem(), 0);//new HardwareSpec.MenuItem(hw), pos);
					log.addLine(hw.getDescription());
				}
			}
			if (getItemCount() > 1 && !ExpCoordinator.theCoordinator.isAdvanced())
			{
				JMenuItem def = getItem(type.defaultSubtype);
				if (def != null) remove(def);
			}
		}

		public void intervalRemoved(ListDataEvent e) {}
		//end interface ListDataListener

		private JMenuItem getItem(HardwareSpec.Subtype hw)
		{
			JMenuItem elem;
			int max = getItemCount();
			for (int i = 0; i < max; ++i)
			{
				elem = getItem(i);
				if ((elem instanceof Subtype.MenuItem) && ((HardwareSpec.Typeable)elem).isSubtype(hw)) 
				{
					return elem;
				}
			}
			return null;
		}

		public void propertyChange(PropertyChangeEvent e)
		{
			if (e.getPropertyName().equals(ExpCoordinator.ADVANCED))
			{
				if (Boolean.valueOf((String)e.getNewValue()).booleanValue())
				{
					if (getItem(type.defaultSubtype) == null) add(type.defaultSubtype.getMenuItem());
				}
				else
				{

					JMenuItem def = getItem(type.defaultSubtype);
					if (def != null && getItemCount() > 1) remove(def);
				}
			}
		}
	}

	////////////////////////////////////////////////////// HardwareSpec Constructor /////////////////////////////////////////////////////////////////////////////////
	protected HardwareSpec()
	{
		super();
		has_daemon = false;
		monitorSpecs = new Vector();
		commandSpecs = new Vector();
		tableSpecs = new Vector();
	}
	public HardwareSpec(Attributes attributes)
	{
		this();
		typeName = new String(attributes.getValue(ExperimentXML.TYPENAME));
		version = Double.parseDouble(attributes.getValue(ExperimentXML.VERSION));
	}

	protected void initializeSubtypes()
	{
		ExpCoordinator.print(new String("HardwareSpec(" + typeName +").initializeSubtypes componentType:" + componentType), HardwareBaseSpec.TEST_SUBTYPE);
		defaultSubtype = new Subtype(this);
		subtypes = new Subtypes();
		//subtypes.add(defaultSubtype);
		//if (isAny()) subtypes.add(new Subtype(this, ROUTER));
		//if (routerType == EITHER) subtypes.add(new Subtype(this, true));
	}
	public void print() { print(0);}
	public void print(int d)
	{
		ExpCoordinator.print(new String("HardwareSpec:" + typeName + " version:" + version + " cluster only:" + clusterOnly + " daemon:" + has_daemon + " #ports:" + getNumPorts() + " componentType:" + componentType), d);
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
					ExpCoordinator.print(new String(typeName + ".HardwareSpec.getSpec op:" + op_code + " is_mon:" + is_mon), 6);
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
				ExpCoordinator.print(new String(typeName + ".HardwareSpec.getSpec op:" + op_code + " is_mon:" + is_mon), 6);
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

	public CommandSpec[] getSpecs(int tp)
	{
		Vector tmp = new Vector();
		Vector specs = getSpecsVector(tp);
		CommandSpec rtn[] = null;
		if (specs == null) return null;
		int max = specs.size();
		CommandSpec elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (CommandSpec)specs.elementAt(i);
			if (elem.getType() == tp) tmp.add(elem);
		}
		if (tmp.size() > 0)
		{
			rtn = new CommandSpec[tmp.size()];
			tmp.toArray(rtn);
		}
		return rtn;
	}

	public boolean equals(HardwareSpec hw)
	{
		return (hw.typeName.equals(typeName) && hw.version == version && hw.componentType.equals(componentType));
	}

	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.HWTYPE);//"hwType");
		xmlWrtr.writeAttribute(ExperimentXML.TYPENAME, typeName);//"typeName", typeName);
		if (file != null)
		{
			xmlWrtr.writeStartElement(ExperimentXML.FILE);//"file");
			xmlWrtr.writeCharacters(file.getAbsolutePath());
			xmlWrtr.writeEndElement(); //file
		}
		else
		{
			if (resourceName != null)
			{
				xmlWrtr.writeStartElement(ExperimentXML.RESOURCE);//"resource");
				xmlWrtr.writeCharacters(resourceName);
				xmlWrtr.writeEndElement(); //resource
			}
		}
		xmlWrtr.writeEndElement(); //hardwareType
	}

	public void writeID(ONL.Writer wrtr) throws IOException
	{
		wrtr.writeString(typeName);
		if (file != null)
			wrtr.writeString(file.getAbsolutePath());
		else
		{
			if (resourceName != null) wrtr.writeString(resourceName);
			else wrtr.writeString("");
		}
	}

	public static HardwareSpec readID(ONL.Reader rdr) throws IOException
	{
		String tp_nm = rdr.readString();
		boolean is_rtr = false;
		if (rdr.getVersion() > 3.3 && rdr.getVersion() < 3.7 ) is_rtr = rdr.readBoolean();
		//else
		//{
		//if (tp_nm.equals("NPR")) is_rtr = true;
		//}
		String fl_nm = rdr.readString();
		return (readID(tp_nm, is_rtr, fl_nm));
	}

	public static HardwareSpec.Subtype readSubtypeID(ONL.Reader rdr) throws IOException
	{
		String tp_nm = rdr.readString();
		boolean is_rtr = false;
		if (rdr.getVersion() > 3.3 && rdr.getVersion() < 3.7 ) is_rtr = rdr.readBoolean();
		else
		{
			if (tp_nm.equals("NPR")) is_rtr = true;
		}
		String fl_nm = rdr.readString();
		HardwareSpec hw = readID(tp_nm, fl_nm);
		if (hw != null)
		{
			if (hw.isAny()) return (hw.getDefaultSubtype(is_rtr));
			else return (hw.getDefaultSubtype());
		}
		return null;
	}

	public HardwareSpec.Subtype getSubtype(String tp_nm)
	{
		HardwareSpec.Subtype rtn = subtypes.getSubtype(tp_nm);
		ExpCoordinator.print(new String("HardwareSpec.getSubtype tp_nm:"), 5);
		return rtn;
	}
	public HardwareSpec.Subtype readSubtypeID(String tp_nm, boolean is_rtr, String fl_nm)
	{
		HardwareSpec.Subtype rtn = subtypes.getSubtype(tp_nm);
		ExpCoordinator.print(new String("HardwareSpec.readSubtypeID tp_nm:" + tp_nm + " is_rtr:" + is_rtr + " fl_nm:" + fl_nm), 5);
		if (rtn == null)
		{
			manager.addSubtype(new java.io.File(fl_nm));
			rtn = subtypes.getSubtype(tp_nm);
		}
		return rtn;
	}
	public static HardwareSpec readID(String tp_nm, boolean is_rtr, String fl_nm)
	{
		ExpCoordinator.print(new String("HardwareSpec.readID tp_nm:" + tp_nm + " fl_nm:" + fl_nm), 6);
		HardwareSpec rtn = manager.getHardware(tp_nm);//, is_rtr);
		if (rtn == null)
		{
			rtn = manager.addFromFile(tp_nm, fl_nm);
		}
		return rtn;
	}

	public static HardwareSpec readID(String tp_nm, String fl_nm)
	{
		ExpCoordinator.print(new String("HardwareSpec.readID tp_nm:" + tp_nm + " fl_nm:" + fl_nm), 4);
		HardwareSpec rtn = manager.getHardware(tp_nm);
		if (rtn == null)
		{
			rtn = manager.addFromFile(tp_nm, fl_nm);
		}
		return rtn;
	}

	protected static HardwareSpec.Manager getTheManager() 
	{
		if (manager == null) manager = new Manager();
		return manager;
	}

	public int getNumPorts() { return (portSpec.getNumPorts());}
	public boolean hasDaemon() { return has_daemon;}
	public String getLabel() { return typeName;}
	public String getTypeLabel() { return typeName;}
	public String getDisplayLabel()
	{
		return typeName;
	}
	public double getVersion() { return version;}
	public int getCPPort() { return cpPort;}
	public boolean isClusterOnly() { return clusterOnly;}
	public void setClusterOnly(boolean b) { clusterOnly = b;}
	public String getComponentType() { return componentType;}
	//public boolean isAny() { return (componentType.equals(ANY));}
	//public boolean isRouter() { return (componentType.equals(ROUTER));}
	//public boolean isAddressed() { return (componentType.equals(ADDRESSED) || isRouter());}
	public JMenuItem getMenuItem() { return (new HardwareSpec.SubtypeMenu(this));}
	public void addSubtypeListener(ListDataListener l) { subtypes.addListDataListener(l);}
	public Subtype getDefaultSubtype() { return defaultSubtype;}
	public Subtype getDefaultSubtype(boolean is_rtr) 
	{
		if (defaultSubtype.isRouter() == is_rtr)
			return defaultSubtype;
		else
		{
			int max = subtypes.size();
			for (int i = 0; i < max; ++i)
			{
				Subtype selem = (Subtype)subtypes.getElementAt(i);
				if (selem.isRouter() == is_rtr) return selem;
			}
		}
		return null;
	}

	public void addSubtype(Subtype stp)
	{
		Subtype s = subtypes.getSubtype(stp.getLabel());
		ExpCoordinator.print(new String("HardwareSpec(" + getLabel() + ").addSubtype " + stp.getLabel()), TEST_ADD);
		if (s == null)
		{
			subtypes.addElement(stp);
		}
	}

	//interface ComponentXMLHandler.Spec
	/*
  public void addCommand(CommandSpec cspec)
  {
    if (cspec.isMonitor()) monitorSpecs.add(cspec);
    if (cspec.isCommand()) commandSpecs.add(cspec);
    if (cspec.isReboot()) rebootSpec = cspec;
    if (cspec.isInit()) initSpec = cspec;
  }
	 */
	public void addTable(HWTable.Spec htspec) { tableSpecs.add(htspec);}
	public void addField(FieldSpec fspec) { fieldSpecs.add(fspec);}
	public String getEndToken() { return (ExperimentXML.HARDWARE);}
	public boolean isPort() { return false;}
	public Vector getMonitorSpecs() { return monitorSpecs;}
	public Vector getCommandSpecs() { return commandSpecs;}
	public Vector getFieldSpecs() { return fieldSpecs;}
	public Vector getTableSpecs() { return tableSpecs;}
	//end interface ComponentXMLHandler.Spec
	public Subtypes getSubtypes() { return subtypes;}
}
