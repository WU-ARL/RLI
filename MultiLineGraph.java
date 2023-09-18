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
 * File: MultiLineGraph.java
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
/**subclass of Graph. draws a line graph based on data supplied in the Vector data, a member inherited from Graph.
 * currently data may only be supplied by a subclass. LineGraph defines an inner class LGDataDisplay which does the
 * painting of the lines between points
 */

import java.util.Vector;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.stream.*;

public class MultiLineGraph extends NumberGraph implements LogScale.Scaleable
{ 
	public static final String SOLID_LINE = "solid";
	public static final String DASHED_LINE = "dashed";
	public static final int TEST_PARAMINIT = 4;
	protected boolean scrolling;

	private Vector pending;

	protected int scrollingFactor = 1;
	protected BWScrollBar yscrollBar = null;

	private MLGBoxedRange notLogBRange;
	private MLGBoxedRange logBRange = null;
	//////////////////////////////////////////////// class MultiLineGraph.MLGXMLHandler ////////////////////////////////////////////////////
	protected class MLGXMLHandler extends Graph.GXMLHandler
	{
		public MLGXMLHandler(ExperimentXML exp_xml, MultiLineGraph d)
		{
			super(exp_xml, d);
		}
		public void paramFound(String uri, Attributes attributes)
		{
			SinglePlot pb = new SinglePlot(uri, attributes, (MultiLineGraph)display);
			expXML.setContentHandler(pb.getContentHandler(expXML, (Graph)display));
		}
	}

	//////////////////////////////////////////// MultiLineGraph.DashStroke /////////////////////////////////////////////////////////
	//inner class for dashed stroke
	public static class DashStroke extends BasicStroke
	{
		public DashStroke(float width, float[] array)
		{
			super(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, array, 0);
		}
	}

	////////////////////////////////////////// MultiLineGraph.MLGSpec //////////////////////////////////////////////////////////
	//inner class MLGSpec - defines how to save(load) graph to(from) a file
	private class MLGSpec extends SpecFile.MFunctionSpec
	{
		private MultiLineGraph graph = null;
		//inner class MLGParamSpec - defines how to save(load) parameter to(from) a file this will basically save the color and/or line style of a plot
		private class MLGParamSpec extends SpecFile.MFParamSpec
		{
			private MultiLineGraph graph = null;
			public MLGParamSpec(MultiLineGraph g, MonitorManager mw)
			{
				super(mw);
				graph = g;
			}
			public Monitor getMFParameter(SpecFile.SpecReader reader) throws IOException 
			{
				//specific info here
				int level = reader.getLevel();
				String st_type = MultiLineGraph.SOLID_LINE;
				double st_scale = 1;
				int st_unittype = graph.yaxis.getUnits();
				String str = reader.readLine();
				double version = reader.getVersion();
				boolean st_inc_hdr = true;
				if (str != null && !reader.atToken())
				{
					String[] strarray = str.split(" ");
					int len = Array.getLength(strarray);
					//ExpCoordinator.print("MLGParamSpec::getParameter");
					ExpCoordinator.print(new String("MultiLineGraph.MLGSpec.MLGParamSpec.getMFParameter " + str), 5);
					for (int i = 0; i < len; ++i)
					{
						if (strarray[i].equals("STROKE"))
						{
							st_type = strarray[++i];
							ExpCoordinator.print(new String("     found STROKE " + st_type), 5);
						}
						else

						{
							if (strarray[i].equals("SCALE"))
							{
								if (version >= 1.3) st_unittype = Integer.parseInt(strarray[++i]);
								st_scale = Double.parseDouble(strarray[++i]);
								ExpCoordinator.print(new String("     found SCALE " + st_unittype +  " " + st_scale), 5);
								if (version > 1.4) st_inc_hdr = Boolean.valueOf(strarray[++i]).booleanValue();
							}
						}
					}
				}
				Monitor m = super.getMFParameter(reader);

				if (m != null && !m.getDataType().isConditional())
				{
					SinglePlot sp = new  SinglePlot((NumberData)m, graph, null);
					sp.setStroke(st_type);
					sp.setScale(st_scale);
					sp.changeUnits(st_unittype, st_inc_hdr);
					graph.addPending(sp);
					//ExpCoordinator.print("MLGspec::getMFParameter level" + level);
					reader.readToEnd(level);
				}
				return m;
			}
		}

		public MLGSpec(MultiLineGraph g)
		{
			super(g.monitorManager, g, null, true);
			graph = g;
			paramSpec = new MLGParamSpec(g, g.monitorManager);
		} 

		protected void writeParam(SpecFile.SpecWriter writer, int i) throws IOException
		{
			SpecFile.ParamSpec ps = new SpecFile.ParamSpec(graph.monitorManager);
			SinglePlot elem = ((MLGDataDisplay)graph.dataDisplay).getPlot(i);
			writer.writeToken(SpecFile.tokMFPARAM);
			writer.writeLine("STROKE " + elem.getStrokeType() + " SCALE " + elem.getUnits() + " " + elem.getScale() + " " + elem.isHeaderIncluded());
			ps.writeParam(elem.getMonitor().getDataType(), writer);
			writer.writeEndToken(SpecFile.tokMFPARAM);
		}
		protected void writeParams(SpecFile.SpecWriter writer) throws IOException
		{
			SpecFile.ParamSpec ps = new SpecFile.ParamSpec(graph.monitorManager);
			int max = graph.dataDisplay.length();
			SinglePlot elem;
			for (int i = 0; i < max; ++i)
			{
				//writeParam(i);
				elem = ((MLGDataDisplay)graph.dataDisplay).getPlot(i);
				writer.writeToken(SpecFile.tokMFPARAM);
				writer.writeLine("STROKE " + elem.getStrokeType() + " SCALE " + elem.getUnits() + " " + elem.getScale() + " " + elem.isHeaderIncluded());
				ps.writeParam(elem.getMonitor().getDataType(), writer);
				writer.writeEndToken(SpecFile.tokMFPARAM);
			}
			Conditional c = graph.dataDisplay.getConditional();
			if (c != null)
			{
				//writer.writeToken(SpecFile.tokMFPARAM);
				ps.writeParam(c.getDataType(), writer);
				//writer.writeEndToken(SpecFile.tokMFPARAM);
			}
		} 
	}



	//inner class defining data display component: draws plot
	protected class MLGBoxedRange extends Graph.BoxedRange
	{
		private boolean atEnd = true;
		public MLGBoxedRange(Graph g)
		{
			super(g,50,0,2,0);
			this.addBoxedRangeListener(new BoxedRangeAdapter()
			{
				public void changeVisible(BoxedRangeEvent e)
				{
					MLGBoxedRange tmp = (MLGBoxedRange) e.getSource();
					if ((tmp.getXExtent() + tmp.getXValue() + scrollingFactor) >= xmax)
						atEnd = true;
					else atEnd = false;
				}
			});
		}
		public void changeXBounds(BoxedRangeEvent e)
		{
			if (scrolling)
			{
				MonitorPlus tmp = (MonitorPlus) e.getSource();
				if (atEnd && (tmp.getXMax() > (xvalue + xextent)))
				{
					//ExpCoordinator.print("MLG::changeXBounds scrolling X xmax is " + tmp.getXMax() + " compared to " + (xvalue+xextent));
					scrollX(scrollingFactor);
				}
			}
			super.changeXBounds(e);
		}
		public void print(int d)
		{
			ExpCoordinator.print(new String("MLG::MLGBR xmin,xmax,ymin,ymax = " + xmin + "," + xmax + "," + ymin + "," + ymax), d);
		}
	}

	//////////////////////////////////////////// MultiLineGraph.SinglePlot //////////////////////////////////////////////////////
	//inner class incharge of drawing individual plots
	protected class SinglePlot extends Graph.ParamBase implements java.awt.event.ActionListener//, MonitorFunction.Param
	{
		public final int TEST_STROKE = 5;
		//private NumberData dataModel;
		private MultiLineGraph graph;
		private Color color = null;
		//private JTextField keyEntry;
		private JLabel keyEntry;
		private Units dataUnits = null;
		//private int dataUnits = Units.UNKNOWN;
		private LogScale.Data lscaledData = null;
		private BasicStroke stroke = null;
		private String stroke_type = SOLID_LINE;
		private float[] dashArray;
		private boolean highlighted = false;
		private double scale = 1;
		private Units.Converter uconverter = null;

		//////////////////////////////////////////////// class MultiLineGraph.SinglePlot.XMLHandler ////////////////////////////////////////
		protected class XMLHandler extends Graph.ParamBase.XMLHandler
		{
			public XMLHandler(ExperimentXML exp_xml, SinglePlot p) { super(exp_xml, p);}
			public XMLHandler(ExperimentXML exp_xml, SinglePlot p, Graph g)
			{
				super(exp_xml, p, g);
			}    
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	  
				super.startElement(uri, localName, qName, attributes);
				if (localName.equals(MonitorManager.XMLHandler.HEADER_INC))
				{
					((SinglePlot)param).changeUnits(((SinglePlot)param).getUnits(), true);
				}
			}
			public void characters(char[] ch, int start, int length)
			{
				super.characters(ch, start, length);
				ExpCoordinator.print(new String("MultiLineGraph.SinglePlot.XMLHandler.characters(" + new String(ch, start, length) + ") currentElement:" + currentElement), ExperimentXML.TEST_XML);
				if (currentElement.equals(MonitorManager.XMLHandler.SCALING_FACTOR)) 
				{
					if (mdataType != null && param.getMonitor() == null) param.setMonitor(MonitorManager.getMonitor(mdataType));
					((SinglePlot)param).setScale(Double.parseDouble(new String(ch, start, length)));
				}
				if (currentElement.equals(MonitorManager.XMLHandler.DISPLAY_UNITS)) 
				{
					if (mdataType != null && param.getMonitor() == null) param.setMonitor(MonitorManager.getMonitor(mdataType));
					((SinglePlot)param).changeUnits(Units.getType(new String(ch, start, length)), false);
				}
				if (currentElement.equals(MonitorManager.XMLHandler.STROKE)) 
				{
					ExpCoordinator.print(new String("MultiLineGraph.SinglePlot.XMLHandler.characters setting stroke(" + new String(ch, start, length) + ")"), ExperimentXML.TEST_XML);
					if (mdataType != null && param.getMonitor() == null) param.setMonitor(MonitorManager.getMonitor(mdataType));
					((SinglePlot)param).setStroke(new String(ch, start, length));
				}
				//handle params
			}
			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("MultiLineGraph.SinglePlot.XMLHandler.endElement localName:" + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.PARAM)) 
				{
					if (param.getMonitor() == null) param.setMonitor(MonitorManager.getMonitor(mdataType));
					if (graph != null) graph.addParam(param);
					else ExpCoordinator.print("    graph is null", ExperimentXML.TEST_XML);
					expXML.removeContentHandler(this);
				}
			}
		}
		/////////////////////////////////////////////// MultiLineGraph.SinglePlot.KeyMouseListener ///////////////////////////////////
		private class KeyMouseListener extends MouseAdapter
		{
			private SinglePlot plot;
			private MLGDataDisplay display; 
			public KeyMouseListener(SinglePlot p, MLGDataDisplay d)
			{
				super();
				plot = p;
				display = d;
			}
			public void mouseEntered(MouseEvent e)
			{
				//ExpCoordinator.print("KeyMouseListener::mouseEntered");
				display.setSelectedPlot(plot);
				display.revalidate();
				display.repaint();
			}
			public void mouseClicked(MouseEvent e)
			{
			        boolean tmp_edit = true;
				MonitorDataType.DescriptionDialog desc = new MonitorDataType.DescriptionDialog(plot.keyEntry);
				JCheckBox dashedButton = new JCheckBox("Dashed Line");
				dashedButton.setEnabled(tmp_edit);
				Units.EditablePanel scalePanel = plot.dataUnits.getEditablePanel(tmp_edit);
				Object[] suffix = {scalePanel,dashedButton};
				if (plot.getStrokeType().equals(DASHED_LINE)) dashedButton.setSelected(true);
				int rtn = desc.showDescription(plot.getMonitor().getDataType(), suffix, 2);
				if (rtn == JOptionPane.YES_OPTION && tmp_edit)
				{
					if (dashedButton.isSelected()) plot.setStroke(DASHED_LINE);
					else plot.setStroke(SOLID_LINE);
					//if (scale_field.getText().length() > 0) scale = Double.parseDouble(scale_field.getText());
					scalePanel.setUnits();
					monitorManager.fireEvent(new MonitorManager.Event(graph, plot, MonitorManager.Event.CHANGE_PARAM, graph.getParamIndex(plot)));  
				}
				//plot.dataModel.getDataType().showDescription(plot.keyEntry);
				JMenuItem mi = display.graph.getRmMenuItem(plot.getMonitor());
				mi.setText(plot.getLabel());
				plot.keyEntry.setText(plot.getLabel());
				plot.keyEntry.revalidate();
				//  }
			}
		}

		public SinglePlot(NumberData d, MultiLineGraph g, Color c)
		{
			super(d);
			init(g,c);
		}

		public SinglePlot(ONL.Reader rdr, MultiLineGraph g) throws IOException
		{
			super();
			graph = g;
			read(rdr);
		}

		public SinglePlot(String uri, Attributes attributes, MultiLineGraph g)
		{
			super(uri, attributes);
			graph = g;
		}

		protected void setMonitor(MonitorPlus mp) 
		{
			super.setMonitor(mp);
			if (mp == null)
				ExpCoordinator.print("MultilineGraph.setMonitor monitor null", TEST_PARAMINIT);
			if (graph != null)
				ExpCoordinator.print(new String("MultilineGraph(" + graph.getTitle() +").setMonitor " + mp.getDataType().getName()), TEST_PARAMINIT);
			else
				ExpCoordinator.print(new String("MultilineGraph.setMonitor " + mp.getDataType().getName()), TEST_PARAMINIT);

			if (mp instanceof NumberData) init(graph, null);
		}
		//protected void setDataModel(NumberData d) { init(d, graph, null);}
		//private void init(NumberData d, MultiLineGraph g, Color c)
		private void init(MultiLineGraph g, Color c)
		{
			NumberData dataModel = (NumberData)getMonitor();
			graph = g;
			ExpCoordinator.print(new String("MultilineGraph(" + graph.getTitle() +").SinglePlot(" + dataModel.getName() + ").init"), TEST_PARAMINIT);
			color = c;
			dataUnits = new Units(dataModel.getDataType().getDataUnits());
			//keyEntry = new JTextField(dataModel.getName());
			keyEntry = new JLabel(dataModel.getName());
			if (c != null) keyEntry.setForeground(c);
			keyEntry.setBackground(Color.lightGray);
			keyEntry.setFont(new Font("Dialog", Font.PLAIN, graph.monitorManager.getFontSize()));
			//keyEntry.addActionListener((java.awt.event.ActionListener)this);
			keyEntry.addMouseListener(new KeyMouseListener(this, (MLGDataDisplay)graph.dataDisplay));
			//if (stroke == null) 
				stroke = new BasicStroke(3);
			dashArray = new float[2];
			dashArray[0] = 3;
			dashArray[1] = 7;
			dataUnits.setType(graph.yaxis.getUnits());
			//uconverter = new Units.Converter(dataUnits, graph.yaxis.getUnits());
			//scale = uconverter.getScaleFactor();
		}

		private void changeUnits(int frm_u, int to_u)
		{
			changeUnits(to_u);
			//scale = uconverter.getInverse(scale);
			//uconverter = new Units.Converter(dataUnits, to_u);
			//scale = uconverter.convert(scale);
		}

		private void changeUnits(int to_u)
		{
			dataUnits.setType(to_u);
		}

		private void changeUnits(int to_u, boolean b)
		{
			dataUnits.setType(to_u);
			dataUnits.setIncludeHeader(b);
		}
		public void setHighlight(boolean on)
		{
			if (highlighted != on)
			{
				if (on) 
				{
					keyEntry.setOpaque(true);
					keyEntry.setBackground(new Color(255,250,205));
				}
				else
				{
					keyEntry.setOpaque(false);
					keyEntry.setBackground(Color.lightGray);
				}
				keyEntry.revalidate();
				highlighted = on;
				//ExpCoordinator.print("setHighlight for " + dataModel.getDataType().getName() + " to " + on);
				keyEntry.repaint();
			}
		}

		public void setStroke(String tp)
		{
			stroke_type = tp;
			ExpCoordinator.print(new String("MultiLineGraph.SinglePlot(" + getLabel() + ").setStroke " + tp), TEST_STROKE);
			if (tp.equals(SOLID_LINE)) stroke = new BasicStroke(3);
			else 
			{
				stroke = new DashStroke(3, dashArray);
			}
		}

		public void setScale(double d) { if (d != 0) dataUnits.setScaleFactor(d);}
		public double getScale() { return (dataUnits.getScaleFactor());}
		public boolean isHeaderIncluded() { return (dataUnits.isHeaderIncluded());}
		public int getUnits() { return (dataUnits.getType());}

		public String getStrokeType() { return stroke_type;}

		public void setLogScale(boolean b)
		{
			NumberData dataModel = (NumberData)getMonitor();
			if (b && lscaledData == null)
			{
				lscaledData = new LogScale.Data(dataModel, graph.yaxis.getUnits());
				dataUnits.setType(graph.yaxis.getUnits());
				lscaledData.addBoxedRangeListener(graph.logBRange);
				BoxedRangeEvent tmp_e = new BoxedRangeEvent(lscaledData, BoxedRangeEvent.ALL_CHANGED); 
				logBRange.changeXBounds(tmp_e);
				logBRange.changeYBounds(tmp_e);
				ExpCoordinator.print(new String(dataModel.getDataType().getName() + ":MLG.SinglePlot.setLogScale log scale is on"), 2);
				logBRange.print(2);
			}
			else if (!b && lscaledData != null)
			{
				lscaledData.stop();
				lscaledData.removeBoxedRangeListener(graph.logBRange);
				lscaledData = null;
				dataUnits.revertType();
				ExpCoordinator.print(new String(dataModel.getDataType().getName() + ":MLG.SinglePlot.setLogScale log scale is off"), 2);
			}
		}

		public boolean isLogScale() { return (lscaledData != null);}

		public void setColor(Color c)
		{
			color = c;
			if (c != null) keyEntry.setForeground(c);
		}

		public Color getColor()
		{
			return color;
		}
		/*
      public MonitorPlus getData()
      {
      return (getMonitor());
      }*/

		//interface MonitorManager.Display.Param
		public void write(ONL.Writer wrtr) throws IOException
		{
			wrtr.writeString(new String("STROKE " + getStrokeType() + " SCALE " + getUnits() + " " + getScale() + " " + isHeaderIncluded()));
			//((MonitorPlus)dataModel).getDataType().write(wrtr);
			super.write(wrtr);
		}
		public void writeXMLFooter(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{ 
			//this is where color goes if we want to support it
			xmlWrtr.writeStartElement(MonitorManager.XMLHandler.STROKE);
			xmlWrtr.writeCharacters(getStrokeType());
			xmlWrtr.writeEndElement();
			xmlWrtr.writeStartElement(MonitorManager.XMLHandler.SCALING_FACTOR);
			xmlWrtr.writeCharacters(String.valueOf(getScale()));
			xmlWrtr.writeEndElement();
			xmlWrtr.writeStartElement(MonitorManager.XMLHandler.DISPLAY_UNITS);
			xmlWrtr.writeCharacters(Units.getLabel(getUnits()));
			xmlWrtr.writeEndElement();
			if (isHeaderIncluded())
			{
				xmlWrtr.writeStartElement(MonitorManager.XMLHandler.HEADER_INC);
				xmlWrtr.writeEndElement();
			}
		}
		public void writeChangeable(ONL.Writer wrtr) throws IOException
		{
			wrtr.writeString(new String("STROKE " + getStrokeType() + " SCALE " + getUnits() + " " + getScale() + " " + isHeaderIncluded()));
			super.writeChangeable(wrtr);
		}
		public void read(ONL.Reader reader) throws IOException
		{
			//specific info here
			String st_type = MultiLineGraph.SOLID_LINE;
			double st_scale = 1;
			int st_unittype = graph.yaxis.getUnits();
			String str = reader.readString();
			double version = reader.getVersion();
			boolean st_inc_hdr = true;
			if (str != null)
			{
				String[] strarray = str.split(" ");
				int len = Array.getLength(strarray);
				//ExpCoordinator.print("MLGParamSpec::getParameter");
				ExpCoordinator.print(new String("MultiLineGraph.SinglePlot.read " + str), 5);
				for (int i = 0; i < len; ++i)
				{
					if (strarray[i].equals("STROKE"))
					{
						st_type = strarray[++i];
						ExpCoordinator.print(new String("     found STROKE " + st_type), 5);
					}
					else
					{
						if (strarray[i].equals("SCALE"))
						{
							st_unittype = Integer.parseInt(strarray[++i]);
							st_scale = Double.parseDouble(strarray[++i]);
							ExpCoordinator.print(new String("     found SCALE " + st_unittype +  " " + st_scale), 5);
							st_inc_hdr = Boolean.valueOf(strarray[++i]).booleanValue();
						}
					}
				}
			}
			super.read(reader);
			Monitor m = getMonitor();

			if (m != null && !m.getDataType().isConditional())
			{
				init(graph, null);
				setStroke(st_type);
				setScale(st_scale);
				changeUnits(st_unittype, st_inc_hdr);
				//ExpCoordinator.print("MLGspec::getMFParameter level" + level);
			}

			if (m == null)
				ExpCoordinator.print("MultiLineGraph.SinglePlot m is null", 6);
		}
		public void readChangeable(ONL.Reader reader) throws IOException
		{
			//specific info here
			String st_type = MultiLineGraph.SOLID_LINE;
			double st_scale = 1;
			int st_unittype = graph.yaxis.getUnits();
			String str = reader.readString();
			double version = reader.getVersion();
			boolean st_inc_hdr = true;
			if (str != null)
			{
				String[] strarray = str.split(" ");
				int len = Array.getLength(strarray);
				//ExpCoordinator.print("MLGParamSpec::getParameter");
				ExpCoordinator.print(new String("MultiLineGraph.SinglePlot.readChangeable " + str), 5);
				for (int i = 0; i < len; ++i)
				{
					if (strarray[i].equals("STROKE"))
					{
						st_type = strarray[++i];
						ExpCoordinator.print(new String("     found STROKE " + st_type), 5);
						setStroke(st_type);
					}
					else
					{
						if (strarray[i].equals("SCALE"))
						{
							if (version >= 1.3) st_unittype = Integer.parseInt(strarray[++i]);
							st_scale = Double.parseDouble(strarray[++i]);
							ExpCoordinator.print(new String("     found SCALE " + st_unittype +  " " + st_scale), 5);
							if (version > 1.4) st_inc_hdr = Boolean.valueOf(strarray[++i]).booleanValue();
							setScale(st_scale);
						}
					}
				}
			}
			changeUnits(st_unittype, st_inc_hdr);
			super.readChangeable(reader);
			String nm = getMonitor().getDataType().getName();
			JMenuItem mi = graph.getRmMenuItem(getMonitor());
			mi.setText(nm);
			keyEntry.setText(nm);
			keyEntry.revalidate();
		}
		//end interface MonitorManager.Display.Param

		//public JTextField getKeyEntry()
		//	{
		//	  return keyEntry;
		//	}
		public JLabel getKeyEntry()
		{
			return keyEntry;
		}

		public String getLabel()
		{
			return (getMonitor().getDataType().getName());
		}

		//ActionEventListener interface
		public void actionPerformed(ActionEvent e)
		{
			//not sure want to do anything maybe just note the name when saving the graph
			getMonitor().getDataType().setName(keyEntry.getText());
		}
		//end of interface

		public void setPlot(SinglePlot p)
		{
			NumberData dataModel = (NumberData)getMonitor();
			setStroke(p.getStrokeType());
			setScale(p.getScale());
			changeUnits(p.getUnits(), p.isHeaderIncluded());
			String pnm = p.getLabel();
			if (!dataModel.getName().equals(pnm))
			{
				keyEntry.setText(pnm);
				dataModel.setName(pnm);
			}
		}

		public void drawPlot(Graphics g, int units)
		{
			//coordinates relative to local component
			NumberData nd = (NumberData)getMonitor();
			if (lscaledData != null) nd = lscaledData;
			//else ExpCoordinator.print("MLG.SinglePlot.drawPlot log scale is off");
			int dm_sz = nd.getSize();
			//ExpCoordinator.print("MLG.SinglePlot.drawPlot data size is " + dm_sz);
			if (dm_sz <= 0) return;
			int[] Xs = new int[dm_sz];//only needs to be size of visible range
			int[] Ys = new  int[dm_sz];


			Graphics2D g2 = (Graphics2D) g;
			g2.setStroke(stroke);//new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, dashArray, 0));
			//set line color
			Color oldColor = g2.getColor();
			g2.setColor(color);


			//Vector vector = nd.getVector();
			NumberPair elem = null; //(NumberPair)vector.elementAt(0);
			NumberPair old_elem = null;
			boolean drawing = false;
			//elem.printOut();
			double xpix = ((NumberedAxis)xaxis).getScaleFactor();
			double xscale = ((NumberedAxis)xaxis).getTransFactor();
			double ypix = ((NumberedAxis)yaxis).getScaleFactor();
			double yscale = ((NumberedAxis)yaxis).getTransFactor();
			double x; //elem.getX();
			double y; 
			double xmn = graph.getBoxedRange().getXValue();
			double xmx = graph.getBoxedRange().getXExtent() + xmn; // ((NumberedAxis)xaxis).getMaxValue();//
			double ymn = ((NumberedAxis)yaxis).getMinValue();
			int num_pts = 0;
			boolean x_log = ((NumberedAxis)xaxis).isLogScale();
			boolean y_log = ((NumberedAxis)yaxis).isLogScale();
			for (int i = 0; i < dm_sz; ++i)
			{
				elem = nd.getElementAt(i);//nd.getElementAt(i);//iter.next();
				//elem.printOut();
				x = elem.getX();
				y = elem.getY();
				y *= dataUnits.getTotalScaleFactor();
				if (x_log) x = Math.log(x);
				if (y_log) x = Math.log(y);
				if (drawing)
				{
				    if (elem.isError()) //if there is an error skip the bad part and start redrawing later - 9/30/2020
					{
					    if (num_pts > 0)
						{
						    g2.drawPolyline(Xs, Ys, num_pts);
						    num_pts = 0;
						}
					    continue;  
					}
					if ((x >= xmn) && (x <= xmx))
					{
						//Xs[num_pts] = xaxis.getCoordinate(x); 
						Xs[num_pts] = (int)((x*xpix) + xscale);
						if (y <= ymn) Ys[num_pts] = (int)yscale;
						else
							Ys[num_pts] = (int)((y*ypix) + yscale);
						//Ys[num_pts] = (int)(yscale - (y*ypix));
						//Xs[num_pts] = (int)((x*xpix) + xscale);
						++num_pts;
					}
					if (x >= xmx) break;
				}
				else //haven't started adding to the plot yet
				{
				    if (x >= xmn && !elem.isError())
					{
						drawing = true;
						if (x > xmn && old_elem != null)
						{
							double tmp_y;
							tmp_y = old_elem.getY();
							Xs[num_pts] = (int)((old_elem.getX()*xpix) + xscale);
							//Ys[num_pts] = (int)(yscale - (y*ypix));
							Ys[num_pts] = (int)((y*ypix) + yscale);
							++num_pts;
						}
						Xs[num_pts] = (int)((x*xpix) + xscale);
						Ys[num_pts] = (int)((y*ypix) + yscale);
						//Ys[num_pts] = (int)(yscale - (y*ypix));
						++num_pts;
					}
					else old_elem = elem;
				}
			}
			if (num_pts > 0) g2.drawPolyline(Xs, Ys, num_pts);
			//have to draw label for key somewhere
			//return to original color
			g2.setColor(oldColor);
		}
		public void record(ONL.Writer wrtr) throws IOException 
		{
			NumberData dataModel = (NumberData)getMonitor();
			if (dataModel != null) dataModel.recordData(wrtr);
		}
		public void loadRecording(ONL.Reader rdr) throws IOException
		{
			NumberData dataModel = (NumberData)getMonitor();
			if (dataModel != null) dataModel.readRecordedData(rdr);
		}
		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new SinglePlot.XMLHandler(exp_xml, this));}
		public ContentHandler getContentHandler(ExperimentXML exp_xml, Graph g) { return (new SinglePlot.XMLHandler(exp_xml, this, g));}
	}//end MultiLineGraph.SinglePlot definition


	///////////////////////////////////////////////// MultiLineGraph.MLGDataDisplay //////////////////////////////////////////////////
	protected class MLGDataDisplay extends Graph.DataDisplay implements LogScale.Scaleable
	{    
		private Vector plots;
		private Vector colors;
		private JPanel key = null;
		private boolean keyDrawn = false;
		private SinglePlot selectedPlot = null;
		private boolean logscaled = false;



		public MLGDataDisplay(MultiLineGraph g)
		{
			super(g);
			plots = new Vector();
			key = new JPanel();
			key.setLayout(new BoxLayout(key, BoxLayout.Y_AXIS));
			key.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

			colors = new Vector(20);
			colors.add(Color.black);
			colors.add(Color.blue);
			colors.add(Color.green.darker());
			colors.add(Color.red);
			colors.add(Color.magenta);
			colors.add(new Color(165, 42, 42)); //brown
			colors.add(new Color(0, 180, 175));//green
			colors.add(new Color(255, 140, 0));//dark orange
			colors.add(new Color(0, 250, 154));//medium spring green
			colors.add(new Color(225,120,221));//light purple
			colors.add(new Color(218, 165, 32));//goldenrod
			colors.add(new Color(25, 25, 112));//midnight blue
			colors.add(new Color(155, 48,255));//purple1
			colors.add(new Color(176, 48, 96));//maroon
			colors.add(new Color(139, 102, 139));//plum4
			colors.add(new Color(176, 224, 230));//powder blue
			colors.add(new Color(244,164,96));//sandy brown
			colors.add(Color.pink);
			colors.add(Color.orange);
			colors.add(Color.cyan);


		}

		//start of LogScale.Scaleable interface
		public void setLogScale(boolean b)
		{
			logscaled = b;
			int max = plots.size();
			for (int i = 0; i < max; ++i)
			{
				((SinglePlot)plots.elementAt(i)).setLogScale(b);
			}
		}

		public boolean isLogScale() { return logscaled;}
		//end of LogScale.Scaleable interface

		private void changeUnits(int frm_u, int to_u)
		{
			int max = plots.size();
			for (int i = 0; i < max; ++i)
			{
				((SinglePlot)plots.elementAt(i)).changeUnits(frm_u, to_u);
			}
		}

		public MultiLineGraph.SinglePlot getPlot(int ndx) { return ((MultiLineGraph.SinglePlot)getParam(ndx));}
		public MonitorFunction.Param getParam(int ndx)
		{
			if (plots.size() > ndx)
				return (MonitorFunction.Param) plots.elementAt(ndx);
			else return null;
		}

		public void setSelectedPlot(MultiLineGraph.SinglePlot p) { selectedPlot = p;}

		public void addData(MonitorPlus d)
		{
			Color color;
			if (!colors.isEmpty()) color = (Color)colors.remove(0);
			else color = Color.black;
			SinglePlot tmp_plot = removePending(d);
			if (tmp_plot == null) tmp_plot = new SinglePlot((NumberData)d, (MultiLineGraph)graph, color);
			else 
			{
				if (tmp_plot.getColor() == null) 
					tmp_plot.setColor(color);
			}
			tmp_plot.setLogScale(logscaled);
			ExpCoordinator.print(new String("MLG::addData " + ((NumberData)d).getName()), 5);
			double pSecs  = d.getDataType().getPollingRate().getSecs();
			if (pSecs > scrollingFactor) //set scrolling factor in case the period is larger than the current scrollingFactor
			{
				if (d.getDataType().getPollingRate().getSecsOnly() > pSecs)
					scrollingFactor = (int)pSecs + 1;
				else scrollingFactor = (int)pSecs;
			}
			plots.add(tmp_plot);
			setSelectedPlot(tmp_plot);
			if (key != null) 
			{
				//ExpCoordinator.print("key.addData");
				key.add(tmp_plot.getKeyEntry());
			}	//this is for testing
			key.revalidate();
			key.repaint();
			repaint();
		}

		public void removeData(MonitorPlus d)
		{
			int i = 0;
			NumberData nd = (NumberData) d;
			removePending(d);
			for ( i = 0; i < plots.size(); ++i)
			{
				SinglePlot elem = (SinglePlot)plots.elementAt(i);
				if ((NumberData)elem.getMonitor() == nd)
				{
					plots.remove(elem);
					if (elem == selectedPlot) setSelectedPlot(null);
					key.remove(elem.getKeyEntry());
					colors.add(elem.getColor());
					key.repaint();
					ExpCoordinator.print(new String("MLG::removeData removing " + elem.getLabel()), 2);
					break;
				}
			} 
			repaint();
		}
		public void removeAllElements() { plots.removeAllElements();}
		public int length(){ return (plots.size());}//returns the number of data structures
		public MonitorPlus getData(int ndx)
		{
			if (plots.size() > ndx)
				return (((SinglePlot)plots.elementAt(ndx)).getMonitor());
			else return null;
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			int i = 0;
			SinglePlot elem;
			int max = plots.size();
			//ExpCoordinator.print("MLGDisplay::paintComponent");
			for ( i = 0; i < max; ++i)
			{
				elem = (SinglePlot)plots.elementAt(i);
				if (elem != selectedPlot) elem.drawPlot(g, graph.yaxis.getUnits());
				//ExpCoordinator.print("    plotting " + i);
			}
			if (selectedPlot != null) selectedPlot.drawPlot(g, graph.yaxis.getUnits());
			key.revalidate();
			key.repaint();
			//if (zoom.isDrawing() || zoom.isWaiting()) zoom.drawBox(g);
		}
		//ConditionListener interface
		public void changeCondition(ConditionalEvent e)
		{
			int i = 0;
			SinglePlot elem;
			int max = plots.size();
			Conditional c = (Conditional)e.getSource();
			//ExpCoordinator.print("MLGDisplay::paintComponent");
			for ( i = 0; i < max; ++i)
			{
				elem = (SinglePlot)plots.elementAt(i);
				elem.setHighlight(c.isTrue(elem.getMonitor()));
			}
		}
		//end ConditionListener interface
		protected void reset()
		{
			int i = 0;
			SinglePlot elem;
			int max = plots.size();
			//ExpCoordinator.print("MLGDisplay::paintComponent");
			for ( i = 0; i < max; ++i)
			{
				elem = (SinglePlot)plots.elementAt(i);
				elem.setHighlight(false);
			}
		}

		public JPanel getKey() 
		{ 
			return key;
		}

		public void updateVars()
		{
			//ExpCoordinator.print("MLGDataDisplay:updateVars");
			repaint();
		}
	}
	//end inner class MLGDataDisplay definition

	//Main class constructor
	public MultiLineGraph(MonitorManager mw, String xlbl, String ylbl, int scaleX, int scaleY, boolean scroll)
	{
		super(mw, false, 1, 0,  MonitorManager.Display.MULTILINE);

		setSpec(new MLGSpec(this));

		//log scale option --- out for now (it eats up major cpu)
		LogScale.LSMenuItem menuItem = new LogScale.LSMenuItem(this);
		addToViewMenu(menuItem);

		pending = new Vector();

		scrolling = scroll;

		notLogBRange = new MLGBoxedRange(this);
		logBRange = new MLGBoxedRange(this);
		setBoxedRange(notLogBRange);

		MLGDataDisplay dDisplay = new MLGDataDisplay(this);

		//zoom stuff not sure if this should be moved up into Graph
		setZoomable(true);//disable zoom for now

		//super.setLayout((new TimeAxis(this, true, scaleX)), (new NumberedAxis(this, false, scaleY)), dDisplay, xlbl, ylbl);
		super.setLayout((new NumberedAxis(this, true, scaleX)), (new NumberedAxis(this, false, scaleY)), dDisplay, xlbl, ylbl);

		setXUnits(Units.PERSEC);
		//setYUnits(Units.MBITS | Units.PERSEC);
		yaxis.setUnits(Units.MBITS | Units.PERSEC);
		((NumberedAxis)yaxis).setResizeable(true);
		((NumberedAxis)xaxis).setResizeable(true);


		//now put in key
		//get layout manager from content pane & insert time display into the layout
		Container contentPane = getContentPane();
		GridBagLayout gridbag = (GridBagLayout)contentPane.getLayout();
		GridBagConstraints c = new GridBagConstraints();
		contentPane.setLayout(gridbag);
		JPanel key = dDisplay.getKey();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = offsetx + 3;
		c.gridy = offsety;
		gridbag.setConstraints(key, c);
		contentPane.add(key);

		//add scrollbar for x axis
		xscrollBar = new BWScrollBar(BWScrollBar.HORIZONTAL, 100, boxedRange);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = offsety + 3;
		c.gridx = offsetx + 2;
		c.weighty = 0;//.5;
		c.weightx = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(0,0,5,0);
		c.ipady = 2;
		c.ipadx = 0;

		gridbag.setConstraints(xscrollBar, c);
		contentPane.add(xscrollBar);

		//add scrollbar for y axis
		yscrollBar = new BWScrollBar(BWScrollBar.VERTICAL, 100, boxedRange);
		c.fill = GridBagConstraints.VERTICAL;
		c.gridy = 0;
		c.gridx = 0;
		c.weighty = 0;//.5;
		c.weightx = 0;
		//c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(5,5,0,0);
		c.ipady = 0;
		c.ipadx = 2;

		gridbag.setConstraints(yscrollBar, c);
		contentPane.add(yscrollBar);


		//testing conditional
		//addConditional(new TestConditional(.8));
	}


	//overrides NumberGraph.addToRmMenu

	protected void addToRmMenu(MonitorPlus d)
	{
		int mx = dataDisplay.length();
		SinglePlot elem;
		for (int i = 0; i < mx; ++i)
		{
			elem = ((MLGDataDisplay)dataDisplay).getPlot(i);
			if (d == elem.getMonitor())
			{
				addToRmMenu(d, elem.getColor());
				break;
			}
		}
	}

	//overrides NumberGraph.removeData

	public void removeData(MonitorPlus dm)
	{
		super.removeData(dm);
		//reset bounds
		MonitorPlus d = dataDisplay.getData(0);
		if (d != null)
		{
			ExpCoordinator.print(new String("MLG.removeData setting bounds from " + dm.getDataType().getName()), 5);

			boxedRange.setBoundary(d.getXMax(), d.getXMin(), d.getYMax(), d.getYMin());
			//ExpCoordinator.print("to " + d.getDataType().getName() + " (" + xmax + "," + ymax + "," + xmin + "," + ymin + ")");
			redraw();
		}
	} 

	public void setLogScale(boolean isLog) 
	{
		ExpCoordinator.print(new String("MLG::setLogScale " + isLog), 2);
		if (isLog)
		{
			logBRange.print(2);
			logBRange.setXVisible(boxedRange.getXValue(), boxedRange.getXExtent());
			logBRange.setXBoundary(boxedRange.getXMax(), boxedRange.getXMin()); 

			((LogScale.Scaleable)dataDisplay).setLogScale(isLog);
			boxedRange = logBRange;
			boxedRange.addBoxedRangeListener(new BoxedRangeAdapter()
			{
				public void changeVisible(BoxedRangeEvent e)
				{
					redraw();
				}
			});
			if (xscrollBar != null) xscrollBar.setBoxedRange(boxedRange);
			ExpCoordinator.print("MLG::setLogScale setting to logBRange", 5);
			logBRange.print(5);
		}
		else
		{
			((LogScale.Scaleable)dataDisplay).setLogScale(isLog);
			boxedRange = notLogBRange;
		}
		redraw();
	}

	protected SinglePlot removePending(MonitorPlus d)
	{
		int max = pending.size();
		NumberData nd = (NumberData) d;
		for ( int i = 0; i < max; ++i)
		{
			SinglePlot elem = (SinglePlot)pending.elementAt(i);
			if ((NumberData)elem.getMonitor() == nd)
			{
				pending.remove(elem);
				//ExpCoordinator.print("MLG::removePending removing " + elem.getLabel());
				return elem;
			}
		} 
		return null;
	}
	protected void addPending(SinglePlot sp)
	{ 
		if (!pending.contains(sp)) pending.add(sp);
	}

	public void stopMonitoring()
	{
		Conditional c = dataDisplay.getConditional();
		if (c != null)
		{
			c.removeMonitorFunction(this);
			ExpCoordinator.print("MLG::stopMonitoring found conditional", 5);
		}
		super.stopMonitoring();
	}
	public void setYUnits(int u)
	{
		int units = 0;
		if (yaxis != null)
		{
			units = yaxis.getUnits();
		}
		super.setYUnits(u);
		if (u != units) ((MLGDataDisplay)dataDisplay).changeUnits(units, u);
	}
	public MonitorFunction.Param readParam(ONL.Reader rdr) throws IOException
	{
		SinglePlot tmp = new SinglePlot(rdr, this);
		SinglePlot rtn = (SinglePlot)dataDisplay.getParam(tmp.getMonitor().getDataType());
		if (rtn == null) 
		{
			rtn = tmp;
			rtn.getMonitor().addMonitorFunction(this);
			addPending(rtn);
		}
		else rtn.setPlot(tmp);
		return rtn;
	}
	public MonitorFunction.Param addParam(Graph.ParamBase pb) 
	{
		ExpCoordinator.print("MultiLineGraph.addParam(SinglePlot)", TEST_PARAMINIT);
		SinglePlot tmp = (SinglePlot)pb;
		SinglePlot rtn = (SinglePlot)dataDisplay.getParam(tmp.getMonitor().getDataType());
		if (rtn == null) 
		{
			rtn = tmp;
			rtn.getMonitor().addMonitorFunction(this);
			addPending(rtn);
		}
		else rtn.setPlot(tmp);
		return rtn;
	}
	private void addNewPlot(SinglePlot p)
	{
		SinglePlot plot = (SinglePlot)dataDisplay.getParam(p.getMonitor().getDataType());
		if (plot == null) 
		{
			plot = p;
			plot.getMonitor().addMonitorFunction(this);
			addPending(plot);
		}
		else plot.setPlot(p);
	}
	public ContentHandler getXMLHandler(ExperimentXML exp_xml) { return (new MLGXMLHandler(exp_xml, this));}
}
