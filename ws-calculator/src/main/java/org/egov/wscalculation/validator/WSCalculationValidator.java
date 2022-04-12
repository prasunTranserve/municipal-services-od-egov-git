package org.egov.wscalculation.validator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.repository.WSCalculationDao;
import org.egov.wscalculation.service.MasterDataService;
import org.egov.wscalculation.util.CalculatorUtil;
import org.egov.wscalculation.web.models.AnnualAdvance;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReading.MeterStatusEnum;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;
import org.egov.wscalculation.web.models.WaterConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WSCalculationValidator {

	@Autowired
	private WSCalculationDao wSCalculationDao;
	
	@Autowired
	private CalculatorUtil calculationUtil;
	
	@Autowired
	private MasterDataService masterDataService;

	/**
	 * 
	 * @param meterConnectionRequest
	 *            meterReadingConnectionRequest is request for create or update
	 *            meter reading connection
	 * @param isUpdate
	 *            True for create
	 */
	public void validateMeterReading(MeterConnectionRequest meterConnectionRequest, boolean isUpdate) {
		MeterReading meterReading = meterConnectionRequest.getMeterReading();
		Map<String, String> errorMap = new HashMap<>();

		// Future Billing Period Check
		validateBillingPeriod(meterReading.getBillingPeriod());
  
		List<WaterConnection> waterConnectionList = calculationUtil.getWaterConnection(meterConnectionRequest.getRequestInfo(),
				meterReading.getConnectionNo(), meterConnectionRequest.getMeterReading().getTenantId());
		WaterConnection connection = null;
		if(waterConnectionList != null){
			int size = waterConnectionList.size();
			connection = waterConnectionList.get(size-1);
		}

		if (meterConnectionRequest.getMeterReading().getGenerateDemand() && connection == null) {
			errorMap.put("INVALID_METER_READING_CONNECTION_NUMBER", "Invalid water connection number");
		}
		if (connection != null
				&& !WSCalculationConstant.meteredConnectionType.equalsIgnoreCase(connection.getConnectionType())) {
			errorMap.put("INVALID_WATER_CONNECTION_TYPE",
					"Meter reading can not be create for : " + connection.getConnectionType() + " connection");
		}
		Set<String> connectionNos = new HashSet<>();
		connectionNos.add(meterReading.getConnectionNo());
		MeterReadingSearchCriteria criteria = MeterReadingSearchCriteria.builder().
				connectionNos(connectionNos).tenantId(meterReading.getTenantId()).build();
		List<MeterReading> previousMeterReading = wSCalculationDao.searchCurrentMeterReadings(criteria);
		if (!CollectionUtils.isEmpty(previousMeterReading)) {
			Double currentMeterReading = previousMeterReading.get(0).getCurrentReading();
			if (meterReading.getCurrentReading() < currentMeterReading && !(meterReading.getMeterStatus() == MeterStatusEnum.RESET)) {
				errorMap.put("INVALID_METER_READING_CONNECTION_NUMBER",
						"Current meter reading has to be greater than the past last readings in the meter reading!");
			}
		}

		if (meterReading.getCurrentReading() < meterReading.getLastReading() && !(meterReading.getMeterStatus() == MeterStatusEnum.RESET)) {
			errorMap.put("INVALID_METER_READING_LAST_READING",
					"Current Meter Reading cannot be less than last meter reading");
		}

		if (StringUtils.isEmpty(meterReading.getMeterStatus())) {
			errorMap.put("INVALID_METER_READING_STATUS", "Meter status can not be null");
		}

		if (isUpdate && (meterReading.getCurrentReading() == null)) {
			errorMap.put("INVALID_CURRENT_METER_READING",
					"Current Meter Reading cannot be update without current meter reading");
		}

		if (isUpdate && !StringUtils.isEmpty(meterReading.getId())) {
			int n = wSCalculationDao.isMeterReadingConnectionExist(Arrays.asList(meterReading.getId()));
			if (n > 0) {
				errorMap.put("INVALID_METER_READING_CONNECTION", "Meter reading Id already present");
			}
		}
		if (StringUtils.isEmpty(meterReading.getBillingPeriod())) {
			errorMap.put("INVALID_BILLING_PERIOD", "Meter Reading cannot be updated without billing period");
		}

		int billingPeriodNumber = wSCalculationDao.isBillingPeriodExists(meterReading.getConnectionNo(),
				meterReading.getBillingPeriod());
		if (billingPeriodNumber > 0)
			errorMap.put("INVALID_METER_READING_BILLING_PERIOD", "Billing Period Already Exists");
		
		// Restricting to add multiple meter reading in a month
		Calendar lastReadingDate = calculationUtil.getCalendar(meterReading.getLastReadingDate());
		Calendar currentReadingDate = calculationUtil.getCalendar(meterReading.getCurrentReadingDate());
		if(lastReadingDate.get(Calendar.MONTH)==currentReadingDate.get(Calendar.MONTH) && lastReadingDate.get(Calendar.YEAR)==currentReadingDate.get(Calendar.YEAR)) {
			errorMap.put("INVALID_CURRENT_METER_READING",
					"Meter reading for the current month has already been captured. Cannot insert more than one meter reading in a month");
		}
		
		if (!errorMap.isEmpty()) {
			throw new CustomException(errorMap);
		}
	}
	
	/**
	 * Billing Period Validation
	 */
	private void validateBillingPeriod(String billingPeriod) {
		if (StringUtils.isEmpty(billingPeriod))
			 throw new CustomException("BILLING_PERIOD_PARSING_ISSUE", "Billing can not empty!!");
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			ZoneId defaultZoneId = ZoneId.systemDefault();
			Date billingDate = sdf.parse(billingPeriod.split("-")[1].trim());
			Instant instant = billingDate.toInstant();
			LocalDate billingLocalDate = instant.atZone(defaultZoneId).toLocalDate();
			LocalDate localDateTime = LocalDate.now();
			if ((billingLocalDate.getYear() == localDateTime.getYear())
					&& (billingLocalDate.getMonthValue() > localDateTime.getMonthValue())) {
				throw new CustomException("BILLING_PERIOD_ISSUE", "Billing period can not be in future!!");
			}
			if ((billingLocalDate.getYear() > localDateTime.getYear())) {
				throw new CustomException("BILLING_PERIOD_ISSUE", "Billing period can not be in future!!");
			}

		} catch (CustomException | ParseException ex) {
			log.error("", ex);
			if (ex instanceof CustomException)
				throw new CustomException("BILLING_PERIOD_ISSUE", "Billing period can not be in future!!");
			throw new CustomException("BILLING_PERIOD_PARSING_ISSUE", "Billing period can not parsed!!");
		}
	}
	
	/**
	 * 
	 * @param meterConnectionRequest
	 *            meterReadingConnectionRequest is request for create or update
	 *            meter reading connection
	 * @param isUpdate
	 *            True for create
	 */
	public void validateUpdateMeterReading(MeterConnectionRequest meterConnectionRequest, boolean isUpdate) {
		MeterReading meterReading = meterConnectionRequest.getMeterReading();
		Map<String, String> errorMap = new HashMap<>();

		// Future Billing Period Check
		validateBillingPeriod(meterReading.getBillingPeriod());

		List<WaterConnection> waterConnectionList = calculationUtil.getWaterConnection(meterConnectionRequest.getRequestInfo(),
				meterReading.getConnectionNo(), meterConnectionRequest.getMeterReading().getTenantId());
		WaterConnection connection = null;
		if(waterConnectionList != null){
			int size = waterConnectionList.size();
			connection = waterConnectionList.get(size-1);
		}

		if (meterConnectionRequest.getMeterReading().getGenerateDemand() && connection == null) {
			errorMap.put("INVALID_METER_READING_CONNECTION_NUMBER", "Invalid water connection number");
		}
		if (connection != null
				&& !WSCalculationConstant.meteredConnectionType.equalsIgnoreCase(connection.getConnectionType())) {
			errorMap.put("INVALID_WATER_CONNECTION_TYPE",
					"Meter reading can not be created for : " + connection.getConnectionType() + " connection");
		}
		Set<String> connectionNos = new HashSet<>();
		connectionNos.add(meterReading.getConnectionNo());

		if (meterReading.getCurrentReading() < meterReading.getLastReading() && !(meterReading.getMeterStatus() == MeterStatusEnum.RESET)) {
			errorMap.put("INVALID_METER_READING_LAST_READING",
					"Current Meter Reading cannot be less than last meter reading");
		}

		if (StringUtils.isEmpty(meterReading.getMeterStatus())) {
			errorMap.put("INVALID_METER_READING_STATUS", "Meter status can not be null");
		}

		if (isUpdate && (meterReading.getCurrentReading() == null)) {
			errorMap.put("INVALID_CURRENT_METER_READING",
					"Current Meter Reading cannot be updated without current meter reading");
		}

		if (isUpdate && !StringUtils.isEmpty(meterReading.getId())) {
			int n = wSCalculationDao.isMeterReadingConnectionExist(Arrays.asList(meterReading.getId()));
			if (n == 0) {
				errorMap.put("INVALID_METER_READING_CONNECTION", "Meter reading does not exist");
			}
		}

		if (StringUtils.isEmpty(meterReading.getBillingPeriod())) {
			errorMap.put("INVALID_BILLING_PERIOD", "Meter Reading cannot be updated without billing period");
		}

		if (!errorMap.isEmpty()) {
			throw new CustomException(errorMap);
		}
	}

	public void validateUpdate(MeterConnectionRequest meterConnectionRequest) {
		// TODO Auto-generated method stub
		Map<String, String> errorMap = new HashMap<>();

		Set<String> connectionNos = new HashSet<>();
		connectionNos.add(meterConnectionRequest.getMeterReading().getConnectionNo());

		MeterReadingSearchCriteria criteria = MeterReadingSearchCriteria.builder().tenantId(meterConnectionRequest.getMeterReading().getTenantId()).connectionNos(connectionNos).build();
		List<MeterReading> meterReadings = wSCalculationDao.searchMeterReadings(criteria);
		if(meterReadings.isEmpty()) {
			errorMap.put("INVALID_METER_READING", "Meter reading does not exist");
		}

		if(!meterReadings.isEmpty()) {
			MeterReading meterReading = meterReadings.get(0);
			if(!meterReading.getId().equals(meterConnectionRequest.getMeterReading().getId())) {
				errorMap.put("INVALID_METER_READING_UPDATE", "Only the last meter reading entry is allowed to update");
			}
		}

		if (!errorMap.isEmpty()) {
			throw new CustomException(errorMap);
		}

	}

	public void validateAnnualAdvance(RequestInfo requestInfo, String tenantId, String connectionNo) {
		Map<String, String> errorMap = new HashMap<>();
		
		List<WaterConnection> waterConnectionList = calculationUtil.getWaterConnection(requestInfo,connectionNo, tenantId);
		WaterConnection connection = null;
		if(waterConnectionList != null){
			int size = waterConnectionList.size();
			connection = waterConnectionList.get(size-1);
		}
		
		if(connection != null) {
			String assessYear = calculationUtil.getFinancialYear();
			List<AnnualAdvance> annualAdvances = wSCalculationDao.getAnnualAdvance(tenantId, connectionNo, assessYear);
			if(!annualAdvances.isEmpty()) {
				errorMap.put("INVALID_ANNUAL_ADVANCE", "Annual advance already applied on this connection for this financial year");
			}
			
		} else {
			errorMap.put("INVALID_WATER_CONNECTION", "No connection found with this connectionNo");
		}
		
		if (!errorMap.isEmpty()) {
			throw new CustomException(errorMap);
		}
	}

}
