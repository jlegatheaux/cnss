package cnss.examples;

import cnss.simulator.ControlAlgorithm;
import cnss.simulator.GlobalParameters;
import cnss.simulator.Link;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class FloodingSwControl implements ControlAlgorithm {
	
	private Node nodeObj;
	private int nodeId;
	private GlobalParameters parameters;
	private Link[] links;
	private int numInterfaces;
	private String name="flooding switch control: ";
	private boolean tracingOn = false;

	public FloodingSwControl() {
		
	}

	public int initialise(int now, int node_id, Node mynode, GlobalParameters parameters, Link[] links, int nint) {
		nodeId = node_id;
		nodeObj = mynode;
		this.parameters=parameters;
		this.links=links;
		numInterfaces=nint;
		return 0;
	}
	
	
	public void on_clock_tick(int now) {
		trace(now,"clock tick");
	}
	
	public void on_timeout(int now) {
		trace(now,"timeout");
	}
	
	public void on_link_up(int now, int iface) {
		trace(now,iface+" link up");
	}
	
	public void on_link_down(int now, int iface) {
		trace(now,iface+" link down");
	}
	
	public void on_receive(int now, Packet p, int iface) {
		trace(now,"received control packet");
	}
	
	public void forward_packet(int now, Packet p, int iface) {
		int copiesSent = 0;
		for (int i=0; i<links.length; i++) {
			if ( i != iface ) {
				if ( copiesSent == 0 ) {
					nodeObj.send(p,i);
					copiesSent++;
				}
				else {
					Packet copy = p.getCopy();
					nodeObj.send(copy,i);
					copiesSent++;
				}
			}
		}
		trace(now, "forwarded "+copiesSent+" packet copies");
	}

	public void showControlState(int now) {
		trace(now,"has no state to show");
	}
	
	public void showRoutingTable(int now) {
		trace(now,"has no routing table to show");
	}

	// auxiliary methods
	
	private void trace (int now, String msg) {
		if ( tracingOn ) System.out.println("-- trace: "+name+" time "+now+" node "+nodeId+" "+msg);
	}



}

