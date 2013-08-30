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
