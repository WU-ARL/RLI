import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
//import java.io.*;
//import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////// CommandSpec //////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

public class CommandSpec
{
	public static final String INT_STR = "int";
	public static final String DOUBLE_STR = "double";
	public static final String STRING_STR = "string";
	public static final String BOOL_STR = "boolean";
	public static final String NEXTHOP_STR = "nexthop";
	public static final String NEXTHOPIP_STR = "nexthopip";
	public static final String IPADDR_STR = "ipaddress";
	public static final String FIELD_STR = "field";
	public static final String COMMAND_STR = "COMMAND";
	public static final String CFG_STR = "CONFIGURE";
	public static final String PORT_STR = "PORT";
	public static final String MONITOR_STR = "MONITOR";
	public static final String REBOOT_STR = "REBOOT";
	public static final String INIT_STR = "INIT";
	public static final int INT = 1;
	public static final int DOUBLE = 2;
	public static final int BOOL = 3;
	public static final int STRING = 4;
	public static final int NEXTHOP = 5;
	public static final int IPADDR = 6;
	public static final int UNKNOWN = 0;
	public static final int COMMAND = 1;
	public static final int MONITOR = 2;
	public static final int PERPORT = 4;
	public static final int REBOOT = 8;
	public static final int INIT = 16;
	public static final int CFG_COMMAND = 32;
	public static final int SUBTYPE_INIT = 64;
	protected int commandType = UNKNOWN;
	protected String displayLabel = "";
	protected int opcode = 0;
	protected String description = "";
	protected int units = Units.UNKNOWN;
	protected int numParams = 0;
	private boolean immediate = false;
	private boolean is_positive = true; //for monitoring operations, is true if rate should always be positive. 
										//if rate becomes negative it will assume a value has wrapped
	//protected Param[] paramArray = null;
	protected Vector<ParamSpec> params = null;
	protected Vector displayGroups = null;
	//protected boolean subtype = false;

	///////////////////////////////////////////// CommandSpec.DisplayGroup ////////////////////////////////////////////////

	protected static class DisplayGroup extends Vector
	{
		protected String label = null;
		public DisplayGroup() { super();}
		public DisplayGroup(String nm) { super(); label = new String(nm);}
		public DisplayGroup(DisplayGroup dg) { super((Vector)dg); label = new String(dg.label);}
	}

	/////////////////////////////////////////////////////// CommandSpec.XMLHandler ///////////////////////////////////////////////////////

	protected static class XMLHandler extends DefaultHandler
	{
		protected ContentHandler hwhandler = null;
		protected XMLReader xmlReader = null;
		protected String currentElement = "";
		//protected ParamSpec currentParam = null;
		protected int paramIndex = 0;
		protected CommandSpec cspec = null;
		protected DisplayGroup dGroup = null;

		public XMLHandler(XMLReader xmlr, ContentHandler hwh, CommandSpec cs)
		{
			super();
			xmlReader = xmlr;
			hwhandler = hwh;
			cspec = cs;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			ExpCoordinator.print(new String("CommandSpec.XMLHandler.startElement localName:" + localName), ExperimentXML.TEST_XML);
			currentElement = new String(localName);
			if (localName.equals(ExperimentXML.GROUP))
			{
				String nm = attributes.getValue(ExperimentXML.LABEL);
				if (nm != null) dGroup = new DisplayGroup(nm);
				else dGroup = new DisplayGroup();
			}
			if (localName.equals(ExperimentXML.PARAM) && paramIndex < cspec.numParams) 
			{
				ParamSpec param = new ParamSpec(uri, attributes);
				param.setCommandSpec(cspec);
				cspec.setParam(param, paramIndex);
				if (dGroup != null) dGroup.add(new Integer(paramIndex));
				else cspec.addDisplayGroup(new Integer(paramIndex));
				++paramIndex;
				xmlReader.setContentHandler(param.getXMLHandler(xmlReader, this));
			}
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("CommandSpec.XMLHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
			if (currentElement.equals(ExperimentXML.DLABEL)) cspec.displayLabel = new String(ch, start, length);
			if (currentElement.equals(ExperimentXML.DESCRIPTION)) cspec.description = new String(ch, start, length);
			if (currentElement.equals(ExperimentXML.UNITS)) 
			{
				cspec.units = Units.getType(new String(ch, start, length));
				ExpCoordinator.print(new String("   units:" + cspec.units), ExperimentXML.TEST_XML);
			}
			/*if (currentElement.equals(ExperimentXML.PLABEL) && currentParam != null) currentParam.label = new String(ch, start, length);
      if (currentElement.equals(ExperimentXML.PTYPE) && currentParam != null) currentParam.type = new String(ch, start, length);
      if (currentElement.equals(ExperimentXML.PDEFAULT) && currentParam != null) currentParam.setDefaultFromString(new String(ch, start, length));*/
		}
		public void endElement(String uri, String localName, String qName)
		{
			ExpCoordinator.print(new String("CommandSpec.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(cspec.getXMLElemName()))//ExperimentXML.COMMAND) || localName.equals(ExperimentXML.UPDATE_COMMAND))
			{
				cspec.print(ExperimentXML.TEST_XML);
				cspec = null;
				xmlReader.setContentHandler(hwhandler);
			}
			if (localName.equals(ExperimentXML.GROUP) && dGroup != null) 
			{
				cspec.addDisplayGroup(dGroup);
				dGroup = null;
			}
		}
	}// end inner class CommandSpec.XMLHandler


	////////////////////////////////////////////// CommandSpec methods /////////////////////////////////////////////////////////////////////////
	public CommandSpec() { params = new Vector<ParamSpec>(); displayGroups = new Vector();}
	//public CommandSpec(String uri, Attributes attributes) { this(uri, attributes, COMMAND);}
	public CommandSpec(String uri, Attributes attributes, int cmd_tp)
	{
		/*
      String tp = attributes.getValue(ExperimentXML.COMMANDTYPE);
      if (tp.equalsIgnoreCase(REBOOT_STR)) commandType = REBOOT;
      if (tp.equalsIgnoreCase(INIT_STR)) commandType = INIT;
      if (tp.equalsIgnoreCase(MONITOR_STR)) commandType = MONITOR;
      if (tp.equalsIgnoreCase(COMMAND_STR))commandType = COMMAND;
      if (tp.equalsIgnoreCase(CFG_STR))commandType = CFG_COMMAND;
      if (Boolean.valueOf(attributes.getValue(ExperimentXML.PERPORT)).booleanValue())
        {
          commandType |= PERPORT;
        }
		 */
		this();
		commandType = cmd_tp;
		numParams = Integer.parseInt(attributes.getValue(ExperimentXML.NUMPARAMS));
		opcode = Integer.parseInt(attributes.getValue(ExperimentXML.OPCODE));
		immediate = false;
		String tmp = attributes.getValue(ExperimentXML.IMMEDIATE);
		if (tmp != null) immediate = Boolean.valueOf(tmp).booleanValue();
		if (attributes.getValue(ExperimentXML.NEGATIVE_POSSIBLE) != null) 
		{
			is_positive = Boolean.valueOf(attributes.getValue(ExperimentXML.NEGATIVE_POSSIBLE));
		}
	}
	//public CommandSpec(boolean stype) { subtype = stype;}
	public CommandSpec(CommandSpec cspec) { this(cspec, null);}
	public CommandSpec(CommandSpec cspec, Vector defaults) 
	{ 
		//subtype = cspec.subtype;
		params = new Vector<ParamSpec>();
		commandType = cspec.commandType;
		if ((commandType & MONITOR) > 0) units = cspec.units;
		numParams = cspec.numParams;
		description = new String(cspec.description);
		displayLabel = new String(cspec.displayLabel);
		opcode = cspec.opcode;
		immediate = cspec.immediate;
		initParams(cspec, defaults);
		displayGroups = new Vector(cspec.displayGroups);
		is_positive = cspec.is_positive;
	}
	public void initParams(CommandSpec cspec, Vector defaults)//Param[] defaults)
	{
		ParamSpec param;
		for (int i = 0; i < numParams; ++i)
		{
			param = new ParamSpec(cspec.getParam(i));
			param.setCommandSpec(cspec);
			setParam(param, i); 
		}
		if (defaults != null) updateDefaults(defaults);
	}

	public boolean isEqual(CommandSpec cs)
	{
		if (opcode == cs.opcode && commandType == cs.commandType && numParams == cs.numParams)
		{
			for (int i = 0; i < numParams; ++i)
			{
				if (!getParam(i).isEqual(cs.getParam(i))) return false;
			}
			return true;
		}
		return false;
	}
	public int getOpcode() { return opcode;}
	protected void setOpcode(int o) { opcode = o;}
	public int getUnits() { return units;}
	public boolean isRate() { return ((units & Units.PERSEC) > 0);}
	public boolean isMonitor() { return ((commandType & MONITOR) > 0);}
	public boolean isCommand() { return ((commandType & COMMAND) > 0);}
	public boolean isCFGCommand() { return ((commandType & CFG_COMMAND) > 0);}
	public boolean isPerPort() { return ((commandType & PERPORT) > 0);}
	public boolean isReboot() { return (commandType == REBOOT);}
	public boolean isInit() { return (commandType == INIT);}
	public boolean isSubtypeInit() { return (commandType == SUBTYPE_INIT);}
	protected int getType() { return commandType;}
	public boolean isImmediate() { return immediate;}
	public void setImmediate(boolean b) { immediate = b;}
	public String getDescription() { return description;}
	public int getNumParams() { return numParams;}
	public void setParam(ParamSpec p, int ndx) 
	{ 
		params.add(ndx, p);
	}//Array[ndx] = p;}
	public ParamSpec getParam(int ndx) { return ((ParamSpec)params.elementAt(ndx));}
	public Vector getParams() { return params;}
	public void print() { print(0);}
	public void print(int d)
	{
		ExpCoordinator.print(new String("    " +  toString() + "  " + description) , d);
		/*ExpCoordinator.print("    params:", d);
      ParamSpec param;
      int max = params.size();
      for (int i = 0; i < max; ++i)
        {
	  param = getParam(i);
	  ExpCoordinator.print(new String("         " + i + ") " + param.toString()) , d);
        }*/
	}
	public String toString()
	{
		String rtn = new String(displayLabel + ":" + commandType + ":" + opcode + " numParams:" + numParams + " units:" + units + " params(");

		ParamSpec param;
		int max = params.size();
		for (int i = 0; i < max; ++i)
		{
			param = getParam(i);
			if (i == 0) rtn = rtn.concat(new String(param.toString()));
			else rtn = rtn.concat(new String(", " + param.toString()));
		}
		rtn = rtn.concat(")");
		return rtn;
	}

	public String getLabel() { return displayLabel;}
	public void setDefaults(Object[] vals)
	{
		int num_vals = Array.getLength(vals);
		if (num_vals == numParams)
		{
			for (int i = 0; i < numParams; ++i)
			{
				getParam(i).setDefaultValue(vals[i]);
			}
		}
	}
	//will try to update defaults from spec if possible, this is to try and handle subtypes whose version is out of sync with hardware file
	//protected boolean updateDefaults(Param[] pdefaults) 
	protected void updateDefaults(Vector pdefaults) 
	{
		if (pdefaults == null) return;
		int max = pdefaults.size();//Array.getLength(pdefaults);
		int i = 0;
		ParamSpec pelem;
		ParamSpec pcompare = null;
		for (i = 0; i < max; ++i)
		{
			pelem = (ParamSpec)pdefaults.elementAt(i);//[i];
			pcompare = getParam(pelem.getLabel());
			if (pcompare != null)
			{
				pcompare.setDefaultValue(pelem.getDefaultValue());
			}
		}
		if (max != numParams)
		{
			max = numParams;
			for (i = 0; i < max; ++i)
			{
				pelem = getParam(i);//paramArray[i];
				if (pelem.getDefaultValue() == null) pelem.setDefaultValue();
			}
		}
	}

	public Object[] getDefaultValues()
	{
		Object[] rtn = new Object[numParams];
		for (int i = 0; i < numParams; ++i)
		{
			rtn[i] = getParam(i).getDefaultValue();
		}
		return rtn;
	}

	public ParamSpec getParam(String lbl)
	{
		int max = numParams;
		ParamSpec pelem;
		for (int i = 0; i < max; ++i)
		{
			pelem = getParam(i);
			if (pelem.getLabel().equals(lbl)) return (pelem);
		}
		return null;
	}

	//public void setSubtype(boolean b) { subtype = b;}
	public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) { return (new CommandSpec.XMLHandler(xmlr, hwh, this));}
	public Command createCommand(ONLComponent parent)
	{
		return (new Command(this, parent));
	}       
	public Command createCommand(ONLComponent parent, Field.Owner owner)
	{
		return (new Command(this, parent, owner));
	}
	public String getXMLElemName() 
	{
		if (isInit()) return (ExperimentXML.INIT);
		if (isSubtypeInit()) return (ExperimentXML.SUBTYPE_INIT);
		if (isReboot()) return (ExperimentXML.REBOOT);
		return (ExperimentXML.COMMAND);
	}
	public void addDisplayGroup(Object o) { displayGroups.add(o);}
	public boolean isPositive() { return is_positive;}
}//end class CommandSpec

