package edu.uci.ics.graphics.neurovizj.src.process;

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
	private ImageProcessor orig;
	private List<Cell> cells;
	
	public SegmentedImage(ImageProcessor ip, ImageProcessor orig, List<Cell> cells){
		this.ip = ip;
		this.orig = orig;
		this.cells = cells;
	}
	
	public SegmentedImage(String image, Segmentator segmentator){
		this.orig = new ImagePlus(image).getProcessor();
		ImagePlus segmented = segmentator.segment(image);
		this.ip = segmented.getProcessor();
		this.cells = Cell.findCells(new ImagePlus(image), segmented, image);
	}
	
	public ImageProcessor getImage(){
		return ip;
	}
	
	public ImageProcessor getOrig(){
		return orig;
	}
	
	public List<Cell> getCells(){
		return cells;
	}
	
	public void setSuccessor(int i, Cell c){
		getCell(i).setNextCell(c);
	}
	
	public Cell getCell(int i){
		return cells.get(i);
	}
	
	public int getArea(int index){
		return getCell(index).getArea();
	}
	
	public double getMean(int index){
		return getCell(index).getMean();
	}
	
	public BoundaryBox getBB(int index){
		return getCell(index).getBB();
	}
	
	public Point getCentroid(int index){
		return getCell(index).getCentroid();
	}
	
	public Set<Point> getPointSet(int index){
		return getCell(index).getPointSet();
	}
	
	public int numCells(){
		return cells.size();
	}
	
	public void printCells(){
		for(Cell cell : cells){
			System.out.println(cell);
		}
	}
	
	
}
