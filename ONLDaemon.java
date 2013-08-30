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
 * File: ONLDaemon.java
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

public abstract class ONLDaemon extends NCCPConnection
{
  public static final int UNKNOWN = 0;
  public static final int WUGSC = 1; //GBNSC
  public static final int HWC = 2; //HWC the hardware daemon
  public static final int NSPC = HWC; //MSRC
  public static final int NPC = HWC; //NPC
  public static final int HOSTC = 3; //ENDNODE Monitoring this is a vague daemon, used for tcp monitoring in the past
  public static final int ONLCD = 4; //Experiment coordinator
  public static final int OBSERVE = 7; //daemon for peer to peer communication

  public static final int HOSTC_PORT = 3553; //shouldn't be used in GUI this is also the management daemon may need a separate one eventually
  public static final int COORDINATOR_PORT = 3560; //3555; //main coordinator that spawns individual coordinator
  public static final int WUGSC_PORT = 3551; //GBNSC
  public static final int NSPC_PORT = 3552; //MSRC
  public static final int NPC_PORT = 3552; //MSRC
  public static final int WUGSC_SUFFIX = 551; //GBNSC
  public static final int NSPC_SUFFIX = 552; //MSRC
  public static final int HOSTC_SUFFIX = 553;
  public static final int NPC_SUFFIX = 552; //NPC

  protected int type = WUGSC; 

  public ONLDaemon(String h, int p, int tp)
    {
      super(h, p, getTypeString(tp));
      type = tp;
      //label = getTypeString(type);
    }

  public static int getType(String str)
    {
      if (str.compareTo("wugsc") == 0)  return WUGSC;
      if (str.compareTo("nspc") == 0)  return NSPC;  
      if (str.compareTo("hostc") == 0) return HOSTC;
      if (str.compareTo("onlcd") == 0) return ONLCD; 
      return -1;
    }

  public static String getTypeString(int tp)
    {  
      String rtn;
      switch (tp)
	{
	case WUGSC:
	  rtn = "wugsc";
	  break;
          //case NSPC:
	  //rtn = "nspc";
	  //break;
	case HOSTC:
	  rtn = "enodem";
	  break;
	case ONLCD:
	  rtn = "onlcd";
	  break;
        case HWC:
          rtn = "hwc";
          break;
	default:
	  rtn = "unknown";
	}
      return rtn;
    }
  public boolean isType(int tp) { return (type == tp);}
  public int getDaemonType() { return type;}
  public String toString()
    {
      return (new String(label + " " + super.toString()));
    }
  public void write(ONL.Writer wrtr) throws java.io.IOException
    {
      wrtr.writeInt(type);
      super.write(wrtr);
    }
  public void read(ONL.Reader rdr) throws java.io.IOException
    {
      type = rdr.readInt();
      super.read(rdr);
    }
}

