package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.Binary;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * This class will perform segmentation on an image
 * @author Alec
 *
 */
public class Segmentator {
	
	private RankFilters filter = new RankFilters();
	private Canny canny = new Canny(.4, .7, Math.sqrt(2));
	private Binary binFilt = new Binary();
	private ContrastEnhancer enhance = new ContrastEnhancer();
	
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
		long begin = System.nanoTime();
		
		InputImage input = new InputImage(path);
		ImageProcessor origImg = input.getOrig().getProcessor();
		ImageProcessor adjustedImg = input.getAdjusted().getProcessor();
		int width = origImg.getWidth();
		int height = adjustedImg.getHeight();
		
		ImageProcessor minSuppres = doMinimumSuppression(origImg, adjustedImg);
		
		//Now perform difference of Gaussians
		ImageProcessor en = gaussDifference(minSuppres, 8.0, 5.0);
		
		en.copyBits(minSuppres, 0, 0, Blitter.MULTIPLY);
		
		//Detect edges
		ImageProcessor e = canny.canny(en);
	
		//dilate the image
		filter.rank(e, 10, RankFilters.MAX);
		
		e = bwAreaOpen(e, 500);
		e = bwAreaOpen(e, 300);
		
		fillHoles(e);
		
		ImageProcessor cells = findCells(e).convertToFloatProcessor();
		
		//smoothing
		ImageProcessor presmooth1 = cells.duplicate();
		presmooth1.resetMinAndMax();
		
		ImageProcessor smooth = doSmoothing(cells, adjustedImg);
		
		List<Point> maximList = findMaxima(smooth);
		
		ImageProcessor boundaries = findBoundaries(e);		
		
		//The ith maximum belongs to the jth cell (belongs[i] = j)
		//may want to have data structure so that the link is more clear
		Hashtable<Point, BoundaryBox> belongs = new Hashtable<Point, BoundaryBox>();
		for(int i = 0; i < maximList.size(); i++){
			Point p = maximList.get(i);
			belongs.put(p, new BoundaryBox(p, presmooth1));
		}
		
		ImageProcessor imc = origImg.convertToFloatProcessor();
		imc.invert();
		
		boundaries = findConnectedComponents(boundaries);
		
		double[] hMin = findhMin(maximList, belongs, width, height, boundaries, imc);
		
		ImageProcessor threshSmooth = presmooth1.convertToByteProcessor(true);
		threshSmooth.threshold(0);
		
		List<BoundaryBox> bbs = BoundaryBox.getBoundaries(threshSmooth, maximList);
		
		ImageProcessor mask = performWatershedding(maximList, bbs, width, height, hMin, origImg);
		
		System.out.println("Time elapsed: " + (System.nanoTime() - begin)/1000000000.0 + " seconds");
		return new ImagePlus("Hello", mask); //TODO: change when completed testing
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
		
		return dst.convertToFloatProcessor();
		
	}
	
	/**
	 * Perform watershedding!
	 * @param region
	 * @param minBright
	 * @param minHeight
	 * @return
	 */
	private ImageProcessor regionTight(ImageProcessor region, int minBright, double minHeight){
		region.resetMinAndMax();
		ImageProcessor mask = region.convertToByteProcessor(true);
		enhance.stretchHistogram(mask, 2);
		mask.blurGaussian(1.5);
		mask.threshold(minBright);
		mask = bwAreaOpen(mask, 1000);
		mask.invert();
		mask = bwAreaOpen(mask, 1000);
		mask.invert();
		
		ImageProcessor s = region.convertToFloatProcessor();
		s.invert();
		s.blurGaussian(3.0);
		s.resetMinAndMax();
		s.subtract(s.getMin());
		s.resetMinAndMax();
		if(s.getMax() > 0.0) s.multiply(1.0 / s.getMax());
		
		s.resetMinAndMax();
		ImageProcessor w = s.convertToByteProcessor(true);
		
		MaximumFinder mf = new MaximumFinder();
		w = hMinima(w, (int) minHeight);
		w = mf.findMaxima(w, 1.0, 1.0, MaximumFinder.SEGMENTED , false, false);
		
		w.threshold(0);
		
		mask.copyBits(w, 0, 0, Blitter.AND);
		
		mask = bwAreaOpen(mask, 200);
		
		fillHoles(mask);
		return setLabels(mask);
		
	}
	
	/**
	 * Performs an h-minima transform. Suppresses all minima with depth below m
	 * @param ip
	 * @param m
	 * @return
	 */
	private ImageProcessor hMinima(ImageProcessor ip, int m){
		ip.invert();
		ImageProcessor ip1 = ip.duplicate();
		ip1.subtract(m);
		GreyscaleReconstruct_ gr = new GreyscaleReconstruct_();
		Object[] result = gr.exec(new ImagePlus("temp", ip), new ImagePlus("temp1", ip1), "null", true, false);
		ip.invert();
		return ((ImagePlus) result[1]).getProcessor();
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
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_MASKS, 
				0, new ResultsTable(), size, Double.POSITIVE_INFINITY);
		pa.analyze(ip);
		result = ip.getProcessor();
		result.invertLut();
		return result;
	}
	
	/**
	 * Performs minimum suppression on the image.
	 * @param in
	 * @param adjusted
	 * @return
	 */
	private ImageProcessor doMinimumSuppression(ImageProcessor in, ImageProcessor adjusted){
		//Generate a mask with threshold 0.2*255
		ImageProcessor adjImg = adjusted.duplicate();
		adjImg.threshold(51);
				
		//now median filter with a 5x5 kernel to smooth the mask
		
		filter.rank(adjImg, 2, RankFilters.MEDIAN);
				
		//apply mask
		ImageProcessor minSuppres = in.duplicate();
		minSuppres.copyBits(adjImg, 0, 0, Blitter.AND);
		minSuppres = minSuppres.convertToFloatProcessor();
		minSuppres.resetMinAndMax();
		minSuppres.multiply(1.0/minSuppres.getMax());
		minSuppres.resetMinAndMax();
		return minSuppres;
	}
	
	/**
	 * Fills holes
	 * @param ip
	 */
	private void fillHoles(ImageProcessor ip){
		binFilt.setup("fill holes", null);
		ip.invert();
		binFilt.run(ip);	
		ip.invert();
	}
	
	/**
	 * Finds cells in ip
	 * Similar to finding connected components, but with a minimum size of 300
	 * @param ip
	 * @return
	 */
	private ImageProcessor findCells(ImageProcessor ip){
		ImageProcessor temp = ip.duplicate();
		temp.invert();
		ImagePlus imp = new ImagePlus("Temp", temp);
		ResultsTable blobs = new ResultsTable();
		Double min_size = 300.0;
		Double max_size = Double.POSITIVE_INFINITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_ROI_MASKS, 
				Measurements.AREA, blobs, min_size, max_size);
		pa.analyze(imp);
		return imp.getProcessor();
	}
	
	/**
	 * Performs smoothing on the minimum suppressed image
	 * @param minSuppres
	 * @param adjusted
	 * @return
	 */
	private ImageProcessor doSmoothing(ImageProcessor minSuppres, ImageProcessor adjusted){
		ImageProcessor adj = adjusted.convertToFloatProcessor();
		ImageProcessor ip = minSuppres.duplicate();
		adj.resetMinAndMax();
		if(adj.getMax() > 0.0) adj.multiply(1.0/adj.getMax());
		adj.resetMinAndMax();

		ip.copyBits(adj, 0, 0, Blitter.MULTIPLY);
		ip.blurGaussian(10);
		//tempSmooth.setMinAndMax(0, 1.0);
		
		return ip;
	}
	
	/**
	 * Finds the maximas of smoothed
	 * @param smoothed
	 * @return
	 */
	private List<Point> findMaxima(ImageProcessor smoothed){
		smoothed.setRoi(40, 40, smoothed.getWidth()-2*40, smoothed.getHeight()-2*40);
		MaximumFinder maxFind = new MaximumFinder();
		ByteProcessor maximas = maxFind.findMaxima(smoothed, 0.0, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, false, false);
		
		List<Point> maximList = new ArrayList<Point>();
		for(int i = 0; i < maximas.getWidth(); i++){
			for(int j = 0; j < maximas.getHeight(); j++){
				if(maximas.get(i,j) == 255){
					maximList.add(new Point(i,j));
				}
			}
		}
		return maximList;
	}
	
	/**
	 * Finds cell boundaries
	 * @param ip
	 * @return
	 */
	private ImageProcessor findBoundaries(ImageProcessor ip){
		ImageProcessor boundaries = ip.duplicate();
		boundaries.threshold(0);
		ImageProcessor tempBoundaries = boundaries.duplicate();
		filter.rank(tempBoundaries, 1, RankFilters.MIN);
		boundaries.copyBits(tempBoundaries, 0, 0, Blitter.SUBTRACT);
		return boundaries;
	}
	
	/**
	 * Labels connected components
	 * @param ip
	 * @return
	 */
	private ImageProcessor findConnectedComponents(ImageProcessor ip){
		ImageProcessor boundariesTemp = ip.convertToByteProcessor(true);
		boundariesTemp.threshold(0);
		return setLabels(ip);
	}
	
	/**
	 * Sets labels on a thresholded image of connected components
	 * @param ip
	 * @return
	 */
	private ImageProcessor setLabels(ImageProcessor ip){
		ImageProcessor temp = ip.duplicate();
		temp.invert();
		ImagePlus im = new ImagePlus("Temp", temp.duplicate());
		ResultsTable cc = new ResultsTable();
		Double min_size = 0.0;
		Double max_size = Double.POSITIVE_INFINITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_ROI_MASKS, 
				0, cc, min_size, max_size);
		pa.analyze(im);	
		return im.getProcessor();
	}
	
	private double[] findhMin(List<Point> maximList, Hashtable<Point, BoundaryBox> belongs, int width, int height, 
			ImageProcessor boundaries, ImageProcessor imc){
		
		//TODO: make faster
		double[] hMin = new double[maximList.size()];
		for(int i = 0; i < maximList.size(); i++){
			Point maxim = maximList.get(i);
			BoundaryBox bb = belongs.get(maxim);
			int area = 0;
			float tot = 0;
			
			for(int j = bb.getX(); j < bb.getX() + bb.getWidth(); j++){
				for(int k = bb.getY(); k < bb.getY() + bb.getHeight(); k++){
					if(boundaries.get(j,k) != 0.0){
						tot += (Float.intBitsToFloat(imc.get(j,k))/255.0);
						area++;
						//System.out.println(tot);
					}
				}
			}
			float mean = tot/area;
			hMin[i] = .25*mean;
		}
		
		return hMin;
	}
	
	private ImageProcessor performWatershedding(List<Point> maximList, List<BoundaryBox> bbs, 
			int width, int height, double[] hMin, ImageProcessor origImg){
		ImageProcessor tempMask = new ByteProcessor(width, height);
		for(int i = 0; i < maximList.size(); i++){
			BoundaryBox bb = bbs.get(i);
			int[] bbSize = {bb.getWidth(), bb.getHeight()};
			BoundaryBox boundBox = BoundaryBox.clip(new BoundaryBox(
					maximList.get(i).getX() - bbSize[0] - 80, 
					maximList.get(i).getY() - bbSize[1] - 80, 
					2*bbSize[0]+160, 2*bbSize[1]+160),
					0, 0, width - 1, height - 1);
			ImageProcessor window = new FloatProcessor(width, height);
			ImageProcessor im = origImg.duplicate();
			im.setRoi(boundBox.getX(), boundBox.getY(), boundBox.getWidth(), boundBox.getHeight());
			ImageProcessor interest = im.crop();
			window.copyBits(regionTight(interest, 28, hMin[i]*255), boundBox.getX(), boundBox.getY(), Blitter.COPY);
			if(window.get(maximList.get(i).getX(), maximList.get(i).getY()) != 0){
				float num = Float.intBitsToFloat(window.get(maximList.get(i).getX(), maximList.get(i).getY()));
				for(int j = boundBox.getX(); j < boundBox.getX() + boundBox.getWidth(); j++){
					for(int k = boundBox.getY(); k < boundBox.getY() + boundBox.getHeight(); k++){
						if(Float.intBitsToFloat(window.get(j,k)) == num){
							tempMask.putPixelValue(j,k,255);
						}
					}
				}
			}
		}
		ImageProcessor mask = origImg.duplicate();
		mask.threshold(28);
		mask.copyBits(tempMask, 0, 0, Blitter.AND);
		mask = setLabels(bwAreaOpen(mask, 800));
		return mask;
	}
		
}
