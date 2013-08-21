//import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class Field extends Param
{
	public static final int TEST_ASSIGNMENT = 8;
	public static final int TEST_CURRENTVAL = Param.TEST_PARAM;
	public static final int TEST_UPDATE = 3;
	public static final int TEST_NHLISTENER = Topology.TEST_DEFRTS;
	private int id = 0;
	private UpdateAction updateAction = null;
	private FieldSpec.UpdateCommandSpec updateCommandSpec = null;
	private Field.Owner owner = null;
	private EventListenerList listeners = null;
	private IDAssigner.ID assignedDefault = null;

	//////////////////////////////////////////////////// Field.FContentHandler ///////////////////////////////////////////////
	protected static class FContentHandler extends Param.PContentHandler
	{
		public FContentHandler(ExperimentXML exp_xml, Field p) { super(exp_xml, p);}
		public void characters(char[] ch, int start, int length)
		{
			currentParam.setDefaultFromString(new String(ch, start, length));
		}
	}

	///////////////////////////////////////////////////////// Field.Owner interface //////////////////////////////////////////
	public interface Owner
	{
		public Field getField(String nm);
	}

	///////////////////////////////////////////////////// Field.Listener interface ///////////////////////////////////////////
	public interface Listener extends EventListener
	{
		public void valueChanged(Field.DataEvent e);
	}

	///////////////////////////////////////////// Field.UIRepCopy ///////////////////////////////////////////////////////////
	public static class UIRepCopy implements Field.Listener
	{
		//private Param.PComponent component = null;
		private JComponent component = null;
		private Field field = null;
		private UIRepCopy.Listener listener = null;

		/////////////////////////////////////////// Field.UIRepCopy.Listener ////////////////////////////////////////////////////
		private class Listener implements ActionListener, FocusListener
		{
			private UIRepCopy uiRepCopy = null;
			public Listener(UIRepCopy uir) { uiRepCopy = uir;}

			//FocusListener
			public void focusLost(FocusEvent e) 
			{
				actionPerformed(null);
			}
			public void focusGained(FocusEvent e) { }
			//ActionListener
			public void actionPerformed(ActionEvent e)
			{
				//field.setDefaultValue(uiRepCopy.field.getValueFromUIRep(field_ui));
				ExpCoordinator.print(new String("Field.UIRepCopy.Listener.actionPerformed"), HWTable.TEST_HWTABLE);
				uiRepCopy.updateColor();
			}
		}//end class Field.UIRepCopy.Listener

		public UIRepCopy(Field f, JComponent c, boolean editable)//PComponent c)
		{
			field = f;
			component = c;
			listener = new UIRepCopy.Listener(this);
			if (component instanceof TextFieldwLabel)
			{
				((TextFieldwLabel)component).addActionListener(listener); 
				((TextFieldwLabel)component).setEditable(editable);
			}
			if (component instanceof JTextField)
			{
				((JTextField)component).addActionListener(listener); 
				((JTextField)component).setEditable(editable);
			}
			if (component instanceof AbstractButton)
			{
				((AbstractButton)component).addActionListener(listener); 
				((JComponent)component).setEnabled(editable);
			}
			if (component instanceof ONL.ComponentwLabel)
			{
				JComponent jc = ((ONL.ComponentwLabel)component).getJComponent();
				if (jc instanceof JComboBox) ((JComboBox)jc).addActionListener(listener); 
				((JComponent)component).setEnabled(editable);
			}
			((JComponent)component).addFocusListener(listener);
			field.addListener(this);
		}

		public void valueChanged(Field.DataEvent e) //interface Field.Listener
		{
			//if (!(component.equals(e.getNewValue()))) 
			//{
			if (component instanceof TextFieldwLabel && !((TextFieldwLabel)component).getText().equals(e.getNewValue()))
				((TextFieldwLabel)component).setText(e.getNewValue()); 
			if (component instanceof JTextField && !((JTextField)component).getText().equals(e.getNewValue()))
				((JTextField)component).setText(e.getNewValue()); 
			if (component instanceof AbstractButton && !((AbstractButton)component).getText().equals(e.getNewValue()))
			{
				Boolean tmp_bool = new Boolean(e.getNewValue());
				((AbstractButton)component).setSelected(tmp_bool.booleanValue());
				//component.setValue(e.getNewValue());
			}
			if (component instanceof ONL.ComponentwLabel)
			{
				JComponent c = ((ONL.ComponentwLabel)component).getJComponent();
				if (c instanceof JComboBox && !((JComboBox)c).getSelectedItem().toString().equals(e.getNewValue())) 
				{
					ExpCoordinator.print(new String("Field(" + field.getLabel() + ").UIRepCopy.valueChanged:" + e.getNewValue()), TEST_CHOICE);
					JComboBox cb = (JComboBox)c;
					int max = cb.getItemCount();
					for (int i = 0; i < max; ++i)
					{
						Object o = cb.getItemAt(i);
						if (o.toString().equals(e.getNewValue()))
						{
							cb.setSelectedIndex(i);
							ExpCoordinator.print(new String("   setting selected index " + i), TEST_CHOICE);
							return;
						}
					}
					cb.setSelectedItem(e.getNewValue());
				}
			}
			//}
		}

		public void updateColor()
		{
			JComponent comp = (JComponent)component;
			if (field.needsCommit()) 
				comp.setForeground(Color.red);
			else
				comp.setForeground(Color.black);
			comp.revalidate();
		}
		public void clear() { field.removeListener(this);}

		//public Param.PComponent 
		public JComponent getComponent() { return component;}
	}

	/////////////////////////////////////// Field.FPanel ////////////////////////////////////////////////////////////
	public static class FPanel extends JPanel //Priority extends TextFieldwLabel
	{
		private Field field = null;
		private Field.UIRepCopy uiRep = null;
		private Field.FPanel.Listener listener = null;

		////////////////////////////////////////////////// Field.FPanel.Listener /////////////////////////////////////////////
		private class Listener implements ActionListener, FocusListener
		{
			public Listener(Experiment exp){}
			//FocusListener
			public void focusLost(FocusEvent e) { actionPerformed(null);}
			public void focusGained(FocusEvent e) { }
			//ActionListener
			public void actionPerformed(ActionEvent e)
			{
				//panel.field.setDefaultValue(panel.uiRep.getComponent().getValue());
				ExpCoordinator.print(new String("Field.FPanel.Listener.actionPerformed"), HWTable.TEST_HWTABLE);
				updateColor();
			}
		}//end class Field.FPanel.Listener

		///////////////////////////////////////////// Field.FPanel Methods /////////////////////////////////////////////////////
		public FPanel(Field f) { this(f, f.isEditable());}
		public FPanel(Field f, boolean edit)
		{
			super();
			//boolean edit = f.isEditable();
			field = f;
			uiRep = field.getUIRepCopy();
			JComponent tmp_comp = (JComponent)uiRep.getComponent();
			listener = new Listener(ExpCoordinator.theCoordinator.getCurrentExp());
			if (tmp_comp instanceof TextFieldwLabel)
			{
				((TextFieldwLabel)tmp_comp).addActionListener(listener); 
				((TextFieldwLabel)tmp_comp).setDisabledTextColor(Color.black);
			}
			if (tmp_comp instanceof JTextField)
			{
				((JTextField)tmp_comp).addActionListener(listener); 
				((JTextField)tmp_comp).setDisabledTextColor(Color.black);
			}
			if (tmp_comp instanceof AbstractButton)
				((AbstractButton)tmp_comp).addActionListener(listener); 
			((JComponent)tmp_comp).addFocusListener(listener);
			((JComponent)tmp_comp).setEnabled(edit);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add((JComponent)tmp_comp);
		}
		public void addEditForCommit()
		{
			listener.actionPerformed(null);
		}
		public void updateColor()
		{
			JComponent comp = (JComponent)uiRep.getComponent();
			if (field.needsCommit()) 
				comp.setForeground(Color.red);
			else
				comp.setForeground(Color.black);
			revalidate();
		}
		public Field getField() { return field;}
		public void clear() { field.removeListener(uiRep);}
	}//end class Field.FPanel


	///////////////////////////////////////////// Field.DataEvent ///////////////////////////////////////////////////////
	public static class DataEvent extends EventObject
	{
		private String oldValue = "";
		private String newValue = "";
		private Field field = null;

		public DataEvent(Object src, Field f, String ov, String nv)
		{
			super(src);
			field = f;
			newValue = nv;
			oldValue = ov;
		}
		public String getNewValue() { return newValue;}
		public String getOldValue() { return oldValue;}
		public Field getField() { return field;}
	}


	/////////////////////////////////////////////////////// Field.UpdateCommand ////////////////////////////////////////////////////
	private class UpdateCommand extends Command
	{
		public UpdateCommand(CommandSpec cspec, Field f)
		{
			super(cspec, f.getParent());
			//params.add(f);
			setParam(f, 0);
		}
		public void initParams(CommandSpec cspec, Vector defaults){}
	}

	////////////////////////////////////////////////////// Field.UpdateAction //////////////////////////////////////////////////////
	private class UpdateAction extends Command.CAction //implements Field.Listener
	{
		private Field field = null;
		//public UpdateAction(CommandSpec cspec, Field f) //CHANGED 8/20/10
		public UpdateAction(Command cmd, Field f)
		{
			super(f.getParent(), cmd);//new UpdateCommand(cspec, f));
			field = f;
			//field.addListener(this);
		}
		//public void valueChanged(Field.DataEvent e) 
		//{
		//if (field == e.getField() && e.getSource() != this)
		//{
		//  addOperation();
		//}
		//}
		public void actionPerformed()
		{
			super.actionPerformed();
			ExpCoordinator.print(new String("Field.UpdateAction(" + field.getLabel() + ").actionPerformed command:" + getCommand().toString()), TEST_UPDATE);
		}
		protected void addOperation() 
		{ 
			Command cmd = getCommand();
			ExpCoordinator.print(new String("Field(" + field.getLabel() + ").UpdateAction.addOperation cmd:" + cmd.toString()), TEST_UPDATE);
			if (!cmd.isImmediate())
				ExpCoordinator.theCoordinator.addEdit(new Command.Edit(cmd, cmd.getCurrentValues(), ExpCoordinator.theCoordinator.getCurrentExp()));
			else
				((NCCPOpManager.Manager)getONLComponent()).addOperation(cmd.createNCCPOp(cmd.getCurrentValues()));
			//((NCCPOpManager.Manager)getONLComponent()).addOperation(new Command.NCCPOp(getCommand(), getCommand().getParamValues(), getONLComponent()));
		}
		protected void addOperation(Command cspec) {} 
		protected void addOperation(Command cspec, Object[] vals) {}
	}


	////////////////////////////////////////////////////////// Field methods ///////////////////////////////////////////////////////
	public Field(Field f)
	{
		super((Param)f);
		listeners = new EventListenerList();
		id = f.id;
		owner = f.owner;
		/*REMOVED 8/20/10
		if (f.updateAction != null)
		{
			//CommandSpec cs = (CommandSpec)f.updateAction.getCommand();
			updateAction = new UpdateAction(f.updateAction.getCommand().getCopy(), this);//cs, this);
		}
		*/
		if (f.updateCommandSpec != null) updateCommandSpec = f.updateCommandSpec;//ADDED 8/20/10
		if (f.assignedDefault != null) assignedDefault = f.assignedDefault.getCopy();
	}
	public Field(FieldSpec fs, ONLComponent pc) { this(fs, pc, null);}
	public Field(FieldSpec fs, ONLComponent pc, Field.Owner o)
	{
		super((ParamSpec)fs, pc, null);
		listeners = new EventListenerList();
		id = fs.id;
		if (o == null && pc instanceof Field.Owner)
			owner = (Field.Owner)pc;
		else owner = o;
		if (fs.isAssigned())
		{
			assignedDefault = pc.getNewID(fs.getAssigner());
			ExpCoordinator.print(new String("Field(" + getLabel() + ") assigned " + assignedDefault.getValue()), TEST_ASSIGNMENT);
			setDefaultFromString(String.valueOf(assignedDefault.getValue()));
		}
		else ExpCoordinator.print(new String("Field(" + getLabel() + ") unassigned"), TEST_ASSIGNMENT);
		ExpCoordinator.print(new String("Field(" + getLabel() +") wildcard:" + wildcardValue), TEST_WILDCARD);
		updateCommandSpec = (FieldSpec.UpdateCommandSpec)fs.getUpdateCommand();//ADDED 8/20/10
		/*REMOVED 8/20/10
		if (fs.getUpdateCommand() != null) 
		{
			Command tmp_cmd = fs.getUpdateCommand().createCommand(pc, o);
			ExpCoordinator.print(new String("Field(" + getLabel() + ").Field(FieldSpec, ONLComponent) updateCommand:" + tmp_cmd.toString()), TEST_ACTION);
			updateAction = new UpdateAction(tmp_cmd, this);//fs.getUpdateCommand().createCommand(pc, o), this);
		}
		*/
	}
	public void setDefaultValue(Object o) //think i need to detect a change and issue update command 
	{
		if (o == null) return;
		if (getDefaultValue() != null && o != null && getDefaultValue().toString().equals(o.toString())) return;
		if (assignedDefault != null)
		{
			int val = Integer.parseInt(o.toString());
			if (val != assignedDefault.getValue()) assignedDefault.setValue(val);
		}
		DataEvent event = null;
		if (getDefaultValue() != null) event = new DataEvent(o, this, getDefaultValue().toString(), o.toString());
		else event = new DataEvent(o, this, null, o.toString());
		super.setDefaultValue(o);
		if (getUpdateAction() != null) ((UpdateAction)getUpdateAction()).addOperation();
		fireEvent(event);
	}
	public void addNextHopListener()
	{ 
		if (defaultValue instanceof NextHop) 
		{
			ExpCoordinator.print(new String("Field(" + toString() + ").addNextHopListener " + defaultValue), TEST_NHLISTENER);
			((NextHop)defaultValue).addListener();
		}
	}
	public boolean isEqual(ParamSpec p)
	{
		if (p instanceof Field)
		{
			Field tmp_field = (Field)p;
			return (super.isEqual((Param)tmp_field) && id == tmp_field.id);
		}
		return false;
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{  
		xmlWrtr.writeStartElement(ExperimentXML.FIELD);
		xmlWrtr.writeAttribute(ExperimentXML.FIELDNAME, getLabel());
		xmlWrtr.writeCharacters(defaultValue.toString());
		xmlWrtr.writeEndElement();
	}
	public Action getUpdateAction() 
	{
		if (updateCommandSpec != null && updateAction == null)
		{
			Command tmp_cmd = updateCommandSpec.createCommand(getParent(), getOwner());
			ExpCoordinator.print(new String("Field(" + getLabel() + ").getUpdateAction updateCommand:" + tmp_cmd.toString()), TEST_UPDATE);
			updateAction = new UpdateAction(tmp_cmd, this);//fs.getUpdateCommand().createCommand(pc, o), this);
		}
		return updateAction;
	}
	public Field.Owner getOwner() { return owner;}
	public Object getCurrentValue() 
	{
		ExpCoordinator.print(new String("Field(" + label + ").getCurrentValue"), TEST_CURRENTVAL);
		return (super.getCurrentValue());
	}
	private void fireEvent(DataEvent e)
	{
		if (listeners == null) return;
		int max = listeners.getListenerCount();
		int i = 0;
		Object[] list = listeners.getListenerList();
		//turns out this is a list of class, listener pairs so list looks like class1, listener1, class2, listener2 ... classn, listenern
		Field.Listener l;
		for (i = 1; i < list.length; i += 2)
		{
			l = (Field.Listener) (list[i]);
			l.valueChanged(e);
		}
	}
	public void addListener(Field.Listener l){ listeners.add(Field.Listener.class, l);}
	public void removeListener(Field.Listener l){ listeners.remove(Field.Listener.class, l);}
	public Field.UIRepCopy getUIRepCopy()
	{
		return (getUIRepCopy(isEditable()));
	}
	public Field.UIRepCopy getUIRepCopy(boolean e)
	{
		UIRepCopy rtn = new UIRepCopy(this, (JComponent)getNewUIRep(), e);
		addListener(rtn);
		return rtn;
	}
	public String getXMLElemName() { return (ExperimentXML.FIELD);}
	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new Field.FContentHandler(exp_xml, this));} 
	public void clear() 
	{
		if (defaultValue != null && defaultValue instanceof NextHop) ((NextHop)defaultValue).removeListener();
		if (assignedDefault != null) assignedDefault.clear();
	}
}
