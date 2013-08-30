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
 * File: AveragingNData.java
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
public class AveragingNData extends NumberData
{
	private double dataSum = 0;
	private double timeSum = 0;
	private double avgWindow = 0;
	private boolean runningAvg = true;
	public AveragingNData(MonitorDataType.Base dt, int style)
	{ 
		super(dt, style);
	}
	public AveragingNData(MonitorDataType.Base dt, double xmn, double xmx, double ymn, double ymx, int style)
	{ 
		super(dt, xmn, xmx, ymn, ymx, style);
	}

	public void setAvgWindow(double s) 
	{
		runningAvg = false; 
		avgWindow = s;
	}

	public void setRunningAvg(boolean b) { runningAvg = b;}

	public void setData(double data, double timeInterval)
	{
		if (!stopped && !paused)
		{
			if (receivedFirst)
			{
				dataSum += data;
				timeSum += timeInterval;
				if (runningAvg)
					super.setData((dataSum/timeSum), timeInterval);
				else
				{
					if (timeSum >= avgWindow)
					{
						super.setData((dataSum/timeSum), timeSum);
						timeSum = 0;
						dataSum = 0;
					}
				}
			}
			else receivedFirst = true;//don't add the first one since the calculation will be off
		}
	}

}
