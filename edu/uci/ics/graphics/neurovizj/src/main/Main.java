package edu.uci.ics.graphics.neurovizj.src.main;

import edu.uci.ics.graphics.neurovizj.src.process.Segmentator;
import ij.IJ;
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
			Segmentator segmentator = new Segmentator();
			IJ.save(segmentator.segment(iName), oName); //TODO: remove when finished testing
			opener.open("test.png");
		}
	}

}
