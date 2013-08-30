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
 * File: MonitorAlgFunction.java
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
import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;

public class MonitorAlgFunction extends NumberData implements MonitorFunction, BoxedRangeListener
{
	public static final int ADD = 0;
	public static final int SUBTRACT = 1;
	public static final String ADD_LBL = "+";
	public static final String SUBTRACT_LBL = "-";
	protected MFParam mfparams[];
	protected int numParams = 0;
	protected double paramsCurrentTime = -1;
	protected int nextOp = ADD;
	protected Vector pending;
	private SpecFile.MFunctionSpec mspec = null;

	///////////////////////////////////////////Formula//////////////////////////////////////////////////////
	public static class Formula extends Vector
	{

		////////////////////////////////////////// MonitorAlgFunction.Formula.XMLHandler //////////////////////////////////////////////////////
		private class XMLHandler extends DefaultHandler
		{
			protected ExperimentXML expXML = null;
			protected String currentElement = "";
			public XMLHandler(ExperimentXML exp_xml)
			{
				super();
				expXML = exp_xml;
			}
			public void startElement(String uri, String localName, String qName, Attributes attributes) 
			{	
				ExpCoordinator.print(new String("MonitorAlgFunction.Formula.XMLHandler.startElement " + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.PARAM))
				{
					Pair p = new Pair(uri, attributes);
					add(p);
					expXML.setContentHandler(p.getContentHandler(expXML));
				}
			}

			public void endElement(String uri, String localName, String qName)
			{
				ExpCoordinator.print(new String("MonitorAlgFunction.Formula.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
				if (localName.equals(ExperimentXML.FORMULA)) expXML.removeContentHandler(this);
			}
		}

		//inner class Pair
		////////////////////////////////////////// MonitorAlgFunction.Formula.Pair //////////////////////////////////////////////////////
		protected class Pair
		{
			public MonitorDataType.Base mDataType = null;
			public int op = ADD;

			////////////////////////////////// MonitorAlgFunction.Formula.Pair.PXMLHandler //////////////////////////////////////////////////
			private class PXMLHandler extends MonitorXMLHandler
			{
				private Pair pair = null;
				public PXMLHandler(ExperimentXML exp_xml, Pair p)
				{
					super(exp_xml);
					pair = p;
				}
				/*
	  public void startElement(String uri, String localName, String qName, Attributes attributes) 
	  {	  
	  super.startElement(uri, localName, qName, attributes);
	  if (localName.equals(ExperimentXML.DATA_TYPE)) 
	  {
	  processDataType(uri, attributes);
	  if (this.mdataType != null) pair.mDataType = this.mdataType;
	  }
	  }*/
				public void endElement(String uri, String localName, String qName)
				{
					if (localName.equals(ExperimentXML.PARAM) &&
							this.mdataType != null) 
					{
						pair.mDataType = this.mdataType;
						expXML.removeContentHandler(this);
					}
				}
			}

			public Pair(String uri, Attributes attributes)
			{
				String opname = attributes.getValue(ExperimentXML.OP);
				if (opname.equals(ADD_LBL)) op = ADD;
				else op = SUBTRACT;
			}
			public Pair(MFParam p) { this(p.getData().getDataType(), p.getOp());}
			public Pair(MonitorDataType.Base mdt, int o)
			{
				mDataType = mdt;
				op = o;
			}
			public Pair(ONL.Reader rdr) throws IOException
			{
				read(rdr);
			}     
			public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
			{
				xmlWrtr.writeStartElement(ExperimentXML.PARAM);
				String opname = ADD_LBL;
				if (op == SUBTRACT) opname = SUBTRACT_LBL;
				xmlWrtr.writeAttribute(ExperimentXML.OP, opname);
				mDataType.writeXML(xmlWrtr);
				xmlWrtr.writeEndElement();
			}

			public void write(ONL.Writer wrtr) throws IOException
			{
				wrtr.writeInt(op);
				mDataType.write(wrtr);
			}
			public void read(ONL.Reader rdr) throws IOException
			{
				op = rdr.readInt();
				mDataType = MonitorDataType.Base.read(rdr);
				if (mDataType != null)
					ExpCoordinator.print(new String("MonitorAlgFunction.Formula.Pair.read op:" + op + " param:" + mDataType.getName()), 5);
				else
					ExpCoordinator.print(new String("MonitorAlgFunction.Formula.Pair.read op:" + op + " param:null"), 5);
			}
			public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new PXMLHandler(exp_xml, this));}
		}//end inner class Pair

		///////////////////////////////////////////////// Formula methods /////////////////////////////////////////////////////////////
		public Formula(String uri, Attributes attributes)
		{
			super();
		}
		public Formula(MonitorAlgFunction mf)
		{
			super();
			MFParam elem;
			for (int i = 0; i < mf.numParams; ++i)
			{
				elem = mf.mfparams[i];
				if (elem != null) add(new Pair(elem));
			}
		}
		public Formula(ONL.Reader rdr) throws IOException
		{
			super();
			read(rdr);
		}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
		{
			xmlWrtr.writeStartElement(ExperimentXML.FORMULA);
			int max = size();
			xmlWrtr.writeAttribute(ExperimentXML.NUMPARAMS, String.valueOf(max));
			for (int i = 0; i < max; ++i)
			{
				((Pair)elementAt(i)).writeXML(xmlWrtr);
			}
			xmlWrtr.writeEndElement();//end numparams
		}
		public void write(ONL.Writer wrtr) throws IOException
		{
			int max = size();
			wrtr.writeInt(max);
			for (int i = 0; i < max; ++i)
			{
				((Pair)elementAt(i)).write(wrtr);
			}
		}
		public void read(ONL.Reader rdr) throws IOException
		{
			int max = rdr.readInt();
			ExpCoordinator.print(new String("MonitorAlgFunction.Formula.read numParams=" + max),5);
			for (int i = 0; i < max; ++i)
			{
				add(new Pair(rdr));
			}
		}
		public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new XMLHandler(exp_xml));}   
	} // end MonitorAlgFunction.Formula

	//inner class Spec
	public static class Spec extends SpecFile.MFunctionSpec
	{    private class FormulaPSpec extends SpecFile.MFParamSpec
		{
		private MonitorAlgFunction mfunction;
		public FormulaPSpec(MonitorManager mw, MonitorAlgFunction maf)
		{
			super(mw);
			mfunction = maf;
		}
		public Monitor getMFParameter(SpecFile.SpecReader rdr) throws IOException 
		{
			StringTokenizer line = rdr.tokenizeLine();
			int op = ADD;
			int level = rdr.getLevel();
			//ExpCoordinator.printer.print("MonitorAlgFunction::getParamter");
			if (line.hasMoreTokens())
			{
				String type = line.nextToken();
				if (type.compareTo("OPERATION") == 0) 
				{
					op = Integer.parseInt(line.nextToken());
					//ExpCoordinator.printer.print("   MonitorAlgFunction found OPERATION " + op);
				}
				else
				{
					if (type.compareTo("FORMULA_DESC") == 0) 		    {
						MonitorDataType.MFormula mdt = (MonitorDataType.MFormula)((MonitorAlgFunction)mfunction).getDataType();
						mdt.setFormula(rdr.readLine());
						rdr.readToEnd(level);
						return null;
					}
				}
			}
			line = rdr.tokenizeLine();
			MonitorPlus m = null;
			if ((!rdr.atToken()) && 
					line.hasMoreTokens() && 
					(line.nextToken().compareTo("CONSTANT") == 0))
			{
				mfunction.addPending(Double.parseDouble(line.nextToken()), op);
				ExpCoordinator.printer.print(new String("   MonitorAlgFunction found CONSTANT"), 2);
			}
			else
			{
				//ExpCoordinator.printer.print("   MonitorAlgFunction calling getParameter");
				m = (MonitorPlus)super.getMFParameter(rdr);
				//ExpCoordinator.printer.print("   MonitorAlgFunction found Monitor");
				mfunction.addPending(m, op);
			}
			//ExpCoordinator.printer.print("MAFspec::getMFParameter level" + level);
			rdr.readToEnd(level);
			return m;
		}	    	 
		}

	public Spec(MonitorManager mw, MonitorAlgFunction maf)
	{
		super(mw, maf, null, false); 
		paramSpec = new FormulaPSpec(mw, maf);
	}
	protected void writeParam(SpecFile.SpecWriter writer, int i) throws IOException {}//override for writing function specific data
	protected void writeParams(SpecFile.SpecWriter writer) throws IOException //override for writing function specific data
	{
		int i;
		int num_realParams = 0;
		MonitorPlus elem;
		MonitorAlgFunction maf = (MonitorAlgFunction)mfunction;
		int max = maf.numParams;
		for (i = 0; i < max; ++i)
		{
			if (maf.mfparams[i] != null)
				++num_realParams;
		}
		//ExpCoordinator.printer.print("MAF::saveToFile num_realParams " + num_realParams);
		//writer.writeLine(String.valueOf(num_realParams));

		//first write formula description frame it in the MFPARAM for backward compatibility
		MonitorDataType.MFormula mdt = (MonitorDataType.MFormula)((MonitorAlgFunction)mfunction).getDataType();
		if (mdt.getFormula() != null)
		{
			writer.writeToken(SpecFile.tokMFPARAM);
			writer.writeLine("FORMULA_DESC");
			writer.writeLine(mdt.getFormula());
			writer.writeEndToken(SpecFile.tokMFPARAM);
		}

		SpecFile.ParamSpec ps = new SpecFile.ParamSpec(monitorManager);
		MFParam mfparam;

		for (i = 0; i < max; ++i)
		{
			mfparam = maf.mfparams[i];
			if (mfparam != null)
			{
				writer.writeToken(SpecFile.tokMFPARAM);
				writer.writeLine("OPERATION " + mfparam.getOp());
				if (mfparam.isConstant())
				{
					writer.writeLine("CONSTANT " + mfparam.getConstVal());
				}  
				else
					ps.writeParam(mfparam.getData().getDataType(), writer);
				writer.writeEndToken(SpecFile.tokMFPARAM);
			}
		} 
	}
	}
	//inner class MFParam
	protected class MFParam implements MonitorFunction.Param
	{
		private MonitorPlus ndata = null; //one of the monitors we need
		private NumberPair currentData = null;
		private boolean is_constant = false;
		private double const_val = 0;
		private int operation = ADD;

		public MFParam(MonitorPlus nd, int op)
		{
			ndata = nd;
			if (ndata == null)
			{
				ExpCoordinator.print("MFParam ndata is null", 2);
			}
			operation = op;
			MonitorDataType.Base mdt = ndata.getDataType();
			if (mdt.getParamType() == MonitorDataType.CONSTANT)
			{
				is_constant = true;
				const_val = ((MonitorDataType.Constant)mdt).getConstant();
			}
			//print();
		}
		public MFParam(double c, int op)
		{
			const_val = c;
			is_constant = true;
			operation = op;
			ndata = new Constant(c);
			//print();
		}
		public MFParam(ONL.Reader rdr) throws IOException { read(rdr);}
		public boolean isEqual(MonitorPlus m)
		{
			return (!is_constant && m == (MonitorPlus)ndata);
		}
		public MonitorPlus getData() { return ndata;}
		public NumberPair getCurrentData() { return currentData;}
		public boolean isUpdated() { return (currentData != null);}
		public void setCurrentData(NumberPair np) { currentData = np;}
		public boolean isAdd() { return (operation == ADD);}
		public boolean isConstant() { return is_constant;}
		public double getConstVal() { return const_val;}
		public int getOp() { return operation;}
		public void print()
		{
			if (operation == ADD)
				ExpCoordinator.printer.print(new String("MFParam monitor " + ndata.getDataType().getName() + " operation add"), 2);
			else
				ExpCoordinator.printer.print(new String("MFParam monitor " + ndata.getDataType().getName() + " operation subtract"), 2);
		}

		//interface MonitorFunction.Param
		public MonitorPlus getMonitor() { return ndata;}
		public void write(ONL.Writer wrtr) throws IOException
		{
			wrtr.writeInt(operation);
			ndata.getDataType().write(wrtr);
		}
		public void writeChangeable(ONL.Writer wrtr) throws IOException{}
		public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException {}
		public void readChangeable(ONL.Reader rdr) throws IOException{}
		public void read(ONL.Reader rdr) throws IOException
		{
			operation = rdr.readInt();
			MonitorDataType.Base mdt = MonitorDataType.Base.read(rdr);
			mdt.print(2);
			ndata = MonitorManager.getMonitor(mdt);
			if (mdt.getParamType() == MonitorDataType.CONSTANT)
			{
				is_constant = true;
				const_val = ((MonitorDataType.Constant)mdt).getConstant();
			}
		}
		public void record(ONL.Writer wrtr) throws IOException 
		{
			if (ndata != null) ndata.recordData(wrtr);
		}
		public void loadRecording(ONL.Reader rdr) throws IOException
		{
			if (ndata != null) ndata.readRecordedData(rdr);
		}
		//end interface MonitorFunction.Param
	}
	//end inner class MFParam


	public MonitorAlgFunction(MonitorDataType.MFormula dt, MonitorManager mw)
	{
		super(dt, 0, 0, 0, 0, NumberData.DROP_OLD);
		int nparams = dt.getNumParams();
		dt.setMFunction(this);
		mfparams = new MFParam[nparams];
		numParams = nparams;
		pending = new Vector();
		mspec = new Spec(mw, this);
		for (int i = 0; i < numParams; ++i)
			mfparams[i] = null;
		if (dt.getFormulaObj() != null)
			initParams(dt.getFormulaObj());
	}

	private void initParams(Formula formula)
	{
		int max = formula.size();
		ExpCoordinator.print("MonitorAlgFunction.initParams numParams:" + numParams + " formula.size:" + max);
		if (numParams != max)
		{
			if (numParams < max) max = numParams;
			else numParams = max;
		}
		for (int i = 0; i < max; ++ i)
		{
			Formula.Pair pair = (Formula.Pair)formula.elementAt(i);
			MonitorPlus m = MonitorManager.getMonitor(pair.mDataType);
			if (m != null) 
			{
				addPending(m, pair.op);
				m.addMonitorFunction(this);
			}
		}
	}

	public void compute()
	{
		if (paused) return;
		double val = 0;
		double tmp_val = 0;
		NumberPair np;
		NumberPair np2;
		ExpCoordinator.print(new String("MonitorAdd::compute numParams:" + numParams), 5);
		for (int i = 0; i < numParams; ++i)
		{
			if (mfparams[i] != null) 
			{
				if (mfparams[i].isConstant()) tmp_val = mfparams[i].getConstVal();
				else
				{
					np = mfparams[i].getCurrentData();
					//if (np == null)  ;//ExpCoordinator.printer.print("MonitorAdd::compute " + mfparams[i].getData().getDataType().getName() + " np is null");
					//else   
					if (np != null) 
					{
						mfparams[i].setCurrentData(null);
						if (currentTime < np.getX()) currentTime = np.getX();
						tmp_val = np.getY();
						//ExpCoordinator.printer.print("     " + mfparams[i].getData().getDataType().getName() + " np is ( "  + np.getX() + ", " + np.getY() + " adding val " + tmp_val + " to " + val);
					}
				}

				if (mfparams[i].isAdd()) 
				{
					val = tmp_val + val;
					//ExpCoordinator.printer.print("  adding");
				}
				else val = val - tmp_val;
			}
		}
		np2 = new NumberPair(currentTime, val);
		//ExpCoordinator.printer.print("  adding pair (" + np2.getX() + ", " + np2.getY() + ")");
		addElement(np2);
	}

	//public abstract void compute(); //this should set mfparams current data to null. and perform the function

	public void setNextOp(int v) { 
		//ExpCoordinator.printer.print("MFormula::setNextOp " + v);
		nextOp = v;}

	private MFParam getMFParam(MonitorPlus nd)
	{
		for (int i = 0; i < numParams; ++i)
		{
			if (mfparams[i] != null && !mfparams[i].isConstant())
			{
				if (mfparams[i].isEqual(nd))
					return mfparams[i];
			}
		}
		return null;
	} 
	private MFParam removeFromPending(MonitorPlus nd)
	{
		int max = pending.size();
		MFParam elem;
		for (int i = 0; i < max; ++i)
		{
			elem = (MFParam)pending.elementAt(i);
			if (elem.isEqual(nd))
			{
				pending.remove(i);
				return elem;
			}
		}
		return null;
	} 

	private MFParam addMFParam(MFParam param)
	{
		ExpCoordinator.print(new String("MonitorAlgFunction.addMFParam numParams: " + numParams), 2);
		for (int i = 0; i < numParams; ++i)
		{
			if (mfparams[i] == null)
			{
				String nm = "";
				if (param.getData() != null ) nm = param.getData().getDataType().getName();
				ExpCoordinator.print(new String(   " setting param:" + i + " for " + nm), 2);
				mfparams[i] = param;
				return param;
			}
		}
		return null;
	}

	//Monitorable interface
	public void addData(MonitorPlus m) //this is called from addMonitorFunction when added to an existing monitor
	{
		boolean notAdded = true;
		//if (stopped) start();
		MFParam mfp = getMFParam(m); //check if already there
		if (mfp != null) return;

		mfp = removeFromPending(m); //check if on the pending list

		if (mfp == null)
			mfp = new MFParam(m, nextOp);//otw make a new one

		mfp = addMFParam(mfp); //adds to list if can returns null if already full

		if (mfp != null)
		{ 
			if (getDataType().getDataUnits() == Units.UNKNOWN) getDataType().setDataUnits(m.getDataType().getDataUnits());
			m.addBoxedRangeListener(this);
			ExpCoordinator.printer.print(new String("MonitorAlgFunction::addData " + m.getDataType().getName() + " to " + getDataType().getName()), 2);
			notAdded = false;
		}
		if (notAdded) m.removeMonitorFunction(this);//couldn't find room for it so tell it we're no longer interested
	}

	public void removeData(MonitorPlus m)
	{
		removeFromPending(m);
		for (int i = 0; i < numParams; ++i)
		{
			if (mfparams[i] != null && mfparams[i].isEqual(m))
			{
				mfparams[i] = null;
				m.removeMonitorFunction(this); 
				m.removeBoxedRangeListener(this);
				ExpCoordinator.printer.print(new String("MonitorFunction::removeData " + m.getDataType().getName() + " to " + getDataType().getName()), 2);
				break;
			}
		}
	}
	//end MonitorFunction Interface

	public void addPending(MonitorPlus mp, int op) { addPending(new MFParam(mp, op));}
	public void addPending(double k, int op) { addPending(new MFParam(k, op));}
	private void addPending(MFParam param)
	{
		if (param.isConstant()) addMFParam(param);
		pending.add(param);
	}

	public void userStop()
	{
		//ExpCoordinator.printer.print("MonitorFunction::userStop " + getDataType().getName());
		for (int i = 0; i < numParams; ++i)
		{
			if (mfparams[i] != null && !mfparams[i].isConstant())
			{
				MonitorPlus m = mfparams[i].getData();
				//ExpCoordinator.printer.print("     remove " + m.getDataType().getName() + " from " + getDataType().getName());
				m.removeMonitorFunction(this);
				m.removeBoxedRangeListener(this);
			}
			mfparams[i] = null;
		}
		super.userStop();
	}

	//BoxedRangeListener interface
	public void changeVisible(BoxedRangeEvent e){}
	public void changeBoundaries(BoxedRangeEvent e){}
	public void changeXVisible(BoxedRangeEvent e){}
	public void changeYVisible(BoxedRangeEvent e){}
	public void changeXBounds(BoxedRangeEvent e)
	{
		MFParam param = getParam((MonitorPlus) e.getSource());
		if (param != null)
		{
			NumberPair np = (NumberPair) param.getData().getLastElement();
			if (np != null)
				ExpCoordinator.print(new String("MonitorAlgFunction::changeXBounds param:" + ((MonitorPlus)e.getSource()).getDataType().getName() + " np = " + np.getX() + "," + np.getY()), 5);
			else
				ExpCoordinator.print(new String("MonitorAlgFunction::changeXBounds param:" + ((MonitorPlus)e.getSource()).getDataType().getName()), 5);

			if ((e.getType() & BoxedRangeEvent.XMAX_CHANGED) != 0)
			{
				//if (param.getCurrentData() == null) ExpCoordinator.printer.print("MonitorFunction::changeXBounds currentData is null");
				//else
				if (param.getCurrentData() != null)
				{
					if (param.isUpdated()) //this is the second update for this param. go ahead and do the computation
					{
						if (stopped) start();
						//if (!isStarted()) start();
						compute();
					}
				}
				param.setCurrentData(np);
			}
		}
	}
	public void changeYBounds(BoxedRangeEvent e){}
	//end BoxedRangeListener interface

	public MonitorAlgFunction.MFParam getParam(MonitorPlus nd)
	{
		for (int i = 0; i < numParams; ++i)
		{
			if (mfparams[i] != null && mfparams[i].getData() == nd)
				return mfparams[i];
		}
		return null;
	}
	public MonitorFunction.Param readParam(ONL.Reader rdr) throws IOException //not used info stored and retrieve by formula
	{
		MFParam rtn = new MFParam(rdr);
		rtn = addMFParam(rtn);
		return rtn;
	} 
	public void write(ONL.Writer wrtr) throws IOException //not used
	{
		getDataType().write(wrtr);
	}
	public void read(ONL.Reader rdr)throws IOException //not used
	{
		MonitorDataType.MFormula mdt = (MonitorDataType.MFormula)MonitorDataType.Base.read(rdr);
		ExpCoordinator.print(new String("MonitorAlgFunction.read"), 2);
		mdt.print(2);
		initParams(mdt.getFormulaObj());
	}

	public SpecFile.MFunctionSpec getSpec() { return mspec;}

	public MonitorFunction.Param getParam(MonitorDataType.Base mdt)
	{
		for (int i = 0; i < numParams; ++i)
		{
			if (mfparams[i] != null && mfparams[i].getMonitor().getDataType().isEqual(mdt))
				return mfparams[i];
		}
		return null;
	}

	public void addConditional(Conditional c){}
	public void removeConditional(Conditional c){}
	public Formula getFormula() { return (new Formula(this));}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException { getDataType().writeXML(xmlWrtr);}
}
