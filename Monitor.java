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
 * File: Monitor.java
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
public interface Monitor
  {
      public void setData(double val, double timeInt, boolean error); //not sure if future monitors will take this form of params so this is why it is a separate descendant of Monitor
    public void setDataExact(double val, double timev);//exact data value and exact time value 
    public void stop();
    public void start();
    public boolean isStarted();
    public void addMonitorable(Monitorable m);
    public void removeMonitorable(Monitorable m);
    public MonitorDataType.Base getDataType();
    public void addMonitorFunction(MonitorFunction mf);
    public void removeMonitorFunction(MonitorFunction mf);
    public void setLogFile(java.io.File f);
  }

