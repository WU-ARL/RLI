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
 * File: ONLTableCellRenderer.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 12/2/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.awt.*;
import javax.swing.table.*;
import javax.swing.*;


public class ONLTableCellRenderer extends DefaultTableCellRenderer
{
  private Color uncommittedColor = null;
  private Color in2_color = Color.blue;
  public interface Committable 
  {
    public boolean needsCommit();
    public String getState();
    public void setState(String s);
    //public boolean isFailed();
  }
  public interface CommittableTable
  {
    public Committable getCommittableAt(int row);
  }

  public ONLTableCellRenderer()
    {
      this(Color.red);
    }
  public ONLTableCellRenderer(Color c)
    {
      super();
      uncommittedColor = c;
    }
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
    {
      Committable row_elem = ((CommittableTable)table.getModel()).getCommittableAt(row);
      if (row_elem != null)
        {
          //if (row_elem.isFailed())//strike through
	  String tmp_st = row_elem.getState();
          if ((ExpCoordinator.theCoordinator.isCurrentExpLive() && row_elem.needsCommit()) || (tmp_st != null && tmp_st.equals(ONLComponent.IN1)))
            {
              setForeground(uncommittedColor);
            }
          else 
	    {
	      if (tmp_st == null || !tmp_st.equals(ONLComponent.IN2))
		setForeground(null);
	      else setForeground(in2_color);
	    }
        }
      else System.out.println("ONLTableCellRenderer row_elem null for " + value.toString());
      return (super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col));
    }
}
