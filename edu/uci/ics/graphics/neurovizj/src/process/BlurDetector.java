package edu.uci.ics.graphics.neurovizj.src.process;

import ij.process.ImageProcessor;

/**
 * Detects whether images are sharp enough for segmentation
 * @author Alec
 *
 */
public class BlurDetector {
	
	private static int[] laplaceKernel = {0, -1, 0, -1, 4, -1, 0, -1, 0};

	/**
	 * Performs blur detection by convolving the image with the laplacian kernel and comparing the brightest pixel
	 * @param image
	 * @param threshold
	 * @return
	 */
	public static boolean detectBlur(ImageProcessor image, double threshold){
		// method taken from http://stackoverflow.com/questions/7765810/is-there-a-way-to-detect-if-an-image-is-blurry
		ImageProcessor dummy = image.convertToFloatProcessor();
		dummy.resetMinAndMax();
		dummy.multiply(1.0 / dummy.getMax()); //normalize image
		dummy.resetMinAndMax();
		dummy.convolve3x3(laplaceKernel);
		dummy.resetMinAndMax();
		double maxValue = dummy.getMax();
		System.out.println(maxValue);
		System.out.println(threshold < maxValue);
		return threshold > maxValue;
	}
}
