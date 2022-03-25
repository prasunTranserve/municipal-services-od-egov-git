package org.egov.waterconnection.validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;

import com.jayway.jsonpath.JsonPath;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.repository.ServiceRequestRepository;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.egov.waterconnection.web.models.RoadCuttingInfo;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MDMSValidator {
	@Autowired
	private WaterServicesUtil waterServicesUtil;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Value("${egov.mdms.host}")
	private String mdmsHost;

	@Value("${egov.mdms.search.endpoint}")
	private String mdmsEndpoint;
    
	/**
	 * Validate Master data for given request
	 * 
	 * @param request
	 * @param reqType
	 */
	public void validateMasterData(WaterConnectionRequest request, int reqType) {
		switch (reqType) {
			case WCConstants.UPDATE_APPLICATION:
				validateMasterDataForUpdateConnection(request);
				break;
			case WCConstants.MODIFY_CONNECTION:
				validateMasterDataForModifyConnection(request);
				break;
			default:
				break;
		}
	}

	public void validateMasterDataForUpdateConnection(WaterConnectionRequest request) {
		if (request.getWaterConnection().getProcessInstance().getAction()
				.equalsIgnoreCase(WCConstants.ACTIVATE_CONNECTION_CONST)) {
			String jsonPath = WCConstants.JSONPATH_ROOT;
			String taxjsonPath = WCConstants.TAX_JSONPATH_ROOT;
			String tenantId = request.getWaterConnection().getTenantId();
			List<String> names = new ArrayList<>(Arrays.asList(WCConstants.MDMS_WC_CONNECTION_TYPE,
					WCConstants.MDMS_WC_CONNECTION_CATEGORY, WCConstants.MDMS_WC_WATER_SOURCE, WCConstants.MDMS_WC_CONNECTION_FACILITY));
			Map<String, List<String>> codes = getAttributeValues(tenantId, WCConstants.MDMS_WC_MOD_NAME, names,
					"$.*.code", jsonPath, request.getRequestInfo());
			List<String> taxModelnames = new ArrayList<>(Arrays.asList(WCConstants.WC_ROADTYPE_MASTER));
			Map<String, List<String>> codeFromCalculatorMaster = getAttributeValues(tenantId, WCConstants.WS_TAX_MODULE,
					taxModelnames, "$.*.code", taxjsonPath, request.getRequestInfo());

			// merge codes
			String[] finalmasterNames = {WCConstants.MDMS_WC_CONNECTION_TYPE, WCConstants.MDMS_WC_CONNECTION_CATEGORY,
					WCConstants.MDMS_WC_WATER_SOURCE, WCConstants.WC_ROADTYPE_MASTER, WCConstants.MDMS_WC_CONNECTION_FACILITY};
			Map<String, List<String>> finalcodes = Stream.of(codes, codeFromCalculatorMaster).map(Map::entrySet)
					.flatMap(Collection::stream).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			validateMDMSData(finalmasterNames, finalcodes);
			validateCodes(request.getWaterConnection(), finalcodes);
			validateInstallment(request, finalcodes);
		}
	}

	private void validateInstallment(WaterConnectionRequest waterConnectionRequest, Map<String, List<String>> finalcodes) {
		Map<String, String> errorMap = new HashMap<>();
		WaterConnection waterConnection = waterConnectionRequest.getWaterConnection();
		
		Map<String, List<LinkedHashMap>> allInstallments = null;
		List<LinkedHashMap> installments = null;
		allInstallments = getMdmsAttributeValues(waterConnection.getTenantId(), WCConstants.WS_TAX_MODULE, Collections.singletonList(WCConstants.MDMS_WC_INSTALLMENT),
				null, WCConstants.TAX_JSONPATH_ROOT, waterConnectionRequest.getRequestInfo());
		installments = allInstallments.get("Installment");
		
		if(Objects.isNull(allInstallments) || Objects.isNull(installments)) {
			errorMap.put("MDMS DATA ERROR ", "Unable to fetch " + WCConstants.MDMS_WC_INSTALLMENT + " codes from MDMS");
			return;
		}
		
		boolean isLabourFeeApplicable = Boolean.FALSE;
		boolean isInstallmentApplicable = Boolean.FALSE;
		BigDecimal labourFee = BigDecimal.ZERO;
		int noOfLabourFeeInstallments = 0;

		BigDecimal scrutinyFee = BigDecimal.ZERO;
		boolean isInstallmentApplicableForScrutinyFee = Boolean.FALSE;
		int noOfScrutinyFeeInstallments = 0;

		LinkedHashMap additionalDetailJsonNode = (LinkedHashMap)waterConnection.getAdditionalDetails();

		if(additionalDetailJsonNode.containsKey(WCConstants.IS_LABOUR_FEE_APPLICABLE)
				&& !StringUtils.isEmpty(additionalDetailJsonNode.get(WCConstants.IS_LABOUR_FEE_APPLICABLE))) {
			isLabourFeeApplicable = additionalDetailJsonNode.get(WCConstants.IS_LABOUR_FEE_APPLICABLE).toString().equalsIgnoreCase("Y") 
					? Boolean.TRUE:Boolean.FALSE;
		}

		if(additionalDetailJsonNode.containsKey(WCConstants.IS_INSTALLMENT_APPLICABLE)
				&& !StringUtils.isEmpty(additionalDetailJsonNode.get(WCConstants.IS_INSTALLMENT_APPLICABLE))) {
			isInstallmentApplicable = additionalDetailJsonNode.get(WCConstants.IS_INSTALLMENT_APPLICABLE).toString().equalsIgnoreCase("Y") 
					? Boolean.TRUE:Boolean.FALSE;
		}

		//Validate amount for Labour Fee
		if(isLabourFeeApplicable && isInstallmentApplicable) {
			/*Temporary stopping metered connection for installment
			if(WCConstants.METERED_CONNECTION.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getConnectionType())) {
				errorMap.put("INVALID_LABOUR_FEE_INSTALLMENT", "Labour fee installment is not applicable for Metered connection");
			}
			*/

			if(additionalDetailJsonNode.containsKey(WCConstants.NO_OF_LABOUR_FEE_INSTALLMENTS)
					&& !StringUtils.isEmpty(additionalDetailJsonNode.get(WCConstants.NO_OF_LABOUR_FEE_INSTALLMENTS))) {
				noOfLabourFeeInstallments = Integer.parseInt(additionalDetailJsonNode.get(WCConstants.NO_OF_LABOUR_FEE_INSTALLMENTS).toString());
			}
			if(additionalDetailJsonNode.containsKey(WCConstants.LABOUR_FEE_INSTALLMENT_AMOUNT)
					&& !StringUtils.isEmpty(additionalDetailJsonNode.get(WCConstants.LABOUR_FEE_INSTALLMENT_AMOUNT))) {
				labourFee = new BigDecimal(additionalDetailJsonNode.get(WCConstants.LABOUR_FEE_INSTALLMENT_AMOUNT).toString());
			}

			if((WCConstants.METERED_CONNECTION.equalsIgnoreCase(waterConnection.getConnectionType())
				&& ((WCConstants.CONNECTION_DOMESTIC.equalsIgnoreCase(waterConnection.getUsageCategory())
								&& waterConnection.getNoOfFlats().compareTo(0) <= 0)
					|| WCConstants.CONNECTION_BPL.equalsIgnoreCase(waterConnection.getUsageCategory())))
				|| (WCConstants.NON_METERED_CONNECTION.equalsIgnoreCase(waterConnection.getConnectionType())
						&& ((WCConstants.CONNECTION_TEMPORARY.equalsIgnoreCase(waterConnection.getConnectionCategory())
								&& WCConstants.CONNECTION_ROAD_SIDE_EATERS.equalsIgnoreCase(waterConnection.getUsageCategory()))
							|| (WCConstants.CONNECTION_PERMANENT.equalsIgnoreCase(waterConnection.getConnectionCategory())
									&& ((WCConstants.CONNECTION_DOMESTIC.equalsIgnoreCase(waterConnection.getUsageCategory())
											&& waterConnection.getNoOfFlats().compareTo(0) <= 0)
										|| WCConstants.CONNECTION_BPL.equalsIgnoreCase(waterConnection.getUsageCategory())))))) {

				for(LinkedHashMap installmentObj : installments) {
					if(!Objects.isNull(installmentObj.get("feeType")) && WCConstants.WS_LABOUR_FEE.equalsIgnoreCase(installmentObj.get("feeType").toString())
							&& !Objects.isNull(installmentObj.get("usageCategory")) && WCConstants.CONNECTION_DOMESTIC.equalsIgnoreCase(installmentObj.get("usageCategory").toString())
							&& !Objects.isNull(installmentObj.get("noOfInstallment")) && Integer.parseInt(installmentObj.get("noOfInstallment").toString()) == noOfLabourFeeInstallments) {
						if(!Objects.isNull(installmentObj.get("installmentAmount")) && new BigDecimal(installmentObj.get("installmentAmount").toString()).compareTo(labourFee) != 0) {
							errorMap.put("INVALID_LABOUR_FEE_INSTALLMENT_AMOUNT", "Invalid installment amount for Labour Fee");
						}
					}
				}
			}

		}

		if(additionalDetailJsonNode.containsKey(WCConstants.IS_INSTALLMENT_APPLICABLE_FOR_SCRUTINY_FEE)
				&& !StringUtils.isEmpty(additionalDetailJsonNode.get(WCConstants.IS_INSTALLMENT_APPLICABLE_FOR_SCRUTINY_FEE))) {
			isInstallmentApplicableForScrutinyFee = additionalDetailJsonNode.get(WCConstants.IS_INSTALLMENT_APPLICABLE_FOR_SCRUTINY_FEE).toString().equalsIgnoreCase("Y") 
					? Boolean.TRUE:Boolean.FALSE;
		}

		//Validate amount for Scrutiny Fee
		if(isInstallmentApplicableForScrutinyFee) {
			/* Temporary stopping metered connection for installment
			if(WCConstants.METERED_CONNECTION.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getConnectionType())) {
				errorMap.put("INVALID_SCRUTINY_FEE_INSTALLMENT", "Scrutiny fee installment is not applicable for Metered connection");
			}
			*/
			
			if(additionalDetailJsonNode.containsKey(WCConstants.NO_OF_SCRUTINY_FEE_INSTALLMENTS)
					&& !StringUtils.isEmpty(additionalDetailJsonNode.get(WCConstants.NO_OF_SCRUTINY_FEE_INSTALLMENTS))) {
				noOfScrutinyFeeInstallments = Integer.parseInt(additionalDetailJsonNode.get(WCConstants.NO_OF_SCRUTINY_FEE_INSTALLMENTS).toString());
			}
			if(additionalDetailJsonNode.containsKey(WCConstants.SCRUTINY_FEE_INSTALLMENT_AMOUNT)
					&& !StringUtils.isEmpty(additionalDetailJsonNode.get(WCConstants.SCRUTINY_FEE_INSTALLMENT_AMOUNT))) {
				scrutinyFee = new BigDecimal(additionalDetailJsonNode.get(WCConstants.SCRUTINY_FEE_INSTALLMENT_AMOUNT).toString());
			}
			if(WCConstants.CONNECTION_PERMANENT.equalsIgnoreCase(waterConnection.getConnectionCategory())
					&& WCConstants.CONNECTION_DOMESTIC.equalsIgnoreCase(waterConnection.getUsageCategory())
					&& waterConnection.getNoOfFlats().compareTo(0) <= 0) {
				for(LinkedHashMap installmentObj : installments) {
					if(!Objects.isNull(installmentObj.get("feeType")) && WCConstants.WS_SCRUTINY_FEE.equalsIgnoreCase(installmentObj.get("feeType").toString())
							&& !Objects.isNull(installmentObj.get("usageCategory")) && WCConstants.CONNECTION_DOMESTIC.equalsIgnoreCase(installmentObj.get("usageCategory").toString())
							&& !Objects.isNull(installmentObj.get("noOfInstallment")) && Integer.parseInt(installmentObj.get("noOfInstallment").toString()) == noOfScrutinyFeeInstallments) {
						if(!Objects.isNull(installmentObj.get("installmentAmount")) && new BigDecimal(installmentObj.get("installmentAmount").toString()).compareTo(scrutinyFee) != 0) {
							errorMap.put("INVALID_SCRUTINY_FEE_INSTALLMENT_AMOUNT", "Invalid installment amount for Scrutiny Fee");
						}
					}
				}
			}
		}
	}

	public Map<String, List<String>> getAttributeValues(String tenantId, String moduleName, List<String> names,
			String filter, String jsonPath, RequestInfo requestInfo) {
		StringBuilder uri = new StringBuilder(mdmsHost).append(mdmsEndpoint);
		MdmsCriteriaReq criteriaReq = waterServicesUtil.prepareMdMsRequest(tenantId, moduleName, names, filter,
				requestInfo);
		try {

			Object result = serviceRequestRepository.fetchResult(uri, criteriaReq);
			return JsonPath.read(result, jsonPath);
		} catch (Exception e) {
			throw new CustomException(WCConstants.INVALID_CONNECTION_TYPE, WCConstants.INVALID_CONNECTION_TYPE);
		}
	}
 
	private Map<String, List<LinkedHashMap>> getMdmsAttributeValues(String tenantId, String moduleName, List<String> names,
			String filter, String jsonPath, RequestInfo requestInfo) {
		StringBuilder uri = new StringBuilder(mdmsHost).append(mdmsEndpoint);
		MdmsCriteriaReq criteriaReq = waterServicesUtil.prepareMdMsRequest(tenantId, moduleName, names, filter,
				requestInfo);
		try {

			Object result = serviceRequestRepository.fetchResult(uri, criteriaReq);
			return JsonPath.read(result, jsonPath);
		} catch (Exception e) {
			throw new CustomException(WCConstants.INVALID_CONNECTION_TYPE, WCConstants.INVALID_CONNECTION_TYPE);
		}
	}

	private void validateMDMSData(String[] masterNames, Map<String, List<String>> codes) {
		Map<String, String> errorMap = new HashMap<>();
		for (String masterName : masterNames) {
			if (CollectionUtils.isEmpty(codes.get(masterName))) {
				errorMap.put("MDMS DATA ERROR ", "Unable to fetch " + masterName + " codes from MDMS");
			}
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

	/**
	 * validateCodes will validate for given fields and return error map if codes
	 * are not matching
	 * 
	 * @param waterConnection
	 *            WaterConnection Object
	 * @param codes
	 *            List of codes
	 * @return error map for given fields
	 */
	private void validateCodes(WaterConnection waterConnection, Map<String, List<String>> codes) {
		Map<String, String> errorMap = new HashMap<>();
		StringBuilder messageBuilder = new StringBuilder();
		if (!StringUtils.isEmpty(waterConnection.getConnectionType())
				&& !codes.get(WCConstants.MDMS_WC_CONNECTION_TYPE).contains(waterConnection.getConnectionType())) {
			messageBuilder = new StringBuilder();
			messageBuilder.append("Connection type value is invalid, please enter proper value! ");
			errorMap.put("INVALID_WATER_CONNECTION_TYPE", messageBuilder.toString());
		}
		if (!StringUtils.isEmpty(waterConnection.getWaterSource())
				&& !WCConstants.SERVICE_SEWERAGE.equalsIgnoreCase(waterConnection.getConnectionFacility())
				&& !codes.get(WCConstants.MDMS_WC_WATER_SOURCE).contains(waterConnection.getWaterSource())) {
			messageBuilder = new StringBuilder();
			messageBuilder.append("Water Source / Water Sub Source value is invalid, please enter proper value! ");
			errorMap.put("INVALID_WATER_CONNECTION_SOURCE", messageBuilder.toString());
		}
		/*if (!StringUtils.isEmpty(waterConnection.getRoadType())
				&& !codes.get(WCConstants.WC_ROADTYPE_MASTER).contains(waterConnection.getRoadType())) {
			messageBuilder = new StringBuilder();
			messageBuilder.append("Road type value is invalid, please enter proper value! ");
			errorMap.put("INVALID_WATER_ROAD_TYPE", messageBuilder.toString());
		}*/

		// if(waterConnection.getRoadCuttingInfo() == null){
		// 	errorMap.put("INVALID_ROAD_INFO", "Road Cutting Information should not be empty");
		// }

		if(waterConnection.getRoadCuttingInfo() != null){
			for(RoadCuttingInfo roadCuttingInfo : waterConnection.getRoadCuttingInfo()){
				if (!StringUtils.isEmpty(roadCuttingInfo.getRoadType())
						&& !codes.get(WCConstants.WC_ROADTYPE_MASTER).contains(roadCuttingInfo.getRoadType())) {
					messageBuilder = new StringBuilder();
					messageBuilder.append("Road type value is invalid, please enter proper value! ");
					errorMap.put("INVALID_WATER_ROAD_TYPE", messageBuilder.toString());
				}
			}
		}
		
		if (!StringUtils.isEmpty(waterConnection.getConnectionFacility())
				&& !codes.get(WCConstants.MDMS_WC_CONNECTION_FACILITY).contains(waterConnection.getConnectionFacility())) {
			messageBuilder = new StringBuilder();
			messageBuilder.append("Connection facility value is invalid, please enter proper value! ");
			errorMap.put("INVALID_WATER_CONNECTION_Facility", messageBuilder.toString());
		}
		
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

	/**
	 * Validate master data of water connection request
	 *
	 * @param request waterconnection request
	 */
	public void validateMasterForCreateRequest(WaterConnectionRequest request) {
		// calling property related master
		List<String> propertyModuleMasters = new ArrayList<>(Arrays.asList(WCConstants.PROPERTY_OWNERTYPE));
		Map<String, List<String>> codesFromMasters = getAttributeValues(request.getWaterConnection().getTenantId(),
				WCConstants.PROPERTY_MASTER_MODULE, propertyModuleMasters, "$.*.code",
				WCConstants.PROPERTY_JSONPATH_ROOT, request.getRequestInfo());
		List<String> wnsModuleMasters = new ArrayList<>(Arrays.asList(WCConstants.MDMS_WC_CONNECTION_FACILITY));
		Map<String, List<String>> codesFromWnsMasters = getAttributeValues(request.getWaterConnection().getTenantId(),
				WCConstants.MDMS_WC_MOD_NAME, wnsModuleMasters, "$.*.code",
				WCConstants.WS_SERVICES_JSONPATH_ROOT, request.getRequestInfo());
		codesFromMasters.putAll(codesFromWnsMasters);
		
		// merge codes
		String[] finalmasterNames = {WCConstants.PROPERTY_OWNERTYPE, WCConstants.MDMS_WC_CONNECTION_FACILITY};
		validateMDMSData(finalmasterNames, codesFromMasters);
		validateCodesForCreateRequest(request, codesFromMasters);
	}

	/**
	 *
	 * @param request Water connection request
	 * @param codes list of master data codes to varify against the water connection request
	 */
	public void validateCodesForCreateRequest(WaterConnectionRequest request, Map<String, List<String>> codes) {
		Map<String, String> errorMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(request.getWaterConnection().getConnectionHolders())) {
			request.getWaterConnection().getConnectionHolders().forEach(holderDetail -> {
				if (!StringUtils.isEmpty(holderDetail.getOwnerType())
						&& !codes.get(WCConstants.PROPERTY_OWNERTYPE).contains(holderDetail.getOwnerType())) {
					errorMap.put("INVALID_CONNECTION_HOLDER_TYPE",
							"The Connection holder type '" + holderDetail.getOwnerType() + "' does not exists");
				}
			});
		}
		
		if(!StringUtils.hasText(request.getWaterConnection().getConnectionFacility())
				|| !codes.get(WCConstants.MDMS_WC_CONNECTION_FACILITY).contains(request.getWaterConnection().getConnectionFacility())) {
			errorMap.put("INVALID_CONNECTION_FACILITY",
					"The Connection facility '" + request.getWaterConnection().getConnectionFacility() + "' does not exists");
		}

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}
	
	public void validateMasterDataForModifyConnection(WaterConnectionRequest request) {
		if (request.getWaterConnection().getProcessInstance().getAction()
				.equalsIgnoreCase(WCConstants.APPROVE_CONNECTION)) {
			String jsonPath = WCConstants.JSONPATH_ROOT;
			String taxjsonPath = WCConstants.TAX_JSONPATH_ROOT;
			String tenantId = request.getWaterConnection().getTenantId();
			List<String> names = new ArrayList<>(Arrays.asList(WCConstants.MDMS_WC_CONNECTION_TYPE,
					WCConstants.MDMS_WC_CONNECTION_CATEGORY, WCConstants.MDMS_WC_WATER_SOURCE, WCConstants.MDMS_WC_CONNECTION_FACILITY));
			Map<String, List<String>> codes = getAttributeValues(tenantId, WCConstants.MDMS_WC_MOD_NAME, names,
					"$.*.code", jsonPath, request.getRequestInfo());
			List<String> taxModelnames = new ArrayList<>(Arrays.asList(WCConstants.WC_ROADTYPE_MASTER));
			Map<String, List<String>> codeFromCalculatorMaster = getAttributeValues(tenantId, WCConstants.WS_TAX_MODULE,
					taxModelnames, "$.*.code", taxjsonPath, request.getRequestInfo());
			// merge codes
			String[] finalmasterNames = {WCConstants.MDMS_WC_CONNECTION_TYPE, WCConstants.MDMS_WC_CONNECTION_CATEGORY,
					WCConstants.MDMS_WC_WATER_SOURCE, WCConstants.WC_ROADTYPE_MASTER, WCConstants.MDMS_WC_CONNECTION_FACILITY};
			Map<String, List<String>> finalcodes = Stream.of(codes, codeFromCalculatorMaster).map(Map::entrySet)
					.flatMap(Collection::stream).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			validateMDMSData(finalmasterNames, finalcodes);
			validateCodes(request.getWaterConnection(), finalcodes);
		}
	}
}
