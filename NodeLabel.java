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
