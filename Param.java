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
import java.util.Vector;
import java.lang.String;
import java.lang.reflect.Array;
import java.io.*;
import javax.xml.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.swing.*;

import java.awt.Component;
import java.awt.Dimension;

public class Param extends ParamSpec
{
	private Object committedValue = null;
	//private PComponent uiRep = null;
	private JComponent uiRep = null;
	private Hardware hardware = null;
	private Hardware.Port hwport = null;
	protected ONLComponent parentComponent = null;

	///////////////////////////////////////////////////// Param.PContentHandler ////////////////////////////////////////////////
	protected static class PContentHandler extends DefaultHandler
	{
		private ExperimentXML expXML = null;
		protected String currentElement = "";
		protected Param currentParam = null;

		public PContentHandler(ExperimentXML exp_xml, Param p)
		{
			super();
			expXML = exp_xml;
			currentParam = p;
		}
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
		{
			currentElement = new String(localName);
		}
		public void characters(char[] ch, int start, int length)
		{
			ExpCoordinator.print(new String("Param.PContentHandler.characters currentElement:" + currentElement + " chars:" + new String(ch, start, length)), ExperimentXML.TEST_XML);
			if (currentElement.equals(ExperimentXML.PDEFAULT) && currentParam != null)// &&(!currentParam.isField())) 
			{
				currentParam.setDefaultFromString(new String(ch, start, length));
			}
		}
		public void endElement(String uri, String localName, String qName)
		{
			ExpCoordinator.print(new String("ParamSpec.XMLHandler.endElement " + localName), ExperimentXML.TEST_XML);
			if (localName.equals(currentParam.getXMLElemName()))//ExperimentXML.PARAM)) 
			{
				currentParam = null;
				expXML.removeContentHandler(this);
			}
		}
	}//end Param.PContentHandler
	
	////////////////////////////////////////////// Param.NextHopPanel /////////////////////////////////////////////////////
	protected class NextHopPanel extends JPanel
	{
	  private JFormattedTextField nhport = null;
	  private JFormattedTextField nhip = null;
	  
	  public NextHopPanel(NextHop nh, String d_lbl, boolean e)
	  {
		  super();
		  setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		  nhport = new JFormattedTextField(new Integer(nh.getPort()));
		  nhport.setEditable(e);
		  nhip = new JFormattedTextField(new ONL.IPAddress(nh.getNextHopIP()));
		  nhip.setEditable(e);
		  TextFieldwLabel fld = new TextFieldwLabel(nhport, new String(d_lbl + "  nh port:"));
		  fld.setEditable(e);
		  fld.setMinimumSize(new Dimension(150, 15));//fld.getHeight()));
		  fld.setPreferredSize(new Dimension(150, 15));//fld.getHeight()));
		  add(fld);
		  fld = new TextFieldwLabel(nhip, "   nh ip:");
		  fld.setMinimumSize(new Dimension(250, 15));//fld.getHeight()));
		  fld.setPreferredSize(new Dimension(250, 15));//fld.getHeight()));
		  fld.setEditable(e);
		  add(fld);
		  setPreferredSize(new Dimension(250,60));
	  }
	  public NextHop getNextHop()
	  {
		  int p = Integer.parseInt(nhport.getText());
		  try
		  {
			  ONL.IPAddress ip = new ONL.IPAddress(nhip.getText());
			  return (new NextHop(p, ip, hardware));
		  }
		  catch (java.text.ParseException ex)
		  { 
			  ExpCoordinator.print(new String("Param.NextHopPanel.getNextHop error:" + ex.getMessage()));
		  }
		  return (new NextHop(p, hardware));
	  }
	}

	///////////////////////////////////////////////////// Param methods ///////////////////////////////////////////////////
	public Param(Param p)
	{
		super((ParamSpec)p);
		parentComponent = p.parentComponent;
		hardware = p.hardware;
		hwport = p.hwport;
		if (p.format != null) format = new String(p.format);
		setCurrentValue(p.getCurrentValue());
		if (p.committedValue != null) setCommittedValue(p.committedValue);
	}
	public Param(ParamSpec ps)
	{
		super(ps);
	}
	public Param(ParamSpec ps, ONLComponent pc, Command cs)
	{
		super(ps);
		setParent(pc);
		setCommandSpec(cs);
	}
	public void setParent(ONLComponent pc)
	{
		parentComponent = pc;
		if (pc == null) 
		{
			ExpCoordinator.print(new String("Param(" + label + ").setParent null"), TEST_PARAM);
			return;
		}
		ONLComponent o = pc;
		//hardware = pc.getHardware();
		//ExpCoordinator.print(new String("Param(" + label + ").setParent " + pc.getLabel() + " hardware:" + hardware.getLabel()), TEST_PARAM);
		while (o != null && !(o instanceof Hardware))
		{
			if (o instanceof Hardware.Port) hwport = (Hardware.Port)o;
			o = o.getParent();
		}
		if (o != null && (o instanceof Hardware)) hardware = (Hardware)o;
		if (defaultValue != null && isNextHop()) ((NextHop)defaultValue).setHardware(hardware);
	}

	protected Object getNewUIRep()
	{

		String d_lbl = new String(label + ": ");
		Object rtn = null;
		if (choices != null)
		{
			JComboBox cb = new JComboBox(choices.toArray());
			cb.setEditable(choiceEditable);
			if (defaultValue != null) 
			{
				int max = cb.getItemCount();
				boolean found = false;
				for (int i = 0; i < max; ++i)
				{
					if (cb.getItemAt(i).toString().equals(defaultValue.toString()))
					{
						cb.setSelectedIndex(i);
						found = true;
						break;
					}
				}
				if (!found && choiceEditable) cb.setSelectedItem(defaultValue);
			}
			ExpCoordinator.print(new String("Param(" + label + ").getNewUIRep comboBox in componentwLabel d_lbl:" + d_lbl + " defaultValue:" + defaultValue), TEST_CHOICE);
			rtn = new ONL.ComponentwLabel(cb, d_lbl);
		}
		else
		{
			if (isInt())
			{
				ExpCoordinator.print(new String("Param(" + getLabel() + ").getNewUIRep Int wildcard:" + wildcardValue), TEST_WILDCARD);
				JFormattedTextField fld;
				if (defaultValue != null) 
				{
					if (wildcardValue == null) fld = new JFormattedTextField((Integer)defaultValue);
					else 
					{
						ExpCoordinator.print(new String("Param(" + getLabel() + ").getNewUIRep using WCInteger"), TEST_WILDCARD);
						fld = new JFormattedTextField(new WCInteger(defaultValue));
					}
				}
				else
				{
					if (wildcardValue == null) fld = new JFormattedTextField((Integer)0);
					else 
					{
						ExpCoordinator.print(new String("Param(" + getLabel() + ").getNewUIRep using WCInteger"), TEST_WILDCARD);
						fld = new JFormattedTextField(new WCInteger(0));
					}
				}
				fld.setEditable(editable);
				rtn = new TextFieldwLabel(fld, d_lbl);
				((TextFieldwLabel)rtn).setEditable(editable);
			}
			if (isDouble())
			{
				JFormattedTextField fld;
				if (defaultValue != null) 
				{
					if (wildcardValue == null) fld = new JFormattedTextField((Double)defaultValue);
					else fld = new JFormattedTextField(new WCDouble(defaultValue));
				}
				else
				{
					if (wildcardValue == null) fld = new JFormattedTextField(new Double(0));
					else fld = new JFormattedTextField(new WCDouble((double)0));
				}
				fld.setEditable(editable);
				rtn = new TextFieldwLabel(fld, d_lbl);
				((TextFieldwLabel)rtn).setEditable(editable);
			}
			if (isBool()) 
			{
				JCheckBox rtn_chk = new JCheckBox(d_lbl);
				rtn_chk.setHorizontalTextPosition(SwingConstants.LEFT);
				if (defaultValue != null) rtn_chk.setSelected(((Boolean)defaultValue).booleanValue());
				rtn_chk.setEnabled(editable);
				rtn_chk.setAlignmentX(Component.LEFT_ALIGNMENT);
				rtn = rtn_chk;
			}
			if (isIPAddr())
			{
				JFormattedTextField fld = new JFormattedTextField(new ONL.IPAddress());
				if (defaultValue != null) fld.setValue((ONL.IPAddress)defaultValue);
				fld.setEditable(editable);
				rtn = new TextFieldwLabel(fld, d_lbl);
				((TextFieldwLabel)rtn).setEditable(editable);
			}
			if (isNextHop())
			{
				/*JFormattedTextField fld = null;
				if (isNextHopIP()) fld = new JFormattedTextField(new NextHopIP((NextHopIP)defaultValue));
				else fld = new JFormattedTextField(new NextHop((NextHop)defaultValue));
				fld.setEditable(editable);
				rtn = new TextFieldwLabel(fld, d_lbl);
				((TextFieldwLabel)rtn).setEditable(editable);*/
				 rtn = new NextHopPanel((NextHop)defaultValue, d_lbl, editable);
				 //rtn.setEditable(editable);
				 
			}
			if (rtn == null)
			{
				rtn = new TextFieldwLabel(50, d_lbl);
				if (defaultValue != null) ((TextFieldwLabel)rtn).setValue((String)defaultValue);
				((TextFieldwLabel)rtn).setEditable(editable);
			}
		}
		if (rtn != null)
		{
			if (toolTipText != null && toolTipText.length() > 0)
			{
				ExpCoordinator.print(new String("    setting tooltip: " + toolTipText), TEST_TOOLTIP);
				((JComponent)rtn).setToolTipText(toolTipText);
			}
			((JComponent)rtn).setPreferredSize(new Dimension(preferredWidth, 25));
		}

		return rtn;
	}
	public Object getUIRep() 
	{
		if (uiRep == null)
		{
			uiRep = (JComponent)getNewUIRep();
		}
		return uiRep;
	}

	public void updateFromUIRep()
	{
		Object rtn = null;
		if (uiRep != null)
		{
			Object uir = getUIRep();
			if (uir != null)
			{
				rtn = getValueFromUIRep(uir);
				if (rtn != null) setCurrentValue(rtn);
			}
		}
	}
	public Object getValueFromUIRep(Object uir)
	{
		Object rtn = null;
		if (uir != null)
		{
			if (uir instanceof ONL.ComponentwLabel) 
			{
				JComponent c = ((ONL.ComponentwLabel)uir).getJComponent();
				if (c instanceof JComboBox)
				{
					rtn = ((JComboBox)c).getSelectedItem();
					ExpCoordinator.print(new String("Param(" + getLabel() + ").getValueFromUIRep " + rtn + " items in comboBox:" + ((JComboBox)c).getItemCount() + " selectedIndex:" + ((JComboBox)c).getSelectedIndex()), TEST_CHOICE);
					//Throwable t = new Throwable();
					//t.printStackTrace();
					//if editable need to convert string
					if (choiceEditable && !isString())
					{
						if (!choices.contains(rtn)) rtn = getValueFromString(rtn.toString());
					}
				}
			}
			if (uir instanceof TextFieldwLabel)
			{
				String val = ((TextFieldwLabel)uir).getText();
				rtn = getValueFromString(val);
				ExpCoordinator.print(new String("Param(" + label + ").getValueFromUIRep val:" + val), TEST_PARAM);
			}
			else
			{
				if (uir instanceof JCheckBox)
					rtn = new Boolean(((JCheckBox)uir).isSelected());
				else if (uir instanceof NextHopPanel)
				{
					rtn = ((NextHopPanel)uir).getNextHop();
					((NextHop)rtn).setHardware(hardware);
				}
			}
			if (rtn == null)  
			{
				if (isInt()) rtn = new Integer(0);
				if (isDouble()) rtn = new Double(0);
				if (isIPAddr()) rtn = new ONL.IPAddress();
				if (isBool()) rtn = new Boolean(false);
				if (isNextHop())
				{
					NextHop rtn_nh;
					if (isNextHopIP()) rtn_nh = new NextHopIP();
					else rtn_nh = new NextHop();
					rtn_nh.setHardware(hardware);
					rtn = rtn_nh;
				}
				if (rtn == null) rtn = new String("");
			}
			ExpCoordinator.print(new String("Param(" + label + ").getValueFromUIRep  return value " + rtn.toString()), TEST_CHOICE);
		}
		return rtn;
	}
	public void setDefaultValue(Object o)
	{
		super.setDefaultValue(o);
		if (defaultValue instanceof NextHop) ((NextHop)defaultValue).setHardware(hardware);
		if (o != null) ExpCoordinator.print(new String("Param(" + label + ").setDefaultValue to:" + o.toString() + " defaultValue:" + defaultValue.toString()), TEST_PARAM);
	}
	public void setDefaultFromString(String s)
	{
		super.setDefaultFromString(s);
		if (defaultValue instanceof NextHop) ((NextHop)defaultValue).setHardware(hardware);
	}
	public Object getCommittedValue() { return committedValue;}
	public void setCommittedValue(Object o)
	{
		if (o == null) return;
		if (isInt() && o instanceof Integer) committedValue = new Integer((Integer)o);
		if (isInt() && o instanceof WCInteger) committedValue = new WCInteger((WCInteger)o);
		if (isDouble() && o instanceof Double) committedValue = new Double((Double)o);
		if (isDouble() && o instanceof WCDouble) committedValue = new WCDouble((WCDouble)o);
		if (isBool() && o instanceof Boolean) committedValue = new Boolean((Boolean)o);
		if ((isString() || isPassword()) && o instanceof String) committedValue = new String((String)o);
		if (isIPAddr() && o instanceof ONL.IPAddress) committedValue = new ONL.IPAddress((ONL.IPAddress)o);  
		if (isNextHop() && o instanceof NextHop) 
		{
			if (isNextHopIP()) committedValue = new NextHopIP((NextHopIP)o);  
			else  committedValue = new NextHop((NextHop)o);  
			((NextHop)committedValue).setHardware(hardware);
		}
	}
	public void setCommittedValue() { setCommittedValue(defaultValue);}
	public boolean needsCommit()
	{
		if (committedValue == null) return true;
		if (defaultValue != null & !defaultValue.toString().equals(committedValue.toString())) return true;
		return false;
	}
	public boolean isEqual(ParamSpec p)
	{
		if (p instanceof Param)
		{
			Param tmp_param = (Param)p;
			return (super.isEqual(tmp_param) && getCurrentValue().equals(tmp_param.getCurrentValue()));
		}
		return false;
	}
	public Object getCurrentValue() 
	{ 
		if (defaultValue == null)
		{
			ExpCoordinator.print(new String("Param(" + label + ").getCurrentValue defaultValue null"), TEST_PARAM);
			if (isInt()) defaultValue = new Integer(0);
			if (isDouble()) defaultValue = new Double(0);
			if (isBool()) defaultValue = new Boolean(false);
			if (isString()) defaultValue = new String("");
			if (isPassword()) setDefaultValue();
			if (isIPAddr()) defaultValue = new ONL.IPAddress();
			if (isNextHop() && !isNextHopIP()) defaultValue = new NextHop(0, hardware);
			if (isNextHopIP()) defaultValue = new NextHopIP(0, hardware);
		} 
		else
			ExpCoordinator.print(new String("Param(" + label + ").getCurrentValue defaultValue:" + defaultValue.toString()), TEST_PARAM);
		return (getDefaultValue());
	}
	public void setCurrentValue(Object o) { setDefaultValue(o);}
	public Hardware getHardware() { return hardware;}
	public Hardware.Port getHWPort() { return hwport;}
	public ONLComponent getParent() { return parentComponent;}
	protected void setUIRep(Object uir) { uiRep = (JComponent)uir;}
	public void writeXML(XMLStreamWriter xmlWrtr) throws XMLStreamException
	{
		xmlWrtr.writeStartElement(getXMLElemName());
		xmlWrtr.writeAttribute(ExperimentXML.PLABEL, label);
		if (isEditable())
		{
			xmlWrtr.writeStartElement(ExperimentXML.PDEFAULT);
			xmlWrtr.writeCharacters(getCurrentValue().toString());
			xmlWrtr.writeEndElement();
		}
		xmlWrtr.writeEndElement();
	}
	public ContentHandler getContentHandler(ExperimentXML exp_xml) { return (new Param.PContentHandler(exp_xml, this));}  
}
