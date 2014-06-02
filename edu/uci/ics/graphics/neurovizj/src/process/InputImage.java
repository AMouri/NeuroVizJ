package edu.uci.ics.graphics.neurovizj.src.process;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * Wrapper for several ImagePlus instances
 * @author Alec
 *
 */
public class InputImage {
	
	private ImagePlus origImg;
	private ImagePlus adjImg;
	
	/**
	 * Loads image located at path and stores useful processed variations of the image
	 * @param path
	 */
	public InputImage(String path){
		origImg = new ImagePlus(path);
		//adjImg = new ImagePlus(path);
		
		//rescale the intensity of the image so that min is 5.1 and max is 249.9
		//Bounds are chosen so that we have a 2% buffer between true min and true max
		ImageStatistics imDat = origImg.getStatistics(); //retrieve statistics from the image
		ImageProcessor imProc = origImg.getProcessor();
		imProc.subtract(imDat.min);
		if((imDat.max - imDat.min) > 0.0) imProc.multiply(255.0/(imDat.max - imDat.min));
		
		//now apply a median filter with a 4x4 kernel
		ImageProcessor adjProc = imProc.duplicate();
		RankFilters medFilt = new RankFilters();
		medFilt.rank(adjProc, 1.5, RankFilters.MEDIAN);
		
		//map pixel intensities from 0-255 to 5.1-249.9
		//currently a bit bright, leaving alone for now
		/*
		adjProc.multiply((249.9 - 5.1)/255);
		adjProc.add(5.1);
		*/
		
		//TODO, image is a bit bright
		
		adjImg = new ImagePlus("test.png", adjProc);
		
		IJ.save(adjImg, "test.png"); //TODO: remove from testing
	}
	
	public ImagePlus getOrig(){
		return origImg;
	}
	
	public ImagePlus getAdjusted(){
		return adjImg;
	}
}
