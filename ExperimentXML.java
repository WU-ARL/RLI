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
import java.util.Stack;
import java.lang.*;
import org.xml.sax.*;
import java.io.*;


public class ExperimentXML extends DefaultHandler
{
	public static final int TEST_XML = 3;


	//public static final String  = "";
	public static final String ADD_ELEM = "addCommand";
	public static final String ANY = "any";
	public static final String ASSIGNER = "assigner";
	public static final String AUX = "aux";
	public static final String BANDWIDTH = "bandwidth";
	public static final String BATCH_COMMAND = "batchCommand";
	public static final String BATCH = "batch";
	public static final String BINDING = "binding";
	public static final String BINDINGS = "bindings";
	public static final String CLASSID = "classID";
	public static final String CLASSNAME = "className";
	public static final String CLUSTER = "cluster";
	public static final String CLUSTERONLY = "clusterOnly";
	public static final String CLUSTERS = "clusters";
	public static final String CLUSTER_REF = ONLComponent.CLUSTER_REF;
	public static final String COLOR = "color";
	public static final String COLUMN = "column";
	public static final String COMMANDS = "commands";
	public static final String COMMAND = "command";
	public static final String COMMAND_LOG = "commandLog";
	public static final String COMMANDTYPE = "commandType";
	public static final String COMPONENT = "component";
	public static final String CONFIGURE = "configure";
	public static final String CORES = "cores";
	public static final String COUNTER = "counter";
	public static final String CPADDR = "cpaddr";
	public static final String CPPORT = "cpport";
	public static final String CTYPE = "componentType";
	public static final String DAEMON = "daemon";
	public static final String DATA_TYPE = "dataType";
	public static final String DELETE_ELEM = "deleteCommand";
	public static final String DESCRIPTION = "description";
	public static final String DESTADDR = "destAddr";
	public static final String DESTPORT = "destPort";
	public static final String DIRECTORY = "directory";
	public static final String DISPLAY = "display";
	public static final String DLABEL = "displayLabel";
	public static final String DROP = "drop";
	public static final String EDITABLE = "editable";
	public static final String ELEMENT = "entry";
	public static final String ELEMNAME = "elementName";
	public static final String ELEM_OPS = "elementOperations";
	public static final String ENABLED = "enabled";
	public static final String ENDPOINT = "endpoint";
	public static final String ENTRYKEY = "entryKey";
	public static final String EXBITS = "exceptionBits";
	public static final String EXMASK = "exceptionMask";
	public static final String EXP = "Experiment";
	public static final String EXPADDR = "expaddr";
	public static final String EXPNAME = "expName";
	public static final String FIELD = "field";
	public static final String FIELDS = "fields";
	public static final String FIELDNAME = "fieldName";
	public static final String FIELDLOCALE = "fieldLocale";
	public static final String FIELDPARAM = "fieldParam";
	public static final String FILE = "file";
	public static final String FILTER = "filter";
	public static final String FILTER_TABLE = "FilterTable";
	public static final String FILTER_TYPE = "ftype";
	public static final String FORMULA = "formula";
	public static final String FORWARD_KEY = "forwardKey";
	public static final String GATEWAY = "gatewayaddr";
	public static final String GENERATED_PW = "generatedPW";
	public static final String GROUP = "group";
	public static final String HARDWARE = "hardware";//ExperimentXML.HWTYPE;
	public static final String HEIGHT = "height";
	public static final String HELP = "help";
	public static final String HOST = "host";
	public static final String HWASSIGN = "hwAssigned";
	public static final String HWREF = ONLComponent.REF_NUM;
	public static final String HWTABLE = "HardwareTable";
	public static final String HWTYPE = "hwType";
	public static final String HWTYPES = "hardwareTypes";
	public static final String ID = "id";
	public static final String IMMEDIATE = "immediate";
	public static final String INDEX = "index";
	public static final String INGRESS = "ingress";
	public static final String INIT = "init";
	public static final String INSTANCEID = "instanceID";
	public static final String INTERFACE = "interface";
	public static final String INTERFACES = "interfaces";
	public static final String INTERFACE_TYPE = "interfaceType";
	public static final String ITYPE_1G = "1G";
	public static final String ITYPE_10G = "10G";
	public static final String KBITS = "Kbits";
	public static final String KBYTES = "Kbytes";
	public static final String LABEL = ONLComponent.LABEL;
	public static final String LINK = "link";
	public static final String LINKS = "links";
	public static final String LOGFILE = "logfile";
	public static final String MASK = "mask";
	public static final String MAX = "max";
	public static final String MBITS = "Mbits";
	public static final String MBYTES = "Mbytes";
	public static final String MEMORY = "memory";
	public static final String MENUOP = "menuOperation";
	public static final String MICRO_ENG = "microengine";
	public static final String MIN = "min";
	public static final String MONITOR = "mcommand";
	public static final String MONITORID = "monitorid";
	public static final String MONITORING = "monitoring";//"Monitoring";
	public static final String MONITORS = "monitorCommands";
	public static final String MWPOSITION = "mainWindowPosition";
	public static final String MWSIZE = "mainWindowSize";
	public static final String NAME = "name";
	public static final String NEGATE = "negate";
	public static final String NEGATIVE_POSSIBLE = "negative_possible";
	public static final String NEWLINE = "newline";
	public static final String NEXTHOP = "nextHop";
	public static final String NO_UPDATE = "no_update";
	public static final String NODE = "node";
	public static final String NODES = "nodes";
	public static final String NUMCOLS = "numColumns";
	public static final String NUMFIELDS = "numFields";
	public static final String NUMINTERFACES = "numInterfaces";
	public static final String NUMPARAMS = "numParams";
	public static final String NUMPORTS = "numPorts";
	public static final String OLDSUBNET = "oldSubnetAllocation";
	public static final String OP = "operation";
	public static final String OPCODE = "opcode";
	public static final String OPTYPE = "optype";
	public static final String OPERATION = "operation";
	public static final String ORIG_LABEL = "orig_label";
	public static final String ORIG_IPADDR = "orig_ipaddr";
	public static final String ORIG_SUBNET = "orig_subnet";
	public static final String OUTPUT_PLUGINS = "outputPlugins";
	public static final String OUTPUT_PORTS = "outputPorts";
	public static final String PACKETS = "packets";
	public static final String PARAM = "param";
	public static final String PARENT = "parent";
	public static final String PBINDING = "pluginBinding";
	public static final String PCHOICE = "pChoice";
	public static final String PCHOICES = "pChoices";
	public static final String PCLASS = "pClass";
	public static final String PCLASSES = "pluginClasses";
	public static final String PDEFAULT = "default";
	public static final String PERPORT = "perport";
	public static final String PINSTANCES = "pluginInstances";
	public static final String PLABEL = "plabel";
	public static final String PLUGIN = "plugin";
	public static final String POINT = "point";
	public static final String POINT1 = "point1";
	public static final String POINT2 = "point2";
	public static final String POLLING_RATE = "pollingRate";
	public static final String PORT = "port";
	public static final String PORTASSIGN = "portAssigned";
	public static final String PORTS = "ports";
	public static final String POSITION = "position";
	public static final String PPS = "pps";
	public static final String PREFIX = "prefix";
	public static final String PREFIX_MASK = "prefixMask";
	public static final String PRIORITY = "priority";
	public static final String PROTOCOL = "protocol";
	public static final String PTAG = "pluginTag";
	public static final String PTYPE = "ptype";
	public static final String QUANTUM = "quantum";
	public static final String QUEUE = "queue";
	public static final String QUEUE_TABLE = "queueTable";
	public static final String QID = "queueID";
	public static final String RATE = "rate";
	public static final String REBOOT = "reboot";
	public static final String RESOURCE = "resource";
	public static final String ROUTE = "route";
	public static final String ROUTER = "router";
	public static final String ROUTE_TABLE = "routeTable";
	public static final String ROUTE_NEXTHOP = "nexthop";
	public static final String ROUTE_NEXTHOP_IP = "nexthopIP";
	public static final String SAMPLING_TYPE = "samplingType";
	public static final String SECS = "secs";
	public static final String SIZE = "size";
	public static final String SPINNER = "spinnerPosition";
	public static final String SRCADDR = "srcAddr";
	public static final String SRCPORT = "srcPort";
	public static final String STATS_INDEX = "statsIndex";
	public static final String SUBHWTYPE = "hwSubtype";
	public static final String SUBNET = "subnet";
	public static final String SUBNET_ID = "subnetID";
	public static final String SUBNET_INDEX = "subnetIndex";
	public static final String SUBTYPE = "subtype";
	public static final String SUBTYPE_INIT = "subtypeInit";
	public static final String SWITCH = "switch";
	public static final String SYMBOL = "symbol";
	public static final String TABLE = "table";
	public static final String TABLEASSIGN = "tableAssigned";
	public static final String TABLE_ID = "tableID";
	public static final String TABLEELEM_ID = "tableElemID";
	public static final String TABLE_OP = "tableOperation";
	public static final String TABLE_OPS = "tableOperations";
	public static final String TABLES = "tables";
	public static final String TCPFLAGS = "tcpFlags";
	public static final String TCPFLAGSMASK = "tcpFlagsMask";
	public static final String THOLD = "threshold";
	public static final String TITLE = "title";
	public static final String TOPO = "Topology";
	//public static final String TYPECODE = "typeCode";
	public static final String TYPE = "type";
	public static final String TYPENAME = "typeName";
	public static final String UNITS = "units";
	public static final String UPDATE_COMMAND = "updateCommand";
	public static final String USECS = "usecs";
	public static final String USER_LABEL = "userLabel";
	public static final String VALUE = "value";
	public static final String VERSION = "version";
	public static final String VINFO = "vtypeInfo";
	public static final String VINTERFACE = "vinterface";
	public static final String VIRTUAL = "virtual";
	public static final String VLINK = "vlink";
	public static final String VPORTS = "virtualPorts";
	public static final String VTOPO = "virtualTopology";
	public static final String VOQ = "voq";
	public static final String WIDTH = "width";
	public static final String WILDCARD = "wildcard";
	public static final String XCOORD = "x-coord";
	public static final String YCOORD = "y-coord";

	private Experiment experiment = null;
	private XMLReader xmlReader = null;
	private String currentElement = "";
	private String sectionName = "";
	private Stack contentHandlers = null;
	private double version = ExpCoordinator.VERSION;
	private java.io.File file = null;
	private boolean monitorOnly = false;


	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////// ONLStringWriter ////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class ONLStringWriter extends java.io.StringWriter implements ONL.Writer
	{
		public ONLStringWriter() { super();}
		public void writeString(String s) throws IOException
		{
			if (s != null)
				append(s);
			//System.out.println("writing string:" + s);
			append('\n');
		}
		public void writeByte(int i)  throws IOException { writeInt(i);}
		public void writeShort(int i) throws IOException { writeInt(i);}
		public void writeInt(int i) throws IOException { writeString(String.valueOf(i));}
		public void writeDouble(double i) throws IOException { writeString(String.valueOf(i));}
		public void writeFloat(float i) throws IOException { writeString(String.valueOf(i));}
		public void writeUnsignedInt(double i) throws IOException { writeInt((int)i);} 
		public void writeLine(String s) throws IOException { writeString(s);}
		public void writeBoolean(boolean b) throws IOException { writeString(String.valueOf(b));}
		public void finish() throws IOException {}
	} //end class ExperimentXML.ONLStringWriter

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public ExperimentXML(Experiment exp, XMLReader rdr, java.io.File f) {this(exp, rdr, f, false);}
	public ExperimentXML(Experiment exp, XMLReader rdr, java.io.File f, boolean monitor_only)
	{
		super();
		monitorOnly = monitor_only;
		experiment = exp;
		xmlReader = rdr;
		contentHandlers = new Stack();
		xmlReader.setContentHandler(this);
		file = f;
		if (!monitorOnly)
		{
			ExpCoordinator.setOldSubnet(false);
			experiment.setFile(f);
		}
	}
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
	{
		if (!monitorOnly)
		{
			if (localName.equals(EXP)) 
			{
				ExpCoordinator.print(new String("ExperimentXML.startElement starting Experiment name: " + attributes.getValue("expName") + " version:" + attributes.getValue("version")), TEST_XML);
				experiment.setLabel(attributes.getValue("expName"));
				//experiment.setVersion(attributes.getValue("version"));
				version = Double.parseDouble(attributes.getValue("version"));
				if (version < 6.5) ExpCoordinator.setOldSubnet(true);
				if (attributes.getValue(OLDSUBNET) != null) ExpCoordinator.setOldSubnet(true);
				if (attributes.getValue(GENERATED_PW) != null) experiment.setGeneratedPW(attributes.getValue(GENERATED_PW));
				currentElement = new String(localName);		      
			}
			else ExpCoordinator.print(new String("ExperimentXML.startElement " + localName), TEST_XML);
			if (localName.equals(MWPOSITION))
			{
				ONLMainWindow mw = ExpCoordinator.theCoordinator.getMainWindow();
				mw.setLocation(Integer.parseInt(attributes.getValue(uri, XCOORD)), Integer.parseInt(attributes.getValue(uri, YCOORD)));
			}
			if (localName.equals(MWSIZE))
			{
				ONLMainWindow mw = ExpCoordinator.theCoordinator.getMainWindow();
				mw.setSize(Integer.parseInt(attributes.getValue(uri, WIDTH)), Integer.parseInt(attributes.getValue(uri, HEIGHT)));
			}
			if (localName.equals(HWTYPES))
			{
				setContentHandler(HardwareSpec.getTheManager().getContentHandler(this));
			}
			if (localName.equals(TOPO))
			{
				currentElement = new String(localName);		      
			}
			
			if (localName.equals(VTOPO))
			{
				currentElement = new String(localName);	
				setContentHandler(experiment.getVirtualTopology().getContentHandler(this));
			}
			 
			if (localName.equals(CLUSTER) && currentElement.equals(TOPO))
			{
				Cluster.Instance cl = new Cluster.Instance(uri, attributes, experiment);
				ExpCoordinator.print(new String("     adding cluster type:" + cl.getTypeLabel() + " index:" + cl.getIndex()), TEST_XML);
			} 
			if (localName.equals(NODE) && currentElement.equals(TOPO)) addNode(uri, localName, qName, attributes);
			if (localName.equals(LINK) && currentElement.equals(TOPO)) addLink(uri, localName, qName, attributes);
		}
		if (localName.equals(MONITORING))
		{
			currentElement = new String(localName);	
			setContentHandler(ExpCoordinator.theCoordinator.getMonitorManager().getContentHandler(this));
		}
		if (localName.equals(BATCH))
			currentElement = new String(localName);
		if (localName.equals(BATCH_COMMAND))
		{
			setContentHandler(new Hardware.BatchCommandHandler(this));
		}
	}
	public void endElement(String uri, String localName, String qName)
	{
		if (localName.equals(TOPO)) 
		{
			//experiment.getTopology().fillClusters(experiment);
			ExpCoordinator.print("ExperimentXML topology loaded");
			experiment.getTopology().print(1);
		}
		if (localName.equals(BATCH)) ExpCoordinator.print("batch file loaded");
	}

	public void addNode(ONLComponent c) 
	{
		String cl_ref = c.getProperty(CLUSTER_REF);
		if (cl_ref != null)
		{
			Topology topo = experiment.getTopology();//ExpCoordinator.theCoordinator.getTopology();
			Cluster.Instance cli = topo.getCluster(Integer.parseInt(cl_ref));
			cli.addHardware(c);
			c.setCluster(cli);
		}
		experiment.addNode(c);
	}
	public void addNode(String uri, String localName, String qName, Attributes attributes) 
	{
		//int type = Integer.parseInt(attributes.getValue(uri, TYPECODE));
		String tp_nm = attributes.getValue(uri, TYPENAME);
		ONLComponent c = null;
		if (tp_nm.equals(ONLComponent.VGIGE_LBL))
		    c = new GigEDescriptor(uri, attributes);
		else
		    {
			HardwareSpec.Subtype st = HardwareSpec.manager.getSubtype(tp_nm);
			if (st != null && st.isHost()) //PROBLEM
			    c = new HardwareHost(uri, attributes);
			else
			    c = new Hardware(uri, attributes);
		    }
		if (c != null) 
		{
			ExpCoordinator.print(new String("ExperimentXML.addNode adding contentHandler for component " + c.getType()), TEST_XML);
			setContentHandler(c.getContentHandler(this));
		}
	}

	public void addLink(ONLComponent c) 
	{
		String cl_ref = c.getProperty(CLUSTER_REF);
		if (cl_ref != null)
		{
			Topology topo = ExpCoordinator.theCoordinator.getTopology();
			Cluster.Instance cli = topo.getCluster(Integer.parseInt(cl_ref));
			cli.addLink((LinkDescriptor)c);
			c.setCluster(cli);
		} 
		ONLComponent.PortBase start_comp = (ONLComponent.PortBase)((LinkDescriptor)c).getPoint1();
		ONLComponent.PortBase end_comp = (ONLComponent.PortBase)((LinkDescriptor)c).getPoint2();
		//SUBNET:add call SubnetManager addLink here
		try {
			if (ExpCoordinator.isOldSubnet()) OldSubnetManager.addLink(start_comp, end_comp);
			else SubnetManager.addLink(start_comp, end_comp);
			experiment.addLink(c);
		}catch(SubnetManager.SubnetException e)
		{
			ExpCoordinator.print(new String("ExperimentXML.addLink failed (" + start_comp.getLabel() + ", " + end_comp.getLabel() + ") -- " + e.getMessage()));
		}
	}
	public void addLink(String uri, String localName, String qName, Attributes attributes) 
	{
		ONLComponent c = new LinkDescriptor(uri, attributes);
		setContentHandler(c.getContentHandler(this));
	}
	public void setContentHandler(ContentHandler ch)
	{
		ExpCoordinator.print(new String("ExperimentXML.setContentHandler #contentHandlers " + contentHandlers.size()), TEST_XML);
		ContentHandler next_ch = ch;
		if (ch == null && !contentHandlers.empty()) 
		{
			next_ch = (ContentHandler)contentHandlers.peek();
			ExpCoordinator.print(new String("ExperimentXML.setContentHandler setting to head of stack " + contentHandlers.size()), TEST_XML);
		}
		else 
		{
			if (next_ch != null)
				ExpCoordinator.print(new String("ExperimentXML.setContentHandler setting to new handler " + contentHandlers.size()), TEST_XML);
		}
		if (ch != null && ch != this && !contentHandlers.contains(ch)) 
		{
			contentHandlers.push(ch);
			ExpCoordinator.print(new String("ExperimentXML.setContentHandler pushing new handler " + contentHandlers.size()), TEST_XML);
		}
		if (next_ch == null) 
		{
			next_ch = this;
			ExpCoordinator.print(new String("ExperimentXML.setContentHandler setting to ExperimentXML " + contentHandlers.size()), TEST_XML);
		}
		xmlReader.setContentHandler(next_ch);
	}
	public void removeContentHandler(ContentHandler ch) 
	{
		ExpCoordinator.print(new String("ExperimentXML.removeContentHandler"), TEST_XML);
		if (contentHandlers.contains(ch)) contentHandlers.remove(ch);
		if (ch == xmlReader.getContentHandler()) setContentHandler(null);
	}
	public double getVersion() { return version;}
	public Experiment getExperiment() { return experiment;}
}
