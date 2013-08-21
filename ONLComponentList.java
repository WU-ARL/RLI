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
 * 
 */

/*
 * File: ONLComponentList.java
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
import javax.swing.*;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.String;
import java.util.*;

public class ONLComponentList extends DefaultListModel
{
	//private int type = ONLComponent.UNKNOWN;
	private boolean links = false;
	public static int TEST_LIST = 4;
	public ONLComponentList(ONLComponentList l)
	{
		super();
		//type = l.type;
		int max = l.size();
		for (int i = 0; i < max; ++i)
			addComponent(l.onlComponentAt(i));
	}
	public ONLComponentList()
	{ 
		super();
	}
	/*
  public ONLComponentList(int tp)
    { 
      super();
      type = tp;
    }
	 */
	public boolean addComponent(ONLComponent sd)
	{
		if (!contains(sd))
		{
			//System.out.println("ONLComponentList::adding " + sd.getLabel());
			addElement(sd);
			return true; //added to list
		}
		return false; //already there
	}

	public boolean removeComponent(ONLComponent sd)
	{
		//System.out.println("ONLComponentList::removeNode " + sd.toString());
		if (contains(sd))
		{
			removeElement(sd);
			return true; //added to list
		}
		return false; //already there
	}

	public ONLComponent getONLComponent(String lbl, String tp)
	{
		return (getONLComponent(lbl, tp, 0));
	}

	public ONLComponent getONLComponent(String lbl, String tp, int level)
	{
		ONLComponent rtn_c = null;
		int max = size();
		String strarray[] = lbl.split(":|\t|\r|\n|\f");
		int i = 0;
		int token_cnt = Array.getLength(strarray);
		if (token_cnt <= level) 
		{
			ExpCoordinator.print(new String("ONLComponentList.getONLComponent size:" + max + " level:" + level + " lbl:" + lbl + " tp:" + tp + " token count:" + token_cnt), TEST_LIST);
			return null;
		}
		String tmp_lbl = strarray[0];
		ExpCoordinator.print(new String("ONLComponentList::getONLComponent size:" + max + " level:" + level + " lbl:" + lbl + " prefix[0]: " + tmp_lbl + " token count:" + token_cnt), TEST_LIST);
		for (i = 1; i <= level; ++i)
		{
			tmp_lbl = tmp_lbl.concat(new String(":" + strarray[i]));
			ExpCoordinator.print(new String("                                 prefix[" + i + "]: " + tmp_lbl),TEST_LIST);
		}
		for (i = 0; i < max; ++i)
		{
			rtn_c = (ONLComponent)elementAt(i);
			ExpCoordinator.print(new String("                                 in loop " + rtn_c.getLabel() + " for " + lbl + " at level " + level), TEST_LIST);
			if (rtn_c.isLabel(lbl))// && rtn_c.getType() == tp)
			{ 
				ExpCoordinator.print(new String("ONLComponentList::getONLComponent returning " + rtn_c.getLabel() + " " + level), TEST_LIST);
				return rtn_c;
			}
			if (rtn_c.isLabel(tmp_lbl))//rtn_c.getLabel().equals(tmp_lbl) && rtn_c.isParent(tp))//rtn_c.isEqual(tmp_lbl, tp) || rtn_c.isAncestor)//
			{
				/*	if (tmp_lbl.equals(lbl))// && rtn_c.isType(tp))
			{ 
			ExpCoordinator.print(new String("ONLComponentList::getONLComponent returning " + rtn_c.getLabel() + " " + level), TEST_LIST);
			return rtn_c;
			}
			else */
				return (rtn_c.getChild(lbl, tp, (level+1)));
			}
		}
		return null;
	}


	public ONLComponent getONLComponent(ONLComponent c)
	{
		ONLComponent rtn_c = null;
		int max = size();
		for (int i = 0; i < max; ++i)
		{
			rtn_c = (ONLComponent)elementAt(i);
			if (c == rtn_c || rtn_c.isEqual(c)) return rtn_c;
		}
		return null;
	}

	public ONLComponent getONLComponentByType(String tp)
	{
		ONLComponent rtn_c = null;
		int max = size();
		for (int i = 0; i < max; ++i)
		{
			rtn_c = (ONLComponent)elementAt(i);
			if (rtn_c.getType().equals(tp)) return rtn_c;
		}
		return null;
	}

	public ONLComponent getONLComponent(String lbl)
	{
		ONLComponent rtn_c = null;
		int max = size();
		for (int i = 0; i < max; ++i)
		{
			rtn_c = (ONLComponent)elementAt(i);
			if (rtn_c.getLabel().equals(lbl)) return rtn_c;
		}
		return null;
	}
	public ONLComponent onlComponentAt(int i) { return ((ONLComponent)elementAt(i));}
	//public int getType() { return type;}
	public void removeAll(Collection coll)
	{
		Iterator iter = coll.iterator();
		ONLComponent elem;
		while(iter.hasNext())
		{
			removeComponent((ONLComponent)iter.next());
		}
	}
	public boolean isLinks() { return links;}
	protected void setLinks(boolean b) { links = b;}
}

