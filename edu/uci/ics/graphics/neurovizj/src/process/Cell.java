package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.Blitter;
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
	//private List<Point> boundary;
	private BoundaryBox bb;
	
	private int area;
	private double mean;
	private Point centroid;
	
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
	public Cell(String name, ImageProcessor origImg, ImageProcessor segmented, int num, BoundaryBox bb, int area, double mean, Point centroid){
		imgName = name;
		this.origImg = origImg;
		this.segImg = segmented;
		id = num;
		this.bb = bb;
		this.area = area;
		this.mean = mean;
		this.centroid = centroid;
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
					wand.autoOutline(i, j);
					Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
					orig.setRoi(roi);
					ResultsTable rt = new ResultsTable();
					Analyzer analyzer = new Analyzer(orig, 
							Measurements.AREA | Measurements.RECT | Measurements.CENTROID | Measurements.MEAN, rt);
					analyzer.measure();
					cells.add(new Cell(imgName, masked, threshSeg, id, 
							new BoundaryBox(rt, 0), (int) rt.getValue("Area", 0), rt.getValue("Mean", 0), 
							new Point((int) rt.getValue("X", 0), (int) rt.getValue("Y", 0))));
					orig.restoreRoi();
				}
			}
		}
//		for(int i = 0; i < rt.getCounter(); i++){
//			cells.add(new Cell(imgName, masked,i+1, new BoundaryBox(rt, i), (int) rt.getValue("Area", i), rt.getValue("Mean", i), 
//					new Point((int) rt.getValue("X", i), (int) rt.getValue("Y", i))));
//		}
		
		return cells;
	}
	
	public String toString(){
		return "Cell #" + id + " in image " + imgName + " at " + centroid;
	}
}
