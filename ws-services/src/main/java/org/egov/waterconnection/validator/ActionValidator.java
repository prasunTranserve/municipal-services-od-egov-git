package org.egov.waterconnection.validator;


import java.util.HashMap;
import java.util.Map;

import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.workflow.BusinessService;
import org.egov.waterconnection.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ActionValidator {
	
	@Autowired
	private WorkflowService workflowService;
	
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Validate update request
	 * 
	 * @param request Water Connection Request
	 * @param businessService BusinessService
	 */
	public void validateUpdateRequest(WaterConnectionRequest request, BusinessService businessService, String applicationStatus) {
		validateLabourFeeForUpdate(request);
		validateDocumentsForUpdate(request);
		validateIds(request, businessService, applicationStatus);
	}
	
	/**
	 * Validate Labour Fee for water connection
	 * 
	 * @param request water connection request
	 */

	private void validateLabourFeeForUpdate(WaterConnectionRequest request) {
		if(request.getWaterConnection().getAdditionalDetails() != null) {
			HashMap<String, Object> addDetail = mapper.convertValue(request.getWaterConnection().getAdditionalDetails(), HashMap.class);
			
			if(addDetail.containsKey(WCConstants.IS_LABOUR_FEE_APPLICABLE) 
					&& WCConstants.YES.equalsIgnoreCase(addDetail.get(WCConstants.IS_LABOUR_FEE_APPLICABLE).toString())) {
				if(WCConstants.CONNECTION_TEMPORARY.equalsIgnoreCase(request.getWaterConnection().getConnectionCategory())) {
					throw new CustomException("INVALID_LABOUR_FEE",
							"Labour fee is not applicable on Temporary connection");
				} else if(WCConstants.METERED_CONNECTION.equalsIgnoreCase(request.getWaterConnection().getConnectionType())
						&& !(WCConstants.CONNECTION_DOMESTIC.equalsIgnoreCase(request.getWaterConnection().getUsageCategory())
								|| WCConstants.CONNECTION_BPL.equalsIgnoreCase(request.getWaterConnection().getUsageCategory()))) {
					throw new CustomException("INVALID_LABOUR_FEE",
							"Labour fee is applicable on Permanent Metered and Domestic or BPL connection only");
				} else if(WCConstants.NON_METERED_CONNECTION.equalsIgnoreCase(request.getWaterConnection().getConnectionType())
						&& !WCConstants.CONNECTION_BPL.equalsIgnoreCase(request.getWaterConnection().getUsageCategory())) {
					throw new CustomException("INVALID_LABOUR_FEE",
							"Labour fee is applicable on Permanent Non Metered and BPL connection only");
				}
			}
		}
	}

	/**
	 * Validate documents for water connection
	 * 
	 * @param request water connection request
	 */
	private void validateDocumentsForUpdate(WaterConnectionRequest request) {
		if (request.getWaterConnection().getProcessInstance().getAction().equalsIgnoreCase(WCConstants.ACTION_INITIATE)
				&& request.getWaterConnection().getDocuments() != null) {
			throw new CustomException("INVALID_STATUS",
					"Status cannot be INITIATE when application document are provided");
		}
	}
	
	/**
	 * Validate Id's if update is not in update-able state
	 * 
	 * @param request WaterConnectionRequest
	 * @param businessService BusinessService
	 */
	private void validateIds(WaterConnectionRequest request, BusinessService businessService, String applicationStatus) {
		WaterConnection connection = request.getWaterConnection();
		Map<String, String> errorMap = new HashMap<>();
		if (!workflowService.isStateUpdatable(applicationStatus, businessService)) {
			if (connection.getId() == null)
				errorMap.put("INVALID_UPDATE", "Id of waterConnection cannot be null");
			if (!CollectionUtils.isEmpty(connection.getDocuments())) {
				connection.getDocuments().forEach(document -> {
					if (document.getId() == null)
						errorMap.put("INVALID_UPDATE", "Id of document cannot be null");
				});
			}
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}
}
