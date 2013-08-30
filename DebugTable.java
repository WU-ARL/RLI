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
 * File: DebugTable.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 2/3/2006
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.*;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import java.util.*;
import java.text.ParseException;

public class DebugTable
{
  private NSPDescriptor nsp = null;
  private ExpCoordinator expCoordinator = null;
  protected static final String PORT = "port";
  protected static final String D_LEVEL = "debug level";
  private final int NUMPORTS = NSPDescriptor.NUMPORTS;
  
  private JTable jtable = null;
  private TableModel tableModel = null;
  private JFrame frame = null;
  private JCheckBox debugCheckBox = null;
  private TextFieldwLabel logFile = null;

  private State committedState = null;
  private State currentState = null;

  private Edit edit = null;

  private OpManager opManager = null;
  public static final int PORT_NDX = 0;
  public static final int DLVL_NDX = 1;


  private class State
  {
    protected boolean monmsgsOn = false;
    protected String logFile = "/tmp/debug.log";
    protected DebugLevel[] portLevels;
    
    public State(State s) 
     {
       this();
       setState(s);
     }
    public State() 
     {
       int max = NUMPORTS;
       portLevels = new DebugLevel[max];
       for (int i = 0; i < max; ++i)
         {
           portLevels[i] = new DebugLevel(DebugLevel.CRITICAL, i);
         }
     }

    public boolean isEqual(State s)
     {
       boolean ports_eq = true;
       int max = NUMPORTS;
       for (int i = 0; i < max; ++i)
         {
           if (portLevels[i].getType() != s.portLevels[i].getType()) 
             {
               ports_eq = false;
               break;
             }
         }
       return (s.monmsgsOn == monmsgsOn &&
               s.logFile.equals(logFile) && 
               ports_eq);
               
     }
    public void setState(State st)
      {
        monmsgsOn = st.monmsgsOn;
        logFile = new String(st.logFile);
        int max = NUMPORTS;
        for (int i = 0; i < max; ++i)
          {
            portLevels[i].setType(st.portLevels[i].getType());
          }
      }
    public void print(int lvl)
      {
        ExpCoordinator.print(new String("on:" + monmsgsOn + " portLevels:(" + portLevels[0] + ", "
                                        + portLevels[1] + ", "
                                        + portLevels[2] + ", "
                                        + portLevels[3] + ", "
                                        + portLevels[4] + ", "
                                        + portLevels[5] + ", "
                                        + portLevels[6] + ", "
                                        + portLevels[7] + ")"), lvl);
      }
  }

  private class DebugLevel implements ONLTableCellRenderer.Committable
  {
    public static final int CRITICAL = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 2;
    public static final int TRACE = 3;
    public static final int INFO = 4;
    public static final int VERBOSE = 5;
    private int type = CRITICAL;
    private int port = 0;
    private String state = "";

    public DebugLevel(String str)  throws java.text.ParseException
     {
       parseString(str);
     } 
    public DebugLevel() {}
    public DebugLevel(int t) { type = t;}
    public DebugLevel(int t, int p) 
     { 
       if (t >= CRITICAL && t <= VERBOSE) type = t;
       port = p;
     }
    public String toString()
      {
        switch(type)
          {
          case CRITICAL:
            return "critical";
          case ERROR:
            return "error";
          case WARNING:
            return "warning";
          case TRACE:
            return "trace";
          case INFO:
            return "info";
          case VERBOSE:
            return "verbose";
          }
        return "unknown";
      }
    public int getPort() { return port;}
    public void setPort(int p) { port = p;}
    public int getType() { return type;}
    public void setType(int t)
     {
       if (t >= CRITICAL && t <= VERBOSE) type = t;
     } 
    public void setType(String s)
     {
       try
         {
           parseString(s);
         }
       catch (ParseException e) {}
     }
      public void parseString(String s) throws java.text.ParseException
      {
        int t= -1;
       if (s.equals("critical")) t = CRITICAL;
       if (s.equals("error")) t = ERROR;
       if (s.equals("warning")) t = WARNING;
       if (s.equals("trace")) t = TRACE;
       if (s.equals("info")) t = INFO;
       if (s.equals("verbose")) t = VERBOSE;
       if (t >= 0) type = t;
       else
         throw new java.text.ParseException("DebugTable.DebugLevel not a valid string", 0);
     } 
    //begin interface ONLTableCellRenderer.Committable
    public boolean needsCommit()
    {
      return (type != committedState.portLevels[port].type);
    }
    //end interface ONLTableCellRenderer.Committable
    public String getState() { return state;}
    public void setState(String s) { state = new String(s);}
  }

    private class Edit extends AbstractUndoableEdit implements NCCPOpManager.Operation,ONLComponent.Undoable
    {
      private NCCP_Requester request = null;
      private State oldState = null;
      private boolean committed = false;
      
      private class NCCP_Requester extends NCCP.RequesterBase
      {
        private State state = null;
        public NCCP_Requester(State cstate)
        {
          super(NCCP.Operation_Debug, true);
          state = cstate;
          setMarker(new REND_Marker_class());
        }
        
        public void storeData(DataOutputStream dout) throws IOException
        {
          ONL.NCCPWriter writer = new ONL.NCCPWriter(dout);
          writer.writeBoolean(state.monmsgsOn);
          writer.writeString(state.logFile);
          writer.writeInt(NUMPORTS);
          for (int i = 0; i < NUMPORTS; ++i)
            writer.writeShort(state.portLevels[i].getType());
        }
        public State getSent() { return state;}
      }

      public Edit()
        {
          oldState = new State();
        }
      public void opSucceeded(NCCP.ResponseBase resp) 
        {
          ExpCoordinator.printer.print(new String(nsp.getLabel() + "DebugTable.Edit.opSucceeded - " + request.getSent().monmsgsOn), 2);
          removeOperation(this);
        }
      public void opFailed(NCCP.ResponseBase resp)
        {
          if (request.getSent().monmsgsOn)
            ExpCoordinator.printer.print(new String(nsp.getLabel() + ":Debugging failed to turn on"), 2);
          else
            ExpCoordinator.printer.print(new String(nsp.getLabel() + ":Debugging failed to turn off"), 2);
          expCoordinator.addError(new StatusDisplay.Error(new String("Failure on " + nsp.getLabel() + ": " + ((NCCP.ErrorMsgResponse)resp).getErrorMsg()), 
                                                          new String("Debugging Failure on " + nsp.getLabel()),
                                                          "Debugging Failure",
                                                          (StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR)));
          if (request.getSent().isEqual(committedState)) committedState.setState(oldState);
          removeOperation(this);
        }
      public Vector getRequests() //return list of requests to send
        {
          Vector rtn = new Vector();
          rtn.add(request);
          return rtn;
        }
      public boolean containsRequest(REND_Marker_class rend) { return (request != null && request.getMarker().isEqual(rend));}
      //start interface ONLComponent.Undoable
      public void commit()
        { 
          //ExpCoordinator.printer.print("Experiment.Edit.commit");
          if (!committed)
            {
	      //ExpCoordinator.printer.print("calling sendMessage");
              sendMessage();
              //do the real thing and send a message
            }
          committed = true;
        }
      public boolean canUndo() { return false;}
      public boolean canRedo() { return false;}
      public void actionPerformed(ActionEvent e){}
      public boolean isCancelled(ONLComponent.Undoable un) { return false;}
      protected void sendMessage() 
      {
        ExpCoordinator.print(new String("DebugTable.Edit.sendMessage " + nsp.getLabel()), 3);
        if (nsp.getONLDaemon(ONLDaemon.NSPC) != null) //if daemon is available 
          {
            if ((committedState.monmsgsOn || currentState.monmsgsOn) && !currentState.isEqual(committedState))
              {
                oldState.setState(committedState);
                updateCurrentState();
                State st = new State(currentState);
                committedState.setState(st);
                request = new NCCP_Requester(st);
                addOperation(this);//add the operation
              }
            else
              {
                ExpCoordinator.print(new String("DebugTable.Edit.sendMessage nothing to send"), 3);
                currentState.print(3);
                committedState.print(3);
              }
          }
	else System.err.println("DebugTable.Edit no daemon");
        resetEdit();
      }
    // interface ONLComponent.Undoable
  }

  private class OpManager extends NCCPOpManager
  {
    public OpManager()
      {
	super((MonitorDaemon)nsp.getONLDaemon(ONLDaemon.NSPC));
	response = new NCCP.ErrorMsgResponse();
      }
    protected void setRendMarker(NCCP.RequesterBase req, NCCPOpManager.Operation op, int i)
      {
	req.setMarker(new REND_Marker_class(req.getOp(), i));
      }
  }

  //class TableModel
  private class TableModel extends AbstractTableModel implements ONLTableCellRenderer.CommittableTable
  {
    private Class types[] = null;
    private DebugTable debugTable = null;
    public TableModel(DebugTable dtable)
      {
	super();
	debugTable = dtable;
        DebugLevel dlevel = new DebugLevel();
	types = new Class[]{
	  Integer.class, dlevel.getClass()
	    };
      }
    public int getColumnCount() { return 2;}
    public Class getColumnClass(int c) 
      { return types[c];}
    public int getRowCount() { return (NSPDescriptor.NUMPORTS);}
    public String getColumnName(int ndx)
      {
	String nm = null;
	switch(ndx)
	  {
	  case 0:
	    nm = PORT;
	    break;
	    /*
	      case 2:
	      nm = MASK;
	      break;*/
	  case 1:
	    nm = D_LEVEL;
	    break;
	  }
	return nm;
      }
    public boolean isCellEditable(int r, int c)     
      {
	//check if field set in route if not let it be editable
	//if we're filling in a new route can edit the prefix and the mask too
        return (c != PORT_NDX  && (!ExpCoordinator.isObserver()));
      }
  public int findColumn(String nm)
    {
      if (nm.equals(PORT)) return PORT_NDX;
      if (nm.equals(D_LEVEL)) return DLVL_NDX;
      System.err.println("DebugTable::findColumn not a recognized string");
      return 0;
    }
  public void setValueAt(Object a, int r, int c)
    {
      if (c == DLVL_NDX)
        {
          currentState.portLevels[r].setType(a.toString());
        }
      if (!expCoordinator.isCurrentEdit(edit))
        {
          expCoordinator.addEdit(edit);
        }
      fireTableCellUpdated(r,c);
      fireTableRowsUpdated(c,c);
    }
  public Object getValueAt(int r, int c)
    {
      Object rtn = null;
      if (c == DLVL_NDX)
        return (currentState.portLevels[r]);
      else return (new Integer(r));
    }
  // begin interface ONLTableCellRenderer.CommitableTable
    public ONLTableCellRenderer.Committable getCommittableAt(int row)
    {
      return ((ONLTableCellRenderer.Committable)currentState.portLevels[row]);
    }
  // end interface ONLTableCellRenderer.CommitableTable
  } //class TableModel


  public static class DTAction extends AbstractAction
  {
    private NSPDescriptor nsp = null;
    private JFrame table = null;
    private DebugTable debugTable = null;
    public DTAction(String ttl, NSPDescriptor n) 
      {
	super(ttl);
        nsp = n;
	debugTable = nsp.getDebugTable();
	if (debugTable == null) ExpCoordinator.print("DTAction debugTable is null", 1);
      }
    public DTAction(NSPDescriptor n) 
      {
        this(new String(n.getLabel() + ":Debug Table"), n);
      }
    public void actionPerformed(ActionEvent e) 
      {
        ExpCoordinator.print("DTAction.actionPerformed", 2);
	if (table == null)
	  {
	    if (debugTable == null) 
              {
               ExpCoordinator.print("DTAction.actionPerformed debugTable is null", 10);
	       debugTable = nsp.getDebugTable();
	      }
	    table = debugTable.getFrame();
	  }
	table.setSize(250,250);
        table.setVisible(true);
      }
  }

  public DebugTable(NSPDescriptor n)
    {
      nsp = n;
      expCoordinator = ExpCoordinator.theCoordinator;
      currentState = new State();
      committedState = new State();
      tableModel = new TableModel(this);
      edit = new Edit();
    }

  private JFrame getFrame() 
    {
      if (frame == null)
	{
	  if (nsp == null) ExpCoordinator.print("DebugTable.getFrame nsp is null");
	  frame = new JFrame(new String(nsp.getLabel() + ":Debug"));  

          JPanel panel = new JPanel();
	  panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
          JPanel panel2 = new JPanel();
	  panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
          panel2.setAlignmentX(Component.LEFT_ALIGNMENT);
          debugCheckBox = new JCheckBox(new AbstractAction(""){
              public void actionPerformed(ActionEvent e)
                {
                  currentState.monmsgsOn = debugCheckBox.isSelected();
                  if (!expCoordinator.isCurrentEdit(edit))
                      expCoordinator.addEdit(edit);
                }
            });
          JLabel tmp_lbl = new JLabel("Debug On:");
          tmp_lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
          debugCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
          panel2.add(tmp_lbl);
          panel2.add(debugCheckBox);
          JTextField txt_fld = new JTextField(75);
          logFile = new TextFieldwLabel(txt_fld, "log to file:");
          panel.add(panel2);
          logFile.setText(currentState.logFile);
          txt_fld.addActionListener(new AbstractAction() {
              public void actionPerformed(ActionEvent e)
                {
                  currentState.logFile = new String(logFile.getText());
                  if (!expCoordinator.isCurrentEdit(edit))
                    expCoordinator.addEdit(edit);
                }
            });
          //panel.add(new JLabel("logs to file /tmp/debug.log on CP"));
          logFile.setAlignmentX(Component.LEFT_ALIGNMENT);
          //removed because right now will write to fixed file
          panel.add(logFile);
	  
	  jtable = new JTable(tableModel);
          jtable.setAlignmentX(Component.LEFT_ALIGNMENT);
	  jtable.setDefaultRenderer(String.class, new ONLTableCellRenderer());
	  for (int col = 0; col < 2; ++col)
	    jtable.setDefaultRenderer(tableModel.getColumnClass(col), new ONLTableCellRenderer());
	  
	  //create combo box for debug levels
          DebugLevel[] dlvls = { new DebugLevel(0),
                                 new DebugLevel(1),
                                 new DebugLevel(2),
                                 new DebugLevel(3),
                                 new DebugLevel(4),
                                 new DebugLevel(5)};
	  JComboBox cb = new JComboBox(dlvls);
	  cb.setEditable(false);
	  jtable.setDefaultEditor(tableModel.getColumnClass(DLVL_NDX), new DefaultCellEditor(cb));
	  //turn off column selection
	  jtable.setColumnSelectionAllowed(false);
	  //jtable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	  jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	  jtable.setShowVerticalLines(true);
	  TableColumnModel tcm = jtable.getColumnModel();
	  tcm.getColumn(0).setPreferredWidth(50);
	  tcm.getColumn(1).setPreferredWidth(70);
	  tcm.getColumn(0).setMinWidth(50);
	  tcm.getColumn(1).setMinWidth(70);
          panel.add((new JScrollPane(jtable)), "Center");
	  frame.getContentPane().add(panel);
	  jtable.setPreferredScrollableViewportSize(new Dimension(150,200));
	}
      return frame;
    }
  
  public void addOperation(NCCPOpManager.Operation op) 
    {
      if (opManager == null) 
	{
	  opManager = new OpManager();
	  ((MonitorDaemon)nsp.getONLDaemon(ONLDaemon.NSPC)).addOpManager(opManager);
	}
      opManager.addOperation(op);
    }
  
  public void removeOperation(NCCPOpManager.Operation op) 
    {
      if (opManager != null)
	opManager.removeOperation(op);
      if (jtable != null) jtable.repaint();
    }
  protected void clear()
    {
      if (frame != null) frame.dispose();
    }
  private void resetEdit() { edit = new Edit();}
  private void updateCurrentState()
    {
      currentState.monmsgsOn = debugCheckBox.isSelected();
      currentState.logFile = new String(logFile.getText());
    }
}
