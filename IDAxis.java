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
