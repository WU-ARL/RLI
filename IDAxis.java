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
 * File: IDAxis.java
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
import java.math.*;
import java.util.Vector;

public class IDAxis extends Axis
{
  private double halfSpace; //pixels per unit of measure

  private Vector IDs;
  
  public IDAxis(Graph gr, boolean xory)
    {
      super(gr, xory);
      IDs = new Vector();
    }
  
  public void updateDimensions()
    {
      float osbd =  spaceBetweenDivs;
      double ohs = halfSpace;
      int onod = numberOfDivs;
      updateBounds();
      if (IDs.size() > 0) spaceBetweenDivs = (length())/(IDs.size());
      else spaceBetweenDivs = length();
      halfSpace = spaceBetweenDivs/2;
      numberOfDivs = IDs.size();
      if (ohs != halfSpace ||
	  osbd !=  spaceBetweenDivs ||
	  onod != numberOfDivs)
	redoDivisions();
    }

  public void redoDivisions()
    {
      Division elem;
      for (int i = 0; i < numberOfDivs; ++i)
	{
	  elem = (Division)IDs.elementAt(i);
	  elem.setDimensions(getCoordinate(i));      
	}
    }
  
  public int getCoordinate(double value)
    {
      //here value is an index
      if (isXAxis)
	{
	  return (int)(((value * spaceBetweenDivs) + halfSpace) + left);
	}
      else 
	{
	  return (int)(length() + top - ((value * spaceBetweenDivs) + halfSpace));// because the origin is at the top-left corner
	}
    }

  public double getValue(int coord)
    {
      //here value is an index
      float tmp_coord = 0;
      if (isXAxis) tmp_coord = left + spaceBetweenDivs;
      else tmp_coord = top + length() - spaceBetweenDivs;
      for (int i = 0 ; i < numberOfDivs; ++i)
	{
	  if (isXAxis)
	    {
	      if (tmp_coord >= coord) return i;
	      else
		tmp_coord += spaceBetweenDivs;
	    }
	  else 
	    {
	      if (tmp_coord <= coord) return i;
	      else
		tmp_coord -= spaceBetweenDivs;
	    }   
	}
      return -1;
    }
  
  public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      
      if (numberOfDivs <= 0) return;
      
      //use number of marks to find val
      Division elem;
      int vsize = IDs.size();
      for (int i = 0; i < vsize; ++i)
	{
	  elem = (Division)IDs.elementAt(i);
	  elem.drawDivision(g);
	}
    }

  void addID(String id)
    {
      //System.out.println("  IDAxis::addID " + id);
      IDs.add(IDs.size(), new Division(id, 0));
      updateDimensions();
    }

  void removeID(String id)
    {
      int i;
      for (i = 0; i < IDs.size(); ++i)
	{
	  Division elem = (Division) IDs.elementAt(i);
	  if (elem.compareTo(id) == 0)
	    {
	      IDs.remove(i);
	      break;
	    }
	}
      updateDimensions();
    }
}
