import java.util.Vector;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;







public class OldSubnetManager {

	public static final int TEST = 2;
	/**
	 * Used to generate subnet addresses in sequence.  Currently does 192.168.0.0/16 and up.
	 */
	/**
	 * List of all subnets assigned to ONL components; actual addresses may not yet be assigned
	 */
	private static Vector<OldSubnetManager.Subnet> subnetList;
	/**
	 * This single instance of the static SubnetManager class is used to have a place to associate storage of variables of the static class 
	 */
	private static final OldSubnetManager instanceStorage = new OldSubnetManager();
	private static final int MIN_INDEX = 1;
	private static final int MAX_INDEX = 255;

	private OldSubnetManager()
	{
	  subnetList = new Vector<OldSubnetManager.Subnet>();
	}
	/**
	 * @return Next subnet address (as Subnet object) for a non-P2P, non-naked host subnet.
	 * Increments the allocation pool to the next subnet address in sequence.
	 */
	public static Subnet getSubnet(int i, int port) 
	{
		Subnet sn = null;

		for (Subnet subnet : subnetList)
		{
			if (subnet.index == i && subnet.port == port) return subnet;
		}
		sn = new Subnet(i, port);
		subnetList.add(sn);
		ExpCoordinator.print(new String("OldSubnetManager.getSubnet returning " + sn.toString()), TEST);
		return sn;
	}
	public static Subnet getSubnetFromSubIndex(int i, int sub_index) 
	{
		int port = (sub_index/16) - 1;
		return (getSubnet(i, port));
	}
	
	protected static void clear()
	{
		Vector<Subnet> tmp_sn = new Vector<Subnet>(subnetList);
		for (Subnet sn : tmp_sn) freeSubnet(sn);
	}

	private static void freeSubnet(Subnet sn) 
	{
	  if (subnetList.contains(sn)) 
	    {
	      subnetList.remove(sn);
	      sn.clearPorts();
	    }
	}

	public static void addLink(ONLComponent.PortBase p1, ONLComponent.PortBase p2) throws SubnetManager.SubnetException
	{
		//check for cycles
		if (ExpCoordinator.theCoordinator.getTopology().isCycle(p1, p2)) 
			throw (new SubnetManager.SubnetException("OldSubnetManager.addLink(" + p1.getLabel() + ", " + p2.getLabel() + ") creates cycle"));

		ExpCoordinator.print(new String("OldSubnetManager.addLink (" + p1.getLabel() + ", " + p2.getLabel() + ")"), TEST);
		//IPADDR change
		//if one component is a router and the other is already in a router subnet throw an exception
		//if both components already belong to a subnet owned by a router throw an exception
		OldSubnetManager.Subnet p1_sn = (OldSubnetManager.Subnet)p1.getSubnet();
		OldSubnetManager.Subnet p2_sn = (OldSubnetManager.Subnet)p2.getSubnet();
		if ((p1.isRouter() && p2_sn != null && p2_sn.isRouter() && p2 != p2_sn.getOwner()) ||
				(p2.isRouter() && p1_sn != null && p1_sn.isRouter() && p1 != p1_sn.getOwner()) ||
				(p1_sn != null && p2_sn != null && p1_sn.isRouter() && p1 != p1_sn.getOwner()) && p2_sn.isRouter() && p2 != p2_sn.getOwner())
		{
			if (p1_sn != null && p1_sn != p2_sn)
				throw (new SubnetManager.SubnetException("OldSubnetManager.addLink(" + p1.getLabel() + ", " + p2.getLabel() + ") link failed due to subnet allocation"));
		}
		if (!p1.isRouter() && p2.isRouter()) p2.getSubnet().addPort(p1);
		if (!p2.isRouter() && p1.isRouter()) p1.getSubnet().addPort(p2);
		if (!p1.isRouter() && p2.isSwitch() && p2.getSubnet() != null && ((OldSubnetManager.Subnet)p2.getSubnet()).isRouter()) p2.getSubnet().addPort(p1);
		else if (!p2.isRouter() && p1.isSwitch() && p1.getSubnet() != null && ((OldSubnetManager.Subnet)p1.getSubnet()).isRouter()) p1.getSubnet().addPort(p2);
	}

	public static void removeLink(ONLComponent.PortBase p1, ONLComponent.PortBase p2)
	{
	  //
	  Subnet sn = (Subnet)p1.getSubnet();
	  //remove ports from subnet if either is not a switch
	  if (!p1.isRouter()) sn.removePort(p1);
	  if (!p2.isRouter()) sn.removePort(p2);
	  if (sn.isEmpty()) 
	    {
	      freeSubnet(sn);
	      return;
	    }
	}
	/**
	 * @author jp
	 *
	 * <p>Subnet abstraction which ONLComponents see (and which sees them).  Each port in the Topology
	 * is mapped to a SubnetComponent, and each SubnetComponent is aware of all components mapped to it.
	 * After this mapping is complete, each SubnetComponent also obtains an IP Address and Netmask
	 * for the subnet to which the ONLComponents should be added.  SubnetManager also handles the
	 * individual address assignment for each ONLComponent. 
	 */
	public static class Subnet extends SubnetManager.Subnet {

		/**
		 * 
		 * Index used as subnet identifier and as 3rd byte of all IPaddresses assigned from this subnet
		 */
		int subIndex;
		int port;
		ONLComponent.PortBase owner = null;

		/**
		 * Construct an empty SubnetComponent.  This state is not an acceptable
		 * final state and must change as ports are assigned.
		 */
		public Subnet(int ndx, int port_ndx) 
		{
			super();
			try{
				MIN_NDX = 1;
				MAX_NDX = 15;
				index = ndx;
				port = port_ndx;
				subIndex = (port_ndx + 1) * 16;
				baseIP = new ONL.IPAddress("192.168." + index + "." + subIndex);
				netmask = new SubnetManager.Netmask(28);
				allocatedIndices = new boolean[16];
				for (int i = 0; i < 16; ++i)
					allocatedIndices[i] = false;
			} catch(java.text.ParseException e){
				ExpCoordinator.print(new String("OldSubnetManager.Subnet ndx:" + ndx + " port_ndx:" + port_ndx + " error:" + e.getMessage()));}

			ExpCoordinator.print(new String("OldSubnetManager.Subnet " + toString()), TEST);
		}
		public SubnetManager.Subnet getRootSubnet()
		{
			return (new SubnetManager.Subnet(index));
		}
		public boolean hasRoot(SubnetManager.Subnet sn) { return (index == sn.getIndex());}
		public void setOwner(ONLComponent.PortBase p) { owner = p;}
		public ONLComponent.PortBase getOwner() { return owner;}
		public boolean isRouter() { return (owner != null && owner.isRouter());}
		public void addPort(ONLComponent.PortBase p) throws SubnetManager.SubnetException
		{
			if (portList.contains(p)) return;
			portList.add(p);
			p.setSubnet(this);
			if (!p.isSwitch())
			{
				ONL.IPAddress ipaddr;
				if (ExpCoordinator.isOldSubnet() && p.isRouter()) ipaddr = getNewIPAddress(14);
				else
					ipaddr = getNewIPAddress();
				p.setIPAddr(ipaddr.toString());
			}
			else //p.isSwitch
			{
				Vector<ONLComponent.PortBase> connected = p.getConnectedPorts();
				if (connected != null && !connected.isEmpty())
				{
					for (ONLComponent.PortBase connectedPort : connected) addPort(connectedPort);
				}
			}
		}
		public void removePort(ONLComponent.PortBase p)
		{
			if (p == owner) return;
			if (portList.contains(p))
			{
				//p.setSubnet(null);ls
				if (!p.isSwitch()) 
				{
					try{
						freeIPAddress(new ONL.IPAddress(p.getIPAddr().toString()));
					} catch (java.text.ParseException e){}
				}
				p.setSubnet(null);
				portList.remove(p);
				if (p.isSwitch())
				{
					Vector<ONLComponent.PortBase> connected = p.getConnectedPorts();
					if (connected != null && !connected.isEmpty())
					{
						for (ONLComponent.PortBase connectedPort : connected) removePort(connectedPort);
					}
				}
			}
		}
		
		protected int getSubIndex() { return subIndex;}

		protected ONL.IPAddress getNewIPAddress(int i) throws  SubnetManager.SubnetException
		{
			int j = i;
			if (allocatedIndices[i]) j = getUnallocatedIndex();
			if (!allocatedIndices[j])
			{
				try {
					ONL.IPAddress rtn = new ONL.IPAddress(new String("192.168." + index + "." + (subIndex+j)));
					allocatedIndices[j] = true;
					return rtn;
				} catch(java.text.ParseException e){
					throw new SubnetManager.SubnetException("OldSubnetManager.Subnet(" + baseIP.toString() + ").getNewIPAddress -- " + e.getMessage());		
				}

			}
			throw new  SubnetManager.SubnetException("OldSubnetManager.Subnet(" + baseIP.toString() + ").getNewIPAddress out of addresses");
		}
		protected void freeIPAddress(ONL.IPAddress ip)
		{
		  byte[] ip_bytes = ip.getBytes();
		  if ((int)ip_bytes[2] == index)
		    {
		      int i = ((int)ip_bytes[3])-subIndex;
		      if (i >= MIN_NDX && i <= MAX_NDX) allocatedIndices[i] = false;
		    }
		}
		protected int getSubnetPort() { return port;}
		public String toString()
		{
			return (new String("OldSubnetManager.Subnet baseIP:" + baseIP + " getBaseIP:" + getBaseIP()));
		}
	}


}
