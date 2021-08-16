package org.egov.migration.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egov.migration.business.model.AssessmentDTO;
import org.egov.migration.business.model.AssessmentResponse;
import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.business.model.PropertyResponse;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.common.model.RequestInfo;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
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
		if (propertyDTO == null) {
			isPropertyMigrated = migrateProperty(propertyDetail);
		} else {
			log.info(String.format("%s property already migrated", propertyDTO.getPropertyId()));
			isPropertyMigrated = true;
			propertyDetail.setProperty(propertyDTO);
		}

		if (isPropertyMigrated) {
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
			}
			AssessmentDTO assessmentDTO = searchAssessment(propertyDetail);
			if (assessmentDTO == null) {
				boolean isAssessmentMigrated = doMigrateAssessment(propertyDetail);
				if (!isAssessmentMigrated) {
					MigrationUtility.addErrorForProperty(propertyDetail.getProperty().getOldPropertyId(),
							ASSESSMENT_MIGRATION_ERROR_MSG);
				}
			}
			return true;
		} else {
			MigrationUtility.addErrorForProperty(propertyDetail.getProperty().getOldPropertyId(),
					PROPERTY_MIGRATION_ERROR_MSG);
			return false;
		}

	}

	public boolean migrateProperty(PropertyDetailDTO propertyDetail) {
		boolean isPropertyMigrated = false;

		PropertyDTO propertyDTO = searchProperty(propertyDetail);
		if (propertyDTO == null) {
			isPropertyMigrated = doMigrateProperty(propertyDetail);
		} else {
			log.info(String.format("%s property already migrated", propertyDTO.getPropertyId()));
			isPropertyMigrated = true;
			propertyDetail.setProperty(propertyDTO);
		}
		
		return isPropertyMigrated;
	}
	
	public boolean migrateAssessment(PropertyDetailDTO propertyDetail) {
		boolean isAssessmentMigrated = false;
		PropertyDTO propertyDTO = searchProperty(propertyDetail);
		if (propertyDTO != null) {
			AssessmentDTO assessmentDTO = searchAssessment(propertyDetail);
			if (assessmentDTO == null) {
				isAssessmentMigrated = doMigrateAssessment(propertyDetail);
//				if (!isAssessmentMigrated) {
//					MigrationUtility.addErrorForProperty(propertyDetail.getProperty().getOldPropertyId(),
//							ASSESSMENT_MIGRATION_ERROR_MSG);
//				}
			} else {
				log.info(String.format("Assessment for property %s already migrated", assessmentDTO.getPropertyId()));
				isAssessmentMigrated = true;
			}
			return isAssessmentMigrated;
		} else {
			MigrationUtility.addErrorForProperty(propertyDetail.getProperty().getOldPropertyId(),
					PROPERTY_MIGRATION_ERROR_MSG);
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
			if (assessmentResponse.getAssessments().isEmpty())
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
			if (propertyResponse.getProperties().isEmpty())
				return null;

			return propertyResponse.getProperties().get(0);
		}
	}

	private Map<String, Object> prepareSearchPropertyRequest(PropertyDetailDTO propertyDetail) {
		Map<String, Object> migratePropertyRequest = new HashMap<>();
		migratePropertyRequest.put("RequestInfo", prepareRequestInfo());
		// migratePropertyRequest.put("oldpropertyids",
		// propertyDetail.getProperty().getOldPropertyId());
		// migratePropertyRequest.put("tenantId",
		// propertyDetail.getProperty().getTenantId());
		return migratePropertyRequest;
	}

	private Map<String, Object> prepareSearchAssessmentRequest(PropertyDetailDTO propertyDetail) {
		Map<String, Object> migrateAssessmentRequest = new HashMap<>();
		migrateAssessmentRequest.put("RequestInfo", prepareRequestInfo());
		// migrateAssessmentRequest.put("propertyIds",
		// propertyDetail.getProperty().getPropertyId());
		// migrateAssessmentRequest.put("tenantId",
		// propertyDetail.getProperty().getTenantId());
		return migrateAssessmentRequest;
	}

	private boolean doMigrateProperty(PropertyDetailDTO propertyDetail) {
		StringBuilder uri = new StringBuilder(properties.getPtServiceHost())
				.append(properties.getMigratePropertyEndpoint());

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

	private boolean doMigrateAssessment(PropertyDetailDTO propertyDetail) {
		propertyDetail.getAssessment().setPropertyId(propertyDetail.getProperty().getPropertyId());

		StringBuilder uri = new StringBuilder(properties.getPtServiceHost())
				.append(properties.getMigrateAssessmentEndPoint());

		Map<String, Object> migratePropertyRequest = prepareMigrateAssessmentRequest(propertyDetail);
		Object response = remoteService.fetchResult(uri, migratePropertyRequest);
		if (response == null) {
			return false;
		}
		
		AssessmentResponse assessmentResponse = mapper.convertValue(response, AssessmentResponse.class);
		propertyDetail.setAssessment(assessmentResponse.getAssessments().get(0));
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
				Row headerRow = sheet.createRow(rownum++);
				Cell headerCellProperty = headerRow.createCell(0);
				headerCellProperty.setCellValue("PROPERTY_ID");
				Cell headerCellMessage = headerRow.createCell(1);
				headerCellMessage.setCellValue("ErrorMessage");
			}

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
			workbook.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeSuccess() throws IOException, InvalidFormatException {
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
				Row headerRow = sheet.createRow(rownum++);
				Cell headerCellProperty = headerRow.createCell(0);
				headerCellProperty.setCellValue("OLD_PROPERTY_ID");
				Cell headerCellPropertyId = headerRow.createCell(1);
				headerCellPropertyId.setCellValue("DIGIT_PROPERTY_ID");
				Cell headerCellAssessmentNumber = headerRow.createCell(2);
				headerCellAssessmentNumber.setCellValue("DIGIT_ASSESSMENT_NUMBER");
			}

			for (String oldPropertyId : recordStatistic.getSuccessRecords().keySet()) {
				String propertyId = recordStatistic.getSuccessRecords().get(oldPropertyId).get(MigrationConst.PROPERTY_ID);
				String assessmentNumber = recordStatistic.getSuccessRecords().get(oldPropertyId).get(MigrationConst.ASSESSMENT_NUMBER);
				Row row = sheet.createRow(rownum++);
				int cellnum = 0;
				Cell cellProperty = row.createCell(cellnum++);
				cellProperty.setCellValue(oldPropertyId);
				Cell cellPropertyId = row.createCell(cellnum++);
				cellPropertyId.setCellValue(propertyId);
				Cell cellAssessmentNumber = row.createCell(cellnum++);
				cellAssessmentNumber.setCellValue(assessmentNumber);

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
