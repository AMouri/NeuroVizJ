package edu.uci.ics.graphics.neurovizj.src.io;

import java.awt.Color;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import edu.uci.ics.graphics.neurovizj.src.process.ProcessedCell;
import edu.uci.ics.graphics.neurovizj.src.process.Point;
import edu.uci.ics.graphics.neurovizj.src.process.SegmentedImage;

/**
 * Defines methods to save processed data as images
 * @author Alec
 *
 */
public class ImageExporter {

	/**
	 * Saves the tracked images from begin to end as tiff stacks. May or may not be thresholded.
	 * @param tracked
	 * @param begin
	 * @param end
	 * @param out
	 * @param thresholded
	 */
	public static void saveTiff(SegmentedImage[] tracked, int begin, int end, String out, boolean thresholded){
		int cellNum = 1;
		for(ProcessedCell cell : tracked[0].getCells()){
			System.out.println("Saving Cell #" + cellNum);
			ImageStack is = new ImageStack(tracked[0].getImage().getWidth(), tracked[0].getImage().getHeight());
			ColorProcessor[] images = new ColorProcessor[end-begin];
			ProcessedCell c = cell;
			for(int i = 0; c != null && i < end - begin; i++, c= c.getNextCell()){
				images[i] = c.getImg().convertToColorProcessor();
			}
			
			//draw cell backs first
			c = cell;
			for(int i = 0; i < end - begin && c != null; i++, c = c.getNextCell()){
				images[i].setColor(Color.RED);
				for(Point p : c.getPointSet()){
					images[i].drawPixel(p.getX(), p.getY());
				}
			}
			
			//draw centroids
			c = cell;
			for(int i = 0; i < end - begin - 1 && c.getNextCell() != null; i++, c = c.getNextCell()){
				for(int j = 0; images[j] != null && j < end - begin; j++){
					images[j].setColor(Color.GREEN);
					images[j].setLineWidth(4);
					Point cent1 = c.getCentroid();
					images[j].drawDot(cent1.getX(), cent1.getY());
					images[j].setLineWidth(1);
					Point cent2 = c.getNextCell().getCentroid();
					images[j].drawLine(cent1.getX(), cent1.getY(), cent2.getX(), cent2.getY());
				}
			}
			
			//
			c = cell;
			for(int j = 0; j < end - begin && c != null; j++, c = c.getNextCell()){
				images[j].setColor(Color.GREEN);
				images[j].setLineWidth(2);
				Point cent1 = c.getCentroid();
				images[j].drawDot(cent1.getX(), cent1.getY());
			}
			
			for(int i = 0; i < end - begin; i++){
				is.addSlice(images[i]);
			}
			
			String name = out + " - Cell #" + cellNum++ + ".TIF";
			FileSaver fs = new FileSaver(new ImagePlus(name, is));
			fs.saveAsTiffStack(name);
		}
	}
	
	/**
	 * Saves a singleton image as a tiff
	 * @param image
	 * @param out
	 */
	public static void saveTiff(SegmentedImage image, String out){
		int cellNum = 1;
		for(ProcessedCell cell : image.getCells()){
			String name = out + " - Cell #" + cellNum++ + ".TIF";
			cell.setImgName(name);
			FileSaver fs = new FileSaver(new ImagePlus(name, cell.getImg()));
			fs.saveAsTiff(name);
		}
	}
}
