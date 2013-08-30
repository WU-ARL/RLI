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
 * File: BWScrollBar.java
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

public class BWScrollBar extends JScrollBar implements AdjustmentListener
{
  protected BoxedRangeModel boxedRange = null;
  private boolean settingValues = false;
  private int valueLength; //length of the bar
  private BRListener brListener = null;
  private int oldExtent = 0;
  private double oldmax = 0;
  private double oldmin = 0;
  protected class BRListener extends BoxedRangeAdapter
  {
    private BWScrollBar scrollBar;
    public BRListener(BWScrollBar sb)
      {
	scrollBar = sb;
      }
    public void changeVisible(BoxedRangeEvent e)
      {
	if (!scrollBar.getValueIsAdjusting()) scrollBar.calculateValues();
      }
    public void changeBoundaries(BoxedRangeEvent e)
      {
	if (!scrollBar.getValueIsAdjusting()) scrollBar.calculateValues();
      }
  }

  private class SBMouseAdapter extends MouseAdapter
  {
    private boolean pressed = false;
    private BWScrollBar scrollBar;
    public SBMouseAdapter(BWScrollBar sb) 
      { 
	super();
	scrollBar = sb;
      }
    public void mousePressed(MouseEvent e) { pressed = true;}
    public void mouseReleased(MouseEvent e)
      {
	if (pressed)
	  {
	    pressed = false;
	    scrollBar.calculateValues();
	  }
      }
    public void mouseExited(MouseEvent e)
      {
	if (pressed)
	  {
	    pressed = false;
	    scrollBar.calculateValues();
	  }
      }
  }


  public BWScrollBar(int or, int lnth, BoxedRangeModel br)
    {
      super(or);
      valueLength = lnth;
      brListener = new BWScrollBar.BRListener(this);
      addAdjustmentListener(this);
      addMouseListener(new SBMouseAdapter(this));
      setBoxedRange(br);
    }

  public BoxedRangeModel getBoxedRange()
    {
      return boxedRange;
    }

  public void setBoxedRange(BoxedRangeModel br)
    {
      if (boxedRange != null)
	{
	  boxedRange.removeBoxedRangeListener(brListener);
	  boxedRange = null;
	}

      if (br != null)
	{
	  boxedRange = br;
	  boxedRange.addBoxedRangeListener(brListener);
	  setEnabled(true);
	}
      else setEnabled(false);
      calculateValues();
    }


  public boolean calculateValues()
    {
      if (!getValueIsAdjusting())
	{
	  settingValues = true;
	  int newValue = 0;
	  int newExtent = valueLength;
	  int diff = 0;
	  if (boxedRange != null)
	    {
	      if (orientation == HORIZONTAL)
		{
		  newValue = (int)(((boxedRange.getXValue() - boxedRange.getXMin())*valueLength)/(boxedRange.getXMax() - boxedRange.getXMin()));
		  newExtent = (int)((boxedRange.getXExtent()*valueLength)/(boxedRange.getXMax() - boxedRange.getXMin()));
		}
	      else
		{
		  newValue = (int)(((boxedRange.getYValue() - boxedRange.getYMin())*valueLength)/(boxedRange.getYMax() - boxedRange.getYMin()));
		  newExtent = (int)((boxedRange.getYExtent()*valueLength)/(boxedRange.getYMax() - boxedRange.getYMin()));
		  newValue = valueLength - (newExtent + newValue);
		}
	      if (newValue > valueLength) newValue = valueLength;
	      oldExtent = newExtent;
	      if (orientation == HORIZONTAL)
		{
		  oldmax = boxedRange.getXMax(); 
		  oldmin = boxedRange.getXMin(); 
		}
	      else
		{   
		  oldmax = boxedRange.getYMax(); 
		  oldmin = boxedRange.getYMin();
		}
	    }
	  //System.out.println("BWScrollBar::calculateValues");
	  setValues(newValue, newExtent, 0, valueLength);
	}
      return true;
    }


  public void adjustmentValueChanged(AdjustmentEvent e)
    {
      if (!settingValues)
	{
	  //System.out.println("BWScrollBar::adjustmentValueChanged");
	  double newValue;
	  if (orientation == HORIZONTAL)
	    {
	      newValue = ((getModifiedValue() * (boxedRange.getXMax() - boxedRange.getXMin()))/valueLength) + boxedRange.getXMin();
	      //System.out.println("horizontal setting xvalue to " + newValue); 
	      //if (newValue >= oldmin) boxedRange.setXValue(newValue);
	      boxedRange.setXValue(newValue);
	    }
	  else
	    {
	      newValue = ((getModifiedValue() * (boxedRange.getYMax() - boxedRange.getYMin()))/valueLength) + boxedRange.getYMin();
	      //System.out.println("vertical setting yvalue to " + newValue); 
	      //if (newValue >= oldmin) boxedRange.setYValue(newValue);
	      boxedRange.setYValue(newValue);
	    }
	}
      else settingValues = false;
    }

  public int getValueLength() 
    {
      return valueLength;
    }
  public void setValueLength(int v)
    {
      valueLength = v;
    }
  public int getModifiedValue()
    {
      int rtn = getValue();
      if (orientation == VERTICAL)
	{
	  rtn = valueLength - (getVisibleAmount() + rtn);
	}
      return rtn;
    }
}
