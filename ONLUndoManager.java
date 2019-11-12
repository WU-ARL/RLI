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
 * File: ONLUndoManager.java
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
import javax.swing.undo.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.String;
import java.util.Vector;
import java.io.*;

public class ONLUndoManager extends UndoManager implements NCCPConnection.ConnectionListener
{
	private UndoAction undoAction = null;
	private SubUndoManager uncommittedEdits = null;
	private CommitAction commitAction = null;
	private ExpCoordinator expCoordinator = null;
	private PendingEdits pendingEdits = null;
	private Class aggregateClass = null;

	public static class AggregateEdit extends CompoundEdit implements ONLComponent.Undoable
	{
		private boolean committed = false;
		public AggregateEdit() { super();}
		public void commit()
		{
			int max = edits.size();
			for (int i = 0; i < max; ++i)
			{
				ONLComponent.Undoable elem = (ONLComponent.Undoable)edits.elementAt(i);
				elem.commit();
			}
			committed = true;
			end();
		}
		public void actionPerformed(ActionEvent e)
		{
			int max = edits.size();
			for (int i = 0; i < max; ++i)
			{
				ONLComponent.Undoable elem = (ONLComponent.Undoable)edits.elementAt(i);
				elem.actionPerformed(e);
			}
		}
		public void undo() throws CannotUndoException
		{
			//super.undo();
			int max = edits.size();
			for (int i = 0; i < max; ++i)
			{
				ONLComponent.Undoable elem = (ONLComponent.Undoable)edits.elementAt(i);
				elem.undo();
			}
		}
		public boolean isCancelled(ONLComponent.Undoable un)
		{
			if (edits.isEmpty()) return true;
			Vector tmp_edits = new Vector(edits);
			int max = tmp_edits.size();
			Vector aggEdits = null;
			AggregateEdit aggEdit = new AggregateEdit();
			if (aggEdit.getClass().isInstance(un)) 
			{
				aggEdit = (AggregateEdit)un;
				aggEdits = aggEdit.getEditsCopy();
			}
			//ExpCoordinator.printer.print(new String("Aggregate Edit isCancelled for " + un.getClass().getName()), 4);
			for (int i = 0; i < max; ++i)
			{
				ONLComponent.Undoable elem = (ONLComponent.Undoable)tmp_edits.elementAt(i);
				if (aggEdits == null && elem.isCancelled(un)) 
				{
					edits.remove(elem);
					//ExpCoordinator.printer.print(new String("   removing " + elem.getClass().getName()), 4);
				}
				if (aggEdits != null)
				{
					int max2 = aggEdits.size();
					ONLComponent.Undoable elem2;
					for (int i2 = 0; i2 < max2; ++i2)
					{
						elem2 = (ONLComponent.Undoable)aggEdits.elementAt(i2);
						if (elem.isCancelled(elem2))
						{
							edits.remove(elem);
							//ExpCoordinator.printer.print(new String("   removing " + elem.getClass().getName()), 4);
							break;
						}
					}
					aggEdit.isCancelled(elem);
				}
			}
			return (edits.isEmpty());
		}
		public void redo() throws CannotRedoException
		{
			//super.redo();
			int max = edits.size();
			for (int i = 0; i < max; ++i)
			{
				ONLComponent.Undoable elem = (ONLComponent.Undoable)edits.elementAt(i);
				elem.redo();
			}
		}
		public boolean isEmpty() { return (edits.isEmpty());}
		protected Vector getEditsCopy() { return (new Vector(edits));}
		protected boolean isCommitted() { return committed;}
		protected void setCommitted(boolean b) { committed = b;}
	}


	private class PendingEdits extends Vector
	{
		public PendingEdits() { super();}
		public void doNext() 
		{
			if (!isEmpty()) 
			{
				SubUndoManager sum = (SubUndoManager)remove(0);
				sum.end();
			}
		}
	}

	public static class NCCP_Requester extends ExpRequest.RequesterBase
	{
		private Experiment experiment = null;
		private ExpCoordinator expCoordinator = null;

		public NCCP_Requester(Experiment e) 
		{ 
			this(e,ExpDaemon.NCCP_Operation_ExperimentRequest);
			setMarker(new REND_Marker_class());
		}

		public NCCP_Requester(Experiment e, int o)
		{
			super(o, true);
			experiment = e;
			expCoordinator = ExpCoordinator.theCoordinator;
			ExpCoordinator.printer.print(new String("ONLUndoManager.NCCP_Requester op " + op), 2);
		}

		public void storeData(DataOutputStream dout) throws IOException
		{
			ExpCoordinator.print(new String("ONLUndoManager.NCCP_Requester.storeData op " + op), 6);
			NCCP.writeString("", dout);
			NCCP.writeString(experiment.getProperty(Experiment.LABEL), dout);
			NCCP.writeString(expCoordinator.getProperty(ExpCoordinator.USERNAME), dout);
			NCCP.writeString(expCoordinator.getProperty(ExpCoordinator.PASSWORD), dout); 
			if (op == ExpDaemon.NCCP_Operation_ExperimentRequest); 
			dout.writeInt(experiment.getNumCfgObjs());
			//ExpCoordinator.print(new String("ONLUndoManager.NCCP_Requester.storeData uname:" + expCoordinator.getProperty(ExpCoordinator.USERNAME) + " password:" + expCoordinator.getProperty(ExpCoordinator.PASSWORD)), 6);

			//ONL.NCCPWriter nccp_writer = new ONL.NCCPWriter(dout);
			//experiment.writeTopologySummary(nccp_writer);
		}
	}//end inner class ONLUndoManager.NCCP_Requester


	public static class NCCP_Response extends NCCP.ResponseBase
	{
		private String expID = "";
		private String msg = "";
		private int errorCode = 0;
		public NCCP_Response() 
		{
			super(0);
		}
		public void retrieveData(DataInput din) throws IOException
		{     
			expID = NCCP.readString(din);
			msg = NCCP.readString(din);
			errorCode = din.readInt();
			ExpCoordinator.printer.print(new String("ONLUndoManager.NCCP_Response.retrieveData msg: " + msg + " errorCode:" + errorCode), 2);
		}
		public String getExperimentID() { return expID;}
		public void retrieveFieldData(DataInput din) throws IOException {}
		public double getData(MonitorDataType.Base mdt) { return 0;}
		public String getMessage() { return msg;}
		public int getErrorCode() { return errorCode;}
	}//end inner class NCCP_Response

	private class NCCP_ExperimentReq extends ExpRequest
	{
		private Experiment experiment;
		private SubUndoManager undoManager;
		private Observer.ObserveRequest.NCCP_Response observeResponse = null;

		public NCCP_ExperimentReq(Experiment e, SubUndoManager um)
		{
			super();
			experiment = e;
			//ExpCoordinator.print(new String("NCCP_ExperimentReq.NCCP_ExperimentReq #edits = " + um.size()), 4);
			request = new NCCP_Requester(e);
			response = new NCCP_Response();
			observeResponse = new Observer.ObserveRequest.NCCP_Response();
			undoManager = um;
		}
		public void retrieveData(NCCP.ResponseHeader hdr, DataInput din) throws IOException
		{
			if (hdr.getOp() == ExpDaemon.NCCP_Operation_ObserveExpReq)
			{
				observeResponse.setHeader(hdr); //set header info
				observeResponse.retrieveData(din); //get op specific return data
				processResponse(observeResponse);
			}
			else super.retrieveData(hdr, din);
		}
		public void processResponse(NCCP.ResponseBase r)
		{
			//System.out.println("NCCP_ExperimentReq.processResponse status = " + r.getStatus());
			//ExpCoordinator.print(new String("NCCP_ExperimentReq.processResponse #edits = " + undoManager.size()), 4);
			ExpCoordinator expc = ExpCoordinator.theCoordinator;

			//if (r == observeResponse)
			//{
			boolean show_error = true;
			if (r.getOp() == ExpDaemon.NCCP_Operation_ObserveExpReq)
			{
				Observer.ObserveRequest.NCCP_Response oresp = (Observer.ObserveRequest.NCCP_Response)r;
				//inform user of observation request
				Object[] options = {"Invite", "Reject"};
				Observer.ObserveData od = oresp.getObserveData();

				ExpCoordinator.print(new String("ONLUndoManager.processResponse. observe request odata  " + od.toString()), 5);
				Object[] paramObjects = {new String(od.oname + " wants to observe your experiment.")};
				int rtn = JOptionPane.showOptionDialog(ExpCoordinator.getMainWindow(), 
						paramObjects, 
						"Observation Request", 
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, 
						null,
						options,
						options[0]);
				//if user says yes launch observation window
				boolean accept = (rtn == JOptionPane.YES_OPTION);
				if (accept) Observer.start();
				//send response
				expc.sendRequest(new Observer.ObserveRequest(experiment.getID(),od.oname, accept));
				return;
			}
			if (r.getStatus() == NCCP.Status_Fine) 
			{
				experiment.setProperty(ONLComponent.STATE, ONLComponent.ACTIVE);
				experiment.setProperty(Experiment.ONLID, ((NCCP_Response)r).getExperimentID());
				undoManager.end();
			}
			else //failed, set to uninitialized so we can try again later, inform the user
			{
				//experiment.setProperty(ONLComponent.STATE, ONLComponent.NOT_INIT);
				//now tell the user
				String msg = null;
				boolean resend = false;
				boolean commit_error = false;
				msg = ((NCCP_Response)r).getMessage();
				switch(((NCCP_Response)r).getErrorCode())
				{
				case Reservation.EXPIRED:
					if (expc.getCurrentExp() == experiment) 
					{
						expc.closeCurrentExp();
						String tmp_str = "Experiment Closing. Reservation Expired.";
						expc.addError(new StatusDisplay.Error(tmp_str, 
								tmp_str,
								"Experiment Terminated", 
								(StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR)));
					}
					expc.removeRequest(this);
					break;
				case Reservation.ALERT:
					String opt = "OK";
					int rtn = JOptionPane.showOptionDialog(expc.getMainWindow(), 
							new Object[]{new String(msg + " Do you want to make a new one?")}, 
							"Reservation Alert", 
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE, 
							null,
							new Object[]{opt, "Cancel"},
							opt);
					if (rtn == JOptionPane.OK_OPTION)
					{
						Reservation res = new Reservation(expc);
						res.getExtension();
					}
					if (!experiment.isActive()) 
					{
						experiment.setProperty(ONLComponent.STATE, ONLComponent.ACTIVE);
						experiment.setProperty(Experiment.ONLID, ((NCCP_Response)r).getExperimentID());
						undoManager.end();
					}
					break;
				case Reservation.AUTH_ERR:
					expc.clearPassword();
					expc.getUserInfo();
					resend = true;
					commit_error = true;
					show_error = false;
					break;
				case Reservation.NO_RES_ERR:
					Reservation res = new Reservation(expc);
					res.getReservation();
					resend = false;
					commit_error = true;
					show_error = false;
					break;
				default:
				    expc.addError(new StatusDisplay.Error(msg, 
									  msg,
									  "Commit Error", 
									  (StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR)));
				    expc.removeRequest(this);
				}
				//System.out.println("Experiment request failed");
				if (commit_error)
				{
					if (show_error)
					{
						JOptionPane.showMessageDialog(expc.getMainWindow(), 
								msg,
								"Commit Error",
								JOptionPane.ERROR_MESSAGE);
					}
					if (!resend)
					{
						//if (((NCCP_Response)r).getErrorCode() != Reservation.ALLOC_ERROR)
						if (!experiment.isActive())
						{
							expc.removeRequest(this);
							uncommittedEdits.addEdits(undoManager);
							experiment.setProperty(ONLComponent.STATE, ONLComponent.FAILED);
						}
					}
					else 
					{
						expc.sendRequest(this);
						expc.print("Resending Commit");
					}
				}
			}
		}

		public void processPeriodicMessage(NCCP.PeriodicMessage msg)
		{
			ExpCoordinator expc = undoManager.getExpCoordinator();
			if (msg.isStop())
			{
				if (expc.getCurrentExp() == experiment) 
				{
					expc.closeCurrentExp();
					String tmp_str = "Experiment Closing.";
					expc.addError(new StatusDisplay.Error(tmp_str, 
							tmp_str,
							"Experiment Terminated", 
							(StatusDisplay.Error.LOG | StatusDisplay.Error.POPUP | StatusDisplay.Error.STATUSBAR)));
				}
				expc.removeRequest(this);
			}
		}
		public void retrieveFieldData(DataInput din) throws IOException {}
		public double getData(MonitorDataType.Base mdt) { return 0;}
	}


	private class NCCP_EndCommitReq extends ExpRequest
	{
		private class NCCP_EndCommit extends ExpRequest.RequesterBase
		{
			private Experiment experiment = null;
			public NCCP_EndCommit(Experiment e)
			{
				super(ExpDaemon.NCCP_Operation_EndCommit);
				experiment = e;
				setMarker(new REND_Marker_class());
			}

			public void storeData(DataOutputStream dout) throws IOException
			{
				ExpCoordinator.theCoordinator.setTopoSet(true);
				NCCP.writeString(experiment.getProperty(Experiment.ONLID), dout);
				//NCCP.writeComponentID(null, dout);
			}
		}//end class ONLUndoManager.NCCP_EndCommitReq
		public NCCP_EndCommitReq(Experiment e)
		{
			super();
			//experiment = e;
			//ExpCoordinator.print(new String("NCCP_ExperimentReq.NCCP_ExperimentReq #edits = " + um.size()), 4);
			request = new NCCP_EndCommit(e);
			response = new NCCP_Response();
		}
		public void processResponse(NCCP.ResponseBase r)
		{
			ExpCoordinator.print(new String("NCCP_EndCommitReq.processResponse status = " + r.getStatus()), 5);
			//ExpCoordinator.print(new String("NCCP_ExperimentReq.processResponse #edits = " + undoManager.size()), 4);
			ExpCoordinator expc = ExpCoordinator.theCoordinator;
			if (r.getStatus() != NCCP.Status_Fine) 
			{
				String msg = ((NCCP_Response)r).getMessage();
				JOptionPane.showMessageDialog(expc.getMainWindow(), 
						msg,
						"Experiment Allocation Error",
						JOptionPane.ERROR_MESSAGE);
			}
			expc.removeRequest(this);
		}

	}//end class ONLUndoManager.NCCP_EndCommitReq

	//protected static class SubUndoManager extends UndoManager
	private class SubUndoManager extends UndoManager
	{
		private UndoAction undoAction = null;
		private final int NOTHING = 0;
		private final int UNDO = 1;
		private final int REDO = 2;
		private int state = NOTHING;
		private ONLUndoManager parent = null;

		public SubUndoManager(UndoAction uma, ONLUndoManager p)
		{
			super();
			undoAction = uma;
			undoAction.setUndoManager(this);
			parent = p;
		}

		public synchronized boolean addEditAt(UndoableEdit anEdit, int i)
		{
			boolean rtn = addEdit(anEdit);
			if (anEdit != null && edits.contains(anEdit))
			{
				edits.remove(anEdit);
				edits.add(i, anEdit);
			}
			return rtn;
		}
		public synchronized boolean addEdit(UndoableEdit anEdit)
		{
			if (anEdit == null) 
			{
				ExpCoordinator.print("ONLUndoManager.SubUndoManager::addEdit anEdit is null",2);
				return false;
			}
			if (edits.size() >= getLimit()) 
			{
				int limit = getLimit();
				int new_limit = limit + (limit/10);
				setLimit(new_limit);
				ExpCoordinator.print(new String("ONLUndoManager.SubUndoManager::addEdit increasing limit from " + limit + " to " + getLimit()), 9);
			}
			if (edits.contains(anEdit)) 
			{
				edits.remove(anEdit);
				ExpCoordinator.print("ONLUndoManager.SubUndoManager::addEdit removing edit", 4);
			}
			if (!edits.isEmpty() && aggregateClass.isAssignableFrom(lastEdit().getClass())) ((AggregateEdit)lastEdit()).end();
			boolean rtn = super.addEdit(anEdit);
			state = UNDO;
			if (!undoAction.isEnabled()) undoAction.setEnabled(true);
			undoAction.setLabel(anEdit.getUndoPresentationName());
			return rtn;
		}

		public synchronized void redo() throws CannotRedoException
		{
			super.redo();
			UndoableEdit anEdit = editToBeUndone();
			if (anEdit != null)
			{
				undoAction.setLabel(anEdit.getUndoPresentationName());
				state = UNDO;
			}
		}

		public synchronized void undo() throws CannotUndoException
		{
			super.undo();
			UndoableEdit anEdit = editToBeRedone();
			if (anEdit != null)
			{
				undoAction.setLabel(anEdit.getRedoPresentationName());
				state = REDO;
			}
		}

		public void end()
		{
			Experiment exp = parent.expCoordinator.getCurrentExp();
			boolean sppmon = ExpCoordinator.isSPPMon();
			if (!sppmon && (exp.getProperty(ONLComponent.STATE).equals(ONLComponent.NOT_INIT) || exp.getProperty(ONLComponent.STATE).equals(ONLComponent.FAILED)))
			{
				parent.expCoordinator.getUserInfo();
				parent.expCoordinator.sendRequest(new NCCP_ExperimentReq(parent.expCoordinator.getCurrentExp(), this));
				exp.setProperty(ONLComponent.STATE, ONLComponent.WAITING);
			}
			if (sppmon || exp.getProperty(ONLComponent.STATE).equals(ONLComponent.ACTIVE))
			{
				//ExpCoordinator.print(new String("OUM.end start #edits = " + edits.size()), 4);
				if (!sppmon) edits.addAll(exp.getFailedEdits());
				//ExpCoordinator.print(new String("OUM.end before super.end #edits = " + edits.size()), 4);
				super.end();
				//ExpCoordinator.print(new String("OUM.end before removing cancelled #edits = " + edits.size()), 4);
				removeCancelledEdits();
				int max = edits.size();
				//ExpCoordinator.print(new String("OUM.end #edits = " + max)ls, 2);
				ONLComponent.Undoable elem;
				for (int i = 0; i < max; ++i)
				{
					elem = (ONLComponent.Undoable)edits.elementAt(i);
					elem.commit();
				}
				if (!sppmon && !parent.expCoordinator.isTopoSet())
					parent.expCoordinator.sendRequest(new NCCP_EndCommitReq(exp)); //PROBLEM don't want to send everytime just on topo. change
				parent.pendingEdits.doNext();
			}
		}

		protected void addEdits(SubUndoManager um)
		{
			ExpCoordinator.print("ONLUndoManager.SubUndoManager.addEdits", 2);
			if (um == this)
			{
				ExpCoordinator.print("  adding edits already there", 2);
				return;
			}
			Vector tmp_v = new Vector(um.edits);
			tmp_v.addAll(edits);
			discardAllEdits();
			int max = tmp_v.size();
			for (int i = 0; i < max; ++i)
			{
				addONLUndoable((ONLComponent.Undoable)tmp_v.elementAt(i));
			}
			ExpCoordinator.print(new String("   " + edits.size() + "edits uncommitted"), 2);
			if (parent != null) parent.commitAction.setEnabled(true);
		}

		public boolean isCurrentEdit(ONLComponent.Undoable e)
		{
			return (lastEdit() == e);
		}

		private void removeCancelledEdits()
		{
			//System.out.println("OUM.removeCancelledEdits");
			int max;
			int i = 0;
			boolean cancelled = false;
			ONLComponent.Undoable elem;
			ONLComponent.Undoable elem2;
			while (i < edits.size())
			{
				elem = (ONLComponent.Undoable)edits.elementAt(i);
				max = edits.size();
				for (int j = i+1; j < max; ++j)
				{
					elem2 = (ONLComponent.Undoable)edits.elementAt(j);
					if (!aggregateClass.isInstance(elem) && !aggregateClass.isInstance(elem2))
					{
						if (elem.isCancelled(elem2)) 
						{
							//ExpCoordinator.printer.print(new String("removeCancelled no aggregates" + elem.getClass().getName()), 4);
							edits.remove(elem);
							cancelled = true;
						}
						if (elem2.isCancelled(elem)) 
						{
							//ExpCoordinator.printer.print(new String("removeCancelled no aggregates " + elem2.getClass().getName()), 4);
							edits.remove(elem2);
							cancelled = true;
						}
						if (cancelled) break;
					}
					else
					{
						if (aggregateClass.isInstance(elem))
						{
							Vector aggEdits = ((AggregateEdit)elem).getEditsCopy();
							if (elem.isCancelled(elem2))
							{
								edits.remove(elem);
								cancelled = true;
							}
							int max2 = aggEdits.size();
							ONLComponent.Undoable elem_a;
							for (int e = 0; e < max2; ++e)
							{
								elem_a = (ONLComponent.Undoable)aggEdits.elementAt(e);
								if (elem2.isCancelled(elem_a))
								{
									//ExpCoordinator.printer.print(new String("removeCancelled " + elem2.getClass().getName()), 4);
									edits.remove(elem2);
									cancelled = true;
									break;
								}
							}
						}
						else
						{
							if (aggregateClass.isInstance(elem2))
							{
								Vector aggEdits = ((AggregateEdit)elem2).getEditsCopy();
								int max2 = aggEdits.size();
								ONLComponent.Undoable elem_a;
								for (int e = 0; e < max2; ++e)
								{
									elem_a = (ONLComponent.Undoable)aggEdits.elementAt(e);
									if (elem.isCancelled(elem_a))
									{
										//ExpCoordinator.printer.print(new String("removeCancelled " + elem.getClass().getName()), 4);
										edits.remove(elem);
										cancelled = true;
										break;
									}
								}
								if (elem2.isCancelled(elem))
								{
									edits.remove(elem2);
									cancelled = true;
								}
							}
						} 
					}
					if (cancelled) break;
				}
				if (cancelled)
				{
					i = 0;
					cancelled = false;
				}
				else ++i;
			}
		}

		public void doNext()
		{
			try
			{
				switch (state)
				{
				case NOTHING:
					break;
				case UNDO:
					undo();
					break;
				case REDO:
					redo();
					break;
				}
			}
			catch (CannotUndoException e)
			{
				System.err.println("SubUndoManager::doNext error - cannot undo");
			}
			catch (CannotRedoException e)
			{
				System.err.println("SubUndoManager::doNext error - cannot redo");
			}
		}
		private int size() { return (edits.size());}
		public ExpCoordinator getExpCoordinator() { return (parent.expCoordinator);}

	} // end inner class SubUndoManager

	protected static class UndoAction extends AbstractAction
	{
		private SubUndoManager undoManager = null;
		public UndoAction()
		{
			super("Undo");
			setEnabled(false);
		}
		public void setLabel(String nm) { putValue(Action.NAME, nm);}
		public String getLabel() { return ((String)getValue(Action.NAME));}
		public void actionPerformed(ActionEvent e)
		{
			//System.out.println("UndoAction::actionPerformed");
			if (undoManager != null) undoManager.doNext();
			else System.out.println(" UndoAction::actionPerformed undoManager null");
		}
		public void setUndoManager(SubUndoManager um)
		{
			undoManager = um; 
		}
	}


	private class CommitAction extends AbstractAction implements ExpCoordinator.MenuAction
	{
		private ONLUndoManager onlUndoManager = null;

		public CommitAction(ONLUndoManager oum)
		{
			super("Commit");
			onlUndoManager = oum;
		}
		public void actionPerformed(ActionEvent e)
		{
			setEnabled(false);
			onlUndoManager.commit();
		}
		public int getType() { return ExpCoordinator.COMMIT;}
		public boolean isType(int t) { return (t == ExpCoordinator.COMMIT);}
	} //end inner class CommitAction

	public ONLUndoManager(ExpCoordinator exp)
	{
		super();
		expCoordinator = exp;
		undoAction = new UndoAction();
		AggregateEdit tmp = new AggregateEdit();
		aggregateClass = tmp.getClass();
		uncommittedEdits = new SubUndoManager(undoAction, this);
		pendingEdits = new PendingEdits();
		commitAction = new CommitAction(this);
		commitAction.setEnabled(false);
	}

	public boolean isCurrentEdit(ONLComponent.Undoable e) { return (uncommittedEdits.isCurrentEdit(e));}
	public void commit() 
	{
		if (commitAction.isEnabled())
			commitAction.setEnabled(false);
		System.out.println("OUM.commit uncommitted edits = " + uncommittedEdits.size());
		if (uncommittedEdits.size() > 0)
		{
			SubUndoManager tmp = uncommittedEdits;
			undoAction.setLabel("Undo");
			undoAction.setEnabled(false);
			uncommittedEdits = new SubUndoManager(undoAction, this);
			pendingEdits.add(tmp);
			if (!expCoordinator.isSPPMon() && !expCoordinator.isConnected())// && !expCoordinator.isTestLevel(ExpCoordinator.NPTEST))
			{
				//listen on connection when connection is made can end the
				//edit, but need to save the set the user wants to commit as separate.
				if (!expCoordinator.connect()) 
				{
					System.out.println("ONLUndoManager.commit enabling commit action");
					commitAction.setEnabled(true);
				}
			}
			else
				pendingEdits.doNext();//tmp.end();
			addEdit(tmp);
		}
		else 
		{
			if (!expCoordinator.isSPPMon() && !expCoordinator.isConnected())// && !expCoordinator.isTestLevel(ExpCoordinator.NPTEST))
			{
				if (!expCoordinator.connect()) commitAction.setEnabled(true);
			}
			else
			{
				pendingEdits.doNext();
			}
		}
	}
	public void clear() 
	{  
		ExpCoordinator.print("OUM.clear", 4);
		commitAction.setEnabled(false);
		uncommittedEdits.discardAllEdits();
		discardAllEdits();//CHANGED 8/10/10
	}
	public Action getCommitAction() { return commitAction;}
	public Action getUndoAction() { return undoAction;}
	public void addONLUndoable(ONLComponent.Undoable ou) 
	{ 
		uncommittedEdits.addEdit(ou);
		if (!commitAction.isEnabled() && uncommittedEdits.size() > 0) commitAction.setEnabled(true);
	}
	public void addONLUndoableAt(ONLComponent.Undoable ou, int i) 
	{ 
		uncommittedEdits.addEditAt(ou, i);
		if (!commitAction.isEnabled() && uncommittedEdits.size() > 0) commitAction.setEnabled(true);
	}
	//NCCPConnection.ConnectionListener
	public void connectionFailed(NCCPConnection.ConnectionEvent e){}
	public void connectionClosed(NCCPConnection.ConnectionEvent e){}
	public void connectionOpened(NCCPConnection.ConnectionEvent e)
	{
		pendingEdits.doNext();
	}
	//end NCCPConnection.ConnectionListener
}
