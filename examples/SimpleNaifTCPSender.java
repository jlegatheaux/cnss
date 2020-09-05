package cnss.examples;

import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class SimpleNaifTCPSender implements ApplicationAlgorithm {

	private Node nodeObj;
	private int nodeId;
	private String[] args;
	private String name = "simple naif TCP sender";
	private boolean logOn = true;

	int counter = 0;
	int total = 0;
	int congWindow = 1;
	int sentInWindow = 0;
	int acksReceived = 0;

	public SimpleNaifTCPSender() {
	}

	public int initialise(int now, int node_id, Node mynode, String[] args) {
		nodeId = node_id;
		nodeObj = mynode;
		this.args = args;
		total = Integer.parseInt(args[0]);
		log(0, "starting with the transfer of " + total + " packets");
		return 1;
	}

	public void on_clock_tick(int now) {
		if (sentInWindow < congWindow && counter < total) {
			// send one more packet
			counter++;
			sentInWindow++;
			DataPacket msg = nodeObj.createDataPacket(2, new byte[1000]);
			log(now, "sent packet " + counter);
			nodeObj.send(msg);
		}
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}

	public void on_receive(int now, Packet p) {
		log(now, "received ack message \"" + new String(p.getPayload()) + "\"");
		acksReceived++;
		if (acksReceived < total) {
			if (sentInWindow == congWindow) {
				sentInWindow = 0;
				congWindow = congWindow * 2;
			}
		}
		if (acksReceived == total && counter == total)
			log(now, "all messages transfered");
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
