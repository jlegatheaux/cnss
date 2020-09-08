package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class SimpleBulkSender implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;
	private String name = "simple bulk sender";
	private boolean logOn = true;

	private int total;

	public SimpleBulkSender() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;
		total = Integer.parseInt(args[0]);
		log(0, "starting");
		return 1;
	}

	public void on_clock_tick(int now) {
		if (now <= total) {
			// send one more packet
			DataPacket p = nodeObj.createDataPacket(2, new byte[1000]);
			log(now, "sent one packet of size " + p.getSize());
			nodeObj.send(p);
		}
		if (now == 2000) {
			// send one only packet
			DataPacket p = nodeObj.createDataPacket(2, new byte[total * 1000]);
			log(now, "sent one packet of size " + p.getSize());
			nodeObj.send(p);
		}
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, Packet p) {
		log(now, " received echoed message \"" + new String(p.getPayload()) + "\"");
	}

	public void showState(int now) {
		log(now, "has no state to show");
	}

	// auxiliary methods

	private void log(int now, String msg) {
		if (logOn)
			System.out.println("log: " + name + " time " + now + " node " + nodeId + " " + msg);
	}

}
