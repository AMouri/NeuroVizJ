package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.filter.Binary;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;

/**
 * This class will perform segmentation on an image
 * @author Alec
 *
 */
public class Segmentator {
	
	/**
	 * Directions for canny edge detection
	 * @author Alec
	 *
	 */
	
	/**
	 * Segments the image located at path
	 * @param path
	 * @return
	 */
	public ImagePlus segment(String path){
		//long begin = System.nanoTime();
		long begin = System.nanoTime();
		InputImage input = new InputImage(path);
		ImageProcessor adjustedImg = input.getAdjusted().getProcessor();
		//Generate a mask with threshold 0.2*255
		ImageProcessor adjImg = adjustedImg.duplicate();
		adjustedImg.threshold(51);
		
		//now median filter with a 5x5 kernel to smooth the mask
		RankFilters filter = new RankFilters();
		filter.rank(adjustedImg, 2, RankFilters.MEDIAN);
		
		//apply mask
		ImageProcessor minSuppres = input.getOrig().getProcessor();
		minSuppres.copyBits(adjustedImg, 0, 0, Blitter.AND);
		
		//FloatProcessor temp1 = minSuppres.convertToFloatProcessor();

		//Now perform difference of Gaussians
		ImageProcessor en = gaussDifference(minSuppres, 8.0, 5.0);
		ByteProcessor minSuppres2 = minSuppres.convertToByteProcessor();
		ByteProcessor temp = en.convertToByteProcessor();
		temp.copyBits(minSuppres2, 0, 0, Blitter.MULTIPLY);
		FloatProcessor temp2 = temp.convertToFloatProcessor();
		temp2.resetMinAndMax();
		
		//Detect edges
		Canny canny = new Canny(.4, .7, Math.sqrt(2));
		ImageProcessor e = canny.canny(temp2);
		
		Binary binFilt = new Binary();
		binFilt.setup("fill holes", null);
		e.invert();
		binFilt.run(e);
		e.invert();
		//dilate the image
		filter.rank(e, 5, RankFilters.MAX);
		e.invert();
		binFilt.run(e);
		e.invert();
		
		//opening!
		/*
		e.invert();
		filter.rank(e, 10, RankFilters.MIN);
		filter.rank(e, 10, RankFilters.MAX);
		e.invert();
		*/
		
		/*
		e.invert();
		ImagePlus imp = new ImagePlus("Temp", e.duplicate());
		ResultsTable rt = new ResultsTable();
		Double min_size = 0.0;
		Double max_size = Double.POSITIVE_INFINITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS, 0, rt, min_size, max_size);
		pa.analyze(imp);
		e = imp.getProcessor();
		e.invert();
		*/
		
		//fill holes
		/*
		e.invert();
		binFilt.run(e);
		e.invert();
		*/
		
		filter.rank(e, 5, RankFilters.OPEN);
		
		//smoothing
		//may want to make better
		ImageProcessor presmooth = e.convertToFloatProcessor();
		presmooth.multiply(100);
		ImageProcessor adj = adjImg.convertToFloatProcessor();
		presmooth.copyBits(adj, 0, 0, Blitter.ADD);
		presmooth.blurGaussian(10);
		ImageProcessor smooth = presmooth.convertToByteProcessor(true);
		
		//determine maxima of smoothed image
		ImageProcessor mask = genMask(smooth.getWidth(), smooth.getHeight(), 40);
		smooth.copyBits(mask, 0, 0, Blitter.AND);
		MaximumFinder maxFind = new MaximumFinder();
		ImageProcessor maximas = maxFind.findMaxima(smooth, .002, .02, MaximumFinder.IN_TOLERANCE, false, false);
		
		
		
		System.out.println("Time elapsed: " + (System.nanoTime() - begin)/1000000000.0 + " seconds");
		//e = e.convertToFloat();
		return new ImagePlus("Hello", smooth); //TODO: change when completed testing
	}
	
	/**
	 * Performs difference of 2 gaussians on ip, weighted by weight 1 and weight 2.
	 * Note that ip is not changed.
	 * @param ip
	 * @param weight1
	 * @param weight2
	 * @return
	 */
	private ImageProcessor gaussDifference(ImageProcessor ip, double weight1, double weight2){
		double weight3 = Math.max(weight1, weight2);
		ImageProcessor src = ip.duplicate();
		ImageProcessor dst = ip.duplicate();
		dst.blurGaussian(2);
		dst.multiply(weight1/weight3);
		
		src.blurGaussian(15);
		src.multiply(weight2/weight3);
		dst.copyBits(src, 0, 0, Blitter.SUBTRACT);
		
		dst.multiply(weight3);
		
		return dst;
		
	}
	
	/**
	 * Generates a square mask of size width, height, with true values being a square offset
	 * @param width
	 * @param height
	 * @param offset
	 * @return
	 */
	private ByteProcessor genMask(int width, int height, int offset){
		ByteProcessor bp = new ByteProcessor(width, height);
		
		ByteProcessor src = new ByteProcessor(width-2*offset, height-2*offset);
		src.add(255);
		bp.copyBits(src, offset, offset, Blitter.ADD);
		
		return bp;
	}
	
}
