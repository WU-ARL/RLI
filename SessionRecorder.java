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
 * File: SessionRecorder.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 1/15/2008
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.io.*;
import java.util.*;
import java.lang.reflect.Array;
import java.text.*;
import java.lang.String;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;

//additional events
//UPDATE_HOST opcode:57 name:Update Host <old label> <new label> <ipaddress> NULL NULL NULL

class SessionRecorder extends AbstractAction implements ExpCoordinator.MenuAction
{

  public static final int UNKNOWN = 0;
  public static final int ADD_ROUTER = 1;
  public static final int ADD_HOST = 2;
  public static final int ADD_LINK = 3;
  public static final int GENERATE_DEFRTS = 4;
  public static final int PRIORITY_RT_TABLE = 5;
  public static final int ADD_ROUTE = 6;
  public static final int DELETE_ROUTE = 7;
  public static final int GENERATE_LOCAL_DEFRTS = 8;
  public static final int PREFIX_MASK_RT = 9;
  public static final int NEXTHOP_RT = 10;
  public static final int STATS_ROUTE_TABLE = 11;
  public static final int LINKBANDWIDTH = 12;
  public static final int VOQ_THOLD = 13;
  public static final int VOQ_RATES = 14;
  public static final int ADD_EGRESS_Q = 15;
  public static final int DELETE_EGRESS_Q = 16;
  public static final int Q_ID_EGRESS_Q = 17;
  public static final int THOLD_EGRESS_Q = 18;
  public static final int QUANTUM_EGRESS_Q = 19;
  public static final int PRIORITY_EMF_TABLE = 20;
  public static final int ADD_FILTER = 21;
  public static final int EDIT_FILTER = 22;
  public static final int CHANGE_SAMPLING_FILTER = 23;
  public static final int DELETE_FILTER = 24;
  public static final int STATS_FILTER = 25;
  public static final int ENABLE_FILTER = 25;
  public static final int SWBANDWIDTH = 27;
  public static final int ADD_PLUGIN_DIR = 28;
  public static final int ADD_PLUGIN = 29;
  public static final int DELETE_PLUGIN = 30;
  public static final int COMMAND_PLUGIN = 31;
  public static final int BINDINGS_PLUGIN = 32; //NSP only
  public static final int UNLOAD_PLUGIN_CLASS = 33; //NSP only
  public static final int ADD_HARDWARE = 34; 
  public static final int ADD_MON_DISPLAY = 35; 
  public static final int DELETE_MON_DISPLAY = 36; 
  public static final int ADD_MONITOR = 37; 
  public static final int DELETE_MONITOR = 38; 
  public static final int CHANGE_MONITOR = 39; 
  public static final int SET_DEF_POLLING = 40;
  public static final int DELETE_ROUTER = 41;
  public static final int DELETE_HOST = 42;
  public static final int DELETE_LINK = 43;
  public static final int DELETE_HARDWARE = 44;
  public static final int ADD_GIGE = 45;
  public static final int DELETE_GIGE = 46;


  private BufferedWriter fwriter = null;
  private boolean recording = false;
  private TimePrinter timePrinter = null;

  private final String START_STR = "Start Session Recording";
  private final String STOP_STR = "Stop Session Recording";
  private int type = ExpCoordinator.SESSION_RECORDING;
  private java.io.File currentDirectory = null;
  private int index = 0;

  private JFrame frame = null;
  private JLabel directoryLabel = null;

  /////////////////////////////////////// class SessionRecorder.TimePrinter /////////////////////////////////
  private class TimePrinter
  {
    private GregorianCalendar date = null;
    private TimeZone timezone = null;
    private SimpleDateFormat dateFormat = null;
    
    public TimePrinter()
    {
      date = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
      dateFormat =  new SimpleDateFormat("yyyyMMddHHmmss");
    }
    public String getTimeString() 
    {
      date.setTime(new Date());
      return (dateFormat.format(date.getTime()));
    } 
  }//end class SessionRecorder.TimePrinter
  
  /////////////////////////////////////// class SessionRecorder.LoadSnapAction ///////////////////////////////////
  private class LoadSnapAction extends MenuFileAction implements MenuFileAction.Saveable
  {
    public LoadSnapAction()
    {
      super(null, true, null, "Load Snap Shot", false, false);
      setSaveable(this);
      setLocalDir(currentDirectory);
      setUseLocalDir(true);
    }
    public void loadFromFile(java.io.File f)
    {
      ExpCoordinator.theCoordinator.loadSnapShot(f);
    }
    public void saveToFile(java.io.File f){}
  }//end class SessionRecorder.LoadSnapAction


  public SessionRecorder()
  {
    super("Record Session");
    timePrinter = new TimePrinter();
  }
  public void actionPerformed(ActionEvent e) 
  {
    if (!ExpCoordinator.isRecording())
      {
        //launch frame asking for name of dir, snapshot
        if (frame == null)
          {
            frame = new JFrame("Session Recording");
            JPanel jpanel = new JPanel();
            jpanel.setLayout(new BoxLayout(jpanel, BoxLayout.Y_AXIS));
            if (currentDirectory == null)
              currentDirectory = new File(MenuFileAction.currentDirectory.getPath());
            directoryLabel = new JLabel(new String("Current Directory: " + currentDirectory.getPath()));
            directoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            jpanel.add(directoryLabel);
            //if dir is new
            Action dir_action = (new AbstractAction("Change Directory")
              {
                public void actionPerformed(ActionEvent e)
                {
                  JFileChooser fChooser = new JFileChooser(currentDirectory.getPath());
                  fChooser.setFileHidingEnabled(false);
                  fChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                  int rtn = fChooser.showOpenDialog(frame);
                  if (rtn == JFileChooser.APPROVE_OPTION)
                    {
                      File dir = fChooser.getSelectedFile();
                      if (currentDirectory == null || !currentDirectory.equals(dir)) 
                        {
                          currentDirectory = dir;
                          if (!currentDirectory.exists()) currentDirectory.mkdir();
                          directoryLabel.setText(currentDirectory.getPath());
                          index = 0;
                        }
                    }
                }
              });
            JButton tmp_button = new JButton(dir_action);
            tmp_button.setAlignmentX(Component.LEFT_ALIGNMENT);
            jpanel.add(tmp_button);
            Action snap_action = (new AbstractAction("Snap")
              {
                public void actionPerformed(ActionEvent e)
                {
                  ++index;
                  try
                    {
                      recordSnapshot();
                    }
                  catch (java.io.IOException e1)
                    {
                      System.err.println("Error: SessionRecorder.actionPerformed  ioexception during recordSnapshot " + e1.getMessage());
                    }
                }
              });
            JPanel jpanel2 = new JPanel();
            jpanel2.setLayout(new BoxLayout(jpanel2, BoxLayout.X_AXIS));
            jpanel2.add(new JButton(snap_action));
            Action close_action = (new AbstractAction("Close")
              {
                public void actionPerformed(ActionEvent e)
                {
                  frame.setVisible(false);
                }
              });
            jpanel2.add(new JButton(close_action));
            jpanel.add(jpanel2);
            frame.getContentPane().add(jpanel);
            frame.setSize(200,120);
          }
        if ((frame.getExtendedState() & Frame.ICONIFIED) != 0)
          frame.setExtendedState(frame.getExtendedState() - Frame.ICONIFIED);
        frame.setVisible(true); 
      }
  }
  //interface ExpCoordinator.MenuAction
  public int getType() {return type;}
  public boolean isType(int t) { return (type == t);}
  //end interface ExpCoordinator.MenuAction
  
 

  private void recordSnapshot() throws java.io.IOException
  {
    /*
    ONL.BaseFileWriter writer = new ONL.BaseFileWriter(new File(currentDirectory.getPath(), new String("snapshot."+ index)));
    writer.setNoClose(true);
    ExpCoordinator ec = ExpCoordinator.theCoordinator;
    ec.setRecording(true);
    writer.writeLine(timePrinter.getTimeString());
    ec.getCurrentExp().write(writer);
    ec.getMonitorManager().recordDisplays(writer);
    ec.setRecording(false);
    writer.setNoClose(false);
    writer.finish();
    */
  }
  public boolean isRecording() { return recording;}
  public Action getLoadAction() { return (new LoadSnapAction());}
}
