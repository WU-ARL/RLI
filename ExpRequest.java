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
