package org.egov.waterconnection.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.web.models.ValidatorResult;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MeterInfoValidator implements WaterActionValidator {

	@Autowired
	private ObjectMapper mapper;

	@Override
	@SuppressWarnings("unchecked")
	public ValidatorResult validate(WaterConnectionRequest waterConnectionRequest, int reqType) {
		Map<String, String> errorMap = new HashMap<>();
		switch (reqType) {
		case WCConstants.MODIFY_CONNECTION:
			handleModifyConnectionRequest(waterConnectionRequest, errorMap);
			break;
		case WCConstants.UPDATE_APPLICATION:
			handleUpdateApplicationRequest(waterConnectionRequest, errorMap);
			break;
		case WCConstants.METER_REPLACE:
			handleMeterReplacementRequest(waterConnectionRequest, errorMap);
			break;	
		default:
			break;
		}
		if (!errorMap.isEmpty())
			return new ValidatorResult(false, errorMap);
		return new ValidatorResult(true, errorMap);
	}
	
	private void handleUpdateApplicationRequest(WaterConnectionRequest waterConnectionRequest,
			Map<String, String> errorMap) {
		if (WCConstants.ACTIVATE_CONNECTION_CONST
				.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			if (WCConstants.METERED_CONNECTION
					.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getConnectionType())) {
				validateMeteredConnectionRequst(waterConnectionRequest, errorMap);
			}
		}
	}
	
	private void handleModifyConnectionRequest(WaterConnectionRequest waterConnectionRequest,
			Map<String, String> errorMap) {
		if (WCConstants.APPROVE_CONNECTION
				.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			if (WCConstants.METERED_CONNECTION
					.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getConnectionType())) {
//				validateMeteredConnectionRequst(waterConnectionRequest, errorMap);
			}
		}
	}
	
	private void handleMeterReplacementRequest(WaterConnectionRequest waterConnectionRequest,
			Map<String, String> errorMap) {
		if (WCConstants.APPROVE_CONNECTION
				.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			if (WCConstants.METERED_CONNECTION
					.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getConnectionType())) {
				validateMeteredConnectionRequst(waterConnectionRequest, errorMap);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void validateMeteredConnectionRequst(WaterConnectionRequest waterConnectionRequest,
			Map<String, String> errorMap) {

		Integer allowedMaxMeterDigits[] = {4, 5, 6, 7, 8};

		if (waterConnectionRequest.getWaterConnection().getMeterId() == null) {
			errorMap.put("INVALID_METER_ID", "Meter Id cannot be empty");
		}
		if (waterConnectionRequest.getWaterConnection().getMeterInstallationDate() == null
				|| waterConnectionRequest.getWaterConnection().getMeterInstallationDate() < 0
				|| waterConnectionRequest.getWaterConnection().getMeterInstallationDate() == 0) {
			errorMap.put("INVALID_METER_INSTALLATION_DATE",
					"Meter Installation date cannot be null or negative");
		}
		HashMap<String, Object> addDetail = mapper.convertValue(
				waterConnectionRequest.getWaterConnection().getAdditionalDetails(), HashMap.class);
		if (StringUtils.isEmpty(addDetail)
				|| addDetail.getOrDefault(WCConstants.INITIAL_METER_READING_CONST, null) == null) {
			errorMap.put("INVALID_INITIAL_METER_READING", "Initial meter reading can not be null");
		} else {
			BigDecimal initialMeterReading = BigDecimal.ZERO;
			initialMeterReading = new BigDecimal(
					String.valueOf(addDetail.get(WCConstants.INITIAL_METER_READING_CONST)));
			if (initialMeterReading.compareTo(BigDecimal.ZERO) == 0 || initialMeterReading.compareTo(BigDecimal.ZERO) == -1 ) {
				errorMap.put("INVALID_INITIAL_METER_READING", "Initial meter reading can not be zero or negative");
			}
		}
		if(StringUtils.isEmpty(addDetail.get(WCConstants.MAX_METER_DIGITS_CONST))) {
				errorMap.put("Invalid_Max_Meter_Digits", "Maximum meter reading can not be zero or negative");
		} else {
			if(!Arrays.asList(allowedMaxMeterDigits).contains((Integer)(addDetail.get(WCConstants.MAX_METER_DIGITS_CONST)))) {
				errorMap.put("Invalid_Max_Meter_Digits", "Max meter digits has to be in between " + allowedMaxMeterDigits[0] + " to " + allowedMaxMeterDigits[allowedMaxMeterDigits.length - 1] + " range.");
		}
	}
	}	
}
