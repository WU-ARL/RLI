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

import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.swing.*;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.border.EmptyBorder;

import java.awt.event.*;

public class Command extends CommandSpec
{
	public static final int TEST_COMMAND = 3;
	public static final int TEST_UIREP = 3;
	public static final int TEST_ACTION = 3;
	protected ONLComponent parentComponent = null;
	private Field.Owner fieldOwner = null;

	/////////////////////////////////////////////////////// Command.CContentHandler ///////////////////////////////////////////////////////

	protected static class CContentHandler extends DefaultHandler
	{
		private ExperimentXML expXML = null;
		protected String currentElement = "";
		protected ParamSpec currentParam = null;
		protected int paramIndex = 0;
		protected Command command = null;

		public CContentHandler(ExperimentXML exp_xml, Command cmd)
		{
			super();
			expXML = exp_xml;
			command = cmd;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			ExpCoordinator.print(new String("CommandSpec.XMLHandler.startElement localName:" + localName), ExperimentXML.TEST_XML);
			currentElement = new String(localName);
			if (localName.equals(ExperimentXML.PARAM)) 
			{
				Param param = (Param)command.getParam(attributes.getValue(ExperimentXML.PLABEL));
				if (param != null) expXML.setContentHandler(param.getContentHandler(expXML));
			}
		}
		public void endElement(String uri, String localName, String qName)
		{
			ExpCoordinator.print(new String("Command.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(command.getXMLElemName()))//ExperimentXML.COMMAND) || localName.equals(ExperimentXML.UPDATE_COMMAND))
			{
				command.print(ExperimentXML.TEST_XML);
				command = null;
				expXML.removeContentHandler(this);
			}
		}
	}// end inner class Command.XMLHandler


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////// Command.Edit ///////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class Edit extends Experiment.Edit
	{
		private Command.NCCPOp commandOp = null;
		private boolean cancelled = false;
		private boolean can_undo = false;

		public Edit(Command cmd, Experiment exp)
		{
			this(cmd, cmd.getCurrentValues(), exp);
		}

		public Edit(Command cmd, Object[] vals, Experiment exp)
		{
			super(cmd.parentComponent, exp);
			ExpCoordinator.print(new String("Command.Edit"), HWTable.TEST_HWTABLE);
			cmd.print(HWTable.TEST_HWTABLE);
			commandOp = cmd.createNCCPOp(vals);//new Command.NCCPOp(cmd, vals, cmd.parentComponent);
		}

		//ONLComponent.Undoable
		public boolean canUndo() 
		{ 
			if (can_undo) return (super.canUndo());
			else return false;
		}
		public boolean canRedo() 
		{ 
			if (can_undo) return (super.canRedo());
			else return false;
		}
		public void setCanUndo(boolean b) { can_undo = b;}
		protected void sendMessage() 
		{
			if (commandOp != null)
			{
				ExpCoordinator.printer.print(new String("Command.Edit.sendMessage"), HWTable.TEST_HWTABLE);
				((NCCPOpManager.Manager)onlComponent).addOperation(commandOp);
			}
		}
		protected void sendUndoMessage() {}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			return (cancelled);
		}
		public void setCancelled(boolean b) { cancelled = b;}
		public boolean getCancelled() { return cancelled;}
		protected void setCommandOp(Command.NCCPOp op) { commandOp = op;}
		protected Command.NCCPOp getCommandOp() { return commandOp;}
	}//end class Command.Edit

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////// Command.CAction ////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class CAction extends AbstractAction
	{
		private ONLComponent onlComponent = null;
		private Command command = null;
		private JFrame window = null;
		private Object[] prefix = null;
		private Object[] suffix = null;
		private String title = null;

		public CAction(ONLComponent c, Command cmd)
		{
			super(cmd.getLabel());
			command = cmd;
			onlComponent = c;
			window = onlComponent.getExpCoordinator().getMainWindow();
		}

		public ONLComponent getONLComponent() { return onlComponent;}
		public CommandSpec getSpec() { return ((CommandSpec)getCommand());}
		protected void setCommand(CommandSpec cs) { command = new Command(cs, onlComponent.getHardware());}
		protected void setCommand(Command cs) { command = cs;}
		protected Object[] getParamUIReps() { return (getCommand().getParamUIReps());}
		public void actionPerformed(ActionEvent e) { actionPerformed();}
		public void actionPerformed()
		{
			Command cspec = getCommand();
			String ttl = "";
			if (title != null) ttl = title;
			else ttl = new String(onlComponent.getLabel() + " Send Command " + cspec.getLabel());

			JDialog dialog = new JDialog( window, ttl, true);
			dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
			JToggleButton opt0 = new JToggleButton("Enter");
			JToggleButton opt1 = new JToggleButton("Cancel");
			class CloseAction implements ActionListener
			{
				private JDialog dlog = null;
				CloseAction(JDialog d){ dlog = d;}
				public void actionPerformed(ActionEvent e) { dlog.setVisible(false); dlog.dispose();}
			}
			CloseAction close_action = new CloseAction(dialog);
			opt0.addActionListener(close_action);
			opt1.addActionListener(close_action);
			ButtonGroup optGroup = new ButtonGroup();
			optGroup.add(opt0);
			optGroup.add(opt1);
			optGroup.setSelected(opt0.getModel(), false);
			JPanel options = new JPanel();
			//options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
			options.setLayout(new FlowLayout(FlowLayout.CENTER));
			options.setBorder(new EmptyBorder(1,0,1,0));
			options.add(opt0);
			options.add(opt1);
			
			Object[] params = cspec.getParamUIReps();
			int param_len = Array.getLength(params);
			int prefix_len = 0;
			int suffix_len = 0;
			if (prefix != null) prefix_len = Array.getLength(prefix);
			if (suffix != null) suffix_len = Array.getLength(suffix);
			int num_display = param_len + prefix_len + suffix_len + 1;
			int height = (num_display)*32 + 37;
		
			//JPanel display = new JPanel();
			//display.setLayout(new BoxLayout(display, BoxLayout.Y_AXIS));
			int i = 0;
			JComponent comp;
			comp = new JLabel(cspec.getDescription());
			comp.setAlignmentX(Component.LEFT_ALIGNMENT);
			//comp.setAlignmentX(1);
			comp.setBorder(new EmptyBorder(1,0,1,0));
			dialog.getContentPane().add(comp);
			for (i= 0; i < prefix_len; ++i) 
			{
				Object o = prefix[i];
				if (o instanceof JComponent) comp = (JComponent)o;
				else comp = new JLabel(o.toString());
				comp.setAlignmentX(Component.LEFT_ALIGNMENT);
				//comp.setAlignmentX(1);
				comp.setBorder(new EmptyBorder(1,0,1,0));
				dialog.add(comp);
			}
			for (i = 0; i < param_len; ++i)
			{
				Object o = params[i];
				if (o instanceof JComponent) comp = (JComponent)o;
				else comp = new JLabel(o.toString());
				comp.setAlignmentX(Component.LEFT_ALIGNMENT);
				//comp.setAlignmentX(1);
				comp.setBorder(new EmptyBorder(1,0,1,0));
				dialog.add(comp);
			}
			for (i = 0; i < suffix_len; ++i)
			{
				Object o = suffix[i];
				if (o instanceof JComponent) comp = (JComponent)o;
				else comp = new JLabel(o.toString());
				comp.setAlignmentX(Component.LEFT_ALIGNMENT);
				//comp.setAlignmentX(1);
				comp.setBorder(new EmptyBorder(1,0,1,0));
				dialog.add(comp);
			}

			//dialog.add(display);
		    options.setAlignmentX(Component.LEFT_ALIGNMENT);
			dialog.add(options);
			dialog.setSize(250, height);
			dialog.setVisible(true);
			
			if (opt0.isSelected())
			{
				//Object[] vals = cspec.getParamValues();
				Object[] vals = cspec.getCurrentValues();
				//CommandSpec.Param[] param_lst = cspec.getParams();
				ExpCoordinator.print(new String("Sending command " + cspec.getLabel() + ":"));
				int max = cspec.getNumParams();
				for (i = 0; i < max; ++i)
				{
					ExpCoordinator.print(new String("    " + cspec.getParam(i).label + ":" + vals[i].toString()));
				}
				addOperation(cspec, vals);
			}
		}
		//protected void addOperation() { addOperation(getCommand(), getCommand().getParamValues());}
		//protected void addOperation(Command cspec) { addOperation(cspec, cspec.getParamValues());}
		protected void addOperation() { addOperation(getCommand(), getCommand().getCurrentValues());}
		protected void addOperation(Command cspec) { addOperation(cspec, cspec.getCurrentValues());}
		protected void addOperation(Command cmd, Object[] vals) 
		{
			if (!cmd.isImmediate())
				ExpCoordinator.theCoordinator.addEdit(new Command.Edit(cmd, vals, ExpCoordinator.theCoordinator.getCurrentExp()));
			else
			{
				Command.NCCPOp nccp_op = cmd.createNCCPOp(vals);
				if (nccp_op != null) ((NCCPOpManager.Manager)onlComponent).addOperation(nccp_op);
			}
		}
		protected Command getCommand() { return command;}
		public void setPrefix(Object[] pr) { prefix = pr;}
		public void setSuffix(Object[] suf) { suffix = suf;}
		public void setTitle(String ttl) 
		{ 
			title = ttl;
			putValue(Action.NAME, ttl);
		}
	}

	/////////////////////////////////////////////////// Command.NCCPOp /////////////////////////////////////////////////////////////
	protected static class NCCPOp implements NCCPOpManager.Operation
	{
		private Hardware.MDataType mDataType = null;
		private Hardware.NCCP_Request request = null;
		private ONL.Log commandLog = null;

		public NCCPOp(Hardware.MDataType mdt)
		{
			ExpCoordinator.print("Command.NCCPOp(Hardware.MDataType)", TEST_COMMAND);
			mDataType = mdt;
			request = new Hardware.NCCP_Request(mDataType.getMonitorID(), mDataType.getValues(), mDataType.getPort(), mDataType.getVersion());
			((Hardware.NCCP_Request)request).setHardware(mDataType.getHardware());
			request.setMarker(new REND_Marker_class(mDataType.getPort(), mDataType.isIPP(), mDataType.getMonitorID(), mDataType.getIndex()));	
		}
		public NCCPOp(Command cs, ONLComponent oc) 
		{
			ExpCoordinator.print("Command.NCCPOp(Command cs, ONLComponent oc)", TEST_COMMAND);
			mDataType = new Hardware.MDataType(oc, cs);
			//mDataType.setONLComponent(oc);
			request = new Hardware.NCCP_Request(mDataType.getMonitorID(), mDataType.getValues(), mDataType.getPort(), mDataType.getVersion());
			((Hardware.NCCP_Request)request).setHardware(mDataType.getHardware());
			request.setMarker(new REND_Marker_class(mDataType.getPort(), mDataType.isIPP(), mDataType.getMonitorID(), mDataType.getIndex()));
		}

		public NCCPOp(Command cs, Object[] params, ONLComponent oc)//Instance in, Port p)
		{
			ExpCoordinator.print("Command.NCCPOp(Command cs, Object[] params, ONLComponent oc)", TEST_COMMAND);
			mDataType = new Hardware.MDataType(oc, cs, params);
			request = new Hardware.NCCP_Request(mDataType.getMonitorID(), mDataType.getValues(), mDataType.getPort(), mDataType.getVersion());
			((Hardware.NCCP_Request)request).setHardware(mDataType.getHardware());
			request.setMarker(new REND_Marker_class(mDataType.getPort(), mDataType.isIPP(), mDataType.getMonitorID(), mDataType.getIndex()));
		}

		//start interface NCCPOpManager.Operation
		public void opSucceeded(NCCP.ResponseBase resp)
		{
			ExpCoordinator.print("Hardware.CommandNCCPOp.OpSucceeded command :", 3);
			if (commandLog != null) commandLog.addLine(new String(mDataType.getCommand().getLabel() + "(" + getRequest().getValuesString() + ") -- succeeded.\n   Response:" + ((Hardware.NCCP_MonitorResponse)resp).getStatusMsg() + "\n"));
			mDataType.print(3);
			((NCCPOpManager.Manager)mDataType.getONLComponent()).removeOperation(this);
			mDataType.getCommand().setCommittedValues(getRequest().getValues());
		}
		public void opFailed(NCCP.ResponseBase resp)
		{
			if (commandLog != null) commandLog.addLine(new String(mDataType.getCommand().getLabel() + "(" + getRequest().getValuesString() + ") -- failed \n  Response:" + ((Hardware.NCCP_MonitorResponse)resp).getStatusMsg() + "\n"));
			ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("Hardware Command Failure for " + mDataType.getONLComponent().getLabel() + " command:" + mDataType.getName() + " msg:" + ((Hardware.NCCP_MonitorResponse)resp).getStatusMsg())),
					"Hardware Command Operation Failure.",
					"Hardware Command Operation Failure.",
					(StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR )));//| StatusDisplay.Error.POPUP)));
			ExpCoordinator.print("Hardware.CommandOp.OpFailed command :", 3);
			mDataType.print(3);
			((NCCPOpManager.Manager)mDataType.getONLComponent()).removeOperation(this);
		} 
		public void setCommandLog(ONL.Log clog) { commandLog = clog;}
		public Vector getRequests()
		{
			Vector rtn = new Vector();
			rtn.add(request);
			return rtn;
		}
		public boolean containsRequest(REND_Marker_class rend) { return (request.getMarker().isEqual(rend));}
		//end interface NCCPOpManager.Operation
		public Hardware.MDataType getMDataType() { return mDataType;}
		public CommandSpec getCommandSpec() { return ((CommandSpec)mDataType.getCommand());}
		public Command getCommand() { return (mDataType.getCommand());}
		//public Object[] getParamValues() { return (mDataType.getValues());} 
		protected Hardware.NCCP_Request getRequest() { return request;}
	}
	//////////////////////////////////// Command methods /////////////////////////////////////////////////////////////////////////
	public Command(Command c) { this((CommandSpec)c, null, c.getParentComponent(), c.getFieldOwner());}
	public Command(CommandSpec cspec, ONLComponent c) { this(cspec, null, c, null);}
	public Command(CommandSpec cspec, ONLComponent c, Field.Owner o) { this(cspec, null, c, o);}
	public Command(CommandSpec cspec, Vector defaults, ONLComponent c) { this(cspec, defaults, c, null);}
	public Command(CommandSpec cspec, Vector defaults, ONLComponent c, Field.Owner o)
	{
		super(cspec, defaults);
		parentComponent = c;
		if (o != null) fieldOwner = o;
		initParams(cspec, defaults);
	}
	public void initParams(CommandSpec cspec, Vector defaults)//Param[] defaults)
	{
		if (parentComponent == null) return;
		Param param;
		for (int i = 0; i < numParams; ++i)
		{
			param = cspec.getParam(i).createParam(parentComponent, this);
			setParam(param, i);
		}
		if (defaults != null) updateDefaults(defaults);
	}
	public void setParentComponent(ONLComponent c) 
	{ 
		if (parentComponent == c) return;
		parentComponent = c;
		int max = params.size();
		for (int i = 0; i < max; ++i)
		{
			((Param)params.elementAt(i)).setParent(parentComponent);
		}
	}
	public ONLComponent getParentComponent() { return parentComponent;}
	public Object[] getParamUIReps()
	{
		Param elem;
		ExpCoordinator.print(new String("Command(" + getLabel() + ").getParamUIReps"), TEST_UIREP);
		int max = displayGroups.size();
		Object rtn[] = new Object[max];//numParams];
		for (int i = 0; i < max; ++i)
		{
			Object o = displayGroups.elementAt(i);
			if (o instanceof DisplayGroup)
			{
				JPanel p = new JPanel();
				p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
				DisplayGroup dg = (DisplayGroup)o;
				int max_j = dg.size();
				if (dg.label != null) p.add(new JLabel(dg.label));
				for (int j = 0; j < max_j; ++j)
				{
					elem = (Param)getParam(((Integer)dg.elementAt(j)).intValue());
					p.add((JComponent)elem.getUIRep());
				}
				rtn[i] = p;
			}
			if (o instanceof Integer)
			{
				elem = (Param)getParam(((Integer)o).intValue());
				rtn[i] = elem.getUIRep();
			}
		}
		/*
    for (int i = 0; i < numParams; ++ i)
      {
	elem = (Param)getParam(i);
	rtn[i] = elem.getUIRep();
	ExpCoordinator.print(new String("    " + rtn[i]), TEST_UIREP);	
      }
		 */
		return rtn;
	}
	public Object[] getCurrentValues()
	{
		Object rtn[] = new Object[numParams];
		Param elem;
		for (int i = 0; i < numParams; ++ i)
		{
			elem = (Param)getParam(i);
			elem.updateFromUIRep();
			rtn[i] = elem.getCurrentValue();
		}
		return rtn;
	}
	/*
  public void setCurrentFromUI()
  {
    for (int i = 0; i < numParams; ++i)
      ((Param)getParam(i)).setCurrentFromUI();
      }*/
	public void setCurrentValues(Object[] vals) { setDefaults(vals);}
	public void setCommittedValues()
	{
		Param elem;
		for (int i = 0; i < numParams; ++ i)
		{
			elem = (Param)getParam(i);//paramArray[i];
			elem.setCommittedValue();
		}
	}
	public void setCommittedValues(Object[] vals)
	{
		Param elem;
		for (int i = 0; i < numParams; ++ i)
		{
			elem = (Param)getParam(i);//paramArray[i];
			elem.setCommittedValue(vals[i]);
		}
	}
	public boolean needsCommit()
	{
		for (int i = 0; i < numParams; ++ i)
		{
			if (((Param)getParam(i)).needsCommit()) return true;
		}
		return false;
	}
	public boolean isEqual(Command c)
	{
		boolean rtn = isEqual((CommandSpec)c);
		return (rtn && parentComponent == c.parentComponent);
	}
	public Field.Owner getFieldOwner() { return fieldOwner;}
	public Command getCopy() { return (getCopy(null, null));}
	public Command getCopy(ONLComponent parent) { return (getCopy(parent, null));}
	public Command getCopy(Object[] defaults) { return (getCopy(null, defaults));}
	public Command getCopy(ONLComponent parent, Object[] defaults)
	{
		Command rtn = new Command(this);
		if (parent != null) rtn.setParentComponent(parent);
		if (defaults != null) rtn.setDefaults(defaults);
		ExpCoordinator.print("Command.getCopy", TEST_COMMAND);
		rtn.print(TEST_COMMAND);
		return rtn;
	}  

	public Command.NCCPOp createNCCPOp(Object[] vals) 
	{ 
		if (opcode >= 0) return (new Command.NCCPOp(this, vals, parentComponent));
		return null;
	}

	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException { writeXML(xmlWrtr, getXMLElemName());}
	public void writeXML(XMLStreamWriter xmlWrtr, String elemnm) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(elemnm);
		xmlWrtr.writeAttribute(ExperimentXML.DLABEL, getLabel());
		xmlWrtr.writeAttribute(ExperimentXML.OPCODE, String.valueOf(opcode));
		xmlWrtr.writeAttribute(ExperimentXML.NUMPARAMS, String.valueOf(getNumParams()));
		writeXMLHeader(xmlWrtr);
		int max = getNumParams();
		for (int i = 0; i < max; ++i)
		{
			((Param)getParam(i)).writeXML(xmlWrtr);
		}
		writeXMLFooter(xmlWrtr);
		xmlWrtr.writeEndElement(); //end elemnm
	} 
	public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException{}
	public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException{}
	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new Command.CContentHandler(exp_xml, this));}
	protected boolean merge(Command cmd)
	{
		if (cmd == null)
			ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String(getLabel() + " Inconsistency master:" + toString() + " other:none\n"),
					"Command Param Error",	
					"Command Param Error",	
					StatusDisplay.Error.LOG)); 
		int max = getNumParams();
		for (int i = 0; i < max; ++i)
		{
			Param p1 = (Param)getParam(i);
			Param p2 = (Param)cmd.getParam(i);
			if (p2 != null && !p1.isEqual(p2))
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(new String(getLabel() + " " + p1.getLabel() + " Inconsistency master has " + p1.getLabel() + ":" + p1.getCurrentValue().toString() + " other has " + p1.getLabel() + ":" + p2.getCurrentValue().toString() + "\n"),
						"Command Param Error",
						"Command Param Error",
						StatusDisplay.Error.LOG)); 
		}
		return true;
	}
}
