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
			if (p_type.equals(MonitorDataType.VCIBANDWIDTH_LBL)) mdataType = new MonitorDataType.VCIBandwidth(uri, attributes);
			if (p_type.equals(MonitorDataType.VPIBANDWIDTH_LBL)) mdataType = new MonitorDataType.VPIBandwidth(uri, attributes);
			if (p_type.equals(MonitorDataType.IPPBANDWIDTH_LBL)) mdataType = new MonitorDataType.IPPBandwidth(uri, attributes);
			if (p_type.equals(MonitorDataType.OPPBANDWIDTH_LBL)) mdataType = new MonitorDataType.OPPBandwidth(uri, attributes);
			if (p_type.equals(MonitorDataType.IPPDISCARD_LBL)) mdataType = new MonitorDataType.IPPDiscard(uri, attributes);
			if (p_type.equals(MonitorDataType.OPPDISCARD_LBL)) mdataType = new MonitorDataType.OPPDiscard(uri, attributes);
			if (p_type.equals(MonitorDataType.CONSTANT_LBL)) mdataType = new MonitorDataType.Constant(uri, attributes);
			if (p_type.equals(MonitorDataType.PLUGIN_LBL)) mdataType = new PluginData.MDataType(uri, attributes);
			if (p_type.equals(NSPMonClasses.NSP_QLENGTH_LBL)) mdataType = new NSPMonClasses.MDataType_QLength(uri, attributes);
			if (p_type.equals(NSPMonClasses.NSP_PKTCOUNTER_LBL)) mdataType = new NSPMonClasses.MDataType_PktCounter(uri, attributes);
			if (p_type.equals(NSPMonClasses.NSP_FPXCNTR_LBL)) mdataType = new NSPMonClasses.MDataType_FPXCounter(uri, attributes);
			if (p_type.equals(NSPMonClasses.NSP_EMFILTER_LBL)) mdataType = new NSPMonClasses.MDataType_EMFilter(uri, attributes);
			if (p_type.equals(NSPMonClasses.NSP_RTSTATS_LBL)) mdataType = new NSPMonClasses.MDataType_RTStats(uri, attributes);
			if (p_type.equals(NSPMonClasses.NSP_VOQLENGTH_LBL)) mdataType = new NSPMonClasses.MDataType_VOQLength(uri, attributes);
			if (p_type.equals(MonitorDataType.HWMONITOR_LBL) || p_type.equals(MonitorDataType.HWCOMMAND_LBL)) mdataType = new Hardware.MDataType(uri, attributes);
			if (p_type.equals(MonitorDataType.FORMULA_LBL)) mdataType = new MonitorDataType.MFormula(uri, attributes);
		}
		if (mdataType != null) expXML.setContentHandler(mdataType.getContentHandler(expXML));
	}
	public MonitorDataType.Base getMDataType() { return mdataType;}
}
