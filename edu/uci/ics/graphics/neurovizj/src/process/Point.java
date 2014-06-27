package edu.uci.ics.graphics.neurovizj.src.process;

/**
 * Wrapper class for point in an image
 * @author Alec
 *
 */
public class Point {
	private int x;
	private int y;
	
	/**
	 * Constructs a point at x and y
	 * @param x
	 * @param y
	 */
	public Point(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Gets the x-coordinate
	 * @return
	 */
	public int getX(){
		return x;
	}
	
	/**
	 * Gets the y-coordinate
	 * @return
	 */
	public int getY(){
		return y;
	}
	
	/**
	 * Retruns whether this point is equal to o
	 * Equality is true when the respective coordinates are equal to each other
	 */
	public boolean equals(Object o){
		return this.x == ((Point) o).getX() && this.y == ((Point) o).getY();
	}
	
	@Override
	public int hashCode(){
		int hash = 7;
	    hash = 71 * hash + this.x;
	    hash = 71 * hash + this.y;
	    return hash;
	}
	
	public String toString(){
		return "(" + x + ", " + y + ")";
	}
}
