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
 * File: MonitorFunction.java
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
//defines interface for objects that take data from existing monitors and either combine it or display it in some way
import java.io.IOException;
import javax.xml.stream.*;
public interface MonitorFunction 
{

  /////////////////////////////////////////////// Param ////////////////////////////////////////////////////////
  public static interface Param
  {
    public MonitorPlus getMonitor();
    public void write(ONL.Writer wrtr) throws IOException;
    public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException;
    public void read(ONL.Reader rdr) throws IOException;
    public void writeChangeable(ONL.Writer wrtr) throws IOException;
    public void readChangeable(ONL.Reader rdr) throws IOException;
    public void record(ONL.Writer wrtr) throws IOException;
    public void loadRecording(ONL.Reader rdr) throws IOException;
  }
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  public void addData(MonitorPlus nd);
  public void removeData(MonitorPlus nd);
  public void addConditional(Conditional c);
  public void removeConditional(Conditional c);
  public Param getParam(MonitorDataType.Base mdt);
  public Param readParam(ONL.Reader rdr) throws IOException;
  public void write(ONL.Writer wrtr) throws IOException;
  public void writeXML(XMLStreamWriter xmlWrtr)  throws XMLStreamException;
  public void read(ONL.Reader rdr)throws IOException;
}
