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
 * File: NumberData.java
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
/**just an interface for data which allows the histogram to extract the ID
 *this should maybe be either an inner class of Histogram or an inner class
 *of the Data Structure that will eventually replace Vector for representing 
 *the actual data plotted
 */

import java.util.Vector;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

public class NumberData extends BoxedRangeDefault implements MonitorPlus
{
	//whether this is live or we're reading from a file
	public static final int LOGFILE = 1;
	public static final int LIVE_DATA = 0;

	//protected Vector vector;
	protected Vector data = null;//NumberPair data[];
	//protected int change = 0;
	protected MonitorDataType.Base dataType = null;
	protected boolean receivedFirst = false;
	protected boolean stopped = true;
	protected boolean paused = true;
	protected boolean inUserStop = false;
	protected boolean inStop = false;
	protected Vector monitorables;
	protected Vector mfunctions;
	private boolean oneshot = false;

	protected double currentTime = -1; //this is the time in secs the last response was processed. this is relative to the time the monitor op
	//starts

	//these define whether the structure remains a fixed size dropping
	//new elements or old elements or if the structure expands to add
	//new elements
	public static final int DROP_NEW = 1;
	public static final int DROP_OLD = 2;
	public static final int EXPAND = 4;
	public static final int LOG_TO_FILE = 32;

	private ONL.BaseFileWriter lfStream = null;
	private File logfile = null;

	protected int maxSize = 0; //ignore if expandable
	protected int addStyle = DROP_OLD;
	private int currentSize = 0;
	private int first = 0;
	private int last = -1;
	private MonitorManager.TimeListener timeListener = null;



	public NumberData(MonitorDataType.Base dt, int style)
	{
		this(dt, MonitorManager.getHistory(), style);
	}
	public NumberData(MonitorDataType.Base dt, int sz, int style)
	{ 
		initialize(dt, sz, style);
	}
	public NumberData(MonitorDataType.Base dt, double xmn, double xmx, double ymn, double ymx, int style)
	{
		this (dt, xmn, xmx, ymn, ymx,  MonitorManager.getHistory(), style);
	}
	public NumberData(MonitorDataType.Base dt, double xmn, double xmx, double ymn, double ymx, int sz, int style)
	{ 
		super(xmx, xmn, ymx, ymn);
		initialize(dt, sz, style);
	}

	public void initialize(MonitorDataType.Base dt, int sz, int style)
	{
		addStyle = style;
		//vector = new Vector(sz);
		data = new Vector(sz); //NumberPair[sz];
		maxSize = sz;
		dataType = dt;
		monitorables = new Vector();
		mfunctions = new Vector();
		setHold(true);
		oneshot = (!dataType.isPeriodic());
		//System.out.println(dataType.getName() + " NumberData::initialize dataType is " + dataType.getMonitorID());
	}

	public void setStyle(int s) { addStyle = s;}

	public void addElement(NumberPair np)
	{
		if (currentSize > 0 && ((NumberPair)data.lastElement()).getX() == np.getX()) return;
		//currently not bothering to adjust the bounds if an element is
		//removed to make room: need to to adjust for zoom
		//System.out.println(getDataType().getName() + "::addElement (" + np.getX() + ", " + np.getY() + ")");
		adjustBounds(np.getX(), np.getY());
		//vector.add(vector.size(), np); //adds to end
		//all going to drop old for now
		//System.out.println("NumberData::addElement about to remove old element");
		double x_diff = getXMax() - getXMin();
		if (x_diff > maxSize && currentSize > 0)//currentSize >= maxSize)
		{
			//get the element we're going to overwrite
			NumberPair elem = (NumberPair)data.firstElement();
			//advance the first and last pointers
			++first;
			++last;
			if (first >= maxSize) first = 1; //1.15.08 changed from first = 0;
			if (last >= maxSize) last = 0;
			data.add(np);//data[last] = np;

			if (lfStream != null)
			{
				try
				{
					lfStream.writeLine(new String(elem.getX() + " " +elem.getY()));
					//lfStream.writeChars(new String (String.valueOf(elem.getX()) + " " + String.valueOf(elem.getY()) + "\n"));
				}
				catch(IOException e)
				{
					System.err.println("NumberData couldn't write data to file -- " + dataType.getName());
				}
			}
			setXMin(((NumberPair)data.firstElement()).getX()); //PROBLEM this assumes the vector is sorted according to x
		}
		else
		{
			++currentSize;
			++last;
			data.add(np);//data[last] = np;
			if (currentSize == 1) setXMin(((NumberPair)data.firstElement()).getX()); 
		}
	}
	public void adjustBounds(double x, double y)
	{

		//System.out.println("NumberData::adjustBounds y is " + y + " ymax is " + getYMax() + " ymin is " + getYMin()); 
		if (x > getXMax())
		{
			//System.out.println("NumberData::adjustBounds x is " + x + " xmax is " + getXMax()); 
			setXMax(x);
		}
		else if (x < getXMin())
		{
			//if (x < 0) 
			//System.out.println("NumberData::adjustBounds for " + dataType.getName() + " x = " + x);
			setXMin(x);
		}

		if (y > getYMax())
		{
			//System.out.println("NumberData::adjustBounds calling set ymax"); 
			setYMax(y);
		}
		else if (y < getYMin())
		{
			//System.out.println("NumberData::adjustBounds calling set ymin"); 
			setYMin(y);
		}
	}


	public void setBounds()
	{
		double oxmx = getXMax();
		double oxmn = getXMin();
		double oymx = getYMax();
		double oymn = getYMin();

		double xmx;
		double xmn;
		double ymx;
		double ymn;

		NumberPair np = (NumberPair)data.firstElement(); //[first];
		xmx = xmn = np.getX();
		ymx = ymn = np.getY();

		int i;
		int j = first + 1;
		for (i = 1; i < currentSize; ++i)
		{
			np = (NumberPair)data.elementAt(j);//[j];
			if (np.getX() > xmx)
				xmx = np.getX();
			else if (np.getX() < xmn)
				xmn = np.getX();

			if (np.getY() > ymx)
				ymx = np.getY();
			else if (np.getY() < ymn)
				ymn = np.getY();
			++j;
		}

		if (xmx != oxmx) 
			setXMax(xmx);
		if (xmn != oxmn) 
			setXMin(xmn);

		if (ymx != oymx) 
			setYMax(ymx);
		if (ymn != oymn) 
			setYMin(ymn);
	}

	public void addElement(double a, double b)
	{ 
		addElement(new NumberPair(a,b));
		//System.out.println("NumberData::addElement ( " + a + ", " + b + ")");
	}

	public NumberPair getElementAt(int index)
	{ 
		return ((NumberPair)data.elementAt(index));
		//	int i = first + index;
		//if (i < maxSize) 
		//    return (data[i]);
		//else return (data[i - maxSize]);
	}

	public NumberPair getLastElement()
	{ 
		if (currentSize == 0) return null;
		else 
		{
			//NumberPair rtn = (NumberPair)vector.elementAt((vector.size()) - 1);
			//System.out.println("   getLastElement " + rtn.getX() + ", " + rtn.getY());
			//return rtn;
			return ((NumberPair)data.lastElement());//data[last]);
		}
	}

	public Object getFirstElement() 
	{ 
		if (currentSize <= 0) return null;
		else return ((NumberPair)data.firstElement());//data[first]);
	}

	public int getSize() { return (data.size());}//currentSize);}

	public int getMaxSize() { return maxSize;}

	public String getName() { return dataType.getName();}

	public void setName(String nm)
	{ 
		dataType.setName(nm);
		//System.out.println("NumberData::setName " + nm);
	}


	public void printOut()
	{
		System.out.println("printing limits (xmin, ymin, xmax, ymax) = (" + String.valueOf(getXMin()) + ", " + String.valueOf(getYMin()) + ", " + String.valueOf(getXMax()) + ", " + String.valueOf(getYMax()) + ")");
	}


	//Monitor interface
	public void addMonitorFunction(MonitorFunction d) 
	{ 
		if (!mfunctions.contains(d))
		{
			ExpCoordinator.print(new String(dataType.getName() + "::addMonitorFunction"), 5);;
			//if (!stopped) d.addData(this);
			//change July 31
			d.addData(this);
			mfunctions.addElement(d);
		}
	}

	public void removeMonitorFunction(MonitorFunction d) 
	{ 
		if (!inStop)
		{
			System.out.println(dataType.getName() + "::removeMonitorFunction");
			mfunctions.removeElement(d);
			if (mfunctions.isEmpty()) userStop();
			//System.out.println("   mfunctions " + mfunctions.size());
		}
	}

	public void setDataExact(double data, double timev)
	{
		if (!stopped && !paused)
		{
			//System.out.println(dataType.getName()::setDataExact + " time is " + timev);
			currentTime = timev;
			ExpCoordinator.print(new String(dataType.getName() + " NumberData::addElement ( " + data + ", " + timev + ")"), 7);
			addElement(new NumberPair( timev, data)); //puts into Mb/s
			receivedFirst = true;//don't add the first one since the calculation will be off
		}
	}

	public void setData(double data, double timeInterval) //timeInterval is in seconds
	{
		if (!stopped && !paused)
		{
			if (receivedFirst || oneshot)
			{
				//System.out.println(dataType.getName() + " time interval is " + timeInterval);
				if (currentTime >= 0) 
					currentTime += timeInterval;
				else currentTime = 0;
				ExpCoordinator.print(new String(dataType.getName() + " NumberData::setData ( " + data + ", " + timeInterval + " currentTime:" + currentTime +")"), 5);
				addElement(new NumberPair( currentTime, dataType.convertData(data))); //puts into Mb/s
				if (oneshot)
				{
					fireEvent(new BoxedRangeEvent(this, (BoxedRangeEvent.XVALUE_CHANGED | 
							BoxedRangeEvent.XEXTENT_CHANGED | 
							BoxedRangeEvent.YVALUE_CHANGED | 
							BoxedRangeEvent.YEXTENT_CHANGED)));
				}
			}
			else receivedFirst = true;//don't add the first one since the calculation will be off
		}
	}

	public void start()
	{
		ExpCoordinator.print(new String(dataType.getName() + ".start " + dataType.getMonitorID()), 3);
		if (timeListener != null) setCurrentTime(timeListener.getCurrentTime());
		else ExpCoordinator.print( "    timeListener null", 3);
		//removed July 31
		/*
      for (int i = 0; i < mfunctions.size(); ++i) 
	{
	  MonitorFunction g = (MonitorFunction)mfunctions.elementAt(i);
	  //System.out.println("calling MonitorFunction.addData");
	  g.addData(this);
	}
		 */
		//System.out.println("   returning");
		stopped = false;
		paused = false;
		setHold(false);
	}

	public void resetTime(){ if (timeListener != null) setCurrentTime(timeListener.getCurrentTime());}
	public void stop()
	{
		inStop = true;
		for (int i = 0; i < mfunctions.size(); ++i) 
		{
			MonitorFunction g = (MonitorFunction)mfunctions.elementAt(i);
			//System.out.println("calling MonitorFunction.removeData");
			g.removeData(this);
		}
		mfunctions.clear();
		userStop();
		stopped = true;
		receivedFirst = false;
		currentTime = -1;
		inStop = false;
	}

	public boolean isStarted() { return (!stopped); }
	public void pause() { paused = true;}
	public void restart() { paused = false;}
	//end Monitor interface

	public void userStop() 
	{
		inUserStop = true;
		System.out.println("NumberData::userStop for " + dataType.getName() + ", " + dataType.getMonitorID());
		for (int i = 0; i < monitorables.size(); ++i) 
		{
			Monitorable m = (Monitorable)monitorables.elementAt(i);
			//System.out.println("calling ME.removeMonitor");
			m.removeMonitor(this);
		}
		monitorables.clear();
		stopLogging();
		//vector.clear();
		clearData();
		inUserStop = false;
	}
	protected void clearData()
	{
		currentSize = 0;
		last = -1;
		first = 0;
	}
	public MonitorDataType.Base getDataType() { return dataType;}
	public void setCurrentTime(double d) 
	{ 
		ExpCoordinator.print(new String(dataType.getName() + " NumberData.setCurrentTime to " + d), 3);
		currentTime = d;
	}
	public double getCurrentTime() { return currentTime;}
	public void setMonitorEntry(MonitorEntry.Base me) { addMonitorable(me);}
	public void addMonitorable(Monitorable m) 
	{ 
		if (!monitorables.contains(m)) 
		{
			//System.out.println("NumberData::addMonitorable");
			monitorables.addElement(m);
		}
	}
	public void removeMonitorable(Monitorable m) 
	{ 
		if (!inUserStop) monitorables.removeElement(m);
	}
	public boolean isLive() { return (dataType.isLiveData());}
	public void setTimeListener(MonitorManager.TimeListener tL) { timeListener = tL;}
	public boolean isLogging() { return (lfStream != null);}
	public java.io.File getLogFile() { return logfile;}
	public void setLogFile(File f) 
	{
		ExpCoordinator.print(new String("NumberData.setLogFile " + f) , 2);
		if (lfStream == null)
		{
			//open logfile for writing   
			try
			{
				//should say if file already exists
				lfStream = new ONL.BaseFileWriter(f);
				//lfStream = new ONL.OutStream(f);
				//BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(lfStream));
				dataType.saveToLogFile(lfStream);
				//wr.flush();
				logfile = f;
			}
			catch (IOException e)
			{
				System.err.println("Error: File " + f.getName() + " cannot be opened.");
				lfStream = null;
				logfile = null;
			}
		}
	}
	public void stopLogging()
	{ 
		if (lfStream != null) 
		{
			try
			{
				NumberPair elem;
				int j = first;
				for (int i = 0; i < currentSize; ++i)
				{ 
					if (j >= maxSize) j = 0; //wrap around we're trying to record from first to last
					elem = (NumberPair)data.elementAt(j);//data[j];
					lfStream.writeLine(new String(elem.getX() + " " +elem.getY()));
					//lfStream.writeChars(new String (String.valueOf(elem.getX()) + " " + String.valueOf(elem.getY()) + "\n"));
					++j;
				}
				//lfStream.flush();
				//lfStream.close();
				lfStream.finish();
				lfStream = null;
				logfile = null;
			}
			catch (IOException e)
			{
				System.err.println("NumberData::userStop IOException closing file -- " + dataType.getName());
			}
		}
	}

	public void recordData(ONL.Writer writer) throws java.io.IOException
	{
		int j = first;
		NumberPair elem;
		writer.writeInt(currentSize);
		for (int i = 0; i < currentSize; ++i)
		{ 
			if (j >= maxSize) j = 0; //wrap around we're trying to record from first to last
			elem = (NumberPair)data.elementAt(j);//[j];
			writer.writeLine(new String(elem.getY() + " " +elem.getX()));
			++j;
		}
	}

	public void readRecordedData(ONL.Reader reader) throws java.io.IOException
	{
		clearData();
		start();
		int max = reader.readInt();
		ExpCoordinator.print(new String("NumberData.readRecordedData size " + max), 6);
		if (max <= 0) return;
		ONL.StringReader str_rdr = new ONL.StringReader(reader.readLine());
		setDataExact(str_rdr.readDouble(), str_rdr.readDouble());
		for (int i = 1; i < max; ++i)
		{
			str_rdr.reset(reader.readLine());
			setDataExact(str_rdr.readDouble(), str_rdr.readDouble());
		}
	}
}
