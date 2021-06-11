package org.egov.pgr.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.pgr.contract.RequestInfoWrapper;
import org.egov.pgr.contract.SMSRequest;
import org.egov.pgr.contract.ServiceReqSearchCriteria;
import org.egov.pgr.contract.ServiceResponse;
import org.egov.pgr.model.ActionInfo;
import org.egov.pgr.model.Service;
import org.egov.pgr.model.user.UserResponse;
import org.egov.pgr.repository.ServiceRequestRepository;
import org.egov.pgr.utils.PGRConstants;
import org.egov.pgr.utils.PGRUtils;
import org.egov.pgr.utils.WorkFlowConfigs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@org.springframework.stereotype.Service
public class NotificationService {
		
	@Value("${egov.hr.employee.v2.host}")
	private String hrEmployeeV2Host;

	@Value("${egov.hr.employee.v2.search.endpoint}")
	private String hrEmployeeV2SearchEndpoint;
	
	@Value("${egov.hrms.host}")
	private String egovHRMShost;

	@Value("${egov.hrms.search.endpoint}")
	private String egovHRMSSearchEndpoint;
	
	@Value("${egov.user.host}")
	private String egovUserHost;

	@Value("${egov.user.search.endpoint}")
	private String egovUserSearchEndpoint;
	
	@Value("${egov.default.sla.in.ms}")
	private Long egovDefaultServiceSla;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private GrievanceService requestService;

	@Autowired
	private PGRUtils pGRUtils;
	
	public static Map<String, Map<String, String>> localizedMessageMap = new HashMap<>();

	/**
	 * Fetches the Service type and sla hours for the respective service type
	 * 
	 * @param serviceReq
	 * @param requestInfo
	 * @param locale
	 * @return
	 */
	public List<Object> getServiceType(Service serviceReq, RequestInfo requestInfo, String locale) {
		StringBuilder uri = new StringBuilder();
		List<Object> listOfValues = new ArrayList<>();
		MdmsCriteriaReq mdmsCriteriaReq = pGRUtils.prepareSearchRequestForServiceType(uri, serviceReq.getTenantId(),
				serviceReq.getServiceCode(), requestInfo);
		String serviceType = null;
		List<String> serviceTypes = null;
		List<Integer> slaHours = null;
		String tenantId = serviceReq.getTenantId().split("[.]")[0]; // localization values are for now state-level.
		try {
			Object result = serviceRequestRepository.fetchResult(uri, mdmsCriteriaReq);
			serviceTypes = JsonPath.read(result, PGRConstants.JSONPATH_SERVICE_CODES);
			slaHours = JsonPath.read(result, PGRConstants.JSONPATH_SLA);
			if (CollectionUtils.isEmpty(serviceTypes) || CollectionUtils.isEmpty(slaHours))
				return null;
			if (null == localizedMessageMap.get(locale + "|" + tenantId)) // static map that saves code-message pair against locale | tenantId.
				getLocalisedMessages(requestInfo, tenantId, locale, PGRConstants.LOCALIZATION_MODULE_NAME);
			//serviceType = localizedMessageMap.get(locale + "|" + tenantId).get(PGRConstants.LOCALIZATION_COMP_CATEGORY_PREFIX + serviceTypes.get(0).toUpperCase()); //result set is always of size one.
			serviceType = localizedMessageMap.get(locale + "|" + tenantId).get(PGRConstants.LOCALIZATION_CODE_COMPLAINT_PREFIX + serviceTypes.get(0).toUpperCase()); //result set is always of size one.
			if(StringUtils.isEmpty(serviceType))
				serviceType = PGRUtils.splitCamelCase(serviceTypes.get(0));
		} catch (Exception e) {
			log.error("SERVICE_TYPE_EXCEPTION", e);
			return null;
		}
		Integer sla = slaHours.get(0) / 24; //converting hours to days.
		listOfValues.add(serviceType); listOfValues.add(sla);
		return listOfValues;
	}
	
	public String getDepartmentFromServiceCode(Service serviceReq, RequestInfo requestInfo)
	{
		String department =  null ;
		StringBuilder uri = new StringBuilder();
		MdmsCriteriaReq mdmsCriteriaReq = pGRUtils.prepareSearchRequestForServiceType(uri, serviceReq.getTenantId(),serviceReq.getServiceCode(), requestInfo);
		List<String> departmentList = null;
		try {
			Object result = serviceRequestRepository.fetchResult(uri, mdmsCriteriaReq);
			departmentList = JsonPath.read(result, PGRConstants.JSONPATH_DEPARTMENT);
			if (CollectionUtils.isEmpty(departmentList) )
				return department;
			department = departmentList.get(0);
			
		}catch (Exception e) {
			log.error("SERVICE_TYPE_EXCEPTION", e);
			return department;
		}
		return department;
	}
	

	/**
	 * Fetches Employee Details
	 * 
	 * @param tenantId
	 * @param id
	 * @param requestInfo
	 * @return
	 */
	public Map<String, String> getEmployeeDetails(String tenantId, String id, RequestInfo requestInfo) {
		StringBuilder uri = new StringBuilder();
		RequestInfoWrapper requestInfoWrapper = new RequestInfoWrapper();
		requestInfoWrapper.setRequestInfo(requestInfo);
		uri.append(egovHRMShost).append(egovHRMSSearchEndpoint).append("?ids=" + id)
				.append("&tenantId=" + tenantId);
		Object response = null;
		Map<String, String> employeeDetails = new HashMap<>();
		try {
			response = serviceRequestRepository.fetchResult(uri, requestInfoWrapper);
			if (null == response) {
				return employeeDetails;
			}
			employeeDetails.put("name", JsonPath.read(response, PGRConstants.EMPLOYEE_NAME_JSONPATH));
			employeeDetails.put("phone", JsonPath.read(response, PGRConstants.EMPLOYEE_PHNO_JSONPATH));
			try {
				employeeDetails.put("emailId", JsonPath.read(response, PGRConstants.EMPLOYEE_EMAILID_JSONPATH));
			} catch (Exception e) {
				log.info(" email id not found for the user  "+JsonPath.read(response, PGRConstants.EMPLOYEE_NAME_JSONPATH));
			}
			employeeDetails.put("department", ((List<String>) JsonPath.read(response, PGRConstants.EMPLOYEE_DEPTCODE_JSONPATH)).get(0));
			employeeDetails.put("designation", ((List<String>)JsonPath.read(response, PGRConstants.EMPLOYEE_DESGCODE_JSONPATH)).get(0));
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
		return employeeDetails;
	}


	/**
	 * Fetches Employee Details based roles and department
	 * @param tenantId
	 * @param departments
	 * @param roles
	 * @param requestInfo
	 * @return
	 */
	public List<Map<String, String>> getEmployeeDetailsOnDepartmentRoleBased(String tenantId, String department ,String role , RequestInfo requestInfo) {
		StringBuilder uri = new StringBuilder();
		RequestInfoWrapper requestInfoWrapper = new RequestInfoWrapper();
		requestInfoWrapper.setRequestInfo(requestInfo);
		uri.append(egovHRMShost).append(egovHRMSSearchEndpoint).append("?tenantId=" + tenantId).append("&roles=" + role);

		if (department != null && !department.isEmpty()) {
			uri.append("&departments=" + department);
		}

		Object response = null;



		List<Map<String, String>>  allEmployeeDeatils = new ArrayList<Map<String,String>>();

		try {
			ObjectMapper mapper = pGRUtils.getObjectMapper();
			response = serviceRequestRepository.fetchResult(uri, requestInfoWrapper);
			if (null == response) {
				return allEmployeeDeatils;
			}

			List<Map<String, Object>> resultCast = mapper.convertValue(JsonPath.read(response, PGRConstants.EMPLOYEE_BASE_JSONPATH), List.class);

			for (Map<String, Object> employee : resultCast) {
				Map<String, Object> user = (Map) employee.get("user");

				Map<String, String> employeeDetails = new HashMap<>();

				employeeDetails.put("name", user.get("name")!=null?user.get("name").toString():"");
				employeeDetails.put("phone", user.get("mobileNumber")!=null?user.get("mobileNumber").toString():"");
				try {
					employeeDetails.put("emailId", user.get("emailId")!=null?user.get("emailId").toString():"");
				} catch (Exception e) {
					log.info(" email id not found for the user  ",user.get("name")!=null?user.get("name").toString():"");
				}


				allEmployeeDeatils.add(employeeDetails);

			}


		} catch (Exception e) {
			log.error("Exception: ", e);
		}
		return allEmployeeDeatils;
	}








	/**
	 * An employee might belong to different departments, 
	 * This method fetches all his departments and returns only that department to which the currently assigned complaint belongs to.
	 *  
	 * @param serviceReq
	 * @param codes
	 * @param requestInfo
	 * @return
	 */
	public String getDepartmentForNotification(Service serviceReq, List<String> codes, RequestInfo requestInfo) {
		String department = null;
		try {
			if (CollectionUtils.isEmpty(codes))
				 return department;
			else {
				Object response = requestService.fetchServiceDefs(requestInfo, serviceReq.getTenantId(), codes);
				if (null == response) {
					 return department;
				}
				try {
					List<String> departments = JsonPath.read(response, "$.MdmsRes.RAINMAKER-PGR.ServiceDefs.[?(@.serviceCode=='" + serviceReq.getServiceCode() + "')].department");
					if(CollectionUtils.isEmpty(departments)) {
						 return department;
					}else {
						department = departments.get(0); //Every serviceCode is mapped to always only one dept.
					}
				} catch (Exception e) {
			log.error("DEPARTMENT_EXCEPTION", e);
					 return department;
				}
			}
		}catch (Exception e) {
			log.error("DEPARTMENT_EXCEPTION", e);
		    return department;
		}
		 return department;
	}

	/**
	 * 	Fetches designation for notification text
	 * 
	 * @param serviceReq
	 * @param code
	 * @param requestInfo
	 * @return
	 */
	public String getDesignation(Service serviceReq, String code, RequestInfo requestInfo) {
		StringBuilder uri = new StringBuilder();
		MdmsCriteriaReq mdmsCriteriaReq = pGRUtils.prepareMdMsRequestForDesignation(uri, serviceReq.getTenantId(), code,
				requestInfo);
		List<String> designations = null;
		try {
			Object result = serviceRequestRepository.fetchResult(uri, mdmsCriteriaReq);
			log.info("Desgination search: " + result);
			designations = JsonPath.read(result, PGRConstants.JSONPATH_DESIGNATIONS);
			if (null == designations || designations.isEmpty())
				return null;
		} catch (Exception e) {
			log.error("DESIGNATION_EXCEPTION", e);
			return null;
		}

		return designations.get(0);
	}
	
	
	/**
	 * 	Fetches department for notification text
	 * 
	 * @param serviceReq
	 * @param code
	 * @param requestInfo
	 * @return
	 */
	public String getDepartment(Service serviceReq, String code, RequestInfo requestInfo) {
		StringBuilder uri = new StringBuilder();
		List<String>  codesList = new ArrayList<>();
		codesList.add(code);
		MdmsCriteriaReq mdmsCriteriaReq = pGRUtils.prepareMdMsRequestForDept(uri, serviceReq.getTenantId(), codesList,
				requestInfo);
		List<String> departments = null;
		try {
			Object result = serviceRequestRepository.fetchResult(uri, mdmsCriteriaReq);
			log.info("Department search: " + result);
			departments = JsonPath.read(result, PGRConstants.JSONPATH_DEPARTMENTS);
			if (null == departments || departments.isEmpty())
				return null;
		} catch (Exception e) {
			log.error("DESIGNATION_EXCEPTION", e);
			return null;
		}

		return departments.get(0);
	}
	
	

	/**
	 * Populates the localized msg cache
	 * 
	 * @param requestInfo
	 * @param tenantId
	 * @param locale
	 * @param module
	 */
	public void getLocalisedMessages(RequestInfo requestInfo, String tenantId, String locale, String module) {
		Map<String, String> mapOfCodesAndMessages = new HashMap<>();
		StringBuilder uri = new StringBuilder();
		RequestInfoWrapper requestInfoWrapper = pGRUtils.prepareRequestForLocalization(uri, requestInfo, locale,
				tenantId, module);
		List<String> codes = null;
		List<String> messages = null;
		Object result = null;
		try {
			result = serviceRequestRepository.fetchResult(uri, requestInfoWrapper);
			codes = JsonPath.read(result, PGRConstants.LOCALIZATION_CODES_JSONPATH);
			messages = JsonPath.read(result, PGRConstants.LOCALIZATION_MSGS_JSONPATH);
		} catch (Exception e) {
			log.error("Exception while fetching from localization: " + e);
		}
		if (null != result) {
			for (int i = 0; i < codes.size(); i++) {
				mapOfCodesAndMessages.put(codes.get(i), messages.get(i));
			}
			localizedMessageMap.put(locale + "|" + tenantId, mapOfCodesAndMessages);
		}
	}
	
	/**
	 * Fetches phone number for notification based on the recepient of the notif.
	 * 
	 * @param requestInfo
	 * @param userId
	 * @param tenantId
	 * @param assignee
	 * @param role
	 * @return
	 */
	public String getMobileAndIdForNotificationService(RequestInfo requestInfo, String userId, String tenantId, String assignee, String role) {
		String phoneNumber = null;
		String uuid = "uuid";
		Object response = null;
		ObjectMapper mapper = pGRUtils.getObjectMapper();
		StringBuilder uri = new StringBuilder();
		Object request = new HashMap<>();
		if(role.equals(PGRConstants.ROLE_CITIZEN)) {
			request = pGRUtils.prepareRequestForUserSearch(uri, requestInfo, userId, tenantId);
			try {
				response = serviceRequestRepository.fetchResult(uri, request);
				if(null != response) {
					UserResponse res = mapper.convertValue(response, UserResponse.class);
					phoneNumber = res.getUser().get(0).getMobileNumber();
					uuid = res.getUser().get(0).getUuid();
				}
			}catch(Exception e) {
				log.error("Couldn't fetch user for id: " + userId + " error: " + e);
			}
			return phoneNumber + "|" + uuid;
		}else if(role.equals(PGRConstants.ROLE_EMPLOYEE)) {
			Map<String, String> employeeDetails = getEmployeeDetails(tenantId, assignee, requestInfo);
			if(!StringUtils.isEmpty(employeeDetails.get("phone"))) {
				phoneNumber = employeeDetails.get("phone");
			}
		}
		return phoneNumber + "|" + uuid;
	}
	
	
	/**
	 * Fetches email id for notification based on the recepient of the notif.
	 * 
	 * @param requestInfo
	 * @param userId
	 * @param tenantId
	 * @param assignee
	 * @param role
	 * @return
	 */
	public String getEmailIdForNotificationService(RequestInfo requestInfo, String userId, String tenantId, String assignee, String role) {
		String emailId = null;
		String uuid = "uuid";
		Object response = null;
		ObjectMapper mapper = pGRUtils.getObjectMapper();
		StringBuilder uri = new StringBuilder();
		Object request = new HashMap<>();
		if(role.equals(PGRConstants.ROLE_CITIZEN)) {
			request = pGRUtils.prepareRequestForUserSearch(uri, requestInfo, userId, tenantId);
			try {
				response = serviceRequestRepository.fetchResult(uri, request);
				if(null != response) {
					UserResponse res = mapper.convertValue(response, UserResponse.class);
					emailId = res.getUser().get(0).getEmailId();
					uuid = res.getUser().get(0).getUuid();
				}
			}catch(Exception e) {
				log.error("Couldn't fetch user for id: " + userId + " error: " + e);
			}
			return emailId + "|" + uuid;
		}else if(role.equals(PGRConstants.ROLE_EMPLOYEE)) {
			Map<String, String> employeeDetails = getEmployeeDetails(tenantId, assignee, requestInfo);
			if(!StringUtils.isEmpty(employeeDetails.get("emailId"))) {
				emailId = employeeDetails.get("emailId");
			}
		}
		return emailId + "|" + uuid;
	}
	
	
	
	
	/**
	 * Returns current assignee for a complaint
	 * 
	 * @param serviceReq
	 * @param requestInfo
	 * @return
	 */
	public String getCurrentAssigneeForTheServiceRequest(Service serviceReq, RequestInfo requestInfo) {
		ServiceReqSearchCriteria serviceReqSearchCriteria = ServiceReqSearchCriteria.builder().tenantId(serviceReq.getTenantId())
				.serviceRequestId(Arrays.asList(serviceReq.getServiceRequestId())).build();
		ServiceResponse response = (ServiceResponse) requestService.getServiceRequestDetails(requestInfo, serviceReqSearchCriteria);
		try {
			if((WorkFlowConfigs.STATUS_RESOLVED.equalsIgnoreCase(serviceReq.getStatus().toString()) 
					|| WorkFlowConfigs.STATUS_CLOSED.equalsIgnoreCase(serviceReq.getStatus().toString()))
					&& isEscalated(response.getActionHistory().get(0).getActions())) {
				for(ActionInfo actionInfo : response.getActionHistory().get(0).getActions()){
					if(WorkFlowConfigs.STATUS_RESOLVED.equalsIgnoreCase(actionInfo.getStatus())) {
						return actionInfo.getBy().split(":")[0];
					}
				}
			}else {
				List<ActionInfo> actions = response.getActionHistory().get(0).getActions().stream()
						.filter(obj -> !StringUtils.isEmpty(obj.getAssignee())).collect(Collectors.toList());
				if(CollectionUtils.isEmpty(actions))
					return null;
				return actions.get(0).getAssignee();
			}
		}catch(Exception e) {
			return null;
		}
		return null;
	}
	
	private boolean isEscalated(List<ActionInfo> actions) {
		ActionInfo actionInfo = actions.stream().filter(obj -> 
			obj.getStatus()!= null &&
			(obj.getStatus().equalsIgnoreCase(WorkFlowConfigs.STATUS_ESCALATED_LEVEL1_PENDING)
			|| obj.getStatus().equalsIgnoreCase(WorkFlowConfigs.STATUS_ESCALATED_LEVEL2_PENDING)
			|| obj.getStatus().equalsIgnoreCase(WorkFlowConfigs.STATUS_ESCALATED_LEVEL3_PENDING)
			|| obj.getStatus().equalsIgnoreCase(WorkFlowConfigs.STATUS_ESCALATED_LEVEL4_PENDING))).findAny().orElse(null);
		
		if(null != actionInfo) {
			return true;
		}
		
		return false;
	}
	
	public boolean isEscalatedToLevel4(Service serviceReq, RequestInfo requestInfo) {
		
		try {
			ServiceReqSearchCriteria serviceReqSearchCriteria = ServiceReqSearchCriteria.builder().tenantId(serviceReq.getTenantId())
					.serviceRequestId(Arrays.asList(serviceReq.getServiceRequestId())).build();
			ServiceResponse response = (ServiceResponse) requestService.getServiceRequestDetails(requestInfo, serviceReqSearchCriteria);
			if(null != response) {	
				ActionInfo actionInfo = response.getActionHistory().get(0).getActions().stream().filter(obj -> 
				obj.getStatus() !=null &&  obj.getStatus().equalsIgnoreCase(WorkFlowConfigs.STATUS_ESCALATED_LEVEL4_PENDING)).findAny().orElse(null);
				
				if(null != actionInfo) {
					return true;
				}
			}
		}catch(Exception e) {
			log.error("Error in isEscalatedToLevel4 "+e);
		}
		
		return false;
	}
	
	public Long getSlaHours() {
		log.info("Returning default sla: " + egovDefaultServiceSla);
		return egovDefaultServiceSla;
	}

}