/*
 * Copyright (c) 2003-2013 Jyoti Parwatikar
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
 * File:AddFormulaWindow.java
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
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.IOException;
import java.lang.reflect.Array;
import javax.xml.stream.*;

public class AddFormulaWindow extends JFrame implements MonitorFunction
{
  private Graph display;
  private Vector monitors;
  //private JButton buttons[];
  private BListener buttonListener;
  private AddListener addListener;
  private int opCounter = 0;
  private JTextField name;
  private JLabel formula;
  private MonitorManager monitorManager;
  private boolean done = false;
  private final String MON_START = "M(";
  private final String MON_END = ")";

  private class BListener implements ActionListener
  {
    public BListener(){}
    public void actionPerformed(ActionEvent e)
      {
	JButton button = (JButton)e.getSource();
	String val = button.getText();
	if (val.equals("+") || val.equals("-"))
	  {
	    ++opCounter;
	    addFormulaText(new String(" " + val + " "));
	  }
	else  
	  addFormulaText(val);
      }
  }
  private class AddListener implements ActionListener
  {
    public AddListener(){}
    public void actionPerformed(ActionEvent e)
      {
	JButton button = (JButton)e.getSource();
	String val = button.getText();
	if (val.equals("Add"))
	  createFormula();
	else cancel();
      }
  }
  public AddFormulaWindow(Graph g, MonitorManager mw)
    {
      super("Add Formula");
      setResizable(true);
      monitorManager = mw;
      display = g;
      monitors = new Vector();
      buttonListener = new BListener();
      addListener = new AddListener();
      //layout
      Container contentPane = getContentPane();
      //BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
      contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

      JPanel tmp_panel;

      //create name panel
      tmp_panel = new JPanel();
      tmp_panel.add(new JLabel("name: "));
      name = new JTextField(10);
      tmp_panel.add(name);
      tmp_panel.setMinimumSize(new Dimension(150, 20));
      tmp_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      contentPane.add(tmp_panel);

      //create formula text display
      formula = new JLabel();
      formula.setBorder(BorderFactory.createLineBorder(Color.black, 1));
      formula.setBackground(Color.white);
      formula.setMinimumSize(new Dimension(150, 20));
      formula.setSize(new Dimension(150, 20));
      formula.setAlignmentX(Component.LEFT_ALIGNMENT);
      //formula.setHorizontalAlignment(SwingConstants.LEFT);
      //formula.setHorizontalTextPosition(SwingConstants.LEFT);
      formula.setFont(new Font("Dialog", Font.PLAIN, 9));
      contentPane.add(formula);

      //create number and op buttons
      tmp_panel = new JPanel();
      tmp_panel.setLayout(new GridLayout(4,4));
      JButton buttons[] = new JButton[13];
      for (int i = 0; i < 13; ++i)
	{
	  if (i < 10) buttons[i] = new JButton(Integer.toString(i));
	  else
	    {
	      if (i == 10) buttons[i] = new JButton("+");
	      if (i == 11) buttons[i] = new JButton("-");
	      if (i == 12) buttons[i] = new JButton(".");
	    }
	  buttons[i].setFont(new Font("Dialog", Font.PLAIN, 9));
	  buttons[i].addActionListener(buttonListener);
	}
      //row 1
      tmp_panel.add(buttons[7]);
      tmp_panel.add(buttons[8]);
      tmp_panel.add(buttons[9]);
      tmp_panel.add(buttons[10]);
      //row 2
      tmp_panel.add(buttons[4]);
      tmp_panel.add(buttons[5]);
      tmp_panel.add(buttons[6]);
      tmp_panel.add(buttons[11]);
      //row 3
      tmp_panel.add(buttons[1]);
      tmp_panel.add(buttons[2]);
      tmp_panel.add(buttons[3]);
      tmp_panel.add(buttons[12]);
      //row 4
      tmp_panel.add(buttons[0]);
      //add button panel to this frame
      tmp_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      contentPane.add(tmp_panel);

      //add and cancel buttons

      tmp_panel = new JPanel();
      JButton tmp_button = new JButton("Cancel");
      tmp_button.addActionListener(addListener);
      tmp_panel.add(tmp_button);
      tmp_button = new JButton("Add");
      tmp_button.addActionListener(addListener);
      tmp_panel.add(tmp_button);
      tmp_panel.setMinimumSize(new Dimension(150, 20));
      tmp_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      contentPane.add(tmp_panel);

      //add as the current Monitor Function
      monitorManager.setCurrentMFunction(this);
      monitorManager.getParamSelector().setListenEnable(false);

      //add a listener for a close
      addWindowListener(new WindowAdapter(){
	public void windowClosing (WindowEvent e)
	  {
	    if (!done) cancel();
	  }
      });
    }
  
  public void addFormulaText(String txt)
    {
      String f_txt = formula.getText();
      formula.setText(new String(f_txt + txt));
      formula.revalidate();
      repaint();
    }
  
  //interface MonitorFunction 
  public void addData(MonitorPlus nd)
    {
      monitors.add(nd);
      addFormulaText(new String(MON_START + nd.getDataType().getName() + MON_END));
    }
  public void removeData(MonitorPlus nd){}
  public void addConditional(Conditional c){}
  public void removeConditional(Conditional c){}
  //end interface MonitorFunction

  public void createFormula()
    {
      String str_array[] = formula.getText().split(" ");
      int array_len = Array.getLength(str_array);
      int num_params = array_len - opCounter;
      ExpCoordinator.print("AddFormulaWindow::createFormula num_params = " + num_params);
      if (num_params > 0)
	{
	  MonitorAlgFunction maf = new MonitorAlgFunction(new MonitorDataType.MFormula(name.getText(), num_params, Units.UNKNOWN, formula.getText()), monitorManager);
	  MonitorPlus mp;
	  String token;
	  int op = MonitorAlgFunction.ADD;
          int i = 0;
	  while(i < array_len)
	    {
	      token = str_array[i++];
	      if (token.startsWith(MON_START))
		{
		  mp = (MonitorPlus)monitors.remove(0);
		  maf.addPending(mp, op);
		  mp.addMonitorFunction(maf);
		  mp.removeMonitorFunction(this);
		  ExpCoordinator.print("    add monitor " + mp.getDataType().getName());
		  while(!token.endsWith(MON_END))
		    token = str_array[i++];
		}
              else
		{
		  if (token.equals("+")) 
		    {
		      op = MonitorAlgFunction.ADD;
		      maf.setNextOp(op);
		    }
		  else 
		    {
		      if (token.equals("-")) 
			{
			  op = MonitorAlgFunction.SUBTRACT;
			  maf.setNextOp(op);
			}
		      else
			{
			  ExpCoordinator.print("    add constant " + token);
			  maf.addPending(Double.parseDouble(token), op);
			}
		    }
                }
	    }
	  maf.addMonitorFunction(display);
	}
      close();
    }
  public void cancel()
    {
      int max = monitors.size();
      MonitorPlus mp;
      for (int i = 0; i < max; ++i)
	{
	  mp = (MonitorPlus)monitors.remove(0);
	  mp.removeMonitorFunction(this);
	}
      close();
    }
  
  public void close()
    {
      if (!done)
	{
	  done = true;
	  monitorManager.removeCurrentMFunction(this);
	  monitorManager.getParamSelector().setListenEnable(true);
	  if (isShowing())
	    {
		  dispose();
		  /*
		    }
		    catch (java.beans.PropertyVetoException e2)
		    {
		    System.err.println("Error: AddFormulaWindow::close can't close frame");
		    }*/
	    }
	}
    }
  public SpecFile.MFunctionSpec getSpec() { return null;}
  public Param getParam(MonitorDataType.Base mdt) { return null;}
  public Param readParam(ONL.Reader rdr) throws IOException { return null;}
  public void write(ONL.Writer wrtr) throws IOException {}
  public void read(ONL.Reader rdr)throws IOException {}
  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException {}
}
