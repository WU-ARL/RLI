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
 * File: MenuDragMouseAdapter.java
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
import javax.swing.event.*;
import java.awt.event.*;

public class MenuDragMouseAdapter implements MenuDragMouseListener, MouseListener
    {
      public MenuDragMouseAdapter(){}

      //MenuDragMouseListener Interface
      public void menuDragMouseDragged(MenuDragMouseEvent e){}
      public void menuDragMouseEntered(MenuDragMouseEvent e){}
      public void menuDragMouseExited(MenuDragMouseEvent e){}
      public void menuDragMouseReleased(MenuDragMouseEvent e){select();}
      //end MenuDragMouseListener Interface
      
      //MouseListener Interface
      public void mouseClicked(MouseEvent e){}
      public void mouseEntered(MouseEvent e){}
      public void mouseExited(MouseEvent e){}
      public void mousePressed(MouseEvent e){}
      public void mouseReleased(MouseEvent e){select();}
      //end MouseListener Interface

      protected void select() {}
    }
