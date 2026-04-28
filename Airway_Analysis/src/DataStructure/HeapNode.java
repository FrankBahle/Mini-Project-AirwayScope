package DataStructure;

import GraphConstruction.AirwayNode;

public class HeapNode {
	private AirwayNode node;
	private double cost;
	HeapNode leftHeap;
	HeapNode rightHeap;
	
	public HeapNode(AirwayNode n,double c) {
		node=n;
		cost=c;
		leftHeap=null;
		rightHeap=null;
	}
	
	public AirwayNode getAirWayNode() {
		return node;
	}
	
	public void setAirWayNode(AirwayNode awNode) {
		node = awNode;
	}
	public double getCost() {
		return cost;
	}
	
	public void setCost(double c) {
		cost=c;
	}
}
