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
 * File: Zoom.java
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

/**class ZoomAdapter descends from MouseInputAdapter. it is in charge of instructing a Zoomable object to
 * draw a box as the user drags the mouse across the component and then it waits until the user clicks inside the box
 * and then passes the zoom coordinates to the component
 */ 

import javax.swing.event.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

public class Zoom extends Rubberband
{
  protected Zoomable zoomable;
  protected boolean zoomed = false;
  protected Vector state;
  protected double cZoomOutVal;
  protected double cZoomOutXtnt;
  
  //graphic label to display when in zoom
  JLabel label;
  
  public static class State
  {
    public double xval_fact; //amount to scale xval by
    public double xextent_fact;//amount to scale xextent by
    public double yval_fact;//amount to scale yval by
    public double yextent_fact;//amount to scale yextent by
    private boolean first = false;
    public State(){}
    public boolean firstZoom() { return first;}
    public void setFirst(boolean v) { first = v;}
  }
   
  public static interface Zoomable
  {
    public void zoomIn(Zoom.State st);
    public void zoomOut(Zoom.State st);
    //public void cancelZoom();
    public Rectangle getDisplayBounds();
  }
  
  public Zoom(Zoomable d)
    {
      super();
      zoomable = d;
      setEnabled(false);
      state = new Vector();
      cZoomOutVal = (double)1/12;
      cZoomOutXtnt = (double)5/6;//this will increase the extent by 20%
    }
  
  public Zoom(Zoomable d,JPanel c)
    {
      this(d);
      setComponent(c);
    }
  
  public boolean isZoomed()
    {
      return (!state.isEmpty());
      //return zoomed;
    }
  protected void select(Point p)
    {
      Rectangle newVis = lastBounds();
      Rectangle oldVis = zoomable.getDisplayBounds();
      Zoom.State tmp = new Zoom.State();
      if (state.size() == 0) 
	{
	  tmp.setFirst(true);
	}
      //zoomed = true;
      tmp.xval_fact = (newVis.x - oldVis.x)/((double)oldVis.width);
      tmp.xextent_fact = newVis.width/((double)oldVis.width);
      tmp.yval_fact = ((oldVis.y + oldVis.height) - (newVis.y + newVis.height))/((double)oldVis.height);
      tmp.yextent_fact = newVis.height/((double)oldVis.height);
      state.add(tmp);
      zoomable.zoomIn(tmp);
      setEnabled(false);
    }

  public void end(Point p) //override RubberBand.end
    {
      if (isDrawn()) 
	setEnabled(false);
      super.end(p);
    }
  
  public void cancelZoom()
    {
      //System.out.println("Zoom::cancelZoom");
      /*
      int max = state.size();
      int i = 0;
      Zoom.State elem;
      Zoom.State tmp = new Zoom.State();
      tmp.xval_fact = 0;
      tmp.xextent_fact = 1;
      tmp.yval_fact = 0;
      tmp.yextent_fact = 1;
      
      //create the combined factor using all past zooms
      for (i = 0; i < max; ++i)
	{
	  elem = (Zoom.State)state.elementAt(i);
	  tmp.xval_fact = tmp.xval_fact + (elem.xval_fact*tmp.xextent_fact);
	  tmp.xextent_fact = tmp.xextent_fact*elem.xextent_fact;
	  tmp.yval_fact = tmp.yval_fact + (elem.yval_fact*tmp.yextent_fact);
	  tmp.yextent_fact = tmp.yextent_fact*elem.yextent_fact;
	}
      zoomable.zoomOut(tmp);
      */
      state.clear();
    }
  
  public void zoomOut()
    {
      Zoom.State tmp;
      if (state.isEmpty()) //not zoomed in so just start increasing by 20%
	{
	  tmp = new State();
	  tmp.xval_fact = cZoomOutVal;
	  tmp.xextent_fact = cZoomOutXtnt;
	  tmp.yval_fact = cZoomOutVal;
	  tmp.yextent_fact = cZoomOutXtnt;
	  //System.out.println("Zoom::zoomOut (" + tmp.xval_fact + ", " + tmp.xextent_fact + ", " + tmp.yval_fact + ", " + tmp.yextent_fact + ")");
	}
      else
	{
	  tmp = (Zoom.State)state.firstElement();
	  state.remove(tmp);
	}
      zoomable.zoomOut(tmp);
    }
}
