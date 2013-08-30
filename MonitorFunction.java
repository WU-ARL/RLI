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
