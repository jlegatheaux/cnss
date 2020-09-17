# CNSS Overview ( Computer Networks Simple Simulator )

CNSS is written in Java and has been developed for teaching purposes.  

CNSS was inspired by a simulator, developed around 2001, by Adam Greenhalgh from the University College of London. This simulator was
mainly intended for testing routing algorithms based on the distance vector principle, and was limited to networks with zero transit
time links. 
 
CNSS is capable of simulating any routing algorithm, as well as simple applications running on heterogeneous nodes. To that end, CNSS leverages a more realistic notion of link, characterised by transmission and propagation delays as well as error rate. Morevoer, in CNSS a network can be comprised of different types of nodes, capable of executing user provided control and application algorithms, in a more generic fashion. 

To allow a link to simulate a logical link made of a set of links and packet switches, it is also possible to introduce jitter in a link if one so desires.

## CNSS in short

CNSS simulates a packet switched network where each processing step is **instantaneous**, i.e., executed without advancing the virtual clock, while communications make that clock advance. Virtual time resolution is 1 millisecond, i.e., the clock advances 1 by 1 ms. All nodes and links update their state in a lock step way, according to a logically, perfectly synchronised global notion of time. Their state changes from a state to the following one by executing processing steps. The main concepts of the simulator are presented next.

A network is composed of a set or mix of nodes, interconnected by links. 

Nodes exchange packets. 

In each clock tick, all nodes execute a processing step by consuming the events scheduled to them for this processing step. These events may be clock ticks, alarms or packet deliveries to the node. The execution of a processing step in each node may generate future events like alarms, delivery of messages to the same or other nodes, etc.

Nodes execute a common kernel that triggers processing steps execution using up calls to the methods of two algorithms defined by the users: a control algorithm and an application algorithm. These algorithms may be different in each node. These up calls processing steps can send messages, set alarms, etc. to the nodes algorithms using interfaces downcalls. The system imposes that the execution of each node is structured as the execution of two automata: a control automaton controlled by the control algorithm, and an application automaton controlled by the application algorithm.

Network configuration is defined by a configuration file using simple commands to add nodes and links and their parameters, as well as special events that can be triggered at configuration defined virtual time values.

The real execution progress of the simulation is bound to the time required to execute the nodes processing steps. Therefore, nodes processing steps cannot execute blocking actions, which would block the simulator. Reading and writing local files is accdeptable as well as any other quick execution method calls, but using Java calls like Thread.sleep() or any kind of synchronization is fully discouraged.

Next, packets, nodes, links and the configuration file are presented in more detail.

## Packets

Packets are objects of the class *Packet*. This class has several subclasses among which *DataPacket* and *ControlPacket* that represent different types of packets. Data packets are sent and received by ApplicationAlgorithms. Control packets are sent and received by the ControlAlgorithms. In fact, by analogy, these two types of packets could also be understood as if CNSS supports two fixed IP ports. One addressing a traditional OS kernel, and the other addressing a single application running in the node. 

Packets have several fields, namely: 

```java
protected int src;  // the initial sending node
protected int dst;  // the destination node
protected int ttl;  // packets time-to-live
protected PacketType type; // DATA, CONTROL, ...
protected int seq;  // sequence number
protected int size; // size of the full packet including header and payload
protected byte[] payload; // the payload of the packet
``` 

Some contants in the Packet class have speacial meaning for the CNSS notion of Packet: *HEADERSIZE = 20* is the size of the header to mirror IPv4 packets size and *INITIALTTL = 32* is the default value of packets TTL. 

## Nodes

Nodes execute two algorithms, an application algorithm and a control algorithm. Each of these algorithms is structured as an automaton executing actions associated with a pre-defined set of events, each one called an **upcall**.

Among the most important upcalls are: `initialise(int now, ...), on_clock_tick(int now), on_receive(int now, DataPacket p), on_timeout(int now)` and several others. Nodes automata may choose to use *clock_ticks*, if so, their periodic value in millisecends should be returned by the `initialise(...)` upcal. If the `initialise` method returns 0, no *clock_ticks* will be delivered to this algorithm.

Each upcall, but the `initialise` one, is triggered by the delivery of an event that got to the node. All events that should be triggered in the same processig step (characterized by the same value of the *clock*) are delivered in sequence without any predefineded specified order. 

The definition of the interfaces of the two algorithms executed by nodes are presented next.

## ApplicationAlgorithm Interface

The `ApplicationAlgorithm interface should be implemented by any class whose instances are intended to implement application automata executed by nodes. The class that implements this algorithm must have a zero argument constructor. All methods have, as first argument, the virtual time of the processing step where the event fired. Methods:

```java
public int initialise(int now, int node_id, Node nodeObj, String[] args);
```

Initialises the application algorithm or automaton and returns the desired *control_clock_tick_period*. If the returned *control_clock_tick_period is equal to 0*, no clock_ticks will be delivered to the algorithm.

Parameters: `id` is this node id, `nodeObj` is a reference to the node object kernel executing this algorithm, `args` is an array of arguments specified in the *configuration file*  (see the configuration file section). 

```java
public void on_clock_tick(int now);
```

Signals a clock tick event.	

```java
public void on_timeout(int now);
```
	
Signals a timeout event.

```java
public void on_receive(int now, DataPacket p);
```

Given a data packet from another node, here it is and process it. Parameter: `p` the received packet.

```java
public void showState(int now);
```

Prints application state table(s) to the screen in a previously agreed format of users of a simulation. This up call is called at each time step where there is a correspondent event directed to the node. See the section on the configuration file.

The node processing steps application algorithm can use public methods of the class DataPacket as well as the following down calls:

```java
nodeObj.createDataPacket (int destination, byte[] payload)
nodeObj.send(DataPacket p)
nodeObj.set_timeout(int t)
```

When a packet is directly created, its sequence number is 0. In order to guarantee that packet sequence numbers are different (relative to each node), packets must be created using `nodeObj.createDataPacket(…)` method, which takes care of providing unique sequence numbers.


## ControlAlgorithm Interface

The `ControlAlgorithm` interface should be implemented by any class whose instances are intended to implement a control automata executed by nodes, for example, the way the node routes packets not directed to himself. The `ControlAlgorithm` interface should be implemented by any class whose instances are intended to implement the control part of the node. The class that implements this algorithm must have a zero argument constructor. All upcall methods have as first argument the virtual time of the processing step where the event fired. First are apresented some of the interface constants, and then the methods.

```java
static final int LOCAL = Node.LOCAL;       // the number of the virtual loop back interface
static final int UNKNOWN = Node.UNKNOWN;   // means an inexistent or unknown interface 
```

```java
public int initialise(int now, int node_id, Node nodeObj, GlobalParams parameters, Link[] links, int nint);
```	
Initializes the control algorithm and returns the desired *control_clock_tick_period*. If the returned *control_clock_tick_period is equal to 0*, no **clock_ticks** will be delivered to the algorithm. Interfaces are numbered 0 to `nint-1`. Each has a link attached: `links[i]`. Interface `LOCAL`, with value -1, is virtual and denotes, when needed, the local loop interface. 

Parameters: `id` is this node id, `nodeObj` is a reference to the node kernel object executing this algorithm, `parameters` is the collection of global parameters (see the configuration file section), `links` is the nodes links array, `nint` is the number of interfaces (or links) of this node. The method must return the requested *clock_tick_period* value. 

```java
public void on_clock_tick(int now);
```
Signals a clock tick event.
	
```java
public void on_timeout(int now);
```

Signals a timeout event.

```java
public void on_receive(int now, Packet p);
```

Given a control packet from another node, here it is, process it. Parameter: `p` the packet received.

	
```java
public void forward_packet(int now, Packet p, int iface);
```

Given a packet destinated to another node, forward it to the appropriate interfaces by using the downcall `nodeObj.send(Packet p, int iface)`. Parameters are: `p` is the packet to forward, `iface` is the interface where this node received that packet. If it is not possible to forward the packet, deliver it using `nodeObj.send(Packet p, Node.UNKNOWN)`, since this way the `node` will correctly count all dropped packets.

```java
public void on_link_up(int now, int iface);
public void on_link_down(int now, int iface);
```

Signals a link up or down event. Parameter: `iface` the interface (link) that changed state.


```java
public void showControlState(int now);
```

Prints control algorithm state table(s) to the screen in a previously user agreed format.

```java
public void showRoutingTable(int now);
```

Prints control algorithm routing table to the screen in a previously user agreed format.

These up calls are called at each time step where there is a correspondent event directed to the node, see the section on the configuration file.

The node processing steps control algorithm can use the following down calls:

```java
nodeObj.send(DataPacket p)
nodeObj.send(Packet p, int iface)
nodeObj.set_control_timeout(int t)

nodeObj.createDataPacket (int receiver, byte[] payload)
nodeObj.createControlPacket (int sender, int receiver, byte[] payload)
nodeObj.getInterfaceState(int iface)
```

The node control algorithm processing steps or *upcalls* can also use the public methods of the following objects:

```java
class Packet
class Link (*)
class Node (*)
class Parameters (*)
```

(*) but never using public methods that write the state of these objects.

When a packet is created, its sequence number is 0. In order to guarantee that packet sequence numbers are different (relative to each node), packets must be created using `nodeObj.createDataPacket(…)` and `nodeObj.createControlPacket(...)` methods, which take care of providing unique sequence numbers. Therefore, this algorithm must avoid creating packets directly when maintaining packets sequence numbers uniqueness is important.

## Links

The model of links is very simple: a link is point to point (connects exactly two nodes) and has two extremes, end 1 and end 2, each directly connected to one interface in a (in general different) node. Links are charactetized by:

```java
private long bwidth = 1000;  // in bits per second - bps
private int latency = 0;     // in ms
private double errors = 0.0; // error rate in % - 0.0 is a perfect (no errors) link
private double jitter = 0.0; // in % - 0.0 is a link without jitter
private boolean up;   
```

Besides these variables, links have two queues at each end: an *out queue* or output queue, and an *in queue* or input queue. At the end of each processing step, packets queued in the *out queues* of all links are consumed and become *delivery* events, associated with the other extreme of the link, to be delivered when the corresponding transit time ends. Transit times are computed using the time required to transmit the packet, as well as those in front of it in the same *out queue*, added to the propagation time.

## Network definition and simulation configuration file

To start a simulation, a *configuration file* must be given as parameter.

```
java -cp bin cnss.simulator.Simulafor config.txt
```
Virtual time is in milliseconds, starts at 0 and ends at a value that can be changed in the configuration file. Its limit is *Integer.MAXVALUE* which corresponds to around two million seconds or more or less 555 hours of virtual time.

### The configuration file is made of lines

These lines obey a simple syntax. In the current version, tokens must be separated by exactly one space chracter. The different possible configuration file lines are the following.

```
parameter name value 
```

Defines a global parameter of name _name_ and value _value_ (both are character strings without any blanck character in the middle); global parameters are accessible to nodes ControlAlgorithms as a collection of name / value pairs accessible via an hash map collection.

Examples:
```
parameter stop 100000  
```
This parameter defines the duration of the simulation in virtual ms. It is good practice to make this the first line of the config file; this first parameter is directly recognized by the simulator.

```
parameter splithorizon true
parameter expiration true
```

which can be used to parametrize the control algorithms.

```
node node_id #interfaces name_of_control_class name_of_application [class args …]
```

Node ids must start at 0 and follow a strict increasing order. Args are accessible to the node application algorithm via a `String[] args` parameter of the `initialise()` method.

Example:

```
Node 1 5 FloodSwitchAlgorithm SwitchAppAlgorithm hello world
```

```
link side1_node.side1_interface side2_node.side2_interface bandwidth latency errors jitter [ state ]
```
Example:
```
link 0.0 1.0 10000000 10 0.0 0.0 down
```

introduces a link from interface 0 of node 0 to interface 0 of node 1 with a 10 Mbps bit rate, 0.0 error rate, 0.0 jitter and starting in state down. Jitter is defined as percentage of the link bandwidth and the propagation time varies randomly in the range [latency .. latency * jitter].

The configuration file can also introduce several types of events to be fired at given time steps. The general syntax is *event_name time_of_event event_parameters*. Here are examples of the available events. 

```
traceroute 12000 origin_node destination_node
dumpappstate 8000 [ all | node id ]
dumproute 1000 [ all | node id ]
dumpPacketStats 120000 [ all | node id ]
uplink / downlink 18000 link_origin link_destination
dumpcontrolstate 8000 [ all | node id ]
```

The first one sends a *tracing packet* at *time = 12000* from *from_node* to *destination_node*. Tracing packets are directly recognized by nodes kernels and allow tracing the path from origin to destination in the current configuration, using the instantiated *control algorithms*.

The *dumpappstate* one delivers a *dumpappstate event* at *time = 8000* to the *Application Algorithm* of all nodes or to a specific one.

The *dumproute* one delivers a *dumproute event* at *time = 1000* to the *Control Algorithm* of all nodes or to a specific one.

The *dumpPacketsStats* one delivers a *dump packet statistics event* at *time = 120000* to all nodes or to a specific one.

The *uplink / downlink* ones delivers an *uplink event or a down link event* at *time = 18000* to the *Control Algorithm* of all nodes or to a specific one.

The *dumpcontrolstate* one delivers a *dumpcontrolstate event* at *time = 8000* to the *Control Algorithm* of all nodes or to a specific one.

Finally, a line starting with ´#´is considered a *comment*.

In the configuration file, in the first token or command, character case is not relevant. For example, writing 'node' or writing 'NoDe' produces the same result. The same is true for events to be fired. 'dumpPacketStats' or 'dumppacketstats' produces the same result. It is also possible to use underscrores as separators while writing events names, as shown in the table below, where each row shows equivalent forms of writing the same token.

| Original name        | Using case to highlight | Using underscores        |
| -------------------- |-------------------------| -------------------------|
| dumpappstate         | DumpAppState            | dump_app_state           |
| dumproute            | DumpRoute               | dump_route               |
| dumppacketstats      | dumpPacketStats         | dump_packet_stats        |
| dumpcontrolstate     | DumpControlState        | dump_control_state       |


## Example of a configuration file

```
# Simple network: 2 nodes and one switch

parameter stop 8000   # 8 seconds

node 0 2 cnss.library.FloodingSwitch cnss.library.EmptyApp
node 1 1 cnss.library.EndSystemControl cnss.examples.Sender
node 2 1 cnss.library.EndSystemControl cnss.examples.Receiver

Link 0.0 1.0 1000000 50 0 0
Link 0.1 2.0 1000000 50 0 0

dumpAppState 8000 all
dumpPacketStats 8000 1
dumpPacketStats 8000 2
```

This configuration file defines the network of the figure below. Two nodes, node 1 and 2, are connected to a switch, node 0. Links have 1 Mbps bandwidth and 50 ms of propagation time. The configuration file sets the end of the simulation to 8000 ms and schedules events *dump_app_state* to be delivered to all nodes, and events *dump_packet_stats* to be sent to nodes 1 and 2, all at simulation time = 8000, the end of the simulation. The code of the different classes is shown below, as well as the output of the simulation execution.

!(Figures/simpleNet.config.png "A simple network with two application nodes and a switch")


Class `Sender()` is a simple sender that sends a packet to the `Receiver()` every second. Its `initialise(...)` method returns 1000, i.e. the value of the interval among clock ticks. Whenever it receives a packet, it prints its value using the method `log(...)`. Whenever it receives a *dumpAppState* event, the node kernel calls the `showState()`upcall and it prints the numbers of reply packets received.

```Java
package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;
import cnss.simulator.DataPacket;

public class Sender implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;

	private String name = "sender";
	private boolean logOn = true;
	private	int count = 0;

    
	public Sender() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;

		log(now, "starting pings");
		return 1000;
	}

	public void on_clock_tick(int now) {
		count++;
		byte[] message = ("ping "+count).getBytes();
		DataPacket p = nodeObj.createDataPacket(2, message);
		log(now, "sent ping packet n. "+count+" - " + p);
		nodeObj.send(p);
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, DataPacket p) {
		log(now, " received reply \"" + new String(p.getPayload()) + "\"");
	}

	public void showState(int now) {
		System.out.println(name + " received "+count+" replies to pings");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
```
Class `Receiver()` implements the receiver node algorithm, one that only prints the contents of each received packet and replies to the sender, mirroring a *ping reply*. It also prints the number of received messages when it receives a *dump_app_stat* event signaled via a `showState()` upcall.

```java
package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;

public class Receiver implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;

	private String name = "receiver";
	private boolean logOn = true;
	private	int counter = 0;

	public Receiver() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;

		log(now, "started listening");
		return 0;
	}

	public void on_clock_tick(int now) {
		log(now, "clock tick");
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, DataPacket p) {
	    counter++;
		String msg = name + " received \"" + new String(p.getPayload()) + "\"";
		log(now, msg);
		// Reply to sender
		DataPacket reply = nodeObj.createDataPacket(p.getSource(), msg.getBytes());
		nodeObj.send(reply);
	}

	public void showState(int now) {
		System.out.println(name + " replyed to "+counter+" ping messages");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
```
The example also illustrates how control algorithms can be used to forward packets. Class `EndSystemControl()` implements packet forwarding in a node with one only interface. If the interface from which the packet came is the local interface, and the node interface is up, the packet is forwarded to the node interface, i.e. the one that is not the local loop one. The node kernel only calls the upcall `forward_packet()` of the control algorithm to forward a packet whose destination is not the local node, thus, it is useless to test if the destination of the packet os not the node itself. 

Whenever it is not possible to forward the packet, for example because the interface is down, the downcall `send()` is still called with `UNKNOWN` as the value of the interface used to forward the packet. This allows the node kernel to count dropped packets. If `tracingOn == true`, the algorithm prints a trace of its execution to help the simulation users to understand what is going on.

Class `EndSystemControl()` can only adequately forward packets of node using a kind of *default route*, i.e. with one only interface. Therefore, during its initialization, it tests this condition to avoid simulation users the trouble of using this control inadequately. This method also `returns 0` since this control does not requires the use of periodic clock ticks.

```java
package cnss.library;

// the control (routing) of an end system with one only interface

import cnss.simulator.ControlAlgorithm;
import cnss.simulator.GlobalParameters;
import cnss.simulator.Link;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class EndSystemControl implements ControlAlgorithm {
	
	private Node nodeObj;
	private int nodeId;
	private GlobalParameters parameters;
	private Link[] links;
	private int numInterfaces;
	private String name="end system";
	private boolean tracingOn = false;

	public EndSystemControl() {
		
	}

	public int initialise(int now, int node_id, Node mynode, GlobalParameters parameters, Link[] links, int nint) {
		if ( nint > 1 ) {
			tracingOn = true;
			trace(now,"end system has more than one interface");
			System.exit(-1);
		}
		nodeId = node_id;
		nodeObj = mynode;
		this.parameters=parameters;
		this.links=links;
		numInterfaces=nint;
		return 0;
	}
	
	
	public void on_clock_tick(int now) {
		trace(now,"clock tick");
	}
	
	public void on_timeout(int now) {
		trace(now,"timeout");
	}
	
	public void on_link_up(int now, int iface) {
		trace(now,iface+" link up");
	}
	
	public void on_link_down(int now, int iface) {
		trace(now,iface+" link down");
	}
	
	public void on_receive(int now, Packet p, int iface) {
		trace(now,"received control packet");
	}
	
	public void forward_packet(int now, Packet p, int iface) {
		if ( iface == LOCAL && links[0].isUp() ) { // locally sent packet
		    // always sends a copy, not the Packet object itself
		    nodeObj.send(p.getCopy(),0);
			trace(now, "forwarded a locally sent packet");
			return;
		} 
		if ( iface == LOCAL && ! links[0].isUp() ) {
			nodeObj.send(p,UNKNOWN);
			trace(now, "network interface is down");
			return;
		}
		// allows the node to count dropped packets
		nodeObj.send(p,UNKNOWN);
	}

	public void showControlState(int now) {
		trace(now,"has no state to show");
	}
	
	public void showRoutingTable(int now) {
		trace(now,"has no routing table to show");
	}

	// auxiliary methods
	
	private void trace (int now, String msg) {
		if ( tracingOn ) System.out.println("-- trace: "+name+" time "+now+" node "+nodeId+" "+msg);
	}
}
```

Class *FloodingSwitch* is another example of a control algorithm. This one implements a flooding switch and therefore can only be used in a network without cycles. Most of its upcall methods are identical to the ones of the previous algorithm but the upcall *forward_packet*. All identical methods are not shown.

Method `initialise()` does not tests the number of interfaces of the node and also `returns 0`. The upcall `forward_packet()` does the flood, i.e. it sends a copy of the packet to all interfaces whose state is *up* but the one from which the packet came. 

Both control algorithms shown always send a copy of the packet to be forwarded, not the packet object itself. Sending the object may introduce hard to debubg errors. That would be more error prone in this case since the algorithm implements a real flood.

If this control algorithm is used in a network with cycles, a broadcast storm of duplicte packets would arise. The EndSystem control algorithm only forwards locally originated packets (`interface == LOCAL`) and therefore drops packets received by the node whose destination is not the node.

```java

public class FloodingSwitch implements ControlAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private GlobalParameters parameters;
	private Link[] links;
	private int numInterfaces;
	private String name = "flooding switch control: ";
	private boolean tracingOn = false;

	public FloodingSwitch() {
	}

	public int initialise(int now, int node_id, Node mynode, GlobalParameters parameters, Link[] links, int nint) {
		nodeId = node_id;
		nodeObj = mynode;
		this.parameters = parameters;
		this.links = links;
		numInterfaces = nint;
		return 0;
	}

	public void forward_packet(int now, Packet p, int iface) {
		int copiesSent = 0;
		// do the flood
		for (int i = 0; i < links.length; i++) {
			if (i != iface && links[i].isUp()) {
			    // always send a copy of p, not the object itself
			    nodeObj.send(p.getCopy(), i);
			    copiesSent++;
			}
		}
		if (copiesSent == 0) { // allows the node to count dropped packets
			nodeObj.send(p, UNKNOWN);
		}
		trace(now, "forwarded " + copiesSent + " packet copies");
	}

}
```
Class `EmptyApp()` is not shown. This application algorithm does nothing and is provided in the library to be used as application algorithm of switches that run no application code, i.e. all upcalls are empty.

Below, the result of the simulation is shown. 

```
java -cp bin cnss.simulator.Simulator configs/simpleNet.config.txt 
Loading configuration : configs/simpleNet.config.txt
Reading file configs/simpleNet.config.txt
Created Node 0: 2 interf.s, ctr code: cnss.library.FloodingSwitch app code: cnss.library.EmptyApp
Created Node 1: 1 interf.s, ctr code: cnss.library.EndSystemControl app code: cnss.examples.Sender
Created Node 2: 1 interf.s, ctr code: cnss.library.EndSystemControl app code: cnss.examples.Receiver
Added link to node 0 - Link (Node1:0 I1:0)<-->(Node2:1 I2:0) bwd: 1000000 bps lat: 50 ms error %: 0.0 jit %: 0.0 up
Added link to node 1 - Link (Node1:0 I1:0)<-->(Node2:1 I2:0) bwd: 1000000 bps lat: 50 ms error %: 0.0 jit %: 0.0 up
Added link to node 0 - Link (Node1:0 I1:1)<-->(Node2:2 I2:0) bwd: 1000000 bps lat: 50 ms error %: 0.0 jit %: 0.0 up
Added link to node 2 - Link (Node1:0 I1:1)<-->(Node2:2 I2:0) bwd: 1000000 bps lat: 50 ms error %: 0.0 jit %: 0.0 up

simulation starts - first processing step with clock = 0

log: sender time 0 node 1 starting pings
log: receiver time 0 node 2 started listening
log: sender time 1000 node 1 sent ping packet n. 1 - src 1 dst 2 type DATA ttl 32 seq 1 size 26
log: receiver time 1100 node 2 receiver received "ping 1"
log: sender time 1200 node 1  received reply "receiver received "ping 1""
log: sender time 2000 node 1 sent ping packet n. 2 - src 1 dst 2 type DATA ttl 32 seq 2 size 26
log: receiver time 2100 node 2 receiver received "ping 2"
log: sender time 2200 node 1  received reply "receiver received "ping 2""
log: sender time 3000 node 1 sent ping packet n. 3 - src 1 dst 2 type DATA ttl 32 seq 3 size 26
log: receiver time 3100 node 2 receiver received "ping 3"
log: sender time 3200 node 1  received reply "receiver received "ping 3""
log: sender time 4000 node 1 sent ping packet n. 4 - src 1 dst 2 type DATA ttl 32 seq 4 size 26
log: receiver time 4100 node 2 receiver received "ping 4"
log: sender time 4200 node 1  received reply "receiver received "ping 4""
log: sender time 5000 node 1 sent ping packet n. 5 - src 1 dst 2 type DATA ttl 32 seq 5 size 26
log: receiver time 5100 node 2 receiver received "ping 5"
log: sender time 5200 node 1  received reply "receiver received "ping 5""
log: sender time 6000 node 1 sent ping packet n. 6 - src 1 dst 2 type DATA ttl 32 seq 6 size 26
log: receiver time 6100 node 2 receiver received "ping 6"
log: sender time 6200 node 1  received reply "receiver received "ping 6""
log: sender time 7000 node 1 sent ping packet n. 7 - src 1 dst 2 type DATA ttl 32 seq 7 size 26
log: receiver time 7100 node 2 receiver received "ping 7"
log: sender time 7200 node 1  received reply "receiver received "ping 7""
sender received 7 replies to pings
receiver replyed to 7 ping messages
Pkt stats for node 1 :  s 7 r 7 d 0 f 0
(Node1:0 I1:0) s 7 r 7<-->(Node2:1 I2:0) s 7 r 7
Pkt stats for node 2 :  s 7 r 7 d 0 f 0
(Node1:0 I1:1) s 7 r 7<-->(Node2:2 I2:0) s 7 r 7
log: sender time 8000 node 1 sent ping packet n. 8 - src 1 dst 2 type DATA ttl 32 seq 8 size 26

simulation ended - last processing step with clock = 8000
```

In the iorst lines, the processing of the configuration file contents is shown, namely the creation of nodes and the instalation of links. Now, the simulation starts. At the first clock tick a first packet is sent, 100 ms later it gets to the destination, the receiver replies and more 100 ms later the reply gets to the sender. This is so, because transmition time is negligible and only the links latency accounts for the end to end transit time. The packet has a size of 26 bytes: 20 for the header as in IP, and 6 to represent the value of the counter (as a character string). The origin and destination nodes, as well as the sequence numbers and the original value of the TTL are also shown.

At the last processing step, when the value of the clock is 8000, nodes reveive the events written in the configuration file. Only the sender  and the receiver show their state, since the switch has nothing to show. After, all nodes print the number of packets sent and received as well as the number of packets sent and received through each of its links. The sender and receiver nodes sent and received 7 packets, and these packets were also sent and received by their links. It is also noted that these links never droped or forwarded packets. It is interesting to note that the sender still sends a new packet during this processing step, but as this happens after the execution of the show status and stats upcalls, this packet is not considered by nodes and links counters.

As the reader can realize, the shown algorithms are build as classes that implement the `ApplicationAlgorithm` and the `ControlAlgorithm` interfaces. That way the code shows all the required details. Package `library` also contains two abstract classes, `AbstractApplicationAlgorithm()` and   `AbstractControlAlgorithm()`. Theses classe may be used to write application and control algorithms that extend them, what results in a more concise code style. For example, using the `AbstractApplicationAlgorithm` the `Sender()` class could become (without counting the number of packets sent and ignoring the replies):

```java
import java.util.Arrays;
import cnss.simulator.*;
import cnss.utils.*;

public class Sender extends AbstractApplicationAlgorithm {

  public SenderNode() {
      super(true, "sender");
  }

  public int initialise(int now, int node_id, Node self, String[] args) {
    super.initialise(now, node_id, self, args);
		return 1000;
	}

  public void on_clock_tick(int now) {
      self.send(self.createDataPacket( 1, new byte[0]));
  }
} 
```





