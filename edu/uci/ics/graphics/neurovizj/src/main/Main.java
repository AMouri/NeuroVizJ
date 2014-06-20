package edu.uci.ics.graphics.neurovizj.src.main;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.extensions.MatlabTypeConverter;
import edu.uci.ics.graphics.neurovizj.src.process.Cell;
import edu.uci.ics.graphics.neurovizj.src.process.Segmentator;
import edu.uci.ics.graphics.neurovizj.src.process.SegmentedImage;
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;

public class Main {
	
	static String oName = "output.png";
	static String iName = "input.png";
	static boolean segment = false;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		for(int i = 0; i < args.length; i++){
			if(args[i].equals("-i")){
				iName = args[++i];
			} else if (args[i].equals("-o")){
				oName = args[++i];
			} else if (args[i].equals("-s")){
				segment = true;
			}
		}
		
		if(segment){
			Opener opener = new Opener();
			try{
				MatlabProxyFactory factory = new MatlabProxyFactory();
				MatlabProxy proxy = factory.getProxy();
				MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
				Segmentator segmentator = new Segmentator(proxy, processor);
				SegmentedImage result = new SegmentedImage(iName, segmentator);
				IJ.save(new ImagePlus(oName, result.getImage()), oName); //TODO: remove when finished testing
//				IJ.save(segmentator.segment(iName, null, null), oName); //TODO: remove when finished testing
				opener.open(oName);
				proxy.disconnect();
				for(Cell cell : result.getCells()){
					System.out.println(cell);
				}
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}

}
