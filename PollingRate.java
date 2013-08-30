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
