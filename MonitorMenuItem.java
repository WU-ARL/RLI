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
 * File: MonitorMenuItem.java
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
import java.util.Vector;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;
import java.text.DecimalFormat;
import java.lang.reflect.Array;

public class MonitorMenuItem
{
	private static final int TEST_VXI = 6;
	//add mouse listener to make menu visible when selected

	//class used by menu item to get user preferences
	public static class MMLParamOptions
	{
		protected PollingRate pollingRate = null;
		protected File logfile = null;
		protected Vector xtraParams = null;
		private boolean oneShot = false;
		private boolean is_rate = true;
		private boolean can_be_rate = true;

		public MMLParamOptions() { this(new Vector());}
		public MMLParamOptions(Vector x) 
		{
			xtraParams = x;
		}
		public MMLParamOptions(Object[] x)
		{
			xtraParams = new Vector();
			int max = Array.getLength(x);
			for (int i = 0; i < max; ++i)
			{
				xtraParams.add(x[i]);
			}
		}
		public PollingRate getPollingRate() { return pollingRate;}
		public void setPollingRate(PollingRate pr) { pollingRate = pr;}
		public java.io.File getLogFile() { return logfile;}
		public void setLogFile(java.io.File f) { logfile = f;}
		public void setXtraParams(Vector x)
		{
			xtraParams = x;
		}
		public Vector getXtraParams() { return xtraParams;}
		public void setOneShot(boolean b) 
		{ 
			//if (b) pollingRate = null;
			oneShot = b;
		}
		public boolean isOneShot() { return oneShot;}
		public void setIsRate(boolean r) { is_rate = r;}
		public boolean isRate() { return (is_rate && canBeRate());}
		public void setCanBeRate(boolean r) { can_be_rate = r;}
		public boolean canBeRate() { return can_be_rate;}
	}

	public static class INTParamOptions  extends MonitorMenuItem.MMLParamOptions
	{
		protected JTextField intParam = null;
		public INTParamOptions(String lbl)
		{
			this(lbl,"");
		} 
		public INTParamOptions(String lbl, String initVal)
		{
			super();
			intParam = new JTextField(5);
			intParam.setText(initVal);
			xtraParams.add(lbl);
			xtraParams.add(intParam);
		} 
		public int getIntParam() 
		{
			if (intParam.getText().length() > 0) return (Integer.parseInt(intParam.getText()));
			else return 0;
		}
	}

	public static abstract class BaseItem extends AbstractMonitorAction //JMenuItem implements MonitorAction
	{
		//protected PollingRate pollingRate;
		protected MonitorManager.TimeListener timeListener = null;
		protected ParamSelector selector = null;
		protected MonitorManager monitorManager = null;
		//protected ONLMonitorable onlMonitorable = null; //for most this will be NSPMonitor
		private JMenuItem menuItem = null;

		//inner class MonitorMenuListener
		public static class MonitorMenuListener extends MenuDragMouseAdapter
		{
			public static final int MML_CANCELLED = 0;
			public static final int MML_RATESET = 1;
			public static final int MML_NORATESET = 2;
			protected AbstractMonitorAction monAction;
			private ExpCoordinator expCoordinator = null;

			public MonitorMenuListener(AbstractMonitorAction d)
			{
				monAction = d;
				expCoordinator = d.onlMonitorable.getONLComponent().getExpCoordinator();
			}
			public void menuDragMouseReleased(MenuDragMouseEvent e)
			{
				String nm = "";
				JMenuItem mi = null;
				if (e.getSource() instanceof JMenuItem)
				{
					mi = (JMenuItem)e.getSource();
					nm = mi.getText();
				}
				ExpCoordinator.print(new String("MonitorMenuItem.BaseItem.MonitorMenuListener.menuDragMouseReleased " + nm), 4);
				//if (mi != null && mi.isArmed()) 
				monAction.select();
			}
			public void mouseReleased(MouseEvent e)
			{
				//System.out.println("MMI.BaseItem::mouseClicked");
				String nm = "";
				JMenuItem mi = null;
				if (e.getSource() instanceof JMenuItem)
				{
					mi = (JMenuItem)e.getSource();
					nm = mi.getText();
				}
				ExpCoordinator.print(new String("MonitorMenuItem.BaseItem.MonitorMenuListener.mouseReleased " + nm), 4);
				//if (mi != null && mi.isArmed()) 
				monAction.select();
			}
		}//inner class MonitorMenuListener

		protected BaseItem(String nm, MonitorManager w, ONLComponent.ONLMonitorable om)
		{
			this(nm, false, w, om);
		}
		protected BaseItem(String nm, boolean noListener, MonitorManager w, ONLComponent.ONLMonitorable om)
		{
			this(nm, noListener, w, om, new Font("Dialog", Font.PLAIN, 9));
		}
		protected BaseItem(String nm, boolean noListener, MonitorManager w, ONLComponent.ONLMonitorable om, Font f)
		{ 
			super(om);
			menuItem = new JMenuItem(nm);
			if (!noListener) 
			{
				MonitorMenuListener mml = new MonitorMenuListener(this);
				menuItem.addMenuDragMouseListener(mml);
				menuItem.addMouseListener(mml);
			}
			if (f != null) menuItem.setFont(f);
		}
		public JMenuItem getMenuItem() { return menuItem;}
		public String getText() { return (menuItem.getText());}
		public void setFont(Font f)
		{
			if (f != null) menuItem.setFont(f);
		}
	}

}
