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
 * File: PopupList.java
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
import javax.swing.*;
import java.awt.*;

public class PopupList extends JPopupMenu
{
  public PopupList() 
    {
      super();
    }
  public PopupList(String lbl)
    {
      super();
      JLabel mi = new JLabel(lbl);
      //mi.setEnabled(false);
      //mi.setFocusPainted(false);
      mi.setFont(new Font("Dialog", Font.BOLD, 10));
      mi.setForeground(Color.black);
      add(mi);
      addSeparator();
    }

  public void addItem(String txt)
    {
      JLabel mi = new JLabel(txt);
      //mi.setEnabled(false);
      //mi.setFocusPainted(false);
      mi.setFont(new Font("Dialog", Font.PLAIN, 10));
      mi.setForeground(Color.black);
      add(mi);
    }
}
