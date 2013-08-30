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
 * File: HWTableElement.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 2/20/2010
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
import java.lang.reflect.Array;
import java.util.*;
import java.text.ParseException;
import java.lang.NumberFormatException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;


public class HWTableElement implements ONLCTable.Element,IDAssigner.IDable,Field.Owner
{
	//wild card 
	public static final String WILDCARD_STRING = "*";
	public static final int WILDCARD = 0xffffffff;
	public static final String NEWLINE = ExperimentXML.NEWLINE;


	public static final int TEST_FIELD = 3;
	public static final int TEST_DISPLAY = 7;
	public static final int TEST_ENABLE = 3;
	public static final int TEST_LOAD = 5;
	public static final int TEST_NHLISTENER = Topology.TEST_DEFRTS;

	public static final String GENERATED = "generated";//property name denotes if the RLI generated this entry
	public static final String HOST_GENERATED = "hst_generated";//property name denotes if a host generated this entry

	private HWTable hwtable = null;
	private Spec elementSpec = null;
	private int enableField = -1;
	private int enableCol = -1;

	private Field fields[] = null;
	//private Stats componentIndex = null;
	private ONLPropertyList properties = null;

	private EventListenerList listeners = null;
	//private UpdateEdit updateEdit = null;
	private Vector commands = null;
	private Vector monitoring = null;
	private ONL.Log commandLog = null;

	private int numCommits = 0;

	//////////////////////////////////////////// HWTableElement.Spec //////////////////////////////////////////////////////////
	public static class Spec implements ComponentXMLHandler.Spec
	{   
		protected Vector fieldSpecs = null;
		protected Vector commandSpecs = null;
		protected Vector monitorSpecs = null;
		protected Vector columnSpecs = null;
		protected HWTable.Spec tableSpec = null;
		protected ONLPropertyList properties = null;
		private boolean initialized = false;
		private int enableField = -1;
		private int enableCol = -1;

		//////////////////////////////////////////// HWTableElement.Spec.EnableSpec /////////////////////////////////////////////
		public static class EnableSpec extends FieldSpec
		{
			public EnableSpec() 
			{ 
				super(ExperimentXML.ENABLED, CommandSpec.BOOL_STR);
				setDefaultValue(new Boolean(true));
			}
		}//end class HWTableElement.Spec.EnableSpec

		//////////////////////////////////////////////////////// HWTableElement.Spec.XMLHandler //////////////////////////////////
		public static class XMLHandler extends ComponentXMLHandler
		{
			protected HWTableElement.Spec elementSpec = null;
			//protected ContentHandler hwhandler = null;
			//protected XMLReader xmlReader = null;
			//protected int currentIndex = 0;

			public XMLHandler(XMLReader xmlr, ContentHandler hwh, HWTableElement.Spec ts)
			{	
				super(xmlr, ts, hwh);
				elementSpec = ts;
			}

			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{
				super.startElement(uri, localName, qName, attributes);
				ExpCoordinator.print(new String("HWTableElement.Spec.XMLHandler.startElement localName:" + localName), HWTable.TEST_HWTABLE);

				if (localName.equals(ExperimentXML.COLUMN))
				{
					HWTable.ColumnSpec cs = new HWTable.ColumnSpec(uri, attributes);
					elementSpec.columnSpecs.add(cs);//addColumn(cs);
					//ExpCoordinator.print(new String("      setting column " ), HWTable.TEST_HWTABLE);
					xmlReader.setContentHandler(cs.getXMLHandler(xmlReader, this));
				}
				if (localName.equals(ExperimentXML.ADD_ELEM) ||
						localName.equals(ExperimentXML.DELETE_ELEM))
				{
					commandType = CommandSpec.COMMAND;
					if (getSpec().isPort()) commandType = commandType | CommandSpec.PERPORT;
					CommandSpec cspec = createCommandSpec(uri, localName, attributes, commandType);
					cspec = getSpec().addCommand(cspec);
					xmlReader.setContentHandler(cspec.getXMLHandler(xmlReader, this));
				}
				if (localName.equals(ExperimentXML.COMMAND_LOG)) elementSpec.properties.setProperty(ExperimentXML.COMMAND_LOG, true);
			}

			protected CommandSpec createCommandSpec(String uri, String localName, Attributes attributes, int ctype) 
			{ 
				return (new TableCommandSpec(uri, attributes, elementSpec, ctype, localName));
			}
			public boolean isSection(String nm)
			{
				if (!super.isSection(nm))
				{
					return (nm.equals(ExperimentXML.DISPLAY) ||
							nm.equals(ExperimentXML.ELEMENT));
				}
				return true;
			}
			public void endElement(String uri, String localName, String qName)
			{
				super.endElement(uri, localName, qName);
				if (localName.equals(ExperimentXML.ELEMENT)) elementSpec.initEnabled();
				ExpCoordinator.print(new String("HWTableElement.Spec.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
			}
		}
		///////////////////////////////////////////////////// HWTableElement.Spec methods /////////////////////////////////////////////////////////////
		public Spec(String uri, Attributes attributes, HWTable.Spec ts)
		{
			this(attributes.getValue(ExperimentXML.NAME), attributes.getValue(ExperimentXML.NUMFIELDS), attributes.getValue(ExperimentXML.NUMCOLS), ts, attributes.getValue(ExperimentXML.ENABLED));
		}

		private Spec(String nm, String nfields, String ncols, HWTable.Spec ts, String enable)
		{
			properties = new ONLPropertyList(this);
			properties.setProperty(ExperimentXML.NAME, new String(nm));
			if (enable != null) properties.setProperty(ExperimentXML.ENABLED, enable);
			else properties.setProperty(ExperimentXML.ENABLED, false);

			properties.setProperty(ExperimentXML.NUMFIELDS, nfields);
			properties.setProperty(ExperimentXML.NUMCOLS, ncols);

			tableSpec = ts;
			fieldSpecs = new Vector();
			commandSpecs = new Vector();
			monitorSpecs = new Vector();
			columnSpecs = new Vector();
			properties.setProperty(ExperimentXML.COMMAND_LOG, false);
		}

		public Spec(HWTableElement.Spec sp, HWTable.Spec ts)
		{
			this(sp.properties.getProperty(ExperimentXML.NAME), sp.properties.getProperty(ExperimentXML.NUMFIELDS), sp.properties.getProperty(ExperimentXML.NUMCOLS), ts, sp.properties.getProperty(ExperimentXML.ENABLED));
			Vector tmp_v = sp.getFieldSpecs();
			int max = tmp_v.size();
			enableField = sp.enableField;
			enableCol = sp.enableCol;
			int i;
			for(i = 0; i < max; ++i)
			{
				fieldSpecs.add(new FieldSpec((FieldSpec)tmp_v.elementAt(i)));
			}
			tmp_v = sp.getCommandSpecs();
			max = tmp_v.size();
			for(i = 0; i < max; ++i)
			{
				commandSpecs.add(new TableCommandSpec((TableCommandSpec)tmp_v.elementAt(i)));
			}
			tmp_v = sp.getMonitorSpecs();
			max = tmp_v.size();
			for(i = 0; i < max; ++i)
			{
				monitorSpecs.add(new TableCommandSpec((TableCommandSpec)tmp_v.elementAt(i)));
			}
			tmp_v = sp.getColumnSpecs();
			max = tmp_v.size();
			for(i = 0; i < max; ++i)
			{
				columnSpecs.add(new HWTable.ColumnSpec((HWTable.ColumnSpec)tmp_v.elementAt(i)));
			}
			initialized = true;
			if (sp.hasCommandLog()) properties.setProperty(ExperimentXML.COMMAND_LOG, true);
		}
		public HWTable.ColumnSpec getColumnSpec(int i) { return ((HWTable.ColumnSpec)columnSpecs.elementAt(i));}
		//interface ComponentXMLHandler.Spec
		public CommandSpec addCommand(CommandSpec cspec)
		{
			if (cspec.isMonitor()) monitorSpecs.add(cspec);
			if (cspec.isCommand()) commandSpecs.add(cspec);
			return cspec;
		}
		public void addTable(HWTable.Spec htspec) {}
		public void addAssigner(ComponentXMLHandler.AssignerSpec htspec) {}
		public void addField(FieldSpec fspec) { fieldSpecs.add(fspec);}
		public String getEndToken() { return (ExperimentXML.ELEMENT);}
		public boolean isPort() { return (tableSpec.isPort());}
		public Vector getMonitorSpecs() { return monitorSpecs;}
		public Vector getCommandSpecs() { return commandSpecs;}
		public Vector getFieldSpecs() { return fieldSpecs;}
		public Vector getTableSpecs() { return null;}
		public Vector getAssignerSpecs() { return null;}
		//end interface ComponentXMLHandler.Spec
		public Vector getColumnSpecs() { return columnSpecs;}
		public int getEnableCol() { return enableCol;}
		public int getEnableField() { return enableField;}
		public int getNumCols() { return (properties.getPropertyInt(ExperimentXML.NUMCOLS));}
		public int getNumFields() { return (properties.getPropertyInt(ExperimentXML.NUMFIELDS));}
		public String getElementName() { return (properties.getProperty(ExperimentXML.NAME));}
		public boolean hasCommandLog() { return (properties.getPropertyBool(ExperimentXML.COMMAND_LOG));}
		public HWTable.Spec getTableSpec() { return tableSpec;}
		public boolean isEnabled() { return (properties.getPropertyBool(ExperimentXML.ENABLED));}
		protected void initEnabled()
		{
			if (isEnabled() && !initialized)
			{
				initialized = true;
				enableField = getNumFields();
				enableCol = getNumCols();
				int nfs = getNumFields() + 1;
				int ncs = getNumCols() + 1;
				properties.setProperty(ExperimentXML.NUMFIELDS, nfs);
				properties.setProperty(ExperimentXML.NUMCOLS, ncs);
				fieldSpecs.add(new EnableSpec());
				HWTable.ColumnSpec colsp = new HWTable.ColumnSpec("on", 30);
				colsp.add(new HWTable.ColumnSpec.ColumnElement(ExperimentXML.FIELDNAME, ExperimentXML.ENABLED));
				columnSpecs.add(colsp);
				ExpCoordinator.print(new String("HWTableElement.initEnabled enableField:" + enableField + " enableCol:" + enableCol), TEST_ENABLE);
			}
		}
		public HWTableElement.TableCommandSpec getAddSpec()
		{
			int max = commandSpecs.size();
			TableCommandSpec tcs;
			for (int i = 0; i < max; ++i)
			{
				tcs = (TableCommandSpec)commandSpecs.elementAt(i);
				if (tcs.isAdd()) return tcs;
			}
			return null;
		}
		public HWTableElement.TableCommandSpec getDeleteSpec()
		{
			int max = commandSpecs.size();
			TableCommandSpec tcs;
			for (int i = 0; i < max; ++i)
			{
				tcs = (TableCommandSpec)commandSpecs.elementAt(i);
				if (tcs.isDelete()) return tcs;
			}
			return null;
		}
		public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) { return (new HWTableElement.Spec.XMLHandler(xmlr, hwh, this));} 
		public String[] getColumnNames()
		{
			int max = getNumCols();
			String rtn[] = new String[max];
			for (int i = 0; i < max; ++i)
			{
				rtn[i] = new String(getColumnSpec(i).getTitle());
			}
			return rtn;
		}
		public Class[] getColumnTypes()
		{
			int max = getNumCols();
			Class rtn[] = new Class[max];
			for (int i = 0; i < max; ++i)
			{
				if (i == getEnableCol()) rtn[i] = Boolean.class;
				else
					rtn[i] = String[].class;
			}
			return rtn;
		}
		public int getRowHeight()
		{
			int num_cols = columnSpecs.size();
			HWTable.ColumnSpec cspec = null;
			HWTable.ColumnSpec.ColumnElement elem = null;
			int num_elems = 0;
			int max_lines = 1;
			int j = 0;
			int num_lines = 0;
			for(int i = 0; i < num_cols; ++i)
			{
				cspec = (HWTable.ColumnSpec)columnSpecs.elementAt(i);
				num_elems = cspec.size();
				num_lines = 0;
				for(j = 0; j < num_elems; ++j)
				{
					elem = cspec.getColumnElement(j);
					if (elem.isNewLine()) ++num_lines;
				}
				if (num_lines > max_lines) max_lines = num_lines;
			}
			ExpCoordinator.print(new String("HWTableElement.Spec.getRowHeight lines:" + max_lines), TEST_DISPLAY);
			return (25*max_lines);
		}
	}

	//////////////////////////////////////////////////////// HWTableElement.TableCommandSpec //////////////////////////////////
	public static class TableCommandSpec extends CommandSpec
	{
		private String optype = null;

		/////////////////////////////////////////// HWTableElement.TableCommandSpec.XMLHandler ///////////////////////////////////
		protected static class XMLHandler extends CommandSpec.XMLHandler
		{
			public XMLHandler(XMLReader xmlr, ContentHandler hwh, TableCommandSpec cs)
			{
				super(xmlr, hwh, (CommandSpec)cs);
			}

			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{
				super.startElement(uri, localName, qName, attributes);
				currentElement = new String(localName);
				ExpCoordinator.print(new String("HWTable.TableCommandSpec.XMLHandler.startElement localName:" + localName + " paramIndex:" + paramIndex), HWTable.TEST_HWTABLE);
				//if (localName.equals(EDITABLE) && cspec.paramArray[paramIndex] instanceof Param) ((Param)cspec.paramArray[paramIndex]).editable = true;
			}
			public void characters(char[] ch, int start, int length)
			{
				ExpCoordinator.print(new String("HWTable.Spec.TableCommandSpec.XMLHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
				super.characters(ch, start, length);
			}
			public void endElement(String uri, String localName, String qName)
			{
				super.endElement(uri, localName, qName);
				ExpCoordinator.print(new String("HWTable.Spec.TableCommandSpec.XMLHandler.endElement " + localName + " paramIndex:" + paramIndex), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.ADD_ELEM) || 
						localName.equals(ExperimentXML.DELETE_ELEM))
				{
					cspec.print(ExperimentXML.TEST_XML);
					cspec = null;
					xmlReader.setContentHandler(hwhandler);
				}
			}
		}

		//////////////////// TableCommandSpec methods //////////////////////
		public TableCommandSpec(HWTableElement.Spec ts, String o) 
		{
			super();
			//tableSpec = ts;
			optype = new String(o);
		}
		public TableCommandSpec(String uri, Attributes attributes, HWTableElement.Spec ts, int cmdtp, String o)
		{
			super(uri, attributes, cmdtp);
			//tableSpec = ts;
			if (o != null) optype = new String(o);
		}
		public TableCommandSpec(TableCommandSpec cspec) 
		{
			super(cspec);
			//tableSpec = cspec.tableSpec;
			optype = new String(cspec.optype);
			//updateDefaults();
		}
		public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) { return (new HWTableElement.TableCommandSpec.XMLHandler(xmlr, hwh, this));}
		public boolean isAdd() { return (optype.equals(ExperimentXML.ADD_ELEM));}
		public boolean isDelete() { return (optype.equals(ExperimentXML.DELETE_ELEM));}
		public void setOptype(String s) { optype = new String(s);}
		public String getOpType() { return optype;}
		public boolean isParamEditable(int i) { return (getParam(i).isEditable());}
		public Command createCommand(ONLComponent parent) { return (createCommand(parent, null));}
		public Command createCommand(ONLComponent parent, Field.Owner owner)
		{
			if (owner != null && owner instanceof HWTableElement)
				return (new TableCommand(this, (HWTable)parent, (HWTableElement)owner));
			else 
			{
				if (parent instanceof HWTable) return (new TableCommand(this, (HWTable)parent));
			}
			return null;
		}                                          
	}//end HWTableElement.TableCommandSpec

	//////////////////////////////////////////////////////// HWTableElement.TableCommand //////////////////////////////////
	public static class TableCommand extends Command //TableCommandSpec
	{
		private TableCommandSpec templateSpec = null;
		private HWTableElement tableElement = null;
		//protected HWTable.Spec tableSpec = null;
		//protected boolean is_entry = false;
		private String optype = null;

		//////////////////// TableCommand methods //////////////////////
		public TableCommand(TableCommand cspec, HWTable c, HWTableElement e)
		{
			this(cspec.templateSpec, c, e);
			//tableSpec = cspec.tableSpec;
			ExpCoordinator.print(new String("HWTableElement.TableCommand params:" + params.size() + " table " +  c), TEST_LOAD);
			cspec.print(TEST_LOAD);
			setCurrentValues(cspec.getCurrentValues());
			//updateDefaults();
		}
		public TableCommand(TableCommandSpec cspec, HWTable c) { this(cspec, c, null);}
		public TableCommand(TableCommandSpec cspec, HWTable c, HWTableElement e)
		{
			super((CommandSpec)cspec, c, (Field.Owner)e);
			templateSpec = cspec;
			tableElement = e;
			//tableSpec = cspec.tableSpec;
			optype = new String(cspec.getOpType());
		}
		public TableCommand(TableCommandSpec ts, Vector defaults, HWTable c, HWTableElement e)
		{
			this(ts, c, e);
			updateDefaults(defaults);
		}
		public void setCommittedValues()
		{
			super.setCommittedValues();
			((HWTable)getParentComponent()).changeElement(tableElement);
		}
		public void setCommittedValues(Object[] vals)
		{
			super.setCommittedValues(vals);
			((HWTable)getParentComponent()).changeElement(tableElement);
		}
		public HWTableElement getEntry() { return tableElement;}
		public boolean isAdd() { return (optype.equals(ExperimentXML.ADD_ELEM));}
		public boolean isDelete() { return (optype.equals(ExperimentXML.DELETE_ELEM));}
		public String getOpType() { return optype;}
		public Command getCopy(ONLComponent parent, Object[] defaults)
		{
			TableCommand rtn = new TableCommand(this, (HWTable)parent, (HWTableElement)getFieldOwner());
			if (defaults != null) rtn.setDefaults(defaults);
			return rtn;
		}
		public Command.NCCPOp createNCCPOp(Object[] vals) 
		{ 
			Command.NCCPOp rtn = super.createNCCPOp(vals);
			if (tableElement.commandLog != null && !isDelete()) rtn.setCommandLog(tableElement.commandLog);
			return rtn;
		}
	}//end HWTableElement.TableCommand

	///////////////////////////////////////////// HWTableElement.Event ////////////////////////////////////////////////////////
	public static class Event extends EventObject
	{
		public static final int QID_CHANGED = 0;
		public static final int STATS_CHANGED = 1;
		public static final int DATA_CHANGED = 3;

		private int event_type = QID_CHANGED;
		private double oldValue = 0;
		private double newValue = 0;


		public Event(HWTableElement hwte) { this(hwte, DATA_CHANGED, 0, 0);}
		public Event(HWTableElement hwte, int tp, double ov, double nv)
		{
			super(hwte);
			event_type = tp;
			oldValue = ov;
			newValue = ov;
		}
		public int getType() { return event_type;}
		public double getNewValue() { return newValue;}
		public double getOldValue() { return oldValue;}
	}// end HWTableElement.Event

	///////////////////////////////////////////// HWTableElement.Listener ////////////////////////////////////////////////////////
	public interface Listener extends EventListener
	{
		public void valueChanged(HWTableElement.Event e);
	}
	///////////////////////////////////////////// HWTableElement.TContentHandler /////////////////////////////////////////////////
	protected class TContentHandler extends DefaultHandler
	{
		private ExperimentXML expXML = null;
		private String currentElement = "";
		private HWTableElement tableElement = null;
		private String currentField = null;
		private boolean add = true;

		public TContentHandler(ExperimentXML exp_xml, HWTableElement telem)
		{
			super();
			expXML = exp_xml;
			tableElement = telem;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			currentElement = new String(localName);
			if (localName.equals(ExperimentXML.FIELD))
			{
				currentField = attributes.getValue(ExperimentXML.FIELDNAME);
				ExpCoordinator.print(new String("HWTableElement.TContentHandler.startElement localName:" + localName + " field:" + currentField), ExperimentXML.TEST_XML);
			}
			else
				ExpCoordinator.print(new String("HWTableElement.TContentHandler.startElement localName:" + localName), ExperimentXML.TEST_XML);
		}
		public void characters(char[] ch, int start, int length) throws SAXException
		{
			if (currentField != null) setField(currentField, new String(ch, start, length));
		}
		public void setCurrentElement(String s) { currentElement = new String(s);}
		public String getCurrentElement() { return currentElement;}
		public void endElement(String uri, String localName, String qName)
		{
			if (localName.equals(ExperimentXML.FIELD)) 
			{
				currentField = null;
			}
			if (localName.equals(ExperimentXML.ELEMENT)) 
			{
				expXML.removeContentHandler(this);
				if (add) hwtable.addNewElement(tableElement, false);
			}
		}
		public void setAdd(boolean b) { add = b;}
	}
	/////////////////////////////////////////// HWTableElement.Edit ///////////////////////////////////////////////////////////////////
	
	public static class Edit extends Command.Edit
	{
		private HWTableElement tableElement = null;

		public Edit(HWTableElement elem, HWTableElement.TableCommand cspec, Experiment exp)
		{
			this(elem, cspec, cspec.getCurrentValues(), exp);
		}

		public Edit(HWTableElement elem, HWTableElement.TableCommand cspec, Object[] vals, Experiment exp)
		{
			super(cspec, vals, exp);
			ExpCoordinator.print(new String("HWtableElement(" + elem.toString() + ").Edit"), HWTable.TEST_HWTABLE);
			cspec.print(HWTable.TEST_HWTABLE);
			tableElement = elem;
		}
		protected void sendMessage() 
		{
			if (getCommandOp() == null) return;
			ExpCoordinator.print(new String("HWTableElement(" + tableElement.toString() + ").Edit.sendMessage"), HWTable.TEST_HWTABLE);
			if (tableElement.isEnabled()) tableElement.hwtable.addOperation(getCommandOp());
		}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (!tableElement.isEnabled()) 
			{
				setCancelled(true);
				return true;
			}
			if (un instanceof HWTableElement.DeleteEdit)
			{
				HWTableElement.DeleteEdit edit = (HWTableElement.DeleteEdit) un;
				if (getTableElement().isEqual(edit.getTableElement())) 
				{
					ExpCoordinator.print(new String("HWTableElement(" + tableElement.toString() + ").Edit.isCancelled because of delete"), HWTable.TEST_HWTABLE);
					
					setCancelled(true);
					return true;
				}
			}
			return (super.isCancelled(un));
		}
		public HWTableElement getTableElement() { return tableElement;}
	}//end class HWTableElement.Edit

	/////////////////////////////////////////// HWTableElement.AddEdit ///////////////////////////////////////////////////////////////
	public static class AddEdit extends Edit
	{
		public AddEdit(HWTableElement elem, HWTableElement.TableCommand cspec, Experiment exp) 
		{
			this(elem, cspec, cspec.getCurrentValues(), exp);
		}
		public AddEdit(HWTableElement elem, HWTableElement.TableCommand cspec, Object[] vals, Experiment exp)
		{
			super(elem, cspec, vals, exp);
			setCanUndo(true);
		}
		public void undo() throws CannotUndoException
		{
			HWTable table = getTableElement().hwtable;
			table.removeElement(getTableElement());
			if (table.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(table);
		}
		public void redo() throws CannotRedoException
		{
			HWTable table = getTableElement().hwtable;
			table.addElement(getTableElement());
			if ((getExperiment() != null) && !(getExperiment().containsParam(table))) getExperiment().addParam(table);
		}   
		protected void sendMessage() 
		{
			super.sendMessage();
			HWTableElement elem = getTableElement();
			if (elem.enableField >= 0) elem.fields[elem.enableField].setCommittedValue(new Boolean(true));
		}
		/*
    public boolean isCancelled(ONLComponent.Undoable un)
    {
      if (super.isCancelled(un)) return true;
      if (un instanceof HWTableElement.DeleteEdit)
	{
	  HWTableElement.DeleteEdit edit = (HWTableElement.DeleteEdit) un;
	  if (getTableElement().isEqual(edit.getTableElement())) 
	    {
	      setCancelled(true);
	      return true;
	    }
	}
      return false;
    }
		 */
	}


	/////////////////////////////////////////// HWTableElement.DeleteEdit ///////////////////////////////////////////////////////////////
	public static class DeleteEdit extends Edit
	{
		public DeleteEdit(HWTableElement elem, HWTableElement.TableCommand cspec, Experiment exp) 
		{
			this(elem, cspec, cspec.getCurrentValues(), exp);
		}
		public DeleteEdit(HWTableElement elem, HWTableElement.TableCommand cspec, Object[] vals, Experiment exp)
		{
			super(elem, cspec, vals, exp);
			setCanUndo(true);
		}
		public void undo() throws CannotUndoException
		{
			HWTable table = getTableElement().hwtable;
			table.addElement(getTableElement());
			if ((getExperiment() != null) && !(getExperiment().containsParam(table))) getExperiment().addParam(table);
		}   
		public void redo() throws CannotRedoException
		{
			HWTable table = getTableElement().hwtable;
			table.removeElement(getTableElement());
			if (table.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(table);
		}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			//if (super.isCancelled(un)) return true;
			if (un instanceof HWTableElement.AddEdit)
			{
				HWTableElement.AddEdit edit = (HWTableElement.AddEdit) un;
				if (getTableElement().isEqual(edit.getTableElement()))
				{
					setCancelled(true);
					return true;
				}
			}
			return false;
		}
	}


	/////////////////////////////////////////// HWTableElement.DisableEdit ///////////////////////////////////////////////////////////////
	public static class DisableEdit extends DeleteEdit
	{
		public DisableEdit(HWTableElement elem, HWTableElement.TableCommand cspec, Experiment exp) 
		{
			super(elem, cspec, cspec.getCurrentValues(), exp);
			setCanUndo(false);
		}
		public void undo() throws CannotUndoException {}
		public void redo() throws CannotRedoException {}
		public boolean isCancelled(ONLComponent.Undoable un) 
		{
			if (un instanceof HWTableElement.DeleteEdit && !(un instanceof HWTableElement.DisableEdit))
			{
				HWTableElement.DeleteEdit edit = (HWTableElement.DeleteEdit) un;
				if (getTableElement().isEqual(edit.getTableElement())) 
				{
					setCancelled(true);
					return true;
				}
			}
			return false;
		}
		protected void sendMessage() 
		{
			getTableElement().hwtable.addOperation(getCommandOp());
			HWTableElement elem = getTableElement();
			ExpCoordinator.printer.print(new String("HWTableElement(" + elem.toString() + ").DisableEdit.sendMessage"), HWTable.TEST_HWTABLE);
			if (elem.enableField > 0) elem.fields[elem.enableField].setCommittedValue(new Boolean(false));
		}
	}


	/////////////////////////////////////////// HWTableElement.EnableEdit ///////////////////////////////////////////////////////////////
	public static class EnableEdit extends Edit
	{
		public EnableEdit(HWTableElement elem, HWTableElement.TableCommand cspec, Experiment exp) 
		{
			super(elem, cspec, exp);
		}
		public void undo() throws CannotUndoException {}
		public void redo() throws CannotRedoException {}
		protected void sendMessage() 
		{
			super.sendMessage();
			HWTableElement elem = getTableElement();
			ExpCoordinator.printer.print(new String("HWTableElement(" + elem.toString() + ").EnableEdit.sendMessage"), HWTable.TEST_HWTABLE);
			elem.fields[elem.enableField].setCommittedValue(new Boolean(true));
		}
	}

	/////////////////////////////////////////// HWTableElement methods ////////////////////////////////////////////////////////////////

	public HWTableElement(HWTable hwt, ONL.Reader rdr) throws java.io.IOException
	{
		this(hwt);
		parseFromReader(rdr);
	}

	public HWTableElement(HWTable hwt)
	{
		ExpCoordinator.print(new String("HWTableElement new elem for table: " + hwt.getLabel()), HWTable.TEST_HWTABLE);
		hwtable = hwt;
		elementSpec = hwt.getTableSpec().getElementSpec();
		Vector field_specs = elementSpec.getFieldSpecs();
		commands = new Vector();
		monitoring = new Vector();
		properties = new ONLPropertyList(this);
		properties.setProperty(GENERATED, false);
		properties.setProperty(HOST_GENERATED, false);
		int num_fs = field_specs.size();
		properties.setProperty(ExperimentXML.NUMFIELDS, num_fs);
		fields = new Field[num_fs];
		if (hwtable == null) ExpCoordinator.print("    hwtable null", HWTable.TEST_HWTABLE);
		for (int i = 0; i < num_fs; ++i)
			fields[i] = ((FieldSpec)field_specs.elementAt(i)).createField(hwtable, this);
		Vector tcs = elementSpec.getCommandSpecs();
		int max = tcs.size();
		TableCommandSpec op;
		for (int i = 0; i < max; ++i)
		{
			op = (TableCommandSpec)tcs.elementAt(i);
			if (op.isCommand())
			{
				commands.add(new TableCommand(op, hwtable, this));
				ExpCoordinator.print(new String("    adding command:" + op.getLabel()), HWTable.TEST_HWTABLE);
			}
			if (op.isMonitor())
				monitoring.add(new TableCommand(op, hwtable, this));
		}
		tcs = elementSpec.getMonitorSpecs();
		max = tcs.size();
		for (int i = 0; i < max; ++i)
		{
			op = (TableCommandSpec)tcs.elementAt(i);
			if (op.isMonitor())
			{
				monitoring.add(new TableCommand(op, hwtable, this));
				ExpCoordinator.print(new String("    adding monitor:" + op.getLabel()), HWTable.TEST_HWTABLE);
			}
		}
		listeners = new EventListenerList();
		setID(-1);
		if (elementSpec.isEnabled())
		{
			enableField = elementSpec.getEnableField();//enableField;
			enableCol = elementSpec.getEnableCol();//enableCol;
			ExpCoordinator.print(new String("HWTableElement(" + toString() + ") enableField:" + enableField + " enableCol:" + enableCol), TEST_ENABLE);
		}
		if (elementSpec.hasCommandLog()) commandLog = new ONL.Log(new String(hwtable.getLabel() + " Entry Command Log"));
	}

	public HWTableElement(HWTableElement elem)
	{
		enableField = elem.enableField;
		enableCol = elem.enableCol;
		hwtable = elem.hwtable;
		ExpCoordinator.print(new String("HWTableElement(" + elem.toString() +") new elem for table: " + hwtable.getLabel()), HWTable.TEST_HWTABLE);
		elementSpec = hwtable.getTableSpec().getElementSpec();
		Vector field_specs = elementSpec.getFieldSpecs();
		commands = new Vector();
		monitoring = new Vector();
		properties = new ONLPropertyList(this);
		properties.setProperty(GENERATED, elem.properties.getProperty(GENERATED));
		properties.setProperty(HOST_GENERATED, elem.properties.getProperty(HOST_GENERATED));
		int num_fs = field_specs.size();
		properties.setProperty(ExperimentXML.NUMFIELDS, num_fs);
		fields = new Field[num_fs];
		if (hwtable == null) ExpCoordinator.print("    hwtable null", HWTable.TEST_HWTABLE);
		for (int i = 0; i < num_fs; ++i)
			fields[i] = new Field(elem.fields[i]);
		Vector tcs = elementSpec.getCommandSpecs();
		int max = tcs.size();
		TableCommandSpec op;
		for (int i = 0; i < max; ++i)
		{
			op = (TableCommandSpec)tcs.elementAt(i);
			if (op.isCommand())
			{
				commands.add(new TableCommand(op, hwtable, this));
				ExpCoordinator.print(new String("    adding command:" + op.getLabel()), HWTable.TEST_HWTABLE);
			}
		}
		tcs = elementSpec.getMonitorSpecs();
		max = tcs.size();
		for (int i = 0; i < max; ++i)
		{
			op = (TableCommandSpec)tcs.elementAt(i);
			if (op.isMonitor())
			{
				monitoring.add(new TableCommand(op, hwtable, this));
				ExpCoordinator.print(new String("    adding monitor:" + op.getLabel()), HWTable.TEST_HWTABLE);
			}
		}
		//if update edit
		//updateEdit = new Edit();
		//componentIndex = hwtable.getNewComponentIndex(this);
		listeners = new EventListenerList();
		setID(-1);
		if (elementSpec.hasCommandLog()) commandLog = new ONL.Log(new String(hwtable.getLabel() + " Entry Command Log"));
	}

	public void addNextHopListener()
	{
		int num_fs = Array.getLength(fields);
		ExpCoordinator.print(new String("HWTableElement(" + toString() + ").addNextHopListener"), TEST_NHLISTENER);
		for (int i = 0; i < num_fs; ++i)
		{
			fields[i].addNextHopListener();
		}
	}
	public boolean isWritable() { return true;}
	public String toString()
	{
		String rtn = "";
		int num_fs = Array.getLength(fields);
		for (int i = 0; i < num_fs; ++i)
		{
			rtn = rtn.concat(" " + fields[i].getCurrentValue().toString());
		}
		return rtn;
	}
	public void write(ONL.Writer wrtr) throws java.io.IOException
	{
		wrtr.writeString(toString());
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.ELEMENT);
		xmlWrtr.writeAttribute(GENERATED, properties.getProperty(GENERATED));
		if (hwtable.getParent().isEndpoint()) xmlWrtr.writeAttribute(HOST_GENERATED, properties.getProperty(HOST_GENERATED));
		int max = getNumFields();
		for (int i = 0; i < max; ++i)
		{
			xmlWrtr.writeStartElement(ExperimentXML.FIELD);
			xmlWrtr.writeAttribute(ExperimentXML.FIELDNAME, fields[i].getLabel());
			xmlWrtr.writeCharacters(fields[i].getCurrentValue().toString());
			xmlWrtr.writeEndElement();
		}
		xmlWrtr.writeEndElement();
	}
	public HWTable getTable() { return hwtable;}
	public void parseString(String str) throws java.text.ParseException//throws java.io.IOException;
	{
		ExpCoordinator.printer.print(new String("HWTableElement.parseString " + str), 5);
		if (str == null) 
		{
			//initializeEmpty();
			return;
		}
		try
		{
			parseFromReader(new ONL.StringReader(str, " "));
		}
		catch (java.io.IOException e)
		{
			throw new java.text.ParseException(new String("HWTableElement error: " + e.getMessage()), 0);
		}
	}
	public void parseFromReader(ONL.Reader rdr) throws java.io.IOException
	{
		if (!rdr.ready()) 
		{
			//initializeEmpty();
			return;
		}
		//try
		//{
		if (rdr instanceof ONL.StringReader) ((ONL.StringReader)rdr).resetSplit(" ");
		int num_fs = getNumFields();
		for (int i = 0; rdr.ready() && i < num_fs; ++i)
		{
			fields[i].setDefaultFromString(rdr.readString());
		}
		if (rdr.ready()) properties.setProperty(GENERATED, rdr.readString());
		if (rdr.ready()) properties.setProperty(HOST_GENERATED, rdr.readString());
		//}
	//catch (java.text.ParseException e)
		//{
		//throw new java.io.IOException(e.getMessage());
		//}
	}
	//public Object getField(int c);
	public Object getDisplayField(int c) 
	{
		if (c == enableCol) return (new Boolean(isEnabled()));
		return (getDisplayField(elementSpec.getColumnSpec(c)));
	}
	public Object getDisplayField(HWTable.ColumnSpec cspec)
	{
		int max = cspec.size();
		//String rtn[] = new String[max];
		Vector rtn = new Vector();
		String currentString = "";
		int j = 0;
		HWTable.ColumnSpec.ColumnElement elem = null;
		ExpCoordinator.print(new String("HWTableElement(" + toString() + ").getDisplayField " + cspec.getTitle()), TEST_DISPLAY);
		for (int i = 0; i < max; ++i)
		{
			elem = cspec.getColumnElement(i);
			if (elem.getType().equals(ExperimentXML.FIELDNAME)) currentString = currentString.concat(getField(elem.getValue()).defaultValue.toString());
			else
			{
				ExpCoordinator.print(new String("   elem:" + elem.getValue()), TEST_DISPLAY);
				if (elem.isNewLine()) 
				{
					rtn.add(j, currentString);//rtn[j] = currentString;
					ExpCoordinator.print(new String("   found end of line(" + j + "): " + currentString), TEST_DISPLAY);
					currentString = "";
					++j;
				}
				else
					currentString = currentString.concat(elem.getValue());
			}
		}
		ExpCoordinator.print(new String("   end of display(" + j + "): " + currentString), TEST_DISPLAY);
		rtn.add(j, currentString);//rtn[j] = currentString;
		return (rtn.toArray());
	}
	public boolean isEditable(int c) 
	{
		//ExpCoordinator.print(new String("HWTableElement.isEditable enableCol:" + enableCol + " c:" + c), TEST_ENABLE);
		return (c == enableCol);
		//return false;
	}
	public void setField(int c, Object a) 
	{ 
		if (enableCol >= 0 && c == enableCol)
		{
			setEnabled(a);
		}
	}
	public void setField(String nm, Object a)
	{
		ExpCoordinator.print(new String("HWTableElement.setField field:" + nm + " value:" + a), TEST_FIELD);
		Field f = getField(nm);
		if (f != null)
		{
			if (a instanceof String) f.setDefaultFromString((String)a);
			else f.setDefaultValue(a);
		}
	}
	public Field getField(String nm)
	{
		int max = getNumFields();
		for (int i = 0; i < max; ++i)
		{
			if (fields[i].getLabel().equals(nm)) return fields[i];
		}
		return null;
	}
	public void setCommitted(boolean b) 
	{
		if (b)
		{
			int max = getNumFields();
			for (int i = 0; i < max; ++i)
				fields[i].setCommittedValue();
		}
	}
	public boolean needsCommit()//ONLTableCellRenderer.Committable
	{
		int max = getNumFields();
		if (!isEnabled() && enableField >= 0) return (fields[enableField].needsCommit());
		for (int i = 0; i < max; ++i)
			if (fields[i].needsCommit()) return true;
		return false;
	}
	public int getNumFields() { return (properties.getPropertyInt(ExperimentXML.NUMFIELDS));}
	public HWTableElement.TableCommand getCommand(CommandSpec ts)
	{
		if (ts == null) return null;
		else return (getCommand(ts.getOpcode()));
	}
	public HWTableElement.TableCommand getCommand(int opcode)
	{
		int max = commands.size();
		ExpCoordinator.print(new String("HWTable.Element.getCommand " + toString()), HWTable.TEST_HWTABLE);
		HWTableElement.TableCommand elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (HWTableElement.TableCommand)commands.elementAt(i);
			if (opcode == elem.getOpcode()) return elem;
		}
		return null;
	}
	public HWTableElement.TableCommand getMonitorCommand(CommandSpec ts)
	{
		return (getMonitorCommand(ts.getOpcode()));
	}
	public HWTableElement.TableCommand getMonitorCommand(int opcode)
	{
		int max = monitoring.size();
		HWTableElement.TableCommand elem;
		ExpCoordinator.print(new String("HWTable.Element.getMonitorCommand " + toString()), HWTable.TEST_HWTABLE);
		for (int i = 0; i < max; ++i)
		{
			elem = (HWTableElement.TableCommand)monitoring.elementAt(i);
			if (opcode == elem.getOpcode()) return elem;
		}
		return null;
	}
	public ContentHandler getContentHandler(ExperimentXML expXML) { return (new TContentHandler(expXML, this));}
	public boolean isEqual(HWTableElement elem)
	{
		if (hwtable != elem.hwtable) return false;
		if (toString().equals(elem.toString())) return true;
		ExpCoordinator.print(new String("HWTableElement(" + toString() + ").isEqual(" + elem.toString() + ")"), HWTable.TEST_HWTABLE);
		int max = getNumFields();
		for (int i = 0; i < max; ++i)
		{
			if (!fields[i].isEqual(elem.fields[i])) 
			{
				ExpCoordinator.print(new String("        field:" + fields[i].label + " != elem.field:" + elem.fields[i].label), HWTable.TEST_HWTABLE);
				return false;
			}
		}
		return true;
	}
	public void setState(String s) { properties.setProperty(ONLComponent.STATE, s);}
	public String getState() { return (properties.getProperty(ONLComponent.STATE));}
	public boolean isEnabled() 
	{ 
		boolean enabled = true;    
		if (enableField >= 0) 
		{
			//ExpCoordinator.print(new String("HWTableElement(" + toString() + ").isEnabled fields[" + enableField + "] = " + fields[enableField].getLabel()), TEST_ENABLE);
			enabled = ((Boolean)fields[enableField].getCurrentValue()).booleanValue();
		}
		return enabled;
	}
	public void setEnabled(Object a) 
	{ 
		if (enableField >= 0)
		{
			Boolean ov = (Boolean)fields[enableField].getCurrentValue();
			if (ov == null || (ov.booleanValue() != ((Boolean)a).booleanValue()))
			{
				//set field
				fields[enableField].setCurrentValue(a);
				//set edits
				if (isEnabled())
				{
					ExpCoordinator.print(new String("HWTableElement(" + toString() + ").setEnabled " + a.toString() + " enabling"), TEST_ENABLE);
					ExpCoordinator.theCoordinator.addEdit(new HWTableElement.EnableEdit(this, getCommand(hwtable.getAddElemSpec()), ExpCoordinator.theCoordinator.getCurrentExp()));
				}
				else
				{
					ExpCoordinator.print(new String("HWTableElement(" + toString() + ").setEnabled " + a.toString() + " disabling"), TEST_ENABLE);
					ExpCoordinator.theCoordinator.addEdit(new HWTableElement.DisableEdit(this, getCommand(hwtable.getDeleteElemSpec()), ExpCoordinator.theCoordinator.getCurrentExp()));
				}
			}
		}
	}
	public void addListener(HWTableElement.Listener l){ listeners.add(HWTableElement.Listener.class, l);}
	public void removeListener(HWTableElement.Listener l){ listeners.remove(HWTableElement.Listener.class, l);}
	private void fireEvent(HWTableElement.Event e)
	{
		int max = listeners.getListenerCount();
		int i = 0;
		Object[] list = listeners.getListenerList();
		//turns out this is a list of class, listener pairs so list looks like class1, listener1, class2, listener2 ... classn, listenern
		HWTableElement.Listener l;
		int change = e.getType();
		for (i = 1; i < list.length; i += 2)
		{
			l = (HWTableElement.Listener) (list[i]);
			l.valueChanged(e);
		}
	}
	public void setID(int i) 
	{ 
		properties.setProperty(ONLComponent.INDEX, i);
		if (commandLog != null) commandLog.setTitle(new String(hwtable.getLabel() + " Entry" + i + " Command Log"));
	}
	public int getID() { return (properties.getPropertyInt(ONLComponent.INDEX));}
	protected void clear()
	{
		int max = getNumFields();
		for (int i = 0; i < max; ++i)
			fields[i].clear();
		if (commandLog != null)
		{
			commandLog.clear();
		}
	}
	protected void showLog() { if (commandLog != null) commandLog.showLog();}
	public boolean isGenerated() { return (properties.getPropertyBool(GENERATED));}
	public void setGenerated(String g) { properties.setProperty(GENERATED, g);}
	public void setGenerated(boolean g) { properties.setProperty(GENERATED, g);}
	public boolean isHostGenerated() { return (properties.getPropertyBool(HOST_GENERATED));}
	public void setHostGenerated(String g) { properties.setProperty(HOST_GENERATED, g);}
	public void setHostGenerated(boolean g) { properties.setProperty(HOST_GENERATED, g);}
}

