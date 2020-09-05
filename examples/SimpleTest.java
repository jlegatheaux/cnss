package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class SimpleTest implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;

	private String name = "simple test";
	boolean logingOn = true;

	public SimpleTest() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;
		log(0, "starting");

		if (nodeId == 1) { // sender to 2
			DataPacket p = nodeObj.createDataPacket(2, new byte[1000]);
			log(0, "sent 2 packets of size " + p.getSize());
			nodeObj.send(p);
			nodeObj.send(p);
			nodeObj.set_timeout(200);
			return 0;
		}

		if (nodeId == 2) { // receiver from 1
			nodeObj.set_timeout(200);
			return 0;
		}

		if (nodeId == 3) { // sender to 4
			DataPacket p = nodeObj.createDataPacket(4, new byte[10000]);
			log(0, "sent 2 packets of size " + p.getSize());
			nodeObj.send(p);
			nodeObj.send(p);
			return 0;
		}

		if (nodeId == 4) {
			nodeObj.set_timeout(450);
			return 500;
		}

		return 0;
	}

	public void on_clock_tick(int now) {
		log(now, "received clock tick");
		nodeObj.set_timeout(450);
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, Packet p) {
		log(now, "received app packet " + p);

	}

	public void showState(int now) {
		log(now, "no state to show");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logingOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
