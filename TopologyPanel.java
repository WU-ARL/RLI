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
 * File: TopologyPanel.java
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
import javax.swing.*;
import java.util.Vector;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math;

//public class TopologyPanel extends JPanel implements ListDataListener
public class TopologyPanel extends JLayeredPane implements ListDataListener
{
	public static final int TEST_MOUSE = 9;
	public static final int TEST_TOPO = 5;
	private final int LINK_LAYER = 1;
	private final int NODE_LAYER = 2;
	private final int LOOPBACK_LAYER = LINK_LAYER;//3;
	private Vector<ONLGraphic> nodes = null;
	private Vector<ONLGraphic> links = null;
	private int numRouters = 0;
	private LinkTool linktool = null;
	private Selector componentSelector = null;
	private SelectorBox selectorBox = null;
	//private JLayeredPane layeredPane = null;
	// private ONLMainWindow mainWindow = null; 
	private ExpCoordinator expCoordinator = null;
	private Topology topology = null;

	private class SelectorBox extends Rubberband //implements MouseListener
	{
		//private ExpCoordinator expCoordinator = null;
		public SelectorBox(TopologyPanel tp)//, ExpCoordinator ec)
		{
			super(tp);
			//expCoordinator = ec;
			setEnabled(true);
			properties.setProperty(Rubberband.DISAPPEAR, true);
		}
		public void end(Point p)
		{
			//System.out.println("TP.SelectorBox end");
			if (getDrawingState() != Rubberband.NOTACTIVE)
			{
				//System.out.println(" state is active");
				expCoordinator.setCurrentSelection(null);
				//go through nodes and set Selection to the set of components in the box
				//Vector<ONLGraphic> selected = new Vector<ONLGraphic>();
				int max = nodes.size();
				int i;
				ONLGraphic cg = null;
				for (i = 0; i < max; ++i)
				{
					cg = nodes.elementAt(i);
					if (inBounds((Point)cg.getLocation())) expCoordinator.addToCurrentSelection(cg.getONLComponent());
				}
				LinkDescriptor ld = null;
				max = links.size();
				for (i = 0; i < max; ++i)
				{
					cg = links.elementAt(i);
					ld = (LinkDescriptor)cg.getONLComponent();
					if (ld.getPoint1().isSelected() || ld.getPoint2().isSelected()) expCoordinator.addToCurrentSelection(ld);
				}
			}
			super.end(p);
		}
	}

	private class Deselector extends MouseInputAdapter
	{
		//private ExpCoordinator expCoordinator = null;
		public Deselector()//ExpCoordinator ec)
		{
			super();
			//expCoordinator = ec;
		}
		public void mousePressed(MouseEvent e)
		{
			expCoordinator.setCurrentSelection(null);
			revalidate();
		}
	}
	private class Selector extends ONLGraphic.ButtonListener
	{
		//private ExpCoordinator expCoordinator = null;
		public Selector()//ExpCoordinator ec)
		{
			super();
			//expCoordinator = ec;
		}
		public void mousePressed(MouseEvent e)
		{
			ONLComponent onlcomp = ((ONLComponentButton)e.getSource()).getONLComponent();
			//System.out.println(onlcomp.getLabel() + " is selected");
			//if (onlcomp == expCoordinator.getCurrentSelection()) expCoordinator.setCurrentSelection(null);
			//else 
			ExpCoordinator.print(new String("TopologyPanel.Selector.mousePressed " + onlcomp.getLabel() + " is clicked"), TEST_MOUSE);
			if (!onlcomp.isSelected())
				expCoordinator.setCurrentSelection(onlcomp);
		}
		public void mouseClicked(MouseEvent e)
		{
			ONLComponent onlcomp = ((ONLComponentButton)e.getSource()).getONLComponent();
			ExpCoordinator.print(new String("TopologyPanel.Selector.mouseClicked " + onlcomp.getLabel() + " is clicked"), TEST_MOUSE);
			if (e.getClickCount() == 1 && onlcomp.isSelected()) expCoordinator.setCurrentSelection(onlcomp);
		}
	}

	private class DragListener extends ONLGraphic.ButtonListener
	{
		private Point startDrag = null;
		private boolean pressed = false;
		private ONLGraphic onlGraphic = null;
		private TopologyPanel tpanel = null;

		public DragListener(ONLGraphic onlg, TopologyPanel tp) 
		{ 
			super();
			onlGraphic = onlg;
			tpanel = tp;
		}
		public void mouseDragged(MouseEvent e)
		{
			if (tpanel.linktool != null && tpanel.linktool.isEnabled()) return;
			ONLComponent c = onlGraphic.getONLComponent();
			if (pressed && onlGraphic instanceof HardwareGraphic)
			{
				if (!((HardwareGraphic)onlGraphic).isSpinning()) pressed = true;
				else pressed = false;
			}
			ExpCoordinator.print(new String("TopologyPanel.DragListener moving by (" + e.getX() + "," + e.getY() + ") pressed:" + pressed), TEST_MOUSE);
			if (pressed)
			{
				Vector selected = expCoordinator.getCurrentSelection();
				int max = selected.size();
				Rectangle bnds = e.getComponent().getBounds();
				double y_offset = bnds.getHeight()/2;
				double x_offset = bnds.getWidth()/2;
				for (int i = 0; i <max; ++i)
				{
					ONLGraphic cg = ((ONLComponent)selected.elementAt(i)).getGraphic();
					if (cg != null)
					{
						//calculate how much to move graphic
						Point loc = cg.getLocation(null);
						loc.translate((int)(e.getX()-x_offset), (int)(e.getY()- y_offset));
						//loc.translate((int)(e.getX()), (int)(e.getY()));
						cg.setLocation(loc);
						cg.revalidate();
						if (!tpanel.contains(loc))
						{
							tpanel.resizeToContain(loc);
						}
					}
				}
			}
		} 
		public void mousePressed(MouseEvent e) 
		{
			if (tpanel.linktool != null && tpanel.linktool.isEnabled()) return;
			//mark as start of drag
			ONLComponent c = onlGraphic.getONLComponent();
			if (onlGraphic instanceof HardwareGraphic)
			{
				if (!((HardwareGraphic)onlGraphic).isSpinning()) pressed = true;
				else pressed = false;
			}
			else pressed = true;
		} 
		public void mouseReleased(MouseEvent e)
		{
			if (tpanel.linktool != null && tpanel.linktool.isEnabled()) return;
			//mark as end of drag
			pressed = false;
		}
	}

	public TopologyPanel(ONLMainWindow mw) { this(mw.getExpCoordinator());}//before initializing Topology register this as a listener
	public TopologyPanel(ExpCoordinator ec) //before initializing Topology register this as a listener
	{
		super();
		nodes = new Vector();
		links = new Vector();
		expCoordinator = ec;
		componentSelector = new Selector();
		selectorBox = new SelectorBox(this);
		addMouseListener(new Deselector());
		setOpaque(false);
	}

	public void resetTopology(Topology topo)
	{
		if (topology == topo) return;
		if (topology != null) 
		{
			topology.removeTopologyListener(this);
			for (ONLGraphic node : nodes)
			{
				remove(node);
				node.removeMouseListener(componentSelector);
			}
			nodes.removeAllElements();
			for (ONLGraphic link : links)
			{
				remove(link);
				link.removeMouseListener(componentSelector);
			}
			links.removeAllElements();
		}
		topology = topo;
		if (topology != null)
		{
			setLinkTool(topology.getLinkTool());
			topology.addTopologyListener(this);
		}
		else setLinkTool(null);
		revalidate();     
		repaint();
	}
	public void removeNode(ONLGraphic nd)
	{
		Action rm_action = nd.getONLComponent().getRemoveAction();

		if (rm_action != null) rm_action.actionPerformed(new ActionEvent(this, 0, ""));
		remove(nd);
		nd.removeMouseListener(componentSelector);
		if (nodes.contains(nd.getONLComponent())) nodes.remove(nd.getONLComponent());
		validate();
	}

	public void addNode(ONLComponent c)
	{
		ONLGraphic nd = getNode(c);
		//System.out.println("TopologyPanel::addNode ");
		if (nd == null)
		{
			ExpCoordinator.print(new String("TopologyPanel.addNode " + c.getLabel() ), TEST_TOPO);
			//System.out.println("TopologyPanel::addNode " + c.getLabel());
			if (c instanceof Hardware)
			{
				nd = c.getGraphic();
				//System.out.println("     adding WUGS or NSP");
				//need to set size and location
				if (nd != null)
				{
					ExpCoordinator.print(" got the graphic", ONLGraphic.TEST_GRAPHIC);
					if (nd instanceof HardwareGraphic)
					{
						((HardwareGraphic)nd).setSize(82);//70);
						if (!ExpCoordinator.isSPPMon()) ((HardwareGraphic)nd).addPortListener(linktool.getButtonAction());
						((HardwareGraphic)nd).addNodeListener(componentSelector);
						//nd.addComponentListener(c.getGraphicsListener());
						setRouterLocation(nd);
					}
					if (nd instanceof HostGraphic)
					{
						nd.setSize(30,52);
						((HostGraphic)nd).addNodeListener(linktool.getButtonAction());
						((HostGraphic)nd).addNodeListener(componentSelector);
						//nd.addComponentListener(c.getGraphicsListener());
						setHostLocation(nd);
					}
				}
			}
			else
			{
				if (c instanceof GigEDescriptor)
				{
					nd = c.getGraphic();
					//System.out.println("     adding GigE");
					if (nd != null)
					{
						nd.setSize(35,20);
						((GigEDescriptor.Graphic)nd).addNodeListener(linktool.getButtonAction());
						((GigEDescriptor.Graphic)nd).addNodeListener(componentSelector);
						setHostLocation(nd);
					}
				}
			}
			if (nd != null)
			{
				DragListener dListener = new DragListener(nd, this);
				nd.addDragListener(dListener);
				add(nd, new Integer(NODE_LAYER));
				setLayer(nd, NODE_LAYER);
				//System.out.println("adding node to layer " + getLayer(nd));
				//nd.addMouseListener(componentSelector);
				nd.setVisible(true);
				nd.addComponentListener(c.getGraphicsListener());
				nodes.add(nd);
			}
		}
		else 
			ExpCoordinator.print(new String("TopologyPanel.addNode " + c.getLabel() + " node already there"), TEST_TOPO);
		validate();
		repaint();
	}

	public void setRouterLocation(ONLGraphic onlg)
	{
		++numRouters;
	}

	public void setHostLocation(ONLGraphic onlg)
	{
		setRouterLocation(onlg);
	}

	public ONLGraphic getNode(ONLComponent c)
	{      
		int max = nodes.size();
		ONLGraphic elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLGraphic)nodes.elementAt(i);
			if (c == elem.getONLComponent()) return elem;
		}
		return null;
	}

	public void addLink(LinkGraphic l) 
	{
		ONLGraphic l2 = getLink(l);
		if (l2 == null)
		{
			ExpCoordinator.print("TopologyPanel::addLink adding selector", 2);
			l.addMouseListener(componentSelector);
			links.add(l);
			int tmp_layer = LINK_LAYER;
			if (l.isSelfLoop()) 
			{
				tmp_layer = LOOPBACK_LAYER;
				// add(l, new Integer(tmp_layer), 0);
			}
			//else
			setLayer(l, tmp_layer);
			add(l, new Integer(tmp_layer));
			ExpCoordinator.print(new String("adding link to layer " + getLayer(l)), 2);
		}
		revalidate();
		repaint();
	}
	public void removeLink(LinkGraphic l) 
	{
		ONLGraphic l2 = getLink(l);
		if (l2 != null)
		{
			//System.out.println("TopologyPanel::removeLink adding selector");
			l.removeMouseListener(componentSelector);
			links.remove(l);
			remove(l);
		}
		revalidate();
		repaint();
	}

	public void addLink(ONLComponent c)
	{
		ONLGraphic l = getLink(c);
		if (l == null)
		{
			l = c.getGraphic();
			if (l != null)
			{
				//System.out.println("TopologyPanel::addLink adding selector"); 
				l.addMouseListener(componentSelector);
				l.setVisible(true);
				links.add(l);
				add(l, new Integer(LINK_LAYER));
				setLayer(l, LINK_LAYER);
				//System.out.println("adding link to layer " + getLayer(l));
			}
		}
		//validate();
		revalidate();
		repaint();
	}
	public void removeLink(ONLComponent l) 
	{
		int max = links.size();
		ONLGraphic elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLGraphic)links.elementAt(i);
			if (l == elem.getONLComponent())
			{
				//System.out.println("TopologyPanel::removeLink " + l.getLabel());
				elem.removeMouseListener(componentSelector);
				links.remove(elem);
				remove(elem);
				break;
			}
		}
		//validate();
		revalidate();     
		repaint();
	}
	//public boolean isValidateRoot() { return true;}
	private ONLGraphic getLink(ONLGraphic c)
	{      
		int max = links.size();
		ONLGraphic elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLGraphic)links.elementAt(i);
			if (c == elem) return elem;
		}
		return null;
	}
	public ONLGraphic getLink(ONLComponent c)
	{      
		int max = links.size();
		ONLGraphic elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLGraphic)links.elementAt(i);
			if (c == elem.getONLComponent()) return elem;
		}
		return null;
	}

	//interface ListDataListener
	public void contentsChanged(ListDataEvent e) {}

	public void intervalAdded(ListDataEvent e) 
	{
		//System.out.println("TopologyPanel::intervalAdded");
		ONLComponentList l = (ONLComponentList)e.getSource();
		int max = l.getSize();
		ONLComponent elem = null;
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLComponent)l.get(i);
			if (elem instanceof LinkDescriptor) addLink(elem);
			else addNode(elem);
		}
		revalidate();     
		repaint();
	}

	public void intervalRemoved(ListDataEvent e) 
	{
		Vector rmElems = new Vector();
		ONLComponentList l = (ONLComponentList)e.getSource();
		Vector clist = nodes;
		if (l.isLinks()) clist = links;
		int max = clist.size();
		ONLGraphic elem = null;
		int i = 0;
		for (i = 0; i < max; ++i)
		{
			elem = (ONLGraphic)clist.elementAt(i);
			if (!l.contains(elem.getONLComponent())) rmElems.add(elem);
		}
		max = rmElems.size();
		for (i = 0; i < max; ++i)
		{
			elem = (ONLGraphic)rmElems.elementAt(i);
			remove(elem);
			if (elem.getONLComponent() != null)
			{
				//System.out.println("TopologyPanel::intervalRemoved onlComponent is " + elem.getONLComponent().getLabel());
				if (elem instanceof HardwareGraphic)
				{
					if (!ExpCoordinator.isSPPMon()) ((HardwareGraphic)elem).removePortListener(linktool.getButtonAction());
					((HardwareGraphic)elem).removeNodeListener(componentSelector);
					--numRouters;
				}
				else 
				{
					if (elem instanceof HostGraphic) 
					{
						((HostGraphic)elem).removeNodeListener(linktool.getButtonAction());
						((HostGraphic)elem).removeNodeListener(componentSelector);
					}
					else elem.removeMouseListener(componentSelector); //is link
				}
			}
			else System.out.println("TopologyPanel::intervalRemoved onlComponent is null");
		}
		clist.removeAll(rmElems);
		rmElems.removeAllElements();
		revalidate();     
		repaint();
	}
	//end interface ListDataListener
	public void setSize(int x, int y)
	{
		super.setSize(x,y);
		//System.out.println("TopologyPanel::setSize " + x + " " + y);
	}
	public void setLinkTool(LinkTool lt) 
	{
		if (linktool != null)
		{
			removeMouseListener(linktool);
			removeMouseMotionListener(linktool);
		}
		linktool = lt;
		if (linktool != null)
		{
			addMouseListener(linktool);
			addMouseMotionListener(linktool);
		}
	}
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (selectorBox.isDrawing()) selectorBox.draw(g);
	}

	private void resizeToContain(Point loc)
	{
		if (ExpCoordinator.getDebugLevel() < 5) return;
		int ox = getX();
		int oy = getY();
		int x = (int)loc.getX();
		int y = (int)loc.getY();
		int w = getWidth();
		int h = getHeight();
		//System.out.println("TP::resetting bounds from point (" + x + "," + y + ") original (x,y,w,h) = (" + ox + "," + oy + "," + w + "," + h + ")");
		if (x < ox) 
		{
			x -= 30;
			w += 30 + (ox - x);
		}
		else
		{
			if (x > ox)
				w += x-ox + 45;
			x = ox;
		}
		if (y < oy) 
		{
			y -= 30;
			h += 30 + (oy - y);
		}
		else
		{
			if (y > oy)
				h += y-oy + 45;
			y = oy;
		}
		setBounds(new Rectangle(x,y,w,h));
		setPreferredSize(new Dimension(w, h));
		//System.out.println("                     to (x,y,w,h) = (" + x + "," + y + "," + w + "," + h + ")");
		revalidate();
	}
}
