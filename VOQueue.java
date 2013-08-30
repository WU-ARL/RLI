
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
 * File: VOQueue.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 8/11/2005
 *
 * Description:
 *
 * Modification History:
 *
 */
import java.lang.String;
import java.io.*;
import javax.swing.undo.*;
import javax.swing.*;
import java.lang.reflect.Array;
import java.util.*;
import java.text.ParseException;
import java.lang.NumberFormatException;
import java.awt.*;
import javax.swing.table.*;
import javax.xml.stream.*;

public class VOQueue extends Queue
{
  public static class RateCellRenderer extends DefaultTableCellRenderer
  {
    private NSPPort nspport = null;
    private Color uncommittedColor = null;
    public RateCellRenderer(NSPPort n) 
      { 
        this(n, Color.red);
      }
    public RateCellRenderer(NSPPort n, Color c) 
      { 
        super();
        uncommittedColor = c;
        nspport = n;
      }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
    {
      if (nspport != null && nspport.isDQon())
        {
          //ExpCoordinator.print("VOQueue.RateCellRenderer.getTableCellRendererComponent graying", 2);
          JTableHeader header = table.getTableHeader();
          if (header != null) 
            {
              setForeground(header.getBackground());
              setBackground(header.getBackground());
            }
          else
            {
              setForeground(Color.gray);
              setBackground(Color.gray);
            }
        }
      else
        {
          setBackground(table.getBackground());
          ONLTableCellRenderer.Committable row_elem = null;
          row_elem = ((ONLTableCellRenderer.CommittableTable)table.getModel()).getCommittableAt(row);
          if (row_elem != null)
            {
              //if (row_elem.isFailed())//strike through
              if (row_elem.needsCommit())
                {
                  setForeground(uncommittedColor);
                }
              else setForeground(null);
            }
          else System.out.println("ONLTableCellRenderer row_elem null for " + value.toString());
        }
      return (super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col));
    }
  }
  public static class QTable extends QueueTable
  {
    private VOQueue.UpdateEdit commonUpdateEdit = null;
    public QTable(NSPPort p) { super(p, ONLComponent.IPPQTABLE);}
    public void resetUpdateEdit()
      {
        commonUpdateEdit = new VOQueue.UpdateEdit(getPort(), this, expCoordinator.getCurrentExp());
      }
    public VOQueue.UpdateEdit getUpdateEdit() 
      { 
        if (commonUpdateEdit == null) resetUpdateEdit();
        return commonUpdateEdit;
      }
    
  }//end QTable

  public static class NCCP_VOQRequester extends NCCP.RequesterBase
  {
    private QueueTable queueTable;
    private NSPPort nspport;
    private int[] tholdSent;
    private int[] quantumSent;
    private String op_str = "voq change";
    
    public NCCP_VOQRequester(NSPPort p, QueueTable qt)
      {
	super(NCCP.Operation_ChangeVOQ, true);
	queueTable = qt;
        tholdSent = new int[queueTable.size()];
        quantumSent = new int[queueTable.size()];
	nspport = p;
	setMarker(new REND_Marker_class());
      }

      public void storeData(DataOutputStream dout) throws IOException
      {
        Queue queue = null;
        dout.writeShort(nspport.getID());
        int numQueues = queueTable.size();
        dout.writeInt(numQueues);
        for (int i = 0; i < numQueues; ++i)
          {
            queue = queueTable.getQueueAt(i);
            tholdSent[i] = queue.getThreshold();
            quantumSent[i] = queue.getQuantum();
            dout.writeInt(tholdSent[i]);
            dout.writeInt(quantumSent[i]);
          }
        ExpCoordinator.print(new String("NCCP_VOQRequester.storeData " + op_str + " for port:" + nspport.getID()), 3);
      }

    public Queue getQueueAt(int i) { return (queueTable.getQueueAt(i));}
    public Queue getQueue(int qid) { return (queueTable.getQueueAt(qid));}
    public String getOpString() { return op_str;}
    public int getQuantum(int i) { return quantumSent[i];}
    public int getThreshold(int i) { return tholdSent[i];}
  }


  public static class UpdateEdit extends Experiment.AddEdit implements NCCPOpManager.Operation, Queue.Edit //extends Queue.AddEdit
  {
    private int[] oldThold;
    private int[] newThold;
    private int[] oldQuantum;
    private int[] newQuantum;
    protected VOQueue.QTable queueTable = null;
    private NCCP_VOQRequester request = null;

    public UpdateEdit(NSPPort c, VOQueue.QTable qt, Experiment exp)
      {
	super(c,  exp);
        queueTable = qt;
        int s = qt.size();
        oldThold = new int[s];
        newThold = new int[s];
        oldQuantum = new int[s];
        newQuantum = new int[s];
      }
    public void undo() throws CannotUndoException
      {
        int max = queueTable.size();
        VOQueue queue;
        for (int i = 0; i < max; ++i)
          {
            if (oldQuantum[i] != newQuantum[i] || oldThold[i] != newThold[i])
              {
                queue = (VOQueue)queueTable.getQueueAt(i);
                newThold[i] = queue.getThreshold();
                newQuantum[i] = queue.getQuantum();
                queue.setQuantumValue(oldQuantum[i]);
                queue.setTholdValue(oldThold[i]);
                queueTable.changeQueue(queue);
              }
          }
      }
    public void redo() throws CannotRedoException
      {	
        int max = queueTable.size();
        VOQueue queue;
        for (int i = 0; i < max; ++i)
          {
            if (oldQuantum[i] != newQuantum[i] || oldThold[i] != newThold[i])
              {
                queue = (VOQueue)queueTable.getQueueAt(i);
                queue.setQuantumValue(newQuantum[i]);
                queue.setTholdValue(newThold[i]);
                queueTable.changeQueue(queue);
              }
          }
        ExpCoordinator.print("VOQueue.redo", 3);
	if ((getExperiment() != null) && !(getExperiment().containsParam(queueTable))) getExperiment().addParam(queueTable);
      }
    protected void sendMessage() 
      {
        ONLDaemon ec = ((NSPPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);

        if (ec != null) 
	  {
            NSPPort port = (NSPPort)getONLComponent();
            if (!ec.isConnected()) ec.connect();
            int max = queueTable.size();
            VOQueue queue;
            ExpCoordinator.print("VOQueue.UpdateEdit.sendMessage ", 4);
            request = new NCCP_VOQRequester(port, queueTable);
            ((VOQueue.QTable)queueTable).resetUpdateEdit();
            for (int i = 0; i < max; ++i)
              {
                queue = (VOQueue)queueTable.getQueue(i);
                //System.out.println("    updating queue " + queue.getQueueID() + " daemon " + ec.toString());
                queue.setProperty(COMMITTED, true);
                //ec.sendMessage(new NCCP_QueueRequester(NCCP.Operation_AddFIPLQueue, (NSPPort)getONLComponent(), queue));
                queue.resetUpdateEdit();
              }
            if (port.isDQon()) 
              {
                ExpCoordinator.print("VOQueue.UpdateEdit.sendMessage dq on", 4);
              }
            else 
              {
                ExpCoordinator.print("VOQueue.UpdateEdit.sendMessage dq off", 4);
              }
            queueTable.addOperation(this);
	  }
	else System.err.println("AddQueue no daemon");
      }
    protected void sendUndoMessage() {}
    public boolean isCancelled(ONLComponent.Undoable un)
      {
        //if (un instanceof QueueTable.DQOperation)
        //  {
        //   QueueTable.DQOperation edit = (QueueTable.DQOperation)un;
        //   if ((queueTable.getPort().getParent() == edit.getNSP()) && edit.isOn()) return true;
        //  }
        return false;
      }
    //Queue.Edit
    public boolean contains(Queue r)
      {
	return (queueTable.containsQueue(r));
      }
    //end Queue.Edit
    //NCCPOpManager.Operation
    public void opSucceeded(NCCP.ResponseBase resp)
      {
        if (request != null) 
          {
            Queue queue;
            int max = queueTable.size();
            for (int i = 0; i < max; ++i)
              {
                queue = queueTable.getQueueAt(i);
                queue.setProperty(COMMITTED_QUANTUM, request.getQuantum(i));
                queue.setProperty(COMMITTED_THOLD, request.getThreshold(i));
              }
            ExpCoordinator.print(new String("Queue.AddEdit.opSucceeded for voqs on port " + queueTable.getPort().getLabel()), 5);
	  }
        queueTable.removeOperation(this);
      }
    public void opFailed(NCCP.ResponseBase resp)
      {
	if (request != null) 
	  {
            Queue queue;
            int max = queueTable.size();
            for (int i = 0; i < max; ++i)
              {
                queue = queueTable.getQueueAt(i);
                if (request.getQuantum(i) == queue.getQuantum()) queue.setQuantum(queue.getPropertyInt(COMMITTED_QUANTUM));
                if (request.getThreshold(i) == queue.getThreshold()) queue.setThreshold(queue.getPropertyInt(COMMITTED_THOLD));
              }
            ExpCoordinator.print(new String("Queue.AddEdit.opFailed for voqs on port " + queueTable.getPort().getLabel()), 3);
	  }
	queueTable.removeOperation(this);
      }
    public Vector getRequests() 
      { 
        Vector rtn = new Vector();
        if (request != null) rtn.add(request);
        return rtn;
      }
    public boolean containsRequest(REND_Marker_class rend)
      {
        return (request != null && request.getMarker().isEqual(rend));
      }
    //end NCCPOpManager.Operation
  
    public void reset()
      {
        int max = queueTable.size();
        VOQueue queue;
        for (int i = 0; i < max; ++i)
          {
            queue = (VOQueue)queueTable.getQueueAt(i);
            oldThold[i] = queue.getThreshold();
            oldQuantum[i] = queue.getQuantum();
            newThold[i] = queue.getThreshold();
            newQuantum[i] = queue.getQuantum();
          }
	if ((getExperiment() != null) && !(getExperiment().containsParam(queueTable))) getExperiment().addParam(queueTable);
      }
  }//inner class UpdateEdit
    
  public VOQueue(Queue q, QueueTable qt)
    {
      super(q, qt);
    }
  public VOQueue(QueueTable qt) 
    {
      super(qt);
    }
  public VOQueue(int i, QueueTable qt)  
    {
      super(i, qt);
    }

  public VOQueue(String s, QueueTable qt) throws java.text.ParseException
    {
      super(s, qt);
    }
  protected void resetUpdateEdit()
    {
      //ExpCoordinator.print("VOQueue.resetUpdateEdit", 5);
      if (queueTable != null) updateEdit = ((VOQueue.QTable)queueTable).getUpdateEdit();
      else 
        ExpCoordinator.print("   queueTable is null", 2);
    } 
  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
  {
    xmlWrtr.writeStartElement(ExperimentXML.VOQ);
    xmlWrtr.writeStartElement(ExperimentXML.QID);
    xmlWrtr.writeCharacters(String.valueOf(getQueueID()));
    xmlWrtr.writeEndElement();
    xmlWrtr.writeStartElement(ExperimentXML.QUANTUM);
    xmlWrtr.writeCharacters(String.valueOf(getQuantum()));
    xmlWrtr.writeEndElement();
    xmlWrtr.writeStartElement(ExperimentXML.RATE);
    xmlWrtr.writeCharacters(String.valueOf(getThreshold()));
    xmlWrtr.writeEndElement();
    xmlWrtr.writeEndElement();
  }
}// end class VOQueue

