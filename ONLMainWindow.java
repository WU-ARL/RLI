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
 * File: ONLMainWindow.java
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
//import java.awt.Color;
//import java.awt.Font;
import java.awt.*;

public class ONLMainWindow extends JFrame //implements Mode.MListener
{
	private ExpCoordinator expCoordinator = null;
	private TopologyPanel topology = null;
	private JMenu fileMenu = null;
	private JMenu editMenu = null;
	private JMenu extrasMenu = null;
	private JMenu insertMenu = null;
	private JMenu topologyMenu = null;

	private final String CONFIGURE_MODE = "";
	private final String MONITOR_MODE = "Monitoring";
	private JLabel modeLabel = null;
	private Color defaultBackground = Color.gray;
	private Component hGlue = null;


	public ONLMainWindow(ExpCoordinator expc, String ttl)
	{
		super(ttl);
		setVisible(false);
		expCoordinator = expc;
		topology = new TopologyPanel(this);
		if (!expCoordinator.isSPPMon()) topology.setLinkTool(expCoordinator.getTopology().getLinkTool());
		expCoordinator.getTopology().addTopologyListener(topology);

		if (expCoordinator.getDebugLevel() > 12) getContentPane().add((new JScrollPane(topology, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)), BorderLayout.CENTER);
		else getContentPane().add(topology);


		defaultBackground = new Color(getContentPane().getBackground().getRGB());

		//setSize
		//setup the menu bar
		//create menu bar
		JMenuItem mi;
		JMenuBar mb = new JMenuBar();
		setJMenuBar(mb);
		fileMenu = new JMenu("File");
		addToMenu(expCoordinator.getFileActions(), fileMenu);
		mb.add(fileMenu);
		editMenu = new JMenu("Edit");
		addToMenu(expCoordinator.getEditActions(), editMenu);
		mb.add(editMenu);
		validate(); //want to cause the menubar to show up
	}

	public boolean isDoubleBuffered() { return true;}

	public void setSize(int x, int y)
	{
		super.setSize(x,y);
		topology.setSize((x - 15), (y - 15));
	}

	private void addToMenu(Vector actions, JMenu menu)
	{
		int max = actions.size();
		Object elem;
		for (int i = 0; i < max; ++i)
		{
			elem = actions.elementAt(i);
			if (elem instanceof JMenu)
				menu.add((JMenu)elem);
			else
			{
				if (elem instanceof JMenuItem)
					menu.add((JMenuItem)elem);
				else
				{
					if (elem instanceof Action)
						menu.add((Action)elem);
				}
			}
		}
	}

	public JMenu addMenu(Vector actions, String str)
	{
		JMenu menu = new JMenu(str);
		addToMenu(actions, menu);
		getJMenuBar().add(menu);
		validate();
		return menu;
	}
	public void removeMenu(String str)
	{
		JMenuBar mbar = getJMenuBar();
		int max = mbar.getMenuCount();
		JMenu menu;
		ExpCoordinator.print(new String("ONLMainWindow.removeMenu " + str), 1);
		for (int i = 0; i < max; ++i)
		{
			menu = mbar.getMenu(i);
			ExpCoordinator.print(new String("     menu:" + menu.getText()),1 );
			if (menu.getText().equals(str))
			{
				mbar.remove(menu);
				mbar.validate();
				return;
			}
		}
	}
	public void addMenu(JMenu menu)
	{
		getJMenuBar().add(menu);
		validate();
	}
	public TopologyPanel getTopologyPanel() { return topology;}
	public ExpCoordinator getExpCoordinator() { return expCoordinator;}
	private void setMBBackground(Color c)
	{
		JMenuBar mbar = getJMenuBar();
		int max = mbar.getMenuCount();
		for (int i = 0; i < max; ++i)
		{
			JMenu elem = mbar.getMenu(i);
			if (elem != null) elem.setBackground(c);
		}
		getJMenuBar().setBackground(c);
	}
} 
