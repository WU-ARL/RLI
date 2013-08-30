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
 * File: DirectConn.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 2/24/2006
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.io.*;
import javax.swing.*;


public class DirectConn
{
  public static void main(String args[])
    {
      int max = args.length;
      int i = 0;
      boolean proxy = false;
      for (i = 0; i < max; ++i)
	{
	  if (args[i].equals("-proxy")) 
            {
              proxy = true;
              break;
            }
        }
      ExpCoordinator display = null;
      if (!proxy) display = new ExpCoordinator(args, true, 0);
      else display = new ExpCoordinator(args);
    }
}
