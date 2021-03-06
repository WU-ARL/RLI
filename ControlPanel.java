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
 * File: ControlPanel.java
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
import javax.swing.plaf.basic.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;


public class ControlPanel extends JFrame //provides a gui for pause, resume, speed up and slow down operations
{
  private boolean paused = false;
  private Controllable controllable = null;
  private JButton pauseButton;
  private JFrame parentFrame = null;
  private ParentListener parentListener = null;
  private CPListener cpListener = null;


  public static interface Controllable //interface defined by object that will respond to input from the control panel
  {
    public void pause();
    public void resume();
    public void speedUp();
    public void slowDown();
    public void panelClosed();
  }
  
  public static class ButtonAdapter implements java.awt.event.ActionListener
  {
    public ButtonAdapter(){}
    public void actionPerformed(ActionEvent e){}
  }

 

  private static class CPListener extends ComponentAdapter //listens for close or iconizing of parent and does the same
  {
    private ControlPanel controlPanel;
    
    public CPListener(ControlPanel cp)
      {
	super();
	controlPanel = cp;
      }
    
    public void componentMoved(ComponentEvent e)
      {
	System.out.println("CPListener::componentMoved");
	if (controlPanel.parentFrame != null)
	  {
	    System.out.println("     " + controlPanel.parentFrame.getTitle());
	    ((Graph)controlPanel.parentFrame).redraw();
	    //controlPanel.parentFrame.repaint();
	  }
      }
    
    public void componentResized(ComponentEvent e)
      {
	System.out.println("CPListener::componentResized");
	if (controlPanel.parentFrame != null)
	  {
	    System.out.println("     " + controlPanel.parentFrame.getTitle());
	    controlPanel.parentFrame.repaint();
	  }
      }
  }
    
  private static class ParentListener extends  WindowAdapter //listens for close or iconizing of parent and does the same
  {
    private JFrame childFrame;
    
    public ParentListener(JFrame cf)
      {
	super();
	childFrame = cf;
      }
    public void windowIconified(WindowEvent e)
      {
	childFrame.setState(Frame.ICONIFIED);
      }
    public void windowDeiconified(WindowEvent e)
      {
	childFrame.setState(Frame.NORMAL);
      }
    public void windowClosed(WindowEvent e)
      {
	childFrame.dispose();
      }
  }
  
  public ControlPanel(Controllable ctl, MonitorManager w, String nm, JFrame jif)
    {
      super();
      parentListener = new ParentListener(this);
      setParentFrame(jif);
      controllable = ctl;
      //set properties so individual frame may be closed, resized or iconified
      setResizable(true);
      
      //set layout manager to GridBagLayout
      Container contentPane = getContentPane();
      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      contentPane.setLayout(gridbag);
      
      JLabel speedLbl = new JLabel("Speed Up:");
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 1;
      c.gridheight = 2;
      gridbag.setConstraints(speedLbl, c);
      contentPane.add(speedLbl);
      
      setBackground(speedLbl.getBackground());
      
      BasicArrowButton speedUp = new BasicArrowButton(BasicArrowButton.NORTH);
      c.gridx = 1;
      c.gridy = 0;
      c.gridwidth = 1;
      c.gridheight = 1;
      c.ipady = 8;
      c.ipadx = 8;
      gridbag.setConstraints(speedUp, c);
      contentPane.add(speedUp);
      speedUp.addActionListener(new ButtonAdapter() {
	public void actionPerformed(ActionEvent e) {
	  controllable.speedUp();}
      });
      
      
      BasicArrowButton slowDown = new BasicArrowButton(BasicArrowButton.SOUTH);
      c.gridx = 1;
      c.gridy = 1;
      c.gridwidth = 1;
      c.gridheight = 1;
      gridbag.setConstraints(slowDown, c);
      contentPane.add(slowDown);
      slowDown.addActionListener(new ButtonAdapter() {
	public void actionPerformed(ActionEvent e) {
	  controllable.slowDown();}
      });
      
      
      JPanel empty = new JPanel();
      c.gridx = 0;
      c.gridy = 2;
      c.gridwidth = 2;
      c.gridheight = 1;
      c.ipady = 1;
      c.ipadx = 0;
      gridbag.setConstraints(empty, c);
      contentPane.add(empty);
      
      pauseButton = new JButton("Pause");
      c.gridx = 0;
      c.gridy = 3;
      c.gridwidth = 2;
      c.gridheight = 1;
      c.ipady = 0;
      c.anchor = GridBagConstraints.SOUTH;
      gridbag.setConstraints(pauseButton, c);
      contentPane.add(pauseButton);
      pauseButton.addActionListener(new ButtonAdapter() {
	public void actionPerformed(ActionEvent e) {
	  if (paused)
	    {
	      paused = false;
	      controllable.resume();
	      pauseButton.setText("Pause");
	    }
	  else
	    {
	      paused = true;
	      controllable.pause();
	      pauseButton.setText("Resume");
	    }
	  pauseButton.revalidate();
	}
      });
      addWindowListener(new WindowAdapter(){
	public void windowClosing(WindowEvent e)
	  {
	    controllable.panelClosed();
	  }
      });
      cpListener = new CPListener(this);
      addComponentListener(cpListener);
      setTitle(nm);
      setLocation(parentFrame.getLocation());
      setSize(new Dimension(150,110));
      setVisible(true);
      w.addToDesktop(this);
    }
  
  public void setParentFrame(JFrame jif)
    {
      if (parentFrame != null)
	{
	  parentFrame.removeWindowListener(parentListener);
	  parentFrame = null;
	}
      parentFrame = jif;
      parentFrame.addWindowListener(parentListener);
    }
  
}
