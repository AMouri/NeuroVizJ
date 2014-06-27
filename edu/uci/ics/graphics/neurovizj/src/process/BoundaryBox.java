package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.ArrayList;
import java.util.List;

import ij.gui.Wand;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

/**
 * A boundary box enclosing some region
 * @author Alec
 *
 */
public class BoundaryBox {
	private Point ul;
	private int width;
	private int height;
	
	/**
	 * Constructs a boundary box with UL = (x,y)
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	BoundaryBox(int x, int y, int width, int height){
		this.ul = new Point(x,y);
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Constructs a boundary box from a results table at a given row
	 * @param rt
	 * @param row
	 */
	BoundaryBox(ResultsTable rt, int row){
		this((int) rt.getValue("BX", row), (int) rt.getValue("BY", row), (int) rt.getValue("Width", row), (int) rt.getValue("Height", row));
	}
	
	/**
	 * Constructs a boundary box of the object located at p in ip.
	 * @param p
	 * @param ip
	 */
	BoundaryBox(Point p, ImageProcessor ip){
		Wand wand = new Wand(ip);
		wand.autoOutline(p.getX(), p.getY());
		int minX, minY, maxX, maxY;
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		maxX = 0;
		maxY = 0;
		for(int j = 0; j < wand.npoints; j++){
			int x = wand.xpoints[j];
			int y = wand.ypoints[j];
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		this.ul = new Point(minX, minY);
		this.width = maxX - minX;
		this.height = maxY - minY;
	}
	
	/**
	 * Gets the width of the boundary box
	 * @return
	 */
	public int getWidth(){
		return width;
	}
	
	/**
	 * Gets the height of the boundary box
	 * @return
	 */
	public int getHeight(){
		return height;
	}
	
	/**
	 * Gets the x coordinate of the UL corner of the boundary box
	 * @return
	 */
	public int getX(){
		return ul.getX();
	}
	
	/**
	 * Gets the y-coordinate of the UL corner of the boundary box
	 * @return
	 */
	public int getY(){
		return ul.getY();
	}
	
	/**
	 * Clips a boundary box to be constrained by minimum and maximum x/y valuess
	 * @param bb
	 * @param minX
	 * @param minY
	 * @param maxX
	 * @param maxY
	 * @return
	 */
	public static BoundaryBox clip(BoundaryBox bb, int minX, int minY, int maxX, int maxY){
		int newX = Math.min(Math.max(minX, bb.getX()), maxX);
		int newY = Math.min(Math.max(minY, bb.getY()), maxY);
		
		int endX = Math.min(Math.max(minX, bb.getX()+bb.getWidth()), maxX);
		int endY = Math.min(Math.max(minY, bb.getY()+bb.getHeight()), maxY);
		
		return new BoundaryBox(newX, newY, endX-newX, endY-newY);
		
		
	}
	
	/**
	 * Gets the boundary boxes of the objects belonging to maximPoints
	 * @param im
	 * @param maximPoints
	 * @return
	 */
	public static List<BoundaryBox> getBoundaries(ImageProcessor im, List<Point> maximPoints){
		List<BoundaryBox> result = new ArrayList<BoundaryBox>();
		
		for(int i = 0; i < maximPoints.size(); i++){
			result.add(new BoundaryBox(maximPoints.get(i), im));
		}
		
		return result;
	}
	
	public String toString(){
		return "LU: " + ul.toString() + ", " + width + " x " + height;
	}
}
