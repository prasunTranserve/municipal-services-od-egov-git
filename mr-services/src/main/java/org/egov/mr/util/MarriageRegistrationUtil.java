package org.egov.mr.util;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.repository.ServiceRequestRepository;
import org.egov.mr.web.models.AuditDetails;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.workflow.BusinessService;
import org.egov.mr.workflow.WorkflowService;
import org.egov.mr.util.MRConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MarriageRegistrationUtil {

	private MRConfiguration config;

	private ServiceRequestRepository serviceRequestRepository;

	private WorkflowService workflowService;




	@Autowired
	public MarriageRegistrationUtil(MRConfiguration config, ServiceRequestRepository serviceRequestRepository,
			WorkflowService workflowService) {
		super();
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.workflowService = workflowService;
	}


	/**
	 * Method to return auditDetails for create/update flows
	 *
	 * @param by
	 * @param isCreate
	 * @return AuditDetails
	 */
	public AuditDetails getAuditDetails(String by, Boolean isCreate) {
		Long time = System.currentTimeMillis();
		if(isCreate)
			return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time).build();
		else
			return AuditDetails.builder().lastModifiedBy(by).lastModifiedTime(time).build();
	}









	/**
	 * Creates a map of id to isStateUpdatable
	 * @param searchresult marriageRegistrations from DB
	 * @param businessService The businessService configuration
	 * @return Map of is to isStateUpdatable
	 */
	public Map<String, Boolean> getIdToIsStateUpdatableMap(BusinessService businessService, List<MarriageRegistration> searchresult) {
		Map<String, Boolean> idToIsStateUpdatableMap = new HashMap<>();
		searchresult.forEach(result -> {
			
				idToIsStateUpdatableMap.put(result.getId(), workflowService.isStateUpdatable(result.getStatus(), businessService));
		});
		return idToIsStateUpdatableMap;
	}


	public Object mDMSCall(MarriageRegistration marriageRegistrationRequest , RequestInfo requestInfo){
        String tenantId = marriageRegistrationRequest.getTenantId();
        MdmsCriteriaReq mdmsCriteriaReq = getMDMSRequest(requestInfo,tenantId);
        Object result = serviceRequestRepository.fetchResult(getMdmsSearchUrl(), mdmsCriteriaReq);
        return result;
    }

	
    private MdmsCriteriaReq getMDMSRequest(RequestInfo requestInfo,String tenantId){
        ModuleDetail registrationFeeRequest = getRegistrationFeeRequest();

        List<ModuleDetail> moduleDetails = new LinkedList<>();
        moduleDetails.add(registrationFeeRequest);

        MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId)
                .build();

        MdmsCriteriaReq mdmsCriteriaReq = MdmsCriteriaReq.builder().mdmsCriteria(mdmsCriteria)
                .requestInfo(requestInfo).build();
        return mdmsCriteriaReq;
    }
    
    /**
     * Returns the url for mdms search endpoint
     *
     * @return url for mdms search endpoint
     */
    public StringBuilder getMdmsSearchUrl() {
        return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsEndPoint());
    }
    
  
    
    private ModuleDetail getRegistrationFeeRequest() {

        List<MasterDetail> mrMasterDetails = new ArrayList<>();

        mrMasterDetails.add(MasterDetail.builder().name(MRConstants.MDMS_REGISTRATION_FEE).build());

        ModuleDetail mrModuleDtls = ModuleDetail.builder().masterDetails(mrMasterDetails)
                .moduleName(MRConstants.MDMS_MARRIAGE_REGISTRATION).build();



        return mrModuleDtls;
    }


}
