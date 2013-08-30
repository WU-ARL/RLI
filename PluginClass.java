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
 * File: PluginClass.java
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
import java.util.*;
import java.lang.*;
import java.lang.reflect.Array;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.AWTEvent;
import javax.xml.stream.*;


public class PluginClass
{
  private String name = "";
  protected String pluginDir = "";
  private int classID = 0;
  private String state = ONLComponent.NOT_INIT;
  protected String rootDir = "";

  private EventListenerList listeners;
  
  public class Event extends AWTEvent
  {
    public Event(PluginClass source)
    {
      super(source, -1);
    }
  }
  public interface Listener extends EventListener
  {
    public void stateChanged(PluginClass.Event e);
  }
  public PluginClass() { listeners = new EventListenerList();}
  public PluginClass(PluginClass pc)
    {
      this();
      name = new String(pc.name);
      classID = pc.classID;
      setPluginDir(new String(pc.pluginDir));
      state = new String(pc.state);
    }
  public PluginClass(ONL.Reader rdr) throws java.io.IOException
    {
      this();
      read(rdr);
    }

  public PluginClass(String nm, String pd, int cd, String st)
    {
      name = nm;
      setPluginDir(pd);
      classID = cd;
      state = new String(st);
    }

  public PluginClass(String nm, String pd, int cd)
    {
      name = nm;
      setPluginDir(pd);
      classID = cd;
    }

  public String toString()
    {
      return (new String(name + " " + classID + " " + pluginDir));
    }

  protected static void skip(ONL.Reader rdr) throws java.io.IOException
    {
      rdr.readString();
      rdr.readInt();
      rdr.readString();
    }

  public void read(ONL.Reader rdr) throws java.io.IOException
    {
      name = rdr.readString();
      classID = rdr.readInt();
      setPluginDir(rdr.readString());
    }

  public void write(ONL.Writer wrtr) throws java.io.IOException
    {
      wrtr.writeString(name);
      wrtr.writeInt(classID);
      wrtr.writeString(pluginDir);
    }

  public void writeXML(XMLStreamWriter xmlWrtr, boolean isnpr) throws XMLStreamException
  {
    xmlWrtr.writeStartElement(ExperimentXML.DIRECTORY);//"directory");
    xmlWrtr.writeCharacters(pluginDir);
    xmlWrtr.writeEndElement(); 
    xmlWrtr.writeStartElement(ExperimentXML.CLASSNAME);//"className");
    xmlWrtr.writeCharacters(name);
    xmlWrtr.writeEndElement(); 
    if (!isnpr)
      {
	xmlWrtr.writeStartElement(ExperimentXML.CLASSID);//"classID");
	xmlWrtr.writeCharacters(String.valueOf(classID));
	xmlWrtr.writeEndElement();
      } 
  }
  public boolean isEqual(PluginClass c) 
    { 
      return (name.equals(c.name) && 
              classID == c.classID && 
              pluginDir.equals(c.pluginDir));
    }

  public String getName() { return name;}
  public void setName(String str) { name = str;}
  public int getID() { return classID;}
  public void setID(int v) { classID = v;}
  public String getPluginDir() { return pluginDir;}
  public void setPluginDir(String str) 
  { 
    String tmp_str = PluginClasses.getDirString(str);
    if (pluginDir.equals(tmp_str)) return;
    pluginDir = new String(tmp_str);
    if (pluginDir.length() == 0) rootDir = pluginDir;
    else
      {
        String[] strarray = pluginDir.split("/");
        int len = Array.getLength(strarray) - 1;
        rootDir = "";
        for (int i = 0; i < len; ++i)
          {
            if (strarray[i].length() > 0)
              rootDir = rootDir.concat(new String("/" + strarray[i]));
          }
      }
    ExpCoordinator.print(new String("PluginClass.setPluginDir " + str + " pluginDir:" + pluginDir + " rootDir:" + rootDir), 2);
  }
  public void setCommitted(boolean b) 
  { 
    if (b && state.equals(ONLComponent.NOT_INIT)) setState(ONLComponent.WAITING);
    if (!b && !state.equals(ONLComponent.NOT_INIT)) setState(ONLComponent.NOT_INIT);
  }
  public boolean isCommitted() { return ( !state.equals(ONLComponent.NOT_INIT));}
  public void setState(String str)
  {
    if (!str.equals(state)) // || state.equals(ONLComponent.NOT_INIT) || state.equals(ONLComponent.FAILED))) //not sure why this was here
      {
        ExpCoordinator.printer.print(new String("PluginClass-" + name + ".setState " + str), 4);
        state = new String(str);
        fireEvent(new PluginClass.Event(this));
      }
  }
  public String getState() { return state;}
  public void fireEvent(PluginClass.Event e)
  {
    int max = listeners.getListenerCount();
    int i = 0;
    Object[] list = listeners.getListenerList();
    PluginClass.Listener l;
    //turns out this is a list of class, listener pairs so list looks like class1, listener1, class2, listener2 ... classn, listenern
    for (i = 1; i < list.length; i += 2)
      {
        l = (PluginClass.Listener) (list[i]);
        l.stateChanged(e);
      }
  }
  public void addPClassListener(PluginClass.Listener l) { listeners.add(PluginClass.Listener.class, l);}
  public void removePClassListener(PluginClass.Listener l){ listeners.remove(PluginClass.Listener.class, l);}
  public String getRootDir() { return rootDir;}
}
