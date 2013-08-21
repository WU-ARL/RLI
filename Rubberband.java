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
 * File: Rubberband.java
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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Rubberband
{
  public static final String DRAWING_STATE = "drawing_state";
  public static final String ENABLED = "enabled";
  public static final String DISAPPEAR = "disappear"; //whether or not to show the final box after the mouse is released
  //drawing states
  protected static final int NOTACTIVE = 0;
  protected static final int DRAWING = 1;
  protected static final int DRAWN = 2;

  protected Point anchorPt = new Point(0,0);
  protected Point stretchedPt = new Point(0,0);
  protected Point lastPt = new Point(0,0);
  protected Point endPt = new Point(0,0);

  private JComponent component;
  //private boolean firstStretch = true;
  //private boolean active = false;
  //private int drawingState = NOTACTIVE;

  protected ONLPropertyList properties = null;


  public Rubberband() 
    { 
      properties = new ONLPropertyList(this);
      properties.setProperty(DRAWING_STATE, NOTACTIVE);
      properties.setProperty(ENABLED, false);
      properties.setProperty(DISAPPEAR, false);
    }
  
  public Rubberband(JComponent c) 
    {
      this();
      setComponent(c);
    }

  public void setEnabled(boolean b) {
      properties.setProperty(ENABLED, b);
  }

  public void setComponent(JComponent c) {
      component = c;
      
      component.addMouseListener( new MouseAdapter() {
	public void mousePressed(MouseEvent event){
	  if (isEnabled())// && !isDrawn())
	    anchor(event.getPoint());
	}
	
	public void mouseReleased(MouseEvent event) {
	  if (isEnabled()) {
	    //if (!isDrawn()) 
            select(event.getPoint());
            end(event.getPoint());
            //else 
            //{
            //if (inBounds(event.getPoint()))
            //select(event.getPoint());
            //end(getEnd());
            //}
	  }
	}
      });

      component.addMouseMotionListener( new MouseMotionAdapter(){
	public void mouseDragged(MouseEvent event) {
	  if (isEnabled())//&& !isDrawn())
	    stretch(event.getPoint());
	}
      });
  }

  public void setDrawingState(int s) { properties.setProperty(DRAWING_STATE, s);}
  public boolean isEnabled() { return (properties.getPropertyBool(ENABLED));}
  public boolean isDrawn() { return (getDrawingState() == DRAWN);} 
  public boolean isDrawing() { return (getDrawingState() == DRAWING);} 
  public boolean isDisplayed() 
    { 
      int drawingState = getDrawingState();
      return (drawingState == DRAWN || drawingState == DRAWING);
    }
  public int getDrawingState() { return (properties.getPropertyInt(DRAWING_STATE));}
  public Point getAnchor() { return anchorPt;}
  public Point getStretched() { return stretchedPt;}
  public Point getLast() { return lastPt;}
  public Point getEnd() { return endPt;}

  public void anchor(Point p) {
      //firstStretch = true;
    setDrawingState(DRAWING);
    anchorPt.x = p.x;
    anchorPt.y = p.y;
    
    stretchedPt.x = lastPt.x = anchorPt.x;
    stretchedPt.y = lastPt.y = anchorPt.y;
  }

  public void stretch(Point p) {
    lastPt.x = stretchedPt.x;
    lastPt.y = stretchedPt.y;
    stretchedPt.x = p.x;
    stretchedPt.y = p.y;
    //if (firstStretch == true) firstStretch = false;
    component.revalidate();
    component.repaint();
  }
  

  public void end(Point p) {
    lastPt.x = endPt.x = p.x;
    lastPt.y = endPt.y = p.y;
    
    //if (isDrawn() || 
    //if (properties.getPropertyBool(DISAPPEAR)) 
    //{
    setDrawingState(NOTACTIVE);
    component.revalidate();
    component.repaint();
    //}
    //else setDrawingState(DRAWN);  
  }
  

  protected void select(Point p) {
  //override this if you want to have some other behavior for when a user clicks in the rubberband object - this only works if DISAPPEAR is false
  }


  public Rectangle getBounds() {
    return new Rectangle(stretchedPt.x < anchorPt.x ? stretchedPt.x : anchorPt.x,
			 stretchedPt.y < anchorPt.y ? stretchedPt.y : anchorPt.y,
			 Math.abs(stretchedPt.x - anchorPt.x),
			 Math.abs(stretchedPt.y - anchorPt.y));
  }

  public Rectangle lastBounds() {
    return new Rectangle(lastPt.x < anchorPt.x ? lastPt.x : anchorPt.x,
			 lastPt.y < anchorPt.y ? lastPt.y : anchorPt.y,
			 Math.abs(lastPt.x - anchorPt.x),
			 Math.abs(lastPt.y - anchorPt.y));
  }


  public boolean inBounds(Point p)
  {
    Rectangle tmp_rect = getBounds();
    //System.out.println("Rubberband.inBounds (" + tmp_rect.getX() + ", " + tmp_rect.getY() + ", " + tmp_rect.getWidth() + ", " + tmp_rect.getHeight() + ") for point (" + p.getX() + ", " + p.getY() + ") " + (tmp_rect.contains(p)));

    return (tmp_rect.contains(p));
  }
  
  public void drawLast(Graphics g)
    {
      Rectangle rect = lastBounds();
      //System.out.println("Zoom::drawLast (" + rect.x + ", " + rect.y + ", " + rect.width + ", " + rect.height + ")");
      g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

  public void draw(Graphics g)
    {
      if (isDisplayed())
	{
	  Color oldColor = g.getColor();
	  g.setColor(Color.black);
	  drawNext(g);
	  g.setColor(oldColor);
	}
    }
  
  public void drawNext(Graphics g)
    {
      Rectangle rect = getBounds();
      //System.out.println("Zoom::drawNext (" + rect.x + ", " + rect.y + ", " + rect.width + ", " + rect.height + ")");
      g.drawRect(rect.x, rect.y, rect.width, rect.height); 
    }
  
}
				      
	
