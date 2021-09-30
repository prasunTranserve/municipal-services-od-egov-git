package org.egov.mr.service;

import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.model.user.Citizen;
import org.egov.mr.model.user.CreateUserRequest;
import org.egov.mr.model.user.UserResponse;
import org.egov.mr.model.user.UserSearchRequest;
import org.egov.mr.model.user.UserType;
import org.egov.mr.repository.IdGenRepository;
import org.egov.mr.repository.ServiceRequestRepository;
import org.egov.mr.util.MRConstants;
import org.egov.mr.util.MarriageRegistrationUtil;
import org.egov.mr.web.models.AuditDetails;
import org.egov.mr.web.models.Couple;
import org.egov.mr.web.models.CoupleDetails;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mr.web.models.Idgen.IdResponse;
import org.egov.mr.web.models.MarriageRegistration.ApplicationTypeEnum;
import org.egov.mr.web.models.workflow.BusinessService;
import org.egov.mr.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import java.util.*;
import java.util.stream.Collectors;
import static org.egov.mr.util.MRConstants.*;


@Service
public class EnrichmentService {

	private IdGenRepository idGenRepository;
	private MRConfiguration config;
	private MarriageRegistrationUtil marriageRegistrationUtil;
	private BoundaryService boundaryService;
	private WorkflowService workflowService;
    private ObjectMapper mapper;
    private ServiceRequestRepository serviceRequestRepository;
    
    
	@Autowired
	public EnrichmentService(IdGenRepository idGenRepository, MRConfiguration config,
			BoundaryService boundaryService,WorkflowService workflowService,MarriageRegistrationUtil marriageRegistrationUtil,ObjectMapper mapper,ServiceRequestRepository serviceRequestRepository) {
		this.idGenRepository = idGenRepository;
		this.config = config;
		this.marriageRegistrationUtil=marriageRegistrationUtil;
		this.boundaryService = boundaryService;
		this.workflowService = workflowService;
		this.mapper = mapper;
		this.serviceRequestRepository =serviceRequestRepository ;
	}


	/**
	 * Enriches the incoming createRequest
	 * @param marriageRegistrationRequest The create request for the maariageRegistration
	 */
	public void enrichMRCreateRequest(MarriageRegistrationRequest marriageRegistrationRequest) {
		RequestInfo requestInfo = marriageRegistrationRequest.getRequestInfo();
		String uuid = requestInfo.getUserInfo().getUuid();
		AuditDetails auditDetails = marriageRegistrationUtil.getAuditDetails(uuid, true);
		marriageRegistrationRequest.getMarriageRegistrations().forEach(marriageRegistration -> {
			marriageRegistration.setAuditDetails(auditDetails);
			marriageRegistration.setId(UUID.randomUUID().toString());
			marriageRegistration.setApplicationDate(auditDetails.getCreatedTime());
			marriageRegistration.getMarriagePlace().setId(UUID.randomUUID().toString());
			marriageRegistration.getMarriagePlace().setAuditDetails(auditDetails);
			String businessService = marriageRegistration.getBusinessService();
			if (businessService == null)
			{
				businessService = businessService_MR;
				marriageRegistration.setBusinessService(businessService);
			}
			
			switch (businessService) {
            case businessService_MR:

                if(marriageRegistration.getApplicationType() != null && marriageRegistration.getApplicationType().toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION)){
                	marriageRegistration.setMrNumber(marriageRegistrationRequest.getMarriageRegistrations().get(0).getMrNumber());

                }
               
                break;
        }

			marriageRegistration.getCoupleDetails().forEach(couple -> {
				couple.getBride().setTenantId(marriageRegistration.getTenantId());
				couple.getBride().setId(UUID.randomUUID().toString());
				couple.getGroom().setTenantId(marriageRegistration.getTenantId());
				couple.getGroom().setId(UUID.randomUUID().toString());
			});

			if (marriageRegistration.getAction().equalsIgnoreCase(ACTION_APPLY)) {
                marriageRegistration.getApplicationDocuments().forEach(document -> {
                    document.setId(UUID.randomUUID().toString());
                    document.setActive(true);
                });
            }
			
			
			marriageRegistration.getCoupleDetails().forEach(couple -> {
				
				if(couple.getId() ==null )
					couple.setId(UUID.randomUUID().toString());

				if(couple.getBride().getAddress()!=null )
					couple.getBride().getAddress().setId(UUID.randomUUID().toString());

				if(couple.getBride().getGuardianDetails()!=null )
					couple.getBride().getGuardianDetails().setId(UUID.randomUUID().toString());
				
				if(couple.getBride().getWitness()!=null )
					couple.getBride().getWitness().setId(UUID.randomUUID().toString());
				
				if(couple.getGroom().getAddress()!=null )
					couple.getGroom().getAddress().setId(UUID.randomUUID().toString());

				if(couple.getGroom().getGuardianDetails()!=null )
					couple.getGroom().getGuardianDetails().setId(UUID.randomUUID().toString());
				
				if(couple.getGroom().getWitness()!=null )
					couple.getGroom().getWitness().setId(UUID.randomUUID().toString());

			});


			
			
			
			if(marriageRegistration.getApplicationType() !=null && marriageRegistration.getApplicationType().toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION)){
                if(marriageRegistration.getAction().equalsIgnoreCase(ACTION_APPLY) || marriageRegistration.getAction().equalsIgnoreCase(MRConstants.ACTION_INITIATE)){
                    marriageRegistration.getApplicationDocuments().forEach(document -> {
                        document.setId(UUID.randomUUID().toString());
                        document.setActive(true);
                    });
                }
                               
            }
			

			if (requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN"))
				marriageRegistration.setAccountId(requestInfo.getUserInfo().getUuid());
			else
			{
				List<String>  userRoles = new ArrayList<>();
				
				marriageRegistrationRequest.getRequestInfo().getUserInfo().getRoles().forEach(role -> {
					userRoles.add(role.getCode());
				});
				
				if(userRoles.contains(ROLE_CODE_COUNTER_EMPLOYEE) && marriageRegistration.getAction().equalsIgnoreCase(MRConstants.ACTION_INITIATE)
						&&  marriageRegistration.getApplicationType() != null && marriageRegistration.getApplicationType().toString().equalsIgnoreCase(ApplicationTypeEnum.NEW.toString()) )
				{
					marriageRegistration.getCoupleDetails().forEach(couple -> {
						if(couple.getBride().getIsPrimaryOwner())
						{
							String mobileNumber = couple.getBride().getAddress().getContact();
							
							String accId = null ;
							
							accId = marriageRegistrationUtil.isUserPresent(mobileNumber,requestInfo,marriageRegistration.getTenantId());
							if (StringUtils.isEmpty(accId)) {
								accId = createUser(couple.getBride() , requestInfo,marriageRegistration.getTenantId());
							}
							marriageRegistration.setAccountId(accId);
						}
						if(couple.getGroom().getIsPrimaryOwner())
						{
							String mobileNumber = couple.getGroom().getAddress().getContact();
							
							String accId = null ;
							
							accId = marriageRegistrationUtil.isUserPresent(mobileNumber,requestInfo,marriageRegistration.getTenantId());
							if (StringUtils.isEmpty(accId)) {
								accId = createUser(couple.getGroom() , requestInfo,marriageRegistration.getTenantId());
							}
							marriageRegistration.setAccountId(accId);
						}
						
					});
				}
				
			}

		});



		setIdgenIds(marriageRegistrationRequest);
		setStatusForCreate(marriageRegistrationRequest);
		String businessService = marriageRegistrationRequest.getMarriageRegistrations().isEmpty()?null:marriageRegistrationRequest.getMarriageRegistrations().get(0).getBusinessService();
		if (businessService == null)
			businessService = businessService_MR;
		switch (businessService) {
		case businessService_MR:
			boundaryService.getAreaType(marriageRegistrationRequest, config.getHierarchyTypeCode());
			break;
		}
	}


	
	
	

	
	private String createUser(CoupleDetails couple ,RequestInfo requestInfo, String tenantId) {
		Citizen citizen = new Citizen();
		citizen.setUserName(couple.getAddress().getContact());
		citizen.setName(couple.getFirstName());
		citizen.setActive(true);
		citizen.setMobileNumber(couple.getAddress().getContact());
		citizen.setTenantId(tenantId);
		citizen.setType(UserType.CITIZEN);
		citizen.setRoles(Arrays.asList(org.egov.common.contract.request.Role.builder().code(ROLE_CITIZEN).build()));
		StringBuilder url = new StringBuilder(config.getUserHost()+config.getUserCreateEndpoint()); 
		CreateUserRequest req = CreateUserRequest.builder().citizen(citizen).requestInfo(requestInfo).build();
		UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, req), UserResponse.class);
		return res.getUser().get(0).getUuid().toString();
	}


	/**
	 * Returns a list of numbers generated from idgen
	 *
	 * @param requestInfo RequestInfo from the request
	 * @param tenantId    tenantId of the city
	 * @param idKey       code of the field defined in application properties for which ids are generated for
	 * @param idformat    format in which ids are to be generated
	 * @param count       Number of ids to be generated
	 * @return List of ids generated using idGen service
	 */
	private List<String> getIdList(RequestInfo requestInfo, String tenantId, String idKey,
			String idformat, int count) {
		List<IdResponse> idResponses = idGenRepository.getId(requestInfo, tenantId, idKey, idformat, count).getIdResponses();

		if (CollectionUtils.isEmpty(idResponses))
			throw new CustomException("IDGEN ERROR", "No ids returned from idgen Service");

		return idResponses.stream()
				.map(IdResponse::getId).collect(Collectors.toList());
	}


	/**
	 * Sets the ApplicationNumber for given MarriageRegistrationRequest
	 *
	 * @param request MarriageRegistrationRequest which is to be created
	 */
	private void setIdgenIds(MarriageRegistrationRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		String tenantId = request.getMarriageRegistrations().get(0).getTenantId();
		List<MarriageRegistration> marriageRegistrations = request.getMarriageRegistrations();
		String businessService = marriageRegistrations.isEmpty() ? null : marriageRegistrations.get(0).getBusinessService();
		if (businessService == null)
			businessService = businessService_MR;
		List<String> applicationNumbers = null;
		switch (businessService) {
		case businessService_MR:
			applicationNumbers = getIdList(requestInfo, tenantId, config.getApplicationNumberIdgenNameMR(), config.getApplicationNumberIdgenFormatMR(), request.getMarriageRegistrations().size());
			break;

		}
		ListIterator<String> itr = applicationNumbers.listIterator();

		Map<String, String> errorMap = new HashMap<>();
		if (applicationNumbers.size() != request.getMarriageRegistrations().size()) {
			errorMap.put("IDGEN ERROR ", "The number of MarriageRegistrations returned by idgen is not equal to number of MarriageRegistrations");
		}

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

		marriageRegistrations.forEach(marriageRegistartion -> {
			marriageRegistartion.setApplicationNumber(itr.next());
		});
	}







	/**
	 * Enriches the boundary object in address
	 * @param marriageRegistrationRequest The create request
	 */
	public void enrichBoundary(MarriageRegistrationRequest marriageRegistrationRequest){
		List<MarriageRegistrationRequest> requests = getRequestByTenantId(marriageRegistrationRequest);
		requests.forEach(tenantWiseRequest -> {
			boundaryService.getAreaType(tenantWiseRequest,config.getHierarchyTypeCode());
		});
	}


	/**
	 *
	 * @param request
	 * @return
	 */
	private List<MarriageRegistrationRequest> getRequestByTenantId(MarriageRegistrationRequest request){
		List<MarriageRegistration> marriageRegistrations = request.getMarriageRegistrations();
		RequestInfo requestInfo = request.getRequestInfo();

		Map<String,List<MarriageRegistration>> tenantIdToProperties = new HashMap<>();
		if(!CollectionUtils.isEmpty(marriageRegistrations)){
			marriageRegistrations.forEach(marriageRegistration -> {
				if(tenantIdToProperties.containsKey(marriageRegistration.getTenantId()))
					tenantIdToProperties.get(marriageRegistration.getTenantId()).add(marriageRegistration);
				else{
					List<MarriageRegistration> list = new ArrayList<>();
					list.add(marriageRegistration);
					tenantIdToProperties.put(marriageRegistration.getTenantId(),list);
				}
			});
		}
		List<MarriageRegistrationRequest> requests = new LinkedList<>();

		tenantIdToProperties.forEach((key,value)-> {
			requests.add(new MarriageRegistrationRequest(requestInfo,value));
		});
		return requests;
	}





	/**
	 * Sets status for create request
	 * @param marriageRegistrationRequest The create request
	 */
	private void setStatusForCreate(MarriageRegistrationRequest marriageRegistrationRequest) {
		marriageRegistrationRequest.getMarriageRegistrations().forEach(marriageRegistration -> {
			String businessService = marriageRegistrationRequest.getMarriageRegistrations().isEmpty()?null:marriageRegistrationRequest.getMarriageRegistrations().get(0).getBusinessService();
			if (businessService == null)
				businessService = businessService_MR;
			switch (businessService) {
			case businessService_MR:
				if (marriageRegistration.getAction().equalsIgnoreCase(ACTION_INITIATE))
					marriageRegistration.setStatus(STATUS_INITIATED);
				if (marriageRegistration.getAction().equalsIgnoreCase(ACTION_APPLY))
					marriageRegistration.setStatus(STATUS_APPLIED);
				break;


			}
		});
	}


	public void enrichMRUpdateRequest(MarriageRegistrationRequest marriageRegistrationRequest, BusinessService businessService){
		RequestInfo requestInfo = marriageRegistrationRequest.getRequestInfo();
		AuditDetails auditDetails = marriageRegistrationUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		marriageRegistrationRequest.getMarriageRegistrations().forEach(marriageRegistration -> {
			marriageRegistration.setAuditDetails(auditDetails);
			enrichAssignes(marriageRegistration);
			String nameOfBusinessService = marriageRegistration.getBusinessService();
			if(nameOfBusinessService==null)
			{
				nameOfBusinessService=businessService_MR;
				marriageRegistration.setBusinessService(nameOfBusinessService);
			}
			if ( workflowService.isStateUpdatable(marriageRegistration.getStatus(), businessService)) {
				marriageRegistration.getMarriagePlace().setAuditDetails(auditDetails);

				if(!CollectionUtils.isEmpty(marriageRegistration.getApplicationDocuments())){
					marriageRegistration.getApplicationDocuments().forEach(document -> {
						if(document.getId()==null){
							document.setId(UUID.randomUUID().toString());
							document.setActive(true);
						}
					});

				}
				
				

				if(!CollectionUtils.isEmpty(marriageRegistration.getVerificationDocuments())){
					marriageRegistration.getVerificationDocuments().forEach(document -> {
						if(document.getId()==null){
							document.setId(UUID.randomUUID().toString());
							document.setActive(true);
						}
					});

				}

				marriageRegistration.getCoupleDetails().forEach(couple -> {
					
					if(couple.getId()==null)
					{
						couple.setId(UUID.randomUUID().toString());
					}
					
					
					if(couple.getBride().getId()==null)
					{
						couple.getBride().setId(UUID.randomUUID().toString());
					}

					if(couple.getBride().getTenantId()==null)
					{
						couple.getBride().setTenantId(marriageRegistration.getTenantId());
					}

					if(couple.getBride().getAddress()!=null &&  couple.getBride().getAddress().getId()==null)
						couple.getBride().getAddress().setId(UUID.randomUUID().toString());

					if(couple.getBride().getGuardianDetails()!=null && couple.getBride().getGuardianDetails().getId()==null)
						couple.getBride().getGuardianDetails().setId(UUID.randomUUID().toString());
					
					if(couple.getBride().getWitness()!=null && couple.getBride().getWitness().getId()==null)
						couple.getBride().getWitness().setId(UUID.randomUUID().toString());
					
					if(couple.getGroom().getId()==null)
					{
						couple.getGroom().setId(UUID.randomUUID().toString());
					}

					if(couple.getGroom().getTenantId()==null)
					{
						couple.getGroom().setTenantId(marriageRegistration.getTenantId());
					}

					if(couple.getGroom().getAddress()!=null &&  couple.getGroom().getAddress().getId()==null)
						couple.getGroom().getAddress().setId(UUID.randomUUID().toString());

					if(couple.getGroom().getGuardianDetails()!=null && couple.getGroom().getGuardianDetails().getId()==null)
						couple.getGroom().getGuardianDetails().setId(UUID.randomUUID().toString());
					
					if(couple.getGroom().getWitness()!=null && couple.getGroom().getWitness().getId()==null)
						couple.getGroom().getWitness().setId(UUID.randomUUID().toString());

				});


			



			}
			
			if (marriageRegistration.getAction().equalsIgnoreCase(ACTION_SCHEDULE) || marriageRegistration.getAction().equalsIgnoreCase(ACTION_RESCHEDULE)) {
				if(!CollectionUtils.isEmpty(marriageRegistration.getAppointmentDetails())){
					marriageRegistration.getAppointmentDetails().forEach(appointment -> {
						if(appointment.getId()==null){
							appointment.setId(UUID.randomUUID().toString());
							appointment.setActive(true);
						}
					});
				}
			}

		});
	}

	/**
	 * Sets the MarriageRegistrationNumber generated by idgen
	 * @param request The update request
	 */
	private void setMRNumberAndIssueDate(MarriageRegistrationRequest request,List<String>endstates ) {
		RequestInfo requestInfo = request.getRequestInfo();
		String tenantId = request.getMarriageRegistrations().get(0).getTenantId();
		List<MarriageRegistration> marriageRegistrations = request.getMarriageRegistrations();
		int count=0;



		for (int i = 0; i < marriageRegistrations.size(); i++) {
			MarriageRegistration marriageRegistration = marriageRegistrations.get(i);
			if ((marriageRegistration.getStatus() != null) && marriageRegistration.getStatus().equalsIgnoreCase(endstates.get(i)))
				count++;
		}
		if (count != 0) {
			List<String> mrNumbers = null;
			String businessService = marriageRegistrations.isEmpty() ? null : marriageRegistrations.get(0).getBusinessService();
			if (businessService == null)
				businessService = businessService_MR;
			switch (businessService) {
			case businessService_MR:
				mrNumbers = getIdList(requestInfo, tenantId, config.getMrNumberIdgenNameMR(), config.getMrNumberIdgenFormatMR(), count);
				break;

			}
			ListIterator<String> itr = mrNumbers.listIterator();

			Map<String, String> errorMap = new HashMap<>();
			if (mrNumbers.size() != count) {
				errorMap.put("IDGEN ERROR ", "The number of MarriageRegistration Numbers returned by idgen is not equal to number of MarriageRegistartions");
			}

			if (!errorMap.isEmpty())
				throw new CustomException(errorMap);

			for (int i = 0; i < marriageRegistrations.size(); i++) {
				MarriageRegistration marriageRegistration = marriageRegistrations.get(i);
				if ((marriageRegistration.getStatus() != null) && marriageRegistration.getStatus().equalsIgnoreCase(endstates.get(i))) {
					marriageRegistration.setMrNumber(itr.next());
					Long time = System.currentTimeMillis();
					marriageRegistration.setIssuedDate(time);
				}
			}


		}
	}


	/**
	 * Adds accountId of the logged in user to search criteria
	 * @param requestInfo The requestInfo of searhc request
	 * @param criteria The MarriageRegistrationSearchCriteria 
	 */
	public void enrichSearchCriteriaWithAccountId(RequestInfo requestInfo,MarriageRegistrationSearchCriteria criteria){
		if(criteria.isEmpty() && requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN")){
			criteria.setAccountId(requestInfo.getUserInfo().getUuid());
			criteria.setMobileNumber(requestInfo.getUserInfo().getUserName());
			criteria.setTenantId(requestInfo.getUserInfo().getTenantId());
		}

	}




	/**
	 * Enriches the object after status is assigned
	 * @param MarriageRegistrationRequest The update request
	 */
	public void postStatusEnrichment(MarriageRegistrationRequest marriageRegistrationRequest,List<String>endstates){
		setMRNumberAndIssueDate(marriageRegistrationRequest,endstates);
	}


	/**
	 * In case of SENDBACKTOCITIZEN enrich the assignee with the owners and creator of marriageRegistration
	 * @param marriageRegistrations  to be enriched
	 */
	public void enrichAssignes(MarriageRegistration marriageRegistrations){

		if(marriageRegistrations.getAction().equalsIgnoreCase(CITIZEN_SENDBACK_ACTION)){

			Set<String> assignes = new HashSet<>();


			// Adding creator of MarriageRegistration
			if(marriageRegistrations.getAccountId()!=null)
				assignes.add(marriageRegistrations.getAccountId());


			marriageRegistrations.setAssignee(new LinkedList<>(assignes));
		}
	}

	



}
