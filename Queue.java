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
 * File: Queue.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/25/2004
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
import javax.xml.stream.*;

public class Queue implements ONLCTable.Element 
{
  public static final String DEFAULT_STR = "default";
  public static final String DATAGRAM_STR = "datagram"; 
  public static final int DEFAULT = 0; 
  public static final int DATAGRAM = 1;
  public static final int DEFAULT_INTHOLD = 1000000;
  public static final int DEFAULT_OUTTHOLD = 32000;
  public static final int DEFAULT_QUANTUM = 2048;
  public static final int DEFAULT_VOQRATE = 0; //in Kbps

  //properties
  private ONLPropertyList properties = null;
  public static final String COMMITTED = "committed"; //boolean
  public static final String COMMITTED_QUANTUM = "cquantum"; //int
  public static final String COMMITTED_THOLD = "cthold"; //int
  public static final String VOQ = "voq"; //boolean
  protected static final String LENGTH_STRING = "length";

  //field indices or table column
  protected static final int QUEUE = 0;
  protected static final int THRESHOLD = 1;
  protected static final int QUANTUM = 2;
  protected static final int LENGTH = 3;
  
  private ID qid = null;
  private int threshold = DEFAULT_OUTTHOLD;
  private int quantum = DEFAULT_QUANTUM;
  //private int type = DEFAULT;
  private double length = 0;
  protected Queue.Edit updateEdit = null;
  protected QueueTable queueTable = null;
  
  public static class TholdRange extends ONLCTable.IntRange
  {
    public TholdRange() { this(DEFAULT_OUTTHOLD);}
    public TholdRange(int i) { super(i,1,16000000,DEFAULT_OUTTHOLD);}
    public TholdRange(String s) throws java.text.ParseException
    {
      super(s,1,16000000,DEFAULT_OUTTHOLD);
    }
  }

  public static class ID 
  {
    private String value = DEFAULT_STR;
    public ID(){}
        
    public ID(ID i)
      {
	value = new String(i.value);
      }
    public ID(String s) throws java.text.ParseException
      {
	this(s, false);
      }
    public ID(String s, boolean is_voq) throws java.text.ParseException
      {
	parseString(s, is_voq);
      }

    protected void parseString(String s, boolean is_voq) throws java.text.ParseException
      {
	value = new String(s);
	if (s.equals(DEFAULT_STR) || s.equals(DATAGRAM_STR)) ;
	else
	  {
	    int q = Integer.parseInt(s);
	    if (is_voq)
	      {
		if (q > 7 || q < 0) 
		  throw new java.text.ParseException("QueueTable.Queue parse error: not valid voq must be 0-7", 0);
	      }
	    else
	      {
		if (q < QueueTable.Q_START || q >= QueueTable.DATAGRAM_START)
		  throw new java.text.ParseException("QueueTable.Queue parse error: not valid queue id must be 8-503", 0);
	      }
	  }
      }
    public String toString() { return (new String(value));}
    public boolean equals(String s) { return (s.equals(value));}
    public boolean equals(ID i) { return (value.equals(i.value));}
    public int getIntValue() { return (getIntValue(false));}
    public int getIntValue(boolean is_voq) 
      {
	if (value.equals(DEFAULT_STR)) return DEFAULT;
	if (value.equals(DATAGRAM_STR)) return DATAGRAM;
	int q = Integer.parseInt(value);
	//if (is_voq)
	// q += QueueTable.VOQ_START;
	return q;
      }
    protected void setValue(String s) { value = s;}
    public boolean isDefault() { return  (getIntValue() == DEFAULT);}
    public boolean isSPCBound() { return (getIntValue() >= QueueTable.Q_START && getIntValue() < QueueTable.FPX_START);}
  }//end class Queue.ID
  
  public static class VOQID extends ID
  {
      public VOQID() 
      { 
	  super();
	  setValue(String.valueOf(0));
      }
    public VOQID(ID voq)
      {
	super(voq);
      }
    public VOQID(VOQID voq)
      {
	super((ID)voq);
      }
    public VOQID(String s) throws java.text.ParseException
      {
	super(s, true);
      }
    public int getIntValue() { return (getIntValue(true));}
  }//end class Queue.VOQID
  



  public static class NCCP_QueueRequester extends NCCP.RequesterBase
  {
    private Queue queue;
    private NSPPort nspport;
    private int tholdSent = 0;
    private int quantumSent = 0;
    private String op_str = "";
    
    public NCCP_QueueRequester(int op, NSPPort p, Queue r)
      {
	super(op);
	queue = r;
	nspport = p;
	
	switch(getOp())
	  {
	  case NCCP.Operation_ChangeQueue:
	    op_str = "queue change";
	    break;
	  case NCCP.Operation_ChangeVOQ:
	    op_str = "voq change";
	    break;
	  }
	setMarker(new REND_Marker_class());
      }

      public void storeData(DataOutputStream dout) throws IOException
      {
	  tholdSent = queue.getThreshold();
	  quantumSent = queue.getQuantum();
	  dout.writeShort(nspport.getID());
	  dout.writeInt(queue.getQueueID());
	  dout.writeInt(tholdSent);
	  dout.writeInt(quantumSent);
	  System.out.println("NCCP_QueueRequester.storeData " + op_str + " for port:" + nspport.getID() + "qid:" + queue.qid + " " + op_str + " threshold:" + tholdSent + " quantum:" + quantumSent);
      }

    public Queue getQueue() { return queue;}
    public String getOpString() { return op_str;}
    public int getQuantum() { return quantumSent;}
    public int getThreshold() { return tholdSent;}
  }
  
        
  public static interface Edit extends ONLComponent.Undoable
  {
    public boolean contains(Queue q);
    public ONLComponent getONLComponent();
    public void reset();
  }

  public static class AddEdit extends Experiment.AddEdit implements NCCPOpManager.Operation, Queue.Edit
  {
    private Vector queues = null;
    private Vector cancelled = null;
    protected QueueTable queueTable = null;
    protected static final String AddClassName = "Queue$AddEdit";
    protected static final String UpdateClassName = "Queue$UpdateEdit";
    private Vector requests = null;

    public AddEdit(NSPPort c, Queue r, QueueTable rt, Experiment exp)
      {
	this(c, new Vector(), rt, exp);
	queues.add(r);
      }

    public AddEdit(NSPPort c, Vector r, QueueTable rt, Experiment exp)
      {
	super(c, exp);
	queueTable = rt;
	queues = r;
	cancelled = new Vector();
        //System.out.println("Queue.AddEdit " + getClass().getName());
	requests = new Vector();
      }
    public void undo() throws CannotUndoException
      {
	  //super.undo();
	int max = queues.size();
	for (int i = 0; i < max; ++i)
	  {
	    queueTable.removeQueue((Queue)queues.elementAt(i));
	  }
	if (queueTable.size() <= 0 && (getExperiment() != null)) getExperiment().removeParam(queueTable);
      }
    public void redo() throws CannotRedoException
      {
	//super.redo();
	int max = queues.size();
	for (int i = 0; i < max; ++i)
	  {
	    queueTable.addQueue((Queue)queues.elementAt(i));
	  }
	if ((getExperiment() != null) && !(getExperiment().containsParam(queueTable))) getExperiment().addParam(queueTable);	
      }
    protected void sendMessage() 
      {
        ONLDaemon ec = ((NSPPort)getONLComponent()).getONLDaemon(ONLDaemon.NSPC);

        if (ec != null) 
	  {
	      if (!ec.isConnected()) ec.connect();
	      int max = queues.size();
              Queue queue;
	      //System.out.println("Add/UpdateEdit ");
              int op = NCCP.Operation_ChangeQueue;
              if (queueTable.isIngress()) op = NCCP.Operation_ChangeVOQ;
	      for (int i = 0; i < max; ++i)
		{
                  queue = (Queue)queues.elementAt(i);
	          //System.out.println("    updating queue " + queue.getQueueID() + " daemon " + ec.toString());
		  queue.properties.setProperty(COMMITTED, true);
                  queueTable.fireCommitEvent(queue);
		  requests.add(new NCCP_QueueRequester(op, (NSPPort)getONLComponent(), queue));
		  //ec.sendMessage(new NCCP_QueueRequester(NCCP.Operation_AddFIPLQueue, (NSPPort)getONLComponent(), queue));
		  queue.resetUpdateEdit();
		}
	      if (max > 0) queueTable.addOperation(this);
	  }
	else System.err.println("AddQueue no daemon");
      }
    protected void sendUndoMessage() {}
    public boolean isCancelled(ONLComponent.Undoable un)
      {
	  //if (Queue.DeleteEdit.isQDeleteEdit(un) || 
	  //(Queue.AddEdit.isQUpdateEdit(this) &&Queue.AddEdit.isQAddEdit(un)))//fix should check if is of type ONLComponent.Undoable or whatever i'm looking for
	if (Queue.AddEdit.isQUpdateEdit(this) && Queue.AddEdit.isQAddEdit(un))
	  {
	    Queue.Edit edit = (Queue.Edit) un;
	    if (onlComponent == edit.getONLComponent()) 
	      {
		int max = queues.size();
		Vector removals = new Vector();
		int i;
		Queue elem;
		for (i = 0; i < max; ++i)
		  {
		    elem = (Queue)queues.elementAt(i);
		    if (edit.contains(elem))
		      removals.add(elem);
		  }
		max = removals.size();
		for (i = 0; i < max; ++i)
		  {
		    elem = (Queue)removals.elementAt(i);
		    queues.remove(elem);
		    cancelled.add(elem);
		  }
	      }
	    if (queues.isEmpty()) return true;
	  }
	return false;
      }
    protected static boolean isQAddEdit(ONLComponent.Undoable un)
      {
        String nm = un.getClass().getName();
        return (nm.equals(AddClassName));
      }
    protected static boolean isQUpdateEdit(ONLComponent.Undoable un)
      {
        String nm = un.getClass().getName();
        return (nm.equals(UpdateClassName));
      }
    public Vector getQueues() { return queues;}
    //Queue.Edit
    public boolean contains(Queue r)
      {
	return (queues.contains(r) || cancelled.contains(r));
      }
    //end Queue.Edit
    //NCCPOpManager.Operation
    public void opSucceeded(NCCP.ResponseBase resp)
      {
	NCCP_QueueRequester req = getRequest(resp.getRendMarker());
	Queue queue = req.getQueue();
	if (req != null) 
	  {
	    queue.properties.setProperty(COMMITTED_QUANTUM, req.getQuantum());
	    queue.properties.setProperty(COMMITTED_THOLD, req.getThreshold());
	    requests.remove(req);
	    ExpCoordinator.print(new String("Queue.AddEdit.opSucceeded for queue " + queue.getQueueID() + " port " + queueTable.getPort().getID()), 5);
	  }
	if (requests.isEmpty()) queueTable.removeOperation(this);
      }
    public void opFailed(NCCP.ResponseBase resp)
      {
	NCCP_QueueRequester req = getRequest(resp.getRendMarker());
	Queue queue = req.getQueue();
	if (req != null) 
	  {
	    ExpCoordinator.print(new String("Queue.AddEdit.opFailed for queue " + queue.getQueueID() + " port " + queueTable.getPort().getID()), 3);
	    if (req.getQuantum() == queue.getQuantum()) queue.setQuantum(queue.properties.getPropertyInt(COMMITTED_QUANTUM));
	    if (req.getThreshold() == queue.getThreshold()) queue.setThreshold(queue.properties.getPropertyInt(COMMITTED_THOLD));
	    requests.remove(req);
	  }
	if (requests.isEmpty()) queueTable.removeOperation(this);
      }
    public Vector getRequests() { return requests;}
    public boolean containsRequest(REND_Marker_class rend)
      {
	if (getRequest(rend) != null) return true;
	return false;
      }
    //end NCCPOpManager.Operation
    private NCCP_QueueRequester getRequest(REND_Marker_class rend)
      {
	int max = requests.size();
	NCCP_QueueRequester req = null;
	//System.out.println("Queue.AddEdit numReqs = " + max + " looking for");
	//rend.print();
	for (int i = 0; i < max; ++i)
	  {
	    req = (NCCP_QueueRequester)requests.elementAt(i);
	    //System.out.println("  elem " + i);
	    //req.getMarker().print();
	    if (req.getMarker().isEqual(rend)) return req;
	  }
	return null;
      }
    
    public void reset(){};
  }//inner class AddEdit


  public static class UpdateEdit extends AddEdit 
  {
    private Queue queue = null;
    private int oldThold = 0;
    private int newThold = 0;
    private int oldQuantum = 0;
    private int newQuantum = 0;

    public UpdateEdit(NSPPort c, Queue r, QueueTable rt, Experiment exp)
      {
	super(c, r, rt, exp);
	queue = r;
      }
    public void undo() throws CannotUndoException
      {
	newThold = queue.getThreshold();
	newQuantum = queue.getQuantum();
	queue.quantum = oldQuantum;
	queue.threshold = oldThold;
	queueTable.changeQueue(queue);
      }
    public void redo() throws CannotRedoException
      {	
	queue.quantum = newQuantum;
	queue.threshold = newThold;
	queueTable.changeQueue(queue);
	if ((getExperiment() != null) && !(getExperiment().containsParam(queueTable))) getExperiment().addParam(queueTable);
      }
    public void reset()
      {
	oldThold = queue.getThreshold();
	oldQuantum = queue.getQuantum();
	newThold = queue.getThreshold();
	newQuantum = queue.getQuantum();
	if ((getExperiment() != null) && !(getExperiment().containsParam(queueTable))) getExperiment().addParam(queueTable);
      }
  }//inner class UpdateEdit

  public Queue(Queue q, QueueTable qt)
    {
      threshold = q.threshold;
      queueTable = qt;
      quantum = q.quantum;
      //type = q.type;
      properties = new ONLPropertyList(this);
      properties.setProperty(VOQ, qt.isIngress());
      if (isVOQ()) 
	{
	  qid = new VOQID(q.qid);
          quantum = DEFAULT_VOQRATE;
	  properties.setProperty(COMMITTED_THOLD, DEFAULT_INTHOLD);
	}
      else 
	{
	  qid = new ID(q.qid);
	  properties.setProperty(COMMITTED_THOLD, DEFAULT_OUTTHOLD);
	}
      properties.setProperty(COMMITTED, false);
      properties.setProperty(COMMITTED_QUANTUM, DEFAULT_QUANTUM);
    }
  public Queue(QueueTable qt) 
    {
      try
	{
          queueTable = qt;
          int tmp_q = QueueTable.Q_START;
          if (qt.isIngress()) tmp_q = 0;
	  initQueue(String.valueOf(tmp_q), qt);
	}
      catch (java.text.ParseException e) {}
    }
  public Queue(int i, QueueTable qt)  
    {
      try
	{
          queueTable = qt;
	  initQueue(String.valueOf(i), qt);
	}
      catch (java.text.ParseException e) {}
    }

  public Queue(String s, QueueTable qt) throws java.text.ParseException
    {
      queueTable = qt;
      String[] strarray = s.split(" "); //need new java to use this
      int len = Array.getLength(strarray);
      initQueue(strarray[0], qt);
      if (len > 2)
	  {
            quantum = Integer.parseInt(strarray[1]);
            threshold = Integer.parseInt(strarray[2]);
	  }
    }
  public void parseString(String s) throws java.text.ParseException
    {
      String[] strarray = s.split(" "); //need new java to use this
      int len = Array.getLength(strarray);
      initQueue(strarray[0], queueTable);
      if (len > 2)
	  {
            quantum = Integer.parseInt(strarray[1]);
            threshold = Integer.parseInt(strarray[2]);
	  }
    }
  private void initQueue(String s, QueueTable qt) throws java.text.ParseException
    {
      properties = new ONLPropertyList(this);
      properties.setProperty(VOQ, qt.isIngress());
      if (isVOQ()) 
	{
	  qid = new VOQID(s);
          if (ExpCoordinator.isTestLevel(ExpCoordinator.DQ)) quantum = DEFAULT_VOQRATE;
	  threshold = DEFAULT_INTHOLD;
	}
      else qid = new ID(s);
      properties = new ONLPropertyList(this);
      properties.setProperty(COMMITTED, false);
      properties.setProperty(COMMITTED_QUANTUM, quantum);
      properties.setProperty(COMMITTED_THOLD, threshold);
    }
  public int getThreshold() { return threshold;}
  public void setThreshold(int t) 
    {
      if (updateEdit == null) resetUpdateEdit();
      updateEdit.reset();
      threshold = t;
      if (ExpCoordinator.isRecording())
        {
          if (queueTable.isIngress())
            ExpCoordinator.recordEvent(SessionRecorder.VOQ_THOLD, new String[]{queueTable.getPort().getLabel(), String.valueOf(getQueueID()), String.valueOf(t)});
          else
            ExpCoordinator.recordEvent(SessionRecorder.THOLD_EGRESS_Q, new String[]{queueTable.getPort().getLabel(), String.valueOf(getQueueID()), String.valueOf(t)}); 
        }
    } 
  public int getQuantum() { return quantum;}
  public void setQuantum(int q) 
    { 
      if (updateEdit == null) resetUpdateEdit();
      updateEdit.reset();
      quantum = q;
      if (ExpCoordinator.isRecording())
        {
          if (queueTable.isIngress())
            ExpCoordinator.recordEvent(SessionRecorder.VOQ_RATES, new String[]{queueTable.getPort().getLabel(), String.valueOf(getQueueID()), String.valueOf(q)});
          else
            ExpCoordinator.recordEvent(SessionRecorder.QUANTUM_EGRESS_Q, new String[]{queueTable.getPort().getLabel(), String.valueOf(getQueueID()), String.valueOf(q)}); 
        }
    }
  public Queue.ID getQueueLabel() { return qid;}
  public void setQueueID(String s)
   {
      try
	{
	  if (isVOQ())
	    qid = new VOQID(s);
	  else 
            {
              qid = new ID(s);
              if (ExpCoordinator.isRecording())
                ExpCoordinator.recordEvent(SessionRecorder.Q_ID_EGRESS_Q, new String[]{queueTable.getPort().getLabel(), s}); 
            }
	}
      catch (java.text.ParseException e) {}
    }
  public boolean isVOQ() { return (properties.getPropertyBool(VOQ));}
  public int getQueueID() { return (qid.getIntValue());}
  public double getLength() { return length;}
  public void setLength(double l) { length = l;}
  public boolean isEditable()
    {
      if (isVOQ() || qid.equals(DEFAULT_STR) || qid.equals(DATAGRAM_STR) || isCommitted()) return false;
      else return true;
    }
  protected void setCommitted()
    {
      properties.setProperty(COMMITTED, true);
      properties.setProperty(COMMITTED_THOLD, getThreshold());
      properties.setProperty(COMMITTED_QUANTUM, getQuantum());
    }
  public boolean isCommitted() { return (properties.getPropertyBool(COMMITTED));}
  public ONLComponent.Undoable getUpdateEdit() 
    { 
      if (updateEdit == null) resetUpdateEdit();
      return updateEdit;
    }
  protected void resetUpdateEdit()
   {
     if (queueTable != null) updateEdit = new UpdateEdit(queueTable.getPort(), this, queueTable, queueTable.getExpCoordinator().getCurrentExp());
   }

  //interface ONLCTable.Element
  public boolean isWritable() { return true;}
  public String toString()
   {
     String rtn = new String(qid.toString() + " " + quantum + " " + threshold);
     return rtn;
   } 
  public void write(ONL.Writer wrtr) throws java.io.IOException { wrtr.writeString(toString());} 
  public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
  {
    xmlWrtr.writeStartElement(ExperimentXML.QUEUE);//"queue");
    xmlWrtr.writeStartElement(ExperimentXML.QID);
    xmlWrtr.writeCharacters(qid.toString());
    xmlWrtr.writeEndElement();
    xmlWrtr.writeStartElement(ExperimentXML.QUANTUM);//"quantum");
    xmlWrtr.writeCharacters(String.valueOf(quantum));
    xmlWrtr.writeEndElement();
    xmlWrtr.writeStartElement(ExperimentXML.THOLD);//"threshold");
    xmlWrtr.writeCharacters(String.valueOf(threshold));
    xmlWrtr.writeEndElement();
    xmlWrtr.writeEndElement();
  }
  public ONLComponent getTable() { return queueTable;}
  //begin interface ONLTableCellRenderer.Committable
  public boolean needsCommit()
    {
      if (!properties.getPropertyBool(COMMITTED) ||
	  properties.getProperty(COMMITTED_QUANTUM) == null ||
	  properties.getProperty(COMMITTED_THOLD) == null ) return true;
      return (properties.getPropertyInt(COMMITTED_QUANTUM) != getQuantum() || properties.getPropertyInt(COMMITTED_THOLD) !=  getThreshold());
    }
  //end interface ONLTableCellRenderer.Committable
  public void setCommitted(boolean b)
  {
    if (b) setCommitted();
  }
  public Object getDisplayField(int c)
  {
    switch(c)
      {
      case QUEUE:
        return (getQueueLabel().toString());
      case THRESHOLD:
        return (new Queue.TholdRange(getThreshold()));
      case QUANTUM:
        if (ExpCoordinator.isTestLevel(ExpCoordinator.DQ) && queueTable.isIngress())
          return (new Double((double)(getQuantum()/1000)));
        else
          return (new Integer(getQuantum()));
      case LENGTH:
        return (new Integer((int)getLength()));
      }
    return null;
  }
  public boolean isEditable(int c)
  {
    switch(c)
      {  
      case QUEUE:
        return (isEditable());
      case THRESHOLD:
        return true;
      case QUANTUM:
        if (queueTable.getPort().isDQon() && queueTable.isIngress()) return false;
        else
          return true;
      case LENGTH: 
        return false;
      default:
        return false;
      }
  }

  public void setField(int c, Object a)
  {
    ExpCoordinator ec = ExpCoordinator.theCoordinator;
    switch(c)
      { 
      case QUEUE:
        setQueueID((String)a);
        break;
      case THRESHOLD:
        setThreshold(Integer.parseInt((String)a));
        ec.addEdit(getUpdateEdit());
        break;
      case QUANTUM:
        ec.print(new String("QT.setValue " + a.getClass()), 2);
        if (ec.isTestLevel(ExpCoordinator.DQ) && queueTable.isIngress())
          {
            int v = (int)(((Double)a).doubleValue() * 1000);
            setQuantum(v);
            //resetSwitchBW();
          }
        else
          {
            if (a instanceof Double) //expCoordinator.isTestLevel(ExpCoordinator.DQ) && ingress)
              setQuantum((int)((Double)a).doubleValue());
            else
              setQuantum(((Integer)a).intValue());
          }
        ec.addEdit(getUpdateEdit());
	  break;
      }
  }
  public void setState(String s) { properties.setProperty(ONLComponent.STATE, s);}
  public String getState() { return (properties.getProperty(ONLComponent.STATE));}
  //end interface ONLCTable.Element

  protected void setTholdValue(int t) { threshold = t;}
  protected void setQuantumValue(int q) { quantum = q;}
  public void setProperty(String nm, int o) { properties.setProperty(nm, o);}
  public void setProperty(String nm, boolean o) { properties.setProperty(nm, o);}
  public String getProperty(String nm) { return (properties.getProperty(nm));}
  public int getPropertyInt(String nm) { return (properties.getPropertyInt(nm));}
  public double getPropertyDouble(String nm) { return (properties.getPropertyDouble(nm));}
  public boolean getPropertyBool(String nm) { return (properties.getPropertyBool(nm));}
}// end class QUEUE

