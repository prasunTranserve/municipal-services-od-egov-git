package org.egov.migration.processor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.migration.mapper.PropertyRowMapper;
import org.egov.migration.reader.model.Property;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PtFetchbillItemReader implements ItemReader<Property> {

	private String file;
	private String ulb;
	
	private int connectionRowIndex;
	private Iterator<Row> connectionRowIterator;
	private int skipRecord;
	
	private Map<String, Integer> connectionColMap;
	
	@Autowired
	private PropertyRowMapper propertyRowMapper;
	
	public PtFetchbillItemReader() throws EncryptedDocumentException, IOException, Exception {
		this.connectionRowIndex = 0;
	}
	
	public PtFetchbillItemReader(String file) throws EncryptedDocumentException, IOException, Exception {
		this.file = file;
		this.connectionRowIndex = 0;
	}
	
	@Value("#{jobParameters['filePath']}")
	public void setFile(final String fileName) {
		this.file = fileName;
	}
	
	public void setSkipRecord(int skipRecord) {
		this.skipRecord = skipRecord;
	}

	@Override
	public Property read()
			throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if(this.connectionRowIndex==0) {
			readConnection();
			skipRecord();
		}
		
		if(this.connectionRowIterator.hasNext()) {
			this.connectionRowIndex++;
			return getProperty(this.connectionRowIterator.next());
		}
		return null;
	}

	private Property getProperty(Row propertyRow) {
		Property property = propertyRowMapper.mapRow(this.ulb, propertyRow, this.connectionColMap);
		log.info("ConnectionNo: "+property.getPropertyId()+" reading...");
		return property;
	}

	private void readConnection() throws EncryptedDocumentException, IOException {
		File inputFile = new File(this.file);
		this.ulb = inputFile.getName().split("\\.")[0].toLowerCase();
		
		Workbook workbook = WorkbookFactory.create(inputFile);
		Sheet connectionSheet = workbook.getSheet("CONNECTION");
		
		updateConnectionColumnMap(connectionSheet.getRow(0));
		
		this.connectionRowIterator = connectionSheet.iterator();
	}
	
	private void updateConnectionColumnMap(Row row) {
		this.connectionColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.connectionColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void skipRecord() {
		for(int i=1; i<=this.skipRecord; i++) {
			if(connectionRowIterator.hasNext()) {
				this.connectionRowIterator.next();
				this.connectionRowIndex++;
			}
		}
	}
}
