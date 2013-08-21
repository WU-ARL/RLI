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

