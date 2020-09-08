# CNSS Overview ( Computer Networks Simple Simulator )

CNSS is written in Java and has been developed for teaching purposes.  

CNSS was inspired by a simulator, developed around 2001, by Adam Greenhalgh from the University College of London. This simulator is
mainly intended for testing routing algorithms based on the distance vector principle, and is limited to networks with zero transit
time links. 
 
CNSS is capable of simulating any routing algorithm, as well as simple applications running on heterogeneous nodes. To that end, CNSS leverages a more realistic notion of link, characterised by transmission and propagation delays as well as and error rate. Morevoer, in CNSS a network can be comprised of different types of nodes, capable of executing user provided control and application algorithms, in a more generic fashion. To allow a link to simulate a logical link made of a set of links and packet switches, it is also possible to introduce jitter in a link if one so desires.

## CNSS in short

CNSS simulates a packet switched network where each processing step is **instantaneous**, i.e., executed without advancing the virtual clock, while communications make that clock advance. Virtual time resolution is 1 millisecond, i.e., the clock advances 1 by 1 ms. All nodes and links update their state in a lock step way, according to a logically, perfectly synchronised global notion of time. Their state changes from a state to the following by executing processing steps.

The main concepts of the simulator are the following:

A network is composed of a set or mix of nodes, interconnected by links. 

Nodes exchange packets. 

In each clock tick, all nodes execute a processing step by consuming the events scheduled to them for this processing step. These events may be clock ticks, alarms or packet receptions and eliveries to teh node. The execution of a processing step in each node may trigger future events like alarms, delivery of messages to the same or other nodes, etc.

Nodes execute a common kernel that triggers processing steps execution using up calls to the methods of two algorithms defined by the users: a control algorithm and an applications algorithm. These algorithms may be different in each node. These up calls processing steps can send messages, set alarms, etc. The system imposes that the execution of each node is structured as the execution of two automata: a control automaton controlled by the control algorithm, and an application automaton controlled by the application algorithm.

Network configuration is defined by a configuration file using simple commands to add nodes and links and their parameters, as well as special events that can be triggered at defined processing steps or time values.

The real time progress of the simulation is driven by the time required to execute the nodes processing steps, therefore, nodes processing steps cannot execute blocking actions, which would block the simulator. Reading and writing local files is accdeptable as well as any other quick execution method calss, but using Java calls like Thread.sleep() or any kind of synchronization is fully discouraged.

Next, packets, nodes, links and the configuration file are presented in more detail.

## Packets

Packets are objects of the class *Packet*. This class has several subclasses among which *DataPacket* and *ControlPacket* that represent different types of packets. Data packets are sent and received by the ApplicationAlgorithm part of the nodes. Control packets are sent and received by the ApplicationAlgorithm part of the nodes. In fact, these two types of packets could also be understood as CNSS supporting two kinds of IP ports by analogy. Packets have several fields, namely: 

```java
protected int src;  // the initial sending node
protected int dst;  // the destination node
protected int ttl;  // packets time-to-live
protected int type; // data, control, ...
protected int seq;  // sequence number
protected int size; // size of the packet including the payload size
protected byte[] payload; // the payload of the packet
``` 

Some contants in the Packet class have speacial meaning for the CNSS notion of Packet: *HEADERSIZE = 20* is the size of the header to mirror IPv4 packets size and *INITIALTTL = 32* is the default value of packets TTL. 

## Nodes

Nodes execute two algorithms, an application algorithm and a control algorithm. Each of these algorithms is structured as an automaton executing actions associated with a pre-defined set of events, each one called an **upcall**.

Among the most important upcalls are: *initialise(int now, ...)*, *on_clock_tick(int now)*, *on_receive(int now, Packet p)*, *on_timeout(int now)* and several others. Nodes automata may choose to use *clock_ticks*, if so, their periodic value, in millisecends, is the result of the *initialise(...)* upcal. If the *initialise* method returns 0, no *clock_ticks* will be delivered to that algorithm.

Each upcall, but the *initialise* one, is triggered by the delivery of an event that got to the node. All events that should be triggered in the same processig step (charcaterized by the value of the *clock*) are delivered in sequence without any predefineded order specified. 

The definition of the interfaces of the two algorithms executed by a node are presented below.

## ApplicationAlgorithm Interface

The ApplicationAlgorithm interface should be implemented by any class whose instances are intended to implement application automata executed by nodes. The class that implements this algorithm must have a zero argument constructor. All methods have as first argument, the virtual time of the processing step where the event fired. Methods:

```java
public int initialise(int now, int node_id, Node nodeObj, String[] args);
```

Initializes the application algorithm or automaton and returns the desired *control_clock_tick_period*. If *control_clock_tick_period == 0*, no **clock_ticks** will be submitted to the algorithm.

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

Prints application state table(s) to the screen in a previously agreed format.

The node processing steps application algorithm can use public methods of the class DataPacket as well as the following down calls:

```java
nodeObj.createDataPacket (int receiver, byte[] payload)
nodeObj.send(DataPacket p)
nodeObj.set_timeout(int t)
```

When a packet is directly created, its sequence number is 0. In order to guarantee that packet sequence numbers are different (relative to each node), packets must be created using *nodeObj.createDataPacket(…)* method, which take care of providing unique sequence numbers.


## ControlAlgorithm Interface

The ControlAlgorithm interface should be implemented by any class whose instances are intended to implement control automata executed by nodes, for example, the way the node routes packets not directed to himself. The ControlAlgorithm interface should be implemented by any class whose instances are intended to implement the application part of the node. The class that implements this algorithm must have a zero argument constructor. All methods have as first argument the virtual time of the processing step where the event fired. First are apresented the interface constants, followed by the methods.

```java
static final int LOCAL = Node.LOCAL;     // the number of the virtual loop back interface
static final int UNKNOWN = Node.UNKNOWN;   // an unknown interface - means do not know how
static final int INFINITY = 60;  // just a suggestion
```

```java
public int initialise(int now, int node_id, Node nodeObj, GlobalParams parameters, Link[] links, int nint);
```	
Initializes the control algorithm and returns the desired *control_clock_tick_period*. If *control_clock_tick_period == 0*, no **clock_ticks** will be submitted to the algorithm. Interfaces are numbered 0 to *nint-1*. Each has a link attached: *links[i]*. Interface *LOCAL* with value -1 is virtual and denotes, when needed, the local loop interface. 

Parameters: *id* is this node id, *nodeObj* is a reference to the node object executing this algorithm, *parameters* is the collection of global parameters (see the configuration file section), *links* is the nodes links array, *nint* is the number of interfaces (or links) of this node, and the method must return its requested *clock_tick_period* value. 

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
```

Signals a link up event. Parameter: *iface* the interface (link) that changed state.

```java
public void on_link_down(int now, int iface);
```
	
Signals a link down event. Parameter: *iface* the interface (link) that changed state.


```java
public void showControlState(int now);
```

Prints control algorithm state table(s) to the screen in a previously agreed format.

```java
public void showRoutingTable(int now);
```

Prints control algorithm routing table to the screen in a previously agreed format.

These up calls are called at each time step where there is an event directed to this node.

The node processing steps control algorithm can use the following down calls:

```java
nodeObj.send(DataPacket p)
nodeObj.control_send(Packet p, int iface)
nodeObj.set_control_timeout(int t)

nodeObj.createDataPacket (int receiver, byte[] payload)
nodeObj.createControlPacket (int sender, int receiver, byte[] payload)
nodeObj.getInterfaceWeight(int iface, int type)
nodeObj.getInterfaceState(int iface)
```

The node control algorithm processing steps or *upcalls* can also use the public methods of the following objects:

```java
class Packet
class Link (*)
class Node (*)
class Parameters (*)
```

(*) Use only public methods reading the state of these objects.

When a packet is created its sequence number is 0. In order to guarantee that packet sequence numbers are different (relative to each node), packets must be created using *nodeObj.createDataPacket(…)* and *nodeObj.createControlPacket(...)* methods, which take care of providing unique sequence numbers. Therefore, this algorithm must avoid creating packets directly when maintaining packets sequence numbers uniqueness is important.

## Links

Todos os links são ponto a ponto (com duas extremidades – extremidade 1 e 2) que ligam entre si duas interfaces de nós (em princípio distintos). Os links são caracterizados por:

Estado – up ou down
Débito – em bps
Latência (tempo de propagação) – ms ms 
Error rate - % 
Jitter - %

Cada extremidade tem uma fila de espera de entrada e outra de saída (in e out). No final da execução de cada processing step dos nós, todos os pacotes que estes enviaram estão depositados nas filas de espera de out dos seus links e são gerados eventos Packet Delivery correspondentes calculando o tempo de trânsito para a travessar o link.

## Network and simulation configuration file

A rede é configurada através de um ficheiro de texto passado em parâmetro da chamada do simulador.

java -cp bin cnss.simulator.Simulafor config.txt

O tempo virtual é em ms e começa em 0. Não existe limitação à duração excepto Integer.MAXVALUE em ms. São cerca de 2 milhões de segundos ou 33333 segundos ou quase 10 horas.

O tempo de execução dos nós é sempre desprezável ou 0 ms em virtual time.

O ficheiro passado em parâmetro pode ter várias linhas. A sintaxe das indicações de configuração é a seguinte:

1)	parameter name value 

defines a global parameter of name "name" and value “value” (both are strings); global parameters are accessible to nodes’ ControlAlgorithms.

Examples:
parameter stop 100000  

max duration of simulation in virtual ms; it is good practice to make this the first line of the config file; this first parameter is recognized by the simulator. Other examples:

parameter splithorizon true
parameter expiration true

which can be used to parametrize the control algorithm.

2)	node node_id #interfaces name_of_control_class name_of_application_[class args …]

node ids must start at 0
args are accessible to the node application algorithm

Example:

Node 1 5 FloodSwitchAlgorithm  EmptyAppAlgorithm hello world


3)	link side1_node.side1_interface side2_node.side2_interface bandwidth latency errors [ state ]

Link ids must start in 0 and follow a strict increasing order in each node;
all links are full point2point and symmetric, bandwidth is in bps, 
latency in ms, errors is a %, jitter in % (of latency); if state is not "down", all other values are considered as equivalent to "up".  By the moment, most errors in the logic of nodes and their links are only detected in simulation time by the way of raised exceptions.

Eventos criados a partir do ficheiro de configuração

O ficheiro de configuração pode conter diretivas para gerar eventos. Sintaxe genérica e exemplos:


event_name time_of_event event_parameters

	 traceroute 12000 from_node destination_node
	 dumproute 1000 [ all | node id ]
	 dumpPacketStats 120000 [ all | node id ]
	 uplink / downlink 18000 link_origin link_destination
	 dumpcontrolstate 8000 [ all | node id ]
	 dumpappstate 8000 [ all | node id ]

