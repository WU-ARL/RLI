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
 * File: ONLPropertyList.java
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
import java.beans.*;
import java.util.*;
import java.lang.reflect.Array;


public class ONLPropertyList extends Properties
{
  private PropertyChangeSupport pChangeSupport = null;
  private ONLComponent onlComponent = null;

  public ONLPropertyList(Object c)
    {
      super();
      pChangeSupport = new PropertyChangeSupport(c);
      if (c instanceof ONLComponent) onlComponent = (ONLComponent)c;
    } 
  public void addListener(String pname, PropertyChangeListener l)
    {
      pChangeSupport.addPropertyChangeListener(pname, l);
    }
  public void addListener(PropertyChangeListener l)
    {
      pChangeSupport.addPropertyChangeListener(l);
    }
  public void removeListener(String pname, PropertyChangeListener l)
    {
      pChangeSupport.removePropertyChangeListener(pname, l);
    }
  public void removeListener(PropertyChangeListener l)
    {
      pChangeSupport.removePropertyChangeListener(l);
    }
  public Object setProperty(String name, String nvalue)
    {
      String oldValue = getProperty(name);
      ExpCoordinator.print(new String("ONLPropertyList::setProperty " + name + " to " + nvalue + " from " + oldValue), 10);
      Object rtn = super.setProperty(name, nvalue);
      pChangeSupport.firePropertyChange(name, oldValue, nvalue);
      return (rtn);
    }
  public Object setProperty(String name, int nvalue)
    {
      return (setProperty(name, String.valueOf(nvalue)));
    }
  public Object setProperty(String name, boolean nvalue)
    {
      return (setProperty(name, String.valueOf(nvalue)));
    }
  public Object setProperty(String name, double nvalue)
    {
      return (setProperty(name, String.valueOf(nvalue)));
    }
  public String getPropertyString(String name)
    {
      return ((String)getProperty(name));
    }
  public int getPropertyInt(String name)
    {
      String v = getProperty(name);
      if (v == null) return 0;
      return (Integer.parseInt(v));
    }
  public double getPropertyDouble(String name)
    {
      String v = getProperty(name);
      if (v == null) return 0;
      return (Double.parseDouble(v));
    }
  public boolean getPropertyBool(String name)
    {
      String v = getProperty(name);
      if (v == null) return false;
      return (Boolean.valueOf(v).booleanValue());
    }
  public void read(String s)
    {
      String[] strarray = s.split(" ");
      int len = Array.getLength(strarray);
      for (int i = 0; i < len; i += 2)
        {
          setProperty(strarray[i], strarray[i+1]);
        }
    }
  public String toString()
    {
      String str = "";
      for (Enumeration e = propertyNames() ; e.hasMoreElements() ;) 
        {
          String elem = (String)e.nextElement();
          str = str.concat(new String(elem + " " + getProperty(elem) + " "));
        }
      return str;
    }  

  protected void removeExpListeners(Experiment exp)
  {
    PropertyChangeListener[] p_array = pChangeSupport.getPropertyChangeListeners();
    int max = Array.getLength(p_array);
    ONL.ExpPropertyListener elem;
    for (int i = 0; i < max; ++i)
      {
        elem = null;
        if (p_array[i] instanceof PropertyChangeListenerProxy)
          {
            if (((PropertyChangeListenerProxy)p_array[i]).getListener() instanceof ONL.ExpPropertyListener)
              elem = (ONL.ExpPropertyListener)((PropertyChangeListenerProxy)p_array[i]).getListener();
          }
        else
          {
            if (p_array[i] instanceof ONL.ExpPropertyListener)
              elem = (ONL.ExpPropertyListener)p_array[i];
          }
        if (elem != null && elem.getExperiment() == exp) removeListener(elem);
      }
  }

  public ONLComponent getONLComponent() { return onlComponent;}
}
