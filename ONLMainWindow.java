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
