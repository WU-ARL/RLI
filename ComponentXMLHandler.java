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

import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;
import java.lang.String;
import java.util.Vector;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////// ComponentXMLHandler /////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

public class ComponentXMLHandler extends DefaultHandler
{
	public final static int TEST_XML = ExperimentXML.TEST_XML;
	protected Spec spec = null;
	protected ContentHandler lastHandler = null;
	protected XMLReader xmlReader = null;
	protected String currentElement = "";
	protected int commandType = CommandSpec.COMMAND;
	protected String section = "";
	private double version = ExpCoordinator.VERSION;

	public static interface Spec
	{
		public CommandSpec addCommand(CommandSpec cspec);
		public void addTable(HWTable.Spec htspec);
		public void addField(FieldSpec fspec);
		public void addAssigner(AssignerSpec aspec);
		public String getEndToken();
		public boolean isPort();
		public Vector getMonitorSpecs();
		public Vector getCommandSpecs();
		public Vector getFieldSpecs();
		public Vector getTableSpecs();
		public Vector getAssignerSpecs();
	}

	public static class AssignerSpec
	{
		private int max = 0;
		private int min = 0;
		private String name = "";

		public AssignerSpec(String uri, Attributes attributes)
		{
			max = Integer.parseInt(attributes.getValue(ExperimentXML.MAX));
			min = Integer.parseInt(attributes.getValue(ExperimentXML.MIN));
			name = new String(attributes.getValue(ExperimentXML.NAME));
		}
		public AssignerSpec(AssignerSpec as)
		{
			max = as.max;
			min = as.min;
			name = new String(as.name);
		}

		public IDAssigner createAssigner()
		{
			return (new IDAssigner(name, max, min, true));
		}

		public int getMin() { return min;}
		public int getMax() { return max;}
		public String getName() { return name;}
	}

	public ComponentXMLHandler(XMLReader xmlr) { this(xmlr, null, null);}
	public ComponentXMLHandler(XMLReader xmlr, Spec sp) { this(xmlr, sp, null);}
	public ComponentXMLHandler(XMLReader xmlr, Spec sp, ContentHandler last_h)
	{
		super();
		lastHandler = last_h;
		spec = sp;
		xmlReader = xmlr;
		xmlReader.setContentHandler(this);
	}
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
	{
		currentElement = new String(localName);
		ExpCoordinator.print(new String("ComponentXMLHandler.startElement localName:" + localName), TEST_XML);
		if (isSection(localName)) section = new String(localName);

		if (localName.equals(ExperimentXML.FIELD)) 
		{
			FieldSpec fs = new FieldSpec(uri, attributes);
			spec.addField(fs);
			xmlReader.setContentHandler(fs.getXMLHandler(xmlReader,this));
		}
		if (localName.equals(ExperimentXML.ASSIGNER)) 
		{
			AssignerSpec as = new AssignerSpec(uri, attributes);
			spec.addAssigner(as);
		}
		if (localName.equals(ExperimentXML.REBOOT)) commandType = CommandSpec.REBOOT;
		if (localName.equals(ExperimentXML.INIT)) commandType = CommandSpec.INIT;
		if (localName.equals(ExperimentXML.SUBTYPE_INIT)) commandType = CommandSpec.SUBTYPE_INIT;
		if (localName.equals(ExperimentXML.COMMAND)) 
		{
			if (section.equals(ExperimentXML.CONFIGURE))
				commandType = CommandSpec.CFG_COMMAND;
			else
			{
				if (section.equals(ExperimentXML.COMMANDS))
					commandType = CommandSpec.COMMAND;
				if (section.equals(ExperimentXML.MONITORING))
					commandType = CommandSpec.MONITOR;
			}   
			if (spec.isPort()) commandType = commandType | CommandSpec.PERPORT;
		}
		if (localName.equals(ExperimentXML.MONITOR)) 
		{
			commandType = CommandSpec.MONITOR;
			if (spec.isPort()) commandType = commandType | CommandSpec.PERPORT;
		}

		if (localName.equals(ExperimentXML.COMMAND) || 
				localName.equals(ExperimentXML.MONITOR) || 
				localName.equals(ExperimentXML.REBOOT) || 
				localName.equals(ExperimentXML.INIT) ||
				localName.equals(ExperimentXML.SUBTYPE_INIT))
		{
			CommandSpec cspec = createCommandSpec(uri, localName, attributes, commandType);
			cspec = spec.addCommand(cspec);
			xmlReader.setContentHandler(cspec.getXMLHandler(xmlReader, this));
		}
		if (localName.equals(ExperimentXML.TABLE))
		{
			HWTable.Spec tcspec = new HWTable.Spec(uri, attributes, spec.isPort());
			spec.addTable(tcspec);
			xmlReader.setContentHandler(tcspec.getXMLHandler(xmlReader, this));
		}
		if (localName.equals(ExperimentXML.ROUTE_TABLE))
		{
			HWRouteTable.Spec tcspec = new HWRouteTable.Spec(uri, attributes, spec.isPort());
			spec.addTable(tcspec);
			xmlReader.setContentHandler(tcspec.getXMLHandler(xmlReader, this));
		}
	}

	protected CommandSpec createCommandSpec(String uri, String localName, Attributes attributes, int ctype) { return (new CommandSpec(uri, attributes, ctype));}
	public void endElement(String uri, String localName, String qName)
	{
		ExpCoordinator.print(new String("ComponentXMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
		if (spec != null && localName.equals(spec.getEndToken()) && lastHandler != null)
		{
			xmlReader.setContentHandler(lastHandler);
		}
		if (localName.equals(section)) section = "";
	}
	public void setSpec(ComponentXMLHandler.Spec sp) { spec = sp;}
	public ComponentXMLHandler.Spec getSpec() { return spec;}
	public boolean isSection(String nm)
	{
		return (nm.equals(ExperimentXML.FIELDS) ||
				nm.equals(ExperimentXML.MONITORING) || 
				nm.equals(ExperimentXML.COMMANDS) ||
				nm.equals(ExperimentXML.TABLES) ||
				nm.equals(ExperimentXML.CONFIGURE));
	}
}// ComponentXMLHandler
