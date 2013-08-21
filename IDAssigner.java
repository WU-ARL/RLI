
import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.lang.reflect.Array;
import java.util.*;
///////////////////////////////////////////////////// IDAssigner ///////////////////////////////////////////////////////////
public class IDAssigner 
{
	public static final int TEST_ASSIGNMENT = Field.TEST_ASSIGNMENT;
	private Vector ids = null;
	private int maxID = 0;
	private int minID = 0;
	private int numIDs = 0;
	private Listener listener = null;
	private boolean reuse = false;
	private String name = "";

	////////////////////////////////////////// IDAssigner.IDable /////////////////////////////////////////////////////////////
	public static interface IDable
	{
		public int getID();
		public void setID(int i);
	}

	/////////////////////////////////////////////////// IDAssigner.ID ///////////////////////////////////////////////////////
	public static class ID implements IDable
	{
		private IDAssigner idassigner = null;
		private int value = -1;

		public ID(IDAssigner ida, int v)
		{
			idassigner = ida;
			value = v;
		}

		public int getValue() { return value;}
		public void setValue(int v)
		{
			if (v != value)
			{
				int ov = value;
				value = v;
				idassigner.changeIDable(this, ov, value);
			}
		}
		public int getID() { return (getValue());}
		public void setID(int i) { setValue(i);}
		public void clear(){ idassigner.removeIDable(this);}
		public ID getCopy() { return (idassigner.getNewID(value));}
	}

	////////////////////////////////////////// IDAssigner.Event /////////////////////////////////////////////////////////////
	public static class Event extends EventObject
	{ 
		//private int event_type = STATE_CHANGED;
		private double oldValue = 0;
		private double newValue = 0;

		public Event(IDable ps, double ov, double nv)
		{
			super(ps);
			//event_type = tp;
			oldValue = ov;
			newValue = ov;
		}
		//public int getType() { return event_type;}
		public double getNewValue() { return newValue;}
		public double getOldValue() { return oldValue;}
	}

	////////////////////////////////////////// IDAssigner.IDListener ///////////////////////////////////////////////////////////////////
	public static interface IDListener
	{
		public void indexChanged(IDAssigner.Event pe);
	}

	////////////////////////////////////////// IDAssigner.Listener ///////////////////////////////////////////////////////////////////
	private class Listener implements IDListener
	{
		public Listener(){}
		public void indexChanged(IDAssigner.Event e)
		{
			IDable o = (IDable)e.getSource();
			int ov = (int)e.getOldValue();
			int nv = (int)e.getNewValue();
			if (nv <= 0 && (maxID <= 0 || nv <= maxID))
			{
				changeIDable(o, ov, nv);
			}
		}
	}

	///////////////////////////////////////////////// IDAssigner.IDEntry ////////////////////////////////////////////////////////
	private class IDEntry
	{
		public int id = 0;
		public Vector objects = null;

		public IDEntry(int i) { id = i; objects = new Vector();}
		public void add(IDable o) { add(o, true);}
		public void add(IDable o, boolean set_id)
		{
			if (!objects.contains(o)) 
			{
				objects.add(o);
				if (set_id) o.setID(id);
			}
		}
		public void remove(IDable o) { remove(o, true);}
		public void remove(IDable o, boolean set_id)
		{
			if (objects.contains(o)) 
			{
				objects.remove(o);
				if (set_id) o.setID(-1);
			}
		}
	}

	public IDAssigner(boolean r)
	{
		ids = new Vector();
		reuse = r;
		listener = new Listener();
	}
	public IDAssigner(int max) { this(max, 0, false);}
	public IDAssigner(int max, boolean r) { this(max, 0, r);}
	public IDAssigner(String nm, int max, int min, boolean r)
	{
		this(max, min, r);
		name = new String(nm);
	}
	public IDAssigner(int max, int min, boolean r)
	{
		this(r);
		maxID = max;
		minID = min;
		numIDs = maxID+1;
	}

	public void addID(IDable o)
	{
		if (o == null) return;
		if (o.getID() < minID || (o.getID() > maxID && maxID > 0)) 
		{
			o.setID(assignIndex());
		}
		//o.addListener(listener);
		int i = o.getID();
		IDEntry id = getIDEntry(i);
		id.add(o);
	}

	public void removeIDable(IDable o)
	{
		IDEntry id = getIDEntry(o.getID());
		id.remove(o);
		o.setID(-1);
		//o.removeListener(listener);
	}

	public void changeIDable(IDable o, int ov, int nv)
	{
		//ExpCoordinator.print(new String("IDAssigner.changeIDable from " + ov + " to " + nv), TEST_ASSIGNMENT);
		IDEntry id = getIDEntry(ov);
		id.remove(o, false);
		id = getIDEntry(nv);
		id.add(o, false);
		if (o.getID() != nv) o.setID(nv);
	}

	public IDAssigner.ID getNewID(int i)
	{
		if (i <= maxID && i >= minID) 
		{
			ID rtn = new ID(this, i);

			IDEntry id = getIDEntry(i);
			id.add(rtn);

			return rtn;
		}
		else return (getNewID());
	}

	public IDAssigner.ID getNewID()
	{
		return getNewID(assignIndex());
	}

	private IDEntry getIDEntry(int i)
	{
		int max = ids.size();
		IDEntry elem = null;
		for (int j = 0; j < max; ++j)
		{
			elem = (IDEntry)ids.elementAt(j);
			if (elem.id == i) return elem;
		}
		elem = new IDEntry(i);
		ids.add(elem);
		return elem;
	}

	private int assignIndex()
	{
		int min_ndx = minID;
		IDEntry elem = null;
		int max = ids.size();
		for (int i = 0; i < max; ++i)
		{
			elem = (IDEntry)ids.elementAt(i);
			if (elem.id >= min_ndx) min_ndx = elem.id + 1;
			if (reuse && elem.objects.isEmpty()) return (elem.id);
		}
		if (maxID <= 0 || min_ndx <= maxID)
			return min_ndx;
		else return minID;
	}

	//IDAssigner.Listener interface
	public void indexChanged(IDAssigner.Event e)
	{
		IDable o = (IDable)e.getSource();
		int ov = (int)e.getOldValue();
		int nv = (int)e.getNewValue();
		if (nv <= 0 && (maxID <= 0 || nv <= maxID))
		{
			changeIDable(o, ov, nv);
		}
	}
	//end IDAssigner.Listener interface
	public int getMaxID() { return maxID;}
	public int getMinID() { return minID;}
	public String getName() { return name;}
}
