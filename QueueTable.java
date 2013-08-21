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
 * File: QueueTable.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/25/2004
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
import java.beans.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class QueueTable extends ONLComponent implements PropertyChangeListener //AbstractTableModel
{
  public static final int TEST_BW = 2;
  public static final int DATAGRAM_START = 440;
  public static final int VOQ_START = 504;
  public static final int Q_START = 8;
  public static final int FPX_START = 256;
  public static final int SPC_IN_START = 8; //going to SPC
  public static final int SPC_OUT_START = 136; //coming from SPC
  private Vector queues = null;
  private NSPPort port = null;
  private boolean ingress = true;
  protected static final String BANDWIDTH = "bandwidth"; //property label
  protected static final String QUEUE_STRING = "queue id";
  protected static final String QUANTUM_STRING = "quantum";
  protected static final String RATES_STRING = "rates(Mbps)";
  protected static final String THRESHOLD_STRING = "threshold(bytes)";
  protected static final String LENGTH_STRING = "length";
  protected static final int QUEUE = 0;
  protected static final double DEFAULT_BANDWIDTH = 600;
  protected static final double DEFAULT_SWBANDWIDTH = 900;
  protected static final int THRESHOLD = 1;
  protected static final int QUANTUM = 2;
  protected static final int LENGTH = 3;
  private Bandwidth bandwidth = null;
  private JTable jtable = null;
  private TableModel tableModel = null;
  //private JFrame frame = null;
  private JPanel panel = null;
  private OpManager opManager = null;

  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// QueueTable.QTContentHandler ////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private class QTContentHandler extends DefaultHandler
   {
      private ExperimentXML expXML = null;
      private String currentElement = "";
      private Queue currentQueue = null;
      private QueueTable queueTable = null;

      public QTContentHandler(ExperimentXML exp_xml, QueueTable qt)
      {
	super();
	expXML = exp_xml;
	queueTable = qt;
      }
      public void startElement(String uri, String localName, String qName, Attributes attributes) 
      {	  
	  currentElement = new String(localName);
	  if (localName.equals(ExperimentXML.QUEUE) && currentQueue == null) 
	    currentQueue = new Queue(queueTable);
	  if ( localName.equals(ExperimentXML.VOQ) && currentQueue == null) 
	    currentQueue = new VOQueue(queueTable);
      }
      public void characters(char[] ch, int start, int length)
      {
	if (currentElement.endsWith(ExperimentXML.BANDWIDTH))
	  {
	    queueTable.bandwidth.setValue(new String(ch, start, length));
	    queueTable.bandwidth.addEditForCommit();
	  }
	if (currentElement.equals(ExperimentXML.QID) && currentQueue != null)
	  currentQueue.setQueueID(new String(ch, start, length));
	if (currentElement.equals(ExperimentXML.QUANTUM) && currentQueue != null)
	  currentQueue.setQuantum(Integer.parseInt(new String(ch, start, length)));
	if ((currentElement.equals(ExperimentXML.THOLD) || currentElement.equals(ExperimentXML.RATE)) && currentQueue != null)
	  currentQueue.setThreshold(Integer.parseInt(new String(ch, start, length)));

      }
      public void setCurrentElement(String s) { currentElement = new String(s);}
      public String getCurrentElement() { return currentElement;}
      public void endElement(String uri, String localName, String qName)
      {
	  if (localName.endsWith(ExperimentXML.NSPQUEUE_TABLE)) 
	    {
              expXML.removeContentHandler(this);
            }
	  if ((localName.equals(ExperimentXML.QUEUE)|| localName.equals(ExperimentXML.VOQ)) && currentQueue != null) 
	    {
	      addNewQueue(currentQueue);
	      currentQueue = null;
	    }
      }
   }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// QueueTable.OpManager //////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private class OpManager extends NCCPOpManager
  {
    private class NCCP_Response extends NCCP.ResponseBase
    {
      private double data = 0;
      public NCCP_Response()
	{
	  super(4);
	}
      public void retrieveFieldData(DataInput din) throws IOException
	{
	  data = NCCP.readUnsignedInt(din);
	}
      public double getData(MonitorDataType.Base mdt) { return data;}
      public double getData() { return data;}
    }//end QueueTable.OpManager.NCCP_Response

    public OpManager()
      {
	super((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC));
	response = new NCCP_Response();
      }
    protected void setRendMarker(NCCP.RequesterBase req, NCCPOpManager.Operation op, int i)
      {
	req.setMarker(new REND_Marker_class(port.getID(), ingress, req.getOp(), i));
      }
  }


  protected class DQOperation extends AbstractUndoableEdit implements ONLComponent.Undoable, NCCPOpManager.Operation
  { 
    private NSPDescriptor nsp = null;
    private boolean turnOn = false;
    private NCCP_DQRequester request = null;
    private QueueTable queueTable = null;
    private boolean cancelled = false;

    private class NCCP_DQRequester extends NCCP.RequesterBase
    {
      public NCCP_DQRequester()
        {
          super(NCCP.Operation_DQONOFF);
          setMarker(new REND_Marker_class());
        }
      
      public void storeData(DataOutputStream dout) throws IOException
        {
          int val = 0;
          if (turnOn) val = 1;
          dout.writeByte(val);
        }
    }//end inner class NSPDescriptor.DQOperation.NCCP_DQRequester
    //NCCPOpManager.Operation

    public DQOperation(NSPDescriptor n, QueueTable qt, boolean trn_on)
      {
        super();
        queueTable = qt;
        turnOn = trn_on;
        request = new NCCP_DQRequester();
        nsp = n;
      }
    
    public void undo() throws CannotUndoException
      { 
        nsp.setProperty(NSPDescriptor.DQ, (!turnOn));
      } 
    public void redo() throws CannotRedoException
      {	
        nsp.setProperty(NSPDescriptor.DQ, turnOn);
      }
    //ONLComponent.Undoable
    public void commit() 
      { 
        ONLDaemon ec = nsp.getONLDaemon(ONLDaemon.NSPC);
      
        if (ec != null) 
          {
            if (!ec.isConnected()) ec.connect();
            queueTable.addOperation(this);
	  }
	else System.err.println("DQOperation no daemon");
      }
      public boolean isCancelled(ONLComponent.Undoable un)
        { 
          if (un instanceof QueueTable.DQOperation)
            {
              QueueTable.DQOperation edit = (QueueTable.DQOperation)un;
              cancelled = ((nsp == edit.nsp) && !edit.cancelled); //cancel all but the last one
              return cancelled;
            }        
          return false;
        }
    public void actionPerformed(ActionEvent e){}
    //end ONLComponent.Undoable
    //NCCPOpManager.Operation
    public void opSucceeded(NCCP.ResponseBase resp) { queueTable.removeOperation(this);}
    public void opFailed(NCCP.ResponseBase resp)
      {
        String on_str ;
        if (turnOn) on_str = "on";
        else on_str = "off";
        ExpCoordinator expc = queueTable.getExpCoordinator();
        
        expc.addError(new StatusDisplay.Error(new String("Distributed Queueing: failed to turn " + on_str + " for " + nsp.getLabel()),
                                              "DQ Failure",
                                              "DQ Failure",
                                              (StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));
        queueTable.removeOperation(this);
        nsp.setProperty(NSPDescriptor.DQ, !turnOn);
      }
    public Vector getRequests() 
      { 
        Vector rtn = new Vector();
        rtn.add(request);
        return rtn;
      }
    public boolean containsRequest(REND_Marker_class rend)
      {
	return (request.getMarker().isEqual(rend));
      }
    //end NCCPOpManager.Operation
    public NSPDescriptor getNSP() { return nsp;}
    public boolean isOn() { return turnOn;}
  }//end inner class QueueTable.DQOperation
  


        
  public static class Bandwidth extends TextFieldwLabel
  {
    private Edit edit = null;
    private QueueTable qtable = null;
    private double committedBW = DEFAULT_BANDWIDTH;
    private boolean settingCommitted = false;
    private double lastUserValue = DEFAULT_BANDWIDTH;
    private String state = null;


    public static class NCCP_BWRequester extends NCCP.RequesterBase
    {
      private Bandwidth bandwidth = null;
      private NSPPort nspport;
      private double bwSent = 0;
      private String op_str = "";
      
      public NCCP_BWRequester(int op, Bandwidth qt)
	{
	  super(op);
	  bandwidth = qt;
	  nspport = bandwidth.qtable.getPort();
	  
	  switch(getOp())
	    {
	    case NCCP.Operation_SwitchBW:
	      op_str = "set switch bw";
	      break;
	    case NCCP.Operation_LinkBW:
	      op_str = "set link bw";
	      break;
	    }
	  setMarker(new REND_Marker_class());
	}
      
      public void storeData(DataOutputStream dout) throws IOException
	{
	  bwSent = bandwidth.getBandwidth();
	  dout.writeShort(nspport.getID());
	  dout.writeInt((int)(bwSent*1000)); //convert from Mbps to Kbps
	  ExpCoordinator.printer.print(new String("QueueTable.Bandwidth.NCCP_BWRequester.storeData " + op_str + " for port:" + nspport.getID() + "bw:" + bwSent), 4);
	}
      
      public String getOpString() { return op_str;}
      public double getBWSent() { return bwSent;}
    }
    
    private class Edit extends AbstractUndoableEdit implements ONLComponent.Undoable, ActionListener, NCCPOpManager.Operation, FocusListener
    {
      private Bandwidth bandwidth = null;
      private Experiment experiment = null;
      private NCCP_BWRequester request = null;
      private double oldBW = 0;
      private double newBW = 0;
      
      public Edit(Bandwidth bw)
	{
	  super();
	  bandwidth = bw;
	  oldBW = bandwidth.getBandwidth();
	  newBW = oldBW;
	}
      //FocusListener
      public void focusLost(FocusEvent e) { actionPerformed(null);}
      public void focusGained(FocusEvent e) { }
      //ActionListener
      public void actionPerformed(ActionEvent e)
	{
	  oldBW = newBW;
	  newBW = bandwidth.getBandwidth();
          if (settingCommitted || oldBW == newBW)
            {
              oldBW = newBW;
              settingCommitted = false;
              return;
            }
          bandwidth.lastUserValue = newBW;
	  ExpCoordinator.printer.print(new String("Bandwidth.Edit.actionPerformed oldBW = " + oldBW + " newBW = " + newBW + " lastUserValue = " + bandwidth.lastUserValue), 2);
          if (e != null) //it came from the user and not internal
            qtable.resetVOQs();
	  bandwidth.qtable.getExpCoordinator().addEdit(this);
          //commit(); //calling commit directly causes the change to take place immediately
	  bandwidth.updateColor();
	  experiment = bandwidth.qtable.getExpCoordinator().getCurrentExp();
	  if ((experiment != null) && !(experiment.containsParam(bandwidth.qtable))) experiment.addParam(bandwidth.qtable);
	}
      //end ActionListener
      public void undo() throws CannotUndoException
	{
	  newBW = bandwidth.getBandwidth();
	  bandwidth.setValue(oldBW);
	  if (bandwidth.qtable.size() <= 0 && (experiment != null)) experiment.removeParam(bandwidth.qtable);
	  bandwidth.updateColor();
	}
      public void redo() throws CannotRedoException
	{
	  //super.redo();
	  bandwidth.setValue(newBW);
	  if ((experiment != null) && !(experiment.containsParam(bandwidth.qtable))) experiment.addParam(bandwidth.qtable);
	  bandwidth.updateColor();	
	}
      //ONLComponent.Undoable
      public void commit() 
	{
	  ONLDaemon ec = bandwidth.qtable.getPort().getONLDaemon(ONLDaemon.NSPC);
	  
	  if (ec != null) 
	    {
	      if (!ec.isConnected()) ec.connect();
	      
	      //ExpCoordinator.printer.print("Edit bandwidth daemon " + ec.toString());
	      int op = NCCP.Operation_SwitchBW;
	      if (!bandwidth.qtable.isIngress()) op = NCCP.Operation_LinkBW;
	      request = new NCCP_BWRequester(op, bandwidth);
		  //ec.sendMessage(new NCCP_QueueRequester(NCCP.Operation_AddFIPLQueue, (NSPPort)getONLComponent(), queue));
	      bandwidth.qtable.addOperation(this);
	      Experiment exp = bandwidth.qtable.getExpCoordinator().getCurrentExp();
	      if ((exp != null) && !(exp.containsParam(qtable))) exp.addParam(qtable);
	  }
	else System.err.println("AddQueue no daemon");
	bandwidth.resetEdit();
	bandwidth.updateColor();
      }
      public boolean isCancelled(ONLComponent.Undoable un){ return false;}
      //end ONLComponent.Undoable
      //NCCPOpManager.Operation
      public void opSucceeded(NCCP.ResponseBase resp) 
	{
          double bw = ((OpManager.NCCP_Response)resp).getData()/1000; //convert from Kbits to Mbits
          ExpCoordinator.print(new String("QueueTable.Bandwidth.opSucceeded bandwidth = " + bw), 2);
	  bandwidth.committedBW = bw;
          bandwidth.setCommittedValue(String.valueOf(bw));
	  bandwidth.qtable.removeOperation(this);
	  bandwidth.updateColor();
          settingCommitted = false;
	}
      public void opFailed(NCCP.ResponseBase resp)
	{
	  if (request.getBWSent() == bandwidth.getBandwidth())
	    {
	      bandwidth.setCommittedValue(String.valueOf(bandwidth.committedBW));
	    }
	  bandwidth.qtable.removeOperation(this);
	  bandwidth.updateColor();
          settingCommitted = false;
	}
      public Vector getRequests() 
	{ 
	  Vector rtn = new Vector();
	  rtn.add(request);
	  return rtn;
	}
      public boolean containsRequest(REND_Marker_class rend) { return (request != null && request.getMarker().isEqual(rend));}
      public double getBWSent() 
	  {
	    if (request != null) return request.getBWSent();
	    else return 0;
	  }
      //end NCCPOpManager.Operation
    }//inner class Edit

    public Bandwidth(String lbl, QueueTable qt)
      {
	super(20, lbl);
        setObserveAction(false, true);
	qtable = qt;
        if (qtable.isIngress()) committedBW = DEFAULT_SWBANDWIDTH;
        setValue(committedBW);
	resetEdit();
      }
    private void resetEdit()
      {
        if (edit != null) 
          {
            textField.removeActionListener(edit);
            textField.removeFocusListener(edit);
          }
	edit = new Edit(this);
	textField.addActionListener(edit);
	textField.addFocusListener(edit);
      }
    public double getBandwidth()
      {
        double rtn = 0;
        try
          {
            rtn = Double.parseDouble(getText());
            return rtn;
          }
        catch (java.lang.NumberFormatException e)
          {
            return 0;
          }
      }
    public void addEditForCommit()
      {
	edit.actionPerformed(null);
      }
    public void updateColor()
      {
        double tmp_bw = getBandwidth();
	if (committedBW == tmp_bw) textField.setForeground(Color.black);
	else
	  {
	    if (edit != null && edit.getBWSent() == tmp_bw) textField.setForeground(Color.black);
	    else textField.setForeground(Color.red);
	  }
      }
    public void setCommittedValue(String str)
      {
        settingCommitted = true;
        setValue(str);
        if (edit == null) settingCommitted = false;
      }
    public void setValue(String s) 
    { 
      super.setValue(s);
      if (qtable != null) qtable.setProperty(BANDWIDTH, s);
      if (ExpCoordinator.isRecording() && qtable.ingress)
        ExpCoordinator.recordEvent(SessionRecorder.LINKBANDWIDTH, new String[]{qtable.getPort().getLabel(), s});
      ExpCoordinator.print(new String("QTable("+ qtable.getLabel() + ").Bandwidth.setValue(" + s + ")"), TEST_BW);
    }
    protected boolean merge(Bandwidth bw)
    { 
      ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String(qtable.getLabel() + ":Bandwidth Inconsistency for master:" + getBandwidth() + " compared with:" + bw.getBandwidth() + "\n")),
								     "Table Inconsistency",
								     "Table Inconsistency",
								     StatusDisplay.Error.LOG)); 
      return true;
    }
    public void setState(String s) { state = new String(s);}
  }

  //class TableModel
  private class TableModel extends AbstractTableModel implements ONLTableCellRenderer.CommittableTable
  {
    private Class types[] = null;
    private QueueTable queueTable = null;
    public TableModel(QueueTable rtable)
      {
	super();
	queueTable = rtable;
	Queue tmp_route = new Queue(rtable); 
        Queue.TholdRange trange = new Queue.TholdRange();
        Class tmp_class = Integer.class;
        if (expCoordinator.isTestLevel(ExpCoordinator.DQ) && ingress) 
         {
	types = new Class[]{
	  tmp_route.getQueueLabel().getClass(), trange.getClass(), Double.class, Integer.class
	    };
         }
        else
         {
	types = new Class[]{
	  tmp_route.getQueueLabel().getClass(), trange.getClass(), Integer.class, Integer.class
	    };
         }
      }
      public int getColumnCount() { return 3;} //4;}
    public Class getColumnClass(int c) 
      { return types[c];}
    public int getRowCount() { return (queueTable.queues.size());}
    public String getColumnName(int ndx)
      {
	String nm = null;
	switch(ndx)
	  {
	  case QUEUE:
	    nm = QUEUE_STRING;
	    break;
	  case THRESHOLD:
	    nm = THRESHOLD_STRING;
	    break;
	  case QUANTUM:
	    nm = QUANTUM_STRING;
            if (expCoordinator.isTestLevel(ExpCoordinator.DQ) && ingress) nm = RATES_STRING;
	    break;
	  case LENGTH:
	    nm = LENGTH_STRING;
	    break;
	  }
	return nm;
      }
    public boolean isCellEditable(int r, int c)     
      {
	//check if field set in route if not let it be editable
	//if we're filling in a new route can edit the prefix and the mask too
        if (ExpCoordinator.isObserver() || (!ingress && r <= 0)) //the first one is the default for egress so don't change it
	  return false;
	Queue rt = queueTable.getQueueAt(r);
	if (rt != null)
	  {
            return (rt.isEditable(c));
	  }
	return false;
    }
  public int findColumn(String nm)
    {
      if (nm.equals(QUEUE_STRING)) return QUEUE;
      if (nm.equals(THRESHOLD_STRING)) return THRESHOLD;
      if (nm.equals(QUANTUM_STRING)) return QUANTUM;
      if (nm.equals(RATES_STRING)) return QUANTUM;
      if (nm.equals(LENGTH_STRING)) return LENGTH;
      System.err.println("QueueTable::findColumn not a recognized string");
      return 0;
    }
  public void setValueAt(Object a, int r, int c)
    {
      Queue rt = (Queue)queueTable.queues.elementAt(r);
      //ExpCoordinator.printer.print("QueueTable::setValueAt class " + a.getClass().getName());
      if (rt != null) rt.setField(c, a);
      fireTableCellUpdated(r,c);
      fireTableRowsUpdated(c,c);
      fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_CHANGE, queueTable, rt, r, null, "queue changed")); 
    }
  public Object getValueAt(int r, int c)
    {
      Object rtn = null;
      Queue rt = (Queue)queueTable.queues.elementAt(r);
      if (rt != null) return (rt.getDisplayField(c));
      return null;
    }
  // begin interface ONLTableCellRenderer.CommittableTable
    public ONLTableCellRenderer.Committable getCommittableAt(int row)
    {
      return ((ONLTableCellRenderer.Committable)queueTable.getQueueAt(row));
    }
  // end interface ONLTableCellRenderer.CommittableTable
  } //class TableModel


  protected static class AddQueueAction extends AbstractAction //implements ListSelectionListener, KeyListener
  {
    private QueueTable queueTable;
    private ExpCoordinator expCoordinator;
    //private int nQueue = -1;
    public AddQueueAction(QueueTable rt, String ttl)
      {
	super(ttl);
	queueTable = rt;
	expCoordinator = queueTable.getExpCoordinator();
      }
    
    public void actionPerformed(ActionEvent e)
      {
	Queue nQueue = queueTable.addNewQueue();
	Experiment exp = expCoordinator.getCurrentExp();
	if (!exp.containsParam(queueTable)) exp.addParam(queueTable);
	expCoordinator.addEdit(new Queue.AddEdit(queueTable.port, nQueue, queueTable, exp));
      if (ExpCoordinator.isRecording()&& !queueTable.isIngress())
        ExpCoordinator.recordEvent(SessionRecorder.ADD_EGRESS_Q, new String[]{queueTable.getPort().getLabel(), String.valueOf(nQueue.getQueueID()), String.valueOf(nQueue.getThreshold()), String.valueOf(nQueue.getQuantum())}); 
      }
  }



  protected static class DeleteQueueAction extends AbstractAction 
  {
    private QueueTable queueTable;
    private ExpCoordinator expCoordinator;
    private JTable jtable;
    
    public DeleteQueueAction(QueueTable rt, JTable jt, String ttl)
      {
	super(ttl);
	queueTable = rt;
	jtable = jt;
	expCoordinator = queueTable.port.getExpCoordinator();
      }
    
    public void actionPerformed(ActionEvent e)
      {
	int[] ndxs = jtable.getSelectedRows();
	int max = Array.getLength(ndxs);
	Vector rts = new Vector(max);
	int i = 0;
        Queue elem;
	for (i = 0; i < max; ++i)
	  {
            elem = queueTable.getQueueAt(ndxs[i]);
	    rts.add(elem);
            if (ExpCoordinator.isRecording()&& !queueTable.isIngress())
              ExpCoordinator.recordEvent(SessionRecorder.DELETE_EGRESS_Q, new String[]{queueTable.getPort().getLabel(), String.valueOf(elem.getQueueID()), String.valueOf(elem.getThreshold()), String.valueOf(elem.getQuantum())}); 
	  }
	for (i = 0; i < max; ++i)
	  {
	    queueTable.removeQueue((Queue)rts.elementAt(i));
	  }
	
	//expCoordinator.addEdit(new Queue.DeleteEdit(queueTable.port, rts, queueTable, expCoordinator.getCurrentExp()));
      }
  }

  public static class QTAction extends AbstractAction
  {
    private NSPPort port = null;
    private JInternalFrame frame = null;
    private QueueTable inQTable = null;
    private QueueTable outQTable = null;
    private boolean ingress = true;
    public QTAction(String ttl, NSPPort p) 
      {
	super(ttl);
	port = p;
	inQTable = port.getQueueTable(true);
	outQTable = port.getQueueTable(false);
	//if (inQTable == null) ExpCoordinator.printer.print("QTAction ingress queueTable is null");
	//if (outQTable == null) ExpCoordinator.printer.print("QTAction egress queueTable is null");
      }
    public QTAction(NSPPort p) 
      {
        this("Queue Table", p);
      }
    public void actionPerformed(ActionEvent e) 
      {
	if (inQTable == null) 
	  {
	    //ExpCoordinator.printer.print("QTAction.actionPerformed ingress queueTable is null");
	    inQTable = port.getQueueTable(true);
	  }
	if (outQTable == null) 
	  {
	    //ExpCoordinator.printer.print("QTAction.actionPerformed egress queueTable is null");
	    outQTable = port.getQueueTable(false);
	  }
	if (frame == null)
	  {
	    frame = new JInternalFrame(new String("Port " + port.getID() + " Queues"));
	    JPanel pane = new JPanel();
	    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
	    //Bandwidth inbw = inQTable.getBandwidth();
	    //inbw.setAlignmentX(Component.LEFT_ALIGNMENT);
	    //inbw.setMaximumSize(new Dimension(200, 20));
	    //pane.add(inbw);
	    Bandwidth outbw = outQTable.getBandwidth();
	    outbw.setAlignmentX(Component.LEFT_ALIGNMENT);
	    outbw.setMaximumSize(new Dimension(200, 20));
	    pane.add(outbw);
	    pane.add(inQTable.getPanel());
	    pane.add(outQTable.getPanel());
	    frame.getContentPane().add(pane);
	    //frame.getContentPane().add(inQTable.getPanel());
	    //frame.getContentPane().add(outQTable.getPanel());
	    JMenuBar mb = new JMenuBar();
	    frame.setJMenuBar(mb);
	    JMenu menu = new JMenu("Edit");
	    mb.add(menu);
	    AddQueueAction add_action = new AddQueueAction(outQTable, "Add Egress Queue");
	    menu.add(add_action);
	    menu.add(new DeleteQueueAction(outQTable, outQTable.jtable, "Delete Egress Queue"));
            port.addToDesktop(frame);
            frame.addInternalFrameListener(new ONLComponent.ONLInternalFrameListener(inQTable));
            //frame.addInternalFrameListener(new ONLComponent.ONLInternalFrameListener(outQTable));
	  }
	frame.setSize(250,250);
        port.showDesktop(frame);
      }
    public void clear()
      {
	  if (frame != null && frame.isShowing()) frame.dispose();
	  if (inQTable != null) inQTable.clear();
	  if (outQTable != null) outQTable.clear();
      }
  }

  public QueueTable(NSPPort p, String tp)
    {
      super(p, "QT", tp, p.getExpCoordinator());
      port = p;
      queues = new Vector();
      tableModel = new TableModel(this);
      if (tp.equals(ONLComponent.IPPQTABLE))
        ingress = true;
      else ingress = false;
      if (ingress) 
        {
          bandwidth = new Bandwidth("Switch Bandwidth(Mbps):", this);
          if (expCoordinator.isTestLevel(ExpCoordinator.DQ))
            {
              ((NSPDescriptor)port.getParent()).addPropertyListener(NSPDescriptor.INTERNALBW, this);
              ((NSPDescriptor)port.getParent()).addPropertyListener(NSPDescriptor.DQ, this);
            }
        }
      else bandwidth = new Bandwidth("Link Bandwidth(Mbps):", this);
      bandwidth.setMaximumSize(new Dimension(150,15));
      bandwidth.setAlignmentX(Component.LEFT_ALIGNMENT);
      if (ingress) bandwidth.setEditable(false);
      //bandwidth.setAlignmentY(0);
      initLabel();
      Queue tmp_queue = null;
      try
	{
	  if (!ingress)
	    {
	      tmp_queue = new Queue(Queue.DEFAULT_STR, this);
	      tmp_queue.setCommitted();
	      queues.add(tmp_queue);
	      tmp_queue = new Queue(Queue.DATAGRAM_STR, this);
	      tmp_queue.setCommitted();
	      queues.add(tmp_queue);
	    }
	  else
	    {
              int i;
	      for (i = 0; i < 8; ++i)
		{
		  tmp_queue = new VOQueue(i, this);
		  tmp_queue.setCommitted();
		  queues.add(tmp_queue);
		}
	    }
	}
      catch (java.text.ParseException e) {}
      addStateListener(BANDWIDTH);
    }
  private void initLabel()
    {
      String desc = "Ingress ";
      if (!ingress) desc = "Egress ";
      addToDescription(1, new String(desc + "Port " + port.getLabel() + " Queue Table"));
    }

  protected static void skip(ONL.Reader tr) throws IOException
    {
      tr.readString();
      int numQueues = tr.readInt();
      for (int i = 0; i < numQueues; ++i) tr.readString();
    }
      
  protected void read(ONL.Reader tr) throws IOException
    {
      ExpCoordinator.print(new String(getLabel() + " QT.read"), 5);
      bandwidth.setValue(tr.readString());
      bandwidth.addEditForCommit();
      int numQueues = tr.readInt();
      Vector tmp_queues = new Vector();
      String ln;
      for (int i = 0; i < numQueues; ++i)
	{
	  try
	    {
              ln = tr.readString();
	      Queue tmp_rt = new Queue(ln, this);
	      Queue q = getQueue(tmp_rt.getQueueID());
              ExpCoordinator.print(new String( "  " + ln), 5);
	      if (q != null)
		{
                  ExpCoordinator.print(new String( "  q" + q.getQueueID() + " set thold=" + tmp_rt.getThreshold() + " quantum=" + tmp_rt.getQuantum()), 5);
		  q.setThreshold(tmp_rt.getThreshold());
		  q.setQuantum(tmp_rt.getQuantum());
                  expCoordinator.addEdit(q.getUpdateEdit());
		  changeQueue(q);
		}
	      else
		{
		  q = tmp_rt;
		  addQueue(tmp_rt);
		}
	      tmp_queues.add(q);
	      expCoordinator.addEdit(q.getUpdateEdit());
	    }
	  catch (java.text.ParseException e)
	    {
	      System.err.println("Error:- QueueTable.read ParseException bad route. " + e.getMessage());
	    }
	}
      if (!tmp_queues.isEmpty()) 	
	{
	  Experiment exp = expCoordinator.getCurrentExp();
	  if (!exp.containsParam(this)) exp.addParam(this);
	}
    }
  public void writeExpDescription(ONL.Writer tw) throws IOException 
    { 
      //ExpCoordinator.printer.print("RT::writeExpDescription");
      super.writeExpDescription(tw);
      writeTable(tw);
    }
  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
  {
    if (ingress)
      {
	xmlWrtr.writeStartElement("inQueueTable");
	xmlWrtr.writeStartElement("linkBandwidth");
	xmlWrtr.writeCharacters(String.valueOf(bandwidth.lastUserValue));
	xmlWrtr.writeEndElement();
      }
    else
      {
	xmlWrtr.writeStartElement("outQueueTable");
	xmlWrtr.writeStartElement("swBandwidth");
	xmlWrtr.writeCharacters(String.valueOf(bandwidth.lastUserValue));
	xmlWrtr.writeEndElement();
      }

      int numQueues = queues.size();
      for (int i = 0; i < numQueues; ++i)
	{
	  Queue q = (Queue)queues.elementAt(i);
	  q.writeXML(xmlWrtr);
	}
      xmlWrtr.writeEndElement();
  }
  public void writeTable(ONL.Writer wr) throws IOException
    {
      wr.writeString(Boolean.toString(ingress));
      wr.writeDouble(bandwidth.lastUserValue);
      int numQueues = queues.size();
      wr.writeInt(numQueues);
      for (int i = 0; i < numQueues; ++i)
	{
	  Queue rt = (Queue)queues.elementAt(i);
	  wr.writeString(rt.toString());
	}
    }
  public void addQueues(QueueTable rt)
    {
      int max = rt.size();
      Queue elem;
      int start_index = queues.size();
      
      for (int i = 0; i < max; ++i)
	{
	  elem = rt.getQueueAt(i);
	  if (!containsQueue(elem)) 
            {
              if (ingress) queues.add(new VOQueue(elem, this));
              else queues.add(new Queue(elem, this));
            }
	}
      int end_index = queues.size() - 1;
      if (end_index >= start_index) //if something was added
	  tableModel.fireTableRowsInserted(start_index, end_index);
    }
  public boolean containsQueue(Queue r)
    {
      if (queues.contains(r)) return true;
      int max = queues.size();
      Queue elem;
      for (int i = 0; i < max; ++i)
	{
	  elem = (Queue)queues.elementAt(i);
	  if (elem.getQueueID() == r.getQueueID()) return true;
	}
      return false;
    }
  public void changeQueue(Queue r)
    {
      if (queues.contains(r)) 
        {
          fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_CHANGE, this, r, getIndex(r), null, "queue changed")); 
          tableModel.fireTableDataChanged();
        }
    }

  public ONLCTable.Element addElement(ONLCTable.Element elem, boolean can_repeat) { return (addQueue((Queue)elem, queues.size(), can_repeat));}
  public Queue addQueue(Queue r) { return(addQueue(r, queues.size(), false));}
  public Queue addQueue(Queue r, int s) { return(addQueue(r, s, false));}
  public Queue addQueue(Queue r, int s, boolean can_repeat)
    {
      if (r != null && (!containsQueue(r) || can_repeat)) 
 	{
	  queues.add(s, r);//new Queue(r, this));
          fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_ADD, this, r, s, null, "queue added")); 
	  tableModel.fireTableRowsInserted(s,s);
          return r;
	}
      else return null;
    }
  private Queue addNewQueue(Queue q)
  {
    Queue nq = getQueue(q.getQueueID());
    ExpCoordinator.print(new String("QueueTable.addNewQueue :" + q.toString()), 5);
    if (nq != null)
      {
	ExpCoordinator.print(new String( "  q" + nq.getQueueID() + " set thold=" + q.getThreshold() + " quantum=" + q.getQuantum()), 5);
	nq.setThreshold(q.getThreshold());
	nq.setQuantum(q.getQuantum());
	expCoordinator.addEdit(nq.getUpdateEdit());
	changeQueue(nq);
      }
    else
      {	
	nq = addQueue(q);
      }
    expCoordinator.addEdit(nq.getUpdateEdit());
    return nq;
  }
  private Queue addNewQueue()
  {
    Queue rtn = null;
    if (ingress) rtn = new VOQueue(this);
    else rtn = new Queue(this);
    int s = queues.size();
    queues.add(rtn);
    if (jtable != null) jtable.setRowSelectionInterval(s,s);
    fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_ADD, this, rtn, s, null, "queue added")); 
    tableModel.fireTableRowsInserted(s,s);
    return rtn;
  }
  public void removeQueue(Queue r)
    {
      if (r!= null && containsQueue(r))
	{
	  int max = queues.size();
	  Queue elem = null;
	  int removed = -1;
	  for (int i = 0; i < max; ++i)
	    {
	      elem = (Queue) queues.elementAt(i);
	      if ((elem == r) || elem.getQueueID() == r.getQueueID()) 
		{
		  removed = i;
		  break;
		}
	    }
	  if (removed >= 0) //should always be true
	    {
	      queues.remove(elem);
              fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_ADD, this, r, removed, null, "queue removed"));
	      tableModel.fireTableRowsDeleted(removed,removed);
	    }
	}
    }
  public Queue getQueueAt(int i) 
    { 
      if (i >= 0 && i < queues.size())
	{
	  return ((Queue)queues.elementAt(i));
	}
      return null;
    }
  public int getIndex(Queue q)
    {
      int max = queues.size();
      Queue elem;
      for (int i = 0; i < max; ++i)
        {
          elem = (Queue)queues.elementAt(i);
          if (elem.getQueueID() == q.getQueueID()) return i;
        }
      return -1;
    }
  public ONLCTable.Element getElement(int i) { return (getQueueAt(i));}
  public ONLCTable.Element getElementExact(Object o)
    { 
      int max = queues.size();
      ONLCTable.Element elem;
      for (int i = 0; i < max; ++i)
        {
          elem = (ONLCTable.Element)queues.elementAt(i);
          if (elem.toString().equals(o.toString())) return elem;
        }
      return null;
    }
  public Queue getQueue(int q) 
    { 
      int max = queues.size();
      Queue elem;
      for (int i = 0; i < max; ++i)
        {
          elem = (Queue)queues.elementAt(i);
          if (elem.getQueueID() == q) return elem;
        }
      return null;
    }
  public int size() { return (queues.size());}
  //private JFrame getFrame() 
  private JPanel getPanel() 
    {
      if (panel == null)
	{
	  if (port == null) ExpCoordinator.printer.print("QueueTable.getPanel port is null");
	  panel = new JPanel();
	  panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	  String ttl;
	  if (ingress) ttl = "VOQs";
          else ttl = "Egress Queues";
	  Border border = BorderFactory.createTitledBorder((BorderFactory.createLineBorder(Color.black)),ttl);
	  panel.setBorder(border); 
	  
	  jtable = new JTable(tableModel);
	  jtable.setDefaultRenderer(String.class, new ONLTableCellRenderer());
	  for (int col = 0; col < 2; ++col)
            {
              jtable.setDefaultRenderer(tableModel.getColumnClass(col), new ONLTableCellRenderer());
            }
	  jtable.setDefaultEditor(tableModel.getColumnClass(QUEUE), new DefaultCellEditor(new JFormattedTextField(new Queue.ID())));
	  jtable.setDefaultEditor(tableModel.getColumnClass(THRESHOLD), new DefaultCellEditor(new JFormattedTextField(new Queue.TholdRange())));
	  //turn off column selection
	  jtable.setColumnSelectionAllowed(false);
	  jtable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	  //jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	  jtable.setShowVerticalLines(true);
	  TableColumnModel tcm = jtable.getColumnModel();
	  if (ingress)
            {
              ExpCoordinator.print("  setting VOQueue.RateCellRenderer", 2);
              tcm.getColumn(QUANTUM).setCellRenderer(new VOQueue.RateCellRenderer(port));
            }
	  tcm.getColumn(QUANTUM).setPreferredWidth(50);
	  tcm.getColumn(THRESHOLD).setPreferredWidth(115);
	  tcm.getColumn(QUEUE).setPreferredWidth(70);
	  //tcm.getColumn(LENGTH).setPreferredWidth(50);
	  tcm.getColumn(QUANTUM).setMinWidth(50);
	  tcm.getColumn(THRESHOLD).setMinWidth(50);
	  tcm.getColumn(QUEUE).setMinWidth(65);
	  //tcm.getColumn(LENGTH).setMinWidth(50);
	  //panel.add(bandwidth,BorderLayout.PAGE_START);
	  JScrollPane pane = new JScrollPane(jtable);
	  //pane.setBorder(BorderFactory.createLineBorder(Color.black));
	  panel.add(pane, "Center");
	  jtable.setPreferredScrollableViewportSize(new Dimension(200,250));
	}
      return panel;
    }
  public void clear()
    {
      if (!queues.isEmpty())
	{
	  Vector del_rts = new Vector(queues);
	  int end = queues.size() - 1;
	  queues.removeAllElements();
	  tableModel.fireTableRowsDeleted(0, end);
	}
      //if (frame != null && frame.isShowing()) frame.dispose();
    }
  
  public void addOperation(NCCPOpManager.Operation op) 
    {
      if (opManager == null) 
	{
	  opManager = new OpManager();
	  ((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC)).addOpManager(opManager);
	}
      opManager.addOperation(op);
    }
  
  public void removeOperation(NCCPOpManager.Operation op) 
    {
      if (opManager != null)
	opManager.removeOperation(op);
      if (panel != null) panel.repaint();
    }
  public NSPPort getPort() { return port;}
  public boolean isIngress() { return ingress;}
  public Bandwidth getBandwidth() { return bandwidth;}
  public double getBandwidthVal() { return (bandwidth.getBandwidth());}
  protected void setDQ(boolean dq)
    {
      if (ingress)
        {
          bandwidth.setEditable(false);//!dq); //never edit
          if (!dq)
            {
              resetVOQs();
            }
          if (port.getID() == 0)
            expCoordinator.addEdit(new DQOperation(((NSPDescriptor)port.getParent()), this, dq));
        }
    }

  //interface PropertyChangeListener
  public void propertyChange(PropertyChangeEvent e)
  {
    if (ingress)
      { 
        if (e.getPropertyName().equals(NSPDescriptor.DQ))
          {
            boolean dq = Boolean.valueOf((String)e.getNewValue()).booleanValue();
            setDQ(dq);
          }
        if (e.getPropertyName().equals(NSPDescriptor.INTERNALBW))
          {
            String bw_str = (String)e.getNewValue();
            if (!bw_str.equals(bandwidth.getValue()))
              {
                bandwidth.setValue(bw_str);
                bandwidth.addEditForCommit();
                //if (!port.isDQon()) resetVOQs();
              }
          }
      }
  }      
  //end interface PropertyChangeListener

  public void resetVOQs()
    {
      if (ingress)
        {
          QueueTable outQT = port.getQueueTable(false);
          int numPorts = NSPDescriptor.NUMPORTS;
          double rate = 0; //get the link bandwidth for the ports
          if (outQT != null) 
              rate = outQT.getBandwidth().getBandwidth();
          else rate = bandwidth.getBandwidth()/numPorts;
          Queue q;
          rate = rate * 1000; //convert to Kbits
          for (int i = 0; i < numPorts; ++i)
            {
              q = (Queue)queues.elementAt(i);
              q.setQuantum((int)rate);
              changeQueue(q);
              expCoordinator.addEdit(q.getUpdateEdit());
            }
        }
    }
  public void resetSwitchBW()
    {
      ExpCoordinator.print("QT resetSwitchBW", 2);
      if (false)//ingress)
        {
          Queue q;
          int numPorts = NSPDescriptor.NUMPORTS;
          double rate = 0;
          for (int i = 0; i < numPorts; ++i)
            {
              q = (Queue)queues.elementAt(i);
              rate += q.getQuantum();
            }
          rate = rate/1000; //convert to Mbits
   
          ExpCoordinator.print(new String("set to " + rate), 2);
          bandwidth.setValue(String.valueOf(rate));
          bandwidth.addEditForCommit();
        }
    }
  public void processEvent(ONLComponent.Event event)
  {
    if (event instanceof ONLComponent.PropertyEvent)
      {
        PropertyEvent pevent = (PropertyEvent)event;
        if (pevent.getLabel().equals(BANDWIDTH)) 
          {
            String val = pevent.getNewValue();
            if (val != null && val.length() > 0)
              bandwidth.setValue(val);
            return;
          }
      }

    ONLComponent.TableEvent tevent = null;
    if (event instanceof ONLComponent.TableEvent)
      tevent = (ONLComponent.TableEvent)event;
    int tp = event.getType();
    
    try
      {
        switch(tp)
          {
          case ONLComponent.Event.TABLE_CHANGE:
            Queue oldQueue = getQueueAt(tevent.getIndex());
            if (oldQueue != null)
              {
                oldQueue.parseString(tevent.getNewValue());
                changeQueue(oldQueue);
                break;
              }
          case ONLComponent.Event.TABLE_ADD:
            Queue newQueue = addQueue(new Queue(tevent.getNewValue(), this), tevent.getIndex());
            if (newQueue != null) newQueue.setCommitted(tevent.isCommitted());
            break;
          case ONLComponent.Event.TABLE_DELETE:
            removeQueue(getQueueAt(tevent.getIndex()));
            break;
          case ONLComponent.Event.TABLE_COMMIT:
            getQueueAt(tevent.getIndex()).setCommitted(true);
            break;
          default:
            super.processEvent(event);
          }
      }
    catch (java.text.ParseException pe)
      {
        System.err.print("QueueTable.processEvent ParseException");
      }
    
    //super.processEvent(event);
  }
  public void show()
  {
    ((QTAction)port.getAction(NSPPort.QTACTION)).actionPerformed(null);
  }

  public void fireCommitEvent(Queue q)
    {
      if (queues.contains(q)) 
        fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_COMMIT, this, q, getIndex(q), null, "Queue committed")); 
    }

  public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new QTContentHandler(exp_xml, this));}
  protected boolean merge(ONLComponent c)
  {
    if ((c.getType() == getType()) && (c instanceof QueueTable))
      {
	QueueTable t2 = (QueueTable)c;
	int max = queues.size();
	ONLCTable.Element elem1;
	ONLCTable.Element elem2;
	int i;
	boolean differences = false;
	for (i = 0; i < max; ++i)
	  {
	    elem1 = getElement(i);
	    elem2 = t2.getElementExact(elem1);
	    if (elem2 == null) 
	      {
		elem1.setState(ONLComponent.IN1);
		differences = true;
	      }
	    else elem1.setState(ONLComponent.INBOTH);
	  }
	max = t2.size();
	for (i = 0; i < max; ++i)
	  {
	    elem2 = t2.getElement(i);
	    elem1 = getElementExact(elem2);
	    if (elem1 == null) 
	      {
		elem2.setState(ONLComponent.IN2);
		differences = true;
		addElement(elem2, true);
	      }
	  }
	if (bandwidth.getBandwidth() != t2.bandwidth.getBandwidth())
	  {
	    bandwidth.setState(ONLComponent.IN1);
	    bandwidth.merge(t2.bandwidth);
	    differences = true;
	  }
	else bandwidth.setState(ONLComponent.INBOTH);
	if (differences)
	  {
	    ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("Queue Table Inconsistency for " + getLabel() + "\n")),
									   "Table Inconsistency",
									   "Table Inconsistency",
									   StatusDisplay.Error.LOG)); 
	  }
	return true;
      }
    else return false;
  }
}
