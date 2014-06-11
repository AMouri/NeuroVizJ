package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.Opener;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Binary;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
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
		ImageProcessor minSuppres = input.getOrig().getProcessor().duplicate();
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
		
		Binary binFilt = new Binary();
		binFilt.setup("fill holes", null);
		e.invert();
		binFilt.run(e);	
		e.invert();
		
		System.out.println("finding cells");
		//find all cells
		e.invert();
		ImagePlus imp = new ImagePlus("Temp", e);
		ResultsTable blobs = new ResultsTable();
		Double min_size = 300.0;
		Double max_size = Double.POSITIVE_INFINITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_ROI_MASKS | ParticleAnalyzer.SHOW_RESULTS, 0, blobs, min_size, max_size);
		pa.analyze(imp);
		e = imp.getProcessor();
		
		//filter.rank(e, 5, RankFilters.OPEN);
		
		//smoothing
		ImageProcessor tempSmooth = e.convertToFloatProcessor();
		//tempSmooth.resetMinAndMax();
		ImageProcessor presmooth1 = tempSmooth.duplicate();
		
		ImageProcessor adj = adjImg.convertToFloatProcessor();
		adj.resetMinAndMax();
		if(adj.getMax() > 0.0) adj.multiply(1.0/adj.getMax());
		adj.resetMinAndMax();

		tempSmooth.copyBits(adj, 0, 0, Blitter.MULTIPLY);
		tempSmooth.blurGaussian(10);
		//tempSmooth.setMinAndMax(0, 1.0);
		
		ImageProcessor smooth = tempSmooth.duplicate();
		
		//determine maxima of smoothed image
		smooth.setRoi(40, 40, smooth.getWidth()-2*40, smooth.getHeight()-2*40);
		MaximumFinder maxFind = new MaximumFinder();
		ByteProcessor maximas = maxFind.findMaxima(smooth, 0.0, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, false, false);

		
		ArrayList<Point> maximList = new ArrayList<Point>();
		for(int i = 0; i < maximas.getWidth(); i++){
			for(int j = 0; j < maximas.getHeight(); j++){
				if(maximas.get(i,j) == 255){
					maximList.add(new Point(i,j));
				}
			}
		}
		
		//determine boundaries
		ImageProcessor boundaries = presmooth1.duplicate();
		boundaries.invert();
		ImageProcessor tempBoundaries = boundaries.duplicate();
		filter.rank(tempBoundaries, 1.5, RankFilters.MIN);
		boundaries.copyBits(tempBoundaries, 0, 0, Blitter.SUBTRACT);
		
		int num_cells = 0;
		int[] belongs = new int[maximList.size()];
		for(int i = 0; i < maximList.size(); i++){
			Point p = maximList.get(i);
			belongs[i] = (int) Float.intBitsToFloat(presmooth1.get(p.getX(), p.getY()));
			System.out.println(belongs[i]);
			num_cells = Math.max(num_cells, belongs[i]);
		}
		
		//invert the original image
		ImageProcessor imc = input.getOrig().getProcessor().convertToFloatProcessor();
		imc.invert();
		
		System.out.println("finding connected components");
		//find connected components
		boundaries = boundaries.convertToByteProcessor(true);
		boundaries.threshold(0);
		boundaries.invert();
		ImagePlus conComp = new ImagePlus("Temp", boundaries.duplicate());
		boundaries.invert();
		ResultsTable cc = new ResultsTable();
		min_size = 0.0;
		max_size = Double.POSITIVE_INFINITY;
		pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_ROI_MASKS | ParticleAnalyzer.SHOW_RESULTS, 0, cc, min_size, max_size);
		pa.analyze(conComp);
		boundaries = conComp.getProcessor().convertToByteProcessor();
		
		//do something to find minimum heights
		//has to be a faster way
		double[] hMin = new double[maximList.size()];
		ImageProcessor some_result = new FloatProcessor(1000, 1000);
		for(int i = 0; i < maximList.size(); i++){
			int curr = belongs[i];
			FloatProcessor tempBound = new FloatProcessor(boundaries.getWidth(), boundaries.getHeight());
			int area = 0;
			for(int j = 0; j < tempBound.getWidth(); j++){
				for(int k = 0; k < tempBound.getHeight(); k++){
					if(boundaries.get(j,k) == curr){
						tempBound.putPixelValue(j,k,1.0);
						area++;
					}
				}
			}
			ImageProcessor om = imc.duplicate();
			om.multiply(1.0/255);
			om.copyBits(tempBound, 0, 0, Blitter.MULTIPLY);
			some_result = om;
			
			ImageStatistics stats = om.getStatistics();
			hMin[i] = .25*stats.mean*boundaries.getWidth()*boundaries.getHeight()/(area);
			System.out.println(hMin[i]);
		}
		
		ImageProcessor result = new ByteProcessor(input.getOrig().getWidth(), input.getOrig().getHeight());

		
		System.out.println("Time elapsed: " + (System.nanoTime() - begin)/1000000000.0 + " seconds");
		//e = e.convertToFloat();
		return new ImagePlus("Hello", some_result); //TODO: change when completed testing
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
