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
 * File: MonitorPlus.java
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

public interface MonitorPlus extends BoxedRangeModel, Monitor
{
	public java.io.File getLogFile();
	public void pause();
	public void stopLogging();
	public void restart();
	public void resetTime();
	public boolean isLogging();
	public NumberPair getElementAt(int index);
	public NumberPair getLastElement();
	public int getSize();
	public void recordData(ONL.Writer writer) throws java.io.IOException;
	public void readRecordedData(ONL.Reader reader) throws java.io.IOException;
} 
