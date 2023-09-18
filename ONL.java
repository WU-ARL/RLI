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
 * File: ONL.java
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
import java.io.*;
import java.lang.StringBuffer;
import java.lang.reflect.Array;
import javax.swing.*;
import java.awt.Component;
import java.text.ParseException;
import java.util.Vector;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.xml.stream.*;

public class ONL
{ 
	public static final int HST_NDX = 30;


	public static final String NXP0_STR = "p0";
	public static final String NXP1A_STR = "p1a";
	public static final String NXP1B_STR = "p1b";
	public static final String NXP1C_STR = "p1c";
	public static final String NXP2_STR = "p2";
	public static final String NXP3_STR = "p3";
	public static final String NXP4_STR = "p4";
	public static final String NXP5_STR = "p5";
	public static final String NXP6_STR = "p6";
	public static final String NXP7_STR = "p7";
	public static final String HST_STR = "HST";
	public static final String NSP_STR = "n";

	public static final int NXP0 = 16;
	public static final int NXP1A = 32;
	public static final int NXP1B = 33;
	public static final int NXP1C = 34;
	public static final int NXP2 = 48;
	public static final int NXP3 = 64;
	public static final int NXP4 = 80;
	public static final int NXP5 = 96;
	public static final int NXP6 = 112;
	public static final int NXP7 = 128;

	protected static final String portLabels = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	public static final int TEST_IPADDRESS = 5;
	protected static final String VERSION_TOK = "VERSION";
	public static final String tokEND = "END";
	//GRAPHICS OBJECTS
	public static class ComponentwLabel extends JPanel
	{
		protected JComponent component = null;
		protected JLabel label = null;
		public ComponentwLabel(JComponent tf, String lbl)
		{
			super();
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			component = tf;
			label = new JLabel(lbl);
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(label);
			add(component);
		}
		public void setEnabled(boolean b)
		{
			//super.setEnabled(ignoreObsStatus && b);
			//component.setEnabled(ignoreObsStatus && b);
			super.setEnabled(b);
			component.setEnabled(b);
		}
		/*
    public void setEditable(boolean b)
    {
      //super.setEnabled(ignoreObsStatus && b);
      //component.setEnabled(ignoreObsStatus && b);
      super.setEditable(b);
      component.setEditable(b);
    }
		 */
		public JComponent getJComponent() { return component;}
		public String getLabel() { return (label.getText());}
		public void setLabel(String s) { label.setText(s);}
	}

	//READERS and WRITERS
	public static interface Reader
	{
		public String readString() throws IOException;
		public int readByte() throws IOException;
		public int readShort() throws IOException;
		public int readInt() throws IOException;
		public double readDouble() throws IOException;
		public float readFloat() throws IOException;
		public double readUnsignedInt() throws IOException;
		public String readLine() throws IOException;
		public boolean readBoolean() throws IOException;
		public boolean ready() throws IOException;
		public void finish() throws IOException;
		public double getVersion();
		public void setVersion(double d);
	}

	public static interface Writer //use to define an NCCP and File writer that writes a component the File writer will add new lines so can be read
	{
		public void writeString(String s) throws IOException;
		public void writeByte(int i) throws IOException;
		public void writeShort(int i) throws IOException;
		public void writeInt(int i) throws IOException;
		public void writeDouble(double i) throws IOException;
		public void writeFloat(float i) throws IOException;
		public void writeUnsignedInt(double i) throws IOException;
		public void writeLine(String s) throws IOException;
		public void writeBoolean(boolean b) throws IOException;
		public void finish() throws IOException;
	}
	public static class BaseFileReader extends BufferedReader implements ONL.Reader
	{
		private double version;
		private String fileName = "";
		private String savedLine = null;
		private boolean countAllLines = false;
		private boolean no_close = false;

		public BaseFileReader(java.io.StringReader r)
		{
			super(r);
			no_close = true;
			version = ExpCoordinator.VERSION;
		}

		public BaseFileReader(File f) throws IOException
		{
			super(new FileReader(f));
			fileName = new String(f.getAbsolutePath());
			setVersion();
			//version = ExpCoordinator.VERSION;
		}
		public BaseFileReader(String fname) throws IOException
		{
			super(new FileReader(fname));
			fileName = new String(fname);
			setVersion();
			//version = ExpCoordinator.VERSION;
		}
		public BaseFileReader(InputStream in_str, String fname) throws IOException
		{
			super(new InputStreamReader(in_str));
			fileName = new String(fname);
			setVersion();
			//version = ExpCoordinator.VERSION;
		}
		public String readString() throws IOException { return (readLine()); }
		public int readByte()  throws IOException { return (readInt());}
		public int readShort()  throws IOException { return (readInt());}
		public int readInt()  throws IOException { return Integer.parseInt(readLine());}
		public double readDouble() throws IOException { return Double.parseDouble(readLine());}
		public float readFloat() throws IOException { return Float.parseFloat(readLine());}
		public double readUnsignedInt() throws IOException { return readDouble();}
		public boolean readBoolean() throws IOException { return (Boolean.valueOf(readLine()).booleanValue());}
		public double getVersion() { return version;}
		public void setVersion(double d) { version = d;}
		public void setVersion() throws IOException
		{
			String tmp = readLine();
			if (tmp.equals(VERSION_TOK)) 
			{
				version = readDouble();
			}
			else
			{
				savedLine = tmp;
				version = 0;
			}
			ExpCoordinator.print(new String("ONL.BaseFileReader.setVersion version " + version), 5);
		}
		public void setCountAllLines(boolean b) { countAllLines = b;}
		public boolean getCountAllLines() { return countAllLines;}
		public void finish() throws IOException { if (!no_close) close();}
		public void setNoClose(boolean b) { no_close = b;}
		public String getFileName() { return (new String(fileName));}
		public boolean ready()
		{
			try
			{
				savedLine = readLine();
				if (savedLine != null) return true;
			}
			catch (IOException e){}
			return false;
		}
		public String readLine() throws IOException
		{
			if (savedLine == null) 
			{
				String rtn = "";
				String[] strarray;
				boolean done = false;
				while (rtn.length() == 0 && !done)
				{
					rtn = super.readLine();
					if (rtn == null) return rtn;
					if (!rtn.startsWith("#"))
					{
						strarray = rtn.split("#");
						rtn = strarray[0].trim();
					}
					else
						rtn = "";
					done = countAllLines;
				}
				ExpCoordinator.print(new String("ONL.BaseFilerReader.readLine:" + rtn + " countAllLines:" + countAllLines), 2);
				return (rtn);
			}
			else
			{
				String tmp = savedLine;
				savedLine = null;
				return tmp;
			}
		} 
	}

	public static class BaseFileWriter extends BufferedWriter implements ONL.Writer  
	{
		private boolean no_close = false;
		public BaseFileWriter(String fname, boolean append) throws IOException
		{
			super(new FileWriter(fname, append));
			writeVersion();
		}
		public BaseFileWriter(File f) throws IOException	{
			super(new FileWriter(f));
			writeVersion();
		}
		public BaseFileWriter(String fname) throws IOException
		{
			super(new FileWriter(fname, true));
			writeVersion();
		}
		public void writeString(String s) throws IOException
		{
			if (s != null)
				write(s);
			//System.out.println("writing string:" + s);
			newLine();
		}
		public void writeByte(int i)  throws IOException { writeInt(i);}
		public void writeShort(int i) throws IOException { writeInt(i);}
		public void writeInt(int i) throws IOException { writeString(String.valueOf(i));}
		public void writeDouble(double i) throws IOException { writeString(String.valueOf(i));}
		public void writeFloat(float i) throws IOException { writeString(String.valueOf(i));}
		public void writeUnsignedInt(double i) throws IOException { writeInt((int)i);} 
		public void writeLine(String s) throws IOException { writeString(s);}
		public void writeBoolean(boolean b) throws IOException { writeString(String.valueOf(b));}
		public void finish() throws IOException
		{
			flush();
			if (!no_close) close();
		}
		private void writeVersion() throws IOException
		{
			writeString(VERSION_TOK);
			writeDouble(ExpCoordinator.VERSION);
		}
		public void setNoClose(boolean b) { no_close = b;}
	}

	//should create readers and writers for the NCCP stream
	public static class NCCPReader implements Reader
	{
		private double version;
		private DataInput din = null;
		public NCCPReader( DataInput d) 
		{ 
			version = ExpCoordinator.VERSION;
			din = d;
		}
		public String readString() throws IOException { return (NCCP.readString(din));}
		public int readByte() throws IOException { return (din.readByte());}
		public int readShort() throws IOException { return (din.readShort());}
		public int readInt() throws IOException { return (din.readInt());}
		public double readDouble() throws IOException 
		{ 
			double rtn = Double.parseDouble(NCCP.readString(din));
			return (rtn);
		}
		public float readFloat() throws IOException 
		{ 
			float rtn = Float.parseFloat(NCCP.readString(din));
			return (rtn);
		}
		public boolean readBoolean()  throws IOException 
		{ 
			int i = readByte();
			return (i != 0);
		}
		public double readUnsignedInt() throws IOException { return (NCCP.readUnsignedInt(din));}
		public String readLine() throws IOException { return (readString());}
		public boolean ready() throws IOException { return (din != null);}
		public void finish() throws IOException {}
		public double getVersion() { return version;}
		public void setVersion(double d) { version = d;}
	}

	public static class NCCPWriter implements Writer
	{ 
		private DataOutput dout = null;
		public NCCPWriter(DataOutput d) { dout = d;}
		public void writeString(String s) throws IOException { NCCP.writeString(s, dout);}
		public void writeByte(int i) throws IOException { dout.writeByte(i);}
		public void writeShort(int i) throws IOException { dout.writeShort(i);}
		public void writeInt(int i) throws IOException {dout.writeInt(i);}
		public void writeDouble(double i) throws IOException { NCCP.writeString(String.valueOf(i), dout);}
		public void writeFloat(float i) throws IOException { NCCP.writeString(String.valueOf(i), dout);}
		public void writeUnsignedInt(double i) throws IOException { dout.writeInt((int)i);}
		public void writeLine(String s) throws IOException { NCCP.writeString(s, dout);}
		public void writeBoolean(boolean b) throws IOException 
		{
			if (b) writeByte(1);
			else writeByte(0);
		}
		public void writeComponentID(ONLComponent c) throws IOException
		{
			NCCP.writeComponentID(c,dout);
		}
		public void finish() throws IOException{}
	}

	public static class InStream implements Reader
	{
		private double version;
		private File file = null;
		private DataInputStream instream = null;
		private BufferedReader lineReader = null;
		public InStream(InputStream in) 
		{  
			instream = new DataInputStream(in);
			version = 0;
		}
		public InStream(File f)
		{
			version = 0;
			try
			{
				instream = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
				lineReader = new BufferedReader(new InputStreamReader(instream));
				file = f;
			}
			catch (FileNotFoundException e)
			{
				System.err.println("ONL.Instream.Instream(File) File " + file.getName() + " not found.");
			}
		}
		public String readString() throws IOException
		{
			int len = instream.readInt()/2;
			char chs[] = new char[len];
			for (int i = 0; i < len; ++i)
				chs[i] = instream.readChar();
			ExpCoordinator.print(new String("ONL.InStream.readString len = " + len ), 2);
			return (new String(chs));
		}
		public int readByte()  throws IOException { return ((int)instream.readByte());}
		public int readInt()  throws IOException { return (instream.readInt());}
		public double readUnsignedInt() throws IOException { return ((double)instream.readInt());}
		public int readShort() throws IOException { return ((int)instream.readShort());}
		public double readDouble() throws IOException { return (instream.readDouble());}
		public float readFloat() throws IOException { return (instream.readFloat());}
		public String readLine() throws IOException { return (lineReader.readLine());}
		public boolean readBoolean() throws IOException
		{
			if (readByte() != 0 ) return true;
			else return false;
		}
		public boolean ready() throws IOException { return (instream.available() > 0);}
		public void finish() throws IOException 
		{ 
			instream.close();
			lineReader.close();
		}
		public double getVersion() { return version;}
		public void setVersion(double d) { version = d;}
		public String getFileName() 
		{
			if (file != null) return ( file.getAbsolutePath());
			else return "";
		}
	}

	public static class OutStream extends DataOutputStream implements Writer //use to define an NCCP and File writer that writes a component the File writer will add new lines so can be read
	{
		//private File file = null;
		public OutStream(OutputStream out)
		{
			super(out);
		}
		public OutStream(File f) throws IOException
		{
			this(new BufferedOutputStream(new FileOutputStream(f)));
			//file = f;
		}
		public void writeString(String s) throws IOException
		{
			writeInt(2*(s.length()));
			writeChars(s);
		}
		public void writeUnsignedInt(double i) throws IOException {writeInt((int) i);}
		public void writeLine(String s) throws IOException 
		{ 
			writeChars(s);
			writeChar('\n');
		}
		public void finish() throws IOException
		{
			flush();
			close();
		}
	}
	public static class StringReader implements Reader
	{
		private double version;
		private String string = null;
		private String strarray[] = null;
		private int len = 0;
		private int current = 0;
		private int prn = -1;
		public StringReader(String str, String sp) 
		{ 
			version = ExpCoordinator.VERSION;
			reset(str, sp);
		}
		public StringReader(String str) { this(str, "(\\s)+");}
		public void reset() { reset(string);}
		public void reset(String str) { reset(str, "(\\s)+");}
		public void resetSplit(String sp) { reset(string, sp);}
		public void reset(String str, String sp)
		{
			string = str;
			strarray = str.split(sp);
			len = Array.getLength(strarray);
			current = 0; 
			while (current < len && strarray[current].length() == 0) ++current; //skip over empty strings
		}
		public void print(int level)
		{
			prn = level;
			String txt = "";
			for (int i = 0 ; i < len; ++i)
				txt = txt.concat(new String(strarray[i] + ":"));
			ExpCoordinator.print(new String("tokens - :" + txt + "  string - " + string), level);
		}
		public String readString() throws IOException
		{
			if (current < len)
			{
				String rtn = strarray[current];
				++current;
				return rtn;
			}
			else throw new java.io.IOException("ONL.StringReader no more tokens");
		}
		public int readByte() throws IOException { return (readInt());}
		public int readShort() throws IOException{ return (readInt());}
		public boolean readBoolean() throws IOException { return (Boolean.valueOf(readLine()).booleanValue());}
		public int readInt() throws IOException
		{
			if (current < len)
			{
				if (prn > 0) ExpCoordinator.print(strarray[current], prn);
				int rtn = Integer.parseInt(strarray[current]);
				++current;
				return rtn;
			}
			else throw new java.io.IOException("ONL.StringReader no more tokens");
		}
		public double readDouble() throws IOException
		{
			if (current < len)
			{
				double rtn = Double.parseDouble(strarray[current]);
				++current;
				return rtn;
			}
			else throw new java.io.IOException("ONL.StringReader no more tokens");
		}
		public float readFloat() throws IOException
		{
			if (current < len)
			{
				float rtn = Float.parseFloat(strarray[current]);
				++current;
				return rtn;
			}
			else throw new java.io.IOException("ONL.StringReader no more tokens");
		}
		public double readUnsignedInt() throws IOException {return (readInt());}
		public String readLine() throws IOException {return (readString());}
		public double getVersion() { return version;}
		public void setVersion(double d) { version = d;}
		public boolean ready() throws IOException { return (current < len);}
		public void finish() throws IOException { current = len;}
	}

	public static class StringWriter implements Writer
	{
		private int count = 0;
		private StringBuffer stringBuffer = null;
		public StringWriter() { stringBuffer = new StringBuffer();}
		public void writeString(String s) throws IOException 
		{ 
			if (count > 0) stringBuffer.append(" ");
			stringBuffer.append(s);
			++count;
		}
		public void writeByte(int i) throws IOException { writeInt(i);}
		public void writeShort(int i) throws IOException { writeInt(i);}
		public void writeInt(int i) throws IOException
		{ 
			if (count > 0) stringBuffer.append(" ");
			stringBuffer.append(i);
			++count;
		}
		public void writeDouble(double i) throws IOException
		{ 
			if (count > 0) stringBuffer.append(" ");
			stringBuffer.append(i);
			++count;
		}
		public void writeFloat(float i) throws IOException
		{ 
			if (count > 0) stringBuffer.append(" ");
			stringBuffer.append(i);
			++count;
		}
		public void writeUnsignedInt(double i) throws IOException
		{ 
			if (count > 0) stringBuffer.append(" ");
			stringBuffer.append(i);
			++count;
		}
		public void writeBoolean(boolean b) throws IOException { writeString(String.valueOf(b));}
		public void writeLine(String s) throws IOException
		{ 
			if (count > 0) stringBuffer.append(" ");
			stringBuffer.append(s);
			++count;
		}
		public void finish() throws IOException {}
		public String getString() { return (stringBuffer.toString());}
	}

	public static interface ExpPropertyListener extends PropertyChangeListener
	{
		public boolean removeAtClose();
		public void setExperiment(Experiment e);
		public Experiment getExperiment();
	}

	public static abstract class ExpPropertyAction implements ExpPropertyListener
	{
		private boolean experimentWide = true;
		private Experiment experiment = null;
		public ExpPropertyAction(boolean exp)
		{
			experimentWide = exp;
			if (exp) experiment = ExpCoordinator.theCoordinator.getCurrentExp();
			ExpCoordinator.theCoordinator.addPropertyListener(this);
		}
		//interface Experiment.PropertyListener
		public boolean removeAtClose() { return experimentWide;}
		public void setExperiment(Experiment e) {experiment = e;}
		public Experiment getExperiment() { return experiment;}
		//end Experiment.PropertyListener
		//public void propertyChange(PropertyChangeEvent e);//needs to be defined
	}
	public static abstract class UserAction extends AbstractAction //implements ExpPropertyListener //PropertyChangeListener
	{
		private boolean ignoreObsStatus = false;
		private boolean experimentWide = true;
		private ExpPropertyAction expPAction = null;
		public UserAction() { this(false, true);}
		public UserAction(boolean ign, boolean exp) 
		{
			this(null, ign, exp);
		}
		public UserAction(String nm) { this(nm, false, true);}
		public UserAction(String nm, boolean ign, boolean exp) 
		{ 
			super(nm);
			ignoreObsStatus = ign;
			experimentWide = exp;
			if (!ign) 
			{
				expPAction = (new ExpPropertyAction(exp)
				{            
					public void propertyChange(PropertyChangeEvent e)
					{
						if (e.getPropertyName().equals(ExpCoordinator.RECORDING))
							setEnabled(!ExpCoordinator.isRecording());
					}
				});
				setEnabled(true);
			}
		}
		public boolean isEnabled()
		{
			if (ExpCoordinator.isRecording()) return false;
			else return(super.isEnabled());
		}
	    public void propertyChange(PropertyChangeEvent e){}
	}

	public static class CompositeAction extends UserAction
	{
		protected Vector actions;
		public CompositeAction() { this("");}
		public CompositeAction(String lbl) { this(lbl, false, false);}
		public CompositeAction(String lbl, boolean ign, boolean exp)
		{
			super(lbl, ign, exp);
			actions = new Vector();
		}
		public void actionPerformed(ActionEvent e)
		{
			if (!isEnabled()) return;
			int max = actions.size();
			UserAction elem;
			for (int i = 0; i < max; ++i)
			{
				elem = (UserAction)actions.elementAt(i);
				if (elem.isEnabled()) elem.actionPerformed(e);
			}
		}

		//    protected boolean elementIsEnabled(UserAction a) 
		//{ 
		//if (mode.getCurrentTool() == null)
		//  return (a.isEnabled(mode.getCurrentMode()));
		//else return (a.getTool() == mode.getCurrentTool());
		// }
		public void addAction(UserAction a)
		{
			if (!actions.contains(a)) actions.add(a);
		}
		public void removeAction(UserAction a)
		{
			if (actions.contains(a)) actions.remove(a);
		}
	}
	public static class XMLSWriter 
	{
		private XMLStreamWriter xmlWrtr = null;
		private BufferedWriter bWrtr = null;

		public XMLSWriter(XMLStreamWriter x_wrtr, BufferedWriter b_wrtr)
		{
			xmlWrtr = x_wrtr;
			bWrtr = b_wrtr;
		}
		public void writeStartElement(String s) throws XMLStreamException { xmlWrtr.writeStartElement(s);}
		public void writeEndElement() throws XMLStreamException { xmlWrtr.writeEndElement();}
		public void writeCharacters(String s) throws XMLStreamException { xmlWrtr.writeCharacters(s);}
		public void writeAttribute(String a, String v)  throws XMLStreamException { xmlWrtr.writeAttribute(a,v);}
		public void newLine() { 
			/*
      try{
	bWrtr.newLine();
      }
      catch (java.io.IOException e)
	{
	  ExpCoordinator.print(new String("ONL.XMLSWriter.newLine IOException " + e.getMessage()));
	  e.printStackTrace();
	  }*/
		}
	}
	public static class XMLWriter implements ONL.Writer //use to define an NCCP and File writer that writes a component the File writer will add new lines so can be read
	{
		private XMLStreamWriter xmlWrtr = null;

		public XMLWriter(XMLStreamWriter xml_wrtr)
		{
			xmlWrtr = xml_wrtr;
		}
		public void writeString(String s) throws IOException { writeLine(s);}
		public void writeByte(int i) throws IOException  { writeLine(String.valueOf(i));}
		public void writeShort(int i) throws IOException { writeLine(String.valueOf(i));}
		public void writeInt(int i) throws IOException { writeLine(String.valueOf(i));}
		public void writeDouble(double i) throws IOException { writeLine(String.valueOf(i));}
		public void writeFloat(float i) throws IOException { writeLine(String.valueOf(i));}
		public void writeUnsignedInt(double i) throws IOException { writeLine(String.valueOf((int)i));}
		public void writeLine(String s) throws IOException
		{
			try{
				xmlWrtr.writeCharacters(s.concat("\n"));
			}
			catch (XMLStreamException e)
			{
				ExpCoordinator.print(new String("XMLWriter.writeLine XMLStreamException " + e.getMessage()));
			}
		}
		public void writeBoolean(boolean b) throws IOException { writeLine(String.valueOf(b));}
		public void finish() throws IOException{}
	}

	public static class IPAddress
	{   
		protected String ip_str = "0.0.0.0";
		protected byte[] ip_bytes;
		public IPAddress()
		{
			ip_bytes = new byte[4];
			for (int i = 0; i < 4; ++i)
				ip_bytes[i] = 0;
		}
		public IPAddress(String s) throws java.text.ParseException
		{
			this();
			parseString(s);
		}
		public IPAddress(IPAddress ip) //throws java.text.ParseException
		{
			this();
			ip_str = new String(ip.ip_str);  
			for (int i = 0; i < 4; ++i)
				ip_bytes[i] = ip.ip_bytes[i];
		}

		public String toString() { return (new String(ip_str));}
		public byte[] getBytes() { return ip_bytes;}
		public int getInt() 
		{ 
			int rtn = ip_bytes[0] & 0x000000FF;
			for (int i = 1; i < 4; ++i)
			{
				rtn = (rtn << 8) | (ip_bytes[i] & 0x000000FF);
			}
			ExpCoordinator.print(new String("ONL.IPAddress(" + ip_str + ").getInt " + rtn + " ip_bytes[0]=" + ip_bytes[0] + " ip_bytes[1]=" + ip_bytes[1] + " ip_bytes[2]=" + ip_bytes[2] + " ip_bytes[3]=" + ip_bytes[3]), TEST_IPADDRESS); 
			return rtn;
		}
		protected void parseString(String ip) throws java.text.ParseException
		{
			String str = getAddrFromLabel(ip);
			ExpCoordinator.print(new String("ONL.IPAddress.parseString ip_str:" + ip + " addr:" + str), TEST_IPADDRESS);
			String[] iparray = str.split("\\."); //need new java to use this
			int len = Array.getLength(iparray);
			int i = 0;
			if (len != 4 ) 
			{
				String msg = "ONL.IPAddress.parseString error: ip not of form x.x.x.x ";
				ExpCoordinator.print(msg, HWTable.TEST_HWTABLE);
				throw new java.text.ParseException(msg, 0);
			}
			else
			{
				try
				{
					int tmp_int;
					for (i = 0; i < 4; ++i)
					{
						tmp_int = Integer.parseInt(iparray[i]);
						if (tmp_int < 0 || tmp_int > 255) 
						{
							String msg = "ONL.IPAddress.parseString error: ip address component out of range 0-255";
							ExpCoordinator.print(msg, TEST_IPADDRESS);
							throw new java.text.ParseException(msg, 0);
						}
						else 
						{
							//System.out.println("                      token[" + i +"] = " + iparray[i]); 
							ip_bytes[i] = (Integer.decode(iparray[i])).byteValue();
						}
					}
					ip_str = new String(iparray[0] + "." + iparray[1] + "." + iparray[2]+ "." + iparray[3]);
					ExpCoordinator.print(new String("ONL.IPAddress.parseString ip_str:" + ip_str + " ip_bytes[0]=" + ip_bytes[0] + " ip_bytes[1]=" + ip_bytes[1] + " ip_bytes[2]=" + ip_bytes[2] + " ip_bytes[3]=" + ip_bytes[3]), TEST_IPADDRESS); 
				}
				catch (NumberFormatException e)
				{ 
					String msg = new String("ONL.IPAddress.parseString error: " + e.getMessage());
					ExpCoordinator.print(new String(msg + " --- index i = " + i), TEST_IPADDRESS);
					throw new java.text.ParseException(msg, 0);
				}
			}
		}
		public boolean isZero() { return (ip_str.equals("0.0.0.0"));}
		public boolean equals(Object o) 
		{
			boolean rtn = super.equals(o);
			if (!rtn && (o instanceof IPAddress))
				rtn = ip_str.equals(((IPAddress)o).ip_str);
			return rtn;
		}
	}
	public static String getAddrFromLabel(String lbl)
	{
		int b2 = 0;
		int b3 = 0;
		   ExpCoordinator.print(new String("ONL.getAddrFromLabel " + lbl), 8);
		if (lbl.startsWith(HST_STR)) 
		{
			b2 = HST_NDX;
			b3 = 100 + Integer.parseInt(lbl.substring(3));
		}
		else if (lbl.startsWith("h"))
		{
		   String strarray[] = lbl.split("h|x");
		   ExpCoordinator.print(new String("ONL.getAddrFromLabel " + lbl +" " + strarray[0] + " : " + strarray[1] + ":" + strarray[2]), 8);
		   b2 = Integer.parseInt(strarray[1]);
		   //b3 = Integer.parseInt(strarray[1]);
		   b3 = Integer.parseInt(strarray[2]);
		}
		if (b2 > 0 || b3 > 0) return (new String("192.168." + b2 + "." + b3));
		else return lbl;
	} 

	///////////////////////////////////////////////////// ONL.Log /////////////////////////////////////////////////////   
	public static class Log 
	{
		private Vector lines = null;
		private JDialog dialog = null;
		private JTextArea log = null;
		private String title = "Log";
		private boolean editable = true;
		private boolean show = true;
		private int width = 225;
		private int height = 120;

		public Log(String ttl, boolean s)
		{
			this(ttl);
			show = s;
		}
		public Log(String ttl)
		{
			title = ttl;
			lines = new Vector();
		}
		public void addLine(String s)
		{
			initLog();
			if (show) showLog();
			lines.add(s);
			String msg = s;
			if (!msg.endsWith("\n")) 
			{
				ExpCoordinator.printer.print(new String("ONL.Log(" + title + ").addLine (" + msg + ") adding new line"), 2);
				msg = msg.concat("\n");
			}
			else 
				ExpCoordinator.printer.print(new String("ONL.Log(" + title + ").addLine (" + msg + ")"), 2);
			log.append(msg);
		}
		private void initLog()
		{
			if (log == null) 
			{
				log = new JTextArea();
				log.setEditable(editable);
				//log.setEditable(false);
				log.setLineWrap(true);
				log.append("\n");
				int max = lines.size();
				for (int i = 0; i < max; ++i)
					log.append((String)lines.elementAt(i));
			}
		}
		public void showLog()
		{
			initLog();
			if (dialog == null)
			{
				dialog = new JDialog(ExpCoordinator.getMainWindow(), new String(title));
				dialog.setSize(width, height);                       
				JScrollPane sp = new JScrollPane(log);
				sp.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
				dialog.getContentPane().add(sp);
			}
			if (!dialog.isVisible()) dialog.setVisible(true);
		}

		public void setSize(int w, int h)
		{
			width = w;
			height = h;
			if (dialog != null) dialog.setSize(w, h);
		}
		protected void clear()
		{ 
			if (dialog != null) 
			{
				dialog.dispose();
				dialog = null;
			}
		}
		public void setTitle(String ttl)
		{
			title = ttl;
			if (dialog != null) dialog.setTitle(ttl);
		}
		public void setEditable(boolean b) 
		{
			if (log != null) log.setEditable(b);
			editable = b;
		}
	}
	public static void addAllNoDupe(Vector v1, Vector v2)
	{
		int max = v2.size();
		for (int i = 0; i < max; ++i)
		{
			if (!v1.contains(v2.elementAt(i))) v1.add(v2.elementAt(i));
		}
	}
	
	
}
