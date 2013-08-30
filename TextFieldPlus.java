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
 * File: TextFieldPlus.java
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
import java.awt.event.*;
import java.text.*;
import javax.swing.text.*;

public class TextFieldPlus extends JTextField
{
  private boolean firstSelection = true;
  private boolean isDefault = false;


  public static class ONLInteger //extends Integer
  {
    private int value = 0;
    private String string = null;
    public ONLInteger(int v) { value = v;}
    public ONLInteger(String s) throws java.text.ParseException
    {
      try 
        {
          long tmp_int = Long.decode(s).longValue();
          if (tmp_int > Integer.MAX_VALUE)
            {
              //tmp_int -= Integer.MAX_VALUE;
              value = (int)((tmp_int - (2*Integer.MAX_VALUE)) - 2);
            }
          else value = (int) tmp_int;
          string = s;
        }
      catch (NumberFormatException e)
        { 
          throw new java.text.ParseException("ONL.ONLInteger error: not in unsigned int range", 0);
        }
    }
    public static int getInt(String s)
    {
      int rtn = 0;
      try 
        {
          long tmp_int = Long.decode(s).longValue();
          if (tmp_int > Integer.MAX_VALUE)
            {
              //tmp_int -= Integer.MAX_VALUE;
              rtn = (int)((tmp_int - (2*Integer.MAX_VALUE)) - 2);
            }
          else rtn = (int) tmp_int;
        }
      catch (NumberFormatException e)
        { 
        }
      return rtn;
    }
    public String toString()
    {
      if (string == null) return (String.valueOf(value));
      else return string;
    }
    public int intValue() { return value;}
  }

  public static class NumberTextField extends JFormattedTextField //implements PropertyChangeListener,CaretListener
  {
    private NumberFormat numberFormat = null;
    private KMAction kmaction = null;
    private boolean positiveOnly = true;

    private class KMAction extends AbstractAction
    {
      private Keymap kmap = null;
      private javax.swing.Action action = null;
      private String currentValue = "";
      public KMAction(Keymap km) { this(km, true);}
      public KMAction(Keymap km, boolean pos)
      {
        super("ntaction");
        kmap = km;
        action = kmap.getDefaultAction();
        if (action == null) 
          ExpCoordinator.print("Keymap.action null", 2);
        kmap.setDefaultAction(this);
        positiveOnly = pos;
      }
      public void actionPerformed(ActionEvent e)
      {
        ExpCoordinator.print(new String("Keymap.actionPerformed " + e.getActionCommand()), 2);
        if (action != null) 
          {
            ParsePosition ppos = new ParsePosition(0);
            currentValue = getText();
            
            action.actionPerformed(e);
            String str = getText();
            int strln = str.length();
            Number rtn = numberFormat.parse(str, ppos);
            if (rtn == null || (ppos.getIndex() < strln) || (rtn.intValue() < 0 && positiveOnly))
              {
                ExpCoordinator.print(new String("illegal key entered " + str + " string length = " + strln + " index = " + ppos.getIndex() + " errorIndex = " + ppos.getErrorIndex() + " : " + rtn), 2);
                setText(currentValue);
              }  
          }
      }
    }
    public NumberTextField(NumberFormat nf)
    {
      super(nf);
      numberFormat = nf;
      kmaction = new KMAction(getKeymap());
    } 
  } //end inner class NumberTextField



  ///////////////////////////////////////// TextFieldPlus ///////////////////////////////////////////////////////////////////////////
  public TextFieldPlus(int x) 
    { 
      super(x);
      MouseAdapter ma = new  MouseAdapter(){
	public void mousePressed(MouseEvent e)
	  {
	    if (firstSelection)
	      {
		//System.out.println("TextFieldPlus::resetting text " + getText());
		firstSelection = false;
                setText("");
		revalidate();
		//repaint();
	      }
	  }
      };
      addMouseListener(ma);
    }
  public TextFieldPlus(int x, String txt)
    {
      this(x);
      setText(txt);
    }
  public TextFieldPlus(int x, String txt, boolean def)
    {
      this(x, txt);
      isDefault = def;
    }
  public String getText()
    {
      if (firstSelection && !isDefault) return (new String(""));
      else return super.getText();
    }
}
