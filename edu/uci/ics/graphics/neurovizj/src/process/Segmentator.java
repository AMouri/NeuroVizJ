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
	
	private RankFilters filter = new RankFilters();
	private Canny canny = new Canny(.4, .7, Math.sqrt(2));
	private Binary binFilt = new Binary();
	
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
		
		System.out.println("finding cells");
		
		ImageProcessor cells = findCells(e).convertToFloatProcessor();
		
		//smoothing
		ImageProcessor presmooth1 = cells.duplicate();
		presmooth1.resetMinAndMax();
		
		ImageProcessor smooth = doSmoothing(cells, adjustedImg);
		
		//determine maxima of smoothed image
		List<Point> maximList = findMaxima(smooth);
		
		//determine boundaries
		ImageProcessor boundaries = findBoundaries(e);		
		
		//TODO: fix
		int[] belongs = new int[maximList.size()];
		for(int i = 0; i < maximList.size(); i++){
			Point p = maximList.get(i);
			belongs[i] = (int) Float.intBitsToFloat(presmooth1.get(p.getX(), p.getY()));
		}
		
		//invert the original image
		ImageProcessor imc = origImg.convertToFloatProcessor();
		imc.invert();
		
		System.out.println("finding connected components");
		//find connected components
		boundaries = findConnectedComponents(boundaries);
		
		//do something to find minimum heights
		//has to be a faster way
		double[] hMin = new double[maximList.size()];
		for(int i = 0; i < maximList.size(); i++){
			int curr = belongs[i];
			FloatProcessor tempBound = new FloatProcessor(width, height);
			int area = 0;
			for(int j = 0; j < width; j++){
				for(int k = 0; k < height; k++){
					if(boundaries.get(j,k) == curr){
						tempBound.putPixelValue(j,k,1.0);
						area++;
					}
				}
			}
			ImageProcessor om = imc.duplicate();
			om.multiply(1.0/255);
			om.copyBits(tempBound, 0, 0, Blitter.MULTIPLY);
			
			ImageStatistics stats = om.getStatistics();
			hMin[i] = .25*stats.mean*boundaries.getWidth()*boundaries.getHeight()/(area);
			System.out.println(hMin[i]);
		}
		
		ImageProcessor tempMask = new FloatProcessor(input.getOrig().getWidth(), input.getOrig().getHeight());
		ImageProcessor threshSmooth = presmooth1.convertToByteProcessor(true);
		threshSmooth.threshold(0);
		
		List<BoundaryBox> bbs = BoundaryBox.getBoundaries(threshSmooth);
		for(BoundaryBox bb : bbs){
			System.out.println(bb);
		}
		
		for(int i = 0; i < maximList.size(); i++){
			int curr = belongs[i]; //fix, this belongs business is pretty bad
			BoundaryBox bb = bbs.get(curr);
			int[] bbSize = {bb.getWidth(), bb.getHeight()};
			BoundaryBox boundBox = BoundaryBox.clip(new BoundaryBox(
					maximList.get(i).getX() - bbSize[0] - 80, maximList.get(i).getY() - bbSize[1] - 80, 2*bbSize[0]+160, 2*bbSize[1]+160),
					0, 0, input.getOrig().getWidth() - 1, input.getOrig().getHeight() - 1);
			ImageProcessor zz = new FloatProcessor(input.getOrig().getWidth(), input.getOrig().getHeight());
			ImageProcessor im = input.getOrig().getProcessor().duplicate();
			im.setRoi(boundBox.getX(), boundBox.getY(), boundBox.getX() + boundBox.getWidth(), boundBox.getY() + boundBox.getHeight());
			ImageProcessor interest = im.crop();
			System.out.println(bb + " " + boundBox + " Maxima: " + maximList.get(i));
//			zz.copyBits(regionTight(interest, 255*.1, hMin[i]*255), boundBox.getX(), boundBox.getY(), Blitter.COPY);
//			if(zz.get(maximList.get(i).getX(), maximList.get(i).getY()) != 0){
//				float num = Float.intBitsToFloat(zz.get(maximList.get(i).getX(), maximList.get(i).getY()));
//				for(int j = 0; j < zz.getWidth(); j++){
//					for(int k = 0; k < zz.getHeight(); k++){
//						if(Float.intBitsToFloat(zz.get(j,k)) == num){
//							tempMask.putPixelValue(j,k,i);
//						}
//					}
//				}
//			}
			tempMask = regionTight(interest, 255*.1, hMin[i]*255);
			
		}

		
		
		System.out.println("Time elapsed: " + (System.nanoTime() - begin)/1000000000.0 + " seconds");
		//e = e.convertToFloat();
		return new ImagePlus("Hello", tempMask); //TODO: change when completed testing
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
	private ImageProcessor regionTight(ImageProcessor region, double minBright, double minHeight){
		region.resetMinAndMax();
		ImageProcessor mask = region.convertToByteProcessor(true);
		mask.blurGaussian(2.5);
		mask.threshold((int) minBright);
		mask = bwAreaOpen(mask, 1000);
		mask.invert();
		mask = bwAreaOpen(mask, 1000);
		mask.invert();
		if(mask.isInvertedLut()){
			mask.invertLut();
			mask.invert();
		}
		
		ImageProcessor s = region.convertToFloatProcessor();
		s.invert();
		s.blurGaussian(3.0);
		s.resetMinAndMax();
		s.subtract(s.getMin());
		s.resetMinAndMax();
		if(s.getMax() > 0.0) s.multiply(1.0 / s.getMax());
		
		s.resetMinAndMax();
		ImageProcessor w = s.convertToByteProcessor(true);
//		for(int i = 0; i < s.getWidth(); i++){
//			for(int j = 0; j < s.getHeight(); j++){
//				System.out.print(w.get(i,j) + " ");
//			}
//			System.out.println("");
//		}
		//w.invert();
		
		MaximumFinder mf = new MaximumFinder();
		w = hMinima(w, (int) minHeight);
		//w.invert();
		//not sure if this is correct
		w.invert();
		w = mf.findMaxima(w, 1.0, 1.0, MaximumFinder.SEGMENTED , false, false);
		//w.invert();
		//w = hMinima(w, (int) minHeight);
		//w.resetMinAndMax();
		//w.invert();	
		
		w.threshold(0);
		
		//mask.copyBits(w, 0, 0, Blitter.AND);
		
		mask = bwAreaOpen(mask, 200);
		
		Binary binFilt = new Binary();
		binFilt.setup("fill holes", null);
		mask.invert();
		binFilt.run(mask);
		mask.invert();
		
//		mask.invert();
		ImagePlus imp = new ImagePlus("Temp", mask.duplicate());
		mask.invert();
		ResultsTable blobs = new ResultsTable();
		Double min_size = 0.0;
		Double max_size = Double.POSITIVE_INFINITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_ROI_MASKS, 0, blobs, min_size, max_size);
		pa.analyze(imp);
		mask = imp.getProcessor();
		mask.invert();
		
		return mask;
		
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
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW, 
				0, new ResultsTable(), size, Double.POSITIVE_INFINITY);
		pa.analyze(ip);
		result = ip.getProcessor();
		result.invert();
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
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_ROI_MASKS, Measurements.AREA, blobs, min_size, max_size);
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
		filter.rank(tempBoundaries, 1.5, RankFilters.MIN);
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
		boundariesTemp.invert();
		ImagePlus conComp = new ImagePlus("Temp", boundariesTemp.duplicate());
		ResultsTable cc = new ResultsTable();
		Double min_size = 0.0;
		Double max_size = Double.POSITIVE_INFINITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.IN_SITU_SHOW | ParticleAnalyzer.SHOW_ROI_MASKS, 0, cc, min_size, max_size);
		pa.analyze(conComp);	
		return conComp.getProcessor().convertToByteProcessor();
	}
	
}
