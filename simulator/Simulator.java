package cnss.simulator;

/**  
 * The <code>Simulator</code> class loads the configuration and runs each
 * node with the correct classes for that node as specified in the 
 * configuration file.
 * 
 * At each processing step the Simulator carries out:
 * 
 * global event processing <code>process_events</code> that often generate
 * events for nodes to process and enqueue these events in each node
 * for each node:
 *    <code>process_input_events</code> delivered to the node
 *    <code>enqueue_generated_events</code> by the node in the global queue
 * for each link:
 *    <code>transmitPackets</code> which generate packet delivery events local
 *    to each link
 *    <code>enqueue_packets_to_deliver</code> in the global queue
 *
 * This <code>main_loop</code> ends when there are no more global events to process
 * or the end of simulation time is reached.
 * 
 * @author  System's team of the Department of Informatics of FCT/UNL based on a
 * @author  preliminary version by Adam Greenhalgh of UCL
 * @version 1.0, September 2021                                                            
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import cnss.simulator.Event.EventType;


public class Simulator {
	private String config_file;

	private List<Node> tmp_nodes = new ArrayList<>();
	private List<Link> tmp_links = new ArrayList<>();
	private Node[] nodes;
	private Link[] links;
	private GlobalParameters globalParameters = new GlobalParameters();

	private SortedMap<Long, Event> events = new TreeMap<>();

	private int nextEventId = 0; // allows the generation of global events UUIDs
	private int packet_counter = 0; // allows the generation of tracing packets sequence numbers

	private int stop_time = 1200000; // 20 virtual minutes as time runs in virtual ms

	/**
	 * <code>Simulator</code> constructor, loads the configuration given the config
	 * file cf.
	 * 
	 * @param cf configuration file
	 */
	public Simulator(String cf) {
		config_file = cf;

		System.out.println("Loading configuration : " + config_file);
		config(config_file);
	}

	/**
	 * Simple toString method.
	 * 
	 * @return String
	 */
	public String toString() {
		return "Simple Network Simulator";
	}

	/***********************************************************
	 * 
	 * METHODS FOR SIMULATOR CONFIGURATION
	 * 
	 ***********************************************************/

	/**
	 * <code>config</code> loads the configuration file and configures the
	 * simulator.
	 * 
	 * @param String    filename of the configuration file
	 * @param Simulator s, reference to the simulator object
	 */
	private void config(String filename) {
		System.out.println("Reading file " + filename);
		try {
			BufferedReader input = new BufferedReader(new FileReader(filename));

			String str;
			while ((str = input.readLine()) != null) {
				process_config_line(str);
			}

			input.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		// adding nodes and links to their vectors
		nodes = new Node[tmp_nodes.size()];
		links = new Link[tmp_links.size()];

		// nodes have their own id
		for (Node nd : tmp_nodes) {
			nodes[nd.getId()] = nd;
		}

		// links have no id
		int count = 0;
		for (Link ml : tmp_links) {
			links[count] = ml;
			count++;
		}

		// adding to each node its links
		for (int i = 0; i < links.length; i++) {
			nodes[links[i].getNode(1)].addLinks(links[i]);
			nodes[links[i].getNode(2)].addLinks(links[i]);
		}

		// TODO: it would be helpful if a configuration coherence
		// test would be available

	}

	/**
	 * Processes each line of the configuration file and creates the appropriate
	 * objects
	 * 
	 * @param String line of config from the config file
	 */
	private void process_config_line(String s) {
		String[] result = s.split("\\s");

		if (result.length == 1)
			return;

		if (result[0].equalsIgnoreCase("parameter")) {
			globalParameters.put(result[1], result[2]);
			if (result[1].equals("stop"))
				stop_time = Integer.parseInt(result[2]);
		}

		else if (result[0].equalsIgnoreCase("node")) {
			String[] args = new String[result.length - 5];
			for (int i = 0; i < args.length; i++)
				args[i] = result[i + 5];
			Node nd = new Node(Integer.parseInt(result[1]), Integer.parseInt(result[2]), result[3], result[4], args, globalParameters);
			// result[1] = node id, result[2] = # interfaces, result[3] = control class name,
			// result[4] = app class name result[5] = args[0] .....
			tmp_nodes.add(nd);
		}

		else if (result[0].equalsIgnoreCase("link")) {
			Link l = new Link(
					Integer.parseInt(result[1].split("\\.")[0]),
					Integer.parseInt(result[1].split("\\.")[1]),
					Integer.parseInt(result[2].split("\\.")[0]),
					Integer.parseInt(result[2].split("\\.")[1]),
					Long.parseLong(result[3]),      // bandwidth in bps
					Integer.parseInt(result[4]),    // latency in ms
					Double.parseDouble(result[5]),    // error rate
					Double.parseDouble(result[6]),    // jitter
					this   // the link needs a reference to the simulator to call newEvent
					);
			// the initial state of the link is always "up" unless otherwise stated
			if (result.length == 8) {
				if (result[7].equalsIgnoreCase("down") ) {
					l.setState(false);
				}
			}
			tmp_links.add(l);
		}

		// in all the following events, their packet, node and interface parameters are
		// not used

		else if (result[0].equalsIgnoreCase("traceroute")) {
			// result[1] = time, result[1] = src, result[2] = dst
			int arg_len = result.length - 2;
			String[] args = new String[arg_len];
			for (int i = 0; i < arg_len; i++) {
				args[i] = result[i + 2];
			}
			createMainQueueEvent(EventType.TRACEROUTE, Integer.parseInt(result[1]), args);
		} else if (result[0].equalsIgnoreCase("uplink") || result[0].equalsIgnoreCase("downlink")) {
			int arg_len = result.length;

			String[] args = new String[arg_len];
			args[0] = result[2].split("\\.")[0];
			args[1] = result[2].split("\\.")[1];
			args[2] = result[3].split("\\.")[0];
			args[3] = result[3].split("\\.")[1];
			if (result[0].equalsIgnoreCase("uplink")) {
				createMainQueueEvent(EventType.UPLINK, Integer.parseInt(result[1]), args);
			} else
				createMainQueueEvent(EventType.DOWNLINK, Integer.parseInt(result[1]), args);
		} else if (result[0].equalsIgnoreCase("dumproutes")) {
			// result[1] = time, result[2] = all or node id
			String[] args = new String[1];
			args[0] = result[2];
			createMainQueueEvent(EventType.DUMPRT, Integer.parseInt(result[1]), args);
		} else if (result[0].equalsIgnoreCase("dumpcontrolstate")) {
			// result[1] = time, result[2] = all or node id
			String[] args = new String[1];
			args[0] = result[2];
			createMainQueueEvent(EventType.DUMPCONTROLSTATE, Integer.parseInt(result[1]), args);
		} else if (result[0].equalsIgnoreCase("dumpappstate")) {
			// result[1] = time, result[2] = all or node id
			String[] args = new String[1];
			args[0] = result[2];
			createMainQueueEvent(EventType.DUMPAPPSTATE, Integer.parseInt(result[1]), args);
		} else if (result[0].equalsIgnoreCase("dumppacketstats")) {
			// result[1] = time, result[2] = all or node id
			String[] args = new String[1];
			args[0] = result[2];
			createMainQueueEvent(EventType.DUMPPACKETS, Integer.parseInt(result[1]), args);
		} else if (result[0].startsWith("#")) {
			// skipping comments
		} else {
			System.out.println("Wrong config file line : " + s);
			System.exit(-1);
		}
	}

	/**********************************************************************
	 * 
	 * SIMULATION EXECUTION
	 * 
	 **********************************************************************/

	/**
	 * Process the events scheduled for the time <code>now</code>
	 * 
	 * @param now current time
	 * @return true if some event has been processed, false otherwise
	 */
	private void process_events(int now) {
		while (events.size() > 0) {
			long uuid = events.firstKey();
			Event ev = events.get(uuid);
			if (ev.getTime() > now)
				return; // nothing to do at this time
			// otherwise treat the event
			events.remove(uuid);

			switch (ev.getOperation()) {
			case TRACEROUTE:
				// a new tracing packet is created and the origin node starts its forwarding
				// the source node will proceed by sending the packet

				Packet packet = new TracingPacket(Integer.parseInt(ev.getArgument(0)), Integer.parseInt(ev.getArgument(1)), "".getBytes());
				packet_counter++;
				packet.setSequenceNumber(packet_counter);
				ev.setPacket(packet);
				ev.setNode(packet.getSource());
				ev.setOperation(EventType.DELIVERPACKET);
				nodes[packet.getSource()].addInputEvent(ev);
				break;

			case UPLINK:
			case DOWNLINK:
				for (int i = 0; i < links.length; i++) {
					if (links[i].getNode(1) == Integer.parseInt(ev.getArgument(0)) && links[i].getInterface(1) == Integer.parseInt(ev.getArgument(1))
							&& links[i].getNode(2) == Integer.parseInt(ev.getArgument(2)) && links[i].getInterface(1) == Integer.parseInt(ev.getArgument(3))) {
						if (ev.getOperation() == EventType.UPLINK) {
							System.out.println("Setting link status to up " + links[i]);
							links[i].setState(true);
						} else {
							System.out.println("Setting link status to down " + links[i]);
							links[i].setState(false);
						}
						// the two sides of the link must be notified;
						ev.setNode(links[i].getNode(1));
						ev.setInterface(links[i].getInterface(1));
						Event ev2 = ev.clone();
						ev2.setNode(links[i].getNode(2));
						ev2.setInterface(links[i].getInterface(2));
						nodes[links[i].getNode(1)].addInputEvent(ev);
						nodes[links[i].getNode(2)].addInputEvent(ev2);
					}
				}
				break;
			case DUMPRT: // Immediately executed
				if (ev.getArgument(0).equals("all")) {
					for (int i = 0; i < nodes.length; i++) {
						nodes[i].dumpRoutingTable(now);
					}
				} else {
					nodes[Integer.parseInt(ev.getArgument(0))].dumpRoutingTable(now);
				}
				break;
			case DUMPPACKETS: // Immediately executed
				// System.out.println("event "+ev);
				if (ev.getArgument(0).equals("all")) {
					for (int i = 0; i < nodes.length; i++) {
						nodes[i].dumpPacketStats(now);
					}
				} else {
					nodes[Integer.parseInt(ev.getArgument(0))].dumpPacketStats(now);
				}
				break;
			case DUMPCONTROLSTATE: // Immediately executed
				// System.out.println("event "+ev);
				if (ev.getArgument(0).equals("all")) {
					for (int i = 0; i < nodes.length; i++) {
						nodes[i].dumpControlState(now);
					}
				} else {
					nodes[Integer.parseInt(ev.getArgument(0))].dumpControlState(now);
				}
				break;
			case DUMPAPPSTATE: // Immediately executed
				// System.out.println("event "+ev);
				if (ev.getArgument(0).equals("all")) {
					for (int i = 0; i < nodes.length; i++) {
						nodes[i].dumpAppState(now);
					}
				} else {
					nodes[Integer.parseInt(ev.getArgument(0))].dumpAppState(now);
				}
				break;
			case CONTROLTIMEOUT:
			case APPTIMEOUT:
			case DELIVERPACKET:
			case CONTROLCLOCKTICK:
			case APPCLOCKTICK:
				nodes[ev.getNode()].addInputEvent(ev);
				break;
			default:
				System.out.println("Unknown event " + ev);
				System.exit(-1);
			}
		}
	}

	/**
	 * Main loop of the simulator that runs through all the tasks at each time step.
	 * At each time step with events to process, the Simulator carries out (in
	 * order): event processing <code>process_events</code>, for each node processes
	 * nodes tasks <code>process_input_events</code> and puts in the main queue
	 * event the new events generated it generates and then processes the
	 * transmission of packets <code>process_packets</code> by all links.
	 */
	public void main_loop() {
		System.out.println("\nsimulation starts - processing step of time = 0\n");
		// start all nodes
		// nodes are initialized at time step 0
		nextEventId = 0;
		int now = 0;
		for (int i = 0; i < nodes.length; i++) {
			nodes[i].initialize();
			enqueue_generated_events(nodes[i], now);
		}
		// transmit the enqueued packets by initialization of nodes and
		// enqueue in the global queue the generated delivery events
		for (int i = 0; i < links.length; i++) {
			links[i].transmitPackets(0);
			enqueue_packets_to_deliver(links[i], now);
		}

		while (events.size() > 0) {
			now = events.get(events.firstKey()).getTime();
			if (now > stop_time)
				break;
			// process queued events scheduled for now and redistribute
			// them by the corresponding nodes
			process_events(now);
			// make each node to process its events and enqueue in the
			// global queue the events it generates
			for (int i = 0; i < nodes.length; i++) {
				nodes[i].process_input_events(now);
				enqueue_generated_events(nodes[i], now);
			}
			// transmit the enqueued packets by nodes and enqueue in the
			// global queue the generated delivery events
			for (int i = 0; i < links.length; i++) {
				links[i].transmitPackets(now);
				enqueue_packets_to_deliver(links[i], now);
			}
		}
		System.out.println("\nsimulation ended - processing step of time = " + now + "\n");
		check_completed();
	}

	/**
	 * main function called from the command line with one argument which is the
	 * configuration file.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("Usage : java Simulator <config file>");
			System.exit(1);
		}
		// instantiates and configures the simulator and starts it
		new Simulator(args[0]).main_loop();
	}

	/**********************************************************************
	 * 
	 * AUXILIARY METHODS
	 * 
	 **********************************************************************/

	/**
	 * Creates a new event with appropriate uuid and adds it to the main event queue
	 * 
	 * @param op the operation of the event.
	 * @param t  the time the event should be triggered.
	 * @param a  String[] parameters of the event.
	 * @param p  the packet associated with the event.
	 * @param n  the node associated with the event.
	 * @param s  the interface associated with the event.
	 * 
	 */
	public void createMainQueueEvent(EventType op, int t, String[] a, Packet p, int n, int s) {
		if (t > stop_time) {
			// System.err.println("new event time older than stop_time");
			return;
		}
		nextEventId++;
		Event ev = new Event(op, t, nextEventId, a, p, n, s);
		events.put(ev.getUUID(), ev);
		// System.out.println("Adding "+ev);
	}

	/**
	 * Creates a new event with appropriate uuid and adds it to the main event queue
	 * 
	 * @param op the operation of the event.
	 * @param t  the time the event should be triggered.
	 * @param a  String[] parameters of the event.
	 * 
	 */
	public void createMainQueueEvent(EventType op, int t, String[] a) {
		if (t > stop_time) {
			// System.err.println("new event time older than stop_time");
			return;
		}
		nextEventId++;
		Event ev = new Event(op, t, nextEventId, a);
		events.put(ev.getUUID(), ev);
		// System.out.println("Adding "+ev);
	}

	/**
	 * Creates a new event with appropriate uuid from a previously created event
	 * with a (potentially) not unique uuid and adds it to the main event queue
	 * 
	 * @param e the event created by the node
	 * 
	 */
	public void createMainQueueEvent(Event e) {
		if (e.getTime() > stop_time) {
			// System.err.println("new event time older than stop_time");
			return;
		}
		nextEventId++;
		Event ev = new Event(e.getOperation(), e.getTime(), nextEventId, e.getArgs(), e.getPacket(), e.getNode(), e.getInterface());
		events.put(ev.getUUID(), ev);
		// System.out.println("Adding "+ev);
	}

	/**
	 * Processes the events in the output queue of a Link at the end of a
	 * processing step by controlling them and transferring them to the main event
	 * queue
	 * 
	 * @param l   the link
	 * @param now virtual clock of the executed processing step
	 * 
	 */
	private void enqueue_packets_to_deliver(Link l, int now) {
		Event ev = l.getOutputEvent();
		while (ev != null) {
			if (ev.getTime() <= now) {
				System.err.println("Time: " + now + " link: " + l + " new deliver packet event time younger than now");
				return;
			}
			createMainQueueEvent(ev);
			ev = l.getOutputEvent();
		}
	}

	/**
	 * Processes the events generated by a Node at the end of its processing step
	 * and transfers them to the main event queue
	 * 
	 * @param n   the node
	 * @param now virtual clock of the executed processing step
	 */
	private void enqueue_generated_events(Node nd, int now) {
		Event e = nd.getOutputEvent();
		while (e != null) {
			if (e.getTime() <= now) {
				System.err.println("Time: " + now + " node: " + nd + " new deliver packet event time younger than now");
				return;
			}
			createMainQueueEvent(e);
			e = nd.getOutputEvent();
		}
	}

	/**
	 * Checks that all events have been processed.
	 */
	private void check_completed() {
		if (events.size() > 0) {
			System.out.println("Error, " + events.size() + " events not run. Stoped too early?");
			System.exit(-1);
		}
	}

}
