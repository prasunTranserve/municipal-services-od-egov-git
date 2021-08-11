package org.egov.pt.migration.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egov.pt.migration.business.model.AssessmentDTO;
import org.egov.pt.migration.business.model.AssessmentResponse;
import org.egov.pt.migration.business.model.PropertyDTO;
import org.egov.pt.migration.business.model.PropertyDetailDTO;
import org.egov.pt.migration.business.model.PropertyResponse;
import org.egov.pt.migration.common.model.RecordStatistic;
import org.egov.pt.migration.common.model.RequestInfo;
import org.egov.pt.migration.config.PropertiesData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PropertyService {

	private static final String PROPERTY_MIGRATION_ERROR_MSG = "Property not migrated due to server error";
	private static final String ASSESSMENT_MIGRATION_ERROR_MSG = "Property migrated but Assessment and Demand not migrated due to server error";

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private PropertiesData properties;

	@Autowired
	private RemoteService remoteService;

	@Autowired
	private RecordStatistic recordStatistic;

	public boolean migrateItem(PropertyDetailDTO propertyDetail) {
		boolean isPropertyMigrated = false;
		
		PropertyDTO propertyDTO = searchProperty(propertyDetail);
		if(propertyDTO == null) {
			isPropertyMigrated = migrateProperty(propertyDetail);
		} else {
			log.info(String.format("%s property already migrated", propertyDTO.getPropertyId()));
			isPropertyMigrated = true;
			propertyDetail.setProperty(propertyDTO);
		}
		
		if(isPropertyMigrated) {
			try {
				Thread.sleep(3000);
			} catch (Exception e) { }
			AssessmentDTO assessmentDTO = searchAssessment(propertyDetail);
			if(assessmentDTO == null) {
				boolean isAssessmentMigrated = migrateAssessment(propertyDetail);
				if(!isAssessmentMigrated) {
					if(recordStatistic.getErrorRecords().get(propertyDetail.getProperty().getOldPropertyId())==null) {
						recordStatistic.getErrorRecords().put(propertyDetail.getProperty().getOldPropertyId(), new ArrayList<>());
					}
					recordStatistic.getErrorRecords().get(propertyDetail.getProperty().getOldPropertyId()).add(ASSESSMENT_MIGRATION_ERROR_MSG);
				}
			}
			return true;
		} else {
			if(recordStatistic.getErrorRecords().get(propertyDetail.getProperty().getOldPropertyId())==null) {
				recordStatistic.getErrorRecords().put(propertyDetail.getProperty().getOldPropertyId(), new ArrayList<>());
			}
			recordStatistic.getErrorRecords().get(propertyDetail.getProperty().getOldPropertyId()).add(PROPERTY_MIGRATION_ERROR_MSG);
			return false;
		}
		
	}

	private AssessmentDTO searchAssessment(PropertyDetailDTO propertyDetail) {
		// http://local.egov.org/property-services/assessment/_search?tenantId=od.jatni&propertyIds=PT-2021-08-10-000672
		StringBuilder uri = new StringBuilder(properties.getPtServiceHost()).append(properties.getAsmtSearchEndPoint())
				.append("?").append("tenantId=").append(propertyDetail.getProperty().getTenantId())
				.append("&propertyIds=").append(propertyDetail.getProperty().getPropertyId());
		
		
		Map<String, Object> assessmentSearchRequest = prepareSearchAssessmentRequest(propertyDetail);
		Object response = remoteService.fetchResult(uri, assessmentSearchRequest);
		if (response == null) {
			return null;
		} else { 
			AssessmentResponse assessmentResponse = mapper.convertValue(response, AssessmentResponse.class);
			if(assessmentResponse.getAssessments().isEmpty())
				return null;
			
			return assessmentResponse.getAssessments().get(0);
		}
		
	}

	private PropertyDTO searchProperty(PropertyDetailDTO propertyDetail) {
		// http://localhost:8083/property-services/property/_search?oldpropertyids=008000436&tenantId=od.jatni
		StringBuilder uri = new StringBuilder(properties.getPtServiceHost()).append(properties.getPtSearchEndpoint())
				.append("?").append("oldpropertyids=").append(propertyDetail.getProperty().getOldPropertyId())
				.append("&tenantId=").append(propertyDetail.getProperty().getTenantId());
		
		Map<String, Object> propertySearchRequest = prepareSearchPropertyRequest(propertyDetail);
		Object response = remoteService.fetchResult(uri, propertySearchRequest);
		if (response == null) {
			return null;
		} else { 
			PropertyResponse propertyResponse = mapper.convertValue(response, PropertyResponse.class);
			if(propertyResponse.getProperties().isEmpty())
				return null;
			
			return propertyResponse.getProperties().get(0);
		}
	}

	private Map<String, Object> prepareSearchPropertyRequest(PropertyDetailDTO propertyDetail) {
		Map<String, Object> migratePropertyRequest = new HashMap<>();
		migratePropertyRequest.put("RequestInfo", prepareRequestInfo());
		//migratePropertyRequest.put("oldpropertyids", propertyDetail.getProperty().getOldPropertyId());
		//migratePropertyRequest.put("tenantId", propertyDetail.getProperty().getTenantId());
		return migratePropertyRequest;
	}
	
	private Map<String, Object> prepareSearchAssessmentRequest(PropertyDetailDTO propertyDetail) {
		Map<String, Object> migrateAssessmentRequest = new HashMap<>();
		migrateAssessmentRequest.put("RequestInfo", prepareRequestInfo());
		//migrateAssessmentRequest.put("propertyIds", propertyDetail.getProperty().getPropertyId());
		//migrateAssessmentRequest.put("tenantId", propertyDetail.getProperty().getTenantId());
		return migrateAssessmentRequest;
	}

	private boolean migrateProperty(PropertyDetailDTO propertyDetail) {
		StringBuilder uri = new StringBuilder(properties.getPtServiceHost()).append(properties.getMigratePropertyEndpoint());

		Map<String, Object> migratePropertyRequest = prepareMigratePropertyRequest(propertyDetail);
		Object response = remoteService.fetchResult(uri, migratePropertyRequest);
		if (response == null) {
			return false;
		} else { 
			PropertyResponse propertyResponse = mapper.convertValue(response, PropertyResponse.class);
			propertyDetail.setProperty(propertyResponse.getProperties().get(0));
		}
		return true;
	}

	private Map<String, Object> prepareMigratePropertyRequest(PropertyDetailDTO propertyDetail) {
		Map<String, Object> migratePropertyRequest = new HashMap<>();
		migratePropertyRequest.put("RequestInfo", prepareRequestInfo());
		migratePropertyRequest.put("Property", propertyDetail.getProperty());
		return migratePropertyRequest;
	}

	private RequestInfo prepareRequestInfo() {
		RequestInfo requestInfo = RequestInfo.builder().apiId("Rainmaker").ver(".01").ts("").action("_create").did("1")
				.key("").msgId("20170310130900|en_IN").authToken(properties.getAuthToken()).build();
		return requestInfo;
	}

	private boolean migrateAssessment(PropertyDetailDTO propertyDetail) {
		propertyDetail.getAssessment().setPropertyId(propertyDetail.getProperty().getPropertyId());

		StringBuilder uri = new StringBuilder(properties.getPtServiceHost())
				.append(properties.getMigrateAssessmentEndPoint());

		Map<String, Object> migratePropertyRequest = prepareMigrateAssessmentRequest(propertyDetail);
		Object response = remoteService.fetchResult(uri, migratePropertyRequest);
		if (response == null) {
			return false;
		}
		return true;
	}

	private Map<String, Object> prepareMigrateAssessmentRequest(PropertyDetailDTO propertyDetail) {
		Map<String, Object> migrateAssessmentRequest = new HashMap<>();
		migrateAssessmentRequest.put("RequestInfo", prepareRequestInfo());
		migrateAssessmentRequest.put("Assessment", propertyDetail.getAssessment());
		migrateAssessmentRequest.put("Demands", propertyDetail.getDemands());
		return migrateAssessmentRequest;
	}

	public void writeError() throws IOException, InvalidFormatException {
		String fileName = recordStatistic.getErrorFile();
		File file = new File(fileName);
		if(!file.exists()) {
			file.createNewFile();
		}
		
		int rownum = 0;
		try(Workbook workbookRead = new XSSFWorkbook(file);) {
			//Workbook workbookRead = new XSSFWorkbook(file);
			Sheet sheetRead = workbookRead.getSheet("ERROR_RECORD");
			rownum = sheetRead.getLastRowNum()+1;
			workbookRead.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("ERROR_RECORD");

		Row headerRow = sheet.createRow(rownum++);
		Cell headerCellProperty = headerRow.createCell(0);
		headerCellProperty.setCellValue("PROPERTY_ID");
		Cell headerCellMessage = headerRow.createCell(1);
		headerCellMessage.setCellValue("ErrorMessage");

		for (String propertyId : recordStatistic.getErrorRecords().keySet()) {
			for (String errorMessage : recordStatistic.getErrorRecords().get(propertyId)) {
				Row row = sheet.createRow(rownum++);
				int cellnum = 0;
				Cell cellProperty = row.createCell(cellnum++);
				cellProperty.setCellValue(propertyId);
				Cell cellMessage = row.createCell(cellnum++);
				cellMessage.setCellValue(errorMessage);
			}

		}

		FileOutputStream fos = new FileOutputStream(new File(fileName));
		workbook.write(fos);
		fos.close();
	}

	public void writeSuccess() throws IOException, InvalidFormatException {
		String fileName = recordStatistic.getSuccessFile();
		File file = new File(fileName);
		if(!file.exists()) {
			file.createNewFile();
		}
		
		int rownum = 0;
		try(Workbook workbookRead = new XSSFWorkbook(file);) {
			//Workbook workbookRead = new XSSFWorkbook(file);
			Sheet sheetRead = workbookRead.getSheet("SUCCESS_RECORD");
			rownum = sheetRead.getLastRowNum()+1;
			workbookRead.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("SUCCESS_RECORD");
		
		
		Row headerRow = sheet.createRow(rownum++);
		Cell headerCellProperty = headerRow.createCell(0);
		headerCellProperty.setCellValue("OLD_PROPERTY_ID");
		Cell headerCellMessage = headerRow.createCell(1);
		headerCellMessage.setCellValue("DIGIT_PROPERTY_ID");

		for (String oldPropertyId : recordStatistic.getSuccessRecords().keySet()) {
			String propertyId = recordStatistic.getSuccessRecords().get(oldPropertyId);
			Row row = sheet.createRow(rownum++);
			int cellnum = 0;
			Cell cellProperty = row.createCell(cellnum++);
			cellProperty.setCellValue(oldPropertyId);
			Cell cellMessage = row.createCell(cellnum++);
			cellMessage.setCellValue(propertyId);

		}

		FileOutputStream fos = new FileOutputStream(new File(fileName));
		workbook.write(fos);
		fos.close();
		
	}
	
	public static void main(String[] args) throws InvalidFormatException, IOException {
		String fileName = "C:\\Users\\prasunAdmin\\Desktop\\Tasks\\Migration\\Batch_file\\Files\\PT_Migration_Standard_excel.xlsx";
		Workbook workbookRead = new XSSFWorkbook(new File(fileName));
		Sheet sheetRead = workbookRead.getSheet("EMPTY");
		
		int rownum = sheetRead.getLastRowNum();
		
		System.out.println(rownum);
	}

}
