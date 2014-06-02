package edu.uci.ics.graphics.neurovizj.src.process;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ImageProcessor;

/**
 * This class will perform segmentation on an image
 * @author Alec
 *
 */
public class Segmentator {
	
	//test for now
	public static void main(String[] args){
		//System.out.println("Hello World!");
		//InputImage test = new InputImage("1.png");
		
		//Opener opener = new Opener();
		//opener.open(segment("1.png"))
		IJ.save(segment("1.png"), "test.png"); //TODO: remove when finished testing
	}
	
	/**
	 * Segments the image located at path
	 * @param path
	 * @return
	 */
	public static ImagePlus segment(String path){
		InputImage input = new InputImage(path);
		ImageProcessor adjustedImg = input.getAdjusted().getProcessor();
		//Generate a mask with threshold 0.2*255
		adjustedImg.threshold(51);
		
		//now median filter with a 5x5 kernel to smooth the mask
		RankFilters medFilt = new RankFilters();
		medFilt.rank(adjustedImg, 2, RankFilters.MEDIAN);
		
		//apply mask
		ImageProcessor minSuppres = input.getOrig().getProcessor();
		minSuppres.copyBits(adjustedImg, 0, 0, Blitter.AND);
	
		//Now perform difference of Gaussians
		GaussianBlur gauss = new GaussianBlur();
		ImageProcessor en = minSuppres.duplicate();
		ImageProcessor temp = minSuppres.duplicate();
		
		gauss.blurGaussian(en, 2, 2, .02);
		en.multiply(1);
		
		gauss.blurGaussian(temp, 15, 15, .02);
		temp.multiply(.625);
		en.copyBits(temp, 0, 0, Blitter.SUBTRACT);
		
		en.multiply(8);
		
		return new ImagePlus("Hello", en); //TODO: change was completed testing
	}
	
}
