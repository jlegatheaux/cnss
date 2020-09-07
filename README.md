## CNSS Overview ( Computer Networks Simple Simulator )

CNSS is written in Java and has been developed for teaching purposes.  

CNSS was inspired by a simulator, developed around 2001, by Adam Greenhalgh from the University College of London. This simulator is
mainly intended for testing routing algorithms based on the distance vector principle, and is limited to networks with zero transit
time links. 
 
CNSS is capable of simulating any routing algorithm, as well as simple applications running on heterogeneous nodes. To that end, CNSS leverages
a more realistic notion of link, characterised by transmission and propagation delays as well as **(jitter?)** and error rate. Morevoer,
in CNSS a network can be comprised of different types of nodes, capable of executing user provided control and application algorithms,
in a more generic fashion.




<!--- (While the initial version was only suitable to test routing algorithms based on the distance vector principle, with a notion of link that had 0 transit time, CNSS has a traditional notion of link characterised by transmission and propagation delays as well as error rate. The notion of network node is also more general. CNSS nodes execute processing steps of user provided control and application algorithms. With CNSS it is possible to simulate any routing algorithm as well as applications running on heterogeneous nodes.
-->

## CNSS in short

CNSS simulates a packet switched network where each processing step is **instantaneous**, i.e., executed without advancing the virtual clock, while communications make that clock advance. Virtual time resolution is 1 millisecond, i.e., the clock advances 1 by 1 ms. All nodes and links update their state in a lock step way, according to a logically, perfectly synchronised global notion of time. 

The main concepts of the simulator are the following:

A network is composed of a set or mix of nodes, interconnected by links. 

Nodes exchange packets. 

In each clock tick, all nodes execute a processing step by consuming the events scheduled to them for this processing step. These events may be clock ticks, alarms or packet receptions. The execution of a processing step in each node may trigger future events like alarms, delivery of messages to the same or other nodes, etc.

Nodes execute a common kernel that triggers processing steps execution using up calls to the methods of two algorithms defined by the users: a control algorithm and an applications algorithm. These algorithms may be different in each node. These up calls processing steps can send messages, set alarms, etc. The system imposes that the execution of each node is structured as the execution of two automata: a control automaton controlled by the control algorithm, and an application automaton controlled by the application algorithm.

Network configuration is defined by a configuration file using simple commands to add nodes and links and their parameters, as well as special events that can be triggered at defined processing steps or time values.

---

Next, nodes, links and the configuration file are presented in more detail.

Nodes

Um nó da rede executa um algoritmo de controlo e um algoritmo de aplicação. 

A execução de cada um desses algoritmos está estruturada como um autómato que reage aos seguintes eventos (upcalls):

Interface ControlAlgorithm

The ApplicationAlgorithm interface should be implemented by any class whose instances are intended to implement the application part running in nodes. The class that implements this algorithm must have a zero argument constructor. All methods have as first argument the virtual time of the processing step where the event fired. Constants:

static final int LOCAL = Node.LOCAL;     // the number of the virtual loop back interface
static final int UNKNOWN = Node.UNKNOWN;   // an unknown interface - means do not know how
static final int INFINITY = 60;  // just a suggestion
	
Methods:

public int initialise(int now, int node_id, Node nodeObj, GlobalParams parameters, Link[] links, int nint);
	
Initializes the control algorithm and returns the required control_clock_tick_period. If control_clock_tick_period == 0, no clock_ticks will be submitted. Interfaces are numbered 0 to nint-1. Each has a link attached: links[i]. Interface -1 is virtual and denotes, when needed, the local loop interface. Parameters: id - this node id, nodeObj - a reference to the node object executing this algorithm, parameters - the collection of global parameters, links - the nodes links array, nint - the number of interfaces (or links) of this node, and returns the requested clock_tick_period 

public void on_clock_tick(int now);

Signals a clock tick event.
	
public void on_timeout(int now);
	
Signals a timeout event.
public void on_receive(int now, Packet p);

Given a packet from another node, process it. Parameter: p the packet received.

public void on_link_up(int now, int iface);

Signals a link up event.

public void on_link_down(int now, int iface);
	
Signals a link down event.
	
public void forward_packet(int now, Packet p, int iface);

Given a packet from another node, forward it to the appropriate interfaces using: 
nodeObj.control_send(Packet p, int iface).
Packet ttl has already been decreased. Methods: p - the packet to process, iface - the interface it was received by the node.	

public void showControlState(int now);

Prints control algorithm state table(s) to the screen in a previously agreed format.

public void showRoutingTable(int now);

Prints control algorithm routing table to the screen in a previously agreed format.

These up calls are called at each time step where there is an event directed to this node.

The node processing steps control algorithm can use the following down calls:

nodeObj.send(DataPacket p)
nodeObj.control_send(Packet p, int iface)
nodeObj.set_control_timeout(int t)

nodeObj.createDataPacket (int receiver, byte[] payload)
nodeObj.createControlPacket (int sender, int receiver, byte[] payload)
nodeObj.getInterfaceWeight(int iface, int type)
nodeObj.getInterfaceState(int iface)


The node processing steps control algorithm can also use the public methods of the following objects:

	 class Packet
	 class Link (*)
	 class Node (*)
	 class Parameters (*)

(*) Use only methods reading state of these objects.

When a packet is created its sequence number is 0. In order to guarantee that packet sequence numbers are different for each node, packets must be created using nodeObj.createDataPacket(…) and nodeObj.createControlPacket methods, that take care of providing unique sequence numbers. Therefore, this algorithm must avoid creating packets directly when maintaining packets se.
Quince numbers uniqueness.

Interface ApplicationAlgorithm 

The ApplicationAlgorithm interface should be implemented by any class whose instances are intended to implement the application part running in nodes. The class that implements this algorithm must have a zero argument constructor. All methods have as first argument the virtual time of the processing step where the event fired. Methods:

public int initialise(int now, int node_id, Node nodeObj, String[] args);

Initialize the application algorithm and returns the required app_clock_tick_period. If app_clock_tick_period == 0, no clock_ticks will be submitted to the class. Parameters: id - this node id, nodeObj - a refrence to the node object executing this algorithm, args - the arguments of the application algorithm.

public void on_clock_tick(int now);

Signals a clock tick event.	

public void on_timeout(int now);
	
Signals a timeout event.

public void on_receive(int now, Packet p);

Given an application packet from another node, process it. Parameter: p the packet received.

public void showState(int now);

Prints application state table(s) to the screen in a previously agreed format.
	
These up calls are called at each processing step where there is an event directed to this node.

The node processing steps application algorithm can use the following down calls:

	 nodeObj.send(DataPacket p)
	 nodeObj.set_timeout(int t)
	 nodeObj.createDataPacket (int receiver, byte[] payload)

These processing steps can also access the public methods of class Packet.

Execução dos nós

Os nós também usar um relógio que provoca clock_ticks com a periodicidade desejada. Este relógio virtual avança em milissegundos (por causa da forma de cálculo do tempo de trânsito dos pacotes nos links). O seu valor é o resultado dos métodos initialise().

Sempre que numa dada processing step existem um ou mais eventos para um nó, estes são-lhe entregues em sequência (invocando sucessivamente on_*) por uma ordem em termo dos tipos dos eventos que é indefinida.

Os nós têm um certo número de interfaces às quais estão ligados links.

Conceito de link

Todos os links são ponto a ponto (com duas extremidades – extremidade 1 e 2) que ligam entre si duas interfaces de nós (em princípio distintos). Os links são caracterizados por:

Estado – up ou down
Débito – em bps
Latência (tempo de propagação) – ms ms 
Error rate - % 
Jitter - %

Cada extremidade tem uma fila de espera de entrada e outra de saída (in e out). No final da execução de cada processing step dos nós, todos os pacotes que estes enviaram estão depositados nas filas de espera de out dos seus links e são gerados eventos Packet Delivery correspondentes calculando o tempo de trânsito para a travessar o link.
Configuração da rede

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

