package org.egov.migration.processor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.migration.mapper.ConnectionHolderRowMapper;
import org.egov.migration.mapper.ConnectionRowMapper;
import org.egov.migration.mapper.ConnectionServiceRowMapper;
import org.egov.migration.mapper.MeterReadingRowMapper;
import org.egov.migration.mapper.WnsDemandRowMapper;
import org.egov.migration.reader.model.WnsConnection;
import org.egov.migration.reader.model.WnsConnectionHolder;
import org.egov.migration.reader.model.WnsDemand;
import org.egov.migration.reader.model.WnsMeterReading;
import org.egov.migration.reader.model.WnsConnectionService;
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
public class WnsItemReader implements ItemReader<WnsConnection> {
	
	private String file;
	private String ulb;
	
	private int connectionRowIndex;
	private Iterator<Row> connectionRowIterator;
	private int skipRecord;
	
	private Map<String, Integer> activeConnectionCountMap;
	
	private Map<String, Integer> connectionColMap;
	private Map<String, Integer> serviceColMap;
	private Map<String, Integer> meterReadingColMap;
	private Map<String, Integer> holderColMap;
	private Map<String, Integer> demandColMap;
	
	private Map<String, Integer> serviceRowMap;
	private Map<String, List<Integer>> meterReadingRowMap;
	private Map<String, Integer> holderRowMap;
	private Map<String, List<Integer>> demandRowMap;
	
	private Sheet serviceSheet;
	private Sheet meterReadingSheet;
	private Sheet holderSheet;
	private Sheet demandSheet;
	
	@Autowired
	private ConnectionRowMapper connectionRowMapper;
	
	@Autowired
	private ConnectionServiceRowMapper connectionServiceRowMapper;
	
	@Autowired
	private ConnectionHolderRowMapper connectionHolderRowMapper;
	
	@Autowired
	private MeterReadingRowMapper meterReadingRowMapper;
	
	@Autowired
	private WnsDemandRowMapper demandRowMapper;
	
	public WnsItemReader() throws EncryptedDocumentException, IOException, Exception {
		this.connectionRowIndex = 0;
	}
	
	public WnsItemReader(String file) throws EncryptedDocumentException, IOException, Exception {
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
	public WnsConnection read()
			throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if(this.connectionRowIndex==0) {
			readConnection();
			skipRecord();
		}
		
//		if(this.connectionRowIterator.hasNext()) {
//			this.connectionRowIndex++;
//			return getConnection(this.connectionRowIterator.next());
//		}
		
		WnsConnection connection = null;
		do {
			connection = null;
			if(this.connectionRowIterator.hasNext()) {
				this.connectionRowIndex++;
				connection = getConnection(this.connectionRowIterator.next());
			}
			if(connection != null && activeConnectionCountMap.get(connection.getConnectionNo()) != null && activeConnectionCountMap.get(connection.getConnectionNo())>1) {
				MigrationUtility.addError(connection.getConnectionNo(), String.format("%s Approved and Active connection present", activeConnectionCountMap.get(connection.getConnectionNo())));
			}
		} while(connection != null && activeConnectionCountMap.get(connection.getConnectionNo()) != null && activeConnectionCountMap.get(connection.getConnectionNo())>1
				&& this.connectionRowIterator.hasNext());
		
		return connection;
	}

	private WnsConnection getConnection(Row connectionRow) {
		WnsConnection connection = connectionRowMapper.mapRow(this.ulb, connectionRow, this.connectionColMap);
		log.info("ConnectionNo: "+connection.getConnectionNo()+" reading...");
		
		WnsConnectionService service = getConnectionService(connection.getConnectionNo());
		WnsConnectionHolder holder = getConnectionHolder(connection.getConnectionNo());
		List<WnsMeterReading> meterReading = getMeterReading(connection.getConnectionNo());
		List<WnsDemand> demand = getDemand(connection.getConnectionNo());
		
		connection.setService(service);
		connection.setConnectionHolder(holder);
		connection.setMeterReading(meterReading);
		connection.setDemands(demand);
		
		log.info("ConnectionNo: "+connection.getConnectionNo()+" read successfully");
		return connection;
	}

	private List<WnsDemand> getConsolidateDemand(List<WnsDemand> demand1, List<WnsDemand> demand2) {
		if(demand1 != null) {
			demand1.addAll(demand2);
			return demand1;
		} else {
			return demand2;
		}
	}

	private List<WnsDemand> getDemand(String connectionNo) {
		connectionNo = MigrationUtility.addLeadingZeros(connectionNo);
		if(this.demandRowMap.get(MigrationUtility.addLeadingZeros(connectionNo)) == null)
			return null;
		return this.demandRowMap.get(connectionNo).stream().map(rowIndex -> demandRowMapper.mapRow(this.ulb, demandSheet.getRow(rowIndex), this.demandColMap)).collect(Collectors.toList());
	}

	private List<WnsMeterReading> getMeterReading(String connectionNo) {
		connectionNo = MigrationUtility.addLeadingZeros(connectionNo);
		if(this.meterReadingRowMap.get(connectionNo) == null)
			return null;
		//return meterReadingRowMapper.mapRow(this.ulb, this.meterReadingSheet.getRow(this.meterReadingRowMap.get(connectionNo)), this.meterReadingColMap);
		return this.meterReadingRowMap.get(connectionNo).stream().map(rowIndex -> meterReadingRowMapper.mapRow(this.ulb, meterReadingSheet.getRow(rowIndex), this.meterReadingColMap)).collect(Collectors.toList());
	}

	private WnsConnectionHolder getConnectionHolder(String connectionNo) {
		connectionNo = MigrationUtility.addLeadingZeros(connectionNo);
		if(this.holderRowMap.get(connectionNo) == null)
			return null;
		return connectionHolderRowMapper.mapRow(this.ulb, this.holderSheet.getRow(this.holderRowMap.get(connectionNo)), this.holderColMap);
	}

	private WnsConnectionService getConnectionService(String connectionNo) {
		connectionNo = MigrationUtility.addLeadingZeros(connectionNo);
		if(this.serviceRowMap.get(connectionNo) == null)
			return null;
		return connectionServiceRowMapper.mapRow(this.ulb, this.serviceSheet.getRow(this.serviceRowMap.get(connectionNo)), this.serviceColMap);
	}

	private void readConnection() throws EncryptedDocumentException, IOException {
		File inputFile = new File(this.file);
		this.ulb = inputFile.getName().split("\\.")[0].toLowerCase();
		
		Workbook workbook = WorkbookFactory.create(inputFile);
		Sheet connectionSheet = workbook.getSheet(MigrationConst.SHEET_CONNECTION);
		
		updateConnectionColumnMap(connectionSheet.getRow(0));
		searchDupplicateActiveConnection(connectionSheet);
		
		this.connectionRowIterator = connectionSheet.iterator();
		updatConnectionServiceRowMap(workbook);
		updateHolderRowMap(workbook);
		updateMeterReadingRowMap(workbook);
		updateDemandRowMap(workbook);
	}

	private void searchDupplicateActiveConnection(Sheet connectionSheet) {
		activeConnectionCountMap = new HashMap<>();
		Iterator<Row> connections = connectionSheet.iterator();
		while(connections.hasNext()) {
			WnsConnection connection = connectionRowMapper.mapRow(this.ulb, connections.next(), this.connectionColMap);
			if(MigrationConst.APPLICATION_APPROVED.equalsIgnoreCase(connection.getApplicationStatus())
					&& MigrationConst.STATUS_ACTIVE.equalsIgnoreCase(connection.getStatus())) {
				if(activeConnectionCountMap.get(connection.getConnectionNo())==null) {
					activeConnectionCountMap.put(connection.getConnectionNo(), 1);
				} else {
					activeConnectionCountMap.put(connection.getConnectionNo(), activeConnectionCountMap.get(connection.getConnectionNo())+1);
				}
			}
		}
	}

	private void updateDemandRowMap(Workbook workbook) {
		this.demandRowMap = new HashMap<>();
		this.demandSheet = workbook.getSheet(MigrationConst.SHEET_DEMAND);
		this.demandSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateDemandColumnMap(row);
			} else {
				String connectionNo = MigrationUtility.readCellValue(row.getCell(this.demandColMap.get(MigrationConst.COL_CONNECTION_NO)), false);
				List<Integer> list = this.demandRowMap.get(connectionNo);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.demandRowMap.put(MigrationUtility.addLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.demandColMap.get(MigrationConst.COL_CONNECTION_NO)), false)), list);
			}
		});
		
	}
	
	private void updateDemandColumnMap(Row row) {
		this.demandColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.demandColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
		
	}

	private void updateMeterReadingRowMap(Workbook workbook) {
		this.meterReadingRowMap = new HashMap<>();
		this.meterReadingSheet = workbook.getSheet(MigrationConst.SHEET_METER_READING);
		this.meterReadingSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateMeterReadingColumnMap(row);
			} else {
				//this.meterReadingRowMap.put(MigrationUtility.addLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.meterReadingColMap.get(MigrationConst.COL_CONNECTION_NO)), false)), row.getRowNum());

				String propertyId = MigrationUtility.readCellValue(row.getCell(this.meterReadingColMap.get(MigrationConst.COL_CONNECTION_NO)), false);
				List<Integer> list = this.meterReadingRowMap.get(propertyId);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.meterReadingRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.meterReadingColMap.get(MigrationConst.COL_CONNECTION_NO)), false)), list);
			
			}
		});
		
	}

	private void updateMeterReadingColumnMap(Row row) {
		this.meterReadingColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.meterReadingColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void updateHolderRowMap(Workbook workbook) {
		this.holderRowMap = new HashMap<>();
		this.holderSheet = workbook.getSheet(MigrationConst.SHEET_CONNECTION_HOLDER);
		this.holderSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateHolderColumnMap(row);
			} else {
				this.holderRowMap.put(MigrationUtility.addLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.holderColMap.get(MigrationConst.COL_CONNECTION_NO)), false)), row.getRowNum());
			}
		});
		
	}

	private void updateHolderColumnMap(Row row) {
		this.holderColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.holderColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void updatConnectionServiceRowMap(Workbook workbook) {
		this.serviceRowMap = new HashMap<>();
		this.serviceSheet = workbook.getSheet(MigrationConst.SHEET_CONNECTION_SERVICE);
		this.serviceSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateserviceColumnMap(row);
			} else {
				this.serviceRowMap.put(MigrationUtility.addLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.serviceColMap.get(MigrationConst.COL_CONNECTION_NO)), false)), row.getRowNum());
			}
		});
	}

	private void updateserviceColumnMap(Row row) {
		this.serviceColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.serviceColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
		
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
