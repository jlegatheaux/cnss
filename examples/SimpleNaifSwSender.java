package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class SimpleNaifSwSender implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;
	private String name = "simple naif sw sender";
	private boolean logOn = true;

	int counter = 0;
	int total = 0;

	public SimpleNaifSwSender() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;
		total = Integer.parseInt(args[0]);
		log(0, "starting");
		// send the first packet
		DataPacket msg = nodeObj.createDataPacket(2, new byte[1000]);
		log(now, "sent one packet of size " + msg.getSize());
		nodeObj.send(msg);
		counter = 1;
		return 0;
	}

	public void on_clock_tick(int now) {
		log(now, "clock tick");
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, Packet p) {
		log(now, " received ack message \"" + new String(p.getPayload()) + "\"");
		if (counter < total) {
			// send one more packet
			DataPacket msg = nodeObj.createDataPacket(2, new byte[1000]);
			log(now, "received one ack, sent one more packet of size " + msg.getSize());
			nodeObj.send(msg);
			counter++;
		}
		if (counter == total) {
			// send one more packet
			log(now, "received last ack message");
		}
	}

	public void showState(int now) {
		System.out.println(name + " sent and received ack of " + counter + " messages");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
