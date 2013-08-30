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
 * File: ONLTCCollRenderer.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 5/27/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.awt.*;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.*;


public class ONLTCCollRenderer extends JPanel implements TableCellRenderer
{
  private Color uncommittedColor = null;

  private class ElementRenderer extends DefaultTableCellRenderer
  {
    public ElementRenderer() 
    { 
      super();
      setBorder(new EmptyBorder(0,0,0,10));
    }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
    {
      ONLTableCellRenderer.Committable elem = (ONLTableCellRenderer.Committable) value;
      Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
      if (elem != null)
        {
          //if (row_elem.isFailed())//strike through
          if (elem.needsCommit())
            {
              comp.setForeground(uncommittedColor);
            }
          else comp.setForeground(Color.black);
        }
      else System.out.println("ONLTCCollRenderer.ElementRenderer elem null for " + value.toString());
      return comp; //(super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col));
    }
  } 

  public ONLTCCollRenderer()
    {
      this(Color.red);
    }
  public ONLTCCollRenderer(Color c)
    {
      super();
      setVisible(true);
      setSize(50,50);
      uncommittedColor = c;
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    }
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
    {
      removeAll();
      invalidate();
      if (value == null) return this;
      Collection coll = (Collection) value;
      Iterator iter = ((Collection)value).iterator();
      ONLTableCellRenderer.Committable elem;
      Component elem_label;
      if (isSelected) 
        {
          setBackground(table.getSelectionBackground());
          setForeground(table.getSelectionForeground());
        }
      else
        {
          setBackground(table.getBackground());
          setForeground(table.getForeground());
        }
      boolean started = false;
      while(iter.hasNext())
        {
          if (started) add(Box.createHorizontalGlue());
          else started = true;
          elem = (ONLTableCellRenderer.Committable)iter.next();
          //System.out.println("Coll Renderer adding " + elem.toString());
          ElementRenderer rend = new ElementRenderer();
          elem_label = rend.getTableCellRendererComponent(table, elem, isSelected, hasFocus, row, col);
          /*elem_label = new JLabel(elem.toString());
          elem_label.setFont(table.getFont());
          
          if (elem.needsCommit()) 
            {
              elem_label.setForeground(uncommittedColor);
            }
            else elem_label.setForeground(Color.black);*/
          add(elem_label);
        }
      return this;
    }
  public boolean isOpaque() { return false;}
  //public void validate() {}
  public void revalidate() {}
  public void firePropertyChange() {}
  public void repaint(Rectangle r) {}
  public void repaint(long tm, int x, int y, int width, int height) {}
}
 
