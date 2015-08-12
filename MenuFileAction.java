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
 * File: MenuFileAction.java
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
//import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.String;
import javax.swing.*;
import javax.swing.filechooser.*;

//import javax.swing.event.*;

public class MenuFileAction extends ONL.UserAction //Mode.BaseAction
{
  public static interface Saveable
  {
    //public void saveToFile(String fName);
    //public void loadFromFile(String fName);
    public void saveToFile(java.io.File f);
    public void loadFromFile(java.io.File f);
  }
  
  private Saveable control;
  private boolean loading = false;//determines whether to call load file or save file
  private Component parentComponent = null;
  private JFileChooser fChooser = null;
  private String suffix = "";
  private ONLFileFilter fileFilter = null;
  protected static File currentDirectory = null;
  private File localDirectory = null;
  private boolean useLocalDir = false;
  

  public static class MFileFilter extends javax.swing.filechooser.FileFilter
  {
    private String suffix = "";
    public MFileFilter(String suf)
      {
    	this.suffix = suf;
      }
    public boolean accept(File pathname)
      {
    	//take this out for now because rename not working
    	//return (pathname.getAbsolutePath().endsWith(suffix));
    	return true;
      }
    public String getDescription() { return "All Files";}
  }

  public static class ONLFileFilter extends javax.swing.filechooser.FileFilter
  {
    public ONLFileFilter()
      {
        super();
      }
    public boolean accept(File f)
      {
	//take this out for now because rename not working
        if (f.equals(ExpCoordinator.getONLDir())) return true;
        return (!f.isHidden());
      }
    public String getDescription() { return "";}
  }


  public MenuFileAction(Saveable c, Component pc, String lbl)
    {
      this(c, false, pc, lbl, false, true);
    }
  public MenuFileAction(Saveable c, boolean ld, Component pc, String lbl, boolean ign, boolean exp)
    {
      super(lbl, ign, exp);
      control = c;
      loading = ld;
      parentComponent = pc;
      if (currentDirectory == null) currentDirectory = ExpCoordinator.getDefaultDir();
      fileFilter = new ONLFileFilter();
      //fChooser = new JFileChooser(new String("."));
    }

  public void setUseLocalDir(boolean b)
  {
    if (b && localDirectory == null) localDirectory = currentDirectory;
  }
  public void actionPerformed(ActionEvent e)
    {
      int rtn;
      ExpCoordinator.print(new String("MenuFileAction::actionPerformed currentDirectory " + currentDirectory), 4);
      if (localDirectory == null || !useLocalDir)
        fChooser = new JFileChooser(currentDirectory);
      else fChooser = new JFileChooser(localDirectory);
      fChooser.setFileHidingEnabled(false);
      fChooser.setFileFilter(fileFilter);
      //fChooser.rescanCurrentDirectory();
      if (loading)
      {
    	  rtn = fChooser.showOpenDialog(parentComponent);
    	  if (rtn == JFileChooser.APPROVE_OPTION) control.loadFromFile(fChooser.getSelectedFile());
      }
      else
      {
    	  rtn = fChooser.showSaveDialog(parentComponent);
    	  if (rtn == JFileChooser.APPROVE_OPTION) 
    	  {
    		  File file = fChooser.getSelectedFile();
    		  //System.out.println("MenuFileAction::actionPerformed save suffix = "  + suffix);
    		  //if (suffix.length() > 0)
    		  //{
    		  //  file.renameTo(new File(file.getAbsolutePath().concat(suffix)));
    		  //}
    		  control.saveToFile(file);
    	  }
      }
      if (useLocalDir)
        localDirectory = fChooser.getCurrentDirectory();
      else 
      {
    	  currentDirectory = fChooser.getCurrentDirectory();
    	  ExpCoordinator.setProperty(ExpCoordinator.DEFAULT_DIR, currentDirectory.getPath());
      }
      fChooser = null;
    }
  public void setSuffix(String str)
    {
      /*
      suffix = new String("." + str);
      fileFilter = new MFileFilter(str);
      if (fChooser != null) fChooser.setFileFilter(fileFilter);
      //System.out.println("MenuFileAction::setSuffix = "  + suffix);
      */
    }
  public void setSaveable(Saveable s) { control = s;}
  protected void setLocalDir(java.io.File f) { localDirectory = f;}
  protected boolean isLoading() { return loading;}
}
