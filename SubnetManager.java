import java.util.Vector;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

public class SubnetManager implements ListDataListener {
	/**
	 * @author mlw2
	 *
	 * <p>InvalidInput is the exception thrown on all attempts to misuse the SubnetManager internal interfaces.
	 * SubnetManager's public APIs are error-checked and do not throw this exception.  InvalidInput
	 * is a purely internal exception of SubnetManager.
	 */
	public static class InvalidInput extends Exception {
		/**
		 * random serial version for Exception's inherited Serializeable interface
		 */
		private static final long serialVersionUID = 7814979896077483994L;

		public InvalidInput(String s) {
			super(s);
		}
	}

	public static class SubnetException extends Exception {
		/**
		 * random serial version for Exception's inherited Serializeable interface
		 */
		private static final long serialVersionUID = -3328944345547607364L;

		public SubnetException(String s) {
			super(s);
		}
	}
	public static final int TEST = 2;
	/**
	 * Used to generate P2P link subnet addresses in sequence.  Currently does 192.168.254.252/30 and down.
	 */
	private static boolean allocatedSubnets[];
	/**
	 * Used to generate subnet addresses in sequence.  Currently does 192.168.0.0/16 and up.
	 */
	/**
	 * List of all subnets assigned to ONL components; actual addresses may not yet be assigned
	 */
	private static Vector<SubnetManager.Subnet> subnetList;
	/**
	 * This single instance of the static SubnetManager class is used to have a place to associate storage of variables of the static class 
	 */
	private static final SubnetManager instanceStorage = new SubnetManager();
	private static final int MIN_INDEX = 1;
	private static final int MAX_INDEX = 255;

	private SubnetManager()
	{
		allocatedSubnets = new boolean[256];
		subnetList = new Vector<SubnetManager.Subnet>();
		allocatedSubnets[0] = true;
		for (int i = 1; i < 256; ++i)
			allocatedSubnets[i] = false;
	}
	/**
	 * @return Next subnet address (as Subnet object) for a non-P2P, non-naked host subnet.
	 * Increments the allocation pool to the next subnet address in sequence.
	 */
	private Subnet getNewSubnet() throws SubnetException
	{
		for (int i = MIN_INDEX; i <= MAX_INDEX; ++i)
		{
			if (!allocatedSubnets[i])
			{
				Subnet sn = new Subnet(i);
				subnetList.add(sn);
				allocatedSubnets[i] = true;
				return sn;
			}
		}
		throw (new SubnetException("SubnetManager.getNewSubnet ran out of subnets."));
	}
	public static Subnet getSubnet(int i) throws SubnetException
	{
		Subnet sn = null;
		if (!allocatedSubnets[i])
		{
			sn = new Subnet(i);
			subnetList.add(sn);
			allocatedSubnets[i] = true;
		}
		else
		{
			for (Subnet subnet : subnetList)
			{
				if (subnet.index == i) return subnet;
			}
			throw (new SubnetException("SubnetManager.getSubnet(" + i +") subnet marked allocated but not in list"));
		}
		return sn;
	}

	protected static void clear()
	{
		Vector<Subnet> tmp_sn = new Vector<Subnet>(subnetList);
		for (Subnet sn : tmp_sn) freeSubnet(sn);
	}

	private static void freeSubnet(Subnet sn) 
	{
		allocatedSubnets[sn.index] = false;
		ExpCoordinator.print(new String("SubnetManager.freeSubnet " + sn.index), TEST);
		if (subnetList.contains(sn)) 
		{
			subnetList.remove(sn);
			sn.clearPorts();
		}
	}

	public static void addLink(ONLComponent.PortBase p1, ONLComponent.PortBase p2) throws SubnetException
	{
		String sn1_str = "null";
		String sn2_str= "null";
		//assign subnets to any switches that don't already have one
		if (p1.isSwitch() && p1.getOriginalSubnet() == null)
		{
			Subnet sn = instanceStorage.getNewSubnet();
			sn.addPort(p1);
		}
		if (p2.isSwitch() && p2.getOriginalSubnet() == null)
		{
			Subnet sn = instanceStorage.getNewSubnet();
			sn.addPort(p2);
		}
		if (p1.getSubnet() != null) sn1_str = String.valueOf(p1.getSubnet().getIndex());
		if (p2.getSubnet() != null) sn2_str = String.valueOf(p2.getSubnet().getIndex());
		ExpCoordinator.print(new String("SubnetManager.addLink p1:" + p1.getLabel() + " sn1:" + sn1_str + "p2:" + p2.getLabel() + " sn2:" + sn2_str), TEST);
		if (p1.getSubnet() == null) //p1 not in a subnet already
		{
			if (p2.getSubnet() != null) //p2 already in a subnet
			{
				//add p1 to p2's subnet
				p2.getSubnet().addPort(p1);
			}
			else
			{ //p2 isn't in a subnet either
				Subnet sn = instanceStorage.getNewSubnet();
				sn.addPort(p1);	
				sn.addPort(p2);
			}
		}
		else //p1 is already in a subnet
		{
			if (p2.getSubnet() == null) //p2 not in a subnet
			{
				//add p2 to p1's subnet
				p1.getSubnet().addPort(p2);
			}
			else //they are both in subnets already so we need to merge them
			{
				if (p1.getSubnet() != p2.getSubnet()) //merge the subnet with the higher index into the subnet with the lower index
				{
					//may want to alert user of the change
					if (p1.getSubnet().getIndex() < p2.getSubnet().getIndex())
					{
						Subnet sn2 = p2.getSubnet();
						p1.getSubnet().merge(sn2);
						if (sn2.isEmpty()) freeSubnet(sn2);
					}
					else
					{
						Subnet sn1 = p1.getSubnet();
						p2.getSubnet().merge(sn1);
						if (sn1.isEmpty()) freeSubnet(sn1);
					}
				}//else already in the same subnet do nothing
			}
		}
	}

	public static void removeLink(ONLComponent.PortBase p1, ONLComponent.PortBase p2)
	{
		//
		Subnet sn = p1.getSubnet();
		if (sn == null) ExpCoordinator.print(new String("SubnetManager.removeLink(" + p1.getLabel() + ", " + p2.getLabel() + ") sn null"));
		else ExpCoordinator.print(new String("SubnetManager.removeLink(" + p1.getLabel() + ", " + p2.getLabel() + ") sn:" + sn.getIndex()));
		//remove ports from subnet if either is not a switch
		if (!p1.isSwitch() || (p1.getParent() != sn.onlSwitch)) 
			sn.removePort(p1);
		if (!p2.isSwitch()|| (p2.getParent() != sn.onlSwitch)) 
			sn.removePort(p2);
		if (sn.isEmpty()) 
		{
			freeSubnet(sn);
			return;
		}
		//otw split into 2 subnets or if just a subnet of the 2 ports remove both from subnet if port not a switch
		//TODO: need to see if the owner of the subnet is in the reachable group. leave that group and change the other
		if (p1.isSwitch() && p2.isSwitch() && (p1.getParent() != p2.getParent()))//if both are on switch and it's not a loopback
		{
			ONLComponent.PortBase tmp_ps = p1;
			ONLComponent.PortBase tmp_pe = p2;
			if (sn.ownerReachableFrom(p1)) 
			{
				tmp_ps = p2;
				tmp_pe = p1;
			}
			Vector<ONLComponent.PortBase> reachable = new Vector<ONLComponent.PortBase>();
			reachable.add(tmp_ps);
			getReachable(reachable, tmp_ps);
			if (!reachable.contains(tmp_pe))
			{
				sn = tmp_ps.getOriginalSubnet();
				for (ONLComponent.PortBase port : reachable)
				{
					try{
					sn.addPort(port);
					}
					catch(SubnetException e)
					{
						ExpCoordinator.print("SubnetManager.removeLink error:" + e.getMessage());
					}
					//port.setSubnet(sn);
				}
			}
		}
	}

	private static void getReachable(Vector<ONLComponent.PortBase> ports, ONLComponent.PortBase root)
	{
		ONLComponent.PortBase connectedTo = (ONLComponent.PortBase)root.getLinkedTo();
		if (connectedTo != null && !ports.contains(connectedTo))
		{
			ports.add(connectedTo);
			getReachable(ports, connectedTo);
		}
		if (root.isSwitch())
		{
			if (root.getParent() instanceof GigEDescriptor)
			{
				GigEDescriptor g = (GigEDescriptor)root.getParent();
				int max = g.getNumPorts(); 
				ONLComponent.PortBase tmp_p;
				for (int i = 0; i < max; ++i)
				{
					tmp_p = g.getPort(i);
					if (!ports.contains(tmp_p))
					{
						ports.add(tmp_p);
						connectedTo =  (ONLComponent.PortBase)tmp_p.getLinkedTo();
						if (connectedTo != null && !ports.contains(connectedTo))
						{
							ports.add(connectedTo);
							getReachable(ports, connectedTo);
						}
					}
				}
			}
			if (root.getParent() instanceof Hardware)
			{
				Hardware hw = (Hardware)root.getParent();
				int max = hw.getNumPorts(); 
				ONLComponent.PortBase tmp_p;
				for (int i = 0; i < max; ++i)
				{
					tmp_p = hw.getPort(i);
					if (!ports.contains(tmp_p))
					{
						ports.add(tmp_p);
						connectedTo =  (ONLComponent.PortBase)tmp_p.getLinkedTo();
						if (connectedTo != null && !ports.contains(connectedTo))
						{
							ports.add(connectedTo);
							getReachable(ports, connectedTo);
						}
					}
				}
			}
		}
	}

	/**
	 * @author mlw2
	 *
	 * <p>Subnet abstraction which ONLComponents see (and which sees them).  Each port in the Topology
	 * is mapped to a SubnetComponent, and each SubnetComponent is aware of all components mapped to it.
	 * After this mapping is complete, each SubnetComponent also obtains an IP Address and Netmask
	 * for the subnet to which the ONLComponents should be added.  SubnetManager also handles the
	 * individual address assignment for each ONLComponent. 
	 */
	public static class Subnet {

		int MIN_NDX = 1;
		int MAX_NDX = 255;
		/**
		 * Index used as subnet identifier and as 3rd byte of all IPaddresses assigned from this subnet
		 */
		int index;
		/**
		 * Base IPaddress: 192.168.<index>.0
		 */
		ONL.IPAddress baseIP = null;
		/**
		 * Top of the IP Address range for the SubnetManager.  Used for assigning routers.
		 */
		boolean allocatedIndices[] = null;
		/**
		 * Number of ports assigned to this SubnetComponent.  Used to select subnet type (P2P, Host, gen'l subnet)
		 */
		Netmask netmask = null;
		/**
		 * List of all ports (ONLComponents) assigned to this subnet.
		 */
		Vector<ONLComponent.PortBase> portList;
		ONLComponent onlSwitch = null;

		/**
		 * Construct an empty SubnetComponent.  This state is not an acceptable
		 * final state and must change as ports are assigned.
		 */
		public Subnet() 
		{
			portList = new Vector<ONLComponent.PortBase>();
		}
		public Subnet(int ndx) 
		{
			try{
				index = ndx;
				baseIP = new ONL.IPAddress("192.168." + index + ".0");
				portList = new Vector<ONLComponent.PortBase>();
				netmask = new Netmask(24);
				allocatedIndices = new boolean[256];
				allocatedIndices[0] = true;
				for (int i = 1; i < 256; ++i)
					allocatedIndices[i] = false;
			} catch(java.text.ParseException e){}
		}
		public boolean isEqual(SubnetManager.Subnet sn)
		{
			return (index == sn.index && baseIP.equals(sn.baseIP) && netmask.isEqual(sn.netmask));
		}
		public ONL.IPAddress getBase() { return baseIP;}
		public Netmask getNetmask() { return netmask;}
		public int getIndex() { return index;}
		public boolean isEmpty() { return (portList.isEmpty() && onlSwitch == null);}
		public void addPort(ONLComponent.PortBase p, int i) throws SubnetException
		{
			if (portList.contains(p)) return;
			Subnet os = p.getSubnet();
			if (os != null) os.removePort(p);
			p.setSubnet(this);
			portList.add(p);
			ExpCoordinator.print(new String("SubnetManager.Subnet(" + index + ").addPort " + p.getLabel() + " with index " + i), TEST);

			if (!p.isSwitch())
			{
				ONL.IPAddress ipaddr = getNewIPAddress(i);
				p.setIPAddr(ipaddr.toString());
			}
		}
		public void addPort(ONLComponent.PortBase p) throws SubnetException
		{
			if (portList.contains(p)) return;
			Subnet os = p.getSubnet();
			if (os != null) os.removePort(p);
			p.setSubnet(this);
			ExpCoordinator.print(new String("SubnetManager.Subnet(" + index + ").addPort " + p.getLabel()), TEST);
			portList.add(p);
			if (!p.isSwitch())
			{
				ONL.IPAddress ipaddr = getNewIPAddress();
				p.setIPAddr(ipaddr.toString());
			}
		}
		public void setSwitch(ONLComponent sw) { onlSwitch = sw;}
		public void removePort(ONLComponent.PortBase p)
		{
			if (portList.contains(p))
			{
				p.setSubnet(null);

				ExpCoordinator.print(new String("SubnetManager.Subnet(" + index + ").removePort " + p.getLabel()), TEST);

				if (p.isSwitch() && p.getOriginalSubnet() == this) return;
				if (!p.isSwitch()) 
				{
					try{
						freeIPAddress(new ONL.IPAddress(p.getIPAddr().toString()));
					} catch (java.text.ParseException e){}
				}
				portList.remove(p);
			}
		}
		public void clearPort(ONLComponent.PortBase p)
		{
			if (portList.contains(p))
			{
				//p.setSubnet(null);
				portList.remove(p);
			}
		}
		public boolean hasEndpoint()
		{
			for (ONLComponent.PortBase elem : portList)
			{
				if (elem.isEndpoint()) return true;
			}
			return false;
		}
		public Vector<ONLComponent.PortBase> getRouters()
		{
			Vector<ONLComponent.PortBase> rtn = new Vector<ONLComponent.PortBase>();
			for (ONLComponent.PortBase elem : portList)
			{
				if (elem.isRouter()) rtn.add(elem);
			}
			return rtn;
		}
		public void merge(Subnet sn) throws SubnetException
		{
			Vector<ONLComponent.PortBase> sn_ports = new Vector<ONLComponent.PortBase>(sn.portList);
			for (ONLComponent.PortBase p : sn_ports)
			{
				sn.removePort(p);
				addPort(p);
			}
		}
		protected void clearPorts()
		{
			for (ONLComponent.PortBase p : portList) p.setSubnet(null);
			portList.clear();
			onlSwitch = null;
		}

		protected ONL.IPAddress getNewIPAddress() throws SubnetException
		{ 
			return (getNewIPAddress(getUnallocatedIndex()));
		}
		protected int getUnallocatedIndex()	throws SubnetException
		{
			for (int k = MIN_NDX; k <= MAX_NDX; ++k)
				if (!allocatedIndices[k])  return k;
			throw new SubnetException("SubnetManager.Subnet(" + baseIP.toString() + ").getUnallocatedIndex out of addresses");
		}

		protected ONL.IPAddress getNewIPAddress(int i) throws SubnetException
		{
			int j = i;
			if (allocatedIndices[i]) j = getUnallocatedIndex();
			if (!allocatedIndices[j])
			{
				try {
					ONL.IPAddress rtn = new ONL.IPAddress(new String("192.168." + index + "." + j));
					allocatedIndices[j] = true;
					return rtn;
				} catch(java.text.ParseException e){
					throw new SubnetException("SubnetManager.Subnet(" + baseIP.toString() + ").getNewIPAddress -- " + e.getMessage());		
				}

			}
			throw new SubnetException("SubnetManager.Subnet(" + baseIP.toString() + ").getNewIPAddress out of addresses");
		}
		protected void freeIPAddress(ONL.IPAddress ip)
		{
			byte[] ip_bytes = ip.getBytes();
			if ((int)ip_bytes[2] == index)
			{
				int i = (int)ip_bytes[3];
				if (i >= MIN_NDX && i <= MAX_NDX) allocatedIndices[i] = false;
			}
		}
		public ONL.IPAddress getBaseIP() { return baseIP;}
		public boolean ownerReachableFrom(ONLComponent.PortBase p)
		{
			if (onlSwitch == null) return false;
			Vector<ONLComponent.PortBase> reachable = new Vector<ONLComponent.PortBase>();
			reachable.add(p);
			getReachable(reachable, p);
			for (ONLComponent.PortBase r : reachable)
			{
				if (r.getParent() == onlSwitch) return true;
			}
			return false;
		}
	}

	public static class Netmask
	{
		protected int bitlen = 24;
		protected String maskString = "255.255.255.0";
		public Netmask() {}
		public Netmask(String s) throws java.text.ParseException
		{
			parseString(s);
		}
		public Netmask(Netmask nm)
		{
			bitlen = nm.bitlen;
			maskString = new String(nm.maskString);
		}
		public Netmask(int bl)throws java.text.ParseException
		{
			bitlen = bl;
			try{
				int[] octet = mask2octet(bitlen);
				maskString = octet2str(octet);		  
			}
			catch(InvalidInput e)
			{
				throw new java.text.ParseException(e.getMessage(), 0);
			}
		}
		public boolean isEqual(Netmask nm) { return (maskString.equals(nm.maskString));}
		public int getBitlen() { return bitlen;}
		public String toString() { return maskString;}
		protected void parseString(String nm) throws java.text.ParseException
		{
			try{
				String ssplit[] = nm.split("\\.");
				int[] octet;
				if (ssplit.length == 1)
				{
					bitlen = Integer.parseInt(nm);
					octet = mask2octet(bitlen);
					maskString = octet2str(octet);
					return;
				}
				octet = str2octet(nm);
				bitlen = octet2bitlen(octet);
			}
			catch (InvalidInput e)
			{
				throw new java.text.ParseException(e.getMessage(), 0);
			}
			catch (NumberFormatException e)
			{ 
				String msg = new String("SubnetManager.Netmask.parseString error: " + e.getMessage());
				ExpCoordinator.print(msg, TEST);
				throw new java.text.ParseException(msg, 0);
			}
		}
		/**
		 * @param s String in IPv4 dotted decimal format
		 * @return 4-int array of octets
		 * @throws InvalidInput if input string is not in IPv4 dotted decimal form
		 */
		private int[] str2octet(String s) throws InvalidInput {
			int octet[] = new int[4];
			String ssplit[] = s.split("\\.");
			if (ssplit.length != 4) {
				throw new SubnetManager.InvalidInput("Subnet: could not convert \"" + s + "\" to dotted octet format length:" + ssplit.length);
			}
			for (int i = 0; i < 4; i++) {
				octet[i] = Integer.parseInt(ssplit[i]);
				if (octet[i] < 0 || octet[i] > 255)
					throw new SubnetManager.InvalidInput("Subnet: could not convert \"" + s + "\" to dotted octet format");
			}
			return octet;
		}
		/**
		 * @param octet 4-int array of IPv4 octets, 0..255
		 * @return IPv4 dotted decimal string
		 * @throws InvalidInput if input array is not in proper form (4 ints, 0..255)
		 */
		private String octet2str(int octet[]) throws InvalidInput {
			String s;
			// Validate input:
			if (octet.length != 4)
				throw new SubnetManager.InvalidInput("Subnet: will not convert invalid octet array \"" + octet + "\" to string format");
			for (int i = 0; i < 4; i++) {
				if (octet[i] < 0 || octet[i] > 255)
					throw new SubnetManager.InvalidInput("Subnet: will not convert out-of-range octet \"" + octet[i] + "\" to string format");
			}
			s = "" + octet[0] + "." + octet[1] + "." + octet[2] + "." + octet[3];
			return s;
		}
		/**
		 * @param mask number of left-packed bits in the netmask, 0..32
		 * @return IPv4 netmask in 4-int array
		 * @throws InvalidInput if mask parameter is not 0..32 (invalid length)
		 */
		private int[] mask2octet(int m) throws InvalidInput {
			int mask = m;
			int maskOctet[] = new int[4];
			// Validate input:
			if (mask < 0 || mask > 32)
				throw new SubnetManager.InvalidInput("Subnet: could not convert invalid mask length \"" + mask + "\" to octet format");
			int i = 0;
			while (mask >= 8) {
				maskOctet[i] = 255;
				mask -= 8;
				i++;
			}
			while (i < 4) {
				switch (mask) {
				case 0: maskOctet[i] = 0x00; break;
				case 1: maskOctet[i] = 0x80; break;
				case 2: maskOctet[i] = 0xC0; break;
				case 3: maskOctet[i] = 0xE0; break;
				case 4: maskOctet[i] = 0xF0; break;
				case 5: maskOctet[i] = 0xF8; break;
				case 6: maskOctet[i] = 0xFC; break;
				case 7: maskOctet[i] = 0xFE; break;
				}
				mask -= mask;
				i++;
			}
			// All 4 octets filled
			ExpCoordinator.print(new String("SubnetManager.Netmask.mask2octet mask:" + m + " octet(" + maskOctet[0] + "," + maskOctet[1] + "," + maskOctet[2] + "," + maskOctet[3] + ")"), TEST);
			return maskOctet;
		}
		/**
		 * @param maskOctet IP Network mask in 4-in octet format
		 * @return Bit-length version of netmask
		 * @throws InvalidInput if netmask was not in valid format
		 *
		 * <p> Takes a 4-int octet netmask (E.g., [ 255, 255, 224, 0 ]) and returns the
		 * corresponding bitlength form of the mask (E.g., 19)
		 */
		private int octet2bitlen(int[] maskOctet) throws InvalidInput {
			int maskAcc = 0;
			if (maskOctet.length != 4)
				throw new SubnetManager.InvalidInput("Subnet: could not convert invalid mask octet array \"" + maskOctet + "\" to bitlen format");
			boolean done = false;
			for (int i = 0; i < 4; i++) {
				switch (maskOctet[i]) {
				case 0xFF:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 8;
					break;
				case 0xFE:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 7;
					break;
				case 0xFC:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 6;
					break;
				case 0xF8:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 5;
					break;
				case 0xF0:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 4;
					break;
				case 0xE0:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 3;
					break;
				case 0xC0:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 2;
					break;
				case 0x80:
					if (done)
						throw new SubnetManager.InvalidInput("Subnet: Netmask octet array \"" + maskOctet + "\" not left-packed");
					else maskAcc += 1;
					break;
				case 0x00:
					maskAcc += 0;
					done = true;
					break;
				default:
					throw new SubnetManager.InvalidInput("Subnet: Netmask octet value \"" + maskOctet[i] + "\" is not valid");
				}
			}
			return maskAcc;
		}
	}
	//interface ListDataListener
	public void contentsChanged(ListDataEvent e) {}

	public void intervalAdded(ListDataEvent e) 
	{
		//setIPAddress(ExpCoordinator.theCoordinator.getTopology());
	}

	public void intervalRemoved(ListDataEvent e) 
	{
		//setIPAddress(ExpCoordinator.theCoordinator.getTopology());
	}

	public static void listen(Topology topo) { topo.addTopologyListener(instanceStorage);}
}

