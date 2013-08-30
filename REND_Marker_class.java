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
 * File: REND_Marker_class.java
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
import java.io.*;
import java.util.Arrays;

public class REND_Marker_class //28 bytes but we'll only use the last 4 bytes for now
{
  protected int field1 = 0; //2 bytes
  protected int field2 = 0; //2 bytes
  protected byte[] field3; //4 bytes
  protected byte[] field4; //4 bytes
  protected int op = -1; //4 bytes same as the GBNSC op code
  protected  int ipp = 1; //1 if ipp or 0 if opp
  protected int port = -1; //4 bytes port id or -1 if switch wide or something else
  protected int index = -1; // mr field, vpi, or vci depending on op

  public REND_Marker_class()
    {
      field3 = new byte[4];
      field4 = new byte[4];
      for (int i = 0; i < 4; ++i)
	{
	  field3[i] = 0;
	  field4[i] = 0;
	}
    }

  public REND_Marker_class(int i)
    {
      this();
      index = i;
    }

  public REND_Marker_class(int p, boolean input, int tp, int i)
    {
      this(i);
      port = p;
      if (input) ipp = 1;
      else ipp = 0;
      op = tp;
    }

  public REND_Marker_class(int p, boolean input, int tp)
    {
      this(p, input, tp, -1);
    }

  public REND_Marker_class(int tp, int i)
    {
      this(i);
      op = tp;
    }

  public REND_Marker_class(REND_Marker_class mrkr)
  {
    this();
    field1 = mrkr.field1;
    field2 = mrkr.field2;
    op = mrkr.op;
    ipp = mrkr.ipp;
    port = mrkr.port;
    index = mrkr.index;
    
    for (int i = 0; i < 4; ++i)
      {
        field3[i] = mrkr.field3[i];
        field4[i] = mrkr.field4[i];
      }
  }
  public void setIndex(int i) { index = i;}
  public int getIndex() { return index;}
  public void setPort(int p) { port = p;}
  public int getPort() { return port;}
  public boolean isIPP() { return (ipp > 0);}
  public boolean isOPP() { return (ipp < 1);}
  public boolean isNodeWide() { return (port < 0);}
  public void setOp(int t) { op = t;}
  public int getOp() { return op;}

  public void setField1(short v) { field1 = v;}
  public void setField2(short v) { field2 = v;}

  public void setField3(int v)
  {
    field3[0] = (byte)(0x000000ff & v);
    field3[1] = (byte)((0x0000ff00 & v) >> 8);
    field3[2] = (byte)((0x00ff0000 & v) >> 16);
    field3[3] = (byte)((0xff000000 & v) >> 24);
  }

  public void setField4(int v)
  {
    field4[0] = (byte)(0x000000ff & v);
    field4[1] = (byte)((0x0000ff00 & v) >> 8);
    field4[2] = (byte)((0x00ff0000 & v) >> 16);
    field4[3] = (byte)((0xff000000 & v) >> 24);
  }

  public void retrieve(DataInput din) throws IOException
    {
      field1 = din.readUnsignedShort(); //read 2 bytes
      field2 = din.readUnsignedShort(); //read 2 bytes
      din.readFully(field3); //read 4 bytes
      din.readFully(field4); //read 4 bytes
      ipp = din.readInt(); //read 4 bytes
      op = din.readInt(); //read 4 bytes
      port = din.readInt(); //read 4 bytes
      index = din.readInt(); //read index from last 4 bytes
    }

  public void store(DataOutput dout) throws IOException
    {
      dout.writeShort(field1); //write 2 bytes
      dout.writeShort(field2); //write 2 bytes
      dout.write(field3); //write 4 bytes
      dout.write(field4); //write 4 bytes
      dout.writeInt(ipp); //write 4 bytes
      dout.writeInt(op); //write 4 bytes
      dout.writeInt(port); //write 4 bytes
      dout.writeInt(index); //write index into last 4 bytes
    }

  public void print(int level)
    {
      ExpCoordinator.print(new String("REND_Marker_Class " + toString()), level);
      //ExpCoordinator.printer.print(new String("field1 = " + field1), level);
      //ExpCoordinator.printer.print(new String("field2 = " + field2), level);
      //ExpCoordinator.printer.print(new String("field3 = " + field3), level);
      //ExpCoordinator.printer.print(new String("field4 = " + field4), level);
      //ExpCoordinator.printer.print(new String("ipp = " + ipp), level);
      //ExpCoordinator.printer.print(new String("op = " + op), level);
      //ExpCoordinator.printer.print(new String("port = " + port), level);
      //ExpCoordinator.printer.print(new String("index = " + index), level);
    }


  public String toString()
  {
    String rtn = new String("(" + field1 + ", " + field2 + ", " + field3 + ", " + field4 + ", " + ipp + ", " + op + ", " + port + ", " + index + ")");
    return rtn;
  }

  public void assign(REND_Marker_class mrkr)
    {
      field1 = mrkr.field1;
      field2 = mrkr.field2;
      field3 = mrkr.field3;
      field4 = mrkr.field4;
      ipp = mrkr.ipp;
      op = mrkr.op;
      port = mrkr.port;
      index = mrkr.index;
    }

  public boolean isEqual(REND_Marker_class mrkr)
    {
      return ( field1 == mrkr.field1 &&
	       field2 == mrkr.field2 &&
	       Arrays.equals(field3, mrkr.field3) &&
	       Arrays.equals(field4, mrkr.field4) &&
	       ipp == mrkr.ipp &&
	       op == mrkr.op &&
	       port == mrkr.port &&
	       index == mrkr.index);
    }
}
