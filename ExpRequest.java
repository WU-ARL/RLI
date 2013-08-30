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
 * File: ExpRequest.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 12/02/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.io.*;

//generic NCCP message consisting of a request/response pair and a rendezvous marker so we can get the response back
//used to communicate with ONLCD
public abstract class ExpRequest implements NCCP.Message
{
  protected REND_Marker_class marker;
  protected RequesterBase request = null; //forms the nccp request for this op
  protected NCCP.ResponseBase response = null; //parses the nccp response for this op
  protected boolean requestSent = false;
  protected boolean inClear = false; //used to let us know if we're in the process of stopping monitors
  public static abstract class RequesterBase extends NCCP.RequesterBase
  {
    public RequesterBase(int op) { super(op, true);}
    public RequesterBase(int op, boolean b) { super(op, b);}
    public ONLComponent getONLComponent() { return null;}
  }//end inner class NCCP.RequesterBase

  public ExpRequest(){}
  
  public REND_Marker_class getMarker() { return marker;}
  
  public REND_Marker_class getReturnMarker()
    {
      if (response != null && response.isPeriodic())
        {
          return (response.getStopMarker());
        }
      else return null;
    }
  
  public void setMarker(REND_Marker_class m)
    {
      marker = m;
      if (request != null) request.setMarker(marker);
    }
  
  public void clear()
    {
      //System.out.println("ME::clear");
      //inClear = true;
      inClear = false;
    }
  public void store(DataOutputStream dout) throws IOException { storeRequest(dout);}
  public void storeRequest(DataOutputStream dout) throws IOException
    {
      //System.out.println("ME::storeRequest");
      if (request != null) request.store(dout);
      else System.err.println("ExpRequest.storeRequest request is null");
    }

  public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException
    {
      if (response != null)
        {
          ExpCoordinator.print(new String("ExpRequest::retrieveData " + getMarker().toString() + " response " + response.getClass()), 7);
          response.setHeader(hdr); //set header info
          response.retrieveData(din); //get op specific return data
          processResponse(response);
        }
    } 

  public boolean isPeriodic()
    {
      if (request != null & request.isPeriodic())
        return true;
      else
        return false;
    }
  public boolean isRequestSent(){ return requestSent;}
  public int getType()
    {
      if (request != null) return (request.getOp());
      else return 0;
    }
  public void setRequestSent(boolean r) 
    {
      //System.out.println("ExpRequest::setRequestSent to " + r + " marker is ");
      //marker.print();
      requestSent = r;
    }

  public abstract void processResponse(NCCP.ResponseBase r); //should pass parsed data to monitor
  public void processPeriodicMessage(NCCP.PeriodicMessage msg){} 
    protected ONLComponent getONLComponent() { return (request.getONLComponent());}
}
