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
 * File: LogFileGraph.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 8/28/2007
 *
 * Description:
 *
 * Modification History:
 *
 */
/**subclass of Graph. draws a line graph based on data supplied in the Vector data, a member inherited from Graph.
 * currently data may only be supplied by a subclass. LineGraph defines an inner class LGDataDisplay which does the
 * painting of the lines between points
 */

import java.util.Vector;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;

public class LogFileGraph extends MultiLineGraph 
{ 
  private boolean showAll = true;

  private class SetRateAction extends AbstractAction
    {
      private MonitorManager.Display display = null;
      private TextFieldwLabel dataRate = null;
      public SetRateAction(MonitorManager.Display d) { this("set display rate...", d);}
      public SetRateAction(String nm, MonitorManager.Display d)
        {
          super(nm);
          display = d;
          dataRate = new TextFieldwLabel(50, "seconds of data/s:");
        }
      public void actionPerformed(ActionEvent e)
        {
          JPanel tmp_panel = new JPanel();
	  tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
          tmp_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
          JCheckBox all_checkbox = new JCheckBox(new AbstractAction(""){
              public void actionPerformed(ActionEvent e)
                {
                  dataRate.setEnabled(!((JCheckBox)e.getSource()).isSelected());
                }
            });
          JLabel tmp_lbl = new JLabel("Display all data:");
          tmp_lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
          all_checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
          tmp_panel.add(tmp_lbl);
          tmp_panel.add(all_checkbox);
          Object[] objectArray = {dataRate, tmp_panel};
          final String opt0 = "Enter";
          final String opt1 = "Cancel";
          Object[] options = {opt0,opt1};
	  
          int rtn = JOptionPane.showOptionDialog(ExpCoordinator.getMainWindow(), 
                                                 objectArray, 
                                                 "Set Display Rate", 
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE, 
                                                 null,
                                                 options,
                                                 options[0]);
          
          if (rtn == JOptionPane.YES_OPTION)
            {
              setShowAll(all_checkbox.isSelected());
              if (!showAll && (dataRate.getText().length() > 0)) setDataRate(Double.parseDouble(dataRate.getText()));
            }
        } 
    }
  //Main class constructor
  public LogFileGraph(MonitorManager mw, String xlbl, String ylbl, int scaleX, int scaleY, boolean scroll)
    {
      super(mw, xlbl, ylbl, scaleX, scaleY, scroll);
      setType(MonitorManager.Display.LOGGED_DATA);
      paramMenu.remove(2); //remove add formula
      paramMenu.remove(0); //remove add parameter
      JMenuItem menuItem = paramMenu.add("Add Parameter From File ...");
      MenuDragMouseAdapter mdma = new Graph.MenuAddParameterListener(this, monitorManager, new MenuFileAction(monitorManager.getLFRegistry(), true, monitorManager.getMainWindow(), "", false, true));
      menuItem.addMenuDragMouseListener(mdma);
      menuItem.addMouseListener(mdma);
      addToOptionMenu(new JMenuItem(new SetRateAction(this)));
    }
  public void addData(MonitorPlus dm)
    {
      ExpCoordinator.print(new String("LogFileGraph.addData " + dm.getDataType().getName() + " class:" + dm.getClass()),5);
      if (dm instanceof LogFileEntry.NData)
        {
          ((LogFileEntry.NData)dm).setShowAll(showAll);
          super.addData(dm);
        }
    }
  public void setShowAll(boolean b)
    {
      showAll = b;
      int max = dataDisplay.length();
      LogFileEntry.NData lfe;
      for (int i = 0; i < max; ++i)
        {
          lfe = (LogFileEntry.NData)dataDisplay.getData(i);
          lfe.setShowAll(showAll);
        }
    }
  public void setDataRate(double d)
    {
      int max = dataDisplay.length();
      LogFileEntry.NData lfe;
      for (int i = 0; i < max; ++i)
        {
          lfe = (LogFileEntry.NData)dataDisplay.getData(i);
          lfe.setDisplayRate(d, 0);
        }
    }
}
