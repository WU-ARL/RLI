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
				      
	
