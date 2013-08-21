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
