package cnss.simulator;

import java.util.LinkedList;
import java.util.Queue;

import cnss.simulator.Event.EventType;
import cnss.simulator.Packet;
import cnss.simulator.DataPacket;
import cnss.simulator.Packet.PacketType;


/**
 * The <code>Node</code> class represents a node in the network. Each node
 * references its own control algorithm class, whose name was provided in the
 * constructor. A node looks up the class from its name and instantiates it
 * using java reflection. The same applies for the application code the node
 * executes.
 * 
 * @author System's team of the Department of Informatics of FCT/UNL based on a
 * @author preliminary version by Adam Greenhalgh of UCL
 * @version 1.0, September 2021
 */
public class Node {

	static final int LOCAL = -1; // the number of the virtual loop back interface
	static final int UNKNOWN = -2; // an unknown interface - means do not know how

	// The node specific state
	private int node_id;
	private int now; // the current virtual now
	private int num_interfaces;
	private Link[] links;
	private String control_class_name;
	private String application_class_name;
	private ControlAlgorithm control_alg;
	private ApplicationAlgorithm app_alg;
	private String[] args;

	// Events and global variables
	private Queue<Event> inputEvents = new LinkedList<>();
	private Queue<Event> outputEvents = new LinkedList<>();
	private GlobalParameters parameters;
	private int app_clock_tick_period = 0;
	private int control_clock_tick_period = 0;

	// If a timeout event is received, it is only delivered if its
	// timestamp is equal to this value. Thus, a new timeout cancels
	// an old one. Any received packet also cancels a timeout by
	// zeroing this variable
	private int next_app_timeout = 0;
	private int next_control_timeout = 0;

	// Packet counters
	private int[] counter = new int[4];
	private int SENT = 0;
	private int RECV = 1;
	private int DROP = 2;
	private int FORW = 3;

	private int packet_counter = 0; // allows the generation of sequence numbers

	/**
	 * <code>Node</code> constructor takes the node id, the number of interfaces,
	 * the class name of the control algorithm to load as well as the class name of
	 * the application algorithm. If the control algorithm class is not found,
	 * prints an exception. If the application algorithm class is not found, prints
	 * an exception.
	 * 
	 * @param i  node id
	 * @param n  number of interfaces
	 * @param c  class name of the control algorithm to load.
	 * @param a  class name of the application algorithm to load.
	 * @param gp a reference to the global parameters collection
	 */
	public Node(int i, int n, String c, String a, String[] ags, GlobalParameters gp) {
		node_id = i;
		num_interfaces = n;
		links = new Link[n];
		now = 0;
		control_class_name = c;
		application_class_name = a;
		args = ags;
		parameters = gp;

		try {
			control_alg = (ControlAlgorithm) (Class.forName(control_class_name)).getDeclaredConstructor().newInstance();
			app_alg = (ApplicationAlgorithm) (Class.forName(application_class_name)).getDeclaredConstructor().newInstance();
		} catch (Exception exp) {
			exp.printStackTrace();
		}

		counter[SENT] = 0;
		counter[RECV] = 0;
		counter[DROP] = 0;
		counter[FORW] = 0;
		System.out.println("Created " + this);
	}

	/**
	 * Add a link to the <code>Node</code> so the node knows about its attached
	 * links. A <code>Node</code> has 0 to num_interfaces links each connected to
	 * the <code>Link</code> links[i]. Additionally, there is a virtual interface
	 * numbered LOCAL, the local interface. It is only used as the sending interface
	 * when the node sends a local packet.
	 * 
	 * @param l the link to attach.
	 */
	public void addLinks(Link l) {
		int i1 = l.getInterface(1);
		int n1 = l.getNode(1);
		int i2 = l.getInterface(2);
		int n2 = l.getNode(2);
		System.out.println("Added link to node " + node_id + " - " + l);
		if (n1 == node_id) {
			links[i1] = l;
		} else if (n2 == node_id) {
			links[i2] = l;
		}
	}

	/**
	 * This <code>Node</code> starts by initializing the control and application
	 * objects
	 */
	public void initialize() {
		now = 0; // it is redundant, but ....
		control_clock_tick_period = control_alg.initialise(now, node_id, this, parameters, links, num_interfaces);
		app_clock_tick_period = app_alg.initialise(now, node_id, this, args);
		if (control_clock_tick_period < 0)
			control_clock_tick_period = 0;
		if (app_clock_tick_period < 0)
			control_clock_tick_period = 0;
		if (control_clock_tick_period > 0)
			outputEvents.add(new Event(EventType.CONTROL_CLOCK_TICK, control_clock_tick_period, 0, null, null, node_id, 0));
		if (app_clock_tick_period > 0)
			outputEvents.add(new Event(EventType.APP_CLOCK_TICK, app_clock_tick_period, 0, null, null, node_id, 0));
	}

	/**
	 * Add an event to the <code>Node</code> input queue so the node will treat it
	 * in the next execution step.
	 * 
	 * @param e the event to add.
	 */
	public void addInputEvent(Event e) {
		inputEvents.add(e);
	}

	/**
	 * Return an output <code>Event</code> generated by the execution step to be
	 * treated by the main loop of the simulator
	 * 
	 * @return Event, the event
	 */
	public Event getOutputEvent() {
		return outputEvents.poll();
	}

	/**
	 * Generic toString method
	 * 
	 * @return String
	 */
	public String toString() {
		return "Node " + node_id + ": " + num_interfaces + " interf.s, ctr code: " + control_class_name + " app code: " + application_class_name;

	}

	/**
	 * Dump the control (routing | forward) table to stdout
	 * 
	 * @param now the current virtual time
	 */
	public void dumpRoutingTable(int now) {
		control_alg.showRoutingTable(now);
	}

	/**
	 * Dump control state to stdout
	 * 
	 * @param now the current virtual time
	 */
	public void dumpControlState(int now) {
		control_alg.showControlState(now);
	}

	/**
	 * Dump application state to stdout
	 * 
	 * @param now the current virtual time
	 */
	public void dumpAppState(int now) {
		app_alg.showState(now);
	}

	/**
	 * Dump packet Stats to stdout for both the router and each link. s : sent , r :
	 * recv , d : drop , f : forw
	 * 
	 * @param now the current virtual time
	 */
	public void dumpPacketStats(int now) {
		String s = "Pkt stats for node " + node_id + " : ";
		s = s + " s " + counter[SENT];
		s = s + " r " + counter[RECV];
		s = s + " d " + counter[DROP];
		s = s + " f " + counter[FORW];
		s = s + "\n";
		for (int i = 0; i < links.length; i++) {
			s = s + links[i].dumpPacketStats() + "\n";
		}
		System.out.print(s);
	}

	/**
	 * Gets this <code>Node</code> id
	 * 
	 * @return int
	 */
	public int getId() {
		return node_id;
	}

	/**
	 * In the same processing step the presence of <code>Packet</code> delivery
	 * events cannot coexist with (same type) timeout events, since the reception
	 * cancels the timeout but it is not guaranteed that packet delivery is treated
	 * before timeouts
	 */
	private void clean_input_queue() {
		boolean has_app_delivery = false;
		boolean has_control_delivery = false;
		// System.err.println("clean_input_events");
		for (Event ev : inputEvents) {
			if (ev.getOperation() == EventType.DELIVER_PACKET) {
				if (ev.getPacket().getType() == PacketType.DATA)
					has_app_delivery = true;
				else if (ev.getPacket().getType() == PacketType.CONTROL)
					has_control_delivery = true;
			}
		}
		if (!has_app_delivery && !has_control_delivery)
			return; // there are no packet deliver events

		Queue<Event> newInputList = new LinkedList<>();
		while (inputEvents.size() > 0) {
			Event ev = inputEvents.poll();
			if (ev.getOperation() == EventType.APP_TIMEOUT && has_app_delivery || ev.getOperation() == EventType.CONTROL_TIMEOUT && has_control_delivery) {
				// remove from input list
			} else
				newInputList.add(ev);
		}
		inputEvents = newInputList;
		// System.err.println("clean_input_events cleaned
		// "+(inputEvents.size()-newInputList.size())+" events");
	}

	/**
	 * Process the <code>Event</code>s scheduled by the simulator for this
	 * processing step
	 */
	public void process_input_events(int n) {
		if (inputEvents.size() == 0)
			return; // nothing to process
		now = n;
		clean_input_queue();
		while (inputEvents.size() > 0) {
			Event ev = inputEvents.poll(); // gets the head of the events queue and removes it
			// System.err.println("process_input_events: processing: "+ev);
			if (ev.getTime() != now) {
				System.err.println("process_input_events: out of order event? " + ev);
				System.exit(-1);
			}
			if (ev.getOperation() == EventType.UPLINK)
				control_alg.on_link_up(now, ev.getInterface());
			else if (ev.getOperation() == EventType.DOWNLINK)
				control_alg.on_link_down(now, ev.getInterface());
			else if (ev.getOperation() == EventType.CONTROL_TIMEOUT) {
				if (ev.getTime() == next_control_timeout)
					control_alg.on_timeout(now); // the node is still waiting for this timeout
				else {
				}
				; // ignore this timeout
			} else if (ev.getOperation() == EventType.APP_TIMEOUT) {
				if (ev.getTime() == next_app_timeout) // the node is still waiting for this timeout
					app_alg.on_timeout(now);
				else {
				}
				; // ignore this timeout
			} else if (ev.getOperation() == EventType.CONTROL_CLOCK_TICK) {
				control_alg.on_clock_tick(now);
				outputEvents.add(new Event(EventType.CONTROL_CLOCK_TICK, now + control_clock_tick_period, 0, null, null, node_id, 0));
			} else if (ev.getOperation() == EventType.APP_CLOCK_TICK) {
				app_alg.on_clock_tick(now);
				outputEvents.add(new Event(EventType.APP_CLOCK_TICK, now + app_clock_tick_period, 0, null, null, node_id, 0));
			} else if (ev.getOperation() == EventType.DELIVER_PACKET) {
				Packet p = ev.getPacket();
				if (p.getTtl() == 1) {
					counter[DROP]++;
					System.out.println("Dropping expired packet " + p);
					return; // ignore packet
				}
				if (p.getDestination() == node_id || p.getDestination() == Packet.ONEHOP) { // local packet
					// System.out.println("Received packet "+p);
					counter[RECV]++;
					if (p.getType() == PacketType.DATA) {
						next_app_timeout = 0; // cancels all waiting timeouts
						app_alg.on_receive(now, p.toDataPacket() );
					} else if (p.getType() == PacketType.CONTROL) {
						next_control_timeout = 0; // cancels all waiting timeouts
						control_alg.on_receive(now, p, ev.getInterface());
					} else if (p.getType() == PacketType.TRACING) {
						// make the result of the tracing available
						System.out.println("Received tracing packet at time " + now + " " + p);
					} else {
						System.err.println("Node process_events: unknown received packet type " + p);
						System.exit(-1);
					}
				} else { // not a local packet, forward it
					p.decrementTtl();
					counter[FORW]++;
					control_alg.forward_packet(now, p, ev.getInterface());
				}
			} else {
				System.out.println("Node process_events: Unknown event " + ev);
			}
		}
	}

	/***************************************************************************
	 * 
	 * Algorithms down calls
	 * 
	 ***************************************************************************/

	/**
	 * Sends an application <code>DataPacket</code>; by convention the input
	 * interface of a locally created and sent packet is the local interface (-1)
	 * this method is to be be used by the application algorithm
	 * 
	 * @param p the packet
	 */
	public void send(DataPacket p) {
		if (p == null) {
			System.err.println("send: no packet to send");
			System.exit(-1);
		}
		if (p.getSource() != node_id) {
			System.err.println("send: can only send local origin packets");
			System.exit(-1);
		}
		if (p.getType() != PacketType.DATA) {
			System.err.println("send: can only send data packets");
			System.exit(-1);
		}
		if (p.getDestination() == node_id) { // local delivery
			outputEvents.add(new Event(EventType.DELIVER_PACKET, now + 1, 0, null, p, node_id, LOCAL));
		} else
			control_alg.forward_packet(now, p, LOCAL);
	}

	/**
	 * Sends a <code>Packet</code> using a given interface Besides increasing
	 * counters, this could be done by Control Algorithms It is however, cleaner and
	 * better practice to make it available here to be shared by all different
	 * ControlAlgorithms
	 * 
	 * @param p     the packet
	 * @param iface the interface
	 */
	public void send(Packet p, int iface) {
		if (p == null) {
			System.err.println("control_send: no packet to send");
			System.exit(-1);
		}
		if (iface == LOCAL || p.getDestination() == node_id) {
			// local delivery
			outputEvents.add(new Event(EventType.DELIVER_PACKET, now + 1, 0, null, p, node_id, LOCAL));
			counter[SENT]++;
		} else if (iface == UNKNOWN || iface >= num_interfaces) {
			// drop the packet since it is impossible to send it
			counter[DROP]++;
		} else {
			links[iface].enqueuePacket(node_id, p); // the link side is relative to the node calling it
			counter[SENT]++;
		}
	}

	/**
	 * Returns the interface weight for the specified interface. This may or should
	 * be computed by Control Algorithms. However, it is given as an help and
	 * example
	 * 
	 * @param iface
	 * @param type  (= 1, weight always 1 for RIP, 2 weight better than RIP weight ,
	 *              3 weight for OSPF
	 * @return int (weight)
	 */
	public int getInterfaceWeight(int iface, int type) {
		if (iface == ControlAlgorithm.LOCAL) {
			return 0;
		} // loopback interfaces have no cost
		long bandwith = links[iface].getBandWidth();
		int result = 1;
		if (type == 3)
			return (int) (1000000000.0 / bandwith); // ref. is 1000 Mbps
		else if (type == 2) { // Discrete values
			if (bandwith <= 1000000)
				result = 3;
			else if (bandwith <= 10000000)
				result = 2;
			else
				result = 1;
		}
		return result;
	}

	/**
	 * Installs an application timeout The reception of a data message before or at
	 * now+t cancels all application timeouts (including those to be delivered in
	 * the same time step)
	 * 
	 * @param t the timeout value
	 */
	public void set_timeout(int t) {
		if (t < 1) {
			System.err.println("set_app_timeout: timeout value must be >= 1");
			System.exit(-1);
		}
		next_app_timeout = now + t; // next expected timeout
		outputEvents.add(new Event(EventType.APP_TIMEOUT, next_app_timeout, 0, null, null, node_id, 0));
	}

	/**
	 * Installs a control timeout The reception of a data message before or at now+t
	 * cancels all control timeouts (including those to be delivered in the same
	 * time step)
	 * 
	 * @param t the timeout value
	 */
	public void set_control_timeout(int t) {
		if (t < 1) {
			System.err.println("set_control_timeout: timeout value must be >= 1");
			System.exit(-1);
		}
		next_control_timeout = now + t; // next expected timeout
		outputEvents.add(new Event(EventType.CONTROL_TIMEOUT, next_control_timeout, 0, null, null, node_id, 0));
	}

	/**
	 * Creates a data packet with the current node as sender the
	 * ApplicationAlgorithm could implement the functionality but setting a good
	 * sequence number
	 * 
	 * @param receiver the receiver id
	 * @param payload  the payload of the packet
	 * @return the created data packet
	 */
	public DataPacket createDataPacket(int receiver, byte[] payload) {
		DataPacket p = new DataPacket(node_id, receiver, payload);
		packet_counter++;
		p.setSequenceNumber(packet_counter);
		return p;
	}

	/**
	 * Creates a control packet the ControlAlgorithm could implement the
	 * functionality but setting a good sequence number
	 * 
	 * @param sender   the sender id
	 * @param receiver the receiver id
	 * @param payload  the payload of the packet
	 * @return the created data packet
	 */
	public ControlPacket createControlPacket(int sender, int receiver, byte[] payload) {
		ControlPacket p = new ControlPacket(sender, receiver, payload);
		packet_counter++;
		p.setSequenceNumber(packet_counter);
		return p;
	}

	/**
	 * Returns the interface state for the specified interface, is it up or down.
	 * 
	 * @param iface
	 * @return boolean (true if is Up)
	 */
	public boolean getInterfaceState(int iface) {
		// If the interface value is -1, it is the virtual loop back interface, so
		// it is always up.
		if (iface == LOCAL) {
			return true;
		}
		return links[iface].isUp();
	}

}
