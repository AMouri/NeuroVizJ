package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;

/**
 * Contains important attributes about a cell
 * @author Alec
 *
 */
public class ProcessedCell {
	
	private String imgName;
	private ImageProcessor origImg;
	private int id;
	private BoundaryBox bb;
	
	private ProcessedCell nextCell;
	private ProcessedCell prevCell;
	private static String[] attributes = {"Image", "Id", "Area", "Mean", "Centroid", "Prev", "Next"};
	
	private int area;
	private double mean;
	private Point centroid;
	private Set<Point> points;
	
	/**
	 * Makes a cell
	 * @param ip
	 * @param num
	 * @param boundary
	 * @param bb
	 * @param area
	 * @param mean
	 * @param centroid
	 */
	public ProcessedCell(String name, ImageProcessor origImg, ImageProcessor segmented, int num, 
			BoundaryBox bb, int area, double mean, Point centroid){
		imgName = name;
		this.id = num;
		this.bb = bb;
		this.area = area;
		this.mean = mean;
		this.centroid = centroid;
		
		origImg.setRoi(this.bb.getX(), this.bb.getY(), this.bb.getWidth(), this.bb.getHeight());
		this.origImg = origImg.crop();
		origImg.resetRoi();
		
		this.points = new HashSet<Point>();
		for(int i = bb.getX(); i < bb.getX() + bb.getWidth(); i++){
			for(int j = bb.getY(); j < bb.getY() + bb.getHeight(); j++){
				if(segmented.get(i,j) == id){
					this.points.add(new Point(i,j));
				}
//				this.points.add(new Point(i,j));
			}
		}
	}
	
	/**
	 * Finds all cells inside a segmented image
	 * @param ip
	 * @return
	 */
	public static List<ProcessedCell> findCells(ImagePlus orig, ImagePlus segmented, String imgName){
		ImageProcessor masked = orig.getProcessor();
		ImageProcessor threshSeg = segmented.getProcessor();
		int width = segmented.getWidth();
		int height = segmented.getHeight();
		Wand wand = new Wand(threshSeg);
		List<ProcessedCell> cells = new LinkedList<ProcessedCell>();
		HashSet<Integer> ids = new HashSet<Integer>();
		ids.add(0);
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				int id = threshSeg.get(i,j);
				if(!ids.contains(id)){
					ids.add(id);
					wand.autoOutline(i, j, 0.0, Wand.EIGHT_CONNECTED);
					Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
					orig.setRoi(roi);
					ResultsTable rt = new ResultsTable();
					Analyzer analyzer = new Analyzer(orig, 
							Measurements.AREA | Measurements.RECT | Measurements.CENTER_OF_MASS | Measurements.MEAN, rt);
					analyzer.measure();
					cells.add(new ProcessedCell(imgName, masked, threshSeg, id, 
							new BoundaryBox(rt, 0), (int) rt.getValue("Area", 0), rt.getValue("Mean", 0), 
							new Point((int) rt.getValue("XM", 0), (int) rt.getValue("YM", 0))));
					orig.restoreRoi();
				}
			}
		}
		
		return cells;
	}
	
	/**
	 * Gets the cell area
	 * @return
	 */
	public int getArea(){
		return area;
	}
	
	public int getId(){
		return id;
	}
	
	/**
	 * Gets the mean BW intensity of the cell
	 * @return
	 */
	public double getMean(){
		return mean;
	}
	
	/**
	 * Gets the boundary box of the cell
	 * @return
	 */
	public BoundaryBox getBB(){
		return bb;
	}
	
	/**
	 * Gets the cell's centroid
	 * @return
	 */
	public Point getCentroid(){
		return centroid;
	}
	
	/**
	 * Gets the point-set hash of the cell
	 * @return
	 */
	public Set<Point> getPointSet(){
		return new HashSet<Point>(points);
	}
	
	/**
	 * Gets the cell's successor
	 * @return
	 */
	public ProcessedCell getNextCell(){
		return nextCell;
	}
	
	/**
	 * Gets the cell's ancestor
	 * @return
	 */
	public ProcessedCell getPrevCell(){
		return prevCell;
	}
	
	/**
	 * Sets the cell's successor to c
	 * @param c
	 */
	public void setNextCell(ProcessedCell c){
		nextCell = c;
		if(c != null && c.getPrevCell() != this){
			c.setPrevCell(this);
		}
	}
	
	/**
	 * Sets the cell's ancestor to c
	 * @param c
	 */
	public void setPrevCell(ProcessedCell c){
		prevCell = c;
		if(c != null && c.getNextCell() != this){
			c.setNextCell(this);
		}
	}
	
	public static String[] getTags(){
		return attributes;
	}
	
	public String getAttribute(String attr){
		switch(attr){
			case "Image":
				return imgName;
			case "Id":
				return Integer.toString(id);
			case "Area":
				return Integer.toString(area);
			case "Mean":
				return Double.toString(mean);
			case "Centroid":
				return centroid.toString();
			case "Prev":
				return (prevCell != null) ? Integer.toString(prevCell.getId()) : "None";
			case "Next":
				return (nextCell != null) ? Integer.toString(nextCell.getId()) : "None";
			default:
				return "";
		}
	}
	
	public String toString(){
		return "Cell #" + id + " in image " + imgName + " at " + centroid + "becomes Cell #" + 
				((nextCell != null) ? nextCell.id : "END");
	}
}
