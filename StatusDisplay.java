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
 * File: StatusDisplay.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created:
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.util.Vector;
import java.awt.event.*;
import java.awt.*;
import java.beans.*;
import javax.swing.border.*;
import java.io.IOException;

public class StatusDisplay extends JPanel implements PropertyChangeListener
{
	public static final int TEST_ERROR = 3;
	public static final int TEST_COMMITBAR = 8;
	public static final int TEST_WAITING = TEST_COMMITBAR;
	private ExpCoordinator expCoordinator = null;
	private ONLMainWindow mainWindow = null;
	private CommitProgressBar commitProgress = null;
	private Vector errors = null;
	private JLabel statusMsg = null;
	private ErrorLog errorLog = null;
	private TitledBorder needCommitBorder = null;
	private Border commitBorder = null;
	private Vector listeners = null;

	public static interface Listener
	{
		public void errorAdded(StatusDisplay.Error e);
		public void setCommit(boolean b);
		public void commitCompleted(String msg);
	}

	private class ErrorLog extends AbstractAction
	{
		private JDialog dialog = null;
		private JTextArea log = null;

		public ErrorLog()
		{
			super("Show Error Log");
			setEnabled(true);
			//log = new JTextArea();
		}

		public void actionPerformed(ActionEvent e) { showLog();}
		public void clear() 
		{
			if (dialog != null && dialog.isVisible()) dialog.setVisible(false);
			if (log != null)
			{
				log.selectAll();
				log.cut();
				ExpCoordinator.print("\nStatusDisplay.ErrorLog.clear\n", TEST_ERROR);
			}
		}
		public void add(String m) 
		{ 
			showLog();
			String msg = m;
			if (!msg.endsWith("\n")) msg = msg.concat("\n");
			log.append(msg);
		}
		public void showLog()
		{
			if (log == null) 
			{
				log = new JTextArea();
				//log.setEditable(false);
				log.setLineWrap(true);
				log.append("\n");
			}
			if (dialog == null)
			{
				dialog = new JDialog(mainWindow, "Error Log");
				dialog.setSize(500, 120);
				dialog.setFocusableWindowState(false);

				JScrollPane sp = new JScrollPane(log);
				sp.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
				dialog.getContentPane().add(sp);
			}
			if (!dialog.isVisible()) dialog.setVisible(true);
		}
	}

	public static class Error
	{
		public static final int LOG = 1;
		public static final int POPUP = 2;
		public static final int STATUSBAR = 4;
		public static final int ALL = 7;

		private int type = LOG;
		private String statusBarMsg = "";
		private String logMessage = "";
		private String popupTitle = "";

		public Error(String lg) { logMessage = new String(lg);}
		public Error(String lg, String sbm, String pt, int tp)
		{
			logMessage = new String(lg);
			statusBarMsg = new String(sbm);
			popupTitle = new String(pt);
			type = tp;
		}
		public Error(ONL.Reader rdr) throws IOException
		{
			read(rdr);
		}
		public Error(Error e) { this(e.logMessage, e.statusBarMsg, e.popupTitle, e.type);}
		public boolean isStatusBar() { return ((type & STATUSBAR) > 0);}
		public boolean isPopup() { return ((type & POPUP) > 0);}
		public boolean isLog() { return ((type & LOG) > 0);}
		public void setType(int tp) { type = tp;}
		public String getLogMsg() { return logMessage;}
		public void setLogMsg(String lg) { logMessage = new String(lg);}
		public String getPopupTitle() { return popupTitle;}
		public void setPopupTitle(String pt) { popupTitle = new String(pt);}
		public String getStatusBarMsg() { return statusBarMsg;}
		public void setStatusBarMsg(String sbm) { statusBarMsg = new String(sbm);}
		public void write(ONL.Writer wrtr) throws IOException
		{
			wrtr.writeString(logMessage);
			wrtr.writeString(statusBarMsg);
			wrtr.writeString(popupTitle);
			wrtr.writeInt(type);
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			logMessage = rdr.readString();
			statusBarMsg = rdr.readString();
			popupTitle = rdr.readString();
			type = rdr.readInt();
		}
	}

	private class CommitProgressBar extends JProgressBar implements ActionListener, PropertyChangeListener//JProgressBar
	{
		private Timer timer = null;
		private Vector waiting = null;
		private int delay = 0;
		private int maximum = 0;
		private boolean enabled = false;

		public CommitProgressBar(int max, int update) //max time in secs, and update period in secs
		{
			super(0, max);
			//setSize(20, 100);
			Dimension tmp_sz = new Dimension(160,40);
			setPreferredSize(tmp_sz);
			setMaximumSize(tmp_sz);
			delay = update;
			maximum = max;
			timer = new Timer((update*1000), this);
			waiting = new Vector();
			setString("Progress Bar");
			setStringPainted(true);
			setBorderPainted(true);
			setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
			setFont(new Font("Dialog", Font.BOLD, 10));
		}

		public void addWaiting(Object w) 
		{
			if (ExpCoordinator.isSnapshotView()) return;
			if (!enabled) return;
			if (!timer.isRunning()) start();
			waiting.add(w);
			ExpCoordinator.print(new String("CommitProgressBar.addWaiting " + w.toString() + " number waiting " + waiting.size()), TEST_WAITING);
		}
		public void removeWaiting(Object w) 
		{
		    if (ExpCoordinator.isSPPMon()) removeWaiting(w, "Completed.");
		    else removeWaiting(w, "Commit Completed");
		}
	    /* CHANGED SPPMON
			if (waiting.contains(w)) 
			{
				waiting.remove(w);
				ExpCoordinator.printer.print(new String("CommitProgressBar.removeWaiting " + w.toString() + " number waiting " + waiting.size()), 2);
			}
			if (waiting.size() == 0) //if there are no outstanding commits kill the progress bar
			{
				setValue(getMaximum());
				setString("Commit Completed.");
				stop();
			}
		}
	    */
		public void removeWaiting(Object w, String s) 
		{
			if (waiting.contains(w)) 
			{
				waiting.remove(w);
				ExpCoordinator.print(new String("CommitProgressBar.removeWaiting " + w.toString() + " number waiting " + waiting.size()), TEST_WAITING);
			}
			if (waiting.size() == 0) //if there are no outstanding commits kill the progress bar
			{
				setValue(getMaximum());
				setString(s);
				stop();
				commitCompleted(s);
			}
		}
		public void increaseMax(int i)
		{
			if (!enabled) return;
			int val_left = getMaximum() - getValue();
			if (val_left < i) setMaximum(getValue() + i);
		}

		public void start() 
		{
			if (!enabled) return;
			ExpCoordinator.printer.print("CommitProgressBar.start", 2);
			setMaximum(maximum);
			setValue(getMinimum());
			if (!timer.isRunning()) timer.start(); //not sure if this should be a restart
			else timer.restart();
			if (ExpCoordinator.isSPPMon()) setString("Processing");
			else setString("Committing");
			//updateString();
		}
		private void stop() 
		{ 
			enabled = false;
			ExpCoordinator.printer.print("CommitProgressBar.stop", 2);
			timer.stop();
		}
		public void actionPerformed(ActionEvent e)
		{
			if (!enabled) return;
			int ov = getValue();
			int nv = getValue() + (timer.getDelay()/1000);
			if (nv >= getMaximum()) 
			{
				//ExpCoordinator.printer.print("CommitProgressBar.actionPerformed increasing max");
				setMaximum(nv + 30);
			}
			ExpCoordinator.print(new String("StatusDisplay.CommitProgressBar.actionPerformed waiting size:" + waiting.size()), TEST_COMMITBAR);
			setValue(nv);
		}
		public void setEnabled(boolean b) { enabled = b;}
		public boolean isEnabled() { return enabled;}

		public void clear() 
		{ 
			setString("Progress Bar");
			waiting.clear();
			setValue(getMaximum());
			stop();
		}

		public boolean isCommitted() { return (!timer.isRunning() && waiting.isEmpty());}

		//PropertyChangeListener
		public void propertyChange(PropertyChangeEvent e)
		{
			String prop = e.getPropertyName();
			if (prop.equals(ExpCoordinator.OBSERVE))
			{
				if (ExpCoordinator.isObserver()) setString("Observing");
				else 
					setString("Progress Bar");
			}
			if (prop.equals(ExpCoordinator.VIEW_SNAPSHOT))
			{
				if (ExpCoordinator.isSnapshotView()) setString("Snapshot");
				else 
					setString("Progress Bar");
			}
		}
	}

	public StatusDisplay(ExpCoordinator ec)
	{
		super();
		Font tmp_fnt = new Font("Dialog", Font.BOLD, 10);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		expCoordinator = ec;
		mainWindow = expCoordinator.getMainWindow();
		errorLog = new ErrorLog();
		commitProgress = new CommitProgressBar(0, 2);
		JButton errorButton = new JButton(errorLog);
		errorButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		Dimension tmp_sz = new Dimension(130,30); 
		errorButton.setPreferredSize(tmp_sz);
		errorButton.setMaximumSize(tmp_sz);
		errorButton.setBorderPainted(true);
		errorButton.setFont(tmp_fnt);
		statusMsg = new JLabel("No Status");
		tmp_sz = new Dimension(300,30);
		statusMsg.setPreferredSize(tmp_sz);
		statusMsg.setMaximumSize(tmp_sz);
		statusMsg.setFont(tmp_fnt);
		statusMsg.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		errors = new Vector();
		add(statusMsg);
		add(commitProgress);
		add(Box.createHorizontalGlue());
		add(errorButton);
		mainWindow.getContentPane().add(this, BorderLayout.PAGE_END);
		needCommitBorder = new TitledBorder((BorderFactory.createLineBorder(Color.red)), 
				"Commit Needed",
				TitledBorder.DEFAULT_JUSTIFICATION,
				TitledBorder.TOP,
				new Font("Dialog", Font.PLAIN, 9),
				Color.red);
		commitBorder = BorderFactory.createEmptyBorder(7,0,0,0);
		setBorder(commitBorder);
		setVisible(true);
		listeners = new Vector();
		expCoordinator.addPropertyListener(commitProgress);
	}

	public boolean isCommitted() { return (commitProgress.isCommitted());}

	public void addError(Error e)
	{
		if (e.isLog()) 
		{
			errorLog.add(e.getLogMsg());
			ExpCoordinator.printer.print(e.getLogMsg(), 1);
		}
		if (e.isStatusBar()) statusMsg.setText(e.getStatusBarMsg());
		if (e.isPopup()) 
		{
			JOptionPane.showMessageDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(), //mainWindow,
					e.getLogMsg(),
					e.getPopupTitle(), 
					JOptionPane.PLAIN_MESSAGE);
		}
		int max = listeners.size();
		StatusDisplay.Listener elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (StatusDisplay.Listener)listeners.elementAt(i);
			elem.errorAdded(e);
		}
		errors.add(new Error(e));
	}

	public void clear()
	{
		commitProgress.clear();
		errorLog.clear();
		errors.clear();
		statusMsg.setText("");
	}

	public void statusReturned(Object o) { commitProgress.removeWaiting(o);}
	public void waitForStatus(Object o) 
	{	
		if (commitProgress.waiting.size() == 0) statusMsg.setText("");
		commitProgress.addWaiting(o);
	}
	public void increaseProgressMax(int v) { commitProgress.increaseMax(v);}

	//PropertyChangeListener
	public void propertyChange(PropertyChangeEvent e)
	{
		if (ExpCoordinator.isObserver()) return;
		if (((Action)e.getSource()).isEnabled()) setCommitBorder(false);//needCommitBorder);
		else 
		{
			setCommitBorder(true);//commitBorder);
			commitProgress.setEnabled(true);
		}
		revalidate();
		//repaint();
	}
	//end PropertyChangeListener
	public void setCommitBorder(boolean b)
	{
		if (b)
			setBorder(commitBorder);
		else
			setBorder(needCommitBorder);
		int max = listeners.size();
		StatusDisplay.Listener elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (StatusDisplay.Listener)listeners.elementAt(i);
			elem.setCommit(b);
		}
	}
	private void commitCompleted(String s)
	{
		int max = listeners.size();
		StatusDisplay.Listener elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (StatusDisplay.Listener)listeners.elementAt(i);
			elem.commitCompleted(s);
		}
	}
	public void addListener(StatusDisplay.Listener l)
	{
		if (!listeners.contains(l)) listeners.add(l);
	}
	public void removeListener(StatusDisplay.Listener l)
	{
		if (listeners.contains(l)) listeners.remove(l);
	}
}
