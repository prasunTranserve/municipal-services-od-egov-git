package org.egov.migration.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egov.migration.business.model.DemandDetailSearchRequest;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.common.model.RequestInfo;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.util.MigrationConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemandService {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private PropertiesData properties;

	@Autowired
	private RemoteService remoteService;

	@Autowired
	private RecordStatistic recordStatistic;

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
				headerCellProperty.setCellValue("DEMAND_ID");
				Cell headerCellMessage = headerRow.createCell(1);
				headerCellMessage.setCellValue("ErrorMessage");
			}

			for (String demandId : recordStatistic.getErrorRecords().keySet()) {
				for (String errorMessage : recordStatistic.getErrorRecords().get(demandId)) {
					Row row = sheet.createRow(rownum++);
					int cellnum = 0;
					Cell cellProperty = row.createCell(cellnum++);
					cellProperty.setCellValue(demandId);
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
				Cell headerCellDemand = headerRow.createCell(0);
				headerCellDemand.setCellValue("DEMAND_ID");
				Cell headerCellAdjustedAmount = headerRow.createCell(1);
				headerCellAdjustedAmount.setCellValue("ADJUSTED_AMOUNT");
				Cell headerCellStatus = headerRow.createCell(2);
				headerCellStatus.setCellValue("UPDATE_STATUS");
			}
			for (String demand : recordStatistic.getSuccessRecords().keySet()) {
				String demandId = recordStatistic.getSuccessRecords().get(demand).get(MigrationConst.COL_DEMAND_ID);
				String adjustedAmount = recordStatistic.getSuccessRecords().get(demand)
						.get(MigrationConst.AMOUNT_ADJUSTED);
				Row row = sheet.createRow(rownum++);
				int cellnum = 0;
				Cell cellDemand = row.createCell(cellnum++);
				cellDemand.setCellValue(demandId);
				Cell cellAdjustedAmount = row.createCell(cellnum++);
				cellAdjustedAmount.setCellValue(adjustedAmount);
				Cell cellStatus = row.createCell(cellnum++);
				cellStatus.setCellValue("Success");
			}
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			workbook.write(fos);
			workbook.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void writeExecutionTime(DemandDetailSearchRequest request) throws IOException, InvalidFormatException {
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
		map.add("username", properties.getUsername());
		map.add("scope", "read");
		map.add("password", properties.getPassword());
		map.add("grant_type", "password");
		map.add("tenantId", properties.getTenantId());
		map.add("userType", "EMPLOYEE");

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

		try {
			Object response = remoteService.fetchResult(url, entity);
			Map authResponse = mapper.convertValue(response, Map.class);
			String authToken = authResponse.get("access_token").toString();
			properties.setAuthToken(authToken);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void writeFileError(String filename) {
		String filePath = properties.getPropertyErrorFileDirectory().concat(File.separator)
				.concat("File_Not_Processed.txt");
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(filePath, true);
			fw.write(String.format("%s not processed. Tenant Id is not available for the file. Check the file name.\n",
					filename));
			fw.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private RequestInfo prepareRequestInfo() {
		RequestInfo requestInfo = RequestInfo.builder()
				.apiId("Rainmaker")
				.ver(".01")
				.ts("")
				.action("")
				.did("1")
				.key("")
				.msgId("20170310130900|en_IN")
				.authToken(properties.getAuthToken())
				.build();
		return requestInfo;
	}

//Call Update API New
	
	public boolean callDemandDetailsUpdate(String requestInfo, String demandId, Double diffAmount,
			String businessService, String tenantId) {
		boolean retVal = false;
		StringBuilder uri = new StringBuilder(properties.getBillingServiceHost())
				.append(properties.getDemandUpdateHost());

		List<String> demands = new ArrayList<>();
		demands.add(demandId);
		DemandDetailSearchRequest demandDetailSearchRequest = DemandDetailSearchRequest.builder()
				.requestInfo(prepareRequestInfo()).businessService(businessService)
				.amountToBeAdjusted(String.valueOf(diffAmount)).tenantId(tenantId).demands(demands).build();
		try {
			Object response = remoteService.fetchResult(uri, demandDetailSearchRequest);
			log.info("Response Received");
			retVal = true;
			/*
			 * List<Demand> authResponse = mapper.convertValue(response, List.class); retVal
			 * = authResponse.toString();
			 */
		} catch (Exception e) {
			e.printStackTrace();
			retVal = false;
		}
		return retVal;
	}

}
