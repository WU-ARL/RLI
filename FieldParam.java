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

public class FieldParam extends Param
{
	public static final int TEST_FIELDPARAM = 4;
	private Field field = null;
	private Field.Owner fieldOwner = null;
	private Field.UIRepCopy uiRepCopy = null;

	public FieldParam(FieldParam p)
	{
		super((Param)p);
		field = p.field;
		fieldOwner = p.fieldOwner;
		//uiRepCopy = field.getUIRepCopy();//REMOVED 8/10/10
		//setUIRep(uiRepCopy.getComponent());//REMOVED 8/10/10
		ExpCoordinator.print(new String("FieldParam(" + label + ")(FieldParam) default:" + p.getDefaultValue()), TEST_FIELDPARAM);
	}

	public FieldParam(ParamSpec fps, ONLComponent pc, Command cs) 
	{
		super((ParamSpec)fps, pc, cs);
		if (cs.getFieldOwner() == null && pc instanceof Field.Owner) 
			fieldOwner = (Field.Owner)pc;
		else fieldOwner = cs.getFieldOwner();
		getField();
		setDefaultValue(fps.getDefaultValue());
		ExpCoordinator.print(new String("FieldParam(" + label + ")(ParamSpec, ONLComponent, Command) command:" + cs.getLabel() + " default:" + fps.getDefaultValue()), TEST_FIELDPARAM);
	}

	public Field getField()
	{
		if (field == null && fieldOwner != null)
		{
			setField(fieldOwner.getField(getLabel()));
			if (field != null) 
			{
				//uiRepCopy = field.getUIRepCopy(isEditable()); //REMOVED 8/10/10
				//setUIRep(uiRepCopy.getComponent()); //REMOVED 8/10/10
				if (field.wildcardValue != null) wildcardValue = field.wildcardValue;
			}
		}
		return field;
	}

	public Object getUIRep()
	{
		if (uiRepCopy == null)  
		{
			if (field == null) ExpCoordinator.print(new String("FieldParam(" + getLabel() + ").getUIRep field is null"), TEST_UIREP);
			else ExpCoordinator.print(new String("FieldParam(" + getLabel() + ").getUIRep field:" + field.toString()), TEST_UIREP);
			uiRepCopy = field.getUIRepCopy(isEditable()); 
			setUIRep(uiRepCopy.getComponent());
		}
		return (super.getUIRep());
	}
	public void setField(Field f) { field = f;}
	protected void setFieldOwner(Field.Owner o) { fieldOwner = o;}
	public Field.Owner getFieldOwner() { return fieldOwner;}
	public Object getDefaultValue() { return (field.getDefaultValue());}
	public void setDefaultValue(Object o)
	{
		if (field != null) field.setDefaultValue(o);
	}
	public void setDefaultFromString(String s)
	{
		if (field != null) field.setDefaultFromString(s);
	}
	public Object getCommittedValue() { return (field.getCommittedValue());}
	public void setCommittedValue(Object o)
	{
		field.setCommittedValue(o);
	}
	public void setCommittedValue() { field.setCommittedValue();}
	public boolean needsCommit()
	{
		return (field.needsCommit());
	}
	public boolean isEqual(ParamSpec p)
	{
		if (p instanceof FieldParam)
		{
			FieldParam tmp_param = (FieldParam)p;
			return (field == tmp_param.getField() && super.isEqual(tmp_param) && getCurrentValue().equals(tmp_param.getCurrentValue()));
		}
		return false;
	}
	public Object getCurrentValue() 
	{
		if (field != null) return (field.getCurrentValue());
		return null;
	}
	public void setCurrentValue(Object o) { field.setCurrentValue(o);}
	public boolean isInt() { return (field != null && field.isInt());}
	public boolean isString() { return (field != null && field.isString());}
	public boolean isBool() { return (field != null && field.isBool());}
	public boolean isDouble() { return (field != null && field.isDouble());}
	public boolean isIPAddr() { return (field != null && field.isIPAddr());}
	public boolean isNextHop() { return (field != null && field.isNextHop());}
}
