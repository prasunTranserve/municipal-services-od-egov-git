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
import org.egov.mr.model.user.Citizen;
import org.egov.mr.model.user.UserResponse;
import org.egov.mr.model.user.UserSearchRequest;
import org.egov.mr.repository.ServiceRequestRepository;
import org.egov.mr.web.models.AuditDetails;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.workflow.BusinessService;
import org.egov.mr.workflow.WorkflowService;
import org.egov.mr.util.MRConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MarriageRegistrationUtil {

	private MRConfiguration config;

	private ServiceRequestRepository serviceRequestRepository;

	private WorkflowService workflowService;

	private ObjectMapper mapper;


	@Autowired
	public MarriageRegistrationUtil(MRConfiguration config, ServiceRequestRepository serviceRequestRepository,
			WorkflowService workflowService,ObjectMapper mapper) {
		super();
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.workflowService = workflowService;
		this.mapper = mapper;
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

    
    public Citizen getMobileNumberWithUuid(String uuid, RequestInfo requestInfo, String tenantId) {
    	try {
			List<String>  uuidList = new ArrayList<String>();
			uuidList.add(uuid);
			UserSearchRequest searchRequest = UserSearchRequest.builder().uuid(uuidList)
					.tenantId(tenantId).userType(MRConstants.ROLE_CITIZEN).requestInfo(requestInfo).build();
			StringBuilder url = new StringBuilder(config.getUserHost()+config.getUserSearchEndpoint()); 
			UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, searchRequest), UserResponse.class);
			if(CollectionUtils.isEmpty(res.getUser())) {
				return null;
			}
			return res.getUser().get(0);
		} catch (Exception e) {
			log.error(" User not found with this uuid "+uuid);
			return null;
		}
	}
    
    public boolean checkUserPresentWithUuid(String uuid, RequestInfo requestInfo, String tenantId) {
		List<String>  uuidList = new ArrayList<>();
		uuidList.add(uuid);
		UserSearchRequest searchRequest = UserSearchRequest.builder().uuid(uuidList).active(true)
				.tenantId(tenantId).userType(MRConstants.ROLE_CITIZEN).requestInfo(requestInfo).build();
		StringBuilder url = new StringBuilder(config.getUserHost()+config.getUserSearchEndpoint()); 
		UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, searchRequest), UserResponse.class);
		if(CollectionUtils.isEmpty(res.getUser())) {
			return false;
		}
		return true;
	}
    /**
     * 
     * @param mobileNumber
     * @param requestInfo
     * @param tenantId
     * @return
     */
    public String isUserPresent(String mobileNumber, RequestInfo requestInfo, String tenantId) {
		UserSearchRequest searchRequest = UserSearchRequest.builder().userName(mobileNumber)
				.tenantId(tenantId).userType(MRConstants.ROLE_CITIZEN).requestInfo(requestInfo).build();
		StringBuilder url = new StringBuilder(config.getUserHost()+config.getUserSearchEndpoint()); 
		UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, searchRequest), UserResponse.class);
		if(CollectionUtils.isEmpty(res.getUser())) {
			return null;
		}
		return res.getUser().get(0).getUuid().toString();
	}
    
    /**
     * 
     * @param mobileNumber
     * @param requestInfo
     * @param tenantId
     * @retur
     */
    public Citizen getUserFromMobileNumber(String mobileNumber, RequestInfo requestInfo, String tenantId) {
		UserSearchRequest searchRequest = UserSearchRequest.builder().userName(mobileNumber).active(true)
				.tenantId(tenantId).userType(MRConstants.ROLE_CITIZEN).requestInfo(requestInfo).build();
		StringBuilder url = new StringBuilder(config.getUserHost()+config.getUserSearchEndpoint()); 
		UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, searchRequest), UserResponse.class);
		if(CollectionUtils.isEmpty(res.getUser())) {
			return null;
		}
		return res.getUser().get(0);
	}

}
