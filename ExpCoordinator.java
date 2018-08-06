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
 * File: ExpCoordinator.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/23/2003
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import javax.swing.undo.*;
import javax.xml.stream.XMLStreamException;

import java.awt.event.*;
import java.lang.String;
import java.util.*;
import java.awt.*;
import java.lang.reflect.Array;
import java.beans.PropertyChangeListener;
import java.io.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;

public class ExpCoordinator //implements Mode.MListener
{
	public static final double VERSION = 8.6;
	public static final int VERSION_BYTES = 0x8600; //& with ops to add version
	public static PrintMessage printer = null;
	public static ExpCoordinator theCoordinator = null;
	public static final int NEW = 0;
	public static final int LOADFILE = 1;
	public static final int CLOSE = 2;
	public static final int COMMIT = 3;
	public static final int SAVEFILE = 4;
	public static final int SAVEAS = 5;
	public static final int ADDFILE = 6;
	public static final int EXIT = 7;
	public static final int RESERVATION = 8;
	public static final int EXT_RESERVATION = 9;
	public static final int CANCEL_RESERVATION = 10;
	public static final int SESSION_RECORDING = 17;
	//public static final int FILEMENU = 7;
	//private ButtonGroup modes = null;
	private final int TOPOLOGY = 0;
	private final int MONITORING = 1;

	public static final int CUT = 8;
	public static final int COPY = 9;
	public static final int PASTE = 10;
	public static final int DELETE = 11;
	public static final int CLEAR = 12;
	public static final int SELECTALL = 13;
	public static final int EDITMENU = 14;
	public static final int STATUS = 16;
	
	public static final int TEST_ADD = 10;

	private Proxy proxy = null;
	private ExpDaemon expDaemon = null;
	private Experiment currentExperiment = null;
	private ONLMainWindow mainWindow = null;
	private StatusDisplay statusDisplay = null;
	private MonitorManager monitorManager = null;
	private Topology topology = null;
	private SessionRecorder sessionRecorder = null;
	//private Mode mode = null;
	private Vector fileActions = null;
	private Vector editActions = null;
	private Vector extraActions = null;
	//private JRadioButtonMenuItem modeActions[] = null;
	private Vector topologyActions = null;
	private VirtualTopology.VAction virtualAction = null;

	private boolean management = false;
	private boolean directConn = false;

	private Vector currentSelection = null;
	private ONLUndoManager onlUndoManager = null;
	private ONLUndoManager.AggregateEdit compoundEdit = null;
	protected static int coord_port = ONLDaemon.COORDINATOR_PORT;
	private ONLPropertyList properties = null;

	private static boolean oldSubnet = false;
	
	private int expCount = 0;
	private static int debug = 0;
	private static TestCodes testCodes = null;
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String ADVANCED = "advanced";
	public static final String OBSERVE = "observe";
	public static final String RECORDING = "recording";
	public static final String VIEW_SNAPSHOT = "viewing_snapshot";
	public static final String TOPO_SET = "set_topo";
	public static final String DEFAULT_DIR = "default_dir";
	public static final String DEFAULT_POLLING = "default_polling";
	public static final String SPPMON = "sppmon";

	private static final String PROP_FILE = ".onlprops";

	//test codes
	public static final int NCCP_OFF = 0x00000001;
	public static final int DQ = 0x00000002;
	public static final int EXTRAS = 0x00000004;
	public static final int LOG = 0x00000008;
	public static final int CLUSTER = 0x00000010;
	public static final int REMAP = 0x00000020;
	//public static final int CMPTEST = 0x00000040;
	//public static final int OTABLE = 64;
	public static final int HTOH_LNK = 0x00000080;
	public static final int GIGETEST = 0x00000100;
	public static final int ONLCDTEST = 0x00000200;
	//public static final int NPRTEST =  0x00000400;
	public static final int PROXYTEST =  0x00000800;
	//public static final int OBSTEST =  0x00001000;
	public static final int MANAGER =  0x00002000;
	public static final int ASK_FOR_USER =  0x00004000;


	//.onldir directory in home directory
	private java.io.File onldir = null;

	//screen bounds
	private Rectangle screenBounds = null;

	private class TestRun extends Thread
	{
		private java.io.File file;
		private int iterations = 1;
		private int count = 0;
		public TestRun(java.io.File f, int i)
		{
			super();
			file = f;
			iterations = i;
		}
		public void run()
		{
			while(count < iterations)
			{
				try
				{
					setCurrentExp(file);
					onlUndoManager.commit();
					sleep(5000);
					Thread.yield();
					++count;
				}
				catch (InterruptedException e) 
				{
					//do nothing
				} 
			}
		}
	}


	public static class TestCodes
	{
		private int value = 0;
		public TestCodes(){}
		public void add(int t) { value = value | t;}
		public boolean isLevel(int t) { return ((value & t) > 0);}
	}

	protected static class PrintMessage
	{
		private int max_history = 0;
		private Vector history = null;
		private int current_ptr = -1;
		private ExpCoordinator expCoordinator = null;
		public PrintMessage(ExpCoordinator ec) { this(ec, 0);}
		public PrintMessage(ExpCoordinator ec, int maxh)
		{ 
			expCoordinator = ec;
			max_history = maxh;
			if (max_history > 0) history = new Vector();
		}
		public void print(String s) { print(s, 0);}
		public void print(String s, int level) 
		{ 
			if (expCoordinator.debug >= level)
			{
				if (history == null)
					System.out.println(s);
				else 
				{
					++current_ptr;
					if (current_ptr >= max_history) current_ptr = 0;
					if (history.size() == max_history) history.removeElementAt(current_ptr);
					history.add(current_ptr, s);
				}
			}
		}
		public void printHistory()
		{
			if (history != null)
			{
				int max = history.size();
				String s;
				int j = current_ptr + 1;
				if (j >= max || max < max_history) j = 0;
				for (int i = 0; i < max; ++i)
				{
					s = (String)history.elementAt(j);
					System.out.println(s);
					++j;
					if (j >= max) j = 0;
				}
				history.removeAllElements();
				current_ptr = -1;
			}
		}
	}
	private class AddVGigEAction extends Topology.TopologyAction
	{
		public AddVGigEAction(ExpCoordinator ec)
		{
			super("Add GigE Switch");
			setEnabled(true);
		}
		public void actionPerformed(ActionEvent e)
		{
			//super.actionPerformed(e);
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			TextFieldwLabel num = new TextFieldwLabel(new JTextField("1"), "How many instances?");
			TextFieldwLabel tf = new TextFieldwLabel(10, "number of ports:");
			String[] tmp_strarray = {ExperimentXML.ITYPE_1G, ExperimentXML.ITYPE_10G};
			JComboBox cb = new JComboBox(tmp_strarray);
			cb.setEditable(false);
			Object[] objs = {num};//, (new ONL.ComponentwLabel(cb, "interface type:"))};
			int rtn = JOptionPane.showOptionDialog(mainWindow, 
					objs, 
					"Add GigE Switch", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				int num_inst = Integer.parseInt(num.getText());
				for (int i = 0; i < num_inst; ++i)
				{
					GigEDescriptor g = topology.getNewVGigE();//Integer.parseInt(tf.getText()));
					if (g != null) 
					{
						//g.setInterfaceType((String)cb.getSelectedItem());
						getCurrentExp().addNode(g);
					}
				}
			}
		}
	}
	//sets experiment password atarts as a generated password but can be overwritten by user
	private class ExpPasswordAction extends ONL.UserAction 
	{
		public ExpPasswordAction()
		{
			super("Change VM Password");
		}
		public void actionPerformed(ActionEvent e)
		{
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			String warning = "Warning: password is sent unencrypted";
			Object[] options = {opt0,opt1};
			TextFieldwLabel tf = new TextFieldwLabel(20, "password:");
			tf.setText(currentExperiment.getGeneratedPW());
			JCheckBox gen_checkbox = new JCheckBox("use generated password:", false);
			Object[] objs = {tf, gen_checkbox, warning};
			
			int rtn = JOptionPane.showOptionDialog(mainWindow, 
					objs, 
					"Change VM password", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				if (currentExperiment != null) 
				{
					if (gen_checkbox.isSelected())
						currentExperiment.setGeneratedPW(null);
					else
						currentExperiment.setGeneratedPW(tf.getText());
				}
			}
		}
	}

	private class ExitAction extends ONL.UserAction //Mode.BaseAction
	{
		private ExpCoordinator expCoordinator = null;
		public ExitAction(ExpCoordinator ec)
		{
			super(true, false);
			expCoordinator = ec;
		}

		public void actionPerformed(ActionEvent e)
		{
			monitorManager.stopMonitoring();
			if (currentExperiment != null) currentExperiment.close();
			if (isConnected())
			{
				int rtn = JOptionPane.showConfirmDialog(mainWindow, 
						new String("Do you wish to cancel your current reservation?"),
						new String("Cancel Reservation"),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (rtn == JOptionPane.YES_OPTION)
				{
					Reservation reservation = new Reservation(expCoordinator);
					reservation.cancelReservation();
				}
			}
			printer.printHistory();
			try{
				Thread.sleep(1000);
			}
			catch (java.lang.InterruptedException e2) 
			{}
			expCoordinator.saveDefaults();
			System.exit(0);
		}
	}
	private class TestAction extends MenuFileAction implements MenuFileAction.Saveable
	{
		public TestAction()
		{
			super(null, true, null, "TestAction", false, false);
			setSuffix(Experiment.FILE_SUFFIX);
			setSaveable(this);
			setEnabled(true);
		}
		public void saveToFile(java.io.File f){}
		public void loadFromFile(java.io.File f)
		{     
			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			TextFieldwLabel tf = new TextFieldwLabel(10, "iterations:");
			Object[] objs = {tf};
			int rtn = JOptionPane.showOptionDialog(mainWindow, 
					objs, 
					"Iterations", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				TestRun tr = new TestRun(f, Integer.parseInt(tf.getText()));
				tr.start();
			}
		}
	}
	private class ExpFileAction extends MenuFileAction implements MenuFileAction.Saveable
	{
		private ExpCoordinator expCoordinator = null;
		private boolean append = false;
		private boolean save = false;

		public ExpFileAction(ExpCoordinator ec, boolean ld)
		{
			super(null, ld, null, "ExpFileAction", false, false);
			expCoordinator = ec;
			setSaveable(this);
			setSuffix(Experiment.FILE_SUFFIX);
		}

		public void actionPerformed(ActionEvent e)
		{
			print(new String("ExpFileAction::actionPerformed save = " + save), 2);
			Experiment exp = expCoordinator.getCurrentExp();
			if (exp != null)
			{
				if (save && exp.getFile() != null) 
				{
					print(new String("ExpCoordinator.ExpFileAction.actionPerformed calling saveToFile save = " + save), 2);
					try
					{
						exp.writeXML(new BufferedWriter(new FileWriter(exp.getFile())));
						//exp.saveToFile();
					}
					catch (java.io.IOException ioe)
					{
						print(ioe.getMessage());
					}
				}
				else
				{
					print(new String("ExpFileAction::actionPerformed calling super.actionPerformed save = " + save), 2);
					super.actionPerformed(e);
				}
			}
		}
		public void saveToFile(java.io.File f)
		{
			print("ExpFileAction::saveToFile", 2);
			Experiment exp = expCoordinator.getCurrentExp();
			exp.getTopology().print(2);
			if (exp != null) 
			{
				try
				{
					exp.setFile(f);
					exp.writeXML(new BufferedWriter(new FileWriter(f)));
				}
				catch(IOException e)
				{
					print(new String("ExpFileAction.saveToFile IOException " + e.getMessage()));
				}
			}
			//if (exp != null) exp.saveToFile(f);
		}
		public void loadFromFile(java.io.File f)
		{
			//if (expCoordinator.isMode(Mode.MONITORING))
			//  expCoordinator.getMonitorManager().loadFromFile(f);
			//else
			//  {
			if (!append) expCoordinator.setCurrentExp(f);
			//else expCoordinator.getCurrentExp().loadFromFile(f, false);
			// }
		}
		public void setAppend(boolean b) { append = b;}
		public void setSave(boolean b) { save = b;}
	}

	
	private class BatchFileAction extends MenuFileAction implements MenuFileAction.Saveable
	{
		private ExpCoordinator expCoordinator = null;
		private boolean append = false;
		private boolean save = false;
		private boolean on = false;
		private JLabel batchLabel = null;

		public BatchFileAction(ExpCoordinator ec, boolean ld)
		{
			this(ec, ld, "Start Batching");
		}
		public BatchFileAction(ExpCoordinator ec, boolean ld, String nm)
		{
			super(null, ld, null, nm, false, false);
			expCoordinator = ec;
			setSaveable(this);
			setSuffix(Experiment.FILE_SUFFIX);
			if (!ld) 
				{
					batchLabel = new JLabel("batching");
					batchLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
					batchLabel.setForeground(Color.blue);
				}
		}

		public void actionPerformed(ActionEvent e)
		{
			print(new String("BatchFileAction::actionPerformed save = " + save), 2);
			
			if (!on || isLoading())
				super.actionPerformed(e);
			else
				saveToFile(null);
		}
		public void setOn(boolean b)
		{
			if (b != on)
			{
				ONLMainWindow mw = expCoordinator.getMainWindow();
				if (b) 
					{
						putValue(Action.NAME, "Stop Batching");
						mw.getJMenuBar().add(batchLabel);
						mw.getJMenuBar().validate();
					}
				else 
					{
						putValue(Action.NAME, "Start Batching");
						mw.getJMenuBar().remove(batchLabel);
						//mw.getJMenuBar().invalidate();
						mw.getJMenuBar().revalidate();
						mw.getJMenuBar().validate();
					}
				on = b;
				expCoordinator.setBatch(on);
			}
		}
		public void saveToFile(java.io.File f)
		{
			print("BatchFileAction::saveToFile", 2);
			Experiment exp = expCoordinator.getCurrentExp();
			if (exp != null) 
			{
				try{
					if (!on)
						exp.setBatchWriter(new BufferedWriter(new FileWriter(f)));	
					else 
						exp.setBatchWriter(null);
					setOn(!on);
				}
				catch(IOException e)
				{
					print(new String("BatchFileAction.saveToFile IOException " + e.getMessage()));
				}
			}
		}
		public void loadFromFile(java.io.File f)
		{
			print("BatchFileAction::loadFromFile", 2);
			Experiment exp = expCoordinator.getCurrentExp();
			if (exp != null) 
			{
				try{
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					ExperimentXML exp_xml = new ExperimentXML(exp, xmlReader, f, true);
					xmlReader.parse(new InputSource(new FileInputStream(f)));
				}
				catch(IOException e)
				{
					print(new String("BatchFileAction.loadFromFile IOException " + e.getMessage()));
				}
				catch(SAXException e2)
				{
					print(new String("BatchFileAction.loadFromFile SAXException " + e2.getMessage()));
				}
			}
		}
		public void setAppend(boolean b) { append = b;}
		public void setSave(boolean b) { save = b;}
	}
	
	
	private class ExpXMLFileAction extends MenuFileAction implements MenuFileAction.Saveable
	{
		private ExpCoordinator expCoordinator = null;
		private boolean append = false;
		private boolean save = false;

		public ExpXMLFileAction(ExpCoordinator ec, boolean ld)
		{
			this(ec, ld, "ExpXMLFileAction");
		}
		public ExpXMLFileAction(ExpCoordinator ec, boolean ld, String nm)
		{
			super(null, ld, null, nm, false, false);
			expCoordinator = ec;
			setSaveable(this);
			setSuffix(Experiment.FILE_SUFFIX);
		}

		public void actionPerformed(ActionEvent e)
		{
			print(new String("ExpXMLFileAction::actionPerformed save = " + save), 2);
			/*
	Experiment exp = expCoordinator.getCurrentExp();
        if (exp != null)
          {
            if (save && exp.getFile() != null) 
              {
                print(new String("ExpXMLFileAction::actionPerformed calling saveToFile save = " + save), 2);
                exp.saveToFile();
              }
            else
              {
	      print(new String("ExpXMLFileAction::actionPerformed calling super.actionPerformed save = " + save), 2);*/
			super.actionPerformed(e);
			/*
              }
          }
			 */
		}
		public void saveToFile(java.io.File f)
		{
			print("ExpXMLFileAction::saveToFile", 2);
			Experiment exp = expCoordinator.getCurrentExp();
			if (exp != null) 
			{
				try{
					exp.writeXML(new BufferedWriter(new FileWriter(f)));
				}
				catch(IOException e)
				{
					print(new String("ExpXMLFileAction.saveToFile IOException " + e.getMessage()));
				}
			}
		}
		public void loadFromFile(java.io.File f)
		{
			print("ExpXMLFileAction::loadFromFile", 2);
			expCoordinator.closeCurrentExp();
			Experiment exp = expCoordinator.getCurrentExp();
			if (exp != null) 
			{
				try{
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					ExperimentXML exp_xml = new ExperimentXML(exp, xmlReader, f);
					xmlReader.parse(new InputSource(new FileInputStream(f)));
				}
				catch(IOException e)
				{
					print(new String("ExpXMLFileAction.loadFromFile IOException " + e.getMessage()));
				}
				catch(SAXException e2)
				{
					print(new String("ExpXMLFileAction.loadFromFile SAXException " + e2.getMessage()));
				}
			}
		}
		public void setAppend(boolean b) { append = b;}
		public void setSave(boolean b) { save = b;}
	}
	protected static interface MenuAction
	{
		public int getType();
		public boolean isType(int t);
	}
	protected static class ECMenuAction extends ONL.CompositeAction implements MenuAction //extends Mode.CompositeAction implements MenuAction
	{
		protected ExpCoordinator expCoordinator = null;
		private int type = 0;

		public ECMenuAction(ExpCoordinator ec, int tp, String lbl) { this(ec, tp, lbl, false);}
		public ECMenuAction(ExpCoordinator ec, int tp, String lbl, boolean ign)
		{
			super(lbl, ign, false);
			type = tp;
			expCoordinator = ec;
			setEnabled(false);
		}
		public int getType() { return type;}
		public boolean isType(int t) { return (t == type);}
	}

	private class AdvancedMenuItem extends JMenuItem
	{
		protected static final String advanced = "View Advanced Interface";
		protected static final String basic = "View Basic Interface";

		private class AMIAction extends ONL.UserAction
		{
			public AMIAction() { super(advanced);}
			public void actionPerformed(ActionEvent e)
			{
				if (getText().equals(advanced))
				{
					properties.setProperty(ADVANCED, true);
					setText(basic);
					mainWindow.addMenu(extraActions, "Extras");
				}
				else
				{
					properties.setProperty(ADVANCED, false);
					setText(advanced);
					mainWindow.removeMenu("Extras");
				}
			}
		}

		public AdvancedMenuItem()
		{
			super(advanced);
			setAction(new AMIAction());
			if (properties.getPropertyBool(ADVANCED))
				setText(basic);
		}
	}
	protected static class DeleteAction extends ONL.UserAction //Mode.BaseAction
	{
		protected ExpCoordinator expCoordinator = null;
		public DeleteAction(ExpCoordinator ec)
		{
			super(false, false);
			expCoordinator = ec;
		}
		public void actionPerformed(ActionEvent e)
		{
			if (expCoordinator.currentSelection.size() > 0)
			{
				ONLComponent oc = null;
				ONLComponent oc_rm = null;
				Vector seen = new Vector();
				int max = expCoordinator.currentSelection.size();
				ONLUndoManager.AggregateEdit delEdit = new ONLUndoManager.AggregateEdit();
				for (int i = 0; i < max; ++i)
				{
					oc = (ONLComponent)expCoordinator.currentSelection.elementAt(i);
					if (!seen.contains(oc))
					{
						seen.add(oc);
						if (!(oc instanceof LinkDescriptor) || !((LinkDescriptor)oc).isFixed())
						{
							Vector rms = expCoordinator.topology.getAttachedComponents(oc);
							ONLComponent.Undoable edit = oc.getDeleteEdit();
							if (edit != null) delEdit.addEdit(edit);
							int max_rms = rms.size();
							Object elem;
							for (int j = 0; j < max_rms; ++j)
							{
								elem = rms.elementAt(j);
								if (!seen.contains(elem))
								{
									seen.add(elem);
									if (elem instanceof ONLComponent)
										edit = ((ONLComponent)elem).getDeleteEdit();
									if (elem instanceof Cluster.Instance)
										edit = ((Cluster.Instance)elem).getDeleteEdit();
									if (edit != null) delEdit.addEdit(edit);
								}
							}
						}
					}
				}
				if (!delEdit.isEmpty()) 
				{
					delEdit.actionPerformed(new ActionEvent(this, DELETE, "Delete"));
					expCoordinator.addEdit(delEdit);
					expCoordinator.setCurrentSelection(null);
				}
				else
				{
					ExpCoordinator.printer.print("DeleteAction edit null");
				}
			}
			else ExpCoordinator.printer.print("DeleteAction selection null");
		}
	}

	//inner class window adapter to look for window closing
	private class MainWindowAdapter extends WindowAdapter
	{
		private ExpCoordinator expCoordinator = null;
		public MainWindowAdapter(ExpCoordinator ec)
		{
			expCoordinator = ec;
		}
		public void windowClosing (WindowEvent e)
		{
			expCoordinator.closeCurrentExp();
			if (!isSPPMon() && isConnected())
			{
				int rtn = JOptionPane.showConfirmDialog(mainWindow, 
						new String("Do you wish to cancel your current reservation?"),
						new String("Cancel Reservation"),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (rtn == JOptionPane.YES_OPTION)
				{
					Reservation reservation = new Reservation(expCoordinator);
					reservation.cancelReservation();
				}
			}
			expCoordinator.saveDefaults();
			System.exit(0);
		}
	}

	//private LinkList links = null;
	//probably want window listeners for listening for close
	public ExpCoordinator(String args[]) { this(args, false, 0);}
	public ExpCoordinator(String args[], int test_codes) { this(args, false, test_codes);}
	public ExpCoordinator(String args[], boolean dc, int test_codes)
	{
		directConn = dc; 
		int max = args.length;
		int i = 0;
		testCodes = new TestCodes();
		String onlhost = "onlsrv";
		String title = null;
		int print_history = 0;
		properties = new ONLPropertyList(this);
		properties.setProperty(SPPMON, false);
		properties.setProperty(ADVANCED, false);
		System.setProperty("user.language", "en");
		System.setProperty("user.country", "US");
		System.setProperty("user.variant", "US");
		for (i = 0; i < max; ++i)
		{
			if (args[i].equals("-onlcd")) 
			{
				coord_port = Integer.parseInt(args[i+1]);
			}
			if (args[i].equals("-onlhost"))
			{
				onlhost = args[i+1];
				++i;
			}
			if (args[i].equals("-multi_user"))
			{
			    testCodes.add(ASK_FOR_USER);
			}
			if (args[i].equals("-testONLCD"))
			{
				testCodes.add(ONLCDTEST);
			}
			if (args[i].equals("-debug")) 
			{
				debug = Integer.parseInt(args[i+1]);
			}
			if (args[i].equals("-nccpOff")) 
			{
				testCodes.add(NCCP_OFF);
			}          
			if (args[i].equals("-remap")) 
			{
				testCodes.add(REMAP);
			}
			if (args[i].equals("-gige")) 
			{
				testCodes.add(GIGETEST);
			}
			if (args[i].equals("-logfile")) 
			{
				testCodes.add(LOG);
			}
			if (args[i].equals("-extras")) 
			{
				testCodes.add(EXTRAS);
			}
			if (args[i].equals("-proxytest"))
			{
				testCodes.add(PROXYTEST);
			}
			if (args[i].startsWith("-man"))
			{
				testCodes.add(MANAGER);
			}
			if (args[i].startsWith("-record"))
			{
				sessionRecorder = new SessionRecorder();
			}
			if (args[i].startsWith("-printh"))
			{
				print_history = Integer.parseInt(args[++i]);
			}
			if (args[i].startsWith("-sppmon"))
			{
				properties.setProperty(SPPMON, true);
			}
			if (args[i].startsWith("-title"))
			{
			    title = args[++i];
			}
		}

		testCodes.add(DQ);
		//testCodes.add(RT_STATS);
		testCodes.add(test_codes);
		if (sessionRecorder == null) sessionRecorder = new SessionRecorder();
		if (isTestLevel(MANAGER))
		{
			testCodes.add(CLUSTER);
			testCodes.add(LOG);
		}
		theCoordinator = this;
		properties.setProperty(OBSERVE,false);
		properties.setProperty(RECORDING,false);

		//read properties from file     
		boolean sppmon = properties.getPropertyBool(SPPMON);
		try
		{
			java.io.File prop_file = new java.io.File(getONLDir(), PROP_FILE);
			if (prop_file.exists())
			{ 
				BufferedReader frdr = new BufferedReader(new FileReader(prop_file));
				String snm;
				String sval;
				while (frdr.ready())
				{
					snm = frdr.readLine();
					sval = frdr.readLine();
					properties.setProperty(snm, sval);
				}
				frdr.close();
			}
		}
		catch(java.io.IOException e){}

		printer = new PrintMessage(this, print_history);
		currentSelection = new Vector();

		topology = new Topology(this);
		//mode = new Mode(Mode.TOPOLOGY);
		onlUndoManager = new ONLUndoManager(this);
		if (!sppmon) 
		{
			proxy = new Proxy(this);
			expDaemon = new ExpDaemon(new String(onlhost), coord_port, ONLDaemon.ONLCD); //hard coded for now during test
			if (!directConn) expDaemon.setProxy(proxy);
			else expDaemon.setNonProxy();
			expDaemon.addConnectionListener(onlUndoManager);
		}
		initializeFileMenu();
		initializeEditMenu();
		if (!sppmon) initializeExtraMenu();

		if (sppmon)
			mainWindow = new ONLMainWindow(this, new String("SPPmon v." + ExpCoordinator.VERSION));
		else
		{
		    if (title != null)
			mainWindow = new ONLMainWindow(this, new String(title + " (RLI) v." + ExpCoordinator.VERSION));
		    else
			mainWindow = new ONLMainWindow(this, new String("Remote Laboratory Interface (RLI) v." + ExpCoordinator.VERSION));
		}
	
		mainWindow.setSize(550, 300);
		mainWindow.addWindowListener(new MainWindowAdapter(this));

		monitorManager = new MonitorManager(args, 550, 300, this);
		mainWindow.addMenu(monitorManager.getMenu());

		initializeTopologyActions();
		JMenu tmp_menu = mainWindow.addMenu(topologyActions, "Topology");
		HardwareSpec.ManagerListener mlistener = new HardwareSpec.ManagerListener(tmp_menu);
		if (!sppmon) mainWindow.getJMenuBar().add(new JButton(topology.getLinkToolAction()));

		statusDisplay = new StatusDisplay(this);
		onlUndoManager.getCommitAction().addPropertyChangeListener(statusDisplay);

		if (!sppmon && isTestLevel(EXTRAS)) mainWindow.addMenu(extraActions, "Extras");


		mainWindow.setVisible(true);


		DisplayMode dmode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
		if (dmode != null) screenBounds = new Rectangle(dmode.getWidth(), dmode.getHeight());

		print(new String("Version:" + VERSION), 1);
		HardwareSpec.Manager hwmanager = HardwareSpec.getTheManager();

		if (!sppmon)
		{
			hwmanager.addFromResource("IXP", "IXP.hw");
			hwmanager.addFromResource("PC1core", "PC1core.hw");
			hwmanager.addFromResource("PC2core", "PC2core.hw");
			hwmanager.addFromResource("PC8core1g", "PC8core1g.hw");
			hwmanager.addFromResource("PC8core10g", "PC8core10g.hw");
			hwmanager.addFromResource("VM", "VM.hw");
			hwmanager.addFromResource("PC48core", "PC48core.hw");
			hwmanager.addSubtypeFromResource("VMsmall.shw");
			hwmanager.addSubtypeFromResource("VM64bit.shw");
			hwmanager.addSubtypeFromResource("VM64bit_2port.shw");
			hwmanager.addSubtypeFromResource("HOST1core.shw");
			hwmanager.addSubtypeFromResource("HOST2core.shw");
			hwmanager.addSubtypeFromResource("SWR5_1.shw");
			hwmanager.addSubtypeFromResource("SWR8.shw");
			hwmanager.addSubtypeFromResource("SWR16.shw");
			hwmanager.addSubtypeFromResource("HOST48core.shw");
			hwmanager.addSubtypeFromResource("NPR.shw");
			HardwareSpec ixp_spec = hwmanager.getHardware("IXP");
			if (ixp_spec != null)
				tmp_menu.add(new Cluster.IXPMenu(ixp_spec));
			//add any subtypes we can find
			/*SUBTYPE*/
			java.io.File dir = getDefaultDir();
			java.io.File[] s_list = dir.listFiles(new HardwareSpec.Subtype.SHWFileFilter());
			java.io.File[] h_list = dir.listFiles(new HardwareSpec.HWFileFilter());
			int num_hwtype = 0;
			if (h_list != null) num_hwtype = Array.getLength(h_list);
			int j = 0;
			for (j = 0; j < num_hwtype; ++j)
			{
				hwmanager.addFromFile(h_list[j]);
			}
			int num_subtype = 0;//Array.getLength(s_list);
			if (s_list != null) num_subtype = Array.getLength(s_list);
			for (j = 0; j < num_subtype; ++j)
			{
				hwmanager.addSubtype(s_list[j]);
			}
			dir = new java.io.File(".");
			h_list = dir.listFiles(new HardwareSpec.HWFileFilter());
			if (h_list != null) num_hwtype = Array.getLength(h_list);
			else num_hwtype = 0;//Array.getLength(h_list);
			for (j = 0; j < num_hwtype; ++j)
			{
				hwmanager.addFromFile(h_list[j]);
			}
			s_list = dir.listFiles(new HardwareSpec.Subtype.SHWFileFilter());
			if (s_list != null) num_subtype = Array.getLength(s_list);
			else num_subtype = 0;//Array.getLength(s_list);
			for (j = 0; j < num_subtype; ++j)
			{
				hwmanager.addSubtype(s_list[j]);
			}   
			dir = getONLDir();
			h_list = dir.listFiles(new HardwareSpec.HWFileFilter());
			if (h_list != null) num_hwtype = Array.getLength(h_list);
			else num_hwtype = 0;//Array.getLength(h_list);
			for (j = 0; j < num_hwtype; ++j)
			{
				hwmanager.addFromFile(h_list[j]);
			}
			s_list = dir.listFiles(new HardwareSpec.Subtype.SHWFileFilter());
			if (s_list != null) num_subtype = Array.getLength(s_list);
			else num_subtype = 0;//Array.getLength(s_list);
			for (j = 0; j < num_subtype; ++j)
			{
				hwmanager.addSubtype(s_list[j]);
			}   
		}

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutdown hook ran!");
            	if (currentExperiment != null) currentExperiment.close();
            }
        });
	}

	public void initializeTopologyActions()
	{
		topologyActions = new Vector();
		if (!isSPPMon())
		{
			topologyActions.add(new Topology.DefaultRtsAction(topology));
			topologyActions.add(new Topology.DefaultHostRtAction(topology));
			topologyActions.add(new Topology.ClearRtsAction(topology));
			//if (getDebugLevel() > 0) topologyActions.add(new Topology.RemoveRtsAction(topology));
			//shows experiment password in separate window
			topologyActions.add(new ONL.UserAction("Show VM Password", true, false)
			{	
				public void actionPerformed(ActionEvent e)
				{
					if (currentExperiment != null) currentExperiment.showGeneratedPW();
				}
			});
			topologyActions.add(new ExpPasswordAction());
			//topologyActions.add(new Topology.TopologyAction("Set Subnet Allocation") {
				//public void actionPerformed(ActionEvent e)
				//{
					//final String opt0 = "OK";
					//final String opt1 = "Cancel";
					//Object[] options = {opt0,opt1};
					//JCheckBox cbox = new JCheckBox("Use Old Subnet Allocation:", isOldSubnet());
					//int rtn = JOptionPane.showOptionDialog(getMainWindow(),  
					//		cbox, 
					//		null,
					//		JOptionPane.YES_NO_OPTION,
					//		JOptionPane.QUESTION_MESSAGE, 
					//		null,
					//		options,
					//		options[0]);

				//	if (rtn == JOptionPane.YES_OPTION)
				//	{
				//		setOldSubnet(cbox.isSelected());
			    //	}
				//}
				//public boolean isEnabled() 
				//{
				//	if (topology.getNumComponents() > 0 || isTopoSet()) return false;
				//	else return (super.isEnabled());
				//}
			//});
			//topologyActions.add(new Topology.AddLinkAction(topology));
			topologyActions.add(new AddVGigEAction(this));
		}
		topologyActions.add(new Topology.AddHWTypeAction());
	}

	public void initializeFileMenu()
	{
		fileActions = new Vector();
		ONL.UserAction closeAction = (new ONL.UserAction(true, false)
		{
			public void actionPerformed(ActionEvent e)
			{
				if (statusDisplay.isCommitted() || (!expIsWaiting()))
				{
					closeCurrentExp();
					//if (isTestLevel(OBSTEST))
					closeObserver();
				}
				else
				{
					JOptionPane.showMessageDialog(mainWindow, 
							"You are in the progress of committing. Please wait until commit is finished to close",
							"Error Closing Experiment",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		ECMenuAction tmp_action = new ECMenuAction(this, LOADFILE, "Open");
		tmp_action.setEnabled(true);
		tmp_action.addAction(new ExpFileAction(this, true));
		fileActions.add(tmp_action);

		ExpFileAction efa;
		if (!isSPPMon())
		{
			fileActions.add(onlUndoManager.getCommitAction());
			fileActions.add(new Reservation.Action(this));
			fileActions.add(new Reservation.ExtendAction(this));
			fileActions.add(new Reservation.CancelAction(this));
			
		}
		//fileActions.add(new AdvancedMenuItem());
		tmp_action = new ECMenuAction(this, SAVEFILE, "Save");
		efa = new ExpFileAction(this, false);
		efa.setSave(true);
		tmp_action.addAction(efa);
		tmp_action.setEnabled(true);
		fileActions.add(tmp_action);
		tmp_action = new ECMenuAction(this, SAVEAS, "Save As ..."); 
		tmp_action.addAction(new ExpFileAction(this, false));
		tmp_action.setEnabled(true);
		fileActions.add(tmp_action);
		tmp_action = new ECMenuAction(this, CLOSE, "Close", true);
		tmp_action.addAction(closeAction);
		tmp_action.setEnabled(true);
		fileActions.add(tmp_action);
		tmp_action = new ECMenuAction(this, EXIT, "Exit");
		tmp_action.setEnabled(true);
		tmp_action.addAction(new ExitAction(this));
		fileActions.add(tmp_action);
		if (isTestLevel(NCCP_OFF)) fileActions.add(new TestAction());
	}
	public void initializeEditMenu()
	{
		editActions = new Vector();
		editActions.add(onlUndoManager.getUndoAction());
		ECMenuAction tmp_action;
		tmp_action = new ECMenuAction(this, DELETE, "Delete");
		tmp_action.setEnabled(true);
		tmp_action.addAction(new DeleteAction(this));
		editActions.add(tmp_action);
		tmp_action = new ECMenuAction(this, CLEAR, "Clear");
		editActions.add(tmp_action);
		tmp_action = new ECMenuAction(this, SELECTALL, "Select All");
		editActions.add(tmp_action);
		editActions.add(new BatchFileAction(this, false));
		editActions.add(new BatchFileAction(this, true, "Load Commands"));
	}

	public void initializeExtraMenu()
	{
		extraActions = new Vector();
		if (!isSPPMon())
		{
			virtualAction = new VirtualTopology.VAction();
			extraActions.add(virtualAction);
			extraActions.add(new Observer.AddObserver());
			extraActions.add(new Observer.GetObservable());
			extraActions.add(new ExpCompare.CompareAction("Compare Sessions"));
			if (isTestLevel(MANAGER)) 
			{
				if (sessionRecorder != null) 
				{
					extraActions.add(sessionRecorder);
					extraActions.add(sessionRecorder.getLoadAction());
				}
				extraActions.add((new ONL.UserAction("Print Debug Messages", true, false)
				{
					public void actionPerformed(ActionEvent e)
					{
						printer.printHistory();
					}
				}));
				extraActions.add((new ONL.UserAction("Set Debug Level", true, false)
				{
					public void actionPerformed(ActionEvent e)
					{
						String rtn = JOptionPane.showInputDialog(mainWindow, "Set Debug Level", String.valueOf(debug));
						debug = Integer.parseInt(rtn);
					}
				}));
			}
		}
	}
	public static boolean isSPPMon() { return (theCoordinator.properties.getPropertyBool(SPPMON));}
	public static ONLMainWindow getMainWindow() { return (theCoordinator.mainWindow);}
	public static VirtualTopology.VAction getVirtualAction() { return (theCoordinator.virtualAction);}
	public MonitorManager getMonitorManager() 
	{
		return monitorManager;
	}
	public Topology getTopology() { return topology;}
	public ONLComponent getComponentFromString(String str)
	{
		if (str.equals("null")) return null;
		String strarray[] = str.split(" ");
		String lbl = strarray[0];
		String tp = strarray[1];
		ExpCoordinator.print(new String("ExpCoordinator.getComponentFromString " + str), 6);
		return (topology.getONLComponent(lbl, tp));
	}
	public boolean isMonitoring() { return true;} //come up with better way to deal with mode but just want to get this thing up and running

	//interface Mode.MListener
	//public void modeChange(Mode.MEvent e) {}
	//end Mode.MListener

	public void addAction(ONL.UserAction a, int tp)
	{
		if (tp == COMMIT) return;
		ECMenuAction elem = (ECMenuAction)getMenuAction(tp);
		if (elem != null)  elem.addAction(a);
	}

	public void removeAction(ONL.UserAction a, int tp)
	{
		if (tp == COMMIT) return;
		ECMenuAction elem = (ECMenuAction)getMenuAction(tp);
		if (elem != null)  elem.removeAction(a);
	}

	private MenuAction getMenuAction(int tp)
	{
		Vector actions = null;
		switch(tp)
		{
		case EXIT:
		case SAVEFILE:
		case LOADFILE:
		case NEW:
		case SAVEAS:
		case CLOSE:
		case COMMIT:
			actions = fileActions;
			break;
		case SESSION_RECORDING:
			actions = extraActions;
			break;
		case CUT:
		case COPY:
		case PASTE:
		case DELETE:
		case CLEAR:
		case SELECTALL:
			actions = editActions;
			break;
		}
		MenuAction elem = null; 
		Action act = null;
		if (actions != null)
		{
			int max = actions.size();
			for (int i = 0; i < max; ++i)
			{
				act = (Action)actions.elementAt(i);
				if (act instanceof MenuAction)
				{
					elem = (MenuAction)act;
					if (elem.getType() == tp) return ((MenuAction)elem);
				}
			}
		}
		return null;
	}
	public Vector getEditActions() { return editActions;}
	public Vector getFileActions() { return fileActions;}
	public Vector getExtraActions() { return (extraActions);}
	public void setCurrentSelection(ONLComponent c) 
	{
		/*
      if (c != null)
	print(new String("ExpCoordinator.setCurrentSelection " + c.getLabel()), ExpCompare.TEST_CMP);
      else
	print("ExpCoordinator.setCurrentSelection null", ExpCompare.TEST_CMP);
		 */

		if (currentSelection.size() > 0)
		{
			Vector tmp = currentSelection;
			currentSelection = new Vector();
			int max = tmp.size();
			ONLComponent oc;
			for (int i = 0; i < max; ++i)
			{
				oc = (ONLComponent)tmp.elementAt(i);
				oc.setSelected(false);
			}
		}
		if (c != null) 
		{
			c.setSelected(true);
			currentSelection.add(c);
		}
	}
	public void addToCurrentSelection(ONLComponent c) 
	{
		if (c != null && !currentSelection.contains(c)) 
		{
			c.setSelected(true);
			currentSelection.add(c);
		}
	}
	public Vector getCurrentSelection() { return currentSelection;}
	public void addEdit(ONLComponent.Undoable e) 
	{  
		if (isBatch() && e instanceof Command.Edit)
		{
			Command cmd = ((Command.Edit)e).getBatchCommand();
				//get the current experiment
			try
			{
					cmd.writeXMLBatchCommand(getCurrentExp().getBatchWriter());
			}
			catch(XMLStreamException se)
			{
					ExpCoordinator.print(new String("ExpCoordinator.addEdit writing command for batch XMLStreamException " + se.getMessage()));
					se.printStackTrace();
			}
		}
		if (e == null) return;
		print("ExpCoordinator.addEdit", TEST_ADD);
		//if (isObserver() || isSnapshotView()) return;
		if (isObserver() || isSnapshotView() || !isCurrentExpLive()) return;
		if (compoundEdit != null) compoundEdit.addEdit(e);
		else
			onlUndoManager.addONLUndoable(e);
	} //will add to the Undo Manager
	public void addEditAt(ONLComponent.Undoable e, int i) 
	{ 
		print("ExpCoordinator.addEditAt", TEST_ADD);
		//if (isObserver() || isSnapshotView()) return;
		if (isObserver() || isSnapshotView() || !isCurrentExpLive()) return;
		if (compoundEdit != null) compoundEdit.addEdit(e);
		else
			onlUndoManager.addONLUndoableAt(e, i);
	} //will add to the Undo Manager
	public boolean isCurrentExpLive()
	{
		//if (!isTestLevel(CMPTEST)) return true;
		if (currentExperiment != null) return (currentExperiment.isLive());
		return true;
	}
	public boolean isCurrentEdit(ONLComponent.Undoable e) { return (onlUndoManager.isCurrentEdit(e));} //will add to the Undo Manager
	public Experiment getCurrentExp() 
	{
		if (currentExperiment == null) 
		{
			currentExperiment = new Experiment(this, new String("experiment" + expCount));
			++expCount;
		}
		return currentExperiment;
	}
	public boolean expIsWaiting()
	{
		if (currentExperiment != null) return (currentExperiment.isWaiting());
		else return true;
	}
	public void loadSnapShot(java.io.File f)
	{
		closeCurrentExp();
		/*
    properties.setProperty(VIEW_SNAPSHOT, true);
    if (f!= null )
      {
        try
          {
            currentExperiment = new Experiment(this, "");
            ONL.BaseFileReader frdr = new ONL.BaseFileReader(f);
            frdr.setCountAllLines(true);//blank lines mean something
            frdr.setNoClose(true);
            currentExperiment.read(frdr);
            monitorManager.loadRecordedDisplays(frdr);
            frdr.setNoClose(false);
            frdr.finish();
          }
        catch (IOException e)
          {
            System.err.println("Experiment - error could not open file:" + f.getAbsolutePath() + " for reading :" + e.getMessage());
          }
      }
		 */
	}
	private void setCurrentExp(java.io.File f)
	{
		closeCurrentExp();
		if (f != null) 
		{	
			try
			{
				FileReader fr = new FileReader(f);
				char buf[] = new char[7];
				fr.read(buf, 0, 7);
				fr.close();
				String str = new String(buf);
				if (str.equals("VERSION"))
				{
					addError(new StatusDisplay.Error("Sorry. Experiment file format no longer supported.",
							"Experiment load error.",
							"Experiment load error.",
							StatusDisplay.Error.POPUP | StatusDisplay.Error.LOG));  
				}
				else
				{
					currentExperiment = new Experiment(this, "");
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					Topology.DefaultHostRtAction.setOn(false);
					ExperimentXML exp_xml = new ExperimentXML(currentExperiment, xmlReader, f);
					xmlReader.parse(new InputSource(new FileInputStream(f)));
					Topology.DefaultHostRtAction.setOn(true);
				}
				ExpCoordinator.printer.print("new experiment " + currentExperiment.getProperty(Experiment.LABEL));
			}
			catch (IOException e)
			{
				System.err.println("ExpCoordinator- error could not open file:" + f.getAbsolutePath() + " for reading");
			}
			catch(SAXException e2)
			{
				ExpCoordinator.print(new String("ExpXMLFileAction.loadFromFile SAXException " + e2.getMessage()));
			}
		} 
	} 
	protected void setCurrentExp(Experiment exp)
	{
		//closeCurrentExp();
		currentExperiment = exp;
	}
	protected void closeCurrentExp()
	{
		if (monitorManager != null) monitorManager.stopMonitoring();
		//if (virtualAction != null) virtualAction.clearWindow();
		if (currentExperiment != null) currentExperiment.close();
		setOldSubnet(false);
		onlUndoManager.clear();
		statusDisplay.clear();
		properties.removeExpListeners(currentExperiment);
		currentExperiment = null;
		properties.setProperty(VIEW_SNAPSHOT, false);
		properties.setProperty(TOPO_SET, false);
	} 
	protected void closeObserver()
	{
		if (Observer.theObserver != null) Observer.theObserver.close();
	}
	public void sendMessage(NCCP.Message msg) 
	{ 
		if (expDaemon.isConnected()) expDaemon.sendMessage(msg);
	}
	public void sendRequest(ExpRequest req) 
	{ 
		if (expDaemon.isConnected()) expDaemon.addExpRequest(req);
	}
	public void remap(ONLComponent c)
	{
		if (isTestLevel(REMAP))
		{
			c.setProperty(ONLComponent.REMAP_SENT, true);
			ExpDaemon.NCCP_RemapComponent remap = new ExpDaemon.NCCP_RemapComponent(c, currentExperiment, expDaemon);
			sendRequest(remap);
		}
	}
	public void removeRequest(ExpRequest req) { expDaemon.removeExpRequest(req);}
	public boolean isManagement() { return isTestLevel(MANAGER);}
	public void setManagement(boolean b) 
	{  
		management = b;
		if (b && isTestLevel(PROXYTEST)) addProxyTest();
		testCodes.add(MANAGER);
	}
	public boolean isDirectConn() { return (directConn);}// || isTestLevel(NPTEST));}
	public void setDirectConn(boolean b) { directConn = b;}
	public boolean isConnected() 
	{ return (isSPPMon() || ((proxy.isConnected() || directConn)&& expDaemon.isConnected()));}
	public boolean connect() 
	{ 
		if (isSPPMon()) return true;
		boolean rtn = false;
		ExpCoordinator.printer.print("ExpCoordinator.connect");
		if (!proxy.isConnected() && !directConn)
		{
			if (!proxy.connect()) 
			{
				ExpCoordinator.printer.print("EC.connect returning false");
				return false;
			}
		}
		if (!expDaemon.isConnected())
		{
			expDaemon.connect();
		}
		return true;
	}
	public void addError(StatusDisplay.Error e) 
	{ 
		statusDisplay.addError(e);
	}
	public void addToProgressBar(ONLComponent c) 
	{ 
		if (isSPPMon()) return;
		if (c instanceof Hardware)
		    statusDisplay.increaseProgressMax(120);
		else
		    statusDisplay.increaseProgressMax(30);
		statusDisplay.waitForStatus(c);
	}
	public void addToProgressBar(NCCPOpManager.Operation o) 
	{ 
		if (isSPPMon()) return;
		statusDisplay.increaseProgressMax(30);
		statusDisplay.waitForStatus(o);
	}
	public void removeFromProgressBar(Object o) { statusDisplay.statusReturned(o);}
	public String getProperty(String pname) { return (properties.getProperty(pname));}
	public Proxy getProxy() { return proxy;}
	public void startCompoundEdit() 
	{
		if (compoundEdit != null)
			endCompoundEdit();
		compoundEdit = new ONLUndoManager.AggregateEdit();
	}
	public void endCompoundEdit()
	{
		if (compoundEdit != null)
			onlUndoManager.addONLUndoable(compoundEdit);
		compoundEdit = null;
	}
	public static int getDebugLevel() { return debug;}
	public static boolean isTestLevel(int t) { return (theCoordinator.testCodes.isLevel(t));}
	protected static void setTestLevel(int t){ theCoordinator.testCodes.add(t);}
	public void getUserInfo() { getUserInfo("Log In");}
	public void getUserInfo(String ttl)
	{
		//print(new String("ExpCoordinator.getUserInfo(" + ttl + ") password:" + properties.getProperty(PASSWORD)), 6);
	    if (properties.getProperty(PASSWORD) != null && !isTestLevel(ASK_FOR_USER)) return;

		TextFieldwLabel uname = new TextFieldwLabel(30, "User Name:");
		if (properties.getProperty(USERNAME) != null) 
			uname.setText(properties.getProperty(USERNAME));
		else
			uname.setText(System.getProperty("user.name"));
		JPasswordField pwd_fld = new JPasswordField();
		pwd_fld.setEchoChar('*');
		TextFieldwLabel password = new TextFieldwLabel(pwd_fld, "Password:");
		Object[] fields = {uname, password};
		String opt = "Enter";
		Object[] opts = {opt};

		JOptionPane.showOptionDialog(mainWindow, 
				fields, 
				ttl, 
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, 
				null,
				opts,
				opt);

		String un = uname.getText();
		properties.setProperty(USERNAME, un);
		if (un.length() > 1)
		{
			properties.setProperty(PASSWORD, jcrypt.crypt(un.substring(0,2), pwd_fld.getPassword()));
			pwd_fld.setText("");
			pwd_fld = null;
			//print(new String("user name:" + un + " password hash:" + properties.getProperty(PASSWORD)), 6);
		}
	}
	public void clearPassword() 
	{ 
		//print("ExpCoordinator.clearPassword", 6);
		properties.remove(PASSWORD);
	}
	public static void print(String s, int l) { if (printer != null) printer.print(s,l);}
	public static void print(String s) {  if (printer != null) printer.print(s);}
	public static void setObserve(boolean b) 
	{
		if (theCoordinator != null)
		{
			print(new String("ExpCoordinator.setObserve " + b), 2);
			theCoordinator.properties.setProperty(OBSERVE, b);
		}
	}
	public void addConnectionListener(NCCPConnection.ConnectionListener l) {expDaemon.addConnectionListener(l);}
	public void removeConnectionListener(NCCPConnection.ConnectionListener l) {expDaemon.removeConnectionListener(l);}
	public static boolean isTesting() { return (isTestLevel(ONLCDTEST));}
	public void setWindowBounds(JFrame f, int x, int y, int w, int h)
	{
		int width = w;
		int height = h;
		int x_coord = x;
		int y_coord = y;

		if (screenBounds != null && screenBounds.contains(x,y,w,h))
		{
			int screen_w = (int)screenBounds.getWidth();
			int screen_h = (int)screenBounds.getHeight();
			if (w > screen_w) width = screen_w - 10;
			if (h > screen_h) height = screen_h - 10;
			int bottom = height + y;
			int right = width + x;

			int screen_b = screen_h + (int)screenBounds.getY();
			int screen_r = screen_w + (int)screenBounds.getX();

			if (bottom > screen_b) y_coord = y - (bottom - screen_b - 5);
			if (right > screen_r) x_coord = x - (right - screen_r - 5);
		}
		f.setBounds(x_coord, y_coord, width, height);
	}

	public static boolean isObserver() { return (theCoordinator.properties.getPropertyBool(OBSERVE));}

	public void addProxyTest()
	{
		mainWindow.addMenu(proxy.getTestMenu());
	}
	public void addPropertyListener(PropertyChangeListener l) 
	{ 
		if (l instanceof ONL.ExpPropertyListener)
			((ONL.ExpPropertyListener)l).setExperiment(currentExperiment);
		properties.addListener(l);
	}
	public void addPropertyListener(String pnm, PropertyChangeListener l) 
	{ 
		if (l instanceof ONL.ExpPropertyListener)
			((ONL.ExpPropertyListener)l).setExperiment(currentExperiment);
		properties.addListener(pnm, l);
	}
	public void removePropertyListener(PropertyChangeListener l) { properties.removeListener(l);}
	public void removePropertyListener(String pnm, PropertyChangeListener l) { properties.removeListener(pnm, l);}

	public static java.io.File getONLDir()
	{
		if (theCoordinator.onldir == null)
		{
			java.io.File home_dir = new java.io.File(System.getProperty("user.home"));
			theCoordinator.onldir = new java.io.File(home_dir, ".onldir");
			if (!theCoordinator.onldir.exists())
				theCoordinator.onldir.mkdir();
		}
		return (theCoordinator.onldir);
	}

	public static java.io.File getDefaultDir()
	{
		String sval = theCoordinator.properties.getProperty(DEFAULT_DIR);
		if (sval != null) return (new java.io.File(sval));
		else 
			return(getONLDir());
	}

	public boolean isLinkToolEnabled() { return (topology.getLinkTool().isEnabled());}
	public static void recordEvent(int index, String output[])
	{
		//if (theCoordinator.sessionRecorder != null) theCoordinator.sessionRecorder.recordEvent(index, output);
	}
	public static void recordMonitorEvent(int index, String graph, MonitorDataType.Base mdt)
	{
		//if (theCoordinator.sessionRecorder != null) theCoordinator.sessionRecorder.recordMonitorEvent(index, graph, mdt);
	}
	public static boolean isRecording()
	{
		//if (theCoordinator.currentExperiment != null)
		//return (theCoordinator.properties.getPropertyBool(RECORDING) || (!theCoordinator.currentExperiment.isLive()));//(theCoordinator.sessionRecorder != null) && theCoordinator.sessionRecorder.isRecording());
		//else 
		return (theCoordinator.properties.getPropertyBool(RECORDING));
	}
	public static boolean isBatch(){ return (theCoordinator.properties.getPropertyBool(ExperimentXML.BATCH));}
	protected static void setBatch(boolean b)  { theCoordinator.properties.setProperty(ExperimentXML.BATCH, b);}
	protected static void setRecording(boolean b) { theCoordinator.properties.setProperty(RECORDING, b);}
	protected void setCommit(boolean b) { statusDisplay.setCommitBorder(b);}
	protected void addStatusListener(StatusDisplay.Listener l) { statusDisplay.addListener(l);}
	protected void removeStatusListener(StatusDisplay.Listener l) { statusDisplay.removeListener(l);}
	public static boolean isSnapshotView()
	{
		return (theCoordinator.properties.getPropertyBool(VIEW_SNAPSHOT));
	}
	public static boolean isTopoSet()
	{
		return (theCoordinator.properties.getPropertyBool(TOPO_SET));
	}
	public static void setTopoSet(boolean b) { theCoordinator.properties.setProperty(TOPO_SET, b);}
	protected static void setProperty(String nm, String s) { theCoordinator.properties.setProperty(nm, s);}
	public void saveDefaults()
	{
		try
		{
			java.io.File prop_file = new java.io.File(getONLDir(), PROP_FILE);
			if (!prop_file.exists()) prop_file.createNewFile();
			BufferedWriter fwrtr = new BufferedWriter(new FileWriter(prop_file));
			//XMLStreamWriter xmlWrtr = XMLOutputFactory.newInstance().createXMLStreamWriter(fwrtr);
			//xmlWrtr.setDefaultNamespace("onl");
			//xmlWrtr.setPrefix("onl", "onl");
			//xmlWrtr.writeStartDocument();

			String sval = properties.getProperty(DEFAULT_DIR);
			if (sval != null) 
			{
				// xmlWrtr.writeStartElement(DEFAULT_DIR);
				//xmlWrtr.writeAttribute(ExperimentXML.VALUE, sval);
				//xmlWrtr.writeEndElement();

				fwrtr.write(DEFAULT_DIR);
				fwrtr.newLine();
				fwrtr.write(sval);
				fwrtr.newLine();

			}
			sval = properties.getProperty(DEFAULT_POLLING);
			if (sval != null) 
			{
				//xmlWrtr.writeStartElement(DEFAULT_POLLING);
				//xmlWrtr.writeAttribute(ExperimentXML.VALUE, sval);
				//xmlWrtr.writeEndElement();

				fwrtr.write(DEFAULT_POLLING);
				fwrtr.newLine();
				fwrtr.write(sval);
				fwrtr.newLine();

			}
			//Base and subtype hardware
			//HardwareSpec.getTheManager().writeXMLHWTypes(xmlWrtr);
			fwrtr.close();
		}
		catch(java.io.IOException e){}
	}

	public static boolean isAdvanced() { return (theCoordinator.properties.getPropertyBool(ADVANCED));}
	public static boolean isOldSubnet() { return oldSubnet;}
	protected static void setOldSubnet(boolean b) { oldSubnet = b;}
}
