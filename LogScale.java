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
