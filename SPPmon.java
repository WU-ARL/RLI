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
 * File: RLIbeta.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 7/20/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.io.*;
import javax.swing.*;
import java.lang.String;
import java.lang.reflect.Array;


public class SPPmon
{  
  public static void main(String as[])
  {
    int len = Array.getLength(as);
    String args[] = new String[len + 2];
    
    for (int i = 0; i < len; ++i)
      {
        args[i] = as[i];
      }
    args[len] = "-management";
    args[len+1] = "-sppmon";
    ExpCoordinator ec = new ExpCoordinator(args);
  }
}
