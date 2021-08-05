package org.egov.tl.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.tl.web.models.excel.RowExcel;
import org.springframework.stereotype.Service;

import com.monitorjbl.xlsx.StreamingReader;

@Service
public class ExcelService {

	
	public void read(InputStream is, Long skip, Long limit, Function<RowExcel, Boolean> func) throws Exception{
		Workbook workbook = StreamingReader.builder().rowCacheSize(100).bufferSize(4096).open(is);
		for (Sheet sheet: workbook) {
			Map<Integer, String> headerMap = new HashMap<Integer, String>();
			Iterator<Row> itr =  sheet.rowIterator();
			Row r = itr.next();
			for (Cell cell: r) {
				headerMap.put(cell.getColumnIndex(), cell.getStringCellValue());
			}

			while(itr.hasNext()){
				while ( (skip != null && r.getRowNum()+2 <=skip)) { if(itr.hasNext()) r= itr.next();}

				if(itr.hasNext()) r = itr.next();

				if(r.getPhysicalNumberOfCells() <= 0) break;
				Map<Integer, Cell> cellMap = new HashMap<Integer, Cell>();
				for (Cell cell: r) {
					cellMap.put(cell.getColumnIndex(),cell);
				}
				if(r.getRowNum() > 0) func.apply(RowExcel.builder().rowIndex(r.getRowNum()+1).cells(cellMap).header(headerMap).build());
				
				if(limit != null && r.getRowNum()+2 >= limit) break;
			}
		}
		workbook.close();
	}
	
	
}
