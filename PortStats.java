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
 * File: PortStats.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/25/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.*;
import javax.swing.*;

public abstract class PortStats implements Monitor
{
	public static final int TEST_STATS = 3;
	public static final int STATE_CHANGED = 1;
	public static final int VALUE_CHANGED = 2;
	public static final int INDEX_CHANGED = 3;
	public static final int STOP = 0;
	public static final int ONE_SHOT = 1;
	public static final int PERIODIC = 2;
	public static final int MONITOR = 6;
	public static final String STOP_STRING = "Stop";
	public static final String ONE_SHOT_STRING = "Once";
	public static final String PERIODIC_STRING = "Periodic";
	public static final String MONITOR_STRING = "Add to Graph";
	private double value = 0;
	private int state = STOP;
	private Index index = null;
	private String display_str = "0";
	//private MonitorDataType.Base mDataType = null;
	protected RouterPort port = null;
	private Vector monitorables = null;
	private Vector stateListeners = null;
	private ONLComponent.ONLMonitorable onlMonitorable = null;
	private boolean inUserStop = false;
	protected PollingRate pollingRate = null;
	private ExpCoordinator expCoordinator = null;
	private NumberData monitor = null;
	protected MonitorManager.TimeListener timeListener = null;

	public static class Indices implements Listener
	{
		private Stats indices[] = null;
		private int maxIndex = 0;
		private int numIndices = 0;
		private class Stats
		{
			public int index = 0;
			public Vector stats = null;
			public Stats(int i)
			{
				index = i;
				stats = new Vector();
			}
		}

		public Indices(int max)
		{
			maxIndex = max;
			numIndices = maxIndex+1;
			indices = new Stats[numIndices];
			for (int i = 0; i < numIndices; ++i)
			{
				indices[i] = new Stats(i);
			}
		}

		public void addStat(PortStats ps)
		{
			if (ps.index.getValue() < 0 || ps.index.getValue() > maxIndex) 
			{
				ps.setIndex(assignIndex());
			}
			ps.addListener(this);
			int i = ps.getIndex();
			if (!indices[i].stats.contains(ps)) indices[i].stats.add(ps);
		}

		public void removeStat(PortStats ps)
		{
			for (int i = 0; i < numIndices; ++i)
			{
				if (indices[i].stats.contains(ps))
				{
					indices[i].stats.remove(ps);
					ps.removeListener(this);
					ps.setIndex(-1);
					return;
				}
			}
		}

		private int assignIndex()
		{
			int min_ndx = 1;
			int min = indices[min_ndx].stats.size();
			for (int i = min_ndx; i < numIndices; ++i)
			{
				if (indices[i].stats.isEmpty()) return i;
				else
				{
					if (indices[i].stats.size() < min)
					{
						min_ndx = i;
						min = indices[i].stats.size();
					}
				}
			}
			return min_ndx;
		}

		//PortStats.Listener interface
		public void stateChanged(PortStats.Event pe){}
		public void valueChanged(PortStats.Event pe){}
		public void indexChanged(PortStats.Event pe)
		{
			PortStats ps = (PortStats)pe.getSource();
			int ov = (int)pe.getOldValue();
			int nv = (int)pe.getNewValue();
			if (nv <= 0 && nv <= maxIndex)
			{
				if (indices[ov].stats.contains(ps)) indices[ov].stats.remove(ps);
				if (!indices[nv].stats.contains(ps)) indices[nv].stats.add(ps);
			}
		}
		//end PortStats.Listener interface
		public int getMaxIndex() { return maxIndex;}
	}

	public static class Event extends EventObject
	{ 
		private int event_type = STATE_CHANGED;
		private double oldValue = 0;
		private double newValue = 0;

		public Event(PortStats ps, int tp, double ov, double nv)
		{
			super(ps);
			event_type = tp;
			oldValue = ov;
			newValue = ov;
		}
		public int getType() { return event_type;}
		public double getNewValue() { return newValue;}
		public double getOldValue() { return oldValue;}
	}

	public static interface Listener
	{
		public void stateChanged(PortStats.Event pe);
		public void valueChanged(PortStats.Event pe);
		public void indexChanged(PortStats.Event pe);
	}

	public static class Index
	{
		private int value = -1;
		public Index(String s) throws java.text.ParseException
		{
			int v = Integer.parseInt(s);
			if (v != -1 && !setValue(v)) throw new java.text.ParseException(new String("PortStats.Index parse error: integer not in range 0-" + NSPPort.MAX_STAT_INDEX), 0);
		}
		public Index(int v)
		{
			setValue(v);
		}
		public Index() {}
		public int getValue() { return value;}
		public boolean setValue(int v)
		{
			if (v >= 0 && v <= NSPPort.MAX_STAT_INDEX) 
			{
				value = v;
				return true;
			}
			else return false;
		}
		public String toString() { return (String.valueOf(value));}
	}

	public PortStats(String s) throws java.text.ParseException
	{
		state = getStateFromString(s);
		if (state < 0) throw new java.text.ParseException("PortStats parse error: not 'Stop', 'Once', 'Periodic', or 'Monitor'", 0);
		else display_str = s;
		index = new Index();
	}
	public PortStats(RouterPort p)
	{
		port = p;
		onlMonitorable = p.getMonitorable();
		monitorables = new Vector();
		stateListeners = new Vector();
		expCoordinator = p.getExpCoordinator();
		timeListener = onlMonitorable.getMonitorManager().getTimeListener();
		index = new Index();
		//p.addStat(this);
	}
	//Monitor interface
	public void setData(double val, double timeInt)
	{
		setValue(val);
		if (state == ONE_SHOT) setState(STOP);
	}
	public void setDataExact(double val, double timev)//exact data value and exact time value 
	{
		setValue(val);
		if (state == ONE_SHOT) setState(STOP);
	}
	public void stop()
	{
		int ostate = state;
		state = STOP;
		if (ostate != state) fireStateChange(ostate, state);
	}
	public void start(){}
	public boolean isStarted() { return true;}
	public void addMonitorable(Monitorable m)
	{
		if (!monitorables.contains(m)) 
		{
			//System.out.println("NumberData::addMonitorable");
			monitorables.addElement(m);
		}
	}
	public void removeMonitorable(Monitorable m) { if (!inUserStop) monitorables.removeElement(m);}
	public abstract MonitorDataType.Base getDataType();
	public void addMonitorFunction(MonitorFunction mf){}
	public void removeMonitorFunction(MonitorFunction mf){}
	public void setLogFile(java.io.File f){}
	//end Monitor interface

	public void setValue(double val)
	{
		double ovalue = value;
		value = val;
		display_str = String.valueOf((int)value);
		if (value != ovalue) fireValueChange(ovalue, value);
	}
	public double getValue() { return value;}
	public String toString() { return display_str;}
	public int getState() { return state;}
	public void setState(int s)
	{ 
		int ostate = state;
		if (s != ostate) 
		{
			//send any requests if stop remove from monitorables
			switch(s)
			{
			case STOP:
				inUserStop = true;
				for (int i = 0; i < monitorables.size(); ++i) 
				{
					Monitorable m = (Monitorable)monitorables.elementAt(i);
					//System.out.println("calling ME.removeMonitor");
					m.removeMonitor(this);
				}
				monitorables.clear();
				inUserStop = false;
				state = s;
				break;
			case ONE_SHOT:
				if (ostate == STOP)
				{
					//set to one shot
					state = s;
					//if (mDataType != null) mDataType.setPeriodic(false);
					if (onlMonitorable != null) onlMonitorable.addMonitor(this);
				}
				//else it's already periodic so don't change
				break;
			case MONITOR:
			case PERIODIC:
				if (ostate == STOP && onlMonitorable != null) 
				{
					boolean b = getPollingRate();
					if (b) 
					{
						//NOTE:: should hold off calling onlMonitorable.addMonitor until ROUTE is committed so need
						// to separate out the the add some way
						if (s == MONITOR)
						{
							addMonitor(getMonitorDataType());
							/*
                          ParamSelector selector = onlMonitorable.getMonitorManager().getParamSelector();
                          if (selector.isListenersEmpty()) onlMonitorable.getMonitorManager().addDisplay(Graph.MULTILINE);
                          if (monitor == null) monitor = new NumberData(getMonitorDataType(), 0, 0, 0, 0, NumberData.DROP_OLD);
                          selector.setCurrentParam(monitor);
                          onlMonitorable.addMonitor(monitor);
							 */
						}
						else 
						{
							state = s;
							onlMonitorable.addMonitor(this);
						}
					}
				}
				break;
			}
		}
		if (ostate != state) fireStateChange(ostate, state);
	}
	public Monitor addMonitor(MonitorDataType.Base mdt)
	{
		if (mdt.isPeriodic())
		{
			//state = PERIODIC;
			pollingRate = new PollingRate(mdt.getPollingRate());
			ParamSelector selector = onlMonitorable.getMonitorManager().getParamSelector();
			if (selector.isListenersEmpty()) onlMonitorable.getMonitorManager().addDisplay(Graph.MULTILINE);
			if (monitor == null) 
			{
				ExpCoordinator.print(new String("PortStats.addMonitor " + mdt.getName()), 3);
				if (timeListener == null) 
					ExpCoordinator.print("   timeListerner is null", 3);
				monitor = new NumberData(getMonitorDataType(), 0, 0, 0, 0, NumberData.DROP_OLD);
				((NumberData)monitor).setTimeListener(timeListener);
				((NumberData)monitor).addBoxedRangeListener(timeListener);
			}
			selector.setCurrentParam(monitor);
			onlMonitorable.addMonitor(monitor);
			return monitor;
		}
		return null;
	}
	public int getStateFromString(String str)
	{
		if (str.equals(STOP_STRING)) return STOP;
		if (str.equals(ONE_SHOT_STRING)) return ONE_SHOT;
		if (str.equals(PERIODIC_STRING)) return PERIODIC;
		if (str.equals(MONITOR_STRING)) return MONITOR;
		return STOP;
	}
	public void addListener(PortStats.Listener l)
	{
		if (!stateListeners.contains(l)) stateListeners.addElement(l);
	} 
	public void removeListener(PortStats.Listener l)
	{
		if (stateListeners.contains(l)) stateListeners.removeElement(l);
	} 
	private void fireStateChange(int os, int ns)
	{
		int max = stateListeners.size();
		PortStats.Listener elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (PortStats.Listener)stateListeners.elementAt(i);
			elem.stateChanged(new Event(this, STATE_CHANGED, os, ns));
		}
	}
	private void fireValueChange(double os, double ns)
	{
		int max = stateListeners.size();
		PortStats.Listener elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (PortStats.Listener)stateListeners.elementAt(i);
			elem.valueChanged(new Event(this, VALUE_CHANGED, os, ns));
		}
	}
	private void fireIndexChange(int os, int ns)
	{
		int max = stateListeners.size();
		PortStats.Listener elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (PortStats.Listener)stateListeners.elementAt(i);
			elem.indexChanged(new Event(this, INDEX_CHANGED, os, ns));
		}
	}
	private boolean getPollingRate() //start option window and get polling rate from the user
	{
		// System.out.println("MonitorMenuListener::menuDragMouseReleased");
		//first check if the MonitorEntry already exists for this entry	  
		final String str0 = "Enter Counter Polling Rate";	  
		final String str1 = "minutes:";	  
		final String str2 = "seconds:";
		JTextField mins = new JTextField(5);
		mins.setText("");
		JTextField secs = new JTextField(5);
		secs.setText("");
		JCheckBox logFileButton = new JCheckBox("Log to File");

		Vector objectVector = new Vector();
		objectVector.add(str0);
		objectVector.add(str1);
		objectVector.add(mins);
		objectVector.add(str2);
		objectVector.add(secs);

		final String opt0 = "Enter";
		final String opt1 = "Cancel";
		Object[] options = {opt0,opt1};

		int rtn = JOptionPane.showOptionDialog((onlMonitorable.getMonitorManager().getMainWindow()), 
				objectVector.toArray(), 
				"Statistics", 
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, 
				null,
				options,
				options[0]);

		if (rtn == JOptionPane.YES_OPTION)
		{
			double s = 0;
			double us = 0;
			if (mins.getText().length() > 0)
				s = (Double.parseDouble(mins.getText())) * 60;
			if (secs.getText().length() > 0)
			{
				us = Double.parseDouble(secs.getText());
				s = s + us;
			}
			if (s > 0) 
			{
				pollingRate = new PollingRate(s, 0);
				return true;
			}
		}
		return false;
	}
	public void stopMonitoring() 
	{
		if (monitor != null)
		{
			//monitor.userStop();
			monitor.stop();
			monitor = null;
			ExpCoordinator.print("PortStats.stopMonitoring", 3);
		}
		else
			ExpCoordinator.print("PortStats.stopMonitoring monitor null", 3);
	}
	protected void reset()
	{
		setState(STOP);
		stopMonitoring();
	}
	public abstract MonitorDataType.Base getMonitorDataType();
	protected int getIndex() { return index.value;} 
	protected void setIndex(Index si) { setIndex(si.value);} 
	protected void setIndex(int si) 
	{ 
		int osi = index.value;
		if (osi != si)
		{
			if (index.setValue(si))
			{
				ExpCoordinator.print(new String("Port(" + port.getLabel() + ")Stats.setIndex " + si + " from " + osi + "  - success"), TEST_STATS);
				fireIndexChange(osi, index.value);
			}
			else ExpCoordinator.print(new String("Port(" + port.getLabel() + ")Stats.setIndex " + si + " from " + osi + "  - failed to set index value"), TEST_STATS);
		}
		else ExpCoordinator.print(new String("Port(" + port.getLabel() + ")Stats.setIndex " + si + " from " + osi + "  - no change in value"), TEST_STATS);
	}
}

