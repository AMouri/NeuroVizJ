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
	
	public String toString(){
		return "(" + x + ", " + y + ")";
	}
}
