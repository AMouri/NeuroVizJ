package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;

public class BoundaryBox {
	private Point ul;
	private int width;
	private int height;
	
	BoundaryBox(int x, int y, int width, int height){
		this.ul = new Point(x,y);
		this.width = width;
		this.height = height;
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	public int getX(){
		return ul.getX();
	}
	
	public int getY(){
		return ul.getY();
	}
	
	public static BoundaryBox clip(BoundaryBox bb, int minX, int minY, int maxX, int maxY){
		int newX = Math.min(Math.max(minX, bb.getX()), maxX);
		int newY = Math.min(Math.max(minY, bb.getY()), maxY);
		
		int endX = Math.min(Math.max(minX, bb.getX()+bb.getWidth()), maxX);
		int endY = Math.min(Math.max(minY, bb.getY()+bb.getHeight()), maxY);
		
		System.out.println(bb.getWidth() + " " + bb.getHeight());
		return new BoundaryBox(newX, newY, endX-newX, endY-newY);
		
		
	}
	
	public static List<BoundaryBox> getBoundaries(ImageProcessor im){
		List<BoundaryBox> result = new ArrayList<BoundaryBox>();
		im.invert();
		ImagePlus bb = new ImagePlus("Temp", im.duplicate());
		im.invert();
		ResultsTable bbs = new ResultsTable();
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_RESULTS, Measurements.RECT, bbs, 0.0, Double.POSITIVE_INFINITY);
		pa.analyze(bb);
		
		for(int i = 0; i < bbs.getCounter(); i++){
			result.add(new BoundaryBox((int) bbs.getValueAsDouble(bbs.getColumnIndex("BX"),i), (int) bbs.getValueAsDouble(bbs.getColumnIndex("BY"),i), (int) bbs.getValueAsDouble(bbs.getColumnIndex("Width"),i), (int) bbs.getValueAsDouble(bbs.getColumnIndex("Height"),i)));
		}
		
		return result;
	}
}
