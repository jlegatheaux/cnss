package cnss.examples;

// the control (routing) of an end system with one only interface

import cnss.simulator.ControlAlgorithm;
import cnss.simulator.GlobalParameters;
import cnss.simulator.Link;
import cnss.simulator.Node;
import cnss.simulator.Packet;

public class EndSystemControl implements ControlAlgorithm {
	
	private Node nodeObj;
	private int nodeId;
	private GlobalParameters parameters;
	private Link[] links;
	private int numInterfaces;
	private String name="end system - ";
	private boolean tracingOn = false;

	public EndSystemControl() {
		
	}

	public int initialise(int now, int node_id, Node mynode, GlobalParameters parameters, Link[] links, int nint) {
		if ( nint > 1 ) {
			tracingOn = true;
			trace(now,"end system has more than one interface");
			System.exit(-1);
		}
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
		if ( iface == LOCAL ) { // locally sent packet
			nodeObj.send(p,0);
			trace(now, "forwarded a locally sent packet");
			return;
		} 
		if ( iface == 0 && p.getDestination() != nodeId ) {
			trace(now,"received a packet for another node - ignore it");
			nodeObj.send(p,UNKNOWN);
			return;
		}
		// just to help debugging purposes
		tracingOn = true; 
		trace(now,"this should not happen");
		nodeObj.send(p,UNKNOWN);
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

