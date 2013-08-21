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
