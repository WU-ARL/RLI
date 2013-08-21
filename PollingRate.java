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
 * File: PollingRate.java
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
import org.xml.sax.*;
import javax.xml.stream.*;

public class PollingRate
{
  private double secs = 1;
  private double usecs = 0;
  private double onlySecs = 1;
  //these are the rates corrected for the scheduler
  private double cSecs = 1;
  private double cUsecs = 0;
  private double cOnlySecs = 1;

  public PollingRate()
    {
      setRate(1, 0, true);
    }

  public PollingRate(String ln)
    {
      parseRate(ln);
    }

  public PollingRate(PollingRate p) { setRate(p.getSecs(), p.getUSecs(), true);}

  public PollingRate(double s, double us)
    {
      setRate(s,us, true);
    }
  public void parseRate(String ln)
    {
      String[] starray = ln.split(" ");
      double s = Double.parseDouble(starray[0]);
      double us = Double.parseDouble(starray[1]);
      setRate(s,us, true);
    }
  public void setRate(Attributes attributes) 
    { 
      setRate(Double.parseDouble(attributes.getValue(ExperimentXML.SECS)), Double.parseDouble(attributes.getValue(ExperimentXML.USECS)), false);
      print(ExperimentXML.TEST_XML);
    }
  public void setRate(double s, double us) { setRate(s, us, false);}
  public void setRate(double s, double us, boolean noprint)
    {
      //System.out.println("PollingRate::setRate s = " + s + " us = " + us);
      secs = Math.floor(s);
      usecs = us + ((s-secs)*1000000);
      onlySecs = secs + (usecs/1000000);
      //first correct polling rate
      cSecs = secs;
      cUsecs = usecs;
      if (secs > 0 && secs < 11) //this does a scheduling correction
	{
	  if (secs > 1) cSecs -= 1;
	  else if (usecs < 100000) //s == 1
	    {
	      cSecs = 0;
	      cUsecs += 900000;
	    }
	}
      cOnlySecs = cSecs + (cUsecs/1000000);
      if (!noprint) print(10);
    }

  public double getSecs() { return secs;}
  public double getUSecs() { return usecs;}
  public double getSecsOnly() { return onlySecs;}
  public double getCSecs() { return cSecs;}
  public double getCUSecs() { return cUsecs;}
  public double getCSecsOnly() { return cOnlySecs;}
  public String toString() { return (new String(secs + " " + usecs));}
  public boolean isLessThan(PollingRate pr) 
    {
      return (onlySecs < pr.getSecsOnly());
    }
  public boolean isEqual(PollingRate pr)
    {
      if (pr != null)
        return (onlySecs == pr.getSecsOnly());
      else return false;
    }
  public void print() { print(0);}
  public void print(int level)
    {
      ExpCoordinator.printer.print(new String("PollingRate (secs,usecs,cSecs,cUsecs,cOnlySecs) = ( " + secs + ", " + usecs + ", " + cSecs + ", " + cUsecs + ", " + cOnlySecs + ")"), level);
    }
  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
  {
    xmlWrtr.writeAttribute(ExperimentXML.SECS, String.valueOf(secs));
    xmlWrtr.writeAttribute(ExperimentXML.USECS, String.valueOf(usecs));
  }
}
