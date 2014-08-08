package edu.uci.ics.graphics.neurovizj.src.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import matlabcontrol.MatlabProxy;
import matlabcontrol.extensions.MatlabTypeConverter;

/**
 * Implementation of a tracker geared towards the tracking of neurological stem cells
 * @author Alec
 *
 */
public class Tracker {
	
	private Segmentator segmentator;
	
	/**
	 * Contrusts a tracker instance with matlab links
	 * @param proxy
	 * @param processor
	 */
	public Tracker(MatlabProxy proxy, MatlabTypeConverter processor){
		this.segmentator = new Segmentator(proxy, processor);
	}
	
	/**
	 * Tracks a sequence of images in a folder. Each image will be found by a header name,
	 * and appending "t", followed by a number, denoting the position of the image in the
	 * sequence.
	 * 
	 * First performs segmentation, then performs several tracking procedures.
	 * @param folderName
	 * @param headerName
	 * @return
	 */
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
			if(segmented[i].isValid() && segmented[i+1].isValid()){
				match(segmented[i], segmented[i+1]);
			}
		}
		
		return segmented;
		
	}
	
	/**
	 * Gets the sequence number of the image
	 * @param header
	 * @param name
	 * @return
	 */
	private int getId(String header, String name){
		return Integer.parseInt(name.replace(header + "t", "").replace(".TIF", ""));
	}
	
	/**
	 * Matches cells from image A to image B using the Hungarian algorithm
	 * If ambiguity is detected, handles them (WIP)
	 * @param A
	 * @param B
	 */
	private void match(SegmentedImage A, SegmentedImage B){
//		int numNodes = Math.max(A.numCells(), B.numCells()) + 3;
		
		double[][] costs = assignCosts(A, B);
		
		HungarianAlgorithm hungarian = new HungarianAlgorithm(costs);
		
		int[] assigns = hungarian.execute();
		
		for(int i = 0; i < A.numCells(); i++){
			A.setSuccessor(i, (assigns[i] == -1 || assigns[i] >= B.numCells()) ? null : B.getCell(assigns[i]));
		}
		
	}
	
	/**
	 * Will handle potential conflicts, both by using other image sets in the data and by prompting
	 * a user to distinguish between cells
	 * @param costs
	 * @param assigns
	 * @param A
	 * @param B
	 */
	private void handlePotentialConflicts(double[][] costs, int[] assigns, SegmentedImage A, SegmentedImage B){
		
		//TODO: test some constants
		List<MergeCell> merges = detectPotentialMerge(costs, assigns, A, B, .9);
		List<SplitCell> splits = detectPotentialSplit(costs, assigns, A, B, .9);
		//TODO: finish method
	}
	
	/**
	 * Detects potential conflicts where multiple cells in frame i match to a cell in frame i+1.
	 * Proposed method is, after running the Hungarian algorithm on A and B, find all ratios of the
	 * assignment found and possible assignments, and return conflicts if the ratio is close to 1.
	 * @param costs
	 * @param assigns
	 * @param A
	 * @param B
	 * @param ep
	 * @return
	 */
	private List<MergeCell> detectPotentialMerge(double[][] costs, int[] assigns, SegmentedImage A, SegmentedImage B, double ep){
		List<MergeCell> result = new ArrayList<MergeCell>();
		for(int j = 0; j < costs[0].length; j++){
			List<ProcessedCell> temp = new ArrayList<ProcessedCell>();
			int index = -1;
			for(int k = 0; k < costs.length; k++){
				if(assigns[k] == j){
					index = j;
					break;
				}
			}
			if(index == -1) continue;
			double weight = costs[index][j];
			
			for(int i = 0; i < costs.length; i++){
				if(weight/costs[i][j] >= ep){
					temp.add(A.getCell(i));
				}
			}
			if(temp.size() > 1){
				result.add(new MergeCell(temp, B.getCell(j)));
			}
		}
		return result;
	}
	
	/**
	 * Detects potential conflicts where a cell in frame i matches to multiple cells in frame i+1.
	 * Proposed method is, after running the Hungarian algorithm on A and B, find all ratios of the
	 * assignment found and possible assignments, and return conflicts if the ratio is close to 1.
	 * @param costs
	 * @param assigns
	 * @param A
	 * @param B
	 * @param ep
	 * @return
	 */
	private List<SplitCell> detectPotentialSplit(double[][] costs, int[] assigns, SegmentedImage A, SegmentedImage B, double ep){
		List<SplitCell> result = new ArrayList<SplitCell>();
		for(int i = 0; i < costs.length; i++){
			List<ProcessedCell> temp = new ArrayList<ProcessedCell>();
			if(assigns[i] == -1) continue;
			double weight = costs[i][assigns[i]];
			for(int j = 0; i < costs[0].length; j++){
				if(weight/costs[i][j] >= ep){
					temp.add(B.getCell(j));
				}
			}
			if(temp.size() > 1){
				result.add(new SplitCell(A.getCell(i), temp));
			}
		}
		return result;
	}
	
	/**
	 * Generates a cost matrix for assignments from A to B
	 * @param A
	 * @param B
	 * @return
	 */
	private double[][] assignCosts(SegmentedImage A, SegmentedImage B){
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

/**
 * Wrapper for a File with its sequence number
 * @author Alec
 *
 */
class FileWrapper implements Comparable<FileWrapper>{

	private File file;
	private int id;
	
	/**
	 * Constructs a FileWrapper
	 * @param file
	 * @param id
	 */
	public FileWrapper(File file, int id){
		this.file = file;
		this.id = id;
	}
	
	/**
	 * For Comparable interface
	 */
	public int compareTo(FileWrapper o){
		return id - o.getId();
	}
	
	/**
	 * Returns the file's sequence number
	 * @return
	 */
	public int getId(){
		return id;
	}
	
	/**
	 * Returns the file itself
	 * @return
	 */
	public File getFile(){
		return file;
	}
	
	/**
	 * Returns the name of the file
	 * @return
	 */
	public String getName(){
		return file.getName();
	}
}


/* There exists too different types of ambiguity between images i and i+1:
 * 1. multiple cells in image i have close weights to 1 cell in image i+1
 * 2. 1 cell in image i have close weights to multiple cells in image i+1
 */

/**
 * Wrapper for storing merge conflicts
 * @author Alec
 *
 */
class MergeCell {
	public List<ProcessedCell> src;
	public ProcessedCell dst;
	
	/**
	 * Constructs a potential conflict with a src list mapping to a destination cell
	 * @param src
	 * @param dst
	 */
	public MergeCell(List<ProcessedCell> src, ProcessedCell dst){
		this.src = src;
		this.dst = dst;
	}
}

/**
 * Wrapper for storing split conflicts
 * @author Alec
 *
 */
class SplitCell {
	public ProcessedCell src;
	public List<ProcessedCell> dst;
	
	/**
	 * Constructs a potential conflict with a src cell mapping to a destination list
	 * @param src
	 * @param dst
	 */
	public SplitCell(ProcessedCell src, List<ProcessedCell> dst){
		this.src = src;
		this.dst = dst;
	}
}
