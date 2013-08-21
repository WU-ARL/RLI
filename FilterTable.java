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
 * File: FilterTable.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 10/29/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.lang.reflect.Array;
import java.util.*;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;



public class FilterTable extends ONLComponent implements FilterDescriptor.Listener, Priority.PRTable, MonitorAction, PropertyChangeListener
{

	protected static final String PRIORITY_PROP = "priority";
	private Vector filters = null;
	private NSPPort port = null;
	private NSPDescriptor nsp = null;
	//private int type = FilterDescriptor.EXCL_GEN_MATCH;
	private Priority priority = null;
	private boolean ingress = true;
	private int filterType = FilterDescriptor.EXCL_GEN_MATCH;
	private JTable jtable = null;
	private TableModel tableModel = null;
	private JPanel panel = null;
	private OpManager opManager = null;
	private boolean statsOn = false;

	private RouterPort.SamplingType samplingTypes[] = null;


	//number of columns
	protected static final int EMNUMCOLS = 8;//9;
	protected static final int GMNUMCOLS = 19;

	//private int STATS_COL = EMSTATS_COL;

	private String fieldNames[];
	//field types

	protected static final int SRCADDR = FilterDescriptor.SRCADDR;
	protected static final int EMSRCADDR = FilterDescriptor.DESTADDR;
	protected static final int SRCPORT = FilterDescriptor.SRCPORT;
	protected static final int EMSRCPORT = FilterDescriptor.DESTPORT;

	protected static final int PROTOCOL = FilterDescriptor.PROTOCOL;

	protected static final int FORWARDKEY = FilterDescriptor.FORWARDKEY;  
	protected static final int PBINDING = FilterDescriptor.PBINDING;
	protected static final int QID = FilterDescriptor.QID;
	protected static final int STATS = FilterDescriptor.STATS;
	protected static final int PRIORITY = FilterDescriptor.PRIORITY;
	protected static final int MIRROR = FilterDescriptor.MIRROR;
	protected static final int ADDR_TYPE = FilterDescriptor.ADDR_TYPE;
	protected static final int NEGATE = FilterDescriptor.NEGATE;
	protected static final int DROP = FilterDescriptor.DROP;
	protected static final int ENABLE = FilterDescriptor.ENABLE;
	protected static final int SAMPLING_TYPE =  FilterDescriptor.SAMPLING_TYPE;
	//protected static final int TCPFLAGS =  FilterDescriptor.TCPFLAGS;
	//protected static final int TCPFLAGSMASK =  FilterDescriptor.TCPFLAGSMASK;
	protected static final int TCPFIN = FilterDescriptor.TCPFIN;
	protected static final int TCPSYN = FilterDescriptor.TCPSYN;
	protected static final int TCPRST = FilterDescriptor.TCPRST;
	protected static final int TCPPSH = FilterDescriptor.TCPPSH;
	protected static final int TCPACK = FilterDescriptor.TCPACK;
	protected static final int TCPURG = FilterDescriptor.TCPURG;
	protected static final int NUMFIELDS = 22;



	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////// FilterTable.FTContentHandler ////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private class FTContentHandler extends FilterDescriptor.XMLHandler
	{
		private FilterTable filterTable = null;

		public FTContentHandler(ExperimentXML exp_xml, FilterTable ft)
		{
			super(exp_xml, null);
			filterTable = ft;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{	  
			super.startElement(uri, localName, qName, attributes);
			if (localName.equals(ExperimentXML.FILTER) && currentFilter == null) 
				currentFilter = new FilterDescriptor(uri, attributes, (NSPPort)filterTable.getPort());//Integer.parseInt(attributes.getValue(uri, ExperimentXML.FILTER_TYPE)), (NSPPort)filterTable.getPort(), Boolean.valueOf(attributes.getValue(uri, ExperimentXML.INGRESS)).booleanValue());
		}
		public void setCurrentElement(String s) { currentElement = new String(s);}
		public String getCurrentElement() { return currentElement;}
		public void endElement(String uri, String localName, String qName)
		{
			if (localName.endsWith(ExperimentXML.FILTER_TABLE)) 
			{
				expXML.removeContentHandler(this);
			}
			if (localName.equals(ExperimentXML.FILTER) && currentFilter != null) 
			{
				addNewFilter(currentFilter);
				currentFilter = null;
			}
		}
	}

	//////////////////////////////////////////////// EditFilterWindow /////////////////////////////////////////////////////////////////////////  
	protected static class EditFilterWindow
	{
		private FilterDescriptor filter = null;
		private Object[] options;
		private String label;
		private FilterTable ftable = null;

		private class LabelPanel extends JPanel
		{
			private JLabel label = null;
			private JComponent component = null;
			public LabelPanel(String nm, JComponent c)
			{
				super();
				setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				label = new JLabel(nm);
				label.setAlignmentY(Component.TOP_ALIGNMENT);
				add(label);
				c.setAlignmentY(Component.TOP_ALIGNMENT);
				component = c;
				//setAlignmentY(Component.TOP_ALIGNMENT);
				add(c);
			}
			public void setComponentAlignmentY(float a) { component.setAlignmentY(a);}
			public void setLabelAlignmentY(float a) { label.setAlignmentY(a);}
			public void setLabel(String lbl) { label.setText(lbl);}
		}


		public EditFilterWindow(FilterDescriptor fd, String l) { this(fd, l, "Edit", "Cancel");}
		public EditFilterWindow(FilterDescriptor fd, String l, String o1, String o2)
		{
			filter = fd;
			ftable = (FilterTable)filter.getTable();
			label = new String(l);
			options = new String[2];
			options[0] = o1;
			options[1] = o2;
		}
		public FilterDescriptor edit()
		{
			Object[] array = new Object[9];
			int next_ndx = 0;
			FilterDescriptor rtn_filter = new FilterDescriptor(filter);
			TextFieldwLabel srcaddr = new TextFieldwLabel(new JFormattedTextField(rtn_filter.getSrcAddr()), "source address/mask:");
			TextFieldwLabel srcport = new TextFieldwLabel(new JFormattedTextField(rtn_filter.getSrcPort()), "source port:");
			JPanel tmp_panel = new JPanel();
			tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
			tmp_panel.add(srcaddr);
			tmp_panel.add(srcport);
			array[next_ndx++] = tmp_panel;

			TextFieldwLabel destaddr = new TextFieldwLabel(new JFormattedTextField(rtn_filter.getDestAddr()), "destination address/mask:");
			TextFieldwLabel destport = new TextFieldwLabel(new JFormattedTextField(rtn_filter.getDestPort()), "destination port:");
			tmp_panel = new JPanel();
			tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
			tmp_panel.add(destaddr);
			tmp_panel.add(destport);
			array[next_ndx++] = tmp_panel;

			//protocol
			tmp_panel = new JPanel();
			tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
			FilterDescriptor.Protocol tmp_protocols[]= {new FilterDescriptor.Protocol(FilterDescriptor.WILDCARD),
					new FilterDescriptor.Protocol(FilterDescriptor.Protocol.TCP), 
					new FilterDescriptor.Protocol(FilterDescriptor.Protocol.UDP), 
					new FilterDescriptor.Protocol(FilterDescriptor.Protocol.ICMP)};
			final JComboBox protocol_cb = new JComboBox(tmp_protocols);
			FilterDescriptor.Protocol protocol = rtn_filter.getProtocol();
			if (protocol.toInt() == FilterDescriptor.WILDCARD) protocol_cb.setSelectedIndex(0);
			if (protocol.toInt() == FilterDescriptor.Protocol.TCP) protocol_cb.setSelectedIndex(1);
			if (protocol.toInt() == FilterDescriptor.Protocol.UDP) protocol_cb.setSelectedIndex(2);
			if (protocol.toInt() == FilterDescriptor.Protocol.ICMP) protocol_cb.setSelectedIndex(3);
			protocol_cb.setEditable(true);
			//protocol_cb.setMaximumSize(new Dimension(75,30));
			LabelPanel tmp_lblpan = new LabelPanel("protocol:",protocol_cb);
			tmp_lblpan.setLabelAlignmentY(Component.CENTER_ALIGNMENT);
			tmp_lblpan.setComponentAlignmentY(Component.CENTER_ALIGNMENT);
			//tmp_lblpan.setMaximumSize(new Dimension(150,30));
			tmp_panel.add(tmp_lblpan);
			//array[next_ndx++] = tmp_panel;

			//tcpflags
			JPanel tcpflags_pan = null;
			FilterDescriptor.TCPFlag bitopts[] = {new FilterDescriptor.TCPFlag(0,0), new FilterDescriptor.TCPFlag(1,1), new FilterDescriptor.TCPFlag(0,1)};
			final JComboBox tcpfin_cb = new JComboBox(bitopts);
			final JComboBox tcpsyn_cb = new JComboBox(bitopts);
			final JComboBox tcprst_cb = new JComboBox(bitopts);
			final JComboBox tcppsh_cb = new JComboBox(bitopts);
			final JComboBox tcpack_cb = new JComboBox(bitopts);
			final JComboBox tcpurg_cb = new JComboBox(bitopts);

			if (rtn_filter.getType() != FilterDescriptor.EXACT_MATCH)
			{
				tcpflags_pan = new JPanel();
				tcpflags_pan.setLayout(new BoxLayout(tcpflags_pan, BoxLayout.Y_AXIS));

				tcpfin_cb.setEditable(false);
				FilterDescriptor.TCPFlag flagbit = rtn_filter.getTCPFlag(FilterDescriptor.TCPFIN_MASK);
				if (flagbit.toString().equals("*")) tcpfin_cb.setSelectedIndex(0);
				if (flagbit.toString().equals("1")) tcpfin_cb.setSelectedIndex(1);
				if (flagbit.toString().equals("0")) tcpfin_cb.setSelectedIndex(2);
				tcpflags_pan.add(new LabelPanel("tcpfin ", tcpfin_cb));
				tcpsyn_cb.setEditable(false);
				flagbit = rtn_filter.getTCPFlag(FilterDescriptor.TCPSYN_MASK);
				if (flagbit.toString().equals("*")) tcpsyn_cb.setSelectedIndex(0);
				if (flagbit.toString().equals("1")) tcpsyn_cb.setSelectedIndex(1);
				if (flagbit.toString().equals("0")) tcpsyn_cb.setSelectedIndex(2);
				tcpflags_pan.add(new LabelPanel("tcpsyn ", tcpsyn_cb));
				tcprst_cb.setEditable(false);
				flagbit = rtn_filter.getTCPFlag(FilterDescriptor.TCPRST_MASK);
				if (flagbit.toString().equals("*")) tcprst_cb.setSelectedIndex(0);
				if (flagbit.toString().equals("1")) tcprst_cb.setSelectedIndex(1);
				if (flagbit.toString().equals("0")) tcprst_cb.setSelectedIndex(2);
				tcpflags_pan.add(new LabelPanel("tcprst ", tcprst_cb));
				tcppsh_cb.setEditable(false);
				flagbit = rtn_filter.getTCPFlag(FilterDescriptor.TCPPSH_MASK);
				if (flagbit.toString().equals("*")) tcppsh_cb.setSelectedIndex(0);
				if (flagbit.toString().equals("1")) tcppsh_cb.setSelectedIndex(1);
				if (flagbit.toString().equals("0")) tcppsh_cb.setSelectedIndex(2);
				tcpflags_pan.add(new LabelPanel("tcppsh ", tcppsh_cb));
				tcpack_cb.setEditable(false);
				flagbit = rtn_filter.getTCPFlag(FilterDescriptor.TCPACK_MASK);
				if (flagbit.toString().equals("*")) tcpack_cb.setSelectedIndex(0);
				if (flagbit.toString().equals("1")) tcpack_cb.setSelectedIndex(1);
				if (flagbit.toString().equals("0")) tcpack_cb.setSelectedIndex(2);
				tcpflags_pan.add(new LabelPanel("tcpack ", tcpack_cb));
				tcpurg_cb.setEditable(false);
				flagbit = rtn_filter.getTCPFlag(FilterDescriptor.TCPURG_MASK);
				if (flagbit.toString().equals("*")) tcpurg_cb.setSelectedIndex(0);
				if (flagbit.toString().equals("1")) tcpurg_cb.setSelectedIndex(1);
				if (flagbit.toString().equals("0")) tcpurg_cb.setSelectedIndex(2);
				tcpflags_pan.add(new LabelPanel("tcpurg ", tcpurg_cb));
				tmp_panel.add(new LabelPanel("tcpflags:", tcpflags_pan));
				//array[next_ndx++] = tmp_panel;

				ActionListener tcp_listener = new AbstractAction(){
					public void actionPerformed(ActionEvent e)
					{
						if (protocol_cb.getSelectedIndex() == 1)
						{
							tcpfin_cb.setEnabled(true);
							tcpsyn_cb.setEnabled(true);
							tcprst_cb.setEnabled(true);
							tcppsh_cb.setEnabled(true);
							tcpack_cb.setEnabled(true);
							tcpurg_cb.setEnabled(true);
						}
						else
						{
							tcpfin_cb.setEnabled(false);
							tcpsyn_cb.setEnabled(false);
							tcprst_cb.setEnabled(false);
							tcppsh_cb.setEnabled(false);
							tcpack_cb.setEnabled(false);
							tcpurg_cb.setEnabled(false);
						}
					}
				};

				protocol_cb.addActionListener(tcp_listener);
			}

			array[next_ndx++] = tmp_panel;
			tmp_panel = new JPanel();
			tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
			//aux or primary
			final JCheckBox aux_primary =  new JCheckBox();
			if (rtn_filter.getType() != FilterDescriptor.EXACT_MATCH)
			{ 
				aux_primary.setSelected( rtn_filter.isAux());
				tmp_lblpan = new LabelPanel("aux:", aux_primary);
				tmp_lblpan.setLabelAlignmentY(Component.CENTER_ALIGNMENT);
				tmp_lblpan.setComponentAlignmentY(Component.CENTER_ALIGNMENT);
				tmp_panel.add(tmp_lblpan);
			}
			//multicast
			final JCheckBox multicast = new JCheckBox();
			multicast.setSelected(rtn_filter.getFKey().isMCast());
			tmp_lblpan = new LabelPanel("multicast:", multicast);
			tmp_lblpan.setLabelAlignmentY(Component.CENTER_ALIGNMENT);
			tmp_lblpan.setComponentAlignmentY(Component.CENTER_ALIGNMENT);
			tmp_panel.add(tmp_lblpan);
			array[next_ndx++] = tmp_panel;

			final JCheckBox drop_box = new JCheckBox();
			final JCheckBox negation_box = new JCheckBox();
			if (rtn_filter.getType() != FilterDescriptor.EXACT_MATCH)
			{ 
				tmp_panel = new JPanel();
				tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));

				//drop
				drop_box.setSelected(rtn_filter.isDrop());
				tmp_lblpan = new LabelPanel("drop:", drop_box);
				tmp_lblpan.setLabelAlignmentY(Component.CENTER_ALIGNMENT);
				tmp_lblpan.setComponentAlignmentY(Component.CENTER_ALIGNMENT);
				tmp_lblpan.setAlignmentY(Component.CENTER_ALIGNMENT);
				tmp_panel.add(tmp_lblpan);

				//negation
				negation_box.setSelected(rtn_filter.isNegated());
				tmp_lblpan = new LabelPanel("!", negation_box);
				tmp_panel.add(tmp_lblpan);

				array[next_ndx++] = tmp_panel;
			}

			tmp_panel = new JPanel();
			tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
			//voq, forward keys
			JComboBox fkey_cb = null;
			JFormattedTextField qid = null;
			if (rtn_filter.isIngress())
			{
				FilterDescriptor.ForwardKey fkeys[] = { new FilterDescriptor.ForwardKey(0,0),
						new FilterDescriptor.ForwardKey(1,0),
						new FilterDescriptor.ForwardKey(2,0),
						new FilterDescriptor.ForwardKey(3,0),
						new FilterDescriptor.ForwardKey(4,0),
						new FilterDescriptor.ForwardKey(5,0),
						new FilterDescriptor.ForwardKey(6,0),
						new FilterDescriptor.ForwardKey(7,0),
						new FilterDescriptor.ForwardKey(FilterDescriptor.ForwardKey.USE_ROUTE,0)};
				fkey_cb = new JComboBox(fkeys);
				if (ftable.getType() == ONLComponent.IPPEMFTABLE)
					fkey_cb.setEditable(true);
				else
					fkey_cb.setEditable(false);
				tmp_panel.add(new LabelPanel("forward key/voq:", fkey_cb));
			}
			else
			{
				//qid
				qid = new JFormattedTextField(rtn_filter.getQID());
				tmp_panel.add(new LabelPanel("queue id (valid queue ids 8-503):", qid));
			}

			//spc qid
			FilterDescriptor.PluginBinding pbs[]= {new FilterDescriptor.NoPluginPBinding(), new FilterDescriptor.PluginPBinding()};
			JComboBox spcqid_cb = new JComboBox(pbs);
			spcqid_cb.setEditable(true);
			tmp_panel.add(new LabelPanel("spc qid:", spcqid_cb));

			array[next_ndx++] = tmp_panel;

			JFormattedTextField priority =  null;
			final JComboBox sampling_cb = new JComboBox(ftable.port.getSamplingTypes());
			if (rtn_filter.getType() != FilterDescriptor.EXACT_MATCH)
			{
				tmp_panel = new JPanel();
				tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
				//priority
				priority =  new JFormattedTextField(new Integer(rtn_filter.getPriority()));
				tmp_panel.add(new LabelPanel("priority(0-63):", priority));
				//sampling types
				sampling_cb.setEditable(false);
				sampling_cb.setSelectedIndex(rtn_filter.getSamplingType().getType());
				tmp_panel.add(new LabelPanel("sampling type (aux only):", sampling_cb));
				array[next_ndx++] = tmp_panel;
				if (!rtn_filter.isAux()) sampling_cb.setEnabled(false);
				ActionListener aux_listener = new AbstractAction(){
					public void actionPerformed(ActionEvent e)
					{
						if (aux_primary.isSelected())
						{
							//multicast.setSelected(false);
							sampling_cb.setEnabled(true);
							drop_box.setEnabled(false);
						}
						else
						{
							sampling_cb.setEnabled(false);
							drop_box.setEnabled(true);
						}
					}
				};

				aux_primary.addActionListener(aux_listener);
			}


			/*
      ActionListener mc_listener = new AbstractAction(){
          public void actionPerformed(ActionEvent e)
          {
            if (multicast.isSelected())
            {
              aux_primary.setSelected(false);
            }
          }
        };

      multicast.addActionListener(mc_listener);
			 */




			String ostr = "";
			if (ExpCoordinator.isRecording()) ostr = rtn_filter.getRecordedString();
			//window
			int rtn = JOptionPane.showOptionDialog(ftable.port.getFrame(), 
					array, 
					label, 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, 
					null,
					options,
					options[0]);
			if (rtn == JOptionPane.YES_OPTION)
			{
				try 
				{
					filter.setSrcAddr(new FilterDescriptor.PrefixMask(srcaddr.getText()));
					filter.setSrcPort(new FilterDescriptor.PortRange(srcport.getText()));
					filter.setDestAddr(new FilterDescriptor.PrefixMask(destaddr.getText()));
					filter.setDestPort(new FilterDescriptor.PortRange(destport.getText()));
					filter.setProtocol(new FilterDescriptor.Protocol((FilterDescriptor.Protocol)protocol_cb.getSelectedItem()));
					//set tcp flags
					filter.setTCPFlags(tcpfin_cb.getSelectedItem().toString(), FilterDescriptor.TCPFIN_MASK);
					filter.setTCPFlags(tcpsyn_cb.getSelectedItem().toString(), FilterDescriptor.TCPSYN_MASK);
					filter.setTCPFlags(tcprst_cb.getSelectedItem().toString(), FilterDescriptor.TCPRST_MASK);
					filter.setTCPFlags(tcppsh_cb.getSelectedItem().toString(), FilterDescriptor.TCPPSH_MASK);
					filter.setTCPFlags(tcpack_cb.getSelectedItem().toString(), FilterDescriptor.TCPACK_MASK);
					filter.setTCPFlags(tcpurg_cb.getSelectedItem().toString(), FilterDescriptor.TCPURG_MASK);

					//set aux
					if (aux_primary != null) filter.setAux(aux_primary.isSelected());

					//set multicast
					//filter.setMulticast(multicast.isSelected());

					//set drop
					if (drop_box != null) filter.setDrop(drop_box.isSelected());

					//set plugin binding
					filter.setPluginBinding(new FilterDescriptor.PluginBinding(spcqid_cb.getSelectedItem().toString()));

					//set negation
					if (negation_box != null) filter.setNegation(negation_box.isSelected());

					//set qid
					if (qid != null) filter.setQID(new FilterDescriptor.QueueID(qid.getText()));

					//set voq
					if (fkey_cb != null) filter.setFKey(new FilterDescriptor.ForwardKey(fkey_cb.getSelectedItem().toString()));

					//set plugin binding
					filter.setPluginBinding(new FilterDescriptor.PluginBinding(spcqid_cb.getSelectedItem().toString()));

					//set priority
					if (priority != null) filter.setPriority(Integer.parseInt(priority.getText()));

					//set sampling
					filter.setSamplingType(ftable.getSamplingType(sampling_cb.getSelectedIndex()));

					if (filter.isCommitted()) 
					{
						ExpCoordinator ec = ExpCoordinator.theCoordinator;
						FilterDescriptor.UpdateEdit edit = (FilterDescriptor.UpdateEdit)filter.getUpdateEdit();
						if (!ec.isCurrentEdit(edit)) 
						{
							edit.resetUpdate();
							ec.addEdit(edit);
						}
						else edit.setLatest();

						if (ExpCoordinator.isRecording())
							ExpCoordinator.recordEvent(SessionRecorder.EDIT_FILTER, new String[]{ftable.port.getLabel(), ostr, filter.getRecordedString()});
					}
					return filter;
				}
				catch (java.text.ParseException e)
				{
					ExpCoordinator.print("Parse Error:FilterTable.EditFilterWindow");
				}
			}

			return null;
		}
	}


	/////////////////////////////////////////////////////// AlternatingRenderer //////////////////////////////////////////////////////////////////////////////
	public static class AlternatingRenderer extends DefaultTableCellRenderer
	{
		private boolean drawOdd = false;
		private TableCellRenderer alternate = null;

		public AlternatingRenderer(boolean odd, TableCellRenderer alt) 
		{ 
			super();
			drawOdd = odd;
			alternate = alt;
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			boolean is_odd = ((row & 1) != 0);
			if ((!drawOdd && !is_odd) || (drawOdd && is_odd)) 
				return (alternate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
			/*
      removeAll();
      invalidate();
      if (isSelected)
        {
          setForeground(table.getSelectionForeground());
          setBackground(table.getSelectionBackground());
        }
      else
        {
          setForeground(table.getForeground());
          setBackground(table.getBackground());
          }*/
			return (super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
		}
	}

	private class OpManager extends NCCPOpManager
	{

		public OpManager()
		{
			super((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC));
			response = new FilterDescriptor.NCCP_FilterResponse();
		}
		protected void setRendMarker(NCCP.RequesterBase r, NCCPOpManager.Operation op, int i)
		{
			if (r instanceof FilterDescriptor.NCCP_FilterRequester)
			{
				FilterDescriptor.NCCP_FilterRequester req = (FilterDescriptor.NCCP_FilterRequester)r;
				req.setMarker(new FilterDescriptor.REND_Marker(req.getOp(), port, req.getFilter(), ingress, i));
			}
			else
				r.setMarker(new REND_Marker_class(port.getID(), ingress, r.getOp(), i));
		}
	}



	/////////////////////////////////////////////// FilterTable.TableModel ////////////////////////////////////////////////////////////////
	//class TableModel
	private class TableModel extends AbstractTableModel implements ONLTableCellRenderer.CommittableTable
	{
		private FilterTable filterTable = null;

		private Class col_types[] = null;
		private int colFieldMap[] = null;
		private int fieldColMap[] = null;
		private int numCols = EMSTATS_COL;
		//column index
		protected static final int ADDRTYPE_COL = 0;
		protected static final int SRCADDR_COL = 1;
		protected static final int SRCPORT_COL = 2;
		protected static final int PROTOCOL_COL = 3;
		protected static final int GMNEGATE_COL = 4;
		protected static final int GM_ADD = 1;
		//add GM_ADD to following to make up for NEGATE for GM
		protected static final int FKEY_COL = 4;
		protected static final int QID_COL = 4;  
		protected static final int PBINDING_COL = 5; 
		//stop adding 
		protected static final int DROP_COL = 6;
		protected static final int EMSTATS_COL = 6;
		protected static final int ENABLE_COL = 7;
		protected static final int GMMIRROR_COL = 8;
		protected static final int GMDROP_COL = 9;
		protected static final int GMSTATS_COL = 17;
		protected static final int GMPRIORITY_COL = 7;
		protected static final int GMENABLE_COL = 18;
		protected static final int GMSAMPLING_COL = 10;
		protected static final int GMTCPF_COL = 11;
		protected static final int GMTCPFMASK_COL = 12;
		protected static final int GMTCPFIN_COL = 11;
		protected static final int GMTCPSYN_COL = 12;
		protected static final int GMTCPRST_COL = 13;
		protected static final int GMTCPPSH_COL = 14;
		protected static final int GMTCPACK_COL = 15;
		protected static final int GMTCPURG_COL = 16;
		public TableModel(FilterTable ft)
		{
			super();
			filterTable = ft;     
			FilterDescriptor tmp_filter = new FilterDescriptor(filterTable.getFilterType(), filterTable.port, filterTable.ingress);
			col_types = new Class[NUMFIELDS];
			int tmp[] = new int[]{SRCADDR_COL,SRCADDR_COL,SRCPORT_COL,SRCPORT_COL,PROTOCOL_COL,FKEY_COL,PBINDING_COL,QID_COL,EMSTATS_COL,-1,GMMIRROR_COL,ADDRTYPE_COL,-1, -1, -1, GMSAMPLING_COL, GMTCPFIN_COL, GMTCPSYN_COL, GMTCPRST_COL, GMTCPPSH_COL, GMTCPACK_COL, GMTCPURG_COL} ;  
			fieldColMap = tmp;
			int gm_add = 0;


			if (filterTable.getType() != ONLComponent.IPPEMFTABLE && filterTable.getType() != ONLComponent.OPPEMFTABLE) 
			{
				numCols = GMNUMCOLS;
				colFieldMap = new int[numCols];
				fieldColMap[QID] = QID_COL+1;
				fieldColMap[FORWARDKEY] = FKEY_COL+1;
				fieldColMap[PBINDING] = PBINDING_COL+1;
				fieldColMap[DROP] = GMDROP_COL;
				fieldColMap[STATS] = GMSTATS_COL;
				fieldColMap[NEGATE] = GMNEGATE_COL;
				fieldColMap[PRIORITY] = GMPRIORITY_COL;
				/*
	    colFieldMap[SRCADDR_COL] = SRCADDR;
	    colFieldMap[SRCPORT_COL] = SRCPORT;
	    colFieldMap[GMMIRROR_COL] = MIRROR;
	    colFieldMap[GMSTATS_COL] = STATS;
	    colFieldMap[GMPRIORITY_COL] = PRIORITY;
	    colFieldMap[GMNEGATE_COL] = NEGATE;
            colFieldMap[GMDROP_COL] = DROP;
            gm_add = GM_ADD;*/
			}
			else
			{
				numCols = EMNUMCOLS;
				colFieldMap = new int[numCols];

				/*
	    colFieldMap[SRCADDR_COL] = EMSRCADDR;
	    colFieldMap[SRCPORT_COL] = EMSRCPORT;
	    colFieldMap[EMSTATS_COL] = STATS;
            colFieldMap[DROP_COL] = DROP;
				 */
			}
			fieldColMap[ENABLE] = fieldColMap[STATS] + 1;
			if (filterTable.ingress) fieldColMap[QID] = -1;
			else fieldColMap[FORWARDKEY] = -1;

			int tmp_col;
			for (int i = 0; i < NUMFIELDS; ++i)
			{
				tmp_col = fieldColMap[i];
				if (tmp_col >= 0 && tmp_col < numCols)
					colFieldMap[tmp_col] = i;
			}
			/*
        colFieldMap[ADDRTYPE_COL] = ADDR_TYPE;
	colFieldMap[PBINDING_COL+gm_add] = PBINDING;
	if (filterTable.ingress) colFieldMap[FKEY_COL+gm_add] = FORWARDKEY;
	else colFieldMap[QID_COL+gm_add] = QID;
	colFieldMap[PROTOCOL_COL] = PROTOCOL;
			 */
			FilterDescriptor.TCPFlag tcpflag = new FilterDescriptor.TCPFlag();
			col_types[ADDR_TYPE] = String.class;
			col_types[PRIORITY] = Integer.class;
			col_types[MIRROR] = Boolean.class;
			if (tmp_filter.getSrcAddr() == null) ExpCoordinator.print("FilterTable fd srcAddr is null");
			col_types[SRCADDR] = tmp_filter.getSrcAddr().getClass();
			col_types[EMSRCADDR] = tmp_filter.getSrcAddr().getClass();
			col_types[SRCPORT] = tmp_filter.getSrcPort().getClass(); 
			col_types[EMSRCPORT] = tmp_filter.getSrcPort().getClass(); 
			col_types[PROTOCOL] = tmp_filter.getProtocol().getClass(); 
			col_types[FORWARDKEY] = tmp_filter.getFKey().getClass();
			col_types[PBINDING] = tmp_filter.getPluginBinding().getClass(); 
			col_types[QID] = tmp_filter.getQID().getClass(); 
			col_types[STATS] = tmp_filter.getStats().getClass();
			col_types[NEGATE] = Boolean.class;
			col_types[DROP] = Boolean.class;
			col_types[ENABLE] = Boolean.class;
			col_types[TCPFIN] = tcpflag.getClass();
			col_types[TCPSYN] = tcpflag.getClass();
			col_types[TCPRST] = tcpflag.getClass();
			col_types[TCPPSH] = tcpflag.getClass();
			col_types[TCPACK] = tcpflag.getClass();
			col_types[TCPURG] = tcpflag.getClass();
			//col_types[TCPFLAGS] = Integer.class;
			//col_types[TCPFLAGSMASK] = Integer.class;
			col_types[SAMPLING_TYPE] = tmp_filter.getSamplingType().getClass();
		}
		//TableModel
		public int getColumnCount() { return numCols;}
		public Class getColumnClass(int c) { return (col_types[(colFieldMap[c])]);}
		public int getRowCount() { return (2*(filterTable.filters.size()));}
		public String getColumnName(int ndx)
		{
			String nm = filterTable.fieldNames[(colFieldMap[ndx])];
			return nm;
		}
		public int getFilterField(int c, boolean is_src)
		{
			int field = colFieldMap[c];
			if (is_src)
			{
				if (field == EMSRCADDR) return SRCADDR;
				if (field == EMSRCPORT) return SRCPORT;
				return field;
			}
			else
			{    
				if (field == EMSRCADDR || field == SRCADDR) return (FilterDescriptor.DESTADDR);
				if (field == EMSRCPORT || field == SRCPORT) return (FilterDescriptor.DESTPORT);
			}
			return -1;
		}
		public boolean isCellEditable(int r, int c)     
		{
			if (ExpCoordinator.isObserver() || c == ADDRTYPE_COL) return false;
			//check if field set in filter if not let it be editable
			//if we're filling in a new filter can edit the prefix and the mask too
			FilterDescriptor rt = getFilterAtRow(r);//getfilterTable.getFilter(r);
			boolean is_src = isSrcRow(r);
			if (rt != null)
			{
				int field = getFilterField(c, is_src);
				if (field >= 0) return (rt.isEditable(field));
			}
			return false;
		}
		public int findColumn(String nm)
		{
			//first find the field type
			int field = filterTable.getFieldType(nm);
			int rtn = findColumnFromType(field);
			if (rtn < 0) return 0;
			return rtn;
		}
		public int findColumnFromType(int field)
		{
			int rtn = -1;
			if (field >= 0 && field < NUMFIELDS) rtn = fieldColMap[field];
			if (rtn < numCols) return rtn;
			return -1;
		}
		public void setValueAt(Object a, int r, int c)
		{
			if (c == ADDRTYPE_COL) return;
			FilterDescriptor rt = getFilterAtRow(r);//(FilterDescriptor)filterTable.filters.elementAt(r);
			//System.out.println("FilterTable::setValueAt class " + a.getClass().getName() + " value " + a);   
			boolean is_src = isSrcRow(r);
			int field = getFilterField(c, is_src);
			if (field >= 0) rt.setField(field, a);
			fireTableCellUpdated(r,c);
			fireTableRowsUpdated(c,c);
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_CHANGE, filterTable, rt, r, null, "filter changed"));
		}
		public Object getValueAt(int r, int c)
		{
			FilterDescriptor rt = getFilterAtRow(r);//(FilterDescriptor)filterTable.filters.elementAt(r);  
			boolean is_src = isSrcRow(r);
			if (c == ADDRTYPE_COL)
			{
				if (is_src) return ("src");
				else return ("dest");
			}
			int field = getFilterField(c, is_src);
			if (field >= 0) return (rt.getDisplayField(field));
			return null;
		}
		//TableModel
		// begin interface ONLTableCellRenderer.CommittableTable
		public ONLTableCellRenderer.Committable getCommittableAt(int row)
		{
			return ((ONLTableCellRenderer.Committable)getFilterAtRow(row));//filterTable.getFilter(row));
		}
		// end interface ONLTableCellRenderer.CommittableTable
		public void setColumnWidth(int field, int w, TableColumnModel tcm)
		{
			setColumnWidth(field, w, w, tcm);
		}
		public void setColumnWidth(int field, int minw, int pw, TableColumnModel tcm)
		{
			int col = findColumnFromType(field);
			if (col >= 0)
			{
				tcm.getColumn(col).setPreferredWidth(pw);
				tcm.getColumn(col).setMinWidth(minw);
			}
		}
		public void setColumnRenderer(int field, TableCellRenderer r, TableColumnModel tcm)    
		{
			int col = findColumnFromType(field);
			if (col >= 0)
			{
				tcm.getColumn(col).setCellRenderer(r);
			}
		}
		private FilterDescriptor getFilterAtRow(int r)
		{
			if (isSrcRow(r)) return ((FilterDescriptor)filterTable.filters.elementAt(r/2));
			else return ((FilterDescriptor)filterTable.filters.elementAt((r-1)/2));
		}
		private boolean isSrcRow(int r) { return ((r & 1) == 0);}
	}//end inner class TableModel


	//private class SelectionModel extends DefaultListSelectionModel
	//{
	// public SelectionModel() { super();}
	// public setSelectionInterval(int index0, int index1)
	// {
	//   int i0 = index0;
	//   int i1 = index1;
	//   if ((index0 & 1) > 0) //it's odd
	//       i0 = index0 - 1;
	//    if ((index0 & 1) == 0) //it's even
	//      i1 = index1 + 1;
	//    super.setSelectionInterval(i0, i1);
	//  }
	// }
	/*
  protected static class AddFilterAction extends ONL.UserAction //AbstractAction //implements ListSelectionListener
  {
    private FilterTable filterTable;
    private ExpCoordinator expCoordinator;
    public AddFilterAction(FilterTable rt, String ttl)
      {
	super(ttl, false, true);
	//super("Add Filter");
	filterTable = rt;
	expCoordinator = filterTable.port.getExpCoordinator();      
      }
    public void actionPerformed(ActionEvent e)
      {
        ExpCoordinator.print("FilterTable.AddFilterAction.actionPerformed", 5);
	FilterDescriptor nfilter = filterTable.addNewFilter();
	Experiment exp = expCoordinator.getCurrentExp();
	if (!exp.containsParam(filterTable)) exp.addParam(filterTable);
	expCoordinator.addEdit(new FilterDescriptor.AddEdit(filterTable.port, filterTable.ingress, nfilter, filterTable, expCoordinator.getCurrentExp()));
      }
  } 
	 */

	protected static class AddFilterAction extends ONL.UserAction //implements ListSelectionListener
	{
		private FilterTable filterTable;
		private ExpCoordinator expCoordinator;
		public AddFilterAction(FilterTable ft, String ttl)
		{
			super(ttl, false, true);
			filterTable = ft;
			expCoordinator = ExpCoordinator.theCoordinator;      
		}
		public void actionPerformed(ActionEvent e)
		{
			//pop up window to set new filter
			ExpCoordinator.print("FilterTable.AddFilterAction.actionPerformed", 5);
			boolean in = true;
			if (filterTable.getType() == ONLComponent.OPPEMFTABLE ||
					filterTable.getType() == ONLComponent.OPPGMFTABLE ||
					filterTable.getType() == ONLComponent.OPPEXGMFTABLE)
				in = false;
			EditFilterWindow ewindow = new EditFilterWindow(new FilterDescriptor(filterTable.getFilterType(), filterTable.port, in), "Add Filter", "Add", "Cancel");
			FilterDescriptor nfilter = ewindow.edit();
			if (nfilter != null)
				nfilter = (FilterDescriptor)filterTable.addNewFilter(nfilter);
			if (nfilter != null)
			{
				Experiment exp = expCoordinator.getCurrentExp();
				if (!exp.containsParam(filterTable)) exp.addParam(filterTable);
			}
		}
	}

	protected static class EditFilterAction extends ONL.UserAction //implements ListSelectionListener
	{
		private FilterTable filterTable;
		private ExpCoordinator expCoordinator;
		public EditFilterAction(FilterTable ft, String ttl)
		{
			super(ttl, false, true);
			filterTable = ft;
			expCoordinator = ExpCoordinator.theCoordinator;      
		}
		public void actionPerformed(ActionEvent e)
		{
			//pop up window to set new filter
			ExpCoordinator.print("FilterTable.EditFilterAction.actionPerformed", 5);

			FilterDescriptor filter = filterTable.getSelectedFilter();
			if (filter == null) return;
			EditFilterWindow ewindow = new EditFilterWindow(filter, "Edit Filter");
			FilterDescriptor nfilter = ewindow.edit();
			if (nfilter != null)
				filterTable.changeFilter(nfilter);
		}
	}
	protected static class DeleteFilterAction extends ONL.UserAction 
	{
		//private FilterTable filterTable;
		private ExpCoordinator expCoordinator;
		//private JTable jtable;
		private FTAction ftaction = null;

		public DeleteFilterAction(FTAction fa, String ttl)//FilterTable rt, JTable jt, String ttl)
		{
			super(ttl, false, true);
			//super("Delete Filter");
			//filterTable = rt;
			//jtable = jt;
			ftaction = fa;
			expCoordinator = ftaction.getExpCoordinator();
		}

		public void actionPerformed(ActionEvent e)
		{
			FilterTable filterTable = ftaction.getCurrentChoice();
			if (filterTable == null) return;
			Vector rts = filterTable.getSelectedFilters();
			int max = rts.size();
			for (int i = 0; i < max; ++i)
			{
				filterTable.removeFilter((FilterDescriptor)rts.elementAt(i));
			}

			expCoordinator.addEdit(new FilterDescriptor.DeleteEdit(filterTable.port, filterTable.ingress, rts, filterTable, expCoordinator.getCurrentExp()));
		}
	}
	public static class FTAction extends AbstractAction 
	{
		private NSPPort port = null;
		private JInternalFrame frame = null;
		private FilterTable emFTable = null;
		private FilterTable gmFTable = null;
		private boolean ingress = true;
		private TableListener tlistener = null;

		private class TableListener extends FocusAdapter implements TableModelListener
		{
			private FilterTable currentChoice = null;
			public TableListener()
			{
				super();
			}
			public void focusGained(FocusEvent e)
			{
				//System.out.println("FTAction.TableListener.focusGained");
				if (emFTable.jtable == e.getSource()) setCurrentChoice(emFTable);
				else setCurrentChoice(gmFTable);
			}
			public void tableChanged(TableModelEvent e)
			{
				//System.out.println("FTAction.TableListener.tableModelChanged");
				if (gmFTable.size() == 0 || (emFTable.size() > 0 && emFTable.tableModel == e.getSource())) 
					setCurrentChoice(emFTable);
				else setCurrentChoice(gmFTable);
			}
			public FilterTable getCurrentChoice() { return currentChoice;}
			private void setCurrentChoice(FilterTable ft)
			{
				FilterTable oft = currentChoice;
				currentChoice = ft;
				if (oft != null && oft != currentChoice) oft.jtable.clearSelection();
				//if (currentChoice != null) System.out.println("FTAction.TableListener.setCurrentChoice " + currentChoice.getLabel());
			}

		}

		public FTAction(String ttl, NSPPort p, boolean in) 
		{
			super(ttl);
			port = p;
			ingress = in;
			tlistener = new TableListener();
			if (ingress)
			{
				emFTable = port.getFilterTable(ONLComponent.IPPEMFTABLE);
				gmFTable = port.getFilterTable(ONLComponent.IPPEXGMFTABLE);   
			}   
			else
			{
				emFTable = port.getFilterTable(ONLComponent.OPPEMFTABLE);
				gmFTable = port.getFilterTable(ONLComponent.OPPEXGMFTABLE);   
			}   
		}
		public FTAction(NSPPort p, boolean in) 
		{
			this("Filter Table", p, in);
		}
		public void actionPerformed(ActionEvent e) 
		{
			if (emFTable == null)
			{
				//System.out.println("FTAction.actionPerformed Exact Match filterTable is null");   
				if (ingress)
					emFTable = port.getFilterTable(ONLComponent.IPPEMFTABLE);
				else 
					emFTable = port.getFilterTable(ONLComponent.OPPEMFTABLE);
			}
			if (gmFTable == null) 
			{
				//System.out.println("FTAction.actionPerformed egress filterTable is null"); 
				if (ingress)
					gmFTable = port.getFilterTable(ONLComponent.IPPEXGMFTABLE);
				else
					gmFTable = port.getFilterTable(ONLComponent.OPPEXGMFTABLE);
			}
			if (frame == null)
			{
				String name = (String)getValue(Action.NAME);
				frame = new JInternalFrame(new String(port.getLabel() + "  " + name));
				//String name = (String)getValue(Action.NAME);
				JPanel pane = new JPanel();
				pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
				JPanel f_panel = emFTable.getPanel("Exact Match");
				emFTable.jtable.addFocusListener(tlistener);
				emFTable.tableModel.addTableModelListener(tlistener);
				//emFTable.jtable.addMouseListener(tlistener);
				pane.add(f_panel);
				f_panel = gmFTable.getPanel("General Match");
				gmFTable.jtable.addFocusListener(tlistener);
				gmFTable.tableModel.addTableModelListener(tlistener);
				//gmFTable.jtable.addMouseListener(tlistener);
				//f_panel.addMouseListener(tlistener);
				pane.add(f_panel);
				frame.getContentPane().add(pane);
				//frame = filterTable.getPanel(name);
				JMenuBar mb = new JMenuBar();
				frame.setJMenuBar(mb);
				JMenu menu = new JMenu("Edit");
				mb.add(menu);
				menu.add(new AddFilterAction(emFTable, "Add Exact Match Filter"));
				menu.add(new EditFilterAction(emFTable, "Edit Exact Match Filter"));
				//menu.add(new DeleteFilterAction(emFTable, emFTable.jtable, "Delete Exact Match Filter"));
				//menu.add(new StopStatsAction(emFTable, emFTable.jtable, "Stop Exact Match Stats"));
				menu.add(new AddFilterAction(gmFTable, "Add General Match Filter"));
				menu.add(new EditFilterAction(gmFTable, "Edit General Match Filter"));
				menu.add(port.getSamplingAction());
				menu.add(new DeleteFilterAction(this, "Delete Filter"));
				menu.add(new StopStatsAction(this, "Stop Stats"));
				port.addToDesktop(frame);
				frame.addInternalFrameListener(new ONLComponent.ONLInternalFrameListener(emFTable)); 
				//frame.addInternalFrameListener(new ONLComponent.ONLInternalFrameListener(gmFTable)); 
			}
			frame.setSize(850,250);
			//frame.pack();
			port.showDesktop(frame);
		}

		public void clear()      {
			if (frame != null && frame.isShowing()) frame.dispose();
			if (emFTable != null) emFTable.clear();
			if (gmFTable != null) gmFTable.clear();
		}
		public FilterTable getCurrentChoice() { return (tlistener.getCurrentChoice());}
		public ExpCoordinator getExpCoordinator() { return (port.getExpCoordinator());}
	}//end FTAction


	protected static class StopStatsAction extends ONL.UserAction 
	{
		//private FilterTable filterTable;
		//private JTable jtable;
		private FTAction ftaction = null;

		public StopStatsAction(FTAction fa, String ttl)
		{
			super(ttl, false, true);
			//super("Stop Stats");
			//filterTable = rt;
			//jtable = jt;
			ftaction = fa;
		}

		public void actionPerformed(ActionEvent e)
		{
			FilterTable filterTable = ftaction.getCurrentChoice();
			if (filterTable == null) return;
			Vector filters = filterTable.getSelectedFilters();
			int max = filters.size();
			FilterDescriptor fd;
			int i = 0;
			boolean confirmed = false;
			for (i = 0; i < max; ++i)
			{
				fd = (FilterDescriptor)filters.elementAt(i);
				if (fd != null && fd.getStats() != null && ((fd.getStats().getState() & PortStats.PERIODIC) > 0))
				{
					if (!confirmed)
					{
						//ask user for confirmation that they really want to stop
						int rtn = JOptionPane.showConfirmDialog(filterTable.jtable, "Do you really want to stop statistics polling?");
						//if no return else continue
						if (rtn == JOptionPane.OK_OPTION)
							confirmed = true;
						else return;
					}
					fd.getStats().setState(PortStats.STOP);
				}
			}
		}
	}


	public FilterTable(String lbl, String tp, ONL.Reader tr, ExpCoordinator ec) throws IOException
	{
		super(lbl,tp,tr,ec);
		setParentFromString(tr.readString());
		ingress = (Boolean.valueOf(tr.readString())).booleanValue();
		initFields((NSPPort)getParent());
		read(tr);
		initLabel();
		setFilterType(tp);
	}

	public FilterTable(NSPPort p, boolean in, String tp)
	{
		super(p, "FT", tp, p.getExpCoordinator());
		ingress = in;
		initFields(p);
		initLabel();
		setFilterType(tp);
	}

	private void initLabel()
	{
		String nm = "FT";
		String desc = "Filter Table";
		if (ingress)
		{
			nm = "in";
			desc = " Input ";
		}
		else
		{
			nm = "out";
			desc = " Output ";
		}
		if (getType() != ONLComponent.IPPEMFTABLE && getType() != ONLComponent.OPPEMFTABLE)
		{
			nm = nm.concat("FiltT");
			desc = desc.concat("Filter Table");
		}
		else 
		{
			nm = nm.concat("FlowT");
			desc = desc.concat("Flow Table");
		}
		//setLabel(nm);
		addToDescription(1, "Port " + port.getLabel() + desc);
	}
	private void initFields(NSPPort p)
	{
		String[] fnames = {"address/mask",
				"address",
				"port[-port]",
				"port",
				"protocol",
				"forward to/voq",
				"spc qid",
				"qid",
				"stats",
				"priority",
				"aux",
				"",
				"!",
				"drop",
				"on",
				"sampling",
				"tcpfin",
				"tcpsyn",
				"tcprst",
				"tcppsh",
				"tcpack",
		"tcpurg"}; 
		fieldNames = fnames;
		port = p;
		nsp = (NSPDescriptor)port.getParent();
		filters = new Vector();
		tableModel = new TableModel(this);
		priority = new Priority("priority:", this, NCCP.Operation_EMFPriority, "emf table priority", ingress);
		priority.setObserveAction(false, true);
		priority.setValue(56);
		if (getType() == IPPGMFTABLE || getType() == IPPEXGMFTABLE || getType() == OPPGMFTABLE || getType() == OPPEXGMFTABLE)
		{
			if (samplingTypes == null) samplingTypes = port.getSamplingTypes();
			port.addPropertyListener(this);
		}
		else
		{
			addStateListener(PRIORITY_PROP);
		}
	}
	protected static void skip(ONL.Reader tr) throws IOException
	{
		tr.readInt();
		int numFilters = tr.readInt();
		for (int i = 0; i < numFilters; ++i) tr.readString();
	}
	protected void read(ONL.Reader tr) throws IOException
	{
		setPriority(tr.readInt());
		int numFilters = tr.readInt();
		Vector tmp_filters = new Vector();
		for (int i = 0; i < numFilters; ++i)
		{
			try
			{
				FilterDescriptor tmp_fltr = addFilter(new FilterDescriptor(port, ingress, tr.readString()));
				if (tmp_fltr != null) //!containsFilter(tmp_fltr))
				{
					tmp_filters.add(tmp_fltr);
					//tmp_fltr.resetUpdateEdit(this);
					//addFilter(tmp_fltr);
				}
			}
			catch (java.text.ParseException e)
			{
				System.err.println("Error:- FilterTable.read ParseException bad filter. " + e.getMessage());
			}
		}
		if (!tmp_filters.isEmpty()) 	
		{
			Experiment exp = expCoordinator.getCurrentExp();
			if (!exp.containsParam(this)) exp.addParam(this);
			ExpCoordinator.print("FilterTable.read", 5);
			expCoordinator.addEdit(new FilterDescriptor.AddEdit(port, ingress, tmp_filters, this, exp));
		}
	}
	public void writeExpDescription(ONL.Writer tw) throws IOException 
	{ 
		//System.out.println("FT::writeExpDescription");
		super.writeExpDescription(tw);
		writeTable(tw);
	}
	public void writeTable(ONL.Writer wr) throws IOException
	{
		wr.writeString(Boolean.toString(ingress));
		wr.writeInt(priority.getPriority());
		int numFilters = filters.size();
		Vector to_write = new Vector();
		for (int i = 0; i < numFilters; ++i)
		{
			FilterDescriptor fltr = (FilterDescriptor)filters.elementAt(i);
			if (fltr.isEnabled()) to_write.add(fltr);
		}
		numFilters = to_write.size();
		wr.writeInt(numFilters);
		for (int i = 0; i < numFilters; ++i)
		{
			FilterDescriptor fltr = (FilterDescriptor)to_write.remove(0);
			if (fltr.isEnabled()) wr.writeString(fltr.toString());
		}
	}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		String tmp_nm = "out";
		if (ingress) tmp_nm = "in";
		if (getType() == ONLComponent.IPPEMFTABLE || getType() == ONLComponent.OPPEMFTABLE)
			tmp_nm = tmp_nm.concat("EM");
		else
			tmp_nm = tmp_nm.concat("GM");
		tmp_nm = tmp_nm.concat(ExperimentXML.FILTER_TABLE);//"FilterTable");
		xmlWrtr.writeStartElement(tmp_nm);
		xmlWrtr.writeAttribute(ExperimentXML.FILTER_TYPE, String.valueOf(getType()));
		if (getType() == ONLComponent.IPPEMFTABLE || getType() == ONLComponent.OPPEMFTABLE)
			xmlWrtr.writeAttribute(ExperimentXML.PRIORITY, String.valueOf(priority.getPriority()));
		//xmlWrtr.writeCharacters("\n");
		int numFilters = filters.size();
		for (int i = 0; i < numFilters; ++i)
		{
			FilterDescriptor fltr = (FilterDescriptor)filters.elementAt(i);
			if (fltr.isEnabled()) fltr.writeXML(xmlWrtr);
		}
		xmlWrtr.writeEndElement();
		//xmlWrtr.writeCharacters("\n");
	}
	public boolean containsFilter(FilterDescriptor r)
	{
		if (filters.contains(r)) return true;
		int max = filters.size();
		FilterDescriptor elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (FilterDescriptor)filters.elementAt(i);
			if (elem.isIDEqual(r)) return true;
		}
		return false;
	}
	public FilterDescriptor getSelectedFilter()
	{
		//int ndxs[] = jtable.getSelectedRows();
		return (getFilterFromRow(jtable.getSelectedRow()));
	}
	public Vector getSelectedFilters()
	{
		int ndxs[] = jtable.getSelectedRows();
		int max = Array.getLength(ndxs);
		Vector rts = new Vector(max);
		int i = 0;
		FilterDescriptor fdes;
		for (i = 0; i < max; ++i)
		{
			fdes = getFilterFromRow(ndxs[i]);
			if (!rts.contains(fdes))
				rts.add(fdes);
		}
		return rts;
	}
	private FilterDescriptor getFilterFromRow(int row)
	{
		if ((row & 1) == 0) return (getFilter(row/2));
		else return (getFilter((row-1)/2));
	}
	private int getIndex(FilterDescriptor r)
	{
		int max = filters.size();
		FilterDescriptor elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (FilterDescriptor)filters.elementAt(i);
			if (elem == r || elem.isIDEqual(r)) return i;
		}
		return -1;
	}
	public FilterDescriptor getFilter(FilterDescriptor fd)
	{
		int ndx = getIndex(fd);
		if (ndx >= 0) return((FilterDescriptor)filters.elementAt(ndx));
		else return null;
	}
	public void changeFilter(FilterDescriptor r)
	{
		if (filters.contains(r))
		{
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_CHANGE, this, r, getIndex(r), null, "filter changed")); 
			tableModel.fireTableDataChanged();
		}
	}
	public ONLCTable.Element addElement(ONLCTable.Element elem, boolean can_repeat) { return (addFilter((FilterDescriptor)elem, filters.size(), can_repeat));}
	public FilterDescriptor addFilter(FilterDescriptor r) { return(addFilter(r, filters.size(), false));}
	public FilterDescriptor addFilter(FilterDescriptor r, int s) { return (addFilter(r,s,false));}
	public FilterDescriptor addFilter(FilterDescriptor r, int s, boolean can_repeat)
	{
		ExpCoordinator.print("FilterTable.addFilter", 5);
		if (r != null && (!containsFilter(r) || can_repeat)) 
		{
			FilterDescriptor fd = new FilterDescriptor(r);
			if (!ingress) fd.setFKey(new FilterDescriptor.ForwardKey(port.getID()));
			int p = fd.getFKey().getPort();
			if (p != FilterDescriptor.ForwardKey.USE_ROUTE)
				nsp.getPort(p).addPropertyListener(NSPPort.SUBPORT, fd);
			fd.resetUpdateEdit(this);
			filters.add(fd);
			fd.addListener(this);
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_ADD, this, r, s, null, "filter added")); 
			if (panel != null && panel.isVisible() && filters.size() == 1) fireEvent(new ONLComponent.Event(ONLComponent.Event.GET_FOCUS, this, "Get Focus"));
			tableModel.fireTableRowsInserted(s,s);
			ExpCoordinator.print(fd.toString(), 5);
			port.addStat(r.getStats());  
			if (ExpCoordinator.isRecording())
				ExpCoordinator.recordEvent(SessionRecorder.ADD_FILTER, new String[]{port.getLabel(), fd.getRecordedString()});
			return fd;
		}
		else return null;
	}
	private FilterDescriptor addNewFilter(FilterDescriptor f)
	{
		FilterDescriptor nf = addFilter(f);
		expCoordinator.addEdit(new FilterDescriptor.AddEdit(port, ingress, nf, this, expCoordinator.getCurrentExp()));
		return nf;
	}
	private FilterDescriptor addNewFilter()
	{
		ExpCoordinator.print("FilterTable.addNewFilter", 5);
		int s = filters.size();
		FilterDescriptor fd = new FilterDescriptor(filterType, port, ingress);
		if (!ingress) 
		{
			fd.setFKey(new FilterDescriptor.ForwardKey(port.getID()));
			port.addPropertyListener(NSPPort.SUBPORT, fd);
		}
		port.addStat(fd.getStats());
		fd.resetUpdateEdit(this);
		filters.add(fd);
		fd.addListener(this);
		if (jtable != null) jtable.setRowSelectionInterval(s,s);
		fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_ADD, this, fd, s, null, "new filter added")); 
		tableModel.fireTableRowsInserted(s,s);
		if (ExpCoordinator.isRecording())
			ExpCoordinator.recordEvent(SessionRecorder.ADD_FILTER, new String[]{port.getLabel(), fd.getRecordedString()});
		return fd;
	}
	public void removeFilter(FilterDescriptor r)
	{
		if (r!= null && containsFilter(r))
		{
			int max = filters.size();
			FilterDescriptor elem = null;
			int removed = -1;
			for (int i = 0; i < max; ++i)
			{
				elem = (FilterDescriptor) filters.elementAt(i);
				if ((elem == r) || elem.isIDEqual(r)) 
				{
					removed = i;
					break;
				}
			}
			if (removed >= 0) //should always be true
			{
				port.removeStat(elem.getStats());
				filters.remove(elem);
				elem.removeListener(this);
				int p = elem.getFKey().getPort();
				if (p != FilterDescriptor.ForwardKey.USE_ROUTE)
					nsp.getPort(p).removePropertyListener(NSPPort.SUBPORT, elem);
				fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_DELETE, this, r, removed, null, "filter removed")); 
				tableModel.fireTableRowsDeleted(removed,removed); 
				if (ExpCoordinator.isRecording())
				{
					String ingress_str = "ingress";
					if (!ingress) ingress_str = "egress";
					String tp = "exact match";
					if (getType() != ONLComponent.IPPEMFTABLE && getType() != ONLComponent.OPPEMFTABLE) tp = "general match";
					ExpCoordinator.recordEvent(SessionRecorder.DELETE_FILTER, new String[]{port.getLabel(), elem.getRecordedString()});
				}
			}
		}
	}
	public ONLCTable.Element getElement(int i) { return (getFilter(i));}
	public ONLCTable.Element getElementExact(Object o)
	{ 
		int max = filters.size();
		ONLCTable.Element elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (ONLCTable.Element)filters.elementAt(i);
			if (elem.toString().equals(o.toString())) return elem;
		}
		return null;
	}
	public FilterDescriptor getFilter(int i) 
	{ 
		if (i >= 0 && i < filters.size())
		{
			return ((FilterDescriptor)filters.elementAt(i));
		}
		return null;
	}
	public int size() { return (filters.size());}

	public int getPriority() { return (priority.getPriority());}
	public void setPriority(int p) 
	{ 
		priority.setValue(p);
		ExpCoordinator.print("FilterTable.setPriority", 5);
		if (ExpCoordinator.isRecording())
		{
			String ingress_str = "ingress";
			if (!ingress) ingress_str = "egress";
			ExpCoordinator.recordEvent(SessionRecorder.PRIORITY_EMF_TABLE, new String[]{getPort().getLabel(), ingress_str, String.valueOf(p)});
		}
		priority.addEditForCommit();
	}
	private void setJTable(JTable t) { jtable = t;}

	//FilterDescriptor.Listener
	public void changedPBinding(FilterDescriptor.Event e)
	{
		valueChanged(PBINDING, (FilterDescriptor)e.getSource());
	}
	public void changedQID(FilterDescriptor.Event e)
	{
		valueChanged(QID, (FilterDescriptor)e.getSource());
	}
	public void changedStats(FilterDescriptor.Event e)
	{
		valueChanged(STATS, (FilterDescriptor)e.getSource());
	}
	public void changedData(FilterDescriptor.Event e)
	{
		valueChanged(-1, (FilterDescriptor)e.getSource());
	}
	//end FilterDescriptor.Listener

	private void valueChanged(int field, FilterDescriptor fd)
	{
		int r = getIndex(fd); 
		int c = tableModel.findColumnFromType(field);
		if (c >= 0)
		{
			if (r >= 0) tableModel.fireTableCellUpdated(r,c);
			else tableModel.fireTableRowsUpdated(r,r);
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_CHANGE, this, fd, r, null, "filter changed"));
		}
	} 

	private TableModel getTableModel() { return tableModel;} 
	private JPanel getPanel(String name) 
	{
		if (panel == null)
		{
			ListSelectionModel selectionModel = new DefaultListSelectionModel(){
				public void setSelectionInterval(int index0, int index1)
				{
					int i0 = index0;
					int i1 = index1;
					if ((index0 & 1) > 0) //it's odd
						i0 = index0 - 1;
					if ((index1 & 1) == 0) //it's even
						i1 = index1 + 1;
					super.setSelectionInterval(i0, i1);
				}
				public void addSelectionInterval(int index0, int index1)
				{
					int i0 = index0;
					int i1 = index1;
					if ((index0 & 1) > 0) //it's odd
						i0 = index0 - 1;
					if ((index1 & 1) == 0) //it's even
						i1 = index1 + 1;
					super.addSelectionInterval(i0, i1);
				}
				public void setLeadSelectionIndex(int index)
				{
					super.setLeadSelectionIndex(index);
					//System.out.println("setLeadSel");
				}
				public void setAnchorSelectionIndex(int index)
				{
					super.setAnchorSelectionIndex(index);
					//System.out.println("setAnchorSel");
				}
			};
			//    public void setSelectionInterval(int in0, int in1)
			//  {
			//    if ((in0 & 1)
			//  }
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			String ttl = name;
			//if (getType() == ONLComponent.EMFTABLE) ttl = new String(name + " (" + getPriority()  + ")"); 
			//else ttl = new String(port.getLabel() + name);
			Border border = BorderFactory.createTitledBorder((BorderFactory.createLineBorder(Color.black)),ttl);
			panel.setBorder(border); 
			if (getType() == ONLComponent.IPPEMFTABLE || getType() == ONLComponent.OPPEMFTABLE) 
				panel.add(priority);
			jtable = new JTable(tableModel);
			jtable.setSelectionModel(selectionModel);
			FilterDescriptor tmp_filter = new FilterDescriptor(getFilterType(), port, ingress);
			JFormattedTextField jftf = new JFormattedTextField(tmp_filter.getSrcAddr());
			jftf.setFocusLostBehavior(JFormattedTextField.PERSIST);
			AlternatingRenderer bool_renderer = new AlternatingRenderer(false, jtable.getDefaultRenderer(Boolean.class));
			jtable.setDefaultRenderer(Boolean.class, bool_renderer);
			jtable.setDefaultRenderer(String.class, new ONLTableCellRenderer());
			for (int col = 0; col < NUMFIELDS; ++col)
			{
				if (col != STATS && tableModel.col_types[col] != Boolean.class)
					jtable.setDefaultRenderer(tableModel.col_types[col], new ONLTableCellRenderer());
			}
			jtable.setDefaultEditor(tableModel.col_types[SRCADDR], new DefaultCellEditor(jftf));
			jtable.setDefaultEditor(tableModel.col_types[SRCPORT], new DefaultCellEditor(new JFormattedTextField(tmp_filter.getSrcPort())));
			jtable.setDefaultEditor(tableModel.col_types[QID], new DefaultCellEditor(new JFormattedTextField(tmp_filter.getQID()))); 
			FilterDescriptor.Protocol protocols[];
			if (getType() == ONLComponent.IPPEMFTABLE || getType() == ONLComponent.OPPEMFTABLE)
			{
				FilterDescriptor.Protocol tmp_protocols[]= {new FilterDescriptor.EMProtocol(FilterDescriptor.Protocol.TCP), new FilterDescriptor.EMProtocol(FilterDescriptor.Protocol.UDP)};
				protocols = tmp_protocols;
			}
			else 
			{
				FilterDescriptor.Protocol tmp_protocols[]= {new FilterDescriptor.GMProtocol(FilterDescriptor.Protocol.TCP), new FilterDescriptor.GMProtocol(FilterDescriptor.Protocol.UDP)};
				protocols = tmp_protocols;
				JComboBox cb = new JComboBox(port.getSamplingTypes());
				cb.setEditable(false);
				jtable.setDefaultEditor(tableModel.col_types[SAMPLING_TYPE], new DefaultCellEditor(cb));
			}
			JComboBox cb = new JComboBox(protocols);
			cb.setEditable(true);
			jtable.setDefaultEditor(tableModel.col_types[PROTOCOL], new DefaultCellEditor(cb));
			//create combo box for forward key
			FilterDescriptor.ForwardKey fkeys[] = { new FilterDescriptor.ForwardKey(0,0),
					new FilterDescriptor.ForwardKey(1,0),
					new FilterDescriptor.ForwardKey(2,0),
					new FilterDescriptor.ForwardKey(3,0),
					new FilterDescriptor.ForwardKey(4,0),
					new FilterDescriptor.ForwardKey(5,0),
					new FilterDescriptor.ForwardKey(6,0),
					new FilterDescriptor.ForwardKey(7,0),
					new FilterDescriptor.ForwardKey(FilterDescriptor.ForwardKey.USE_ROUTE,0)};
			cb = new JComboBox(fkeys);
			if (getType() == ONLComponent.IPPEMFTABLE)
				cb.setEditable(true);
			else
				cb.setEditable(false);
			//jtable.setDefaultEditor(tableModel.col_types[FORWARDKEY], new DefaultCellEditor(new JFormattedTextField(tmp_filter.getFKey())));
			jtable.setDefaultEditor(tableModel.col_types[FORWARDKEY], new DefaultCellEditor(cb));
			FilterDescriptor.PluginBinding ftags[]= {new FilterDescriptor.NoPluginPBinding(), new FilterDescriptor.PluginPBinding()};
			cb = new JComboBox(ftags);
			cb.setEditable(true);
			jtable.setDefaultEditor(tableModel.col_types[PBINDING], new DefaultCellEditor(cb));
			FilterDescriptor.TCPFlag tcpflags[] = {new FilterDescriptor.TCPFlag(0,0), new FilterDescriptor.TCPFlag(1,1), new FilterDescriptor.TCPFlag(0,1)};
			cb = new JComboBox(tcpflags);
			cb.setEditable(true);
			jtable.setDefaultEditor(tableModel.col_types[TCPFIN], new DefaultCellEditor(cb));
			try
			{
				FilterDescriptor.Stats stats[] = { new FilterDescriptor.Stats(PortStats.ONE_SHOT_STRING), new FilterDescriptor.Stats(PortStats.PERIODIC_STRING), new FilterDescriptor.Stats(PortStats.MONITOR_STRING)};
				cb = new JComboBox(stats);
				jtable.setDefaultEditor(tableModel.col_types[STATS], new DefaultCellEditor(cb));
			}
			catch (java.text.ParseException e2)
			{
			}
			/*
	  JMenuBar mb = new JMenuBar();
	  panel.setJMenuBar(mb);
	  JMenu menu = new JMenu("Edit");
	  mb.add(menu);
	  AddFilterAction add_action = new AddFilterAction(this);
	  menu.add(add_action);
	  menu.add(new DeleteFilterAction(this, jtable));
	  menu.add(new StopStatsAction(this, jtable));
			 */

			setJTable(jtable);
			//turn off column selection
			jtable.setColumnSelectionAllowed(false);
			//jtable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			jtable.setShowVerticalLines(true);
			TableColumnModel tcm = jtable.getColumnModel();
			int max = tableModel.getColumnCount();
			ONLCTable.MultiLineHeader mlh = new ONLCTable.MultiLineHeader();
			//tcm.getColumn(tableModel.FKEY_COL).setHeaderRenderer(mlh);
			if (ingress)
			{
				String[] tmp_strarray = {"forward to", "voq"};
				tcm.getColumn(tableModel.findColumnFromType(FORWARDKEY)).setHeaderValue(tmp_strarray);
			}
			if (getType() != ONLComponent.IPPEMFTABLE &&getType() != ONLComponent.OPPEMFTABLE)
			{
				String[] tmp_fin = {"F","I","N"};
				tcm.getColumn(tableModel.findColumnFromType(TCPFIN)).setHeaderValue(tmp_fin);
				String[] tmp_syn = {"S","Y","N"};
				tcm.getColumn(tableModel.findColumnFromType(TCPSYN)).setHeaderValue(tmp_syn);
				String[] tmp_rst = {"R","S","T"};
				tcm.getColumn(tableModel.findColumnFromType(TCPRST)).setHeaderValue(tmp_rst);
				String[] tmp_psh = {"P","S","H"};
				tcm.getColumn(tableModel.findColumnFromType(TCPPSH)).setHeaderValue(tmp_psh);
				String[] tmp_ack = {"A","C","K"};
				tcm.getColumn(tableModel.findColumnFromType(TCPACK)).setHeaderValue(tmp_ack);
				String[] tmp_urg = {"U","R","G"};
				tcm.getColumn(tableModel.findColumnFromType(TCPURG)).setHeaderValue(tmp_urg);
			}
			//tcm.getColumn(tableModel.PBINDING_COL).setHeaderRenderer(mlh);
			//String[] tmp_strarray2 = {"spc", "qid"};
			//tcm.getColumn(tableModel.PBINDING_COL).setHeaderValue(tmp_strarray2);
			for (int i = 1; i < max; ++i)
			{
				tcm.getColumn(i).setHeaderRenderer(mlh);
			}

			ONLCTable.MultiLineHeader mlh2 = new ONLCTable.MultiLineHeader();
			mlh2.setBorder(null);
			tcm.getColumn(0).setHeaderRenderer(mlh2);
			tableModel.setColumnWidth(ADDR_TYPE, 30, tcm);
			tableModel.setColumnWidth(SRCADDR, 120, 130, tcm);
			tableModel.setColumnWidth(SRCPORT, 75, 120, tcm);
			tableModel.setColumnWidth(PROTOCOL, 53, tcm);
			//tableModel.setColumnWidth(FORWARDKEY, 85, tcm);
			tableModel.setColumnWidth(FORWARDKEY, 65, tcm);
			//tableModel.setColumnWidth(PBINDING, 50, 85, tcm);
			tableModel.setColumnWidth(PBINDING, 60, tcm);
			tableModel.setColumnWidth(QID, 50, tcm);
			tableModel.setColumnWidth(STATS, 100, tcm);
			tableModel.setColumnWidth(PRIORITY, 50, tcm);
			tableModel.setColumnWidth(MIRROR, 30, tcm);
			tableModel.setColumnWidth(NEGATE, 30, tcm);
			tableModel.setColumnWidth(DROP, 45, tcm);
			tableModel.setColumnWidth(ENABLE, 30, tcm);
			tableModel.setColumnWidth(SAMPLING_TYPE, 55, tcm);
			//tableModel.setColumnWidth(TCPFLAGS, 55, tcm);
			tableModel.setColumnWidth(TCPFIN, 15, tcm);
			tableModel.setColumnWidth(TCPSYN, 15, tcm);
			tableModel.setColumnWidth(TCPRST, 15, tcm);
			tableModel.setColumnWidth(TCPPSH, 15, tcm);
			tableModel.setColumnWidth(TCPACK, 15, tcm);
			tableModel.setColumnWidth(TCPURG, 15, tcm);
			//tableModel.setColumnWidth(TCPFLAGSMASK, 55, tcm);
			//tableModel.setColumnRenderer(MIRROR, bool_renderer, tcm);
			panel.add((new JScrollPane(jtable)), "Center");
			jtable.setPreferredScrollableViewportSize(new Dimension(200,400));
		}
		return panel;
	}

	public void clear()
	{
		//super.clear();
		int length = filters.size();
		if (length > 0)
		{
			Vector del_fltrs = new Vector(filters);
			filters.removeAllElements();
			FilterDescriptor.DeleteEdit delEdit = new FilterDescriptor.DeleteEdit(port, ingress, del_fltrs, this, port.getExpCoordinator().getCurrentExp());
			tableModel.fireTableRowsDeleted(0, (length -1));
			delEdit.commit();
		}
		//if (panel != null && panel.isShowing()) panel.dispose();
	}
	public int getFieldType(String nm)
	{
		for (int i = 0; i < NUMFIELDS; ++i)
		{
			if (nm.equals(fieldNames[i])) return i;
		}
		return -1;
	}

	//Priority.PRTable interface
	public void addOperation(NCCPOpManager.Operation op) 
	{
		if (opManager == null) 
		{
			opManager = new OpManager();
			((MonitorDaemon)port.getONLDaemon(ONLDaemon.NSPC)).addOpManager(opManager);
		}
		opManager.addOperation(op);
	}

	public void removeOperation(NCCPOpManager.Operation op) 
	{
		if (opManager != null)
			opManager.removeOperation(op);
		if (panel != null) panel.repaint();
	}
	public RouterPort getPort() { return port;}
	public ONLComponent getONLComponent() { return this;}
	//end Priority.PRTable interface 
	//interface MonitorAction
	public Monitor addMonitor(Monitor m){return null;}
	public void removeMonitor(Monitor m){}
	public boolean isDataType(MonitorDataType.Base mdt)
	{
		return (mdt.getParamType() == NSPMonClasses.NSP_EMFILTER && mdt.getONLComponent() == this);
	}
	public Monitor addMonitor(MonitorDataType.Base mdt)
	{
		if (isDataType(mdt))
		{
			FilterDescriptor fd = ((NSPMonClasses.MDataType_EMFilter)mdt).getFilter();
			if (fd != null)
			{
				return (fd.getStats().addMonitor(mdt));
			}
		}
		return null;
	}
	//end interface MonitorAction

	public void processEvent(ONLComponent.Event event)
	{
		if (event instanceof ONLComponent.PropertyEvent)
		{
			PropertyEvent pe = (PropertyEvent)event;
			if (pe.getLabel().equals(PRIORITY_PROP)) 
			{
				String val = pe.getNewValue();
				if (val != null && val.length() > 0)
					setPriority(Integer.parseInt(val));
				return;
			}
		}
		ONLComponent.TableEvent tevent = null;
		if (event instanceof ONLComponent.TableEvent)
			tevent = (ONLComponent.TableEvent)event;

		int tp = event.getType();
		ExpCoordinator.print(new String("FilterTable(" + getLabel() + "):TableChanged index = " + event.getIndex()), 2);
		FilterDescriptor r;
		try
		{
			switch(tp)
			{
			case ONLComponent.Event.TABLE_CHANGE:
				FilterDescriptor oldFilter = getFilter(tevent.getIndex());
				if (oldFilter != null)
				{
					oldFilter.parseString(tevent.getNewValue());
					changeFilter(oldFilter);
					break;
				}
			case ONLComponent.Event.TABLE_ADD:
				FilterDescriptor newFilter = addFilter(new FilterDescriptor(port, ingress, tevent.getNewValue()), tevent.getIndex());
				if (newFilter != null) newFilter.setCommitted(tevent.isCommitted());
				break;
			case ONLComponent.Event.TABLE_DELETE:
				removeFilter(getFilter(tevent.getIndex()));
				break;
			case ONLComponent.Event.TABLE_COMMIT:
				getFilter(tevent.getIndex()).setCommitted(true);
				break;
			default:
				super.processEvent(event);
			}
		}
		catch (java.text.ParseException pe)
		{
			System.err.print("FilterTable.processEvent ParseException");
		}
		super.processEvent(event);
	}

	public RouterPort.SamplingType getSamplingType(RouterPort.SamplingType tp) { return (getSamplingType(tp.getType()));}
	public RouterPort.SamplingType getSamplingType(int tp) 
	{ 
		if (samplingTypes == null) samplingTypes = port.getSamplingTypes();
		if (tp >= 0 && tp < 5) 
			return (samplingTypes[tp]);
		else return (samplingTypes[RouterPort.SamplingType.ALL]);
	}
	//PropertyChangeListener
	public void propertyChange(PropertyChangeEvent e) //listens on change in router port sampling types
	{
		String tmp = e.getPropertyName();
		if (tmp.equals(RouterPort.SAMPLING_TYPE1) ||
				tmp.equals(RouterPort.SAMPLING_TYPE2) ||
				tmp.equals(RouterPort.SAMPLING_TYPE3))
			tableModel.fireTableDataChanged();
	}
	//end PropertyChangeListener
	public void show()
	{
		ExpCoordinator.print(new String("FilterTable.show " + getLabel()), 4);
		FTAction ftaction = null;
		if (ingress)
			ftaction = (FTAction)port.getAction(NSPPort.INFTACTION);
		else 
			ftaction = (FTAction)port.getAction(NSPPort.OUTFTACTION);
		ftaction.actionPerformed(null);
	}

	public void fireCommitEvent(FilterDescriptor fd)
	{
		if (filters.contains(fd)) 
			fireEvent(new ONLComponent.TableEvent(ONLComponent.Event.TABLE_COMMIT, this, fd, getIndex(fd), null, "FilterDescriptor committed")); 
	}

	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new FTContentHandler(exp_xml, this));}
	protected boolean merge(ONLComponent c)
	{
		if ((c.getType() == getType()) && (c instanceof FilterTable))
		{
			ONLCTable t2 = (ONLCTable)c;
			int max = filters.size();
			ONLCTable.Element elem1;
			ONLCTable.Element elem2;
			int i;
			boolean differences = false;
			for (i = 0; i < max; ++i)
			{
				elem1 = getElement(i);
				elem2 = t2.getElementExact(elem1);
				if (elem2 == null) 
				{
					elem1.setState(ONLComponent.IN1);
					differences = true;
				}
				else elem1.setState(ONLComponent.INBOTH);
			}
			max = t2.size();
			for (i = 0; i < max; ++i)
			{
				elem2 = t2.getElement(i);
				elem1 = getElementExact(elem2);
				if (elem1 == null) 
				{
					elem2.setState(ONLComponent.IN2);
					differences = true;
					addElement(elem2, true);
				}
			}
			if (differences)
			{
				int error_type = StatusDisplay.Error.LOG;
				ExpCoordinator.theCoordinator.addError(new StatusDisplay.Error((new String("NSP Filter Table Inconsistency for " + getLabel() + "\n")),
						"Table Inconsistency",
						"Table Inconsistency",
						error_type)); 
			}
			return true;
		}
		else return false;
	}
	private void setFilterType(String tp)
	{
		filterType = FilterDescriptor.EXCL_GEN_MATCH;
		if (tp.equals(ONLComponent.IPPEMFTABLE) || tp.equals(ONLComponent.OPPEMFTABLE)) filterType = FilterDescriptor.EXACT_MATCH;
		if (tp.equals(ONLComponent.IPPGMFTABLE) || tp.equals(ONLComponent.OPPGMFTABLE)) filterType = FilterDescriptor.GEN_MATCH;
	}
	public int getFilterType() { return filterType;}
}
