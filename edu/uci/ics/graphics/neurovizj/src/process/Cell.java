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
public class Cell {
	
	private String imgName;
	private ImageProcessor origImg;
	private ImageProcessor segImg;
	private int id;
	private BoundaryBox bb;
	
	private Cell nextCell;
	private Cell prevCell;
	
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
	public Cell(String name, ImageProcessor origImg, ImageProcessor segmented, int num, 
			BoundaryBox bb, int area, double mean, Point centroid){
		imgName = name;
		this.origImg = origImg;
		this.segImg = segmented;
		this.id = num;
		this.bb = bb;
		this.area = area;
		this.mean = mean;
		this.centroid = centroid;
		
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
	public static List<Cell> findCells(ImagePlus orig, ImagePlus segmented, String imgName){
		ImageProcessor masked = orig.getProcessor();
		ImageProcessor threshSeg = segmented.getProcessor();
		int width = segmented.getWidth();
		int height = segmented.getHeight();
		Wand wand = new Wand(threshSeg);
		List<Cell> cells = new LinkedList<Cell>();
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
					cells.add(new Cell(imgName, masked, threshSeg, id, 
							new BoundaryBox(rt, 0), (int) rt.getValue("Area", 0), rt.getValue("Mean", 0), 
							new Point((int) rt.getValue("XM", 0), (int) rt.getValue("YM", 0))));
					orig.restoreRoi();
				}
			}
		}
		
		return cells;
	}
	
	public int getArea(){
		return area;
	}
	
	public double getMean(){
		return mean;
	}
	
	public BoundaryBox getBB(){
		return bb;
	}
	
	public Point getCentroid(){
		return centroid;
	}
	
	public Set<Point> getPointSet(){
		return new HashSet<Point>(points);
	}
	
	public Cell getNextCell(){
		return nextCell;
	}
	
	public Cell getPrevCell(){
		return prevCell;
	}
	
	public void setNextCell(Cell c){
		nextCell = c;
		if(c != null && c.getPrevCell() != this){
			c.setPrevCell(this);
		}
	}
	
	public void setPrevCell(Cell c){
		prevCell = c;
		if(c != null && c.getNextCell() != this){
			c.setNextCell(this);
		}
	}
	
	
	public String toString(){
		return "Cell #" + id + " in image " + imgName + " at " + centroid + "becomes Cell #" + 
				((nextCell != null) ? nextCell.id : "END");
	}
}
