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
import org.xml.sax.helpers.*;
import java.lang.*;
import org.xml.sax.*;
import java.io.*;


public class MonitorXMLHandler extends DefaultHandler
{
	protected MonitorDataType.Base mdataType = null;
	protected ExperimentXML expXML = null;
	protected String currentElement = "";


	public MonitorXMLHandler(ExperimentXML exp_xml)
	{
		super();
		expXML = exp_xml;
	}

	public MonitorXMLHandler(String uri, Attributes attributes, ExperimentXML exp_xml)
	{
		this(exp_xml);
		processDataType(uri, attributes);
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) 
	{	  
		currentElement = new String(localName);
		ExpCoordinator.print(new String("MonitorXMLHandler.startElement " + localName), ExperimentXML.TEST_XML);
		if (localName.equals(ExperimentXML.DATA_TYPE)) processDataType(uri, attributes);
	}

	public void processDataType(String uri, Attributes attributes)
	{
		boolean logfile = Boolean.valueOf(attributes.getValue(ExperimentXML.LOGFILE)).booleanValue();
		String p_type = attributes.getValue(ExperimentXML.TYPE);
		if (logfile) mdataType = new MonitorDataType.LogFile(uri, attributes);
		else
		{
			if (p_type.equals(MonitorDataType.CONSTANT_LBL)) mdataType = new MonitorDataType.Constant(uri, attributes);
			if (p_type.equals(MonitorDataType.HWMONITOR_LBL) || p_type.equals(MonitorDataType.HWCOMMAND_LBL)) mdataType = new Hardware.MDataType(uri, attributes);
			if (p_type.equals(MonitorDataType.FORMULA_LBL)) mdataType = new MonitorDataType.MFormula(uri, attributes);
		}
		if (mdataType != null) expXML.setContentHandler(mdataType.getContentHandler(expXML));
	}
	public MonitorDataType.Base getMDataType() { return mdataType;}
}
