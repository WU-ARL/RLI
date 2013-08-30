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
 * File:Cluster.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created:8/26/2008
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;
import javax.swing.undo.*;
import javax.swing.event.*;
import java.awt.Color;
import java.awt.Point;
import javax.xml.stream.*;
import org.xml.sax.*;


public class Cluster
{
	public static final String HWINDEX = "hwindex";
	private String label;
	private int version;
	private String id;
	private Vector hardware;
	private Vector links;

	/////////////////////////////////////////////////// AddAction //////////////////////////////////////////////////////////

	public static class AddAction extends Topology.TopologyAction
	{
		private Cluster clusterType = null;
		public AddAction(Cluster c) { this(c, new String("Add " + c.label));}
		public AddAction(Cluster c, String nm)
		{
			super(nm);
			clusterType = c;
		}
		public void actionPerformed(ActionEvent e)
		{
			Topology topo = ExpCoordinator.theCoordinator.getTopology();
			Cluster.Instance cli = new Cluster.Instance(clusterType,topo);
			//topo.addNewCluster(cli);
		}
		public Cluster getClusterType() { return clusterType;}
	}//end class Cluster.AddAction


	///////////////////////////////////////////////// Hard coded clusters for NPR and NSP //////////////////////////////////
	public static class IXPCluster extends Cluster
	{
		public IXPCluster() { this(ONLComponent.IXP_LBL);}
		public IXPCluster(String sub_tp)
		{
			super("IXPCluster", 0x0100, ONLComponent.IXP_LBL);
			addHardware(sub_tp);
			addHardware(sub_tp);
		}
	}//end class Cluster.NPRCluster 


	public static class IXPMenu extends JMenu implements ListDataListener
	{
		private HardwareSpec hwSpec = null;
		public IXPMenu(HardwareSpec tp)
		{
			super("Add IXPCluster");
			hwSpec = tp;
			hwSpec.addSubtypeListener(this);
			HardwareSpec.Subtypes stypes = hwSpec.getSubtypes();
			int max = stypes.size();
			HardwareSpec.Subtype elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (HardwareSpec.Subtype)stypes.getElementAt(i);
				if (getItem(elem) == null) 
					insert(new IXPAction(elem), 0);
			}
		}
		//interface ListDataListener
		public void contentsChanged(ListDataEvent e) {}

		public void intervalAdded(ListDataEvent e) 
		{
			//System.out.println("TopologyPanel::intervalAdded");
			HardwareSpec.Subtypes l = (HardwareSpec.Subtypes)e.getSource();
			int bot = e.getIndex0();
			int top = e.getIndex1();
			JMenuItem elem;
			HardwareSpec.Subtype hw;
			for (int i = bot; i <= top; ++i)
			{
				hw = (HardwareSpec.Subtype)l.get(i);
				if (getItem(hw) == null) 
				{
					int pos = getItemCount() - 1;
					insert(new IXPAction(hw), pos);//new HardwareSpec.MenuItem(hw), pos);
				}
			}
		}

		public void intervalRemoved(ListDataEvent e) {}
		//end interface ListDataListener
		private Action getItem(HardwareSpec.Subtype hw)
		{
			Action elem_action;
			int max = getItemCount();
			for (int i = 0; i < max; ++i)
			{
				elem_action = getItem(i).getAction();
				if ((elem_action instanceof IXPAction) && (((IXPAction)elem_action).getSubtype() == hw)) 
				{
					return elem_action;
				}
			}
			return null;
		}
	}
	public static class IXPAction extends Cluster.AddAction
	{
		private HardwareSpec.Subtype subtype = null;
		public IXPAction()
		{
			super(new IXPCluster());
		}
		public IXPAction(HardwareSpec.Subtype sub_tp)
		{
			super(new IXPCluster(sub_tp.getLabel()), new String("Add " + sub_tp.getLabel() + "Cluster"));
			subtype = sub_tp;
		}
		public void actionPerformed(ActionEvent e)
		{
			final String opt0 = "OK";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			TextFieldwLabel num = new TextFieldwLabel(new JTextField("1"), "How many instances?");
			Object[] objarray = {num};
			int rtn = JOptionPane.showOptionDialog(ExpCoordinator.getMainWindow(), 
					objarray, 
					(String)getValue(Action.NAME), 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				int num_inst = Integer.parseInt(num.getText());
				Topology topo = ExpCoordinator.theCoordinator.getTopology();
				for (int i = 0; i < num_inst; ++i)
				{
					Cluster.Instance cli = new Cluster.Instance(getClusterType(),topo);
					//topo.addNewCluster(cli);
					ONLComponent ixp1 = cli.getHardwareAt(0);
					ONLComponent ixp2 = cli.getHardwareAt(1);
					ixp1.getGraphic().setLocation(10, 95);
					ixp2.getGraphic().setLocation(90, 95);
				}
			}
		}
		public HardwareSpec.Subtype getSubtype() { return subtype;}
	}//end class Cluster.IXPAction

	public static class NPRCluster extends Cluster
	{
		public NPRCluster()
		{
			super("IXPCluster", 0x0100, ONLComponent.NPR_LBL);
			addHardware(ONLComponent.NPR_LBL);
			addHardware(ONLComponent.NPR_LBL);
		}
	}//end class Cluster.NPRCluster 


	public static class NPRAction extends Cluster.AddAction
	{
		public NPRAction()
		{
			super(new NPRCluster());
		}

		public void actionPerformed(ActionEvent e)
		{
			Topology topo = ExpCoordinator.theCoordinator.getTopology();
			Cluster.Instance cli = new Cluster.Instance(getClusterType(),topo);
			//topo.addNewCluster(cli);
			ONLComponent npr1 = cli.getHardwareAt(0);
			ONLComponent npr2 = cli.getHardwareAt(1);
			npr1.getGraphic().setLocation(10, 95);
			npr2.getGraphic().setLocation(90, 95);
		}
	}//end class Cluster.NPRAction


	public static class NSPAction extends AbstractAction //Cluster.AddAction
	{
		public NSPAction()
		{
			super("Add NSP");
			//super(new NSPCluster());
		}
		public void actionPerformed(ActionEvent e)
		{
			final String opt0 = "OK";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};
			TextFieldwLabel num = new TextFieldwLabel(new JTextField("1"), "How many instances?");
			Object[] objarray = {num};
			int rtn = JOptionPane.showOptionDialog(ExpCoordinator.getMainWindow(), 
					objarray, 
					(String)getValue(Action.NAME), 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				int num_inst = Integer.parseInt(num.getText());
				Topology topo = ExpCoordinator.theCoordinator.getTopology();
				for (int i = 0; i < num_inst; ++i)
				{
					Hardware hw = (Hardware)topo.getNewHW(ONLComponent.NSP_LBL, ExperimentXML.ROUTER);
					if (hw != null)
						ExpCoordinator.theCoordinator.getCurrentExp().addComponent(hw);
					else ExpCoordinator.print("Cluster.NSPAction.actionPerformed hw null");
				}
			}
		}
	}//end class Cluster.NSPAction

	/////////////////////////////////////////////////// Cluster.Link ///////////////////////////////////////////////////////

	public static class Link
	{
		private int hw1Index;
		private int port1Index;
		private int hw2Index;
		private int port2Index;

		public Link(int h1, int p1, int h2, int p2)
		{
			hw1Index = h1;
			port1Index = p1;
			hw2Index = h2;
			port2Index = p2;
		}

		public Link(Link lnk) { this(lnk.hw1Index, lnk.port1Index, lnk.hw2Index, lnk.port2Index);}

		public Link(ONL.Reader rdr) throws IOException
		{
			read(rdr);
		}

		public void read(ONL.Reader rdr) throws IOException
		{
			hw1Index = rdr.readInt();
			port1Index = rdr.readInt();
			hw2Index = rdr.readInt();
			port2Index = rdr.readInt();
		}

		public void write(ONL.Writer wrtr) throws IOException
		{
			wrtr.writeInt(hw1Index);
			wrtr.writeInt(port1Index);
			wrtr.writeInt(hw2Index);
			wrtr.writeInt(port2Index);
		}
	}//end class Cluster.Link



	////////////////////////////////////////////////// class Cluster.Instance ////////////////////////////////////////////////////

	public static class Instance
	{
		private Cluster type = null;
		private int index = 0;
		private String state = ONLComponent.NOT_INIT;
		private ONLComponentList hardware;
		private ONLComponentList links;
		private Experiment experiment = null;
		private AddClusterEdit edit = null;


		///////////////////////////////////////////// Cluster.Instance.NCCP_AddCluster /////////////////////////////////////////
		protected static class NCCP_AddCluster extends ExpRequest implements Reservation.Reservable
		{
			private Cluster.Instance cluster = null;
			private boolean reserved = false;
			private class NCCP_Requester extends ExpDaemon.NCCP_Requester
			{
				public NCCP_Requester(Experiment e)
				{
					super(ExpDaemon.NCCP_Operation_AddCluster, e);
				}
				public void storeData(DataOutputStream dout) throws IOException
				{
					//super.storeData(dout);
					ExpCoordinator.print(new String("NCCP_AddCluster.NCCP_Requester.storeData cluster reference " + cluster.index), 5);
					ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
					wrtr.writeString(cluster.type.getLabel());
					wrtr.writeInt(cluster.index);
					wrtr.writeInt(0);//parent cluster
					wrtr.writeString("");
					//wrtr.writeShort(cluster.type.getID()); //type code
					wrtr.writeString(cluster.getCPAddr());
					int max = cluster.hardware.getSize();
					wrtr.writeInt(max);
					ONLComponent elem;
					for (int i = 0; i < max; ++i)
					{
						elem = cluster.hardware.onlComponentAt(i);
						wrtr.writeInt(elem.getReference());
					}
				}
			}//end class Cluster.Instance.AddClusterEdit.NCCP_AddCluster.NCCP_Requester

			private class NCCP_Response extends ExpDaemon.NCCP_Response
			{
				public NCCP_Response() { super();}
				public void retrieveData(DataInput din) throws IOException
				{
					super.retrieveData(din);
					ONL.NCCPReader reader = new ONL.NCCPReader(din);
					//if (status == NCCP.Status_Fine)
					//cluster.setIndex(reader.readInt());
					//else
					ExpCoordinator.print(new String("Cluster.Instance.AddClusterEdit.NCCP_AddCluster.NCCP_Response endOfMessage:" + endOfMessage), 4);
					if (status != NCCP.Status_Fine && endOfMessage == 1)
					{
						int error_type = StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR;
						ExpCoordinator ec = ExpCoordinator.theCoordinator;
						ec.addError(new StatusDisplay.Error((new String("Cluster:" + cluster.type.getLabel() + " Allocation Error: Resources in use. Try again later.\n")),
								"Resources in Use.",
								"Allocation Error",
								error_type));  
					}
				}
			}//end class Cluster.Instance.AddClusterEdit.NCCP_AddCluster.NCCP_Response

			/////////////////////////////////////////////////// Cluster.Instance.NCCP_AddCluster Constructor ////////////////////////////////

			public NCCP_AddCluster(Cluster.Instance cl, Experiment ex)
			{
				super();
				cluster = cl;
				request = new NCCP_Requester(ex);
				response = new NCCP_Response();
			}
			public void processResponse(NCCP.ResponseBase r)
			{
				if (r.getStatus() == NCCP.Status_Fine)
				{
					if (((NCCP_Requester)request).getReservationID() == null)
						cluster.setState(ONLComponent.ACTIVE);
					else reserved = true;
					//commitEdits();
				}
				else
				{
					if (((NCCP_Requester)request).getReservationID() == null)
						cluster.setState(ONLComponent.FAILED);
				}
				ExpCoordinator.theCoordinator.removeRequest(this);
			}//end class Cluster.Instance.AddClusterEdit.NCCP_AddCluster
			protected void setReservation(String res_id, int opc)
			{
				if (request != null)
				{
					request.setOp(opc);
					((NCCP_Requester)request).setReservationID(res_id);
				}
			}
			public boolean isReserved() { return reserved;}
		}//end class Cluster.Instance.NCCP_AddCluster

		/////////////////////////////////////////////////// Cluster.Instance.AddClusterEdit ////////////////////////////////////////////////////
		private class AddClusterEdit extends AbstractUndoableEdit implements ONLComponent.Undoable//extends ONLUndoManager.AggregateEdit
		{
			private Cluster.Instance cluster = null;
			private NCCP_AddCluster request = null;
			private boolean committed = false;
			private ExpCoordinator expcoord = null;

			/////////////////////////////////////////////////// Cluster.Instance.AddClusterEdit Constructor /////////////////////////////////////////////////
			public AddClusterEdit(Cluster.Instance cl) { this(cl, null);}
			public AddClusterEdit(Cluster.Instance cl, Experiment exp) 
			{ 
				super();
				cluster = cl;
				if (exp == null)
				{
					ExpCoordinator.print("Cluster.Instance.AddClusterEdit exp null", 7);
					request = new NCCP_AddCluster(cl, ExpCoordinator.theCoordinator.getCurrentExp());
				}
				else
					request = new NCCP_AddCluster(cl, exp);
				expcoord = ExpCoordinator.theCoordinator;
			}
			public void commit()
			{
				if (!committed) 
				{
					ExpCoordinator.print("AddClusterEdit.commit", 5);
					expcoord.sendRequest(request);
				}
				committed = true;
			}
			public boolean isCancelled(ONLComponent.Undoable un)
			{
				// if (un instanceof Experiment.DeleteEdit)
				//return (cluster.contains(((Experiment.DeleteEdit)un).getONLComponent()));
				if (un instanceof Cluster.Instance.DeleteClusterEdit)
					return (cluster == ((Cluster.Instance.DeleteClusterEdit)un).getCluster());
				return false;
			}
			public void actionPerformed(ActionEvent e)
			{
				ExpCoordinator.print("AddClusterEdit.actionPerformed", 5);
				expcoord.getCurrentExp().addCluster(cluster);
				//if (experiment == null) return;
			}
			public void undo() throws CannotUndoException
			{
				super.undo();
				expcoord.getCurrentExp().removeCluster(cluster);
			}
			public void redo() throws CannotRedoException
			{
				super.redo();
				expcoord.getCurrentExp().addCluster(cluster);
			}
			protected Cluster.Instance getCluster() { return cluster;}
		}//end Cluster.Instance.AddClusterEdit


		/////////////////////////////////////////////////// Cluster.Instance.DeleteClusterEdit ////////////////////////////////////////////////////
		private class DeleteClusterEdit extends AbstractUndoableEdit implements ONLComponent.Undoable//extends ONLUndoManager.AggregateEdit
		{
			private Cluster.Instance cluster = null;
			private boolean committed = false;
			private ExpCoordinator expcoord = null;

			/////////////////////////////////////////////////// Cluster.Instance.DeleteClusterEdit Constructor /////////////////////////////////////////////////
			public DeleteClusterEdit(Cluster.Instance cl) 
			{ 
				super();
				cluster = cl;
				//request = new NCCP_DeleteCluster(cl, ExpCoordinator.theCoordinator.getCurrentExp());
				expcoord = ExpCoordinator.theCoordinator;
			}
			public void commit()
			{
				if (!committed) 
				{
					ExpCoordinator.print("DeleteClusterEdit.commit", 5);
					//ExpCoordinator.theCoordinator.sendRequest(request);
				}
				committed = true;
			}
			public boolean isCancelled(ONLComponent.Undoable un)
			{
				// if (un instanceof Experiment.DeleteEdit)
				//return (cluster.contains(((Experiment.DeleteEdit)un).getONLComponent()));
				if (un instanceof Cluster.Instance.AddClusterEdit)
					return (cluster == ((Cluster.Instance.AddClusterEdit)un).getCluster());
				return false;
			}
			public void actionPerformed(ActionEvent e)
			{
				ExpCoordinator.print("DeleteClusterEdit.actionPerformed", 5);
				expcoord.getCurrentExp().removeCluster(cluster);
				//if (experiment == null) return;
			}
			public void undo() throws CannotUndoException
			{
				super.undo();
				expcoord.getCurrentExp().addCluster(cluster);
			}
			public void redo() throws CannotRedoException
			{
				super.redo();
				expcoord.getCurrentExp().removeCluster(cluster);
			}
			protected Cluster.Instance getCluster() { return cluster;}
		}//end Cluster.Instance.DeleteClusterEdit

		////////////////////////////////////////// Cluster.Instance ///////////////////////////////////////////////////////////////////////////

		public Instance(String uri, Attributes attrs, Experiment exp)
		{
			type = Cluster.getClusterType(attrs.getValue(uri, ExperimentXML.TYPENAME));
			setIndex(Integer.parseInt(attrs.getValue(uri, ExperimentXML.HWREF)));
			hardware = new ONLComponentList();
			links = new ONLComponentList();
			ExpCoordinator ec = ExpCoordinator.theCoordinator;
			ec.print(new String("Cluster.Instance(String uri, Attributes attrs, Experiment exp) type:" + type + " index:" + getIndex()), 6);
			experiment = exp;
			edit = new AddClusterEdit(this, exp);
			experiment.getTopology().addNewCluster(this);
			if (experiment.isLive()) ec.addEdit(edit);
		}
		public Instance(ONL.Reader rdr, Experiment exp) throws java.io.IOException
		{
			type = Cluster.getClusterType(rdr);
			setIndex(rdr.readInt());
			hardware = new ONLComponentList();
			links = new ONLComponentList();
			ExpCoordinator ec = ExpCoordinator.theCoordinator;

			experiment = exp;
			edit = new AddClusterEdit(this, exp);
			experiment.getTopology().addNewCluster(this);
			if (experiment.isLive()) ec.addEdit(edit);
		}

		public Instance(Cluster tp)
		{
			type = tp;
			hardware = new ONLComponentList();
			links = new ONLComponentList();
		}

		public Instance(Cluster tp, Topology topo)
		{
			this(tp);
			ExpCoordinator ec = ExpCoordinator.theCoordinator;

			ec.startCompoundEdit();
			experiment = ec.getCurrentExp();
			edit = new AddClusterEdit(this);
			topo.addNewCluster(this);
			if (experiment.isLive()) 
				ec.addEdit(edit);
			//fill hardware
			int max = tp.hardware.size();
			int i = 0;
			ONLComponent hwi;
			for (i = 0; i < max; ++i)
			{
				hwi = topo.getNewHW((String)tp.hardware.elementAt(i), null);
				hwi.setCluster(this);
				hwi.setProperty(Cluster.HWINDEX, i);
				if ((hwi instanceof Hardware) && !(hwi instanceof NSPDescriptor))
					((Hardware)hwi).initializePorts();
				hardware.addElement(hwi);
				experiment.addComponent(hwi);
			}
			//fill links
			fillLinks(topo);
			max = links.size();
			for (i = 0; i < max; ++i)
			{
				experiment.addComponent((LinkDescriptor)links.elementAt(i));
			}
			ec.endCompoundEdit();
		}

		public void writeExpDescription(ONL.Writer wrtr) throws IOException 
		{ 
			wrtr.writeString(type.getLabel());
			//wrtr.writeString(type.getID());
			wrtr.writeInt(index);
		}

		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(ExperimentXML.CLUSTER);//"cluster");
			xmlWrtr.writeAttribute(ExperimentXML.TYPENAME, type.getID());
			//xmlWrtr.writeAttribute(ExperimentXML.TYPECODE, String.valueOf(type.getID()));
			xmlWrtr.writeAttribute(ExperimentXML.HWREF, String.valueOf(index));
			xmlWrtr.writeEndElement();//cluster
		}

		private void fillLinks(Topology topo)
		{
			ExpCoordinator ec = ExpCoordinator.theCoordinator;
			LinkDescriptor lnk = null;
			Cluster.Link clnk = null;
			int max = type.links.size();
			String lbl;
			ONLComponent hw1;
			ONLComponent hw2;
			ExpCoordinator.print("Cluster.Instance.fillLinks " + max);
			for (int i = 0; i < max; ++i)
			{
				lbl = topo.getNewLinklbl();
				clnk = (Cluster.Link)type.links.elementAt(i);
				hw1 = getLinkPoint1(clnk);
				hw2 = getLinkPoint2(clnk);
				lnk = new LinkDescriptor(lbl, hw1, hw2, 0, ec);
				lnk.setFixed(true);
				lnk.setCluster(this);
				lnk.print(0);
				links.addElement(lnk);
			}
		}

		private ONLComponent getLinkPoint1(Cluster.Link clnk)
		{
			ONLComponent hw = (ONLComponent)hardware.elementAt(clnk.hw1Index);
			ONLComponent rtn = hw;
			if (hw instanceof Hardware)
				rtn = ((Hardware)hw).getPort(clnk.port1Index);
			if (hw instanceof GigEDescriptor)
			{
				if (clnk.port1Index >= 0)
					rtn = ((GigEDescriptor)hw).getPort(clnk.port1Index);
			}
			return rtn;
		}

		private ONLComponent getLinkPoint2(Cluster.Link clnk)
		{
			ONLComponent hw = (ONLComponent)hardware.elementAt(clnk.hw2Index);
			ONLComponent rtn = hw;
			if (hw instanceof Hardware)
				rtn = ((Hardware)hw).getPort(clnk.port2Index);
			if (hw instanceof GigEDescriptor)
			{
				if (clnk.port2Index >= 0)
					rtn = ((GigEDescriptor)hw).getPort(clnk.port2Index);
			}
			return rtn;
		}

		public void addHardware(ONLComponent hw) { addHardware(hw, -1);}
		public void addHardware(ONLComponent hw, int ndx)
		{
			if (!hardware.contains(hw))
			{
				if (ndx < 0) hardware.addElement(hw);
				else hardware.add(ndx, hw);
				hw.setCluster(this);
				//experiment.addNode(hw);
			}
		}

		public void addLink(LinkDescriptor lnk) { addLink(lnk, -1);}
		public void addLink(LinkDescriptor lnk, int ndx)
		{
			if (!links.contains(lnk))
			{
				if (ndx < 0) links.addElement(lnk);
				else links.add(ndx, lnk);
				lnk.setFixed(true);
				lnk.setCluster(this);
				//experiment.addLink(lnk);
			}
		}

		public void setState(String s)
		{
			if (!s.equals(state))
			{
				state = new String(s);
				//if (state.equals(ONLComponent.ACTIVE))
				// edit.commitEdits();
				//if (state.equals(ONLComponent.FAILED))
				// {
				// }
			}
		}

		public void setIndex(int i) { index = i;}
		public int getIndex() { return index;}

		public String getTypeLabel() { return (type.getLabel());}
		public Cluster getType() { return type;}

		protected String getCPAddr()
		{
			int max = hardware.size();
			String rtn = null;
			ONLComponent elem;
			for (int i = 0; i < max; ++i)
			{
				elem = hardware.onlComponentAt(i);
				rtn = elem.getProperty(Hardware.CPREQ);
				if (rtn != null && rtn.length() > 0) break;
			}
			return rtn;
		}
		public int getNumHardware() { return (hardware.size());}
		public ONLComponent getHardwareAt(int i) { return (hardware.onlComponentAt(i));}
		public boolean contains(ONLComponent onlc)
		{
			if (hardware.contains(onlc)) return true;
			if (links.contains(onlc)) return true;
			return false;
		}
		public ONLComponent.Undoable getDeleteEdit() { return (new DeleteClusterEdit(this));}
		public void addEdit(Experiment exp)
		{
			if (exp.isLive()) 
				ExpCoordinator.theCoordinator.addEditAt((new AddClusterEdit(this, exp)), 0);
		}
		public void merge(Cluster.Instance cli) {}
	}//end class Cluster.Instance
	////////////////////////////////////////////////// class Cluster ////////////////////////////////////////////////////

	public Cluster(String l, int v, String i, Vector hw, Vector lnks)
	{
		this(l,v,i);
		hardware.addAll(hw);
		links.addAll(lnks);
	}

	public Cluster(String l, int v, String i)
	{
		label = l;
		version = v;
		id = i;
		hardware = new Vector();
		links = new Vector();
	}

	public void read(ONL.Reader rdr) throws IOException
	{
		label = rdr.readString();
		version = rdr.readInt();
		id = rdr.readString();
		int max = rdr.readInt();
		int i;
		for (i = 0; i < max; ++i)
		{
			hardware.add(new String(rdr.readString()));
		}
		max = rdr.readInt();
		for (i = 0; i < max; ++i)
		{
			links.add(new Cluster.Link(rdr));
		}
	}

	public void write(ONL.Writer wrtr) throws IOException
	{
		wrtr.writeString(label);
		wrtr.writeInt(version);
		wrtr.writeString(id);
		int max = hardware.size();
		wrtr.writeInt(max);
		String lbl;
		int i;
		for (i = 0; i < max; ++i)
		{
			lbl = (String)hardware.elementAt(i);
			wrtr.writeString(lbl);
		}
		max = links.size();
		wrtr.writeInt(max);
		Cluster.Link lnk;
		for (i = 0; i < max; ++i)
		{
			lnk = (Cluster.Link)links.elementAt(i);
			lnk.write(wrtr);
		}
	}
	public String getID() { return id;}
	public void addHardware(String hw) { hardware.add(hw);}
	public void addLink(Cluster.Link lnk) { links.add(lnk);}
	public String getLabel() { return label;}
	public static Cluster getClusterType(ONL.Reader rdr) throws java.io.IOException
	{
		return (getClusterType(rdr.readString()));
		//String tp_nm = rdr.readString();
		//int tp_int = rdr.readInt();
		/*
      if (tp_int == ONLComponent.NSP) return (new NSPCluster());
      if (str_tp.equals(ONLComponent.NPR_LBL) && tp_int == ONLComponent.NPR) return (new NPRCluster());
      if (str_tp.equals(ONLComponent.IXP_LBL) && tp_int == ONLComponent.IXP) return (new IXPCluster());
		 */
		//return null;
	}
	public static Cluster getClusterType(String tp_nm)
	{
		if (tp_nm.equals(ONLComponent.NPR_LBL)) return (new NPRCluster());
		if (tp_nm.equals(ONLComponent.IXP_LBL)) return (new IXPCluster());
		return null;
	}
}
