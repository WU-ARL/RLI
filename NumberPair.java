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
 * File: NumberPair.java
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
import java.lang.String;

public class NumberPair implements DataPair
{
  protected double value1;
  protected double value2;
  
  public NumberPair(){ }
  
  public NumberPair(double v1, double v2)
    {
      value1 = v1;
      value2 = v2;
    }
  
  public double getValue() { return value2;}
  public String getID() { return String.valueOf((int)value1);}
  public double getX() { return value1;}
  public double getY() { return value2;}
  public void setX(double v1) { value1 = v1;} 
  public void setY(double v2) { value2 = v2;} 
  
  public void printOut()
    {
      System.out.println("(" + String.valueOf(value1) + "," + String.valueOf(value2) + ")");
    }
}
