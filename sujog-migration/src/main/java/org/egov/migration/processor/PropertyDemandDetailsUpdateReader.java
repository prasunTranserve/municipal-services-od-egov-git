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
import org.egov.migration.mapper.PropertyDemandDetailsRowMapper;
import org.egov.migration.reader.model.DemandDetailPaymentMapper;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyDemandDetailsUpdateReader implements ItemReader<DemandDetailPaymentMapper> {
	
	private String file;
	private String demandReqObj;
	private String businessService;
	private String tenantId;
	private String ulb;
	
	private int propertyRowIndex;
	private int skipRecord;
	private Iterator<Row> propertyRowIterator;
	
	private Map<String, Integer> propertyColMap;
	
	@Autowired
	private PropertyDemandDetailsRowMapper demandDetailsRowMapper;
	
	public PropertyDemandDetailsUpdateReader() throws EncryptedDocumentException, IOException, Exception {
		this.propertyRowIndex = 0;
	}
	
	public PropertyDemandDetailsUpdateReader(String file) throws EncryptedDocumentException, IOException, Exception {
		this.file = file;
		this.propertyRowIndex = 0;
	}

	@Value("#{jobParameters['filePath']}")
	public void setFile(final String fileName) {
		this.file = fileName;
	}
	
	@Value("#{jobParameters['demandDetailSearchRequest']}")
	public void setDemandReqObj(final String demandReq) {
		this.demandReqObj = demandReq;
	}

	@Value("#{jobParameters['businessService']}")
	public void setBusinessService(String businessService) {
		this.businessService = businessService;
	}

	@Value("#{jobParameters['tenantId']}")
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public DemandDetailPaymentMapper read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if(this.propertyRowIndex==0) {
			readDemandDetail();
			skipRecord();
		}
		
		if(this.propertyRowIterator.hasNext()) {
			this.propertyRowIndex++;
			return getDemand(this.propertyRowIterator.next());
		}
		
		return null;
	}
	
	private void readDemandDetail() throws EncryptedDocumentException, IOException {
		File inputFile = new File(this.file);
		this.ulb = inputFile.getName().split("\\.")[0].toLowerCase();
		
		Workbook workbook = WorkbookFactory.create(inputFile);
		Sheet propertySheet = workbook.getSheet(MigrationConst.SHEET_PROPERTY);
		this.propertyRowIterator = propertySheet.iterator();
		
		updatePropertyColumnMap(propertySheet.getRow(0));
		
	}
	
	private void skipRecord() {
		for(int i=1; i<=this.skipRecord; i++) {
			if(propertyRowIterator.hasNext()) {
				this.propertyRowIterator.next();
				this.propertyRowIndex++;
			}
		}
	}
	
	private void updatePropertyColumnMap(Row row) {
		this.propertyColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.propertyColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}
	
	private DemandDetailPaymentMapper getDemand(Row demandRow) {
		DemandDetailPaymentMapper demand = new DemandDetailPaymentMapper();
		try {
			demand = demandDetailsRowMapper.mapRow(this.ulb, demandRow, this.propertyColMap);
			log.info("Demand: "+demand.getDemandid()+" Paid.."+demand.getAmountpaid()+"..COll.."+demand.getCollectionamount());
			if(MigrationUtility.isDemandEmpty(demand)) {
				return null;
			}
			
			log.info("Demand: "+demand.getDemandid()+" read successfully");
			demand.setRequestInfo(demandReqObj);
			demand.setBusinessService(businessService);
			demand.setTenantId(tenantId);
		} catch (Exception e) {
			String demandId = propertyColMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(demandRow.getCell(propertyColMap.get(MigrationConst.COL_DEMAND_ID)), false);
			log.error(String.format("Some exception generated while reading demand %s", demand.getDemandid()));
			MigrationUtility.addError(demandId, "Not able to read the data. Check the data");
			MigrationUtility.addError(demandId, e.getMessage());
		}
		
		return demand;
		
	}
	
	public void setSkipRecord(int skipRecord) {
		this.skipRecord = skipRecord;
	}

}

