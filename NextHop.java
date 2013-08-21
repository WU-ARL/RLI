import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.lang.*;//String;
///////////////////////////////////////////////// NextHop ////////////////////////////////////////////////////////////////



public class NextHop
{
	public static final int TEST_IPADDRESS = 5;
	public static final int TEST_NHLISTENER = Topology.TEST_DEFRTS;
	protected int port = 0;
	protected ONL.IPAddress nhip = null;
	private Hardware hardware = null;
	private NHListener nhlistener = null;
	
	private class NHListener implements PropertyChangeListener
	{
		private NextHop nexthop = null;
		public NHListener(NextHop nh){ nexthop = nh;}
		//PropertyChangeListener
		public void propertyChange(PropertyChangeEvent e)
		{
			ExpCoordinator.print(new String("NextHop(" + nexthop.toString() + ").NHListener.propertyChange hardware:" + hardware.getLabel()), TEST_NHLISTENER);
			if (e.getPropertyName().equals(ONLComponent.PortBase.NEXTHOP))
			{
				getNextHopIP();
				ExpCoordinator.print(new String("NextHop(" + nexthop.toString() + ").NHListener.propertyChange getNextHopIP"), TEST_NHLISTENER);
			}
		}
		//end PropertyChangeListener
	}
	public NextHop() {}
	public NextHop(String s) throws java.text.ParseException
	{
		parseString(s);
	}
	public NextHop(NextHop nh)
	{
		this();//nh.port, nh.hardware);
		if (nh != null)
		{
			port = nh.port;
			hardware = nh.hardware;
			if (nh.nhip != null) nhip = new ONL.IPAddress(nh.nhip);
			ExpCoordinator.print(new String("NextHop.NextHop(" + nh.toString() + ") port:" + port + " nhip:" + nhip), TEST_IPADDRESS);
		}
	}
	public NextHop(int p) { port = p;}
	public NextHop(int p, ONL.IPAddress ip, Hardware hwi)
	{ 
		this(p,ip);
		hardware = hwi;
	}
	public NextHop(int p, ONL.IPAddress ip)
	{
		this(p);
		nhip = new ONL.IPAddress(ip);
		ExpCoordinator.print(new String("NextHop.NextHop(" + p + ", " + ip.toString() + ") port:" + port + " nhip:" + nhip), TEST_IPADDRESS);
		
	}
	public NextHop(int p, Hardware hwi)
	{
		port = p;
		hardware = hwi;
	}
	public NextHop(Hardware.Port hwip)
	{
		port = hwip.getID();
		hardware = (Hardware)hwip.getParent();
	}
	public void parseString(String s) throws java.text.ParseException
	{
		String[] strarray = s.split(":");
		int max = 7;
		if (hardware != null) max = hardware.getNumPorts() - 1;
		try 
		{
			int tmp_int = Integer.parseInt(strarray[0]);    
			if (tmp_int < 0 || tmp_int > max) throw new java.text.ParseException(new String("NextHop not integer in 0-" + max + " range"), 0);
			else port = tmp_int;
			if (strarray.length > 1)
				nhip = new ONL.IPAddress(strarray[1]);
		}
		catch (NumberFormatException e)
		{
			throw new java.text.ParseException("NextHop not integer in range", 0);
		}
	}
	public int getPort() { return port;}
	public void setPort(int p) 
	  { 
		  if (port != p)
		  {
			  if (nhlistener != null) removeListener();
			  port = p;
			  if (nhlistener != null && hardware != null) hardware.getPort(port).addPropertyListener(ONLComponent.PortBase.NEXTHOP, nhlistener);
		  }
      }
	public boolean isEqual(NextHop nh)
	{
		return (nh.port == port && nh.getNextHopIP().toString().equals(getNextHopIP().toString()));// && (nh.hardware == hardware));
	}
	public String toString()
	{
		if (nhip == null) getNextHopIP();
		return (new String(port + ":" + nhip.toString()));
		//CHANGED 9/9/10 //return (String.valueOf(port));
	}
	public void set(NextHop nh)
	{
		//port = 
	    setPort(nh.port);
		if (nh.nhip != null) nhip = new ONL.IPAddress(nh.nhip);
		if (hardware == null) hardware = nh.hardware;
	}
	public void setHardware(Hardware hwi) 
	{
		if (hardware == null) 
		{
			hardware = hwi;
			if (nhip == null || nhip.isZero()) getNextHopIP();
		}
	}
	public void setNextHopIP(ONL.IPAddress ip) { nhip = ip;}
	public ONL.IPAddress getNextHopIP() 
	{
		if (hardware == null) 
		{
			ExpCoordinator.print(new String("NextHop(" + port + ").getNextHopIP hardware null"), TEST_IPADDRESS);
			if (nhip == null) nhip = new ONL.IPAddress();
			return nhip; //return null;
		}
		Hardware.Port p = hardware.getPort(port);
		if (nhip == null || nhip.isZero() || ExpCoordinator.isOldSubnet())
		{ 	
			ONLComponent c = p.getLinkedTo();
		
			if (c != null && c.isRouter()) nhip = new ONL.IPAddress(c.getIPAddr());
			else if (nhip == null || c == null) nhip = new ONL.IPAddress();
		}
		ExpCoordinator.print(new String("NextHop.getNextHopIP hardware:" + hardware.getLabel() + " port:" + port + " nhip:" + nhip.toString()), TEST_IPADDRESS);

		return nhip;
		/*
		try
		{
			ONL.IPAddress tmp_addr = new ONL.IPAddress(p.getNHIPAddr());
			ExpCoordinator.print(new String("NextHop(" + port + ").getNextHopIP hardware:" + hardware.getLabel() + " ip:" + tmp_addr.getInt()), TEST_IPADDRESS);
			return (tmp_addr.getInt());
		}
		catch (java.text.ParseException e) 
		{
			ExpCoordinator.print(new String("NextHop(" + port + ").getNextHopIP error:" + e.getMessage()));
			return 0;
		}
		*/
	}
	/*
	public String getNHIPString() 
	{
		if (hardware == null) 
		{
			ExpCoordinator.print(new String("NextHop(" + port + ").getNextHopIP hardware null"), TEST_IPADDRESS);
			return "";
		}
		Hardware.Port p = hardware.getPort(port);
		try
		{
			ONL.IPAddress tmp_addr = new ONL.IPAddress(p.getNHIPAddr());
			ExpCoordinator.print(new String("NextHop(" + port + ").getNextHopIP hardware:" + hardware.getLabel() + " ip:" + tmp_addr.getInt()), TEST_IPADDRESS);
			return (tmp_addr.toString());
		}
		catch (java.text.ParseException e) 
		{
			ExpCoordinator.print(new String("NextHop(" + port + ").getNextHopIP error:" + e.getMessage()));
			return "";
		}
	}
	*/
	public boolean equals(Object o)
	{
		if (o == this) return true;
		if (o instanceof NextHop && isEqual((NextHop)o)) return true;
		else return false;
	}
	public String toCommandString()
	{
		return (new String(toString() + ":" + nhip.getInt()));
	}
	public void addListener()
	{
		ExpCoordinator.print(new String("NextHop(" + toString() + ").addListener"), TEST_NHLISTENER);
		if (nhlistener == null)
		{
			ExpCoordinator.print(new String("NextHop(" + toString() + ").addListener creating new listener"), TEST_NHLISTENER);
			nhlistener = new NHListener(this);
			if (hardware != null) hardware.getPort(port).addPropertyListener(ONLComponent.PortBase.NEXTHOP, nhlistener);
		}
	}
	public void removeListener()
	{
		if (nhlistener != null && hardware != null) hardware.getPort(port).removePropertyListener(nhlistener);
	}
}
