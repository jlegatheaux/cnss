package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class SimpleReceiver implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;

	private String name = "simple receiver";
	private boolean logOn = true;

	public SimpleReceiver() {
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
		log(now, "received app packet " + p);
		String m = name + " received message \"" + new String(p.getPayload()) + "\"";
		log(now, m);
		// Reply to sender
		DataPacket reply = nodeObj.createDataPacket(p.getSource(), m.getBytes());
		nodeObj.send(reply);
	}

	public void showState(int now) {
		log(now, "no state to show");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
