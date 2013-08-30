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
 * File: PluginData.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 5/27/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.*;
import java.lang.*;
import java.io.*;
import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Array;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class PluginData
{

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////// NCCP_Requester //////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class NCCP_Requester extends PluginInstance.NCCP_Requester
  {
    private int commandID = 0;
    private int numFieldsSent = 0;
    private int[] sentFields = null;
    private final int maxFieldsReturn = 30;
    
    public NCCP_Requester(PluginInstance pi, int cmdid) { this(pi, cmdid, null, 0);}
    
    public NCCP_Requester(PluginInstance pi, int cmdid, int[] sf, int nfs)
      {
	super(NCCP.Operation_PluginData, pi);
	commandID = cmdid;
        if (sf != null && nfs > 0)
          {
            numFieldsSent = nfs;
            sentFields = new int[numFieldsSent];
            for (int i = 0; i < numFieldsSent; ++i)
              sentFields[i] = sf[i];
          }
        else numFieldsSent = 0;
      }
    public void storeData(DataOutputStream dout) throws IOException
      {
        super.storeData(dout);
	dout.writeInt(commandID);
	dout.writeInt(numFieldsSent);
        for (int i = 0; i < numFieldsSent; ++i)
          {
            dout.writeInt(sentFields[i]);
          }
        dout.writeInt(maxFieldsReturn);
      }
  }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////// NCCP_.Response ///////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class NCCP_Response extends NCCP.ResponseBase
  {
    protected double numFields = 0;
    protected double fieldlist[];
    private boolean fieldsSet = false;
    
    public NCCP_Response()
      {
        super(4);
      }
    public NCCP_Response(int nfields)
      {
	super(4 * (nfields + 1));
        fieldsSet = true;
	numFields = nfields;
	fieldlist = new double[(int)numFields];
	System.out.println("PluginData.NCCP_Response numfields = " + numFields);
	for (int i = 0; i < numFields; ++i) 
          fieldlist[i] = 0;
      }

    public double getField(int i) 
      {
	if (fieldlist != null && i < numFields) return (fieldlist[i]);
        else return 0;
      }
    public double getNumFields() { return numFields;}
    
    public double getData(MonitorDataType.Base mdt)
      {
	if (fieldlist != null && mdt.getMonitorID() < numFields ) return (fieldlist[mdt.getMonitorID()]);
        else return 0;
      }

    public void retrieveFieldData(DataInput din) throws IOException
    {
      double tmp_nf = NCCP.readUnsignedInt(din);
      if (tmp_nf != numFields)
        {
          if (tmp_nf > numFields) fieldlist = new double[(int)tmp_nf];
          numFields = tmp_nf;
        }
      for (int i = 0; i < numFields; ++i) 
        {
          fieldlist[i] = NCCP.readUnsignedInt(din);
        }
    }
  }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////// MonitorDataType /////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class MDataType extends MonitorDataType.Base
  {
    protected PluginInstance pinstance = null;
    protected int commandID = 0;
    protected int numFields = 0;
    
    public MDataType(String uri, Attributes attributes)
    {
      super(uri, attributes, ONLDaemon.NSPC, MonitorDataType.PLUGIN);
      monitorID = Integer.parseInt(attributes.getValue(ExperimentXML.MONITORID));
      commandID = Integer.parseInt(attributes.getValue(ExperimentXML.COMMAND));
      numFields = Integer.parseInt(attributes.getValue(ExperimentXML.NUMFIELDS));
    }
    public MDataType(SpecFile.SpecReader infile, int lv, ExpCoordinator nlists) throws IOException
      {
	super(ONLDaemon.NSPC, MonitorDataType.PLUGIN, Units.UNKNOWN);
	initFromFile(infile, lv, nlists);
	//PROBLEM: need to look up nspMonitor based on the ONLComponent 
	//nspMonitor = ((NSPDescriptor)onlComponent).getMonitor();
      }
    public MDataType(ONL.Reader rdr, int dt) throws IOException
      {
	super(ONLDaemon.NSPC, MonitorDataType.PLUGIN, Units.UNKNOWN);
	initFromReader(rdr, dt);
	//PROBLEM: need to look up nspMonitor based on the ONLComponent 
	//nspMonitor = ((NSPDescriptor)onlComponent).getMonitor();
      }
    public MDataType(PluginInstance pi, int field, int commid)
      {
	this(pi, field, commid, (new String("Plugin " + pi.getInstanceID() + "." + commid + "." + field)));
      }
    public MDataType(PluginInstance pi, int field, int commid, String nm)
      {
	super(ONLDaemon.NSPC, MonitorDataType.PLUGIN, Units.UNKNOWN);
	monitorID = field;
        NSPPort p = pi.getPort();
        setONLComponent(p);
	port = p.getID();
	pinstance = pi;
	commandID = commid;
	setName(nm);
        setIsRate(false);
      }
    public boolean isEqual(MonitorDataType.Base mdt)
      {
        if (mdt instanceof MDataType)
          {
            MDataType tmp_mdt = (MDataType) mdt;
            return (super.isEqual(mdt) 
                    && pinstance == tmp_mdt.pinstance 
                    && commandID == tmp_mdt.commandID 
                    && numFields == tmp_mdt.numFields);
          }
        else
          return false;
      }
    public boolean canBeRate() { return false;}
    public String getParamTypeName() { return MonitorDataType.PLUGIN_LBL;}
    public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException
    {
      xmlWrtr.writeAttribute(ExperimentXML.MONITORID, String.valueOf(monitorID));
      xmlWrtr.writeAttribute(ExperimentXML.COMMAND, String.valueOf(commandID));
      xmlWrtr.writeAttribute(ExperimentXML.NUMFIELDS, String.valueOf(numFields));
    }
    public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException
    {
      pinstance.writeXML(xmlWrtr);   
    }
    public String getSpec()
      {
        ONL.StringWriter wr = new ONL.StringWriter();
        try
          {
            wr.writeInt(monitorID);
            pinstance.write(wr);
            wr.writeInt(commandID);
            wr.writeInt(numFields);
            wr.writeInt(dataUnits);
          }
        catch (java.io.IOException e){}
	return (wr.getString());
      }
    public void loadFromSpec(String ln)//fills in params based on ln
      {
        ONL.StringReader rdr = new ONL.StringReader(ln);
        rdr.print(2);
        try
          {
            monitorID = rdr.readInt();
            NSPPort p = (NSPPort)getONLComponent();
            pinstance = (PluginInstance)p.getPInstanceTable().addNewElement(rdr);
            commandID = rdr.readInt();
            numFields = rdr.readInt();
            dataUnits = rdr.readInt();
          }
        catch (java.io.IOException e){}
      }
    public void initFromReader(ONL.Reader rdr, int dt) throws IOException
      {
        super.initFromReader(rdr, dt);
        monitorID = rdr.readInt();
        NSPPort p = (NSPPort)getONLComponent();
        pinstance = (PluginInstance)p.getPInstanceTable().addNewElement(rdr);
        commandID = rdr.readInt();
        numFields = rdr.readInt();
        dataUnits = rdr.readInt();
      }
    public void write(ONL.Writer wrtr) throws IOException
      {
        super.write(wrtr);
        wrtr.writeInt(monitorID);
        pinstance.write(wrtr);
        wrtr.writeInt(commandID);
        wrtr.writeInt(numFields);
        wrtr.writeInt(dataUnits);
      }
    protected void setDescription()
      {
	setDescription(new String("Plugin class " + pinstance.getPluginClass().getName() + " instance " + pinstance.getInstanceID() + " command " + commandID +  " field " + monitorID));
      }
    public void setCommandID(int c) { commandID = c;}
    public int getCommandID() { return commandID;}
    public void setPluginInstance(PluginInstance pi) { pinstance = pi;}
    public PluginInstance getPluginInstance() { return pinstance;}
    public void setNumFields(int nf) { numFields = nf;}
    public int getNumFields() { return numFields;}
  }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////// MonitorEntry //////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class MEntry extends MonitorEntry.Base implements PropertyChangeListener
  {
    private double pollInterval;
    private PluginInstance pinstance = null;
    
    public static class Plugin_REND_Marker extends REND_Marker_class 
    {
      public Plugin_REND_Marker(PluginData.MDataType mdt)
	{
	  super(mdt.getPort(), mdt.isIPP(), NCCP.Operation_ReadPlugin, mdt.getCommandID());
	  field1 = mdt.getPluginInstance().getIndex();
	}
    }

    public MEntry( MonitorEntry.Listener l, MonitorDataType.Base dt)
      {
	super(l);
	PluginData.MDataType mdt = (PluginData.MDataType)dt;
        pinstance = mdt.getPluginInstance();
	request = new NCCP_Requester(pinstance, mdt.getCommandID());
	response = new NCCP_Response();
	setMarker(getNewMarker(dt));
	msr = true;
      }

    public void storeRequest(DataOutputStream dout) throws IOException
      {
        String tmp_state = ((NCCP_Requester)request).getPInstance().getProperty(ONLComponent.STATE);
        if (tmp_state.equals(ONLComponent.ACTIVE)) request.store(dout);
	else 
          {
            ExpCoordinator.printer.print("ME::storeRequest pinstance not active", 4);
            ((NCCP_Requester)request).getPInstance().addPropertyListener(ONLComponent.STATE, this);
          } 
      }
    
    public void processResponse(NCCP.ResponseBase r)
      {
	//System.out.println("ME::processResponse");
	Monitor monitor;
	int numParams = monitors.size();
	for (int i = 0; i < numParams; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    if (monitor != null)
	      {
		//System.out.println("  calling monitor");
		if (!monitor.isStarted()) monitor.start();
		monitor.setData(r.getData(monitor.getDataType()), pollInterval);
	      }
	  }
      }

    public void setPeriod(PollingRate pr)
      {
	super.setPeriod(pr);
	pollInterval = pr.getSecsOnly(); 
      }
    
    public static REND_Marker_class getNewMarker(MonitorDataType.Base mdt)
      {
	return (new Plugin_REND_Marker((PluginData.MDataType)mdt));
      }
    //interface PropertyChangeListener
    public void propertyChange(PropertyChangeEvent e)
      {
        if (e.getSource() == pinstance)
          {
            if (e.getPropertyName().equals(ONLComponent.STATE))
              {
                String st = pinstance.getProperty(ONLComponent.STATE);
                if (st.equals(ONLComponent.FAILED))
                  {
                    pinstance.removePropertyListener(this);
                    if (isActive()) 
                      {
                        setRequestSent(false);
                        mdaemon.removeMonitorEntry(this);
                      }
                  }
                if (st.equals(ONLComponent.ACTIVE))
                  {
                    pinstance.removePropertyListener(this);
                    if (isActive()) mdaemon.sendMessage(request);
                  }
              }
          }
      } 
    //end interface PropertyChangeListener
  }


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////// MAction /////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class MAction extends AbstractMonitorAction
  {
    protected Vector monitors;
    private NSPPort nspport = null;

    //////////////////////////////////////////////// MMI.MMLParamOptions /////////////////////////////////////////////////////////////////////////////////
    
    //inner MenuItemParamOptions class 
    protected static class PIParamOptions extends MonitorMenuItem.MMLParamOptions
    {
      protected TextFieldwLabel field = null; 
      protected TextFieldwLabel command = null;
      protected Units.ComboBox units = null;
      private boolean is_monitor = true;
      private PluginInstance pinstance = null;

      public PIParamOptions(PluginInstance pi, boolean mon)
	{
	  super();
          setIsRate(false);
          setCanBeRate(false);
          pinstance = pi;
          is_monitor = mon;
	  command = new TextFieldwLabel(25, "Command:");
	  xtraParams.add(command);
          if (is_monitor)
            {
              field = new TextFieldwLabel(5, "Field to Monitor (starts at 0):");
              units = new Units.ComboBox();
              xtraParams.add(field);
              xtraParams.add("Units:");
              xtraParams.add(units);
            }
	}
      public int getField()
	{
          if (field == null || field.getText().length() <= 0) return 0;
	  else return (Integer.parseInt(field.getText()));
	}
      public int getCommand()
	{
	  if (command.getText().length() > 0) return (Integer.parseInt(command.getText()));
	  else return 0;
	}
      public int getUnits()
	{
          if (units != null)
            {
              return (units.getSelectedUnits().getType());
            }
	  return Units.UNKNOWN;
	}
      public PluginInstance getPluginInstance() { return pinstance;}
    }

    /////////////////////////////////////////////////// MMI methods //////////////////////////////////////////////////////////////////////////////////////
    public MAction(NSPPort p)
      {
	super(p.getMonitorable());
	monitors = new Vector();
        nspport = p;
      }
    protected MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo)
      {
	MonitorDataType.Base dt = null;
	PIParamOptions piop = (PIParamOptions)mpo;
	dt = new  PluginData.MDataType(piop.getPluginInstance(), piop.getField(), piop.getCommand()); 
	dt.setPollingRate(mpo.getPollingRate());
	dt.setONLComponent(nspport);
	dt.setDataUnits(piop.getUnits());
	return dt;
      }
    protected void select(PluginInstance pi)
      {
        ExpCoordinator.printer.print(new String("PluginData.MAction.select"), 2);
        MonitorMenuItem.MMLParamOptions mpo = getParamOptions(pi);
        int rtn = displayOptions(mpo, nspport.getFrame());
        if (rtn != MML_CANCELLED)
            selected(mpo);
      }
    public PIParamOptions getParamOptions(PluginInstance pi) { return (new PIParamOptions(pi, true));}
    public Monitor getMonitor(MonitorDataType.Base dt)
      {
        if (!dt.isPeriodic()) return null;
	Monitor monitor;
	int max = monitors.size();
	PluginData.MDataType pmdt = (PluginData.MDataType)dt;
	PluginData.MDataType elem_pmdt = null;
	for (int i = 0; i < max; ++i)
	  {
	    monitor = (Monitor)monitors.elementAt(i);
	    elem_pmdt = (PluginData.MDataType)monitor.getDataType();
	    if (elem_pmdt.getCommandID() == pmdt.getCommandID() && 
		elem_pmdt.getMonitorID() == pmdt.getMonitorID() &&
		elem_pmdt.getPluginInstance() == pmdt.getPluginInstance()) return monitor;
	  }
	return null;
      }
    public Monitor createMonitor(MonitorDataType.Base dt)
      {
	Monitor monitor = super.createMonitor(dt);
	if (monitor != null && dt.isPeriodic() && !monitors.contains(monitor)) monitors.add(monitor);
	return monitor;
      }
    
    public void removeMonitors(PluginInstance pi) //stops all monitors associated with plugin instance
    {
      int max = monitors.size();
      Monitor elem_mon;
      PluginData.MDataType elem_pmdt = null;
      Vector rms = new Vector();
      int i;
      for (i = 0; i < max; ++i)
        {
          elem_mon = (Monitor)monitors.elementAt(i);
          elem_pmdt = (PluginData.MDataType)elem_mon.getDataType();
          if (elem_pmdt.getPluginInstance() == pi)
            {
              elem_mon.stop();
              rms.add(elem_mon);
            }
        }
      max = rms.size();
      if (max > 0)
        {
          monitors.remove(rms);
          rms.removeAllElements();
        }
    }
    public void removeMonitor(Monitor m)
      {
	monitors.removeElement(m);
      }
    public boolean isDataType(MonitorDataType.Base mdt)
      {
	if (mdt.getParamType() == MonitorDataType.PLUGIN)
	  return true;
	else return false;
      }
  }  
}
