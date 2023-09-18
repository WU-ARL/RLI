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
 * File: Graph.java
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
/**generic Graph class with separate components representing the axes and the data display. 
 * these are supplied by the subclasses. each component is in charge of drawing itself based on 
 *data represented as a Vector & the current size of the graph. Graph takes care of layout using the 
 * GridBagLayout manager. it is a subclass of JInternalFrame to allow a Graph to be repositioned or
 * iconified within a bounding frame. subclasses are responsible for setting these attributes.
 */


import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.stream.*;

public abstract class Graph extends MonitorManager.Display implements Zoom.Zoomable, MenuFileAction.Saveable//, MonitorFunction 
{
	public static final String tokPARAM = "PARAM";
	//spec
	private SpecFile.MFunctionSpec spec = null;

	//axes text labels
	protected JLabel xlabel;
	protected Axis.Label ylabel;

	//axes
	protected Axis xaxis = null;
	protected Axis yaxis = null;

	//data display
	protected DataDisplay dataDisplay;
	protected JMenu menu = null;
	protected JMenu viewMenu = null;
	protected JMenu paramMenu = null;


	//menu and listener for removing parameters from the graph
	private MenuRemoveParameterListener rmParamListener;
	private JMenu rmParamMenu = null; 

	//file class for logging a graph and later replaying (when it's fixed)
	private LogFileSaver logfile = null;
	private boolean isLoggable = true;

	//start variables for paused and stopped monitoring or ongoing display
	protected boolean paused = false;
	private boolean stopped = false;

	//
	private double seenXMax = 0;
	private double seenYMax = 0;

	//scrollbar
	protected BWScrollBar xscrollBar = null;

	protected BoxedRange boxedRange;

	//offsets based on grid cells this allows inheriting classes to move basic layout around in the grid
	protected int offsetx; 
	protected int offsety;

	//Zoom object draws box and handles user input
	protected Zoom zoom = null;

	//has an axis been resized 
	private boolean resized = false;

	//is boxedRange initializd
	protected boolean initialized = false;

	private static int DEFAULT_INITIAL_VECTOR = 10;

	//////////////////////////////////////////////// class Graph.GXMLHandler ////////////////////////////////////////////////////
	protected static class GXMLHandler extends MonitorManager.Display.DXMLHandler
	{
		public GXMLHandler(ExperimentXML exp_xml, Graph d)
		{
			super(exp_xml, d);
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			super.startElement(uri, localName, qName, attributes);
			if (localName.equals(ExperimentXML.PARAM)) paramFound(uri, attributes);
		}
		public void paramFound(String uri, Attributes attributes)
		{
			ParamBase pb = new ParamBase(uri, attributes);
			expXML.setContentHandler(pb.getContentHandler(expXML, (Graph)display));
		}
		public void characters(char[] ch, int start, int length)
		{
			//super.characters(ch, start, length);
			if (currentElement.equals(MonitorManager.XMLHandler.XLABEL)) ((Graph)display).setXlabel(new String(ch, start, length));
			if (currentElement.equals(MonitorManager.XMLHandler.YLABEL)) 
			{
				String tmp_lbl = new String(ch, start, length);
				if (tmp_lbl.equals("Mbits/s")) tmp_lbl = Units.getLabel(Units.MBITS|Units.PERSEC);
				((Graph)display).setYlabel(tmp_lbl);
			}
			if (currentElement.equals(MonitorManager.XMLHandler.YUNITS)) ((Graph)display).setYUnits(Units.getType(new String(ch, start, length)));
			//handle params
		}
	}


	//implements BoxedRangeModel and BoxedRangeListener will change its boundaries based on the data streams
	//it listens to which also implement BoxedRangeModel
	public static class BoxedRange extends BoxedRangeDefault implements BoxedRangeListener
	{
		protected Graph graph;
		protected String name = "";

		public BoxedRange(Graph g) { graph = g;}
		public BoxedRange(Graph g, double xmx, double xmn, double ymx, double ymn) 
		{ 
			super(xmx, xmn, ymx, ymn);
			//if (yextent <= 0.5) 
			//  {
			//    setYExtent(0.5);
			//    setYMax(ymin + 0.5);
			//  }
			//if (xextent <= 0.5) 
			//  {
			//    setXExtent(0.5);
			//   setXMax(xmin + 0.5);
			//  }
			graph = g;
		}

		//add scrolling capabilities
		public void scrollX(double val) { setXValue(xvalue + val);}
		public void scrollY(double val) { setYValue(yvalue + val);}

		//name access
		public String getName()
		{ return name;}

		public void setName(String nm)
		{ name = nm;}


		//listener interface only registered on MonitorPlus
		public void changeVisible(BoxedRangeEvent e){}
		public void changeBoundaries(BoxedRangeEvent e){}
		public void changeXVisible(BoxedRangeEvent e){}
		public void changeYVisible(BoxedRangeEvent e){}
		public void changeXBounds(BoxedRangeEvent e)
		{
			MonitorPlus tmp = (MonitorPlus) e.getSource();
			boolean min = false;
			boolean max = false;
			double newmin = xmin;
			double newmax = xmax;

			//System.out.println("Graph::changeXBounds");
			if ((e.getType() & BoxedRangeEvent.XMIN_CHANGED) != 0)
			{
				newmin = graph.recalculateXMin(xmin);
				if (xmin != newmin) min = true;
			}
			if (tmp.getXMax() > xmax)
			{
				max = true;
				newmax = tmp.getXMax();
			}

			if (min || max)
			{
				//if (newmin < 0);
				//{
				//  System.out.println("changing xmax old = " + String.valueOf(xmax) + " new = " + String.valueOf(newmax));
				//if (min)  System.out.println("Graph::changeXBounds changing xmin old = " + String.valueOf(xmin) + " new = " + String.valueOf(newmin));
				// }
				setXBoundary(newmax, newmin);
				//if (!graph.isZoomed()) setXVisible(newmin, (newmax - newmin));
			}
			if (tmp.getXMax() > graph.seenXMax) graph.seenXMax = tmp.getXMax();
			graph.redraw();
		}
		public void changeYBounds(BoxedRangeEvent e)
		{	
			MonitorPlus tmp = (MonitorPlus) e.getSource();
			boolean min = false;
			boolean max = false;
			double newmin = ymin;
			double newmax = ymax;
			double tmpmin = tmp.getYMin();
			double tmpmax = tmp.getYMax();

			int units = tmp.getDataType().getDataUnits();
			int tmpunits = units;
			if (graph.yaxis != null) units = graph.yaxis.getUnits();


			//System.out.println("Graph.changeYBounds tmpmin,tmpmax = " + tmpmin + "," + tmpmax);
			//System.out.println("     tmpunits,units = " + Units.getLabel(tmpunits) + "," + Units.getLabel(units));
			if (units != tmpunits)
			{
				Units.Converter uc = new Units.Converter(tmpunits, units);
				tmpmin = uc.convert(tmpmin);
				tmpmax = uc.convert(tmpmax);
			}

			if (tmpmin < ymin)
			{
				min = true;
				//System.out.println("changing ymin old = " + String.valueOf(newmin) + " new = " + String.valueOf( tmpmin ));
				newmin = tmpmin;
			}
			if (tmpmax > ymax)
			{
				max = true;
				//System.out.println("changing ymax old = " + String.valueOf(newmax) + " new = " + String.valueOf( tmpmax ));
				newmax = tmpmax;
			}
			if (min || max)
			{
				setYBoundary(newmax, newmin);
				if (!graph.isResized())// && ((newmax - newmin) > 0.5)) 
				{
					//if (min) System.out.println("change in min");
					//if (max) System.out.println("change in max");
					setYVisible(newmin, (newmax - newmin));
				}
			}
			if (tmpmax > graph.seenYMax) graph.seenYMax = tmpmax;
			graph.redraw();
		}

		//  public void fireEvent(BoxedRangeEvent e)
		//{
		//super.fireEvent(e);
		//if (xmin < 0) System.out.println("Graph.BoxedRange::fireEvent " + xmin);
		//}
	}
	//end inner class BoxedRange

	//inner class listener starts logging process for graph -- menu item "Log To File ..."
	private class SaveToLogListener implements MenuFileAction.Saveable
	{
		private Graph graph;
		public SaveToLogListener(Graph g)
		{
			graph = g;
		}
		public void saveToFile(java.io.File f)
		{
			graph.logToFile(f.getPath(), f); 
		}
		public void loadFromFile(java.io.File f){}
	}
	//end inner class SaveToLogListener
	private class GSpec extends SpecFile.MFunctionSpec
	{
		private Graph graph = null;

		public GSpec(Graph g)
		{
			this(g, new SpecFile.MFParamSpec(g.monitorManager));
			graph = g;
		}
		public GSpec(Graph g, SpecFile.MFParamSpec ps)
		{
			super(g.monitorManager, g, ps, true);
		}

		protected void writeParam(SpecFile.SpecWriter writer, int i) throws IOException
		{
			paramSpec.writeParam(graph.dataDisplay.getData(i), writer);
		}
		protected void writeParams(SpecFile.SpecWriter writer) throws IOException
		{
			int max = graph.dataDisplay.length();
			int i;
			for (i = 0; i < max; ++i)
			{
				writeParam(writer, i);
			}
			Conditional c = graph.dataDisplay.getConditional();
			if (c != null)
				paramSpec.writeParam(c, writer);
		}
	} 

	//inner class saves graph and parameter specs to file
	private class LogFileSaver
	{
		private Graph graph = null;
		private String basepath = null;
		private int paramIndex = 0;
		private SpecFile.SpecWriter outfile = null;
		private final String LOGFILE_SUFFIX = ".lf";

		public LogFileSaver(Graph g, String bp, File f) throws IOException
		{
			graph = g;
			outfile = new SpecFile.SpecWriter(new ONL.BaseFileWriter(f));
			basepath = bp;
		} 
		public void saveParams() throws IOException
		{
			int max = graph.dataDisplay.length();
			int i;
			for (i = 0; i < max; ++i)
			{
				addParam(graph.dataDisplay.getData(i));
			}
		}
		public void writeGraphInfo() throws IOException
		{
			outfile.writeToken(SpecFile.tokGRAPH);
			outfile.writeLine(new String(graph.getTitle()));
			outfile.writeLine(new String(graph.xlabel.getText() + " " + graph.ylabel.getText()));
			outfile.writeLine(new String(MonitorManager.Display.LOGGED_DATA + " " + graph.yaxis.getUnits()) );
		}
		public void addParam(MonitorPlus nd) throws IOException
		{
			if (basepath == null) return;
			outfile.writeToken(SpecFile.tokPARAM);
			File paramf;
			if (!nd.isLogging())
			{
				paramf = new File(new String(basepath + (++paramIndex) + LOGFILE_SUFFIX)); 
				nd.setLogFile(paramf);
			}
			else paramf = nd.getLogFile();
			MonitorDataType.LogFile mdt = new MonitorDataType.LogFile(paramf.getName(), nd.getDataType());
			mdt.setReplayType(MonitorDataType.LogFile.EXACT_REPLAY);
			mdt.saveToFile(outfile);

			outfile.writeEndToken(SpecFile.tokPARAM);
		}  
		public void endSpec() throws IOException
		{
			outfile.writeEndToken(SpecFile.tokGRAPH);
			outfile.getONLWriter().finish();
		}
	}
	//end inner class LogFileSaver



	//inner class implementing listener for "Add Parameter" menu option
	protected static class MenuAddParameterListener extends MenuDragMouseAdapter
	{
		private Graph display;
		private MonitorManager monitorManager;
		private MenuFileAction mfListener = null;
		private boolean isFormula = false;
		public MenuAddParameterListener(Graph d, MonitorManager mwin)
		{
			this(d, mwin, false);
		}
		public MenuAddParameterListener(Graph d, MonitorManager mwin, boolean is_formula)
		{
			display = d;
			monitorManager = mwin;
			isFormula = is_formula;
		}
		public MenuAddParameterListener(Graph d, MonitorManager mwin, MenuFileAction mfl)
		{
			this(d, mwin, false);
			mfListener = mfl;
		}

		public void select()
		{
			//System.out.println("Graph.MenuAddParameterListener::select");
			if (!isFormula)
			{
				monitorManager.setCurrentDisplay(display);
				if (mfListener != null) mfListener.actionPerformed(new ActionEvent(this, 0, ""));
			}
			else
			{
				//System.out.println("  is formula");
				AddFormulaWindow formula = new AddFormulaWindow(display, monitorManager);
				formula.setLocation(display.getLocation());
				formula.setSize(new Dimension(190, 200));
				formula.setVisible(true);
				monitorManager.addToDesktop(formula);
			}
			//if (display.getType() == MonitorManager.Display.ONESHOT_TABLE)
			//monitorManager.getExpCoordinator().setCurrentMode(Mode.MONITORING_ONESHOT);
			//else
			//monitorManager.getExpCoordinator().setCurrentMode(Mode.MONITORING);
		}
	}
	//end inner class MenuAddParameterListener

	//inner class implementing listener for "Remove Parameter" menu option
	private class MenuRemoveParameterListener extends MenuDragMouseAdapter
	{
		private Graph display;
		public MenuRemoveParameterListener(Graph d)
		{
			display = d;
		}

		public void menuDragMouseReleased(MenuDragMouseEvent e)
		{
			display.removeData((Graph.DataMenuItem)e.getSource());
		}
		public void mouseReleased(MouseEvent e)
		{
			display.removeData((Graph.DataMenuItem)e.getSource());
		}
	}
	//end inner class MenuRemoveParameterListener

	//inner class menu item with data used in the rm Param Menu
	private class DataMenuItem extends JMenuItem
	{
		private MonitorPlus data = null;
		public DataMenuItem(MonitorPlus d)
		{
			super(d.getDataType().getName());
			data = d;
		}
		public MonitorPlus getData() { return data;}
	}//end inner class DataMenuItem 

	///////////////////////////////////////////// Graph.ParamBase //////////////////////////////////////////////////////
	public static class ParamBase implements MonitorFunction.Param
	{
		private MonitorPlus monitor = null;

		//////////////////////////////// Graph.ParamBase.XMLHandler //////////////////////////////////////////////////////
		public static class XMLHandler extends MonitorXMLHandler
		{
			protected ParamBase param = null;
			protected Graph graph = null;
			public XMLHandler(ExperimentXML exp_xml, ParamBase p) { this(exp_xml, p, null);}
			public XMLHandler(ExperimentXML exp_xml, ParamBase p, Graph g) 
			{ 
				super(exp_xml);
				param = p;
				graph = g;
			}
			public void characters(char[] ch, int start, int length){}
			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("Graph.ParamBase.XMLHandler.endElement localName:" + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.PARAM)) 
				{
					param.setMonitor(MonitorManager.getMonitor(mdataType));
					if (graph != null) graph.addParam(param);
					expXML.removeContentHandler(this);
				}
			}
		}

		public ParamBase() {}
		public ParamBase(String uri, Attributes attributes) {}
		public ParamBase(MonitorPlus mp) { monitor = mp;}
		public ParamBase(ONL.Reader rdr) throws IOException { read(rdr);}
		public MonitorPlus getMonitor() { return monitor;}
		protected void setMonitor(MonitorPlus mp) { monitor = mp;}
		public void write(ONL.Writer wrtr) throws IOException { monitor.getDataType().write(wrtr);}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{ 
			xmlWrtr.writeStartElement(MonitorManager.XMLHandler.PARAM);
			writeXMLHeader(xmlWrtr);
			monitor.getDataType().writeXML(xmlWrtr);
			writeXMLFooter(xmlWrtr);
			xmlWrtr.writeEndElement();
			/*
	try
	{
	xmlWrtr.writeStartElement(MonitorManager.XMLHandler.PARAMSPEC);
	ExperimentXML.ONLStringWriter onl_strwrtr = new ExperimentXML.ONLStringWriter();
	monitor.getDataType().write(onl_strwrtr);
	xmlWrtr.writeProcessingInstruction(MonitorManager.XMLHandler.PARAM, onl_strwrtr.getBuffer().toString());
	xmlWrtr.writeEndElement();
	}
	catch (java.io.IOException e)
	{
	ExpCoordinator.print(e.getMessage());
	}
			 */
		}
		public void writeXMLHeader(XMLStreamWriter xmlWrtr) throws XMLStreamException {}
		public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException {}
		public void read(ONL.Reader rdr) throws IOException
		{
			monitor = MonitorManager.getMonitor(MonitorDataType.Base.read(rdr));
			if (monitor instanceof MonitorAlgFunction)
			{
				//((MonitorAlgFunction)m).read(reader);
				((MonitorAlgFunction)monitor).start();
			}
		}
		public void writeChangeable(ONL.Writer wrtr) throws IOException { monitor.getDataType().writeChangeable(wrtr);}
		public void readChangeable(ONL.Reader rdr) throws IOException { monitor.getDataType().readChangeable(rdr);}
		public void record(ONL.Writer wrtr) throws IOException 
		{
			if (monitor != null) monitor.recordData(wrtr);
		}
		public void loadRecording(ONL.Reader rdr) throws IOException
		{
			if (monitor != null) monitor.readRecordedData(rdr);
		}
		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new ParamBase.XMLHandler(exp_xml, this));}
		public ContentHandler getContentHandler(ExperimentXML exp_xml, Graph g) { return (new ParamBase.XMLHandler(exp_xml, this, g));}
	}  

	//inner class for DataDisplay
	public static abstract class DataDisplay extends JPanel implements ConditionListener
	{
		protected Graph graph = null;
		protected Conditional conditional = null;
		public DataDisplay()
		{
			super();
			setDoubleBuffered(true);
		}
		public DataDisplay(Graph g) 
		{
			super();
			setDoubleBuffered(true);
			graph = g;
			if (graph.monitorManager.backgroundOn()) setBackground(Color.white);
		}
		public abstract void addData(MonitorPlus d);
		public abstract void removeData(MonitorPlus d);
		public abstract void removeAllElements();
		public abstract int length() ;//returns the number of data structures
		public MonitorPlus getData(int ndx)
		{
			return ((MonitorPlus)getParam(ndx).getMonitor());
		}
		public MonitorFunction.Param getParam(int ndx)
		{
			return (new ParamBase(getData(ndx)));
		}
		public MonitorFunction.Param getParam(MonitorDataType.Base mdt)
		{
			int max = length();
			MonitorFunction.Param elem;
			for (int i = 0; i < max; ++i)
			{
				elem = getParam(i);
				if (elem.getMonitor().getDataType().isEqual(mdt)) return elem;
			}
			return null;
		}
		public int getParamIndex(MonitorFunction.Param p)
		{
			int max = length();
			MonitorFunction.Param elem;
			for (int i = 0; i < max; ++i)
			{
				elem = getParam(i);
				if (elem == p) return i;
			}
			return -1;
		}
		public void setGraph(Graph g) { graph = g;}
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (graph != null)
			{
				if (graph.isZoomable())
				{
					graph.drawZoom(g);
				}
			}
		}
		//ConditionListener interface
		public void changeCondition(ConditionalEvent e){}
		//end ConditionListener interface
		public void addConditional(Conditional c)
		{
			conditional = c;
			c.addConditionListener(this);
			if (c.isPassive())
			{
				PassiveConditional pc = (PassiveConditional)c;
				int max = length();
				for (int i = 0; i < max; ++i)
				{
					getData(i).addBoxedRangeListener(pc);
				}
			}
		}
		public void removeConditional(Conditional c)
		{
			if (c == conditional) conditional = null;
			c.removeConditionListener(this);
			if (c.isPassive())
			{
				int max = length();
				for (int i = 0; i < max; ++i)
				{
					getData(i).removeBoxedRangeListener((BoxedRangeListener)c);
				}
			}
			reset();
		}
		public Conditional getConditional() { return conditional;}
		protected void reset(){} //if any changes were made because of condition, return to normal
	}
	//end inner class DataDisplay


	//start of main class Graph  
	//constructor meant to be called by subclasses
	protected Graph(MonitorManager bwd, int offx, int offy, int t)//descendants need to set boxedRange in constructor 
	{
		this(bwd, offx, offy, null, t, true);
	}
	protected Graph(MonitorManager bwd, int offx, int offy, int t, boolean is_loggable)//descendants need to set boxedRange in constructor 
	{
		this(bwd, offx, offy, null, t, is_loggable);
	}

	protected Graph(MonitorManager bwd, int offx, int offy, Graph.BoxedRange b, int t) { this(bwd, offx, offy, b, t, true);}
	protected Graph(MonitorManager bwd, int offx, int offy, Graph.BoxedRange b, int t, boolean is_loggable)
	{
		super(bwd, t);      

		addWindowListener(new WindowAdapter(){
			public void windowClosing (WindowEvent e)
			{
				ExpCoordinator.print("Graph.windowClosing");
				stopMonitoring();
			}
		});

		MenuDragMouseAdapter mdma;

		isLoggable = is_loggable;

		menu = new JMenu("Options");
		addToMenuBar(menu);
		menu.setPopupMenuVisible(true);

		JMenuItem menuItem;

		//take out for demo
		//create file menu
		menu.add(new MenuFileAction(this, this, "Save To File ..."));

		//create log file menu
		ExpCoordinator expCoordinator = ExpCoordinator.theCoordinator;
		if (isLoggable && ExpCoordinator.isTestLevel(ExpCoordinator.LOG))
		{
			menu.add(new MenuFileAction(new SaveToLogListener(this), monitorManager.getMainWindow(), "Log To File ..."));
		}

		menu.add(new MonitorManager.Display.TitleAction(this));


		paramMenu = new JMenu("Parameter");
		addToMenuBar(paramMenu);
		paramMenu.setPopupMenuVisible(true);

		menuItem = paramMenu.add("Add Parameter");
		mdma = new MenuAddParameterListener(this, monitorManager);
		menuItem.addMenuDragMouseListener(mdma);
		menuItem.addMouseListener(mdma);

		rmParamMenu = new JMenu("RemoveParameter");
		paramMenu.add(rmParamMenu);
		rmParamListener = new MenuRemoveParameterListener(this);


		//add formula
		menuItem = paramMenu.add("Add Formula");
		mdma = new MenuAddParameterListener(this, monitorManager, true);
		menuItem.addMenuDragMouseListener(mdma);
		menuItem.addMouseListener(mdma);

		initializeGraph(offx, offy, b);
	}

	protected void finalize() throws Throwable
	{
		super.finalize();
		stopMonitoring();
	}

	//allows outside objects to access the max and min values of the x and y axes
	public BoxedRangeModel getBoxedRange() { return boxedRange;}

	protected void setBoxedRange(Graph.BoxedRange b)
	{
		b.setXValue(monitorManager.getCurrentTime());
		b.setXMin(monitorManager.getCurrentTime());
		if (boxedRange != null) b.initializeFrom(boxedRange);
		boxedRange = b;
		boxedRange.addBoxedRangeListener(new BoxedRangeAdapter()
		{
			public void changeVisible(BoxedRangeEvent e)
			{
				redraw();
			}
		});
		if (xscrollBar != null) xscrollBar.setBoxedRange(boxedRange);
	}

	public void redraw()
	{
		//set a flag to update dimensions when the paint is finally done by Graphics thread
		if (xaxis != null) 
		{
			xaxis.setUpdateDimensions(true);
			xaxis.repaint();
		}
		if (yaxis != null) 
		{
			yaxis.setUpdateDimensions(true);
			yaxis.repaint();
		}
		dataDisplay.repaint();
		//drawZoom(dataDisplay.getGraphics());
	}


	public void drawZoom(Graphics g)
	{
		if (isZoomable() && zoom.isDisplayed()) 
		{
			if (g != null) {
				//try {
				//g.setColor(Color.black);
				//g.setXORMode(dataDisplay.getBackground());
				zoom.draw(g);
				//finally {
				// g.dispose();
				// }
			}
		}
	}

	protected void initializeGraph(int offx, int offy, Graph.BoxedRange b)
	{
		offsetx = offx;
		offsety = offy;

		//add Graph as a Listener for changes in data
		if (b != null) setBoxedRange(b); 

		//add Component Listener to call redraw when Graph is resized
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) { redraw();}
		});
	}

	public void setZoomable(boolean z)
	{
		if (z)
		{
			if (zoom == null) 
			{
				if (dataDisplay != null) zoom = new Zoom(this, dataDisplay); //this);
				else zoom = new Zoom(this);

				JMenuItem menuItem = new JMenuItem("Zoom In From Selection");
				addToViewMenu(menuItem);
				MenuDragMouseAdapter mdma = new MenuDragMouseAdapter() {
					public void menuDragMouseReleased(MenuDragMouseEvent e)
					{
						zoom.setEnabled(true);
					}
					public void mouseReleased(MouseEvent e)
					{
						zoom.setEnabled(true);
					}
				};
				menuItem.addMenuDragMouseListener(mdma);
				menuItem.addMouseListener(mdma);
			}
		}
		else if (zoom != null)
		{
			zoom.cancelZoom();
			zoom = null;
		}
	}

	public boolean isZoomable()
	{
		if (zoom != null) return true;
		else return false;
	}

	//this method sets layout to grid bag and places the axes, data display and labels in the right
	//position relative to offsets specified in the constructor
	protected void setLayout(Axis x, Axis y, DataDisplay dDisplay, String xlbl, String ylbl)
	{
		xaxis = x;
		yaxis = y;
		dataDisplay = dDisplay;

		setBackground(xaxis.getBackground());
		if (zoom != null) zoom.setComponent(dataDisplay);
		//get content pane and set layout to grid bag
		Container contentPane = getContentPane();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		contentPane.setLayout(gridbag);
		//setBackground(bg);
		//setForeground(fg);	
		//create and setup y axis label and add to layout
		ylabel = new Axis.Label(ylbl, false, monitorManager.getAxisFontSize());
		c.gridx = offsetx;
		c.gridy = offsety;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.ipadx = 30;
		c.insets = new Insets(0,5,0,0);
		c.ipady = 35;
		gridbag.setConstraints(ylabel, c);
		contentPane.add(ylabel);


		//setup y axis and add to layout
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx = offsetx + 1;
		c.weightx = 0;
		c.weighty = 1;
		//c.ipadx = 40;
		c.ipady = 0;
		c.insets = new Insets(5,5,0,0);
		gridbag.setConstraints(yaxis, c);
		contentPane.add(yaxis);
		c.ipadx = 0;

		//setup data display  and add to layout
		c.fill = GridBagConstraints.BOTH;
		c.gridx = offsetx + 2;
		c.weightx = 1;
		c.weighty = 1;
		c.ipadx = 0;
		gridbag.setConstraints(dataDisplay, c);
		contentPane.add(dataDisplay);

		//setup x axis and add to layout
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = offsety + 1;
		c.weighty = 0;
		c.ipady = 20;
		c.insets = new Insets(0,0,0,0);
		gridbag.setConstraints(xaxis, c);
		contentPane.add(xaxis); 

		//create and setup x axis label and add to layout
		xlabel = new JLabel(xlbl);
		xlabel.setFont(new Font("Dialog", Font.BOLD, monitorManager.getAxisFontSize()));
		c.fill = GridBagConstraints.NONE;
		c.gridy = offsety + 2;
		c.weightx = 0;
		c.weighty = 0;
		c.ipady = 0;
		c.insets = new Insets(2,0,2,0);
		c.anchor = GridBagConstraints.CENTER;
		gridbag.setConstraints(xlabel, c);
		contentPane.add(xlabel);
	}


	public double recalculateXMin(double cur_xmin)
	{
		if (dataDisplay.length() > 0)
		{
			MonitorPlus elem = dataDisplay.getData(0);
			double tmp_xmin = elem.getXMin();
			for (int i = 1; i < dataDisplay.length(); ++i)
			{
				elem = dataDisplay.getData(i);
				if (elem.getXMin() < tmp_xmin) tmp_xmin = elem.getXMin();
			}
			return tmp_xmin;
		}
		else return cur_xmin;
	}


	public boolean isZoomed() { return ((zoom != null) && (zoom.isZoomed()));}

	//Zoomable interface 
	public void zoomIn(Zoom.State st)
	{ 
		double xval = boxedRange.getXValue(); 
		double xxtnt = boxedRange.getXExtent();
		double yval = boxedRange.getYValue();
		double yxtnt = boxedRange.getYExtent();
		boxedRange.setVisible((xval + (st.xval_fact * xxtnt)),
				(st.xextent_fact * xxtnt),
				(yval + (st.yval_fact * yxtnt)),
				(st.yextent_fact * yxtnt));
		//System.out.println("Graph::zoomIn original ( " + xval + ", " + xxtnt + ", " + yval + ", " + yxtnt + ")");
		//System.out.println("     zoom factors ( " + st.xval_fact + ", " + st.xextent_fact + ", " + st.yval_fact + ", " + st.yextent_fact + ")");
		//System.out.println("     new ( " + boxedRange.getXValue() + ", " + boxedRange.getXExtent() + ", " + boxedRange.getYValue() + ", " + boxedRange.getYExtent() + ")");
	}

	public void zoomOut(Zoom.State st)
	{ 
		double xxtnt = (boxedRange.getXExtent())/st.xextent_fact;
		double yxtnt = (boxedRange.getYExtent())/st.yextent_fact;
		double xval = boxedRange.getXValue() - (st.xval_fact * xxtnt);
		double yval = boxedRange.getYValue() - (st.yval_fact * yxtnt);

		//System.out.println("Graph::zoomOut new ( " + xval + ", " + xxtnt + ", " + yval + ", " + yxtnt + ")");
		//System.out.println("     zoom factors ( " + st.xval_fact + ", " + st.xextent_fact + ", " + st.yval_fact + ", " + st.yextent_fact + ")");

		if (xval < boxedRange.getXMin())
		{
			//yval = yval + (boxedRange.getXMin() - xval);
			xval = boxedRange.getXMin();
		}

		if (!isResized() && ((yval + yxtnt) < boxedRange.getYMax())) //if we're not zoomed in from the original make sure we resize to the max
		{
			yxtnt = boxedRange.getYMax() - yval;
		}

		boxedRange.setVisible(xval, xxtnt, yval, yxtnt);
	}

	public Rectangle getDisplayBounds()
	{
		if (xaxis == null || yaxis == null)
		{
			return new Rectangle(0,0,0,0);
		}
		else
		{
			return new Rectangle(xaxis.getMinCoordinate(),//left
					yaxis.getMaxCoordinate(),//top
					xaxis.length(),
					yaxis.length());
		}
	}
	//end Zoomable interface

	public void addData(MonitorPlus dm)
	{
		if (!initialized)
		{
			//System.out.println("Graph::graphData");
			//dm.print();
			//boxedRange.setBoundary(dm.getXMax(), dm.getXMin(), dm.getYMax(), dm.getYMin());
			initialized = true;
		}
		dm.addBoxedRangeListener(boxedRange);
		BoxedRangeEvent tmp_e = new BoxedRangeEvent(dm, 0); 
		boxedRange.changeXBounds(tmp_e);
		boxedRange.changeYBounds(tmp_e);
		dataDisplay.addData(dm);
		Conditional c = dataDisplay.getConditional();
		if (c != null && c.isPassive())
		{
			((PassiveConditional)c).addData(dm);
		}
		if (paused) dm.pause();
		redraw();
		if (logfile != null)
		{
			try 
			{
				logfile.addParam(dm);
			}
			catch (IOException e)
			{
				System.err.println("Error: writing file -- Graph::addData");
			}
		}

		if (ExpCoordinator.isRecording())
			ExpCoordinator.recordMonitorEvent(SessionRecorder.ADD_MONITOR, getTitle(), dm.getDataType());
		monitorManager.fireEvent(new MonitorManager.Event(this, getParam(dm.getDataType()), MonitorManager.Event.ADD_PARAM));
		addToRmMenu(dm);
	}

	protected void addToRmMenu(MonitorPlus d)
	{
		addToRmMenu(d, Color.black);
	}

	protected void addToRmMenu(MonitorPlus d, Color c)
	{
		//DataMenuItem dmi = (DataMenuItem)getRmMenuItem(d);
		//if (dmi == null)
		//{
		DataMenuItem dmi = new DataMenuItem(d);
		dmi.setForeground(c);
		dmi.setFont(new Font("Dialog", Font.PLAIN, 9));
		dmi.addMenuDragMouseListener(rmParamListener);
		dmi.addMouseListener(rmParamListener);
		rmParamMenu.add(dmi);
		//System.out.println("Graph.addData ");
		//}
	} 

	private void removeData(DataMenuItem d)
	{
		d.getData().removeMonitorFunction(this);
		ExpCoordinator.print(new String("Graph::removeData " + d.getData().getDataType().getName()));
		rmParamMenu.remove(d);
		dataDisplay.removeData(d.getData());
	} 

	public void removeData(MonitorPlus dm)
	{
		int mx = dataDisplay.length();
		DataMenuItem elem = (DataMenuItem)getRmMenuItem(dm);
		if (elem != null)
		{
			if (logfile != null) dm.stopLogging();
			Conditional c = dataDisplay.getConditional();
			if (c != null && c.isPassive())
			{
				((PassiveConditional)c).removeData(dm);
			}

			dm.removeBoxedRangeListener(boxedRange);
			if (ExpCoordinator.isRecording())
				ExpCoordinator.recordMonitorEvent(SessionRecorder.DELETE_MONITOR, getTitle(), dm.getDataType());
			monitorManager.fireEvent(new MonitorManager.Event(this, getParam(dm.getDataType()), MonitorManager.Event.REMOVE_PARAM));
			removeData(elem);
			return;
		}
	} 

	protected JMenuItem getRmMenuItem(MonitorPlus dm)
	{
		int mx = dataDisplay.length();
		DataMenuItem elem;
		for (int i = 0; i < mx; ++i)
		{
			elem = (DataMenuItem)rmParamMenu.getItem(i);
			if (elem.getData() == dm)
			{
				return elem;
			}
		}
		return null;
	} 

	public void endLogging()
	{
		if (logfile != null)
		{
			try 
			{
				int max = dataDisplay.length();
				int i = 0;
				MonitorPlus elem;
				for (i = 0; i < max; ++i)
				{
					elem = dataDisplay.getData(i);
					elem.stopLogging();
				}
				logfile.endSpec();	    
			}
			catch (IOException e)
			{
				System.err.println("Error: file -- Graph::endLogging");
			}
		} 
	}

	public void setPause(boolean v)
	{
		if (paused != v)
		{
			paused = v;
			int max = dataDisplay.length();
			int i = 0;
			MonitorPlus elem;
			for (i = 0; i < max; ++i)
			{
				elem = dataDisplay.getData(i);
				if (paused) elem.pause();
				else elem.restart();
			}
		}
	}



	public void realignCurves()
	{
		int max = dataDisplay.length();
		int i = 0;
		MonitorPlus elem;
		for (i = 0; i < max; ++i)
		{
			elem = dataDisplay.getData(i);
			elem.resetTime();
		}
	}

	public void stopMonitoring()
	{
		//if (switchDescriptor.getHandle() > -1 && !stopped)
		ExpCoordinator.print(new String("Graph::stopMonitoring " + stopped));
		if (!stopped)
		{
			if (logfile != null) endLogging();
			int max = dataDisplay.length();
			int i = 0;
			MonitorPlus elem;
			for (i = 0; i < max; ++i)
			{
				elem = dataDisplay.getData(i);
				elem.removeMonitorFunction(this);
			}
			stopped = true;
			dataDisplay.removeAllElements();
			monitorManager.removeDisplay(this);
		}
	}

	public void saveToFile(java.io.File f) //this graph saved by itself
	{
		try
		{
			ExpCoordinator.print(new String("Graph::saveToFile " + f.getName()));
			//SpecFile.SpecWriter outfile = new SpecFile.SpecWriter(new ONL.BaseFileWriter(f));
			ONL.BaseFileWriter outfile = new ONL.BaseFileWriter(f);
			outfile.writeString(MonitorManager.tokGRAPH_ONLY);
			outfile.writeString(MonitorManager.tokGRAPH);
			write(outfile);
			outfile.finish();
		}
		catch (IOException e)
		{
			System.err.println("Error: File " + f.getName() + " cannot be opened.");
		}
	}

	public void saveToFile(SpecFile.SpecWriter outfile) //for saving to a file that might contain more than one graph 
	{
		//problem reimplement to support the fact that monitors may be from different switches
		try
		{
			getSpec().write(outfile);
		}
		catch (IOException e)
		{
			System.err.println("Error: Graph::saveToFile File cannot be written.");
		}
	}

	public void logToFile(String basePath, java.io.File f)
	{
		if (logfile == null)
		{
			//save the specification to a file and setup log files for each parameter being monitored   
			try
			{
				logfile = new LogFileSaver(this, basePath, f);
				logfile.writeGraphInfo();
				logfile.saveParams();
			}
			catch (IOException e)
			{
				System.err.println("Error: Graph::logToFile File cannot be written.");
			}
		}
	}

	public void loadFromFile(java.io.File f){} //this is part of the interface but not an option from the menu may use later for logfiles


	public JMenuItem addToOptionMenu(String mnm) //create and add menu item w/ label mnm. returns the new menu item
	{
		if (menu != null)
			return (menu.add(mnm));
		else
			return null;
	}


	public JMenuItem addToOptionMenu(JMenuItem mi) //create and add menu item w/ label mnm. returns the new menu item
	{
		if (menu != null)
		{
			menu.add(mi);
			return mi;
		}
		else
			return null;
	}


	//set units for x and y axis used for conversion
	public void setXUnits(int u) { if (xaxis != null) xaxis.setUnits(u);}
	public void setYUnits(int u) 
	{ 
		if (yaxis != null) 
		{
			int units = yaxis.getUnits();
			if ((units != u) && (boxedRange != null))
			{
				Units.Converter uc = new Units.Converter(units, u);
				double tmpmax = uc.convert(boxedRange.getYMax());
				double tmpmin = uc.convert(boxedRange.getYMin());
				double tmpextent = uc.convert(boxedRange.getYExtent());
				double tmpval = uc.convert(boxedRange.getYValue());
				//System.out.println("Graph::setYUnits to units " + u + " new boundaries ");
				//System.out.println(tmpmin);
				//System.out.println(tmpval);
				//System.out.println(tmpextent);
				//System.out.println(tmpmax);

				boxedRange.setYVisible(tmpval, tmpextent);
				boxedRange.setYBoundary(tmpmax, tmpmin);
			}
			yaxis.setUnits(u);
			String str = ylabel.getText();
			if (str == null || str.isEmpty() || Units.getLabel(units).equals(str)) str = Units.getLabel(u);
			/*
	    String tmp_lbl = Units.getLabel(u);
	    if (!str.contains(tmp_lbl))
	    {
	    String[] strarray = str.split("'('|')'");
	    if (strarray[0].length() > 0)
	    str = new String(strarray[0] + "(" + tmp_lbl + ")");
	    else str = tmp_lbl;
	    }
			 */
			ylabel.setText(str);
			ylabel.repaint();
			redraw();
		}
	}

	//add menu to menu bar
	public void addToMenuBar(JMenu m)
	{
		JMenuBar mb = getJMenuBar();
		if (mb == null)
		{
			mb = new JMenuBar();
			setJMenuBar(mb);
		}
		mb.add(m);
	}
	//add menuItem to View Menu start it if it hasn't already been started
	public void addToViewMenu(JMenuItem m)
	{
		if (viewMenu == null)
		{
			viewMenu = new JMenu(new String("View"));
			addToMenuBar(viewMenu);
		}
		viewMenu.add(m);
	}

	//for resizing axes
	public void resizeXVal(double val)
	{
		resized = true;
		//System.out.println("Graph::resizeXVal val = " + val + " min = " + boxedRange.getXValue() + " extent = " + boxedRange.getXExtent());
		if ((val + boxedRange.getXValue()) > seenXMax) boxedRange.setXMax(val + boxedRange.getXValue());
		boxedRange.setXExtent(val);
	}
	public void resizeYVal(double val)
	{
		resized = true;
		//System.out.println("Graph::resizeYVal val = " + val + " min = " + boxedRange.getYValue() + " extent = " + boxedRange.getYExtent());
		if ((val + boxedRange.getYValue()) > seenYMax) boxedRange.setYMax(val + boxedRange.getYValue());
		boxedRange.setYExtent(val);
	}
	public boolean isResized() { return (resized || isZoomed());}
	public void resetYView()
	{
		double ymx = 0;
		double ymn = 0;
		double tmp_ymx = 0;
		double tmp_ymn = 0;
		int max = dataDisplay.length();
		int i = 0;
		MonitorPlus elem;
		int units = yaxis.getUnits();

		int tmp_units = units;
		Units.Converter uc = new Units.Converter();
		if (max > 0)
		{
			elem = dataDisplay.getData(0);
			tmp_units = elem.getDataType().getDataUnits();
			if (units != tmp_units)
			{
				uc.setConversion(tmp_units, units);
				ymn = uc.convert(elem.getYMin());
				ymx = uc.convert(elem.getYMax());
			}
			else 
			{	  
				ymn = elem.getYMin();
				ymx = elem.getYMax();
			}
			int tmp2_units;
			for (i = 1; i < max; ++i)
			{
				tmp2_units = tmp_units;
				elem = dataDisplay.getData(i);  
				tmp_units = elem.getDataType().getDataUnits();
				if (units != tmp_units)
				{
					if (tmp2_units != tmp_units) uc.setConversion(tmp_units, units);
					tmp_ymn = uc.convert(elem.getYMin());
					tmp_ymx = uc.convert(elem.getYMax());
				}
				else 
				{	  
					tmp_ymn = elem.getYMin();
					tmp_ymx = elem.getYMax();
				}
				if (ymx < tmp_ymx) ymx = tmp_ymx;
				if (ymn > tmp_ymn) ymn = tmp_ymn;
			}
			boxedRange.setYMax(ymx);
			boxedRange.setYMin(ymn);
			boxedRange.setYValue(ymn);
			boxedRange.setYExtent(ymx - ymn);
		}
		resized = false;
	}
	public void resetView()
	{
		if (isZoomed())
		{
			zoom.cancelZoom();
		}
		resetYView();
	}
	public void addConditional(Conditional c)
	{
		dataDisplay.addConditional(c);
	}
	public void removeConditional(Conditional c)
	{
		dataDisplay.removeConditional(c);
	}
	public SpecFile.MFunctionSpec getSpec()
	{
		if (spec == null) spec = new GSpec(this);
		return spec;
	}
	protected void setSpec(SpecFile.MFunctionSpec sp) { spec = sp;}
	//MonitorManager.Display
	public boolean isEqual(MonitorManager.Display d)
	{
		if (super.isEqual(d) && (d instanceof Graph))
		{
			Graph g = (Graph)d;
			if (xlabel.getText().equals(g.xlabel.getText()) && ylabel.getText().equals(g.ylabel.getText()))
				return true;
		}
		return false;
	}

	public void setYlabel(String s) { if (ylabel != null) ylabel.setText(s);}
	public String getYlabel() 
	{
		if (ylabel != null) return (ylabel.getText());
		return "";
	}

	public void setXlabel(String s) { if (xlabel != null) xlabel.setText(s);}
	public String getXlabel() 
	{
		if (xlabel != null) return (xlabel.getText());
		return "";
	}
	public void writeChangeable(ONL.Writer wrtr) throws IOException
	{
		super.writeChangeable(wrtr);
		wrtr.writeString(new String(xlabel.getText() + " " + ylabel.getText() + " " + yaxis.getUnits()));
	}
	public void write(ONL.Writer wrtr) throws IOException
	{
		super.write(wrtr);
		wrtr.writeString(new String(xlabel.getText() + " " + ylabel.getText() + " " + yaxis.getUnits()));
		wrtr.writeInt(dataDisplay.length());
		int max = dataDisplay.length();
		for (int i = 0; i < max; ++i)
		{
			wrtr.writeString(tokPARAM);
			writeParam(i, wrtr);
		}
	}

	public void writeXMLParams(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		super.writeXMLParams(xmlWrtr);
		//write xaxis label
		xmlWrtr.writeStartElement(MonitorManager.XMLHandler.XLABEL);
		xmlWrtr.writeCharacters(xlabel.getText());
		xmlWrtr.writeEndElement();//end xaxis label
		//write yaxis label
		xmlWrtr.writeStartElement(MonitorManager.XMLHandler.YLABEL);
		xmlWrtr.writeCharacters(ylabel.getText());
		xmlWrtr.writeEndElement();//end yaxis label
		//write yaxis units
		xmlWrtr.writeStartElement(MonitorManager.XMLHandler.YUNITS);
		xmlWrtr.writeCharacters(Units.getLabel(yaxis.getUnits()));
		xmlWrtr.writeEndElement();//end yaxis units
		int max = dataDisplay.length();
		for (int i = 0; i < max; ++i)
		{
			dataDisplay.getParam(i).writeXML(xmlWrtr);
		}
	}
	public void readChangeable(ONL.Reader rdr) throws IOException
	{
		super.readChangeable(rdr);
		String[] strarray = rdr.readString().split(" ");
		setXlabel(strarray[0]);
		setYlabel(strarray[1]);
		setYUnits(Integer.parseInt(strarray[2]));
	}
	public void read(ONL.Reader rdr) throws IOException
	{
		super.read(rdr);
		String[] strarray = rdr.readString().split(" ");
		setXlabel(strarray[0]);
		setYlabel(strarray[1]);
		setYUnits(Integer.parseInt(strarray[2]));
		int numParams = rdr.readInt();
		String tmp_str;
		int i = 0;
		while (i < numParams)
		{
			tmp_str = rdr.readString();
			if (tmp_str.equals(tokPARAM))
			{
				++i;
				readParam(rdr);
			}
		}
	}
	public MonitorFunction.Param addParam(ParamBase pb)
	{
		MonitorFunction.Param rtn = (MonitorFunction.Param)dataDisplay.getParam(pb.getMonitor().getDataType());
		if (rtn == null) 
		{
			rtn = pb;
			rtn.getMonitor().addMonitorFunction(this);
		}
		return rtn;
	}
	public MonitorFunction.Param readParam(ONL.Reader rdr) throws IOException
	{
		MonitorFunction.Param tmp = new Graph.ParamBase(rdr);
		MonitorFunction.Param rtn = (MonitorFunction.Param)dataDisplay.getParam(tmp.getMonitor().getDataType());
		if (rtn == null) 
		{
			rtn = tmp;
			rtn.getMonitor().addMonitorFunction(this);
		}
		return rtn;
	}
	public void writeParam(int i, ONL.Writer wrtr) throws IOException
	{
		if ( i >= 0 ) dataDisplay.getParam(i).write(wrtr);
	}

	public MonitorFunction.Param getParam(int ndx) { return (dataDisplay.getParam(ndx));}
	public int getParamIndex(MonitorFunction.Param p) { return (dataDisplay.getParamIndex(p));}
	public MonitorFunction.Param getParam(MonitorDataType.Base mdt) { return (dataDisplay.getParam(mdt));}
	public int getNumParams() { return (dataDisplay.length());}
	public ContentHandler getXMLHandler(ExperimentXML exp_xml) { return (new GXMLHandler(exp_xml, this));}
}
