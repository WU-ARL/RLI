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
 * File: MonitorManager.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 11/07/2003
 *
 * Description:
 *
 * Modification History:
 *
 */

import java.io.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.text.DecimalFormat;
import java.lang.reflect.Array;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class MonitorManager implements MenuFileAction.Saveable//, Mode.Manager
{
	public static final int TEST_LOADING = 4;
	public static final String tokGRAPH_ONLY = "GRAPH_ONLY"; //used when graph is the only thing saved
	public static final String tokGRAPH= "GRAPH";

	//need to make file name and polling interval command line arguments
	private int pollInterval = 1;
	private Vector displays;
	private Vector eventListeners = null;
	private boolean stopped = false;
	private int plotHeight = 300;
	private int plotWidth = 550;
	private boolean tst = false;
	private LogFileRegistry logFileRegistry;
	private Display currentDisplay = null;
	private int type = 0;
	private ParamSelector paramSelector;
	private ExpCoordinator expCoordinator = null;
	protected static int history = 400;
	private PollingRate defaultPolling = null;
	private final String tokPOLLING_DEFAULT = "DEF_POLLING";

	private JMenu menu = null;
	private JToolBar toolbar = null;
	private AddDisplayAction addDisplayAction = null;
	private MenuFileAction saveFileAction = null;
	private MenuFileAction loadFileAction = null;
	private int fontSize = 9; //this sets the label font for legend
	private int axisFontSize = 12; //this sets the label font axis label
	private int axisNFontSize = 9; //this sets the label font axis numbers

	private final int showLF = 1;
	private final int showQ = 2;
	private final int cTEST = 4;
	private final int BACKGROUNDOFF = 8;
	private final int ARL = 19;


	/////////////////////////////////////////////////////XMLHandler/////////////////////////////////////////////////////////////
	protected static class XMLHandler extends DefaultHandler
	{
		public static final String DEFPOLLING = "defaultPolling";
		public static final String DISPLAY_UNITS = "displayUnits";
		public static final String GRAPH = "graph";
		public static final String GRAPHTYPE = "graphtype";
		public static final String HEADER_INC = "headerIncluded";
		public static final String PARAM = "param";
		public static final String PARAMSPEC = "paramSpec";
		public static final String POSITION = "position";
		public static final String SCALING_FACTOR = "scalingFactor";
		public static final String STROKE = "stroke";
		public static final String SIZE = "size";
		public static final String TITLE = "title";
		public static final String WBOUNDS = "windowBounds";
		public static final String XLABEL = "xaxisLabel";
		public static final String YLABEL = "yaxisLabel";
		public static final String YUNITS = "yaxisUnits";

		protected ExperimentXML expXML = null;
		private MonitorManager mmanager = null;

		public XMLHandler(ExperimentXML exp_xml, MonitorManager mm)
		{
			super();
			expXML = exp_xml;
			mmanager = mm;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			ExpCoordinator.print(new String("MonitorManager.XMLHandler.startElement: " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(DEFPOLLING)) mmanager.defaultPolling.setRate(attributes);
			if (localName.equals(GRAPH))
			{	  
				Display.ID id = new Display.ID(attributes);
				Display rtn_display = mmanager.addDisplay(id);
				if (rtn_display != null) expXML.setContentHandler(rtn_display.getXMLHandler(expXML));
			}
		}
		/*
    public void processingInstruction(String target, String data)
    {
      ExpCoordinator.print(new String("XMLHandler.processingInstruction target:" + target + " data.length:" + data.length()), ExperimentXML.TEST_XML);
      //try
      //	{
	  mmanager.loadFromReader(new ONL.BaseFileReader(new StringReader(data)));
	  //	}
	  //catch (java.io.IOException e){System.err.println(new String("MonitorManager.XMLHandler.processingInstruction IOException: " + e.getMessage()));}
    }
		 */
		public void endElement(String uri, String localName, String qName)
		{
			ExpCoordinator.print(new String("XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(ExperimentXML.MONITORING)) 
			{
				expXML.removeContentHandler(this);
			}
		}
	}
	////////////////////////////////////////////// Snapshot //////////////////////////////////////////////////////////////////////////
	public static class Snapshot extends Vector
	{
		public Snapshot(MonitorManager m)
		{
			super(m.displays);
		}
	}

	//////////////////////////////////////////////// Event //////////////////////////////////////////////////////////////////////////////
	public static class Event extends EventObject
	{
		public static final int ADD_DISPLAY = 1;
		public static final int REMOVE_DISPLAY = 2;
		public static final int ADD_PARAM = 3;
		public static final int REMOVE_PARAM = 4;
		public static final int CHANGE_PARAM = 5;
		public static final int CHANGE_DISPLAY = 6;
		public static final int GET_FOCUS = 7;
		private int type = ADD_DISPLAY;
		private MonitorFunction.Param param = null;
		private int index = -1;

		public Event(Display d, MonitorFunction.Param p, int tp) { this(d, p, tp, -1);}
		public Event(Display d, MonitorFunction.Param p, int tp, int ndx)
		{
			super(d);
			type = tp;
			param = p;
			index = ndx;
		}

		public Event(Display d, int tp) { this(d, null, tp, -1);}

		public int getType() { return type;}
		public int getIndex() { return index;}
		public Display getDisplay() { return ((Display)getSource());}
		public MonitorFunction.Param getParam() { return param;}
		public void write(ONL.Writer wrtr) throws IOException
		{
			ExpCoordinator.print(new String("MonitorManager.Event.write"), 8);
			wrtr.writeInt(type);
			Display display = getDisplay();
			switch (type)
			{
			case ADD_DISPLAY:
				if (display != null) display.write(wrtr);
				//display.getSpec().write(new SpecFile.SpecWriter(wrtr));
				break;
			case REMOVE_DISPLAY:
			case GET_FOCUS:
				if (display != null) display.getID().write(wrtr);
				break;
			case ADD_PARAM:
			case REMOVE_PARAM:
				if (display != null) display.getID().write(wrtr);
				if (param != null) param.write(wrtr);
				break;
			case CHANGE_PARAM:
				if (display != null) display.getID().write(wrtr);
				wrtr.writeInt(index);
				if (param != null) param.writeChangeable(wrtr);
				break;
			case CHANGE_DISPLAY:
				wrtr.writeInt(index);
				display.writeChangeable(wrtr);
				break;
			}
		}

		public static Event read(ONL.Reader rdr) throws IOException
		{
			Event rtn = null;
			int tp = rdr.readInt();
			Display display = null;
			Display.Param p = null;
			Display.ID id = null;
			int ndx = -1;
			MonitorManager monitorManager = ExpCoordinator.theCoordinator.getMonitorManager();
			switch (tp)
			{
			case ADD_DISPLAY:
				display = monitorManager.readDisplay(rdr);
				break;
			case REMOVE_DISPLAY:
			case ADD_PARAM:
			case REMOVE_PARAM:
				//case CHANGE_DISPLAY:
					id = new Display.ID(rdr);
					display = monitorManager.getDisplay(id);
					if (tp == REMOVE_DISPLAY || display == null)
						break;
					p = display.readParam(rdr);
					break;
			case CHANGE_PARAM:
				id = new Display.ID(rdr);
				display = monitorManager.getDisplay(id);
				ndx = rdr.readInt();
				p = display.getParam(ndx);
				p.readChangeable(rdr);
				break;
			case CHANGE_DISPLAY:
				ndx = rdr.readInt();
				display = monitorManager.getDisplayAt(ndx);
				String ottl = display.getTitle();
				display.readChangeable(rdr);
				ExpCoordinator.print(new String("MonitorManager.EventResponse.read Change Display from " + ottl + " to:" + display.getTitle()), 6);
				break;
			case GET_FOCUS:
				id = new Display.ID(rdr);
				display = monitorManager.getDisplay(id);
				display.setVisible(true);
				break;
			}
			if (display != null)
				rtn = new Event(display, p, tp, ndx);
			return rtn;
		}
	}


	//////////////////////////////////////////////// Listener/////////////////////////////////////////////////////////////////////////////
	public static interface Listener
	{
		public void monitoringChanged(MonitorManager.Event e);
	}


	public static final int STARTER = -1;

	//this is to allow a delay between monitoring requests
	private int currIndex = 0;

	//start time so all displays have same reference point
	private TimeListener timeListener = null;


	//////////////////////////////////////////////////////////// Display ////////////////////////////////////////////////////////////////
	public static abstract class Display extends JFrame implements MonitorFunction
	{
		//GRAPH TYPES
		public static final int UNKNOWN = 0;
		public static final int HISTOGRAM = 1;
		public static final int MULTILINE = 2;
		public static final int GRID = 3;
		public static final int ONESHOT_TABLE = 4;
		public static final int LOGGED_DATA = 5;

		public static final String UNKNOWN_STR = "unknown";
		public static final String HISTOGRAM_STR = "histogram";
		public static final String MULTILINE_STR = "multiline";
		public static final String GRID_STR = "grid";
		public static final String ONESHOT_TABLE_STR = "oneshotTable";
		public static final String LOGGED_DATA_STR = "logged";
		protected MonitorManager monitorManager = null;
		protected int type = UNKNOWN;

		/////////////////////////////////////////////////MonitorManager.Display.DXMLHandler //////////////////////////////////////
		protected static class DXMLHandler extends DefaultHandler
		{
			protected String currentElement = "";
			protected ExperimentXML expXML = null;
			protected Display display = null;

			public DXMLHandler(ExperimentXML exp_xml, Display d)
			{
				super();
				expXML = exp_xml;
				display = d;
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{
				currentElement = new String(localName);
				ExpCoordinator.print(new String("MonitorManager.Display.DXMLHandler.startElement " + localName + " display " + display.getTitle()), ExperimentXML.TEST_XML);
				if (localName.equals(XMLHandler.WBOUNDS))
				{
					int x = Integer.parseInt(attributes.getValue(ExperimentXML.XCOORD));
					int y = Integer.parseInt(attributes.getValue(ExperimentXML.YCOORD));
					int w = Integer.parseInt(attributes.getValue(ExperimentXML.WIDTH));
					int h = Integer.parseInt(attributes.getValue(ExperimentXML.HEIGHT));
					ExpCoordinator.theCoordinator.setWindowBounds(display, x, y, w, h);
				}
			}
			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("MonitorManager.Display.DXMLHandler.endElement " + localName + " display " + display.getTitle()), ExperimentXML.TEST_XML);
				if (localName.equals(XMLHandler.GRAPH)) expXML.removeContentHandler(this);
			}
		}
		/////////////////////////////////////////////////ID//////////////////////////////////////
		public static class ID 
		{
			private int type = UNKNOWN;
			private String title = "";
			public ID(int tp, String t)
			{
				type = tp;
				title = t;
			}
			public ID(Attributes attributes)
			{
				type = getTypeFromString(attributes.getValue(XMLHandler.GRAPHTYPE));
				title = attributes.getValue(XMLHandler.TITLE);
				ExpCoordinator.print(new String("MonitorManager.Display.ID from XML type:" + type + " title:" + title), ExperimentXML.TEST_XML);
			}
			public ID(ONL.Reader rdr) throws IOException
			{
				read(rdr);
			}
			public void write(ONL.Writer wrtr) throws IOException
			{
				wrtr.writeInt(type);
				wrtr.writeString(title);
				ExpCoordinator.print(new String("MonitorManager.Display.ID.write type:" + type + " title:" + title), 5);
			}
			public void read(ONL.Reader rdr) throws IOException
			{
				type = rdr.readInt();
				title = rdr.readString();
				ExpCoordinator.print(new String("MonitorManager.Display.ID.read type:" + type + " title:" + title), 5);
			}
			public int getType() { return type;}
			public String getTitle() { return (new String(title));}
		}

		////////////////////////////////////////////////////////////////////TitleAction/////////////////////////////////////////////////
		public static class TitleAction extends ONL.UserAction //AbstractAction
		{
			private Display display = null;
			public TitleAction(Display d)
			{
				super("Change Title", false, true);
				display = d;
			}
			public void actionPerformed(ActionEvent e)
			{
				if (display != null)
				{
					TextFieldwLabel new_title = new TextFieldwLabel(50, "New Title:");
					Object[] objectArray = {new String("Old Title: " + display.getTitle()), new_title};
					final String opt0 = "Enter";
					final String opt1 = "Cancel";
					Object[] options = {opt0,opt1};

					int rtn = JOptionPane.showOptionDialog(display.monitorManager.expCoordinator.getMainWindow(), 
							objectArray, 
							"Change Title", 
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, 
							null,
							options,
							options[0]);

					if (rtn == JOptionPane.YES_OPTION)
					{
						if (new_title.getText().length() > 0)
						{
							display.setTitle(new_title.getText());
							int ndx = display.monitorManager.getDisplayIndex(display);
							display.monitorManager.fireEvent(new MonitorManager.Event(display, null, MonitorManager.Event.CHANGE_DISPLAY, ndx));
						}
					}
				} 
			}
		}
		public Display(MonitorManager mw, ID id) 
		{
			super(id.getTitle());
			monitorManager = mw;
			type = id.getType();
			//set properties so individual frame may be closed, resized or iconified
			setResizable(true);
			addWindowListener(new WindowAdapter(){
				public void windowGainedFocus (WindowEvent e)
				{
					monitorManager.fireEvent(new MonitorManager.Event((Display)e.getSource(), null, MonitorManager.Event.GET_FOCUS));
				}
			});
		}

		public Display(MonitorManager mw, int t) 
		{
			super();
			monitorManager = mw;
			type = t;
			//set properties so individual frame may be closed, resized or iconified
			setResizable(true);
			addWindowListener(new WindowAdapter(){
				public void windowGainedFocus (WindowEvent e)
				{
					monitorManager.fireEvent(new MonitorManager.Event((Display)e.getSource(), null, MonitorManager.Event.GET_FOCUS));
				}
			});
		}
		public boolean isDoubleBuffered() { return true;}
		public abstract void stopMonitoring();
		public abstract SpecFile.MFunctionSpec getSpec();
		public boolean isMonitorFunction()
		{
			if (type == UNKNOWN) return false;
			else return true;
		}
		public boolean isGraph() 
		{
			if (type == HISTOGRAM || type == MULTILINE) return true;
			else return false;
		}
		public void setType(int t) { type = t;}
		public int getDType() { return type;}

		//public interface MonitorFunction 
		public void addData(MonitorPlus nd){}
		public void removeData(MonitorPlus nd){}
		public void addConditional(Conditional c){}
		public void removeConditional(Conditional c){}
		public void writeChangeable(ONL.Writer wrtr) throws IOException
		{
			getID().write(wrtr);
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			getID().write(wrtr);
			wrtr.writeString(new String(getX() + " " + getY() + " " + getWidth() + " " + getHeight()));
		}


		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(XMLHandler.GRAPH);
			xmlWrtr.writeAttribute(XMLHandler.GRAPHTYPE, getTypeString());
			xmlWrtr.writeAttribute(XMLHandler.TITLE, getTitle());
			//write display window bounds
			xmlWrtr.writeStartElement(XMLHandler.WBOUNDS);
			xmlWrtr.writeAttribute(ExperimentXML.XCOORD, String.valueOf(getX()));
			xmlWrtr.writeAttribute(ExperimentXML.YCOORD, String.valueOf(getY()));
			xmlWrtr.writeAttribute(ExperimentXML.WIDTH, String.valueOf(getWidth()));
			xmlWrtr.writeAttribute(ExperimentXML.HEIGHT, String.valueOf(getHeight()));
			xmlWrtr.writeEndElement();//display window bounds
			writeXMLParams(xmlWrtr);
			xmlWrtr.writeEndElement();//end graph
		}

		public void writeXMLParams(XMLStreamWriter xml_wrtr) throws XMLStreamException{}

		public abstract ContentHandler getXMLHandler(ExperimentXML exp_xml);
		public static int getTypeFromString(String str)
		{
			if (str.equals(MULTILINE_STR)) return MULTILINE;
			if (str.equals(HISTOGRAM_STR)) return HISTOGRAM;
			if (str.equals(GRID_STR)) return GRID;
			if (str.equals(ONESHOT_TABLE_STR)) return ONESHOT_TABLE;
			if (str.equals(LOGGED_DATA_STR)) return LOGGED_DATA;
			return UNKNOWN;
		}

		public String getTypeString()
		{
			switch (type)
			{
			case MULTILINE:
				return MULTILINE_STR;
			case HISTOGRAM:
				return HISTOGRAM_STR;
			case GRID:
				return GRID_STR;
			case ONESHOT_TABLE:
				return ONESHOT_TABLE_STR;
			case LOGGED_DATA:
				return LOGGED_DATA_STR;
			default:
				return UNKNOWN_STR;
			}
		}

		public void readChangeable(ONL.Reader rdr) throws IOException
		{
			ID tmp_id = new ID(rdr);
			setTitle(tmp_id.getTitle());
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			String[] strarray = rdr.readString().split(" ");
			String xlabel = null;
			String ylabel = null;
			int len = Array.getLength(strarray);
			Point p = null;
			Dimension dim = null;
			int x = Integer.parseInt(strarray[0]);
			int y = Integer.parseInt(strarray[1]);
			int w = Integer.parseInt(strarray[2]);
			int h = Integer.parseInt(strarray[3]);
			ExpCoordinator.theCoordinator.setWindowBounds(this, x, y, w, h);
		}
		public void recordData(ONL.Writer writer) throws IOException
		{
			getID().write(writer);
			int max = getNumParams();
			writer.writeInt(max);
			MonitorFunction.Param mparam;
			for (int i = 0; i < max; ++i)
			{
				mparam = getParam(i);
				if (mparam != null) 
				{
					writer.writeInt(i);
					mparam.record(writer);
				}
			}
		}
		public void readRecordedData(ONL.Reader reader) throws IOException
		{
			int max = reader.readInt();
			MonitorFunction.Param mparam;
			int index;
			for (int i = 0; i < max; ++i)
			{
				index = reader.readInt();
				mparam = getParam(i);
				if (mparam != null) 
				{
					mparam.loadRecording(reader);
				}
			}
			validate();
		}
		public int getNumParams() { return 0;}
		public abstract MonitorFunction.Param readParam(ONL.Reader rdr) throws IOException;
		public abstract MonitorFunction.Param getParam(int ndx) ;
		//end public interface MonitorFunction
		public boolean isEqual(Display.ID id)
		{
			return (type == id.getType() && (getTitle().equals(id.getTitle())));
		}
		public boolean isEqual(Display d)
		{
			return (isEqual(d.getID()));
		}
		public ID getID() { return (new ID(type, getTitle()));}
	}

	//inner class basically keeps a universal time
	public class TimeListener implements BoxedRangeListener
	{
		private double xmax = 0;
		public TimeListener(){}
		public void changeVisible(BoxedRangeEvent e){}
		public void changeBoundaries(BoxedRangeEvent e){}
		public void changeXVisible(BoxedRangeEvent e){}
		public void changeYVisible(BoxedRangeEvent e){}
		public void changeXBounds(BoxedRangeEvent e)
		{
			if ((e.getType() & BoxedRangeEvent.XMAX_CHANGED) != 0)
			{
				if (((BoxedRangeModel)e.getSource()).getXMax() > xmax)
					xmax = ((BoxedRangeModel)e.getSource()).getXMax();
			}
		}
		public void changeYBounds(BoxedRangeEvent e){}
		public double getCurrentTime(){ return xmax;}
		private void reset() { xmax = 0;}
	}


	//put in Main Window mainWindow-> monitorManager
	//perhaps make an action that can be called from either window
	private class AddFileAction extends MenuFileAction
	{
		public AddFileAction(MonitorManager mm, ONLMainWindow w)
		{
			super(mm, true, w, "Add From File", false, false);
		}
		public void actionPerformed(ActionEvent e)
		{
			//expCoordinator.setCurrentMode(Mode.MONITORING);
			super.actionPerformed(e);
		}
	}
	private class AddDisplayAction extends ONL.UserAction //AbstractAction
	{
		private MonitorManager monitorManager;
		private int type = Display.MULTILINE;

		public AddDisplayAction(MonitorManager w) {this(w, "Add Monitoring Display", Display.MULTILINE);}
		public AddDisplayAction(MonitorManager w, String nm, int t)
		{
			super(nm, false, false);
			monitorManager = w;
			type = t;
		}
		public void actionPerformed(ActionEvent e)
		{

			monitorManager.setCurrentDisplay(null);
			//monitorManager.expCoordinator.setCurrentMode(Mode.MONITORING);
			String str1 = new String("Name:");
			JTextField titleField = new JTextField(20);
			titleField.setText("");
			Object[] array = {str1,titleField};

			final String opt0 = "Enter";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			int rtn = JOptionPane.showOptionDialog(monitorManager.getMainWindow(), 
					array, 
					"Graph Title", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				if (titleField.getText().length() > 0)
				{
					monitorManager.addDisplay(type, titleField.getText());
				}
				else monitorManager.addDisplay(type, new String("Bandwidth"));
			}
		}
	}

	public MonitorManager(String args[], int w, int h, ExpCoordinator ec)
	{
		if (args == null) ExpCoordinator.print("MonitorManager args is null");
		int i = 0;
		int max_vcs = 10;
		int p = -1;
		plotHeight = h;
		plotWidth = w;
		displays = new Vector(max_vcs);
		String fname = null; //file name to load graph from file
		logFileRegistry = new LogFileRegistry(this);
		timeListener = new TimeListener();
		paramSelector = new ParamSelector();
		expCoordinator = ec;

		eventListeners = new Vector();

		defaultPolling = new PollingRate();
		String s = ec.getProperty(ExpCoordinator.DEFAULT_POLLING);
		if (s != null)
		{
			defaultPolling.parseRate(s);
		}

		//TopologyFile tf = new TopologyFile(this);

		while (i < args.length)
		{
			if (args[i].compareTo("-test") == 0)
			{
				type = type | cTEST;
				tst = true;
				++i;
			}
			else if (args[i].compareTo("-arl") == 0)
			{
				type = type | ARL;
				++i;
			}
			else if (args[i].compareTo("-lf") == 0)
			{
				type = type | showLF;
				++i;
			}
			else if (args[i].compareTo("-q") == 0)
			{
				type = type | showQ;
				//ExpCoordinator.print("DQ is enabled type is " + type);
				++i;
			}
			else if (args[i].compareTo("-h") == 0)
			{
				history = Integer.parseInt(args[++i]);
			}
			else if (args[i].compareTo("-gray") == 0)
			{
				type = type | BACKGROUNDOFF;
				++i;
			}
			else if (args[i].compareTo("-font") == 0)
			{
				fontSize = Integer.parseInt(args[++i]);
				++i;
			}
			else if (args[i].compareTo("-afont") == 0)
			{
				axisFontSize = Integer.parseInt(args[++i]);
				++i;
			}
			else if (args[i].compareTo("-anfont") == 0)
			{
				axisNFontSize = Integer.parseInt(args[++i]);
				++i;
			}
			else ++i;
		}
		//add Monitor dependent menu
		menu = new JMenu("Monitoring");
		addDisplayAction = new AddDisplayAction(this);
		menu.add(addDisplayAction);
		menu.add(new AddDisplayAction(this, "Add Parameter Table", Display.ONESHOT_TABLE));

		if (ExpCoordinator.isTestLevel(ExpCoordinator.LOG)) menu.add(new AddDisplayAction(this, "Add Logged Data Display", Display.LOGGED_DATA));

		loadFileAction = new AddFileAction(this, expCoordinator.getMainWindow());
		//if (ExpCoordinator.isTestLevel(ExpCoordinator.CMPTEST)) 
		menu.add(loadFileAction);

		JMenuItem menuItem = menu.add("Show Monitors");
		menuItem.setEnabled(false);

		menu.add(new ONL.UserAction("Set Default Polling Rate", false, false){

			public void actionPerformed(ActionEvent e)
			{	  
				final String str0 = "Enter Polling Rate";
				JFormattedTextField tf = new JFormattedTextField(new DecimalFormat("####0.###"));
				tf.setValue(new Double(defaultPolling.getSecsOnly()));
				//tf.setFocusLostBehavior(JFormattedTextField.REVERT);
				TextFieldwLabel secs = new TextFieldwLabel(tf, "secs:");//new TextFieldPlus(5, "1", true), "secs:");

				Object[] objectArray = {str0, secs};
				final String opt0 = "Enter";
				final String opt1 = "Cancel";
				Object[] options = {opt0,opt1};

				int rtn = JOptionPane.showOptionDialog(expCoordinator.getMainWindow(), 
						objectArray, 
						"Change Default Polling Rate", 
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, 
						null,
						options,
						options[0]);

				if (rtn == JOptionPane.YES_OPTION)
				{
					if (secs.getText().length() > 0)
					{
						double s = Double.parseDouble(secs.getText());
						if (s > 0) 
						{
							defaultPolling.setRate(s,0);
							ExpCoordinator.setProperty(ExpCoordinator.DEFAULT_POLLING, defaultPolling.toString());
						}
						ExpCoordinator.print(new String("Changing default polling to " + defaultPolling.toString() + " from " + secs.getValue()), 2);
						if (ExpCoordinator.isRecording())
							ExpCoordinator.recordEvent(SessionRecorder.SET_DEF_POLLING, new String[]{defaultPolling.toString()});
					}
				}
			}
		});
	}

	public boolean isQEnabled() { 
		//ExpCoordinator.print("type is " + type + " showQ = " + showQ); 
		//return ((type & showQ) > 0);
		return true;}
	public boolean isLFEnabled() { return ((type & showLF) > 0);}
	public boolean isTest() { return ((type & cTEST) > 0);}

	public void addToDesktop(JFrame jif)
	{
		//desktop.add(jif);
		//desktop.moveToFront(jif);
	}

	public static int getHistory() { return history;}
	public static void setHistory(int h) { history = h;}

	public Display addUnpublishedDisplay(int type, String title)
	{
		Display disp = null;
		switch (type)
		{
		case (Display.HISTOGRAM):
			disp = new Histogram(this, "queues", "depth(bytes)", 3, false);
		((Histogram)disp).setYUnits(Units.BYTES);
		break;
		case (Display.ONESHOT_TABLE):
			disp = new MonitorTable(this);
		break;
		default:
			disp = new MultiLineGraph(this, "time(secs)", "Mb/s", 2, 2, true);
		}
		disp.setTitle(title);
		return (addDisplay(disp));
	}

	public Display addDisplay(int type)
	{
		return (addDisplay(type, new String("Display" + getNumberDisplays()), null, null));
	}

	public Display addDisplay(int type, String title)
	{
		return (addDisplay(type, title, null, null));
	}

	public Display addDisplay(Display.ID id)
	{
		Display rtn = getDisplay(id);
		if (rtn == null) rtn = addDisplay(id.getType(), id.getTitle());
		return rtn;
	}

	public Display addDisplay(int type, String title, String xl, String yl)
	{
		Display disp = null;
		String xlabel = xl;
		String ylabel = yl;
		switch(type)
		{
		case Display.HISTOGRAM:
			if (xlabel == null) xlabel = "queues";
			if (ylabel == null) ylabel = "depth(bytes)";
			disp = new Histogram(this, xlabel, ylabel, 3, false);
			((Histogram)disp).setYUnits(Units.BYTES);
			break;
		case Display.MULTILINE:
			if (xlabel == null) xlabel = "time(secs)";
			if (ylabel == null) ylabel = "Mb/s";
			disp = new MultiLineGraph(this, xlabel, ylabel, 2, 2, true);
			break;
		case Display.LOGGED_DATA:
			if (xlabel == null) xlabel = "time(secs)";
			if (ylabel == null) ylabel = "Mb/s";
			disp = new LogFileGraph(this, xlabel, ylabel, 2, 2, true);
			break;
		case Display.ONESHOT_TABLE:
			disp = new MonitorTable(this);
			break;
		}
		disp.setTitle(title);
		return (addDisplay(disp));
	}

	public Display addDisplay(Display d)
	{
		Display disp = getDisplay(d);
		if (disp == null)
		{
			disp = d;
			disp.setLocation((displays.size() * 15),(displays.size() * 15));
			disp.setSize(new Dimension((plotWidth-20), (plotHeight-20)));
			disp.setVisible(true);
			displays.add(disp);
			if (ExpCoordinator.isRecording())
				ExpCoordinator.recordEvent(SessionRecorder.ADD_MON_DISPLAY, new String[]{disp.getTitle()});
		}
		else
			disp.setVisible(true);
		setCurrentDisplay(disp);
		fireEvent(new Event(disp,Event.ADD_DISPLAY));
		return disp;
	} 

	protected Display getDisplayAt(int index)
	{
		if (index >= 0 && index < displays.size()) return ((Display)displays.elementAt(index));
		else return null;
	}

	protected int getDisplayIndex(Display d)
	{
		int max = displays.size();
		Display elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (Display)displays.elementAt(i);
			if (elem == d) return i;
		}
		return -1;
	}
	protected Display getDisplay(Display.ID id)
	{
		int max = displays.size();
		Display elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (Display)displays.elementAt(i);
			if (elem.isEqual(id)) return elem;
		}
		return null;
	}
	protected Display getDisplay(Display d)
	{
		int max = displays.size();
		Display elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (Display)displays.elementAt(i);
			if (elem == d || elem.isEqual(d)) return elem;
		}
		return null;
	}

	public void stopMonitoring()
	{
		//ExpCoordinator.print("MonitorManager.stopMonitoring stopped " + stopped);
		//if (!stopped)
			//	{
			//int max = displays.size();
			int i = 0;
			Display elem;
			//for (i = 0; i < max; ++i)
			while(!displays.isEmpty())
			{
				elem = (Display)displays.elementAt(0);
				elem.setVisible(false);
				elem.stopMonitoring();
				displays.remove(elem);
				if (ExpCoordinator.isRecording())
					ExpCoordinator.recordEvent(SessionRecorder.DELETE_MON_DISPLAY, new String[]{elem.getTitle()});
				elem.dispose();
			}
			//displays.removeAllElements();
			timeListener.reset();
			//  stopped = true;
			//	}
	}

	public void removeDisplay(Display d)
	{
		if (d.isMonitorFunction()) paramSelector.removeParamListener((MonitorFunction)d);
		displays.remove(d);
		fireEvent(new Event(d,Event.REMOVE_DISPLAY));
		if (ExpCoordinator.isRecording())
			ExpCoordinator.recordEvent(SessionRecorder.DELETE_MON_DISPLAY, new String[]{d.getTitle()});
		if (displays.isEmpty()) timeListener.reset();
	}


	//end of interface
	public void saveToFile(java.io.File f) //save all graphs displayed to file fName
	{
		try
		{
			ONL.BaseFileWriter writer = new ONL.BaseFileWriter(f);
			saveToFile(writer);
			writer.finish();
		}
		catch (IOException e)
		{
			System.err.println("Error: writing to File " + f.getName());
		}
	}
	public void saveToFile(ONL.Writer writer) throws IOException
	{
		SpecFile.SpecWriter outfile = new SpecFile.SpecWriter(writer);
		outfile.writeLine(new String(tokPOLLING_DEFAULT + " " + defaultPolling.toString()));
		int max = displays.size();
		int i = 0;
		Display elem;
		for (i = 0; i < max; ++i)
		{
			elem = (Display)displays.elementAt(i);
			elem.getSpec().write(outfile);//saveToFile(outfile);
			//outfile.newLine(); 
		}     
		//outfile.flush();
	}

	public void write(ONL.Writer wrtr) throws IOException
	{
		wrtr.writeString(new String(tokPOLLING_DEFAULT + " " + defaultPolling.toString()));
		int i = 0;
		Display elem;
		int max = displays.size();
		for (i = 0; i < max; ++i)
		{
			elem = (Display)displays.elementAt(i);
			wrtr.writeString(tokGRAPH);
			elem.write(wrtr);
		}     
		wrtr.writeString(ONL.tokEND);
	}

	public void writeXML(XMLStreamWriter xmlWrtr)  throws XMLStreamException
	{
		xmlWrtr.writeStartElement(XMLHandler.DEFPOLLING);
		defaultPolling.writeXML(xmlWrtr);
		xmlWrtr.writeEndElement();
		int i = 0;
		Display elem;
		int max = displays.size();
		for (i = 0; i < max; ++i)
		{
			elem = (Display)displays.elementAt(i);
			elem.writeXML(xmlWrtr);
		}     
	}

	public void recordDisplays(ONL.Writer wrtr) throws IOException
	{
		int i = 0;
		Display elem;
		int max = displays.size();
		wrtr.writeInt(max);
		for (i = 0; i < max; ++i)
		{
			elem = (Display)displays.elementAt(i);
			wrtr.writeString(tokGRAPH);
			elem.recordData(wrtr);
		}     
		wrtr.writeString(ONL.tokEND);
	}

	public void loadFromFile(java.io.File f)
	{
		try
		{
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			ExperimentXML exp_xml = new ExperimentXML(ExpCoordinator.theCoordinator.getCurrentExp(), xmlReader, f, true);
			xmlReader.parse(new InputSource(new FileInputStream(f)));
			/*ONL.BaseFileReader reader = new ONL.BaseFileReader(f);
			reader.setCountAllLines(true);
			if (reader.getVersion() < 2.3)
				loadFromSpecReader(reader);
			else
				read(reader);
			reader.finish();*/
		}
		catch(FileNotFoundException e)
		{
			System.err.println("Error: File " + f.getName() + " not found.");
		}
		catch(IOException e2)
		{
			System.err.println("Error: While reading file " + f.getName());
		}
		catch(SAXException e3)
		{
			ExpCoordinator.print(new String("MonitorManager.loadFromFile SAXException " + e3.getMessage()));
		}
	}

	public void loadFromReader(ONL.Reader rdr)
	{
		try
		{
			if (rdr.getVersion() < 2.3)
				loadFromSpecReader(rdr);
			else
				read(rdr);
			rdr.finish();
		}
		catch(IOException e)
		{
			System.err.println("Error: While reading MonitorManager.loadFromReader ");
		}
	}

	public void loadRecordedDisplays(ONL.Reader rdr) throws IOException
	{
		boolean token_found = false;
		Display.ID id;
		Display tmp_display;
		String str;
		while (rdr.ready())
		{
			str = rdr.readString();
			if (str.equals(tokGRAPH))
			{
				token_found = true;
				id = new Display.ID(rdr);
				tmp_display = getDisplay(id);
				if (tmp_display != null) 
				{
					ExpCoordinator.print(new String("MonitorManager.loadRecordedDisplays Display found " + id.getTitle()));
					tmp_display.readRecordedData(rdr);
					ExpCoordinator.print("MonitorManager.loadRecordedDisplays data read");
				}
			}
			if (str.equals(ONL.tokEND) && token_found) break;
		}
	}

	public Display readDisplay(ONL.Reader rdr) throws IOException
	{
		Display.ID id = new Display.ID(rdr);
		Display rtn_display = addDisplay(id);
		if (rtn_display != null) rtn_display.read(rdr);
		return rtn_display;
	}

	public void read(ONL.Reader rdr) throws IOException
	{
		String str = rdr.readString();
		if (!str.equals(tokGRAPH_ONLY))
		{
			while (!str.startsWith(tokPOLLING_DEFAULT) && rdr.ready())
				str = rdr.readString();
			if (str.startsWith(tokPOLLING_DEFAULT))
			{
				String strarray[] = str.split(" ");
				if (strarray[0].equals(tokPOLLING_DEFAULT))
					defaultPolling.setRate(Double.parseDouble(strarray[1]), Double.parseDouble(strarray[2]));
			}
		}
		//else readDisplay(rdr);

		boolean token_found = false;
		while (rdr.ready())
		{
			str = rdr.readString();
			if (str.equals(tokGRAPH))
			{
				token_found = true;
				readDisplay(rdr);
			}
			if (str.equals(ONL.tokEND) && token_found) break;
		}
	}

	public void loadFromSpecReader(ONL.Reader rdr) throws IOException
	{
		SpecFile.SpecReader reader = new SpecFile.SpecReader(rdr);
		String line;
		boolean hasControlPanel = false;//used for log file graphs
		Display tmp_display = null;
		//first read default polling rate
		if (reader.getVersion() >= 1.3)
		{
			line = reader.readLine();
			if (!(line.equals(tokGRAPH_ONLY)))
			{
				String strarray[] = line.split(" ");
				if (strarray[0].equals(tokPOLLING_DEFAULT))
					defaultPolling.setRate(Double.parseDouble(strarray[1]), Double.parseDouble(strarray[2]));
			}
		}
		//ExpCoordinator.print("MonitorManager::loadFromFile " + f.getName());
		SpecFile spec = new SpecFile(this);
		while (reader.ready())
		{
			tmp_display = (Display)spec.getNextDisplay(reader);
			//if (tmp_display != null) addDisplay(tmp_display);
		}
		//ExpCoordinator.print("MonitorManager::loadFromFile closing reader");
	}

	public void setCurrentDisplay(Display display) 
	{ 
		if (display != null && display.isMonitorFunction()) 
		{
			//if (display.getType() == Display.ONESHOT_TABLE) 
			//expCoordinator.setCurrentMode(Mode.MONITORING_ONESHOT);
			setCurrentMFunction((MonitorFunction)display);
		}
	}
	public void setCurrentMFunction(MonitorFunction mf) 
	{ 
		if (mf != null)
			paramSelector.addParamListener(mf);
		else paramSelector.clear();
	}
	public void removeCurrentMFunction(MonitorFunction mf) { paramSelector.removeParamListener(mf);}
	public ParamSelector getParamSelector() { return paramSelector;}
	//public Display getCurrentDisplay() { return currentDisplay;}
	private int getNumberDisplays() { return (displays.size());}

	public MonitorManager.TimeListener getTimeListener() { return timeListener;}
	public double getCurrentTime() { return (timeListener.getCurrentTime());}

	public LogFileRegistry getLFRegistry() { return logFileRegistry;}
	public boolean backgroundOn() { return ((type & BACKGROUNDOFF) == 0);}
	public int getFontSize() { return fontSize;}
	public int getAxisFontSize() { return axisFontSize;}
	public int getAxisNFontSize() { return axisNFontSize;}
	public boolean isARL() { return ((type & ARL) > 0);}
	public Action getAddDisplayAction() { return addDisplayAction;}
	public Action getSaveFileAction() { return saveFileAction;}
	public Action getLoadFileAction() { return loadFileAction;}
	public ExpCoordinator getExpCoordinator() { return expCoordinator;}
	public JMenu getMenu() { return menu;}
	public JToolBar getToolBar() { return toolbar;}
	public ONLMainWindow getMainWindow() { return expCoordinator.getMainWindow();}
	public PollingRate getDefaultPolling() { return (new PollingRate(defaultPolling));}



	public void addGraphListener(MonitorManager.Listener l) { if (!eventListeners.contains(l)) eventListeners.add(l);}
	public void removeGraphListener(MonitorManager.Listener l) { if (eventListeners.contains(l)) eventListeners.remove(l);}
	public void fireEvent(MonitorManager.Event e)
	{
		int len = eventListeners.size();
		for (int i =0; i < len; ++i)
		{
			((MonitorManager.Listener)eventListeners.elementAt(i)).monitoringChanged(e);
		}
	}

	public static MonitorPlus getMonitor(MonitorDataType.Base mdt)
	{
		if (mdt == null) return null;
		MonitorPlus rtn = null;
		ExpCoordinator.print(new String("MonitorManager.getMonitor for mdt:" + mdt.getName()), TEST_LOADING);
		mdt.print(TEST_LOADING);
		if (mdt.isLiveData())
		{
			ONLComponent.ONLMonitorable m = mdt.getONLComponent().getMonitorable();
			if (m != null) 
			{
				mdt.setMonitorable(m);
				rtn = (MonitorPlus)m.addMonitor(mdt); 
				ExpCoordinator.print(new String("   add monitor to " +  mdt.getONLComponent().getLabel() + " returning " + rtn), TEST_LOADING);
			}
			else ExpCoordinator.print(new String("   no monitorable for " +  mdt.getONLComponent().getLabel()), TEST_LOADING);
		}
		if (mdt.isMonitorFunction())
		{
			rtn = new MonitorAlgFunction((MonitorDataType.MFormula)mdt, ExpCoordinator.theCoordinator.getMonitorManager());
		}
		if (mdt.isConstant())
		{
			rtn = new Constant((MonitorDataType.Constant)mdt);
		}
		return rtn;
	}

	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new XMLHandler(exp_xml, this));}
}
