package org.egov.migration.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xalan.xsltc.compiler.sym;
import org.egov.migration.business.model.AssessmentDTO;
import org.egov.migration.business.model.AssessmentResponse;
import org.egov.migration.business.model.OwnerInfoDTO;
import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.business.model.PropertyResponse;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.common.model.RequestInfo;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.Assessment;
import org.egov.migration.reader.model.Demand;
import org.egov.migration.reader.model.DemandDetail;
import org.egov.migration.reader.model.Property;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PropertyService {

	private static final String PROPERTY_MIGRATION_ERROR_MSG = "Property is not available during assessment migration";
	private static final String ASSESSMENT_MIGRATION_ERROR_MSG = "Property migrated but Assessment and Demand not migrated due to server error";

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private PropertiesData properties;
	
	@Autowired
	private SystemProperties mdProperties;

	@Autowired
	private RemoteService remoteService;

	@Autowired
	private RecordStatistic recordStatistic;

	public boolean migrateItem(PropertyDetailDTO propertyDetail) throws Exception {
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
					MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(),
							ASSESSMENT_MIGRATION_ERROR_MSG);
				}
			}
			return true;
		} else {
			MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(),
					PROPERTY_MIGRATION_ERROR_MSG);
			return false;
		}

	}

	public boolean migrateProperty(PropertyDetailDTO propertyDetail) throws Exception {
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
	
	public boolean migrateAssessment(PropertyDetailDTO propertyDetail) throws Exception {
		if(propertyDetail.getProperty().getPropertyId() == null)
			return false;
		boolean isAssessmentMigrated = false;
		PropertyDTO propertyDTO = searchProperty(propertyDetail);
		if (propertyDTO != null) {
			AssessmentDTO assessmentDTO = searchAssessment(propertyDetail);
			if (assessmentDTO == null) {
				isAssessmentMigrated = doMigrateAssessment(propertyDetail);
			} else {
				log.info(String.format("Assessment for property %s already migrated", assessmentDTO.getPropertyId()));
				propertyDetail.setAssessment(assessmentDTO);
				isAssessmentMigrated = true;
			}
			return isAssessmentMigrated;
		} else {
			MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(),
					PROPERTY_MIGRATION_ERROR_MSG);
			return false;
		}
	}
	
	private AssessmentDTO searchAssessment(PropertyDetailDTO propertyDetail) throws Exception {
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

	private PropertyDTO searchProperty(PropertyDetailDTO propertyDetail) throws Exception {
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

	private boolean doMigrateProperty(PropertyDetailDTO propertyDetail) throws Exception {
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

	private boolean doMigrateAssessment(PropertyDetailDTO propertyDetail) throws Exception {
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

	public void refreshAuthToken() {

		StringBuilder url = new StringBuilder(properties.getAuthTokenUrl());
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setBasicAuth("ZWdvdi11c2VyLWNsaWVudDplZ292LXVzZXItc2VjcmV0");
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("username",properties.getUsername());
		map.add("scope","read");
		map.add("password",properties.getPassword());
		map.add("grant_type","password");
		map.add("tenantId",properties.getTenantId());
		map.add("userType","EMPLOYEE");
		
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
		
		try {
			Object response = remoteService.fetchResult(url, entity);
			Map authResponse = mapper.convertValue(response, Map.class);
			String authToken = authResponse.get("access_token").toString();
			properties.setAuthToken(authResponse.get("access_token").toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void enrichDemandDetails(Property property) {
		
		if(property.getDemands() != null) {
			property.getDemands().forEach(demand -> {
				demand.setPaymentComplete(MigrationUtility.getPaymentComplete(demand.getPaymentComplete()));
				if(demand.getTaxPeriodFrom() == null || !demand.getTaxPeriodFrom().matches(MigrationConst.TAX_PERIOD_PATTERN)) {
					if(demand.getTaxPeriodTo() != null && demand.getTaxPeriodTo().matches(MigrationConst.TAX_PERIOD_PATTERN)) {
						demand.setTaxPeriodFrom(demand.getTaxPeriodTo().split("/")[0].concat("/Q1"));
					} else {
						demand.setTaxPeriodFrom("1980-81/Q1");
						demand.setTaxPeriodTo("1980-81/Q4");
					}
				}
				
				String startYear = demand.getTaxPeriodFrom().substring(0, 4);
				if(Integer.parseInt(startYear) < 1980) {
					demand.setTaxPeriodFrom("1980-81/Q1");
					demand.setTaxPeriodTo("1980-81/Q4");
				}
			});
		}
		
		if(property.getDemandDetails() != null) {
			property.getDemandDetails().forEach(demandDtl -> {
				if(demandDtl.getTaxHead() == null) {
					demandDtl.setTaxHead(MigrationConst.TAXHEAD_HOLDING_TAX_INPUT);
				}
				
				if(mdProperties.getTaxhead().get(demandDtl.getTaxHead()) == null) {
					demandDtl.setTaxHead(MigrationConst.TAXHEAD_HOLDING_TAX_INPUT);
				}
				
				demandDtl.setTaxAmt(MigrationUtility.getNearest(demandDtl.getTaxAmt(), "4"));
			});
		}
		
		if(property.getDemandDetails() == null && property.getDemands() == null) {
			property.setDemands(Arrays.asList(Demand.builder().ulb(property.getUlb())
					.propertyId(property.getPropertyId())
					.taxPeriodFrom(MigrationUtility.getFinYear().concat("/Q1"))
					.taxPeriodTo(MigrationUtility.getFinYear().concat("/Q4"))
					.minPayableAmt("0")
					.paymentComplete("N").build()));
			property.setDemandDetails(Arrays.asList(DemandDetail.builder()
					.propertyId(property.getPropertyId())
					.ulb(property.getUlb())
					.taxHead(MigrationConst.TAXHEAD_HOLDING_TAX_INPUT)
					.taxAmt("0").build()));
		}
		
		if(property.getDemandDetails() == null && property.getDemands() != null) {
			Double totalDemandAmt = property.getDemands().stream().mapToDouble(demand -> MigrationUtility.getDoubleAmount(demand.getMinPayableAmt())).sum();
			property.setDemandDetails(Arrays.asList(DemandDetail.builder()
					.propertyId(property.getPropertyId())
					.ulb(property.getUlb())
					.taxHead(MigrationConst.TAXHEAD_HOLDING_TAX_INPUT)
					.taxAmt(totalDemandAmt.toString()).build()));
		}
		
		if(property.getDemandDetails() != null && property.getDemands() == null) {
			Double totalDemandAmt = property.getDemandDetails().stream().mapToDouble(dtl -> MigrationUtility.getDoubleAmount(dtl.getTaxAmt())).sum();
			property.setDemands(Arrays.asList(Demand.builder().ulb(property.getUlb())
					.propertyId(property.getPropertyId())
					.taxPeriodFrom(MigrationUtility.getFinYear().concat("/Q1"))
					.taxPeriodTo(MigrationUtility.getFinYear().concat("/Q4"))
					.minPayableAmt(totalDemandAmt.toString())
					.paymentComplete("N").build()));
		}
		
	}

	public void enrichAssessment(Property property) {
		if(property.getAssessments() == null) {
			property.setAssessments(Arrays.asList(Assessment.builder()
					.propertyId(property.getPropertyId())
					.finYear(MigrationUtility.getFinYear())
					.assessmentDate(MigrationUtility.getCurrentDate()).build()));
		} else {
			property.getAssessments().forEach(asmt -> {
				asmt.setFinYear(MigrationUtility.getAssessmentFinYear(asmt.getFinYear()));
			});
		}
	}

	public void writeFileError(String filename) {
		String filePath = properties.getPropertyErrorFileDirectory().concat(File.separator).concat("File_Not_Processed.txt");
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

	public boolean getProperty(PropertyDetailDTO propertyDetail) throws Exception {
		boolean isPropertyExist = false;

		PropertyDTO propertyDTO = searchProperty(propertyDetail);
		if (propertyDTO != null) {
			log.info(String.format("%s property found", propertyDTO.getPropertyId()));
			isPropertyExist = true;
			propertyDetail.setProperty(propertyDTO);
		}
		
		return isPropertyExist;
	}

	public void callFetchBill(PropertyDetailDTO propertyDetail) throws Exception {
		//http://localhost:8083/billing-service/bill/v2/_fetchbill?tenantId=od.khordha&consumerCode=WS/KHU/000056&businessService=WS
		StringBuilder uri = new StringBuilder(properties.getBillingServiceHost()).append(properties.getFetchBillEndpoint())
				.append("?").append("tenantId=").append(propertyDetail.getProperty().getTenantId())
				.append("&consumerCode=").append(propertyDetail.getProperty().getPropertyId())
				.append("&businessService=").append("PT");

		Map<String, Object> propertySearchRequest = prepareSearchPropertyRequest(propertyDetail);
		Object response = remoteService.fetchResult(uri, propertySearchRequest);
	}
	
	public void writeFetchbillSuccess() throws IOException, InvalidFormatException {
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
				Cell headerCellFetchBillStatus = headerRow.createCell(2);
				headerCellFetchBillStatus.setCellValue("FETCH_BILL_SUCCESS");
			}

			for (String oldPropertyId : recordStatistic.getSuccessRecords().keySet()) {
				String propertyId = recordStatistic.getSuccessRecords().get(oldPropertyId).get(MigrationConst.PROPERTY_ID);
				String fetchBillStatus = recordStatistic.getSuccessRecords().get(oldPropertyId).get(MigrationConst.FETCH_BILL_STATUS);
				Row row = sheet.createRow(rownum++);
				int cellnum = 0;
				Cell cellProperty = row.createCell(cellnum++);
				cellProperty.setCellValue(oldPropertyId);
				Cell cellPropertyId = row.createCell(cellnum++);
				cellPropertyId.setCellValue(propertyId);
				Cell cellAssessmentNumber = row.createCell(cellnum++);
				cellAssessmentNumber.setCellValue(fetchBillStatus);

			}

			FileOutputStream fos = new FileOutputStream(new File(fileName));
			workbook.write(fos);
			workbook.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean migratePropertyUser(PropertyDetailDTO propertyDetail) throws Exception {
		boolean isPropertyUserMigrated = false;

		PropertyDTO propertyDTO = searchProperty(propertyDetail);
		if(propertyDTO != null) {
			List<OwnerInfoDTO> propertyOwner = propertyDTO.getOwners().stream().filter(ow -> ow.getStatus().equalsIgnoreCase("ACTIVE"))
				.filter(ow -> ow.getUserName()==null && ow.getPassword()==null).collect(Collectors.toList());
			if (!propertyOwner.isEmpty()) {
				enrichOwners(propertyDetail, propertyDTO);
				isPropertyUserMigrated = doMigratePropertyUser(propertyDetail);
			} else {
				log.info(String.format("%s property already migrated", propertyDTO.getPropertyId()));
				isPropertyUserMigrated = false;
				propertyDetail.setProperty(propertyDTO);
			}
		}
		
		return isPropertyUserMigrated;
	}

	private boolean doMigratePropertyUser(PropertyDetailDTO propertyDetail) throws Exception {
		StringBuilder uri = new StringBuilder(properties.getPtServiceHost())
				.append(properties.getMigratePropertyUserEndpoint());

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

	private void enrichOwners(PropertyDetailDTO propertyDetail, PropertyDTO propertyDTO) {
		if(propertyDetail.getProperty().getOwnershipCategory().equalsIgnoreCase("INDIVIDUAL.MULTIPLEOWNERS")) {
			List<OwnerInfoDTO> propertyOwner = propertyDTO.getOwners().stream().filter(ow -> ow.getStatus().equalsIgnoreCase("ACTIVE")).collect(Collectors.toList());
			List<String> names = propertyOwner.stream().map(ow -> ow.getName()).collect(Collectors.toList());
			propertyDetail.getProperty().setOwners(propertyDetail.getProperty().getOwners().stream().filter(ow -> !names.contains(ow.getName())).collect(Collectors.toList()));
			propertyOwner = propertyOwner.stream().filter(ow -> !StringUtils.hasText(ow.getName())).collect(Collectors.toList());
			
			int index = 0;
			for (OwnerInfoDTO owner : propertyDetail.getProperty().getOwners()) {
				owner.setUuid(propertyOwner.get(index).getUuid());
				index++;
			}
		} else {
			String uuid = propertyDTO.getOwners().stream().filter(ow -> ow.getStatus().equalsIgnoreCase("ACTIVE")).map(ow -> ow.getUuid()).findFirst().orElse(null);
			propertyDetail.getProperty().getOwners().get(0).setUuid(uuid);
		}
		
	}
	
}
