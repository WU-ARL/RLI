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
 * File: Units.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/23/2008
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.lang.reflect.Array;

public class Units
{
  public static final int UNKNOWN = 0;
  
  public static final int PERSEC = 64;
  public static final int KILO = 128;
  public static final int MEGA = 256;
  
  public static final int BITS = 1;
  public static final int MBITS = MEGA + BITS;
  public static final int KBITS  = KILO + BITS;
  public static final int BYTES = 2;
  public static final int MBYTES = MEGA + BYTES;
  public static final int KBYTES  = KILO + BYTES;
  public static final int CELLS = 4;
  public static final int PKTS = 8; 

  public static final String UNKNOWN_STR = "Other";
  public static final String BITS_STR = "bits";
  public static final String MBITS_STR = "Mb";
  public static final String KBITS_STR = "Kb";
  public static final String BYTES_STR = "bytes";
  public static final String MBYTES_STR = "MB";
  public static final String KBYTES_STR = "KB";
  public static final String CELLS_STR = "Cells";
  public static final String PKTS_STR = "Pkts"; 
  public static final String SEC_STR = "s"; 

  private int originalType = UNKNOWN;
  private int currentType = UNKNOWN;
  private double conversionFactor = 1;
  private double scalingFactor = 1;
  private double totalScale = 1;
  private boolean includeHeader = true;
  
  public static class ComboBox extends JComboBox
  {
    private Units units[];
    public ComboBox() 
     {
       super();
       units = new Units[]{
         (new Units(BITS)),
           (new Units(BITS+PERSEC)),
           (new Units(KBITS)),
           (new Units(KBITS+PERSEC)),
           (new Units(MBITS)),
           (new Units(MBITS+PERSEC)),
           (new Units(BYTES)),
           (new Units(BYTES+PERSEC)),
           (new Units(KBYTES)),
           (new Units(KBYTES+PERSEC)),
           (new Units(MBYTES)),
           (new Units(MBYTES+PERSEC)),
           (new Units(CELLS)),
           (new Units(CELLS+PERSEC)),
           (new Units(PKTS)),
           (new Units(PKTS+PERSEC)),
           (new Units(UNKNOWN))};
       for (int i = 0; i < 17; ++i)
         insertItemAt(units[i], i);
       setEditable(false);
     }
    public ComboBox(int t)
     {
       this();
       setSelectedUnits(t);
     }
    public Units getSelectedUnits() { return ((Units)getSelectedItem());}
    public void setSelectedUnits(int t)
    {
      for (int i = 0; i < 16; ++i)
        {
          if (units[i].getType() == t) 
            {
              setSelectedItem(units[i]);
            }
        }
    }    
  }

  public static String getLabel(int u)
    {
      String nm;
      int tmp_u = u;
      if ((tmp_u & PERSEC) > 0) tmp_u -= PERSEC;	
      switch(tmp_u)
	{
	case UNKNOWN:
	  nm = new String(UNKNOWN_STR);
	  break;
	case BYTES:
	  nm = new String(BYTES_STR);
	  break;
	case MBYTES:
	  nm = new String(MBYTES_STR);
	  break;
	case KBYTES:
	  nm = new String(KBYTES_STR);
	  break;
	case MBITS:
	  nm = new String(MBITS_STR);
	  break;
	case KBITS:
	  nm = new String(KBITS_STR);
	  break;
	case CELLS:
	  nm = new String(CELLS_STR);
	    break;
	case PKTS:
	  nm = new String(PKTS_STR);
	  break;
	default: //BITS
	    nm = new String(BITS_STR);
	}
      if ((u & PERSEC) > 0) return (new String(nm + "/" + SEC_STR));
      else return nm;
    }

  public static double getScaleFactor(int frm_u, int to_u, boolean b)
    {
      Converter converter = new Converter(frm_u, to_u, b);
      return (converter.getScaleFactor());
    } 
  public static double getScaleFactor(int frm_u, int to_u)
    {
      Converter converter = new Converter(frm_u, to_u);
      return (converter.getScaleFactor());
    }
  
  //inner class create and object that does conversion between units
  public static class Converter
  {
    private double scaleFactor = 1;
    private double transFactor = 0;
    private int fromUnits = Units.UNKNOWN;
    private int toUnits = Units.UNKNOWN;
    private boolean includeHeader = true;
    
    public Converter(){}
    
    public Converter(int frm_u, int to_u) { this(frm_u, to_u, true);}
    public Converter(int frm_u, int to_u, boolean b)
      {
	setConversion(frm_u, to_u, b);
      }
    public void setConversion(int frm_u, int to_u) { setConversion(frm_u, to_u, true);}
    public void setConversion(int frm_u, int to_u, boolean b)
      {
	transFactor = 0;
	fromUnits = frm_u;
	toUnits = to_u;
        includeHeader = b;
	int tmp_frmu = frm_u;
	if ((tmp_frmu & PERSEC) > 0) tmp_frmu -= PERSEC;
	int tmp_tou = to_u;
	if ((tmp_tou & PERSEC) > 0) tmp_tou -= PERSEC;
        //can't convert unknown or packets in either direction
	if (tmp_frmu == tmp_tou || tmp_frmu == UNKNOWN || tmp_tou == UNKNOWN || tmp_frmu == PKTS || tmp_tou == PKTS)
	  {
	    scaleFactor = 1;
	    return;
	  }
	
	//first convert to bits
	double tmp = 1;
	int tmp_u = tmp_frmu;
	switch(tmp_u)
	  {
	  case BYTES:
	    tmp = 8;
	    break;
	  case MBYTES:
	    tmp = 8000000;
	    break;
	  case KBYTES:
	    tmp = 8000;
	    break;
	  case MBITS:
	    tmp = 1000000;
	    break;
	  case KBITS:
	    tmp = 1000;
	    break;
	  case CELLS:
            if (includeHeader)
              tmp = 424; //53 * 8
            else
              tmp = 384; //48 * 8
	    break;
	  default: //BITS
	    tmp = 1;
	  }
	
	tmp_u = tmp_tou;
	//convert to return units	
	switch(tmp_u)
	  {
	  case BYTES:
	    scaleFactor = tmp/8;
	    break;
	  case MBYTES:
	    scaleFactor = tmp/8000000;
	    break;
	  case KBYTES:
	    scaleFactor = tmp/8000;
	    break;
	  case MBITS:
	    scaleFactor = tmp/1000000;
	    break;
	  case KBITS:
	    scaleFactor = tmp/1000;
            break;
	  case CELLS:
            if (includeHeader)
              scaleFactor = tmp/424; //53 * 8
            else
              scaleFactor = tmp/384; //48 * 8
	    break;
	  default: //BITS
	    scaleFactor = tmp;
	  }
      }
    public int getFromUnits() { return fromUnits;}
    public int getToUnits() { return toUnits;}
    public double getScaleFactor() { return scaleFactor;}
    public double getTransFactor() { return transFactor;}
    public double convert(double val)
      {
	return ((val * scaleFactor) + transFactor);
      }
    public double getInverse(double val)
      {
	return ((val - transFactor)/scaleFactor);
      }
    public boolean isHeaderIncluded() { return includeHeader;}
    public void setIncludeHeader(boolean b) { setConversion(fromUnits, toUnits, b);}
  } //end inner class Converter

  public Units(ONL.Reader rdr) throws java.io.IOException
    {
      this(rdr.readInt());
      if (rdr.getVersion() >= 1.3)
        {
          setType(rdr.readInt());
          setScaleFactor(rdr.readDouble());
        }
    }
  public Units(int t)
    {
      originalType = t;
      currentType = t;
    }
  public void write(ONL.Writer wrtr)  throws java.io.IOException
    { 
      wrtr.writeString(new String(Units.getLabel(originalType) +  " " + Units.getLabel(currentType) + " " + scalingFactor));
    }
  public String getLabel() { return (Units.getLabel(originalType));}
  public String getDisplayLabel()
    {
      return (new String(Units.getLabel(originalType) + " converted to " + Units.getLabel(currentType) + " scaled by " + scalingFactor));
    }
  public String toString() { return (Units.getLabel(currentType));}
  public int getType() { return currentType;}
  public void setType(int t) 
    {
      if ((originalType & PKTS) == 0)
        {
          currentType = t;
          conversionFactor = getScaleFactor(originalType, currentType, includeHeader);
          totalScale = conversionFactor * scalingFactor;
        }
      else
        {
          if ((t & PKTS) > 0) currentType = t; //can change from PKTS to PKTS/s 
        }
    }
  public void revertType() { setType(originalType);}
  public int getOriginalType() { return originalType;}
  public void setScaleFactor(double d) 
    { 
      scalingFactor = d;
      totalScale = conversionFactor * scalingFactor;
    }
  public double getScaleFactor() { return scalingFactor;}
  public double getTotalScaleFactor() { return totalScale;}
  public EditablePanel getEditablePanel() { return (getEditablePanel(true));}
  public EditablePanel getEditablePanel(boolean edit)
   {
     return (new EditablePanel(this, edit));
   }

  public void setIncludeHeader(boolean b) 
   { 
     includeHeader = b;
     setType(currentType);
   }
  public boolean isHeaderIncluded() { return includeHeader;}

  public static class EditablePanel extends JPanel
  {
    private TextFieldwLabel scaleField = null;
    private ComboBox newType = null;
    private Units units = null;
    private JCheckBox includeATMHdr = null;
    public EditablePanel(Units u) { this(u, true);}
    public EditablePanel(Units u, boolean panel_edit)
      {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        units = u;
        if ((units.originalType & PKTS) == 0)
          {
            add(new JLabel(new String("Units: " + Units.getLabel(units.originalType)  + " converted to: " )));
            newType = new Units.ComboBox(units.currentType);
            newType.setEditable(panel_edit);
            add(newType);
          }
        else
          add(new JLabel(new String("Units: " + Units.getLabel(units.originalType))));
        scaleField = new TextFieldwLabel(new TextFieldPlus(10, Double.toString(units.scalingFactor), true), " scaled by:");
        scaleField.setEditable(panel_edit);
        add(scaleField);
        if ((units.originalType & CELLS) > 0)
          {
            JPanel tmp_panel = new JPanel();
            tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
            tmp_panel.add(new JLabel(" include ATM header:"));
            includeATMHdr = new JCheckBox();
            includeATMHdr.setSelected(units.isHeaderIncluded());
            includeATMHdr.setEnabled(panel_edit);
            tmp_panel.add(includeATMHdr);
            add(tmp_panel);
          }
      }
    public void setUnits()
     {
       if (newType != null && newType.getSelectedUnits() != null) units.setType(newType.getSelectedUnits().getType());
       ExpCoordinator.print(new String("Units.EditablePanel.setUnits scale = " + scaleField.getText()), 3);
       if (scaleField.getText().length() > 0) units.setScaleFactor(Double.parseDouble(scaleField.getText()));
       if (includeATMHdr != null) units.setIncludeHeader(includeATMHdr.isSelected());
     }
   }

  public static int getType(String lbl)
  {
    String[] strarray = lbl.split("/");
    int len = Array.getLength(strarray);
    int tp = UNKNOWN;
    if (len > 0)
      {
        if (strarray[0].equalsIgnoreCase(BYTES_STR)) tp = BYTES;
        if (strarray[0].equals(MBYTES_STR) || strarray[0].equalsIgnoreCase(ExperimentXML.MBYTES)) tp = MBYTES;
        if (strarray[0].equals(KBYTES_STR) || strarray[0].equalsIgnoreCase(ExperimentXML.KBYTES)) tp = KBYTES;
        if (strarray[0].equalsIgnoreCase(BITS_STR)) tp = BITS;
        if (strarray[0].equals(MBITS_STR) || strarray[0].equalsIgnoreCase(ExperimentXML.MBITS)) tp = MBITS;
        if (strarray[0].equals(KBITS_STR) || strarray[0].equalsIgnoreCase(ExperimentXML.KBITS)) tp = KBITS;
        if (strarray[0].equalsIgnoreCase(CELLS_STR)) tp = CELLS;
        if (strarray[0].equalsIgnoreCase(PKTS_STR) || strarray[0].equalsIgnoreCase(ExperimentXML.PACKETS)) tp = PKTS;
      }
    if (len > 1 && strarray[1].equals(SEC_STR)) tp |= PERSEC;
    return tp;
  }
}
