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
 * File: PluginInstance.java
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
import java.lang.String;
import java.io.*;
import javax.swing.undo.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.lang.reflect.Array;
import java.util.*;
import java.lang.String;
import java.text.ParseException;
import java.lang.NumberFormatException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;


public class PluginInstance implements ONLTableCellRenderer.Committable, ONLCTable.Element
{
  private PluginClass pluginClass = null;
  private PluginInstanceTable pinstanceTable = null;
  private Bindings bindings = null;
  private NSPPort nspport = null;
  private BindingsEdit bindingsEdit = null;
  private ExpCoordinator expCoordinator = null;
  private CommandLog commandLog = null;
  private PClassesListener pclassesListener = null;
  private PluginClasses pluginClasses = null;
  private PluginClasses.List pluginClassList = null;

  //property names
  public static final String INDEX = "index";
  public static final String INSTANCE_ID = "instance_id";
  public static final String DELETED = "deleted";
  public static final String COMMITTED = "committed";


  //field indices or column numbers
  protected static final int CLASSNAME = 0;
  protected static final int INSTANCE = 1;
  protected static final int BINDING = 2;

  private ONLPropertyList properties = null;
  
  public static class CommandLog extends AbstractAction
  {
    private Vector commands = null;
    private JInternalFrame dialog = null;
    private JTextArea log = null;
    private PluginInstance pinstance = null;
    private ONLMainWindow mainWindow = null;
    private JFrame portWindow = null;
    private JInternalFrame sendDialog = null;
    private TextFieldwLabel command = null;
    private TextFieldwLabel parameters = null;
      
    private class NCCPOp implements PluginInstanceTable.PluginOp
    {
      private String command = null;
      private PluginData.NCCP_Requester request = null;
   
      public NCCPOp(int cmdid, String sf)
      {
        String[] strarray = sf.split("(\\D|\\s)+");
        int len = Array.getLength(strarray);
        int[] fields = new int[len];
        command = new String("Command id:" + cmdid + " " + sf);
        //System.out.println("PluginInstance.CommandLog.NCCPOp " + sf + " split into " + len + " parts");
        int j = 0;
        for (int i = 0; i < len; ++i)
          {
            if (strarray[i].matches("\\d+")) fields[j++] = Integer.parseInt(strarray[i]);
          }
        request = new PluginData.NCCP_Requester(pinstance, cmdid, fields, j);
      }
      public int getClassID() { return (pinstance.pluginClass.getID());}
      public int getInstanceID() { return (pinstance.getInstanceID());}
      public void opSucceeded(NCCP.ResponseBase resp)
        {
          PluginData.NCCP_Response pd_resp = (PluginData.NCCP_Response)resp;
          int max = (int)pd_resp.getNumFields();
          String msg = new String(command + " -- succeeded.");
          ExpCoordinator.printer.print(new String("Plugin command returned " + max + " fields"), 2);
          for (int i = 0; i < max; ++i)
            {
              if (i == 0) msg = msg.concat(" Returned");
              msg = msg.concat(new String(" " + ((int)pd_resp.getField(i))));
              ExpCoordinator.printer.print(String.valueOf(pd_resp.getField(i)), 2);
            }
          addCommand(msg);
          pinstance.pinstanceTable.removeOperation(this);
        }
      public void opFailed(NCCP.ResponseBase resp)
        {
          addCommand(new String(command + " -- failed"));
          pinstance.pinstanceTable.removeOperation(this);
        }
      public Vector getRequests() //return list of requests to send
        {
          Vector rtn = new Vector();
          rtn.add(request);
          return rtn;
        }
      public boolean containsRequest(REND_Marker_class rend) { return (request != null && request.getMarker().isEqual(rend));}
    }//end inner class PluginInstance.CommandLog.NCCPOp
    public CommandLog(PluginInstance pi)
    {
      commands = new Vector();
      pinstance = pi;
      mainWindow = ExpCoordinator.theCoordinator.getMainWindow();
      portWindow = pinstance.nspport.getFrame();
    }
    public void addCommand(String s)
    {
      showLog();
      commands.add(s);
      String msg = s;
      if (!msg.endsWith("\n")) 
        {
          ExpCoordinator.printer.print(new String("CommandLog.addCommand (" + msg + ") adding new line"), 2);
          msg = msg.concat("\n");
        }
      else 
        ExpCoordinator.printer.print(new String("CommandLog.addCommand (" + msg + ")"), 2);
      log.append(msg);
    }
    public void showLog()
    {
      if (log == null) 
        {
          log = new JTextArea();
          //log.setEditable(false);
          log.setLineWrap(true);
          log.append("\n");
          int max = commands.size();
          for (int i = 0; i < max; ++i)
            log.append((String)commands.elementAt(i));
        }
      if (dialog == null)
        {
          dialog = new JInternalFrame(new String("Plugin Command Log:" + pinstance.pluginClass.getName() + pinstance.getInstanceID()));// + " " + pinstance.nspport.getLabel()));
          dialog.setSize(225, 120);                       
          JScrollPane sp = new JScrollPane(log);
          sp.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
          dialog.getContentPane().add(sp);
        }
      pinstance.nspport.showDesktop(dialog);
    }
    public void actionPerformed(ActionEvent e) { actionPerformed();}
    public void actionPerformed()
    {
      if (sendDialog == null)
        {
          JPanel panel = new JPanel();
          panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
          sendDialog = new JInternalFrame(new String("Send Command:" + pinstance.pluginClass.getName() + pinstance.getInstanceID()));
          sendDialog.setSize(225, 120); 
          pinstance.nspport.showDesktop(sendDialog);
          command = new TextFieldwLabel(25, "Command ID:");
          parameters = new TextFieldwLabel(25, "Parameters:");
          panel.add(command);
          panel.add(parameters);
          JButton acceptButton = new JButton(new AbstractAction("Send"){
              public void actionPerformed(ActionEvent e)
              {
                if (command.getText().length() > 0) 
                  {
                    pinstance.pinstanceTable.addOperation(new NCCPOp(Integer.parseInt(command.getText()), parameters.getText()));
                    if (ExpCoordinator.isRecording())
                      ExpCoordinator.recordEvent(SessionRecorder.COMMAND_PLUGIN, new String[]{pinstance.nspport.getLabel(), pinstance.toString(), command.getText(), parameters.getText()});
                  }
              }
            });
          JButton cancelButton = new JButton(new AbstractAction("Close"){
              public void actionPerformed(ActionEvent e)
              {
                sendDialog.dispose();
              }
            });
          JPanel tmp_panel = new JPanel();
          tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
          tmp_panel.add(acceptButton);
          tmp_panel.add(cancelButton);
          panel.add(tmp_panel);
          sendDialog.getContentPane().add(panel);
        } 
      pinstance.nspport.showDesktop(sendDialog);  
    }
    private void clear()
      {
        if (sendDialog != null) 
          {
            sendDialog.dispose();
            sendDialog = null;
          }
        
        if (dialog != null) 
          {
            dialog.dispose();
            dialog = null;
          }
      }
  }
  public static class Binding extends FilterDescriptor.PluginBinding implements ONLTableCellRenderer.Committable
  {
    private boolean committed = false;
    private String state = "";
    public Binding(Binding b) 
      { 
        super(b);
        committed = b.committed;
      }
    public Binding(ONL.Reader rdr) throws java.io.IOException, java.text.ParseException 
      {
        this(rdr.readString());
      }
    public Binding(String s) throws java.text.ParseException
      {
        super(s);
        if (isEqual(FilterDescriptor.PluginBinding.PLUGIN) || isEqual(FilterDescriptor.PluginBinding.NOPLUGIN ))
          throw new java.text.ParseException("PInstance.Binding parse error: not a valid Binding must be 8-127", 0);
      }
    public void setCommitted(boolean b) { committed = b;}
    public boolean isCommitted() { return committed;}
    public boolean needsCommit() { return (!committed);}
    public void write(ONL.Writer wrtr) throws java.io.IOException { wrtr.writeInt(toInt());}
    public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
    {
      xmlWrtr.writeStartElement(ExperimentXML.BINDING);//"binding");
      xmlWrtr.writeCharacters(String.valueOf(toInt()));
      xmlWrtr.writeEndElement();
    }
    public void read(ONL.Reader rdr) throws java.io.IOException { setPBinding(rdr.readInt());}
    protected static void skip(ONL.Reader rdr) throws java.io.IOException { rdr.readInt();}
    public String getState() { return state;}
    public void setState(String s) { state = new String(s);}
  }


  public static class Bindings extends Vector
  {
    public Bindings(Collection b) { super(b);}
    public Bindings() { super();}
    public Bindings(ONL.Reader rdr) throws java.io.IOException
      {
        super();
        try
          {
            read(rdr);
          }
        catch (java.text.ParseException e) {}
      }
    public String toString()
      {
        ONL.StringWriter wrtr = new ONL.StringWriter();
        try
          {
            write(wrtr);
          }
        catch (java.io.IOException e) {}
        return (wrtr.getString());
      }
    public void parseString(String str) throws java.text.ParseException
      {
        ONL.StringReader rdr = new ONL.StringReader(str);
        try
          {
            read(rdr);
          }
        catch (java.io.IOException e)
          {
            throw new java.text.ParseException("PluginInstance.Bindings parse string: should be series of space separate valid plugin bindings (integer values in range 128-255)", 0);
          }
      }
    public void read(ONL.Reader rdr) throws java.io.IOException, java.text.ParseException
      {
        int len = rdr.readInt();
        for (int i = 0; i < len; ++i)
          addBinding(new Binding(rdr));
      }
    static protected void skip(ONL.Reader rdr) throws java.io.IOException
      {
        int len = rdr.readInt();
        for (int i = 0; i < len; ++i)
          Binding.skip(rdr);
      }
    public void write(ONL.Writer wrtr) throws java.io.IOException
      {
        int len = size();
        wrtr.writeInt(len);
        for (int i = 0; i < len; ++i)
          {
            ((Binding)elementAt(i)).write(wrtr);
          }
      }
    public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
    {
      xmlWrtr.writeStartElement(ExperimentXML.BINDINGS);//"bindings");
      int len = size();
      for (int i = 0; i < len; ++i)
	{
	  ((Binding)elementAt(i)).writeXML(xmlWrtr);
	}
      xmlWrtr.writeEndElement();
    }
    public boolean containsBinding(Binding b)
      {
        if (getBinding(b) != null) return true;
        else return false;
      }
    public Binding getBinding(Binding b)
    {
        int len = size();
        Binding rtn;
        for (int i = 0; i < len; ++i)
          {
            rtn = (Binding)elementAt(i);
            if (rtn.isEqual(b)) return rtn;
          }
        return null;
      }
    public Binding bindingAt(int i) { return ((Binding)elementAt(i));}
    public void addBinding(Binding b)
     {
       if (containsBinding(b)) return;
       ExpCoordinator.printer.print(new String("Bindings.addBinding " + b.toInt()), 4);
       add(b);
     }
    public void addBindings(Bindings b)
     {
        int len = size();
        for (int i = 0; i < len; ++i)
          addBinding((Binding)elementAt(i));
     }
    public void removeBinding(Binding b)
     {
        int len = size();
        for (int i = 0; i < len; ++i)
          {
            if (((Binding)elementAt(i)).isEqual(b)) 
              {
                remove(i);
                break;
              }
          }
     }
  }


  protected static class BindingsDisplay extends PluginInstance.Bindings
  {
    public BindingsDisplay()  
      {
        super();
      }
    public BindingsDisplay(String s)  throws java.text.ParseException
      {
        super();
        parseString(s);
      }
    public BindingsDisplay(PluginInstance.Bindings b)
      {
        super(b);
      }
    public void parseString(String s) throws java.text.ParseException
      {
        String[] strarray = s.split("(\\D|\\s)+");
        int len = Array.getLength(strarray);
        System.out.println("BindingsDisplay.parseString " + s + " split into " + len + " parts");
        for (int i = 0; i < len; ++i)
          {
            if (strarray[i].matches("\\d+"))
              {
                System.out.println("adding binding " + strarray[i]);
                addBinding(new PluginInstance.Binding(strarray[i]));
              } 
          }
      } 
    public String toString()
      {
        String rtn = "";
        int len = size();
        for (int i = 0; i < len; ++i)
          {
            rtn = rtn.concat(elementAt(i).toString());
          }
        return rtn;
      }
  }
  /////////////////////////////////////////////// NCCP Classes ////////////////////////////////////////////////////////
  public static class NCCP_Response extends NCCP.ResponseBase
  {
    private int instanceID = -1;
    private PluginClass pclass = null;

    public NCCP_Response ()
      {
	super(8);
        pclass = new PluginClass();
      }
    public void retrieveFieldData(DataInput din) throws IOException
      {
	pclass.read(new ONL.NCCPReader(din));
        instanceID = din.readInt();
      }
    public double getData(MonitorDataType.Base mdt) { return instanceID;}
    public int getInstanceID() { return instanceID;}
    public PluginClass getPClass() { return pclass;}
    public String getString() 
      { 
        String str = "Plugin Instance Op";
        switch (getOp())
          {
          case NCCP.Operation_CreatePInstance:
            str = "Create Plugin Instance Op";
            break;
          case NCCP.Operation_DeletePInstance:
            str = "Delete Plugin Instance Op";
            break;
          case NCCP.Operation_BindPInstance:
            str = "Bind Plugin Instance Op";
            break;
          case NCCP.Operation_UnbindPInstance:
            str = "Unbind Plugin Instance Op";
            break;
          }
        return str;
      }
  }// class NCCP_Response
  
  public static class NCCP_Requester extends NCCP.RequesterBase
  {
    private PluginInstance pinstance = null;
    private NSPPort nspport = null;
    private PluginClass pclass = null;
    
    public NCCP_Requester(int op, NSPPort p, PluginClass pc)
    {
      super(op);
      pclass = pc;
      nspport = p;
      setMarker(new REND_Marker_class());
    }

    public NCCP_Requester(int op, PluginInstance pi)
    {
      super(op);
      pinstance = pi;
      nspport = pinstance.nspport;
      pclass = pinstance.pluginClass;
      setMarker(new REND_Marker_class());
    }
   
    public void storeData(DataOutputStream dout) throws IOException
    {
      dout.writeShort(nspport.getID()); //rdPort
      //write src address, range of ports, mask in 4 byte form e.g. 32 == 0xffffffff
      pclass.write(new ONL.NCCPWriter(dout));
      if (pinstance != null)
        dout.writeInt(pinstance.getInstanceID());
      else dout.writeInt(0);
      if (getOp() == NCCP.Operation_CreatePInstance) 
        {
          pinstance.setCommitted(true);
          pinstance.pinstanceTable.fireCommitEvent(pinstance);
        }
    }
   
    public PluginInstance getPInstance() { return pinstance;}
  } //class NCCP_Requester

  
  public static class NCCP_BindingRequester extends NCCP_Requester
  {
    private Binding binding = null;
    
    public NCCP_BindingRequester(int op, PluginInstance pi, Binding b)
      {
        super(op, pi);
        binding = b;
      }
   
    public void storeData(DataOutputStream dout) throws IOException
      {
        super.storeData(dout);
        dout.writeInt(binding.toInt());
        binding.setCommitted(true);
      }
    public Binding getBinding() { return binding;}
  }//class NCCP_BindingRequester
    


  /////////////////////////////////////////////// Inner class PClasseseListener ////////////////////////////////////////////
  private class PClassesListener implements ListDataListener
  {
    private Vector listeners = null;
    private PluginClasses.List pclist = null;
    public PClassesListener(PluginClasses.List l)
    {
      pclist = l;
      listeners = new Vector();
    }
    public void addListener(PClassListener pcl)
    {
      if (listeners.size() == 0) pclist.addListDataListener(this);
      listeners.add(pcl);
    }
    public void removeListener(PClassListener pcl)
    {
      if (listeners.contains(pcl)) listeners.remove(pcl);
      if (listeners.size() == 0) pclist.removeListDataListener(this);
    }
    public void contentsChanged(ListDataEvent e)
     {
       int num_ls = listeners.size();
       int i;
       if (pclist.getPClassState(pluginClass).equals(ONLComponent.ACTIVE))
         {
           for (i = 0; i < num_ls; ++i)
             ((PClassListener)listeners.elementAt(i)).classActive();
         }
       if (pclist.getPClassState(pluginClass).equals(ONLComponent.FAILED))
         {
           for (i = 0; i < num_ls; ++i)
             ((PClassListener)listeners.elementAt(i)).classFailed();
         }
     }
    public void intervalAdded(ListDataEvent e) { contentsChanged(e);}
    public void intervalRemoved(ListDataEvent e) { contentsChanged(e);}
  }

  /////////////////////////////////////////////// Inner Interface PClassListener ////////////////////////////////////////////

  private interface PClassListener
  {
    public void classFailed();
    public void classActive();
  }

  /////////////////////////////////////////////// Inner Class InstanceEdit /////////////////////////////////////////////////
  public static class InstanceEdit extends Experiment.Edit implements PluginInstanceTable.PluginOp, PClassListener
  {
    protected PluginInstance pinstance = null;
    protected PluginInstanceTable pinstanceTable = null;
    protected static final String AddClassName = "PluginInstance$AddInstanceEdit";
    protected static final String DeleteClassName = "PluginInstance$DeleteInstanceEdit";
    private NCCP_Requester request = null;
    private Vector cancelled = null;
    private boolean isAddEdit = true;

    public InstanceEdit(NSPPort c, PluginInstance r, PluginInstanceTable rt, Experiment exp, boolean is_add)
      {
	super(c, exp);
	pinstanceTable = rt;
	pinstance = r;
        isAddEdit = is_add;
        //ExpCoordinator.printer.print("FilterDescriptor.AddEdit " + getClass().getName());
      }
    public void undo() throws CannotUndoException
      {
	  //super.undo();
        if (isAddEdit) 
          {
            pinstanceTable.removeInstance(pinstance);
            if (pinstanceTable.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(pinstanceTable);
          }
        else
          {
            pinstanceTable.addElement(pinstance);
            if ((getExperiment() != null) && !(getExperiment().containsParam(pinstanceTable))) getExperiment().addParam(pinstanceTable);	
          }
      }
    public void redo() throws CannotRedoException
      {
	  //super.redo();
        if (isAddEdit) 
          {
            pinstanceTable.addElement(pinstance);
            if ((getExperiment() != null) && !(getExperiment().containsParam(pinstanceTable))) getExperiment().addParam(pinstanceTable);	
          }
        else
          {
            pinstanceTable.removeInstance(pinstance);
            if (pinstanceTable.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(pinstanceTable);
          }
      }
    protected void sendMessage() 
      {
        //TODO change to look at rootDir class list
        //register listener on class list - act like fail if list fails or list is active but there's no class
        if (!pinstance.isPClassActive())
          {
            ExpCoordinator.printer.print(new String("PluginInstance.InstanceEdit.sendMessage " + pinstance.toString() + " plugin class not active"), 2);
            if (!pinstance.isPClassFailed()) 
              {
                pinstance.addPClassListener(this);
              }
            return;
          }
        MonitorDaemon ec = (MonitorDaemon)((NSPPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);

        if (ec != null) 
	  {
            if (isAddEdit && !pinstance.properties.getPropertyBool(COMMITTED))
              {
                request = (new PluginInstance.NCCP_Requester(NCCP.Operation_CreatePInstance, pinstance));
                pinstanceTable.addOperation(this);
                ExpCoordinator.printer.print(new String("PluginInstance.InstanceEdit.sendMessage adding instance of " + pinstance.toString() + " to daemon " + ec.toString() + ":"), 2);
              }
            if (!isAddEdit && pinstance.properties.getPropertyBool(COMMITTED))
              {
                request = (new PluginInstance.NCCP_Requester(NCCP.Operation_DeletePInstance, pinstance));
                pinstanceTable.addOperation(this);
                ExpCoordinator.printer.print(new String("PluginInstance.InstanceEdit.sendMessage deleting instance of " + pinstance.toString() + " to daemon " + ec.toString() + ":"), 2);
              }
	  }
	else System.err.println("PluginInstance.InstanceEdit no daemon");
      }
    protected void sendUndoMessage() {}
    public boolean isCancelled(ONLComponent.Undoable un)
      {
	if (((un instanceof PluginInstance.DeleteInstanceEdit) && isAddEdit) ||
            ((un instanceof PluginInstance.AddInstanceEdit) && !isAddEdit))
	  {
	    InstanceEdit edit = (InstanceEdit) un;
	    return (onlComponent == edit.getONLComponent() && pinstance == edit.pinstance);
	  }
	return false;
      }
    protected static boolean isPIAddEdit(ONLComponent.Undoable un)
      {
        String nm = un.getClass().getName();
        return (nm.equals(AddClassName));
      }
    protected static boolean isPIDeleteEdit(ONLComponent.Undoable un)
      {
        String nm = un.getClass().getName();
        return (nm.equals(DeleteClassName));
      }
    public PluginInstance getPInstance() { return pinstance;}
    //PluginInstanceTable.PluginOp
    public int getInstanceID() { return (pinstance.getInstanceID());}
    public int getClassID() { return (pinstance.pluginClass.getID());}
    //NCCPOpManager.Operation
    public void opSucceeded(NCCP.ResponseBase resp)
      {
	if (request != null && request.getMarker().isEqual(resp.getRendMarker())) 
	  {
	    if (resp.getOp() == NCCP.Operation_CreatePInstance)
	      {
                pinstance.setInstanceID(((PluginInstance.NCCP_Response)resp).getInstanceID());
                pinstance.properties.setProperty(ONLComponent.STATE, ONLComponent.ACTIVE);
                pinstanceTable.changeElement(pinstance);
                ExpCoordinator.printer.print(new String("PluginInstance.InstanceEdit.opSucceeded adding instance " + pinstance.toString()), 2);
	      }
            else
              {
                pinstance.clear();
                ExpCoordinator.printer.print(new String("PluginInstance.InstanceEdit.opSucceeded deleting instance " + pinstance.toString()), 2);
              }
          }
        pinstanceTable.removeOperation(this);
      }
    public void opFailed(NCCP.ResponseBase resp)
      {
	if (request != null && request.getMarker().isEqual(resp.getRendMarker()))
	  { 
            ExpCoordinator expCoordinator = ExpCoordinator.theCoordinator;
            int port = pinstance.nspport.getID();
            expCoordinator.addError(new StatusDisplay.Error((new String("Plugin Instance Operation Failure on port " + port + " for instance " + pinstance.toString() + " failed " + resp.getString())),
                                                            (new String("Plugin Instance Operation Failure on port "+ port)),
                                                            "Plugin Instance Operation Failure.",
                                                            (StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));        
	    if (isAddEdit)
              {
                pinstance.setCommitted(false);
                pinstance.properties.setProperty(ONLComponent.STATE, ONLComponent.FAILED);
                expCoordinator.addEdit(this);
              }
            else pinstanceTable.addElement(pinstance);
          }
        pinstanceTable.removeOperation(this);
      }
    public Vector getRequests() 
      { 
        Vector rtn = new Vector();
        rtn.add(request);
        return rtn;
      }
    public boolean containsRequest(REND_Marker_class rend)
      {
        return (request != null && request.getMarker().isEqual(rend));
      }
    //end NCCPOpManager.Operation
    //PClassListener interface
    public void classFailed()
      {
        pinstance.removePClassListener(this);
        pinstance.setState(ONLComponent.FAILED);
      }
    public void classActive()
      {  
        //enable operation if class is confirmed
        pinstance.removePClassListener(this);
        if (isCommitted()) sendMessage();
      }
    //end PClassListener
  }//inner class InstanceEdit

  public static class AddInstanceEdit extends InstanceEdit
  {
    public AddInstanceEdit(NSPPort c, PluginInstance r, PluginInstanceTable rt, Experiment exp)
      {
	super(c, r, rt, exp, true);
      }
  }//inner class AddInstanceEdit

  public static class DeleteInstanceEdit extends InstanceEdit
  {
    public DeleteInstanceEdit(NSPPort c, PluginInstance r, PluginInstanceTable rt, Experiment exp)
      {
	super(c, r, rt, exp, false);
      }
  }//inner class DeleteInstanceEdit



  /////////////////////////////////////////////// Inner Class BindingsEdit //////////////////////////////////////////////////
  private class BindingsEdit extends Experiment.Edit implements PluginInstanceTable.PluginOp, PropertyChangeListener
  {
    private Bindings adds = null;
    private Bindings removals = null;
    private Bindings old = null;
    private Bindings latest = null; 
    private Vector requests = null;
    private PluginInstance pinstance = null;

    public BindingsEdit(PluginInstance pi, Experiment exp)
      {
	super(nspport, exp);
        pinstance = pi;
        latest = new Bindings(bindings);
        old = new Bindings(bindings);
        requests = new Vector();
        adds = new Bindings();
        removals = new Bindings();
        //ExpCoordinator.printer.print("FilterDescriptor.AddEdit " + getClass().getName());
      }
    public void undo() throws CannotUndoException
      {
        //super.undo();
        resetBindings(old);
      }
    public void redo() throws CannotRedoException
      {
        //super.redo();
        resetBindings(latest);
      }
    protected void sendMessage() 
      {
        resetBindingsEdit();
        if (getState() != ONLComponent.ACTIVE) 
          {
            properties.addListener(ONLComponent.STATE, this);
            return;
          }
        
        MonitorDaemon ec = (MonitorDaemon)((NSPPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);

        if (ec != null) 
	  {
            Binding binding;
            int max = adds.size();
            int i = 0;
            ExpCoordinator.printer.print(new String("PluginInstance.BindingEdit numadds = " + max ), 2);
            for (i = 0; i < max; ++i)
              {
                binding = adds.bindingAt(i);
                requests.add(new PluginInstance.NCCP_BindingRequester(NCCP.Operation_BindPInstance, pinstance, binding));
                ExpCoordinator.printer.print(new String("PluginInstance.BindingEdit binding to " + binding.toString() + " to instance " + toString() + " to daemon " + ec.toString() + ":"), 2);
              }
            max = removals.size();
            ExpCoordinator.printer.print(new String("PluginInstance.BindingEdit numrms = " + max));
            for (i = 0; i < max; ++i)
              {
                binding = removals.bindingAt(i);
                requests.add(new PluginInstance.NCCP_BindingRequester(NCCP.Operation_UnbindPInstance, pinstance, binding));
                ExpCoordinator.printer.print(new String("PluginInstance.BindingEdit unbinding from " + binding.toString() + " to instance " + toString() + " to daemon " + ec.toString() + ":"), 2);
              }
            if (!requests.isEmpty()) 
              {
                ExpCoordinator.printer.print( "   resetting bindings", 2);
                ExpCoordinator.printer.print( "   adding op", 2);
                pinstanceTable.addOperation(this);
              }
          }
	else ExpCoordinator.printer.print("PluginInstance.BindingEdit no daemon", 0);
            ExpCoordinator.printer.print( "   finished", 2);
      }
    protected void sendUndoMessage() {}
    public boolean isCancelled(ONLComponent.Undoable un)
      {
        if (InstanceEdit.isPIDeleteEdit(un))
          {
	    InstanceEdit edit = (InstanceEdit) un;
	    return (onlComponent == edit.getONLComponent() && pinstance == edit.getPInstance());
          }
	return false;
      }
    public PluginInstance getPInstance() { return pinstance;}
    //PluginInstanceTable.PluginOp
    public int getInstanceID() { return (pinstance.getInstanceID());}
    public int getClassID() { return (pluginClass.getID());}
    //NCCPOpManager.Operation
    public void opSucceeded(NCCP.ResponseBase resp)
      {
        NCCP_BindingRequester request = getRequest(resp.getRendMarker());
	if (request != null) 
	  {
            Binding binding = request.getBinding();
	    if (resp.getOp() == NCCP.Operation_BindPInstance)
	      {
                ExpCoordinator.printer.print(new String("PluginInstance.BindingEdit.opSucceeded binding to " + binding.toString() + " for instance " + toString()), 3);
	      }
            else
                ExpCoordinator.printer.print(new String("PluginInstance.BindingEdit.opSucceeded unbinding from " + binding.toString() + " for instance " + toString()), 3);
            requests.remove(request);
          }
        if (requests.isEmpty()) pinstanceTable.removeOperation(this);
      }
    public void opFailed(NCCP.ResponseBase resp)
      {
        NCCP_BindingRequester request = getRequest(resp.getRendMarker());
	if (request != null) 
	  {
            Binding binding = request.getBinding();
            int port = nspport.getID();
            ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("Plugin Instance Bind Operation Failure on port " + port + " for binding " +  binding.toString() + " for instance " + toString() + " failed " + resp.getString())),
                                                            (new String("Plugin Instance Operation Failure on port "+ port)),
                                                            "Plugin Instance Operation Failure.",
                                                            StatusDisplay.Error.LOG));          
	 
	    if (resp.getOp() == NCCP.Operation_BindPInstance)
              {
                binding.setCommitted(false);
                ExpCoordinator.theCoordinator.addEdit(this);
              }
            //else addBinding(binding);
            requests.remove(request);
          }
        if (requests.isEmpty()) pinstanceTable.removeOperation(this);
      }
    public Vector getRequests() 
      { 
        Vector rtn = new Vector(requests);
        return rtn;
      }
    public boolean containsRequest(REND_Marker_class rend)
      {
        NCCP_BindingRequester request = getRequest(rend);
        return (request != null);
      }
    //end NCCPOpManager.Operation
    //interface PropertyChangeListener
    public void propertyChange(PropertyChangeEvent e)
      {
        if (e.getSource() == pinstance)
          {
            if (e.getPropertyName().equals(ONLComponent.STATE))
              {
		  String st = getState();
                if (st.equals(ONLComponent.FAILED))
                  {
                    properties.removeListener(this);
                  }
                if (st.equals(ONLComponent.ACTIVE) && isCommitted())
                  {
                    properties.removeListener(this);
                    sendMessage();
                  }
              }
          }
      } 
    //end interface PropertyChangeListener
    public NCCP_BindingRequester getRequest(REND_Marker_class rend)
      {
	int max = requests.size();
	NCCP_BindingRequester req = null;
	for (int i = 0; i < max; ++i)
	  {
	    req = (NCCP_BindingRequester)requests.elementAt(i);
	    if (req.getMarker().isEqual(rend)) return req;
	  }
	return null;
      }
    public void resetEdit()
      {
        old = new Bindings(bindings);
        latest = new Bindings(bindings);
      }
    public void setLatest()
      {
        Bindings tmp = new Bindings(bindings);
        int max = tmp.size();
        int i = 0;
        Binding elem;
        for (i = 0; i < max; ++i)
          {
            elem = tmp.bindingAt(i);
            if (!latest.containsBinding(elem))
              {
                adds.addBinding(elem);
                removals.removeBinding(elem);
              }
          }
        max = latest.size();
        for (i = 0; i < max; ++i)
          {
            elem = latest.bindingAt(i);
            if (!tmp.containsBinding(elem))
              {
                adds.removeBinding(elem);
                removals.addBinding(elem);
              }
          }
        latest = tmp;
      }
  }//inner class BindingsEdit

  /////////////////////////////////////////////// Plugin Instance ////////////////////////////////////////////////////////

  public PluginInstance(NSPPort p, ONL.Reader rdr) throws java.io.IOException//,java.text.ParseException //form type srcx.x.x.x/mask srcportrange destx.x.x.x/mask destportrange protocl pluginBinding forwardkey (priority) -- priority is for GM filters
    {
      this(p);
      try 
        {
          read(rdr);
        }
      catch (java.text.ParseException e) {}
    }
  public PluginInstance(PluginInstance pi) 
    { 
      this(pi.nspport, pi.pluginClass, pi.properties.getPropertyInt(INDEX));
      setBindings(pi.bindings);
    }
  public PluginInstance(NSPPort p)
    {
      expCoordinator = ExpCoordinator.theCoordinator;
      properties = new ONLPropertyList(this);
      nspport = p;    
      properties.setProperty(COMMITTED, false);
      bindings = new Bindings();
      pinstanceTable = p.getPInstanceTable();
      pluginClasses = pinstanceTable.pluginClasses;
      setState(ONLComponent.NOT_INIT);
      resetBindingsEdit();
    }
  public PluginInstance(NSPPort p, PluginClass pc, int index)
    {
      this(p);
      pluginClass = pc;
      properties.setProperty(INDEX, index);
      ExpCoordinator.printer.print(new String("PluginInstance.constructor " + pc.getName() + " state is " + pc.getState()), 4);
    }

  public void parseString(String str) throws java.text.ParseException //form type srcx.x.x.x/mask srcportrange destx.x.x.x/mask destportrange protocl pluginBinding forwardkey (priority) -- priority is for GM filters
    {
      try
        {
          read(new ONL.StringReader(str));
        }
      catch (java.io.IOException e)
        {
          throw new java.text.ParseException(new String("PluginInstance parse error for : " + str), 0);
        }
    }

  public void read(ONL.Reader rdr) throws java.io.IOException, java.text.ParseException
    {   
      pluginClass = new PluginClass(rdr);
      PluginClasses pclasses = ((NSPDescriptor)nspport.getParent()).getPluginClasses();
      //pluginClass = pclasses.getPluginClass(pc);
      //if (pluginClass == null) pluginClass = pclasses.addPluginClass(pc); //need to do an AddOperation for the class
      rdr.readInt(); //read instance id but don't set because i don't think i can save this
      properties.setProperty(INDEX, rdr.readInt());
      setBindings(new Bindings(rdr));
    }

  static protected void skip(ONL.Reader rdr) throws java.io.IOException
    {   
      PluginClass.skip(rdr);
      rdr.readInt(); //read instance id 
      rdr.readInt(); //read index
      Bindings.skip(rdr);
    }
      

  public void write(ONL.Writer wrtr) throws java.io.IOException
    {
      pluginClass.write(wrtr);
      wrtr.writeInt(getInstanceID());
      wrtr.writeInt(getIndex());
      bindings.write(wrtr);
    }

  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
    {
      xmlWrtr.writeStartElement(ExperimentXML.PLUGIN);//"plugin");
      xmlWrtr.writeStartElement(ExperimentXML.PCLASS);//"pClass");
      pluginClass.writeXML(xmlWrtr, false);
      xmlWrtr.writeEndElement();
      xmlWrtr.writeStartElement(ExperimentXML.INDEX);//"index");
      xmlWrtr.writeCharacters(String.valueOf(getIndex()));
      xmlWrtr.writeEndElement();
      xmlWrtr.writeStartElement(ExperimentXML.INSTANCEID);//"instanceID");
      xmlWrtr.writeCharacters(String.valueOf(getInstanceID()));
      xmlWrtr.writeEndElement();
      bindings.writeXML(xmlWrtr);
      xmlWrtr.writeEndElement();
    }
  public String toString()
    {
      ONL.StringWriter wrtr = new ONL.StringWriter();
      try
        {
          write(wrtr);
        }
      catch (java.io.IOException e) {}
      return (wrtr.getString());
    }

  public boolean isCommitted() 
    { 
      return (properties.getPropertyBool(COMMITTED));
    } 
  //begin interface ONLTableCellRenderer.Committable
  public boolean needsCommit()
    {
      return (!properties.getPropertyBool(COMMITTED));
    }
  //end interface ONLTableCellRenderer.Committable
  public PluginClass getPluginClass() { return pluginClass;}
  public void setPluginClass(PluginClass pc) { pluginClass = pc;}
  public int getInstanceID() { return(properties.getPropertyInt(INSTANCE_ID));}
  public void setInstanceID(int i) { properties.setProperty(INSTANCE_ID, i);}
  public int getIndex() { return (properties.getPropertyInt(INDEX));}
    public void  setIndex(int i) { properties.setProperty(INDEX, i);}
  public void setCommitted(boolean b) { properties.setProperty(COMMITTED, b);}
  //public void setIndex(int i) { properties.setProperty(INDEX, i);}
  public String getProperty(String pnm) { return (properties.getProperty(pnm));}
  public void addPropertyListener(String pnm, PropertyChangeListener pcl) { properties.addListener(pnm, pcl);}
  public void removePropertyListener(PropertyChangeListener pcl) { properties.removeListener(pcl);}
  public Bindings getBindings() { return bindings;}
  public void resetBindings(Bindings b) 
    {
      bindings = new Bindings(b);
      pinstanceTable.changeElement(this);
    }
  public void setBindings(Bindings b)
    {
      if (!expCoordinator.isCurrentEdit(bindingsEdit)) 
        {
          ExpCoordinator.printer.print(new String(toString() + " adding bindingsEdit"), 7);
          bindingsEdit.resetEdit();
          expCoordinator.addEdit(bindingsEdit);
        }
      if (bindings != null) 
        {
          int max = b.size();
          Binding tmp_b;
          Binding tmp_binding;
          for (int i = 0; i < max; ++i)
            {
              tmp_b = b.bindingAt(i);
              tmp_binding = bindings.getBinding(tmp_b);
              if (tmp_binding != null) tmp_b.setCommitted(tmp_binding.isCommitted());
            }
        }
      bindings = new Bindings(b);
      bindingsEdit.setLatest();
    }
  protected void resetBindingsEdit()
    {
      bindingsEdit = new BindingsEdit(this, expCoordinator.getCurrentExp());
    }

  public boolean isWritable() { return true;} //interface ONLCTable.Element
  public boolean isEqual(PluginInstance pi)
  {
    return (pluginClass.isEqual(pi.pluginClass) && 
            nspport == pi.nspport && 
            (properties.getPropertyInt(INDEX) == pi.properties.getPropertyInt(INDEX))); //&&
    //(properties.getPropertyInt(INSTANCE_ID) == pi.properties.getPropertyInt(INSTANCE_ID))); 

  }
  public NSPPort getPort() { return nspport;}
  public void getCommandFromUser() 
    {
      if (commandLog == null) commandLog = new CommandLog(this);
      commandLog.actionPerformed();
    }
  public void showCommandLog()
    {
      if (commandLog == null) commandLog = new CommandLog(this);
      commandLog.showLog();
    }

  private boolean isPClassActive() { return (getPClassList().getPClassState(pluginClass).equals(ONLComponent.ACTIVE));}
  private boolean isPClassFailed() { return (getPClassList().getPClassState(pluginClass).equals(ONLComponent.FAILED));}
  private PluginClasses.List getPClassList()
    {
      if (pluginClassList == null)   
        {
          pluginClassList = pluginClasses.getPClassList(pluginClass.getRootDir());
          if (pluginClassList == null) pluginClassList = pluginClasses.addPClassList(pluginClass.getRootDir());
        }
      return pluginClassList;
    }
  private void addPClassListener(PClassListener pcl) 
    { 
      if (pclassesListener == null)
        {
          PluginClasses.List pclist = getPClassList();
          pclassesListener = new PClassesListener(pclist);
        }
      pclassesListener.addListener(pcl);
    }
  private void removePClassListener(PClassListener pcl) { pclassesListener.removeListener(pcl);}
  private void clear() 
    {
      if (commandLog != null) commandLog.clear();
    }

  public void setState(String s) { properties.setProperty(ONLComponent.STATE, s);}
  public String getState() { return (properties.getProperty(ONLComponent.STATE));}
  //ONLCTable.Element interface
  public ONLComponent getTable() { return pinstanceTable;}
  public Object getDisplayField(int c)
  {
    switch(c)
      {
      case CLASSNAME:
        return (getPluginClass().getName());
      case INSTANCE:
        String tmp = getProperty(PluginInstance.INSTANCE_ID);
        if (tmp == null) tmp = new String("unassigned");
	return tmp;
      case BINDING:
        return (new BindingsDisplay(getBindings()));
	}
    return null;
  }
  public boolean isEditable(int c) { return (c == BINDING);}
  public void setField(int c, Object a)
  {
    ExpCoordinator ec = ExpCoordinator.theCoordinator;
    if (c == BINDING) 
      {
        try
          {
            String ostr = "";
            if (ExpCoordinator.isRecording()) ostr = toString();
            setBindings(new BindingsDisplay((String)a));
            if (ExpCoordinator.isRecording())
              ExpCoordinator.recordEvent(SessionRecorder.BINDINGS_PLUGIN, new String[]{nspport.getLabel(), ostr, toString()});
          }
        catch (java.text.ParseException e) {}
      }
  }
  //end ONLCTable.Element interface
}


