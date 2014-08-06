package edu.uci.ics.graphics.neurovizj.src.io;

import java.io.FileOutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import edu.uci.ics.graphics.neurovizj.src.process.ProcessedCell;
import edu.uci.ics.graphics.neurovizj.src.process.SegmentedImage;

/**
 * Performs exportation into .xls spreadsheets
 * @author Alec
 *
 */
public class ExcelExporter {

	/**
	 * Exports the spreadsheet for a segmented image with the given output name
	 * @param image
	 * @param oName
	 */
	public static void exportImageAsSpreadSheet(SegmentedImage image, String oName){
		Workbook wb = new HSSFWorkbook();
		
		dumpImageToSheet(image, wb);
		
		try{
			FileOutputStream out = new FileOutputStream(oName + ".xls");
			wb.write(out);
			out.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Exports the spreadsheet for a sequence of images with a given output name
	 * @param images
	 * @param oName
	 */
	public static void exportSequenceAsSpreadSheet(SegmentedImage[] images, String oName){
		Workbook wb = new HSSFWorkbook();
		
		for(SegmentedImage image : images){
			dumpImageToSheet(image, wb);
		}
		
		try{
			FileOutputStream out = new FileOutputStream(oName + ".xls");
			wb.write(out);
			out.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Dumps image data into an Apache POI workbook
	 * @param image
	 * @param wb
	 */
	private static void dumpImageToSheet(SegmentedImage image, Workbook wb){
		Sheet s = wb.createSheet();
		
		generateHeader(s);
		
		for(int rownum = 1; rownum <= image.numCells(); rownum++){
			fillData(s, rownum, image.getCell(rownum-1));
		}
	}
	
	/**
	 * Generates a labels for a sheet that will hold data for each cell in an image
	 * @param s
	 */
	private static void generateHeader(Sheet s){
		Row r = s.createRow(0);
		String[] headers = ProcessedCell.getTags();
		Cell c = null;
		for(int i = 0; i < headers.length; i++){
			c = r.createCell(i);
			c.setCellValue(headers[i]);
		}
	}
	
	/**
	 * Populates a sheet's row with a cell's data
	 * @param s
	 * @param rownum
	 * @param cell
	 */
	private static void fillData(Sheet s, int rownum, ProcessedCell cell){
		//Assume that a header was already generated
		Row r = s.createRow(rownum);
		String[] headers = ProcessedCell.getTags();
		Cell c = null;
		for(int i = 0; i < headers.length; i++){
			c = r.createCell(i);
			c.setCellValue(cell.getAttribute(headers[i]));
		}
	}
}
