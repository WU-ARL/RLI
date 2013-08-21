
import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ParamSpec
{
	public static final int TEST_PARAM = 8;
	public static final int TEST_CHOICE = 4;
	public static final int TEST_NEXTHOP = 4;
	public static final int TEST_TOOLTIP = TEST_PARAM;
	public static final int TEST_SETVALUE = TEST_CHOICE;
	public static final int TEST_WILDCARD = 4;
	public static final int TEST_UIREP = 4;

	public static final String WILDCARD_STR = "*";

	int preferredWidth = 100;
	String label = "";
	String type = "";
	boolean editable = true;
	Object defaultValue = null;
	CommandSpec commandSpec = null;
	public String format = null;
	Vector choices = null;
	boolean choiceEditable = false;
	String toolTipText = null;
	Object wildcardValue = null;

	///////////////////////////////////////////////////////////  ParamSpec.XMLHandler ////////////////////////////////////////////////////////////
	protected static class XMLHandler extends DefaultHandler
	{
		protected ContentHandler hwhandler = null;
		protected XMLReader xmlReader = null;
		protected String currentElement = "";
		protected ParamSpec currentParam = null;

		public XMLHandler(XMLReader xmlr, ContentHandler hwh, ParamSpec p)
		{
			super();
			xmlReader = xmlr;
			hwhandler = hwh;
			currentParam = p;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			currentElement = new String(localName);
			if (localName.equals(ExperimentXML.PCHOICES) && currentParam != null) currentParam.choiceEditable = Boolean.valueOf(attributes.getValue(ExperimentXML.EDITABLE)).booleanValue();
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("ParamSpec.XMLHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
			if (currentElement.equals(ExperimentXML.PLABEL) && currentParam != null) currentParam.label = new String(ch, start, length);
			if (currentElement.equals(ExperimentXML.PTYPE) && currentParam != null) currentParam.type = new String(ch, start, length);
			if (currentElement.equals(ExperimentXML.PDEFAULT) && currentParam != null)// &&(!currentParam.isField())) 
			{
				currentParam.setDefaultFromString(new String(ch, start, length));
			} 
			if (currentElement.equals(ExperimentXML.WILDCARD) && currentParam != null) currentParam.setWildcard(new String(ch, start, length));
			if (currentElement.equals(ExperimentXML.PCHOICE) && currentParam != null)// &&(!currentParam.isField())) 
			{
				currentParam.addChoice(new String(ch, start, length));
			}
			if (currentElement.equals(ExperimentXML.HELP) && currentParam != null)// &&(!currentParam.isField())) 
			{
				currentParam.toolTipText = new String(ch, start, length);
			}
		}
		public void endElement(String uri, String localName, String qName)
		{
			ExpCoordinator.print(new String("ParamSpec.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(currentParam.getXMLElemName()))//ExperimentXML.PARAM)) 
			{
				currentParam = null;
				xmlReader.setContentHandler(hwhandler);
			}
		}
	}//end ParamSpec.XMLHandler




	//////////////////////////////////////////// Param.WCObject //////////////////////////////////////////////////////////
	//supports wildcards

	public static interface WCObject
	{
		public Object getCopy();
	}
	//////////////////////////////////////////// Param.WCInteger //////////////////////////////////////////////////////////
	//supports wildcards

	public static class WCInteger implements WCObject
	{
		private int value = 0;
		private boolean wildcard = true;

		public WCInteger() {}
		public WCInteger(int v) { value = v; wildcard = false;}
		public WCInteger(Object v) 
		{ 
			if (v instanceof WCInteger)
			{
				value = ((WCInteger)v).value; 
				wildcard = ((WCInteger)v).wildcard;
			}
			if (v instanceof Integer)
			{
				value = ((Integer)v).intValue();
				wildcard = false;
			}
		}
		public WCInteger(Integer v, boolean wc) { value = v.intValue(); wildcard = wc;}
		public WCInteger(String s) throws java.text.ParseException
		{
			parseString(s);
		}
		public void parseString(String s) throws java.text.ParseException
		{
			ExpCoordinator.print(new String("ParamSpec.WCInteger.parseString " + s), TEST_WILDCARD);
			if (s.equals(WILDCARD_STR)) wildcard = true;
			else
			{
				try 
				{
					value = Integer.parseInt(s);  
					wildcard = false;
				}
				catch (NumberFormatException e)
				{
					throw new java.text.ParseException("ParamSpec.PInteger", 0);
				}
			}
		}
		public String toString() 
		{
			if (wildcard) return WILDCARD_STR;
			else return String.valueOf(value);
		}
		public int intValue()  { return (value);}
		public boolean isWildCard() { return (wildcard);}
		public Object getCopy() { return (new WCInteger(this));}
	}//end class Param.WCInteger



	//////////////////////////////////////////// Param.WCDouble //////////////////////////////////////////////////////////
	//supports wildcards

	public static class WCDouble implements WCObject
	{
		private double value = 0;
		private boolean wildcard = true;

		public WCDouble() {}
		public WCDouble(double v) { value = v; wildcard = false;}
		public WCDouble(Object v) 
		{ 
			if (v instanceof WCDouble)
			{
				value = ((WCDouble)v).value; 
				wildcard = ((WCDouble)v).wildcard;
			}
			if (v instanceof Double)
			{
				value = ((Double)v).doubleValue();
				wildcard = false;
			}
		}
		public WCDouble(Double v, boolean wc) { value = v.doubleValue(); wildcard = wc;}
		public WCDouble(String s) throws java.text.ParseException
		{
			parseString(s);
		}
		public void parseString(String s) throws java.text.ParseException
		{
			if (s.equals(WILDCARD_STR)) wildcard = true;
			else
			{
				try 
				{
					value = Double.parseDouble(s);  
					wildcard = false;
				}
				catch (NumberFormatException e)
				{
					throw new java.text.ParseException("ParamSpec.PDouble", 0);
				}
			}
		}
		public String toString() 
		{
			if (wildcard) return WILDCARD_STR;
			else return String.valueOf(value);
		}
		public double doubleValue() { return (value);}
		public boolean isWildCard() { return (wildcard);}
		public Object getCopy() { return (new WCDouble(this));}
	}//end class ParamSpec.WCDouble



	////////////////////////////////////////////// ParamSpec methods //////////////////////////////////////////////////////////  
	//public ParamSpec(){}
	public ParamSpec(String uri, Attributes attributes)
	{
		if (attributes.getValue(ExperimentXML.EDITABLE) != null)
			editable = Boolean.valueOf(attributes.getValue(ExperimentXML.EDITABLE)).booleanValue();
		if (attributes.getValue(ExperimentXML.WIDTH) != null)
			preferredWidth = Integer.parseInt(attributes.getValue(ExperimentXML.WIDTH));
	}
	public ParamSpec(String lbl, String tp)
	{
		label = new String(lbl);
		type = new String(tp);
	}
	public ParamSpec(ParamSpec p) 
	{
		this(p.label, p.type);
		setDefaultValue(p.defaultValue);
		editable = p.isEditable();
		commandSpec = p.commandSpec;
		if (p.choices != null) 
		{
			choices = new Vector(p.choices);
			choiceEditable = p.choiceEditable;
			ExpCoordinator.print(new String("ParamSpec(" + p.label + ") p.choices:" + p.choices.size() + " choices:" + choices.size()), TEST_CHOICE);
		}
		if (p.toolTipText != null) toolTipText = new String(p.toolTipText);
		if (p.wildcardValue != null) wildcardValue = ((WCObject)p.wildcardValue).getCopy();
		preferredWidth = p.preferredWidth;
	}
	public ParamSpec(ParamSpec p, CommandSpec cs)
	{
		this(p);
		commandSpec = cs;
	}
	public ParamSpec(FieldSpec p, CommandSpec cs)
	{
		this(p.label, CommandSpec.FIELD_STR);
		commandSpec = cs;
	}

	public boolean isInt() { return (type.equalsIgnoreCase(CommandSpec.INT_STR));}
	public boolean isString() { return (type.equalsIgnoreCase(CommandSpec.STRING_STR));}
	public boolean isBool() { return (type.equalsIgnoreCase(CommandSpec.BOOL_STR));}
	public boolean isDouble() { return (type.equalsIgnoreCase(CommandSpec.DOUBLE_STR));}
	public boolean isIPAddr() { return (type.equalsIgnoreCase(CommandSpec.IPADDR_STR));}
	public boolean isNextHop() { return (type.equalsIgnoreCase(CommandSpec.NEXTHOP_STR) || type.equalsIgnoreCase(CommandSpec.NEXTHOPIP_STR));}
	public boolean isNextHopIP() { return (type.equalsIgnoreCase(CommandSpec.NEXTHOPIP_STR));}
	public boolean isField() { return (type.equalsIgnoreCase(CommandSpec.FIELD_STR));}
	public String getLabel() { return label;}
	public String getType() { return type;}
	public void addChoice(String s)
	{
		if (choices == null) choices = new Vector();
		ExpCoordinator.print(new String("ParamSpec(" + label + ").addChoice(" + s + ") choices:" + choices.size()), TEST_CHOICE);
		Object o = getValueFromString(s);
		if (o != null) 
		{
			choices.add(o);
			ExpCoordinator.print(new String("     adding choices:" + choices.size()), TEST_CHOICE);
		}
	}
	public void setDefaultFromString(String s)
	{
		Object o = getValueFromString(s);
		if (o != null) setDefaultValue(o);
	}
	public Object getValueFromString(String s)
	{
		if (s.length() <= 0) return null;
		if (s.equals(WILDCARD_STR))
		{
			ExpCoordinator.print(new String("ParamSpec(" + label + ").getValueFromString " + s + " wildcard:" + wildcardValue), TEST_WILDCARD);
			if (wildcardValue != null) return (((WCObject)wildcardValue).getCopy());
		}
		if (isInt()) 
		{
			String val = s.replaceAll("\\D","");
			//String val = s.replaceAll(",","");
			return (new Integer(val));
		}
		if (isDouble()) return (new Double(s));
		if (isBool())  return (new Boolean(s));
		if (isString()) return (new String(s));
		try
		{
			if (isIPAddr()) return (new ONL.IPAddress(s));
			if (isNextHop() && !isNextHopIP()) return (new NextHop(s));
			if (isNextHopIP()) return (new NextHopIP(s));
		}
		catch(java.text.ParseException e1)
		{
			ExpCoordinator.print(new String("ParamSpec.getValueFromString error for (" + s + "):" + e1.getMessage())); 
		}
		return null;
	}
	public void setDefaultValue(Object o)
	{
		if (o != null) ExpCoordinator.print(new String("ParamSpec(" + label + ":" + type + ").setDefaultValue " + o.toString()), Param.TEST_SETVALUE);
		if (isInt())
		{
			if (o instanceof Integer) defaultValue = new Integer((Integer)o);
			if (o instanceof WCInteger) defaultValue = new WCInteger((WCInteger)o);
			if (defaultValue != null) ExpCoordinator.print(new String("    is int defaultValue:" + defaultValue.toString()), Param.TEST_SETVALUE);
		}
		if (isDouble())
		{
			if (o instanceof Double) defaultValue = new Double((Double)o);
			if (o instanceof WCDouble) defaultValue = new WCDouble((WCDouble)o);
			if (defaultValue != null) ExpCoordinator.print(new String("    is double defaultValue:" + defaultValue.toString()), Param.TEST_SETVALUE);
		}
		if (isBool() && o instanceof Boolean) defaultValue = new Boolean((Boolean)o);
		if (isString() && o instanceof String) defaultValue = new String((String)o);
		
		if (isIPAddr() && o instanceof ONL.IPAddress) defaultValue = new ONL.IPAddress((ONL.IPAddress)o);  
		if (isNextHop() && !isNextHopIP() && o instanceof NextHop) 
		{
			if (defaultValue == null) defaultValue = new NextHop((NextHop)o); 
			else ((NextHop)defaultValue).set((NextHop)o); 
			ExpCoordinator.print(new String("ParamSpec(" + label + ").setDefaultValue defaultValue:" + defaultValue.toString()), TEST_NEXTHOP);
		}
		if (isNextHopIP() && o instanceof NextHopIP) 
		{
			if (defaultValue == null) defaultValue = new NextHopIP((NextHopIP)o); 
			else ((NextHopIP)defaultValue).set((NextHopIP)o); 
		}
	
	}
	protected void setDefaultValue()
	{
		if (isInt()) setDefaultValue(new Integer(0));
		if (isDouble()) setDefaultValue(new Double(0));
		if (isBool()) setDefaultValue(new Boolean(false));
		if (isString()) setDefaultValue("");
		if (isIPAddr()) setDefaultValue(new ONL.IPAddress());
		if (isNextHop() && !isNextHopIP()) setDefaultValue(new NextHop());
		if (isNextHopIP()) setDefaultValue(new NextHopIP());
	}
	public Object getDefaultValue() { return defaultValue;}
	public String toString()
	{
		String rtn = new String(label + "  " + type);
		if (getDefaultValue() != null) 
			rtn = rtn.concat(new String(" " + getDefaultValue().toString()));
		if (editable) rtn = rtn.concat(" editable");
		return rtn;
	}
	public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) { return (new XMLHandler(xmlr, hwh, this));}
	public boolean isEditable() { return editable;}
	public void setCommandSpec(CommandSpec cs) { commandSpec = cs;}
	public CommandSpec getCommandSpec() { return commandSpec;}
	public boolean isEqual(ParamSpec p)
	{
		return (getLabel().equals(p.getLabel()) && type.equals(p.type) && editable == p.editable);
	}
	public String getFormat() { return format;}
	public void setFormat(String f) { format = new String(f);}
	public Param createParam(ONLComponent parent, Command cmd)
	{
		if (isField())
			return (new FieldParam(this, parent, cmd));
		else
			return (new Param(this, parent, cmd));
	}
	public String getXMLElemName() { return (ExperimentXML.PARAM);}
	private void setWildcard(String s) 
	{ 
		if (isInt()) wildcardValue = new WCInteger(new Integer(s), true);
		else
		{
			if (isDouble()) wildcardValue = new WCDouble(new Double(s), true);
		}

		ExpCoordinator.print(new String("ParamSpec(" + label + ").setWildcard(" + s + ") wildcard:" + wildcardValue), TEST_WILDCARD);
	}
} //end class ParamSpec
