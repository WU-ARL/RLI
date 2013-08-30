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
 * File: Constant.java
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
import java.io.File;


public class Constant extends NumberData implements BoxedRangeListener //will listen on graph's visibility range
{
  private double k = 0;
  private BoxedRangeModel boxedRange = null;

  public Constant(double v) { this(v, null);}
  public Constant(double v, BoxedRangeModel g)
    {
      this((new MonitorDataType.Constant(v)), g);
    }

  public Constant(MonitorDataType.Constant mdt)
    {
      super(mdt, 4, NumberData.EXPAND);
      k = mdt.getConstant();
    }
  public Constant(MonitorDataType.Constant mdt, BoxedRangeModel g)
    {
      super(mdt, 4, NumberData.EXPAND);
      k = mdt.getConstant();
      boxedRange = g;
      boxedRange.addBoxedRangeListener(this);
      addElement(new NumberPair(boxedRange.getXMin(), k));
      addElement(new NumberPair(boxedRange.getXValue(), k)) ;
      addElement(new NumberPair((boxedRange.getXValue() + boxedRange.getXExtent()), k));
      addElement(new NumberPair(boxedRange.getXMax(), k));
    }
  public java.io.File getLogFile(){ return null;}
  public void stopLogging(){}
  public void setLogFile(java.io.File f) {}
  public void setData(double val, double timeInt){} 
  public void setDataExact(double val, double timev){}//exact data value and exact time value 
  public void stop()
    {
      boxedRange.removeBoxedRangeListener(this);
    }

  public void setBoxedRange( BoxedRangeModel g)
    {
      boxedRange = g;
      boxedRange.addBoxedRangeListener(this);
      addElement(new NumberPair(boxedRange.getXMin(), k));
      addElement(new NumberPair(boxedRange.getXValue(), k)) ;
      addElement(new NumberPair((boxedRange.getXValue() + boxedRange.getXExtent()), k));
      addElement(new NumberPair(boxedRange.getXMax(), k));
    }

  //interface BoxedRangeListener
  public void changeVisible(BoxedRangeEvent e){}
  public void changeBoundaries(BoxedRangeEvent e){}
  public void changeXVisible(BoxedRangeEvent e)
    { 
      getElementAt(1).setX(boxedRange.getXValue());
      getElementAt(2).setX(boxedRange.getXValue() + boxedRange.getXExtent());
    }
  public void changeYVisible(BoxedRangeEvent e){}
  public void changeXBounds(BoxedRangeEvent e)    
    {
      getElementAt(0).setX(boxedRange.getXMin());
      getElementAt(3).setX(boxedRange.getXMax());
    }
  public void changeYBounds(BoxedRangeEvent e){}
  
  //end interface BoxedRangeListener
  
} 
