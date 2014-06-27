package edu.uci.ics.graphics.neurovizj.src.process;

import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
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
	private ContrastEnhancer enhance = new ContrastEnhancer();
	
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
		
//		enhance.setNormalize(true);
		enhance.stretchHistogram(adjProc, 4);
		
		adjImg = new ImagePlus("test.png", adjProc);
	}
	
	/**
	 * Gets the original image
	 * @return
	 */
	public ImagePlus getOrig(){
		return origImg;
	}
	
	/**
	 * Gets a contrast adjusted de-noised version of the original image.
	 * @return
	 */
	public ImagePlus getAdjusted(){
		return adjImg;
	}
}
