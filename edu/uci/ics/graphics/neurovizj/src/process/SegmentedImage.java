package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.List;

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
	
	
}
