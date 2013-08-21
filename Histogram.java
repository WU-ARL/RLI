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
 * File: Histogram.java
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
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class Histogram extends NumberGraph 
{ 
  protected boolean scrolling;
  protected boolean isXAxis = true;

  protected int scrollingFactor = 1;
  private Graph subgraph = null;
  private SubGIconListener sgIconListener = null;
  private SubGMouseListener sgMouseListener = null;
  //Time display not used for all graphs
  protected TimeDisplay timeDisplay = null;


  protected class HDataDisplay extends Graph.DataDisplay
  { 
    protected boolean isXAxis = true;
    protected Vector plots;
    private boolean logscaled = false;

    public HDataDisplay(Histogram g)
      {
	super(g);
	plots = new Vector();
	isXAxis = g.isXAxis;
      }

    //start of LogScale.Scaleable interface
    public void setLogScale(boolean b) { logscaled = b;}

    public boolean isLogScale() { return logscaled;}
    //end of LogScale.Scaleable interface

    public MonitorPlus getPlot(int ndx)
      {
	if (plots.size() > ndx)
	  return (MonitorPlus) plots.elementAt(ndx);
	else return null;
      }

    public void addData(MonitorPlus d)
      {
	plots.add(d);//appends element
	repaint();
      }

    public void removeData(MonitorPlus nd)
      {
	int i = 0;
	for ( i = 0; i < plots.size(); ++i)
	  {
	    MonitorPlus elem = (MonitorPlus)plots.elementAt(i);
	    if (elem == nd)
	      {
		plots.remove(elem);
		System.out.println("Histogram::removeData removing " + elem.getDataType().getName());
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
	  return ((MonitorPlus)plots.elementAt(ndx));
	else return null;
      }
    public void paintComponent(Graphics g)
      {
	super.paintComponent(g); 
	    
	int r_width = 0; //width of data rectangle
	int minCoordinate = 0; //coordinate of minimum value on the axis displaying values
	
	//coordinates relative to local component
	int x = 0; 
	int y = 0;
	int i = 0;

	Color oldColor = g.getColor();
	g.setColor(Color.green.darker());
	
	int units;
	//compute params based on whether data rectangles are based on the x or y axis
	if (isXAxis) 
	  {
	    r_width = (int)xaxis.getSpaceBetweenDivs();
	    minCoordinate = yaxis.getMinCoordinate();
	    x = xaxis.getMinCoordinate();
	    units = yaxis.getUnits();
	  }
	else
	  {
	    r_width = (int)yaxis.getSpaceBetweenDivs();
	    minCoordinate = xaxis.getMinCoordinate();
	    y = yaxis.getMinCoordinate();
	    units = xaxis.getUnits();
	  }
	
	NumberPair elem;
	NumberData data;
	int tmp_units;
	double val;
	Units.Converter uconverter = new Units.Converter();
	//for each data entry draw the rectangle;
	for (i = 0; i < plots.size(); ++i)
	  {
	    data = (NumberData)plots.elementAt(i);
	    elem = (NumberPair)data.getLastElement();
	    tmp_units = data.getDataType().getDataUnits();
	    if (elem != null)
	      {
		if (tmp_units == units) val = elem.getY();
		else
		  {
		    uconverter.setConversion(tmp_units, units);
		    val = uconverter.convert(elem.getY());
		  }

		if (isXAxis)
		  {
		    y = yaxis.getCoordinate(val);
		    g.fill3DRect(x, y, r_width, (minCoordinate - y), true);//this is because origin at top, left
		    x += r_width;
		  }
		else
		  {
		    x = xaxis.getCoordinate(val);
		    g.fill3DRect(x, y, r_width, (x - minCoordinate), true);
		    y += r_width;
		  }
	      }
	  }
	//System.out.println("HDisplay::paintComponent");
	  g.setColor(oldColor);
      }
  
    public void updateVars()
      {
	//System.out.println("HDataDisplay:updateVars");
	repaint();
      }
  }
  //end inner class HDataDisplay definition
  
  //Main class constructor
  public Histogram(MonitorManager mw, String xlbl, String ylbl, int scaleY, boolean scroll)
    {
      this(mw, xlbl, ylbl, scaleY, scroll, true);
    }
  public Histogram(MonitorManager mw, String xlbl, String ylbl, int scale, boolean scroll, boolean isX)
    {
      super(mw, false, MonitorManager.Display.HISTOGRAM);

      isXAxis = isX;
      scrolling = false;
     
      setBoxedRange(new Graph.BoxedRange(this));

      HDataDisplay dDisplay = new HDataDisplay(this);
      
      //zoom stuff not sure if this should be moved up into Graph
      setZoomable(false);//don't zoom for now

      if (isXAxis)
	{
	  super.setLayout((new IDAxis(this, true)), (new NumberedAxis(this, false, scale)), dDisplay, xlbl, ylbl);
	  //setYUnits(Units.MBITS);
	  yaxis.setUnits(Units.MBITS);
	  ((NumberedAxis)yaxis).setResizeable(true);
	}
      else
	{
	  super.setLayout((new NumberedAxis(this, false, scale)), (new IDAxis(this, true)), dDisplay, xlbl, ylbl);
	  setXUnits(Units.MBITS);
	  ((NumberedAxis)xaxis).setResizeable(true);
	}

      //now put in time
      timeDisplay = new TimeDisplay();
      boxedRange.addBoxedRangeListener(timeDisplay);
      //get layout manager from content pane & insert time display into the layout
      //get layout manager from content pane & insert time display into the layout
      Container contentPane = getContentPane();
      GridBagLayout gridbag = (GridBagLayout)contentPane.getLayout();
      GridBagConstraints c = new GridBagConstraints();
      contentPane.setLayout(gridbag);

      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = offsetx + 3;
      c.gridy = 0;
      c.ipady = 30;
      c.ipadx = 50;
      gridbag.setConstraints(timeDisplay, c);
      contentPane.add(timeDisplay);
    }


  //overrides Graph.addData

  public void addData(MonitorPlus dm)
    {
      //add to xaxis
      if (isXAxis) ((IDAxis)xaxis).addID(dm.getDataType().getName());
      else ((IDAxis)yaxis).addID(dm.getDataType().getName());
      super.addData(dm);
    } 

  //overrides Graph.removeData
  
  public void removeData(MonitorPlus dm)
    {
      super.removeData(dm);
      //remove from xaxis
      if (isXAxis) 
	{
	  ((IDAxis)xaxis).removeID(dm.getDataType().getName());
	  xaxis.revalidate();
	}
      else 
	{
	  ((IDAxis)yaxis).removeID(dm.getDataType().getName());
	  yaxis.revalidate();
	}
      if (subgraph != null) subgraph.removeData(dm);
    } 

  //overrides NumberGraph.java
  public void stopMonitoring()
    {
      super.stopMonitoring();
      if (subgraph != null) 
	{
	  subgraph.stopMonitoring();
	}
    }
  public void addToSubgraph(int index)
    {
      if (subgraph == null)
	{
	  if (isXAxis)
	    {
	      subgraph =  new MultiLineGraph(monitorManager, "time(secs)", ylabel.getText(), 0, 0, true);
	      if (yaxis.getUnits() != Units.BYTES) System.out.println("y units are not bytes");
	      subgraph.setYUnits(yaxis.getUnits());
	    }
	  else
	    {
	      subgraph =  new MultiLineGraph(monitorManager, "time(secs)", xlabel.getText(), 0, 0, true);
	      subgraph.setYUnits(xaxis.getUnits());
	    }
	  BoxedRangeModel brm = subgraph.getBoxedRange();
	  brm.setYMin(boxedRange.getYMin());
	  brm.setYMax(boxedRange.getYMax());
	  brm.setYValue(boxedRange.getYMin());
	  brm.setYExtent(boxedRange.getYMax());
	  
	  subgraph.setTitle(getTitle());
	  subgraph.addWindowListener(new SubGFrameListener(this));
	  monitorManager.addDisplay(subgraph);
	}
      ((HDataDisplay)dataDisplay).getPlot(index).addMonitorFunction((MonitorFunction)subgraph);
    }
  
  public void removeSubgraph(Graph sg)
    {
      if (sg == subgraph) subgraph = null;
    }

  public void setSubgraphable(boolean b)
    {
      if (b)
	{
	  //add listeners
	  if (sgIconListener == null) 
	    {
	      sgIconListener = new SubGIconListener(this);
	      addWindowListener(sgIconListener);
	    }
	  if (sgMouseListener == null) 
	    {
	      sgMouseListener = new SubGMouseListener(this);
	      dataDisplay.addMouseListener(sgMouseListener);
	      if (isXAxis) xaxis.addMouseListener(sgMouseListener);
	      else yaxis.addMouseListener(sgMouseListener);
	    }
	}
      else //remove listeners
	{
	  if (sgIconListener != null) 
	    {
	      removeWindowListener(sgIconListener);
	      sgIconListener = null;
	    }
	  if (sgMouseListener != null) 
	    {
	      dataDisplay.removeMouseListener(sgMouseListener);
	      if (isXAxis) xaxis.removeMouseListener(sgMouseListener);
	      else yaxis.removeMouseListener(sgMouseListener);
	      sgMouseListener = null;
	    }
	}
    }


  //SUBGRAPH INTERNAL CLASSES
  //inner class window adapter to look for subgraph closing
  //listens on subgraph
  private class SubGFrameListener extends WindowAdapter
  {
    private Histogram window;
    public SubGFrameListener(Histogram w)
      {
	window = w;
      }
    public void windowClosing(WindowEvent e)
      {
	window.removeSubgraph((Graph)e.getSource());
      }
  }

  //inner class window adapter to look for iconify and deiconify of main graph to do the same for subgraph
  //listens on main graph
  private class SubGIconListener extends WindowAdapter
  {
    private Histogram window;
    public SubGIconListener(Histogram w)
      {
	window = w;
      }
    public void windowIconified(WindowEvent e)
      {
	if (window.subgraph != null) 
	  {
	    window.subgraph.setState(Frame.ICONIFIED);
	  }
      }
    public void windowDeiconified(WindowEvent e)
      {
	if (window.subgraph != null) 
	  {
	    window.subgraph.setState(Frame.NORMAL);
	  }
      }
  }

  //inner class mouse adapter to look for double click on data area to add param to subgraph
  //listens on IDAxis and dataDisplay
  private class SubGMouseListener extends MouseAdapter
  {
    private Histogram window;
    public SubGMouseListener(Histogram w)
      {
	window = w;
      }
      public void mouseClicked(MouseEvent e)
      {
	if (e.getClickCount() == 2)
	  {
	    int val = -1;
	    if (window.isXAxis)
	      val = (int)((IDAxis)window.xaxis).getValue(e.getX());
	    else
	      val = (int)((IDAxis)window.yaxis).getValue(e.getY());
	    if (val >= 0) window.addToSubgraph(val);
	  }
      }
  }
  //END OF SUBGRAPH INTERNAL CLASSES
}
