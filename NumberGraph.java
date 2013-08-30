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
 * File: NumberGraph.java
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
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.math.*;
import java.text.DecimalFormat;

//assumes y-axis is numbered and data is NumberData
public abstract class NumberGraph extends Graph //implements MenuFileListener.Saveable, MonitorFunction //Monitor, 
{
	//need to make file name and polling interval command line arguments

	private boolean tst = false; 
	private JLabel valueDisplay;
	private MenuShowValueListener showValueListener;
	private JMenuItem pauseItem = null;



	//inner class implementing listener for "Stop/Restart" menu option
	private class MenuPauseListener extends MenuDragMouseAdapter
	{
		private NumberGraph display;
		public MenuPauseListener(NumberGraph d)
		{
			display = d;
		}

		public void select()
		{
			if (display.paused) display.setPause(false);
			else display.setPause(true);
		}
	}

	//inner class implementing listener for Reset View menu option
	private class MenuReMaxListener extends MenuDragMouseAdapter
	{
		private NumberGraph display;
		public MenuReMaxListener(NumberGraph d)
		{
			display = d;
		}

		public void select()
		{
			//System.out.println("MenuReMaxListener::select");
			display.resetView();
		}
	}

	//inner class implementing listener for Realign menu option
	private class MenuResetTimeListener extends MenuDragMouseAdapter
	{
		private NumberGraph display;
		public MenuResetTimeListener(NumberGraph d)
		{
			display = d;
		}

		public void select()
		{
			//System.out.println("MenuReMaxListener::select");
			display.realignCurves();
		}
	}


	//inner menu class changing axis units
	private class YUnitListener extends MenuDragMouseAdapter
	{
		private NumberGraph graph;
		public YUnitListener(NumberGraph g)
		{
			super();
			graph = g;
		}
		public void select()
		{
			Object[] options = {"Change", "Close"};
			Units.ComboBox u_cbox = new Units.ComboBox(graph.yaxis.getUnits());
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(new ONL.ComponentwLabel(u_cbox, "Units:"));
			TextFieldwLabel lbl = new TextFieldwLabel(20, "optional label:");
			Object[] array = new Object[]{panel, lbl, (new JLabel("default label is unit type."))};
			int rtn = JOptionPane.showOptionDialog(graph, 
					array, 
					new String(graph.getTitle() + ": change y-axis label"), 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, 
					null,
					options,
					null);
			if (rtn == JOptionPane.YES_OPTION)
			{
				Units units = u_cbox.getSelectedUnits();
				if (units.getType() != graph.yaxis.getUnits())
				{
					graph.setYUnits(units.getType());
					int ndx = graph.monitorManager.getDisplayIndex(graph);
					graph.monitorManager.fireEvent(new MonitorManager.Event(graph, null, MonitorManager.Event.CHANGE_DISPLAY, ndx));
					//graph.ylabel.setText(Units.getLabel(units));
					//graph.ylabel.repaint();
					//graph.redraw();
				}
				String tmpstr = lbl.getText().replaceAll(" ", "_");
				if (tmpstr.length() > 0)
				{
					graph.ylabel.setText(new String(tmpstr));
					graph.ylabel.repaint();
					graph.redraw();
				}
				else
				{
					graph.ylabel.setText(new String(units.getLabel()));
					graph.ylabel.repaint();
					graph.redraw();
				}
			}
		}
	}
	private class YUnitMenu extends JMenu
	{
		private NumberGraph graph;

		private class UnitListener extends MenuDragMouseAdapter
		{
			private NumberGraph graph;
			private int units = Units.UNKNOWN;
			public UnitListener(NumberGraph g, int u)
			{
				super();
				graph = g;
				units = u;
			}
			public void select()
			{
				if (units != graph.yaxis.getUnits())
				{
					graph.setYUnits(units);
					int ndx = graph.monitorManager.getDisplayIndex(graph);
					graph.monitorManager.fireEvent(new MonitorManager.Event(graph, null, MonitorManager.Event.CHANGE_DISPLAY, ndx));
					//graph.ylabel.setText(Units.getLabel(units));
					//graph.ylabel.repaint();
					//graph.redraw();
				}
			}
		}

		public YUnitMenu(NumberGraph g)
		{
			super("Change Y-Axis Units");
			graph = g;
			JMenuItem menuItem = add("Mb/s");
			UnitListener ul = new UnitListener(graph, (Units.MBITS + Units.PERSEC));
			menuItem.addMenuDragMouseListener(ul);
			menuItem.addMouseListener(ul);
			menuItem = add("bits/s");
			ul = new UnitListener(graph, (Units.BITS + Units.PERSEC));
			menuItem.addMenuDragMouseListener(ul);
			menuItem.addMouseListener(ul);	
			menuItem = add("Cells/s");
			ul = new UnitListener(graph, (Units.CELLS + Units.PERSEC));
			menuItem.addMenuDragMouseListener(ul);
			menuItem.addMouseListener(ul);
			if (monitorManager.isARL())
			{	
				menuItem = add("Bytes");
				ul = new UnitListener(graph, (Units.BYTES));
				menuItem.addMenuDragMouseListener(ul);
				menuItem.addMouseListener(ul);
				menuItem = add("MBytes");
				ul = new UnitListener(graph, (Units.MBYTES));
				menuItem.addMenuDragMouseListener(ul);
				menuItem.addMouseListener(ul);
				menuItem = add("Cells");
				ul = new UnitListener(graph, Units.CELLS);
				menuItem.addMenuDragMouseListener(ul);
				menuItem.addMouseListener(ul);
			}
		}
	}

	//inner class implementing listener for "Show Values" menu option
	private class MenuShowValueListener extends MenuDragMouseAdapter
	{
		private boolean showing = false;
		private NumberGraph graph;
		private JMenuItem menuItem;
		private MouseListener dataDisplayListener;

		public MenuShowValueListener(NumberGraph g, JMenuItem mi)
		{
			graph = g;
			menuItem = mi;
			dataDisplayListener = new MouseAdapter() {
				public void mouseClicked(MouseEvent e)
				{
					graph.showValues(e.getX(), e.getY());
				}
			};
		}

		//public void menuDragMouseReleased(MenuDragMouseEvent e)
		public void select()
		{
			setShow(!showing);
		}

		public void setShow(boolean v)
		{
			if (showing && !v)
			{
				menuItem.setText("Show Values");
				graph.removeValueDisplay();
				graph.dataDisplay.removeMouseListener(dataDisplayListener);
				showing = false;
			}
			else if (!showing && v)
			{
				menuItem.setText("Don't Show Values");
				graph.addValueDisplay();
				graph.dataDisplay.addMouseListener(dataDisplayListener);
				showing = true;
			}
		}
	}


	//Main Constructor

	public NumberGraph(MonitorManager bwd, boolean t, int tp)
	{
		this(bwd, t, 0, 0, tp);
	}

	public NumberGraph(MonitorManager bwd, boolean t, int offsetx, int offsety, int tp)
	{
		//super("Test");
		super(bwd,offsetx,offsety, tp);
		tst = t;

		JMenuItem pauseItem = new JMenuItem("Stop");
		addToOptionMenu(pauseItem);
		MenuDragMouseAdapter mdma = new MenuPauseListener(this);
		pauseItem.addMenuDragMouseListener(mdma);
		pauseItem.addMouseListener(mdma);

		//showValues option
		JMenuItem menuItem = new JMenuItem("Show Values");
		addToViewMenu(menuItem);
		showValueListener = new MenuShowValueListener(this, menuItem);
		menuItem.addMenuDragMouseListener(showValueListener);
		menuItem.addMouseListener(showValueListener);

		//resize option
		menuItem = new JMenuItem("Reset View");
		addToViewMenu(menuItem);
		mdma = new MenuReMaxListener(this);
		menuItem.addMenuDragMouseListener(mdma);
		menuItem.addMouseListener(mdma);

		//change yaxis units option
		menuItem = new JMenuItem("Change Y-axis label");
		addToViewMenu(menuItem);
		mdma = new YUnitListener(this);
		menuItem.addMenuDragMouseListener(mdma);
		menuItem.addMouseListener(mdma);
		//addToViewMenu(new YUnitMenu(this));
		
		//realign option
		menuItem = new JMenuItem("Realign Curves");
		addToViewMenu(menuItem);
		mdma = new MenuResetTimeListener(this);
		menuItem.addMenuDragMouseListener(mdma);
		menuItem.addMouseListener(mdma);

		valueDisplay = new JLabel();
		valueDisplay.setFont(new Font("Dialog", Font.PLAIN, 9));
		repaint();
	}


	public void addData(MonitorPlus d)
	{
		//set up graph
		super.addData(d);
		reMax();
		//if (dataDisplay.length() == 1) 
		//{
		//  if ((d.getDataType().getDataUnits() & Units.CELLS) <= 0)
		//    setYUnits(d.getDataType().getDataUnits());
		//  repaint();
		//} 
	}


	public void setPause(boolean v)
	{
		super.setPause(v);
		if (paused != v)
		{
			if (paused) pauseItem.setText("Restart");
			else pauseItem.setText("Stop");
		}
	}


	public void reMax()
	{
		double ymx = 0;
		int max = dataDisplay.length();
		int i = 0;
		MonitorPlus elem;
		if (max > 0)
		{
			elem = dataDisplay.getData(0);
			ymx = elem.getYMax();
		}
		for (i = 1; i < max; ++i)
		{
			elem = dataDisplay.getData(i);
			if (ymx < elem.getYMax()) ymx = elem.getYMax();
		}
		boxedRange.setYMax(ymx);
		if (!isResized())
			boxedRange.setYExtent(ymx - boxedRange.getYValue());
	}
	public void showValues(int x, int y)// the coordinates to show values for 
	{
		DecimalFormat form = new DecimalFormat();
		//form.setMaximumFractionDigits(2);
		form.setGroupingSize(3);
		BigDecimal x_scaled = new BigDecimal(xaxis.getValue(x));
		x_scaled = x_scaled.setScale(2, BigDecimal.ROUND_UP);
		BigDecimal y_scaled = new BigDecimal(yaxis.getValue(y));
		y_scaled = y_scaled.setScale(2, BigDecimal.ROUND_UP);

		valueDisplay.setText("( " + form.format(xaxis.getValue(x)) + ", " + form.format(yaxis.getValue(y)) + " )");
		valueDisplay.revalidate();
		//ExpCoordinator.print(new String("NumberGraph.showValues for (" + x + ", " + y + ") value = " + valueDisplay.getText()), 4);
	}

	public void setShowValues(boolean v)
	{
		showValueListener.setShow(v);
	}

	public void addValueDisplay()
	{
		Container contentPane = getContentPane();
		GridBagLayout gridbag = (GridBagLayout)(contentPane.getLayout());
		GridBagConstraints c = new GridBagConstraints();
		//create and setup y axis label and add to layout
		c.gridx = offsetx;
		c.gridy = offsety + 2;
		c.gridwidth = 2;
		c.gridheight = 1;
		gridbag.setConstraints(valueDisplay, c);
		contentPane.add(valueDisplay);
		//revalidate();
		repaint();
	}

	public void removeValueDisplay()
	{
		Container contentPane = getContentPane();
		contentPane.remove(valueDisplay);
		//revalidate();
		repaint();
	}
}
