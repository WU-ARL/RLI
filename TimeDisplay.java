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
