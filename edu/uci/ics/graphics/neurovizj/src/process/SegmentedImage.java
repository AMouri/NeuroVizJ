package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.List;
import java.util.Set;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Wrapper for the cells present in an image
 * @author Alec
 *
 */
public class SegmentedImage {
	private List<ProcessedCell> cells;
	
	/**
	 * Constructs with a segmented image, the original image, and the list of cells
	 * @param ip
	 * @param orig
	 * @param cells
	 */
	public SegmentedImage(List<ProcessedCell> cells){
		this.cells = cells;
	}
	
	/**
	 * Processes the image located at path with the given segmentator
	 * @param image
	 * @param segmentator
	 */
	public SegmentedImage(String image, Segmentator segmentator){
		ImagePlus segmented = segmentator.segment(image);
		if(segmented != null){
			this.cells = ProcessedCell.findCells(new ImagePlus(image), segmented, image);
		} else {
			this.cells = null;
		}
	}
	
	/**
	 * Retrieves the list of cells in the image
	 * @return
	 */
	public List<ProcessedCell> getCells(){
		return cells;
	}
	
	/**
	 * Returns whether the segmented image able to be segmented properly
	 * @return
	 */
	public boolean isValid(){
		return cells != null;
	}
	
	/**
	 * Sets the successor to the ith cell to c
	 * @param i
	 * @param c
	 */
	public void setSuccessor(int i, ProcessedCell c){
		getCell(i).setNextCell(c);
	}
	
	/**
	 * Gets the ith cell in the image
	 * @param i
	 * @return
	 */
	public ProcessedCell getCell(int i){
		return cells.get(i);
	}
	
	/**
	 * Gets the area of the ith cell
	 * @param index
	 * @return
	 */
	public int getArea(int i){
		return getCell(i).getArea();
	}
	
	/**
	 * Gets the mean BW intensity of the ith cell
	 * @param i
	 * @return
	 */
	public double getMean(int i){
		return getCell(i).getMean();
	}
	
	/**
	 * Gets the boundary box of the ith cell
	 * @param i
	 * @return
	 */
	public BoundaryBox getBB(int i){
		return getCell(i).getBB();
	}
	
	/**
	 * Gets the centroid of the ith cell
	 * @param i
	 * @return
	 */
	public Point getCentroid(int i){
		return getCell(i).getCentroid();
	}
	
	/**
	 * Gets the point set mask of the ith cell
	 * @param i
	 * @return
	 */
	public Set<Point> getPointSet(int i){
		return getCell(i).getPointSet();
	}
	
	/**
	 * Gets the number of cells in the image
	 * @return
	 */
	public int numCells(){
		return cells.size();
	}
	
	/**
	 * Prints data for each cell in the image.
	 */
	public void printCells(){
		for(ProcessedCell cell : cells){
			System.out.println(cell);
		}
	}
	
	
}
