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
 * File: Experiment.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 12/02/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.undo.*;
import java.beans.*;
import java.lang.reflect.Array;
import javax.xml.stream.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;

//File Format
// NAME
// String
// TOPOLOGY
// NODES
// label type
//     .
//     .
//     .
// END  //end NODES
// LINKS
// label type
// direction
// bandwidth 
// start component
// end component
//     .
//     .
//     .
// END  //end LINKS
// DISPLAY
// label type : location_x location_y
//     .
//     .
//     .
// END  //end DISPLAY
// END  //end TOPOLOGY




public class Experiment //implements MenuFileAction.Saveable
{
	public static final String FILE_SUFFIX = "exp"; 
	protected static final String NAME_TOK = "NAME";
	protected static final String TOPOLOGY_TOK = "TOPOLOGY";
	protected static final String NODES_TOK = "NODES";
	protected static final String LINKS_TOK = "LINKS";
	protected static final String DISPLAY_TOK = "DISPLAY";
	protected static final String PARAMS_TOK = "PARAMS";
	protected static final String MONITOR_TOK = "MONITORING";
	protected static final String CLUSTERS_TOK = "CLUSTERS";
	protected static final String NUM_CLUSTERS = "num_clusters";
	protected static final String END_TOK = "END";
	//create save and save as actions
	//private String[] description = null; //later
	private File file = null;
	protected ExpCoordinator expCoordinator = null;

	private Observer observer = null;

	//private list of plugins, tgs, apps, filters, etc.
	private ONLComponentList params = null;

	protected ONLPropertyList properties = null;
	//property names
	public static final String ONLID = "onlid";
	public static final String LABEL = "label";
	public static final String EMAIL = "email";
	public static final String DURATION_HRS = "duration_hours";
	public static final String DURATION_MINS = "duration_mins";
	public static final String FAILURE = "failure";
	public static final String LIVE = "live";


	//private SaveAction saveAction = null;
	//private MenuFileAction saveasAction = null;
	//private MenuFileAction loadAction = null;

	private Topology topology = null;
	private VirtualTopology vtopology = null;

	private Vector eventListeners = null;
	private int nextRefNum = 1;

	////////////////////////////////////////////////PropertyListener/////////////////////////////////////////////////////////////////////
	//public static interface PropListener extends PropertyChangeListener
	//{
	//public boolean removeAtClose();
	//public void setExperiment(Experiment e);
	//public Experiment getExperiment();
	//}

	//////////////////////////////////////////////// Snapshot ///////////////////////////////////////////////////////////////////////////
	public static class Snapshot
	{
		public ONLComponentList nodes = null;
		public ONLComponentList links = null;
		public ONLComponentList params = null;
		public Vector clusters = null;
		public Experiment experiment = null;
		public MonitorManager.Snapshot monitorDisplays = null;
		public Snapshot(Experiment exp)
		{
			experiment = exp;
			nodes = new ONLComponentList(experiment.topology.getNodes());
			links = new ONLComponentList(experiment.topology.getLinks());
			params = new ONLComponentList(experiment.params);
			ExpCoordinator.print(new String("Snapshot for " + experiment.getLabel() + ":" + experiment.getID() + " experiment.params: " + experiment.params.size() + " params: " + params.size()), Observer.TEST_OBS);
			clusters = new Vector(experiment.topology.getClusters());
			monitorDisplays = new MonitorManager.Snapshot(experiment.expCoordinator.getMonitorManager());
		}
	}
	//////////////////////////////////////////////// Event //////////////////////////////////////////////////////////////////////////////
	public static class Event extends EventObject
	{
		public static final int ADD = 16;
		public static final int REMOVE = 32;
		public static final int NODE = 1;
		public static final int LINK = 2;
		public static final int PARAM = 4;
		public static final int CLUSTER = 8;
		private int type = ADD|NODE;
		private ONLComponent onlComponent = null;
		private Cluster.Instance cluster = null;

		public Event(ONLComponent oc, Experiment e, int tp)
		{
			super(e);
			type = tp;
			onlComponent = oc;
		}

		public Event(Cluster.Instance cl, Experiment e, int tp)
		{
			super(e);
			type = tp;
			cluster = cl;
		}

		public int getType() { return type;}
		public ONLComponent getONLComponent() { return onlComponent;}
		public Cluster.Instance getCluster() { return cluster;}
	}

	private class AddEvent extends Event
	{
		public AddEvent(ONLComponent oc, Experiment e) { super(oc, e, Experiment.Event.ADD);}
		public AddEvent(Cluster.Instance cl, Experiment e) { super(cl, e, Experiment.Event.ADD);}
	}
	private class RemoveEvent extends Event
	{
		public RemoveEvent(ONLComponent oc, Experiment e) { super(oc, e, Experiment.Event.REMOVE);}
		public RemoveEvent(Cluster.Instance cl, Experiment e) { super(cl, e, Experiment.Event.REMOVE);}
	}

	///////////////////////////////////////// Listener /////////////////////////////////////////////////////////////////////////////////
	public static interface Listener
	{
		public void experimentChanged(Experiment.Event e);
	}


	////////////////////////////////////////////// NCCP_ClearExpReq /////////////////////////////////////////////////////////////////////
	public static class NCCP_ClearExpReq extends NCCP.RequesterBase
	{
		private Experiment experiment = null;
		public NCCP_ClearExpReq(Experiment e)
		{
			super(ExpDaemon.NCCP_Operation_ClearExperiment);
			experiment = e;
			setMarker(new REND_Marker_class());
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			ExpCoordinator.print("Experiment.NCCP_ClearExpReq.storeData", 4);
			NCCP.writeString(experiment.getProperty(Experiment.ONLID), dout);
		}
	}

	public static class Edit extends AbstractUndoableEdit implements ONLComponent.Undoable
	{
		public static final int ADD = 0;
		public static final int REMOVE = 1;
		public static final int OTHER = 2;
		protected ONLComponent onlComponent = null;
		private boolean committed = false;
		protected Experiment experiment = null;
		private int type = REMOVE;
		private Class editClass = null;
		protected Vector descendants = null;
		private boolean is_undoable = true;

		private Edit()
		{
			super();
			descendants = new Vector();
		}

		public Edit(ONLComponent oc, Experiment exp) { this(oc, exp, OTHER);}
		public Edit(ONLComponent oc, Experiment exp, int tp)
		{
			super();
			experiment = exp;
			type = tp;
			onlComponent = oc;
			Edit ec = new Edit();
			editClass = ec.getClass();
			descendants = new Vector();
		}
		public void commit() 
		{ 
			//ExpCoordinator.printer.print("Experiment.Edit.commit");
			if (!committed)
			{
				//ExpCoordinator.printer.print("calling sendMessage");
				sendMessage();
				//do the real thing and send a message
			}
			committed = true;
		}
		public void actionPerformed(ActionEvent e)
		{
			if (experiment == null) return;
			//ldescriptor.setLinked(false);
			if (isRemove()) experiment.removeComponent(onlComponent, descendants);
			if (isAdd()) experiment.addComponent(onlComponent, descendants);
		}
		public void undo() throws CannotUndoException
		{
			super.undo();
			if (experiment == null) return;
			if (isAdd()) experiment.removeComponent(onlComponent, descendants);
			if (isRemove()) experiment.addComponent(onlComponent, descendants);
		}
		public void redo() throws CannotRedoException
		{
			super.redo();
			if (experiment == null) return;
			if (isRemove()) experiment.removeComponent(onlComponent, descendants);
			if (isAdd()) experiment.addComponent(onlComponent, descendants);
		}
		protected boolean isEditInstance(ONLComponent.Undoable un)
		{
			return (editClass.isInstance(un));
		}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (isEditInstance(un))//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
			{
				Edit edit = (Edit) un;
				if (isRemove() && onlComponent.isEqual(edit.onlComponent) && edit.isAdd()) return true;
				if (isAdd() && onlComponent.isEqual(edit.onlComponent) && edit.isRemove()) return true;
			}
			return false;
		}
		public boolean isRemove() { return (type == REMOVE);}
		public boolean isDelete() { return (type == REMOVE);}
		public boolean isAdd() { return (type == ADD);}
		public boolean isOther() { return (type == OTHER);}
		public void setExperiment(Experiment exp) { experiment = exp;}
		public Experiment getExperiment() { return experiment;}
		public ONLComponent getONLComponent() { return onlComponent;}
		protected void sendMessage() {}
		public boolean isCommitted() { return committed;}
		protected void setIsUndoable(boolean b) { is_undoable = b;}
		public boolean canUndo()
		{
			if (is_undoable) return (super.canUndo());
			else return false;
		}
		public boolean canRedo()
		{
			if (is_undoable) return (super.canRedo());
			else return false;
		}
	}


	public static class AddEdit extends Edit
	{
		public AddEdit(ONLComponent ld)
		{
			this(ld, null);
		}
		public AddEdit(ONLComponent ld, Experiment exp)
		{
			super(ld, exp, Edit.ADD);
		}
	}


	public static class DeleteEdit extends Edit
	{
		public DeleteEdit(ONLComponent ld)
		{
			this(ld, null);
		}
		public DeleteEdit(ONLComponent ld, Experiment exp)
		{
			super(ld, exp, Edit.REMOVE);
		}
	}

	protected static class NCCP_AddComponent extends ExpRequest implements Reservation.Reservable
	{
		private ONLComponent onlComponent = null;
		private boolean reserved = false;

		private class NCCP_Requester extends ExpDaemon.NCCP_Requester
		{
			private String label = null;
			public NCCP_Requester(Experiment e)
			{
				super(ExpDaemon.NCCP_Operation_AddComponent, onlComponent, e);
			}

			//public void storeData(DataOutputStream dout) throws IOException
			public void storeData(DataOutputStream dout) throws IOException
			{
				super.storeData(dout);
				label = onlComponent.getLabel();
				ONLComponent.PortInterface linkedTo = null;
				ONL.NCCPWriter wrtr = new ONL.NCCPWriter(dout);
				if (onlComponent instanceof GigEDescriptor)
				{
					wrtr.writeString(((GigEDescriptor)onlComponent).getIPAddr().toString());
					wrtr.writeInt(0);//write reboot params
					wrtr.writeInt(0);//write init params
				}
				if (onlComponent instanceof NSPDescriptor)
				{
					wrtr.writeString(onlComponent.getProperty(NSPDescriptor.IPBASEADDR));
					//write reboot params
					String tmp_str = onlComponent.getProperty(NSPDescriptor.BITFILE);
					if (tmp_str != null)
					{
						wrtr.writeInt(1);
						wrtr.writeShort(CommandSpec.STRING);
						wrtr.writeString(tmp_str);
					}
					else 
						wrtr.writeInt(0);
					wrtr.writeInt(0);//write init params
				}
				else
				{
					if (onlComponent instanceof Hardware)
					{
						wrtr.writeString(onlComponent.getProperty(NSPDescriptor.IPBASEADDR));
						((Hardware)onlComponent).writeRebootParams(wrtr);//wrtr.writeInt(0);//write reboot params
						((Hardware)onlComponent).writeInitParams(wrtr);//wrtr.writeInt(0);//write init params
					}
				}
				//write cluster info if component belongs to a cluster
				onlComponent.setCommittedLabel(label);
				if (getOp() == ExpDaemon.NCCP_Operation_ReservationComponent) onlComponent.getExpCoordinator().addToProgressBar(onlComponent);
			}
			public String getLabel() { return label;}
		}

		private class NCCP_Response extends ExpDaemon.NCCP_Response
		{
			private ONLComponent onlComp = null;

			public NCCP_Response(ONLComponent c) 
			{ 
				super();
				onlComp = c;
			}
			public void retrieveData(DataInput din) throws IOException
			{
				super.retrieveData(din);
				ONL.NCCPReader reader = new ONL.NCCPReader(din);
				ExpCoordinator.print(new String("Experiment.NCCP_AddComponent.NCCP_Response.retrieveData " + getRendMarker().toString()), 7);
				if (onlComp != null) 
				{
					onlComp.getExpCoordinator().removeFromProgressBar(onlComp);
					String tmp_lbl = getComponentID();
					onlComp.setProperty(ONLComponent.MAPPEDTO, tmp_lbl);
					onlComp.readParams(reader);
					ExpCoordinator.print(new String("Experiment.NCCP_AddComponent.NCCP_Response.retrieveData component = " + onlComp.toString() + " mapped to " + tmp_lbl), 3);
					if (endOfMessage == 1)
					{
						int error_type = StatusDisplay.Error.LOG;
						if (onlComp instanceof Hardware) error_type = error_type | StatusDisplay.Error.LOG | StatusDisplay.Error.STATUSBAR;

						switch ((int)status)
						{
						case NCCP.Status_OperationIncomplete:
							onlComp.getExpCoordinator().addError(new StatusDisplay.Error((new String(onlComp.getLabel() + " Allocation Error: Resources in use. Try again later.\n")),
									"Resources in Use.",
									"Allocation Error",
									error_type));                        
							break;
						case NCCP.Status_Failed:
							onlComp.getExpCoordinator().addError(new StatusDisplay.Error((new String(onlComp.getLabel() + " Initialization Error.\n")),
									"Initialization Error.",
									"Initialization Error",
									error_type));                       
							break;
						}

						ExpCoordinator.print(new String("Experiment.NCCP_AddComponent.NCCP_Response.retrieveData return from removeFromProgressBar"), 7);
					}
				}
				else
					ExpCoordinator.printer.print("Experiment.NCCP_AddComponent.NCCP_Response::retrieveData component is NULL, watch for falling rocks");
			}	    
		}

		public NCCP_AddComponent(ONLComponent ld, Experiment e)
		{
			super();
			onlComponent = ld;
			request = new NCCP_Requester(e);
			response = new NCCP_Response(onlComponent);
		}
		public void processResponse(NCCP.ResponseBase r)
		{
			ExpCoordinator expc = onlComponent.getExpCoordinator();
			Experiment exp = ((ExpDaemon.NCCP_Requester)request).getExperiment();
			ExpCoordinator.print(new String("NCCP_AddComponent.processResponse for " + onlComponent.getLabel() + " status = " + r.getStatus()),1);
			switch ((int)r.getStatus())
			{
			case NCCP.Status_Fine:
				if (((NCCP_Requester)request).getReservationID() == null)
				{
					onlComponent.setCommittedLabel(((NCCP_Requester)request).getLabel());
					onlComponent.setState( ONLComponent.ACTIVE);
				}
				else reserved = true;
				break;
			default :
				if ((((NCCP_Requester)request).getReservationID()) == null)
				{
					onlComponent.setState( ONLComponent.FAILED); 
					exp.setProperty(FAILURE, true);
					onlComponent.clear();
				}
			break;
			}
			onlComponent.getExpCoordinator().removeRequest(this);
		}
		public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException
		{
			super.retrieveData(hdr, din);
			//if (response != null)
			ExpCoordinator.print("Experiment.NCCP_AddComponent.retrieveData", 5); 
		}
		public boolean isReserved() { return reserved;} //Reservation.Reservable interface
		protected void setReservation(String id_str, int opc)
		{
			if (request != null)
			{
				request.setOp(opc);
				((ExpDaemon.NCCP_Requester)request).setReservationID(id_str);
				ExpCoordinator.theCoordinator.removeFromProgressBar(onlComponent);
			}
		}
	}//end NCCP_AddComponent

	protected static class NCCP_DeleteComponent extends ExpRequest
	{
		private ONLComponent onlComponent = null;
		public NCCP_DeleteComponent(ONLComponent ld, Experiment e)
		{
			super();
			onlComponent = ld;
			request = new  ExpDaemon.NCCP_Requester(ExpDaemon.NCCP_Operation_DeleteComponent, ld, e);
			response = new ExpDaemon.NCCP_Response();
		}
		public void processResponse(NCCP.ResponseBase r)
		{
			if (r.getStatus() == NCCP.Status_Fine) onlComponent.setState( ONLComponent.NOT_INIT);
			onlComponent.getExpCoordinator().removeRequest(this);
		}
	}//end NCCP_DeleteComponent

	public static class AddNodeEdit extends AddEdit
	{
		public AddNodeEdit(ONLComponent oc, Experiment exp)
		{
			super(oc, exp);
		}
		protected void sendMessage()
		{
			//ExpCoordinator.printer.print(new String("AddNodeEdit::sendMessage for " + getONLComponent().getLabel()));
			ExpCoordinator ec = getONLComponent().getExpCoordinator();
			//if (!ec.isTestLevel(ExpCoordinator.NPTEST))
			ec.sendRequest(new NCCP_AddComponent(getONLComponent(), getExperiment()));
			//else //testing np router interface
			// {
			// ONLComponent oc = getONLComponent();
			/*            if (oc instanceof NPDescriptor)
              {
                ec.removeFromProgressBar(oc);
                oc.setProperty(ONLComponent.MAPPEDTO, oc.getLabel());
                oc.setCommittedLabel(oc.getLabel());
                oc.setState( ONLComponent.ACTIVE);
                }*/
			// }
		}
		protected void sendUndoMessage() {}
	}

	public static class DeleteNodeEdit extends DeleteEdit
	{
		public DeleteNodeEdit(ONLComponent oc, Experiment exp)
		{
			super(oc, exp);
		}
		protected void sendMessage() 
		{
			ExpCoordinator ec = getONLComponent().getExpCoordinator();
			ec.sendRequest(new NCCP_DeleteComponent(getONLComponent(), getExperiment()));
			getONLComponent().clear();
		}
		protected void sendUndoMessage() {}
	}
	/*
  private class SaveAction extends MenuFileAction
  {
    private Experiment experiment = null;
    public SaveAction(Experiment e)
      {
	super(e, false, null, "save", false, true);
	experiment = e;
	setSuffix(FILE_SUFFIX);
      }
    public void actionPerformed(ActionEvent e)
      {
	if (experiment.getFile() == null) super.actionPerformed(e);
	else experiment.saveToFile();
      }
  }
	 */
	private class ReadFunction
	{
		public ReadFunction(){}
		public void processString(String str, ONL.Reader reader) throws IOException {}
	}
	/*
  public Experiment(ExpCoordinator ec, File f) { this(ec, f, null, true);}
  public Experiment(ExpCoordinator ec, File f, Topology topo, Boolean lv)
    {
      this(ec, "", topo, lv);
      try
	{
	  file = f;
	  FileReader fr = new FileReader(f);
	  char buf[] = new char[7];
	  fr.read(buf, 0, 7);
	  fr.close();
	  String str = new String(buf);
	  if (str.equals("VERSION"))
	    {
	      ONL.BaseFileReader frdr = new ONL.BaseFileReader(f);
	      frdr.setCountAllLines(true);//blank lines mean something
	      read(frdr);
	    }
	  //else
	  //{
	  //  XMLReader xmlReader = XMLReaderFactory.createXMLReader();
	  //  ExperimentXML exp_xml = new ExperimentXML(this, xmlReader, f);
	  //  xmlReader.parse(new InputSource(new FileInputStream(f)));
	  // }
	}
      catch (IOException e)
	{
	  System.err.println("Experiment - error could not open file:" + file.getAbsolutePath() + " for reading");
	}
      //catch(SAXException e2)
      //{
      //  ExpCoordinator.print(new String("ExpXMLFileAction.loadFromFile SAXException " + e2.getMessage()));
      //}
    }
	 */
	public Experiment(ExpCoordinator ec, String nm) { this(ec, nm, null, true);}
	public Experiment(ExpCoordinator ec, String nm, Topology topo, Boolean lv)
	{
		expCoordinator = ec;
		topology = topo;
		if (topo == null)
			topology = expCoordinator.getTopology();
		eventListeners = new Vector();
		//saveAction = new SaveAction(this);
		//expCoordinator.addAction(saveAction, ExpCoordinator.SAVEFILE);
		params = new ONLComponentList();
		properties = new ONLPropertyList(this);
		properties.setProperty(LABEL, nm);
		//properties.setProperty(EMAIL, ec.getProperty(EMAIL));
		properties.setProperty(DURATION_HRS, 0);
		properties.setProperty(DURATION_MINS, 0);
		properties.setProperty(ONLComponent.STATE, ONLComponent.NOT_INIT);
		properties.setProperty(FAILURE, false);
		properties.setProperty(LIVE, lv);
	}
	public VirtualTopology getVirtualTopology() 
	{
		if (vtopology == null) vtopology = new VirtualTopology(expCoordinator);
		return vtopology;
	}
	public boolean isLive() 
	{ 
		//if (ExpCoordinator.isTestLevel(ExpCoordinator.CMPTEST)) 
		return (properties.getPropertyBool(LIVE));
		//else 
		//return true;
	}
	public void setObserver(Observer o)
	{
		if (o == null) removeObserver();
		else
		{
			if (observer == null)
			{
				observer = o;
				addExpListener(observer);
				expCoordinator.getMonitorManager().addGraphListener(observer);
				expCoordinator.addStatusListener(observer);
			}
		}
	}
	//public void writeTopologySummary(ONL.Writer writer) throws java.io.IOException { topology.writeNodeSummary(writer);}
	public void writeXML(BufferedWriter wrtr) throws java.io.IOException
	{
		try{
			XMLStreamWriter xmlWrtr = XMLOutputFactory.newInstance().createXMLStreamWriter(wrtr);
			xmlWrtr.setDefaultNamespace("onl");
			xmlWrtr.setPrefix("onl", "onl");
			xmlWrtr.writeStartDocument();
			//ONL.XMLSWriter xmlWrtr = new ONL.XMLSWriter(xml_wrtr,wrtr);
			xmlWrtr.writeStartElement(ExperimentXML.EXP);
			xmlWrtr.writeAttribute(ExperimentXML.VERSION, String.valueOf(ExpCoordinator.VERSION));
			xmlWrtr.writeAttribute(ExperimentXML.EXPNAME, properties.getProperty(LABEL));
			if (ExpCoordinator.isOldSubnet()) xmlWrtr.writeAttribute(ExperimentXML.OLDSUBNET, String.valueOf(true));
			//write main window xy coordinates
			ONLMainWindow mw = expCoordinator.getMainWindow();
			xmlWrtr.writeStartElement(ExperimentXML.MWPOSITION);
			xmlWrtr.writeAttribute(ExperimentXML.XCOORD, String.valueOf(mw.getX()));
			xmlWrtr.writeAttribute(ExperimentXML.YCOORD, String.valueOf(mw.getY()));
			xmlWrtr.writeEndElement();//mainWindowPosition
			//write main window width and height
			xmlWrtr.writeStartElement(ExperimentXML.MWSIZE);
			xmlWrtr.writeAttribute(ExperimentXML.WIDTH, String.valueOf(mw.getWidth()));
			xmlWrtr.writeAttribute(ExperimentXML.HEIGHT, String.valueOf(mw.getHeight()));
			xmlWrtr.writeEndElement();//mainWindowSize
			//TOPOLOGY
			xmlWrtr.writeStartElement(ExperimentXML.TOPO);
			topology.writeXML(xmlWrtr);
			xmlWrtr.writeEndElement();//Topology
			//MONITORING
			xmlWrtr.writeStartElement(ExperimentXML.MONITORING);
			//xmlWrtr.writeCharacters("<![CDATA[");
			//ExperimentXML.ONLStringWriter onl_strwrtr = new ExperimentXML.ONLStringWriter();
			//expCoordinator.getMonitorManager().write(onl_strwrtr);
			//xmlWrtr.writeProcessingInstruction(ExperimentXML.MONITORING, onl_strwrtr.getBuffer().toString());
			expCoordinator.getMonitorManager().writeXML(xmlWrtr);
			xmlWrtr.writeEndElement(); //Monitoring
			//VirtualTOPOLOGY
			if (vtopology != null && vtopology.getNumNodes() > 0)
			{
				xmlWrtr.writeStartElement(ExperimentXML.VTOPO);
				vtopology.writeXML(xmlWrtr);
				xmlWrtr.writeEndElement();//Topology
			}
			//end Experiment
			xmlWrtr.writeEndElement();//Experiment
			xmlWrtr.writeEndDocument();
			xmlWrtr.close();
		}
		catch(XMLStreamException e)
		{
			ExpCoordinator.print(new String("Experiment.writeXML XMLStreamException " + e.getMessage()));
			e.printStackTrace();
		}
	}
	/*
  public void write(ONL.Writer writer)
    {
      //ExpCoordinator.printer.print("Experiment.write");
      try
	{
          //write version
          if (!(writer instanceof ONL.BaseFileWriter))
            {
              writer.writeString(ONL.VERSION_TOK);
              writer.writeDouble(ExpCoordinator.VERSION);
            }
	  //write name
	  writer.writeString(NAME_TOK);
	  writer.writeString(properties.getProperty(LABEL));

	  //write topology
	  writer.writeString(TOPOLOGY_TOK);      
	  writer.writeString(CLUSTERS_TOK);  
	  writeClusters(topology.getClusters(), writer);
	  writer.writeString(END_TOK); //end nodes       
	  writer.writeString(NODES_TOK); 
	  writeComponentList(topology.getNodes(), writer);
	  writer.writeString(END_TOK); //end nodes     
	  writer.writeString(LINKS_TOK); 
	  writeComponentList(topology.getLinks(), writer);       
	  writer.writeString(END_TOK);   //end links   
	  writer.writeString(DISPLAY_TOK);
          ONLMainWindow mw = expCoordinator.getMainWindow();
          writer.writeString(new String(mw.getX() + " " + mw.getY() + " " + mw.getWidth() + " " + mw.getHeight()));
	  writeDisplays(topology.getNodes(), writer);
	  writer.writeString(END_TOK);   //end display
	  writer.writeString(END_TOK); //end topology
	  writer.writeString(PARAMS_TOK);
	  writeComponentList(params, writer);   
	  writer.writeString(END_TOK); //end params
          writer.writeString(MONITOR_TOK);
          //expCoordinator.getMonitorManager().saveToFile(writer); //write monitoring stuff
          expCoordinator.getMonitorManager().write(writer); //write monitoring stuff
	  writer.finish();
	}
      catch (IOException e)
	{
	  System.err.println("Experiment::write - error writing");
	}

    }
  public void read(ONL.Reader rdr) { read(rdr,false);}
  public void read(ONL.Reader rdr, boolean skipMon)
    {
      //Experiment.Reader reader = new Experiment.Reader(rdr);
      ONL.Reader reader = rdr;
      try
	{
	  if (!readName(reader)) return;
	  if (!readTo(reader, TOPOLOGY_TOK)) return;
	  if (rdr.getVersion() > 3.0)
	    {
	      //read clusters
	      if (!read(reader, 
			(new ReadFunction() 
			{
			  public void processString(String str, ONL.Reader reader) throws IOException
			  {
			    readClusters(str, reader);
			  }
			}),
			CLUSTERS_TOK)) return;
	    }
	  //read nodes
	  if (!read(reader, 
		    (new ReadFunction() 
		    {
		      public void processString(String str, ONL.Reader reader) throws IOException
		      {
			readComponent(str, reader);
		      }
		    }),
		    NODES_TOK)) return;

	  //read links
	  if (!read(reader, 
		    (new ReadFunction() 
		    {
		      public void processString(String str, ONL.Reader reader) throws IOException
		      {	
			readLink(str, reader);
		      }
		    }),
		    LINKS_TOK)) return;
	  topology.fillClusters(this);
	  //read displays
	  if (!read(reader, 
		    (new ReadFunction() 
		    {
		      private boolean location_set = false;
		       public void processString(String str, ONL.Reader reader) throws IOException
		       {
			 String[] strarray;
			 int a_len = 0;
			 if (!location_set && reader.getVersion() >= 1.3)
			   {
			     strarray = str.split(" ");//reader.readString().split(" ");
			     a_len = Array.getLength(strarray);
			     if (a_len >= 4)
                                 {
                                   ONLMainWindow mw = expCoordinator.getMainWindow();
                                   expCoordinator.setWindowBounds(mw,
                                                                  Integer.parseInt(strarray[0]), 
                                                                  Integer.parseInt(strarray[1]),
                                                                  Integer.parseInt(strarray[2]),
                                                                  Integer.parseInt(strarray[3]));
                                 }
			     location_set = true;
			     return;
                             }

			 readComponentLocation(str, reader);
		       }
		    }),
		    DISPLAY_TOK)) return; 
	  //read params - route & filter tables
	  if (!readTo(reader, END_TOK)) return; //end topology
	  if (!read(reader, 
		    (new ReadFunction() 
		     {
		       public void processString(String str, ONL.Reader reader) throws IOException
			 {
                           readParam(str,reader);
                         }
                      }),
                    PARAMS_TOK)) return;
	  //if (!readTo(reader, END_TOK)) return;
          if (!skipMon)
            {
              if (readTo(reader, MONITOR_TOK))
                {
                  //reader.setCountAllLines(true);
                  expCoordinator.getMonitorManager().loadFromReader(reader);
                }
            }
          if (reader.getVersion() != ExpCoordinator.VERSION)
            {
              String v_string = "older";
              if (reader.getVersion() > ExpCoordinator.VERSION) v_string = "newer";
              expCoordinator.addError(new StatusDisplay.Error(new String("Your Experiment was successfully loaded.\n However, experiment file was saved with a " + v_string + " RLI version.\n Please resave the experiment. \n"),
                                                              "Version Inconsistency.",
                                                              "Version Inconsistency.",
                                                              StatusDisplay.Error.POPUP | StatusDisplay.Error.LOG));     
            }
	}
      catch (IOException e)
	{
	  System.err.println("Experiment::read - error reading");
          String error_msg = "Your Experiment failed to load.\n ";
          if (reader.getVersion() != ExpCoordinator.VERSION)
            {
              String v_string = "older";
              if (reader.getVersion() > ExpCoordinator.VERSION) v_string = "newer";
              error_msg = error_msg.concat(new String("Experiment file was saved with a " + v_string + " RLI version.\n Please resave the experiment. \n"));
            }
          expCoordinator.addError(new StatusDisplay.Error(error_msg,
                                                          "Experiment load error.",
                                                          "Experiment load error.",
                                                          StatusDisplay.Error.POPUP | StatusDisplay.Error.LOG));  
          expCoordinator.closeCurrentExp();
	}
    }
  private boolean readTo(ONL.Reader reader, String token) throws IOException
    {
      while (reader.ready())
	{	
	  if (reader.readString().equals(token)) return true;
	}
      return false;
    }
  private boolean read(ONL.Reader reader, ReadFunction rfunction, String token) throws IOException
    {
      String str;
      boolean found_start = false;
      boolean found_end = false;
      ExpCoordinator.printer.print(new String("Experiment::read token: " + token), 1);
      while (reader.ready())
	{	
	  str = reader.readString();
          ExpCoordinator.printer.print(new String("                 string: " + str), 1);
	  if (str.equals(token)) 
	      {
		  ExpCoordinator.printer.print(new String("                 found start"), 1);
		  found_start = true;
	      }
	  else
	    {
	      if (str.equals(END_TOK)) 
                {
                  found_end = true;
                  break;
                }
	      else
		{
		  if (found_start)
		    {
		      rfunction.processString(str, reader);
		    }
		}
	    }
	}
      if (found_start && found_end) return true;
      else 
	{
	  System.err.println("Experiment::read " + token + " error - not correct format");
	  return false;
	}
    }

  private boolean readName(ONL.Reader reader) throws IOException
    {
      //read and set name
      boolean version_found = false;
      if (reader instanceof ONL.BaseFileReader) version_found = true; //this cl
      while (reader.ready())
	{
          String ln = reader.readString();
          ExpCoordinator.print(new String("Experiment.readName: " + ln), 2);
	  if (ln.equals(ONL.VERSION_TOK)) 
	   {
             reader.setVersion(reader.readDouble());
	     version_found = true;
	   }
	 if (ln.equals(NAME_TOK)) 
	   {
	     properties.setProperty(LABEL, reader.readString());
             if (!version_found) reader.setVersion(0);
	     return true;
	   }
	}
      System.err.println("Experiment::readName error - not correct format");
      return false;
    }

  protected void readClusters(String str, ONL.Reader reader) throws IOException
    { 
      ExpCoordinator.print(new String("Experiment.readCluster " + str), 2);
      if (str.startsWith(NUM_CLUSTERS))
       {
	 String strarray[] = str.split(" ");
	 int num_clusters = Integer.parseInt(strarray[1]);
	 for (int i = 0; i < num_clusters; ++i)
	  {
	    Cluster.Instance cli = new Cluster.Instance(reader, this);
	  }
       }
    }
	 */
	protected ONLComponent readComponent(String str, ONL.Reader reader) throws IOException
	{ 
		ExpCoordinator.print(new String("Experiment.readComponent " + str), 2);
		String[] strarray = str.split(" ");
		String type = strarray[1];
		ONLComponent c = null;
		//Hardware hw ;
		if (type.equals(ONLComponent.NSP_LBL))
			c = new NSPDescriptor(strarray[0], reader, expCoordinator);
		else
		{
			if (type.equals(ONLComponent.VGIGE_LBL))
				c = new GigEDescriptor(strarray[0], type, reader, expCoordinator);
			else
			{
				c = new Hardware(strarray[0], reader, expCoordinator);
				if (((Hardware)c).getHWSubtype() != null)
					((Hardware)c).initializePorts();
				else c = null;
			}
		}
		if (c != null) 
		{
			//c = topology.addExpComponent(c);
			addNode(c);
		}
		ExpCoordinator.print(new String("Experiment.readComponent " + str + " end"), 5);
		return c;
	}
	protected LinkDescriptor readLink(String str, ONL.Reader reader) throws IOException
	{       
		//StringTokenizer str_tok = new StringTokenizer(str);
		String str_array[] = str.split(" ");
		//LinkDescriptor tmp_ld = new LinkDescriptor(str_tok.nextToken(), reader, expCoordinator);
		LinkDescriptor tmp_ld = new LinkDescriptor(str_array[0], reader, expCoordinator);
		if (tmp_ld.getPoint1() == tmp_ld.getPoint2())
		{
			expCoordinator.addError(new StatusDisplay.Error(new String("Loopback links to same endpoint no longer supported. Rejecting Link. from " + tmp_ld.getPoint1().getLabel() + " to " + tmp_ld.getPoint1().getLabel()), 
					"", 
					"", 
					StatusDisplay.Error.POPUP));
			return null;
		}
		tmp_ld = (LinkDescriptor)addLink(tmp_ld);
		return tmp_ld;
	}
	protected ONLComponent readParam(String str, ONL.Reader reader) throws IOException
	{
		String[] strarray = str.split(" ");	
		String type = strarray[1];
		reader.readInt(); //hw reference
		reader.readInt(); //cluster reference
		ONLComponent c = expCoordinator.getComponentFromString(reader.readString());
		ONLComponent param = null;
		boolean in;
		if (c == null) return null;
		if (c instanceof NSPPort)
		{
			if (type.equals(ONLComponent.RTABLE))
			{
				if (c != null)
				{
					param = ((RouterPort)c).getTableByType(ONLComponent.RTABLE);
					if (param != null) ((RouteTable)param).read(reader);
				}
				else RouteTable.skip(reader);
			}
			if (type.equals(ONLComponent.IPPEMFTABLE) ||
					type.equals(ONLComponent.IPPGMFTABLE) ||
					type.equals(ONLComponent.IPPEXGMFTABLE) ||
					type.equals(ONLComponent.OPPEMFTABLE) ||
					type.equals(ONLComponent.OPPGMFTABLE) ||
					type.equals(ONLComponent.OPPEXGMFTABLE))
			{
				if (c != null)
				{
					in = Boolean.valueOf(reader.readString()).booleanValue();
					param = ((NSPPort)c).getFilterTable(type);
					((FilterTable)param).read(reader);
				}
			}
			else FilterTable.skip(reader);
			if (type.equals(ONLComponent.IPPQTABLE) ||
					type.equals(ONLComponent.OPPQTABLE))
			{
				in = Boolean.valueOf(reader.readString()).booleanValue();
				param = ((NSPPort)c).getQueueTable(type);
				((QueueTable)param).read(reader);
			}
			if (type.equals(ONLComponent.PINSTANCES))
			{
				param = ((NSPPort)c).getPInstanceTable();
				((PluginInstanceTable)param).read(reader);
			}
			else
			{
				param = ((Hardware)c).getTableByType(type);
				((ONLCTable)param).read(reader);
			}
		}
		if (c instanceof NSPDescriptor && type.equals(ONLComponent.PCLASSES))
		{
			param = ((NSPDescriptor)c).getPluginClasses();
			//else
				// param = ((NPRouter.Instance)c).getPluginClasses();
			((PluginClasses)param).read(reader);
		}
		//PROBLEM:need to handle HARDWARE

		if (param != null && !containsParam((ONLComponent)param)) addParam((ONLComponent)param);
		return param;
	}

	protected void readComponentLocation(String str, ONL.Reader reader) throws IOException
	{
		ONLComponent c = expCoordinator.getComponentFromString(str);
		String[] strarray = reader.readString().split(" ");
		int x = Integer.parseInt(strarray[0]);
		int y = Integer.parseInt(strarray[1]);

		if (c != null) 
		{
			ONLGraphic ograph = c.getGraphic();
			if (ograph != null) 
			{
				ograph.setLocation(x, y);
				if ((c instanceof Hardware) && (Array.getLength(strarray) >= 3))
				{
					((HardwareGraphic)ograph).setSpinnerPosition(Integer.parseInt(strarray[2]));
				}
			}
		}
	}
	/*
  protected static void writeDisplays(ONLComponentList l, ONL.Writer writer) throws IOException
    {
      int max = l.size();
      ONLComponent elem;
      ONLGraphic elem_graph = null;
      for (int i = 0; i < max; ++i)
	{
	  elem = l.onlComponentAt(i);
	  elem_graph = elem.getGraphic();
	  if (elem_graph != null)
	    {
	      writer.writeString(elem.toString());
	      if (!(elem instanceof Hardware)) writer.writeString(new String(elem_graph.getX() + " " + elem_graph.getY()));
              else
                {
                  int spinner = ((HardwareGraphic)elem_graph).getSpinnerPosition();
                  writer.writeString(new String(elem_graph.getX() + " " + elem_graph.getY() + " " + spinner));
                }
	    }
	}
    }

  private void writeClusters(Vector l, ONL.Writer writer) throws IOException
    {
      int max = l.size();
      Cluster.Instance elem;
      writer.writeString(new String(NUM_CLUSTERS + " " + max));
      for (int i = 0; i < max; ++i)
       {
	 elem = (Cluster.Instance)l.elementAt(i);
	 elem.writeExpDescription(writer);
       }
    }

  private void writeComponentList(ONLComponentList l, ONL.Writer writer) throws IOException
    {
      int max = l.size();
      ONLComponent elem;
      for (int i = 0; i < max; ++i)
	{
	  elem = l.onlComponentAt(i);
	  ExpCoordinator.print(new String("Experiment.writeComponentList writing component:" + elem.getLabel()), 6);
	  elem.writeExpDescription(writer);
	}
    }

	 */
	public void setLabel(String nm) { properties.setProperty(LABEL, nm);}
	public String getLabel() { return (properties.getProperty(LABEL));}
	public String getProperty(String nm) {  return (properties.getProperty(nm));}
	public int getPropertyInt(String nm) {  return (properties.getPropertyInt(nm));}
	public boolean getPropertyBool(String nm) {  return (properties.getPropertyBool(nm));}
	public void setProperty(String nm, String val) {  properties.setProperty(nm,val);}
	public void setProperty(String nm, int val) {  properties.setProperty(nm,val);}
	public void setProperty(String nm, boolean val) {  properties.setProperty(nm,val);}
	public void setFile(File f) 
	{ 
		String tmp_nm = f.getName();
		if (tmp_nm.endsWith(FILE_SUFFIX))
		{
			int len = tmp_nm.length();
			int len2 = 1 + FILE_SUFFIX.length();
			setLabel(tmp_nm.substring(0, (len - len2)));
		}
		else setLabel(tmp_nm);
		file = f;
	}
	public File getFile() { return file;} 

	public void addComponent(ONLComponent c) { addComponent(c, null);}
	public void addComponent(ONLComponent c, Vector descendants) 
	{
		if (c instanceof LinkDescriptor) addLink(c);
		else 
		{
			if (c instanceof Hardware || c instanceof GigEDescriptor) 
			{
				addNode(c);
				if (descendants != null) addDescendants(descendants);
			}
			else addParam(c);
		}
	}
	public void removeComponent(ONLComponent c) { removeComponent(c, null);}
	public void removeComponent(ONLComponent c, Vector descendants) 
	{
		ExpCoordinator.printer.print(new String("Experiment.removeComponent " + c.getLabel()), 1);
		if (c instanceof LinkDescriptor) removeLink(c);
		else 
		{
			if (c instanceof Hardware || c instanceof GigEDescriptor) 
			{
				removeDescendants(c, descendants);
				removeNode(c);
			}
			else removeParam(c);
		}
		if (observer != null) c.removeONLCListener(observer);
	}
	public void removeCluster(int ref) { removeCluster(topology.getCluster(ref));}
	public void removeCluster(Cluster.Instance ci)
	{
		if (ci != null)
		{
			topology.removeCluster(ci);
			fireEvent(new Event(ci, this, Event.REMOVE|Event.CLUSTER));
		}
	}
	public void addCluster(Cluster.Instance ci)
	{
		if (ci != null)
		{
			topology.addCluster(ci);
			fireEvent(new Event(ci, this, Event.ADD|Event.CLUSTER));
		}
	}
	private void removeDescendants(ONLComponent c, Vector descendants)
	{
		Vector removals = getDescendants(c);
		if (descendants != null) descendants.addAll(removals);
		int max = removals.size();
		if (observer != null) 
		{
			for (int i = 0; i < max; ++i)
			{
				((ONLComponent)removals.elementAt(i)).removeONLCListener(observer);
			}
		}
		params.removeAll(removals);
	}
	private void addDescendants(Vector descendants)
	{
		int max = descendants.size();
		for (int i = 0; i < max; ++i)
		{
			addParam((ONLComponent)descendants.elementAt(i));
		}
	}
	private Vector getDescendants(ONLComponent c)
	{
		Vector rtn = new Vector();
		int max = params.size();
		ONLComponent elem;
		for (int i = 0; i < max; ++i)
		{
			elem = params.onlComponentAt(i);
			if (elem.isDescendant(c)) rtn.add(elem); 
		}
		return rtn;
	}
	public void addParam(ONLComponent c) 
	{
		if (!params.contains(c))
		{
			ExpCoordinator.print(new String("\nExperiment(" + getLabel() + ":" + getID() +").addParam " + c.getLabel() + " params:" + params.size()), 4);
			if (observer != null) c.addONLCListener(observer);
			else ExpCoordinator.print("   observer null", 8);
			params.addComponent(c);
			fireEvent(new Event(c, this, Event.ADD|Event.PARAM));
			ExpCoordinator.print(new String("    after add " + c.getLabel() + " params:" + params.size() +"\n"), 4);
			//if (observer != null) c.addONLCListener(observer);
		}
	}
	public void removeParam(ONLComponent c) 
	{
		if (params.contains(c))
		{
			ExpCoordinator.print(new String("\nExperiment.removeParam " + c.getLabel() + " params:" + params.size()), 4);
			params.removeComponent(c);
			fireEvent(new Event(c, this, Event.REMOVE|Event.PARAM));
		}
	}
	public boolean containsParam(ONLComponent c){ return (params.contains(c));}
	public ONLComponent addNode(ONLComponent oc) 
	{
		ExpCoordinator.print(new String("Experiment.addNode " + oc.getLabel() + " isLive:" + isLive()), 5);
		if (observer != null) oc.addONLCListener(observer);
		else ExpCoordinator.printer.print("   observer null", 8);
		ONLComponent c = topology.addComponent(oc);

		//if (c == null) return c; //node was already there
		
		if (!(c.isActive() || c.isWaiting()) && isLive()) 
		{
			expCoordinator.addEdit(new AddNodeEdit(c, this));
			//otherwise it's in a cluster and the edit will be added when the cluster becomes active
			//c.setState( ONLComponent.WAITING);
			if (ExpCoordinator.isSPPMon()) c.setProperty(ONLComponent.STATE, ONLComponent.ACTIVE);
			else c.setState( ONLComponent.WAITING);
			fireEvent(new Event(c, this, Event.ADD|Event.NODE));
		}
		if (c instanceof Hardware)
			ExpCoordinator.recordEvent(SessionRecorder.ADD_HARDWARE, new String[]{c.getLabel(), c.getProperty(ONLComponent.IPADDR)});
		else
		{
			if (c instanceof GigEDescriptor)
				ExpCoordinator.recordEvent(SessionRecorder.ADD_GIGE, new String[]{c.getLabel()}); 
		}
		
		return c;
	}  
	public void removeNode(ONLComponent c) 
	{ 
		ExpCoordinator.print(new String("Experiment.removeNode " + c.getLabel()), 5);
		if (topology.getONLComponent(c) == null) return;
		//check if fixed host if so don't remove, remove fixed hosts and links only when the assoc. NSP is removed
		c.setState( ONLComponent.WAITING);
		topology.removeComponent(c);
		fireEvent(new Event(c, this, Event.REMOVE|Event.NODE));
		if (c instanceof Hardware)
			ExpCoordinator.recordEvent(SessionRecorder.DELETE_HARDWARE, new String[]{c.getLabel(), c.getProperty(ONLComponent.IPADDR)});
		if (c instanceof GigEDescriptor)
			ExpCoordinator.recordEvent(SessionRecorder.DELETE_GIGE, new String[]{c.getLabel()});    
		//now remove cluster need to remove all links with endpoint c and all nodes connected via fixed links
	}
	public ONLComponent addLink(ONLComponent oc) //should add Edit be generated here
	{ 
		ONLComponent c = topology.addComponent(oc);
		//if (c == null) return c; //already there
		ExpCoordinator.printer.print(new String("Experiment::addLink " + c.getLabel()), 1);
		if (observer != null) c.addONLCListener(observer);
		else ExpCoordinator.printer.print("   observer null", 8);
		if (!(c.isActive() || c.isWaiting()) && isLive()) 
		{
			if (isLive()) 
				expCoordinator.addEdit(new LinkDescriptor.AddEdit((LinkDescriptor)c, topology, this));//expCoordinator.getCurrentExp()));
			//otherwise in cluster and cluster will add edit when cluster becomes active
			c.setState( ONLComponent.WAITING);
			fireEvent(new Event(c, this, Event.ADD|Event.LINK));
		}

		//record event for session
		if (ExpCoordinator.isRecording())
		{
			ONLComponent p1 = ((LinkDescriptor)c).getPoint1();
			ONLComponent p2 = ((LinkDescriptor)c).getPoint2();
			ExpCoordinator.recordEvent(SessionRecorder.ADD_LINK, new String[]{p1.getLabel(), p2.getLabel()});   
		}
		return c;
	}  
	public void removeLink(ONLComponent c) //should remove Edit be generated here
	{ 
		boolean fire_event = false;
		if (topology.getONLComponent(c) != null) fire_event = true;
		topology.removeComponent(c);
		if (fire_event)
			fireEvent(new Event(c, this, Event.REMOVE|Event.LINK));
		//record event for session
		if (ExpCoordinator.isRecording())
		{
			ONLComponent p1 = ((LinkDescriptor)c).getPoint1();
			ONLComponent p2 = ((LinkDescriptor)c).getPoint2();
			ExpCoordinator.recordEvent(SessionRecorder.DELETE_LINK, new String[]{p1.getLabel(), p2.getLabel()});   
		}
	}
	/*
  public void saveToFile()
    { 
      if (file != null) 
        {
          ExpCoordinator.print(new String("saveToFile() " + file), 2);
          saveToFile(file);
        }
    }

  //MenuFileAction.Saveable interface

  public void saveToFile(java.io.File f)
    {
      setFile(f);
      ExpCoordinator.print(new String("saveToFile " + file),2);
      try
	{
	  write(new ONL.BaseFileWriter(f));
	}
      catch(IOException e)
	{
	  System.err.println("Experiment::saveToFile - error could not open file:" + f.getAbsolutePath() + " for writing");
	}
    }
  public void loadTopology(java.io.File f)    
    {
      try
	{
	  read(new ONL.BaseFileReader(f), true);
	}
      catch(IOException e)
	{
	  System.err.println("Experiment::loadTopology - error could not open file:" + f.getAbsolutePath() + " for reading");
	}
    }
  public void loadFromFile(java.io.File f) { loadFromFile(f, true);}
  public void loadFromFile(java.io.File f, boolean set)
    {
      if (set) setFile(f);
      try
	{
	  read(new ONL.BaseFileReader(f));
	}
      catch(IOException e)
	{
	  System.err.println("Experiment::loadFromFile - error could not open file:" + f.getAbsolutePath() + " for reading");
	}
    }
	 */
	//end MenuFileAction.Saveable interface

	public void close()
	{
		//clear virtual topology
		if (vtopology != null) vtopology.clear();
		//remove from main window save action save action
		//expCoordinator.removeAction(saveAction, ExpCoordinator.SAVEFILE);
		//should cycle through nodes and links setting up removes for each or sending a single clear exp command
		//send clear command
		ONLComponentList nodes = topology.getNodes();
		int max = topology.getNumNodes();
		for (int i = 0; i < max; ++i)
		{
			nodes.onlComponentAt(i).clear();
		}
		//clear topology
		topology.clear();
		if (!expCoordinator.isSPPMon())
		{
			expCoordinator.sendMessage(new NCCP_ClearExpReq(this));
			removeObserver();
			SubnetManager.clear();
			OldSubnetManager.clear();
		}
	}
	public void removeObserver()
	{
		if (observer != null)
		{
			removeExpListener(observer);
			expCoordinator.getMonitorManager().removeGraphListener(observer);
			expCoordinator.removeStatusListener(observer);
			observer = null;
		}
	}
	public boolean isEmpty() { return (topology.getNumNodes() == 0 && params.isEmpty());}
	public void addPropertyListener(PropertyChangeListener l) { properties.addListener(l);}
	public void addPropertyListener(String pname, PropertyChangeListener l) { properties.addListener(pname, l);}
	public void removePropertyListener(PropertyChangeListener l) { properties.removeListener(l);}
	public void removePropertyListener(String pname, PropertyChangeListener l) { properties.removeListener(pname, l);}
	public boolean isActive() { return (properties.getProperty(ONLComponent.STATE) != null && properties.getProperty(ONLComponent.STATE).equals(ONLComponent.ACTIVE));}
	protected Vector getFailedEdits()
	{
		Vector fedits = new Vector();
		if (!properties.getPropertyBool(FAILURE)) return fedits;
		properties.setProperty(FAILURE, false);
		ONLComponentList nodes = topology.getNodes();
		ONLComponentList links = topology.getLinks();
		//first add all failed nsps
		int max = nodes.size();
		int i = 0;
		ONLComponent elem = null;
		for (i = 0; i < max; ++i)
		{
			elem = nodes.onlComponentAt(i);
			if (elem instanceof NSPDescriptor && elem.isFailed())
			{
				fedits.add(new AddNodeEdit(elem, this));
				elem.setState( ONLComponent.WAITING);
			}
		}
		//now do the hosts
		for (i = 0; i < max; ++i)
		{
			elem = nodes.onlComponentAt(i);
			if (elem.isFailed())
			{
				fedits.add(new AddNodeEdit(elem, this));
				elem.setState( ONLComponent.WAITING);
			}
		}
		//now add any failed links that are still around
		max = links.size();
		for (i = 0; i < max; ++i)
		{
			elem = links.onlComponentAt(i);
			if (elem.isFailed())
			{
				fedits.add(new LinkDescriptor.AddEdit((LinkDescriptor)elem, topology, this));
				elem.setState( ONLComponent.WAITING);
			}
		}
		return fedits;
	}

	public void addExpListener(Experiment.Listener l) { if (!eventListeners.contains(l)) eventListeners.add(l);}
	public void removeExpListener(Experiment.Listener l) { if (eventListeners.contains(l)) eventListeners.remove(l);}
	private void fireEvent(Experiment.Event e)
	{
		int len = eventListeners.size();
		for (int i =0; i < len; ++i)
		{
			((Experiment.Listener)eventListeners.elementAt(i)).experimentChanged(e);
		}
	}
	public String getID() { return (getProperty(ONLID));}
	public int getNextRefNum() { return nextRefNum;}
	public void setNextRefNum(int r) { nextRefNum = r;}
	public Topology getTopology() { return topology;}
	public boolean isWaiting() { return (topology.isWaiting());}
	public int getNumCfgObjs() { return (topology.getNumComponents());}
}
