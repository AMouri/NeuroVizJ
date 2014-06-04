package edu.uci.ics.graphics.neurovizj.src.process;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
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
	private enum Direction{
		EAST, NORTHEAST, NORTH, NORTHWEST
	}
	
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
		adjustedImg.convertToFloat();
		//adjustedImg.multiply(1/255.0);
		
		//now median filter with a 5x5 kernel to smooth the mask
		RankFilters medFilt = new RankFilters();
		medFilt.rank(adjustedImg, 2, RankFilters.MEDIAN);
		
		//apply mask
		ImageProcessor minSuppres = input.getOrig().getProcessor();
		minSuppres.copyBits(adjustedImg, 0, 0, Blitter.AND);
		
		minSuppres = minSuppres.convertToFloatProcessor();
		//minSuppres.multiply(1.0/255);

		//Now perform difference of Gaussians
		ImageProcessor en = gaussDifference(minSuppres, 8.0, 5.0).convertToFloatProcessor();
		//en.multiply(1.0/255);
		
		en.copyBits(minSuppres, 0, 0, Blitter.MULTIPLY);
		
		//en.multiply(255*255);
		
		//en = en.convertToByteProcessor(false);
		ImageProcessor e = canny(en, Math.sqrt(2));
		
		return new ImagePlus("Hello", e); //TODO: change was completed testing
	}
	
	/**
	 * Performs difference of 2 gaussians on ip, weighted by weight 1 and weight 2.
	 * Note that ip is not changed.
	 * @param ip
	 * @param weight1
	 * @param weight2
	 * @return
	 */
	private static ImageProcessor gaussDifference(ImageProcessor ip, double weight1, double weight2){
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
	
	private static ImageProcessor canny(ImageProcessor ip, double sigma){
		double thresholdRatio = 0.4;
		double percentNotEdges = 0.7;
		//Filter out noise
		ImageProcessor result = ip.duplicate();
		result.blurGaussian(sigma);
		//result.convertToFloat();
		//result.multiply(1/255.0);
		//Compute the intensity gradient using Sobel
		ImageProcessor Gx = xGradient(result);
		ImageProcessor Gy = yGradient(result);
		
		//Compute the magnitude of the gradient
		float[][] gradientMag = gradientStrength(Gx, Gy);
		
		//Perform non-Maximum Supression to remove weak edges
		ImageProcessor suppressed = nonMaxSuppression(Gx, Gy, gradientMag);
		
		//Determine hysterisis threshold
		double highThreshold = getHighThreshold(new ByteProcessor(new FloatProcessor(gradientMag), true), percentNotEdges);
		double lowThreshold = thresholdRatio * highThreshold;
		
		System.out.println(highThreshold + " " + lowThreshold);
		
		//now perform hysterisis
		result = doHysterisis(suppressed, lowThreshold, highThreshold);
		//result.multiply(255.0);
		
		return result;
	}
	
	/**
	 * Computes the horizontal gradient of ip
	 * @param ip
	 * @return
	 */
	private static ImageProcessor xGradient(ImageProcessor ip){
		//ImageProcessor result = new TypeConverter(ip, false).convertToFloat(null);
		ImageProcessor result = ip.duplicate();
		int[] Gx = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
		result.convolve3x3(Gx);
		return result;
	}
	
	/**
	 * Computers the vertical gradient of ip
	 * @param ip
	 * @return
	 */
	private static ImageProcessor yGradient(ImageProcessor ip){
		ImageProcessor result = new TypeConverter(ip, false).convertToFloat(null);
		int[] Gy = {-1, -2, -1, 0, 0, 0, 1, 2, 1};
		result.convolve3x3(Gy);
		return result;
	}
	
	/**
	 * Computes the strength of the gradient described by (x, y)
	 * @param x
	 * @param y
	 * @return
	 */
	private static float[][] gradientStrength(ImageProcessor x, ImageProcessor y){
		float[][] result = new float[x.getWidth()+2][x.getHeight()+2];
		for(int i = 1; i <= x.getWidth(); i++){
			for(int j = 1; j <= x.getHeight(); j++){
				float xCor = Float.intBitsToFloat(x.get(i-1,j-1));
				float yCor = Float.intBitsToFloat(y.get(i-1,j-1));
				result[i][j] = (float) Math.sqrt(Math.pow(xCor,2) + Math.pow(yCor, 2));
			}
		}
		return result;
	}
	
	/**
	 * Performs non-maximum suppression with gradient described by (x,y) with maginitude g
	 * @param x
	 * @param y
	 * @return
	 */
	private static ImageProcessor nonMaxSuppression(ImageProcessor x, ImageProcessor y, float[][] g){
		ImageProcessor result = new ByteProcessor(x.getWidth(), x.getHeight());
		for(int i = 0; i < x.getWidth(); i++){
			for(int j = 0; j < x.getHeight(); j++){
				double xCoord = x.get(i,j);
				double yCoord = y.get(i,j);
				double mag = g[i+1][j+1];
				double trueAngle = Math.atan(yCoord/xCoord);
				Direction dir = approxAngle(trueAngle);
				switch(dir){
					case EAST:
						result.set(i, j, 
								(g[i][j+1] < mag && g[i+2][j+1] < mag) ? 255 : 0);
						break;
					case NORTHEAST:
						result.set(i, j, 
								(g[i][j] < mag && g[i+2][j+2] < mag) ? 255 : 0);
						break;
					case NORTH:
						result.set(i, j, 
								(g[i+1][j] < mag && g[i+1][j+2] < mag) ? 255 : 0);
						break;
					case NORTHWEST:
						result.set(i, j, 
								(g[i][j+2] < mag && g[i+2][j] < mag) ? 255 : 0);
						break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Gives an approximate direction of an edge for non-maximum suppression
	 * @param theta
	 * @return
	 */
	private static Direction approxAngle(double theta){
		if(theta <= Math.PI/8 && theta >= -Math.PI/8){
			return Direction.EAST;
		} else if (theta <= 3*Math.PI/8 && theta >= Math.PI/8){
			return Direction.NORTHEAST;
		} else if (theta <= -Math.PI/8 && theta >= -3*Math.PI/8){
			return Direction.NORTHWEST;
		} else return Direction.NORTH;
	}
	
	/**
	 * Generates the high threshold for hysterisis thresholding
	 * @param gradient
	 * @param percentNotEdges
	 * @return
	 */
	private static double getHighThreshold(ImageProcessor gradient, double percentNotEdges){
		gradient.setHistogramSize(64);
		int[] histogram = gradient.getHistogram();
		long cumulative = 0;
		long totalNotEdges = Math.round(gradient.getHeight()*gradient.getWidth()*percentNotEdges);
		int buckets = 0;
		for(; buckets < 64 && cumulative < totalNotEdges; buckets++){
			cumulative += histogram[buckets];
		}
		return buckets/64.0;
	}
	
	private static ImageProcessor doHysterisis(ImageProcessor edges, double low, double high){
		ImageProcessor result = new ByteProcessor(edges.getWidth(), edges.getHeight());
		ImageProcessor med = result.duplicate();
		
		LinkedList<Integer> candidates = new LinkedList<Integer>();
		
		//seek strong and medium edges
		for(int i = 0; i < result.getWidth(); i++){
			for(int j = 0; j < result.getHeight(); j++){
				double intensity = edges.get(i,j);
				if(intensity > high){
					result.set(i,j,255);
				}
				if(intensity > low){
					candidates.add(i*result.getWidth() + j);
					med.set(i,j,255);
				}
			}
		}
		//now look for medium edges that form some path to strong edges
		return search(candidates, result, med, high);
		
	}
	
	
	
	private static ImageProcessor search(List<Integer> candidates, ImageProcessor strong, ImageProcessor med, double high){
		ImageProcessor result = strong.duplicate();
		for(Integer pt : candidates){
			int x = pt/strong.getWidth();
			int y = pt % strong.getWidth();
			if(result.get(x,y) != 255){
				if(searchHelp(x, y, strong, med, new boolean[strong.getWidth()][strong.getHeight()], high)){
					result.set(x,y, 255);
				}
			}
		}
		return result;
	}
	
	private static boolean searchHelp(int x, int y, ImageProcessor strong, ImageProcessor med, boolean[][] visited, double high){
		visited[x][y] = true;
		for(int i = x - 1; i <= x + 1; i++){
			for(int j = y - 1; j <= y+1; j++){
				if(i != x || j != y){
					if(strong.getPixel(i,j) == 255){
						return true;
					}
				}
			}
		}
		
		for(int i = x - 1; i <= x + 1; i++){
			for(int j = y - 1; j <= y+1; j++){
				if(i != x || j != y){
					if(med.getPixel(i,j) == 255 && !visited[i][j] && searchHelp(i, j, strong, med, visited, high)){
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
}
