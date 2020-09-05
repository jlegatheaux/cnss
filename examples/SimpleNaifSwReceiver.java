package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class SimpleNaifSwReceiver implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;

	private String name = "simple naif sw receiver";
	private boolean logOn = true;

	int counter = 0;

	public SimpleNaifSwReceiver() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;
		log(0, "starting");
		return 0;
	}

	public void on_clock_tick(int now) {
		log(now, "clock tick");

	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, Packet p) {
		log(now, name + " received message with " + p.getSize() + " bytes");
		DataPacket ack = nodeObj.createDataPacket(p.getSource(), ("ACK " + Integer.toString(counter)).getBytes());
		nodeObj.send(ack);
		log(now, "sent ack");
		counter++;
	}

	public void showState(int now) {
		log(now, "received and sent ack of " + counter + " messages");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
