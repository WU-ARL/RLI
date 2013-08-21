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
 * File: NodeLabel.java
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
import java.lang.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class NodeLabel
{
  protected String host = "";
  protected int port = 0;
  protected String label = "";

  public NodeLabel(){}
  public NodeLabel(NodeLabel nl)
    {
      this((new String(nl.host)), nl.port, (new String(nl.label)));
    }
  public NodeLabel(String h, int p) 
    {
      this(h,p, (new String(h + p)));
      host = h;
      port = p;
      String str_array[] = host.split(" ");
      //StringTokenizer st = new StringTokenizer(host, ".");
      //System.out.println("NodeLabel::NodeLabel for " + host);
      //if (st.hasMoreTokens()) 
      //	{
      //  label = st.nextToken(); 
	  //System.out.println("   label is " + label);
      //	}
      label = str_array[0];
    }
  public NodeLabel(String h, int p, String  lbl) 
    {
      host = h;
      port = p;
      label = lbl;
    } 
  public String getHost() { return (new String(host));}
  public int getPort() { return port;}
  public String getLabel() { return label;}

  public void setHost(String h) { host = h;}
  public void setPort(int p) { port = p;}
  public void setLabel(String lbl) { label = lbl;}
  public String toString(){ return (new String(label + " " + host + " " + port));}
  public void fromString(String str)
    {
      String strarray[] = str.split(" ");
      label = strarray[0];
      host = strarray[1];
      port = Integer.parseInt(strarray[2]);
    }

  public byte[] getRAddr()
     {
       try
         {
           InetAddress addr = InetAddress.getByName(host);
           return addr.getAddress();
         }
       catch (UnknownHostException e)
         {
           System.err.println("NodeDescriptor.ID::getRAddr Don't know about host: " + host);
           return null;
         }
     }

  public boolean isEqual(NodeLabel nid)
    {
      try
	{
	  InetAddress r1addr = InetAddress.getByName(host);
	  InetAddress r2addr = InetAddress.getByName(nid.getHost());
	  return ((r1addr.getHostAddress().compareTo(r2addr.getHostAddress()) == 0) &&
		  port == nid.getPort());
	}
      catch (UnknownHostException e) 
	{
	  System.err.println("NodeLabel::isEqual Don't know about host: ");
	  return false;
	}
    }
}  
