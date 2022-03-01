package org.egov.migration.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.business.model.ConnectionResponse;
import org.egov.migration.business.model.DemandDTO;
import org.egov.migration.business.model.MeterReadingDTO;
import org.egov.migration.business.model.WaterConnectionDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.common.model.RequestInfo;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.reader.model.WSConnection;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WnsService {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private PropertiesData properties;

	@Autowired
	private RemoteService remoteService;

	@Autowired
	private RecordStatistic recordStatistic;

	public void migrateConnection(ConnectionDTO conn) {
		
		if (conn.isWater()) {
			boolean isWaterMigrated = false;
			try {
				isWaterMigrated = migrateWaterConnection(conn);
				if(isWaterMigrated) {
					// success for water connection Migration
					MigrationUtility.addSuccessForWaterConnection(conn.getWaterConnection());
				}
			} catch (Exception e) {
				log.error(e.getLocalizedMessage());
				MigrationUtility.addError(conn.getWaterConnection().getOldConnectionNo(), e.getMessage());
			}
		}
	}

	private boolean migrateWaterConnection(ConnectionDTO conn) throws Exception {
		boolean isWaterConnectionMigrated = false;
		WaterConnectionDTO waterConnection = null;
		waterConnection = searchWaterConnection(conn);
		if (waterConnection == null) {
			log.info(String.format("Migrating Water: %s", conn.getWaterConnection().getOldConnectionNo()));
			isWaterConnectionMigrated = doMigrateWaterConnection(conn);
		} else {
			log.info(String.format("Water connection: %s already migrated", conn.getWaterConnection().getOldConnectionNo()));
			conn.setWaterConnection(waterConnection);
			isWaterConnectionMigrated = true;
		}
		return isWaterConnectionMigrated;
	}

	private boolean doMigrateWaterConnection(ConnectionDTO conn) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getWsServiceHost()).append(properties.getWsMigrateEndpoint());

		Map<String, Object> migrateConnectionRequest = prepareWaterConnectionRequest(conn.getWaterConnection());
		Object response = remoteService.fetchResult(uri, migrateConnectionRequest);
		if (response == null) {
			return false;
		} else {
			ConnectionResponse connectionResponse = mapper.convertValue(response, ConnectionResponse.class);
			conn.setWaterConnection(connectionResponse.getWaterConnection().get(0));
		}
		return true;
	}

	private Map<String, Object> prepareWaterConnectionRequest(WaterConnectionDTO waterConnection) {
		Map<String, Object> migrateConnectionRequest = new HashMap<>();
		migrateConnectionRequest.put("RequestInfo", prepareRequestInfo());
		migrateConnectionRequest.put("WaterConnection", waterConnection);
		return migrateConnectionRequest;
	}

	private WaterConnectionDTO searchWaterConnection(ConnectionDTO conn) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getWsServiceHost()).append(properties.getWsSearchEndpoint())
				.append("?").append("oldConnectionNumber=").append(conn.getWaterConnection().getOldConnectionNo())
				.append("&tenantId=").append(conn.getWaterConnection().getTenantId());

		Map<String, Object> connectionSearchRequest = prepareSearchConnectionRequest();
		Object response = remoteService.fetchResult(uri, connectionSearchRequest);
		if (response == null) {
			return null;
		} else {
			ConnectionResponse connectionResponse = mapper.convertValue(response, ConnectionResponse.class);
			if (connectionResponse.getWaterConnection().isEmpty())
				return null;
			return connectionResponse.getWaterConnection().get(0);
		}
	}

	public boolean migrateMeterReading(ConnectionDTO conn) throws Exception {
		if(conn.getMeterReading() == null) {
			return false;
		} else {
			conn.getMeterReading().setConnectionNo(conn.getWaterConnection().getConnectionNo());
		}
		
		boolean isMeterReadingMigrated = false;
		MeterReadingDTO meterReading = searchMeterReading(conn);
		if (meterReading == null) {
			isMeterReadingMigrated = doMigrateMeterReading(conn);
		} else {
			conn.setMeterReading(meterReading);
			isMeterReadingMigrated = true;
		}
		return isMeterReadingMigrated;
	}

	private boolean doMigrateMeterReading(ConnectionDTO conn) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getWsCalculatorHost())
				.append(properties.getWsMigrateMeterReadingEndpoint());

		Map<String, Object> migrateMeterReadingRequest = prepareMeterReadingRequest(conn.getMeterReading());
		Object response = remoteService.fetchResult(uri, migrateMeterReadingRequest);
		if (response == null) {
			return false;
		} else {
			ConnectionResponse connectionResponse = mapper.convertValue(response, ConnectionResponse.class);
			conn.setMeterReading(connectionResponse.getMeterReadings().get(0));
		}
		return true;
	}

	private Map<String, Object> prepareMeterReadingRequest(MeterReadingDTO meterReading) {
		Map<String, Object> migrateMeterReadingRequest = new HashMap<>();
		migrateMeterReadingRequest.put("RequestInfo", prepareRequestInfo());
		migrateMeterReadingRequest.put("meterReadings", meterReading);
		return migrateMeterReadingRequest;
	}

	private MeterReadingDTO searchMeterReading(ConnectionDTO conn) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getWsCalculatorHost())
				.append(properties.getWsSearchMeterReadingEndpoint()).append("?").append("connectionNos=")
				.append(conn.getWaterConnection().getConnectionNo()).append("&tenantId=")
				.append(conn.getMeterReading().getTenantId());

		Map<String, Object> connectionSearchRequest = prepareSearchConnectionRequest();
		Object response = remoteService.fetchResult(uri, connectionSearchRequest);
		if (response == null) {
			return null;
		} else {
			ConnectionResponse connectionResponse = mapper.convertValue(response, ConnectionResponse.class);
			if (connectionResponse.getMeterReadings().isEmpty())
				return null;
			return connectionResponse.getMeterReadings().get(0);
		}
	}

	public void migrateDemands(ConnectionDTO conn) throws Exception {
		if (conn.getWaterConnection().getConnectionNo() != null && conn.getWaterDemands() != null) {
			boolean isDemandMigrated = migrateWaterDemand(conn);
			if(isDemandMigrated) {
				MigrationUtility.addSuccessForWaterDemand(conn);
			}
		}
	}

	private boolean migrateWaterDemand(ConnectionDTO conn) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getWsCalculatorHost())
				.append(properties.getWsMigrateDemandEndpoint());
		if(!conn.getWaterDemands().isEmpty()) {
			conn.getWaterDemands().stream().forEach(demand -> {
				demand.setConsumerCode(conn.getWaterConnection().getConnectionNo());
				demand.setPayer(conn.getWaterConnection().getConnectionHolders().get(0));
				demand.setBillExpiryTime(0L);
				});
		}

		boolean isWaterDemandMigrated = false;
		List<DemandDTO> demands = searchDemand(conn, MigrationConst.CONNECTION_WATER);
		if (demands == null) {
			demands = doMigrateDemand(uri, conn.getWaterDemands());
			if(demands != null && !demands.isEmpty()) {
				isWaterDemandMigrated = true;
			}
		} else {
			isWaterDemandMigrated = true;
		}
		conn.setWaterDemands(demands);
		return isWaterDemandMigrated;
	}

	private List<DemandDTO> doMigrateDemand(StringBuilder uri, List<DemandDTO> demands) throws Exception {
		Map<String, Object> migrateDemandRequest = prepareDemandRequest(demands);
		Object response = remoteService.fetchResult(uri, migrateDemandRequest);
		if (response == null) {
			return null;
		} else {
			ConnectionResponse connectionResponse = mapper.convertValue(response, ConnectionResponse.class);
			return connectionResponse.getDemands();
		}
	}

	private Map<String, Object> prepareDemandRequest(List<DemandDTO> demands) {
		Map<String, Object> migrateDemandRequest = new HashMap<>();
		migrateDemandRequest.put("RequestInfo", prepareRequestInfo());
		migrateDemandRequest.put("Demands", demands);
		return migrateDemandRequest;
	}

	private List<DemandDTO> searchDemand(ConnectionDTO conn, String connectionType) throws Exception {
		StringBuilder uri = null;
		if(MigrationConst.CONNECTION_WATER.equalsIgnoreCase(connectionType)) {
			uri = new StringBuilder(properties.getBillingServiceHost()).append(properties.getDemandSearchEndpoint())
					.append("?").append("consumerCode=").append(conn.getWaterConnection().getConnectionNo())
					.append("&tenantId=").append(conn.getWaterConnection().getTenantId())
					.append("&periodFrom=").append(conn.getWaterDemands().get(0).getTaxPeriodFrom())
					.append("&periodTo=").append(conn.getWaterDemands().get(0).getTaxPeriodTo());
		} else if(MigrationConst.CONNECTION_SEWERAGE.equalsIgnoreCase(connectionType)) {
			uri = new StringBuilder(properties.getBillingServiceHost()).append(properties.getDemandSearchEndpoint())
					.append("?").append("consumerCode=").append(conn.getSewerageConnection().getConnectionNo())
					.append("&tenantId=").append(conn.getSewerageConnection().getTenantId())
					.append("&periodFrom=").append(conn.getSewerageDemands().get(0).getTaxPeriodFrom())
					.append("&periodTo=").append(conn.getSewerageDemands().get(0).getTaxPeriodTo());
		}

		Map<String, Object> demandSearchRequest = prepareSearchConnectionRequest();
		Object response = remoteService.fetchResult(uri, demandSearchRequest);
		if (response == null) {
			return null;
		} else {
			ConnectionResponse connectionResponse = mapper.convertValue(response, ConnectionResponse.class);
			if (connectionResponse.getDemands().isEmpty())
				return null;
			return connectionResponse.getDemands();
		}
	}

	private Map<String, Object> prepareSearchConnectionRequest() {
		Map<String, Object> SearchRequest = new HashMap<>();
		SearchRequest.put("RequestInfo", prepareRequestInfo());
		return SearchRequest;
	}

	private RequestInfo prepareRequestInfo() {
		RequestInfo requestInfo = RequestInfo.builder().apiId("Rainmaker").ver(".01").ts("").action("_create").did("1")
				.key("").msgId("20170310130900|en_IN").authToken(properties.getAuthToken()).build();
		return requestInfo;
	}

	public void writeError() throws IOException {
		String fileName = recordStatistic.getErrorFile();
		boolean isNewlyCreated = false;
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
			isNewlyCreated = true;
		}
		FileInputStream inputStream = new FileInputStream(file);

		int rownum = 0;
		try {
			Workbook workbook;
			Sheet sheet;
			if (!isNewlyCreated) {
				workbook = new XSSFWorkbook(inputStream);
				sheet = workbook.getSheet("ERROR_RECORD");
			} else {
				workbook = new XSSFWorkbook();
				sheet = workbook.createSheet("ERROR_RECORD");
			}

			rownum = sheet.getLastRowNum() + 1;

			if (rownum == 0) {
				int cellnum = 0;
				Row headerRow = sheet.createRow(rownum++);
				Cell headerCellConnection = headerRow.createCell(cellnum++);
				headerCellConnection.setCellValue("CONNECTION_NO");
				Cell headerCellMessage = headerRow.createCell(cellnum++);
				headerCellMessage.setCellValue("ERROR_MESSAGE");
			}

			for (String connectionNo : recordStatistic.getErrorRecords().keySet()) {
				for (String errorMessage : recordStatistic.getErrorRecords().get(connectionNo)) {
					Row row = sheet.createRow(rownum++);
					int cellnum = 0;
					Cell cellConnection = row.createCell(cellnum++);
					cellConnection.setCellValue(connectionNo);
					Cell cellMessage = row.createCell(cellnum++);
					cellMessage.setCellValue(errorMessage);
				}

			}

			FileOutputStream fos = new FileOutputStream(new File(fileName));
			workbook.write(fos);
			workbook.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeSuccess() throws IOException {
		String fileName = recordStatistic.getSuccessFile();
		boolean isNewlyCreated = false;
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
			isNewlyCreated = true;
		}
		FileInputStream inputStream = new FileInputStream(file);

		int rownum = 0;
		try {
			Workbook workbook;
			Sheet sheet;
			if (!isNewlyCreated) {
				workbook = new XSSFWorkbook(inputStream);
				sheet = workbook.getSheet("SUCCESS_RECORD");
			} else {
				workbook = new XSSFWorkbook();
				sheet = workbook.createSheet("SUCCESS_RECORD");
			}

			rownum = sheet.getLastRowNum() + 1;

			if (rownum == 0) {
				int cellnum = 0;
				Row headerRow = sheet.createRow(rownum++);
				Cell headerCellConnection = headerRow.createCell(cellnum++);
				headerCellConnection.setCellValue("OLD_CONNECTION_NO");
				Cell headerCellWaterConnection = headerRow.createCell(cellnum++);
				headerCellWaterConnection.setCellValue("DIGIT_WATER_CONNECTION_NO");
				Cell headerCellSewerageConnection = headerRow.createCell(cellnum++);
				headerCellSewerageConnection.setCellValue("DIGIT_SEWERAGE_CONNECTION_NO");
				Cell headerCellMeterReading = headerRow.createCell(cellnum++);
				headerCellMeterReading.setCellValue("DIGIT_METER_READING_ID");
				Cell headerCellWaterDemands = headerRow.createCell(cellnum++);
				headerCellWaterDemands.setCellValue("DIGIT_WATER_DEMAND_IDs");
				Cell headerCellSewerageDemands = headerRow.createCell(cellnum++);
				headerCellSewerageDemands.setCellValue("DIGIT_SEWERAGE_DEMAND_IDs");
			}

			for (String oldConnectionNo : recordStatistic.getSuccessRecords().keySet()) {
				String waterConnectionNo = recordStatistic.getSuccessRecords().get(oldConnectionNo).get(MigrationConst.WATER_CONNECTION_NO);
				String sewerageConnectionNo = recordStatistic.getSuccessRecords().get(oldConnectionNo).get(MigrationConst.SEWERAGE_CONNECTION_NO);
				String meterReadingId = recordStatistic.getSuccessRecords().get(oldConnectionNo).get(MigrationConst.METER_READING);
				String waterDemandIds = recordStatistic.getSuccessRecords().get(oldConnectionNo).get(MigrationConst.DEMAND_WATER);
				String sewerageDemandIds = recordStatistic.getSuccessRecords().get(oldConnectionNo).get(MigrationConst.DEMAND_SEWERAGE);
				
				Row row = sheet.createRow(rownum++);
				int cellnum = 0;
				Cell cellConnection = row.createCell(cellnum++);
				cellConnection.setCellValue(oldConnectionNo);
				Cell cellWaterConnection = row.createCell(cellnum++);
				cellWaterConnection.setCellValue(waterConnectionNo);
				Cell cellSewerageConnection = row.createCell(cellnum++);
				cellSewerageConnection.setCellValue(sewerageConnectionNo);
				Cell cellMeterReading = row.createCell(cellnum++);
				cellMeterReading.setCellValue(meterReadingId);
				Cell cellWaterDemand = row.createCell(cellnum++);
				cellWaterDemand.setCellValue(waterDemandIds);
				Cell cellSewerageDemand = row.createCell(cellnum++);
				cellSewerageDemand.setCellValue(sewerageDemandIds);

			}

			FileOutputStream fos = new FileOutputStream(new File(fileName));
			workbook.write(fos);
			workbook.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void writeExecutionTime() throws IOException, InvalidFormatException {
		String fileName = recordStatistic.getSuccessFile();
		boolean isNewlyCreated = false;
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
			isNewlyCreated = true;
		}
		FileInputStream inputStream = new FileInputStream(file);

		int rownum = 0;
		try {
			Workbook workbook;
			Sheet sheet;
			if (!isNewlyCreated) {
				workbook = new XSSFWorkbook(inputStream);
				sheet = workbook.getSheet("SUCCESS_RECORD");
			} else {
				workbook = new XSSFWorkbook();
				sheet = workbook.createSheet("SUCCESS_RECORD");
			}

			rownum = sheet.getLastRowNum() + 3;

			Row headerRow = sheet.createRow(rownum++);
			Cell headerCellStartTime = headerRow.createCell(0);
			headerCellStartTime.setCellValue("Start Time");
			Cell headerCellEndTime = headerRow.createCell(1);
			headerCellEndTime.setCellValue("End Time");

			Row row = sheet.createRow(rownum++);
			int cellnum = 0;
			Cell cellStartTime = row.createCell(cellnum++);
			cellStartTime.setCellValue(recordStatistic.getStartTime());
			Cell cellEndTime = row.createCell(cellnum++);
			cellEndTime.setCellValue(recordStatistic.getEndTime());


			FileOutputStream fos = new FileOutputStream(new File(fileName));
			workbook.write(fos);
			workbook.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	

	public void writeFileError(String filename) {
		String filePath = properties.getWnsErrorFileDirectory().concat(File.separator).concat("File_Not_Processed.txt");
		try {
			File file = new File(filePath);
			if(!file.exists()) {
				file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(filePath,true);
		    fw.write(String.format("%s not processed. Tenant Id is not available for the file. Check the file name.\n", filename));
		    fw.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public WaterConnectionDTO getWaterConnection(ConnectionDTO connectionDTO) throws Exception {
		WaterConnectionDTO waterConnectionDTO = searchWaterConnection(connectionDTO);
		return waterConnectionDTO;
	}

	public void migrateSewerageConnection(ConnectionDTO conn) {
		boolean isMigrated = false;
		try {
			if(MigrationConst.SERVICE_WATER_SEWERAGE.equals(conn.getWaterConnection().getConnectionFacility())) {
				// Update connection
				isMigrated = updateWaterConnection(conn);
				
			} else if(MigrationConst.SERVICE_SEWERAGE.equals(conn.getWaterConnection().getConnectionFacility())) {
				// new insert
				isMigrated = migrateWaterConnection(conn);
			}
			if(isMigrated) {
				MigrationUtility.addSuccessForWaterConnection(conn.getWaterConnection());
			}
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			MigrationUtility.addError(conn.getWaterConnection().getOldConnectionNo(), e.getMessage());
		}
		
	}

	private boolean updateWaterConnection(ConnectionDTO conn) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getWsServiceHost()).append(properties.getWsUpdateEndpoint());

		Map<String, Object> migrateConnectionRequest = prepareWaterConnectionRequest(conn.getWaterConnection());
		Object response = remoteService.fetchResult(uri, migrateConnectionRequest);
		if (response == null) {
			return false;
		} else {
			ConnectionResponse connectionResponse = mapper.convertValue(response, ConnectionResponse.class);
			conn.setWaterConnection(connectionResponse.getWaterConnection().get(0));
		}
		return true;
	}

	public void callFetchbill(WSConnection conn) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getBillingServiceHost()).append(properties.getFetchBillEndpoint())
				.append("?").append("tenantId=").append(conn.getTenantId())
				.append("&consumerCode=").append(conn.getConnectionNo())
				.append("&businessService=").append(conn.getBusinessservice());
		
		Map<String, Object> fetchbillRequest = prepareWSFetchbillRequest();
		Object response = remoteService.fetchResult(uri, fetchbillRequest);
		
	}
	
	private Map<String, Object> prepareWSFetchbillRequest() {
		Map<String, Object> fetchbillRequest = new HashMap<>();
		fetchbillRequest.put("RequestInfo", prepareRequestInfo());
		return fetchbillRequest;
	}
	
	public void writeFetchbillSuccess() throws IOException {
		String fileName = recordStatistic.getSuccessFile();
		boolean isNewlyCreated = false;
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
			isNewlyCreated = true;
		}
		FileInputStream inputStream = new FileInputStream(file);

		int rownum = 0;
		try {
			Workbook workbook;
			Sheet sheet;
			if (!isNewlyCreated) {
				workbook = new XSSFWorkbook(inputStream);
				sheet = workbook.getSheet("SUCCESS_RECORD");
			} else {
				workbook = new XSSFWorkbook();
				sheet = workbook.createSheet("SUCCESS_RECORD");
			}

			rownum = sheet.getLastRowNum() + 1;

			if (rownum == 0) {
				int cellnum = 0;
				Row headerRow = sheet.createRow(rownum++);
				Cell headerCellConnection = headerRow.createCell(cellnum++);
				headerCellConnection.setCellValue("CONNECTION_NO");
			}

			for (String connectionNo : recordStatistic.getFetchbillSuccessRecords()) {
				Row row = sheet.createRow(rownum++);
				int cellnum = 0;
				Cell cellConnection = row.createCell(cellnum++);
				cellConnection.setCellValue(connectionNo);
			}

			FileOutputStream fos = new FileOutputStream(new File(fileName));
			workbook.write(fos);
			workbook.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
}
