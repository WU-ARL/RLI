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
 * File: Experiment.java
 * Author: Jyoti Parwatikar
 * Email:  jp@arl.wustl.edu
 * Organization: Washington University
 *
 * Derived from: none
 *
 * Date Created: 12/02/2004
 *
 * Description:
 *
 * Modification History:
 *
 */
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.undo.*;
import java.beans.*;
import java.lang.reflect.Array;
import javax.xml.stream.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;

public class ExpCompare extends Experiment
{
	public static final int TEST_CMP = 7;
	private Experiment experiment1 = null;
	private Experiment experiment2 = null;

	///////////////////////////////////////////// class ExpCompare.CompareAction////////////////////////////////////////////////////

	public static class CompareAction extends ONL.UserAction
	{
		private Loader masterLoader = null;
		private Loader compareLoader = null;

		////////////////////////// class ExpCompare.CompareAction.Loader /////////////////////
		private class Loader implements MenuFileAction.Saveable
		{
			private File file = null;
			protected JTextField textField = null;
			public Loader(JTextField fld)
			{ 
				textField = fld;
				textField.setSize(50,11);
			}
			public void saveToFile(java.io.File f){}
			public void loadFromFile(java.io.File f){ setFile(f);}
			public File getFile() { return file;}
			public void setFile(java.io.File f)
			{ 
				file = f;
				if (f != null) textField.setText(file.getAbsolutePath());
			}     
		}//end inner class ExpCompare.CompareAction.Loader

		///////////////////////////////////////////////// CompareAction methods ///////////////////////////////////////////////////
		public CompareAction(String nm)
		{
			super(nm, false, false);
			masterLoader = new Loader(new JTextField());
			compareLoader = new Loader(new JTextField());
		}
		public void actionPerformed(ActionEvent e)
		{
			Object[] components = new Object[4];
			components[0] = new JLabel("master file:");
			JPanel tmp_panel = new JPanel();
			tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
			tmp_panel.add(masterLoader.textField);
			tmp_panel.add(new JButton(new MenuFileAction(masterLoader, true, tmp_panel, "choose file", true, false)));
			components[1] = tmp_panel;
			components[2] = new JLabel("file to compare:");
			tmp_panel = new JPanel();
			tmp_panel.setLayout(new BoxLayout(tmp_panel, BoxLayout.X_AXIS));
			tmp_panel.add(compareLoader.textField);
			tmp_panel.add(new JButton(new MenuFileAction(compareLoader, true, tmp_panel, "choose file", true, false)));
			components[3] = tmp_panel;

			final String opt0 = "Compare";
			final String opt1 = "Cancel";
			Object[] options = {opt0,opt1};

			int rtn = JOptionPane.showOptionDialog(ExpCoordinator.theCoordinator.getMainWindow(), 
					components, 
					"Compare Sessions", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, 
					null,
					options,
					options[0]);

			if (rtn == JOptionPane.YES_OPTION)
			{
				if (masterLoader.file != null && compareLoader.file != null)
				{
					ExpCoordinator.theCoordinator.closeCurrentExp();
					//ExpCoordinator.setRecording(true);
					ExpCoordinator.theCoordinator.setCurrentExp(new ExpCompare(ExpCoordinator.theCoordinator, masterLoader.file, compareLoader.file));
				}
			}
		}
	}//end inner class ExpCompare.CompareAction

	public ExpCompare(ExpCoordinator ec, File f1, File f2)
	{
		super(ec, "diff", null, false);
		setExperiment(f1, true);
		setExperiment(f2, false);
		//experiment1 = new Experiment(ec, f1, new Topology(ec), false);
		//experiment2 = new Experiment(ec, f2, new Topology(ec), false);
		//build topology
		buildTopology();
	}

	private void setExperiment(File f, boolean is1)
	{
		if (f != null) 
		{	
			try
			{
				FileReader fr = new FileReader(f);
				char buf[] = new char[7];
				fr.read(buf, 0, 7);
				fr.close();
				String str = new String(buf);
				ExpCoordinator ec = ExpCoordinator.theCoordinator;
				if (str.equals("VERSION"))
				{
					expCoordinator.addError(new StatusDisplay.Error((new String("Error: comparing experiments: unsupported file:" + f.getAbsolutePath() + ".\n")),
							"File Error",
							"File Error",
							StatusDisplay.Error.ALL)); 
					return;
				}
				else
				{
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					Experiment experiment;
					if (is1) 
					{
						experiment1 = new Experiment(ec, "", new Topology(ec), false);
						experiment = experiment1;
					}
					else 
					{
						experiment2 = new Experiment(ec, "", new Topology(ec), false);
						experiment = experiment2;
					}
					ExperimentXML exp_xml = new ExperimentXML(experiment, xmlReader, f);
					xmlReader.parse(new InputSource(new FileInputStream(f)));
				}
			}
			catch (IOException e)
			{
				System.err.println("ExpCompare.setExperiment- error could not open file:" + f.getAbsolutePath() + " for reading");
				expCoordinator.addError(new StatusDisplay.Error((new String("File Error: comparing experiments: could not open file:" + f.getAbsolutePath() + " for reading\n")),
						"File Error",
						"File Error",
						StatusDisplay.Error.ALL)); 
			}
			catch(SAXException e2)
			{
				ExpCoordinator.print(new String("ExpCompare.setExperiment SAXException " + e2.getMessage()));
				expCoordinator.addError(new StatusDisplay.Error((new String("SAXException Error: comparing experiments: could not parse file:" + f.getAbsolutePath() + " for reading. " + e2.getMessage() +"\n")),
						"File Error",
						"File Error",
						StatusDisplay.Error.ALL)); 
			}
		} 
	}
	public void buildTopology()
	{
		Topology topo1 = experiment1.getTopology();
		Topology topo2 = experiment2.getTopology();

		ExpCoordinator.print(new String("\n\nExpCompare.buildTopology experiment1:" + experiment1.getFile().getAbsolutePath()), TEST_CMP);
		topo1.print(TEST_CMP);

		ExpCoordinator.print(new String("\nExpCompare.buildTopology experiment2:" + experiment2.getFile().getAbsolutePath()), TEST_CMP);
		topo2.print(TEST_CMP);

		expCoordinator.addError(new StatusDisplay.Error((new String("COMPARING EXPERIMENTS:\n")),
				"Comparing",
				"Comparing",
				StatusDisplay.Error.LOG)); 
		expCoordinator.addError(new StatusDisplay.Error((new String("EXP1:" + experiment1.getFile().getAbsolutePath()+ " \n")),
				"Comparing",
				"Comparing",
				StatusDisplay.Error.LOG)); 
		expCoordinator.addError(new StatusDisplay.Error((new String("EXP2:" + experiment2.getFile().getAbsolutePath()+ " \n\n")),
				"Comparing",
				"Comparing",
				StatusDisplay.Error.LOG)); 

		addClusters(topo1, topo2);
		addNodes(topo1, topo2);
		addLinks(topo1, topo2);
	}

	private void addClusters(Topology topo1, Topology topo2)
	{
		ExpCoordinator.print("\nExpCompare.addClusters", TEST_CMP);
		//compare clusters
		Cluster.Instance cli1;
		Cluster.Instance cli2;
		Vector clusters = topo1.getClusters();
		int max = clusters.size();
		int i = 0;
		for (i = 0; i < max; ++i)
		{
			cli1 = (Cluster.Instance)clusters.elementAt(i);
			cli2 = topo2.getCluster(cli1.getIndex());
			if (cli2 != null)
			{
				cli1.setState(ONLComponent.INBOTH);
				cli1.merge(cli2);
			}
			else cli1.setState(ONLComponent.IN1);
			addCluster(cli1);
		}

		clusters = topo2.getClusters();
		max = clusters.size();
		for (i = 0; i < max; ++i)
		{
			cli2 = (Cluster.Instance)clusters.elementAt(i);
			if (getTopology().getCluster(cli2.getIndex()) == null)
			{
				cli2.setState(ONLComponent.IN2);
				addCluster(cli2);
			}
		}
	}

	private void addNodes(Topology topo1, Topology topo2)
	{
		ExpCoordinator.print("\nExpCompare.addNodes", TEST_CMP);
		//compare clusters
		ONLComponent oc1;
		ONLComponent oc2;
		ONLComponentList nodes1 = topo1.getNodes();
		ONLComponentList nodes2 = topo2.getNodes();
		int max = nodes1.size();
		int i = 0;
		ExpCoordinator ec = ExpCoordinator.theCoordinator;

		for (i = 0; i < max; ++i)
		{
			oc1 = nodes1.onlComponentAt(i);
			oc2 = nodes2.getONLComponent(oc1);
			if (oc2 != null)
			{
				oc1.setState(ONLComponent.INBOTH);
				oc1.merge(oc2);
			}
			else 
			{
				oc1.setState(ONLComponent.IN1);
				expCoordinator.addError(new StatusDisplay.Error((new String("Hardware Missing: EXP2 does not have " + oc1.getLabel() + " \n")),
						"Hardware Missing",
						"Hardware Missing",
						StatusDisplay.Error.LOG)); 
			}
			addNode(oc1);
			ExpCoordinator.print(new String("   compare1 adding node:" + oc1.getLabel()), TEST_CMP);
		}

		max = nodes2.size();
		for (i = 0; i < max; ++i)
		{
			oc2 = nodes2.onlComponentAt(i);
			if (nodes1.getONLComponent(oc2) == null)
			{
				oc2.setState(ONLComponent.IN2);
				addNode(oc2);
				ExpCoordinator.print(new String("   compare2 adding node:" + oc2.getLabel()), TEST_CMP);
				expCoordinator.addError(new StatusDisplay.Error((new String("Extra Hardware: EXP2 contains hardware(" + oc2.getLabel() + ") not in EXP1.\n")),
						"Extra Hardware",
						"Extra Hardware",
						StatusDisplay.Error.LOG)); 
			}
		}

		getTopology().print(TEST_CMP);
	}

	private void addLinks(Topology topo1, Topology topo2)
	{
		ExpCoordinator.print("\nExpCompare.addLinks", TEST_CMP);
		//compare clusters
		ONLComponent oc1;
		ONLComponent oc2;
		ONLComponentList links1 = topo1.getLinks();
		ONLComponentList tmp_links2 = topo2.getLinks();
		ONLComponentList links2 = new ONLComponentList();
		ONLComponentList nodes = getTopology().getNodes();

		//convert experiment2's links
		int max = tmp_links2.size();
		int i = 0;
		LinkDescriptor lnk2;
		ExpCoordinator ec = ExpCoordinator.theCoordinator;
		for (i = 0; i < max; ++i)
		{
			lnk2 = (LinkDescriptor)tmp_links2.onlComponentAt(i);
			oc1 = nodes.getONLComponent(lnk2.getPoint1().getLabel(), lnk2.getPoint1().getType());
			if (oc1 == null)  ExpCoordinator.print(new String("   failed to find matching component for:" + lnk2.getPoint1().getLabel()), TEST_CMP);
			oc2 = nodes.getONLComponent(lnk2.getPoint2().getLabel(), lnk2.getPoint2().getType());
			if (oc2 == null) ExpCoordinator.print(new String("   failed to find matching component for:" + lnk2.getPoint2().getLabel()), TEST_CMP);
			if (oc1 != null && oc2 != null)
			{
				links2.addComponent(new LinkDescriptor(lnk2.getLabel(), oc1, oc2, lnk2.getBandwidth(), ec));
				ExpCoordinator.print(new String("   adding link:" + lnk2.getFullString()), TEST_CMP);
			}
			else
				ExpCoordinator.print(new String("   failed to find matching components for:" + lnk2.getFullString()), TEST_CMP);
		}

		max = links1.size();
		for (i = 0; i < max; ++i)
		{
			oc1 = links1.onlComponentAt(i);
			oc2 = links2.getONLComponent(oc1);
			if (oc2 != null)
			{
				oc1.setState(ONLComponent.INBOTH);
				oc1.merge(oc2);
			}
			else 
			{
				oc1.setState(ONLComponent.IN1);
				expCoordinator.addError(new StatusDisplay.Error((new String("Link Missing: EXP2 does not have " + ((LinkDescriptor)oc1).printString() + " .\n")),
						"Link Missing",
						"Link Missing",
						StatusDisplay.Error.LOG)); 
			}
			addLink(oc1);
		}

		max = links2.size();
		for (i = 0; i < max; ++i)
		{
			oc2 = links2.onlComponentAt(i);
			if (links1.getONLComponent(oc2) == null)
			{
				oc2.setState(ONLComponent.IN2);
				addLink(oc2);
				expCoordinator.addError(new StatusDisplay.Error((new String("Extra Link: EXP1 contains link(" + ((LinkDescriptor)oc2).printString() + ") not in EXP2.\n")),
						"Extra Link",
						"Extra Link",
						StatusDisplay.Error.LOG)); 
			}
		}
	}
	public void close()
	{
		super.close();
		//ExpCoordinator.setRecording(false);
	}
}
