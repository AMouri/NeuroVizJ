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
		minSuppres = minSuppres.convertToFloatProcessor();
		minSuppres.resetMinAndMax();
		minSuppres.multiply(1.0/minSuppres.getMax());
		minSuppres.resetMinAndMax();
		
		//FloatProcessor temp1 = minSuppres.convertToFloatProcessor();

		//Now perform difference of Gaussians
		ImageProcessor en = gaussDifference(minSuppres, 8.0, 5.0);
		FloatProcessor temp = en.convertToFloatProcessor();
		temp.copyBits(minSuppres, 0, 0, Blitter.MULTIPLY);
		
		//Detect edges
		Canny canny = new Canny(.4, .7, Math.sqrt(2));
		ImageProcessor e = canny.canny(temp);
	
		//dilate the image
		filter.rank(e, 10, RankFilters.MAX);
		
		e = bwAreaOpen(e, 500);
		e = bwAreaOpen(e, 300);
		
		//opening!
		/*
		e.invert();
		filter.rank(e, 10, RankFilters.MIN);
		filter.rank(e, 10, RankFilters.MAX);
		e.invert();
		*/
		
		System.out.println("finding cells");
		//find all cells
		e.invert();
		ImagePlus imp = new ImagePlus("Temp", e.duplicate());
		e.invert();
		ResultsTable blobs = new ResultsTable();
		Double min_size = 0.0;
		Double max_size = Double.POSITIVE_INFINITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS, 0, blobs, min_size, max_size);
		pa.analyze(imp);
		
		Binary binFilt = new Binary();
		binFilt.setup("fill holes", null);
		e.invert();
		binFilt.run(e);	
		e.invert();
		
		//filter.rank(e, 5, RankFilters.OPEN);
		
		//smoothing
		//may want to make better
		ImageProcessor tempSmooth = e.convertToFloatProcessor();
		tempSmooth.resetMinAndMax();
		tempSmooth.multiply(1.0/tempSmooth.getMax());
		tempSmooth.resetMinAndMax();
		ImageProcessor presmooth1 = tempSmooth.duplicate();
		
		ImageProcessor adj = adjImg.convertToFloatProcessor();
		adj.resetMinAndMax();
		if(adj.getMax() > 0.0) adj.multiply(1.0/adj.getMax());
		adj.resetMinAndMax();

		tempSmooth.copyBits(adj, 0, 0, Blitter.MULTIPLY);
		tempSmooth.blurGaussian(10);
		tempSmooth.setMinAndMax(0, 1.0);
		
		ImageProcessor smooth = tempSmooth.convertToByteProcessor(true);
		
		//determine maxima of smoothed image
		ImageProcessor mask = genMask(smooth.getWidth(), smooth.getHeight(), 40);
		smooth.copyBits(mask, 0, 0, Blitter.AND);
		MaximumFinder maxFind = new MaximumFinder();
		ImageProcessor maximas = maxFind.findMaxima(smooth, 0, 0, MaximumFinder.IN_TOLERANCE, true, false);

		System.out.println("finding maxima");
		maximas.invert();
		ImagePlus maxImp = new ImagePlus("Temp", maximas.duplicate());
		maximas.invert();
		ResultsTable m = new ResultsTable();
		min_size = 0.0;
		max_size = Double.POSITIVE_INFINITY;
		pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS, 0, m, min_size, max_size);
		pa.analyze(maxImp);
		
		//determine boundaries
		ImageProcessor boundaries = presmooth1.duplicate();
		boundaries.invert();
		ImageProcessor tempBoundaries = boundaries.duplicate();
		filter.rank(tempBoundaries, 2, RankFilters.MIN);
		boundaries.copyBits(tempBoundaries, 0, 0, Blitter.SUBTRACT);
		
		
		//invert the original image
		ImageProcessor imc = input.getOrig().getProcessor().duplicate();
		imc.invert();
		
		System.out.println("finding connected components");
		//find connected components
		boundaries.invert();
		ImagePlus conComp = new ImagePlus("Temp", boundaries.convertToByteProcessor());
		boundaries.invert();
		ResultsTable cc = new ResultsTable();
		min_size = 0.0;
		max_size = Double.POSITIVE_INFINITY;
		pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS, 0, cc, min_size, max_size);
		pa.analyze(conComp);
		
		/*
		//do something to find minimum heights
		double[] hMin = new double[m.getCounter()];
		for(int i = 0; i < m.getCounter(); i++){
			//hMin[i] = .25*
		}
		*/
		
		ImageProcessor result = new ByteProcessor(input.getOrig().getWidth(), input.getOrig().getHeight());

		
		System.out.println("Time elapsed: " + (System.nanoTime() - begin)/1000000000.0 + " seconds");
		//e = e.convertToFloat();
		return new ImagePlus("Hello", conComp.getProcessor()); //TODO: change when completed testing
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
	
	/**
	 * Perform watershedding!
	 * @param region
	 * @param minBright
	 * @param minHeight
	 * @return
	 */
	private ImageProcessor regionTight(ImageProcessor region, int minBright, int minHeight){
		ImageProcessor mask = region.convertToByteProcessor(false);
		mask.blurGaussian(2.5);
		mask.threshold(minBright);
		bwAreaOpen(mask, 1000);
		mask.invert();
		bwAreaOpen(mask, 1000);
		mask.invert();
		
		ImageProcessor s = region.duplicate();
		s.invert();
		s.blurGaussian(3.0);
		s.resetMinAndMax();
		s.subtract(s.getMin());
		s.resetMinAndMax();
		if(s.getMax() > 0.0) s.multiply(1 / s.getMax());
		
		ImageProcessor w = s.convertToByte(false);
		w.invert();
		
		MaximumFinder mf = new MaximumFinder();
		//not sure if this is correct
		w = mf.findMaxima(s, 0.0, 255 - minHeight, MaximumFinder.SEGMENTED , false, false);
		w.invert();
		
		w.threshold(0);
		
		mask.copyBits(w, 0, 0, Blitter.AND);
		
		bwAreaOpen(mask, 200);
		
		Binary binFilt = new Binary();
		binFilt.setup("fill holes", null);
		mask.invert();
		binFilt.run(mask);
		mask.invert();
		
		return mask;
		
	}
	
	/**
	 * removes areas smaller than size
	 * @param region
	 * @param size
	 */
	private ImageProcessor bwAreaOpen(ImageProcessor region, int size){
		ImageProcessor result = region.duplicate();
		result.invert();
		ImagePlus ip = new ImagePlus("Temp", result);
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.IN_SITU_SHOW, 
				0, new ResultsTable(), size, Double.POSITIVE_INFINITY);
		pa.analyze(ip);
		result.invert();
		return result;
	}
	
}
