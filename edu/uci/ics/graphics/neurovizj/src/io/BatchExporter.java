package edu.uci.ics.graphics.neurovizj.src.io;

import java.io.File;

import edu.uci.ics.graphics.neurovizj.src.process.SegmentedImage;

/**
 * Exports batches of images with an associated excel spreadsheet
 * @author Alec
 *
 */
public class BatchExporter {
	
	/**
	 * Performs exportation of series of tracked images and an output name
	 * @param tracked
	 * @param out
	 */
	public static void exportBatch(SegmentedImage[] tracked, String out){
		for(int i = 0; i < tracked.length; i++){
			System.out.println("Exporting image " + i);
			String dirName = out + " t" + i;
			File dir = new File(dirName);
			dir.mkdir();
			ImageExporter.saveTiff(tracked[i], dirName + "\\");
		}
		
		System.out.println("Generating spreadsheet...");
		ExcelExporter.exportSequenceAsSpreadSheet(tracked, out);
	}
}
