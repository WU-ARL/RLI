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
