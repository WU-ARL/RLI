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
