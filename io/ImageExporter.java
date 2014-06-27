package io;

import java.awt.Color;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import edu.uci.ics.graphics.neurovizj.src.process.Cell;
import edu.uci.ics.graphics.neurovizj.src.process.Point;
import edu.uci.ics.graphics.neurovizj.src.process.SegmentedImage;

public class ImageExporter {

	public void saveTiff(SegmentedImage[] tracked, int begin, int end, String out, boolean thresholded){
		int cellNum = 1;
		for(Cell cell : tracked[0].getCells()){
			System.out.println("Saving Cell #" + cellNum);
			ImageStack is = new ImageStack(tracked[0].getImage().getWidth(), tracked[0].getImage().getHeight());
			ColorProcessor[] images = new ColorProcessor[end-begin];
			for(int i = 0; i < end - begin; i++){
				if(thresholded){
					tracked[begin+i].getImage().threshold(0);
					images[i] = tracked[begin+i].getImage().convertToColorProcessor();
				} else {
					images[i] = tracked[begin+i].getOrig().convertToColorProcessor();
				}
			}
			
			//draw cell backs first
			Cell c = cell;
			for(int i = 0; i < end - begin && c != null; i++, c = c.getNextCell()){
				images[i].setColor(Color.RED);
				for(Point p : c.getPointSet()){
					images[i].drawPixel(p.getX(), p.getY());
				}
			}
			
			c = cell;
			for(int i = 0; i < end - begin - 1 && c.getNextCell() != null; i++, c = c.getNextCell()){
				for(int j = 0; j < end - begin; j++){
					images[j].setColor(Color.GREEN);
					images[j].setLineWidth(4);
					Point cent1 = c.getCentroid();
					images[j].drawDot(cent1.getX(), cent1.getY());
					images[j].setLineWidth(1);
					Point cent2 = c.getNextCell().getCentroid();
					images[j].drawLine(cent1.getX(), cent1.getY(), cent2.getX(), cent2.getY());
				}
			}
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
}
