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
 * File: BoxedRangeEvent.java
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

import java.awt.AWTEvent;
public class BoxedRangeEvent extends AWTEvent
{
  public static final int XVALUE_CHANGED = 1;
  public static final int XEXTENT_CHANGED = 2;
  public static final int YVALUE_CHANGED = 4;
  public static final int YEXTENT_CHANGED = 8;
  public static final int XMAX_CHANGED = 16;
  public static final int XMIN_CHANGED = 32;
  public static final int YMAX_CHANGED = 64;
  public static final int YMIN_CHANGED = 128;
  public static final int ANYX_CHANGED = 51;
  public static final int ANYY_CHANGED = 204;
  public static final int ALL_CHANGED = 255;

  private int type;
  public BoxedRangeEvent(BoxedRangeModel source, int eventType)
    {
      super(source, -1);
      type = eventType;
    }

  public int getType()
    {
      return type;
    }
}
