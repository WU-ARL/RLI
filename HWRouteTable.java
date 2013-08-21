
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.lang.reflect.Array;
import java.util.*;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.Color;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class HWRouteTable extends HWTable
{
	public static final int TEST_ADD = Topology.TEST_DEFRTS;
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////// HWRouteTable.Spec /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class Spec extends HWTable.Spec
	{
		public Spec(String uri, Attributes attributes, boolean perport) { super(uri, attributes, perport);}
		public Spec(String ttl, String perport) { super(ttl, perport);}
		public Spec(HWRouteTable.Spec sp) { super((HWTable.Spec)sp);}

		public HWTable createTable(ONLComponent c)//Hardware.Port p)
		{
			HWRouteTable rtn = new HWRouteTable(c, properties.getProperty(ExperimentXML.TITLE), this);
			HWTable.TableModel tm = (HWTable.TableModel)rtn.getTableModel();

			if (tm == null) ExpCoordinator.print("HWTable.Spec.createTable tablemodel is null");
			Vector cols = getElementSpec().columnSpecs;
			int max = getNumCols();
			for (int i = 0; i < max; ++i)
			{
				tm.setPreferredWidth(i, ((ColumnSpec)cols.elementAt(i)).getWidth());
			}
			String str = properties.getProperty(ExperimentXML.ID);
			if (str != null) rtn.setID(Integer.parseInt(str));
			return rtn;
		}
		public String getEndToken() { return (ExperimentXML.ROUTE_TABLE);}
	}//HWRouteTable.Spec

	///////////////////////////////////////////////// HWRouteTable methods ////////////////////////////////////////////////////
	public HWRouteTable(ONLComponent oc, String ttl, Spec sp)
	{
		super(oc, ttl, sp, ONLComponent.RTABLE);
	}

	public void addRoute(String ip, int mask, int np, ONL.IPAddress nhip) { addRoute(ip, mask, np, nhip, false, false);}
	public void addRoute(String ip, int mask, int np, ONL.IPAddress nhip, boolean generated, boolean hst_generated)
	{
		HWTableElement tmp_elem = createNewElement();
		tmp_elem.setGenerated(generated);
		tmp_elem.setHostGenerated(hst_generated);
		tmp_elem.setField(ExperimentXML.PREFIX, ip);
		tmp_elem.setField(ExperimentXML.MASK, mask);
		NextHop nexthop = new NextHop(np, nhip);
		if (getParent() instanceof Hardware.Port) nexthop.setHardware((Hardware)getParent().getParent());
		tmp_elem.setField(ExperimentXML.ROUTE_NEXTHOP, nexthop);
		/*
		tmp_elem.setField(ExperimentXML.ROUTE_NEXTHOP, np);
		if (tmp_elem.getField(ExperimentXML.ROUTE_NEXTHOP_IP) != null) tmp_elem.setField(ExperimentXML.ROUTE_NEXTHOP_IP, nhip);
		*/
		ExpCoordinator.print(new String("HWRouteTable(" + getLabel() + ").addRoute " + ip + "/" + mask + " " + np + ":" + nhip.toString() + " element to add:" + tmp_elem.toString()), TEST_ADD);

		if (!containsElement(tmp_elem))
		{
			HWTableElement tmp_elem2 = (HWTableElement)addNewElement(tmp_elem, false);
			ExpCoordinator.print(new String("HWRouteTable(" + getLabel() + ").addRoute after adding " + tmp_elem2.toString()), TEST_ADD);
		}
	}
	public void addGeneratedRoute(String ip, int mask, int np, ONL.IPAddress nhip) {addRoute(ip, mask, np, nhip, true, false);}
	public void addHostGeneratedRoute(String ip, int mask, int np, ONL.IPAddress nhip) {addRoute(ip, mask, np, nhip, true, true);}
	
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(ExperimentXML.ROUTE_TABLE);
		xmlWrtr.writeAttribute(ExperimentXML.TITLE, getLabel());
		//write fields
		Vector tmp_fields = getFields();
		int max = tmp_fields.size();
		int i = 0;
		for (i = 0; i < max; ++i)
		{
			((Field)tmp_fields.elementAt(i)).writeXML(xmlWrtr);
		}
		max = size();
		for (i = 0; i < max; ++i)
		{
			HWTableElement elem = (HWTableElement)getElement(i);
			if (elem.isWritable()) elem.writeXML(xmlWrtr);
		}
		xmlWrtr.writeEndElement();
	}
	public boolean areEqual(Object o1, Object o2)
	{
		if (super.areEqual(o1, o2)) return true;
		else
		{
			HWTableElement elem1 = (HWTableElement)o1;
			HWTableElement elem2 = (HWTableElement)o2;
			return (elem1.getField(ExperimentXML.PREFIX).isEqual(elem2.getField(ExperimentXML.PREFIX)) &&
					elem1.getField(ExperimentXML.MASK).isEqual(elem2.getField(ExperimentXML.MASK)));
		}
	}
	public void removeHostGeneratedRoute()
	{
		Vector del_elems = new Vector(getList());
		if (!del_elems.isEmpty())
		{
			int max = del_elems.size();
			ONLComponent.Undoable delEdit;
			HWTableElement elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (HWTableElement)del_elems.elementAt(i);
				if (elem.isHostGenerated()) removeElement(elem);
			}
		}
	}
	public void clearRoutes()
	{
		Vector del_elems = new Vector(getList());
		if (!del_elems.isEmpty())
		{
			int max = del_elems.size();
			ONLComponent.Undoable delEdit;
			HWTableElement elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (HWTableElement)del_elems.elementAt(i);
				ExpCoordinator.print(new String("HWRouteTable(" + getLabel() + ").clearRoutes removing(" + elem.toString() + ")"), Topology.TEST_DEFRTS);
				removeElement(elem);
			}
		}
	}
}
