package edu.uci.ics.graphics.neurovizj.src.process;

/**
 * Wrapper class for point in an image
 * @author Alec
 *
 */
public class Point {
	private int x;
	private int y;
	
	public Point(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
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
