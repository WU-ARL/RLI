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
 * File: NumberedAxis.java
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

/**component class that is in charge of drawing an axis using the a given range of values. this range may be 
 * changed dynamically.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.*;
import java.lang.Math;
import java.text.DecimalFormat;
import java.util.Vector;

public class NumberedAxis extends Axis implements LogScale.Scaleable
{
  private double pixPerUnit = 0; //pixels per unit of measure
  private double max;
  private double min;
  private static int APPROX_SPACE_BETWEEN = 40;
  private int scale = -1;
  private double divisibleBy = 0;
  private double transFactor = 0;
  private boolean drawn = true;
  private int unDrawnLength = 0;
  private double maxVal = 0;
  private boolean logScale = false;
  private ResizeListener rListener = null;
  private DecimalFormat dformat = null;
  private Vector divisions;
  private boolean fakeresize = false; //just marks when a resize is forced because the min and the max are the same

  private class ResizeListener extends MouseAdapter
  {
    private NumberedAxis axis;
    public ResizeListener(NumberedAxis a)
      {
	axis = a;
      }
    public void mouseClicked(MouseEvent e)
      {
	//if (e.getClickCount() >= 2)
	//{
	//System.out.println("NumberedAxis.ResizeListener::mouseClicked double click");
	double coord;
	if (axis.isXAxis) coord = e.getX();
	else coord = e.getY();
	
	double val = (axis.getValue((int)coord)) - axis.min;
	
	if (axis.arrow != null && (axis.arrow.getBounds().contains(e.getPoint())))
	  {
	    val = 1.2 *(axis.max - axis.min);
	  }
	
	if (val > 0)
	  {
	    if (axis.isXAxis) axis.graph.resizeXVal(val);
	    else axis.graph.resizeYVal(val);
	  }
	//}
      }
  }

  
  public NumberedAxis(Graph gr, boolean xory)
    {
      super(gr, xory);
      spaceBetweenDivs = APPROX_SPACE_BETWEEN;
      dformat = new DecimalFormat();
      dformat.setGroupingSize(3);
      divisions = new Vector();
    }
  
  public NumberedAxis(Graph gr, boolean xory, int s)
    {
      this(gr, xory, s, 1); 
    }
  
  public NumberedAxis(Graph gr, boolean xory, int s, double d)
    {
      this(gr, xory);
      spaceBetweenDivs = APPROX_SPACE_BETWEEN;
      scale = s;
      divisibleBy = d;
      dformat.setMaximumFractionDigits(s);
    }
  
  public void setLogScale(boolean v) { logScale = v;}
  public boolean isLogScale() { return logScale;}
  public void setResizeable(boolean v)
    {
      if (v && rListener == null)
	{
	  rListener = new ResizeListener(this);
	  this.addMouseListener(rListener);
	  setArrow(true);
	}
      if (!v && rListener != null)
	{
	  this.removeMouseListener(rListener);
	  rListener = null;
	  setArrow(false);
	}
    }    

  public boolean isResizeable() { return (rListener != null);}

  public void setScale(int s)
    { 
      dformat.setMaximumFractionDigits(s);      
      scale = s;
    }

  public void setNoDraw(boolean b, int lnth)
    {
      if (b)
	{
	  drawn = false;
	  unDrawnLength = lnth;
	  if (isXAxis)
	    {
	      right = lnth;
	      left = 0;
	    }
	  else
	    {
	      top = 0;
	      bottom = lnth;
	    }
	}
      else drawn = true;
    }
  
  public void setDivisibleBy(double d)
    { divisibleBy = d;}
  
  public int length()
    {
      if (drawn)
	return (super.length());
      else return unDrawnLength;
    }

  public void updateDimensions()
    {
      double omin = min;
      double omax = max;
      int onod = numberOfDivs;
      double oppu = pixPerUnit;
      if (drawn) updateBounds();
      BoxedRangeModel tmp_limits = graph.getBoxedRange();
      //System.out.println("NumberedAxis::updateDimensions");
      //tmp_limits.print();
      if (isXAxis) 
	{
	  min = tmp_limits.getXValue();
	  max = min + tmp_limits.getXExtent();
	}
      else
	{
	  min = tmp_limits.getYValue();
	  max = min + tmp_limits.getYExtent();
	}
      

      if (length() < 0) return;
      numberOfDivs = (int) (length()/APPROX_SPACE_BETWEEN);
      if ((max-min) > 0) pixPerUnit = (length())/(max - min);
      else pixPerUnit = 0;

      if (isXAxis) 
	transFactor = left - (min * pixPerUnit);
      else 
	transFactor = length() + top + (min * pixPerUnit);
      double divunit = (double)((max - min)/numberOfDivs);
      double value = (double)min;
      for (int i = 0; i < numberOfDivs; ++i)
	value += divunit;
      maxVal = value; 
      if ((omin != min) ||
	  (omax != max) ||
	  (oppu != pixPerUnit) ||
	  (onod != numberOfDivs))
	{
	  redoDivisions(divunit);
	}
    }

  public void redoDivisions(double divunit)
    {  
      int i;
      if (numberOfDivs <= 0) return;
      
      double neg_scale =  0 - scale;
      double scale_pow =  Math.pow(10, neg_scale);
      if (divunit <  Math.pow(10, (0 - scale))) 
	{
	  scale += 1;
	  while (divunit <  Math.pow(10, (0 - scale))) scale += 1;
	  dformat.setMaximumFractionDigits(scale);
	  //System.out.println("NA: neg_scale " + neg_scale + " scale_pow = " + scale_pow + " divunit = " + divunit);
	}
      else
	{
	  if ((divunit > Math.pow(10, (1-scale))) && (scale > 1))
	    {
	      scale -= 1;
	      while ((divunit >  Math.pow(10, (1 - scale))) && (scale > 1)) scale -= 1;
	      dformat.setMaximumFractionDigits(scale);
	    }
	}
      //use number of marks to find val
      int position;
      int max_length = 0;
      
      double value = min;
      //if (scale >= 0) value = value.setScale(scale, BigDecimal.ROUND_UP);
      
      int vsize = divisions.size();
      Division elem;
      for (i = 0; i <= numberOfDivs; ++i)
	{
	  position = getCoordinate(value, true);
	  if (i < vsize) 
	    {
	      elem = (Division)divisions.elementAt(i);
	      elem.setDimensions(new String(dformat.format(value)), position);
	    }
	  else 
	    {
	      elem = new Division(new String(dformat.format(value)), position);
	      divisions.add(elem);
	    }
	  if (elem.getLength() > max_length) max_length = elem.getLength();
	  value += divunit;
	}
      if (!isXAxis)
	setPreferredSize(new Dimension((max_length + 15), 50));
    }

  public double getTransFactor() { return transFactor;} 

  public double getScaleFactor() 
    {
      if (isXAxis)
	return pixPerUnit;
      else
	return (-1 * pixPerUnit);
    }

  public double getValue(int coord)
    {
      double rtn = min;
      if (max > min)
	{
	  if (isXAxis)
	    {
	      rtn = (coord - transFactor)/pixPerUnit;
	    }
	  else 
	    {
	      rtn = (transFactor - coord)/pixPerUnit; // because the origin is at the top-left corner
	    }
	  if (logScale) rtn = Math.exp(rtn);
	}
      return rtn;
    }
  
  public int getCoordinate(double val) { return (getCoordinate(val, false));}
  protected int getCoordinate(double val, boolean isLog)
    {
      double value = val;
      if (logScale && !isLog) value = Math.log(val);
      if (val <= min) return ((int)transFactor);
      else
      {
	if (isXAxis)
	  {
	    return ((int)((value * pixPerUnit) + transFactor));
	  }
	else 
	  {
	    return ((int)(transFactor - (value * pixPerUnit))); // because the origin is at the top-left corner
	  }
      }
    }
  
  public BigDecimal getScaledValue(double value)
    {
      //System.out.println("NumberedAxis::getScaledValue value is " + value);
      BigDecimal rtn = null;
      if (value == Double.NEGATIVE_INFINITY) 
	{
	  System.out.println("NumberedAxis::getScaledValue negative infinity");
	  rtn = new BigDecimal(value);
	}
      else
	{
	  if (divisibleBy > 0)
	    {
	      double remainder = value - ((int)(value/divisibleBy))*divisibleBy;
	      if (remainder > 0) rtn = new BigDecimal((double)(value - remainder + divisibleBy));
	      else rtn = new BigDecimal(value);
	    }
	  else rtn = new BigDecimal(value);
	  if (scale >= 0) rtn = rtn.setScale(scale, BigDecimal.ROUND_UP);
	}
      return rtn;
    }

  public double getMaxValue() 
    { 
      return maxVal;
    }
  public double getMinValue() { return min;}

  protected Vector getDivisions() { return divisions;}
  
  public void paintComponent(Graphics g)
    {
      super.paintComponent(g);

      //System.out.println("NumberedAxis::paintComponent");
      
      int vsize = divisions.size();
      Division elem;
      //use number of marks to find val
     
      for (int i = 0; i <= numberOfDivs; ++i)
	{
	  if (i < vsize)
	    {
	      elem = (Division)divisions.elementAt(i);
	      elem.drawDivision(g);
	    }
	}
    }
}
