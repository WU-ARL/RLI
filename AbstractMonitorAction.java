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
 * File: AbstractMonitorAction.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 12/22/2005
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

public abstract class AbstractMonitorAction implements MonitorAction
{
  //protected PollingRate pollingRate;
  protected MonitorManager.TimeListener timeListener = null;
  protected ParamSelector selector = null;
  protected MonitorManager monitorManager = null;
  protected ONLComponent.ONLMonitorable onlMonitorable = null; //for most this will be NSPMonitor
  
  public static final int MML_CANCELLED = 0;
  public static final int MML_RATESET = 1;
  public static final int MML_NORATESET = 2;
  private ExpCoordinator expCoordinator = null;
  protected Vector monitors = null;
  
  ////////////////////////////////////////////////////////// OptionPanel //////////////////////////////////////////////////////
  public static class OptionPanel extends JPanel
  {
    private MonitorMenuItem.MMLParamOptions paramOptions = null;
    private AbstractMonitorAction maction = null;
    private MonitorManager monitorManager = null;
    private TextFieldwLabel secs = null;
    private JCheckBox logFileButton = null;
    private JCheckBox rateButton = null;
    private Vector objectVector = null;
    private JButton acceptButton = null;
    private JButton cancelButton = null;
    protected boolean showLogFile = true;

    public OptionPanel(MonitorMenuItem.MMLParamOptions mpo, AbstractMonitorAction ama) { this(mpo, ama, "Enter", "Cancel");}
    public OptionPanel(MonitorMenuItem.MMLParamOptions mpo, AbstractMonitorAction ama, String opt0, String opt1)
    {
      this(mpo, ama.monitorManager, opt0, opt1);
      maction = ama;
    }
    public OptionPanel(MonitorMenuItem.MMLParamOptions mpo, MonitorManager mm, String opt0, String opt1)
    {
      super();
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      paramOptions = mpo;
      monitorManager = mm;
      // System.out.println("MonitorMenuListener::menuDragMouseReleased");
      //first check if the MonitorEntry already exists for this entry	  
      JLabel tmp_label;
      objectVector = new Vector();
      objectVector.addAll(paramOptions.xtraParams);
      if (!paramOptions.isOneShot())
        {
          tmp_label = new JLabel("Enter Polling Rate");
          tmp_label.setAlignmentX(Component.LEFT_ALIGNMENT);
          add(tmp_label);
          JFormattedTextField tf = new JFormattedTextField(new DecimalFormat("####0.###"));
          tf.setValue(new Double(monitorManager.getDefaultPolling().getSecsOnly()));
          secs = new TextFieldwLabel(tf, "secs:");//new TextFieldPlus(5, "1", true), "secs:");
          add(secs);
          
          if (ExpCoordinator.isTestLevel(ExpCoordinator.LOG) && showLogFile)
            {
              logFileButton = new JCheckBox("Log to File");
              add(logFileButton);
            }
          
          if (paramOptions.canBeRate())
            {
              rateButton = new JCheckBox("Compute Rate:");
              rateButton.setSelected(paramOptions.isRate());
              add(rateButton);
            }
        }

      int max = objectVector.size();
      Object elem;
      for (int i = 0; i < max; ++i)
        {
          elem = objectVector.elementAt(i);
          if (elem instanceof String) 
            {
              tmp_label = new JLabel((String)elem);
              tmp_label.setAlignmentX(Component.LEFT_ALIGNMENT);
              add(tmp_label);
            }
          else 
            {
              if (elem instanceof Component) add((Component)elem);
            }
        }

      acceptButton = new JButton(new AbstractAction(opt0){
          public void actionPerformed(ActionEvent e)
            {
              accept();
            }
          });
      cancelButton = new JButton(new AbstractAction(opt1){
          public void actionPerformed(ActionEvent e)
            {
              cancel();
            }
          });
      JPanel tmp_panel = new JPanel();
      tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
      tmp_panel.add(acceptButton);
      tmp_panel.add(cancelButton);
      add(tmp_panel);
    }
    
    public void accept()
      {
        if (logFileButton.isSelected()) 
          {
            ExpCoordinator.print(" logfilebutton is selected", 4);
            JFileChooser fChooser = new JFileChooser(new String("."));
            fChooser.rescanCurrentDirectory();
            int lfrtn = fChooser.showSaveDialog(monitorManager.getMainWindow());
            if (lfrtn == JFileChooser.APPROVE_OPTION) paramOptions.setLogFile(fChooser.getSelectedFile());
          }
        double s = 0;
        
        //System.out.println("MMI::" + menuItem.getText() + "::displayOptions secs " + secs.getText());
        if (!paramOptions.isOneShot() && secs.getText().length() > 0)
          {
            s = Double.parseDouble(secs.getText());
             paramOptions.setPollingRate(new PollingRate(s,0));
             paramOptions.setIsRate(rateButton.isSelected());
          }
      }
    public void cancel() {}
  }
    
  protected AbstractMonitorAction(ONLComponent.ONLMonitorable om)
   {
     monitorManager = om.getMonitorManager();
     onlMonitorable = om;
     timeListener = monitorManager.getTimeListener();
     selector = monitorManager.getParamSelector();
     expCoordinator = ExpCoordinator.theCoordinator;
     monitors = new Vector();
   }
  protected void select()
   {
     MonitorMenuItem.MMLParamOptions mpo = getParamOptions();
     if (selector.isOneShotMon())//expCoordinator.getCurrentMode() == Mode.MONITORING_ONESHOT)
       mpo.setOneShot(true);
     int rtn = displayOptions(mpo);
     if (rtn != MML_CANCELLED)
       {  
          selected(mpo);
       }
   }
  protected int displayOptions(MonitorMenuItem.MMLParamOptions mpo) { return (displayOptions(mpo, monitorManager.getMainWindow()));}
  protected int displayOptions(MonitorMenuItem.MMLParamOptions mpo, JFrame window)
   { 
     // System.out.println("MonitorMenuListener::menuDragMouseReleased");
     //first check if the MonitorEntry already exists for this entry	  
     final String str0 = "Enter Polling Rate";
     JFormattedTextField tf = new JFormattedTextField(new DecimalFormat("####0.###"));
     tf.setValue(new Double(monitorManager.getDefaultPolling().getSecsOnly()));
     TextFieldwLabel secs = new TextFieldwLabel(tf, "secs:");//new TextFieldPlus(5, "1", true), "secs:");
     
     JCheckBox logFileButton = new JCheckBox("Log to File");
     JCheckBox is_rate = null;
     
     Vector objectVector = new Vector();
     objectVector.addAll(mpo.xtraParams);
     if (!mpo.isOneShot())
       {
         objectVector.add(str0);
         objectVector.add(secs);
       }
     if (mpo.canBeRate())
       {
         is_rate = new JCheckBox("rate:");
         is_rate.setSelected(mpo.isRate());
         objectVector.add(is_rate);
       }
     if (ExpCoordinator.isTestLevel(ExpCoordinator.LOG))
       objectVector.add(logFileButton);
     
     final String opt0 = "Enter";
     final String opt1 = "Cancel";
     Object[] options = {opt0,opt1};
     
     int rtn = JOptionPane.showOptionDialog(window, 
                                            objectVector.toArray(), 
                                            "Add Parameter", 
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE, 
                                            null,
                                            options,
                                            options[0]);
     
     if (rtn == JOptionPane.YES_OPTION)
       {
         if (logFileButton.isSelected() && ExpCoordinator.isTestLevel(ExpCoordinator.LOG)) 
            {
              System.out.println("MMI::getPollingRate logfilebutton is selected");
              JFileChooser fChooser = new JFileChooser(new String("."));
              fChooser.rescanCurrentDirectory();
              int lfrtn = fChooser.showSaveDialog(monitorManager.getMainWindow());
              if (lfrtn == JFileChooser.APPROVE_OPTION) mpo.setLogFile(fChooser.getSelectedFile());
            }
         double s = 0;
         
         //System.out.println("MMI::" + menuItem.getText() + "::displayOptions secs " + secs.getText());
         if (!mpo.isOneShot() && secs.getText().length() > 0)
           {
             s = Double.parseDouble(secs.getText());
           }
         if (mpo.canBeRate() && is_rate != null) mpo.setIsRate(is_rate.isSelected());
         if (s > 0) 
           {
             mpo.setPollingRate(new PollingRate(s,0));
             //mpo.getPollingRate().print();
             return MML_RATESET;
           }
          else return MML_NORATESET;
       }
     else
       return MML_CANCELLED;
   }
    
  public MonitorMenuItem.MMLParamOptions getParamOptions() { return (new MonitorMenuItem.MMLParamOptions());}  //over written to add xtra parameters
  public Monitor addMonitor(Monitor m)
   {
     if (m.getDataType().isPeriodic()) m.addMonitorable(this);
     Monitor rtn = onlMonitorable.addMonitor(m);
     ExpCoordinator.print(new String("AbstractMonitorAction::addMonitor " + m.getDataType().getName() + ", " +  m.getDataType().getMonitorID()), 4);
     return rtn;
   }
  
  public Monitor addMonitor(MonitorDataType.Base dt) //, boolean addToDisplay)
   {
     //Monitor m = null;
     //if (dt.getPollingRate() != null) m = getMonitor(dt); //if it's not a one shot monitoring
     //else
     if (dt.getPollingRate() == null)
       dt.setMonitorable(onlMonitorable); //want to be able to add again if necessary, so pass the monitorable
     //if (m == null) 
     //{
         Monitor m = createMonitor(dt);
         addMonitor(m);
         //}
     return m;
   }
  public MonitorManager getMonitorManager() { return monitorManager;} 
    
  public void selected(MonitorMenuItem.MMLParamOptions mpo) 
   {
     MonitorDataType.Base mdt = getDataType(mpo);
     mdt.setIsRate(mpo.isRate());
     Monitor m = addMonitor(mdt);//getDataType(mpo));
     if (m == null) ExpCoordinator.print("MMI::selected m is null");
     else 
       {
         if (m.getDataType().getPollingRate() == null) ExpCoordinator.print("MMI::selected m.pollingRate is null");
       }
     if (m.getDataType().isPeriodic() && !m.getDataType().getPollingRate().isEqual(mpo.getPollingRate()))
    	  {
	    PollingRate pRate = m.getDataType().getPollingRate();
    	    JOptionPane.showMessageDialog(monitorManager.getMainWindow(), 
    					  (new String("Polling Rate is already set at " + pRate.getSecs() + "secs " + pRate.getUSecs() + "usecs")), 
    					  "Current Polling Rate",
    					  JOptionPane.PLAIN_MESSAGE);
    	    //ExpCoordinator.print("MMI::setPollingRate polling rate already set");
    	  }
     if (mpo.getLogFile() != null) m.setLogFile(mpo.getLogFile());
     setSelected(m);
   }
  

  public Monitor createMonitor(MonitorDataType.Base dt)
    {
      Monitor m = null;//getMonitor(dt);
      if (dt != null && (!dt.isPeriodic() || m == null))
        {
          m = new NumberData(dt, 0, 0, 0, 0, NumberData.DROP_OLD);
          ((NumberData)m).setTimeListener(timeListener);
          ((NumberData)m).addBoxedRangeListener(timeListener);
        }
      monitors.add(m);
      return m;
    } 
  public Monitor getMonitor(MonitorDataType.Base dt)
    {
      int max = monitors.size();
      Monitor m;
      for (int i = 0; i < max; ++i)
        {
          m = (Monitor)monitors.elementAt(i);
          if (dt.isEqual(m.getDataType())) return m;
        }
      return null;
    } 
  protected abstract MonitorDataType.Base getDataType(MonitorMenuItem.MMLParamOptions mpo) ;//forms the appropriate data type from these user specified options
  public void setSelected(Monitor mp)
   {
     if (selector.isListenersEmpty()) monitorManager.addDisplay(Graph.MULTILINE);
     selector.setCurrentParam(mp);
   }
  public void removeMonitor(Monitor m)
   {
     if (monitors.contains(m)) monitors.remove(m);
   }
  public abstract boolean isDataType(MonitorDataType.Base mdt);
}
