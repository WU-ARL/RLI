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
