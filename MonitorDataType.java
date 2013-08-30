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
 * File: MonitorDataType.java
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

import java.io.*;
import java.util.*;
import java.awt.Component;
import javax.swing.*;
import java.lang.reflect.Array;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;


class MonitorDataType
{
	//DATA TYPES
	public static final int UNKNOWN = 0;
	public static final int FORMULA = 1;
	public static final int IPPBANDWIDTH = 2;
	public static final int VPIBANDWIDTH = 3;
	public static final int VCIBANDWIDTH = 4;
	public static final int OPPBANDWIDTH = 5;
	public static final int DQSTATS = 6;
	public static final int IPPDISCARD = 7;
	public static final int OPPDISCARD = 8;
	public static final int PORTSTATUS = 9;
	public static final int HWMONITOR = 13;
	public static final int HWCOMMAND = 14;
	public static final int CONSTANT = 16;
	public static final int DQSTRESS = 17;
	public static final int PLUGIN = 18;
	public static final int WUGSCLOCKRATE = 19;
	public static final int NSPOP = 64;
	public static final int NPOP = 256;

	public static final String UNKNOWN_LBL = "unknown";
	public static final String FORMULA_LBL = ExperimentXML.FORMULA;//"formula";
	public static final String IPPBANDWIDTH_LBL = "ippbw";
	public static final String VPIBANDWIDTH_LBL = "vpibw";
	public static final String VCIBANDWIDTH_LBL = "vcibw";
	public static final String OPPBANDWIDTH_LBL = "oppbw";
	public static final String IPPDISCARD_LBL = "ippdiscard";
	public static final String OPPDISCARD_LBL = "oppdiscard";
	public static final String PORTSTATUS_LBL = "portstats";
	public static final String HWMONITOR_LBL = "hwMonitor";
	public static final String HWCOMMAND_LBL = "hwCommand";
	public static final String CONSTANT_LBL = "constant";
	public static final String PLUGIN_LBL = "plugin";
	public static final String WUGSCLOCKRATE_LBL = "clockRate";
	public static final String NSPOP_LBL = "nsp";
	public static final String LOGFILE_LBL = "logfile";


	public static final int LIVE_DATA = 1;
	public static final int MONITOR_FUNCTION = 2;
	public static final int LOG_FILE = 0;
	public static final int CONDITIONAL = 32;

	public static class DescriptionDialog
	{
		private Component pcomponent = null;
		public DescriptionDialog(Component c) { pcomponent = c;}
		public int showDescription(MonitorDataType.Base mdt){ return (showDescription(mdt, null, 0));}
		public int showDescription(MonitorDataType.Base mdt, Object[] suffix, int suffix_length) { return (showDescription(mdt, suffix, suffix_length, true));}
		public int showDescription(MonitorDataType.Base mdt, Object[] suffix, int suffix_length, boolean show_rate)
		{	
			Object[] options = {"Change", "Close"};
			Object[] array;
			boolean tmp_edit = !(ExpCoordinator.isObserver());
			int end_prefix = 3;
			JRadioButton rate = null;
			JRadioButton abs_val = null;
			if (mdt.canBeRate() && show_rate) 
			{
				end_prefix = 4;
			}
			if (mdt.getONLComponent() != null) 
			{
				++end_prefix;
				if (mdt.getPort() > -1) ++end_prefix;
			}
			if (mdt.isPeriodic()) ++end_prefix;
			array = new Object[end_prefix + suffix_length];
			if (mdt.getONLComponent() != null) 
			{
				if (mdt.getPort() > -1) 
				{
					String port_string = "Port ";
					//int tp = mdt.getONLComponent().getType();
					//ONLComponent c = mdt.getONLComponent().getParent();
					//int ptp = tp;
					//if (c != null) ptp = c.getType();
					if (mdt.getONLComponent().isAncestor(ONLComponent.NSP_LBL))
						//tp == ONLComponent.NSP || tp == ONLComponent.NSPPORT ||
						//ptp == ONLComponent.NSP || ptp == ONLComponent.NSPPORT)
					{
						if (mdt.isIPP()) port_string ="Input port ";
						else port_string = "Output port ";
					}
					array[3] = new String(port_string + mdt.getPort());
					array[4] = mdt.getDescription();
				}
				else 
					array[3] = mdt.getDescription();
				array[2] = mdt.getONLComponent().getLabel();
			}
			else 
			{
				array[2] = mdt.getDescription();
			}
			//ExpCoordinator.print(new String("MonitorDataType.DescriptionDialog.showDescription end prefix " + end_prefix + " suffix length " + suffix_length), 5);
			if (mdt.canBeRate() && show_rate)
			{
				ExpCoordinator.print("    show rate", 5);
				rate = new JRadioButton("rate");
				abs_val = new JRadioButton("absolute value");
				if (mdt.isRate()) rate.setSelected(true);
				else abs_val.setSelected(true);
				ButtonGroup bg = new ButtonGroup();
				rate.setEnabled(tmp_edit);
				abs_val.setEnabled(tmp_edit);
				bg.add(rate);
				bg.add(abs_val);
				JPanel p = new JPanel();
				p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
				p.setAlignmentX(Component.LEFT_ALIGNMENT);
				rate.setAlignmentX(Component.LEFT_ALIGNMENT);
				abs_val.setAlignmentX(Component.LEFT_ALIGNMENT);
				p.add(rate);
				p.add(abs_val);
				int ndx = end_prefix - 1;
				if (mdt.isPeriodic()) ndx -= 1;
				array[ndx] = p;
			}

			if (mdt.isPeriodic()) array[end_prefix-1] = new String("Polling every " + mdt.getPollingRate().getSecsOnly() + " seconds");

			array[0] = "name:";
			JTextField nm = new JTextField(10);
			nm.setText(mdt.getName());
			nm.setEditable(tmp_edit);
			array[1] = nm;

			int end_array = end_prefix + suffix_length;
			int j = 0;
			for (int i = end_prefix; i < end_array; ++i)
			{
				array[i] = suffix[j];
				++j;
			}
			int rtn = JOptionPane.showOptionDialog(pcomponent, 
					array, 
					mdt.getName(), 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, 
					null,
					options,
					options[0]);
			if (rtn == JOptionPane.YES_OPTION && tmp_edit)
			{
				//ExpCoordinator.printer.print(new String("MDT::showDescription changing name from " + name));
				mdt.setName(nm.getText());
				//ExpCoordinator.printer.print(new String("          to  " + name));
				if (rate != null)
				{
					if (rate.isSelected()) mdt.setIsRate(true);
					else mdt.setIsRate(false);
				}
			}
			return rtn;
		}
	}

	public static abstract class Base
	{
		protected int paramType;
		protected int dataUnits = Units.UNKNOWN;
		protected String name = "";
		protected int dataType = LIVE_DATA;
		protected int daemonType = ONLDaemon.UNKNOWN;
		protected int monitorID; //used by monitor entry to decide which field to look at
		protected PollingRate pollingRate = null;
		protected int port = -1;//designates a non-port function
		protected ONLComponent onlComponent = null;
		protected int specLength = 7;
		private boolean periodic = true;
		private ONLComponent.ONLMonitorable onlMonitorable = null;
		private boolean is_rate = true;
		private String description = "";
		private double version = ExpCoordinator.VERSION;

		//////////////////////////////////// MonitorDataType.Base.XMLHandler ////////////////////////////
		protected static class XMLHandler extends DefaultHandler
		{
			protected ExperimentXML expXML = null;
			protected String currentElement = "";
			protected MonitorDataType.Base mdataType = null;

			//////////////////////////////////////////// MonitorDataType.Base.XMLHandler.ComponentHandler ////////////////////////////////////////////////////////
			private class ComponentHandler extends ONLComponent.XMLComponentHandler
			{
				public ComponentHandler(String uri, Attributes attributes, ExperimentXML exp_xml)
				{
					super(uri,attributes,exp_xml);
				}

				public void endElement(String uri, String localName, String qName)
				{
					if (localName.equals(ExperimentXML.COMPONENT))
					{
						ExpCoordinator.print(new String("MonitorXMLHandler.ComponentHandler.endElement " + localName + "  component " + getComponent().getLabel()), ExperimentXML.TEST_XML);
						expXML.removeContentHandler(this);
						if (mdataType != null) mdataType.setONLComponent(getComponent());
					}
				}
			}// end class MonitorDataType.Base.XMLHandler.ComponentHandler

			public XMLHandler(ExperimentXML exp_xml, MonitorDataType.Base mdt)
			{
				super();
				expXML = exp_xml;
				mdataType = mdt;
				mdataType.is_rate = false;
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	  
				currentElement = new String(localName);
				ExpCoordinator.print(new String("MonitorDataType.Base.XMLHandler.startElement " + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.COMPONENT)) expXML.setContentHandler(new ComponentHandler(uri, attributes, expXML));
				if (localName.equals(ExperimentXML.RATE)) mdataType.is_rate = true;
				if (localName.equals(ExperimentXML.POLLING_RATE)) 
				{
					mdataType.pollingRate = new PollingRate();
					mdataType.pollingRate.setRate(attributes);
				}
			}
			public void characters(char[] ch, int start, int length)
			{
				if (currentElement.equals(ExperimentXML.PORT)) mdataType.port = Integer.parseInt(new String(ch, start, length));
				if (currentElement.equals(ExperimentXML.UNITS)) mdataType.dataUnits = Units.getType(new String(ch, start, length));
			}
			public void setCurrentElement(String s) { currentElement = new String(s);}
			public String getCurrentElement() { return currentElement;}
			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("MonitorDataType.Base.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.DATA_TYPE)) 
				{
					mdataType.print(ExperimentXML.TEST_XML);
					expXML.removeContentHandler(this);
				}
			}
		}//end class MonitorDataType.Base.XMLHandler

		public boolean isEqual(MonitorDataType.Base mdt)
		{
			return (paramType == mdt.paramType && 
					name.equals(mdt.name) && 
					dataType == mdt.dataType && 
					port == mdt.port && 
					monitorID == mdt.monitorID && 
					onlComponent == mdt.onlComponent &&
					is_rate == mdt.is_rate &&
					dataUnits == mdt.dataUnits);
		}

		public void print() { print(0);}
		public void print(int level)
		{
			int dt = dataType;
			if ((dt & CONDITIONAL) > 0) 
			{
				ExpCoordinator.printer.print("conditional", level);
				dt = dt - CONDITIONAL;
			}
			switch (dt)
			{
			case LIVE_DATA:
				ExpCoordinator.printer.print("live data", level);
				break;
			case MONITOR_FUNCTION:
				ExpCoordinator.printer.print("monitor function", level);
				break;
			case CONSTANT:
				ExpCoordinator.printer.print("constant", level);
				break;
			default:
				ExpCoordinator.printer.print("logfile", level);
			}
			switch(paramType)
			{
			case WUGSCLOCKRATE:
				ExpCoordinator.printer.print("WUGSClockRate", level);
				break;
			case VCIBANDWIDTH:
				ExpCoordinator.printer.print("VCIBandwidth", level);
				break;
			case VPIBANDWIDTH:
				ExpCoordinator.printer.print("VPIBandwidth", level);
				break;
			case IPPBANDWIDTH:
				ExpCoordinator.printer.print("IPPBandwidth", level);
				break;
			case OPPBANDWIDTH:
				ExpCoordinator.printer.print("OPPBandwidth", level);
				break;
			case IPPDISCARD:
				ExpCoordinator.printer.print("IPPDiscard", level);
				break;
			case OPPDISCARD:
				ExpCoordinator.printer.print("OPPDiscard", level);
				break;
			case DQSTRESS:
			case DQSTATS:
				ExpCoordinator.printer.print("DQStats", level);
				break;
			case CONSTANT:
				ExpCoordinator.printer.print("Constant", level);
				break;
			case HWMONITOR:
				ExpCoordinator.print(new String("HardwareMonitor " + monitorID), level);
				break;
			case HWCOMMAND:
				ExpCoordinator.print(new String("HardwareCommand " + monitorID), level);
				break;
			default: //don't know what it is so just read the next 5 lines
				ExpCoordinator.printer.print("UNKNOWN", level);
			}
			ExpCoordinator.printer.print(new String("name " + name), level);
			if (onlComponent != null) ExpCoordinator.printer.print(new String("switch " + onlComponent.getLabel()), level);
			else ExpCoordinator.printer.print(" no switch", level);
			ExpCoordinator.printer.print(new String("port " + port), level);
			ExpCoordinator.printer.print(new String("isRate " + is_rate), level);
			ExpCoordinator.printer.print("polling rate", level);
			if (pollingRate != null) pollingRate.print(level);
			ExpCoordinator.printer.print(getSpec(), level);
		}

		protected Base(int dt, int t, String nm)
		{
			paramType = t;
			daemonType = dt;
			name = nm;
			pollingRate = new PollingRate();
		}
		protected Base(int dt, int t, String nm, int du) 
		{
			this(dt, t, nm);
			dataUnits = du;
		}
		protected Base(int dt, int t, int du) 
		{
			paramType = t;
			daemonType = dt;
			dataUnits = du;
			pollingRate = new PollingRate();
		}
		protected Base(String uri, Attributes attributes, int dt, int t)
		{
			name = new String(attributes.getValue(ExperimentXML.NAME));
			daemonType = dt;
			paramType = t;
			boolean b = Boolean.valueOf(attributes.getValue(ExperimentXML.LOGFILE)).booleanValue();
			if (b) dataType = LOG_FILE;
			else
			{
				if (attributes.getValue(ExperimentXML.TYPE).equals(FORMULA_LBL)) dataType = MONITOR_FUNCTION;
				else dataType = LIVE_DATA;
			}
		}
		protected void initFromFile(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			dataType = lv;
			if (infile.ready())
			{
				version = infile.getVersion();
				ExpCoordinator.printer.print("MonitorDataType.Base read param", 2);
				if (expcoord != null) 
				{
					setONLComponent(expcoord.getComponentFromString(infile.readLine()));
					if (onlComponent != null) ExpCoordinator.printer.print(new String("     node = " + onlComponent.toString()), 2);
				}
				else //PROBLEM not sure ever want this case
				{
					if (isLogFile()) onlComponent = null;
					infile.readLine();
				}
				port = Integer.parseInt(infile.readLine());
				ExpCoordinator.printer.print(new String("     port = " + port), 2);
				String ln = infile.readLine();
				if (!ln.equals("not periodic"))
				{
					pollingRate = new PollingRate(ln);
					ExpCoordinator.printer.print(new String("     pollingRate = " + pollingRate.toString()), 2);
				}
				else ExpCoordinator.printer.print("not periodic", 2);
				ExpCoordinator.printer.print(new String("     version = " + infile.getVersion()), 2);
				if (version >= 1.3)
				{
					is_rate = Boolean.valueOf(infile.readLine()).booleanValue();
					ExpCoordinator.printer.print(new String(" isRate = " + is_rate), 2);
				}
				if (version > 2.0)
				{
					String tmp_str = infile.readLine();
					dataUnits = Units.getType(tmp_str);
					ExpCoordinator.printer.print(new String(" units = " + tmp_str), 2);
				}
				name = infile.readLine();
				ExpCoordinator.printer.print(new String("     name = " + name), 2);
				loadFromSpec(infile.readLine());
				ExpCoordinator.printer.print(new String("     monitorID = " + getMonitorID()), 2);

				//now add monitor to switch descriptor
				//if (onlComponent != null) onlComponent.addMonitor(this);
			}
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			dataType = dt;
			ExpCoordinator expcoord = ExpCoordinator.theCoordinator;
			if (reader.ready())
			{
				version = reader.getVersion();
				ExpCoordinator.printer.print("MonitorDataType.Base.initFromReader", 2);
				setONLComponent(expcoord.getComponentFromString(reader.readString()));
				if (onlComponent != null) ExpCoordinator.printer.print(new String("     node = " + onlComponent.toString()), 2);
				port = reader.readInt();
				ExpCoordinator.printer.print(new String("     port = " + port), 2);
				String ln = reader.readString();
				if (!ln.equals("not periodic"))
				{
					pollingRate = new PollingRate(ln);
					ExpCoordinator.printer.print(new String("     pollingRate = " + pollingRate.toString()), 2);
				}
				else ExpCoordinator.printer.print("not periodic", 2);
				ExpCoordinator.printer.print(new String("     version = " + reader.getVersion()), 2);
				if (version >= 1.3)
				{
					is_rate = reader.readBoolean();
					ExpCoordinator.printer.print(new String(" isRate = " + is_rate), 2);
				}
				if (version > 2.0)
				{
					String tmp_str = reader.readString();
					dataUnits = Units.getType(tmp_str);
					ExpCoordinator.printer.print(new String(" units = " + tmp_str), 2);
				}
				name = reader.readString();
				ExpCoordinator.printer.print(new String("     name = " + name), 2);
			}
		}
		public void setPeriodic(boolean b)
		{
			periodic = b;
			if (!b) setIsRate(false);
		}
		public boolean isPeriodic() { return (periodic);}
		public double convertData(double data) { return data;} //override for any conversion
		public void setName(String nm) 
		{
			name = nm;
			//ExpCoordinator.printer.print("MonitorDataType setName " + name);
		}
		public String getName() { return name;}
		public void setDataUnits(int du) { dataUnits = du;}
		public int getDataUnits() 
		{
			if (is_rate)
				return (dataUnits | Units.PERSEC);
			else
				return dataUnits;
		}
		public boolean isRate() { return (is_rate && canBeRate());}
		public boolean isAbsolute() { return (!is_rate || !canBeRate());}
		public void setIsRate(boolean b) { is_rate = b;}
		public void setPollingRate(double s, double us) 
		{ 
			periodic = true;
			if (pollingRate == null) pollingRate = new PollingRate(s, us);
			else pollingRate.setRate(s, us);
		}
		public void setPollingRate(PollingRate pr) 
		{ 
			setPeriodic(pr != null);
			pollingRate = pr;
		}
		public PollingRate getPollingRate() { return pollingRate;}
		public void setONLComponent(ONLComponent sd) { onlComponent = sd;}
		public ONLComponent getONLComponent() { return onlComponent;}

		private int getDataType() { return dataType;}
		public int getDaemonType() { return daemonType;}
		public int getParamType() { return paramType;}
		public boolean isLiveData() { return ((dataType & LIVE_DATA) > 0);}
		public boolean isLogFile() { return (dataType == LOG_FILE);}
		public boolean isConstant() { return (dataType == CONSTANT);}
		public boolean isMonitorFunction() { return ((dataType & MONITOR_FUNCTION) > 0);}
		public boolean isConditional() { return ((dataType & CONDITIONAL) > 0);}
		public boolean isType(int tp) { return (daemonType == tp);}
		public boolean canBeRate() { return true;}

		public double getVersion() { return version;} //if read from file returns the version of the file
		public int getPort() { return port;}
		public boolean isIPP() { return true;} //override for OPP functions
		public boolean isNodeWide() { return (port < 0);}
		public int getSpecLength() { return specLength;} //returns the length of the spec in lines. this is for skipping over in logfiles

		public int getMonitorID() { return monitorID;}


		public abstract String getSpec(); //returns line that represents this data structure
		public abstract void loadFromSpec(String ln);//fills in params based on ln
		public String getDescription()
		{
			if (description.length() == 0) setDescription();
			if (is_rate)
				return (description.concat("/sec"));
			else return description;
		}
		protected abstract void setDescription();//define to set description
		public void setDescription(String d) { description = d;}
		public void saveToLogFile(ONL.Writer outfile) throws IOException
		{
			write(outfile);
			//saveToFile(new SpecFile.SpecWriter(outfile));
		}
		public void saveToFile(SpecFile.SpecWriter outfile) throws IOException
		{
			outfile.writeLine(String.valueOf(dataType));
			outfile.writeLine(String.valueOf(paramType)); //param type
			if (onlComponent != null) outfile.writeLine(new String(onlComponent.getLabel() + " " + onlComponent.getType()));//switch descriptor
			else outfile.writeLine("null");
			outfile.writeLine(String.valueOf(port));//port
			if (pollingRate != null)
			{
				outfile.writeLine(pollingRate.toString());
				ExpCoordinator.printer.print(new String("MDT:: saveToFile " + name), 2);
				pollingRate.print(2);
			}
			else outfile.writeLine("not periodic");
			outfile.writeLine(String.valueOf(is_rate));
			outfile.writeLine(Units.getLabel(dataUnits));
			outfile.writeLine(name);
			outfile.writeLine(getSpec());
		} 
		public void write(ONL.Writer writer) throws IOException
		{
			writer.writeInt(dataType);
			writer.writeInt(paramType); //param type
			if (onlComponent != null) writer.writeString(onlComponent.toString());//switch descriptor
			else writer.writeLine("null");
			writer.writeInt(port);//port
			if (pollingRate != null)
			{
				writer.writeString(pollingRate.toString());
				ExpCoordinator.print(new String("MonitorDataType.Base.write " + name), 2);
				pollingRate.print(2);
			}
			else writer.writeString("not periodic");
			writer.writeBoolean(is_rate);
			writer.writeString(Units.getLabel(dataUnits));
			writer.writeString(name);
		} 
		public String getParamTypeName() { return HWMONITOR_LBL;}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(ExperimentXML.DATA_TYPE);
			xmlWrtr.writeAttribute(ExperimentXML.NAME, name);
			if (dataType == LOG_FILE) xmlWrtr.writeAttribute(ExperimentXML.LOGFILE, "true");
			else xmlWrtr.writeAttribute(ExperimentXML.LOGFILE, "false");
			xmlWrtr.writeAttribute(ExperimentXML.TYPE, getParamTypeName());
			writeXMLHeader(xmlWrtr);
			if (onlComponent != null)
			{
				xmlWrtr.writeStartElement(ExperimentXML.COMPONENT);
				onlComponent.writeXMLID(xmlWrtr);
				xmlWrtr.writeEndElement();
			}
			if (port >= 0)
			{
				xmlWrtr.writeStartElement(ExperimentXML.PORT);
				xmlWrtr.writeCharacters(String.valueOf(port));
				xmlWrtr.writeEndElement();
			}
			if (pollingRate != null)
			{
				xmlWrtr.writeStartElement(ExperimentXML.POLLING_RATE);
				pollingRate.writeXML(xmlWrtr);
				xmlWrtr.writeEndElement();
			}
			if (is_rate) 
			{
				xmlWrtr.writeStartElement(ExperimentXML.RATE);
				xmlWrtr.writeEndElement();
			}
			xmlWrtr.writeStartElement(ExperimentXML.UNITS);
			xmlWrtr.writeCharacters(Units.getLabel(dataUnits));
			xmlWrtr.writeEndElement();
			writeXMLFooter(xmlWrtr);
			xmlWrtr.writeEndElement();
		}
		public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException{}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException{}
		public void writeChangeable(ONL.Writer writer) throws IOException 
		{ 
			writer.writeBoolean(is_rate);
			writer.writeString(Units.getLabel(dataUnits));
			writer.writeString(name);
		}
		public void readChangeable(ONL.Reader reader) throws IOException 
		{ 
			setIsRate(reader.readBoolean());
			String tmp_str = reader.readString();
			setDataUnits(Units.getType(tmp_str));
			setName(reader.readString());
		}
		public static MonitorDataType.Base loadFromFile(SpecFile.SpecReader infile, ExpCoordinator expcoord)  throws IOException
		{	
			String line = null;
			MonitorDataType.Base rtn = null;
			if (infile.ready())
			{
				line = infile.readLine();
				ExpCoordinator.printer.print(new String("MonitorDataType::loadFromFile line =" + line + " length = " + line.length()), 2);
				int live_data = Integer.parseInt(line);
				ExpCoordinator.printer.print(new String("     live_data = " + live_data), 2);
				line = infile.readLine();
				ExpCoordinator.printer.print(new String("     line = " + line), 3);
				int p_type = Integer.parseInt(line);
				ExpCoordinator.printer.print(new String("     ptype = " + p_type), 2);
				if ((live_data & MONITOR_FUNCTION) > 0)
				{
					switch(p_type)
					{
					case FORMULA:
						rtn = new MFormula(infile);
						break;
					default:
						rtn = null;
					}
				}  
				else 
				{
					if (live_data == LOG_FILE)
					{
						ExpCoordinator.printer.print(new String("    found logfile"), 2);
						rtn = new LogFile(infile, live_data);
					}
					else
					{
						switch(p_type)
						{
						case VCIBANDWIDTH:
							rtn = new VCIBandwidth(infile, live_data, expcoord);
							break;
						case VPIBANDWIDTH:
							rtn = new VPIBandwidth(infile, live_data, expcoord);
							break;
						case IPPBANDWIDTH:
							rtn = new IPPBandwidth(infile, live_data, expcoord);
							break;
						case OPPBANDWIDTH:
							rtn = new OPPBandwidth(infile, live_data, expcoord);
							break;
						case IPPDISCARD:
							rtn = new IPPDiscard(infile, live_data, expcoord);
							break;
						case OPPDISCARD:
							rtn = new OPPDiscard(infile, live_data, expcoord);
							break;
						case CONSTANT:
							rtn = new Constant(infile);
							break;
						case PLUGIN:
							rtn = new PluginData.MDataType(infile, live_data, expcoord);
							break;
						case HWMONITOR:
						case HWCOMMAND:
							rtn = new Hardware.MDataType(p_type, infile, live_data, expcoord);
							break;
						default: 
							if ((p_type & NSPOP) > 0)
								rtn = NSPMonClasses.getMonitorDataType(p_type, infile, live_data, expcoord);
						break;
						}
					}
				}
			}
			//if (rtn != null) rtn.print();
			return rtn;
		}
		public static MonitorDataType.Base read(ONL.Reader reader)  throws IOException
		{	
			ExpCoordinator expcoord = ExpCoordinator.theCoordinator;
			String str = null;
			MonitorDataType.Base rtn = null;
			if (reader.ready())
			{
				ExpCoordinator.printer.print("MonitorDataType.Base.read", 2);
				int live_data = reader.readInt();
				ExpCoordinator.printer.print(new String("     live_data = " + live_data), 2);
				int p_type = reader.readInt();
				ExpCoordinator.printer.print(new String("     ptype = " + p_type), 2);
				if ((live_data & MONITOR_FUNCTION) > 0)
				{
					switch(p_type)
					{
					case FORMULA:
						rtn = new MFormula(reader);
						break;
					default:
						rtn = null;
					}
				}  
				else 
				{
					if (live_data == LOG_FILE)
					{
						ExpCoordinator.printer.print(new String("    found logfile"), 2);
						rtn = new LogFile(reader, live_data);
					}
					else
					{
						switch(p_type)
						{
						case VCIBANDWIDTH:
							rtn = new VCIBandwidth(reader, live_data);
							break;
						case VPIBANDWIDTH:
							rtn = new VPIBandwidth(reader, live_data);
							break;
						case IPPBANDWIDTH:
							rtn = new IPPBandwidth(reader, live_data);
							break;
						case OPPBANDWIDTH:
							rtn = new OPPBandwidth(reader, live_data);
							break;
						case IPPDISCARD:
							rtn = new IPPDiscard(reader, live_data);
							break;
						case OPPDISCARD:
							rtn = new OPPDiscard(reader, live_data);
							break;
						case CONSTANT:
							rtn = new Constant(reader);
							break;
						case PLUGIN:
							rtn = new PluginData.MDataType(reader, live_data);
							break;
						case HWMONITOR:
						case HWCOMMAND:
							rtn = new Hardware.MDataType(p_type, reader, live_data);
							break;
						default: 
							if ((p_type & NSPOP) > 0)
								rtn = NSPMonClasses.getMonitorDataType(p_type, reader, live_data);
						break;
						}
					}
				}
			}
			//if (rtn != null) rtn.print();
			return rtn;
		}

		public void setMonitorable(ONLComponent.ONLMonitorable om) 
		{
			//ExpCoordinator.print(new String("MonitorDataType.Base.setMonitorable " + om), 2);
			onlMonitorable = om;
		}
		public ONLComponent.ONLMonitorable getMonitorable() { return onlMonitorable;}
		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new XMLHandler(exp_xml, this));}
	} //end class MonitorDataType.Base

	public static class ClockRate extends Base
	{
		public ClockRate(ONLComponent c)
		{
			super(ONLDaemon.WUGSC, WUGSCLOCKRATE, Units.UNKNOWN);
			setONLComponent(c);
		}
		public String getSpec() //returns line that represents this data structure
		{ 
			return ("");
		}
		public void loadFromSpec(String ln) {}//fills in params based on ln
		protected void setDescription()
		{
			setDescription(new String("WUGS Clockrate"));
		}
	} 
	public static class PortStatus extends Base
	{
		public PortStatus(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			super(ONLDaemon.WUGSC, PORTSTATUS, Units.BYTES);
			initFromFile(infile, lv, expcoord);
		}
		public PortStatus(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.WUGSC, PORTSTATUS, Units.BYTES);
			initFromReader(rdr, dt);
		}
		public PortStatus(int p, boolean in)
		{
			super(ONLDaemon.WUGSC, PORTSTATUS, "PortStatus", Units.BYTES);
			port = p;
			if (in) monitorID = 1;
			else monitorID = 0;
		}    
		public String getSpec() //returns line that represents this data structure
		{ 
			return ("");
		}
		public void loadFromSpec(String ln) {}//fills in params based on ln
		public String getParamTypeName() { return PORTSTATUS_LBL;}
		protected void setDescription()
		{
			if (monitorID > 0)
				setDescription(new String("Port status for input port " + port));
			else 
				setDescription(new String("Port status for output port " + port));
		}
	}

	public static class IPPBandwidth extends Base
	{
		private boolean wRecycled = true;
		public static final String WRECYCLED = "withRecycled";

		public IPPBandwidth(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.WUGSC, IPPBANDWIDTH);
			monitorID = Integer.parseInt(attributes.getValue(ExperimentXML.MONITORID));
			wRecycled = Boolean.valueOf(attributes.getValue(WRECYCLED)).booleanValue();
		}
		public IPPBandwidth(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			super(ONLDaemon.WUGSC, IPPBANDWIDTH, Units.CELLS);
			initFromFile(infile, lv, expcoord);
		}
		public IPPBandwidth(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.WUGSC, IPPBANDWIDTH, Units.CELLS);
			initFromReader(rdr, dt);
		}

		public IPPBandwidth(int p, int mid) 
		{
			super(ONLDaemon.WUGSC, IPPBANDWIDTH, Units.CELLS);
			monitorID = mid;
			port = p;
			if (mid == MonitorEntry.IPPBandwidth.BANDWIDTH) setName("IPPBW " + String.valueOf(p));
			else setName("VCSdis " + String.valueOf(p));
		}

		public IPPBandwidth(int p, String nm)
		{
			this(p, MonitorEntry.IPPBandwidth.BANDWIDTH, nm);
		}

		public IPPBandwidth(int p, int mid, String nm)
		{
			super(ONLDaemon.WUGSC, IPPBANDWIDTH, nm, Units.CELLS);
			port = p;
			monitorID = mid;
		}
		public String getParamTypeName() { return IPPBANDWIDTH_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.MONITORID, String.valueOf(monitorID));
			xmlWrtr.writeAttribute(WRECYCLED, String.valueOf(wRecycled));
		}

		public boolean isWithRecycled() { return wRecycled;}
		public void setWithRecycled(boolean b) 
		{ 
			if (wRecycled != b && b) setName("IPPBW+r " + String.valueOf(port));
			if (wRecycled != b && !b) setName("IPPBW " + String.valueOf(port));
			wRecycled = b;
		}
		public String getSpec() //returns line that represents this data structure
		{ 
			int wr = 1;
			if (!wRecycled) wr = 0;
			return (String.valueOf(monitorID) + " " + String.valueOf(wr));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			String str_array[] = ln.split(" ");
			monitorID = Integer.parseInt(str_array[0]);
			wRecycled = (Integer.parseInt(str_array[1]) > 0);
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			super.initFromReader(reader, dt);
			monitorID = reader.readInt();
			wRecycled = reader.readBoolean();
			ExpCoordinator.printer.print(new String("     monitorID = " + getMonitorID()), 2);
		}
		public void write(ONL.Writer writer) throws IOException
		{
			super.write(writer);
			writer.writeInt(monitorID);
			writer.writeBoolean(wRecycled);
		}
		protected void setDescription()
		{
			String tmp = "";
			if (monitorID == MonitorEntry.IPPBandwidth.BANDWIDTH)
			{
				tmp = "Input port cell count";
				if (wRecycled)
					tmp = tmp.concat(" including recycled cells");
			}
			else 
				tmp = "VXTCS0 Discard";
			setDescription(tmp);
		}

		public boolean isEqual(MonitorDataType.Base mdt)
		{
			if (mdt instanceof IPPBandwidth)
				return (super.isEqual(mdt) && ((IPPBandwidth)mdt).wRecycled == wRecycled);
			else
				return false;
		}
	}


	public static class VXIBandwidth extends Base
	{
		protected int vxi = 0;
		public static final String VXI = "vxi";
		public static final String VCI = "vci";
		public static final String VPI = "vpi";

		public VXIBandwidth(String uri, Attributes attributes, int t)
		{
			super(uri, attributes, ONLDaemon.WUGSC, t);
			if (t == VPIBANDWIDTH) vxi = Integer.parseInt(attributes.getValue(VPI));
			else vxi = Integer.parseInt(attributes.getValue(VCI));
		}
		public VXIBandwidth(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord, int tp) throws IOException
		{
			super(ONLDaemon.WUGSC, tp, Units.CELLS);
			initFromFile(infile, lv, expcoord);
		}
		public VXIBandwidth(ONL.Reader rdr, int dt, int tp) throws IOException
		{
			super(ONLDaemon.WUGSC, tp, Units.CELLS);
			initFromReader(rdr, dt);
		}

		public VXIBandwidth(int p, int v, String nm, int tp)
		{
			super(ONLDaemon.WUGSC, tp, nm, Units.CELLS);
			port = p;
			vxi = v;
		}

		public boolean isEqual(MonitorDataType.Base mdt)
		{
			if (mdt instanceof VXIBandwidth)
				return (super.isEqual(mdt) && ((VXIBandwidth)mdt).vxi == vxi);
			else
				return false;
		}
		public int getMonitorID() 
		{ 
			return vxi;
		}
		/*
    public double convertData(double data) 
      { 
        if (isPeriodic()) return (data/1000000);
        else return data;
        } //override for any conversion*/
		public String getParamTypeName() { return VPIBANDWIDTH_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			int vpi = vxi; 
			int vci = 0;
			if (paramType != VPIBANDWIDTH) 
			{
				vpi = 0; 
				vci = vxi;
			}
			xmlWrtr.writeAttribute(VPI, String.valueOf(vpi));
			xmlWrtr.writeAttribute(VCI, String.valueOf(vci));
		}
		public String getSpec() //returns line that represents this data structure
		{
			if (paramType == VPIBANDWIDTH)
				return (String.valueOf(vxi) + " " + String.valueOf(0));
			else
				return (String.valueOf(0) + " " + String.valueOf(vxi));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			String str_array[] = ln.split(" ");
			int vpi = Integer.parseInt(str_array[0]);
			int vci = Integer.parseInt(str_array[1]);
			if (paramType == VPIBANDWIDTH) vxi = vpi;
			else vxi = vci;
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			super.initFromReader(reader, dt);
			vxi = reader.readInt();
		}
		public void write(ONL.Writer writer) throws IOException
		{
			super.write(writer);
			writer.writeInt(vxi);
		}
		protected void setDescription()
		{
			if (paramType == VPIBANDWIDTH)
				setDescription(new String("VPI cell count for vpi " + vxi));
			else 
				setDescription(new String("VCI cell count for vci " + vxi));
		}
	}

	public static class VPIBandwidth extends VXIBandwidth
	{
		public VPIBandwidth(String uri, Attributes attributes)
		{
			super(uri, attributes, VPIBANDWIDTH);
		}
		public VPIBandwidth(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			super(infile, lv, expcoord, VPIBANDWIDTH);
		}
		public VPIBandwidth(ONL.Reader reader, int dt) throws IOException
		{
			super(reader, dt, VPIBANDWIDTH);
		}

		public VPIBandwidth(int p, int v, String nm)
		{
			super(p, v, nm, VPIBANDWIDTH);
		}

		public VPIBandwidth(int p, int v)
		{
			super(p, v, (new String("IPP " + String.valueOf(p) + " vpi " + String.valueOf(v))), VPIBANDWIDTH);
		}
		public String getParamTypeName() { return VPIBANDWIDTH_LBL;}
	}

	public static class VCIBandwidth extends VXIBandwidth
	{
		public VCIBandwidth(String uri, Attributes attributes)
		{
			super(uri, attributes, VCIBANDWIDTH);
		}
		public VCIBandwidth(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			super(infile, lv, expcoord, VCIBANDWIDTH);
		}

		public VCIBandwidth(ONL.Reader reader, int dt) throws IOException
		{
			super(reader, dt, VCIBANDWIDTH);
		}

		public VCIBandwidth(int p, int v, String nm)
		{
			super(p, v, nm, VCIBANDWIDTH);
		}

		public VCIBandwidth(int p, int v)
		{
			super(p, v, (new String("IPP " + String.valueOf(p) + " BW to OPP " + String.valueOf(v))), VCIBANDWIDTH);
		}
		public String getParamTypeName() { return VCIBANDWIDTH_LBL;}
		protected void setDescription()
		{
			setDescription(new String("Cell Count from port " + port + " to port " + vxi));
		}

		public int getMonitorID() 
		{ 
			return (vxi+64);
		}
	}


	public static class OPPBandwidth extends Base
	{
		private boolean wRecycled = true;
		public static final String WRECYCLED = "withRecycled";
		public OPPBandwidth(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.WUGSC, OPPBANDWIDTH);
			wRecycled = Boolean.valueOf(attributes.getValue(WRECYCLED)).booleanValue();
		}
		public OPPBandwidth(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			super(ONLDaemon.WUGSC, OPPBANDWIDTH, Units.CELLS);
			initFromFile(infile, lv, expcoord);
		}

		public OPPBandwidth(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.WUGSC, OPPBANDWIDTH, Units.CELLS);
			initFromReader(rdr, dt);
		}

		public OPPBandwidth(int p) 
		{
			super(ONLDaemon.WUGSC, OPPBANDWIDTH, ("OPP " + String.valueOf(p)), Units.CELLS);
			port = p;	
		}

		public boolean isIPP() { return false;}


		public boolean isWithRecycled() { return wRecycled;}
		public void setWithRecycled(boolean b) 
		{ 
			if (wRecycled != b )
			{
				if (!b) setName("OPPBW " + String.valueOf(port));
				else setName("OPPBW+r " + String.valueOf(port));
			}
			wRecycled = b;
		}

		public String getSpec() //returns line that represents this data structure
		{ 
			int wr = 1;
			if (!wRecycled) wr = 0;
			return (String.valueOf(wr));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			wRecycled = (Integer.parseInt(ln) > 0);
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			super.initFromReader(reader, dt);
			wRecycled = reader.readBoolean();
		}
		public void write(ONL.Writer writer) throws IOException
		{
			super.write(writer);
			writer.writeBoolean(wRecycled);
		}
		public String getParamTypeName() { return OPPBANDWIDTH_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(WRECYCLED, String.valueOf(wRecycled));
		}
		/*
    public double convertData(double data)
      { 
        if (isPeriodic()) return (data/1000000);
        else return data;
      } //override for any conversion
		 */
		public int getMonitorID() { return 0;}
		protected void setDescription()
		{
			if (wRecycled)
				setDescription(new String("Port cell count including recycled cells"));
			else
				setDescription(new String("Port cell count"));
		}

		public boolean isEqual(MonitorDataType.Base mdt)
		{
			if (mdt instanceof OPPBandwidth)
				return (super.isEqual(mdt) && ((OPPBandwidth)mdt).wRecycled == wRecycled);
			else
				return false;
		}
	}


	public static class OPPDiscard extends Base
	{
		public OPPDiscard(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.WUGSC, OPPDISCARD);
			monitorID = Integer.parseInt(attributes.getValue(ExperimentXML.MONITORID));
		}
		public OPPDiscard(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			super(ONLDaemon.WUGSC, OPPDISCARD, Units.CELLS);
			initFromFile(infile, lv, expcoord);
		}

		public OPPDiscard(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.WUGSC, OPPDISCARD, Units.CELLS);
			initFromReader(rdr, dt);
		}

		public OPPDiscard(int p, int mid) 
		{
			super(ONLDaemon.WUGSC, OPPDISCARD, Units.CELLS);
			port = p;		
			monitorID = mid;
			switch (mid)
			{
			case MonitorEntry.OPPDiscard.XMBCS0:
				setName("XMBCS0 " + String.valueOf(p));
				break;
			case MonitorEntry.OPPDiscard.XMBCS1:
				setName("XMBCS1 " + String.valueOf(p));
				break;
			case MonitorEntry.OPPDiscard.TOOLATE:
				setName("LATE " + String.valueOf(p));
				break;
			case MonitorEntry.OPPDiscard.RESEQUENCER:
				setName("RSQ " + String.valueOf(p));
				break;
			default: //total discard
				setName("OPPD " + String.valueOf(p));
			}
		}

		public boolean isIPP() { return false;}

		public String getSpec() //returns line that represents this data structure
		{ 
			return (String.valueOf(monitorID));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			monitorID = Integer.parseInt(ln);
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			super.initFromReader(reader, dt);
			monitorID = reader.readInt();
		}
		public void write(ONL.Writer writer) throws IOException
		{
			super.write(writer);
			writer.writeInt(monitorID);
		}
		public String getParamTypeName() { return OPPDISCARD_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.MONITORID, String.valueOf(monitorID));
		}
		protected void setDescription()
		{
			String rtn;
			switch (monitorID)
			{
			case MonitorEntry.OPPDiscard.XMBCS0:
				rtn = new String("XMBCS0 Discard");
				break;
			case MonitorEntry.OPPDiscard.XMBCS1:
				rtn = new String("XMBCS1 Discard");
				break;
			case MonitorEntry.OPPDiscard.TOOLATE:
				rtn = new String("LATE Discard");
				break;
			case MonitorEntry.OPPDiscard.RESEQUENCER:
				rtn = new String("RESEQUENCER Discard");
				break;
			default: //total discard
				rtn = new String("Total Discard");
			}
			setDescription(rtn);
		}
	}


	public static class IPPDiscard extends Base
	{
		public IPPDiscard(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.WUGSC, IPPDISCARD);
			monitorID = Integer.parseInt(attributes.getValue(ExperimentXML.MONITORID));
		}
		public IPPDiscard(SpecFile.SpecReader infile, int lv, ExpCoordinator expcoord) throws IOException
		{
			super(ONLDaemon.WUGSC, IPPDISCARD, Units.CELLS);
			initFromFile(infile, lv, expcoord);
		}

		public IPPDiscard(ONL.Reader rdr, int dt) throws IOException
		{
			super(ONLDaemon.WUGSC, IPPDISCARD, Units.CELLS);
			initFromReader(rdr, dt);
		}

		public IPPDiscard(int p, int mid) 
		{
			super(ONLDaemon.WUGSC, IPPDISCARD, Units.CELLS);
			port = p;	
			monitorID = mid;
			switch (mid)
			{
			case MonitorEntry.IPPDiscard.RCBCLP0:
				setName("RCBCLP0 " + String.valueOf(p));
				break;
			case MonitorEntry.IPPDiscard.RCBCLP1:
				setName("RCBCLP1 " + String.valueOf(p));
				break;
			case MonitorEntry.IPPDiscard.CYCB:
				setName("CYCB " + String.valueOf(p));
				break;
			case MonitorEntry.IPPDiscard.BADHEC:
				setName("BADHEC " + String.valueOf(p));
				break;
			default: //total discard
				setName("IPPD " + String.valueOf(p));
			}
		}

		public String getSpec() //returns line that represents this data structure
		{ 
			return (String.valueOf(monitorID));
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{
			monitorID = Integer.parseInt(ln);
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			super.initFromReader(reader, dt);
			monitorID = reader.readInt();
		}
		public void write(ONL.Writer writer) throws IOException
		{
			super.write(writer);
			writer.writeInt(monitorID);
		}
		public String getParamTypeName() { return IPPDISCARD_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.MONITORID, String.valueOf(monitorID));
		}
		protected void setDescription()
		{
			String rtn;
			switch (monitorID)
			{
			case MonitorEntry.IPPDiscard.RCBCLP0:
				rtn = new String("Discard RCBCLP0");
				break;
			case MonitorEntry.IPPDiscard.RCBCLP1:
				rtn = new String("Discard RCBCLP1");
				break;
			case MonitorEntry.IPPDiscard.CYCB:
				rtn = new String("Discard CYCB");
				break;
			case MonitorEntry.IPPDiscard.BADHEC:
				rtn = new String("Discard BADHEC");
				break;
			default: //total discard
				rtn = new String("Discard IPPD");
			}
			setDescription(rtn);
		}
	}

	public static class LogFile extends Base
	{
		public static final int RELATIVE_REPLAY = 0;
		public static final int EXACT_REPLAY = 1;
		public static final String LOG_TYPE = "logType";
		protected String fileName = null;
		protected MonitorDataType.Base loggedDataType = null;
		protected int replayType = RELATIVE_REPLAY;

		public LogFile(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.UNKNOWN, Integer.parseInt(attributes.getValue(LOG_TYPE)));
			fileName = attributes.getValue(ExperimentXML.FILE);
		}
		public LogFile(ONL.Reader rdr, int tp) throws IOException
		{
			super(ONLDaemon.UNKNOWN, tp, Units.UNKNOWN);
			ExpCoordinator.printer.print(new String("LogFile"), 2);
			initFromReader(rdr, LOG_FILE);
			//loggedDataType = MonitorDataType.Base.loadFromFile(infile, null);
		}
		public LogFile(SpecFile.SpecReader infile, int tp) throws IOException
		{
			super(ONLDaemon.UNKNOWN, tp, Units.UNKNOWN);
			ExpCoordinator.printer.print(new String("LogFile"), 2);
			initFromFile(infile, LOG_FILE, null);
			dataType = LOG_FILE;
			loggedDataType = MonitorDataType.Base.loadFromFile(infile, null);
		}
		public LogFile(String fnm, MonitorDataType.Base mdt)
		{
			super(ONLDaemon.UNKNOWN, UNKNOWN, mdt.getDataUnits());
			name = mdt.getName();
			setPollingRate(mdt.getPollingRate());
			fileName = fnm;
			loggedDataType = mdt;
			dataType = LOG_FILE;
			setIsRate(false);
		}
		public String getSpec() //returns line that represents this data structure
		{
			ExpCoordinator.printer.print(new String("MDT.LogFile::getSpec for " + name + " monitorID = " + monitorID + " file = " + fileName), 10);
			if (fileName != null)
				return fileName;
			else return (new String("unknown"));
		}
		public String getParamTypeName() { return LOGFILE_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.FILE, fileName);
			xmlWrtr.writeAttribute(LOG_TYPE, String.valueOf(paramType));
		}
		public boolean isEqual(MonitorDataType.Base mdt)
		{
			if (mdt instanceof LogFile)
			{
				if (super.isEqual(mdt) && 
						fileName.equals(((LogFile)mdt).fileName) && 
						loggedDataType.isEqual(((LogFile)mdt).loggedDataType))
					return true;
				else return false;
			}
			else return false;
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{ 
			fileName = ln;
			ExpCoordinator.printer.print(new String("MDT.LogFile::loadFromSpec fileName = " + fileName), 10);
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			super.initFromReader(reader, dt);
			monitorID = reader.readInt();
			fileName = reader.readString();
			ExpCoordinator.printer.print(new String("MDT.LogFile.initFromReader fileName = " + fileName), 10);
			loggedDataType = MonitorDataType.Base.read(reader);
		}
		public void write(ONL.Writer writer) throws IOException
		{
			super.write(writer);
			writer.writeInt(monitorID);
			ExpCoordinator.printer.print(new String("MDT.LogFile.write for " + name + " monitorID = " + monitorID + " file = " + fileName), 10);
			if (fileName != null)
				writer.writeString(fileName);
			else writer.writeString("unknown");
			loggedDataType.write(writer);
		}
		public String getFileName() { return fileName;}
		public void setFileName(String fn) { fileName = fn;}
		public MonitorDataType.Base getLogDataType() { return loggedDataType;}
		public void saveToLogFile(ONL.Writer wrtr) throws IOException
		{
			loggedDataType.write(wrtr);
		}
		public void saveToFile(SpecFile.SpecWriter outfile) throws IOException
		{
			super.saveToFile(outfile);
			loggedDataType.saveToFile(outfile);
		}
		public void setReplayType(int v) { replayType = v;}
		public boolean isExactReplay() { return (replayType == EXACT_REPLAY);}
		public boolean isRelReplay() { return (replayType == RELATIVE_REPLAY);}
		public int getDataUnits() { return (loggedDataType.getDataUnits());} 
		protected void setDescription(){}
		public String getDescription()
		{
			if (loggedDataType != null) return (new String("Log File: " + loggedDataType.getDescription()));
			else return (new String("Log File"));
		}
		public boolean canBeRate() { return false;}
	}

	public static class MFormula extends Base
	{
		//public static final int LFSFLOW = 2;
		public static final String START_FORMULA = "startFormula";
		private int numParams = 0;
		private MonitorAlgFunction mfunction = null;
		private String formula = null;
		private MonitorAlgFunction.Formula formulaObject = null;

		////////////////////////// MonitorDataType.MFormula.XMLHandler /////////////////////////////////////////
		private class XMLHandler extends MonitorDataType.Base.XMLHandler
		{
			public XMLHandler(ExperimentXML exp_xml, MonitorDataType.MFormula mdt)
			{
				super(exp_xml, mdt);
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	  
				super.startElement(uri, localName, qName, attributes);
				if (localName.equals(START_FORMULA))
				{
					((MFormula)mdataType).formula = new String(attributes.getValue(ExperimentXML.VALUE));
				}
				if (localName.equals(ExperimentXML.FORMULA))
				{
					formulaObject = new MonitorAlgFunction.Formula(uri, attributes);
					expXML.setContentHandler(formulaObject.getContentHandler(expXML));
				}
			}
		}//end MonitorDataType.MFormula.XMLHandler

		public  MFormula(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.UNKNOWN, FORMULA);
			numParams = Integer.parseInt(attributes.getValue(ExperimentXML.NUMPARAMS));
		}
		public  MFormula(SpecFile.SpecReader infile) throws IOException
		{
			super(ONLDaemon.UNKNOWN, FORMULA, Units.UNKNOWN);
			initFromFile(infile, MONITOR_FUNCTION, null);
		}

		public  MFormula(ONL.Reader rdr) throws IOException
		{
			super(ONLDaemon.UNKNOWN, FORMULA, Units.UNKNOWN);
			initFromReader(rdr, MONITOR_FUNCTION);
		}

		public MFormula(String nm, int nparams, int du)
		{
			this(nm, nparams, du, null);
		}

		public MFormula(String nm, int nparams, int du, String form_str)
		{
			super(ONLDaemon.UNKNOWN, FORMULA, nm, du);
			numParams = nparams;
			mfunction = null;
			dataType = MONITOR_FUNCTION;
			setFormula(form_str);
			setIsRate(false);
		}
		public String getParamTypeName() { return FORMULA_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.NUMPARAMS, String.valueOf(numParams));
		}
		public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(START_FORMULA);
			xmlWrtr.writeAttribute(ExperimentXML.VALUE, formula);
			mfunction.getFormula().writeXML(xmlWrtr);
			xmlWrtr.writeEndElement();//end formula
		}
		public void loadFromSpec(String ln)//fills in params based on ln
		{ 	
			String str_array[] = ln.split(" ");
			numParams = Integer.parseInt(str_array[0]);
			dataUnits = Integer.parseInt(str_array[1]);
		}
		public void write(ONL.Writer writer) throws IOException
		{
			super.write(writer);
			writer.writeInt(dataUnits);
			writer.writeString(formula);
			mfunction.getFormula().write(writer);
		}
		protected void initFromReader(ONL.Reader reader, int dt) throws IOException
		{
			super.initFromReader(reader, dt);
			dataUnits = reader.readInt();
			formula = reader.readString();
			formulaObject = new MonitorAlgFunction.Formula(reader);
			numParams = formulaObject.size();
		}

		public String getSpec() //returns line that represents this data structugre
		{return (new String(numParams + " " + dataUnits));}

		public void saveToFile(SpecFile.SpecWriter outfile) throws IOException
		{
			super.saveToFile(outfile);
			if (mfunction != null) mfunction.getSpec().writeParams(outfile);
			else 
			{
				outfile.writeLine("0");
			}
		}
		public void saveToLogFile(ONL.Writer outfile) throws IOException
		{
			super.saveToLogFile(outfile);
		}
		public int getNumParams() { return numParams;}
		public MonitorAlgFunction getMFunction() { return mfunction;}
		public void setMFunction(MonitorAlgFunction mf) { mfunction = mf;}
		protected void setDescription()
		{
			if (formula != null)
				setDescription(new String("Formula: " + formula));
			else setDescription(new String("Formula"));
		}
		public String getFormula() { return formula;}
		public void setFormula(String s) 
		{ 
			formula = s;
			setDescription();
		}
		public boolean canBeRate() { return false;}
		public MonitorAlgFunction.Formula getFormulaObj() { return formulaObject;}
		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new MonitorDataType.MFormula.XMLHandler(exp_xml, this));}
	} //end class MonitorFunction

	public static class Constant extends Base
	{
		private double k = 0;
		public Constant(String uri, Attributes attributes)
		{
			super(uri, attributes, ONLDaemon.UNKNOWN, CONSTANT);
			k = Double.parseDouble(attributes.getValue(ExperimentXML.VALUE));
		}
		public Constant(SpecFile.SpecReader infile) throws IOException
		{
			super(ONLDaemon.UNKNOWN, CONSTANT, Units.UNKNOWN);
			initFromFile(infile, CONSTANT, null);
		}
		public Constant(ONL.Reader rdr) throws IOException
		{
			super(ONLDaemon.UNKNOWN, CONSTANT, Units.UNKNOWN);
			initFromReader(rdr, CONSTANT);
		}
		public Constant(double val)
		{
			super(ONLDaemon.UNKNOWN, CONSTANT, new String("K=" + val), Units.UNKNOWN);
			k = val;
			dataType = CONSTANT;
			setIsRate(false);
		}
		protected void setDescription(){}
		public String getDescription()
		{
			return (new String("Constant K=" + k));
		}    
		public String getSpec() { return ("");}//returns line that represents this data structure
		public void loadFromSpec(String ln) {}//fills in params based on ln
		public boolean canBeRate() { return false;}

		public void initFromReader(ONL.Reader rdr, int dt) throws IOException
		{
			super.initFromReader(rdr, dt);
			k = rdr.readDouble();
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			super.write(wrtr);
			wrtr.writeDouble(k);
		}
		public String getParamTypeName() { return CONSTANT_LBL;}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeAttribute(ExperimentXML.VALUE, String.valueOf(k));
		}
		public double getConstant() { return k;}
	}
}
