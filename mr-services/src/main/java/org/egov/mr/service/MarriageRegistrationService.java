package org.egov.mr.service;

import static org.egov.mr.util.MRConstants.STATUS_APPROVED;
import static org.egov.mr.util.MRConstants.businessService_MR;
import static org.egov.tracer.http.HttpUtils.isInterServiceCall;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.repository.MRRepository;
import org.egov.mr.service.notification.EditNotificationService;
import org.egov.mr.util.MRConstants;
import org.egov.mr.util.MarriageRegistrationUtil;
import org.egov.mr.validator.MRValidator;
import org.egov.mr.web.models.Difference;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mr.web.models.workflow.BusinessService;
import org.egov.mr.workflow.ActionValidator;
import org.egov.mr.workflow.WorkflowIntegrator;
import org.egov.mr.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;



@Service
public class MarriageRegistrationService {



	private MRValidator mrValidator ;

	private EnrichmentService enrichmentService;

	private CalculatorService calculatorService;

	private MRRepository repository;

	private MRConfiguration config;

	private WorkflowService workflowService;

	private ActionValidator actionValidator;
	
    private DiffService diffService;

	private MarriageRegistrationUtil util;

	private WorkflowIntegrator wfIntegrator;
	
	private EditNotificationService  editNotificationService;

	@Autowired
	public MarriageRegistrationService(MRValidator mrValidator,EnrichmentService enrichmentService , MRRepository repository ,MRConfiguration config ,WorkflowService workflowService ,
			ActionValidator actionValidator,MarriageRegistrationUtil util ,WorkflowIntegrator wfIntegrator,CalculatorService calculatorService,EditNotificationService  editNotificationService,DiffService diffService) {
		this.mrValidator = mrValidator;
		this.enrichmentService = enrichmentService;
		this.repository = repository;
		this.config=config;
		this.workflowService=workflowService;
		this.actionValidator=actionValidator;
		this.util = util;
		this.wfIntegrator =wfIntegrator ;
		this.editNotificationService =editNotificationService ;
		this.calculatorService =calculatorService;
		this.diffService =diffService;
	}

	public List<MarriageRegistration> create(@Valid MarriageRegistrationRequest marriageRegistrationRequest,String businessServicefromPath) {
		if(businessServicefromPath==null)
			businessServicefromPath = businessService_MR;

		mrValidator.validateBusinessService(marriageRegistrationRequest,businessServicefromPath);
		mrValidator.validateCreate(marriageRegistrationRequest);
		enrichmentService.enrichMRCreateRequest(marriageRegistrationRequest);
		
		
		MarriageRegistration.ApplicationTypeEnum applicationType = marriageRegistrationRequest.getMarriageRegistrations().get(0).getApplicationType();

		if(businessServicefromPath!=null && businessServicefromPath.equals(businessService_MR) && applicationType != null && !(applicationType).toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION ) )
		{
			calculatorService.addCalculation(marriageRegistrationRequest);
		}

		/*
		 * call workflow service if it's enable else uses internal workflow process
		 */
		switch(businessServicefromPath)
		{
		case businessService_MR:
			wfIntegrator.callWorkFlow(marriageRegistrationRequest);
			break;
		}

		repository.save(marriageRegistrationRequest);

		return marriageRegistrationRequest.getMarriageRegistrations();
	}


	public List<MarriageRegistration> search(MarriageRegistrationSearchCriteria criteria, RequestInfo requestInfo, String serviceFromPath, HttpHeaders headers){
		List<MarriageRegistration> marriageRegistrations;
		// allow mobileNumber based search by citizen if interserviceCall
		boolean isInterServiceCall = isInterServiceCall(headers);
		mrValidator.validateSearch(requestInfo,criteria,serviceFromPath, isInterServiceCall);
		criteria.setBusinessService(serviceFromPath);
		enrichmentService.enrichSearchCriteriaWithAccountId(requestInfo,criteria);

		marriageRegistrations = getMarriageRegistrationsWithOwnerInfo(criteria,requestInfo);

		return marriageRegistrations;
	}


	public List<MarriageRegistration> getMarriageRegistrationsWithOwnerInfo(MarriageRegistrationSearchCriteria criteria,RequestInfo requestInfo){
		List<MarriageRegistration> marriageRegistrations = repository.getMarriageRegistartions(criteria);
		if(marriageRegistrations.isEmpty())
			return Collections.emptyList();
		return marriageRegistrations;
	}


	public List<MarriageRegistration> getMarriageRegistrationsWithOwnerInfo(MarriageRegistrationRequest request){
		MarriageRegistrationSearchCriteria criteria = new MarriageRegistrationSearchCriteria();
		List<String> ids = new LinkedList<>();
		request.getMarriageRegistrations().forEach(marriageRegistrations -> {ids.add(marriageRegistrations.getId());});

		criteria.setTenantId(request.getMarriageRegistrations().get(0).getTenantId());
		criteria.setIds(ids);
		criteria.setBusinessService(request.getMarriageRegistrations().get(0).getBusinessService());

		List<MarriageRegistration> marriageRegistrations = repository.getMarriageRegistartions(criteria);

		if(marriageRegistrations.isEmpty())
			return Collections.emptyList();
		return marriageRegistrations;
	}

	/**
	 * 
	 * @param marriageRegistartionRequest
	 * @param businessServicefromPath
	 * @return
	 */
	public List<MarriageRegistration> update(MarriageRegistrationRequest marriageRegistartionRequest, String businessServicefromPath){
		MarriageRegistration marriageRegistration = marriageRegistartionRequest.getMarriageRegistrations().get(0);
		MarriageRegistration.ApplicationTypeEnum applicationType = marriageRegistration.getApplicationType();
		List<MarriageRegistration> marriageRegistrationResponse = null;
		if(applicationType != null && (applicationType).toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION ) &&
				marriageRegistration.getAction().equalsIgnoreCase(MRConstants.ACTION_INITIATE) && marriageRegistration.getStatus().equals(MRConstants.STATUS_APPROVED)){
			List<MarriageRegistration> createResponse = create(marriageRegistartionRequest, businessServicefromPath);
			marriageRegistrationResponse =  createResponse;
		}else
		{

			if (businessServicefromPath == null)
				businessServicefromPath = businessService_MR;

			mrValidator.validateBusinessService(marriageRegistartionRequest, businessServicefromPath);

			String businessServiceName = marriageRegistartionRequest.getMarriageRegistrations().get(0).getWorkflowCode();

			BusinessService businessService = workflowService.getBusinessService(marriageRegistartionRequest.getMarriageRegistrations().get(0).getTenantId(), marriageRegistartionRequest.getRequestInfo(), businessServiceName);
			List<MarriageRegistration> searchResult = getMarriageRegistrationsWithOwnerInfo(marriageRegistartionRequest);
			actionValidator.validateUpdateRequest(marriageRegistartionRequest, businessService);
			enrichmentService.enrichMRUpdateRequest(marriageRegistartionRequest, businessService);
			mrValidator.validateUpdate(marriageRegistartionRequest, searchResult);

			mrValidator.validateNonUpdatableFileds(marriageRegistartionRequest, searchResult);

			Map<String, Difference> diffMap = diffService.getDifference(marriageRegistartionRequest, searchResult);
			Map<String, Boolean> idToIsStateUpdatableMap = util.getIdToIsStateUpdatableMap(businessService, searchResult);

			/*
			 * call workflow service if it's enable else uses internal workflow process
			 */
			 List<String> endStates = Collections.nCopies(marriageRegistartionRequest.getMarriageRegistrations().size(),STATUS_APPROVED);
			 switch (businessServicefromPath) {
			 case businessService_MR:
				 wfIntegrator.callWorkFlow(marriageRegistartionRequest);
				 break;

			 }
			 enrichmentService.postStatusEnrichment(marriageRegistartionRequest,endStates);

			 //Need to implement the user creation logic

			 // userService.createUser(marriageRegistartionRequest, false);

			 if(applicationType != null && !(applicationType).toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION ))
					 {
				 calculatorService.addCalculation(marriageRegistartionRequest);
					 }
			 switch (businessServicefromPath) {
             case businessService_MR:
                 editNotificationService.sendEditNotification(marriageRegistartionRequest, diffMap);
                 break;
         }

			 repository.update(marriageRegistartionRequest, idToIsStateUpdatableMap);
			 marriageRegistrationResponse=  marriageRegistartionRequest.getMarriageRegistrations();

		}
		return marriageRegistrationResponse;
	}


	
    


}
