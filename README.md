# CNSS Overview ( Computer Networks Simple Simulator )

CNSS is written in Java and has been developed for teaching purposes.  

CNSS was inspired by a simulator, developed around 2001, by Adam Greenhalgh from the University College of London. This simulator was
mainly intended for testing routing algorithms based on the distance vector principle, and is limited to networks with zero transit
time links. 
 
CNSS is capable of simulating any routing algorithm, as well as simple applications running on heterogeneous nodes. To that end, CNSS leverages a more realistic notion of link, characterised by transmission and propagation delays as well as and error rate. Morevoer, in CNSS a network can be comprised of different types of nodes, capable of executing user provided control and application algorithms, in a more generic fashion. To allow a link to simulate a logical link made of a set of links and packet switches, it is also possible to introduce jitter in a link if one so desires.

## CNSS in short

CNSS simulates a packet switched network where each processing step is **instantaneous**, i.e., executed without advancing the virtual clock, while communications make that clock advance. Virtual time resolution is 1 millisecond, i.e., the clock advances 1 by 1 ms. All nodes and links update their state in a lock step way, according to a logically, perfectly synchronised global notion of time. Their state changes from a state to the following one by executing processing steps. The main concepts of the simulator are presented next.

A network is composed of a set or mix of nodes, interconnected by links. 

Nodes exchange packets. 

In each clock tick, all nodes execute a processing step by consuming the events scheduled to them for this processing step. These events may be clock ticks, alarms or packet deliveries to the node. The execution of a processing step in each node may generate future events like alarms, delivery of messages to the same or other nodes, etc.

Nodes execute a common kernel that triggers processing steps execution using up calls to the methods of two algorithms defined by the users: a control algorithm and an applications algorithm. These algorithms may be different in each node. These up calls processing steps can send messages, set alarms, etc. to the nodes algorithms. The system imposes that the execution of each node is structured as the execution of two automata: a control automaton controlled by the control algorithm, and an application automaton controlled by the application algorithm.

Network configuration is defined by a configuration file using simple commands to add nodes and links and their parameters, as well as special events that can be triggered at configuration defined processing steps or time values.

The real execution progress of the simulation is bound to the time required to execute the nodes processing steps. Therefore, nodes processing steps cannot execute blocking actions, which would block the simulator. Reading and writing local files is accdeptable as well as any other quick execution method calls, but using Java calls like Thread.sleep() or any kind of synchronization is fully discouraged.

Next, packets, nodes, links and the configuration file are presented in more detail.

## Packets

Packets are objects of the class *Packet*. This class has several subclasses among which *DataPacket* and *ControlPacket* that represent different types of packets. Data packets are sent and received by the ApplicationAlgorithm part of the nodes. Control packets are sent and received by the ControlAlgorithm part of the nodes. In fact, by analogy, these two types of packets could also be understood as if CNSS supports two fixed IP ports. One addressing a traditional OS kernel, and the other addressing the single application running in the node. Packets have several fields, namely: 

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

Among the most important upcalls are: *initialise(int now, ...)*, *on_clock_tick(int now)*, *on_receive(int now, DataPacket p)*, *on_timeout(int now)* and several others. Nodes automata may choose to use *clock_ticks*, if so, their periodic value in millisecends, is the result of the *initialise(...)* upcal. If the *initialise* method returns 0, no *clock_ticks* will be delivered to that algorithm.

Each upcall, but the *initialise* one, is triggered by the delivery of an event that got to the node. All events that should be triggered in the same processig step (characterized by the value of the *clock*) are delivered in sequence without any predefineded specified order. 

The definition of the interfaces of the two algorithms executed by a node are presented below.

## ApplicationAlgorithm Interface

The ApplicationAlgorithm interface should be implemented by any class whose instances are intended to implement application automata executed by nodes. The class that implements this algorithm must have a zero argument constructor. All methods have, as first argument, the virtual time of the processing step where the event fired. Methods:

```java
public int initialise(int now, int node_id, Node nodeObj, String[] args);
```

Initializes the application algorithm or automaton and returns the desired *control_clock_tick_period*. If *control_clock_tick_period == 0*, no **clock_ticks** will be delivered to the algorithm.

Parameters: *id* is this node id, *nodeObj* is a reference to the node object executing this algorithm, *args* is an array of arguments specified in the *configuration file*  (see the configuration file section). 

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

Given a data packet from another node, process it. Parameter: *p* the received packet.

```java
public void showState(int now);
```

Prints application state table(s) to the screen in a users previously agreed format.

The node processing steps application algorithm can use public methods of the class DataPacket as well as the following down calls:

```java
nodeObj.createDataPacket (int destination, byte[] payload)
nodeObj.send(DataPacket p)
nodeObj.set_timeout(int t)
```

When a packet is directly created, its sequence number is 0. In order to guarantee that packet sequence numbers are different (relative to each node), packets must be created using *nodeObj.createDataPacket(…)* method, which takes care of providing unique sequence numbers.


## ControlAlgorithm Interface

The ControlAlgorithm interface should be implemented by any class whose instances are intended to implement a control automata executed by nodes, for example, the way the node routes packets not directed to himself. The ControlAlgorithm interface should be implemented by any class whose instances are intended to implement the application part of the node. The class that implements this algorithm must have a zero argument constructor. All methods have as first argument the virtual time of the processing step where the event fired. First are apresented one of the interface constants then the methods.

```java
static final int LOCAL = Node.LOCAL;       // the number of the virtual loop back interface
static final int UNKNOWN = Node.UNKNOWN;   // means an unknown interface 
```

```java
public int initialise(int now, int node_id, Node nodeObj, GlobalParams parameters, Link[] links, int nint);
```	
Initializes the control algorithm and returns the desired *control_clock_tick_period*. If *control_clock_tick_period == 0*, no **clock_ticks** will be submitted to the algorithm. Interfaces are numbered 0 to *nint-1*. Each has a link attached: *links[i]*. Interface *LOCAL* with value -1 is virtual and denotes, when needed, the local loop interface. 

Parameters: *id* is this node id, *nodeObj* is a reference to the node object executing this algorithm, *parameters* is the collection of global parameters (see the configuration file section), *links* is the nodes links array, *nint* is the number of interfaces (or links) of this node. The method must return its requested *clock_tick_period* value. 

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

Given a control packet from another node, process it. Parameter: *p* the packet received.

	
```java
public void forward_packet(int now, Packet p, int iface);
```

Given a packet from another node, forward it to the appropriate interfaces by using the downcall *nodeObj.control_send(Packet p, int iface)*. Parameters are: *p* is the packet to forward, *iface* is the interface where this node received that packet.
	

```java
public void on_link_up(int now, int iface);
public void on_link_down(int now, int iface);
```

Signals a link up or down event. Parameter: *iface* the interface (link) that changed state.


```java
public void showControlState(int now);
```

Prints control algorithm state table(s) to the screen in a previously user agreed format.

```java
public void showRoutingTable(int now);
```

Prints control algorithm routing table to the screen in a previously user agreed format.

These up calls are called at each time step where there is a correspondeng event directed to this node.

The node processing steps control algorithm can use the following down calls:

```java
nodeObj.send(DataPacket p)
nodeObj.control_send(Packet p, int iface)
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

(*) Never use public methods taht write the state of these objects.

When a packet is created its sequence number is 0. In order to guarantee that packet sequence numbers are different (relative to each node), packets must be created using *nodeObj.createDataPacket(…)* and *nodeObj.createControlPacket(...)* methods, which take care of providing unique sequence numbers. Therefore, this algorithm must avoid creating packets directly when maintaining packets sequence numbers uniqueness is important.

## Links

The model of links is very simple: a link is point to point (connects exactly two nodes) and has two extremes, end 1 and end 2 each directly connected to one interface in a different node. Links are charactetized by:

```java
private long bwidth = 1000;  // in bits per second - bps
private int latency = 0;     // in ms
private double errors = 0.0; // error rate in % - 0.0 is a perfect (no errors) link
private double jitter = 0.0; // in % - 0.0 is a link without jitter
private boolean up;   
```

Besides these variables, lins have two queues in each end: an *out queue* or output queue, and an *in queue* or input queue. At the end of each processing step, packets queued in the *in queues* of all links are consumed and become *delivery* events, associated with the other extreme of the link, to be delivered when the corresponding transit time ends. Trsnit times are computed using the time required to transmit the packet as well as those in front of it in the same *out queue*, added to the propagation time.

## Network definitian and simulation configuration file

To start a simulation, a *configuration file* must be given as parameter.

```
java -cp bin cnss.simulator.Simulafor config.txt
```
Virtual time is in milliseconds, starts at 0 and ends at value that can be changed in the configuration file. Its limit is *Integer.MAXVALUE* which corresponds to around 2000000 (two million) seconds or more or less 10 hours of virtual time.

### The configuration file is made of lines

These lines obey a simple syntax. In the current version tokens must be separated by exactly one space chracter. The different possible configuration file lines are the following.

```
parameter name value 
```

Defines a global parameter of name _name_ and value _value_ (both are character strings without any blanck character in the middle); global parameters are accessible to nodes’ ControlAlgorithms as a collection of name / value pairs accessible via an hash map.

Examples:
```
parameter stop 100000  
```
This parameter defines the duration of simulation in virtual ms; it is good practice to make this the first line of the config file; this first parameter is recognized by the simulator.

```
parameter splithorizon true
parameter expiration true
```

which can be used to parametrize the control algorithms.

```
node node_id #interfaces name_of_control_class name_of_application_[class args …]
```

Node ids must start at 0 and follow a strict increasing order. Args are accessible to the node application algorithm via a String[] args parameter of the initialise() method.

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

introduces a link from interface 0 of node 0 to interface 0 of node 1 with 10 Mbps bit rate, 0.0 error rate, 0.0 jitter strating in state down.

The configuration file can also introduce several type of events to be fired at given time step. The general syntax is *event_name time_of_event event_parameters*. Here are examples of the possible ones. 

```
traceroute 12000 origin_node destination_node
dumpappstate 8000 [ all | node id ]
dumproute 1000 [ all | node id ]
dumpPacketStats 120000 [ all | node id ]
uplink / downlink 18000 link_origin link_destination
dumpcontrolstate 8000 [ all | node id ]
```

The first one send a *tracing packet* at *time = 12000* from *from_node* to *destination_node*. Tracing packets are directly recognized by nodes and allow tracing the path from origin to destination in the current configuration, using the instantiated *control algorithms*.

The *dumpappstate* one delivers a *dumpappstate event* at *time = 8000* to the *Application Algorithm* of all nodes or to a specific one.

The *dumproute* one delivers a *dumproute event* at *time = 1000* to the *Control Algorithm* of all nodes or to a specific one.

The *dumpPacketsStats* one delivers a *dump packet statistics event* at *time = 120000* to all nodes or to a specific one.

The *uplink / downlink* ones delivers an *uplink event or a down link event* at *time = 18000* to the *Control Algorithm* of all nodes or to a specific one.

The *dumpcontrolstate* one delivers a *dumpcontrolstate event* at *time = 8000* to the *Control Algorithm* of all nodes or to a specific one.

Finally, a line strating with ´#´is considered a *comment*.

## Example of a configuration file

```
# Miscellaneous simple test

parameter stop 3000   # 3 seconds

node 0 4 cnss.examples.FloodingSwControl cnss.examples.EmptyApp hello world
node 1 1 cnss.examples.EndSystemControl cnss.examples.SimpleTest
node 2 1 cnss.examples.EndSystemControl cnss.examples.SimpleTest
node 3 1 cnss.examples.EndSystemControl cnss.examples.SimpleTest
node 4 1 cnss.examples.EndSystemControl cnss.examples.SimpleTest 

Link 0.0 1.0 1000000 50 0.0 0.0
Link 0.1 2.0 1000000 50 0.0 0.0
Link 0.2 3.0 1000000 50 0.0 0.0
Link 0.3 4.0 1000000 50 0.0 0.0

dumppacketstats 2900 all
```

This configuration file defines the star network decpited in the figura below. The configuration file schedules events *dumppcketstats* to all nodes at simultaion time = 2900. The code of the different classes is shown below, as well as the output of the simulation execution.






FIGURE

At time 0 the simulation starts and ends at time 3000.



