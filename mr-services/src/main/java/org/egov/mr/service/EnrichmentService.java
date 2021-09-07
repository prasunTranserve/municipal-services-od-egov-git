package org.egov.mr.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.repository.IdGenRepository;
import org.egov.mr.util.MarriageRegistrationUtil;
import org.egov.mr.web.models.AuditDetails;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mr.web.models.Idgen.IdResponse;
import org.egov.mr.web.models.workflow.BusinessService;
import org.egov.mr.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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

    @Autowired
    public EnrichmentService(IdGenRepository idGenRepository, MRConfiguration config,
                             BoundaryService boundaryService,WorkflowService workflowService,MarriageRegistrationUtil marriageRegistrationUtil) {
        this.idGenRepository = idGenRepository;
        this.config = config;
        this.marriageRegistrationUtil=marriageRegistrationUtil;
        this.boundaryService = boundaryService;
        this.workflowService = workflowService;
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
           
            marriageRegistration.getCoupleDetails().forEach(couple -> {
            	couple.setTenantId(marriageRegistration.getTenantId());
            	couple.setId(UUID.randomUUID().toString());
            });
          

            if (requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN"))
                marriageRegistration.setAccountId(requestInfo.getUserInfo().getUuid());

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
                	
                	if(couple.getTenantId()==null)
                	{
                		couple.setTenantId(marriageRegistration.getTenantId());
                	}
                	
                	if(couple.getCoupleAddress().getId()==null)
                		couple.getCoupleAddress().setId(UUID.randomUUID().toString());
                	
                	if(couple.getGuardianDetails().getId()==null)
                		couple.getGuardianDetails().setId(UUID.randomUUID().toString());
                	
                });
                
                marriageRegistration.getWitness().forEach(  witness -> {
                	
                	if(witness.getId() ==  null)
                		witness.setId(UUID.randomUUID().toString());
                	
                });
                
                
              
            }
           
        });
    }

    /**
     * Sets the licenseNumber generated by idgen
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
                    errorMap.put("IDGEN ERROR ", "The number of LicenseNumber returned by idgen is not equal to number of MarriageRegistartions");
                }

                if (!errorMap.isEmpty())
                    throw new CustomException(errorMap);

                for (int i = 0; i < marriageRegistrations.size(); i++) {
                    MarriageRegistration license = marriageRegistrations.get(i);
                    if ((license.getStatus() != null) && license.getStatus().equalsIgnoreCase(endstates.get(i))) {
                        license.setMrNumber(itr.next());
                        Long time = System.currentTimeMillis();
                        license.setIssuedDate(time);
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
