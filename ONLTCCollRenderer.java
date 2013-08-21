/*
 * Copyright (c) 2008 Jyoti Parwatikar and Washington University in St. Louis.
 * All rights reserved
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author or Washington University may not be used
 *       to endorse or promote products derived from this source code
 *       without specific prior written permission.
 *    4. Conditions of any other entities that contributed to this are also
 *       met. If a copyright notice is present from another entity, it must
 *       be maintained in redistributions of the source code.
 *
 * THIS INTELLECTUAL PROPERTY (WHICH MAY INCLUDE BUT IS NOT LIMITED TO SOFTWARE,
 * FIRMWARE, VHDL, etc) IS PROVIDED BY THE AUTHOR AND WASHINGTON UNIVERSITY
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR WASHINGTON UNIVERSITY
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS INTELLECTUAL PROPERTY, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * */

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
 
