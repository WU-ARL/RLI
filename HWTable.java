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
 * File: HWTable.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 11/15/2009
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
import java.lang.*;
import java.lang.reflect.Array;
import java.util.*;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.Color;
import javax.xml.stream.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;


public class HWTable extends ONLCTable implements Field.Owner,HWTableElement.Listener //,MonitorAction, PropertyChangeListener, 
{
	public static final int TEST_HWTABLE = 2;
	public static final int TEST_HWTMONITOR = 8;
	public static final int TEST_NHLISTENER = Topology.TEST_DEFRTS;
	private Hardware.Port port = null;
	private Hardware hardware = null;
	private Spec tableSpec = null;
	private JInternalFrame frame = null;
	private Vector<Field> fields = null;
	private Vector assigners = null;
	private Vector actions = null;
	private IDAssigner elementIDs = null;
	private ONLComponent.CfgMonJMenu cfgMonMenu = null;


	//field types
	//protected HWTableElement.Field fields[] = null;
	//protected int numFields = 0;

	///////////////////////////////////////// HWTable.TableMonitorable //////////////////////////////////////////
	private class TableMonitorable extends Hardware.HWMonitor.ChildMonitorable
	{
		private Vector elementSpecs = null;
		public TableMonitorable(HWTable t, HWTable.Spec spec)
		{
			super((ONLComponent)t,t.hardware.getMonitorable(), spec);
			elementSpecs = spec.getElementSpec().getMonitorSpecs();
		}
		public void initializeMMItems()
		{
			super.initializeMMItems();
			int max = elementSpecs.size();
			for (int i = 0; i < max; ++i)
			{
				addMMenuItem(createMMItem((CommandSpec)elementSpecs.elementAt(i)));
			}
		}
		public Hardware.MMenuItem createMMItem(CommandSpec cs)
		{
			if (cs instanceof HWTableElement.TableCommandSpec)
			{
				HWTable tbl = (HWTable)getONLComponent();
				return (new ElemMMenuItem(tbl, (HWTableElement.TableCommand)cs.createCommand(tbl)));
			}
			else return (super.createMMItem(cs));
		}
		public void removeElemMonitors(HWTableElement elem)
		{
			Vector mmis = getMMenuItems();
			int max = mmis.size();
			for (int i = 0; i < max; ++i)
			{
				Object mmi = mmis.elementAt(i);
				if (mmi instanceof HWTable.ElemMMenuItem)
					((HWTable.ElemMMenuItem)mmi).removeElemMonitors(elem);
			}
		}
	}//end HWTable.TableMonitorable 

	//////////////////////////////////////////////////////// HWTable.ColumnSpec //////////////////////////////////
	public static class ColumnSpec extends Vector
	{
		private String title = "";
		private int width = 50;

		protected static class XMLHandler extends DefaultHandler
		{
			protected ContentHandler hwhandler = null;
			protected XMLReader xmlReader = null;
			protected String currentElement = "";
			protected ColumnSpec columnSpec = null;
			protected ColumnElement columnElement = null;

			public XMLHandler(XMLReader xmlr, ContentHandler hwh, ColumnSpec cs)
			{
				super();
				xmlReader = xmlr;
				hwhandler = hwh;
				columnSpec = cs;
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{
				currentElement = new String(localName);
				if (localName.equals(ExperimentXML.FIELDNAME) || 
						localName.equals(ExperimentXML.NEWLINE) ||
						localName.equals(ExperimentXML.SYMBOL) )
				{
					columnElement = new ColumnElement(localName);
					columnSpec.add(columnElement);
				}
			}
			public void characters(char[] ch, int start, int length)
			{
				ExpCoordinator.print(new String("HWTable.Spec.ColumnSpec.XMLHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
				if ((currentElement.equals(ExperimentXML.FIELDNAME) || currentElement.equals(ExperimentXML.SYMBOL)) &&
						columnElement != null)
				{
					columnElement.value = new String(ch, start, length);
				}	  
			}
			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("HWTable.Spec.ColumnSpec.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
				if (currentElement.equals(ExperimentXML.FIELDNAME) || currentElement.equals(ExperimentXML.SYMBOL)) columnElement = null;
				if (localName.equals(ExperimentXML.COLUMN)) 
				{
					columnSpec.print(TEST_HWTABLE);
					xmlReader.setContentHandler(hwhandler);
				}
			}
		}//end HWTable.ColumnSpec.XMLHandler

		/////////////////////////////////////////// HWTable.ColumnSpec.ColumnElement /////////////////////////////////////////////
		public static class ColumnElement
		{
			String type = ExperimentXML.FIELDNAME;
			String value = "";

			public ColumnElement(String tp, String v)
			{
				type = new String(tp);
				value = new String(v);
			}
			public ColumnElement(String tp)
			{
				type = new String(tp);
			}
			public ColumnElement(ColumnElement ce)
			{
				this(ce.type, ce.value);
			}
			public String getType() { return type;}
			public String getValue() { return value;}
			public boolean isNewLine() { return (type.equals(ExperimentXML.NEWLINE));}
			public String toString() { return (new String(type + "|" + value));}
		}//end HWTable.ColumnSpec.ColumnElement

		/////////////////////////// ColumnSpec methods ///////////////////////////////////////////////
		public ColumnSpec(String uri, Attributes attributes)
		{
			super();
			title = new String(attributes.getValue(ExperimentXML.TITLE));
			width = Integer.parseInt(attributes.getValue(ExperimentXML.WIDTH));
		}
		public ColumnSpec(String ttl, int w)
		{
			super();
			title = new String(ttl);
			width = w;
		}
		public ColumnSpec(HWTable.ColumnSpec cs)
		{
			super();
			title = new String(cs.title);
			width = cs.width;
			int max = cs.size();
			for (int i = 0; i < max; ++i)
			{
				add(new ColumnElement((ColumnElement)cs.elementAt(i)));
			}
		}

		public String getTitle() { return title;}
		public int getWidth() { return width;}
		public ColumnElement getColumnElement(int i) { return ((ColumnElement)elementAt(i));}
		public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) { return (new XMLHandler(xmlr, hwh, this));}
		public void print(int d)
		{
			String rtn = new String("HWTable.ColumnSpec(" + title + ") width:" + width + " elements: ");
			int max = size();
			for (int i = 0; i < max; ++i)
				rtn = rtn.concat(new String(getColumnElement(i).toString() + ", "));
			ExpCoordinator.print(rtn, d);
		}
	}//end HWTable.ColumnSpec



	/////////////////////////////////// HWTable.ElemMenuAction /////////////////////////////////////////////////////////
	public static class ElemMenuAction extends Command.CAction
	{
		public ElemMenuAction(HWTable t, HWTableElement.TableCommandSpec cs)
		{
			super(t, new HWTableElement.TableCommand(cs, t, null));
		}
		public ElemMenuAction(HWTable t, HWTableElement.TableCommand cs)
		{
			super(t, cs);
		}
		/*
    public OpMenuAction(HWTable t, TableCommand cs)
    {
      super(t, cs);
      setCommand(new TableCommand(cs, t));
      }*/
		protected Command getCommand() 
		{ 
			HWTableElement.TableCommand template_spec = (HWTableElement.TableCommand)super.getCommand();
			Command rtn_spec = null;
			HWTable table = (HWTable)getONLComponent();
			int i = table.jtable.getSelectedRow();
			HWTableElement elem = (HWTableElement)table.getElement(i);
			rtn_spec = elem.getCommand(template_spec);
			if (rtn_spec != null) 
			{
				ExpCoordinator.print(new String("HWTable(" + getONLComponent().getLabel() + ").ElemMenuAction.getCommand"), TEST_HWTMONITOR);
				rtn_spec.print(TEST_HWTABLE);
			}
			return rtn_spec;
		}
		protected void addOperation(Command cspec, Object[] vals) 
		{
			HWTableElement.TableCommand spec = (HWTableElement.TableCommand)cspec;
			((HWTable)getONLComponent()).changeElement(spec.getEntry());
			if (!spec.isImmediate())
				ExpCoordinator.theCoordinator.addEdit(new HWTableElement.Edit(spec.getEntry(), spec, vals, ExpCoordinator.theCoordinator.getCurrentExp()));
			else
				((NCCPOpManager.Manager)getONLComponent()).addOperation(spec.createNCCPOp(vals));
		}
	}//end HWTable.ElemMenuAction

	/////////////////////////////////////////// HWTable.ElemMMenuItem ////////////////////////////////////////////////////////////////
	public static class ElemMMenuItem extends Hardware.MMenuItem
	{
		private HWTable hwtable = null;
		public ElemMMenuItem(HWTable t, HWTableElement.TableCommand tc)
		{
			super((ONLComponent)t, (Command)tc);
			hwtable = t;
		}
		public MonitorMenuItem.MMLParamOptions getParamOptions() 
		{
			return (new Hardware.MParamOptions(getCommand()));
		}
		protected Command getCommand() 
		{ 
			HWTableElement.TableCommand template_spec = (HWTableElement.TableCommand)super.getCommand();
			Command rtn_spec = null;
			int i = hwtable.jtable.getSelectedRow();
			HWTableElement elem = (HWTableElement)hwtable.getElement(i);
			rtn_spec = elem.getMonitorCommand(template_spec);
			if (rtn_spec != null) 
			{
				ExpCoordinator.print(new String("HWTable(" + hwtable.getLabel() + ").ElemMMenuItem.getCommand"), TEST_HWTABLE);
				rtn_spec.print(TEST_HWTABLE);
			}
			return rtn_spec;
		}
		public void removeElemMonitors(HWTableElement elem)
		{
			int max = monitors.size();
			Vector removals = new Vector();
			for(int i = 0; i < max; ++i)
			{
				Monitor mon = (Monitor)monitors.elementAt(i);
				Hardware.MDataType mdt = (Hardware.MDataType)mon.getDataType();
				if (elem == ((HWTableElement.TableCommand)mdt.getCommand()).getEntry()) removals.add(mon);
			}
			max = removals.size();
			ExpCoordinator.print(new String("HWTable(" + hwtable.getLabel() + ").ElemMMenuItem(" + getText() + ").removeElemMonitors removals:" + max + " numMonitors:" + monitors.size() + " element:" + elem.toString()), TEST_HWTMONITOR);
			for (int i = 0; i < max; ++i)
			{
				((Monitor)removals.elementAt(i)).stop();
			}
		}
	}//end HWTable.ElemMMenuItem

	//////////////////////////////////////////// HWTable.AddAction /////////////////////////////////////////////////////////
	public static class AddAction extends Command.CAction
	{
		private HWTableElement tableElement = null;
		public AddAction(HWTable t, HWTableElement.TableCommandSpec add_spec)
		{
			super(t, new HWTableElement.TableCommand(add_spec, t));
			//setCommand(new HWTableElement.TableCommand(t.getAddElemSpec(), t, null));
		}
		protected Command getCommand() 
		{ 
			HWTableElement.TableCommand template_spec = (HWTableElement.TableCommand)super.getCommand();
			HWTableElement.TableCommand rtn_spec = null;
			HWTable table = (HWTable)getONLComponent();
			tableElement = table.createNewElement();
			rtn_spec = (HWTableElement.TableCommand)tableElement.getCommand(template_spec);
			return rtn_spec;
		}
		protected void addOperation(Command cspec, Object[] vals) 
		{
			HWTableElement.TableCommand spec = (HWTableElement.TableCommand)cspec;
			//ExpCoordinator.theCoordinator.addEdit(new HWTableElement.AddEdit(spec.getElement(), spec, vals, ExpCoordinator.getCurrentExperiment()));
			ExpCoordinator.print(new String("HWTable.AddAction.addOperation tableElement:" + tableElement.toString()), TEST_HWTABLE);
			tableElement = (HWTableElement)((HWTable)getONLComponent()).addNewElement(tableElement);
			/*Vector tmp_params = cspec.getParams();
      int max = tmp_params.size();
      Param pelem;
      for (int i = 0; i < max; ++i)
	{
	  pelem = (Param)tmp_params.elementAt(i);
	  if (pelem instanceof Field && pelem.isEditable()) 
	    {
	      ExpCoordinator.print(new String("    setting field:" + pelem.label), TEST_HWTABLE);
	      tableElement.setField(pelem.label, pelem.getCurrentValue());
	    }
	}
			 */
			ExpCoordinator.print(new String("     tableElement:" + tableElement.toString()), TEST_HWTABLE);
			//super.addOperation(cspec, vals);
		}
	}//end HWTable.AddAction 

	//////////////////////////////////////////// HWTable.UpdateAction /////////////////////////////////////////////////////////
	public static class UpdateAction extends ElemMenuAction
	{
		private HWTableElement oldElement = null;
		public UpdateAction(HWTable t)
		{
			super(t, new HWTableElement.TableCommand(t.getAddElemSpec(), t));
			//setCommand(new HWTableElement.TableCommand(t.getAddElemSpec(), t, null));
			setPrefix( new Object[]{"Update Element will cause a delete followed by an add."});
			setTitle(new String("Edit " + t.tableSpec.getElementSpec().getElementName()));
		}
		protected Command getCommand() 
		{ 
			Command cmd = super.getCommand();
			if (cmd != null) oldElement = new HWTableElement(((HWTableElement.TableCommand)cmd).getEntry());
			else oldElement = null;
			return cmd;
		}
		protected void addOperation(Command cspec, Object[] vals) 
		{
			HWTableElement.TableCommand spec = (HWTableElement.TableCommand)cspec;
			if (oldElement != null)
			{
				HWTableElement.TableCommand del_cmd = (HWTableElement.TableCommand)oldElement.getCommand(((HWTable)getONLComponent()).getDeleteElemSpec());

				if (del_cmd != null)
					ExpCoordinator.theCoordinator.addEdit(new HWTableElement.DisableEdit(oldElement, del_cmd, ExpCoordinator.theCoordinator.getCurrentExp()));
			}
			HWTableElement tableElement = spec.getEntry();
			ExpCoordinator.print(new String("HWTable.UpdateAction.addOperation tableElement:" + tableElement.toString()), TEST_HWTABLE);
			cspec.setImmediate(false);	
			ExpCoordinator.theCoordinator.addEdit(new HWTableElement.AddEdit(tableElement, spec, ExpCoordinator.theCoordinator.getCurrentExp()));
			
			//super.addOperation(cspec, vals);
		}
	}//end HWTable.UpdateAction 

	//////////////////////////////////////////// HWTable.DeleteAction /////////////////////////////////////////////////////////
	public static class DeleteAction extends Command.CAction
	{
		public DeleteAction(HWTable t, HWTableElement.TableCommandSpec del_spec)
		{
			super(t, new HWTableElement.TableCommand(del_spec, t));
			//super(t, del_spec);
			//setCommand(new HWTableElement.TableCommand(t.getDeleteElemSpec(), t));
		}
		protected Command getCommand() 
		{ 
			HWTableElement.TableCommand template_spec = (HWTableElement.TableCommand)super.getCommand();
			HWTable table = (HWTable)getONLComponent();
			int i = table.jtable.getSelectedRow();
			HWTableElement elem = (HWTableElement)table.getElement(i);
			Command rtn_spec = elem.getCommand(template_spec);
			if (rtn_spec != null) return rtn_spec;
			return null;
		}
		protected void addOperation(Command cspec, Object[] vals) 
		{
			HWTableElement.TableCommand spec = (HWTableElement.TableCommand)cspec;
			ExpCoordinator.print(new String("HWTable(" + getONLComponent().getLabel() + ").DeleteAction.addOperation cspec:" + cspec.getLabel()), TEST_HWTABLE);
			((HWTable)getONLComponent()).removeElement(((HWTableElement.TableCommand)spec).getEntry());
			//ExpCoordinator.theCoordinator.addEdit(new HWTableElement.DeleteEdit(((TableCommand)spec).getEntry(), (TableCommand)spec, vals, ExpCoordinator.theCoordinator.getCurrentExp()));
			//((HWTable)getONLComponent()).addNewElement(tableElement);
			//super.addOperation(cspec, vals);
		}
	}//end HWTable.DeleteAction


	////////////////////////////////////////////////// HWTable.CLogAction /////////////////////////////////////////////////////
	public static class CLogAction extends AbstractAction
	{
		private HWTable hwtable = null;

		public CLogAction (HWTable hwt) { this("Show Command Log", hwt);}
		public CLogAction (String nm, HWTable hwt)
		{
			super(nm);
			hwtable = hwt;
		}
		public void actionPerformed(ActionEvent e) { actionPerformed();}
		public void actionPerformed()
		{
			int i = hwtable.jtable.getSelectedRow();
			HWTableElement elem = (HWTableElement)hwtable.getElement(i);
			elem.showLog();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////// HWTable.Spec //////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class Spec implements ComponentXMLHandler.Spec
	{
		//public static final String FIELD = "field";
		//public static final String FIELDNAME = "fieldName";
		//public static final String FIELDPARAM = "fieldParam";
		//public static final String FIELDS = "fields";
		//private ColumnSpec columns[] = null; //move to HWTableElement.Spec
		//private HWTParamSpec fields[] = null;
		//private Vector operations = null;
		private Vector fieldSpecs = null; 
		private Vector assignerSpecs = null; 
		private Vector monitorSpecs = null; 
		private Vector commandSpecs = null;
		private HWTableElement.Spec elementSpec = null;
		protected ONLPropertyList properties = null;

		//////////////////////////////////////////////////////// HWTable.Spec.XMLHandler //////////////////////////////////
		public static class XMLHandler extends ComponentXMLHandler
		{

			protected HWTable.Spec tableSpec = null;
			//protected ContentHandler hwhandler = null;
			//protected XMLReader xmlReader = null;
			//protected int currentIndex = 0;

			public XMLHandler(XMLReader xmlr, ContentHandler hwh, HWTable.Spec ts)
			{	
				super(xmlr, ts, hwh);
				tableSpec = ts;
			}

			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{
				super.startElement(uri, localName, qName, attributes);
				ExpCoordinator.print(new String("HWTable.Spec.XMLHandler.startElement localName:" + localName), TEST_HWTABLE);
				if (localName.equals(ExperimentXML.ELEMENT))
				{
					HWTableElement.Spec espec = new HWTableElement.Spec(uri, attributes, (HWTable.Spec)getSpec());
					((HWTable.Spec)getSpec()).elementSpec = espec;
					if (xmlReader == null) ExpCoordinator.print(new String("    xmlReader is null"));
					xmlReader.setContentHandler(espec.getXMLHandler(xmlReader, this));
				}
			}

			public void characters(char[] ch, int start, int length)
			{
				ExpCoordinator.print(new String("HWTable.Spec.XMLHandler.characters " + currentElement), ExperimentXML.TEST_XML);
				if (currentElement.equals(ExperimentXML.ID)) tableSpec.properties.setProperty(ExperimentXML.ID, new String(ch, start, length));
			}
			/*
	public void endElement(String uri, String localName, String qName)
	{
	ExpCoordinator.print(new String("HWTable.Spec.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
	if (localName.equals(ExperimentXML.FIELDS)) tableSpec.printFields(TEST_HWTABLE);
	if (localName.equals(ExperimentXML.FIELDS) || 
	localName.equals(ExperimentXML.DISPLAY) ||
	localName.equals(ExperimentXML.ELEM_OPS) || 
	localName.equals(ExperimentXML.TABLE_OPS))
	currentIndex = 0;
	if (localName.equals(ExperimentXML.TABLE))
	{
	xmlReader.setContentHandler(hwhandler);
	}
	}
			 */
			public boolean isSection(String nm)
			{
				if (!super.isSection(nm))
				{
					return (nm.equals(ExperimentXML.ELEMENT));
				}
				return true;
			}
		}//end HWTable.Spec.XMLHandler

		//////////////////////////////////////// HWTable.Spec methods ///////////////////////////////////////////////////////////////

		public Spec(String uri, Attributes attributes, boolean perport)
		{
			this(new String(attributes.getValue(ExperimentXML.TITLE)), String.valueOf(perport));
		}

		public Spec(String ttl, String perport)
		{
			properties = new ONLPropertyList(this);
			properties.setProperty(ExperimentXML.TITLE, new String(ttl));
			properties.setProperty(ExperimentXML.PERPORT, new String(perport));


			fieldSpecs = new Vector();
			assignerSpecs = new Vector();
			commandSpecs = new Vector();
			monitorSpecs = new Vector();
		}

		public Spec(Spec sp) 
		{
			this(sp.properties.getProperty(ExperimentXML.TITLE), sp.properties.getProperty(ExperimentXML.PERPORT));
			Vector tmp_v = sp.getFieldSpecs();
			int max = tmp_v.size();
			int i;
			for(i = 0; i < max; ++i)
			{
				fieldSpecs.add(new FieldSpec((FieldSpec)tmp_v.elementAt(i)));
			}
			tmp_v = sp.getAssignerSpecs();
			max = tmp_v.size();
			for (i = 0; i < max; ++i)
			{
				assignerSpecs.add(new ComponentXMLHandler.AssignerSpec((ComponentXMLHandler.AssignerSpec)tmp_v.elementAt(i)));
			}
			tmp_v = sp.getCommandSpecs();
			max = tmp_v.size();
			for(i = 0; i < max; ++i)
			{
				commandSpecs.add(new CommandSpec((CommandSpec)tmp_v.elementAt(i)));
			}
			tmp_v = sp.getMonitorSpecs();
			max = tmp_v.size();
			for(i = 0; i < max; ++i)
			{
				monitorSpecs.add(new CommandSpec((CommandSpec)tmp_v.elementAt(i)));
			}
			elementSpec = new HWTableElement.Spec(sp.elementSpec, this);
		}

		public HWTable createTable(ONLComponent c)//Hardware.Port p)
		{
			HWTable rtn = new HWTable(c, properties.getProperty(ExperimentXML.TITLE), this);
			HWTable.TableModel tm = (HWTable.TableModel)rtn.getTableModel();

			if (tm == null) ExpCoordinator.print("HWTable.Spec.createTable tablemodel is null");
			Vector cols = elementSpec.columnSpecs;
			int max = getNumCols();
			for (int i = 0; i < max; ++i)
			{
				tm.setPreferredWidth(i, ((ColumnSpec)cols.elementAt(i)).getWidth());
			}
			String str = properties.getProperty(ExperimentXML.ID);
			if (str != null) rtn.setID(Integer.parseInt(str));
			return rtn;
		}

		/*public HWTable createTable(Hardware hwi)
    {
      HWTable rtn = new HWTable(hwi, properties.getProperty(ExperimentXML.TITLE), this);
      HWTable.TableModel tm = (HWTable.TableModel)rtn.getTableModel();

      int max = elementSpec.getNumCols();
      for (int i = 0; i < max; ++i)
	{
	  tm.setPreferredWidth(i, elementSpec.getColumnSpec(i).getWidth());
	}
      String str = properties.getProperty(ExperimentXML.ID);
      if (str != null) rtn.setID(Integer.parseInt(str));
      return rtn;
      }*/
		public String[] getColumnNames()
		{
			int max = elementSpec.getNumCols();
			String rtn[] = new String[max];
			for (int i = 0; i < max; ++i)
			{
				rtn[i] = new String(elementSpec.getColumnSpec(i).getTitle());
			}
			return rtn;
		}
		public Class[] getColumnTypes()
		{
			int max = elementSpec.getNumCols();
			Class rtn[] = new Class[max];
			for (int i = 0; i < max; ++i)
			{
				if (i == elementSpec.getEnableCol()) rtn[i] = Boolean.class;
				else
					rtn[i] = String[].class;
			}
			return rtn;
		}
		/*
    public HWTParamSpec getField(String nm)
    {
      int max = getNumFields();
      for (int i = 0; i < max; ++i)
	{
	  if (fields[i].getLabel().equals(nm)) return (fields[i]);
	}
      return null;
    }
    public HWTParamSpec[] getFields() { return fields;}
    public void printFields(int d)
    {
      String rtn = "fields[]= ";
      int max = getNumFields();
      for (int i = 0; i < max; ++i)
	rtn = rtn.concat(new String(fields[i].toString() + ", "));
      ExpCoordinator.print(rtn, d);
    }
    public TableCommandSpec getAddElemSpec()
    {
      int max = operations.size();
      for (int i = 0; i < max; ++i)
	{
	  TableCommandSpec elem = (TableCommandSpec)operations.elementAt(i);
	  if (elem.isAdd()) return elem;
	}
      return null;
    }
    public TableCommandSpec getDeleteElemSpec()
    {
      int max = operations.size();
      for (int i = 0; i < max; ++i)
	{
	  TableCommandSpec elem = (TableCommandSpec)operations.elementAt(i);
	  if (elem.isDelete()) return elem;
	}
      return null;
    }
		 */
		//interface ComponentXMLHandler.Spec
		public CommandSpec addCommand(CommandSpec cspec)
		{
			if (cspec.isMonitor()) monitorSpecs.add(cspec);
			if (cspec.isCommand()) commandSpecs.add(cspec);
			return cspec;
		}
		public void addTable(HWTable.Spec htspec) {}
		public void addField(FieldSpec fspec) { fieldSpecs.add(fspec);}
		public void addAssigner(ComponentXMLHandler.AssignerSpec aspec) { assignerSpecs.add(aspec);}
		public String getEndToken() { return (ExperimentXML.TABLE);}
		public boolean isPort() { return (properties.getPropertyBool(ExperimentXML.PERPORT));}
		public Vector getMonitorSpecs() { return monitorSpecs;}
		public Vector getCommandSpecs() { return commandSpecs;}
		public Vector getFieldSpecs() { return fieldSpecs;}
		public Vector getTableSpecs() { return null;}
		public Vector getAssignerSpecs() { return assignerSpecs;}
		//end interface ComponentXMLHandler.Spec
		public HWTableElement.Spec getElementSpec() { return elementSpec;}
		public int getNumCols() { return (elementSpec.getNumCols());}
		public int getNumFields() { return (elementSpec.getNumFields());}
		public boolean isEnable() { return (elementSpec.isEnabled());}
		/*public boolean hasOp(MonitorDataType.Base mdt)
    {
      int max = operations.size();
      for (int i = 0; i < max; ++i)
	{
	  TableCommandSpec elem = (TableCommandSpec)operations.elementAt(i);
	  if (elem.getOpcode() == mdt.getMonitorID()) return true;
	}
      return false;
      }
    protected ColumnSpec getColumnSpec(int i) 
    {
      if (i >= 0 && i < getNumCols()) return (columns[i]);
      else return null;
    }
    protected Vector getOperations() { return operations;}
		 */
		public String getTitle() { return (properties.getProperty(ExperimentXML.TITLE));}
		public String getElementName() { return (elementSpec.getElementName());}
		public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) { return (new XMLHandler(xmlr, hwh, this));}
		public boolean isPerPort() { return (isPort());}
	} //end HWTable.Spec

	///////////////////////////////////////////// HWTable.HWTContentHandler ///////////////////////////////////////////////////////////
	private class HWTContentHandler extends DefaultHandler
	{
		private ExperimentXML expXML = null;
		private String currentElement = "";
		//private HWTableElement currentTElement = null;
		private HWTable hwtable = null;
		private int currentField = 0;

		public HWTContentHandler(ExperimentXML exp_xml, HWTable hwt)
		{
			super();
			expXML = exp_xml;
			hwtable = hwt;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			ExpCoordinator.print(new String("HWTable(" + hwtable.getLabel() + ").HWTContentHandler.startElement localName:" + localName), ExperimentXML.TEST_XML);
			currentElement = new String(localName);
			if (localName.equals(ExperimentXML.ELEMENT)) 
			{
				HWTableElement telement = (HWTableElement)hwtable.createNewElement();
				if (attributes.getValue(HWTableElement.GENERATED) != null) telement.setGenerated(attributes.getValue(HWTableElement.GENERATED));	
				if (attributes.getValue(HWTableElement.HOST_GENERATED) != null) telement.setHostGenerated(attributes.getValue(HWTableElement.HOST_GENERATED));
				expXML.setContentHandler(telement.getContentHandler(expXML));
			}
			if (localName.equals(ExperimentXML.FIELD)) 
			{
				String plabel = attributes.getValue(ExperimentXML.FIELDNAME);
				ExpCoordinator.print(new String("    field:" + plabel), ExperimentXML.TEST_XML);
				Field field = hwtable.getField(plabel);
				expXML.setContentHandler(field.getContentHandler(expXML));
			}
			//need to read and support table wide commands
		}
		public void endElement(String uri, String localName, String qName)
		{
			if (localName.equals(ExperimentXML.HWTABLE) || localName.equals(ExperimentXML.ROUTE_TABLE)) 
			{
				expXML.removeContentHandler(this);
			}
		}
	}

	/////////////////////////////////////////////// HWTable.TableModel ////////////////////////////////////////////////////////////////
	//class TableModel
	public static class TableModel extends ONLCTable.TableModel 
	{
		private HWTable hwTable = null;

		public TableModel(HWTable hwt)
		{
			super(hwt);
		}
		public TableCellRenderer getRenderer(int col) 
		{
			if (getColumnClass(col) == Boolean.class) return null;
			else
			{
				ONLCTable.MultiLineCell rtn = new ONLCTable.MultiLineCell(Color.red);
				rtn.setAlignmentX(Component.LEFT_ALIGNMENT);
				return (rtn);
			}
			//return (super.getRenderer(col));
		}
	}//end inner class TableModel

	///////////////////////////////////////////////////// HWTAction ///////////////////////////////////////////////////
	private class HWTAction extends AbstractAction 
	{ 
		public HWTAction(String ttl) 
		{
			super(ttl);  
		}
		public HWTAction() 
		{
			this(getLabel());
		}
		public void actionPerformed(ActionEvent e) 
		{
			show();
		}
	}//end HWTAction


	/*  public HWTable(String uri, Attributes attributes, Hardware hwi) throws IOException
    {
      super(new String(attributes.getValue(Hardware.XMLHandler.TITLE)),ONLComponent.HWTABLE,ec, null);
      setParent(hwi);
      hardware = hwi;
      createTableModel();
      //initFields(port);
      read(tr);
      addToDescription(1, "Port " + port.getLabel() + "Filter Table");
      }*/

	//public HWTable(Hardware.Port p, String ttl, Spec sp)
	public HWTable(ONLComponent oc, String ttl, Spec sp){ this(oc, ttl, sp, ONLComponent.HWTABLE);}
	public HWTable(ONLComponent oc, String ttl, Spec sp, String tp)
	{
		super(ttl, tp, oc.getExpCoordinator(), null, oc);
		port = null;
		elementIDs = new IDAssigner(true);
		setID(-1);
		if (oc instanceof Hardware.Port)
		{
			port = (Hardware.Port)oc;
			hardware = (Hardware)port.getParent();
			addToDescription(1, "Port " + port.getLabel() + " " + ttl);
		}
		if (oc instanceof Hardware)
		{
			hardware = (Hardware)oc;
			addToDescription(1, "Hardware " + hardware.getLabel() + " " + ttl);
		}
		tableSpec = sp;
		cfgMonMenu = new ONLComponent.CfgMonJMenu(this, "Operations");
		assigners = new Vector();
		Vector aspecs = tableSpec.getAssignerSpecs();
		int max = aspecs.size();
		int i = 0;
		for (i = 0; i < max; ++i)
		{
			ComponentXMLHandler.AssignerSpec as = (ComponentXMLHandler.AssignerSpec)aspecs.elementAt(i);
			assigners.add(as.createAssigner());
		}
		fields = new Vector<Field>();
		max = tableSpec.fieldSpecs.size();
		for (i = 0; i < max; ++i)
		{
			fields.add(((FieldSpec)tableSpec.fieldSpecs.elementAt(i)).createField(this));
		}
		actions = new Vector();
		max = tableSpec.commandSpecs.size();
		CommandSpec cs;
		for (i = 0; i < max; ++i)
		{
			cs = (CommandSpec)tableSpec.commandSpecs.elementAt(i);
			actions.add(new Command.CAction(this, cs.createCommand(this)));
			//tableOps.add(new HWTableOpPanel(cs, this));
		}
		max = fields.size();
		for (i = 0; i < max; ++i)
		{
			Action ua = ((Field)fields.elementAt(i)).getUpdateAction();
			if (ua != null) actions.add(ua);
		}
		if (tableSpec.elementSpec.hasCommandLog()) actions.add(new CLogAction(this));
		max = tableSpec.elementSpec.commandSpecs.size();
		HWTableElement.TableCommandSpec tcs;
		for (i = 0; i < max; ++i)
		{
			tcs = (HWTableElement.TableCommandSpec)tableSpec.elementSpec.commandSpecs.elementAt(i);
			if (tcs.isAdd())
			{
				actions.add(new AddAction(this, tcs));
				if (tcs.hasUpdate()) actions.add(new UpdateAction(this));
			}
			else
			{
				if (tcs.isDelete())
					actions.add(new DeleteAction(this, tcs));
				else
					actions.add(new ElemMenuAction(this, (HWTableElement.TableCommand)tcs.createCommand(this)));
			}
			//tableOps.add(new HWTableOpPanel(cs, this));
		}
		TableMonitorable t_mon = new TableMonitorable(this, tableSpec);
		setMonitorable(t_mon);
		t_mon.initializeMMItems();
		initializeMenu();
		createTableModel();
	}

	public IDAssigner.ID getNewID(FieldSpec.AssignerSpec as)
	{
		if (as.isTableAssigner())
		{
			IDAssigner assigner = getIDAssigner(as.getName());
			if (assigner != null) 
				return (assigner.getNewID());
			else return null;
		}
		else return(super.getNewID(as));
	}

	public IDAssigner getIDAssigner(String nm)
	{
		int max = assigners.size();
		for (int i = 0; i < max; ++i)
		{
			IDAssigner ias = (IDAssigner)assigners.elementAt(i);
			if (ias.getName().equals(nm)) return ias;
		}
		return null;
	}

	private void initializeMenu()
	{
		int max = actions.size();
		for (int i = 0; i < max; ++i)
		{
			cfgMonMenu.addCfg((Action)actions.elementAt(i));
		}
		getMonitorable().addActions(cfgMonMenu);
	}
	/*
    public HWTable(Hardware hwi, String ttl, Spec sp)
    {
    super(ttl, ONLComponent.HWTABLE, hwi.getExpCoordinator(), null, hwi);
    port = null;
    hardware = hwi;
    tableSpec = sp;
    addToDescription(1, "Hardware " + hardware.getLabel() + " " + ttl);
    }
	 */
	public boolean areEqual(Object o1, Object o2)
	{
		return (o1 != null && 
				o2 != null && 
				(((HWTableElement)o1).isEqual((HWTableElement)o2) || o1 == o2));
	}
	public Object addElement(ONLCTable.Element r, int s, boolean can_repeat)
	{
		ExpCoordinator.print("HWTable.addElement", TEST_HWTABLE);
		Object rtn = super.addElement(r,s,can_repeat);
		elementIDs.addID((IDAssigner.IDable)rtn);
		if (rtn != null)
			{
			ExpCoordinator.print(new String("HWTable(" + getLabel() + ").addElement(" + rtn.toString() + ") adding nexthop listener"), TEST_NHLISTENER);
			((HWTableElement)rtn).addNextHopListener();
			}
		return rtn;
	}
	protected Object addNewElement(ONL.Reader rdr, int i) //adds an element read from some stream
	{
		HWTableElement ntbl_e = null;
		try
		{
			ntbl_e = new HWTableElement(this, rdr);
		}
		catch (java.io.IOException e) 
		{
			ExpCoordinator.print(new String("HWTable.addNewElement ioexception: " + e.getMessage()));
		}
		//    catch (java.text.ParseException e2) {}
		return (addNewElement(ntbl_e, i));
	}
	protected Object addNewElement(ONLCTable.Element elem, int i) { return (addNewElement(elem, i, true));}//adds new element making a copy of elem
	protected Object addNewElement(ONLCTable.Element elem, boolean can_repeat) { return (addNewElement(elem, size(), can_repeat));}//adds new element making a copy of elem
	protected Object addNewElement(ONLCTable.Element elem, int i, boolean can_repeat) //adds new element making a copy of elem
	{
		if (elem == null) return null;
		HWTableElement tbl_e = new HWTableElement((HWTableElement)elem);
		// port.addStat(tbl_e.getStats());
		//tbl_e.resetUpdateEdit(this);
		HWTableElement rtn = (HWTableElement)addElement(tbl_e, i, can_repeat);
		rtn.addListener(this);
		if (rtn != null)
		{
			HWTableElement.TableCommand cmd = rtn.getCommand(getAddElemSpec());
			ExpCoordinator.print(new String("HWTable(" + getLabel() + ").addNewElement element:" + rtn.toString() + " elementGiven:" + elem.toString()), TEST_HWTABLE);
			expCoordinator.addEdit(new HWTableElement.AddEdit(rtn, cmd, expCoordinator.getCurrentExp()));

		}
		//if (rtn != null && ExpCoordinator.isRecording())
		//ExpCoordinator.recordEvent(SessionRecorder.ADD_FILTER, new String[]{port.getLabel(), fd.getRecordedString()});
		return rtn;
	}
	protected HWTable.Spec getTableSpec() { return tableSpec;}
	protected HWTableElement createNewElement() { return (new HWTableElement(this));}
	protected HWTableElement.TableCommandSpec getAddElemSpec() { return (tableSpec.elementSpec.getAddSpec());}
	protected HWTableElement.TableCommandSpec getDeleteElemSpec() { return (tableSpec.elementSpec.getDeleteSpec());}
	protected Object addNewElement(int i) { return (addNewElement(createNewElement(), i));}//creates new element and adds //appears no one ever uses this
	public void removeElement(ONLCTable.Element e)
	{
		super.removeElement(e);
		expCoordinator.addEdit(getDeleteEdit(e));
		elementIDs.removeIDable((IDAssigner.IDable)e);
		((TableMonitorable)getMonitorable()).removeElemMonitors((HWTableElement)e);
		ExpCoordinator.print(new String("HWTable(" + getLabel() + ").removeElement(" + e.toString() + ")"), TEST_HWTABLE);
		((HWTableElement)e).clear();
	}
	public void removeFromList(ONLCTable.Element r)
	{
		if (r!= null && containsElement(r))
		{
			super.removeFromList(r);
			HWTableElement tbl_e = (HWTableElement)r;
			//port.removeStat(fd.getStats());
			tbl_e.removeListener(this);
			//if (ExpCoordinator.isRecording())
			// {
			//  ExpCoordinator.recordEvent(SessionRecorder.DELETE_FILTER, new String[]{port.getLabel(), fd.getRecordedString()});
			//}
		}
	}
	private void setJTable(JTable t) { jtable = t;}

	//HWTableElement.Listener
	public void valueChanged(HWTableElement.Event e)
	{
		changeElement((ONLCTable.Element)e.getSource());
	}
	//end HWTableElement.Listener

	protected NCCPOpManager createOpManager() 
	{ 
		if (port != null) return (port.getOpManager());
		else return (hardware.getOpManager());
	}
	public Hardware.Port getPort() { return port;}
	//public Hardware getHardware() { return hardware;}
	public ONLComponent getONLComponent() { return this;}
	public Command getCommand(CommandSpec tcs) 
	{ 
		if (tcs == null) return null;
		else return (getCommand(tcs.getOpcode()));
	}
	public Command getCommand(int opcode)
	{
		int max = actions.size();
		Command elem;
		for (int i = 0; i < max; ++i)
		{
			elem = ((Command.CAction)actions.elementAt(i)).getCommand();
			if (opcode == elem.getOpcode()) return elem;
		}
		return null;
	}
	public Command getMonitorCommand(int opcode) { return (((Hardware.HWMonitor.ChildMonitorable)monitorable).getCommand(opcode));}
	/*
  //interface MonitorAction
  public Monitor addMonitor(Monitor m){return null;}
  public void removeMonitor(Monitor m){}
  public boolean isDataType(MonitorDataType.Base mdt)
  {
    return false;
  }
  public Monitor addMonitor(MonitorDataType.Base mdt)
  {
  }
  //end interface MonitorAction
	 */
	protected ONLComponent.Undoable getAddEdit(HWTableElement telem)  { return (new HWTableElement.AddEdit(telem, (HWTableElement.TableCommand)telem.getCommand(getAddElemSpec()), expCoordinator.getCurrentExp()));}
	protected ONLComponent.Undoable getAddEdit(Vector telems)  { return null;}
	protected ONLComponent.Undoable getDeleteEdit(Vector telems) { return null;}
	protected ONLComponent.Undoable getDeleteEdit(ONLCTable.Element elem)
	{ 
		HWTableElement telem = (HWTableElement)elem;
		HWTableElement.TableCommand cmd = (HWTableElement.TableCommand)telem.getCommand(getDeleteElemSpec());
		if (cmd != null)
			return (new HWTableElement.DeleteEdit(telem, cmd, expCoordinator.getCurrentExp()));
		else return null;
	}
	protected String[] getColumnNames()
	{
		return (tableSpec.getElementSpec().getColumnNames());
	}
	protected Class[] getColumnTypes()
	{
		return (tableSpec.getElementSpec().getColumnTypes());
	}

	public Action getAction() { return (new HWTAction(tableSpec.getTitle()));}


	public ONLCTable.TablePanel getPanel() 
	{ 
		ONLCTable.TablePanel panel = getPanel(tableModel);
		int max = fields.size();
		JPanel tmp_panel;
		for (int i = 0; i < max; ++i)
		{
			Field.FPanel fpanel = new Field.FPanel((Field)fields.elementAt(i), false);
			fpanel.setEnabled(false);
			panel.add(fpanel, i);
		}
		jtable = panel.getJTable();
		jtable.setRowHeight(tableSpec.getElementSpec().getRowHeight());
		return panel;
	}

	private JInternalFrame getFrame() 
	{
		if (frame == null)
		{
			if (port == null && hardware == null) ExpCoordinator.print("HWTable.getFrame port and hardware is null");
			frame = new JInternalFrame(new String(getParent().getLabel() + " " + tableSpec.getTitle())); 

			JPanel panel = getPanel();
			frame.getContentPane().add(panel);
			JMenuBar mb = new JMenuBar();
			frame.setJMenuBar(mb);
			//JMenu menu = new JMenu("Operations");
			mb.add(cfgMonMenu);
			//menu.add(new AddAction(this));

			//add other operations
			//Vector tcs = tableSpec.getOperations();	  
			//int max = tcs.size();
			//HWTable.TableCommandSpec op;
			//for (int i = 0; i < max; ++i)
			//{
			//  op = (HWTable.TableCommandSpec)tcs.elementAt(i);
			//if (!(op.isAdd() || op.isDelete()))
			//    menu.add(new OpMenuAction(this, op));
			//}
			//menu.add(new DeleteAction(this));
		}
		return frame;
	}
	public void clear()
	{
		Vector del_elems = new Vector(getList());
		super.clear();
		if (!del_elems.isEmpty())
		{
			int max = del_elems.size();
			ONLComponent.Undoable delEdit;
			HWTableElement elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (HWTableElement)del_elems.elementAt(i);
				delEdit = getDeleteEdit(elem);
				if (delEdit != null) delEdit.commit();
				elem.clear();
			}
		}
		if (frame != null && frame.isShowing()) frame.dispose();
	}
	public void removeGeneratedElements()
	{
		Vector del_elems = new Vector(getList());
		if (!del_elems.isEmpty())
		{
			int max = del_elems.size();
			ONLComponent.Undoable delEdit;
			HWTableElement elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (HWTableElement)del_elems.elementAt(i);
				if (elem.isGenerated()) 
				{
					ExpCoordinator.print(new String("HWTable(" + getLabel() + ").removeGeneratedElements removing(" + elem.toString() + ")"), Topology.TEST_DEFRTS);
					removeElement(elem);
				}
			}
		}
		else
			ExpCoordinator.print(new String("HWTable(" + getLabel() + ").removeGeneratedElements list empty"), Topology.TEST_DEFRTS);
		
	}
	public void createTableModel()
	{
		if (getTableModel() == null) setTableModel(new TableModel(this));
	}
	public void show()
	{
		if (frame == null)
		{
			getFrame();
			if (port != null) port.addToDesktop(frame);
			else hardware.addToDesktop(frame);
			frame.addInternalFrameListener(new ONLComponent.ONLInternalFrameListener(this));
		}
		frame.setSize(850,250);
		//frame.pack();

		if (port != null) port.showDesktop(frame);
		else hardware.showDesktop(frame);
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.HWTABLE);
		xmlWrtr.writeAttribute(ExperimentXML.TITLE, getLabel());
		//write fields
		int max = fields.size();
		int i = 0;
		for (i = 0; i < max; ++i)
		{
			((Field)fields.elementAt(i)).writeXML(xmlWrtr);
		}
		max = size();
		for (i = 0; i < max; ++i)
		{
			HWTableElement elem = (HWTableElement)getElement(i);
			if (elem.isWritable()) elem.writeXML(xmlWrtr);
		}
		xmlWrtr.writeEndElement();
	}
	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new HWTContentHandler(exp_xml, this));}   
	/* public Hardware getHardware()
  {
    ONLComponent c = getParent();
    if (c instanceof Hardware) return ((Hardware)c);
    if (c instanceof Hardware.Port) return ((Hardware)c.getParent());
    return null;
    }*/

	//interface Field.Owner
	public Field getField(String str)
	{
		int max = fields.size();
		Field elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (Field)fields.elementAt(i);
			if (elem.getLabel().equals(str)) return elem;
		}
		return null;
	}
	//end interface Field.Owner
	protected Vector getFields() { return fields;}
	protected boolean merge(ONLComponent c)
	{
		HWTable hwt = (HWTable)c;
		//merge fields
		int max = fields.size();
		for (int i = 0; i < max; ++i)
		{
			Field f1 = fields.elementAt(i);
			Field f2 = hwt.getField(f1.getLabel());
			if (f2 != null && !f1.isEqual(f2))
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String(getLabel() + " " + f1.getLabel() + " Inconsistency master has " + f1.getLabel() + ":" + f1.getCurrentValue().toString() + " other has " + f1.getLabel() + ":" + f2.getCurrentValue().toString() + "\n"),
						"Hardware Field Error",
						"Hardware Field Error",
						StatusDisplay.Error.LOG)); 
		}
		return (super.merge(c));
	}
}
