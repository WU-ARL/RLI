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
 * File: MonitorTable.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 11/08/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import javax.swing.table.*;
import java.awt.Dimension;
import java.lang.reflect.Array;
import java.io.*;
import java.util.*;

//no x or y axes table of counters
public class MonitorTable extends Graph 
{
  //////////////////////////////////// DataDisplay ////////////////////////////////////////////////////////////////////
  private class DataDisplay extends Graph.DataDisplay
  {
    private Vector data = null;
    private JTable jtable = null;
    private TableModel tableModel = null;

    /////////////////////////////////////// DataDisplay.Parameter //////////////////////////////////////////////////////////////////////////////////
    private class Parameter implements MouseListener
    {
      private MonitorPlus monitor = null;
      private boolean updating = true;
      private ONLComponent.ONLMonitorable onlMonitorable = null;
      private String label = "";
      private double currentVal = 0;
      private MonitorTable monitorTable = null;
      
      public Parameter(MonitorPlus m, MonitorTable mt)
      {
        monitor = m;
        MonitorDataType.Base mdt = monitor.getDataType();
        label = mdt.getName();
        onlMonitorable = mdt.getMonitorable();
        //if (onlMonitorable == null) ExpCoordinator.print(new String("MonitorTable.Parameter for " + label + " onlMonitorable is null"), 2);
        //else ExpCoordinator.print(new String("MonitorTable.Parameter for " + label + " onlMonitorable not null"), 2);
        monitorTable = mt;
      }
      public void updateValue()
      {
        //if (!updating)
        //{
        ExpCoordinator.print(new String("MonitorTable.Parameter.updateValue for " + label + " updating " + updating), 2);
        updating = true;
        onlMonitorable.addMonitor(monitor);
        monitor.addMonitorFunction(monitorTable);
            //}
      }
      public void setValue()
      {
        ExpCoordinator.print(new String("MonitorTable.Parameter.setValue for " + label + " updating " + updating), 2);
        if (updating)
          {
            updating = false;
            monitor.removeMonitorFunction(monitorTable);
            //double oval = currentVal;
            //currentVal = monitor.getLastElement().getY();
            //if (oval != currentVal) 
            changeParameter(this);
          }
      }
      public void mouseEntered(MouseEvent e){}
      public void mouseExited(MouseEvent e){}
      public void mousePressed(MouseEvent e){}
      public void mouseReleased(MouseEvent e){}
      public void mouseClicked(MouseEvent e)
      {
        //if (e.getClickCount() >= 2)
        //{
        MonitorDataType.DescriptionDialog desc = new MonitorDataType.DescriptionDialog(monitorTable);
        int rtn = desc.showDescription(monitor.getDataType());
        
        JMenuItem mi = monitorTable.getRmMenuItem(monitor);
        label = monitor.getDataType().getName();
        mi.setText(label);
        changeParameter(this);
        //  }
      }
      public double getValue() 
        { 
          NumberPair elem = monitor.getLastElement();
          if (elem != null) currentVal = elem.getY();
          return currentVal;
        }
      public MonitorPlus getData() { return monitor;}
      public String getLabel() { return label;}
    }

    /////////////////////////////// DataDisplay.TableModel /////////////////////////////////////////////////////////////
    private class TableModel extends AbstractTableModel 
    {
      private Class types[] = null;
      public TableModel() 
      { 
        super();
        types = new Class[]{String.class, Double.class};
      }
      public int getColumnCount() { return 2;}
      public Class getColumnClass(int c) { return types[c];}
      public int getRowCount() { return (data.size());}
      public String getColumnName(int ndx)   
      {
        if (ndx == 0) return ("Parameter");
        else return ("Data");
      }
      public boolean isCellEditable(int r, int c) { return false;}
      public int findColumn(String nm)
      {
        if (nm.equals("Parameter")) return 0;
        else return 1;
      }
      public void setValueAt(Object a, int r, int c){}
      public Object getValueAt(int r, int c)
      {
        Object rtn = null;
        Parameter p = (Parameter)data.elementAt(r);
        if (c == 0) rtn = p.getLabel();
        else rtn = new Double(p.getValue());
        return rtn;
      }
    }

    ///////////////////////////////////// DataDisplay ////////////////////////////////////////////////////////////////////////////
    public DataDisplay(MonitorTable mt)
    {
      super(mt);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      data = new Vector();
      tableModel = new TableModel();
      jtable = new JTable(tableModel);
      TableColumnModel tcm = jtable.getColumnModel();
      tcm.getColumn(0).setPreferredWidth(70);
      tcm.getColumn(1).setPreferredWidth(70);
      tcm.getColumn(0).setMinWidth(70);
      tcm.getColumn(1).setMinWidth(70);
      jtable.setPreferredScrollableViewportSize(new Dimension(200,250));
      jtable.addMouseListener(new MouseAdapter()
        {
          public void mouseClicked(MouseEvent e)
          {
            int ndx = jtable.getSelectedRow();
            ((Parameter)data.elementAt(ndx)).mouseClicked(e);
          }
        });
      add((new JScrollPane(jtable)), "Center");
      JButton button = new JButton(new AbstractAction("Update")
        { 
          public void actionPerformed(ActionEvent e) { update();}
        });
      add(button);
    }
    public void addData(MonitorPlus d)
      {
        Parameter p = getParameter(d);
        if (p == null) 
          {
            p = new Parameter(d, (MonitorTable)graph);
            int s = data.size();
            data.add(p);
            tableModel.fireTableRowsInserted(s,s);
            ExpCoordinator.print(new String("MonitorTable.DataDisplay.addData d.isStarted() " + d.isStarted()),2);
            if (d.isStarted()) 
              {
                p.setValue();
                p.updateValue();
              }
          }
      }
    public void removeData(MonitorPlus d)
      {
        int p = getIndex(d);
        if (p >= 0) 
          {
            data.removeElementAt(p);
            tableModel.fireTableRowsDeleted(p,p);
          }
      }
    public void removeAllElements() 
      { 
        int max = data.size();
        data.removeAllElements();
        tableModel.fireTableRowsDeleted(0,max);
      }
    public int length() { return (data.size());} //returns the number of data structures
    public MonitorPlus getData(int ndx) 
      {
        //ExpCoordinator.print(new String("MonitorTable.DataDisplay.getData index = " + ndx + " data.size " + data.size()), 5);
        if (ndx >= 0 && ndx < data.size()) 
          {
            MonitorPlus mp = ((Parameter)data.elementAt(ndx)).getData();
            //if (mp == null) 
            //  ExpCoordinator.print("     monitor is null", 5);
            //else
            // {
            //   String nm = "null";
            //    if (mp.getDataType() != null) nm = mp.getDataType().getName();
            //   ExpCoordinator.print(new String("     monitor " + nm), 5);
            // }
            return mp;
          }
        else return null;
      }
    private int getIndex(MonitorPlus mp)
      {
        int max = data.size();
        Parameter rtn = null;
        for (int i = 0; i < max; ++i)
          {
            rtn = (Parameter)data.elementAt(i);
            if (mp == rtn.getData()) return i;
          }
        return -1;
      }
    private Parameter getParameter(MonitorPlus mp)
      {
        int max = data.size();
        Parameter rtn = null;
        for (int i = 0; i < max; ++i)
          {
            rtn = (Parameter)data.elementAt(i);
            if (mp == rtn.getData()) return rtn;
          }
        return null;
      }
    private void changeParameter(Parameter p)
      {
        if (data.contains(p)) tableModel.fireTableDataChanged(); 
      }
    public void update()
      {
        int max = data.size();
        for (int i = 0; i < max; ++i)
          {
            ((Parameter)data.elementAt(i)).updateValue();
          }
      }
    public void updateValues()
      {
        int max = data.size();
        for (int i = 0; i < max; ++i)
          {
            ((Parameter)data.elementAt(i)).setValue();
          }
      }
    public boolean containsData(MonitorPlus mp) { return (getParameter(mp) != null);}
  }
  //////////////////////////////////// MonitorTable ////////////////////////////////////////////////////////////////////
  public MonitorTable(MonitorManager bwd)
    {
      super(bwd, 0, 0, MonitorManager.Display.ONESHOT_TABLE);
      dataDisplay = new DataDisplay(this);
      getContentPane().add(dataDisplay);
      setBoxedRange(new Graph.BoxedRange(this)
        {
          public void changeBoundaries(BoxedRangeEvent e)
          {
            updateData((MonitorPlus)e.getSource());
          }
          public void changeVisible(BoxedRangeEvent e)
          {
            updateData((MonitorPlus)e.getSource());
          }
        });
    }
  public void setZoomable(boolean b) { super.setZoomable(false);}
  public boolean isZoomable() { return false;}
  
  public void addData(MonitorPlus dm)
    {
      if (!((MonitorTable.DataDisplay)dataDisplay).containsData(dm))
        {
          super.addData(dm);
          ExpCoordinator.print(new String("MonitorTable.addData " + dm.getDataType().getName()), 2);
        }
    }
  private void updateData(MonitorPlus mp)
    {
      DataDisplay.Parameter p = ((MonitorTable.DataDisplay)dataDisplay).getParameter(mp);
      if (p != null) p.setValue();
    }
}
