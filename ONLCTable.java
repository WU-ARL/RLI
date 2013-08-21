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
 * File: ONLCTable.java
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
import javax.swing.JTextArea;
import java.awt.Color;
import javax.xml.stream.*;

public abstract class ONLCTable extends ONLComponent implements IDAssigner.IDable,NCCPOpManager.Manager
{
	public final static int TEST_REMOVE = HWTable.TEST_HWTABLE;
	public final static int TEST_ADD = 3;//HWTable.TEST_HWTABLE;
	public final static int TEST_GET = 5;
	public final static int TEST_CELLRENDERER = 8;
	private Vector list = null;
	protected TableModel tableModel = null;
	protected TablePanel panel = null;
	protected JTable jtable = null;
	private NCCPOpManager opManager = null;

	public static class TablePanel extends JPanel
	{
		private JTable jtable = null;
		public TablePanel(JTable jt) 
		{ 
			super();
			jtable = jt;
		}
		public void setJTable(JTable jt) { jtable = jt;}
		public JTable getJTable() { return jtable;}
	}

	public static class IntRange
	{
		private int hi = 0;
		private int lo = 0;
		private int value = 0;
		private int valDefault = 0;

		public IntRange (int v, int l, int h, int d)
		{
			hi = h;
			lo = l;
			valDefault = d;
			if (v >= lo && v <= hi) value = v;
			else value = valDefault;
		}
		public IntRange(IntRange ir){}
		public IntRange(String s, int l, int h) throws java.text.ParseException
		{
			this(s,l,h,h);
		}
		public IntRange(String s, int l, int h, int d) throws java.text.ParseException
		{
			value = d;
			parseString(s);
		}
		public String toString() { return (String.valueOf(value));}
		public void setValue(String s)
		{
			try
			{
				parseString(s);
			}
			catch (java.text.ParseException e){}
		}
		protected void parseString(String s) throws java.text.ParseException
		{
			int q = Integer.parseInt(s);
			if (q <= lo || q >= hi)
				throw new java.text.ParseException(new String("ONLCTable.IntRange parse error: not valid int must be " + lo + "-" + hi), 0);
			else value = q;
		}
		public int getValue() { return value;}
	}

	public static interface Element extends ONLTableCellRenderer.Committable
	{
		public boolean isWritable();
		public String toString();
		public void write(ONL.Writer wrtr) throws java.io.IOException;
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException;
		public ONLComponent getTable();
		public void parseString(String str) throws java.text.ParseException;//throws java.io.IOException;
		//public Object getField(int c);
		public Object getDisplayField(int c);
		public boolean isEditable(int c);
		public void setField(int c, Object a);
		public void setCommitted(boolean b);
		//public boolean needsCommit(); //ONLTableCellRenderer.Committable
	}//interface Elements 

	/////////////////////////////////////////////////////// MultiLineHeader ///////////////////////////////////////////////////////////////////
	public static class MultiLineHeader extends JPanel implements TableCellRenderer
	{
		public MultiLineHeader() 
		{ 
			super();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			removeAll();
			invalidate();
			if (value == null) return this;
			if (table != null)
			{
				JTableHeader header = table.getTableHeader();
				if (header != null) 
				{
					setForeground(header.getForeground());
					setBackground(header.getBackground());
					setFont(header.getFont());
				}
			}
			Object[] values = null;
			if (value instanceof Object[])
				values = (Object[]) value;
			else
			{
				values = new Object[1];
				values[0] = value;
			}
			int max = values.length;

			for (int i = 0; i < max; ++i)
			{
				JLabel lbl = new JLabel(values[i].toString());
				lbl.setForeground(getForeground());
				lbl.setBackground(getBackground());
				lbl.setOpaque(false);
				lbl.setFont(getFont());
				lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
				add(lbl);
			}
			add(Box.createVerticalGlue());
			return this;
		}
		public boolean isOpaque() { return false;}
		//public void validate() {}
		public void revalidate() {}
		public void firePropertyChange() {}
		public void repaint(Rectangle r) {}
	}


	/////////////////////////////////////////////////////// MultiLineCell ///////////////////////////////////////////////////////////////////
	public static class MultiLineCell extends JPanel implements TableCellRenderer
	{
		private Color uncommittedColor = null;
		public MultiLineCell(Color c) 
		{ 
			super();
			uncommittedColor = c;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//setVerticalAlignment(SwingConstants.TOP);
			//setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			removeAll();
			invalidate();
			if (value == null || table == null) return this;
			Color cellForeground = table.getForeground();
			Color cellBackground = table.getBackground();

			if (isSelected) 
			{
				cellForeground = table.getSelectionForeground();
				cellBackground = table.getSelectionBackground();
			}

			if (hasFocus)
			{     
				setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
				if (table.isCellEditable(row, column))
				{
					cellForeground = UIManager.getColor("Table.focusCellForeground");
					cellBackground = UIManager.getColor("Table.focusCellBackground");
				}
			}
			else setBorder(new EmptyBorder(1,2,1,2));


			ONLTableCellRenderer.Committable row_elem = ((ONLTableCellRenderer.CommittableTable)table.getModel()).getCommittableAt(row);
			if (row_elem != null && row_elem.needsCommit())
				cellForeground = uncommittedColor;

			setForeground(cellForeground);
			setBackground(cellBackground);
			Object[] values = null;
			if (value instanceof Object[])
			{
				values = (Object[]) value;
				ExpCoordinator.print(new String("ONLCTable.getTableCellRendererComponent is array length:" + values.length), TEST_CELLRENDERER);
			}
			else
			{
				values = new Object[1];
				values[0] = value;
			}
			int max = values.length;
			for (int i = 0; i < max; ++i)
			{
				if (values[i] instanceof JComponent)
					add((JComponent)values[i]);
				else
				{
					JLabel lbl = new JLabel(values[i].toString());
					lbl.setForeground(cellForeground);
					lbl.setBackground(cellBackground);
					lbl.setOpaque(false);
					lbl.setFont(getFont());
					lbl.setAlignmentX(getAlignmentX());
					add(lbl);
				}
			}
			add(Box.createVerticalGlue());
			return this;
		}
		public boolean isOpaque() { return true;}
		//public void validate() {}
		public void revalidate() {}
		public void firePropertyChange() {}
		public void repaint(Rectangle r) {}
	}

	//////////////////////////////////////////////////// ONLCTable.TableModel ////////////////////////////////////////////////////////////
	//class TableModel
	protected static abstract class TableModel extends AbstractTableModel implements ONLTableCellRenderer.CommittableTable
	{
		private Class types[] = null;
		private String names[] = null;
		protected int preferredW[] = null;//array of preferred column widths this set min and preferred width
		private int numCols = 0;
		protected ONLCTable onlcTable = null;
		public TableModel(ONLCTable rtable)
		{
			super();
			onlcTable = rtable;
			types = onlcTable.getColumnTypes();
			names = onlcTable.getColumnNames();
			numCols = Array.getLength(names);
			preferredW = new int[numCols];
			for (int i = 0; i < numCols; ++i)
			{
				preferredW[i] = 100;
			}
		}
		public int getColumnCount() { return numCols;} //4;}
		public Class getColumnClass(int c) 
		{ return types[c];}
		public int getRowCount() { return (onlcTable.list.size());}
		public String getColumnName(int ndx) { return (names[ndx]);}
		public int findColumn(String nm)
		{
			for (int i = 0; i < numCols; ++i)
			{
				if (nm.equals(names[i])) return i;
			}
			System.err.println("ONLCTable.TableModel.findColumn not a recognized string");
			return 0;
		}
		//have to define
		//public abstract boolean isCellEditable(int r, int c);    
		//public abstract void setValueAt(Object a, int r, int c);
		//public abstract Object getValueAt(int r, int c);
		//end have to define

		// begin interface ONLTableCellRenderer.CommittableTable
		public ONLTableCellRenderer.Committable getCommittableAt(int row)
		{
			return ((ONLTableCellRenderer.Committable)onlcTable.list.elementAt(row));
		}
		// end interface ONLTableCellRenderer.CommittableTable

		public boolean isCellEditable(int r, int c)
		{
			if (ExpCoordinator.isObserver()) 
			{
				//if (onlcTable instanceof NPRPluginTable) 
				ExpCoordinator.print(new String("ONLCTable.isCellEditable isObserver " + onlcTable.getLabel() + " (" +r + "," + c + ")"), 8);
				return false;
			}
			else 
				ExpCoordinator.print(new String("ONLCTable.isCellEditable " + onlcTable.getLabel() + " (" +r + "," + c + ")"), 8);
			Element elem = (Element)onlcTable.list.elementAt(r);
			if (elem != null) return (elem.isEditable(c));
			else return false;
		}
		public void setValueAt(Object a, int r, int c)
		{
			Element elem = (Element)onlcTable.list.elementAt(r);
			if (elem != null) elem.setField(c, a);
			fireTableCellUpdated(r,c);
			fireTableRowsUpdated(c,c);
			onlcTable.fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_CHANGE, onlcTable, elem, r, null, "ONLCTable changed")); 
		}
		public Object getValueAt(int r, int c)
		{
			Element elem = (Element)onlcTable.list.elementAt(r);
			if (elem != null) return (elem.getDisplayField(c));
			return null;
		}
		public int getPreferredWidth(int col) { return (preferredW[col]);}
		public void setPreferredWidth(int col, int w) { preferredW[col] = w;}
		public TableCellEditor getEditor(int col) { return null;}
		public TableCellRenderer getRenderer(int col) 
		{ 
			if (getColumnClass(col) == Boolean.class) return null;
			else
				return (new ONLTableCellRenderer());
		}
	} //class TableModel


	protected static class ElementAction extends ONL.UserAction //implements ListSelectionListener, KeyListener
	{
		private ONLCTable onlcTable;
		private ExpCoordinator expCoordinator;
		private boolean isAdd = true;
		//private int nQueue = -1;
		public ElementAction(ONLCTable rt, String ttl) { this(rt, ttl, true);}
		public ElementAction(ONLCTable rt, String ttl, boolean a) { this(rt, ttl, a, false, true);}
		public ElementAction(ONLCTable rt, String ttl, boolean a, boolean ign_obs, boolean exp_wide)
		{
			super(ttl, ign_obs, exp_wide);
			onlcTable = rt;
			expCoordinator = onlcTable.getExpCoordinator();
			isAdd = a;
		}

		public void actionPerformed(ActionEvent e)
		{
			if (isAdd)
			{
				Experiment exp = expCoordinator.getCurrentExp();
				if (!exp.containsParam(onlcTable)) exp.addParam(onlcTable);
				onlcTable.addNewElement();
			}
			else
			{
				ExpCoordinator.print("ONLCTable.ElementAction remove", 5);
				if (onlcTable.jtable != null)
				{
					int[] ndxs = onlcTable.jtable.getSelectedRows();
					int max = Array.getLength(ndxs);
					Vector rts = new Vector(max);
					int i = 0;
					ExpCoordinator.print(new String("     " + max + " selected"), 5);
					for (i = 0; i < max; ++i)              
					{
						rts.add(onlcTable.list.elementAt(ndxs[i]));
					}
					onlcTable.removeElements(rts);
				}
				else
					ExpCoordinator.print("     jtable null", 5);
			}
		}
	}

	public ONLCTable(String lbl, String tp, ExpCoordinator ec, Vector data)
	{ 
		super(lbl, tp, ec);
		if (data != null)
			list = new Vector(data);
		else list = new Vector();
	}


	public ONLCTable(String lbl, String tp, ExpCoordinator ec, Vector data, ONLComponent p)
	{
		super(p, lbl, tp, ec);
		if (data != null)
			list = new Vector(data);
		else list = new Vector();
	}


	protected void read(ONL.Reader tr) throws IOException
	{
		int numElems = tr.readInt();
		ExpCoordinator.printer.print(new String("ONLCTable.read numElems:" + numElems), 2);
		Vector tmp_elems = new Vector();
		for (int i = 0; i < numElems; ++i)
		{
			Object tmp_obj = addNewElement(tr);
			if (tmp_obj != null) tmp_elems.add(tmp_obj);
		}
		/*
      if (!tmp_elems.isEmpty()) 	
	{
	  Experiment exp = expCoordinator.getCurrentExp();
	  if (!exp.containsParam(this)) exp.addParam(this);
	}
		 */
	}

	protected static void skip(ONL.Reader tr) throws IOException
	{
		tr.readString();
		int num_elems = tr.readInt();
		for (int i = 0; i < num_elems; ++i) tr.readString();
	}
	public void writeExpDescription(ONL.Writer tw) throws IOException 
	{ 
		//System.out.println("RT::writeExpDescription");
		super.writeExpDescription(tw);
		writeTable(tw);
	}

	public void writeTable(ONL.Writer wr) throws IOException
	{
		int num_elems = list.size();
		int num_to_write = getNumToWrite();
		wr.writeInt(getNumToWrite());
		int j = 0;
		for (int i = 0; i < num_elems; ++i)
		{
			Element rt = (Element)list.elementAt(i);
			if (rt.isWritable() && j < num_to_write) 
			{
				rt.write(wr);
				//wr.writeString(rt.toString());
				++j;
			}
		}
	} 
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		int num_elems = list.size();
		for (int i = 0; i < num_elems; ++i)
		{
			Element rt = (Element)list.elementAt(i);
			if (rt.isWritable()) 
			{
				rt.writeXML(xmlWrtr);
				//wr.writeString(rt.toString());
			}
		}
	}
	protected int getNumToWrite() 
	{
		int num_elems = list.size();
		int rtn = 0;
		for (int i = 0; i < num_elems; ++i)
		{
			Element rt = (Element)list.elementAt(i);
			if (rt.isWritable()) ++rtn;
		}
		return rtn;
	}
	public void addElements(ONLCTable rt)
	{
		int max = rt.size();
		Element elem;
		int start_index = list.size();

		for (int i = 0; i < max; ++i)
		{
			elem = (Element)rt.list.elementAt(i);
			addNewElement(elem);
		}
		int end_index = list.size() - 1;
		if (end_index >= start_index) //if something was added
			tableModel.fireTableRowsInserted(start_index, end_index);
	}
	public boolean containsElement(Object r)
	{
		if (list.contains(r)) return true;
		int max = list.size();
		Object elem;
		for (int i = 0; i < max; ++i)
		{
			elem = list.elementAt(i);
			if (areEqual(elem, r)) return true;
		}
		return false;
	}

	public int getSelectedRow() 
	{
		int ndx = jtable.getSelectedRow();
		if (ndx < 0 && list.size() > 0) ndx = 0;
		return ndx;
	} 

	public Vector getSelectedElements()
	{
		int ndxs[] = jtable.getSelectedRows();
		int max = Array.getLength(ndxs);
		Vector rtn_elems = new Vector(max);
		int i = 0;
		ONLCTable.Element elem;
		for (i = 0; i < max; ++i)
		{
			elem = getElement(ndxs[i]);
			if (!rtn_elems.contains(elem))
				rtn_elems.add(elem);
		}
		return rtn_elems;
	}

	public abstract boolean areEqual(Object o1, Object o2);
	public void changeElement(ONLCTable.Element r)
	{
		if (list.contains(r)) 
		{
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_CHANGE, this, r, getIndex(r), null, "ONLCTable.Element changed")); 
			tableModel.fireTableDataChanged();
		}
	}
	public Object addElement(ONLCTable.Element r) { return (addElement(r, list.size(), false));}
	public Object addElement(ONLCTable.Element r, int s) { return (addElement(r, s, false));}
	protected Object addElement(ONLCTable.Element r, boolean can_repeat) { return (addElement(r, list.size(), can_repeat));}
	protected Object addElement(ONLCTable.Element r, int s, boolean can_repeat)
	{
		boolean there = false;
		if (r != null) there = containsElement(r);
		if (r != null && (can_repeat || !there)) 
		{
			list.add(r);//new Queue(r, this));
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_ADD, this, r, s, null, "ONLCTable.Element added")); 
			if (panel != null && panel.isVisible())  fireEvent(new ONLComponent.Event(ONLComponent.Event.GET_FOCUS, this, "Get Focus"));
			tableModel.fireTableRowsInserted(s,s);
			Experiment exp = ExpCoordinator.theCoordinator.getCurrentExp();
			if (!exp.containsParam(this)) exp.addParam(this);
			return r;
		}
		else
		{
			if (r != null)
				ExpCoordinator.print(new String("ONLCTable(" + getLabel() + ").addElement(" + r.toString() + ", " + s + ", " + can_repeat + ") there:" + there), TEST_ADD);
			else
				ExpCoordinator.print(new String("ONLCTable(" + getLabel() + ").addElement(" + null + ", " + s + ", " + can_repeat + ") there:" + there), TEST_ADD);
		}
		return null;
	}

	protected Object addNewElement(ONL.Reader rdr) { return (addNewElement(rdr, list.size()));} //adds an element read from some stream
	protected Object addNewElement(ONLCTable.Element elem) { return (addNewElement(elem, list.size()));} //adds new element making a copy of elem
	protected Object addNewElement() { return (addNewElement(list.size()));}  //creates new element and adds
	protected abstract Object addNewElement(ONL.Reader rdr, int i); //adds an element read from some stream
	protected abstract Object addNewElement(ONLCTable.Element elem, int i); //adds new element making a copy of elem
	protected abstract Object addNewElement(int i); //creates new element and adds //appears no one ever uses this
	public void removeElements(Vector v)
	{
		int max = v.size();
		for (int i = 0; i < max; ++i)
		{
			removeElement((Element)v.elementAt(i));
		}
	}
	public void removeElement(ONLCTable.Element r)
	{
		removeFromList(r);
	}
	protected void removeFromList(ONLCTable.Element r)
	{
		if (r!= null && containsElement(r))
		{
			int max = list.size();
			int removed = -1;
			Element elem = null;
			for (int i = 0; i < max; ++i)
			{
				elem = (Element)list.elementAt(i);
				if ((elem == r) || areEqual(elem, r)) 
				{
					ExpCoordinator.print(new String("ONLCTable(" + getLabel() + ").removeFromList found elem:" + elem.toString()), TEST_REMOVE);
					removed = i;
					break;
				}
			}
			if (removed >= 0) //should always be true
			{
				list.remove(elem);
				fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_DELETE, this, elem, removed, null, "ONLCTable.Element removed"));
				ExpCoordinator.print(new String("ONLCTable(" + getLabel() + ").removeFromList elem:" + elem.toString()), TEST_REMOVE);
				tableModel.fireTableRowsDeleted(removed,removed);
			}
		}
	}
	public ONLCTable.Element getElement(Object o) 
	{ 
		int max = list.size();
		ExpCoordinator.print(new String("ONLCTable(" + getLabel() + ").getElement size " + max), TEST_GET);
		for (int i = 0; i < max; ++i)
		{
			Object elem = list.elementAt(i);
			if (areEqual(elem, o))
				return ((ONLCTable.Element)elem);
		}
		return null;
	}
	public ONLCTable.Element getElementExact(Object o) 
	{ 
		int max = list.size();
		for (int i = 0; i < max; ++i)
		{
			Object elem = list.elementAt(i);
			if (elem.toString().equals(o.toString()))
				return ((ONLCTable.Element)elem);
		}
		return null;
	}
	public ONLCTable.Element getElement(int i) 
	{ 
		int max = list.size();
		if (i >= 0 && i < max)
		{
			return ((ONLCTable.Element)list.elementAt(i));
		}
		return null;
	}
	//private JFrame getFrame() 
	public ONLCTable.TablePanel getPanel() { return (getPanel(tableModel));}

	public ONLCTable.TablePanel getPanel(TableModel tm)
	{
		JTable tmp_jtable = new JTable(tm);
		TablePanel panel = new TablePanel(tmp_jtable);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		tmp_jtable.setDefaultRenderer(String.class, new ONLTableCellRenderer());
		int num_cols = tm.getColumnCount();
		TableCellEditor tceditor = null;
		TableCellRenderer tcrenderer = null;
		int col = 0;
		for (col = 0; col < num_cols; ++col)
		{
			tceditor = tm.getEditor(col);
			tcrenderer = tm.getRenderer(col);
			if (tcrenderer != null)
				tmp_jtable.setDefaultRenderer(tm.getColumnClass(col), tcrenderer);
			if (tceditor != null)
				tmp_jtable.setDefaultEditor(tm.getColumnClass(col), tceditor);
		}
		tmp_jtable.setColumnSelectionAllowed(false);
		tmp_jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tmp_jtable.setShowVerticalLines(false);
		TableColumnModel tcm = tmp_jtable.getColumnModel();

		for (col = 0; col < num_cols; ++col)
		{
			setColumnWidth(tcm, col, tm.getPreferredWidth(col));
		}
		panel.add((new JScrollPane(tmp_jtable)), "Center");
		tmp_jtable.setPreferredScrollableViewportSize(new Dimension(200,250));
		return panel;
	}

	public void clear()
	{
		if (!list.isEmpty())
		{
			Vector del_rts = new Vector(list);
			int end = list.size() - 1;
			list.removeAllElements();
			tableModel.fireTableRowsDeleted(0, end);
		}
	}
	protected abstract NCCPOpManager createOpManager();
	public void addOperation(NCCPOpManager.Operation op) 
	{
		if (opManager == null) opManager = createOpManager();
		if (opManager != null)
			opManager.addOperation(op);
	}

	public void removeOperation(NCCPOpManager.Operation op) 
	{
		if (opManager != null)
			opManager.removeOperation(op);
		if (panel != null) panel.repaint();
	}

	public int getIndex(Object o)
	{
		if (o != null)
		{
			int max = list.size();
			for (int i = 0; i < max; ++i)
			{
				if (areEqual(o, (list.elementAt(i)))) return i;
			}
		}
		return -1;
	}
	protected void setOpManager(NCCPOpManager opm) { opManager = opm;}
	protected void setTableModel(TableModel tm) { tableModel = tm;}
	protected TableModel getTableModel() { return tableModel;}
	protected Vector getList() { return list;}
	protected abstract String[] getColumnNames();
	protected abstract Class[] getColumnTypes();
	public int size() { return (list.size());}
	protected void setColumnWidth(TableColumnModel tcm, int col, int w)
	{
		tcm.getColumn(col).setPreferredWidth(w);
		tcm.getColumn(col).setMinWidth(w);
	}

	public void processEvent(ONLComponent.Event event)
	{
		ONLComponent.TableEvent tevent = null;
		if (event instanceof ONLComponent.TableEvent)
			tevent = (ONLComponent.TableEvent)event;

		int tp = event.getType();
		ExpCoordinator.print(new String("ONLCTable(" + getLabel() + ").processEvent:TableChanged index = " + event.getIndex()), 2);
		if (tevent != null)
			ExpCoordinator.print(new String("    tevent.nval = " + tevent.getNewValue() + "  tevent.oval = " + tevent.getOldValue()), 2);
		ONLCTable.Element r;
		try
		{
			ONL.StringReader str_rdr = null;
			switch(tp)
			{
			case ONLComponent.Event.TABLE_CHANGE:
				ONLCTable.Element oldElement = getElement(tevent.getIndex());
				if (oldElement != null)
				{
					oldElement.parseString(tevent.getNewValue());
					changeElement(oldElement);
					break;
				}
			case ONLComponent.Event.TABLE_ADD:
				str_rdr = new ONL.StringReader(tevent.getNewValue(), " ");
				ExpCoordinator.print("ONLCTable.processEvent TABLE_ADD", 7);
				str_rdr.print(7);
				ONLCTable.Element newElement = (ONLCTable.Element)addNewElement(str_rdr, tevent.getIndex());
				if (newElement != null) newElement.setCommitted(tevent.isCommitted());
				break;
			case ONLComponent.Event.TABLE_DELETE:
				removeElement(getElement(tevent.getIndex()));
				break;
			case ONLComponent.Event.TABLE_COMMIT:
				getElement(tevent.getIndex()).setCommitted(true);
				tableModel.fireTableDataChanged();
				break;
			default:
				super.processEvent(event);
			}
		}
		catch (java.text.ParseException pe)
		{
			System.err.print("ONLCTable.processEvent ParseException");
		} 
	}

	public abstract Action getAction();

	public void fireCommitEvent(ONLCTable.Element r)
	{
		if (list.contains(r)) 
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_COMMIT, this, r, getIndex(r), null, "ONLCTable.Element committed")); 
	}
	protected boolean merge(ONLComponent c)
	{
		if ((c.getType() == getType()) && (c instanceof ONLCTable))
		{
			ONLCTable t2 = (ONLCTable)c;
			int max = list.size();
			Element elem1;
			Element elem2;
			int i;
			boolean differences = false;
			for (i = 0; i < max; ++i)
			{
				elem1 = getElement(i);
				elem2 = t2.getElementExact(elem1);
				if (elem2 == null) 
				{
					elem1.setState(ONLComponent.IN1);
					ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String(getLabel() + " Table Inconsistency master contains elem:(" + elem1.toString() + ") other does not\n")),
							"Table Inconsistency",
							"Table Inconsistency",
							StatusDisplay.Error.LOG)); 
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
					ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String(getLabel() + " Table Inconsistency other contains elem:(" + elem2.toString() + ")\n")),
							"Table Inconsistency",
							"Table Inconsistency",
							StatusDisplay.Error.LOG)); 
					differences = true;
					addElement(elem2, true);
				}
			}
			/*REMOVED 8/16/10
			if (differences)
			{			
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("Table Inconsistency for " + getLabel() + "\n")),
						"Table Inconsistency",
						"Table Inconsistency",
						StatusDisplay.Error.LOG)); 
			}
			 */
			return true;
		}
		else return false;
	}
	public void setID(int i) { setProperty(ONLComponent.INDEX, i);}
	public int getID() { return (getPropertyInt(ONLComponent.INDEX));}
}
