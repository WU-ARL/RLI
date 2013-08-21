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
 * File: TimeDisplay.java
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
/**simple component that increments a counter and displays the current value. just simulates a clock tic.
 */

import javax.swing.*;
import java.awt.*;
import java.math.*;
import java.util.Vector;

public class TimeDisplay extends JPanel implements BoxedRangeListener
{
  private double secs = 0;
  private double usecs = 0;
  private double ticSecs = 1;
  private double ticUSecs = 0;
  protected Vector monitorables;
  private boolean started = false;
  
  public TimeDisplay()
    {
      super();
      setMinimumSize(new Dimension(50,15));
      monitorables = new Vector();
    }
 
  public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      Insets insets = getInsets();
       BigDecimal unscaled = new BigDecimal(secs);
       BigDecimal scaled = unscaled.setScale(1, BigDecimal.ROUND_UP);
      //g.drawString((String.valueOf(secs) + "s " + String.valueOf(usecs) + "us" ), insets.left, insets.top + 15);
      g.drawString((scaled.toString() + "s"), insets.left, insets.top + 15);
    }

 
  public void changeVisible(BoxedRangeEvent e){}
  public void changeBoundaries(BoxedRangeEvent e){}
  public void changeXVisible(BoxedRangeEvent e)    
    {
    
    }
  public void changeYVisible(BoxedRangeEvent e){}
  public void changeXBounds(BoxedRangeEvent e)
    {
      BoxedRangeModel tmp = (BoxedRangeModel) e.getSource();
      //System.out.println("Graph::changeXBounds");
      if ((e.getType() & BoxedRangeEvent.XMAX_CHANGED) != 0)
	{
	  if (tmp.getXMax() == (tmp.getXValue() + tmp.getXExtent()))
	    secs = tmp.getXMax();
	}
    }
  public void changeYBounds(BoxedRangeEvent e){}
}
