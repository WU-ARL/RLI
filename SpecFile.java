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
 * File: SpecFile.java
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
import java.util.Vector;
import java.util.StringTokenizer;
import java.lang.String;
import java.lang.reflect.Array;
import java.awt.Point;
import java.awt.Dimension;


public class SpecFile //saves(loads) one or more graphs to(from) a file
{
  public static final String tokPARAM = "PARAM";
  public static final String tokMFPARAM = "MFPARAM";
  public static final String tokGRAPH= "GRAPH";
  private MonitorManager monitorManager = null;
  private boolean at_end = false;
  public static class SpecWriter
  {
    private ONL.Writer writer = null;
    private Vector tokens;
    private File spec_file = null;
    public SpecWriter(ONL.Writer w)
      {
	writer = w;
	tokens = new Vector();
      }
    //public void newLine() throws IOException  { writer.newLine();}
    public void writeLine(String str) throws IOException 
      {
	writer.writeLine(str);
	//writer.newLine();
      }
    public void writeToken(String type) throws IOException 
      {
	writeLine(type);
	tokens.add(type);
      }
    //public void flush() throws IOException { writer.flush();}
    public boolean writeEndToken()  throws IOException { return (writeEndToken(null));}
    public boolean writeEndToken(String type) throws IOException 
      {
	int t_end = tokens.size() - 1;
	if (t_end >= 0)
	  {
	    String elem = (String) tokens.elementAt(t_end);
	    if (type == null || elem.compareTo(type) == 0) 
	      {
		writeLine(new String("END" + elem));
		tokens.removeElementAt(t_end);
		return true;
	      }
	  }
	return false;
      }
    public ONL.Writer getONLWriter() { return writer;}
  }

  public static class SpecReader
  {
    private ONL.Reader reader = null;
    private boolean at_token;
    private String currentToken;
    private Vector tokens;
    private File spec_file = null;
    public SpecReader(ONL.Reader r)
      {
	this(r, null);
      }
    public SpecReader(ONL.Reader r, File f)
      {
	reader = r;
	tokens = new Vector();
	spec_file = f;
      }
    public String readNextToken() throws IOException 
      {
	String rtn = null;
	at_token = false;
	//System.out.println("SpecReader::readNextToken");
	while (reader.ready() && !atToken()) 
	  {
	    rtn = readLine();
	  }
	if (atToken()) return rtn;
	else return null;
      }
    public boolean isEndToken(String tok)
      {
	if (tok != null)
	  return (tok.startsWith("END"));
	else return false;
      }
    //public void close()  throws IOException { reader.close();}
    public boolean inParam() {return (((currentToken != null) && currentToken.compareTo(tokPARAM) == 0));}
    public boolean inMFParam() { return((currentToken != null) &&  (currentToken.compareTo(tokMFPARAM) == 0));}
    public boolean inGraph() { return ((currentToken != null) && (currentToken.compareTo(tokGRAPH) == 0));}
    public boolean atToken() { return at_token;}
    public String readLine()  throws IOException 
      {
	at_token = false;
	if (reader.ready())
	  {
	    String line = reader.readLine();
	    ExpCoordinator.print(new String("SpecReader::readLine (" + line + ")"), 5);
	    at_token = addToken(line);
	    return line;
	  }
	return null;
      }
    public void readToEnd(int level) throws IOException 
      {
	while (tokens.size() >= level) readLine();
      }
    private boolean addToken(String ln) throws IOException 
      {
	boolean rtn = false;
	if (ln.startsWith("END") && !tokens.isEmpty())
	  {
	    int t_end = tokens.size() - 1;
	    String elem = (String)tokens.elementAt(t_end);
	    if (ln.endsWith(elem)) 
	      {
		//System.out.println("found " + ln);
		if (t_end > 0) 
		  currentToken = (String)tokens.elementAt(t_end -1);
		else currentToken = null;
		tokens.removeElementAt(t_end);
		rtn = true;
	      }
	  }
	else
	  {
	    if ((ln.compareTo(tokPARAM) == 0) ||
		(ln.compareTo(tokMFPARAM) == 0) ||
		(ln.compareTo(tokGRAPH) == 0))
	      {
		currentToken = ln;
		tokens.add(ln);
		rtn = true;
		//System.out.println("found " + currentToken);
	      }
	  }
	return rtn;
      }

    public int getLevel() { return tokens.size();}
    public boolean inLevel(int l) { return (l <= tokens.size());}
    public StringTokenizer tokenizeLine() throws IOException 
      {
	String line = readLine();
	if (line != null) return (new StringTokenizer(line));
	else return null;
      }
    public boolean ready()  throws IOException { return (reader.ready());}
    public File getSpecFile() { return spec_file;}
    public double getVersion() { return (reader.getVersion());}
  }//end inner class SpecReader


  //inner class ParamSpec
  public static class ParamSpec //saves(loads) a parameter at a time to(from) a file
  {
    private ExpCoordinator nlists = null;
    private MonitorManager monitorManager = null;
    public ParamSpec(MonitorManager mw)
      {
	monitorManager = mw;
	nlists = monitorManager.getExpCoordinator();
      }
    public Monitor getParameter(SpecReader reader) throws IOException 
      {
	int level = reader.getLevel();
	MonitorDataType.Base mdt = MonitorDataType.Base.loadFromFile(reader, nlists);
	Monitor m = null;
	if (mdt.isLiveData()) m = processLive(mdt, reader);
	if (mdt.isLogFile()) m = processLogFile(mdt, reader);
	if (mdt.isMonitorFunction()) m = processFormula(mdt, reader);
	if (mdt.isConstant()) m = processConstant(mdt, reader);
	ExpCoordinator.print(new String("ParamSpec::getParameter level" + level), 12);
	reader.readToEnd(level);
	return m;
      }
    
    private Monitor processLive(MonitorDataType.Base mdt, SpecReader reader) throws IOException
      {
	while (reader.ready() && (reader.readLine().compareTo("ENDPARAM") != 0)) ; //read to the ENDPARAM
	//CHANGE need to change to look up the correct component and monitor
        if (mdt.getONLComponent() == null) return null;
	ONLComponent.ONLMonitorable sd = mdt.getONLComponent().getMonitorable();
	if (sd != null) 
          {
            mdt.setMonitorable(sd);
            return (sd.addMonitor(mdt));
          }
	else return null;
      }
    
    private Monitor processLogFile(MonitorDataType.Base mdt, SpecReader reader) throws IOException
      {
	System.out.println("ParamSpec::processLogFile");
	while (reader.ready() && (reader.readLine().compareTo("ENDPARAM") != 0)) {} //read to the ENDPARAM
	Monitor m = null;
	String old_fname = ((MonitorDataType.LogFile)mdt).getFileName(); 
	//System.out.println("    processLogFile");
	String par = null; 
	File spec_file = reader.getSpecFile();
	if (spec_file != null)
	  par = spec_file.getParent(); //need to pass the file
	if (par != null)
	  ((MonitorDataType.LogFile)mdt).setFileName(par + File.separator + old_fname);
	else 
	  ((MonitorDataType.LogFile)mdt).setFileName(old_fname);
	((MonitorDataType.LogFile)mdt).setReplayType(MonitorDataType.LogFile.EXACT_REPLAY);
	m = monitorManager.getLFRegistry().addMonitorToFile((MonitorDataType.LogFile)mdt);
	//if (!hasControlPanel) //this should be moved to graph specific stuff
	//  {
	//   ntry.addControlPanel((NumberGraph)tmp_display);
	//   hasControlPanel = true;
	// }
	
	return m;
      }
    
    private Monitor processFormula(MonitorDataType.Base mdt, SpecReader reader) throws IOException
      {	
        Monitor m = new MonitorAlgFunction((MonitorDataType.MFormula)mdt, monitorManager);
        ((MonitorAlgFunction)m).getSpec().parse(reader);
        ((MonitorAlgFunction)m).start();
        
	return m;
      }

    private Monitor processConstant(MonitorDataType.Base mdt, SpecReader reader) throws IOException
      {
	while (reader.ready() && (reader.readLine().compareTo("ENDPARAM") != 0)) ; //read to the ENDPARAM
	Monitor m = new Constant((MonitorDataType.Constant)mdt);
	return m;
      }
    
    public void writeParam(MonitorDataType.Base mdt, SpecWriter writer) throws IOException
      {
	//System.out.println("ParamSpec::writeParam");
	mdt.print(1);
	writer.writeToken(tokPARAM);
	mdt.saveToFile(writer);
	writer.writeEndToken(tokPARAM);
      }
  }

  public static class MFParamSpec extends ParamSpec //saves(loads) a parameter at a time to(from) a file also parsing function specific parameter info
    {
      private MonitorManager monitorManager;
      public MFParamSpec(MonitorManager mw)
	{
	  super(mw);
	}
      public Monitor getMFParameter(SpecReader reader) throws IOException 
	{
	  //specific info here
	  Monitor m = null;
	  //System.out.println("SpecFile.MFParamSpec::getMFParameter");
	  int level = reader.getLevel();
	  if (reader.inLevel(level) && !reader.inParam())
	    {
	      reader.readNextToken();
	    }
	  if (reader.inParam())
	    {
	      //System.out.println("   calling getParameter");
	      m = getParameter(reader);
	    }

	  reader.readToEnd(level);
	  //System.out.println("MFParamSpec::getMFParameter level" + level);
	  return m;
	}
      public void writeParam(Monitor m, SpecWriter writer) throws IOException
	{
	  //System.out.println("MFParamSpec::writeParam");
	  MonitorDataType.Base mdt = m.getDataType();
	  writer.writeToken(tokMFPARAM);
	  writePrefix(m, writer); //writes any extra information
	  super.writeParam(mdt, writer);
	  writeSuffix(m, writer);
	  writer.writeEndToken(tokMFPARAM);

	}
      protected void writePrefix(Monitor m, SpecWriter writer) throws IOException {}
      protected void writeSuffix(Monitor m, SpecWriter writer) throws IOException {}
    }

  public abstract static class MFunctionSpec //saves(loads) a graph to(from) a file
    {
      protected MonitorFunction mfunction = null;
      protected MFParamSpec paramSpec = null;
      protected MonitorManager monitorManager = null;
      private boolean is_graph = true;
      private String token_type = null;
      
      public MFunctionSpec(MonitorManager mw, MonitorFunction mf, MFParamSpec ps, boolean is_g)
	{
	  mfunction = mf;
	  paramSpec = ps;
	  monitorManager = mw;
	  is_graph = is_g;
	  if (is_g) token_type = tokGRAPH;
	  else token_type = tokMFPARAM;
	}
      
      public void parse(SpecReader reader) throws IOException 
	{
	  //readInfo(reader);//if want graph specific info override and put in here
	  Monitor m;
	  String tok = null;
	  int level = reader.getLevel();
	  //System.out.println("MFunctionSpec::parse level " + level);
	  if (reader.ready()) tok = reader.readNextToken();
	  while (reader.inLevel(level) && reader.ready())
	    {
	      //System.out.println("    MFunctionSpec::parse  level " + level + " read token: " + tok);
	      if (reader.inParam() || reader.inMFParam())
		{
		  if (reader.inParam())
		    {
		      //System.out.println("    MFunctionSpec::parse  inParam");
		      m = paramSpec.getParameter(reader); //reads ENDPARAM for this parameter 
		    }
		  else 
		    {
		      //System.out.println("    MFunctionSpec::parse  inMFParam");
		      m = paramSpec.getMFParameter(reader);
		    }
		  if (m != null) m.addMonitorFunction(mfunction);
		}
	      tok = reader.readNextToken();
	    }
	}
      public final void write(SpecWriter writer) throws IOException 
	{
	  //ParamSpec plainSpec = new ParamSpec(monitorManager);
	  writer.writeToken(token_type);
	  if (is_graph)
	    {
	      writeGraphInfo(writer);
	    }
	  writeParams(writer);
	  writer.writeEndToken(token_type);
	}
    public void writeGraphInfo(SpecWriter writer) throws IOException
      {
        if (!(mfunction instanceof MonitorManager.Display)) return;
	MonitorManager.Display display = (MonitorManager.Display)mfunction;
	writer.writeLine(new String(display.getTitle()));
        writer.writeLine(new String(display.getX() + " " + display.getY() + " " + display.getWidth() + " " + display.getHeight()));
	if (display.isGraph())
	  {
	    Graph graph = (Graph) display;
	    writer.writeLine(new String(graph.xlabel.getText() + " " + graph.ylabel.getText()));
	    writer.writeLine(new String(graph.getType() + " " + graph.yaxis.getUnits()) );
	  }
	else
	  {
	    writer.writeLine("");
	    writer.writeLine(String.valueOf(display.getType()));
	  }
      }

      protected abstract void writeParams(SpecWriter writer) throws IOException;//override for writing function specific data
      protected abstract void writeParam(SpecWriter writer, int i) throws IOException;//override for writing function specific data
    }


  public SpecFile(MonitorManager mw)
    {
      monitorManager = mw;
    }

  public MonitorManager.Display getNextDisplay(SpecReader reader) throws IOException
    {	  
      String line;
      MonitorManager.Display g = null;
      ExpCoordinator.print(new String("SpecFile::getNextDisplay"), 12);
      //SpecReader reader = new SpecReader(rdr);
      int level = reader.getLevel();
      while (reader.inLevel(level) && reader.ready())
	{
	  reader.readNextToken();
	  if (reader.inGraph())
	    {
	      g = readInfo(reader);
	      g.getSpec().parse(reader); //this should read the ENDGRAPH
	      return g;
	    }
	}
      at_end = (!reader.ready());
      return null;
    }

  private MonitorManager.Display readInfo(SpecReader reader) throws IOException
    {
      String title = reader.readLine();
      String[] strarray = reader.readLine().split(" ");
      String xlabel = null;
      String ylabel = null;
      MonitorManager.Display tmp_display = null;
      int len = Array.getLength(strarray);
      Point p = null;
      Dimension dim = null;
      if (reader.getVersion() >= 1.3 && len >= 4)
        {
          p = new Point(Integer.parseInt(strarray[0]),Integer.parseInt(strarray[1]));
          dim = new Dimension(Integer.parseInt(strarray[2]), Integer.parseInt(strarray[3]));
          strarray = reader.readLine().split(" ");
          len = Array.getLength(strarray);
        }
      if (len >= 2) 
	{
	  xlabel = strarray[0];
	  ylabel = strarray[1];
	}
      strarray = reader.readLine().split(" ");
      len = Array.getLength(strarray);
      int type = Integer.parseInt(strarray[0]);
      --len;
      //PROBLEM:: need to support old stuff
      //create new graph of type and add to display list and set as current
      tmp_display = monitorManager.addDisplay(type, title, xlabel, ylabel);
      if (p != null && dim != null) 
        {
          ExpCoordinator.theCoordinator.setWindowBounds(tmp_display, (int)p.getX(), (int)p.getY(), (int)dim.getWidth(), (int)dim.getHeight());
        }
      //switch(type)
      //{
      //case MonitorManager.Display.HISTOGRAM:
      //tmp_display = new Histogram(monitorManager, xlabel, ylabel, 3, false);
      //	break;
      //default:
      //	tmp_display = new MultiLineGraph(monitorManager, xlabel, ylabel, 2, 1, true);
      //}
      //tmp_display.setTitle(title); 
      if (len > 0) 
	{
	  int yunits = Integer.parseInt(strarray[1]);
	  if (tmp_display.isGraph()) ((Graph)tmp_display).setYUnits(yunits);
	}
      
      // MonitorManager.Display rtn = monitorManager.getDisplay(tmp_display);
      //if (rtn == null) rtn = tmp_display;
      return tmp_display;
    }
  
  public boolean atEnd() { return at_end;}
  public void writeGraph(SpecWriter writer, MonitorManager.Display g) throws IOException
    {
      if (g.getSpec() != null)
	g.getSpec().write(writer);
    }
}
