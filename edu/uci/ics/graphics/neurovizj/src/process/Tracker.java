package edu.uci.ics.graphics.neurovizj.src.process;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import matlabcontrol.MatlabProxy;
import matlabcontrol.extensions.MatlabTypeConverter;

public class Tracker {
	
	private Segmentator segmentator;
	
	public Tracker(MatlabProxy proxy, MatlabTypeConverter processor){
		this.segmentator = new Segmentator(proxy, processor);
	}
	
	public SegmentedImage[] track(String folderName, String headerName){
		//perform tracking
		final File folder = new File(folderName);
		File[] folderFiles = folder.listFiles();
		FileWrapper[] files = new FileWrapper[folderFiles.length];
		SegmentedImage[] segmented = new SegmentedImage[folderFiles.length];
		System.out.println("Retrieving files");
		for(int i = 0; i < folderFiles.length; i++){
			files[i] = new FileWrapper(folderFiles[i], getId(headerName, folderFiles[i].getName()));
		}
		Arrays.sort(files);
		for(int i = 0; i < files.length; i++){
			System.out.println("Segmenting " + files[i].getName());
			segmented[i] = new SegmentedImage(folderName + "\\" + files[i].getName(), segmentator);
		}
		
		for(int i = 0; i < files.length - 1; i++){
			System.out.println("Matching " + files[i].getName() + " and " + files[i+1].getName());
			match(segmented[i], segmented[i+1]);
		}
		
		return segmented;
		
	}
	
	public int getId(String header, String name){
		return Integer.parseInt(name.replace(header + "t", "").replace(".TIF", ""));
	}
	
	public void match(SegmentedImage A, SegmentedImage B){
//		int numNodes = Math.max(A.numCells(), B.numCells()) + 3;
		
		double[][] costs = assignCosts(A, B);
		
		HungarianAlgorithm hungarian = new HungarianAlgorithm(costs);
		
		int[] assigns = hungarian.execute();
		
		for(int i = 0; i < A.numCells(); i++){
			A.setSuccessor(i, (assigns[i] == -1 || assigns[i] >= B.numCells()) ? null : B.getCell(assigns[i]));
		}
		
	}
	
	public double[][] assignCosts(SegmentedImage A, SegmentedImage B){
		double[][] costs = new double[A.numCells()+3][B.numCells()+3];
		double minWeight = 5*100*100;
		for(int i = 0; i < A.numCells() + 3; i++){
			for(int j = 0; j < B.numCells() + 3; j++){
				if (i >= A.numCells() || j >= B.numCells()){
					costs[i][j] = minWeight;
				} else {
					double aArea = A.getArea(i);
					double bArea = B.getArea(j);
					double aMean = A.getMean(i);
					double bMean = B.getMean(j);
					Point aCentroid = A.getCentroid(i);
					Point bCentroid = B.getCentroid(j);
					Set<Point> intersect = A.getPointSet(i);
					intersect.retainAll(B.getPointSet(j));
					
					double areaFact = (aArea - bArea)/Math.max(aArea, bArea);
					double meanFact = (aMean - bMean)/Math.max(aMean, bMean);
					double xFact = aCentroid.getX() - bCentroid.getX();
					double yFact = aCentroid.getY() - bCentroid.getY();
					double intersectFact = 1 - intersect.size() /Math.max(aArea, bArea);
					costs[i][j] = Math.pow(areaFact, 2) + Math.pow(meanFact, 2) + 
							Math.pow(xFact, 2) + Math.pow(yFact, 2) + Math.pow(intersectFact, 2);
				}
			}
		}
		return costs;
	}
	
}

class FileWrapper implements Comparable<FileWrapper>{
	
	private File file;
	private int id;
	
	public FileWrapper(File file, int id){
		this.file = file;
		this.id = id;
	}
	
	public int compareTo(FileWrapper o){
		return id - o.getId();
	}
	
	public int getId(){
		return id;
	}
	
	public File getFile(){
		return file;
	}
	
	public String getName(){
		return file.getName();
	}
}
