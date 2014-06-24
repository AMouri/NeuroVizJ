package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Wrapper for the segmented image and the list of cells
 * @author Alec
 *
 */
public class SegmentedImage {
	private ImageProcessor ip;
	private List<Cell> cells;
	
	public SegmentedImage(ImageProcessor ip, List<Cell> cells){
		this.ip = ip;
		this.cells = cells;
	}
	
	public SegmentedImage(String image, Segmentator segmentator){
		ImagePlus segmented = segmentator.segment(image);
		this.ip = segmented.getProcessor();
		this.cells = Cell.findCells(new ImagePlus(image), segmented, image);
	}
	
	public ImageProcessor getImage(){
		return ip;
	}
	
	public List<Cell> getCells(){
		return cells;
	}
	
	public int getArea(int index){
		return cells.get(index).getArea();
	}
	
	public double getMean(int index){
		return cells.get(index).getMean();
	}
	
	public BoundaryBox getBB(int index){
		return cells.get(index).getBB();
	}
	
	public Point getCentroid(int index){
		return cells.get(index).getCentroid();
	}
	
	public Set<Point> getPointSet(int index){
		return cells.get(index).getPointSet();
	}
	
	public int numCells(){
		return cells.size();
	}
	
	
}
