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
 * File: PluginClasses.java
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
import java.io.*;
import java.awt.Color;
import javax.swing.*;
import javax.swing.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class PluginClasses extends ONLComponent implements PropertyChangeListener
{
  private PluginClasses.DirList pclassDirs = null;
  private PluginClasses.List defaultClasses = null;
  private NCCPOpManager opManager = null;
  //private int refreshCount = 0;

  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// PluginClasses.PCContentHandler ////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private class PCContentHandler extends DefaultHandler
   {
      private ExperimentXML expXML = null;
      private String currentElement = "";
      PluginClass currentPClass = null;

      public PCContentHandler(ExperimentXML exp_xml)
      {
	super();
	expXML = exp_xml;
      }
      public void startElement(String uri, String localName, String qName, Attributes attributes) 
      {	  
	  currentElement = new String(localName);
	  if (localName.equals(ExperimentXML.PLUGIN) && currentPClass == null) currentPClass = new PluginClass();
      }
      public void characters(char[] ch, int start, int length)
      {
	if (currentElement.equals(ExperimentXML.DIRECTORY) && currentPClass != null)
	  currentPClass.setPluginDir(new String(ch, start, length));
	if (currentElement.equals(ExperimentXML.CLASSNAME) && currentPClass != null)
	  currentPClass.setName(new String(ch, start, length));
	if (currentElement.equals(ExperimentXML.CLASSID) && currentPClass != null)
	  currentPClass.setID(Integer.parseInt(new String(ch, start, length)));
      }
      public void setCurrentElement(String s) { currentElement = new String(s);}
      public String getCurrentElement() { return currentElement;}
      public void endElement(String uri, String localName, String qName)
      {
	  if (localName.equals(ExperimentXML.PCLASSES)) 
	    {
              expXML.removeContentHandler(this);
            }
	  if (localName.equals(ExperimentXML.PLUGIN) && currentPClass != null) 
	    {
	      addPluginClass(currentPClass);
	      currentPClass = null;
	    }
      }
   }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// PluginClasses.Menu ////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  public static abstract class Menu extends JMenu implements ListDataListener
  {
    private PluginClasses pluginClasses = null;
    private Vector labels = null;
    private Color labelColor = null;

    private class Label
    {
      public JLabel label = null;
      public PluginClasses.List pclist = null;
      public Label(JLabel l, PluginClasses.List pl)
        {
          label = l;
          pclist = pl;
        }
    }
    
    private class DirListener implements ListDataListener
    {
      private PluginClasses.Menu menu = null;
      public DirListener(PluginClasses.Menu m) { menu = m;}
      public void contentsChanged(ListDataEvent e){}
      public void intervalAdded(ListDataEvent e)
      {
        int start = e.getIndex0();
        int end = e.getIndex1();
        
        for (int i = start; i <= end; ++i)
          {
            PluginClasses.List clist = pluginClasses.pclassDirs.getPCListAt(i);
            clist.addListDataListener(menu);
            Label lbl = new Label(new JLabel(clist.getRootDir()), clist);
            //lbl.label.setBackground(labelColor);
            labels.add(lbl);
            addSeparator();
            add(lbl.label);
            addSeparator();
            int max = clist.getSize();
            for (int j = 0; j < max; ++j)
              {
                add(new PCMenuItem(getPClassAction(clist.getPClassAt(j))));
              }
          }
      }

    public void intervalRemoved(ListDataEvent e)
      {
        int start = e.getIndex0();
        int end = e.getIndex1();
        
        for (int i = start; i <= end; ++i)
          {
            PluginClasses.List clist = pluginClasses.pclassDirs.getPCListAt(i);
            clist.removeListDataListener(menu);
            int menu_start = getStartIndex(clist);
            int menu_end = getEndIndex(clist);
            for (int j = menu_start; j <= menu_end; ++j)
              remove(menu_start);
          }
      }
    }

    private class PCMenuItem extends JMenuItem
    {
      private PCAction pcaction = null;
      public PCMenuItem(PCAction a) 
      {
        super(a);
        pcaction = a;
      }
      public PluginClass getPluginClass() { return (pcaction.getPluginClass());}
    }

    public static abstract class PCAction extends AbstractAction
    {
      private PluginClass pluginClass = null;

      public PCAction(PluginClass pc) { this(pc, new String(pc.getName()+ ":" + pc.getID()));}
      public PCAction(PluginClass pc, String nm)
      {
        super(nm);
        pluginClass = pc;
        setEnabled(true);
        ExpCoordinator.printer.print(new String("PCAction.constructor " + pc.getName() + " state is " + pc.getState()), 4);
      }
      public PluginClass getPluginClass() { return pluginClass;}
    }

    private class DirRefresher implements MenuListener
    {
      public DirRefresher() {}
      public void menuCanceled(MenuEvent e) {}
      public void menuDeselected(MenuEvent e) {}
      public void menuSelected(MenuEvent e) 
        {
          ExpCoordinator.print("PluginClasses.DirRefresher.menuSelected", 2);
          if (!pluginClasses.isRefreshing()) pluginClasses.refreshDirs();
        }
    }


    public Menu(String ttl, PluginClasses pc) { this(ttl, pc, true);}
    public Menu(String ttl, PluginClasses pc, boolean refresh)
      {
        super(ttl);
        labels = new Vector();
        labelColor = new Color(7,141,208);
        pluginClasses = pc;
        if (pluginClasses == null) ExpCoordinator.print("PluginClasses.Menu pluginClasses is null", 4);
        else 
          {
            if (pluginClasses.pclassDirs == null) ExpCoordinator.print("PluginClasses.Menu pluginClasses.pclassDirs is null", 4);
          }
        pluginClasses.pclassDirs.addListDataListener(new DirListener(this));
        if (refresh) addMenuListener(new DirRefresher());
      }

    protected void initialize()
      {
        PluginClasses.DirList dir_lst = pluginClasses.pclassDirs;
        int max1 = dir_lst.getSize();
        for (int i = 0; i < max1; ++i)
          {
            PluginClasses.List clist = dir_lst.getPCListAt(i);
            clist.addListDataListener(this);
            Label lbl = null;
            if (i != 0)
              lbl = new Label(new JLabel(clist.getRootDir()), clist);
            else lbl = new Label(new JLabel("Predefined Classes"), clist);
            //lbl.label.setBackground(labelColor);
            labels.add(lbl);
            addSeparator();
            add(lbl.label);
            addSeparator();
            int max2 = clist.getSize(); 
            ExpCoordinator.print(new String("PluginClasses.Menu.initialize rootdir: " + clist.getRootDir() + " list size " + max2), 5);
            for (int j = 0; j < max2; ++j)
              {
                add(new PCMenuItem(getPClassAction(clist.getPClassAt(j))));
              }
          }
      }
    public abstract PCAction getPClassAction(PluginClass pc) ;
    public void contentsChanged(ListDataEvent e){}
    public void intervalAdded(ListDataEvent e)
      {
        int start = e.getIndex0();
        int end = e.getIndex1();
        PluginClasses.List clist = (PluginClasses.List)e.getSource();
        int menu_end = getEndIndex(clist);
        for (int i = start; i <= end; ++i)
          {
            add(new PCMenuItem(getPClassAction(clist.getPClassAt(i))), menu_end);
            ++menu_end;
          }
      }
    public void intervalRemoved(ListDataEvent e)
      {
        PluginClasses.List clist = (PluginClasses.List)e.getSource();
        int menu_start = getStartIndex(clist) + 3;
        int start = e.getIndex0() + menu_start;
        int end = e.getIndex1() + menu_start;

        for (int i = start; i <= end; ++i)
          {
            remove(i);
          }
      }
    private int getStartIndex(PluginClasses.List clist)
    {
      int max = labels.size();
      int i;
      JLabel lbl = null; 
      for (i = 0; i < max; ++i)
        {
          Label elem = (Label)labels.elementAt(i);
          if (elem.pclist == clist) 
            {
              lbl = elem.label;
              break;
            }
        }
      max = getItemCount();
      if (lbl != null)
        {
          for (i = 0; i < max; ++i)
            {
              if (lbl == getMenuComponent(i)) return (i - 1);
            }
        }
      return max;
    }
    private int getEndIndex(PluginClasses.List clist)
    {
      int max = labels.size();
      int i;
      JLabel lbl = null; 
      for (i = 0; i < max; ++i)
        {
          Label elem = (Label)labels.elementAt(i);
          if (elem.pclist == clist) 
            {
              if ( i < (max -1 )) lbl = ((Label)labels.elementAt(i+1)).label;
              break;
            }
        }
      max = getItemCount();
      if (lbl != null) 
        {
          for (i = 0; i < max; ++i)
            {
              if (lbl == getMenuComponent(i)) return (i - 1);
            }
        }
      return max;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// PluginClasses.OpManager ///////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  private class OpManager extends NCCPOpManager
  {
    Vector pendingOps = null;
    public OpManager()
      {
	super((MonitorDaemon)((Router)getParent()).getONLDaemon(ONLDaemon.HWC));
	response = new NCCP_PClassResponse(false);
        ((MonitorDaemon)((Router)getParent()).getONLDaemon(ONLDaemon.HWC)).addOpManager(this);
      }
    protected void setRendMarker(NCCP.RequesterBase r, NCCPOpManager.Operation op, int i)
      {
	NCCP_PClassRequester req = (NCCP_PClassRequester)r;
        if (getParent() != null && getParent() instanceof NSPDescriptor)
          req.setMarker(new REND_Marker_class(req.getOp(), false, getParent().getPropertyInt(NSPDescriptor.INDEX), i));
        else req.setMarker(new REND_Marker_class(req.getOp(), false, 0, i));
      }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// PluginClasses.NCCP_PClassRequester ////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  public static class NCCP_PClassRequester extends NCCP.RequesterBase
  {
    private String rootDir = null;
    
    public NCCP_PClassRequester()
    {
      super(NCCP.Operation_AddPClasses);
    }
    
    public NCCP_PClassRequester(String dir)
    {
      this();
      rootDir = dir;
      setMarker(new REND_Marker_class());
    }
    
    public NCCP_PClassRequester(PluginClass pc)
    {
      this();
      setMarker(new REND_Marker_class());
    }
   
    public void storeData(DataOutputStream dout) throws IOException
    {
      NCCP.writeString(rootDir, dout);
      NCCP.writeString("", dout);
      dout.writeInt(0);
      NCCP.writeString("", dout);
    }
    public String getRootDir() { return rootDir;}
  } //class NCCP_PClassRequester

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////// NCCP_PClassResponse Inner class /////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class NCCP_PClassResponse extends NCCP.ResponseBase
  {
    private Vector pclasses = null;
    public NCCP_PClassResponse (boolean npr)//FilterDescriptor fd)
      {
	super(4);
        pclasses = new Vector();
      }
    public void retrieveFieldData(DataInput din) throws IOException
      {
	pclasses.clear();
        int size = din.readInt();
        ONL.NCCPReader rdr = new ONL.NCCPReader(din);
        for (int i = 0; i < size; ++i)
          {
	    pclasses.add(new PluginClass(rdr));
          }
      }
    public double getData(MonitorDataType.Base mdt)
      {
	return (pclasses.size());
      }
    public Vector getPClasses() { return pclasses;}
    public String getString() { return "Plugin Add Class Op";}
  }// class NCCP_PClassResponse

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// PluginClasses.AddClassAction //////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class AddClassAction extends AbstractAction
  {
    private PluginClasses pluginClasses = null;
    
    public AddClassAction (PluginClasses cl, String ttl, boolean is_dir)
    {
      super(ttl);
      pluginClasses = cl;
    }
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
      String ttl = (String)getValue(NAME);

      Object[] array = new Object[2];
     
      array[0] = new JLabel("directory (give full path of directory to add):");     
      array[1] = new JTextField(50);
      
      final String opt0 = "OK";
      final String opt1 = "Cancel";
      Object[] options = {opt0,opt1};
      
      int rtn = JOptionPane.showOptionDialog(ExpCoordinator.theCoordinator.getMainWindow(), 
                                             array, 
                                             ttl, 
                                             JOptionPane.YES_NO_OPTION,
                                             JOptionPane.QUESTION_MESSAGE, 
                                             null,
                                             options,
                                             options[0]);
      
      if (rtn == JOptionPane.YES_OPTION) 
        {
          String tmp_dir = getDirString(((JTextField)array[1]).getText());
          pluginClasses.addPClassList(tmp_dir, true);
        }
    }
  }


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// PluginClasses.AddClassOp //////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class AddClassOp implements NCCPOpManager.Operation
  {
    //private NSPDescriptor nsp = null;
    private PluginClasses.List pluginClassList = null;
    //private PluginClass addedClass = null;
    private PluginClasses pluginClasses = null;
    private Vector returnPClasses = null;
    private NCCP_PClassRequester request = null;

    public AddClassOp(PluginClasses pcs, String dir)
      {
        
        ExpCoordinator.print(new String("PluginClasses.AddClassOp for dir " + dir), 5);
	pluginClassList = pcs.getPClassList(dir);
        pluginClasses = pcs;
        //boolean is_npr = (pluginClasses.getParent() instanceof NPRouter.Instance);
	request = new NCCP_PClassRequester(dir);
        returnPClasses = new Vector();
      }

    //NCCPOpManager.Operation
    public void opSucceeded(NCCP.ResponseBase resp)
      {
        processResponse(resp);
      }
    public void opFailed(NCCP.ResponseBase resp)
      {
        ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("Add Plugin Directory Failure - couldn't read directory " + request.getRootDir())),
                                                                       (new String("Add Plugin Directory Failure for "+ pluginClasses.getLabel())),
                                                                       "Add Plugin Directory Failure.",
                                                                       StatusDisplay.Error.LOG));  
        pluginClassList.setState(ONLComponent.FAILED);
        //--pluginClasses.refreshCount;
        //pluginClasses.removePClassList(request.getRootDir());
	pluginClasses.opManager.removeOperation(this);
      }
    public Vector getRequests() 
      {
        Vector r = new Vector();
        r.add(request);
        return r;
      }
    public boolean containsRequest(REND_Marker_class rend) { return (request.getMarker().isEqual(rend));}
    //end NCCPOpManager.Operation
    private void processResponse(NCCP.ResponseBase resp)
      {
        Vector pclasses = ((NCCP_PClassResponse)resp).getPClasses();
        if (pclasses.size() > 0)
          {
            returnPClasses.addAll(pclasses);
          }
        else
          {
            String rdir = request.getRootDir();
            ONLComponent c = pluginClasses.getParent();
            String tmp  = new String("Add Plugin Directory - found no classes in "+ rdir);
            if (c != null) tmp = tmp.concat(new String(" on router " + c.getLabel()));
            ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error(tmp,
                                                                           tmp,
                                                                           "Add Plugin Directory - found no classes.",
                                                                           (StatusDisplay.Error.STATUSBAR | StatusDisplay.Error.LOG))); 
              
          }
	if (resp.isEndOfMessage())
          {
            if (returnPClasses.size() > 0)
              {
                pluginClassList.confirm(returnPClasses);  
                pluginClassList.setState(ONLComponent.ACTIVE);
              }
            pluginClasses.opManager.removeOperation(this);
            //--pluginClasses.refreshCount;
          }
      }
  }//inner class AddClassOp


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////// PluginClasses.List ///////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public class List extends AbstractListModel
   {     
     private String rootDir = "";
     private Vector classes = null;
     private String state = ONLComponent.NOT_INIT;
     public List(String rdir)
     {
       super();
       rootDir = getDirString(rdir);
       classes = new Vector();
     }

     public List(ONL.Reader rdr) throws IOException
     {
       super();
       classes = new Vector();
       read(rdr);
     }
    
     public PluginClass add(PluginClass c) { return(add(c, true));}
  
     public PluginClass add(PluginClass c, boolean fire)
     {
       PluginClass rtn = getPClass(c);
       if (rtn == null && c.getRootDir().equals(rootDir)) 
         {
           int len = classes.size();
           rtn = getNewPluginClass(c);
           classes.add(rtn);
           ExpCoordinator.print(new String(getLabel() + "PluginClasses.List.add " + rtn.toString()), 4);
           if (fire) fireIntervalAdded(this, len, len);
         }
       return rtn;
     }
     protected PluginClass getNewPluginClass(PluginClass c) 
     { 
       return (new PluginClass(c));
     }
     public void add(Vector pcs)
     {
       int max = pcs.size();
       boolean added;
       int start = classes.size();
       for (int i = 0; i < max; ++i)
         {
           PluginClass pclass = (PluginClass)pcs.elementAt(i);
           add(pclass, false);
         }
       int end = classes.size() - 1;
       if (end >= start) fireIntervalAdded(this, start, end);
     }

     private void confirm(Vector pcs)
     {  
       int max = pcs.size();
       int i;
       PluginClass pclass1;
       PluginClass pclass2;
       int start = classes.size();
       ExpCoordinator.printer.print(new String(getLabel() + ".confirm"), 4);
       for (i = 0; i < max; ++i)
         {
           pclass1 = (PluginClass)pcs.elementAt(i);
           pclass1.setState(ONLComponent.ACTIVE);
           pclass2 = add(pclass1, false);
           if (pclass2 != null)
             {
               pclass2.setState(ONLComponent.ACTIVE);
               ExpCoordinator.printer.print(new String(pclass2.getName() + " state:" + pclass2.getState()), 4);
             }
         }
       int end = classes.size() - 1;
       if (end >= start) fireIntervalAdded(this, start, end);
       max = pcs.size();
       //need to remove classes that no longer exist
       Stack rms = new Stack();
       int max2 = classes.size();
       boolean there = false;
       int j;
       for (i = 0; i < max2; ++i)
         {
           there = false;
           pclass1 = (PluginClass)classes.elementAt(i);
           for (j = 0; j < max; ++j)
             {
               if (pclass1.isEqual((PluginClass)pcs.elementAt(j)))
                 {
                   there = true;
                   break;
                 }
             }
           if (!there) rms.push(pclass1);
         }
       max2 = rms.size();
       for (i = 0; i < max2; ++i)
         {
           remove((PluginClass)rms.pop());
         }
     }
     
     public void remove(PluginClass c)
     {
       int i = getIndex(c);
       if (i >= 0)
         {
           classes.remove(i);
           fireIntervalRemoved(this, i, i);
         }
     }

     public boolean contains(PluginClass c)
     {
       if (classes.contains(c)) return true;
       return (getPClass(c) != null);
     }

     public String getPClassState(PluginClass c)
      {
        PluginClass pc = getPClass(c);
        if (pc != null) return ONLComponent.ACTIVE;
        if (state == ONLComponent.ACTIVE || state == ONLComponent.FAILED) return ONLComponent.FAILED;
        return ONLComponent.NOT_INIT;
      }
     
     public PluginClass getPClass(PluginClass c)
     {
       int max = classes.size();
       PluginClass elem;
       for (int i = 0; i < max; ++i)
         {
           elem = (PluginClass)classes.elementAt(i);
           if (elem.isEqual(c)) return elem;
         }       
       return null;
    }
     
     private int getIndex(PluginClass c)
     {
       int max = classes.size();
       PluginClass elem;
       for (int i = 0; i < max; ++i)
         {
           elem = (PluginClass)classes.elementAt(i);
           if (elem.isEqual(c)) return i;
         }       
       return -1;
     }

     //ListModel interface
     public Object getElementAt(int i) { return (classes.elementAt(i));}
     public int getSize() { return (classes.size());}
     //end ListModel interface

     public PluginClass getPClassAt(int i) { return ((PluginClass)classes.elementAt(i));}
     
     public PluginClass getPClass(String nm)
     {
       int max = classes.size();
       PluginClass elem;
       for (int i = 0; i < max; ++i)
         {
          elem = (PluginClass)classes.elementAt(i);
          if (elem.getName().equals(nm)) return elem;
         }
       return null;
     }
     protected void read(ONL.Reader tr) throws IOException
     {
       rootDir = getDirString(tr.readString());
     }
     
     public void write(ONL.Writer wr) throws IOException
     {
       wr.writeString(rootDir);
     }

     public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
     {
       int max = classes.size();
       PluginClass elem;
       for (int i = 0; i < max; ++i)
         {
          elem = (PluginClass)classes.elementAt(i);
	  xmlWrtr.writeStartElement(ExperimentXML.PLUGIN);
	  elem.writeXML(xmlWrtr, false);
	  xmlWrtr.writeEndElement();
         }
     }

     public String getState() { return state;}
     public void setState(String s) 
     {
       if (!state.equals(s))
         {
           state = s;
           if (state.equals(ONLComponent.FAILED))
             {
               int i1 = classes.size();
               classes.clear();
               fireIntervalRemoved(this,0,i1);
             }
           fireContentsChanged(this, 0, classes.size());
         }
     }
     public String getRootDir() { return rootDir;}
  }//end PluginClasses.List


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////// PluginClasses.DirList ///////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public class DirList extends AbstractListModel
   {     
     private Vector directories = null;
     public DirList()
     {
       super();
       directories = new Vector();
     }

     public DirList(ONL.Reader rdr) throws IOException
     {
       this();
       read(rdr);
     }
    
     public PluginClasses.List add(PluginClasses.List c) { return(add(c, true));}
  
     public PluginClasses.List add(PluginClasses.List c, boolean fire)
     {
       PluginClasses.List rtn = getPCList(c.getRootDir());
       if (rtn == null) 
         {
           int len = directories.size();
           rtn = c;
           directories.add(rtn);
           ExpCoordinator.printer.print(new String(getLabel() + "PluginClasses.DirList.add " + rtn.toString()), 4);
           if (fire) fireIntervalAdded(this, len, len);
         }
       return rtn;
     }
     
     public void add(Vector pcs)
     {
       int max = pcs.size();
       boolean added;
       int start = directories.size();
       for (int i = 0; i < max; ++i)
         {
           PluginClasses.List pclist = (PluginClasses.List)pcs.elementAt(i);
           add(pclist, false);
         }
       int end = directories.size() - 1;
       if (end >= start) fireIntervalAdded(this, start, end);
     }
     
     public void remove(PluginClasses.List c)
     {
       int i = getIndex(c);
       if (i >= 0)
         {
           directories.remove(i);
           fireIntervalRemoved(this, i, i);
         }
     }
     
     private void removePCList(String rdir)
     {
       PluginClasses.List lst = getPCList(rdir);
       ExpCoordinator.print(new String("PluginClasses.DirList.removePCList " + rdir), 2);
       if (lst != null) 
         {
           int o_sz = directories.size();
           directories.remove(lst);
           ExpCoordinator.print(new String("   pclist is non null o_sz = " + o_sz + " now " + directories.size()), 2);
         }
     }
     
     public PluginClasses.List getPCList(String rdir)
     {
       int max = directories.size();
       ExpCoordinator.print(new String("PluginClasses.DirList.getPCList " + rdir + " size " + max), 5);
       PluginClasses.List elem;
       for (int i = 0; i < max; ++i)
         {
           elem = (PluginClasses.List)directories.elementAt(i);
           ExpCoordinator.print(new String("PluginClasses.DirList.getPCList looking for " + rdir + " found " + elem.getRootDir()), 5);
           if (elem.getRootDir().equals(rdir)) return elem;
         }       
       return null;
     }

     public PluginClasses.List getPCListAt(int i) { return ((PluginClasses.List)directories.elementAt(i));}
     
     private int getIndex(PluginClasses.List c)
     {
       int max = directories.size();
       PluginClasses.List elem;
       for (int i = 0; i < max; ++i)
         {
           elem = (PluginClasses.List)directories.elementAt(i);
           if (elem == c) return i;
         }       
       return -1;
     }

     //ListModel interface
     public Object getElementAt(int i) { return (directories.elementAt(i));}
     public int getSize() { return (directories.size());}
     //end ListModel interface
     protected void read(ONL.Reader tr) throws IOException
     {
       int numElems = tr.readInt();
       for (int i = 0; i < numElems; ++i)
         {
           add(new PluginClasses.List(tr));
         }
     }
     
     public void write(ONL.Writer wr) throws IOException
     {
       int num_elems = directories.size();
       wr.writeInt(num_elems);
       for (int i = 0; i < num_elems; ++i)
	{
	  PluginClasses.List rt = (PluginClasses.List)directories.elementAt(i);
          rt.write(wr);
	}
     }

     public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
     {
       int num_elems = directories.size();
       for (int i = 0; i < num_elems; ++i)
	   {
	       PluginClasses.List rt = (PluginClasses.List)directories.elementAt(i);
	       rt.writeXML(xmlWrtr);
	   }
     }
  }//end PluginClasses.DirList

  /////////////////////////////////// PluginClasses ////////////////////////////////////////////////////////////////////
     
  public PluginClasses(Router nsp)
    {
      super(nsp, "PluginClasses", ONLComponent.PCLASSES, nsp.getExpCoordinator());
      addToDescription(1, new String(nsp.getLabel() + " Plugin Classes"));
      opManager = getOpManager();//new OpManager();
      pclassDirs = new DirList();
      defaultClasses = addPClassList(getStandardPath());
      if (expCoordinator.getDebugLevel() > 6)
        {
          defaultClasses.add( new PluginClass("test1", "", 1));
          defaultClasses.add(new PluginClass("test2", "", 2));
          defaultClasses.add(new PluginClass("test3", "", 3));
        }
      pclassDirs.add(defaultClasses);
      nsp.addPropertyListener(ONLComponent.STATE, this);
    }

  public String getStandardPath() { return "";}

  public NCCPOpManager getOpManager() 
  {
    if (opManager == null) opManager = new OpManager();
    return opManager;
  }

  public void writeExpDescription(ONL.Writer tw) throws IOException 
    { 
      //System.out.println("RT::writeExpDescription");
      super.writeExpDescription(tw);
      write(tw);
    }
      
  protected static void skip(ONL.Reader tr) throws IOException
    {
      int num_dirs = tr.readInt();
      for (int i = 0; i < num_dirs; ++i) tr.readString();
    }
      
  protected void read(ONL.Reader tr) throws IOException
    {
      int num_dirs = tr.readInt();
      for (int i = 0; i < num_dirs; ++i)
        {
          addPClassList(tr.readString());
        }
      if (pclassDirs.getSize() > 1) 	
       {
         Experiment exp = expCoordinator.getCurrentExp();
         if (!exp.containsParam(this)) exp.addParam(this);
       }
    }

  protected void write(ONL.Writer tw) throws IOException
    {
      int max = pclassDirs.getSize();
      //just write userClasses
      //write number of classes 
      tw.writeInt(max - 1);
      for (int i = 1; i < max; ++i)
        {
          ((List)pclassDirs.getElementAt(i)).write(tw);
        }
    }

  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
  {
      int max = pclassDirs.getSize();
      //just write userClasses
      //write number of classes 
      for (int i = 1; i < max; ++i)
        {
          ((List)pclassDirs.getElementAt(i)).writeXML(xmlWrtr);
        }
  }

  public PluginClasses.List getDefaultClasses() { return defaultClasses;}
  public PluginClasses.List getPClassList(String rdir) { return (pclassDirs.getPCList(rdir));}
  public PluginClasses.List addPClassList(String rdir) { return (addPClassList(rdir, false));}
  public PluginClasses.List addPClassList(String rdir, boolean force_add) 
    { 
      List pclist = pclassDirs.getPCList(rdir);
      if (pclist == null)
        {
          pclist = new List(rdir);
          pclassDirs.add(pclist);
          opManager.addOperation(new AddClassOp(this, rdir));
          if (pclassDirs.getSize() > 1) 	
            {
              Experiment exp = expCoordinator.getCurrentExp();
              if (!exp.containsParam(this)) exp.addParam(this);
            }
          if (ExpCoordinator.isRecording())
            ExpCoordinator.recordEvent(SessionRecorder.ADD_PLUGIN_DIR, new String[]{getParent().getLabel(), rdir});
        }
       if (force_add) opManager.addOperation(new AddClassOp(this, rdir));
      return pclist;
    }
  
  private void removePClassList(String rdir)
    {
      pclassDirs.removePCList(rdir);
    }
  /*
  public PluginClass getPluginClass(PluginClass pc)
    {
      List pclist = getPClassList(pc.getRootDir());
      
      if (pclist != null) return (pclist.getPClass(pc));
      else return null;
    }

  public boolean contains(PluginClass pc) { return (getPluginClass(pc) != null);}
  */
  //PropertyChangeListener

  public void propertyChange(PropertyChangeEvent e)
    {
      if (e.getSource() == getParent())
	{
	  if (e.getPropertyName().equals(ONLComponent.STATE))
	    {
              if (((String)e.getNewValue()).equals(ONLComponent.ACTIVE))
                {
                  //if (expCoordinator.getDebugLevel() > 6)
                    //opManager.addOperation(new AddClassOp(this, defaultClasses, ""));
                }
            }
        }
    }
  //end PropertyChangeListener

  private boolean isRefreshing() 
  { 
    return false;
    //if (refreshCount < 0) refreshCount = 0;
    //return (refreshCount == 0);
  }
  private void refreshDirs()
    {
      if (getParent().isActive())
        {
          ExpCoordinator.print("PluginClasses.refreshDirs", 2);
          int max = pclassDirs.getSize();
          //refreshCount += max;
          for (int i = 0; i < max; ++ i)
            {
              opManager.addOperation(new AddClassOp(this, ((List)pclassDirs.getElementAt(i)).getRootDir()));
            }
        }
    }

  public static String getDirString(String s)
  {
    String rtn_str = s.replaceAll("/+", "/");
    if (rtn_str.endsWith("/"))
      {
        int len = rtn_str.length();
        rtn_str = rtn_str.substring(0, len-1);
      }
    //ExpCoordinator.print(new String("PluginClasses.getDirString " + s + " return string:" + rtn_str), 6);
    return rtn_str;
  }

    public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new PCContentHandler(exp_xml));}
    private void addPluginClass(PluginClass pc)
    {
      if (pc != null)
	{
	  PluginClasses.List pclist = getPClassList(pc.getRootDir());
          if (pclist == null) addPClassList(pc.getRootDir());
	}
    }
  protected boolean merge(ONLComponent c)
  {
    if (c instanceof PluginClasses) 
      {
	return true;
      }
    else return false;
  }
}
