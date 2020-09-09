package cnss.simulator;

/**
 * The <code>Packet</code> class models a network packet.
 */
public class Packet {

	public static enum PacketType {
		DATA, CONTROL, TRACING, UNKNOWN
	}

	/**
	 * The size of a Packet with no payload - similar to IP
	 */
	public static int HEADERSIZE = 20;
	/**
	 * The size of a Packet with no payload - similar to IP
	 */
	public static int INITIALTTL = 32;
	/**
	 * A packet with destination ONEHOP is directed to the first node that receives
	 * it
	 */
	public static int ONEHOP = 1000000;

	/**
	 * The unknown address.
	 */
	public static int UNKNOWNADDR = -1;

	protected int src;
	protected int dst;
	protected int ttl;
	protected int seq;
	protected int size; // size of the packet including payload size
	protected byte[] payload;
	protected PacketType type;

	/**
	 * <code>Packet</code> constructor for the super class. This defaults to setting
	 * the packet type to be the UNKNOWN type.
	 * 
	 * @param s  source address
	 * @param d  destination address
	 * @param pl initial payload
	 */
	public Packet(int s, int d, byte[] pl) {
		src = s;
		dst = d;
		type = PacketType.UNKNOWN;
		payload = pl;
		ttl = INITIALTTL;
		seq = 0;
		size = HEADERSIZE + payload.length;
	}

	/**
	 * make an exact copy of this packet
	 * 
	 * @return a copy of the packet
	 */
	public Packet getCopy() {
		byte[] copypl = new byte[this.payload.length];
		System.arraycopy(this.payload, 0, copypl, 0, this.payload.length);
		Packet copy = new Packet(this.src, this.dst, copypl);
		copy.setType(this.type);
		// copy.setPayload(copypl); because it is useless
		copy.setTtl(this.ttl);
		copy.setSequenceNumber(this.seq);
		// copy.setSize(this.getSize()); because it is useless
		return copy;
	}

	/**
	 * Gets the source address
	 * 
	 * @return int source address
	 */
	public int getSource() {
		return src;
	}

	/**
	 * Gets the packet type
	 * 
	 * @return int packet type
	 */
	public PacketType getType() {
		return type;
	}

	/**
	 * Sets the packet type
	 * 
	 * @param t packet type
	 */
	public void setType(PacketType t) {
		type = t;
	}

	/**
	 * Gets the size of the packet.
	 * 
	 * @return int the size of the packets
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Sets the size of the packet.
	 * 
	 * @param s the size of the packet
	 */
	private void setSize(int s) {
		size = s;
	}

	/**
	 * Gets the destination address
	 * 
	 * @return int destination address
	 */
	public int getDestination() {
		return dst;
	}

	/**
	 * Sets the packet sequence number, this is for marking purposes.
	 * 
	 * @param s sequence number
	 */
	public void setSequenceNumber(int s) {
		seq = s;
	}

	/**
	 * Simple to string method.
	 * 
	 * @return String string representation
	 */
	public String toString() {
		String s;
		s = "src " + src + " dst " + dst + " type " + type + " ttl " + ttl + " seq " + seq + " size " + size;
		if (type == PacketType.TRACING) {
			s = s + " path " + payload.toString();
		}
		return s;
	}

	/**
	 * Sets the Payload for the packet.
	 * 
	 * @param d the packets payload
	 */
	public void setPayload(byte[] d) {
		payload = d;
		size = HEADERSIZE + payload.length;
	}

	/**
	 * Gets the Payload of the packet.
	 * 
	 * @return Payload the packets's payload.
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Reduces the ttl by 1.
	 */
	public void decrementTtl() {
		ttl--;
	}

	/**
	 * Gets the current packet ttl
	 * 
	 * @return int current ttl
	 */
	public int getTtl() {
		return ttl;
	}

	/**
	 * Sets the current packet ttl
	 * 
	 * @param t the new ttl
	 */
	private void setTtl(int t) {
		ttl = t;
	}

}
