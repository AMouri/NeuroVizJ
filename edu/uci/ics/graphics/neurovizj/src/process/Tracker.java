package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.Set;

public class Tracker {
	
	public void track(String folder){
		//perform tracking
		//TODO
	}
	
	public int[] match(SegmentedImage A, SegmentedImage B){
//		int numNodes = Math.max(A.numCells(), B.numCells()) + 3;
		
		double[][] costs = assignCosts(A, B);
		
		HungarianAlgorithm hungarian = new HungarianAlgorithm(costs);
		
		int[] assigns = hungarian.execute();
		
		return assigns;
		
	}
	
	public double[][] assignCosts(SegmentedImage A, SegmentedImage B){
		double[][] costs = new double[A.numCells()][B.numCells()];
		for(int i = 0; i < A.numCells(); i++){
			for(int j = 0; j < B.numCells(); j++){
				double aArea = A.getArea(i);
				double bArea = B.getArea(i);
				double aMean = A.getMean(i);
				double bMean = B.getMean(i);
				Point aCentroid = A.getCentroid(i);
				Point bCentroid = B.getCentroid(i);
				Set<Point> intersect = A.getPointSet(i);
				intersect.retainAll(B.getPointSet(i));
				
				double areaFact = (aArea - bArea)/Math.max(aArea, bArea);
				double meanFact = (aMean - bMean)/Math.max(aMean, bMean);
				double xFact = aCentroid.getX() - bCentroid.getX();
				double yFact = aCentroid.getY() - bCentroid.getY();
				double intersectFact = 1 - intersect.size() /Math.max(aArea, bArea);
				costs[i][j] = Math.pow(areaFact, 2) + Math.pow(meanFact, 2) + 
						Math.pow(xFact, 2) + Math.pow(yFact,  2) + Math.pow(intersectFact, 2);
			}
		}
		return costs;
	}
	
}
