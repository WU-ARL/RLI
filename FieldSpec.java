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

import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class FieldSpec extends ParamSpec
{
	public static final int TEST_UPDATECMD = 3;
	int id = 0;
	UpdateCommandSpec updateCommand = null;
	AssignerSpec assigner = null;

	////////////////////////////////////////////////  FieldSpec.UpdateCommandSpec /////////////////////////////////////////////
	protected static class UpdateCommandSpec extends CommandSpec
	{
		//deal with perport command type when implementing UpdateCommand
		//public UpdateCommandSpec() { super();}
		public UpdateCommandSpec(String uri, Attributes attributes, FieldSpec fs)
		{
			super();
			commandType = CommandSpec.COMMAND;
			numParams = 1;
			if (attributes.getValue(ExperimentXML.NUMPARAMS) != null)
				numParams = Integer.parseInt(attributes.getValue(ExperimentXML.NUMPARAMS));
			params.add(new ParamSpec(fs, this));
			addDisplayGroup(new Integer(0));
			opcode = Integer.parseInt(attributes.getValue(ExperimentXML.OPCODE));
			String tmp_str = new String("Set " + fs.getLabel());
			description = tmp_str;
			displayLabel = tmp_str;
		}
		public UpdateCommandSpec(UpdateCommandSpec ucs, FieldSpec fs)
		{
			super(ucs);
			/*REMOVED 8/20/10
			commandType = ucs.commandType;
			numParams = 1;//Integer.parseInt(attributes.getValue(ExperimentXML.NUMPARAMS));
			params.add(new ParamSpec(fs, this));
			opcode = ucs.getOpcode();
			description = new String(ucs.description);
			displayLabel = new String(ucs.displayLabel);
			*/
		}
		//public void initParams(CommandSpec cspec, Vector defaults){}//REMOVED 8/20/10
		//public Command createCommand(ONLComponent p) { return null;}//REMOVED 8/20/10
		public String getXMLElemName() { return (ExperimentXML.UPDATE_COMMAND);}
	}

	/////////////////////////////////////////////////////////  FieldSpec.AssignerSpec ////////////////////////////////////////////////////////
	public static class AssignerSpec
	{
		private String type = ExperimentXML.HWASSIGN;
		private String name = "";

		public AssignerSpec(AssignerSpec as)
		{
			type = new String(as.type);
			name = new String(as.name);
		}
		public AssignerSpec(String tp)
		{
			type = new String(tp);
		}
		public void setName(String nm) { name = new String(nm);}
		public String getName() { return name;}
		public String getType() { return type;}
		public boolean isHWAssigner() { return (type.equals(ExperimentXML.HWASSIGN));}
		public boolean isPortAssigner() { return (type.equals(ExperimentXML.PORTASSIGN));}
		public boolean isTableAssigner() { return (type.equals(ExperimentXML.TABLEASSIGN));}
	}

	/////////////////////////////////////////////////////////  FieldSpec.XMLHandler //////////////////////////////////////////////////////////
	protected class XMLHandler extends ParamSpec.XMLHandler //DefaultHandler
	{ 
		public XMLHandler(XMLReader xmlr, ContentHandler hwh, FieldSpec p)
		{
			super(xmlr, hwh, (ParamSpec)p);
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			super.startElement(uri, localName, qName, attributes);
			if (localName.equals(ExperimentXML.UPDATE_COMMAND))
			{
				UpdateCommandSpec cs = new UpdateCommandSpec(uri, attributes, ((FieldSpec)currentParam));
				((FieldSpec)currentParam).updateCommand = cs;
				ExpCoordinator.print(new String("FieldSpec.XMLHandler.startElement("+ localName + ") updateCommand:" + cs.toString()), TEST_UPDATECMD);
				xmlReader.setContentHandler(cs.getXMLHandler(xmlReader, this));
			}
			if (localName.equals(ExperimentXML.HWASSIGN) || localName.equals(ExperimentXML.PORTASSIGN) || localName.equals(ExperimentXML.TABLEASSIGN))
				((FieldSpec)currentParam).assigner = new AssignerSpec(localName);
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("FieldSpec.XMLHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
			super.characters(ch, start, length);
			if (currentElement.equals(ExperimentXML.ID)) ((FieldSpec)currentParam).id = Integer.parseInt(new String(ch, start, length));
			//if (currentElement.equals(ExperimentXML.FIELDLOCALE)) ((FieldSpec)currentParam).setLocale(new String(ch, start, length));
			if (currentElement.equals(ExperimentXML.HWASSIGN) || currentElement.equals(ExperimentXML.PORTASSIGN) || currentElement.equals(ExperimentXML.TABLEASSIGN))
				((FieldSpec)currentParam).assigner.setName(new String(ch, start, length));

		}
		public void endElement(String uri, String localName, String qName)
		{
			super.endElement(uri, localName, qName);
			if (localName.equals(ExperimentXML.FIELD)) 
			{
				currentParam = null;
				xmlReader.setContentHandler(hwhandler);
			}
		}
	}//end FieldSpec.XMLHandler


	///////////////////////////////////////////////// FieldSpec methods //////////////////////////////////////////////////////////////
	//public FieldSpec(){ super();}
	public FieldSpec(String uri, Attributes attributes) { super(uri, attributes);}
	public FieldSpec(String lbl, String tp) { super(lbl,tp);}
	public FieldSpec(FieldSpec p) 
	{
		super((ParamSpec)p);
		if (p.updateCommand != null)
			updateCommand = new UpdateCommandSpec(p.updateCommand, this);
		if (p.assigner != null) assigner = new AssignerSpec(p.assigner);
	}
	public Field createField(ONLComponent parent)
	{
		ExpCoordinator.print(new String("FieldSpec(" + label + ").createField parent wildcard:" + wildcardValue), TEST_WILDCARD);
		return (new Field(this, parent));
	}
	public Field createField(ONLComponent parent, Field.Owner o)
	{
		ExpCoordinator.print(new String("FieldSpec(" + label + ").createField (parent, fieldowner) wildcard:" + wildcardValue), TEST_WILDCARD);
		return (new Field(this, parent, o));
	}
	public boolean isEqual(ParamSpec p)
	{
		if (p instanceof FieldSpec)
		{
			FieldSpec tmp_field = (FieldSpec)p;
			return (super.isEqual((ParamSpec)tmp_field) && id == tmp_field.id);
		}
		return false;
	}
	public CommandSpec getUpdateCommand() { return updateCommand;}
	public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) { return (new XMLHandler(xmlr, hwh, this));}
	public FieldSpec.AssignerSpec getAssigner() { return assigner;}
	public boolean isAssigned() { return (assigner != null);}
} //end class FieldSpec
