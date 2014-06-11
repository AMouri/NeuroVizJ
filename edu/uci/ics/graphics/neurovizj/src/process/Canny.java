package edu.uci.ics.graphics.neurovizj.src.process;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.LinkedList;
import java.util.List;

public class Canny {
	
	private double thresholdRatio;
	private double percentNotEdges;
	private double sigma;
	
	private enum Direction{
		EAST, NORTHEAST, NORTH, NORTHWEST
	}
	
	public Canny(double thresh, double perc, double sigma){
		this.thresholdRatio = thresh;
		this.percentNotEdges = perc;
		this.sigma = sigma;
	}
	
	public ImageProcessor canny(ImageProcessor ip){
		//Filter out noise
		ImageProcessor result = ip.duplicate();
		result.blurGaussian(sigma);
		
		//Compute the intensity gradient using Sobel
		ImageProcessor Gx = xGradient(result);
		ImageProcessor Gy = yGradient(result);
	
		//Compute the magnitude of the gradient
		FloatProcessor gradientMag = gradientStrength(Gx, Gy);
		gradientMag.resetMinAndMax();
		
		if(gradientMag.getMax() > 0) gradientMag.multiply(1.0/gradientMag.getMax());
		
		
		//Perform non-Maximum Supression to remove weak edges
		ImageProcessor suppressed = nonMaxSuppression(Gx, Gy, gradientMag);
		
		
		double highThreshold = getHighThreshold(gradientMag.convertToByteProcessor(false), percentNotEdges);
		double lowThreshold = thresholdRatio * highThreshold;
		
		//now perform hysterisis
		result = doHysterisis(suppressed, gradientMag, lowThreshold, highThreshold);
		
		return result;
	}
	
	/**
	 * Computes the horizontal gradient of ip
	 * @param ip
	 * @return
	 */
	private static ImageProcessor xGradient(ImageProcessor ip){
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
		ImageProcessor result = ip.duplicate();
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
	private static FloatProcessor gradientStrength(ImageProcessor x, ImageProcessor y){
		FloatProcessor result = new FloatProcessor(x.getWidth(), x.getHeight());
		for(int i = 1; i <= x.getWidth(); i++){
			for(int j = 1; j <= x.getHeight(); j++){
				float xCor = Float.intBitsToFloat(x.get(i-1,j-1));
				float yCor = Float.intBitsToFloat(y.get(i-1,j-1));
				result.putPixelValue(i, j, (float) Math.hypot(xCor, yCor));
			}
		}
		return result;
	}
	
	/**
	 * Performs non-maximum suppression with gradient described by (x,y) with magnitude g
	 * @param x
	 * @param y
	 * @return
	 */
	private static ImageProcessor nonMaxSuppression(ImageProcessor x, ImageProcessor y, FloatProcessor g){
		ImageProcessor result = new ByteProcessor(x.getWidth(), x.getHeight());
		for(int i = 0; i < x.getWidth(); i++){
			for(int j = 0; j < x.getHeight(); j++){
				double xCoord = Float.intBitsToFloat(x.get(i,j));
				double yCoord = Float.intBitsToFloat(y.get(i,j));
				double mag = Float.intBitsToFloat(g.get(i,j));
				double trueAngle = Math.atan(yCoord/xCoord);
				if(!Double.isNaN(trueAngle)){
					Direction dir = approxAngle(trueAngle);
					switch(dir){
						case EAST:
							result.set(i, j, 
									(Float.intBitsToFloat(g.getPixel(i-1, j)) < mag && Float.intBitsToFloat(g.getPixel(i+1, j)) < mag) ? 255 : 0);
							break;
						case NORTHEAST:
							result.set(i, j, 
									(Float.intBitsToFloat(g.getPixel(i-1, j-1)) < mag && Float.intBitsToFloat(g.getPixel(i+1, j+1)) < mag) ? 255 : 0);
							break;
						case NORTH:
							result.set(i, j, 
									(Float.intBitsToFloat(g.getPixel(i, j-1)) < mag && Float.intBitsToFloat(g.getPixel(i, j+1)) < mag) ? 255 : 0);
							break;
						case NORTHWEST:
							result.set(i, j, 
									(Float.intBitsToFloat(g.getPixel(i-1, j+1)) < mag && Float.intBitsToFloat(g.getPixel(i+1, j-1)) < mag) ? 255 : 0);
							break;
					}
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
		} else {
			return Direction.NORTH;
		}
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
	
	private static ImageProcessor doHysterisis(ImageProcessor edges, ImageProcessor gradient, double low, double high){
		ImageProcessor result = new ByteProcessor(edges.getWidth(), edges.getHeight());
		ImageProcessor med = result.duplicate();
		
		LinkedList<Integer> candidates = new LinkedList<Integer>();
		
		//seek strong and medium edges
		for(int i = 0; i < result.getWidth(); i++){
			for(int j = 0; j < result.getHeight(); j++){
				if(edges.get(i,j) == 255){
					double intensity = Float.intBitsToFloat(gradient.get(i,j));
					if(intensity > high){
						result.set(i,j,255);
					}
					if(intensity > low){
						candidates.add(i*result.getHeight() + j);
						med.set(i,j,255);
					}
				}
			}
		}
		//now look for medium edges that form some path to strong edges
		return search(candidates, result, med);
		
	}
	
	
	
	private static ImageProcessor search(List<Integer> candidates, ImageProcessor strong, ImageProcessor med){
		ImageProcessor result = strong.duplicate();
		for(Integer pt : candidates){
			int x = pt / strong.getHeight();
			int y = pt % strong.getHeight();
			if(result.get(x,y) != 255){
				if(searchHelp(x, y, strong, med, new boolean[strong.getWidth()][strong.getHeight()])){
					result.set(x,y, 255);
				}
			} else {
				result.set(x,y,255);
			}
		}
		return result;
	}
	
	private static boolean searchHelp(int x, int y, ImageProcessor strong, ImageProcessor med, boolean[][] visited){
		visited[x][y] = true;
		for(int i = x - 1; i <= x + 1; i++){
			for(int j = y - 1; j <= y+1; j++){
				if(strong.getPixel(i,j) == 255){
					return true;
				}
			}
		}
		
		for(int i = x - 1; i <= x + 1; i++){
			for(int j = y - 1; j <= y+1; j++){
				if(med.getPixel(i,j) == 255 && !visited[i][j] && searchHelp(i, j, strong, med, visited)){
					return true;
				}
			}
		}
		
		return false;
	}
}
