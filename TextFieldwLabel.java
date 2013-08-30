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
 * File: TextFieldwLabel.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/25/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.awt.event.*;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Color;
import java.beans.PropertyChangeEvent;

public class TextFieldwLabel extends ONL.ComponentwLabel
{
  protected JTextField textField = null;
  protected JLabel label = null;
  private ONL.ExpPropertyAction observeAction = null;

  public TextFieldwLabel(JTextField tf, String lbl)
    {
      super(tf,lbl);
      textField = tf;
    }
  public TextFieldwLabel(int x) 
    { 
      this(new JTextField(x), "");
    }
  public TextFieldwLabel(int x, String txt)
    {
      this(new JTextField(x), txt);
      //textField.setPreferredSize(new Dimension(x, getHeight()));
      //textField.setMaximumSize(new Dimension(x, getHeight()));
    }
  public Object getValue()
    {
      return (textField.getText());
    }
  public String getText() { return (textField.getText());}
  public int getInt() { return (Integer.parseInt(textField.getText()));}
  public double getDouble() { return (Double.parseDouble(textField.getText()));}
  public void setText(String s) { textField.setText(s);}
  public void setValue(String s) 
  { 
    ExpCoordinator.print(new String("TextFieldwLabel.setValue " + s), 3);
    textField.setText(s);
  }
  public void setValue(int s) { setValue(String.valueOf(s));}
  public void setValue(double s) { setValue(String.valueOf(s));}
  public void setValue(boolean s) { setValue(String.valueOf(s));}
  public void addActionListener(ActionListener l) { textField.addActionListener(l);} 
  public void removeActionListener(ActionListener l) { textField.removeActionListener(l);}
  public void setEditable(boolean b) { textField.setEditable(b);}
  public JTextField getTextField() { return textField;}
  public void setObserveAction(boolean ign_obs, boolean exp_wide)
  {
    if (observeAction == null && !ign_obs) 
      {
        observeAction = (new ONL.ExpPropertyAction(exp_wide)
          {            
            public void propertyChange(PropertyChangeEvent e)
            {
              //ExpCoordinator.print(new String("ONL.UserAction.propertyChange action " + getValue(Action.NAME) + " ignore = " + ignoreObsStatus + " isobserver = " + ExpCoordinator.isObserver()), 3);
              if (e.getPropertyName().equals(ExpCoordinator.OBSERVE))
                textField.setEnabled(!ExpCoordinator.isObserver());
            }
          });
        textField.setEnabled(!ExpCoordinator.isObserver());
      }
    else 
      {
        if (ign_obs)
          {
            ExpCoordinator.theCoordinator.removePropertyListener(ExpCoordinator.OBSERVE, observeAction);
            observeAction = null;
          }
      }
  }
  public void setDisabledTextColor(Color c) { textField.setDisabledTextColor(c);}
}
