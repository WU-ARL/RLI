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
 * File: LogScale.java
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
import java.lang.Math;
import javax.swing.*;

public class LogScale //translates a set of data into log base x. by default x = 2.
{
  public static interface Scaleable
  {
    public void setLogScale(boolean v);
    //public boolean isLogScale();
  }

  public static class LSMenuItem extends JMenuItem
  {
    private boolean isOn = false;
    private LogScale.Scaleable scaleable;
    public LSMenuItem(LogScale.Scaleable s)
      {
	super("Set Log Scale");
	scaleable = s;
	MenuDragMouseAdapter mdma = new MenuDragMouseAdapter() {
	  public void select() { 
	    scaleable.setLogScale(!isOn);
	    isOn = !isOn;
	    if (!isOn) setText("Set Log Scale");
	    else setText("Unset Log Scale");
	    repaint();
	  }
	};
	addMenuDragMouseListener(mdma);
	addMouseListener(mdma);
      }
    public boolean isLog() { return isOn;}
  }

  public static class Data extends NumberData implements BoxedRangeListener
    {
      private NumberData numberData;
      private Units.Converter uconverter = null;
      private double ln_base;
      public Data(NumberData nd, int dunits)
	{
	  this(nd, dunits, 2);
	}
      public Data(NumberData nd, int dunits, double base)
	{
	  super(nd.getDataType(), 0, 0, 0, 0, nd.getMaxSize(), NumberData.DROP_OLD); 
	  ln_base = Math.log(base);
	  numberData = nd;
	  int max = numberData.getSize();
	  double log_y;
	  NumberPair np;
	  NumberPair newNP;
	  boolean show = false;
	  if (dunits != nd.getDataType().getDataUnits()) uconverter = new Units.Converter(dunits, nd.getDataType().getDataUnits());
	  for (int i = 0; i < max; ++i)
	    {
	      np = nd.getElementAt(i);
	      log_y = np.getY();
	      if (log_y <= 0)
		{
		  //System.out.println("LogScale::cconstructor y = " + log_y);
		  log_y = 0.000001;
		  // show = true;
		}
	      if (uconverter != null) log_y = uconverter.convert(log_y);
	      log_y = Math.log(log_y)/ln_base;
	      //if (show)
	      //	{
		  //System.out.println("      log(y) = " + log_y);
	      //	  show = false;
	      //	}
	      addElement(np.getX(), log_y);
	    }
	  System.out.println("LogScale.Data xmin,xmax,ymin,ymax = " + xmin + "," + xmax + "," + ymin + "," + ymax);  
	  numberData.addBoxedRangeListener(this);
	}  
      
      //BoxedRangeListener interface
      public void changeVisible(BoxedRangeEvent e){}
      public void changeBoundaries(BoxedRangeEvent e){}
      public void changeXVisible(BoxedRangeEvent e){}
      public void changeYVisible(BoxedRangeEvent e){}
      public void changeYBounds(BoxedRangeEvent e){}
      public void changeXBounds(BoxedRangeEvent e)
	{
	  boolean show = false;
	  NumberData nd = (NumberData) e.getSource();
	  if (nd == numberData)
	    {
	      NumberPair np = numberData.getLastElement();
	      if ((e.getType() & BoxedRangeEvent.XMAX_CHANGED) != 0)
		{
		  if (stopped) start(); 
		  double log_y = np.getY();
		  if (log_y <= 0)
		    {
		      //System.out.println("LogScale::changeXBounds y = " + log_y);
		      log_y = 0.000001;
		      show = true;
		    }
		  if (uconverter != null) log_y = uconverter.convert(log_y);
		  log_y = Math.log(log_y)/ln_base;
		  //if (show) System.out.println("      log(y) = " + log_y);
		  addElement(np.getX(), log_y);
		}
	    }
	} 
      
      public void stop()
	{
	  numberData = null;
	  //vector.clear();
	  clearData();
	}
    }
}
