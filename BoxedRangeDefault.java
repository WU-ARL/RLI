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
 * File: BoxedRangeDefault.java
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

import java.util.*;
import javax.swing.event.*;

public class BoxedRangeDefault implements BoxedRangeModel
{
  private EventListenerList listeners;
  //outer boundary
  protected double xmax = 0;
  protected double xmin = 0;
  protected double ymax = 0;
  protected double ymin = 0;

  //inner visible region
  protected double xvalue = 0;
  protected double xextent = 0;
  protected double yvalue = 0;
  protected double yextent= 0;

  private boolean hold = false;
  private int changed = 0;


  public BoxedRangeDefault() 
    {
      listeners = new EventListenerList();
    }

  //pass outer boundary and make visible region equal to boundaries
  public BoxedRangeDefault(double xmx, double xmn, double ymx, double ymn) //x-maximum, x-minimum, y-maximum, y-minimum
    {
      xmin = xvalue = xmn;
      xmax = xmx;
      xextent = xmx - xmn;
      
      ymin = yvalue = ymn;
      ymax = ymx;
      yextent = ymx - ymn;

      listeners = new EventListenerList();
    }


  public void setHold(boolean b)
    {
      if (hold != b)
	{
	  hold = b;
	  if (!hold && changed > 0)
	    {
	      changed = 0;
	      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.ALL_CHANGED));
	    }
	}
    }

  public boolean getHold() { return hold;}


  public void initializeFrom(BoxedRangeDefault b)
    {
      setBoundary(b.getXMax(), b.getXMin(), b.getYMax(), b.getYMin());
      setVisible(b.getXValue(), b.getXExtent(), b.getYValue(), b.getYExtent());
      EventListenerList tmp_listeners = b.getListeners();
      int max = tmp_listeners.getListenerCount();
      int i = 0;
      Object[] list = tmp_listeners.getListenerList();
      BoxedRangeListener l;
      //turns out this is a list of class, listener pairs so list looks like class1, listener1, class2, listener2 ... classn, listenern
      for (i = 1; i < list.length; i += 2)
	{
	  l = (BoxedRangeListener) (list[i]);
	  addBoxedRangeListener(l);
	}
    }

  //accessors for outer boundary
  public double getXMax(){ return xmax;}
  public double getXMin(){ return xmin;}
  public double getYMax(){ return ymax;}
  public double getYMin(){ return ymin;}
  //accessors for visible region
  public double getXValue(){ return xvalue;}
  public double getXExtent(){ return xextent;}
  public double getYValue(){ return yvalue;}
  public double getYExtent(){ return yextent;}

  //sets outer boundary
  public void setXMin(double mn)
    {
      xmin = mn;
      //ExpCoordinator.print("BoxedRangeDefault::setXMin " + xmin);
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.XMIN_CHANGED));
    }
  public void setXMax(double mx)
    {
      xmax = mx;
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.XMAX_CHANGED));
    }
  public void setYMin(double mn)
    {
      ymin = mn;
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.YMIN_CHANGED));
    }
  public void setYMax(double mx)
    {
      //ExpCoordinator.print("setYMax " + String.valueOf(mx));
      ymax = mx;
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.YMAX_CHANGED));
    }
  public void setXBoundary(double mx, double mn)
    {
      xmax = mx;
      xmin = mn;
      //if (xmin < 0) ExpCoordinator.print("BoxedRangeDefault::setXBoundary " + xmin);
      fireEvent(new BoxedRangeEvent(this, (BoxedRangeEvent.XMAX_CHANGED | BoxedRangeEvent.XMIN_CHANGED)));
    }
  public void setYBoundary(double mx, double mn)
    {
      ymax = mx;
      ymin = mn;
      fireEvent(new BoxedRangeEvent(this, (BoxedRangeEvent.YMAX_CHANGED | BoxedRangeEvent.YMIN_CHANGED)));
    }
  public void setBoundary(double x_mx, double x_mn, double y_mx, double y_mn)
    {
      xmax = x_mx;
      xmin = x_mn;
      ymax = y_mx;
      ymin = y_mn;
      if (xmin < 0) ExpCoordinator.print(new String("BoxedRangeDefault::setBoundary " + xmin), 4);
      fireEvent(new BoxedRangeEvent(this, (BoxedRangeEvent.XMAX_CHANGED | 
					   BoxedRangeEvent.XMIN_CHANGED | 
					   BoxedRangeEvent.YMAX_CHANGED | 
					   BoxedRangeEvent.YMIN_CHANGED)));
    }
  public void print(int d)
    {
      ExpCoordinator.print("BoxedRangeDefault", d);
      ExpCoordinator.print(new String("xmax = " + xmax + " xmin = " + xmin + " xextent = " + xextent + " xval = " + xvalue), d);
      ExpCoordinator.print(new String("ymax = " + ymax + " ymin = " + ymin + " yextent = " + yextent + " yval = " + yvalue), d);
    }

  //sets inner visible boundary
  public void setXVisible(double val, double xtnt)
    {
      xvalue = val;
      xextent = xtnt;
      fireEvent(new BoxedRangeEvent(this, (BoxedRangeEvent.XVALUE_CHANGED | BoxedRangeEvent.XEXTENT_CHANGED)));
    }
  public void setYVisible(double val, double xtnt)
    {
      yvalue = val;
      yextent = xtnt;
      fireEvent(new BoxedRangeEvent(this, (BoxedRangeEvent.YVALUE_CHANGED | BoxedRangeEvent.YEXTENT_CHANGED)));
    }
  public void setXValue(double val)
    {
      xvalue = val;
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.XVALUE_CHANGED));
    }
  public void setXExtent(double val)
    {
      xextent = val;
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.XEXTENT_CHANGED));
    }
  public void setYValue(double val)
    {
      yvalue = val;
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.YVALUE_CHANGED));
    }
  public void setYExtent(double val)
    {
      yextent = val;
      fireEvent(new BoxedRangeEvent(this, BoxedRangeEvent.YEXTENT_CHANGED));
    }
  public void setVisible(double x_val, double x_xtnt, double y_val, double y_xtnt)
    {
      xvalue = x_val;
      xextent = x_xtnt;
      yvalue = y_val;
      yextent = y_xtnt;
      fireEvent(new BoxedRangeEvent(this, (BoxedRangeEvent.XVALUE_CHANGED | 
					   BoxedRangeEvent.XEXTENT_CHANGED | 
					   BoxedRangeEvent.YVALUE_CHANGED | 
					   BoxedRangeEvent.YEXTENT_CHANGED)));
    }

  //support for listeners
  public void addBoxedRangeListener(BoxedRangeListener l)
    { listeners.add(BoxedRangeListener.class, l);}
  public void removeBoxedRangeListener(BoxedRangeListener l)
    { listeners.remove(BoxedRangeListener.class, l);}
  public EventListenerList getListeners()
    { return listeners;}
  public void fireEvent(BoxedRangeEvent e)
    {
      if (hold)
	{
	  ++changed;
	  return;
	}
      //if (xmin < 0) ExpCoordinator.print("BoxedRangeDefault::fireEvent " + xmin);
      int max = listeners.getListenerCount();
      int i = 0;
      Object[] list = listeners.getListenerList();
      BoxedRangeListener l;
      int change = e.getType();
      //turns out this is a list of class, listener pairs so list looks like class1, listener1, class2, listener2 ... classn, listenern
      for (i = 1; i < list.length; i += 2)
	{
	  l = (BoxedRangeListener) (list[i]);
	  //Visible changed
	  if (((change & BoxedRangeEvent.XVALUE_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.XEXTENT_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.YVALUE_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.YEXTENT_CHANGED) != 0)
	       ) l.changeVisible(e);
	  //the x dimension of the visible changed
	  if (((change & BoxedRangeEvent.XVALUE_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.XEXTENT_CHANGED) != 0)
	       ) l.changeXVisible(e);
	  //the y dimension of the visible changed
	  if (((change & BoxedRangeEvent.YVALUE_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.YEXTENT_CHANGED) != 0)
	       ) l.changeYVisible(e);
	  //Outer boundaries changed
	  if (((change & BoxedRangeEvent.XMAX_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.XMIN_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.YMAX_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.YMIN_CHANGED) != 0)
	       ) l.changeBoundaries(e);
	  //the x dimension of the outer boundary changed
	  if (((change & BoxedRangeEvent.XMAX_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.XMIN_CHANGED) != 0) 
	        ) l.changeXBounds(e);
	  //the y dimension of the outer boundary changed
	  if (((change & BoxedRangeEvent.YMAX_CHANGED) != 0) ||
	      ((change & BoxedRangeEvent.YMIN_CHANGED) != 0)
	       ) l.changeYBounds(e);
	}
    }
}
