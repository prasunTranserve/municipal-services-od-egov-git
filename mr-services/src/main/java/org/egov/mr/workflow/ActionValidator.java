package org.egov.mr.workflow;

import static org.egov.mr.util.MRConstants.ACTION_APPLY;
import static org.egov.mr.util.MRConstants.ACTION_INITIATE;
import static org.egov.mr.util.MRConstants.STATUS_INITIATED;
import static org.egov.mr.util.MRConstants.businessService_MR;

import java.util.HashMap;
import java.util.Map;

import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.workflow.BusinessService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component
public class ActionValidator {



    private WorkflowService workflowService;

    @Autowired
    public ActionValidator( WorkflowService workflowService) {
        this.workflowService = workflowService;
    }




    /**
     * Validates create request
     * @param request The marriageRegistration Create request
     */
	public void validateCreateRequest(MarriageRegistrationRequest request){
        Map<String, String> errorMap = new HashMap<>();
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
                       
            String businessService = marriageRegistration.getBusinessService();
            if (businessService == null)
                businessService = businessService_MR;
                
            switch(businessService)
            {
                case businessService_MR:

                    
                    if (ACTION_APPLY.equalsIgnoreCase(marriageRegistration.getAction())) {
                        if (marriageRegistration.getApplicationDocuments() == null)
                            errorMap.put("INVALID ACTION", "Action cannot be changed to APPLY. Application document are not provided");
                    }
                    if (!ACTION_APPLY.equalsIgnoreCase(marriageRegistration.getAction()) &&
                            !ACTION_INITIATE.equalsIgnoreCase(marriageRegistration.getAction())) {
                        errorMap.put("INVALID ACTION", "Action can only be APPLY or INITIATE during create");
                    }
                    break;

             
            }
        });
        //    validateRole(request);

       

        if (!errorMap.isEmpty())
            throw new CustomException(errorMap);
    }


    /**
     * Validates the update request
     * @param request The Marriage Registration update request
     */
    public void validateUpdateRequest(MarriageRegistrationRequest request,BusinessService businessService){
        validateDocumentsForUpdate(request);
       // validateRole(request);
       // validateAction(request);
        validateIds(request,businessService);
    }


    /**
     * Validates the applicationDocument
     * @param request The Marriage Registration create or update request
     */
    private void validateDocumentsForUpdate(MarriageRegistrationRequest request){
        Map<String,String> errorMap = new HashMap<>();
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            if(ACTION_INITIATE.equalsIgnoreCase(marriageRegistration.getAction()) ){
                if(marriageRegistration.getApplicationDocuments()!=null)
                    errorMap.put("INVALID STATUS","Status cannot be INITIATE when application document are provided");
            }
            if(ACTION_APPLY.equalsIgnoreCase(marriageRegistration.getAction())){
                if(marriageRegistration.getApplicationDocuments()==null)
                    errorMap.put("INVALID STATUS","Status cannot be APPLY when application document are not provided");
            }
        });

        if(!errorMap.isEmpty())
            throw new CustomException(errorMap);
    }








    /**
     * Validates if the any new object is added in the request
     * @param request The Marriage Registration update request
     */
    private void validateIds(MarriageRegistrationRequest request,BusinessService businessService){
        Map<String,String> errorMap = new HashMap<>();
        request.getMarriageRegistrations().forEach(marriageRegistration -> {

            String namefBusinessService=marriageRegistration.getBusinessService();
            if((namefBusinessService==null) || (namefBusinessService.equals(businessService_MR))  && (!marriageRegistration.getStatus().equalsIgnoreCase(STATUS_INITIATED)))
            {
                if(!workflowService.isStateUpdatable(marriageRegistration.getStatus(), businessService)) {
                    if (marriageRegistration.getId() == null)
                        errorMap.put("INVALID UPDATE", "Id of marriageRegistration cannot be null");
                    if(marriageRegistration.getMarriagePlace().getId()==null)
                        errorMap.put("INVALID UPDATE", "Id of Marriage Place cannot be null");
                    marriageRegistration.getCoupleDetails().forEach(couple -> {
                        if(couple.getId()==null)
                            errorMap.put("INVALID UPDATE", "Id of Couple cannot be null");
                        
                        if(couple.getCoupleAddress().getId()==null)
                            errorMap.put("INVALID UPDATE", "Id of Couple Address cannot be null");
                      
                        });
                    
                    marriageRegistration.getWitness().forEach( marriageWitness -> {
                    	if(marriageWitness.getId() == null)
                    	{
                    		 errorMap.put("INVALID UPDATE", "Id of Marriage Witness cannot be null");
                    	}
                    });
                 
                    if(!CollectionUtils.isEmpty(marriageRegistration.getApplicationDocuments())){
                        marriageRegistration.getApplicationDocuments().forEach(document -> {
                            if(document.getId()==null)
                                errorMap.put("INVALID UPDATE", "Id of applicationDocument cannot be null");
                        });
                    }
                }
            }
        });
        if(!errorMap.isEmpty())
            throw new CustomException(errorMap);
    }





}
