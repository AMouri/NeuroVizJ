package edu.uci.ics.graphics.neurovizj.src.main;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.extensions.MatlabTypeConverter;
import edu.uci.ics.graphics.neurovizj.src.io.BatchExporter;
import edu.uci.ics.graphics.neurovizj.src.io.ExcelExporter;
import edu.uci.ics.graphics.neurovizj.src.io.ImageExporter;
import edu.uci.ics.graphics.neurovizj.src.process.Segmentator;
import edu.uci.ics.graphics.neurovizj.src.process.SegmentedImage;
import edu.uci.ics.graphics.neurovizj.src.process.Tracker;

/**
 * Entry point for the program
 * Command line flags are documented in the README
 * @author Alec
 *
 */
public class Main {
	
	static String oName = "output.png";
	static String iName = "input.png";
	static String folder = "";
	static boolean segment = false;
	static boolean track = false;
	static boolean thresholded = false;

	public static void main(String[] args) {
		for(int i = 0; i < args.length; i++){
			switch(args[i]){
				case "-i":
					iName = args[++i];
					break;
				case "-o":
					oName = args[++i];
					break;
				case "-f":
					folder = args[++i];
					break;
				case "-s":
					segment = true;
					break;
				case "-t":
					track = true;
					break;
				case "-thresh":
					thresholded = true;
					break;
				default:
					System.err.println("Invalid command line arguments");
					System.exit(1);	
			}
		}
		
		if(segment){
			try{
				MatlabProxyFactory factory = new MatlabProxyFactory();
				MatlabProxy proxy = factory.getProxy();
				MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
				Segmentator segmentator = new Segmentator(proxy, processor);
				SegmentedImage result = new SegmentedImage(iName, segmentator);
				ExcelExporter.exportImageAsSpreadSheet(result, oName);
				proxy.disconnect();
			} catch(Exception e){
				e.printStackTrace();
			}
		} else if (track){
			
			try{
				MatlabProxyFactory factory = new MatlabProxyFactory();
				MatlabProxy proxy = factory.getProxy();
				MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
				Tracker tracker = new Tracker(proxy, processor);
				SegmentedImage[] tracked = tracker.track(folder, iName);
				BatchExporter.exportBatch(tracked, oName);
				proxy.disconnect();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}

}
