package PathFindingDectection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import GraphConstruction.AirwayEdge;
import GraphConstruction.AirwayNode;
import GraphConstruction.Graph;

public class SuspiciousNodeDectector {
	AirwayNode node;
	double narrowingScore;
	double curvatureScore;
	double branchingScore;
	double totalScore;
	boolean isSuspicious;
	List<String> reasons;
	
	private AirwayNode getNeighbour(AirwayEdge edge, AirwayNode current) {
	    if (edge.getFrom().equals(current)) {
	        return edge.getTo();
	    }
	    return edge.getFrom();
	}
	
	private double clamp(double value) {
	    return Math.max(0.0, Math.min(1.0, value));
	}
	
	private double curvatureAngle(AirwayNode prev, AirwayNode curr,AirwayNode next) {
		double angle = 0.0;
		double u1 = curr.getCol() - prev.getCol();
		double u2 = curr.getRow() - prev.getRow();
		
		double v1 = next.getCol() - curr.getCol();
		double v2 = next.getRow() - curr.getRow();
		
		double dot = (u1 * v1) + (u2 * v2);
		
		double mag1 = Math.sqrt(v1 * v1 + v2 * v2);
		double mag2 = Math.sqrt(u1 * u1 + u2 * u2);;
		
		double cosTheta = dot/(mag1*mag2);
		
		cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
		angle = Math.toDegrees(Math.acos(cosTheta));
		return angle;
	}
	private double scoreCurvature(double angle) {
		double score = angle/180.0;
		return clamp(score);
	}
	
	public List<Double> computeCurveScore(List<AirwayNode> path){
		List<Double> scores = new ArrayList<Double>();
		for(int i=0;i<path.size();i++) {
			if(i==0 || i==path.size()-1) {
				scores.add(0.0);
				continue;
			}
			AirwayNode prev = path.get(i-1);
			AirwayNode curr = path.get(i);
			AirwayNode next = path.get(i+1);
			
			double angle = curvatureAngle(prev,curr,next);
			double score = scoreCurvature(angle);
			scores.add(score);
			System.out.println("Node (" + curr.getRow() + "," + curr.getCol() + 
		            ") angle=" + angle + " score=" + score);
		}
		return scores;
		
	}
	
	public List<Double> computeNarrowingScore(List<AirwayEdge> edges){
		List<Double> scores = new ArrayList<>();
		for(AirwayEdge edge:edges) {
			double score = computeEdgeNarrowing(edge);
			scores.add(score);
		}
		return scores;
	}
	private double computeEdgeNarrowing(AirwayEdge edge) {
		List<Double> widths = edge.getLocalWidths();
		if(widths==null || widths.size()<3) {
			return 0.0;
		}
		double avg = edge.getAverageWidth();
		double min = edge.getMinWidth();
		double absoluteScore = 0.0;
		if(avg>0) {
			absoluteScore = 1.0 - (min/avg);
			
		}
		double maxDrop = 0.0;
		double maxRise = 0.0;
		for(int i=1;i<widths.size();i++) {
			double prev = widths.get(i-1);
			double curr = widths.get(i);
			if(prev<=0) {
				continue;
			}
			double change = (prev-curr)/prev;
			if(Math.abs(change)<0.1) continue;
			if(change >0) {
				
				if(change>maxDrop) {
					maxDrop = change;
				}
				
			}else {
				double rise = -change;
				if(rise > maxRise) {
					maxRise = rise;
				}
			}
		}
		double instability = maxRise*0.5;
		double finalScore = Math.max(absoluteScore, maxDrop+instability);
		return Math.max(0.0, Math.min(1.0, finalScore));
	}
	
	public List<AirwayEdge> extractPathEdges(Graph graph,List<AirwayNode> path){
		List<AirwayEdge> edges = new ArrayList<AirwayEdge>();
		for(int i=0;i<path.size()-1;i++) {
			AirwayNode a = path.get(i);
			AirwayNode b = path.get(i+1);
			for(AirwayEdge edge:graph.getConnectedEdges(a)) {
				if((edge.getFrom().equals(a) && edge.getTo().equals(b)) || (edge.getFrom().equals(b)&&edge.getTo().equals(a))) {
					edges.add(edge);
					break;
				}
			}
		}
		return edges;
	}
	
	public Map<AirwayNode, Double> mapNarrowingToNodes(
	        List<AirwayNode> path,
	        List<AirwayEdge> edges,
	        List<Double> edgeScores
	) {

	    Map<AirwayNode, Double> nodeScores = new HashMap<>();

	    // initialize all nodes
	    for (AirwayNode node : path) {
	        nodeScores.put(node, 0.0);
	    }

	    for (int i = 0; i < edges.size(); i++) {

	        AirwayEdge edge = edges.get(i);
	        double score = edgeScores.get(i);

	        AirwayNode from = edge.getFrom();
	        AirwayNode to = edge.getTo();

	        // assign max score to both nodes
	        nodeScores.put(from, Math.max(nodeScores.get(from), score));
	        nodeScores.put(to, Math.max(nodeScores.get(to), score));
	    }

	    return nodeScores;
	}
	
	public Map<AirwayNode,Double> combineSuspiciousScores(
			List<AirwayNode> path,
			List<Double> curvatureScores,
			Map<AirwayNode,Double> narrowNodeScores,
			double curvatureWeight,
			double narrowWeight){
		Map<AirwayNode,Double> combined = new HashMap<>();
		for(int i=0;i<path.size();i++) {
			AirwayNode node = path.get(i);
			double curvature = (i < curvatureScores.size()) ? curvatureScores.get(i) : 0.0;
            double narrowing = narrowNodeScores.getOrDefault(node, 0.0);
            double total = (curvatureWeight * curvature) + (narrowWeight * narrowing);
            combined.put(node, Math.max(0.0, Math.min(1.0, total)));
		}
		return combined;
	}
	 
	 public String buildReason(
	            AirwayNode node,
	            List<AirwayNode> path,
	            List<Double> curvatureScores,
	            Map<AirwayNode, Double> narrowingNodeScores,
	            double curvatureThreshold,
	            double narrowingThreshold
	    ) {
	        int index = path.indexOf(node);

	        double curvature = 0.0;
	        if (index >= 0 && index < curvatureScores.size()) {
	            curvature = curvatureScores.get(index);
	        }

	        double narrowing = narrowingNodeScores.getOrDefault(node, 0.0);

	        List<String> reasons = new ArrayList<>();

	        if (curvature >= curvatureThreshold) {
	            reasons.add("high curvature=" + curvature);
	        }

	        if (narrowing >= narrowingThreshold) {
	            reasons.add("narrowing=" + narrowing);
	        }

	        if (reasons.isEmpty()) {
	            reasons.add("low-risk");
	        }

	        return String.join(", ", reasons);
	    }
	 	public Map<AirwayNode, Double> computeFullGraphCurvatureScores(Graph graph) {
		    Map<AirwayNode, Double> scores = new HashMap<>();

		    for (AirwayNode node : graph.getNodes()) {
		        List<AirwayEdge> connected = graph.getConnectedEdges(node);

		        if (connected == null || connected.size() != 2) {
		            scores.put(node, 0.0);
		            continue;
		        }

		        AirwayNode prev = getNeighbour(connected.get(0), node);
		        AirwayNode next = getNeighbour(connected.get(1), node);

		        double angle = curvatureAngle(prev, node, next);
		        double score = scoreCurvature(angle);
		        scores.put(node, score);
		    }

		    return scores;
		}
	 	public Map<AirwayNode, Double> computeFullGraphNarrowingScores(Graph graph) {
	 	    Map<AirwayNode, Double> nodeScores = new HashMap<>();

	 	    for (AirwayNode node : graph.getNodes()) {
	 	        nodeScores.put(node, 0.0);
	 	    }

	 	    for (AirwayEdge edge : graph.getEdges()) {
	 	        double score = computeEdgeNarrowing(edge);

	 	        AirwayNode from = edge.getFrom();
	 	        AirwayNode to = edge.getTo();

	 	        nodeScores.put(from, Math.max(nodeScores.get(from), score));
	 	        nodeScores.put(to, Math.max(nodeScores.get(to), score));
	 	    }

	 	    return nodeScores;
	 	}
	 	public Map<AirwayNode, Double> combineFullGraphScores(
	 	        Graph graph,
	 	        Map<AirwayNode, Double> curvatureScores,
	 	        Map<AirwayNode, Double> narrowingScores,
	 	        double curvatureWeight,
	 	        double narrowingWeight
	 	) {
	 	    Map<AirwayNode, Double> combined = new HashMap<>();

	 	    for (AirwayNode node : graph.getNodes()) {
	 	        double curvature = curvatureScores.getOrDefault(node, 0.0);
	 	        double narrowing = narrowingScores.getOrDefault(node, 0.0);

	 	        double total = (curvatureWeight * curvature) + (narrowingWeight * narrowing);
	 	        combined.put(node, clamp(total));
	 	    }

	 	    return combined;
	 	}
	 	public List<AirwayNode> findSuspiciousNodes(
	 	        Map<AirwayNode, Double> combinedScores,
	 	        double threshold
	 	) {
	 	    List<AirwayNode> suspicious = new ArrayList<>();

	 	    for (Map.Entry<AirwayNode, Double> entry : combinedScores.entrySet()) {
	 	        if (entry.getValue() >= threshold) {
	 	            suspicious.add(entry.getKey());
	 	        }
	 	    }

	 	    return suspicious;
	 	}
	 	public String buildFullGraphReason(
	 	        AirwayNode node,
	 	        Map<AirwayNode, Double> curvatureScores,
	 	        Map<AirwayNode, Double> narrowingScores,
	 	        double curvatureThreshold,
	 	        double narrowingThreshold
	 	) {
	 	    List<String> reasons = new ArrayList<>();

	 	    double curvature = curvatureScores.getOrDefault(node, 0.0);
	 	    double narrowing = narrowingScores.getOrDefault(node, 0.0);

	 	    if (curvature >= curvatureThreshold) {
	 	        reasons.add("curvature");
	 	    }

	 	    if (narrowing >= narrowingThreshold) {
	 	        reasons.add("narrowing");
	 	    }

	 	    if (reasons.isEmpty()) {
	 	        reasons.add("general score");
	 	    }

	 	    return String.join(" + ", reasons);
	 	}
}
