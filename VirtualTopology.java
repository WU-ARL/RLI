import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.beans.*;

//import java.io.DataInput;
import java.io.IOException;
//import java.lang.reflect.Array;
import java.util.Vector;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;

public class VirtualTopology extends Topology {
	public static final int TEST = 1;
	public static final int TEST_DAEMON = TEST;
	public static final int TEST_ADD = TEST;
	public static final int TEST_XML = TEST;
	private VirtualWindow vwindow = null;
	public static final String VNODE_TYPE = "VNode";

	// ///////////////////////////////// VirtualTopology.VNode
	// ////////////////////////////////////////////////
	public static class VNode extends Hardware 
	{

		private VNodeSubtype.VNodeTypeInfo vnodeInfo = null;

		// //////////////////////////////////////////////////
		// VOpManager/////////////////////////////////////////////////////////////////////

		protected static class VOpManager extends Hardware.OpManager implements PropertyChangeListener 
		{
			public static final String OPMANAGER_STATE = "opmanager_state";
			private ONLComponent vcomponent = null;
			private NCCPOpManager.Operation initOperation = null;
			private Vector<NCCPOpManager.Operation> pendingOperations = null;

			public VOpManager(MonitorDaemon md, VNode vc) {
				super(md);
				vcomponent = vc;
				pendingOperations = new Vector<NCCPOpManager.Operation>();
				Vector<ONLComponent.PortBase> vps = ((Hardware) vcomponent)
						.getPorts();
				for (ONLComponent.PortBase p : vps) {
					p.addPropertyListener(OPMANAGER_STATE, this);
				}
			}

			public VOpManager(MonitorDaemon md, VNode.VInterface p) {
				super(md, p);// (MonitorDaemon)getONLDaemon(ONLDaemon.HWC));
				vcomponent = p;
				pendingOperations = new Vector<NCCPOpManager.Operation>();
			}

			public void processResponse(NCCP.ResponseBase r) {
				super.processResponse(r);
				ExpCoordinator.print(
						"VirtualTopology.VNode.OpManager.processResponse", 7);
				if (initOperation.containsRequest(r.getRendMarker())) {
					if (r.getStatus() == NCCP.Status_Fine)
						vcomponent.setProperty(OPMANAGER_STATE, ACTIVE);
					else
						vcomponent.setProperty(OPMANAGER_STATE, FAILED);
				}
			}

			public void addInitOperation(Operation op) {
				initOperation = op;
				vcomponent.setProperty(OPMANAGER_STATE, WAITING);
				addOperation(op);
			}

			public void addOperation(Operation op) 
			{
				ExpCoordinator.print(new String("VirtualTopology.VNode" + vcomponent.getLabel() + ".VOpManager.addOperation"), TEST);
				String state = vcomponent.getProperty(OPMANAGER_STATE);
				if (vcomponent instanceof VNode)
					state = vcomponent.getState();
				if (op == initOperation || (state != null && state.equals(ACTIVE)))
					super.addOperation(op);
				else
					pendingOperations.add(op);
			}

			public void propertyChange(PropertyChangeEvent evt) 
			{
				if (evt.getPropertyName().equals(OPMANAGER_STATE)) {
					// check if all interfaces are active
					String vc_state = vcomponent.getProperty(OPMANAGER_STATE);
					if (vc_state == null || vc_state.equals(WAITING))
						return;
					boolean failed = (vc_state.equals(FAILED));
					boolean is_initialized = (vc_state.equals(ACTIVE));
					if (vcomponent instanceof VNode) {
						Vector<ONLComponent.PortBase> vps = ((Hardware) vcomponent)
								.getPorts();
						for (ONLComponent.PortBase p : vps) {
							String tmp_state = p.getProperty(OPMANAGER_STATE);
							if (tmp_state != null) {
								if (tmp_state.equals(FAILED))
									failed = true;
								else if (!tmp_state.equals(ACTIVE))
									is_initialized = false;
							} else
								is_initialized = false;
						}
						// if any failures, set component failed and say what
						// failed
						if (failed)
							vcomponent.setProperty(STATE, FAILED);
						// if all active, set component active, send all pending
						// ops for interfaces and components
						else if (is_initialized) {
							vcomponent.setProperty(STATE, ACTIVE);
							addPendingOps();
							for (ONLComponent.PortBase p : vps)
								((VNode.VInterface) p).addPendingOps();
						}
					}
				}
			}

			public void addPendingOps() {
				while (!pendingOperations.isEmpty()) {
					NCCPOpManager.Operation op = pendingOperations.remove(0);
					addOperation(op);
				}
			}
		}// end VirtualTopology.VNode.VOpManager

		// //////////////////////////////////////////////////
		// VirtualTopology.Vnode.VContentHandler
		// ///////////////////////////////////////////////////////////
		protected static class VContentHandler extends
				Hardware.HWContentHandler {
			private VirtualTopology.VTContentHandler vtopoXML = null;
			private boolean fillingVinfo = false;

			public VContentHandler(Hardware hwi, ExperimentXML expxml,
					VirtualTopology.VTContentHandler vtopo) {
				super(hwi, expxml);
				vtopoXML = vtopo;
			}

			public void startElement(String uri, String localName,
					String qName, Attributes attributes) {
				super.startElement(uri, localName, qName, attributes);
				if (localName.equals(ExperimentXML.VINFO)) {
					((VNode) getComponent()).vnodeInfo = new VNodeSubtype.VNodeTypeInfo(
							((VNode) getComponent()).getNumPorts());
					fillingVinfo = true;
				}
				if (localName.equals(ExperimentXML.INTERFACE) && fillingVinfo) {
					VNodeSubtype.InterfaceInfo ii = new VNodeSubtype.InterfaceInfo(
							uri, attributes);

					ExpCoordinator.print(
							new String("VirtualTopology.VNode("
									+ getComponent().getLabel()
									+ ").VContentHandler.startElement "
									+ localName + " adding interface "
									+ attributes.getValue(ExperimentXML.ID)),
							TEST_XML);

					((VNode) getComponent()).vnodeInfo.interfaces.add(ii);
					expXML.setContentHandler(ii.getContentHandler(expXML));
				}
			}

			public void endElement(String uri, String localName, String qName) {
				ExpCoordinator.print(new String(
						"VirtualTopology.VNode.VContentHandler.endElement "
								+ localName), TEST_XML);

				if (localName.equals(ExperimentXML.NODE)) {
					// initialize();
					vtopoXML.addNode((VNode) component);
					expXML.removeContentHandler(this);
					if (position != null)
						getComponent().getGraphic().setLocation(position);
					component.getGraphic().setSpinnerPosition(spinner);
				}
				if (localName.equals(ExperimentXML.VINFO)) {
					fillingVinfo = false;
					((VNode) getComponent())
							.setHWSubtype(((VNode) getComponent()).vnodeInfo
									.getCP().realNode.getVirtualType());
				}

				/*
				 * if (localName.equals(ExperimentXML.SUBHWTYPE)) { if (hwtype
				 * != null) { ((Hardware)getComponent()).setHWSubtype(hwtype);
				 * ExpCoordinator.print(new
				 * String("Hardware.HWContentHandler.endElement " + localName +
				 * " " + hwtype.getLabel()), HardwareBaseSpec.TEST_SUBTYPE); }
				 * else ExpCoordinator.print(new
				 * String("Hardware.HWContentHandler.endElement " + localName +
				 * " null"), HardwareBaseSpec.TEST_SUBTYPE);
				 * 
				 * }
				 */
			}
		}

		// ////////////////////////////////////////////////////
		// VirtualTopology.VNode.VInterface
		// ////////////////////////////////////////
		public static class VInterface extends Hardware.Port {
			private VNodeSubtype.InterfaceInfo interfaceInfo = null;

			public VInterface(Hardware hwi, int ndx, ExpCoordinator ec,
					VNodeSubtype.InterfaceInfo ii) {
				this(hwi, new String(hwi.getType() + ONLComponent.PORT_LBL),
						ndx, ec, ii);
			}

			public VInterface(Hardware wd, String tp, int p, ExpCoordinator ec,
					VNodeSubtype.InterfaceInfo ii) {
				super(wd, tp, p, ec);
				interfaceInfo = ii;
				if (interfaceInfo != null)
					initialize();
			}

			protected void initialize() {
				if (interfaceInfo == null)
					return;
				else {
					if (portSpec == null)
						setPortSpec();
					super.initialize();
					if (interfaceInfo.command != null) {
						interfaceInfo.command.setParentComponent(this);
						addOperation(new Command.NCCPOp(interfaceInfo.command,
								interfaceInfo.command.getDefaultValues(), this));
					}
				}
			}

			protected void initializeActions() {
				ExpCoordinator.print("VirtualTopology.VNode.initializeActions",
						5);
				super.initializeActions();
			}

			protected void initializeMonitorable() {
				monitorable = new HWMonitor.ChildMonitorable(this,
						((HWMonitor) interfaceInfo.realNode.getMonitorable()),
						portSpec);
				((HWMonitor.ChildMonitorable) monitorable).initializeMMItems();
			}

			protected void addPendingOps() {
				((VOpManager) getOpManager()).addPendingOps();
			}

			public NCCPOpManager getOpManager() {
				if (opManager == null)
					opManager = new VOpManager(
							(MonitorDaemon) getDaemon(ONLDaemon.HWC), this);
				return opManager;
			}

			public ONLDaemon getDaemon(int tp) {
				return (interfaceInfo.realNode.getDaemon(tp));
			}

			public void addDaemon(int tp, int port, boolean is_dir) {
			}

			protected void setPortSpec() {
				if (interfaceInfo != null)
					portSpec = interfaceInfo.realNode.getVirtualType()
							.getInterfaceSpec();
			}

			public boolean isLinked() {
				return false;
			}
		}

		public VNode(int ndx, ExpCoordinator ec, VNodeSubtype hw_type,
				VNodeSubtype.VNodeTypeInfo nfo) {
			this(new String(hw_type.getLabel() + "." + ndx), ec, hw_type, nfo);
		}

		public VNode(String lbl, ExpCoordinator ec, VNodeSubtype hw_type,
				VNodeSubtype.VNodeTypeInfo nfo) {
			super(lbl, ec, hw_type, nfo.numInterfaces);
			if (nfo == null)
				ExpCoordinator.print("VirtualTopology.VNode nfo is null",
						TEST_ADD);
			else
				ExpCoordinator.print("VirtualTopology.VNode nfo", TEST_ADD);
			vnodeInfo = nfo;
			initialize(null);
		}

		public VNode(String uri, Attributes attributes) {
			super(uri, attributes);
			// vnodeInfo = new VNodeSubtype.VNodeTypeInfo(numPorts);
		}

		protected Hardware.Port createPort(int ndx) {
			Port rtn = new VInterface(this, ndx, expCoordinator,
					vnodeInfo.getInterface(ndx));
			ExpCoordinator.print(new String("VirtualTopology.VNode("
					+ getLabel() + ").createPort(" + ndx + ")"), TEST_PORT);
			return rtn;
		}

		public void initializeMonitorable() // override to create and initialize
											// monitorable if there is one otw
											// make empty
		{
			if (monitorable == null) {
				ExpCoordinator.print(new String("VirtualTopology.VNode("
						+ getLabel() + ").initializeMonitorable"), 5);
				if (vnodeInfo == null)
					ExpCoordinator.print("   vnodeInfo is null", TEST);
				else if (vnodeInfo.getCP() == null)
					ExpCoordinator.print("   vnodeInfo.getCP() is null", TEST);
				else if (vnodeInfo.getCP().realNode == null)
					ExpCoordinator.print(
							"   vnodeInfo.getCP().realNode is null", TEST);
				monitorable = new HWMonitor.ChildMonitorable(this,
						((HWMonitor) vnodeInfo.getCP().realNode
								.getMonitorable()), getHWSubtype());
				((HWMonitor.ChildMonitorable) monitorable).initializeMMItems();
				// monitorable = new HWMonitor(daemons, this,
				// expCoordinator.getMonitorManager());
			}
		}

		/*
		 * protected void initializeActions() {
		 * ExpCoordinator.print("VirtualTopology.VNode.initializeActions", 5);
		 * 
		 * // if (hwType != null) // { // initializeDaemons();
		 * //initializeMonitorable(); //}
		 * 
		 * actions.add(new UserLabelAction(this)); if
		 * (getExpCoordinator().isManagement()) { if (buttonAction == null)
		 * buttonAction = new ButtonAction(this); }
		 * 
		 * HardwareSpec.Subtype hw_type =
		 * vnodeInfo.getCP().realNode.getHWSubtype(); if (fields != null) {
		 * fields.addAll(vnodeInfo.getCP().realNode.fields); int max =
		 * fields.size(); Field elem; for (int i = 0; i < max; ++i) { elem =
		 * (Field)fields.elementAt(i); Action ua = elem.getUpdateAction(); if
		 * (ua != null) actions.add(ua); } } if (hw_type != null) {
		 * ExpCoordinator.print(new
		 * String("VirtualTopology.VNode.initializeActions " + getLabel() +
		 * " subtype "), HardwareBaseSpec.TEST_SUBTYPE);
		 * hw_type.print(HardwareBaseSpec.TEST_SUBTYPE); Vector cmds =
		 * hw_type.getCommandSpecs();
		 * cmds.addAll(getHWSubtype().getCommandSpecs()); if (cmds.isEmpty()) {
		 * ExpCoordinator.print("     no commands to add",
		 * HardwareBaseSpec.TEST_SUBTYPE); return; } int max = cmds.size();
		 * ExpCoordinator.print(new String("     " + max + " commands to add"),
		 * HardwareBaseSpec.TEST_SUBTYPE); for (int i = 0; i < max; ++i) {
		 * Command cmd = new Command((CommandSpec)cmds.elementAt(i), this);
		 * actions.add(new Command.CAction(this, cmd)); } }
		 * 
		 * }
		 */
		protected void setBaseIPAddr(String ipaddr) {
		}

		public void addDaemon(int tp, int port, boolean is_dir) {
		/*
																 * if
																 * (realDaemons
																 * != null) {
																 * Hardware
																 * real_comp =
																 * (Hardware
																 * )vnodeInfo.
																 * getPhysicalComponent
																 * (tp); String
																 * cp =
																 * real_comp
																 * .getCPHost();
																 * if (cp ==
																 * null) cp =
																 * ""; //first
																 * check if the
																 * daemon is
																 * already there
																 * MonitorDaemon
																 * md =
																 * (MonitorDaemon
																 * )
																 * getONLDaemonByPort
																 * (port); if
																 * (md == null)
																 * //if not get
																 * it from the
																 * real_component
																 * { md =
																 * (MonitorDaemon
																 * )real_comp.
																 * getONLDaemonByPort
																 * (port); if
																 * (md == null)
																 * //if the real
																 * component
																 * doesn't know
																 * anything
																 * about it have
																 * the real_comp
																 * create add it
																 * { real_comp.
																 * addDaemon
																 * (port, port);
																 * md =
																 * (MonitorDaemon
																 * )real_comp.
																 * getONLDaemonByPort
																 * (port); }
																 * //add it to
																 * the VNode's
																 * list of
																 * daemons if
																 * (md != null)
																 * {
																 * ExpCoordinator
																 * .print(new
																 * String(
																 * "VirtualTopology.VNode("
																 * + getLabel()
																 * +
																 * ").addDaemon for type:"
																 * + tp +
																 * " port:" +
																 * port), 4);
																 * realDaemons
																 * .add(new
																 * VDaemon(tp,
																 * md)); if
																 * (monitorable
																 * != null)
																 * ((NodeMonitor
																 * )
																 * monitorable).
																 * initializeDaemons
																 * (
																 * realDaemons);
																 * } else
																 * ExpCoordinator
																 * .print(new
																 * String(
																 * "VirtualTopology.VNode("
																 * + getLabel()
																 * +
																 * ").addDaemon("
																 * + tp + "," +
																 * port + "," +
																 * is_dir +
																 * ") failed"),
																 * TEST_DAEMON);
																 * } if (md !=
																 * null) {
																 * md.setHost
																 * (cp);
																 * md.setPort
																 * (port);
																 * ExpCoordinator
																 * .print(new
																 * String(
																 * "VirtualTopology.VNode.addDaemon for "
																 * + getLabel()
																 * +
																 * " setting host to "
																 * + cp), 4); }
																 * if
																 * (!md.isConnected
																 * ())
																 * md.connect();
																 * } else
																 * System.
																 * out.println(
																 * "VirtualTopology.VNode realDaemons is null"
																 * );
																 */
		}

		public ONLDaemon getDaemon(int tp) {
			return (vnodeInfo.getCP().realNode.getDaemon(tp));
		}

		protected void initialize(String cp) {
			if (vnodeInfo == null) {
				ExpCoordinator.print(
						"VirtualTopology.VNode.initialize vnodeInfo null",
						TEST_ADD);
				return;
			} else
				super.initialize(cp);
		}

		public void writeXMLParams(XMLStreamWriter xmlWrtr)
				throws XMLStreamException {
			vnodeInfo.writeXML(xmlWrtr);
			super.writeXMLParams(xmlWrtr);
		}

		public ContentHandler getContentHandler(ExperimentXML expXML,
				VirtualTopology.VTContentHandler vtch) {
			return (new VContentHandler(this, expXML, vtch));
		}

		public Color getIconColor() {
			if (super.getIconColor() != null)
				return (super.getIconColor());
			else
				return (vnodeInfo.getCP().realNode.getIconColor());
		}
		public ONLComponent getPhysicalComponent(int interface_id) { return (vnodeInfo.getPhysicalComponent(interface_id));}

		public ONLComponent getPhysicalComponent() { return (vnodeInfo.getPhysicalComponent());}

		public void setSelected(boolean b) 
		{ 
			super.setSelected(b);
		    ONLComponent rnode = vnodeInfo.getPhysicalComponent();
		    if (rnode != null) rnode.setSelected(b);
		}
		/*
		 * 
		 * protected void initializeMenu() { if (menu == null) {
		 * super.initializeMenu(); if (cpComponent != null && (cpComponent
		 * instanceof Hardware)) { Vector<Action> cp_actions =
		 * ((Hardware)cpComponent).getActions(); for (Action a : cp_actions) {
		 * if (!(a instanceof HWTable.HWTAction)) menu.addCfg(a); } if
		 * (!(cpComponent instanceof NSPDescriptor)) { cp_actions =
		 * ((Hardware.HWMonitor)cpComponent.getMonitorable()).getActions(); for
		 * (Action a : cp_actions) { menu.addMon(a); } } } //TODO add commands
		 * and monitors from cpComponent } }
		 */
	}

	// ///////////////////////////////////// VirtualTopology.VNodeType
	// ////////////////////////////////////////////////
	public static class VNodeType extends HardwareSpec {
		protected static final VNodeType vtype = new VNodeType();

		private VNodeType() {
			super();
			typeName = VNODE_TYPE;
			version = 1;
			portSpec = new HardwareBaseSpec.PortSpec(this);
			initializeSubtypes();
		}
	}// end class VirtualTopology.VNodeType

	// ///////////////////////////////////// VirtualTopology.VNodeSubtype
	// ////////////////////////////////////////////////
	public static class VNodeSubtype extends HardwareSpec.Subtype {
		// ////////////////////////////////////
		// VirtualTopology.VNodeSubtype.VPortSpec //////////////////////////
		public static class VPortSpec extends PortSpec {
			public VPortSpec(String uri, Attributes attributes,
					HardwareBaseSpec hw) {
				super(uri, attributes, hw);
			}

			public VPortSpec(HardwareBaseSpec hw) {
				super(hw);
			}

			public VPortSpec(PortSpec ps) {
				super(ps);
			}

			public String getEndToken() {
				return (ExperimentXML.INTERFACES);
			}
		}

		// /////////////////////////////////////
		// VirtualTopology.VNodeSubtype.VSHWXMLHandler ////////////////////

		public static class VSHWXMLHandler extends SHWXMLHandler {
			protected ContentHandler hwhandler = null;
			protected XMLReader xmlReader = null;

			public VSHWXMLHandler(XMLReader xmlr, ContentHandler hwh,
					VNodeSubtype vst) {
				super(xmlr);
				hardware = vst;
				setSpec(vst);
				xmlReader = xmlr;
				hwhandler = hwh;
			}

			public void endElement(String uri, String localName, String qName) {
				super.endElement(uri, localName, qName);
				ExpCoordinator.print(new String(
						"VirtualTopology.VNodeSubtype.VSHWXMLHandler.endElement "
								+ localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.VIRTUAL))// ExperimentXML.COMMAND)
															// ||
															// localName.equals(ExperimentXML.UPDATE_COMMAND))
				{
					xmlReader.setContentHandler(hwhandler);
				}
			}
		}

		// ////////////////////////////////////////////
		// VirtualTopology.VNodeSubtype.AddWindow ///////////////////////
		public static class AddWindow extends JDialog {
			Vector<InterfacePanel> interfaces = null;
			InterfacePanel cpPanel = null;
			TextFieldwLabel numInterfaces = null;
			JToggleButton opt0 = null;
			JToggleButton opt1 = null;
			VNodeTypeInfo typeInfo = null;

			// ////////////////////////////////////////////
			// VirtualTopology.VNodeSubtype.AddWindow.InterfacePanel
			// ////////////////////////////////
			public static class InterfacePanel extends JPanel {
				public static final String NONE = "N/A";
				public static final String PREFIX_STR = "interface ";
				public static final String CP_STR = "CP";
				JComboBox component = null;
				JComboBox commandChoice = null;
				String interfaceID = null;
				Command command = null;

				private class CommandListener implements ItemListener {
					public CommandListener() {
					}

					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							Object o = e.getItem();
							if (o instanceof Command) {
								CAction ca = new CAction(
										((Command) o).getParentComponent(),
										(Command) o);
								ca.actionPerformed();
								command = ca.getVNodeCommand();
							} else
								command = null;
						}
					}
				}

				public static class CAction extends Command.CAction {
					private Command commandCopy;

					public CAction(ONLComponent o, Command c) {
						super(o, c);
					}

					protected void addOperation(Command cmd, Object[] vals) {
						commandCopy = cmd.getCopy(vals);
					}

					protected Command getVNodeCommand() {
						return commandCopy;
					}
				}

				public InterfacePanel(int iid) {
					super();
					if (iid == InterfaceInfo.CP)
						interfaceID = CP_STR;
					else
						interfaceID = new String(PREFIX_STR + iid);
					setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

					component = new JComboBox(ExpCoordinator.theCoordinator
							.getTopology().getHWNodes().toArray());
					component.setEditable(false);
					if (iid != InterfaceInfo.CP)
						component.addItem(CP_STR);

					commandChoice = new JComboBox();

					Action componentListener = new AbstractAction() {
						public void actionPerformed(ActionEvent e) {
							commandChoice.removeAllItems();
							commandChoice.setEnabled(false);
							ONLComponent c = (ONLComponent) component
									.getSelectedItem();
							if (c instanceof Hardware) {
								Vector cmds = ((Hardware) c).getActions();
							    int cmd_index = 0;
								for (Object a : cmds)
									if (a instanceof Command.CAction) {
										Command cmd = ((Command.CAction) a).getCommand();
										if (cmd.getType() == CommandSpec.COMMAND)
											//commandChoice.addItem(cmd);
											commandChoice.insertItemAt(cmd, cmd_index++);
									}
								commandChoice.addItem(NONE);
								commandChoice.setEnabled(true);
								commandChoice.setSelectedItem(NONE);
							}
							validate();
						}
					};

					component.addActionListener(componentListener);

					JLabel label = new JLabel(new String(interfaceID
							+ "    real component:"));
					label.setAlignmentX(Component.LEFT_ALIGNMENT);
					add(label);
					add(component);
					label = new JLabel("   command:");
					label.setAlignmentX(Component.LEFT_ALIGNMENT);
					add(label);
					commandChoice.setEditable(false);
					commandChoice.setEnabled(false);
					commandChoice.addItemListener(new CommandListener());
					add(commandChoice);

					setAlignmentX(Component.LEFT_ALIGNMENT);
					// comp.setAlignmentX(1);
					setBorder(new EmptyBorder(1, 0, 1, 0));
				}

				public InterfaceInfo getInterfaceInfo() {
					ExpCoordinator.print(new String(
							"VNodeSubtype.AddWindow.InterfacePanel.getInterfaceInfo component "
									+ component.getSelectedItem()), TEST);
					if (component.getSelectedItem() instanceof ONLComponent)// &&
																			// (command
																			// !=
																			// null))
					{
						int iid = InterfaceInfo.CP;
						if (interfaceID.startsWith(PREFIX_STR)) {
							try {
								iid = Integer.parseInt(interfaceID
										.substring(PREFIX_STR.length()));
							} catch (NumberFormatException e) {
							}
						}
						Hardware hw = null;
						if (component.getSelectedItem() instanceof Hardware)
							hw = (Hardware) component.getSelectedItem();
						InterfaceInfo rtn = new InterfaceInfo(hw, command, iid);
						return rtn;
					}
					return null;
				}
			}

			public AddWindow(JFrame frame) {
				this(frame, "Add Virtual Node");
			}

			public AddWindow(JFrame frame, String ttl) {
				super(frame, ttl, true);
				interfaces = new Vector<InterfacePanel>();
				getContentPane().setLayout(
						new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
				opt0 = new JToggleButton("Enter");
				opt1 = new JToggleButton("Cancel");
				class CloseAction implements ActionListener {
					private JDialog dlog = null;

					CloseAction(JDialog d) {
						dlog = d;
					}

					public void actionPerformed(ActionEvent e) {
						dlog.setVisible(false);
						if (opt0.isSelected()) {
							typeInfo = new VNodeTypeInfo(getNumInterfaces());
							typeInfo.interfaces.add(cpPanel.getInterfaceInfo());
							String tmp_str = numInterfaces.getText();
							try {
								int i = Integer.parseInt(tmp_str);
								for (int j = 0; j < i; ++j) {
									InterfaceInfo ii = cpPanel
											.getInterfaceInfo();
									ii.interfaceID = j;
									typeInfo.interfaces.add(ii); // could be
																	// null
								}
							} catch (NumberFormatException nfe) {
							}
							/*
							 * for (InterfacePanel ip : interfaces) {
							 * InterfaceInfo ii = ip.getInterfaceInfo();
							 * typeInfo.interfaces.add(ii); //could be null }
							 */
							ExpCoordinator.print("VirtualTopology.VNodeSubtype.AddWindow.CloseAction.actionPerformed", TEST_ADD);
						}
						dlog.dispose();
					}
				}
				CloseAction close_action = new CloseAction(this);
				opt0.addActionListener(close_action);
				opt1.addActionListener(close_action);
				ButtonGroup optGroup = new ButtonGroup();
				optGroup.add(opt0);
				optGroup.add(opt1);
				optGroup.setSelected(opt0.getModel(), false);
				JPanel options = new JPanel();
				// options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
				options.setLayout(new FlowLayout(FlowLayout.CENTER));
				options.setBorder(new EmptyBorder(1, 0, 1, 0));
				options.add(opt0);
				options.add(opt1);

				int num_display = 2;// num_interfaces + 1;
				int height = (num_display) * 32 + 75;

				cpPanel = new InterfacePanel(InterfaceInfo.CP);
				add(cpPanel);

				JFormattedTextField tmp_ni = new JFormattedTextField(1);
				/*
				 * Action nia = new AbstractAction() { public void
				 * actionPerformed(ActionEvent e) { String tmp_str =
				 * numInterfaces.getText(); try { int i =
				 * Integer.parseInt(tmp_str); if (i > interfaces.size()) {
				 * ExpCoordinator.print(new String(
				 * "VirtualTopology.VNodeSubtype.AddWindow number interfaces listener adding interfaces "
				 * + interfaces.size() + " - " + i), TEST_ADD); for (int j =
				 * interfaces.size(); j < i; ++ j ) { //add interface panels to
				 * interfaces int ndx = j + 1; InterfacePanel ip = new
				 * InterfacePanel(ndx); interfaces.add(ip); add(ip); } } else if
				 * (i < interfaces.size()) { for (int j = interfaces.size(); j >
				 * i; --j ) { //remove extra interface panels to interfaces
				 * ExpCoordinator.print(new String(
				 * "VirtualTopology.VNodeSubtype.AddWindow number interfaces listener removing interfaces "
				 * + interfaces.size() + " - " + i), TEST_ADD); int ndx = j - 1;
				 * InterfacePanel ip = interfaces.remove(ndx); remove(ip); } } }
				 * catch (NumberFormatException er){} validate(); } };
				 * tmp_ni.addActionListener(nia);
				 */
				numInterfaces = new TextFieldwLabel(tmp_ni,
						"number of interfaces:");

				add(numInterfaces);
				// dialog.add(display);
				options.setAlignmentX(Component.LEFT_ALIGNMENT);
				add(options);
				setSize(550, height);
				setVisible(true);
			}

			public VNodeTypeInfo getTypeInfo() {
				return typeInfo;
			}

			public int getNumInterfaces() {
				String tmp_str = numInterfaces.getText();
				try {
					int i = Integer.parseInt(tmp_str);
					return i;
				} catch (NumberFormatException e) {
				}
				return 1;
			}
		}

		// ////////////////////////////////////////////
		// VirtualTopology.VNodeSubtype.MenuItem
		// ////////////////////////////////////////////////
		public static class MenuItem extends JMenuItem {
			// private VNodeSubtype type;
			private class VNodeAction extends Topology.TopologyAction {
				private ExpCoordinator expCoordinator = null;
				private boolean done = false;

				private class WindowAction extends WindowAdapter {
					private AddWindow addWindow = null;
					private VNode vnode = null;

					public WindowAction(AddWindow aw) {
						addWindow = aw;
					}

					public void windowClosed(WindowEvent e) 
					{
						ExpCoordinator.print("VirtualTopology.VNodeSubtype.MenuItem.VNodeAction.WindowAction.windowClosed adding component",TEST_ADD);
						if (vnode != null)
							return;
						VirtualTopology vtopo = expCoordinator.getCurrentExp().getVirtualTopology();
						VNodeSubtype.VNodeTypeInfo vst = addWindow.getTypeInfo();
						if (vst == null)
							ExpCoordinator.print("VirtualTopology.VNodeSubtype.MenuItem.VNodeAction.WindowAction.windowClosed adding component type info null",TEST_ADD);
						else 
						{
							vnode = vtopo.getNewVNode(vst);
							vnode.initializePorts();
							vtopo.addComponent(vnode);
						}
					}
				}

				public VNodeAction(String nm) {
					super(new String(nm));
					expCoordinator = ExpCoordinator.theCoordinator;
				}

				public void actionPerformed(ActionEvent e) {
					AddWindow aw = new AddWindow(null);// ExpCoordinator.getVirtualWindow
					aw.addWindowListener(new WindowAction(aw));
					//aw.setSize(550,200);
				}
			}

			public MenuItem() {
				this(new String("Add " + VNodeType.vtype));
			}

			public MenuItem(String nm) {
				super(new String(nm));
				// type = new VNodeSubtype();
				setAction(new VNodeAction(nm));
			}
			/*
			 * public boolean isType(HardwareSpec tp) { return
			 * (type.hardwareType.equals(tp)); } public HardwareSpec.Subtype
			 * getType() { return type;} public boolean
			 * isSubtype(HardwareSpec.Subtype shw) { return (type.equals(shw));
			 * }
			 */
		}// end VirtualTopology.VNodeSubtype.MenuItem

		// ///////////////////////////////////
		// VirtualTopology.VNodeSubtype.VNodeTypeInfo
		// /////////////////////////////////
		public static class VNodeTypeInfo {
			Vector<InterfaceInfo> interfaces = null;
			int numInterfaces = 0;

			VNodeTypeInfo(int ni) {
				numInterfaces = ni;
				interfaces = new Vector<InterfaceInfo>();
			}

			public ONLComponent getPhysicalComponent(int interface_id) {
				InterfaceInfo nfo = getInterface(interface_id);
				if (nfo != null)
					return nfo.realNode;
				return null;
			}
			
			public ONLComponent getPhysicalComponent()
			{
				InterfaceInfo nfo = getCP();
				if (nfo != null)
					return nfo.realNode;
				return null;
			}

			public InterfaceInfo getInterface(int interface_id) {
				for (InterfaceInfo nfo : interfaces) {
					if (nfo.interfaceID == interface_id)
						return nfo;
				}
				return null;
			}

			public InterfaceInfo getCP() {
				return (getInterface(InterfaceInfo.CP));
			}

			public void writeXML(XMLStreamWriter xmlWrtr)
					throws XMLStreamException {
				xmlWrtr.writeStartElement(ExperimentXML.VINFO);
				xmlWrtr.writeAttribute(ExperimentXML.NUMINTERFACES,
						String.valueOf(numInterfaces));
				for (InterfaceInfo nfo : interfaces) {
					nfo.writeXML(xmlWrtr);
				}
				xmlWrtr.writeEndElement();
			}
		}

		public static class InterfaceInfo {
			public static final int CP = -1;
			Hardware realNode = null;
			int interfaceID = CP;
			Command command = null;

			private class IContentHandler extends DefaultHandler {
				private ExperimentXML expXML = null;
				protected String currentElement = "";
				protected InterfaceInfo iinfo = null;

				private class ComponentHandler extends
						ONLComponent.XMLComponentHandler {
					public ComponentHandler(String uri, Attributes attributes,
							ExperimentXML exp_xml) {
						super(uri, attributes, exp_xml);
					}

					public void endElement(String uri, String localName,
							String qName) {
						if (localName.equals(ExperimentXML.COMPONENT))
						// if (localName.equals(ExperimentXML.NODE))
						{
							ExpCoordinator.print(new String(
									"MonitorXMLHandler.ComponentHandler.endElement "
											+ localName + "  component "
											+ getComponent().getLabel()),
									ExperimentXML.TEST_XML);
							expXML.removeContentHandler(this);
							iinfo.realNode = (Hardware) getComponent();
						}
					}
				}// end class ComponentHandler

				public IContentHandler(ExperimentXML exp_xml, InterfaceInfo ii) {
					super();
					expXML = exp_xml;
					iinfo = ii;
				}

				public void startElement(String uri, String localName,
						String qName, Attributes attributes) {
					ExpCoordinator
							.print(new String(
									"VirtualTopology.VNodeSubtype.InterfaceInfo.IContentHandler.startElement localName:"
											+ localName), TEST_XML);
					currentElement = new String(localName);
					if (localName.equals(ExperimentXML.COMPONENT))
						expXML.setContentHandler(new ComponentHandler(uri,
								attributes, expXML));

					// if (localName.equals(ExperimentXML.NODE))
					// expXML.setContentHandler(new ComponentHandler(uri,
					// attributes, expXML));
					if (localName.equals(ExperimentXML.COMMAND)
							&& iinfo.realNode != null) {
						iinfo.command = ((Hardware) iinfo.realNode)
								.getCommand(
										Integer.parseInt(attributes
												.getValue(ExperimentXML.OPCODE)),
										false).getCopy();
						expXML.setContentHandler(iinfo.command
								.getContentHandler(expXML));
					}
				}

				public void endElement(String uri, String localName,
						String qName) {
					ExpCoordinator.print(new String(
							"VirtualTopology.VNodeSubtype.InterfaceInfo.IContentHandler.endElement "
									+ localName), ExperimentXML.TEST_XML);
					if (localName.equals(ExperimentXML.INTERFACE)) {
						expXML.removeContentHandler(this);
					}
				}
			}// end inner class IContentHandler

			public InterfaceInfo(Hardware c, Command cmd, int iid) {
				realNode = c;
				command = cmd;
				interfaceID = iid;
			}

			public InterfaceInfo(String uri, Attributes attributes) {
				if (attributes.getValue(ExperimentXML.ID) != null)
					interfaceID = Integer.parseInt(attributes
							.getValue(ExperimentXML.ID));
			}

			public void writeXML(XMLStreamWriter xmlWrtr)
					throws XMLStreamException {
				xmlWrtr.writeStartElement(ExperimentXML.INTERFACE);
				xmlWrtr.writeAttribute(ExperimentXML.ID,
						String.valueOf(interfaceID));
				xmlWrtr.writeStartElement(ExperimentXML.COMPONENT);
				realNode.writeXMLID(xmlWrtr);
				xmlWrtr.writeEndElement();
				if (command != null)
					command.writeXML(xmlWrtr);
				xmlWrtr.writeEndElement();
			}

			public ContentHandler getContentHandler(ExperimentXML expXML) {
				return (new IContentHandler(expXML, this));
			}
		}// end InterfaceInfo

		public VNodeSubtype() {
			super(VNodeType.vtype);
		}

		public VNodeSubtype(Attributes attributes) {
			super(attributes);
			setHardwareType(VNodeType.vtype);
			if (typeName == null)
				typeName = VNODE_TYPE;
		}

		public ContentHandler getXMLHandler(XMLReader xmlr, ContentHandler hwh) {
			return (new VSHWXMLHandler(xmlr, hwh, this));
		}

		public PortSpec getInterfaceSpec() {
			return (getPortSpec());
		}

		protected PortSpec createPortSpec(String uri, Attributes attributes) {
			return (new VPortSpec(uri, attributes, this));
		}

		protected PortSpec createPortSpec(PortSpec ps) {
			return (new VPortSpec(ps));
		}
	}

	// ///////////////////////////////// VirtualTopology.LinkTool
	// ////////////////////////////////////////////////
	private static class VLinkTool extends LinkTool 
	{
		public VLinkTool(ExpCoordinator ec) 
		{
			super(ec);
		}

		protected boolean addLink(LinkGraphic lg, ONLComponent end_comp) // returns true if successfully added
		{
			if (lg == null) return false;
			LinkDescriptor comp = (LinkDescriptor) lg.getONLComponent();
			ONLComponent start_comp = comp.getPoint1();
			// fail link if end_comp is not a component, it's a loop, the
			// end_comp is unlinkable or is already linked
			if (end_comp == null || end_comp == start_comp || !end_comp.isLinkable()) 
			{
				if (end_comp == null)
					ExpCoordinator.print(new String("VirtualTopology.VLinkTool.addLink " + comp.toString() + " fail end is null"), TEST_ADD);
				else 
				{
					if (end_comp == start_comp)
						ExpCoordinator.print(new String("VirtualTopology.VLinkTool.addLink " + comp.toString() + " fail end(" + end_comp.getLabel() + ") is same as start"), TEST_ADD);
					if (!end_comp.isLinkable())
						ExpCoordinator.print(
								new String("VirtualTopology.VLinkTool.addLink " + comp.toString() + " failed end(" + end_comp.getLabel() + ") unlinkable isLinkable:"
										+ end_comp.isLinkable() + " isLinked:" + end_comp.isLinked()), TEST_ADD);
				}
				return false;
			}

			comp.setPoint2(end_comp);
			addLink(lg);
			return true;
		}

		protected void addLink(LinkGraphic lg) {
			LinkDescriptor comp = (LinkDescriptor) lg.getONLComponent();
			ExpCoordinator.print(new String("VirtualTopology.VLinkTool::addLink " + comp.getPoint1().getLabel() + " to " + comp.getPoint2().getLabel()));

			lg.getONLComponent().setLabel(new String("expvlink" + getNextCount()));
			expCoordinator.getCurrentExp().getVirtualTopology().addComponent(comp);
		}

		protected LinkDescriptor createLinkDescriptor(ONLComponent startc) {
			LinkDescriptor ld = new VLinkDescriptor(getNextLabel(), startc, getCurrentBW(), expCoordinator);
			return ld;
		}

		protected TopologyPanel getTopologyPanel() 
		{
			return (expCoordinator.getCurrentExp().getVirtualTopology().getVWindow().getTopologyPanel());
		}

		public boolean isVirtualTopology() { return true;}
	}// end class VirtualTopology.VLinkTool

	// ///////////////////////////////// VirtualTopology.LinkDescriptor
	// ////////////////////////////////////////////////

	public static class VLinkDescriptor extends LinkDescriptor {
		private class VLContentHandler extends ONLComponent.ONLContentHandler {
			private VirtualTopology.VTContentHandler vtopoXML = null;

			private class PointHandler extends ONLComponent.XMLComponentHandler {
				private String pointType = "";
				private VLContentHandler lchandler = null;

				public PointHandler(String uri, Attributes attributes,
						String p, VLContentHandler lch) {
					super(uri, attributes, lch.expXML, vtopoXML.vtopology);
					pointType = new String(p);
					lchandler = lch;
				}

				public String getPointType() {
					return pointType;
				}

				public void endElement(String uri, String localName,
						String qName) {
					ExpCoordinator.print(new String(
							"LinkDescriptor.LContentHandler.PointHandler.endElement "
									+ localName + "  pointType " + pointType),
							3);
					if (localName.equals(pointType)) {
						if (pointType.equals(ExperimentXML.POINT1))
							((LinkDescriptor) lchandler.getComponent())
									.setPoint1(getComponent());
						else
							((LinkDescriptor) lchandler.getComponent())
									.setPoint2(getComponent());
						expXML.removeContentHandler(this);
					}
				}
			}

			public VLContentHandler(VLinkDescriptor vld, ExperimentXML expxml,
					VirtualTopology.VTContentHandler vxml) {
				super(vld, expxml);
				vtopoXML = vxml;
			}

			public void endElement(String uri, String localName, String qName) {
				ExpCoordinator.print(new String(
						"VirtualTopology.VLinkDescriptor.VLContentHandler.endElement "
								+ localName), TEST_XML);

				if (localName.equals(ExperimentXML.VLINK)) {
					// initialize();
					vtopoXML.addLink((VLinkDescriptor) component);
					expXML.removeContentHandler(this);
				}
			}

			public void startElement(String uri, String localName,
					String qName, Attributes attributes) {
				super.startElement(uri, localName, qName, attributes);
				if (getCurrentElement().startsWith(ExperimentXML.POINT))
					expXML.setContentHandler(new PointHandler(uri, attributes,
							getCurrentElement(), this));
			}
		}

		public VLinkDescriptor(String lbl, ONL.Reader tr, ExpCoordinator onlcd)
				throws IOException {
			super(lbl, tr, onlcd);
		}

		protected VLinkDescriptor(String uri, Attributes attributes) {
			super(uri, attributes);
		}

		protected VLinkDescriptor(String lbl, ONLComponent p1, ONLComponent p2,
				double bw, ExpCoordinator ec) {
			super(lbl, p1, p2, bw, ec);
		}

		protected VLinkDescriptor(String lbl, ONLComponent p1, double bw,
				ExpCoordinator ec) {
			super(lbl, p1, bw, ec);
		}

		protected String getXMLElementName() {
			return ExperimentXML.VLINK;
		}

		public ContentHandler getContentHandler(ExperimentXML expXML,
				VirtualTopology.VTContentHandler vtch) {
			return (new VLContentHandler(this, expXML, vtch));
		}
		public boolean isVirtualTopology() { return true;}
	}// end class VirtualTopology.VLinkTool

	// ///////////////////////////////////// VirtualTopology.VAction
	// /////////////////////////////////////////////
	public static class VAction extends AbstractAction {
		public VAction() {
			this("Show Virtual Topology");
		}

		public VAction(String nm) {
			super(nm);
		}

		public void actionPerformed(ActionEvent e) {
			VirtualWindow vwindow = ExpCoordinator.theCoordinator.getCurrentExp().getVirtualTopology().getVWindow();
			// vwindow.setSize(550, 300);
			// if ((vwindow.getExtendedState() & Frame.ICONIFIED) != 0)
			// vwindow.setExtendedState(vwindow.getExtendedState() -
			// Frame.ICONIFIED);
			vwindow.setVisible(true);
		}
		/*
		 * public VirtualWindow getVWindow() { if (vwindow == null) { vwindow =
		 * new VirtualWindow("Virtual Topology",
		 * ExpCoordinator.theCoordinator.getCurrentExp().getVirtualTopology());
		 * } return vwindow; } public void clearWindow() { if (vwindow != null)
		 * vwindow.dispose(); vwindow = null; }
		 */
	}

	// ///////////////////////////////// VirtualTopology.VirtualWindow
	// ////////////////////////////////////////////////

	private static class VirtualWindow extends JFrame {
		private TopologyPanel topologyPanel = null;
		// private Color defaultBackground = Color.gray;
		// private Component hGlue = null;
		private JMenu editMenu = null;

		// private JMenu topologyMenu = null;

		public VirtualWindow(String ttl, VirtualTopology vtopo) {
			super(ttl);
			setVisible(false);
			vtopo.vwindow = this;
			topologyPanel = new TopologyPanel(ExpCoordinator.theCoordinator);
			topologyPanel.resetTopology(vtopo);
			// topology.setLinkTool(vtopo.getLinkTool());
			// vtopo.addTopologyListener(topology);

			if (ExpCoordinator.getDebugLevel() > 12)
				getContentPane().add(
						(new JScrollPane(topologyPanel,
								JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
								JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)),
						BorderLayout.CENTER);
			else
				getContentPane().add(topologyPanel);

			// defaultBackground = new
			// Color(getContentPane().getBackground().getRGB());

			// setSize
			// setup the menu bar
			// create menu bar
			JMenuBar mb = new JMenuBar();
			setJMenuBar(mb);
			editMenu = new JMenu("Edit");
			addToMenu(ExpCoordinator.theCoordinator.getEditActions(), editMenu);
			editMenu.add(new VNodeSubtype.MenuItem("Add Virtual Node"));
			mb.add(editMenu);
			// TODO: This is where we should add VirtualComponent menu //JMenu
			// tmp_menu = addMenu(topologyActions, "Topology");
			// HardwareSpec.ManagerListener mlistener = new
			// HardwareSpec.ManagerListener(tmp_menu);
			if (!ExpCoordinator.isSPPMon())
				mb.add(new JButton(vtopo.getLinkToolAction()));
			validate(); // want to cause the menubar to show up
		}

		private void addToMenu(Vector actions, JMenu menu) {
			int max = actions.size();
			Object elem;
			for (int i = 0; i < max; ++i) {
				elem = actions.elementAt(i);
				if (elem instanceof JMenu)
					menu.add((JMenu) elem);
				else {
					if (elem instanceof JMenuItem)
						menu.add((JMenuItem) elem);
					else {
						if (elem instanceof Action)
							menu.add((Action) elem);
					}
				}
			}
		}

		public JMenu addMenu(Vector actions, String str) {
			JMenu menu = new JMenu(str);
			addToMenu(actions, menu);
			getJMenuBar().add(menu);
			validate();
			return menu;
		}

		public TopologyPanel getTopologyPanel() {
			return topologyPanel;
		}
	}// end class VirtualTopology.VirtualWindow

	////////////////////////////////// VirtualTopology /////////////////////////////////////////////
	private class VTContentHandler extends DefaultHandler {
		protected VirtualTopology vtopology = null;
		protected ExperimentXML expXML = null;
		private String currentElement = "";

		public VTContentHandler(VirtualTopology vt, ExperimentXML exp_xml) {
			super();
			vtopology = vt;
			expXML = exp_xml;
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) {
			currentElement = new String(localName);
			ExpCoordinator.print(new String(
					"VirtualTopology.VTContentHandler.startElement "
							+ localName), TEST_XML);
			if (localName.equals(ExperimentXML.POSITION)) {
				vtopology.getVWindow().setLocation(
						Integer.parseInt(attributes.getValue(uri,
								ExperimentXML.XCOORD)),
						Integer.parseInt(attributes.getValue(uri,
								ExperimentXML.YCOORD)));
			}
			if (localName.equals(ExperimentXML.SIZE)) {
				vtopology.getVWindow().setSize(
						Integer.parseInt(attributes.getValue(uri,
								ExperimentXML.WIDTH)),
						Integer.parseInt(attributes.getValue(uri,
								ExperimentXML.HEIGHT)));
			}

			if (localName.equals(ExperimentXML.NODE))
				addNode(uri, localName, qName, attributes);
			if (localName.equals(ExperimentXML.VLINK))
				addLink(uri, localName, qName, attributes);
		}

		public void endElement(String uri, String localName, String qName) {
			if (localName.equals(ExperimentXML.VTOPO)) {
				// experiment.getTopology().fillClusters(experiment);
				ExpCoordinator
						.print("VirtualTopology.VTContentHandler topology loaded");
				expXML.removeContentHandler(this);
				ExpCoordinator.getVirtualAction().actionPerformed(null);
			}
		}

		public void addNode(VirtualTopology.VNode c) {
			vtopology.addComponent(c);
		}

		public void addNode(String uri, String localName, String qName,
				Attributes attributes) {
			// int type = Integer.parseInt(attributes.getValue(uri, TYPECODE));
			String tp_nm = attributes.getValue(uri, ExperimentXML.TYPENAME);
			VNode c = new VNode(uri, attributes);
			ExpCoordinator.print(new String(
					"ExperimentXML.addNode adding contentHandler for component "
							+ c.getType()), TEST_XML);
			expXML.setContentHandler(c.getContentHandler(expXML, this));
		}

		public void addLink(ONLComponent c) {
			vtopology.addLink((LinkDescriptor) c);
		}

		public void addLink(String uri, String localName, String qName,
				Attributes attributes) {
			VLinkDescriptor c = new VLinkDescriptor(uri, attributes);
			expXML.setContentHandler(c.getContentHandler(expXML, this));
		}
	}

	// ///////////////////////////////// VirtualTopology
	// /////////////////////////////////////////////////////////

	public VirtualTopology(ExpCoordinator ec) {
		super(ec, new VLinkTool(ec));
		// TODO Auto-generated constructor stub
	}

	public VNode getNewVNode(VNodeSubtype.VNodeTypeInfo ti) {
		int x = getNewRouterIndex();
		if (ti == null)
			ExpCoordinator.print("VirtualTopology.getNewVNode type info null",
					TEST_ADD);
		else
			ExpCoordinator.print("VirtualTopology.getNewVNode", TEST_ADD);
		VNode rtn = new VNode(x, ExpCoordinator.theCoordinator,
				ti.getCP().realNode.getVirtualType(), ti);
		rtn.setProperty(Router.INDEX, x);
		return rtn;
	}

	public VirtualWindow getVWindow() {
		if (vwindow == null) {
			vwindow = new VirtualWindow("Virtual Topology", this);
			vwindow.setSize(550, 300);
			if ((vwindow.getExtendedState() & Frame.ICONIFIED) != 0)
				vwindow.setExtendedState(vwindow.getExtendedState()
						- Frame.ICONIFIED);
		}
		return vwindow;
	}
	public TopologyPanel getTopologyPanel()
	{
		if (vwindow != null) return (vwindow.getTopologyPanel());
		else return null;
	}

	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException {
		if (vwindow != null) {
			xmlWrtr.writeStartElement(ExperimentXML.POSITION);
			xmlWrtr.writeAttribute(ExperimentXML.XCOORD,
					String.valueOf(vwindow.getX()));
			xmlWrtr.writeAttribute(ExperimentXML.YCOORD,
					String.valueOf(vwindow.getY()));
			xmlWrtr.writeEndElement();// mainWindowPosition
			// write main window width and height
			xmlWrtr.writeStartElement(ExperimentXML.SIZE);
			xmlWrtr.writeAttribute(ExperimentXML.WIDTH,
					String.valueOf(vwindow.getWidth()));
			xmlWrtr.writeAttribute(ExperimentXML.HEIGHT,
					String.valueOf(vwindow.getHeight()));
		}
		super.writeXML(xmlWrtr);
	}

	public void clear() {
		clearWindow();
		super.clear();
	}

	public void clearWindow() {
		if (vwindow != null)
			vwindow.dispose();
		vwindow = null;
	}

	public ContentHandler getContentHandler(ExperimentXML expXML) {
		return (new VTContentHandler(this, expXML));
	}
}
