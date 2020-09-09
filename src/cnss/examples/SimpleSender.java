package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class SimpleSender implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;

	private String name = "simple sender";
	private boolean logOn = true;

	public SimpleSender() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;
		log(0, "starting");
		// my specific code
		byte[] message = ("hello I am the " + name).getBytes();
		DataPacket p = nodeObj.createDataPacket(2, message);
		log(0, "sent packet " + p);
		nodeObj.send(p);

		DataPacket p1 = nodeObj.createDataPacket(3, new byte[10000]);
		nodeObj.send(p1);
		log(0, "sent big packet " + p1);

		message = ("hello I am the " + name + " sending a third packet").getBytes();
		p = nodeObj.createDataPacket(2, message);
		log(0, "sent packet " + p);
		nodeObj.send(p);

		return 0;
	}

	public void on_clock_tick(int now) {
		log(now, "clock tick");
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, Packet p) {
		log(now, " received echoed message \"" + new String(p.getPayload()) + "\"");
	}

	public void showState(int now) {
		System.out.println(name + " has no state to show");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
