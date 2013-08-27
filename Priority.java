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
 * File: Priority.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 6/13/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.undo.*;
import java.io.*;
import java.util.*;


public class Priority extends TextFieldwLabel
{
  public static final int DONT_SET = -1;
  public static final int INGRESS = 1;
  public static final int EGRESS = 0;
  public static final String PRIORITY = "priority";
  private Edit edit = null;
  //private FilterTable qtable = null;
  private PRTable prtable = null;
  private int committedPR = 60;
  private int ingress = DONT_SET;
  private int nccpOp = 0;
  private String nccpOpStr = "";
  
  
  public static interface PRTable
  {
    public RouterPort getPort();
    public void addOperation(NCCPOpManager.Operation op);
    public void removeOperation(NCCPOpManager.Operation op);
    public ONLComponent getONLComponent();
    public int size();
  }


  public static class NCCP_PRRequester extends NCCP.RequesterBase
  {
    private Priority priority = null;
    private NSPPort nspport;
    private int prSent = 0;
    private String op_str = "";
    
    public NCCP_PRRequester(int op, String o_str, Priority qt)
    {
      super(op);
      priority = qt;
      nspport = (NSPPort)priority.prtable.getPort();
      op_str = new String(o_str);
      setMarker(new REND_Marker_class());
    }
    
    public void storeData(DataOutputStream dout) throws IOException
    {
      prSent = priority.getPriority();
      dout.writeShort(nspport.getID());
      if (priority.getIngress() != DONT_SET) dout.writeByte(priority.getIngress());
      dout.writeInt(prSent);
      ExpCoordinator.printer.print(new String("Priority.NCCP_PRRequester.storeData " + op_str + " for port:" + nspport.getID() + "pr:" + prSent), 4);
    }
    
    public String getOpString() { return op_str;}
    public int getPRSent() { return prSent;}
  }
  
  private class Edit extends AbstractUndoableEdit implements ONLComponent.Undoable, ActionListener, NCCPOpManager.Operation, FocusListener
  {
    private Priority priority = null;
    private Experiment experiment = null;
    private  NCCP_PRRequester request = null;
    private int oldPR = 0;
    private int newPR = 0;
    private ExpCoordinator expCoordinator = null;
    private PRTable prtable = null;
    private ONLComponent onlComponent = null;
    
    public Edit(Priority pr)
    {
      super();
      priority = pr;
      oldPR = priority.getPriority();
      newPR = oldPR;
      prtable = priority.prtable;
      onlComponent = prtable.getONLComponent();
      expCoordinator = onlComponent.getExpCoordinator();
    }
    //FocusListener
    public void focusLost(FocusEvent e) 
      {
        if (prtable.size() > 0) actionPerformed(null);
      }
    public void focusGained(FocusEvent e) { }
    //ActionListener
    public void actionPerformed(ActionEvent e)
    {
      oldPR = newPR;
      newPR = priority.getPriority();
      if (oldPR == newPR) return;
      ExpCoordinator.print(new String("Priority.Edit.actionPerformed oldPR = " + oldPR + " newPR = " + newPR), 5);
      expCoordinator.addEdit(this);
      priority.updateColor();
      experiment = expCoordinator.getCurrentExp();
      onlComponent.setProperty(PRIORITY,newPR);
      if ((experiment != null) && !(experiment.containsParam(onlComponent))) experiment.addParam(onlComponent);	
    }
    //end ActionListener
    public void undo() throws CannotUndoException
    {
      newPR = priority.getPriority();
      priority.setValue(oldPR);
      //if (priority.qtable.size() <= 0 && (experiment != null)) experiment.removeParam(priority.qtable);
      priority.updateColor();
    }
    public void redo() throws CannotRedoException
    {
      //super.redo();
      priority.setValue(newPR);
      if ((experiment != null) && !(experiment.containsParam(onlComponent))) experiment.addParam(onlComponent);
      priority.updateColor();	
    }
    //ONLComponent.Undoable
    public void commit() 
    {
      ONLDaemon ec = priority.prtable.getPort().getONLDaemon(ONLDaemon.NSPC);
      
      if (ec != null) 
        {
          if (!ec.isConnected()) ec.connect();
          
          //ExpCoordinator.printer.print("Edit priority daemon " + ec.toString());
          request = new NCCP_PRRequester(nccpOp, nccpOpStr, priority);
          //ec.sendMessage(new NCCP_QueueRequester(NCCP.Operation_AddFIPLQueue, (NSPPort)getONLComponent(), queue));
          priority.prtable.addOperation(this);
          Experiment exp = expCoordinator.getCurrentExp();
          if ((exp != null) && !(exp.containsParam(onlComponent))) exp.addParam(onlComponent);
        }
      else System.err.println("AddQueue no daemon");
	priority.resetEdit();
	priority.updateColor();
    }
    public boolean isCancelled(ONLComponent.Undoable un){ return false;}
    //end ONLComponent.Undoable
    //NCCPOpManager.Operation
    public void opSucceeded(NCCP.ResponseBase resp) 
    {
      int pr = (int)resp.getData((MonitorDataType.Base)null);
      priority.committedPR = pr;
      priority.setValue(pr);
      priority.prtable.removeOperation(this);
      priority.updateColor();
    }
    public void opFailed(NCCP.ResponseBase resp)
    {
      if (request.getPRSent() == priority.getPriority())
        {
          priority.setValue(priority.committedPR);
        }
      priority.prtable.removeOperation(this);
      priority.updateColor();
    }
    public Vector getRequests() 
    { 
      Vector rtn = new Vector();
      rtn.add(request);
      return rtn;
    }
    public boolean containsRequest(REND_Marker_class rend) { return (request != null && request.getMarker().isEqual(rend));}
    public int getPRSent() 
    {
      if (request != null) return request.getPRSent();
      else return 0;
    }
    //end NCCPOpManager.Operation
  }//inner class Edit

  public Priority(String lbl, PRTable prt, int nccp_op, String nccp_op_str, boolean in) 
  { 
    this(lbl, prt, nccp_op, nccp_op_str);
    if (in) ingress = INGRESS;
    else ingress = EGRESS;
  }
  public Priority(String lbl, PRTable prt, int nccp_op, String nccp_op_str)
  {
    super(20, lbl);
    prtable = prt;
    setValue(committedPR);
    resetEdit();
    nccpOp = nccp_op;
    nccpOpStr = nccp_op_str;
    setPreferredSize(new Dimension(150,15));
    setMaximumSize(new Dimension(150,15));
    setAlignmentX(Component.LEFT_ALIGNMENT);
  }
  private void resetEdit()
  {
    if (edit != null) 
      {
        textField.removeActionListener(edit);
        textField.removeFocusListener(edit);
      }
    edit = new Edit(this);
    textField.addActionListener(edit);
    textField.addFocusListener(edit);
  }
  public int getPriority()
  {
    return (Integer.parseInt(getText()));
  }
  public void addEditForCommit()
  {
    edit.actionPerformed(null);
  }
  public void updateColor()
  {
    if (committedPR == Integer.parseInt(getText())) textField.setForeground(Color.black);
    else
      {
        if (edit != null && edit.getPRSent() == Integer.parseInt(getText())) textField.setForeground(Color.black);
        else textField.setForeground(Color.red);
      }
  }
  private int getIngress() { return ingress;}
}