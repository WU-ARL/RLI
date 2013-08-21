/*
 * Copyright (c) 2008 Jyoti Parwatikar and Washington University in St. Louis.
 * All rights reserved
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author or Washington University may not be used
 *       to endorse or promote products derived from this source code
 *       without specific prior written permission.
 *    4. Conditions of any other entities that contributed to this are also
 *       met. If a copyright notice is present from another entity, it must
 *       be maintained in redistributions of the source code.
 *
 * THIS INTELLECTUAL PROPERTY (WHICH MAY INCLUDE BUT IS NOT LIMITED TO SOFTWARE,
 * FIRMWARE, VHDL, etc) IS PROVIDED BY THE AUTHOR AND WASHINGTON UNIVERSITY
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR WASHINGTON UNIVERSITY
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS INTELLECTUAL PROPERTY, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * */

/*
 * File: PluginInstanceTable.java
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
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import java.util.*;
import javax.swing.undo.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class PluginInstanceTable extends ONLCTable
{
  protected PluginClasses pluginClasses = null;
  private NSPPort port = null;
  private PluginData.MAction monitorAction = null;
  private JInternalFrame frame = null;

  protected static final int CLASSNAME = PluginInstance.CLASSNAME;
  protected static final int INSTANCE = PluginInstance.INSTANCE;
  protected static final int BINDING = PluginInstance.BINDING;

  protected static final String CLASSES_SHOWN = "classes_shown";
  private int index = 0;

  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// PluginInstanceTable.PITContentHandler ////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private class PITContentHandler extends DefaultHandler
   {
      private ExperimentXML expXML = null;
      private String currentElement = "";
      private PluginInstance currentPInstance = null;
      private PluginClass currentPClass = null;
      private PluginInstanceTable pinstanceTable = null;

      public PITContentHandler(ExperimentXML exp_xml, PluginInstanceTable pit)
      {
	super();
	expXML = exp_xml;
	pinstanceTable = pit;
      }
      public void startElement(String uri, String localName, String qName, Attributes attributes) 
      {	  
	  currentElement = new String(localName);
	  if (localName.equals(ExperimentXML.PLUGIN) && currentPInstance == null) 
	    currentPInstance = new PluginInstance(pinstanceTable.getPort());
	  if (localName.equals(ExperimentXML.PCLASS) && currentPClass == null) 
	    currentPClass = new PluginClass();
      }
      public void characters(char[] ch, int start, int length)
      {
	if (currentElement.equals(ExperimentXML.DIRECTORY) && currentPClass != null)
	  currentPClass.setPluginDir(new String(ch, start, length));
	if (currentElement.equals(ExperimentXML.CLASSNAME) && currentPClass != null)
	  currentPClass.setName(new String(ch, start, length));
	if (currentElement.equals(ExperimentXML.CLASSID) && currentPClass != null)
	  currentPClass.setID(Integer.parseInt(new String(ch, start, length)));
	if (currentElement.equals(ExperimentXML.INDEX) && currentPInstance != null)
	    currentPInstance.setIndex(Integer.parseInt(new String(ch, start, length)));
	if (currentElement.equals(ExperimentXML.INSTANCEID) && currentPInstance != null)
	  currentPInstance.setInstanceID(Integer.parseInt(new String(ch, start, length)));
	try
	  {
	    if (currentElement.equals(ExperimentXML.BINDING) && currentPInstance != null)
	      currentPInstance.getBindings().addBinding(new PluginInstance.Binding(new String(ch, start, length)));
	  }
	catch (java.text.ParseException e)
	  {
	    ExpCoordinator.print(new String("PluginInstanceTable.PITContentHandler.characters ParseException " + e.getMessage()));
	  }
      }
      public void setCurrentElement(String s) { currentElement = new String(s);}
      public String getCurrentElement() { return currentElement;}
      public void endElement(String uri, String localName, String qName)
      {
	  if (localName.equals(ExperimentXML.PINSTANCES)) 
	    {
              expXML.removeContentHandler(this);
            }
	  if (localName.equals(ExperimentXML.PLUGIN) && currentPInstance != null) 
	    {
	      addNewElement(currentPInstance);
	      currentPInstance = null;
	    }
	  if (localName.equals(ExperimentXML.PCLASS) && currentPClass != null) 
	    {
	      currentPInstance.setPluginClass(currentPClass);
	      currentPClass = null;
	    }
      }
   }

   ////////////////////////////////////////////////////// PluginInstanceTable.MonitorListener ///////////////////////////////
  private class MonitorListener extends MouseInputAdapter
  {
    private ExpCoordinator expCoordinator = null;
    public MonitorListener() 
      { 
        super();
        expCoordinator = port.getExpCoordinator();
      }
    public void mouseClicked(MouseEvent e) 
      {
        //ExpCoordinator.printer.print(new String("PluginInstanceTable.MonitorListener.mouseClicked " + e.getClickCount() + " times"), 2);
	if (e.getClickCount() >= 2) 
	  { 
            //ExpCoordinator.printer.print(new String("          calling select"), 2);
            int ndx = getSelectedRow();
            if (ndx >= 0) monitorAction.select((PluginInstance)getElement(ndx));
          }
      }
  }
  private class OpManager extends NCCPOpManager
  {
    private PluginData.NCCP_Response pDataResponse = null;
    private NCCP.ErrorMsgResponse pDebugResponse = null;
    public OpManager()
      {
	super((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC));
	response = new PluginInstance.NCCP_Response();
        pDataResponse = new PluginData.NCCP_Response();
        pDebugResponse = new NCCP.ErrorMsgResponse(new String("plugin debug" + port.getID()));
        ((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC)).addOpManager(this);
      }
    protected void setRendMarker(NCCP.RequesterBase req, NCCPOpManager.Operation op, int i)
      {
        REND_Marker_class mrkr = new REND_Marker_class(port.getID(), false, req.getOp(), i);
        mrkr.field1 = ((PluginOp)op).getClassID();
        mrkr.field2 = ((PluginOp)op).getInstanceID();
	req.setMarker(mrkr);
      }

    public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException
      {
        switch ((hdr.getOp()))
        {
        case NCCP.Operation_PluginData:
            pDataResponse.setHeader(hdr); //set header info
	    pDataResponse.retrieveData(din); //get op specific return data
	    processResponse(pDataResponse);
            break;
        case NCCP.Operation_PluginDebug:
            pDebugResponse.setHeader(hdr); //set header info
	    pDebugResponse.retrieveData(din); //get op specific return data
	    processResponse(pDebugResponse);
            break;
        default:
          super.retrieveData(hdr, din);
        }
      }
  }

  public static interface PluginOp extends NCCPOpManager.Operation
  {
    public int getClassID();
    public int getInstanceID();
  }

  //class TableModel
  private class TableModel extends ONLCTable.TableModel
  {
    public TableModel(PluginInstanceTable rtable)
      {
	super(rtable);
        preferredW[CLASSNAME] = 115;
        preferredW[INSTANCE] = 50;
        preferredW[BINDING] = 50;
      }
    public TableCellEditor getEditor(int col) 
    {
      if (col == BINDING) return (new DefaultCellEditor(new JTextField()));
      else return (super.getEditor(col));
    }
    public TableCellRenderer getRenderer(int col)
    {
      if (col == BINDING) return ( new ONLTCCollRenderer());
      else return (super.getRenderer(col));
    }
  } //class TableModel

  protected static class UnloadMenu extends PluginClasses.Menu
  {
    private PluginInstanceTable pluginTable;
    private ExpCoordinator expCoordinator;
   
    private class Edit extends Experiment.Edit implements PluginInstanceTable.PluginOp
    {
      private PluginClass pclass = null;
      private PluginInstance.NCCP_Requester request = null;
      public Edit(PluginClass pc)
       {
         super(pluginTable.port, expCoordinator.getCurrentExp());
         pclass = pc;
       }

      public void undo() throws CannotUndoException{}
      public void redo() throws CannotRedoException {}
      protected void sendMessage() 
      {
        request = new PluginInstance.NCCP_Requester(NCCP.Operation_UnloadPClass, pluginTable.port, pclass);
        pluginTable.addOperation(this);
        ExpCoordinator.printer.print(new String("PluginInstanceTable.UnloadMenu.Edit.sendMessage unloading plugin class " + pclass.toString() + " from port " + pluginTable.port), 2);
        if (ExpCoordinator.isRecording())
          ExpCoordinator.recordEvent(SessionRecorder.UNLOAD_PLUGIN_CLASS, new String[]{pluginTable.port.getLabel(), pclass.toString()});
      }
      protected void sendUndoMessage() {}
      public boolean isCancelled(ONLComponent.Undoable un) { return false;}
    //PluginInstanceTable.PluginOp
      public int getInstanceID() { return 0;}
      public int getClassID() { return 0;}
      //NCCPOpManager.Operation
      public void opSucceeded(NCCP.ResponseBase resp)
      {
	if (request != null && request.getMarker().isEqual(resp.getRendMarker())) 
	  {
            ExpCoordinator.printer.print(new String("PluginInstanceTable.UnloadMenu.Edit.opSucceeded unloading plugin class " + pclass.toString()  + " from port " + pluginTable.port), 2);
          }
        pluginTable.removeOperation(this);
      }
      public void opFailed(NCCP.ResponseBase resp)
      {
	if (request != null && request.getMarker().isEqual(resp.getRendMarker()))
	  { 
            expCoordinator.addError(new StatusDisplay.Error((new String("Plugin Failure Unloading class " +  pclass.toString() + " on port " + pluginTable.port + " " + resp.getString())),
                                                            (new String("Plugin Failure Unloading class " +  pclass.toString() + " on port "+ pluginTable.port)),
                                                            "Plugin Unload Class Failure.",
                                                            (StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR))); 
          }
        pluginTable.removeOperation(this);
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
    }//PluginInstanceTable.UnloadMenu.Edit

    private class UnloadAction extends PluginClasses.Menu.PCAction //implements ListSelectionListener//, KeyListener
    {
      //private int nPlugin = -1;
      public UnloadAction(PluginClass pc) 
        { 
          super(pc);
        }
      
      public void actionPerformed(ActionEvent e)
        {
          //remove all plugin class related instances
          Vector removals = new Vector();
          int max = pluginTable.size();
          int i = 0;
          PluginInstance pi;
          PluginClass pc = getPluginClass();
          ONLUndoManager.AggregateEdit agg_edit = new ONLUndoManager.AggregateEdit();
          for (i = 0; i < max; ++i)
            {
              pi = (PluginInstance)pluginTable.getElement(i);
              if (pi.getPluginClass().isEqual(pc)) removals.add(pi);
            }
          max = removals.size();
          for (i = 0; i < max; ++i)
            {
              pluginTable.removeInstance((PluginInstance)removals.remove(0), agg_edit);
            }
          agg_edit.addEdit(new Edit(getPluginClass()));
          expCoordinator.addEdit(agg_edit);
          //pluginTable.addOperation(new NCCPOp(pluginTable.port, getPluginClass()));
        }
    }
    public UnloadMenu(PluginInstanceTable pt, String ttl)
      {
        super(ttl, pt.pluginClasses, false);
        pluginTable = pt;
        expCoordinator = pt.expCoordinator;
        initialize();
      }
    public PCAction getPClassAction(PluginClass pc) { return (new UnloadAction(pc));}
  }

  protected static class AddMenu extends PluginClasses.Menu
  {
    private PluginInstanceTable pluginTable;
    private ExpCoordinator expCoordinator;
    private class AddAction extends PluginClasses.Menu.PCAction //implements ListSelectionListener//, KeyListener
    {
      //private int nPlugin = -1;
      public AddAction(PluginClass pc) 
        { 
          super(pc);
        }
      
      public void actionPerformed(ActionEvent e)
        {
          Experiment exp = expCoordinator.getCurrentExp();
          if (!exp.containsParam(pluginTable)) exp.addParam(pluginTable);
          pluginTable.addNewElement(new PluginInstance(pluginTable.port, getPluginClass(), pluginTable.getIndex()));
        }
    }
    public AddMenu(PluginInstanceTable pt, String ttl)
      {
        super(ttl, pt.pluginClasses);
        pluginTable = pt;
        expCoordinator = pt.expCoordinator;
        initialize();
      }
    public PCAction getPClassAction(PluginClass pc) { return (new AddAction(pc));}
  }

  protected static class DeleteAction extends ONLCTable.ElementAction
  {
    public DeleteAction(PluginInstanceTable rt) { this(rt, "Delete Instance");}
    public DeleteAction(PluginInstanceTable rt, String ttl) 
      {
	super(rt, ttl, false);
      }
  }

  protected static class AddMonAction extends AbstractAction
  {
    private PluginInstanceTable ptable = null;
    public AddMonAction(PluginInstanceTable pt, String ttl)
      {
        super(ttl);
        ptable = pt;
      }
    public AddMonAction(PluginInstanceTable pt) { this(pt, "Add Instance Data to Graph");}
    public void actionPerformed(ActionEvent e)
      {
        if (ptable.jtable != null)
          {
            int ndx = ptable.getSelectedRow();
            if (ndx >= 0)
              {
                PluginInstance pi = (PluginInstance)ptable.getElement(ndx);
                ptable.monitorAction.select(pi);
              }
          }
      }
  }

  protected static class CommandAction extends AbstractAction
  {
    private PluginInstanceTable ptable = null;
    private boolean showLog;
    public CommandAction(PluginInstanceTable pt, String ttl, boolean sl)
      {
        super(ttl);
        ptable = pt;
        showLog = sl;
      }
    public CommandAction(PluginInstanceTable pt) { this(pt, "Send Command to Instance", false);}
    public void actionPerformed(ActionEvent e)
      {
        if (ptable.jtable != null)
          {
            int ndx = ptable.getSelectedRow();
            if (ndx >= 0)
              {
                PluginInstance pi = (PluginInstance)ptable.getElement(ndx);
                if (!showLog) pi.getCommandFromUser();
                else  pi.showCommandLog();
              }
          }
      }
  }

  public static class PTAction extends AbstractAction
  {
    private NSPPort port = null;
    //private JInternalFrame frame = null;
    private PluginInstanceTable pTable = null;
    public PTAction(String ttl, NSPPort p) 
      {
	super(ttl);
	port = p;
	pTable = port.getPInstanceTable();
      }
    public PTAction(NSPPort p) 
      {
        this("Plugin Table", p);
      }
    public void actionPerformed(ActionEvent e) 
      {
	if (pTable == null) 
	  {
	    //System.out.println("QTAction.actionPerformed ingress pluginTable is null");
	    pTable = port.getPInstanceTable();
	  }
        pTable.show();
      }
    public void clear()
      {
        if (pTable != null) pTable.clear();
      }
  }

  public PluginInstanceTable(NSPPort p, PluginClasses pc)
    {
      super("PT", ONLComponent.PINSTANCES, p.getExpCoordinator(), null, p);
      port = p;
      monitorAction = new PluginData.MAction(port);
      ((NSPPortMonitor)port.getMonitorable()).addMonitorAction(monitorAction);
      pluginClasses = pc;
      setTableModel(new TableModel(this));
      //tableModel = new TableModel(this);
    }

  protected String[] getColumnNames()
    {
      String[] rtn = new String[]{"class name", "instance", "linked to spc qids"};
      return rtn;
    }
  protected Class[] getColumnTypes()
    {
      PluginInstance.BindingsDisplay bd = new PluginInstance.BindingsDisplay();
      Class[] rtn = new Class[]{String.class, String.class, bd.getClass()};
      return rtn;
    }

  public boolean areEqual(Object o1, Object o2)
    {
      return (o1 == o2 || (((PluginInstance)o1).isEqual((PluginInstance)o2)));
    }

  protected Object addNewElement(ONL.Reader str, int i)//adds an element defined by a reader
    {
      PluginInstance pi = null;
      try
        {
          pi = (PluginInstance)addNewElement(new PluginInstance(port, str), i);
        }
      catch (java.io.IOException e) {}
      return pi;
    }
    
  protected Object addNewElement(ONLCTable.Element o, int i) //adds new element making a copy of elem 
    {
      PluginInstance elem = (PluginInstance)addElement(new PluginInstance((PluginInstance)o), i);
      if (elem != null) 
        {
          expCoordinator.addEdit(new PluginInstance.AddInstanceEdit(port, elem, this, expCoordinator.getCurrentExp()));
          if (ExpCoordinator.isRecording())
            ExpCoordinator.recordEvent(SessionRecorder.ADD_PLUGIN, new String[]{port.getLabel(), elem.getPluginClass().toString()});
          if (panel != null && panel.isVisible() && size() == 1) fireEvent(new ONLComponent.Event(ONLComponent.Event.GET_FOCUS, this, "Get Focus"));
        }
      else elem = getPInstance((PluginInstance) o);
      return elem;
    }
  protected Object addNewElement(int i) //creates new element and adds    
    {
      PluginInstance elem = (PluginInstance)addElement(new PluginInstance(port), i, true);
      if (elem != null) 
        expCoordinator.addEdit(new PluginInstance.AddInstanceEdit(port, elem, this, expCoordinator.getCurrentExp()));
      return elem;
    }
  public void removeElement(ONLCTable.Element r)
    {
      if (r!= null && containsElement(r))
	{
          super.removeElement(r);
          expCoordinator.addEdit(new PluginInstance.DeleteInstanceEdit(port, (PluginInstance)r, this, expCoordinator.getCurrentExp()));
          if (ExpCoordinator.isRecording())
            ExpCoordinator.recordEvent(SessionRecorder.DELETE_PLUGIN, new String[]{port.getLabel(), r.toString()});
        }
    }
  protected void removeFromList(ONLCTable.Element r)
  {
    if (r!= null && containsElement(r))
      {
        super.removeFromList(r);
        monitorAction.removeMonitors((PluginInstance)r);
      }
  }
  public void removeInstance(PluginInstance r) { removeElement(r);}
  public void removeInstance(PluginInstance r, ONLUndoManager.AggregateEdit edit)
    {
      if (r!= null && containsElement(r))
	{
          //should add delete edit here
          removeFromList(r);
          if (edit != null)
            edit.addEdit(new PluginInstance.DeleteInstanceEdit(port, (PluginInstance)r, this, expCoordinator.getCurrentExp()));
          else
            expCoordinator.addEdit(new PluginInstance.DeleteInstanceEdit(port, (PluginInstance)r, this, expCoordinator.getCurrentExp()));
        }
    }
  public ONLCTable.TablePanel getPanel() 
    {
      if (panel == null)
	{
	  if (port == null) System.out.println("PluginInstanceTable.getPanel port is null");
	  panel = super.getPanel();
	  jtable = panel.getJTable();
          jtable.addMouseListener(new MonitorListener());
	  jtable.setPreferredScrollableViewportSize(new Dimension(200,250));
          //panel.add(panel2);
	}
      return panel;
    }
  
  protected NCCPOpManager createOpManager() 
    {
      OpManager tmp_opm = new OpManager();
      ((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC)).addOpManager(tmp_opm);
      return tmp_opm;
    }
 
  public NSPPort getPort() { return port;}
  public int getIndex() { return (++index);}
  public PluginInstance getPInstance(PluginInstance pi)
   {
     Vector tmp_list = getList();
     
      int max = tmp_list.size();
      PluginInstance elem;
      for (int i = 0; i < max; ++i)
	{
	  elem = (PluginInstance)tmp_list.elementAt(i);
	  if (elem.isEqual(pi)) return elem;
	}
      return null;
   }
  public MonitorAction getMonitorAction() { return monitorAction;}
  
  static protected void skip(ONL.Reader tr) throws IOException
  {
      int numElems = tr.readInt();
      for (int i = 0; i < numElems; ++i)
	PluginInstance.skip(tr);
  }

  public Action getAction() { return (new PluginInstanceTable.PTAction(NSPPort.PINTACTION, port));} //define later
  public void show()
   {
     if (frame == null)
       {
         frame = new JInternalFrame(new String("Port " + port.getID() + " Plugins"));
         frame.getContentPane().add(getPanel());
         JMenuBar mb = new JMenuBar();
         frame.setJMenuBar(mb);
         JMenu menu = new JMenu("Edit");
         mb.add(menu);
         menu.add(new CommandAction(this));
         menu.add(new CommandAction(this, "Show Instance Command Log", true));
         menu.add(new AddMonAction(this));
         menu.add(new PluginClasses.AddClassAction(pluginClasses, "Add Plugin Directory", true));
         menu.add(new UnloadMenu(this, "Unload Class"));
         menu.add(new DeleteAction(this, "Delete Instance"));
         menu.add(new AddMenu(this, "Add Instance"));
         //menu.add(new PluginClasses.AddClassAction(this.pluginClasses, "Add Class", false));
         port.addToDesktop(frame);
         frame.addInternalFrameListener(new ONLComponent.ONLInternalFrameListener(this));
       }
     frame.setSize(275,100);
     port.showDesktop(frame);
   }

  public void clear()
  {
    if (frame != null && frame.isShowing()) frame.dispose();
    super.clear();
  }
  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
  {
    xmlWrtr.writeStartElement(ExperimentXML.PINSTANCES);//"pluginInstances");
    super.writeXML(xmlWrtr);
    xmlWrtr.writeEndElement();
  }
  public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new PITContentHandler(exp_xml, this));}
}
